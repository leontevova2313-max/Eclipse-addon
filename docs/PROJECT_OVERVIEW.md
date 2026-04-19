# Eclipse-Addon Project Overview

<p align="center">
  <img src="eclipse_logo.png" alt="Eclipse-Addon" width="720">
</p>

This page describes the current public release state of Eclipse-Addon version
`1.0.0`.

## What The Project Is

Eclipse-Addon is a Meteor addon for Minecraft `1.21.11`. It adds an `Eclipse`
category to Meteor with visual customization, menu skin preview tools, utility
modules, movement testing modules, and a Litematica printer.

The project does not replace Meteor Client. It extends it.

## Main Areas

1. Visual and menu layer.
2. Skin and cape preview/apply tools.
3. Chat and middle-click utilities.
4. Movement and packet-related modules.
5. Litematica schematic printing.
6. Server information helpers.

## Runtime Module List

Registered in the public release:

- `chat-fix`
- `eclipse-camera`
- `eclipse-name-guard`
- `eclipse-flight`
- `eclipse-move`
- `eclipse-no-slow`
- `eclipse-visuals`
- `external-cheat-trace`
- `eclipse-elytra`
- `litematica-printer`
- `middle-click-info`
- `pearl-phase`
- `ping-spoof`
- `eclipse-server-intel`
- `eclipse-velocity`

Not registered in the public runtime:

- `eclipse-anti-crash`
- `eclipse-custom-packets`
- `server-auto-setup`
- `server-diagnostics`

These files remain in source form for internal or diagnostic builds.

## Visual Layer

`eclipse-visuals` controls:

- title screen background;
- custom Eclipse logo;
- safe logo/button layout;
- menu backgrounds;
- crosshair rendering;
- notification overlay;
- skin preview menu integration.

The title screen layout reserves real space for the logo and moves button rows
below it. The vanilla yellow splash text is disabled for the custom menu.

## Skin And Cape Tools

The customization screen supports:

- local PNG skin loading;
- official username skin preview;
- official skin upload for the active authenticated Minecraft account;
- official cape list loading for the active authenticated account;
- cape selection and preview on the menu model;
- saving selected skin/cape state.

Important limitation:

Eclipse-Addon does not spoof accounts and does not switch Minecraft sessions
inside a running client. To manage another account's official cape/skin, launch
Minecraft with that account through the launcher.

## Chat And Middle Click

`chat-fix` leaves signed chat untouched and only decorates unsigned/system text
for links and quick reply suggestions.

`middle-click-info` adds target inspection, friend add notifications, distance
limits, and entity preview.

## Litematica Printer

`litematica-printer` reads the loaded Litematica placement, compares schematic
state against world state, filters targets, checks inventory, attempts placement,
verifies results, and retries or temporarily skips failing positions.

Use conservative settings first:

- `blocks-per-tick = 1`
- `tick-delay = 2`
- `build-order = StableSupport`

## Movement And Packet Modules

Movement modules are included for advanced users and server testing:

- `eclipse-elytra`
- `eclipse-flight`
- `eclipse-move`
- `eclipse-no-slow`
- `eclipse-velocity`
- `pearl-phase`
- `ping-spoof`

They are server-sensitive. Start with conservative values and test one module at
a time.

## Server Information Helpers

`eclipse-server-intel` and `external-cheat-trace` provide additional observation
and diagnostic value. They are useful for analysis, but should be configured
carefully to avoid noisy output.

## Important Files

- `src/main/java/eclipse/Eclipse.java` - addon and module registration.
- `src/main/java/eclipse/modules/` - modules.
- `src/main/java/eclipse/gui/` - GUI and overlay code.
- `src/main/java/eclipse/skins/` - skin/cape state and loading logic.
- `src/main/java/com/eclipse/mixin/` - mixin integration.
- `src/main/resources/assets/eclipse/` - textures and language files.
- `src/main/resources/eclipse.mixins.json` - mixin registration.

## Release Goal

Version `1.0.0` is the first professional public release point for the project:

- the repository has structured documentation;
- stable and advanced modules are clearly separated;
- internal modules are not registered in the clean runtime;
- install, settings, troubleshooting, and FAQ pages are present;
- the jar builds cleanly with Gradle.
