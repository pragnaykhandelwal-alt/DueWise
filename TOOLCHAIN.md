# TOOLCHAIN.md — ReadyShelf tools and versions

Checked on 2026-10-04 by Claude Code. Every version comes either from the installed tool itself or from the official source listed.
To change a version: check its official page, change it, rebuild, and update this file in the same change.

## This computer and phone (observed)

| Tool | Version / value | How it was checked |
|---|---|---|
| Windows | 11 Enterprise 10.0.26200 | system info |
| Android Studio | Quail 4 \| 2026.1.4 Patch 1 (build AI-261.26222.65.2614.16379836) | Studio's `product-info.json` and log |
| JDK | JetBrains Runtime (JBR) 25.0.3, bundled with Android Studio: `C:\Program Files\Android\Android Studio\jbr` | `java -version` |
| Terminal JDK | User variable `JAVA_HOME` = the JBR path above; `%JAVA_HOME%\bin` is on the user Path | Windows user environment |
| Git | 2.55.0.windows.5 | `git --version` |
| Android SDK | `%LOCALAPPDATA%\Android\Sdk` | `local.properties` |
| SDK Platform | android-37.0 (android-36 is also installed) | SDK folder |
| SDK Build-Tools | 36.0.0 | SDK folder |
| SDK Platform-Tools (adb) | 37.0.1 (the latest on the release page) | `adb version` |
| Test phone | motorola moto g play – 2024, Android 14 (API 34), arm64-v8a | `adb shell getprop` |
| Emulator (AVD) | Pixel_10a: system image `android-37.2`, `google_apis_playstore_ps16k`, x86_64. Runs as `emulator-5554`. Used for migration and upgrade tests, so test APKs never go on the household phone (added 2026-10-11) | `~/.android/avd/Pixel_10a.avd/config.ini` |

## Project versions

| Item | Version | Why this one | Official source |
|---|---|---|---|
| Android Gradle Plugin (AGP) | 9.4.1 | Newest stable 9.4.x on Google Maven (9.5 is alpha only). Released with Studio Quail 4 Patch 1 | [AGP 9.4 release notes](https://developer.android.com/build/releases/agp-9-4-0-release-notes), [AGP/Studio compatibility](https://developer.android.com/build/releases/about-agp) |
| Gradle (wrapper) | 9.6.0 | AGP 9.4's minimum and default Gradle version | AGP 9.4 release notes; [Gradle releases](https://gradle.org/releases/) |
| JDK for Gradle | Java 25 | Set in `gradle/gradle-daemon-jvm.properties` by the template. Gradle 9.6 runs on Java 17–26. AGP 9.4 needs JDK 17 or newer | [Gradle compatibility](https://docs.gradle.org/9.6.0/userguide/compatibility.html), [Java versions in Android builds](https://developer.android.com/build/jdks) |
| compileSdk / targetSdk | 37 (Android 17) | The highest API level AGP 9.4 supports. Google Play requires 36 or higher for new apps | AGP 9.4 release notes; [Play target API](https://developer.android.com/google/play/requirements/target-sdk) |
| minSdk | 26 (Android 8.0) | `java.time` is built in from API 26. The phone is API 34. All libraries below need API 23 or lower | [Android 8.0 features](https://developer.android.com/about/versions/oreo/android-8.0) |
| Java language level | 11 | The template's `compileOptions`. The Android docs say to always set it explicitly | [Java versions in Android builds](https://developer.android.com/build/jdks) |
| androidx.appcompat:appcompat | 1.8.0 | Latest stable (minSdk 23) | [AppCompat releases](https://developer.android.com/jetpack/androidx/releases/appcompat) |
| com.google.android.material:material | 1.14.0 | Latest stable (minSdk 23) | [MDC-Android 1.14.0](https://github.com/material-components/material-components-android/releases/tag/1.14.0) |
| androidx.activity:activity-ktx | 1.13.0 | Latest stable. The template uses this artifact, which includes `androidx.activity` | [Activity releases](https://developer.android.com/jetpack/androidx/releases/activity) |
| androidx.constraintlayout:constraintlayout | 2.2.2 | Latest stable | [ConstraintLayout releases](https://developer.android.com/jetpack/androidx/releases/constraintlayout) |
| androidx.room:room-runtime + room-compiler | 2.8.5 | Latest Room 2.x, with Java `annotationProcessor` (see "Room 2.x is deprecated" below) | [Room releases](https://developer.android.com/jetpack/androidx/releases/room) |
| Room Gradle plugin (`androidx.room`) | 2.8.5 | Saves the database schema in `app/schemas/` for later migrations. The release notes say it sets up the "javac, KAPT, KSP" backends, so it works with the Java annotation processor. Added 2026-10-04 | [Room releases](https://developer.android.com/jetpack/androidx/releases/room) |
| androidx.lifecycle:lifecycle-viewmodel + lifecycle-livedata | 2.11.0 | Latest stable (June 17, 2026; minSdk 23). These are the Java artifacts the release page lists. Added 2026-10-04 | [Lifecycle releases](https://developer.android.com/jetpack/androidx/releases/lifecycle) |
| androidx.recyclerview:recyclerview | 1.4.0 | Latest stable (January 15, 2025). Needs compileSdk 35 or higher. Added 2026-10-04 | [RecyclerView releases](https://developer.android.com/jetpack/androidx/releases/recyclerview) |
| androidx.work:work-runtime | 2.12.0 | Latest stable (September 23, 2026). The Java artifact; `work-runtime-ktx` is for Kotlin. Needs minSdk 24, compileSdk 35, and AGP 8.6 or higher. It asks for Room 2.7.0 and Lifecycle 2.6.2, and Gradle uses the app's Room 2.8.5 and Lifecycle 2.11.0, so no app version went up. Added 2026-10-11 for the daily reminder check | [WorkManager releases](https://developer.android.com/jetpack/androidx/releases/work) |
| cz.adaptech.tesseract4android:tesseract4android | 4.9.0 | On-device text recognition for label photos: Tesseract 5.5.1 and Leptonica 1.85.0. Apache-2.0, no network code, no analytics. Hosted only on JitPack. Added 2026-10-04 | [Release 4.9.0 (2025-06-11)](https://github.com/adaptech-cz/Tesseract4Android/releases/tag/4.9.0), [README](https://github.com/adaptech-cz/Tesseract4Android) |
| `eng.traineddata` (tessdata_fast) | commit `87416418657359cb625c412a48b6e1d6d41c29bd` | English language data for Tesseract, the "fast" model (the tessdoc guide says "Most users will want tessdata_fast"). 4,113,088 bytes, Apache-2.0. Stored in `app/src/main/assets/tessdata/` | [tessdata_fast](https://github.com/tesseract-ocr/tessdata_fast), [Data files](https://tesseract-ocr.github.io/tessdoc/Data-Files.html) |
| androidx.camera:camera-core, camera-camera2, camera-lifecycle, camera-view | 1.6.2 | ReadyShelf's own camera screen for label photos (photo entry). Latest stable: the releases page lists Stable 1.6.2 (August 26, 2026) and only 1.7.0 alphas after it (its own Gradle sample uses an alpha, so it was not copied). The 1.6.2 AAR files need minSdk 23, `minCompileSdk=36`, and AGP 8.9.1 or higher. The only fix in 1.6.2 is for "a compilation crash on newer JDK versions", and this project compiles with JDK 25. Brings 25 more libraries at run time (see "Photo entry and CameraX"). Added 2026-10-12 | [CameraX releases](https://developer.android.com/jetpack/androidx/releases/camera), [CameraController](https://developer.android.com/media/camera/camerax/architecture) |
| androidx.concurrent:concurrent-futures | 1.1.0, **compileOnly** | Only so javac can compile against CameraX (see "Photo entry and CameraX"). The app already shipped 1.1.0 (released August 19, 2020) through androidx.core and CameraX, so nothing inside the app changes. The latest stable is 1.3.0 (July 16, 2025); it is not used, because it would compile against a newer version than the app ships. Added 2026-10-12 | [Concurrent releases](https://developer.android.com/jetpack/androidx/releases/concurrent) |
| junit:junit | 4.13.2 | JUnit 4 for local unit tests | Maven Central |
| androidx.test.ext:junit | 1.3.0 | Latest stable | [AndroidX Test releases](https://developer.android.com/jetpack/androidx/releases/test) |
| androidx.test.espresso:espresso-core | 3.7.0 | Latest stable | [AndroidX Test releases](https://developer.android.com/jetpack/androidx/releases/test) |

### Selected for later, not added yet

| Library | Version | Add it when |
|---|---|---|
| androidx.room:room-testing | 2.8.5 | Tried on 2026-10-11 and taken out again (see "Migration test without room-testing" below) |
| androidx.work:work-testing | 2.12.0 | Not added on 2026-10-11: the reminder tests use the real WorkManager on the emulator. Before adding it, check that it doesn't need newer versions of the app's libraries (see "Migration test without room-testing" below) |

RecyclerView, Lifecycle, and the Room Gradle plugin moved to "Project versions" on 2026-10-04, when the first screens were built.

### Newer versions that exist but are not used, on purpose

- **Android Studio Rabbit 1 (2026.2.1)** is the newest stable Studio. It supports the same AGP range (7.1–9.4) as Quail 4, so updating is optional.
- **Gradle 9.6.1** (patch) and **9.8.0** (latest) exist. We stay on 9.6.0, the version AGP 9.4 documents.
- **Room 3.x** (`androidx.room3`) is stable, but it only generates Kotlin and needs KSP. That breaks the Java-only rule.

## Important notes

### Room 2.x is deprecated
The Room 2 guide now begins: "Caution: This guide covers Room 2.x, which is deprecated."
Room 2.x is still the only Room that supports Java annotation processing, and 2.8.5 was released on September 9, 2026.
Use the Room 2 guide (https://developer.android.com/training/data-storage/room/v2) and the Room release notes. Newer Room pages show Room 3 code that won't work here.

### One JDK for Android Studio and the terminal
- The template's `gradle/gradle-daemon-jvm.properties` says the Gradle daemon needs Java 25. Both Android Studio and the terminal follow it.
- `gradlew` in a terminal starts with `JAVA_HOME`, which is Studio's JBR 25.0.3. That matches.
- If no Java 25 can be found, Gradle downloads one using the links in that file.
- Checked: `.\gradlew.bat --version` shows "Launcher JVM: 25.0.3" and "Daemon JVM: Compatible with Java 25".

### Network note: Maven Central blocked (HTTP 429)
- On the NVIDIA network, Maven Central (`repo.maven.apache.org`) answers every request with "429 Too Many Requests". Sonatype's FAQ says this happens when a shared network passes a usage limit, and blocks can last up to 30 days: https://central.sonatype.org/faq/429-error/
- `settings.gradle` therefore uses Google's copy of Maven Central, `https://maven-central.storage-download.googleapis.com/maven2/`, in place of `mavenCentral()`:
  - Google Cloud's docs describe it as a "hosted mirror of Maven Central": https://docs.cloud.google.com/docs/buildpacks/java
  - Its own index page says it is "not an officially supported Google product".
- The mirror is listed before `gradlePluginPortal()`. The Plugin Portal forwards Maven Central files to the blocked server.
- On another network, `mavenCentral()` works and can replace the mirror lines.

### AGP 9 notes for a Java app
- `enableKotlin = false` (in `app/build.gradle`) turns off AGP 9's built-in Kotlin for this Java-only module. It removes the Kotlin compile step and the automatic Kotlin standard library: https://developer.android.com/build/migrate-to-built-in-kotlin
  - AndroidX libraries still bring the Kotlin standard library as their own dependency. That's library code, not app source.
- `R.id` values are not constants, so Java `switch` cases on them won't compile. Use `if`/`else`.
- The release build uses the new `optimization { enable false }` block with keep rules in `app/src/main/keepRules/`.

### Label scanning (OCR) (added 2026-10-04)

**Why Tesseract and not Google ML Kit**
- ML Kit's text recognition runs on the phone. But its official data-disclosure page (https://developers.google.com/ml-kit/android-data-disclosure) says "ML Kit Android SDKs collect device information, app information, performance metrics, and other data for diagnostics and usage analytics".
- Neither that page nor the ML Kit terms describes a way to turn this off.
- Its libraries include Google's logging libraries (datatransport, Firebase components).
- That conflicts with CLAUDE.md rules 6 and 7 (no networking libraries, no analytics).
- **Checked again on 2026-10-12**, because the guide's photo-entry step asks for "the bundled ML Kit text-recognition model". The student chose to keep Tesseract.
  - The ML Kit terms' Privacy section (https://developers.google.com/ml-kit/terms) still says: "The ML Kit APIs also send metrics about the performance and utilization of the APIs in your app to Google." The page describes no way to turn this off.
  - The bundled Latin model is `com.google.mlkit:text-recognition:16.0.1` (the guide's line and Google Maven's latest, checked 2026-10-12). Its POM files on Google Maven (read on 2026-10-12, not built) show this chain: `text-recognition:16.0.1` → `play-services-mlkit-text-recognition:19.0.1` → `play-services-mlkit-text-recognition-common:19.1.0` → Google's datatransport logging libraries `transport-api` 2.2.1, `transport-backend-cct` 2.3.3, and `transport-runtime` 2.2.6.
  - A request on GitHub to turn the metrics off (googlesamples/mlkit issue #593, open since December 2022) is not an official source and has no answer from Google.
  - What the guide wanted from "bundled", recognition that works offline after installation, Tesseract already does: its English data is inside the APK (see "Model download requirements").

**Model download requirements (added 2026-10-12)**
- None at run time. Tesseract's English data (`eng.traineddata`, 4,113,088 bytes) is inside the APK, in `assets/tessdata/`. The first reading copies it once to the app's private `files/ocr/tessdata/`.
- Installing the APK is all that is needed. Reading works in airplane mode (checked on the emulator, TEST-RESULTS.md section 9).
- CameraX has no model. The photo picker uses no Play-services module (see below).
- If the language data is ever replaced, update `MODEL_BYTES` in `LabelPhotoReader.java`.

**JitPack**
- Tesseract4Android is published only on JitPack.
- `settings.gradle` adds JitPack inside an `exclusiveContent` filter for the group `cz.adaptech.tesseract4android`, so Gradle asks JitPack for that group only.
- The downloaded AAR's SHA-256 is `bce5d6413a1a5ae3d7240033fbbc851ba3217d0a08d9769400e17a077f42cb2a`, the same value as in JitPack's `.module` file. Checked 2026-10-04.
- A GitHub issue (#86) reports that the JitPack download can fail on a fresh machine. If that happens, keep the checked AAR in a project-local Maven folder.

**Language data**
- `eng.traineddata` was downloaded from `https://raw.githubusercontent.com/tesseract-ocr/tessdata_fast/87416418657359cb625c412a48b6e1d6d41c29bd/eng.traineddata`.
- SHA-256: `7d4322bd2a7749724879683fc3912cb542f19906c83bcc1a52132556427170b2`.
- The app copies it once to `files/ocr/tessdata/`. If the file is ever replaced, update `MODEL_BYTES` in `LabelPhotoReader.java`.

**Native code**
- The library brings C++ libraries: libtesseract, libleptonica, libjpeg, libpngx. The app source stays Java.
- `abiFilters` keeps arm64-v8a, armeabi-v7a, and x86_64 (the emulator).
- No NDK is installed, so the build prints "Unable to strip the following libraries…". This is expected.
- A search of the arm64 `.so` files found no network functions (`socket`, `connect`, `getaddrinfo`, curl). The only URLs are XML format names and compiler build strings.

**ExifInterface**
- `LabelPhotoReader` uses Android's built-in `android.media.ExifInterface`, so no extra library is needed.
- Lint suggests the AndroidX ExifInterface because of bugs in older Android versions, so that lint check is suppressed on that one method.
- Switching to `androidx.exifinterface` is a possible follow-up (it needs its own version check).

**Size**
- The debug APK grew from 8,831,760 bytes (commit 108539f) to 32,162,046 bytes: native code for 3 CPU types, plus the language data.
- `zipalign -c -P 16 -v 4` passed. **Correction (2026-10-12):** that command checks only where the `.so` files sit inside the APK (zip alignment). The official page-size guide also asks for the LOAD segments inside each `.so` to be at least `2**14` (16 KB). That was first checked on 2026-10-12 (see "Photo entry and CameraX"): every 64-bit library is aligned, and Tesseract's four 32-bit (armeabi-v7a) libraries are 4 KB aligned.

Licenses are listed in THIRD-PARTY-NOTICES.md.

### Photo entry and CameraX (added 2026-10-12)
Photo entry replaced the camera-app path above (SPEC.md, "Photo entry"). Tesseract, JitPack, and the language data are unchanged.

**Why CameraX now**
- The guide's step asks for CameraX, camera-permission handling, focus, rotation, and poor light. The 2026-10-04 reason against it (the CAMERA permission) is now accepted: CAMERA is asked for only on the Label photos screen, and everything else works without it.
- The manifest declares CAMERA and four camera `<uses-feature>` lines with `required="false"` (`camera.any`, `camera`, `camera.autofocus`, `camera.flash`), as the official uses-feature page recommends, so phones without a camera can still install the app. `aapt2 dump badging` lists all four as "uses-feature-not-required".
- The FileProvider and `res/xml/label_photo_paths.xml` were removed: CameraX writes straight into the app's private `cache/label_photos/`, so no other app gets a photo.

**How it is used**
- `LifecycleCameraController` + `PreviewView` (camera-view), which the architecture page calls "the simplest way to use CameraX". It has Java samples there.
  - Only `IMAGE_CAPTURE` is enabled (no image analysis). `CAPTURE_MODE_MAXIMIZE_QUALITY`, flash off.
  - **Rotation:** the controller has an automatic device-rotation listener. The 1.7.0-alpha02 note "Added CameraController.setAutoRotationEnabled(boolean) to allow disabling the automatic device rotation listener" shows it is on in 1.6.2. The saved JPEG gets an EXIF orientation tag, which `LabelPhotoReader` applies.
  - **Focus feedback** uses `getTapToFocusInfoState()`. `getTapToFocusState()` is deprecated since 1.5.0-alpha06, so it is not used (no javac warnings).
  - **Low light:** CameraX's Low Light Boost is for the preview, not still photos, and only on Android 15 or newer; the night-mode extension is not on every phone. Neither is used. The torch ("Turn on the light") is.

**The compile-only library**
- With CameraX 1.6.2, javac 25 stopped with `error: Cannot attach type annotations @org.jspecify.annotations.Nullable to PendingValue.mCompleterAndValue` and `class file for androidx.concurrent.futures.CallbackToFutureAdapter not found`.
- `androidx.camera.view.PendingValue` (camera-view) has a type-annotated field whose type names that class. CameraX lists `concurrent-futures` for run time only, so it was missing at compile time. javac reads type annotations from class files since JDK 22 (JDK-8225377).
- Fix: `compileOnly libs.concurrent.futures` (1.1.0, the version the app already ships). The build then printed no javac warnings or notes.

**What CameraX brings (from `:app:dependencies`, debugRuntimeClasspath, before and after)**
- 91 → 116 libraries: 25 added, and no library that the app declares changed version.
- **Added:**
  - CameraX: camera-core, camera-camera2, camera-camera2-pipe, camera-lifecycle, camera-video, camera-view (1.6.2), featurecombinationquery 1.6.2, viewfinder-core 1.5.1
  - `androidx.core:core-backported-fixes` 1.0.0, `exifinterface` 1.4.2, `tracing-android` 1.3.0
  - Media3 common, container, and muxer 1.9.0 (camera-view needs camera-video)
  - Guava 33.3.1-android and failureaccess 1.0.2, Dagger 2.59, `javax.inject` 1, `jakarta.inject-api` 2.0.1, auto-value-annotations 1.6.3, jsr305 3.0.2, j2objc-annotations 3.0.0, checker-qual 3.43.0, atomicfu (and atomicfu-jvm) 0.28.0
- **Moved up by Gradle:** tracing and tracing-ktx 1.2.0 → 1.3.0, error_prone_annotations 2.15.0 → 2.28.0, listenablefuture 1.0 → Guava's empty placeholder.
- The androidTest classpath resolved without a conflict, and all 96 instrumented tests passed.
- Non-Google libraries (Guava, Dagger, kotlinx, JSpecify, auto-value, javax.inject) come through the Google mirror of Maven Central (see "Network note").
- **Manifest:** camera-core adds a disabled, non-exported `MetadataHolderService`. media3-common asks for ACCESS_NETWORK_STATE; the merger report lists it as "REJECTED" (removed by the manifest, like WorkManager's). No library adds INTERNET.
- None of these libraries has INTERNET, and the app has no INTERNET permission, so nothing can be sent.

**Size and 16 KB pages**
- The debug APK grew from 31,387,790 bytes (2026-10-12, before) to 34,972,951 bytes. The release build doesn't shrink code (`optimization { enable false }`), so all of the added libraries are inside.
- **16 KB:** `zipalign -c -P 16 -v 4` gave "Verification successful".
- A Python script (scratchpad, not in the project) read every `.so` file's LOAD segments, the check the official guide does with `llvm-objdump` (no NDK is installed):
  - All 12 libraries for arm64-v8a and x86_64 are aligned to `0x4000`: CameraX's `libimage_processing_util_jni.so` and `libsurface_util_jni.so`, and Tesseract's four.
  - CameraX's two armeabi-v7a libraries are aligned too.
  - Tesseract's four armeabi-v7a libraries are aligned to `0x1000` (4 KB).
  - The guide's Google Play requirement is about "16 KB memory page sizes on 64-bit devices", so the 32-bit libraries are not covered by it.
- On the 16 KB emulator image (`getconf PAGE_SIZE` = 16384), `dumpsys package` showed `primaryCpuAbi=x86_64` and `pageSizeCompat=0` for ReadyShelf.

### Room and Lifecycle notes (added 2026-10-04)
- `app/build.gradle` applies the Room Gradle plugin and sets `room { schemaDirectory "$projectDir/schemas" }` at the top level.
  The build writes `app/schemas/com.example.readyshelf.data.AppDatabase/1.json`. Keep that folder in Git: later Room migrations are checked against it.
- Version 2 (2026-10-11) added `2.json`. `1.json` stays: the version 1 schema is the starting point of the migration test.
- The Room Gradle plugin also packs these schema files into the androidTest APK (seen in `app-debug-androidTest.apk`: `assets/com.example.readyshelf.data.AppDatabase/1.json` and `2.json`).
- If the plugin ever stops working, the fallback is the annotation-processor option `room.schemaLocation`
  (`android.defaultConfig.javaCompileOptions.annotationProcessorOptions { argument(...) }`). Never use both.
- Before this change, the project got Lifecycle through `androidx.activity:activity` 1.13.0, which asks for Lifecycle 2.6.1.
  Adding Lifecycle 2.11.0 directly raises the whole Lifecycle group to 2.11.0, because Gradle picks the highest version asked for.
- Material 1.14.0 asks for RecyclerView 1.3.1-rc01, and Gradle resolves it to the 1.4.0 declared here.

### Migration test without room-testing (2026-10-11)
- `androidx.room:room-testing:2.8.5` was added for `MigrationTestHelper`, and the test failed on the emulator before any migration ran:
  `java.lang.AbstractMethodError: abstract method "kotlinx.serialization.KSerializer[] kotlinx.serialization.internal.GeneratedSerializer.typeParametersSerializers()" on receiver ... androidx.room.migration.bundle.FieldBundle$$serializer`.
- Cause, from `.\gradlew.bat :app:dependencies`:
  - The app's own libraries bring `kotlinx-serialization-core` 1.7.3 (`debugRuntimeClasspath`).
  - room-testing needs `kotlinx-serialization-json` 1.8.1.
  - AGP keeps the test APK on the app's versions (`kotlinx-serialization-core:{strictly 1.7.3}` in `debugAndroidTestRuntimeClasspath`), so Room's schema reader ran on the older library.
- Fixing it would mean raising a library version inside the shipped app, only for a test. Instead, room-testing was removed again.
- `MigrationTest` now writes a version 1 database file with plain SQLite (the tables copied from `1.json`) and opens it with the app's own Room database. Room runs the migration and then checks every table against version 2; a mismatch fails with "Migration didn't properly handle".
- Run it **only on the emulator**. `connectedDebugAndroidTest` would also run on the connected phone and uninstall the app there, which deletes its data. Use:
  ```
  adb -s emulator-5554 install -r -t app\build\outputs\apk\debug\app-debug.apk
  adb -s emulator-5554 install -r -t app\build\outputs\apk\androidTest\debug\app-debug-androidTest.apk
  adb -s emulator-5554 shell am instrument -w com.example.readyshelf.test/androidx.test.runner.AndroidJUnitRunner
  ```

### Reminders and WorkManager (added 2026-10-11)
- **Permissions.** The app's manifest adds POST_NOTIFICATIONS. WorkManager's own manifest adds WAKE_LOCK, RECEIVE_BOOT_COMPLETED, ACCESS_NETWORK_STATE, and FOREGROUND_SERVICE.
  - ACCESS_NETWORK_STATE and FOREGROUND_SERVICE are removed with `tools:node="remove"`, like INTERNET. The daily check has no network condition and never runs as a foreground service. The manifest merger printed no warning for these two lines, so they removed real declarations.
  - WAKE_LOCK (WorkManager keeps the phone awake while a check runs) and RECEIVE_BOOT_COMPLETED (WorkManager schedules the check again after the phone restarts) stay.
  - Never use `setExpedited`, `setForeground`, or network constraints in a work request: they would need the removed permissions.
  - WorkManager asks for no exact-alarm permission (SCHEDULE_EXACT_ALARM or USE_EXACT_ALARM). Don't add one.
- **Changing the schedule later.** WorkManager stores the worker's class name, so don't rename or move `ReminderWorker`. To change the interval, enqueue once with `ExistingPeriodicWorkPolicy.UPDATE`; KEEP would keep the old request.
- **Force-stop.** A force-stop removes the app's jobs until the app is opened again. `am instrument` force-stops the app at its start and end, so open the app once before checking `adb -s emulator-5554 shell dumpsys jobscheduler`.
  - On Android 17 the job is listed as `JOB androidx.work.systemjobscheduler:<uid>/<id>` (WorkManager uses a job namespace), not `JOB #…`.
- **The notification permission in tests.** Taking POST_NOTIFICATIONS away (`pm revoke`, or turning notifications off in Android's settings) stops the app's process, and a running test with it. So the tests grant it with `UiAutomation.grantRuntimePermission` and never take it away.
- **Lint.** `MissingPermission` wants the Android 13 permission check in the same method as `notify()` (`ReminderNotifier.post`).
- **Toasts.** Android shows only two lines of a toast, so a message that must be read in full goes on the screen or in a dialog.
- **Notification taps.** Android compares a tapped intent with the intent that started the app's task, without the extras. If they are the same, it only brings the task to the front. So each review intent gets its own data (`readyshelf-review:<time>`; see `HomeActivity.newReviewIntent`).
- **The reminder channel (changed 2026-10-18).** An app can't raise an existing channel's importance, so the silent `review_reminders` was replaced by `review_alerts` (high importance: pop-up and sound). `ReminderNotifier.createChannel()` creates the new channel, then deletes the old one. Never use either ID for a different setting: Android brings back a deleted channel with its old settings when its ID is created again. `ReminderChannelTest` uses its own test channel IDs for the same reason.
  - If "Review reminders" is changed by hand on the emulator, the reminder tests can fail until ReadyShelf is installed on the emulator again (never on the phone).
  - `cmd notification set_dnd on` turns on Do Not Disturb on the emulator for a check. On 2026-10-18 it left the ringer on silent after `set_dnd off`; `cmd audio set-ringer-mode NORMAL` put it back (check with `settings get global mode_ringer`, 2 = normal).
- **The home-screen widget (added 2026-10-18).** No new library: an `AppWidgetProvider` with a RemoteViews layout.
  - `updatePeriodMillis` is 6 hours. Android's minimum is 30 minutes, and each refresh wakes the phone for a moment.
  - Never start WorkManager work in `ReviewWidgetProvider.onUpdate`: that can make Android send the update again and again.
  - Room's `InvalidationTracker.addObserver` must run on a background thread (`ReadyShelfRepository.listenForBatchChanges`).
  - Never rename or move `ReviewWidgetProvider`: placed widgets are tied to its class name.
  - `ReviewWidgetTest`'s "no widget placed" test skips itself while a ReadyShelf widget is on the emulator's home screen.
- **Two emulator-only tests that run only on purpose** (both skip themselves in a normal run):
  - Notifications refused:
    ```
    adb -s emulator-5554 shell pm revoke com.example.readyshelf android.permission.POST_NOTIFICATIONS
    adb -s emulator-5554 shell am instrument -w -e notifications denied -e class com.example.readyshelf.reminders.ReminderDeniedTest com.example.readyshelf.test/androidx.test.runner.AndroidJUnitRunner
    ```
  - Reboot check: schedule a test check that waits two days, open the app once, look at `dumpsys jobscheduler`, restart the emulator (`adb -s emulator-5554 reboot`), wait until the boot has finished plus about a minute, and look again without opening the app. Then cancel it:
    ```
    adb -s emulator-5554 shell am instrument -w -e rebootCheck schedule -e class com.example.readyshelf.reminders.ReminderRebootSetup com.example.readyshelf.test/androidx.test.runner.AndroidJUnitRunner
    adb -s emulator-5554 shell am instrument -w -e rebootCheck cancel -e class com.example.readyshelf.reminders.ReminderRebootSetup com.example.readyshelf.test/androidx.test.runner.AndroidJUnitRunner
    ```

### Privacy settings that keep data on the phone
- `AndroidManifest.xml` removes the INTERNET permission with `tools:node="remove"`, so no library can add network access. Since 2026-10-11 it also removes WorkManager's ACCESS_NETWORK_STATE (see "Reminders and WorkManager"), and since 2026-10-12 the same request from CameraX's media3-common.
- Since 2026-10-12 the app has the CAMERA permission, for photo entry only. Photos stay in the private `cache/label_photos/` folder and are deleted after reading (SPEC.md, "Photo entry").
- It also sets `android:allowBackup="false"`.
- `res/xml/data_extraction_rules.xml` (Android 12+) and `res/xml/backup_rules.xml` (Android 11 and lower) exclude all app data from cloud backup and from phone-to-phone transfer. That includes the reminder file (`shared_prefs/reminders.xml`) and WorkManager's database in the no-backup folder (checked 2026-10-11).
- Trade-off: data will not move to a new phone automatically.

## Verified build (2026-10-04)

Claude Code ran these in PowerShell, with `JAVA_HOME` set to Studio's JBR. The student ran the Android Studio sync and shared a screenshot of the result. The results are copied from the real output.

| Check | Command | Real result |
|---|---|---|
| Gradle and JDK | `.\gradlew.bat --version` | Gradle 9.6.0; Launcher JVM 25.0.3 (JetBrains); Daemon JVM "Compatible with Java 25" |
| Debug build | `.\gradlew.bat assembleDebug` | BUILD SUCCESSFUL in 1m 37s (first build, including downloads through the mirror) |
| Unit tests | `.\gradlew.bat testDebugUnitTest` | `ExampleUnitTest`: 1 test, 0 failures, 0 errors |
| Lint | `.\gradlew.bat lintDebug` | 0 errors, 2 warnings, both `UnusedResources` for the template's colors `black` and `white` |
| APK contents | `aapt2 dump badging` and `aapt2 dump permissions` | minSdk 26, targetSdk 37, compileSdk 37. **No INTERNET permission.** The only permission is the app-private `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION` that AndroidX adds |
| Source languages | file search outside `build` folders | 3 `.java` files, 0 `.kt`/`.kts` files |
| Install | `.\gradlew.bat installDebug` | "Installed on 1 device." (moto g play - 2024, Android 14) |
| Launch | `adb shell am start -W -n com.example.readyshelf/.MainActivity` | "Status: ok", cold start 1366 ms; `topResumedActivity` was `com.example.readyshelf/.MainActivity` |
| On screen | `adb exec-out screencap -p` | The screenshot showed "Hello World!" on the app screen |
| Android Studio sync | File › Sync Project with Gradle Files (run by the student) | "finished" in 36 s; output "BUILD SUCCESSFUL in 30s". Notices: "Download info" and "New Minor Gradle Version Available" (not applied; see "Newer versions") |

**Not run:**
- the instrumented test (`connectedDebugAndroidTest`)
- a release build

### Known warnings (expected, not errors)
- **Manifest merger:** "uses-permission#android.permission.INTERNET was tagged at AndroidManifest.xml:7 to remove other declarations but no other declaration present".
  - This means no library asks for INTERNET today. The removal line stays as a safeguard.
- **Gradle:** "Deprecated Gradle features were used in this build, making it incompatible with Gradle 10."
  - Gradle's problems report (`build/reports/problems/problems-report.html`) says: "Properties should be assigned using the 'propName = value' syntax".
  - It points at two template lines in `app/build.gradle`: line 6 `namespace 'com.example.readyshelf'` and line 29 `enable false`.
  - Fix: `namespace = 'com.example.readyshelf'` and `enable = false`. Not changed during setup.

## Verified build: first screens (2026-10-04)

Claude Code ran these in PowerShell, with `JAVA_HOME` set to Studio's JBR, after building the first screen structure (see SPEC.md and AI-USE.md). The results are copied from the real output.

| Check | Command | Real result |
|---|---|---|
| Debug build | `.\gradlew.bat assembleDebug` | BUILD SUCCESSFUL. `compileDebugJavaWithJavac` printed no warnings |
| Unit tests | `.\gradlew.bat testDebugUnitTest` | 21 tests, 0 failures, 0 errors: `DateThresholdsTest` 4, `InventoryFilterTest` 8, `RuleRunnerTest` 8, `ExampleUnitTest` 1 |
| Lint | `.\gradlew.bat lintDebug` | 0 errors, 2 warnings: the template's unused colors `black` and `white`, the same as at setup |
| Room schema | file check | `app/schemas/com.example.readyshelf.data.AppDatabase/1.json`: version 1, tables `locations`, `batches`, `batch_events` |
| APK contents | `aapt2 dump permissions` and `aapt2 dump badging` | minSdk 26, targetSdk 37, compileSdk 37. **No INTERNET permission.** Launcher activity: `com.example.readyshelf.ui.home.HomeActivity` |
| Source languages | file search outside `build` folders | 50 `.java` files in `app/src`, 0 `.kt`/`.kts` files |
| Gradle deprecations | `.\gradlew.bat help --warning-mode all` | Only the two template lines described above (now lines 7 and 30 of `app/build.gradle`). The Room plugin added none |
| Install | `.\gradlew.bat installDebug` | "Installed on 1 device." (moto g play - 2024, Android 14) |
| Launch | `adb shell am start -W -n com.example.readyshelf/.ui.home.HomeActivity` | "Status: ok" |
| Smoke test on the phone | `uiautomator dump`, `input tap`, and screenshots | See below |

Smoke test on the phone (default font size, light theme, portrait):
- Home showed the getting-started message, the four buttons, and "Not available yet" for "Needs review" and "Recommended swaps".
- The batch form's Location list showed Pantry, Earthquake kit, First aid box (the seeded locations).
- The New location dialog showed "Enter a name." for an empty name and "A location with this name already exists." for "Pantry", and it stayed open both times. No location was added.
- Settings showed four thresholds of 7, a turned-off reminder switch with its reason, and the sample data buttons.
- "Load sample data" added 6 items, and each showed "Sample item".
  - Home showed the sample banner.
  - Inventory listed them soonest date first, with "No date entered" last.
  - The location filter (Earthquake kit) showed 2 of 6, and the search "gran" showed 2 of 6.
- Batch details showed the fields, "Status: not available yet…", and the history.
  Editing the quantity from 6 to 4 added the history entry "Quantity changed from 6 bars to 4 bars."
- The Purchase list showed its explanation and "Not available yet".
- "Remove sample data" asked for confirmation, then removed all 6. Home and Inventory showed their empty-state messages again.
- The phone was left with no batches. Its font size and other settings were not changed.

**Not run:** the instrumented test (`connectedDebugAndroidTest` uninstalls the app, which deletes its data), a release build, TalkBack, large font or display size, dark theme, screen rotation, and adding a new location (not done, because locations can't be deleted yet).

## Verified build: label scanning (2026-10-04)

Claude Code ran these in PowerShell, with `JAVA_HOME` set to Studio's JBR. The results are copied from the real output.

| Check | Command | Real result |
|---|---|---|
| Debug build | `.\gradlew.bat assembleDebug` | BUILD SUCCESSFUL. `compileDebugJavaWithJavac --rerun` printed no warnings |
| Unit tests | `.\gradlew.bat testDebugUnitTest` | 75 tests, 0 failures, 0 errors. New: `LabelDateFinderTest` 28, `CategoryGuesserTest` 11, `LabelReadingTest` 8, `NameCandidatesTest` 7 |
| Lint | `.\gradlew.bat lintDebug` | 0 errors, 2 warnings (the template's unused colors, as before) |
| Gradle deprecations | `.\gradlew.bat help --warning-mode all` | Only the two template lines (now lines 7 and 37 of `app/build.gradle`) |
| Library checksum | SHA-256 of the cached AAR | Matches JitPack's `.module` value |
| APK contents | `aapt2 dump permissions` and `aapt2 dump xmltree` | **No INTERNET, no CAMERA, no ACCESS_NETWORK_STATE.** The FileProvider authority is `com.example.readyshelf.fileprovider`, with `exported=false` |
| 16 KB alignment | `zipalign -c -P 16 -v 4 app-debug.apk` | "Verification successful" (all `.so` files OK) |
| APK size | file size | 32,162,046 bytes (before: 8,831,760) |
| Install | `.\gradlew.bat installDebug` | "Installed on 1 device." |

**On the phone, with 4 synthetic test pictures.** The pictures were made with java.awt; they are not photos of real labels. They were put in Download, chosen with "Choose a photo", and deleted afterwards.
- **Printed "BEST BY 10/25/2026" + "Nutrition Facts":**
  - The date was filled in as Best by Oct 25, 2026.
  - Category: Food (found "NUTRITION FACTS").
  - Name buttons: "Nature Valley", "Crunchy Granola Bars". Tapping one filled in the name.
  - Read in 843 ms (first scan, including the language data copy).
- **Dot-matrix "EXP 03/2027" + "Drug Facts":**
  - No date found (a known Tesseract weakness).
  - Category: Medicine (found "DRUG FACTS").
  - The first run also offered junk lines read from the dots as names. After a fix to `NameCandidates`, only "100 Tablets" was offered.
  - 635 ms, then 754 ms after the fix.
- **"BEST BY 11/20/2026" + "USE BY 12/20/2026" + "Ingredients":**
  - Two "Choose" date buttons. Choosing one filled in the date, with its note.
  - The Food suggestion was not applied, because the form's category was already Medicine.
  - 635 ms, then 653 ms.
- **"NATURAL SPRING WATER" + "EXP 04/05/2027" (fresh form):**
  - Two readings offered: Apr 5 (month first) and May 4 (day first).
  - Category: Water (found "SPRING WATER").
  - 555 ms.
- **Camera opened and closed:**
  - The Motorola camera app opened, and ReadyShelf asked for no permission.
  - With the camera open, `adb shell am kill com.example.readyshelf` stopped the app. Back in the camera app, the app restarted (new process) and showed "No photo was taken.".
  - The name, Medicine, Pantry, and Best by Nov 20, 2026 were all still filled in, each with its "From the photo" note.
- **Older flows:**
  - Load sample data, then edit a sample batch: Edit showed no scan section, and the history shows "Quantity changed from 12 bottles to 10 bottles.".
  - Remove sample data worked.
- **Clean-up:**
  - The 4 pictures were deleted from Download and from the media index.
  - `run-as … ls cache/label_photos` showed an empty folder.
  - Inventory showed "Showing 0 of 0 batches". No scanned batch was saved.

**Not run:**
- photos of real labels
- saving a new batch from a scan
- TalkBack
- large font or display size
- dark theme
- rotation during a scan
- the instrumented test
- a release build

## Verified build: products and stock changes (2026-10-11)

Claude Code ran these in PowerShell and Git Bash, with `JAVA_HOME` set to Studio's JBR. The results are copied from the real output.

| Check | Command | Real result |
|---|---|---|
| Compile | `.\gradlew.bat :app:compileDebugJavaWithJavac --rerun` | BUILD SUCCESSFUL. javac printed no warnings |
| Debug build | `.\gradlew.bat assembleDebug` | BUILD SUCCESSFUL |
| Unit tests | `.\gradlew.bat testDebugUnitTest` | 103 tests, 0 failures, 0 errors. New: `StockChangeTest` 14, `DateEntryTest` 5, `BatchTextTest` 3; `RuleRunnerTest` 8 → 13; `InventoryFilterTest` 8 → 9 |
| Lint | `.\gradlew.bat lintDebug` | 0 errors, 3 warnings: the template's unused colors `black` and `white` (as before), and `TooManyViews` for `activity_batch_edit.xml` (more than 80 views in the long form) |
| Room schema | file check | `2.json`: version 2, tables `locations`, `products`, `batches`, `batch_events`. A script compared the CREATE statements in `Migrations.java` with `2.json`: all 3 tables and 3 indexes match exactly |
| Instrumented tests (emulator only) | the three `adb -s emulator-5554` commands above | "OK (4 tests)": `MigrationTest` 3 (keeps every batch and history entry; works with no batches; a failing step changes nothing) and `ExampleInstrumentedTest` 1 |
| APK contents | `aapt2 dump permissions` and `aapt2 dump badging` | **No INTERNET and no CAMERA permission.** The only permission is AndroidX's app-private `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. targetSdk 37. Launcher: `ui.home.HomeActivity` |
| Source languages | file search in `app/src` | 74 `.java` files, 0 `.kt`/`.kts` files |

**Upgrade test on the emulator (not the phone).** The committed version (048b49e, database version 1) was built in a temporary git worktree and installed on the emulator. Sample data was loaded, a batch "Tea" (3, no unit, no date) was added, and a sample batch was edited (6 → 5 bars). Then the new build was installed over it with `adb install -r`:
- All 7 batches and their history were still there. Every date showed "Date needs review"; Tea showed "3 (no unit entered) in Pantry".
- Record used → Some: 9 was refused ("Enter a whole number from 1 to 5."). With 2, the review showed "Granola bars in Earthquake kit: 5 bars before, 3 bars after. Replacement may be needed. …". The review stayed after rotating to landscape and back. After Confirm, the history showed the entry.
- Move → Some → 1 to Pantry: the kit batch went to 2 bars, a new 1-bar batch appeared in Pantry with its own "Moved" history, and the other Pantry batch (10 bars) didn't change.
- Add batch with empty fields: errors appeared at the product field, the quantity field, and under "Choose date".
- New product "Canned beans / Low sodium / 15 oz / cans", 6, a past date (5 Sep 2026), not confirmed, Sealed = Yes: every field and choice stayed after rotating; the four food questions started at "Not checked"; system Back on the review returned to the form; the saved batch showed "Date needs review".
- The same product typed again with other capital letters was refused ("This product already exists…"). From "Existing product", the list row showed "Canned beans" and "Low sodium · 15 oz · unit: cans". (On the first try the row was cut off at the right edge; this was fixed, rebuilt, and checked again.) The chosen product stayed chosen after rotating. An incomplete date "03/2027" with Expires was saved as typed.
- Edit: ticking the confirmation saved "Date and label type confirmed.", and "Date needs review" went away. A count correction on the kit batch (2 → 1) showed the kit warning on the review; it was not saved.
- Archive (Tea): hidden in Inventory, shown again with "Show archived batches"; details showed "Archived" and only Edit.
- A made-up medicine batch: Record used showed "This only records that this amount is no longer in stock. ReadyShelf does not give advice about taking medicine." Nothing was confirmed.
- Remove sample data: only the 4 test batches stayed. A copy of the emulator's database showed user_version 2, no sample products left, the saved answers, and an empty `PRAGMA foreign_key_check`. The copy was deleted afterwards.
- Clean-up: the app was uninstalled from the emulator, the emulator's rotation setting was put back, the emulator was shut down, and the temporary worktree was deleted.

**Not run:** anything on the household phone (the student runs the phone test), TalkBack, large font or display size, dark theme, a label scan with the new form, and a release build.

## Verified build: confirmable swaps, purchase list, and food class (2026-10-11)

Claude Code ran these in PowerShell and Git Bash, with `JAVA_HOME` set to Studio's JBR. The results are copied from the real output. No library or tool version changed.

| Check | Command | Real result |
|---|---|---|
| Debug build, unit tests, lint (all tasks run again) | `.\gradlew.bat assembleDebug testDebugUnitTest lintDebug --rerun-tasks` | BUILD SUCCESSFUL. javac printed no warnings |
| Unit tests | (same run) | 131 tests, 0 failures, 0 errors. `RuleRunnerTest` 9 → 21. New: `SwapPreviewTest` 4, `PurchaseSuggestionsTest` 6 |
| Lint | (same run) | 0 errors, 3 warnings, the same as before: the template's unused colors `black` and `white`, and `TooManyViews` for `activity_batch_edit.xml`. The first lint run of this step also found `PluralsCandidate` ("%d days"); it was fixed with the `day_count` plural |
| Room schema | file check and a script | `3.json`: version 3, identity hash `1ce4c134a8a1738d3cf4299f3af1d362`. Tables `locations`, `products` (new column `foodClass`), `batches`, `batch_events`, and the new `swap_actions` and `purchase_items`. The script compared `MIGRATION_2_3` with `3.json`: the `ALTER TABLE`, both new tables, and all 4 new indexes match exactly |
| Instrumented tests (emulator only) | `.\gradlew.bat assembleDebugAndroidTest`, then the three `adb -s emulator-5554` commands above | "OK (41 tests)": `SwapStoreTest` 21, `PurchaseListStoreTest` 14, `MigrationTest` 5 (3 updated to end at version 3, 2 new for version 2 → 3), `ExampleInstrumentedTest` 1 |
| APK contents | `aapt2 dump permissions` and `aapt2 dump badging` | **No INTERNET and no CAMERA permission.** The only permission is AndroidX's app-private `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. targetSdk 37. Launcher: `ui.home.HomeActivity` |
| Source languages | file search in `app/src` | 98 `.java` files, 0 `.kt`/`.kts` files |

Notes on the storage tests:
- They use a Room database in memory, so they never touch the app's own database file.
- Their planners only return answers that each test sets up. They are not a RotationPlanner.
- A failure in the middle of a swap is made with a test-only SQLite trigger (`RAISE(ABORT)`), with no hook in the app's code. The failure happens after both movements and their history were written, so the test proves that nothing is kept.
- `Room.inMemoryDatabaseBuilder` turns foreign keys on (`PRAGMA foreign_keys` = 1, checked in `SwapStoreTest`).

**Upgrade test on the emulator (not the phone).** The committed version (86bc7e6, database version 2) was built in a temporary git worktree and installed on the emulator, and sample data was loaded. Then the new build was installed over it with `adb install -r`:
- A copy of the database before and after the upgrade was compared. The only differences were `user_version` 2 → 3, the new `foodClass` column (empty for all 5 products), and the two new tables (empty). All 6 batches, 6 history entries, and 3 locations were unchanged, and `PRAGMA foreign_key_check` was empty.
- Home showed the sample banner and "Not available yet" for Needs review and Recommended swaps. After a made-up "Test medicine" batch (First aid box, no date) was added, Home showed the separate "Medicine dates to review" section with its note and "Not available yet".
- Food class:
  - On the Edit form of the kit granola bars it started at "Not classified", with "Automatic swaps are not turned on yet."
  - "Shelf-stable food" stayed chosen after rotating to landscape and back. TalkBack descriptions read "Food class: …".
  - The review showed only "Food class of the product changed from Not classified to Shelf-stable food. …"; a class-only change is not lost.
  - After saving, the history had that sentence, and Batch details of the other granola bar batch (in the Pantry) also showed "Food class of the product: Shelf-stable food".
  - For the medicine batch the review had no food class line.
- Purchase list:
  - With no item it showed the empty-list text and "Not available yet" for suggestions and needs.
  - Purchase-list items can't be made in the app until RotationPlanner is connected, so one test item (3 cans of a made-up "Test beans" product for Earthquake kit) was put into a copy of the emulator's database and pushed back.
  - The item card showed "To buy: 3 cans for Earthquake kit". The Remove dialog said "Your recorded stock does not change."; Cancel kept the item, and rotating with the dialog open closed it without a crash.
- Record bought item:
  - The title was right. The product was fixed (its choices turned off). The quantity was filled in with 3 and stayed after rotating. "No date printed" was turned off, with its note.
  - With "03/2028" as the printed text, the review listed the new batch, "Food class of the product: Not classified", and "This removes “3 cans of Test beans … for Earthquake kit” from your purchase list."
  - After Confirm, the list was empty. The database had a new 3-can batch in the Pantry with printed text "03/2028" and the history "Added 3 cans to Pantry. Bought from the purchase list."; the item was gone.
- Remove sample data: only the two test batches and their history stayed, the sample products were deleted, and `PRAGMA foreign_key_check` was empty.
- logcat showed no app crash during the test.
- Clean-up: the database copies were deleted, the app was uninstalled from the emulator, the emulator's rotation setting was put back, the emulator was shut down, and the temporary worktree was deleted.

**Not run:**
- swap cards, the Review swap screen with a real swap, needs, suggestions, "Suggested now", and medicine statuses in the app. They need the student's DatePolicy and RotationPlanner, which are not written yet. The storage tests cover the swap and purchase-list saving.
- anything on the household phone
- TalkBack, large font or display size, and dark theme
- a release build

## Verified build: reminders and planning settings (2026-10-11)

Claude Code ran these in PowerShell and Git Bash, with `JAVA_HOME` set to Studio's JBR. The results are copied from the real output. One library was added: WorkManager 2.12.0.

| Check | Command | Real result |
|---|---|---|
| Dependencies | `.\gradlew.bat :app:dependencies --configuration debugRuntimeClasspath` | `androidx.work:work-runtime:2.12.0`. The `room-runtime:2.7.0` it asks for resolved to 2.8.5, and its Lifecycle 2.6.2 to 2.11.0 (the app's versions). The new downloads (`work-runtime`, `lifecycle-service` 2.11.0, `tracing-ktx` 1.2.0) came from Google Maven |
| Debug build, unit tests, lint (all tasks run again) | `.\gradlew.bat assembleDebug testDebugUnitTest lintDebug --rerun-tasks` | BUILD SUCCESSFUL. javac printed no warnings. The manifest merger printed only the two known warnings, for the INTERNET and CAMERA removals |
| Unit tests | (same run) | 157 tests, 0 failures, 0 errors. New: `ReminderCheckTest` 15, `ReminderTimingTest` 5; `DateThresholdsTest` 6 → 12 |
| Lint | (same run) | 0 errors, 3 warnings, the same as before: the template's unused colors `black` and `white`, and `TooManyViews` for `activity_batch_edit.xml` |
| Instrumented tests (emulator only) | `.\gradlew.bat assembleDebugAndroidTest`, then the three `adb -s emulator-5554` commands above | "OK (58 tests)". New: `ReminderFlowTest` 9, `ReminderSchedulerTest` 3, `ReminderPrefsTest` 2, `ReminderWorkerTest` 1, and `ReminderDeniedTest` and `ReminderRebootSetup` 1 each (these two skip themselves in this run). See the note below the table |
| Notifications refused (emulator) | `pm revoke …`, then the `ReminderDeniedTest` command above | "OK (1 test)". logcat showed the test start and finish, so it really ran |
| Reboot check (emulator) | the `ReminderRebootSetup` commands above, `dumpsys jobscheduler`, and `adb -s emulator-5554 reboot` | Before: one ReadyShelf job, minimum latency +1d23h59m58s, waiting only for its time delay. After the reboot (the boot finished after about 20 s; checked about 60 s later, without opening the app): the job was there again, with +1d23h58m25s left, and `WM-RescheduleReceiver` logged the BOOT_COMPLETED intent. No `WM-` errors, no SecurityException in ReadyShelf's process, and no crash. Cancel: "OK (1 test)", and no ReadyShelf job after the app was opened |
| APK contents | `aapt2 dump permissions` | Exactly POST_NOTIFICATIONS, WAKE_LOCK, RECEIVE_BOOT_COMPLETED, and AndroidX's app-private `DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. **No INTERNET, CAMERA, ACCESS_NETWORK_STATE, FOREGROUND_SERVICE, or exact-alarm permission** |
| Source languages | file search in `app/src` | 115 `.java` files, 0 `.kt`/`.kts` files |

Notes on the reminder tests:
- The emulator tests post real notifications, but with made-up batches and a policy that only returns preset answers (`FixedStatusPolicy`). It is not a DatePolicy.
- They use their own settings files and work names, so they never touch the app's own reminder file or schedule.
- The app "restart" in `ReminderFlowTest` is a new set of objects in the same process. `ReminderPrefsTest` also reads the settings file on disk, because Android keeps a copy of it in memory.
- One full run failed in `ReminderFlowTest.theTestReminder_isShownOnce_andNeverCountsAsTheDailyReminder`: "Notification 1002 was still shown after 5000 ms", in the clean-up. A second post that Android was still finishing came in after the clean-up's cancel; the test passed alone twice. Now the clean-up cancels again when needed, and the test waits a second after its second post. After that, 3 full runs in a row passed, and 1 more after the last app changes of this step.

**Checks by hand on the emulator (not the phone):**
- **Upgrade from commit 99d250d:** the old build was made in a temporary git worktree and installed, and 200 and 180 were saved in its Settings (the file showed `near_date_days` 200 and `replacement_days` 180). The new build was installed over it with `adb install -r`. It started without a ReadyShelf crash, and Planning settings showed 30 and 180. (The crash log had only aborts of `android.hardware.uwb-service`, an emulator hardware service.)
- **Planning settings:** 180 and 180 showed "Enter a number more than the near-date days (180), or lower the near-date days." on the replacement field.
- **Fresh install:** the "Daily reminder" switch was turned off with "Not available yet: the daily reminder needs the date rules (DatePolicy). …", and the status line said notifications are not allowed.
- **Notifications refused:**
  - "Send a test reminder" showed Android's dialog; "Don't allow". A second tap showed the dialog again; "Don't allow". A third tap showed no dialog, only the message. No notification was posted, and no reminder file was written.
  - The first message had three sentences, and Android cut it after two lines ("Everything else…"). It was shortened to "Notifications are not allowed. Everything else still works."; the how-to text and "Open notification settings" are right above the buttons. The rare "settings page missing" message became a dialog.
  - With notifications refused, Home, Inventory ("Showing 6 of 6 batches" with sample data), Add batch, and the Purchase list opened and worked.
- **"Open notification settings"** opened Android's notification page for ReadyShelf. After notifications were turned on there, Back returned to Settings, and the status line said "Notifications from ReadyShelf are allowed."
- **Test reminder:** two taps gave exactly one notification: ID 1002, channel `review_reminders`, importance low (2), private with a public version, `LOCAL_ONLY`, category reminder. The message said "Test reminder sent. Look in your notifications.", and the shade listed it under "Silent".
- **Lock screen** (a PIN was set for this check and removed afterwards):
  - With the emulator's own settings, the silent reminder was not shown on the lock screen at all.
  - With silent notifications allowed on the lock screen, it showed only its icon. The shade, pulled down while locked, showed only the generic test text.
  - With "hide sensitive content" (`lock_screen_allow_private_notifications` 0), the system's own notification lost its text, and ReadyShelf's showed its public version with the same generic words.
  - The two settings were put back (1, and not set), and the PIN was removed.
- **Tapping the notification:**
  - **Found and fixed:** with Settings open, the tap only brought ReadyShelf to the front, on Settings. The task had been started with `am start -n` (an intent without an action), so Android saw the same intent (see "Notification taps" above). The same would happen to a household whose ReadyShelf was started by a reminder tap.
  - After the fix, started like the launcher (MAIN/LAUNCHER) → Settings → test reminder → tap: a new Home on top (Home → Settings → Home) with "Needs review" on screen; Back returned to Settings. In landscape, where Home is taller than the screen, Home opened scrolled to "Needs review".
  - With no ReadyShelf task, a tap started ReadyShelf on Home; the task's first intent was the review intent. Then Settings → another test reminder → the Home key → tap: a new Home on top of Settings.
  - Opening ReadyShelf from the launcher intent after that put a second Home on top of Settings (a known limit; see SPEC.md).
- **Force-stop and relaunch:** no crash; "No daily reminder has been shown yet." didn't change; no ReadyShelf job (the switch stays off until DatePolicy is connected).
- **Notifications turned off in Android's settings while ReadyShelf was in the background:** the permission became not granted and Android stopped the app's process. Opening it from recents showed Settings again in a new process, with no crash, and the status line said notifications are not allowed.
- **Clean-up:** both packages were uninstalled from the emulator, the rotation setting was put back (1 and 0), the lock-screen settings and the PIN were put back, the worktree was deleted, and the emulator was shut down.

**Not run:**
- a real daily reminder from the background check, and the switch's permission flow. They need the student's DatePolicy; the flow tests cover the check with preset answers, and the test reminder uses the same permission code.
- the dialog for a phone without a notification settings page (the emulator has the page)
- tapping a reminder while Home is already in front (`onNewIntent`)
- anything on the household phone
- TalkBack, large font or display size, and dark theme
- a release build

## Review of the first version (2026-10-11)

The results of the review against SPEC.md are in TEST-RESULTS.md: the build, unit and instrumented test runs, the emulator checks (including an upgrade from database version 1 and a restart check), the repairs, the open defects, and the phone checklist for the student. No tool or library version changed.

- The instrumented tests ran with the three `adb -s emulator-5554` commands above; there are now 69 (the new `ReadyShelfRepositoryTest` included).
- The review also ran the case "tapping a reminder while Home is already in front", listed as not run above: the same Home got the tap (no new screen) and scrolled to "Needs review".

## Verified build: sample data and demo (2026-10-12)

Claude Code ran these in PowerShell and Git Bash, with `JAVA_HOME` set to Studio's JBR, after moving sample data to its own database file (see SPEC.md, "Sample data", and DEMO.md). The results are copied from the real output. No tool or library version changed.

| Check | Command | Real result |
|---|---|---|
| Debug build, test APK, unit tests, lint (every task run again) | `.\gradlew.bat assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug --rerun-tasks` | BUILD SUCCESSFUL in 23s, 80 actionable tasks: 80 executed. The manifest merger printed its 2 known warnings (the INTERNET and CAMERA removals) |
| javac warnings | `.\gradlew.bat compileDebugJavaWithJavac compileDebugUnitTestJavaWithJavac compileDebugAndroidTestJavaWithJavac --rerun-tasks` | No javac warnings or notes. Gradle printed its known notice "Deprecated Gradle features were used in this build" (see "Known warnings") |
| Unit tests | (the first run) | 181 tests, 0 failures, 0 errors, 0 skipped. New: `SampleDataTest` 14 |
| Lint | (the first run) | 0 errors, the same 3 warnings: the unused colors `black` and `white`, and `TooManyViews` in `activity_batch_edit.xml` |
| Database schema | `git diff --exit-code -- app/schemas` | No change: the database stays version 3 (the new `SampleResetDao` adds no table) |
| Instrumented tests (emulator only) | the three `adb -s emulator-5554` commands in "Migration test without room-testing" | "OK (78 tests)", with the final APKs. New: `DataSetsTest` 9; `ReadyShelfRepositoryTest` 7 (its `loadSampleData` test moved to `DataSetsTest`, and one test of removing old sample items was added) |
| APK contents | `aapt2 dump permissions` | The same four: POST_NOTIFICATIONS, WAKE_LOCK, RECEIVE_BOOT_COMPLETED, and AndroidX's app-private permission. **No INTERNET permission** |
| Debug APK | file check | `app\build\outputs\apk\debug\app-debug.apk`, 31,387,790 bytes, built 2026-10-12 09:13 |
| Source languages | file search in `app/src` | 121 `.java` files, 0 `.kt`/`.kts` files |
| Signing keys | `git check-ignore -v`, `git ls-files` | `*.jks`, `*.keystore`, and `keystore.properties` are ignored. No key file is tracked, and no build file has a signing config or password |

**Checks by hand on the Pixel_10a emulator (Android 17), not the phone.** They used the build made just before the final one; the only later change reworded one code comment in `SettingsViewModel`.
- **Opening sample data:**
  - One household batch ("Rice", 2 bags) was added first.
  - Settings → Open sample data → the dialog → Home again, as the only screen of the task.
  - The sample file had exactly the rows in SPEC.md, "Sample data", dated from 12 Oct 2026. Its planning settings were 30 and 180.
- **The label was on 7 of the 8 screens:** Home, Inventory, Batch details, Add batch and Edit batch, Change stock (Record discarded), Purchase list, and Settings.
  - Review swap could not be opened: it needs RotationPlanner. It uses the same `ScreenSetup`.
  - The label stayed after a rotation, a force-stop and cold start, and at font scale 2.0, where it wraps to three lines.
- **What the sample data showed:**
  - Inventory "gran" showed 3 of 5. The 3-bar batch read "Best by: “07/2027” (incomplete date)" with "Date needs review", and its Edit form offered "Use the last day of the month (31 Jul 2027)". It was left without saving.
  - The medicine batch read "Expires: 2 Oct 2026". Record discarded showed the no-advice note. Home showed the medicine note and "Not available yet".
- **Manual entry while sample data was open** ("Bottled water", 12 bottles, Earthquake kit, 12 Oct 2027, confirmed) went only into the sample file.
- **The household's database, compared row by row:**
  - It was identical after the manual entry.
  - It was identical after "Close sample data".
  - "Start sample data again" removed the hand-added water.
- **A launcher start** with Settings open brought the task back with no second Home.
- **Airplane mode on:**
  - "Active default network: none".
  - Inventory search worked, and "Start sample data again" worked.
  - The test reminder was posted (ID 1002, "Test reminder").
  - The app's log had no network errors: its one line with "connect" was a keyboard message.
  - Airplane mode was then turned off.
- **Closing:**
  - Home showed no label, and Inventory showed only the household's batch.
  - The sample file had 0 rows in all six tables, and `data_mode` said `false`.
- **Old sample items:** one old-style sample row was put into a copy of the test database.
  - Home showed the old-sample banner, and Settings showed "Your own records have 1 made-up sample item…".
  - With sample data open, both were hidden, and the household's rows stayed identical.
  - "Remove old sample items" brought the rows back to the original ones.
- **Clean-up:**
  - Both packages were uninstalled from the emulator.
  - Font scale (1.0), rotation (1 and 0), and airplane mode (off) were checked.
  - The emulator was shut down.
- **Crash log:** no ReadyShelf crash both times it was read (after the airplane-mode steps, and after closing sample data).

**The phone (ZY22KN9QJP, moto g play 2024, Android 14), at the student's request:**
- **Before:** ReadyShelf was last updated 2026-10-04 19:25:54, which means database version 1. The student chose to install anyway, after being told about the one-way upgrade.
- **A copy first:** the three database files were copied into ReadyShelf's own private folder on the phone (`files/pre-upgrade-2026-10-12`). `cmp` showed the copies identical. The copy never left the phone.
- **Install:** `adb -s ZY22KN9QJP install -r app\build\outputs\apk\debug\app-debug.apk` gave "Success". `lastUpdateTime` became 2026-10-12 09:21:47, and the first install time (2026-10-04 13:26:35) stayed the same, so it was an update with the data kept.
- **One start:** `am start -W -n com.example.readyshelf/.ui.home.HomeActivity` gave "Status: ok" (cold start, 1316 ms), and HomeActivity was the resumed activity. The phone stayed locked and was not touched or woken.
  - The database header's version field (bytes 60–63, no rows read) changed from version 1 to **3**.
  - The crash log had no ReadyShelf lines. The app's only error line was `OpenGLRenderer: Unable to match the desired swap behavior.`, a graphics message.
- **Not done on the phone:** no sample data was opened, no data was read or changed by hand, and no setting was changed.

## Verified build: photo entry (2026-10-12)

Claude Code ran these in PowerShell and Git Bash, with `JAVA_HOME` set to Studio's JBR, after adding photo entry (SPEC.md, "Photo entry"; AI-USE.md). The results are copied from the real output. Libraries added: CameraX 1.6.2 (four artifacts) and concurrent-futures 1.1.0 (compile only).

| Check | Command | Real result |
|---|---|---|
| Dependencies | `.\gradlew.bat :app:dependencies` (debugRuntimeClasspath and debugAndroidTestRuntimeClasspath), before and after | Both resolved. Runtime: 91 → 116 libraries, listed in "Photo entry and CameraX". No library the app declares changed version |
| First compile | `.\gradlew.bat :app:compileDebugJavaWithJavac --rerun` | Failed: `Cannot attach type annotations … PendingValue.mCompleterAndValue` and `class file for androidx.concurrent.futures.CallbackToFutureAdapter not found`. Fixed with the compile-only library (see "Photo entry and CameraX") |
| Build, test APK, unit tests, lint (every task run again) | `.\gradlew.bat assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug --rerun-tasks` | The final run, after the lint fixes below: BUILD SUCCESSFUL in 14s, 80 actionable tasks: 80 executed. No javac warnings or notes. The manifest merger printed 1 known warning (the INTERNET removal; the CAMERA removal no longer exists) |
| Unit tests | (same run) | 197 tests, 0 failures, 0 errors, 0 skipped |
| Lint | `.\gradlew.bat lintDebug` after two fixes | 0 errors, the same 3 warnings as before (`black`, `white`, `TooManyViews`). The first run had 3 more: NestedWeights in the new layouts (fixed with a ConstraintLayout chain) and GradleDependency for concurrent-futures (kept on purpose, with a `#noinspection` comment and the reason) |
| APK permissions | `aapt2 dump permissions` | CAMERA, POST_NOTIFICATIONS, WAKE_LOCK, RECEIVE_BOOT_COMPLETED, and AndroidX's app-private permission. **No INTERNET, ACCESS_NETWORK_STATE, or FOREGROUND_SERVICE** |
| APK features | `aapt2 dump badging` | `uses-feature-not-required` for `camera`, `camera.any`, `camera.autofocus`, and `camera.flash`; only the default `faketouch` is implied. minSdk 26, targetSdk 37 |
| Merged manifest | `aapt2 dump xmltree` | No FileProvider. `LabelPhotosActivity` exported=false. CameraX's `MetadataHolderService` enabled=false, exported=false |
| 16 KB | `zipalign -c -P 16 -v 4`, the ELF script, `getconf PAGE_SIZE`, `dumpsys package` | "Verification successful"; every 64-bit library aligned to 0x4000 (Tesseract's 32-bit libraries 0x1000); emulator page size 16384, `pageSizeCompat=0` |
| APK size | file size | `app\build\outputs\apk\debug\app-debug.apk`: 34,972,951 bytes |
| Source languages | file search in `app/src` | 132 `.java` files, 0 `.kt`/`.kts` files |
| Instrumented tests (emulator only) | the three `adb -s emulator-5554` commands in "Migration test without room-testing" | "OK (96 tests)" (78 before). New: `LabelPhotoReaderTest` 6, `BatchEditPhotoResultTest` 12 |
| Offline | `cmd connectivity airplane-mode enable`, then the two new test classes | "Active default network: none"; "OK (18 tests)"; no network lines from ReadyShelf in logcat |

The checks by hand on the emulator are in TEST-RESULTS.md, section 9.
