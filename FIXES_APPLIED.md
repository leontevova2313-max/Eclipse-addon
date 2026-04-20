# Applied safe fixes

## Changed
- Kept the stricter `fabric.mod.json` dependency ranges from the previous pass.
- Kept the less fragile `eclipse.mixins.json` defaults (`required = false`, `defaultRequire = 0`).
- Removed broken **ColorChat** wiring from addon bootstrap and deleted its preview hook.
- Added a small **Eclipse / Rud API layer** to centralize bootstrap, chat decoration, and first-run window layout.
- Rebuilt chat click handling around `RudChatBridge` so nickname click-to-reply parsing is not tied to the old brittle regex.
- Added a first-run **vertical Meteor window layout** for Eclipse categories via `ModulesScreenCategoryControllerMixin` + `RudWindowLayout`.
- Cached category/window ownership in bootstrap instead of scattering names and order across the addon.
- Kept the safer title-screen layout and diagnostics changes from the previous archive.

## Files added
- `src/main/java/eclipse/api/EclipseApi.java`
- `src/main/java/eclipse/api/bootstrap/EclipseBootstrap.java`
- `src/main/java/eclipse/api/bootstrap/RudModulePlan.java`
- `src/main/java/eclipse/api/chat/RudChatBridge.java`
- `src/main/java/eclipse/api/layout/RudWindowLayout.java`
- `src/main/java/com/eclipse/mixin/ModulesScreenCategoryControllerMixin.java`
- `RUD_API_OVERVIEW.md`

## Files changed
- `src/main/java/eclipse/Eclipse.java`
- `src/main/java/eclipse/modules/chat/ChatLinks.java`
- `src/main/resources/eclipse.mixins.json`

## Files removed
- `src/main/java/eclipse/modules/chat/ColorChat.java`
- `src/main/java/eclipse/modules/chat/colorchat/*`
- `src/main/java/com/eclipse/mixin/ChatScreenMixin.java`

## Not changed
- Combat, movement, printer, and other unsafe gameplay logic were not modified.
- I did not add obfuscation or anti-extraction tricks. The new structure is internal API cleanup, not concealment.

## Build note
- I could not rerun Gradle in this container because the wrapper tries to fetch `gradle-9.2.0-bin.zip` from `services.gradle.org`, and outbound network is blocked here.
