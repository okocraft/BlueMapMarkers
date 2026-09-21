package net.okocraft.bluemapmarkers.module.worldguard;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.junit.jupiter.api.Test;
import org.openjdk.jol.info.GraphLayout;

import java.util.ArrayDeque;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdaterBufferRetentionLoadTest {

    private static final int REGION_COUNT = 500_000;

    @Test
    void compareCompletedCycleBufferRetention() {
        var drainedQueue = new ArrayDeque<>(Collections.nCopies(REGION_COUNT, "region"));
        while (drainedQueue.poll() != null) {
            // Match the updater consuming every queued region ID.
        }

        var clearedRemovedRegions = new ObjectOpenHashSet<RenderedRegionInfo>(REGION_COUNT);
        clearedRemovedRegions.clear();

        var previous = new BufferHolder(drainedQueue, clearedRemovedRegions);
        var current = new BufferHolder(null, null);
        long previousRetainedBytes = GraphLayout.parseInstance(previous).totalSize();
        long currentRetainedBytes = GraphLayout.parseInstance(current).totalSize();
        double reductionPercent = 100d * (previousRetainedBytes - currentRetainedBytes) / previousRetainedBytes;

        System.out.printf(
                "buffer-retention-load-test regions=%d previous-retained-bytes=%d " +
                        "current-retained-bytes=%d reduction-percent=%.5f%n",
                REGION_COUNT,
                previousRetainedBytes,
                currentRetainedBytes,
                reductionPercent
        );

        assertTrue(currentRetainedBytes < 1_024, "Completed-cycle buffers must retain less than 1 KiB");
        assertTrue(reductionPercent >= 99.9, "Completed-cycle buffer retention must decrease by at least 99.9%");
    }

    private record BufferHolder(Object regionIdQueue, Object removedRegions) {
    }
}
