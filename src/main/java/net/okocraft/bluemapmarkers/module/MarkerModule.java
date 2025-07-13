package net.okocraft.bluemapmarkers.module;

import net.okocraft.bluemapmarkers.BlueMapMarkersPlugin;
import org.jetbrains.annotations.NotNull;

public interface MarkerModule {

    void init(@NotNull BlueMapMarkersPlugin plugin);

    void start();

    void stop();

}
