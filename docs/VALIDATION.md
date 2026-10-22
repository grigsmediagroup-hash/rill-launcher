# Validation and known limits

Prepared 23 October 2026. Package dev.personal.rill, version 0.1.0/code 1, min API 21, target API 28, compiled with API 34.

## Current build

Rebuilt from the repository Java/resources with JDK 21 and Android build tools 36.0.0. Compilation, D8 min-api 21, zip alignment and v1/v2/v3 signature verification passed. APK: 45,525 bytes (44.46 KiB), above 45 decimal KB. SHA-256: `0A2B9F645B4800A6D6FACD1ECA286732C05321C5D7E38980BF9E51AEEDA9BE42`. Other toolchains/keys can change size and hash.

## Android 6 device

Updates were successfully installed through ADB on an Xperia T2 Ultra / Android 6. A startup crash was traced to ListView header layout parameters; explicit AbsListView.LayoutParams with the header/footer view type repaired initial attachment. The user supplied a successful startup report after that repair. Later installations succeeded, but installation success is not a complete runtime regression test.

## Earlier emulator evidence

An Android 16 emulator supplied screenshots and earlier integration results. A 13-check run covered startup/favorites, all apps, search, swipe cancellation, long press, settings, second clock, three gradient themes, widget-picker cancellation and absence of fatal app exceptions in that run. Six additional checks covered widget binding, restart restoration, stack cycling, cancellation, wallpaper mode and the chooser.

Later targeted checks covered wallpaper rendering, search, pop-ups and home return. Subsequent launches and visual checks covered compact search, text shadows, dimming and status-bar styling. A full rerun encountered a UiAutomation connection timeout, so it did not establish a pass. The latest AMOLED/transparent/grayscale/navigation-gradient additions have build/signature and installation evidence but no complete automated runtime pass. Screenshots show several stages and are labeled accordingly.

## Performance

Recycled framework views, worker-thread queries/loading, a 64-entry icon cache and broadcast-driven battery updates aim to reduce overhead. APK size is not runtime memory. Software emulator timings were uneven and cannot establish phone fluidity. No zero-bug, guaranteed 60fps or every-phone claim is made.

## Still to test

- Android 5/API 21 runtime behavior.
- Final-build startup, rotation, Home/Back, reboot, unlock and app return on Android 6.
- Final system bar rendering on Xperia and in landscape orientation.
- Bright/live wallpapers, grayscale recognition and widgets across providers.
- Notification access/revocation, sensitive previews, expired actions, newer-API reply and media sessions.
- Widget configuration, provider removal, multiple instances and reboot restoration.
- Newer Android installation policy, work profiles and broader OEM coverage.

External icon packs, built-in cloud weather/agenda, standalone folders, backup import/export, full transliteration and system recents transition parity are outside this preview's scope.
