package net.okocraft.bluemapmarkers.module.worldguard;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.okocraft.bluemapmarkers.config.MarkerSetSetting;
import net.okocraft.bluemapmarkers.config.WorldGuardSetting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.UUID;

class SeparatingWorldGuardRenderer extends WorldGuardRenderer {

    private final Int2ObjectMap<MarkerSet> separatedMarkerSetMap = new Int2ObjectOpenHashMap<>();

    private final MarkerSetSetting markerSetSetting;
    private final int centerSize;
    private final int length;
    private final String labelFormat;

    SeparatingWorldGuardRenderer(@NotNull WorldGuardSetting.WorldSetting worldSetting, @NotNull MarkerSetSetting markerSetSetting) {
        super(worldSetting.renderSetting());
        this.markerSetSetting = markerSetSetting;
        this.labelFormat = worldSetting.separationSetting().labelFormat();
        this.centerSize = worldSetting.separationSetting().centerSize();
        this.length = Math.max(1, worldSetting.separationSetting().size());
    }

    @Nullable RenderedRegionInfo renderRegion(@NotNull ProtectedRegion region) {
        var renderResult = super.renderIfNeeded(region);

        if (renderResult == null) {
            return null;
        }

        var markerId = super.createMarkerId(region);
        var markerSetKey = this.getMarkerSetKey(region.getMinimumPoint());
        var markerSet = this.separatedMarkerSetMap.computeIfAbsent(markerSetKey, this::createMarkerSet);
        markerSet.put(markerId, super.createMarker(region, renderResult));
        return new RenderedRegionInfo(markerId, markerSetKey);
    }

    @Override
    void putMarkerSets(@NotNull UUID worldUid, @NotNull BlueMapMap target) {
        for (var entry : this.separatedMarkerSetMap.int2ObjectEntrySet()) {
            target.getMarkerSets().put("WorldGuard-" + worldUid + "_" + entry.getIntKey(), entry.getValue());
        }
    }

    @Override
    void removeRegions(@NotNull Collection<RenderedRegionInfo> regions) {
        for (var region : regions) {
            var markerSet = this.separatedMarkerSetMap.get(region.markerSetKey());

            if (markerSet != null) {
                markerSet.remove(region.markerId());
            }
        }
    }

    private int getMarkerSetKey(@NotNull BlockVector3 loc) {
        int absX = Math.abs(loc.x()) - this.centerSize;
        int absZ = Math.abs(loc.z()) - this.centerSize;
        if (absX <= 0 && absZ <= 0) {
            return 0;
        }

        return Math.max(absX, absZ) / this.length + 1;
    }

    private @NotNull MarkerSet createMarkerSet(int sec) {
        String label;

        if (sec == 0) {
            label = this.formatLabel(0, this.centerSize);
        } else {
            label = this.formatLabel(this.centerSize + (sec - 1) * this.length, this.centerSize + sec * this.length);
        }

        return MarkerSet.builder()
                .defaultHidden(this.markerSetSetting.defaultHidden())
                .label(label)
                .sorting(this.markerSetSetting.sorting() + sec)
                .build();
    }

    private @NotNull String formatLabel(int min, int max) {
        return this.labelFormat
                .replace("%min%", Integer.toString(min))
                .replace("%max%", Integer.toString(max));
    }
}
