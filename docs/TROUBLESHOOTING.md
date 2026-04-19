# Troubleshooting / Решение проблем

## English

### Game Does Not Start

Check:

- Minecraft version is `1.21.11`.
- Fabric Loader is installed.
- Meteor Client is present and matches the same Minecraft version.
- Only one Eclipse jar is in the `mods` folder.
- Java `21` is being used.

If you use Prism Launcher, open the instance `mods` folder and remove older
Eclipse jars before adding a new one.

### Eclipse Category Is Missing

Possible causes:

- The jar is not in the correct `mods` folder.
- Meteor Client is missing.
- The game loaded a different instance.
- Startup failed before addon initialization.

Check the latest log for `Eclipse` and Fabric loading errors.

### Skin Preview Resets

Temporary network errors or invalid PNG files should not erase the last valid
skin. If the preview resets, check that the local file still exists, verify PNG
size, load the skin again, and remove `eclipse-skins/customization.txt` only if
the saved state is corrupted.

### Official Skin Or Cape Does Not Load

Check that the username is valid, network access is available, Mojang services
are reachable, and the active session is a real Microsoft/Minecraft account.
The addon does not spoof account authentication.

### Litematica Printer Does Nothing

Check that Litematica is installed, a schematic is loaded, a placement is
selected, required blocks are available, you are in range, and the target chunk
is loaded.

Start with:

- `blocks-per-tick = 1`
- `tick-delay = 2`
- `build-order = StableSupport`

### Movement Modules Rubberband

Server correction means the server rejected or adjusted your movement. Lower
speed, disable boost/firework assist temporarily, use conservative settings, and
test one movement module at a time.

### Chat Shows Desync Warning

The release avoids rewriting signed chat messages. If desync appears, disable
chat prefix first and check whether another mod is modifying signed chat.

### Notifications Do Not Show

Check that `eclipse-visuals` and `use-custom-notifier` are enabled, HUD is not
hidden, and `max-notifications` is not too low.

## Русский

### Игра не запускается

Проверь:

- Версия Minecraft: `1.21.11`.
- Fabric Loader установлен.
- Meteor Client установлен и подходит под ту же версию Minecraft.
- В папке `mods` лежит только один Eclipse jar.
- Используется Java `21`.

Если используешь Prism Launcher, открой папку `mods` нужного instance и удали
старые Eclipse jar перед добавлением новой версии.

### Нет категории Eclipse

Возможные причины:

- Jar лежит не в той папке `mods`.
- Meteor Client отсутствует.
- Запущен другой instance.
- Игра упала до initialization addon.

Проверь latest log по словам `Eclipse` и Fabric loading errors.

### Skin preview сбрасывается

Временные network errors или invalid PNG не должны стирать последний валидный
скин. Если preview сбрасывается, проверь что local file существует, проверь PNG
size, загрузи скин снова и удаляй `eclipse-skins/customization.txt` только если
сохранённое состояние повреждено.

### Официальный skin или cape не загружается

Проверь валидность ника, интернет, доступность Mojang services и то, что активная
сессия является реальным Microsoft/Minecraft аккаунтом. Addon не spoof-ит
авторизацию аккаунтов.

### Litematica Printer ничего не делает

Проверь, что Litematica установлена, schematic загружена, placement выбран,
нужные блоки есть в инвентаре, ты находишься в range, а target chunk загружен.

Начни с:

- `blocks-per-tick = 1`
- `tick-delay = 2`
- `build-order = StableSupport`

### Movement модули rubberband

Server correction означает, что сервер отклонил или поправил движение. Уменьши
speed, временно отключи boost/firework assist, используй консервативные настройки
и тестируй один movement модуль за раз.

### Chat показывает desync warning

Релиз не переписывает signed chat messages. Если desync появляется, сначала
отключи chat prefix и проверь, не меняет ли signed chat другой мод.

### Notifications не показываются

Проверь, что `eclipse-visuals` и `use-custom-notifier` включены, HUD не скрыт, а
`max-notifications` не стоит слишком низко.
