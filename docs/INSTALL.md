# Installation

## Requirements

- Minecraft `1.21.11`
- Java `21`
- Fabric Loader `0.18.2` or compatible newer version
- Meteor Client `1.21.11-SNAPSHOT`
- Eclipse Addon jar
- Litematica for `1.21.11` if you want to use `litematica-printer`

Exact dependency versions are defined in:

```text
gradle/libs.versions.toml
```

## Install From Jar

1. Install Fabric Loader for Minecraft `1.21.11`.
2. Install Meteor Client for the same Minecraft version.
3. Download or build the Eclipse Addon jar.
4. Put the jar into the Minecraft instance `mods` folder.
5. Start the game.
6. Open Meteor GUI.
7. Find modules under the `Eclipse` category.

## Build Locally

Windows:

```powershell
.\gradlew.bat build
```

Linux/macOS:

```bash
./gradlew build
```

The compiled jar is created in:

```text
build/libs/
```

## Prism Launcher

1. Open the instance folder.
2. Open `.minecraft/mods`.
3. Remove older Eclipse jar versions.
4. Copy the new Eclipse jar into `mods`.
5. Launch the instance.

Do not keep two Eclipse jars in the same `mods` folder. Fabric can load both and
that can create duplicate modules, mixin conflicts, or confusing behavior.

## First Startup Checklist

- Minecraft version matches `1.21.11`.
- Fabric Loader is installed.
- Meteor Client is present.
- Eclipse Addon jar is present.
- No duplicate Eclipse jars exist.
- Litematica is present only if you need printer support.

## Where Settings Are Stored

Meteor handles module settings through its normal config system.

Eclipse skin/cape preview state is stored under the Minecraft run directory:

```text
eclipse-skins/customization.txt
```
