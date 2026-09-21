package net.okocraft.bluemapmarkers.module.worldguard;

import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.UUID;

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
    void testFormatResolvesOwnerAndMemberNames() {
        var ownerId = UUID.fromString("00000000-0000-0000-0000-000000000021");
        var memberId = UUID.fromString("00000000-0000-0000-0000-000000000022");
        var region = region("spawn");
        region.getOwners().addPlayer(ownerId);
        region.getMembers().addPlayer(memberId);

        var owner = Mockito.mock(OfflinePlayer.class);
        var member = Mockito.mock(OfflinePlayer.class);
        Mockito.when(owner.getName()).thenReturn("Alice");
        Mockito.when(member.getName()).thenReturn("Bob");

        try (var bukkit = Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getOfflinePlayer(ownerId)).thenReturn(owner);
            bukkit.when(() -> Bukkit.getOfflinePlayer(memberId)).thenReturn(member);

            var result = DetailFormatter.compile("{region_owners}|{region_members}").format(region);

            Assertions.assertEquals("Alice|Bob", result);
        }
    }

    @Test
    void testFormatFallsBackToUuidWhenPlayerNameIsUnavailable() {
        var ownerId = UUID.fromString("00000000-0000-0000-0000-000000000023");
        var region = region("spawn");
        region.getOwners().addPlayer(ownerId);

        var owner = Mockito.mock(OfflinePlayer.class);
        Mockito.when(owner.getName()).thenReturn(null);

        try (var bukkit = Mockito.mockStatic(Bukkit.class)) {
            bukkit.when(() -> Bukkit.getOfflinePlayer(ownerId)).thenReturn(owner);

            var result = DetailFormatter.compile("{region_owners}").format(region);

            Assertions.assertEquals(ownerId.toString(), result);
        }
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
