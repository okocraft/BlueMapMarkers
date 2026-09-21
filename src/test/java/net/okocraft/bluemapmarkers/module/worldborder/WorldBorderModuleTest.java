package net.okocraft.bluemapmarkers.module.worldborder;

import de.bluecolored.bluemap.api.BlueMapAPI;
import de.bluecolored.bluemap.api.math.Color;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.okocraft.bluemapmarkers.BlueMapMarkersPlugin;
import net.okocraft.bluemapmarkers.config.MarkerSetSetting;
import net.okocraft.bluemapmarkers.config.WorldBorderSetting;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.Set;

class WorldBorderModuleTest {

    @Test
    void testLifecycleRegistersListenerSchedulesUpdatesAndCancelsTask() {
        var plugin = Mockito.mock(BlueMapMarkersPlugin.class);
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

            var module = new WorldBorderModule(setting());
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

    private static WorldBorderSetting setting() {
        return new WorldBorderSetting(
                true,
                new MarkerSetSetting("Borders", false, 0, Set.of()),
                "Border",
                new Color(255, 0, 0, 1),
                64f,
                15,
                Set.of()
        );
    }
}
