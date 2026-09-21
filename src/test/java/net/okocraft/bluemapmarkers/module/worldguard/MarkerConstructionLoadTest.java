package net.okocraft.bluemapmarkers.module.worldguard;

import com.flowpowered.math.vector.Vector3d;
import com.sun.management.ThreadMXBean;
import de.bluecolored.bluemap.api.markers.ExtrudeMarker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.ObjectMarker;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkerConstructionLoadTest {

    private static final int UPDATE_COUNT = 500_000;
    private static final int WARMUP_RUNS = 3;
    private static final int MEASURED_RUNS = 9;
    private static final Shape SHAPE = Shape.createRect(0, 0, 100, 100);
    private static final Color OUTLINE = new Color(0, 191, 255, 0.4f);
    private static final Color FILL = new Color(30, 144, 255, 0.1f);
    private static final Vector3d POSITION = new Vector3d(0, 63, 0);

    @Test
    void compareBuilderAndDirectConstruction() {
        verifyShapeState(createShapeWithBuilder(), createShapeDirectly());
        verifyExtrudeState(createExtrudeWithBuilder(), createExtrudeDirectly());

        var shape = measurePair(
                MarkerConstructionLoadTest::replaceShapeWithBuilder,
                MarkerConstructionLoadTest::replaceShapeDirectly
        );
        var extrude = measurePair(
                MarkerConstructionLoadTest::replaceExtrudeWithBuilder,
                MarkerConstructionLoadTest::replaceExtrudeDirectly
        );

        reportAndAssert("shape", shape);
        reportAndAssert("extrude", extrude);
    }

    private static void replaceShapeWithBuilder() {
        var markerSet = MarkerSet.builder().label("test").build();
        for (int i = 0; i < UPDATE_COUNT; i++) {
            markerSet.put("region", createShapeWithBuilder());
        }
    }

    private static void replaceShapeDirectly() {
        var markerSet = MarkerSet.builder().label("test").build();
        for (int i = 0; i < UPDATE_COUNT; i++) {
            markerSet.put("region", createShapeDirectly());
        }
    }

    private static void replaceExtrudeWithBuilder() {
        var markerSet = MarkerSet.builder().label("test").build();
        for (int i = 0; i < UPDATE_COUNT; i++) {
            markerSet.put("region", createExtrudeWithBuilder());
        }
    }

    private static void replaceExtrudeDirectly() {
        var markerSet = MarkerSet.builder().label("test").build();
        for (int i = 0; i < UPDATE_COUNT; i++) {
            markerSet.put("region", createExtrudeDirectly());
        }
    }

    private static ShapeMarker createShapeWithBuilder() {
        return ShapeMarker.builder()
                .shape(SHAPE, 63f)
                .lineColor(OUTLINE)
                .depthTestEnabled(false)
                .fillColor(FILL)
                .label("region")
                .detail("detail")
                .position(POSITION)
                .minDistance(0)
                .maxDistance(1000)
                .build();
    }

    private static ShapeMarker createShapeDirectly() {
        var marker = new ShapeMarker("region", SHAPE, 63f);
        marker.setColors(OUTLINE, FILL);
        marker.setDepthTestEnabled(false);
        configure(marker);
        return marker;
    }

    private static ExtrudeMarker createExtrudeWithBuilder() {
        return ExtrudeMarker.builder()
                .shape(SHAPE, -64f, 320f)
                .lineColor(OUTLINE)
                .fillColor(FILL)
                .label("region")
                .detail("detail")
                .position(POSITION)
                .minDistance(0)
                .maxDistance(1000)
                .build();
    }

    private static ExtrudeMarker createExtrudeDirectly() {
        var marker = new ExtrudeMarker("region", SHAPE, -64f, 320f);
        marker.setColors(OUTLINE, FILL);
        configure(marker);
        return marker;
    }

    private static void configure(ObjectMarker marker) {
        marker.setLabel("region");
        marker.setDetail("detail");
        marker.setPosition(POSITION);
        marker.setMinDistance(0);
        marker.setMaxDistance(1000);
    }

    private static void verifyShapeState(ShapeMarker expected, ShapeMarker actual) {
        verifyObjectState(expected, actual);
        assertEquals(expected.getShape(), actual.getShape());
        assertEquals(expected.getHoles(), actual.getHoles());
        assertEquals(expected.getShapeY(), actual.getShapeY());
        assertEquals(expected.isDepthTestEnabled(), actual.isDepthTestEnabled());
        assertEquals(expected.getLineWidth(), actual.getLineWidth());
        assertEquals(expected.getLineColor(), actual.getLineColor());
        assertEquals(expected.getFillColor(), actual.getFillColor());
    }

    private static void verifyExtrudeState(ExtrudeMarker expected, ExtrudeMarker actual) {
        verifyObjectState(expected, actual);
        assertEquals(expected.getShape(), actual.getShape());
        assertEquals(expected.getHoles(), actual.getHoles());
        assertEquals(expected.getShapeMinY(), actual.getShapeMinY());
        assertEquals(expected.getShapeMaxY(), actual.getShapeMaxY());
        assertEquals(expected.isDepthTestEnabled(), actual.isDepthTestEnabled());
        assertEquals(expected.getLineWidth(), actual.getLineWidth());
        assertEquals(expected.getLineColor(), actual.getLineColor());
        assertEquals(expected.getFillColor(), actual.getFillColor());
    }

    private static void verifyObjectState(ObjectMarker expected, ObjectMarker actual) {
        assertEquals(expected.getType(), actual.getType());
        assertEquals(expected.getLabel(), actual.getLabel());
        assertEquals(expected.getPosition(), actual.getPosition());
        assertEquals(expected.getSorting(), actual.getSorting());
        assertEquals(expected.isListed(), actual.isListed());
        assertEquals(expected.getMinDistance(), actual.getMinDistance());
        assertEquals(expected.getMaxDistance(), actual.getMaxDistance());
        assertEquals(expected.getDetail(), actual.getDetail());
        assertEquals(expected.getLink(), actual.getLink());
        assertEquals(expected.isNewTab(), actual.isNewTab());
    }

    private static MeasurementPair measurePair(Runnable builder, Runnable direct) {
        for (int i = 0; i < WARMUP_RUNS; i++) {
            builder.run();
            direct.run();
            direct.run();
            builder.run();
        }

        var builderElapsed = new long[MEASURED_RUNS];
        var builderAllocated = new long[MEASURED_RUNS];
        var directElapsed = new long[MEASURED_RUNS];
        var directAllocated = new long[MEASURED_RUNS];
        var threadBean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        long threadId = Thread.currentThread().threadId();

        for (int i = 0; i < MEASURED_RUNS; i++) {
            if ((i & 1) == 0) {
                measure(builder, builderElapsed, builderAllocated, i, threadBean, threadId);
                measure(direct, directElapsed, directAllocated, i, threadBean, threadId);
            } else {
                measure(direct, directElapsed, directAllocated, i, threadBean, threadId);
                measure(builder, builderElapsed, builderAllocated, i, threadBean, threadId);
            }
        }

        Arrays.sort(builderElapsed);
        Arrays.sort(builderAllocated);
        Arrays.sort(directElapsed);
        Arrays.sort(directAllocated);
        return new MeasurementPair(
                new Measurement(builderElapsed[MEASURED_RUNS / 2], builderAllocated[MEASURED_RUNS / 2]),
                new Measurement(directElapsed[MEASURED_RUNS / 2], directAllocated[MEASURED_RUNS / 2])
        );
    }

    private static void measure(Runnable operation, long[] elapsed, long[] allocated, int index,
                                ThreadMXBean threadBean, long threadId) {
        long allocationBefore = threadBean.getThreadAllocatedBytes(threadId);
        long start = System.nanoTime();
        operation.run();
        elapsed[index] = System.nanoTime() - start;
        allocated[index] = threadBean.getThreadAllocatedBytes(threadId) - allocationBefore;
    }

    private static void reportAndAssert(String markerType, MeasurementPair pair) {
        double allocationReduction = reduction(pair.builder.medianAllocatedBytes, pair.direct.medianAllocatedBytes);
        double timeReduction = reduction(pair.builder.medianNanos, pair.direct.medianNanos);

        System.out.printf(
                "marker-construction-load-test type=%s updates=%d builder-median-ms=%.3f direct-median-ms=%.3f " +
                        "builder-median-allocated-bytes=%d direct-median-allocated-bytes=%d " +
                        "allocation-reduction-percent=%.3f time-reduction-percent=%.3f%n",
                markerType, UPDATE_COUNT,
                pair.builder.medianNanos / 1_000_000d, pair.direct.medianNanos / 1_000_000d,
                pair.builder.medianAllocatedBytes, pair.direct.medianAllocatedBytes,
                allocationReduction, timeReduction
        );

        assertTrue(allocationReduction >= 20, markerType + " must reduce allocation by at least 20%");
        assertTrue(timeReduction >= 10, markerType + " must reduce median elapsed time by at least 10%");
    }

    private static double reduction(long before, long after) {
        return 100d * (before - after) / before;
    }

    private record Measurement(long medianNanos, long medianAllocatedBytes) {
    }

    private record MeasurementPair(Measurement builder, Measurement direct) {
    }
}
