# Eclipse v21 — mac dock, script labels, client editor

## Что изменено

### Верхний док
- верхняя панель переделана ближе к стилю macOS dock / hotbar;
- иконки теперь сидят на отдельной прозрачной tray-плашке;
- при наведении иконки визуально увеличиваются (magnify);
- сверху появляется hover-label.

### Script / handwritten labels
- добавлен отдельный режим `script labels`;
- подписи при наведении в верхнем доке выводятся в более рукописной манере;
- часть заголовков в HUD Studio / Client Editor тоже рисуется в этом стиле.

### HUD Editor → Client Editor
В HUD Editor добавлен встроенный блок `Client Editor`, через который можно переключать:

- `Mac Dock / Classic Dock`
- `Script Labels / Plain Labels`
- `Magnify On / Off`
- `Alert Style / Soft Style`

Это не просто HUD-редактор, а уже мини-редактор самого клиентского UI.

## Изменённые файлы
- `src/main/java/eclipse/gui/client/EclipseClientTheme.java`
- `src/main/java/eclipse/gui/client/EclipseClientScreen.java`
