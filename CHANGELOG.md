# Changelog / История изменений

## 1.0.0 - First Clean Public Release

### English

This is the first documented public release point for Eclipse-Addon. Earlier
work existed as active development changes, but this release is the first one
with a structured module list, release notes, installation guide, settings guide,
and known issue documentation.

#### Added

- Custom Eclipse title screen with safe logo layout.
- Menu skin preview with selected display name above the model.
- Local PNG skin loading through a file chooser.
- Official username skin preview with async loading and cache.
- Official cape loading, selection, preview, and apply flow for the active
  authenticated Minecraft session.
- Custom Eclipse notification queue for module toggles and important addon events.
- Middle-click target inspection with distance setting and entity preview.
- Chat link and name helpers that avoid modifying signed chat messages.
- Litematica printer placement pipeline with candidate filtering, retries,
  temporary skips, and verification.
- Elytra flight state machine with sustained input-based control.
- Velocity packet-side knockback scaling/cancellation.
- Release documentation set.

#### Improved

- Title screen logo and buttons now share a layout model instead of overlapping.
- Skin state is saved and restored more consistently.
- Failed username or PNG loads no longer overwrite the last valid skin preview.
- Official skin preview requests are cached by username.
- Middle-click notifications now include useful player context.
- Notification rendering uses a bounded queue instead of a single transient toast.
- README has been reduced to a clear entry point and links to detailed docs.

#### Fixed

- Vanilla yellow splash text is disabled for the custom title menu.
- Chat desync risk from rebuilding signed chat messages is avoided.
- Clickable text components are preserved when adding link/name helpers to
  unsigned messages.
- Local skin preview no longer resets to fallback after a temporary file or
  network error.

#### Changed

- `server-diagnostics`, `server-auto-setup`, `eclipse-anti-crash`, and
  `eclipse-custom-packets` are not registered in the clean release runtime.
  They remain in source form as internal/not-for-release code.
- Official cape management is intentionally limited to the current authenticated
  Minecraft account. The addon does not spoof accounts.
- Release documentation is now bilingual: English first, Russian second.

#### Known Issues

- Movement modules remain server-sensitive and should be treated as advanced
  server-dependent tools.
- `litematica-printer` depends on schematic shape, inventory, block support,
  server timing, and Litematica compatibility.
- Official skin/cape updates may take time to propagate through Mojang services.
- Multi-account support does not switch the Minecraft session inside a running
  client. Use the launcher to start with another account.

### Русский

Это первая оформленная публичная точка релиза Eclipse-Addon. Раньше проект
развивался как набор активных изменений, а этот релиз впервые содержит
структурированный список модулей, release notes, инструкцию установки, описание
настроек и known issues.

#### Добавлено

- Кастомный главный экран Eclipse с безопасным layout логотипа.
- Preview скина в меню с выбранным именем над моделью.
- Загрузка локального PNG скина через file chooser.
- Preview официального скина по нику с async загрузкой и кэшем.
- Загрузка, выбор, preview и apply flow официальных плащей для активной
  авторизованной Minecraft-сессии.
- Кастомная очередь уведомлений Eclipse для переключения модулей и важных событий addon.
- Middle-click проверка целей с настройкой дистанции и preview сущностей.
- Chat helpers для ссылок и имён без переписывания signed chat сообщений.
- Litematica printer pipeline с фильтрацией целей, retry, temporary skip и verification.
- Elytra flight state machine с sustained input-based control.
- Velocity packet-side scaling/cancellation для knockback.
- Комплект релизной документации.

#### Улучшено

- Логотип и кнопки главного экрана теперь используют общую layout-модель и не накладываются друг на друга.
- Состояние скина сохраняется и восстанавливается стабильнее.
- Ошибка загрузки ника или PNG больше не затирает последний валидный preview скина.
- Официальные skin preview запросы кэшируются по нику.
- Middle-click уведомления теперь показывают полезный контекст игрока.
- Notifier использует ограниченную очередь вместо одиночного transient toast.
- README стал входной страницей с понятными ссылками на подробные документы.

#### Исправлено

- Vanilla yellow splash text отключён для кастомного главного меню.
- Риск chat desync от пересборки signed chat сообщений устранён.
- Clickable text components сохраняются при добавлении link/name helpers в unsigned сообщения.
- Local skin preview больше не сбрасывается на fallback после временной file/network ошибки.

#### Изменено

- `server-diagnostics`, `server-auto-setup`, `eclipse-anti-crash` и
  `eclipse-custom-packets` не регистрируются в clean release runtime.
  Они остаются в исходниках как internal/not-for-release код.
- Управление официальными плащами намеренно ограничено текущим авторизованным
  Minecraft аккаунтом. Addon не spoof-ит аккаунты.
- Релизная документация теперь двуязычная: сначала английский, затем русский.

#### Известные ограничения

- Movement модули зависят от сервера и должны считаться advanced server-dependent tools.
- `litematica-printer` зависит от формы schematic, инвентаря, block support,
  server timing и совместимости с Litematica.
- Официальные обновления skin/cape могут не сразу появляться у других игроков из-за кэшей Mojang/клиента.
- Multi-account support не переключает Minecraft-сессию внутри уже запущенного клиента.
  Для другого аккаунта запускай игру через launcher с этим аккаунтом.
