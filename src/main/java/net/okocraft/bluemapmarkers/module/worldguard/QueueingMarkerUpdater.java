package net.okocraft.bluemapmarkers.module.worldguard;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import it.unimi.dsi.fastutil.objects.ObjectSet;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;

class QueueingMarkerUpdater {

    private final ObjectSet<RenderedRegionInfo> renderedRegions = new ObjectOpenHashSet<>();
    private final int limit;

    private Iterator<ProtectedRegion> regionIterator;
    private ObjectSet<RenderedRegionInfo> removedRegions;

    QueueingMarkerUpdater(int limit) {
        this.limit = limit;
    }

    void beginUpdate(@NotNull Collection<ProtectedRegion> regions) {
        if (!this.isFinished()) {
            throw new IllegalStateException("The previous update is still in progress");
        }

        this.regionIterator = regions.iterator();
        this.removedRegions = new ObjectOpenHashSet<>(this.renderedRegions);
    }

    boolean doUpdate(@NotNull WorldGuardRenderer renderer) {
        if (this.regionIterator == null) {
            throw new IllegalStateException("beginUpdate must be called before doUpdate");
        }

        for (int i = 0; i < this.limit; i++) {
            if (!this.regionIterator.hasNext()) {
                this.finishUpdate(renderer);
                return true;
            }

            var region = this.regionIterator.next();

            var info = renderer.renderRegion(region);

            if (info != null) {
                this.renderedRegions.add(info);
                this.removedRegions.remove(info);
            }
        }

        if (!this.regionIterator.hasNext()) {
            this.finishUpdate(renderer);
            return true;
        }

        return false;
    }

    private void finishUpdate(@NotNull WorldGuardRenderer renderer) {
        renderer.removeRegions(this.removedRegions);
        this.renderedRegions.removeAll(this.removedRegions);
        this.removedRegions.clear();
    }

    boolean isFinished() {
        return this.regionIterator == null || !this.regionIterator.hasNext();
    }
}
