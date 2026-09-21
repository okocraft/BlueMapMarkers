package net.okocraft.bluemapmarkers.module.worldguard;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import net.okocraft.bluemapmarkers.config.MarkerSetSetting;
import net.okocraft.bluemapmarkers.config.WorldGuardSetting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.UUID;

class DefaultWorldGuardRenderer extends WorldGuardRenderer {

    private final MarkerSet markerSet;

    DefaultWorldGuardRenderer(@NotNull WorldGuardSetting.WorldSetting worldSetting, @NotNull MarkerSetSetting markerSetSetting) {
        super(worldSetting.renderSetting());
        this.markerSet = markerSetSetting.createMarkerSet();
    }

    @Override
    @Nullable RenderedRegionInfo renderRegion(@NotNull ProtectedRegion region) {
        var renderResult = super.renderIfNeeded(region);

        if (renderResult != null) {
            var markerId = super.createMarkerId(region);
            this.markerSet.put(markerId, super.createMarker(region, renderResult));
            return new RenderedRegionInfo(markerId, 0);
        } else {
            return null;
        }
    }

    @Override
    void putMarkerSets(@NotNull UUID worldUid, @NotNull BlueMapMap target) {
        target.getMarkerSets().put("WorldGuard-" + worldUid, this.markerSet);
    }

    @Override
    void removeRegions(@NotNull Collection<RenderedRegionInfo> regions) {
        for (var region : regions) {
            this.markerSet.remove(region.markerId());
        }
    }
}
