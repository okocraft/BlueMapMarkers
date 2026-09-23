package net.okocraft.bluemapmarkers.module.worldborder;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.BlueMapWorld;
import de.bluecolored.bluemap.api.math.Color;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.okocraft.bluemapmarkers.BlueMapMarkersPlugin;
import net.okocraft.bluemapmarkers.config.MarkerSetSetting;
import net.okocraft.bluemapmarkers.config.WorldBorderSetting;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.WorldBorder;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

class WorldBorderModuleTest {

    private static final UUID WORLD_ID = UUID.fromString("00000000-0000-0000-0000-000000000041");

    @Test
    void testLifecycleRegistersListenerSchedulesUpdatesAndCancelsTask() {
        var plugin = plugin();
        var pluginManager = Mockito.mock(PluginManager.class);
        var scheduler = Mockito.mock(GlobalRegionScheduler.class);
        var task = Mockito.mock(ScheduledTask.class);
        Mockito.when(scheduler.runAtFixedRate(
                Mockito.eq(plugin),
                Mockito.any(),
                Mockito.eq(60L),
                Mockito.eq(300L)
        )).thenReturn(task);

        try (var bukkit = Mockito.mockStatic(Bukkit.class);
             var blueMap = Mockito.mockStatic(BlueMapAPI.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
            bukkit.when(Bukkit::getGlobalRegionScheduler).thenReturn(scheduler);
            blueMap.when(BlueMapAPI::getInstance).thenReturn(Optional.empty());

            var module = new WorldBorderModule(setting(Set.of(), Set.of()));
            module.init(plugin);
            module.start();
            module.stop();

            Mockito.verify(pluginManager).registerEvents(module, plugin);
            Mockito.verify(scheduler).runAtFixedRate(
                    Mockito.eq(plugin),
                    Mockito.any(),
                    Mockito.eq(60L),
                    Mockito.eq(300L)
            );
            Mockito.verify(task).cancel();
        }
    }

    @Test
    void testUpdateSkipsDisabledWorld() {
        var plugin = plugin();
        var pluginManager = Mockito.mock(PluginManager.class);
        var scheduler = Mockito.mock(GlobalRegionScheduler.class);
        var task = Mockito.mock(ScheduledTask.class);
        var world = world("disabled", "minecraft:disabled");
        var api = Mockito.mock(BlueMapAPI.class);
        var update = new AtomicReference<Consumer<ScheduledTask>>();

        Mockito.when(scheduler.runAtFixedRate(
                Mockito.eq(plugin),
                Mockito.any(),
                Mockito.eq(60L),
                Mockito.eq(300L)
        )).thenAnswer(invocation -> {
            update.set(invocation.getArgument(1));
            return task;
        });

        try (var bukkit = Mockito.mockStatic(Bukkit.class);
             var blueMap = Mockito.mockStatic(BlueMapAPI.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
            bukkit.when(Bukkit::getGlobalRegionScheduler).thenReturn(scheduler);
            bukkit.when(Bukkit::getWorlds).thenReturn(List.of(world));
            blueMap.when(BlueMapAPI::getInstance).thenReturn(Optional.of(api));

            var module = new WorldBorderModule(setting(Set.of(), Set.of("disabled")));
            module.init(plugin);
            module.start();

            update.get().accept(task);

            Mockito.verify(api, Mockito.never()).getWorld(Mockito.any());
        }
    }

    @Test
    void testUpdatePublishesMarkerSetOnlyToEnabledMaps() {
        var plugin = plugin();
        var pluginManager = Mockito.mock(PluginManager.class);
        var scheduler = Mockito.mock(GlobalRegionScheduler.class);
        var task = Mockito.mock(ScheduledTask.class);
        var world = world("world", "minecraft:overworld");
        var worldBorder = Mockito.mock(WorldBorder.class);
        Mockito.when(world.getWorldBorder()).thenReturn(worldBorder);
        Mockito.when(worldBorder.getSize()).thenReturn(100d);
        Mockito.when(worldBorder.getCenter()).thenReturn(new Location(null, 10, 64, -20));

        var api = Mockito.mock(BlueMapAPI.class);
        var blueMapWorld = Mockito.mock(BlueMapWorld.class);
        var enabledMap = Mockito.mock(BlueMapMap.class);
        var disabledMap = Mockito.mock(BlueMapMap.class);
        var enabledMarkerSets = new HashMap<String, de.bluecolored.bluemap.api.markers.MarkerSet>();
        var disabledMarkerSets = new HashMap<String, de.bluecolored.bluemap.api.markers.MarkerSet>();
        Mockito.when(api.getWorld(WORLD_ID)).thenReturn(Optional.of(blueMapWorld));
        Mockito.when(api.getMaps()).thenReturn(List.of(enabledMap, disabledMap));
        Mockito.when(blueMapWorld.getMaps()).thenReturn(List.of(enabledMap, disabledMap));
        Mockito.when(enabledMap.getId()).thenReturn("enabled-map");
        Mockito.when(disabledMap.getId()).thenReturn("disabled-map");
        Mockito.when(enabledMap.getMarkerSets()).thenReturn(enabledMarkerSets);
        Mockito.when(disabledMap.getMarkerSets()).thenReturn(disabledMarkerSets);

        var update = new AtomicReference<Consumer<ScheduledTask>>();
        Mockito.when(scheduler.runAtFixedRate(
                Mockito.eq(plugin),
                Mockito.any(),
                Mockito.eq(60L),
                Mockito.eq(300L)
        )).thenAnswer(invocation -> {
            update.set(invocation.getArgument(1));
            return task;
        });

        try (var bukkit = Mockito.mockStatic(Bukkit.class);
             var blueMap = Mockito.mockStatic(BlueMapAPI.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
            bukkit.when(Bukkit::getGlobalRegionScheduler).thenReturn(scheduler);
            bukkit.when(Bukkit::getWorlds).thenReturn(List.of(world));
            blueMap.when(BlueMapAPI::getInstance).thenReturn(Optional.of(api));

            var module = new WorldBorderModule(setting(Set.of("disabled-map"), Set.of()));
            module.init(plugin);
            module.start();

            update.get().accept(task);

            Assertions.assertTrue(enabledMarkerSets.containsKey("WorldBorder-" + WORLD_ID));
            Assertions.assertFalse(disabledMarkerSets.containsKey("WorldBorder-" + WORLD_ID));

            enabledMarkerSets.put("unrelated", new de.bluecolored.bluemap.api.markers.MarkerSet("keep"));

            module.stop();

            Assertions.assertFalse(enabledMarkerSets.containsKey("WorldBorder-" + WORLD_ID));
            Assertions.assertTrue(enabledMarkerSets.containsKey("unrelated"));
        }
    }

    private static BlueMapMarkersPlugin plugin() {
        var plugin = Mockito.mock(BlueMapMarkersPlugin.class);
        Mockito.when(plugin.getSLF4JLogger()).thenReturn(Mockito.mock(Logger.class));
        return plugin;
    }

    private static World world(String name, String keyValue) {
        var world = Mockito.mock(World.class);
        var key = Mockito.mock(NamespacedKey.class);
        Mockito.when(world.getUID()).thenReturn(WORLD_ID);
        Mockito.when(world.getName()).thenReturn(name);
        Mockito.when(world.getKey()).thenReturn(key);
        Mockito.when(key.asString()).thenReturn(keyValue);
        return world;
    }

    private static WorldBorderSetting setting(Set<String> disabledMaps, Set<String> disabledWorlds) {
        return new WorldBorderSetting(
                true,
                new MarkerSetSetting("Borders", false, 0, disabledMaps),
                "Border",
                new Color(255, 0, 0, 1),
                64f,
                15,
                disabledWorlds
        );
    }
}
