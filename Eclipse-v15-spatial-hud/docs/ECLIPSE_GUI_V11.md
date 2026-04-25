# Eclipse GUI v11

## What changed
- Rebuilt the own `EclipseClientScreen` into a denser left-rail / module-workspace / detail-panel shell.
- Removed repeated per-frame filtering allocations by caching visible modules and visible settings inside the screen.
- Removed repeated bridge sorting on every call and simplified module counting paths.
- Upgraded the monochrome theme into a more coherent raised/inset surface system.

## Optimization notes
- module list is rebuilt only when section or search changes
- settings list is rebuilt only when selected module changes or a setting is edited
- layout rectangles are rebuilt only on screen init / resize
- repeated `modules()` sorting in the bridge was removed

## Known limit
- full Gradle build was not runnable in the container because the wrapper still tries to download Gradle from the network
- source changes were made to the real project structure and are intended for local verification in the user's environment
