package net.okocraft.bluemapmarkers.config;

import dev.siroshun.codec4j.api.decoder.Decoder;
import dev.siroshun.codec4j.api.decoder.object.FieldDecoder;
import dev.siroshun.codec4j.api.decoder.object.ObjectDecoder;
import dev.siroshun.codec4j.api.error.DecodeError;
import dev.siroshun.codec4j.io.yaml.YamlIO;
import dev.siroshun.jfun.result.Result;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public record Config(@NotNull WorldBorderSetting worldBorderSetting,
                     @NotNull WorldGuardSetting worldGuardSetting) {

    private static final Decoder<Config> DECODER = ObjectDecoder.create(
            Config::new,
            FieldDecoder.required("world-border-setting", WorldBorderSetting.DECODER),
            FieldDecoder.required("world-guard-setting", WorldGuardSetting.DECODER)
    );

    public static @NotNull Result<Config, DecodeError> loadFromYamlFile(@NotNull Path filepath) {
        return YamlIO.DEFAULT.decodeFrom(filepath, DECODER);
    }
}
