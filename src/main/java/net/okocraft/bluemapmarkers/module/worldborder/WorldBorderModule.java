package net.okocraft.bluemapmarkers.module.worldborder;

import de.bluecolored.bluemap.api.BlueMapAPI;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.okocraft.bluemapmarkers.BlueMapMarkersPlugin;
import net.okocraft.bluemapmarkers.config.WorldBorderSetting;
import net.okocraft.bluemapmarkers.module.MarkerModule;
import net.okocraft.bluemapmarkers.util.BlueMapWorldId;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldUnloadEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class WorldBorderModule implements MarkerModule, Listener {

    private final WorldBorderSetting setting;
    private final Map<UUID, WorldBorderRenderer> rendererMap = new HashMap<>();

    private BlueMapMarkersPlugin plugin;
    private ScheduledTask updateTask;

    public WorldBorderModule(@NotNull WorldBorderSetting setting) {
        this.setting = setting;
    }

    @Override
    public void init(@NotNull BlueMapMarkersPlugin plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @Override
    public void start() {
        this.updateTask = Bukkit.getGlobalRegionScheduler().runAtFixedRate(
                this.plugin,
                ignored -> this.doUpdate(),
                60L,
                this.setting.updateInterval() * 20L
        );
    }

    @Override
    public void stop() {
        if (this.updateTask != null) {
            this.updateTask.cancel();
            this.updateTask = null;
        }

        var api = BlueMapAPI.getInstance().orElse(null);
        if (api != null) {
            for (var map : api.getMaps()) {
                for (var worldUid : this.rendererMap.keySet()) {
                    map.getMarkerSets().remove("WorldBorder-" + worldUid);
                }
            }
        }

        this.rendererMap.values().forEach(HandlerList::unregisterAll);
        this.rendererMap.clear();
    }

    @EventHandler
    private void onWorldUnload(@NotNull WorldUnloadEvent event) {
        var world = event.getWorld();
        var renderer = this.rendererMap.remove(world.getUID());

        if (renderer != null) {
            HandlerList.unregisterAll(renderer);
        }

        var api = BlueMapAPI.getInstance().orElse(null);
        if (api == null) {
            return;
        }

        var blueMapWorld = api.getWorld(world.getUID()).or(() ->
                api.getWorld(BlueMapWorldId.create(world.getWorldPath(), world.getEnvironment()))
        );

        blueMapWorld.ifPresent(value -> {
            var markerSetId = "WorldBorder-" + world.getUID();
            value.getMaps().forEach(map -> map.getMarkerSets().remove(markerSetId));
        });
    }

    private void doUpdate() {
        var api = BlueMapAPI.getInstance().orElse(null);

        if (api == null) {
            return;
        }

        var worlds = List.copyOf(Bukkit.getWorlds());

        for (var world : worlds) {
            if (this.setting.disabledWorlds().contains(world.getName()) || this.setting.disabledWorlds().contains(world.getKey().asString())) {
                continue;
            }

            this.doUpdate(api, world);
        }
    }

    private void doUpdate(@NotNull BlueMapAPI api, @NotNull World world) {
        var blueMapWorld = api.getWorld(world.getUID()).or(() -> api.getWorld(BlueMapWorldId.create(world.getWorldPath(), world.getEnvironment())));
        if (blueMapWorld.isEmpty()) {
            return;
        }

        var renderer = this.getRenderer(world);
        renderer.render(world);

        for (var map : blueMapWorld.get().getMaps()) {
            if (this.setting.markerSetSetting().disabledMaps().contains(map.getId())) {
                continue;
            }

            var id = "WorldBorder-" + world.getUID();
            var markerSet = renderer.getMarkerSet();

            if (map.getMarkerSets().get(id) != markerSet) {
                map.getMarkerSets().put(id, markerSet);
            }
        }
    }

    private @NotNull WorldBorderRenderer getRenderer(@NotNull World world) {
        var cached = this.rendererMap.get(world.getUID());

        if (cached != null) {
            return cached;
        }

        var newRenderer = new WorldBorderRenderer(this.setting, world.getUID());
        Bukkit.getPluginManager().registerEvents(newRenderer, this.plugin);

        this.rendererMap.put(world.getUID(), newRenderer);
        return newRenderer;
    }
}
