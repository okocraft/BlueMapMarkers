package net.okocraft.bluemapmarkers.config;

import de.bluecolored.bluemap.api.math.Color;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.Set;

@ConfigSerializable
public final class WorldBorderSetting {

    private boolean enabled = true;
    @Setting("marker-set")
    private MarkerSetSetting markerSetSetting = new MarkerSetSetting("World Border", false, 0, Set.of());
    private String label = "World Border";
    @Setting("outline-color")
    private String outlineColorValue = "#ff0000ff";
    private float height = 63f;
    @Setting("update-interval")
    private int updateInterval = 15;
    @Setting("disabled-worlds")
    private Set<String> disabledWorlds = Set.of();

    private transient Color outlineColor;

    public WorldBorderSetting() {
    }

    public WorldBorderSetting(
            boolean enabled,
            MarkerSetSetting markerSetSetting,
            String label,
            Color outlineColor,
            float height,
            int updateInterval,
            Set<String> disabledWorlds
    ) {
        this.enabled = enabled;
        this.markerSetSetting = markerSetSetting;
        this.label = label;
        this.outlineColor = outlineColor;
        this.height = height;
        this.updateInterval = updateInterval;
        this.disabledWorlds = disabledWorlds;
    }

    public boolean enabled() {
        return this.enabled;
    }

    public MarkerSetSetting markerSetSetting() {
        return this.markerSetSetting;
    }

    public String label() {
        return this.label;
    }

    public Color outlineColor() {
        if (this.outlineColor == null) {
            this.outlineColor = new Color(this.outlineColorValue);
        }
        return this.outlineColor;
    }

    public float height() {
        return this.height;
    }

    public int updateInterval() {
        return this.updateInterval;
    }

    public Set<String> disabledWorlds() {
        return this.disabledWorlds;
    }
}
