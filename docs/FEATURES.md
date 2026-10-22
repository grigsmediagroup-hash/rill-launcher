# Using Rill

## Home and navigation

Tap an app to launch it. Long press to rename, hide, add/remove favorites or reorder them. Favorites share a scrolling list with the clock and home widgets. Back or Home returns to favorites. All apps opens the complete visible list.

Slide the right alphabet rail to jump to a letter. Nearby letters bend inward and a touch-following marker identifies the selection. The selected section is aligned to the top, including near the end of the list. The star returns home. Latin accents are folded for matching; full transliteration is not implemented.

Tap the magnifier or swipe up from the bottom controls or an empty, widget-free clock area to search. Prefix matches appear before substring matches. The keyboard's Go action launches the first result. Swipe down from those empty areas to request the notification shade; some OEMs restrict programmatic expansion.

## Appearance

Open the bottom ellipsis for settings:

| Option | Behavior |
| --- | --- |
| Deep water, Azure, Slate, Plum | Built-in gradients |
| AMOLED black | Pure black launcher background and system bars |
| Device wallpaper (dimmed) | System wallpaper with adjustable 0–80% vertical dimming |
| Transparent wallpaper | System wallpaper without the main dimming overlay |
| Black-and-white app icons | Grayscale original icons for every app; enabled by default |
| Text size / home vertical position | Adjust app label size and space above the clock |
| Compact app rows / show app icons | Adjust list density and icon visibility |
| Reduce motion | Disable the custom sheet entrance transition |

Text has a dark shadow for wallpaper contrast. Transparent mode still uses a small bottom navigation gradient for button visibility. The background extends behind system bars; insets keep controls clear. OEMs may impose their own bar rendering. Rill styles bars only while its activity is displayed.

Grayscale icons provide universal coverage, including future installs. They are not a third-party icon pack or a set of individually redrawn outline icons. External icon-pack loading is not implemented.

## Clock, battery and widgets

The main clock follows the system's 12/24-hour preference. Tap it for alarms, and tap the date to open a calendar. Enable an optional second clock and search Android timezone identifiers by city/region. Battery percentage and charging state are available without a widget app.

Add home widgets from settings. Long press an app and open its pop-up for related apps, widgets, notifications and media controls. Widget providers own their content, appearance, account requirements and configuration. Android asks for binding permission when required.

Home and per-app locations support widget stacks. Use Previous/Next to cycle without taking over the widget's gestures. Manage widgets to change height, move earlier or remove. Widgets survive process restart; provider removal, configuration and reboot paths still need wider testing.

## Notifications and gestures

Notification access is optional and granted in Android settings. Swipe right on an app to open its pop-up; swipe left to dismiss its clearable notifications. App rows can show a preview. Notification text is held in memory, with no network transmission or persistent notification history.

Android 6 supports ordinary actions and dismissal. Standard direct reply requires Android 7+, while app shortcuts require Android 7.1+ and the appropriate launcher role. Compatible media sessions can expose playback controls. These integrations have not all received comprehensive device testing.

## Scope

Rill is an independent implementation inspired by public Niagara Launcher design documentation. It does not contain Niagara code or assets. It does not implement system recents transitions, weather services, built-in agenda, work-profile handling, standalone folders, backup import/export or complete Niagara feature parity.
