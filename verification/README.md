# Verification Records

Scoped verification records following the
[OKOCRAFT Formal Verification Guidelines](https://github.com/okocraft/docs/blob/main/dev/guidelines/formal-verification.md).
A record only covers the property, domain and assumptions written in it.

### FV-001: A WorldGuard region is drawn on BlueMap exactly over the blocks it protects

- Owner: BlueMapMarkers maintainers
- Target: `src/main/java/net/okocraft/bluemapmarkers/module/worldguard/RegionRenderer.java`, `RegionRenderer.render(ProtectedRegion)`
- Property:
  - **Coverage:** for a rendered region, a block column `(x, z)` is drawn (the winding number of the returned
    `Shape` around the block centre `(x + 0.5, z + 0.5)` is non-zero) **iff**
    `region.contains(BlockVector3.at(x, minY, z))` is `true` in WorldGuard.
  - **No overlap:** no block centre has `|winding number| > 1`, so the outline never covers a block twice.
  - **Vertical range:** `Result.minY()` / `Result.maxY()` equal the region's minimum / maximum Y.
  - **Redundant vertices:** inserting a vertex in the middle of a polygon edge changes the rendered outline only by
    zero-length edges and straight-through vertices (no extra spikes), including for edges whose vectors exceed
    `int` range when squared.
- Preconditions:
  - `ProtectedCuboidRegion`, or `ProtectedPolygonalRegion` with at least 3 points whose outline is a
    **simple rectilinear (axis-aligned) polygon**, or a zero-area polygon whose points lie on one axis-aligned line
    (including backtracking and repeated points).
  - Block coordinates within the Minecraft world limits, `[-30,000,000, 29,999,999]`.
- Scope (enumerated domains, all in `RegionRendererCoverageTest`):
  - Cuboids: every pair of corners in `[-2, 2]^2`, at the origin and translated to three world-border corners.
  - Rectilinear polygons: every simple polyomino outline in a 4×4 cell grid (4-connected, hole-free, no diagonal pinch);
    each with corner-only and every-integer-point vertex lists, both orientations, every start vertex
    (at the origin) and each world-border translation.
  - Collapsed polygons: every horizontal / vertical sequence of 3 or 4 points with offsets in `[0, 3]`, at every translation.
  - Redundant-vertex property: every simple polyomino in a 3×3 grid scaled by 2, 1,000, 50,000 and 2,000,000.
  - Oracle: WorldGuard 7.0.20 `ProtectedRegion#contains` (independent implementation, counts boundary points as inside).
- Out of scope:
  - Polygons with diagonal edges (the renderer approximates them; no exactness claim) and self-intersecting polygons.
  - Collapsed polygons along a diagonal line.
  - How BlueMap's web client triangulates and draws the `Shape` (the claim stops at the `Shape` vertex list).
  - Marker creation/update in `WorldGuardRenderer`, scheduling and concurrency.
  - Coordinates outside the world limits (`max + 1` is computed in `int`).
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
     adding a spurious spike vertex. Fixed by computing the dot product in `long`.
