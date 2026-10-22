# Build and install

## Requirements

- Windows with PowerShell and a JDK. The tested JDK is 21.
- Android SDK platform `android-34`.
- SDK build tools `36.0.0`, including aapt, D8, zipalign and apksigner.
- No Gradle, Maven dependencies or network access during the build.

```powershell
.\build.ps1 -Sdk 'C:\path\to\Android\Sdk' -Jdk 'C:\path\to\jdk-21'
```

The defaults are the current user's LocalAppData Android SDK and the original development JDK path. Pass explicit paths on other machines. The script compiles framework Java/resources, creates DEX with minimum API 21, aligns the package and verifies the signed APK. Output: `dist/Rill-0.1.0.apk`.

The app declares Android 5/API 21 minimum and targets API 28. Targeting an older API is deliberate for this personal preview; this is not a current Play Store publishing configuration.

## Signing and updates

The script generates `signing/personal.jks` if absent, using alias `rill` and the development password `rill-personal`. Keep the generated private key private. The repository excludes it. Each contributor gets their own key; independently built APKs cannot update one another unless signed with the same key.

The committed APK is signed with the project's existing private development key. That private key is not included. Its password is a development default, not a production secret. Preserve your local key for your own updates, and increment versionCode/versionName in the manifest for future releases. Public binaries and contributor builds may have different byte sizes and hashes.

## Install

Download the APK to your device and permit installation from that source when prompted, or use ADB:

```powershell
adb devices -l
adb -s DEVICE_SERIAL install -r dist/Rill-0.1.0.apk
```

Press Home and choose Rill, or select it under Android Settings → Home. To switch back, choose your previous launcher there. Installing an update with `-r` preserves settings when the package/signing key match. Uninstalling clears launcher configuration. Rill does not change your default launcher automatically.

## Troubleshooting

- **App not installed:** check API level, available space and whether an existing Rill installation uses another signing key.
- **Widget unavailable:** install/enable its provider and finish the Android binding/configuration prompts.
- **Colored icons wanted:** disable Black-and-white app icons in Rill settings.
- **Wallpaper hidden:** select Transparent wallpaper or Device wallpaper (dimmed), then adjust dimming.
- **Android 6 startup:** ListView headers and footers need `AbsListView.LayoutParams`. Preserve the explicit header/footer view type when changing this code.
- **Notification actions absent:** grant optional listener access and check that the source notification exposes that action.

## Emulator scripts

The Python integration scripts require a development emulator, Python, ADB and, for gallery rendering, Pillow. They use local emulator address `127.0.0.1:5561` with private ADB port `5041`; update those paths/ports for your environment. Installation scripts assert `ro.kernel.qemu=1`. They change emulator settings and widgets. Do not use them as general phone deployment scripts.
