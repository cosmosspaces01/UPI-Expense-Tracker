# UPI Expense Tracker — Codebase Documentation

> **Last updated**: 29 May 2026  
> **Package**: `com.upi.expensetracker`  
> **Min SDK**: 29 (Android 10) &nbsp;|&nbsp; **Target SDK**: 34 (Android 14)  
> **Architecture**: Single-Activity MVVM with Jetpack Compose + Room (SQLCipher encrypted)

---

## Table of Contents

1. [High-Level Architecture](#high-level-architecture)
2. [Data Flow](#data-flow)
3. [Project Structure](#project-structure)
4. [Data Layer](#data-layer)
5. [Service Layer](#service-layer)
6. [Utilities](#utilities)
7. [UI Layer](#ui-layer)
8. [Theme & Design System](#theme--design-system)
9. [Build Configuration](#build-configuration)
10. [Security Model](#security-model)
11. [Database Schema](#database-schema)
12. [Known Quirks & Edge Cases](#known-quirks--edge-cases)

---

## High-Level Architecture

```
┌─────────────────────────────────────────────────┐
│                   MainActivity                   │
│         (BiometricPrompt gate → Compose UI)      │
└──────────────┬──────────────────────────┬────────┘
               │                          │
        ┌──────▼──────┐           ┌───────▼────────┐
        │ MainViewModel│           │  NavHost       │
        │ (StateFlow)  │◄──────────│  7 screens     │
        └──────┬──────┘           └────────────────┘
               │
    ┌──────────┼──────────────┐
    │          │              │
┌───▼───┐ ┌───▼────┐  ┌──────▼──────────────────┐
│  DAO  │ │SmsParser│  │NotificationListenerSvc  │
│ (Room)│ │(regex)  │  │(intercepts UPI notifs)  │
└───┬───┘ └────────┘  └─────────────────────────┘
    │
┌───▼───────────────────────┐
│  SQLCipher Encrypted DB   │
│  (upi_expense_tracker_db) │
└───────────────────────────┘
```

---

## Data Flow

### SMS Sync (user-initiated)
```
User taps "Sync" → MainViewModel.syncTransactions()
  → SmsParser.syncTodayTransactions(context)
    → ContentResolver reads SMS inbox
    → Regex parsing extracts: amount, merchant, category, ref, date
    → rawSMS body redacted via redactSmsBody()
    → Returns List<TransactionEntity>
  → TransactionDao.insertTransactions() (IGNORE on conflict)
  → StateFlow emits → UI updates
```

### Notification Capture (automatic, background)
```
UPI app sends notification → UpiNotificationListenerService.onNotificationPosted()
  → Check: is target package? is sync enabled?
  → NotificationParser.parseNotification()
    → Delegates to SmsParser.parseSMS() for extraction
    → rawSMS redacted, source set to "NOTIFICATION"
  → Dedup check: TransactionDao.countDuplicates() within ±5 min window
  → If no duplicate → TransactionDao.insertTransactions()
```

### Manual Entry
```
User fills AddTransactionSheet → MainViewModel.addTransaction()
  → Creates TransactionEntity with source="MANUAL"
  → TransactionDao.insertTransactions()
```

---

## Project Structure

```
app/src/main/
├── AndroidManifest.xml
├── java/com/upi/expensetracker/
│   ├── MainActivity.kt              ← Entry point + biometric lock
│   ├── data/                         ← Database layer
│   │   ├── AppDatabase.kt           ← Room DB with SQLCipher
│   │   ├── DatabaseKeyManager.kt    ← Encryption key via Keystore
│   │   ├── TransactionEntity.kt     ← Transaction table schema
│   │   ├── TransactionDao.kt        ← Transaction queries
│   │   ├── CategoryEntity.kt        ← Category table schema
│   │   └── CategoryDao.kt           ← Category queries
│   ├── service/
│   │   └── UpiNotificationListenerService.kt  ← Background listener
│   ├── utils/
│   │   ├── SmsParser.kt             ← SMS regex engine + redaction
│   │   ├── NotificationParser.kt    ← UPI notification → entity
│   │   ├── Exporter.kt              ← CSV/PDF export with sharing
│   │   └── SecureLogger.kt          ← Debug-only log wrapper
│   └── ui/
│       ├── MainViewModel.kt         ← Central state + business logic
│       ├── screens/
│       │   ├── HomeScreen.kt        ← Dashboard (today/month stats)
│       │   ├── TransactionsScreen.kt ← Full list + filters + search
│       │   ├── AnalyticsScreen.kt   ← Monthly charts + breakdowns
│       │   ├── SettingsScreen.kt    ← Config, export, app lock
│       │   ├── InsightsScreen.kt    ← Trends, highlights, subscriptions
│       │   ├── BudgetsScreen.kt     ← Per-category budget limits
│       │   └── CategoriesScreen.kt  ← CRUD for categories
│       ├── components/
│       │   ├── AddTransactionSheet.kt   ← Bottom sheet for manual add
│       │   ├── EditTransactionSheet.kt  ← Bottom sheet for edit/delete
│       │   └── CustomCharts.kt          ← SpendBarChart, TrendLineChart
│       └── theme/
│           ├── Color.kt             ← Design tokens (Midnight Slate)
│           ├── Theme.kt             ← Material3 theme config
│           └── Type.kt              ← Typography scale
└── res/
    ├── xml/file_paths.xml           ← FileProvider scope (exports/ only)
    └── mipmap-*/                    ← App icons
```

---

## Data Layer

### TransactionEntity (`data/TransactionEntity.kt`)
The core data model. Stored in the `transactions` table.

| Field | Type | Purpose |
|-------|------|---------|
| `id` | String (PK) | SHA-256 hash of `amount+merchant+date+refId` |
| `amount` | Double | Transaction amount in ₹ |
| `merchant` | String | Payee name (parsed or user-entered) |
| `accountLast4` | String | Last 4 digits of bank account |
| `refId` | String | UPI reference ID |
| `date` | String | `YYYY-MM-DD` format |
| `time` | String | `HH:mm` format |
| `category` | String | Category name (FK by name, not ID) |
| `description` | String | Auto-generated or user-written |
| `notes` | String | User notes |
| `isSplit` | Boolean | Whether this is a split expense |
| `splitWith` | String | Name of split partner |
| `splitAmount` | Double | Each person's share |
| `isSettled` | Boolean | Whether the split is settled |
| `rawSMS` | String | **Redacted** SMS/notification text |
| `isRecurring` | Boolean | Auto-detected recurring payment |
| `source` | String | `"SMS"`, `"NOTIFICATION"`, or `"MANUAL"` |

### CategoryEntity (`data/CategoryEntity.kt`)
| Field | Type | Purpose |
|-------|------|---------|
| `id` | String (PK) | UUID |
| `name` | String | Display name (e.g., "Food & Dining") |
| `icon` | String | Material icon name (unused in current UI) |
| `color` | String | Hex color string (e.g., `"#FF9F43"`) |
| `budget` | Double? | Monthly budget limit (null = no budget) |

**10 default categories** are seeded on first launch via `AppDatabaseCallback.onCreate()`.

### TransactionDao (`data/TransactionDao.kt`)
| Query | Purpose |
|-------|---------|
| `getAllTransactions()` | Flow of all txns, sorted by date+time DESC |
| `getTransactionsByDate(date)` | Filter by single date |
| `getTransactionsByMonth(prefix)` | Filter by `YYYY-MM` prefix |
| `getTodayTotalSpend(date)` | Sum for today's date |
| `getMonthTotalSpend(prefix)` | Sum for current month |
| `getMonthCategoryBreakdown(prefix)` | Group by category with sums |
| `getRecurringTransactions()` | All `isRecurring=1` txns |
| `getPendingSplits()` | Unsettled splits |
| `countDuplicates(amount, date, timeStart, timeEnd)` | Dedup window check |
| `insertTransactions(list)` | Batch insert with `IGNORE` on conflict |
| `insertTransaction(single)` | Single insert with `REPLACE` on conflict |
| `clearAllTransactions()` | Wipe all data |

### AppDatabase (`data/AppDatabase.kt`)
- **Version**: 2 (Migration 1→2 adds `source` column)
- **Encryption**: SQLCipher via `SupportFactory` — key from `DatabaseKeyManager`
- **Key lifecycle**: Random 256-bit passphrase generated once → stored in `EncryptedSharedPreferences` → retrieved every launch

### DatabaseKeyManager (`data/DatabaseKeyManager.kt`)
- Creates `MasterKey` with `AES256_GCM` scheme (Android Keystore-backed)
- Generates 32-byte random passphrase on first launch
- Stores as hex string in `EncryptedSharedPreferences`
- Returns `CharArray` for SQLCipher's `SupportFactory`

---

## Service Layer

### UpiNotificationListenerService (`service/UpiNotificationListenerService.kt`)
A `NotificationListenerService` that intercepts notifications from known UPI apps.

**Monitored packages** (defined in `NotificationParser.KNOWN_UPI_PACKAGES`):
- `com.google.android.apps.navi` → Google Pay
- `in.org.npci.upiapp` → BHIM
- `net.one97.paytm` → Paytm
- `com.phonepe.app` → PhonePe
- `com.whatsapp` → WhatsApp Pay

**Dedup logic**: Before inserting, checks `TransactionDao.countDuplicates()` for same amount ± 5 minutes. This prevents double-recording when both SMS and notification arrive for the same payment.

**User toggle**: Respects `SharedPreferences` key `notification_sync_enabled`. Even if the OS grants notification access, sync only runs if the user has enabled it in Settings.

---

## Utilities

### SmsParser (`utils/SmsParser.kt`)
The core parsing engine. ~440 lines of regex-based extraction.

**Key functions:**
| Function | Purpose |
|----------|---------|
| `parseSMS(body, dateMs)` | Parses a single SMS body → `TransactionEntity?` |
| `syncTodayTransactions(context)` | Reads today's SMS inbox → list of parsed txns |
| `syncTransactionsForDate(context, dateMs)` | Reads SMS for a specific date |
| `redactSmsBody(body)` | Strips account numbers, UPI VPAs, digit sequences |

**Parsing pipeline:**
1. Credit filter — skip if only credit keywords present
2. Amount extraction — 12 regex patterns for Indian bank SMS formats
3. Merchant extraction — UPI-specific regex + 35 known merchants fallback
4. Date/time extraction — regex for DD/MM/YYYY, DD-Mon-YYYY etc.
5. Category classification — keyword matching against category lists
6. Recurring detection — checks merchant name against subscription keywords
7. ID generation — SHA-256 of `amount|merchant|date|refId` for dedup

**Bank sender patterns**: 60+ sender prefixes (e.g., `AD-HDFCBK`, `VM-ICICIB`, `BZ-SBIUPI`)

### NotificationParser (`utils/NotificationParser.kt`)
Thin adapter that converts notification title+text into a format `SmsParser.parseSMS()` can process. Applies the same `redactSmsBody()` before storing.

### Exporter (`utils/Exporter.kt`)
| Function | Purpose |
|----------|---------|
| `generateCSVString(txns)` | Builds RFC 4180 CSV with formula-injection sanitization |
| `shareCSV(context, txns)` | Writes to `cacheDir/exports/`, shares via `FileProvider` |
| `sharePDF(context, txns)` | Generates A4 PDF with Android `PdfDocument` API, shares |

**Security**: `sanitizeCSVField()` prefixes `=`, `+`, `-`, `@`, `\t`, `\r` characters with a single quote.

### SecureLogger (`utils/SecureLogger.kt`)
Drop-in replacement for `android.util.Log`. All debug/warning output is suppressed in release builds. Error logs emit a generic message in release without sensitive context.

---

## UI Layer

### MainActivity (`MainActivity.kt`)
- Entry point for the app
- **Biometric gate**: If `isAppLockEnabled`, shows `LockScreen` composable + auto-triggers `BiometricPrompt` (fingerprint + device credential fallback)
- Creates `AppDatabase`, `MainViewModelFactory`, sets up `NavHost`
- Bottom navigation: Home, Transactions, Analytics, Settings
- FAB for adding transactions (animated scale in/out)

### MainViewModel (`ui/MainViewModel.kt`)
Central state management. Holds:

**StateFlows** (reactive Room queries):
- `allTransactions` — all txns, sorted by date DESC
- `allCategories` — all categories
- `todayTotalSpend` — sum for today
- `monthTotalSpend` — sum for current month
- `recurringTransactions` — all recurring txns
- `pendingSplits` — unsettled splits

**SharedPreferences** (via `upi_tracker_prefs`):
- `userName` — display name on HomeScreen (default: "Kukkiii")
- `isAppLockEnabled` — biometric lock toggle
- `isNotificationSyncEnabled` — notification capture toggle

**Key methods:**
| Method | Purpose |
|--------|---------|
| `syncTransactions(callback)` | Sync today's SMS → DB |
| `syncTransactionsForDate(ms, callback)` | Sync specific date |
| `addTransaction(...)` | Manual entry with source="MANUAL" |
| `updateTransaction(txn)` | Edit existing |
| `deleteTransaction(txn)` | Remove single |
| `clearAllData()` | Wipe all transactions |
| `injectMockSMS()` | Insert 4 test transactions |
| `sanitizeExistingTransactions()` | Auto-fix `00xx-` year dates → `20xx-` on launch |
| `isSmsPermissionGranted()` | Check READ_SMS permission |
| `isNotificationListenerGranted()` | Check OS notification access |

### Screens

#### HomeScreen (`ui/screens/HomeScreen.kt`)
- Greeting with time-of-day awareness
- Two stat cards: Today's spend + This month's spend
- "Sync Today" button with animated spinner
- "Sync a Past Date" with Material3 DatePicker dialog
- Recent transactions list (top 5) with skeleton loading
- Contains reusable `TransactionItemCard` and `SkeletonCard` composables

#### TransactionsScreen (`ui/screens/TransactionsScreen.kt`)
- 14-day horizontal date scroll selector
- Date range mode toggle (start/end DatePickers)
- Per-date SMS sync button
- Search bar (merchant + notes + amount)
- Min/Max amount range filters
- Category filter chips (horizontal scrollable)
- Sort options: Newest, Oldest, Highest, Lowest
- Full transaction list with `TransactionItemCard`

#### AnalyticsScreen (`ui/screens/AnalyticsScreen.kt`)
- Month selector (left/right arrows)
- Total spend card with delta % vs last month (↑ red / ↓ green pill)
- Segmented color bar showing category proportions
- Daily spending bar chart (`SpendBarChart` component)
- Category detail cards with amounts + percentages
- Top 5 payees ranked list

#### SettingsScreen (`ui/screens/SettingsScreen.kt`)
- Profile card (editable username)
- **App Lock** toggle card (biometric, with lock/unlock icon)
- Permission guide cards (SMS permission, notification access)
- Notification sync toggle
- Export section (CSV + PDF share via FileProvider)
- Developer Tools (gated behind `BuildConfig.DEBUG`)
- Danger Zone (Wipe All Data with confirmation dialog)
- Privacy note footer

#### InsightsScreen (`ui/screens/InsightsScreen.kt`)
- Highlight carousel (horizontally scrollable insight cards):
  - Highest spending day this month
  - Top category
  - Unusual spend detection (>2.5x category average)
- 6-month trend line chart (`TrendLineChart` component)
- Subscription list (auto-detected from `isRecurring` transactions)

#### BudgetsScreen (`ui/screens/BudgetsScreen.kt`)
- Overview card: Total Budget | Spent | Remaining (green/red)
- Per-category cards with progress bars:
  - Green = under 80%, Amber = 80-99%, Red = over budget
  - Tap to set/edit budget limit via AlertDialog
- Budget values stored in `CategoryEntity.budget`

#### CategoriesScreen (`ui/screens/CategoriesScreen.kt`)
- 2-column grid of category cards
- "Add Category" card with accent border
- Each card shows: color dot, name, budget status
- Tap → Edit dialog (name, color picker, budget)
- Long-press → Delete confirmation dialog
- 10 preset color options

### Components

#### AddTransactionSheet (`ui/components/AddTransactionSheet.kt`)
BottomSheet for manual transaction entry. Fields: amount, merchant, category dropdown, date, time, description, notes.

#### EditTransactionSheet (`ui/components/EditTransactionSheet.kt`)
BottomSheet for editing an existing transaction. Same fields as Add + Split toggle (splitWith person, splitAmount, settled checkbox) + Delete button.

#### CustomCharts (`ui/components/CustomCharts.kt`)
Two custom Canvas-drawn charts:
- `SpendBarChart` — 30-day bar chart with accent bars, labels for peak days
- `TrendLineChart` — 6-month line chart with gradient fill, data point dots

---

## Theme & Design System

**Theme name**: Midnight Slate — clean dark fintech aesthetic.

### Color Tokens (`ui/theme/Color.kt`)

| Token | Hex | Usage |
|-------|-----|-------|
| `Background` | `#0F1117` | Page background |
| `Surface` | `#1A1D26` | Cards, bottom sheets |
| `SurfaceElevated` | `#242832` | Modals, elevated content |
| `Accent` | `#00BFA6` | Primary CTA, active states (teal) |
| `AccentDim` | `#00897B` | Borders, secondary buttons |
| `TextPrimary` | `#F0F2F5` | Headings, amounts |
| `TextSecondary` | `#8E95A2` | Labels, subtitles |
| `TextMuted` | `#4A5060` | Timestamps, hints |
| `DebitRed` | `#EF5350` | Debit amounts, danger actions |
| `SuccessGreen` | `#66BB6A` | Settled, credited |
| `WarningAmber` | `#FFA726` | Budget warnings |
| `Divider` | `#1E222C` | Card borders, separators |

Legacy color aliases are maintained for backward compatibility — they all map to the tokens above.

### Typography (`ui/theme/Type.kt`)
Material3 typography scale. Uses system default fonts.

### Theme (`ui/theme/Theme.kt`)
Dark-only theme. Wraps Material3 `darkColorScheme` with the Midnight Slate tokens.

---

## Build Configuration

### `build.gradle.kts`
| Setting | Value | Notes |
|---------|-------|-------|
| `minSdk` | 29 | Android 10+ |
| `targetSdk` | 34 | Android 14 |
| `isMinifyEnabled` | `true` | R8 code shrinking in release |
| `isShrinkResources` | `true` | Remove unused resources |
| Compose BOM | `2024.02.01` | Compose UI version |
| Room | `2.6.1` | Database ORM |
| SQLCipher | `4.5.4` | Database encryption |
| Security-Crypto | `1.1.0-alpha06` | EncryptedSharedPreferences |
| Biometric | `1.1.0` | BiometricPrompt API |
| KSP | used for Room | Annotation processing |

### `proguard-rules.pro`
Keep rules for: Room entities, SQLCipher JNI, Compose, Security-Crypto, Biometric, Kotlin coroutines.

### `AndroidManifest.xml`
| Config | Value |
|--------|-------|
| `allowBackup` | `false` — prevents ADB data extraction |
| Permissions | `READ_SMS`, `RECEIVE_SMS`, `BIND_NOTIFICATION_LISTENER_SERVICE` |
| `FileProvider` | scoped to `exports/` subdirectory only |

---

## Security Model

| Layer | Protection |
|-------|-----------|
| **Database** | SQLCipher AES-256 encryption, key in Android Keystore |
| **App Lock** | BiometricPrompt (fingerprint + device credential fallback) |
| **Logging** | `SecureLogger` — zero output in release builds |
| **rawSMS** | Account numbers, UPI IDs, digit sequences redacted before storage |
| **Backup** | `allowBackup=false` — blocks ADB extraction |
| **Code** | R8 minification + Proguard obfuscation in release |
| **Export** | CSV formula-injection sanitized; FileProvider scoped to `exports/` |
| **Dev Tools** | Mock data injection hidden behind `BuildConfig.DEBUG` |

---

## Database Schema

```sql
-- Table: transactions (version 2)
CREATE TABLE transactions (
    id          TEXT PRIMARY KEY,
    amount      REAL NOT NULL,
    merchant    TEXT NOT NULL,
    accountLast4 TEXT NOT NULL,
    refId       TEXT NOT NULL,
    date        TEXT NOT NULL,    -- YYYY-MM-DD
    time        TEXT NOT NULL,    -- HH:mm
    category    TEXT NOT NULL,
    description TEXT NOT NULL,
    notes       TEXT NOT NULL,
    isSplit     INTEGER NOT NULL, -- 0/1
    splitWith   TEXT NOT NULL,
    splitAmount REAL NOT NULL,
    isSettled   INTEGER NOT NULL, -- 0/1
    rawSMS      TEXT NOT NULL,    -- redacted
    isRecurring INTEGER NOT NULL, -- 0/1
    source      TEXT NOT NULL DEFAULT 'SMS'
);

-- Table: categories
CREATE TABLE categories (
    id     TEXT PRIMARY KEY,
    name   TEXT NOT NULL,
    icon   TEXT NOT NULL,
    color  TEXT NOT NULL,
    budget REAL             -- nullable
);
```

---

## Known Quirks & Edge Cases

| Issue | Location | Details |
|-------|----------|---------|
| **Midnight dedup bug** | `UpiNotificationListenerService` | Dedup uses `HH:mm` string comparison. A txn at 23:57 won't match a duplicate at 00:02 because `"23:57" > "00:02"` is true. The window calculation wraps correctly, but the SQL `time >= :timeStart AND time <= :timeEnd` fails when the window crosses midnight. |
| **Year-fix on launch** | `MainViewModel.sanitizeExistingTransactions()` | Some bank SMS have 2-digit years. The parser once stored dates like `0026-05-21`. This function auto-corrects `00xx-` → `20xx-` on every app launch. |
| **IGNORE vs REPLACE** | `TransactionDao` | `insertTransactions()` (batch) uses `IGNORE` — won't overwrite user edits on re-sync. `insertTransaction()` (single) uses `REPLACE` — used for notification inserts that should update. |
| **Category FK by name** | `TransactionEntity.category` | Categories are linked by name string, not by foreign key. Deleting a category doesn't cascade — orphan txns keep the old category name but fall back to default styling. |
| **Mock data in rawSMS** | `MainViewModel.injectMockSMS()` | Mock transactions have unredacted SMS bodies, but this code path is gated behind `BuildConfig.DEBUG` so it never runs in production. |
| **SharedPreferences for config** | `MainViewModel` | User settings (`userName`, `appLock`, `notifSync`) use unencrypted `SharedPreferences` (`upi_tracker_prefs`). These contain no financial data — just boolean toggles and a display name. |
