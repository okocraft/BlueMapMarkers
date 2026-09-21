package net.okocraft.bluemapmarkers.config;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Required;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.IOException;
import java.nio.file.Path;

@ConfigSerializable
public final class Config {

    @Required
    private WorldBorderSetting worldBorderSetting;
    @Required
    private WorldGuardSetting worldGuardSetting;

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
                .defaultOptions(options -> options.serializers(serializers ->
                        serializers.registerExact(ColorSerializer.INSTANCE)
                ))
                .build();

        return loader.load().require(Config.class);
    }
}
