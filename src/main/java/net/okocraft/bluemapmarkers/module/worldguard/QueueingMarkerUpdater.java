package net.okocraft.bluemapmarkers.module.worldguard;

import com.sk89q.worldguard.protection.managers.RegionManager;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

class QueueingMarkerUpdater {

    /** Time one update may spend on the main thread; the remaining regions are left for the next update. */
    static final long TIME_BUDGET_NANOS = TimeUnit.MILLISECONDS.toNanos(10);

    private final ObjectSet<RenderedRegionInfo> renderedRegions = new ObjectOpenHashSet<>();
    private final int limit;
    private final LongSupplier nanoTime;

    private Queue<String> regionIdQueue;
    private ObjectSet<RenderedRegionInfo> removedRegions;

    QueueingMarkerUpdater(int limit) {
        this(limit, System::nanoTime);
    }

    QueueingMarkerUpdater(int limit, @NotNull LongSupplier nanoTime) {
        this.limit = limit;
        this.nanoTime = nanoTime;
    }

    boolean doUpdate(@NotNull WorldGuardRenderer renderer, @NotNull RegionManager regionManager) {
        if (this.regionIdQueue == null || this.regionIdQueue.isEmpty()) {
            this.regionIdQueue = new ArrayDeque<>(regionManager.getRegions().keySet());
            this.removedRegions = new ObjectOpenHashSet<>(this.renderedRegions);
        }

        long start = this.nanoTime.getAsLong();
        for (int i = 0; i < this.limit; i++) {
            // Rendering a region for the first time can take tens of milliseconds, so also stop when time is up.
            if (i > 0 && this.nanoTime.getAsLong() - start >= TIME_BUDGET_NANOS) {
                break;
            }

            var id = this.regionIdQueue.poll();

            if (id == null) {
                this.finishUpdate(renderer);
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

        if (this.regionIdQueue.isEmpty()) {
            this.finishUpdate(renderer);
            return true;
        }

        return false;
    }

    private void finishUpdate(@NotNull WorldGuardRenderer renderer) {
        renderer.removeRegions(this.removedRegions);
        this.renderedRegions.removeAll(this.removedRegions);
        this.regionIdQueue = null;
        this.removedRegions = null;
    }

    boolean isFinished() {
        return this.regionIdQueue == null || this.regionIdQueue.isEmpty();
    }
}
