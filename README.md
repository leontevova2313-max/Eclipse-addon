# Eclipse-Addon

<p align="center">
  <img src="docs/eclipse_logo.png" alt="Eclipse-Addon" width="720">
</p>

## English

Eclipse-Addon is a client-side Meteor Client addon for Minecraft `1.21.11`.
This repository is prepared as a clean public release with documented modules,
settings, installation steps, limitations, and release notes.

### Status

- Release line: public release.
- Current project version: `1.0.0`.
- Minecraft: `1.21.11`.
- Fabric Loader: `0.18.2`.
- Yarn mappings: `1.21.11+build.3`.
- Meteor Client: `1.21.11-SNAPSHOT`.

### What Eclipse-Addon Adds

- Custom Eclipse title screen, menu background, logo layout, skin preview, and notifier.
- Skin preview and official account skin/cape tools for the active Minecraft session.
- Chat quality fixes for links and quick private replies without breaking signed chat.
- Middle-click target inspection with player friend add notifications and entity preview.
- Movement and packet utility modules for controlled testing.
- Litematica printer with range, retry, skip, inventory, and placement verification logic.
- Server intelligence helpers for chunks, sounds, coordinates, and ore-related observations.

### Release Modules

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

- `eclipse-anti-crash`
- `eclipse-custom-packets`
- `server-auto-setup`
- `server-diagnostics`

These internal modules still exist in source form, but they are not added to the
Meteor module list in the clean release runtime.

### Installation

See [docs/INSTALL.md](docs/INSTALL.md).

Short version:

1. Install Fabric Loader for Minecraft `1.21.11`.
2. Install Meteor Client for Minecraft `1.21.11`.
3. Put the Eclipse-Addon jar in your Minecraft `mods` folder.
4. Start the game.
5. Open Meteor GUI and find the `Eclipse` category.

### Documentation

- [Project overview](docs/PROJECT_OVERVIEW.md)
- [Release notes](RELEASE_NOTES.md)
- [Changelog](CHANGELOG.md)
- [Installation](docs/INSTALL.md)
- [Modules](docs/MODULES.md)
- [Settings](docs/SETTINGS.md)
- [Troubleshooting](docs/TROUBLESHOOTING.md)
- [FAQ](docs/FAQ.md)

### Building

```powershell
.\gradlew.bat build
```

The jar is created in:

```text
build/libs/
```

### Important Notes

- Some movement and packet modules depend heavily on server rules, ping, TPS,
  anticheat behavior, and player inventory state.
- Official skin/cape actions use the active Minecraft session only. The addon
  does not fake authentication and does not spoof account identity.
- Litematica is optional for launching the addon, but required for
  `litematica-printer`.
- This addon does not include Minecraft, Meteor Client, Fabric Loader, or
  Litematica.

## Русский

Eclipse-Addon - клиентский addon для Meteor Client под Minecraft `1.21.11`.
Репозиторий подготовлен как чистый публичный релиз: с описанием модулей,
настроек, установки, ограничений и релизных заметок.

### Статус

- Линейка релиза: публичный релиз.
- Текущая версия проекта: `1.0.0`.
- Minecraft: `1.21.11`.
- Fabric Loader: `0.18.2`.
- Yarn mappings: `1.21.11+build.3`.
- Meteor Client: `1.21.11-SNAPSHOT`.

### Что добавляет Eclipse-Addon

- Кастомный главный экран Eclipse, фон меню, layout логотипа, preview скина и notifier.
- Preview скина и официальные инструменты скинов/плащей для активной Minecraft-сессии.
- Исправления чата для ссылок и быстрых личных сообщений без поломки signed chat.
- Middle-click проверка целей, добавление игроков в друзья и preview сущностей.
- Movement и packet utility модули для контролируемого тестирования.
- Litematica printer с range, retry, skip, inventory и verification логикой.
- Server intelligence helpers для чанков, звуков, координат и наблюдений по миру.

### Модули релиза

Стабильные / готовые к обычному использованию:

- `eclipse-visuals`
- `chat-fix`
- `middle-click-info`
- `eclipse-camera`
- `eclipse-name-guard`

Продвинутые / зависят от сервера:

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

Внутренние / не зарегистрированы в этом релизе:

- `eclipse-anti-crash`
- `eclipse-custom-packets`
- `server-auto-setup`
- `server-diagnostics`

Эти внутренние модули остаются в исходниках, но не добавляются в список модулей
Meteor в чистом runtime-релизе.

### Установка

Смотри [docs/INSTALL.md](docs/INSTALL.md).

Коротко:

1. Установи Fabric Loader для Minecraft `1.21.11`.
2. Установи Meteor Client для Minecraft `1.21.11`.
3. Положи jar Eclipse-Addon в папку `mods`.
4. Запусти игру.
5. Открой Meteor GUI и найди категорию `Eclipse`.

### Документация

- [Обзор проекта](docs/PROJECT_OVERVIEW.md)
- [Релизные заметки](RELEASE_NOTES.md)
- [История изменений](CHANGELOG.md)
- [Установка](docs/INSTALL.md)
- [Модули](docs/MODULES.md)
- [Настройки](docs/SETTINGS.md)
- [Решение проблем](docs/TROUBLESHOOTING.md)
- [FAQ](docs/FAQ.md)

### Сборка

```powershell
.\gradlew.bat build
```

Готовый jar создаётся здесь:

```text
build/libs/
```

### Важные замечания

- Movement и packet модули сильно зависят от правил сервера, ping, TPS,
  anticheat, состояния инвентаря и текущей ситуации в игре.
- Официальные действия со скинами и плащами используют только активную
  Minecraft-сессию. Addon не подделывает авторизацию и не spoof-ит аккаунты.
- Litematica не нужна для запуска addon, но нужна для `litematica-printer`.
- Этот addon не включает Minecraft, Meteor Client, Fabric Loader или Litematica.

## License / Лицензия

The project uses the `CC0-1.0` license. See [LICENSE](LICENSE).

Проект использует лицензию `CC0-1.0`. Смотри [LICENSE](LICENSE).
