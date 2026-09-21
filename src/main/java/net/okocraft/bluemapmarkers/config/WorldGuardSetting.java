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

    private boolean enabled = true;
    @Required
    @Setting("marker-set")
    private MarkerSetSetting markerSetSetting;
    @Required
    private Map<String, WorldSetting> worldSettingMap;

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

    public boolean enabled() {
        return this.enabled;
    }

    public MarkerSetSetting markerSetSetting() {
        return this.markerSetSetting;
    }

    public Map<String, WorldSetting> worldSettingMap() {
        return this.worldSettingMap;
    }

    @ConfigSerializable
    public static final class WorldSetting {

        private boolean enabled = true;
        private Set<String> disabledMaps = Set.of();
        private int updateInterval = 10;
        private int updateLimit = 50;
        @Required
        private RenderSetting renderSetting;
        @Required
        private SeparationSetting separationSetting;

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

        public boolean enabled() {
            return this.enabled;
        }

        public Set<String> disabledMaps() {
            return this.disabledMaps;
        }

        public int updateInterval() {
            return this.updateInterval;
        }

        public int updateLimit() {
            return this.updateLimit;
        }

        public RenderSetting renderSetting() {
            return this.renderSetting;
        }

        public SeparationSetting separationSetting() {
            return this.separationSetting;
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

    public interface RegionColor {

        Color fillColor();

        Color outlineColor();
    }

    @ConfigSerializable
    public static final class OwnedRegionColor implements RegionColor {

        private Color fillColor = new Color(30, 144, 255, 1);
        private Color outlineColor = new Color(0, 191, 255, 1);

        public OwnedRegionColor() {
        }

        public OwnedRegionColor(Color fillColor, Color outlineColor) {
            this.fillColor = fillColor;
            this.outlineColor = outlineColor;
        }

        @Override
        public Color fillColor() {
            return this.fillColor;
        }

        @Override
        public Color outlineColor() {
            return this.outlineColor;
        }
    }

    @ConfigSerializable
    public static final class UnownedRegionColor implements RegionColor {

        private Color fillColor = new Color(30, 144, 255, 1);
        private Color outlineColor = new Color(0, 255, 0, 1);

        public UnownedRegionColor() {
        }

        public UnownedRegionColor(Color fillColor, Color outlineColor) {
            this.fillColor = fillColor;
            this.outlineColor = outlineColor;
        }

        @Override
        public Color fillColor() {
            return this.fillColor;
        }

        @Override
        public Color outlineColor() {
            return this.outlineColor;
        }
    }

    @ConfigSerializable
    public static final class RenderSetting {

        private boolean defaultRender = true;
        @Required
        private OwnedRegionColor ownedRegion;
        @Required
        private UnownedRegionColor unownedRegion;
        private String detailFormat = """
                <h2 style="color:#00bfff;text-align:center;margin-block-end:0.3em">{region_displayname}</h2>
                <br/>
                <span style="font-size:100%;">Owners: </span><span style="font-weight:bold;">{region_owners}</span><br/>
                <span style="font-size:100%;">Members: </span><span style="font-weight:bold;">{region_members}</span><br/>
                """.strip();
        @Setting("render-3d")
        private boolean render3D = true;
        private float height = 63f;
        private double minDistance = 0d;
        private double maxDistance = 1000d;

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

        public boolean defaultRender() {
            return this.defaultRender;
        }

        public OwnedRegionColor ownedRegion() {
            return this.ownedRegion;
        }

        public UnownedRegionColor unownedRegion() {
            return this.unownedRegion;
        }

        public String detailFormat() {
            return this.detailFormat;
        }

        public boolean render3D() {
            return this.render3D;
        }

        public float height() {
            return this.height;
        }

        public double minDistance() {
            return this.minDistance;
        }

        public double maxDistance() {
            return this.maxDistance;
        }
    }

    @ConfigSerializable
    public static final class SeparationSetting {

        private boolean enabled = true;
        private String labelFormat = "WorldGuard (%min% ~ %max%)";
        private int size = 3;
        private int centerSize = 500;

        public SeparationSetting() {
        }

        public SeparationSetting(boolean enabled, String labelFormat, int size, int centerSize) {
            this.enabled = enabled;
            this.labelFormat = labelFormat;
            this.size = size;
            this.centerSize = centerSize;
        }

        public boolean enabled() {
            return this.enabled;
        }

        public String labelFormat() {
            return this.labelFormat;
        }

        public int size() {
            return this.size;
        }

        public int centerSize() {
            return this.centerSize;
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
