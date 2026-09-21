package net.okocraft.bluemapmarkers.config;

import de.bluecolored.bluemap.api.math.Color;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.PostProcess;
import org.spongepowered.configurate.objectmapping.meta.Required;
import org.spongepowered.configurate.objectmapping.meta.Setting;
import org.spongepowered.configurate.serialize.SerializationException;

import java.util.Map;
import java.util.Set;

@ConfigSerializable
public final class WorldGuardSetting {

    public boolean enabled = true;
    @Required
    @Setting("marker-set")
    public MarkerSetSetting markerSetSetting;
    @Required
    public Map<String, WorldSetting> worldSettingMap;

    public WorldGuardSetting() {
    }

    public WorldGuardSetting(
            boolean enabled,
            MarkerSetSetting markerSetSetting,
            Map<String, WorldSetting> worldSettingMap
    ) {
        this.enabled = enabled;
        this.markerSetSetting = markerSetSetting;
        this.worldSettingMap = worldSettingMap;
    }

    @ConfigSerializable
    public static final class WorldSetting {

        public boolean enabled = true;
        public Set<String> disabledMaps = Set.of();
        public int updateInterval = 10;
        public int updateLimit = 50;
        @Required
        public RenderSetting renderSetting;
        @Required
        public SeparationSetting separationSetting;

        public WorldSetting() {
        }

        public WorldSetting(
                boolean enabled,
                Set<String> disabledMaps,
                int updateInterval,
                int updateLimit,
                RenderSetting renderSetting,
                SeparationSetting separationSetting
        ) {
            this.enabled = enabled;
            this.disabledMaps = disabledMaps;
            this.updateInterval = updateInterval;
            this.updateLimit = updateLimit;
            this.renderSetting = renderSetting;
            this.separationSetting = separationSetting;
        }

        @PostProcess
        private void validate() throws SerializationException {
            if (this.updateInterval < 0) {
                throw new SerializationException("update-interval must be a non-negative integer");
            }
            if (this.updateLimit <= 0) {
                throw new SerializationException("update-limit must be a positive integer");
            }
        }
    }

    @ConfigSerializable
    public static final class OwnedRegionColor {

        public Color fillColor = new Color(30, 144, 255, 1);
        public Color outlineColor = new Color(0, 191, 255, 1);

        public OwnedRegionColor() {
        }

        public OwnedRegionColor(Color fillColor, Color outlineColor) {
            this.fillColor = fillColor;
            this.outlineColor = outlineColor;
        }
    }

    @ConfigSerializable
    public static final class UnownedRegionColor {

        public Color fillColor = new Color(30, 144, 255, 1);
        public Color outlineColor = new Color(0, 255, 0, 1);

        public UnownedRegionColor() {
        }

        public UnownedRegionColor(Color fillColor, Color outlineColor) {
            this.fillColor = fillColor;
            this.outlineColor = outlineColor;
        }
    }

    @ConfigSerializable
    public static final class RenderSetting {

        public boolean defaultRender = true;
        @Required
        public OwnedRegionColor ownedRegion;
        @Required
        public UnownedRegionColor unownedRegion;
        public String detailFormat = """
                <h2 style="color:#00bfff;text-align:center;margin-block-end:0.3em">{region_displayname}</h2>
                <br/>
                <span style="font-size:100%;">Owners: </span><span style="font-weight:bold;">{region_owners}</span><br/>
                <span style="font-size:100%;">Members: </span><span style="font-weight:bold;">{region_members}</span><br/>
                """.strip();
        @Setting("render-3d")
        public boolean render3D = true;
        public float height = 63f;
        public double minDistance = 0d;
        public double maxDistance = 1000d;

        public RenderSetting() {
        }

        public RenderSetting(
                boolean defaultRender,
                OwnedRegionColor ownedRegion,
                UnownedRegionColor unownedRegion,
                String detailFormat,
                boolean render3D,
                float height,
                double minDistance,
                double maxDistance
        ) {
            this.defaultRender = defaultRender;
            this.ownedRegion = ownedRegion;
            this.unownedRegion = unownedRegion;
            this.detailFormat = detailFormat;
            this.render3D = render3D;
            this.height = height;
            this.minDistance = minDistance;
            this.maxDistance = maxDistance;
        }
    }

    @ConfigSerializable
    public static final class SeparationSetting {

        public boolean enabled = true;
        public String labelFormat = "WorldGuard (%min% ~ %max%)";
        public int size = 3;
        public int centerSize = 500;

        public SeparationSetting() {
        }

        public SeparationSetting(boolean enabled, String labelFormat, int size, int centerSize) {
            this.enabled = enabled;
            this.labelFormat = labelFormat;
            this.size = size;
            this.centerSize = centerSize;
        }

        @PostProcess
        private void validate() throws SerializationException {
            if (this.size <= 0) {
                throw new SerializationException("size must be a positive integer");
            }
            if (this.centerSize < 0) {
                throw new SerializationException("center-size must be a non-negative integer");
            }
        }
    }
}
