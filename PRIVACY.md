# Privacy

Rill has no Internet permission, advertising SDK, analytics SDK, account, telemetry endpoint or background downloader.

Installed launcher activities are queried to build the app list. Favorites, hidden apps, renamed labels, widget IDs and appearance choices stay in local SharedPreferences. Android application backup is disabled in the manifest.

Notification access is optional. If enabled, notification content can appear in the launcher and is kept in service memory, not a persistent notification log. Revoking access stops this integration. Widget providers and launched applications have their own permissions, accounts, network behavior and privacy policies.

Rill uses Android's wallpaper chooser and does not upload wallpapers. The app declares widget binding and notification-shade expansion permissions and a notification listener protected by Android's binding permission. There is no contacts, location, calendar, storage, accessibility or usage-stats permission.
