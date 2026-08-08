# iOS parity execution plan — Zone News Android

**Goal:** bring the Android app to feature and visual parity with the current iOS reference as
closely as Android permits, without replacing the existing XML/ViewBinding architecture.

**Audience:** agents executing this plan and the human verifier. Agents must also read
[`IOS_PARITY_AGENTS.md`](IOS_PARITY_AGENTS.md) before changing code.

**Reference snapshot audited for this revision:** `Newsletter_iOS copy/` at commit
`82d548a4e74530f2d5b12cf29b46083d0729f0a0` (`2026-08-08`, “news detail page bug”). The iOS
reference is a nested Git repository and is read-only.

This document is the source of truth across sessions. It defines ownership, dependencies,
file-level scope, exit gates, device checks, and the progress log. A chat message is not a durable
record of completed or broken parity work.

---

## Execution rules

1. Read this document, [`IOS_PARITY_AGENTS.md`](IOS_PARITY_AGENTS.md), the current phase, its
   dependencies, and the newest progress-log entries before touching code.
2. Confirm the iOS reference revision with:

   ```bash
   git -C "Newsletter_iOS copy" rev-parse HEAD
   ```

   If it differs from the audited commit above, first run
   `git -C "Newsletter_iOS copy" diff --name-status <audited-commit>..HEAD`, revise every affected
   phase, update the audited commit in this header, and add a **Reference delta** progress entry.
3. Work on one phase or one clearly named subset of a phase per session. Set its status to
   `In progress` before implementation. Do not silently work in a different phase.
4. Read only the iOS sources named by the active phase plus directly referenced dependencies.
   Inspect the Android targets before deciding they are absent; much of the app is partially
   implemented under different names.
5. Match observable behaviour and data contracts, not Swift syntax. Reuse Android platform
   conventions where an iOS interaction has no direct equivalent and record the deviation.
6. Never edit anything inside `Newsletter_iOS copy/`, including its nested `.git` directory,
   project file, schemes, assets, or documentation. Never copy its credentials or entitlements.
7. Keep the Android stack: Fragments/Activities, XML layouts, ViewBinding, Retrofit/RxJava2, Hilt,
   Glide, MMKV, WorkManager, and Play Billing. `compose = false`; do not enable or introduce
   Jetpack Compose even though unused Compose dependencies remain in the Gradle catalog.
8. Localize every user-facing string in all four Android resource sets:
   `values`, `values-en`, `values-zh-rCN`, and `values-zh-rHK`. No hardcoded display text in Kotlin,
   layouts, menus, accessibility labels, notifications, widgets, or errors.
9. Support light and dark mode. Layouts reference resources/theme attributes, never raw colour
   literals. Define icon, text, ripple, selected, disabled, and menu tint explicitly; inherited
   accent tint is not an acceptable default.
10. Preserve user data and authentication state across migrations. Credentials/tokens belong in
    encrypted storage, never plain MMKV, resources, logs, fixtures, or commits.
11. Do not run local Gradle builds or tests as a routine phase gate. The repository owner will
    commit and push the changes, then build and test them on another device/environment. Agents
    must leave exact remote test cases and likely risk areas in the handoff.
12. Before ending a coding session, tick only agent-verifiable checklist items, update phase
    status, and add a dated progress entry with sources reviewed, files changed, decisions,
    deviations, unrun test coverage, and open issues.
13. Agents may set `Not started`, `In progress`, `Blocked`, or `Awaiting verification`. Only the
    human verifier may set `Done` after completing the phase's device checks.

---

## Environment and remote verification

| Component | Location / value |
| --- | --- |
| JDK | `/opt/homebrew/opt/openjdk@17` |
| Android SDK | `/opt/homebrew/share/android-commandlinetools` |
| SDK packages | `platform-tools`, `platforms;android-34`, `build-tools;34.0.0` |
| Gradle JDK pin | `~/.gradle/gradle.properties` → `org.gradle.java.home` |
| SDK path | `local.properties` → `sdk.dir` (gitignored) |
| Verification handoff | Commit/push the Android changes, then build and test in the verifier's environment/device |

Release signing reads from `keystore.properties` (gitignored; see
`keystore.properties.example`). If absent, release builds are unsigned and debug builds are
unaffected.

There is no emulator or connected device on this machine. Per repository-owner direction, agents
do not need to run local Gradle builds or tests before handoff. Build, automated checks, install,
runtime checks, and visual comparison happen after the changes are committed and pushed. Agents
must not claim that unrun code compiles; they record it as awaiting remote verification.

---

## Verification workflow

1. The agent completes all implementation and locally checkable items, records that build/tests
   were not run locally, sets the phase to `Awaiting verification`, and writes the exact handoff.
2. The repository owner commits and pushes the changes. The remote verifier builds the pushed
   commit, runs the applicable automated checks, installs it on a real device, and performs the
   phase's **Device verification** block side by side with iOS.
3. The verifier records pass/fail details in the phase block. Screens, locale, theme, device model,
   Android version, account state, and reproduction steps belong there when relevant.
4. Failures return the phase to `In progress`. Fix them in the owning phase; do not hide them in a
   later phase. After fixes, push a new revision for another remote build/device pass.
5. Only the verifier changes the phase to `Done`. A later phase may start early only when its
   dependencies are `Done`, or when the human explicitly accepts the risk in the progress log.

Compile success does not catch Hilt runtime graph failures, ViewBinding lifecycle mistakes,
adapter/data mismatches, production JSON differences, WebView behaviour, background playback,
widget refreshes, locale omissions, or dark-theme fallbacks. Device verification is mandatory.

---

## Current source layout

### iOS reference — read-only

The refreshed reference is about 60,600 lines across 165 Swift files. The previous `ZNews/`,
`Zone News/`, and `NewsletterLegacy/` duplicate-target description is obsolete.

| Path | Current purpose |
| --- | --- |
| `Newsletter/` | App entry point, assets, models, services, utilities, view models, string catalog |
| `Views/` | Main SwiftUI screen tree; this is intentionally at the reference repository root |
| `Newsletter/Views/Personal/PersonalRecapView.swift` | The one app view still nested under `Newsletter/` |
| `CompactViewWidget/` | Compact news widget extension |
| `TraditionalViewWidget/` | Detailed/traditional news widget extension |
| `PodcastPlayer/` | Podcast widget extension and widget App Intent |
| `NewsletterTests/`, `NewsletterUITests/` | Formatting/classification, podcast builder, launch tests |
| `api_documentation.md` | API reference at the root of the nested iOS repository |

The iOS API document at `Newsletter_iOS copy/api_documentation.md` is newer than the Android root
copy. It documents the standardized `{code,msg,data}` envelope, error codes, updated GET/POST
variants, and newer feed endpoints. Phase 1 must reconcile the documents and implementation.

Largest/most sensitive sources should be read in sections:

| Source | Lines at audited snapshot | Owning phase |
| --- | ---: | --- |
| `Views/Shared/NewsDetailView.swift` | 6,531 | 6 |
| `Views/Shared/TimelinePageView.swift` | 2,225 | 5 |
| `Views/Account/AccountView.swift` | 1,957 | 8 |
| `Newsletter/Services/NetworkService.swift` | 1,902 | 1 |
| `PodcastPlayer/PodcastPlayer.swift` | 1,429 | 11/12 |
| `Views/Account/SubscriptionView.swift` | 1,299 | 9 |
| `Views/Shared/NewsArticleRow.swift` | 1,129 | 2 |
| `Newsletter/Models/AppState.swift` | 1,083 | 1 |
| `Newsletter/Services/PodcastScriptBuilder.swift` | 1,013 | 11 |
| `Views/Search/SituationMonitorView.swift` | 1,008 | 7 |
| `Newsletter/Models/UserAuthentication.swift` | 996 | 1/8 |

### Android target

The Android target currently has about 27,100 lines across 114 Kotlin files and 426 XML resource
files. It uses `compileSdk 34`, `targetSdk 34`, `minSdk 24`, AGP 8.13.2, Gradle 8.14, Kotlin
2.0.21, and JVM 17.

Primary areas:

| Area | Android paths |
| --- | --- |
| API/data | `net/`, `utils/network/`, `entry/`, `model/`, `repository/` |
| Shell/feed | `ui/MainActivity.kt`, `ui/mainfrag/`, `ui/mainfrag/homechild/` |
| Detail | `ui/newsdetail/`, `ui/newsdetail/NewsDetailActivity.kt` |
| Account/auth | `ui/mainfrag/MyFrag.kt`, `ui/login/`, `ui/settings/`, `billing/` |
| Topic/history/notice | `ui/topicmodify/`, `ui/his/`, `ui/collect/`, `ui/notice/` |
| Shared custom UI | `selfview/`, `ui/components/`, `res/layout/`, `res/drawable/` |
| Widgets | `widget/`, `res/layout/widget_*`, `res/xml/widget_*` |
| Persistent helpers | `utils/MVUtils.kt`, `utils/LanguageManager.kt`, `utils/ThemeManager.kt` |

### Current navigation mismatch

| iOS tab order | Android state before parity work |
| --- | --- |
| Home (`PersonalSubTabView`) | `HomeFrag` (tag `A`) |
| Timeline (`TimelineMainPageView`) | No tab/fragment |
| World (`WorldView`) | No tab/fragment |
| Recap (`RecapView`) | `RecapFragment` exists but is not in the current four-tab shell |
| Search (`SearchView`) | `SearchFrag` (tag `C`) |
| Account is a Home-menu route | `MyFrag` (tag `E`) is currently a tab |
| Headspace/Levity is a Home-menu route | `AdviceFrag` (tag `B`) is currently a tab |

---

## Scope corrections from the refreshed iOS reference

Confirmed missing or substantially incomplete on Android:

- Five-tab shell, Timeline tab, World tab, and region overview
- Text-to-speech service, persistent mini-player, and Android media controls
- Podcast models/builders/library/player/transcript/voice UI and podcast widget
- Situation Monitor and settings, YouTube live feed, and offline trivia
- Article notes and edit-persona workflow
- Several news-detail behaviours, WebView find-in-page, and cross-tab timeline handoff

Already present but requiring contract, visual, state, or interaction alignment:

- Home/personal feeds, Levity/Headspace, Recap, Search, topics
- News detail, quotes, sentiment/subjectivity, related news, publisher distribution, screenshots
- Login/register, account, billing, history, saved articles, notifications, settings, tutorial,
  feedback, app icons, and two news widgets

Removed from the refreshed iOS source and therefore **not parity requirements**:

- `ShareExtension/` and inbound-share UI
- `TTSLiveActivityExtension/` and all Live Activity files
- `SmallWidget/` (the current third extension is a podcast widget, not a third news widget)
- `Views/Shared/IconChooser.swift` (Android may retain its existing app-icon setting, but parity work
  must not rebuild a deleted iOS screen)

### Platform substitutions and exclusions

| iOS behaviour | Android parity decision |
| --- | --- |
| StoreKit purchase/restore | Use existing Play Billing and backend entitlement state |
| Keychain | EncryptedSharedPreferences/Android Keystore; never plain MMKV for secrets |
| Spotlight indexing | Optional AppSearch/shortcuts only after core parity; absence does not block a phase |
| AirPlay picker | Use Android media output routing; add Google Cast only if existing product infrastructure supports it |
| iOS app-group widget storage | Use Android widget storage/WorkManager with the same visible freshness behaviour |
| Sign in with Apple | Do not show a non-functional copy; retain supported Firebase Google/Facebook/email flows unless product requirements add Apple OAuth |
| SwiftUI material/liquid-glass effects | Approximate with existing Android surfaces, blur, elevation, and transitions |
| Status-bar tap to scroll top | Tab reselection scroll-to-top is the Android acceptance behaviour |

---

## Phase dependency map

Status values: `Not started` · `In progress` · `Blocked` · `Awaiting verification` · `Done`

| Phase | Deliverable | Depends on |
| ---: | --- | --- |
| 1 | API, models, session/state, persistence contracts | Environment baseline |
| 2 | Design tokens and reusable feed/shared components | 1 for real model binding |
| 3 | Five-tab navigation shell and route/deep-link contract | 1, 2 |
| 4 | Home, personal feed, Levity/Headspace, topic controls | 1–3 |
| 5 | Timeline, World/regions, Recap | 1–4 |
| 6 | News detail, WebView, share, detail↔timeline handoff | 1–5 |
| 7 | Search, Situation Monitor, live feed, offline trivia | 1–3; reuse 2/6 rows where applicable |
| 8 | Account, auth, profile, settings, edit persona | 1–3 |
| 9 | Subscription, history, saved, notes, notifications, feedback | 1, 2, 6, 8 |
| 10 | TTS and Android background media session | 1, 2, 6 |
| 11 | Podcast domain, library, generation, and playback UI | 1, 2, 8, 10 |
| 12 | News/podcast widgets and all external/deep-link entry points | 1, 3, 6, 11 |
| 13 | Localization, accessibility, resilience, target SDK, regression | 1–12 |

---

## Phases

### Phase 1 — API, domain models, session state, and persistence

**Status:** Not started

**Outcome:** every later screen consumes a documented Android domain contract rather than ad-hoc
maps or iOS-shaped global state.

**iOS sources:**

- `api_documentation.md`
- `Newsletter/Services/NetworkService.swift`, `KeychainService.swift`, `SubscriptionService.swift`
- `Newsletter/Models/NewsArticle.swift`, `AppState.swift`, `UserAuthentication.swift`,
  `Subscription.swift`, `TopicCategory.swift`, `ArticleNote.swift`,
  `PersonalizationSettings.swift`, `Notification.swift`
- All `Newsletter/ViewModels/*.swift` for endpoint-to-screen usage (do not port UI state yet)
- `Newsletter/Utils/FeedCache.swift`, `CacheManager.swift`, `ImageCache.swift`,
  `PublisherLogoCache.swift`, `RelativeTimeFormatter.swift`, `SentimentSubjectivityHelper.swift`

**Android targets:** `net/AppHttpService.kt`, `utils/network/`, `entry/`, `model/`, `repository/`,
`utils/MVUtils.kt`, `utils/ImageCacheManager.kt`, `repository/LoginRepository.kt`.

**Implementation checklist:**

- [ ] Diff the Android and iOS API documents; record an endpoint matrix covering method, path,
      auth/cookie requirement, parameters, language, success type, and error type for every method
      actually called by an iOS view model.
- [ ] Implement the standardized `{code,msg,data}` success/error envelope and preserve backend error
      codes without collapsing them into generic strings.
- [ ] Align Retrofit endpoints, including documented GET/POST variants, feed/personal/levity,
      detail/timeline, profile topics/history/saved, notifications, subscription, recap, search,
      Situation Monitor, and podcast inputs used by the current source.
- [ ] Centralize language mapping: Android locale → API `en-UK`, `zh-CN`, or `zh-HK`; verify every
      localized endpoint uses it consistently.
- [ ] Map `NewsArticle`/`DetailedArticle` identifiers, dates, optional fields, sources, regions,
      tags, stance, sentiment, subjectivity, quotes, screenshots, related articles, and timeline
      payloads into explicit Kotlin models with safe defaults only where the API permits them.
- [ ] Decompose `AppState` into scoped session, entitlement, feed preference, saved/history, and
      navigation state. Do not create one application-wide mutable god object.
- [ ] Align Firebase/email authentication and backend cookie/token lifecycle with
      `UserAuthentication`; logout must clear secrets and account-scoped caches without deleting
      unrelated theme/language settings.
- [ ] Store tokens/credentials with Android Keystore-backed encrypted storage. Document each MMKV
      key retained for non-secret preferences and its migration/default behaviour.
- [ ] Reconcile cache ownership, key format, expiration, invalidation on locale/account changes,
      offline fallback, and image/publisher-logo placeholder behaviour.
- [ ] Add unit/contract tests for envelope parsing, representative full/minimal article JSON,
      locale mapping, date/score formatting, and at least one backend error payload.
- [ ] Leave the remote verifier the unit-test/build commands and representative payload cases;
      record that they remain unrun until the commit is pushed.

**Device verification:** _Not yet checked._ Verify login persistence/relaunch, logout isolation,
one successful and one failed API action in each locale, feed/detail parsing with real responses,
offline cached relaunch, and no credential/token content in logcat.

---

### Phase 2 — Design tokens and reusable shared components

**Status:** Not started

**Outcome:** later phases assemble screens from one Android visual/interaction system instead of
forking near-duplicate row, loading, menu, and image behaviour.

**iOS sources:** `Newsletter/Extensions/Color+Extensions.swift`, `Newsletter/Assets.xcassets/`,
`Newsletter/Utils/InteractiveButtonStyles.swift`, and these `Views/Shared/` files:
`NewsArticleRow`, `LevityArticleRow`, `AppHeader`, `AppThrobber`, `ArticleLongPressMenu`,
`CachedAsyncImage`, `AsyncImageWithPlaceholder`, `CachedPublisherLogo`, `ErrorView`,
`OfflineConnectionView`, `TransientBannerMessage`, `CatMascotView`, `TacticalSheetChrome`,
`StarsBackgroundView`, and `IconImage`. `LoadingAnimationView.swift` is empty at the audited
snapshot and is not an implementation reference.

**Android targets:** `res/values/{attrs,colors,dimens,themes}.xml`, `res/values-night/`, shared
styles/drawables/layouts, `ui/components/`, `selfview/`, `NewsAdapter`, `YourFeedAdapter`,
`LevityArticleAdapter`, `widget/CatMascotView.kt`, and Glide helpers.

**Implementation checklist:**

- [ ] Create a documented token map for semantic background/surface/text/divider/brand/status
      colours in day/night resources; reconcile against `res/colors_reference.xml`.
- [ ] Define reusable typography, spacing, corner, elevation, touch-target, and motion values;
      support Android font scaling without clipping fixed-height containers.
- [ ] Consolidate normal/large/smart feed row variants, publisher logo, media stance, source/region
      tags, time, sentiment/subjectivity meters, image aspect ratio, saved state, and premium state.
- [ ] Align shared header and scroll/reselection behaviour without duplicating toolbar logic per
      fragment.
- [ ] Implement shared loading/shimmer, empty, error, offline, retry, transient banner, and mascot
      states with localized accessibility text.
- [ ] Standardize Glide request, cancellation, placeholder, broken-image, publisher-logo cache,
      and dark-mode behaviour.
- [ ] Implement the long-press action surface and reusable tactical sheet chrome. Explicitly tint
      every stateful icon/checkmark/text/ripple; verify no accent-colour leakage.
- [ ] Add previews or lightweight tests where practical for token/resource resolution and adapter
      binding; list the remote build and visual checks in the handoff.

**Device verification:** _Not yet checked._ Compare normal/large/smart rows in both themes and all
locales; check text scaling, slow-image placeholders, offline/error/retry, saved/premium states,
long-press actions, disabled states, and exact icon colours.

---

### Phase 3 — Five-tab shell, Home menu, and navigation contract

**Status:** Not started

**Outcome:** Android exposes the same top-level destinations and navigation semantics before
feature screens are filled in.

**iOS sources:** `Newsletter/ContentView.swift`, `Newsletter/NewsletterApp.swift`,
`Views/MainTabView.swift`, `Views/Home/HomeMenuView.swift`, `Views/Shared/LaunchScreenView.swift`,
`Newsletter/Utils/DeepLinkCoordinator.swift`, `StatusBarTapHandler.swift`,
`OrientationManager.swift`, `TutorialManager.swift`, and `PersonalizationSettings.landingPage`.

**Android targets:** `ui/MainActivity.kt`, `res/layout/activity_main.xml`, bottom-navigation
drawables/selectors, `AndroidManifest.xml`, `utils/TutorialManager.kt`, and new placeholder
fragments only where later phase screens do not exist yet.

**Implementation checklist:**

- [ ] Change tab order to Home, Timeline, World, Recap, Search with stable tags/IDs and explicit
      selected/unselected icons matching iOS (`house`, ECG timeline, globe, recap, search).
- [ ] Preserve fragment preloading, saved-instance restoration, back-stack state, configuration
      changes, process recreation, and no-lag tab switching.
- [ ] Move Account and Headspace/Levity out of the tab bar. Implement the Home menu route set:
      Notifications, Podcast, Headspace, Situation Monitor, Trivia, Account; unfinished routes may
      show a localized disabled/coming-soon state until their owning phase.
- [ ] Implement active-tab reselection: scroll/reload Home, Timeline, World, and Recap; refocus
      Search; dismiss pushed detail/topic/web overlays when appropriate.
- [ ] Define one route contract for article IDs, podcast show IDs, Home-menu destinations, initial
      landing page, notification taps, and widget taps. Route payloads must survive cold start.
- [ ] Match tab-bar visibility on web/detail/tutorial flows, edge-to-edge insets, day/night surface,
      launch screen, and offline-trivia presentation hook without implementing trivia content yet.
- [ ] Confirm back behaviour: child → owning tab, double-back/explicit action for app exit, and no
      duplicate fragment instances after repeated route taps.
- [ ] Hand off the five destinations and route matrix for remote build/device navigation checks.

**Device verification:** _Not yet checked._ Exercise all tabs, repeated reselection, rotation,
background/restore, process death restore, every Home-menu item, back stacks, cold/warm article
links, and both themes. Confirm Account and Headspace are no longer tabs.

---

### Phase 4 — Home, personal feed, Levity/Headspace, and topic controls

**Status:** Not started

**Outcome:** the Home tab and its personalized subflows match the current feed content, controls,
state restoration, and refreshed tag/stance presentation.

**iOS sources:** `Views/Home/HomeView.swift`, `Views/Personal/PersonalSubTabView.swift`,
`PersonalFeedView.swift`, `PersonalView.swift`, `LevityFeedView.swift`, `LevityDetailView.swift`,
`TrendingTopicsCrawlRow.swift`, `TopicSelectionView.swift`, `Views/Shared/LevityHeaderButton.swift`,
`PersonalizationDropDown.swift`, `PersonalizationSummaryView.swift`, `FeedSortMenu.swift`,
`FeedLayoutStyleSubmenu.swift`, and corresponding Home/Personal/Topic view models.

**Android targets:** `ui/mainfrag/HomeFrag.kt`, `YourFeedFragment.kt`, `YourFeedAdapter.kt`,
`AdviceFrag.kt`, `ui/activity/Levity*`, `LevityArticleAdapter.kt`, `ui/topicmodify/`, feed layouts,
and personalization/sort popups.

**Implementation checklist:**

- [ ] Map Home/personal subtab structure and keep selected subtab, scroll, paging, and refresh state
      across navigation and recreation.
- [ ] Align authenticated, anonymous, empty-topic, loading, offline, error, and end-of-feed states.
- [ ] Implement latest/popular/relevant sort and normal/large/smart layout choices with the audited
      smart default; persist per-account choices exactly once.
- [ ] Match article rows including refreshed media-stance tag and World/tag formatting changes from
      the 2026-08-07/08 iOS commits.
- [ ] Align trending crawl, topic follow/manage/selection, topic limits, premium gates, save/share,
      and long-press actions.
- [ ] Rename/reposition the current `AdviceFrag` experience as the Home-menu Headspace/Levity route;
      match feed/detail content and return path without keeping it as a hidden tab.
- [ ] Ensure refreshes cancel stale requests, deduplicate article IDs, preserve sort/layout, and do
      not append results from an old locale/account.
- [ ] Add adapter/view-model tests for pagination, deduplication, preference restoration, and
      empty/error transitions; identify them for the remote verifier to run after push.

**Device verification:** _Not yet checked._ Compare anonymous/signed-in Home, all layouts/sorts,
topic changes, pagination/refresh, stance/tags, Levity feed/detail, locale switch, rotation,
light/dark, and offline recovery.

---

### Phase 5 — Timeline, World/regions, and Recap

**Status:** Not started

**Outcome:** the three middle tabs are real, independently stateful screens rather than shell
placeholders.

**iOS sources:** `Views/Shared/TimelinePageView.swift`, `Views/Home/WorldView.swift`,
`HongKongRegionOverview.swift`, `NewsListView.swift`, `Views/Recap/RecapView.swift`,
`Newsletter/Views/Personal/PersonalRecapView.swift`, `Views/Shared/RecapScopePicker.swift`,
`Newsletter/ViewModels/NewsListViewModel.swift`, `PersonalViewModel.swift`, and region mapping/data
calls in `NetworkService.swift`.

**Android targets:** new Timeline and World fragments/adapters/layouts, `utils/RegionMappingUtils.kt`,
`ui/mainfrag/RecapFragment.kt`, `selfview/RecapView.kt`, `RecapSectionAdapter.kt`, and
`RecapProgressTracker.kt`.

**Implementation checklist:**

- [ ] Implement the Timeline tab's grouped event chronology, article focus, loading/empty/error,
      publisher/timestamp metadata, expandable content, and stable item restoration.
- [ ] Add the World tab with iOS region/category controls, day/night compass artwork, region tags,
      Hong Kong overview, news list pagination, article navigation, and refresh/reselection.
- [ ] Centralize iOS region values ↔ API tags ↔ localized display names; unknown regions must render
      safely without falling into the wrong flag/category.
- [ ] Align Recap scope picker, loading/progress, numbered sections/bullets, personal/global states,
      premium/auth gates, retry, and the audited 2026-08-08 Recap UI fixes.
- [ ] Keep independent scroll/filter state per tab and prevent cross-tab request results from
      replacing visible data.
- [ ] Wire article cards to Phase 6's route contract and Timeline launches from detail to Phase 3's
      cross-tab contract, using temporary route stubs only until Phase 6 lands.
- [ ] Add tests for timeline grouping/order, region mapping, and Recap state progression; identify
      them for the remote verifier to run after push.

**Device verification:** _Not yet checked._ Compare all three tabs, reselect behaviour, compass and
tags in both themes, region changes, recap scopes/progress/errors, rotation/relaunch state,
timeline focus, and article return paths.

---

### Phase 6 — News detail, WebView, outbound share, and timeline handoff

**Status:** Not started

**Outcome:** the primary reading flow matches iOS content order, controls, analysis components,
web fallback, and navigation behaviour.

**iOS sources:** read `Views/Shared/NewsDetailView.swift` by `MARK` section; also
`NewsDetailSmartLayout.swift`, `ArticleWebView.swift`, `PageFindInPage.swift`,
`EnhancedShareSheet.swift`, `TimelinePageView.swift`, `ArticleLongPressMenu.swift`,
`Newsletter/ViewModels/NewsDetailViewModel.swift`, `Newsletter/Utils/ShareMetadataGenerator.swift`,
`ArticleActivityItemSource.swift`, and `SentimentSubjectivityHelper.swift`.

**Android targets:** `ui/newsdetail/NewsDetailActivity.kt`, `NewsDetailRepository.kt`,
`NewsDetailModel.kt`, `activity_news_detail.xml`, `ui/newsdetail/*View.kt`, detail adapters/sheets,
`selfview/popup/NewsDetailMorePopupWindow.kt`, and `activity_web.xml`/browser toolbar.

**Implementation checklist:**

- [ ] Create a section-order/state matrix from every `MARK` in `NewsDetailView.swift`; map each
      visible iOS section to an Android view, adapter, sheet, explicit exclusion, or platform
      substitution before editing the layout.
- [ ] Align hero/title/publisher/time/region/stance/tags, summary/body, screenshots, quotes,
      sentiment, subjectivity, related news, publisher distribution, timeline, and premium/error
      states, including the audited 2026-08-08 smart-layout/news-detail fixes.
- [ ] Make save/history/subscription state authoritative and account-aware; rapid taps, relaunch,
      logout, and server conflicts must not visually invert state.
- [ ] Align detail controls: back, share, browser, text settings, feedback, subscription, source
      selection, long press, and tutorial anchors with explicit enabled/selected tint.
- [ ] Implement adaptive smart layout for phone widths/orientations and font scaling without
      covering content or nesting unbounded scrolling containers.
- [ ] Implement safe WebView navigation, progress/error/offline, external URL policy, find-in-page
      query/next/previous/close, back history, and state restoration. Do not weaken the existing
      network security configuration to make a page load.
- [ ] Implement outbound Android share text/URL/preview behaviour corresponding to iOS metadata.
      Do not add the deleted iOS inbound Share Extension as a parity task.
- [ ] Complete detail → Timeline tab focus → return-to-origin behaviour, including cold/missing
      article fallback and no duplicate detail activities.
- [ ] Add tests for detail mapping/section visibility, saved-state transitions, share payload, and
      URL policy; identify them for the remote verifier to run after push.

**Device verification:** _Not yet checked._ Compare several complete/sparse/premium articles,
every section/control/sheet, save/history, quote/detail lists, screenshots, WebView and find,
share targets, text scaling/orientation/themes/locales, detail↔timeline return, and offline/error.

---

### Phase 7 — Search, Situation Monitor, live feed, and offline trivia

**Status:** Not started

**Outcome:** discovery and connectivity-dependent auxiliary experiences are complete and reachable
from Search or the Home menu.

**iOS sources:** `Views/Search/SearchView.swift`, `SituationMonitorView.swift`,
`SituationMonitorSettingsSheet.swift`, `LiveFeedView.swift`, `Views/Shared/SituationMonitorButton.swift`,
`Views/LiveFeed/LiveFeedChannel.swift`, `YouTubePlayerView.swift`, `Views/Home/OfflineTriviaView.swift`,
`Newsletter/ViewModels/SearchViewModel.swift`, `Newsletter/Services/SearchHistoryStore.swift`,
`Newsletter/Utils/TriviaManager.swift`, `NetworkMonitor.swift`, and `Resources/Trivia/*.json`.

**Android targets:** `ui/mainfrag/SearchFrag.kt`, `SearchRepository.kt`, `SearchModel.kt`, search row
layouts, plus new feature packages/layouts and the Phase 3 Home-menu/offline hooks.

**Implementation checklist:**

- [ ] Align search focus-on-tab, debouncing/cancellation, submit, paging, trends, history storage,
      clear/remove actions, no-result/error/offline states, and article navigation.
- [ ] Implement Situation Monitor cards/data refresh, category/filter/settings persistence, sheet
      controls, loading/error/empty states, and Home-menu/Search entry points.
- [ ] Implement YouTube live channels/player lifecycle with safe URL/video-ID validation, pause on
      background, rotation restoration, external fallback, and localized unavailable/error states.
- [ ] Port all three offline trivia JSON sets and question/scoring/progress/dismiss behaviour;
      present once per offline session after launch readiness and dismiss automatically when
      connectivity returns, matching `MainTabView` coordination.
- [ ] Ensure Search, live playback, and monitors cancel observers/requests when views are destroyed
      and never retain a stale Activity.
- [ ] Add tests for debounce/stale-result suppression, search history scoping, trivia decoding, and
      invalid video/channel inputs; identify them for the remote verifier to run after push.

**Device verification:** _Not yet checked._ Test typing/clearing/reselect focus, trends/history,
slow-network stale results, monitor settings/refresh, live playback background/rotation/failure,
airplane-mode trivia timing/content/scoring, reconnection, themes, and all locales.

---

### Phase 8 — Account, authentication, profile, settings, and edit persona

**Status:** Not started

**Outcome:** Account works as a Home-menu destination with complete identity, preference, and
profile management rather than a former tab fragment.

**iOS sources:** `Views/Account/AccountView.swift`, `AboutView.swift`, `EditPersonaView.swift`,
`Views/Login/LoginView.swift`, `RegisterView.swift`, `Views/Shared/ContinueWithAppleButton.swift`
for exclusion context only, `Newsletter/Models/UserAuthentication.swift`,
`Newsletter/FacebookAuthManager.swift`, `Newsletter/Models/PersonalizationSettings.swift`, and
account/auth calls in `NetworkService.swift`.

**Android targets:** `ui/mainfrag/MyFrag.kt`, `ui/login/`, `repository/LoginRepository.kt`,
`repository/MyRepository.kt`, `ui/settings/`, `ui/about/`, `utils/ThemeManager.kt`,
`LanguageManager.kt`, and `AppIconManager.kt`.

**Implementation checklist:**

- [ ] Convert `MyFrag` to an Account destination that opens/closes through the Phase 3 Home menu
      without tab-only assumptions or duplicated fragment instances.
- [ ] Align signed-out, signed-in, loading, expired-session, and subscription summaries plus all
      rows, counts, social/about/legal links, and destructive-action confirmations.
- [ ] Align email register/login/verification/reset and supported Firebase Google/Facebook flows;
      do not display Apple login unless a working Android OAuth path is explicitly approved.
- [ ] Implement Edit Persona fields, validation, load/save/cancel/error/conflict behaviour and
      refresh dependent personalized data after successful edits.
- [ ] Align settings for language, system/light/dark appearance, landing destination, feed default
      layout/sort, notification preferences, cache/reset, tutorials, and existing Android app-icon
      choice. The app-icon choice is Android-owned because iOS deleted `IconChooser.swift`.
- [ ] Make external URLs come from centralized constants/config and handle missing browser/apps
      without crashing.
- [ ] Verify logout/account deletion clears account-scoped state and returns to a valid Home flow;
      destructive actions require explicit localized confirmation.
- [ ] Add tests for validation, auth-state transitions, preference migration, and locale/theme
      application; identify them for the remote verifier to run after push.

**Device verification:** _Not yet checked._ Exercise all auth flows available to the verifier,
expired sessions, edit persona, every setting, locale/theme immediate application and relaunch,
landing routes, external links, logout/reset/delete confirmations, and Account back navigation.

---

### Phase 9 — Subscription, history, saved articles, notes, notifications, and feedback

**Status:** Not started

**Outcome:** account-linked collections, entitlements, and communication surfaces are consistent
across feed/detail/account entry points.

**iOS sources:** `Views/Account/SubscriptionView.swift`, `ManageSubscriptionView.swift`,
`ReadingHistoryView.swift`, `TopicNewsView.swift`, `Views/Notes/AllNotesView.swift`,
`ArticleNotesView.swift`, `Views/Notification/NotificationView.swift`,
`Views/Shared/FeedbackSheetContent.swift`, `Newsletter/Services/NotesStore.swift`,
`SubscriptionService.swift`, and Notification/ReadingHistory/TopicNews view models.

**Android targets:** `billing/BillingManager.kt`, `SubscriptionBottomSheetFragment`,
`BrownHisActivity`, `ReadingHistoryBottomSheetFragment`, `SavedArticlesBottomSheetFragment`,
`MyCollectListActivity`, `TagNewsBottomSheetFragment`, new notes UI/store, `NoticeListActivity`,
`MyFirebaseMessagingService`, `FeedbackBottomSheetFragment`, and related repositories/models.

**Implementation checklist:**

- [ ] Align plans, entitlement display, purchase, pending/cancel/error, restore/requery, redeem code,
      manage-subscription link, and backend reconciliation; Play Billing remains authoritative for
      Android purchase state while server entitlement gates content.
- [ ] Align reading history and saved lists: paging, refresh, deletion/undo or confirmation,
      empty/error/offline, account changes, and navigation to detail.
- [ ] Align topic-news list filters/paging and reuse the canonical Phase 2 row/detail route.
- [ ] Implement article notes create/edit/delete, character/empty validation, per-article lookup,
      all-notes ordering, persistence, account scoping, and deletion confirmation.
- [ ] Align notification list/read state, push tap routes, permission-denied state, token refresh,
      deduplication, empty/error, and cold-start navigation.
- [ ] Align feedback category/content validation, submit/loading/success/error, keyboard/insets, and
      retention policy after failure/success.
- [ ] Add tests for entitlement state transitions, collection deletion/account isolation, notes
      CRUD/order, notification route decoding, and feedback validation; identify them for the
      remote verifier to run after push.

**Device verification:** _Not yet checked._ Use test billing where available; verify plans and
gates, history/saved/topic lists, notes CRUD/relaunch/account switch, notifications foreground and
cold start, feedback success/failure, themes, locales, and offline recovery.

---

### Phase 10 — Text-to-speech and Android background media session

**Status:** Not started

**Outcome:** article speech continues safely outside the detail screen and exposes native Android
media controls.

**iOS sources:** `Newsletter/Services/TTSService.swift`, `SpeechVoiceCatalog.swift`,
`SpokenTextNormalizer.swift`, TTS portions of `PodcastPlayerService.swift`,
`Views/Shared/FloatingTTSPlayer.swift`, detail TTS controls, and
`Newsletter/Utils/AccessibilityManager.swift` reduced-motion/announcement use.

**Android targets:** new TTS engine/controller, foreground `Service`, Media3 or platform
`MediaSession` integration consistent with project dependencies, notification channel/actions,
persistent mini-player views, manifest/service declarations, and Phase 6 controls.

**Implementation checklist:**

- [ ] Define one playback queue/state model for article ID, normalized segments, current segment,
      elapsed/progress estimate, rate, voice/language, play/pause/stop, and terminal/error state.
- [ ] Port spoken-text normalization for punctuation, abbreviations, URLs, repeated whitespace,
      language boundaries, and empty/unsupported content; add tests using iOS test cases where
      behaviour applies.
- [ ] Map voice availability and fallback for English, Simplified Chinese, and Traditional Chinese;
      guide users to install missing system voice data without trapping them in a loop.
- [ ] Implement audio focus, duck/pause/resume policy, headphone/Bluetooth disconnect, phone-call
      interruption, noisy broadcast, service restart, and Activity/process recreation.
- [ ] Implement foreground media notification and lock-screen controls. The deleted iOS Live
      Activity is not a parity target; Android media controls are the platform behaviour.
- [ ] Implement a persistent mini-player across tabs/details with title, progress, play/pause,
      close, expand/return-to-article, correct bottom inset, and no overlap with tab/tutorial UI.
- [ ] Keep speech and UI observers lifecycle-safe; stopping/finishing clears notification/service
      state and releases the TextToSpeech engine.
- [ ] Add unit tests for normalization/queue transitions and identify them for the remote verifier
      to run after push.

**Device verification:** _Not yet checked._ Test all languages/voices/rates, long articles,
play/pause/seek-equivalent controls, screen lock, background, process recreation, audio focus,
headset disconnect, incoming interruption if possible, completion/error, mini-player insets, and
notification dismissal/stop.

---

### Phase 11 — Podcast domain, generation, library, and playback UI

**Status:** Not started

**Outcome:** Android supports the refreshed iOS podcast subsystem end to end, reusing Phase 10's
media foundation where behaviour overlaps.

**iOS sources:** `Newsletter/Models/PodcastModels.swift`; services `PodcastEpisodeBuilder`,
`PodcastScriptBuilder`, `PodcastPlayerService`, `PodcastFavouritesStore`,
`PodcastWidgetSnapshotStore`, `SpeechVoiceCatalog`, `SpokenTextNormalizer`; view model
`PodcastLibraryViewModel`; all `Views/Podcast/*.swift`; `Views/Home/HomeMenuView.swift` podcast
route; and `NewsletterTests/PodcastScriptBuilderTests.swift`.

**Android targets:** new podcast domain/repository/player packages, Room or existing persistence
consistent with project architecture, podcast Activities/Fragments/layouts, Home-menu route, and
shared media notification/session from Phase 10. Widget UI belongs to Phase 12.

**Implementation checklist:**

- [ ] Map show/episode/script/voice/favourite/download-or-generated-state models and stable IDs;
      document which content is fetched, generated, cached, or derived locally.
- [ ] Port episode/script building, ordering, speaker assignment, normalization, invalid/partial
      input handling, and deterministic tests corresponding to iOS podcast builder tests.
- [ ] Implement library, all shows, show detail, favourites, loading/empty/error, pull-to-refresh,
      artwork placeholders, and Home-menu/cold deep-link entry.
- [ ] Implement now playing, transcript/current-segment highlighting, voice controls, mini-player,
      rate, play/pause/skip/stop, queue/end behaviour, and return paths.
- [ ] Reuse the Phase 10 audio-focus/session/service state rather than running competing TTS and
      podcast engines; switching content must have an explicit stop/replace policy.
- [ ] Implement media output routing appropriate to Android; AirPlay and iOS liquid-glass styling
      are platform-specific and must not be imitated with non-functional controls.
- [ ] Persist favourites and resumable state per the iOS behaviour, invalidate stale generated
      content on locale/voice/account changes, and bound cache growth.
- [ ] Identify the podcast unit tests and build/device scenarios for the remote verifier to run
      after push.

**Device verification:** _Not yet checked._ Generate/open several shows, test partial/error data,
favourites/relaunch, transcript sync, voice/rate, background/lock-screen/audio focus, switching
between article TTS and podcast, mini-player navigation, themes/locales, and cold show links.

---

### Phase 12 — Widgets and external/deep-link entry points

**Status:** Not started

**Outcome:** the two news widgets and one podcast widget display fresh state and route reliably
into the correct in-app destination from cold and warm starts.

**iOS sources:** all files in `CompactViewWidget/`, `TraditionalViewWidget/`, and `PodcastPlayer/`;
`Newsletter/Services/PodcastWidgetSnapshotStore.swift`; `Newsletter/Utils/DeepLinkCoordinator.swift`;
`Views/Personal/WidgetConfigurationView.swift`; `Newsletter/NewsletterApp.swift` URL handling.

**Android targets:** `widget/CompactNewsWidgetProvider.kt`, `DetailedNewsWidgetProvider.kt`,
`WidgetConfigActivity.kt`, `WidgetDataProvider.kt`, widget layouts/info XML, new podcast widget,
`AndroidManifest.xml`, WorkManager refresh, and Phase 3 route parser.

**Implementation checklist:**

- [ ] Map compact iOS news widget to Android compact and traditional iOS widget to Android
      detailed; align family-specific content density, image/logo fallback, scores, timestamps,
      empty/offline/error, day/night, and responsive size behaviour.
- [ ] Add the current third extension as a **podcast widget** with snapshot artwork/show/episode,
      play/open intent behaviour supported by Android, and safe empty/not-generated state. Do not
      resurrect the deleted SmallWidget as a third news widget.
- [ ] Align widget configuration, selected topics/regions/content, per-widget IDs, cancel/delete,
      and restoration after process/device restart.
- [ ] Implement bounded periodic/manual refresh, locale/account/theme invalidation, network
      failure fallback, and unique `PendingIntent` identities so widgets do not open one another's
      content.
- [ ] Verify article, podcast show, notification, and supported web/App Link parsing through one
      validated route layer for cold, warm, duplicate, missing, and malformed payloads.
- [ ] Keep outbound share in Phase 6. Do not add an inbound `ACTION_SEND` target solely to replace
      the deleted iOS Share Extension.
- [ ] Add tests for route parsing and widget intent identity; identify them for the remote verifier
      to run after push.

**Device verification:** _Not yet checked._ Add multiple widgets of each type/size, configure them
differently, refresh online/offline, change locale/theme/account, reboot if practical, test unique
article/show taps from cold/warm app, and test malformed/stale destinations.

---

### Phase 13 — Localization, accessibility, resilience, SDK upgrade, and full regression

**Status:** Not started

**Outcome:** the complete app is shippable across supported Android versions, locales, themes,
font/accessibility settings, connectivity states, and lifecycle events.

**iOS sources:** `Newsletter/InfoPlist.xcstrings` (1,139 keys; locales `en`, `en-GB`, `en-US`,
`zh-Hans`, `zh-Hant` at audit), `Newsletter/Extensions/LocalizedStringKey+Extensions.swift`,
`Newsletter/Utils/AccessibilityManager.swift`, `HapticFeedback.swift`, `AppRatingManager.swift`,
`NetworkMonitor.swift`, and `NewsletterUITests/NewsletterUITestsLaunchTests.swift`.

**Android targets:** all four `strings.xml` sets, day/night resources, every manifest/menu/widget/
layout/Kotlin user-facing string, `HapticFeedbackHelper.kt`, accessibility attributes/actions,
Gradle/manifest SDK configuration, and test sources.

**Implementation checklist:**

- [ ] Produce a key audit across all four Android locales; reject missing/extra keys, malformed
      format placeholders, accidental untranslated English, hardcoded display text, and stale iOS
      strings for features not present on Android.
- [ ] Translate semantic content for English, Simplified Chinese, and Traditional Chinese; preserve
      placeholders/plurals and use Android locale fallback intentionally rather than duplicating
      inconsistent values.
- [ ] Audit every screen/widget/notification in light/dark, high contrast where available, 1.0× and
      maximum supported font scale, narrow/wide phone, portrait/landscape where supported, and
      system bars/IME insets.
- [ ] Add content descriptions only where they add meaning, heading/live-region roles, logical
      focus order, labelled custom actions/meters, 48dp touch targets, keyboard/switch access, and
      reduced-motion behaviour. Decorative images must not create noisy focus stops.
- [ ] Align haptics with user/system settings and add Play In-App Review at the equivalent eligible
      moment without blocking normal navigation.
- [ ] Exercise online/slow/offline/reconnect, anonymous/signed-in/subscribed, fresh install/update,
      process death, rotation, background/foreground, notification/widget/deep links, and storage
      migration. Fix crashes/leaks/stale observers in the owning phase and reference them in logs.
- [ ] Raise `targetSdk`/`compileSdk` to the current Play submission requirement, review behaviour
      changes and permissions, then rerun the complete regression matrix. Do this only after core
      parity is stable.
- [ ] Provide the remote verifier the unit/instrumentation, lint, build, and regression commands;
      document any accepted lint baseline rather than suppressing new findings broadly.

**Device verification:** _Not yet checked._ Complete the full side-by-side regression matrix and
record device/Android version, build hash, locales, themes, font scales, account tiers, lifecycle,
connectivity, widget/notification/media, and all accepted platform deviations. Only then mark the
phase and overall plan `Done`.

---

## Known issues carried into implementation

- `settings.gradle` references `https://maven.aliyun.com/repository/jcenter`, a mirror of the
  retired JCenter. If dependency resolution stalls, assess and remove/replace that repository.
- `targetSdk 34` is below the expected contemporary Play submission requirement; the controlled
  upgrade belongs to Phase 13 after core parity stabilizes.
- `android.enableJetifier = true` may be unnecessary and slows builds. Test removal only after a
  stable baseline, and record dependency evidence.
- The Gradle catalog/build still includes unused Compose dependencies while `compose = false`.
  Do not interpret their presence as permission to add Compose; cleanup can be handled separately.
- A previous release-signing password was removed from `app/build.gradle` but remains in Git
  history. Rotate the affected keystore before release.
- The refreshed iOS `MainTabView.swift` contains a duplicated `@StateObject tabCoordinator`
  declaration at the audited commit. Treat it as a reference-source defect; do not reproduce it on
  Android. Record later upstream correction as a reference delta.

---

## Progress log

Newest entries go first. Each coding entry must name the phase/subset, iOS sources reviewed,
Android files changed, behaviour decisions, platform deviations, tests added or still unrun,
phase status, and unresolved/device-verification items.

### 2026-08-08 — Reference audit and plan rewrite (documentation only)

- Audited the refreshed nested iOS repository at
  `82d548a4e74530f2d5b12cf29b46083d0729f0a0` without editing it.
- Corrected the canonical layout from obsolete duplicate `ZNews/`/`Zone News/` targets to
  `Newsletter/` for app logic, root `Views/` for most UI, and the Compact, Traditional, and Podcast
  widget extensions.
- Recorded current inventory: about 60,600 Swift lines/165 files on iOS and 27,100 Kotlin
  lines/114 files on Android.
- Replaced nine broad phases with thirteen dependency-ordered execution phases, each with exact
  source groups, Android targets, implementation checklist, exit build, and device checks.
- Added refreshed scope: expanded podcast subsystem/widget and recent smart-layout, Recap,
  media-stance, tag, dropdown, API, and localization changes.
- Removed obsolete parity requirements for the deleted Share Extension, TTS Live Activity,
  SmallWidget, and iOS IconChooser; retained explicit Android platform substitutions.
- Renamed the focused execution guide from `AGENTS.md` to `IOS_PARITY_AGENTS.md` and linked it from
  this plan.
- Updated the verification workflow per repository-owner direction: agents do not run local
  Gradle/tests as a routine gate; the owner commits/pushes, then another environment/device builds
  and verifies the pushed revision. A documentation-session Gradle attempt was stopped during
  dependency/configuration and no compile result is claimed.
- No implementation phase status or checkbox was changed.

### 2026-07-26 — Phase 0 (environment)

- Installed Homebrew `openjdk@17` (17.0.20) and pinned it through
  `~/.gradle/gradle.properties`; installed Android command-line tools, accepted licenses, and added
  platform/build-tools 34.
- Created gitignored `local.properties` with the SDK path.
- Reworked release signing to use gitignored `keystore.properties`, removed plaintext credentials
  and the machine-specific Windows keystore path, and added `keystore.properties.example`.
- Removed invalid/duplicate ABI filters, fixed configuration-cache-safe debug key hash resolution,
  raised Gradle memory, added signing files to `.gitignore`, and made `gradlew` executable.

---

## Toolchain teardown — only after the entire plan is verified

Do not run teardown while any phase is open. These are intentionally destructive maintenance
commands and require the human to confirm the exact scope at that future time.

- Uninstall the project-only Android command-line tools and JDK 17 if no other project uses them.
- Remove only project build outputs/caches and the gitignored `local.properties` SDK pointer.
- Remove the Gradle JDK pin or Gradle cache only if it is not shared with other projects.
- Keep `keystore.properties.example`, `.gitignore` rules, and signing configuration changes.
