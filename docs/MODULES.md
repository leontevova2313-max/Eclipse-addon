# Modules / Модули

## English

This page describes the modules that matter for the public release.

### Release Classification

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

- `server-diagnostics`
- `server-auto-setup`
- `eclipse-anti-crash`
- `eclipse-custom-packets`

### Stable Modules

`eclipse-visuals` controls Eclipse menu visuals, title background, logo layout,
crosshair, skin preview, and notification overlay. It is included because it is
the main identity layer of the addon and does not depend on server behavior.

Recommended settings: keep adaptive performance and logo auto-scale enabled, and
use a small notification limit.

`chat-fix` improves chat usability without breaking Minecraft signed chat. It
adds clickable links and quick reply suggestions only where the message can be
decorated safely. Signed chat messages are left untouched.

Recommended settings: keep clickable links and names enabled. Set
`reply-command` to the server format, for example `/msg {name} `.

`middle-click-info` adds target inspection, Meteor friend add workflow, entity
preview, and configurable range. Recommended range is `6.0`.

`eclipse-camera` provides client-side camera/FOV tuning. Keep values conservative.

`eclipse-name-guard` reports duplicate Meteor module names and helps detect addon
conflicts.

### Advanced / Server-Sensitive Modules

`litematica-printer` places blocks from a loaded Litematica schematic using
candidate filtering, inventory checks, placement attempts, verification, retries,
and temporary skips.

Recommended first settings:

- `blocks-per-tick`: `1`
- `tick-delay`: `2`
- `build-order`: `StableSupport`
- `retry-delay`: around `12`
- `max-retries`: `3`
- `pause-when-missing-blocks`: enabled

Known limitations: complex NBT blocks are not fully recreated, directional
blocks can still need manual correction, and server timing can affect placement.

`eclipse-elytra` provides elytra flight profiles and sustained flight control.
Start with conservative speeds and avoid aggressive boosts until tested.

`eclipse-velocity` controls knockback response through packet-side handling. It
supports horizontal and vertical scaling and explosion velocity handling.

Other advanced modules:

- `eclipse-move`
- `eclipse-flight`
- `eclipse-no-slow`
- `pearl-phase`
- `ping-spoof`
- `eclipse-server-intel`
- `external-cheat-trace`

Treat these as server-sensitive tools. Change one setting at a time and test
carefully.

### Internal / Not Registered

`server-diagnostics`, `server-auto-setup`, `eclipse-anti-crash`, and
`eclipse-custom-packets` remain in source form but are not registered in the
clean public runtime.

## Русский

Эта страница описывает модули, которые важны для публичного релиза.

### Классификация релиза

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

- `server-diagnostics`
- `server-auto-setup`
- `eclipse-anti-crash`
- `eclipse-custom-packets`

### Стабильные модули

`eclipse-visuals` управляет визуальной частью меню Eclipse, title background,
layout логотипа, crosshair, skin preview и notification overlay. Модуль включён,
потому что это основной визуальный слой addon и он не зависит от поведения сервера.

Рекомендуемые настройки: оставить adaptive performance и logo auto-scale
включёнными, а лимит уведомлений держать небольшим.

`chat-fix` улучшает удобство чата без поломки Minecraft signed chat. Он добавляет
кликабельные ссылки и быстрые reply suggestions только там, где сообщение можно
безопасно декорировать. Signed chat сообщения не трогаются.

Рекомендуемые настройки: оставить clickable links и names включёнными. Настроить
`reply-command` под сервер, например `/msg {name} `.

`middle-click-info` добавляет проверку цели, добавление игроков в Meteor friends,
preview сущностей и настройку дистанции. Рекомендуемая дистанция: `6.0`.

`eclipse-camera` даёт client-side настройку camera/FOV. Значения лучше держать
консервативными.

`eclipse-name-guard` сообщает о дублях имён Meteor модулей и помогает находить
конфликты addon.

### Продвинутые / server-sensitive модули

`litematica-printer` ставит блоки из загруженной Litematica schematic через
фильтрацию кандидатов, проверку инвентаря, placement attempts, verification,
retries и temporary skips.

Рекомендуемые первые настройки:

- `blocks-per-tick`: `1`
- `tick-delay`: `2`
- `build-order`: `StableSupport`
- `retry-delay`: около `12`
- `max-retries`: `3`
- `pause-when-missing-blocks`: включено

Ограничения: complex NBT blocks не полностью воспроизводятся, directional blocks
могут требовать ручной коррекции, а server timing может влиять на placement.

`eclipse-elytra` добавляет elytra flight profiles и sustained flight control.
Начинай с консервативных скоростей и не включай агрессивные boost значения до теста.

`eclipse-velocity` управляет knockback response через packet-side handling.
Поддерживает horizontal/vertical scaling и explosion velocity handling.

Другие advanced модули:

- `eclipse-move`
- `eclipse-flight`
- `eclipse-no-slow`
- `pearl-phase`
- `ping-spoof`
- `eclipse-server-intel`
- `external-cheat-trace`

Считай их server-sensitive инструментами. Меняй одну настройку за раз и проверяй
поведение аккуратно.

### Internal / не зарегистрированы

`server-diagnostics`, `server-auto-setup`, `eclipse-anti-crash` и
`eclipse-custom-packets` остаются в исходниках, но не регистрируются в чистом
публичном runtime.
