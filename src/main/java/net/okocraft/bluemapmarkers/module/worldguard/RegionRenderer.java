package net.okocraft.bluemapmarkers.module.worldguard;

import com.flowpowered.math.vector.Vector2d;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import de.bluecolored.bluemap.api.math.Shape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Converts a WorldGuard region into BlueMap shapes that cover exactly the block columns the region protects.
 *
 * <p>Polygonal regions are rasterized column by column with the same rule as
 * {@code ProtectedPolygonalRegion#contains} (points on an edge are inside, otherwise crossing parity), and the
 * outline of the resulting set of blocks is traced. Diagonal edges therefore become block staircases, and
 * self-intersecting polygons are drawn as the blocks WorldGuard actually protects. A region whose blocks form several
 * separate pieces yields one {@link Area} per piece; enclosed unprotected blocks become holes.
 *
 * <p>Rendering runs on the main thread, so its cost is bounded: a polygon that exceeds one of the {@code MAX_*} limits
 * is drawn with straight edges through its vertices instead, which is approximate.
 */
final class RegionRenderer {

    /** Columns evaluated one by one (columns spanned by a sloped edge, plus one per vertex x). */
    static final int MAX_EVALUATED_COLUMNS = 100_000;
    /** Evaluated columns multiplied by the number of polygon edges, each of which is checked in every column. */
    static final long MAX_EDGE_CHECKS = 5_000_000;
    /** Protected z ranges over all column runs; tracing costs about four outline corners per range. */
    static final int MAX_RANGES = 10_000;

    /**
     * One connected piece of a rendered region.
     *
     * @param shape the outer outline
     * @param holes outlines of unprotected areas enclosed by {@code shape}
     */
    record Area(@NotNull Shape shape, @NotNull List<Shape> holes) {
    }

    /**
     * @param areas the pieces of the region, never empty
     * @param minY  the lowest protected block layer
     * @param maxY  the highest protected block layer
     */
    record Result(@NotNull List<Area> areas, int minY, int maxY) {
    }

    static @Nullable Result render(ProtectedRegion region) {
        var min = region.getMinimumPoint();
        var max = region.getMaximumPoint();

        int minY = Math.min(min.y(), max.y());
        int maxY = Math.max(min.y(), max.y());

        if (region instanceof ProtectedCuboidRegion) {
            var shape = new Shape(
                    point(min.x(), min.z()),
                    point(max.x() + 1, min.z()),
                    point(max.x() + 1, max.z() + 1),
                    point(min.x(), max.z() + 1)
            );
            return new Result(List.of(new Area(shape, List.of())), minY, maxY);
        } else if (region instanceof ProtectedPolygonalRegion) {
            var points = region.getPoints();

            if (points.size() < 3) {
                return null;
            }

            var runs = rasterize(points);
            if (runs == null) {
                return new Result(List.of(new Area(approximate(points), List.of())), minY, maxY);
            }

            var areas = traceAreas(runs);
            return areas.isEmpty() ? null : new Result(areas, minY, maxY);
        }

        return null;
    }

    // ---- Rasterization ------------------------------------------------------------------------------------------

    /**
     * Consecutive block columns {@code x0..x1} that contain the same protected z ranges.
     *
     * @param intervals inclusive z ranges as {@code [from0, to0, from1, to1, ...]}, sorted and non-adjacent
     */
    private record ColumnRun(int x0, int x1, int[] intervals) {
    }

    /** Returns {@code null} if the polygon exceeds one of the {@code MAX_*} limits. */
    private static @Nullable List<ColumnRun> rasterize(List<BlockVector2> points) {
        int size = points.size();
        int[] xs = new int[size];
        int[] zs = new int[size];
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (int i = 0; i < size; i++) {
            xs[i] = points.get(i).x();
            zs[i] = points.get(i).z();
            minX = Math.min(minX, xs[i]);
            maxX = Math.max(maxX, xs[i]);
            minZ = Math.min(minZ, zs[i]);
            maxZ = Math.max(maxZ, zs[i]);
        }

        int[] vertexXs = Arrays.stream(xs).distinct().sorted().toArray();
        if ((long) vertexXs.length * size > MAX_EDGE_CHECKS) {
            return null; // checked first, as finding the sloped columns below already costs this much
        }
        long evaluatedColumns = vertexXs.length;
        for (int i = 0; i + 1 < vertexXs.length; i++) {
            int from = vertexXs[i] + 1;
            int to = vertexXs[i + 1] - 1;
            if (from <= to && hasSlopedEdgeSpanning(xs, zs, from, to)) {
                evaluatedColumns += to - from + 1;
            }
        }
        if (evaluatedColumns > MAX_EVALUATED_COLUMNS || evaluatedColumns * size > MAX_EDGE_CHECKS) {
            return null;
        }

        List<ColumnRun> runs = new ArrayList<>();

        for (int i = 0; i < vertexXs.length; i++) {
            int x = vertexXs[i];
            addColumn(runs, x, x, column(xs, zs, x, minZ, maxZ));

            if (i + 1 == vertexXs.length || vertexXs[i + 1] == x + 1) {
                continue;
            }

            // Columns strictly between two vertex x coordinates. If only horizontal edges span them, every column is
            // identical and one evaluation covers the whole range; diagonal edges need one column at a time.
            int from = x + 1;
            int to = vertexXs[i + 1] - 1;
            if (hasSlopedEdgeSpanning(xs, zs, from, to)) {
                for (int column = from; column <= to; column++) {
                    addColumn(runs, column, column, column(xs, zs, column, minZ, maxZ));
                }
            } else {
                addColumn(runs, from, to, column(xs, zs, from, minZ, maxZ));
            }
        }

        long ranges = 0;
        for (var run : runs) {
            ranges += run.intervals().length / 2;
        }
        return ranges > MAX_RANGES ? null : runs;
    }

    /**
     * Draws the polygon with straight edges through its vertices. If all vertices lie on one line, the outline would
     * have no area, so the line is drawn one block wide instead.
     */
    private static Shape approximate(List<BlockVector2> points) {
        var low = points.getFirst();
        var high = low;
        for (var p : points) {
            if (p.x() < low.x() || p.x() == low.x() && p.z() < low.z()) {
                low = p;
            }
            if (p.x() > high.x() || p.x() == high.x() && p.z() > high.z()) {
                high = p;
            }
        }
        boolean collinear = true;
        for (var p : points) {
            collinear &= (long) (high.x() - low.x()) * (p.z() - low.z()) == (long) (high.z() - low.z()) * (p.x() - low.x());
        }
        if (!collinear) {
            return new Shape(points.stream().map(p -> point(p.x(), p.z())).toList());
        }
        // Outline of the blocks swept from low to high; low is west of (or north of) high.
        if (low.z() <= high.z()) {
            return new Shape(
                    point(low.x(), low.z()), point(low.x() + 1, low.z()),
                    point(high.x() + 1, high.z()), point(high.x() + 1, high.z() + 1),
                    point(high.x(), high.z() + 1), point(low.x(), low.z() + 1)
            );
        }
        return new Shape(
                point(low.x(), low.z()), point(high.x(), high.z()),
                point(high.x() + 1, high.z()), point(high.x() + 1, high.z() + 1),
                point(low.x() + 1, low.z() + 1), point(low.x(), low.z() + 1)
        );
    }

    private static void addColumn(List<ColumnRun> runs, int x0, int x1, int[] intervals) {
        if (!runs.isEmpty()) {
            var last = runs.getLast();
            if (last.x1() + 1 == x0 && Arrays.equals(last.intervals(), intervals)) {
                runs.set(runs.size() - 1, new ColumnRun(last.x0(), x1, intervals));
                return;
            }
        }
        runs.add(new ColumnRun(x0, x1, intervals));
    }

    private static boolean hasSlopedEdgeSpanning(int[] xs, int[] zs, int from, int to) {
        for (int i = 0, size = xs.length; i < size; i++) {
            int j = (i - 1 + size) % size;
            if (zs[i] != zs[j] && Math.min(xs[i], xs[j]) < from && to < Math.max(xs[i], xs[j])) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the z ranges of column {@code x} that WorldGuard's {@code ProtectedPolygonalRegion#contains} accepts.
     *
     * <p>WorldGuard walks the edges with a vertical ray: a block is inside if it lies on an edge, or if an odd number of
     * edges with {@code x1 < x <= x2} pass strictly above it ({@code z < zEdge(x)}). For an integer z,
     * {@code z < zEdge} is equivalent to {@code z < ceil(zEdge)}, so exact integer thresholds suffice.
     */
    private static int[] column(int[] xs, int[] zs, int x, int minZ, int maxZ) {
        int size = xs.length;
        long[] thresholds = new long[size];
        int thresholdCount = 0;
        List<long[]> ranges = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            int j = (i - 1 + size) % size;
            // Same edge orientation as WorldGuard: (x1, z1) is the end with the smaller x.
            int x1;
            int z1;
            int x2;
            int z2;
            if (xs[i] > xs[j]) {
                x1 = xs[j];
                z1 = zs[j];
                x2 = xs[i];
                z2 = zs[i];
            } else {
                x1 = xs[i];
                z1 = zs[i];
                x2 = xs[j];
                z2 = zs[j];
            }

            if (x < x1 || x2 < x) {
                continue;
            }

            if (x1 == x2) {
                ranges.add(new long[]{Math.min(z1, z2), Math.max(z1, z2)});
                continue;
            }

            long den = (long) x2 - x1;
            long num = (long) z1 * den + ((long) z2 - z1) * ((long) x - x1);
            if (num % den == 0) {
                long onEdge = num / den;
                ranges.add(new long[]{onEdge, onEdge});
            }
            if (x != x1) {
                thresholds[thresholdCount++] = Math.ceilDiv(num, den);
            }
        }

        Arrays.sort(thresholds, 0, thresholdCount);
        // z in [t_k, t_{k+1}) is below (thresholdCount - k - 1) crossings; inside when that count is odd.
        for (int k = thresholdCount % 2 == 0 ? 0 : 1; k + 1 < thresholdCount; k += 2) {
            if (thresholds[k] < thresholds[k + 1]) {
                ranges.add(new long[]{thresholds[k], thresholds[k + 1] - 1});
            }
        }

        ranges.sort((a, b) -> Long.compare(a[0], b[0]));
        int[] merged = new int[ranges.size() * 2];
        int count = 0;
        for (long[] range : ranges) {
            long from = Math.max(range[0], minZ);
            long to = Math.min(range[1], maxZ);
            if (from > to) {
                continue;
            }
            if (count > 0 && from <= (long) merged[count - 1] + 1) {
                merged[count - 1] = (int) Math.max(merged[count - 1], to);
            } else {
                merged[count++] = (int) from;
                merged[count++] = (int) to;
            }
        }
        return Arrays.copyOf(merged, count);
    }

    // ---- Outline tracing ----------------------------------------------------------------------------------------

    /**
     * A boundary segment between protected and unprotected blocks. The protected side is to the left of the direction
     * in screen coordinates (x east, z south), i.e. at {@code (-dz, dx)}.
     */
    private record Edge(int fromX, int fromZ, int toX, int toZ) {
        int dx() {
            return Integer.signum(this.toX - this.fromX);
        }

        int dz() {
            return Integer.signum(this.toZ - this.fromZ);
        }
    }

    private static List<Area> traceAreas(List<ColumnRun> runs) {
        List<Edge> edges = new ArrayList<>();
        int[] previous = new int[0];
        int previousEnd = Integer.MIN_VALUE;

        for (var run : runs) {
            // Columns are contiguous from the first to the last vertex x; an empty column is a run without intervals.
            // Horizontal edges first, so that each outline starts at the top of its west-most column, heading east.
            for (int k = 0; k < run.intervals().length; k += 2) {
                int top = run.intervals()[k];
                int bottom = run.intervals()[k + 1] + 1;
                edges.add(new Edge(run.x0(), top, run.x1() + 1, top));
                edges.add(new Edge(run.x1() + 1, bottom, run.x0(), bottom));
            }
            addVerticalEdges(edges, run.x0(), previousEnd + 1 == run.x0() ? previous : new int[0], run.intervals());
            previous = run.intervals();
            previousEnd = run.x1();
        }
        if (!runs.isEmpty()) {
            addVerticalEdges(edges, previousEnd + 1, previous, new int[0]);
        }

        Map<Long, List<Edge>> outgoing = new HashMap<>();
        for (var edge : edges) {
            outgoing.computeIfAbsent(key(edge.fromX(), edge.fromZ()), ignored -> new ArrayList<>(2)).add(edge);
        }

        List<List<Edge>> outers = new ArrayList<>();
        List<List<Edge>> holes = new ArrayList<>();
        var visited = new java.util.HashSet<Edge>();

        for (var start : edges) {
            if (visited.contains(start)) {
                continue;
            }
            List<Edge> loop = new ArrayList<>();
            var current = start;
            while (visited.add(current)) {
                loop.add(current);
                current = next(current, outgoing.get(key(current.toX(), current.toZ())));
                if (current == null) {
                    throw new IllegalStateException("open outline at " + loop.getLast());
                }
            }
            if (!current.equals(start)) {
                throw new IllegalStateException("outline does not close at " + start);
            }
            (signedArea(loop) > 0 ? outers : holes).add(loop);
        }

        List<Area> areas = new ArrayList<>(outers.size());
        List<List<Shape>> holesByOuter = new ArrayList<>(outers.size());
        double[] outerAreas = new double[outers.size()];
        for (int i = 0; i < outers.size(); i++) {
            outerAreas[i] = signedArea(outers.get(i));
            holesByOuter.add(new ArrayList<>());
        }

        for (var hole : holes) {
            // Centre of the protected block next to the hole's first edge; the smallest outer outline containing it
            // belongs to the same piece.
            var edge = hole.getFirst();
            double probeX = edge.fromX() + 0.5 * edge.dx() - 0.5 * edge.dz();
            double probeZ = edge.fromZ() + 0.5 * edge.dz() + 0.5 * edge.dx();
            int owner = -1;
            for (int i = 0; i < outers.size(); i++) {
                if ((owner == -1 || outerAreas[i] < outerAreas[owner]) && contains(outers.get(i), probeX, probeZ)) {
                    owner = i;
                }
            }
            if (owner == -1) {
                throw new IllegalStateException("hole outside of every outline at " + edge);
            }
            holesByOuter.get(owner).add(toShape(hole));
        }

        for (int i = 0; i < outers.size(); i++) {
            areas.add(new Area(toShape(outers.get(i)), List.copyOf(holesByOuter.get(i))));
        }
        return areas;
    }

    /** Adds the vertical boundary at {@code x} between the column ranges on its west and east side. */
    private static void addVerticalEdges(List<Edge> edges, int x, int[] west, int[] east) {
        // Protected only on the east side: the protected side is on the left when walking north (-z).
        for (int[] range : difference(east, west)) {
            edges.add(new Edge(x, range[1] + 1, x, range[0]));
        }
        // Protected only on the west side: walk south (+z).
        for (int[] range : difference(west, east)) {
            edges.add(new Edge(x, range[0], x, range[1] + 1));
        }
    }

    /** Returns the inclusive ranges contained in {@code a} but not in {@code b}. */
    private static List<int[]> difference(int[] a, int[] b) {
        List<int[]> result = new ArrayList<>();
        int j = 0;
        for (int i = 0; i < a.length; i += 2) {
            long from = a[i];
            long to = a[i + 1];
            while (j < b.length && b[j + 1] < from) {
                j += 2;
            }
            int k = j;
            while (from <= to && k < b.length && b[k] <= to) {
                if (b[k] > from) {
                    result.add(new int[]{(int) from, b[k] - 1});
                }
                from = Math.max(from, (long) b[k + 1] + 1);
                k += 2;
            }
            if (from <= to) {
                result.add(new int[]{(int) from, (int) to});
            }
        }
        return result;
    }

    /**
     * Picks the edge that continues the outline. Where two protected blocks touch only at a corner, two outlines meet
     * at one vertex; turning towards the other block keeps diagonally touching blocks in one piece.
     */
    private static @Nullable Edge next(Edge incoming, @Nullable List<Edge> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }
        if (candidates.size() == 1) {
            return candidates.getFirst();
        }
        for (var candidate : candidates) {
            long cross = (long) incoming.dx() * candidate.dz() - (long) incoming.dz() * candidate.dx();
            if (cross < 0) {
                return candidate;
            }
        }
        return null;
    }

    private static long key(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    private static double signedArea(List<Edge> loop) {
        var origin = loop.getFirst();
        double area = 0;
        for (var edge : loop) {
            double ax = (double) edge.fromX() - origin.fromX();
            double az = (double) edge.fromZ() - origin.fromZ();
            double bx = (double) edge.toX() - origin.fromX();
            double bz = (double) edge.toZ() - origin.fromZ();
            area += ax * bz - az * bx;
        }
        return area / 2;
    }

    private static boolean contains(List<Edge> loop, double px, double pz) {
        int winding = 0;
        for (var edge : loop) {
            double ax = edge.fromX();
            double az = edge.fromZ();
            double bx = edge.toX();
            double bz = edge.toZ();
            double side = (bx - ax) * (pz - az) - (px - ax) * (bz - az);
            if (az <= pz) {
                if (bz > pz && side > 0) {
                    winding++;
                }
            } else if (bz <= pz && side < 0) {
                winding--;
            }
        }
        return winding != 0;
    }

    /** Keeps only the corners: vertices where the outline changes direction. */
    private static Shape toShape(List<Edge> loop) {
        List<Vector2d> corners = new ArrayList<>();
        int size = loop.size();
        for (int i = 0; i < size; i++) {
            var previous = loop.get((i - 1 + size) % size);
            var current = loop.get(i);
            if (previous.dx() != current.dx() || previous.dz() != current.dz()) {
                corners.add(point(current.fromX(), current.fromZ()));
            }
        }
        return new Shape(corners);
    }

    /**
     * Creates a {@link Vector2d} from block coordinates without precision loss.
     * <p>
     * {@code new Vector2d(int, int)} resolves to {@code Vector2d(float, float)}, which cannot represent every
     * block coordinate beyond 2^24 (e.g. {@code 29_999_999} becomes {@code 3.0E7}).
     */
    private static Vector2d point(int x, int z) {
        return new Vector2d((double) x, (double) z);
    }

    private RegionRenderer() {
        throw new UnsupportedOperationException();
    }
}
