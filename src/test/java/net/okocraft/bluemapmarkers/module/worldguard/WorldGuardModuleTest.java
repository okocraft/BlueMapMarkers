package net.okocraft.bluemapmarkers.module.worldguard;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.math.Color;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.okocraft.bluemapmarkers.BlueMapMarkersPlugin;
import net.okocraft.bluemapmarkers.config.MarkerSetSetting;
import net.okocraft.bluemapmarkers.config.WorldGuardSetting;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

class WorldGuardModuleTest {

    private static final UUID WORLD_ID = UUID.fromString("00000000-0000-0000-0000-000000000031");

    @Test
    void testLifecycleRegistersListenerSchedulesWorldTaskAndCancelsIt() {
        var plugin = plugin();
        var pluginManager = Mockito.mock(PluginManager.class);
        var scheduler = Mockito.mock(GlobalRegionScheduler.class);
        var task = Mockito.mock(ScheduledTask.class);
        var world = world("world", "minecraft:overworld");
        var api = Mockito.mock(BlueMapAPI.class);
        var map = Mockito.mock(BlueMapMap.class);
        var markerSets = new HashMap<String, MarkerSet>();
        markerSets.put("WorldGuard-" + WORLD_ID, new MarkerSet("base"));
        markerSets.put("WorldGuard-" + WORLD_ID + "_1", new MarkerSet("separated"));
        markerSets.put("unrelated", new MarkerSet("keep"));
        Mockito.when(api.getMaps()).thenReturn(List.of(map));
        Mockito.when(map.getMarkerSets()).thenReturn(markerSets);

        Mockito.when(scheduler.runAtFixedRate(
                Mockito.eq(plugin),
                Mockito.any(),
                Mockito.eq(20L),
                Mockito.eq(20L)
        )).thenReturn(task);

        try (var bukkit = Mockito.mockStatic(Bukkit.class);
             var blueMap = Mockito.mockStatic(BlueMapAPI.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
            bukkit.when(Bukkit::getWorlds).thenReturn(List.of(world));
            bukkit.when(Bukkit::getGlobalRegionScheduler).thenReturn(scheduler);
            blueMap.when(BlueMapAPI::getInstance).thenReturn(Optional.of(api));

            var module = new WorldGuardModule(setting(Map.of("default", worldSetting(true))));
            module.init(plugin);
            module.start();
            module.stop();

            Mockito.verify(pluginManager).registerEvents(module, plugin);
            Mockito.verify(scheduler).runAtFixedRate(
                    Mockito.eq(plugin),
                    Mockito.any(),
                    Mockito.eq(20L),
                    Mockito.eq(20L)
            );
            Mockito.verify(task).cancel();
            Assertions.assertFalse(markerSets.containsKey("WorldGuard-" + WORLD_ID));
            Assertions.assertFalse(markerSets.containsKey("WorldGuard-" + WORLD_ID + "_1"));
            Assertions.assertTrue(markerSets.containsKey("unrelated"));
        }
    }

    @Test
    void testStartSkipsDisabledWorldSetting() {
        var plugin = plugin();
        var pluginManager = Mockito.mock(PluginManager.class);
        var scheduler = Mockito.mock(GlobalRegionScheduler.class);
        var world = world("world", "minecraft:overworld");

        try (var bukkit = Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
            bukkit.when(Bukkit::getWorlds).thenReturn(List.of(world));
            bukkit.when(Bukkit::getGlobalRegionScheduler).thenReturn(scheduler);

            var module = new WorldGuardModule(setting(Map.of("default", worldSetting(false))));
            module.init(plugin);
            module.start();

            Mockito.verify(scheduler, Mockito.never()).runAtFixedRate(
                    Mockito.any(),
                    Mockito.any(),
                    Mockito.anyLong(),
                    Mockito.anyLong()
            );
        }
    }

    @Test
    void testStartPrefersWorldNameSettingOverDefault() {
        var plugin = plugin();
        var pluginManager = Mockito.mock(PluginManager.class);
        var scheduler = Mockito.mock(GlobalRegionScheduler.class);
        var task = Mockito.mock(ScheduledTask.class);
        var world = world("world", "minecraft:overworld");

        Mockito.when(scheduler.runAtFixedRate(
                Mockito.eq(plugin),
                Mockito.any(),
                Mockito.eq(20L),
                Mockito.eq(20L)
        )).thenReturn(task);

        try (var bukkit = Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
            bukkit.when(Bukkit::getWorlds).thenReturn(List.of(world));
            bukkit.when(Bukkit::getGlobalRegionScheduler).thenReturn(scheduler);

            var module = new WorldGuardModule(setting(Map.of(
                    "default", worldSetting(false),
                    "world", worldSetting(true)
            )));
            module.init(plugin);
            module.start();

            Mockito.verify(scheduler).runAtFixedRate(
                    Mockito.eq(plugin),
                    Mockito.any(),
                    Mockito.eq(20L),
                    Mockito.eq(20L)
            );
        }
    }

    @Test
    void testStartUsesWorldKeySettingWhenNameIsNotConfigured() {
        var plugin = plugin();
        var pluginManager = Mockito.mock(PluginManager.class);
        var scheduler = Mockito.mock(GlobalRegionScheduler.class);
        var task = Mockito.mock(ScheduledTask.class);
        var world = world("world", "minecraft:overworld");

        Mockito.when(scheduler.runAtFixedRate(
                Mockito.eq(plugin),
                Mockito.any(),
                Mockito.eq(20L),
                Mockito.eq(20L)
        )).thenReturn(task);

        try (var bukkit = Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
            bukkit.when(Bukkit::getWorlds).thenReturn(List.of(world));
            bukkit.when(Bukkit::getGlobalRegionScheduler).thenReturn(scheduler);

            var module = new WorldGuardModule(setting(Map.of(
                    "default", worldSetting(false),
                    "minecraft:overworld", worldSetting(true)
            )));
            module.init(plugin);
            module.start();

            Mockito.verify(scheduler).runAtFixedRate(
                    Mockito.eq(plugin),
                    Mockito.any(),
                    Mockito.eq(20L),
                    Mockito.eq(20L)
            );
        }
    }

    @Test
    void testStartWarnsWhenWorldHasNoSettingOrDefault() {
        var logger = Mockito.mock(Logger.class);
        var plugin = Mockito.mock(BlueMapMarkersPlugin.class);
        var pluginManager = Mockito.mock(PluginManager.class);
        var scheduler = Mockito.mock(GlobalRegionScheduler.class);
        var world = world("world", "minecraft:overworld");
        Mockito.when(plugin.getSLF4JLogger()).thenReturn(logger);

        try (var bukkit = Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
            bukkit.when(Bukkit::getWorlds).thenReturn(List.of(world));
            bukkit.when(Bukkit::getGlobalRegionScheduler).thenReturn(scheduler);

            var module = new WorldGuardModule(setting(Map.of()));
            module.init(plugin);
            module.start();

            Mockito.verify(logger).warn(
                    "No WorldGuard marker setting is configured for world {} and no default setting is available.",
                    "minecraft:overworld"
            );
            Mockito.verify(scheduler, Mockito.never()).runAtFixedRate(
                    Mockito.any(),
                    Mockito.any(),
                    Mockito.anyLong(),
                    Mockito.anyLong()
            );
        }
    }

    @Test
    void testScheduledTaskRemovesMarkerSetsAndCancelsWhenWorldDisappears() {
        var plugin = plugin();
        var pluginManager = Mockito.mock(PluginManager.class);
        var scheduler = Mockito.mock(GlobalRegionScheduler.class);
        var task = Mockito.mock(ScheduledTask.class);
        var world = world("world", "minecraft:overworld");
        var api = Mockito.mock(BlueMapAPI.class);
        var map = Mockito.mock(BlueMapMap.class);
        var markerSets = new HashMap<String, MarkerSet>();
        markerSets.put("WorldGuard-" + WORLD_ID, new MarkerSet("stale"));
        markerSets.put("WorldGuard-" + WORLD_ID + "_1", new MarkerSet("stale-separated"));
        markerSets.put("unrelated", new MarkerSet("keep"));
        Mockito.when(api.getMaps()).thenReturn(List.of(map));
        Mockito.when(map.getMarkerSets()).thenReturn(markerSets);

        var tick = new AtomicReference<Consumer<ScheduledTask>>();
        Mockito.when(scheduler.runAtFixedRate(
                Mockito.eq(plugin),
                Mockito.any(),
                Mockito.eq(20L),
                Mockito.eq(20L)
        )).thenAnswer(invocation -> {
            tick.set(invocation.getArgument(1));
            return task;
        });

        try (var bukkit = Mockito.mockStatic(Bukkit.class);
             var blueMap = Mockito.mockStatic(BlueMapAPI.class)) {
            bukkit.when(Bukkit::getPluginManager).thenReturn(pluginManager);
            bukkit.when(Bukkit::getWorlds).thenReturn(List.of(world));
            bukkit.when(Bukkit::getGlobalRegionScheduler).thenReturn(scheduler);
            bukkit.when(() -> Bukkit.getWorld(WORLD_ID)).thenReturn(null);
            blueMap.when(BlueMapAPI::getInstance).thenReturn(Optional.of(api));

            var module = new WorldGuardModule(setting(Map.of("default", worldSetting(true))));
            module.init(plugin);
            module.start();

            tick.get().accept(task);
            module.stop();

            Assertions.assertFalse(markerSets.containsKey("WorldGuard-" + WORLD_ID));
            Assertions.assertFalse(markerSets.containsKey("WorldGuard-" + WORLD_ID + "_1"));
            Assertions.assertTrue(markerSets.containsKey("unrelated"));
            Mockito.verify(task, Mockito.times(1)).cancel();
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

    private static WorldGuardSetting setting(Map<String, WorldGuardSetting.WorldSetting> worldSettings) {
        return new WorldGuardSetting(
                true,
                new MarkerSetSetting("Regions", false, 0, Set.of()),
                worldSettings
        );
    }

    private static WorldGuardSetting.WorldSetting worldSetting(boolean enabled) {
        return new WorldGuardSetting.WorldSetting(
                enabled,
                Set.of(),
                10,
                50,
                new WorldGuardSetting.RenderSetting(
                        true,
                        new WorldGuardSetting.OwnedRegionColor(
                                new Color(1, 2, 3, 1),
                                new Color(4, 5, 6, 1)
                        ),
                        new WorldGuardSetting.UnownedRegionColor(
                                new Color(7, 8, 9, 1),
                                new Color(10, 11, 12, 1)
                        ),
                        "{region_id}",
                        false,
                        64f,
                        0d,
                        1000d
                ),
                new WorldGuardSetting.SeparationSetting(false, "unused", 3, 500)
        );
    }
}
