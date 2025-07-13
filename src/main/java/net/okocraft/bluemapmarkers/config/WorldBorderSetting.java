package net.okocraft.bluemapmarkers.config;

import de.bluecolored.bluemap.api.math.Color;
import dev.siroshun.codec4j.api.codec.Codec;
import dev.siroshun.codec4j.api.decoder.Decoder;
import dev.siroshun.codec4j.api.decoder.object.ObjectDecoder;

import java.util.Set;

public record WorldBorderSetting(boolean enabled, MarkerSetSetting markerSetSetting, String label, Color outlineColor,
                                 float height, int updateInterval, Set<String> disabledWorlds) {

    static final Decoder<WorldBorderSetting> DECODER = ObjectDecoder.create(
            WorldBorderSetting::new,
            Codec.BOOLEAN.toOptionalFieldDecoder("enabled", true),
            MarkerSetSetting.DECODER.toRequiredFieldDecoder("marker-set"),
            Codec.STRING.toOptionalFieldDecoder("label", "World Border"),
            ColorCodec.COLOR_DECODER.toOptionalFieldDecoder("outline-color", new Color(255, 0, 0, 1)),
            Codec.FLOAT.toOptionalFieldDecoder("height", 63f),
            Codec.INT.toOptionalFieldDecoder("update-interval", 15),
            Codec.STRING.toSetCodec().toOptionalFieldDecoder("disabled-worlds", Set.of())
    );

}
