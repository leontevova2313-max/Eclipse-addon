# Eclipse v12

This iteration moves the custom Eclipse shell forward in three areas:

1. Full dark workspace theme
2. HUD editor shell
3. Performance Inspector shell

## Dark theme
The main Eclipse shell now uses a full dark grayscale palette with:
- dark workspace background
- raised and inset surfaces
- dark chips and buttons
- muted and faint text hierarchy
- monochrome diagnostics visuals

## HUD editor shell
The shell now includes a dedicated HUD Editor view with:
- widget list
- central canvas
- inspector panel
- widget visibility toggle
- widget nudging controls
- reset action

This is a shell phase, not the final HUD editor.

## Performance Inspector shell
The shell now includes a dedicated Inspector view with:
- compact / expanded modes
- sampled FPS graph
- module activity graph
- memory trend graph
- warnings panel
- runtime status cards

## Performance notes
The new inspector uses:
- sampled metrics
- bounded arrays
- no heavy tracing
- no per-frame graph data allocations

## Next step
The next correct step is to persist HUD editor state and connect it to the real HUD layout/render path.
