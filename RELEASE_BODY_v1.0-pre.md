## Eclipse Addon — v1.0-pre

Первая **pre-release** версия исходника с обновлённым UI, подключённой темой `Eclipse Modern`, cleanup по коду и GitHub-ready структурой.

### Что вошло
- реальная интеграция `Eclipse Modern`
- компактный и более строгий GUI-подход
- фиксы по `performanceMode`
- убран двойной рендер toast overlay
- убран хардкод reach distance
- diagnostics очищены от server-specific special-case
- безопаснее snapshot/export в `DiagnosticStore`
- меньше лишних аллокаций в части `LitematicaPrinter`
- GitHub templates, docs и release-файлы

### Что проверить локально
- `./gradlew build`
- открытие ClickGUI
- HUD editor
- title screen
- overlay / toast rendering
- modules screen и category switching

### Репозиторий
https://github.com/leontevova2313-max/Eclipse-addon
