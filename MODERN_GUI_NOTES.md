# Eclipse Modern GUI

В архив добавлена новая компактная тема `Eclipse Modern` для интерфейса Meteor/Eclipse.

## Что изменено
- Полупрозрачные тёмные панели без тяжёлого blur.
- Простые мягкие тени.
- Более строгие заголовки окон.
- Компактные плитки модулей.
- Более чистые кнопки, чекбоксы и слайдеры.
- Обновлён верхний bar/theme look.

## Новые файлы
- `src/main/java/eclipse/gui/theme/EclipseModernTheme.java`
- `src/main/java/eclipse/gui/theme/EclipseThemeRenderer.java`
- `src/main/java/eclipse/gui/theme/EclipseThemeBootstrap.java`
- `src/main/java/eclipse/gui/theme/widgets/EclipseWindow.java`
- `src/main/java/eclipse/gui/theme/widgets/EclipseButton.java`
- `src/main/java/eclipse/gui/theme/widgets/EclipseCheckbox.java`
- `src/main/java/eclipse/gui/theme/widgets/EclipseSlider.java`
- `src/main/java/eclipse/gui/theme/widgets/EclipseModule.java`
- `src/main/java/eclipse/gui/theme/widgets/EclipseTopBar.java`

## Как это работает
Тема теперь не просто добавлена в архив: она реально регистрируется и выбирается при инициализации аддона, а также остаётся подключённой через PostInit-фолбэк.

## Настройки темы
В самой теме добавлены параметры:
- `density`
- `flat-mode`
- `shadows`
- `surface-color`
- `surface-hover-color`
- `header-color`
- `header-hover-color`
- `border-color`
- `row-color`
- `row-hover-color`

## Важно
Сборку внутри контейнера я не смог прогнать, потому что Gradle wrapper требует загрузить внешний дистрибутив, а сеть в среде отключена.


## Дополнительно исправлено
- Исправлен импорт `SettingColor` в теме.
- Убрана рассинхронизация `performanceMode` / `adaptivePerformance`.
- Тени темы автоматически гасятся в режимах пониженной нагрузки.
- Исправлен двойной рендер toast overlay.
- Добавлен недостающий `transition_glow.png`.
