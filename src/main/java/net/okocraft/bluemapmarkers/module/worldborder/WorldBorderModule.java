package net.okocraft.bluemapmarkers.module.worldborder;

import de.bluecolored.bluemap.api.BlueMapAPI;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.okocraft.bluemapmarkers.BlueMapMarkersPlugin;
import net.okocraft.bluemapmarkers.config.WorldBorderSetting;
import net.okocraft.bluemapmarkers.module.MarkerModule;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class WorldBorderModule implements MarkerModule {

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
    }

    @Override
    public void start() {
        this.updateTask = Bukkit.getAsyncScheduler().runAtFixedRate(
                this.plugin,
                ignored -> this.doUpdate(),
                3,
                this.setting.updateInterval(),
                TimeUnit.SECONDS
        );
    }

    @Override
    public void stop() {
        if (this.updateTask != null) {
            this.updateTask.cancel();
            this.rendererMap.clear();
            this.updateTask = null;
        }
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
        var blueMapWorld = api.getWorld(world.getUID());

        if (blueMapWorld.isEmpty()) {
            return;
        }

        var renderer = this.getRenderer(world);

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

        var newRenderer = new WorldBorderRenderer(this.setting);
        newRenderer.render(world);
        Bukkit.getPluginManager().registerEvents(newRenderer, this.plugin);

        this.rendererMap.put(world.getUID(), newRenderer);
        return newRenderer;
    }
}
