# Eclipse v17 GUI/HUD fixes

## Исправлено

- Клик по строке модуля в Workspace теперь открывает настройки, а не включает модуль.
- Включение/выключение модуля перенесено на маленький status-dot слева в строке модуля и на отдельный toggle в detail-панели.
- Строка бинда в detail-панели перенесена ниже header-controls, поэтому больше не наезжает на toggle/category/state.
- Текст названия модуля, описания, бинда и setting-title режется по реальной ширине текста, а не по грубому количеству символов.
- Убраны лишние яркие полосы/градиентные заливки в `drawGlassPanel` и search capsule.
- HUD Editor preview теперь строится как scaled full-screen surface, а не как маленький угол canvas.
- Drag/arrow-move HUD widgets теперь clamp-ятся по фактическому scaled screen size, а не по размеру preview-панели.

## Проверка сборки

Команда запускалась:

```bash
./gradlew clean build --stacktrace
```

В текущей среде Gradle wrapper не стартует из-за отсутствия DNS/доступа к `services.gradle.org`, поэтому Java-компиляция Gradle не дошла до стадии `compileJava`.

Ошибка окружения:

```text
java.net.UnknownHostException: services.gradle.org
```

Кодовые изменения изолированы в GUI/HUD-слое:

- `src/main/java/eclipse/gui/client/EclipseClientScreen.java`
- `src/main/java/eclipse/gui/client/EclipseClientTheme.java`
