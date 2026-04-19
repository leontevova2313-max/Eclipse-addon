# Settings Guide

This guide explains the important settings and why the defaults are conservative.

It does not list every tiny setting. It focuses on settings users are most likely
to change.

## General Rule

Start conservative. Increase values only after confirming the client works on
your server.

For movement, printer, and packet modules, safer values usually mean:

- fewer actions per tick;
- lower speed;
- longer retry delay;
- smaller range;
- fewer packet pulses.

## eclipse-visuals

### screen-backgrounds

Type: boolean.

Enables Eclipse backgrounds on supported screens.

Default reason: enabled because it is a core visual feature and does not affect
server behavior.

### performance-mode / adaptive-performance

Type: boolean.

Reduces or limits heavier menu visual effects.

Default reason: adaptive mode keeps the UI safer on weaker machines and heavier
screens.

Safe values: keep adaptive performance enabled. Use performance mode if
animations feel heavy.

### title-logo, logo-auto-scale, logo-safe-margin

Types: boolean / integer.

Control the custom Eclipse title logo and its spacing from buttons.

Default reason: auto-scale and safe margin prevent overlap across GUI scales and
window sizes.

When to change: increase safe margin if buttons feel too close. Avoid extreme
logo scale values.

### custom-background

Type: boolean plus file path.

Uses a local image for title background.

Default reason: disabled by default because bad paths or huge images are
user-specific.

### crosshair settings

Important settings:

- `crosshair`
- `style`
- `gap`
- `length`
- `thickness`
- `dynamic-gap`
- `movement-expansion`
- `recoil-simulation`
- `opacity`
- `rainbow`

Default reason: the custom crosshair is optional because vanilla crosshair is
predictable and server-independent.

Safe values: use low thickness, moderate gap, and avoid rainbow if you want
maximum performance.

## Notifier Settings

Settings live under `eclipse-visuals`.

### use-custom-notifier

Type: boolean.

Replaces Meteor module toggle chat feedback with Eclipse overlay notifications.

Default reason: enabled because it reduces chat clutter and uses the addon visual
style.

When to change: disable if you prefer Meteor's normal chat feedback.

### position

Type: enum.

Controls where notifications appear.

Safe value: top-right is a good default because it avoids most vanilla HUD
elements.

### max-notifications

Type: integer.

Limits visible notification count.

Default reason: small bounded queue prevents screen clutter and memory growth.

Safe values: `3` to `5`.

### duration

Type: milliseconds.

Controls how long notifications remain visible.

Safe values: `2500` to `4000`.

## chat-fix

### prefix

Type: boolean plus string.

Adds a prefix to normal outgoing chat messages.

Default reason: useful for specific workflows, but server-dependent.

When to change: disable if a server rejects modified chat or if you do not need a
prefix.

### clickable-links

Type: boolean.

Makes plain links clickable in unsigned/system messages.

Default reason: useful and safe because signed chat is left untouched.

### click-names / reply-command

Types: boolean / string.

Makes detected player names suggest a private message command.

Default reason: quick reply is useful, and `SuggestCommand` does not send
anything by itself.

Set `reply-command` to match the server, for example:

```text
/msg {name} 
```

## middle-click-info

### range

Type: double.

Maximum target inspection distance.

Default reason: `6.0` is close to normal interaction distance and avoids
inspecting far-away targets by accident.

Safe values: `4.5` to `8.0`.

### cancel-middle-click

Type: boolean.

Cancels vanilla middle-click pick-block after Eclipse handles the target.

Default reason: enabled to avoid duplicate actions.

When to change: disable if you want vanilla pick-block behavior.

### entity-preview

Type: boolean.

Highlights player/mob target under the crosshair.

Default reason: useful visual feedback with low cost.

## litematica-printer

### blocks-per-tick

Type: integer.

Maximum placement interactions per tick.

Recommended: `1`.

Why: slow placement is more stable on servers and easier to debug.

Risky values: high values can cause missed placements, corrections, or anticheat
reactions.

### interaction-range

Type: double.

Maximum distance for placement interaction.

Recommended: around `4.5` to `5.0`.

Why: stays near real reach and avoids impossible targets.

### build-order

Type: enum.

Controls target priority.

Recommended: `StableSupport`.

Why: it prefers targets that are more likely to have support and succeed.

### retry-delay

Type: ticks.

Wait time before checking/retrying a placement.

Recommended: around `12`.

Why: gives the server time to confirm world state.

### max-retries

Type: integer.

Failed verification attempts before temporary skip.

Recommended: `3`.

Why: prevents infinite loops on impossible positions.

## eclipse-elytra

Important settings:

- horizontal speed;
- vertical speed;
- firework assist;
- correction recovery;
- control response.

Default reason: values should be conservative because elytra flight is
server-sensitive.

Safe approach: start with low speed and verify sustained control before
increasing values.

## eclipse-velocity

### horizontal / vertical

Purpose: scale knockback components.

Recommended: use `50/50` for conservative testing. Use `0/0` only when testing
full cancel.

Risk: servers may correct position after reduced knockback.

### explosion settings

Purpose: separately scale explosion knockback.

Recommended: keep similar to normal velocity until tested.

## Movement / Packet Modules

Modules:

- `eclipse-move`
- `eclipse-flight`
- `eclipse-no-slow`
- `pearl-phase`
- `ping-spoof`

Recommended policy:

- Change one setting at a time.
- Test in a safe environment.
- Keep speeds/delays conservative.
- Disable modules if the server starts correcting position repeatedly.

## Skin and Cape Settings

Skin/cape state is stored in:

```text
eclipse-skins/customization.txt
```

Local skin PNG sizes accepted for preview:

- `64x64`
- `64x32`
- `128x128`

Official account skin/cape actions:

- use the active Minecraft session token;
- do not spoof another account;
- may take time to propagate to other players.

