# CODEX_HANDOFF.md

## Что это
Короткая сводка для Codex, чтобы продолжать проект без повторного разбора с нуля.

## Текущее состояние
- исходник уже содержит реальные фиксы, а не только декоративные markdown-файлы
- тема `Eclipse Modern` встроена в проект
- GitHub docs уже добавлены
- версия сейчас считается `1.0-pre`

## На что смотреть в первую очередь
1. `README.md`
2. `CHANGELOG.md`
3. `CODE_REWORK_NOTES.md`
4. `MODERN_GUI_NOTES.md`
5. `src/main/java/eclipse/gui/theme/`
6. `src/main/java/eclipse/EclipseConfig.java`

## Самые рискованные зоны
- `LitematicaPrinter`
- `TitleScreenMixin`
- любая логика, влияющая на render/update ticks
- любые изменения в mixin targets

## Если нужно обновить релиз
- поменять версию в `gradle/libs.versions.toml`
- при необходимости поправить `fabric.mod.json` description
- обновить `CHANGELOG.md`
- обновить `RELEASE_NOTES.md`
- создать новый файл в `docs/releases/`
- обновить `RELEASE_BODY_*.md`

## Если нужно продолжить UI-рефактор
- расширять покрытие темы через `eclipse/gui/theme/widgets`
- не делать тяжёлый blur
- предпочтительны простые тени, полупрозрачность и чистые состояния элементов
