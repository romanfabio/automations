# Changelog

All notable changes are listed here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and
the project uses [Semantic Versioning](https://semver.org/).

## [1.0.1] - 2026-11-06

### Added
- **About** screen (button at the top right of the main screen) with the app version, the MIT license, the list of
  third-party libraries and the full Apache License 2.0 text, so the license notices travel inside the APK. The texts are
  the repository's own `LICENSE`, `THIRD_PARTY_LICENSES.md` and `licenses/APACHE-2.0.txt`, bundled at build time.

### Changed
- `versionCode` is now 2 (an upgrade needs a higher code). The signing key is unchanged, so 1.0.0 upgrades in place and
  keeps its settings.

## [1.0.0] - 2026-11-06

First public release.

### Added
- List of predefined automations, each with an enable switch, a **Run now** button, a detail screen and a run history.
- **Airplane mode on** automation: runs at a chosen time on selected weekdays (default 23:00, every day, disabled until
  enabled) and does nothing if airplane mode is already on. Uses the default-digital-assistant role, because apps
  cannot switch airplane mode on Android 15 otherwise.
- Exact alarm-clock scheduling that fires in Doze and Battery Saver, re-armed after reboot (also before the first
  unlock), app update, clock change and time-zone change. Missed runs are skipped.
- Notification when a run fails.
- First-run **Setup** checklist: assistant role, notifications, unrestricted battery use, and realme / OPPO / OnePlus
  background settings.
- Original launcher icon; backup and device transfer disabled for app data.

### Requirements
- Android 13 (API 33) or newer. Tested on Android 15 (realme RMX3890) only.

### Known limitations
- Deep Doze, a run before the first unlock after a reboot, and a live time-zone change are implemented but not verified
  on a device (see `docs/TESTING.md`).
- Not suitable for Google Play as is (battery-optimization and exact-alarm policies).

[1.0.1]: https://github.com/romanfabio/automations/releases/tag/v1.0.1
[1.0.0]: https://github.com/romanfabio/automations/releases/tag/v1.0.0
