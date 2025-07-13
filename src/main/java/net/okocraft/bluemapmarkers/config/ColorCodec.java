package net.okocraft.bluemapmarkers.config;

import de.bluecolored.bluemap.api.math.Color;
import dev.siroshun.codec4j.api.codec.Codec;
import dev.siroshun.codec4j.api.decoder.Decoder;
import dev.siroshun.codec4j.api.error.DecodeError;
import dev.siroshun.jfun.result.Result;

final class ColorCodec {

    static final Decoder<Color> COLOR_DECODER = Codec.STRING.flatMap(
            rgba -> {
                {
                    try {
                        return Result.success(new Color(rgba));
                    } catch (NumberFormatException e) {
                        return DecodeError.failure(e.getMessage()).asFailure();
                    }
                }
            }
    );

    private ColorCodec() {
        throw new UnsupportedOperationException();
    }
}
