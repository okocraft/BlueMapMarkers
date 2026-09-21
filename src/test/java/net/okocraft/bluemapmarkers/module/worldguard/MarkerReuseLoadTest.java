package net.okocraft.bluemapmarkers.module.worldguard;

import com.flowpowered.math.vector.Vector3d;
import com.sun.management.ThreadMXBean;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarkerReuseLoadTest {

    private static final int UPDATE_COUNT = 500_000;
    private static final int WARMUP_RUNS = 3;
    private static final int MEASURED_RUNS = 9;
    private static final Shape SHAPE = Shape.createRect(0, 0, 100, 100);
    private static final Color OUTLINE = new Color(0, 191, 255, 0.4f);
    private static final Color FILL = new Color(30, 144, 255, 0.1f);

    @Test
    void compareMarkerReplacementAndReuse() {
        for (int i = 0; i < WARMUP_RUNS; i++) {
            replaceMarkers();
            reuseMarker();
        }

        var replacement = measure(MarkerReuseLoadTest::replaceMarkers);
        var reuse = measure(MarkerReuseLoadTest::reuseMarker);

        var expected = createMarker();
        var actual = createMarker();
        updateMarker(actual);
        assertEquals(expected, actual, "Reused marker must have the same observable state as a new marker");

        double allocationReduction = 100d *
                (replacement.medianAllocatedBytes - reuse.medianAllocatedBytes) /
                replacement.medianAllocatedBytes;
        double timeReduction = 100d *
                (replacement.medianNanos - reuse.medianNanos) /
                replacement.medianNanos;

        System.out.printf(
                "marker-reuse-load-test updates=%d replacement-median-ms=%.3f reuse-median-ms=%.3f " +
                        "replacement-median-allocated-bytes=%d reuse-median-allocated-bytes=%d " +
                        "allocation-reduction-percent=%.3f time-reduction-percent=%.3f%n",
                UPDATE_COUNT,
                replacement.medianNanos / 1_000_000d,
                reuse.medianNanos / 1_000_000d,
                replacement.medianAllocatedBytes,
                reuse.medianAllocatedBytes,
                allocationReduction,
                timeReduction
        );

        assertTrue(allocationReduction >= 80, "Marker reuse must reduce allocation by at least 80%");
        assertTrue(timeReduction >= 20, "Marker reuse must reduce median update time by at least 20%");
    }

    private static void replaceMarkers() {
        var markerSet = MarkerSet.builder().label("test").build();
        for (int i = 0; i < UPDATE_COUNT; i++) {
            markerSet.put("region", createMarker());
        }
    }

    private static void reuseMarker() {
        var marker = createMarker();
        var markerSet = MarkerSet.builder().label("test").build();
        markerSet.put("region", marker);
        for (int i = 0; i < UPDATE_COUNT; i++) {
            updateMarker(marker);
        }
    }

    private static ShapeMarker createMarker() {
        return ShapeMarker.builder()
                .shape(SHAPE, 63f)
                .lineColor(OUTLINE)
                .depthTestEnabled(false)
                .fillColor(FILL)
                .label("region")
                .detail("detail")
                .position(new Vector3d(0, 63, 0))
                .minDistance(0)
                .maxDistance(1000)
                .build();
    }

    private static void updateMarker(ShapeMarker marker) {
        marker.setShape(SHAPE, 63f);
        marker.setColors(OUTLINE, FILL);
        marker.setDepthTestEnabled(false);
        marker.setLabel("region");
        marker.setDetail("detail");
        marker.setPosition(new Vector3d(0, 63, 0));
        marker.setMinDistance(0);
        marker.setMaxDistance(1000);
    }

    private static Measurement measure(Runnable operation) {
        var elapsed = new long[MEASURED_RUNS];
        var allocated = new long[MEASURED_RUNS];
        var threadBean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        long threadId = Thread.currentThread().threadId();

        for (int i = 0; i < MEASURED_RUNS; i++) {
            long allocationBefore = threadBean.getThreadAllocatedBytes(threadId);
            long start = System.nanoTime();
            operation.run();
            elapsed[i] = System.nanoTime() - start;
            allocated[i] = threadBean.getThreadAllocatedBytes(threadId) - allocationBefore;
        }

        Arrays.sort(elapsed);
        Arrays.sort(allocated);
        return new Measurement(elapsed[MEASURED_RUNS / 2], allocated[MEASURED_RUNS / 2]);
    }

    private record Measurement(long medianNanos, long medianAllocatedBytes) {
    }
}
