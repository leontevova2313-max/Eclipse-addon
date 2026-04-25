# Eclipse v19 — custom icon dock and alert panel style

## Что изменено

### Верхняя панель

- буквенные обозначения заменены на рисованные мини-иконки;
- иконки рисуются кодом, без отдельных png-ассетов;
- top dock остался компактным и не жрёт место.

### Иконки

Добавлены компактные glyph-иконки:

- `workspace` — 4 секции / control grid;
- `spatial` — locator / route;
- `hud` — экран / overlay frame;
- `inspector` — inspector / magnifier frame;
- `theme-dark` — split disk / dark theme;
- `theme-light` — sun-like signal;
- `close` — compact kill/close mark.

### Визуал

Усилен alert-характер интерфейса:

- более заметные оранжево-красные акценты;
- дополнительная напряжённая подсветка выбранных карточек;
- более выраженный industrial / system-warning характер.

## Файлы

- `src/main/java/eclipse/gui/client/EclipseClientTheme.java`
- `src/main/java/eclipse/gui/client/EclipseClientScreen.java`
