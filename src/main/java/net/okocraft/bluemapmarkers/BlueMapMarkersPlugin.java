package net.okocraft.bluemapmarkers;

import de.bluecolored.bluemap.api.BlueMapAPI;
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
import java.util.function.Consumer;

public class BlueMapMarkersPlugin extends JavaPlugin {

    private final List<MarkerModule> modules = new ArrayList<>();
    private final Consumer<BlueMapAPI> blueMapEnableListener = this::onBlueMapEnable;
    private final Consumer<BlueMapAPI> blueMapDisableListener = this::onBlueMapDisable;

    @Override
    public void onLoad() {
        if (this.getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            WorldGuardModule.registerFlags();
        }
    }

    @Override
    public void onEnable() {
        Path configFilepath = this.getDataFolder().toPath().resolve("config.yml");
        if (Files.notExists(configFilepath)) {
            try (InputStream in = this.getResource("config.yml")) {
                if (in == null) {
                    this.getSLF4JLogger().error("Could not find config.yml in the jar file");
                    return;
                }
                Files.createDirectories(configFilepath.getParent());
                Files.copy(in, configFilepath);
            } catch (IOException e) {
                this.getSLF4JLogger().error("Could not copy config.yml from the jar file", e);
                return;
            }
        }

        Config config;
        try {
            config = Config.loadFromYamlFile(configFilepath);
        } catch (IOException e) {
            this.getSLF4JLogger().error("Failed to load config.yml", e);
            return;
        }

        if (config.worldBorderSetting().enabled()) {
            this.addModule(new WorldBorderModule(config.worldBorderSetting()));
        }

        if (config.worldGuardSetting().enabled() && this.getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            this.addModule(new WorldGuardModule(config.worldGuardSetting()));
        }

        BlueMapAPI.onDisable(this.blueMapDisableListener);
        BlueMapAPI.onEnable(this.blueMapEnableListener);
    }

    @Override
    public void onDisable() {
        BlueMapAPI.unregisterListener(this.blueMapEnableListener);
        BlueMapAPI.unregisterListener(this.blueMapDisableListener);
        this.stopModules();
    }

    private void addModule(@NotNull MarkerModule module) {
        module.init(this);
        this.modules.add(module);
    }

    private void onBlueMapEnable(@NotNull BlueMapAPI api) {
        this.getServer().getGlobalRegionScheduler().execute(
                this,
                () -> this.modules.forEach(MarkerModule::start)
        );
    }

    private void onBlueMapDisable(@NotNull BlueMapAPI api) {
        this.getServer().getGlobalRegionScheduler().execute(this, this::stopModules);
    }

    private void stopModules() {
        this.modules.forEach(MarkerModule::stop);
    }
}
