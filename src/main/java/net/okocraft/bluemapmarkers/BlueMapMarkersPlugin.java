package net.okocraft.bluemapmarkers;

import de.bluecolored.bluemap.api.BlueMapAPI;
import dev.siroshun.codec4j.api.error.DecodeError;
import dev.siroshun.jfun.result.Result;
import net.okocraft.bluemapmarkers.config.Config;
import net.okocraft.bluemapmarkers.module.MarkerModule;
import net.okocraft.bluemapmarkers.module.worldborder.WorldBorderModule;
import net.okocraft.bluemapmarkers.module.worldguard.WorldGuardModule;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class BlueMapMarkersPlugin extends JavaPlugin {

    private final List<MarkerModule> modules = new ArrayList<>();

    @Override
    public void onEnable() {
        Path configFilepath = this.getDataFolder().toPath().resolve("config.yml");
        if (Files.notExists(configFilepath)) {
            try (InputStream in = this.getResource("config.yml")) {
                if (in == null) {
                    this.getSLF4JLogger().error("Could not find config.yml in the jar file");
                    return;
                }
                Files.copy(in, configFilepath);
            } catch (IOException e) {
                this.getSLF4JLogger().error("Could not copy config.yml from the jar file", e);
                return;
            }
        }

        Result<Config, DecodeError> configLoadResult = Config.loadFromYamlFile(configFilepath);
        if (configLoadResult.isFailure()) {
            this.getSLF4JLogger().error("Failed to load config.yml: {}", configLoadResult.unwrapError());
            return;
        }

        Config config = configLoadResult.unwrap();

        if (config.worldBorderSetting().enabled()) {
            this.addModule(new WorldBorderModule(config.worldBorderSetting()));
        }

        if (config.worldGuardSetting().enabled() && this.getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            this.addModule(new WorldGuardModule(config.worldGuardSetting()));
        }

        BlueMapAPI.onEnable(this::onEnable);
    }

    @Override
    public void onDisable() {
        BlueMapAPI.onDisable(this::onDisable);
    }

    private void addModule(@NotNull MarkerModule module) {
        module.init(this);
        this.modules.add(module);
    }

    private void onEnable(@NotNull BlueMapAPI api) {
        this.modules.forEach(MarkerModule::start);
    }

    private void onDisable(@NotNull BlueMapAPI api) {
        this.modules.forEach(MarkerModule::stop);
    }
}
