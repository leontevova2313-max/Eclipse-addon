# AGENTS.md

Этот файл нужен как короткий вход для Codex / агентов, которые будут дальше обновлять репозиторий.

## Цель проекта
Eclipse Addon — addon для Meteor Client с упором на:
- компактный современный GUI
- Eclipse Modern theme
- visuals / utility / diagnostics / network modules
- аккуратную структуру, пригодную для дальнейшего рефакторинга

## Точки входа
- `src/main/java/eclipse/Eclipse.java` — основной entrypoint аддона
- `src/main/java/eclipse/EclipseConfig.java` — конфиг и часть feature flags
- `src/main/java/eclipse/gui/theme/` — реализация современной темы
- `src/main/java/com/eclipse/mixin/` — mixin-слой GUI и client hooks
- `src/main/java/eclipse/modules/` — модули
- `src/main/resources/fabric.mod.json` — метаданные мода
- `gradle/libs.versions.toml` — версия мода и зависимостей

## Что уже было исправлено
- тема реально подключена
- логика performanceMode очищена
- двойной рендер toast overlay убран
- часть diagnostics стала безопаснее
- часть лишних аллокаций в utility логике уменьшена

## Что менять осторожно
- `LitematicaPrinter`
- `TitleScreenMixin`
- `EclipseConfig`
- рендер-пайплайн темы и overlay
- файлы mixin-конфигурации

## Предпочтительный стиль изменений
- не ломать существующие имена без причины
- выносить повторяющуюся логику в helper/util классы
- не добавлять тяжёлые эффекты, бьющие по FPS
- держать UI компактным и минималистичным
- большие рефакторы описывать в markdown-файлах рядом с кодом

## Перед PR / релизом
- обновить `CHANGELOG.md`
- обновить `RELEASE_NOTES.md`
- если нужен новый релиз, добавить файл в `docs/releases/`
