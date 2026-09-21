package net.okocraft.bluemapmarkers.config;

import de.bluecolored.bluemap.api.markers.MarkerSet;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Setting;

import java.util.Set;

@ConfigSerializable
public final class MarkerSetSetting {

    private String name = "";
    @Setting("default-hidden")
    private boolean defaultHidden = false;
    private int sorting = 0;
    @Setting("disabled-maps")
    private Set<String> disabledMaps = Set.of();

    public MarkerSetSetting() {
    }

    public MarkerSetSetting(String name, boolean defaultHidden, int sorting, Set<String> disabledMaps) {
        this.name = name;
        this.defaultHidden = defaultHidden;
        this.sorting = sorting;
        this.disabledMaps = disabledMaps;
    }

    public String name() {
        return this.name;
    }

    public boolean defaultHidden() {
        return this.defaultHidden;
    }

    public int sorting() {
        return this.sorting;
    }

    public Set<String> disabledMaps() {
        return this.disabledMaps;
    }

    public @NotNull MarkerSet createMarkerSet() {
        return MarkerSet.builder()
                .label(this.name)
                .sorting(this.sorting)
                .defaultHidden(this.defaultHidden)
                .build();
    }
}
