# Eclipse V15 — Client Layer Migration

This iteration continues the migration away from addon-shaped UX and moves more safe functionality into `eclipse.client.*`.

## Added in V15
- `ClientModuleCatalog` and `ClientSection` to decouple the GUI from `EclipseClientBridge`
- `SpatialRuntime` with persisted waypoints and lightweight routes
- `nearest-waypoint` HUD widget
- drag + snap HUD editor behaviour in `EclipseClientScreen`
- inspector breakdown driven by `PerfSnapshot`
- dark theme utility methods (`drawCanvasGrid`, `drawGraph`, warning color)

## User-visible results
- standalone-style **Spatial** tab in the Eclipse client shell
- waypoint add/remove and route create/append/remove inside the client GUI
- nearest waypoint visible in the HUD runtime
- HUD editor widgets can be dragged and snapped to grid
- inspector shows frame, HUD, memory, widget, waypoint, and route counts

## Migration note
The addon still hosts module/runtime integration, but the UX path is now more strongly centered around:
- `eclipse.client.runtime.*`
- `eclipse.client.spatial.*`
- `eclipse.client.hud.*`
- `eclipse.client.ui.*`

Legacy addon screens are no longer the target for new UX work.
