# Eclipse v16 — functions, profiles, visual direction

## AutoFireworks

Module: `Eclipse Movement / auto-fireworks`

Bind the module to any key in Meteor's module keybind UI. The module is intentionally one-shot: on key press it activates, searches for a firework rocket, uses it, then disables itself.

Search/use order:

1. Offhand rocket: use `Hand.OFF_HAND`.
2. Hotbar rocket: swap to the slot, use `Hand.MAIN_HAND`, optionally swap back.
3. Main inventory rocket: move to the configured fallback hotbar slot, use, optionally restore the remaining stack.

Relevant settings:

- `fallback-hotbar-slot`
- `swap-back`
- `restore-inventory-slot`
- `notify`

## InventoryPresets

Module: `Eclipse Utility / inventory-presets`

The module has two actions:

- `Save`: captures current hotbar/main inventory layout.
- `Apply`: moves matching items into saved slots.

Storage file:

```text
config/eclipse-inventory-presets.json
```

Persistence is crash-safe enough for normal use:

- writes through `*.tmp`;
- replaces with atomic move when the filesystem supports it;
- keeps `*.bak` before overwriting.

Current scope:

- hotbar slots 0-8;
- main inventory slots 9-35;
- no armor/offhand yet.

## Profiles

Module: `Eclipse Utility / profiles`

Profiles are folders under:

```text
config/eclipse-profiles/<profile-name>/
```

Copied files:

```text
meteor-client/modules.nbt
meteor-client/config.nbt
meteor-client/hud.nbt
meteor-client/macros.nbt
config/eclipse-client.json
config/eclipse-inventory-presets.json
```

Safety behavior:

- `_autosafe` profile is refreshed on addon init;
- target files are copied through `*.tmp`;
- previous versions become `*.bak`;
- loading a profile should be followed by restart/rejoin because Meteor may already have old NBT config in memory.

## Visual ideas for the client

### 1. Eclipse Command Center

A ClickGUI replacement with a central command panel:

- search-first module launch;
- left-side category rail;
- module cards with short status lines;
- right-side live setting inspector;
- bottom bar with ping, TPS, FPS, profile name and active visual theme.

### 2. Profile Carousel

A profile switcher screen:

- cards for PvP, Utility, Builder, Visual, Safe;
- last save time;
- warning if a profile has no backup;
- one-click save/load.

### 3. Inventory Ghost Overlay

When `InventoryPresets` is active:

- draw faint ghost icons in target slots;
- show wrong/missing items;
- animate swaps with small arrows;
- show “preset complete” state.

### 4. HUD Docking Grid

A modern HUD editor:

- magnetic guides;
- anchor presets;
- opacity preview;
- collision warning when widgets overlap.

### 5. Eclipse Modern theme pass

Visual identity:

- black/graphite base;
- thin cyan/green accent lines;
- compact module cards;
- reduced hard borders;
- soft animated focus glow;
- consistent typography scale.
