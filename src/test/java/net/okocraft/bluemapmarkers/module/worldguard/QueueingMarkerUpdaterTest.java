package net.okocraft.bluemapmarkers.module.worldguard;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

class QueueingMarkerUpdaterTest {

    @Test
    void testUpdateProcessesRegionsInBatchesAndFinishes() {
        var first = region("first");
        var second = region("second");
        var third = region("third");
        var regions = new LinkedHashMap<String, com.sk89q.worldguard.protection.regions.ProtectedRegion>();
        regions.put(first.getId(), first);
        regions.put(second.getId(), second);
        regions.put(third.getId(), third);

        var regionManager = regionManager(regions);
        var renderer = Mockito.mock(WorldGuardRenderer.class);
        var firstInfo = new RenderedRegionInfo("first-marker", 0);
        var secondInfo = new RenderedRegionInfo("second-marker", 0);
        var thirdInfo = new RenderedRegionInfo("third-marker", 0);
        Mockito.when(renderer.renderRegion(first)).thenReturn(firstInfo);
        Mockito.when(renderer.renderRegion(second)).thenReturn(secondInfo);
        Mockito.when(renderer.renderRegion(third)).thenReturn(thirdInfo);

        var updater = new QueueingMarkerUpdater(2);

        Assertions.assertFalse(updater.doUpdate(renderer, regionManager));
        Assertions.assertFalse(updater.isFinished());
        Mockito.verify(renderer, Mockito.times(2)).renderRegion(Mockito.any());
        Mockito.verify(renderer, Mockito.never()).removeRegions(Mockito.any());

        Assertions.assertTrue(updater.doUpdate(renderer, regionManager));
        Assertions.assertTrue(updater.isFinished());
        Mockito.verify(renderer, Mockito.times(3)).renderRegion(Mockito.any());
        Mockito.verify(renderer).removeRegions(Mockito.argThat(removed -> removed.isEmpty()));
    }

    @Test
    void testUpdateFinishesAndCleansUpWhenQueueEmptiesExactlyAtLimit() {
        var first = region("first");
        var second = region("second");
        var regions = new LinkedHashMap<String, com.sk89q.worldguard.protection.regions.ProtectedRegion>();
        regions.put(first.getId(), first);
        regions.put(second.getId(), second);

        var regionManager = regionManager(regions);
        var renderer = Mockito.mock(WorldGuardRenderer.class);
        Mockito.when(renderer.renderRegion(first)).thenReturn(new RenderedRegionInfo("first-marker", 0));
        Mockito.when(renderer.renderRegion(second)).thenReturn(new RenderedRegionInfo("second-marker", 0));

        var updater = new QueueingMarkerUpdater(2);

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

        var updater = new QueueingMarkerUpdater(10);
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

        var updater = new QueueingMarkerUpdater(10);

        Assertions.assertTrue(updater.doUpdate(renderer, regionManager));
        Mockito.verify(renderer).removeRegions(Mockito.argThat(removed -> removed.isEmpty()));
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
