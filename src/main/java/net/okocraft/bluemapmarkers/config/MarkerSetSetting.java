package net.okocraft.bluemapmarkers.config;

import de.bluecolored.bluemap.api.markers.MarkerSet;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.objectmapping.ConfigSerializable;
import org.spongepowered.configurate.objectmapping.meta.Required;

import java.util.Set;

@ConfigSerializable
public final class MarkerSetSetting {

    @Required
    public String name;
    public boolean defaultHidden = false;
    public int sorting = 0;
    public Set<String> disabledMaps = Set.of();

    public MarkerSetSetting() {
    }

    public MarkerSetSetting(String name, boolean defaultHidden, int sorting, Set<String> disabledMaps) {
        this.name = name;
        this.defaultHidden = defaultHidden;
        this.sorting = sorting;
        this.disabledMaps = disabledMaps;
    }

    public @NotNull MarkerSet createMarkerSet() {
        return MarkerSet.builder()
                .label(this.name)
                .sorting(this.sorting)
                .defaultHidden(this.defaultHidden)
                .build();
    }
}
