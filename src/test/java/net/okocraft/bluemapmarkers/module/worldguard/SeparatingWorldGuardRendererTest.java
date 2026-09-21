package net.okocraft.bluemapmarkers.module.worldguard;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.math.Color;
import net.okocraft.bluemapmarkers.config.MarkerSetSetting;
import net.okocraft.bluemapmarkers.config.WorldGuardSetting;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

class SeparatingWorldGuardRendererTest {

    private static final UUID WORLD_ID = UUID.fromString("00000000-0000-0000-0000-000000000003");

    @Test
    void testPutMarkerSetsSeparatesRegionsByDistanceBand() {
        var renderer = renderer();

        var center = renderer.renderRegion(region("center", 0, 0));
        var near = renderer.renderRegion(region("near", 11, 0));
        var far = renderer.renderRegion(region("far", -16, 0));

        Assertions.assertEquals(new RenderedRegionInfo("WorldGuard-center", 0), center);
        Assertions.assertEquals(new RenderedRegionInfo("WorldGuard-near", 1), near);
        Assertions.assertEquals(new RenderedRegionInfo("WorldGuard-far", 2), far);

        var markerSets = new HashMap<String, MarkerSet>();
        renderer.putMarkerSets(WORLD_ID, map(markerSets));

        assertMarkerSet(markerSets.get("WorldGuard-" + WORLD_ID + "_0"), "Regions 0-10", 100);
        assertMarkerSet(markerSets.get("WorldGuard-" + WORLD_ID + "_1"), "Regions 10-15", 101);
        assertMarkerSet(markerSets.get("WorldGuard-" + WORLD_ID + "_2"), "Regions 15-20", 102);
    }

    @Test
    void testPutMarkerSetsRemovesEmptyAndPreviouslyPublishedBands() {
        var renderer = renderer();
        var info = renderer.renderRegion(region("temporary", 11, 0));
        Assertions.assertNotNull(info);

        var markerSets = new HashMap<String, MarkerSet>();
        markerSets.put("WorldGuard-" + WORLD_ID + "_99", new MarkerSet("stale"));
        markerSets.put("WorldGuard-" + WORLD_ID + "_not-a-number", new MarkerSet("keep"));
        markerSets.put("unrelated", new MarkerSet("keep"));

        renderer.putMarkerSets(WORLD_ID, map(markerSets));
        Assertions.assertTrue(markerSets.containsKey("WorldGuard-" + WORLD_ID + "_1"));
        Assertions.assertFalse(markerSets.containsKey("WorldGuard-" + WORLD_ID + "_99"));
        Assertions.assertTrue(markerSets.containsKey("WorldGuard-" + WORLD_ID + "_not-a-number"));
        Assertions.assertTrue(markerSets.containsKey("unrelated"));

        renderer.removeRegions(Set.of(info));
        renderer.putMarkerSets(WORLD_ID, map(markerSets));

        Assertions.assertFalse(markerSets.containsKey("WorldGuard-" + WORLD_ID + "_1"));
        Assertions.assertTrue(markerSets.containsKey("WorldGuard-" + WORLD_ID + "_not-a-number"));
        Assertions.assertTrue(markerSets.containsKey("unrelated"));
    }

    private static SeparatingWorldGuardRenderer renderer() {
        var worldSetting = new WorldGuardSetting.WorldSetting(
                true,
                Set.of(),
                10,
                50,
                new WorldGuardSetting.RenderSetting(
                        true,
                        new WorldGuardSetting.OwnedRegionColor(
                                new Color(1, 2, 3, 1),
                                new Color(4, 5, 6, 1)
                        ),
                        new WorldGuardSetting.UnownedRegionColor(
                                new Color(7, 8, 9, 1),
                                new Color(10, 11, 12, 1)
                        ),
                        "{region_id}",
                        false,
                        64f,
                        0d,
                        1000d
                ),
                new WorldGuardSetting.SeparationSetting(
                        true,
                        "Regions %min%-%max%",
                        5,
                        10
                )
        );

        return new SeparatingWorldGuardRenderer(
                worldSetting,
                new MarkerSetSetting("Regions", true, 100, Set.of())
        );
    }

    private static ProtectedCuboidRegion region(String id, int x, int z) {
        return new ProtectedCuboidRegion(
                id,
                BlockVector3.at(x, 0, z),
                BlockVector3.at(x + 1, 10, z + 1)
        );
    }

    private static BlueMapMap map(Map<String, MarkerSet> markerSets) {
        var map = Mockito.mock(BlueMapMap.class);
        Mockito.when(map.getMarkerSets()).thenReturn(markerSets);
        return map;
    }

    private static void assertMarkerSet(MarkerSet markerSet, String label, int sorting) {
        Assertions.assertNotNull(markerSet);
        Assertions.assertEquals(label, markerSet.getLabel());
        Assertions.assertTrue(markerSet.isDefaultHidden());
        Assertions.assertEquals(sorting, markerSet.getSorting());
        Assertions.assertEquals(1, markerSet.getMarkers().size());
    }
}
