package net.okocraft.bluemapmarkers.module.worldguard;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

class QueueingMarkerUpdaterTest {

    @Test
    void testUpdateStopsWhenTimeBudgetIsUsedUpAndFinishesLater() {
        var regionManager = regionManager(regions("first", "second", "third", "fourth"));
        long[] now = {0};
        var renderer = slowRenderer(now);
        var updater = new QueueingMarkerUpdater(() -> now[0]);

        Assertions.assertFalse(updater.doUpdate(renderer, regionManager));
        Assertions.assertFalse(updater.isFinished());
        Mockito.verify(renderer, Mockito.times(2)).renderRegion(Mockito.any());
        Mockito.verify(renderer, Mockito.never()).removeRegions(Mockito.any());

        Assertions.assertTrue(updater.doUpdate(renderer, regionManager));
        Assertions.assertTrue(updater.isFinished());
        Mockito.verify(renderer, Mockito.times(4)).renderRegion(Mockito.any());
        Mockito.verify(renderer).removeRegions(Mockito.argThat(removed -> removed.isEmpty()));
    }

    @Test
    void testUpdateFinishesWhenQueueEmptiesExactlyWhenTimeIsUp() {
        var regionManager = regionManager(regions("first", "second"));
        long[] now = {0};
        var renderer = slowRenderer(now);
        var updater = new QueueingMarkerUpdater(() -> now[0]);

        Assertions.assertTrue(updater.doUpdate(renderer, regionManager));
        Assertions.assertTrue(updater.isFinished());
        Mockito.verify(renderer).removeRegions(Mockito.argThat(removed -> removed.isEmpty()));
    }

    @Test
    void testUpdateRemovesPreviouslyRenderedRegionThatDisappeared() {
        var first = region("first");
        var second = region("second");
        var regions = new LinkedHashMap<String, com.sk89q.worldguard.protection.regions.ProtectedRegion>();
        regions.put(first.getId(), first);
        regions.put(second.getId(), second);

        var regionManager = regionManager(regions);
        var renderer = Mockito.mock(WorldGuardRenderer.class);
        var firstInfo = new RenderedRegionInfo("first-marker", 0);
        var secondInfo = new RenderedRegionInfo("second-marker", 0);
        Mockito.when(renderer.renderRegion(first)).thenReturn(firstInfo);
        Mockito.when(renderer.renderRegion(second)).thenReturn(secondInfo);

        var updater = new QueueingMarkerUpdater();
        Assertions.assertTrue(updater.doUpdate(renderer, regionManager));

        Mockito.clearInvocations(renderer);
        regions.remove(second.getId());

        Assertions.assertTrue(updater.doUpdate(renderer, regionManager));

        Mockito.verify(renderer).renderRegion(first);
        Mockito.verify(renderer).removeRegions(Mockito.argThat(removed -> removed.equals(Set.of(secondInfo))));

        Mockito.clearInvocations(renderer);

        Assertions.assertTrue(updater.doUpdate(renderer, regionManager));

        Mockito.verify(renderer).renderRegion(first);
        Mockito.verify(renderer).removeRegions(Mockito.argThat(removed -> removed.isEmpty()));
    }

    @Test
    void testUpdateDoesNotTrackRegionWhenRendererSkipsIt() {
        var region = region("hidden");
        var regions = new LinkedHashMap<String, com.sk89q.worldguard.protection.regions.ProtectedRegion>();
        regions.put(region.getId(), region);

        var regionManager = regionManager(regions);
        var renderer = Mockito.mock(WorldGuardRenderer.class);
        Mockito.when(renderer.renderRegion(region)).thenReturn(null);

        var updater = new QueueingMarkerUpdater();

        Assertions.assertTrue(updater.doUpdate(renderer, regionManager));
        Mockito.verify(renderer).removeRegions(Mockito.argThat(removed -> removed.isEmpty()));
    }

    /** Each region takes 6 ms, so the 10 ms budget is used up after two regions. */
    private static WorldGuardRenderer slowRenderer(long[] now) {
        var renderer = Mockito.mock(WorldGuardRenderer.class);
        Mockito.when(renderer.renderRegion(Mockito.any())).thenAnswer(invocation -> {
            now[0] += TimeUnit.MILLISECONDS.toNanos(6);
            return new RenderedRegionInfo(invocation.getArgument(0, ProtectedRegion.class).getId(), 0);
        });
        return renderer;
    }

    private static Map<String, ProtectedRegion> regions(String... ids) {
        var regions = new LinkedHashMap<String, ProtectedRegion>();
        for (var id : ids) {
            regions.put(id, region(id));
        }
        return regions;
    }

    private static RegionManager regionManager(
            Map<String, com.sk89q.worldguard.protection.regions.ProtectedRegion> regions
    ) {
        var regionManager = Mockito.mock(RegionManager.class);
        Mockito.when(regionManager.getRegions()).thenAnswer(ignored -> Map.copyOf(regions));
        Mockito.when(regionManager.getRegion(Mockito.anyString()))
                .thenAnswer(invocation -> regions.get(invocation.getArgument(0, String.class)));
        return regionManager;
    }

    private static ProtectedCuboidRegion region(String id) {
        return new ProtectedCuboidRegion(id, BlockVector3.ZERO, BlockVector3.at(1, 1, 1));
    }
}
