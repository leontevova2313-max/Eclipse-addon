# Changelog

## 0.2.0-beta.2 - First Clean Release

This is the first documented clean release point for Eclipse Addon. Earlier work
existed as active development changes, but this release is the first one with a
structured module list, release notes, installation guide, settings guide, and
known issue documentation.

### Added

- Custom Eclipse title screen with safe logo layout.
- Menu skin preview with selected display name above the model.
- Local PNG skin loading through a file chooser.
- Official username skin preview with async loading and cache.
- Official cape loading, selection, preview, and apply flow for the active
  authenticated Minecraft session.
- Custom Eclipse notification queue for module toggles and important addon events.
- Middle-click target inspection with distance setting and entity preview.
- Chat link and name helpers that avoid modifying signed chat messages.
- Litematica printer placement pipeline with candidate filtering, retries,
  temporary skips, and verification.
- Elytra flight state machine with sustained input-based control.
- Velocity packet-side knockback scaling/cancellation.
- Release documentation set.

### Improved

- Title screen logo and buttons now share a layout model instead of overlapping.
- Skin state is saved and restored more consistently.
- Failed username or PNG loads no longer overwrite the last valid skin preview.
- Official skin preview requests are cached by username.
- Middle-click notifications now include useful player context.
- Notification rendering uses a bounded queue instead of a single transient toast.
- README has been reduced to a clear entry point and links to detailed docs.

### Fixed

- Vanilla yellow splash text is disabled for the custom title menu.
- Chat desync risk from rebuilding signed chat messages is avoided.
- Clickable text components are preserved when adding link/name helpers to
  unsigned messages.
- Local skin preview no longer resets to fallback after a temporary file or
  network error.

### Changed

- `server-diagnostics`, `server-auto-setup`, `eclipse-anti-crash`, and
  `eclipse-custom-packets` are not registered in the clean release runtime.
  They remain in source form as internal/not-for-release code.
- Official cape management is intentionally limited to the current authenticated
  Minecraft account. The addon does not spoof accounts.

### Known Issues

- Movement modules remain server-sensitive and should be treated as beta tools.
- `litematica-printer` depends on schematic shape, inventory, block support,
  server timing, and Litematica compatibility.
- Official skin/cape updates may take time to propagate through Mojang services.
- Multi-account support does not switch the Minecraft session inside a running
  client. Use the launcher to start with another account.
