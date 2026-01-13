package net.okocraft.bluemapmarkers.config;

import de.bluecolored.bluemap.api.math.Color;
import dev.siroshun.codec4j.api.codec.Codec;
import dev.siroshun.codec4j.api.decoder.Decoder;
import dev.siroshun.codec4j.api.decoder.collection.SetDecoder;
import dev.siroshun.codec4j.api.decoder.object.FieldDecoder;
import dev.siroshun.codec4j.api.decoder.object.ObjectDecoder;

import java.util.Set;

public record WorldBorderSetting(boolean enabled, MarkerSetSetting markerSetSetting, String label, Color outlineColor,
                                 float height, int updateInterval, Set<String> disabledWorlds) {

    static final Decoder<WorldBorderSetting> DECODER = ObjectDecoder.create(
            WorldBorderSetting::new,
            FieldDecoder.optional("enabled", Codec.BOOLEAN, true),
            FieldDecoder.required("marker-set", MarkerSetSetting.DECODER),
            FieldDecoder.optional("label", Codec.STRING, "World Border"),
            FieldDecoder.optional("outline-color", ColorCodec.COLOR_DECODER, new Color(255, 0, 0, 1)),
            FieldDecoder.optional("height", Codec.FLOAT, 63f),
            FieldDecoder.optional("update-interval", Codec.INT, 15),
            FieldDecoder.optional("disabled-worlds", SetDecoder.create(Codec.STRING), Set.of())
    );

}
