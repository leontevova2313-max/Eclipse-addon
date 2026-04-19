# FAQ

## What is Eclipse Addon?

Eclipse Addon is a client-side Meteor Client addon for Minecraft `1.21.11`. It
adds visual customization, skin preview tools, chat helpers, target inspection,
movement utilities, velocity handling, and a Litematica printer.

## Is this a standalone client?

No. It is a Meteor addon. You need Fabric Loader and Meteor Client.

## Does it include Meteor Client?

No. Install Meteor separately.

## Is this a stable release?

It is the first clean public release. Some modules are stable enough
for normal use, while movement, packet, and printer modules remain advanced or server-sensitive.

## Which modules should I try first?

Start with:

- `eclipse-visuals`
- `chat-fix`
- `middle-click-info`
- `eclipse-camera`
- `eclipse-name-guard`

Then test advanced modules one by one.

## Why are some source files not registered as modules?

Some modules are useful for internal diagnostics or server-specific profiles but
are not appropriate for the first clean public runtime. They remain in source
form so they can be restored or developed later.

## Can Eclipse switch Minecraft accounts inside the game?

No. That would require changing the authenticated Minecraft session and can break
normal servers, profile keys, and signed chat.

Eclipse can remember a preview username and load that skin for menu preview, but
official cape management only works for the account that launched the game.

## Can Eclipse apply official skins and capes?

It can use the official Minecraft Services API for the active authenticated
account. It does not fake authentication and does not modify another account.

## Will other players see my selected skin/cape?

If the official Minecraft Services API successfully updates your account, other
players should see it on normal servers after Mojang/client caches refresh.

Local preview skins are only local preview and are not visible to others as your
official account skin.

## Why does Litematica printer need Litematica?

The printer reads the loaded schematic and selected placement from Litematica. It
cannot print without a schematic source.

## Is Litematica printer safe on every server?

No. It depends on server rules, anticheat, ping, TPS, and block placement rules.
Use conservative settings and test carefully.

## Why do movement modules rubberband?

Rubberbanding means the server corrected your movement. Reduce speed, use
server-safe modes, or disable the module on that server.

## Why does chat-fix avoid signed messages?

Minecraft 1.19+ has signed chat. Rebuilding signed messages can trigger desync
warnings. Eclipse only decorates unsigned/system messages for clickable helpers.

## Where are skin settings stored?

In the Minecraft run directory:

```text
eclipse-skins/customization.txt
```

## Where do I report problems?

Use the GitHub repository issue tracker if available. Include:

- Minecraft version;
- Fabric Loader version;
- Meteor version;
- Eclipse version;
- latest log;
- module settings;
- what you expected and what happened.

