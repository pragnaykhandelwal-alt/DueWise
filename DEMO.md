# DEMO.md — DueWise demonstration script

Written on 2026-10-12 by Claude Code (see AI-USE.md). It describes the app as it is on that day. Update it when the app changes.

Updated on 2026-10-18 by Claude Code: DatePolicy is connected, so the statuses, "Needs review", "Medicine dates to review", and the daily reminder work now.

Updated again on 2026-10-18 by Claude Code: RotationPlanner is connected, so the swap (step 5) and the purchase suggestion (step 6) work now. Steps 5 and 6 were checked on the emulator with this sample data.

Updated a third time on 2026-10-18 by Claude Code: the "Items need review" banner on Home (step 2), a reminder that pops up with a sound (step 8), and the home-screen widget "Review list" (step 9, and "Add the widget" below). They were checked on the emulator only, not yet on the phone.

Updated a fourth time on 2026-10-18 by Claude Code: batch cards with category icons and colored status labels (step 4), checked on the emulator only.

Updated an eleventh time on 2026-10-19 by Claude Code: Home has a row of three shortcut tiles (Inventory, Purchase list, Settings) and a floating "Add batch" button (step 3). After a save, a short message appears at the bottom. The home-screen widget is dark with the hourglass (step 9), and the start-up screen shows the hourglass. Batch details (step 7) and Settings (steps 2, 8, and 10) are now in cards; "Record used" is Batch details' main button, and the history is a timeline. Checked on the emulator only.

Updated a twelfth time on 2026-10-19 by Claude Code: the Add batch form is streamlined (step 3). It asks "What kind of item?" first, choices are one-tap chips, there is one product box, and the review has "Save and add another". There are three new categories: Cosmetics, Documents, and Subscriptions. Checked on the emulator only.

Updated a thirteenth time on 2026-10-19 by Claude Code: the swap texts are shorter (step 5). Home's note above the swap cards is three lines, and the Review swap screen folds "What you recorded" and the details for each place under "More about this". Every safety sentence stays in view. Checked on the emulator only.

Updated a fourteenth time on 2026-10-19 by Claude Code: the daily reminder names up to two of your own items and says how many more there are, but never names medicine (step 7). Checked on the emulator only.

Updated a fifteenth time on 2026-10-19 by Claude Code: Home's review lists start closed. **Show the review list** opens them, and **Hide the review list** closes them again (steps 2 and 7). A tap on a reminder or the widget opens Home at the top, with the lists closed (step 9). Checked on the emulator only.

Updated a sixteenth time on 2026-10-19 by Claude Code: the "Items need review" box shows every status tile again when it has 1 or 3 tiles (D-11; step 2). Checked on the emulator only.

Updated a seventeenth time on 2026-10-19 by Claude Code: in the "Items need review" box, the "Near date" tile says **Action needed date**, "Date needs review" has no tile, and **Show the review list** is a tile the same size as the others (step 2). Home's three shortcut tiles are always the same size. Checked on the emulator only.

Updated an eighteenth time on 2026-10-19 by Claude Code: all of Home's tiles, Inventory, Purchase list, and Settings too, form one square grid of two columns, with every icon in the same two columns (step 2). The gray box around the status tiles is gone. Checked on the emulator only.

Updated a nineteenth time on 2026-10-19 by Claude Code: Settings starts with **Reminders**, and its **Daily reminder** switch is right under the title (steps 7 and 8). Planning settings come next, and **Sample data** is still last (steps 2, 5, and 10). While sample data is open, **Close sample data** is the main button, with **Start sample data again** under it. Leaving Settings with a changed planning number asks "Leave without saving?". Checked on the emulator only.

Updated a twentieth time on 2026-10-19 by Claude Code: while sample data is open, every screen has a **Close sample data** button under the purple "Sample data" label (step 10), and Settings shows much less text: each card has one or two short lines, and the longer reminder texts are under **More about this**. Checked on the emulator only.

Updated a twenty-first time on 2026-10-19 by Claude Code, after a whole-app UI review (you chose five improvements; SPEC.md, "UI review of 2026-10-19"):
- Home's gold tile says **Near date** again, the same word as Inventory's chip (step 2).
- Inventory's filters are folded under **Filters**; the chosen ones show as chips under the search box (steps 2 and 4).
- **Review and save**, **Confirm and save**, **Review**, **Confirm**, and **Confirm swap** are in a bar at the bottom of their screens, always in view (steps 3, 5, and 7).
- **Add batch** shrinks to a round **+** while a screen scrolls.
- Back on a filled-in batch form asks **Leave without saving?** (step 3).

Checked on the emulator only.

Updated a twenty-second time on 2026-10-21 by Claude Code: the name is spelled **DueWise** again, with a capital W, everywhere: under the app's icon, in the logo at the top of Home, in the widget, in the notifications, and in the notes on the screens. Checked on the emulator only.

Updated a twenty-third time on 2026-10-21 by Claude Code: the descriptions on every screen are shorter, at your request (SPEC.md, "Short texts of 2026-10-21"). The safety sentences are still in view, in fewer words, and the quotes in steps 2 to 7 match the new texts. Checked on the emulator only.

Updated a twenty-fourth time on 2026-10-21 by Claude Code: Home has no note above the swap cards (your choice). Review swap still says "Replacement days are a planning choice, not a safety guarantee." (step 5). Checked on the emulator only.

Updated a twenty-fifth time on 2026-10-22 by Claude Code: the app is light, in neutral grays ("Graphite light", your choice from three previews). The statuses keep their colors as pale labels with dark words, the sample tags are lilac, the logo is pine and brass, and the widget stays dark. Checked on the emulator only.

Updated a twenty-sixth time on 2026-10-22 by Claude Code: new icons, Google's Material Symbols in the Rounded style (your choice from four versions). The statuses have solid icons and everything else gray outlines; the pantry has shelves, other places a map pin, the category Other a triangle, square, and circle, and Settings' Reminders card a bell (steps 2, 3, and 5). Checked on the emulator only.

Updated a twenty-seventh time on 2026-10-22 by Claude Code: Home has no "Items need review" heading, no **Show the review list** tile, and no review lists (your choice: they were redundant). Each status tile opens Inventory with its batches, medicine too (steps 2, 7, and 9). Checked on the emulator only.

Updated a twenty-eighth time on 2026-10-22 by Claude Code: the Purchase list has no "Suggestions" section (your choice). Each replacement need has **Add to purchase list**, and expired food, water, first aid supplies, cosmetics, and Other items are listed in any place, not only in kits; medicine, documents, and subscriptions are not (step 6). Checked on the emulator only.

Updated a twenty-ninth time on 2026-10-22 by Claude Code: Settings has no **Sample data** card any more (your choice), so sample data can't be opened, started again, or kept, and the demo runs on your own records. The items you already copied with **Keep these items as my own** are your own records and stay as they are. Steps 2, 3, 5, 6, 7, 9, and 10 now say what changed. **The numbers in the steps still come from the sample data** (for example "Showing 3 of 6 batches"), so check each step against your own records on the phone before you record. Checked on the emulator only.

Updated a thirtieth time on 2026-10-22 by Claude Code: Inventory opened from a status tile has only a home icon at the top left, in place of the back arrow and "Inventory" (your request); a tap on it returns to Home (step 2). Inventory opened from Home's **Inventory** tile still has the back arrow. Checked on the emulator only.

Updated a thirty-first time on 2026-10-22 by Claude Code: the Add batch form is simpler (your four choices; SPEC.md, "Simpler form (2026-10-22)"). **Take label photos** is one button; a new product has only a name and a unit; the label type is one line with **Change**; "What is printed?" is folded under **No full date on the package?**; and for Food, the food type and "Shelf-stable?" questions are gone and Sealed, Damaged, and Storage concern are one tick (steps 3 and 4). Checked on the emulator only.

Updated a thirty-second time on 2026-10-22 by Claude Code: the Add batch form has no food section any more, and Batch details has no "Food condition" card (your choices). Swaps and replacement needs now cover food, water, first aid supplies, cosmetics, and Other, and only the dates decide; medicine, documents, and subscriptions are never swapped (steps 3, 5, and 6). Checked on the emulator only.

Updated a thirty-third time on 2026-10-23 by Claude Code: new colors on every screen, from your design B1, "Brass highlights" (your choice for the whole app). Home has a pine band behind the logo and the tiles, with light status-bar icons, white tiles, and a brass **Add batch**. Main buttons are pine, cards and panels are white, and a status is a red, brass, or cream label, as on its tile (steps 2, 4, and 5). Checked on the emulator only.

Updated a thirty-fourth time on 2026-10-23 by Claude Code: every screen has Home's pine top bar with white words and icons (your choice), and a list that a status tile opens starts with that tile as its header on a pine band, in place of the chip (step 2). Turned sideways, the pine reaches the screen's edges. Checked on the emulator only.

Updated a thirty-fifth time on 2026-10-23 by Claude Code: Settings' **Send a test reminder** became **Show reminder now** (your request and choices). It shows the real reminder at once, with up to two of your own items and the rest as "others", for example "Medicine, Beans, and 5 others". The daily reminder now says "others" too, in place of "more". When nothing is near date or passed, a message says so and no notification appears (steps 7 and 8). Checked on the emulator only.

Updated a thirty-sixth time on 2026-10-23 by Claude Code: the number of items that need attention, as you asked ("similar to LinkedIn"). It is Home's tiles added up (your choice). The reminder carries it, so DueWise's icon gets a dot while the reminder is shown, and a saved change updates it without a second sound. Your phone's and the emulator's home screens show a dot, not the number; some phones, like many Samsung ones, can show the number on the icon. The widget shows it too, for example "7 household items need review" (steps 8 and 9). Checked on the emulator only.

Updated a thirty-seventh time on 2026-10-23 by Claude Code: Home's **Add batch** is a tile now, the last one in the grid, in the cell that was empty next to **Settings** (your request and choices). It looks like Inventory, Purchase list, and Settings, and Home has no floating button. When Add batch is alone in its row (with one or three status tiles), it fills the row, so the grid has no empty space. Inventory keeps its floating **Add batch** (steps 2 and 3). Checked on the emulator only.

Updated a thirty-eighth time on 2026-10-23 by Claude Code: the Add batch form has no **Take label photos** button any more (your request: you don't use label photos), so the product box comes right after "What kind of item?" (step 3). The demo never used label photos, so no step changed. Checked on the emulator only.

Updated a thirty-ninth time on 2026-10-24 by Claude Code: the Purchase list is now the **Purchase/Renewal list** (your name), with a new **Renewal** section at the end (your request and choices). Documents and subscriptions dated today or earlier are listed there, each with **Renewed** (pick the new date, then **Save**) and **Discard** (recorded as discarded, then archived). Both ask first, and the item then leaves the list (step 6). Checked on the emulator only.

Updated a tenth time on 2026-10-18 by Claude Code: the top of Home shows the DueWise logo (the hourglass and the name, in the logo's off-white and brass) instead of the plain title. Checked on the emulator only.

Updated a ninth time on 2026-10-18 by Claude Code: a tap on a status tile on Home opens Inventory with only that status's batches, and Inventory has Status chips (steps 2 and 4). Checked on the emulator only.

Updated an eighth time on 2026-10-18 by Claude Code:
- Inventory's filters are chips and a two-button switch (step 4).
- Warnings and errors have their own look.
- Long explanations fold under "More about this" (step 5).
- Checked on the emulator only.

Updated a seventh time on 2026-10-18 by Claude Code: DueWise is always dark, and Home's status tiles are dark cards with colored icons and counts (step 2). Checked on the emulator only.

Updated a sixth time on 2026-10-18 by Claude Code: the name is spelled "Duewise", the app has a blue color scheme (not taken from the logo), Home's "Items need review" box shows status tiles (step 2), and the swap card shows a swap picture (step 5). Checked on the emulator only.

Updated a fifth time on 2026-10-18 by Claude Code: the app is now called DueWise and has the student's hourglass icon (SPEC.md, "Name and icon"). The app ID, the database files, and the project folder keep the old ReadyShelf name on purpose, so the household's data stays.

## What the demo can show today
Every step works now:
- your own records, with the items you kept from sample data (since 2026-10-22 sample data can't be opened)
- the status tiles at the top of Home, which open Inventory with those batches (step 2)
- manual entry, Inventory, and Batch details, with each batch's status
- the swap and its Review swap screen (step 5), and the replacement need and Renewal on the Purchase/Renewal list (step 6)
- medicine under Home's "Date passed" tile, with its "Date passed" status in Inventory (step 7)
- stock changes, including the medicine notes
- airplane mode, the reminder that pops up when you tap **Show reminder now**, and the daily reminder (turn it on in Settings)
- the home-screen widget with the number of items that need review, and the dot on DueWise's icon while the reminder is shown (steps 8 and 9)

## Before the demo

### Open the project in Android Studio
1. Start Android Studio. Choose **File → Open**, select `C:\Users\dkhandelwal\AndroidStudioProjects\ReadyShelfJava`, and click **OK**. If it's in the list of recent projects, you can click it there.
2. Wait until Gradle sync finishes. The status bar at the bottom shows its progress.
3. To run on the phone from Studio:
   1. Connect the phone with USB, with USB debugging on.
   2. Choose "motorola moto g play - 2024" in the device menu at the top.
   3. Click **Run ▶**. Studio installs over the existing app and keeps its data.

### Run the tests (PowerShell, in the project folder)
- `JAVA_HOME` must be `C:\Program Files\Android\Android Studio\jbr` (check with `echo $env:JAVA_HOME`).
- **Unit tests:** `.\gradlew.bat testDebugUnitTest`. The report is `app\build\reports\tests\testDebugUnitTest\index.html`.
- **Instrumented tests: on the emulator only, never on the phone.** Start the Pixel_10a emulator first.
  ```
  .\gradlew.bat assembleDebug assembleDebugAndroidTest
  adb -s emulator-5554 install -r -t app\build\outputs\apk\debug\app-debug.apk
  adb -s emulator-5554 install -r -t app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk
  adb -s emulator-5554 shell am instrument -w com.example.readyshelf.test/androidx.test.runner.AndroidJUnitRunner
  ```
  - Don't use `connectedAndroidTest`. It also runs on the connected phone and uninstalls DueWise there, which deletes its data.
  - `adb` is in `%LOCALAPPDATA%\Android\Sdk\platform-tools`.

### Build and install the APK
- **Build:** `.\gradlew.bat assembleDebug`. The APK is `app\build\outputs\apk\debug\app-debug.apk`.
- **Install on the phone:** `adb -s ZY22KN9QJP install -r app\build\outputs\apk\debug\app-debug.apk`. The `-r` replaces the app and keeps its data.
  - `.\gradlew.bat installDebug` also works and keeps data, but it installs on every connected device, the emulator too.
- **Never uninstall DueWise, and never use "Clear storage" for it.** Either one deletes all household data.
- **First start after an install:**
  - DueWise upgrades its database if the old build used an older version. Your data is kept: the upgrade is tested in `MigrationTest`.
  - After an upgrade, the older build can't open the data.
  - On 2026-10-12 this build was installed on the phone, and its first start upgraded the phone's database from version 1 to 3. Before that, a copy of the version 1 files was kept in DueWise's own private folder on the phone (`files/pre-upgrade-2026-10-12`). It never left the phone.
- **Signing:**
  - This is a debug build, signed with Android Studio's debug key. The key is in `%USERPROFILE%\.android`, not in the project, and `.gitignore` keeps key files out of Git.
  - A debug build is for testing and the demo, not for an app store.

### Add the widget
- Touch and hold an empty spot on the phone's home screen → **Widgets** → find **DueWise** → **Review list**.
- On the Pixel emulator, tapping the preview shows an **Add** button. On other home screens you may need to drag the preview onto the home screen. The steps on the moto g play were not checked.
- It can be resized: touch and hold it, then drag its edges.

### On the day of the demo
- Charge the phone.
- **Turn off Do Not Disturb** (swipe down from the top of the screen). While it is on, a reminder doesn't pop up or make a sound; it only appears in the notification list. On 2026-10-18 it was on on the phone.
- **Don't change the phone's date or time.** The items you kept from sample data have fixed dates, counted from the day you opened sample data before keeping them, so each day they are one day closer. Check their statuses on the day of the demo.
- Practice the steps once.

## The items from sample data
These are the made-up items that sample data had. Since 2026-10-22 sample data can't be opened. If you copied them into your own records with **Keep these items as my own**, they are there with the dates they had that day, not marked "Sample item". In sample data, the dates were counted from the day it was opened; the example column is for 12 Oct 2026.

Since 2026-10-22 the kit is called **Emergency kit** (before: **Earthquake kit**). A build from that day or later renames it in your records the first time it opens. Older history entries still say "Earthquake kit".

| Item | Where | Amount | Date | Why it's there |
|---|---|---|---|---|
| Granola bars (Oat and honey, 1.5 oz, counted in bars) | Emergency kit | 6 bars | Best by: today + 20 days (example: 1 Nov 2026), confirmed | The kit batch near its date |
| Granola bars, the same product | Pantry | 6 bars | Best by: today + 240 days (example: 9 Jun 2027), confirmed | The replacement |
| Granola bars, the same product | Pantry | 3 bars | Only "MM/YYYY" printed, 9 months ahead (example: "07/2027"), not confirmed | Stays out of every swap |
| Canned beans (Black beans, 15 oz) | Emergency kit | 4 cans | Best by: today + 14 days, confirmed | No pantry beans, so nothing can replace them |
| Pain reliever tablets (24 tablets) | First aid box | 2 bottles | Expires: today − 10 days, confirmed | A medicine date that has passed |

- **The food:**
  - Every food batch has the food class "Shelf-stable food".
  - Its made-up answers are: sealed Yes, shelf-stable Yes, damaged No, storage concern No.
  - These are records only. DueWise does not check or judge food.
  - Since 2026-10-22 evening no screen shows the food class or the answers, and swaps don't use them. They stay in the copies' records.
- In sample data, every row was marked "Sample item". The copies in your own records are not.

## The demo, step by step

### 1. Airplane mode
- Swipe down from the top of the screen and tap **Airplane mode**. Wi-Fi and mobile data turn off.
- Open DueWise.
- Say: "DueWise has no internet permission. Everything stays on this phone."

### 2. Home's status tiles
- Until 2026-10-22 this step opened sample data from Settings. You removed Settings' Sample data card that day, so the demo shows your own records, and no screen has the "Sample data" label.
- At the top of Home, right under the logo, all of Home's tiles form one square grid of two columns, on a pine band (since 2026-10-23). The first tiles count the batches that need review, by status: each has a solid status icon in a colored circle and the count in a matching color. Your own records give your own numbers. With the sample data on the emulator on 2026-10-22, before the card was removed, the grid was:
  - row 1: red "Date passed" (1) and cream "Near date" (2) (the colors since 2026-10-23; gold until then)
  - row 2: **Inventory** and **Purchase/Renewal list** (called **Purchase list** until 2026-10-24; pine icons in cream circles; gray icons in white circles until 2026-10-23)
  - row 3: **Settings** and **Add batch** (a pine "+" in a cream circle; until 2026-10-23 that cell was empty, and Add batch was a floating button)

  Every row is the same height, and every icon sits in the same two columns. With one or three status tiles, **Add batch** is alone in the last row and fills it, so the grid has no empty space. A batch whose date needs review (for example the 3 granola bars with only "07/2027" printed) has no tile; Inventory shows it under **Filters → Status → Date needs review**. (Until 2026-10-22 Home also had an "Items need review" heading and a **Show the review list** tile that opened two lists; you removed them as redundant.)
- Say: "When I open the app, it tells me right away what needs attention."
- Tap the **Near date** tile (each status tile has a small ">" arrow). Inventory opens with its filters folded and only the batches that the tile counted. Under the pine top bar, the same **Near date** tile is the list's header, with the tile's number, on a pine band (since 2026-10-23; until then a **Near date ✕** chip under the search box said it). The top bar has only a home icon (no back arrow and no "Inventory"). Tap it to return to Home.

### 3. Manual entry
1. Home → the **Add batch** tile, the last tile on the pine band (until 2026-10-23 a floating button at the bottom right).
2. **What kind of item?** Tap **Water**. (Since 2026-10-22 evening the form asks no food questions, for Food either.)
3. **The product:** your own records have products, so the form starts with the product box. Type `Bottled water`, then tap **Add “Bottled water” as a new product** (if you already have that product, choose it from the list instead and skip the unit).
   - Product name: `Bottled water` (already filled in from the box)
   - Unit: tap **bottles** (one of the choices under the field). That is all a new product needs (since 2026-10-22 there is no variant or package size).
4. **Where and how many:**
   - Location: tap **Emergency kit**
   - Quantity: type `12` (it starts at 1; **+** and **−** change it by one)
5. **The date:**
   - The line says **Label type: Best by**. Keep it (**Change** would show the other label types).
   - Tap **Choose date**, and pick a date about a year away. (**No full date on the package?** under it is for a package with only part of a date or none; leave it alone.)
   - Tick **"I checked the date and label type on the package."** (right under the date)
6. Tap **Review and save** in the bar at the bottom (it stays in view however far the form scrolls) and read the review. Then tap **Save and add another** under the review: the water is saved, "Batch added." appears, and an empty form opens with Water and Emergency kit already chosen.
   - Since 2026-10-22 this saves a real batch in your own records. Use water you really have, or archive the batch afterwards (Batch details).
   - Optional: before saving, show that Back doesn't lose your work: on the form, after typing, tap the back arrow. DueWise asks **Leave without saving?**; tap **Keep editing**.
7. Optional, to show a new kind: on the empty form, tap **Documents**. The form asks no quantity and no unit ("Saved as 1 document."), and the label type starts at **Expires**. Then tap the back arrow: the form closes at once (a category alone isn't asked about), and nothing is saved.
8. Inventory now lists the water. The category chips include Cosmetics, Documents, and Subscriptions.

### 4. Inventory: the three granola batches
- **Inventory** → search `gran`: "Showing 3 of 6 batches". The keyboard's search key closes the keyboard.
  - The filters are folded under **Filters**, so the list starts near the top. Tap **Filters**: tap **Emergency kit** under "Location" to show only the kit's batches. "Soonest first | Latest first" switches the order.
  - Under "Status", the chips show only the batches with one status, for example **Date needs review**; **All statuses** shows everything again.
  - Tap **Hide filters**: each chosen filter is now a chip under the search box, for example **Emergency kit ✕**. A tap on a chip sets that filter back; with two or more, **Clear all** sets them all back.
  - 6 bars in Emergency kit
  - 6 bars in Pantry
  - 3 bars in Pantry, "Best by: “07/2027” (incomplete date)", with "Date needs review: only part of a date."
  - Each batch is a card with its category icon. Its status is a label with an icon and a color: cream "Near date" (gold until 2026-10-23) for the kit bars, gray "Later date" for the 6 pantry bars, and blue "Date needs review" for the 3 bars.
  - Say: "The words say the status. The color and the icon only help, so nobody has to tell colors apart."
- Tap the 3-bar batch → **Edit batch**. "What is printed?" is open by itself at **Part of a date** with `07/2027`, and the form offers **"Use the last day of the month (31 Jul 2027)"**.
  - Say: "DueWise never makes up a day. Even this button only fills in a date that still needs my check."
  - Go back with the **←** arrow without saving.
- Say: "Only a complete, confirmed date can be part of a swap, so these 3 bars stay out."

### 5. A swap: its reason and the unchanged totals
- Home → **Recommended swaps** shows one card, for the granola bars, with the swap picture (**More about this** above it says what is never swapped: medicine, documents, subscriptions, and kit items dated today or earlier):
  - "Emergency kit → Pantry", with "6 bars · Best by: … · for review" under it
  - "Pantry → Emergency kit", with "6 bars · Best by: …" under it
  - Say: "The kit bars that are near their date go to the pantry for review, and newer pantry bars take their place."
  - Since 2026-10-22 evening your own records may show more cards, for example for kit water near its date with a later pantry batch of the same product.
- Then tap **Review swap**:
  - "Why the replacement qualifies": a one-line summary, and "Replacement days are a planning choice, not a safety guarantee." Tap **More about this** to show every rule:
    - the same product and unit
    - a later date, at least the replacement days (180) after today
    - the kit date 1 to 30 days away
    - both batches with confirmed full dates whose label type is Best by, Use by, or Expires
    - medicine, documents, and subscriptions are never swapped (since 2026-10-22 evening; before that, only shelf-stable food recorded as sealed, not damaged, and with no storage concern)
  - The same fold also has "What you recorded": the product, and each batch's date, place, and "Date and label type confirmed."
  - "After the swap": a small Before/After table. Emergency kit: 6 bars before, 6 bars after. Pantry: 9 bars before, 9 bars after. The totals don't change, because 6 bars go each way. **More about this** under the table gives the details for each place.
  - **Confirm swap** (in the bar at the bottom, in view the whole time) saves both moves together. Home then shows "No swaps to suggest right now."
  - Since 2026-10-22 there is no **Start sample data again**, and the swap moves your own batches. Once confirmed, the swap can't be shown again. Stop before **Confirm swap** while you practice, and confirm only in the take you keep.
- Say:
  - "Nothing moves until I confirm."
  - "The 180 days are a planning choice, not a safety guarantee."

### 6. A replacement need, and Renewal
- Open **Purchase/Renewal list** (called **Purchase list** until 2026-10-24):
  - "Replacement needs" lists the canned beans in the Emergency kit: "Still needs replacement: 4 cans. No pantry batch qualifies as a replacement."
  - With your own records the card has **Add to purchase list**: after a confirmation it puts the item under "On your purchase list". (With sample data the card said "Sample items can't go on the purchase list." and had no button; any old item still marked "Sample item" says that too.)
  - Expired food, water, first aid supplies, cosmetics, and Other items are listed too, wherever they are (since 2026-10-22). Medicine never is. The items from sample data have none of these.
  - Since 2026-10-22 evening a kit's water, first aid supplies, cosmetics, and Other items near their date are listed too when no swap covers them, like kit food.
  - Until 2026-10-22 there was also a "Suggestions" section; you removed it.
- **Renewal**, at the end of the screen (since 2026-10-24): your documents and subscriptions dated today or earlier, each on its card with "Date is today" or "Date passed". With nothing to renew it says "Nothing to renew right now."
  - **Renewed:** pick the new date (tomorrow or later), then **Save** in "Save the new date?". The same record gets the new date, and its history says "Renewed. Date changed from … to …". It leaves Renewal and Home's "Date passed" tile.
  - **Discard:** **Discard** in "Discard and archive?". It is recorded as discarded and archived, so it leaves Renewal, Home's tiles, the reminder, and the widget. Inventory shows it again with **Show archived batches**.
  - Both change your records. Practice with a test item, and confirm only in the take you keep.
  - Say: "Documents and subscriptions are renewed, not bought, so they have their own section."

### 7. Medicine, without medicine advice
- **Home → the "Date passed" tile:** Inventory opens with only the batches whose date passed. Medicine counts like any other batch (since 2026-10-22, your choice; Home had a separate "Medicine dates to review" list before).
  - Pain reliever tablets is listed with the status "Date passed".
- **Inventory → Pain reliever tablets** (Batch details): "Expires: …" with the past date.
- **Record discarded** shows: "This only records your stock. It isn't medicine advice." Go back without confirming: with your own records, confirming changes them.
- **What to say:** only what the screen says. Don't say whether the medicine can still be used.
- **The daily reminder:**
  - It says "Household items need review" and names up to two of your own items, soonest date first, with how many others there are. For example: "Canned beans, Granola bars, and 3 others".
  - It never names medicine. It says "medicine" instead, for example "Medicine and Canned beans".
  - On a locked phone, the names show only if the phone's lock-screen setting shows sensitive content. Otherwise the lock screen says "Open DueWise to see the review list." instead.
  - It checks only your own records, never sample data.
  - It starts off. Turn it on in **Settings → "Daily reminder"**; Android asks to allow notifications.
  - It pops up with the phone's notification sound. Do Not Disturb and silent mode still apply.

### 8. A reminder works offline
- **Settings → Show reminder now.** If Android asks, tap **Allow**. The reminder pops up at the top of the screen with a sound, still in airplane mode: "Household items need review" with up to two of your own items and how many others there are (on the emulator: "Medicine, Beans, and 5 others"). The message at the bottom says "Reminder sent. Look in your notifications."
  - It doesn't change "Last reminder", and the daily reminder still comes as usual.
  - When nothing is near date or passed, no notification appears, and the message says "Nothing is near date or passed. No reminder to show."
- With Do Not Disturb on, or the phone on silent, it doesn't pop up or make a sound; it is only in the notification list.
- **The number on the icon (since 2026-10-23):** press the phone's Home button. While the reminder is shown, DueWise's icon has a dot. The reminder carries the number of items that need attention (Home's tiles added up). Your phone and the emulator show only the dot; some phones show the number itself on the icon. Tapping or swiping the reminder away removes the dot.
  - While the reminder is shown, a saved change updates it quietly, with no second sound, so the number stays right.

### 9. The home-screen widget
- Press the phone's Home button. The **Review list** widget (dark, with the hourglass and "DueWise") says how many items need review, for example "7 household items need review" (Home's tiles added up), or "Nothing needs review right now.".
- It shows only your own records, never the sample data. It never names items, because anyone can see the home screen. Since 2026-10-23 it gives one number (your choice); before that it gave no counts.
- Tap it: DueWise opens Home at the top, at the status tiles.
- Say: "The widget never names items. It only says how many need review."

### 10. Finish
- Turn airplane mode off again if you need it.
- Until 2026-10-22 this step closed sample data. Sample data can't be opened any more, so there is nothing to close. If a phone still shows the "Sample data" label (it was open when the new version was installed), tap **Close sample data** under the label, then **Close**: the made-up items in it are deleted, and your own records come back.

## What not to say
- **No claims about waste.**
  - DueWise has measured nothing about waste, and there is no evidence that it reduces waste.
  - Describe what it does. For example: "It shows which kit items are near their date and proposes a swap with a later pantry batch."
- **Never say food is "safe" or "still good".** DueWise reports dates and the household's own answers.
- **No advice about taking, replacing, or throwing away medicine.**
- **Who made what:**
  - The date rules (`DatePolicy`), the date-candidate rules (`DateCandidateParser`), and the swap rules (`RotationPlanner`) were written by Claude Code at your request on 2026-10-18, after you approved their test cases. Don't present them as your own code.
  - The "Items need review" banner, the reminder that pops up with a sound, and the home-screen widget were also written by Claude Code at your request on 2026-10-18, after you chose them from Claude Code's suggestions.
  - The batch cards with category icons and the colored status labels were written by Claude Code at your request on 2026-10-18, after you chose them from Claude Code's UI suggestions. The icons were Google's Material Icons; since 2026-10-22 they are Google's Material Symbols (see the icons below).
  - The app's blue color scheme, the status tiles on Home, and the swap picture with the Before/After table were made by Claude Code at your request on 2026-10-18, after you chose them from Claude Code's suggestions. You asked for the colors to follow best practice instead of the logo, and Claude Code chose the blue. You chose to make the app always dark, and Claude Code designed the dark tiles. On 2026-10-22 you asked for other colors by best practice: Claude Code suggested three schemes and built previews of the one you picked, Graphite, in a softer dark and a light version, and you chose "Graphite light" from the screenshots. Claude Code generated the colors and changed the app.
  - The icons as they look since 2026-10-22: you asked Claude Code to look at Home's icons and suggest better ones by design best practice. Claude Code reviewed them, showed four versions on a preview page, and recommended one. You chose B: Google's Material Symbols in the Rounded style (statuses solid, everything else gray outlines), with a bell on Settings' Reminders card. Claude Code downloaded and converted the icons and changed the app.
  - Inventory's filter chips, the warning and error boxes, and "More about this" were also written by Claude Code at your request on 2026-10-18, after you chose them from Claude Code's suggestions.
  - Tapping a status tile to open Inventory with only those batches (and Inventory's Status chips) was your idea. Claude Code wrote it at your request on 2026-10-18, after you chose where a tap goes (Inventory with Status chips), the arrow on each tile, and keeping "Show the review list".
  - The second UI round (2026-10-19) was written by Claude Code at your request, after you picked the items from Claude Code's suggestions:
    - Home's shortcut tiles and the floating "Add batch" button (you chose one row of three tiles plus the floating button)
    - the save messages
    - the dark widget with the hourglass (you chose always dark)
    - the hourglass start-up screen
    - Batch details and Settings in cards
  - The streamlined Add batch form and the categories Cosmetics, Documents, and Subscriptions were written by Claude Code at your request on 2026-10-19:
    - You asked for the three categories and a friendlier form.
    - You picked all four changes Claude Code suggested: the category first, one product box, one-tap choices, and "Save and add another".
    - You chose that documents and subscriptions skip the quantity and the unit, and you kept the date's tick box.
    - Claude Code chose the icons (Google's Material Icons) and the unit choices for each category.
  - The simpler Add batch form (2026-10-22) was your request: you found the form "very complex" and asked for ways to simplify it, with permission to remove fields that are not mandatory. Claude Code counted the form's controls, traced each field to the code that reads it, and suggested changes. You chose Claude Code's recommended options for the food section (no food type or "Shelf-stable?", and one tick for Sealed, Damaged, and Storage concern) and for the date (the label type as one line with "Change", and "What is printed?" folded), and both smaller cuts it offered (no variant or package size for a new product, and the label photos as one button). Claude Code wrote them.
  - The form without a food section, and swaps for more categories (2026-10-22, evening), were your request: you asked to remove the "Food condition" section shown in your screenshot, and to drop the "only sealed items" logic so that everything is a candidate for replacement, to keep things simple. Claude Code asked three questions. You chose "Food condition only" (recommended; the date part stays), "All but medicine, documents, subs" for swaps, and removing Batch details' "Food condition" card (recommended). Claude Code changed the app and the swap rules (`RotationPlanner`) with their tests, after you approved the changed test cases in its plan.
  - The shorter swap texts (2026-10-19) were written by Claude Code at your request. You asked for less text, and you chose a short note with "More about this" on Home and the folded details on Review swap.
  - The item names in the daily reminder (2026-10-19) were your idea: you asked for the top two items plus the others. Claude Code wrote them at your request, after you chose the one-line layout, "and 3 more" with the number, "medicine" in place of medicine names, and that the phone's lock-screen setting decides whether names show there.
  - **Show reminder now** (2026-10-23) was your request: you asked for the test reminder to show a real reminder with up to two items whose date passed or is near, and the rest as "others". Claude Code asked four questions. You chose "and 3 others" with the number, not counting it as the daily reminder, only a message when nothing is near or passed, and the label "Show reminder now" (all four were Claude Code's recommended options). Claude Code changed the app and its tests.
  - **The number on the app icon and in the widget** (2026-10-23) was your request: you asked for a number on the app icon, like LinkedIn's. Claude Code checked how Android shows such numbers and which home screen your phone uses (it shows a dot), then asked three questions. You chose to build it and also put the number in the widget, that the number is Home's tiles added up (recommended), and that the daily reminder carries it (recommended). Claude Code changed the app and its tests.
  - **Add batch as a tile on Home** (2026-10-23) was your request: on a screenshot you marked the empty space next to Settings and asked for Add batch there, as a tile with the same color scheme, instead of the floating button. Claude Code asked four questions. You chose the look of the other tiles, a wide Add batch when it would be alone in its row, keeping Inventory's floating button, and no floating button on Home (all four were Claude Code's recommended options). Claude Code changed the app and its tests.
  - Home's review lists starting closed (2026-10-19) was your idea: you asked for the list to show only after you tap "Show the review list". Claude Code wrote it at your request, after you chose a Show/Hide button, the short note when nothing needs review, and that reminder and widget taps leave the lists closed.
  - Removing the "Items need review" heading, the **Show the review list** tile, and Home's review lists (2026-10-22) was your idea: you found them redundant. You answered that medicine should count under "Date passed" or "Near date" like other batches, and that "Date needs review" still gets no tile. Claude Code changed the app and kept a short note under the tiles for when nothing needs review or the date rules can't run.
  - Removing the Purchase list's "Suggestions" and listing expired items under "Replacement needs" (2026-10-22) was your idea. You chose "Everything you'd buy again" (food, water, first aid supplies, cosmetics, and Other, in any place) and an **Add to purchase list** button on each need. Claude Code changed the app; medicine stays off the list because DueWise gives no advice about replacing medicine.
  - The fix for D-11 (2026-10-19), the banner's missing last tile, was written by Claude Code at your request.
  - The banner's tile changes (2026-10-19) were your requests: "Action needed date" on the "Near date" tile (that evening you chose "Near date" again, after the UI review), no "Date needs review" tile, and "Show the review list" the size of the other tiles. You also asked for the best practices of tile and icon layout on Home. Claude Code chose and wrote those: one size for every tile, the same parts on every tile, the review-list tile's look, one tile per row with large text, and shortcut tiles that are always the same size.
  - The square grid (2026-10-19) was your request: every icon on Home lined up in rows and columns, with no gap. You chose the order (Settings last) from Claude Code's three sketches. Claude Code wrote it, and gave the shortcut tiles the same look as the other tiles.
  - The Settings changes (2026-10-19, evening) were your request: UX improvements to the Settings screen by best practice. Claude Code chose them from Android's and Material's guidance, and you approved its plan. Claude Code wrote them:
    - Reminders first, with the switch at the top
    - the warning look for the notifications line
    - the keyboard's Next and Done
    - errors that go away once fixed
    - the "Leave without saving?" question
    - "Close sample data" as the main button while sample data is open
  - The second Settings round (2026-10-19, evening) was your request: you found closing sample data "confusing and complicated", and the Settings page "a huge wall of text". You chose "Short cards" from Claude Code's three layouts, asked for the close button under the purple label, and said the texts should get to the main point without becoming too short. Claude Code wrote the shorter texts and the button on every screen.
  - The whole-app UI review (2026-10-19, evening) was your request: you asked Claude Code to evaluate the app against UI best practices and suggest improvements. Claude Code wrote the evaluation (grades, seven problems found in the code) and twelve suggestions. You chose five, the ones Claude Code recommended, and "Near date" as the word everywhere. Claude Code wrote them:
    - the small fixes ("No changes to save.", "Leave without saving?" on the batch form, plain words for rule errors, the search key, the progress bar while photos are read, the note's text size, no empty boxes while a screen opens, and labels read with their questions)
    - "Near date" on the tile again, "swap proposals" in place of "automatic swaps", and Home's empty text
    - Inventory's folded filters with the chosen ones as chips
    - the bar at the bottom with the main button (batch form, Change stock, Review swap)
    - the "Add batch" button that shrinks while a screen scrolls
  - The shorter texts (2026-10-21) were your request: you asked for short, concise descriptions on every page, like Inventory and the review lists. Claude Code wrote the new words for 164 texts and kept every safety sentence in view. Later that day you chose to remove Home's note above the swap cards; Review swap keeps its safety sentence.
  - **The DueWise logo and app icon:** an AI draft that you edited. Write the AI tool, the date, and what you asked it for in AI-USE.md. Claude Code only put the logo kit's icon files into the app and changed the name on screen, at your request on 2026-10-18. Later that day, at your request, it also put the kit's horizontal logo at the top of Home. You chose the full logo, the colors for dark backgrounds, and the size; Claude Code converted the kit's SVG without changing it. On 2026-10-21, at your request, Claude Code changed one letter in the app's copy of the logo: the "w" became a capital "W". The "W" comes from the Instrument Serif Italic font file in your logo kit and is spaced like the kit's other letters. The kit's own files still say "Duewise".
  - "Keep these items as my own" (Settings, while sample data was open) was written by Claude Code at your request on 2026-10-18. You chose to copy everything as it is, and then to close sample data.
  - Removing Settings' Sample data card (2026-10-22) was your request: you asked to remove the section and keep the items that came from sample data. Claude Code changed the app: sample data can't be opened any more, the items already copied stay in your own records, and "Close sample data" under the label still works where sample data was open.
  - AI help is disclosed in AI-USE.md.

## Known limits and unfinished features (2026-10-12, updated 2026-10-18)
- **Sample data (since 2026-10-22):**
  - It can't be opened any more: you removed Settings' Sample data card. So the demo uses your own records, and each saved step changes them.
  - The items you kept from it are your own records. Their dates don't move, so each day they are one day closer to their date.
  - Items still marked "Sample item" (from a version before 2026-10-12) stay too. They never cause a reminder and can't go on the purchase list, and Home shows a banner about them.
- **Open items from the first review (TEST-RESULTS.md section 4.4):**
  - D-2: stock changes check only the quantity before saving
  - D-3 to D-5
  - D-6: an amount of one shows as "1 bars"
  - D-7 to D-10
- **Locations** can't be renamed or deleted.
- **The number on the app icon (since 2026-10-23):** your phone (Moto) and the emulator (Pixel) show a dot, not the number, and the emulator's long-press menu doesn't show it either. It shows only while the reminder is shown. After you save the planning settings, the number waits until Home is opened again.
- **Testing:**
  - The automated tests ran on the Android 17 emulator. The phone runs Android 14.
  - The banner, the reminder that pops up, and the widget were checked on the emulator only (2026-10-18). So were the names in the daily reminder and Home's new tile layout (2026-10-19), and the changes from the UI review (2026-10-19 evening; TEST-RESULTS.md section 26). So were the name spelled DueWise and the shorter texts (2026-10-21; sections 28 and 29). So were the number on the icon and in the widget (2026-10-23; section 46).
  - The phone checklist is in TEST-RESULTS.md section 7.
  - There is no release build.
- **What DueWise is not:** it is not medically validated, and it doesn't judge food. It records dates and the household's own answers.
