package net.okocraft.bluemapmarkers.config;

import de.bluecolored.bluemap.api.markers.MarkerSet;
import dev.siroshun.codec4j.api.codec.Codec;
import dev.siroshun.codec4j.api.decoder.Decoder;
import dev.siroshun.codec4j.api.decoder.object.FieldDecoder;
import dev.siroshun.codec4j.api.decoder.object.ObjectDecoder;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public record MarkerSetSetting(String name, boolean defaultHidden, int sorting, Set<String> disabledMaps) {

    static final Decoder<MarkerSetSetting> DECODER = ObjectDecoder.create(
            MarkerSetSetting::new,
            FieldDecoder.required("name", Codec.STRING),
            FieldDecoder.optional("default-hidden", Codec.BOOLEAN, false),
            FieldDecoder.optional("sorting", Codec.INT, 0),
            FieldDecoder.optional("disabled-maps", Codec.STRING.toSetCodec(), Set.of())
    );

    public @NotNull MarkerSet createMarkerSet() {
        return MarkerSet.builder().label(this.name).sorting(this.sorting).defaultHidden(this.defaultHidden).build();
    }
}
