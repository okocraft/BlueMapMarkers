package net.okocraft.bluemapmarkers.module.worldguard;

import com.sk89q.worldedit.bukkit.BukkitWorld;
import com.sk89q.worldguard.WorldGuard;
import de.bluecolored.bluemap.api.BlueMapAPI;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.okocraft.bluemapmarkers.BlueMapMarkersPlugin;
import net.okocraft.bluemapmarkers.config.WorldGuardSetting;
import net.okocraft.bluemapmarkers.module.MarkerModule;
import net.okocraft.bluemapmarkers.util.BlueMapWorldId;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class WorldGuardModule implements MarkerModule, Listener {

    private final WorldGuardSetting setting;
    private final ConcurrentHashMap<UUID, ScheduledTask> scheduledTasks = new ConcurrentHashMap<>();

    private BlueMapMarkersPlugin plugin;
    private volatile boolean started;

    public WorldGuardModule(@NotNull WorldGuardSetting setting) {
        this.setting = setting;
    }

    public static void registerFlags() {
        WorldGuardRenderer.register(WorldGuard.getInstance().getFlagRegistry());
    }

    @Override
    public void init(@NotNull BlueMapMarkersPlugin plugin) {
        this.plugin = plugin;
        PerWorldTask.logger = plugin.getSLF4JLogger();
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void start() {
        this.started = true;
        for (var world : List.copyOf(Bukkit.getWorlds())) {
            this.startWorld(world);
        }
    }

    @Override
    public void stop() {
        this.started = false;
        this.scheduledTasks.values().forEach(ScheduledTask::cancel);
        this.scheduledTasks.clear();
    }

    @EventHandler
    private void onWorldLoad(@NotNull WorldLoadEvent event) {
        if (this.started) {
            this.startWorld(event.getWorld());
        }
    }

    @EventHandler
    private void onWorldUnload(@NotNull WorldUnloadEvent event) {
        var task = this.scheduledTasks.remove(event.getWorld().getUID());
        if (task != null) {
            task.cancel();
        }
    }

    private void startWorld(@NotNull World world) {
        this.scheduledTasks.computeIfAbsent(world.getUID(), worldUid -> {
            var worldSettingMap = this.setting.worldSettingMap();
            var worldSetting = worldSettingMap.getOrDefault(
                    world.getName(),
                    worldSettingMap.getOrDefault(world.getKey().asString(), worldSettingMap.get("default"))
            );

            if (worldSetting == null) {
                this.plugin.getSLF4JLogger().warn(
                        "No WorldGuard marker setting is configured for world {} and no default setting is available.",
                        world.getKey().asString()
                );
                return null;
            }

            if (!worldSetting.enabled()) {
                return null;
            }

            WorldGuardRenderer renderer =
                    worldSetting.separationSetting().enabled() ?
                            new SeparatingWorldGuardRenderer(worldSetting, this.setting.markerSetSetting()) :
                            new DefaultWorldGuardRenderer(worldSetting, this.setting.markerSetSetting());

            return Bukkit.getGlobalRegionScheduler().runAtFixedRate(
                    this.plugin,
                    new PerWorldTask(worldUid, renderer, worldSetting, this.scheduledTasks),
                    20L, 20L
            );
        });
    }

    private static class PerWorldTask implements Consumer<ScheduledTask> {

        private static Logger logger;

        private final UUID worldUid;
        private final WorldGuardRenderer renderer;
        private final WorldGuardSetting.WorldSetting setting;
        private final QueueingMarkerUpdater updater;
        private final ConcurrentHashMap<UUID, ScheduledTask> scheduledTasks;

        private int cooldown;
        private boolean warned;

        private PerWorldTask(@NotNull UUID worldUid,
                             @NotNull WorldGuardRenderer renderer,
                             @NotNull WorldGuardSetting.WorldSetting setting,
                             @NotNull ConcurrentHashMap<UUID, ScheduledTask> scheduledTasks) {
            this.worldUid = worldUid;
            this.renderer = renderer;
            this.setting = setting;
            this.updater = new QueueingMarkerUpdater(setting.updateLimit());
            this.scheduledTasks = scheduledTasks;
        }

        @Override
        public void accept(@NotNull ScheduledTask scheduledTask) {
            if (0 < --this.cooldown && this.updater.isFinished()) {
                return;
            }

            var api = BlueMapAPI.getInstance().orElse(null);
            if (api == null) {
                return;
            }

            var world = Bukkit.getWorld(this.worldUid);
            if (world == null) {
                this.cancelAndForget(scheduledTask);
                return;
            }

            var blueMapWorld = api.getWorld(this.worldUid).or(() -> api.getWorld(BlueMapWorldId.create(world.getWorldPath(), world.getEnvironment())));
            if (blueMapWorld.isEmpty() || blueMapWorld.get().getMaps().isEmpty()) {
                this.cancelAndForget(scheduledTask);
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

        private void cancelAndForget(@NotNull ScheduledTask scheduledTask) {
            scheduledTask.cancel();
            this.scheduledTasks.remove(this.worldUid, scheduledTask);
        }
    }
}
