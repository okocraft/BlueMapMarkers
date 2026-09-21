# WorldGuard marker load testing

## Scope and risk model

BlueMapMarkers performs scheduled in-process work rather than serving requests. The primary load path is a full WorldGuard region snapshot followed by bounded marker rendering. The relevant risks are:

- CPU time and allocation while starting and traversing a large snapshot;
- scheduler fairness when the number of regions exceeds `update-limit`;
- convergence after additions, removals, or movement between separated marker sets;
- cost growth from polygon vertex count and detail formatting;
- retained heap across repeated refresh cycles.

The automated component test isolates snapshot traversal, which can be measured reproducibly on a shared CI runner. A live Paper soak test remains appropriate when choosing production values for `update-limit` and `update-interval`, because TPS, BlueMap map count, polygon complexity, and other plugins are server-specific.

## Automated workload

Run:

```shell
./gradlew test loadTest
```

`QueueingMarkerUpdaterLoadTest` traverses a 500,000-entry region snapshot. It compares the current value iterator with the previous key queue plus per-entry map lookup in the same JVM. Three warm-up passes precede nine measured passes.

Observed metrics:

- median elapsed time for one complete snapshot traversal;
- median bytes allocated by the test thread;
- render calls per scheduler pass in the functional tests;
- removal records emitted at the end of a refresh cycle.

## Acceptance criteria

- All functional tests pass, including an exact `update-limit` boundary.
- Removed regions and regions moved between marker sets are removed after the refresh cycle.
- Snapshot traversal allocates at least 90% fewer bytes than the previous implementation.
- Median traversal time is no more than 10% slower than the previous implementation.

Elapsed time is a regression guard, not a universal production latency target. Production acceptance should additionally require no sustained TPS below 20, no update-cycle-overrun warning, and stable old-generation heap during a soak test using the target server's actual maps and regions.
