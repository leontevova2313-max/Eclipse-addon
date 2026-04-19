# Eclipse Beta 2 Overview

<p align="center">
  <img src="eclipse_logo.png" alt="Eclipse" width="720">
</p>

Это одна актуальная страница по состоянию проекта на версии `0.2.0-beta.2`.
Она заменяет разрозненные заметки и короткие описания по файлам.

## Что сейчас представляет собой проект

Eclipse - это Meteor addon для Minecraft `1.21.11`, собранный вокруг четырех основных направлений:

1. визуальный слой клиента и меню;
2. диагностика поведения сервера;
3. movement и packet utility-модули;
4. автоматизация постройки через Litematica printer.

Проект не переписывает Meteor целиком. Он добавляет отдельную категорию `Eclipse` со своими модулями, экранами и mixin-интеграцией.

## Что входит в beta 2

Beta 2 - это не просто новый jar. В этой версии приведены в рабочее состояние несколько ключевых частей проекта:

- title screen layout с отдельной зоной под logo;
- skin management окно с внятным flow загрузки и применения скина;
- структурированный diagnostic module вместо сырого логгера;
- Litematica printer с нормальным placement pipeline;
- ElytraFly с явной state machine и sustained control;
- Velocity с реальной packet-side обработкой knockback;
- EclipseVisuals с расширенными, но не захламленными настройками.

## Как проект устроен

### Регистрация addon

Файл `src/main/java/eclipse/Eclipse.java` регистрирует категорию `Eclipse` и добавляет модули в Meteor.

### Модули

Основные runtime-модули находятся в:

- `src/main/java/eclipse/modules/`

На текущий момент в репозитории зарегистрированы:

- `AntiCrash`
- `CameraTweaks`
- `ChatFix`
- `CustomPackets`
- `DuplicateNameGuard`
- `EclipseFlight`
- `EclipseMove`
- `EclipseNoSlow`
- `EclipseVisuals`
- `ExternalCheatTrace`
- `ExtraElytra`
- `LitematicaPrinter`
- `MiddleClickInfo`
- `PearlPhase`
- `PingSpoof`
- `ServerAutoSetup`
- `ServerDiagnostics`
- `ServerIntel`
- `Velocity`

### GUI и overlay

Файлы GUI находятся в:

- `src/main/java/eclipse/gui/`

Сюда входят:

- `EclipseCustomizationScreen` - окно управления скином;
- `TitleLogoLayout` - единая модель раскладки title screen;
- `EclipseToastOverlay` - верхние всплывающие уведомления;
- `EclipseDynamicTextures` - динамические GUI-текстуры;
- `ConstellationLogoRenderer` - отдельный renderer логотипа.

### Mixin-интеграция

Интеграция с Minecraft GUI и HUD вынесена в:

- `src/main/java/com/eclipse/mixin/`

Через mixin-слой проект встраивает title logo, фон экранов, crosshair, overlays, поведение некоторых экранов и HUD-элементов.

## Что работает сейчас

### 1. Визуал и интерфейс

#### EclipseVisuals

`eclipse-visuals` отвечает за визуальный слой клиента.

Что умеет сейчас:

- кастомный title screen background;
- layout-aware logo placement;
- дополнительные настройки темы, анимаций и layout;
- расширенный crosshair;
- menu background rendering;
- skin preview и связанные UI-элементы.

Как это работает:

- title screen больше не использует две независимые системы координат для logo и кнопок;
- `TitleLogoLayout` считает безопасную область logo, отступы и старт кнопочной группы;
- render логотипа и placement кнопок опираются на одну layout-модель;
- новые visual settings синхронизируются через `EclipseConfig`.

#### CameraTweaks

`eclipse-camera` меняет поведение камеры и FOV без тяжёлой render-логики.

#### MiddleClickInfo

`middle-click-info` показывает полезную информацию по цели под курсором:

- игроки;
- блоки;
- другие сущности.

Если цель - игрок, он может быть добавлен в друзья Meteor. Если цель - блок или сущность, показываются имя, id и связанные данные.

#### ChatFix

`chat-fix` закрывает конкретные проблемы чата и клиентских UI-сценариев, не превращаясь в отдельную систему логов.

### 2. Skin management

Отдельное окно настройки скина уже есть и в beta 2 доведено до более понятного состояния.

Что есть:

- отдельный screen для skin management;
- preview персонажа;
- `Load Skin`;
- `Apply Skin`;
- выбор модели `Auto / Classic / Slim`;
- статусы загрузки и ошибок.

Как это работает:

1. пользователь выбирает PNG через `Load Skin`;
2. PNG валидируется;
3. preview обновляется без постоянного перечитывания файла;
4. `Apply Skin` запускает реальную official upload-логику там, где это доступно;
5. UI не подменяет local preview настоящим успешным apply.

Важно:

- preview и account apply разделены;
- ошибка авторизации или runtime limitation показывается честно;
- head tracking реализован настолько, насколько это позволяет используемый preview path.

### 3. Диагностика сервера

#### ServerDiagnostics

В beta 2 `server-diagnostics` приведён к нормальной диагностической модели.

Что собирает:

- network / packet diagnostics;
- movement / correction diagnostics;
- combat / interaction diagnostics;
- module context snapshots;
- session summary counters.

Как это теперь устроено:

- capture logic отделена от storage;
- storage отделён от export;
- история ограничена bounded buffers;
- verbose packet logging больше не включён по умолчанию;
- summary и counters считаются отдельно от rolling history;
- модуль не пишет бесконечный текстовый шум каждый тик.

Что это даёт:

- меньше мусора;
- понятная структура данных;
- меньше нагрузки на игру;
- пригодный к чтению и анализу session output.

#### ServerIntel

`eclipse-server-intel` объединяет server-side наблюдение вокруг chunks, звуков и других полезных сигналов.

#### CustomPackets

`eclipse-custom-packets` нужен для controlled packet tests и локальной диагностики поведения сервера.

#### ExternalCheatTrace

`external-cheat-trace` помогает анализировать сторонние client modules в той же Fabric-сборке через packet-level trace.

#### DuplicateNameGuard

`eclipse-name-guard` сообщает о конфликтах имён модулей до того, как они создадут трудноловимые проблемы в Meteor.

#### AntiCrash

`eclipse-anti-crash` отсекает часть заведомо подозрительных пакетов, которые могут дестабилизировать клиент.

### 4. Movement и packet utilities

#### EclipseMove

`eclipse-move` - настраиваемое движение с консервативным профилем.

#### EclipseFlight

`eclipse-flight` объединяет packet fly, glide, boost и related movement profiles.

#### EclipseNoSlow

`eclipse-no-slow` работает через multipliers и packet pulses, не смешивая всё в один постоянный tick spam.

#### PingSpoof

`ping-spoof` задерживает выбранные latency-пакеты и выпускает их по контролируемой паузе.

#### PearlPhase

`pearl-phase` автоматизирует последовательность действий вокруг эндер-перла и phase packet sequence.

### 5. ElytraFly

`eclipse-elytra` в beta 2 больше не сводится к одному firework start.

Что исправлено:

- добавлена явная state machine;
- takeoff, glide engage и sustained control разделены;
- correction recovery вынесен в отдельную ветку логики;
- reset состояния происходит корректнее на lifecycle-событиях.

Как это работает теперь:

1. модуль входит в стадию ожидания takeoff;
2. отслеживает реальный вход игрока в glide state;
3. после engagement переходит в sustained flight;
4. horizontal и vertical control завязаны на input игрока;
5. firework остаётся assist-частью, а не единственной логикой полёта.

Это даёт реально управляемый полёт после старта, а не только boost на пару тиков.

### 6. Velocity

`eclipse-velocity` в beta 2 работает через packet/event path, а не через декоративную local подмену без эффекта.

Что умеет:

- full cancel;
- partial scaling;
- раздельный horizontal / vertical multiplier;
- отдельная обработка explosion knockback.

Как это работает:

1. принимается velocity packet;
2. пакет фильтруется только для локального игрока;
3. применяется cancel или scaled replacement;
4. vanilla client получает уже скорректированный packet;
5. итоговое knockback behavior реально меняется в игре.

### 7. Litematica Printer

`litematica-printer` - одна из самых важных частей проекта.

Что он делает сейчас:

- читает выбранный placement через reflection bridge к Litematica;
- сравнивает schematic state и world state;
- ищет следующий осмысленный placement target;
- проверяет предмет, range, visibility и support;
- делает placement attempt;
- подтверждает результат или переводит точку в retry / temporary skip.

Что исправлено в beta 2:

- placement pipeline разделён на scan, selection, placement, verification;
- добавлен pending verification после попытки;
- добавлены fail counters;
- невозможные позиции временно skip'аются;
- candidate selection учитывает более устойчивый build order;
- модуль меньше зацикливается на проблемной точке.

Результат:

- меньше бессмысленных попыток;
- стабильнее работа на реальной схеме;
- лучше синхронизация между world state и schematic state.

### 8. ServerAutoSetup

`server-auto-setup` нужен как прикладной helper для готовых профилей настроек под конкретный серверный сценарий.

Он не заменяет ручную настройку, а задаёт базовые значения для модулей, где это действительно полезно.

## Что пользователь видит в игре

Если смотреть на проект не по исходникам, а глазами игрока, beta 2 сейчас даёт:

- кастомный главный экран Eclipse;
- отдельное окно работы со скином;
- расширенный crosshair;
- уведомления и overlays;
- категорию `Eclipse` в Meteor с movement, diagnostics и utility-модулями;
- printer для построения схем из Litematica;
- диагностический сбор данных по сессии.

## Что стоит понимать про стабильность

Проект рабочий, но он всё ещё beta.

Это значит:

- поведение movement-модулей зависит от сервера;
- некоторые модули требуют реальных игровых условий для корректной работы;
- принтер и elytra logic зависят от мира, ping, server corrections и inventory state;
- часть функций по скинам зависит от текущего auth/runtime контекста.

## Краткая карта по важным файлам

- `src/main/java/eclipse/Eclipse.java` - регистрация addon и модулей;
- `src/main/java/eclipse/EclipseConfig.java` - конфиг visual-слоя;
- `src/main/java/eclipse/modules/EclipseVisuals.java` - visual settings;
- `src/main/java/eclipse/modules/ServerDiagnostics.java` - диагностика;
- `src/main/java/eclipse/modules/LitematicaPrinter.java` - принтер;
- `src/main/java/eclipse/modules/ExtraElytra.java` - ElytraFly;
- `src/main/java/eclipse/modules/Velocity.java` - velocity handling;
- `src/main/java/eclipse/gui/EclipseCustomizationScreen.java` - skin window;
- `src/main/java/eclipse/skins/SkinCustomizationManager.java` - skin loading/apply logic;
- `src/main/java/eclipse/gui/TitleLogoLayout.java` - layout title screen;
- `src/main/resources/eclipse.mixins.json` - список mixin-классов.

## Итог

Beta 2 - это уже не просто шаблонный Meteor addon.

Сейчас это сборка, в которой:

- есть собственный визуальный слой;
- есть диагностическая подсистема;
- есть рабочий Litematica printer;
- есть ElytraFly и Velocity с реальной runtime-логикой;
- есть skin UI и доработанный title screen;
- есть набор служебных и movement-модулей под конкретный практический сценарий использования.

Если нужен один документ, который описывает текущее состояние проекта, это и есть он.
