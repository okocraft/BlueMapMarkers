package net.okocraft.bluemapmarkers.util;

import org.bukkit.World;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Path;
import java.util.stream.Stream;

class BlueMapWorldIdTest {

    @ParameterizedTest
    @MethodSource("environments")
    void testCreateUsesExpectedDimension(World.Environment environment, String dimension) {
        var relativePath = Path.of("worlds", environment.name().toLowerCase());
        var worldPath = Path.of("").toAbsolutePath().resolve(relativePath);

        var result = BlueMapWorldId.create(worldPath, environment);

        Assertions.assertEquals(relativePath + "#" + dimension, result);
    }

    @ParameterizedTest
    @MethodSource("environments")
    void testCreateNormalizesPath(World.Environment environment, String dimension) {
        var relativePath = Path.of("worlds", environment.name().toLowerCase());
        var worldPath = Path.of("").toAbsolutePath()
                .resolve("ignored")
                .resolve("..")
                .resolve(relativePath);

        var result = BlueMapWorldId.create(worldPath, environment);

        Assertions.assertEquals(relativePath + "#" + dimension, result);
    }

    private static Stream<Arguments> environments() {
        return Stream.of(
                Arguments.of(World.Environment.NORMAL, "minecraft:overworld"),
                Arguments.of(World.Environment.NETHER, "minecraft:the_nether"),
                Arguments.of(World.Environment.THE_END, "minecraft:the_end"),
                Arguments.of(World.Environment.CUSTOM, "")
        );
    }
}
