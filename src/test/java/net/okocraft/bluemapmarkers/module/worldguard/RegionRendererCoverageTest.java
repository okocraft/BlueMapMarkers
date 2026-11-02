package net.okocraft.bluemapmarkers.module.worldguard;

import com.flowpowered.math.vector.Vector2d;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bounded-exhaustive checks for the claim FV-001 recorded in {@code verification/README.md}.
 *
 * <p>Claim: for every supported region in the enumerated domain, a block column {@code (x, z)} is drawn by the
 * rendered BlueMap shape (winding number around the block centre {@code (x + 0.5, z + 0.5)} is non-zero) if and only
 * if WorldGuard reports that the region contains the block, and the shape never covers a block twice
 * (|winding number| &lt;= 1).
 *
 * <p>WorldGuard's own {@code contains} implementation is used as the oracle, so the expected value is computed
 * independently of {@link RegionRenderer}. These are executed tests over finite domains, not a proof.
 */
class RegionRendererCoverageTest {

    private static final int MIN_Y = -64;
    private static final int MAX_Y = 320;

    /**
     * Translations applied to every enumerated shape: the origin and both corners of the Minecraft world border
     * (block coordinates are within [-30,000,000, 29,999,999]).
     */
    private static final int[][] TRANSLATIONS = {
            {0, 0},
            {-30_000_000, -30_000_000},
            {29_999_990, 29_999_990},
            {-30_000_000, 29_999_990},
    };

    /** Polyomino domain: every simple rectilinear polygon formed by unit cells inside a GRID x GRID square. */
    private static final int GRID = 4;

    // ---- Cuboid regions -------------------------------------------------------------------------------------

    @Test
    void testCuboidCoverageEqualsWorldGuardContainsForAllSmallCorners() {
        int checked = 0;
        for (int[] t : TRANSLATIONS) {
            for (int x1 = -2; x1 <= 2; x1++) {
                for (int z1 = -2; z1 <= 2; z1++) {
                    for (int x2 = -2; x2 <= 2; x2++) {
                        for (int z2 = -2; z2 <= 2; z2++) {
                            var region = new ProtectedCuboidRegion(
                                    "cuboid",
                                    BlockVector3.at(t[0] + x1, MAX_Y, t[1] + z1),
                                    BlockVector3.at(t[0] + x2, MIN_Y, t[1] + z2)
                            );
                            assertCoverageMatches(region);
                            checked++;
                        }
                    }
                }
            }
        }
        Assertions.assertEquals(TRANSLATIONS.length * 625, checked);
    }

    @Test
    void testCuboidVerticalRangeIsNormalized() {
        var region = new ProtectedCuboidRegion("cuboid", BlockVector3.at(0, 100, 0), BlockVector3.at(1, -5, 1));

        var result = RegionRenderer.render(region);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(-5, result.minY());
        Assertions.assertEquals(100, result.maxY());
    }

    @Test
    void testCuboidSpanningWholeWorldBorderUsesInclusiveBounds() {
        var region = new ProtectedCuboidRegion(
                "world",
                BlockVector3.at(-30_000_000, MIN_Y, -30_000_000),
                BlockVector3.at(29_999_999, MAX_Y, 29_999_999)
        );

        var result = RegionRenderer.render(region);

        Assertions.assertNotNull(result);
        Assertions.assertArrayEquals(
                new Vector2d[]{
                        new Vector2d(-30_000_000, -30_000_000),
                        new Vector2d(30_000_000, -30_000_000),
                        new Vector2d(30_000_000, 30_000_000),
                        new Vector2d(-30_000_000, 30_000_000)
                },
                result.shape().getPoints()
        );
    }

    // ---- Simple rectilinear polygons ------------------------------------------------------------------------

    @Test
    void testRectilinearPolygonCoverageEqualsWorldGuardContains() {
        List<List<BlockVector2>> outlines = enumerateRectilinearOutlines(GRID);
        // Sanity check of the generator: the domain must be non-trivial and include non-convex shapes.
        Assertions.assertTrue(outlines.size() > 1000, "unexpectedly small domain: " + outlines.size());

        int checked = 0;
        for (List<BlockVector2> unitSteps : outlines) {
            List<BlockVector2> corners = removeStraightThroughVertices(unitSteps);
            for (List<BlockVector2> variant : List.of(corners, unitSteps)) {
                for (List<BlockVector2> oriented : List.of(variant, reversed(variant))) {
                    // Every start vertex at the origin; the large translations use the canonical start vertex only.
                    for (int start = 0; start < oriented.size(); start++) {
                        assertCoverageMatches(polygon(rotate(oriented, start)));
                        checked++;
                    }
                    for (int[] t : TRANSLATIONS) {
                        assertCoverageMatches(polygon(translate(oriented, t[0], t[1])));
                        checked++;
                    }
                }
            }
        }
        Assertions.assertTrue(checked > outlines.size());
    }

    // ---- Collapsed (zero-area) polygons ---------------------------------------------------------------------

    @Test
    void testAxisAlignedCollapsedPolygonCoverageEqualsWorldGuardContains() {
        int checked = 0;
        for (int length = 3; length <= 4; length++) {
            int[] values = new int[length];
            int combinations = (int) Math.pow(4, length);
            for (int code = 0; code < combinations; code++) {
                for (int i = 0, c = code; i < length; i++, c /= 4) {
                    values[i] = c % 4;
                }
                for (int[] t : TRANSLATIONS) {
                    var horizontal = new ArrayList<BlockVector2>(length);
                    var vertical = new ArrayList<BlockVector2>(length);
                    for (int value : values) {
                        horizontal.add(BlockVector2.at(t[0] + value, t[1] + 7));
                        vertical.add(BlockVector2.at(t[0] + 7, t[1] + value));
                    }
                    assertCoverageMatches(polygon(horizontal));
                    assertCoverageMatches(polygon(vertical));
                    checked += 2;
                }
            }
        }
        Assertions.assertEquals(TRANSLATIONS.length * 2 * (64 + 256), checked);
    }

    @Test
    void testBacktrackingSpikeCoverageEqualsWorldGuardContains() {
        var region = polygon(List.of(
                BlockVector2.at(0, 0),
                BlockVector2.at(2, 0),
                BlockVector2.at(1, 0),
                BlockVector2.at(1, 2),
                BlockVector2.at(0, 2)
        ));

        assertCoverageMatches(region);
    }

    // ---- Long edges (arithmetic range) ----------------------------------------------------------------------

    /**
     * Metamorphic property for long edges, where the block-by-block oracle is too expensive: inserting a redundant
     * vertex in the middle of every edge must not change the rendered outline except for the corresponding
     * redundant (straight-through) vertices. Exercises edge vectors whose dot product exceeds {@code int} range.
     */
    @Test
    void testRedundantMidpointsDoNotChangeOutlineOfLongEdges() {
        List<List<BlockVector2>> outlines = enumerateRectilinearOutlines(3);
        int[] scales = {2, 1_000, 50_000, 2_000_000};

        for (List<BlockVector2> unitSteps : outlines) {
            List<BlockVector2> corners = removeStraightThroughVertices(unitSteps);
            for (int scale : scales) {
                for (List<BlockVector2> oriented : List.of(corners, reversed(corners))) {
                    List<BlockVector2> scaled = translate(scale(oriented, scale), -3 * scale / 2, -3 * scale / 2);
                    List<BlockVector2> withMidpoints = insertMidpoints(scaled);

                    var expected = RegionRenderer.render(polygon(scaled));
                    var actual = RegionRenderer.render(polygon(withMidpoints));

                    Assertions.assertNotNull(expected);
                    Assertions.assertNotNull(actual);
                    Assertions.assertEquals(
                            Arrays.asList(expected.shape().getPoints()),
                            removeStraightThroughPoints(Arrays.asList(actual.shape().getPoints())),
                            () -> "outline changed by redundant midpoints: " + withMidpoints
                    );
                }
            }
        }
    }

    // ---- Oracle ---------------------------------------------------------------------------------------------

    private static void assertCoverageMatches(ProtectedRegion region) {
        var result = RegionRenderer.render(region);
        Assertions.assertNotNull(result, () -> "not rendered: " + describe(region));

        BlockVector3 min = region.getMinimumPoint();
        BlockVector3 max = region.getMaximumPoint();
        Assertions.assertEquals(min.y(), result.minY(), () -> describe(region));
        Assertions.assertEquals(max.y(), result.maxY(), () -> describe(region));

        Vector2d[] shape = result.shape().getPoints();
        for (int x = min.x() - 2; x <= max.x() + 2; x++) {
            for (int z = min.z() - 2; z <= max.z() + 2; z++) {
                boolean expected = region.contains(BlockVector3.at(x, min.y(), z));
                int winding = windingNumber(shape, x + 0.5, z + 0.5);
                int bx = x;
                int bz = z;
                Assertions.assertTrue(
                        Math.abs(winding) <= 1,
                        () -> "block (" + bx + ", " + bz + ") covered " + winding + " times: " + describe(region)
                                + " -> " + Arrays.toString(shape)
                );
                Assertions.assertEquals(
                        expected, winding != 0,
                        () -> "block (" + bx + ", " + bz + ") expected " + (expected ? "inside" : "outside") + ": "
                                + describe(region) + " -> " + Arrays.toString(shape)
                );
            }
        }
    }

    /** Winding number of a closed polygon around a point that never lies on its boundary. */
    private static int windingNumber(Vector2d[] polygon, double px, double pz) {
        int winding = 0;
        for (int i = 0; i < polygon.length; i++) {
            Vector2d a = polygon[i];
            Vector2d b = polygon[(i + 1) % polygon.length];
            double side = (b.getX() - a.getX()) * (pz - a.getY()) - (px - a.getX()) * (b.getY() - a.getY());
            if (a.getY() <= pz) {
                if (b.getY() > pz && side > 0) {
                    winding++;
                }
            } else if (b.getY() <= pz && side < 0) {
                winding--;
            }
        }
        return winding;
    }

    // ---- Domain generation ----------------------------------------------------------------------------------

    /**
     * Enumerates outlines of every non-empty, 4-connected, hole-free set of unit cells within a {@code size} x
     * {@code size} grid that has no diagonal pinch, i.e. every simple rectilinear polygon of that extent. Each
     * outline contains every integer point on its boundary (unit steps).
     */
    private static List<List<BlockVector2>> enumerateRectilinearOutlines(int size) {
        List<List<BlockVector2>> outlines = new ArrayList<>();
        for (int mask = 1; mask < (1 << (size * size)); mask++) {
            if (isSimplePolyomino(mask, size)) {
                outlines.add(traceOutline(mask, size));
            }
        }
        return outlines;
    }

    private static boolean filled(int mask, int size, int x, int z) {
        return 0 <= x && x < size && 0 <= z && z < size && (mask & (1 << (z * size + x))) != 0;
    }

    private static boolean isSimplePolyomino(int mask, int size) {
        // No diagonal pinch in any 2x2 window (including windows overlapping the outside).
        for (int x = -1; x < size; x++) {
            for (int z = -1; z < size; z++) {
                boolean a = filled(mask, size, x, z);
                boolean b = filled(mask, size, x + 1, z);
                boolean c = filled(mask, size, x, z + 1);
                boolean d = filled(mask, size, x + 1, z + 1);
                if (a == d && b == c && a != b) {
                    return false;
                }
            }
        }
        // Filled cells are 4-connected, and empty cells are all 4-connected to the outside (no holes).
        int filledCount = Integer.bitCount(mask);
        int start = Integer.numberOfTrailingZeros(mask);
        if (countReachable(mask, size, start % size, start / size, true) != filledCount) {
            return false;
        }
        int padded = (size + 2) * (size + 2);
        return countReachable(mask, size, -1, -1, false) == padded - filledCount;
    }

    private static int countReachable(int mask, int size, int startX, int startZ, boolean target) {
        boolean[][] seen = new boolean[size + 2][size + 2];
        var queue = new ArrayDeque<int[]>();
        queue.add(new int[]{startX, startZ});
        seen[startX + 1][startZ + 1] = true;
        int count = 0;
        int[][] steps = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!queue.isEmpty()) {
            int[] cur = queue.poll();
            count++;
            for (int[] step : steps) {
                int nx = cur[0] + step[0];
                int nz = cur[1] + step[1];
                if (nx < -1 || nx > size || nz < -1 || nz > size || seen[nx + 1][nz + 1]) {
                    continue;
                }
                if (filled(mask, size, nx, nz) == target) {
                    seen[nx + 1][nz + 1] = true;
                    queue.add(new int[]{nx, nz});
                }
            }
        }
        return count;
    }

    private static List<BlockVector2> traceOutline(int mask, int size) {
        Map<BlockVector2, BlockVector2> next = new HashMap<>();
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                if (!filled(mask, size, x, z)) {
                    continue;
                }
                if (!filled(mask, size, x, z - 1)) {
                    putEdge(next, x, z, x + 1, z);
                }
                if (!filled(mask, size, x + 1, z)) {
                    putEdge(next, x + 1, z, x + 1, z + 1);
                }
                if (!filled(mask, size, x, z + 1)) {
                    putEdge(next, x + 1, z + 1, x, z + 1);
                }
                if (!filled(mask, size, x - 1, z)) {
                    putEdge(next, x, z + 1, x, z);
                }
            }
        }
        BlockVector2 start = next.keySet().iterator().next();
        List<BlockVector2> outline = new ArrayList<>();
        BlockVector2 cur = start;
        do {
            outline.add(cur);
            cur = next.get(cur);
        } while (!cur.equals(start));
        Assertions.assertEquals(next.size(), outline.size(), "outline is not a single simple loop");
        return outline;
    }

    private static void putEdge(Map<BlockVector2, BlockVector2> next, int x1, int z1, int x2, int z2) {
        Assertions.assertNull(next.put(BlockVector2.at(x1, z1), BlockVector2.at(x2, z2)), "pinched outline");
    }

    // ---- Polygon helpers ------------------------------------------------------------------------------------

    private static ProtectedPolygonalRegion polygon(List<BlockVector2> points) {
        return new ProtectedPolygonalRegion("polygon", points, MIN_Y, MAX_Y);
    }

    private static List<BlockVector2> removeStraightThroughVertices(List<BlockVector2> points) {
        List<BlockVector2> result = new ArrayList<>();
        int size = points.size();
        for (int i = 0; i < size; i++) {
            BlockVector2 prev = points.get((i - 1 + size) % size);
            BlockVector2 cur = points.get(i);
            BlockVector2 next = points.get((i + 1) % size);
            if (!isStraightThrough(prev.x(), prev.z(), cur.x(), cur.z(), next.x(), next.z())) {
                result.add(cur);
            }
        }
        return result;
    }

    /**
     * Removes geometrically redundant points: consecutive duplicates (zero-length edges) and points lying strictly
     * between their neighbours. Backtracking spikes are kept, so a spurious spike still changes the outline.
     */
    private static List<Vector2d> removeStraightThroughPoints(List<Vector2d> input) {
        List<Vector2d> points = new ArrayList<>();
        for (int i = 0; i < input.size(); i++) {
            if (!input.get(i).equals(input.get((i + 1) % input.size()))) {
                points.add(input.get(i));
            }
        }
        List<Vector2d> result = new ArrayList<>();
        int size = points.size();
        for (int i = 0; i < size; i++) {
            Vector2d prev = points.get((i - 1 + size) % size);
            Vector2d cur = points.get(i);
            Vector2d next = points.get((i + 1) % size);
            if (!isStraightThrough(
                    (long) prev.getX(), (long) prev.getY(),
                    (long) cur.getX(), (long) cur.getY(),
                    (long) next.getX(), (long) next.getY())) {
                result.add(cur);
            }
        }
        return result;
    }

    /** Whether {@code cur} lies strictly between {@code prev} and {@code next} on a straight line. */
    private static boolean isStraightThrough(long px, long pz, long cx, long cz, long nx, long nz) {
        long inX = cx - px;
        long inZ = cz - pz;
        long outX = nx - cx;
        long outZ = nz - cz;
        return inX * outZ - inZ * outX == 0 && inX * outX + inZ * outZ > 0;
    }

    private static List<BlockVector2> insertMidpoints(List<BlockVector2> points) {
        List<BlockVector2> result = new ArrayList<>();
        for (int i = 0; i < points.size(); i++) {
            BlockVector2 cur = points.get(i);
            BlockVector2 next = points.get((i + 1) % points.size());
            result.add(cur);
            result.add(BlockVector2.at((cur.x() + next.x()) / 2, (cur.z() + next.z()) / 2));
        }
        return result;
    }

    private static List<BlockVector2> reversed(List<BlockVector2> points) {
        List<BlockVector2> result = new ArrayList<>(points);
        java.util.Collections.reverse(result);
        return result;
    }

    private static List<BlockVector2> rotate(List<BlockVector2> points, int start) {
        List<BlockVector2> result = new ArrayList<>(points.size());
        for (int i = 0; i < points.size(); i++) {
            result.add(points.get((start + i) % points.size()));
        }
        return result;
    }

    private static List<BlockVector2> translate(List<BlockVector2> points, int dx, int dz) {
        return points.stream().map(p -> BlockVector2.at(p.x() + dx, p.z() + dz)).toList();
    }

    private static List<BlockVector2> scale(List<BlockVector2> points, int factor) {
        return points.stream().map(p -> BlockVector2.at(p.x() * factor, p.z() * factor)).toList();
    }

    private static String describe(ProtectedRegion region) {
        if (region instanceof ProtectedPolygonalRegion) {
            return "polygon " + region.getPoints();
        }
        return "cuboid " + region.getMinimumPoint() + " .. " + region.getMaximumPoint();
    }
}
