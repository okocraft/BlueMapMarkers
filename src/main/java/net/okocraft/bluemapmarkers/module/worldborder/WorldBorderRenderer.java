package net.okocraft.bluemapmarkers.module.worldborder;

import com.flowpowered.math.vector.Vector2d;
import com.flowpowered.math.vector.Vector3d;
import de.bluecolored.bluemap.api.markers.Marker;
import de.bluecolored.bluemap.api.markers.MarkerSet;
import de.bluecolored.bluemap.api.markers.ShapeMarker;
import de.bluecolored.bluemap.api.math.Color;
import de.bluecolored.bluemap.api.math.Shape;
import io.papermc.paper.event.world.border.WorldBorderBoundsChangeEvent;
import io.papermc.paper.event.world.border.WorldBorderCenterChangeEvent;
import net.okocraft.bluemapmarkers.config.WorldBorderSetting;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

class WorldBorderRenderer implements Listener {

    private final WorldBorderSetting setting;
    private final MarkerSet markerSet;

    private String markerId = null;

    private double size = Double.NaN;
    private double centerX = Double.NaN;
    private double centerZ = Double.NaN;

    WorldBorderRenderer(@NotNull WorldBorderSetting setting) {
        this.setting = setting;
        this.markerSet = setting.markerSetSetting().createMarkerSet();
    }

    MarkerSet getMarkerSet() {
        return this.markerSet;
    }

    void render(@NotNull World world) {
        this.markerId = "wb_" + world.getUID();

        var worldBorder = world.getWorldBorder();
        var center = worldBorder.getCenter();

        this.size = worldBorder.getSize();
        this.centerX = center.getX();
        this.centerZ = center.getZ();

        this.updateMarker(this.markerSet.get(this.markerId));
    }

    @EventHandler
    private void onWorldBorderBoundsChange(@NotNull WorldBorderBoundsChangeEvent event) {
        this.size = event.getNewSize();
        this.updateMarker(this.markerSet.get(this.markerId));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onWorldBorderCenterChange(@NotNull WorldBorderCenterChangeEvent event) {
        var center = event.getNewCenter();
        this.centerX = center.getX();
        this.centerZ = center.getZ();
        this.updateMarker(this.markerSet.get(this.markerId));
    }

    private void updateMarker(@Nullable Marker current) {
        if (current instanceof ShapeMarker lineMarker) {
            lineMarker.setShape(this.createShape(), this.setting.height());
            lineMarker.setPosition(this.createCenter());
        } else {
            var newMarker = new ShapeMarker(this.markerId, this.createShape(), this.setting.height());
            newMarker.setPosition(this.createCenter());
            newMarker.setLineColor(this.setting.outlineColor());
            newMarker.setFillColor(new Color(0, 0, 0, 0));
            newMarker.setLabel(this.setting.label());
            newMarker.setDetail(this.setting.label());
            newMarker.setDepthTestEnabled(false);
            newMarker.setMinDistance(0);
            newMarker.setMaxDistance(Double.MAX_VALUE);
            newMarker.setLineWidth(3);
            this.markerSet.put(this.markerId, newMarker);
        }
    }

    private @NotNull Shape createShape() {
        double radius = this.size / 2;
        return new Shape(
                new Vector2d(this.centerX + radius, this.centerZ + radius),
                new Vector2d(this.centerX + radius, this.centerZ - radius),
                new Vector2d(this.centerX - radius, this.centerZ - radius),
                new Vector2d(this.centerX - radius, this.centerZ + radius)
        );
    }

    private @NotNull Vector3d createCenter() {
        return new Vector3d(this.centerX, this.setting.height(), this.centerZ);
    }
}
