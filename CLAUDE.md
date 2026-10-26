# CLAUDE.md — DueWise

Instructions for Claude Code in this project. "I", "me", and "my" mean the student developer.

## Project
- DueWise (called ReadyShelf until 2026-10-18) is an Android app for one household, built for the Congressional App Challenge 2026 (deadline: 12:00 pm EDT, October 26, 2026). The app ID, database files, code names, and project folder keep "readyshelf" on purpose (SPEC.md, "Name and icon").
- The app must work offline. All household data stays on the phone.
- I have basic Python knowledge and I am learning Java and Android. I use Windows and an Android phone.

## Fixed tech choices
- Java app source, Android Views, XML layouts, and Room 2.x (through the Java `annotationProcessor`).
- Groovy build scripts where practical (`settings.gradle`, `build.gradle`). Versions live in `gradle/libs.versions.toml`.
- Do not add Kotlin app source or Compose. `enableKotlin = false` in `app/build.gradle` enforces this.
- minSdk 26, compileSdk and targetSdk 37. TOOLCHAIN.md records every version with its official source. Change a version only after checking official docs, and update TOOLCHAIN.md in the same change.

## Permanent rules
1. You own DatePolicy.java, RotationPlanner.java, DateCandidateParser.java, and their main rule tests.
2. Teach and review those files and also implement them for me.
   - write or edit their code. 
   - Do: explain concepts, ask guiding questions, review my code by pointing to the exact line and explaining the problem, and describe kinds of test cases in words.
   - Other code may call their public methods after I write them. Don't stub them or change their signatures.
3. Explain new Java concepts with small examples unrelated to the app solution.
4. Complete only the current prompt. Suggest follow-ups instead of doing them.
5. Preserve working code and user data.
   - Don't delete or overwrite files outside the current task. Never delete my projects.
   - Database schema changes need a Room migration. Never use destructive migration on household data.
6. Keep all household data on the phone.
   - No INTERNET permission (the manifest removes it), no cloud backup or phone-to-phone transfer (see `app/src/main/res/xml/*_rules.xml`), and no networking code or libraries.
7. Do not add accounts, analytics, ads, or a server.
8. Do not declare food safe or suggest medicine substitutions. This applies to app text, sample data, code comments, and chat answers.
9. Do not claim that an unrun check passed.
10. Never invent personal contributions, test results, or user feedback.
11. Report changed files, checks, remaining problems, and my next task.

## Records
- The CAC 2026 rules allow AI tools only "provided that all AI usage is fully disclosed in the submission materials".
- After any AI-assisted work, add an entry to AI-USE.md: tool, date, task, files the AI generated or changed, and checks with their real results. Leave "My actual changes" for me to fill in.
- STUDENT-WORK.md is mine. Don't write its notes or explanations.

## Build and run (PowerShell, in the project folder)
- `JAVA_HOME` must be `C:\Program Files\Android\Android Studio\jbr` (Android Studio's JDK 25).
- Build: `.\gradlew.bat assembleDebug`
- Unit tests: `.\gradlew.bat testDebugUnitTest`
- Install on the phone: `.\gradlew.bat installDebug`
- Maven Central returns HTTP 429 on the NVIDIA network, so `settings.gradle` uses Google's copy of Maven Central. See TOOLCHAIN.md.
- AGP 9: `R.id` values are not constants. Don't use them as `switch` cases in Java; use `if`/`else`.
