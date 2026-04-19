# Eclipse-Addon 1.0.0 Release Notes

This is the first clean public release of Eclipse-Addon. The repository now has
clear documentation, a release module list, installation instructions, settings
guidance, troubleshooting, and FAQ pages.

## What Is Ready

- Visual customization core:
  - title screen background;
  - Eclipse logo layout;
  - menu skin preview;
  - custom crosshair;
  - custom notification overlay.
- Chat helper:
  - clickable links in unsigned/system messages;
  - quick private reply suggestions;
  - signed chat is left untouched to avoid desync warnings.
- Middle-click helper:
  - target inspection;
  - player friend add;
  - entity preview;
  - configurable range.
- Skin preview and account appearance tools:
  - local PNG preview;
  - official username skin preview;
  - official cape list for the active session;
  - official cape preview and apply flow.

## Advanced Features

- `litematica-printer`
- `eclipse-elytra`
- `eclipse-velocity`
- `eclipse-flight`
- `eclipse-move`
- `eclipse-no-slow`
- `pearl-phase`
- `ping-spoof`
- `eclipse-server-intel`
- `external-cheat-trace`

These modules are included because they are useful and compile cleanly, but they
are sensitive to server behavior and should be tested with conservative settings.

## Not Included In Runtime Registration

The following modules exist in source form but are not registered in the clean
release runtime:

- `server-diagnostics`
- `server-auto-setup`
- `eclipse-anti-crash`
- `eclipse-custom-packets`

They were excluded from the release module list because they are internal,
server-profile-specific, or better suited for diagnostic builds.

## Recommended First Launch

1. Start Minecraft with Fabric, Meteor, and Eclipse-Addon installed.
2. Open Meteor GUI.
3. Check the `Eclipse` category.
4. Start with stable visual and utility modules.
5. Test advanced movement/printer modules only after confirming the client
   starts cleanly.

## Things To Watch

- If the game crashes on startup, first verify Minecraft/Meteor/Fabric versions.
- If Litematica printer cannot find a schematic, confirm Litematica is installed
  and a placement is loaded.
- If official skin/cape actions fail, check that the active session is a real
  Microsoft/Minecraft account and that the token is valid.
- If a server corrects movement repeatedly, reduce movement module speeds or
  disable the module for that server.

## Build Verification

```powershell
.\gradlew.bat build
```
