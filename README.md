# Eclipse Addon

<p align="center">
  <img src="docs/eclipse_logo.png" alt="Eclipse" width="720">
</p>

[English version](README_EN.md)

Eclipse - клиентский аддон для Meteor Client под Minecraft `1.21.11`.
Он добавляет модули для диагностики сервера, настройки движения, визуального
оформления Eclipse, печати схем через Litematica и небольших удобных функций.

Это Fabric-мод для клиента. Он должен запускаться вместе с Meteor Client.
Используй его только там, где такие клиентские инструменты разрешены правилами
сервера.

## Статус проекта

- Проект находится в бета-стадии.
- Проект на 100% сделан ИИ по пользовательским требованиям и правкам.
- Разработка и настройка ведутся только под сервер `play.karasique.com`.
  Работа на других серверах не гарантируется.
- `litematica-printer` является переработанным аддоном от игрока `twilight`,
  адаптированным под текущий проект и его серверные особенности.

## Возможности

### Визуал и интерфейс

- `eclipse-visuals` меняет главный экран, фон меню, брендинг, поведение
  прицела и локальный предпросмотр скина/плаща.
- `eclipse-camera` настраивает FOV от первого лица, высоту камеры и
  наведение на блоки через камеру.
- `middle-click-info` добавляет верхнее всплывающее уведомление при нажатии
  колесиком мыши. Если цель - игрок, он добавляется в друзья Meteor. Если цель
  блок или другая сущность, показывается имя, registry id и координаты блока,
  где это применимо.

### Диагностика сервера

- `server-diagnostics` записывает коррекции сервера, velocity, движение,
  пакеты, активные модули и сформированный анализ.
- `eclipse-server-intel` объединяет NewChunks, SoundLocator, лог координат и
  лог обновлений руд.
- `eclipse-custom-packets` отправляет контролируемые packet-pulse действия для
  тестирования поведения сервера.
- `external-cheat-trace` пишет packet-level лог действий других клиентских
  модулей/читов, загруженных в тот же Fabric-инстанс. Нужен для анализа
  закрытых функций без исходного кода.
- `eclipse-name-guard` сообщает о дублях имён модулей Meteor до того, как они
  приведут к конфликтам.
- `eclipse-anti-crash` отменяет часть подозрительных пакетов, которые могут
  дестабилизировать клиент.

### Движение и служебные модули

- `eclipse-move` даёт настраиваемое движение с осторожными значениями.
- `eclipse-flight` содержит PacketFly, flight, glide, boost и jetpack-профили.
- `eclipse-elytra` добавляет elytra fly, ground glide и chestplate fake-fly.
- `eclipse-no-slow` использует множители движения и slot/offhand packet pulses.
- `eclipse-velocity` меняет knockback или включает режимы отмены velocity.
- `pearl-phase` бросает жемчуг около стены и отправляет настраиваемую
  последовательность phase-пакетов.
- `ping-spoof` задерживает выбранные latency-пакеты и отправляет их после
  заданной паузы.

### Litematica Printer

- `litematica-printer` читает загруженную схему Litematica через reflection
  bridge и ставит нужные блоки с настраиваемым темпом.
- Принтер умеет рендерить текущую очередь постановки блоков в мире.
- Полоска опыта может заменяться на прогресс строительства выбранной схемы.
- Есть проверки безопасности: сущности на пути, TPS pause, correction pause,
  retry delay, защита от falling blocks, перенос предметов в хотбар и swap-back.

Litematica не обязательна для запуска аддона, но нужна для работы
`litematica-printer` с загруженной схемой.

## Требования

- Minecraft `1.21.11`
- Java `21`
- Fabric Loader `0.18.2` или совместимая версия новее
- Meteor Client `1.21.11-SNAPSHOT`
- Litematica для `1.21.11`, если нужен `litematica-printer`

Точные версии зависимостей указаны в
[`gradle/libs.versions.toml`](gradle/libs.versions.toml).

## Установка

1. Установи Fabric Loader для Minecraft `1.21.11`.
2. Установи Meteor Client под ту же версию Minecraft.
3. Собери или скачай jar-файл Eclipse Addon.
4. Положи jar Eclipse в папку `mods` рядом с Meteor Client.
5. Запусти игру и открой список модулей Meteor.
6. Модули будут в категории `Eclipse`.

## Сборка

В Windows:

```powershell
.\gradlew.bat build
```

В Linux или macOS:

```bash
./gradlew build
```

Готовый jar появляется в папке:

```text
build/libs/
```

## Разработка

Основные пути проекта:

- `src/main/java/eclipse/Eclipse.java` регистрирует категорию и модули аддона.
- `src/main/java/eclipse/modules/` содержит модули Eclipse.
- `src/main/java/eclipse/gui/` содержит GUI и overlay-код.
- `src/main/java/com/eclipse/mixin/` содержит client mixins.
- `src/main/resources/eclipse.mixins.json` регистрирует mixins.
- `src/main/resources/fabric.mod.json` содержит metadata Fabric-мода.
- `src/main/resources/assets/eclipse/` содержит текстуры и языковые файлы.

После изменения кода или ресурсов запускай:

```powershell
.\gradlew.bat build
```

## GitHub Actions

В репозитории есть два workflow:

- `dev_build.yml` собирает проект при каждом push и публикует snapshot artifact.
- `pull_request.yml` собирает pull request и загружает compiled artifacts.

## Заметки

- Поведение серверов сильно отличается. Movement и packet-модули лучше считать
  диагностическими инструментами, которые нужно настраивать под конкретный
  сервер.
- Аддон не включает в себя Meteor Client, Fabric Loader, Litematica или сам
  Minecraft.
- Gradle output, runtime-файлы Minecraft и IDE-файлы исключены через
  `.gitignore`.

## Лицензия

Проект использует лицензию `CC0-1.0`, текст находится в [`LICENSE`](LICENSE).
