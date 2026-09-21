package net.okocraft.bluemapmarkers.config;

import org.spongepowered.configurate.serialize.ScalarSerializer;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

final class IntegerSerializer {

    static final ScalarSerializer<Integer> INSTANCE = TypeSerializer.of(
            Integer.class,
            (value, acceptedType) -> value,
            value -> {
                if (!(value instanceof Number number)) {
                    throw new SerializationException("Expected a numeric value");
                }

                long longValue = number.longValue();
                if (longValue < Integer.MIN_VALUE || longValue > Integer.MAX_VALUE) {
                    throw new SerializationException(
                            "Value " + longValue + " is out of range for an integer (["
                                    + Integer.MIN_VALUE + "," + Integer.MAX_VALUE + "])"
                    );
                }

                return (int) longValue;
            }
    );

    private IntegerSerializer() {
        throw new UnsupportedOperationException();
    }
}
