# Contributing

Small, focused fixes and improvements are welcome. Start with the [build guide](docs/BUILDING.md), [architecture](docs/ARCHITECTURE.md) and [validation limits](docs/VALIDATION.md).

Keep the launcher offline, lightweight and usable on Android 5/API 21. Guard newer APIs explicitly. Test Android 6/API 23 when possible, especially ListView headers/footers, transparent windows and widget flows. Include what you changed, device/API tested, build output and before/after screenshots for visual changes. Do not promise compatibility or frame rates that have not been measured.

Never commit signing keys, device serials, personal wallpapers, raw device logs, account data or notification screenshots containing private content. Use your own signing key for local builds. Changes are contributed under the MIT license.

Report crashes with Android version, phone model, steps, expected behavior and a redacted stack trace. An issue template is provided.
