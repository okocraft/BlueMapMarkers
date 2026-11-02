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

### FV-002: The BlueMap webapp fills exactly the protected blocks, at the right height

- Owner: BlueMapMarkers maintainers
- Target: `WorldGuardRenderer#createMarker` (2D `ShapeMarker` and 3D `ExtrudeMarker`) together with
  `RegionRenderer.render`, as consumed by the BlueMap webapp v5.29
  (`common/webapp/src/js/markers/ShapeMarker.js`, `ExtrudeMarker.js`; three.js 0.186.1).
- Property:
  - **Fill:** the triangles the webapp creates (`ShapeGeometry`, and the top cap of `ExtrudeGeometry`) cover a block
    column **iff** WorldGuard reports the region contains it, and never cover a block twice.
  - **Border:** the outline the webapp draws (`shape.getPoints(5)`) is the vertex list sent in `markers.json`,
    apart from consecutive duplicates that three.js drops.
  - **Height (3D):** the extruded volume spans from `minY` to `maxY + 1`, i.e. includes the top block layer.
- Preconditions and domain: the same region families as FV-001 (cuboids in `[-2, 2]^2`, every simple polyomino in
  a 4×4 grid with corner-only / every-point vertex lists and both orientations, axis-aligned collapsed polygons,
  the backtracking spike), each at the origin and near both world-border corners: 115,986 cases.
- Scope: markers are created by the production `createMarker` and serialized with BlueMap API 2.8.0 `MarkerGson`.
  The webapp's JavaScript (shape construction relative to `position`, the 0.01 offset of extrude shapes,
  geometry creation and `setShapeY`) is mirrored in `verification/bluemap-webapp/check.mjs` and evaluated with the
  real three.js triangulation. Each block is probed at two generic interior points (block centres lie on
  triangulation diagonals).
- Out of scope: GPU rasterization, line width and anti-aliasing, depth test / z-fighting, label and popup
  placement, other BlueMap versions (the mirrored code must be re-checked when BlueMap changes it), and the region
  families excluded in FV-001.
- Method/tool: opt-in JUnit exporter + Node.js script with three.js 0.186.1 (pinned in `package-lock.json`).
- Reproduce:
  ```bash
  BLUEMAP_EXPORT_PATH=/tmp/bluemap-export.jsonl ./gradlew test --tests '*BlueMapWebappExportTest' --rerun
  cd verification/bluemap-webapp && npm ci && node check.mjs /tmp/bluemap-export.jsonl
  ```
- Evidence: `BlueMapWebappExportTest.java`, `verification/bluemap-webapp/check.mjs`; the script prints per-family
  counts and exits non-zero with the first counterexample per check.
- Status: TESTED (115,986 cases, no counterexample; checked 2026-11-02 against BlueMap v5.29)
- Reviewed by: _pending human review of the specification_
- Follow-up / counterexamples found and fixed:
  1. **3D markers were one block too low.** `ExtrudeMarker` extrudes between the two Y values it is given, but
     `createMarker` passed the block coordinate `maxY`, so the top block layer (`maxY`..`maxY + 1`) was missing in
     every case and a single-layer region became a flat plane. Fixed by passing `maxY + 1`.
