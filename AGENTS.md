# Agent instructions — Zone News Android

## Read this first

This repository is mid-way through a multi-session port bringing the Android app to feature and
visual parity with the iOS app.

**Before doing any work, read [`IOS_PARITY_PLAN.md`](IOS_PARITY_PLAN.md)** and follow its rules
section. It holds the phase breakdown, the file-level iOS → Android mapping, the progress log, and
the verification workflow. This file is only a pointer; the plan is the source of truth.

Do not start a phase without checking the progress log to see what the previous session did and
what the human verifier reported.

## Build

```bash
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
./gradlew assembleDebug
```

JDK 17 is pinned via `org.gradle.java.home` in `~/.gradle/gradle.properties`. The SDK path is in
`local.properties` (gitignored).

There is no emulator or connected device on this machine. Work is compile-verified by the agent,
then checked on a real device by a human after each phase. The debug APK for handoff is at
`app/build/outputs/apk/debug/app-debug.apk`.

## The iOS reference

Located at `Newsletter_iOS copy/`. **Read-only — never edit anything inside it.**

`ZNews/` and `Zone News/` are near-identical duplicate targets differing only in branding assets.
Treat `ZNews/` as canonical and ignore `Zone News/` so the same file is not ported twice.

It is a large tree (~4.2 GB including Xcode build output). It is not tracked by git. When searching
it, pass the path explicitly — a default workspace search will not reliably cover it, and an agent
that forgets this will wrongly conclude a feature does not exist.

## Hard constraints

- **No Jetpack Compose.** This project is XML layouts + ViewBinding with `compose = false`. Match
  the existing patterns in `ui/`, `model/`, and `repository/`.
- **Localize everything.** Every user-facing string goes in `strings.xml` and all four locales:
  `values`, `values-en`, `values-zh-rCN`, `values-zh-rHK`. No hardcoded strings in layouts or Kotlin.
- **Light and dark mode.** Colours go through theme attributes, never raw hex in a layout. Verify
  `values-night` alongside `values`.
- **Menu and icon tinting.** Inherited tint will override symbol colours. Define every icon, text,
  and button colour explicitly per state, and confirm nothing falls back to the accent colour.
- **Never commit secrets.** Release signing reads from `keystore.properties`, which is gitignored.
  Do not reintroduce credentials into `app/build.gradle`.

## Before you finish a session

1. `./gradlew assembleDebug` must pass.
2. Tick only the checkboxes in `IOS_PARITY_PLAN.md` you actually verified.
3. Set the phase status. A phase moves to `Awaiting verification`, not `Done` — only the human
   verifier marks a phase `Done`.
4. Add a dated entry to the progress log covering decisions made, deviations, files touched, and
   anything left broken.
