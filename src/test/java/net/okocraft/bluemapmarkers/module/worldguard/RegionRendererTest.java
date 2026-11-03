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
                result.areas().getFirst().shape().getPoints()
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
                result.areas().getFirst().shape().getPoints()
        );
    }

    @Test
    void testRenderPolygonDrawsDiagonalEdgeAsBlockStaircase() {
        var region = new ProtectedPolygonalRegion(
                "diagonal",
                List.of(
                        BlockVector2.at(0, 0),
                        BlockVector2.at(3, 0),
                        BlockVector2.at(0, 3)
                ),
                0,
                10
        );

        var result = RegionRenderer.render(region);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.areas().size());
        Assertions.assertArrayEquals(
                new Vector2d[]{
                        new Vector2d(0, 0),
                        new Vector2d(4, 0),
                        new Vector2d(4, 1),
                        new Vector2d(3, 1),
                        new Vector2d(3, 2),
                        new Vector2d(2, 2),
                        new Vector2d(2, 3),
                        new Vector2d(1, 3),
                        new Vector2d(1, 4),
                        new Vector2d(0, 4)
                },
                result.areas().getFirst().shape().getPoints()
        );
    }

    @Test
    void testRenderPolygonDrawsTooLongLineOneBlockWide() {
        var region = new ProtectedPolygonalRegion(
                "line",
                List.of(BlockVector2.at(0, 0), BlockVector2.at(100_001, 100_001), BlockVector2.at(200_002, 200_002)),
                0,
                10
        );

        var result = RegionRenderer.render(region);

        Assertions.assertNotNull(result);
        Assertions.assertArrayEquals(
                new Vector2d[]{
                        new Vector2d(0, 0), new Vector2d(1, 0),
                        new Vector2d(200_003, 200_002), new Vector2d(200_003, 200_003),
                        new Vector2d(200_002, 200_003), new Vector2d(0, 1)
                },
                result.areas().getFirst().shape().getPoints()
        );
    }

    @Test
    void testRenderPolygonDrawsStraightEdgesWhenTooManyEdgesToRasterize() {
        // 50,000 columns with 200 edges each: within the column limit, over the edge-check limit.
        List<BlockVector2> points = new java.util.ArrayList<>();
        for (int i = 0; i < 200; i++) {
            points.add(BlockVector2.at(i * 250, i % 2 == 0 ? 0 : 1_000));
        }
        points.add(BlockVector2.at(49_750, -1_000));

        var result = RegionRenderer.render(new ProtectedPolygonalRegion("saw", points, 0, 10));

        Assertions.assertNotNull(result);
        Assertions.assertEquals(points.size(), result.areas().getFirst().shape().getPoints().length);
    }

    @Test
    void testRenderPolygonDrawsStraightEdgesWhenOutlineIsTooLong() {
        // Three parallel diagonal arms over 4,000 columns: within both cost limits, but 12,000 ranges.
        int l = 4_000;
        var points = List.of(
                BlockVector2.at(0, 0), BlockVector2.at(l, l), BlockVector2.at(l, l + 500), BlockVector2.at(10, 510),
                BlockVector2.at(10, 1_000), BlockVector2.at(l, l + 1_000), BlockVector2.at(l, l + 1_500), BlockVector2.at(10, 1_510),
                BlockVector2.at(10, 2_000), BlockVector2.at(l, l + 2_000), BlockVector2.at(l, l + 2_500), BlockVector2.at(0, 2_500)
        );

        var result = RegionRenderer.render(new ProtectedPolygonalRegion("arms", points, 0, 10));

        Assertions.assertNotNull(result);
        Assertions.assertEquals(points.size(), result.areas().getFirst().shape().getPoints().length);
    }

    @Test
    void testRenderPolygonDrawsStraightEdgesWhenTooWideToRasterize() {
        var region = new ProtectedPolygonalRegion(
                "huge",
                List.of(
                        BlockVector2.at(0, 0),
                        BlockVector2.at(1_000_000, 0),
                        BlockVector2.at(0, 1_000_000)
                ),
                0,
                10
        );

        var result = RegionRenderer.render(region);

        Assertions.assertNotNull(result);
        Assertions.assertArrayEquals(
                new Vector2d[]{
                        new Vector2d(0, 0),
                        new Vector2d(1_000_000, 0),
                        new Vector2d(0, 1_000_000)
                },
                result.areas().getFirst().shape().getPoints()
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
                        new Vector2d(2, 1),
                        new Vector2d(2, 4),
                        new Vector2d(1, 4)
                },
                result.areas().getFirst().shape().getPoints()
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
        Assertions.assertEquals(6, result.areas().getFirst().shape().getPointCount());
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
