package net.okocraft.bluemapmarkers.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;

class MarkerSetSettingTest {

    @Test
    void testCreateMarkerSetCopiesPresentationSettings() {
        var setting = new MarkerSetSetting("Regions", true, 42, Set.of("flat"));

        var markerSet = setting.createMarkerSet();

        Assertions.assertEquals("Regions", markerSet.getLabel());
        Assertions.assertTrue(markerSet.isDefaultHidden());
        Assertions.assertEquals(42, markerSet.getSorting());
    }
}
