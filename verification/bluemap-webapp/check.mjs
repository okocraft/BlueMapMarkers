// FV-002: reproduce how the BlueMap webapp turns markers.json data into geometry and compare it with WorldGuard.
//
// Mirrors BlueMap v5.29 common/webapp/src/js/markers/ShapeMarker.js and ExtrudeMarker.js
// (createShapeFromData, ShapeGeometry(shape, 5), ExtrudeGeometry({depth: 1, steps: 5, bevelEnabled: false}),
// setShapeY, and the border built from shape.getPoints(5)), using the same three.js version (0.186.1).
//
// Usage: node check.mjs <export.jsonl>   (the file written by BlueMapWebappExportTest)
import { ExtrudeGeometry, Shape, ShapeGeometry, Vector2 } from "three";
import fs from "node:fs";
import readline from "node:readline";

// ShapeMarker#createShapeFromData (offset 0) / ExtrudeMarker#createShapeFromData (offset 0.01)
function shapeFromData(marker, offset) {
  const position = marker.position;
  return new Shape(marker.shape.map(p => new Vector2((p.x || 0) - position.x + offset, (p.z || 0) - position.z + offset)));
}

// Triangles of a geometry in its local (x, y) plane, before rotateX(PI / 2) maps y to world z.
function triangles(geometry, topCapOnly) {
  const pos = geometry.attributes.position.array;
  const idx = geometry.index ? geometry.index.array : null;
  const count = idx ? idx.length : pos.length / 3;
  const result = [];
  for (let i = 0; i < count; i += 3) {
    const v = [0, 1, 2].map(k => {
      const j = idx ? idx[i + k] : i + k;
      return [pos[3 * j], pos[3 * j + 1], pos[3 * j + 2]];
    });
    // ExtrudeGeometry: the cap at local z = 0 becomes the face at maxY after rotateX and setShapeY.
    if (topCapOnly && !v.every(p => p[2] === 0)) continue;
    result.push(v);
  }
  return result;
}

function coverCount(tris, px, py) {
  let count = 0;
  for (const [a, b, c] of tris) {
    const area = (b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0]);
    if (area === 0) continue;
    const s = Math.sign(area);
    const w0 = s * ((b[0] - a[0]) * (py - a[1]) - (b[1] - a[1]) * (px - a[0]));
    const w1 = s * ((c[0] - b[0]) * (py - b[1]) - (c[1] - b[1]) * (px - b[0]));
    const w2 = s * ((a[0] - c[0]) * (py - c[1]) - (a[1] - c[1]) * (px - c[0]));
    if (w0 > 0 && w1 > 0 && w2 > 0) count++;
  }
  return count;
}

function withoutConsecutiveDuplicates(points) {
  return points.filter((p, i) => {
    const q = points[(i + 1) % points.length];
    return !(p[0] === q[0] && p[1] === q[1]);
  });
}

// Block centres lie on triangulation diagonals, so probe two generic interior points per block; both must agree.
const PROBES = [[0.5123, 0.5371], [0.4703, 0.4887]];

const stats = {};
const counterexamples = {};
const fail = (kind, check, input) => {
  stats[kind][check]++;
  counterexamples[`${kind}: ${check}`] ??= input;
};

const lines = readline.createInterface({ input: fs.createReadStream(process.argv[2]) });
for await (const line of lines) {
  const r = JSON.parse(line);
  const st = stats[r.kind] ??= { cases: 0, fill2D: 0, fill3D: 0, overlap: 0, border: 0, height3D: 0 };
  st.cases++;

  // 2D ShapeMarker
  const shape2D = shapeFromData(r.shape, 0);
  const tris2D = triangles(new ShapeGeometry(shape2D, 5), false);
  const border = shape2D.getPoints(5).map(p => [p.x, p.y]);
  const sent = withoutConsecutiveDuplicates(r.shape.shape.map(p => [p.x - r.shape.position.x, p.z - r.shape.position.z]));
  if (border.length !== sent.length || border.some((p, i) => p[0] !== sent[i][0] || p[1] !== sent[i][1])) {
    fail(r.kind, "border", r.input);
  }

  // 3D ExtrudeMarker: setShapeY(minY, maxY) places the cap at maxY and scales depth 1 to (maxY - minY).
  const shape3D = shapeFromData(r.extrude, 0.01);
  const tris3D = triangles(new ExtrudeGeometry(shape3D, { depth: 1, steps: 5, bevelEnabled: false }), true);
  const top = r.extrude.shapeMaxY;
  const bottom = top - (r.extrude.shapeMaxY - r.extrude.shapeMinY);
  if (bottom !== r.minY || top !== r.maxY + 1) fail(r.kind, "height3D", r.input);

  let fill2D = false, fill3D = false, overlap = false;
  for (let z = 0; z < r.h; z++) {
    for (let x = 0; x < r.w; x++) {
      const expected = r.expected[z * r.w + x] === "1";
      const c2 = PROBES.map(([dx, dz]) => coverCount(tris2D, r.x0 + x + dx - r.shape.position.x, r.z0 + z + dz - r.shape.position.z));
      const c3 = PROBES.map(([dx, dz]) => coverCount(tris3D, r.x0 + x + dx - r.extrude.position.x + 0.01, r.z0 + z + dz - r.extrude.position.z + 0.01));
      if (c2.some(c => (c > 0) !== expected)) fill2D = true;
      if (c3.some(c => (c > 0) !== expected)) fill3D = true;
      if ([...c2, ...c3].some(c => c > 1)) overlap = true;
    }
  }
  if (fill2D) fail(r.kind, "fill2D", r.input);
  if (fill3D) fail(r.kind, "fill3D", r.input);
  if (overlap) fail(r.kind, "overlap", r.input);
}

console.log(JSON.stringify(stats, null, 2));
const failures = Object.keys(counterexamples).length;
if (failures > 0) {
  console.log("FAIL: first counterexample per check");
  console.log(JSON.stringify(counterexamples, null, 2));
  process.exitCode = 1;
} else {
  console.log("PASS: no counterexample in", Object.values(stats).reduce((a, s) => a + s.cases, 0), "cases");
}
