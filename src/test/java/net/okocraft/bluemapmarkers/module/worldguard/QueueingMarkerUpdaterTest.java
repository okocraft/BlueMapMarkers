package net.okocraft.bluemapmarkers.module.worldguard;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueueingMarkerUpdaterTest {

    @Test
    void respectsThePerRunLimitAndCompletesAtTheExactBoundary() {
        var updater = new QueueingMarkerUpdater(2);
        var renderer = new RecordingRenderer(region -> new RenderedRegionInfo(region.getId(), 0));

        updater.beginUpdate(List.of(region("one"), region("two"), region("three"), region("four")));

        assertFalse(updater.doUpdate(renderer));
        assertEquals(List.of("one", "two"), renderer.renderedIds);
        assertTrue(updater.doUpdate(renderer));
        assertEquals(List.of("one", "two", "three", "four"), renderer.renderedIds);
        assertTrue(updater.isFinished());
    }

    @Test
    void removesRegionsThatDisappearOrMoveToAnotherMarkerSet() {
        var markerSetKey = new int[]{1};
        var updater = new QueueingMarkerUpdater(10);
        var renderer = new RecordingRenderer(region -> new RenderedRegionInfo(region.getId(), markerSetKey[0]));
        var retained = region("retained");
        var removed = region("removed");

        updater.beginUpdate(List.of(retained, removed));
        assertTrue(updater.doUpdate(renderer));

        markerSetKey[0] = 2;
        updater.beginUpdate(List.of(retained));
        assertTrue(updater.doUpdate(renderer));

        assertEquals(
                Set.of(new RenderedRegionInfo("retained", 1), new RenderedRegionInfo("removed", 1)),
                Set.copyOf(renderer.removed)
        );
    }

    @Test
    void requiresExplicitCycleBoundaries() {
        var updater = new QueueingMarkerUpdater(1);
        var renderer = new RecordingRenderer(region -> null);

        assertThrows(IllegalStateException.class, () -> updater.doUpdate(renderer));
        updater.beginUpdate(List.of(region("one"), region("two")));
        assertFalse(updater.doUpdate(renderer));
        assertThrows(IllegalStateException.class, () -> updater.beginUpdate(List.of()));
    }

    private static @NotNull ProtectedRegion region(@NotNull String id) {
        return new ProtectedCuboidRegion(id, BlockVector3.at(0, 0, 0), BlockVector3.at(1, 1, 1));
    }

    private static final class RecordingRenderer implements RegionUpdateRenderer {

        private final Function<ProtectedRegion, RenderedRegionInfo> resultFactory;
        private final List<String> renderedIds = new ArrayList<>();
        private final List<RenderedRegionInfo> removed = new ArrayList<>();

        private RecordingRenderer(Function<ProtectedRegion, RenderedRegionInfo> resultFactory) {
            this.resultFactory = resultFactory;
        }

        @Override
        public RenderedRegionInfo renderRegion(@NotNull ProtectedRegion region) {
            this.renderedIds.add(region.getId());
            return this.resultFactory.apply(region);
        }

        @Override
        public void removeRegions(@NotNull Collection<RenderedRegionInfo> regions) {
            this.removed.addAll(regions);
        }
    }
}
