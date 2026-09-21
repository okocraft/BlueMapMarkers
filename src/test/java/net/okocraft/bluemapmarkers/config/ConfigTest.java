package net.okocraft.bluemapmarkers.config;

import de.bluecolored.bluemap.api.math.Color;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

class ConfigTest {

    private static final String MINIMAL_CONFIG = """
            world-border-setting:
              marker-set:
                name: Border
            world-guard-setting:
              marker-set:
                name: Regions
              world-setting-map:
                default:
                  render-setting:
                    owned-region: {}
                    unowned-region: {}
                  separation-setting: {}
            """;

    @TempDir
    Path tempDir;

    @Test
    void testLoadAppliesDefaults() throws IOException {
        var result = this.load(MINIMAL_CONFIG);

        Assertions.assertFalse(result.isFailure());

        var config = result.unwrap();
        var worldBorder = config.worldBorderSetting();
        Assertions.assertTrue(worldBorder.enabled());
        Assertions.assertEquals("World Border", worldBorder.label());
        Assertions.assertEquals(new Color(255, 0, 0, 1), worldBorder.outlineColor());
        Assertions.assertEquals(63f, worldBorder.height());
        Assertions.assertEquals(15, worldBorder.updateInterval());
        Assertions.assertEquals(Set.of(), worldBorder.disabledWorlds());

        var borderMarkerSet = worldBorder.markerSetSetting();
        Assertions.assertEquals("Border", borderMarkerSet.name());
        Assertions.assertFalse(borderMarkerSet.defaultHidden());
        Assertions.assertEquals(0, borderMarkerSet.sorting());
        Assertions.assertEquals(Set.of(), borderMarkerSet.disabledMaps());

        var worldGuard = config.worldGuardSetting();
        Assertions.assertTrue(worldGuard.enabled());

        var worldSetting = worldGuard.worldSettingMap().get("default");
        Assertions.assertNotNull(worldSetting);
        Assertions.assertTrue(worldSetting.enabled());
        Assertions.assertEquals(Set.of(), worldSetting.disabledMaps());
        Assertions.assertEquals(10, worldSetting.updateInterval());
        Assertions.assertEquals(50, worldSetting.updateLimit());

        var renderSetting = worldSetting.renderSetting();
        Assertions.assertTrue(renderSetting.defaultRender());
        Assertions.assertEquals("WorldGuard (%min% ~ %max%)", renderSetting.detailFormat());
        Assertions.assertTrue(renderSetting.render3D());
        Assertions.assertEquals(63f, renderSetting.height());
        Assertions.assertEquals(0d, renderSetting.minDistance());
        Assertions.assertEquals(1000d, renderSetting.maxDistance());

        var separationSetting = worldSetting.separationSetting();
        Assertions.assertTrue(separationSetting.enabled());
        Assertions.assertEquals("WorldGuard (%min% ~ %max%)", separationSetting.labelFormat());
        Assertions.assertEquals(3, separationSetting.size());
        Assertions.assertEquals(500, separationSetting.centerSize());
    }

    @Test
    void testBundledConfigLoads() throws IOException {
        try (var input = ConfigTest.class.getResourceAsStream("/config.yml")) {
            Assertions.assertNotNull(input);

            var path = this.tempDir.resolve("bundled-config.yml");
            Files.copy(input, path);

            var result = Config.loadFromYamlFile(path);
            Assertions.assertFalse(result.isFailure(), result::toString);
        }
    }

    @Test
    void testLoadRejectsNonPositiveWorldBorderUpdateInterval() throws IOException {
        var yaml = MINIMAL_CONFIG.replace(
                "  marker-set:\n    name: Border\n",
                "  marker-set:\n    name: Border\n  update-interval: 0\n"
        );

        Assertions.assertTrue(this.load(yaml).isFailure());
    }

    @Test
    void testLoadRejectsNegativeWorldGuardUpdateInterval() throws IOException {
        var yaml = MINIMAL_CONFIG.replace(
                "    default:\n      render-setting:",
                "    default:\n      update-interval: -1\n      render-setting:"
        );

        Assertions.assertTrue(this.load(yaml).isFailure());
    }

    @Test
    void testLoadRejectsNonPositiveWorldGuardUpdateLimit() throws IOException {
        var yaml = MINIMAL_CONFIG.replace(
                "    default:\n      render-setting:",
                "    default:\n      update-limit: 0\n      render-setting:"
        );

        Assertions.assertTrue(this.load(yaml).isFailure());
    }

    @Test
    void testLoadRejectsNonPositiveSeparationSize() throws IOException {
        var yaml = MINIMAL_CONFIG.replace(
                "      separation-setting: {}",
                "      separation-setting:\n        size: 0"
        );

        Assertions.assertTrue(this.load(yaml).isFailure());
    }

    @Test
    void testLoadRejectsNegativeSeparationCenterSize() throws IOException {
        var yaml = MINIMAL_CONFIG.replace(
                "      separation-setting: {}",
                "      separation-setting:\n        center-size: -1"
        );

        Assertions.assertTrue(this.load(yaml).isFailure());
    }

    @Test
    void testLoadRejectsInvalidColor() throws IOException {
        var yaml = MINIMAL_CONFIG.replace(
                "  marker-set:\n    name: Border\n",
                "  marker-set:\n    name: Border\n  outline-color: not-a-color\n"
        );

        Assertions.assertTrue(this.load(yaml).isFailure());
    }

    private dev.siroshun.jfun.result.Result<Config, dev.siroshun.codec4j.api.error.DecodeError> load(String yaml)
            throws IOException {
        var path = this.tempDir.resolve("config.yml");
        Files.writeString(path, yaml);
        return Config.loadFromYamlFile(path);
    }
}
