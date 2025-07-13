package net.okocraft.bluemapmarkers.module.worldguard;

import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldguard.WorldGuard;
import de.bluecolored.bluemap.api.BlueMapAPI;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.okocraft.bluemapmarkers.BlueMapMarkersPlugin;
import net.okocraft.bluemapmarkers.config.WorldGuardSetting;
import net.okocraft.bluemapmarkers.module.MarkerModule;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class WorldGuardModule implements MarkerModule {

    private final WorldGuardSetting setting;
    private final List<ScheduledTask> scheduledTasks = new ArrayList<>();

    private BlueMapMarkersPlugin plugin;

    public WorldGuardModule(@NotNull WorldGuardSetting setting) {
        this.setting = setting;
    }

    @Override
    public void init(@NotNull BlueMapMarkersPlugin plugin) {
        this.plugin = plugin;
        PerWorldTask.logger = plugin.getSLF4JLogger();
        WorldGuardRenderer.register(WorldGuard.getInstance().getFlagRegistry());
    }

    @Override
    public void start() {
        var worldSettingMap = this.setting.worldSettingMap();

        for (var world : List.copyOf(Bukkit.getWorlds())) {
            var worldSetting = worldSettingMap.getOrDefault(world.getName(), worldSettingMap.getOrDefault(world.getKey().asString(), worldSettingMap.get("default")));

            if (worldSetting.enabled()) {
                WorldGuardRenderer renderer =
                        worldSetting.separationSetting().enabled() ?
                                new SeparatingWorldGuardRenderer(worldSetting, this.setting.markerSetSetting()) :
                                new DefaultWorldGuardRenderer(worldSetting, this.setting.markerSetSetting());

                this.scheduledTasks.add(Bukkit.getAsyncScheduler().runAtFixedRate(
                        this.plugin,
                        new PerWorldTask(world.getUID(), renderer, worldSetting),
                        1L, 1L, TimeUnit.SECONDS));
            }
        }
    }

    @Override
    public void stop() {
        this.scheduledTasks.forEach(ScheduledTask::cancel);
        this.scheduledTasks.clear();
    }

    private static class PerWorldTask implements Consumer<ScheduledTask> {

        private static Logger logger;

        private final UUID worldUid;
        private final WorldGuardRenderer renderer;
        private final WorldGuardSetting.WorldSetting setting;
        private final QueueingMarkerUpdater updater;

        private int cooldown;
        private boolean warned;

        private PerWorldTask(@NotNull UUID worldUid, @NotNull WorldGuardRenderer renderer, @NotNull WorldGuardSetting.WorldSetting setting) {
            this.worldUid = worldUid;
            this.renderer = renderer;
            this.setting = setting;
            this.updater = new QueueingMarkerUpdater(setting.updateLimit());
        }

        @Override
        public void accept(@NotNull ScheduledTask scheduledTask) {
            if (0 < --this.cooldown && this.updater.isFinished()) {
                return;
            }

            var world = Bukkit.getWorld(this.worldUid);
            var blueMapWorld = BlueMapAPI.getInstance().orElseThrow().getWorld(this.worldUid);

            if (world == null || blueMapWorld.isEmpty() || blueMapWorld.get().getMaps().isEmpty()) {
                scheduledTask.cancel();
                return;
            }

            var regionManager = WorldGuard.getInstance().getPlatform().getRegionContainer().get(new BukkitWorld(world));

            if (regionManager == null) {
                return;
            }

            if (this.updater.isFinished()) { // If the updater is completed in previous process, reset cooldown
                this.cooldown = this.setting.updateInterval();
            }

            boolean finished = this.updater.doUpdate(this.renderer, regionManager);

            for (var map : blueMapWorld.get().getMaps()) {
                if (!this.setting.disabledMaps().contains(map.getId())) {
                    this.renderer.putMarkerSets(this.worldUid, map);
                }
            }

            if (finished) {
                if (this.cooldown < 0 && !this.warned) {
                    this.warned = true;
                    logger.warn("World {} has so many protections that it cannot be updated at the specified interval!", world.getKey().asString());
                    logger.warn("Please consider increasing values of update-interval or update-limit.");
                }
            }
        }
    }
}
