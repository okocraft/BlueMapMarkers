package net.okocraft.bluemapmarkers.config;

import de.bluecolored.bluemap.api.math.Color;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.PostProcess;
import org.spongepowered.configurate.objectmapping.meta.Required;
import org.spongepowered.configurate.objectmapping.meta.Setting;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.Set;

@ConfigSerializable
public final class WorldBorderSetting {

    private boolean enabled = true;
    @Required
    @Setting("marker-set")
    private MarkerSetSetting markerSetSetting;
    private String label = "World Border";
    private Color outlineColor = new Color(255, 0, 0, 1);
    private float height = 63f;
    private int updateInterval = 15;
    private Set<String> disabledWorlds = Set.of();

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

    @PostProcess
    private void validate() throws SerializationException {
        if (this.updateInterval <= 0) {
            throw new SerializationException("update-interval must be a positive integer");
        }
    }
}
