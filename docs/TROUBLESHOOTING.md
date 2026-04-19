# Troubleshooting

## Game Does Not Start

Check:

- Minecraft version is `1.21.11`.
- Fabric Loader is installed.
- Meteor Client is present and matches the same Minecraft version.
- Only one Eclipse jar is in the `mods` folder.
- Java `21` is being used.

If you use Prism Launcher, open the instance `mods` folder and remove older
Eclipse jars before adding a new one.

## Eclipse Category Is Missing

Possible causes:

- The jar is not in the correct `mods` folder.
- Meteor Client is missing.
- The game loaded a different instance.
- Startup failed before addon initialization.

Check the latest log for `Eclipse` and Fabric loading errors.

## Yellow Splash Text Appears On Main Menu

In the custom Eclipse title menu, vanilla splash text should be disabled.

If it appears:

- confirm that the current jar is the new build;
- remove old Eclipse jars;
- verify `eclipse-visuals` and title logo settings;
- restart the game.

## Skin Preview Resets

Expected behavior:

- temporary network errors should not erase the last valid skin;
- invalid PNG files should not erase the last valid local skin;
- selected state should be restored from `eclipse-skins/customization.txt`.

If it resets:

- check that the selected local file still exists;
- check the PNG size;
- try loading the skin again from the customization screen;
- remove a broken `customization.txt` only if the saved state is corrupted.

## Official Skin By Username Does Not Load

Check:

- username is 1-16 characters;
- username contains only letters, numbers, and underscores;
- network access is available;
- Mojang/Minecraft services are reachable.

The addon caches successful username loads. Failed loads should not replace the
previous valid preview.

## Official Cape List Does Not Load

Official capes require the active authenticated Minecraft session.

Check:

- you are logged into a real Microsoft/Minecraft account;
- the current account owns capes;
- the active account in the Eclipse screen is the same as the launched Minecraft
  session;
- network access is available.

The addon does not spoof account auth. To manage another account's capes, launch
the game through the launcher with that account.

## Litematica Printer Does Nothing

Check:

- Litematica is installed.
- A schematic is loaded.
- A placement is selected.
- Required blocks are in hotbar or inventory.
- You are within placement range.
- The target chunk is loaded.

Start with:

- `blocks-per-tick = 1`
- `tick-delay = 2`
- `build-order = StableSupport`

## Printer Repeats The Same Position

Common causes:

- missing support block;
- entity collision;
- wrong block state;
- block requires special placement;
- server rejects fast placement;
- chunk or schematic state changed.

Try:

- lower `blocks-per-tick`;
- increase `retry-delay`;
- increase `skip-impossible-ticks`;
- place support blocks manually.

## Elytra Or Movement Modules Rubberband

Server correction means the server rejected or adjusted your movement.

Try:

- lower horizontal/vertical speed;
- disable boost/firework assist temporarily;
- use conservative/server-safe settings;
- test one movement module at a time.

## Velocity Does Not Fully Cancel Knockback

Possible causes:

- server sends a correction after velocity handling;
- another module modifies movement;
- explosion settings are separate from normal velocity settings;
- mode is not set to cancel/scale as expected.

Test in a controlled environment with one module enabled.

## Chat Shows Desync Warning

The release avoids rewriting signed chat messages. If desync appears:

- disable chat prefix first;
- test with clickable links/names still enabled;
- check whether another mod is modifying signed chat.

## Notifications Do Not Show

Check:

- `eclipse-visuals` is enabled;
- `use-custom-notifier` is enabled;
- HUD is not hidden;
- max notifications is not set too low.

If you prefer Meteor chat feedback, disable `use-custom-notifier`.

