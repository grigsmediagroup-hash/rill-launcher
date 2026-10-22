# Niagara Launcher: interaction study and personal launcher feasibility

Research date: 3 October 2026. Target: a lightweight personal launcher for Android 6, with Android 5 as the minimum.

This is a study of publicly documented behavior, not Niagara's proprietary implementation. Official help articles, the product site, release notes, and Android platform documentation were consulted. Public documentation does not expose Niagara's animation constants, rendering pipeline, private algorithms, or every device-specific behavior. “All online documentation” is not a finite, verifiable corpus; this report covers the major documented systems and records implementation gaps. Some articles are older than current releases and some describe proposals rather than shipped features. These distinctions matter.

Agent Reach's doctor was run. Its Exa backend was unavailable, social backends lacked authentication/extensions, and its Jina web reader failed through the configured proxy. Built-in web search and page reading supplied the source material instead. Its update check also failed; no tooling or accounts were changed.

## 1. The design system

Niagara explicitly evaluates new features against simplicity, impact, universality, and independence. Optional features should not complicate the experience when unused. Its argument is practical: settings multiply the combinations that must remain polished. The implication for this project is to make the primary route to an app obvious, then place secondary capabilities behind contextual interactions. [Design principles](https://help.niagaralauncher.app/article/8-niagaras-design-principles).

The vertical list is a core architectural choice. Niagara declines a grid mode and alternative alignment modes because maintaining those would multiply feature and layout work. It reserves horizontal swipes for pop-ups/notification dismissal and vertical swipes for navigation; it avoids double tapping app entries because waiting for a possible second tap delays launching. App spacing also protects usable touch targets. For our launcher, reliable single taps take precedence over gesture abundance. [Customization decisions](https://help.niagaralauncher.app/article/77-missing-customization-features).

Typography has greater weight than in an icon grid because names are primary navigation targets. Niagara accepts custom TTF and OTF fonts through its theme settings. Our first implementation uses Android's installed sans-serif faces, saving storage and avoiding font-loading complexity; text size is adjustable. [Custom fonts](https://help.niagaralauncher.app/article/79-custom-fonts).

Whitespace has a purpose: keeping controls within reach. Niagara lets the user move the clock and connected content together, rather than independently dragging every item. This preserves relationships between the clock, widgets, and favorites. Our home vertical offset provides a simpler version of that control. [Move clock](https://help.niagaralauncher.app/article/156-move-clock).

## 2. Favorites and the all-apps list

The home list holds selected apps; the alphabet provides access to everything else. Long pressing edits favorites. Niagara can also show frequently used app suggestions beneath favorites. That adaptive behavior reduces the need to curate a static home list but requires usage policy and persistence. Our preview uses explicit favorites and does not request usage access or infer habits. [Tips and tricks](https://help.niagaralauncher.app/article/34-tips-and-tricks).

App identity must be a component, not merely a package or displayed name: a package can expose multiple launchable activities, and names can collide. Rill stores flattened component names, preserves a favorite's order, and handles apps disappearing without attempting to launch a stale entry. It queries launchable activities on a worker thread and refreshes after package changes while visible. It currently covers the personal profile only; work profiles would need profile-aware LauncherApps queries and callbacks. [LauncherApps API](https://developer.android.com/reference/android/content/pm/LauncherApps).

Renaming changes the presentation, not the underlying app. Hidden apps remain installed. In Rill, hidden entries are removed from both the visible list and search, and can be restored in settings. There is no password protection. Treat hiding as organization, not access control.

## 3. Alphabet navigation and handedness

Niagara's signature alphabet is a direct spatial shortcut into a long list. Its product page calls this the wave alphabet. Conceptually the rail has two duties: continuously translate thumb position into a letter, and show which letter is selected. The wave makes the currently touched region easier to read; it need not animate an entire list. Public material does not disclose its exact interpolation or spring physics. [Product overview](https://niagaralauncher.com/).

Niagara also accepts scrolling from the left edge, without mirroring the entire app list. The help article explains why the list stays aligned consistently and why the invisible left alphabet has no disable switch. Rill's preview currently exposes only the visible right rail; a left edge route is a clear parity gap. [Left hand behavior](https://help.niagaralauncher.app/article/39-left-hand-mode).

Alphabet grouping is not just taking the first ASCII character. Niagara documents transliteration and locale-dependent letters, with extra Scandinavian and Spanish letters chosen from device locales. Rill strips Latin accents for search and offers A–Z plus a # bucket for other initials; it does not implement Niagara's complete transliteration or locale alphabet. That is adequate for an English-first personal setup but needs improvement for other scripts. [Alphabet localization](https://help.niagaralauncher.app/article/155-alphabet-localization).

Rill draws a single narrow custom view, changes letter size/position near the thumb, and immediately positions a recycled ListView at the selected group. It avoids launching a long scroll animation for every move event. Missing letters jump to the next available Latin letter; # goes to the first non-Latin initial. The star returns to favorites.

## 4. Gesture map

| Interaction | Documented Niagara behavior | Rill preview |
|---|---|---|
| Tap app name/icon | Launch app | Immediate launch intent |
| Long press app | Edit favorites and app options | Favorite, reorder, rename, hide, info, pop-up |
| Right swipe on app | Open pop-up with notifications/shortcuts | Notification cards, related apps, widgets, media controls |
| Left swipe on app | Dismiss notifications | Cancels clearable notifications for that app |
| Drag alphabet | Navigate app groups | Continuous letter selection with wave feedback |
| Left screen edge | Invisible alphabet navigation | Not implemented |
| Long upward swipe | Search; distinguished from ordinary scrolling | Search from bottom controls or clock region without widgets |
| Downward swipe | Notification tray | Same restricted origin regions; platform-dependent shade request |
| Swipe widget stack | Switch widget | Previous/next controls; preserves widget's own touch gestures |
| Tap clock/date | Open clock/calendar | Android clock/calendar intents |
| Double tap alphabet | Quick lock when supported | Not implemented |
| Home | Return home; some configurations allow search | Favorites |
| Back in app list/search | Navigation | Favorites |

Niagara specifically advises fast release swipes for ordinary list scrolling and longer swipes for search. This means gesture recognition must distinguish navigation from scrolling, rather than assigning every vertical movement to a global command. Our first version uses distinct origin regions, reducing ambiguity on old hardware at the cost of “swipe anywhere” parity. [Search gesture](https://help.niagaralauncher.app/article/90-mastering-the-search-gesture), [notification gesture](https://help.niagaralauncher.app/article/98-notification-gesture).

Niagara's quick lock requires Android 9+ and an accessibility service, with a separate rooted workaround for older phones. It only uses alphabet/edge regions so app taps remain immediate. Rill does not request accessibility or device administrator access for this feature. [Double tap to sleep](https://help.niagaralauncher.app/article/19-double-tap-to-sleep).

## 5. Pop-ups and folders

Niagara pop-ups are contextual containers that can hold apps, shortcuts, notifications, and widgets. Basic folders and basic app pop-ups are available in the free tier; editable app pop-ups, widgets in pop-ups, and more placements are documented as Pro capabilities. Niagara declines nested pop-ups because another level raises navigation and gesture ambiguity. These are product decisions rather than Android constraints. [Pop-ups](https://help.niagaralauncher.app/article/115-pop-ups).

Rill gives each app its own bottom sheet. The app stays the primary tap action; related apps can be edited independently, so a messaging app can expose another messenger without adding a home row. Notification cards and supported media controls appear here. A widget stack can belong to each app independently of the home stack. Standalone named folder rows and pinned shortcuts are not implemented in this preview.

Niagara's configurable button is another entry point into an app, folder, widget, or action. Pro adds a secondary swipe-up action. The limited gesture vocabulary is deliberate. Rill instead has explicit all-apps, search, and settings controls in the bottom bar, with 48dp minimum button heights. [Niagara button](https://help.niagaralauncher.app/article/116-niagara-button).

## 6. Widget flexibility

Niagara offers home widgets, replacing the clock, widget stacks, and stacks inside app/folder pop-ups. The help page documents up to four widgets in a home stack and horizontal swipes to switch, with reorder controls. This keeps multiple information sources within one footprint. [Adding multiple widgets](https://help.niagaralauncher.app/article/41-adding-multiple-widgets).

Its built-in clock styles integrate time, date, battery, weather, and upcoming events. Alternatively the clock can be replaced by a third-party widget such as a KWGT clock. This is real Android widget hosting, not a screenshot of another app. Rill retains its clock and supports third-party widgets below it and within pop-ups; clock replacement is not yet implemented. [Custom clock](https://help.niagaralauncher.app/article/23-custom-clock).

A launcher hosting widgets must allocate IDs, obtain user-authorized binding, run configuration activities when required, persist identity/size, deliver size options, listen while visible, and delete IDs when removed. These contracts are more important than simply placing a rectangular view on the home screen. Rill implements these flows and cleans up canceled allocations; provider removal and unusual configurations still need device testing. [Android widget hosting guide](https://developer.android.com/develop/ui/views/appwidgets/host), [AppWidgetHost API](https://developer.android.com/reference/android/appwidget/AppWidgetHost).

Rill uses explicit stack navigation to avoid stealing gestures from interactive widgets. Widget count is not artificially limited, but each extra widget instance can incur provider costs. Only one instance per stack is displayed. Resize height and move-earlier controls are included. A provider's own actions, data, and permissions remain its responsibility.

KWGT and KLWP are distinct integration surfaces: widgets versus live wallpaper. Niagara documents visibility and scroll-related broadcasts for Kustom layouts. Those broadcasts are not a generic Android widget requirement, and Rill does not emulate them. Ordinary widgets and the device wallpaper option remain available. [Kustom integration](https://help.niagaralauncher.app/article/59-klwp-integration).

## 7. Notifications and media

Notification previews turn the app list into a lightweight triage surface. Rill uses Android's notification listener with explicit access granted in system settings. It keeps notifications only in process memory; no content is written to preferences, logs, or a server. Access can be revoked without removing the launcher. Android may withhold sensitive content, and app-specific actions can expire. [NotificationListenerService API](https://developer.android.com/reference/android/service/notification/NotificationListenerService).

Direct replies require a compatible notification action and standard RemoteInput behavior. Rill enables reply UI on API 24+, while Android 6 offers notification opening and ordinary actions. This avoids presenting a standard reply flow the target system cannot reliably support. Replies are an explicit user action; the launcher never automatically responds. [RemoteInput API](https://developer.android.com/reference/android/app/RemoteInput).

Niagara's media widget appears contextually during playback. It relies on proper media sessions and transport controls, and its help page records app/OEM differences and why media content pushes favorites downward into reach. Rill presently places supported media-session controls inside the corresponding app pop-up instead of adding an always-visible home player. [Integrated media player](https://help.niagaralauncher.app/article/11-media-widget).

Notification summaries are a separate feature. Niagara documents Android 8+ support, selected apps, silent-notification inclusion, and delivery roughly six hours after the previous batch was cleared. Rill does not intercept or batch notifications; previews simply mirror notifications Android delivers. The older “notification section” page explicitly labels its proposal unimplemented, so it should not be mistaken for a shipped universal section. [Notification summary](https://help.niagaralauncher.app/article/122-notification-summary), [notification section proposal](https://help.niagaralauncher.app/article/35-notification-section).

## 8. Calendar and weather

Niagara's agenda consumes Android Calendar Provider data; apps that don't publish to that provider cannot participate in the same way. Work calendar visibility also depends on policy. Google tasks/reminders are not equivalent to ordinary provider events. Rill currently opens the calendar and can host a calendar widget; it has no built-in event query or calendar permission. [Calendar widget](https://help.niagaralauncher.app/article/76-calendar-faq).

Niagara's weather provides current conditions, hourly information, and upcoming daily forecasts using OpenWeather. It documents periodic refreshes and fixed/current location modes. A comparable built-in feature would need a data service, network access, cache, unit/location controls, and error states. Our offline base delegates weather to an installed widget app, avoiding a provider key and recurring network work in the launcher itself. [Weather widget](https://help.niagaralauncher.app/article/105-weather-widget).

## 9. Colors and responsive layouts

Niagara documents wallpaper-derived colors and Material You palettes backported to its supported Android versions. It chooses palette colors that harmonize with dominant wallpaper colors rather than blindly adopting an OEM palette. Live wallpaper changes and Android's deferred palette updates can complicate this. Rill has deep-water, azure, slate, and plum gradients plus device wallpaper with a dark contrast overlay; automatic color extraction is a future feature. [Color system](https://help.niagaralauncher.app/article/133-color-system).

On wide displays Niagara can split its app list and display two widgets side by side. It adjusts sizing and edge placement for different holding distances and supports continuity when folding. Rill's current phone-first, single-column layout is intentionally narrower in scope; rotation and keyboard accessibility need explicit validation. [Foldable support](https://help.niagaralauncher.app/article/100-foldable-device-support).

## 10. Animation: what we can and cannot reproduce

The personal build also adds a native secondary time-zone clock and a battery percentage/charging indicator beside the date. TextClock handles time changes without our own polling loop, and battery state comes from Android's battery-change broadcast. The secondary zone is searchable by city/region and can be turned off. These are small, useful additions rather than a separate dashboard. [TextClock](https://developer.android.com/reference/android/widget/TextClock), [BatteryManager](https://developer.android.com/reference/android/os/BatteryManager).

The wallpaper option displays the phone's system wallpaper with a contrast overlay. Its chooser delegates selection/cropping to installed wallpaper handlers, so gallery images and live wallpapers depend on those handlers being present. It does not import files into launcher storage, request broad photo access, or force a wallpaper change at install time. [WallpaperManager](https://developer.android.com/reference/android/app/WallpaperManager).

Separate three layers: feedback inside the launcher, ordinary transitions between activities, and system recents/gesture navigation. The first is under our control; the others depend on Android and the device's system launcher. Niagara's help documents that third-party launchers cannot provide some elaborate first-party app-opening animations. Its gesture troubleshooting also identifies OEM restrictions and system bugs. Android 6's three-button navigation avoids many modern gesture-recents integration issues, but still does not give a personal launcher ownership of system recents. [App launch animations](https://help.niagaralauncher.app/article/81-app-launch-animations), [gesture navigation limitations](https://help.niagaralauncher.app/article/7-gesture-navigation).

There is no evidence in the reviewed documentation for Niagara's precise easing curves, animation durations, or proprietary wave mathematics. Do not describe guessed constants as facts. Rill's choices are original: direct rail feedback, normal platform app transitions, native touch ripples, and a 160ms translation/opacity entrance for pop-up content. Reduce motion disables the custom sheet entrance. There is no continuous render loop, real-time blur, wallpaper parallax, or simulated full-screen app animation.

At 60Hz a frame has approximately 16.7ms. App enumeration and icon fetching should not occupy the UI thread. Reusing list rows matters more than adding an ornamental animation framework. Performance must be measured on the Xperia, with representative app counts, widgets, and notifications; emulator smoothness cannot prove phone smoothness.

## 11. Feasibility and implementation choices

Yes: an independently written list-first launcher is feasible on API 21/23. The essential Android APIs predate or exist on Android 5. Rill uses Java and Android framework views, compiled to Java 8 bytecode and DEX with min API 21. It has no third-party runtime libraries, no Internet permission, no ads, no subscriptions, and no analytics. It uses its own name and visual assets rather than Niagara's artwork or code.

Target API 28 is a deliberate personal-preview compromise for old-device and framework compatibility. It is not an Android version ceiling, but later Android releases may show old-app warnings and impose additional restrictions. This preview is not Play Store submission ready. Raising the target requires testing newer home roles, receiver flags, visibility rules, and permission behavior.

The build uses installed SDK tools directly rather than requiring Gradle downloads. The signing certificate is local to the project; its key must be preserved for future in-place APK upgrades. The script uses a documented local personal-key password, not a production signing secret. Do not commit or distribute the key. Android 5 installation needs v1 signatures; the produced APK is verified with v1 and v2 signatures enabled. The APK's minimum is checked from its compiled manifest, not inferred from source alone.

## 12. Scope and validation gates

Implemented in the preview: home registration, personal-profile launchable list, favorites and ordering, search, hide/rename, alphabet feedback, app pop-ups, related apps, widget bindings/configuration, persistent widget placement and size, optional notification previews/actions, media controls in pop-ups, themes, text sizing, compact rows, and motion reduction.

Not equivalent to full Niagara: complete locale alphabets/transliteration, left edge alphabet, clock replacement, swipeable widget carousel, independent folder rows, icon packs, arbitrary font import, automatic wallpaper palette, usage suggestions, built-in weather/agenda, notification batching, quick lock, tablet columns, Kustom broadcast integration, custom button action, backup export/import, system recents animation integration, or work-profile management.

Niagara itself notes that backup cannot fully transport all third-party widget configurations and app shortcuts. Provider data is not launcher-owned; exporting a widget ID is not a portable backup. That must inform any later Rill backup design. [Backup limitations](https://help.niagaralauncher.app/article/37-backup-settings).

Before phone installation: compile, verify signatures and min SDK, inspect actual emulator UI, exercise navigation and widget cancellation, and show the user the result. Before calling it daily-driver ready: test Android 6 cold start, home role, app launch/return, many apps, icon cache, notification access/revocation, package changes, widget configuration/cancellation/removal/resize/reboot, orientation, keyboard, accessibility, and memory/frame behavior. Android 5 runtime testing requires an API 21 system image or device; declaring minSdk alone does not establish that all paths have been exercised there.

The user requested a preview before deployment. No installation on the connected physical phone is authorized until that preview has been reviewed. Build artifacts can be delivered now, but “no bugs” and guaranteed fluidity cannot be honestly certified from compilation alone.
