# Settings Guide / Настройки

## English

This guide explains the important settings and why the defaults are conservative.
It does not list every tiny setting; it focuses on values users are most likely
to change.

### General Rule

Start conservative. Increase values only after confirming the client works on
your server. For movement, printer, and packet modules, safer values usually
mean fewer actions per tick, lower speed, longer retry delay, smaller range, and
fewer packet pulses.

### eclipse-visuals

- `screen-backgrounds`: enables Eclipse backgrounds on supported screens.
  Enabled by default because it is a core visual feature and does not affect
  server behavior.
- `performance-mode` / `adaptive-performance`: limits heavier menu effects.
  Keep adaptive mode enabled on weaker machines.
- `title-logo`, `logo-auto-scale`, `logo-safe-margin`: control logo visibility,
  resolution-aware scaling, and spacing from buttons. Auto-scale and safe margin
  prevent overlap across GUI scales.
- `custom-background`: optional local image path. Disabled by default because
  bad paths or huge images are user-specific.
- Crosshair settings: `crosshair`, `style`, `gap`, `length`, `thickness`,
  `dynamic-gap`, `movement-expansion`, `recoil-simulation`, `opacity`,
  `rainbow`. Use moderate values for best performance.

### Notifier

- `use-custom-notifier`: replaces Meteor chat toggle feedback with Eclipse
  overlay notifications.
- `position`: controls where notifications appear. Top-right avoids most vanilla
  HUD elements.
- `max-notifications`: limits visible notification count. Safe range: `3` to `5`.
- `duration`: visible time in milliseconds. Safe range: `2500` to `4000`.

### chat-fix

- `prefix`: optional outgoing message prefix. Disable it if a server rejects
  modified chat.
- `clickable-links`: makes links clickable in unsigned/system messages.
- `click-names` / `reply-command`: makes detected player names suggest a private
  message command. Example: `/msg {name} `.

### middle-click-info

- `range`: maximum target inspection distance. Default `6.0` is close to normal
  interaction distance.
- `cancel-middle-click`: cancels vanilla pick-block after Eclipse handles the target.
- `entity-preview`: highlights player/mob target under the crosshair.

### litematica-printer

- `blocks-per-tick`: recommended `1` for stable server behavior.
- `interaction-range`: recommended around `4.5` to `5.0`.
- `build-order`: recommended `StableSupport`.
- `retry-delay`: recommended around `12` ticks.
- `max-retries`: recommended `3` before temporary skip.

### Movement / Packet Modules

For `eclipse-elytra`, `eclipse-move`, `eclipse-flight`, `eclipse-no-slow`,
`pearl-phase`, and `ping-spoof`, change one setting at a time and test in a safe
environment. Lower speed and longer delays are usually more stable.

For `eclipse-velocity`, test horizontal/vertical scaling carefully. Use `50/50`
for conservative testing and `0/0` only when testing full cancel.

### Skin and Cape Settings

Skin/cape state is stored in:

```text
eclipse-skins/customization.txt
```

Accepted local skin PNG sizes for preview:

- `64x64`
- `64x32`
- `128x128`

Official account skin/cape actions use the active Minecraft session token and do
not spoof another account.

## Русский

Этот документ объясняет важные настройки и почему значения по умолчанию выбраны
консервативно. Здесь не перечислена каждая мелочь; фокус на настройках, которые
пользователь реально будет менять.

### Общее правило

Начинай с консервативных значений. Увеличивай их только после проверки на своём
сервере. Для movement, printer и packet модулей безопаснее обычно означает:
меньше действий за tick, ниже скорость, больше retry delay, меньше range и меньше
packet pulses.

### eclipse-visuals

- `screen-backgrounds`: включает фоны Eclipse на поддерживаемых экранах.
  Включено по умолчанию, потому что это основная визуальная функция и она не
  влияет на сервер.
- `performance-mode` / `adaptive-performance`: ограничивает тяжёлые эффекты меню.
  На слабых ПК adaptive mode лучше оставлять включённым.
- `title-logo`, `logo-auto-scale`, `logo-safe-margin`: управляют логотипом,
  auto-scale под разрешение и отступом от кнопок. Это предотвращает overlap на
  разных GUI scale.
- `custom-background`: optional путь к локальному изображению. По умолчанию
  выключено, потому что путь и размер файла зависят от пользователя.
- Crosshair settings: `crosshair`, `style`, `gap`, `length`, `thickness`,
  `dynamic-gap`, `movement-expansion`, `recoil-simulation`, `opacity`,
  `rainbow`. Для производительности используй умеренные значения.

### Notifier

- `use-custom-notifier`: заменяет Meteor chat feedback на overlay уведомления Eclipse.
- `position`: место уведомлений. Top-right обычно не мешает vanilla HUD.
- `max-notifications`: лимит видимых уведомлений. Безопасно: `3`-`5`.
- `duration`: время показа в миллисекундах. Безопасно: `2500`-`4000`.

### chat-fix

- `prefix`: optional prefix для исходящих сообщений. Отключи, если сервер не
  принимает изменённый чат.
- `clickable-links`: делает ссылки кликабельными в unsigned/system сообщениях.
- `click-names` / `reply-command`: делает найденные имена игроков подсказкой для
  личного сообщения. Пример: `/msg {name} `.

### middle-click-info

- `range`: максимальная дистанция проверки цели. Default `6.0` близок к обычной
  дистанции взаимодействия.
- `cancel-middle-click`: отменяет vanilla pick-block после обработки цели Eclipse.
- `entity-preview`: подсвечивает player/mob цель под прицелом.

### litematica-printer

- `blocks-per-tick`: рекомендовано `1` для стабильности на серверах.
- `interaction-range`: рекомендовано около `4.5`-`5.0`.
- `build-order`: рекомендовано `StableSupport`.
- `retry-delay`: рекомендовано около `12` ticks.
- `max-retries`: рекомендовано `3` перед temporary skip.

### Movement / Packet модули

Для `eclipse-elytra`, `eclipse-move`, `eclipse-flight`, `eclipse-no-slow`,
`pearl-phase` и `ping-spoof` меняй одну настройку за раз и тестируй в безопасном
месте. Меньше скорость и больше delay обычно стабильнее.

Для `eclipse-velocity` отдельно проверяй horizontal/vertical scaling. Используй
`50/50` для осторожного теста и `0/0` только для проверки full cancel.

### Skin и Cape настройки

Состояние skin/cape хранится здесь:

```text
eclipse-skins/customization.txt
```

Валидные размеры local skin PNG для preview:

- `64x64`
- `64x32`
- `128x128`

Официальные действия со skin/cape используют token активной Minecraft-сессии и
не spoof-ят другой аккаунт.
