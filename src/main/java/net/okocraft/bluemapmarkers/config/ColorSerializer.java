package net.okocraft.bluemapmarkers.config;

import de.bluecolored.bluemap.api.math.Color;
import org.spongepowered.configurate.serialize.ScalarSerializer;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

final class ColorSerializer {

    static final ScalarSerializer<Color> INSTANCE = TypeSerializer.of(
            Color.class,
            (color, acceptedType) -> "#%02x%02x%02x%02x".formatted(
                    color.getRed(),
                    color.getGreen(),
                    color.getBlue(),
                    Math.round(color.getAlpha() * 255)
            ),
            value -> {
                try {
                    return new Color(value.toString());
                } catch (NumberFormatException e) {
                    throw new SerializationException(e);
                }
            }
    );

    private ColorSerializer() {
        throw new UnsupportedOperationException();
    }
}
