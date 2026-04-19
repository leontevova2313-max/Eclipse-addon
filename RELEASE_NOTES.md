# Eclipse-Addon 1.0.0 Release Notes

## English

This is the first clean public release of Eclipse-Addon. The repository now has
clear documentation, a release module list, installation instructions, settings
guidance, troubleshooting, and FAQ pages.

### What Is Ready

- Visual customization core:
  - title screen background;
  - Eclipse logo layout;
  - menu skin preview;
  - custom crosshair;
  - custom notification overlay.
- Chat helper:
  - clickable links in unsigned/system messages;
  - quick private reply suggestions;
  - signed chat is left untouched to avoid desync warnings.
- Middle-click helper:
  - target inspection;
  - player friend add;
  - entity preview;
  - configurable range.
- Skin preview and account appearance tools:
  - local PNG preview;
  - official username skin preview;
  - official cape list for the active session;
  - official cape preview and apply flow.

### Advanced Features

- `litematica-printer`
- `eclipse-elytra`
- `eclipse-velocity`
- `eclipse-flight`
- `eclipse-move`
- `eclipse-no-slow`
- `pearl-phase`
- `ping-spoof`
- `eclipse-server-intel`
- `external-cheat-trace`

These modules are included because they are useful and compile cleanly, but they
are sensitive to server behavior and should be tested with conservative settings.

### Not Included In Runtime Registration

The following modules exist in source form but are not registered in the clean
release runtime:

- `server-diagnostics`
- `server-auto-setup`
- `eclipse-anti-crash`
- `eclipse-custom-packets`

They were excluded from the release module list because they are internal,
server-profile-specific, or better suited for diagnostic builds.

### Recommended First Launch

1. Start Minecraft with Fabric, Meteor, and Eclipse-Addon installed.
2. Open Meteor GUI.
3. Check the `Eclipse` category.
4. Start with stable visual and utility modules.
5. Test advanced movement/printer modules only after confirming the client
   starts cleanly.

### Things To Watch

- If the game crashes on startup, first verify Minecraft/Meteor/Fabric versions.
- If Litematica printer cannot find a schematic, confirm Litematica is installed
  and a placement is loaded.
- If official skin/cape actions fail, check that the active session is a real
  Microsoft/Minecraft account and that the token is valid.
- If a server corrects movement repeatedly, reduce movement module speeds or
  disable the module for that server.

### Build Verification

```powershell
.\gradlew.bat build
```

## Русский

Это первый чистый публичный релиз Eclipse-Addon. Репозиторий теперь содержит
понятную документацию, список модулей релиза, инструкцию установки, описание
настроек, troubleshooting и FAQ.

### Что готово

- Визуальная часть:
  - фон главного экрана;
  - layout логотипа Eclipse;
  - preview скина в меню;
  - кастомный crosshair;
  - кастомный notification overlay.
- Chat helper:
  - кликабельные ссылки в unsigned/system сообщениях;
  - быстрые подсказки для личных сообщений;
  - signed chat не переписывается, чтобы не вызывать desync warnings.
- Middle-click helper:
  - проверка цели;
  - добавление игрока в друзья;
  - preview сущностей;
  - настраиваемая дистанция.
- Инструменты скина и внешнего вида аккаунта:
  - local PNG preview;
  - preview официального скина по нику;
  - список официальных плащей активной сессии;
  - preview и apply flow для официального плаща.

### Продвинутые функции

- `litematica-printer`
- `eclipse-elytra`
- `eclipse-velocity`
- `eclipse-flight`
- `eclipse-move`
- `eclipse-no-slow`
- `pearl-phase`
- `ping-spoof`
- `eclipse-server-intel`
- `external-cheat-trace`

Эти модули включены, потому что они полезны и проект собирается с ними чисто,
но они зависят от поведения сервера. Начинай с консервативных настроек.

### Не зарегистрировано в runtime-релизе

Следующие модули остаются в исходниках, но не регистрируются в чистом runtime:

- `server-diagnostics`
- `server-auto-setup`
- `eclipse-anti-crash`
- `eclipse-custom-packets`

Они исключены из списка модулей релиза, потому что относятся к internal,
server-profile-specific или diagnostic сборкам.

### Рекомендуемый первый запуск

1. Запусти Minecraft с Fabric, Meteor и Eclipse-Addon.
2. Открой Meteor GUI.
3. Проверь категорию `Eclipse`.
4. Начни со стабильных visual и utility модулей.
5. Movement/printer модули тестируй только после того, как клиент чисто стартует.

### На что обратить внимание

- Если игра падает при запуске, сначала проверь версии Minecraft/Meteor/Fabric.
- Если Litematica printer не видит schematic, проверь установку Litematica и загруженный placement.
- Если официальные skin/cape действия не работают, проверь реальную Microsoft/Minecraft-сессию и токен.
- Если сервер постоянно корректирует движение, уменьши скорости movement модулей или отключи модуль на этом сервере.

### Проверка сборки

```powershell
.\gradlew.bat build
```
