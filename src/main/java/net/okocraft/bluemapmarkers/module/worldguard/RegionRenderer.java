package net.okocraft.bluemapmarkers.module.worldguard;

import com.flowpowered.math.vector.Vector2d;
import com.google.common.collect.ImmutableList;
import com.sk89q.worldedit.math.BlockVector2;
import com.sk89q.worldedit.regions.Polygonal2DRegion;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import com.sk89q.worldguard.protection.regions.ProtectedPolygonalRegion;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import de.bluecolored.bluemap.api.math.Shape;
import org.jetbrains.annotations.Nullable;

import java.util.List;

final class RegionRenderer {

    /**
     * One connected piece of a rendered region.
     *
     * @param shape the outer outline
     * @param holes outlines of unprotected areas enclosed by {@code shape}
     */
    record Area(Shape shape, List<Shape> holes) {
    }

    /**
     * @param areas the pieces of the region, never empty
     * @param minY  the lowest protected block layer
     * @param maxY  the highest protected block layer
     */
    record Result(List<Area> areas, int minY, int maxY) {
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

            ImmutableList<BlockVector2> pointsRef;

            if (points instanceof ImmutableList<BlockVector2> immutableList) {
                pointsRef = immutableList;
            } else {
                pointsRef = ImmutableList.copyOf(points);
            }

            var shape = new Shape(expandPolygonXZByOne(pointsRef));
            return new Result(List.of(new Area(shape, List.of())), minY, maxY);
        }

        return null;
    }

    // Original: https://github.com/okocraft/Dynmap-WorldGuard/blob/master/src/main/java/org/dynmap/worldguard/UpdateTask.java#L146-L241
    // Optimized: Reduce object creations (List copies, BlockVector2 -> Vector2d, and more...)

    private static double cross(BlockVector2 p1, BlockVector2 p2) {
        return (double) p1.x() * p2.z() - (double) p1.z() * p2.x();
    }

    private static Vector2d[] expandPolygonXZByOne(ImmutableList<BlockVector2> points) {
        ImmutableList<BlockVector2> pointsRef = points;

        int loop = getPolygonLoop(points);

        if (loop == 0) {
            Polygonal2DRegion poly2d = new Polygonal2DRegion(null, points, 0, 0);
            BlockVector2 max = poly2d.getMaximumPoint().toBlockVector2();
            BlockVector2 min = poly2d.getMinimumPoint().toBlockVector2();

            if (min.x() == max.x()) {
                return new Vector2d[]{
                        point(min.x(), min.z()),
                        point(max.x(), max.z() + 1),
                        point(max.x() + 1, max.z() + 1),
                        point(min.x() + 1, min.z())
                };
            } else {
                return new Vector2d[]{
                        point(min.x(), min.z()),
                        point(max.x() + 1, max.z()),
                        point(max.x() + 1, max.z() + 1),
                        point(min.x(), min.z() + 1)
                };
            }
        }

        if (loop != 1) {
            pointsRef = points.reverse();
        }

        int additionalPoints = 0;
        for (int i = 0, size = pointsRef.size(); i < size; i++) {
            BlockVector2 prev = pointsRef.get((i - 1 + size) % size);
            BlockVector2 cur = pointsRef.get(i);
            BlockVector2 next = pointsRef.get((i + 1) % size);
            if (isBacktracking(prev, cur, next)) {
                additionalPoints++;
            }
        }

        Vector2d[] result = new Vector2d[pointsRef.size() + additionalPoints];
        int resultIndex = 0;

        for (int i = 0, size = pointsRef.size(); i < size; i++) {
            BlockVector2 prev = pointsRef.get((i - 1 + size) % size);
            BlockVector2 cur = pointsRef.get(i);
            BlockVector2 next = pointsRef.get((i + 1) % size);
            boolean backtracking = isBacktracking(prev, cur, next);

            result[resultIndex++] = expandPoint(prev, cur, backtracking ? cur : next);
            if (backtracking) {
                result[resultIndex++] = expandPoint(cur, cur, next);
            }
        }

        return result;
    }

    private static boolean isBacktracking(BlockVector2 prev, BlockVector2 cur, BlockVector2 next) {
        int incomingX = cur.x() - prev.x();
        int incomingZ = cur.z() - prev.z();
        int outgoingX = next.x() - cur.x();
        int outgoingZ = next.z() - cur.z();

        double cross = (double) incomingX * outgoingZ - (double) incomingZ * outgoingX;
        long dot = (long) incomingX * outgoingX + (long) incomingZ * outgoingZ;
        return cross == 0 && dot < 0;
    }

    private static Vector2d expandPoint(BlockVector2 prev, BlockVector2 cur, BlockVector2 next) {
        int xPrev = prev.x();
        int zPrev = prev.z();
        int xCur = cur.x();
        int zCur = cur.z();
        int xNext = next.x();
        int zNext = next.z();

        int xCurNew = xCur;
        int zCurNew = zCur;

        if (zPrev < zCur || zCur < zNext || cur.equals(next) && xPrev < xCur || prev.equals(cur) && xNext < xCur) {
            xCurNew++;
        }
        if (xCur < xPrev || xNext < xCur || cur.equals(next) && zPrev < zCur || prev.equals(cur) && zNext < zCur) {
            zCurNew++;
        }

        return point(xCurNew, zCurNew);
    }

    /**
     * Calc loop direction of given polygon.
     *
     * @param points Polygon points.
     * @return When returns 1 it is clockwise, when returns -1 it is anticlockwise.
     * Other than that, polygon is collapsed.
     */
    private static int getPolygonLoop(List<BlockVector2> points) {
        double area = calcAreaOfPolygon(points);
        if (area > 0) {
            return 1;
        } else if (area < 0) {
            return -1;
        } else {
            return 0;
        }
    }

    private static double calcAreaOfPolygon(List<BlockVector2> points) {
        double area = 0;
        for (int i = 0; i < points.size(); i++) {
            area += cross(points.get(i), points.get((i + 1) % points.size()));
        }
        return area / 2.0;
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
