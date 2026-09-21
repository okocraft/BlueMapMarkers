package net.okocraft.bluemapmarkers.config;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.nio.file.Path;

@ConfigSerializable
public final class Config {

    private WorldBorderSetting worldBorderSetting = new WorldBorderSetting();
    private WorldGuardSetting worldGuardSetting = new WorldGuardSetting();

    public Config() {
    }

    public Config(@NotNull WorldBorderSetting worldBorderSetting, @NotNull WorldGuardSetting worldGuardSetting) {
        this.worldBorderSetting = worldBorderSetting;
        this.worldGuardSetting = worldGuardSetting;
    }

    public @NotNull WorldBorderSetting worldBorderSetting() {
        return this.worldBorderSetting;
    }

    public @NotNull WorldGuardSetting worldGuardSetting() {
        return this.worldGuardSetting;
    }

    public static @NotNull Config loadFromYamlFile(@NotNull Path filepath) throws IOException {
        var loader = YamlConfigurationLoader.builder()
                .path(filepath)
                .build();
        var node = loader.load();
        var config = node.require(Config.class);

        try {
            config.validate();
        } catch (IllegalArgumentException e) {
            throw new ConfigurateException(node, e.getMessage());
        }

        return config;
    }

    private void validate() {
        this.worldBorderSetting.validate();
        this.worldGuardSetting.validate();
    }
}
