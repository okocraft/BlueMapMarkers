package net.okocraft.bluemapmarkers.module.worldguard;

import com.sk89q.worldguard.protection.managers.RegionManager;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Queue;

class QueueingMarkerUpdater {

    private final ObjectSet<RenderedRegionInfo> renderedRegions = new ObjectOpenHashSet<>();
    private final int limit;

    private Queue<String> regionIdQueue;
    private ObjectSet<RenderedRegionInfo> removedRegions;

    QueueingMarkerUpdater(int limit) {
        this.limit = limit;
    }

    boolean doUpdate(@NotNull WorldGuardRenderer renderer, @NotNull RegionManager regionManager) {
        if (this.regionIdQueue == null || this.regionIdQueue.isEmpty()) {
            this.regionIdQueue = new ArrayDeque<>(regionManager.getRegions().keySet());
            this.removedRegions = new ObjectOpenHashSet<>(this.renderedRegions);
        }

        for (int i = 0; i < this.limit; i++) {
            var id = this.regionIdQueue.poll();

            if (id == null) {
                renderer.removeRegions(this.removedRegions);
                this.removedRegions.clear();
                return true;
            }

            var region = regionManager.getRegion(id);

            if (region != null) {
                var info = renderer.renderRegion(region);

                if (info != null) {
                    this.renderedRegions.add(info);
                    this.removedRegions.remove(info);
                }
            }
        }

        return this.regionIdQueue.isEmpty();
    }

    boolean isFinished() {
        return this.regionIdQueue == null || this.regionIdQueue.isEmpty();
    }
}
