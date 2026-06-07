# FinTrack

A private, on-device personal finance tracker for Android. FinTrack reads the
transaction notifications your bank, card, and payment apps already post, turns
them into income/expense entries, and shows your spending — without sending any
data off your phone.

## How it works

FinTrack runs a `NotificationListenerService`. For each notification from an app
you've chosen to monitor, it:

1. extracts the dollar amount,
2. classifies it as income or expense by keyword,
3. auto-categorizes the spend (Groceries, Dining, Transport, …), and
4. stores it locally in a Room database.

Notifications that are purely informational — available credit, credit limit,
statement balance, payment-due reminders, rewards balances — are recognized and
skipped so they don't become phantom transactions. Credit-card charges are
always recorded as expenses even though the word "credit" appears in the text.

Anything the parser can't classify confidently is flagged for review so you can
correct it in the app.

## Features

- Automatic transaction capture from notifications (opt-in per app)
- Manual add / edit / delete and re-categorization
- Monthly dashboard: income, spending, net, and a 6-month trend
- Spending-by-category breakdown with per-category monthly budgets
- 100% on-device — no accounts, no network calls, no telemetry

## Privacy

All data stays on the device. FinTrack makes no network requests and has no
backend. Notification access is only used to read transaction text from the apps
you explicitly enable.

## Tech

- Kotlin + Jetpack Compose (Material 3)
- Room for local persistence
- `NotificationListenerService` for capture
- minSdk 26, targetSdk 34

## Building

Open the project in Android Studio and run the `app` module on a device or
emulator (Android 8.0+). After install, grant notification access in
**Settings → Notification access** and pick the apps to monitor inside the app.

Unit tests for the notification parser:

```
./gradlew testDebugUnitTest
```

## License

MIT — see [LICENSE](LICENSE).
