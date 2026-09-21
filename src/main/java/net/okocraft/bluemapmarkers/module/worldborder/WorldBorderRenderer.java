package net.okocraft.bluemapmarkers.module.worldborder;

import com.flowpowered.math.vector.Vector2d;
import com.flowpowered.math.vector.Vector3d;
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

import java.util.UUID;

class WorldBorderRenderer implements Listener {

    private final WorldBorderSetting setting;
    private final MarkerSet markerSet;
    private final UUID worldUid;

    private String markerId = null;

    private double size = Double.NaN;
    private double centerX = Double.NaN;
    private double centerZ = Double.NaN;

    WorldBorderRenderer(@NotNull WorldBorderSetting setting, @NotNull UUID worldUid) {
        this.setting = setting;
        this.markerSet = setting.markerSetSetting.createMarkerSet();
        this.worldUid = worldUid;
    }

    MarkerSet getMarkerSet() {
        return this.markerSet;
    }

    void render(@NotNull World world) {
        this.markerId = "wb_" + world.getUID();

        var worldBorder = world.getWorldBorder();
        var center = worldBorder.getCenter();
        double newSize = worldBorder.getSize();
        double newCenterX = center.getX();
        double newCenterZ = center.getZ();

        if (Double.compare(this.size, newSize) == 0 &&
                Double.compare(this.centerX, newCenterX) == 0 &&
                Double.compare(this.centerZ, newCenterZ) == 0 &&
                this.markerSet.get(this.markerId) != null) {
            return;
        }

        this.size = newSize;
        this.centerX = newCenterX;
        this.centerZ = newCenterZ;

        this.updateMarker();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onWorldBorderBoundsChange(@NotNull WorldBorderBoundsChangeEvent event) {
        if (!event.getWorld().getUID().equals(this.worldUid)) {
            return;
        }

        if (event.getType() == WorldBorderBoundsChangeEvent.Type.INSTANT_MOVE) {
            this.size = event.getNewSize();
            this.updateMarker();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onWorldBorderCenterChange(@NotNull WorldBorderCenterChangeEvent event) {
        if (!event.getWorld().getUID().equals(this.worldUid)) {
            return;
        }

        var center = event.getNewCenter();
        this.centerX = center.getX();
        this.centerZ = center.getZ();
        this.updateMarker();
    }

    private void updateMarker() {
        var newMarker = new ShapeMarker(
                this.markerId, this.createCenter(), this.createShape(), this.setting.height
        );
        newMarker.setLineColor(this.setting.outlineColor);
        newMarker.setFillColor(new Color(0, 0, 0, 0));
        newMarker.setLabel(this.setting.label);
        newMarker.setDetail(this.setting.label);
        newMarker.setDepthTestEnabled(false);
        newMarker.setMinDistance(0);
        newMarker.setMaxDistance(Double.MAX_VALUE);
        newMarker.setLineWidth(3);
        this.markerSet.put(this.markerId, newMarker);
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
        return new Vector3d(this.centerX, this.setting.height, this.centerZ);
    }
}
