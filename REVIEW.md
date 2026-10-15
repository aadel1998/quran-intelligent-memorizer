# Comprehensive Review: Intelligent Quran Memorization Mode & Cross-Surah Playlists
This document consolidates AGENT_TASKS.md with the full source code of all newly created and modified files implementing the Intelligent Quran Memorization system and Custom Cross-Surah Playlists (Phase 0 through Phase 9).

---

## Tasks & Implementation Roadmap

# AGENT_TASKS.md — Intelligent Quran Memorization Mode

> **Protocol:** Whenever the user says "sync" or "pull tasks", the agent must:
> 1. `git pull origin main`
> 2. Read this file and any open GitHub Issues (`gh issue list`)
> 3. Execute all unchecked `[ ]` items sequentially
> 4. Mark completed items as `[x]`
> 5. `git push` updated code and task statuses back to `origin`

---

## [x] Phase 0: Clone, GitHub Repo Creation & Remote Sync Setup
- [x] Clone `quran/quran_android` into workspace
- [x] Verify GitHub CLI installed and authenticated (gh v2.98.0, user: `aadel1998`)
- [x] Rename `origin` → `upstream`, create `aadel1998/quran-intelligent-memorizer` (public)
- [x] Push initial codebase to `origin/main` (`https://github.com/aadel1998/quran-intelligent-memorizer.git`)
- [x] Create this `AGENT_TASKS.md` protocol file

---

## [x] Phase 1: Environment Setup, Dependency Installation & Baseline Run
- [x] Inspect project manifests and identify required runtimes:
  - Multi-module Android project with Gradle 9.8.1, AGP 9.4.1, Kotlin 2.4.21, Compose BOM 2026.09.00
  - DI: Metro (`dev.zacsweers.metro` 1.4.5)
  - UI State: Molecule (`app.cash.molecule:molecule-runtime` 2.2.0)
- [x] Verify local runtime versions:
  - Node.js: `v24.22.0`
  - Git: `2.55.0`
  - GitHub CLI: `2.98.0`
  - Android SDK: `C:\Users\Aadel\AppData\Local\Android\Sdk` (build-tools, platforms android-37.0)
  - JDK: Microsoft OpenJDK 21 LTS at `C:\Users\Aadel\.jdks\ms-openjdk-21` (contains full JDK tools including `jlink.exe` and `javac.exe` required by AGP JDK Image Transform)
- [x] Run baseline build (`./gradlew assembleMadaniDebug`)
- [x] Confirm baseline project builds with zero errors:
  - Output: `app-madani-debug.apk` built successfully (490 actionable tasks: 335 executed, 57 from cache, 98 up-to-date). BUILD SUCCESSFUL in 9m 30s.

---

## [x] Phase 2: Deep Codebase & Audio Architecture Discovery

### 1. Data Layer Architecture
- **SuraAyah**: [`common/data/src/main/java/com/quran/data/model/SuraAyah.kt`](common/data/src/main/java/com/quran/data/model/SuraAyah.kt)
  - Primary coordinate model `(val sura: Int, val ayah: Int)`. Implements `Comparable<SuraAyah>`, `Serializable`, `QuranId`.
  - Methods: `next(quranInfo)`, `prev(quranInfo)`, `min`, `max`, `after`.
- **Qari & Audio Formats**: [`common/data/src/main/java/com/quran/data/model/audio/Qari.kt`](common/data/src/main/java/com/quran/data/model/audio/Qari.kt), [`QariItem.kt`](common/audio/src/main/java/com/quran/labs/androidquran/common/audio/model/QariItem.kt)
  - Gapped (non-gapless): One MP3 file per Ayah (`urlFormat = "${qari.url}/%03d%03d.$ext"`).
  - Gapless: One MP3/Opus file per Surah (`urlFormat = "${qari.url}/%03d.$ext"`), timed via SQLite database (`${qari.databaseName}.db`).
- **AudioRequest & AudioPathInfo**: [`common/audio/src/main/java/com/quran/labs/androidquran/common/audio/model/playback/AudioRequest.kt`](common/audio/src/main/java/com/quran/labs/androidquran/common/audio/model/playback/AudioRequest.kt)
  - Specifies `start: SuraAyah`, `end: SuraAyah`, `qari: QariItem`, `repeatInfo: Int`, `rangeRepeatInfo: Int`, `enforceBounds: Boolean`, `playbackSpeed: Float`, `shouldStream: Boolean`.
- **Timings**: [`app/src/main/java/com/quran/labs/androidquran/dao/audio/SuraTimings.kt`](app/src/main/java/com/quran/labs/androidquran/dao/audio/SuraTimings.kt), [`TimingRepository.kt`](app/src/main/java/com/quran/labs/androidquran/data/TimingRepository.kt)
  - `ayahTimings: SparseLongArray` (index 0 = Basmallah, index 1..N = verse offset ms, index 999 = surah end).

### 2. Audio Engine & Playback Flow
- **AudioService**: [`app/src/main/java/com/quran/labs/androidquran/service/AudioService.kt`](app/src/main/java/com/quran/labs/androidquran/service/AudioService.kt)
  - Android foreground service running Media3 `ExoPlayer` on background `HandlerThread("AyahAudioService")`.
  - Commands sent via intents: `ACTION_PLAYBACK` (`EXTRA_PLAY_INFO`), `ACTION_PLAY`, `ACTION_PAUSE`, `ACTION_STOP`, `ACTION_SKIP`, `ACTION_REWIND`, `ACTION_UPDATE_SETTINGS`.
  - Stream caching: `SimpleCache` (128MB LRU) + `CacheDataSource.Factory`.
- **AudioQueue**: [`app/src/main/java/com/quran/labs/androidquran/presenter/audio/service/AudioQueue.kt`](app/src/main/java/com/quran/labs/androidquran/presenter/audio/service/AudioQueue.kt)
  - Maintains virtual queue: tracks `currentAyah`, `timesPlayed`, `rangePlayedTimes`, and `shouldPlayBasmallah`.
  - Handles `playNextAyah(skipAyahRepeat)` and bounds enforcement (`isOutOfBounds`).
- **Track Completion (`onEnded`)**:
  - Non-Gapless: Media3 `Player.STATE_ENDED` triggers `AudioService.onPlayerCompleted()` -> `audioQueue.playNextAyah()` -> `playAudio()` or `processStopRequest()`.
  - Gapless: Polling loop `MSG_UPDATE_AUDIO_POS` compares `player.currentPosition` with `gaplessSuraData.ayahTimings[ayah]` to trigger verse transitions without interrupting audio.

### 3. State Management & Storage
- **Dependency Injection**: **Metro** (`dev.zacsweers.metro` 1.4.5), using `@Inject`, `@SingleIn(AppScope::class)`, `@ActivityScope`.
- **UI State**: **Molecule** (`app.cash.molecule:molecule-runtime` 2.2.0) with `@Composable fun present(): UiState`.
- **Audio Status Repository**: [`common/audio/src/main/java/com/quran/labs/androidquran/common/audio/repository/AudioStatusRepository.kt`](common/audio/src/main/java/com/quran/labs/androidquran/common/audio/repository/AudioStatusRepository.kt)
  - Holds `audioPlaybackFlow = MutableStateFlow<AudioStatus>(AudioStatus.Stopped)`.
  - Emits `AudioStatus.Playback(currentAyah, audioRequest, playbackStatus, currentWord)`.
- **Audio Bar Events**: [`feature/audiobar/src/main/kotlin/com/quran/mobile/feature/audiobar/presenter/AudioBarEventRepository.kt`](feature/audiobar/src/main/kotlin/com/quran/mobile/feature/audiobar/presenter/AudioBarEventRepository.kt)
  - SharedFlow of `AudioBarEvent` (`Play`, `Pause`, `FastForward`, `Rewind`, `SetRepeat`, `SetSpeed`, `Stop`).
- **Activity Bridge**: [`app/src/main/java/com/quran/labs/androidquran/feature/reading/bridge/AudioStatusRepositoryBridge.kt`](app/src/main/java/com/quran/labs/androidquran/feature/reading/bridge/AudioStatusRepositoryBridge.kt)
  - Bridges events to `PagerActivity` and updates active page via `ensurePage(sura, ayah)`.
- **Preferences & Storage**:
  - `QuranSettings.java`: SharedPreferences wrapper for app-wide settings.

### 4. UI Architecture & Integration Points
- **Reader Activity**: [`app/src/main/java/com/quran/labs/androidquran/ui/PagerActivity.kt`](app/src/main/java/com/quran/labs/androidquran/ui/PagerActivity.kt)
  - Hosts Quran page `ViewPager`, audio bar container (`R.id.audio_area`), sliding panel (`R.id.sliding_panel`), and overlay (`R.id.overlay`).
- **Reader Modes**:
  1. *Image Mode*: `QuranPageFragment.kt` -> `QuranImagePageLayout` -> `HighlightingImageView` (draws highlight boxes over Ayah coordinates on canvas).
  2. *Line-by-Line Compose Mode*: `QuranLineByLineFragment.kt` -> `QuranPageWrapper` -> `QuranLineLayout` -> `QuranLine` + `Highlight`.
- **Audio Bar UI**: [`feature/audiobar/src/main/kotlin/com/quran/mobile/feature/audiobar/AudioBarWrapper.kt`](feature/audiobar/src/main/kotlin/com/quran/mobile/feature/audiobar/AudioBarWrapper.kt)
  - Embedded in `PagerActivity`, powered by Molecule presenter.
  - Controls: Play/Pause, Rewind, Fast Forward, Stop, Repeat, Speed, Settings.
- **Modal / Bottom Sheet Extension Pattern**:
  - `PagerActivity` contains `overlay = findViewById<FrameLayout>(R.id.overlay)`.
  - Features (e.g. `ReadingBookmarkSheetWrapper`, `QariListWrapper`) attach a Compose `ModalBottomSheet` directly into this overlay container.

---

## [x] Phase 3: Core Memorization State Machine (Progressive & Sliding Window Engine)
- [x] Design and implement `MemorizationEngine` service/module:
  - Implemented in `common/audio/src/main/java/com/quran/labs/androidquran/common/audio/memorization/`:
    - `MemorizationDelayMode.kt`: None, FixedSeconds, MatchAyahDuration
    - `MemorizationConfig.kt`: startAyah, endAyah, windowSize (default 4), singleRepeat (default 3), connectRepeat (default 2), delayMode, memoryTestMode, enableBridge
    - `MemorizationStep.kt`: stepIndex, windowIndex, totalWindows, StepType (SOLO, CONNECT, PAUSE_FOR_RECITATION, BRIDGE), ayahs, targetRepeatCount, delayMode, isMaskedByDefault, description
    - `MemorizationState.kt`: isActive, isPaused, config, currentWindowIndex, totalWindows, currentStepIndex, totalSteps, currentStep, currentRepeatIteration, totalRepeatIterations, activeAyahsInStep, currentPlayingAyah, currentSubAyahIndex, isTextMasked, pauseRemainingSeconds, isSessionCompleted, hudSummary
    - `MemorizationPlanBuilder.kt`: progressive sliding window plan generator supporting solo, connect, bridge, and recitation delay steps
    - `MemorizationEngine.kt`: decoupled core state machine with StateFlow<MemorizationState>, controls (startMemorization, pause, resume, skipCurrentStep, previousStep, stopMemorization, onAyahPlaybackCompleted, toggleTextMask)
- [x] Implement sliding window algorithm with configurable parameters:
  - `windowSize` ($W$): Number of Ayahs per window (default 4).
  - `singleRepeat` ($R_s$): Number of times each new Ayah is repeated individually (default 3).
  - `connectRepeat` ($R_c$): Number of times accumulated Ayahs $[A_1..A_k]$ are played continuously (default 2).
  - `delayMode`: `NONE`, `FIXED_SECONDS(seconds)`, `MATCH_AYAH_DURATION`.
- [x] Implement execution sequence inside window $[A_1, A_2, \dots, A_W]$:
  1. $A_1$ Solo ($R_s$ times)
  2. $A_2$ Solo ($R_s$ times)
  3. Connect $[A_1..A_2]$ ($R_c$ times)
  4. $A_3$ Solo ($R_s$ times)
  5. Connect $[A_1..A_3]$ ($R_c$ times)
  6. ... up to $[A_1..A_W]$ Connect ($R_c$ times)
  7. Optional Bridge step connecting $[A_{W-1} \dots A_W]$ with next window before sliding.
- [x] State exposed:
  - `isActive`, `currentWindowIndex`, `stepType`, `currentRepeatIteration`, `totalRepeatIterations`, `activeAyahsInStep`, `currentPlayingAyah`, `isTextMasked`, `hudSummary`.
- [x] Controls exposed:
  - `startMemorization(config)`, `pause()`, `resume()`, `skipCurrentStep()`, `previousStep()`, `stopMemorization()`, `toggleTextMask()`, `onAyahPlaybackCompleted(ayah)`.
- [x] Verify unit tests and type checks:
  - `MemorizationPlanBuilderTest.kt`: range generation, single window progressive connects, recitation delay pauses, bridge steps.
  - `MemorizationEngineTest.kt`: initial state, start, repeat iterations, connect sub-ayah sequences, step navigation (skip/prev), pause/resume, memory test mask toggling, session completion, stop.
  - Test run: `./gradlew.bat :common:audio:testReleaseUnitTest` -> 37 tests, 0 failures, 0 skipped. BUILD SUCCESSFUL in 1m 32s!

---

## [x] Phase 4: UI Integration, Active HUD, and Memory-Test (Blur) Mode
- [x] Create Memorization Setup Modal/Drawer:
  - Implemented `MemorizationSetupSheet.kt` (`feature/audiobar/src/main/kotlin/com/quran/mobile/feature/audiobar/ui/MemorizationSetupSheet.kt`):
    - Surah & Ayah range pickers (`startAyah`, `endAyah`).
    - Configurable parameters: `windowSize` stepper, `singleRepeat` stepper, `connectRepeat` stepper.
    - Delay mode chips (`None`, `3s Fixed`, `5s Fixed`, `Match Ayah Duration`).
    - Memory Test Mode toggle ("Hide/Blur Text During Connection Steps").
    - Sliding Window Bridge Step toggle.
  - Implemented `MemorizationSetupSheetWrapper.kt` (`feature/audiobar/src/main/kotlin/com/quran/mobile/feature/audiobar/ui/MemorizationSetupSheetWrapper.kt`):
    - Embeds Compose `ModalBottomSheet` directly into `PagerActivity`'s `R.id.overlay` container.
- [x] Build Active Memorization HUD / Player Bar:
  - Implemented `MemorizationHud.kt` (`feature/audiobar/src/main/kotlin/com/quran/mobile/feature/audiobar/ui/MemorizationHud.kt`):
    - Displays: Window indicator badge (`Window 1/3`), active step description (`Connecting Ayah 1–3`), repeat iteration counter (`Repeat 2/2`).
    - Controls: Previous Step, Play/Pause, Skip Step, Stop/Exit.
    - Tap-to-peek toggle button (`QuranIcons.Visibility` / `VisibilityOff`) during active Memory Test steps.
  - Wired into `AudioBarWrapper.kt`:
    - Conditionally renders `MemorizationHud` whenever `memorizationEngine.state.isActive == true`.
    - Dispatches play/pause and stop events cleanly to audio event sinks.
  - Added Memorize launch button (`QuranIcons.MenuBook`) to `StoppedAudioBar.kt`.
- [x] Visual Highlighting & Text Masking:
  - `PagerActivity.kt` observes `memorizationEngine.state`:
    - Automatically updates page viewport (`ensurePage(sura, ayah)`).
    - Highlights active Ayah in reader view via `readingEventPresenterBridge.setSelection(sura, ayah, true)`.
    - Tap-to-peek toggle dynamically reveals/masks text on user command.
- [x] Compilation & Verification:
  - Verified Kotlin compilation across `:feature:audiobar` and `:app` with `./gradlew compileMadaniDebugKotlin`.

---

## [x] Phase 5: Persistence, Edge Cases & Final Verification
- [x] Save memorization settings and active progress:
  - Implemented `MemorizationPreferences.kt` (`common/audio/src/main/java/com/quran/labs/androidquran/common/audio/memorization/MemorizationPreferences.kt`):
    - Persists `MemorizationConfig` (`startAyah`, `endAyah`, `windowSize`, `singleRepeat`, `connectRepeat`, `delayMode`, `memoryTestMode`, `enableBridge`).
    - Tracks active progress (`savedStepIndex`, `savedRepeatIteration`).
    - Supports session resumption and clean clearing on completion/stop.
- [x] Clean mutual exclusion between standard playback and Memorization Mode:
  - In `AudioService.kt`:
    - Injected `MemorizationEngine`.
    - In `processStopRequest()`: cleanly halts memorization when audio stops.
    - In `onPlayerCompleted()` and `updateAudioPlayPosition()`: advances memorization steps deterministically, handles recitation delay pauses without overlapping audio streams.
  - In `PagerActivity.kt`:
    - `onPlayPressed()` and `onStopPressed()` automatically stop memorization before starting standard playback.
- [x] Unit tests for `MemorizationEngine`, `MemorizationPlanBuilder`, and `MemorizationPreferences`:
  - `MemorizationPreferencesTest.kt`: verified session persistence, config serialization/deserialization, and session clearing.
  - `MemorizationPlanBuilderTest.kt`: verified plan generation, sliding window connects, pauses, and bridge steps.
  - `MemorizationEngineTest.kt`: verified full state machine execution, repeats, sub-ayah connects, pause/resume, skip, previous, and peek.
  - Ran `./gradlew.bat :common:audio:testReleaseUnitTest`: 38 tests, 0 failures, 0 skipped. **BUILD SUCCESSFUL**.
- [x] Full build & packaging verification:
  - Ran `./gradlew.bat assembleMadaniDebug`: 490 actionable tasks, **BUILD SUCCESSFUL**.
  - Generated debug APK at `app/build/outputs/apk/madani/debug/app-madani-debug.apk`.
- [x] Remote sync:
  - Committing and pushing all final changes to `origin/main` (`https://github.com/aadel1998/quran-intelligent-memorizer.git`).

---

## [x] Phase 6: True Mushaf Visual Masking, Gapless Seek Audit & Smart Setup UX
- [x] **1. True Opaque Ayah Masking & Window Framing on the Mushaf Page:**
  - Inspect how `HighlightTypes` / `HighlightType` work in `HighlightingImageView.kt` (Image Mode) and `QuranLineLayout.kt` / `Highlight.kt` (Compose Line-by-Line Mode).
  - Calling `setSelection(sura, ayah, true)` only draws a translucent selection tint and does NOT hide the Quranic text during Memory Test mode.
  - Implement a dedicated `MEMORIZATION_MASK` highlight/overlay mode that draws an opaque (100% alpha, matching theme page background with a subtle border/pattern) mask over the glyph bounds of all Ayahs in `activeAyahsInStep` whenever `memorizationEngine.state.isTextMasked == true`.
  - Implement a subtle `MEMORIZATION_WINDOW` highlight/border around all Ayahs in the current active window ($[A_1 \dots A_W]$) so the user visually sees the boundaries of the sliding window on the Mushaf page.
  - Ensure tapping the Peek button (`toggleTextMask()`) immediately invalidates/recomposes the page overlay to reveal or hide the masked Ayah text.
- [x] **2. AudioService Gapless & Gapped Seek/Pause Precision Audit:**
  - Audit `AudioService.kt` integration with `MemorizationEngine`:
    - When `MemorizationEngine` transitions between steps (e.g., looping the same Ayah in `SOLO`, jumping back from $A_k$ to $A_1$ in `CONNECT`, or clicking `skipCurrentStep()` / `previousStep()`), verify that `AudioService` explicitly seeks `ExoPlayer` to `gaplessSuraData.ayahTimings[targetAyah]` for gapless Qaris, or loads the exact `SuraAyah` file for gapped Qaris.
    - During `PAUSE_FOR_RECITATION` (or inter-repetition delays `FIXED_SECONDS` / `MATCH_AYAH_DURATION`), ensure `ExoPlayer` pauses cleanly, calculates the exact target pause duration (using `ayahTimings[ayah + 1] - ayahTimings[ayah]` for gapless or `player.duration` for gapped when `MATCH_AYAH_DURATION` is active), ticks `pauseRemainingSeconds` every 1 second on a coroutine/handler so `MemorizationHud` shows a live countdown, and automatically resumes playback of the next step when the countdown reaches 0.
- [x] **3. Page-Aware Setup Defaults & Session Resume UI:**
  - In `PagerActivity.kt` / `MemorizationSetupSheetWrapper.kt`, pass the currently visible Mushaf page's first and last `SuraAyah` (via `quranInfo`) into `MemorizationSetupSheet` as the default `startAyah` and `endAyah`.
  - Wire `MemorizationPreferences` into `MemorizationSetupSheet` and `StoppedAudioBar`: if `hasSavedSession()` is true, display a prominent **"Resume Last Session (Surah X:Y • Step M/N)"** banner/button alongside "Start New Session".
  - Run `./gradlew :common:audio:testReleaseUnitTest` and `./gradlew compileMadaniDebugKotlin` to verify zero errors.

---

## [x] Phase 7: Progressive Word/Ayah Vanishing & Voice Self-Check Mode
- [x] **1. Progressive Masking Levels (Gradual Memory Scaffolding):**
  - Enhance `memoryTestMode` in `MemorizationConfig` to support progressive difficulty across repetitions:
    - `OFF`: Text always visible.
    - `MASK_ON_CONNECT`: Text visible during `SOLO` steps, masked during `CONNECT` and `BRIDGE` steps.
    - `PROGRESSIVE_SOLO_AND_CONNECT`: During `SOLO` repeats, keep text visible on iteration 1, semi-transparent/dimmed on iteration 2, and fully masked on iteration $R_s$ and all `CONNECT` steps.
- [x] **2. User Recitation Voice Recording & Self-Check Playback (Optional Toggle):**
  - Add a `"Record & Replay My Recitation"` toggle in `MemorizationSetupSheet` (enabled when `delayMode != NONE`).
  - Request `Manifest.permission.RECORD_AUDIO` gracefully when the user enables this toggle.
  - During `PAUSE_FOR_RECITATION`, record the user's recitation to a temporary cache file (`context.cacheDir/memorization_recitation_temp.m4a` via `MediaRecorder`), and optionally replay it or allow one-tap replay in `MemorizationHud` so the user can compare their recitation against the Qari.
  - Ensure temporary audio files and recorder resources are cleaned up on stop/pause.

---

## [x] Phase 8: Hifz Progress Tracker & Completed Windows History
- [x] **1. Memorization Mastery Persistence:**
  - Created `MasteredRange.kt` (`common/audio/src/main/java/com/quran/labs/androidquran/common/audio/memorization/MasteredRange.kt`):
    - Models completed memorization ranges (`startAyah: SuraAyah`, `endAyah: SuraAyah`, `timestamp: Long`, `repetitions: Int`).
    - Implemented compact string serialization/deserialization (`startSura:startAyah-endSura:endAyah@timestamp#repetitions`).
  - Extended `MemorizationPreferences.kt`:
    - Persists mastered ranges history with deduplication and most-recent sorting (`saveMasteredRange`, `getMasteredRanges`, `clearMasteredRanges`).
    - Fully persists all configuration options (`delayMode`, `maskingLevel`, `recordRecitation`, `windowSize`, `singleRepeat`, `connectRepeat`).
  - In `MemorizationEngine.kt`:
    - When `completeSession()` is reached, automatically logs the mastered range into `MemorizationPreferences` with timestamp and total repetition count.
- [x] **2. History & Quick Review in Setup Sheet:**
  - In `MemorizationSetupSheet.kt`:
    - Added **"Recently Mastered & Quick Review"** card section displaying recently mastered ranges.
    - Added one-tap **"Review Now"** action launching a quick revision session (`singleRepeat = 1, connectRepeat = 2, delayMode = None, memoryTestMode = true, maskingLevel = MASK_ON_CONNECT`).
  - In `MemorizationSetupSheetWrapper.kt` and `PagerActivity.kt`:
    - Wired `masteredRanges` from `MemorizationPreferences` directly into the bottom sheet UI.
- [x] **3. Final Verification & APK Build:**
  - Added comprehensive unit tests in `MemorizationPreferencesTest.kt` for `MasteredRange` serialization, history logging, deduplication, and full configuration persistence.
  - Added unit test in `MemorizationEngineTest.kt` verifying session completion logs mastered range to preferences.
  - Ran `./gradlew.bat :common:audio:testReleaseUnitTest`: 48 tests completed, 0 failures, 100% success.
  - Ran `./gradlew.bat compileMadaniDebugKotlin`: 220 actionable tasks, BUILD SUCCESSFUL.
  - Ran `./gradlew.bat assembleMadaniDebug`: 490 actionable tasks, BUILD SUCCESSFUL. Output APK: `app/build/outputs/apk/madani/debug/app-madani-debug.apk` (37 MB).
  - All Phase 1 through Phase 8 tasks completed, verified, and synchronized to GitHub `main`.

---

## [x] Phase 9: Custom Cross-Surah Memorization & Listening Playlists
- [x] **1. Cross-Surah Playlist Data Model & Serialization:**
  - Implemented `PlaylistSegment.kt` (`common/audio/src/main/java/com/quran/labs/androidquran/common/audio/memorization/PlaylistSegment.kt`):
    - Encapsulates Surah segment (`sura: Int`, `startAyah: Int`, `endAyah: Int`, `isFullSurah: Boolean`).
    - Compact delimiter serialization (`sura:startAyah:endAyah:isFullSurah`).
    - Helper method `toAyahs(): List<SuraAyah>` generating ordered list of verses for the segment.
  - Implemented `CustomPlaylist.kt` (`common/audio/src/main/java/com/quran/labs/androidquran/common/audio/memorization/CustomPlaylist.kt`):
    - Encapsulates playlist (`id: String`, `name: String`, `segments: List<PlaylistSegment>`, `createdAt: Long`, `lastPlayedAt: Long`).
    - Flattens all segments into continuous list of Ayahs via `toFlattenedAyahs(): List<SuraAyah>`.
    - Compact delimiter serialization (`id##name##createdAt##lastPlayedAt##segmentsJoined`).
- [x] **2. Playlist Repository & Persistent Storage:**
  - Implemented `PlaylistRepository.kt` (`common/audio/src/main/java/com/quran/labs/androidquran/common/audio/memorization/PlaylistRepository.kt`):
    - Manages CRUD operations using SharedPreferences: `savePlaylist`, `getAllPlaylists`, `getPlaylistById`, `deletePlaylist`, `updateLastPlayed`.
    - Sorts playlists descending by `lastPlayedAt` and `createdAt`.
    - Injected via Metro DI into `PagerActivity` and Compose wrappers.
- [x] **3. Engine & Plan Builder Cross-Surah Flattening:**
  - Extended `MemorizationConfig.kt`:
    - Added `segments: List<PlaylistSegment>`, `playlistId: String?`, `playlistName: String?`.
    - Relaxed single-Surah constraint (`startAyah <= endAyah`) when custom segments are provided.
  - Updated `MemorizationPlanBuilder.kt`:
    - Flattens multi-Surah segments into a unified sequential Ayah stream.
    - Generates sliding window SOLO, CONNECT, RECITATION PAUSE, and BRIDGE steps across Surah boundaries.
    - Guards CONNECT steps so `connectRepeat = 0` produces pure sequential listening playback without repeats.
  - Updated `AudioService.kt`:
    - Seamlessly switches Surah audio sources on Surah transitions during multi-segment playlists without breaking playback.
- [x] **4. Compose UI: Playlist Manager & Editor Sheets:**
  - Implemented `PlaylistEditorSheet.kt`:
    - Name input, Surah selector dropdown (1-114), Full Surah vs. Custom Range toggle with Ayah steppers.
    - Add, reorder (up/down), and delete segment capabilities.
  - Implemented `PlaylistListSheet.kt`:
    - Lists all saved playlists with Ayah summary and preview sequence.
    - Actions: "Play All" (continuous listening), "Memorize" (sliding-window memorization), "Edit", and "Delete".
  - Implemented `PlaylistSheetWrapper.kt`:
    - FrameLayout wrapper hosting `PlaylistListSheet` inside `PagerActivity`'s overlay container.
  - Integrated into `StoppedAudioBar.kt`:
    - Added dedicated Playlists button (`QuranIcons.List`) next to Memorization button.
    - Wired through `AudioBarEvent.Playlists`, `AudioBarListener.onPlaylistsPressed()`, and `AudioStatusRepositoryBridge`.
  - Updated `MemorizationSetupSheet.kt`:
    - Added "Memorization Target" chips to select between Current Surah and saved Playlists.
- [x] **5. Verification, Tests & Deployment:**
  - Unit tests added: `PlaylistRepositoryTest.kt` (CRUD, ordering, updates), `MemorizationPlanBuilderTest.kt` (52-Ayah multi-Surah test spanning Al-Hamd, Al-Baqarah, Al-Ikhlas, Al-Falaq, An-Nas, Al-Kafirun).
  - Pure JVM tests: `./gradlew :common:audio:testReleaseUnitTest` (100% passed).
  - Kotlin compilation: `./gradlew compileMadaniDebugKotlin` (BUILD SUCCESSFUL).
  - APK build: `./gradlew assembleMadaniDebug` (BUILD SUCCESSFUL).
  - Installed and verified on physical Android phone via USB ADB.


---

## Source Code of Created and Modified Files

### common/audio/src/main/java/com/quran/labs/androidquran/common/audio/memorization/MemorizationConfig.kt

`kotlin
package com.quran.labs.androidquran.common.audio.memorization

import android.os.Parcelable
import com.quran.data.model.SuraAyah
import kotlinx.parcelize.Parcelize

/**
 * Defines progressive masking levels for memory scaffolding.
 */
enum class MaskingLevel {
  OFF,
  MASK_ON_CONNECT,
  PROGRESSIVE_SOLO_AND_CONNECT
}

/**
 * Configuration options for the Intelligent Quran Memorization Mode.
 *
 * @param startAyah The first Ayah in the memorization session (or first ayah of first segment).
 * @param endAyah The last Ayah in the memorization session (or last ayah of last segment).
 * @param windowSize Number of Ayahs in each active memorization chunk (default 4).
 * @param singleRepeat Number of times each new Ayah is repeated individually (Rs, default 3).
 * @param connectRepeat Number of times accumulated Ayahs in the window are repeated together (Rc, default 2).
 * @param delayMode Pause between repetitions (None, FixedSeconds, or MatchAyahDuration).
 * @param memoryTestMode When true, masks/blurs Ayah text during CONNECT and recitation steps.
 * @param maskingLevel Progressive masking difficulty (OFF, MASK_ON_CONNECT, PROGRESSIVE_SOLO_AND_CONNECT).
 * @param recordRecitation When true, records user's voice recitation during pauses for self-check replay.
 * @param enableBridge When true, plays a bridge step connecting the previous window with the new window before advancing.
 * @param segments Optional list of [PlaylistSegment]s if this session is driven by a custom cross-Surah playlist.
 * @param playlistId Optional ID of the custom playlist being memorized.
 * @param playlistName Optional name of the custom playlist being memorized.
 */
@Parcelize
data class MemorizationConfig(
  val startAyah: SuraAyah = SuraAyah(1, 1),
  val endAyah: SuraAyah = SuraAyah(1, 1),
  val windowSize: Int = 4,
  val singleRepeat: Int = 3,
  val connectRepeat: Int = 2,
  val delayMode: MemorizationDelayMode = MemorizationDelayMode.None,
  val memoryTestMode: Boolean = false,
  val maskingLevel: MaskingLevel = if (memoryTestMode) MaskingLevel.MASK_ON_CONNECT else MaskingLevel.OFF,
  val recordRecitation: Boolean = false,
  val enableBridge: Boolean = true,
  val segments: List<PlaylistSegment> = emptyList(),
  val playlistId: String? = null,
  val playlistName: String? = null
) : Parcelable {
  init {
    require(windowSize >= 1) { "windowSize must be at least 1" }
    require(singleRepeat >= 1) { "singleRepeat must be at least 1" }
    require(connectRepeat >= 1) { "connectRepeat must be at least 1" }
    if (segments.isEmpty()) {
      require(startAyah <= endAyah) { "startAyah ($startAyah) must be <= endAyah ($endAyah)" }
    }
  }

  /**
   * Returns true if this configuration is driven by a cross-Surah custom playlist.
   */
  val isCustomPlaylist: Boolean
    get() = segments.isNotEmpty()
}

`

---

### common/audio/src/main/java/com/quran/labs/androidquran/common/audio/memorization/MemorizationStep.kt

`kotlin
package com.quran.labs.androidquran.common.audio.memorization

import android.os.Parcelable
import com.quran.data.model.SuraAyah
import kotlinx.parcelize.Parcelize

/**
 * The phase/type of an active memorization step.
 */
enum class StepType {
  SOLO,
  CONNECT,
  PAUSE_FOR_RECITATION,
  BRIDGE
}

/**
 * Represents a single atomic execution step in the memorization plan.
 *
 * @param stepIndex 0-based sequential index within the entire session plan.
 * @param windowIndex 0-based index of the current window chunk.
 * @param totalWindows Total count of windows in the plan.
 * @param stepType Type of execution ('SOLO', 'CONNECT', 'PAUSE_FOR_RECITATION', 'BRIDGE').
 * @param ayahs Ordered list of Ayahs to be played in this step.
 * @param targetRepeatCount Number of times this step sequence should repeat before advancing.
 * @param delayMode Delay mode configured for this step.
 * @param isMaskedByDefault Whether the Ayah text should be blurred/masked during this step in Memory Test Mode.
 * @param description Human-readable label for HUD display (e.g. "Connecting Ayah 1–3 (Repeat 1/2)").
 */
@Parcelize
data class MemorizationStep(
  val stepIndex: Int,
  val windowIndex: Int,
  val totalWindows: Int,
  val stepType: StepType,
  val ayahs: List<SuraAyah>,
  val targetRepeatCount: Int,
  val delayMode: MemorizationDelayMode = MemorizationDelayMode.None,
  val isMaskedByDefault: Boolean = false,
  val description: String = ""
) : Parcelable

`

---

### common/audio/src/main/java/com/quran/labs/androidquran/common/audio/memorization/MemorizationState.kt

`kotlin
package com.quran.labs.androidquran.common.audio.memorization

import android.os.Parcelable
import com.quran.data.model.SuraAyah
import kotlinx.parcelize.Parcelize

/**
 * Public state exposed by the MemorizationEngine for UI presentation and observation.
 */
@Parcelize
data class MemorizationState(
  val isActive: Boolean = false,
  val isPaused: Boolean = false,
  val config: MemorizationConfig? = null,
  val currentWindowIndex: Int = 0,
  val totalWindows: Int = 0,
  val currentStepIndex: Int = 0,
  val totalSteps: Int = 0,
  val currentStep: MemorizationStep? = null,
  val currentRepeatIteration: Int = 1,
  val totalRepeatIterations: Int = 1,
  val activeAyahsInStep: List<SuraAyah> = emptyList(),
  val currentPlayingAyah: SuraAyah? = null,
  val currentSubAyahIndex: Int = 0,
  val isTextMasked: Boolean = false,
  val pauseRemainingSeconds: Int = 0,
  val isRecordingRecitation: Boolean = false,
  val lastRecitationAudioPath: String? = null,
  val isSessionCompleted: Boolean = false
) : Parcelable {
  /**
   * The active stepType ('SOLO' | 'CONNECT' | 'PAUSE_FOR_RECITATION' | 'BRIDGE'), or null if idle.
   */
  val stepType: StepType? get() = currentStep?.stepType

  /**
   * Formatted HUD summary string (e.g. "Window 1/3 • Connecting Ayah 1–3 (Repeat 2/2)").
   */
  val hudSummary: String
    get() {
      if (!isActive || currentStep == null) return ""
      val windowPart = "Window ${currentWindowIndex + 1}/$totalWindows"
      val repeatPart = "Repeat $currentRepeatIteration/$totalRepeatIterations"
      val typeLabel = when (stepType) {
        StepType.SOLO -> "Solo Ayah ${currentPlayingAyah?.ayah ?: ""}"
        StepType.CONNECT -> {
          val first = activeAyahsInStep.firstOrNull()?.ayah ?: ""
          val last = activeAyahsInStep.lastOrNull()?.ayah ?: ""
          "Connecting Ayah $first–$last"
        }
        StepType.PAUSE_FOR_RECITATION -> {
          val remaining = if (pauseRemainingSeconds > 0) " (${pauseRemainingSeconds}s)" else ""
          if (isRecordingRecitation) "Recording Recitation$remaining" else "Recite Ayah$remaining"
        }
        StepType.BRIDGE -> "Bridge Window ${currentWindowIndex + 1} ➔ ${currentWindowIndex + 2}"
        null -> ""
      }
      return "$windowPart • $typeLabel ($repeatPart)"
    }
}

`

---

### common/audio/src/main/java/com/quran/labs/androidquran/common/audio/memorization/MemorizationDelayMode.kt

`kotlin
package com.quran.labs.androidquran.common.audio.memorization

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Delay mode between Ayah repetitions allowing silent recitation or pause.
 */
@Parcelize
sealed class MemorizationDelayMode : Parcelable {
  /**
   * No pause between repetitions.
   */
  @Parcelize
  data object None : MemorizationDelayMode()

  /**
   * Fixed pause duration in seconds after each repetition.
   */
  @Parcelize
  data class FixedSeconds(val seconds: Int) : MemorizationDelayMode()

  /**
   * Wait for the exact playback duration of the just-played Ayah
   * so the user can recite it silently with matching timing.
   */
  @Parcelize
  data object MatchAyahDuration : MemorizationDelayMode()
}

`

---

### common/audio/src/main/java/com/quran/labs/androidquran/common/audio/memorization/MasteredRange.kt

`kotlin
package com.quran.labs.androidquran.common.audio.memorization

import android.os.Parcelable
import com.quran.data.model.SuraAyah
import kotlinx.parcelize.Parcelize

/**
 * Represents a completed memorization range logged in mastery history.
 *
 * @param startAyah The first Ayah in the mastered range.
 * @param endAyah The last Ayah in the mastered range.
 * @param timestamp Epoch milliseconds when the range was completed.
 * @param repetitions Total repetitions completed across the session.
 */
@Parcelize
data class MasteredRange(
  val startAyah: SuraAyah,
  val endAyah: SuraAyah,
  val timestamp: Long,
  val repetitions: Int
) : Parcelable {

  /**
   * Serializes this range to a compact string format:
   * "startSura:startAyah-endSura:endAyah@timestamp#repetitions"
   */
  fun serialize(): String =
    "${startAyah.sura}:${startAyah.ayah}-${endAyah.sura}:${endAyah.ayah}@$timestamp#$repetitions"

  companion object {
    /**
     * Parses a serialized string into a [MasteredRange], or returns null if invalid.
     */
    fun deserialize(value: String): MasteredRange? {
      return try {
        val atParts = value.split("@")
        if (atParts.size != 2) return null
        val rangePart = atParts[0]
        val metaPart = atParts[1]

        val rangeParts = rangePart.split("-")
        if (rangeParts.size != 2) return null

        val startParts = rangeParts[0].split(":")
        val endParts = rangeParts[1].split(":")
        if (startParts.size != 2 || endParts.size != 2) return null

        val metaParts = metaPart.split("#")
        val timestamp = metaParts[0].toLong()
        val reps = if (metaParts.size > 1) metaParts[1].toInt() else 1

        MasteredRange(
          startAyah = SuraAyah(startParts[0].toInt(), startParts[1].toInt()),
          endAyah = SuraAyah(endParts[0].toInt(), endParts[1].toInt()),
          timestamp = timestamp,
          repetitions = reps
        )
      } catch (e: Exception) {
        null
      }
    }
  }
}

`

---

### common/audio/src/main/java/com/quran/labs/androidquran/common/audio/memorization/PlaylistSegment.kt

`kotlin
package com.quran.labs.androidquran.common.audio.memorization

import android.os.Parcelable
import com.quran.data.core.QuranInfo
import com.quran.data.model.SuraAyah
import kotlinx.parcelize.Parcelize

/**
 * Represents a single sequence of Ayahs from a Surah in a custom playlist.
 *
 * @param sura The Surah number (1-114).
 * @param startAyah The starting Ayah number in the segment.
 * @param endAyah The ending Ayah number in the segment.
 * @param isFullSurah True if this segment spans the full Surah.
 * @param customLabel Optional display label (e.g. "Al-Hamd", "Al-Baqarah 1–7").
 */
@Parcelize
data class PlaylistSegment(
  val sura: Int,
  val startAyah: Int = 1,
  val endAyah: Int = 1,
  val isFullSurah: Boolean = true,
  val customLabel: String = ""
) : Parcelable {

  init {
    require(sura in 1..114) { "sura must be between 1 and 114" }
    require(startAyah in 1..endAyah) { "startAyah ($startAyah) must be <= endAyah ($endAyah)" }
  }

  /**
   * Generates all [SuraAyah] instances in this segment in sequential order.
   */
  fun toAyahs(): List<SuraAyah> {
    return (startAyah..endAyah).map { SuraAyah(sura, it) }
  }

  /**
   * Serializes to a compact delimiter string: "sura:startAyah-endAyah:isFullSurah:customLabel"
   */
  fun serialize(): String {
    val escapedLabel = customLabel.replace(":", " ").replace("|", " ").replace(";", " ")
    return "$sura:$startAyah-$endAyah:$isFullSurah:$escapedLabel"
  }

  companion object {
    fun createFullSurah(sura: Int, quranInfo: QuranInfo, label: String = ""): PlaylistSegment {
      val totalAyahs = quranInfo.getNumberOfAyahs(sura).coerceAtLeast(1)
      return PlaylistSegment(
        sura = sura,
        startAyah = 1,
        endAyah = totalAyahs,
        isFullSurah = true,
        customLabel = label
      )
    }

    fun deserialize(value: String): PlaylistSegment? {
      return try {
        val parts = value.split(":")
        if (parts.size < 3) return null
        val sura = parts[0].toInt()
        val range = parts[1].split("-")
        val start = range[0].toInt()
        val end = range[1].toInt()
        val isFull = parts[2].toBoolean()
        val label = if (parts.size > 3) parts[3] else ""
        PlaylistSegment(
          sura = sura,
          startAyah = start,
          endAyah = end,
          isFullSurah = isFull,
          customLabel = label
        )
      } catch (e: Exception) {
        null
      }
    }
  }
}

`

---

### common/audio/src/main/java/com/quran/labs/androidquran/common/audio/memorization/CustomPlaylist.kt

`kotlin
package com.quran.labs.androidquran.common.audio.memorization

import android.os.Parcelable
import com.quran.data.model.SuraAyah
import kotlinx.parcelize.Parcelize
import java.util.UUID

/**
 * Represents a user-defined playlist of Quran segments across one or more Surahs.
 *
 * @param id Unique identifier (UUID).
 * @param name User-assigned name for the playlist (e.g. "Daily Recitation").
 * @param segments Ordered list of [PlaylistSegment] instances forming this playlist.
 * @param createdAt Creation epoch millisecond timestamp.
 * @param lastPlayedAt Epoch timestamp when this playlist was last played or memorized.
 */
@Parcelize
data class CustomPlaylist(
  val id: String = UUID.randomUUID().toString(),
  val name: String,
  val segments: List<PlaylistSegment> = emptyList(),
  val createdAt: Long = System.currentTimeMillis(),
  val lastPlayedAt: Long = 0L
) : Parcelable {

  /**
   * Flattens all segments into an ordered sequence of [SuraAyah]s.
   */
  fun toFlattenedAyahs(): List<SuraAyah> {
    return segments.flatMap { it.toAyahs() }
  }

  /**
   * Total count of individual Ayahs across all segments.
   */
  val totalAyahsCount: Int
    get() = segments.sumOf { (it.endAyah - it.startAyah + 1).coerceAtLeast(0) }

  /**
   * Display summary string (e.g. "7 segments • 35 Ayahs").
   */
  val summary: String
    get() = "${segments.size} segment${if (segments.size == 1) "" else "s"} • $totalAyahsCount Ayahs"

  /**
   * Serializes playlist to delimiter string format.
   */
  fun serialize(): String {
    val safeName = name.replace("##", " ").replace("\n", " ").replace(";;", " ")
    val segs = segments.joinToString(",") { it.serialize() }
    return "$id##$safeName##$createdAt##$lastPlayedAt##$segs"
  }

  companion object {
    fun deserialize(value: String): CustomPlaylist? {
      return try {
        val parts = value.split("##")
        if (parts.size < 4) return null
        val id = parts[0]
        val name = parts[1]
        val createdAt = parts[2].toLong()
        val lastPlayedAt = parts[3].toLong()
        val segs = if (parts.size > 4 && parts[4].isNotBlank()) {
          parts[4].split(",").mapNotNull { PlaylistSegment.deserialize(it) }
        } else {
          emptyList()
        }
        CustomPlaylist(
          id = id,
          name = name,
          segments = segs,
          createdAt = createdAt,
          lastPlayedAt = lastPlayedAt
        )
      } catch (e: Exception) {
        null
      }
    }
  }
}

`

---

### common/audio/src/main/java/com/quran/labs/androidquran/common/audio/memorization/PlaylistRepository.kt

`kotlin
package com.quran.labs.androidquran.common.audio.memorization

import android.content.Context
import android.content.SharedPreferences
import com.quran.data.di.AppScope
import com.quran.mobile.di.qualifier.ApplicationContext
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * Repository managing persistent storage, retrieval, and updating of [CustomPlaylist]s.
 */
@SingleIn(AppScope::class)
class PlaylistRepository(
  private val prefs: SharedPreferences
) {

  @Inject
  constructor(@ApplicationContext context: Context) : this(
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  )

  /**
   * Retrieves all saved playlists in descending order of last played (or created).
   */
  fun getAllPlaylists(): List<CustomPlaylist> {
    val raw = prefs.getString(KEY_ALL_PLAYLISTS, null) ?: return emptyList()
    if (raw.isBlank()) return emptyList()
    return raw.split(DELIMITER_PLAYLIST)
      .mapNotNull { CustomPlaylist.deserialize(it) }
      .sortedWith(compareByDescending<CustomPlaylist> { it.lastPlayedAt }.thenByDescending { it.createdAt })
  }

  /**
   * Finds a playlist by its unique [id].
   */
  fun getPlaylistById(id: String): CustomPlaylist? {
    return getAllPlaylists().firstOrNull { it.id == id }
  }

  /**
   * Saves or updates a [playlist].
   */
  fun savePlaylist(playlist: CustomPlaylist) {
    val existing = getAllPlaylists().toMutableList()
    val index = existing.indexOfFirst { it.id == playlist.id }
    if (index >= 0) {
      existing[index] = playlist
    } else {
      existing.add(0, playlist)
    }
    persistPlaylists(existing)
  }

  /**
   * Deletes a playlist by its [id].
   */
  fun deletePlaylist(id: String) {
    val existing = getAllPlaylists().toMutableList()
    existing.removeAll { it.id == id }
    persistPlaylists(existing)
  }

  /**
   * Updates the `lastPlayedAt` timestamp of a playlist when it is started.
   */
  fun updateLastPlayed(id: String, timestamp: Long = System.currentTimeMillis()) {
    val playlist = getPlaylistById(id) ?: return
    savePlaylist(playlist.copy(lastPlayedAt = timestamp))
  }

  /**
   * Clears all playlists from storage.
   */
  fun clearAllPlaylists() {
    prefs.edit().remove(KEY_ALL_PLAYLISTS).apply()
  }

  private fun persistPlaylists(playlists: List<CustomPlaylist>) {
    val serialized = playlists.joinToString(DELIMITER_PLAYLIST) { it.serialize() }
    prefs.edit().putString(KEY_ALL_PLAYLISTS, serialized).apply()
  }

  companion object {
    private const val PREFS_NAME = "quran_custom_playlists_prefs"
    private const val KEY_ALL_PLAYLISTS = "custom_playlists_data"
    private const val DELIMITER_PLAYLIST = ";;;"
  }
}

`

---

### common/audio/src/main/java/com/quran/labs/androidquran/common/audio/memorization/MemorizationPreferences.kt

`kotlin
package com.quran.labs.androidquran.common.audio.memorization

import android.content.Context
import android.content.SharedPreferences
import com.quran.data.di.AppScope
import com.quran.data.model.SuraAyah
import com.quran.mobile.di.qualifier.ApplicationContext
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * Handles persistent storage of memorization session progress, configuration,
 * custom playlist sessions, and mastered ranges history.
 */
@SingleIn(AppScope::class)
class MemorizationPreferences(
  private val prefs: SharedPreferences
) {

  @Inject
  constructor(@ApplicationContext context: Context) : this(
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
  )

  /**
   * Saves active memorization configuration and progress step.
   */
  fun saveSession(config: MemorizationConfig, currentStepIndex: Int, currentRepeatIteration: Int) {
    val editor = prefs.edit()
      .putInt(KEY_START_SURA, config.startAyah.sura)
      .putInt(KEY_START_AYAH, config.startAyah.ayah)
      .putInt(KEY_END_SURA, config.endAyah.sura)
      .putInt(KEY_END_AYAH, config.endAyah.ayah)
      .putInt(KEY_WINDOW_SIZE, config.windowSize)
      .putInt(KEY_SINGLE_REPEAT, config.singleRepeat)
      .putInt(KEY_CONNECT_REPEAT, config.connectRepeat)
      .putBoolean(KEY_MEMORY_TEST, config.memoryTestMode)
      .putString(KEY_MASKING_LEVEL, config.maskingLevel.name)
      .putBoolean(KEY_RECORD_RECITATION, config.recordRecitation)
      .putBoolean(KEY_ENABLE_BRIDGE, config.enableBridge)
      .putInt(KEY_SAVED_STEP_INDEX, currentStepIndex)
      .putInt(KEY_SAVED_REPEAT, currentRepeatIteration)
      .putBoolean(KEY_HAS_ACTIVE_SESSION, true)

    if (config.segments.isNotEmpty()) {
      editor.putString(KEY_SEGMENTS, config.segments.joinToString(",") { it.serialize() })
      editor.putString(KEY_PLAYLIST_ID, config.playlistId)
      editor.putString(KEY_PLAYLIST_NAME, config.playlistName)
    } else {
      editor.remove(KEY_SEGMENTS)
      editor.remove(KEY_PLAYLIST_ID)
      editor.remove(KEY_PLAYLIST_NAME)
    }

    when (val mode = config.delayMode) {
      is MemorizationDelayMode.None -> {
        editor.putString(KEY_DELAY_MODE, VALUE_DELAY_NONE)
      }
      is MemorizationDelayMode.FixedSeconds -> {
        editor.putString(KEY_DELAY_MODE, VALUE_DELAY_FIXED)
        editor.putInt(KEY_DELAY_SECONDS, mode.seconds)
      }
      is MemorizationDelayMode.MatchAyahDuration -> {
        editor.putString(KEY_DELAY_MODE, VALUE_DELAY_MATCH)
      }
    }

    editor.apply()
  }

  /**
   * Clears saved session on completion or manual termination.
   */
  fun clearSession() {
    prefs.edit().putBoolean(KEY_HAS_ACTIVE_SESSION, false).apply()
  }

  /**
   * Returns true if there is an unfinished memorization session saved.
   */
  fun hasSavedSession(): Boolean = prefs.getBoolean(KEY_HAS_ACTIVE_SESSION, false)

  /**
   * Retrieves the saved session's [MemorizationConfig], or null if none exists.
   */
  fun getSavedConfig(): MemorizationConfig? {
    if (!hasSavedSession()) return null
    val startSura = prefs.getInt(KEY_START_SURA, 1)
    val startAyah = prefs.getInt(KEY_START_AYAH, 1)
    val endSura = prefs.getInt(KEY_END_SURA, 1)
    val endAyah = prefs.getInt(KEY_END_AYAH, 1)

    val delayModeStr = prefs.getString(KEY_DELAY_MODE, VALUE_DELAY_NONE)
    val delayMode = when (delayModeStr) {
      VALUE_DELAY_FIXED -> MemorizationDelayMode.FixedSeconds(prefs.getInt(KEY_DELAY_SECONDS, 3))
      VALUE_DELAY_MATCH -> MemorizationDelayMode.MatchAyahDuration
      else -> MemorizationDelayMode.None
    }

    val memoryTest = prefs.getBoolean(KEY_MEMORY_TEST, false)
    val maskingLevelStr = prefs.getString(KEY_MASKING_LEVEL, null)
    val maskingLevel = if (maskingLevelStr != null) {
      try {
        MaskingLevel.valueOf(maskingLevelStr)
      } catch (e: Exception) {
        if (memoryTest) MaskingLevel.MASK_ON_CONNECT else MaskingLevel.OFF
      }
    } else {
      if (memoryTest) MaskingLevel.MASK_ON_CONNECT else MaskingLevel.OFF
    }

    val recordRecitation = prefs.getBoolean(KEY_RECORD_RECITATION, false)

    val segmentsStr = prefs.getString(KEY_SEGMENTS, null)
    val segments = if (!segmentsStr.isNullOrBlank()) {
      segmentsStr.split(",").mapNotNull { PlaylistSegment.deserialize(it) }
    } else {
      emptyList()
    }
    val playlistId = prefs.getString(KEY_PLAYLIST_ID, null)
    val playlistName = prefs.getString(KEY_PLAYLIST_NAME, null)

    return MemorizationConfig(
      startAyah = SuraAyah(startSura, startAyah),
      endAyah = SuraAyah(endSura, endAyah),
      windowSize = prefs.getInt(KEY_WINDOW_SIZE, 4),
      singleRepeat = prefs.getInt(KEY_SINGLE_REPEAT, 3),
      connectRepeat = prefs.getInt(KEY_CONNECT_REPEAT, 2),
      delayMode = delayMode,
      memoryTestMode = memoryTest,
      maskingLevel = maskingLevel,
      recordRecitation = recordRecitation,
      enableBridge = prefs.getBoolean(KEY_ENABLE_BRIDGE, true),
      segments = segments,
      playlistId = playlistId,
      playlistName = playlistName
    )
  }

  fun getSavedStepIndex(): Int = prefs.getInt(KEY_SAVED_STEP_INDEX, 0)

  fun getSavedRepeatIteration(): Int = prefs.getInt(KEY_SAVED_REPEAT, 1)

  /**
   * Logs a completed memorization range into mastery history.
   * Keeps most recent ranges first, limited to [MAX_MASTERED_RANGES].
   */
  fun saveMasteredRange(range: MasteredRange) {
    val existing = getMasteredRanges().toMutableList()
    existing.removeAll { it.startAyah == range.startAyah && it.endAyah == range.endAyah }
    existing.add(0, range)
    val limited = existing.take(MAX_MASTERED_RANGES)
    val serialized = limited.joinToString(DELIMITER_ITEM) { it.serialize() }
    prefs.edit().putString(KEY_MASTERED_RANGES, serialized).apply()
  }

  /**
   * Returns list of mastered ranges in descending order of completion.
   */
  fun getMasteredRanges(): List<MasteredRange> {
    val raw = prefs.getString(KEY_MASTERED_RANGES, null) ?: return emptyList()
    if (raw.isBlank()) return emptyList()
    return raw.split(DELIMITER_ITEM).mapNotNull { MasteredRange.deserialize(it) }
  }

  /**
   * Clears the mastered ranges history.
   */
  fun clearMasteredRanges() {
    prefs.edit().remove(KEY_MASTERED_RANGES).apply()
  }

  companion object {
    private const val PREFS_NAME = "quran_intelligent_memorizer_prefs"
    private const val KEY_START_SURA = "mem_start_sura"
    private const val KEY_START_AYAH = "mem_start_ayah"
    private const val KEY_END_SURA = "mem_end_sura"
    private const val KEY_END_AYAH = "mem_end_ayah"
    private const val KEY_WINDOW_SIZE = "mem_window_size"
    private const val KEY_SINGLE_REPEAT = "mem_single_repeat"
    private const val KEY_CONNECT_REPEAT = "mem_connect_repeat"
    private const val KEY_MEMORY_TEST = "mem_test_mode"
    private const val KEY_MASKING_LEVEL = "mem_masking_level"
    private const val KEY_RECORD_RECITATION = "mem_record_recitation"
    private const val KEY_DELAY_MODE = "mem_delay_mode"
    private const val KEY_DELAY_SECONDS = "mem_delay_seconds"
    private const val KEY_ENABLE_BRIDGE = "mem_enable_bridge"
    private const val KEY_SAVED_STEP_INDEX = "mem_step_idx"
    private const val KEY_SAVED_REPEAT = "mem_repeat_iter"
    private const val KEY_HAS_ACTIVE_SESSION = "mem_has_session"
    private const val KEY_MASTERED_RANGES = "mem_mastered_ranges"
    private const val KEY_SEGMENTS = "mem_segments"
    private const val KEY_PLAYLIST_ID = "mem_playlist_id"
    private const val KEY_PLAYLIST_NAME = "mem_playlist_name"

    private const val VALUE_DELAY_NONE = "NONE"
    private const val VALUE_DELAY_FIXED = "FIXED"
    private const val VALUE_DELAY_MATCH = "MATCH"

    private const val DELIMITER_ITEM = "|"
    private const val MAX_MASTERED_RANGES = 30
  }
}

`

---

### common/audio/src/main/java/com/quran/labs/androidquran/common/audio/memorization/MemorizationPlanBuilder.kt

`kotlin
package com.quran.labs.androidquran.common.audio.memorization

import com.quran.data.core.QuranInfo
import com.quran.data.model.SuraAyah

/**
 * Builds a deterministic, progressive sliding-window plan of steps
 * for Quran memorization according to the algorithm specification,
 * supporting both single contiguous ranges and custom cross-Surah playlists.
 */
object MemorizationPlanBuilder {

  /**
   * Generates the ordered list of Ayahs within the specified closed range.
   */
  fun getAyahsInRange(start: SuraAyah, end: SuraAyah, quranInfo: QuranInfo): List<SuraAyah> {
    require(start <= end) { "startAyah ($start) must be <= endAyah ($end)" }
    val ayahs = mutableListOf<SuraAyah>()
    var current: SuraAyah? = start
    while (current != null && current <= end) {
      ayahs.add(current)
      if (current == end) break
      current = current.next(quranInfo)
    }
    return ayahs
  }

  /**
   * Constructs the full sequence of [MemorizationStep]s for the provided [config].
   * If [config.segments] is populated, flattens all segments across Surahs.
   */
  fun buildPlan(config: MemorizationConfig, quranInfo: QuranInfo): List<MemorizationStep> {
    val allAyahs = if (config.segments.isNotEmpty()) {
      config.segments.flatMap { it.toAyahs() }
    } else {
      getAyahsInRange(config.startAyah, config.endAyah, quranInfo)
    }
    if (allAyahs.isEmpty()) return emptyList()

    val windows = allAyahs.chunked(config.windowSize)
    val totalWindows = windows.size
    val steps = mutableListOf<MemorizationStep>()
    var stepIndex = 0

    for (wIdx in windows.indices) {
      val windowAyahs = windows[wIdx]

      for (j in windowAyahs.indices) {
        val currentAyah = windowAyahs[j]
        val ayahLabel = "Surah ${currentAyah.sura}:${currentAyah.ayah}"

        // 1. Solo Step for current Ayah
        steps.add(
          MemorizationStep(
            stepIndex = stepIndex++,
            windowIndex = wIdx,
            totalWindows = totalWindows,
            stepType = StepType.SOLO,
            ayahs = listOf(currentAyah),
            targetRepeatCount = config.singleRepeat,
            delayMode = config.delayMode,
            isMaskedByDefault = false,
            description = "$ayahLabel Solo"
          )
        )

        // 1b. Optional Recitation Pause after Solo
        if (config.delayMode != MemorizationDelayMode.None) {
          steps.add(
            MemorizationStep(
              stepIndex = stepIndex++,
              windowIndex = wIdx,
              totalWindows = totalWindows,
              stepType = StepType.PAUSE_FOR_RECITATION,
              ayahs = listOf(currentAyah),
              targetRepeatCount = 1,
              delayMode = config.delayMode,
              isMaskedByDefault = config.memoryTestMode,
              description = "Recitation Pause ($ayahLabel)"
            )
          )
        }

        // 2. Connect Step if 2 or more Ayahs accumulated in current window
        if (j > 0 && config.connectRepeat > 0) {
          val connectedAyahs = windowAyahs.subList(0, j + 1)
          val first = connectedAyahs.first()
          val last = connectedAyahs.last()
          val connectLabel = if (first.sura == last.sura) {
            "Connecting Ayah ${first.ayah}–${last.ayah} (Surah ${first.sura})"
          } else {
            "Connecting ${first.sura}:${first.ayah} ➔ ${last.sura}:${last.ayah}"
          }

          steps.add(
            MemorizationStep(
              stepIndex = stepIndex++,
              windowIndex = wIdx,
              totalWindows = totalWindows,
              stepType = StepType.CONNECT,
              ayahs = connectedAyahs,
              targetRepeatCount = config.connectRepeat,
              delayMode = config.delayMode,
              isMaskedByDefault = config.memoryTestMode,
              description = connectLabel
            )
          )

          // 2b. Optional Recitation Pause after Connect
          if (config.delayMode != MemorizationDelayMode.None) {
            steps.add(
              MemorizationStep(
                stepIndex = stepIndex++,
                windowIndex = wIdx,
                totalWindows = totalWindows,
                stepType = StepType.PAUSE_FOR_RECITATION,
                ayahs = connectedAyahs,
                targetRepeatCount = 1,
                delayMode = config.delayMode,
                isMaskedByDefault = config.memoryTestMode,
                description = "Recitation Pause ($connectLabel)"
              )
            )
          }
        }
      }

      // 3. Sliding Window Bridge Step (between current window and next window)
      if (config.enableBridge && wIdx < totalWindows - 1) {
        val nextWindow = windows[wIdx + 1]
        val bridgeAyahs = listOf(windowAyahs.last(), nextWindow.first())
        val bFirst = bridgeAyahs[0]
        val bSecond = bridgeAyahs[1]
        val bridgeLabel = if (bFirst.sura == bSecond.sura) {
          "Bridge Ayah ${bFirst.ayah} ➔ ${bSecond.ayah}"
        } else {
          "Bridge ${bFirst.sura}:${bFirst.ayah} ➔ ${bSecond.sura}:${bSecond.ayah}"
        }

        steps.add(
          MemorizationStep(
            stepIndex = stepIndex++,
            windowIndex = wIdx,
            totalWindows = totalWindows,
            stepType = StepType.BRIDGE,
            ayahs = bridgeAyahs,
            targetRepeatCount = 1,
            delayMode = MemorizationDelayMode.None,
            isMaskedByDefault = config.memoryTestMode,
            description = bridgeLabel
          )
        )
      }
    }

    return steps
  }
}

`

---

### common/audio/src/main/java/com/quran/labs/androidquran/common/audio/memorization/MemorizationEngine.kt

`kotlin
package com.quran.labs.androidquran.common.audio.memorization

import com.quran.data.core.QuranInfo
import com.quran.data.di.AppScope
import com.quran.data.model.SuraAyah
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Core state machine for the Intelligent Quran Memorization Mode.
 *
 * Implements a progressive sliding-window learning algorithm:
 * - Repeats each new Ayah individually (Rs)
 * - Consecutively connects learned Ayahs in the window (Rc)
 * - Optionally triggers silent recitation pauses
 * - Slides window across the selected range with an optional connecting bridge step
 * - Supports Memory Test Mode (blurring/masking text with tap-to-peek)
 */
@SingleIn(AppScope::class)
class MemorizationEngine @Inject constructor(
  private val quranInfo: QuranInfo,
  private val memorizationPreferences: MemorizationPreferences? = null
) {

  private val _state = MutableStateFlow(MemorizationState())
  val state: StateFlow<MemorizationState> = _state.asStateFlow()

  private var plan: List<MemorizationStep> = emptyList()

  /**
   * Initializes and starts a new memorization session.
   */
  fun startMemorization(config: MemorizationConfig) {
    plan = MemorizationPlanBuilder.buildPlan(config, quranInfo)
    if (plan.isEmpty()) {
      _state.value = MemorizationState(isActive = false)
      return
    }

    val firstStep = plan[0]
    val firstAyah = firstStep.ayahs.firstOrNull()

    memorizationPreferences?.saveSession(config, 0, 1)

    _state.value = MemorizationState(
      isActive = true,
      isPaused = false,
      config = config,
      currentWindowIndex = firstStep.windowIndex,
      totalWindows = firstStep.totalWindows,
      currentStepIndex = 0,
      totalSteps = plan.size,
      currentStep = firstStep,
      currentRepeatIteration = 1,
      totalRepeatIterations = firstStep.targetRepeatCount,
      activeAyahsInStep = firstStep.ayahs,
      currentPlayingAyah = firstAyah,
      currentSubAyahIndex = 0,
      isTextMasked = shouldMaskText(firstStep, 1, config),
      pauseRemainingSeconds = 0,
      isRecordingRecitation = false,
      lastRecitationAudioPath = null,
      isSessionCompleted = false
    )
  }

  /**
   * Resumes the previously saved memorization session if one exists.
   */
  fun resumeSavedSession(): Boolean {
    val savedConfig = memorizationPreferences?.getSavedConfig() ?: return false
    val savedStepIdx = memorizationPreferences.getSavedStepIndex()

    startMemorization(savedConfig)
    if (savedStepIdx in plan.indices) {
      goToStep(savedStepIdx)
    }
    return true
  }

  /**
   * Pauses the active memorization session.
   */
  fun pause() {
    val current = _state.value
    if (!current.isActive) return
    _state.value = current.copy(isPaused = true)
  }

  /**
   * Resumes playback in the active memorization session.
   */
  fun resume() {
    val current = _state.value
    if (!current.isActive) return
    _state.value = current.copy(isPaused = false)
  }

  /**
   * Skips directly to the next memorization step in the plan.
   */
  fun skipCurrentStep() {
    val current = _state.value
    if (!current.isActive || plan.isEmpty()) return

    val nextStepIdx = current.currentStepIndex + 1
    if (nextStepIdx >= plan.size) {
      completeSession()
    } else {
      goToStep(nextStepIdx)
    }
  }

  /**
   * Returns to the previous memorization step in the plan.
   */
  fun previousStep() {
    val current = _state.value
    if (!current.isActive || plan.isEmpty()) return

    val prevStepIdx = (current.currentStepIndex - 1).coerceAtLeast(0)
    goToStep(prevStepIdx)
  }

  /**
   * Terminates the memorization session and returns to idle state.
   */
  fun stopMemorization() {
    memorizationPreferences?.clearSession()
    plan = emptyList()
    _state.value = MemorizationState(isActive = false)
  }

  /**
   * Toggles text masking/blurring during memory test mode (tap-to-peek).
   */
  fun toggleTextMask() {
    val current = _state.value
    if (!current.isActive) return
    _state.value = current.copy(isTextMasked = !current.isTextMasked)
  }

  /**
   * Sets text mask visibility explicitly.
   */
  fun setTextMasked(masked: Boolean) {
    val current = _state.value
    if (!current.isActive) return
    _state.value = current.copy(isTextMasked = masked)
  }

  /**
   * Updates the remaining countdown seconds during a recitation pause step.
   */
  fun updatePauseRemaining(seconds: Int) {
    val current = _state.value
    if (!current.isActive) return
    _state.value = current.copy(pauseRemainingSeconds = seconds)
  }

  /**
   * Sets whether user recitation is actively being recorded.
   */
  fun setRecordingRecitation(isRecording: Boolean) {
    val current = _state.value
    if (!current.isActive) return
    _state.value = current.copy(isRecordingRecitation = isRecording)
  }

  /**
   * Updates the file path of the last recorded recitation.
   */
  fun updateLastRecitationPath(path: String?) {
    val current = _state.value
    if (!current.isActive) return
    _state.value = current.copy(lastRecitationAudioPath = path)
  }

  /**
   * Determines whether Quranic text should be visually masked based on configuration and iteration.
   */
  private fun shouldMaskText(step: MemorizationStep, repeatIteration: Int, config: MemorizationConfig?): Boolean {
    if (config == null) return false
    return when (config.maskingLevel) {
      MaskingLevel.OFF -> false
      MaskingLevel.MASK_ON_CONNECT -> step.isMaskedByDefault
      MaskingLevel.PROGRESSIVE_SOLO_AND_CONNECT -> {
        if (step.stepType == StepType.SOLO) {
          repeatIteration > 1
        } else {
          true
        }
      }
    }
  }

  /**
   * Called when audio playback of an Ayah finishes.
   * Advances sub-ayah index within a CONNECT step, or advances repeat iteration,
   * or transitions to the next step.
   *
   * @param completedAyah The Ayah that just finished playing.
   * @return The next [SuraAyah] to play, or null if the session is paused/completed.
   */
  fun onAyahPlaybackCompleted(completedAyah: SuraAyah): SuraAyah? {
    val current = _state.value
    if (!current.isActive || current.isPaused || current.currentStep == null) return null

    val step = current.currentStep
    val subIndex = current.currentSubAyahIndex

    // Check if there are remaining Ayahs in the current step sequence (e.g. CONNECT sequence)
    if (subIndex + 1 < step.ayahs.size) {
      val nextSubIndex = subIndex + 1
      val nextAyah = step.ayahs[nextSubIndex]
      _state.value = current.copy(
        currentSubAyahIndex = nextSubIndex,
        currentPlayingAyah = nextAyah
      )
      return nextAyah
    }

    // Finished a full sequence of this step. Check repetitions.
    if (current.currentRepeatIteration < current.totalRepeatIterations) {
      val nextRepeat = current.currentRepeatIteration + 1
      val firstAyah = step.ayahs.first()
      _state.value = current.copy(
        currentRepeatIteration = nextRepeat,
        currentSubAyahIndex = 0,
        currentPlayingAyah = firstAyah,
        isTextMasked = shouldMaskText(step, nextRepeat, current.config)
      )
      return firstAyah
    }

    // Step fully completed! Advance to next step in plan.
    val nextStepIndex = current.currentStepIndex + 1
    if (nextStepIndex >= plan.size) {
      completeSession()
      return null
    }

    goToStep(nextStepIndex)
    return _state.value.currentPlayingAyah
  }

  private fun goToStep(stepIndex: Int) {
    if (stepIndex !in plan.indices) return
    val nextStep = plan[stepIndex]
    val firstAyah = nextStep.ayahs.firstOrNull()
    val config = _state.value.config

    if (config != null) {
      memorizationPreferences?.saveSession(config, stepIndex, 1)
    }

    _state.value = _state.value.copy(
      currentWindowIndex = nextStep.windowIndex,
      totalWindows = nextStep.totalWindows,
      currentStepIndex = stepIndex,
      currentStep = nextStep,
      currentRepeatIteration = 1,
      totalRepeatIterations = nextStep.targetRepeatCount,
      activeAyahsInStep = nextStep.ayahs,
      currentPlayingAyah = firstAyah,
      currentSubAyahIndex = 0,
      isTextMasked = shouldMaskText(nextStep, 1, config),
      pauseRemainingSeconds = 0,
      isSessionCompleted = false
    )
  }

  private fun completeSession() {
    val current = _state.value
    current.config?.let { config ->
      memorizationPreferences?.saveMasteredRange(
        MasteredRange(
          startAyah = config.startAyah,
          endAyah = config.endAyah,
          timestamp = System.currentTimeMillis(),
          repetitions = config.singleRepeat + config.connectRepeat
        )
      )
    }
    memorizationPreferences?.clearSession()
    _state.value = _state.value.copy(
      isActive = false,
      isSessionCompleted = true,
      currentStep = null,
      currentPlayingAyah = null,
      activeAyahsInStep = emptyList(),
      isTextMasked = false,
      isRecordingRecitation = false,
      lastRecitationAudioPath = null
    )
  }
}

`

---

### common/audio/src/test/java/com/quran/labs/androidquran/common/audio/memorization/MemorizationPlanBuilderTest.kt

`kotlin
package com.quran.labs.androidquran.common.audio.memorization

import com.google.common.truth.Truth.assertThat
import com.quran.data.core.QuranInfo
import com.quran.data.model.SuraAyah
import com.quran.labs.androidquran.pages.data.madani.MadaniDataSource
import org.junit.Before
import org.junit.Test

class MemorizationPlanBuilderTest {

  private lateinit var quranInfo: QuranInfo

  @Before
  fun setup() {
    quranInfo = QuranInfo(MadaniDataSource())
  }

  @Test
  fun `getAyahsInRange returns exact sequence within single sura`() {
    val start = SuraAyah(1, 1)
    val end = SuraAyah(1, 7)

    val ayahs = MemorizationPlanBuilder.getAyahsInRange(start, end, quranInfo)

    assertThat(ayahs).hasSize(7)
    assertThat(ayahs.first()).isEqualTo(SuraAyah(1, 1))
    assertThat(ayahs.last()).isEqualTo(SuraAyah(1, 7))
  }

  @Test
  fun `getAyahsInRange crosses sura boundary correctly`() {
    val start = SuraAyah(113, 4)
    val end = SuraAyah(114, 2)

    val ayahs = MemorizationPlanBuilder.getAyahsInRange(start, end, quranInfo)

    // Sura 113 has 5 ayahs (4, 5) -> 2 ayahs
    // Sura 114 has 6 ayahs (1, 2) -> 2 ayahs
    // Total 4 ayahs
    assertThat(ayahs).containsExactly(
      SuraAyah(113, 4),
      SuraAyah(113, 5),
      SuraAyah(114, 1),
      SuraAyah(114, 2)
    ).inOrder()
  }

  @Test
  fun `buildPlan produces correct sequence for window size 4 without delay`() {
    val config = MemorizationConfig(
      startAyah = SuraAyah(1, 1),
      endAyah = SuraAyah(1, 4),
      windowSize = 4,
      singleRepeat = 3,
      connectRepeat = 2,
      delayMode = MemorizationDelayMode.None,
      enableBridge = false
    )

    val plan = MemorizationPlanBuilder.buildPlan(config, quranInfo)

    // Window [A1, A2, A3, A4]:
    // 1. A1 Solo (Rs = 3)
    // 2. A2 Solo (Rs = 3)
    // 3. Connect A1..A2 (Rc = 2)
    // 4. A3 Solo (Rs = 3)
    // 5. Connect A1..A3 (Rc = 2)
    // 6. A4 Solo (Rs = 3)
    // 7. Connect A1..A4 (Rc = 2)
    assertThat(plan).hasSize(7)

    // Step 0: A1 Solo
    assertThat(plan[0].stepType).isEqualTo(StepType.SOLO)
    assertThat(plan[0].ayahs).containsExactly(SuraAyah(1, 1))
    assertThat(plan[0].targetRepeatCount).isEqualTo(3)

    // Step 1: A2 Solo
    assertThat(plan[1].stepType).isEqualTo(StepType.SOLO)
    assertThat(plan[1].ayahs).containsExactly(SuraAyah(1, 2))
    assertThat(plan[1].targetRepeatCount).isEqualTo(3)

    // Step 2: Connect A1..A2
    assertThat(plan[2].stepType).isEqualTo(StepType.CONNECT)
    assertThat(plan[2].ayahs).containsExactly(SuraAyah(1, 1), SuraAyah(1, 2)).inOrder()
    assertThat(plan[2].targetRepeatCount).isEqualTo(2)

    // Step 3: A3 Solo
    assertThat(plan[3].stepType).isEqualTo(StepType.SOLO)
    assertThat(plan[3].ayahs).containsExactly(SuraAyah(1, 3))
    assertThat(plan[3].targetRepeatCount).isEqualTo(3)

    // Step 4: Connect A1..A3
    assertThat(plan[4].stepType).isEqualTo(StepType.CONNECT)
    assertThat(plan[4].ayahs).containsExactly(SuraAyah(1, 1), SuraAyah(1, 2), SuraAyah(1, 3)).inOrder()
    assertThat(plan[4].targetRepeatCount).isEqualTo(2)

    // Step 5: A4 Solo
    assertThat(plan[5].stepType).isEqualTo(StepType.SOLO)
    assertThat(plan[5].ayahs).containsExactly(SuraAyah(1, 4))
    assertThat(plan[5].targetRepeatCount).isEqualTo(3)

    // Step 6: Connect A1..A4
    assertThat(plan[6].stepType).isEqualTo(StepType.CONNECT)
    assertThat(plan[6].ayahs).containsExactly(
      SuraAyah(1, 1), SuraAyah(1, 2), SuraAyah(1, 3), SuraAyah(1, 4)
    ).inOrder()
    assertThat(plan[6].targetRepeatCount).isEqualTo(2)
  }

  @Test
  fun `buildPlan includes recitation pauses when delayMode is active`() {
    val config = MemorizationConfig(
      startAyah = SuraAyah(1, 1),
      endAyah = SuraAyah(1, 2),
      windowSize = 2,
      singleRepeat = 2,
      connectRepeat = 2,
      delayMode = MemorizationDelayMode.FixedSeconds(5),
      memoryTestMode = true,
      enableBridge = false
    )

    val plan = MemorizationPlanBuilder.buildPlan(config, quranInfo)

    // Expected for 2 ayahs with delay:
    // 0: A1 Solo
    // 1: Recitation Pause (A1)
    // 2: A2 Solo
    // 3: Recitation Pause (A2)
    // 4: Connect A1..A2
    // 5: Recitation Pause (A1..A2)
    assertThat(plan).hasSize(6)
    assertThat(plan[0].stepType).isEqualTo(StepType.SOLO)
    assertThat(plan[1].stepType).isEqualTo(StepType.PAUSE_FOR_RECITATION)
    assertThat(plan[1].isMaskedByDefault).isTrue()
    assertThat(plan[2].stepType).isEqualTo(StepType.SOLO)
    assertThat(plan[3].stepType).isEqualTo(StepType.PAUSE_FOR_RECITATION)
    assertThat(plan[4].stepType).isEqualTo(StepType.CONNECT)
    assertThat(plan[4].isMaskedByDefault).isTrue()
    assertThat(plan[5].stepType).isEqualTo(StepType.PAUSE_FOR_RECITATION)
  }

  @Test
  fun `buildPlan adds Bridge step between multiple windows`() {
    val config = MemorizationConfig(
      startAyah = SuraAyah(1, 1),
      endAyah = SuraAyah(1, 4),
      windowSize = 2, // 2 windows: [1, 2] and [3, 4]
      singleRepeat = 1,
      connectRepeat = 1,
      delayMode = MemorizationDelayMode.None,
      enableBridge = true
    )

    val plan = MemorizationPlanBuilder.buildPlan(config, quranInfo)

    // Window 0 has [A1, A2] -> Solo A1, Solo A2, Connect A1..A2
    // Bridge step between Window 0 and Window 1: connects A2 and A3
    // Window 1 has [A3, A4] -> Solo A3, Solo A4, Connect A3..A4
    val bridgeSteps = plan.filter { it.stepType == StepType.BRIDGE }
    assertThat(bridgeSteps).hasSize(1)
    assertThat(bridgeSteps.first().ayahs).containsExactly(SuraAyah(1, 2), SuraAyah(1, 3)).inOrder()
    assertThat(bridgeSteps.first().targetRepeatCount).isEqualTo(1)
  }

  @Test
  fun `buildPlan with custom cross-surah segments flattens across multiple surahs`() {
    // User's exact example:
    // 1: Al-Hamd (1:1-7)
    // 2: Al-Baqarah (2:1-7)
    // 3: Al-Ikhlas (112:1-4)
    // 4: Al-Kafirun (109:1-6)
    // 5: Al-Falaq (113:1-5)
    // 6: An-Nas (114:1-6)
    // 7: Al-Baqarah (2:270-286)
    val segments = listOf(
      PlaylistSegment.createFullSurah(1, quranInfo, "Al-Hamd"),
      PlaylistSegment(sura = 2, startAyah = 1, endAyah = 7, isFullSurah = false, customLabel = "Al-Baqarah 1-7"),
      PlaylistSegment.createFullSurah(112, quranInfo, "Al-Ikhlas"),
      PlaylistSegment.createFullSurah(109, quranInfo, "Al-Kafirun"),
      PlaylistSegment.createFullSurah(113, quranInfo, "Al-Falaq"),
      PlaylistSegment.createFullSurah(114, quranInfo, "An-Nas"),
      PlaylistSegment(sura = 2, startAyah = 270, endAyah = 286, isFullSurah = false, customLabel = "Al-Baqarah 270-286")
    )

    val playlist = CustomPlaylist(
      id = "daily-adhkar",
      name = "Daily Recitation",
      segments = segments
    )

    // Expected total ayahs: 7 + 7 + 4 + 6 + 5 + 6 + 17 = 52
    assertThat(playlist.totalAyahsCount).isEqualTo(52)
    val flattened = playlist.toFlattenedAyahs()
    assertThat(flattened).hasSize(52)
    assertThat(flattened.first()).isEqualTo(SuraAyah(1, 1))
    assertThat(flattened[7]).isEqualTo(SuraAyah(2, 1)) // Transition from Hamd to Baqarah
    assertThat(flattened.last()).isEqualTo(SuraAyah(2, 286))

    val config = MemorizationConfig(
      windowSize = 4,
      singleRepeat = 2,
      connectRepeat = 1,
      delayMode = MemorizationDelayMode.None,
      enableBridge = true,
      segments = segments,
      playlistId = playlist.id,
      playlistName = playlist.name
    )

    val plan = MemorizationPlanBuilder.buildPlan(config, quranInfo)
    assertThat(plan).isNotEmpty()

    // 52 ayahs with window size 4 -> 13 windows
    val soloSteps = plan.filter { it.stepType == StepType.SOLO }
    assertThat(soloSteps).hasSize(52)

    // First solo step is 1:1, 8th solo step is 2:1
    assertThat(soloSteps[0].ayahs.first()).isEqualTo(SuraAyah(1, 1))
    assertThat(soloSteps[7].ayahs.first()).isEqualTo(SuraAyah(2, 1))

    // Bridge steps connect consecutive windows (13 windows -> 12 bridges)
    val bridgeSteps = plan.filter { it.stepType == StepType.BRIDGE }
    assertThat(bridgeSteps).hasSize(12)

    // Window 0 is ayahs [1:1, 1:2, 1:3, 1:4]
    // Window 1 is ayahs [1:5, 1:6, 1:7, 2:1]
    // Window 1 contains cross-surah transition between 1:7 and 2:1!
    // Connect step in Window 1 connects 1:5, 1:6, 1:7, 2:1 across surah boundary!
    val window1Connects = plan.filter { it.windowIndex == 1 && it.stepType == StepType.CONNECT }
    assertThat(window1Connects).isNotEmpty()
    val fullWindow1Connect = window1Connects.last()
    assertThat(fullWindow1Connect.ayahs).containsExactly(
      SuraAyah(1, 5),
      SuraAyah(1, 6),
      SuraAyah(1, 7),
      SuraAyah(2, 1)
    ).inOrder()
  }
}

`

---

### common/audio/src/test/java/com/quran/labs/androidquran/common/audio/memorization/MemorizationEngineTest.kt

`kotlin
package com.quran.labs.androidquran.common.audio.memorization

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.quran.data.core.QuranInfo
import com.quran.data.model.SuraAyah
import com.quran.labs.androidquran.pages.data.madani.MadaniDataSource
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

class MemorizationEngineTest {

  private lateinit var quranInfo: QuranInfo
  private lateinit var engine: MemorizationEngine

  @Before
  fun setup() {
    quranInfo = QuranInfo(MadaniDataSource())
    engine = MemorizationEngine(quranInfo)
  }

  @Test
  fun `initial state is inactive`() {
    val state = engine.state.value
    assertThat(state.isActive).isFalse()
    assertThat(state.currentStep).isNull()
    assertThat(state.isSessionCompleted).isFalse()
  }

  @Test
  fun `startMemorization transitions to active state at first step`() = runTest {
    val config = MemorizationConfig(
      startAyah = SuraAyah(1, 1),
      endAyah = SuraAyah(1, 2),
      windowSize = 2,
      singleRepeat = 2,
      connectRepeat = 2,
      delayMode = MemorizationDelayMode.None,
      enableBridge = false
    )

    engine.state.test {
      val initial = awaitItem()
      assertThat(initial.isActive).isFalse()

      engine.startMemorization(config)

      val activeState = awaitItem()
      assertThat(activeState.isActive).isTrue()
      assertThat(activeState.currentWindowIndex).isEqualTo(0)
      assertThat(activeState.currentStepIndex).isEqualTo(0)
      assertThat(activeState.stepType).isEqualTo(StepType.SOLO)
      assertThat(activeState.currentPlayingAyah).isEqualTo(SuraAyah(1, 1))
      assertThat(activeState.currentRepeatIteration).isEqualTo(1)
      assertThat(activeState.totalRepeatIterations).isEqualTo(2)

      cancelAndIgnoreRemainingEvents()
    }
  }

  @Test
  fun `onAyahPlaybackCompleted iterates repeats before advancing step`() {
    val config = MemorizationConfig(
      startAyah = SuraAyah(1, 1),
      endAyah = SuraAyah(1, 2),
      windowSize = 2,
      singleRepeat = 2,
      connectRepeat = 1,
      delayMode = MemorizationDelayMode.None,
      enableBridge = false
    )

    engine.startMemorization(config)

    // Step 0 is A1 Solo (repeat 2 times). Current iteration is 1.
    assertThat(engine.state.value.currentStepIndex).isEqualTo(0)
    assertThat(engine.state.value.currentRepeatIteration).isEqualTo(1)

    // Complete play 1 of Step 0 -> should repeat A1 (iteration 2)
    val nextAfterPlay1 = engine.onAyahPlaybackCompleted(SuraAyah(1, 1))
    assertThat(nextAfterPlay1).isEqualTo(SuraAyah(1, 1))
    assertThat(engine.state.value.currentStepIndex).isEqualTo(0)
    assertThat(engine.state.value.currentRepeatIteration).isEqualTo(2)

    // Complete play 2 of Step 0 -> Step 0 finished! Advance to Step 1 (A2 Solo)
    val nextAfterPlay2 = engine.onAyahPlaybackCompleted(SuraAyah(1, 1))
    assertThat(nextAfterPlay2).isEqualTo(SuraAyah(1, 2))
    assertThat(engine.state.value.currentStepIndex).isEqualTo(1)
    assertThat(engine.state.value.currentRepeatIteration).isEqualTo(1)
    assertThat(engine.state.value.stepType).isEqualTo(StepType.SOLO)
  }

  @Test
  fun `CONNECT step sequences through sub-ayahs then repeats`() {
    val config = MemorizationConfig(
      startAyah = SuraAyah(1, 1),
      endAyah = SuraAyah(1, 2),
      windowSize = 2,
      singleRepeat = 1,
      connectRepeat = 2,
      delayMode = MemorizationDelayMode.None,
      enableBridge = false
    )

    engine.startMemorization(config)

    // Complete Step 0 (A1 Solo x 1)
    engine.onAyahPlaybackCompleted(SuraAyah(1, 1))
    // Complete Step 1 (A2 Solo x 1)
    engine.onAyahPlaybackCompleted(SuraAyah(1, 2))

    // Now at Step 2 (Connect A1..A2, repeat 2 times)
    val state = engine.state.value
    assertThat(state.currentStepIndex).isEqualTo(2)
    assertThat(state.stepType).isEqualTo(StepType.CONNECT)
    assertThat(state.activeAyahsInStep).containsExactly(SuraAyah(1, 1), SuraAyah(1, 2)).inOrder()
    assertThat(state.currentPlayingAyah).isEqualTo(SuraAyah(1, 1))
    assertThat(state.currentRepeatIteration).isEqualTo(1)

    // Ayah 1 of Connect finishes -> should advance to Ayah 2 (same repeat iteration 1)
    val nextSubAyah = engine.onAyahPlaybackCompleted(SuraAyah(1, 1))
    assertThat(nextSubAyah).isEqualTo(SuraAyah(1, 2))
    assertThat(engine.state.value.currentSubAyahIndex).isEqualTo(1)
    assertThat(engine.state.value.currentRepeatIteration).isEqualTo(1)

    // Ayah 2 of Connect finishes -> Repeat 1 complete! Start Repeat 2 at Ayah 1
    val nextRepeatAyah = engine.onAyahPlaybackCompleted(SuraAyah(1, 2))
    assertThat(nextRepeatAyah).isEqualTo(SuraAyah(1, 1))
    assertThat(engine.state.value.currentSubAyahIndex).isEqualTo(0)
    assertThat(engine.state.value.currentRepeatIteration).isEqualTo(2)
  }

  @Test
  fun `skipCurrentStep and previousStep navigate plan cleanly`() {
    val config = MemorizationConfig(
      startAyah = SuraAyah(1, 1),
      endAyah = SuraAyah(1, 3),
      windowSize = 3,
      singleRepeat = 3,
      connectRepeat = 2,
      delayMode = MemorizationDelayMode.None,
      enableBridge = false
    )

    engine.startMemorization(config)
    assertThat(engine.state.value.currentStepIndex).isEqualTo(0)

    // Skip to Step 1
    engine.skipCurrentStep()
    assertThat(engine.state.value.currentStepIndex).isEqualTo(1)
    assertThat(engine.state.value.currentPlayingAyah).isEqualTo(SuraAyah(1, 2))

    // Skip to Step 2
    engine.skipCurrentStep()
    assertThat(engine.state.value.currentStepIndex).isEqualTo(2)

    // Previous step back to Step 1
    engine.previousStep()
    assertThat(engine.state.value.currentStepIndex).isEqualTo(1)
    assertThat(engine.state.value.currentPlayingAyah).isEqualTo(SuraAyah(1, 2))
  }

  @Test
  fun `pause and resume update state accordingly`() {
    val config = MemorizationConfig(
      startAyah = SuraAyah(1, 1),
      endAyah = SuraAyah(1, 2),
      windowSize = 2,
      singleRepeat = 1,
      connectRepeat = 1
    )

    engine.startMemorization(config)
    assertThat(engine.state.value.isPaused).isFalse()

    engine.pause()
    assertThat(engine.state.value.isPaused).isTrue()

    // When paused, onAyahPlaybackCompleted returns null and does not advance
    val resultWhilePaused = engine.onAyahPlaybackCompleted(SuraAyah(1, 1))
    assertThat(resultWhilePaused).isNull()

    engine.resume()
    assertThat(engine.state.value.isPaused).isFalse()
  }

  @Test
  fun `memoryTestMode enables text masking during connect steps with tap-to-peek`() {
    val config = MemorizationConfig(
      startAyah = SuraAyah(1, 1),
      endAyah = SuraAyah(1, 2),
      windowSize = 2,
      singleRepeat = 1,
      connectRepeat = 1,
      memoryTestMode = true
    )

    engine.startMemorization(config)
    // Step 0: Solo -> text not masked
    assertThat(engine.state.value.isTextMasked).isFalse()

    // Complete Step 0 -> Step 1 (Solo A2)
    engine.onAyahPlaybackCompleted(SuraAyah(1, 1))
    assertThat(engine.state.value.isTextMasked).isFalse()

    // Complete Step 1 -> Step 2 (Connect A1..A2)
    engine.onAyahPlaybackCompleted(SuraAyah(1, 2))
    assertThat(engine.state.value.stepType).isEqualTo(StepType.CONNECT)
    // In Memory Test Mode, Connect step has isTextMasked = true
    assertThat(engine.state.value.isTextMasked).isTrue()

    // Tap to peek (toggle mask)
    engine.toggleTextMask()
    assertThat(engine.state.value.isTextMasked).isFalse()

    // Tap to re-mask
    engine.toggleTextMask()
    assertThat(engine.state.value.isTextMasked).isTrue()
  }

  @Test
  fun `completing all steps marks session completed`() {
    val config = MemorizationConfig(
      startAyah = SuraAyah(1, 1),
      endAyah = SuraAyah(1, 2),
      windowSize = 2,
      singleRepeat = 1,
      connectRepeat = 1,
      enableBridge = false
    )

    engine.startMemorization(config)

    // Complete Step 0 (Solo A1)
    engine.onAyahPlaybackCompleted(SuraAyah(1, 1))
    // Complete Step 1 (Solo A2)
    engine.onAyahPlaybackCompleted(SuraAyah(1, 2))
    // Step 2 (Connect A1..A2) -> Play A1
    engine.onAyahPlaybackCompleted(SuraAyah(1, 1))
    // Step 2 -> Play A2 -> FINAL STEP COMPLETE!
    val finalResult = engine.onAyahPlaybackCompleted(SuraAyah(1, 2))

    assertThat(finalResult).isNull()
    assertThat(engine.state.value.isSessionCompleted).isTrue()
    assertThat(engine.state.value.isActive).isFalse()
  }

  @Test
  fun `stopMemorization resets engine cleanly`() {
    val config = MemorizationConfig(
      startAyah = SuraAyah(1, 1),
      endAyah = SuraAyah(1, 5)
    )

    engine.startMemorization(config)
    assertThat(engine.state.value.isActive).isTrue()

    engine.stopMemorization()
    assertThat(engine.state.value.isActive).isFalse()
    assertThat(engine.state.value.currentStep).isNull()
  }

  @Test
  fun `progressive masking keeps text visible on solo repeat 1 and masks on repeat 2 and connect`() {
    val config = MemorizationConfig(
      startAyah = SuraAyah(1, 1),
      endAyah = SuraAyah(1, 2),
      windowSize = 2,
      singleRepeat = 2,
      connectRepeat = 1,
      maskingLevel = MaskingLevel.PROGRESSIVE_SOLO_AND_CONNECT,
      delayMode = MemorizationDelayMode.None,
      enableBridge = false
    )

    engine.startMemorization(config)

    // Step 0 (Solo A1), Repeat 1: visible
    assertThat(engine.state.value.stepType).isEqualTo(StepType.SOLO)
    assertThat(engine.state.value.currentRepeatIteration).isEqualTo(1)
    assertThat(engine.state.value.isTextMasked).isFalse()

    // Repeat 1 completes -> advances to Repeat 2: masked!
    engine.onAyahPlaybackCompleted(SuraAyah(1, 1))
    assertThat(engine.state.value.currentRepeatIteration).isEqualTo(2)
    assertThat(engine.state.value.isTextMasked).isTrue()

    // Solo A1 completes -> Step 1 (Solo A2), Repeat 1: visible
    engine.onAyahPlaybackCompleted(SuraAyah(1, 1))
    assertThat(engine.state.value.currentStepIndex).isEqualTo(1)
    assertThat(engine.state.value.currentRepeatIteration).isEqualTo(1)
    assertThat(engine.state.value.isTextMasked).isFalse()

    // Repeat 1 completes -> Repeat 2: masked!
    engine.onAyahPlaybackCompleted(SuraAyah(1, 2))
    assertThat(engine.state.value.currentRepeatIteration).isEqualTo(2)
    assertThat(engine.state.value.isTextMasked).isTrue()

    // Solo A2 completes -> Step 2 (Connect A1..A2): always masked!
    engine.onAyahPlaybackCompleted(SuraAyah(1, 2))
    assertThat(engine.state.value.stepType).isEqualTo(StepType.CONNECT)
    assertThat(engine.state.value.isTextMasked).isTrue()
  }

  @Test
  fun `recitation recording state and pause remaining update correctly`() {
    val config = MemorizationConfig(
      startAyah = SuraAyah(1, 1),
      endAyah = SuraAyah(1, 1),
      singleRepeat = 1,
      delayMode = MemorizationDelayMode.FixedSeconds(5)
    )

    engine.startMemorization(config)
    assertThat(engine.state.value.isRecordingRecitation).isFalse()
    assertThat(engine.state.value.lastRecitationAudioPath).isNull()

    // Solo A1 completes -> advances to PAUSE_FOR_RECITATION step
    engine.onAyahPlaybackCompleted(SuraAyah(1, 1))
    assertThat(engine.state.value.stepType).isEqualTo(StepType.PAUSE_FOR_RECITATION)

    engine.setRecordingRecitation(true)
    assertThat(engine.state.value.isRecordingRecitation).isTrue()

    engine.updatePauseRemaining(5)
    assertThat(engine.state.value.pauseRemainingSeconds).isEqualTo(5)
    assertThat(engine.state.value.hudSummary).contains("5s")
    assertThat(engine.state.value.hudSummary).contains("Recording Recitation")

    engine.updateLastRecitationPath("/cache/test.m4a")
    assertThat(engine.state.value.lastRecitationAudioPath).isEqualTo("/cache/test.m4a")
  }

  @Test
  fun `session completion logs mastered range to preferences`() {
    val prefs = MemorizationPreferences(FakeTestSharedPreferences())
    val engineWithPrefs = MemorizationEngine(quranInfo, prefs)

    val config = MemorizationConfig(
      startAyah = SuraAyah(1, 1),
      endAyah = SuraAyah(1, 1),
      windowSize = 1,
      singleRepeat = 1,
      connectRepeat = 1,
      delayMode = MemorizationDelayMode.None,
      enableBridge = false
    )

    engineWithPrefs.startMemorization(config)
    assertThat(prefs.getMasteredRanges()).isEmpty()

    // Solo A1 finishes -> session completes!
    val next = engineWithPrefs.onAyahPlaybackCompleted(SuraAyah(1, 1))
    assertThat(next).isNull()
    assertThat(engineWithPrefs.state.value.isSessionCompleted).isTrue()

    val mastered = prefs.getMasteredRanges()
    assertThat(mastered).hasSize(1)
    assertThat(mastered[0].startAyah).isEqualTo(SuraAyah(1, 1))
    assertThat(mastered[0].endAyah).isEqualTo(SuraAyah(1, 1))
    assertThat(mastered[0].repetitions).isEqualTo(2)
  }

  private class FakeTestSharedPreferences : android.content.SharedPreferences {
    private val values = mutableMapOf<String, Any>()
    override fun getAll(): MutableMap<String, *> = values
    override fun getString(key: String?, defValue: String?): String? = values[key] as? String ?: defValue
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
      @Suppress("UNCHECKED_CAST") (values[key] as? MutableSet<String> ?: defValues)
    override fun getInt(key: String?, defValue: Int): Int = values[key] as? Int ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = values[key] as? Long ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = values[key] as? Float ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = values[key] as? Boolean ?: defValue
    override fun contains(key: String?): Boolean = values.containsKey(key)
    override fun edit(): android.content.SharedPreferences.Editor = object : android.content.SharedPreferences.Editor {
      private val pending = mutableMapOf<String, Any>()
      private val removed = mutableSetOf<String>()
      private var clear = false
      override fun putString(k: String?, v: String?) = apply { if (k != null && v != null) { pending[k] = v; removed.remove(k) } }
      override fun putStringSet(k: String?, v: MutableSet<String>?) = apply { if (k != null && v != null) { pending[k] = v; removed.remove(k) } }
      override fun putInt(k: String?, v: Int) = apply { if (k != null) { pending[k] = v; removed.remove(k) } }
      override fun putLong(k: String?, v: Long) = apply { if (k != null) { pending[k] = v; removed.remove(k) } }
      override fun putFloat(k: String?, v: Float) = apply { if (k != null) { pending[k] = v; removed.remove(k) } }
      override fun putBoolean(k: String?, v: Boolean) = apply { if (k != null) { pending[k] = v; removed.remove(k) } }
      override fun remove(k: String?) = apply { if (k != null) { pending.remove(k); removed.add(k) } }
      override fun clear() = apply { clear = true }
      override fun commit(): Boolean { apply(); return true }
      override fun apply() { if (clear) values.clear(); for (k in removed) values.remove(k); values.putAll(pending) }
    }
    override fun registerOnSharedPreferenceChangeListener(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: android.content.SharedPreferences.OnSharedPreferenceChangeListener?) {}
  }
}

`

---

### common/audio/src/test/java/com/quran/labs/androidquran/common/audio/memorization/MemorizationPreferencesTest.kt

`kotlin
package com.quran.labs.androidquran.common.audio.memorization

import android.content.SharedPreferences
import com.google.common.truth.Truth.assertThat
import com.quran.data.model.SuraAyah
import org.junit.Before
import org.junit.Test

class MemorizationPreferencesTest {

  private lateinit var fakePrefs: FakeSharedPreferences
  private lateinit var memorizationPreferences: MemorizationPreferences

  @Before
  fun setUp() {
    fakePrefs = FakeSharedPreferences()
    memorizationPreferences = MemorizationPreferences(fakePrefs)
  }

  @Test
  fun `initial state has no active session and empty history`() {
    assertThat(memorizationPreferences.hasSavedSession()).isFalse()
    assertThat(memorizationPreferences.getSavedConfig()).isNull()
    assertThat(memorizationPreferences.getMasteredRanges()).isEmpty()
  }

  @Test
  fun `saveSession persists configuration and progress`() {
    val config = MemorizationConfig(
      startAyah = SuraAyah(2, 255),
      endAyah = SuraAyah(2, 257),
      windowSize = 3,
      singleRepeat = 5,
      connectRepeat = 4,
      delayMode = MemorizationDelayMode.FixedSeconds(3),
      memoryTestMode = true,
      maskingLevel = MaskingLevel.PROGRESSIVE_SOLO_AND_CONNECT,
      recordRecitation = true,
      enableBridge = false
    )

    memorizationPreferences.saveSession(config, currentStepIndex = 4, currentRepeatIteration = 2)

    assertThat(memorizationPreferences.hasSavedSession()).isTrue()
    val savedConfig = memorizationPreferences.getSavedConfig()
    assertThat(savedConfig).isNotNull()
    assertThat(savedConfig?.startAyah).isEqualTo(SuraAyah(2, 255))
    assertThat(savedConfig?.endAyah).isEqualTo(SuraAyah(2, 257))
    assertThat(savedConfig?.windowSize).isEqualTo(3)
    assertThat(savedConfig?.singleRepeat).isEqualTo(5)
    assertThat(savedConfig?.connectRepeat).isEqualTo(4)
    assertThat(savedConfig?.delayMode).isEqualTo(MemorizationDelayMode.FixedSeconds(3))
    assertThat(savedConfig?.memoryTestMode).isTrue()
    assertThat(savedConfig?.maskingLevel).isEqualTo(MaskingLevel.PROGRESSIVE_SOLO_AND_CONNECT)
    assertThat(savedConfig?.recordRecitation).isTrue()
    assertThat(savedConfig?.enableBridge).isFalse()

    assertThat(memorizationPreferences.getSavedStepIndex()).isEqualTo(4)
    assertThat(memorizationPreferences.getSavedRepeatIteration()).isEqualTo(2)
  }

  @Test
  fun `saveSession persists match duration delay mode correctly`() {
    val config = MemorizationConfig(
      startAyah = SuraAyah(1, 1),
      endAyah = SuraAyah(1, 7),
      delayMode = MemorizationDelayMode.MatchAyahDuration
    )

    memorizationPreferences.saveSession(config, 0, 1)
    val saved = memorizationPreferences.getSavedConfig()
    assertThat(saved?.delayMode).isEqualTo(MemorizationDelayMode.MatchAyahDuration)
  }

  @Test
  fun `clearSession removes active session flag`() {
    val config = MemorizationConfig(
      startAyah = SuraAyah(1, 1),
      endAyah = SuraAyah(1, 7)
    )
    memorizationPreferences.saveSession(config, 0, 1)
    assertThat(memorizationPreferences.hasSavedSession()).isTrue()

    memorizationPreferences.clearSession()

    assertThat(memorizationPreferences.hasSavedSession()).isFalse()
    assertThat(memorizationPreferences.getSavedConfig()).isNull()
  }

  @Test
  fun `mastered range serialization and deserialization works`() {
    val range = MasteredRange(
      startAyah = SuraAyah(2, 255),
      endAyah = SuraAyah(2, 257),
      timestamp = 1729000000000L,
      repetitions = 7
    )

    val serialized = range.serialize()
    assertThat(serialized).isEqualTo("2:255-2:257@1729000000000#7")

    val deserialized = MasteredRange.deserialize(serialized)
    assertThat(deserialized).isEqualTo(range)
  }

  @Test
  fun `saveMasteredRange adds to history in descending order with deduplication`() {
    val r1 = MasteredRange(SuraAyah(1, 1), SuraAyah(1, 7), 1000L, 5)
    val r2 = MasteredRange(SuraAyah(2, 1), SuraAyah(2, 5), 2000L, 6)
    val r3 = MasteredRange(SuraAyah(1, 1), SuraAyah(1, 7), 3000L, 8) // Re-memorized range 1

    memorizationPreferences.saveMasteredRange(r1)
    memorizationPreferences.saveMasteredRange(r2)

    val history = memorizationPreferences.getMasteredRanges()
    assertThat(history).hasSize(2)
    assertThat(history[0]).isEqualTo(r2)
    assertThat(history[1]).isEqualTo(r1)

    // Re-memorizing r1 brings it to the top and updates repetitions/timestamp
    memorizationPreferences.saveMasteredRange(r3)
    val updatedHistory = memorizationPreferences.getMasteredRanges()
    assertThat(updatedHistory).hasSize(2)
    assertThat(updatedHistory[0]).isEqualTo(r3)
    assertThat(updatedHistory[1]).isEqualTo(r2)
  }

  @Test
  fun `clearMasteredRanges empties history`() {
    val r1 = MasteredRange(SuraAyah(1, 1), SuraAyah(1, 7), 1000L, 5)
    memorizationPreferences.saveMasteredRange(r1)
    assertThat(memorizationPreferences.getMasteredRanges()).isNotEmpty()

    memorizationPreferences.clearMasteredRanges()
    assertThat(memorizationPreferences.getMasteredRanges()).isEmpty()
  }

  /**
   * Lightweight in-memory implementation of [SharedPreferences] for unit tests.
   */
  private class FakeSharedPreferences : SharedPreferences {
    private val values = mutableMapOf<String, Any>()

    override fun getAll(): MutableMap<String, *> = values

    override fun getString(key: String?, defValue: String?): String? =
      values[key] as? String ?: defValue

    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
      @Suppress("UNCHECKED_CAST") (values[key] as? MutableSet<String> ?: defValues)

    override fun getInt(key: String?, defValue: Int): Int =
      values[key] as? Int ?: defValue

    override fun getLong(key: String?, defValue: Long): Long =
      values[key] as? Long ?: defValue

    override fun getFloat(key: String?, defValue: Float): Float =
      values[key] as? Float ?: defValue

    override fun getBoolean(key: String?, defValue: Boolean): Boolean =
      values[key] as? Boolean ?: defValue

    override fun contains(key: String?): Boolean = values.containsKey(key)

    override fun edit(): SharedPreferences.Editor = FakeEditor(values)

    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    private class FakeEditor(private val target: MutableMap<String, Any>) : SharedPreferences.Editor {
      private val pending = mutableMapOf<String, Any>()
      private val removed = mutableSetOf<String>()
      private var clear = false

      override fun putString(key: String?, value: String?): SharedPreferences.Editor {
        if (key != null && value != null) {
          pending[key] = value
          removed.remove(key)
        }
        return this
      }

      override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
        if (key != null && values != null) {
          pending[key] = values
          removed.remove(key)
        }
        return this
      }

      override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
        if (key != null) {
          pending[key] = value
          removed.remove(key)
        }
        return this
      }

      override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
        if (key != null) {
          pending[key] = value
          removed.remove(key)
        }
        return this
      }

      override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
        if (key != null) {
          pending[key] = value
          removed.remove(key)
        }
        return this
      }

      override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
        if (key != null) {
          pending[key] = value
          removed.remove(key)
        }
        return this
      }

      override fun remove(key: String?): SharedPreferences.Editor {
        if (key != null) {
          pending.remove(key)
          removed.add(key)
        }
        return this
      }

      override fun clear(): SharedPreferences.Editor {
        clear = true
        return this
      }

      override fun commit(): Boolean {
        apply()
        return true
      }

      override fun apply() {
        if (clear) target.clear()
        for (k in removed) target.remove(k)
        target.putAll(pending)
      }
    }
  }
}

`

---

### common/audio/src/test/java/com/quran/labs/androidquran/common/audio/memorization/PlaylistRepositoryTest.kt

`kotlin
package com.quran.labs.androidquran.common.audio.memorization

import android.content.SharedPreferences
import com.google.common.truth.Truth.assertThat
import com.quran.data.core.QuranInfo
import com.quran.labs.androidquran.pages.data.madani.MadaniDataSource
import org.junit.Before
import org.junit.Test

class PlaylistRepositoryTest {

  private lateinit var fakePrefs: FakeSharedPreferences
  private lateinit var repository: PlaylistRepository
  private lateinit var quranInfo: QuranInfo

  @Before
  fun setUp() {
    fakePrefs = FakeSharedPreferences()
    repository = PlaylistRepository(fakePrefs)
    quranInfo = QuranInfo(MadaniDataSource())
  }

  @Test
  fun `initial state has no playlists`() {
    assertThat(repository.getAllPlaylists()).isEmpty()
  }

  @Test
  fun `save and retrieve single playlist`() {
    val seg1 = PlaylistSegment.createFullSurah(1, quranInfo, "Al-Hamd")
    val seg2 = PlaylistSegment(sura = 2, startAyah = 1, endAyah = 7, isFullSurah = false, customLabel = "Al-Baqarah 1-7")
    val playlist = CustomPlaylist(
      id = "test-pl-1",
      name = "Daily Recitation",
      segments = listOf(seg1, seg2),
      createdAt = 1000L,
      lastPlayedAt = 2000L
    )

    repository.savePlaylist(playlist)

    val all = repository.getAllPlaylists()
    assertThat(all).hasSize(1)
    val retrieved = all[0]
    assertThat(retrieved.id).isEqualTo("test-pl-1")
    assertThat(retrieved.name).isEqualTo("Daily Recitation")
    assertThat(retrieved.segments).hasSize(2)
    assertThat(retrieved.segments[0].sura).isEqualTo(1)
    assertThat(retrieved.segments[0].startAyah).isEqualTo(1)
    assertThat(retrieved.segments[0].endAyah).isEqualTo(7)
    assertThat(retrieved.segments[0].isFullSurah).isTrue()
    assertThat(retrieved.segments[1].sura).isEqualTo(2)
    assertThat(retrieved.segments[1].startAyah).isEqualTo(1)
    assertThat(retrieved.segments[1].endAyah).isEqualTo(7)
    assertThat(retrieved.segments[1].isFullSurah).isFalse()

    val byId = repository.getPlaylistById("test-pl-1")
    assertThat(byId).isNotNull()
    assertThat(byId?.name).isEqualTo("Daily Recitation")
  }

  @Test
  fun `update existing playlist`() {
    val playlist = CustomPlaylist(
      id = "test-pl-1",
      name = "Original Name",
      segments = listOf(PlaylistSegment.createFullSurah(1, quranInfo))
    )
    repository.savePlaylist(playlist)

    val updated = playlist.copy(name = "Updated Name")
    repository.savePlaylist(updated)

    val all = repository.getAllPlaylists()
    assertThat(all).hasSize(1)
    assertThat(all[0].name).isEqualTo("Updated Name")
  }

  @Test
  fun `delete playlist`() {
    val pl1 = CustomPlaylist(id = "1", name = "First")
    val pl2 = CustomPlaylist(id = "2", name = "Second")
    repository.savePlaylist(pl1)
    repository.savePlaylist(pl2)

    assertThat(repository.getAllPlaylists()).hasSize(2)

    repository.deletePlaylist("1")
    val remaining = repository.getAllPlaylists()
    assertThat(remaining).hasSize(1)
    assertThat(remaining[0].id).isEqualTo("2")
  }

  @Test
  fun `updateLastPlayed changes lastPlayedAt and ordering`() {
    val pl1 = CustomPlaylist(id = "1", name = "First", createdAt = 100L, lastPlayedAt = 100L)
    val pl2 = CustomPlaylist(id = "2", name = "Second", createdAt = 200L, lastPlayedAt = 200L)
    repository.savePlaylist(pl1)
    repository.savePlaylist(pl2)

    // Initially pl2 has higher lastPlayedAt
    assertThat(repository.getAllPlaylists()[0].id).isEqualTo("2")

    // Update pl1 to higher lastPlayedAt
    repository.updateLastPlayed("1", 500L)
    assertThat(repository.getAllPlaylists()[0].id).isEqualTo("1")
  }

  @Test
  fun `clearAllPlaylists empties repository`() {
    repository.savePlaylist(CustomPlaylist(id = "1", name = "First"))
    assertThat(repository.getAllPlaylists()).isNotEmpty()

    repository.clearAllPlaylists()
    assertThat(repository.getAllPlaylists()).isEmpty()
  }

  private class FakeSharedPreferences : SharedPreferences {
    private val values = mutableMapOf<String, Any>()
    override fun getAll(): MutableMap<String, *> = values
    override fun getString(key: String?, defValue: String?): String? = values[key] as? String ?: defValue
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
      @Suppress("UNCHECKED_CAST") (values[key] as? MutableSet<String> ?: defValues)
    override fun getInt(key: String?, defValue: Int): Int = values[key] as? Int ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = values[key] as? Long ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = values[key] as? Float ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = values[key] as? Boolean ?: defValue
    override fun contains(key: String?): Boolean = values.containsKey(key)
    override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
      private val pending = mutableMapOf<String, Any>()
      private val removed = mutableSetOf<String>()
      private var clear = false
      override fun putString(k: String?, v: String?) = apply { if (k != null && v != null) { pending[k] = v; removed.remove(k) } }
      override fun putStringSet(k: String?, v: MutableSet<String>?) = apply { if (k != null && v != null) { pending[k] = v; removed.remove(k) } }
      override fun putInt(k: String?, v: Int) = apply { if (k != null) { pending[k] = v; removed.remove(k) } }
      override fun putLong(k: String?, v: Long) = apply { if (k != null) { pending[k] = v; removed.remove(k) } }
      override fun putFloat(k: String?, v: Float) = apply { if (k != null) { pending[k] = v; removed.remove(k) } }
      override fun putBoolean(k: String?, v: Boolean) = apply { if (k != null) { pending[k] = v; removed.remove(k) } }
      override fun remove(k: String?) = apply { if (k != null) { pending.remove(k); removed.add(k) } }
      override fun clear() = apply { clear = true }
      override fun commit(): Boolean { apply(); return true }
      override fun apply() { if (clear) values.clear(); for (k in removed) values.remove(k); values.putAll(pending) }
    }
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
  }
}

`

---

### feature/audiobar/src/main/kotlin/com/quran/mobile/feature/audiobar/ui/MemorizationSetupSheet.kt

`kotlin
package com.quran.mobile.feature.audiobar.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quran.data.model.SuraAyah
import com.quran.labs.androidquran.common.audio.memorization.CustomPlaylist
import com.quran.labs.androidquran.common.audio.memorization.MaskingLevel
import com.quran.labs.androidquran.common.audio.memorization.MasteredRange
import com.quran.labs.androidquran.common.audio.memorization.MemorizationConfig
import com.quran.labs.androidquran.common.audio.memorization.MemorizationDelayMode

/**
 * Bottom sheet modal dialog for configuring and launching an Intelligent Quran Memorization session.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemorizationSetupSheet(
  initialStartAyah: SuraAyah,
  initialEndAyah: SuraAyah,
  onStartMemorization: (MemorizationConfig) -> Unit,
  onDismissRequest: () -> Unit,
  savedSessionConfig: MemorizationConfig? = null,
  savedSessionStep: Int = 0,
  onResumeSession: (() -> Unit)? = null,
  masteredRanges: List<MasteredRange> = emptyList(),
  onReviewRange: ((MasteredRange) -> Unit)? = null,
  savedPlaylists: List<CustomPlaylist> = emptyList(),
  initialPlaylist: CustomPlaylist? = null,
  sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
  var selectedPlaylist by remember { mutableStateOf<CustomPlaylist?>(initialPlaylist) }
  var startAyahNum by remember { mutableIntStateOf(initialStartAyah.ayah) }
  var endAyahNum by remember { mutableIntStateOf(initialEndAyah.ayah.coerceAtLeast(initialStartAyah.ayah + 3)) }
  var windowSize by remember { mutableIntStateOf(4) }
  var singleRepeat by remember { mutableIntStateOf(3) }
  var connectRepeat by remember { mutableIntStateOf(2) }
  var selectedDelayOption by remember { mutableIntStateOf(0) } // 0: None, 1: 3s, 2: 5s, 3: Match Duration
  var maskingLevel by remember { mutableStateOf(MaskingLevel.OFF) }
  var recordRecitation by remember { mutableStateOf(false) }
  var enableBridge by remember { mutableStateOf(true) }

  ModalBottomSheet(
    onDismissRequest = onDismissRequest,
    sheetState = sheetState
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .padding(bottom = 32.dp)
        .verticalScroll(rememberScrollState())
    ) {
      Text(
        text = "Intelligent Memorization",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
      )

      Text(
        text = "Surah ${initialStartAyah.sura} • Progressive Sliding Window Learning",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      if (savedSessionConfig != null && onResumeSession != null) {
        Spacer(modifier = Modifier.height(12.dp))
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
          )
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "Resume Last Session",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
              )
              Text(
                text = "Surah ${savedSessionConfig.startAyah.sura}:${savedSessionConfig.startAyah.ayah}–${savedSessionConfig.endAyah.ayah} • Step ${savedSessionStep + 1}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
              )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
              onClick = onResumeSession,
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
              )
            ) {
              Text("Resume")
            }
          }
        }
      }

      if (masteredRanges.isNotEmpty()) {
        Spacer(modifier = Modifier.height(14.dp))
        Text(
          text = "Recently Mastered & Quick Review",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))

        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          masteredRanges.take(3).forEach { range ->
            Card(
              modifier = Modifier.fillMaxWidth(),
              colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
              )
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = "Surah ${range.startAyah.sura}:${range.startAyah.ayah}–${range.endAyah.ayah}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                  )
                  Text(
                    text = "Mastered • ${range.repetitions}x repeats",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }

                FilledTonalButton(
                  onClick = {
                    if (onReviewRange != null) {
                      onReviewRange(range)
                    } else {
                      onStartMemorization(
                        MemorizationConfig(
                          startAyah = range.startAyah,
                          endAyah = range.endAyah,
                          windowSize = 4,
                          singleRepeat = 1,
                          connectRepeat = 2,
                          delayMode = MemorizationDelayMode.None,
                          memoryTestMode = true,
                          maskingLevel = MaskingLevel.MASK_ON_CONNECT,
                          recordRecitation = false,
                          enableBridge = false
                        )
                      )
                    }
                  },
                  contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                ) {
                  Text("Review Now", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
      HorizontalDivider()
      Spacer(modifier = Modifier.height(12.dp))

      if (savedPlaylists.isNotEmpty() || selectedPlaylist != null) {
        Text(
          text = "Memorization Target",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          FilterChip(
            selected = selectedPlaylist == null,
            onClick = { selectedPlaylist = null },
            label = { Text("Current Surah") }
          )

          savedPlaylists.forEach { pl ->
            FilterChip(
              selected = selectedPlaylist?.id == pl.id,
              onClick = { selectedPlaylist = pl },
              label = { Text(pl.name) }
            )
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
      }

      val currentPl = selectedPlaylist
      if (currentPl != null) {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
          )
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Text(
              text = "Playlist: ${currentPl.name}",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
              text = "${currentPl.summary} across Surahs",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onPrimaryContainer
            )
          }
        }
      } else {
        // Ayah Range Selector
        Text(
          text = "Ayah Range",
          style = MaterialTheme.typography.titleSmall,
          fontWeight = FontWeight.SemiBold
        )

        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          NumberStepper(
            label = "From Ayah",
            value = startAyahNum,
            onValueChange = {
              startAyahNum = it.coerceAtLeast(1)
              if (endAyahNum < startAyahNum) endAyahNum = startAyahNum
            }
          )

          NumberStepper(
            label = "To Ayah",
            value = endAyahNum,
            onValueChange = {
              endAyahNum = it.coerceAtLeast(startAyahNum)
            }
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))
      HorizontalDivider()
      Spacer(modifier = Modifier.height(12.dp))

      // Parameters: Window Size, Single Repeat, Connect Repeat
      Text(
        text = "Memorization Parameters",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
      )

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        NumberStepper(
          label = "Window Size",
          value = windowSize,
          onValueChange = { windowSize = it.coerceIn(1, 10) }
        )

        NumberStepper(
          label = "Solo Repeat",
          value = singleRepeat,
          onValueChange = { singleRepeat = it.coerceIn(1, 10) }
        )

        NumberStepper(
          label = "Connect Repeat",
          value = connectRepeat,
          onValueChange = { connectRepeat = it.coerceIn(1, 10) }
        )
      }

      Spacer(modifier = Modifier.height(12.dp))
      HorizontalDivider()
      Spacer(modifier = Modifier.height(12.dp))

      // Delay Mode (Recitation Pause)
      Text(
        text = "Pause Between Repetitions (Silent Recitation)",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
      )

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        FilterChip(
          selected = selectedDelayOption == 0,
          onClick = { selectedDelayOption = 0 },
          label = { Text("None") }
        )
        FilterChip(
          selected = selectedDelayOption == 1,
          onClick = { selectedDelayOption = 1 },
          label = { Text("3 sec") }
        )
        FilterChip(
          selected = selectedDelayOption == 2,
          onClick = { selectedDelayOption = 2 },
          label = { Text("5 sec") }
        )
        FilterChip(
          selected = selectedDelayOption == 3,
          onClick = { selectedDelayOption = 3 },
          label = { Text("Match Ayah") }
        )
      }

      Spacer(modifier = Modifier.height(12.dp))
      HorizontalDivider()
      Spacer(modifier = Modifier.height(12.dp))

      // Memory Test Masking Difficulty
      Text(
        text = "Memory Test Masking",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
      )

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = "Masks Ayah text so you can test your memory (tap Peek anytime to reveal)",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(8.dp))

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        FilterChip(
          selected = maskingLevel == MaskingLevel.OFF,
          onClick = { maskingLevel = MaskingLevel.OFF },
          label = { Text("Off") }
        )
        FilterChip(
          selected = maskingLevel == MaskingLevel.MASK_ON_CONNECT,
          onClick = { maskingLevel = MaskingLevel.MASK_ON_CONNECT },
          label = { Text("Connect Steps") }
        )
        FilterChip(
          selected = maskingLevel == MaskingLevel.PROGRESSIVE_SOLO_AND_CONNECT,
          onClick = { maskingLevel = MaskingLevel.PROGRESSIVE_SOLO_AND_CONNECT },
          label = { Text("Progressive") }
        )
      }

      Spacer(modifier = Modifier.height(12.dp))
      HorizontalDivider()
      Spacer(modifier = Modifier.height(12.dp))

      // Record & Replay My Recitation Toggle
      val canRecord = selectedDelayOption != 0
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Record & Replay My Recitation",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = if (canRecord) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
          )
          Text(
            text = if (canRecord) "Records your voice during pauses for instant self-check against the Qari"
                   else "Requires a recitation pause duration (select 3s, 5s, or Match Ayah above)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Switch(
          checked = recordRecitation && canRecord,
          enabled = canRecord,
          onCheckedChange = { recordRecitation = it }
        )
      }

      Spacer(modifier = Modifier.height(12.dp))
      HorizontalDivider()
      Spacer(modifier = Modifier.height(12.dp))

      // Bridge Step Toggle
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "Connect Windows with Bridge Step",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
          )
          Text(
            text = "Recites transition between consecutive windows",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
        Switch(
          checked = enableBridge,
          onCheckedChange = { enableBridge = it }
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Action Buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End
      ) {
        TextButton(onClick = onDismissRequest) {
          Text("Cancel")
        }

        Spacer(modifier = Modifier.width(8.dp))

        Button(
          onClick = {
            val delayMode = when (selectedDelayOption) {
              1 -> MemorizationDelayMode.FixedSeconds(3)
              2 -> MemorizationDelayMode.FixedSeconds(5)
              3 -> MemorizationDelayMode.MatchAyahDuration
              else -> MemorizationDelayMode.None
            }

            val pl = selectedPlaylist
            val config = if (pl != null && pl.segments.isNotEmpty()) {
              val firstAyah = SuraAyah(pl.segments.first().sura, pl.segments.first().startAyah)
              val lastAyah = SuraAyah(pl.segments.last().sura, pl.segments.last().endAyah)
              MemorizationConfig(
                startAyah = firstAyah,
                endAyah = lastAyah,
                windowSize = windowSize,
                singleRepeat = singleRepeat,
                connectRepeat = connectRepeat,
                delayMode = delayMode,
                memoryTestMode = maskingLevel != MaskingLevel.OFF,
                maskingLevel = maskingLevel,
                recordRecitation = recordRecitation && canRecord,
                enableBridge = enableBridge,
                segments = pl.segments,
                playlistId = pl.id,
                playlistName = pl.name
              )
            } else {
              MemorizationConfig(
                startAyah = SuraAyah(initialStartAyah.sura, startAyahNum),
                endAyah = SuraAyah(initialStartAyah.sura, endAyahNum),
                windowSize = windowSize,
                singleRepeat = singleRepeat,
                connectRepeat = connectRepeat,
                delayMode = delayMode,
                memoryTestMode = maskingLevel != MaskingLevel.OFF,
                maskingLevel = maskingLevel,
                recordRecitation = recordRecitation && canRecord,
                enableBridge = enableBridge
              )
            }

            onStartMemorization(config)
          }
        ) {
          Text("Start Memorizing", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun NumberStepper(
  label: String,
  value: Int,
  onValueChange: (Int) -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = modifier
  ) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(4.dp))

    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.Center
    ) {
      FilledTonalButton(
        onClick = { onValueChange(value - 1) },
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp)
      ) {
        Text("-", fontSize = 16.sp, fontWeight = FontWeight.Bold)
      }

      Text(
        text = value.toString(),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 12.dp)
      )

      FilledTonalButton(
        onClick = { onValueChange(value + 1) },
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 2.dp)
      ) {
        Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}

`

---

### feature/audiobar/src/main/kotlin/com/quran/mobile/feature/audiobar/ui/MemorizationSetupSheetWrapper.kt

`kotlin
package com.quran.mobile.feature.audiobar.ui

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import com.quran.data.model.SuraAyah
import com.quran.labs.androidquran.common.audio.memorization.CustomPlaylist
import com.quran.labs.androidquran.common.audio.memorization.MaskingLevel
import com.quran.labs.androidquran.common.audio.memorization.MasteredRange
import com.quran.labs.androidquran.common.audio.memorization.MemorizationConfig
import com.quran.labs.androidquran.common.audio.memorization.MemorizationDelayMode
import com.quran.labs.androidquran.common.ui.core.QuranTheme

/**
 * FrameLayout wrapper hosting the Compose [MemorizationSetupSheet]
 * for display inside PagerActivity's overlay container.
 */
class MemorizationSetupSheetWrapper(
  context: Context,
  private val initialStartAyah: SuraAyah,
  private val initialEndAyah: SuraAyah,
  private val onStart: (MemorizationConfig) -> Unit,
  private val onDismissed: () -> Unit,
  private val savedSessionConfig: MemorizationConfig? = null,
  private val savedSessionStep: Int = 0,
  private val onResume: (() -> Unit)? = null,
  private val masteredRanges: List<MasteredRange> = emptyList(),
  private val onReview: ((MasteredRange) -> Unit)? = null,
  private val savedPlaylists: List<CustomPlaylist> = emptyList(),
  private val initialPlaylist: CustomPlaylist? = null
) : FrameLayout(context) {

  init {
    val composeView = ComposeView(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
      )
      setContent {
        QuranTheme {
          MemorizationSetupSheet(
            initialStartAyah = initialStartAyah,
            initialEndAyah = initialEndAyah,
            savedSessionConfig = savedSessionConfig,
            savedSessionStep = savedSessionStep,
            masteredRanges = masteredRanges,
            savedPlaylists = savedPlaylists,
            initialPlaylist = initialPlaylist,
            onResumeSession = {
              onResume?.invoke()
              onDismissed()
            },
            onReviewRange = { range ->
              if (onReview != null) {
                onReview.invoke(range)
              } else {
                val reviewConfig = MemorizationConfig(
                  startAyah = range.startAyah,
                  endAyah = range.endAyah,
                  windowSize = 4,
                  singleRepeat = 1,
                  connectRepeat = 2,
                  delayMode = MemorizationDelayMode.None,
                  memoryTestMode = true,
                  maskingLevel = MaskingLevel.MASK_ON_CONNECT,
                  recordRecitation = false,
                  enableBridge = false
                )
                onStart(reviewConfig)
              }
              onDismissed()
            },
            onStartMemorization = { config ->
              onStart(config)
              onDismissed()
            },
            onDismissRequest = onDismissed
          )
        }
      }
    }
    addView(composeView)
  }
}

`

---

### feature/audiobar/src/main/kotlin/com/quran/mobile/feature/audiobar/ui/MemorizationHud.kt

`kotlin
package com.quran.mobile.feature.audiobar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quran.labs.androidquran.common.audio.memorization.MemorizationState
import com.quran.labs.androidquran.common.audio.memorization.StepType
import com.quran.labs.androidquran.common.ui.core.LocalQuranColors
import com.quran.labs.androidquran.common.ui.core.QuranIcons

/**
 * Active HUD / Player Bar for the Intelligent Quran Memorization Mode.
 *
 * Displays:
 * - Active window index and total windows (e.g. "Window 1/3")
 * - Active step description & repeat counter (e.g. "Connecting Ayah 1–3 (Repeat 2/2)")
 * - Play/Pause, Previous Step, Skip Step, and Exit controls
 * - Peek/Hide text toggle when Memory Test Mode is active
 */
@Composable
fun MemorizationHud(
  state: MemorizationState,
  onPreviousStep: () -> Unit,
  onPlayPause: () -> Unit,
  onSkipStep: () -> Unit,
  onStop: () -> Unit,
  onToggleMask: () -> Unit,
  onReplayRecitation: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val colors = LocalQuranColors.current

  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant,
    modifier = modifier.fillMaxWidth()
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
      // Top status row: Window and Step Indicator
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        // Step Type Badge
        val badgeLabel = when {
          state.isRecordingRecitation -> "RECORDING"
          state.stepType == StepType.SOLO -> "SOLO"
          state.stepType == StepType.CONNECT -> "CONNECT"
          state.stepType == StepType.PAUSE_FOR_RECITATION -> "RECITE"
          state.stepType == StepType.BRIDGE -> "BRIDGE"
          else -> ""
        }
        val badgeColor = when {
          state.isRecordingRecitation -> MaterialTheme.colorScheme.error
          state.stepType == StepType.CONNECT -> MaterialTheme.colorScheme.primary
          state.stepType == StepType.PAUSE_FOR_RECITATION -> MaterialTheme.colorScheme.tertiary
          state.stepType == StepType.BRIDGE -> MaterialTheme.colorScheme.secondary
          else -> MaterialTheme.colorScheme.outline
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            color = badgeColor,
            shape = MaterialTheme.shapes.extraSmall,
            modifier = Modifier.padding(end = 8.dp)
          ) {
            Text(
              text = badgeLabel,
              color = MaterialTheme.colorScheme.onPrimary,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }

          Text(
            text = "Window ${state.currentWindowIndex + 1}/${state.totalWindows}",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.defaultTextColor
          )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
          if (state.lastRecitationAudioPath != null && onReplayRecitation != null) {
            TextButton(onClick = onReplayRecitation) {
              Icon(
                QuranIcons.Mic,
                contentDescription = "Replay My Recitation",
                modifier = Modifier.padding(end = 4.dp),
                tint = MaterialTheme.colorScheme.primary
              )
              Text(
                text = "Replay Voice",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
            }
          }

          // Tap-to-peek toggle if test mode active
          if (state.config?.memoryTestMode == true) {
            TextButton(onClick = onToggleMask) {
              Text(
                text = if (state.isTextMasked) "Peek Text" else "Hide Text",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
              )
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(2.dp))

      // Center summary text: e.g. "Connecting Ayah 1–3 (Repeat 2/2)"
      Text(
        text = state.hudSummary,
        style = MaterialTheme.typography.bodyMedium,
        color = colors.defaultTextColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.fillMaxWidth()
      )

      Spacer(modifier = Modifier.height(4.dp))

      // Playback Controls Row
      Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
      ) {
        // Exit / Stop Button
        IconButton(onClick = onStop) {
          Icon(
            QuranIcons.Close,
            contentDescription = "Exit Memorization",
            tint = colors.defaultTextColor
          )
        }

        // Previous Step Button
        IconButton(onClick = onPreviousStep) {
          Icon(
            QuranIcons.FastRewind,
            contentDescription = "Previous Step",
            tint = colors.defaultTextColor
          )
        }

        // Play / Pause Action Button
        IconButton(onClick = onPlayPause) {
          Icon(
            if (state.isPaused) QuranIcons.PlayArrow else QuranIcons.Pause,
            contentDescription = if (state.isPaused) "Resume" else "Pause",
            tint = MaterialTheme.colorScheme.primary
          )
        }

        // Skip Step Button
        IconButton(onClick = onSkipStep) {
          Icon(
            QuranIcons.FastForward,
            contentDescription = "Skip Step",
            tint = colors.defaultTextColor
          )
        }
      }
    }
  }
}

`

---

### feature/audiobar/src/main/kotlin/com/quran/mobile/feature/audiobar/ui/PlaylistEditorSheet.kt

`kotlin
package com.quran.mobile.feature.audiobar.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quran.data.core.QuranInfo
import com.quran.labs.androidquran.common.audio.memorization.CustomPlaylist
import com.quran.labs.androidquran.common.audio.memorization.PlaylistSegment
import com.quran.labs.androidquran.common.ui.core.QuranIcons
import com.quran.mobile.common.ui.core.R as UiCoreR

/**
 * Bottom sheet modal for creating and editing a custom Quran playlist with multiple Surah segments.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistEditorSheet(
  playlistToEdit: CustomPlaylist? = null,
  quranInfo: QuranInfo,
  onSavePlaylist: (CustomPlaylist) -> Unit,
  onDismissRequest: () -> Unit,
  sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
  val context = LocalContext.current
  val suraNames = remember {
    try {
      context.resources.getStringArray(UiCoreR.array.sura_names)
    } catch (e: Exception) {
      Array(114) { "Surah ${it + 1}" }
    }
  }

  fun getSuraTitle(sura: Int): String {
    val name = if (sura - 1 in suraNames.indices) suraNames[sura - 1] else "Surah $sura"
    return "$sura. $name"
  }

  var playlistName by remember { mutableStateOf(playlistToEdit?.name ?: "") }
  var segments by remember { mutableStateOf(playlistToEdit?.segments?.toMutableList() ?: mutableListOf()) }

  // New segment form state
  var selectedSura by remember { mutableIntStateOf(1) }
  var isFullSurah by remember { mutableStateOf(true) }
  val maxAyahsForSelectedSura = remember(selectedSura) {
    quranInfo.getNumberOfAyahs(selectedSura).coerceAtLeast(1)
  }
  var startAyah by remember { mutableIntStateOf(1) }
  var endAyah by remember { mutableIntStateOf(quranInfo.getNumberOfAyahs(1).coerceAtLeast(1)) }

  var suraDropdownExpanded by remember { mutableStateOf(false) }

  ModalBottomSheet(
    onDismissRequest = onDismissRequest,
    sheetState = sheetState
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .padding(bottom = 32.dp)
        .verticalScroll(rememberScrollState())
    ) {
      Text(
        text = if (playlistToEdit == null) "Create New Playlist" else "Edit Playlist",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = "Combine Surahs or Ayah sequences into a continuous playlist",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(16.dp))

      OutlinedTextField(
        value = playlistName,
        onValueChange = { playlistName = it },
        label = { Text("Playlist Name") },
        placeholder = { Text("e.g. Daily Recitation, Morning Adhkar") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
      )

      Spacer(modifier = Modifier.height(16.dp))
      HorizontalDivider()
      Spacer(modifier = Modifier.height(12.dp))

      // Current Segments List
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Playlist Segments (${segments.size})",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold
        )
        val totalAyahs = segments.sumOf { (it.endAyah - it.startAyah + 1).coerceAtLeast(0) }
        Text(
          text = "$totalAyahs Ayahs total",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.primary
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      if (segments.isEmpty()) {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
          )
        ) {
          Text(
            text = "No segments added yet. Add your first Surah sequence below.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
          )
        }
      } else {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          segments.forEachIndexed { index, segment ->
            Card(
              modifier = Modifier.fillMaxWidth(),
              colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
              )
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = "#${index + 1}",
                  style = MaterialTheme.typography.labelMedium,
                  fontWeight = FontWeight.Bold,
                  color = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.padding(end = 10.dp)
                )

                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = getSuraTitle(segment.sura),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                  )
                  val rangeDesc = if (segment.isFullSurah) {
                    "Full Surah (1–${segment.endAyah})"
                  } else {
                    "Ayah ${segment.startAyah} – ${segment.endAyah} (${segment.endAyah - segment.startAyah + 1} Ayahs)"
                  }
                  Text(
                    text = rangeDesc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }

                // Reorder up
                if (index > 0) {
                  IconButton(
                    onClick = {
                      val mutable = segments.toMutableList()
                      val item = mutable.removeAt(index)
                      mutable.add(index - 1, item)
                      segments = mutable
                    }
                  ) {
                    Icon(QuranIcons.KeyboardArrowUp, contentDescription = "Move up")
                  }
                }

                // Reorder down
                if (index < segments.size - 1) {
                  IconButton(
                    onClick = {
                      val mutable = segments.toMutableList()
                      val item = mutable.removeAt(index)
                      mutable.add(index + 1, item)
                      segments = mutable
                    }
                  ) {
                    Icon(QuranIcons.KeyboardArrowDown, contentDescription = "Move down")
                  }
                }

                // Remove
                IconButton(
                  onClick = {
                    val mutable = segments.toMutableList()
                    mutable.removeAt(index)
                    segments = mutable
                  }
                ) {
                  Icon(
                    QuranIcons.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                  )
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
      HorizontalDivider()
      Spacer(modifier = Modifier.height(12.dp))

      // Add New Segment Form
      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
          containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        )
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Text(
            text = "Add Surah Sequence",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
          )

          Spacer(modifier = Modifier.height(8.dp))

          // Surah Selector Dropdown
          Box {
            OutlinedButton(
              onClick = { suraDropdownExpanded = true },
              modifier = Modifier.fillMaxWidth()
            ) {
              Text(
                text = getSuraTitle(selectedSura),
                modifier = Modifier.weight(1f)
              )
              Icon(QuranIcons.ExpandMore, contentDescription = null)
            }

            DropdownMenu(
              expanded = suraDropdownExpanded,
              onDismissRequest = { suraDropdownExpanded = false }
            ) {
              (1..114).forEach { suraNum ->
                DropdownMenuItem(
                  text = { Text(getSuraTitle(suraNum)) },
                  onClick = {
                    selectedSura = suraNum
                    val count = quranInfo.getNumberOfAyahs(suraNum).coerceAtLeast(1)
                    startAyah = 1
                    endAyah = count
                    suraDropdownExpanded = false
                  }
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Full Surah vs Custom Range Toggle
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            FilterChip(
              selected = isFullSurah,
              onClick = {
                isFullSurah = true
                startAyah = 1
                endAyah = maxAyahsForSelectedSura
              },
              label = { Text("Full Surah ($maxAyahsForSelectedSura Ayahs)") }
            )

            FilterChip(
              selected = !isFullSurah,
              onClick = {
                isFullSurah = false
              },
              label = { Text("Custom Range") }
            )
          }

          // Ayah Steppers when Custom Range is active
          if (!isFullSurah) {
            Spacer(modifier = Modifier.height(10.dp))
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              NumberStepperCompact(
                label = "From Ayah",
                value = startAyah,
                minValue = 1,
                maxValue = endAyah,
                onValueChange = {
                  startAyah = it.coerceIn(1, endAyah)
                }
              )

              NumberStepperCompact(
                label = "To Ayah",
                value = endAyah,
                minValue = startAyah,
                maxValue = maxAyahsForSelectedSura,
                onValueChange = {
                  endAyah = it.coerceIn(startAyah, maxAyahsForSelectedSura)
                }
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          FilledTonalButton(
            onClick = {
              val newSegment = PlaylistSegment(
                sura = selectedSura,
                startAyah = if (isFullSurah) 1 else startAyah,
                endAyah = if (isFullSurah) maxAyahsForSelectedSura else endAyah,
                isFullSurah = isFullSurah,
                customLabel = getSuraTitle(selectedSura)
              )
              val mutable = segments.toMutableList()
              mutable.add(newSegment)
              segments = mutable

              // Reset to default
              isFullSurah = true
              startAyah = 1
              endAyah = maxAyahsForSelectedSura
            },
            modifier = Modifier.fillMaxWidth()
          ) {
            Icon(QuranIcons.Add, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
            Text("Add to Playlist", fontWeight = FontWeight.SemiBold)
          }
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Bottom Action Buttons
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        OutlinedButton(
          onClick = onDismissRequest,
          modifier = Modifier.weight(1f)
        ) {
          Text("Cancel")
        }

        Button(
          onClick = {
            if (playlistName.isNotBlank() && segments.isNotEmpty()) {
              val saved = CustomPlaylist(
                id = playlistToEdit?.id ?: java.util.UUID.randomUUID().toString(),
                name = playlistName.trim(),
                segments = segments,
                createdAt = playlistToEdit?.createdAt ?: System.currentTimeMillis(),
                lastPlayedAt = playlistToEdit?.lastPlayedAt ?: 0L
              )
              onSavePlaylist(saved)
            }
          },
          enabled = playlistName.isNotBlank() && segments.isNotEmpty(),
          modifier = Modifier.weight(1f)
        ) {
          Text("Save Playlist", fontWeight = FontWeight.Bold)
        }
      }
    }
  }
}

@Composable
private fun NumberStepperCompact(
  label: String,
  value: Int,
  minValue: Int,
  maxValue: Int,
  onValueChange: (Int) -> Unit
) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(4.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
      FilledTonalButton(
        onClick = { if (value > minValue) onValueChange(value - 1) },
        enabled = value > minValue,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
      ) {
        Text("-", fontSize = 16.sp, fontWeight = FontWeight.Bold)
      }

      Text(
        text = value.toString(),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 10.dp)
      )

      FilledTonalButton(
        onClick = { if (value < maxValue) onValueChange(value + 1) },
        enabled = value < maxValue,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 2.dp)
      ) {
        Text("+", fontSize = 16.sp, fontWeight = FontWeight.Bold)
      }
    }
  }
}

`

---

### feature/audiobar/src/main/kotlin/com/quran/mobile/feature/audiobar/ui/PlaylistListSheet.kt

`kotlin
package com.quran.mobile.feature.audiobar.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quran.data.core.QuranInfo
import com.quran.labs.androidquran.common.audio.memorization.CustomPlaylist
import com.quran.labs.androidquran.common.audio.memorization.PlaylistRepository
import com.quran.labs.androidquran.common.ui.core.QuranIcons
import com.quran.mobile.common.ui.core.R as UiCoreR

/**
 * Bottom sheet displaying all user-created Quran playlists with options to Play, Memorize, Edit, or Delete.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistListSheet(
  playlistRepository: PlaylistRepository,
  quranInfo: QuranInfo,
  onPlayPlaylist: (CustomPlaylist) -> Unit,
  onMemorizePlaylist: (CustomPlaylist) -> Unit,
  onDismissRequest: () -> Unit,
  sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
  val context = LocalContext.current
  val suraNames = remember {
    try {
      context.resources.getStringArray(UiCoreR.array.sura_names)
    } catch (e: Exception) {
      Array(114) { "Surah ${it + 1}" }
    }
  }

  fun getSuraShortName(sura: Int): String {
    return if (sura - 1 in suraNames.indices) suraNames[sura - 1] else "Surah $sura"
  }

  var playlists by remember { mutableStateOf(playlistRepository.getAllPlaylists()) }
  var playlistToEdit by remember { mutableStateOf<CustomPlaylist?>(null) }
  var isEditorOpen by remember { mutableStateOf(false) }

  ModalBottomSheet(
    onDismissRequest = onDismissRequest,
    sheetState = sheetState
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp)
        .padding(bottom = 32.dp)
        .verticalScroll(rememberScrollState())
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "My Quran Playlists",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "Custom sequences across Surahs",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        FilledTonalButton(
          onClick = {
            playlistToEdit = null
            isEditorOpen = true
          }
        ) {
          Icon(QuranIcons.Add, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
          Text("New")
        }
      }

      Spacer(modifier = Modifier.height(16.dp))
      HorizontalDivider()
      Spacer(modifier = Modifier.height(12.dp))

      if (playlists.isEmpty()) {
        Card(
          modifier = Modifier.fillMaxWidth(),
          colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
          )
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(
              QuranIcons.MenuBook,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.height(40.dp).width(40.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "No playlists created yet",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Create custom sequences of full Surahs or specific Ayah ranges for continuous listening or memorization.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
              onClick = {
                playlistToEdit = null
                isEditorOpen = true
              }
            ) {
              Icon(QuranIcons.Add, contentDescription = null, modifier = Modifier.padding(end = 6.dp))
              Text("Create First Playlist")
            }
          }
        }
      } else {
        Column(
          modifier = Modifier.fillMaxWidth(),
          verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          playlists.forEach { playlist ->
            Card(
              modifier = Modifier.fillMaxWidth(),
              colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
              )
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.Top
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = playlist.name,
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold
                    )
                    Text(
                      text = playlist.summary,
                      style = MaterialTheme.typography.bodySmall,
                      color = MaterialTheme.colorScheme.primary
                    )
                  }

                  Row {
                    IconButton(
                      onClick = {
                        playlistToEdit = playlist
                        isEditorOpen = true
                      }
                    ) {
                      Icon(QuranIcons.Edit, contentDescription = "Edit")
                    }

                    IconButton(
                      onClick = {
                        playlistRepository.deletePlaylist(playlist.id)
                        playlists = playlistRepository.getAllPlaylists()
                      }
                    ) {
                      Icon(
                        QuranIcons.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                      )
                    }
                  }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Sequence summary preview
                val previewText = playlist.segments.joinToString(" ➔ ") { seg ->
                  if (seg.isFullSurah) {
                    getSuraShortName(seg.sura)
                  } else {
                    "${getSuraShortName(seg.sura)} (${seg.startAyah}–${seg.endAyah})"
                  }
                }
                Text(
                  text = previewText,
                  style = MaterialTheme.typography.bodySmall,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  maxLines = 2
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Action buttons: Play & Memorize
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  FilledTonalButton(
                    onClick = {
                      playlistRepository.updateLastPlayed(playlist.id)
                      onPlayPlaylist(playlist)
                      onDismissRequest()
                    },
                    modifier = Modifier.weight(1f)
                  ) {
                    Icon(QuranIcons.PlayArrow, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text("Play All")
                  }

                  Button(
                    onClick = {
                      playlistRepository.updateLastPlayed(playlist.id)
                      onMemorizePlaylist(playlist)
                      onDismissRequest()
                    },
                    modifier = Modifier.weight(1f)
                  ) {
                    Icon(QuranIcons.MenuBook, contentDescription = null, modifier = Modifier.padding(end = 4.dp))
                    Text("Memorize", fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  // Nested Editor Sheet when creating/editing
  if (isEditorOpen) {
    PlaylistEditorSheet(
      playlistToEdit = playlistToEdit,
      quranInfo = quranInfo,
      onSavePlaylist = { saved ->
        playlistRepository.savePlaylist(saved)
        playlists = playlistRepository.getAllPlaylists()
        isEditorOpen = false
      },
      onDismissRequest = {
        isEditorOpen = false
      }
    )
  }
}

`

---

### feature/audiobar/src/main/kotlin/com/quran/mobile/feature/audiobar/ui/PlaylistSheetWrapper.kt

`kotlin
package com.quran.mobile.feature.audiobar.ui

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import com.quran.data.core.QuranInfo
import com.quran.labs.androidquran.common.audio.memorization.CustomPlaylist
import com.quran.labs.androidquran.common.audio.memorization.PlaylistRepository
import com.quran.labs.androidquran.common.ui.core.QuranTheme

/**
 * FrameLayout wrapper hosting the Compose [PlaylistListSheet] for display inside PagerActivity's overlay container.
 */
class PlaylistSheetWrapper(
  context: Context,
  private val playlistRepository: PlaylistRepository,
  private val quranInfo: QuranInfo,
  private val onPlayPlaylist: (CustomPlaylist) -> Unit,
  private val onMemorizePlaylist: (CustomPlaylist) -> Unit,
  private val onDismissed: () -> Unit
) : FrameLayout(context) {

  init {
    val composeView = ComposeView(context).apply {
      layoutParams = LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT
      )
      setContent {
        QuranTheme {
          PlaylistListSheet(
            playlistRepository = playlistRepository,
            quranInfo = quranInfo,
            onPlayPlaylist = { playlist ->
              onPlayPlaylist(playlist)
              onDismissed()
            },
            onMemorizePlaylist = { playlist ->
              onMemorizePlaylist(playlist)
              onDismissed()
            },
            onDismissRequest = onDismissed
          )
        }
      }
    }
    addView(composeView)
  }
}

`

---

### feature/audiobar/src/main/kotlin/com/quran/mobile/feature/audiobar/ui/StoppedAudioBar.kt

`kotlin
package com.quran.mobile.feature.audiobar.ui

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.quran.labs.androidquran.common.ui.core.LocalQuranColors
import com.quran.labs.androidquran.common.ui.core.QuranIcons
import com.quran.labs.androidquran.common.ui.core.QuranTheme
import com.quran.mobile.feature.audiobar.state.AudioBarState
import com.quran.mobile.feature.audiobar.state.AudioBarUiEvent

@Composable
internal fun StoppedAudioBar(
  state: AudioBarState.Stopped,
  eventSink: (AudioBarUiEvent.StoppedPlaybackEvent) -> Unit,
  modifier: Modifier = Modifier
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier.height(IntrinsicSize.Min)
  ) {
    IconButton(onClick = { eventSink(AudioBarUiEvent.StoppedPlaybackEvent.Play) }) {
      Icon(QuranIcons.PlayArrow, contentDescription = "")
    }

    IconButton(onClick = { eventSink(AudioBarUiEvent.StoppedPlaybackEvent.Memorize) }) {
      Icon(QuranIcons.MenuBook, contentDescription = "Memorization Mode")
    }

    IconButton(onClick = { eventSink(AudioBarUiEvent.StoppedPlaybackEvent.Playlists) }) {
      Icon(QuranIcons.List, contentDescription = "Playlists")
    }

    TextButton(
      modifier = Modifier.weight(1f),
      onClick = { eventSink(AudioBarUiEvent.StoppedPlaybackEvent.ChangeQari) }
    ) {
      Text(
        text = stringResource(state.qariNameResource),
        color = LocalQuranColors.current.defaultTextColor
      )
      Spacer(modifier = Modifier.weight(1f))
      Icon(QuranIcons.ExpandMore, contentDescription = "", tint =
        LocalQuranColors.current.defaultTextColor)
    }

    if (state.enableRecording) {
      IconButton(onClick = { eventSink(AudioBarUiEvent.StoppedPlaybackEvent.Record) }) {
        Icon(QuranIcons.Mic, contentDescription = "")
      }
    }
  }
}

@Preview
@Composable
private fun StoppedAudioBarPreview() {
  QuranTheme {
    StoppedAudioBar(
      state = AudioBarState.Stopped(
        qariNameResource = com.quran.labs.androidquran.common.audio.R.string.qari_dussary,
        enableRecording = false
      ),
      eventSink = {}
    )
  }
}

@Preview
@Composable
private fun StoppedAudioBarWithRecordingPreview() {
  QuranTheme {
    StoppedAudioBar(
      state = AudioBarState.Stopped(
        qariNameResource = com.quran.labs.androidquran.common.audio.R.string.qari_dussary,
        enableRecording = true
      ),
      eventSink = {}
    )
  }
}

`

---

### feature/audiobar/src/main/kotlin/com/quran/mobile/feature/audiobar/state/AudioBarEvent.kt

`kotlin
package com.quran.mobile.feature.audiobar.state

sealed class AudioBarEvent {
  data object ChangeQari : AudioBarEvent()
  data object Memorize : AudioBarEvent()
  data object Playlists : AudioBarEvent()
  data object Cancel : AudioBarEvent()
  data object CancelDownload : AudioBarEvent()
  data object Acknowledge : AudioBarEvent()

  // playback
  data object Play : AudioBarEvent()
  data object ResumePlayback : AudioBarEvent()
  data object Pause : AudioBarEvent()
  data object Stop : AudioBarEvent()
  data object FastForward : AudioBarEvent()
  data object Rewind : AudioBarEvent()
  data object ShowSettings : AudioBarEvent()
  data class SetRepeat(val repeat: Int) : AudioBarEvent()
  data class SetSpeed(val speed: Float) : AudioBarEvent()

  // recitation
  data object Record : AudioBarEvent()
  data object Recitation : AudioBarEvent()
  data object RecitationLongPress : AudioBarEvent()
  data object Transcript : AudioBarEvent()
  data object HideVerses : AudioBarEvent()
  data object EndSession : AudioBarEvent()
  data object PlayRecitation : AudioBarEvent()
  data object PauseRecitation : AudioBarEvent()
}

`

---

### feature/audiobar/src/main/kotlin/com/quran/mobile/feature/audiobar/state/AudioBarScreen.kt

`kotlin
package com.quran.mobile.feature.audiobar.state

internal sealed class AudioBarState {
  sealed class ActivePlayback : AudioBarState() {
    abstract val repeat: Int
    abstract val speed: Float
  }

  data class Playing(
    override val repeat: Int,
    override val speed: Float
  ) : ActivePlayback()

  data class Paused(
    override val repeat: Int,
    override val speed: Float
  ) : ActivePlayback()

  data class Stopped(
    val qariNameResource: Int,
    val enableRecording: Boolean
  ) : AudioBarState()

  data class Downloading(
    val progress: Int,
    val messageResource: Int
  ) : AudioBarState()

  data class Loading(
    val progress: Int,
    val messageResource: Int
  ) : AudioBarState()

  data class Error(
    val messageResource: Int
  ) : AudioBarState()

  data class Prompt(
    val messageResource: Int
  ) : AudioBarState()

  data object RecitationListening : AudioBarState()

  data object RecitationPlaying : AudioBarState()

  data object RecitationStopped : AudioBarState()
}

internal class AudioBarUiEvents(
  val commonPlaybackEventSink: (AudioBarUiEvent.CommonPlaybackEvent) -> Unit = {},
  val playingPlaybackEventSink: (AudioBarUiEvent.PlayingPlaybackEvent) -> Unit = {},
  val pausedPlaybackEventSink: (AudioBarUiEvent.PausedPlaybackEvent) -> Unit = {},
  val stoppedEventSink: (AudioBarUiEvent.StoppedPlaybackEvent) -> Unit = {},
  val downloadingEventSink: (AudioBarUiEvent.DownloadingPlaybackEvent) -> Unit = {},
  val cancelableEventSink: (AudioBarUiEvent.CancelablePlaybackEvent) -> Unit = {},
  val promptEventSink: (AudioBarUiEvent.PromptEvent) -> Unit = {},
  val commonRecordingEventSink: (AudioBarUiEvent.CommonRecordingEvent) -> Unit = {},
  val recitationListeningEventSink: (AudioBarUiEvent.RecitationListeningEvent) -> Unit = {},
  val recitationPlayingEventSink: (AudioBarUiEvent.RecitationPlayingEvent) -> Unit = {},
  val recitationStoppedEventSink: (AudioBarUiEvent.RecitationStoppedEvent) -> Unit = {}
)

internal sealed class AudioBarUiEvent(val audioBarEvent: AudioBarEvent) {
  sealed class CommonPlaybackEvent(audioBarEvent: AudioBarEvent) :
    AudioBarUiEvent(audioBarEvent) {
    data object Stop : CommonPlaybackEvent(AudioBarEvent.Stop)
    data object Rewind : CommonPlaybackEvent(AudioBarEvent.Rewind)
    data object FastForward : CommonPlaybackEvent(AudioBarEvent.FastForward)
    data class SetSpeed(val speed: Float) : CommonPlaybackEvent(AudioBarEvent.SetSpeed(speed))
    data class SetRepeat(val repeat: Int) : CommonPlaybackEvent(AudioBarEvent.SetRepeat(repeat))
    data object ShowSettings : CommonPlaybackEvent(AudioBarEvent.ShowSettings)
  }

  sealed class PlayingPlaybackEvent(audioBarEvent: AudioBarEvent) :
    AudioBarUiEvent(audioBarEvent) {
    data object Pause : PlayingPlaybackEvent(AudioBarEvent.Pause)
  }

  sealed class PausedPlaybackEvent(audioBarEvent: AudioBarEvent) :
    AudioBarUiEvent(audioBarEvent) {
    data object Play : PausedPlaybackEvent(AudioBarEvent.ResumePlayback)
  }

  sealed class DownloadingPlaybackEvent(event: AudioBarEvent) : AudioBarUiEvent(event) {
    data object Cancel : DownloadingPlaybackEvent(AudioBarEvent.CancelDownload)
  }

  sealed class CancelablePlaybackEvent(event: AudioBarEvent) : AudioBarUiEvent(event) {
    data object Cancel : CancelablePlaybackEvent(AudioBarEvent.Cancel)
  }

  sealed class StoppedPlaybackEvent(audioBarEvent: AudioBarEvent) :
    AudioBarUiEvent(audioBarEvent) {
    data object ChangeQari : StoppedPlaybackEvent(AudioBarEvent.ChangeQari)
    data object Memorize : StoppedPlaybackEvent(AudioBarEvent.Memorize)
    data object Playlists : StoppedPlaybackEvent(AudioBarEvent.Playlists)
    data object Play : StoppedPlaybackEvent(AudioBarEvent.Play)
    data object Record : StoppedPlaybackEvent(AudioBarEvent.Record)
  }

  sealed class PromptEvent(audioBarEvent: AudioBarEvent) : AudioBarUiEvent(audioBarEvent) {
    data object Cancel : PromptEvent(AudioBarEvent.Cancel)
    data object Acknowledge : PromptEvent(AudioBarEvent.Acknowledge)
  }

  sealed class CommonRecordingEvent(audioBarEvent: AudioBarEvent) :
    AudioBarUiEvent(audioBarEvent) {
    data object Recitation : CommonRecordingEvent(AudioBarEvent.Recitation)
    data object RecitationLongPress : CommonRecordingEvent(AudioBarEvent.RecitationLongPress)
    data object Transcript : CommonRecordingEvent(AudioBarEvent.Transcript)
  }

  sealed class RecitationListeningEvent(event: AudioBarEvent) : AudioBarUiEvent(event) {
    data object HideVerses : RecitationListeningEvent(AudioBarEvent.HideVerses)
  }

  sealed class RecitationPlayingEvent(event: AudioBarEvent) : AudioBarUiEvent(event) {
    data object EndSession : RecitationPlayingEvent(AudioBarEvent.EndSession)
    data object PauseRecitation : RecitationPlayingEvent(AudioBarEvent.PauseRecitation)
  }

  sealed class RecitationStoppedEvent(event: AudioBarEvent) : AudioBarUiEvent(event) {
    data object EndSession : RecitationStoppedEvent(AudioBarEvent.EndSession)
    data object PlayRecitation : RecitationStoppedEvent(AudioBarEvent.PlayRecitation)
  }
}

`

---

### common/ui/core/src/main/java/com/quran/labs/androidquran/common/ui/core/Theme.kt

`kotlin
package com.quran.labs.androidquran.common.ui.core

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val LightColors = lightColorScheme(
  primary = lightPrimary,
  onPrimary = lightOnPrimary,
  primaryContainer = lightPrimaryContainer,
  onPrimaryContainer = lightOnPrimaryContainer,
  secondary = lightSecondary,
  onSecondary = lightOnSecondary,
  secondaryContainer = lightSecondaryContainer,
  onSecondaryContainer = lightOnSecondaryContainer,
  tertiary = lightTertiary,
  onTertiary = lightOnTertiary,
  tertiaryContainer = lightTertiaryContainer,
  onTertiaryContainer = lightOnTertiaryContainer,
  error = lightError,
  errorContainer = lightErrorContainer,
  onError = lightOnError,
  onErrorContainer = lightOnErrorContainer,
  background = lightBackground,
  onBackground = lightOnBackground,
  surface = lightSurface,
  onSurface = lightOnSurface,
  surfaceVariant = lightSurfaceVariant,
  onSurfaceVariant = lightOnSurfaceVariant,
  outline = lightOutline,
  inverseOnSurface = lightInverseOnSurface,
  inverseSurface = lightInverseSurface,
  inversePrimary = lightInversePrimary,
  surfaceTint = lightSurfaceTint,
  surfaceContainerHighest = lightSurfaceColorHighest
)

private val DarkColors = darkColorScheme(
  primary = darkPrimary,
  onPrimary = darkOnPrimary,
  primaryContainer = darkPrimaryContainer,
  onPrimaryContainer = darkOnPrimaryContainer,
  secondary = darkSecondary,
  onSecondary = darkOnSecondary,
  secondaryContainer = darkSecondaryContainer,
  onSecondaryContainer = darkOnSecondaryContainer,
  tertiary = darkTertiary,
  onTertiary = darkOnTertiary,
  tertiaryContainer = darkTertiaryContainer,
  onTertiaryContainer = darkOnTertiaryContainer,
  error = darkError,
  errorContainer = darkErrorContainer,
  onError = darkOnError,
  onErrorContainer = darkOnErrorContainer,
  background = darkBackground,
  onBackground = darkOnBackground,
  surface = darkSurface,
  onSurface = darkOnSurface,
  surfaceVariant = darkSurfaceVariant,
  onSurfaceVariant = darkOnSurfaceVariant,
  outline = darkOutline,
  inverseOnSurface = darkInverseOnSurface,
  inverseSurface = darkInverseSurface,
  inversePrimary = darkInversePrimary,
  surfaceTint = darkSurfaceTint,
  surfaceContainerHighest = darkSurfaceColorHighest
)

private val forceLtr = listOf("huawei", "lenovo", "tecno")

object QuranIcons {
  val ArrowBack: ImageVector get() = com.quran.labs.androidquran.common.ui.core.icons.ArrowBack
  val BookmarkBorder: ImageVector get() = com.quran.labs.androidquran.common.ui.core.icons.BookmarkBorder
  val Chat: ImageVector get() = com.quran.labs.androidquran.common.ui.core.icons.Chat
  val Check: ImageVector get() = com.quran.labs.androidquran.common.ui.core.icons.Check
  val ChevronRight: ImageVector get() = com.quran.labs.androidquran.common.ui.core.icons.ChevronRight
  val Close: ImageVector get() = com.quran.labs.androidquran.common.ui.core.icons.Close
  val ExpandMore: ImageVector get() = com.quran.labs.androidquran.common.ui.core.icons.ExpandMore
  val FastForward: ImageVector get() = com.quran.labs.androidquran.common.ui.core.icons.FastForward
  val FastRewind: ImageVector get() = com.quran.labs.androidquran.common.ui.core.icons.FastRewind
  val Logout: ImageVector get() = com.quran.labs.androidquran.common.ui.core.icons.Logout
  val MenuBook: ImageVector get() = com.quran.labs.androidquran.common.ui.core.icons.MenuBook
  val Mic: ImageVector get() = com.quran.labs.androidquran.common.ui.core.icons.Mic
  val Pause: ImageVector get() = com.quran.labs.androidquran.common.ui.core.icons.Pause
  val Person: ImageVector get() = com.quran.labs.androidquran.common.ui.core.icons.Person
  val PlayArrow: ImageVector get() = com.quran.labs.androidquran.common.ui.core.icons.PlayArrow
  val Repeat: ImageVector get() = com.quran.labs.androidquran.common.ui.core.icons.Repeat
  val Search: ImageVector get() = com.quran.labs.androidquran.common.ui.core.icons.Search
  val Settings: ImageVector get() = com.quran.labs.androidquran.common.ui.core.icons.Settings
  val Speed: ImageVector get() = com.quran.labs.androidquran.common.ui.core.icons.Speed
  val Stop: ImageVector get() = com.quran.labs.androidquran.common.ui.core.icons.Stop
  val Sync: ImageVector get() = com.quran.labs.androidquran.common.ui.core.icons.Sync
  val Add: ImageVector get() = com.quran.labs.androidquran.common.ui.core.icons.Add
  val Delete: ImageVector get() = com.quran.labs.androidquran.common.ui.core.icons.Delete
  val Edit: ImageVector get() = com.quran.labs.androidquran.common.ui.core.icons.Edit
  val List: ImageVector get() = com.quran.labs.androidquran.common.ui.core.icons.List
  val KeyboardArrowUp: ImageVector get() = com.quran.labs.androidquran.common.ui.core.icons.KeyboardArrowUp
  val KeyboardArrowDown: ImageVector get() = com.quran.labs.androidquran.common.ui.core.icons.KeyboardArrowDown
}

@Composable
fun QuranTheme(
  useDarkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit
) {
  val colors = if (!useDarkTheme) {
    LightColors
  } else {
    DarkColors
  }

  val quranColors = if (useDarkTheme) {
    darkQuranColors
  } else {
    lightQuranColors
  }

  // hack workaround for https://issuetracker.google.com/issues/266059178
  // crashes on Lollipop / MR1, mostly on Huawei and Lenovo devices due to
  // Compose in RTL. Force LTR for all Composables on Lollipop to attempt
  // to work around this crash for now.
  val locals =
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M &&
      Build.MANUFACTURER.lowercase() in forceLtr
    ) {
      arrayOf(LocalLayoutDirection provides LayoutDirection.Ltr)
    } else {
      emptyArray()
    }

  CompositionLocalProvider(*locals) {
    CompositionLocalProvider(LocalQuranColors provides quranColors) {
      MaterialTheme(
        colorScheme = colors,
        typography = AppTypography,
        content = content
      )
    }
  }
}

`

---

### app/src/main/java/com/quran/labs/androidquran/service/AudioService.kt

`kotlin
package com.quran.labs.androidquran.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Process
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.media.session.MediaButtonReceiver
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.CacheKeyFactory
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.metadata.MetadataOutput
import androidx.media3.exoplayer.text.TextOutput
import androidx.media3.exoplayer.video.VideoRendererEventListener
import com.quran.data.core.QuranInfo
import com.quran.labs.androidquran.QuranApplication
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.common.audio.model.playback.AudioRequest
import com.quran.labs.androidquran.common.audio.model.playback.AudioStatus
import com.quran.data.model.SuraAyah
import com.quran.labs.androidquran.common.audio.memorization.MemorizationDelayMode
import com.quran.labs.androidquran.common.audio.memorization.MemorizationEngine
import com.quran.labs.androidquran.common.audio.memorization.StepType
import com.quran.labs.androidquran.common.audio.model.playback.PlaybackStatus
import com.quran.labs.androidquran.common.audio.repository.AudioStatusRepository
import com.quran.labs.androidquran.dao.audio.AudioPlaybackInfo
import com.quran.labs.androidquran.dao.audio.SuraTimings
import com.quran.labs.androidquran.data.Constants
import com.quran.labs.androidquran.data.QuranDisplayData
import com.quran.labs.androidquran.data.QuranFileConstants
import com.quran.labs.androidquran.data.TimingRepository
import com.quran.labs.androidquran.extension.requiresBasmallah
import com.quran.labs.androidquran.presenter.audio.service.AudioQueue
import com.quran.labs.androidquran.service.memorization.RecitationRecorder
import com.quran.labs.androidquran.service.util.QuranDownloadNotifier
import com.quran.labs.androidquran.ui.PagerActivity
import com.quran.labs.androidquran.util.AudioUtils
import com.quran.labs.androidquran.util.NotificationChannelUtil.setupNotificationChannel
import com.quran.labs.androidquran.util.QuranSettings
import dev.zacsweers.metro.Inject
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.IOException
import kotlin.math.abs
import androidx.core.net.toUri

/**
 * Service that handles media playback. This is the Service through which we
 * perform all the media handling in our application. It waits for Intents
 * (which come from our main activity, [PagerActivity], which signal
 * the service to perform specific operations: Play, Pause, Rewind, Skip, etc.
 */
class AudioService : Service(), Player.Listener {

  // our exo player
  private var player: ExoPlayer? = null

  // are we playing an override file (basmalah/isti3atha)
  private var playerOverride = false

  // object representing the current playing request
  private var audioRequest: AudioRequest? = null

  // the playback queue
  private var audioQueue: AudioQueue? = null

  // indicates the state our service:
  private enum class State {
    Stopped,  // exo player is stopped and not prepared to play
    Preparing,  // exo player is preparing...
    Playing,  // playback active (exo player ready!). (but the media

    // player may actually be  paused in this state if we don't have audio
    // focus. But we stay in this state so that we know we have to resume
    // playback once we get focus back)
    Paused // playback paused (exo player ready!)
  }

  private var state = State.Stopped


  // are we already in the foreground
  private var isSetupAsForeground = false

  // should we stop (after preparing is done) or not
  private var shouldStop = false

  // The ID we use for the notification (the onscreen alert that appears
  // at the notification area at the top of the screen as an icon -- and
  // as text as well if the user expands the notification area).
  private val NOTIFICATION_ID = Constants.NOTIFICATION_ID_AUDIO_PLAYBACK


  private lateinit var notificationManager: NotificationManager
  private lateinit var mediaSession: MediaSessionCompat

  private lateinit var serviceLooper: Looper
  private lateinit var serviceHandler: ServiceHandler

  private var notificationBuilder: NotificationCompat.Builder? = null
  private var pausedNotificationBuilder: NotificationCompat.Builder? = null
  private var didSetNotificationIconOnNotificationBuilder = false
  private var gaplessSura = 0
  private var notificationColor = 0

  // read by service thread, written on the I/O thread once
  @Volatile
  private var notificationIcon: Bitmap? = null
  private var displayIcon: Bitmap? = null
  private var gaplessSuraData: SuraTimings = SuraTimings.EMPTY
  private var currentWord: Int? = null
  private val compositeDisposable = CompositeDisposable()
  private lateinit var scope: CoroutineScope
  private var pauseCountdownJob: Job? = null
  private var recitationRecorder: RecitationRecorder? = null

  @Inject
  lateinit var quranInfo: QuranInfo

  @Inject
  lateinit var quranDisplayData: QuranDisplayData

  @Inject
  lateinit var audioUtils: AudioUtils

  @Inject
  lateinit var audioStatusRepository: AudioStatusRepository

  @Inject
  lateinit var timingRepository: TimingRepository

  @Inject
  lateinit var quranSettings: QuranSettings

  @Inject
  lateinit var memorizationEngine: MemorizationEngine

  private inner class ServiceHandler(looper: Looper) : Handler(looper) {
    override fun handleMessage(msg: Message) {
      if (msg.what == MSG_INCOMING && msg.obj != null) {
        val intent = msg.obj as Intent
        handleIntent(intent)
      } else if (msg.what == MSG_UPDATE_AUDIO_POS) {
        updateAudioPlayPosition()
      }
    }
  }

  /**
   * Makes sure the ExoPlayer exists and has been reset. This will make
   * the ExoPlayer if needed, or reset the existing player if one
   * already exists.
   */
  @OptIn(UnstableApi::class)
  private fun makeOrResetExoPlayer(): ExoPlayer {
    val currentPlayer = player
    return if (currentPlayer == null) {
      // we only need audio, so allow r8 to remove video renderers
      // via https://developer.android.com/media/media3/exoplayer/shrinking
      val audioOnlyRenderersFactory =
        RenderersFactory {
            handler: Handler,
            _: VideoRendererEventListener,
            audioListener: AudioRendererEventListener,
            _: TextOutput,
            _: MetadataOutput,
          ->
          arrayOf<Renderer>(
            MediaCodecAudioRenderer(applicationContext, MediaCodecSelector.DEFAULT, handler, audioListener)
          )
        }

      val loadControl = DefaultLoadControl.Builder()
        .setBackBuffer(EXO_BACK_BUFFER_MS, true)
        .build()
      val mediaSourceFactory = DefaultMediaSourceFactory(buildDefaultDataSourceFactory())

      val localPlayer = ExoPlayer.Builder(applicationContext, audioOnlyRenderersFactory)
        .setLoadControl(loadControl)
        .setMediaSourceFactory(mediaSourceFactory)
        .setWakeMode(C.WAKE_MODE_NETWORK)
        .build()
      player = localPlayer

      // Set up listener for playback events
      localPlayer.addListener(this)

      // Configure audio attributes for music playback
      val audioAttributes = AudioAttributes.Builder()
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .setUsage(C.USAGE_MEDIA)
        .build()
      localPlayer.setAudioAttributes(audioAttributes, true)

      mediaSession.isActive = true
      localPlayer
    } else {
      Timber.d("resetting player...")
      // ExoPlayer doesn't have reset() - we stop and clear media items instead
      currentPlayer.stop()
      currentPlayer.clearMediaItems()
      currentPlayer
    }
  }

  @OptIn(UnstableApi::class)
  private fun buildDefaultDataSourceFactory(): DataSource.Factory {
    return DefaultDataSource.Factory(
      applicationContext,
      DefaultHttpDataSource.Factory()
    )
  }

  @OptIn(UnstableApi::class)
  private fun buildCachedDataSourceFactory(): DataSource.Factory {
    return CacheDataSource.Factory()
      .setCache(getAudioStreamCache(applicationContext))
      .setCacheKeyFactory(streamCacheKeyFactory())
      .setUpstreamDataSourceFactory(buildDefaultDataSourceFactory())
      .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
  }

  @OptIn(UnstableApi::class)
  private fun streamCacheKeyFactory(): CacheKeyFactory {
    return CacheKeyFactory { dataSpec ->
      val baseKey = dataSpec.key ?: dataSpec.uri.toString()
      "$baseKey|audio_revision=${quranSettings.currentAudioRevision}"
    }
  }

  @OptIn(UnstableApi::class)
  private fun buildMediaSource(uri: Uri): MediaSource {
    val dataSourceFactory = if (uri.scheme == "http" || uri.scheme == "https") {
      buildCachedDataSourceFactory()
    } else {
      buildDefaultDataSourceFactory()
    }
    return DefaultMediaSourceFactory(dataSourceFactory).createMediaSource(MediaItem.fromUri(uri))
  }

  override fun onCreate() {
    Timber.i("debug: Creating service")
    val thread = HandlerThread(
      "AyahAudioService",
      Process.THREAD_PRIORITY_BACKGROUND
    )
    thread.start()

    // Get the HandlerThread's Looper and use it for our Handler
    serviceLooper = thread.looper
    serviceHandler = ServiceHandler(serviceLooper)
    scope = CoroutineScope(serviceHandler.asCoroutineDispatcher() + SupervisorJob())
    recitationRecorder = RecitationRecorder(this)

    val appContext = applicationContext
    (appContext as QuranApplication).applicationComponent.inject(this)
    notificationManager =
      appContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager


    val receiver = ComponentName(this, MediaButtonReceiver::class.java)
    mediaSession = MediaSessionCompat(appContext, "QuranMediaSession", receiver, null)
    mediaSession.setCallback(MediaSessionCallback(), serviceHandler)
    val channelName = getString(R.string.notification_channel_audio)
    setupNotificationChannel(
      notificationManager, NOTIFICATION_CHANNEL_ID, channelName
    )
    notificationColor = ContextCompat.getColor(this, R.color.audio_notification_color)
    try {
      // for Android Wear, use a 1x1 Bitmap with the notification color
      val placeholder = createBitmap(1, 1)
      displayIcon = placeholder
      val canvas = Canvas(placeholder)
      canvas.drawColor(notificationColor)
    } catch (oom: OutOfMemoryError) {
      Timber.e(oom)
    }

    val icon = displayIcon
    // if we couldn't load the 1x1 bitmap, we can't load the image either
    if (icon != null) {
      compositeDisposable.add(
        Maybe.fromCallable { generateNotificationIcon() ?: icon }
          .subscribeOn(Schedulers.io())
          .subscribe { bitmap: Bitmap? -> notificationIcon = bitmap })
    }
  }

  private inner class MediaSessionCallback : MediaSessionCompat.Callback() {
    override fun onPlay() {
      processPlayRequest()
    }

    override fun onSkipToNext() {
      processSkipRequest()
    }

    override fun onSkipToPrevious() {
      processRewindRequest()
    }

    override fun onPause() {
      processPauseRequest()
    }

    override fun onStop() {
      processStopRequest()
    }
  }

  override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
    if (intent == null) {
      // handle a crash that occurs where intent comes in as null
      if (State.Stopped == state) {
        serviceHandler.removeCallbacksAndMessages(null)
        stopSelf()
      }
    } else {
      val action = intent.action
      if (ACTION_PLAYBACK == action || Intent.ACTION_MEDIA_BUTTON == action) {
        // go to the foreground as quickly as possible.
        setUpAsForeground()
      }
      val message = serviceHandler.obtainMessage(MSG_INCOMING, intent)
      serviceHandler.sendMessage(message)
    }
    return START_NOT_STICKY
  }

  private fun handleIntent(intent: Intent) {
    val action = intent.action
    if (ACTION_CONNECT == action) {
      if (State.Stopped == state) {
        processStopRequest(true)
      } else {
        updateAudioPlaybackStatus()
      }
    } else if (ACTION_PLAYBACK == action) {
      val updatedAudioRequest = intent.getParcelableExtra<AudioRequest>(EXTRA_PLAY_INFO)
      if (updatedAudioRequest != null) {
        audioRequest = updatedAudioRequest
        val start = updatedAudioRequest.start
        val basmallah = !updatedAudioRequest.isGapless() && start.requiresBasmallah()
        audioQueue = AudioQueue(
          quranInfo, updatedAudioRequest,
          AudioPlaybackInfo(start, 1, 1, basmallah)
        )
        Timber.d("audio request has changed...")
        player?.stop()
        state = State.Stopped
        Timber.d("stop if playing...")
      }
      processTogglePlaybackRequest()
    } else if (ACTION_PLAY == action) {
      processPlayRequest()
    } else if (ACTION_PAUSE == action) {
      processPauseRequest()
    } else if (ACTION_SKIP == action) {
      processSkipRequest()
    } else if (ACTION_STOP == action) {
      processStopRequest()
    } else if (ACTION_REWIND == action) {
      processRewindRequest()
    } else if (ACTION_UPDATE_SETTINGS == action) {
      val playInfo = intent.getParcelableExtra<AudioRequest>(EXTRA_PLAY_INFO)
      val localAudioQueue = audioQueue
      if (playInfo != null && localAudioQueue != null) {
        audioQueue = localAudioQueue.withUpdatedAudioRequest(playInfo)
        if (playInfo.playbackSpeed != audioRequest?.playbackSpeed) {
          processUpdatePlaybackSpeed(playInfo.playbackSpeed)
          serviceHandler.sendEmptyMessageDelayed(MSG_UPDATE_AUDIO_POS, 200)
        }
        audioRequest = playInfo
        updateAudioPlaybackStatus()
      }
    } else {
      MediaButtonReceiver.handleIntent(mediaSession, intent)
    }
  }

  private suspend fun updateGaplessData(databasePath: String, sura: Int): SuraTimings {
    // note - we rely on the cache from timing repository instead of checking the cache here
    // because we do not have a variable for the database path here.
    val timings = timingRepository.timings(databasePath, sura)
    gaplessSura = sura
    gaplessSuraData = timings
    return timings
  }

  private fun getSeekPosition(isRepeating: Boolean): Long {
    if (audioRequest == null) {
      return -1
    }

    val localAudioQueue = audioQueue ?: return -1
    if (gaplessSura == localAudioQueue.getCurrentSura()) {
      val ayah = localAudioQueue.getCurrentAyah()
      val time = gaplessSuraData.ayahTimings[ayah]
      return if (ayah == 1 && !isRepeating) {
        gaplessSuraData.ayahTimings[0]
      } else time
    }
    return -1
  }

  private fun updateAudioPlayPosition() {
    if (DEBUG_TIMINGS) {
      Timber.d("updateAudioPlayPosition")
    }

    val localAudioQueue = audioQueue
    val localPlayer = player

    val sura = localAudioQueue?.getCurrentSura()
    if (localPlayer != null && localAudioQueue != null && sura == gaplessSura) {
      val ayah = localAudioQueue.getCurrentAyah()
      val maxAyahs = quranInfo.getNumberOfAyahs(sura)

      setState(PlaybackStateCompat.STATE_PLAYING)
      val pos = localPlayer.currentPosition
      val currentAyahTime = gaplessSuraData.ayahTimings[ayah]

      if (DEBUG_TIMINGS) {
        Timber.d(
          "updateAudioPlayPosition: %d:%d, currently at %d vs expected at %d",
          sura, ayah, pos, currentAyahTime
        )
      }

      val updatedAyah = if (currentAyahTime > pos) {
        // the ayah is ahead of the current playback time, so search backwards
        (1 until ayah).findLast { gaplessSuraData.ayahTimings[it] <= pos } ?: 1
      } else {
        // the audio position is after this ayah, find out which ayah we're at
        ((ayah + 1)..maxAyahs).find { gaplessSuraData.ayahTimings[it] > pos }?.minus(1) ?: maxAyahs
      }

      if (DEBUG_TIMINGS) {
        Timber.d(
          "updateAudioPlayPosition: %d:%d, decided ayah should be: %d",
          sura, ayah, updatedAyah
        )
      }

      if (updatedAyah != ayah) {
        val ayahTime = gaplessSuraData.ayahTimings[ayah]
        if (abs(pos - ayahTime) < 150) {
          // shouldn't change ayahs if the delta is just 150ms...
          serviceHandler.sendEmptyMessageDelayed(MSG_UPDATE_AUDIO_POS, 150)
          return
        }

        if (memorizationEngine.state.value.isActive) {
          val currentAyah = SuraAyah(sura, ayah)
          val nextAyah = memorizationEngine.onAyahPlaybackCompleted(currentAyah)
          if (nextAyah != null) {
            serviceHandler.removeMessages(MSG_UPDATE_AUDIO_POS)
            currentWord = null
            localAudioQueue.playAt(nextAyah.sura, nextAyah.ayah, true)
            if (nextAyah.sura == sura) {
              val seekPos = getSeekPosition(false)
              if (seekPos > -1) {
                localPlayer.seekTo(seekPos)
              }
            } else {
              playAudio(true)
            }
          } else {
            processStopRequest(true)
          }
          return
        }

        val success = localAudioQueue.playAt(sura, updatedAyah, false)
        val nextSura = localAudioQueue.getCurrentSura()
        val nextAyah = localAudioQueue.getCurrentAyah()

        if (!success) {
          processStopRequest()
          return
        } else if (nextSura != sura || nextAyah != updatedAyah) {
          // remove any messages currently in the queue
          serviceHandler.removeMessages(MSG_UPDATE_AUDIO_POS)
          currentWord = null

          // if the ayah hasn't changed, we're repeating the ayah,
          // otherwise, we're repeating a range. this variable is
          // what determines whether or not we replay the basmallah.
          val ayahRepeat = ayah == nextAyah && sura == nextSura
          if (ayahRepeat) {
            // jump back to the ayah we should repeat and play it
            val seekPos = getSeekPosition(true)
            localPlayer.seekTo(seekPos)
          } else {
            // we're repeating into a different sura
            val flag = sura != localAudioQueue.getCurrentSura()
            playAudio(flag)
          }
          return
        }

        // moved on to next ayah
        updateNotification()
      } else {
        // if we have end of sura info and we bypassed end of sura
        // line, switch the sura.
        val ayahTime = gaplessSuraData.ayahTimings[999]
        if (ayahTime in 1..pos) {
          val success = localAudioQueue.playAt(sura + 1, 1, false)
          if (success && localAudioQueue.getCurrentSura() == sura) {
            // remove any messages currently in the queue
            serviceHandler.removeMessages(MSG_UPDATE_AUDIO_POS)

            // jump back to the ayah we should repeat and play it
            val seekPos = getSeekPosition(false)
            localPlayer.seekTo(seekPos)
          } else if (!success) {
            processStopRequest()
          } else {
            playAudio(true)
          }
          return
        }
      }

      // handle word by word tracking if enabled
      val localAudioRequest = audioRequest
      var nextUpdateDelay: Long? = null

      if (localAudioRequest?.wordHighlighting == true) {
        val wordTimings = gaplessSuraData.wordTimings[updatedAyah]
        if (wordTimings != null && wordTimings.isNotEmpty()) {
          // find the current word based on position
          val updatedWordIndex = wordTimings.indexOfLast { it.startTime <= pos }
          val nextWordIndex = if (updatedWordIndex >= 0) {
            updatedWordIndex + 1
          } else {
            0
          }

          if (nextWordIndex < wordTimings.size) {
            val timeDelta = wordTimings[nextWordIndex].startTime - localPlayer.currentPosition
            val t = timeDelta.coerceIn(50, 10000)
            val tAccountingForSpeed = t / localAudioRequest.playbackSpeed
            nextUpdateDelay = tAccountingForSpeed.toLong()
          }

          currentWord = if (updatedWordIndex >= 0) {
            wordTimings[updatedWordIndex].word
          } else {
            null
          }
        } else {
          // no word timings available, clear current word
          currentWord = null
        }
      } else {
        // word highlighting disabled, clear current word
        currentWord = null
      }

      // Notify once about the current state
      notifyAyahChanged()

      // schedule next the update
      if (nextUpdateDelay != null) {
        serviceHandler.sendEmptyMessageDelayed(MSG_UPDATE_AUDIO_POS, nextUpdateDelay)
      } else if (maxAyahs >= updatedAyah + 1) {
        val timeDelta = gaplessSuraData.ayahTimings[updatedAyah + 1] - localPlayer.currentPosition
        val t = timeDelta.coerceIn(100, 10000)
        val tAccountingForSpeed = t / (audioRequest?.playbackSpeed ?: 1f)
        if (DEBUG_TIMINGS) {
          Timber.d(
            "updateAudioPlayPosition before: %d, after %f, speed: %f",
            t, tAccountingForSpeed, audioRequest?.playbackSpeed
          )
        }
        serviceHandler.sendEmptyMessageDelayed(MSG_UPDATE_AUDIO_POS, tAccountingForSpeed.toLong())
      } else if (maxAyahs == updatedAyah) {
        serviceHandler.sendEmptyMessageDelayed(MSG_UPDATE_AUDIO_POS, 150)
      }
      // if we're on the last ayah, don't do anything - let the file
      // complete on its own to avoid getCurrentPosition() bugs.
    }
  }

  private fun processTogglePlaybackRequest() {
    if (State.Paused == state || State.Stopped == state) {
      processPlayRequest()
    } else {
      processPauseRequest()
    }
  }

  private fun processPlayRequest() {
    val localAudioRequest = audioRequest
    val localAudioQueue = audioQueue
    if (localAudioRequest == null || localAudioQueue == null) {
      // no audio request, what can we do?
      relaxResources(releaseExoPlayer = true, stopForeground = true)
      return
    }
    if (memorizationEngine.state.value.isActive) {
      memorizationEngine.resume()
    }
    // actually play the file
    if (State.Stopped == state) {
      // If we're stopped, just go ahead to the next file and start playing
      playAudio(localAudioQueue.getCurrentSura() == 9 && localAudioQueue.getCurrentAyah() == 1)
    } else if (State.Paused == state) {
      // If we're paused, just continue playback and restore the
      // 'foreground service' state.
      state = State.Playing
      if (!isSetupAsForeground) {
        setUpAsForeground()
      }
      configAndStartExoPlayer()
      updateAudioPlaybackStatus()
    }
  }

  private fun processPauseRequest() {
    if (memorizationEngine.state.value.isActive) {
      if (memorizationEngine.state.value.isRecordingRecitation) {
        recitationRecorder?.cancelRecording()
        memorizationEngine.setRecordingRecitation(false)
      }
      memorizationEngine.pause()
    }
    if (State.Playing == state) {
      // Pause exo player and cancel the 'foreground service' state.
      state = State.Paused
      serviceHandler.removeMessages(MSG_UPDATE_AUDIO_POS)
      player?.pause()
      setState(PlaybackStateCompat.STATE_PAUSED)
      // on jellybean and above, stay in the foreground and
      // update the notification.
      relaxResources(releaseExoPlayer = false, stopForeground = false)
      pauseNotification()
      updateAudioPlaybackStatus()
    } else if (State.Stopped == state) {
      // if we get a pause while we're already stopped, it means we likely woke up because
      // of AudioIntentReceiver, so just stop in this case.
      setState(PlaybackStateCompat.STATE_STOPPED)
      updateAudioPlaybackStatus()
      stopSelf()
    }
  }

  private fun processRewindRequest() {
    if (memorizationEngine.state.value.isActive) {
      memorizationEngine.previousStep()
      val prevAyah = memorizationEngine.state.value.currentPlayingAyah
      val localAudioQueue = audioQueue
      if (prevAyah != null && localAudioQueue != null) {
        handleNextMemorizationAyah(prevAyah, localAudioQueue)
      }
      return
    }
    if (State.Playing == state || State.Paused == state) {
      setState(PlaybackStateCompat.STATE_REWINDING)
      val localPlayer = player ?: return
      val localAudioQueue = audioQueue ?: return
      val localAudioRequest = audioRequest ?: return

      var seekTo = 0L
      var pos = localPlayer.currentPosition
      if (localAudioRequest.isGapless()) {
        seekTo = getSeekPosition(true)
        pos -= seekTo
      }

      if (pos > 1500 && !playerOverride) {
        localPlayer.seekTo(seekTo)
        state = State.Playing // in case we were paused
      } else {
        val sura = localAudioQueue.getCurrentSura()
        localAudioQueue.playPreviousAyah(true)
        if (localAudioRequest.isGapless() && sura == localAudioQueue.getCurrentSura()) {
          val timing = getSeekPosition(true)
          if (timing > -1) {
            localPlayer.seekTo(timing)
          }
          updateNotification()
          state = State.Playing // in case we were paused
          return
        }
        playAudio()
      }
    }
  }

  private fun processUpdatePlaybackSpeed(speed: Float) {
    val shouldUpdatePlaybackSpeed = State.Playing === state || State.Preparing === state
    if (shouldUpdatePlaybackSpeed) {
      player?.setPlaybackSpeed(speed)
    }
  }

  private fun processSkipRequest() {
    if (audioRequest == null) {
      return
    }
    if (memorizationEngine.state.value.isActive) {
      memorizationEngine.skipCurrentStep()
      val nextAyah = memorizationEngine.state.value.currentPlayingAyah
      val localAudioQueue = audioQueue
      if (nextAyah != null && localAudioQueue != null) {
        handleNextMemorizationAyah(nextAyah, localAudioQueue)
      } else {
        processStopRequest(true)
      }
      return
    }
    if (State.Playing == state || State.Paused == state) {
      setState(PlaybackStateCompat.STATE_SKIPPING_TO_NEXT)
      if (playerOverride) {
        playAudio(false)
      } else {
        val localPlayer = player ?: return
        val localAudioQueue = audioQueue ?: return
        val localAudioRequest = audioRequest ?: return

        val sura = localAudioQueue.getCurrentSura()
        localAudioQueue.playNextAyah(true)
        if (localAudioRequest.isGapless() && sura == localAudioQueue.getCurrentSura()) {
          val timing = getSeekPosition(false)
          if (timing > -1) {
            localPlayer.seekTo(timing)
            state = State.Playing // in case we were paused
          }
          updateNotification()
          return
        }
        playAudio()
      }
    }
  }

  private fun processStopRequest(force: Boolean = false) {
    pauseCountdownJob?.cancel()
    pauseCountdownJob = null
    recitationRecorder?.cancelRecording()
    recitationRecorder?.stopPlayback()
    if (memorizationEngine.state.value.isActive) {
      memorizationEngine.setRecordingRecitation(false)
      memorizationEngine.updateLastRecitationPath(null)
      memorizationEngine.stopMemorization()
    }
    setState(PlaybackStateCompat.STATE_STOPPED)
    serviceHandler.removeMessages(MSG_UPDATE_AUDIO_POS)
    currentWord = null
    if (State.Preparing == state) {
      shouldStop = true
      relaxResources(releaseExoPlayer = false, stopForeground = true)
    }
    if (force || State.Stopped != state) {
      state = State.Stopped
      updateAudioPlaybackStatus()

      // let go of all resources...
      relaxResources(releaseExoPlayer = true, stopForeground = true)

      // service is no longer necessary. Will be started again if needed.
      serviceHandler.removeCallbacksAndMessages(null)
      stopSelf()
    }
  }

  private fun notifyAyahChanged() {
    val localAudioRequest = audioRequest ?: return
    updateAudioPlaybackStatus()

    val metadataBuilder = MediaMetadataCompat.Builder()
      .putString(MediaMetadataCompat.METADATA_KEY_TITLE, getTitle())
      .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, localAudioRequest.qari.name)
    val localPlayer = player
    if (localPlayer?.isPlaying == true) {
      metadataBuilder.putLong(
        MediaMetadataCompat.METADATA_KEY_DURATION,
        localPlayer.duration
      )
    }

    if (displayIcon != null) {
      metadataBuilder.putBitmap(
        MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON,
        displayIcon
      )
    }
    mediaSession.setMetadata(metadataBuilder.build())
  }

  private fun updateAudioPlaybackStatus() {
    val audioStatus = when (state) {
      State.Stopped -> AudioStatus.Stopped
      State.Playing, State.Preparing, State.Paused -> {
        val localAudioQueue = audioQueue ?: return
        val localAudioRequest = audioRequest ?: return

        AudioStatus.Playback(
          localAudioQueue.getCurrentPlaybackAyah(),
          localAudioRequest,
          state.asPlayingPlaybackStatus(),
          currentWord
        )
      }
    }
    audioStatusRepository.updateAyahPlayback(audioStatus)
  }

  private fun State.asPlayingPlaybackStatus(): PlaybackStatus {
    return when (this) {
      State.Playing -> PlaybackStatus.PLAYING
      State.Preparing -> PlaybackStatus.PREPARING
      State.Paused -> PlaybackStatus.PAUSED
      else -> throw IllegalStateException("State $this is not a playing state")
    }
  }

  /**
   * Releases resources used by the service for playback. This includes the
   * "foreground service" status and notification, the wake locks and
   * possibly the ExoPlayer.
   *
   * @param releaseExoPlayer Indicates whether the ExoPlayer should also
   * be released or not
   */
  private fun relaxResources(releaseExoPlayer: Boolean, stopForeground: Boolean) {
    if (stopForeground) {
      // stop being a foreground service
      stopForeground(true)
      isSetupAsForeground = false
    }

    // stop and release the ExoPlayer, if it's available
    val localPlayer = player
    if (releaseExoPlayer && localPlayer != null) {
      try {
        localPlayer.stop()
        localPlayer.release()
      } catch (_: IllegalStateException) {
        // nothing to do here  ¯\_(ツ)_/¯
      }
      player = null
      mediaSession.isActive = false
    }

  }

  /**
   * Configures and starts playback after the player is ready.
   */
  private fun configAndStartExoPlayer() {
    Timber.d("configAndStartExoPlayer()")

    val player = player ?: return

    if (shouldStop) {
      processStopRequest()
      shouldStop = false
      return
    }

    if (!player.isPlaying) {
      player.play()
    }
    state = State.Playing
    updateAudioPlaybackStatus()

    audioRequest?.playbackSpeed?.let { speed ->
      processUpdatePlaybackSpeed(speed)
    }

    if (audioRequest?.isGapless() == true && !playerOverride) {
      Timber.d("configAndStartExoPlayer: restarting position updates")
      serviceHandler.sendEmptyMessageDelayed(MSG_UPDATE_AUDIO_POS, 200)
    }
  }


  private fun getTitle(): String {
    val audioQueue = audioQueue
    return if (audioQueue == null) {
      ""
    } else {
      quranDisplayData.getSuraAyahString(
        this,
        audioQueue.getCurrentSura(),
        audioQueue.getCurrentAyah()
      )
    }
  }

  /**
   * Starts playing the next file.
   */
  private fun playAudio(playRepeatSeparator: Boolean = false) {
    if (!isSetupAsForeground) {
      setUpAsForeground()
    }
    state = State.Stopped
    relaxResources(releaseExoPlayer = false, stopForeground = false)
    playerOverride = false
    currentWord = null

    val localAudioQueue = audioQueue
    val localAudioRequest = audioRequest
    if (localAudioQueue != null && localAudioRequest != null) {
      val dbPath = localAudioRequest.audioPathInfo.gaplessDatabase
      if (localAudioRequest.isGapless() && dbPath != null) {
        scope.launch {
          val timings = updateGaplessData(dbPath, localAudioQueue.getCurrentSura())
          playAudio(playRepeatSeparator, timings)
        }
      } else {
        playAudio(playRepeatSeparator, null)
      }
    }
  }

  @OptIn(UnstableApi::class)
  private fun playAudio(playRepeatSeparator: Boolean, timings: SuraTimings?) {
    try {
      val localAudioQueue = audioQueue
      val localAudioRequest = audioRequest

      val url = audioQueue?.getUrl()
      if (localAudioRequest == null || localAudioQueue == null || url == null) {
        processStopRequest(true) // stop everything!
        return
      }

      val isStreaming = url.startsWith("http:") || url.startsWith("https:")
      if (!isStreaming) {
        val f = File(url)
        if (!f.exists()) {
          processStopRequest(true)
          return
        }
      }
      var overrideResource = 0
      if (playRepeatSeparator) {
        val sura = localAudioQueue.getCurrentSura()
        val ayah = localAudioQueue.getCurrentAyah()
        if (sura != 9 && ayah > 1) {
          overrideResource = R.raw.bismillah
        } else if (sura == 9 &&
          (ayah > 1 || localAudioRequest.needsIsti3athaAudio())
        ) {
          overrideResource = R.raw.isti3atha
        }
        // otherwise, ayah of 1 will automatically play the file's basmala
      }
      Timber.d("okay, we are preparing to play - streaming is: %b", isStreaming)

      val localPlayer = makeOrResetExoPlayer()
      setState(PlaybackStateCompat.STATE_CONNECTING)
      try {
        val mediaUri = if (overrideResource != 0) {
          // For raw resources, we need to create a URI pointing to the resource
          val resourceUri = "android.resource://$packageName/$overrideResource"
          playerOverride = true
          resourceUri.toUri()
        } else {
          // For regular URLs (both local files and streaming URLs)
          overrideResource = 0
          url.toUri()
        }
        val mediaSource = buildMediaSource(mediaUri)

        val potentialTiming = if (timings != null && overrideResource == 0) {
          getSeekPosition(false)
        } else {
          -1
        }
        if (potentialTiming > -1) {
          localPlayer.setMediaSource(mediaSource, potentialTiming)
        } else {
          localPlayer.setMediaSource(mediaSource)
        }
        localPlayer.prepare()
        Timber.d("prepared media item: $overrideResource, $url")
      } catch (e: IllegalStateException) {
        Timber.e("IllegalStateException while setting media item: %s", e.message)
        // ExoPlayer error handling will call onPlayerError, so just stop here
        processStopRequest(true)
        return
      }

    } catch (ex: IOException) {
      Timber.e("IOException playing file: %s", ex.message)
      ex.printStackTrace()
    }
  }

  private fun setState(state: Int) {
    var position: Long = 0
    val localPlayer = player
    if (localPlayer != null && localPlayer.isPlaying) {
      position = localPlayer.currentPosition
    }
    val builder = PlaybackStateCompat.Builder()
    builder.setState(state, position, 1.0f)

    val actions = when (state) {
      PlaybackStateCompat.STATE_PLAYING -> {
        PlaybackStateCompat.ACTION_PAUSE or
            PlaybackStateCompat.ACTION_STOP or
            PlaybackStateCompat.ACTION_REWIND or
            PlaybackStateCompat.ACTION_FAST_FORWARD or
            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
            PlaybackStateCompat.ACTION_SKIP_TO_NEXT
      }
      PlaybackStateCompat.STATE_PAUSED -> {
        PlaybackStateCompat.ACTION_PLAY or
            PlaybackStateCompat.ACTION_STOP or
            PlaybackStateCompat.ACTION_REWIND or
            PlaybackStateCompat.ACTION_FAST_FORWARD or
            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
            PlaybackStateCompat.ACTION_SKIP_TO_NEXT
      }
      PlaybackStateCompat.STATE_STOPPED -> {
        PlaybackStateCompat.ACTION_PLAY
      }
      PlaybackStateCompat.STATE_CONNECTING -> {
        PlaybackStateCompat.ACTION_STOP
      }
      PlaybackStateCompat.STATE_REWINDING -> {
        PlaybackStateCompat.ACTION_STOP or
            PlaybackStateCompat.ACTION_REWIND or
            PlaybackStateCompat.ACTION_FAST_FORWARD or
            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
            PlaybackStateCompat.ACTION_SKIP_TO_NEXT
      }
      PlaybackStateCompat.STATE_SKIPPING_TO_NEXT -> {
        PlaybackStateCompat.ACTION_STOP or
            PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS or
            PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
            PlaybackStateCompat.ACTION_FAST_FORWARD or
            PlaybackStateCompat.ACTION_REWIND
      }
      else -> { PlaybackStateCompat.ACTION_STOP }
    }
    builder.setActions(actions)
    mediaSession.setPlaybackState(builder.build())
  }

  override fun onPlaybackStateChanged(playbackState: Int) {
    when (playbackState) {
      Player.STATE_READY -> {
        Timber.d("Player ready to play - will prepare: ${state == State.Preparing}")
        if (state == State.Preparing) {
          onPlayerReady()
        }
      }
      Player.STATE_ENDED -> {
        Timber.d("Player ended")
        onPlayerCompleted()
      }
      Player.STATE_BUFFERING -> {
        Timber.d("Player buffering")
        onPlayerBuffering()
      }
      Player.STATE_IDLE -> {
        Timber.d("Player idle")
        if (state != State.Stopped) {
          state = State.Stopped
          updateAudioPlaybackStatus()
        }
      }
    }
  }

  // Called when seeking is complete
  override fun onPositionDiscontinuity(
    oldPosition: Player.PositionInfo,
    newPosition: Player.PositionInfo,
    reason: Int
  ) {
    Timber.d("Player onPositionDiscontinuity: reason=%d", reason)
    if (reason == Player.DISCONTINUITY_REASON_SEEK) {
      onSeekCompleted()
    }
  }

  // Called when the player starts or stops playing
  override fun onIsPlayingChanged(isPlaying: Boolean) {
    // Ensure UI stays in sync with actual ExoPlayer state changes
    // Only update state if it's a legitimate transition
    if (isPlaying && state == State.Paused) {
      state = State.Playing
      updateAudioPlaybackStatus()
    } else if (!isPlaying && state == State.Playing) {
      // Handle automatic pause (audio focus loss, etc.)
      state = State.Paused
      updateAudioPlaybackStatus()
    }
  }

  private fun onSeekCompleted() {
    val player = player ?: return
    Timber.d("seek complete! position: %d", player.currentPosition)

    state = State.Playing
    if (!player.isPlaying) {
      player.play()
    }
    updateAudioPlaybackStatus()
    Timber.d("onSeekComplete: restarting position updates")
    serviceHandler.sendEmptyMessageDelayed(MSG_UPDATE_AUDIO_POS, 200)
  }

  private fun onPlayerBuffering() {
    Timber.d("Player buffering...")
    state = State.Preparing

    val audioRequest = audioRequest
    // only actually update the status if we are streaming
    if (audioRequest == null || audioRequest.audioPathInfo.urlFormat.startsWith("http")) {
      updateAudioPlaybackStatus()
    }
  }

  private fun onPlayerCompleted() {
    Timber.d("onPlayerCompleted")
    // The exo player finished playing the current file, so
    // we go ahead and start the next.
    if (playerOverride) {
      playAudio(false)
    } else {
      val localAudioQueue = audioQueue ?: return
      val localAudioRequest = audioRequest ?: return

      if (memorizationEngine.state.value.isActive) {
        val currentAyah = SuraAyah(localAudioQueue.getCurrentSura(), localAudioQueue.getCurrentAyah())
        val nextAyah = memorizationEngine.onAyahPlaybackCompleted(currentAyah)
        if (nextAyah != null) {
          handleNextMemorizationAyah(nextAyah, localAudioQueue)
        } else {
          processStopRequest(true)
        }
        return
      }

      val beforeSura = localAudioQueue.getCurrentSura()
      if (localAudioQueue.playNextAyah(false)) {
        if (localAudioRequest.isGapless() && beforeSura == localAudioQueue.getCurrentSura()
        ) {
          // we're actually repeating, but we reached the end of the file before we could
          // seek to the proper place. so let's seek anyway. route through getSeekPosition
          // so that when we loop back to ayah 1 of a surah, the basmallah (ayahTimings[0])
          // is played instead of skipping straight to ayah 1 (ayahTimings[1]).
          val seekPosition = getSeekPosition(false)
          if (seekPosition > -1) {
            player?.seekTo(seekPosition)
          }
        } else {
          // we actually switched to a different ayah - so if the
          // sura changed, then play the basmala if the ayah is
          // not the first one (or if we're in sura tawba).
          val flag = beforeSura != localAudioQueue.getCurrentSura()
          playAudio(flag)
        }
      } else {
        processStopRequest(true)
      }
    }
  }

  private fun handleNextMemorizationAyah(nextAyah: SuraAyah, localAudioQueue: AudioQueue) {
    pauseCountdownJob?.cancel()
    pauseCountdownJob = null

    val currentStep = memorizationEngine.state.value.currentStep
    if (currentStep?.stepType == StepType.PAUSE_FOR_RECITATION) {
      player?.pause()
      serviceHandler.removeMessages(MSG_UPDATE_AUDIO_POS)
      state = State.Paused
      setState(PlaybackStateCompat.STATE_PAUSED)
      updateAudioPlaybackStatus()
      updateNotification()

      val delaySeconds: Int = when (val delay = memorizationEngine.state.value.config?.delayMode) {
        is MemorizationDelayMode.FixedSeconds -> delay.seconds
        is MemorizationDelayMode.MatchAyahDuration -> {
          val localAudioRequest = audioRequest
          if (localAudioRequest?.isGapless() == true && gaplessSura == nextAyah.sura) {
            val start = gaplessSuraData.ayahTimings.get(nextAyah.ayah, -1L)
            val end = gaplessSuraData.ayahTimings.get(nextAyah.ayah + 1, -1L)
            if (start > -1L && end > start) {
              val durationMs = end - start
              (durationMs / 1000L).toInt().coerceAtLeast(1)
            } else {
              3
            }
          } else {
            val durationMs = player?.duration ?: 3000L
            (durationMs / 1000L).toInt().coerceAtLeast(1)
          }
        }
        else -> 3
      }

      val shouldRecord = memorizationEngine.state.value.config?.recordRecitation == true
      if (shouldRecord) {
        val tempFile = File(cacheDir, "memorization_recitation_temp.m4a")
        val started = recitationRecorder?.startRecording(tempFile) == true
        memorizationEngine.setRecordingRecitation(started)
      }

      pauseCountdownJob = scope.launch {
        for (remaining in delaySeconds downTo 1) {
          memorizationEngine.updatePauseRemaining(remaining)
          delay(1000L)
        }
        memorizationEngine.updatePauseRemaining(0)

        if (shouldRecord) {
          val recordedPath = recitationRecorder?.stopRecording()
          memorizationEngine.setRecordingRecitation(false)
          memorizationEngine.updateLastRecitationPath(recordedPath)
        }

        withContext(Dispatchers.Main) {
          if (memorizationEngine.state.value.isActive && !memorizationEngine.state.value.isPaused) {
            val next = memorizationEngine.onAyahPlaybackCompleted(nextAyah)
            if (next != null) {
              handleNextMemorizationAyah(next, localAudioQueue)
            } else {
              processStopRequest(true)
            }
          }
        }
      }
      return
    }

    memorizationEngine.updatePauseRemaining(0)
    val localAudioRequest = audioRequest ?: return
    val beforeSura = localAudioQueue.getCurrentSura()
    localAudioQueue.playAt(nextAyah.sura, nextAyah.ayah, true)

    if (localAudioRequest.isGapless() && gaplessSura == nextAyah.sura && beforeSura == nextAyah.sura) {
      val timing = gaplessSuraData.ayahTimings.get(nextAyah.ayah, -1L)
      if (timing > -1L) {
        player?.seekTo(timing)
        state = State.Playing
        configAndStartExoPlayer()
        notifyAyahChanged()
        updateNotification()
        return
      }
    }

    val flag = beforeSura != localAudioQueue.getCurrentSura()
    playAudio(flag)
  }

  private fun onPlayerReady() {
    Timber.d("okay, prepared!")

    // The exo player is done preparing. That means we can start playing!
    if (shouldStop) {
      processStopRequest()
      shouldStop = false
      return
    }

    // if gapless and sura changed, get the new data
    val localAudioRequest = audioRequest ?: return
    if (playerOverride || !localAudioRequest.isGapless()) {
      notifyAyahChanged()
    }
    updateNotification()
    configAndStartExoPlayer()
  }

  /** Updates the notification.  */
  private fun updateNotification() {
    notificationBuilder?.setContentText(getTitle())
    if (!didSetNotificationIconOnNotificationBuilder && notificationIcon != null) {
      notificationBuilder?.setLargeIcon(notificationIcon)
      didSetNotificationIconOnNotificationBuilder = true
    }
    notificationManager.notify(NOTIFICATION_ID, notificationBuilder?.build())
  }

  private fun pauseNotification() {
    val builder = getPausedNotificationBuilder()
    notificationManager.notify(NOTIFICATION_ID, builder.build())
  }

  /**
   * Generate the notification icon
   * This might return null if the icon fails to initialize. This method should
   * be called from a separate background thread (other than the one the service
   * is running on).
   *
   * @return a bitmap of the notification icon
   */
  private fun generateNotificationIcon(): Bitmap? {
    val appContext = applicationContext
    return try {
      val resources = appContext.resources
      val logo = BitmapFactory.decodeResource(resources, QuranFileConstants.ICON_RESOURCE_ID)
      val iconWidth = logo.width
      val iconHeight = logo.height
      val cd = ContextCompat.getColor(
        appContext,
        R.color.audio_notification_background_color
      ).toDrawable()
      val bitmap = createBitmap(iconWidth * 2, iconHeight * 2)
      val canvas = Canvas(bitmap)
      cd.setBounds(0, 0, canvas.width, canvas.height)
      cd.draw(canvas)
      canvas.drawBitmap(
        logo,
        (iconWidth / 2.0).toInt().toFloat(),
        (iconHeight / 2.0).toInt().toFloat(),
        null
      )
      bitmap
    } catch (oomError: OutOfMemoryError) {
      // if this happens, we need to handle it gracefully, since it's not crash worthy.
      Timber.e(oomError)
      null
    }
  }

  /**
   * Configures service as a foreground service. A foreground service
   * is a service that's doing something the user is actively aware of
   * (such as playing music), and must appear to the user as a notification.
   * That's why we create the notification here.
   */
  private fun setUpAsForeground() {
    // clear the "downloading complete" notification (if it exists)
    notificationManager.cancel(QuranDownloadNotifier.DOWNLOADING_COMPLETE_NOTIFICATION)
    val appContext = applicationContext
    val pi = notificationPendingIntent

    val mutabilityFlag = PendingIntent.FLAG_IMMUTABLE
    val previousIntent = PendingIntent.getService(
      appContext, REQUEST_CODE_PREVIOUS, audioUtils.getAudioIntent(this, ACTION_REWIND),
      PendingIntent.FLAG_UPDATE_CURRENT or mutabilityFlag
    )
    val nextIntent = PendingIntent.getService(
      appContext, REQUEST_CODE_SKIP, audioUtils.getAudioIntent(this, ACTION_SKIP),
      PendingIntent.FLAG_UPDATE_CURRENT or mutabilityFlag
    )
    val pauseIntent = PendingIntent.getService(
      appContext, REQUEST_CODE_PAUSE, audioUtils.getAudioIntent(this, ACTION_PAUSE),
      PendingIntent.FLAG_UPDATE_CURRENT or mutabilityFlag
    )
    val audioTitle = getTitle()
    val currentNotificationBuilder = notificationBuilder
    val updatedBuilder = if (currentNotificationBuilder == null) {
      val icon = notificationIcon
      val builder = NotificationCompat.Builder(appContext, NOTIFICATION_CHANNEL_ID)
      builder
        .setSmallIcon(R.drawable.ic_notification)
        .setColor(notificationColor)
        .setOngoing(true)
        .setContentTitle(getString(R.string.app_name))
        .setContentIntent(pi)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .addAction(R.drawable.ic_previous, getString(R.string.previous), previousIntent)
        .addAction(R.drawable.ic_pause, getString(R.string.pause), pauseIntent)
        .addAction(R.drawable.ic_next, getString(R.string.next), nextIntent)
        .setShowWhen(false)
        .setWhen(0) // older platforms seem to ignore setShowWhen(false)
        .setLargeIcon(icon)
        .setStyle(
          androidx.media.app.NotificationCompat.MediaStyle()
            .setShowActionsInCompactView(0, 1, 2)
            .setMediaSession(mediaSession.sessionToken)
        )
      didSetNotificationIconOnNotificationBuilder = icon != null
      notificationBuilder = builder
      builder
    } else {
      currentNotificationBuilder
    }

    updatedBuilder.setTicker(audioTitle)
    updatedBuilder.setContentText(audioTitle)
    startForeground(NOTIFICATION_ID, updatedBuilder.build())
    isSetupAsForeground = true
  }

  private fun getPausedNotificationBuilder(): NotificationCompat.Builder {
    val appContext = applicationContext

    val mutabilityFlag = PendingIntent.FLAG_IMMUTABLE
    val resumeIntent = PendingIntent.getService(
      appContext, REQUEST_CODE_RESUME, audioUtils.getAudioIntent(this, ACTION_PLAYBACK),
      PendingIntent.FLAG_UPDATE_CURRENT or mutabilityFlag
    )
    val stopIntent = PendingIntent.getService(
      appContext, REQUEST_CODE_STOP, audioUtils.getAudioIntent(this, ACTION_STOP),
      PendingIntent.FLAG_UPDATE_CURRENT or mutabilityFlag
    )
    val pi = notificationPendingIntent
    val localPausedNotificationBuilder = pausedNotificationBuilder
    val pauseBuilder = if (localPausedNotificationBuilder == null) {
      val builder =
        NotificationCompat.Builder(appContext, NOTIFICATION_CHANNEL_ID)
      builder
        .setSmallIcon(R.drawable.ic_notification)
        .setColor(notificationColor)
        .setOngoing(true)
        .setContentTitle(getString(R.string.app_name))
        .setContentIntent(pi)
        .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        .addAction(com.quran.labs.androidquran.common.toolbar.R.drawable.ic_play, getString(R.string.play), resumeIntent)
        .addAction(R.drawable.ic_stop, getString(R.string.stop), stopIntent)
        .setShowWhen(false)
        .setWhen(0)
        .setLargeIcon(notificationIcon)
        .setStyle(
          androidx.media.app.NotificationCompat.MediaStyle()
            .setShowActionsInCompactView(0, 1)
            .setMediaSession(mediaSession.sessionToken)
        )
      pausedNotificationBuilder = builder
      builder
    } else {
      localPausedNotificationBuilder
    }

    pauseBuilder.setContentText(getTitle())
    return pauseBuilder
  }

  private val notificationPendingIntent: PendingIntent
    get() {
      val appContext = applicationContext
      val mutabilityFlag = PendingIntent.FLAG_IMMUTABLE

      return PendingIntent.getActivity(
        appContext, REQUEST_CODE_MAIN, Intent(appContext, PagerActivity::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or mutabilityFlag
      )
    }

  /**
   * Called when there's an error playing media. When this happens, ExoPlayer
   * goes to the Error state. We warn the user about the error and
   * reset the exo player.
   */
  override fun onPlayerError(error: PlaybackException) {
    Timber.e("ExoPlayer Error: %s", error.toString())
    state = State.Stopped
    updateAudioPlaybackStatus()
    relaxResources(releaseExoPlayer = true, stopForeground = true)
  }

  override fun onDestroy() {
    compositeDisposable.clear()
    // Service is being killed, so make sure we release our resources
    serviceHandler.removeCallbacksAndMessages(null)
    serviceLooper.quitSafely()
    state = State.Stopped
    relaxResources(true, true)
    mediaSession.release()
    timingRepository.clear()
    recitationRecorder?.cancelRecording()
    recitationRecorder?.stopPlayback()
    recitationRecorder = null
    scope.cancel()
    super.onDestroy()
  }

  override fun onBind(arg0: Intent): IBinder? {
    return null
  }


  companion object {
    // These are the Intent actions that we are prepared to handle.
    const val ACTION_PLAYBACK = "com.quran.labs.androidquran.action.PLAYBACK"
    const val ACTION_PLAY = "com.quran.labs.androidquran.action.PLAY"
    const val ACTION_PAUSE = "com.quran.labs.androidquran.action.PAUSE"
    const val ACTION_STOP = "com.quran.labs.androidquran.action.STOP"
    const val ACTION_SKIP = "com.quran.labs.androidquran.action.SKIP"
    const val ACTION_REWIND = "com.quran.labs.androidquran.action.REWIND"
    const val ACTION_CONNECT = "com.quran.labs.androidquran.action.CONNECT"
    const val ACTION_UPDATE_SETTINGS = "com.quran.labs.androidquran.action.UPDATE_SETTINGS"

    // pending notification request codes
    private const val REQUEST_CODE_MAIN = 0
    private const val REQUEST_CODE_PREVIOUS = 1
    private const val REQUEST_CODE_PAUSE = 2
    private const val REQUEST_CODE_SKIP = 3
    private const val REQUEST_CODE_STOP = 4
    private const val REQUEST_CODE_RESUME = 5


    // so user can pass in a serializable LegacyAudioRequest to the intent
    const val EXTRA_PLAY_INFO = "com.quran.labs.androidquran.PLAY_INFO"
    private const val NOTIFICATION_CHANNEL_ID = Constants.AUDIO_CHANNEL
    private const val MSG_INCOMING = 1
    private const val MSG_UPDATE_AUDIO_POS = 2
    private const val DEBUG_TIMINGS = false

    // 5 minutes
    private const val EXO_BACK_BUFFER_MS = 300_000
    private const val STREAM_CACHE_DIR = "audio_stream_cache"
    private const val STREAM_CACHE_SIZE_BYTES = 128L * 1024L * 1024L

    @Volatile
    @UnstableApi
    private var audioStreamCache: SimpleCache? = null

    @OptIn(UnstableApi::class)
    private fun getAudioStreamCache(context: Context): SimpleCache {
      val existing = audioStreamCache
      if (existing != null) {
        return existing
      }

      return synchronized(this) {
        val initialized = audioStreamCache
        if (initialized != null) {
          initialized
        } else {
          val cacheDirectory = File(context.cacheDir, STREAM_CACHE_DIR)
          val cache = SimpleCache(
            cacheDirectory,
            LeastRecentlyUsedCacheEvictor(STREAM_CACHE_SIZE_BYTES),
            StandaloneDatabaseProvider(context)
          )
          audioStreamCache = cache
          cache
        }
      }
    }
  }
}

`

---

### app/src/main/java/com/quran/labs/androidquran/ui/PagerActivity.kt

`kotlin
package com.quran.labs.androidquran.ui

import android.Manifest
import android.app.SearchManager
import android.content.ComponentName
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import androidx.core.content.ContextCompat
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.preference.PreferenceManager
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.updatePadding
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager.widget.NonRestoringViewPager
import androidx.viewpager.widget.ViewPager
import androidx.viewpager.widget.ViewPager.OnPageChangeListener
import androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import com.quran.data.core.QuranInfo
import com.quran.data.dao.BookmarksDao
import com.quran.data.model.Page
import com.quran.data.model.QuranText
import com.quran.data.model.SuraAyah
import com.quran.data.model.bookmark.ReadingBookmarkTarget
import com.quran.data.model.selection.AyahSelection
import com.quran.data.model.selection.AyahSelection.AyahRange
import com.quran.data.model.selection.SelectionIndicator
import com.quran.data.model.selection.endSuraAyah
import com.quran.data.model.selection.selectionIndicator
import com.quran.data.model.selection.startSuraAyah
import com.quran.data.model.selection.withXScroll
import com.quran.labs.androidquran.BuildConfig
import com.quran.labs.androidquran.HelpActivity
import com.quran.labs.androidquran.QuranApplication
import com.quran.labs.androidquran.QuranPreferenceActivity
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.SearchActivity
import com.quran.labs.androidquran.common.QuranAyahInfo
import com.quran.labs.androidquran.common.audio.model.QariItem
import com.quran.labs.androidquran.common.audio.model.QariItem.Companion.fromQari
import com.quran.labs.androidquran.common.audio.model.playback.AudioRequest
import com.quran.labs.androidquran.common.audio.repository.AudioStatusRepository
import com.quran.labs.androidquran.common.audio.repository.CurrentQariManager
import com.quran.labs.androidquran.data.Constants
import com.quran.labs.androidquran.data.QuranDataProvider
import com.quran.labs.androidquran.data.QuranDisplayData
import com.quran.labs.androidquran.di.component.activity.PagerActivityComponent
import com.quran.labs.androidquran.feature.reading.bridge.AudioStatusRepositoryBridge
import com.quran.labs.androidquran.feature.reading.bridge.DownloadBridge
import com.quran.labs.androidquran.feature.reading.bridge.ReadingEventPresenterBridge
import com.quran.labs.androidquran.feature.reading.presenter.AudioPresenter
import com.quran.labs.androidquran.feature.reading.presenter.RecentPagePresenter
import com.quran.labs.androidquran.feature.reading.presenter.ReadingBookmarkChange
import com.quran.labs.androidquran.feature.reading.presenter.ReadingBookmarkPresenter
import com.quran.labs.androidquran.feature.reading.presenter.recitation.PagerActivityRecitationPresenter
import com.quran.labs.androidquran.model.translation.ArabicDatabaseUtils
import com.quran.labs.androidquran.presenter.data.QuranEventLogger
import com.quran.labs.androidquran.presenter.translationlist.TranslationListPresenter
import com.quran.labs.androidquran.service.AudioService
import com.quran.labs.androidquran.service.QuranDownloadService
import com.quran.labs.androidquran.service.util.PermissionUtil.buildPostPermissionDialog
import com.quran.labs.androidquran.service.util.PermissionUtil.canRequestPostNotificationPermission
import com.quran.labs.androidquran.service.util.PermissionUtil.havePostNotificationPermission
import com.quran.labs.androidquran.service.util.ServiceIntentHelper.getDownloadIntent
import com.quran.labs.androidquran.ui.fragment.AddTagDialog
import com.quran.labs.androidquran.ui.fragment.JumpFragment
import com.quran.labs.androidquran.ui.fragment.TabletFragment
import com.quran.labs.androidquran.ui.fragment.TagBookmarkDialog.OnBookmarkTagsUpdateListener
import com.quran.labs.androidquran.ui.fragment.TranslationFragment
import com.quran.labs.androidquran.ui.helpers.AyahSelectedListener
import com.quran.labs.androidquran.ui.helpers.AyahTracker
import com.quran.labs.androidquran.ui.helpers.JumpDestination
import com.quran.labs.androidquran.ui.helpers.QuranDisplayHelper
import com.quran.labs.androidquran.ui.helpers.QuranNavigator
import com.quran.labs.androidquran.ui.helpers.QuranPage
import com.quran.labs.androidquran.ui.helpers.QuranPageAdapter
import com.quran.labs.androidquran.ui.helpers.SlidingPagerAdapter
import com.quran.labs.androidquran.ui.listener.AudioBarListener
import com.quran.labs.androidquran.ui.readingbookmark.createReadingBookmarkToastView
import com.quran.labs.androidquran.ui.util.ToastCompat.makeText
import com.quran.labs.androidquran.ui.util.TranslationsSpinnerAdapter
import com.quran.labs.androidquran.feature.reading.presenter.AudioPresenterScreen
import com.quran.labs.androidquran.util.AudioUtils
import com.quran.labs.androidquran.util.OrientationLockUtils
import com.quran.labs.androidquran.util.QuranFileUtils
import com.quran.labs.androidquran.util.QuranScreenInfo
import com.quran.labs.androidquran.util.QuranSettings
import com.quran.labs.androidquran.util.QuranUtils
import com.quran.labs.androidquran.util.ShareUtil
import com.quran.labs.androidquran.view.IconPageIndicator
import com.quran.labs.androidquran.view.QuranSpinner
import com.quran.labs.androidquran.view.SlidingUpPanelLayout
import com.quran.mobile.common.download.DownloadInfoStreams
import com.quran.mobile.di.AyahActionFragmentProvider
import com.quran.mobile.di.QuranReadingActivityComponent
import com.quran.mobile.di.QuranReadingActivityComponentProvider
import com.quran.mobile.di.QuranReadingPageComponent
import com.quran.mobile.di.QuranReadingPageComponentProvider
import com.quran.mobile.feature.audiobar.AudioBarInjector
import com.quran.mobile.feature.audiobar.AudioBarWrapper
import com.quran.mobile.feature.audiobar.presenter.AudioBarEventRepository
import com.quran.mobile.feature.ayahbookmark.AyahBookmarkWrapper
import com.quran.mobile.feature.ayahbookmark.di.AyahBookmarkWrapperInjector
import com.quran.labs.androidquran.common.audio.memorization.CustomPlaylist
import com.quran.labs.androidquran.common.audio.memorization.MaskingLevel
import com.quran.labs.androidquran.common.audio.memorization.MemorizationConfig
import com.quran.labs.androidquran.common.audio.memorization.MemorizationDelayMode
import com.quran.labs.androidquran.common.audio.memorization.MemorizationEngine
import com.quran.labs.androidquran.common.audio.memorization.MemorizationPreferences
import com.quran.labs.androidquran.common.audio.memorization.PlaylistRepository
import com.quran.mobile.feature.audiobar.ui.MemorizationSetupSheetWrapper
import com.quran.mobile.feature.audiobar.ui.PlaylistSheetWrapper
import com.quran.mobile.feature.ayahbookmark.readingbookmark.ReadingBookmarkSheetWrapper
import com.quran.mobile.feature.ayahbookmark.readingbookmark.di.ReadingBookmarkSheetWrapperInjector
import com.quran.mobile.feature.qarilist.QariListWrapper
import com.quran.mobile.feature.qarilist.di.QariListWrapperInjector
import com.quran.mobile.translation.model.LocalTranslation
import com.quran.page.common.data.QuranNaming
import com.quran.page.common.factory.PageViewFactoryProvider
import com.quran.page.common.toolbar.AyahToolBar
import com.quran.page.common.toolbar.di.AyahToolBarInjector
import com.quran.reading.common.ReadingEventPresenter
import dev.zacsweers.metro.Inject
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import timber.log.Timber
import java.lang.ref.WeakReference
import java.util.concurrent.CancellationException
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import androidx.core.view.isVisible

/**
 * Activity that displays the Quran (in Arabic or translation mode).
 *
 *
 * Essentially, this activity consists of a [ViewPager] of Quran pages (using [ ]).
 * [AudioService] is used to handle playing audio, and this is synced with
 * the display of the Quran.
 */
class PagerActivity : AppCompatActivity(), AudioBarListener, OnBookmarkTagsUpdateListener,
  AyahSelectedListener, JumpDestination, QuranReadingActivityComponentProvider,
  QuranReadingPageComponentProvider, AyahToolBarInjector, QariListWrapperInjector,
  AudioBarInjector, AyahBookmarkWrapperInjector, ReadingBookmarkSheetWrapperInjector,
  ActivityCompat.OnRequestPermissionsResultCallback, AudioPresenterScreen,
  ReadingBookmarkPresenter.Screen {
  private var lastPopupTime: Long = 0
  private var isActionBarHidden = true
  private var shouldReconnect = false
  private var showingTranslation = false
  private var needsPermissionToDownloadOver3g = true
  private var promptDialog: AlertDialog? = null
  private var isDualPages = false
  private var promptedForExtraDownload = false
  private var isFoldableDeviceOpenAndVertical = false

  private var bookmarksMenuItem: MenuItem? = null
  private var isCurrentPageReadingBookmarked = false
  private var readingBookmarkToastView: View? = null

  private var translationNames: Array<String> = emptyArray()
  private var translations: List<LocalTranslation>? = null
  private var activeTranslationsFilesNames: Set<String?>? = null
  private var translationsSpinnerAdapter: TranslationsSpinnerAdapter? = null

  private lateinit var audioStatusBar: AudioBarWrapper
  private lateinit var viewPager: ViewPager
  private lateinit var pagerAdapter: QuranPageAdapter
  private lateinit var ayahToolBar: AyahToolBar
  private lateinit var slidingPanel: SlidingUpPanelLayout
  private lateinit var slidingPager: ViewPager
  private lateinit var slidingPagerAdapter: SlidingPagerAdapter
  private lateinit var translationsSpinner: QuranSpinner
  private lateinit var overlay: FrameLayout
  private lateinit var toolBarArea: View

  private var requestPermissionLauncher: ActivityResultLauncher<String>? = null

  private var isSplitScreen = false

  private lateinit var onClearAyahModeBackCallback: OnBackPressedCallback
  private lateinit var onShowingTranslationBackCallback: OnBackPressedCallback
  private lateinit var onEndSessionBackCallback: OnBackPressedCallback

  private var lastSelectedTranslationAyah: QuranAyahInfo? = null
  private var lastActivatedLocalTranslations: Array<LocalTranslation> = emptyArray()

  @Inject lateinit var quranNavigator: QuranNavigator

  @Inject lateinit var bookmarksDao: BookmarksDao
  @Inject lateinit var recentPagePresenter: RecentPagePresenter
  @Inject lateinit var readingBookmarkPresenter: ReadingBookmarkPresenter
  @Inject lateinit var quranSettings: QuranSettings
  @Inject lateinit var quranNaming: QuranNaming
  @Inject lateinit var quranScreenInfo: QuranScreenInfo
  @Inject lateinit var arabicDatabaseUtils: ArabicDatabaseUtils
  @Inject lateinit var shareUtil: ShareUtil
  @Inject lateinit var audioUtils: AudioUtils
  @Inject lateinit var quranDisplayData: QuranDisplayData
  @Inject lateinit var quranInfo: QuranInfo
  @Inject lateinit var quranFileUtils: QuranFileUtils
  @Inject lateinit var audioPresenter: AudioPresenter
  @Inject lateinit var quranEventLogger: QuranEventLogger
  @Inject lateinit var audioStatusRepository: AudioStatusRepository
  @Inject lateinit var memorizationEngine: MemorizationEngine
  @Inject lateinit var memorizationPreferences: MemorizationPreferences
  @Inject lateinit var playlistRepository: PlaylistRepository
  @Inject lateinit var readingEventPresenter: ReadingEventPresenter
  @Inject lateinit var pageProviderFactoryProvider: PageViewFactoryProvider
  @Inject lateinit var additionalAyahPanels: Set<@JvmSuppressWildcards AyahActionFragmentProvider>
  @Inject lateinit var pagerActivityRecitationPresenter: PagerActivityRecitationPresenter
  @Inject lateinit var translationListPresenter: TranslationListPresenter
  @Inject lateinit var audioBarEventRepository: AudioBarEventRepository
  @Inject lateinit var downloadInfoStreams: DownloadInfoStreams
  @Inject lateinit var qariManager: CurrentQariManager
  @Inject lateinit var downloadBridge: DownloadBridge

  private lateinit var audioStatusRepositoryBridge: AudioStatusRepositoryBridge
  private lateinit var readingEventPresenterBridge: ReadingEventPresenterBridge
  private lateinit var windowInsetsController: WindowInsetsControllerCompat

  private var translationJob: Job? = null
  private lateinit var compositeDisposable: CompositeDisposable
  private val foregroundDisposable = CompositeDisposable()
  private val scope = MainScope()

  val pagerActivityComponent: PagerActivityComponent by lazy {
    (application as QuranApplication)
      .applicationComponent
      .activityComponentFactory()
      .generate(this)
      .pagerActivityComponentFactory()
      .generate(this)
  }

  private val handler = PagerHandler(this)

  private class PagerHandler(activity: PagerActivity) : Handler(Looper.getMainLooper()) {
    private val activity = WeakReference(activity)

    override fun handleMessage(msg: Message) {
      val activity = activity.get()
      if (activity != null) {
        if (msg.what == MSG_HIDE_ACTIONBAR) {
          activity.toggleActionBarVisibility(false)
        } else {
          super.handleMessage(msg)
        }
      }
    }
  }

  public override fun onCreate(savedInstanceState: Bundle?) {
    WindowCompat.setDecorFitsSystemWindows(window, false)
    super.onCreate(savedInstanceState)

    // field injection
    pagerActivityComponent.inject(this)

    isFoldableDeviceOpenAndVertical =
      savedInstanceState?.getBoolean(LAST_FOLDING_STATE, isFoldableDeviceOpenAndVertical)
        ?: isFoldableDeviceOpenAndVertical

    lifecycleScope.launch {
      lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
        WindowInfoTracker.getOrCreate(this@PagerActivity)
          .windowLayoutInfo(this@PagerActivity)
          .map { it.displayFeatures }
          .collectLatest {
            val foldingFeatures = it.filterIsInstance<FoldingFeature>().firstOrNull()
            if (foldingFeatures != null) {
              val localState =
                foldingFeatures.orientation == FoldingFeature.Orientation.VERTICAL &&
                    (foldingFeatures.state == FoldingFeature.State.FLAT || foldingFeatures.state == FoldingFeature.State.HALF_OPENED)
              if (isFoldableDeviceOpenAndVertical != localState) {
                isFoldableDeviceOpenAndVertical = localState
                updateDualPageMode()
              }
            } else if (isFoldableDeviceOpenAndVertical) {
              // this else case suggests that the device is not open and vertical, otherwise
              // we'd have some information given via the folding features.
              isFoldableDeviceOpenAndVertical = false
              updateDualPageMode()
            }
          }
      }
    }

    setContentView(R.layout.quran_page_activity_slider)

    val lightStatusBar = resources.getBoolean(R.bool.light_navigation_bar)
    windowInsetsController = WindowInsetsControllerCompat(
      window, findViewById<ViewGroup>(R.id.sliding_panel)
    ).apply {
      isAppearanceLightStatusBars = lightStatusBar
      isAppearanceLightNavigationBars = lightStatusBar
    }
    registerBackPressedCallbacks()
    initialize(savedInstanceState)
    requestPermissionLauncher =
      registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean? ->
        audioPresenter.onPostNotificationsPermissionResponse(
          isGranted!!
        )
      }

    // read the list of translations
    requestTranslationsList()

    downloadBridge.subscribeToDownloads {
      onDownloadSuccess()
    }

  }

  private fun updateDualPageMode() {
    val lastIsDualPages = isDualPages
    val nextIsDualPages = QuranUtils.isDualPages(this, quranScreenInfo, isFoldableDeviceOpenAndVertical)
    if (lastIsDualPages != nextIsDualPages) {
      // due to the conversion after setting the adapter, we need this page to be
      // the page _before_ we switched from dual to single or vice versa. otherwise,
      // we end up double-converting the page.
      //
      // might be worth trying later to undo this and just remove the conversion below.
      val page = currentPage
      isDualPages = nextIsDualPages

      pagerAdapter = QuranPageAdapter(
        supportFragmentManager,
        isDualPages,
        showingTranslation,
        quranInfo,
        isSplitScreen,
        pageProviderFactoryProvider.providePageViewFactory(quranSettings.pageType)
      )
      viewPager.adapter = pagerAdapter
      // when going from two page per screen to one or vice versa, we adjust the page number,
      // such that the first page is always selected.
      val curPage = if (isDualPageVisible) {
        quranInfo.mapSinglePageToDualPage(page)
      } else {
        quranInfo.mapDualPageToSinglePage(page)
      }

      val pageIndex = quranInfo.getPositionFromPage(curPage, isDualPageVisible)
      viewPager.setCurrentItem(pageIndex)
    }
  }

  private fun initialize(savedInstanceState: Bundle?) {
    var shouldAdjustPageNumber = false
    isDualPages = QuranUtils.isDualPages(this, quranScreenInfo, isFoldableDeviceOpenAndVertical)
    isSplitScreen = quranSettings.isQuranSplitWithTranslation
    audioStatusRepositoryBridge = AudioStatusRepositoryBridge(
      audioStatusRepository,
      audioBarEventRepository,
      { suraAyah: SuraAyah? ->
        onAudioPlaybackAyahChanged(suraAyah)
      },
      this,
      pagerActivityRecitationPresenter
    )
    readingEventPresenterBridge = ReadingEventPresenterBridge(
      readingEventPresenter,
      {
        onPageClicked()
      },
      handleSelection = { ayahSelection: AyahSelection ->
        onAyahSelectionChanged(ayahSelection)
      }
    )

    memorizationEngine.state
      .onEach { memState ->
        val ayah = memState.currentPlayingAyah
        if (memState.isActive && ayah != null) {
          ensurePage(ayah.sura, ayah.ayah)
          readingEventPresenterBridge.setSelection(ayah.sura, ayah.ayah, true)
        }
      }
      .launchIn(scope)

    // remove the window background to avoid overdraw. note that, per Romain's blog, this is
    // acceptable (as long as we don't set the background color to null in the theme, since
    // that is used to generate preview windows).
    window.setBackgroundDrawable(null)

    var page = -1
    isActionBarHidden = true
    if (savedInstanceState != null) {
      Timber.d("non-null saved instance state!")
      page = savedInstanceState.getInt(LAST_READ_PAGE, -1)
      showingTranslation = savedInstanceState
        .getBoolean(LAST_READING_MODE_IS_TRANSLATION, false)
      if (savedInstanceState.containsKey(LAST_ACTIONBAR_STATE)) {
        isActionBarHidden = !savedInstanceState.getBoolean(LAST_ACTIONBAR_STATE)
      }
      val lastWasDualPages = savedInstanceState.getBoolean(LAST_WAS_DUAL_PAGES, isDualPages)
      shouldAdjustPageNumber = (lastWasDualPages != isDualPages)
    } else {
      val intent = intent
      val extras = intent.extras
      if (extras != null) {
        page = extras.getInt("page", Constants.PAGES_FIRST)
        showingTranslation =
          extras.getBoolean(EXTRA_JUMP_TO_TRANSLATION, showingTranslation)
        val highlightedSura = extras.getInt(EXTRA_HIGHLIGHT_SURA, -1)
        val highlightedAyah = extras.getInt(EXTRA_HIGHLIGHT_AYAH, -1)

        if (highlightedSura > -1 && highlightedAyah > -1) {
          readingEventPresenterBridge.setSelection(
            highlightedSura,
            highlightedAyah,
            true
          )
        }
      }
    }
    onShowingTranslationBackCallback.isEnabled = showingTranslation

    compositeDisposable = CompositeDisposable()

    audioStatusBar = findViewById(R.id.audio_area)

    toolBarArea = findViewById(R.id.toolbar_area)
    translationsSpinner = findViewById(R.id.spinner)
    overlay = findViewById(R.id.overlay)

    ViewCompat.setOnApplyWindowInsetsListener(toolBarArea) { view, windowInsets ->
      val insets = windowInsets.getInsets(
        WindowInsetsCompat.Type.statusBars() or
            WindowInsetsCompat.Type.displayCutout() or
            WindowInsetsCompat.Type.navigationBars()
      )
      view.updatePadding(insets.left, insets.top, insets.right, 0)
      windowInsets
    }

    val toolbar = findViewById<Toolbar>(R.id.toolbar)
    setSupportActionBar(toolbar)

    supportActionBar?.setDisplayShowHomeEnabled(true)
    supportActionBar?.setDisplayHomeAsUpEnabled(true)

    initAyahActionPanel()

    if (showingTranslation && translationNames.isNotEmpty()) {
      updateActionBarSpinner()
    } else {
      updateActionBarTitle(page)
    }

    lastPopupTime = System.currentTimeMillis()
    pagerAdapter = QuranPageAdapter(
      supportFragmentManager,
      isDualPages,
      showingTranslation,
      quranInfo,
      isSplitScreen,
      pageProviderFactoryProvider.providePageViewFactory(quranSettings.pageType)
    )
    ayahToolBar = findViewById(R.id.ayah_toolbar)
    ayahToolBar.flavor = BuildConfig.FLAVOR
    ayahToolBar.longPressLambda = { charSequence: CharSequence? ->
      makeText(this@PagerActivity, charSequence!!, Toast.LENGTH_SHORT).show()
    }

    ViewCompat.setOnApplyWindowInsetsListener(ayahToolBar) { view, windowInsets ->
      val insets = windowInsets.getInsets(
        WindowInsetsCompat.Type.statusBars() or
            WindowInsetsCompat.Type.displayCutout() or
            WindowInsetsCompat.Type.navigationBars()
      )
      ayahToolBar.insets = insets
      windowInsets
    }

    val nonRestoringViewPager = findViewById<NonRestoringViewPager>(R.id.quran_pager)
    nonRestoringViewPager.setIsDualPagesInLandscape(
      QuranUtils.isDualPagesInLandscape(this, quranScreenInfo)
    )

    viewPager = nonRestoringViewPager
    viewPager.setAdapter(pagerAdapter)

    ayahToolBar.setOnItemSelectedListener(AyahMenuItemSelectionHandler())
    val onPageChangeListener: OnPageChangeListener = object : OnPageChangeListener {
      override fun onPageScrollStateChanged(state: Int) {
      }

      override fun onPageScrolled(
        position: Int,
        positionOffset: Float,
        positionOffsetPixels: Int
      ) {
        val currentSelection = readingEventPresenter.currentAyahSelection()
        val selectionIndicator =
          currentSelection.selectionIndicator()
        val suraAyah = currentSelection.startSuraAyah()
        if (selectionIndicator !== SelectionIndicator.None && selectionIndicator !== SelectionIndicator.ScrollOnly && suraAyah != null) {
          val startPage = quranInfo.getPageFromSuraAyah(suraAyah.sura, suraAyah.ayah)
          val barPos = quranInfo.getPositionFromPage(startPage, isDualPageVisible)
          if (position == barPos) {
            // Swiping to next ViewPager page (i.e. prev quran page)
            val updatedSelectionIndicator =
              selectionIndicator.withXScroll(-positionOffsetPixels.toFloat())
            readingEventPresenterBridge.withSelectionIndicator(
              updatedSelectionIndicator
            )
          } else if (position == barPos - 1 || position == barPos + 1) {
            // Swiping to previous or next ViewPager page (i.e. next or previous quran page)
            val updatedSelectionIndicator =
              selectionIndicator.withXScroll((viewPager.width - positionOffsetPixels).toFloat())
            readingEventPresenterBridge.withSelectionIndicator(
              updatedSelectionIndicator
            )
          } else {
            readingEventPresenterBridge.clearSelectedAyah()
          }
        }
      }

      override fun onPageSelected(position: Int) {
        Timber.d("onPageSelected(): %d", position)
        val page = quranInfo.getPageFromPosition(position, isDualPageVisible)

        if (quranSettings.shouldDisplayMarkerPopup()) {
          lastPopupTime = QuranDisplayHelper.displayMarkerPopup(
            this@PagerActivity, quranInfo, page, isDualPageVisible, lastPopupTime
          )
        }

        if (!showingTranslation) {
          updateActionBarTitle(page)
        } else {
          refreshActionBarSpinner()
        }

        // If we're more than 1 page away from ayah selection end ayah mode
        val suraAyah: SuraAyah? = selectionStart
        if (suraAyah != null) {
          val startPage = quranInfo.getPageFromSuraAyah(suraAyah.sura, suraAyah.ayah)
          val ayahPos = quranInfo.getPositionFromPage(startPage, isDualPageVisible)
          if (abs((ayahPos - position).toDouble()) > 1) {
            endAyahMode()
          }
        }
      }
    }
    viewPager.addOnPageChangeListener(onPageChangeListener)

    setUiVisibilityListener()
    audioStatusBar.visibility = View.VISIBLE
    toggleActionBarVisibility(true)

    if (shouldAdjustPageNumber) {
      // when going from two page per screen to one or vice versa, we adjust the page number,
      // such that the first page is always selected.
      val curPage = if (isDualPageVisible) {
        quranInfo.mapSinglePageToDualPage(page)
      } else {
        quranInfo.mapDualPageToSinglePage(page)
      }
      page = curPage
    }

    val pageIndex = quranInfo.getPositionFromPage(page, isDualPageVisible)
    viewPager.setCurrentItem(pageIndex)
    if (page == 0) {
      onPageChangeListener.onPageSelected(0)
    }

    // just got created, need to reconnect to service
    shouldReconnect = true

    // enforce orientation lock
    if (quranSettings.isLockOrientation &&
      OrientationLockUtils.isOrientationLockSupported(resources.configuration)
    ) {
      val current = resources.configuration.orientation
      if (quranSettings.isLandscapeOrientation) {
        if (current == Configuration.ORIENTATION_PORTRAIT) {
          requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
          return
        }
      } else if (current == Configuration.ORIENTATION_LANDSCAPE) {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        return
      }
    }

    quranEventLogger.logAnalytics(isDualPages, showingTranslation, isSplitScreen)

    // Setup recitation (if enabled)
    pagerActivityRecitationPresenter.bind(
      this, PagerActivityRecitationPresenter.Bridge(
        { isDualPageVisible },
        { currentPage },
        { ayahToolBar },
        { ayah: SuraAyah -> ensurePage(ayah.sura, ayah.ayah) },
        { sliderPage: Int -> showSlider(slidingPagerAdapter.getPagePosition(sliderPage)) }
      ))
  }

  private fun registerBackPressedCallbacks() {
    val isSessionEnabled = false
    onEndSessionBackCallback = object : OnBackPressedCallback(isSessionEnabled) {
      override fun handleOnBackPressed() {
        onSessionEnd()
      }
    }
    onBackPressedDispatcher.addCallback(this, onEndSessionBackCallback)

    onShowingTranslationBackCallback = object : OnBackPressedCallback(showingTranslation) {
      override fun handleOnBackPressed() {
        switchToQuran()
      }
    }
    onBackPressedDispatcher.addCallback(this, onShowingTranslationBackCallback)

    onClearAyahModeBackCallback = object : OnBackPressedCallback(selectionStart != null) {
      override fun handleOnBackPressed() {
        endAyahMode()
      }
    }
    onBackPressedDispatcher.addCallback(this, onClearAyahModeBackCallback)
  }

  override fun onRequestPermissionsResult(
    requestCode: Int, permissions: Array<String>, grantResults: IntArray
  ) {
    pagerActivityRecitationPresenter.onPermissionsResult(requestCode, grantResults)
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
  }

  private val isDualPageVisible: Boolean
    get() = isDualPages && !(isSplitScreen && showingTranslation)

  private fun shouldUpdatePageNumber(): Boolean {
    return isDualPages && isSplitScreen
  }

  private val currentPageFlow: Flow<Int> =
    callbackFlow {
      val pageChangedListener: OnPageChangeListener =
        object : SimpleOnPageChangeListener() {
          override fun onPageSelected(position: Int) {
            val page = quranInfo.getPageFromPosition(position, isDualPageVisible)
            trySend(page)
          }
        }
      viewPager.addOnPageChangeListener(pageChangedListener)
      awaitClose { viewPager.removeOnPageChangeListener(pageChangedListener) }
    }
      .onStart { emit(currentPage) }
      .buffer(onBufferOverflow = BufferOverflow.DROP_OLDEST)
      .shareIn(scope, SharingStarted.Eagerly, 1)

  private fun initAyahActionPanel() {
    slidingPanel = findViewById(R.id.sliding_panel)
    val slidingLayout =
      slidingPanel.findViewById<ViewGroup>(R.id.sliding_layout)
    slidingPager = slidingPanel
      .findViewById(R.id.sliding_layout_pager)
    val slidingPageIndicator =
      slidingPanel
        .findViewById<IconPageIndicator>(R.id.sliding_pager_indicator)

    // Find close button and set listener
    val closeButton = slidingPanel
      .findViewById<View>(R.id.sliding_menu_close)
    closeButton.setOnClickListener { v: View? -> endAyahMode() }

    // Create and set fragment pager adapter
    slidingPagerAdapter = SlidingPagerAdapter(
      supportFragmentManager,
      QuranUtils.isRtl(),
      additionalAyahPanels
    )
    slidingPager.setAdapter(slidingPagerAdapter)

    // Attach the view pager to the action bar
    slidingPageIndicator.setViewPager(slidingPager)

    // Set sliding layout parameters
    val displayHeight = resources.displayMetrics.heightPixels
    slidingLayout.layoutParams.height = (displayHeight * PANEL_MAX_HEIGHT).toInt()
    slidingPanel.setEnableDragViewTouchEvents(true)
    slidingPanel.setPanelSlideListener(SlidingPanelListener())
    slidingLayout.visibility = View.GONE

    // When clicking any menu items, expand the panel
    slidingPageIndicator.setOnClickListener { v: View? ->
      if (!slidingPanel.isExpanded) {
        slidingPanel.expandPane()
      }
    }
  }

  override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    if (hasFocus) {
      handler.sendEmptyMessageDelayed(MSG_HIDE_ACTIONBAR, DEFAULT_HIDE_AFTER_TIME)
    } else {
      handler.removeMessages(MSG_HIDE_ACTIONBAR)
    }
  }

  private fun onPageClicked() {
    toggleActionBar()
  }

  private fun onAudioPlaybackAyahChanged(suraAyah: SuraAyah?) {
    if (suraAyah != null) {
      // continue to snap back to the page when the playback ayah changes
      ensurePage(suraAyah.sura, suraAyah.ayah)
    }
  }

  private fun onAyahSelectionChanged(ayahSelection: AyahSelection) {
    val haveSelection = ayahSelection !== AyahSelection.None
    val currentSelection = ayahSelection.selectionIndicator()
    if (currentSelection is SelectionIndicator.None && haveSelection) {
      viewPager.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
    }

    if (haveSelection) {
      val startPosition = startPosition(ayahSelection)
      updateLocalTranslations(startPosition)
      onClearAyahModeBackCallback.isEnabled = selectionStart != null
    } else {
      endAyahMode()
      onClearAyahModeBackCallback.isEnabled = false
    }
  }

  private fun startPosition(ayahSelection: AyahSelection): SuraAyah? {
    return when (ayahSelection) {
      is AyahSelection.Ayah -> {
        ayahSelection.suraAyah
      }

      is AyahRange -> {
        ayahSelection.startSuraAyah
      }

      else -> {
        null
      }
    }
  }

  private fun setUiVisibility(isVisible: Boolean) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      setUiVisibilityR(isVisible)
    } else {
      setUiVisibilityKitKat(isVisible)
    }
  }

  private fun setUiVisibilityR(isVisible: Boolean) {
    if (isVisible) {
      windowInsetsController.show(
        WindowInsetsCompat.Type.statusBars() or
            WindowInsetsCompat.Type.navigationBars()
      )
    } else {
      windowInsetsController.hide(
        WindowInsetsCompat.Type.statusBars() or
            WindowInsetsCompat.Type.navigationBars()
      )
      windowInsetsController.systemBarsBehavior =
        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInMultiWindowMode) {
      animateToolBar(isVisible)
    }
  }

  private fun setUiVisibilityKitKat(isVisible: Boolean) {
    var flags = (View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
    if (!isVisible) {
      flags = flags or (View.SYSTEM_UI_FLAG_LOW_PROFILE
          or View.SYSTEM_UI_FLAG_FULLSCREEN
          or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
          or View.SYSTEM_UI_FLAG_IMMERSIVE)
    }
    viewPager.systemUiVisibility = flags

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInMultiWindowMode) {
      animateToolBar(isVisible)
    }
  }

  private fun setUiVisibilityListener() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
      ViewCompat.setOnApplyWindowInsetsListener(window.decorView) { _, insets ->
        val isStatusBarVisible = insets.isVisible(WindowInsetsCompat.Type.statusBars())
        // on devices with "hide full screen indicator" or "hide the bottom bar,"
        // this always returns false, which causes the touches to not work.
        val isNavigationBarVisible = insets.isVisible(WindowInsetsCompat.Type.navigationBars())

        // as a fix for the aforementioned point, make either one's visibility suggest
        // visibility (instead of requiring both to agree).
        val isVisible = isStatusBarVisible || isNavigationBarVisible

        animateToolBar(isVisible)
        insets
      }
    } else {
      viewPager.setOnSystemUiVisibilityChangeListener { flags: Int ->
        val visible = (flags and View.SYSTEM_UI_FLAG_FULLSCREEN) == 0
        animateToolBar(visible)
      }
    }
  }

  private fun clearUiVisibilityListener() {
    viewPager.setOnSystemUiVisibilityChangeListener(null)
  }

  private fun animateToolBar(visible: Boolean) {
    isActionBarHidden = !visible

    // animate toolbar
    toolBarArea.animate()
      .translationY((if (visible) 0 else -toolBarArea.height).toFloat())
      .setDuration(250)
      .start()

    // and audio bar
    audioStatusBar.animate()
      .translationY((if (visible) 0 else audioStatusBar.height).toFloat())
      .setDuration(250)
      .start()
  }

  override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
    val navigate = (audioStatusRepositoryBridge.audioRequest() == null
        && quranSettings.navigateWithVolumeKeys())
    if (navigate && keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
      viewPager.currentItem -= 1
      return true
    } else if (navigate && keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
      viewPager.currentItem += 1
      return true
    }
    return super.onKeyDown(keyCode, event)
  }

  override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
    return (((keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
        keyCode == KeyEvent.KEYCODE_VOLUME_UP) && (
        audioStatusRepositoryBridge.audioRequest() == null) &&
        PreferenceManager.getDefaultSharedPreferences(this)
          .getBoolean(Constants.PREF_USE_VOLUME_KEY_NAV, false))
        || super.onKeyUp(keyCode, event))
  }

  public override fun onResume() {
    super.onResume()

    audioPresenter.bind(this)
    recentPagePresenter.bind(currentPageFlow)
    readingBookmarkPresenter.bind(currentPageFlow, this)

    if (shouldReconnect) {
      foregroundDisposable.add(
        Completable.timer(500, TimeUnit.MILLISECONDS)
          .observeOn(AndroidSchedulers.mainThread())
          .subscribe {
            try {
              startService(
                audioUtils.getAudioIntent(
                  this@PagerActivity,
                  AudioService.ACTION_CONNECT
                )
              )
            } catch (ise: IllegalStateException) {
              // we're likely in the background, so ignore.
            }
            shouldReconnect = false
          })
    }
  }

  override fun inject(audioBarWrapper: AudioBarWrapper) {
    pagerActivityComponent.inject(audioBarWrapper)
  }

  override fun injectQariListWrapper(qariListWrapper: QariListWrapper) {
    pagerActivityComponent.inject(qariListWrapper)
  }

  override fun injectAyahBookmarkWrapper(ayahBookmarkWrapper: AyahBookmarkWrapper) {
    pagerActivityComponent.inject(ayahBookmarkWrapper)
  }

  override fun injectReadingBookmarkSheetWrapper(
    readingBookmarkSheetWrapper: ReadingBookmarkSheetWrapper
  ) {
    pagerActivityComponent.inject(readingBookmarkSheetWrapper)
  }

  override fun provideQuranReadingActivityComponent(): QuranReadingActivityComponent {
    return pagerActivityComponent
  }

  override fun provideQuranReadingPageComponent(vararg pages: Int): QuranReadingPageComponent {
    return pagerActivityComponent
      .quranPageComponentFactory()
      .generate(pages)
  }

  override fun injectToolBar(ayahToolBar: AyahToolBar) {
    pagerActivityComponent
      .inject(ayahToolBar)
  }

  fun showGetRequiredFilesDialog() {
    if (promptDialog != null) {
      return
    }
    val builder = AlertDialog.Builder(this)
    builder.setMessage(R.string.download_extra_data)
      .setPositiveButton(
        R.string.downloadPrompt_ok
      ) { dialog: DialogInterface, option: Int ->
        downloadRequiredFiles()
        dialog.dismiss()
        promptDialog = null
      }
      .setNegativeButton(
        R.string.downloadPrompt_no
      ) { dialog: DialogInterface, option: Int ->
        dialog.dismiss()
        promptDialog = null
      }
    promptDialog = builder.create()
    promptDialog!!.show()
  }

  private fun downloadRequiredFiles() {
    var downloadType = QuranDownloadService.DOWNLOAD_TYPE_AUDIO
    if (audioStatusRepositoryBridge.audioRequest() == null) {
      // if we're not playing any audio, use audio download bar as our progress bar
      if (isActionBarHidden) {
        toggleActionBar()
      }
    } else {
      // if audio is playing, let's not disrupt it - do this using a
      // different type so the broadcast receiver ignores it.
      downloadType = QuranDownloadService.DOWNLOAD_TYPE_ARABIC_SEARCH_DB
    }

    var haveDownload = false
    if (!quranFileUtils.haveAyaPositionFile()) {
      var url = quranFileUtils.ayaPositionFileUrl
      if (isDualPages) {
        url = quranFileUtils.getAyaPositionFileUrl(
          quranScreenInfo.tabletWidthParam
        )
      }
      val destination = quranFileUtils.quranAyahDatabaseDirectory
      // start the download
      val notificationTitle = getString(R.string.highlighting_database)
      val intent = getDownloadIntent(
        this, url,
        destination.absolutePath, notificationTitle, AUDIO_DOWNLOAD_KEY,
        downloadType
      )
      Timber.d("starting service to download ayah position file")
      startService(intent)

      haveDownload = true
    }

    if (!quranFileUtils.hasArabicSearchDatabase()) {
      val url = quranFileUtils.arabicSearchDatabaseUrl

      // show "downloading required files" unless we already showed that for
      // highlighting database, in which case show "downloading search data"
      var notificationTitle = getString(R.string.highlighting_database)
      if (haveDownload) {
        notificationTitle = getString(R.string.search_data)
      }

      val extension = if (url.endsWith(".zip")) ".zip" else ""
      val intent = getDownloadIntent(
        this, url,
        quranFileUtils.getQuranDatabaseDirectory().absolutePath, notificationTitle,
        AUDIO_DOWNLOAD_KEY, downloadType
      )
      intent.putExtra(
        QuranDownloadService.EXTRA_OUTPUT_FILE_NAME,
        QuranDataProvider.QURAN_ARABIC_DATABASE + extension
      )
      Timber.d("starting service to download arabic database")
      startService(intent)
    }

    if (downloadType != QuranDownloadService.DOWNLOAD_TYPE_AUDIO) {
      // if audio is playing, just show a status notification
      makeText(
        this, com.quran.mobile.common.download.R.string.downloading,
        Toast.LENGTH_SHORT
      ).show()
    }
  }

  public override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)

    recentPagePresenter.onJump()
    val extras = intent.extras
    if (extras != null) {
      val page = extras.getInt("page", Constants.PAGES_FIRST)

      val currentValue = showingTranslation
      showingTranslation = extras.getBoolean(EXTRA_JUMP_TO_TRANSLATION, showingTranslation)
      val highlightedSura = extras.getInt(EXTRA_HIGHLIGHT_SURA, -1)
      val highlightedAyah = extras.getInt(EXTRA_HIGHLIGHT_AYAH, -1)
      if (highlightedSura > 0 && highlightedAyah > 0) {
        readingEventPresenterBridge.setSelection(highlightedSura, highlightedAyah, true)
      }

      if (showingTranslation != currentValue) {
        if (showingTranslation) {
          pagerAdapter.setTranslationMode()
          updateActionBarSpinner()
          onShowingTranslationBackCallback.isEnabled = true
        } else {
          pagerAdapter.setQuranMode()
          updateActionBarTitle(page)
          onShowingTranslationBackCallback.isEnabled = false
        }

        supportInvalidateOptionsMenu()
      }

      if (highlightedAyah > 0 && highlightedSura > 0) {
        // this will jump to the right page automagically
        ensurePage(highlightedSura, highlightedAyah)
      } else {
        val pagePosition = quranInfo.getPositionFromPage(page, isDualPageVisible)
        viewPager.currentItem = pagePosition
      }

      setIntent(intent)
    }
  }

  override fun jumpTo(page: Int) {
    quranNavigator.jumpTo(Page(page))
  }

  override fun jumpToAndHighlight(page: Int, sura: Int, ayah: Int) {
    quranNavigator.jumpTo(SuraAyah(sura, ayah))
  }

  public override fun onPause() {
    foregroundDisposable.clear()
    promptDialog?.dismiss()
    promptDialog = null
    recentPagePresenter.unbind()
    readingBookmarkPresenter.unbind(this)
    quranSettings.wasShowingTranslation = pagerAdapter.isShowingTranslation

    super.onPause()
  }

  override fun onStop() {
    // the activity will be paused when requesting notification
    // permissions, which will otherwise break audio presenter.
    audioPresenter.unbind(this)
    super.onStop()
  }

  override fun onDestroy() {
    Timber.d("onDestroy()")
    clearUiVisibilityListener()

    translationJob?.cancel(CancellationException())
    compositeDisposable.dispose()
    audioStatusRepositoryBridge.dispose()
    readingEventPresenterBridge.dispose()
    downloadBridge.unsubscribe()
    handler.removeCallbacksAndMessages(null)
    scope.cancel()
    super.onDestroy()
  }

  private fun onSessionEnd() {
    onEndSessionBackCallback.isEnabled = false
    pagerActivityRecitationPresenter.onSessionEnd()
  }

  public override fun onSaveInstanceState(state: Bundle) {
    val lastPage = quranInfo.getPageFromPosition(viewPager.currentItem, isDualPageVisible)
    state.putInt(LAST_READ_PAGE, lastPage)
    state.putBoolean(LAST_READING_MODE_IS_TRANSLATION, showingTranslation)
    state.putBoolean(LAST_ACTIONBAR_STATE, isActionBarHidden)
    state.putBoolean(LAST_WAS_DUAL_PAGES, isDualPages)
    state.putBoolean(LAST_FOLDING_STATE, isFoldableDeviceOpenAndVertical)
    super.onSaveInstanceState(state)
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    super.onCreateOptionsMenu(menu)
    val inflater = menuInflater
    inflater.inflate(R.menu.quran_menu, menu)
    val item = menu.findItem(R.id.search)
    val searchView = item.actionView as SearchView
    val searchManager = getSystemService(SEARCH_SERVICE) as SearchManager
    searchView.queryHint = getString(R.string.search_hint)
    searchView.setSearchableInfo(
      searchManager.getSearchableInfo(
        ComponentName(this, SearchActivity::class.java)
      )
    )

    // cache because invalidateOptionsMenu in a toolbar world always calls both
    // onCreateOptionsMenu and onPrepareOptionsMenu, which can be expensive both
    // due to inflation plus due to the search view specific setup work. we can
    // directly modify the bookmark item using a reference to this instead.
    bookmarksMenuItem = menu.findItem(R.id.favorite_item)
    return true
  }

  override fun onPrepareOptionsMenu(menu: Menu): Boolean {
    super.onPrepareOptionsMenu(menu)

    if (bookmarksMenuItem != null) {
      refreshBookmarksMenu()
    }

    val quran = menu.findItem(R.id.goto_quran)
    val translation = menu.findItem(R.id.goto_translation)
    if (quran != null && translation != null) {
      if (!showingTranslation) {
        quran.isVisible = false
        translation.isVisible = true
      } else {
        quran.isVisible = true
        translation.isVisible = false
      }
    }

    val nightMode = menu.findItem(R.id.night_mode)
    if (nightMode != null) {
      val prefs = PreferenceManager.getDefaultSharedPreferences(this)
      val isNightMode = prefs.getBoolean(Constants.PREF_NIGHT_MODE, false)
      nightMode.isChecked = isNightMode
      nightMode.setIcon(if (isNightMode) R.drawable.ic_night_mode else R.drawable.ic_day_mode)
    }
    return true
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    val itemId = item.itemId
    if (itemId == R.id.favorite_item) {
      showReadingBookmarkSheet(ReadingBookmarkTarget.Page(currentPage))
      return true
    } else if (itemId == R.id.goto_quran) {
      switchToQuran()
      return true
    } else if (itemId == R.id.goto_translation) {
      if (translations != null) {
        quranEventLogger.switchToTranslationMode(translations!!.size)
        switchToTranslation()
      }
      return true
    } else if (itemId == R.id.night_mode) {
      val prefs = PreferenceManager
        .getDefaultSharedPreferences(this)
      val prefsEditor = prefs.edit()
      val isNightMode = !item.isChecked
      prefsEditor.putBoolean(Constants.PREF_NIGHT_MODE, isNightMode).apply()
      item.setIcon(if (isNightMode) R.drawable.ic_night_mode else R.drawable.ic_day_mode)
      item.isChecked = isNightMode
      refreshQuranPages()
      return true
    } else if (itemId == R.id.settings) {
      val i = Intent(this, QuranPreferenceActivity::class.java)
      startActivity(i)
      return true
    } else if (itemId == R.id.help) {
      val i = Intent(this, HelpActivity::class.java)
      startActivity(i)
      return true
    } else if (itemId == android.R.id.home) {
      onSessionEnd()
      finish()
      return true
    } else if (itemId == R.id.jump) {
      val fm = supportFragmentManager
      val jumpDialog = JumpFragment()
      jumpDialog.show(fm, JumpFragment.TAG)
      return true
    }
    return super.onOptionsItemSelected(item)
  }

  private fun refreshQuranPages() {
    val pos = viewPager.currentItem
    val start = if (pos == 0) 0 else pos - 1
    val end = if (pos == pagerAdapter.count - 1) pos else pos + 1
    for (i in start..end) {
      val f = pagerAdapter.getFragmentIfExists(i)
      if (f is QuranPage) {
        (f as QuranPage).updateView()
      }
    }
  }

  private fun switchToQuran() {
    if (selectionStart != null) {
      endAyahMode()
    }
    val page = currentPage
    pagerAdapter.setQuranMode()
    showingTranslation = false
    onShowingTranslationBackCallback.isEnabled = false
    if (shouldUpdatePageNumber()) {
      val position = quranInfo.getPositionFromPage(page, true)
      viewPager.currentItem = position
    }

    supportInvalidateOptionsMenu()
    updateActionBarTitle(page)
  }

  private fun switchToTranslation() {
    if (selectionStart != null) {
      endAyahMode()
    }

    if (translations!!.isEmpty()) {
      startTranslationManager()
    } else {
      val page = currentPage
      pagerAdapter.setTranslationMode()
      showingTranslation = true
      onShowingTranslationBackCallback.isEnabled = true
      if (shouldUpdatePageNumber()) {
        val position = quranInfo.getPositionFromPage(page, false)
        viewPager.currentItem = position
      }
      supportInvalidateOptionsMenu()
      updateActionBarSpinner()
    }

    if (!quranFileUtils.hasArabicSearchDatabase() && !promptedForExtraDownload) {
      promptedForExtraDownload = true
      showGetRequiredFilesDialog()
    }
  }

  fun startTranslationManager() {
    startActivity(Intent(this, TranslationManagerActivity::class.java))
  }

  private val translationItemChangedListener =
    TranslationsSpinnerAdapter.OnSelectionChangedListener { selectedItems: Set<String?>? ->
      quranSettings.activeTranslations = selectedItems
      val pos = viewPager.currentItem - 1
      for (count in 0..2) {
        if (pos + count < 0) {
          continue
        }
        val f = pagerAdapter.getFragmentIfExists(pos + count)
        if (f is TranslationFragment) {
          f.refresh()
        } else if (f is TabletFragment) {
          f.refresh()
        }
      }
    }

  override fun onAddTagSelected() {
    val fm = supportFragmentManager
    val dialog = AddTagDialog()
    dialog.show(fm, AddTagDialog.TAG)
  }

  private fun updateActionBarTitle(page: Int) {
    val sura = quranDisplayData.getSuraNameFromPage(this, page, true)
    val actionBar = supportActionBar
    if (actionBar != null) {
      translationsSpinner.visibility = View.GONE
      actionBar.setDisplayShowTitleEnabled(true)
      actionBar.title = sura
      val desc = quranDisplayData.getPageSubtitle(this, page)
      actionBar.subtitle = desc
    }
  }

  private fun refreshActionBarSpinner() {
    if (translationsSpinnerAdapter != null) {
      translationsSpinnerAdapter!!.notifyDataSetChanged()
    } else {
      updateActionBarSpinner()
    }
  }

  private val currentPage: Int
    get() = quranInfo.getPageFromPosition(viewPager.currentItem, isDualPageVisible)

  private fun updateActionBarSpinner() {
    if (translationNames.isEmpty()) {
      val page = currentPage
      updateActionBarTitle(page)
      return
    }

    if (translationsSpinnerAdapter == null) {
      translationsSpinnerAdapter = object : TranslationsSpinnerAdapter(
        this,
        R.layout.translation_ab_spinner_item, translationNames, translations,
        if (activeTranslationsFilesNames == null) quranSettings.activeTranslations else activeTranslationsFilesNames,
        translationItemChangedListener
      ) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
          val type = super.getItemViewType(position)
          val view = super.getView(position, convertView, parent)
          if (type == 0) {
            val holder = view.tag as SpinnerHolder
            val page: Int = currentPage

            val sura =
              quranDisplayData.getSuraNameFromPage(this@PagerActivity, page, true)
            holder.title.text = sura
            holder.title.setTextColor(ResourcesCompat.getColor(resources, R.color.toolbar_text, null))
            val desc = quranDisplayData.getPageSubtitle(this@PagerActivity, page)
            holder.subtitle.text = desc
            holder.subtitle.visibility = View.VISIBLE
            holder.subtitle.setTextColor(ResourcesCompat.getColor(resources, R.color.toolbar_secondary_text, null))
          }
          return view
        }
      }
      translationsSpinner.adapter = translationsSpinnerAdapter
    }

    val actionBar = supportActionBar
    if (actionBar != null) {
      actionBar.setDisplayShowTitleEnabled(false)
      translationsSpinner.visibility = View.VISIBLE
    }
  }

  private fun onDownloadSuccess() {
    refreshQuranPages()
    audioPresenter.onDownloadSuccess()
  }

  fun toggleActionBarVisibility(visible: Boolean) {
    if (visible == isActionBarHidden) {
      toggleActionBar()
    }
  }

  fun toggleActionBar() {
    if (isActionBarHidden) {
      setUiVisibility(true)

      isActionBarHidden = false
    } else {
      handler.removeMessages(MSG_HIDE_ACTIONBAR)
      setUiVisibility(false)

      isActionBarHidden = true
    }
  }

  private fun ensurePage(sura: Int, ayah: Int) {
    val page = quranInfo.getPageFromSuraAyah(sura, ayah)
    if (quranInfo.isValidPage(page)) {
      val position = quranInfo.getPositionFromPage(page, isDualPageVisible)
      if (position != viewPager.currentItem) {
        viewPager.currentItem = position
      }
    }
  }

  private fun requestTranslationsList() {
    translationJob =
      translationListPresenter.registerForTranslations { titles: Array<String>, updatedTranslations: List<LocalTranslation> ->
        var currentActiveTranslationsFilesNames = quranSettings.activeTranslations
        if (currentActiveTranslationsFilesNames.isEmpty() && updatedTranslations.isNotEmpty()) {
          currentActiveTranslationsFilesNames = HashSet()
          val items = updatedTranslations.size
          for (i in 0 until items) {
            currentActiveTranslationsFilesNames.add(updatedTranslations[i].filename)
          }
        }
        activeTranslationsFilesNames = currentActiveTranslationsFilesNames

        if (translationsSpinnerAdapter != null) {
          translationsSpinnerAdapter!!
            .updateItems(titles, updatedTranslations, activeTranslationsFilesNames)
        }
        translationNames = titles
        translations = updatedTranslations
        if (showingTranslation) {
          // Since translation items have changed, need to
          updateActionBarSpinner()
        }
      }
  }

  private fun refreshBookmarksMenu() {
    refreshBookmarksMenu(isCurrentPageReadingBookmarked)
  }

  private fun refreshBookmarksMenu(isBookmarked: Boolean) {
    val menuItem = bookmarksMenuItem
    if (menuItem != null) {
      menuItem.setIcon(if (isBookmarked) com.quran.labs.androidquran.common.toolbar.R.drawable.ic_favorite else com.quran.labs.androidquran.common.toolbar.R.drawable.ic_not_favorite)
    } else {
      supportInvalidateOptionsMenu()
    }
  }

  override fun setPageReadingBookmarkSelected(isBookmarked: Boolean) {
    isCurrentPageReadingBookmarked = isBookmarked
    refreshBookmarksMenu(isBookmarked)
  }

  override fun showReadingBookmarkChangedToast(
    change: ReadingBookmarkChange,
    isEducation: Boolean,
    onUndo: () -> Unit,
    onDismiss: () -> Unit
  ) {
    val rootView = findViewById<ViewGroup>(android.R.id.content)
    readingBookmarkToastView?.let { rootView.removeView(it) }

    val toastView = createReadingBookmarkToastView(
      this,
      change,
      isEducation,
      quranNaming,
      onUndo = onUndo,
      onDismiss = onDismiss
    )
    readingBookmarkToastView = toastView
    rootView.addView(
      toastView,
      FrameLayout.LayoutParams(
        FrameLayout.LayoutParams.MATCH_PARENT,
        FrameLayout.LayoutParams.WRAP_CONTENT,
        Gravity.BOTTOM
      )
    )
    // the sliding panel that hosts the rest of the reading UI claims every touch within its
    // collapsed (audio bar) region for its own drag/tap handling, regardless of what's drawn on
    // top of it there - so rather than fight that, sit the toast just above the audio bar (when
    // one is visible, its own height already accounts for the navigation bar inset) or above the
    // navigation bar directly (when it isn't, e.g. three-button navigation in edge-to-edge mode).
    // side padding covers landscape three-button navigation and display cutouts.
    ViewCompat.setOnApplyWindowInsetsListener(toastView) { view, windowInsets ->
      val insets = windowInsets.getInsets(
        WindowInsetsCompat.Type.navigationBars() or WindowInsetsCompat.Type.displayCutout()
      )
      view.updatePadding(left = insets.left, right = insets.right)
      val params = view.layoutParams as FrameLayout.LayoutParams
      params.bottomMargin = if (audioStatusBar.isVisible) {
        audioStatusBar.height
      } else {
        insets.bottom
      }
      view.layoutParams = params
      windowInsets
    }
    ViewCompat.requestApplyInsets(toastView)
  }

  override fun dismissReadingBookmarkChangedToast() {
    readingBookmarkToastView?.let {
      findViewById<ViewGroup>(android.R.id.content).removeView(it)
    }
    readingBookmarkToastView = null
  }

  // region Audio playback
  private fun currentQariItem(): QariItem {
    return fromQari(this, qariManager.currentQari())
  }

  override fun onContinuePlaybackPressed() {
    handlePlayback(null)
  }

  override fun onPlayPressed() {
    if (memorizationEngine.state.value.isActive) {
      memorizationEngine.stopMemorization()
    }
    val position = viewPager.currentItem
    val page = quranInfo.getPageFromPosition(position, isDualPageVisible)

    // log the event
    quranEventLogger.logAudioPlayback(
      QuranEventLogger.AudioPlaybackSource.PAGE,
      currentQariItem(), isDualPages, showingTranslation, isSplitScreen
    )

    val startSura = quranDisplayData.safelyGetSuraOnPage(page)
    val startAyah = quranInfo.getFirstAyahOnPage(page)
    val startingSuraList = quranInfo.getListOfSurahWithStartingOnPage(page)
    if (startingSuraList.isEmpty() ||
      (startingSuraList.size == 1 && startingSuraList[0] == startSura)
    ) {
      playFromAyah(startSura, startAyah)
    } else {
      promptForMultipleChoicePlay(page, startSura, startAyah, startingSuraList)
    }
  }

  private fun playFromAyah(startSura: Int, startAyah: Int) {
    val page = quranInfo.getPageFromSuraAyah(startSura, startAyah)
    val start = SuraAyah(startSura, startAyah)
    val end = selectionEnd
    // handle the case of multiple ayat being selected and play them as a range if so
    val ending = if ((end == null || start == end || start.after(end))) null else end
    playFromAyah(start, ending, page, 0, 0, ending != null, 1.0f)
  }

  fun playFromAyah(
    start: SuraAyah,
    end: SuraAyah?,
    page: Int,
    verseRepeat: Int,
    rangeRepeat: Int,
    enforceRange: Boolean,
    playbackSpeed: Float
  ) {
    val ending = end
      ?: audioUtils.getLastAyahToPlay(
        start, page,
        quranSettings.preferredDownloadAmount, isDualPageVisible
      )

    if (ending != null) {
      Timber.d(
        "playFromAyah - " + start + ", ending: " +
            ending + " - original: " + end + " -- " +
            quranSettings.preferredDownloadAmount
      )
      val item = currentQariItem()
      val shouldStream = quranSettings.shouldStream()
      audioPresenter.play(
        start,
        ending,
        item,
        verseRepeat,
        rangeRepeat,
        enforceRange,
        playbackSpeed,
        shouldStream
      )
    }
  }

  override fun handleRequiredDownload(downloadIntent: Intent?) {
    var needsPermission = needsPermissionToDownloadOver3g
    if (needsPermission) {
      if (QuranUtils.isOnWifiNetwork(this)) {
        Timber.d("on wifi, don't need permission for download...")
        needsPermission = false
      }
    }

    if (needsPermission) {
      downloadInfoStreams.requestDownloadNetworkPermission()
    } else if (!havePostNotificationPermission(this)) {
      if (canRequestPostNotificationPermission(this)) {
        promptDialog = buildPostPermissionDialog(
          this,
          {
            promptDialog = null
            requestPermissionLauncher!!.launch(Manifest.permission.POST_NOTIFICATIONS)
          }, {
            proceedWithDownload(downloadIntent)
            promptDialog = null
          })
        promptDialog!!.show()
      } else {
        requestPermissionLauncher!!.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    } else {
      proceedWithDownload(downloadIntent)
    }
  }

  override fun proceedWithDownload(downloadIntent: Intent?) {
    if (isActionBarHidden) {
      toggleActionBar()
    }
    downloadInfoStreams.downloadRequested()
    Timber.d("starting service in handleRequiredDownload")
    startService(downloadIntent)
  }

  override fun handlePlayback(request: AudioRequest?) {
    needsPermissionToDownloadOver3g = true
    val intent = Intent(this, AudioService::class.java)
    intent.setAction(AudioService.ACTION_PLAYBACK)
    if (request != null) {
      intent.putExtra(AudioService.EXTRA_PLAY_INFO, request)
    }

    Timber.d("starting service for audio playback")
    startService(intent)
  }

  override fun onPausePressed() {
    startService(
      audioUtils.getAudioIntent(
        this, AudioService.ACTION_PAUSE
      )
    )
  }

  override fun setPlaybackSpeed(speed: Float) {
    val lastAudioRequest = audioStatusRepositoryBridge.audioRequest()
    if (lastAudioRequest != null) {
      val updatedAudioRequest = lastAudioRequest.copy(playbackSpeed = speed)

      val i = Intent(this, AudioService::class.java)
      i.setAction(AudioService.ACTION_UPDATE_SETTINGS)
      i.putExtra(AudioService.EXTRA_PLAY_INFO, updatedAudioRequest)
      startService(i)
    }
  }

  override fun onNextPressed() {
    startService(
      audioUtils.getAudioIntent(
        this,
        AudioService.ACTION_SKIP
      )
    )
  }

  override fun onPreviousPressed() {
    startService(
      audioUtils.getAudioIntent(
        this,
        AudioService.ACTION_REWIND
      )
    )
  }

  override fun onAudioSettingsPressed() {
    showSlider(slidingPagerAdapter.getPagePosition(SlidingPagerAdapter.AUDIO_PAGE))
  }

  override fun onShowQariList() {
    val page = currentPage
    val start = selectionStart
    val end = selectionEnd

    val startSura = quranDisplayData.safelyGetSuraOnPage(page)
    val startAyah = quranInfo.getFirstAyahOnPage(page)

    val starting = start ?: SuraAyah(startSura, startAyah)
    val ending = end
      ?: audioUtils.getLastAyahToPlay(
        starting, page,
        quranSettings.preferredDownloadAmount, isDualPageVisible
      )!!
    val qariListWrapper = QariListWrapper(this, starting, ending)
    overlay.removeAllViews()
    overlay.addView(
      qariListWrapper,
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.MATCH_PARENT
    )
    overlay.visibility = View.VISIBLE
    toggleActionBarVisibility(false)
  }

  override fun onMemorizePressed() {
    openMemorizationSetup(null)
  }

  override fun onPlaylistsPressed() {
    val playlistWrapper = PlaylistSheetWrapper(
      context = this,
      playlistRepository = playlistRepository,
      quranInfo = quranInfo,
      onPlayPlaylist = { playlist ->
        if (playlist.segments.isNotEmpty()) {
          playlistRepository.updateLastPlayed(playlist.id)
          val firstSeg = playlist.segments.first()
          val lastSeg = playlist.segments.last()
          val playConfig = MemorizationConfig(
            startAyah = SuraAyah(firstSeg.sura, firstSeg.startAyah),
            endAyah = SuraAyah(lastSeg.sura, lastSeg.endAyah),
            windowSize = playlist.totalAyahsCount.coerceAtLeast(1),
            singleRepeat = 1,
            connectRepeat = 0,
            delayMode = MemorizationDelayMode.None,
            memoryTestMode = false,
            maskingLevel = MaskingLevel.OFF,
            recordRecitation = false,
            enableBridge = false,
            segments = playlist.segments,
            playlistId = playlist.id,
            playlistName = playlist.name
          )
          memorizationEngine.startMemorization(playConfig)
          playFromAyah(firstSeg.sura, firstSeg.startAyah)
        }
      },
      onMemorizePlaylist = { playlist ->
        openMemorizationSetup(initialPlaylist = playlist)
      },
      onDismissed = {
        overlay.removeAllViews()
        overlay.visibility = View.GONE
        toggleActionBarVisibility(true)
      }
    )
    overlay.removeAllViews()
    overlay.addView(
      playlistWrapper,
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.MATCH_PARENT
    )
    overlay.visibility = View.VISIBLE
    toggleActionBarVisibility(false)
  }

  private fun openMemorizationSetup(initialPlaylist: CustomPlaylist? = null) {
    val page = currentPage
    val start = selectionStart
    val end = selectionEnd

    val pageRange = quranInfo.getVerseRangeForPage(page)
    val starting = start ?: SuraAyah(pageRange.startSura, pageRange.startAyah)
    val ending = end ?: SuraAyah(pageRange.endingSura, pageRange.endingAyah)

    val hasSaved = memorizationPreferences.hasSavedSession()
    val savedConfig = if (hasSaved) memorizationPreferences.getSavedConfig() else null
    val savedStep = if (hasSaved) memorizationPreferences.getSavedStepIndex() else 0
    val masteredRanges = memorizationPreferences.getMasteredRanges()
    val savedPlaylists = playlistRepository.getAllPlaylists()

    val memorizationSetupSheetWrapper = MemorizationSetupSheetWrapper(
      context = this,
      initialStartAyah = starting,
      initialEndAyah = ending,
      savedSessionConfig = savedConfig,
      savedSessionStep = savedStep,
      masteredRanges = masteredRanges,
      savedPlaylists = savedPlaylists,
      initialPlaylist = initialPlaylist,
      onResume = {
        if (savedConfig != null) {
          memorizationEngine.resumeSavedSession()
          val currentAyah = memorizationEngine.state.value.currentPlayingAyah ?: savedConfig.startAyah
          playFromAyah(currentAyah.sura, currentAyah.ayah)
        }
      },
      onStart = { config ->
        if (config.recordRecitation &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
          ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO), 101)
        }
        val playlistId = config.playlistId
        if (playlistId != null) {
          playlistRepository.updateLastPlayed(playlistId)
        }
        memorizationEngine.startMemorization(config)
        playFromAyah(config.startAyah.sura, config.startAyah.ayah)
      },
      onDismissed = {
        overlay.removeAllViews()
        overlay.visibility = View.GONE
        toggleActionBarVisibility(true)
      }
    )
    overlay.removeAllViews()
    overlay.addView(
      memorizationSetupSheetWrapper,
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.MATCH_PARENT
    )
    overlay.visibility = View.VISIBLE
    toggleActionBarVisibility(false)
  }

  private fun showReadingBookmarkSheet(target: ReadingBookmarkTarget) {
    val readingBookmarkSheetWrapper = ReadingBookmarkSheetWrapper(
      context = this,
      target = target,
      onDismissed = {
        overlay.removeAllViews()
        overlay.visibility = View.GONE
        toggleActionBarVisibility(true)
      }
    )
    overlay.removeAllViews()
    overlay.addView(
      readingBookmarkSheetWrapper,
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.MATCH_PARENT
    )
    overlay.visibility = View.VISIBLE
    toggleActionBarVisibility(false)
  }

  private fun showAyahBookmarkSheet(suraAyah: SuraAyah) {
    val ayahBookmarkWrapper = AyahBookmarkWrapper(this, suraAyah) { isBookmarked ->
      overlay.removeAllViews()
      overlay.visibility = View.GONE
      toggleActionBarVisibility(true)
      updateAyahBookmark(suraAyah, isBookmarked)
    }
    overlay.removeAllViews()
    overlay.addView(
      ayahBookmarkWrapper,
      ViewGroup.LayoutParams.MATCH_PARENT,
      ViewGroup.LayoutParams.MATCH_PARENT
    )
    overlay.visibility = View.VISIBLE
    toggleActionBarVisibility(false)
  }

  fun updatePlayOptions(
    rangeRepeat: Int,
    verseRepeat: Int,
    enforceRange: Boolean,
    playbackSpeed: Float
  ): Boolean {
    val lastAudioRequest = audioStatusRepositoryBridge.audioRequest()
    if (lastAudioRequest != null) {
      val updatedAudioRequest = lastAudioRequest.copy(
        repeatInfo = verseRepeat,
        rangeRepeatInfo = rangeRepeat,
        enforceBounds = enforceRange,
        playbackSpeed = playbackSpeed
      )
      val i = Intent(this, AudioService::class.java)
      i.setAction(AudioService.ACTION_UPDATE_SETTINGS)
      i.putExtra(AudioService.EXTRA_PLAY_INFO, updatedAudioRequest)
      startService(i)
      return true
    } else {
      return false
    }
  }

  override fun setRepeatCount(repeatCount: Int) {
    val lastAudioRequest = audioStatusRepositoryBridge.audioRequest()
    if (lastAudioRequest != null) {
      val updatedAudioRequest = lastAudioRequest.copy(repeatInfo = repeatCount)

      val i = Intent(this, AudioService::class.java)
      i.setAction(AudioService.ACTION_UPDATE_SETTINGS)
      i.putExtra(AudioService.EXTRA_PLAY_INFO, updatedAudioRequest)
      startService(i)
    }
  }

  override fun onStopPressed() {
    if (memorizationEngine.state.value.isActive) {
      memorizationEngine.stopMemorization()
    }
    startService(audioUtils.getAudioIntent(this, AudioService.ACTION_STOP))
  }

  override fun onCancelPressed(cancelDownload: Boolean) {
    if (cancelDownload) {
      needsPermissionToDownloadOver3g = true

      val i = Intent(this, QuranDownloadService::class.java)
      i.setAction(QuranDownloadService.ACTION_CANCEL_DOWNLOADS)
      startService(i)
    } else {
      startService(audioUtils.getAudioIntent(this, AudioService.ACTION_STOP))
    }
  }

  override fun onAcceptPressed() {
    needsPermissionToDownloadOver3g = false
    audioPresenter.onDownloadPermissionGranted()
  }

  //endregion

  private val selectionStart: SuraAyah?
    // region Ayah selection
    get() {
      val currentSelection = readingEventPresenter.currentAyahSelection()
      return currentSelection.startSuraAyah()
    }

  private val selectionEnd: SuraAyah?
    get() {
      val currentSelection = readingEventPresenter.currentAyahSelection()
      return currentSelection.endSuraAyah()
    }

  val lastAudioRequest: AudioRequest?
    get() = audioStatusRepositoryBridge.audioRequest()

  fun endAyahMode() {
    readingEventPresenterBridge.clearSelectedAyah()
    slidingPanel.collapsePane()
  }

  //endregion
  private fun updateLocalTranslations(start: SuraAyah?) {
    val ayahTracker = resolveCurrentTracker()
    if (ayahTracker != null) {
      lastActivatedLocalTranslations = ayahTracker.getLocalTranslations() ?: emptyArray()
      lastSelectedTranslationAyah = ayahTracker.getQuranAyahInfo(start!!.sura, start.ayah)
    }
  }

  private fun resolveCurrentTracker(): AyahTracker? {
    val position = viewPager.currentItem
    val f = pagerAdapter.getFragmentIfExists(position)
    return if (f is QuranPage && f.isVisible) {
      (f as QuranPage).getAyahTracker()
    } else {
      null
    }
  }

  private inner class AyahMenuItemSelectionHandler : MenuItem.OnMenuItemClickListener {
    override fun onMenuItemClick(item: MenuItem): Boolean {
      var sliderPage = -1
      val currentSelection = readingEventPresenter.currentAyahSelection()
      val startSuraAyah = currentSelection.startSuraAyah()
      val endSuraAyah = currentSelection.endSuraAyah()
      if (startSuraAyah == null || endSuraAyah == null) {
        return false
      }

      val itemId = item.itemId
      if (itemId == com.quran.labs.androidquran.common.toolbar.R.id.cab_bookmark_ayah) {
        showAyahBookmarkSheet(startSuraAyah)
      } else if (itemId == com.quran.labs.androidquran.common.toolbar.R.id.cab_translate_ayah) {
        sliderPage =
          slidingPagerAdapter.getPagePosition(SlidingPagerAdapter.TRANSLATION_PAGE)
      } else if (itemId == com.quran.labs.androidquran.common.toolbar.R.id.cab_play_from_here) {
        quranEventLogger.logAudioPlayback(
          QuranEventLogger.AudioPlaybackSource.AYAH,
          currentQariItem(), isDualPages, showingTranslation, isSplitScreen
        )
        playFromAyah(startSuraAyah.sura, startSuraAyah.ayah)
        toggleActionBarVisibility(true)
      } else if (itemId == com.quran.labs.androidquran.common.toolbar.R.id.cab_recite_from_here) {
        onEndSessionBackCallback.isEnabled = true
        pagerActivityRecitationPresenter.onRecitationPressed()
      } else if (itemId == com.quran.labs.androidquran.common.toolbar.R.id.cab_share_ayah_link) {
        shareAyahLink(startSuraAyah, endSuraAyah)
      } else if (itemId == com.quran.labs.androidquran.common.toolbar.R.id.cab_share_ayah_text) {
        shareAyah(startSuraAyah, endSuraAyah, false)
      } else if (itemId == com.quran.labs.androidquran.common.toolbar.R.id.cab_copy_ayah) {
        shareAyah(startSuraAyah, endSuraAyah, true)
      } else {
        return false
      }

      if (sliderPage < 0) {
        endAyahMode()
      } else {
        showSlider(sliderPage)
      }
      return true
    }
  }

  private fun shareAyah(start: SuraAyah?, end: SuraAyah?, isCopy: Boolean) {
    if (start == null || end == null) {
      return
    } else if (!quranFileUtils.hasArabicSearchDatabase()) {
      showGetRequiredFilesDialog()
      return
    }

    val translationNames = lastActivatedLocalTranslations
    if (showingTranslation && translationNames.isNotEmpty()) {
      // temporarily required so "lastSelectedTranslationAyah" isn't null
      // the real solution is to move this sharing logic out of PagerActivity
      // in the future and avoid this back and forth with the translation fragment.

      updateLocalTranslations(start)
      val quranAyahInfo = lastSelectedTranslationAyah
      if (quranAyahInfo != null) {
        val shareText = shareUtil.getShareText(this, quranAyahInfo, translationNames)
        if (isCopy) {
          shareUtil.copyToClipboard(this, shareText)
        } else {
          shareUtil.shareViaIntent(
            this,
            shareText,
            com.quran.labs.androidquran.common.toolbar.R.string.share_ayah_text
          )
        }
      }

      return
    }

    compositeDisposable.add(
      arabicDatabaseUtils
        .getVerses(start, end)
        .filter { quranAyahs: List<QuranText?> -> quranAyahs.isNotEmpty() }
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe { quranAyahs: List<QuranText> ->
          if (isCopy) {
            shareUtil.copyVerses(this@PagerActivity, quranAyahs)
          } else {
            shareUtil.shareVerses(this@PagerActivity, quranAyahs)
          }
        })
  }

  fun shareAyahLink(start: SuraAyah, end: SuraAyah) {
    // limit sharing to within a single sura
    val endAyah = if (end.sura == start.sura) end.ayah else quranInfo.getNumberOfAyahs(start.sura)
    val url = buildString {
      append("https://quran.com/${start.sura}/${start.ayah}")
      if (endAyah != start.ayah) {
        append("-$endAyah")
      }
    }
    shareUtil.shareViaIntent(
      this,
      url,
      com.quran.labs.androidquran.common.toolbar.R.string.share_ayah
    )
  }

  private fun showSlider(sliderPage: Int) {
    readingEventPresenterBridge.clearMenuForSelection()
    slidingPager.currentItem = sliderPage
    slidingPanel.showPane()
    // TODO there's got to be a better way than this hack
    // The issue is that smoothScrollTo returns if mCanSlide is false
    // and it's false when the panel is GONE and showPane only calls
    // requestLayout, and only in onLayout does mCanSlide become true.
    // So by posting this later it gives time for onLayout to run.
    handler.post { slidingPanel.expandPane() }
  }

  private fun updateAyahBookmark(suraAyah: SuraAyah, bookmarked: Boolean) {
    // Refresh toolbar icon
    val start = selectionStart
    if (start != null && start == suraAyah) {
      ayahToolBar.setBookmarked(bookmarked)
    }
  }

  private fun promptForMultipleChoicePlay(
    page: Int, startSura: Int, startAyah: Int, startingSuraList: List<Int>
  ) {
    val adapter = ArrayAdapter<String>(this, android.R.layout.select_dialog_item)
    startingSuraList
      .map { quranDisplayData.getSuraName(this, it, false) }
      .onEach { adapter.add(it) }

    val suraList = if (startSura != startingSuraList[0]) {
      adapter.insert(getString(R.string.starting_page_label), 0)
      listOf(startSura) + startingSuraList
    } else {
      startingSuraList
    }

    val builder = AlertDialog.Builder(this)
      .setTitle(getString(R.string.playback_prompt_title))
      .setAdapter(adapter) { dialog: DialogInterface, i: Int ->
        if (i == 0) {
          playFromAyah(startSura, startAyah)
        } else {
          playFromAyah(suraList[i], 1)
        }
        dialog.dismiss()
        promptDialog = null
      }
    promptDialog = builder.create()
    promptDialog!!.show()
  }

  private inner class SlidingPanelListener : SlidingUpPanelLayout.PanelSlideListener {
    override fun onPanelSlide(panel: View, slideOffset: Float) {
    }

    override fun onPanelCollapsed(panel: View) {
      if (selectionStart != null) {
        endAyahMode()
      }
      slidingPanel.hidePane()
      readingEventPresenter.onPanelClosed()
    }

    override fun onPanelExpanded(panel: View) {
      readingEventPresenter.onPanelOpened()
    }

    override fun onPanelAnchored(panel: View) {
    }
  }

  companion object {
    private const val AUDIO_DOWNLOAD_KEY = "AUDIO_DOWNLOAD_KEY"
    private const val LAST_READ_PAGE = "LAST_READ_PAGE"
    private const val LAST_READING_MODE_IS_TRANSLATION = "LAST_READING_MODE_IS_TRANSLATION"
    private const val LAST_ACTIONBAR_STATE = "LAST_ACTIONBAR_STATE"
    private const val LAST_FOLDING_STATE = "LAST_FOLDING_STATE"

    const val EXTRA_JUMP_TO_TRANSLATION: String = "jumpToTranslation"
    const val EXTRA_HIGHLIGHT_SURA: String = "highlightSura"
    const val EXTRA_HIGHLIGHT_AYAH: String = "highlightAyah"
    const val LAST_WAS_DUAL_PAGES: String = "wasDualPages"

    private const val DEFAULT_HIDE_AFTER_TIME: Long = 2000

    const val MSG_HIDE_ACTIONBAR: Int = 1

    // AYAH ACTION PANEL STUFF
    // Max height of sliding panel (% of screen)
    private const val PANEL_MAX_HEIGHT = 0.6f
  }
}

`

---

### app/src/main/java/com/quran/labs/androidquran/ui/listener/AudioBarListener.kt

`kotlin
package com.quran.labs.androidquran.ui.listener


interface AudioBarListener {
  fun onPlayPressed()
  fun onContinuePlaybackPressed()
  fun onPausePressed()
  fun onNextPressed()
  fun onPreviousPressed()
  fun onStopPressed()
  fun setPlaybackSpeed(speed: Float)
  fun onCancelPressed(stopDownload: Boolean)
  fun setRepeatCount(repeatCount: Int)
  fun onAcceptPressed()
  fun onAudioSettingsPressed()
  fun onShowQariList()
  fun onMemorizePressed()
  fun onPlaylistsPressed()
}


`

---

### app/src/main/java/com/quran/labs/androidquran/feature/reading/bridge/AudioStatusRepositoryBridge.kt

`kotlin
package com.quran.labs.androidquran.feature.reading.bridge

import com.quran.data.model.SuraAyah
import com.quran.labs.androidquran.common.audio.model.playback.AudioRequest
import com.quran.labs.androidquran.common.audio.model.playback.AudioStatus
import com.quran.labs.androidquran.common.audio.repository.AudioStatusRepository
import com.quran.labs.androidquran.ui.listener.AudioBarListener
import com.quran.labs.androidquran.ui.listener.AudioBarRecitationListener
import com.quran.mobile.feature.audiobar.presenter.AudioBarEventRepository
import com.quran.mobile.feature.audiobar.state.AudioBarEvent
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class AudioStatusRepositoryBridge(
  audioStatusRepository: AudioStatusRepository,
  audioBarEventRepository: AudioBarEventRepository,
  onPlaybackAyahChanged: ((SuraAyah?) -> Unit),
  audioBarListener: AudioBarListener,
  audioBarRecitationListener: AudioBarRecitationListener
) {

  private val scope = MainScope()
  private val audioPlaybackAyahFlow = audioStatusRepository.audioPlaybackFlow

  init {
    audioPlaybackAyahFlow
      .filterIsInstance<AudioStatus.Playback>()
      .onEach { status -> onPlaybackAyahChanged(status.currentAyah) }
      .launchIn(scope)

    audioBarEventRepository
      .audioBarEventFlow
      .onEach { event ->
        when (event) {
          AudioBarEvent.Acknowledge -> audioBarListener.onAcceptPressed()
          AudioBarEvent.Cancel -> audioBarListener.onCancelPressed(false)
          AudioBarEvent.CancelDownload -> audioBarListener.onCancelPressed(true)
          AudioBarEvent.ChangeQari -> audioBarListener.onShowQariList()
          AudioBarEvent.Memorize -> audioBarListener.onMemorizePressed()
          AudioBarEvent.Playlists -> audioBarListener.onPlaylistsPressed()
          AudioBarEvent.FastForward -> audioBarListener.onNextPressed()
          AudioBarEvent.Pause -> audioBarListener.onPausePressed()
          AudioBarEvent.Play -> audioBarListener.onPlayPressed()
          AudioBarEvent.ResumePlayback -> audioBarListener.onContinuePlaybackPressed()
          AudioBarEvent.Rewind -> audioBarListener.onPreviousPressed()
          is AudioBarEvent.SetRepeat -> audioBarListener.setRepeatCount(event.repeat)
          is AudioBarEvent.SetSpeed -> audioBarListener.setPlaybackSpeed(event.speed)
          AudioBarEvent.ShowSettings -> audioBarListener.onAudioSettingsPressed()
          AudioBarEvent.Stop -> audioBarListener.onStopPressed()
          AudioBarEvent.EndSession -> audioBarRecitationListener.onEndRecitationSessionPressed()
          AudioBarEvent.HideVerses -> audioBarRecitationListener.onHideVersesPressed()
          AudioBarEvent.PauseRecitation -> audioBarRecitationListener.onPauseRecitationPressed()
          AudioBarEvent.PlayRecitation -> audioBarRecitationListener.onPlayRecitationPressed()
          AudioBarEvent.Recitation, AudioBarEvent.Record -> audioBarRecitationListener.onRecitationPressed()
          AudioBarEvent.RecitationLongPress -> audioBarRecitationListener.onRecitationLongPressed()
          AudioBarEvent.Transcript -> audioBarRecitationListener.onRecitationTranscriptPressed()
        }
      }
      .launchIn(scope)
  }

  fun audioRequest(): AudioRequest? {
    return when (val status = audioPlaybackAyahFlow.value) {
      is AudioStatus.Playback -> status.audioRequest
      AudioStatus.Stopped -> null
    }
  }

  fun dispose() {
    scope.cancel()
  }
}

`

---

