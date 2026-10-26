# THIRD-PARTY-NOTICES.md — open-source parts inside DueWise

DueWise includes the open-source software below, used under its own license. Each license applies to its own part only.
Versions are recorded in TOOLCHAIN.md. AndroidX and Material Components libraries (Apache License 2.0) are listed in TOOLCHAIN.md.

## Label scanning (added 2026-10-04)

| Part | Version | License | Official source |
|---|---|---|---|
| Tesseract4Android (Java wrapper and build) | 4.9.0 | Apache License 2.0 | https://github.com/adaptech-cz/Tesseract4Android |
| Tesseract OCR engine (inside Tesseract4Android) | 5.5.1 | Apache License 2.0 | https://github.com/tesseract-ocr/tesseract |
| Leptonica image library (inside Tesseract4Android) | 1.85.0 | Leptonica license (BSD-2-Clause style) | http://www.leptonica.org/about-the-license.html |
| libjpeg (inside Tesseract4Android) | 9f | Independent JPEG Group (IJG) license | https://www.ijg.org/ |
| libpng (inside Tesseract4Android) | 1.6.48 | PNG Reference Library License version 2 | http://www.libpng.org/pub/png/src/libpng-LICENSE.txt |
| English language data `eng.traineddata` (tessdata_fast) | commit 87416418657359cb625c412a48b6e1d6d41c29bd | Apache License 2.0 | https://github.com/tesseract-ocr/tessdata_fast |

The Tesseract4Android README lists the bundled libraries and versions above.

### Required notices
- This software is based in part on the work of the Independent JPEG Group.
- The Apache License 2.0 text: https://www.apache.org/licenses/LICENSE-2.0
- The Tesseract4Android AAR contains no NOTICE file (checked on 2026-10-04).

## Photo entry (added 2026-10-12)

CameraX 1.6.2 (AndroidX, Apache License 2.0) and the AndroidX Media3 1.9.0 libraries it uses are listed in TOOLCHAIN.md. They also brought in the libraries below. The list comes from the resolved build (`:app:dependencies`, debugRuntimeClasspath), and each license is the one named in the library's own POM file (or its parent POM) in the Gradle cache, read on 2026-10-12.

| Part | Version | License (as its POM names it) |
|---|---|---|
| Guava (`com.google.guava:guava`) | 33.3.1-android | Apache License, Version 2.0 |
| Guava failureaccess | 1.0.2 | The Apache Software License, Version 2.0 |
| Dagger (`com.google.dagger:dagger`) | 2.59 | Apache 2.0 |
| `javax.inject:javax.inject` | 1 | The Apache Software License, Version 2.0 |
| `jakarta.inject:jakarta.inject-api` | 2.0.1 | The Apache Software License, Version 2.0 |
| AutoValue annotations (`com.google.auto.value:auto-value-annotations`) | 1.6.3 | Apache 2.0 |
| JSR 305 annotations (`com.google.code.findbugs:jsr305`) | 3.0.2 | The Apache Software License, Version 2.0 |
| J2ObjC annotations (`com.google.j2objc:j2objc-annotations`) | 3.0.0 | Apache License, Version 2.0 |
| Checker Framework qualifiers (`org.checkerframework:checker-qual`) | 3.43.0 | The MIT License |
| kotlinx atomicfu (`org.jetbrains.kotlinx:atomicfu`, `atomicfu-jvm`) | 0.28.0 | Apache-2.0 |
| Error Prone annotations (`com.google.errorprone:error_prone_annotations`, already in the app; now 2.28.0) | 2.28.0 | Apache 2.0 |

- `androidx.concurrent:concurrent-futures` 1.1.0 (AndroidX, Apache License 2.0) is only used for compiling; the app already shipped it.
- The MIT License text: https://opensource.org/license/mit

## Icons (added 2026-10-18; redrawn on 2026-10-22)

Since 2026-10-22 the app's icons are Google's Material Symbols in the Rounded style, used under the Apache License 2.0. The student chose this style from four versions that day (AI-USE.md). That covers every `ic_*.xml` in `app/src/main/res/drawable/` except the notification icon: the status, category, place, and tag icons, the icons and arrows on Home's tiles, the arrows on "More about this" buttons and in the swap picture, the plus on Home's Add batch tile and on Inventory's floating "Add batch" button, the icons of the Settings cards, the plus and minus on the batch form, and the home icon in Inventory's top bar. Each one is a vector drawable, and its comment names the original icon.

| Part | Files | License | Official source |
|---|---|---|---|
| Material Symbols, Rounded style, solid (fill 1), 24 px: "error", "today", "schedule", "help", "warning" | `ic_error.xml`, `ic_today.xml`, `ic_clock.xml`, `ic_help.xml`, `ic_warning.xml` | Apache License 2.0 | https://fonts.google.com/icons (SVG files downloaded from fonts.gstatic.com on 2026-10-22) and https://github.com/google/material-design-icons |
| Material Symbols, Rounded style, outline (fill 0), 24 px: "calendar_today", "inventory_2", "shopping_cart", "settings", "chevron_right", "expand_more", "expand_less", "add", "remove", "arrow_forward", "backpack", "medical_services", "restaurant", "water_drop", "medication", "spa", "description", "autorenew", "archive", "tune" | `ic_calendar.xml`, `ic_inventory.xml`, `ic_shopping_cart.xml`, `ic_settings.xml`, `ic_chevron_right.xml`, `ic_expand_more.xml`, `ic_expand_less.xml`, `ic_add.xml`, `ic_remove.xml`, `ic_arrow_forward.xml`, `ic_backpack.xml`, `ic_medical_services.xml`, `ic_restaurant.xml`, `ic_water_drop.xml`, `ic_medication.xml`, `ic_spa.xml`, `ic_description.xml`, `ic_autorenew.xml`, `ic_archive.xml`, `ic_tune.xml` | Apache License 2.0 | the same |
| Material Symbols, Rounded style, outline (fill 0), 24 px, new on 2026-10-22: "shelves" (the pantry), "location_on" (other places), "category" (the category Other), "notifications" (Settings' Reminders card), "home" (Inventory's top bar after a status tile; downloaded later that day) | `ic_shelves.xml`, `ic_location.xml`, `ic_category.xml`, `ic_bell.xml`, `ic_home.xml` | Apache License 2.0 | the same |

Only the shapes (path data) were copied; the color is white so that each screen can tint it. Material Symbols are drawn from y = -960 to 0, so each file moves the shape into its 960 x 960 viewport with a group. The notification icon `ic_notification.xml` was drawn for this app. "list_alt" (`ic_list_alt.xml`) was added for Home's "Show the review list" tile on 2026-10-22 and removed later that day, together with the tile. "dataset" (`ic_dataset.xml`), the icon of Settings' Sample data card, was removed later on 2026-10-22, together with the card.

From 2026-10-18 until 2026-10-22 these files held Google's older Material Icons in the filled style, also under the Apache License 2.0, downloaded from fonts.gstatic.com and https://github.com/google/material-design-icons on 2026-10-18 and 2026-10-19. The clock and the calendar were the icons of Material Components for Android 1.14.0 (`ic_clock_black_24dp`, `material_ic_calendar_black_24dp`, from https://github.com/material-components/material-components-android), under the same license.

## The letters of the DueWise logo (added 2026-10-18)

The DueWise logo itself is the student's (an AI draft the student edited; see AI-USE.md). Its name is set in Instrument Serif Italic. Home's top bar shows the logo as a vector (`logo_duewise_horizontal.xml`).

The logo kit had already converted the letters to outlines. Since 2026-10-21 the name has a capital "W": Claude Code converted it to an outline the same way, from the kit's own copy of the font (`font/InstrumentSerif-Italic.ttf`, with its `OFL.txt`). So the app contains only the shapes of the letters, not the font file.

| Part | License | Official source |
|---|---|---|
| Instrument Serif Italic (Copyright 2022 The Instrument Serif Project Authors), the letters of the logo's name | SIL Open Font License 1.1 | https://github.com/Instrument/instrument-serif |

## Not done yet
The app has no "open-source licenses" screen yet. If DueWise is shared as an app (not only as source code), add one that shows this information.
That is a follow-up in SPEC.md.
