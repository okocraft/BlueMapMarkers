# Verification Records

Scoped verification records following the
[OKOCRAFT Formal Verification Guidelines](https://github.com/okocraft/docs/blob/main/dev/guidelines/formal-verification.md).
A record only covers the property, domain and assumptions written in it.

### FV-001: A WorldGuard region is drawn on BlueMap exactly over the blocks it protects

- Owner: BlueMapMarkers maintainers
- Target: `src/main/java/net/okocraft/bluemapmarkers/module/worldguard/RegionRenderer.java`, `RegionRenderer.render(ProtectedRegion)`
- Property:
  - **Coverage:** a block column `(x, z)` is drawn by the returned areas (its centre `(x + 0.5, z + 0.5)` is inside
    exactly one area's outer outline and none of that area's holes) **iff**
    `region.contains(BlockVector3.at(x, minY, z))` is `true` in WorldGuard.
  - **No overlap:** no outline winds around a block centre more than once, no block is covered by two areas, and every
    hole lies inside its area's outline.
  - **Vertical range:** `Result.minY()` / `Result.maxY()` equal the region's minimum / maximum Y.
  - **Redundant vertices:** inserting a vertex in the middle of every polygon edge does not change the result.
- Preconditions:
  - `ProtectedCuboidRegion`, or any `ProtectedPolygonalRegion` with at least 3 points: diagonal edges,
    self-intersections, collinear and repeated points are all in scope.
  - The polygon is within the cost limits `RegionRenderer.MAX_EVALUATED_COLUMNS` (100,000 columns evaluated one by
    one), `MAX_EDGE_CHECKS` (5,000,000 = evaluated columns × edges) and `MAX_RANGES` (10,000 protected z ranges, which
    bounds the outline length). Larger polygons are intentionally drawn with straight edges through their vertices
    (one block wide if all vertices are collinear) and are not exact.
  - Block coordinates within the Minecraft world limits, `[-30,000,000, 29,999,999]`.
- Scope (all in `RegionRendererCoverageTest`):
  - Cuboids: every pair of corners in `[-2, 2]^2`, at the origin and translated to three world-border corners.
  - Rectilinear polygons: every simple polyomino outline in a 4×4 cell grid (4-connected, hole-free, no diagonal pinch);
    each with corner-only and every-integer-point vertex lists, both orientations, every start vertex
    (at the origin) and each world-border translation.
  - Triangles: every set of 3 points in a 6×6 grid (7,140, including collinear sets), at every translation.
  - Quadrilaterals: every sequence of 4 points in a 4×4 grid (65,536, including self-intersecting, collinear and
    repeated points), at the origin and near a world-border corner.
  - Random polygons: 3,000 polygons with 3–12 vertices in a 24×24 box at random world positions (fixed seed).
  - Collapsed polygons: every horizontal / vertical sequence of 3 or 4 points with offsets in `[0, 3]`, at every translation.
  - Named cases: a backtracking spike, a pentagram (three separate block groups), a self-intersecting ring (one hole),
    and a triangle with a 5,000-block diagonal edge near the world border (3,000+ sampled blocks).
  - Redundant-vertex property: every simple polyomino in a 3×3 grid scaled by 2, 1,000, 50,000 and 2,000,000.
  - Oracle: WorldGuard 7.0.20 `ProtectedRegion#contains` (independent implementation, counts boundary points as inside).
- Out of scope:
  - How BlueMap's web client triangulates and draws the shapes (the claim stops at the `Shape` vertex lists).
  - Marker creation/update in `WorldGuardRenderer` beyond its unit tests, scheduling and concurrency.
  - Coordinates outside the world limits.
  - Rendering cost: bounded by the limits above (about 20–40 ms per region at the limits, measured locally). Results
    are cached per region and recomputed only when its points or Y range change.
- Method/tool: Bounded-exhaustive JUnit Jupiter tests (JUnit version from `gradle/libs.versions.toml`), Java 25.
  Deductive/bounded-model-checking tools were not used: the production code targets Java 25 bytecode and depends on
  Guava, WorldEdit and BlueMap API types, while JBMC still documents a Java 8 build/bytecode baseline and the OpenJML
  distribution ships a Java 21 runtime (checked 2026-11).
- Reproduce: `./gradlew test --tests 'net.okocraft.bluemapmarkers.module.worldguard.RegionRendererCoverageTest'`
- Evidence: `src/test/java/net/okocraft/bluemapmarkers/module/worldguard/RegionRendererCoverageTest.java`;
  JUnit XML under `build/test-results/test/` (CI runs it as part of `./gradlew build`).
- Status: TESTED (every input of the finite domains above was executed; this is not a proof for other inputs)
- Reviewed by: _pending human review of the specification_
- Follow-up / counterexamples found and fixed while establishing this record:
  1. **Precision loss beyond ±2^24.** `new Vector2d(int, int)` resolves to `Vector2d(float, float)`, so e.g. the
     polygon block `(-30000000, -30000000)` outside the square `(-29999999, -30000000)`–`(-29999998, -29999999)` was
     drawn because vertex `-29999999` became `-3.0E7`.
     Fixed by converting block coordinates through `double`.
  2. **`int` overflow in backtracking detection.** For collinear points `(-3000000, -3000000)`, `(-2000000, -3000000)`,
     `(-1000000, -3000000)` the dot product overflowed and the straight-through vertex was treated as a reversal,
     adding a spurious spike vertex. First fixed by computing the dot product in `long`; the code was later replaced
     (item 3).
  3. **Diagonal edges and self-intersecting polygons did not match the protected blocks.** The previous renderer
     shifted each polygon vertex by one block, which is exact only for axis-aligned simple polygons. For
     `(0, 0) → (16, 3) → (20, 14) → (3, 10)`, 2 protected blocks were not drawn and 15 unprotected blocks were; for the
     bow tie `(0, 0) → (12, 12) → (12, 0) → (0, 12)` the areas cancelled out and 84 of 97 protected blocks were not
     drawn. The renderer now rasterizes each column with WorldGuard's rule and traces the outline of the protected
     blocks, producing one area per separate block group and holes for enclosed unprotected blocks.
