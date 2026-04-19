# Eclipse-Addon Project Overview / Обзор проекта

<p align="center">
  <img src="eclipse_logo.png" alt="Eclipse-Addon" width="720">
</p>

## English

This page describes the current public release state of Eclipse-Addon version
`1.0.0`.

### What The Project Is

Eclipse-Addon is a Meteor addon for Minecraft `1.21.11`. It adds an `Eclipse`
category to Meteor with visual customization, menu skin preview tools, utility
modules, movement testing modules, and a Litematica printer.

The project does not replace Meteor Client. It extends it.

### Main Areas

1. Visual and menu layer.
2. Skin and cape preview/apply tools.
3. Chat and middle-click utilities.
4. Movement and packet-related modules.
5. Litematica schematic printing.
6. Server information helpers.

### Runtime Module List

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

### Important Files

- `src/main/java/eclipse/Eclipse.java` - addon and module registration.
- `src/main/java/eclipse/modules/` - modules.
- `src/main/java/eclipse/gui/` - GUI and overlay code.
- `src/main/java/eclipse/skins/` - skin/cape state and loading logic.
- `src/main/java/com/eclipse/mixin/` - mixin integration.
- `src/main/resources/assets/eclipse/` - textures and language files.
- `src/main/resources/eclipse.mixins.json` - mixin registration.

### Release Goal

Version `1.0.0` is the first professional public release point for the project:

- the repository has structured documentation;
- stable and advanced modules are clearly separated;
- internal modules are not registered in the clean runtime;
- install, settings, troubleshooting, and FAQ pages are present;
- the jar builds cleanly with Gradle.

## Русский

Эта страница описывает текущее состояние публичного релиза Eclipse-Addon версии
`1.0.0`.

### Что это за проект

Eclipse-Addon - addon для Meteor под Minecraft `1.21.11`. Он добавляет категорию
`Eclipse` в Meteor: визуальную кастомизацию, menu skin preview, utility модули,
movement testing модули и Litematica printer.

Проект не заменяет Meteor Client. Он расширяет Meteor.

### Основные направления

1. Визуальный слой и меню.
2. Skin/cape preview и apply инструменты.
3. Chat и middle-click utilities.
4. Movement и packet-related модули.
5. Печать schematic через Litematica.
6. Server information helpers.

### Список runtime модулей

Зарегистрированы в публичном релизе:

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

Не зарегистрированы в публичном runtime:

- `eclipse-anti-crash`
- `eclipse-custom-packets`
- `server-auto-setup`
- `server-diagnostics`

Эти файлы остаются в исходниках для internal или diagnostic сборок.

### Важные файлы

- `src/main/java/eclipse/Eclipse.java` - addon и регистрация модулей.
- `src/main/java/eclipse/modules/` - модули.
- `src/main/java/eclipse/gui/` - GUI и overlay код.
- `src/main/java/eclipse/skins/` - skin/cape state и loading logic.
- `src/main/java/com/eclipse/mixin/` - mixin integration.
- `src/main/resources/assets/eclipse/` - textures и language files.
- `src/main/resources/eclipse.mixins.json` - регистрация mixin.

### Цель релиза

Версия `1.0.0` - первая профессиональная публичная точка релиза проекта:

- в репозитории есть структурированная документация;
- stable и advanced модули разделены;
- internal модули не зарегистрированы в clean runtime;
- есть install, settings, troubleshooting и FAQ страницы;
- jar чисто собирается через Gradle.
