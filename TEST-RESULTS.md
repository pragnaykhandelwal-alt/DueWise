# TEST-RESULTS.md — review of ReadyShelf's first version (2026-10-11)

- **Date:** 2026-10-11
- **Code:** commit f801e0d on main, plus the changes of this review (listed in section 4 and in AI-USE.md). These were tested as described here, then committed on 2026-10-12 as the next commit on main.
- **Who ran what:** Claude Code (Anthropic, model Claude Opus 5.5) ran every check in sections 1, 3, 4, and 5. The student runs the phone checklist in section 7. AI use is recorded in AI-USE.md.
- **Scope:** this is a test record of a first version. It is not a statement that the app is ready for production, and ReadyShelf is not medically validated. The app records dates and the household's own answers. It does not say whether food is safe, and it gives no medicine advice.

## Summary
- **Your rule files are not in the project.**
  - `DatePolicy.java`, `RotationPlanner.java`, `DatePolicyTest.java`, and `RotationPlannerTest.java` were not found in the project, on another branch, in a stash, or in AndroidStudioProjects, Desktop, Documents, Downloads, or OneDrive.
  - `AppContainer` still passes `null` for both rules.
  - So none of your rule tests could run, and there are no rule failures to explain. Section 6 lists the rule test cases still to write, in words.
- **The app's own checks all passed:**
  - Debug build: successful.
  - Unit tests: 167, with 0 failures.
  - Instrumented tests on the emulator: OK (69 tests).
  - Lint: 0 errors, and the same 3 warnings as before.
- **Repairs:** three Android integration and storage defects were repaired (R1, R3, R4). One suspected defect could not be reproduced (R2).
- **Open items:** section 4.4. The most important one is for you: only your RotationPlanner keeps damaged, unsealed, or storage-concern food out of swaps (D-1).

## 1. Checks Claude Code ran

### 1.1 Tools
- Windows 11, PowerShell.
- `JAVA_HOME` = `C:\Program Files\Android\Android Studio\jbr` (JetBrains Runtime 25.0.3).
- The project's Gradle wrapper (Gradle 9.6.0).
- Emulator: Pixel_10a, Android 17 (API 37), x86_64, as `emulator-5554`.
- Your phone (ZY22KN9QJP) stayed connected and was not used.

### 1.2 Before any change (baseline)

| Check | Command | Real result |
|---|---|---|
| Build, unit tests, lint (every task run again) | `.\gradlew.bat assembleDebug testDebugUnitTest lintDebug --rerun-tasks` | BUILD SUCCESSFUL in 14s. javac printed no warnings. The manifest merger printed its 2 known warnings (the INTERNET and CAMERA removals) |
| Unit tests | (same run) | 157 tests in 15 classes, 0 failures, 0 errors, 0 skipped |
| Lint | (same run) | 0 errors, 3 warnings: unused colors `black` and `white`, and `TooManyViews` in `activity_batch_edit.xml` |
| Instrumented tests | `adb -s emulator-5554 shell am instrument -w com.example.readyshelf.test/androidx.test.runner.AndroidJUnitRunner` | OK (58 tests) |
| Notifications refused | `pm revoke … POST_NOTIFICATIONS`, then `am instrument -w -e notifications denied -e class com.example.readyshelf.reminders.ReminderDeniedTest …` | OK (1 test). logcat showed it start and finish |
| Permissions | `aapt2 dump permissions app-debug.apk` | POST_NOTIFICATIONS, WAKE_LOCK, RECEIVE_BOOT_COMPLETED, and AndroidX's app-private DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION |

### 1.3 After the changes (final run)

| Check | Command | Real result |
|---|---|---|
| Build, test APK, unit tests, lint (every task run again) | `.\gradlew.bat assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug --rerun-tasks` | BUILD SUCCESSFUL in 27s. javac printed no warnings. The manifest merger printed the same 2 known warnings. Gradle printed its known notice "Deprecated Gradle features were used in this build" (two template lines in `app/build.gradle`; see TOOLCHAIN.md, "Known warnings") |
| Unit tests | (same run) | **167 tests, 0 failures, 0 errors, 0 skipped.** By class: LabelDateFinderTest 32, RuleRunnerTest 26, ReminderCheckTest 15, StockChangeTest 14, DateThresholdsTest 12, CategoryGuesserTest 11, DateEntryTest 11, InventoryFilterTest 9, LabelReadingTest 8, NameCandidatesTest 7, PurchaseSuggestionsTest 7, ReminderTimingTest 5, SwapPreviewTest 5, BatchTextTest 4, ExampleUnitTest 1 |
| Lint | (same run) | 0 errors, the same 3 warnings |
| Instrumented tests | install both APKs with `adb -s emulator-5554 install -r -t …`, then `am instrument -w …` | **OK (69 tests).** By class: SwapStoreTest 24, PurchaseListStoreTest 14, ReminderFlowTest 9, ReadyShelfRepositoryTest 7, MigrationTest 6, ReminderSchedulerTest 3, ReminderPrefsTest 2, ReminderWorkerTest 1, ExampleInstrumentedTest 1, and ReminderDeniedTest 1 and ReminderRebootSetup 1, which skip themselves in a normal run |
| Notifications refused | as in the baseline | OK (1 test). logcat: started 22:07:48.603, finished 22:07:50.632 |
| Permissions | `aapt2 dump permissions` | The same four. **No INTERNET, CAMERA, ACCESS_NETWORK_STATE, FOREGROUND_SERVICE, or exact-alarm permission** |
| Source languages | file search in `app/src` | 116 `.java` files, 0 `.kt`/`.kts` files |

### 1.4 "Connected tests"
- `.\gradlew.bat connectedAndroidTest` was **not** used. It runs on every connected device, so it would also run on your phone and uninstall ReadyShelf there, which deletes its data.
- The same instrumented tests ran on the emulator only, with `adb -s emulator-5554` (see TOOLCHAIN.md).

## 2. Your domain tests
- **Not run: the files are missing** (see the Summary). No rule failures can be explained until your tests exist.
- The domain tests that did run are Claude's tests of the app's own code: RuleRunner, SwapPreview, PurchaseSuggestions, DateEntry, LabelDateFinder, ReminderCheck, and the others in section 1.3.
  - Their policies and planners only return answers that each test sets up. They are not a DatePolicy or a RotationPlanner and contain no rules.

## 3. The requested scenarios
"App side" means Claude's code: showing, checking, and saving what the rules answer. "Rule side" means your DatePolicy and RotationPlanner, which decide those answers.

| Scenario | App-side check (ran) | Result | Rule side |
|---|---|---|---|
| **Partial swap: kit 6, pantry 2 → swap 2, still 4** | `RuleRunnerTest.plan_partialSwapOfSixAndTwo_showsTheSwapOfTwo_andTheNeedOfFour` (new), `SwapPreviewTest.allOfThePantryBatch_sixAndTwo_…` (new), `SwapStoreTest.partialSwap_sixAndTwo_…` (new), `PurchaseSuggestionsTest.aPartlyReplacedNeedOfFour_…` (new) | Pass. The swap of 2 and the need of 4 are shown. Saving moves 2 each way: the kit batch keeps 4 with its date, the whole pantry batch moves into the kit, a new pantry batch of 2 keeps the kit date, label type, and answers. 8 units before and after (6 in the kit, 2 in the pantry). The need of 4 becomes a purchase suggestion of 4 | **Not run** (RotationPlanner missing). Your planner must propose 2 and note 4 |
| **Two kits can't use the same pantry units** | `RuleRunnerTest.plan_dropsALaterProposalThatWouldUseTheSameUnitsAgain` (existing), `…ThatWouldTakeTheSameKitUnitsAgain` (new), `SwapStoreTest.twoSwapsShownForOnePantryBatch_onlyTheFirstConfirmedIsSaved` (new), `SwapStoreTest.stalePantryBatch_savesNothing` (new) | Pass. A proposal that would use more units than a batch has left is not shown. Of two swaps shown for one pantry batch, the first is saved and the second gives "batch changed" and saves nothing | **Not run.** Your planner must give each pantry unit to at most one proposal |
| **Different products, package sizes, units, and date labels** | `ReadyShelfRepositoryTest` (new): other capital letters → the same product; another variant, size, or unit → a new product; a sample product is never reused. `RuleRunnerTest.plan_dropsAProposalOfTwoDifferentProducts` (existing). `RuleRunnerTest.statusesAndPlans_passEveryLabelType_…` (new) | Pass. Use by, Expires, Other, printed text only, unconfirmed dates, and the sealed, damaged, and storage answers reach both rules unchanged | **Not run.** Your planner must require the same product and unit and known, confirmed labels |
| **Missing dates, today, yesterday, threshold bounds, leap days** | `RuleRunnerTest` (existing: kit food dated today, yesterday, or with no date is never swapped and always gets a need), `DateThresholdsTest` (existing: 1 and 365, 1 and 3650, replacement > near-date), `DateEntryTest.lastDayOfPrintedMonth_february_centuryYears` (new: 02/2000 → 29 Feb, 02/2100 and 02/1900 → 28 Feb), `LabelDateFinderTest.theLeapDay_isADateOnlyInALeapYear` (new: "EXP 02/29/2027" gives no date; "02/29/2028" and "29/02/2028" give 29 Feb 2028), `BatchTextTest.date_theLeapDay` (new) | Pass | **Not run** (DatePolicy missing). The statuses at today, yesterday, N, and N+1, and across 29 Feb, are your rules |
| **Medicine kept out** | `RuleRunnerTest` (existing kit side; new `plan_dropsAProposalWithMedicineOnThePantrySide`), `plan_needs_leaveOutMedicine…` (existing), `PurchaseSuggestionsTest.onlyFoodInKitsBecomesASuggestion` (existing), `ReminderFlowTest` (existing: no medicine name in the notification) | Pass | **Not run.** Your planner must leave out medicine and other categories |
| **Damaged food kept out** | Code review of `RuleRunner.passesSafetyChecks` and `needs()` | **No app-side exclusion** (open item D-1). The sealed, damaged, and storage answers only reach your planner (`PlanBatch`). No app filter was added, because it would hide a planner mistake | **Not run.** Your planner must leave out damaged, unsealed, storage-concern, and unanswered batches |
| **Stale proposals, repeated taps, rollback** | `SwapStoreTest` (21 existing + 3 new), `PurchaseListStoreTest` (14 existing) | Pass. A changed batch, a new day, or changed settings → nothing saved. The same action ID tapped again → saved once. A forced failure (test-only SQLite trigger) after both movements → neither movement kept | Re-planning uses your planner when it exists |
| **A purchase-list item doesn't change stock** | `PurchaseListStoreTest.add_…`, `changeQuantity_…`, `remove_…_andChangesNoStock` (existing). Emulator: the purchase list screen | Pass. Every batch and history row is unchanged | n/a |
| **Restart persistence** | Emulator: force-stop and relaunch, then `adb reboot` and relaunch (section 5). `ReminderPrefsTest` (existing) | Pass. A database copy from before the force-stop and one from after the reboot were identical (23 rows). Planning settings 45/200 were kept | n/a |
| **Migrations without data loss** | `MigrationTest` (5 existing + new `migrate2To3_whenAStepFails_changesNothing`). Emulator: an upgrade from commit 048b49e (database version 1) to this build (section 5) | Pass. Every version 1 row was kept. A failing 2→3 step leaves the file at version 2, unchanged | n/a |

## 4. Review against SPEC.md

### 4.1 What matches SPEC
- **Data:**
  - Every database write runs in one transaction on one background thread.
  - There is no database work on the main thread.
  - Migrations 1→2 and 2→3 are registered, and there is no destructive fallback.
  - Schemas 1–3 are saved, and the foreign keys and cascades match "Data kept on the phone".
- **Purchase list and swaps:**
  - The purchase list never changes stock. "Record bought item" adds a new batch and removes the item in one transaction.
  - The swap action ID is kept in a `SavedStateHandle`, and "Confirm swap" is turned off while it saves.
- **Wording:**
  - No app text, sample data, or comment says food is safe, or advises taking or replacing medicine.
  - Toasts stay short enough for two lines.
- **Privacy:**
  - The permissions in 1.3.
  - Backup and phone-to-phone transfer exclude all app data.
  - The only exported parts are the launcher screen and WorkManager's and AndroidX's system-protected receivers and service.
- There are no TODO or FIXME notes.
- Every screen in SPEC's Screens table that can open without your rules matched on the emulator (section 5). The Review swap screen needs RotationPlanner, so it was not opened.

### 4.2 Repaired (Android integration and storage)

**R1: a second Home after a reminder started ReadyShelf.**
- *Before (emulator, this build without R1):*
  1. A reminder tap started ReadyShelf, so the task's first intent was `dat=readyshelf-review:1791779327483`.
  2. Settings was opened, then the Home key was pressed.
  3. Starting ReadyShelf like the launcher gave the stack Home → Settings → **Home**.
- *Fix:* `HomeActivity.onCreate` closes itself when it is not the task's first screen and was started from the launcher (MAIN + LAUNCHER).
- *After (emulator):*
  - The same steps show Settings, with the stack Home → Settings and no crash.
  - Also checked:
    - a fresh launcher start opens Home
    - a reminder tap with no task opens Home
    - a reminder tap from Settings opens Home on top at "Needs review", and Back returns to Settings
    - a reminder tap while Home is in front keeps the same Home (no new screen), and in landscape scrolls it to "Needs review"

**R3: archived batches would reach DatePolicy.**
- *Before (code review):* SPEC says "Archived batches are never given to the rules."
  - `InventoryViewModel` passed archived rows (with "Show archived batches" on) to `RuleRunner.withStatuses`.
  - `BatchDetailsViewModel` asked for the status of any batch.
  - It couldn't be seen yet, because DatePolicy is not connected.
- *Fix:* a new `RuleRunner.statusesOfActive`, used by both screens, asks the policy only about batches that are not archived. Batch details shows "Status: none, because this batch is archived. The date rules leave out archived batches."
- *After:*
  - `RuleRunnerTest.statusesOfActive_neverGivesAnArchivedBatchToThePolicy` passes.
  - Emulator: an archived batch's details showed that status line.

**R4: three write paths could close the app on a database error.**
- *Before (code review):* "Add location", "Load sample data", and "Remove sample data" ran their transaction with `background.execute(...)` and no error handling. An error would end that background task unhandled, which closes an Android app. Not reproduced before the fix.
- *Fix:* they go through the repository's existing error handling (`runChange`). The screens then show "Sample data could not be added. Nothing was changed.", "Sample data could not be removed. Nothing was changed.", or "The location could not be added. Nothing was changed."
- *After:*
  - `ReadyShelfRepositoryTest`: each of the three, with a test-only SQLite trigger that makes it fail, gives FAILED and leaves every table unchanged.
  - Emulator, with temporary triggers in a copy of the app's database:
    - Both messages appeared, nothing was saved, and the app stayed open.
    - logcat: "A change could not be saved … SQLiteConstraintException: made-up failure (code 1811 SQLITE_CONSTRAINT_TRIGGER)".
    - The app's data was cleared afterwards.

### 4.3 Checked and not a defect
**R2: typed planning numbers after Android stops the app.**
- 45 was typed without saving (the saved value was 30), then the Home key, then `am kill`.
- Reopening showed Settings in a new process (pid 10106) with **45** still typed.
- Not a defect. SPEC's follow-up line that said otherwise was corrected.

### 4.4 Open defects and observations

| ID | Severity | What | How to see it | Expected | Actual | Status |
|---|---|---|---|---|---|---|
| D-1 | Medium (yours) | Only RotationPlanner keeps damaged, unsealed, storage-concern, unanswered, and non-shelf-stable food out of swaps | Read `RuleRunner.passesSafetyChecks` | Your planner's tests prove these exclusions | Not testable: RotationPlanner is missing. The app adds no filter on purpose, so planner mistakes stay visible. The Review swap screen shows the answers before "Confirm swap" | Open, for your RotationPlanner and its tests. **Closed on 2026-10-22 by the student's choice** (section 38): since that evening the swap rules don't use the sealed, damaged, and storage answers or the food class, so there is nothing left to keep out. Medicine, documents, and subscriptions are kept out by the swap rules and by the app's safety net |
| D-2 | Low | Stock changes and the Edit form check only that the quantity is unchanged before saving | Change a batch's location in another screen while a stock-change review is open | SPEC: "If the batch changed after the review was shown, nothing is saved" | Only a quantity change stops the save. The review rebuilds its text from the live batch, so a new location or date is shown before "Confirm" | Open. Suggested fix: compare the whole batch, as `SwapStore` does |
| D-3 | Low | A sample batch edited onto a household product could mix sample and real needs in one purchase suggestion | Needs RotationPlanner | Sample suggestions can't be added | The suggestion takes the sample mark of its first batch (`PurchaseSuggestions`) | Open (code review only) |
| D-4 | Low | A tap on the reminder switch can wait while a daily check runs | Needs DatePolicy (the switch is off now) | The tap answers at once | They share one lock, and the check reads the database while holding it | Open (code review only) |
| D-5 | Low | A screen that stays open past midnight keeps the old day until it comes back to the front | Leave a screen open over midnight | — | Already in SPEC's follow-ups. A swap save always checks with the current date | Open |
| D-6 | Info | Amounts of one read "1 bars" | Move 1 of a batch counted in "bars" | — | Units are the household's own words, so the app can't make them singular | Not planned |
| D-7 | Info | At the largest font size, the empty purchase-list notice wraps well before the edge of its box | Emulator, font scale 2.0 | — | Nothing is cut off; Android's line breaking | Not planned |
| D-8 | Info | The database's name check folds only A–Z capitals (SQLite `NOCASE`) | Code review | — | The batch form's own check (Java `equalsIgnoreCase`) runs first and covers other letters, so no duplicate can come from the form | Not planned |
| D-9 | Info | `PurchaseListStore.recordPurchase` doesn't check the row count of its food-class update | Code review | — | Harmless: the value was read in the same transaction | Not planned. **No longer applies** since 2026-10-22 (section 38): `recordPurchase` has no food-class update any more |
| D-10 | Info | Lint's 3 known warnings, and Gradle's notice about two template lines | 1.3 | — | Unchanged since 2026-10-04 | Recorded in TOOLCHAIN.md |
| D-11 | Medium | Home's banner hides its last status tile when it has an odd number of tiles (found on 2026-10-19; see 17.2) | Home with 1 or 3 statuses, for example the sample data | Every status that has batches gets a tile | The last row holds the tile and an empty `Space`. The tile's height is `match_parent` and the `Space`'s is 0, so Android's LinearLayout gives that row no height, and the tile none. Since `7719052` (2026-10-18), so the phone has it too | Fixed on 2026-10-19: the `Space` is now `MATCH_PARENT` tall (section 18). Not yet on the phone |

## 5. Emulator checks Claude ran (not the phone)
- **Every screen against SPEC's Screens table** (sample data loaded; the household phone was not used):
  - **Home:** sample banner, four buttons, and "Not available yet" for Needs review and swaps.
  - **Inventory:**
    - 6 of 6
    - the search "gran" → 2 of 6
    - Earthquake kit filter → Granola bars and Bottled water
    - an archived batch hidden (6 of 6), then shown with "Show archived batches" (7 of 7)
  - **Batch details:** every field, the "Sample item" mark, the status line, the food answers, and history newest first.
  - **Record used → Some → 2:**
    - The review said "This batch: 6 bars before, 4 bars after." and showed the kit warning "Replacement may be needed…".
    - The review stayed after rotating to landscape and back.
    - After Confirm: "Quantity: 4 bars", with the history entry.
  - **Move → Some → 1 → Pantry:** the review showed 3 left in the kit, a new batch of 1, and the pantry total 10 → 11. Saved as reviewed.
  - **Archive:** "Archived", only "Edit batch" offered, and the R3 status line.
  - **Edit:**
    - The food class was set to Shelf-stable food, and Sealed to Yes.
    - The review listed both changes, and the history recorded them.
  - **Add batch:**
    - New product "Test rice", 2 bags, partial date "03/2027".
    - The "Use the last day of the month (31 Mar 2027)" button appeared.
    - The typed fields stayed after rotating.
    - The review listed every field, and the batch was saved.
  - **Purchase list:** empty, with both "Not available yet" notes.
  - **Settings:** 180/180 showed the error on the replacement field; 45/200 was saved.
  - **Remove sample data:**
    - The dialog appeared. Rotating with it open closed it, by design, and nothing crashed.
    - After confirming, only "Test rice" remained (1 of 1).
- **Restart persistence:**
  - Sample data, "Test rice", and settings 45/200 were in place.
  - A database copy was taken (this force-stopped the app). After relaunching: 7 of 7 batches, 45/200, "No daily reminder has been shown yet."
  - `adb reboot` (boot finished after about 20 s), then relaunch: the same.
  - The database copy from after the reboot was identical to the one from before (23 rows). `PRAGMA foreign_key_check` was empty.
- **Upgrade from commit 048b49e** (database version 1), built in a temporary worktree:
  - In the old build: sample data was loaded, and a location "Car kit" and a batch "Tea" (3 boxes, no date) were added.
  - `adb install -r` with this build. The app opened with no crash.
  - Comparing the database copies:
    - `user_version` 1 → 3
    - the same 4 locations and the same 7 history entries
    - all 7 batches with the same name, amount, unit, date, location, and sample mark
    - dates start unconfirmed, as SPEC says
    - 6 products for the 6 different name, unit, and sample-mark combinations
    - the foreign-key check empty
  - Inventory showed 7 of 7 with "Date needs review".
- **Airplane mode on** (`cmd connectivity airplane-mode enable`; no active network):
  - The app started, Inventory search worked ("bean" → 1 of 7), and "Record discarded 1" was saved (4 → 3 cans).
  - After "Allow", the test reminder was posted (ID 1002).
  - The app's own log had no network errors. The 3 log lines with "network" or "connection" came from Android: a library every app loads, and keyboard warnings.
  - Airplane mode was turned off again.
- **Large text** (`font_scale` 2.0): screenshots of Home, Inventory (filters and rows), Batch details, Add batch, Purchase list, and the Settings reminders section.
  - Text wraps, every screen scrolls, nothing is cut off, and the buttons are full width.
  - Then `font_scale` went back to 1.0.
- **Denied permission** (fresh install):
  - "Send a test reminder", then "Don't allow"; again, then "Don't allow".
  - The third tap showed no dialog, only "Notifications are not allowed. Everything else still works."
  - Nothing was posted. Inventory (0 of 0) and Add batch opened.
- **Clean-up:**
  - Both packages were uninstalled from the emulator.
  - Rotation (1/0), font scale (1.0), and airplane mode (off) were checked.
  - The worktree and every database copy were deleted, and the emulator was shut down.

## 6. For you (the student)

### 6.1 Your rule files are missing
Add `app/src/main/java/com/example/readyshelf/domain/DatePolicy.java` and `RotationPlanner.java` and their tests in `app/src/test/java/com/example/readyshelf/domain/`. Then ask for this review again. Your tests will run, and any failing case will be explained with a hint (your code is never rewritten).

### 6.2 Rule test cases still to write (kinds of cases, in words)
Use a fixed "today" in every test, and name each test after what it shows.

**DatePolicy** (SPEC, "DatePolicy requirements"):
- **Not reviewed yet:** no date at all, only printed text such as "03/2027", and a complete date that is not confirmed should all be NEEDS_REVIEW, even when the date is in the past.
- **Around today:** a confirmed date yesterday, today, and tomorrow. Which status should each get?
- **The near-date edge:** a date exactly N days away and one N+1 days away, for N = 30 (the default) and for a different N, to show the number is not fixed in your code.
- **Calendar counting:**
  - from 28 Feb 2028 to 29 Feb 2028
  - from 29 Feb 2028 to 1 Mar 2028
  - 30 days after 31 Jan 2028
  - a date across New Year
  - a date across a clock change (for example 8 Mar 2026 in the United States)
  - What should each count be if you count calendar days, not hours?
- **Label types:** the same date with Best by, Use by, Expires, and Other. Should the status differ? Write down your decision and test it.

**RotationPlanner** (SPEC, "RotationPlanner requirements"):
- **The 6/2 example:** a kit batch of 6 near its date and one pantry batch of 2 that qualifies. What should the proposal and the note say?
- **Two kits and one pantry batch of 3:** the units go to the earliest kit date first (then the lower batch ID), and never more than 3 in total.
- **Not the same product:** the same name with another variant, package size, or unit means no swap.
- **Dates:**
  - A missing, partial, or unconfirmed date, or an unknown label, on either batch means no swap.
  - Kit food dated today or yesterday: no swap, and a note that it needs replacement.
  - The edges: a kit date at N and at N+1 days, and a pantry date at R and at R−1 days from today.
  - The pantry date must be later than the kit date.
- **What can't be swapped:** medicine, water, first aid supplies, and other categories. A damaged, unsealed, or storage-concern batch, or one whose answers are "Not checked" (on either side). Infant formula, perishable food, and unclassified products.
- **Order and inputs:**
  - The sort order: sources by date, then batch ID; replacements by earliest qualifying date, then batch ID.
  - Changed settings (for example 14 and 90 days) change the results.
  - The list you are given is not changed.

### 6.3 STUDENT-WORK.md notes that differ from SPEC
The file is yours; nothing in it was changed. Questions to think about:
- **Lines 17–23 (Statuses):**
  - The table uses NO_DATE, NOT_DUE, REVIEW_SOON, and DATE_PASSED, with 7- and 14-day windows.
  - SPEC (from your guide) uses NEEDS_REVIEW, PAST_DATE, TODAY, NEAR_DATE, and LATER, with a near-date setting that starts at 30.
  - Which list will your code use?
- **Line 23:** "DATE_PASSED when the expiration date > today's date". Picture a date on a calendar: is a passed date before or after today?
- **Line 39:** "Date_Passed when … date = today". SPEC gives a date equal to today its own status, TODAY. How does your table show that day?
- **Lines 26–33 (Worked examples):** "Today: ?" and the settings numbers are still blank. Each example needs a fixed today to become a test.

## 7. Phone checklist (you run these on your phone)
- **Note added on 2026-10-12:**
  - "Load sample data" was replaced by "Open sample data". It keeps the made-up items in a separate file (SPEC.md, "Sample data"; DEMO.md).
  - For this checklist, use Settings → Open sample data instead, and Settings → Close sample data when you finish.
  - Locations added while sample data is open are deleted when it closes.
  - The rest of this section is unchanged.
- **Note added on 2026-10-12 (photo entry):** rows A4, D5, and D6 describe the old "Scan a label", which photo entry replaced. Use section 9.5 for photo entry instead.
- **Note added on 2026-10-22:** Settings has no Sample data card any more (section 35), so this checklist can't use sample data. The "Remove sample data" dialog of rows B4 and E5 no longer exists (B4's "New location" dialog still does). Steps that save something now change your own records: skip them on the phone, or run them on the emulator.
- **Note added later on 2026-10-22 (the simpler batch form, section 37):** the food section has one tick in place of the condition questions. Row C4 now means that tick: it is read with its words and "checked" or "not checked", and on a batch saved before with other answers, "Saved answers: …" under it is read too. In row E1, "answers" are the tick, plus the label type chips after **Change** and the "What is printed?" chips after **No full date on the package?**; both stay open after a rotation.
- **Note added in the evening of 2026-10-22 (no food section, section 38):** the form has no food section any more, so row C4 no longer applies. In row E1, "answers" are the label type chips after **Change** and the "What is printed?" chips after **No full date on the package?**.
- **Note added on 2026-10-23 (section 44):** Settings' "Send a test reminder" is now **Show reminder now**. In rows A6, C5, C8, D1, F5, F6, and F7, tap that button instead. It shows the real reminder, "Household items need review" with up to two of your items, not a "Test reminder". When nothing is near date or passed, it shows only a message.
- **Before you start:**
  - Use sample data only (Settings → Load sample data).
  - Don't add test locations, because locations can't be deleted yet.
  - Remove the sample data when you finish.
- **Writing results:** write what you really saw in "Your result". Write "not run" for anything you skip.

### A. Airplane mode (Wi-Fi and mobile data off)
| # | Step | Expected | Your result |
|---|---|---|---|
| A1 | Turn on airplane mode, then open ReadyShelf | Home opens | |
| A2 | Add a batch, edit it, and move part of it | Each review and save works | |
| A3 | Inventory: search, filters, and date order | The list changes as chosen | |
| A4 | Add batch → Scan a label → Choose a photo of a label | The photo is read on the phone | |
| A5 | Purchase list and Settings | Both open | |
| A6 | Settings → Send a test reminder | The test notification appears | |
| A7 | Anywhere above | No message about the internet or a network | |

### B. Large text
| # | Step | Expected | Your result |
|---|---|---|---|
| B1 | Phone Settings → Display: font size and display size at their largest | — | |
| B2 | Open every screen and scroll to the end | Text wraps, nothing is cut off, every button can be reached | |
| B3 | Inventory rows, the batch form's review, a stock-change review, the Settings reminders section | Everything can be read | |
| B4 | Open a dialog (Remove sample data, New location) | The dialog's buttons can be reached | |
| B5 | Set both sizes back to what you use | — | |

### C. Screen reader (TalkBack)
Turn it on in phone Settings → Accessibility → TalkBack. Practice the gestures first: swipe right for next, double-tap to activate.

| # | Step | Expected | Your result |
|---|---|---|---|
| C1 | Home: swipe through | Section titles are read as headings | |
| C2 | Add batch: move through every field | Every field's label is read | |
| C3 | Save with empty fields | Each error is read | |
| C4 | A food batch's condition questions | Answers are read with their question, for example "Sealed: Yes" | |
| C5 | Settings: Send a test reminder, then go back to the status line | The notifications status line is read | |
| C6 | The "Not available yet" notes | They are read | |
| C7 | Inventory: double-tap a row | Batch details opens | |
| C8 | Pull down the notification shade | The test reminder's title and text are read | |
| C9 | Turn TalkBack off | — | |

### D. Denied permissions
| # | Step | Expected | Your result |
|---|---|---|---|
| D1 | Settings → Send a test reminder → "Don't allow" | Short message: notifications are not allowed | |
| D2 | Tap it again → "Don't allow" | The same message | |
| D3 | Tap it a third time | No question; only the message | |
| D4 | Home, Inventory, Add batch, Purchase list | All work | |
| D5 | Add batch → Take a photo of the label | The camera app opens. ReadyShelf asks for no permission (the camera app may ask for its own) | |
| D6 | Add batch → Choose a photo | The photo picker opens. ReadyShelf asks for no permission | |

### E. Screen rotation (turn auto-rotate on)
| # | Step | Expected | Your result |
|---|---|---|---|
| E1 | Add batch: type a name, unit, quantity, and choose answers; rotate twice | Everything typed and chosen stays | |
| E2 | The batch form's review; rotate | The review stays | |
| E3 | Record used → Some → amount → Review; rotate | The review stays | |
| E4 | Settings: type planning numbers without saving; rotate | The typed numbers stay | |
| E5 | Open the Remove sample data dialog; rotate | The dialog closes; nothing changes; no crash | |
| E6 | The notification question (D1); rotate while it shows | No crash; the answer is still used | |

### F. Notification settings
| # | Step | Expected | Your result |
|---|---|---|---|
| F1 | Settings → Open notification settings | Android's notification page for ReadyShelf opens | |
| F2 | Turn ReadyShelf's notifications off, go back | The status line says they are not allowed | |
| F3 | Turn them on again, go back | The status line says they are allowed | |
| F4 | Turn off only the "Review reminders" category | The status line says they are not allowed | |
| F5 | Set a screen lock, send a test reminder, lock the phone | Only generic words, or nothing, on the lock screen | |
| F6 | Tap the test reminder | Home opens at "Needs review"; Back returns | |
| F7 | Turn on Do Not Disturb, send a test reminder | Note what happens | |
| F8 | Set everything back to how you use your phone | — | |

## 8. Not run, and why
- **Every DatePolicy and RotationPlanner rule check:** the files are missing.
- **A real daily reminder, the reminder switch's permission flow, swap cards, the Review swap screen with a real swap, needs, suggestions, and statuses on screens:** they need your rules.
- **Anything on your phone**, including the whole checklist in section 7.
- **TalkBack and dark theme** (emulator or phone).
- **On the emulator:**
  - rotation of Settings with typed numbers (the process-death check in 4.3 passed)
  - the "no settings page" dialog (the emulator has the page)
- **Repair evidence:**
  - R3 couldn't be seen working in the app before or after, because DatePolicy is missing (the unit test covers it).
  - R4's crash before the fix was found by code review, not reproduced.
- **A release build.**

## 9. Photo entry (2026-10-12)
- **Code:** the photo-entry changes on top of commit de720b5. They were tested as described here, then committed on 2026-10-12 at the student's request, as the next commit on main. AI-USE.md lists every file.
- **Who ran what:** Claude Code ran everything in 9.1 to 9.3 on the computer and the Pixel_10a emulator. The household phone was not used. You run 9.5 on your phone.
- **Your date-candidate parser is not written yet**, so no possible dates could be shown on screen. The app's side was checked with preset answers (fake parsers that never look at the text), as with the other rules.

### 9.1 Build and tests
The commands and full results are in TOOLCHAIN.md, "Verified build: photo entry".
- Build, test APK, unit tests, and lint with every task run again: BUILD SUCCESSFUL, no javac warnings or notes.
- Unit tests: 197, 0 failures.
  - New: `DateCandidateTest` 11, `PhotoDateChoicesTest` 17, `PhotoEntryResultTest` 7, `LabelPhotoFilesTest` 6.
  - `PrintedMonthReaderTest` 15 (it was `LabelDateFinderTest`, 32).
  - `LabelReadingTest` (8) was removed with its class.
- Lint: 0 errors, the same 3 warnings as before.
- Instrumented tests on the emulator: OK (96 tests), 78 before.
  - New: `LabelPhotoReaderTest` 6 and `BatchEditPhotoResultTest` 12.
  - With airplane mode on, the two new classes again gave OK (18 tests).
- APK:
  - CAMERA added; still no INTERNET or ACCESS_NETWORK_STATE.
  - A camera is not required.
  - No FileProvider.
  - Every 64-bit native library is 16 KB aligned.

### 9.2 Synthetic label pictures read on the emulator
These are drawn pictures (java.awt), not photos of real labels. They are in `app/src/androidTest/assets/labels/`. The text is what Tesseract returned (`LabelPhotoReaderTest`, logcat):

| Picture | Lines read | Time |
|---|---|---|
| Clear: NATURE VALLEY / CRUNCHY GRANOLA BARS / BEST BY 10/25/2026 | the same three lines | 606 ms (first reading, includes copying the language data) |
| Several dates: USE BY 11/20/2026 / BEST BY 10/25/2026 | the same two lines | 164 ms |
| Two readings: SPRING WATER / EXP 03/04/2027 | the same two lines | 345 ms |
| No date: NUTRITION FACTS / CALORIES 190 | the same two lines | 182 ms |
| Dark (gray text on near-black) | the same three lines as "Clear" | 308 ms |
| Blurry (25-pixel box blur of "Clear") | `MATURE WALLEY`, `CRC IT CM AMOL A BARS`, `SES' GY tooeonee`: the date is unreadable | 281 ms |

- Every temporary picture was deleted after reading (checked by the test).
- Blurry text reaches the review as it is, so the household sees that the photo must be taken again. Whether a parser makes no candidate from such text is for your parser's tests.

### 9.3 By hand on the emulator (Android 17, not the phone)
Airplane mode was on for all of these ("Active default network: none").
- **The form:**
  - Add batch shows "Label photos (optional)" and "Take label photos" above the manual fields.
  - The Edit form of a sample batch has no photo section.
- **Camera permission:**
  - The first opening showed Android's question. After "Don't allow": the explanation, "Ask again", "Open app settings", "Choose a saved photo", "Skip this photo", and "Back to the form", and no camera view.
  - "Ask again", then "Don't allow" again: no "Ask again", and the text "Android won't ask again. To use the camera, open the app settings and allow Camera under Permissions."
  - "Back to the form" returned to the unchanged form.
- **Saved photos (no camera permission needed):** two synthetic pictures were put in Download and chosen with the photo picker.
  - The screen went to Step 2 while the first picture was read.
  - The review showed both texts, the product note, and "Not available yet: possible dates need the date-candidate rules (DateCandidateParser). …".
  - Rotating to landscape kept the review.
  - "Continue without a date": the form said "No date was chosen from the photos. Enter the date below. Category set to Water (found “SPRING WATER”). …", with the name buttons "Use “Nature Valley” …" and "Use “Crunchy Granola Bars” …" (from the product picture only).
  - The private photo folder stayed empty: picked photos are never copied.
- **The camera (after `pm grant`):**
  - The CameraX preview showed the emulator's virtual room.
  - Tapping it showed "In focus.", and the line went away after CameraX's 5-second auto-cancel.
  - "Turn on the light" was hidden (the emulator camera has no flash).
  - Both photos were taken (CameraX logged `onImageCaptured`), read, and deleted.
  - On the review, `dumpsys media.camera` listed no active camera client.
  - The room has no text, so the review showed random characters, as read.
- **Android closing the app:** on the review, Home, then `am kill`, then reopening: a new process showed the same review text.
- **Rotation:**
  - The landscape layout put the camera view and the controls side by side.
  - With the emulator's own rotation (`emu rotate`, ROTATION_270), a photo was taken and read.
  - Whether a sideways photo is turned upright could not be seen, because the room has no text: see 9.4.
- **Leaving without a result:**
  - Cancel on the review, and Back during a camera step, both returned to the form unchanged.
  - The folder was empty, and no camera client was open.
- **Font scale 2.0:**
  - Every control can be reached; "Take photo" needs one short scroll of the controls, a possible follow-up in SPEC.md.
- **Manual entry, in sample data:** a new product "Tea", 2 boxes, "No date printed".
  - The review read as before, with no photo sentence.
  - "Confirm and save" saved it ("Showing 6 of 6 batches").
- **Crash log:** no ReadyShelf lines. The only crashes were the emulator's UWB hardware service, as on earlier days.
- **Clean-up:**
  - Sample data closed.
  - The two pictures deleted from Download and the media index.
  - Airplane mode off.
  - Rotation (1 and 0) and font scale (1.0) as before.

### 9.4 Not run, and why
- **Possible dates on screen:** your DateCandidateParser is not written.
  - The choice logic was tested with preset answers: `PhotoDateChoicesTest`, and `BatchEditPhotoResultTest` for how the form puts in a tapped date.
  - Run 9.5's date rows again when your parser is connected.
- **Photos of real labels**, blurry or dim real photos, and a photo of a label held sideways (whether the text comes out upright): they need a real camera and real packages (9.5).
- **"Record bought item" on screen** (it should have no photo section): it needs a purchase-list item, which needs RotationPlanner. The form's code hides the section for it, and `BatchEditPhotoResultTest` checks that nothing is put in.
- **TalkBack and dark theme.**
- **A release build.**
- **The phone checklist below.** At your request, the final debug build was installed on your phone on 2026-10-12 at 12:59:57:
  - `adb install -r` gave "Success". It was an update, so your data was kept: the first install time stayed 2026-10-04 13:26:35. The database stays at version 3.
  - CAMERA is not granted until you allow it.
  - Nothing else was done on the phone.

### 9.5 Phone checklist: photo entry (you run these on your phone)
- Before you start: Settings → Open sample data, so nothing goes into your own records. Then turn on airplane mode.
- Write what you really see in "Your result", and "not run" for anything you skip.
- If you want, Claude can check over adb, during and after the checks, that the private photo folder is empty and that ReadyShelf logged no crash.

| # | Step | Expected | Your result |
|---|---|---|---|
| G1 | Add batch → Take label photos → "Don't allow" | The explanation, "Ask again", and "Back to the form" | |
| G2 | "Ask again" → "Don't allow" | No "Ask again"; "Open app settings" | |
| G3 | "Back to the form" | The form is unchanged | |
| G4 | Take label photos → "Open app settings" → allow Camera → back | The camera view appears | |
| G5 | Product photo of a real package: tap the name to focus, then "Take photo" | "In focus.", then Step 2 | |
| G6 | Close photo of a clear printed date → review | The date photo's text matches the package | |
| G7 | Take the date photo again while moving the phone (blurry) | The text is unclear; nothing is filled in by itself | |
| G8 | A date that can be read two ways (like 03/04/27), if you have one | The text shows it as printed | |
| G9 | A panel with no date | Text, and no date | |
| G10 | A package with several dates or a lot code | All of the text shows | |
| G11 | Dim light: is "Turn on the light" shown? Turn it on and take the date photo again | The light works; compare the text | |
| G12 | Hold the phone sideways for a date photo | The text reads normally (not sideways) | |
| G13 | Rotate the phone on a step and on the review | Nothing is lost | |
| G14 | Cancel on the review | The form is unchanged | |
| G15 | Continue without a date → tap a name button → type the date → Review and save → Confirm | The batch is saved; its history has no photo sentence | |
| G16 | Anywhere above | No message about the internet or a network | |
| G17 | Settings → Close sample data; turn airplane mode off | — | |

- **When your parser is connected,** repeat G6 to G10. Check the date buttons:
  - nothing is chosen by itself
  - a date like 03/04/27 gets three buttons
  - month-and-year text gets no day
  - a lot code is not offered as a date
  - a tapped date is unconfirmed on the form, and its history says "Date read from a label photo: …"

## 10. Status labels and batch cards (2026-10-18)
- **Code:** uncommitted changes on top of commit `9e4a81d`; AI-USE.md lists every file. Claude Code ran everything below on the computer and the Pixel_10a emulator (Android 17). The household phone was not used.

### 10.1 Build and tests
- `assembleDebug testDebugUnitTest`: BUILD SUCCESSFUL. Unit tests: 296, 0 failures (the report files were written at 18:12 that day).
- `lintDebug` (report written at 18:19:43, after the last layout change): 0 errors, the same 3 warnings as before.
  - The first lint run also warned that the category icon in `item_batch.xml` has no spoken label; BatchAdapter sets it in code, so the layout now tells lint so (`tools:ignore`).
- Instrumented tests on the emulator: OK (110 tests).

### 10.2 By hand on the emulator
- **Home and Inventory** (the emulator's own records, then sample data):
  - Every batch is a card with its category icon (fork and knife, medicine bottle).
  - The five labels showed as planned:
    - "Date passed": red, "!"
    - "Date is today": orange, calendar with today marked
    - "Near date": yellow, clock
    - "Date needs review": outline, question mark
    - "Later date": gray, plain calendar
- **Batch details:** the red "Date passed" label. An archived sample batch showed the outlined "Archived" tag with its icon (different from "Sample item") and the sentence "Status: none, because this batch is archived. …".
- **Spoken labels** (read from `uiautomator dump`, not with TalkBack itself): the category icon "Food", the label "Status: Date is today".
- **Dark mode:** every label was readable, and orange and yellow looked different from each other.
- **Font scale 2.0:** the card text wrapped, the label icons grew with the text, and nothing was cut off.
- **Contrast**, computed with the WCAG formula: orange 13.24:1 (light) and 7.22:1 (dark), yellow 13.52:1 and 7.37:1. The other colors come from the Material 3 theme.
- **Clean-up:**
  - Sample data closed (one sample batch had been archived for the check).
  - Font scale back to 1.0, dark mode off.

### 10.3 Not run, and why
- TalkBack itself, on the emulator or the phone (the spoken labels were read from the screen's accessibility data instead).
- Opening the app on the phone: the builds were installed there at the student's request (10.5), and the student looks at them.
- A release build.

### 10.4 Regular-weight review line, and the DueWise name and icon (later the same day)
- **Build and tests** (`assembleDebug testDebugUnitTest lintDebug`): BUILD SUCCESSFUL.
  - Unit tests: 296, 0 failures.
  - Lint (report written at 18:31:21): 0 errors, the same 3 warnings.
  - Instrumented tests on the emulator: OK (110 tests).
- **By hand on the emulator:**
  - **The review line:** "Date needs review: only part of a date is recorded." is regular weight above the outlined label.
  - **The name:** Home's title says "DueWise", and the app list shows "DueWise" with the hourglass icon.
  - **The start-up screen** (Android 17): in light mode, the hourglass on porcelain; in dark mode, the round icon on pine green.
- **Not checked:**
  - Themed icons (Android 13 and newer; the setting was not turned on).
  - The widget's title with the new name (no widget was placed on the emulator).
  - A notification with the new name.

### 10.5 Phone installs (at the student's request)
- **The builds:**
  - 2026-10-18 18:27:53: status labels, batch cards, and the regular-weight line.
  - 18:34:37: the same plus the DueWise name and icon.
- **Before each install:** ReadyShelf was not running and no screen was open.
- **After each install:**
  - `adb -s ZY22KN9QJP install -r` gave "Success". The first install time stayed 2026-10-04 13:26:35, so it was an update.
  - The listing of `databases`, `files`, and `shared_prefs` (names, sizes, and times) was identical before and after.
  - Sample data is still open.
  - CAMERA and POST_NOTIFICATIONS are still allowed.
  - The daily-check job is still scheduled.
  - The crash log had no ReadyShelf line.
- Claude Code did not open the app on the phone.

### 10.6 "Duewise", the logo colors, status tiles, and the swap picture (later the same day)
- **Build and tests:**
  - `assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug`: BUILD SUCCESSFUL.
  - Unit tests: 296, 0 failures (results written at 18:57:29).
  - Lint (report written at 18:57:32): 0 errors, 1 warning, the old `TooManyViews` on the batch form. The two old warnings about unused template colors are gone, because those colors were removed with the new color scheme.
  - Instrumented tests on the emulator: OK (110 tests).
- **By hand on the emulator** (the emulator's own records):
  - **Home:** the title "Duewise", pine filled buttons, and light green tonal buttons. The "Items need review" box showed four tiles, two in a row:
    - red "1 Date passed"
    - orange "1 Date is today"
    - yellow "2 Near date"
    - outlined "1 Date needs review"
  - **Swap card:** "Earthquake kit → Pantry" with "6 bars · Best by: 7 Nov 2026 · for review", and "Pantry → Earthquake kit" with "6 bars · Best by: 15 Jun 2027", each place with its icon.
  - **Review swap:** the same picture under "The two moves". Under "After the swap", the table read Earthquake kit 6 bars → 6 bars and Pantry 9 bars → 9 bars, with the detail lines below it.
  - **Spoken labels** (read with `uiautomator`, not with TalkBack):
    - each tile, for example "Near date: 2 batches"
    - the swap picture: the two full sentences "Move to pantry for review: …" and "Into Earthquake kit in its place: …"
    - the table: "Granola bars in Earthquake kit: 6 bars before, 6 bars after. Granola bars in Pantry: 9 bars before, 9 bars after."
  - **Dark mode:** the tiles, the buttons, and the swap card were readable.
  - **Font scale 2.0:**
    - The tiles' words wrapped inside the tiles.
    - At first the swap picture split "Earthquake" inside the word ("Earth / quake"). Fixed: with large text each move's two places now go under each other with a ↓ arrow, and the word stayed whole on the next check.
    - At font scale 1.0 the two places are side by side again (checked by their positions on screen).
  - **Clean-up:** font scale 1.0, dark mode off. No swap was confirmed.
- **Not checked:**
  - TalkBack itself
  - the widget, which still has its old colors
  - The rest of the screens with the new colors. Only the top of Add batch and of Settings were looked at; their buttons, radio buttons, and switch were pine and readable. Inventory, Purchase list, Batch details, Change stock, and Label photos use the same theme but were not looked at after the change.
  - the phone, which still has the earlier build (the name spelled "DueWise")

### 10.7 The blue color scheme, in place of the logo's pine (later the same day)
- **Why:** the student didn't like the pine scheme and asked for the most appealing scheme by best practice, ignoring the logo. The new scheme is blue (seed #1A73E8, primary #005BC0 in light mode), made with the same Material color tools. The start-up screen now uses the app's own background color.
- **Build and tests:**
  - `assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug`: BUILD SUCCESSFUL.
  - Unit tests: 296, 0 failures.
  - Lint (report written at 19:06:37): 0 errors, 1 warning, the old `TooManyViews`.
  - Instrumented tests on the emulator: OK (110 tests).
- **By hand on the emulator:**
  - **Light mode:** Home's blue filled buttons, light blue-gray tonal buttons, a near-white background, and the status tiles, and the swap card's light blue-gray places.
  - **Dark mode:** Home with light blue filled buttons on a near-black background.
  - **The start-up screen:** in light mode the hourglass on near-white; in dark mode the round porcelain icon with the hourglass on near-black.
- **Not checked:** font scale 2.0 with the blue colors (only colors changed, not sizes), TalkBack itself, the widget (still its old colors), and the phone.

### 10.8 Always dark, and the new tile look (later the same day)
- **Why:** the student preferred the dark version ("Dark mode is better. Make the changes.") and asked for a different card scheme for the tiles.
- **What changed:**
  - The theme is always Material 3 Dark, with the blue scheme's dark colors, and the system bar icons are always light.
  - Each tile is a dark card: the status icon in a circle of the status's dark color, then the count in its bright color, then the word.
  - Status labels now use a dark color with light words for every status. "Date needs review" became blue instead of outline-only, and "Date passed" became dark red instead of bright red.
- **Contrast**, computed with the WCAG formula:
  - Label words on their colors: 7.20 to 7.24:1.
  - Tile numbers on the tile card: 8.40 to 8.45:1.
  - Tile words: 11.13:1.
  - Icons on their circles: about 5.5:1.
- **Build and tests:**
  - `assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug`: BUILD SUCCESSFUL.
  - Unit tests: 296, 0 failures.
  - Instrumented tests on the emulator: OK (110 tests).
  - Lint (report written at 19:19:54, after one layout note was added): 0 errors, 1 warning, the old `TooManyViews`. The first run also suggested merging the tile's icon and number into one view (`UseCompoundDrawables`). That would lose the icon's colored circle, so the layout tells lint it doesn't apply.
- **By hand on the emulator, with the emulator itself in light mode:**
  - Home, Inventory, the swap card, and the start-up screen were all dark (round icon on near-black), with light status bar icons.
  - The tiles showed red, orange, gold, and blue icons and numbers.
  - Inventory showed all five labels: orange, gold, gray, blue, and dark red (on Home).
  - Add batch's date picker (Android's own dialog) was dark too, with a gray header. It was closed with Cancel.
  - At font scale 2.0 the tile words wrapped inside the tiles.
  - **Clean-up:** font scale 1.0.
- **Not checked:** TalkBack itself, Settings and Purchase list after the change, and the phone.

### 10.9 Steps 6 and 7: Inventory filter chips, message kinds, and "More about this" (later the same day)
- **Build and tests:**
  - `assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug`: BUILD SUCCESSFUL.
  - Unit tests: 296, 0 failures.
  - Lint (report written at 19:51:58, after the last fix): 0 errors, 1 warning, the old `TooManyViews`.
  - Instrumented tests on the emulator: OK (110 tests).
- **The emulator was restarted (cold boot) halfway through.** It had become very slow: about 400% system CPU, and its own sensors service crashed every few seconds, as on earlier days.
  - Screens took 2.5 to 8 seconds to open, Settings (unchanged) as slowly as Inventory, and some screenshots came out blank.
  - Duewise logged no crash.
- **Inventory, by hand on the emulator:**
  - **Chips:** location ("All locations", Pantry, Earthquake kit, First aid box) and category, wrapping onto more lines. A check mark shows on the chosen chip.
  - **Filtering:** "Showing 6 of 6 batches". Earthquake kit gave "Showing 2 of 6 batches", Food left it at 2, and "Latest first" put "Best by: 7 Nov 2026" first.
  - **Spoken:** `uiautomator` reported the chips and the two switch halves as radio buttons, checked or not.
  - **Rebuilding the screen** (a configuration change): the choices and "Showing 2 of 6 batches" stayed.
  - **Font scale 2.0:** the chips wrapped.
    - The switch cut its words off: first "Soonest d…", then, with shorter words and two lines allowed, "Soonest fi…". Buttons in this Material toggle group stay on one line.
    - Fixed: with large text the two buttons go under each other at full width (`LargeText`, the same rule as the swap picture).
    - Checked after the emulator restart: at font scale 2.0, "Soonest first" and "Latest first" were whole, one under the other. At 1.0 they were side by side again.
- **Messages, by hand on the emulator:**
  - **A warning:** the kit warning on Record used (granola bars in Earthquake kit, "All (6 bars)", Review) was a dark gold box with a warning sign. "Back" was tapped, not "Confirm": the batch still had 6 bars.
  - **Notes** are rounded boxes, for example the "For review" note.
  - **Error boxes were not seen on screen.** They need a failed save or a rules error.
  - **The batch form's "Date needs review: no date is recorded."** first became a gold warning. That showed on every new form and clashed with the blue "Date needs review" status, so it was changed back to a note (checked on screen).
- **"More about this", by hand on the emulator:**
  - **Settings → Reminders:** the summary, then "More about this". Tapping it showed the three longer texts, and the button said "Show less". After the screen was rebuilt it was still open, with the first text shown under the button.
  - **Review swap:** the one-line summary and "More about this" were in view, as was "The replacement days are a planning choice, not a safety guarantee.". Not expanded on screen.
  - **The batch form's Food condition:** the records note in view, and "More about this" under it. Not expanded on screen.
- **Not checked:**
  - TalkBack itself (the "Collapsed / Expanded" words were not heard)
  - an error box on screen
  - the phone
- **Clean-up:** font scale 1.0, and dark-mode setting off. No data was changed on the emulator.

### 10.10 Phone install of the whole UI plan (at the student's request)
- **The build:** everything in 10.1 to 10.9. The student asked "Install and commit", so it was installed first and then committed on `main` as the next commit after `9e4a81d`.
- **Right before the install:** `assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug` ran again: BUILD SUCCESSFUL, 79 of 80 tasks up to date.
  - Unit tests: 296, 0 failures. Lint: 0 errors, 1 warning, the old `TooManyViews`.
  - The APK (built at 19:51:15) and the lint report (19:51:58) were not rewritten, so the installed APK is the one checked in 10.9.
- **Before the install:** Duewise was not running, and the phone's screen was off.
- **After the install** (2026-10-18 20:07:23):
  - `adb -s ZY22KN9QJP install -r` gave "Success". `lastUpdateTime` changed from 18:34:37 to 20:07:23, and the first install time stayed 2026-10-04 13:26:35, so it was an update.
  - The listing of `databases`, `files`, and `shared_prefs` (names, sizes, and times) was identical before and after. `files/pre-upgrade-2026-10-12` is still there.
  - Sample data is still open (`sample_data_open` true), so the sample items the student wants to keep are still there.
  - CAMERA and POST_NOTIFICATIONS are still allowed.
  - The daily-check job is still scheduled, with its earliest start about 09:00 on 2026-10-19.
  - The crash log was empty.
- **Not run:** Claude Code did not open the app on the phone. The new look, the name, and the icon on the phone are the student's check, as are TalkBack and large text there.

### 10.11 Home's status tiles open Inventory, filtered by status (later the same day)
- **Why:** the student asked that a tap on a tile "act as a filter and show me only the items that are relevant for that tile". The student then chose:
  - Inventory with Status chips
  - a small arrow on each tile
  - keeping "Show the review list"
- **Code:** uncommitted changes on top of commit `7719052`.
- **Build and tests:**
  - `assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug`: BUILD SUCCESSFUL.
  - Unit tests: 300, 0 failures. That is 296 plus 4 new status-filter tests in `InventoryFilterTest`, which now has 13.
  - Lint (report written at 20:38:57, after the last code change): 0 errors, 1 warning, the old `TooManyViews`.
  - Instrumented tests on the emulator: OK (110 tests).
- **By hand on the emulator** (its own records; Home's tiles showed Date passed 1, Date is today 1, Near date 2, Date needs review 1):
  - **Home:** each tile has a small ">" arrow at the top right. In `uiautomator`, each tile is clickable, and its spoken label is unchanged, for example "Near date: 2 batches".
  - **Each tile opened Inventory with its own Status chip checked, and the count matched the tile:**

    | Tile | Inventory said | The rows and their labels |
    |---|---|---|
    | Date passed (1) | "Showing 1 of 6 batches" | Pain reliever tablets: "Date passed" |
    | Date is today (1) | "Showing 1 of 6 batches" | Beans: "Date is today" |
    | Near date (2) | "Showing 2 of 6 batches" | Canned beans and Granola bars: "Near date" |
    | Date needs review (1) | "Showing 1 of 6 batches" | Granola bars: "Date needs review" |

  - **"All statuses"** showed "Showing 6 of 6 batches".
  - **Rebuilding the screen:**
    - Inventory was opened from the "Date needs review" tile, and then "All statuses" was chosen.
    - A font change to 2.0 rebuilt the screen; its chips came back larger.
    - "All statuses" was still chosen. The tile's status did not come back.
  - **Back** (the screen's back arrow) returned to Home each time. Home's own Inventory button opened with "All statuses" chosen.
  - **A press** on a tile, held with `input motionevent`, lightened only that tile, inside its rounded corners. On release, Inventory opened.
  - **Font scale 2.0:** the arrows fit next to the counts, and the longer words wrapped inside the tiles ("Date is / today"). Inventory's Status chips wrapped onto four lines, with whole words.
  - **A first screenshot looked lighter than the app's colors.**
    - The cause: after the instrumented tests the emulator was out of touch mode (`mInTouchMode=false`), so Android drew its keyboard-focus highlight over Home's scroll area.
    - One tap on the title brought touch mode back, and the colors were the theme's again: #121316 background, #1F1F23 panel, #292A2D tile.
    - Not an app change.
  - **Clean-up:**
    - font scale 1.0
    - night mode, which was switched on and off twice during the check, is off again as before
    - Home open, and no crash line for Duewise
- **Not checked:**
  - TalkBack itself. The words "Double-tap to show these batches in Inventory" are set in code but were not heard.
  - Inventory without the date rules, or after a rules error; the Status chips are hidden then.
  - The phone.

### 10.12 Phone install of the tile tap (at the student's request)
- **The build:** everything in 10.11. The student said "It looks good. Install it on the phone And commit", so it was installed first and then committed on `main` as the next commit after `7719052`.
- **Right before the install:** `assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug` ran again: BUILD SUCCESSFUL, 79 of 80 tasks up to date.
  - Unit tests: 300, 0 failures. Lint: 0 errors, 1 warning, the old `TooManyViews`.
  - The APK (built at 20:38:54, after the last code change at 20:38:37) was not rewritten, so the installed APK is the one checked on the emulator in 10.11.
- **Before the install:** Duewise was not running, and the phone's screen was off.
- **After the install** (2026-10-18 21:29:49):
  - `adb -s ZY22KN9QJP install -r` gave "Success". `lastUpdateTime` changed from 20:07:23 to 21:29:49, and the first install time stayed 2026-10-04 13:26:35, so it was an update.
  - The listing of `databases`, `files`, and `shared_prefs` (names, sizes, and times) was identical before and after, and also identical to the listing after the 20:07 install.
  - Sample data is still open (`sample_data_open` true).
  - CAMERA and POST_NOTIFICATIONS are still allowed.
  - The daily-check job is still scheduled, with its earliest start about 09:00 on 2026-10-19.
  - The crash log was empty.
- **Not run:** Claude Code did not open the app on the phone. Tapping the tiles there is the student's check.

### 10.13 The logo at the top of Home (later the same day)
- **Why:** the student asked: "at the top where "Duewise" is on the home screen , display the logo as well". The student then chose:
  - the full logo (the hourglass and the name)
  - the kit's colors for dark backgrounds
  - a size that fits the bar
- **Code:** uncommitted changes on top of commit `c555302`.
- **The vector:** a script converted the kit's `duewise-horizontal-reversed.svg` (5 paths) into `logo_duewise_horizontal.xml`. All 5 paths' data was checked to be in the vector unchanged.
- **Build and tests:**
  - `assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug`: BUILD SUCCESSFUL.
  - Unit tests: 300, 0 failures.
  - Lint (report written at 21:43:54, after the change): 0 errors, 1 warning, the old `TooManyViews`. The long path of the logo's letters is marked for lint's long-path check (VectorPath), so it adds no warning.
  - Instrumented tests on the emulator: OK (110 tests).
- **By hand on the emulator:**
  - **Size and place:** Home's top bar shows the logo at 142 × 32 dp (373 × 84 px at 420 dpi), starting 16 dp from the left like the old title. The bar is still 64 dp, and nothing below it moved.
  - **Zoomed in:** the logo is sharp, with the hourglass, its brass sand, the thin stem, and the italic name.
  - **Colors:** the logo's pixels are exactly the kit's colors, #EEF2F0 and #B8914F, on the app's #121316.
  - **Spoken label:** in `uiautomator`, the bar holds one image with the spoken label "Duewise", and no title text.
- **The phone's screen:** 280 dpi (read with `wm density`). So the logo will be 56 px tall there, and the hourglass about 50 px, above the kit's 48 px limit for this version.
- **Not checked:**
  - TalkBack itself
  - font scale 2.0 (the logo is an image of a fixed size, so it doesn't grow with text)
  - Home with sample data open (the "Sample data" strip goes under the bar; that code didn't change)
  - the phone (not installed)

### 10.14 Phone install of the logo (at the student's request)
- **The build:** everything in 10.13. The student said "commit and install". It was installed first, so this record could go into the same commit on `main`, the next commit after `c555302`.
- **Right before the install:** `assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug` ran again: BUILD SUCCESSFUL, 79 of 80 tasks up to date.
  - Unit tests: 300, 0 failures. Lint: 0 errors, 1 warning, the old `TooManyViews`.
  - The APK (built at 21:43:52, after the last change at 21:43:39) was not rewritten, so the installed APK is the one checked on the emulator in 10.13.
- **Before the install:**
  - Duewise was running in the background with only Home open (no form) while the phone's screen was off. The install closed it.
  - Since the 21:29 install, only the sample data's `-shm` file and `profileInstalled` had new times (21:30), with the same sizes. That came from the student opening the app; no data was written.
- **After the install** (2026-10-18 21:49:56):
  - `adb -s ZY22KN9QJP install -r` gave "Success". `lastUpdateTime` changed from 21:29:49 to 21:49:56, and the first install time stayed 2026-10-04 13:26:35, so it was an update.
  - The listing of `databases`, `files`, and `shared_prefs` (names, sizes, and times) was identical before and after.
  - Sample data is still open (`sample_data_open` true).
  - CAMERA and POST_NOTIFICATIONS are still allowed.
  - The daily-check job is still scheduled, with its earliest start about 09:00 on 2026-10-19.
  - The crash log was empty.
- **Not run:** Claude Code did not open the app on the phone. The logo on the phone is the student's check.

## 11. UI round 2 (2026-10-19)
- **The student's picks** (from Claude Code's suggestions):
  - Batch details, and Settings in cards
  - Home's action tiles and a floating "Add batch" button (one row of three tiles plus the floating button, the student's choice)
  - save messages
  - a widget that matches the app (always dark, the student's choice)
  - the hourglass start-up screen
- **Code:** uncommitted changes on top of `2a19fb1`. Claude Code ran everything below on the computer and the Pixel_10a emulator (Android 17). The phone was not used.

### 11.1 Steps 1–4: the widget, the start-up screen, Home's tiles and floating button, and save messages
- **Build and tests, after the last code change:**
  - `assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug --rerun-tasks`: BUILD SUCCESSFUL, all 80 tasks run again, because lint can miss resource-only changes.
  - Unit tests: 300, 0 failures.
  - Lint (report written at 08:00:13): 0 errors and 3 warnings:
    - the old `TooManyViews`
    - `ic_tune` and `ic_dataset` not used yet; they are for step 6, Settings
  - Instrumented tests on the emulator: OK (110 tests), including `ReviewWidgetTest`.
  - **Lint flagged three things along the way, now handled:**
    1. The 288 dp start-up icon is over lint's 200 dp limit for vectors. Android asks for 288 dp, so the file says so (`tools:ignore`).
    2. The widget's hourglass and name could be one text view. They now are.
    3. Lint then suggested AppCompat's drawable attribute, which home-screen widgets can't use. The file says so.
  - The build also prints a Java note that `NoticeLook.java` (from round 1) uses deprecated helpers. It was there before today.
- **By hand on the emulator:**
  - **Start-up screen:** after a force-stop, the hourglass appears on #121316 and is whole; nothing is cut by Android's round frame. Screenshots were taken 250 to 700 ms after the start.
  - **Home:**
    - The row of three tiles (Inventory, Purchase list, Settings) shows icons, and the floating "Add batch" sits at the bottom right. The list scrolls under it.
    - Each tile opened its screen. Home's floating button opened Add batch, and so did Inventory's. Back returned each time.
  - **Save messages** (with sample data, then the emulator's own records):
    - "Sample data is open." on the new Home, after Settings → Open sample data, above the floating button.
    - "Swap recorded." on Home after Confirm swap.
    - "Recorded as used: 3 bars." on Batch details after Record used.
    - "Batch archived." after Archive.
    - "Changes saved." after Edit batch → Confirm and save, with no changes.
    - "Planning settings saved." on Settings.
    - "Sample data is closed. Your own records are shown." on Home, above the floating button.
    - On the emulator's own Purchase list:
      - "Added to your purchase list: 4 cans of Canned beans — Black beans · 15 oz · unit: cans for Earthquake kit.", shown in full on two lines.
      - Then "Removed from your purchase list: …". The list is empty again, as before.
    - **Not checked by hand:** Record discarded, Move, the reminder messages in Settings, the purchase list's "Changed on …", and "Batch added." for a new batch.
  - **Widget:**
    - Placed on the home screen, it is dark (#1F1F23) with the hourglass and "Duewise", and the line "Household items need review". The picker's preview shows the same look.
    - A tap opened Home at "Needs review". Then the widget was removed.
  - **Font scale 2.0:** the three tiles became full-width rows, with the icon beside the word, and nothing was cut off.
    - At first each row was still 88 dp tall. That was lowered so each row fits its text, about 59 dp. Then the font went back to 1.0.
  - **Clean-up:**
    - Sample data was closed, so the test changes made in it went with it.
    - The purchase-list test entry and the widget were removed.
    - Font scale 1.0 and night mode off.
    - No crash line for Duewise.
- **Not checked:** TalkBack itself, and the phone.

### 11.2 Steps 5–6: Batch details and Settings in cards
- **Build and tests, after the last code change:**
  - `assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug --rerun-tasks`: BUILD SUCCESSFUL.
  - Unit tests: 300, 0 failures.
  - Lint (report written at 08:10:23): 0 errors, 1 warning, the old `TooManyViews`. The icons for Settings are used now.
  - **Two lint notes from the first run, now handled:**
    - Batch details' header could be "one text view with an icon". The icon has its own colored circle, so the layout says so (`tools:ignore`), as for Home's status tiles.
    - The timeline row now sets `baselineAligned="false"`, which lint advises for speed.
  - Instrumented tests on the emulator: OK (110 tests).
- **Batch details, by hand on the emulator:**
  - **A food batch** (the emulator's own granola bars in Earthquake kit):
    - The header card has the category icon, name, product facts, and the "Near date" label.
    - The facts card has Quantity, Location, Category, and Best by, each with its icon: a box, a backpack for the kit, a fork and knife, and a calendar.
    - The "Food condition" card has six label-and-answer rows.
    - "Record used" is filled at full width, then "Edit batch | Move" (tonal) and "Record discarded | Archive" (outlined).
    - The history is a timeline: dots joined by a line that stops at the first and the last entry.
  - **A medicine batch** (pain reliever tablets) shows the medicine icon, "Date passed", a first-aid icon for the place, and no food card.
  - **With sample data:**
    - After "Record used" of all 3 bars, the 0-left note showed. "Record used", "Move", and "Record discarded" were turned off; "Edit batch" and "Archive" were on.
    - After Archive, the archived note showed, with only "Edit batch" at full width and no second row. The timeline showed 3 entries.
  - **Font scale 2.0:**
    - All five buttons went under each other at full width, and the food answers went under their labels.
    - The row icons grew with the text, and long lines wrapped with nothing cut off.
- **Settings, by hand on the emulator:**
  - **Planning card:** the tune icon, the intro, both number fields with their full helper text ("A planning choice, not a safety guarantee" included), and "Save planning settings" filled.
  - **Reminders card:**
    - the summary, "More about this", and the "Daily reminder" switch
    - "Notifications from Duewise are allowed." in a small box, and the last reminder
    - "Send a test reminder" (tonal) with its note, and "Open notification settings" as a text button
  - **Sample data card:**
    - Closed: the intro and "Open sample data".
    - Open: the status in a small box, "Start sample data again", "Close sample data", the keep note, and "Keep these items as my own".
- **Clean-up:**
  - Sample data was closed, so the changes made to it went with it.
  - Font scale 1.0 and night mode off.
  - No crash line for Duewise.
- **Not checked:** TalkBack itself, Settings at font scale 2.0 (its cards have no side-by-side parts), and the phone.

### 11.3 Phone install of UI round 2 (at the student's request)
- **The build:** everything in 11.1 and 11.2. The student said "install and commit". It was installed first, so this record could go into the same commit on `main`, the next commit after `2a19fb1`.
- **Right before the install:** `assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug` ran again: BUILD SUCCESSFUL, 79 of 80 tasks up to date.
  - Unit tests: 300, 0 failures. Lint: 0 errors, 1 warning, the old `TooManyViews`.
  - The APK (built at 08:10:18, after the last code change at 08:10:08) was not rewritten, so the installed APK is the one checked in 11.2.
- **Before the install:**
  - Duewise was open on Home, the only screen in its task (no form), with the phone's screen off. The install closed it.
  - Sample data was closed on the phone (`sample_data_open` false). It had been open at the 21:49 install the night before.
  - The data listing showed that at 21:56 that night the household's own `readyshelf.db-wal` grew from 28,872 to 57,712 bytes, the sample data's WAL changed, and `data_mode.xml` changed.
  - That matches "Keep these items as my own", which copies the sample items into the household's records and then closes sample data. The databases themselves were not read.
- **After the install** (2026-10-19 08:40:40):
  - `adb -s ZY22KN9QJP install -r` gave "Success". `lastUpdateTime` changed from 2026-10-18 21:49:56 to 08:40:40, and the first install time stayed 2026-10-04 13:26:35, so it was an update.
  - The listing of `databases`, `files`, and `shared_prefs` (names, sizes, and times) was identical before and after.
  - CAMERA and POST_NOTIFICATIONS are still allowed.
  - The daily-check job is still scheduled, with its earliest start about 09:00 on 2026-10-19.
  - The crash log was empty.
- **Not run:** Claude Code did not open the app on the phone. The new look on the phone is the student's check.

## 12. A streamlined Add batch form and three new categories (2026-10-19)
- **The student's request:** add the categories Documents, Cosmetics, and Subscriptions to Add batch, and make adding a batch easier.
- **The student's answers** to Claude Code's questions:
  - All four suggested changes: the category first, one product box, one-tap choices, and "Save and add another".
  - Documents and subscriptions skip the quantity and the unit.
  - The date's tick box stays.
- **Code:** uncommitted changes on top of `1277d7e`. Claude Code ran everything below on the computer and the Pixel_10a emulator (Android 17). The phone was not used.

### 12.1 Build and tests, after the last code change
- `assembleDebug assembleDebugAndroidTest testDebugUnitTest lintAnalyzeDebug --rerun lintDebug`: BUILD SUCCESSFUL. Lint analyzed again, because the last change was a layout comment.
- Unit tests: 308, 0 failures. 8 are new: `CategoryDefaultsTest` (5) and `CategoryLookTest` (3).
- Lint (report written at 09:40:34): 0 errors, 1 warning, the old `TooManyViews` (the batch form).
- Instrumented tests on the emulator: OK (118 tests). 8 are new, in `BatchEditFormTest`, and the 12 of `BatchEditPhotoResultTest` still pass.
- **The icons:** a script copied the visible paths of each official SVG (spa, description, autorenew, remove) and checked that the vector file's paths are byte-identical to them.
- **The database:** the exported schema (`app/schemas`) didn't change, so it is still version 3. Room's generated code stores the new categories by name ("COSMETIC", "DOCUMENT", "SUBSCRIPTION").

### 12.2 By hand on the emulator, with the emulator's own records
- **The first look found a problem:** the chosen category chip kept its icon and showed no check mark, so the choice was shown by color alone. Fixed: on the chosen category, the check mark takes the icon's place. Checked again ("✓ Documents").
- **Documents:**
  - Typing "Test passport" in the product box offered "Add “Test passport” as a new product". The tap filled in the name.
  - No unit field. "More details (optional)" was folded, and "Choose one of your products" went back to the box.
  - The headings said "Where it is kept" and "Date". The quantity row was replaced by "Saved as 1 document.", and the label type started at "Expires".
  - The date picker gave 14 Jan 2027. The date button then showed "14 Jan 2027", with the spoken label "Date: 14 Jan 2027. Change date" in the screen dump.
  - After the tick right under the date, the "Date needs review" note went away.
  - **The review:** "Product: Test passport — no variant · no package size · unit: document (new product)", "Category: Documents", "Location: Pantry", "Quantity: 1 document", and "Date: Expires: 14 Jan 2027". Then "Confirm and save" (filled), "Save and add another" (tonal), and "Back to the form" (outlined).
- **"Save and add another":** the batch was saved, and an empty form opened with "✓ Documents" and Pantry, showing "Batch added.". The task held only Home and one form.
- **Cosmetics:**
  - The unit choices were tubes, bottles, and jars. Tapping "tubes" filled in the unit.
  - The quantity started at 1, and + twice made 3 ("Quantity (tubes)").
  - The label type was "Expires".
- **Food:**
  - After the switch from Cosmetics, the label type went to "Best by", because it hadn't been chosen by hand.
  - With a new product, the food class chips were on. "Shelf-stable food", "Canned", "Sealed: Yes", and "Damaged: No" were checked.
  - The screen dump had the spoken labels "Food class: Shelf-stable food" and "Sealed: Yes".
- **Errors:** "Review and save" on an empty Subscriptions form showed "Choose a product from the list, or tap “New product”." on the box and "Choose the date, or choose another answer above.", and the screen scrolled to the first one. The form also said "Saved as 1 subscription.".
- **Label photos:**
  - "Take label photos" opened the Label photos screen. Android asked for the camera, which the emulator's Duewise never had.
  - The question was dismissed without an answer, so CAMERA stays not allowed and not set by the user.
  - "Back to the form" returned with Subscriptions and Pantry still chosen.
- **Font scale 2.0:** the chips wrapped onto more lines. Each food question's three answers still fit on one row, the quantity row fit, and nothing was cut off. The screen was made again for the new size, and every choice stayed. Then the font went back to 1.0.
- **Rotation** (`adb emu rotate`, to landscape and back): the choices and the error message stayed.
- **Edit batch** (the test passport): "✓ Documents", the product, the quantity field (a change there is a count correction), "Location: Pantry…", "Expires", the date, and the tick were all loaded.
- **Inventory:** the category chips have Cosmetics, Documents, and Subscriptions before Other. With Documents chosen, it showed 1 of 7 batches: the test passport card, with the page icon, "1 document in Pantry", "Expires: 14 Jan 2027", and "Later date".
- **"Record bought item":**
  - For this check, the emulator's suggestion (4 cans of Canned beans for Earthquake kit) was added to its purchase list.
  - The form showed the purchase note and the product locked, with no "New product". The quantity was 4, and "No date" was off, with its note.
  - It was left without saving, and the item was removed from the list again ("Your purchase list is empty.").
- **Clean-up and state:**
  - The emulator's own records now have one more product and batch: "Test passport" (Documents, 1 document in Pantry, Expires 14 Jan 2027, confirmed). It was left in place: batches can't be deleted, only archived.
  - The purchase list is empty again. Font scale 1.0 and rotation 0. CAMERA is unchanged (not allowed).
  - No crash line for Duewise. The crash buffer holds only system crashes from the emulator's start on 2026-10-18.
- **Not checked:** TalkBack itself (the spoken labels were read from screen dumps only), photo entry with real photos, Android closing the app while the form is open, and the phone.

## 13. Shorter swap texts (2026-10-19)
- **The student's request:** "The recommended swap is a long wall of text. Is there a way to reduce the text box size by including maybe the "more" option or something else?"
- **The student's answers** to Claude Code's questions: a short note with "More about this" on Home, and the details folded on Review swap.
- **Code:** uncommitted changes on top of section 12's (also uncommitted, on top of `1277d7e`). Claude Code ran everything below on the computer and the Pixel_10a emulator (Android 17). The phone was not used.

### 13.1 Build and tests, after the last code change
- `assembleDebug assembleDebugAndroidTest testDebugUnitTest`: BUILD SUCCESSFUL. Unit tests (run at 10:09, after the last Java change; only a layout margin changed later): 308, 0 failures.
- `lintDebug --rerun-tasks` (report written at 10:19:17, after the last change): 0 errors, 1 warning, the old `TooManyViews`.
- Instrumented tests on the emulator, with the APK built after the last change: OK (118 tests).

### 13.2 By hand on the emulator, with the emulator's own records
- **Home:**
  - "Recommended swaps" shows the granola swap. The note is 3 lines instead of 6: "Proposals only: nothing moves until you confirm. The replacement days are a planning choice, not a safety guarantee."
  - "More about this" opened "Kit food dated today or earlier is never swapped; its replacement need is on the Purchase list. You can change the replacement days in Settings.", and "Show less" closed it.
  - **With no swaps:** for this check, Settings' near-date days were set to 1 and saved. Home then said "No swaps to suggest right now." with no "More about this". The near-date days were set back to 30 and saved, and a fresh Settings screen showed 30 and 180 again.
- **Review swap:**
  - The page is about 1.6 screens long instead of about 3.
  - In view: the product, the amount, the two moves, the "for review" note, the summary, "The replacement days are a planning choice, not a safety guarantee.", the Before/After table, the confirm note, and the buttons.
  - The first "More about this" opened every rule with the Settings numbers (30 and 180) and "What you recorded". The second opened "In Earthquake kit: …" and "In Pantry: …". Each opened on its own.
  - With both open, the emulator was turned to landscape and back (the screen was made again four times), and both still said "Show less".
- **Clean-up:** the emulator is back at font scale 1.0 and rotation 0, and its planning settings are 30 and 180 again. No crash line for Duewise. No swap was confirmed.
- **Not checked:** TalkBack itself, font scale 2.0 for these screens, and the phone.

## 14. Phone install of sections 12 and 13 (at the student's request)
- **The build:** everything in sections 12 and 13. The student said "install abd commit" (install and commit). It was installed first, so this record could go into the same commit on `main`, the next commit after `1277d7e`.
- **Right before the install:** `assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug` ran again: BUILD SUCCESSFUL, 79 of 80 tasks up to date (only `lintDebug` ran).
  - Unit tests: 308, 0 failures. Lint: 0 errors, 1 warning, the old `TooManyViews`.
  - The APK (built at 10:11:34, after the last code change at 10:11:28) was not rewritten, so the installed APK is the one checked on the emulator in 13 (OK, 118 instrumented tests).
- **Before the install** (10:25:49 on the phone):
  - Duewise was running in the background with four screens in its task: Home, Settings, Home, and Settings on top. No batch form was open, and the phone's screen was dozing.
  - The install closed Duewise. Settings holds no batches; only planning numbers typed there and not saved could have been lost.
  - CAMERA and POST_NOTIFICATIONS were allowed, the daily-check job was scheduled, and the crash log had no line for Duewise.
- **After the install** (2026-10-19 10:26:27):
  - `adb -s ZY22KN9QJP install -r` gave "Success". `lastUpdateTime` changed from 08:40:40 to 10:26:27, and the first install time stayed 2026-10-04 13:26:35, so it was an update.
  - The listing of `databases`, `files`, and `shared_prefs` (20 entries: names, sizes, and times to the nanosecond) was identical before and after. The databases themselves were not read.
  - CAMERA and POST_NOTIFICATIONS are still allowed. The daily-check job is still scheduled, with its next start about 09:02 on 2026-10-20.
  - The phone's `base.apk` has the same SHA-256 as the build: `d198b41c9ac970b7a8d55f579ee687bdc74dfb6067f683d2137a51eaacabccfd`.
  - The crash log has no line for Duewise.
- **Not run:** Claude Code did not open the app on the phone. The new form, the new categories, and the shorter swap texts on the phone are the student's check.
- **Note:** once a batch is saved with Cosmetics, Documents, or Subscriptions, an older build can't read it. Don't install an older build on the phone after that.

## 15. The daily reminder names items (2026-10-19)
- **The student's request:** "When the reminder is triggered it doesn't say anything about which items are expiring. I want the reminder to contain a list of items that are expiring. If there are more than two items just say the top two items plus others in the reminder."
- **The student's answers** to Claude Code's questions:
  - The phone's lock-screen setting decides whether the names show on the lock screen.
  - Medicine stays unnamed ("medicine").
  - The names go in one line, under the same title.
  - The rest is said with the number ("and 3 more").
- **Code:** uncommitted changes on top of `5667dec`. Claude Code ran everything below on the computer and the Pixel_10a emulator (Android 17). The phone was not used.

### 15.1 Build and tests, after the last code change
- `assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug` (11:36:47 to 11:37:23): BUILD SUCCESSFUL, 28 of 80 tasks ran.
- Unit tests: 321, 0 failures. 13 are new: `ReminderListTest` (11) and two in `ReminderCheckTest` (now 17).
- Lint (report written at 11:37:22, in the same run): 0 errors, 1 warning, the old `TooManyViews`.
- Instrumented tests on the emulator (both APKs installed with `install -r -t`, then `am instrument -w`): OK (124 tests). 6 are new: `ReminderTextTest` (5) and one in `ReminderFlowTest`.
  - `ReminderFlowTest`'s check for generic words now looks at the lock-screen (public) version. The shown text is checked on its own, and the medicine's name must never appear in either.
- No code changed after this build; only the records did.

### 15.2 By hand on the emulator, with the emulator's own records
- **A real daily check:**
  - The emulator's daily reminder was on, and today's reminder had already been shown.
  - So the record of the last reminder (`shared_prefs/reminders.xml`) was set back one day, to 2026-10-18. Then `ReminderWorkerTest` alone ran the real check: "Daily check: SHOWN".
- **The notification:** "Household items need review" and "Medicine, Beans, and 3 more". The expanded text is the same, and the notification is private (`vis=0`).
- **Home's lists**, after a tap on the notification (it opened at "Needs review"):
  - Pain reliever tablets (medicine), Date passed, 8 Oct 2026
  - Beans, Date passed, 18 Oct 2026
  - EU Visa, Near date, 31 Oct 2026
  - Canned beans, Near date, 1 Nov 2026
  - Granola bars in Earthquake kit, Near date, 7 Nov 2026
  - Granola bars in Pantry, Date needs review
  - So "Medicine, Beans, and 3 more" is right. EU Visa, Canned beans, and Granola bars are the 3 more, and the batch whose date needs review doesn't count.
- **The lock screen:** the check was run again the same way. Then the emulator was locked with a test PIN.
  - With "show sensitive content" on (`lock_screen_allow_private_notifications` 1, as it was): "Household items need review" and "Medicine, Beans, and 3 more".
  - With it off (0): "Household items need review" and "Open Duewise to see the review list.".
  - Afterwards the setting went back to 1 and the PIN was removed (`locksettings verify` succeeded without one). The lock screen is disabled again, as before.
- **Settings → Reminders:** the new summary is in view, and "More about this" shows the new longer text. Both wrap, and nothing is cut off.
- **Clean-up and state:**
  - The emulator's reminder record is 2026-10-19 again (the real check wrote it), and today's daily reminder is still in its notification list.
  - No crash line for Duewise.
- **Not checked:** TalkBack reading the notification, font scale 2.0, a very long product name in the pop-up, and the phone.

## 16. Phone install of section 15 (at the student's request)
- **The build:** everything in section 15. The student said "Install on the phone and commit." It was installed first, so this record could go into the same commit on `main`, the next commit after `5667dec`.
- **Right before the install:** `assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug` ran again (12:05:21 to 12:05:24): BUILD SUCCESSFUL, 79 of 80 tasks up to date.
  - Unit tests: 321, 0 failures. Lint: 0 errors, 1 warning, the old `TooManyViews` (report from 11:37:22).
  - The APK (built at 11:37:13) was not rewritten, so the installed APK is the one checked on the emulator in 15 (OK, 124 instrumented tests).
  - The exported database schema (`app/schemas`) didn't change, so the database stays version 3.
- **Before the install** (12:05:28 on the phone):
  - Duewise was not running: no screens in its task and no process. The phone's screen was dozing.
  - CAMERA and POST_NOTIFICATIONS were allowed, the daily-check job was scheduled, and the crash log had no line for Duewise.
- **After the install** (2026-10-19 12:05:53):
  - `adb -s ZY22KN9QJP install -r` gave "Success". `lastUpdateTime` changed from 10:26:27 to 12:05:53, and the first install time stayed 2026-10-04 13:26:35, so it was an update.
  - The listing of `databases`, `files`, and `shared_prefs` (20 entries: names, sizes, and times to the nanosecond) was identical before and after. The databases themselves were not read.
  - CAMERA and POST_NOTIFICATIONS are still allowed. The daily-check job is still scheduled, with its next start about 09:02 on 2026-10-20.
  - The phone's `base.apk` has the same SHA-256 as the build: `4674ba994aedc35519bc9bd4b0083cc19ed96c8c356c37464feb04d8b4426920`.
  - The crash log has no line for Duewise.
- **Not run:** Claude Code did not open the app on the phone. The student checks the first daily reminder with names and the phone's own lock-screen setting. That reminder comes at the next daily check, if something causes one.

## 17. Home's review lists start closed (2026-10-19)
- **The student's request:** "The interface Is still a little clunky. What I want to happen is the "review list" should be visible only when I click on the "Show the review list" option. It should not show up by default. Make this change."
- **The student's answers** to Claude Code's questions:
  - A reminder or widget tap keeps the lists closed: Home opens at the top.
  - Without the banner, "Needs review" keeps its short note.
  - The button is a Show/Hide toggle.
- **Code:** uncommitted changes on top of `bcae68c`. Claude Code ran everything below on the computer and the Pixel_10a emulator (Android 17). The phone was not used.

### 17.1 Build and tests, after the last code change
- `assembleDebug assembleDebugAndroidTest testDebugUnitTest` (16:52:12 to 16:52:41): BUILD SUCCESSFUL. Unit tests: 321, 0 failures (results written at 16:52:40).
- `lintDebug --rerun-tasks` (16:53:14 to 16:53:41, all 27 tasks ran): 0 errors, 1 warning, the old `TooManyViews` (`activity_batch_edit.xml`). It didn't rebuild the app APK (still 16:52:39).
- Instrumented tests on the emulator (both APKs installed with `install -r -t`, then `am instrument -w`, 16:54:10 to 16:54:30): OK (124 tests).
  - `ReviewWidgetTest` compares the widget description that Android offers with the string, so it passed with the new words ("Tap it to open Duewise.").
- No code changed after this build; only the records did.

### 17.2 By hand on the emulator, with the emulator's own records
- **A new start** (force-stop, then a launcher start):
  - The banner showed "Show the review list" with a down arrow.
  - "Recommended swaps" came right after the shortcut tiles. "Needs review" and "Medicine dates to review" were not on the screen.
- **"Show the review list":**
  - Both lists opened, and Home scrolled so that "Needs review" was at the top.
  - "Needs review" listed Beans (Date passed); EU Visa, Canned beans, and Granola bars in Earthquake kit (Near date); and Granola bars in Pantry (Date needs review).
  - "Medicine dates to review" showed its note and Pain reliever tablets (Date passed).
  - The button said "Hide the review list", with an up arrow.
- **Batch details and back:** EU Visa → Batch details → the back arrow: the lists were still open.
  - **An earlier try with Beans:** Batch details closed by itself after about 5 seconds.
    - The emulator's log shows a Back press at 16:58:47.848 that no adb command sent. The closest command was a screen dump 0.4 s earlier.
    - Home came back with the lists still open. Claude Code then repeated the check with EU Visa and the back arrow, as above.
- **Rotation:**
  - `adb emu rotate` to landscape: the button still said "Hide the review list", and the lists were right below the shortcut tiles.
  - Three more turns, back to portrait (ROTATION_0): still open.
- **"Hide the review list":** the lists closed, and the button said "Show the review list" again. "Recommended swaps" came right after the tiles.
- **Reminder taps:** Settings → "Send a test reminder", then a tap on it in the notification list. The test reminder opens Home the same way as the daily reminder.
  - **With Settings in front:**
    - A new Home opened on top of Settings (the screen stack was Home, Settings, Home).
    - It was at the top, with the lists closed.
    - Back returned to Settings.
  - **With Home in front,** its lists open and scrolled down to "Needs review": the same Home (only one Home in the stack) scrolled back to the top, and the lists stayed open.
- **No banner,** in the separate sample data:
  - Its four batches on the review lists (Canned beans, two Granola bars, and Pain reliever tablets, all made-up) were archived.
  - Home then had no banner and no button. It showed "Needs review" with "Nothing needs review right now." The same was true after a force-stop and a new start, with the lists closed.
  - Afterwards: Settings → "Start sample data again", then "Close sample data".
- **Found, not fixed here (D-11 in 4.4; fixed later the same day, see 18):**
  - With the emulator's own records, the banner showed "Date passed 2" and "Near date 3" but no "Date needs review 1" tile, although the list shows that batch.
  - With the sample data it showed "Date passed 1" and "Near date 2", without the blue tile. Once its "Date passed" batch was archived (two statuses left), both of its tiles showed.
  - The cause is in the tiles' layout (see D-11), not in the review lists. This change didn't touch the tiles.
- **Clean-up and state:**
  - The sample data is closed again, as before.
  - These are as before: planning settings 30 and 180, the daily reminder on (last shown 2026-10-19), rotation 0, and font scale 1.0.
  - The household database's write-ahead file (`readyshelf.db-wal`) still had its 09:55 time, so its records were not written.
  - No crash line for Duewise. The crash buffer has only the emulator's UWB service.
- **Not checked:**
  - TalkBack itself (whether it says "Collapsed" and "Expanded"), and font scale 2.0
  - the home-screen widget's own tap. It opens Home with the same intent as the reminder, which was checked.
  - the date rules missing or stopped. The code shows "Needs review" with its message then, the same as with no banner.
  - the phone

## 18. Fix for D-11: the banner's last tile (2026-10-19)
- **The student's request:** "Yes fix D11", after asking "what is D11?".
- **The fix:** in `ui/home/HomeActivity.java` (`showAttention`), the empty filler next to an odd last tile is now as tall as the row (`MATCH_PARENT`) instead of 0.
  - The row now takes its height from the tile.
  - The filler keeps its gap, so the half-wide tile lines up with the tiles above it.
  - Nothing else changed.
- **Code:** uncommitted, on top of section 17's changes (also uncommitted, on top of `bcae68c`). Claude Code ran everything below on the computer and the Pixel_10a emulator (Android 17). The phone was not used.

### 18.1 Build and tests, after the fix
- `assembleDebug assembleDebugAndroidTest testDebugUnitTest` (17:19:30 to 17:19:38): BUILD SUCCESSFUL. Unit tests: 321, 0 failures (results written at 17:19:37).
- `lintDebug --rerun-tasks` (17:19:55 to 17:20:16, all 27 tasks ran): 0 errors, 1 warning, the old `TooManyViews`. The report was written at 17:20:15, after the fix (17:19:25).
- Instrumented tests on the emulator (both APKs installed with `install -r -t` at 17:19:56, then `am instrument -w`, 17:20:09 to 17:20:55): OK (124 tests).

### 18.2 By hand on the emulator
- **The emulator's own records (3 statuses):**
  - "Date passed 2" and "Near date 3" were in the first row. "Date needs review 1" was alone in the second row, at half width and the same height, lined up with the first column.
  - A tap on it opened Inventory with "Date needs review" chosen: "Showing 1 of 8 batches" (Granola bars). The back arrow returned to Home.
- **The sample data, counting down:**
  - **3 statuses:** "Date passed 1", "Near date 2", and the blue "Date needs review 1" all showed. This is the demo's case in DEMO.md step 2.
  - **After its "Date passed" batch was archived:** "Near date 2" and "Date needs review 1" showed, in one row.
  - **After its "Date needs review" batch was archived:** "Near date 2" showed alone, at half width. Before the fix the box would have had no tile at all.
  - Then "Start sample data again" (3 tiles again, all shown) and "Close sample data".
- **The screen dump** (uiautomator) listed all three tiles as focusable, with their spoken labels, for example "Date needs review: 1 batch". So TalkBack can reach them, although TalkBack itself was not run.
- **Font scale 2.0,** with the emulator's own records: the lone tile's "Date needs review" wrapped onto two lines, and the tile grew to fit. Nothing was cut off. Then the font scale went back to 1.0.
- **Clean-up and state:**
  - These are as before: sample data closed, planning settings 30 and 180, the daily reminder on (last shown 2026-10-19), rotation 0, and font scale 1.0.
  - `readyshelf.db-wal` still had its 09:55 time, so the household's records were not written.
  - No crash line for Duewise.
- **Not checked:**
  - 4 tiles. "Date is today" needs a batch dated today, and an even count works the same way as 2.
  - TalkBack itself
  - the phone

## 19. Home's tiles: "Action needed date", no "Date needs review" tile, and one tile layout (2026-10-19)
- **The student's request:** "I want to change the display of the tile from "near date" to "action needed date." I also want ti change the size of the "show the review list" to match the size of the other tiles and I don't want to show "Date Need review" tile. Implement the best practices of tile/icon layout on this screen." A first message just before it said "Action Needed."; the later words were used.
- **What changed:**
  - Home's "Near date" tile says "Action needed date". Inventory, the status labels, and Batch details still say "Near date".
  - "Date needs review" gets no tile. Its batches still count, so they still show the banner, and they are still on the review lists.
  - "Show the review list" is the banner's last tile (`item_review_toggle_tile.xml`), with the same size and parts as a status tile, instead of a full-width button.
  - Every row of the banner is as tall as the tallest one (`measureWithLargestChild`). With large text, one tile per row.
  - Home's three shortcut tiles are always the same size (`match_parent` in their row, which is not baseline-aligned).
- **Code:** uncommitted, on top of sections 17 and 18 (also uncommitted, on top of `bcae68c`). Claude Code ran everything below on the computer and the Pixel_10a emulator (Android 17). The phone was not used.

### 19.1 Build and tests, after the last code change
- The last code change was in `item_review_toggle_tile.xml` at about 18:18: a FrameLayout in place of a row with a weighted Space (see the lint line below).
- `assembleDebug assembleDebugAndroidTest testDebugUnitTest` (18:18:33 to 18:18:37): BUILD SUCCESSFUL. The APK was written at 18:18:37.
  - `testDebugUnitTest` was up to date, because no Java changed after its run at 18:17:35 to 18:17:39: 321 tests, 0 failures (results written at 18:17:38).
- `lintDebug --rerun-tasks` (18:18:37 to 18:18:50, all 27 tasks ran): 0 errors, 1 warning, the old `TooManyViews`. It didn't rewrite the APK.
  - The run before it (18:17:46 to 18:18:03) had a second warning, `NestedWeights` in the new tile. The FrameLayout fixed it.
- Instrumented tests on the emulator (both APKs installed with `install -r -t`, then `am instrument -w`, 18:19:04 to 18:19:26): OK (124 tests).

### 19.2 By hand on the emulator, with the emulator's own records
The checks below used the builds of 18:00 and 18:07. The final build (18:18:37) was checked again at font scales 1.0 and 2.0: every tile and word was in the same place.
- **A new start, font scale 1.0:**
  - Row 1: "Date passed 2" and "Action needed date 3". Row 2: "Show the review list" with a down arrow, alone, and the same size as the tiles above (445 × 241 px each).
  - There was no "Date needs review" tile, although the list has one batch with a date that needs review (Granola bars, 3 bars in Pantry).
  - The screen dump lists the review-list tile as a Button named "Show the review list". The status tiles are named "Date passed: 2 batches" and "Action needed date: 3 batches".
- **"Show the review list":**
  - Both lists opened, and Home scrolled to "Needs review": Beans, EU Visa, Canned beans, Granola bars in Earthquake kit, and Granola bars in Pantry. The labels of the three near-date batches still say "Near date".
  - Back at the top, the tile said "Hide the review list" with an up arrow, at the same size.
- **"Hide the review list":** the lists closed, and "Recommended swaps" came right after the shortcut tiles.
- **A tap on "Action needed date":** Inventory opened with "Near date" chosen: "Showing 3 of 8 batches" (EU Visa, Canned beans, Granola bars). The back arrow returned to Home.
- **Font scale 1.3:**
  - Two tiles in a row. "Action needed date" and "Show the review list" took two lines, and both rows were 318 px tall.
  - "Purchase list" took two lines. Before the shortcut-tile change, its tile was taller than the other two and started a little lower. After it, all three were 1248 to 1524 px.
- **Font scale 2.0:**
  - One tile per row at full width, all three 307 px tall. The review-list tile's icon and words were in the same places as a status tile's.
  - The shortcut tiles were full-width rows, 155 px each, as before.
- **Rotation,** with the lists open:
  - Landscape: two tiles in a row, 241 px tall, and the tile still said "Hide the review list".
  - Back to portrait (ROTATION_0): still open.
- **The sample data** (opened in Settings):
  - The banner showed "Date passed 1" and "Action needed date 2" in row 1, and "Show the review list" in row 2. Its batch with a date that needs review had no tile. This is the demo's case in DEMO.md step 2.
  - The "Date passed" tile opened Inventory: "Showing 1 of 5 batches" (Pain reliever tablets).
  - Archiving it, to see two tiles in one row, stopped at its review screen: the student interrupted before "Confirm", and nothing was archived. Then "Close sample data".
- **Clean-up and state:**
  - Sample data is closed (`sample_data_open` false), as before. Font scale 1.0, rotation 0.
  - The household database's `readyshelf.db-wal` still had its 09:55 time, so its records were not written.
  - No crash line for Duewise.
- **Not checked:**
  - two status tiles in one row, and a banner with only the review-list tile (when the only batches have dates that need review). The code handles both with the same rows as above.
  - "Date is today", which needs a batch dated today
  - TalkBack itself
  - the phone (see 20)

## 20. Phone install of sections 17 to 19 (at the student's request)
- **The build:** everything in sections 17, 18, and 19. The student said "install and commit". It was installed first, so this record could go into the same commit on `main`, the next commit after `bcae68c`.
- **Right before the install:** `assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug` ran again (18:22:34 to 18:22:36): BUILD SUCCESSFUL, 79 of 80 tasks up to date.
  - Unit tests: 321, 0 failures. Lint: 0 errors, 1 warning, the old `TooManyViews` (report from 18:18:50).
  - The APK (built at 18:18:37) was not rewritten, so the installed APK is the one checked on the emulator in 19 (OK, 124 instrumented tests).
  - The exported database schema (`app/schemas`) didn't change, so the database stays version 3.
- **Before the install** (just before 18:23:04, the computer's clock):
  - Duewise was open on Home only, with no form in its task, and the phone's screen was dozing. The install closed it.
  - CAMERA and POST_NOTIFICATIONS were allowed, the daily-check job was scheduled, the crash log had no line for Duewise, and sample data was closed.
- **After the install** (2026-10-19 18:23:14):
  - `adb -s ZY22KN9QJP install -r` gave "Success". `lastUpdateTime` changed from 12:05:53 to 18:23:14, and the first install time stayed 2026-10-04 13:26:35, so it was an update.
  - The listing of `databases`, `files`, and `shared_prefs` (20 entries: names, sizes, and times to the nanosecond) was identical before and after. The databases themselves were not read.
  - CAMERA and POST_NOTIFICATIONS are still allowed. The daily-check job is still scheduled, with its next start about 09:02 on 2026-10-20.
  - The phone's `base.apk` has the same SHA-256 as the build: `1d0b3cf3c3f2bce3cc8dff333f5c18f58d5bb4ef5276886685e9bfd7d539ece4`.
  - The crash log has no line for Duewise. Sample data is still closed.
- **Not run:** Claude Code did not open the app on the phone. The student checks Home on the phone: the closed review lists, the banner's tiles, and the shortcut tiles.

## 21. One square grid for Home's tiles (2026-10-19)
- **The student's request:** "All the icons on the home screen should neatly align in the column and rows. Right now I see that they're spread across and there is a white space gap between the data icons and the inventory purchase list and the second settings icon. If you want move the second settings icon to the top right here so that everything looks neat and maintained in a square grid."
- **The student's choice,** from three sketches Claude Code offered: one grid of two columns in a fixed order: the status tiles, "Show the review list", Inventory, Purchase list, Settings ("Settings last").
- **What changed:**
  - The gray box around the status tiles is gone. "Items need review" is a heading above the grid.
  - All of Home's tiles are in one grid (`home_tiles`): two in a row (one with large text), every row as tall as the tallest one.
  - Inventory, Purchase list, and Settings use the same tile as "Show the review list" (`item_action_tile.xml`, which replaces `item_review_toggle_tile.xml`): the icon in a blue circle at the top left, ">" at the top right, and the word below.
  - The top part of those tiles is as tall as a status tile's (an invisible stand-in for the count), so the icons and the words line up at every text size.
  - The sample-items notice and the getting-started message moved below the grid.
- **Code:** uncommitted, on top of `41cce25`. Claude Code ran everything below on the computer and the Pixel_10a emulator (Android 17). The phone was not used.

### 21.1 Build and tests, after the last code change
- The last code change was in `item_action_tile.xml` at about 18:41 (the stand-in for the count).
- `assembleDebug assembleDebugAndroidTest testDebugUnitTest` (18:45:10 to 18:45:11): BUILD SUCCESSFUL, all 71 tasks up to date. The APK is from 18:42:04.
  - The unit tests last ran from 18:37:45 to 18:37:54, after the last Java change: 321 tests, 0 failures (results written at 18:37:53).
- `lintDebug --rerun-tasks` (18:45:11 to 18:45:22, all 27 tasks ran): 0 errors, 1 warning, the old `TooManyViews`.
- Instrumented tests on the emulator (both APKs installed with `install -r -t`, then `am instrument -w`, 18:45:26 to 18:45:48): OK (124 tests).

### 21.2 By hand on the emulator
The first checks used the build of 18:37. The font scales, the rotation, and the sample data were checked again, or only, with the final build (18:42:04).
- **The emulator's own records, font scale 1.0** (2 status tiles):
  - A full grid of 3 rows: "Date passed 2" and "Action needed date 3"; "Show the review list" and Inventory; Purchase list and Settings.
  - All six tiles were 487 × 241 px. The left column was at x 42 to 529 and the right column at x 550 to 1038 in every row. Every word started 158 px below its tile's top.
  - The screen dump lists the new tiles by their ids (`home_attention_toggle`, `home_inventory`, `home_purchase_list`, `home_settings`) and spoken labels.
- **"Show the review list":** the lists opened, and Home scrolled to "Needs review". Back at the top, the tile said "Hide the review list". "Hide the review list" closed them, and "Recommended swaps" came right after the grid.
- **Taps:**
  - "Action needed date" opened Inventory with "Near date" chosen: "Showing 3 of 8 batches".
  - Inventory, Purchase list, and Settings opened their screens.
  - The back arrow returned to Home each time.
- **Font scale 1.3:** two tiles in a row, and all three rows were 318 px tall. "Action needed date" and "Show the review list" took two lines.
  - With the build of 18:37, Inventory's icon sat lower than the icon of "Show the review list" in the same row.
  - With the stand-in (18:42), every word started 158 px below its tile's top, so the icons and the words lined up in every row.
- **Font scale 2.0:** one tile per row at full width, each 307 px tall, and every word 184 px below its tile's top. Then font scale 1.0 again.
- **Rotation,** with the lists open:
  - Landscape: two tiles in a row (x 42 to 1130 and 1151 to 2240), 241 px tall, and the tile still said "Hide the review list".
  - Back to portrait (ROTATION_0): still open.
- **The sample data** (opened in Settings, then closed again): a full grid of 3 rows, "Date passed 1" and "Action needed date 2"; "Show the review list" and Inventory; Purchase list and Settings. This is the demo's case in DEMO.md step 2.
- **Clean-up and state:**
  - Sample data is closed (`sample_data_open` false). Font scale 1.0, rotation 0.
  - `readyshelf.db-wal` still had its 09:55 time, so the household's records were not written.
  - No crash line for Duewise.
- **Not checked:**
  - 1 or 3 status tiles (the empty cell at the bottom right), and Home with nothing to review (only the three shortcut tiles)
  - the sample-items notice and the getting-started message in their new place below the grid
  - TalkBack itself
  - the phone (see 22)

## 22. Phone install of section 21 (at the student's request)
- **The build:** everything in section 21. The student said "install and commit". It was installed first, so this record could go into the same commit on `main`, the next commit after `41cce25`.
- **Right before the install:** `assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug` ran again (19:00:45 to 19:00:47): BUILD SUCCESSFUL, 79 of 80 tasks up to date.
  - Unit tests: 321, 0 failures. Lint: 0 errors, 1 warning, the old `TooManyViews` (report from 18:45:22).
  - The APK (built at 18:42:04) was not rewritten, so the installed APK is the one checked on the emulator in 21 (OK, 124 instrumented tests).
  - The exported database schema (`app/schemas`) didn't change, so the database stays version 3.
- **Before the install** (19:00:57, the computer's clock):
  - Duewise was open on Home only, with no form in its task, and the phone's screen was dozing. The install closed it.
  - Since the install at 18:23, only `readyshelf.db-shm` (same size) and `profileInstalled` had new times (18:26), so the app was opened but no records were written.
  - CAMERA and POST_NOTIFICATIONS were allowed, the daily-check job was scheduled, the crash log had no line for Duewise, and sample data was closed.
- **After the install** (2026-10-19 19:01:19):
  - `adb -s ZY22KN9QJP install -r` gave "Success". `lastUpdateTime` changed from 18:23:14 to 19:01:19, and the first install time stayed 2026-10-04 13:26:35, so it was an update.
  - The listing of `databases`, `files`, and `shared_prefs` (20 entries: names, sizes, and times to the nanosecond) was identical before and after. The databases themselves were not read.
  - CAMERA and POST_NOTIFICATIONS are still allowed. The daily-check job is still scheduled, with its next start about 09:02 on 2026-10-20.
  - The phone's `base.apk` has the same SHA-256 as the build: `ebcf23ec57987837cd83280364653536d934069c70910dda54bbfa6b52b2ebdf`.
  - The crash log has no line for Duewise. Sample data is still closed.
- **Not run:** Claude Code did not open the app on the phone. The student checks Home's grid on the phone.

## 23. Settings: UX improvements by best practice (2026-10-19)
- **The student's request:** "go under the settings screen and make some UX improvements according to the best practices." Claude Code made a plan from Android's settings guidelines, Android's guidance for a refused permission, and Material's guidance, and the student approved it.
- **What changed (Settings only):**
  - Reminders is the first card. The "Daily reminder" switch is right under its title, with the summary under it in smaller words. Then "More about this", the last reminder, the notifications line with "Open notification settings" right under it, and "Send a test reminder". Planning settings come second and Sample data last.
  - The notifications line is a warning (dark gold with a warning sign) only while the daily reminder is on but can't appear; otherwise it is the quiet note, with the same words.
  - **Planning numbers:**
    - The keyboard's key is "Next", then "Done", and "Done" saves. After a save the keyboard closes.
    - Errors appear on a save and go away once the number is fixed.
    - Back with an unsaved change asks "Leave without saving?" ([Keep editing] [Leave]).
  - While sample data is open, "Close sample data" is the main (tonal) button, with "Start sample data again" (outlined, new id `settings_restart_sample`) under it.
- **Code:** uncommitted, on top of `4490395`. Claude Code ran everything below on the computer and the Pixel_10a emulator (Android 17). The phone was not used.

### 23.1 Build and tests, after the last code change
- `assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug` (19:49:44 to 19:49:56): BUILD SUCCESSFUL, 80 tasks (24 ran, 56 up to date). The APK is from 19:49:51.
  - Unit tests: 321, 0 failures, 0 errors (results written at 19:49:53).
  - Lint (report written at 19:49:55): 0 errors, 1 warning, the old `TooManyViews` (`activity_batch_edit.xml`).
  - The test APK didn't need a rebuild (no test code changed).
- Instrumented tests on the emulator (both APKs installed with `install -r -t` at 19:50:20, then `am instrument -w`, 19:50:34 to 19:50:54): OK (124 tests).
- No code changed after these runs; only the records did.

### 23.2 By hand on the emulator, with the emulator's own records
- **Before:** screenshots of the old Settings (Planning settings first, the switch under the summary and "More about this", "Open notification settings" last in its card).
- **The emulator's state before:** the daily reminder on, 30 and 180, sample data closed, notifications allowed, font scale 1.0, rotation 0.
- **Order:** Reminders (title, "Daily reminder" switch, summary, "More about this", "Last daily reminder: 19 Oct 2026", "Notifications from Duewise are allowed." in a quiet box, "Open notification settings", "Send a test reminder" and its note), then Planning settings, then Sample data with "Open sample data".
- **The warning:**
  - With the reminder on, "Open notification settings" opened Android's page for Duewise. "All Duewise notifications" was switched off there, and the phone's Back returned to Settings: the line was dark gold with the warning sign and said "Notifications from Duewise are not allowed right now, so reminders can't appear. Everything else in Duewise works as usual."
  - Switch tapped: "Daily reminder turned off. No more reminders will be shown." The same words were then in the quiet note look.
  - Notifications switched on again in Android's page: "Notifications from Duewise are allowed." Switch tapped: "Daily reminder turned on." POST_NOTIFICATIONS stayed granted with the same flags.
- **The keyboard:**
  - The emulator has a hardware keyboard, so Gboard showed only a floating button. For this check `show_ime_with_hard_keyboard` was set to 1 and Gboard's "Show on-screen keyboard" was used; the setting was 0 again afterwards.
  - The near-date days showed the number keyboard with the Next key; tapping it moved the focus to the replacement days, whose key was Done.
  - Replacement days 181, then Done: "Planning settings saved.", the keyboard closed (`mInputShown=false`), and the settings file said 30 and 181.
- **Errors:**
  - Near-date days 0, then Done: "Enter a whole number from 1 to 365." on the near-date days, which got the focus; the keyboard stayed and nothing was saved.
  - Emptying the field kept the error. Typing 3 took it away (the helper text came back) before any save.
  - Near-date days 200 and replacement days 181, then Done: "Enter a number more than the near-date days (200), or lower the near-date days." on the replacement days. Changing 200 to 20 took it away. Nothing was saved.
- **Leaving without saving** (near-date days 20; saved 30):
  - The ← arrow: "Leave without saving?", "You changed the planning settings but didn't save them.", [Keep editing] [Leave]. "Keep editing" kept the 20.
  - Rotated to landscape: the 20 was kept, and the arrow asked again. Rotated back with the question open: the question was gone, the 20 was kept, and the arrow asked again. "Leave" opened Home. Settings opened again showed 30 and 181.
  - 181 changed to 180, then "Save planning settings": "Planning settings saved." at the bottom, the keyboard closed, and the file said 30 and 180.
  - With nothing changed, the ← arrow and the phone's Back closed Settings at once.
  - With near-date days 25: the back gesture from the left edge asked the question. The phone's Back first closed the keyboard, and a second Back asked. "Leave" saved nothing.
  - **"Don't keep activities"** (`always_finish_activities` set to 1): it had no effect here; the events log showed Settings restarted, not rebuilt. The setting was removed again (it was unset before).
  - **Process restart instead:** with 25 typed, Home, then `am kill`. Bringing Duewise back started a new process (pid 9735, then 13877) that rebuilt Settings: the 25 was back, and the arrow asked. "Leave" saved nothing.
- **Sample data:**
  - Opened from Settings (dialog, then "Sample data is open." on Home with the label). Settings then showed the status box, "Close sample data" (tonal) first, "Start sample data again" (outlined), the keep note, and "Keep these items as my own" (outlined).
  - "Start sample data again" asked "Start sample data again?" ([Cancel] [Start again]); Cancel.
  - "Close sample data", then its dialog: Home without the label, and `sample_data_open` false.
- **Font scale 2.0:** every text in Settings wrapped and nothing was cut off ("Open notification settings" took two lines). Then font scale 1.0 again.
  - **Seen, not changed:** at font scale 2.0 the floating "Add batch" button covers the right half of Home's Settings tile until Home is scrolled. A tap on the tile's middle opened the empty Add batch form, which was left with the ← arrow; nothing was saved.
- **Clean-up and state:**
  - the daily reminder on, 30 and 180, sample data closed, and notifications allowed
  - font scale 1.0, rotation 0, `show_ime_with_hard_keyboard` 0, and `always_finish_activities` unset
  - `readyshelf.db` (4,096 bytes, 2026-10-18 12:44:53) and `readyshelf.db-wal` (321,392 bytes, 2026-10-19 09:55:51) unchanged, so the household's records were not written
  - the daily-check job (`ReminderWorker`) is scheduled, next start in about 12 h 50 min (about 09:00 on 2026-10-20), after the switch was turned off and on again
  - no crash line for Duewise, and Duewise is on Home
- **Not checked:**
  - TalkBack itself
  - Android 8.x (no image here). On 8.x the first text field takes the focus when a screen opens. It is now in the second card; NestedScrollView doesn't give the focus to a field that is off screen, so no jump is expected.
  - the phone

## 24. Settings, round 2: short cards and "Close sample data" on every screen (2026-10-19, evening)
- **The student's requests:**
  - "make it easier to close the sample data. It's kind of confusing and complicated."
  - "make the descriptions on the settings page a bit shorter and consise … while getting to the main point."
  - "The settings page still has a huge wall of text which is not user friendly."
- **The student's choices:**
  - "Short cards", from three layouts Claude Code sketched.
  - The close button goes under the words of the purple "Sample data" label.
  - "Not too small" means the length; text sizes stay the same.
- **What changed:**
  - **Every screen, while sample data is open:**
    - A "Close sample data" button sits under the label's words. It asks "Close sample data?" ("Your own records come back. The made-up items and your changes to them are deleted.", [Cancel] [Close]).
    - Settings' card uses the same question (`SampleDataClose`).
  - **Settings:**
    - Each card has one or two short lines. "More about this" moved to the end of the Reminders card and holds five short texts, the test reminder's note included.
    - The sample intro shows only while sample data is closed.
    - The dialogs' words are shorter.
- **Code:** uncommitted, on top of `4490395` together with section 23. Claude Code ran everything below on the computer and the Pixel_10a emulator (Android 17). The phone was not used.

### 24.1 Build and tests
- **After the code changes:** `assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug` (21:11:17 to 21:11:29): BUILD SUCCESSFUL.
  - Unit tests: 321, 0 failures, 0 errors (results written at 21:11:27).
  - Lint: 0 errors, 1 warning, the old `TooManyViews`.
  - Instrumented tests on the emulator (installed at 21:11:39, `am instrument -w` from 21:11:48 to 21:12:11): OK (124 tests).
- **After the last change:**
  - The change: a non-breaking space in "1 to 365", so the helper never splits it over two lines. It is a string only, with no Java change.
  - The same command (21:14:15 to 21:14:21): BUILD SUCCESSFUL, 80 tasks (12 ran, 68 up to date). The APK is from 21:14:19.
  - The unit tests were up to date; they last ran at 21:11:27, after the last Java change.
  - Lint (report written at 21:14:21): 0 errors, 1 warning, the old `TooManyViews`.
  - Instrumented tests (installed at 21:14:24, run from 21:14:39 to 21:15:01): OK (124 tests).

### 24.2 By hand on the emulator, with the emulator's own records
- **Words in view in Settings,** counted from `uiautomator` dumps while scrolling from top to bottom. The count covers every text except buttons, number fields, and the toolbar title; the card titles are included.
  - With sample data closed: 210 words in 11 texts before, 94 words in 10 texts after.
  - With sample data open: 128 words in 13 texts after, the label's words included. This was not measured before.
- **The Reminders card:**
  - the switch
  - "About once a day when a date is near, today, or passed. Never names medicine." and "Last reminder: 19 Oct 2026" in the smaller style
  - "Notifications are allowed." in its box
  - "Open notification settings", "Send a test reminder", and "More about this"
- **The fold:** it opened the five short texts.
- **Planning:** "Used for statuses, reminders, and swaps. They are planning choices, not safety guarantees." and the two short helpers.
  - In the first build the near-date helper split "1" from "to 365" at a line end.
  - After the fix, "1 to 365, starts at 30." stayed together at font scale 1.0 and 2.0.
- **Sample data:**
  - Opened from Settings. The question said "Duewise shows made-up items, dated from today. Your own records stay as they are. To go back, tap “Close sample data” at the top of any screen."
  - Home showed "Sample data is open.", and the label had "Close sample data" under its words. It was there on Home, Inventory, Purchase list, Batch details, Add batch, and Settings.
  - **On Inventory:**
    - "Close sample data", then Cancel: sample data stayed open.
    - The question open, then a rotation: the question closed with its screen, the strip was still there in landscape, the process was the same (pid 2520), and logcat had 0 "leaked" lines and 0 crash lines for Duewise. Then rotation 0 again.
    - "Close sample data", then Close: Home with the emulator's own records ("Date passed 2", "Action needed date 3"), "Sample data is closed. Your own records are shown.", only Home open, and `sample_data_open` false.
  - **Opened again, Settings' card:** no intro; "Sample data is open. Changes go only into the sample data."; "Close sample data" (tonal); "Start sample data again" (outlined); "To track these items for real, copy them into your own records."; "Keep these items as my own". The card's "Close sample data" asked the same question, and Close closed it.
- **Font scale 2.0,** with sample data open:
  - The label's words took three lines with the button under them. "Close sample data", then Close, closed it at that size.
  - Settings wrapped and nothing was cut off.
  - Then font scale 1.0 again.
- **Seen, not changed:** at font scale 2.0 the floating "Add batch" button covers the middle of Home's Settings tile (see 23.2). Twice a tap there opened the empty Add batch form, which was left with the ← arrow; nothing was saved.
- **Clean-up and state:**
  - sample data closed, the daily reminder on, and 30 and 180
  - font scale 1.0, rotation 0, and notifications allowed
  - `readyshelf.db` and `readyshelf.db-wal` unchanged (4,096 bytes from 2026-10-18 12:44:53; 321,392 bytes from 2026-10-19 09:55:51)
  - the daily-check job scheduled (about 09:00 on 2026-10-20), no crash line for Duewise, and Duewise on Home
- **Not checked:**
  - TalkBack itself, the phone, and Android 8.x
  - the strip on Change stock, Review swap, and Label photos (those screens were not opened with sample data)
  - "Keep these items as my own" (not pressed, so the emulator's own records stayed as they were)

## 25. Phone install of sections 23 and 24 (at the student's request)
- **The build:** everything in sections 23 and 24. The student said "install and commit". It was installed first, so this record could go into the same commit on `main`, the next commit after `4490395`.
- **Right before the install:**
  - `assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug` ran again (21:27:08 to 21:27:10): BUILD SUCCESSFUL, 79 of 80 tasks up to date.
  - The APK (built at 21:14:19) was not rewritten, so the installed APK is the one checked on the emulator in 24 (OK, 124 instrumented tests). Its SHA-256 is `959202d125d345060e7894d84b3df6fe7adea20dc776b2729acfca94ec1773c9`.
  - Unit tests: 321, 0 failures (24.1). Lint: 0 errors, 1 warning, the old `TooManyViews` (report from 21:14:21).
  - The exported database schema (`app/schemas`) didn't change, so the database stays version 3.
- **Before the install** (21:26:51, the computer's clock):
  - Duewise was running with Home and Settings open (Settings on top, no batch form), and the phone's screen was dozing. The install closed it.
  - The phone ran `4490395`'s APK (SHA-256 `ebcf23ec…`, as in 22).
  - CAMERA and POST_NOTIFICATIONS were allowed, the daily-check job was scheduled, the crash log had no line for Duewise, and sample data was closed.
- **After the install** (2026-10-19 21:27:35):
  - `adb -s ZY22KN9QJP install -r` gave "Success". `lastUpdateTime` changed from 19:01:19 to 21:27:35, and the first install time stayed 2026-10-04 13:26:35, so it was an update.
  - The listing of `databases`, `files`, and `shared_prefs` (20 entries: names, sizes, and times to the nanosecond) was identical before and after. The databases themselves were not read.
  - CAMERA and POST_NOTIFICATIONS are still allowed. The daily-check job is still scheduled, with its next start about 09:02 on 2026-10-20.
  - The phone's `base.apk` has the same SHA-256 as the build: `959202d125d345060e7894d84b3df6fe7adea20dc776b2729acfca94ec1773c9`.
  - The crash log has no line for Duewise. Sample data is still closed.
- **Not run:** Claude Code did not open the app on the phone. The student checks Settings and the "Close sample data" button on the phone.

## 26. Whole-app UI review and the five improvements the student chose (2026-10-19, evening)
- **The student's request:** "inspect the app as a whole and evaluate it against best practices of UI and suggest improvements".
- **The review (Claude Code, by reading the code and layouts; no screenshots, because it was planned in read-only plan mode):**
  - Grades: accessibility strong; visual consistency good; navigation needs work; words mixed; forms good; layout mixed; feedback mixed; motion missing; platform good.
  - Seven problems found by reading the code:
    1. An edit with no change said "Changes saved."
    2. Back on a half-filled batch form threw it away without asking.
    3. Rule errors showed Java names (`error.toString()`).
    4. Inventory's search key did nothing.
    5. There was no progress indicator anywhere.
    6. The "can be read two ways" note had the wrong text size.
    7. Empty boxes and a blank Change stock form showed before the data arrived.
  - Twelve suggestions in three groups, plus a "later" list. SPEC.md "UI review of 2026-10-19" lists them.
- **The student's choices (AskUserQuestion):** the five recommended ones: "Small fixes", "Consistent words" with **Near date** as the word everywhere, "Compact Inventory filters", "Main button always visible", and "Shrinking Add batch".
- **A design check:** a second Claude Code agent read the design against the code. Its findings were applied:
  - the bar's color seam
  - closing the keyboard before a review
  - a category or location tapped alone doesn't count as input, so DEMO step 3 still closes at once
  - the empty-list texts say "+ Add batch"
  - "Clear all" as a chip, hidden chips while the filters are open
  - the "All" chips have no tag
  - Status chips only when known
  - posting the chip taps
- **What changed** (details in SPEC.md):
  - **Small fixes:**
    - "No changes to save."
    - "Leave without saving?" on the batch form (`BatchEditViewModel.hasUnsavedInput`)
    - "The rules stopped with an error, so this part can't be shown right now. Your records are not changed." (the parser's error is not shown or logged)
    - the search key closes the keyboard
    - a progress bar while photos are read
    - the note's text size
    - notes and Change stock's form hidden until the data arrives
    - "How much? …" and "Type: …" spoken labels
    - two fewer live regions on the photo review
  - **Words:**
    - Home's tile says "Near date" (`home_tile_near_date` removed)
    - "swap proposals" in eight texts
    - `home_empty`, `inventory_empty`, and `inventory_no_matches`
  - **Inventory:** "Filters" folds the filters (closed by default). The chosen ones are chips under the search box (`item_applied_filter_chip.xml`); a tap or ✕ sets one back, and "Clear all" sets all back when there are two or more. `MoreText` got an overload with a hide text and a callback.
  - **Bottom bar** (`ReadyShelf.BottomBar`, `bg_bottom_bar.xml`):
    - batch form: "Review and save", then "Confirm and save"
    - Change stock: "Review", then "Confirm"
    - Review swap: "Confirm swap"
    - A new save error scrolls into view once. Save messages sit above the bar (`Confirmations`).
  - **"Add batch":** `ui/common/AddButton` shrinks it on scroll down and grows it on scroll up; with large text it stays shrunk.
- **Code:** uncommitted, on top of `e575e27`. Claude Code ran everything below on the computer and the Pixel_10a emulator (Android 17). The phone was not used.

### 26.1 Build and tests
- **First run, after the code changes:** `assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug` (22:15:04 to 22:15:13): BUILD SUCCESSFUL.
  - Unit tests: 321, 0 failures, 0 errors, 0 skipped (results written at 22:15:08).
  - Lint: 0 errors, 1 warning, the old `TooManyViews` (`activity_batch_edit.xml`).
  - Instrumented tests on the emulator (`am instrument -w`, 22:15:41 to 22:16:05): **OK (127 tests)**. That is 124 before, plus 3 new in `BatchEditFormTest`:
    - every kind of form starts with nothing to lose
    - typing a field is something to lose until it is as filled again
    - a change by hand is something to lose and is kept in the saved state
- **After the last change** (the note's text size, found in 26.2):
  - The same command with `--rerun-tasks` (22:37:59 to 22:38:18): BUILD SUCCESSFUL. The APK is from 22:38:11; its SHA-256 is `fe89a010b71becb2934ca68fb40d1f6304fb518e7f396fa9e9810e536d6a9576`.
  - Unit tests: 321, 0 failures, 0 errors, 0 skipped (22:38:14).
  - Lint (22:38:18): 0 errors, 1 warning, the old `TooManyViews`.
  - Instrumented tests on that APK (22:38:36 to 22:39:00): **OK (127 tests)**.
- `connectedAndroidTest` was not used.

### 26.2 By hand on the emulator (22:16 to 22:36)
- **Home, the emulator's own records:**
  - The tiles said "Date passed" (2) and **"Near date"** (3).
  - "Add batch" was extended at the top. After a scroll down it was a round "+" (147 px wide), and its text was still "Add batch" for TalkBack. After a scroll up it was extended again.
- **Sample data:** opened from Settings (the question, then "Open sample data"). Home: "Date passed: 1 batch", "Near date: 2 batches".
- **Inventory:**
  - **From the Near date tile:** the filters were folded, with one chip "Near date" (read as "Filter: Near date"). The count said "Showing 2 of 5 batches", and the first two cards were on the first screen.
  - **The chip tapped:** "Showing 5 of 5 batches", and the chip row was hidden.
  - **"Filters":** the toggle said "Hide filters" and the old filters showed. Then Earthquake kit and "Latest first", then "Hide filters": chips "Earthquake kit", "Latest first", and "Clear all". The count said "Showing 2 of 5 batches", newest date first.
  - **"Clear all":** "Showing 5 of 5 batches", soonest first. Opened again, the filters showed "All locations" and "Soonest first" chosen.
  - **Rotation with the filters open:** still open (ROTATION_270). Then rotation 0 again.
  - **Search key:** `show_ime_with_hard_keyboard` was set to 1 for the check, so the on-screen keyboard showed. `gran` gave "Showing 3 of 5 batches". The keyboard's search key changed `mInputShown` from true to false. Then the search was cleared.
- **Add batch (sample data):**
  - The bar showed "Review and save" at the bottom, with a line above it and no darker strip under it.
  - **Water tapped, then ←:** back on Inventory at once, with no question.
  - **"Rice" typed in Product name, then ←:**
    - The question showed "Leave without saving?", "What you entered on this form won't be saved.", [Keep editing] [Leave].
    - With the keyboard open, the bar sat just above it.
    - "Keep editing" stayed on the form.
  - **The review:** with the unit "bags" and a date from the picker, "Review and save" showed the review. The bar showed "Confirm and save"; "Save and add another" and "Back to the form" were under the review, and the text said "Swap proposals will skip this batch until…".
  - **Leaving:** ← went back to the form. ← again asked; "Leave" went to Inventory, still "Showing 5 of 5 batches", so nothing was saved.
- **Edit (sample granola bars):**
  - ← on the untouched form went back to Batch details with no question.
  - Edit, "Review and save" ("Nothing was changed."), then "Confirm and save": Batch details showed **"No changes to save."**
- **Record used:**
  - The bar showed "Review". The choices were read as "How much? All (6 bars)" and "How much? Some".
  - "Review" showed the review with the kit warning, and the bar showed "Confirm".
  - "Back" went to the amount step, and ← to Batch details. Nothing was saved.
- **Review swap:** "Confirm swap" was in the bar, in view at the top of the screen. It was not tapped.
- **Font scale 2.0:**
  - **Home:** "Add batch" was a round "+" from the start, and the Settings tile was fully in view.
  - **Inventory:** Pantry was chosen and the filters folded. "Pantry ✕" showed, and the list started on the first screen. The chip was then tapped: "Showing 5 of 5".
  - **Add batch:** the bar was one row (185 px).
  - **Landscape:** the label, the bar, and the toolbar left 261 px (about 100 dp) for the form. It still scrolled; see 26.4. Then rotation 0 again.
- **Label photos:**
  - **The test photo:** `label_two_readings.png` from the test assets was pushed to Downloads and the media scanned. The camera question was closed with Back, the product photo skipped, and the test photo chosen in the picker.
  - **While reading:** a moving progress bar showed under "Reading the photos… This can take a few seconds." (screenshot).
  - **The two-readings note, first build:** still smaller than the other texts. The design check's one-line fix (a `MaterialTextView` made with the style) doesn't pass the style's text appearance on to the text.
  - **Fixed:** `setTextAppearance` with the theme's `textAppearanceBodyLarge`. After a new install, the note had the same size as the other texts (screenshot).
  - **Cancel:** back to the form. ← closed it with no question.
  - (One pick hit an older screenshot in the picker instead; it was only read, and nothing was saved.)
- **Clean-up and state:**
  - the test photo deleted, and the media scanned again (Downloads empty)
  - CAMERA `granted=false`, without USER_SET, as before
  - `show_ime_with_hard_keyboard` 0, font scale 1.0, rotation 0
  - sample data closed ("Sample data is closed. Your own records are shown."), and Home with the emulator's own records as at the start ("Date passed: 2 batches", "Near date: 3 batches")
  - The planning settings were not touched.

### 26.3 Not checked
- TalkBack by ear, the phone, and Android 8.x.
- **A save error scrolling into view** on the batch form's review or Review swap. The errors need a batch to change while the review is open, which wasn't done.
- **The rule-error sentence on screen.** The rules didn't throw; the string and the code were read instead.
- **The missing empty-box flash** on Home and Purchase list (code reading only; too short to see).
- **"Record bought item" and the New location dialog's "Type: …" labels** (not opened).

### 26.4 Remaining problems
- In landscape with font scale 2.0 and the sample-data label, the batch form gets about 100 dp between the label and the bar. A bar that scrolls with the form on very short screens would fix it (SPEC.md, "Not in this step").
- Lint's old `TooManyViews` on `activity_batch_edit.xml`, and the open items in 4.4, are unchanged.
- The suggestions the student didn't choose are in SPEC.md, "UI review of 2026-10-19".

## 27. Phone install of section 26 (at the student's request)
- **The build:** everything in section 26. The student said "install on the phone and commit". It was installed first, so this record could go into the same commit on `main`, the next commit after `e575e27`.
- **Right before the install:**
  - `assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug` ran again (2026-10-20, 05:57:29 to 05:57:45): BUILD SUCCESSFUL, 79 of 80 tasks up to date.
  - The APK (built at 2026-10-19 22:38:11) was not rewritten. So the installed APK is the one checked on the emulator in 26 (OK, 127 instrumented tests). Its SHA-256 is `fe89a010b71becb2934ca68fb40d1f6304fb518e7f396fa9e9810e536d6a9576`.
  - Unit tests: 321, 0 failures (26.1). Lint: 0 errors, 1 warning, the old `TooManyViews`.
  - The exported database schema (`app/schemas`) didn't change, so the database stays version 3.
- **Before the install** (05:58:36, the computer's clock):
  - Duewise was not running, and the phone's screen was dozing.
  - The phone ran `e575e27`'s APK (SHA-256 `959202d1…`, as in 25). Its last update was 2026-10-19 21:27:35.
  - CAMERA and POST_NOTIFICATIONS were allowed. The daily-check job was scheduled, with its next start about 3 hours later (about 09:02). The crash log had no line for Duewise, and sample data was closed.
  - The listing of `databases`, `files`, and `shared_prefs` was saved: 20 entries, with names, sizes, and times to the nanosecond. The databases themselves were not read.
- **The install** (05:58:49 to 05:59:00): `adb -s ZY22KN9QJP install -r` gave "Success".
- **After the install** (05:59:06):
  - `lastUpdateTime` changed to 2026-10-20 05:59:01, and the first install time stayed 2026-10-04 13:26:35, so it was an update.
  - The listing of `databases`, `files`, and `shared_prefs` was identical to the one before (20 entries, compared line by line).
  - The phone's `base.apk` has the same SHA-256 as the build: `fe89a010…`.
  - CAMERA and POST_NOTIFICATIONS are still allowed. The daily-check job is still scheduled (next start about 09:02). The crash log has no line for Duewise. Sample data is still closed.
- **Not run:** Claude Code did not open the app or tap anything on the phone. The student checks the new screens on the phone, with TalkBack and a large font size too.

## 28. The name spelled "DueWise" again, in the logo too (2026-10-21)
- **The student's request:** "Could you please capitalize the w in Duewise on the phone's app homepage. Change it everywhere, from Duewise to DueWise".
- **What changed:**
  - **Words:** every "Duewise" in `strings.xml` became "DueWise" (30 places, `app_name` included). So the name under the icon, the widget, the notifications, and the notes on the screens say DueWise. Code comments in 10 more files too. Lowercase names (`logo_duewise_horizontal`, the kit's file names) and the code names with "readyshelf" stay.
  - **The logo at the top of Home** (`logo_duewise_horizontal.xml`): its name is one path of letter shapes from the student's logo kit. The "w" became the capital "W" of Instrument Serif Italic, from the kit's own font file (`font/InstrumentSerif-Italic.ttf`, version 1.000).
    - **Checked first:** a script (fontTools 4.67.0, in a temporary Python environment outside the project) drew "Duewise" from that font file and compared it with the kit's path. It was identical, character for character, with each letter 10 font units closer than the font's own spacing and no kerning (the font has no kerning for these letter pairs).
    - **Then** it drew "DueWise" the same way. "D", "u", and "e" are unchanged (checked). "W" is 44 units wider than "w" (653 against 609), so "ise" moved 44 units right.
    - **The vector** is 44 units wider (`viewportWidth` 3861.43 → 3905.43) and 143.65 dp wide instead of 142.03 dp, at the same 32 dp height. So the clear space at its right end is the same. Nothing else in the vector changed.
  - **Records:** CLAUDE.md (the title and the project line), SPEC.md ("Name and icon", the opening paragraph, and the quoted texts), DEMO.md (a twenty-second update note, the script's words, and "Who made what"), THIRD-PARTY-NOTICES.md (where the W's outline came from), this section, and AI-USE.md.
  - **Not changed, on purpose:** the earlier sections of this file and the earlier entries of AI-USE.md, which record what the app said then; DEMO.md's note about the "Duewise" spelling of 2026-10-18; and the logo kit's own files on the student's computer, which still say "Duewise".
- **Code:** uncommitted, on top of `5e02cff`. Claude Code ran everything below on the computer and the Pixel_10a emulator (Android 17). The phone was not connected (`adb devices` listed only the emulator) and was not used.

### 28.1 Build and tests
- `assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug` (19:38:40 to 19:39:14): BUILD SUCCESSFUL, 13 of 80 tasks run.
  - The APK is from 19:38:50; its SHA-256 is `01aaea8cd19b67795af08304669cb4523482d72158a71395f824bda4003d9ed5`. The test APK wasn't rebuilt, because its code didn't change.
  - Lint (report written at 19:39:14): 0 errors, 1 warning, the old `TooManyViews` (`activity_batch_edit.xml`).
  - The unit tests counted as up to date (only comments changed in the Java files). So they ran again with `testDebugUnitTest --rerun` (results written at 19:39:48): 321, 0 failures, 0 errors, 0 skipped.
- Instrumented tests on the emulator (`am instrument -w`, 19:41:29 to 19:41:43): **OK (127 tests)**.
- `connectedAndroidTest` was not used.

### 28.2 By hand on the emulator
- **Install:** `adb -s emulator-5554 install -r` gave "Success". `lastUpdateTime` is 2026-10-21 19:40:09, and the first install time stayed 2026-10-18 11:41:48.
- **Home's logo** (screenshot, and a copy enlarged three times):
  - It reads "DueWise", with the italic capital W. The hourglass, the colors, and the other letters look as before.
  - In `uiautomator`, the logo is one image with the spoken label "DueWise", 377 × 84 px (143.6 × 32 dp at 420 dpi), 42 px (16 dp) from the left.
- **The name under the icon:** the app list shows "DueWise" under the hourglass icon (screenshot). The home screen's row of suggested apps has "DueWise" too (`uiautomator`: "Predicted app: DueWise").
- Claude Code only opened Home and the launcher's app list. It tapped nothing in the app.

### 28.3 Not checked
- The phone: it still shows "Duewise" until this build is installed there.
- The widget and a reminder on screen. Their name and texts come from the changed strings; no widget was placed on the emulator, and no reminder was showing.
- TalkBack by ear (how it says "DueWise"), and Android 8.x.

## 29. Short texts on every screen (2026-10-21)
- **The student's request:** "I notice that there is a lot of wordiness in the app. Make the descrptions in each page short, consice, and to the point. Pages like the Invenbtory, Date passed, and needs review are perfect: No complicatied descriptions. If some pages do need descrpiptions, Make them short, consice, and to the point."
- **What changed:** 164 texts in `strings.xml`: descriptions, notes, field hints, empty-list texts, messages, and dialog texts. Together they went from 2,780 words to 1,643. No code or layout changed. The rules used, and what stayed as it was, are in SPEC.md, "Short texts of 2026-10-21".
  - The script that wrote them stopped unless every name was found once and every text kept the same placeholders (`%1$s`, `%d`, …).
  - Safety sentences stay in view, in fewer words (SPEC.md, "Wording rules").
- **Code:** uncommitted, on top of `5e02cff`, together with section 28. Claude Code ran everything below on the computer and the Pixel_10a emulator (Android 17). The phone was not connected and was not used.

### 29.1 Build and tests
- `assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug` (20:32:16 to 20:32:28): BUILD SUCCESSFUL, 12 of 80 tasks run.
  - The APK is from 20:32:23; its SHA-256 is `bc3273720085295621c8460ae7cee8ca641d2ebe68b276d298213eecb9f4771a`.
  - Lint (report written at 20:32:27): 0 errors, 1 warning, the old `TooManyViews` (`activity_batch_edit.xml`).
  - The unit tests ran again with `testDebugUnitTest --rerun` (results written at 20:32:43): 321, 0 failures, 0 errors, 0 skipped.
- Instrumented tests on the emulator (`am instrument -w`, 20:32:46 to 20:33:01), after installing both APKs: **OK (127 tests)**.
- `connectedAndroidTest` was not used.

### 29.2 By hand on the emulator, with the emulator's own records
- **The same screens before and after:** first with section 28's build, then with this one. Each time `uiautomator` read every text on the screen, scrolled to the end, and a screenshot was taken at each step.
  - The screens: Home, Inventory, Batch details (Beans), Record used (Beans), Purchase list, Settings, Review swap (granola bars), Add batch (it opens with Food chosen), and Label photos.
  - On Label photos, Android's camera question was closed with Back each time. Afterwards CAMERA was still `granted=false`, without a USER_SET flag, as before.
- **Words in the descriptions that changed** (texts found only before or only after; headings, buttons, item data, and spoken labels that didn't change are not counted):

| Screen | Before | After |
|---|---|---|
| Add batch (Food) | 195 | 82 |
| Label photos (camera permission refused) | 83 | 44 |
| Settings | 81 | 54 |
| Review swap | 75 | 44 |
| Purchase list | 50 | 18 |
| Home | 18 | 12 |
| Batch details | 14 | 9 |
| Inventory | 10 | 8 |
| Record used | 0 | 0 |
| **All** | **526** | **271** |

- **Screenshots, before and after side by side:** the top of the form, its date and food sections, Review swap, Settings, Purchase list, and Label photos. Every new text fits, and nothing is cut off.
- The safety sentences in view after the change: "Proposals only. Replacement days are a planning choice, not a safety guarantee." (Home), "“For review” means you check this food yourself. DueWise doesn't check it." and "Replacement days are a planning choice, not a safety guarantee." (Review swap), "Your own records: DueWise doesn't check or judge food." (the form), and "These are planning choices, not safety guarantees." and "… Never names medicine." (Settings).
- Nothing was saved: no form was saved, no swap was confirmed, and no setting was changed.

### 29.3 Not checked
- **Texts that show only in situations that weren't created:** most errors and dialogs, the photo review with its date choices, the sample data screens and dialogs, the medicine notes, the kit warning, and Batch details for an archived or empty batch. Their new words were only read in `strings.xml`, and the build checked them.
- The phone, TalkBack by ear, font scale 2.0, and Android 8.x.

## 30. No note above Home's swap cards (2026-10-21)
- **The student's request:** "Remove the text below "Recommended Swap" . The red section in the screenshot below". The screenshot had Home's note "Proposals only. Replacement days are a planning choice, not a safety guarantee." circled in red.
- **What changed:**
  - `HomeViewModel`: while swap cards show, the swaps box gets no message, so `MessageText` keeps it hidden. "No swaps to suggest right now.", "Not available yet…", and the rules' error still show in it, as before.
  - `strings.xml`: `home_swaps_note` removed (nothing else used it). Comments in `HomeActivity` and `activity_home.xml`.
  - "More about this" above the cards (kit food dated today or earlier is never swapped) still shows with swap cards. It wasn't in the circled part.
  - Review swap didn't change: "Replacement days are a planning choice, not a safety guarantee." and the "For review" note stay in view there, before "Confirm swap".
- **Code:** uncommitted, on top of `7ff7301`. The phone was not connected and was not used.

### 30.1 Build and tests
- `assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug` (21:24:12 to 21:24:28): BUILD SUCCESSFUL, 24 of 80 tasks run.
  - The APK is from 21:24:22; its SHA-256 is `9573e76b723d750c82857b62d5c1428e556dde670b125534eab6c9efbc7790b9`.
  - Unit tests (they ran, because Java changed; results written at 21:24:21): 321, 0 failures, 0 errors.
  - Lint (report written at 21:24:27): 0 errors, 1 warning, the old `TooManyViews`. Nothing about the removed string.
- Instrumented tests on the emulator (`am instrument -w`, 21:24:39 to 21:24:55), after installing both APKs: **OK (127 tests)**.

### 30.2 By hand on the emulator, with the emulator's own records
- **Home:** under "Recommended swaps" come "More about this" and the granola bars' card, with no note (`uiautomator` and a screenshot).
- **Review swap**, opened from that card: "“For review” means you check this food yourself. DueWise doesn't check it." and "Replacement days are a planning choice, not a safety guarantee." are in view. Nothing was confirmed.

### 30.3 Not checked
- Home with no swaps ("No swaps to suggest right now.") or with the swap rules' error. That part of the code didn't change, and neither case was created on the emulator.
- The phone and TalkBack.

## 31. "Graphite light": a light, gray color scheme (2026-10-21 and 2026-10-22)
- **The student's request (2026-10-21):** "I don't like the color scheme. Can you suggest some alternate color scheme according to the best practices".
  - In plan mode the student answered three questions:
    - what bothers them: "The blue", "Too dark", "Dull, all gray-blue"
    - which scheme to preview: "Graphite"
    - how dark: "Show me both"
  - After the comparison page (https://claude.ai/artifact/VMGQH6aDM9pwtf6rDTGePp), the student chose "Graphite light" (2026-10-22).
- **The previews (2026-10-21; no project change):**
  - They were built from `9682acb` in a temporary git worktree and installed on the emulator one after the other: "Graphite dark" (SHA-256 `572f8d81…`) and "Graphite light" (`459e5bc1…`). Then the current build went back on (`9573e76b…`, checked on the emulator afterwards).
  - The same 11 screens of each version were photographed and put side by side.
  - For one photo, sample data was opened and closed again. The date picker was cancelled. Nothing was saved.
- **What changed (2026-10-22):**
  - `values/colors.xml`:
    - the 34 roles in grays (chroma 0, from Material's tonal palettes)
    - the status colors in light tones of the same hues: label tone 90, words tone 10, tile accent tone 40
    - lilac sample tags (#ECBDEF with #2E0D35; hue 325.9, chroma 32, tone 82)
    - the widget's colors, #303030 with #E8E8E8 (it stays dark)
  - `values/themes.xml`:
    - parent `Theme.Material3.Light.NoActionBar`
    - text without its own color from the new `res/color/text_primary.xml` and `text_secondary.xml`
    - the "Add batch" style
  - `values/styles.xml`: `ReadyShelf.AddButton` (the main button color).
  - `ui/common/ScreenSetup.java`: `SystemBarStyle.light(...)`, so the status-bar and navigation-bar icons are dark.
  - `drawable/logo_duewise_horizontal.xml` and `splash_hourglass.xml`: pine #1F3B34 in place of off-white #EEF2F0, the logo kit's colors for light backgrounds. The brass stays, and the widget's hourglass is unchanged.
  - Comments in `HomeActivity`, `StatusLook`, `NoticeLook`, `styles.xml`, `bg_notice_warning.xml`, `item_action_tile.xml`, `widget_review.xml`, and `values-v31/themes.xml`.
  - **The sample tags:**
    - A lavender, the tweak promised before the choice, was measured with Material's color distance (CAM16). It came closer to the light-blue "Date needs review" label (smallest distance 5.1).
    - Today's hue sits halfway between that blue and the "Date passed" red. So that hue was kept, a little deeper and richer: smallest distance 8.8, up from 7.3 in the preview.
- **Code:** uncommitted, on top of `9682acb`. The phone was not connected and was not used.

### 31.1 Build and tests
- `assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug` (2026-10-22, 10:14:24 to 10:15:01): BUILD SUCCESSFUL, 23 of 80 tasks run.
  - The APK is from 10:14:43; its SHA-256 is `7de9f72abc5a638a86c47c1c2ada47f9aa5f3c10441314a53ce46b2dcaf69812`.
  - Unit tests (they ran, because Java changed; results written at 10:14:44): 321, 0 failures, 0 errors.
  - Lint (report written at 10:15:01): 0 errors, 1 warning, the old `TooManyViews`.
- **Instrumented tests on the emulator, after installing both APKs:** **OK (127 tests)**, twice.
  - First run: 10:16:18 to 10:23:01 ("Time: 345.93").
  - Second run, with the screen awake: 10:23:23 to 10:27:36.
  - Both were much slower than the usual 15 seconds. The slowest tests were database tests that use no screen and no color: MigrationTest took 9–21 s each, and DataSetsTest about 6–8 s each. So the emulator itself was slow that morning, after running overnight.

### 31.2 Contrast (WCAG), computed from the final colors

| What | Colors | Contrast |
|---|---|---|
| Text on the background | #1B1B1B on #F9F9F9 | 16.36:1 |
| Text on a card or tile | #1B1B1B on #E8E8E8 | 14.06:1 |
| Secondary text on a tile | #474747 on #E8E8E8 | 7.58:1 |
| Words on a filled button | #FFFFFF on #353535 | 12.27:1 |
| Words on a note or tonal button | #1B1B1B on #E2E2E2 | 13.30:1 |
| Sample strip and tag words | #2E0D35 on #ECBDEF | 10.69:1 |
| "Later date" label | #474747 on #E2E2E2 | 7.17:1 |
| Status labels (passed, today, near, review) | words on their pale labels | 13.25–13.27:1 |
| Status numbers on a tile | #B62320, #8B5018, #735B19, #2B5DAC on #E8E8E8 | 5.25–5.29:1 |
| Save messages | #F1F1F1 on #303030 | 11.69:1 |
| Outlines | #777777 on #F9F9F9 | 4.25:1 |
| The widget (stays dark) | #E8E8E8 on #303030 | 10.77:1 |
| The logo's pine | #1F3B34 on #F9F9F9 | 11.51:1 |

### 31.3 By hand on the emulator (the final build, 10:28 to 10:42)
- The same tour as for the previews, with the emulator's own records. Nothing was saved.
- **Start-up screen:** light, with the pine-and-brass hourglass and dark status-bar icons.
- **Home:**
  - the pine logo
  - pastel status tiles: "Date passed" 2, "Date is today" 1 (a batch dated 22 Oct), and "Near date" 3
  - gray icon circles, and a charcoal "Add batch"
- **Other screens:**
  - Inventory: pale labels with dark words.
  - Batch details.
  - The Add batch form, with the charcoal "Review and save" bar.
  - Review swap, with both safety sentences and the charcoal "Confirm swap" bar.
  - Settings, with the dark switch.
- The final build looks like the "Graphite light" preview, except for the deeper lilac of the sample tags.
- **The date-picker photo** in this run was taken before the dialog appeared, because the emulator was slow. The light date picker was photographed in the preview, with the same theme files.
- **Sample data:** not photographed. On the slow screen the tour couldn't find "Open sample data" while scrolling Settings, so it stopped there and never opened sample data. The emulator was back on Home with its own records afterwards. The lilac's contrast is in 31.2.

### 31.4 Not checked
- The phone (not connected), TalkBack, font scale 2.0, and Android 8.x.
- **The widget on a home screen.** Only its two colors changed: from #1F1F23 / #E3E2E6 to #303030 / #E8E8E8.
- **The phone in dark mode.** The theme is light only, so the app is expected to stay light, but this wasn't tried.
- **The sample tags' new lilac on screen.** Only the preview's lighter lilac was photographed.

## 32. New icons: Google's Material Symbols, Rounded (2026-10-22)
- **The student's request (2026-10-22):** "Next, I want to improve the icons. Can you look at the icons on the home screen and suggest some icons as per the best design principles?"
  - In plan mode Claude Code reviewed Home's icons against design rules (one style, one meaning per icon, familiar pictures, what needs attention stands out, visible circles, icon contrast, no check marks or "×" on statuses) and wrote a plan with suggestions. The student approved it.
  - A private preview page (https://claude.ai/artifact/UzqnGcgKVMprDwG4v9BaWe) drew Home four times in the browser, from the real icon shapes: Now, A (smallest change), B (Material Symbols Rounded, recommended), and C (Material Symbols Outlined).
  - The student chose "B · Rounded" and "Yes, a bell" for Settings' Reminders card.
- **What changed:**
  - **26 icon files redrawn, same names** (so the code that uses them didn't change): Material Symbols, Rounded style, 24 px, downloaded from fonts.gstatic.com.
    - Solid (fill 1): `ic_error`, `ic_today`, `ic_clock` ("schedule"), `ic_help`, `ic_warning`.
    - Outline: the other 21, from `ic_calendar` ("calendar_today") to `ic_dataset`.
    - Each path was copied unchanged (checked by the converter). The shapes are drawn from y = -960 to 0, so each file moves them into its 960 × 960 viewport with a group.
  - **5 new icon files:** `ic_list_alt` ("Show the review list"), `ic_shelves` (pantry), `ic_location` (other places), `ic_category` (the category Other), `ic_bell` (Settings' Reminders card).
  - `HomeActivity`: the review-list tile has the list. `PlaceLook`: pantry → shelves, other places → map pin. `CategoryLook`: Other → shapes.
  - `activity_home.xml`: no icon on "Items need review". `item_action_tile.xml`: a white circle (`colorSurfaceContainerLowest`) and a gray icon (`colorOnSurfaceVariant`). `activity_settings.xml`: the bell.
  - `item_filter_chip.xml`: `chipIconTint` gray.
    - **Found during this check:** the batch form's category chips showed their icons in the files' own white, because Material's filter chip doesn't tint its icon. On the light app, since this morning's "Graphite light", the icons almost vanished (white on #F9F9F9 is 1.05:1). The "before" photo shows it too, and the thin outline icons made it worse. Now 8.82:1.
  - Comments; SPEC.md, THIRD-PARTY-NOTICES.md, and DEMO.md.
  - Unchanged: `ic_notification.xml` (the notification's shelf), the logo, and the widget.
- **Code:** uncommitted, on top of `26c8262`. The phone was not used.

### 32.1 Build and tests
- `assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug --rerun-tasks` (2026-10-22, 11:56:01 to 11:56:21): BUILD SUCCESSFUL, 80 of 80 tasks run.
  - The APK is from 11:56:15; its SHA-256 is `920d6b2a5e809c5100ab114d19a5fe1ba30fc0341c4aad45f5789854cade5be1`.
  - Unit tests (written at 11:56:17): 321, 0 failures, 0 errors. `CategoryLookTest.noTwoCategories_shareANameOrAnIcon` still passes with the shapes for Other.
  - Lint (report written at 11:56:20): 0 errors, 1 warning, the old `TooManyViews`. The new icons gave no long-path (`VectorPath`) warning.
  - Three earlier builds this session (11:33, 11:41, and 11:45, the last with `--rerun-tasks`) also passed with 321 unit tests, 0 failures, and the same lint result. They were before the chip fix.
- **Instrumented tests on the emulator, after installing both APKs** (`adb -s emulator-5554 install`, then `am instrument -w`): **OK (127 tests)**, 11:59:44 to 11:59:59 ("Time: 12.888").

### 32.2 Contrast (WCAG), computed

| What | Colors | Contrast |
|---|---|---|
| Icons on the white circles (review list, Inventory, Purchase list, Settings) | #474747 on #FFFFFF | 9.29:1 |
| Status icons on their pale circles (passed, today, near) | #B62320 on #FFDAD6, #8B5018 on #FFDCC3, #735B19 on #FFDF93 | 5.00–5.01:1 |
| Category chip icons on the batch form | #474747 on #F9F9F9 | 8.82:1 (before: #FFFFFF, 1.05:1) |
| Tile arrows | #474747 on #E8E8E8 | 7.58:1 |
| Category icons on batch cards (colors unchanged) | #1B1B1B on #E2E2E2 | 13.30:1 |
| The white circle against its tile (no WCAG minimum; it only has to show) | #FFFFFF on #E8E8E8 | 1.23:1 (the old gray circle: 1.06:1) |

### 32.3 On the emulator
- **"DueWise isn't responding" before any change (11:30):** the first "before" photo tour ran on the committed build (`26c8262`, SHA-256 `7de9f72a…`). It stopped after one photo with Android's "isn't responding" dialog.
  - The event log: `am_anr … HomeActivity is not responding. Waited 15002ms for FocusEvent(hasFocus=true, …)`.
  - The emulator had been up 16 h 15 min, with a load average of 10.9–14.0. Google Play services had its own "not responding" at 09:56, and Bluetooth had crashed at least 3 times that morning (07:03–07:07).
  - So the emulator was overloaded. This doesn't point to an app bug, but it doesn't prove there is none.
  - `adb reboot` got stuck (no `system_server`, no shell answer). The emulator was stopped with `emu kill` and cold-booted at 11:45 (`-no-boot-anim -no-snapshot-load`). Its data was kept. The load fell to 3.9 before the tours, and to 0.1 later.
- **Photo tours** (the scratchpad's `ui.py icontour`, with the emulator's own records; nothing was saved):
  - Screens: Home; Home with the review list open (3 screens); Inventory (2); Batch details (Beans); Add batch (opened, then left with nothing entered); Settings (2); and Home at font size 2.0. The font scale went back to 1.0 right after.
  - "Before": 11:48–11:51, on the committed build. "After": 11:56–11:59, on the final build.
  - Each tour closed the review list again at the end.
- **What the "after" photos show:**
  - Home: no icon on "Items need review"; the solid "!", calendar, and clock in the pale status circles; the gray list, box, cart, and gear in white circles; and the shelves for Pantry and the backpack in the swap card.
  - The review list: outline category icons, and the status labels with solid icons, the blue "?" too.
  - Batch details (Beans): the box (quantity), the shelves (Pantry), the fork and knife, and the calendar.
  - Add batch: gray outline icons on the category chips, the shapes on "Other" too.
  - Settings: the bell (Reminders), the sliders (Planning settings), and the grid (Sample data).
  - At font size 2.0: one tile per row; the icons stay 24 dp in 40 dp circles.
- The before and after photos are side by side on the preview page.

### 32.4 Not checked
- The phone (not used), TalkBack, landscape, and Android 8.x.
- The Purchase list, Review swap, Move, and the stock screens. They use the same icon files but weren't photographed.
- The map pin and the shapes on a real batch: none of the photographed batches is in another place or in the category Other. The shapes were seen on the Add batch chip.
- The warning and error boxes with the new signs, and the "Archived" tag with the new archive icon.
- The reminder notification and the widget (both unchanged).

## 33. Home without the "Items need review" heading, the review-list tile, and the review lists (2026-10-22)
- **The student's request (2026-10-22, after commit `f377c14`):** "Remove "Items need review" text and also hide the "Show the review list" tile - this is redundant".
  - Without that tile nothing on Home could open the review lists, and "Date needs review" has no tile. So Claude Code asked two questions first:
    - What should happen to the lists? The student answered: "Medicine Date to review should show under 'Date passed' or "near date" as the case may be".
    - Add a "Date needs review" tile? The student answered "No tile".
  - So both review lists left Home. Medicine already counted in the status tiles, and a tile's tap already showed it in Inventory, so the counting didn't change.
- **What changed:**
  - `activity_home.xml`: no "Items need review" heading, and no review section ("Needs review", and "Medicine dates to review" with its note). The note `home_review_message` moved under the grid.
  - `HomeActivity`: no review-list tile, no lists, and no open/closed state. The grid has the status tiles (still none for "Date needs review") and the three shortcuts.
  - `HomeViewModel`: no lists and no medicine split. The counts come from the same list as before (`RuleRunner.needsReview`, medicine too).
    - The note says "Nothing needs review right now." when the household has batches and none needs review.
    - It shows the date rules' message when they are not connected or threw an error.
    - With no batches at all there is no note; the getting-started message shows.
  - **Removed, because nothing used them any more:**
    - 7 strings: `home_needs_review`, `home_medicine_review`, `home_medicine_note`, `home_medicine_empty`, `home_attention_title`, `home_attention_show`, `home_attention_hide`
    - the id `home_attention_toggle`
    - `ic_list_alt.xml`, which was added that morning for the tile
  - Comments in `item_action_tile.xml`, `ic_expand_more.xml`, `ic_expand_less.xml`, and `strings.xml`.
  - Docs: SPEC.md ("Home banner" and the other places that described Home's lists), DEMO.md (steps 2, 7, and 9, and two new lines in its log), and THIRD-PARTY-NOTICES.md.
  - Unchanged: the reminders, the widget, Inventory, the status labels, DatePolicy, RotationPlanner, DateCandidateParser, and the database.
- **Code:** uncommitted, on top of `f377c14`. The phone was not used.

### 33.1 Build and tests
- `assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug --rerun-tasks` (12:46:30 to 12:46:47): BUILD SUCCESSFUL, 80 of 80 tasks run, no compiler warnings.
  - The APK is from 12:46:41; its SHA-256 is `50da1e21ca88f7a783edc5e659e2e3ec9746a1ec60954768dbeac07c83d88a88`.
  - Unit tests (written at 12:46:43): 321, 0 failures, 0 errors.
  - Lint (report written at 12:46:47): 0 errors, 1 warning, the old `TooManyViews`. No unused-resource warning.
- **Instrumented tests on the emulator, after installing both APKs** (`adb -s emulator-5554 install`, then `am instrument -w`): **OK (127 tests)**, 12:48:07 to 12:48:23.

### 33.2 On the emulator (its own records; nothing was saved)
- **Home (12:47):**
  - The logo, then the grid: "Date passed" 2, "Date is today" 1, "Near date" 3, Inventory, Purchase list, and Settings, in 3 full rows.
  - "Recommended swaps" and its card are on the first screen.
  - No heading, no review-list tile, no lists, and no note (there are batches to review).
- **A tap on "Date passed":** Inventory opened with "Filter: Date passed" and "Showing 2 of 9 batches": Pain reliever tablets (medicine, "Status: Date passed") and Beans.
- **Font size 2.0:** one tile per row. The round "+" covers the end of the Settings tile until Home scrolls, as before. The font scale went back to 1.0 right after.
- **Sample data** (opened, photographed, and closed again): "Date passed" 1 and "Near date" 2, then Inventory and Purchase list, and Settings with an empty cell next to it. Afterwards Home had no "Close sample data" button, so the sample data was closed.

### 33.3 Not checked
- **The note on screen.** "Nothing needs review right now." wasn't seen, because the emulator's records and the sample data both have batches to review. The date rules' message wasn't seen either, because the rules are connected and ran without an error.
- A Home whose only batches to review have a date to review: expected no status tile and no note, with Inventory's "Date needs review" filter listing them.
- The phone, TalkBack, and landscape.

## 34. Purchase list without "Suggestions", and expired items as replacement needs (2026-10-22)
- **The student's request (2026-10-22):** "Remove the "Suggestions" functionality from the purchase list. Things that are expired should be added under the "Replacement needs" unless there is a swap available".
  - Claude Code asked two questions first, because the suggestions were the only way onto "On your purchase list", and the list accepted only kits.
  - Which expired items? The student answered "Everything you'd buy again": food, water, first aid supplies, cosmetics, and Other, in any place. Not medicine (DueWise gives no advice about replacing medicine), and not documents or subscriptions (renewed, not bought). "Expired" means dated today or earlier, the rule kit food already used.
  - How should items get on the list? The student answered "Add button on each need".
  - "Unless a swap is available" already held: kit food that a swap covers gets no need, and expired food is never swapped.
- **What changed:**
  - `RuleRunner.needs`: the expired check now covers those 5 categories in any place (until then only kit food). Notes from the swap rules still count only for kit food. RotationPlanner (the swap rules) didn't change.
  - `PurchaseSuggestions.from`: adds up every need that RuleRunner gives; it no longer drops needs outside kits. It now only gives the totals for the button and for "Needed now".
  - `PurchaseListStore.add`: an item can be for any existing place (until then only a kit). The database didn't change: the column is still `kitLocationId`, and the schema is still version 3.
  - **The Purchase list screen:**
    - No "Suggestions" section.
    - Each need's card has "Add to purchase list" (the same confirmation as before). Instead of the button, a card says "On your purchase list." when its product and place are already on the list, or "Sample items can't go on the purchase list." for sample data.
    - List items say "Needed now: …" or "Not needed right now." (until then "Suggested now").
    - The intro says "What to replace, and what you plan to buy.", and the empty list says "Empty. Add items from Replacement needs below."
  - The shared batch card (`item_batch.xml`, `BatchRow`, `BatchAdapter`) can now show one button. Only the Purchase list uses it.
  - Expired batches outside kits say "The date is today or has passed." Kit food keeps "… No pantry use is recommended.", because that sentence is about not moving kit food to the pantry and doesn't fit food that is already there.
  - **Removed:**
    - `item_purchase_suggestion.xml`
    - 6 strings: the five Suggestions texts and `rules_not_ready_suggestions`
  - **Renamed:** 5 strings, now `purchase_need_add`, `purchase_need_add_description`, `purchase_need_sample`, `purchase_entry_needed_now`, and `purchase_entry_not_needed`
  - **New:** `purchase_need_on_list` and `plan_reason_date_today_or_passed_outside_kit`.
  - **Tests:**
    - `RuleRunnerTest`: one test renamed, one changed (expired pantry food is now a need, so it moved out of the "no need" test, which gained an expired document and subscription), and 2 new: expired items bought again, in any place; a kit batch that a swap covers gets no need, but an expired one does.
    - `PurchaseSuggestionsTest`: the kit-only test became "needs in any place are added up, but a need of 0 is not".
    - `PurchaseListStoreTest`: a new test for the pantry being accepted, and a missing place in place of the pantry in the "saves nothing" test.
  - Docs: SPEC.md ("Purchase list" rewritten, and 11 other places), DEMO.md (step 6, its log, and "Who made what"), and comments in RotationPlannerContract, PurchaseItem, PurchaseItemInfo, and PurchaseListStore.
- **Code:** uncommitted, on top of `f377c14`, with section 33's change. The phone was not used.

### 34.1 Build and tests
- `assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug --rerun-tasks` (13:22:14 to 13:22:29): BUILD SUCCESSFUL, 80 of 80 tasks run, no compiler warnings.
  - The APK is from 13:22:23; its SHA-256 is `42869eeaed91d47de14895c6a4c607da510b004f9e2e89cfd5a30ad9965590ba`.
  - Unit tests (written at 13:22:26): 323 (2 new), 0 failures, 0 errors.
  - Lint (report written at 13:22:29): 0 errors, 1 warning, the old `TooManyViews`.
  - The build before the reason-text fix (13:19:54 to 13:20:22) also passed: 323 unit tests, 0 failures, and the same lint.
- **Instrumented tests on the emulator, after installing both APKs of the final build** (`adb -s emulator-5554 install`, then `am instrument -w`): **OK (128 tests)**, one new, 13:27:15 to 13:27:31.

### 34.2 On the emulator (its own records)
- **Before** (the build of section 33): "On your purchase list" was empty. "Suggestions" had "Canned beans, Suggested: 4 cans for Earthquake kit". "Replacement needs" had only the canned beans in the Earthquake kit.
- **After** (the final build), "Replacement needs":
  - Beans, 1 can in Pantry, Best by 18 Oct 2026: "Still needs replacement: 1 cans. The date is today or has passed." with "Add to purchase list".
  - N, 2 cans in Pantry, Best by 22 Oct 2026 (today): the same, with "Add to purchase list".
  - Canned beans, 4 cans in the Earthquake kit: "No pantry batch qualifies as a replacement." with "Add to purchase list".
  - **Not listed:**
    - the expired pain reliever (medicine)
    - EU Visa (a document)
    - the kit's granola bars, which a swap covers
- **Adding and removing:**
  - **Adding** both canned beans (kit) and Beans (pantry) with their buttons showed the dialogs "4 cans of Canned beans — … for Earthquake kit. Only a note: your stock doesn't change." and "1 cans of Beans — … for Pantry. …".
  - **On the list:** "To buy: 4 cans for Earthquake kit" and "To buy: 1 cans for Pantry", each with "Needed now". The two need cards said "On your purchase list."; N kept its button.
  - **Removing** both items again (each confirmed) left the list empty, so the emulator's records ended as they started.
- **Sample data** (opened, photographed, and closed again): the sample canned beans in the Earthquake kit, with "Sample items can't go on the purchase list." and no button.

### 34.3 Not checked
- **Not tried:**
  - "Record as bought" for an item in the pantry. The form only uses the place in its text.
  - "Change to …" after an amount changes.
  - The "Already on your list for this place." message.
- TalkBack on the new button, font size 2.0, landscape, and the phone.

## 35. Settings without the Sample data card (2026-10-22)
- **The student's request (2026-10-22):** "Remove the "Sample data" section under settings but retain the items that were added to the batch from sample data".
  - Claude Code asked no questions. "Keep these items as my own" already copied sample items into the household's records as its own items, not marked sample (`copyFromSampleData` sets `sample = false`). So removing the card can't touch them.
  - Nothing in the database changed: no schema change (still version 3), no migration, and no file deleted.
- **What changed:**
  - `activity_settings.xml`: no Sample data card. Gone are its title and icon, the intro, the status box, "Open sample data", "Close sample data", "Start sample data again", the keep note, "Keep these items as my own", and the old-sample count with "Remove old sample items".
  - `SettingsActivity` and `SettingsViewModel`: the card's code is gone (opening, starting again, keeping, removing old sample items, the busy state, and the message channel that only those used). While sample data is open, the Planning card still says "Sample data has its own numbers. Yours don't change."
  - `SampleDataClose`: Settings no longer calls it, so `restartAtHome` is private. "Close sample data" under the label of every screen works as before.
  - **Texts that pointed to the card:**
    - Home's banner for old sample items no longer ends with "Remove them in Settings."
    - Home's getting-started message is now "No items yet. Tap “+ Add batch” to start." (until now it added ", or try sample data in Settings").
  - **Removed, because nothing used them any more:**
    - 26 strings: 24 of the card's texts and messages, plus `history_sample_added` and `history_kept_from_sample`. Saved history entries keep their words, because the database stores the text itself.
    - 2 plurals: `settings_sample_kept`, `settings_old_sample_count`
    - `ic_dataset.xml`, the card's icon
  - Comments in `Batch`, `DataSets`, `SampleData`, `ReadyShelfRepository`, `DataSetsTest`, and `KeepSampleDataTest` that named Settings' sample-data buttons as the way in.
  - Docs: SPEC.md ("Sample data" and 8 other places), DEMO.md (its log, steps 2, 3, 5, 6, 7, 9, and 10, the items table, "Who made what", and the known limits), THIRD-PARTY-NOTICES.md, and a note in section 7.
  - **Unchanged:** the data code that opens, keeps, and removes sample data (`DataSets`, `SampleData`, `copyFromSampleData`, `removeSampleData`) and its tests, the "Sample data" label, the reminders, the widget, DatePolicy, RotationPlanner, and DateCandidateParser.
- **Code:** uncommitted, on top of `f377c14`, with the changes of sections 33 and 34. The phone was not connected and was not used.

### 35.1 Build and tests
- `assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug --rerun-tasks` (13:51:42 to 13:51:56): BUILD SUCCESSFUL, 80 of 80 tasks run, no compiler warnings.
  - The APK is from 13:51:51; its SHA-256 is `8565368bb1143326be83db6197012b5d6b60ea3e6d2e65edd36798f6e8bf6b79`.
  - Unit tests (written at 13:51:53): 323, 0 failures, 0 errors. No unit test changed.
  - Lint (report written at 13:51:55): 0 errors, 1 warning, the old `TooManyViews`. `UnusedResources` ran (it is not among the report's 39 disabled checks) and found nothing.
- **Instrumented tests on the emulator, after installing both APKs** (`adb -s emulator-5554 install -r -t`, then `am instrument -w`): **OK (128 tests)**, 13:53:06 to 13:53:21. The emulator's installed `base.apk` had the same SHA-256 as the build.

### 35.2 On the emulator (its own records)
- **Before** (13:46, the build of section 34; sample data closed): Settings ended with the Sample data card: "Sample data", "Try DueWise with made-up items. Your records don't change.", and "Open sample data". There was no old-sample part, because no batch in the emulator's records is marked sample.
- **After** (13:53, the new build): Settings has Reminders and Planning settings only, and ends with "Save planning settings". The card's 14 words are gone.
- **The items kept from sample data are still there:** Inventory said "Showing 9 of 9 batches", including the five batches that "Keep these items as my own" copied on 2026-10-18:
  - Pain reliever tablets, 2 bottles in First aid box, Expires 8 Oct 2026
  - Canned beans, 4 cans in Earthquake kit, Best by 1 Nov 2026
  - Granola bars: 6 bars in Earthquake kit (Best by 7 Nov 2026), 6 bars in Pantry (Best by 15 Jun 2027), and 3 bars in Pantry ("07/2027", incomplete date)
  - None of them shows "Sample item", and Home still proposes the granola swap.
- **A phone where sample data was still open,** simulated on the emulator: DueWise was stopped, and its `data_mode` flag was set to true. Its sample file was empty.
  - **Home:** the label "Sample data: made-up items, apart from your records." with "Close sample data", no status tiles, the new getting-started message "No items yet. Tap “+ Add batch” to start.", and "No swaps to suggest right now."
  - **Settings:** the label and its button, Reminders, and Planning settings with "Sample data has its own numbers. Yours don't change." No Sample data card.
  - **Closing:** "Close sample data" in Settings showed "Close sample data?", "Your records come back. Made-up items and changes to them are deleted.", [Cancel] [Close]. **Close** opened Home with the emulator's own records and no label, and `data_mode` was false again.
  - The message "Sample data is closed. Your own records are shown." was not captured: the screen was read about 1.5 s later, and only its top part was listed.
- The crash buffer had no DueWise line. The emulator was left on Settings.

### 35.3 Not checked
- Home's banner for old sample items with its new text. The emulator has no batch marked sample, so the banner didn't show.
- **The phone (not connected).** Whether its records still have items marked "Sample item" from before 2026-10-12 is unknown, because the databases were not read. On 2026-10-20 its sample data was closed.
- TalkBack, font size 2.0, and landscape on Settings.

## 36. A home icon on Inventory after a status tile (2026-10-22)
- **The student's request (2026-10-22, after commit `19b848a`):** "when you click on the "Date Passed" or "Near date" , the screen that opens up has the "Inventory" label at the top, replace that label and the arrow with a home icon and it should take the user back to the home screen". Their screenshot of Inventory after "Date passed" had "Inventory" and the arrow circled and crossed out, with "HOME" written next to them.
  - Claude Code asked no questions. The three status tiles ("Date passed", "Date is today", "Near date") open Inventory the same way, so all three get the home icon.
  - Home's **Inventory** tile was not part of the request, so Inventory opened from it keeps the back arrow and "Inventory".
- **What changed:**
  - `InventoryActivity`: when a status tile opened it (the intent carries a status), `showHomeButton` removes the title and puts `ic_home` in place of the back arrow. It tints the icon `colorOnSurfaceVariant`, the back arrow's gray, and gives it the spoken label "Home".
    - The tap still runs ScreenSetup's `finish()`. Only Home's status tiles open Inventory this way, so Home is the screen under it.
  - New `ic_home.xml`: Material Symbols "home", Rounded style, outline, from the SVG on fonts.gstatic.com (`.../materialsymbolsrounded/home/default/24px.svg`), downloaded on 2026-10-22.
  - New string `navigate_home` ("Home"), and a comment in `activity_inventory.xml`.
  - New instrumented test `InventoryTopBarTest` (3 tests):
    - a status tile: no title, the label "Home", and the screen's own label still "Inventory"
    - a tap on the home icon closes Inventory
    - the Inventory tile: "Inventory" and "Back"
  - Docs: SPEC.md (the log at the top, "Screens", "Home banner", and "Accessibility"), DEMO.md (step 2 and its log), and THIRD-PARTY-NOTICES.md.
  - **Unchanged:** Home, the other screens' top bars, the database (still version 3), DatePolicy, RotationPlanner, and DateCandidateParser.
- **Code:** uncommitted, on top of `19b848a`. The phone was not connected and was not used.

### 36.1 Build and tests
- `assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug --rerun-tasks` (14:18:01 to 14:18:19): BUILD SUCCESSFUL, 80 of 80 tasks run, no compiler warnings.
  - The APK is from 14:18:11; its SHA-256 is `94f8f8cda45bf063ea9dacd2e3b65d734da00cc6be55e669d4f63abda1ddc7dd`.
  - Unit tests (written at 14:18:13): 323, 0 failures, 0 errors. No unit test changed.
  - Lint (report written at 14:18:18): 0 errors, 1 warning, the old `TooManyViews`.
- **Instrumented tests on the emulator, after installing both APKs** (`adb -s emulator-5554 install -r -t`, then `am instrument -w`): **OK (131 tests)**, 3 of them new, 14:18:39 to 14:19:16. The emulator's installed `base.apk` had the same SHA-256 as the build.

### 36.2 On the emulator (its own records; nothing was saved)
- **Before** (14:14, the build of `19b848a`): "Date passed" (2) opened Inventory with the back arrow ("Back", #474747) and "Inventory" at the top, a "Date passed ✕" chip, and "Showing 2 of 9 batches" (Pain reliever tablets and Beans).
- **After** (the new build):
  - **"Date passed"** (14:19): the top bar has only the home icon and no "Inventory". The icon is #474747, the arrow's gray, and its button is 56 dp wide, as the arrow's was. The rest of the screen didn't change.
  - **"Near date"** (14:22): the same icon, with "Showing 3 of 9 batches". A tap on the chip "Near date ✕" showed "Showing 9 of 9 batches", and the home icon stayed. A tap on the home icon showed Home.
  - **"Date is today"** (14:23): the same icon, with "Showing 1 of 9 batches" (N). N's Batch details had its own back arrow and "Batch details". That arrow returned to Inventory, still with the home icon.
  - **Landscape** (`adb emu rotate`, 14:24): Inventory was made again and still had only the home icon, with "Showing 1 of 9 batches". The emulator was turned back to ROTATION_0 afterwards.
  - **The home icon's tap** (14:24:52): Inventory closed, and the Home that was already open (made at 14:19:30) came back (`wm_resume_activity` of the same record). No new Home was made.
  - **Home's Inventory tile** (14:24:58): "Back" and "Inventory", with "Showing 9 of 9 batches". Back returned to Home.
- **Taps that Claude Code didn't send:** the event log shows Inventory closing at 14:19:47, then opening and closing at 14:19:54 to 14:19:57 and 14:20:51 to 14:21:07.
  - These taps came from the emulator's own touch screen (its window). Claude Code's adb taps are "Virtual" input (device −1), and its last tap before them was at 14:19:42.
  - Their results are not counted here. Claude Code's own steps above started after the emulator had had no input for over a minute, and each step was checked in a screen dump.
- The crash buffer had no DueWise line. The emulator was left on Home.

### 36.3 Not checked
- TalkBack: the label "Home" was read from the screen dump, not heard.
- Font size 2.0, and the pressed look (ripple) of the home icon.
- The phone (not connected).

## 37. A simpler Add batch form (2026-10-22)
- **The student's request (2026-10-22, after commit `f070c98`):** "The "Add batch" functionality is very comples and asks for a lot of information. What could be some of the ways to simplify it? Feel free to remove some of the fields that are not mandatory."
  - Claude Code first traced every field to the code that reads it, then asked three questions (AskUserQuestion). The student chose:
    - Food part: "Drop unused + one tick (Recommended)".
    - Date part: "Short label type + full date first (Recommended)".
    - Smaller cuts: "Remove variant and package size" and "Label photos as one button".
- **What changed** (details in SPEC.md, "Simpler form (2026-10-22)"):
  - `activity_batch_edit.xml`:
    - The label photos part is only its button.
    - "More details (optional)" with the variant and package size is gone.
    - The date part has the label line with "Change", the folded label chips, the folded "What is printed?" group, and the button "No full date on the package?".
    - The food part lost the food type chips and the four question chip groups, and got the tick `edit_condition_check` and the note `edit_condition_saved`.
  - `BatchEditViewModel`:
    - the open states of the two folds (`isDateLabelOpen`, `openDateLabel`, `isDateKindOpen`, `openDateKind`, and `putDateKind`, which opens "What is printed?" for any kind but a full date)
    - the tick (`isConditionChecked`, `setConditionChecked`, `getSavedConditionText`)
    - `TypedInput` with name, unit, and quantity
    - `findSameProduct(name, unit)`
    - one review line for the food condition
    - `setFoodType` and `setAnswer` removed
  - `BatchEditActivity`:
    - the new views
    - the tick's listener, the same pattern as the date's tick
    - the typed fields are name, unit, and quantity
    - a missing date opens "What is printed?"
    - pane titles on the two folds
    - `QuestionChips` removed
  - `BatchDetailsActivity`: the Food type and Shelf-stable rows show only when the batch has them.
  - `strings.xml`: 9 strings added; 12 removed because nothing used them any more:
    - the four `condition_*_question` strings
    - `food_type_line`
    - `form_more_details`, `form_variant`, `form_variant_helper`, `form_package_size`, and `form_package_size_helper`
    - `scan_heading` and `scan_intro`
  - Comments: `MoreText`, `activity_batch_details.xml`.
  - Tests:
    - `BatchEditFormTest`: the typed-field order is name, unit, quantity; 10 new tests for the tick, the older answers, the one-line review, the edit's three sentences, and the folds.
    - `BatchEditPhotoResultTest`: the shorter `TypedInput`; 2 new tests: a photo's part of a date opens "What is printed?" and it stays open after "Use the last day of the month"; a full date leaves the folds closed.
  - Docs: SPEC.md (the log at the top, "Screens", "The batch form and its categories" with the new subsection, "Products", "Date entry", "Food condition", "Photo entry", "Accessibility", "Not in this step"), DEMO.md (steps 3 and 4, the log, "Who made what"), and a note in section 7 above (rows C4 and E1).
  - **Unchanged:** the database (still version 3; `app/schemas` unchanged), DatePolicy, RotationPlanner, DateCandidateParser, their contracts and tests, the date tick, and the review before saving.
- **Code:** uncommitted, on top of `f070c98`. The phone was not connected and was not used.

### 37.1 Build and tests
- **First run:**
  - `assembleDebug assembleDebugAndroidTest testDebugUnitTest` (17:22:10 to 17:22:22): BUILD SUCCESSFUL. Unit tests: 323, 0 failures.
  - `lintDebug --rerun-tasks` (17:22:46 to 17:23:05): 0 errors, 2 warnings:
    - the old `TooManyViews`
    - a new `AccessibilityFocus` ("Do not force accessibility focus") at the helper that moved TalkBack's focus to a fold after it opened
  - Fixed: the forced focus was removed. Each fold has an accessibility pane title instead ("Label type (next to the date)", "What is printed?"), so TalkBack can name the part that appears.
- **After the fix:**
  - `lintDebug --rerun-tasks` (17:24:31 to 17:24:44, all 27 lint tasks run; report written at 17:24:43): **0 errors, 1 warning**, the old `TooManyViews`. It now points at `activity_batch_edit.xml:650`.
  - `assembleDebug assembleDebugAndroidTest testDebugUnitTest` (17:24:45 to 17:24:54): BUILD SUCCESSFUL.
    - Unit tests: **323, 0 failures, 0 errors**.
    - The APK is from 17:24:50; its SHA-256 is `dc585e9b5e8187a80ee904f8c294b6d115118f00857b040134ddade00be46254`.
- **Instrumented tests on the emulator, after installing both APKs** (`adb -s emulator-5554 install -r -t`, then `am instrument -w`, 17:25:32 to 17:26:05): **OK (143 tests)**, 12 of them new. The emulator's installed `base.apk` has the same SHA-256 as the build (installed 17:25:21).

### 37.2 On the emulator (its own records)
- **Before** (about 17:08 to 17:16, the build of `f070c98`, whose APK `94f8f8cd…` matched the emulator's):
  - Add batch had 239 words and labels for Food (5 scroll positions from top to end), 124 for Medicine, and 117 for Documents. These counts include TalkBack's labels, each counted once.
  - With the old form, Claude Code added one batch for the edit check below, because the new form can't make one like it: **"Test soup"** (Food, a new product, unit cans, 2 cans in Pantry, Best by 15 Dec 2026, date ticked, food type Canned, Sealed No, Shelf-stable Yes, Damaged and Storage concern Not checked).
  - Its review listed "Food type: Canned", one line for each of the four answers, and "Swap proposals need sealed, damaged, and storage concern answered."
- **After** (the new build):
  - **Food:**
    - 166 words and labels, 4 scroll positions.
    - 31 tappable controls, counted by a script that scrolled the form:
      - 8 categories and "Take label photos"
      - the product box, its arrow, and "New product"
      - 4 locations (this emulator has "Documents Folder" too) and "New location"
      - "Review and save", the quantity, −, and +
      - "Change", "Choose date", the date's tick, and "No full date on the package?"
      - "More about this", 4 food classes, and the food tick
    - Counted the same way as the 53 before the change (3 locations, no arrow), that is 29.
    - "Take label photos" sits right under the categories with no heading.
  - **"Change"** showed "Label type (next to the date)" and the four chips in place of the line. Back then closed the form at once, without "Leave without saving?".
  - **Medicine:**
    - "Label type: Expires". "No full date on the package?" opened "Full date", "Part of a date", and "No date" above the date button, and the screen scrolled up to them.
    - "Part of a date", then `03/2027`, then "Use the last day of the month (31 Mar 2027)" gave "Full date" with 31 Mar 2027 and the month-end note. The tick could be ticked, and the group stayed open.
    - Back asked "Leave without saving?"; **Leave**. Nothing was saved.
  - **Documents:**
    - The heading says "Date", the line "Label type: Expires", and the button "No full date?".
    - With a new product "Test card" and no date, "Review and save" opened the group above the date button and showed "Choose a date, or another answer above." under it, all in view.
    - Left without saving.
  - **Edit of "Test soup":**
    - Its Batch details still shows the Food type (Canned) and Shelf-stable (Yes) rows.
    - The edit form had the tick unticked, with "Saved answers: Sealed: No, Damaged: Not checked, Storage concern: Not checked." under it.
    - **Review and save** without a change: "Nothing was changed."
    - Ticked: the note went away, and the review said "These changes will be saved:", then "Sealed changed from No to Yes.", "Damaged changed from Not checked to No.", and "Storage concern changed from Not checked to No.", with nothing about the food type or Shelf-stable.
    - Unticked again: the note came back, and the review said "Nothing was changed.".
    - Left without saving; Batch details was unchanged.
  - **A new food batch, saved: "Test rice":**
    - The new product asked only for a name and a unit (bags), with no variant or package size. 1 bag in Pantry, Best by 15 Dec 2026, date ticked.
    - With "Change" and "No full date on the package?" open, `adb emu rotate` turned the emulator to landscape (rotation 3): both folds, the date, and the date's tick stayed. It was turned back to rotation 0 afterwards.
    - The review said "Food condition: Not checked. Swap proposals need it checked." With the food tick on, it said "Food condition: Sealed, not damaged, no storage concern".
    - Saved. Its Batch details shows Food class Not classified, Sealed Yes, Damaged No, and Storage concern No, with no Food type or Shelf-stable row.
  - **Record bought item:**
    - **Add to purchase list** on the need for Beans in Pantry (1 can) added it to the emulator's empty list. **Record as bought** opened the form with no photo button and the date part closed.
    - "No full date on the package?" showed the chips with "No date" turned off (`enabled=false`) and its note "Bought items need a package date…".
    - Back closed the form at once.
    - The item was removed from the list again ("Empty. Add items from Replacement needs below.").
  - **Edit of the 3 granola bars ("07/2027"):** "What is printed?" was open by itself at "Part of a date" with `07/2027`, and "Use the last day of the month (31 Jul 2027)" was offered. Back closed the form at once; nothing was saved.
  - **Font size 2.0** (`settings put system font_scale 2.0`, then back to 1.0): "Label type: Best by" and "Change" fit on one line; the date's tick, "No full date on the package?", and the food tick wrap onto more lines; nothing is cut off.
  - Medicine now has 101 words and labels, and Documents 91.
- **The emulator's records now also have** "Test soup" (2 cans) and "Test rice" (1 bag), both in Pantry; nothing else was saved. Font size 1.0, rotation 0, and auto-rotate on, as before.
- The crash buffer had no DueWise line (144,946 lines, from the emulator's own services). The emulator was left on Home.

### 37.3 Not checked
- **TalkBack:**
  - whether the pane titles are read when a fold opens
  - "Change label type"
  - how the tick is read
  - The labels were read from screen dumps only.
- **"Save and add another"** with the new form, and **label photos** with a real photo (both covered only by the ViewModel tests).
- **The other categories:** Water, First aid supplies, Cosmetics, Subscriptions, and Other were not opened. Only Food, Medicine, and Documents were.
- **Landscape with font size 2.0.**
- **The phone** (not connected).

## 38. No food section, and swaps for more categories (2026-10-22, evening)
- **The student's request (after commit `62374fc`):** "The "Add batch" section still has a lot of data entry fields that I want to get rid of . Remove the section in the screenshot [Image #3]. Also , if you are considering only sealed items for replacements, drop that logic and consider everything as a candidate for replacement. I want to keep things simple. Ask me clarifying questions." The screenshot showed the form's "Food condition" part.
  - Claude Code asked three questions (AskUserQuestion). The student chose:
    - What to remove: "Food condition only (Recommended)" (the date part stays).
    - Swaps: "All but medicine, documents, subs".
    - Batch details: "Remove the card (Recommended)".
  - The student approved the plan, which described the changed `RotationPlannerTest` and `RuleRunnerTest` cases in words.
- **What changed** (details in SPEC.md, "No food section, and swaps for more categories (2026-10-22, evening)"):
  - **Swap rules** (`RotationPlanner`): a fixed list of swapped categories (Food, Water, First aid supplies, Cosmetics, Other), checked on the kit side and the pantry side. The food class and condition checks were removed (`whyNotShelfStable`, `hasTheAnswersSwapsNeed`), and only DATE_TODAY_OR_PASSED needs all of a batch. No public signature changed.
  - **`PlanReason`:** NOT_FOOD became CATEGORY_NOT_SWAPPED. INFANT_FORMULA, PERISHABLE_FOOD, UNCLASSIFIED_FOOD, and CONDITION_NOT_CONFIRMED were removed (plans are never stored).
  - **The app's safety net** (`RuleRunner`): swaps and kit needs for the categories the household buys again, not only Food.
  - **The form** (`activity_batch_edit.xml`, `BatchEditActivity`, `BatchEditViewModel`): the food section is gone, with its review lines and history sentences.
  - **Batch details:** the "Food condition" card is gone (`item_detail_row.xml` deleted).
  - **Data** (`ReadyShelfRepository`, `PurchaseListStore`): `addBatch`, `updateBatch`, and `recordPurchase` lost the food-class parameter (`FoodClassChange.java` and both PRODUCT_CHANGED results deleted). `updateBatch` no longer writes the food type and answers, so an edit keeps them.
  - **Texts:** Review swap's note, summary, rules, and "What you recorded", and Home's "More about this" (`SwapReviewViewModel`, `DisplayNames`). In `strings.xml`, 45 strings that nothing used any more were removed, and 6 changed (one under a new name, `plan_reason_category_not_swapped`).
  - **Helpers with no callers left:** `DisplayNames.foodClass` and `foodType`, `BatchText.answer` and `foodType`.
  - **Comments that had become wrong:** `ItemCategory`, `FoodClass`, `CategoryGuesser`, `PlanBatch`, `Product`, `HomeActivity`, `HomeViewModel`, `PurchaseListActivity`, `PurchaseListViewModel`, `SwapText`, `LargeText`, `activity_purchase_list.xml`, `activity_batch_details.xml`, and `styles.xml`.
  - **Tests:**
    - `RotationPlannerTest` (34 → 34): six tests replaced. Removed: not food in a kit, a pantry batch that isn't food, a pantry batch that isn't shelf-stable food, infant formula or perishable or unclassified kit food, and the condition answers on each side. New: medicine, documents, and subscriptions in a kit are never swapped; such a pantry batch is never a replacement; every other category is swapped; the food class and the answers don't matter on either side; and a kit batch and a pantry batch of one product swap even in different categories. "The first reason" and the sample-data test were changed to match. Test batches start with no food class and no answers.
    - `RuleRunnerTest` (28 → 30): a swap of every category bought again is kept; a swap with medicine, a document, or a subscription on either side is dropped; kit needs count for every category bought again (renamed from "only for kit food"); presets that used UNCLASSIFIED_FOOD use PARTLY_REPLACED or NO_REPLACEMENT.
    - Instrumented: `BatchEditFormTest` 21 → 17 (6 tick tests removed; new: a new food batch's review says nothing about food, and an edit of an older food batch names only what the form changed), `PurchaseListStoreTest` 15 → 14 (the food-class test removed), `ReadyShelfRepositoryTest` 7 → 9 (an edit keeps the food type, the answers, and the product's food class, also after a change to Water), `SwapStoreTest` 24 → 26 (a water swap is saved like a food swap; a medicine, document, or subscription pair is never shown, and confirming one anyway saves nothing), and a `batch(…)` helper in `StoreTestData`.
  - **Docs:** SPEC.md, DEMO.md, this section, notes in sections 4.4 (D-1, D-9) and 7, and AI-USE.md.
  - **Unchanged:** the database (still version 3; `app/schemas` unchanged), DatePolicy, DateCandidateParser, the contracts, and the date part of the form.
- **Code:** uncommitted, on top of `62374fc`. The phone was not connected and was not used.

### 38.1 Build and tests
- **After the swap rules changed (first part):** `assembleDebug assembleDebugAndroidTest testDebugUnitTest` (19:04:43 to 19:04:55): BUILD SUCCESSFUL. Unit tests: 325, 0 failures (`RotationPlannerTest` 34, `RuleRunnerTest` 30).
- **After the last code change:**
  - `lintDebug --rerun-tasks` (19:17:31 to 19:17:50; all 27 tasks executed): **"No issues found"**, so 0 errors and 0 warnings. The old `TooManyViews` warning on `activity_batch_edit.xml` is gone, because the form has fewer views now. The manifest merger printed its known warning about the INTERNET removal, and javac its known deprecation note for `NoticeLook.java`.
  - `assembleDebug assembleDebugAndroidTest testDebugUnitTest` (19:18:08 to 19:18:16): BUILD SUCCESSFUL. The unit tests ran (results written at 19:18:15): **325, 0 failures, 0 errors, 0 skipped**.
  - The APK is from 19:18:11. Its SHA-256 is `784ed4f7f6d68a549dd5321ae5c7077edd6aab7decb3748d9130528aa889df34`; the test APK's is `5439dc497e24d3ba3d6be48834ff88beda71d982de2c2ca6cacf3b0cb6f7086f`.
  - `git status app/schemas`: no change.
- **Instrumented tests on the emulator, after installing both APKs** (`adb -s emulator-5554 install -r -t`, then `am instrument -w`, 19:19:10 to 19:19:38): **OK (142 tests)**. Before: 143 (37.1); 4 fewer form tests, 1 fewer purchase-list test, 2 more repository tests, and 2 more swap-store tests. The emulator's installed `base.apk` has the same SHA-256 as the build (installed 19:19:05).

### 38.2 On the emulator (its own records)
- **Before** (the build of `62374fc`, APK `dc585e9b…`): Home had one swap card, Granola bars (6 bars). The Purchase list's needs were Beans (1 can, the date passed), N (2 cans, the date passed), and Canned beans in the Earthquake kit (4 cans, "No pantry batch qualifies as a replacement.").
  - An Add batch form was open on the emulator then, with the product "Test rice" chosen and nothing saved. Claude Code closed it: **Leave without saving?** → **Leave**.
- **After** (the new build, 19:20 to 19:45; the emulator's date 22 Oct 2026, near-date days 30, replacement days 180):
  - **Home:** the same one swap card (Granola bars, 6 bars). "More about this" said "Never swapped: medicine, documents, subscriptions, and kit items dated today or earlier. Kit items to buy again are on the Purchase list."
  - **Purchase list:** the same three needs as before. The expired Pain reliever tablets (Medicine, First aid box, 8 Oct 2026) had no need and no swap; neither had the two documents.
  - **Add batch, Food:** the form ends after "No full date on the package?", with no food section. 25 tappable controls (31 in 37.2): 8 categories, "Take label photos", the product box, its arrow, "New product", 4 locations, "New location", "Review and save", the quantity, −, +, "Change", "Choose date", the date's tick, and "No full date on the package?". Counted like the 53 (3 locations, no arrow), that is 23.
  - **"Test oats", saved:** a new product (unit bags), 1 bag in Pantry, Best by 20 Aug 2027, date ticked. The review listed only the product, category, location, quantity, and date, with nothing about food. Its Batch details has no "Food condition" card. In a copy of the emulator's database (read on the PC with Python's sqlite3), it has no food type, answers, or food class.
  - **Edit of "Test soup"** (saved with the old form in 37.2: Canned, Sealed No, Shelf-stable Yes): its Batch details has no food card. Quantity 2 → 3: the review said only "Count corrected from 2 cans to 3 cans.". Saved. The database copy still has food type Canned, Sealed No, and Shelf-stable Yes, and the history has "Count corrected from 2 cans to 3 cans.".
  - **"Test water":**
    - A Water kit batch: a new product (unit bottles), 6 bottles in the Earthquake kit, Best by 11 Nov 2026 (today + 20), date ticked. Its review had nothing about food. After saving, Home's "Near date" tile went from 3 to 4, Home showed no swap for it, and the Purchase list showed a new "Water" need: "Still needs replacement: 6 bottles. No pantry batch qualifies as a replacement."
    - A Water pantry batch of the same product (chosen from the product list): 6 bottles in Pantry, Best by 10 Jun 2027 (today + 231), date ticked.
    - Home then showed a second swap card, "Test water — no variant · no package size · unit: bottles": Earthquake kit → Pantry, "6 bottles · Best by: 11 Nov 2026 · for review", and Pantry → Earthquake kit, "6 bottles · Best by: 10 Jun 2027". The granola card was still there.
    - **Review swap:** the note "“For review” means you check this item yourself. DueWise doesn't check either item.", the summary "Same product, with a later date at least your replacement days away.", and under **More about this** the rules, ending with "Medicine, documents, and subscriptions are never swapped.", and "What you recorded": "Today: 22 Oct 2026", "Product of both batches: Test water — no variant · no package size · unit: bottles.", and each batch's date and place with "Date and label type confirmed.". The swap was not confirmed.
    - The Purchase list's water need was gone; the other three needs were unchanged.
  - **Rotation:** on a new Add batch form with **Change** and **No full date on the package?** open, `adb emu rotate` turned the emulator to landscape, and both folds stayed open. Three more turns brought it back to rotation 0, and both were still open. Back then closed the empty form at once.
  - **Crash buffer:** 205,561 lines, none naming DueWise (`readyshelf`). No "FATAL EXCEPTION" in the main log, and no `am_anr` or `am_crash` for DueWise, since 19:19.
- **The emulator's records now also have** "Test oats" (1 bag in Pantry) and "Test water" (6 bottles in the Earthquake kit and 6 in Pantry), and "Test soup" has 3 cans. The emulator was left on Home, at rotation 0.

### 38.3 Not checked
- **TalkBack** on the changed screens.
- **Confirming a water swap** by hand (`SwapStoreTest.aWaterSwap_isSaved_likeAFoodSwap` saves one).
- **Swaps of first aid supplies, cosmetics, and Other** on the emulator (only the unit tests and the store test cover the other categories).
- **"Record bought item"** and **"Save and add another"** with the new form (covered only by the store and form tests).
- **Large text** (font size 2.0) on the changed screens.
- **The phone** (not connected).

## 39. The starting kit renamed "Emergency kit" (2026-10-22, evening)
- **The student's request (after commit `2bcba3e`):** "Rename the location "Earthquakr kit" to "Emergency kit"".
  - No place or text in the project is called "Earthquakr kit", so Claude Code took it to mean the starting place "Earthquake kit".
  - The app can't rename a place, so Claude Code asked one question (AskUserQuestion): should the rename also reach the data already saved on the phone and the emulator? The student chose "Rename saved data too (Recommended)". The choice said that this is a database update from version 3 to 4 that can't be undone: older builds can't open the data afterwards.
- **What changed** (details in SPEC.md, "Migration from version 3 to 4 (2026-10-22)"):
  - A new database starts with Pantry, Emergency kit, and First aid box (`AppDatabase.STARTING_LOCATIONS`).
  - The database is version 4, with the same tables as version 3. `Migrations.MIGRATION_3_4` renames the place named exactly "Earthquake kit" with the type Emergency kit, unless a place called "Emergency kit" already exists (in any capital letters). History entries keep their text.
  - **Tests:**
    - `MigrationTest` (6 → 8, instrumented):
      - New: `migrate3To4_renamesTheStartingKit_andKeepsEveryOtherRow`. It writes a version 3 file with four places, two products, three batches, five history entries, a swap receipt, and two purchase-list items. Only the kit's name changes, and the other kit ("Car kit") keeps its name. The app's database code then finds the kit only by its new name.
      - New: `migrate3To4_whenAnEmergencyKitExists_renamesNothing`.
      - The older tests now end at version 4, and the version 2 test expects the kit's new name. The helper `version2Rows` is now `rowsBeforeOpening`, because it also reads version 3 files.
    - `DataSetsTest` and `KeepSampleDataTest` (instrumented) and `SampleDataTest` (unit) expect the new starting name.
  - **Comments:** `SampleData` and `StoreTestData`.
  - **Docs:** SPEC.md ("Locations", "Data kept on the phone", and the new migration section), DEMO.md (every "Earthquake kit", and a note under "The items from sample data"), and AI-USE.md.
  - **Unchanged:** the screens and `strings.xml`; DatePolicy, RotationPlanner, DateCandidateParser, and their tests; and the other tests that use "Earthquake kit" as a made-up place name.
- **Code:** uncommitted, on top of `2bcba3e`. The phone was not connected and was not used.

### 39.1 Build and tests
- `assembleDebug assembleDebugAndroidTest testDebugUnitTest` (20:13:17 to 20:13:28): BUILD SUCCESSFUL. Unit tests (results written at 20:13:27): **325, 0 failures, 0 errors, 0 skipped**.
- The Room Gradle plugin wrote `app/schemas/com.example.readyshelf.data.AppDatabase/4.json`. It differs from `3.json` only in `"version": 4`, so the identity hash is the same.
- `lintDebug --rerun-tasks` (20:13:59 to 20:14:36; all 27 tasks executed): **"No issues found."**
- The APK is from 20:13:25; lint didn't rewrite it. Its SHA-256 is `499f90bcc7c77a3c2844863692fc5af0ee82037deac1b3fd6ab4d943c2895118`; the test APK's is `beeae8fbeabdc1c2f958840ed37f93c3cfca984adcd73107418cc1f1dc5aa5c5`. The emulator's installed `base.apk` has the same SHA-256 as the build (installed 20:15:25).
- **Instrumented tests on the emulator** (the app APK installed first, then `install -r -t` of the test APK, then `am instrument -w`, 20:16:58 to 20:17:35): **OK (144 tests)**. Before: 142 (38.1), so these are the 2 new migration tests.
- `MigrationTest` alone (`-e class`): **OK (8 tests)**, with both new tests named in the output.
- **Right before the commit** (21:02): `assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug` left the APK as it was (20:13:25, `499f90bc…`). The test APK was built again, because the Room plugin copied the new `4.json` into its assets (SHA-256 `8909be98c27578811342f144328092f0c57b3eebcba4e662d4488c8a5bb7d71c`). With it, the instrumented tests on the emulator (21:02:58 to 21:03:37): **OK (144 tests)**.

### 39.2 On the emulator (its own records)
- **Before** (the build of `2bcba3e`, installed 19:19:05): the household file was version 3, with the places Pantry (1), Earthquake kit (2), First aid box (3), and Documents Folder (4).
  - DueWise was open on Home only. The last touch in the emulator's window was at about 20:05, when an Add batch form there was closed. The household file was last written at 19:37, so nothing was saved then.
  - Claude Code copied both database files to the PC (`run-as … cat`; only read on the emulator) and wrote down the version and every row of every table with Python's sqlite3: 59 lines for the household file.
- **The install** (`adb -s emulator-5554 install -r`, 20:15:25): Android stopped DueWise for the update, then started Home again by itself at 20:15:28, because it was on screen. That start ran the update.
- **After the update:** the household file is version 4. Compared with the copy from before, exactly two lines changed: the version (3 → 4), and `2|Earthquake kit|EMERGENCY_KIT` became `2|Emergency kit|EMERGENCY_KIT`. Every other row was the same, including the history entry "Added 6 bottles to Earthquake kit.".
- The sample-data file (sample data closed, no rows) is still version 3, because nothing has opened it since. It gets the same update when it is opened.
- **Screens:**
  - **Home:** the granola swap card shows "Emergency kit → Pantry" and "Pantry → Emergency kit". The name fits in the place box. TalkBack's text is "Move to pantry for review: 6 bars (Best by: 7 Nov 2026) from Emergency kit to Pantry. Into Emergency kit in its place: 6 bars (Best by: 15 Jun 2027) from Pantry.".
  - **Inventory → Filters:** the location chips are All locations, Pantry, Emergency kit, First aid box, and Documents Folder. With Emergency kit chosen and the filters hidden, Inventory showed the chip "Emergency kit ✕", "Showing 3 of 14 batches", "4 cans in Emergency kit", and "6 bars in Emergency kit".
  - **Batch details** of the kit's canned beans: "Location: Emergency kit (Emergency kit)". The line shows the place's name, then its type, and now both are the same words, as in "Pantry (Pantry)".
- **After the instrumented tests:** both files were the same as right after the update. The crash buffer had no DueWise line, and the events log had no `am_crash` or `am_anr` for DueWise.
- The emulator was left on DueWise's Home (with the screens' back arrows, not Back).

### 39.3 Not checked
- **The phone** (not connected). Its data gets the update the first time a build with this change starts.
- **An older build on version 4 data.** Room has no step back to version 3, so such a build stops when it opens the data (not tried).
- **The update of a sample-data file with rows.** Only `MigrationTest`'s files have rows.
- **TalkBack** and **large text** on the screens above.

## 40. B1 "Brass highlights": the student's colors for the whole app (2026-10-23)
- **The student's request (2026-10-22, late evening; the work ran past midnight):** a pasted color spec for "design option B1" with a mock of Home, `B1 Brass highlights@2x.png` (824 × 1846):
  - "Recolor the home screen to match the attached screenshot (design option B1). Keep the layout, text and fonts as they are; only the colors change, plus the pine band behind the header. This replaces any earlier color scheme I gave you. If docs/design/colors.md exists, update it to match."
  - Exact colors for the page, the pine band, the logo, the tiles, the swap cards, and "Add batch".
  - Rules: turn off dynamic color; light status-bar icons on the pine band; no sage or green-tinted grays; light mode only.
  - In plan mode the student answered two questions (AskUserQuestion): **"Whole app"** (the other option was "Home only (Recommended)") and **"Same on both (Recommended)"** for the swap picture on Home and Review swap. The student approved the plan.
- **Found while planning:**
  - Dynamic color was already off: nothing calls `DynamicColors`, and the theme sets every color role.
  - The app has no dark theme (always light since 2026-10-22).
  - `docs/design/colors.md` doesn't exist (there is no `docs/` folder), so SPEC.md records the colors.
  - The mock's colors, sampled from the PNG, are the spec's values. Its band corners measure 26–28 dp.
- **What changed** (details in SPEC.md, "Name and icon" and "Status labels and batch cards"):
  - `values/colors.xml`: B1's colors as Material 3 roles, with warm grays for the roles B1 doesn't name; the named colors pine, brass, brass_light, brass_dark, and cream; the status colors (the labels now have Home's tile colors); a pale brass for warning boxes (#ECDDC3); a light brass text highlight.
  - `values/themes.xml`: the card style, the text highlight, Material's elevation tint off, and comments (dynamic color stays off). `values/styles.xml`: the new `ReadyShelf.Card`, a brass `ReadyShelf.AddButton`, "More about this" in the text color, and the warning colors. `values/dimens.xml`: `band_corner_radius` (28 dp).
  - Drawables: the new `bg_home_band`; white `bg_tile`; white `bg_panel` with a thin border; the warning fill; the logo's three pine colors became #EBEBEA (the brass and every shape are unchanged).
  - Layouts: Home's pine top bar and band (`activity_home.xml`), the tiles' gray chevrons and cream circles, a filled pine "Review swap", lighter swap arrows, and outlined batch cards.
  - Java: `ScreenSetup.apply(…, darkTopBar)` (light status-bar icons; the top bar takes the status bar's height), `HomeActivity`, `SwapDiagram` (gray place icons, "for review" in dark brass and medium weight), `NoticeLook` (warning colors), and comments in `StatusLook`.
  - `values/strings.xml`: "for review" is its own string (`swap_diagram_for_review`), so it can be styled; the words on screen are the same.
- **Code:** uncommitted, on top of `5372776`. The phone was not connected and was not used.

### 40.1 Build and tests
- `assembleDebug assembleDebugAndroidTest testDebugUnitTest` (06:07:08 to 06:07:41): BUILD SUCCESSFUL in 32s, 16 of 71 tasks executed.
  - Unit tests (results written at 06:07:40): **325, 0 failures, 0 errors, 0 skipped**.
  - The APK is from 06:07:39; its SHA-256 is `814e4a185936606b623fbb7d176b2cad5a1fe6cd028e5344a18d661db7872eaf`. The test APK didn't change (21:02:38, `8909be98…`).
- `lintDebug --rerun-tasks` (06:08:19 to 06:09:02; all 27 tasks executed): **"No issues found."** The APK was not rewritten.
- **On the emulator:** `install -r` at 06:09:09; the installed `base.apk` has the same SHA-256 as the build. Then `install -r -t` of the test APK and `am instrument -w` (06:09:48 to 06:10:24, "Time: 32.773"): **OK (144 tests)**.
- **Built again after the tour:** one comment in `HomeActivity` was rewrapped (a line was too long), which moves line numbers in the compiled code.
  - `assembleDebug assembleDebugAndroidTest testDebugUnitTest` (06:19:02 to 06:19:09): BUILD SUCCESSFUL in 7s, 7 of 71 tasks executed. Unit tests ran again (results written at 06:19:08): **325, 0 failures, 0 errors, 0 skipped**.
  - `lintDebug --rerun-tasks` (06:19:09 to 06:19:32; all 27 tasks executed): **"No issues found."**
  - **The final APK** is from 06:19:06; its SHA-256 is `a81271e5528afb22e602cc9df0d2af2582bdf17d4b6b0d3fc3ce6815a4561a53`. Installed on the emulator at 06:19:48; the installed `base.apk` has the same SHA-256.
  - `am instrument -w` with it (06:19:52 to 06:20:26, "Time: 30.799"): **OK (144 tests)**.
- **Right before the commit** (07:11:46 to 07:11:48): `assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug`: BUILD SUCCESSFUL, 79 of 80 tasks up to date. Only `lintDebug` ran ("No issues found"). The APK was not rewritten (06:19:06, `a81271e5…`), so the commit holds the app checked on the emulator above.

### 40.2 Contrast (WCAG), computed from the final colors.xml
A scratchpad script read every color from `colors.xml` (following its `@color/` links). Text needs 4.5:1; icons, arrows, outlines, and the large tile numbers need 3:1. **All 33 pairs pass.**

| What | Colors | Contrast |
|---|---|---|
| Text on the page | #1D1D1C on #F5F5F4 | 15.46:1 |
| Text on a tile or card | #1D1D1C on #FFFFFF | 16.87:1 |
| Detail text on a card / on the page | #5F5F5C on #FFFFFF / #F5F5F4 | 6.41:1 / 5.87:1 |
| Chevrons and swap arrows on white | #858582 on #FFFFFF | 3.70:1 |
| Outlines (fields, outlined buttons) on the page | #858582 on #F5F5F4 | 3.39:1 |
| Words on a chip, note, or tonal button | #1D1D1C on #EBEBEA | 14.14:1 |
| Place icon on its chip | #5F5F5C on #EBEBEA | 5.37:1 |
| "for review" on a white card / on the page | #82622A on #FFFFFF / #F5F5F4 | 5.63:1 / 5.16:1 |
| Words on a pine button; status-bar icons on pine | #FFFFFF on #1F3B34 | 12.11:1 |
| Logo lines and name / logo sand on pine | #EBEBEA / #B8914F on #1F3B34 | 10.16:1 / 4.15:1 |
| "Add batch" words and icon | #1D1D1C on #CFA866 | 7.59:1 |
| Shortcut-tile icon in its cream circle | #1F3B34 on #F6EFE2 | 10.59:1 |
| Date passed label (words and icon) | #FFFFFF on #B23A2B | 5.94:1 |
| Date is today label | #1D1D1C on #B8914F | 5.79:1 |
| Near date label | #82622A on #F6EFE2 | 4.92:1 |
| Date needs review label (unchanged) | #001A41 on #D8E2FF | 13.27:1 |
| Later date label | #5F5F5C on #E1E1DF | 4.89:1 |
| Tile numbers on white: passed, today, near | #B23A2B, #82622A, #1D1D1C | 5.94:1, 5.63:1, 16.87:1 |
| Warning words / sign on the warning box | #1D1D1C / #82622A on #ECDDC3 | 12.61:1 / 4.21:1 |
| Error words on the error box (unchanged) | #410002 on #FFDAD6 | 13.26:1 |
| Field errors on the page | #B23A2B on #F5F5F4 | 5.45:1 |
| Sample strip and tag words (unchanged) | #2E0D35 on #ECBDEF | 10.69:1 |
| Save messages | #F5F5F4 on #30302E | 12.12:1 |
| Words in a dialog | #1D1D1C on #E6E6E5 | 13.51:1 |
| Switch track (on) on a white panel | #1F3B34 on #FFFFFF | 12.11:1 |
| The widget (unchanged, stays dark) | #E8E8E8 on #303030 | 10.77:1 |

- **Not needed for reading, for the record:** "Add batch" against the page 2.04:1 (its words, 7.59:1, say what it is), the Near date circle or label against a white card 1.14:1 (its words and icon carry it), a white card against the page 1.09:1 with its #DCDCDA border 1.26:1, the warning box against the page 1.23:1, and a gray note against a white panel 1.19:1.

### 40.3 On the emulator (its own records; nothing was saved)
- **Photos:** a scratchpad script (`tour.py`) photographed every screen the same way before and after, and saved the screen's text with each photo. Forms were left with the back arrow ("Leave" when asked), the date picker was cancelled, Record used stopped at its review, and Review swap was never confirmed. The comparison is on a private web page (an Artifact): https://claude.ai/artifact/UjgR8ijPmztVTphBGzTBL5.
- **Before** (the build of `5372776`, `499f90bc…`, 05:59 to 06:06): 25 photos.
  - Home (top and swap cards), Inventory and its filters, Inventory from the "Near date" tile, Batch details, and Record used with its review (the kit warning).
  - Add batch, the date picker, "Leave without saving?", and Label photos without the camera permission (Android's question dismissed).
  - Review swap with "More about this", Purchase list, and Settings.
  - The date had passed midnight, so Home showed "Date passed" 3 and "Near date" 4 and no "Date is today" tile (the test batch "N" is dated 22 Oct).
- **After:** the same 25 screens, plus three more:
  - "No changes to save.", Home in landscape, and Home at font scale 2.0 (set back to 1.0 afterwards).
  - Builds: APK `814e4a18…` (06:10 to 06:18), then the Home, Purchase list, Settings, and save-message photos again with the final APK `a81271e5…` (06:20 to 06:25).
  - **Three steps failed in the first run:** Purchase list, Settings, and the save message. Home was still scrolled down from Review swap, so the tiles were off screen. The tour then scrolled Home back to the top, and those steps were taken again.
  - **The save message:** Batch details → Edit batch → Review and save showed "Nothing was changed.", then Confirm and save. Batch details then showed "No changes to save."
    - An edit with no change saves nothing (`BatchEditViewModel`), and Batch details' history showed no new entry.
    - The first photo caught the message sliding in, so a burst of photos was taken again.
- **Home compared with the mock, measured:** 14 pixels of the final Home photo, read at places from the screen dump, are exactly B1's colors.
  - The status bar's area, the top bar, the band between and below the tiles: #1F3B34.
  - The band's rounded corner, which shows the page, and the page under the band: #F5F5F4.
  - A tile: #FFFFFF. The Date passed circle: #B23A2B. The Near date circle and a shortcut circle: #F6EFE2.
  - The swap card: #FFFFFF. A place chip: #EBEBEA. Review swap: #1F3B34. Add batch: #CFA866.
  - The status bar's icons are white on the pine.
  - **Expected differences from the mock:** "Emergency kit" (the mock says "Earthquake kit"), the app's own text sizes, no "Date is today" tile (no batch dated today), and Settings alone in the last row (5 tiles).
- **The other screens** (seen on the photos):
  - **Inventory:** white bordered cards; red "Date passed" labels with white words; cream "Near date" labels with dark brass words; the brass Add batch. Filters: chosen chips light gray with a check mark, the others outlined.
  - **Batch details:** white bordered panels; a pine Record used; light gray Edit batch and Move; pine words on Record discarded and Archive; pine dots in the history.
  - **Record used:** a pine radio button; the kit warning in pale brass with a dark brass sign; a pine Confirm (not tapped).
  - **Add batch:** chips, outlined fields, pine text buttons, and a pine Review and save. The date picker has a pine header, chosen day, and buttons. "Leave without saving?" has pine buttons.
  - **Label photos:** the camera warning in pale brass.
  - **Review swap:** the same swap picture as on Home, "More about this" in dark text with a gray arrow, a white panel for the table, and a pine Confirm swap (not tapped).
  - **Purchase list:** white cards and light gray buttons.
  - **Settings:** white panels, the pine bell and planning icons, and the pine switch.
  - **The save message:** dark warm gray with light words.
  - **Font scale 2.0:** one tile per row on the band, and a round brass "+".
  - **Landscape:** the band and light status-bar icons, but a light strip about 139 px (53 dp) wide on the right edge, from top to bottom (see 40.4).
- **Afterwards:** rotation 0, font scale 1.0, and DueWise on Home. As far as the screens show, the emulator's records are as before: the same counts, Canned beans still 4 cans, and no new history entry. The database files were not compared.

### 40.4 Not checked, and remaining problems
- **Landscape strip (remaining problem):** in landscape the root view keeps clear of the camera cutout on one side (its padding), and the page color shows there next to the pine top bar and band. It didn't show while the whole app was light. Suggested fix: let the top bar and the band reach that edge while their contents stay clear of the cutout.
- **Not seen:** the brass "Date is today" tile and label (no batch on the emulator is dated today), the start-up screen (only its background changed, to the page color), the "Sample data" strip (sample data can't be opened since 2026-10-22), and the widget (unchanged).
- **Not run:** the phone (not connected), TalkBack, and Android 8 to 11 (the emulator runs Android 17).

## 41. Phone install of section 40, and the phone's update to database version 4 (2026-10-23)
- **The student's request:** "commit and install". Section 40 was committed as `d2ac85a` at 07:12. The phone wasn't connected then, so Claude Code waited for it. It appeared at 07:23:59.
- **Before** (read-only checks; no records were read):
  - **The phone ran `499f90bc…`**, the build of `5372776` (the Emergency kit rename, section 39), installed at 2026-10-22 21:11:16 (`lastUpdateTime`). That install wasn't recorded at the time; this line records it from the phone's own dates.
  - **The household's data was still version 3:** the main database file's header said 3, and `readyshelf.db`, `-wal`, and `-shm` still had their old times (12, 18, and 20 Oct). So DueWise hadn't been opened since that install, and the one-way update to version 4 hadn't run.
  - **A copy of the version 3 files already existed:** `files/pre-upgrade-2026-10-22` (the app's private folder, made at 21:10:57, right before that install). The SHA-256 of all six database files (household and sample data, each with `-wal` and `-shm`) matched the copy's, compared on the phone. So no new copy was made.
  - DueWise was not running; the screen was on. CAMERA and POST_NOTIFICATIONS were granted, the daily-check job was present, sample data was closed, and the crash buffer had no DueWise line.
- **The install** (`adb -s ZY22KN9QJP install -r`, 07:24:53): `base.apk`'s SHA-256 is `a81271e5…`, the build checked in section 40. `firstInstallTime` is still 2026-10-04, so the data was kept. The 27-entry listing (names, sizes, times) was identical before and after.
- **The first start** (Claude Code, `am start` of Home at 07:25:09):
  - Home opened and stayed in front.
  - The crash buffer has no DueWise line, the log has no `FATAL EXCEPTION`, and the events log has no `am_crash` or `am_anr` for DueWise.
  - **The update to version 4 ran at 07:25:11:** the household database's files were written, the `-wal` was folded into the main file (0 bytes), and the main file's header now says 4.
  - The sample-data file is unchanged (still version 3). It gets the same update if it is ever opened.
- **The status bar on the phone (Android 14):** DueWise's Home window asks only for dark navigation-bar icons (`apr=LIGHT_NAVIGATION_BARS`), not for dark status-bar icons. So the status-bar icons are light on the pine bar.
- **Afterwards:**
  - Both permissions were still granted, and the daily-check job was present (due in about 2 h 05 m).
  - Sample data was still closed, and the crash buffer still had no DueWise line.
  - DueWise was left open on Home, with Chrome behind it. No Back key was sent, and no screenshot of the phone was taken.
- **Kept on the phone:** `files/pre-upgrade-2026-10-12` (version 1 data) and `files/pre-upgrade-2026-10-22` (version 3 data). Older builds can't open version 4 data, so these copies are the only way back.
- **Not checked:** the new colors on the phone's screen (the student's check), TalkBack, and landscape (section 40.4).

## 42. Pine top bars on every screen, a header on the status lists, and landscape edges (2026-10-23)
- **The student's request (07:33):** "Can you make the same coloring scheme throughout the app and all the screen - "Date Passed", Date is today", "Near Date" , Inventory etc. ASk me clarifying questions"
  - **Three answers in plan mode** (AskUserQuestion, 07:38):
    - **"Pine top bar (Recommended)":** every screen's top bar is pine with white words and icons, like Home's. The layout below stays.
    - **"Header like its tile (Recommended)":** a list opened from Home's Date passed, Date is today, or Near date tile starts with that tile as its header, on a pine band, in place of the status chip. The home icon stays.
    - **"Yes, fix it too (Recommended)":** in landscape the pine reaches the screen's edges (the light strip of 40.4).
  - The student approved the plan at 09:24.
  - **A fourth answer while checking (10:38):** with 3-button navigation (as on the student's phone) turned sideways, Android puts back, home, and recents at a side. On pine their gray (#666666) measured 2.11:1. The student chose **"Light under buttons (Recommended)"**: the screen stops at a side navigation bar, so the buttons stay on the page color (5.26:1).
- **Found while planning** (a Plan agent read the files of Material Components 1.14.0, AppCompat 1.8.0, and androidx.activity 1.13.0):
  - A default style can't set `android:theme`, so the bar's white icons and light ripple come through Material's `materialThemeOverlay`.
  - Material's own toolbar overlay is private (lint `PrivateResource`) and has dark ripples, so the app has its own.
  - The floating "Add batch" button sets its own padding, so it keeps clear of the sides with margins.
  - "Clear all" set back every filter, the status too, so it would have removed the new header without a word. It now sets back only the filters whose chips show.
- **What changed** (details in SPEC.md, "Name and icon" and "Home banner"):
  - **Every top bar:** the theme's toolbar style is the new `ReadyShelf.TopBar` (`styles.xml`): pine, a white title and icon, the button centered in the bar, and a light press ripple (`top_bar_ripple` #33FFFFFF in `colors.xml`, through `ThemeOverlay.ReadyShelf.TopBar` in `themes.xml`). Home's layout no longer sets its own pine.
  - **`ScreenSetup`:** one `apply` for every screen (the `darkTopBar` version is gone), so the status-bar icons are light everywhere. Turned sideways, every part of a screen reaches the edges and only its contents keep clear, set with layout tags (`fullWidth`, `insetSides`, `insetSidesMargin`). A navigation bar at a side stays outside the screen (the fourth answer).
  - **The status lists:** `activity_inventory.xml` starts with a pine band (`inventory_status_band`) that holds a status tile (`inventory_status_tile`); everything else moved into `inventory_below_band` with the same ids. `bg_home_band.xml` became `bg_pine_band.xml` (`git mv`), shared by Home and Inventory.
  - **Java:**
    - the new `StatusTile` (a tile's look, moved out of `HomeActivity`, and `showAsHeader`)
    - `InventoryViewModel`: the chosen status and its count, counted like Home's tile (every batch that is not archived)
    - `InventoryActivity`: the header, no status chip while it shows, "Clear all" keeps the status, and no gray tint on the home icon
    - `HomeActivity`: uses `StatusTile`
  - Comments in `item_status_tile.xml`, `strings.xml` (`home_attention_line`), and `dimens.xml`.
  - **Tests (instrumented):**
    - `InventoryTopBarTest`, 5 new: both top bars pine with a white icon; the button centered under the status bar; a status tile opens its list with the tile as the header (the word, the count from the screen's data, no arrow, not clickable, a heading, no chip); the Inventory tile opens without a header; "Clear all" keeps the header's status.
    - `ScreenSetupInsetsTest`, new, 3 tests: Home, a status list, and Purchase list get made-up sideways insets (gesture navigation; 3-button navigation at a side) and then portrait ones, and every padding and margin is checked.
- **Code:** uncommitted, on top of `d8c4273`. The phone was connected, but only its navigation mode was read (3-button). Nothing was installed there.

### 42.1 Build and tests
- **First build** (09:30:30 to 09:30:40): BUILD SUCCESSFUL in 9s, 19 of 71 tasks executed. Unit tests (results written at 09:30:39): **325, 0 failures, 0 errors, 0 skipped**. APK `b13e1f5f…`.
  - `lintDebug --rerun-tasks` (09:30:51 to 09:31:09): **one warning**, `ObsoleteLayoutParam`: the tile's `layout_weight` (`item_status_tile.xml:16`) does nothing inside Inventory's band, which was a FrameLayout. The band became a vertical LinearLayout.
- **Second build** (09:31:30 to 09:31:35, 8 of 71 tasks executed): APK `ba72cab4…`. Lint (09:31:35 to 09:31:48, all 27 tasks): **"No issues found."** On the emulator, `am instrument -w` (09:32:01 to 09:33:07, "Time: 61.576"): **OK (151 tests)**.
- **Bug found on the first photos** (09:37): on Inventory's pine bars, the back arrow and the home icon sat too low. The toolbar places its button at the top of its minimum height, and that height now includes the status bar.
  - A new test, `theTopBarsButton_isCenteredUnderTheStatusBar`, run alone against `ba72cab4…`: **"FAILURES!!!", "Tests run: 1, Failures: 1"** ("button center 296, bar center 226").
  - Fix: `buttonGravity` `center_vertical` in `ReadyShelf.TopBar`. The photo run was stopped, and its photos were not used.
- **Third build** (09:39:06 to 09:39:08): APK `3298a5fc…` (09:39:07). Unit tests up to date (325, 0 failures, from 09:30:39). Lint (09:39:09 to 09:39:21, all 27 tasks): **"No issues found."** `am instrument -w` (09:39:34 to 09:40:53, "Time: 76.161"): **OK (152 tests)**.
- **The final build**, after the fourth answer (`ScreenSetup` and `ScreenSetupInsetsTest` changed): `assembleDebug assembleDebugAndroidTest testDebugUnitTest` (10:40:17 to 10:40:23): BUILD SUCCESSFUL in 5s, 11 of 71 tasks executed.
  - Unit tests (results written at 10:40:22): **325, 0 failures, 0 errors**.
  - `lintDebug --rerun-tasks` (10:40:24 to 10:40:35, all 27 tasks): **"No issues found."**
  - **The APK** is from 10:40:21; its SHA-256 is `51072aa110708867b1fbb9cc38da7cbfbf85aa986aa2d9e8938132976e197217`. The test APK is `fe7df88b…`.
  - Installed on the emulator with `install -r` (10:40:43); the installed `base.apk` has the same SHA-256. `am instrument -w` (10:40:50 to 10:43:30, "Time: 156.175"): **OK (152 tests)**: the 144 of section 40, 5 new in `InventoryTopBarTest`, and 3 in `ScreenSetupInsetsTest`.

### 42.2 On the emulator (its own records; nothing was saved)
- **Before:** the photos of section 40 (`a81271e5…`).
- **After, with `3298a5fc…`** (09:41 to 10:01). In portrait and with gesture navigation it works like the final build; the fourth answer changed only a navigation bar at a side.
  - **The same 25 screens as in 40.3** (09:41:00 to 09:56:42, no failed step): every top bar pine with a white title and arrow (or home icon), white status-bar icons, and the page below as before.
  - **Status lists:** from Date passed the header said 3, and from Near date 4, the numbers on Home's tiles. With Date passed chosen later in the filters, the header followed. With "All statuses", there was no header and no chip.
  - **Landscape, gesture navigation, both sides:** in the first and last pixel columns, the top-bar rows are pine (#1F3B34) on Home, the status list, Settings, and Add batch, and on Label photos on one side (the other photo was black, taken mid-turn). The bands on Home and the status list are pine at both edges too.
  - **Font scale 2.0:** Home's band and the list's header still fit (set back to 1.0).
- **After, with the final build `51072aa1…`:**
  - **Landscape again** (10:44 to 10:49): the top bar pine at both edges on the status list and Settings (both sides), and on Home, Add batch, and Label photos (one side each). The other three photos were black (mid-turn) or dimmed by Android's camera question, which was closed with Back; the camera stays not allowed. The bands on Home and the list are pine at both edges.
  - **3-button navigation, sideways** (10:57 to 10:59; switched with `cmd overlay`, then back to gesture navigation): on Home and the status list, the side with the buttons has the page color (#F5F5F4) under gray buttons (#666666, 5.26:1). The camera's side stays pine.
  - **Press ripple** (11:00): while the finger was down on Settings' back arrow, 1,745 pixels around it turned #4C625D, which is white at 20 % over pine. The arrow's 397 white pixels stayed white. Settings stayed open (no tap).
  - **Right to left** (11:07 to 11:14):
    - `debug.force_rtl` (the setting, then also the system property) didn't mirror the app, because Android applies it only when its configuration is made again.
    - So DueWise's own language was set to Hebrew (`cmd locale set-app-locales`). DueWise has no Hebrew texts, so its words stayed English. Afterwards the language was set back to none ("[]").
    - Mirrored: the logo, the home icon, and the back arrow on the right; the header's circle and number on the right; "Add batch" on the left. English words still start at the left, because Android lines up each text by its own language. Dates showed Hebrew month names.
    - Sideways, both sides: the top bar and the band are pine at both edges. The home icon and the header's words keep 142 px clear of the cutout, on whichever side it is.
    - Two screen readings (uiautomator) failed right after each language change. A later one worked, DueWise was open on Home, and the log had no DueWise error.
  - **Afterwards:** rotation 0, gesture navigation, font scale 1.0, `debug.force_rtl` 0 (setting) and false (property), DueWise's language empty, and DueWise open on Home.
- **The before and after photos** are on a private web page (an Artifact): https://claude.ai/artifact/AbY9yzx3X2YEsSy4hytXkN.

### 42.3 Not checked, and remaining problems
- **Remaining problem (small, not new):** turned sideways, the floating "Add batch" covers part of the Purchase list tile on Home and the lowest 26 px of Inventory's search box, until the list scrolls. On Home it sits where it did before this change (the same place on the landscape photos of 40.3 and 42.2).
- **Not checked:**
  - The phone: nothing was installed there (the student hasn't asked).
  - TalkBack (only the test checks that the header is a heading).
  - The full on-screen keyboard: the emulator uses a hardware keyboard, so only Gboard's small bar showed.
  - The status lists when the date rules are missing or stop with an error.
  - The brass "Date is today" header (no batch on the emulator is dated today), the "Sample data" strip (sample data can't be opened since 2026-10-22), and Android 8 to 11 (the emulator runs Android 17).

## 43. Phone install of section 42, and the commit (2026-10-23)
- **The student's request:** "install and commit".
- **The build, right before** (11:33:09 to 11:33:12): `assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug`: BUILD SUCCESSFUL in 2s, 79 of 80 tasks up to date. Only `lintDebug` ran ("No issues found.").
  - The APK was not rewritten (10:40:21, `51072aa1…`), so it is the one checked on the emulator in section 42: OK (152 tests), and 325 unit tests with 0 failures.
- **Before** (read-only checks from 11:32:30; no records were read):
  - The phone ran `a81271e5…` (`d2ac85a`, section 41), installed at 07:24:53.
  - DueWise was running with only Home in its task (no form open), and the screen was dozing.
  - The household database's main file header said 4.
  - Since the first start at 07:25, only `shared_prefs/reminders.xml` had changed (09:32:29, same size), from the daily check. The database files had the same sizes and times, so no records were written in between.
  - CAMERA and POST_NOTIFICATIONS were granted, the daily-check job was present, sample data was closed, and the crash buffer had no DueWise line.
  - The phone uses 3-button navigation (`navigation_mode` 0) on Android 14.
- **The install** (`adb -s ZY22KN9QJP install -r`, 11:33:39 to 11:33:50, "Success"):
  - `base.apk`'s SHA-256 is `51072aa1…`, the build checked in section 42.
  - `lastUpdateTime` is 11:33:52. `firstInstallTime` is still 2026-10-04, so the data was kept.
  - The install closed DueWise.
- **After** (11:33:56):
  - The 27-entry listing (names, sizes, times) was identical to the one before, and the header still says 4. This build has no database change.
  - Both permissions were still granted, the daily-check job was present, and sample data was closed.
  - The crash buffer had no DueWise line, and the events log had no `am_crash` or `am_anr` for DueWise.
  - Claude Code did not open DueWise on the phone, take screenshots, or press any key there.
- **Committed** on `main` after this section was written, as the next commit after `d8c4273`.
- **Not checked on the phone:** the new bars and the status-list header on its screen, landscape with its 3-button navigation (the student's checks), and TalkBack.

## 44. "Show reminder now" in place of the test reminder, and "others" in the reminder (2026-10-23)
- **The student's request:** "change the functionality of a Test reminder to generate a reminder- it should display the names of up to 2 items that are expired or near date and include rest as others. I want to see actual notification rather than a message saying that it is a test reminder.Ask me clarifying questions"
  - **Four answers** (AskUserQuestion), all Claude Code's recommended options:
    - **"“and 3 others” (Recommended)":** the rest are counted with the word "others", in the daily reminder too ("and 1 other" for one).
    - **"Don't count it (Recommended)":** the button's reminder isn't recorded, so "Last reminder" keeps its date and the daily check still comes.
    - **"Message only (Recommended)":** when nothing is near date or passed, no notification, only a message.
    - **"Show reminder now (Recommended)":** the button's label.
  - **Stated before the questions, not asked:** the same title, items, and order as the daily reminder (Date passed, Date is today, and Near date; "Date needs review" doesn't count); medicine only as "medicine" (the choice of 2026-10-19); the phone's lock-screen setting decides; it works while the "Daily reminder" switch is off; one reminder at a time.
- **What changed** (details in SPEC.md, "Reminders"):
  - **`Reminders`:** `sendTest()` is gone. New: `showNow(LocalDate)` with the result `NowResult`, and `showNowInBackground(answer)`, which runs it on the object's own background thread. `showNow` reads the household's records with the date rules (`RuleRunner.needsReview` and `ReminderList.of`, like the daily check), never records anything, and removes a shown reminder when nothing causes one.
  - **`ReminderNotifier`:** the test reminder (`TEST_ID` 1002, `showTest`) is gone. The button uses `showDaily`, so it is the daily reminder's notification (ID 1001).
  - **Settings:** the button `settings_show_reminder` ("Show reminder now"); `SettingsViewModel.showReminderNow()` and a one-time message (`getMessage`, `messageShown`); `PendingAction.SHOW_NOW` in place of `TEST`.
  - **Texts (`strings.xml`):** `reminder_daily_more` says "other"/"others"; `settings_reminders_intro` says "and 3 others"; new `settings_show_reminder`, `_note`, `_sent`, `_nothing`, and `_failed`; `reminder_test_title`, `reminder_test_text`, and the three `settings_test_reminder*` strings removed. Comments in `AndroidManifest.xml`, `activity_settings.xml`, and `ReminderList.java`.
  - **Tests (instrumented):**
    - `ReminderFlowTest`: the test-reminder test was replaced by 5 tests. They check that the names show while reminders are off, nothing is recorded, and a second tap replaces the reminder; that the daily check still shows that day's reminder afterwards; that nothing near or passed removes a shown reminder; that missing date rules, or rules that stop with an error, show nothing; and that the background version gives its result.
    - `ReminderTextTest` and one `ReminderFlowTest` text expect "others". `ReminderDeniedTest` uses `showNow`. `NotificationChecks` no longer cancels ID 1002.
- **Code:** uncommitted, on top of `384d162`. Nothing was installed on the phone.

### 44.1 Build and tests
- **Build** `assembleDebug assembleDebugAndroidTest testDebugUnitTest` (11:51:27 to 11:51:38): BUILD SUCCESSFUL in 11s, 23 of 71 tasks executed.
  - Unit tests (results written at 11:51:37): **325, 0 failures, 0 errors, 0 skipped**.
  - The APK is from 11:51:35; its SHA-256 is `ec5268c5b11ed62322404e02dad758dff5c4ec5e2754bebe364af247411b84a6`. The test APK is `45b45ebd…`.
- **Lint** `lintDebug --rerun-tasks` (11:51:58 to 11:52:13, 27 of 27 tasks executed; report written at 11:52:12): **"No issues found."**
- **On the emulator:** both APKs installed with `install -r -t` (11:52:35 to 11:52:41). The installed `base.apk` has the same SHA-256 (`ec5268c5…`).
  - `am instrument -w` (11:52:46 to 11:56:33, "Time: 222.907"): **OK (156 tests)**: the 152 of section 42, minus the test-reminder test, plus the 5 new ones (`ReminderFlowTest` now has 14).
  - `ReminderDeniedTest` with the permission taken away (`pm revoke`, then `-e notifications denied`, 11:56:49 to 11:56:53): **OK (1 test)**. `pm grant` gave the permission back; afterwards it was granted with the same flags as before.

### 44.2 On the emulator (its own records; nothing was saved)
- **The expected text:** Home said Date passed 3 and Near date 4. The lists, soonest first: Pain reliever tablets (medicine, Expires 8 Oct 2026), Beans (18 Oct), and N (22 Oct) passed; EU Visa (31 Oct), Canned beans (1 Nov), Granola bars (7 Nov), and Test water (11 Nov) near. So: "Medicine, Beans, and 5 others".
- **Settings → Show reminder now** (11:59:20; the "Daily reminder" switch was on, and today's daily reminder had been recorded at 06:10):
  - The heads-up notification said "Household items need review · now" and "Medicine, Beans, and 5 others" (screenshot).
  - `dumpsys notification --noredact`: ID 1001, channel `review_alerts`, importance 4, `vis=PRIVATE` with a public version, category reminder, AUTO_CANCEL and LOCAL_ONLY. Title "Household items need review"; text and big text "Medicine, Beans, and 5 others".
  - **The message:** two rounds of screenshots taken from the PC missed it (each screenshot took 1.5 to 2 s). An accessibility-event log of a third tap showed the click, the notification 0.5 s later, and a view at the bottom appearing and leaving about 5 s later, but no words. Screenshots taken on the emulator itself (12:02) showed "Reminder sent. Look in your notifications." together with the heads-up notification.
  - **Not recorded:** `shared_prefs/reminders.xml` stayed 187 bytes with its time of 06:10:07 (the morning's daily check; `last_daily_reminder_day` 2026-10-23) after every tap.
- **Without the permission** (12:02 to 12:03): after `pm revoke`, the reminder was gone and Settings said "Notifications are off, so reminders can't appear.". "Show reminder now" showed Android's question "Allow DueWise to send you notifications?". After **Allow**: the same reminder and "Reminder sent. Look in your notifications."; the line changed to "Notifications are allowed."; the permission is granted with the same flags as before. The public version's text, read with `dumpsys`, is "Open DueWise to see the review list.".
- **More about this:** the five texts, with "Soonest dates first, for example “Canned beans, Granola bars, and 3 others”. Medicine shows only as “medicine”." and, last, "Shows the reminder with your items at once. The daily reminder still comes as usual." It was closed again.
- **Logs:** the crash buffer had no DueWise line, and the events log had no `am_crash` or `am_anr` for DueWise. The only `Reminders` errors were the test's own "A made-up error" (11:53:06).
- **Afterwards:** DueWise open on Settings, the reminder (ID 1001) still in the emulator's notification list, and the permission granted.

### 44.3 Not checked, and remaining problems
- **Not seen on screen:** the messages "Nothing is near date or passed. No reminder to show.", "No reminder: checking your items stopped with an error. Try again.", and "Not available yet: it needs the date rules." The emulator's records always have passed dates; the tests check the results behind these messages, not the screen.
- **Not checked:** the phone (the student hasn't asked to install), a tap on the reminder (the same tap as the daily reminder, unchanged), the lock screen (unchanged; only the public version's text was read), TalkBack, font scale 2.0, landscape, and Do Not Disturb.
- **Remaining problem (small, not new):** the channel's description in Android's settings still says the reminder pops up "at most once a day". Each tap on "Show reminder now" also pops it up, as the test reminder did before.

## 45. Phone install of section 44, and the commit (2026-10-23)
- **The student's request:** "commit and install".
- **The build, right before** (12:28:42 to 12:28:45): `assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug`: BUILD SUCCESSFUL in 2s, 79 of 80 tasks up to date. Only `lintDebug` ran; its report is still the one from 11:52:12, "No issues found." No code had changed since.
  - The APK was not rewritten (11:51:35, `ec5268c5…`), so it is the one checked on the emulator in section 44: OK (156 tests), and 325 unit tests with 0 failures.
- **Before** (read-only checks at 12:29:12, phone time; no records were read):
  - The phone ran `51072aa1…` (`384d162`, section 43), installed at 11:33:52.
  - DueWise was not running (no process and no activities), and the screen was dozing.
  - The household database's main file header said 4.
  - The database files still had their times from 07:25:11, and `shared_prefs/reminders.xml` was from 09:32:29 (the daily check). So no records were written since this morning.
  - CAMERA and POST_NOTIFICATIONS were granted, the daily-check job was present, and sample data was closed. The crash buffer had no DueWise line, and the events log had no `am_crash` or `am_anr` for DueWise.
- **The install** (`adb -s ZY22KN9QJP install -r`, 12:29:21 to 12:29:36, "Success"):
  - `base.apk`'s SHA-256 is `ec5268c5…`, the build checked in section 44.
  - `lastUpdateTime` is 12:29:38. `firstInstallTime` is still 2026-10-04 13:26:35, so the data was kept.
- **After** (12:29:44, phone time):
  - The 27-entry listing (names, sizes, times) was identical to the one before, and the header still says 4. This build has no database change.
  - Both permissions were still granted with the same flags, the daily-check job was present, and sample data was closed.
  - The crash buffer had no DueWise line, and the events log had no `am_crash` or `am_anr` for DueWise.
  - No DueWise notification was shown, so no old "Test reminder" (ID 1002) is left in the phone's list.
  - Claude Code did not open DueWise on the phone, take screenshots, or press any key there.
- **Committed** on `main` after this section was written, as the next commit after `384d162`.
- **Not checked on the phone:** "Show reminder now" and its notification on the phone's screen (the student's check), the lock screen with the phone's own setting, and TalkBack.

## 46. The number of items that need attention, on the app icon and in the widget (2026-10-23)
- **The student's request:** "There is one other feature that I want to implement, the app should show some notification if the item needs attention on the phone -similar to Linkedin, when there are 4 unread messages , it shows the number 4 on the Linkedin APp logo, in this case the app logo should show the total number of items that need attention on the phone. Ask me clarifying question"
  - **Found before the questions** (read-only): Android has no way for an app to put a number on its icon; a home screen shows a badge only while the app has a notification in the list, with that notification's `Notification.number`. The phone (moto g play 2024, Android 14) uses Moto App Launcher (`com.motorola.launcher3`, version 14) with `notification_badging=1` and no setting for numbers, so it is expected to show a dot. SPEC.md's privacy rule said counts appear only inside the app.
  - **Three answers** (AskUserQuestion):
    - **"Also put the number in widget":** build it, and the home-screen widget shows the number too (this reverses the widget's "never gives counts" rule).
    - **"Home's tiles added up (Recommended)":** batches with Date passed, Date is today, or Near date, the same total as Home's three tiles.
    - **"The daily reminder (Recommended)":** the reminder carries the number (no new notification); it updates quietly when changes are saved and goes when the reminder is tapped or swiped away.
  - **Stated in the plan, not asked:** the lock-screen version has no number; a removed reminder doesn't come back because of an update; the widget says "N household items need review" ("Household items need review" when only dates to review are left); no new Settings switch.
- **What changed** (details in SPEC.md, "Reminders", "Home-screen widget", and "Privacy"):
  - **`ReviewCounts`:** `hasTile(status)` (Date passed, Date is today, Near date) and `tilesTotal(reviewList)`. `HomeActivity.showTiles` uses `hasTile`, so the tiles and the number share one rule.
  - **`ReminderList`:** carries `getIconNumber()` (`tilesTotal` of the same review list); it is part of `equals`.
  - **`ReminderNotifier`:** the reminder gets `setNumber` (the public version gets none). New `isShown()` and `updateIfShown(list)` with `UpdateResult`: only while the reminder is shown, nothing when nothing changed, otherwise a quiet post (`setOnlyAlertOnce`, `setSilent`, and the first post's time with `setWhen`), with "is it shown?" checked again right before `notify()`.
  - **`Reminders`:** a new constructor argument for the batch-change listener (`AppContainer` passes `listenForBatchChanges`, like the widget); `onAppStart` starts listening on its own thread; new `refreshShownSoon()` and `refreshShown(LocalDate)` with `RefreshResult`. `onReviewListShown` still removes at once when nothing causes a reminder, and otherwise asks for a refresh.
  - **Widget:** `ReviewWidgetState` became a value class (kind and number); `ReviewWidget.lineOf(context, state)`; new plurals `widget_review_count`; new `widget_description`. Comments in `strings.xml`, `AndroidManifest.xml`, and `HomeViewModel`.
  - **Tests:** unit: `ReviewCountsTest` +5, `ReminderListTest` +2 (and icon numbers in 5 others), `ReviewWidgetStateTest` +3 (moved to the new class), an icon number in `ReminderCheckTest`. Instrumented: `ReminderFlowTest` +6 (numbers, the quiet update, the listener, a removed reminder, nothing new, rules missing or stopping) and numbers in 3 old ones; `ReviewWidgetTest` checks each line's words and number; `NotificationChecks.waitUntilNumber`; `FixedStatusPolicy` is synchronized; `ReminderDeniedTest` gets the new argument.
- **Code:** uncommitted, on top of `399399d`. Nothing was installed on the phone.

### 46.1 Build and tests
- **Build** `assembleDebug assembleDebugAndroidTest testDebugUnitTest` (18:59:10 to 18:59:34): BUILD SUCCESSFUL in 24s, 23 of 71 tasks executed.
  - Unit tests (results written at 18:59:33): **335, 0 failures, 0 errors, 0 skipped** (325 before, plus 10).
  - The APK is from 18:59:32; its SHA-256 is `ce22453b4c9cbd1f2d684eb182af9b7a26df78b0bab69614cfa36be259a16125`. The test APK is `25abd972…`.
- **Lint** `lintDebug --rerun-tasks` (18:59:51 to 19:00:22, 27 of 27 tasks executed; report written at 19:00:21): **"No issues found."**
- **On the emulator:** both APKs installed with `install -r -t` (finished 19:00:56). The installed `base.apk` has the same SHA-256 (`ce22453b…`).
  - `am instrument -w` (19:01:02 to 19:04:11, "Time: 185.405"): **OK (162 tests)**: the 156 of section 44 plus the 6 new ones (`ReminderFlowTest` now has 20).
- **Right before the commit** (the student's request: "commit"; 19:25:15 to 19:25:17): `assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug`: BUILD SUCCESSFUL in 1s, 79 of 80 tasks up to date. The APK was not rewritten (18:59:32, `ce22453b…`), so the commit holds the code checked above. Lint still says "No issues found." Committed on `main` as the next commit after `399399d`. Not installed on the phone.

### 46.2 On the emulator (its own records; one test record changed and changed back)
- **Home** said Date passed 3 and Near date 4, so the expected number was 7.
- **Settings → Show reminder now** (19:05:16): `dumpsys notification --noredact` showed ID 1001 with **`number=7`**, text "Medicine, Beans, and 5 others", and flags AUTO_CANCEL and LOCAL_ONLY. The public version had `number=0` and "Open DueWise to see the review list.".
- **The icon** (Pixel home screen, Android 17; screenshot): DueWise's hourglass icon had a dot.
- **The icon's long-press menu** (screenshot): App info, Pause app, Pin Prediction, Widgets, Don't suggest app, Bubble, and App lock. **It has no notification part, so the number doesn't show there.** (AOSP Launcher3 for Android 14 shows the number in that menu's header; the phone's Moto menu is not checked.)
- **The widget:** added from the menu's "Widgets". The widget list showed "Shows how many of your own household items need review, without naming them. Tap it to open DueWise.". The placed widget said **"7 household items need review"** (screenshot).
- **A saved change with Home not in front** (the real change listener): "Test oats" (Food, 1 bag in Pantry, Best by 20 Aug 2027, Later date) was edited to Best by 5 Nov 2026. Changing the date clears the date tick, as designed, so it was ticked again as before. After "Confirm and save" (19:11:43) the phone's Home button was pressed on Batch details.
  - The reminder then had **`number=8`** and "Medicine, Beans, and 6 others". Its flags were ONLY_ALERT_ONCE, AUTO_CANCEL, LOCAL_ONLY, and SILENT, and `when` was unchanged. The record said `mIsInterruptive=false` (no new alert), `mShowBadge=true`, and `mRankingTimeMs` was the first post's time.
  - The widget said **"8 household items need review"**.
- **Changed back** (19:14:57): Test oats is Best by 20 Aug 2027 again, ticked, "Later date".
  - The reminder had `number=7`, "Medicine, Beans, and 5 others", and the same `when`. The widget said 7.
  - Test oats' history has the two date edits.
- **The notification list** (19:15) showed "DueWise • 9m", so the reminder kept its first time.
- **Swiped away:** no DueWise notification was left, the icon's dot was gone (screenshot), and the widget still said 7.
- **Opening DueWise** (Batch details, back to Inventory, back to Home, which asks for a refresh): still no notification, and tiles 3 and 4.
- **Afterwards:**
  - The widget was removed again; the emulator had none before.
  - DueWise was left in the background on Home, with no DueWise notification.
- **Logs:** the crash buffer had no DueWise line since 19:00:50, the events log had no `am_crash` or `am_anr` for DueWise, and there were no E or W lines from `Reminders`, `ReminderNotifier`, or `ReviewWidget` during the checks.

### 46.3 Not checked, and remaining problems
- **Not checked:**
  - the phone: the dot, the Moto long-press menu, and the student's own records (the student hasn't asked to install)
  - a home screen that shows numbers on icons (no such device or emulator here)
  - the widget's new line at font scale 2.0 (it is shorter than "Open DueWise to see the review list.", which was checked on 2026-10-18)
  - TalkBack, landscape, Do Not Disturb, and the lock screen
  - the 09:00 daily check on screen; it uses the same `showDaily`, and `ReminderFlowTest` checks its number
- **Remaining problems, recorded as known limits in SPEC.md and DEMO.md:**
  - A tap or a swipe at the very same moment as an update can't be seen in time; that post is then quiet too.
  - After the planning settings are saved, the number waits until Home works out its list again, like the widget.
  - Nothing refreshes at midnight.
  - The channel's description still says "at most once a day" (section 44).

## 47. Phone install of section 46 (2026-10-23)
- **The student's request:** "install on phone" (after the commit `9ed3f33`).
- **The build:** the APK of `9ed3f33` is the one checked in section 46 (18:59:32, `ce22453b…`; the check right before the commit found 79 of 80 tasks up to date). Its SHA-256 was read again right before the install.
- **Before** (read-only checks at 19:27:08, phone time; no records were read):
  - The phone ran `ec5268c5…` (`399399d`, section 45), installed at 12:29:38.
  - DueWise was running with Home, Settings, and Home in its task (no form), and the screen was on. So the install closed it, with nothing unsaved.
  - The household database's main file header said 4. CAMERA and POST_NOTIFICATIONS were granted, the daily-check job was present, and sample data was closed.
  - The crash buffer had no DueWise line, and the events log had no `am_crash` or `am_anr` for DueWise. No DueWise notification was shown.
- **The install** (`adb -s ZY22KN9QJP install -r`, 19:27:20 to 19:27:30, "Success"):
  - `base.apk`'s SHA-256 is `ce22453b…`, the build checked in section 46.
  - `lastUpdateTime` is 19:27:31. `firstInstallTime` is still 2026-10-04 13:26:35, so the data was kept.
- **After** (19:27:41, phone time):
  - The 27-entry listing (names, sizes, times) was identical to the one before, and the header still says 4. This build has no database change.
  - Both permissions were still granted with the same flags, the daily-check job was present, and sample data was closed.
  - The crash buffer had no DueWise line, and the events log had no `am_crash` or `am_anr` for DueWise.
  - DueWise was not running, and no DueWise notification was shown.
  - Claude Code did not open DueWise on the phone, take screenshots, or press any key there.
- **Not checked on the phone (the student's checks):**
  - "Show reminder now" and the dot on DueWise's icon on the Moto home screen
  - whether the icon's long-press menu shows the number
  - the widget's number, if a widget is placed
  - a saved change that updates the reminder quietly
  - TalkBack

## 48. Add batch as the last tile on Home (2026-10-23)
- **The student's request:** "The "Add batch" should appear in the empty space as another tile with the same color scheme instead of a floating box at the bottom. Refer to the attached screeshot [Image #8]. Basically I want to remove the empty space which looks odd. Ask me clarifying questions". The screenshot (`C:\Users\dkhandelwal\Downloads\Add_Batch.png`, from the phone) shows Home with Date passed 3, Near date 4, Inventory, Purchase list, and Settings, and "ADD BATCH" written in red in the empty cell next to Settings.
  - **Four answers** (AskUserQuestion; all four were the recommended options):
    - **"Like the other tiles (Recommended)":** a white tile with a pine "+" in a cream circle, the ">" arrow, and the word. (Claude Code noted that a brass tile could look like the brass "Date is today" status.)
    - **"Wide Add batch (Recommended)":** Add batch is always the last tile; when it is alone in its row (1 or 3 status tiles), it fills the row.
    - **"Keep it on Inventory (Recommended)":** Inventory keeps its floating Add batch.
    - **"Let it scroll away (Recommended)":** Home has no floating button.
  - **Stated in the plan, not asked:** the wide tile has the same parts and height as the others; with large text every tile already has its own row; save messages on Home show at the bottom of the screen.
- **What changed** (details in SPEC.md, "Screens" and "Home banner"):
  - `activity_home.xml`: no floating button, and no FrameLayout around the scroll view (`home_scroll` is the root's last child, tagged `fullWidth`). The content's bottom padding is `screen_padding` (16 dp) instead of `fab_clearance` (88 dp).
  - `HomeActivity`: a fourth shortcut tile after Settings, `home_add_batch` (`ic_add`, "Add batch"), which opens `BatchEditActivity.newAddIntent`. The rows are built by `fillGrid` (`@VisibleForTesting`) with no empty filler, so a tile alone in the last row fills it.
  - `ids.xml`: `home_add_batch`. Comments in `AddButton.java`, `item_action_tile.xml`, `activity_inventory.xml`, and `strings.xml` (the text of `home_empty` is unchanged).
  - **Tests:**
    - New `HomeTilesTest` (4 tests): Add batch is the grid's last tile, its label is "Add batch", and Home has no `fab_add_batch`; a tap on it opens `BatchEditActivity` (an ActivityMonitor; the form is closed at once, nothing is saved); and `fillGrid` with 4 to 7 tiles, 2 in a row and 1 in a row, leaves no empty cell (each row's tiles and gaps take its whole width, two tiles in a row are within 1 px of each other, and all rows have the same height above 0).
    - `ScreenSetupInsetsTest` checks the floating button's margins only on Inventory, and checks that Home has none.
- **Code:** uncommitted, on top of `9ed3f33`. Nothing was installed on the phone.

### 48.1 Build and tests
- **Build** `assembleDebug assembleDebugAndroidTest testDebugUnitTest` (21:23:00 to 21:23:10): BUILD SUCCESSFUL in 9s, 19 of 71 tasks executed.
  - Unit tests (results written at 21:23:10): **335, 0 failures, 0 errors, 0 skipped** (unchanged).
  - The APK is from 21:23:07; its SHA-256 is `2e2814970401e1209959cba1cc1270910e4df9bca1b4afb5404d94a3228b9dfd`. The test APK is `5c9bfb89…`.
- **Lint** `lintDebug --rerun-tasks` (21:23:31 to 21:23:50, 27 of 27 tasks executed; report written at 21:23:49): **"No issues found."**
- **On the emulator:** both APKs installed with `install -r -t` (finished 21:24:59; `lastUpdateTime` 21:24:50). The installed `base.apk` has the same SHA-256 (`2e281497…`).
  - `am instrument -w` (21:25:08 to 21:29:10, "Time: 238.614"): **OK (166 tests)**: the 162 of section 46 plus the 4 new ones. `ScreenSetupInsetsTest` (3) and `HomeTilesTest` (4) both ran.
- **After three comment fixes** (21:47:27 to 21:47:37; "all tiles have the same size" in `HomeActivity`, `activity_home.xml`, and SPEC.md): `assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug`: BUILD SUCCESSFUL in 9s, 11 of 80 tasks executed. Java was compiled again, but the unit tests were up to date and the APK was not rewritten (21:23:07, `2e281497…`), so the checks above hold for the final code. Lint's report (21:47:36) still says "No issues found."

### 48.2 On the emulator (its own records; one test record changed and changed back)
- **Before the install** (the build of section 46; screenshot): Date passed 3 and Near date 4, then Inventory and Purchase list, then Settings with an empty cell next to it. The brass floating "Add batch" at the bottom right covered part of the first swap card's "Review swap" button.
- **After** (DueWise started from the launcher; dump and screenshot):
  - Row 3 is Settings and **Add batch** (`home_add_batch`, label "Add batch"), both from y 897 to 1138 (241 px), like the rows above. Add batch has the pine "+" in a cream circle and the ">" arrow. Home has no `fab_add_batch`.
  - Scrolled to the end, the last swap card ends 42 px (16 dp) above the bottom of the scroll area, and nothing floats over it.
  - A tap on Add batch opened the empty form (title "Add batch", "What kind of item?"). Its back arrow closed it at once, with no "Leave without saving?", and Home showed again.
- **The odd count**, with the test record "Test oats" (Food, 1 bag in Pantry, Best by 20 Aug 2027, ticked, Later date):
  - It was edited to Best by 23 Oct 2026, the emulator's date. Changing the date cleared the date tick, as designed, so it was ticked again. The review said only "Date changed from Best by: 20 Aug 2027 to Best by: 23 Oct 2026."; saved at 21:34:37, and Batch details said "Date is today".
  - Home then had **3 status tiles** (Date passed 3, Date is today 1, Near date 4), so 7 tiles in all. **Add batch filled the last row** (x 42 to 1038, 241 px tall like the other rows), with the "+" at the left and ">" at the right (screenshot). This was also the first time the brass "Date is today" tile was seen on screen (section 40 couldn't show it).
  - **Landscape** (`adb emu rotate`; ROTATION_270 and ROTATION_90, screenshots): two tiles in a row. The wide Add batch spans both columns (x 42 to 2240 at 270, 184 to 2382 at 90), with the same edges as the tiles above it, clear of the cutout side. Back at ROTATION_0 afterwards.
  - **Font scale 2.0** (screenshot): one tile per row, each 307 px tall, and Add batch is the last full-width row. Nothing covers any tile. Set back to 1.0.
  - **Changed back** (saved at 21:40:23): Test oats is Best by 20 Aug 2027 again, ticked, "Later date". Its history has the two new date edits (9:34 PM and 9:40 PM).
  - Home went back to Date passed 3 and Near date 4 with Add batch next to Settings, so the grid also rebuilt from 7 tiles to 6.
- **Inventory** still has its floating "Add batch" at the bottom right.
- **Logs:** no DueWise line in the crash buffer, no `am_crash` or `am_anr` for DueWise in the events log, and no `AndroidRuntime` line for DueWise.
- **Afterwards:** font scale 1.0, ROTATION_0, DueWise on Home, and no DueWise notification.
- **Page:** the screenshots are on https://claude.ai/artifact/786NfikQRmzo3bZV6VvcUm (private).

### 48.3 Not checked, and remaining problems
- **Not checked:**
  - the phone (the student hasn't asked to install)
  - TalkBack (`HomeTilesTest` checks the label "Add batch"), right to left, 3-button navigation, and Android 8 to 11
  - a save message on Home without the button ("Batch added." after Add batch, "Swap recorded." after Review swap): showing one needs a saved record
- **Remaining:**
  - Once Home is scrolled down to the swaps, Add batch has scrolled away with the tiles (the student's choice "Let it scroll away").
  - Section 42's sideways problem is gone on Home, where the button covered part of the Purchase list tile. Inventory is unchanged, so its button still covers the lowest 26 px of its search box sideways until the list scrolls (section 42; not checked again).

## 49. Phone install of section 48, and the commit (2026-10-23)
- **The student's request:** "commit and install". As with the same words in section 45, the install came first, so that one commit holds the code and its records.
- **The build, right before** (21:52:17 to 21:52:19): `assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug`: BUILD SUCCESSFUL in 1s, 79 of 80 tasks up to date. Only `lintDebug` ran; its report is still the one from 21:47:36, "No issues found." No code had changed since.
  - The APK was not rewritten (21:23:07, `2e281497…`), so it is the one checked on the emulator in section 48: OK (166 tests), and 335 unit tests with 0 failures.
- **Before** (read-only checks at 21:52:50, phone time; no records were read):
  - The phone ran `ce22453b…` (`9ed3f33`, section 47), installed at 19:27:31.
  - DueWise was running with Home, Settings, and Home in its task (no form), and the screen was dozing. So the install closed it, with nothing unsaved.
  - The household database's main file header said 4. The main database file still had its time from 07:25:11, and `shared_prefs/reminders.xml` was from 09:32:29 (the daily check).
  - CAMERA and POST_NOTIFICATIONS were granted, the daily-check job was present, and sample data was closed. The crash buffer had no DueWise line, and the events log had no `am_crash` or `am_anr` for DueWise. No DueWise notification was shown.
- **The install** (`adb -s ZY22KN9QJP install -r`, 21:53:05 to 21:53:15, "Success"):
  - `base.apk`'s SHA-256 is `2e281497…`, the build checked in section 48.
  - `lastUpdateTime` is 21:53:14. `firstInstallTime` is still 2026-10-04 13:26:35, so the data was kept.
- **After** (21:53:26, phone time):
  - The listing of the app's folder (names, sizes, and times; 43 entries, folders included, so more than the 27 of sections 45 and 47) was identical to the one before, and the header still says 4. This build has no database change.
  - Both permissions were still granted with the same flags, the daily-check job was present, and sample data was closed.
  - The crash buffer had no DueWise line, and the events log had no `am_crash` or `am_anr` for DueWise.
  - DueWise was not running, and no DueWise notification was shown.
  - Claude Code did not open DueWise on the phone, take screenshots, or press any key there.
- **Committed** on `main` after this section was written, as the next commit after `9ed3f33`, with sections 47 to 49.
- **Not checked on the phone (the student's checks):**
  - Add batch next to Settings on Home, and a tap on it
  - the wide Add batch, which shows only with 1 or 3 status tiles
  - a save message on Home after Add batch ("Batch added." at the bottom of the screen)
  - TalkBack

## 50. "Take label photos" hidden on Add batch (2026-10-23)
- **The student's request:** "inside the "Add batch" section - there is a tile that says "Take labe photos"- I am not using this functionality , so hide it"
- **What changed** (details in SPEC.md, "Photo entry"):
  - `BatchEditActivity`: a constant `LABEL_PHOTOS_SHOWN = false`. The label photos section (`scan_section`) shows only when it is true, and as before only when adding, not for a bought item, and not on the review. So it never shows now. The Label photos screen, its launcher, and the form's code for a photo result are unchanged; `true` shows the button again.
  - `activity_batch_edit.xml`: `tools:visibility="visible"` removed from the section, so Android Studio's preview hides it too, and its comment says why. Comments in `BatchEditActivity` and `strings.xml`.
  - **No test changed:** no instrumented test refers to the button (`androidTest` searched for "scan", "photosButton", and "take_photos"). `BatchEditPhotoResultTest` (14 tests) works on the form's ViewModel, not on the button, so it still checks what a photo result puts in.
- **Code:** uncommitted, on top of `465a5e1`. Nothing was installed on the phone.

### 50.1 Build and tests
- **Build** `assembleDebug assembleDebugAndroidTest testDebugUnitTest` (22:06:37 to 22:06:45): BUILD SUCCESSFUL in 7s, 14 of 71 tasks executed.
  - Unit tests (results written at 22:06:45): **335, 0 failures, 0 errors, 0 skipped** (unchanged).
  - The APK is from 22:06:44; its SHA-256 is `cc6f6caf2abaa9a14f0702c0d1f0060e6156c1171ca9855b6c8a0376b8cee7b6`. The test APK was not rewritten (21:23:08, `5c9bfb89…`), and the emulator already had it from section 48.
- **Lint** `lintDebug --rerun-tasks` (22:12:52 to 22:13:06, 27 of 27 tasks executed; report written at 22:13:05): **"No issues found."** The APK was not rewritten (still 22:06:44, `cc6f6caf…`).
- **On the emulator:** the APK installed with `install -r` (22:07:44 to 22:07:47, "Success"; `lastUpdateTime` 22:07:43 by the emulator's clock). The installed `base.apk` has the same SHA-256 (`cc6f6caf…`).
  - `am instrument -w` (22:08:43 to 22:12:38, "Time: 230.811"): **OK (166 tests)**, the same count as section 48. `BatchEditPhotoResultTest` (14) ran.

### 50.2 On the emulator (its own records; nothing was saved)
- **Before** (the build of section 48, `2e281497…`): when this session started, the emulator already showed an empty Add batch form, with the form's starting choices Food and Pantry and an empty product box. Its last touch was about 4 minutes earlier; Claude Code had not opened it. The dump and a screenshot: **"Take label photos"** (`scan_take_photos`, y 892 to 1018) under the category chips (which end at y 871), and "Product" at y 1081.
- **After** (Android reopened Home by itself right after the install, `wm_restart_activity` at 22:07:47; Home → the Add batch tile; dump and screenshot):
  - The dump has no `scan_section` and no "Take label photos". The dump comes from the accessibility tree, so TalkBack can't reach the button either.
  - "Product" is at y 934, 147 px higher, right after the category chips. The Quantity box (`edit_quantity`, "1") now shows above the bottom bar, where only its top edge showed before.
  - The back arrow closed the form at once (nothing was entered), and Home showed.
- **After the instrumented tests:** the crash buffer had no DueWise line, and the events log had no `am_crash` or `am_anr` for DueWise. The only `AndroidRuntime` lines were those of the `am instrument` command itself ("VM exiting with result code 0"). No DueWise notification was shown. ROTATION_0 and font scale 1.0.
- **Afterwards:** the tests left the launcher in front, so Add batch was opened again (Home → the Add batch tile), as the emulator showed it before, with nothing entered. Its dump again has no label photos section.

### 50.3 Not checked, and remaining problems
- **Not checked:**
  - the phone (the student hasn't asked to install)
  - TalkBack itself, landscape, and large text (the form has one layout for both orientations, and the section is hidden in every case)
  - Edit batch and "Record bought item": the section was already hidden there, and that code didn't change
- **Remaining:**
  - Only the button is hidden. The Label photos screen, CameraX, Tesseract, and the CAMERA permission are still in the app. Tesseract's native libraries and English data take about 22 MB of the 36.7 MB debug APK. The phone's CAMERA permission was granted (section 49), and that stays. Removing photo entry for good is listed in SPEC.md, "Not in this step".
  - SPEC.md's "Simpler form (2026-10-22)" and DEMO.md's thirty-first update still say "Take label photos" is one button: they describe the form as it was that day.

## 51. Phone install of section 50, and the commit (2026-10-23)
- **The student's request:** "commit and install". As in sections 45 and 49, the install came first, so that one commit holds the code and its records.
- **The build, right before** (22:18:03 to 22:18:05): `assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug`: BUILD SUCCESSFUL in 1s, 79 of 80 tasks up to date. Only `lintDebug` ran; its report is still the one from 22:13:05, "No issues found." No code had changed since section 50.
  - The APK was not rewritten (22:06:44, `cc6f6caf…`), so it is the one checked on the emulator in section 50: OK (166 tests), and 335 unit tests with 0 failures.
- **Before** (read-only checks at 22:18:44, phone time; no records were read):
  - The phone ran `2e281497…` (`465a5e1`, section 49), installed at 21:53:14.
  - DueWise was running with only Home in its task (no form), and the screen was dozing. So the install closed it, with nothing unsaved.
  - The household database's main file header said 4. The main database file and its empty WAL (0 bytes) still had their times from 07:25:11, so no records had been written since then. The `-shm` file and the lock files were from 21:55:44 (DueWise was opened after the 21:53 install), and `shared_prefs/reminders.xml` was from 09:32:29 (the daily check).
  - CAMERA and POST_NOTIFICATIONS were granted (USER_SET), the daily-check job was present (due in about 11 h 14 min, so about 09:32), and sample data was closed. The crash buffer had no DueWise line, and the events log had no `am_crash` or `am_anr` for DueWise. No DueWise notification was shown.
- **The install** (`adb -s ZY22KN9QJP install -r`, 22:18:54 to 22:19:04 by the PC's clock, "Success"):
  - `base.apk`'s SHA-256 is `cc6f6caf…`, the build checked in section 50.
  - `lastUpdateTime` is 22:19:06 (phone time). `firstInstallTime` is still 2026-10-04 13:26:35, so the data was kept.
- **After** (22:19:15, phone time):
  - The listing of the app's folder (names, sizes, and times; 43 entries) was identical to the one before, and the header still says 4. This build has no database change.
  - Both permissions were still granted with the same flags, the daily-check job was present, and sample data was closed.
  - The crash buffer had no DueWise line, and the events log had no `am_crash` or `am_anr` for DueWise.
  - DueWise was not running, and no DueWise notification was shown.
  - Claude Code did not open DueWise on the phone, take screenshots, or press any key there.
- **Committed** on `main` after this section was written, as the next commit after `465a5e1`, with sections 50 and 51.
- **Not checked on the phone (the student's checks):**
  - Add batch without "Take label photos" (the product box right after "What kind of item?")
  - TalkBack

## 52. The Purchase/Renewal list and its Renewal section (2026-10-24)
- **The student's request:** "When a document expires , it should appear under Renewal, which should be a subsection inside "Purchase list". You should rename "Purchase list" to "Purchase/Renewal list". Inside the renewal section , there should be an option to select that the item has been renewed and the new date or discard - when it should disappear from the list. implement this functionality and ask me clarifying questions."
  - Claude Code read the Purchase list code and asked four questions (AskUserQuestion). The student chose "Documents and subscriptions (Recommended)", "Today or earlier (Recommended)", "Discard and archive (Recommended)", and "Same record, new date (Recommended)".
- **What changed** (details in SPEC.md, "Purchase/Renewal list"):
  - `RenewalList` (new, domain): documents and subscriptions with quantity 1 or more whose status from the date rules is "Date is today" or "Date passed", in the review list's order. `RenewalListTest` (new, 6 unit tests).
  - `ReadyShelfRepository`: `renewBatch` (a new confirmed date, the same label type, one Edited history entry; nothing is saved if the batch is gone, archived, or has another quantity or date) and `discardAndArchive` (the whole quantity recorded as discarded, then archived, with two history entries, in one transaction). No new history type and no database change. `ReadyShelfRepositoryTest`: 7 new instrumented tests (saved values, a changed or archived batch, and a database error that must change nothing).
  - `BatchRow`, `BatchAdapter`, and `item_batch.xml`: a card can have a second, outlined button.
  - `PurchaseListViewModel`, `PurchaseListActivity`, and `activity_purchase_list.xml`: the Renewal section at the end, with **Renewed** (Android's date picker from tomorrow, then "Save the new date?") and **Discard** ("Discard and archive?").
  - `strings.xml`: "Purchase/Renewal list" as the title and Home's tile word, Home's swap note, the intro "What to replace or renew, and what you plan to buy.", and the Renewal texts. Comments in `RuleRunner` and `ItemCategory`.
  - SPEC.md and DEMO.md (the thirty-ninth update, step 6).
- **Code:** uncommitted, on top of `1a49187`. Nothing was installed on the phone.

### 52.1 Build and tests
- **Build** `assembleDebug assembleDebugAndroidTest testDebugUnitTest` (11:20:51 to 11:21:19): BUILD SUCCESSFUL in 27s, 20 of 71 tasks executed.
  - Unit tests: **341, 0 failures, 0 errors, 0 skipped** (335 before, plus `RenewalListTest`'s 6).
  - The APK is from 11:21:16; its SHA-256 is `b9f9a0eb6456991f1eda26471d514a2792dee2e9f39cad90c4f1aa0af756e57c`. The test APK is from 11:21:16 too (`c72946d30beb915c86a5c0b38b582eeb417db11ae782ed7e1d9e8062b25f13d6`).
- **Lint** `lintDebug --rerun-tasks` (11:22:47 to 11:23:17, 27 of 27 tasks executed; report written at 11:23:16): **"No issues found."** The APK was not rewritten (still 11:21:16, `b9f9a0eb…`).
- **On the emulator:** both APKs installed with `install -r` (11:22:21 to 11:22:27, "Success"; `lastUpdateTime` 11:22:19). The installed `base.apk` has the same SHA-256 (`b9f9a0eb…`).
  - `am instrument -w` (11:22:39 to 11:27:09, "Time: 265.836"): **OK (173 tests)**, 166 before plus the 7 new ones. `ReadyShelfRepositoryTest` ran 16.
  - Afterwards the crash buffer had no DueWise line, and the events log had no `am_crash` or `am_anr` for DueWise.

### 52.2 On the emulator (its own records)
- **Before:** the emulator ran the build of section 50 (`cc6f6caf…`) with the Purchase list open, and its last touch was about 35 minutes earlier. A read-only copy of its database showed **"India Visa"** (Documents, 1 document in Documents Folder, Expires 23 Oct 2026, confirmed). Claude Code didn't add it, so it was left as it was.
- **Home** (11:27): the tile says "Purchase/Renewal list" on one line. "Date passed: 3 batches", "Near date: 4 batches".
- **Purchase/Renewal list:** the title "Purchase/Renewal list", the intro "What to replace or renew, and what you plan to buy.", then "On your purchase list" and "Replacement needs" as before. At the end, **Renewal** listed only India Visa, with "Date passed", **Renewed**, and **Discard** (spoken labels "Renewed: India Visa in Documents Folder" and "Discard: India Visa in Documents Folder"). EU Visa (31 Oct 2026, "Near date") and Test passport (14 Jan 2027) were not listed.
- **Two test records, added with Add batch** (11:29 to 11:32): "Test license" (Documents, 1 document in Pantry, Expires 23 Oct 2026, ticked) and "Test streaming" (Subscriptions, 1 subscription in Pantry, Expires 24 Oct 2026, ticked). Home then said "Date passed: 4 batches", "Date is today: 1 batch", and "Near date: 4 batches". Renewal listed India Visa, Test license ("Date passed"), and Test streaming ("Date is today"), in that order.
- **Renewed** on Test license (11:33):
  - The date picker opened at "Sun, Oct 25". The days up to 24 October were not enabled in the screen dump, and a tap on 24 October left Oct 25 chosen.
  - Next month, 30 November, OK: the dialog said "Save the new date?" and "Test license in Pantry: from Expires: 23 Oct 2026 to Expires: 30 Nov 2026."
  - **Save** (11:34): the message "Renewed: Test license, Expires: 30 Nov 2026." showed at the bottom, and the card left Renewal. In one burst frame, Test streaming's card was still sliding up into its place; the later frames show it in place.
- **The emulator froze** right after the next tap (Discard on Test streaming, about 11:35, before any dialog button was tapped):
  - Three screen dumps failed, and a screenshot never returned. `adb shell` (even `cat /proc/loadavg`) and the emulator console (`emu kill`) timed out, while `adb devices` still listed it.
  - Its `qemu-system-x86_64` process used 0 CPU seconds in 3 seconds, and Windows reported it as not responding. It had been running since 2026-10-22 11:45, about 48 hours.
  - A retry loop got no answer until 11:42:28. Claude Code then ended the process (`Stop-Process -Force`) and cold-booted the emulator (`-no-snapshot-load`, 11:43:54; booted at 11:44:50).
- **After the restart** (a read-only copy of the database): `PRAGMA integrity_check` "ok", version 4, and still 4 locations.
  - Test license: Expires 30 Nov 2026, confirmed, with the history entry `EDITED` "Renewed. Date changed from Expires: 23 Oct 2026 to Expires: 30 Nov 2026."
  - Test streaming and India Visa were unchanged, so the frozen Discard had saved nothing.
  - Home: "Date passed: 3 batches", "Date is today: 1 batch", "Near date: 4 batches". The load average was 20 after the boot, so Claude Code waited until it was below 3 (11:48:34).
- **Discard** on Test streaming (11:49):
  - The dialog said "Discard and archive?" and "Test streaming in Pantry. Its history says it was discarded, and Inventory hides it unless “Show archived batches” is on."
  - **Discard:** the message "Discarded and archived: Test streaming." showed at the bottom, and the card left Renewal; India Visa stayed.
  - The database: Test streaming has quantity 0 and is archived, with `DISCARDED` "Recorded as discarded: 1 subscription. Quantity changed from 1 subscription to 0 subscription." and then `ARCHIVED` "Archived, because it was discarded on the Purchase/Renewal list."
  - Home: "Date passed: 3 batches" and "Near date: 4 batches"; the "Date is today" tile was gone.
- **Font scale 2.0** (then back to 1.0): Home's tiles are one per row, and "Purchase/Renewal list" fits on one line.
- **After all of it:** no DueWise line in the crash buffer, and no `am_crash` or `am_anr` for DueWise since the restart. The emulator's records now also hold Test license (Pantry, Expires 30 Nov 2026) and Test streaming (archived). India Visa is still under Renewal.

### 52.3 Not checked, and remaining problems
- **Not checked:**
  - the phone (the student hasn't asked to install)
  - TalkBack itself, landscape, and right to left
  - on screen: "Not changed: this item changed or was removed." and a database error; the repository's side of both is covered by the new instrumented tests
  - turning the screen while the date picker or a dialog is open (they close, as the screen's other dialogs do)
- **Remaining:**
  - The history says "0 subscription" (and would say "0 document"): units are stored as typed, in the singular. "Record discarded" on Batch details writes the same words; this didn't change.
  - A document or subscription with nothing left is not listed under Renewal, but Home's "Date passed" tile still counts it until it is archived on Batch details (SPEC.md, "Not in this step").
  - Why the emulator froze is not known. It froze as a whole, with its console too, after about 48 hours of running. After the cold boot, the same Discard worked.

## 53. Phone install of section 52, and the commit (2026-10-24)
- **The student's request:** "install and commit". As in sections 49 and 51, the install came first, so that one commit holds the code and its records.
- **The build, right before** (12:02:54 to 12:02:56): `assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug`: BUILD SUCCESSFUL in 2s, 79 of 80 tasks up to date. Only `lintDebug` ran; its report is still the one from 11:23:16, "No issues found." No code had changed since section 52.
  - The APK was not rewritten (11:21:16, `b9f9a0eb…`), so it is the one checked on the emulator in section 52: OK (173 tests), and 341 unit tests with 0 failures.
- **Before** (read-only checks at 12:03:19, phone time; no records were read):
  - The phone ran `cc6f6caf…` (`1a49187`, section 51), installed at 2026-10-23 22:19:06.
  - DueWise was running with only Home in its task (no form), and the screen was dozing. So the install closed it, with nothing unsaved.
  - The household database's main file header said 4. The main database file and its empty WAL (0 bytes) still had their times from 2026-10-23 07:25:11, so no records had been written since then. The `-shm` file was from 11:00:18 (DueWise was opened this morning), and `shared_prefs/reminders.xml` from 09:42:16 (the daily check).
  - CAMERA and POST_NOTIFICATIONS were granted (USER_SET), the daily-check job was present (due in about 21 h 39 min, so about 09:42 on 2026-10-25), and sample data was closed. The crash buffer had no DueWise line, and the events log had no `am_crash` or `am_anr` for DueWise.
  - **One DueWise notification was shown:** the daily reminder (id 1001, high importance), from the 09:42 check. Its words were not read.
- **The install** (`adb -s ZY22KN9QJP install -r`, 12:03:46 to 12:03:56 by the PC's clock, "Success"):
  - `base.apk`'s SHA-256 is `b9f9a0eb…`, the build checked in section 52.
  - `lastUpdateTime` is 12:03:59 (phone time). `firstInstallTime` is still 2026-10-04 13:26:35, so the data was kept.
- **After** (12:04:05, phone time):
  - The listing of the app's folder (names, sizes, and times; 43 entries) was identical to the one before, and the header still says 4. This build has no database change.
  - Both permissions were still granted with the same flags, the daily-check job was present, and sample data was closed.
  - The crash buffer had no DueWise line, and the events log had no `am_crash` or `am_anr` for DueWise.
  - DueWise was not running.
  - **The daily reminder was no longer shown** (it was shown at 12:03:19, before the install). Opening DueWise doesn't show it again: Home only brings a shown reminder up to date (`Reminders.onReviewListShown`). The next daily check (about 09:42 on 2026-10-25) shows it if something still needs review, and Settings' "Show reminder now" shows it at once.
  - Claude Code did not open DueWise on the phone, take screenshots, or press any key there.
- **Committed** on `main` after this section was written, as the next commit after `1a49187`, with sections 52 and 53.
- **Not checked on the phone (the student's checks):**
  - the "Purchase/Renewal list" tile and title, and Renewal with the household's own documents and subscriptions
  - Renewed and Discard with real records (each one changes the record)
  - TalkBack
