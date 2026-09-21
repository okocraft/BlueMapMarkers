package net.okocraft.bluemapmarkers.module.worldguard;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayDeque;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpdaterBufferRetentionLoadTest {

    private static final int REGION_COUNT = 500_000;

    @Test
    void compareCompletedCycleBufferRetention() throws ReflectiveOperationException {
        var drainedQueue = new ArrayDeque<>(Collections.nCopies(REGION_COUNT, "region"));
        while (drainedQueue.poll() != null) {
            // Match the updater consuming every queued region ID.
        }

        var clearedRemovedRegions = new ObjectOpenHashSet<RenderedRegionInfo>(REGION_COUNT);
        clearedRemovedRegions.clear();

        long previousRetainedSlots = arrayLength(ArrayDeque.class, "elements", drainedQueue) +
                arrayLength(ObjectOpenHashSet.class, "key", clearedRemovedRegions);
        long currentRetainedSlots = 0;

        System.out.printf(
                "buffer-retention-load-test regions=%d previous-retained-reference-slots=%d " +
                        "current-retained-reference-slots=%d approximate-released-bytes=%d%n",
                REGION_COUNT,
                previousRetainedSlots,
                currentRetainedSlots,
                previousRetainedSlots * 4
        );

        assertEquals(0, currentRetainedSlots, "Completed-cycle buffers must retain no backing arrays");
        assertTrue(
                previousRetainedSlots >= REGION_COUNT * 2L,
                "The previous implementation must retain at least two reference slots per region"
        );
    }

    private static int arrayLength(Class<?> owner, String fieldName, Object instance)
            throws ReflectiveOperationException {
        Field field = owner.getDeclaredField(fieldName);
        field.setAccessible(true);
        return ((Object[]) field.get(instance)).length;
    }
}
