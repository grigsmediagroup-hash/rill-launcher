# Rill

**Niagara Launcher inspired. Re-engineered in the open. Under 45 KiB. Built for Android 5+.**

A tiny, open-source Android launcher with a calm vertical app list, one-handed alphabet navigation, widgets and your own wallpaper. Native Java. No ads, trackers, accounts or Internet permission.

Rill is an independent project inspired by Niagara Launcher's publicly documented interaction ideas. It is not affiliated with Niagara Launcher, contains no Niagara source code or assets, and does not claim feature parity.

[Download APK](dist/Rill-0.1.0.apk) · [Screenshots](docs/SCREENSHOTS.md) · [Features](docs/FEATURES.md) · [Build](docs/BUILDING.md) · [Design study](docs/NIAGARA-STUDY.md)

## Small by design

The included signed APK is **45,525 bytes: 44.46 KiB / 45.525 decimal KB**. “Under 45 KiB” refers to this APK, not runtime memory, installed storage or all future builds. It is not under 45,000 bytes. See the [checksum](dist/Rill-0.1.0.sha256.txt).

![Rill home with wallpaper, dual clock and app list](docs/images/home.png)

Actual emulator capture from an earlier build. The final navigation gradient and grayscale icons are not pictured; see the [gallery notes](docs/SCREENSHOTS.md).

## Features

- Favorites, all apps, rename/hide/reorder and prefix-first search.
- Finger-following alphabet marker, inward rail curve and selected-letter top alignment.
- AMOLED black, transparent wallpaper, adjustable dimming and four gradient themes.
- Universal grayscale app icons, with original colors available in settings.
- Text shadows, compact search control and bottom navigation gradient.
- Native clock, date, optional second timezone and battery percentage.
- Real Android widgets and stacks on home and in app pop-ups.
- Optional local notification previews/actions, swipe dismissal and compatible media controls.
- Framework-only views, bounded icon caching and off-thread app/icon loading.

Grayscale covers all installed apps; it is not a custom outline icon pack. Widgets retain their provider's appearance and behavior. [Complete gesture and settings guide](docs/FEATURES.md).

## Try it

1. Download the APK and install it. Android may ask you to allow installation from that source.
2. Press Home and choose Rill, or select it in Android Settings → Home.
3. Open the bottom ellipsis for themes, widgets, wallpaper, dual clock and icons.
4. Tap apps to launch, long press to edit, slide the alphabet to navigate. Tap the magnifier or swipe up from the bottom controls to search.

Switch back using Android's Home settings. Rill does not set itself as default automatically. [Installation and troubleshooting](docs/BUILDING.md).

## Compatibility

| Item | Status |
| --- | --- |
| Declared minimum | Android 5 / API 21 |
| Primary intended target | Android 6 / API 23 |
| Physical installation | Xperia T2 Ultra running Android 6 |
| Emulator checks | Earlier UI/widget flows on Android 16; later polish partially checked |
| Full Android 5 runtime verification | Pending |
| Guaranteed every-phone compatibility or 60fps | Not claimed |

This passion project is a preview. OEM behavior and newer Android policies may affect installation, widgets and system bars. [Validation history and limits](docs/VALIDATION.md).

## Build

Requires Windows PowerShell, JDK 21, Android platform 34 and build tools 36.0.0. Run `build.ps1` with explicit `-Sdk` and `-Jdk` paths. No Gradle or external app dependencies. The build creates your own local signing key; the project's private key is never published. [Build instructions](docs/BUILDING.md).

## Documentation

- [Features and gestures](docs/FEATURES.md)
- [Screenshots and historical recording](docs/SCREENSHOTS.md)
- [Build and installation](docs/BUILDING.md)
- [Architecture](docs/ARCHITECTURE.md)
- [Validation and limitations](docs/VALIDATION.md)
- [In-depth Niagara design study](docs/NIAGARA-STUDY.md)
- [Privacy](PRIVACY.md), [changelog](CHANGELOG.md), [contributing](CONTRIBUTING.md)

## License and credits

Original Rill code and documentation are [MIT licensed](LICENSE). Referenced products, third-party app icons, screenshot wallpaper artwork and widget UI retain their owners' rights. The study links to third-party documentation and does not relicense it. Personal wallpapers, signing keys and device logs are excluded.

Discovery keywords: Niagara Launcher inspired, Niagra launcher alternative, open-source Android launcher, lightweight Android launcher, under 45 KiB launcher, Android 5 launcher, Android 6 launcher, minimalist launcher, AMOLED black, transparent wallpaper, monochrome icons.
