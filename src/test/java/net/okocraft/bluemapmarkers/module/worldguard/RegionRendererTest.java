package net.okocraft.bluemapmarkers.module.worldguard;

import com.flowpowered.math.vector.Vector2d;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.regions.GlobalProtectedRegion;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class RegionRendererTest {

    @Test
    void testRenderCuboidUsesInclusiveBlockBounds() {
        var region = new ProtectedCuboidRegion(
                "cuboid",
                BlockVector3.at(1, 20, 2),
                BlockVector3.at(3, 5, 4)
        );

        var result = RegionRenderer.render(region);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(5, result.minY());
        Assertions.assertEquals(20, result.maxY());
        Assertions.assertArrayEquals(
                new Vector2d[]{
                        new Vector2d(1, 2),
                        new Vector2d(4, 2),
                        new Vector2d(4, 5),
                        new Vector2d(1, 5)
                },
                result.shape().getPoints()
        );
    }

    @Test
    void testRenderPolygonExpandsPositiveEdgesByOne() {
        var region = new ProtectedPolygonalRegion(
                "polygon",
                List.of(
                        BlockVector2.at(0, 0),
                        BlockVector2.at(2, 0),
                        BlockVector2.at(2, 2),
                        BlockVector2.at(0, 2)
                ),
                -10,
                30
        );

        var result = RegionRenderer.render(region);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(-10, result.minY());
        Assertions.assertEquals(30, result.maxY());
        Assertions.assertArrayEquals(
                new Vector2d[]{
                        new Vector2d(0, 0),
                        new Vector2d(3, 0),
                        new Vector2d(3, 3),
                        new Vector2d(0, 3)
                },
                result.shape().getPoints()
        );
    }

    @Test
    void testRenderPolygonHandlesCollapsedLine() {
        var region = new ProtectedPolygonalRegion(
                "line",
                List.of(
                        BlockVector2.at(1, 1),
                        BlockVector2.at(1, 2),
                        BlockVector2.at(1, 3)
                ),
                0,
                10
        );

        var result = RegionRenderer.render(region);

        Assertions.assertNotNull(result);
        Assertions.assertArrayEquals(
                new Vector2d[]{
                        new Vector2d(1, 1),
                        new Vector2d(1, 4),
                        new Vector2d(2, 4),
                        new Vector2d(2, 1)
                },
                result.shape().getPoints()
        );
    }

    @Test
    void testRenderPolygonPreservesBacktrackingCorner() {
        var region = new ProtectedPolygonalRegion(
                "backtracking",
                List.of(
                        BlockVector2.at(0, 0),
                        BlockVector2.at(2, 0),
                        BlockVector2.at(1, 0),
                        BlockVector2.at(1, 2),
                        BlockVector2.at(0, 2)
                ),
                0,
                10
        );

        var result = RegionRenderer.render(region);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(6, result.shape().getPointCount());
    }

    @Test
    void testRenderReturnsNullForPolygonWithFewerThanThreePoints() {
        var region = new ProtectedPolygonalRegion(
                "short",
                List.of(BlockVector2.at(0, 0), BlockVector2.at(1, 0)),
                0,
                10
        );

        Assertions.assertNull(RegionRenderer.render(region));
    }

    @Test
    void testRenderReturnsNullForUnsupportedRegionType() {
        Assertions.assertNull(RegionRenderer.render(new GlobalProtectedRegion("__global__")));
    }
}
