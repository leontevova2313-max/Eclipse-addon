# Eclipse Addon

<p align="center">
  <img src="docs/eclipse_logo.png" alt="Eclipse" width="720">
</p>

Eclipse - клиентский addon для Meteor Client под Minecraft `1.21.11`.

Главная актуальная сводка по проекту вынесена в одну страницу:

- [Beta 2 Overview](docs/BETA_2.md)
- [English README](README_EN.md)

## Что это

Eclipse собирает в одном addon:

- визуальные улучшения главного меню и интерфейса;
- диагностические инструменты для анализа сервера;
- movement и packet utility-модули;
- Litematica printer;
- окно предпросмотра и загрузки скина.

## Текущее состояние

- текущая версия репозитория: `0.2.0-beta.2`;
- проект находится в beta-стадии;
- основные рабочие области описаны на странице [Beta 2 Overview](docs/BETA_2.md);
- часть модулей ориентирована на конкретный серверный профиль и может требовать подстройки.

## Быстрый старт

1. Установи Fabric Loader для Minecraft `1.21.11`.
2. Установи Meteor Client для той же версии игры.
3. Собери или возьми готовый jar Eclipse.
4. Помести jar в папку `mods` вместе с Meteor Client.
5. Запусти игру и открой категорию `Eclipse` в списке модулей Meteor.

## Сборка

```powershell
.\gradlew.bat build
```

Собранный jar появится в:

```text
build/libs/
```

## Основные пути в проекте

- `src/main/java/eclipse/modules/` - модули Eclipse;
- `src/main/java/eclipse/gui/` - экраны, layout и GUI helper-классы;
- `src/main/java/com/eclipse/mixin/` - mixin-интеграция в клиент;
- `src/main/resources/assets/eclipse/` - текстуры и локализация;
- `src/main/resources/fabric.mod.json` - метаданные Fabric-мода.

## Лицензия

Проект распространяется по лицензии `CC0-1.0`, см. [LICENSE](LICENSE).
