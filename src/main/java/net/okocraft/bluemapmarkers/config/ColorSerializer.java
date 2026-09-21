package net.okocraft.bluemapmarkers.config;

import de.bluecolored.bluemap.api.math.Color;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;

final class ColorSerializer implements TypeSerializer<Color> {

    static final ColorSerializer INSTANCE = new ColorSerializer();

    private ColorSerializer() {
    }

    @Override
    public Color deserialize(Type type, ConfigurationNode node) throws SerializationException {
        var value = node.getString();
        if (value == null) {
            return null;
        }

        try {
            return new Color(value);
        } catch (NumberFormatException e) {
            throw new SerializationException(node, type, e);
        }
    }

    @Override
    public void serialize(Type type, Color color, ConfigurationNode node) throws SerializationException {
        if (color == null) {
            node.raw(null);
            return;
        }

        int alpha = Math.round(color.getAlpha() * 255);
        node.set("#%02x%02x%02x%02x".formatted(
                color.getRed(),
                color.getGreen(),
                color.getBlue(),
                alpha
        ));
    }
}
