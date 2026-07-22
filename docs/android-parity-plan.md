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

- Implemented Android settings behavior instead of placeholder UI.
- Included units toggle, display/cupholder mode, location sharing, Bitmoji sharing placeholder/disabled state, privacy link, and terms link.
- Persist settings through `GolfApplication` / shared preferences using iOS-aligned keys.
- Keep Firestore player cleanup behavior aligned when location sharing is disabled.
- Logged the same high-level analytics events as iOS settings.

## Wave 2: Bag Parity

- Added inline default bag selection for empty bags.
- Added club creation flow similar to iOS `AddClubViewController`.
- Ported `ClubTools` cleaning/validation behavior into add and edit flows.
- Persist bag customization flags consistently.
- Added inline edit cleanup, club removal, list refresh, and empty states.
- Manual drag reorder remains optional polish because clubs are sorted by distance for recommendations.

## Design System Pass

- Added a first-class Android resource layer for the GolfPS look:
  - semantic light/dark colors for background, surface, text, grass, gold, borders, inputs, tabs, and destructive actions
  - shared rounded surface, input, overlay, button, chip, and title styles
  - selected/unselected color selectors for chips and the main tab bar
- Updated the main tab shell, course selection, bag, settings, add-club, add-course, play-map chrome, and info window layouts to use theme-aware resources.
- Kept Android-native controls first: Material buttons, chips, switches, tab layout, EditText fields, and RecyclerView lists.
- Used the playful display font selectively for screen titles and major action buttons to echo the iOS Marker Felt feel without making dense controls noisy.
- Removed the unused legacy profile layout so Android stays clean and aligned with the current Settings structure.
- Verified both light and dark mode are driven by `values` / `values-night` resources instead of one-off layout colors.

## Wave 3: Play Map Parity

- Compare Android `PlayGolfActivity` against iOS map controllers.
- Started with Course Select parity because it feeds the map entry flow:
  - load the signed-in player document before building tabs so ambassador course data is available
  - fix Android `Player` ID assignment so the authenticated user ID is used consistently
  - show iOS-style Course Select sections for Ambassador, Nearby, Visited, and Search Results
  - load nearby courses from the player's current state when location permission is available
  - fall back to a lazy-loaded Available section only when location is denied or cannot be determined
  - show course distance labels when a course spectation point and player location are available
  - show ambassador badges in course rows
  - keep empty section messages instead of silent blank lists
  - add Android pull-to-refresh for course reloads, matching iOS `UIRefreshControl`
  - relax fixed-height text rows that were clipping after the design pass
- Port missing high-value behavior:
  - [x] ambassador state and message
  - [x] long-drive controls
  - [x] drive mark/clear behavior
  - [x] selected club and distance behavior
  - [x] ambassador marker editing and map updates
  - [x] privacy-gated player location publishing
  - [x] course-scoped other-player markers and map updates
  - [x] stale-player filtering and spectator fallback
  - [x] remote avatar marker loading with standard marker fallback
  - [x] phone-side Wear OS state updates for course, hole, distance, units, and club
  - [x] Wear OS next/previous hole command handling
  - [x] Wear OS companion module with on-watch presentation and hole controls

Wind and elevation adjustments are intentionally deferred until both platforms are
ready for a coordinated polish pass. iOS remains unchanged unless a shared bug is
identified and an explicit cross-platform fix is approved.

### Map Interaction And Visual Polish

- [x] Full-bleed map with responsive, safe-area-aware overlays
- [x] Theme-aware bottom navigation and high-contrast map header
- [x] Scalable yardage text and constrained course names for smaller screens
- [x] Density-independent marker sizing, anchors, and themed info windows
- [x] Clearer distance-marker creation, dragging, dismissal, and first-use guidance
- [x] Map camera padding that keeps hole content clear of controls
- [x] Modern Android location and vibration APIs with legacy-device fallback
- [x] iOS-aligned lightweight course/yardage overlay
- [x] cupholder mode moves hole navigation above the map content
- [x] durable bag ID storage with legacy-key migration and complete mutation persistence

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
