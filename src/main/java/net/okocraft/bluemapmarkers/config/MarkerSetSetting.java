package net.okocraft.bluemapmarkers.config;

import de.bluecolored.bluemap.api.markers.MarkerSet;
import dev.siroshun.codec4j.api.codec.Codec;
import dev.siroshun.codec4j.api.decoder.Decoder;
import dev.siroshun.codec4j.api.decoder.object.ObjectDecoder;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public record MarkerSetSetting(String name, boolean defaultHidden, int sorting, Set<String> disabledMaps) {

    static final Decoder<MarkerSetSetting> DECODER = ObjectDecoder.create(
            MarkerSetSetting::new,
            Codec.STRING.toRequiredFieldDecoder("name"),
            Codec.BOOLEAN.toOptionalFieldDecoder("default-hidden", false),
            Codec.INT.toOptionalFieldDecoder("sorting", 0),
            Codec.STRING.toSetCodec().toOptionalFieldDecoder("disabled-maps", Set.of())
    );

    public @NotNull MarkerSet createMarkerSet() {
        return MarkerSet.builder().label(this.name).sorting(this.sorting).defaultHidden(this.defaultHidden).build();
    }
}
