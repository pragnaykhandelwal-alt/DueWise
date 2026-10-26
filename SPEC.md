# SPEC.md — DueWise scope and screens

Agreed on 2026-10-04 between the student and Claude Code (see AI-USE.md). Products, date entry, food condition, and stock changes were added on 2026-10-11. Confirmable swaps, the purchase list, and the product food class were also added on 2026-10-11. Local date reminders and the planning-settings check were added on 2026-10-11 as well. The first version was reviewed against this file on 2026-10-11; the results, repairs, and open defects are in TEST-RESULTS.md. Sample data moved to a separate file on 2026-10-12, and DEMO.md has the demonstration script. Photo entry (CameraX, two photos, possible dates from the student's date-candidate parser) replaced "Scan a label" on 2026-10-12. The "Items need review" banner on Home, a reminder that pops up with a sound, and the home-screen widget were added on 2026-10-18. Batch cards with category icons, and status labels with an icon and a color, were also added on 2026-10-18 (see "Status labels and batch cards"). On 2026-10-19 the batch form was streamlined, and the categories Cosmetics, Documents, and Subscriptions were added (see "The batch form and its categories"). The same day the swap texts got shorter, with more of them under "More about this" (see "Swaps" and "Messages and More about this"). Also on 2026-10-19, Home's review lists started closed, behind the banner's "Show the review list" (see "Home banner"). Later that day the banner's tiles got one layout: "Show the review list" became a tile of the same size, "Date needs review" lost its tile, and the "Near date" tile said "Action needed date". Then all of Home's tiles, the shortcuts too, became one square grid of two columns (see "Home banner"). That evening, after a whole-app UI review by Claude Code, the student chose five improvements (see "UI review of 2026-10-19"): small fixes, the same words everywhere ("Near date" on the tile again), Inventory's filters folded under "Filters", each long screen's main button in a bar at the bottom, and an "Add batch" button that shrinks while the screen scrolls. On 2026-10-21 the name got its capital W back: "DueWise", in the logo at the top of Home too (see "Name and icon"). The same day every screen's texts got shorter (see "Short texts of 2026-10-21"), and Home lost the note above its swap cards (see "What the app does with the answers"). On 2026-10-22 the app became light: the student chose "Graphite light" from three previews (see "Name and icon"). The same day the student chose new icons from four versions: Google's Material Symbols in the Rounded style, solid for the statuses and outlines for everything else, with no picture on Home standing for two unrelated things (see "Status labels and batch cards"). Later that day the student removed the "Items need review" heading, the "Show the review list" tile, and Home's review lists as redundant: each status tile opens Inventory with its batches, medicine too (see "Home banner"). Then the Purchase list lost its "Suggestions" section: each replacement need got "Add to purchase list", and expired items the household buys again are listed in any place (see "Purchase list"). Later that day the student removed the Sample data card from Settings: sample data can't be opened any more, and the items already copied into the household's records with "Keep these items as my own" stay as they are (see "Sample data"). Later on 2026-10-22, at the student's request, Inventory got only a home icon in its top bar when a status tile opens it, in place of the back arrow and the title "Inventory" (see "Home banner"). That evening the student found Add batch too complex and chose four simplifications: the food section lost the food type and the batch's "Shelf-stable?" question and asks the three swap answers as one tick, the label type became one line with "Change" and "What is printed?" folds until it is needed, a new product has no variant or package size, and label photos are one button (see "Simpler form (2026-10-22)"). Later that evening the student removed the food section from Add batch and asked for everything to be a candidate for replacement, not only sealed food: the form asks no food class, food type, or condition, Batch details has no "Food condition" card, and swaps and replacement needs cover food, water, first aid supplies, cosmetics, and Other, with only the date rules deciding; medicine, documents, and subscriptions are never swapped (see "No food section, and swaps for more categories (2026-10-22, evening)"). On 2026-10-23 the student's design B1, "Brass highlights", replaced "Graphite light" on every screen: warm grays, white cards, pine main buttons, brass highlights, and a pine band behind Home's logo and tiles (see "Name and icon"). Later that day every screen got Home's pine top bar, a list that a status tile opens starts with that tile as its header, and in landscape the pine reaches the screen's sides (see "Name and icon" and "Home banner"). That evening the student asked for a number on the app icon, like LinkedIn's, and chose its rules: the number is Home's status tiles added up, the reminder carries it (so the icon shows a dot or the number, as the phone's home screen decides, while the reminder is shown), a shown reminder is brought up to date quietly, and the home-screen widget shows the same number, for example "7 household items need review" (see "Reminders", "Home-screen widget", and "Privacy"). Later that evening the student asked for "Add batch" in the empty cell next to Settings: it is now the last tile of Home's grid, like the shortcut tiles, in place of the floating button, and when it is alone in its row it fills the row, so the grid never has an empty cell. Inventory keeps its floating button (see "Home banner"). Later that evening the student hid "Take label photos" on Add batch, because the household doesn't use label photos: no button opens the Label photos screen any more, but the screen and its code stay (see "Photo entry"). On 2026-10-24 the student asked for expired documents to appear under "Renewal", a new section of the Purchase list, renamed "Purchase/Renewal list", with a way to record each one as renewed with its new date, or to discard it. Claude Code asked four questions, and the student chose documents and subscriptions, dated today or earlier, "Discard and archive", and "Same record, new date" (see "Purchase/Renewal list").
This file describes what the app does now and what is planned. Update it when the scope changes.

## Name and icon (changed 2026-10-18; spelling changed 2026-10-21)
- **The name:** the app was called ReadyShelf until 2026-10-18; the student renamed it DueWise. Later that day the student chose the spelling "Duewise", like the logo kit's name, and on 2026-10-21 the capital W again, everywhere. Every word on screen says DueWise now: the app's name under its icon, the Home title (now the logo; see below), the widget, the notifications, and the notes in the screens. AI-USE.md, TEST-RESULTS.md, and TOOLCHAIN.md keep the old name, and the old spelling, in what they recorded before.
- **Names that stay ReadyShelf, on purpose:**
  - The app ID `com.example.readyshelf`. A new ID would make Android treat DueWise as a different app, and the household's records would stay behind in the old one.
  - The database files (`readyshelf.db`, `readyshelf-sample-data.db`), the daily check's job name, the code's class and style names (for example `ReadyShelfApp`, `Theme.ReadyShelf`), and the project folder `ReadyShelfJava`. Users never see them, and renaming them would risk the household's data or break the build for nothing.
- **The icon:** the student's DueWise logo, a tilted hourglass with brass sand on a porcelain background (an AI draft the student edited; AI-USE.md).
  - Claude Code put the logo kit's Android icon files into the app. They are the hourglass at every screen density and a one-color version for themed icons on Android 13 and newer.
  - The start-up screen on Android 12 and newer shows the hourglass from the logo on the app's background (since 2026-10-19; before that, the round icon). Since 2026-10-22 the background is light and the hourglass has the kit's colors for light backgrounds (pine and brass); until then both were dark. Since 2026-10-23 the background is B1's page color (#F5F5F4). Older Android versions show the plain background.
  - The notification icon is still the one-color shelf, because Android shows notification icons as flat shapes. Since 2026-10-22 the shelf appears nowhere else in the app (see "Status labels and batch cards").
- **The logo at the top of Home** (since 2026-10-18, the student's choice):
  - It is the logo kit's horizontal logo: the hourglass and the name in the logo's own italic letters, as one image in place of the plain title.
  - Since 2026-10-23 it sits on Home's pine top bar (B1), in B1's light colors: light gray (#EBEBEA) lines and name, with brass (#B8914F) sand. The kit's own version for dark backgrounds has porcelain (#EEF2F0) lines, a green-tinted gray, which B1 leaves out. On 2026-10-22 it had the kit's colors for light backgrounds: pine (#1F3B34) lines and name. Until then it was the kit's version for dark backgrounds, with off-white (#EEF2F0) lines and name.
  - It is 32 dp tall and about 144 dp wide, so the top bar stays 64 dp and leaves the clear space the kit asks for.
    - On the student's phone (280 dpi), the hourglass is about 50 px tall, above the kit's 48 px limit for this version.
  - It is a vector converted from the kit's SVG, so it is sharp at every size. No shape was changed, except one letter:
    - Since 2026-10-21 (the student's request), the name's "w" is a capital "W" from the kit's own font file (Instrument Serif Italic). It is spaced like the kit's other letters, so "ise" moved right, and the logo got about 1.6 dp wider (142 dp before).
    - The logo kit's own files still say "Duewise".
  - TalkBack reads it as "DueWise". The other screens keep their plain titles.
- **The colors (since 2026-10-23, the student's choice): B1, "Brass highlights".** The student sent a mock of Home ("design option B1") with its exact colors, a pine band behind the header, and light status-bar icons. In plan mode they chose its colors for the whole app ("Whole app"), and the same swap picture on Home and Review swap. For the parts the mock doesn't show, Claude Code chose matching colors (AI-USE.md).
  - **The student's rules:**
    - Only the colors change, plus Home's pine band; the layout, the words, and the fonts stay.
    - No sage or green-tinted grays.
    - Material's dynamic color stays off, so the phone's wallpaper never replaces the colors. The app never turned it on.
    - Light only. The widget stays dark.
  - **What it looks like:**
    - A warm light gray page (#F5F5F4), white cards and tiles (#FFFFFF) with a thin border (#DCDCDA), near-black text (#1D1D1C), and gray details (#5F5F5C). Chevrons, arrows, and outlines are #858582. These grays are neutral with a hint of warmth (blue a little lower than red and green), never green.
    - **Every screen's top bar** is pine (#1F3B34) with white words and icons (since 2026-10-23, later that day; the student's choice "Pine top bar", so that the whole app looks like Home). It reaches up behind the status bar, whose icons are light, and a press on its back arrow or home icon shows a light ripple (`ReadyShelf.TopBar`). Until then only Home's bar was pine.
    - **Landscape** (since 2026-10-23, later that day): the pine top bar and the bands reach the screen's sides, the camera cutout's too, while words, buttons, and "Add batch" keep clear of it (`ScreenSetup`). Before, a light strip showed there next to the pine.
      - **A navigation bar at a side** (3-button navigation, as on the student's phone, turned sideways) is the exception, the student's choice "Light under buttons": the screen stops at it, so back, home, and recents stay on the page's light color. On pine those gray buttons measured 2.11:1. Gesture navigation and portrait are not affected.
    - **Home:**
      - A pine band (#1F3B34) behind the logo and the tiles, from the top of the screen, with rounded bottom corners (28 dp). It scrolls away with the tiles; the pine top bar stays. The status bar's icons are light on it.
      - The logo is light gray with brass sand (see above).
      - The shortcut tiles (Inventory, Purchase/Renewal list, Settings) have a pine icon in a cream circle (#F6EFE2).
    - **Main buttons** are pine with white words: filled buttons, the switch, radio buttons and check boxes, and Android's date picker. Inventory's floating "Add batch" is light brass (#CFA866) with dark words (Home's too, until it became a white tile on 2026-10-23; see "Home banner"), and its own style (`ReadyShelf.AddButton`) keeps a press from turning it pine.
    - **Chips, notes, tonal buttons, and category circles** are light gray (#EBEBEA) with dark words.
    - **The swap picture** is the same on Home and Review swap: each place has a gray icon, the arrows are lighter gray, and "for review" is dark brass (#82622A) and a little bolder.
    - **"More about this"** is in the text color with a gray arrow (it was in the main button color).
    - **Panels** (Settings, Batch details, Review swap's table) are white with the cards' border. Gray panels would have hidden the gray notes inside them.
    - **Warnings** are pale brass (#ECDDC3, B1's cream and light brass mixed 3:1) with dark words and a dark brass sign.
    - **Errors** keep their pale red box with dark red words. The red of field errors and "Date passed" is B1's #B23A2B, so the app has one red.
    - **Statuses:** see "Status labels and batch cards".
    - Sample tags and the "Sample data" strip keep their lilac (#ECBDEF), because B1 doesn't show them.
    - Save messages are a dark warm gray (#30302E) with light words.
    - Selected text is highlighted in light brass, not Material's pine. Material's tint for raised surfaces is off, so no gray turns green.
    - The start-up screen shows the pine-and-brass hourglass on the page color.
    - The home-screen widget stays dark (#303030), as before (see "Home-screen widget").
  - **Green and safety:** pine, the logo's dark green, is only on buttons, the top bars and the bands under them, and the logo, never on a status. (A status list's header is the status's own white tile on the pine band.) "Later date" stays gray, so no status looks "safe" (see "Wording rules").
  - **Contrast:** every text 4.89:1 or more (the main text 15.46:1 on the page), and every icon, arrow, and outline 3.39:1 or more (TEST-RESULTS.md 40). Inventory's floating "Add batch" against the page is 2.04:1, but its words (7.59:1) say what it is. (Home's Add batch is a white tile since 2026-10-23.)
  - **Earlier:**
    - 2026-10-22: "Graphite light". The student didn't like the dark blue look ("the blue", "too dark", "dull, all gray-blue") and asked for alternatives by best practice. Claude Code suggested Graphite, Teal, and Violet. The student chose Graphite, compared a softer dark and a light version on screenshots of the real app, and chose the light one. Neutral grays (chroma 0, from Google's Material color tools): an off-white background (#F9F9F9), light gray tiles (#E8E8E8), charcoal (#353535) buttons and "Add batch", pale status labels with dark words, and the logo in pine and brass. No green, so only the statuses had color.
    - 2026-10-18 to 2026-10-21: always dark, with a blue Material 3 scheme from the seed #1A73E8: light-blue filled buttons (#ADC7FF) on a near-black background (#121316), blue-gray notes, and muted violet sample tags. The student had asked for best practice instead of the logo's colors, and then preferred the dark version.
    - Earlier on 2026-10-18: a scheme made from the logo's pine, which the student didn't like, then the blue scheme in light and dark versions.
    - Before 2026-10-18: Material's default purple.

## Purpose
DueWise helps one household keep track of food, water, and first aid supplies at home, and since 2026-10-19 also cosmetics, documents, and subscriptions.
It records where each batch is, how many there are, and the date on the package.
It works offline, and all household data stays on the phone.

## Words used in this file
- **Product**: one kind of item, with its name, variant, package size, and unit, for example "Canned beans, Low sodium, 15 oz, counted in cans".
- **Batch**: some of one product with the same date in one place, for example "6 granola bars, best by 12 Oct 2026, in the Earthquake kit".
- **Location**: a place where batches are kept. Every location has a type.
- **Kit**: a location of type Emergency kit or First aid.
- **Label type** (date label): the kind of date printed on the package: Best by, Use by, Expires, or Other.
- **Food class**: what kind of food a product is, as the household classified it: Shelf-stable food, Perishable food, Infant formula, or Not classified. It belongs to the product, so every batch of the product has the same one. Since 2026-10-22 evening nothing asks, shows, or uses it, and each product keeps the class it has (see "Food class").
- **Date rules**: the student's `DatePolicy` class. **Swap rules**: the student's `RotationPlanner` class.

## Screens
Every screen is its own Activity. Home opens the others with large buttons. Back (or the ← arrow) returns.

| Screen | What it shows | Needs the student's rules? |
|---|---|---|
| Home | At the top, one grid of same-size tiles, two in a row (since 2026-10-19; one per row with large text; since 2026-10-23 a last tile alone in its row fills it): one status tile for each status that needs review (see "Home banner"); then the shortcut tiles Inventory, Purchase list, Settings, and Add batch, each with a pine outline icon in a cream circle (since 2026-10-23, B1; a gray icon in a white circle on 2026-10-22, and a black icon in a gray circle before). A tap on a status tile opens Inventory with only that status's batches. "Add batch" is the grid's last tile (since 2026-10-23, the student's request): a tap opens the Add batch form, and when it is alone in its row it fills the row, so the grid never has an empty cell (see "Home banner"). From 2026-10-19 until then it was a floating button at the bottom right, and the screen scrolled under it; from that evening it shrank to a round "+" while Home scrolled down (see "UI review of 2026-10-19"). Under the tiles, a short note when nothing needs review or when the date rules can't run. Since 2026-10-22 Home has no review lists ("Needs review" and "Medicine dates to review"); a status tile opens Inventory with its batches, medicine too (see "Home banner"). "Recommended swaps", each with a "Review swap" button. A getting-started message when there are no batches. A banner when the household's own records still have sample items from an earlier version. A reminder notification or the home-screen widget opens Home at the top, at the tiles (see "Reminders" and "Home-screen widget"). | The status tiles and the note: DatePolicy. Swaps: RotationPlanner. |
| Inventory | "Add batch" is a floating button at the bottom right (since 2026-10-19; Home had one too until 2026-10-23). It shrinks to a round "+" while the list scrolls down, and with large text it is always the round "+". Search by product name (the variant is searched too); the keyboard's search key closes the keyboard (since 2026-10-19 evening). Since 2026-10-19 evening (the student's choice) the filters below fold under a "Filters" button: they start closed, also after a tap on a Home tile, so the list starts near the top. While they are closed, each chosen filter shows as a chip under the search box ("Near date ✕", "Earthquake kit ✕", "Latest first ✕", "Archived batches shown ✕"); a tap on a chip or its ✕ sets that filter back, and with two or more, a "Clear all" chip sets them all back. Since 2026-10-23 a list that a status tile on Home opened shows that status as a header (its tile) instead of a chip, and "Clear all" keeps it (see "Home banner"). While they are open, those chips are hidden. Filters for location and category, as chips since 2026-10-18 (one tap; "All locations" or "All categories" first; the chips wrap onto more lines, so every choice is in view once the filters are open). A Status filter, also chips (since 2026-10-18): "All statuses", then "Date passed", "Date is today", "Near date", "Date needs review", and "Later date". It is hidden when the date rules are missing or stopped, and archived batches (which have no status) show only with "All statuses". A tile on Home opens Inventory with its status chosen; its top bar then has only a home icon, which goes back to Home, instead of the back arrow and "Inventory" (since 2026-10-22; see "Home banner"). Date order: "Soonest first" or "Latest first", a two-button switch since 2026-10-18 (with large text the two buttons go under each other, the same rule as the swap picture); batches without a complete date come last. "Show archived batches" switch. Each batch is a card (since 2026-10-18) with its category icon, the product, amount with unit, location, date, and "Date needs review" when it applies; its status (from DatePolicy, since 2026-10-18) is a label with an icon and a color (see "Status labels and batch cards"). | Status labels only |
| Add or edit batch | Since 2026-10-19 (see "The batch form and its categories"): first "What kind of item?" (the category, as chips with icons), then the product (one box that finds the household's products, or "New product"), location and quantity, and the date. Until 2026-10-23 "Take label photos" came between the category and the product when adding (one button since 2026-10-22); the student hid it then (see "Photo entry"). Choices are one-tap chips. Since 2026-10-22 the form is shorter (see "Simpler form (2026-10-22)"): the label type is one line with "Change", "What is printed?" is folded until needed, and a new product has only a name and a unit. Since that evening it has no food section (until then, for Food, the product's food class and the food condition; see "No food section, and swaps for more categories (2026-10-22, evening)"). "Review and save" shows a review; nothing is saved before "Confirm and save" or, when adding, "Save and add another". Since 2026-10-19 evening "Review and save", and then "Confirm and save", are in a bar at the bottom that stays in view while the form scrolls; "Save and add another" and "Back to the form" are at the end of the review. Back (or ←) on a form with something the household entered asks "Leave without saving?" first (see "The batch form and its categories"). Until 2026-10-23, when adding, "Take label photos" opened photo entry, which can fill in parts of the form (see "Photo entry"). Also opens as "Record bought item" from the purchase list (see "Purchase list"). | No |
| Batch details | Name, variant, package size, unit, quantity, location and its type, category, date and "Date needs review", status, and history (newest first). Buttons: Edit batch, Record used, Record discarded, Move, Archive. In cards since 2026-10-19 (the student's choice): a header card with the category icon, name, product, tags, and the status label; a card of facts (quantity, location, category, date), each with an icon that only helps; until 2026-10-22 evening, for food, a "Food condition" card with one label-and-answer row each (the student removed it then); "Record used" as the main (filled) button, then "Edit batch" with "Move" (tonal) and "Record discarded" with "Archive" (outlined), two to a row (under each other with large text); the history as a timeline of dots and a line. The archived and 0-left rules for the buttons are unchanged. | Status line only |
| Change stock | Record used, Record discarded, Move, or Archive for one batch: first how much (and where to), then a review, then Confirm (see "Stock changes"). Since 2026-10-19 evening "Review", and then "Confirm", are in a bar at the bottom that stays in view; "Back" is at the end of the review. The screen shows nothing until the batch has loaded. | No |
| Review swap | One proposed swap in full: both moves, why the replacement qualifies, what stays in each place, and "Confirm swap" (see "Swaps"). Since 2026-10-19 evening "Confirm swap" is in a bar at the bottom that stays in view while the screen scrolls; the bar shows only while the swap can be confirmed. | Yes: RotationPlanner |
| Purchase/Renewal list ("Purchase list" until 2026-10-24) | "On your purchase list" (items the household plans to buy), "Replacement needs" (kit items the household buys again that the swaps don't cover, only kit food until 2026-10-22 evening, and expired items the household buys again, each with "Add to purchase list"), and since 2026-10-24 "Renewal" (documents and subscriptions dated today or earlier, each with "Renewed" and "Discard"). The "Suggestions" section was removed on 2026-10-22 (see "Purchase/Renewal list"). | Needs: RotationPlanner, plus the app's check for expired items. Renewal: DatePolicy. The stored list: no |
| Settings | Reminders: the "Daily reminder" switch, whether notifications are allowed, "Open notification settings", and "Show reminder now" ("Send a test reminder" until 2026-10-23). Planning settings: near-date days (starts at 30) and replacement days (starts at 180, and more than the near-date days). See "Planning settings" and "Reminders". Until 2026-10-22 a third card, Sample data, had "Open sample data", or while it was open "Close sample data", "Start sample data again", and "Keep these items as my own", and "Remove old sample items" when the household's records had some; the student removed that card on 2026-10-22 (see "Sample data"). In cards since 2026-10-19 (the student's choice): each a quiet card with an icon and a title, and one main button ("Show reminder now" tonal, "Send a test reminder" until 2026-10-23; "Save planning settings" filled). "Open notification settings" is a quieter text button; whether notifications are allowed is in a small box. Every safety sentence stays in view. Since 2026-10-19, later that day (the student asked for UX improvements by best practice; Claude Code chose them): Reminders comes first, with the switch right under its title, because Android's settings guidelines put the settings used most at the top; then Planning settings (and Sample data until 2026-10-22). Since that evening (the student found the page a wall of text and chose "Short cards"), each card shows only one or two short lines and its buttons, and the longer reminder texts are under "More about this" at the end of the Reminders card (about 94 words in view with sample data closed, down from about 210). Since 2026-10-21 its texts are shorter still (see "Short texts of 2026-10-21"). | The "Daily reminder" switch only: DatePolicy |

While the separate sample data is open, every screen shows the label "Sample data: made-up items, apart from your records." right under its title, with a "Close sample data" button under its words since 2026-10-19 (see "Sample data"). Since 2026-10-22 sample data can't be opened, so the label shows only where sample data was still open when that version was installed.

## The batch form and its categories (2026-10-19)
The student asked for three more categories and a friendlier batch form, then chose these from Claude Code's suggestions (AI-USE.md).
- **Categories:** Food, Water, Medicine, First aid supplies, Cosmetics, Documents, Subscriptions, and Other. Cosmetics, Documents, and Subscriptions are new.
  - The database stores each category's name, so the new ones needed no database change (still version 3). An older DueWise build can't read a batch with a new category, so don't install an older build after saving one.
  - They are not food. Until 2026-10-22 evening the swap rules and the food questions were Food only. Since 2026-10-22 expired cosmetics are listed under the Purchase list's "Replacement needs"; documents and subscriptions are not, because they are renewed, not bought. Since 2026-10-24 documents and subscriptions dated today or earlier are listed under the Purchase/Renewal list's "Renewal" instead. Since that evening cosmetics can be swapped like food, and documents and subscriptions never are (see "No food section, and swaps for more categories (2026-10-22, evening)"). They count for Home's status tiles, the daily reminder, and the widget like any other batch.
  - Their icons: a lotus (Cosmetics), a page (Documents), and two circling arrows (Subscriptions).
- **"What kind of item?" comes first:** one chip per category, with its icon. The chosen chip shows a check mark in place of its icon. Since 2026-10-22 the chips' icons are gray. Material's filter chip doesn't color its icon, so before that the icons kept their files' white, which almost vanished on the light chips (found while checking the new icons). The category decides what else the form asks: no quantity or unit for a new document or subscription (and until 2026-10-22 evening the food section, only for Food).
- **Starting choices.** Each is only a starting point: the household can change it, and the review shows it.
  - The label type starts at "Expires" for Medicine, First aid supplies, Cosmetics, Documents, and Subscriptions, and at "Best by" for Food, Water, and Other. It follows the category until the household chooses one, or a label photo or the batch being edited gives one.
  - If the label type follows a new category while the date's box is ticked, the tick is removed, because the household confirmed the date with the other label type.
  - A new batch starts at quantity 1.
- **Documents and subscriptions** (the student's choice): a new one is one item.
  - The form asks for no quantity and no unit, and says "Saved as 1 document." (or "1 subscription").
  - A new product gets the unit "document" or "subscription".
  - It still needs a location. The household adds one once with "New location", for example "Filing cabinet" or "Online".
  - Editing and "Record bought item" always ask for the quantity.
- **One-tap choices:** the location, the label type, and "What is printed?" are chips (and until 2026-10-22 evening, for Food, the food class). The quantity has − and + buttons (0 to 9999, or 1 to 9999 for a bought item), and it can still be typed. Since 2026-10-22 the label type chips and "What is printed?" are folded until needed (see "Simpler form (2026-10-22)"); until then the food type and each condition answer were chips too, and that day the food condition was one tick, until the food section went that evening.
- **The date:** one button shows the chosen date (or "Choose date") and opens the date picker. The confirmation "I checked the date and label type on the package." stays (the student's choice), right under the date.
- **"Save and add another"** (only when adding a new batch): on the review, after "Confirm and save". It saves the same way, then opens an empty form with the same category and location, which shows "Batch added.". A label photo's category clue never replaces that category, except Medicine (the same rule as a category chosen by hand).
- Nothing is saved before the review, as before.
- **"Leave without saving?"** (since 2026-10-19 evening; part of the "small fixes" the student chose): until then Back (or ←) on the form threw away whatever was entered, without asking. Now Back asks first, with the same question and buttons as Settings: "Leave without saving?", "What you entered won't be saved.", [Keep editing] [Leave].
  - It asks only when leaving would lose something the household entered: a typed name, unit, or quantity that differs from how the form started (until 2026-10-22 also the variant and package size), or a choice made by hand (the product, the date, the date's tick, "Use the last day of the month", any other choice chip, or a label photo's result; on 2026-10-22, until the food section went, also the food condition tick). On an edit, a new category counts too. Opening "Change" or "No full date on the package?" doesn't count, because it changes no answer.
  - It doesn't ask for what the form filled in by itself (the starting quantity, the batch being edited, the item being bought, or the category and location that "Save and add another" carries over), or for a category or location tapped alone on a new batch: those are one tap to choose again. So DEMO.md step 3 (tap Documents, then the back arrow) still closes at once.
  - On the review, Back still returns to the form.
  - `BatchEditViewModel.hasUnsavedInput` decides; `BatchEditFormTest` tests it.
- **An edit with no change** (since 2026-10-19 evening): the review says "Nothing was changed.", and "Confirm and save" now closes the form with "No changes to save." (until then the message was "Changes saved.", although nothing was saved).

### Simpler form (2026-10-22)
**Its food parts were replaced the same evening:** the form has no food section any more (see "No food section, and swaps for more categories (2026-10-22, evening)"). This subsection describes the form as it was that afternoon.

The student found Add batch "very complex" and asked for ways to simplify it, with permission to remove fields that are not mandatory. Claude Code counted the controls (53 for a food item, 25 of them in the food section) and traced every field to the code that reads it. The student then chose four changes from Claude Code's suggestions (AI-USE.md). A food item now has 29 controls, counted the same way (31 on the emulator, which has a fourth location and counts the product box's arrow; TEST-RESULTS.md 37).
- **Food section: two questions removed, three answers as one tick.**
  - The food type and the batch's own "Shelf-stable? (this batch only)" answer are no longer asked. No rule read them: only Batch details and the history showed them. The swap rules use the product's food class instead.
  - "Sealed?", "Damaged package?", and "Storage concern? (heat, moisture, or pests)" became one tick: "Sealed, not damaged, and no storage concern (heat, moisture, or pests)". Ticked, the answers are Sealed Yes, Damaged No, and Storage concern No, which is exactly what swap proposals need. Unticked, they are "Not checked". It starts unticked: the form never answers for the household.
  - The food class chips, "Your own records: DueWise doesn't check or judge food." (always in view), and "More about this" are unchanged.
  - The review of a new food batch says the condition in one line: "Food condition: Sealed, not damaged, no storage concern" or "Food condition: Not checked. Swap proposals need it checked."
  - **Older batches keep their answers.** The database is unchanged (still version 3): an edit saves the food type and the "Shelf-stable" answer as they were. A batch whose answers are not the tick's, for example "Sealed: No", shows the tick unticked with "Saved answers: Sealed: No, Damaged: Not checked, Storage concern: Not checked." under it. Those answers stay until the tick changes; unticking after a tick brings them back. An edit's review and history still say each changed answer in its own sentence ("Sealed changed from No to Yes.").
  - Batch details shows the food type and "Shelf-stable" rows only when an older batch has them.
- **The label type is one line:** "Label type: Best by", with a "Change" button that shows the four chips in its place (TalkBack: "Change label type"). It still follows the category until the household chooses one, and a label photo still sets it. The tick under the date still names the label type, which is in view right above it.
- **"What is printed?" folds:** the date part starts with the label line, "Choose date", the tick, and "No full date on the package?" ("No full date?" for documents and subscriptions). That button opens "Full date", "Part of a date", and "No date" above the date button, and then goes away. "What is printed?" also opens by itself, and stays open on that form, when the date isn't a full date: a label photo's month and year, an edited batch with part of a date or no date, or "Review and save" without a date (so "Choose a date, or another answer above." points at something in view). A month-and-year date, common on medicine, takes one more tap than before.
- **A new product has only a name and a unit.** "More details (optional)" with the variant and the package size is gone. Products that already have them keep them, and the product list still shows them.
- **Label photos are one button:** "Take label photos" under the categories, without the "Label photos (optional)" heading and its intro line.
- **Unchanged on purpose:** the date tick (the student's choice on 2026-10-19; without it every date stays "Date needs review"), the review before saving, the database, and the student's DatePolicy, RotationPlanner, and DateCandidateParser with their tests.

### No food section, and swaps for more categories (2026-10-22, evening)
The student still found the form heavy and sent a screenshot of its "Food condition" part (the note "Your own records: DueWise doesn't check or judge food.", its "More about this", the food class chips, and the tick): "Remove the section in the screenshot. Also, if you are considering only sealed items for replacements, drop that logic and consider everything as a candidate for replacement. I want to keep things simple." Claude Code asked three questions. The student chose "Food condition only (Recommended)", "All but medicine, documents, subs", and "Remove the card (Recommended)", and approved the plan with its changed rule tests (AI-USE.md).
- **The form has no food section:** no food class, food type, condition tick, or food note. The date part is unchanged: the label line with "Change", the date, the date's tick, "Date needs review", and "No full date on the package?". A food item has 23 controls, counted like the 53 and the 29 before (25 on the emulator, which has a fourth location and counts the product box's arrow; TEST-RESULTS.md 38).
- **Batch details has no "Food condition" card.** The review and the history of a new batch or an edit say nothing about food.
- **Swaps: everything but medicine, documents, and subscriptions.**
  - Food, water, first aid supplies, cosmetics, and Other can be swapped. Medicine, documents, and subscriptions never are, as the kit batch or as the replacement. A category added later stays out until it is named (RP10).
  - The sealed, damaged, and storage-concern checks are gone, and so is the food class rule (Shelf-stable food only; never infant formula, perishable, or unclassified food). Only the date rules decide: a complete, confirmed date with Best by, Use by, or Expires on both batches; a kit batch 1 to the near-date days away; and a pantry batch of the same product and unit, dated later and at least the replacement days from today.
  - The two batches' categories don't have to match: the swap rules compare the product and the unit.
  - Review swap says "“For review” means you check this item yourself. DueWise doesn't check either item." "Why the replacement qualifies" says "Same product, with a later date at least your replacement days away.", and its rules end with "Medicine, documents, and subscriptions are never swapped." "What you recorded" lists the product and each batch's date, place, and "Date and label type confirmed.", with no food class or answers.
  - Home's "More about this" above the swap cards: "Never swapped: medicine, documents, subscriptions, and kit items dated today or earlier. Kit items to buy again are on the Purchase/Renewal list." ("Purchase list" until 2026-10-24.)
- **Replacement needs widen with the swaps:** a kit batch of those categories near its date that no swap covers is a need, as kit food was before. Medicine, documents, and subscriptions never get one.
- **Older records** (the database is unchanged, still version 3):
  - Food classes, food types, and condition answers saved before stay in the database. No screen shows them, and the swap rules don't use them.
  - An edit keeps them, also when the category changes (until then a change from Food to another category removed them, and the history said so). New batches and products get none.
  - So products marked "Not classified" and batches marked "Not checked", which were never swapped, can be now, and new swap cards may appear. Old answers such as "Damaged: Yes" or "Sealed: No", and the classes Perishable food and Infant formula, no longer keep a batch out of swaps. Near kit water, first aid supplies, cosmetics, and Other items that no swap covers are now Purchase list needs.
  - History entries saved before keep their words (for example "Sealed changed from No to Yes.").
- **Removed with it:** the check "Not saved: the food class changed. Check it and try again.", and the history sentences about the food type, the condition, and the food class.
- **Unchanged on purpose:** the database, DatePolicy, the date part of the form, medicine never swapped and never a need (CLAUDE.md rule 8), and the public methods of RotationPlanner, DatePolicy, and DateCandidateParser and their contracts.

## Products (added 2026-10-11)
- The batch form has one product box (since 2026-10-19; before that, it asked "Existing product" or "New product" first).
- **One of the household's products:** the box is a list that can be searched by typing. Each row shows the name, then the variant, package size, and unit ("no variant" or "no package size" when they are empty), so they are visible before choosing. After a choice, the form shows "Variant: … Package size: … Unit: …".
- **New product:** "New product" under the box, or "Add “Rice” as a new product" while the typed words are not one of the household's products. The typed words become the new product's name.
  - It has a name and a unit (required, for example "cans"; the category's usual units are one-tap chips under the field, and any other unit can be typed).
  - Since 2026-10-22 (the student's choice) it has no variant or package size: "More details (optional)" with those two fields was removed (see "Simpler form (2026-10-22)"). A product that has them from before keeps them, and the list and Batch details still show them.
  - "Choose one of your products" goes back to the box.
- When the household has no products of its own (for example while sample data is open), the form starts with a new product.
- The same name, variant, package size, and unit can't be added twice; capital letters don't matter. On the form, a new product is the same as one of the household's products with that name and unit and no variant or package size. The form says "You already have this product. Choose it from your products."
- Sample products are not offered in the list.
- The quantity field shows the unit, for example "Quantity (cans)". Quantities are shown with their unit everywhere. A product from version 1 with no unit shows "6 (no unit entered)".
- The category is chosen on each batch. It is not filled in from the product.
- Changing a product's own name, variant, size, or unit is not part of this step. Edit can move a batch to another (or a new) product.

## Date entry (added 2026-10-11)
- "What is printed?" has three answers. Since 2026-10-19 they are chips with short words: "Full date", "Part of a date", and "No date". Since 2026-10-22 a new form starts at "Full date" with the three chips folded under "No full date on the package?" ("No full date?" for documents and subscriptions). They open by themselves when the date isn't a full date (a label photo's month and year, or an edited batch with part of a date or no date) and when "Review and save" finds no date, and then stay open on that form (see "Simpler form (2026-10-22)").
- **The label type** (Best by, Use by, Expires, or Other date) shows as one line, "Label type: Best by", with "Change" since 2026-10-22; "Change" shows the four chips in its place.
  - **A complete date (day, month, and year):** chosen with the date picker. Past dates are allowed, so old stock can be recorded.
  - **Only part of a date, or it is hard to read:** the household types what the label shows (for example "03/2027"). It is saved exactly as typed. No day is made up.
  - **No date printed.**
- **"Use the last day of the month"** (added 2026-10-11; the student chose this helper):
  - It shows under the typed text only when the text is exactly one month and year, for example "03/2027", "2027-03", or "Mar 2027". The button names the date: "Use the last day of the month (31 Mar 2027)".
  - It is not offered for anything else, for example a full date, "10/26", or two months, or for years the date picker doesn't show (before 1900 or after 2100).
  - Tapping it switches the form to that complete date, unconfirmed. Under the date: "Last day of “03/2027”. The package may mean another day, so check it."
  - The typed text is kept on the form, so choosing "Part of a date" again brings it back.
  - The review and the history add "The last day of the month was chosen for “03/2027”." Only the complete date is saved with the batch.
  - Nothing changes until the household taps the button. The date shows "Date needs review" until they tick the confirmation.
- **Confirmation:** "I checked the date and label type on the package." It starts unticked, and it can only be ticked for a complete date. Changing the date or the label type unticks it. Since 2026-10-19 it sits right under the date button (see "The batch form and its categories").
- **"Date needs review"** shows (with the reason) when the date is missing, incomplete, or not confirmed. This is not a DatePolicy status: it never compares dates with today.
- Dates are shown as "26 Oct 2026" (day, month word, year), so they can't be read two ways. History entries saved before 2026-10-11 keep the style they were saved in.

## Food condition (added 2026-10-11)
- **Since 2026-10-22 evening (the student's choice) the form doesn't ask it, no screen shows it, and the swap rules don't use it.** Answers saved before stay in the database, and an edit keeps them (see "No food section, and swaps for more categories (2026-10-22, evening)"). The rest of this section describes it until then.
- Shown only for the category Food.
- Food type: Not chosen, Canned, Dry or boxed, Jar or bottle, Pouch, Frozen, Refrigerated, or Other. Since 2026-10-22 the form no longer asks it (no rule read it); a batch that has one keeps it, and Batch details shows it.
- Four questions until 2026-10-22: Sealed? Shelf-stable? Damaged package? Storage concern? Each answer is Yes, No, or Not checked. Every new batch starts with "Not checked"; the form never answers for the household.
- **Since 2026-10-22 (the student's choice) the form asks them as one tick:** "Sealed, not damaged, and no storage concern (heat, moisture, or pests)". Ticked, the answers are Sealed Yes, Damaged No, and Storage concern No; unticked, "Not checked". The batch's own "Shelf-stable?" is no longer asked. An older batch keeps every answer it has: when its answers are not the tick's, the form shows them under the tick ("Saved answers: Sealed: No, …") and keeps them until the tick changes (see "Simpler form (2026-10-22)").
- The form explains (updated 2026-10-11 for the guide's swap rules): swap proposals skip the batch until sealed, damaged, and storage concern are answered, and they also skip a batch that is not sealed, is damaged, or has a storage concern, and a product whose food class is not "Shelf-stable food". Until RotationPlanner was connected on 2026-10-18, the form also said "Automatic swaps are not turned on yet." Since 2026-10-19 evening the app's texts say "swap proposals" instead of "automatic swaps" (for example "Swap proposals need…", "Swaps are proposed only for food."), because nothing moves until the household taps "Confirm swap". The meaning is unchanged. The batch's own "Shelf-stable?" answer is kept as a note for that batch only; the swap rules use the product's food class instead. The answers are records only: DueWise does not check or judge the food. Since 2026-10-18 the note "Your own records: DueWise doesn't check or judge food." (in these words since 2026-10-21) is always in view, and the swap rules fold under "More about this" (see "Messages and More about this").
- Until 2026-10-22 evening, changing a batch from Food to another category removed its food answers (the history said so). Since then they are kept.

## Food class (added 2026-10-11)
- **Since 2026-10-22 evening nothing asks, shows, or uses it.** Each product keeps the class it has (no database change), and a new product gets none. The app still passes it to the swap rules (the contract is unchanged), and they ignore it. The rest of this section describes it until then.
- Stored with each product (database version 3). The household chooses it on the batch form, in the food section (Food category only): Not classified, Shelf-stable food, Perishable food, or Infant formula. The label says it applies to every batch of the product.
- It starts at the product's saved class ("Not classified" for a new product and for every product from before version 3). The form never chooses it, and a label scan never fills it in.
- A change shows on the review and in the history: "Food class of the product changed from Not classified to Shelf-stable food. This applies to every batch of this product." A new product with a class gets "Food class of the new product: …". A change of only the food class is saved as an edit.
- If the product's class changed after the review was shown, nothing is saved ("Not saved: the food class changed. Check it and try again.").
- Batch details shows "Food class of the product: …" for food batches.
- The app passes each product's food class to the swap rules (before version 3 it passed "unclassified" for every product). It is the household's own classification: DueWise does not check the food.

## Swaps (added 2026-10-11)
Swaps come from the student's RotationPlanner. Nothing moves until the household reviews one swap and confirms it.
- **What can be swapped** (since 2026-10-22 evening, the student's choice): food, water, first aid supplies, cosmetics, and Other; never medicine, documents, or subscriptions. Until then only shelf-stable food recorded as sealed, not damaged, and with no storage concern (see "No food section, and swaps for more categories (2026-10-22, evening)").
- **Home card:** the product (name, variant, package size, unit), then the swap picture (since 2026-10-18), and a "Review swap" button. The card never says that an item is safe or that it must be used.
  - **The swap picture:** two moves, each drawn as two places with an arrow between them. A place shows its type's icon and its name: a backpack for an emergency kit, a first aid kit, shelves for the pantry, and a map pin for other places. Until 2026-10-22 the pantry had the reminder's shelf and other places had Inventory's box.
    - "[Earthquake kit] → [Pantry]", with "3 cans · Best by: 20 Oct 2026 · for review" under it.
    - "[Pantry] → [Earthquake kit]", with "3 cans · Best by: 1 Jun 2027".
  - TalkBack reads the two full sentences instead: "Move to pantry for review: 3 cans (Best by: 20 Oct 2026) from Earthquake kit to Pantry." and "Into Earthquake kit in its place: 3 cans (Best by: 1 Jun 2027) from Pantry."
  - When the text is large for the screen (narrower than 300 dp per unit of font scale), each move's two places go under each other with a ↓ arrow, so a place name is never split inside a word.
- **Review swap screen:**
  - The two moves: the amount that moves each way, then the same swap picture as on Home.
  - "Why the replacement qualifies": a one-line summary, then "Replacement days are a planning choice, not a safety guarantee.", which stays in view. Under "More about this" (since 2026-10-18; since 2026-10-19 with what the household recorded too):
    - the swap rules' requirements as listed in "RotationPlanner requirements", with the Settings numbers
    - "What you recorded": today's date, the product, and each batch's date, location, and date confirmation (until 2026-10-22 evening also the food class and the condition answers). The app doesn't count days or compare dates for this text.
  - "After the swap": a small table of the product's total in each place, before and after (since 2026-10-18; TalkBack reads it as one sentence per place, for example "Granola bars in Earthquake kit: 6 bars before, 6 bars after."). Under "More about this" (since 2026-10-19): for each place, what stays, what leaves (the kit batch leaves "for review"), what arrives, and the product's total there before and after.
  - The note "“For review” means you check this item yourself. DueWise doesn't check either item." (since 2026-10-22 evening; until then "“For review” means you check this food yourself. DueWise doesn't check it.")
  - "Confirm swap" is the explicit confirmation. Nothing changes before it.
- **The screen shows one frozen swap.** It finds the swap from Home in a fresh plan (the same two batches and the same amount) and keeps it with every text it shows. If a later plan no longer contains it, the screen says "This swap changed. Go back to see the current proposals." and hides "Confirm swap".
- **Confirm swap checks and saves everything in one database transaction** (`SwapStore`):
  1. If this screen's action ID already has a receipt, nothing changes (a repeated tap, or a tap after Android closed and reopened the app).
  2. Both batches are read again. If either differs in any way from what the screen showed (quantity, date, printed text, label type, confirmation, product or its food class, location, category, food type, condition answers, archived, sample), nothing is saved: "Nothing moved: a batch changed. Go back to see the current proposals."
  3. The swap rules run again with the phone's date and the current Settings numbers. If they no longer propose exactly this swap (for example after midnight or a Settings change, even when no batch changed), nothing is saved: "Nothing moved: this swap is no longer proposed…". A swap that is still proposed is saved.
  4. Both movements: a whole batch changes its location; part of a batch becomes a new batch at the other place with the same product, date, label type, confirmation, food answers, and sample mark, so every part keeps its original date.
  5. A "Swapped" history entry for every batch involved, and the receipt (`swap_actions`).
  - If anything fails, the transaction is rolled back: neither movement is saved ("Nothing moved: the swap couldn't be saved. Try again."). Trying again uses the same action ID, so the swap can still be saved only once.
- **The action ID** is a random ID made once by the Review swap screen and kept through rotation and Android closing the app. "Confirm swap" is turned off while it saves.
- **Nothing is reserved:** proposals are never stored. Home and the Purchase/Renewal list make them again from the current stock after every change, including a confirmed swap.
- A kit batch dated today or earlier is never part of a swap; its replacement need is on the Purchase/Renewal list (none for medicine, documents, or subscriptions; documents and subscriptions are under its "Renewal" instead).

## Purchase/Renewal list (added 2026-10-11 as "Purchase list"; renamed on 2026-10-24)
- **Since 2026-10-24 it is called "Purchase/Renewal list"** (the student's name), on Home's tile and in the screen's title, and it has a third part at the end, "Renewal" (see below). "On your purchase list" keeps its name: it is still the list of things to buy. The intro says "What to replace or renew, and what you plan to buy."
- **Since 2026-10-22 there is no "Suggestions" section** (the student's choice). The student asked for it to be removed, and for expired things to show under "Replacement needs" unless a swap is available. Claude Code asked two questions, and the student chose "Everything you'd buy again" and "Add button on each need". The screen had two parts then: "On your purchase list" and "Replacement needs".
- **Replacement needs:** one card per batch, soonest date first, each with its reason, for example "Still needs replacement: 2 cans. Pantry batches cover only part of it."
  - Kit batches of food, water, first aid supplies, cosmetics, or Other that the swaps don't cover, from the swap rules' notes (only kit food until 2026-10-22 evening). A kit batch that a swap covers gets no need: "unless a swap is available".
  - Expired items (an app check, not one of the swap rules): every batch of food, water, first aid supplies, cosmetics, or Other, in any place, with a complete date of today or earlier needs replacement for its whole quantity. Its reason is "The date is today or has passed."; a batch in a kit gets the longer "The date is today or has passed. No pantry use is recommended." Such a batch is never part of a swap. Until 2026-10-22 only kit food got this need.
  - Medicine never gets a need, because DueWise gives no advice about replacing medicine. Documents and subscriptions don't either, because they are renewed, not bought; since 2026-10-24 they are under "Renewal" (see below).
  - **"Add to purchase list"** on a card adds the batch's product for its place, with the needs of that product and place added up (for example, two kit batches that need 2 and 3 cans give 5). It needs confirmation in a dialog: "3 cans of … for Earthquake kit. Only a note: your stock doesn't change."
  - A card whose product and place are already on the list says "On your purchase list." instead of the button. A sample batch says "Sample items can't go on the purchase list." and has no button.
  - The amounts are made again whenever stock changes; nothing about them is stored.
- **Renewal** (since 2026-10-24). The student asked: "When a document expires, it should appear under Renewal, which should be a subsection inside "Purchase list"… Inside the renewal section, there should be an option to select that the item has been renewed and the new date or discard - when it should disappear from the list." Claude Code asked four questions, and the student chose "Documents and subscriptions (Recommended)", "Today or earlier (Recommended)", "Discard and archive (Recommended)", and "Same record, new date (Recommended)".
  - **What is listed** (`RenewalList`): one card for each document or subscription with something left (quantity 1 or more) whose status from the date rules is "Date is today" or "Date passed", soonest date first. A date that needs review, "Near date", a later date, an archived batch, and every other category are not listed. So these are the documents and subscriptions that Home's "Date is today" and "Date passed" tiles count, except batches with nothing left.
  - Each card is the usual batch card (category icon, amount and place, date, and status label) with two buttons: **Renewed** and, under it, **Discard**. A tap elsewhere on the card opens Batch details.
  - **Renewed** opens Android's date picker at tomorrow; earlier days can't be picked, because a renewed date is after today. After a date is picked, a dialog asks "Save the new date?", for example "Passport in Documents Folder: from Expires: 23 Oct 2026 to Expires: 23 Oct 2036." **Save** gives the same batch the new date, confirmed, with its label type, and adds one history entry (Edited): "Renewed. Date changed from Expires: 23 Oct 2026 to Expires: 23 Oct 2036." Its quantity, place, and earlier history stay. It leaves Renewal and Home's tiles, unless the new date is within the near-date days (then it is "Near date").
  - **Discard** asks "Discard and archive?" ("Passport in Documents Folder. Its history says it was discarded, and Inventory hides it unless “Show archived batches” is on."). **Discard** records the whole quantity as discarded and archives the batch, in one transaction, with two history entries: "Recorded as discarded: …" and "Archived, because it was discarded on the Purchase/Renewal list." It leaves Renewal, Home's tiles, the reminder, and the widget.
  - A saved change shows a short message at the bottom: "Renewed: Passport, Expires: 23 Oct 2036." or "Discarded and archived: Passport." Nothing is saved when the batch changed after the screen showed it (another quantity or date, archived, or gone): "Not changed: this item changed or was removed." A database error says "Not changed: the phone's database reported an error. Try again." Both stay at the top of the screen.
  - The history uses the types that already existed (Edited, Recorded as discarded, Archived), so the database didn't change, and older builds can still read it.
  - An empty section says "Nothing to renew right now." Without the date rules it would say "Not available yet: the date rules are not finished."
  - Documents and subscriptions are still never swapped and never get a replacement need, and medicine is never listed on this screen.
- **On your purchase list:** each item shows "To buy: 3 cans for Earthquake kit" (or another place, such as "for Pantry", since 2026-10-22).
  - The list can hold only one item for each product and place (a unique key in the database). A second add says "Already on your list for this place."
  - While the needs can be made, it also shows "Needed now: …" (what the needs add up to now) or "Not needed right now.". Until 2026-10-22 it said "Suggested now: …".
  - When the amount differs, "Change to …" changes it after a confirmation.
  - "Remove from list" also asks first.
  - None of these change stock, and an item is never counted as stock anywhere.
  - An empty list says "Empty. Add items from Replacement needs below."
- **Record as bought** opens the batch form as "Record bought item":
  - The item's product is filled in and can't be changed, and the amount is filled in (1 to 9999). The household chooses where they put it.
  - "No date" is turned off: a bought item needs its printed date, either a complete date or the printed text of an incomplete one.
  - The review says which list item is removed. The history adds "Bought from the purchase list.".
  - "Confirm and save" adds the new batch and removes the item in one transaction. If the item is already gone (for example after a second tap), nothing is saved: "Not saved: this item is no longer on your purchase list…".
  - A purchase always becomes a new batch; it never adds to an existing batch.
- Purchase-list items stay on the phone with the other data. Until 2026-10-22, "Remove old sample items" also removed items of sample products (there couldn't be any, because sample needs can't be added).
- **Before 2026-10-22:** a "Suggestions" section added up the needs for each product and kit ("Suggested: 4 cans for Earthquake kit", "From: 4 cans (Best by: …)") and had the only "Add to purchase list" button. Replacement needs listed only kit food, without a button, and the list accepted only kits.

## Stock changes (added 2026-10-11)
- From Batch details: **Record used**, **Record discarded**, **Move**, and **Archive**.
- **Step 1 (not for Archive):** "All (4 cans)" or "Some". "Some" needs a whole number from 1 to the batch's quantity. Move also needs a location other than the batch's own.
- **Step 2, the review:** the batch's amount before and after. For a move, the product's total at the new location before and after.
- **Kits:** when stock leaves a kit (Emergency kit or First aid type), the review shows the product's total in that kit before and after, and says: "Replacement may be needed. DueWise doesn't check kit supply targets." The same warning shows when a count correction on the Edit form lowers a kit's amount.
- **Medicine:** Record used and Record discarded say "This only records your stock. It isn't medicine advice."
- **Confirm** saves the change and its history entry in one database transaction. If the batch changed after the review was shown, nothing is saved and the screen says so.
- **Move all** changes the batch's location. **Move some** lowers the batch and adds a new batch with the same product, date, label type, confirmation, and food answers at the new location. Both batches get a "Moved" history entry.
- **Archive** keeps the batch and its history. Inventory hides it unless "Show archived batches" is on, and the rules never see it. An archived batch can still be edited. Un-archiving is not part of this step.
- A batch with 0 left can be edited or archived; Record used, Record discarded, and Move are turned off for it.
- **Edit:** the location changes only with Move. A quantity change on the Edit form is a count correction ("Count corrected from 6 bars to 5 bars.").

## Planning settings (added 2026-10-11)
- Settings → "Planning settings" (called "Date thresholds" before 2026-10-11):
  - **Near-date days:** a whole number from 1 to 365. It starts at 30.
  - **Replacement days:** a whole number from 1 to 3650. It starts at 180. It must be more than the near-date days.
- The screen calls both numbers planning choices, not safety guarantees.
- "Save planning settings" checks both numbers. If the replacement days are not more than the near-date days, the error shows on the replacement field: "Enter more than the near-date days (180)." (until 2026-10-19 evening it went on: ", or lower the near-date days.") Nothing is saved until both numbers are fine.
- **Typing and leaving** (since 2026-10-19, later that day; Claude Code's choices after the student asked for UX improvements by best practice):
  - **The keyboard:**
    - Its key says "Next" on the near-date days and "Done" on the replacement days.
    - "Done" saves, the same as the button.
    - After a save, the keyboard closes, so "Planning settings saved." is in view.
  - **Errors** appear only on a save:
    - The first field with an error gets the focus.
    - While the household types, an error can only go away: when the number is fixed, or when the error no longer fits it (for example "(200)" after the near-date days changed). The next save shows the error that fits.
    - No new error appears halfway through typing.
  - **Leaving without saving:**
    - While a number differs from the saved one (or isn't a whole number), Back (the ← arrow, the phone's Back, or the back gesture) asks "Leave without saving?" with "Your planning changes aren't saved." "Keep editing" stays; "Leave" closes Settings and saves nothing.
    - With nothing changed, Back closes Settings at once.
    - The question doesn't come back by itself after a rotation; the next Back asks again, and the typed numbers are kept.
    - "Close sample data" (under the "Sample data" label of any screen) closes every screen without this question, after its own short question. Until 2026-10-22 so did Settings' "Open sample data", "Close sample data", and "Start sample data again".
- A saved pair that breaks the rule (possible before 2026-10-11, for example 200 and 180) is read as 30 and 180, so the app still starts. The file keeps the old numbers until the household saves new ones.
- The date rules, the swap rules, and the daily reminder check get the saved numbers.

## Reminders (added 2026-10-11)
At most one daily reminder a day, from a check that runs on the phone. Since 2026-10-23 Settings' "Show reminder now" shows the same reminder at once (see "Settings → Reminders"). Nothing leaves the phone.
- **The daily check** (WorkManager):
  - One unique job, `readyshelf-daily-review-check`, repeats once a day. Scheduling it again keeps the existing job (KEEP), so there are never two.
  - The first check is asked for at about 09:00 local time. Android decides when background checks really run, to save battery: a check can come hours late, at a different time each day, or not at all on some days. The app never promises a time and asks for no exact-alarm permission.
  - After a force-stop, Android stops the checks until the household opens DueWise again. After the phone restarts, the schedule comes back by itself.
  - Each check uses the phone's current local date (`LocalDate.now()`) and the planning settings. Every status comes from DatePolicy, through `RuleRunner.needsReview`, the same list as Home.
  - It always reads the household's own database and planning settings, never the separate sample data (added 2026-10-12).
- **What causes a reminder:** a batch whose status is PAST_DATE, TODAY, or NEAR_DATE.
  - NEEDS_REVIEW alone never causes one. Those batches stay on Home's list.
  - Sample batches and batches with 0 left never cause one. Archived batches are never given to the rules.
  - Medicine counts, but the notification never names it: it says "medicine" instead (see "The notification").
- **The order of the checks** (`ReminderCheck`):
  1. Reminders are off: nothing happens, and the database isn't read.
  2. DatePolicy is not connected: nothing happens.
  3. No batch causes a reminder: nothing is shown, and a daily reminder that is still shown is removed.
  4. A daily reminder was already shown today: nothing happens.
  5. Notifications are not allowed: nothing happens.
  6. Otherwise the reminder is shown.
- **At most one a day:** the local day is saved to the phone's storage (with `commit`) before the notification is posted. A second check, or a restart, on the same day doesn't post again. On the next local day it can post again.
- **The notification:**
  - Channel "Review reminders" (`review_alerts`), high importance since 2026-10-18: it pops up at the top of the screen and plays the phone's notification sound. Do Not Disturb and the phone's sound settings still apply, and DueWise never asks to bypass Do Not Disturb. (Do Not Disturb has a "Reminders" exception that the household can turn on.)
  - Until 2026-10-18 the channel was the silent `review_reminders` (low importance: no sound and no pop-up). An app can't make a channel louder, so every start creates the new channel and then deletes the old one. Android's notification settings for DueWise may mention a deleted category. If the household had turned the old category off, the new one starts off too. The old ID is never used again, because Android would bring back its old settings.
  - **Title:** "Household items need review".
  - **Text** (since 2026-10-19, the student's choice; `ReminderList`, tested by `ReminderListTest`): the items that cause the reminder, in Home's order (soonest date first, so passed dates come first).
    - One entry: "Canned beans". Two: "Canned beans and Granola bars". More: "Canned beans, Granola bars, and 3 others" ("and 1 other" for one). Until 2026-10-23 it said "and 3 more"; the student chose the word "others".
    - A name is listed once, even when several batches have it (capital letters and spaces around it don't matter). "3 others" counts the other names, not batches.
    - Medicine is never named. All medicine together is one entry, "medicine" ("Medicine" when it comes first), for example "Canned beans, medicine, and 2 others".
    - Names are shown as the household typed them. The expanded notification shows the same line, so a long name isn't cut off there.
    - Until 2026-10-19 the text was always "Open DueWise to see the review list.", with no names and no counts.
  - **The lock screen** (the student's choice on 2026-10-19: follow the phone's setting): the notification is private (`VISIBILITY_PRIVATE`). Its public version has only "Household items need review" and "Open DueWise to see the review list.".
    - On a locked phone that hides sensitive notification content, Android shows only the public version.
    - When the phone shows sensitive content on the lock screen, the names appear there too. That setting was on in the emulator; the household can turn it off in Android's settings.
    - Checked on the emulator on 2026-10-19, locked with a test PIN: the names showed with the setting on, and only the public version with it off.
  - **The number on the app icon** (since 2026-10-23, the student's request: "similar to LinkedIn … the app logo should show the total number of items that need attention"). The student's answers: the number is Home's status tiles added up, the reminder carries it (no second notification), and the widget shows it too.
    - Android has no way for an app to put a number on its icon by itself. The phone's home screen shows a badge only while the app has a notification in the list, and the number is that notification's `Notification.number` (`setNumber`). So the number shows while the reminder is shown, and goes when the reminder is tapped, swiped away, or removed.
    - **What it counts:** every batch with "Date passed", "Date is today", or "Near date", the same number as Home's tiles added up (`ReviewCounts.tilesTotal`): medicine, old sample items in the household's records, and batches with 0 left count too. So it can be larger than the reminder's list. Dates that only need review have no tile and don't count. A shown reminder always has at least 1.
    - **How it shows is up to the home screen:** some show the number on the icon (many Samsung phones can; not checked on one). The Pixel emulator (Android 17) shows a dot, and its long-press menu has no notification part, so the number doesn't show there (checked on 2026-10-23). The student's phone (moto g play 2024, Moto App Launcher on Android 14) has only Android's "notification dots" setting, so it is expected to show a dot; whether its long-press menu shows the number is not checked yet.
    - The lock-screen version has no number, and Android doesn't show the number inside the notification. Android's own settings can turn the dot off.
  - It always has the same notification ID, also when "Show reminder now" shows it, so a new one replaces the old one. It is not shown on a connected watch (`setLocalOnly`). Apps that the household allowed to read notifications can still read its words, names included.
  - Tapping it opens Home at the top, at the status tiles. (From 2026-10-19 Home's review lists stayed closed until the household tapped "Show the review list"; before that, Home opened scrolled to "Needs review". Since 2026-10-22 Home has no review lists.) A Home that was already in front scrolls back to the top. Home opens on top of the screen that was open, so an open form is kept, and Back returns to it.
- **Refreshing:** Home works out the same list whenever it opens or a record changes. When nothing causes a reminder any more, Home removes a daily reminder that is still shown, at once. Home never posts a new one. While the sample data is open, Home never changes a reminder, because the reminder is about the household's own records.
  - **Since 2026-10-23 a shown reminder is brought up to date** (its names and its number on the app icon): after every saved change to the household's batches, on any screen (Room's change tracker on the `batches` table, the same hook as the widget), and whenever Home works out its list. `Reminders.refreshShown` reads the records again on its own thread, with the same rules as the daily check. It removes the reminder when nothing causes one any more. Otherwise `ReminderNotifier.updateIfShown` posts the new words and number **quietly**: no second pop-up or sound (`setOnlyAlertOnce` and `setSilent`), and the reminder keeps the time it first came, so it stays in its place in the list. Nothing is posted when nothing changed.
  - **A removed reminder stays removed:** an update happens only while the reminder is shown (checked again right before posting), and while none is shown nothing is read. A tap or a swipe at the very same moment as an update can't be seen in time; that post is then quiet too (a known limit).
  - Until 2026-10-23 a shown reminder kept the names it was posted with, until it was removed or the next day's reminder replaced it.
  - Saving the planning settings doesn't refresh a shown reminder until Home works out its list again (like the widget; see there).
- Tapping a daily reminder while the sample data is open opens Home with the sample data and its label (a known limit; see "Not in this step").
- **Settings → Reminders:**
  - The "Daily reminder" switch. Turning it on asks for the notification permission on Android 13 and newer. Turning it off cancels the scheduled check and removes a shown reminder.
  - If the permission is refused, a short message says so and that everything else still works. Every other feature works without the permission. After two refusals Android stops asking; the how-to text explains how to allow notifications in Android's settings (under "More about this" since 2026-10-18).
  - A line says whether DueWise's notifications are allowed. It is read again when the household comes back to the screen.
    - **Its look** (since 2026-10-19, later that day): a warning (pale brass with a warning sign; gold until 2026-10-23) only while the daily reminder is on but its notification can't appear, because notifications or the reminder channel are off. Otherwise it is a quiet note, also when notifications are off while the daily reminder is off, so a household that keeps them off isn't pressed to change that (Android's guidance for a refused permission). The words are the same in both looks.
  - "Open notification settings" opens Android's notification page for DueWise, or else DueWise's app page. If neither exists, a dialog explains the way there.
    - Since 2026-10-19 (later that day) it is right under that line. Since that evening the card's order is: the "Daily reminder" switch (right under the title), its "Not available yet" note when it applies, the summary and the last reminder (in smaller words, as the switch's supporting text), the notifications line and "Open notification settings", "Send a test reminder" ("Show reminder now" since 2026-10-23), and "More about this" at the end of the card.
  - "Last reminder: 11 Oct 2026" or "No reminder shown yet." (until 2026-10-19 evening: "Last daily reminder: …" or "No daily reminder has been shown yet.")
  - The text about delays described above.
  - **"Show reminder now"** (since 2026-10-23, the student's request and choices) shows the reminder right away, with the household's own items: the same notification as the daily reminder (same title, text, lock-screen version, and ID), so a new one replaces a shown one.
    - It reads the records on a background thread (`Reminders.showNow`) and works while the "Daily reminder" switch is off too.
    - **Not recorded** (the student's choice): "Last reminder" keeps its date, and the daily check still comes as usual, so the phone can pop up a second time that day.
    - **Nothing near or passed** (the student's choice): no notification, only the message "Nothing is near date or passed. No reminder to show." A reminder that is still shown is removed, like the daily check does.
    - The other messages: "Reminder sent. Look in your notifications.", "Notifications are not allowed. Everything else still works.", "Not available yet: it needs the date rules.", and "No reminder: checking your items stopped with an error. Try again." (the error itself goes only to the phone's log).
    - It asks for the permission the same way as the switch, and shows the reminder after "Allow".
    - **Until 2026-10-23** the button was **"Send a test reminder"**: a notification titled "Test reminder" with its own ID (1002) and a fixed text. It didn't check items and didn't count as the daily reminder.
- The switch can be turned on since 2026-10-18, when DatePolicy was connected. It starts off; the household turns it on. Before that day, the switch was turned off with "Not available yet", while the channel, the permission request, "Open notification settings", and "Send a test reminder" already worked.
- Opening DueWise from its launcher icon shows its screens as they were, also when a reminder tap started DueWise. Home closes a second copy of itself (fixed in the review of 2026-10-11).

## Home banner (added 2026-10-18)
- **Since 2026-10-22: status tiles only** (the student's choice). The student found the heading "Items need review" and the "Show the review list" tile redundant, so both are gone, and Home has no review lists any more.
  - Each status tile opens Inventory with its batches. Medicine with a passed or near date is under "Date passed" or "Near date", like any other batch (the student's answer).
  - Batches whose date needs review still get no tile (the student's answer). Inventory lists them under its Status filter "Date needs review".
  - Before that, the banner had the heading "Items need review" (with the reminder's shelf as its icon until the morning of 2026-10-22), the status tiles, and the "Show the review list" tile. That tile opened Home's two review lists, "Needs review" and "Medicine dates to review" (with the note "DueWise only shows the dates you recorded. It gives no medicine advice."). The lists started closed from 2026-10-19 and were always shown before that.
- **Where:** at the very top of Home, under the logo. All of Home's tiles form one grid of two columns, in this order: the status tiles, then Inventory, Purchase/Renewal list ("Purchase list" until 2026-10-24), Settings, and Add batch (since 2026-10-19, later that day: the student asked for every icon on Home to line up in rows and columns, in a square grid, and chose the order; Add batch since 2026-10-23).
  - **No empty cell (since 2026-10-23, the student's request):** the student marked the empty cell next to Settings on a screenshot and asked for "Add batch" there, as a tile, instead of the floating button. With 0 or 2 status tiles, Add batch takes the last cell, next to Settings. With 1 or 3, it is alone in the last row and fills it (the student's choice). Before, with 2 status tiles (the demo's sample data) or none, the last cell, at the bottom right, stayed empty.
  - Add batch scrolls away with the other tiles (the student's choice): Home has no floating button, and it opens at the top, where the tile is.
  - The gray box that was around the status tiles is gone, so all tiles line up in the same two columns.
- **The note under the tiles** (since 2026-10-22):
  - "Nothing needs review right now." when the household has batches and none of them needs review.
  - The date rules' message when they are not connected or stopped with an error, so Home never looks calm because the rules didn't run.
  - No note when there are no batches (the getting-started message says enough), or when the only batches that need review have a date to review (they have no tile; see above).
- **Tiles** (since 2026-10-18; before that, one line of text per status):
  - Each status tile is a white card on the pine band (since 2026-10-23, B1; light gray on 2026-10-22, dark before). It shows the status icon in a circle of the status's label colors (the circle in the label's color, the icon in the color of its words), then the count in large digits in the status's accent color, then the status word (see "Status labels and batch cards" for the colors).
  - The status tiles come in the order "Near date", "Date is today", "Date passed" (since 2026-10-25, the student's request to swap "Near date" and "Date passed" so that "Near date" is the first tile; until then "Date passed" came first). A status with no batches gets no tile.
  - **"Date needs review" gets no tile** (since 2026-10-19, the student's choice, kept on 2026-10-22). Inventory's Status filter "Date needs review" lists those batches.
  - **The "Near date" tile says "Near date" again** (since 2026-10-19 evening, the student's choice after the UI review). From that afternoon until then it said "Action needed date" (the student's words then), and a tap on it opened Inventory with the "Near date" chip, so the tile and the screen it opened used different words. Now the tile, Inventory's Status chip, the status labels, Batch details, and Settings' "Near-date days" all say "Near date". "Near date" also doesn't tell anyone to act, which matters because the tile counts medicine too.
  - TalkBack reads each status tile as one line, for example "Near date: 2 batches".
- **Tile layout** (since 2026-10-19: the student asked for the best practices of tile and icon layout on Home):
  - Two tiles in a row. With large text, one tile per row at full width (LargeText).
  - Every row is as tall as the tallest one, and two tiles in a row share it equally, so the tiles line up. With an odd number of tiles, the last one, Add batch, fills its row (since 2026-10-23, the student's choice). From 2026-10-19 until then that last tile was half wide, next to an empty cell. (Until the fix on 2026-10-19, that last tile didn't show at all: TEST-RESULTS.md, D-11.)
  - All tiles have the same parts: an icon in a circle at the top left, a ">" arrow at the top right that shows that a tap opens another screen, and the words below. The top part of every tile is as tall as a status tile's, so at every text size the icons and the first line of the words line up from tile to tile.
  - The status colors are only on status tiles, whose icons are solid. The shortcut tiles have a pine outline icon in a cream circle (#F6EFE2, since 2026-10-23, B1): a box for Inventory, a cart for Purchase/Renewal list, a gear for Settings, and a plus for Add batch (since 2026-10-23). So the statuses are what the eye finds first. (On 2026-10-22, the student's choice, they had a gray outline icon (#474747) in a white circle. Before that a black icon in a light gray circle that almost matched the tile, blue until that morning.)
  - Home's Add batch is a white tile like the other shortcut tiles (since 2026-10-23, the student's choice; a brass tile could look like the brass "Date is today" status). Inventory's floating "Add batch" button is light brass (since 2026-10-23); the swap cards' "Review swap" buttons are pine.
- **A tap on a tile** (since 2026-10-18) opens Inventory with that status chosen in its Status filter, so it lists only those batches. For example, a "Near date" tile that says 2 opens Inventory with "Near date" chosen, showing those 2 batches. Since 2026-10-19 evening Inventory's filters are folded then, and the choice showed as a "Near date ✕" chip under the search box.
  - **The list's header (since 2026-10-23, the student's choice "Header like its tile"):** the list starts with that status's tile as its header, on a pine band under the top bar (rounded bottom corners, like Home's), in place of the chip.
    - It is the same tile as on Home: the colored circle and icon, the count, and the word. The count is the tile's number (every batch that is not archived, medicine too), whatever else is searched or filtered.
    - It has no arrow and can't be tapped, and TalkBack reads it as a heading, for example "Date passed: 3 batches".
    - It follows a status chosen later in the filters; "All statuses" removes it. "Clear all" sets back only the other filters, so the status stays.
    - Opened from Home's Inventory tile, Inventory has no header, and a chosen status still shows as a chip.
  - Inventory counts the same batches as the tile: those that are not archived, medicine too, from the same data, day, and Settings numbers.
  - The household can then choose another status, or "All statuses", there. The home icon (below) and Back return to Home.
  - **The home icon (since 2026-10-22, the student's choice):** Inventory opened from a status tile has only a home icon in its top bar, in place of the back arrow and the title "Inventory".
    - It is Material Symbols "home" (Rounded, outline), white like the back arrow on the pine top bar (since 2026-10-23, later that day; gray #5F5F5C, and #474747 before), and its spoken label is "Home". The screen's own label stays "Inventory".
    - A tap on it closes Inventory, so the Home that was open shows again, as it was.
    - Inventory keeps the icon when the phone turns, after "All statuses", and after Batch details.
    - Opened from Home's Inventory tile, Inventory keeps the back arrow and the title.
  - Each status tile has a small ">" arrow, and a press on any tile shows a ripple inside its rounded corners.
  - TalkBack says "Double-tap to show these batches in Inventory".
- **What it counts:** every batch that needs review, that is every status except "Later date" (the same list as the daily reminder's). Medicine, sample items in the household's records, and batches with 0 left count too.
- **No status tiles** when nothing needs review, when the only batches that need review have a date to review, when the date rules are not connected, and when they stopped with an error. Then the grid has only Inventory, Purchase/Renewal list, Settings, and Add batch, and the note says why (see above).
- While the sample data is open, it counts the sample data.
- The words carry the meaning; the tiles' icons and colors only help.

## Home-screen widget (added 2026-10-18)
- **What it is:** "Review list", a widget for the phone's home screen (3 × 1, and it can be resized). It is never shown on the lock screen. It shows "DueWise" and one line:
  - "7 household items need review" ("1 household item needs review" for one) when the household's review list (the same list as Home's: every status except "Later date") has at least one batch. Since 2026-10-23 (the student's choice) the number is Home's status tiles added up, the same number as on the app icon (see "Reminders"). When every batch on the list only has a date to review (no tile), the line stays "Household items need review", with no number. Until 2026-10-23 the line was always "Household items need review".
  - "Nothing needs review right now." when that list is empty
  - "Open DueWise to see the review list." when the answer isn't known: the date rules are not connected or stopped with an error, or the widget wasn't refreshed yet. So a wrong "Nothing needs review" is never shown.
- **Look (since 2026-10-19, the student's choice):** always dark, whatever the phone's setting, also since the app became light on 2026-10-22. Since then it is Graphite's dark gray (#303030) with light words (#E8E8E8); until then it was the app's dark card color (#1F1F23) with #E3E2E6. The hourglass from the logo comes before "DueWise", in the kit's heavier small version, because it is under 48 px. The hourglass is only decoration, so TalkBack reads "DueWise". Before that day it had Material's default purple-gray colors, light or dark with the phone.
- **Privacy:** it uses only the household's own records, never the separate sample data, like the daily reminder. It never names items. Since 2026-10-23 (the student's choice) it gives one number, Home's status tiles added up; until then it gave no counts. It needs no permission.
- **Tap:** opens Home at the top, at the status tiles, like a reminder (since 2026-10-19; before that, at "Needs review"). Each time a tap opens Home, the widget gets a new tap, so a second tap still opens Home instead of only bringing DueWise to the front. The widget's description in Android's widget list ends "Tap it to open DueWise." (since 2026-10-19; before that, "Tap it to open the review list."). Since 2026-10-23 it starts "Shows how many of your own household items need review" ("Shows whether…" before).
- **Refreshing** (`ReviewWidget`, on one background thread; nothing is read while no widget is placed):
  - when the widget is placed, after a restart or an app update, and about every 6 hours (`updatePeriodMillis`; this wakes the phone for a moment about 4 times a day)
  - when DueWise starts
  - after every saved change to the household's batches, on any screen (Room's change tracker on the `batches` table)
  - whenever Home works out the household's list (for example on a new day, or after the planning settings were saved)
  - after each daily reminder check
- **Text:** sizes are in sp, so they grow with the phone's font size. Both texts shrink to fit, so the whole line still shows in the 1-row widget at font scale 2.0 (checked on the emulator on 2026-10-18). The line with the number (2026-10-23) is shorter than "Open DueWise to see the review list."; it was not checked again at font scale 2.0.
- **Known limits:**
  - A date that becomes near at midnight shows at the next refresh (the 6-hour refresh, the daily check, or the next app start).
  - After the planning settings are saved, the widget waits for the next refresh if the household leaves from Settings without going back to Home. Since 2026-10-23 a shown reminder's number waits the same way.
  - After a force-stop, or before DueWise was ever opened, Android sends the widget no updates until DueWise is opened.
  - While the sample data is open, the widget still shows the household's records, and a tap opens Home with the sample data.
- **Tests:** `ReviewWidgetStateTest` (what it says, and since 2026-10-23 its number) and, on the emulator only, `ReviewWidgetTest` (Android offers it with its settings; each line names no items, and only a line with a tile's batches has a number; the whole widget is the tap target; nothing is read while none is placed).

## Photo entry (added 2026-10-12; replaced "Scan a label")
When adding a batch, the household can take a photo of the product and a close photo of the printed date. DueWise reads the text on the phone and shows it for review. Nothing goes into the form until the household chooses it, and nothing is saved until they confirm the form.

The student's decisions (2026-10-12): Tesseract stays the text reader (not ML Kit; see "Privacy"), the old "Scan a label" is replaced, and the raw label text is kept in the batch history (no database change).

**Hidden since 2026-10-23** (the student's request: "I am not using this functionality, so hide it"): Add batch has no "Take label photos" button, so nothing opens the Label photos screen. Only the button is hidden. The screen, the text reader, the possible dates, and the form's code for a photo result stay, with their tests, and `LABEL_PHOTOS_SHOWN = true` in `BatchEditActivity` shows the button again. The rest of this section describes photo entry as it works while the button is shown.

- **Where:** Add batch → "Take label photos" opens the Label photos screen. Since 2026-10-19 the section comes right after "What kind of item?", and its button is tonal, because "Review and save" is the form's main button. Since 2026-10-22 the section is only that button (the student's choice): the heading "Label photos (optional)" and the line "Fill in the form from photos of the label." were removed. The section is shown only when adding: not when editing, not for "Record bought item" (its product is fixed), and not on the review. Since 2026-10-23 it is never shown (see above). Every manual field works as before.
- **Two steps, both optional:**
  1. A photo of the product.
  2. A close photo of the printed date.
  - Each step has "Take photo", "Choose a saved photo" (the photo picker, which needs no permission), "Skip this photo", and "Back to the form".
- **The camera (CameraX):**
  - DueWise's own camera view. The screen stays on a step until the camera has saved the photo ("Hold still…").
  - **Focus:** continuous autofocus; tap the screen to focus ("In focus." or "Couldn't focus. Hold about 15 cm (6 inches) away, or turn on the light."); pinch to zoom.
  - **"Take photo" focuses first** (since 2026-10-18), on the point last tapped or else the middle, and waits at most 4 seconds. The photo is taken only when the camera says it is in focus; otherwise the screen says "Couldn't focus, so no photo was taken…" and asks for about 15 cm (6 inches). A camera without autofocus takes the photo right away. Why: on the household's phone, whose camera can't focus closer than 10 cm, photos were taken before the camera had focused, and the text reader found almost nothing.
  - **Distance hints:** about 20 cm (8 inches) for the product photo and 15 cm (6 inches) for the date photo, zooming in instead of moving closer.
  - **Rotation:** the photo is turned to match how the phone was held, and the text reader turns it upright. Every step and the review keep their state through rotation and Android closing the app. A photo that was being saved or read when Android closed the app is lost, and the screen says so.
  - **Poor light:** "Turn on the light" (the torch), shown only when the camera has one. The flash is off, because it makes glare on shiny packages. Tips on screen.
  - The camera is on only during a photo step. It is off on the review.
- **The camera permission:**
  - It is asked for only on this screen, once by itself.
  - After a refusal, the screen explains it and offers "Ask again" (while Android still asks) or "Open app settings", plus the picker, Skip, and "Back to the form" (fill in the form by hand).
  - A phone without a camera, or a camera that can't start, gets the same choices without the camera.
- **Temporary photos:**
  - A camera photo goes straight into DueWise's private cache folder (`cache/label_photos/`). No other app can read it, and it is never sent anywhere (no INTERNET permission).
  - Each photo is deleted right after its text is read. The folder is also emptied when the screen opens and when it closes for good (Cancel, Back, or a chosen date).
  - A picked photo is only read; it is never copied, changed, or deleted.
  - The recognized text is shown as plain text that can't be selected, so it doesn't go to the clipboard. It is never written to the log.
- **Reading (text extraction):** Tesseract reads each photo on the phone (`LabelPhotoReader`). It only returns text lines and knows nothing about dates.
- **Possible dates (date parsing):** the date-candidate parser (`DateCandidateParser`, through `DateCandidateParserContract`) reads only the date photo's lines and returns candidates. The app's safety layer (`PhotoDateChoices`) turns them into buttons:
  - A complete date → one button, for example "Use Best by: 25 Oct 2026 (read “BEST BY 10/25/2026”)".
  - A date that can be read two ways (03/04/27) → the note "“03/04/27” can be read two ways. Choose one below." and three buttons: month first, day first, and "Keep “03/04/27” as printed (needs review)", an incomplete date. DueWise never picks a reading.
  - Only a month and a year → one button that keeps the printed text as an incomplete date. No day is made up.
  - Nothing is chosen beforehand. Even a single date needs a tap.
  - Messages instead of buttons: no text, no date found, the date photo was skipped or could not be read, or the parser stopped with an error (shown as text; the app doesn't close). Until the parser was connected on 2026-10-18, the review said "Not available yet…" instead. Since 2026-10-21 the parser's error is "No dates: reading stopped with an error. Enter the date on the form." (from 2026-10-19 evening, "No possible dates: the date-candidate rules stopped with an error…"), without the error's details: they are neither shown nor logged, because they could contain label text.
  - **While the photos are read** (since 2026-10-19 evening): a moving progress bar under "Reading the photos…" (until 2026-10-21 it added "This can take a few seconds."), so the wait is visible. TalkBack skips the bar, because the words say it. Only that status line and the possible dates' message are live regions; the two long texts read from the photos are not, so TalkBack doesn't read everything at once.
  - The note "“03/04/27” can be read two ways…" has the same text size as the other texts (since 2026-10-19 evening; before that it was smaller by mistake).
- **The review:** the text read on each photo, "DueWise can't tell which product this is. Choose it on the form.", the possible dates, "Take the product photo again", "Take the date photo again", "Continue without a date", and "Cancel".
- **What the form gets:**
  - **Date:** only the date the household tapped. It goes in unconfirmed ("Date needs review" until the household ticks the confirmation), with its label type when label words were read. With no label words, the form keeps its label type, and the status line says to check it. A month-and-year or "as printed" date becomes "Part of a date" with the printed text, and since 2026-10-22 that opens "What is printed?" by itself. The month-end button stays the household's own choice.
  - **Notes:** "From the photo. Check it." (or the month-only or as-printed version) under the date; it goes away when the household changes the date.
  - **Category:** the category clue from both photos, with the rules of the old scan (a Medicine clue always wins; other clues never replace Medicine or a category chosen by hand), and its note.
  - **Name:** lines of the product photo that could be the name, as buttons. Nothing is chosen until the household taps one, and the household still chooses an existing product or creates a new one. The text on a photo never identifies the product.
- **History and review:** the form's review and the "Added" history entry say "Date read from a label photo: “BEST BY 10/25/2026”." with the raw label text, while the saved date is still the photo's date. The label type is saved with the batch as before. There is no database change.
- **Known limits:**
  - Possible dates come only from the text Tesseract read on the date photo. When the parser finds no date in that text, the household types the date.
  - Tesseract reads blurry, dark, curved, and dot-matrix print poorly. On blurry synthetic test labels it returned unreadable text (TEST-RESULTS.md, section 9).
  - Photo entry is only for adding a new batch.
  - Julian and lot codes are never turned into dates by the app; whether a code is a date is the parser's job, and the guide says a lot code is not a date.
- **Who wrote what:** Claude Code wrote the camera screen, the safety layer, and the form changes (AI-USE.md). Claude Code also wrote the date-candidate rules (`DateCandidateParser`) and their tests on 2026-10-18, at the student's request, after the student approved the test cases (see "The student's rules"). `PrintedMonthReader` (Claude's, formerly `LabelDateFinder`) is used only by the form's "Use the last day of the month" button and must not be used by the parser. `CategoryGuesser` and `NameCandidates` (Claude's) give the category clue and the name buttons.

## Locations
- A new install starts with three locations: Pantry (PANTRY), Emergency kit (EMERGENCY_KIT), and First aid box (FIRST_AID). Until 2026-10-22 the kit was called "Earthquake kit". The student renamed it that day, and database version 4 renames it in saved data too (see "Migration from version 3 to 4").
- The household can add more from the batch form ("New location"): a name and a type (PANTRY, EMERGENCY_KIT, FIRST_AID, or OTHER).
- Two locations can't have the same name. Capital letters don't make a name different.
- If the phone's database reports an error, no location is added, and the form says "The location wasn't added."
- Renaming and deleting locations are not part of this step.

## Data kept on the phone
Room database `readyshelf.db`, version 4: the household's own records. The separate sample data uses the same tables, in `readyshelf-sample-data.db` (see "Sample data").
The schema of every version is saved in `app/schemas/` so that later versions can be checked against it. Every schema change needs a migration, and destructive migration is never used.

| Table | Fields |
|---|---|
| `locations` | id, name, type |
| `products` | id, name, variant (empty if none), package size (empty if none), unit, sample (yes/no), food class (empty for Not classified; added in version 3; kept, but not asked, shown, or used since 2026-10-22 evening) |
| `batches` | id, product id, category, quantity (0–9999), date (complete date, optional), printed date text (incomplete date, optional), label type, date confirmed (yes/no), location id, food type (optional), sealed / shelf-stable / damaged / storage concern (Yes, No, or empty for Not checked; the food type and these are kept, but not asked, shown, or used since 2026-10-22 evening), archived (yes/no), sample (yes/no) |
| `batch_events` (history) | id, batch id, time, type (ADDED, EDITED, MOVED, USED, DISCARDED, ARCHIVED, or SWAPPED), details |
| `swap_actions` (swap receipts; version 3) | action id, kit batch id, pantry batch id, quantity, the date the swap was checked and saved, time. Deleted together with its batches |
| `purchase_items` (purchase list; version 3) | id, product id, kit location id, quantity, time added. One item at most for each product and kit. Deleted together with its product |

- Categories: Food, Water, Medicine, First aid supplies, Cosmetics, Documents, Subscriptions, and Other (Cosmetics, Documents, and Subscriptions since 2026-10-19). Each is stored by its name, so a new one needs no migration.
- Every change adds history: "Added", "Edited" with what changed (for example "Count corrected from 6 bars to 4 bars."), "Moved", "Recorded as used", "Recorded as discarded", "Archived", or "Swapped".
- Settings (stored in SharedPreferences): near-date days (1 to 365, starts at 30) and replacement days (1 to 3650, starts at 180, and more than the near-date days since 2026-10-11). The replacement days are a planning choice, not a safety guarantee. Until 2026-10-11 Settings held one "review soon" number for each location type (starting at 7, from the student's notes); those values stay in the file but are no longer used.
- Reminders (added 2026-10-11): the SharedPreferences file `reminders` holds whether the daily reminder is on and the local day of the last daily reminder. WorkManager keeps the scheduled check in its own database in the app's no-backup folder. Both are inside the app data that backup and phone-to-phone transfer exclude.
- Sample data (added 2026-10-12): the SharedPreferences file `data_mode` holds whether the sample data is open, and `sample_settings` holds the sample data's own planning settings. Like every other file, they are excluded from backup and transfer.

### Migration from version 1 to 2 (2026-10-11)
- Each different name + unit (+ sample mark) of the old batches became one product with no variant and no package size. Names that differ only in capital letters stayed separate products.
- Every batch kept its id, quantity, date, label type, location, and sample mark. Every history entry kept its id and text.
- Old dates start as not confirmed ("Date needs review"), food answers as "Not checked", and nothing is archived.
- If any step fails, the migration stops and the version 1 data stays as it was.
- `MigrationTest` (in `app/src/androidTest`) checks this on the emulator, not on the household phone.

### Migration from version 2 to 3 (2026-10-11)
- Products get a food class column, empty (Not classified) for every old product. Nothing is guessed.
- The new tables `swap_actions` and `purchase_items` start empty.
- No old row is copied or changed: every batch, product, location, and history entry stays as it was. If any step fails, the migration stops and the version 2 data stays as it was.
- `MigrationTest` checks this on the emulator. The existing version 1 tests now end at version 3.

### Migration from version 3 to 4 (2026-10-22)
- The student renamed the starting kit from "Earthquake kit" to "Emergency kit". No table changes: version 4 has the same tables as version 3.
- Only the place named exactly "Earthquake kit" with the type Emergency kit gets the new name. It keeps its id, so its batches, swap receipts, and purchase-list items show the new name. Every other row stays as it was.
- If a place called "Emergency kit" already exists (capital letters don't matter), nothing is renamed, so two places never share a name.
- History entries keep their text, so entries written before the update still say "Earthquake kit".
- An older build can't open a version 4 database (it stops at start), so don't install a build from before this change once the data is version 4.
- `MigrationTest` checks this on the emulator. The version 1 and 2 tests now end at version 4.

## The student's rules
Until 2026-10-18 these classes and tests were the student's to write, and AI tools didn't write them. On 2026-10-18 the student changed CLAUDE.md (rules 1 and 2) so that Claude Code writes them at the student's request. AI-USE.md records who wrote each one.

| File | Status |
|---|---|
| `app/src/main/java/com/example/readyshelf/domain/DatePolicy.java` | Written by Claude Code on 2026-10-18 and connected in AppContainer the same day (AI-USE.md) |
| `app/src/main/java/com/example/readyshelf/domain/RotationPlanner.java` | Written by Claude Code on 2026-10-18 and connected in AppContainer the same day (AI-USE.md) |
| `app/src/main/java/com/example/readyshelf/domain/DateCandidateParser.java` (added 2026-10-12) | Written by Claude Code on 2026-10-18 and connected in AppContainer the same day (AI-USE.md) |
| `app/src/test/java/com/example/readyshelf/domain/DatePolicyTest.java` | Written by Claude Code on 2026-10-18; the student approved every case first (AI-USE.md) |
| `app/src/test/java/com/example/readyshelf/domain/RotationPlannerTest.java` | Written by Claude Code on 2026-10-18; the student approved every case first (AI-USE.md) |
| `app/src/test/java/com/example/readyshelf/domain/DateCandidateParserTest.java` (added 2026-10-12) | Written by Claude Code on 2026-10-18; the student approved every case first (AI-USE.md) |

The requirements below come from the student's guide (pasted on 2026-10-11). The student describes each rule in their own words in STUDENT-WORK.md.

### DatePolicy requirements
- Accept the date input model (`DateInput`), a supplied `today`, and a near-date threshold. The default near-date threshold is 30 days.
- A missing, incomplete, or unconfirmed date has status NEEDS_REVIEW.
- A date before today has status PAST_DATE.
- A date equal to today has status TODAY.
- A date 1 through the supplied near-date threshold days away has status NEAR_DATE.
- A later date has status LATER.
- Compare calendar dates. Do not divide milliseconds by 24 hours.

Decisions (proposed by Claude Code and approved by the student on 2026-10-18; `DatePolicyTest` has tests for each one):
- DP1. A missing, incomplete (printed text only), or unconfirmed date is NEEDS_REVIEW, even when it has passed. It appears on Home's list but never causes a daily reminder (see "Reminders").
- DP2. The label type (Best by, Use by, Expires, Other) doesn't change the status. DueWise only reports dates and never judges the food.
- DP3. A confirmed complete date that also has printed text (from "Use the last day of the month") gets its status from the complete date.
- DP4. Days are counted as calendar days with `java.time`. Leap days, month ends, New Year, and clock-change days each count as one day.
- DP5. A near-date threshold of 0 means no near-date window, so tomorrow is LATER. A negative threshold is an error (`IllegalArgumentException`); Settings allow only 1 to 365.
- DP6. There are no date limits: very old dates are PAST_DATE and far-future dates are LATER. The answer depends only on the inputs, never on the phone's clock.
- The student's notes in STUDENT-WORK.md (2026-10-04) use other status names and windows (TEST-RESULTS.md, section 6.3). DatePolicy follows the requirements above, because the app's statuses are fixed by `DateStatus`.

### RotationPlanner requirements
- Accept today, the near-date threshold, and the replacement threshold as inputs. The defaults are 30 and 180 days. Changed settings apply without fixed constants.
- Plan food swaps only. Exclude medicine and other categories.
- Require SHELF_STABLE_FOOD products, and confirmed, sealed batches without damage or storage concerns.
- Exclude infant formula, perishable food, and unclassified food from automatic swaps.
- Require complete, confirmed dates and known label types for both batches.
- Kit source batches have dates 1 through the near-date threshold days away.
- A replacement is a pantry batch with the same product ID and counting unit, a date later than the source's, and at least the replacement threshold days from today. The 180 days are a planning choice, not a safety guarantee.
- Sort sources by date, then batch ID. Sort replacements by earliest qualifying date, then batch ID.
- Use available quantities only. Partial swaps are allowed. Each pantry unit goes to at most one proposal in a plan. Equal quantities move in both directions.
- Return proposed movements, clear reasons, and quantities still needing replacement. Do not change stored inventory.
- Never recommend pantry use for a source dated today or earlier.

**The student's departure from the guide (2026-10-22, evening):** the student chose to swap everything but medicine, documents, and subscriptions, and to drop the condition and food class checks. So three lines above no longer hold: "Plan food swaps only. Exclude medicine and other categories.", "Require SHELF_STABLE_FOOD products, and confirmed, sealed batches without damage or storage concerns.", and "Exclude infant formula, perishable food, and unclassified food from automatic swaps." RP10 below takes their place; every other line still holds.

Decisions (proposed by Claude Code and approved by the student on 2026-10-18; `RotationPlannerTest` has tests for each one). RP4, RP5, RP6, and RP8 changed, and RP10 was added, on 2026-10-22 evening; the student approved the changed tests with the plan (AI-USE.md):
- RP1. A kit batch is in an Emergency kit or First aid location. A replacement must be in a Pantry location. Batches in Other locations are never kit batches or replacements. Every kit batch that has units gets exactly one note; pantry and Other batches get none.
- RP2. A batch with 0 units is left out: no swap and no note.
- RP3. The known label types are Best by, Use by, and Expires. "Other" (for example a packed-on date) is not known (LABEL_TYPE_NOT_KNOWN). The two batches' label types don't have to match.
- RP4. The swap rules use a date only when it is complete, confirmed, and has a known label type (like DP1). Otherwise the note names what is missing, and nothing is needed until the household reviews the date, even when the recorded date has passed. The app's own safety check still lists items the household buys again with a recorded date of today or earlier on the Purchase list (see "Safety checks in app code").
- RP5. A kit batch's checks run in this order, and the first one it fails gives the reason:
  1. A category that is swapped (CATEGORY_NOT_SWAPPED; see RP10).
  2. A date the rules can use (RP4).
  3. Today or earlier (DATE_TODAY_OR_PASSED).
  4. More than the near-date days away (NOT_NEAR_DATE).

  A kit batch that passes them all is a source. Until 2026-10-22 evening the first check was Food (NOT_FOOD), and two more came last: Shelf-stable food (INFANT_FORMULA, PERISHABLE_FOOD, UNCLASSIFIED_FOOD), then Sealed Yes, Damaged No, and Storage concern No (CONDITION_NOT_CONFIRMED), where "Not checked" never passed.
- RP6. Still needing replacement:
  - all of a kit batch of a swapped category whose usable date is today or earlier
  - for a source, whatever its swaps don't cover: 0 (SWAP_PROPOSED), the rest (PARTLY_REPLACED), or all of it (NO_REPLACEMENT)

  Every other note needs 0, CATEGORY_NOT_SWAPPED too. Until 2026-10-22 evening all of near kit food that its food class or answers kept out of swaps was needed as well (the student's choice then).
- RP7. Sources go by date, then batch ID. For each source, the qualifying pantry batches go by date (earliest first), then batch ID. Proposals are listed in that order, and notes by batch ID. The order of the given list doesn't change the plan.
- RP8. A replacement:
  - has the same product ID and exactly the same unit text
  - has a date strictly later than the kit batch's (the same day isn't later)
  - has a date at least the replacement days after today (exactly that many qualifies)
  - is of a category that is swapped (RP10) and has a date the rules can use (RP4); its category doesn't have to be the kit batch's (until 2026-10-22 evening it had to pass the same food, food class, answer, and date checks as a source)

  Days are calendar days (`java.time`, like DP4).
- RP9. 0 is allowed for either number; with 0 near-date days, no kit date is near. A negative number is an error (`IllegalArgumentException`), like DP5. A pair that Settings refuse (replacement days not more than the near-date days) still works. Then RP8's "later than the kit batch's date" keeps earlier pantry batches out.
- RP10 (added 2026-10-22 evening, the student's choice). The categories that are swapped are a fixed list: Food, Water, First aid supplies, Cosmetics, and Other. Medicine, Documents, and Subscriptions are never swapped, as kit batches or as replacements. A category added later isn't swapped until it is named in the list (a test fails until then). The swap rules keep this list themselves; the app's safety net has its own copy (see "Safety checks in app code").

### DateCandidateParser requirements (added 2026-10-12)
These come from the student's guide (pasted on 2026-10-12):
- Read the text lines of the date photo (the text reader's output, which can contain reading mistakes). Never see the photo: text extraction and date parsing stay separate.
- Return every possible printed date. When several dates appear, the household selects the correct one.
- Never guess whether 03/04 means March 4 or April 3: return both readings (TWO_READINGS).
- Never invent a day for month-and-year text (MONTH_AND_YEAR).
- Never interpret a lot code as a date.
- Keep the raw label text and the label type with every candidate.
- The app never saves a recognized date without the household's confirmation, and never claims that the text identifies the product.

Decisions (proposed by Claude Code and approved by the student on 2026-10-18; `DateCandidateParserTest` has tests for each one):
- D1. A day and a month that can't be swapped give one date: a number above 12 must be the day, and two readings of the same day are one date.
- D2. Month names are read: JAN to DEC, full or short, in any letter case, with or without a dot, before or after the day, and days like "1ST".
- D3. Two-digit years mean 20YY.
- D4. Letters that the text reader read instead of digits are fixed (O as 0; I, L, and | as 1) when they touch a digit and are not inside a word. The date text stays as printed.
- D5. Impossible dates (February 30, month 13, February 29 outside a leap year) are not offered.
- D6. "/", "-", and "." separate the numbers, and year-first dates are read. Numbers with only spaces between them are not read.
- D7. Two-part numbers such as "10/26" are not offered: they could be a month and a day, or a month and a year.
- D8. Label words at the end of the line directly above count for the first date on the next line. Label words two lines above don't. The raw text is the date's own line.
- D9. Label words, in any letter case: BEST BY, BEST BEFORE, BEST IF USED BY, BB, and BBE give Best by; USE BY, USE BEFORE, and USE OR FREEZE BY give Use by; EXP, EXPIRES, EXPIRY, and EXPIRATION give Expires; SELL BY, FREEZE BY, MFG, MFD, MANUFACTURED, PACKED, PKD, PRODUCED, and PROD give Other. Words that only contain them (BBQ, EXPRESS) are not label words, and label words belong only to the first date after them.
- D10. Made-on and packed-on dates are offered with the label Other. Only a date right after LOT is never offered, because a lot code is not a date.
- D11. `today` is not used: the same text gives the same dates on every day, and old or far-future dates are still offered.

### The contracts (the app's side)
The app uses these classes only through interfaces in the `domain` package (the first two changed on 2026-10-11, the third added on 2026-10-12, before the classes were written):
- `DatePolicyContract`
  - `DateStatus statusOf(DateInput date, LocalDate today, int nearDateDays)`
- `RotationPlannerContract`
  - `RotationPlan plan(List<PlanBatch> batches, LocalDate today, int nearDateDays, int replacementDays)`
- `DateCandidateParserContract`
  - `List<DateCandidate> findCandidates(List<String> lines, LocalDate today)`

The types they use only hold data, with no rules (the product food class is stored since version 3):
- `DateInput`: complete date, printed text of an incomplete date, label type, confirmed.
- `DateStatus`: NEEDS_REVIEW, PAST_DATE, TODAY, NEAR_DATE, LATER.
- `PlanBatch`: batch id, product id, unit, category, food class, location type, available quantity, `DateInput`, and the sealed, damaged, and storage-concern answers. Since 2026-10-22 evening the swap rules ignore the food class and the answers (the contract is unchanged).
- `FoodClass`: SHELF_STABLE_FOOD, PERISHABLE_FOOD, INFANT_FORMULA; null means unclassified.
- `RotationPlan`: a list of `SwapProposal` (kit batch id, pantry batch id, quantity moved each way) and a list of `PlanNote` (batch id, `PlanReason`, quantity still needing replacement).
- `DateCandidate` (added 2026-10-12): one of COMPLETE (a date), TWO_READINGS (the month-first and day-first dates), or MONTH_AND_YEAR (a `YearMonth`), with the raw text it came from, the part read as the date, and the label type (or null). Made with `complete(...)`, `twoReadings(...)`, or `monthAndYear(...)`; it has no rules.
- The rules must not call the app's helpers `DateEntry`, `RuleRunner`, `StockChange`, `PhotoDateChoices`, or `PrintedMonthReader`. Each rule class makes every check itself.

What the app does with the answers (all three rules are connected since 2026-10-18):
- Inventory and Batch details (and Home's review lists until 2026-10-22) show the status as a label with its word: "Date needs review", "Date passed", "Date is today", "Near date", or "Later date". Since 2026-10-18 the label also has an icon and a color (see "Status labels and batch cards").
- Home's status tiles count every batch whose status is not LATER, medicine too, and a tap shows them in Inventory (since 2026-10-22). Until then Home listed them under "Needs review", soonest date first, with medicine apart under "Medicine dates to review" and the note "DueWise only shows the dates you recorded. It gives no medicine advice." Medicine is never part of a swap or a replacement need.
- Home "Recommended swaps" shows each proposal as a card with "Review swap" (see "Swaps"). Since 2026-10-21 (the student's choice) there is no note above the cards. Review swap keeps "Replacement days are a planning choice, not a safety guarantee." in view, so it still shows before any swap is confirmed. Until then a note said so on Home too: from 2026-10-19 "Proposals only: nothing moves until you confirm. The replacement days are a planning choice, not a safety guarantee.", and earlier on 2026-10-21 "Proposals only. Replacement days are a planning choice, not a safety guarantee.". "More about this" above the cards, shown only with them, says what is never swapped (medicine, documents, subscriptions, and kit items dated today or earlier) and that kit items to buy again are on the Purchase list (since 2026-10-22 evening; before that, only that kit food dated today or earlier is never swapped and that its replacement is on the Purchase list).
- The Purchase/Renewal list shows each replacement need with its reason (for example "Still needs replacement: 2 cans. Pantry batches cover only part of it.") and an "Add to purchase list" button (see "Purchase/Renewal list").
- The daily reminder uses the same statuses: only PAST_DATE, TODAY, and NEAR_DATE cause a reminder (see "Reminders").
- Archived batches get no status. Inventory (with "Show archived batches" on) and Batch details never give them to DatePolicy. Batch details says "Status: none (archived)."
- The app passes the phone's date as `today`, the Settings numbers as the thresholds, and each product's stored food class (which the swap rules ignore since 2026-10-22 evening). Tests pass fixed dates.

How the rules get connected: `AppContainer` holds `datePolicy`, `rotationPlanner`, and (since 2026-10-12) `dateCandidateParser`.
Each one stays `null` until its class is written, its tests pass, and it is connected. Since 2026-10-18 all three are connected: `datePolicy` holds a `DatePolicy`, `rotationPlanner` holds a `RotationPlanner`, and `dateCandidateParser` holds a `DateCandidateParser`.
While they are `null`, the features in "Turned off for now" stay off.

### Safety checks in app code (not part of the student's rules)
- Archived batches are never given to the rules.
- The swap rules get the batches in a list that can't be changed. The screens and the swap save share one planner object, so it is called for one plan at a time.
- The screens and the daily reminder check (which runs in the background) share one DatePolicy object in the same way, so it is called for one list at a time.
- A proposal is not shown (`RuleRunner`) when:
  - it names a batch that wasn't given, or the same batch twice
  - either batch is medicine, a document, or a subscription: only the categories the household buys again (food, water, first aid supplies, cosmetics, and Other) are swapped, so a category added later stays out too (until 2026-10-22 evening: either batch isn't FOOD)
  - the kit batch isn't in a kit (Emergency kit or First aid location), or the pantry batch isn't in a Pantry location
  - the two batches are different products
  - either batch lacks a complete date after today, so food dated today or earlier is never sent to the pantry and never put into a kit
  - a sample batch is paired with a real one
  - the quantity is below 1, or more than a batch has left after the proposals already kept

  Which pantry batch is a good replacement (later date, replacement days) is decided only by the swap rules, and `RotationPlannerTest` tests those rules. This is only a safety net.
- The swap rules' notes give needs only for kit batches of the categories the household buys again (only FOOD until 2026-10-22 evening), never more than the batch's quantity, and at most one per batch (the largest note).
- Every batch of food, water, first aid supplies, cosmetics, or Other, in any place, with a complete date of today or earlier always gets a need for its whole quantity when the notes give less, with the reason "The date is today or has passed." (in a kit: "The date is today or has passed. No pantry use is recommended."). Until 2026-10-22 only kit food got this need. Medicine, documents, and subscriptions never get one.
- A confirmed swap is checked again in one transaction, with the swap rules run again (see "Swaps").
- If the rules throw an error or return null, the screen shows the error as text instead of crashing the app. During a swap save, an error saves nothing.

## Turned off for now
Nothing, since 2026-10-18. Until a feature's rules were connected, it showed a "Not available yet" explanation on the screen:
- Photo entry's possible dates: from 2026-10-12 until 2026-10-18, when DateCandidateParser was connected.
- Until 2026-10-18, when DatePolicy was connected:
  - Home's "Needs review" and "Medicine dates to review"
  - the status labels in Inventory and Batch details
  - the "Daily reminder" switch with its daily check
- Until 2026-10-18, when RotationPlanner was connected:
  - Home's "Recommended swaps" and the Review swap screen. Swaps then also needed shelf-stable food classes and answered food questions (until 2026-10-22 evening).
  - the Purchase list's replacement needs and suggestions

"Date needs review" under a batch was never on this list: it only reports how the date was entered.

## Wording rules
- The app never says that food is safe. It reports dates, statuses, and the household's own answers only.
- The app never suggests medicine substitutions, and a medicine stock record is never described as advice to take medicine.
- These rules apply to app text, sample data, and code comments (CLAUDE.md, rule 8).

## Sample data (a separate file since 2026-10-12)
- **Since 2026-10-22 (the student's request: "Remove the "Sample data" section under settings but retain the items that were added to the batch from sample data"), Settings has no Sample data card.**
  - Sample data can't be opened or started again, and the app makes no new made-up items. "Keep these items as my own" and "Remove old sample items" are gone too.
  - **The items kept before stay:** batches copied with "Keep these items as my own" are the household's own records, not marked "Sample item", so the change doesn't touch them. Nothing in the database changed, and no file was deleted.
  - **Old sample items** (see the end of this section) stay too, still marked "Sample item". Home's banner about them no longer says "Remove them in Settings."
  - **Where sample data was still open** when this version was installed, its label and "Close sample data" still work on every screen, and Planning settings still says "Sample data has its own numbers. Yours don't change."
  - Home's getting-started message no longer says "or try sample data in Settings".
  - Unchanged, but no longer reached from any screen: the code that opens, keeps, and removes sample data (`DataSets.prepareSample`, `switchToSample`, `keepSampleAsHousehold`, `ReadyShelfRepository.replaceWithSampleData`, `copyFromSampleData`, `removeSampleData`, `observeSampleCount`, `SampleData`) and its tests.
  - The rest of this section describes sample data as it worked until 2026-10-22.
- Settings → "Open sample data" showed made-up items in place of the household's own records, so the app could be tried or shown to others (DEMO.md). A dialog asked first. Nothing was made unless the household tapped it.
- **A separate file:**
  - The sample data is its own Room database, `readyshelf-sample-data.db`, with the same tables (version 3), and it has its own planning settings (`sample_settings`).
  - While it is open, nothing is written to `readyshelf.db`, and the household's records are not shown. The only exception is "Keep these items as my own" (see below), which the household chooses after a dialog.
  - The choice is kept in `data_mode`, so it lasts when the app is closed or the phone restarts.
- **Opening, and "Start sample data again":**
  1. In one transaction on the sample file: every row is deleted, then the three starting locations and the sample items are added, dated from the phone's local date that day (`LocalDate.now()`). The phone's clock is never changed.
  2. The sample planning settings start again at 30 and 180.
  3. Only then does the app switch. It closes every screen and opens Home again, so each screen reads and saves one set of data for its whole life.
  4. If anything fails, nothing switches: "Sample data didn't open. Nothing changed."
- Only a file whose name ends with `-sample-data.db` can be emptied (`AppDatabase.isSampleDataFile`), so `readyshelf.db` never can.
- **The label:** while the sample data is open, every screen shows "Sample data: made-up items, apart from your records." under its title. It doesn't scroll away, and it wraps at large text sizes. Each made-up row is also marked "Sample item".
  - Since 2026-10-19 (evening; the student found closing "confusing and complicated" and asked for the button under the words): a "Close sample data" button sits under the label's words, at the end, outlined in the label's colors (`SampleDataClose`, through `ScreenSetup`). So sample data can be closed from any screen.
- **The items.** Every row is marked as sample. Dates are counted from the day the sample data is opened.

| Batch | Place | Amount | Date | Answers |
|---|---|---|---|---|
| Granola bars (Oat and honey, 1.5 oz, unit bars; food class Shelf-stable food) | Earthquake kit | 6 | Best by, today + 20 days, confirmed | Dry or boxed; sealed Yes, shelf-stable Yes, damaged No, storage concern No |
| The same granola product | Pantry | 6 | Best by, today + 240 days, confirmed | the same |
| The same granola product | Pantry | 3 | Only printed text: the month and year 9 months ahead, for example "07/2027"; not confirmed | the same, so only the incomplete date keeps it out of swaps |
| Canned beans (Black beans, 15 oz, unit cans; Shelf-stable food), with no pantry batch of the product | Earthquake kit | 4 | Best by, today + 14 days, confirmed | Canned; Yes, Yes, No, No |
| Pain reliever tablets (24 tablets, unit bottles; Medicine) | First aid box | 2 | Expires, today − 10 days, confirmed | none (not food) |

  - The answers are made-up records. DueWise does not check or judge food, and gives no medicine advice.
  - With the rules connected (2026-10-18), the swap rules propose a swap of the two dated granola batches, the canned beans become a replacement need (a purchase suggestion until 2026-10-22), and the medicine date was listed under "Medicine dates to review" (since 2026-10-22 it counts in the matching status tile on Home instead). This was checked on the emulator, and `RotationPlannerTest` and `DatePolicyTest` check it with the sample dates. The app's own checks with preset answers are tested in `SampleDataTest`.
- **Everything else works on the sample data as usual:** adding, editing, stock changes, and new locations.
  - A batch added by hand goes only into the sample file.
  - Such a batch is not marked sample, so it is never paired with a made-up batch in a swap (see "Safety checks in app code").
  - Sample products are not offered on the batch form.
  - Sample needs can't be added to the purchase list.
- **Closing** ("Close sample data", under the label of any screen, and until 2026-10-22 also in Settings' card, after a dialog): the app switches back to the household's records and opens Home again. Then the sample file is emptied. If emptying fails, the next opening starts over anyway.
  - The dialog (since 2026-10-19 evening): "Close sample data?", "Your records come back. Made-up items and changes to them are deleted.", [Cancel] [Close]. It closes with its screen, for example when the phone rotates.
  - Then Home says "Sample data is closed. Your own records are shown." If the choice can't be saved, nothing switches, and the screen says "Sample data didn't close. Try again."
- **Settings' buttons while it is open** (from 2026-10-19, later that day, until the card was removed on 2026-10-22): "Close sample data", the way back to the household's own records, is the main (tonal) button and comes first. "Start sample data again" is a quieter outlined button under it, because it deletes changes made in the sample data. Then the keep note and "Keep these items as my own", as before. Until then "Start sample data again" was the main button and "Close sample data" came second.
  - Since that evening the intro ("Try DueWise with made-up items. Your records don't change.") shows only while sample data is closed; while it is open, the status box ("Sample data is open. Changes stay in it.") takes its place. The dialogs' words are shorter too, with the same meaning.
- **"Keep these items as my own"** (added 2026-10-18, at the student's request; shown only while sample data was open; removed with Settings' Sample data card on 2026-10-22, and the items it copied stay). After a dialog that says what is copied and that it can't be undone, everything in the sample data becomes the household's own records:
  - **What is copied** (`ReadyShelfRepository.copyFromSampleData`, in one transaction on `readyshelf.db`):
    - Every batch, archived ones too: the made-up ones and those added by hand.
    - Every value is copied as it is, also the made-up dates, their confirmation, and the made-up food condition answers. The student chose this over resetting them for a check.
    - Each copy is no longer marked "Sample item".
    - Each copy keeps its history and gets one more entry: "Copied from the sample data into your own records."
  - **Places:** a place with the same name, ignoring capital letters, is used, and so are the household's three starting places. Other places are added.
  - **Products:** a product with the same details is used and keeps its own food class. Other products are added, no longer marked sample.
  - **The purchase list:** copied too. An entry the household already has for the same product and kit is kept, and the copied one is left out.
  - **Swap receipts:** not copied. Each batch's history already says what was swapped.
  - **What doesn't change:** nothing the household already had changes. If anything fails, nothing is copied, sample data stays open, and the screen says "Nothing was copied. Sample data is still open."
  - **Only one way:** only a sample-data file can be copied, and only into a file that is not one.
  - **Then:**
    1. The sample file is emptied right away, so the same items can never be copied twice.
    2. Sample data closes, as with "Close sample data".
    3. The screen says, for example, "5 batches are now in your own records."
  - **From then on:** the copies count everywhere like the household's other records: the daily reminder, the widget, the Home banner, swaps, and the purchase list. Until 2026-10-22, "Open sample data" still worked later, with fresh made-up items. The copies stay after the card's removal.
  - **Tests:** `KeepSampleDataTest` (emulator only).
- **Old sample items:**
  - Before 2026-10-12, "Load sample data" added made-up items to the household's own records; that button no longer exists.
  - When such items exist, Home shows a banner: "Your records include old sample items, marked “Sample item”." Until 2026-10-22 it added "Remove them in Settings.", and Settings showed how many there were and "Remove old sample items". That button deleted only sample batches, their history, and the sample products no batch used; if the phone's database reported an error, nothing changed: "Sample items weren't removed. Nothing changed."
  - Since 2026-10-22 these items stay (the student asked to keep the items that came from sample data). Like before, they never cause a reminder, can't go on the purchase list, and are never paired with the household's own batches in a swap.

## Status labels and batch cards (added 2026-10-18)
The student chose these from Claude Code's suggestions for a clearer look (AI-USE.md).
- **Batch cards:** every list of batches shows each batch on its own card. That is Inventory and the Purchase/Renewal list's "Replacement needs" and (since 2026-10-24) "Renewal" (and Home's review lists until 2026-10-22). A card can have up to two buttons (since 2026-10-24: Renewal's "Renewed" and "Discard").
  - A card starts with a round icon for the category: a fork and knife (Food), a drop (Water), a medicine bottle (Medicine), a first aid kit (First aid supplies), a lotus (Cosmetics), a page (Documents), two circling arrows (Subscriptions), or a triangle, a square, and a circle (Other). The lotus, the page, and the arrows came on 2026-10-19. Other had a box until 2026-10-22; the box now means Inventory.
  - Then come the batch's lines as before.
  - TalkBack reads the category word with the icon, and the whole card as one item.
  - Cards replaced the divider lines between rows.
- **Status labels:** in the cards and on Batch details, the status is a rounded label with its word, an icon, and a color:

| Status | Icon | Color (the label, also the circle on Home's tile; its words, also the tile's icon; the tile's number) |
|---|---|---|
| Date passed | a solid "!" in a circle | red #B23A2B with white words; number #B23A2B |
| Date is today | a solid calendar with today marked | brass #B8914F with words #1D1D1C; number #82622A |
| Near date | a solid clock (a thin outline clock until 2026-10-22) | cream #F6EFE2 with dark brass words #82622A; number #1D1D1C |
| Date needs review | a solid question mark in a circle | pale blue #D8E2FF with words #001A41 (it has no tile) |
| Later date | a plain calendar, as an outline | gray (the theme's) |

- **The words carry the meaning:** the icon and the color only help.
  - TalkBack reads "Status: Near date".
  - "Later date" gets no green and no check mark, because DueWise never says that food is safe.
  - The status colors:
    - since 2026-10-23 (the student's design B1): the colors of Home's tiles, so each status looks the same on Home and in the lists. The most urgent is the strongest: a red label with white words, then brass, then cream. "Date needs review" keeps its pale blue, and "Later date" is the theme's gray (#E1E1DF with words #5F5F5C). Words on a label 4.89:1 or more, a tile's number on its white card 5.63:1 or more (TEST-RESULTS.md 40).
    - on 2026-10-22 (the light app), made with Google's Material color tools from the same hues as before: a pale color (tone 90) for the label, a dark color (tone 10) for its words, and a strong color (tone 40) for the tiles (red #B62320, orange #8B5018, gold #735B19, blue #2B5DAC); words on a label more than 13:1 contrast, a tile's number on its card more than 5:1
    - 2026-10-18 to 2026-10-21 (the dark app): a dark label (#93000A, #6E3900, #594400, #004493) with light words, and bright tile colors (#FFB4AB, #FFB77D, #F1C02C, #ADC6FF); more than 7:1 and 8:1
  - Before 2026-10-18 evening, "Date passed" was a bright red label and "Date needs review" only had an outline. From then until 2026-10-21 every label was a dark color with light words; on 2026-10-22 every label was a pale color with dark words; since 2026-10-23 the labels have the tiles' colors (see above).
  - The icons are sized from the text, so they grow with the phone's font size.
- **Tags:**
  - "Archived" is an outlined tag with an archive icon. Until 2026-10-18 it looked the same as "Sample item".
  - "Sample item" now has rounded corners.
- **The icons** are Google's Material Symbols in the Rounded style (Apache License 2.0; see THIRD-PARTY-NOTICES.md), since 2026-10-22. The student chose them from four versions on a preview page: Now, a smallest change, Rounded, and Outlined. Until then they were Google's older Material Icons, plus a clock and a calendar from Material Components.
  - The statuses that need attention ("Date passed", "Date is today", "Near date", "Date needs review") and the warning sign are solid. Every other icon is an outline, so what needs attention stands out.
  - On Home no picture stands for two unrelated things. (The first aid kit is both the category First aid supplies and the place type first aid kit.) The box means only Inventory (and the amount on Batch details), and the shelf only the reminder notification. The pantry has Material's shelves, other places a map pin, and the category Other a triangle, a square, and a circle. Settings' Reminders card has a bell. "!" still appears on both "Date passed" and the error boxes.
  - No check marks and no "×" on statuses: DueWise never says that food is safe, and "×" looks like "close" or "throw away".

## Messages and More about this (added 2026-10-18)
The student chose these from Claude Code's suggestions (AI-USE.md).
- **Three kinds of message boxes:**
  - **Notes:** a rounded quiet box with no sign. For example "Nothing needs review right now." or the medicine notes.
  - **Warnings:** pale brass with a dark brass warning sign (gold until 2026-10-23). They are the kit warnings "Replacement may be needed…" on the batch form's review and on Change stock, the camera explanations on Label photos, and (since 2026-10-19, later that day) Settings' notifications line while the daily reminder is on but can't appear (`NoticeLook.warning`; see "Reminders").
  - **Errors:** red with an error sign. For example "Not saved: …", "Nothing moved: …", and a missing batch (until 2026-10-21, "Nothing was saved, because…" and "Nothing was moved, because…").
  - Boxes that show a note most of the time switch to the error look when the rules stop with an error (`NoticeLook`, through `MessageText`).
  - Until 2026-10-18 errors looked exactly like notes. The words always say what happened, so color and sign are never the only signal.
  - **When the rules stop with an error** (since 2026-10-19 evening): the box says "Can't show this: the rules stopped with an error. Your records didn't change." (shorter since 2026-10-21). Before 2026-10-19 evening it added the error itself, for example "java.lang.IllegalStateException: …", which means nothing to a household. The details still go to the phone's log, as before (the date rules' and swap rules' screens log them). Batch details says the same sentence in place of the status.
  - Notes that the data fills in (Home's swaps message, the Purchase/Renewal list's notes) are hidden until the data arrives, so no empty box flashes while a screen opens (since 2026-10-19 evening).
- **More about this:** a button under a short text that shows or hides a longer explanation below it. It changes to "Show less", and TalkBack hears "Collapsed" or "Expanded". A rotation keeps it open or closed.
  - **Settings → Reminders:** the summary "Daily, when dates are near or passed. Never names medicine." stays in view (since 2026-10-21; from 2026-10-19 evening it was "About once a day when a date is near, today, or passed. Never names medicine."; before that it was "DueWise can remind you about once a day when a date has passed, is today, or is near. The reminder names up to two items, but never medicine.", and before 2026-10-19 its last sentence was "The reminder never names items."). Five short texts fold, at the end of the card: how the reminder looks, the lock screen, why it can come late, how to allow notifications, and what "Show reminder now" does ("Send a test reminder" until 2026-10-23; that note was in view until 2026-10-19 evening).
  - **Review swap → "Why the replacement qualifies":** a one-line summary and the planning sentence stay in view; every rule with the Settings numbers folds, and since 2026-10-19 "What you recorded" folds with them.
  - **Review swap → "After the swap"** (since 2026-10-19): the Before/After table stays in view; the details for each place fold.
  - **Home → "Recommended swaps":** since 2026-10-21 (the student's choice) only "More about this", with no note above the cards. From 2026-10-19 a short note stayed in view and the rest folded (see "What the app does with the answers").
  - **The batch form → Food condition** (until 2026-10-22 evening, when the form lost its food section): "Your own records: DueWise doesn't check or judge food." stayed in view; the swap rules for the answers folded.
  - **Safety notes never fold.** That covers the medicine notes, the "for review" note, "planning choice, not a safety guarantee", and, until the food section went on 2026-10-22 evening, "doesn't check or judge food". Since 2026-10-21 (the student's choice) Home has no note above its swap cards; "planning choice, not a safety guarantee" stays in view on Review swap and in Settings. Since 2026-10-22 Home has no medicine list, so Home's medicine note went with it. The stock screens keep "This only records your stock. It isn't medicine advice."
- **No code names on screen:** sentences that named "(DatePolicy)", "(RotationPlanner)", or "(DateCandidateParser)" now say only "the date rules", "the swap rules", or "the date-candidate rules".
- **Save messages (since 2026-10-19, the student's choice):** after each save, a short message appears at the bottom of the screen for a few seconds. It is a Material Snackbar (`Confirmations`). It sits above the floating "Add batch" button where there is one (Inventory; Home has none since 2026-10-23, so there it is at the bottom of the screen), or (since 2026-10-19 evening) above the bar at the bottom of the batch form. TalkBack reads it, and Android keeps it longer for people who use TalkBack. It only says what was recorded.
  - **The messages:**
    - "Batch added." or "Changes saved.", or "No changes to save." after an edit that changed nothing (since 2026-10-19 evening)
    - "Recorded as used: 3 bars." and "Recorded as discarded: …"
    - "Moved 6 bars to Pantry." and "Batch archived."
    - "Swap recorded."
    - "Planning settings saved." and the reminder messages in Settings
    - "Sample data is closed. Your own records are shown." ("Sample data is open." too, until 2026-10-22)
    - the purchase list's "Added to …", "Changed on …", and "Removed from your purchase list: …"
  - **When the screen closes:** Add batch, Edit, Record used or discarded, Move, Archive, Review swap, and the sample-data switch close right after saving. Their message appears on the screen underneath, for example Batch details or Home. After "Save and add another" (since 2026-10-19), "Batch added." appears on the new empty form.
  - **The purchase list:** a change that was not made (for example "already on your list") still stays explained in the box at the top.
  - Before 2026-10-19 some of these were Android's small Toast messages, and some saves gave no message at all.

## Short texts of 2026-10-21
The student found the app wordy and asked for every page's descriptions to be short, concise, and to the point, like Inventory and the review lists, which have none. Claude Code wrote the new words (AI-USE.md).
- **What changed:** 164 texts in `strings.xml`: descriptions, notes, field hints, empty-list texts, messages, and dialog texts. Together they went from 2,780 words to 1,643. Each keeps its meaning and every value it shows.
- **How:**
  - One idea, said once. A text says what to do or know, not how the app works inside.
  - Safety sentences stay in view, in fewer words: "Your own records: DueWise doesn't check or judge food.", "“For review” means you check this food yourself. DueWise doesn't check it.", "Replacement days are a planning choice, not a safety guarantee.", "It gives no medicine advice.", "It isn't medicine advice.", and "Never names medicine." (see "Wording rules").
  - Errors start with what happened: "Not saved: …", "Nothing moved: …", "Not changed: …".
- **Not changed:** the words on buttons and headings; the history entries, which are saved with each change, so new and old entries would read differently; the notifications and the widget; and the reason "The date is today or has passed. No pantry use is recommended." (see "Safety checks in app code").
- **Measured on the emulator** (TEST-RESULTS.md 29), with the emulator's own records: the words in the descriptions that changed, before → after. The Add batch form for Food 195 → 82, Label photos after a refused camera permission 83 → 44, Settings 81 → 54, Review swap 75 → 44, Purchase list 50 → 18, Home 18 → 12, Batch details 14 → 9, and Inventory 10 → 8. Record used had no description. In all, 526 → 271.

## UI review of 2026-10-19
The student asked Claude Code to "inspect the app as a whole and evaluate it against best practices of UI and suggest improvements". Claude Code read the screens' code and layouts (no screenshots), graded the app (accessibility strong; visual consistency good; navigation needs work; words mixed; forms good; layout mixed; feedback mixed; motion missing; platform good), found seven problems in the code, and offered twelve improvements. The student chose these five (the ones Claude Code recommended):
- **Small fixes:** "No changes to save." after an edit with no change; "Leave without saving?" on the batch form; plain words when the rules stop with an error; the search key closes the keyboard; a progress bar while label photos are read; the "can be read two ways" note's text size; no empty boxes or blank form before the data arrives; "How much?" and "Type:" read with their answers; fewer live regions on the photo review.
- **The same words everywhere:** "Near date" on Home's tile again; "swap proposals" instead of "automatic swaps"; Home's empty text names more kinds of items.
- **Inventory's filters folded** under "Filters", with the chosen ones as chips (see "Screens").
- **The main button always in view:** a bar at the bottom of the batch form, Change stock, and Review swap.
- **"Add batch" shrinks** to a round "+" while the screen scrolls, and always with large text.

Not chosen (suggestions only): a welcome card on an empty Home, "item" instead of "batch" on screen, reviews as rows, shorter form texts (done on 2026-10-21; see "Short texts of 2026-10-21"), the same controls everywhere (Move's drop-down, Change stock's amount), motion, Material's date picker; and later: Settings as an icon in Home's top bar or a bottom navigation bar, a light theme choice, wider screens, a camera screen with the shutter on the picture, undo, lists that reuse their rows, and headings on Android 8.

## Accessibility
- Touch targets are at least 48 dp. List cards are at least 72 dp tall, and the whole card can be tapped.
- Inventory's filter chips (location, category, and status) have 48 dp touch targets and are read by TalkBack like radio buttons, and so are the two halves of the date-order switch. A chosen chip shows a check mark, so the choice is not shown by color alone.
- The batch form's chips (since 2026-10-19) are the same kind of chip. On the chosen category, the check mark takes the place of the icon. Each food class was read as "Food class: Shelf-stable food" (until 2026-10-22 evening). The − and + buttons are read as "One less" and "One more", and the new amount ("3 cans") is their state, for TalkBack to read after a tap. Once a date is chosen, the date button is read as "Date: 26 Oct 2026. Change date". With large text the chips wrap onto more lines (checked at font scale 2.0 on the emulator). Since 2026-10-22 the label type line's button is read as "Change label type". The label type chips and "What is printed?" are each a pane with a title ("Label type (next to the date)", "What is printed?"), so TalkBack names the part that opens, because the button that opened it goes away. The app doesn't move TalkBack's focus itself; lint warns against forcing it. The food condition tick (2026-10-22, until the food section went that evening) was one check box, read with its words and "checked" or "not checked". The label row, the tick, and "No full date on the package?" wrap at font scale 2.0 (emulator).
- Home's status tiles can be tapped (they are much larger than 48 dp), and TalkBack names what a tap does: "Double-tap to show these batches in Inventory". Until 2026-10-22 the banner's "Show the review list" tile was read as a button, with "Collapsed" or "Expanded".
- Home's shortcut tiles are tiles of Home's grid (since 2026-10-19), the same size as the status tiles: about 92 dp tall, and larger with large text, one per row. Each one's label is its word. Since 2026-10-23 Add batch is one of them, read as "Add batch"; alone in the last row it is twice as wide. Inventory's floating "Add batch" button (Home's too until 2026-10-23) is 56 dp tall, and its label is its word. Since 2026-10-19 evening it shrinks to a round "+" while the screen scrolls down (and always with large text); shrinking only hides its words on screen, so TalkBack still hears "Add batch", and a long press shows the words as a tooltip. The empty-list texts say "+ Add batch", which matches Home's tile (a "+" and the words) and the round button. Save messages at the bottom are read by TalkBack (see "Messages and More about this").
- Inventory's chosen-filter chips (since 2026-10-19 evening) are read as "Filter: Near date", with "Double-tap to remove this filter"; their ✕ is read as "Remove Near date". "Filters" is read with "Collapsed" or "Expanded".
- The bar at the bottom of the batch form, Change stock, and Review swap (since 2026-10-19 evening) holds one full-width Material button (at least 48 dp tall), read after the screen's content.
- On Change stock, each answer of "How much?" is read with its question, for example "How much? All (4 cans)", and so are the types in the New location dialog ("Type: Pantry") (since 2026-10-19 evening).
- Status is always written as text, never shown by color alone ("Date needs review", "Archived", "Sample item", the "Sample data" label, and Home's status tiles). Status labels add an icon and a color to the word (see "Status labels and batch cards").
- Text sizes use sp, and text wraps instead of being cut off. Every screen scrolls, so large system text still fits. The home-screen widget's text shrinks to fit instead, because a widget can't scroll.
- Every input has a visible label. Errors appear on the field, or right under a drop-down list or group of choices, and the screen scrolls to the first one.
- "Close sample data" under the "Sample data" label (since 2026-10-19) is a full-size Material button (at least 48 dp tall) whose label is its words, read after the label's sentence.
- Each answer of a food condition question was read by TalkBack with its question, for example "Sealed: Yes", until 2026-10-22; that day the form had one tick for the three answers (see "Simpler form (2026-10-22)"), and since that evening it has no food section.
- Section titles are marked as headings for screen readers, and the back arrow has a spoken label. So does the home icon that replaces it on Inventory after a status tile ("Home", since 2026-10-22).
- A status list's header (since 2026-10-23) is read as one heading, for example "Date passed: 3 batches". It is not a button, so TalkBack doesn't offer a tap.
- Colors come from the app's Material 3 theme in the student's design B1, "Brass highlights" (see "Name and icon"; "Graphite light" on 2026-10-22, blue and always dark until 2026-10-21). It is always light and keeps WCAG AA contrast or better for every text (TEST-RESULTS.md 40). The only other fixed colors in screens are the status colors (see "Status labels and batch cards"), the warning box, Home's pine band and logo, and Inventory's "Add batch".
- Empty lists explain what to do next.

## Privacy
- No INTERNET permission, no cloud backup, and no phone-to-phone transfer (see TOOLCHAIN.md).
- No accounts, analytics, ads, or server.
- Label photos are read on the phone, by Tesseract. Since 2026-10-12 DueWise takes them with its own camera screen (CameraX), so it has the CAMERA permission. It asks for it only on the Label photos screen, and everything else works without it. Photos stay in the app's private cache folder and are deleted after reading (see "Photo entry"). Since 2026-10-23 no button opens that screen, so DueWise doesn't ask for the camera. The CAMERA permission is still declared, and a phone that already allowed it keeps that answer.
- Google ML Kit is not used for reading labels. Its official terms say the ML Kit APIs "send metrics about the performance and utilization of the APIs in your app to Google", which CLAUDE.md rules 6 and 7 don't allow. The student chose Tesseract again on 2026-10-12 (see TOOLCHAIN.md).
- Permissions (checked in the built app on 2026-10-12): CAMERA (photo entry only), POST_NOTIFICATIONS, which the app asks for only when the household turns on the daily reminder or taps "Show reminder now" ("Send a test reminder" until 2026-10-23) (Android 13 and newer), plus WAKE_LOCK and RECEIVE_BOOT_COMPLETED, which WorkManager uses for the daily check, and AndroidX's app-private DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION. ACCESS_NETWORK_STATE (asked for by WorkManager and by CameraX's media3-common library) and FOREGROUND_SERVICE are removed in the manifest. There is no exact-alarm permission. A camera is not required to install the app. Checked again on 2026-10-18, after the home-screen widget was added: the same list.
- The home-screen widget never names items. Until 2026-10-23 it gave no counts; since then (the student's choice) it gives one number, Home's status tiles added up. Until 2026-10-23 the test reminder never named items either; since then "Show reminder now" shows the daily reminder, with its names.
- The daily reminder (since 2026-10-19, the student's choice) names up to two items that need review and says how many others there are, but never names medicine. Its lock-screen version has only generic words. The phone's lock-screen setting for sensitive content decides whether a locked phone shows the names (see "Reminders").
- Notifications are not shown on a connected watch. Counts by status appear only inside the app, in the banner at the top of Home. The widget shows only the household's own records and is never shown on the lock screen.
- **One total outside the app (since 2026-10-23, the student's choice; until then no count appeared outside the app):** Home's status tiles added up shows on DueWise's icon while the reminder is shown (as a dot or a number, as the home screen decides) and in the widget. Anyone who sees the home screen can read it. It never names items and is never split by status, and the reminder's lock-screen version has no number.

## Not in this step (possible follow-ups)
- Phone checks of the connected rules (all connected on 2026-10-18):
  - swaps, the Review swap screen, and replacement needs with real batches
  - a real daily reminder (the "Daily reminder" switch works since DatePolicy was connected)
  - the reminder that pops up with a sound, the Home banner, and the home-screen widget (added 2026-10-18; checked on the emulator only)
  - the names in the daily reminder, and how the phone's lock screen shows them (added 2026-10-19; checked on the emulator only)
- Rewriting a shown daily reminder when its names change during the day, for example after an item it names is recorded as used. Done on 2026-10-23, together with the number on the app icon (see "Reminders").
- The number on the app icon (since 2026-10-23):
  - a check on the student's phone: the dot, and whether the Moto home screen's long-press menu shows the number
  - refreshing a shown reminder and the widget right after the planning settings are saved
  - a line about the number in Settings' "More about this"
- The reminder channel's description still says "at most once a day", although "Show reminder now" can show the reminder again that day (known since 2026-10-23).
- A line in Settings when Do Not Disturb is on (reminders then arrive without a sound or a pop-up).
- A "Check now" button that runs the real daily check, so a reminder can be tested without waiting for 09:00. Partly done on 2026-10-23: "Show reminder now" shows the real reminder at once, but it isn't recorded, and the background check still runs only when Android starts it.
- Home-screen widget: a refresh at midnight and right after the planning settings are saved, dynamic colors (Android 12 and newer), and a preview image for Android 11 and older.
- Sample data whose needs can be added to its own purchase list (sample needs can't be added now, so "Record bought item" can't be shown with sample data). Moot since 2026-10-22, when Settings' Sample data card was removed.
- Removing the sample-data code that no screen reaches since 2026-10-22 (opening it, "Start sample data again", "Keep these items as my own", "Remove old sample items", `SampleData`) and its tests, or a way to turn old sample items into the household's own items.
- A note on Home when a daily reminder is tapped while the sample data is open (the reminder is about the household's own records).
- Refreshing a screen that stays open past midnight (today it refreshes when it comes back to the front; a swap save always checks with the current date).
- Using DatePolicy's NEEDS_REVIEW for the "Date needs review" line, instead of the app's own `DateEntry` check.
- Deleting a batch, and un-archiving one.
- Editing a product's own details (name, variant, package size, unit), and merging two products.
- Product texts without "no variant" and "no package size" (suggested on 2026-10-22, when the form stopped asking for them; not done). `BatchText.productFacts` would say "Unit: cans." on the form and on Batch details, and `BatchText.productChoiceDetails` would change the form's product list, Review swap, and the Purchase list; new "Product changed…" history entries would use the shorter text too.
- On the batch form, the food class as one line ("Food class: Shelf-stable food", with "Change") for a product that already has one (suggested on 2026-10-22; not done). Moot since that evening, when the form lost its food section.
- Supplements: "Supplement Facts" on a label photo gives the category clue Other (`CategoryGuesser`; a Medicine clue such as "capsules" still wins), so since 2026-10-22 evening a supplement saved as Other can be swapped and get a replacement need. Moving that clue to Medicine would keep supplements out of both (suggested on 2026-10-22; not done).
- The reason "The date is today or has passed. No pantry use is recommended." for a kit item that isn't food (water, first aid supplies, cosmetics, or Other): "pantry use" fits food best (suggested on 2026-10-22; not done).
- Joining a moved batch with a batch that already has the same product and date at the new location.
- Renaming or deleting locations.
- Purchase/Renewal list items that the household adds by hand.
- Renewal (since 2026-10-24; suggested that day, not done):
  - listing documents and subscriptions earlier, while they are "Near date", so there is time to renew (the student chose "today or earlier")
  - a date picker for **Renewed** that starts nearer the likely new date (it starts at tomorrow)
  - a document or subscription with nothing left is not listed, but Home's "Date passed" tile still counts it until it is archived on Batch details
- Headings that TalkBack announces on Android 8 too (`ViewCompat.setAccessibilityHeading`; `android:accessibilityHeading` works from Android 9).
- Removing photo entry for good (its button is hidden since 2026-10-23): the Label photos screen, CameraX, Tesseract with its English data, the CAMERA permission, the photo texts, and their tests. Tesseract's parts take about 22 MB of the 36.7 MB debug APK (measured on 2026-10-23), and DueWise would have no camera permission. Not done: the student asked only to hide the button.
- Photo entry (since 2026-10-12): a date-only photo for "Record bought item", photo entry while editing, a reading pass that handles dot-matrix print, remembering items by barcode, "Take photo" above the hint text at very large font sizes (now it needs one short scroll at font scale 2.0), a smaller stand-alone month reader in place of `PrintedMonthReader`, a photo step in DEMO.md, and an in-app open-source licenses screen.
- Quiet hours, so a check that runs late at night waits until morning before posting (more useful since 2026-10-18, now that reminders make a sound).
- The batch form and the new categories (since 2026-10-19):
  - a "Renews" label type for subscriptions
  - label-photo category clues for cosmetics and documents (a cosmetics label's "INGREDIENTS" is a Food clue, unless the category was chosen first)
  - words on Batch details that fit documents and subscriptions (for example "Record used")
  - helper examples that change with the category (the name's example is always "Canned beans")
  - the new categories in `RotationPlannerTest`'s list of categories that are not food (done on 2026-10-22 evening: its tests name every category, swapped or not)
- Checking the whole batch, not only its quantity, before a stock change or an Edit is saved (TEST-RESULTS.md, D-2). The other open items from the review are listed there too.
- From the UI review of 2026-10-19 (see that section): the suggestions the student didn't choose, and one limit of the new bottom bar: in landscape with font scale 2.0 and the sample-data label, about 100 dp of the batch form shows between the label and the bar (it still scrolls). A bar that scrolls with the form on very short screens would give that room back.
