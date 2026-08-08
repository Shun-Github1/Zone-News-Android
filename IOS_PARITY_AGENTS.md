# Instructions for agents executing the iOS parity plan

This guide applies specifically when implementing or verifying
[`IOS_PARITY_PLAN.md`](IOS_PARITY_PLAN.md). The plan is the source of truth for phase scope,
dependencies, status, file mappings, device checks, and progress history.

## Start-of-session protocol

1. Read the plan header, execution rules, dependency map, active phase, its device-verification
   block, known issues, and the newest progress entries.
2. Check both working copies without modifying them:

   ```bash
   git status --short
   git -C "Newsletter_iOS copy" status --short
   git -C "Newsletter_iOS copy" rev-parse HEAD
   ```

3. Compare the iOS HEAD with the audited revision in the plan. If it changed, stop implementation
   long enough to inventory `git diff --name-status <audited>..HEAD`, update affected phase scope,
   and add a Reference delta log entry.
4. Confirm all dependency phases are `Done` or that the human explicitly accepted proceeding with
   an open dependency. Read every unresolved verification finding before selecting work.
5. Set the selected phase to `In progress` and state the exact subset when a whole phase cannot fit
   safely in one session.

## Read-only iOS reference

`Newsletter_iOS copy/` is a nested Git repository and must never be edited. Do not run formatters,
builds that rewrite project metadata, dependency updates, checkout/reset/clean, or asset conversion
in it. Do not copy `GoogleService-Info.plist`, entitlements, signing material, tokens, or other
environment-specific configuration into Android.

The current canonical layout is:

- `Newsletter/` — app models, services, utilities, view models, assets, entry point, localization
- `Views/` — almost all SwiftUI screens, at the reference repository root
- `Newsletter/Views/Personal/PersonalRecapView.swift` — the exceptional nested view
- `CompactViewWidget/` and `TraditionalViewWidget/` — two news widgets
- `PodcastPlayer/` — podcast widget extension

There are no current `ZNews/` or `Zone News/` targets. Do not search for or port those obsolete
paths. Pass `Newsletter_iOS copy/...` explicitly to search commands; default workspace searches
may omit the nested repository.

## Implementation constraints

- Keep XML layouts, ViewBinding, Fragments/Activities, Retrofit/RxJava2, Hilt, Glide, MMKV,
  WorkManager, Firebase, and Play Billing. `compose = false`; do not enable Jetpack Compose.
- Port observable behaviour and data semantics, not Swift type structure or iOS-only APIs.
- Inspect existing Android code before creating a parallel implementation. Prefer extending the
  repository/model/adapter/view patterns already owning the feature.
- Keep state scoped by lifecycle and account. Do not create a global mutable equivalent of the
  iOS `AppState`; do not retain Activity/View references in repositories, services, adapters, or
  application singletons.
- Every displayed string must exist in `values`, `values-en`, `values-zh-rCN`, and
  `values-zh-rHK`, including errors, menus, notifications, widget text, accessibility labels, and
  content descriptions. Preserve format placeholders consistently.
- Every screen must adapt to light/dark mode and font scaling. Use resources/theme attributes,
  not raw layout colours. Define icon, menu, text, selected, disabled, and ripple tint explicitly
  so nothing silently inherits the accent colour.
- Store secrets only with Android Keystore-backed encrypted storage. Never log or commit tokens,
  cookies, passwords, signing values, Firebase configuration from iOS, or production payloads with
  personal data.
- Do not recreate features deleted from the audited iOS source: inbound Share Extension, TTS Live
  Activity, SmallWidget, or iOS IconChooser. Follow the plan's platform substitutions.
- Preserve unrelated user changes in a dirty Android worktree. Do not reset, discard, or rewrite
  files outside the active phase.

## Phase working method

1. Build a small source-to-target checklist from the phase's named files. For very large Swift
   sources, inspect by declaration/`MARK` section and record which sections were reviewed.
2. Inventory current Android behaviour and classify each requirement as present/aligned,
   present-needs-change, missing, platform substitution, or explicitly excluded.
3. Implement the smallest end-to-end slice that leaves the app buildable. Do not tick a broad
   checkbox after implementing only its happy path.
4. Add proportionate tests for parsers, mappings, state transitions, route decoding, persistence,
   and lifecycle-independent logic. Do not run them locally as a routine phase gate; identify them
   in the remote-verification handoff.
5. Check all four locale files and both colour resource sets for every touched UI surface.
6. Do not spend the session running local Gradle builds or tests. Per repository-owner direction,
   the owner will commit/push first, then another environment/device will build and test the pushed
   revision.
7. Update the plan before ending the session. Record iOS sources reviewed, Android files changed,
   decisions/deviations, tests added but unrun, likely build/runtime risks, exact remote commands or
   scenarios, unresolved issues, and the next step.

## Status and verification ownership

- `Not started`: no implementation work has begun.
- `In progress`: some phase work or verifier fixes remain.
- `Blocked`: name the concrete blocker and what would unblock it; difficulty alone is not a block.
- `Awaiting verification`: every agent-checkable phase item is complete and the pushed revision is
  ready for remote build/test; local build success is not required or implied.
- `Done`: reserved for the human after all listed device checks pass or deviations are explicitly
  accepted in the plan.

Never mark a checklist item based only on intent, partial code, or compilation when it requires
runtime observation. Put device findings in the owning phase, return that phase to `In progress`,
and resolve them there before later phases rely on it.

## End-of-session handoff

Before yielding:

1. State explicitly that local build/tests were not run and list the remote build/test/lint
   commands or device scenarios that should be exercised after commit/push.
2. Tick only completed and agent-verifiable items.
3. Set the truthful phase status; use `Awaiting verification`, never `Done`.
4. Add a dated progress entry detailed enough for a new agent to continue without chat history.
5. Point the human to the pushed revision and the phase's exact remote build/device checklist when
   the phase is ready. Do not claim compile or runtime success before that report returns.
