package net.okocraft.bluemapmarkers.config;

import de.bluecolored.bluemap.api.math.Color;
import dev.siroshun.codec4j.api.codec.Codec;
import dev.siroshun.codec4j.api.decoder.Decoder;
import dev.siroshun.codec4j.api.decoder.object.FieldDecoder;
import dev.siroshun.codec4j.api.decoder.object.ObjectDecoder;

import java.util.Map;
import java.util.Set;

public record WorldGuardSetting(boolean enabled, MarkerSetSetting markerSetSetting,
                                Map<String, WorldSetting> worldSettingMap) {

    static final Decoder<WorldGuardSetting> DECODER = ObjectDecoder.create(
            WorldGuardSetting::new,
            FieldDecoder.optional("enabled", Codec.BOOLEAN, true),
            FieldDecoder.required("marker-set", MarkerSetSetting.DECODER),
            FieldDecoder.required("world-setting-map", Codec.STRING.toMapDecoderAsKey(WorldSetting.DECODER))
    );

    public record WorldSetting(boolean enabled, Set<String> disabledMaps, int updateInterval, int updateLimit,
                               RenderSetting renderSetting, SeparationSetting separationSetting) {
        static final Decoder<WorldSetting> DECODER = ObjectDecoder.create(
                WorldSetting::new,
                FieldDecoder.optional("enabled", Codec.BOOLEAN, true),
                FieldDecoder.optional("disabled-maps", Codec.STRING.toSetCodec(), Set.of()),
                FieldDecoder.optional("update-interval", Codec.INT, 10),
                FieldDecoder.optional("update-limit", Codec.INT, 50),
                FieldDecoder.required("render-setting", RenderSetting.DECODER),
                FieldDecoder.required("separation-setting", SeparationSetting.DECODER)
        );
    }

    public interface RegionColor {
        Color fillColor();

        Color outlineColor();
    }

    public record OwnedRegionColor(Color fillColor, Color outlineColor) implements RegionColor {
        static final Decoder<OwnedRegionColor> DECODER = ObjectDecoder.create(
                OwnedRegionColor::new,
                FieldDecoder.optional("fill-color", ColorCodec.COLOR_DECODER, new Color(30, 144, 255, 1)),
                FieldDecoder.optional("outline-color", ColorCodec.COLOR_DECODER, new Color(0, 191, 255, 1))
        );
    }

    public record UnownedRegionColor(Color fillColor, Color outlineColor) implements RegionColor {
        static final Decoder<UnownedRegionColor> DECODER = ObjectDecoder.create(
                UnownedRegionColor::new,
                FieldDecoder.optional("fill-color", ColorCodec.COLOR_DECODER, new Color(30, 144, 255, 1)),
                FieldDecoder.optional("outline-color", ColorCodec.COLOR_DECODER, new Color(0, 255, 0, 1))
        );
    }

    public record RenderSetting(boolean defaultRender, OwnedRegionColor ownedRegion, UnownedRegionColor unownedRegion,
                                String detailFormat, boolean render3D, float height,
                                double minDistance, double maxDistance) {

        static final Decoder<RenderSetting> DECODER = ObjectDecoder.create(
                RenderSetting::new,
                FieldDecoder.optional("default-render", Codec.BOOLEAN, true),
                FieldDecoder.required("owned-region", OwnedRegionColor.DECODER),
                FieldDecoder.required("unowned-region", UnownedRegionColor.DECODER),
                FieldDecoder.optional("detail-format", Codec.STRING, "WorldGuard (%min% ~ %max%)"),
                FieldDecoder.optional("render-3d", Codec.BOOLEAN, true),
                FieldDecoder.optional("height", Codec.FLOAT, 63f),
                FieldDecoder.optional("min-distance", Codec.DOUBLE, 0d),
                FieldDecoder.optional("max-distance", Codec.DOUBLE, 1000d)
        );

    }

    public record SeparationSetting(boolean enabled, String labelFormat, int size, int centerSize) {

        static final Decoder<SeparationSetting> DECODER = ObjectDecoder.create(
                SeparationSetting::new,
                FieldDecoder.optional("enabled", Codec.BOOLEAN, true),
                FieldDecoder.optional("label-format", Codec.STRING, "WorldGuard (%min% ~ %max%)"),
                FieldDecoder.optional("size", Codec.INT, 3),
                FieldDecoder.optional("center-size", Codec.INT, 500)
        );

    }
}
