package net.okocraft.bluemapmarkers.module.worldguard;

import com.google.gson.JsonObject;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import de.bluecolored.bluemap.api.gson.MarkerGson;
import de.bluecolored.bluemap.api.math.Color;
import net.okocraft.bluemapmarkers.config.MarkerSetSetting;
import net.okocraft.bluemapmarkers.config.WorldGuardSetting;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Opt-in exporter for FV-002 (see {@code verification/README.md}).
 *
 * <p>Writes the markers produced by {@link WorldGuardRenderer#createMarker} for an enumerated set of regions, serialized
 * with BlueMap's {@link MarkerGson} exactly as in {@code markers.json}, together with the blocks WorldGuard reports as
 * contained. {@code verification/bluemap-webapp/check.mjs} then reproduces the BlueMap webapp geometry and compares.
 * Runs only when {@code BLUEMAP_EXPORT_PATH} is set.
 */
@EnabledIfEnvironmentVariable(named = "BLUEMAP_EXPORT_PATH", matches = ".+")
class BlueMapWebappExportTest {

    private static final int MIN_Y = 60;
    private static final int MAX_Y = 70;
    private static final int[][] TRANSLATIONS = {{0, 0}, {29_999_990, 29_999_990}, {-30_000_000, -30_000_000}};

    @Test
    void testExportMarkersForWebappCheck() throws IOException {
        var renderer2D = new DefaultWorldGuardRenderer(worldSetting(false), new MarkerSetSetting("Regions", false, 0, Set.of()));
        var renderer3D = new DefaultWorldGuardRenderer(worldSetting(true), new MarkerSetSetting("Regions", false, 0, Set.of()));
        int written = 0;

        try (BufferedWriter out = Files.newBufferedWriter(Path.of(System.getenv("BLUEMAP_EXPORT_PATH")))) {
            for (int[] t : TRANSLATIONS) {
                for (int x1 = -2; x1 <= 2; x1++) {
                    for (int z1 = -2; z1 <= 2; z1++) {
                        for (int x2 = -2; x2 <= 2; x2++) {
                            for (int z2 = -2; z2 <= 2; z2++) {
                                var region = new ProtectedCuboidRegion(
                                        "cuboid",
                                        BlockVector3.at(t[0] + x1, MAX_Y, t[1] + z1),
                                        BlockVector3.at(t[0] + x2, MIN_Y, t[1] + z2)
                                );
                                write(out, "cuboid", region, renderer2D, renderer3D);
                                written++;
                            }
                        }
                    }
                }

                for (List<BlockVector2> unitSteps : RegionRendererCoverageTest.enumerateRectilinearOutlines(4)) {
                    List<BlockVector2> corners = RegionRendererCoverageTest.removeStraightThroughVertices(unitSteps);
                    for (List<BlockVector2> variant : List.of(corners, unitSteps)) {
                        for (List<BlockVector2> oriented : List.of(variant, RegionRendererCoverageTest.reversed(variant))) {
                            var points = RegionRendererCoverageTest.translate(oriented, t[0], t[1]);
                            write(out, "rectilinear", polygon(points), renderer2D, renderer3D);
                            written++;
                        }
                    }
                }

                for (int length = 3; length <= 4; length++) {
                    for (int code = 0; code < (int) Math.pow(4, length); code++) {
                        var horizontal = new ArrayList<BlockVector2>();
                        var vertical = new ArrayList<BlockVector2>();
                        for (int i = 0, c = code; i < length; i++, c /= 4) {
                            horizontal.add(BlockVector2.at(t[0] + c % 4, t[1] + 7));
                            vertical.add(BlockVector2.at(t[0] + 7, t[1] + c % 4));
                        }
                        write(out, "collapsed", polygon(horizontal), renderer2D, renderer3D);
                        write(out, "collapsed", polygon(vertical), renderer2D, renderer3D);
                        written += 2;
                    }
                }

                var spike = RegionRendererCoverageTest.translate(List.of(
                        BlockVector2.at(0, 0), BlockVector2.at(2, 0), BlockVector2.at(1, 0),
                        BlockVector2.at(1, 2), BlockVector2.at(0, 2)
                ), t[0], t[1]);
                write(out, "spike", polygon(spike), renderer2D, renderer3D);
                written++;
            }
        }

        Assertions.assertTrue(written > 100_000, "unexpectedly small export: " + written);
    }

    private static void write(BufferedWriter out, String kind, ProtectedRegion region,
                              WorldGuardRenderer renderer2D, WorldGuardRenderer renderer3D) throws IOException {
        var result = RegionRenderer.render(region);
        Assertions.assertNotNull(result);

        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        int x0 = min.x() - 2;
        int z0 = min.z() - 2;
        int width = max.x() - min.x() + 5;
        int height = max.z() - min.z() + 5;
        var expected = new StringBuilder(width * height);
        for (int z = 0; z < height; z++) {
            for (int x = 0; x < width; x++) {
                expected.append(region.contains(BlockVector3.at(x0 + x, min.y(), z0 + z)) ? '1' : '0');
            }
        }

        var json = new JsonObject();
        json.addProperty("kind", kind);
        json.addProperty("input", region instanceof ProtectedPolygonalRegion ? region.getPoints().toString() : min + " .. " + max);
        json.add("shape", MarkerGson.INSTANCE.toJsonTree(renderer2D.createMarker(region, result)));
        json.add("extrude", MarkerGson.INSTANCE.toJsonTree(renderer3D.createMarker(region, result)));
        json.addProperty("x0", x0);
        json.addProperty("z0", z0);
        json.addProperty("w", width);
        json.addProperty("h", height);
        json.addProperty("minY", min.y());
        json.addProperty("maxY", max.y());
        json.addProperty("expected", expected.toString());
        out.write(json.toString());
        out.newLine();
    }

    private static ProtectedPolygonalRegion polygon(List<BlockVector2> points) {
        return new ProtectedPolygonalRegion("polygon", points, MIN_Y, MAX_Y);
    }

    private static WorldGuardSetting.WorldSetting worldSetting(boolean render3D) {
        return new WorldGuardSetting.WorldSetting(
                true,
                Set.of(),
                10,
                50,
                new WorldGuardSetting.RenderSetting(
                        true,
                        new WorldGuardSetting.OwnedRegionColor(new Color(1, 2, 3, 0.4f), new Color(4, 5, 6, 0.8f)),
                        new WorldGuardSetting.UnownedRegionColor(new Color(30, 144, 255, 1), new Color(0, 255, 0, 1)),
                        "{region_id}",
                        render3D,
                        63f,
                        0d,
                        10_000_000d
                ),
                new WorldGuardSetting.SeparationSetting(false, "unused", 3, 500)
        );
    }
}
