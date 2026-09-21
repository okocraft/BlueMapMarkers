package net.okocraft.bluemapmarkers.module.worldguard;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StringFlag;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.markers.ExtrudeMarker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
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

class DefaultWorldGuardRendererTest {

    private static final UUID WORLD_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Test
    void testRenderRegionPublishesConfigured2DMarker() {
        var renderer = new DefaultWorldGuardRenderer(
                worldSetting(true, false, "{region_displayname}"),
                new MarkerSetSetting("Regions", true, 7, Set.of())
        );
        var region = region("spawn");
        region.setFlag(WorldGuardRenderer.DISPLAY_FLAG, "<Spawn>");

        var info = renderer.renderRegion(region);

        Assertions.assertEquals(new RenderedRegionInfo("WorldGuard-spawn", 0), info);

        var markerSets = publish(renderer);
        var markerSet = markerSets.get("WorldGuard-" + WORLD_ID);
        Assertions.assertNotNull(markerSet);
        Assertions.assertEquals("Regions", markerSet.getLabel());
        Assertions.assertTrue(markerSet.isDefaultHidden());
        Assertions.assertEquals(7, markerSet.getSorting());

        var marker = Assertions.assertInstanceOf(
                ShapeMarker.class,
                markerSet.get("WorldGuard-spawn")
        );
        Assertions.assertEquals("spawn", marker.getLabel());
        Assertions.assertEquals("&lt;Spawn&gt;", marker.getDetail());
        Assertions.assertEquals(new Color(30, 144, 255, 1), marker.getFillColor());
        Assertions.assertEquals(new Color(0, 255, 0, 1), marker.getLineColor());
        Assertions.assertFalse(marker.isDepthTestEnabled());
        Assertions.assertEquals(64f, marker.getShapeY());
        Assertions.assertEquals(5d, marker.getMinDistance());
        Assertions.assertEquals(500d, marker.getMaxDistance());
    }

    @Test
    void testRenderRegionUsesOwnedColorsAndValidOverrides() {
        var renderer = new DefaultWorldGuardRenderer(
                worldSetting(true, false, "{region_id}"),
                new MarkerSetSetting("Regions", false, 0, Set.of())
        );
        var region = region("owned");
        region.getOwners().addPlayer(UUID.fromString("00000000-0000-0000-0000-000000000010"));
        region.setFlag(stringFlag("bluemap-color"), "#11223380");
        region.setFlag(stringFlag("bluemap-outline-color"), "#44556640");

        renderer.renderRegion(region);

        var marker = Assertions.assertInstanceOf(
                ShapeMarker.class,
                publish(renderer).get("WorldGuard-" + WORLD_ID).get("WorldGuard-owned")
        );
        Assertions.assertEquals(new Color("#11223380"), marker.getFillColor());
        Assertions.assertEquals(new Color("#44556640"), marker.getLineColor());
    }

    @Test
    void testRenderRegionFallsBackToConfiguredColorForInvalidOverride() {
        var renderer = new DefaultWorldGuardRenderer(
                worldSetting(true, false, "{region_id}"),
                new MarkerSetSetting("Regions", false, 0, Set.of())
        );
        var region = region("invalid-color");
        region.setFlag(stringFlag("bluemap-color"), "invalid");

        renderer.renderRegion(region);

        var marker = Assertions.assertInstanceOf(
                ShapeMarker.class,
                publish(renderer).get("WorldGuard-" + WORLD_ID).get("WorldGuard-invalid-color")
        );
        Assertions.assertEquals(new Color(30, 144, 255, 1), marker.getFillColor());
    }

    @Test
    void testRenderRegionHonorsRenderFlagWhenDefaultRenderIsDisabled() {
        var renderer = new DefaultWorldGuardRenderer(
                worldSetting(false, false, "{region_id}"),
                new MarkerSetSetting("Regions", false, 0, Set.of())
        );
        var region = region("conditional");

        Assertions.assertNull(renderer.renderRegion(region));

        region.setFlag(stateFlag("render-on-bluemap"), StateFlag.State.ALLOW);
        Assertions.assertNotNull(renderer.renderRegion(region));

        region.setFlag(stateFlag("render-on-bluemap"), StateFlag.State.DENY);
        Assertions.assertNull(renderer.renderRegion(region));
    }

    @Test
    void testRenderRegionCreatesExtrudeMarkerWhen3DIsEnabled() {
        var renderer = new DefaultWorldGuardRenderer(
                worldSetting(true, true, "{region_id}"),
                new MarkerSetSetting("Regions", false, 0, Set.of())
        );

        renderer.renderRegion(region("three-d"));

        Assertions.assertInstanceOf(
                ExtrudeMarker.class,
                publish(renderer).get("WorldGuard-" + WORLD_ID).get("WorldGuard-three-d")
        );
    }

    @Test
    void testRemoveRegionsRemovesPublishedMarker() {
        var renderer = new DefaultWorldGuardRenderer(
                worldSetting(true, false, "{region_id}"),
                new MarkerSetSetting("Regions", false, 0, Set.of())
        );
        var info = renderer.renderRegion(region("temporary"));
        Assertions.assertNotNull(info);

        renderer.removeRegions(Set.of(info));

        Assertions.assertNull(
                publish(renderer).get("WorldGuard-" + WORLD_ID).get("WorldGuard-temporary")
        );
    }

    private static Map<String, MarkerSet> publish(DefaultWorldGuardRenderer renderer) {
        var markerSets = new HashMap<String, MarkerSet>();
        var map = Mockito.mock(BlueMapMap.class);
        Mockito.when(map.getMarkerSets()).thenReturn(markerSets);
        renderer.putMarkerSets(WORLD_ID, map);
        return markerSets;
    }

    private static ProtectedCuboidRegion region(String id) {
        return new ProtectedCuboidRegion(
                id,
                BlockVector3.at(0, 10, 0),
                BlockVector3.at(4, 30, 4)
        );
    }

    private static StateFlag stateFlag(String name) {
        return Assertions.assertInstanceOf(
                StateFlag.class,
                WorldGuard.getInstance().getFlagRegistry().get(name)
        );
    }

    private static StringFlag stringFlag(String name) {
        return Assertions.assertInstanceOf(
                StringFlag.class,
                WorldGuard.getInstance().getFlagRegistry().get(name)
        );
    }

    private static WorldGuardSetting.WorldSetting worldSetting(
            boolean defaultRender,
            boolean render3D,
            String detailFormat
    ) {
        return new WorldGuardSetting.WorldSetting(
                true,
                Set.of(),
                10,
                50,
                new WorldGuardSetting.RenderSetting(
                        defaultRender,
                        new WorldGuardSetting.OwnedRegionColor(
                                new Color(1, 2, 3, 0.4f),
                                new Color(4, 5, 6, 0.8f)
                        ),
                        new WorldGuardSetting.UnownedRegionColor(
                                new Color(30, 144, 255, 1),
                                new Color(0, 255, 0, 1)
                        ),
                        detailFormat,
                        render3D,
                        64f,
                        5d,
                        500d
                ),
                new WorldGuardSetting.SeparationSetting(false, "unused", 3, 500)
        );
    }
}
