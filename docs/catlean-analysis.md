# CatLean 1.21.11 Static Analysis

Source jar:

```text
C:\Users\kastomer\Downloads\catlean_1.21.11.jar
```

This report is based on static inspection only. The jar was not decompiled into
source and no CatLean code was copied into Eclipse.

## Metadata

- Mod id: `catlean`
- Name: `CatLean`
- Version: `0.1.2`
- Authors: `pan4ur`, `06ED`
- License: `All-Rights-Reserved`
- Environment: `client`
- Minecraft: `>=1.21.11`
- Fabric Loader: `>=0.18.2`
- Requires:
  - `fabric`
  - `fabric-language-kotlin >=1.13.6+kotlin.2.2.20`
- Entrypoints:
  - client: `su.catlean.CatLean`
  - preLaunch: `su.catlean.PreLaunch`
- Mixins config: `catlean.mixins.json`
- Access widener: `catlean.accesswidener`

## Packaging Notes

- Around `3092` class files.
- Around `144` non-class resources.
- Main package: `su.catlean`.
- Kotlin runtime and several libraries are bundled inside the jar.
- Most implementation classes are obfuscated into names like `a`, `b`, `_0`,
  `cv`, `yz`, etc.
- String constants are partly hidden through DES/CBC and invokedynamic-style
  indirection, so class names do not directly reveal every module.

## Resources

Visible assets:

- Fonts:
  - `noto.ttf`
  - `sf_regular.ttf`
  - `sf_medium.ttf`
  - `sf_semibold.ttf`
  - `sf_bold.ttf`
- Translations:
  - `assets/catlean/translations/en.ctlang`
  - `assets/catlean/translations/ru.ctlang`
  - `assets/catlean/translations/pl.ctlang`
- Sounds:
  - `armor-alert`
  - `chat-mention`
  - `config-load`
  - `config-save`
  - `crystal-kill`
  - `disable`
  - `enable`
  - `first-launch`
  - `found-stash`
  - `kill1` ... `kill6`
  - `lag`
  - `lag-back`
  - `lego-breaking`
  - `private-message`
  - `range-in`
  - `range-out`
  - `totem-pop`
- Shaders:
  - rect blur/textured rect
  - bloom
  - camouflage
  - kawase blur
  - lines
  - mirror
  - select
- Vector/UI assets:
  - friend, armor, widgets, panels, logo, warning/success/error, wifi, etc.

## Modules Found From Translation Keys

These are the modules/features that can be identified with high confidence from
translation keys. Their exact Java/Kotlin implementation classes are obfuscated.

### Attack

- `auto-netherite-scrap`
  - Messages mention trap cooldown and target distance.

### Rage

- `elytra-plus`
  - Checks elytra durability.
  - Tracks remaining seconds/meters.
  - Requires enough height.

### Inventory

- `elytra-swap`
  - Swaps to elytra/chestplate.
  - Reports missing chestplate.
- `elytra-replace`
  - Replaces low/broken elytra.
- `tool-saver`
  - Switches away when a tool is almost broken.

### Player

- `auto-auth`
  - Login/register automation.
  - Handles empty password errors.
- `auto-fish`
  - Swaps to a new rod when the current rod is almost broken.
- `auto-respawn`
  - Reports death position.

### Equip

- `elytra-swap`
  - Separate equip-category elytra/chestplate swap messages.
- `middle-click`
  - Adds/removes players from friends.
- `x-carry`
  - Mentions compatibility with `Gui Move` mode.

### ESP

- `name-tags`
  - Friend add/remove through nametags.
- `logout-spots`
  - Logs player logout/login coordinates.

### Misc

- `notifier`
  - Low armor warning.
  - Range enter/leave.
  - Totem pop/death tracking.
  - Server lag/error warning.
  - LagBack/flag notification.
  - Chat mention notification.
  - Expired potion/effect notification.
  - Item pickup tracking.
  - Splash potion/buff notification.
  - Air alarm notification.
  - Item-use notification.
- `spammer`
  - Reads a spammer file and reports if it is empty.
- `auto-leave`
  - Leaves on nearby player.
  - Leaves on low totems.
  - Leaves on low HP.
- `fast-latency`
  - Detects ping spikes.

### Commands

- `bind`
- `friend`
- `config`

### GUI / Hub

- Theme editor:
  - primary/secondary colors
  - blur/widget blur
  - outline/text colors
  - sliders
  - active/inactive module colors
- Config window:
  - load/delete/save
  - open in file manager
- Discord RPC confirmation.
- Version update confirmation/download screen.

## Event Surface

CatLean exposes its own event API. Relevant event names:

### Client

- `InputEvent`
- `TickEvent`
- `PostTickEvent`
- `ScreenEvent`
- `PostTitleScreenInitEvent`
- `FilesDraggedEvent`

### Network

- `SendPacket`
- `AfterSendPacket`
- `ReceivePacket`
- `AfterReceivePacket`
- `DisconnectEvent`
- `PlayerTickMoveEvent`
- `SlowDownEvent`
- `SendMessageEvent`

### Player

- `MoveEvent`
- `JumpEvent`
- `CollisionEvent`
- `CobWebEvent`
- `ElytraEvent`
- `PreElytraEvent`
- `AfterElytraEvent`
- `FallFlyingEvent`
- `FixVelocityEvent`
- `FlagEvent`
- `FreecamStateEvent`
- `ReachStateEvent`
- `PushOutOfBlocksEvent`
- `PreSyncEvent`
- `SyncEvent`
- `PostSyncEvent`
- `AttackEvent`
- `PostAttackEvent`
- `InteractBlockEvent`
- `PreInteractItemEvent`
- `PostInteractItemEvent`
- `UpdateSelectedSlotEvent`
- `UsingItemEvent`
- `FinishUsingItemEvent`
- `StopUsingItemEvent`
- `TridentUseEvent`
- `TridentWeatherEvent`

### Render

- `Render2DEvent`
- `Render3DEvent`
- `RenderNameTagEvent`
- `RenderEntityEvent`
- `CrosshairRenderEvent`
- `HotBarRenderEventPre`
- `HotBarRenderEventPost`
- `BossBarRenderEvent`
- `RenderScreenEvent`
- `RenderGuiBackgroundEvent`
- `ShaderApplyEvent`
- `XRayBlockEvent`
- `WeatherRenderEvent`
- `CameraOffsetEvent`
- `HurtTiltEvent`
- `NauseaRenderEvent`

### World

- `WorldUpdateEvent`
- `EntitySpawn`
- `EntityRemove`
- `BlockStateEvent`
- `CrystalCreateEvent`
- `FireWorkVectorEvent`
- `FireWorkVelocityEvent`
- `VelocityMultiplierEvent`
- `WaterPushEvent`
- `SlipperinessEvent`

## Mixins Summary

CatLean touches a broad surface:

- Network:
  - connection
  - packet send/receive hooks
  - serverbound interact/move packet accessors
- Player:
  - local player
  - remote player
  - inventory
  - movement/collision
  - input
- World:
  - block collisions
  - fluids
  - slime/sweet berry/web collision
  - fireworks
  - end crystals
- Render:
  - game renderer
  - level renderer
  - entity renderer
  - nametag/entity render hooks
  - GUI/hud/title screen
  - shader/render system
  - boss bar/tab overlay/map rendering

## Practical Extraction Result

What can be reused safely:

- Module/function list above.
- Behavioral targets inferred from translations and events.
- Runtime traces through Eclipse `external-cheat-trace`.

What should not be copied directly:

- CatLean class bytecode/source logic. The jar is `All-Rights-Reserved` and
  implementation is obfuscated.

Best next step:

1. Run CatLean in the Eclipse dev instance.
2. Enable Eclipse module `external-cheat-trace`.
3. Trigger one CatLean feature at a time.
4. Use the generated CSV traces to implement equivalent Eclipse behavior from
   observed packets and game state changes.

## Launch Result

Prepared `run/mods` with:

- `catlean_1.21.11.jar`
- `fabric-api-0.141.3+1.21.11.jar`
- `fabric-language-kotlin-1.13.6+kotlin.2.2.20.jar`

`gradlew runClient --stacktrace` reached Minecraft startup with loaded resource
packs/mods including `catlean`, `eclipse`, `fabric-api`,
`fabric-language-kotlin`, and `meteor-client`.

Observed warnings:

- Fabric dev remapper prints many `unknown invokedynamic bsm` warnings for
  CatLean obfuscation/string indirection.
- CatLean references missing UI resource
  `catlean:textures/ui/windows/config.png`.
- CatLean Discord IPC thread can throw `java.io.IOException: Stream Closed` on
  shutdown.
- Realms authentication warnings are expected in the dev account/session and are
  not CatLean-specific.
