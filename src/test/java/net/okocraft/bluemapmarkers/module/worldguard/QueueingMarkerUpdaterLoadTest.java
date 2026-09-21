package net.okocraft.bluemapmarkers.module.worldguard;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.lang.management.ManagementFactory;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("load")
class QueueingMarkerUpdaterLoadTest {

    private static final int REGION_COUNT = 500_000;
    private static final int WARMUP_RUNS = 3;
    private static final int MEASURED_RUNS = 9;

    @Test
    void compareSnapshotTraversalWithThePreviousImplementation() {
        var sharedRegion = new ProtectedCuboidRegion(
                "region",
                BlockVector3.at(0, 0, 0),
                BlockVector3.at(1, 1, 1)
        );
        var regions = new LinkedHashMap<String, ProtectedRegion>(REGION_COUNT);
        for (int i = 0; i < REGION_COUNT; i++) {
            regions.put("region-" + i, sharedRegion);
        }

        var renderer = new NoOpRenderer();
        for (int i = 0; i < WARMUP_RUNS; i++) {
            runLegacy(regions, renderer);
            runCurrent(regions.values(), renderer);
        }

        var legacy = measure(() -> runLegacy(regions, renderer));
        var current = measure(() -> runCurrent(regions.values(), renderer));

        System.out.printf(
                "queue-load-test regions=%d legacy-median-ms=%.3f current-median-ms=%.3f " +
                        "legacy-median-allocated-bytes=%d current-median-allocated-bytes=%d%n",
                REGION_COUNT,
                legacy.medianNanos / 1_000_000d,
                current.medianNanos / 1_000_000d,
                legacy.medianAllocatedBytes,
                current.medianAllocatedBytes
        );

        assertTrue(
                current.medianAllocatedBytes * 10 < legacy.medianAllocatedBytes,
                "The current traversal must allocate at least 90% less memory than the legacy queue"
        );
        assertTrue(
                current.medianNanos <= legacy.medianNanos * 1.10,
                "The current traversal median must not regress by more than 10%"
        );
    }

    private static void runCurrent(Collection<ProtectedRegion> regions, RegionUpdateRenderer renderer) {
        var updater = new QueueingMarkerUpdater(REGION_COUNT + 1);
        updater.beginUpdate(regions);
        assertTrue(updater.doUpdate(renderer));
    }

    private static void runLegacy(Map<String, ProtectedRegion> regions, RegionUpdateRenderer renderer) {
        var queue = new ArrayDeque<>(regions.keySet());
        String id;
        while ((id = queue.poll()) != null) {
            renderer.renderRegion(regions.get(id));
        }
    }

    private static Measurement measure(Runnable operation) {
        var elapsed = new long[MEASURED_RUNS];
        var allocated = new long[MEASURED_RUNS];
        var threadBean = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        long threadId = Thread.currentThread().threadId();

        for (int i = 0; i < MEASURED_RUNS; i++) {
            long allocationBefore = threadBean.getThreadAllocatedBytes(threadId);
            long start = System.nanoTime();
            operation.run();
            elapsed[i] = System.nanoTime() - start;
            allocated[i] = threadBean.getThreadAllocatedBytes(threadId) - allocationBefore;
        }

        java.util.Arrays.sort(elapsed);
        java.util.Arrays.sort(allocated);
        return new Measurement(elapsed[MEASURED_RUNS / 2], allocated[MEASURED_RUNS / 2]);
    }

    private record Measurement(long medianNanos, long medianAllocatedBytes) {
    }

    private static final class NoOpRenderer implements RegionUpdateRenderer {

        @Override
        public RenderedRegionInfo renderRegion(@NotNull ProtectedRegion region) {
            return null;
        }

        @Override
        public void removeRegions(@NotNull Collection<RenderedRegionInfo> regions) {
        }
    }
}
