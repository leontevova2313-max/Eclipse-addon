# Eclipse v20 — motion, hover and system-signal pass

## Что добавлено

### Верхний dock
- hover-реакция на иконки;
- строка статуса в top-bar теперь меняется по наведению;
- анимированная нижняя signal-line в доке при открытии GUI.

### Workspace / HUD / Spatial
- hover-подсветка строк;
- активные модули получают более агрессивный alert-marker справа;
- активные модули читаются визуально быстрее;
- видимые HUD-виджеты тоже получили alert-marker.

### Общий эффект
- GUI ощущается живее;
- стало понятнее, что сейчас активно, что выбрано и куда навёлся курсор;
- top-bar выглядит как системная управляющая панель, а не просто набор кнопок.

## Изменённые файлы
- `src/main/java/eclipse/gui/client/EclipseClientScreen.java`
- `src/main/java/eclipse/gui/client/EclipseClientTheme.java`
