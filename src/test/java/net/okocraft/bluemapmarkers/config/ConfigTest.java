package net.okocraft.bluemapmarkers.config;

import de.bluecolored.bluemap.api.math.Color;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.spongepowered.configurate.ConfigurateException;

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
        var config = this.load(MINIMAL_CONFIG);

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
        Assertions.assertEquals(new Color(30, 144, 255, 1), renderSetting.ownedRegion().fillColor());
        Assertions.assertEquals(new Color(0, 191, 255, 1), renderSetting.ownedRegion().outlineColor());
        Assertions.assertEquals(new Color(30, 144, 255, 1), renderSetting.unownedRegion().fillColor());
        Assertions.assertEquals(new Color(0, 255, 0, 1), renderSetting.unownedRegion().outlineColor());
        Assertions.assertEquals("""
                <h2 style="color:#00bfff;text-align:center;margin-block-end:0.3em">{region_displayname}</h2>
                <br/>
                <span style="font-size:100%;">Owners: </span><span style="font-weight:bold;">{region_owners}</span><br/>
                <span style="font-size:100%;">Members: </span><span style="font-weight:bold;">{region_members}</span><br/>
                """.strip(), renderSetting.detailFormat());
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

            var config = Assertions.assertDoesNotThrow(() -> Config.loadFromYamlFile(path));
            Assertions.assertEquals(new Color("#ff0000ff"), config.worldBorderSetting().outlineColor());

            var renderSetting = config.worldGuardSetting().worldSettingMap().get("default").renderSetting();
            Assertions.assertEquals(new Color("#1e90ff1a"), renderSetting.ownedRegion().fillColor());
            Assertions.assertEquals(new Color("#00ff004d"), renderSetting.unownedRegion().outlineColor());
        }
    }

    @Test
    void testLoadRejectsMissingTopLevelSection() {
        var yaml = MINIMAL_CONFIG.replace("""
                world-border-setting:
                  marker-set:
                    name: Border
                """, "");

        Assertions.assertThrows(ConfigurateException.class, () -> this.load(yaml));
    }

    @Test
    void testLoadRejectsMissingMarkerSetName() {
        var yaml = MINIMAL_CONFIG.replace("    name: Border\n", "");

        Assertions.assertThrows(ConfigurateException.class, () -> this.load(yaml));
    }

    @Test
    void testLoadRejectsMissingRenderSetting() {
        var yaml = MINIMAL_CONFIG.replace("""
                      render-setting:
                        owned-region: {}
                        unowned-region: {}
                """, "");

        Assertions.assertThrows(ConfigurateException.class, () -> this.load(yaml));
    }

    @Test
    void testLoadRejectsMissingOwnedRegion() {
        var yaml = MINIMAL_CONFIG.replace("        owned-region: {}\n", "");

        Assertions.assertThrows(ConfigurateException.class, () -> this.load(yaml));
    }

    @Test
    void testLoadRejectsMissingSeparationSetting() {
        var yaml = MINIMAL_CONFIG.replace("      separation-setting: {}\n", "");

        Assertions.assertThrows(ConfigurateException.class, () -> this.load(yaml));
    }

    @Test
    void testLoadRejectsNonPositiveWorldBorderUpdateInterval() {
        var yaml = MINIMAL_CONFIG.replace(
                "  marker-set:\n    name: Border\n",
                "  marker-set:\n    name: Border\n  update-interval: 0\n"
        );

        Assertions.assertThrows(ConfigurateException.class, () -> this.load(yaml));
    }

    @Test
    void testLoadRejectsNegativeWorldGuardUpdateInterval() {
        var yaml = MINIMAL_CONFIG.replace(
                "    default:\n      render-setting:",
                "    default:\n      update-interval: -1\n      render-setting:"
        );

        Assertions.assertThrows(ConfigurateException.class, () -> this.load(yaml));
    }

    @Test
    void testLoadRejectsNonPositiveWorldGuardUpdateLimit() {
        var yaml = MINIMAL_CONFIG.replace(
                "    default:\n      render-setting:",
                "    default:\n      update-limit: 0\n      render-setting:"
        );

        Assertions.assertThrows(ConfigurateException.class, () -> this.load(yaml));
    }

    @Test
    void testLoadRejectsNonPositiveSeparationSize() {
        var yaml = MINIMAL_CONFIG.replace(
                "      separation-setting: {}",
                "      separation-setting:\n        size: 0"
        );

        Assertions.assertThrows(ConfigurateException.class, () -> this.load(yaml));
    }

    @Test
    void testLoadRejectsNegativeSeparationCenterSize() {
        var yaml = MINIMAL_CONFIG.replace(
                "      separation-setting: {}",
                "      separation-setting:\n        center-size: -1"
        );

        Assertions.assertThrows(ConfigurateException.class, () -> this.load(yaml));
    }

    @Test
    void testLoadRejectsInvalidColor() {
        var yaml = MINIMAL_CONFIG.replace(
                "  marker-set:\n    name: Border\n",
                "  marker-set:\n    name: Border\n  outline-color: not-a-color\n"
        );

        Assertions.assertThrows(ConfigurateException.class, () -> this.load(yaml));
    }

    @Test
    void testLoadRejectsMappingColor() {
        var yaml = MINIMAL_CONFIG.replace(
                "  marker-set:\n    name: Border\n",
                "  marker-set:\n    name: Border\n  outline-color: { invalid: value }\n"
        );

        Assertions.assertThrows(ConfigurateException.class, () -> this.load(yaml));
    }

    private Config load(String yaml) throws IOException {
        var path = this.tempDir.resolve("config.yml");
        Files.writeString(path, yaml);
        return Config.loadFromYamlFile(path);
    }
}
