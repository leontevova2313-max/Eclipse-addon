# Eclipse client migration v14

This pass moves more real functionality away from the addon shell and into Eclipse-owned runtime classes.

## Added runtime pieces
- theme manager
- sampled perf inspector service
- persisted client state
- HUD runtime and layout store
- client HUD renderer

## Added safe utility overlays
- light meter
- screenshot grid
- crosshair info

## Integration points
- title screen Eclipse button
- pause menu Eclipse button
- Right Shift workspace toggle
- in-game HUD overlay rendering through our own renderer

## What is still legacy-hosted
- Meteor module registry
- addon entrypoint
- existing addon categories

## Next phase
- move more settings and visual tools into client-owned runtime
- replace legacy addon-facing assumptions inside the GUI bridge
- add route/waypoint workspace into our client layer
