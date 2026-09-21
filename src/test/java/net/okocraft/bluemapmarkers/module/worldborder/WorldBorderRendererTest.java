package net.okocraft.bluemapmarkers.module.worldborder;

import com.flowpowered.math.vector.Vector2d;
import com.flowpowered.math.vector.Vector3d;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.api.math.Color;
import net.okocraft.bluemapmarkers.config.MarkerSetSetting;
import net.okocraft.bluemapmarkers.config.WorldBorderSetting;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Set;
import java.util.UUID;

class WorldBorderRendererTest {

    private static final UUID WORLD_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void testRenderCreatesConfiguredBorderMarker() {
        var world = world(100, 10, -20);
        var renderer = new WorldBorderRenderer(setting(), WORLD_ID);

        renderer.render(world);

        var marker = Assertions.assertInstanceOf(
                ShapeMarker.class,
                renderer.getMarkerSet().get("wb_" + WORLD_ID)
        );
        Assertions.assertEquals("Border", marker.getLabel());
        Assertions.assertEquals("Border", marker.getDetail());
        Assertions.assertEquals(new Vector3d(10, 70, -20), marker.getPosition());
        Assertions.assertEquals(70f, marker.getShapeY());
        Assertions.assertEquals(new Color(10, 20, 30, 0.5f), marker.getLineColor());
        Assertions.assertEquals(new Color(0, 0, 0, 0), marker.getFillColor());
        Assertions.assertFalse(marker.isDepthTestEnabled());
        Assertions.assertEquals(0d, marker.getMinDistance());
        Assertions.assertEquals(Double.MAX_VALUE, marker.getMaxDistance());
        Assertions.assertEquals(3, marker.getLineWidth());
        Assertions.assertArrayEquals(
                new Vector2d[]{
                        new Vector2d(60, 30),
                        new Vector2d(60, -70),
                        new Vector2d(-40, -70),
                        new Vector2d(-40, 30)
                },
                marker.getShape().getPoints()
        );
    }

    @Test
    void testRenderKeepsExistingMarkerWhenBorderIsUnchanged() {
        var world = world(100, 10, -20);
        var renderer = new WorldBorderRenderer(setting(), WORLD_ID);

        renderer.render(world);
        var first = renderer.getMarkerSet().get("wb_" + WORLD_ID);

        renderer.render(world);

        Assertions.assertSame(first, renderer.getMarkerSet().get("wb_" + WORLD_ID));
    }

    @Test
    void testRenderRecreatesMissingMarkerEvenWhenBorderIsUnchanged() {
        var world = world(100, 10, -20);
        var renderer = new WorldBorderRenderer(setting(), WORLD_ID);

        renderer.render(world);
        renderer.getMarkerSet().remove("wb_" + WORLD_ID);

        renderer.render(world);

        Assertions.assertNotNull(renderer.getMarkerSet().get("wb_" + WORLD_ID));
    }

    @Test
    void testRenderUpdatesMarkerWhenBorderChanges() {
        var world = Mockito.mock(World.class);
        var worldBorder = Mockito.mock(WorldBorder.class);
        Mockito.when(world.getUID()).thenReturn(WORLD_ID);
        Mockito.when(world.getWorldBorder()).thenReturn(worldBorder);
        Mockito.when(worldBorder.getSize()).thenReturn(100d, 40d);
        Mockito.when(worldBorder.getCenter()).thenReturn(
                new Location(null, 10, 64, -20),
                new Location(null, -5, 64, 8)
        );

        var renderer = new WorldBorderRenderer(setting(), WORLD_ID);
        renderer.render(world);
        var first = renderer.getMarkerSet().get("wb_" + WORLD_ID);

        renderer.render(world);

        var second = Assertions.assertInstanceOf(
                ShapeMarker.class,
                renderer.getMarkerSet().get("wb_" + WORLD_ID)
        );
        Assertions.assertNotSame(first, second);
        Assertions.assertEquals(new Vector3d(-5, 70, 8), second.getPosition());
        Assertions.assertArrayEquals(
                new Vector2d[]{
                        new Vector2d(15, 28),
                        new Vector2d(15, -12),
                        new Vector2d(-25, -12),
                        new Vector2d(-25, 28)
                },
                second.getShape().getPoints()
        );
    }

    private static World world(double size, double centerX, double centerZ) {
        var world = Mockito.mock(World.class);
        var worldBorder = Mockito.mock(WorldBorder.class);
        Mockito.when(world.getUID()).thenReturn(WORLD_ID);
        Mockito.when(world.getWorldBorder()).thenReturn(worldBorder);
        Mockito.when(worldBorder.getSize()).thenReturn(size);
        Mockito.when(worldBorder.getCenter()).thenReturn(new Location(null, centerX, 64, centerZ));
        return world;
    }

    private static WorldBorderSetting setting() {
        return new WorldBorderSetting(
                true,
                new MarkerSetSetting("Borders", false, 0, Set.of()),
                "Border",
                new Color(10, 20, 30, 0.5f),
                70f,
                15,
                Set.of()
        );
    }
}
