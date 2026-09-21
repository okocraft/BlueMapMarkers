package net.okocraft.bluemapmarkers.module.worldguard;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DetailFormatterTest {

    @Test
    void testFormatReplacesRegionId() {
        var region = region("spawn");

        var result = DetailFormatter.compile("Region: {region_id}").format(region);

        Assertions.assertEquals("Region: spawn", result);
    }

    @Test
    void testFormatReplacesParentChain() throws Exception {
        var grandParent = region("continent");
        var parent = region("country");
        var child = region("city");
        parent.setParent(grandParent);
        child.setParent(parent);

        var result = DetailFormatter.compile("{region_id}: {region_parents}").format(child);

        Assertions.assertEquals("city: country - continent", result);
    }

    @Test
    void testFormatKeepsUnknownPlaceholderLiteral() {
        var result = DetailFormatter.compile("{unknown}-{region_id}").format(region("spawn"));

        Assertions.assertEquals("{unknown}-spawn", result);
    }

    @Test
    void testFormatKeepsUnclosedPlaceholderLiteral() {
        var result = DetailFormatter.compile("prefix {region_id").format(region("spawn"));

        Assertions.assertEquals("prefix {region_id", result);
    }

    private static ProtectedCuboidRegion region(String id) {
        return new ProtectedCuboidRegion(id, BlockVector3.ZERO, BlockVector3.at(1, 1, 1));
    }
}
