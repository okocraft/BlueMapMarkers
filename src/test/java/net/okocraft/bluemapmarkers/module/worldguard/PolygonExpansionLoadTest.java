package net.okocraft.bluemapmarkers.module.worldguard;

import com.flowpowered.math.vector.Vector2d;
import com.google.common.collect.ImmutableList;
import com.sk89q.worldedit.math.BlockVector2;
import com.sun.management.ThreadMXBean;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PolygonExpansionLoadTest {

    private static final int VERTEX_COUNT = 256;
    private static final int ITERATIONS = 2_500;
    private static final int WARMUP_RUNS = 3;
    private static final int MEASURED_RUNS = 9;
    private static final ImmutableList<BlockVector2> POLYGON = createPolygon();

    private static volatile Vector2d[] sink;

    @Test
    void compareVectorAndPrimitiveCalculations() {
        verifyOutput(POLYGON);
        verifyOutput(POLYGON.reverse());
        verifyOutput(ImmutableList.of(
                BlockVector2.at(0, 0),
                BlockVector2.at(10, 0),
                BlockVector2.at(5, 0),
                BlockVector2.at(10, 0),
                BlockVector2.at(10, 10),
                BlockVector2.at(0, 10)
        ));

        var result = measurePair(
                PolygonExpansionLoadTest::runBaseline,
                PolygonExpansionLoadTest::runPrimitive
        );
        double allocationReduction = reduction(
                result.baseline.medianAllocatedBytes, result.primitive.medianAllocatedBytes
        );
        double timeReduction = reduction(result.baseline.medianNanos, result.primitive.medianNanos);

        System.out.printf(
                "polygon-expansion-load-test vertices=%d iterations=%d baseline-median-ms=%.3f " +
                        "primitive-median-ms=%.3f baseline-median-allocated-bytes=%d " +
                        "primitive-median-allocated-bytes=%d allocation-reduction-percent=%.3f " +
                        "time-reduction-percent=%.3f%n",
                VERTEX_COUNT, ITERATIONS,
                result.baseline.medianNanos / 1_000_000d,
                result.primitive.medianNanos / 1_000_000d,
                result.baseline.medianAllocatedBytes,
                result.primitive.medianAllocatedBytes,
                allocationReduction,
                timeReduction
        );

        assertTrue(allocationReduction >= 50, "Primitive calculation must reduce allocation by at least 50%");
        assertTrue(timeReduction >= 20, "Primitive calculation must reduce median elapsed time by at least 20%");
    }

    private static void verifyOutput(ImmutableList<BlockVector2> points) {
        assertArrayEquals(expandBaseline(points), RegionRenderer.expandPolygonXZByOneForBenchmark(points));
    }

    private static void runBaseline() {
        for (int i = 0; i < ITERATIONS; i++) {
            sink = expandBaseline(POLYGON);
        }
    }

    private static void runPrimitive() {
        for (int i = 0; i < ITERATIONS; i++) {
            sink = RegionRenderer.expandPolygonXZByOneForBenchmark(POLYGON);
        }
    }

    private static Vector2d[] expandBaseline(ImmutableList<BlockVector2> points) {
        ImmutableList<BlockVector2> pointsRef = calcAreaOfPolygon(points) < 0 ? points.reverse() : points;
        List<BlockVector2> pointAdded = new ArrayList<>();

        for (int i = 0, size = pointsRef.size(); i < size; i++) {
            BlockVector2 prev = pointsRef.get((i - 1 + size) % size);
            BlockVector2 cur = pointsRef.get(i);
            BlockVector2 next = pointsRef.get((i + 1) % size);

            pointAdded.add(cur);

            if (cross(cur.subtract(prev), next.subtract(cur)) == 0 &&
                    cur.subtract(prev).dot(next.subtract(cur)) < 0) {
                pointAdded.add(cur);
            }
        }

        Vector2d[] result = new Vector2d[pointAdded.size()];

        for (int i = 0, size = pointAdded.size(); i < size; i++) {
            BlockVector2 prev = pointAdded.get((i - 1 + size) % size);
            BlockVector2 cur = pointAdded.get(i);
            BlockVector2 next = pointAdded.get((i + 1) % size);

            int xPrev = prev.x();
            int zPrev = prev.z();
            int xCur = cur.x();
            int zCur = cur.z();
            int xNext = next.x();
            int zNext = next.z();

            int xCurNew = xCur;
            int zCurNew = zCur;

            if (zPrev < zCur || zCur < zNext || cur.equals(next) && xPrev < xCur ||
                    prev.equals(cur) && xNext < xCur) {
                xCurNew++;
            }
            if (xCur < xPrev || xNext < xCur || cur.equals(next) && zPrev < zCur ||
                    prev.equals(cur) && zNext < zCur) {
                zCurNew++;
            }

            result[i] = new Vector2d(xCurNew, zCurNew);
        }

        return result;
    }

    private static double cross(BlockVector2 p1, BlockVector2 p2) {
        return (double) p1.x() * p2.z() - (double) p1.z() * p2.x();
    }

    private static double calcAreaOfPolygon(List<BlockVector2> points) {
        double area = 0;
        for (int i = 0; i < points.size(); i++) {
            area += cross(points.get(i), points.get((i + 1) % points.size()));
        }
        return area / 2.0;
    }

    private static ImmutableList<BlockVector2> createPolygon() {
        var builder = ImmutableList.<BlockVector2>builderWithExpectedSize(VERTEX_COUNT);
        for (int i = 0; i < VERTEX_COUNT; i++) {
            double angle = 2 * Math.PI * i / VERTEX_COUNT;
            builder.add(BlockVector2.at(
                    (int) Math.round(Math.cos(angle) * 100_000),
                    (int) Math.round(Math.sin(angle) * 100_000)
            ));
        }
        return builder.build();
    }

    private static MeasurementPair measurePair(Runnable baseline, Runnable primitive) {
        for (int i = 0; i < WARMUP_RUNS; i++) {
            baseline.run();
            primitive.run();
            primitive.run();
            baseline.run();
        }

        var baselineElapsed = new long[MEASURED_RUNS];
        var baselineAllocated = new long[MEASURED_RUNS];
        var primitiveElapsed = new long[MEASURED_RUNS];
        var primitiveAllocated = new long[MEASURED_RUNS];
        var threadBean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        long threadId = Thread.currentThread().threadId();

        for (int i = 0; i < MEASURED_RUNS; i++) {
            if ((i & 1) == 0) {
                measure(baseline, baselineElapsed, baselineAllocated, i, threadBean, threadId);
                measure(primitive, primitiveElapsed, primitiveAllocated, i, threadBean, threadId);
            } else {
                measure(primitive, primitiveElapsed, primitiveAllocated, i, threadBean, threadId);
                measure(baseline, baselineElapsed, baselineAllocated, i, threadBean, threadId);
            }
        }

        Arrays.sort(baselineElapsed);
        Arrays.sort(baselineAllocated);
        Arrays.sort(primitiveElapsed);
        Arrays.sort(primitiveAllocated);
        return new MeasurementPair(
                new Measurement(baselineElapsed[MEASURED_RUNS / 2], baselineAllocated[MEASURED_RUNS / 2]),
                new Measurement(primitiveElapsed[MEASURED_RUNS / 2], primitiveAllocated[MEASURED_RUNS / 2])
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

    private static double reduction(long before, long after) {
        return 100d * (before - after) / before;
    }

    private record Measurement(long medianNanos, long medianAllocatedBytes) {
    }

    private record MeasurementPair(Measurement baseline, Measurement primitive) {
    }
}
