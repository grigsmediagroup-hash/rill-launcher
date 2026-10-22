# Architecture

Rill uses Android framework APIs directly: native Java, one home Activity, a notification-listener Service and no dependency runtime.

| Component | Responsibility |
| --- | --- |
| `HomeActivity.java` | App discovery, ListView rows, favorites, search, alphabet Canvas, themes, settings, popup sheets and widget hosting |
| `NoticeService.java` | Optional in-memory notification snapshots and action/dismissal support |
| `AndroidManifest.xml` | Home/launcher intents, SDK levels, protected listener and widget/status-bar permissions |
| `styles.xml` | Native dark UI and translucent system-wallpaper window theme |
| `build.ps1` | Framework compilation, packaging, alignment and signing |

Package queries and icon loading use a single worker executor. A 64-entry icon cache and in-flight set bound cached artwork and suppress duplicate requests. Recycled ListView rows avoid rebuilding every entry. One cached ColorMatrix filter implements grayscale without making separate bitmap copies. Clocks use TextClock, and battery updates use Android broadcasts.

Preferences hold favorites, hidden apps, renamed labels, appearance choices and widget configuration. A widget host owns allocated IDs, tracks pending configuration and renders provider RemoteViews. Next/Previous navigation keeps widget touch handling intact.

Wallpaper modes choose a translucent Activity theme before `super.onCreate`. The root clears transparent drawing to avoid stale frames. Backgrounds extend behind system bars; WindowInsets reserve control space. The bottom shade is a Canvas gradient. The alphabet uses a Canvas rail plus a native marker TextView. Sheets use a short 160ms entrance unless Reduce motion is enabled.

This is intentionally a small prototype with substantial UI logic in one Activity. Larger contributions should separate concerns incrementally, retain API 21 guards and verify real API 23 behavior rather than inferring it from modern emulators.
