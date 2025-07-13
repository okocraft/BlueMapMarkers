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

import java.util.ArrayList;
import java.util.List;

final class RegionRenderer {

    record Result(Shape shape, int minY, int maxY) {
    }

    static @Nullable Result render(ProtectedRegion region) {
        var min = region.getMinimumPoint();
        var max = region.getMaximumPoint();

        int minY = Math.min(min.y(), max.y());
        int maxY = Math.max(min.y(), max.y());

        if (region instanceof ProtectedCuboidRegion) {
            return new Result(
                    new Shape(
                            new Vector2d(min.x(), min.z()),
                            new Vector2d(max.x() + 1, min.z()),
                            new Vector2d(max.x() + 1, max.z() + 1),
                            new Vector2d(min.x(), max.z() + 1)
                    ), minY, maxY);
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

            return new Result(new Shape(expandPolygonXZByOne(pointsRef)), minY, maxY);
        }

        return null;
    }

    // Original: https://github.com/okocraft/Dynmap-WorldGuard/blob/master/src/main/java/org/dynmap/worldguard/UpdateTask.java#L146-L241
    // Optimized: Reduce object creations (List copies, BlockVector2 -> Vector2d, and more...)

    private static double cross(BlockVector2 p1, BlockVector2 p2) {
        return p1.x() * p2.z() - p1.z() * p2.x();
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
                        new Vector2d(min.x(), min.z()),
                        new Vector2d(max.x(), max.z() + 1),
                        new Vector2d(max.x() + 1, max.z() + 1),
                        new Vector2d(min.x() + 1, min.z())
                };
            } else {
                return new Vector2d[]{
                        new Vector2d(min.x(), min.z()),
                        new Vector2d(max.x() + 1, max.z()),
                        new Vector2d(max.x() + 1, max.z() + 1),
                        new Vector2d(min.x(), min.z() + 1)
                };
            }
        }

        if (loop != 1) {
            pointsRef = points.reverse();
        }

        List<BlockVector2> pointAdded = new ArrayList<>();

        for (int i = 0, size = pointsRef.size(); i < size; i++) {
            BlockVector2 prev = pointsRef.get((i - 1 + size) % size);
            BlockVector2 cur = pointsRef.get(i);
            BlockVector2 next = pointsRef.get((i + 1) % size);

            pointAdded.add(cur);

            if (cross(cur.subtract(prev), next.subtract(cur)) == 0 && cur.subtract(prev).dot(next.subtract(cur)) < 0) {
                pointAdded.add(cur);
            }
        }

        Vector2d[] result = new Vector2d[pointAdded.size()];

        for (int i = 0, size = pointAdded.size(); i < size; i++) {
            BlockVector2 prev = pointAdded.get((i - 1 + size) % size);
            BlockVector2 cur = pointAdded.get(i);
            BlockVector2 next = pointAdded.get((i + 1) % size);

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

            result[i] = new Vector2d(xCurNew, zCurNew);
        }

        return result;
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

    private RegionRenderer() {
        throw new UnsupportedOperationException();
    }
}
