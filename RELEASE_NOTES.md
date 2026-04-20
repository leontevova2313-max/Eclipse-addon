# Release Notes — v1.0-pre

## Eclipse Addon / 1.0 pre

Это первая **pre-release** версия исходника с:
- интегрированной темой **Eclipse Modern**
- фиксами по GUI и cleanup
- подготовкой репозитория под GitHub
- документами для дальнейшего сопровождения и обновлений через Codex

### Основное
- UI стал компактнее, строже и чище
- тема реально подключена в коде
- diagnostics и конфиг стали аккуратнее
- часть костылей и лишних рендер-путей убрана
- проект упакован как нормальный GitHub-репозиторий

### Изменения
- подключена и автоприменяется `Eclipse Modern`
- исправлен `SettingColor` import
- выровнено поведение `performanceMode`
- убран двойной рендер toast overlay
- reach distance больше не захардкожен `5.0`
- diagnostics очищены от server-specific special-case
- `DiagnosticStore` стал безопаснее для snapshot/export
- добавлен `transition_glow.png`
- `LitematicaPrinter` слегка очищен от части лишних аллокаций
- title screen использует кэш skin/profile данных

### Для GitHub
- обновлён `README.md`
- добавлен `CHANGELOG.md`
- добавлен `RELEASE_BODY_v1.0-pre.md`
- добавлены issue templates и PR template
- добавлены release/check/build docs
- добавлены файлы для Codex handoff

### Репозиторий
- https://github.com/leontevova2313-max/Eclipse-addon

### Рекомендуемые проверки перед публикацией релиза
1. `./gradlew build`
2. запуск клиента через IDE
3. открытие ClickGUI
4. проверка HUD editor
5. проверка title screen
6. проверка overlay/toast
