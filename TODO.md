# TODO / Roadmap

## High priority
- локально прогнать `gradlew build` и исправить compile/runtime несовпадения, если проявятся
- проверить полное покрытие `Eclipse Modern` theme на всех экранах Meteor
- проверить title screen rendering на слабом железе
- проверить отсутствие регрессий от фикса toast overlay

## Medium priority
- распилить `LitematicaPrinter` на smaller components:
  - candidate scanning
  - inventory actions
  - retry/cooldown logic
  - render helpers
- вынести общие rendering tokens темы в более централизованный слой
- сократить размер и ответственность `TitleScreenMixin`
- дополнительно пройтись по `ServerDiagnostics` и формату экспорта

## Low priority
- добавить больше реальных скриншотов UI в `docs/screenshots`
- завести tagged releases на GitHub
- добавить автоматическую сборку релизных артефактов
- расширить документацию по настройке темы
