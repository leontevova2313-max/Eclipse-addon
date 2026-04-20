# Local Build Checklist

## Перед первой публикацией
- [ ] открыть проект в IntelliJ IDEA как Gradle project
- [ ] дождаться индексации и импорта зависимостей
- [ ] проверить SDK / language level = Java 21
- [ ] проверить, что Fabric/Meteor зависимости подтянулись без ошибок

## Сборка
- [ ] выполнить `./gradlew build`
- [ ] убедиться, что jar появился в `build/libs`
- [ ] проверить, что нет compile errors / unresolved symbols

## Запуск
- [ ] запустить клиент из IDE
- [ ] проверить main menu / title screen
- [ ] открыть ClickGUI
- [ ] проверить, что активна тема `Eclipse Modern`
- [ ] проверить категории и окна модулей
- [ ] проверить хотя бы несколько settings widgets
- [ ] проверить toast overlay
- [ ] проверить HUD / overlays, если они используются

## Поведение
- [ ] нет явных лагов при открытии GUI
- [ ] нет двойного рендера уведомлений
- [ ] нет артефактов прозрачности
- [ ] нет regressions после фикса reach distance
- [ ] diagnostics не содержит старого hardcoded special-case

## Release sanity check
- [ ] README отражает текущее состояние проекта
- [ ] CHANGELOG обновлён
- [ ] release notes готовы
- [ ] скриншоты лежат в `docs/screenshots`


## Repo
- Source repo: https://github.com/leontevova2313-max/Eclipse-addon
