# iOS Parity Plan — Zone News Android

**Goal:** bring the Android app to feature and visual parity with the iOS app, as closely as the
platform allows. Some iOS capabilities have no Android equivalent; those are listed in
[Out of scope](#out-of-scope-ios-exclusive) and must not be faked.

**This document is the shared source of truth across agent conversations.** No single context
window can hold both codebases, so each session works one phase (or one sub-task) at a time and
records what it did here before finishing.

---

## Rules for agents

1. **Read this file first.** Check [Progress log](#progress-log) to see what is already done and
   what the previous session learned. Do not re-derive the mapping from scratch.
2. **One phase per session, or less.** Do not attempt to read both codebases wholesale. Read only
   the iOS file(s) for the current sub-task plus the Android files you are editing.
3. **Update this file before you finish.** Tick the checkboxes you completed, set the phase status,
   and add a dated entry to the [Progress log](#progress-log) with anything the next agent needs:
   decisions made, deviations from this plan, files touched, and known-broken items.
4. **Build before you claim done.** Run `./gradlew assembleDebug` (see [Environment](#environment)).
   A phase is not complete if the build is red.
5. **Never mark a checkbox you did not verify.** An unverified item is worse than an open one,
   because the next agent will build on top of it.
6. **You cannot mark a phase `Done`.** Agents set `Awaiting verification`; only the human verifier
   sets `Done`. See [Verification workflow](#verification-workflow). Before starting a phase, check
   whether the previous one is still awaiting verification, and read its reported issues first.
7. **Localize everything.** Every user-facing string goes into `strings.xml` and all four locale
   variants. No hardcoded strings in layouts or Kotlin.
8. **Light and dark mode both.** Every colour goes through the theme attributes, never a raw hex in
   a layout. Verify `values-night` alongside `values`.
9. **Match the existing Android stack.** This project is XML layouts + ViewBinding + Fragments,
   `compose = false`. Do not introduce Jetpack Compose. Follow the patterns already in
   `ui/`, `model/`, and `repository/`.

---

## Environment

Set up during Phase 0 on this machine. Everything lives outside the repo except `local.properties`.

| Component | Location / value |
| --- | --- |
| JDK | `/opt/homebrew/opt/openjdk@17` (Homebrew `openjdk@17`, keg-only) |
| Android SDK | `/opt/homebrew/share/android-commandlinetools` |
| SDK packages | `platform-tools`, `platforms;android-34`, `build-tools;34.0.0` |
| Gradle JDK pin | `~/.gradle/gradle.properties` → `org.gradle.java.home` |
| SDK path | `local.properties` → `sdk.dir` (gitignored) |

Build command:

```bash
cd /Users/fei/Downloads/Newsletter_Android
export ANDROID_HOME=/opt/homebrew/share/android-commandlinetools
./gradlew assembleDebug
```

Release signing reads from `keystore.properties` (gitignored, see `keystore.properties.example`).
If that file is absent, release builds are produced unsigned and debug builds are unaffected.

---

## Verification workflow

There is **no emulator and no connected device on the build machine**. Agents can only verify that
code compiles. A green build is a weak signal on Android: Hilt graph errors, ViewBinding nulls,
adapter mismatches, JSON parsing failures against real responses, and resources missing from a
locale or from `values-night` all compile cleanly and fail at runtime.

Every phase is therefore checked on a real device by a human verifier before the next phase starts.

**The loop:**

1. Agent completes the phase, gets `./gradlew assembleDebug` green, sets the phase status to
   `Awaiting verification`, and writes a progress log entry.
2. The debug APK is handed to the verifier from:

   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

   Roughly 30 MB, signed with AGP's auto-generated debug key. The verifier needs only this file —
   no repo and no toolchain. Install with `adb install -r app-debug.apk`, or sideload it.
3. The verifier exercises the phase against the iOS app side by side and reports what is wrong.
4. **Those findings are recorded in this file**, in the phase's `Verification` block — not left in
   chat. A finding that lives only in a message is invisible to the next agent, which will read a
   ticked checkbox and build on top of a known-bad screen.
5. Only once the findings are resolved or explicitly carried forward does the phase become `Done`.

**Agents:** if the previous phase is `Awaiting verification` or has open issues in its
`Verification` block, read them before starting new work. Fix reported issues in the phase they
belong to rather than papering over them in a later one.

---

## The two codebases

### iOS — reference, read-only

Lives in `Newsletter_iOS copy/`. Roughly 70,000 lines across 202 Swift files, SwiftUI +
Combine + `ObservableObject`.

> **Important:** `ZNews/` and `Zone News/` are near-identical duplicate targets that differ only in
> app icons and branding assets. Treat `ZNews/` as canonical and ignore `Zone News/` unless you are
> specifically looking at icons. Do not port the same file twice.

| Path | Contents |
| --- | --- |
| `ZNews/Models/` | `NewsArticle`, `AppState`, `UserAuthentication`, `Subscription`, `TopicCategory`, `ArticleNote`, `PersonalizationSettings`, `Notification` |
| `ZNews/Services/` | `NetworkService`, `TTSService`, `KeychainService`, `SubscriptionService`, `NotesStore`, `SpotlightService`, `SearchHistoryStore` |
| `ZNews/ViewModels/` | One per screen — Home, NewsDetail, NewsList, Search, Personal, Notification, ReadingHistory, TopicNews, TopicSelection |
| `ZNews/Utils/` | Caching, haptics, network monitor, deep links, trivia, tutorial, ratings, formatters |
| `Views/` | All SwiftUI screens (69 files), grouped by feature |

Largest files, read these in sections rather than whole:

| File | Lines |
| --- | --- |
| `Views/Shared/NewsDetailView.swift` | 6,476 |
| `Views/Shared/TimelinePageView.swift` | 2,210 |
| `ZNews/Services/NetworkService.swift` | 1,908 |
| `Views/Account/AccountView.swift` | 1,881 |
| `Views/Account/SubscriptionView.swift` | 1,299 |
| `Views/Shared/NewsArticleRow.swift` | 1,128 |
| `ZNews/Models/AppState.swift` | 1,061 |
| `Views/Search/SituationMonitorView.swift` | 1,008 |
| `ZNews/Models/UserAuthentication.swift` | 995 |

### Android — target

Roughly 32,500 lines across 156 Kotlin files, 102 XML layouts, 426 resource files.
`compileSdk 34`, `minSdk 24`, AGP 8.13.2, Gradle 8.14, Kotlin 2.0.21, `jvmTarget 17`.

Stack: Fragments + Activities + ViewBinding, Hilt, Retrofit + RxJava2, Glide, MMKV,
SmartRefreshLayout, BaseAdapter (BRVAH), Firebase (Auth / Messaging / Crashlytics),
Play Billing 7.1.1, WorkManager.

Locales: `values`, `values-en`, `values-zh-rCN`, `values-zh-rHK`, plus `values-night`.

### Navigation gap

The tab structures do not line up, which is why Phase 3 exists.

| iOS tab | Android equivalent today |
| --- | --- |
| Home | `HomeFrag` (tag `A`) |
| Timeline | *missing* |
| World | *missing* |
| Recap | `RecapFragment` exists but is not a tab |
| Search | `SearchFrag` (tag `C`) |
| *(Account is not a tab on iOS)* | `MyFrag` (tag `E`) |
| — | `AdviceFrag` (tag `B`) |

---

## Feature gap summary

Confirmed **absent** on Android:

- Text-to-speech (`TTSService`, `FloatingTTSPlayer`)
- Podcast player
- YouTube live feed (`LiveFeedView`, `YouTubePlayerView`)
- Situation Monitor (`SituationMonitorView` + settings sheet)
- Article notes (`AllNotesView`, `ArticleNotesView`, `NotesStore`)
- Offline trivia (`OfflineTriviaView`, `TriviaManager`)
- `WorldView`, `HongKongRegionOverview`
- `EditPersonaView`
- App icon chooser (`IconChooser`)
- Third widget family (iOS has 3, Android has 2)

Already **present** on Android and only needing visual/behavioural alignment: Recap, Levity,
quotes, sentiment and subjectivity meters, related news, publisher distribution, billing,
tutorial overlay, topic management, reading history, saved articles, notifications, settings.

### Out of scope (iOS-exclusive)

Do not attempt direct ports of these. Where a row lists an alternative, implement that instead and
note the deviation in the progress log.

| iOS feature | Android approach |
| --- | --- |
| Live Activities / Dynamic Island | Media-style foreground notification |
| Spotlight indexing | App Search / shortcuts, or skip |
| StoreKit | Already covered by Play Billing |
| Share Extension | Android share sheet (`ACTION_SEND` intent filter) |
| Handoff / Continuity | Skip |
| SwiftUI-specific motion and material effects | Approximate with existing BlurView + transitions; do not block a phase on pixel-perfect blur |

---

## Phases

Status values: `Not started` · `In progress` · `Blocked` · `Awaiting verification` · `Done`

Agents may set any status except `Done`. See [Verification workflow](#verification-workflow).
Each phase carries a `Verification` block; leave it as-is until the phase is checked on a device,
then record the outcome there.

---

### Phase 1 — Data layer

**Status:** Not started

Everything downstream depends on the response models matching, so this comes first.

- [ ] Port `ZNews/Services/NetworkService.swift` (1,908 lines) onto the existing Retrofit setup in `utils/network/` and `net/AppHttpService.kt`. Reconcile against `api_documentation.md`.
- [ ] Port `ZNews/Models/NewsArticle.swift` (920) — align field names with existing `entry/` and `model/` classes.
- [ ] Port `ZNews/Models/AppState.swift` (1,061) — Android has no single equivalent; map onto Hilt-scoped holders plus MMKV, do not create a god object.
- [ ] Port `ZNews/Models/UserAuthentication.swift` (995) against Firebase Auth + `LoginRepository`.
- [ ] Port remaining models: `Subscription`, `TopicCategory`, `ArticleNote`, `PersonalizationSettings`, `Notification`.
- [ ] Map `KeychainService` onto EncryptedSharedPreferences (not plain MMKV — it holds credentials).
- [ ] Reconcile caching: `FeedCache`, `ImageCache`, `PublisherLogoCache`, `CacheManager` against `ImageCacheManager.kt` and Glide.
- [ ] `./gradlew assembleDebug` green.

**Verification:** _Not yet checked on device._

---

### Phase 2 — Design system and shared components

**Status:** Not started

These appear on every screen. Getting them right once buys parity everywhere, so this must land
before any screen work.

- [ ] Extract the iOS palette from `ZNews/Extensions/Color+Extensions.swift` and `ZNews/Assets.xcassets` into theme attributes in `values/colors.xml` and `values-night/colors.xml`. Cross-check `docs/colors_reference.xml`.
- [ ] Typography scale and spacing tokens as reusable styles.
- [ ] `Views/Shared/NewsArticleRow.swift` (1,128) → the row layouts used by `NewsAdapter`, `YourFeedAdapter`, `LevityArticleAdapter`.
- [ ] `Views/Shared/AppHeader.swift` → shared toolbar.
- [ ] `Views/Shared/AppThrobber.swift` + `LoadingAnimationView.swift` → loading states (Shimmer is already a dependency).
- [ ] `Views/Shared/ArticleLongPressMenu.swift` (882) → long-press menu. **Set icon tints explicitly per state** via `MenuItem` icon drawables with explicit tint; do not rely on theme inheritance, which will silently override state-critical icons such as checkmarks.
- [ ] `Views/Shared/CachedAsyncImage.swift`, `AsyncImageWithPlaceholder.swift`, `CachedPublisherLogo.swift` → Glide wrappers with matching placeholder behaviour.
- [ ] `Views/Shared/ErrorView.swift`, `OfflineConnectionView.swift`, `TransientBannerMessage.swift`.
- [ ] `Views/Shared/CatMascotView.swift` → align with existing `widget/CatMascotView.kt`.
- [ ] `./gradlew assembleDebug` green.

**Verification:** _Not yet checked on device._

---

### Phase 3 — Navigation shell

**Status:** Not started

- [ ] Restructure `ui/MainActivity.kt` from 4 fragments to the iOS five-tab layout: Home, Timeline, World, Recap, Search.
- [ ] Relocate Account out of the tab bar to match iOS placement; decide the fate of `AdviceFrag`.
- [ ] Preserve the existing fragment preloading and state-restoration behaviour in `MainActivity` — it exists to prevent tab-switch lag, do not regress it.
- [ ] Port tab bar iconography and selected/unselected states from `Views/MainTabView.swift` (782).
- [ ] Wire deep links against `ZNews/Utils/DeepLinkCoordinator.swift`; Android already has partial handling in `MainActivity`.
- [ ] `Views/Shared/LaunchScreenView.swift` → splash.
- [ ] `./gradlew assembleDebug` green, all five tabs navigable.

**Verification:** _Not yet checked on device._

---

### Phase 4 — News detail

**Status:** Not started

Largest and most-used screen. Android already has much of this; the work is closing gaps rather
than starting over. Read `NewsDetailView.swift` in sections.

- [ ] `Views/Shared/NewsDetailView.swift` (6,476) → `ui/newsdetail/NewsDetailActivity.kt`.
- [ ] `Views/Shared/NewsDetailSmartLayout.swift` (751) → adaptive layout behaviour.
- [ ] `Views/Shared/ArticleWebView.swift` (755) + `PageFindInPage.swift` → WebView with find-in-page.
- [ ] Align existing components against iOS: `QuotesAdapter`, `SentimentMeterView`, `SubjectivityScoreView`, `RelatedNewsView`, `PublisherDistributionView`, `ScreenshotsCarouselAdapter`.
- [ ] `Views/Shared/EnhancedShareSheet.swift` + `ZNews/Utils/ShareMetadataGenerator.swift` → share sheet.
- [ ] `Views/Shared/TimelinePageView.swift` (2,210) → timeline view (also feeds Phase 5).
- [ ] `./gradlew assembleDebug` green.

**Verification:** _Not yet checked on device._

---

### Phase 5 — Feed screens

**Status:** Not started

- [ ] `Views/Home/HomeView.swift` + `NewsListView.swift` + `HomeMenuView.swift` → `HomeFrag` / `HomeChildFrag`.
- [ ] Timeline tab, reusing the Phase 4 timeline work.
- [ ] `Views/Home/WorldView.swift` and `HongKongRegionOverview.swift` → new World tab. Cross-check `utils/RegionMappingUtils.kt`.
- [ ] `Views/Recap/RecapView.swift` + `ZNews/Views/Personal/PersonalRecapView.swift` + `RecapScopePicker.swift` → `RecapFragment` / `selfview/RecapView.kt`.
- [ ] `Views/Personal/` — `PersonalFeedView`, `PersonalSubTabView`, `LevityFeedView`, `LevityDetailView`, `TrendingTopicsCrawlRow`.
- [ ] `Views/Personal/TopicSelectionView.swift` → `ui/topicmodify/`.
- [ ] `Views/Shared/PersonalizationDropDown.swift` + `PersonalizationSummaryView.swift` + `FeedLayoutStyleSubmenu.swift`.
- [ ] `Views/Search/SearchView.swift` → `SearchFrag`.
- [ ] `./gradlew assembleDebug` green.

**Verification:** _Not yet checked on device._

---

### Phase 6 — Account, subscription, settings, notes

**Status:** Not started

- [ ] `Views/Account/AccountView.swift` (1,881) → `MyFrag`.
- [ ] `Views/Account/SubscriptionView.swift` (1,299) + `ManageSubscriptionView.swift` → `billing/BillingManager.kt` and `SubscriptionBottomSheetFragment`.
- [ ] `Views/Account/ReadingHistoryView.swift` (771) → `BrownHisActivity` / `ReadingHistoryBottomSheetFragment`.
- [ ] `Views/Account/TopicNewsView.swift`, `AboutView.swift`, `EditPersonaView.swift`.
- [ ] `Views/Login/LoginView.swift` + `RegisterView.swift` → `ui/login/`. Note `ContinueWithAppleButton` has no Android equivalent; keep Google and Facebook.
- [ ] `Views/Notes/AllNotesView.swift` + `ArticleNotesView.swift` + `NotesStore` → **new feature**, no Android code exists.
- [ ] `Views/Notification/NotificationView.swift` → `NoticeListActivity`.
- [ ] `Views/Shared/TutorialOverlayView.swift` → align with `selfview/TutorialOverlayView.kt`.
- [ ] `Views/Shared/FeedbackSheetContent.swift` → `FeedbackBottomSheetFragment`.
- [ ] `./gradlew assembleDebug` green.

**Verification:** _Not yet checked on device._

---

### Phase 7 — TTS and audio

**Status:** Not started

Entirely new subsystem on Android. Nothing to align against.

- [ ] `ZNews/Services/TTSService.swift` (812) → Android `TextToSpeech` engine wrapper.
- [ ] Foreground media service with `MediaSession` for lock-screen and notification controls.
- [ ] `Views/Shared/FloatingTTSPlayer.swift` → persistent mini-player UI across tabs.
- [ ] `PodcastPlayer/PodcastPlayer.swift` (976) → podcast playback.
- [ ] Replace the iOS Live Activity with a media-style notification (see [Out of scope](#out-of-scope-ios-exclusive)).
- [ ] Audio focus, headphone disconnect, and interruption handling.
- [ ] `./gradlew assembleDebug` green.

**Verification:** _Not yet checked on device._

---

### Phase 8 — Widgets, sharing, and remaining features

**Status:** Not started

- [ ] Compare iOS widget families (`SmallWidget`, `CompactViewWidget`, `TraditionalViewWidget`) against `widget/CompactNewsWidgetProvider.kt` and `DetailedNewsWidgetProvider.kt`; add the third.
- [ ] `Views/Personal/WidgetConfigurationView.swift` → `widget/WidgetConfigActivity.kt`.
- [ ] Share target — `ACTION_SEND` intent filter replacing the iOS Share Extension.
- [ ] `Views/Search/SituationMonitorView.swift` (1,008) + `SituationMonitorSettingsSheet.swift` + `SituationMonitorButton.swift` → **new feature**.
- [ ] `Views/LiveFeed/` + `Views/Search/LiveFeedView.swift` (961) → YouTube live feed, **new feature**.
- [ ] `Views/Home/OfflineTriviaView.swift` + `TriviaManager.swift` → **new feature**.
- [ ] `Views/Shared/IconChooser.swift` → align with `utils/AppIconManager.kt`.
- [ ] `ZNews/Utils/AppRatingManager.swift` → Play In-App Review.
- [ ] `./gradlew assembleDebug` green.

**Verification:** _Not yet checked on device._

---

### Phase 9 — Localization and polish

**Status:** Not started

- [ ] Audit every string added in Phases 1–8; no hardcoded user-facing text anywhere.
- [ ] Fill all four locales: `values`, `values-en`, `values-zh-rCN`, `values-zh-rHK`. iOS ships an 868 KB string catalog against Android's 773-line `strings.xml`, so expect a large delta.
- [ ] Verify every screen in light and dark mode.
- [ ] Accessibility pass against `ZNews/Utils/AccessibilityManager.swift` — content descriptions, touch targets, font scaling.
- [ ] `ZNews/Utils/HapticFeedback.swift` → align with `utils/HapticFeedbackHelper.kt`.
- [ ] Bump `targetSdk` from 34 to meet the current Play Store minimum, and retest.
- [ ] Full regression pass on a device or emulator.

**Verification:** _Not yet checked on device._

---

## Known issues carried into this work

Found during Phase 0, not yet fixed:

- `settings.gradle` references `https://maven.aliyun.com/repository/jcenter`, a mirror of the
  sunset JCenter. It may be slow or dead depending on location. If dependency resolution hangs,
  this is the first thing to remove.
- `targetSdk 34` is below the current Play Store minimum for new submissions. Deferred to Phase 9
  so it does not destabilise earlier phases.
- `android.enableJetifier = true` is set. It slows every build and is likely unnecessary on a
  fully AndroidX dependency set. Worth testing with it off once the build is stable.
- The old signing config had a keystore password committed in plaintext. It has been removed from
  `app/build.gradle`, but it remains in git history. Rotate that keystore before release.

---

## Teardown — after the whole update is complete

The toolchain installed in Phase 0 exists only to build this port. Once all phases are `Done` and
the work is verified, remove it:

```bash
# Android SDK command-line tools and downloaded SDK packages
brew uninstall --cask android-commandlinetools
rm -rf /opt/homebrew/share/android-commandlinetools

# JDK 17
brew uninstall openjdk@17

# Gradle caches and daemons (can be several GB)
rm -rf ~/.gradle

# Project-local build output and SDK pointer
cd /Users/fei/Downloads/Newsletter_Android
rm -rf .gradle app/build build
rm -f local.properties
```

Notes:

- `~/.gradle/gradle.properties` (the `org.gradle.java.home` pin) is removed by the `rm -rf ~/.gradle`
  above. If you keep other Gradle projects on this machine, delete just that one line instead of
  the whole directory.
- Do **not** delete `keystore.properties.example`, the `.gitignore` entries, or the `build.gradle`
  signing changes. Those are permanent improvements, not scaffolding.
- Only run teardown after the final phase is verified. If any phase is still open, leave the
  toolchain in place.

---

## Progress log

Newest entries at the top. One entry per session. Include: what you did, what you decided, what
you deliberately skipped, and anything that surprised you.

### 2026-07-26 — Phase 0 (environment)

- Installed Homebrew `openjdk@17` (17.0.20). The Temurin cask was attempted first but requires
  `sudo` for its `.pkg` installer, which is not available non-interactively; the formula installs
  without root. It is keg-only, hence the `org.gradle.java.home` pin in `~/.gradle/gradle.properties`
  so both the CLI and Android Studio resolve it.
- Installed `android-commandlinetools` at `/opt/homebrew/share/android-commandlinetools`, accepted
  all licenses, and installed `platform-tools`, `platforms;android-34`, `build-tools;34.0.0`.
- Created `local.properties` with `sdk.dir` (already gitignored).
- Rewrote signing in `app/build.gradle`: removed the hardcoded
  `C:\Users\ShunKwok\Downloads\Keystore` path and its plaintext passwords, moved release signing
  behind a gitignored `keystore.properties`, and dropped `signingConfig` from `defaultConfig` so
  debug builds use AGP's generated debug keystore. Added `keystore.properties.example`.
- Fixed `abiFilters`: removed the invalid `armeabi` ABI and the duplicated `arm64-v8a`.
- Made `printDebugKeyHashes` resolve its values at configuration time (configuration cache is
  enabled) and fall back to the standard debug keystore, with a clear message if it is absent.
- Raised `org.gradle.jvmargs` from 2 GB to 4 GB plus 1 GB metaspace for kapt + Hilt.
- Added `keystore.properties`, `*.jks`, `*.keystore` to `.gitignore`; `chmod +x gradlew`.
