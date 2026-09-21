package net.okocraft.bluemapmarkers.module.worldguard;

import com.flowpowered.math.vector.Vector3d;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.StringFlag;
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

import java.util.Collection;
import java.util.UUID;

abstract class WorldGuardRenderer {

    private static final StateFlag RENDER_FLAG = register(new StateFlag("render-on-bluemap", true), StateFlag.class);
    private static final StringFlag COLOR_FLAG = register(new StringFlag("bluemap-color"), StringFlag.class);
    private static final StringFlag OUTLINE_FLAG = register(new StringFlag("bluemap-outline-color"), StringFlag.class);
    static final StringFlag DISPLAY_FLAG = register(new StringFlag("bluemap-display"), StringFlag.class);

    private final WorldGuardSetting.RenderSetting setting;
    private final RegionColor ownedRegionColor;
    private final RegionColor unownedRegionColor;
    private final DetailFormatter detailFormatter;

    static void registerFlags() {
        // Invoking this method triggers class initialization and registers the static flag fields.
    }

    private static <F extends Flag<?>> @NotNull F register(@NotNull F flag, @NotNull Class<F> flagType) {
        var registry = WorldGuard.getInstance().getFlagRegistry();

        try {
            registry.register(flag);
            return flag;
        } catch (com.sk89q.worldguard.protection.flags.registry.FlagConflictException e) {
            var existing = registry.get(flag.getName());

            if (flagType.isInstance(existing)) {
                return flagType.cast(existing);
            }

            throw new IllegalStateException(
                    "WorldGuard flag '" + flag.getName() + "' is already registered with an incompatible type",
                    e
            );
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

    protected @NotNull Marker createMarker(@Nullable Marker current, @NotNull ProtectedRegion region,
                                            @NotNull RegionRenderer.Result renderResult) {
        boolean isOwned = region.hasMembersOrOwners();
        var outlineColor = getSpecifiedOrDefaultColor(region, OUTLINE_FLAG, isOwned ? this.ownedRegionColor.outlineColor() : this.unownedRegionColor.outlineColor());
        var fillColor = getSpecifiedOrDefaultColor(region, COLOR_FLAG, isOwned ? this.ownedRegionColor.fillColor() : this.unownedRegionColor.fillColor());

        ObjectMarker marker;

        if (this.setting.render3D()) {
            ExtrudeMarker extrudeMarker;
            if (current instanceof ExtrudeMarker existing) {
                extrudeMarker = existing;
                extrudeMarker.setShape(renderResult.shape(), renderResult.minY(), renderResult.maxY());
            } else {
                extrudeMarker = new ExtrudeMarker(
                        region.getId(), renderResult.shape(), renderResult.minY(), renderResult.maxY()
                );
            }
            extrudeMarker.setColors(outlineColor, fillColor);
            marker = extrudeMarker;
        } else {
            ShapeMarker shapeMarker;
            if (current instanceof ShapeMarker existing) {
                shapeMarker = existing;
                shapeMarker.setShape(renderResult.shape(), this.setting.height());
            } else {
                shapeMarker = new ShapeMarker(region.getId(), renderResult.shape(), this.setting.height());
            }
            shapeMarker.setColors(outlineColor, fillColor);
            shapeMarker.setDepthTestEnabled(false);
            marker = shapeMarker;
        }

        var position2d = renderResult.shape().getPoint(0);

        marker.setLabel(region.getId());
        marker.setDetail(this.detailFormatter.format(region));
        marker.setPosition(new Vector3d(
                position2d.getX(), ((double) (renderResult.minY() + renderResult.maxY()) / 2), position2d.getY()
        ));
        marker.setMinDistance(this.setting.minDistance());
        marker.setMaxDistance(this.setting.maxDistance());
        return marker;
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
