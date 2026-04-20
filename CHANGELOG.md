# Changelog

Все заметные изменения по проекту фиксируются здесь.

## [Unreleased]

### Planned
- дополнительная compile/runtime проверка после локальной подтяжки зависимостей
- дальнейший распил `LitematicaPrinter` на более мелкие классы
- расширение покрытия темы на редкие виджеты Meteor
- дополнительная шлифовка title screen

## [1.0-pre] - 2026-04-20

### Added
- GitHub-ready структура репозитория
- issue templates и pull request template
- release checklist и technical overview
- preview-скриншот современного GUI
- `AGENTS.md` для Codex / агентов
- `CODEX_HANDOFF.md`
- `CODEX_UPDATE_PROMPT.md`
- `RELEASE_BODY_v1.0-pre.md`
- `docs/releases/v1.0-pre.md`

### Changed
- версия проекта переведена на `1.0-pre`
- README переписан под реальный репозиторий `Eclipse-addon`
- release-документация обновлена под первый pre-release
- структура репозитория упрощена для дальнейших обновлений через Codex

### Fixed
- реально подключена и применяется тема `Eclipse Modern`
- исправлен импорт `SettingColor` в теме
- исправлена логика `performanceMode` / `adaptivePerformance`
- убран двойной рендер toast overlay
- убран хардкод `5.0` в `GameRendererMixin`
- убран server-specific хардкод в diagnostics
- повышена безопасность snapshot-экспорта в `DiagnosticStore`
- добавлен отсутствующий `transition_glow.png`
- уменьшена часть лишних аллокаций в `LitematicaPrinter`
- добавлен кэш skin/profile для title screen

### Notes
- это исходник, а не только release-пакет
- рекомендуется локально прогнать `./gradlew build`
- рекомендуется вручную проверить ClickGUI, HUD editor и title screen
