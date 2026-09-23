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
            this.getSLF4JLogger().info("Registering WorldGuard flags...");
            WorldGuardModule.registerFlags();
        }
    }

    @Override
    public void onEnable() {
        this.getSLF4JLogger().info("Loading config.yml...");

        Path configFilepath = this.getDataFolder().toPath().resolve("config.yml");
        if (Files.notExists(configFilepath)) {
            this.getSLF4JLogger().info("Creating default config.yml...");

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

        if (config.worldBorderSetting.enabled) {
            this.getSLF4JLogger().info("Initializing WorldBorder module...");
            this.addModule(new WorldBorderModule(config.worldBorderSetting));
        } else {
            this.getSLF4JLogger().info("WorldBorder module is disabled.");
        }

        if (config.worldGuardSetting.enabled) {
            if (this.getServer().getPluginManager().getPlugin("WorldGuard") != null) {
                this.getSLF4JLogger().info("Initializing WorldGuard module...");
                this.addModule(new WorldGuardModule(config.worldGuardSetting));
            } else {
                this.getSLF4JLogger().warn("WorldGuard module is enabled, but WorldGuard is not installed. Skipping the module.");
            }
        } else {
            this.getSLF4JLogger().info("WorldGuard module is disabled.");
        }

        this.getSLF4JLogger().info("Initialized {} marker module(s).", this.modules.size());

        this.getSLF4JLogger().info("Registering BlueMap lifecycle listeners...");
        BlueMapAPI.onDisable(this.blueMapDisableListener);
        BlueMapAPI.onEnable(this.blueMapEnableListener);

        this.getSLF4JLogger().info("Successfully enabled!");
    }

    @Override
    public void onDisable() {
        this.getSLF4JLogger().info("Unregistering BlueMap lifecycle listeners...");
        BlueMapAPI.unregisterListener(this.blueMapEnableListener);
        BlueMapAPI.unregisterListener(this.blueMapDisableListener);

        this.stopModules();

        this.getSLF4JLogger().info("Successfully disabled!");
    }

    private void addModule(@NotNull MarkerModule module) {
        module.init(this);
        this.modules.add(module);
    }

    private void onBlueMapEnable(@NotNull BlueMapAPI api) {
        this.getSLF4JLogger().info("BlueMap API enabled. Starting marker modules...");
        this.getServer().getGlobalRegionScheduler().execute(this, this::startModules);
    }

    private void onBlueMapDisable(@NotNull BlueMapAPI api) {
        this.getSLF4JLogger().info("BlueMap API disabled. Stopping marker modules...");
        this.getServer().getGlobalRegionScheduler().execute(this, this::stopModules);
    }

    private void startModules() {
        this.getSLF4JLogger().info("Starting {} marker module(s)...", this.modules.size());
        this.modules.forEach(MarkerModule::start);
        this.getSLF4JLogger().info("Marker modules started.");
    }

    private void stopModules() {
        this.getSLF4JLogger().info("Stopping {} marker module(s)...", this.modules.size());
        this.modules.forEach(MarkerModule::stop);
        this.getSLF4JLogger().info("Marker modules stopped.");
    }
}
