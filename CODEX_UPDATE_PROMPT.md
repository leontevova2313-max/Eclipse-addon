# CODEX_UPDATE_PROMPT.md

Ниже заготовка промта для Codex.

```text
Проект: Eclipse Addon
Текущая версия: 1.0-pre

Работай поверх текущего исходника, не начиная с нуля.

Что важно:
- сохранить существующую структуру проекта
- не ломать Eclipse Modern theme
- не удалять уже внесённые GitHub docs
- не вносить хаотичных rename без причины
- минимизировать удар по FPS
- по возможности выносить повторяющийся код в helper/util классы

Точки входа:
- eclipse.Eclipse
- eclipse.EclipseConfig
- eclipse.gui.theme.*
- com.eclipse.mixin.*
- eclipse.modules.*

Перед изменениями:
1. прочитай README.md
2. прочитай CHANGELOG.md
3. прочитай CODE_REWORK_NOTES.md
4. прочитай MODERN_GUI_NOTES.md
5. проверь AGENTS.md и CODEX_HANDOFF.md

Что нужно от ответа:
1. план изменений по файлам
2. список новых/изменяемых классов
3. готовый код
4. краткое объяснение интеграции
5. что проверить после сборки
```
