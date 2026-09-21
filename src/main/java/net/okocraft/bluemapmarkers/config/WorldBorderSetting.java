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

    public boolean enabled = true;
    @Required
    @Setting("marker-set")
    public MarkerSetSetting markerSetSetting;
    public String label = "World Border";
    public Color outlineColor = new Color(255, 0, 0, 1);
    public float height = 63f;
    public int updateInterval = 15;
    public Set<String> disabledWorlds = Set.of();

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

    @PostProcess
    private void validate() throws SerializationException {
        if (this.updateInterval <= 0) {
            throw new SerializationException("update-interval must be a positive integer");
        }
    }
}
