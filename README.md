<p align="center"><img src="docs/branding/showcase-hero.png" width="100%" alt="Rill product showcase: colorful app icons over your wallpaper. A little launcher. A lot more room."></p>

<p align="center"><strong>Niagara Launcher inspired. Re-engineered in the open. Under 45 KiB.</strong></p>
<p align="center">
<a href="https://github.com/grigsmediagroup-hash/rill-launcher/releases/download/v0.1.0/Rill-0.1.0.apk"><img alt="Download APK: 44.46 KiB" src="https://img.shields.io/badge/Download_APK-44.46_KiB-101C23?style=for-the-badge&amp;logo=android&amp;logoColor=A7E3CB&amp;labelColor=203C3B"></a>
</p>
<p align="center"><a href="docs/SCREENSHOTS.md">Take the tour</a> · <a href="docs/FEATURES.md">Explore the features</a> · <a href="docs/BUILDING.md">Build it yourself</a></p>

Rill gives your wallpaper room to breathe and keeps your favorite apps close. A small, native Android home screen with one-handed alphabet navigation, real widgets and just enough customization to feel like yours. **No ads. No trackers. No account. No Internet permission.**

## An app list that gets out of your way

Slide to a letter. Tap an app. Or type a few characters and put matching app names first. Favorites stay on home; the rest are a gesture away.

<p align="center"><img src="docs/branding/showcase-navigation.png" width="100%" alt="Real Rill app drawer and prefix-first search screens with colored icons"></p>

## Your wallpaper. Your kind of quiet.

Keep the original icon colors, go monochrome, or settle into pure AMOLED black. Choose transparent wallpaper, adjustable dimming or one of four gradients. The clock, battery, second time zone and text spacing fit around your day.

<p align="center"><img src="docs/branding/showcase-customization.png" width="100%" alt="Rill AMOLED black, colorful wallpaper home and customization settings"></p>

## Useful when you need it

- **A glance, not a dashboard.** Clock, date, battery percentage and an optional second time zone.
- **Real Android widgets.** Add them to home or an app pop-up, with stacks that keep widget gestures intact.
- **Your favorites, your order.** Rename, hide and reorder apps with a long press.
- **Optional notification access.** Local previews, supported actions, dismissal and compatible media controls.
- **Small by design.** Framework Java, recycled rows, off-thread loading and a bounded icon cache.

[See the full product tour](docs/SCREENSHOTS.md) · [Every setting, with screenshots](docs/SETTINGS-GALLERY.md) · [Gesture and feature guide](docs/FEATURES.md)

## Make room on your phone

1. [Download the preview APK](https://github.com/grigsmediagroup-hash/rill-launcher/releases/download/v0.1.0/Rill-0.1.0.apk) and install it.
2. Press Home and choose **Rill**, or select it in Android Settings → Home.
3. Open **⋯** to choose your wallpaper, themes, widgets and icon style.

Prefer your previous launcher? Select it again in Android's Home settings. Rill never makes itself the default automatically. [Installation help](docs/BUILDING.md).

The images are designed presentation panels around **real screenshots of the released APK**, captured on an Android 16 emulator. The hero shows the optional original-color icon setting; grayscale is the default. Device frames and surrounding copy are presentation graphics. No phone performance claim is implied.

<details>
<summary><strong>Compatibility, exact size and preview status</strong></summary>

- **APK:** 45,525 bytes / 44.46 KiB, not under 45,000 bytes. [SHA-256](dist/Rill-0.1.0.sha256.txt).
- **Declared minimum:** Android 5 / API 21. Primarily developed for Android 6 / API 23.
- Installed on an Xperia T2 Ultra running Android 6. Full Android 5 runtime verification and wider device testing remain pending.
- Native widgets, notifications and system bars depend on Android version and provider/OEM behavior.
- This is a personal passion project released as a preview; universal compatibility, zero bugs and guaranteed 60fps are not claimed.

[Validation history and known limits](docs/VALIDATION.md).

</details>

<details>
<summary><strong>Source, build and documentation</strong></summary>

Windows PowerShell, JDK 21, Android platform 34 and build tools 36.0.0. Run `build.ps1` with your `-Sdk` and `-Jdk` paths. No Gradle or third-party app dependencies. Each source build creates its own local signing key; the project's private key is excluded.

[Build instructions](docs/BUILDING.md) · [Architecture](docs/ARCHITECTURE.md) · [Design research](docs/NIAGARA-STUDY.md) · [Privacy](PRIVACY.md) · [Changelog](CHANGELOG.md) · [Contributing](CONTRIBUTING.md) · [Sharing](docs/DISCOVERABILITY.md)

</details>

## Independent, and open

Rill is an independent **Niagara Launcher inspired** project, not affiliated with Niagara Launcher. It contains none of Niagara's source code or assets. If you found us searching for “Niagra launcher,” the inspiration is Niagara; this implementation is Rill.

Original code, documentation and branding are [MIT licensed](LICENSE). Third-party app icons, screenshot wallpaper artwork and widget content retain their owners' rights. [Report a bug](https://github.com/grigsmediagroup-hash/rill-launcher/issues/new?template=bug_report.md) or help make a small launcher better.
