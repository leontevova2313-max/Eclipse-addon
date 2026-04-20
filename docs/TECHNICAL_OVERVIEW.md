# Technical Overview — 1.0-pre

# Technical Overview

## База

Проект представляет собой Meteor Addon с пользовательскими категориями, набором модулей и дополнительным UI/theme слоем.

Ключевые зоны:
- `eclipse.api.bootstrap` — регистрация категорий и модулей
- `eclipse.modules.*` — функциональные модули
- `eclipse.gui.theme.*` — тема и виджеты современного интерфейса
- `com.eclipse.mixin.*` — mixin-слой, влияющий на экраны и рендер
- `eclipse.diagnostics.*` — сбор и экспорт диагностических данных

## Theme layer

Тема `Eclipse Modern` вынесена в отдельный слой:
- `EclipseModernTheme`
- `EclipseThemeRenderer`
- `EclipseThemeBootstrap`
- `eclipse.gui.theme.widgets.*`

Задача слоя — не ломая модульную логику, централизованно менять визуальное представление интерфейса.

## Что было исправлено в rework

### 1. Theme integration
Тема не просто добавлена как набор классов, а реально регистрируется и выбирается на инициализации аддона.

### 2. Performance logic cleanup
Поведение `performanceMode` и `adaptivePerformance` приведено к более предсказуемому виду, чтобы визуальные фолбэки не включались скрыто.

### 3. Overlay rendering cleanup
Устранён сценарий с потенциальным двойным рендером toast overlay.

### 4. Reach hardcode removal
Ray distance больше не жёстко прибита к `5.0`, а берётся из текущей клиентской логики взаимодействия.

### 5. Diagnostics cleanup
Диагностика больше не зависит от захардкоженного server special-case и аккуратнее экспортирует состояние наружу.

### 6. Safe performance tweaks
В тяжёлых местах внесены только относительно безопасные оптимизации без чрезмерного риска сломать поведение.

## Ограничения

Полноценная compile verification не была завершена внутри контейнера, потому что Gradle wrapper требует сетевой доступ к внешним зависимостям.

Именно поэтому этот пакет стоит воспринимать как **GitHub-ready и documentation-ready**, но финальную локальную сборку и smoke-test всё равно нужно сделать отдельно.
