package net.okocraft.bluemapmarkers.config;

import de.bluecolored.bluemap.api.math.Color;
import dev.siroshun.codec4j.api.codec.Codec;
import dev.siroshun.codec4j.api.decoder.Decoder;
import dev.siroshun.codec4j.api.decoder.object.ObjectDecoder;

import java.util.Map;
import java.util.Set;

public record WorldGuardSetting(boolean enabled, MarkerSetSetting markerSetSetting,
                                Map<String, WorldSetting> worldSettingMap) {

    static final Decoder<WorldGuardSetting> DECODER = ObjectDecoder.create(
            WorldGuardSetting::new,
            Codec.BOOLEAN.toOptionalFieldDecoder("enabled", true),
            MarkerSetSetting.DECODER.toRequiredFieldDecoder("marker-set"),
            Codec.STRING.toMapDecoderAsKey(WorldSetting.DECODER).toRequiredFieldDecoder("world-setting-map")
    );

    public record WorldSetting(boolean enabled, Set<String> disabledMaps, int updateInterval, int updateLimit,
                               RenderSetting renderSetting, SeparationSetting separationSetting) {
        static final Decoder<WorldSetting> DECODER = ObjectDecoder.create(
                WorldSetting::new,
                Codec.BOOLEAN.toOptionalFieldDecoder("enabled", true),
                Codec.STRING.toSetCodec().toOptionalFieldDecoder("disabled-maps", Set.of()),
                Codec.INT.toOptionalFieldDecoder("update-interval", 10),
                Codec.INT.toOptionalFieldDecoder("update-limit", 50),
                RenderSetting.DECODER.toRequiredFieldDecoder("render-setting"),
                SeparationSetting.DECODER.toRequiredFieldDecoder("separation-setting")
        );
    }

    public interface RegionColor {
        Color fillColor();

        Color outlineColor();
    }

    public record OwnedRegionColor(Color fillColor, Color outlineColor) implements RegionColor {
        static final Decoder<OwnedRegionColor> DECODER = ObjectDecoder.create(
                OwnedRegionColor::new,
                ColorCodec.COLOR_DECODER.toOptionalFieldDecoder("fill-color", new Color(30, 144, 255, 1)),
                ColorCodec.COLOR_DECODER.toOptionalFieldDecoder("outline-color", new Color(0, 191, 255, 1))
        );
    }

    public record UnownedRegionColor(Color fillColor, Color outlineColor) implements RegionColor {
        static final Decoder<UnownedRegionColor> DECODER = ObjectDecoder.create(
                UnownedRegionColor::new,
                ColorCodec.COLOR_DECODER.toOptionalFieldDecoder("fill-color", new Color(30, 144, 255, 1)),
                ColorCodec.COLOR_DECODER.toOptionalFieldDecoder("outline-color", new Color(0, 255, 0, 1))
        );
    }

    public record RenderSetting(boolean defaultRender, OwnedRegionColor ownedRegion, UnownedRegionColor unownedRegion,
                                String detailFormat, boolean render3D, float height,
                                double minDistance, double maxDistance) {

        static final Decoder<RenderSetting> DECODER = ObjectDecoder.create(
                RenderSetting::new,
                Codec.BOOLEAN.toOptionalFieldDecoder("default-render", true),
                OwnedRegionColor.DECODER.toRequiredFieldDecoder("owned-region"),
                UnownedRegionColor.DECODER.toRequiredFieldDecoder("unowned-region"),
                Codec.STRING.toOptionalFieldDecoder("detail-format", "WorldGuard (%min% ~ %max%)"),
                Codec.BOOLEAN.toOptionalFieldDecoder("render-3d", true),
                Codec.FLOAT.toOptionalFieldDecoder("height", 63f),
                Codec.DOUBLE.toOptionalFieldDecoder("min-distance", 0d),
                Codec.DOUBLE.toOptionalFieldDecoder("max-distance", 1000d)
        );

    }

    public record SeparationSetting(boolean enabled, String labelFormat, int size, int centerSize) {

        static final Decoder<SeparationSetting> DECODER = ObjectDecoder.create(
                SeparationSetting::new,
                Codec.BOOLEAN.toOptionalFieldDecoder("enabled", true),
                Codec.STRING.toOptionalFieldDecoder("label-format", "WorldGuard (%min% ~ %max%)"),
                Codec.INT.toOptionalFieldDecoder("size", 3),
                Codec.INT.toOptionalFieldDecoder("center-size", 500)
        );

    }
}
