# Installation / Установка

## English

### Requirements

- Minecraft `1.21.11`
- Java `21`
- Fabric Loader `0.18.2` or compatible newer version
- Meteor Client `1.21.11-SNAPSHOT`
- Eclipse-Addon jar
- Litematica for `1.21.11` if you want to use `litematica-printer`

Exact dependency versions are defined in:

```text
gradle/libs.versions.toml
```

### Install From Jar

1. Install Fabric Loader for Minecraft `1.21.11`.
2. Install Meteor Client for the same Minecraft version.
3. Download or build the Eclipse-Addon jar.
4. Put the jar into the Minecraft instance `mods` folder.
5. Start the game.
6. Open Meteor GUI.
7. Find modules under the `Eclipse` category.

### Build Locally

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

### Prism Launcher

1. Open the instance folder.
2. Open `.minecraft/mods`.
3. Remove older Eclipse jar versions.
4. Copy the new Eclipse jar into `mods`.
5. Launch the instance.

Do not keep two Eclipse jars in the same `mods` folder. Fabric can load both and
that can create duplicate modules, mixin conflicts, or confusing behavior.

### First Startup Checklist

- Minecraft version matches `1.21.11`.
- Fabric Loader is installed.
- Meteor Client is present.
- Eclipse-Addon jar is present.
- No duplicate Eclipse jars exist.
- Litematica is present only if you need printer support.

### Where Settings Are Stored

Meteor handles module settings through its normal config system.

Eclipse skin/cape preview state is stored under the Minecraft run directory:

```text
eclipse-skins/customization.txt
```

## Русский

### Требования

- Minecraft `1.21.11`
- Java `21`
- Fabric Loader `0.18.2` или совместимая новая версия
- Meteor Client `1.21.11-SNAPSHOT`
- Jar файл Eclipse-Addon
- Litematica для `1.21.11`, если нужен `litematica-printer`

Точные версии зависимостей указаны здесь:

```text
gradle/libs.versions.toml
```

### Установка из jar

1. Установи Fabric Loader для Minecraft `1.21.11`.
2. Установи Meteor Client для той же версии Minecraft.
3. Скачай или собери jar Eclipse-Addon.
4. Положи jar в папку `mods` нужного Minecraft instance.
5. Запусти игру.
6. Открой Meteor GUI.
7. Найди модули в категории `Eclipse`.

### Локальная сборка

Windows:

```powershell
.\gradlew.bat build
```

Linux/macOS:

```bash
./gradlew build
```

Собранный jar появится здесь:

```text
build/libs/
```

### Prism Launcher

1. Открой папку instance.
2. Открой `.minecraft/mods`.
3. Удали старые версии Eclipse jar.
4. Скопируй новый Eclipse jar в `mods`.
5. Запусти instance.

Не держи две версии Eclipse jar в одной папке `mods`. Fabric может загрузить обе,
что приведёт к дублям модулей, mixin conflicts или странному поведению.

### Проверка первого запуска

- Версия Minecraft совпадает с `1.21.11`.
- Fabric Loader установлен.
- Meteor Client находится в `mods`.
- Eclipse-Addon jar находится в `mods`.
- В `mods` нет дублей Eclipse jar.
- Litematica установлена только если нужен printer.

### Где хранятся настройки

Meteor хранит настройки модулей через свою обычную config-систему.

Состояние skin/cape preview Eclipse хранится в директории запуска Minecraft:

```text
eclipse-skins/customization.txt
```
