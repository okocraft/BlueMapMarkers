package net.okocraft.bluemapmarkers.config;

import de.bluecolored.bluemap.api.math.Color;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.Map;
import java.util.Set;

@ConfigSerializable
public final class WorldGuardSetting {

    private boolean enabled = true;
    @Setting("marker-set")
    private MarkerSetSetting markerSetSetting = new MarkerSetSetting("WorldGuard", false, 0, Set.of());
    @Setting("world-setting-map")
    private Map<String, WorldSetting> worldSettingMap = Map.of("default", new WorldSetting());

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
        @Setting("disabled-maps")
        private Set<String> disabledMaps = Set.of();
        @Setting("update-interval")
        private int updateInterval = 10;
        @Setting("update-limit")
        private int updateLimit = 50;
        @Setting("render-setting")
        private RenderSetting renderSetting = new RenderSetting();
        @Setting("separation-setting")
        private SeparationSetting separationSetting = new SeparationSetting();

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
    }

    public interface RegionColor {

        Color fillColor();

        Color outlineColor();
    }

    @ConfigSerializable
    public static final class OwnedRegionColor implements RegionColor {

        @Setting("fill-color")
        private String fillColorValue = "#1e90ffff";
        @Setting("outline-color")
        private String outlineColorValue = "#00bfffff";

        private transient Color fillColor;
        private transient Color outlineColor;

        public OwnedRegionColor() {
        }

        public OwnedRegionColor(Color fillColor, Color outlineColor) {
            this.fillColor = fillColor;
            this.outlineColor = outlineColor;
        }

        @Override
        public Color fillColor() {
            if (this.fillColor == null) {
                this.fillColor = new Color(this.fillColorValue);
            }
            return this.fillColor;
        }

        @Override
        public Color outlineColor() {
            if (this.outlineColor == null) {
                this.outlineColor = new Color(this.outlineColorValue);
            }
            return this.outlineColor;
        }
    }

    @ConfigSerializable
    public static final class UnownedRegionColor implements RegionColor {

        @Setting("fill-color")
        private String fillColorValue = "#1e90ffff";
        @Setting("outline-color")
        private String outlineColorValue = "#00ff00ff";

        private transient Color fillColor;
        private transient Color outlineColor;

        public UnownedRegionColor() {
        }

        public UnownedRegionColor(Color fillColor, Color outlineColor) {
            this.fillColor = fillColor;
            this.outlineColor = outlineColor;
        }

        @Override
        public Color fillColor() {
            if (this.fillColor == null) {
                this.fillColor = new Color(this.fillColorValue);
            }
            return this.fillColor;
        }

        @Override
        public Color outlineColor() {
            if (this.outlineColor == null) {
                this.outlineColor = new Color(this.outlineColorValue);
            }
            return this.outlineColor;
        }
    }

    @ConfigSerializable
    public static final class RenderSetting {

        @Setting("default-render")
        private boolean defaultRender = true;
        @Setting("owned-region")
        private OwnedRegionColor ownedRegion = new OwnedRegionColor();
        @Setting("unowned-region")
        private UnownedRegionColor unownedRegion = new UnownedRegionColor();
        @Setting("detail-format")
        private String detailFormat = """
                <h2 style="color:#00bfff;text-align:center;margin-block-end:0.3em">{region_displayname}</h2>
                <br/>
                <span style="font-size:100%;">Owners: </span><span style="font-weight:bold;">{region_owners}</span><br/>
                <span style="font-size:100%;">Members: </span><span style="font-weight:bold;">{region_members}</span><br/>
                """.strip();
        @Setting("render-3d")
        private boolean render3D = true;
        private float height = 63f;
        @Setting("min-distance")
        private double minDistance = 0d;
        @Setting("max-distance")
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
        @Setting("label-format")
        private String labelFormat = "WorldGuard (%min% ~ %max%)";
        private int size = 3;
        @Setting("center-size")
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
    }
}
