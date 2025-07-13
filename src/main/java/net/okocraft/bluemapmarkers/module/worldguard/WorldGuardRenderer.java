package net.okocraft.bluemapmarkers.module.worldguard;

import com.flowpowered.math.vector.Vector3d;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StringFlag;
import com.sk89q.worldguard.protection.flags.registry.FlagRegistry;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import de.bluecolored.bluemap.api.BlueMapMap;
import de.bluecolored.bluemap.api.markers.ExtrudeMarker;
import de.bluecolored.bluemap.api.markers.Marker;
import de.bluecolored.bluemap.api.markers.ObjectMarker;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.api.math.Color;
import net.okocraft.bluemapmarkers.config.WorldGuardSetting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

abstract class WorldGuardRenderer {

    private static final StateFlag RENDER_FLAG = new StateFlag("render-on-bluemap", true);
    private static final StringFlag COLOR_FLAG = new StringFlag("bluemap-color");
    private static final StringFlag OUTLINE_FLAG = new StringFlag("bluemap-outline-color");
    static final StringFlag DISPLAY_FLAG = new StringFlag("bluemap-display");

    private final WorldGuardSetting.RenderSetting setting;
    private final RegionColor ownedRegionColor;
    private final RegionColor unownedRegionColor;
    private final DetailFormatter detailFormatter;

    static void register(@NotNull FlagRegistry registry) {
        try {
            registry.registerAll(Arrays.asList(RENDER_FLAG, COLOR_FLAG, OUTLINE_FLAG, DISPLAY_FLAG));
        } catch (Exception ignored) {
        }
    }

    protected WorldGuardRenderer(@NotNull WorldGuardSetting.RenderSetting setting) {
        this.setting = setting;
        this.ownedRegionColor = RegionColor.fromSetting(setting.ownedRegion());
        this.unownedRegionColor = RegionColor.fromSetting(setting.unownedRegion());
        this.detailFormatter = DetailFormatter.compile(this.setting.detailFormat());
    }

    abstract @Nullable RenderedRegionInfo renderRegion(@NotNull ProtectedRegion region);

    abstract void putMarkerSets(@NotNull UUID worldUid, @NotNull BlueMapMap target);

    abstract void removeRegions(@NotNull Collection<RenderedRegionInfo> regions);

    @NotNull String createMarkerId(@NotNull ProtectedRegion region) {
        return "WorldGuard-" + region.getId();
    }

    protected @Nullable RegionRenderer.Result renderIfNeeded(@NotNull ProtectedRegion region) {
        StateFlag.State state = region.getFlag(RENDER_FLAG);
        if (state == StateFlag.State.DENY || (!this.setting.defaultRender() && state != StateFlag.State.ALLOW)) {
            return null;
        }
        return RegionRenderer.render(region);
    }

    protected @NotNull Marker createMarker(@NotNull ProtectedRegion region, @NotNull RegionRenderer.Result renderResult) {
        boolean isOwned = region.hasMembersOrOwners();
        var outlineColor = getSpecifiedOrDefaultColor(region, OUTLINE_FLAG, isOwned ? this.ownedRegionColor.outlineColor() : this.unownedRegionColor.outlineColor());
        var fillColor = getSpecifiedOrDefaultColor(region, COLOR_FLAG, isOwned ? this.ownedRegionColor.fillColor() : this.unownedRegionColor.fillColor());

        ObjectMarker.Builder<?, ?> builder;

        if (this.setting.render3D()) {
            builder = ExtrudeMarker.builder()
                    .shape(renderResult.shape(), renderResult.minY(), renderResult.maxY())
                    .lineColor(outlineColor)
                    .fillColor(fillColor);
        } else {
            builder = ShapeMarker.builder()
                    .shape(renderResult.shape(), this.setting.height())
                    .lineColor(outlineColor)
                    .depthTestEnabled(false)
                    .fillColor(fillColor);
        }

        var position2d = renderResult.shape().getPoint(0);

        return builder.label(region.getId())
                .detail(this.detailFormatter.format(region))
                .position(new Vector3d(position2d.getX(), ((double) (renderResult.minY() + renderResult.maxY()) / 2), position2d.getY()))
                .minDistance(this.setting.minDistance())
                .maxDistance(this.setting.maxDistance())
                .build();
    }

    private static @NotNull Color getSpecifiedOrDefaultColor(@NotNull ProtectedRegion region, @NotNull StringFlag flag, @NotNull Color defaultColor) {
        var value = region.getFlag(flag);

        if (value == null || value.isEmpty()) {
            return defaultColor;
        }

        try {
            return new Color(value);
        } catch (NumberFormatException ignored) {
            return defaultColor;
        }
    }

    private record RegionColor(@NotNull Color fillColor, @NotNull Color outlineColor) {
        private static @NotNull RegionColor fromSetting(@NotNull WorldGuardSetting.RegionColor color) {
            return new RegionColor(color.fillColor(), color.outlineColor());
        }
    }
}
