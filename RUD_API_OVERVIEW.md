# Eclipse / Rud API overview

This pass adds a small internal API layer so the addon does not keep wiring bootstrap and UI logic directly in feature classes.

## Structure

### `eclipse.api.bootstrap`
- `EclipseBootstrap` owns category creation, module registration order, and managed window ids.
- `RudModulePlan` is a thin supplier wrapper used to keep startup registration ordered and centralized.

### `eclipse.api.chat`
- `RudChatBridge` owns incoming chat decoration.
- `ChatLinks` now delegates nickname parsing and URL click decoration to this bridge instead of embedding brittle regex and mutation code directly.

### `eclipse.api.layout`
- `RudWindowLayout` owns the first-run category window arrangement for Eclipse categories inside Meteor's module screen.
- The layout only applies when a managed window still has default `x = -1` and `y = -1`, so manual user positioning is preserved after the first placement.

### `eclipse.api.EclipseApi`
- Simple shared access point for the Rud chat/layout services.

## Why this helps
- Fewer hardcoded names scattered across mixins and modules.
- Safer interconnections between bootstrap, UI, and chat behavior.
- Less addon-specific logic embedded directly in module classes.
- Easier future cleanup if you later split visuals/chat/layout into separate packages.
