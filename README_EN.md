# Eclipse Addon

<p align="center">
  <img src="docs/eclipse_logo.png" alt="Eclipse" width="720">
</p>

Eclipse is a client-side Meteor Client addon for Minecraft 1.21.11.
It focuses on server diagnostics, movement tuning, Eclipse-themed UI visuals,
Litematica schematic printing, and small quality-of-life tools.

For the current consolidated project state, use:

- [Project Overview](docs/PROJECT_OVERVIEW.md)

This is a Fabric client mod and must be loaded together with Meteor Client.
Use it only on servers where this kind of client-side tooling is allowed.

## Project status

- This project is a first clean public release.
- The project was made 100% by AI according to user requirements and edits.
- Development and tuning target only the `play.karasique.com` server.
  Behavior on other servers is not guaranteed.
- `litematica-printer` is a reworked addon originally made by the player
  `twilight`, adapted for this project and its server-specific behavior.

## Features

### Visuals and UI

- `eclipse-visuals` customizes the title screen, menu backgrounds, branding,
  crosshair behavior, and local skin/cape preview UI.
- `eclipse-camera` adjusts first-person FOV, view height, and camera-based
  block targeting.
- `middle-click-info` adds a top overlay notification for middle-click target
  inspection. Middle-clicking a player adds them to Meteor friends; clicking a
  block or non-player entity shows its name, registry id, and block position
  where applicable.

### Server diagnostics

- `server-diagnostics` records corrections, velocity changes, movement data,
  packets, active modules, and generated analysis.
- `eclipse-server-intel` combines NewChunks, SoundLocator, coordinate logging,
  and ore update logging in one lightweight module.
- `eclipse-custom-packets` sends controlled packet pulses for diagnostics and
  server behavior testing.
- `external-cheat-trace` writes a packet-level trace of other client
  modules/cheats loaded in the same Fabric instance. It is meant for analyzing
  closed-source behavior without source code.
- `eclipse-name-guard` reports duplicate Meteor module names before they cause
  hard-to-debug conflicts.
- `eclipse-anti-crash` cancels selected suspicious packets that can destabilize
  the client.

### Movement and combat-adjacent utilities

- `eclipse-move` provides conservative configurable movement tuning.
- `eclipse-flight` includes PacketFly, flight, glide, boost, and jetpack-style
  movement profiles.
- `eclipse-elytra` adds elytra fly, ground glide, and chestplate fake-fly
  profiles.
- `eclipse-no-slow` uses movement multipliers and slot/offhand packet pulses.
- `eclipse-velocity` scales knockback or applies velocity cancellation modes.
- `pearl-phase` throws a pearl near a wall and sends a configurable phase
  packet sequence.
- `ping-spoof` queues selected latency packets and releases them after a
  configurable delay.

### Litematica printing

- `litematica-printer` reads the currently loaded Litematica schematic through
  a reflection bridge and places matching blocks with configurable pacing.
- The printer can render the current placement queue in-world.
- The vanilla experience bar can be replaced with schematic build progress
  while printing.
- Placement safety options include entity checks, TPS pause, correction pause,
  retry delays, falling-block protection, inventory movement, and swap-back.

Litematica is optional for launching the addon, but it is required for the
printer module to find and use a loaded schematic.

## Requirements

- Minecraft `1.21.11`
- Java `21`
- Fabric Loader `0.18.2` or newer compatible version
- Meteor Client `1.21.11-SNAPSHOT`
- Litematica for `1.21.11` if you want to use `litematica-printer`

Exact dependency versions are defined in
[`gradle/libs.versions.toml`](gradle/libs.versions.toml).

## Installation

1. Install Fabric Loader for Minecraft `1.21.11`.
2. Install Meteor Client for the same Minecraft version.
3. Build or download the Eclipse addon jar.
4. Put the Eclipse jar into your Minecraft `mods` folder together with Meteor.
5. Start the game and open Meteor's module list.
6. Find the modules under the `Eclipse` category.

## Building

On Windows:

```powershell
.\gradlew.bat build
```

On Linux or macOS:

```bash
./gradlew build
```

The compiled jar is created in:

```text
build/libs/
```

## Development

Useful project paths:

- `src/main/java/eclipse/Eclipse.java` registers the addon category and modules.
- `src/main/java/eclipse/modules/` contains the Eclipse modules.
- `src/main/java/eclipse/gui/` contains custom GUI and overlay code.
- `src/main/java/com/eclipse/mixin/` contains client mixins.
- `src/main/resources/eclipse.mixins.json` registers mixins.
- `src/main/resources/fabric.mod.json` contains Fabric metadata.
- `src/main/resources/assets/eclipse/` contains textures and language files.

After changing code or resources, run:

```powershell
.\gradlew.bat build
```

## GitHub Actions

The repository includes two workflows:

- `dev_build.yml` builds the project on every push and publishes a snapshot
  release artifact.
- `pull_request.yml` builds pull requests and uploads compiled artifacts.

## Notes

- Server behavior differs heavily between configurations. Treat movement and
  packet modules as diagnostic tools that need per-server tuning.
- The addon does not include Meteor Client, Fabric Loader, Litematica, or
  Minecraft itself.
- Generated Gradle output, Minecraft runtime files, and IDE files are ignored
  through `.gitignore`.

## License

This project uses the `CC0-1.0` license included in [`LICENSE`](LICENSE).

