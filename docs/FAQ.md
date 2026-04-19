# FAQ / Вопросы

## English

### What is Eclipse-Addon?

Eclipse-Addon is a client-side Meteor Client addon for Minecraft `1.21.11`. It
adds visual customization, skin preview tools, chat helpers, target inspection,
movement utilities, velocity handling, and a Litematica printer.

### Is this a standalone client?

No. It is a Meteor addon. You need Fabric Loader and Meteor Client.

### Does it include Meteor Client?

No. Install Meteor separately.

### Is this a stable release?

It is the first clean public release. Visual and utility modules are the safest
starting point. Movement, packet, and printer modules remain advanced and
server-sensitive.

### Which modules should I try first?

Start with `eclipse-visuals`, `chat-fix`, `middle-click-info`,
`eclipse-camera`, and `eclipse-name-guard`. Then test advanced modules one by one.

### Why are some source files not registered as modules?

Some modules are useful for internal diagnostics or server-specific profiles but
are not appropriate for the clean public runtime. They remain in source form so
they can be restored or developed later.

### Can Eclipse switch Minecraft accounts inside the game?

No. That would require changing the authenticated Minecraft session and can break
normal servers, profile keys, and signed chat. Use the launcher to start with
another account.

### Can Eclipse apply official skins and capes?

It can use the official Minecraft Services API for the active authenticated
account. It does not fake authentication and does not modify another account.

### Will other players see my selected skin/cape?

If the official Minecraft Services API successfully updates your account, other
players should see it after Mojang/client caches refresh. Local preview skins are
only local preview.

### Why does chat-fix avoid signed messages?

Minecraft 1.19+ has signed chat. Rebuilding signed messages can trigger desync
warnings. Eclipse only decorates unsigned/system messages.

### Where are skin settings stored?

```text
eclipse-skins/customization.txt
```

## Русский

### Что такое Eclipse-Addon?

Eclipse-Addon - клиентский addon для Meteor Client под Minecraft `1.21.11`. Он
добавляет visual customization, skin preview tools, chat helpers, target
inspection, movement utilities, velocity handling и Litematica printer.

### Это отдельный клиент?

Нет. Это addon для Meteor. Нужны Fabric Loader и Meteor Client.

### Meteor Client входит в комплект?

Нет. Meteor нужно установить отдельно.

### Это стабильный релиз?

Это первый чистый публичный релиз. Visual и utility модули - самый безопасный
старт. Movement, packet и printer модули остаются advanced и server-sensitive.

### Какие модули пробовать первыми?

Начни с `eclipse-visuals`, `chat-fix`, `middle-click-info`, `eclipse-camera` и
`eclipse-name-guard`. Потом тестируй advanced модули по одному.

### Почему некоторые source файлы не зарегистрированы как модули?

Некоторые модули полезны для internal diagnostics или server-specific profiles,
но не подходят для clean public runtime. Они остаются в исходниках, чтобы их
можно было доработать или вернуть позже.

### Может ли Eclipse переключать Minecraft аккаунты внутри игры?

Нет. Это требует смены authenticated Minecraft session и может ломать обычные
сервера, profile keys и signed chat. Для другого аккаунта запускай игру через
launcher с этим аккаунтом.

### Может ли Eclipse применять официальные скины и плащи?

Он может использовать официальный Minecraft Services API для активного
авторизованного аккаунта. Он не подделывает авторизацию и не меняет чужой аккаунт.

### Увидят ли другие игроки выбранный skin/cape?

Если официальный Minecraft Services API успешно обновил аккаунт, другие игроки
должны увидеть изменение после обновления кэшей Mojang/клиента. Local preview
skins видны только локально в preview.

### Почему chat-fix не трогает signed messages?

В Minecraft 1.19+ есть signed chat. Пересборка signed сообщений может вызвать
desync warnings. Eclipse декорирует только unsigned/system сообщения.

### Где хранятся skin settings?

```text
eclipse-skins/customization.txt
```
