# Android Parity Plan

Goal: bring the Android app up to the current iOS app while keeping both codebases easy to compare, debug, and evolve side by side.

## Guiding Principles

- Keep Android native Kotlin/XML for now; match the current project instead of introducing a framework rewrite.
- Prefer similar concepts, file names, and package boundaries to iOS where it helps cross-platform debugging.
- Port static/domain code before screen work so feature waves can reuse shared-shaped models and tools.
- There are no current Android users to migrate, so prefer clean iOS-aligned code over legacy compatibility or old Android structures.
- Treat iOS as the feature reference, but use Android platform conventions where the platform genuinely differs.

## Wave 0: Structure, Static Classes, And App Foundation

Purpose: make the Android project structurally comparable to iOS before major feature parity work.

### Main App Shell

- Confirm the three-tab app structure is correct: Course, My Bag, Settings.
- Keep tab order and conceptual naming aligned with iOS:
  - Course selection / play entry
  - My Bag
  - Settings / profile
- Rename Android `ProfileFragment` to `SettingsFragment` for iOS parity.
- Ensure tab styling uses the same core semantic colors as iOS: grass, text, background, gold.

### App Singleton / Application State

iOS reference: `AppSingleton.swift`

Android target:

- Keep `GolfApplication` as Android's platform equivalent of `AppSingleton`.
- Align global state names and concepts:
  - `course`
  - `me`
  - `metric`
  - `cupholderMode`
  - API key helpers if needed
  - debug/testing flags if needed
- Keep preference keys identical where possible:
  - `using_metric`
  - `cupholder_mode`
  - player sharing/customization keys

### Models

iOS model files:

- `Badge.swift`
- `Bag.swift`
- `Club.swift`
- `Course.swift`
- `GolfShotRoute.swift`
- `Hole.swift`
- `Player.swift`

Android now has:

- `Bag.kt`
- `Badge.kt`
- `Club.kt`
- `Course.kt`
- `GolfShotRoute.kt`
- `Hole.kt`
- `Player.kt`

Wave 0 model work:

- Keep Android equivalents for all iOS models:
  - `Badge.kt`
  - `Bag.kt`
  - `Club.kt`
  - `Course.kt`
  - `GolfShotRoute.kt`
  - `Hole.kt`
  - `Player.kt`
- Compare fields and computed properties across:
  - `Bag`
  - `Club`
  - `Course`
  - `Hole`
  - `Player` / `Me`
- Keep Firestore document parsing and field names aligned.
- Avoid migration-only code and legacy Android data structures.

### Tools / Utilities

iOS tool files:

- `AnalyticsLogger.swift`
- `AppUtility.swift`
- `BitmojiUtility.swift`
- `ClubTools.swift`
- `CourseTools.swift`
- `DebugLogger.swift`
- `GeneticAlgorithm.swift`
- `LocationUpdateTimer.swift`
- `MapTools.swift`
- `MarkerTools.swift`
- `PlayerUpdateTimer.swift`
- `ShotTools.swift`

Android now has:

- `Extensions.kt`
- `MapTools.kt`

Wave 0 tool work:

- Use a `tools` package that mirrors iOS `Tools`.
- Keep Android equivalents for the iOS tool names:
  - `AnalyticsLogger.kt`
  - `AppUtility.kt`
  - `BitmojiUtility.kt`
  - `ClubTools.kt`
  - `CourseTools.kt`
  - `DebugLogger.kt`
  - `GeneticAlgorithm.kt`
  - `LocationUpdateTimer.kt`
  - `MapTools.kt`
  - `MarkerTools.kt`
  - `PlayerUpdateTimer.kt`
  - `ShotTools.kt`
- Keep platform-specific tools small until their feature wave needs deeper behavior.
- Fix known utility parity issues while here, especially Android fuzzy search behavior.

### Extensions

iOS reference: `Extensions.swift`

Android target: `Extensions.kt`

Wave 0 extension work:

- Align GeoPoint / LatLng / Location conversions.
- Align distance formatting for metric vs yards.
- Add yards/meters conversion helpers for `Int` and `Double`.
- Add Gaussian random helper if required by shot/drive calculations.
- Keep Android-only helpers such as keyboard and view visibility extensions.
- Audit existing Android extensions for correctness before feature work depends on them.

### Colors, Assets, And Theme Tokens

iOS color assets:

- `AccentColor`
- `Background`
- `Gold`
- `Grass`
- `Text`

Android currently has similar but differently named values:

- `colorPrimary`
- `colorText`
- `background1`
- `gold`
- plus light/dark helper colors

Wave 0 color work:

- Add or alias semantic Android colors:
  - `grass`
  - `text`
  - `background`
  - `gold`
  - `accent`
- Keep existing names until layouts are migrated safely.
- Ensure night-mode colors match iOS light/dark intent.
- Inventory image assets and align names where it reduces mental overhead.

## Wave 1: Settings Parity

- Implement Android settings behavior instead of placeholder UI.
- Include units toggle, display/cupholder mode, location sharing, Bitmoji sharing placeholder/disabled state, privacy link, and terms link.
- Persist settings through `GolfApplication` / shared preferences using iOS-aligned keys.
- Keep Firestore player cleanup behavior aligned when location sharing is disabled.

## Wave 2: Bag Parity

- Add club creation/editing flow similar to iOS `AddClubViewController`.
- Port `ClubTools` cleaning/validation behavior.
- Persist bag customization flags consistently.
- Validate Android list refresh and empty/error states.

## Wave 3: Play Map Parity

- Compare Android `PlayGolfActivity` against iOS map controllers.
- Port missing high-value behavior:
  - ambassador state and message
  - long-drive controls
  - drive mark/clear behavior
  - selected club and distance behavior
  - marker tools and map updates
  - Wear OS updates equivalent to iOS watch updates where applicable

## Wave 4: Social And Achievement Parity

- Add badges screens and model-backed badge display.
- Revisit Snapchat/Bitmoji integration for Android.
- Add analytics parity around settings, map, bag, badges, and sharing flows.

## Validation

For each wave:

- Run Android compile/build checks.
- Run unit tests where available.
- Smoke test on emulator.
- Smoke test on real device for maps, location, and Play Services behavior.
- Verify Firebase read/write behavior in debug before release.
- Increment Android version only when preparing a release build.

## Immediate Next Step

Start Wave 0 with a focused structural PR/change:

1. Preserve current uncommitted Android changes.
2. Add missing static model/tool files only where useful.
3. Align `GolfApplication`, colors, and extensions.
4. Compile the Android app.
5. Then move into Wave 1 settings.
