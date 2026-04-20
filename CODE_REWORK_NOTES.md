# Eclipse Modern rework notes

## What was fixed
- Wired the `Eclipse Modern` GUI theme into addon initialization so it is actually registered and selected.
- Fixed the incorrect `SettingColor` import in `EclipseModernTheme`.
- Tightened the theme palette toward a darker, cleaner semi-transparent card style.
- Fixed inconsistent `performanceMode` / `adaptivePerformance` logic in `EclipseConfig`.
- Changed adaptive performance defaults to `false` to avoid silently downgrading visuals.
- Prevented duplicate toast overlay rendering by only drawing screen toasts outside in-world GUI screens.
- Replaced the hardcoded `5.0` crosshair ray distance with the current interaction reach.
- Removed the hardcoded `karasique.com` special case from diagnostics server canonicalization.
- Made `DiagnosticStore` packet/event snapshots safer to export without exposing mutable internals.
- Added a missing `transition_glow.png` title asset referenced by `TitleScreenMixin`.
- Reduced repeated allocations in `LitematicaPrinter` for candidate/render helper lists and reused the targeting line color.
- Cached title-screen skin lookup through a reusable supplier and a timed local cache instead of resolving every frame.

## Not fully refactored
- `LitematicaPrinter` is still a very large module. I only applied safe cleanup and small performance fixes here.
- I did not do a risky deep split of the printer / title screen into many classes without a buildable dependency graph.

## Build note
- Full Gradle verification could not be run in this environment because the wrapper needs network access to download Gradle and Meteor dependencies.
