package net.okocraft.bluemapmarkers.module.worldguard;

import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

interface RegionUpdateRenderer {

    @Nullable RenderedRegionInfo renderRegion(@NotNull ProtectedRegion region);

    void removeRegions(@NotNull Collection<RenderedRegionInfo> regions);
}
