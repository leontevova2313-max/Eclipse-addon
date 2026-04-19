# Modules

This page describes the modules that matter for the first clean release.

## Release Classification

Stable / release-ready:

- `eclipse-visuals`
- `chat-fix`
- `middle-click-info`
- `eclipse-camera`
- `eclipse-name-guard`

Advanced / server-sensitive:

- `litematica-printer`
- `eclipse-elytra`
- `eclipse-velocity`
- `eclipse-move`
- `eclipse-flight`
- `eclipse-no-slow`
- `pearl-phase`
- `ping-spoof`
- `eclipse-server-intel`
- `external-cheat-trace`

Internal / not registered in this release:

- `server-diagnostics`
- `server-auto-setup`
- `eclipse-anti-crash`
- `eclipse-custom-packets`

## Stable Modules

### eclipse-visuals

Purpose: controls Eclipse menu visuals, title background, logo layout, crosshair,
skin preview, and notification overlay.

Why included: it is the main identity layer of the addon and does not depend on
server behavior.

Recommended settings:

- Keep adaptive performance enabled.
- Keep logo auto-scale enabled.
- Use custom notifier with a small max notification count.

Risks:

- Custom backgrounds and animations can be visually heavy on weak systems.
- Custom crosshair is client-side only.

### chat-fix

Purpose: improves chat usability without breaking Minecraft signed chat.

What it does:

- Adds optional prefix to normal outgoing messages.
- Makes plain links clickable in unsigned/system messages.
- Makes detected player names clickable for quick reply suggestions.
- Leaves signed chat messages untouched.

Why included: it fixes day-to-day chat usability and avoids red chat desync
warnings by not rewriting signed messages.

Recommended settings:

- Keep clickable links enabled.
- Keep clickable names enabled.
- Set `reply-command` to match the server, for example `/msg {name} `.

Risks:

- Prefixing outgoing chat sends a modified player message. Disable prefix if a
  server does not allow it.
- Name detection is pattern-based and may not catch every custom chat format.

### middle-click-info

Purpose: adds practical target inspection and friend add workflow.

What it does:

- Middle-click a player to add them to Meteor friends.
- Middle-click blocks/entities to show information.
- Highlights player/mob targets in 3D.
- Uses a configurable distance range.

Recommended settings:

- Range: `6.0`.
- Entity preview: enabled.
- Cancel vanilla middle-click: enabled if you do not need pick-block behavior.

### eclipse-camera

Purpose: fine-tunes camera/FOV behavior.

Why included: camera tweaks are visual/client-side and useful for normal play.

Recommended settings: keep values conservative.

### eclipse-name-guard

Purpose: reports duplicate Meteor module names.

Why included: it helps catch addon conflicts before they become confusing GUI or
command issues.

## Advanced / server-sensitive Modules

### litematica-printer

Purpose: places blocks from the loaded Litematica schematic with a controlled
pipeline.

What it does:

- Reads selected Litematica placement through a reflection bridge.
- Compares schematic state to world state.
- Filters impossible targets.
- Checks item availability.
- Attempts placement.
- Verifies result.
- Retries or temporarily skips failed positions.

Why included: it is one of the main practical features of the addon, but it still
depends on real server timing and block rules.

Recommended settings:

- `blocks-per-tick`: `1`
- `tick-delay`: `2`
- `build-order`: `StableSupport`
- `retry-delay`: around `12`
- `max-retries`: `3`
- `pause-when-missing-blocks`: enabled

Risks:

- Complex blocks with NBT are not fully recreated.
- Directional blocks can still need manual correction.
- Server anticheat, ping, and TPS can affect placement.
- Requires Litematica and a loaded placement.

### eclipse-elytra

Purpose: provides elytra flight profiles and sustained flight control.

Recommended settings:

- Use conservative speeds.
- Keep server-safe behavior enabled where available.
- Test without aggressive boost values first.

Risks:

- Server corrections can interrupt flight.
- Strict servers may reject or correct movement.

### eclipse-velocity

Purpose: controls knockback response.

What it does:

- Handles player velocity packets.
- Supports cancel and scale behavior.
- Separates horizontal and vertical multipliers.
- Handles explosion knockback.

Recommended settings:

- Conservative testing: horizontal `50`, vertical `50`.
- Full cancel testing: horizontal `0`, vertical `0`.

Risks:

- Servers can send position corrections after reduced knockback.
- Aggressive settings may be detected or feel desynced.

### Other advanced modules

- `eclipse-move`: conservative movement tuning.
- `eclipse-flight`: packet fly / flight / glide profiles. High risk on strict servers.
- `eclipse-no-slow`: no-slow style behavior using multipliers and packet pulses.
- `pearl-phase`: server-specific pearl phase sequence.
- `ping-spoof`: controlled latency packet queue.
- `eclipse-server-intel`: information gathering from server-sent events.
- `external-cheat-trace`: local diagnostic tracing for other loaded client modules.

Treat these as server-sensitive tools. Change one setting at a time and test carefully.

## Internal / Not Registered

These modules are not added in `Eclipse.java` for the first clean release:

- `server-diagnostics`
- `server-auto-setup`
- `eclipse-anti-crash`
- `eclipse-custom-packets`

Reason: they are internal, server-profile-specific, or better suited for a
diagnostic build than a clean public runtime release.

