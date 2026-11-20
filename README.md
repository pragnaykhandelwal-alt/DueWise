# DueWise

DueWise is an Android app that helps **one household** keep track of the things at home that have a date on the package: food, water, medicine, first aid supplies, cosmetics, documents, and subscriptions. It records what you have, where it is, and the date printed on it. Then it tells you what needs a look: items with a near, passing, or passed date, and food that could be swapped between an emergency kit and the pantry so the older stock gets used first.

It was built for the **Congressional App Challenge 2026** by a student developer. (It was called **ReadyShelf** until 2026-10-18. The app ID, database file names, and Java package keep `readyshelf` on purpose, so the folder and code still say ReadyShelf.)

> **Not a safety tool.** DueWise only records dates and the household's own answers. It never says a food is safe or unsafe, and it gives no medicine advice. A date on a package is for the household to read and decide about.

## What makes it different

- **Works offline.** All data stays on the phone. The app has no INTERNET permission, no accounts, no analytics, no ads, no server, no cloud backup, and no phone-to-phone transfer.
- **Nothing changes without a review.** Every save, move, and swap shows a review step first, and every change adds a line to the batch's history.
- **Built around dates.** Status labels come from a small set of date rules written and tested separately from the screens (see [How the rules work](#how-the-rules-work)).

## How the app works

### The basic ideas

| Word | Meaning | Example |
|---|---|---|
| **Product** | One kind of item: a name, optional variant and package size, and a unit | "Granola bars, Oat and honey, 1.5 oz, counted in bars" |
| **Batch** | Some of one product, with one date, in one place | "6 bars, Best by 12 Oct 2026, in the Emergency kit" |
| **Location** | A place where batches are kept. Every location has a type | Pantry, Emergency kit, First aid box |
| **Kit** | A location of type Emergency kit or First aid | |
| **Label type** | The kind of date on the package | Best by, Use by, Expires, Other |

A new install starts with three locations: **Pantry**, **Emergency kit**, and **First aid box**. You can add more when you add an item (for example "Filing cabinet" for documents).

### Date statuses

Every batch with a confirmed, complete date gets one status, by comparing calendar dates with today:

| Status | When |
|---|---|
| **Date passed** | The date is before today |
| **Date is today** | The date is today |
| **Near date** | The date is 1 to *N* days away (*N* = "near-date days", 30 unless you change it in Settings) |
| **Later date** | The date is further away |
| **Date needs review** | The date is missing, only partly known (for example "03/2027"), or you haven't ticked "I checked the date and label type on the package" |

The label type (Best by, Use by, …) never changes the status. DueWise reports dates; it doesn't judge the food.

### The screens

**Home**
- The logo, then a grid of square tiles, two per row.
- **Status tiles** come first, one for each status that has batches, in the order *Near date → Date is today → Date passed*. Each shows a count. Tapping one opens Inventory showing only those batches. "Date needs review" has no tile; find those batches under Inventory's Status filter.
- Then the shortcut tiles: **Inventory**, **Purchase/Renewal list**, **Settings**, and **Add item**.
- **Recommended swaps** appear as cards when the swap rules find any (see [Swaps](#swaps-moving-older-stock-into-use)).
- A short note says "Nothing needs review right now." when everything is fine.

**Inventory**
- Every batch as a card with a category icon, amount, place, date, and a colored status label.
- Search by product name. **Filters** (folded until opened) cover status, location, category, and sort order, and there is a switch to show archived batches. Chosen filters appear as chips under the search box.
- A floating **Add item** button shrinks to **+** while you scroll.

**Add or edit an item** (one form, always with a review step before saving)
1. **What kind of item?** Food, Water, Medicine, First aid supplies, Cosmetics, Documents, Subscriptions, or Other.
2. **Product:** one box that searches your products, or add a new one with a name and a unit. The same product can't be added twice.
3. **Location and quantity** (− and + buttons). A new document or subscription is just one item, so it asks for no quantity.
4. **The date:** pick a full date, type a partial one ("03/2027"), or choose "No date". If the label shows only a month and year, an optional helper offers the last day of that month, but only if you tap it, and the date stays "needs review" until you confirm it.
5. **Confirm the date** with the checkbox, **Review and save**, then **Confirm and save** (or **Save and add another**).
- Going Back from a filled-in form asks "Leave without saving?".

**Batch details** shows the batch's facts, its status, and its history (newest first), with these actions:
- **Done** (record that some or all were used), **Record discarded**, **Move** (all or part to another place), **Archive**, and **Edit**.
- Every action is two steps: how much, then a review showing the amounts before and after. Leaving a kit shows "Replacement may be needed."
- Changing a batch's amount on the Edit form is a "count correction" in the history.
- Archiving hides a batch from Inventory but keeps it and its history.

**Purchase/Renewal list** has three parts:
- **On your purchase list:** things you plan to buy. These are notes only and never count as stock. **Record as bought** opens the item form with the product filled in; saving adds a new batch and removes the list item in one step.
- **Replacement needs:** expired food, water, first aid supplies, cosmetics, and Other items in any place, plus kit items that a swap can't cover. Each card has **Add to purchase list**. Medicine never appears here, because DueWise gives no advice about medicine.
- **Renewal:** documents and subscriptions dated today or earlier. **Renewed** gives the same record a new date, and **Discard** records it as discarded and archives it.

**Settings**
- **Reminders:** the Daily reminder switch, notification status, and **Show reminder now**.
- **Planning settings:** near-date days (1–365, starts at 30) and replacement days (1–3650, starts at 180, must be more than the near-date days). Both are planning choices, not safety guarantees.

### Swaps: moving older stock into use

The idea: a kit holds food, water, and supplies for emergencies, so when a kit item gets close to its date, swap it with a **fresher batch of the same product from the pantry**. The older item moves to the pantry to be used, and the fresher item goes into the kit.

DueWise proposes a swap only when:
- the kit batch is in a kit, in a swappable category, with a complete confirmed date that is 1 to *N* days away (near, but not today or past);
- the pantry has the **same product with the same unit**, with a complete confirmed date that is later than the kit batch's and at least the "replacement days" from today (180 by default).

Medicine, documents, and subscriptions are never swapped. Partial swaps are allowed, and each pantry unit goes into at most one proposal.

A swap goes through **Review swap**, which shows both moves, why the replacement qualifies, and the totals before and after. Nothing moves until you tap **Confirm swap**. At that moment DueWise re-checks everything in one database transaction. If any batch changed, or the rules no longer propose this exact swap, nothing is saved. A swap can be saved only once, even after a double-tap or a restart. Both batches get a "Swapped" history entry, and part of a batch keeps its original date.

### Reminders and the widget

- **Daily reminder.** A background check runs once a day (around 09:00; Android decides the real time to save battery). If any batch is *Date passed*, *Date is today*, or *Near date*, one notification appears, naming up to two items and counting the rest. Medicine is never named. It says "medicine" instead. Batches that only need review never trigger a reminder. At most one reminder a day.
- **App icon number.** While the reminder is shown, it carries the total of Home's status tiles (some phones show only a dot).
- **Home-screen widget "Review list."** A small dark widget that shows how many items need review. It never names items and needs no permission. Tapping it opens Home.

All of this runs on the phone. Nothing is sent anywhere.

### Label photos (hidden)

The app has a finished label-photo feature: it takes photos with its own camera screen (CameraX), reads the text on the phone with Tesseract, and offers possible dates to choose from. It never guesses whether `03/04` means March 4 or April 3. The "Take label photos" button is currently **hidden** (`BatchEditActivity.LABEL_PHOTOS_SHOWN = false`) because the student isn't using it, so the app doesn't ask for the camera. The code and its tests are kept.

## How the rules work

The decisions are kept apart from the screens, in plain Java classes in `domain/` that don't depend on Android:

| Class | Job |
|---|---|
| `DatePolicy` | Turns a date and today's date into one of the five statuses |
| `RotationPlanner` | Finds proposed swaps and what still needs replacing |
| `DateCandidateParser` | Finds possible dates in text read from a label photo |

Their requirements and every design decision are in [SPEC.md](SPEC.md), and each has unit tests. Because they depend only on their inputs (never the phone's clock), the same inputs always give the same answer.

## Your data and privacy

- **Local database:** Room (SQLite) file `readyshelf.db`, currently **version 4**. Tables: locations, products, batches, batch history, swap receipts, and purchase-list items. Each schema version is saved in `app/schemas/`.
- **Safe upgrades:** every schema change has a Room migration. Destructive migration is never used on household data.
- **Settings and reminder state** live in the app's private storage.
- **Permissions:** no INTERNET. Notifications are asked for only when you turn on the reminder or tap "Show reminder now". The camera permission is declared for the hidden label-photo feature. WorkManager adds wake-lock and boot-completed so the daily check survives a restart.
- **No backup or transfer:** Android's cloud backup and device-to-device transfer are excluded in `app/src/main/res/xml/`.
- A single total number can appear outside the app (on the icon and widget), and it never names items.

## Tech stack

| | |
|---|---|
| Language | Java (no Kotlin app source, no Compose) |
| UI | Android Views, XML layouts, Material Components |
| Database | Room 2.x, through the Java annotation processor |
| Background work | WorkManager |
| Camera / text reading | CameraX / Tesseract4Android (on-device) |
| Build | Gradle (Groovy scripts), AGP 9, version catalog in `gradle/libs.versions.toml` |
| SDK | minSdk 26, compileSdk and targetSdk 37 |

[TOOLCHAIN.md](TOOLCHAIN.md) lists every version with its official source.

## Project layout

```
app/src/main/java/com/example/readyshelf/
  data/        Room entities, DAOs, migrations, repository, swap and purchase stores
  domain/      the rules (DatePolicy, RotationPlanner, DateCandidateParser) and the
               plain-Java logic around them: statuses, filters, reminder checks, lists
  reminders/   daily check (WorkManager), notification, preferences
  widget/      home-screen widget
  ui/          one package per screen area: home, inventory, batch, stock, swap,
               purchase, settings, photo, plus shared helpers in common
app/src/test/          JVM unit tests (rules, logic)
app/src/androidTest/   tests that run on an emulator or phone
app/schemas/           saved Room schema for every database version
```

## Build and run

You need Android Studio (its bundled JDK 25) and, to run the app, an Android 8.0+ phone or emulator. In PowerShell, from the project folder:

```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"

.\gradlew.bat assembleDebug          # build
.\gradlew.bat testDebugUnitTest      # unit tests
.\gradlew.bat installDebug           # install on a connected phone or emulator
```

Notes:
- If Maven Central returns HTTP 429 on your network, `settings.gradle` uses Google's mirror of it (see TOOLCHAIN.md).
- Don't install an older build over newer data. The database upgrade to version 4 is one-way.

## Other documents

| File | What it holds |
|---|---|
| [SPEC.md](SPEC.md) | Full scope: every screen, rule, and decision, with dates |
| [DEMO.md](DEMO.md) | Step-by-step demonstration script |
| [TEST-RESULTS.md](TEST-RESULTS.md) | Real results of each build, test run, and review |
| [TOOLCHAIN.md](TOOLCHAIN.md) | Every tool and library version with its official source |
| [THIRD-PARTY-NOTICES.md](THIRD-PARTY-NOTICES.md) | Licenses of the open-source parts inside the app |
| [AI-USE.md](AI-USE.md) | Disclosure of all AI-assisted work, as the contest rules require |
| [STUDENT-WORK.md](STUDENT-WORK.md) | The student's own notes and explanations |
