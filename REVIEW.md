# Comprehensive Review: Intelligent Quran Memorization Mode

This document consolidates AGENT_TASKS.md with the full source code of all newly created and modified files implementing the Intelligent Quran Memorization system across all phases (Phase 0 through Phase 8).

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

## Full Source Code of Created & Modified Files

### File: common/audio/src/main/java/com/quran/labs/androidquran/common/audio/memorization/MemorizationConfig.kt

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
 * @param startAyah The first Ayah in the memorization session.
 * @param endAyah The last Ayah in the memorization session.
 * @param windowSize Number of Ayahs in each active memorization chunk (default 4).
 * @param singleRepeat Number of times each new Ayah is repeated individually (Rs, default 3).
 * @param connectRepeat Number of times accumulated Ayahs in the window are repeated together (Rc, default 2).
 * @param delayMode Pause between repetitions (None, FixedSeconds, or MatchAyahDuration).
 * @param memoryTestMode When true, masks/blurs Ayah text during CONNECT and recitation steps.
 * @param maskingLevel Progressive masking difficulty (OFF, MASK_ON_CONNECT, PROGRESSIVE_SOLO_AND_CONNECT).
 * @param recordRecitation When true, records user's voice recitation during pauses for self-check replay.
 * @param enableBridge When true, plays a bridge step connecting the previous window with the new window before advancing.
 */
@Parcelize
data class MemorizationConfig(
  val startAyah: SuraAyah,
  val endAyah: SuraAyah,
  val windowSize: Int = 4,
  val singleRepeat: Int = 3,
  val connectRepeat: Int = 2,
  val delayMode: MemorizationDelayMode = MemorizationDelayMode.None,
  val memoryTestMode: Boolean = false,
  val maskingLevel: MaskingLevel = if (memoryTestMode) MaskingLevel.MASK_ON_CONNECT else MaskingLevel.OFF,
  val recordRecitation: Boolean = false,
  val enableBridge: Boolean = true
) : Parcelable {
  init {
    require(windowSize >= 1) { "windowSize must be at least 1" }
    require(singleRepeat >= 1) { "singleRepeat must be at least 1" }
    require(connectRepeat >= 1) { "connectRepeat must be at least 1" }
    require(startAyah <= endAyah) { "startAyah ($startAyah) must be <= endAyah ($endAyah)" }
  }
}

`

---

### File: common/audio/src/main/java/com/quran/labs/androidquran/common/audio/memorization/MemorizationDelayMode.kt

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

### File: common/audio/src/main/java/com/quran/labs/androidquran/common/audio/memorization/MemorizationStep.kt

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

### File: common/audio/src/main/java/com/quran/labs/androidquran/common/audio/memorization/MemorizationState.kt

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

### File: common/audio/src/main/java/com/quran/labs/androidquran/common/audio/memorization/MemorizationPlanBuilder.kt

`kotlin
package com.quran.labs.androidquran.common.audio.memorization

import com.quran.data.core.QuranInfo
import com.quran.data.model.SuraAyah

/**
 * Builds a deterministic, progressive sliding-window plan of steps
 * for Quran memorization according to the algorithm specification.
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
   */
  fun buildPlan(config: MemorizationConfig, quranInfo: QuranInfo): List<MemorizationStep> {
    val allAyahs = getAyahsInRange(config.startAyah, config.endAyah, quranInfo)
    if (allAyahs.isEmpty()) return emptyList()

    val windows = allAyahs.chunked(config.windowSize)
    val totalWindows = windows.size
    val steps = mutableListOf<MemorizationStep>()
    var stepIndex = 0

    for (wIdx in windows.indices) {
      val windowAyahs = windows[wIdx]

      for (j in windowAyahs.indices) {
        val currentAyah = windowAyahs[j]

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
            description = "Ayah ${currentAyah.ayah} Solo"
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
              description = "Recitation Pause (Ayah ${currentAyah.ayah})"
            )
          )
        }

        // 2. Connect Step if 2 or more Ayahs accumulated in current window
        if (j > 0) {
          val connectedAyahs = windowAyahs.subList(0, j + 1)
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
              description = "Connecting Ayah ${connectedAyahs.first().ayah}–${connectedAyahs.last().ayah}"
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
                description = "Recitation Pause (Connecting ${connectedAyahs.first().ayah}–${connectedAyahs.last().ayah})"
              )
            )
          }
        }
      }

      // 3. Sliding Window Bridge Step (between current window and next window)
      if (config.enableBridge && wIdx < totalWindows - 1) {
        val nextWindow = windows[wIdx + 1]
        val bridgeAyahs = listOf(windowAyahs.last(), nextWindow.first())
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
            description = "Bridge Window ${wIdx + 1} ➔ ${wIdx + 2}"
          )
        )
      }
    }

    return steps
  }
}

`

---

### File: common/audio/src/main/java/com/quran/labs/androidquran/common/audio/memorization/MemorizationEngine.kt

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

### File: common/audio/src/main/java/com/quran/labs/androidquran/common/audio/memorization/MemorizationPreferences.kt

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
 * and mastered ranges history.
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
      enableBridge = prefs.getBoolean(KEY_ENABLE_BRIDGE, true)
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

    private const val VALUE_DELAY_NONE = "NONE"
    private const val VALUE_DELAY_FIXED = "FIXED"
    private const val VALUE_DELAY_MATCH = "MATCH"

    private const val DELIMITER_ITEM = "|"
    private const val MAX_MASTERED_RANGES = 30
  }
}

`

---

### File: common/audio/src/main/java/com/quran/labs/androidquran/common/audio/memorization/MasteredRange.kt

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

### File: app/src/main/java/com/quran/labs/androidquran/service/memorization/RecitationRecorder.kt

`kotlin
package com.quran.labs.androidquran.service.memorization

import android.content.Context
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import timber.log.Timber
import java.io.File

/**
 * Handles recording the user's voice recitation during silent recitation pauses
 * and optional self-check playback against the Qari.
 */
class RecitationRecorder(private val context: Context) {

  private var mediaRecorder: MediaRecorder? = null
  private var currentOutputFile: File? = null
  private var isRecording = false
  private var activePlayer: MediaPlayer? = null

  /**
   * Starts recording user recitation into the specified output file.
   */
  fun startRecording(outputFile: File): Boolean {
    stopRecording()
    stopPlayback()

    return try {
      if (outputFile.exists()) {
        outputFile.delete()
      }

      val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        MediaRecorder(context)
      } else {
        @Suppress("DEPRECATION")
        MediaRecorder()
      }

      recorder.apply {
        setAudioSource(MediaRecorder.AudioSource.MIC)
        setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        setAudioEncodingBitRate(128000)
        setAudioSamplingRate(44100)
        setOutputFile(outputFile.absolutePath)
        prepare()
        start()
      }

      mediaRecorder = recorder
      currentOutputFile = outputFile
      isRecording = true
      Timber.d("RecitationRecorder: recording started to ${outputFile.absolutePath}")
      true
    } catch (e: Exception) {
      Timber.e(e, "RecitationRecorder: failed to start recording")
      cleanup()
      false
    }
  }

  /**
   * Stops the active recording and returns the file path if successful.
   */
  fun stopRecording(): String? {
    if (!isRecording) return null
    val file = currentOutputFile
    try {
      mediaRecorder?.stop()
      mediaRecorder?.release()
    } catch (e: Exception) {
      Timber.e(e, "RecitationRecorder: error stopping recorder")
    } finally {
      mediaRecorder = null
      isRecording = false
    }

    return if (file != null && file.exists() && file.length() > 0) {
      Timber.d("RecitationRecorder: recording stopped successfully, size = ${file.length()} bytes")
      file.absolutePath
    } else {
      null
    }
  }

  /**
   * Cancels recording and deletes any partial output file.
   */
  fun cancelRecording() {
    stopRecording()
    cleanupFile()
  }

  /**
   * Plays back the recorded recitation.
   */
  fun playRecording(audioPath: String, onCompletion: () -> Unit): Boolean {
    stopPlayback()
    val file = File(audioPath)
    if (!file.exists() || file.length() == 0L) return false

    return try {
      val player = MediaPlayer().apply {
        setDataSource(file.absolutePath)
        setOnCompletionListener {
          it.release()
          if (activePlayer == it) activePlayer = null
          onCompletion()
        }
        prepare()
        start()
      }
      activePlayer = player
      true
    } catch (e: Exception) {
      Timber.e(e, "RecitationRecorder: playback error")
      false
    }
  }

  /**
   * Stops active playback.
   */
  fun stopPlayback() {
    try {
      activePlayer?.stop()
      activePlayer?.release()
    } catch (e: Exception) {
      Timber.e(e, "RecitationRecorder: error stopping playback")
    } finally {
      activePlayer = null
    }
  }

  fun isCurrentlyRecording(): Boolean = isRecording

  fun isCurrentlyPlaying(): Boolean = activePlayer?.isPlaying == true

  private fun cleanup() {
    try {
      mediaRecorder?.release()
    } catch (ignored: Exception) {}
    mediaRecorder = null
    isRecording = false
    cleanupFile()
  }

  private fun cleanupFile() {
    try {
      currentOutputFile?.delete()
    } catch (ignored: Exception) {}
    currentOutputFile = null
  }
}

`

---

### File: feature/audiobar/src/main/kotlin/com/quran/mobile/feature/audiobar/ui/MemorizationHud.kt

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

### File: feature/audiobar/src/main/kotlin/com/quran/mobile/feature/audiobar/ui/MemorizationSetupSheet.kt

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
  sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
) {
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

            val config = MemorizationConfig(
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

### File: feature/audiobar/src/main/kotlin/com/quran/mobile/feature/audiobar/ui/MemorizationSetupSheetWrapper.kt

`kotlin
package com.quran.mobile.feature.audiobar.ui

import android.content.Context
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import com.quran.data.model.SuraAyah
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
  private val onReview: ((MasteredRange) -> Unit)? = null
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

### File: feature/audiobar/src/main/kotlin/com/quran/mobile/feature/audiobar/AudioBarWrapper.kt

`kotlin
package com.quran.mobile.feature.audiobar

import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import app.cash.molecule.RecompositionMode
import app.cash.molecule.launchMolecule
import com.quran.labs.androidquran.common.ui.core.QuranTheme
import com.quran.mobile.feature.audiobar.presenter.AudioBarPresenter
import com.quran.mobile.feature.audiobar.ui.AudioBar
import dev.zacsweers.metro.Inject

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.quran.labs.androidquran.common.audio.memorization.MemorizationEngine
import com.quran.mobile.feature.audiobar.state.AudioBarUiEvent
import com.quran.mobile.feature.audiobar.ui.MemorizationHud

class AudioBarWrapper @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : AbstractComposeView(context, attrs, defStyleAttr) {

  @Inject
  lateinit var audioBarPresenter: AudioBarPresenter

  @Inject
  lateinit var memorizationEngine: MemorizationEngine

  init {
    (context as? AudioBarInjector)?.inject(this)
  }

  @Composable
  override fun Content() {
    QuranTheme {
      val scope = rememberCoroutineScope()
      val flow = remember {
        scope.launchMolecule(mode = RecompositionMode.ContextClock) {
          audioBarPresenter.audioBarPresenter()
        }
      }
      val eventListeners = remember(audioBarPresenter) { audioBarPresenter.eventListeners() }
      val memorizationState by memorizationEngine.state.collectAsState()

      Card(
        shape = CardDefaults.shape.topOnly(),
        modifier = Modifier.fillMaxWidth()
      ) {
        if (memorizationState.isActive) {
          MemorizationHud(
            state = memorizationState,
            onPreviousStep = { memorizationEngine.previousStep() },
            onPlayPause = {
              if (memorizationState.isPaused) {
                memorizationEngine.resume()
                eventListeners.pausedPlaybackEventSink(AudioBarUiEvent.PausedPlaybackEvent.Play)
              } else {
                memorizationEngine.pause()
                eventListeners.playingPlaybackEventSink(AudioBarUiEvent.PlayingPlaybackEvent.Pause)
              }
            },
            onSkipStep = { memorizationEngine.skipCurrentStep() },
            onStop = {
              memorizationEngine.stopMemorization()
              eventListeners.commonPlaybackEventSink(AudioBarUiEvent.CommonPlaybackEvent.Stop)
            },
            onToggleMask = { memorizationEngine.toggleTextMask() },
            onReplayRecitation = {
              val path = memorizationState.lastRecitationAudioPath
              if (path != null) {
                try {
                  android.media.MediaPlayer().apply {
                    setDataSource(path)
                    setOnCompletionListener { it.release() }
                    prepare()
                    start()
                  }
                } catch (e: Exception) {
                  android.util.Log.e("AudioBarWrapper", "Error playing recorded recitation", e)
                }
              }
            },
            modifier = Modifier
              .padding(
                WindowInsets.navigationBars.add(WindowInsets.displayCutout)
                    .only(WindowInsetsSides.Horizontal.plus(WindowInsetsSides.Bottom))
                  .asPaddingValues()
              )
              .padding(top = 4.dp)
              .padding(horizontal = 8.dp)
          )
        } else {
          AudioBar(
            flow,
            eventListeners,
            modifier = Modifier
              .padding(
                WindowInsets.navigationBars.add(WindowInsets.displayCutout)
                    .only(WindowInsetsSides.Horizontal.plus(WindowInsetsSides.Bottom))
                  .asPaddingValues()
              )
              .padding(top = 8.dp)
              .padding(horizontal = 16.dp)
              .heightIn(min = dimensionResource(id = R.dimen.audiobar_height))
          )
        }
      }
    }
  }

  private fun Shape.topOnly(): Shape {
    return if (this is RoundedCornerShape) {
      this.copy(
        bottomStart = CornerSize(0.dp),
        bottomEnd = CornerSize(0.dp)
      )
    } else {
      this
    }
  }
}

`

---

### File: feature/audiobar/src/main/kotlin/com/quran/mobile/feature/audiobar/ui/StoppedAudioBar.kt

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

### File: app/src/main/java/com/quran/labs/androidquran/service/AudioService.kt

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

### File: app/src/main/java/com/quran/labs/androidquran/ui/PagerActivity.kt

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
import com.quran.labs.androidquran.common.audio.memorization.MemorizationEngine
import com.quran.labs.androidquran.common.audio.memorization.MemorizationPreferences
import com.quran.mobile.feature.audiobar.ui.MemorizationSetupSheetWrapper
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

    val memorizationSetupSheetWrapper = MemorizationSetupSheetWrapper(
      context = this,
      initialStartAyah = starting,
      initialEndAyah = ending,
      savedSessionConfig = savedConfig,
      savedSessionStep = savedStep,
      masteredRanges = masteredRanges,
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

### File: app/src/main/java/com/quran/labs/androidquran/ui/helpers/HighlightTypes.kt

`kotlin
package com.quran.labs.androidquran.ui.helpers

import com.quran.data.model.bookmark.ReadingBookmarkType
import com.quran.data.model.highlight.HighlightColor
import com.quran.data.model.highlight.HighlightType
import com.quran.data.model.highlight.HighlightType.Mode.HIGHLIGHT
import com.quran.data.model.highlight.HighlightType.Mode.UNDERLAY
import com.quran.data.model.highlight.HighlightType.Mode.UNDERLINE
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.common.ui.core.HighlightColors
import com.quran.mobile.common.ui.core.R as UiCoreR

object HighlightTypes {

  @JvmField
  val SELECTION = HighlightType(1,  R.color.selection_highlight, HIGHLIGHT, isSingle = true)
  @JvmField
  val AUDIO =     HighlightType(2,  R.color.audio_highlight,     HIGHLIGHT, isSingle = true, isTransitionAnimated = true)
  val NOTE =      HighlightType(3,  R.color.note_highlight,      HIGHLIGHT)
  @JvmField
  val BOOKMARK =  HighlightType(4,  R.color.bookmark_highlight,  HIGHLIGHT)
  @JvmField
  val MEMORIZATION_MASK = HighlightType(
    100L,
    R.color.memorization_mask_color,
    HIGHLIGHT,
    isSingle = false,
    nightColorResId = R.color.memorization_mask_color_night
  )
  @JvmField
  val MEMORIZATION_WINDOW = HighlightType(
    101L,
    R.color.memorization_window_color,
    HIGHLIGHT,
    isSingle = false,
    nightColorResId = R.color.memorization_window_color_night
  )

  private const val FIRST_HIGHLIGHT_ID = 5L

  private val HIGHLIGHTS: Map<HighlightColor, HighlightType> =
    HighlightColors.sorted.mapIndexed { index, spec ->
      spec.highlightColor to
          HighlightType(FIRST_HIGHLIGHT_ID + index, spec.colorResourceId, UNDERLAY)
    }.toMap()

  val highlights: Collection<HighlightType> = HIGHLIGHTS.values

  operator fun get(highlightColor: HighlightColor): HighlightType = HIGHLIGHTS.getValue(highlightColor)

  private val FIRST_READING_BOOKMARK_ID = FIRST_HIGHLIGHT_ID + HIGHLIGHTS.size

  private val READING_BOOKMARKS: Map<ReadingBookmarkType, HighlightType> = mapOf(
    ReadingBookmarkType.TEAL to HighlightType(
      FIRST_READING_BOOKMARK_ID,
      UiCoreR.color.reading_bookmark_page_teal,
      UNDERLINE,
      nightColorResId = UiCoreR.color.reading_bookmark_page_teal_night
    ),
    ReadingBookmarkType.ORANGE to HighlightType(
      FIRST_READING_BOOKMARK_ID + 1,
      UiCoreR.color.reading_bookmark_page_orange,
      UNDERLINE,
      nightColorResId = UiCoreR.color.reading_bookmark_page_orange_night
    ),
    ReadingBookmarkType.RED to HighlightType(
      FIRST_READING_BOOKMARK_ID + 2,
      UiCoreR.color.reading_bookmark_page_red,
      UNDERLINE,
      nightColorResId = UiCoreR.color.reading_bookmark_page_red_night
    )
  )

  val readingBookmarks: Collection<HighlightType> = READING_BOOKMARKS.values

  operator fun get(slot: ReadingBookmarkType): HighlightType = READING_BOOKMARKS.getValue(slot)

  fun getAnimationConfig(type: HighlightType): HighlightAnimationConfig = when(type) {
    AUDIO -> HighlightAnimationConfig.Audio
    else -> HighlightAnimationConfig.None
  }
}

`

---

### File: feature/linebyline/src/main/java/com/quran/labs/androidquran/extra/feature/linebyline/model/HighlightType.kt

`kotlin
package com.quran.labs.androidquran.extra.feature.linebyline.model

import com.quran.data.model.highlight.HighlightColor

sealed interface HighlightType {
  data object Selection : HighlightType
  data object Audio : HighlightType
  data object AudioWord : HighlightType
  data object Bookmark : HighlightType
  data object MemorizationMask : HighlightType
  data object MemorizationWindow : HighlightType

  data class Highlight(val color: HighlightColor) : HighlightType
}

`

---

### File: app/src/main/java/com/quran/labs/androidquran/presenter/quran/ayahtracker/AyahTrackerPresenter.kt

`kotlin
package com.quran.labs.androidquran.presenter.quran.ayahtracker

import android.app.Activity
import android.graphics.RectF
import android.view.MotionEvent
import android.widget.ImageView
import com.quran.data.core.QuranInfo
import com.quran.data.dao.BookmarksDao
import com.quran.data.dao.HighlightsDao
import com.quran.data.dao.ReadingBookmarksDao
import com.quran.data.di.QuranPageScope
import com.quran.data.model.AyahGlyph.AyahEndGlyph
import com.quran.data.model.AyahGlyph.WordGlyph
import com.quran.data.model.AyahWord
import com.quran.data.model.SuraAyah
import com.quran.data.model.bookmark.AyahReadingBookmark
import com.quran.data.model.bookmark.Bookmark
import com.quran.data.model.bookmark.ReadingBookmark
import com.quran.data.model.highlight.Highlight
import com.quran.data.model.highlight.HighlightInfo
import com.quran.data.model.highlight.HighlightType
import com.quran.data.model.selection.AyahSelection
import com.quran.data.model.selection.SelectionIndicator
import com.quran.data.model.selection.startSuraAyah
import com.quran.labs.androidquran.common.QuranAyahInfo
import com.quran.labs.androidquran.common.audio.memorization.MemorizationEngine
import com.quran.labs.androidquran.common.audio.memorization.MemorizationState
import com.quran.labs.androidquran.common.audio.model.playback.currentPlaybackAyah
import com.quran.labs.androidquran.common.audio.repository.AudioStatusRepository
import com.quran.labs.androidquran.data.QuranDisplayData
import com.quran.labs.androidquran.data.SuraAyahIterator
import com.quran.labs.androidquran.presenter.Presenter
import com.quran.labs.androidquran.presenter.quran.ayahtracker.AyahTrackerPresenter.AyahInteractionHandler
import com.quran.labs.androidquran.ui.PagerActivity
import com.quran.labs.androidquran.ui.helpers.AyahSelectedListener.EventType
import com.quran.labs.androidquran.ui.helpers.AyahSelectedListener.EventType.DOUBLE_TAP
import com.quran.labs.androidquran.ui.helpers.AyahSelectedListener.EventType.LONG_PRESS
import com.quran.labs.androidquran.ui.helpers.AyahSelectedListener.EventType.SINGLE_TAP
import com.quran.labs.androidquran.ui.helpers.AyahTracker
import com.quran.labs.androidquran.ui.helpers.HighlightTypes
import com.quran.labs.androidquran.util.QuranFileUtils
import com.quran.labs.androidquran.util.QuranSettings
import com.quran.mobile.translation.model.LocalTranslation
import com.quran.page.common.data.AyahCoordinates
import com.quran.page.common.data.PageCoordinates
import com.quran.reading.common.ReadingEventPresenter
import com.quran.recitation.events.RecitationEventPresenter
import com.quran.recitation.presenter.RecitationHighlightsPresenter
import com.quran.recitation.presenter.RecitationHighlightsPresenter.HighlightAction
import com.quran.recitation.presenter.RecitationHighlightsPresenter.RecitationPage
import com.quran.recitation.presenter.RecitationPopupPresenter
import com.quran.recitation.presenter.RecitationPopupPresenter.PopupContainer
import com.quran.recitation.presenter.RecitationPresenter
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@QuranPageScope
class AyahTrackerPresenter @Inject constructor(
  private val quranInfo: QuranInfo,
  private val quranFileUtils: QuranFileUtils,
  private val quranDisplayData: QuranDisplayData,
  private val quranSettings: QuranSettings,
  private val readingEventPresenter: ReadingEventPresenter,
  private val bookmarksDao: BookmarksDao,
  private val highlightsDao: HighlightsDao,
  private val readingBookmarksDao: ReadingBookmarksDao,
  private val audioStatusRepository: AudioStatusRepository,
  private val memorizationEngine: MemorizationEngine,
  recitationPresenter: RecitationPresenter,
  private val recitationEventPresenter: RecitationEventPresenter,
  private val recitationPopupPresenter: RecitationPopupPresenter,
  private val recitationHighlightsPresenter: RecitationHighlightsPresenter,
) : AyahTracker, Presenter<AyahInteractionHandler>, PopupContainer, RecitationPage {
  // we may bind and unbind several times, and each time we unbind, we cancel
  // the scope, which means we can't launch new coroutines in that same scope.
  // thus, leave this as a var so we replace it every time.
  private lateinit var scope: CoroutineScope

  private var items: Array<AyahTrackerItem> = emptyArray()
  private var pendingHighlightInfo: HighlightInfo? = null
  private var lastHighlightedAyah: SuraAyah? = null
  private var lastHighlightedAudioAyah: SuraAyah? = null

  private val isRecitationEnabled = recitationPresenter.isRecitationEnabled()

  private fun subscribe() {
    readingEventPresenter.ayahSelectionFlow
      .onEach { onAyahSelectionChanged(it) }
      .launchIn(scope)

    audioStatusRepository.audioPlaybackFlow
      .onEach { onAudioSelectionChanged(it.currentPlaybackAyah()) }
      .launchIn(scope)

    items.forEach { trackerItem ->
      bookmarksDao.bookmarksForPage(trackerItem.page)
        .onEach { bookmarks -> onBookmarksChanged(bookmarks) }
        .launchIn(scope)
    }

    highlightsDao.highlightsFlow()
      .onEach { highlights -> onHighlightsChanged(highlights) }
      .launchIn(scope)

    readingBookmarksDao.readingBookmarksFlow()
      .onEach { readingBookmarks -> onReadingBookmarksChanged(readingBookmarks) }
      .launchIn(scope)

    memorizationEngine.state
      .onEach { onMemorizationStateChanged(it) }
      .launchIn(scope)
  }

  fun setPageBounds(pageCoordinates: PageCoordinates) {
    for (item in items) {
      item.onSetPageBounds(pageCoordinates)
    }
  }

  fun setAyahCoordinates(ayahCoordinates: AyahCoordinates) {
    for (item in items) {
      item.onSetAyahCoordinates(ayahCoordinates)
    }

    val pendingHighlightInfo = pendingHighlightInfo
    if (pendingHighlightInfo != null && ayahCoordinates.ayahCoordinates.isNotEmpty()) {
      highlightAyah(
        pendingHighlightInfo.sura, pendingHighlightInfo.ayah, pendingHighlightInfo.word,
        pendingHighlightInfo.highlightType, pendingHighlightInfo.scrollToAyah
      )
    }

    if (isRecitationEnabled) {
      recitationHighlightsPresenter.refresh()
    }
  }

  private fun onAyahSelectionChanged(ayahSelection: AyahSelection) {
    val startSuraAyah = ayahSelection.startSuraAyah()

    // optimization - if the current ayah is still highlighted, don't issue a request
    // to unhighlight.
    if (startSuraAyah != lastHighlightedAyah) {
      unHighlightAyahs(HighlightTypes.SELECTION)
      lastHighlightedAyah = startSuraAyah
    }

    when (ayahSelection) {
      is AyahSelection.Ayah -> {
        val suraAyah = ayahSelection.suraAyah
        val scrollToAyah = ayahSelection.selectionIndicator == SelectionIndicator.ScrollOnly
        highlightAyah(suraAyah.sura, suraAyah.ayah, -1, HighlightTypes.SELECTION, scrollToAyah)
      }
      is AyahSelection.AyahRange -> {
        val highlightAyatIterator =
          SuraAyahIterator(quranInfo, ayahSelection.startSuraAyah, ayahSelection.endSuraAyah)
        val highlightedAyat = highlightAyatIterator.asSet()
        items.forEach {
          val pageAyat = quranDisplayData.getAyahKeysOnPage(it.page)
          val elements = pageAyat.intersect(highlightedAyat)
          if (elements.isNotEmpty()) {
            it.onHighlightAyat(it.page, elements, HighlightTypes.SELECTION)
          }
        }
      }
      else -> { /* nothing is selected, and we already cleared */ }
    }
  }

  private fun onAudioSelectionChanged(suraAyah: SuraAyah?) {
    val currentLastHighlightAudioAyah = lastHighlightedAudioAyah

    // if there is no currently highlighted audio ayah, go ahead and clear all
    if (currentLastHighlightAudioAyah == null) {
      unHighlightAyahs(HighlightTypes.AUDIO)
    }

    // either way, whether we unhighlighted all or not, highlight the new ayah
    if (suraAyah != null) {
      highlightAyah(suraAyah.sura, suraAyah.ayah, -1, HighlightTypes.AUDIO, true)
    }

    // if we had a highlighted ayah before, unhighlight it now.
    // we do this *after* highlighting the new ayah so that the animations continue working.
    if (suraAyah != currentLastHighlightAudioAyah && currentLastHighlightAudioAyah != null) {
      val sura = currentLastHighlightAudioAyah.sura
      val ayah = currentLastHighlightAudioAyah.ayah
      val page = quranInfo.getPageFromSuraAyah(sura, ayah)

      items.filter { it.page == page }
        .onEach {
          it.onUnHighlightAyah(page, sura, ayah, -1, HighlightTypes.AUDIO)
        }
    }

    // and keep track of the last highlighted audio ayah
    lastHighlightedAudioAyah = suraAyah
  }

  private fun onBookmarksChanged(bookmarks: List<Bookmark>) {
    unHighlightAyahs(HighlightTypes.BOOKMARK)
    if (quranSettings.shouldHighlightBookmarks()) {
      items.forEach { tracker ->
        val elements = bookmarks
          .filter { it.page == tracker.page }
          .map { "${it.sura}:${it.ayah}" }
          .toSet()
        if (elements.isNotEmpty()) {
          tracker.onHighlightAyat(tracker.page, elements, HighlightTypes.BOOKMARK)
        }
      }
    }
  }

  private fun onHighlightsChanged(highlights: List<Highlight>) {
    HighlightTypes.highlights.forEach { unHighlightAyahs(it) }

    val highlightsByColor = highlights.groupBy { it.color }
    items.forEach { tracker ->
      highlightsByColor.forEach { (color, colorHighlights) ->
        val elements = colorHighlights
          .map { it.suraAyah }
          .filter { quranInfo.getPageFromSuraAyah(it.sura, it.ayah) == tracker.page }
          .map { "${it.sura}:${it.ayah}" }
          .toSet()
        if (elements.isNotEmpty()) {
          tracker.onHighlightAyat(tracker.page, elements, HighlightTypes[color])
        }
      }
    }
  }

  private fun onReadingBookmarksChanged(readingBookmarks: List<ReadingBookmark>) {
    HighlightTypes.readingBookmarks.forEach { unHighlightAyahs(it) }

    val ayahReadingBookmarks = readingBookmarks.filterIsInstance<AyahReadingBookmark>()
    items.forEach { tracker ->
      ayahReadingBookmarks
        .filter { quranInfo.getPageFromSuraAyah(it.sura, it.ayah) == tracker.page }
        .forEach {
          tracker.onHighlightAyat(tracker.page, setOf("${it.sura}:${it.ayah}"), HighlightTypes[it.slot])
        }
    }
  }

  private fun onMemorizationStateChanged(memState: MemorizationState) {
    unHighlightAyahs(HighlightTypes.MEMORIZATION_MASK)
    unHighlightAyahs(HighlightTypes.MEMORIZATION_WINDOW)

    if (!memState.isActive) {
      return
    }

    // 1. Opaque text masking for active Ayahs during memory test mode
    if (memState.isTextMasked && memState.activeAyahsInStep.isNotEmpty()) {
      val maskedKeys = memState.activeAyahsInStep.map { "${it.sura}:${it.ayah}" }.toSet()
      items.forEach { tracker ->
        val pageAyat = quranDisplayData.getAyahKeysOnPage(tracker.page)
        val elements = pageAyat.intersect(maskedKeys)
        if (elements.isNotEmpty()) {
          tracker.onHighlightAyat(tracker.page, elements, HighlightTypes.MEMORIZATION_MASK)
        }
      }
    }

    // 2. Sliding window boundary framing
    val currentStep = memState.currentStep
    if (currentStep != null && currentStep.ayahs.isNotEmpty()) {
      val windowKeys = currentStep.ayahs.map { "${it.sura}:${it.ayah}" }.toSet()
      items.forEach { tracker ->
        val pageAyat = quranDisplayData.getAyahKeysOnPage(tracker.page)
        val elements = pageAyat.intersect(windowKeys)
        if (elements.isNotEmpty()) {
          tracker.onHighlightAyat(tracker.page, elements, HighlightTypes.MEMORIZATION_WINDOW)
        }
      }
    }
  }

  private fun highlightAyah(sura: Int, ayah: Int, word: Int, type: HighlightType, scrollToAyah: Boolean) {
    val page = quranInfo.getPageFromSuraAyah(sura, ayah)
    val handled = items.any {
      it.page == page && it.onHighlightAyah(page, sura, ayah, word, type, scrollToAyah)
    }

    pendingHighlightInfo = if (!handled) {
      HighlightInfo(sura, ayah, word, type, scrollToAyah)
    } else {
      null
    }
  }

  private fun unHighlightAyahs(type: HighlightType) {
    for (item in items) {
      item.onUnHighlightAyahType(type)
    }
  }

  override fun getToolBarPosition(sura: Int, ayah: Int): SelectionIndicator {
    val page = quranInfo.getPageFromSuraAyah(sura, ayah)
    for (item in items) {
      val position = item.getToolBarPosition(page, sura, ayah)
      if (position != SelectionIndicator.None && position != SelectionIndicator.ScrollOnly) {
        return position
      }
    }
    return SelectionIndicator.None
  }

  override fun getQuranAyahInfo(sura: Int, ayah: Int): QuranAyahInfo? {
    for (item in items) {
      val quranAyahInfo = item.getQuranAyahInfo(sura, ayah)
      if (quranAyahInfo != null) {
        return quranAyahInfo
      }
    }
    return null
  }

  override fun getLocalTranslations(): Array<LocalTranslation>? {
    for (item in items) {
      val localTranslations = item.getLocalTranslations()
      if (localTranslations != null) {
        return localTranslations
      }
    }
    return null
  }

  fun onPressIgnoringSelectionState() {
    readingEventPresenter.onClick()
  }

  fun onLongPress(suraAyah: SuraAyah) {
    handleLongPress(suraAyah)
  }

  fun endAyahMode() {
    readingEventPresenter.onAyahSelection(AyahSelection.None)
  }

  fun handleTouchEvent(
    activity: Activity,
    event: MotionEvent,
    eventType: EventType,
    page: Int,
    ayahCoordinatesError: Boolean
  ): Boolean {
    if (eventType === DOUBLE_TAP) {
      readingEventPresenter.onAyahSelection(AyahSelection.None)
    } else if (eventType == LONG_PRESS ||
      readingEventPresenter.currentAyahSelection() != AyahSelection.None
    ) {
      // press or long press when an ayah is selected
      if (ayahCoordinatesError) {
        checkCoordinateData(activity)
      } else {
        // either a press or a long press, but we're in selection mode
        handleAyahSelection(event, eventType, page)
      }
    } else if (eventType == SINGLE_TAP && recitationEventPresenter.hasRecitationSession()) {
      handleTap(event, eventType, page)
    } else {
      // normal click
      readingEventPresenter.onClick()
    }
    return true
  }

  private fun handleAyahSelection(
    ev: MotionEvent,
    eventType: EventType,
    page: Int
  ) {
    val result = getAyahForPosition(page, ev.x, ev.y)
    if (result != null) {
      if (eventType == SINGLE_TAP) {
        readingEventPresenter.onAyahSelection(
          AyahSelection.Ayah(result, getToolBarPosition(result.sura, result.ayah))
        )
      } else if (eventType == LONG_PRESS) {
        handleLongPress(result)
      }
    }
  }

  private fun handleTap(
    ev: MotionEvent,
    eventType: EventType,
    page: Int
  ) {
    for (item in items) {
      val glyph = item.getGlyphForPosition(page, ev.x, ev.y) ?: continue

      val portion = when (glyph) {
        is WordGlyph -> glyph.toAyahWord()
        is AyahEndGlyph -> glyph.ayah
        else -> glyph.ayah
      }

      readingEventPresenter.onClick(portion)
    }
  }

  private fun handleLongPress(selectedSuraAyah: SuraAyah) {
    val current = readingEventPresenter.currentAyahSelection()
    val updatedAyahSelection = updateAyahRange(selectedSuraAyah, current)
    readingEventPresenter.onAyahSelection(updatedAyahSelection)
  }

  private fun updateAyahRange(selectedAyah: SuraAyah, ayahSelection: AyahSelection): AyahSelection {
    val (startAyah, endAyah) = when (ayahSelection) {
      is AyahSelection.None -> selectedAyah to null
      is AyahSelection.Ayah -> {
        if (selectedAyah > ayahSelection.suraAyah) {
          ayahSelection.suraAyah to selectedAyah
        } else {
          selectedAyah to ayahSelection.suraAyah
        }
      }
      is AyahSelection.AyahRange -> {
        if (selectedAyah > ayahSelection.startSuraAyah) {
          ayahSelection.startSuraAyah to selectedAyah
        } else {
          selectedAyah to ayahSelection.startSuraAyah
        }
      }
    }

    val toolBarPosition = getToolBarPosition(selectedAyah.sura, selectedAyah.ayah)
    return if (endAyah == null) {
      AyahSelection.Ayah(startAyah, toolBarPosition)
    } else {
      AyahSelection.AyahRange(startAyah, endAyah, toolBarPosition)
    }
  }

  private fun getAyahForPosition(page: Int, x: Float, y: Float): SuraAyah? {
    for (item in items) {
      val ayah = item.getAyahForPosition(page, x, y)
      if (ayah != null) {
        return ayah
      }
    }
    return null
  }

  private fun checkCoordinateData(activity: Activity) {
    if (activity is PagerActivity &&
      (!quranFileUtils.haveAyaPositionFile() ||
          !quranFileUtils.hasArabicSearchDatabase())
    ) {
      activity.showGetRequiredFilesDialog()
    }
  }

  override fun bind(what: AyahInteractionHandler) {
    items = what.getAyahTrackerItems()
    scope = MainScope()
    if (isRecitationEnabled) {
      recitationPopupPresenter.bind(this)
      recitationHighlightsPresenter.bind(this)
    }
    subscribe()
  }

  override fun unbind(what: AyahInteractionHandler) {
    if (isRecitationEnabled) {
      recitationHighlightsPresenter.unbind(this)
      recitationPopupPresenter.unbind(this)
    }
    items = emptyArray()
    scope.cancel()
  }

  interface AyahInteractionHandler {
    fun getAyahTrackerItems(): Array<AyahTrackerItem>
  }

  // PopupContainer <--> AyahTrackerItem adapter

  override fun getQuranPageImageView(page: Int): ImageView? {
    val item = items.firstOrNull { it.page == page } ?: return null
    val ayahView = item.getAyahView() ?: return null
    return ayahView as? ImageView ?: return null
  }

  override fun getSelectionBoundsForWord(
    page: Int,
    word: AyahWord
  ): SelectionIndicator.SelectedItemPosition? {
    val item = items.firstOrNull { it.page == page } ?: return null
    val selection = item.getToolBarPosition(item.page, word) as? SelectionIndicator.SelectedItemPosition ?: return null
    return selection
  }

  override fun getBoundsForWord(word: AyahWord): List<RectF>? {
    return (items[0] as? AyahImageTrackerItem)?.pageGlyphsCoords?.getBoundsForWord(word)
  }

  // RecitationPage <--> AyahTrackerItem adapter

  override val pageNumbers: Set<Int>
    get() = items.map { it.page }.toSet()

  override fun applyHighlights(highlights: List<HighlightAction>) {
    highlights.forEach {
      when (it) {
        is HighlightAction.Highlight -> highlight(it.highlightInfo)
        is HighlightAction.Unhighlight -> unhighlight(it.highlightInfo)
        is HighlightAction.UnhighlightAll -> unHighlightAyahs(it.highlightType, it.page)
      }
    }
  }

  private fun highlight(highlightInfo: HighlightInfo) {
    val page = quranInfo.getPageFromSuraAyah(highlightInfo.sura, highlightInfo.ayah)
    items.asSequence()
      .filter { it.page == page }
      .forEach { it.onHighlight(page, highlightInfo) }
  }

  private fun unhighlight(highlightInfo: HighlightInfo) {
    val page = quranInfo.getPageFromSuraAyah(highlightInfo.sura, highlightInfo.ayah)
    items.asSequence()
      .filter { it.page == page }
      .forEach { it.onUnhighlight(page, highlightInfo) }
  }

  private fun unHighlightAyahs(type: HighlightType, page: Int? = null) {
    items.asSequence()
      .filter { page == null || page == it.page }
      .forEach { it.onUnHighlightAyahType(type) }
  }

}

`

---

### File: feature/linebyline/src/main/java/com/quran/labs/androidquran/extra/feature/linebyline/presenter/QuranLineByLinePresenter.kt

`kotlin
package com.quran.labs.androidquran.extra.feature.linebyline.presenter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.asImageBitmap
import com.quran.analytics.CrashReporter
import com.quran.data.core.QuranFileManager
import com.quran.data.core.QuranInfo
import com.quran.data.core.QuranPageInfo
import com.quran.data.dao.BookmarksDao
import com.quran.data.dao.HighlightsDao
import com.quran.data.dao.ReadingBookmarksDao
import com.quran.data.dao.Settings
import com.quran.data.model.SuraAyah
import com.quran.data.model.bookmark.AyahReadingBookmark
import com.quran.data.model.highlight.Highlight
import com.quran.data.model.selection.AyahSelection
import com.quran.data.model.selection.SelectionIndicator
import com.quran.data.model.selection.SelectionRectangle
import com.quran.data.model.selection.endSuraAyah
import com.quran.data.model.selection.selectionIndicator
import com.quran.data.model.selection.startSuraAyah
import com.quran.data.model.selection.withSelectionIndicator
import com.quran.data.model.selection.withYScroll
import com.quran.data.source.PageProvider
import com.quran.labs.androidquran.common.audio.memorization.MemorizationEngine
import com.quran.labs.androidquran.common.audio.model.playback.currentPlaybackAyah
import com.quran.labs.androidquran.common.audio.model.playback.currentPlaybackWord
import com.quran.labs.androidquran.common.audio.repository.AudioStatusRepository
import com.quran.labs.androidquran.extra.feature.linebyline.model.DisplayText
import com.quran.labs.androidquran.extra.feature.linebyline.model.EmptyPageInfo
import com.quran.labs.androidquran.extra.feature.linebyline.model.HighlightAyah
import com.quran.labs.androidquran.extra.feature.linebyline.model.HighlightType
import com.quran.labs.androidquran.extra.feature.linebyline.model.LineModel
import com.quran.labs.androidquran.extra.feature.linebyline.model.MissingLineByLineImagesException
import com.quran.labs.androidquran.extra.feature.linebyline.model.PageInfo
import com.quran.labs.androidquran.extra.feature.linebyline.model.ReadingBookmarkAyah
import com.quran.labs.androidquran.extra.feature.linebyline.model.SidelineDirection
import com.quran.labs.androidquran.extra.feature.linebyline.model.SidelineModel
import com.quran.labs.androidquran.extra.feature.linebyline.presenter.selection.SelectionHelper
import com.quran.labs.androidquran.extra.feature.linebyline.presenter.selection.mergeWith
import com.quran.mobile.linebyline.data.dao.AyahHighlight
import com.quran.mobile.linebyline.data.dao.AyahMarkerInfo
import com.quran.mobile.linebyline.data.dao.SuraHeader
import com.quran.mobile.linebyline.data.dao.WordHighlight
import com.quran.mobile.linebyline.data.dao.asAyahHighlight
import com.quran.mobile.linebyline.data.model.PageModel
import com.quran.mobile.mapper.imlaei.data.ImlaeiUthmaniMapper
import com.quran.mobile.mapper.imlaei.data.WordAlignment
import com.quran.reading.common.ReadingEventPresenter
import dev.zacsweers.metro.Inject
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File

class QuranLineByLinePresenter @Inject constructor(
  private val quranFileManager: QuranFileManager,
  private val pageModel: PageModel,
  private val pageProvider: PageProvider,
  private val bookmarksDao: BookmarksDao,
  private val highlightsDao: HighlightsDao,
  private val readingBookmarksDao: ReadingBookmarksDao,
  private val readingEventPresenter: ReadingEventPresenter,
  private val audioStatusRepository: AudioStatusRepository,
  private val selectionHelper: SelectionHelper,
  private val quranPageInfo: QuranPageInfo,
  private val settings: Settings,
  private val lineByLineSettings: QuranLineByLineSettingsPresenter,
  private val crashReporter: CrashReporter,
  private val quranInfo: QuranInfo,
  private val imlaeiUthmaniMapper: ImlaeiUthmaniMapper,
  private val memorizationEngine: MemorizationEngine,
  pages: IntArray
) {
  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  private val page = pages[0]

  private var lastXOffset: Float = 0f
  private var lastYOffset: Float = 0f
  private var selectionStartSuraAyah: SuraAyah? = null

  private data class PageDetails(
    val lines: ImmutableList<LineModel>,
    val suraHeaders: ImmutableList<SuraHeader>,
    val ayahMarkers: ImmutableList<AyahMarkerInfo>,
    val ayahBounds: ImmutableList<AyahHighlight>,
    val wordBounds: ImmutableList<WordHighlight>,
    val imlaeiUthmaniMappings: List<WordAlignment>
  )

  private val pageDetailsFetcher: Deferred<PageDetails> by lazy {
    scope.async {
      withContext(Dispatchers.IO) {
        val range = quranInfo.getVerseRangeForPage(page)
        val startAyah = SuraAyah(range.startSura, range.startAyah)
        val endAyah = SuraAyah(range.endingSura, range.endingAyah)

        PageDetails(
          lines = linesForPage(page),
          suraHeaders = pageModel.suraHeaders(page),
          ayahMarkers = pageModel.ayahMarkers(page),
          ayahBounds = pageModel.ayahHighlight(page),
          wordBounds = pageModel.wordHighlights(page),
          imlaeiUthmaniMappings = imlaeiUthmaniMapper.mapWords(startAyah, endAyah)
        )
      }
    }
  }

  fun loadPage(): Flow<PageInfo> {
    return combine(
      pageSelection(),
      readingBookmarksForPage(),
      lineByLineSettings.displaySettingsFlow
    ) { highlights, readingBookmarks, displaySettings ->
      val displayText = DisplayText(
        quranPageInfo.suraName(page),
        quranPageInfo.juz(page),
        quranPageInfo.displayRub3(page),
        manzilText = quranPageInfo.manzilForPage(page),
        localizedPageText = quranPageInfo.localizedPage(page)
      )

      val showSidelines = displaySettings.showSidelines && pageProvider.getDataSource().haveSidelines
      val sidelines = if (showSidelines) {
        sidelinesForPage(page)
      } else {
        persistentListOf()
      }

      val scrollItem = highlights.firstOrNull { it.shouldScroll }
      val targetScrollPosition = if (scrollItem != null) {
        selectionHelper.yForLine(scrollItem.ayahHighlights.first().lineId)
      } else {
        -1
      }

      val pageDetails = pageDetailsFetcher.await()
      PageInfo(
        page,
        pageProvider.pageType(),
        displayText,
        displaySettings,
        pageDetails.lines,
        pageDetails.suraHeaders,
        pageDetails.ayahMarkers,
        highlights,
        readingBookmarks,
        sidelines,
        targetScrollPosition,
        showSidelines,
        quranPageInfo.skippedPagesCount()
      )
    }
  }

  fun emptyState(): PageInfo {
    return EmptyPageInfo.copy(displaySettings = lineByLineSettings.latestDisplaySettings())
  }

  suspend fun fallbackToImageType(): Boolean {
    val currentPageType = settings.pageType()
    val fallbackType = pageProvider.getFallbackPageType()
    return if (fallbackType != null && currentPageType != fallbackType) {
      settings.setPageType(fallbackType)
      crashReporter.log("Line-by-line fallback from $currentPageType to $fallbackType")
      true
    } else {
      false
    }
  }

  fun onClick() {
    readingEventPresenter.onClick()
    if (readingEventPresenter.ayahSelectionFlow.value !is AyahSelection.None) {
      readingEventPresenter.onAyahSelection(AyahSelection.None)
    }
  }

  suspend fun startSelection(x: Float, y: Float) {
    selectionStartSuraAyah = null
    selectionHelper.startSelection(x, y)
    modifySelectionRange(0f, 0f)
  }

  fun onPagePositioned(x: Float, y: Float, width: Int, height: Int) {
    selectionHelper.setPageDimensions(width, height)
    val changeScrollY = y != lastYOffset
    lastXOffset = x
    lastYOffset = y

    if (changeScrollY) {
      applyUpdatedSelectionScrollY(y)
    }
  }

  suspend fun modifySelectionRange(offsetX: Float, offsetY: Float) {
    val previousSelection = readingEventPresenter.ayahSelectionFlow.value
    val ayahHighlights = pageDetailsFetcher.await().ayahBounds
    val selectedAyah = selectionHelper.modifySelectionRange(offsetX, offsetY, ayahHighlights)
    if (selectedAyah != null) {
      val selectionStart = selectionStartSuraAyah ?: selectedAyah
      selectionStartSuraAyah = selectionStart

      val updatedSelection =
        previousSelection.mergeWith(AyahSelection.Ayah(selectedAyah), selectionStart)
      if (updatedSelection != previousSelection) {
        // clear the selection when the page changes for a selection range
        val clearSelection = doesSpanMultiplePages(updatedSelection)
        val selectionToUpdate =
          if (clearSelection) { updatedSelection.withSelectionIndicator(SelectionIndicator.None) }
          else { updatedSelection }
        readingEventPresenter.onAyahSelection(selectionToUpdate)
      }
    }
  }

  private fun doesSpanMultiplePages(ayahSelection: AyahSelection): Boolean {
    return if (ayahSelection is AyahSelection.AyahRange) {
      val startSuraAyah = ayahSelection.startSuraAyah
      val endSuraAyah = ayahSelection.endSuraAyah
      val start = quranPageInfo.pageForSuraAyah(startSuraAyah.sura, startSuraAyah.ayah)
      val end = quranPageInfo.pageForSuraAyah(endSuraAyah.sura, endSuraAyah.ayah)
      start != end
    } else {
      false
    }
  }

  suspend fun endSelection() {
    selectionStartSuraAyah = null
    selectionHelper.endSelection()
    val currentSelection = readingEventPresenter.ayahSelectionFlow.value
    if (currentSelection !is AyahSelection.None &&
      (currentSelection.selectionIndicator() is SelectionIndicator.None ||
          currentSelection.selectionIndicator() is SelectionIndicator.ScrollOnly)
    ) {
      val bounds = boundsFor(currentSelection)
      if (bounds != null) {
        val (firstBounds, lastBounds) = bounds
        val updated = currentSelection.withSelectionIndicator(
          SelectionIndicator.SelectedItemPosition(
            firstBounds,
            lastBounds,
            yScroll = lastYOffset
          )
        )

        if (updated != currentSelection) {
          readingEventPresenter.onAyahSelection(updated)
        }
      }
    }
  }

  private fun applyUpdatedSelectionScrollY(y: Float) {
    val currentSelection = readingEventPresenter.ayahSelectionFlow.value
    val selectionIndicator = currentSelection.selectionIndicator()
    if (selectionIndicator !is SelectionIndicator.None && selectionIndicator !is SelectionIndicator.ScrollOnly) {
      val updatedSelectionIndicator = selectionIndicator.withYScroll(y)
      val updatedSelection = currentSelection.withSelectionIndicator(updatedSelectionIndicator)
      readingEventPresenter.onAyahSelection(updatedSelection)
    }
  }

  private suspend fun boundsFor(selection: AyahSelection): Pair<SelectionRectangle, SelectionRectangle>? {
    val startAyah = selection.startSuraAyah() ?: return null
    val endAyah = selection.endSuraAyah() ?: return null

    val highlights = pageDetailsFetcher.await().ayahBounds
    val initialStartBounds = highlights.firstOrNull { it.sura == startAyah.sura && it.ayah == startAyah.ayah }
    val initialEndBounds = highlights.lastOrNull { it.sura == endAyah.sura && it.ayah == endAyah.ayah }

    // if the selection spans multiple pages, we might have either a start or
    // an end - use the one we have for the other one in this case.
    val startBounds = initialStartBounds ?: initialEndBounds
    val endBounds = initialEndBounds ?: initialStartBounds

    return if (startBounds != null && endBounds != null) {
      val startRectangle = selectionHelper.selectionRectangle(startBounds)
      val endRectangle = selectionHelper.selectionRectangle(endBounds)
      if (startRectangle != null && endRectangle != null) {
        // for now only offset by x, we'll use scrollY for y
        // when tablet is implemented, we should reconsider
        startRectangle.offset(lastXOffset, 0f) to
            endRectangle.offset(lastXOffset, 0f)
      } else {
        null
      }
    } else {
      null
    }
  }

  private fun pageSelection(): Flow<ImmutableList<HighlightAyah>> {
    return combine(
      bookmarksDao.bookmarksForPage(page),
      highlightsForPage(),
      readingEventPresenter.ayahSelectionFlow,
      audioStatusRepository.audioPlaybackFlow,
      memorizationEngine.state
    ) { bookmarks, highlights, selectedAyah, audioPlaybackStatus, memState ->
      val pageDetails = pageDetailsFetcher.await()
      val ayahHighlights = pageDetails.ayahBounds
      val currentBookmarkHighlights = bookmarks.map {
          bookmark -> ayahHighlights.filter { it.sura == bookmark.sura && it.ayah == bookmark.ayah }
      }.map { HighlightAyah(HighlightType.Bookmark, it) }

      val colorHighlights = highlights.map { highlight ->
        val suraAyah = highlight.suraAyah
        HighlightAyah(
          HighlightType.Highlight(highlight.color),
          ayahHighlights.filter { it.sura == suraAyah.sura && it.ayah == suraAyah.ayah }
        )
      }

      val bookmarkHighlights =
        if (currentBookmarkHighlights.isEmpty() || settings.shouldShowBookmarks()) {
          currentBookmarkHighlights
        }
        else {
          emptyList()
        }

      val selectedAyahHighlights =
        when (selectedAyah) {
          is AyahSelection.Ayah -> {
            val suraAyah = selectedAyah.suraAyah
            ayahHighlights.filter { it.sura == suraAyah.sura && it.ayah == suraAyah.ayah }
          }
          is AyahSelection.AyahRange -> {
            val start = selectedAyah.startSuraAyah
            val end = selectedAyah.endSuraAyah
            ayahHighlights.filter { isAyahWithin(it.sura, it.ayah, start, end) }
          }
          AyahSelection.None -> emptyList()
        }
      val shouldScroll = selectedAyah is AyahSelection.Ayah &&
          selectedAyah.selectionIndicator is SelectionIndicator.ScrollOnly
      val selectedHighlights = HighlightAyah(HighlightType.Selection, selectedAyahHighlights, shouldScroll)

      val audioPlaybackAyah = audioPlaybackStatus.currentPlaybackAyah()
      val audioHighlight = if (audioPlaybackAyah == null) {
        emptyList()
      } else {
        ayahHighlights.filter {
          it.sura == audioPlaybackAyah.sura && it.ayah == audioPlaybackAyah.ayah
        }
      }

      val audioPlaybackWord = audioPlaybackStatus.currentPlaybackWord()
      val wordHighlights = if (audioPlaybackAyah == null || audioPlaybackWord == null) {
        emptyList()
      } else {
        val ayahWords = pageDetails.wordBounds.filter { it.sura == audioPlaybackAyah.sura && it.ayah == audioPlaybackAyah.ayah }

        // find the corresponding mapping from imlaei => uthmani, since audioWordNumber is imlaei
        val targetWord = pageDetails.imlaeiUthmaniMappings.find {
          it.sura == audioPlaybackAyah.sura && it.ayah == audioPlaybackAyah.ayah && it.audioWordNumber == audioPlaybackWord
        }

        // each word in imlaei might correspond to one or more words in uthmani
        targetWord?.glyphWordNumbers.orEmpty()
          .mapNotNull { ayahWords.getOrNull(it - 1) }
          .map { it.asAyahHighlight() }
      }

      val audioAyahHighlights = HighlightAyah(HighlightType.Audio, audioHighlight, wordHighlights.isEmpty())
      val audioWordHighlights = HighlightAyah(HighlightType.AudioWord, wordHighlights, true)

      val memMaskHighlights = if (memState.isActive && memState.isTextMasked && memState.activeAyahsInStep.isNotEmpty()) {
        val maskedAyahs = ayahHighlights.filter { ayah ->
          memState.activeAyahsInStep.any { it.sura == ayah.sura && it.ayah == ayah.ayah }
        }
        HighlightAyah(HighlightType.MemorizationMask, maskedAyahs)
      } else {
        null
      }

      val memWindowHighlights = if (memState.isActive && memState.currentStep?.ayahs?.isNotEmpty() == true) {
        val stepAyahs = memState.currentStep.ayahs
        val windowAyahs = ayahHighlights.filter { ayah ->
          stepAyahs.any { it.sura == ayah.sura && it.ayah == ayah.ayah }
        }
        HighlightAyah(HighlightType.MemorizationWindow, windowAyahs)
      } else {
        null
      }

      val extraHighlights = listOfNotNull(memWindowHighlights, memMaskHighlights)

      (colorHighlights + bookmarkHighlights + selectedHighlights + audioAyahHighlights + audioWordHighlights + extraHighlights)
        .filter { it.ayahHighlights.isNotEmpty() }
        .toImmutableList()
    }
  }

  private fun readingBookmarksForPage(): Flow<ImmutableList<ReadingBookmarkAyah>> {
    return readingBookmarksDao.readingBookmarksFlow()
      .map { readingBookmarks ->
        val ayahBounds = pageDetailsFetcher.await().ayahBounds
        readingBookmarks
          .filterIsInstance<AyahReadingBookmark>()
          .groupBy({ it.asSuraAyah() }, { it.slot })
          .map { (suraAyah, slots) ->
            ReadingBookmarkAyah(
              slots.sorted().toImmutableList(),
              ayahBounds.filter { it.sura == suraAyah.sura && it.ayah == suraAyah.ayah }
            )
          }
          .filter { it.ayahHighlights.isNotEmpty() }
          .toImmutableList()
      }
      .distinctUntilChanged()
  }

  private fun highlightsForPage(): Flow<List<Highlight>> {
    val range = quranInfo.getVerseRangeForPage(page)
    val startAyah = SuraAyah(range.startSura, range.startAyah)
    val endAyah = SuraAyah(range.endingSura, range.endingAyah)
    return highlightsDao.highlightsFlow()
      .map { highlights -> highlights.filter { it.suraAyah in startAyah..endAyah } }
      .distinctUntilChanged()
  }

  private fun isAyahWithin(sura: Int, ayah: Int, start: SuraAyah, end: SuraAyah): Boolean {
    return SuraAyah(sura, ayah) in start..end
  }

  private suspend fun linesForPage(page: Int): ImmutableList<LineModel> {
    return withContext(Dispatchers.IO) {
      val lines = mutableListOf<LineModel>()
      val whence = quranFileManager.quranImagesDirectory()
      for (lineNumber in 1..15) {
        val file = File(File(whence, page.toString()), "$lineNumber.png")

        val fileName = file.toString()
        val sourceBitmap = BitmapFactory.decodeFile(fileName, options)
        if (sourceBitmap != null) {
          val bitmap = sourceBitmap.extractAlpha()
          sourceBitmap.recycle()
          lines.add(LineModel(lineNumber - 1, bitmap.asImageBitmap()))
        } else {
          val status = if (file.exists()) {
            "exists: ${file.name} with length ${file.length()}"
          } else {
            "missing"
          }
          throw MissingLineByLineImagesException("Failed to load line $lineNumber for page $page - file $status")
        }
      }
      lines.toImmutableList()
    }
  }

  private suspend fun sidelinesForPage(page: Int): ImmutableList<SidelineModel> {
    return withContext(Dispatchers.IO) {
      val whence = quranFileManager.quranImagesDirectory()
      val direction = if (page % 2 == 0) SidelineDirection.DOWN else SidelineDirection.UP
      val sidelinesDirectory = File(File(whence, page.toString()), "sidelines")
      if (sidelinesDirectory.isDirectory) {
        val files = sidelinesDirectory.listFiles()
          ?.filter {
            val filenameWithoutOverrides =
              it.nameWithoutExtension.replace("_down", "").replace("_up", "")
            it.extension == "png" &&
                filenameWithoutOverrides.toIntOrNull() != null
          } ?: emptyList()
        files.mapNotNull {
          val filenameWithoutOverrides =
            it.nameWithoutExtension.replace("_down", "").replace("_up", "")
          val number = filenameWithoutOverrides.toInt()

          val actualDirection = if ("_up" in it.nameWithoutExtension) {
            SidelineDirection.UP
          } else if ("_down" in it.nameWithoutExtension) {
            SidelineDirection.DOWN
          } else {
            direction
          }

          val sourceBitmap = BitmapFactory.decodeFile(it.toString(), options)
          if (sourceBitmap != null) {
            val bitmap = sourceBitmap.extractAlpha()
            sourceBitmap.recycle()
            SidelineModel(bitmap.asImageBitmap(), number, actualDirection)
          } else {
            crashReporter.log("failed to load sideline image [$it] on page [$page]")
            null
          }
        }.toImmutableList()
      } else {
        persistentListOf()
      }
    }
  }

  companion object {
    private val options = BitmapFactory.Options()
      .apply {
        inPreferredConfig = Bitmap.Config.ALPHA_8
      }
  }
}

`

---

### File: app/src/main/java/com/quran/labs/androidquran/view/HighlightsDrawer.kt

`kotlin
package com.quran.labs.androidquran.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.Region
import android.os.Build
import android.util.TypedValue
import android.util.TypedValue.COMPLEX_UNIT_DIP
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.quran.data.model.highlight.HighlightType
import com.quran.data.model.highlight.HighlightType.Mode.BACKGROUND
import com.quran.data.model.highlight.HighlightType.Mode.COLOR
import com.quran.data.model.highlight.HighlightType.Mode.HIDE
import com.quran.data.model.highlight.HighlightType.Mode.HIGHLIGHT
import com.quran.data.model.highlight.HighlightType.Mode.UNDERLAY
import com.quran.data.model.highlight.HighlightType.Mode.UNDERLINE
import com.quran.labs.androidquran.R
import com.quran.labs.androidquran.ui.helpers.HighlightTypes
import com.quran.labs.androidquran.ui.helpers.AyahHighlight
import com.quran.labs.androidquran.ui.helpers.AyahHighlight.TransitionAyahHighlight
import com.quran.page.common.data.AyahBounds
import com.quran.page.common.data.AyahCoordinates
import com.quran.page.common.data.PageCoordinates
import com.quran.page.common.draw.ImageDrawHelper
import java.util.SortedMap

class HighlightsDrawer(
  private val ayahCoordinates: () -> AyahCoordinates?,
  private val highlightCoordinates: () -> Map<AyahHighlight, List<AyahBounds>>?,
  private val currentHighlights: () -> SortedMap<HighlightType, Set<AyahHighlight>>?,
  private val superOnDraw: (Canvas) -> Unit,
  private val isNightMode: () -> Boolean = { false },
  private vararg val highlightTypesFilter: HighlightType.Mode = HighlightType.Mode.entries.toTypedArray(),
) : ImageDrawHelper {

  // cached objects for onDraw
  private val scaledRect = RectF()
  private val alreadyHighlighted = mutableMapOf<HighlightType.Mode, MutableSet<AyahHighlight>>()

  // Singleton object so we can share the cache across all pages
  private object PaintCache {
    private val cache =
      mutableMapOf<Triple<HighlightType.Mode, /* @ColorRes */ Int, Boolean>, Paint>()

    fun getPaintForHighlightType(
      context: Context,
      type: HighlightType,
      isNightMode: Boolean
    ): Paint {
      val colorResId = if (isNightMode) type.nightColorResId else type.colorResId
      return cache.getOrPut(Triple(type.mode, colorResId, isNightMode)) {
        Paint().apply {
          val color = ContextCompat.getColor(context, colorResId)
          when (type.mode) {
            COLOR -> this.colorFilter = PorterDuffColorFilter(color, PorterDuff.Mode.SRC_ATOP)
            UNDERLAY -> this.color = if (isNightMode) {
              ColorUtils.setAlphaComponent(color, NIGHT_UNDERLAY_ALPHA)
            } else {
              color
            }
            else -> this.color = color
          }
        }
      }
    }

    fun getMaskBorderPaint(context: Context, isNightMode: Boolean): Paint {
      val colorRes = if (isNightMode) R.color.memorization_mask_border_night else R.color.memorization_mask_border
      val color = ContextCompat.getColor(context, colorRes)
      return Paint().apply {
        this.color = color
        this.style = Paint.Style.STROKE
        this.strokeWidth = TypedValue.applyDimension(COMPLEX_UNIT_DIP, 1.5f, context.resources.displayMetrics)
      }
    }
  }

  private fun alreadyHighlightedContains(
    highlightType: HighlightType,
    highlight: AyahHighlight
  ): Boolean {
    val alreadyHighlighted = alreadyHighlighted.getOrElse(highlightType.mode) { emptySet() }
    if (highlight in alreadyHighlighted) {
      return true
    }
    if (highlight.isTransition()) {
      val transitionHighlight = highlight as TransitionAyahHighlight
      // if x -> y, either x or y is already highlighted, then we don't show the highlight
      return transitionHighlight.source in alreadyHighlighted
          || transitionHighlight.destination in alreadyHighlighted
    }
    return false
  }

  override fun draw(pageCoordinates: PageCoordinates, canvas: Canvas, image: ImageView) {
    val highlightCoordinates = highlightCoordinates() ?: return
    val glyphsCoords = ayahCoordinates()?.glyphCoordinates
    val ayahLineCoords = ayahCoordinates()?.ayahLineCoordinates
    val underlineRaiseRatio = ayahCoordinates()?.underlineRaiseRatio ?: 0f
    val currentHighlights = currentHighlights() ?: return

    alreadyHighlighted.clear()

    val filteredHighlights = currentHighlights.filterKeys { it.mode in highlightTypesFilter }

    for ((highlightType, highlights) in filteredHighlights) {
      val paint = PaintCache.getPaintForHighlightType(image.context, highlightType, isNightMode())

      for (highlight in highlights) {
        if (alreadyHighlightedContains(highlightType, highlight)) continue

        val isUnderline = highlightType.mode == UNDERLINE
        val rangesToDraw = glyphsCoords?.takeIf { !highlightType.isTransitionAnimated }
          ?.getBounds(highlight.key, !isUnderline, highlightType.mode == BACKGROUND)
          ?: ayahLineCoords?.get(highlight.key)?.takeIf { isUnderline }?.map { it.bounds }
          ?: highlightCoordinates[highlight]?.map { it.bounds }

        if (rangesToDraw != null && rangesToDraw.isNotEmpty()) {
          for (bounds in rangesToDraw) {
            if (highlightType.mode == UNDERLINE) {
              val underlineThickness = TypedValue.applyDimension(
                COMPLEX_UNIT_DIP, UNDERLINE_THICKNESS_DIPS, image.resources.displayMetrics)
              bounds.top = bounds.bottom - bounds.height() * underlineRaiseRatio
              bounds.bottom = bounds.top + underlineThickness
            }

            image.imageMatrix.mapRect(scaledRect, bounds)
            scaledRect.offset(image.paddingLeft.toFloat(), image.paddingTop.toFloat())

            when (highlightType.mode) {
              HIDE -> {
                // Clip out the bounds to be hidden so they don't get drawn
                clipOutRect(canvas, scaledRect)
              }

              COLOR -> {
                // 1. save previously applied clips, and clip to just the bounds we want to colorize
                canvas.save()
                canvas.clipRect(scaledRect)
                // 2. save previously applied color filter, and draw this clipped part with the color we want
                val appliedFilter = image.colorFilter
                image.colorFilter = paint.colorFilter
                superOnDraw(canvas)
                // 3. restore sate (previously applied color filter + previous canvas clips)
                image.colorFilter = appliedFilter
                canvas.restore()
                // 4. clip out the bounds we just colorized so it doesn't get drawn again
                clipOutRect(canvas, scaledRect)
              }

              UNDERLAY, HIGHLIGHT, BACKGROUND, UNDERLINE -> {
                // Draw a rectangle with the specified paint
                canvas.drawRect(scaledRect, paint)
                if (highlightType == HighlightTypes.MEMORIZATION_MASK) {
                  val borderPaint = PaintCache.getMaskBorderPaint(image.context, isNightMode())
                  canvas.drawRect(scaledRect, borderPaint)
                }
              }
            }
          }

          alreadyHighlighted.getOrPut(highlightType.mode, { mutableSetOf() }).add(highlight)
        }
      }
    }
  }

  private fun clipOutRect(canvas: Canvas, rect: RectF) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      canvas.clipOutRect(rect)
    } else {
      canvas.clipRect(rect, Region.Op.DIFFERENCE)
    }
  }

  companion object {
    private const val UNDERLINE_THICKNESS_DIPS = 3f // dp
    private const val NIGHT_UNDERLAY_ALPHA = 0x66
  }
}

`

---

### File: app/src/main/AndroidManifest.xml

`xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
          xmlns:tools="http://schemas.android.com/tools"
  android:installLocation="auto">

  <uses-permission android:name="android.permission.INTERNET"/>
  <uses-permission
      android:name="android.permission.WRITE_EXTERNAL_STORAGE"
      android:maxSdkVersion="29" />
  <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
  <uses-permission android:name="android.permission.WAKE_LOCK"/>
  <uses-permission android:name="android.permission.RECORD_AUDIO"/>

  <!-- foreground services need this when targeting Android 9+ (Api 28+) -->
  <uses-permission-sdk-23 android:name="android.permission.FOREGROUND_SERVICE"/>

  <!-- required for posting notifications on Android 13+ (Api 33+) -->
  <uses-permission-sdk-23 android:name="android.permission.POST_NOTIFICATIONS"/>

  <!-- required for foreground service types on Android 14+ (Api 34+) -->
  <uses-permission-sdk-23 android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
  <uses-permission-sdk-23 android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />

  <!-- support chromebooks without touch screen -->
  <uses-feature android:name="android.hardware.touchscreen" android:required="false" />

  <!-- support android tv -->
  <uses-feature android:name="android.software.leanback" android:required="false" />

  <application
      android:banner="@mipmap/tv_banner"
      android:icon="@mipmap/ic_launcher"
      android:roundIcon="@mipmap/ic_launcher_round"
      android:label="@string/app_name"
      android:name=".QuranApplication"
      android:allowBackup="true"
      android:fullBackupContent="@xml/backup"
      android:dataExtractionRules="@xml/data_extraction"
      android:networkSecurityConfig="@xml/network_security_config"
      android:supportsRtl="true"
      android:theme="@style/Quran"
      android:requestLegacyExternalStorage="true"
      android:enableOnBackInvokedCallback="true"
      android:localeConfig="@xml/locales_config"
      tools:replace="android:fullBackupContent,android:dataExtractionRules"
      tools:targetApi="tiramisu">

    <activity
        android:name=".QuranDataActivity"
        android:configChanges="keyboardHidden|orientation|screenSize"
        android:theme="@style/QuranToolBarDayNight"
        android:exported="true">
      <intent-filter>
        <action android:name="android.intent.action.MAIN"/>
        <category android:name="android.intent.category.LAUNCHER"/>
        <category android:name="android.intent.category.LEANBACK_LAUNCHER"/>
      </intent-filter>
      <meta-data android:name="android.app.shortcuts"
                 android:resource="@xml/shortcuts" />
    </activity>

    <activity android:name=".ShortcutsActivity"
              android:exported="true" />
    <activity
        android:name=".ui.whatsnew.AnnouncementActivity"
        android:theme="@style/QuranToolBarDayNight"
        android:exported="false"/>
    <activity
        android:name=".ui.QuranActivity"
        android:theme="@style/QuranToolBarDayNight"
        android:exported="false"/>
    <activity
        android:name=".AboutUsActivity"
        android:theme="@style/QuranToolBarDayNight"
        android:exported="false"/>
    <activity android:name=".HelpActivity"
        android:theme="@style/QuranToolBarDayNight"
        android:exported="false"/>
    <activity
        android:name=".ui.PagerActivity"
        android:launchMode="singleTop"
        android:theme="@style/QuranToolBar.Overlay"
        android:exported="false"/>
    <activity
        android:name=".ui.TranslationManagerActivity"
        android:theme="@style/QuranToolBarDayNight"
        android:configChanges="keyboardHidden|orientation|screenSize"
        android:exported="false"/>
    <activity
        android:name=".QuranPreferenceActivity"
        android:theme="@style/QuranToolBarDayNight"
        android:configChanges="keyboardHidden|orientation|screenSize"
        android:exported="false"/>
    <activity
        android:name=".QuranAdvancedPreferenceActivity"
        android:theme="@style/QuranToolBarDayNight"
        android:configChanges="keyboardHidden|orientation|screenSize"
        android:exported="false"/>

    <activity
        android:name=".ui.BookmarkListActivity"
        android:theme="@style/QuranToolBarDayNight"
        android:exported="false"/>

    <activity
        android:name=".SearchActivity"
        android:launchMode="singleTop"
        android:theme="@style/QuranToolBarDayNight"
        android:exported="true">
      <intent-filter>
        <action android:name="android.intent.action.SEARCH"/>
      </intent-filter>
      <meta-data
          android:name="android.app.searchable"
          android:resource="@xml/searchable"/>
    </activity>

    <activity android:name=".QuranForwarderActivity"
        android:exported="true">
      <intent-filter>
        <action android:name="android.intent.action.VIEW"/>
        <category android:name="android.intent.category.DEFAULT"/>
        <data android:scheme="quran"/>
      </intent-filter>
    </activity>

    <!-- declare the default searchable Activity for the whole app -->
    <meta-data
        android:name="android.app.default_searchable"
        android:value=".SearchActivity"/>
    <!-- provides search suggestions for verses. -->
    <provider
        android:name=".data.QuranDataProvider"
        android:exported="false"
        android:authorities="@string/authority"/>

    <provider
        android:name="androidx.core.content.FileProvider"
        android:authorities="@string/file_authority"
        android:grantUriPermissions="true"
        android:exported="false">
      <meta-data
          android:name="android.support.FILE_PROVIDER_PATHS"
          android:resource="@xml/file_paths"/>
    </provider>

    <receiver android:name="androidx.media.session.MediaButtonReceiver"
        android:exported="true">
      <intent-filter>
        <action android:name="android.intent.action.MEDIA_BUTTON"/>
      </intent-filter>
    </receiver>

    <activity android:name=".QuranImportActivity"
        android:exported="true">
      <intent-filter>
        <action android:name="android.intent.action.SEND"/>
        <category android:name="android.intent.category.DEFAULT"/>
        <data android:mimeType="application/json"/>
      </intent-filter>
      <intent-filter>
        <action android:name="android.intent.action.VIEW"/>

        <category android:name="android.intent.category.DEFAULT"/>
        <category android:name="android.intent.category.BROWSABLE"/>

        <data android:scheme="file"/>
        <data android:host="*"/>
        <data android:mimeType="*/*"/>
        <data android:pathPattern=".*\\.backup"/>
        <data android:pathPattern=".*\\..*\\.backup"/>
        <data android:pathPattern=".*\\..*\\..*\\.backup"/>
        <data android:pathPattern=".*\\..*\\..*\\..*\\.backup"/>
      </intent-filter>
    </activity>

    <activity android:name=".pageselect.PageSelectActivity"
        android:theme="@style/QuranToolBarDayNight"
        android:exported="false"/>

    <service
        android:exported="false"
        android:name=".service.QuranDownloadService"
        android:foregroundServiceType="dataSync"/>

    <service
        android:exported="false"
        android:name=".service.AudioService"
        android:foregroundServiceType="mediaPlayback">
      <intent-filter>
        <action android:name="android.intent.action.MEDIA_BUTTON"/>
      </intent-filter>
    </service>

    <service
        android:name=".widget.BookmarksWidgetService"
        android:exported="false"
        android:permission="android.permission.BIND_REMOTEVIEWS" />

    <receiver android:name=".widget.BookmarksWidget"
        android:exported="true">
      <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
        <action android:name="com.quran.labs.androidquran.UPDATE_LIST" />
      </intent-filter>

      <meta-data
          android:name="android.appwidget.provider"
          android:resource="@xml/bookmarks_widget_info" />
    </receiver>

    <receiver android:name=".widget.SearchWidget"
        android:exported="true">
      <intent-filter>
        <action android:name="android.appwidget.action.APPWIDGET_UPDATE" />
      </intent-filter>

      <meta-data
          android:name="android.appwidget.provider"
          android:resource="@xml/search_widget_info" />
    </receiver>

    <activity
        android:name=".widget.ShowJumpFragmentActivity"
        android:launchMode="singleInstance"
        android:exported="false"
        android:excludeFromRecents="true"
        android:theme="@style/QuranDialog" />

    <!-- support for app specific locales on Android 12 (32) and lower -->
    <service
        android:name="androidx.appcompat.app.AppLocalesMetadataHolderService"
        android:enabled="false"
        android:exported="false">
      <meta-data
          android:name="autoStoreLocales"
          android:value="true" />
    </service>

    <!-- override WorkManager initialization, but keep androidx.startup for others -->
    <provider
        android:name="androidx.startup.InitializationProvider"
        android:authorities="${applicationId}.androidx-startup"
        android:exported="false"
        tools:node="merge">
      <meta-data
          android:name="androidx.work.WorkManagerInitializer"
          android:value="androidx.startup"
          tools:node="remove" />
    </provider>
  </application>
</manifest>

`

---

### File: app/src/main/res/values/colors.xml

`xml
<?xml version="1.0" encoding="utf-8"?>
<resources>
  <color name="toolbar">@color/surface</color>
  <color name="toolbar_text">#ff44464e</color>
  <color name="toolbar_icon_tint">#ff44464e</color>
  <color name="toolbar_secondary_text">#ff44464e</color>

  <color name="surface">#fffaf8f7</color>
  <color name="accent_color">#ff00838f</color>
  <color name="accent_color_dark">@color/color_control_normal</color>
  <color name="page_background">#FFF4CB</color>
  <color name="memorization_mask_color">#FFF4CB</color>
  <color name="memorization_mask_color_night">#FF181818</color>
  <color name="memorization_mask_border">#55795548</color>
  <color name="memorization_mask_border_night">#55FFFFFF</color>
  <color name="memorization_window_color">#264DB6AC</color>
  <color name="memorization_window_color_night">#334DB6AC</color>
  <color name="secondary_dark_background">#ffffffff</color>
  <color name="header_background">#dee2e6</color>
  <color name="header_text_color">@color/sura_number_color</color>
  <color name="ripple_dark">#1f000000</color>
  <color name="title_color">#212529</color>
  <color name="sura_number_color">#ff212529</color>
  <color name="sura_details_color">#ff6c757d</color>
  <color name="nav_status_bar_colors">#ff424242</color>
  <color name="transparent_actionbar_color">#AA000000</color>
  <color name="panel_background_color">@color/toolbar</color>
  <color name="translation_hdr_color">#ff777777</color>
  <color name="translation_sura_header">#fff1ecd8</color>
  <color name="translation_sura_header_night">#ff424242</color>
  <color name="translation_verse_text_color">#ff858383</color>
  <color name="translation_verse_background_color">#fff2f2f2</color>
  <color name="translation_verse_background_night_color">#ff333333</color>
  <color name="translation_inline_ayah_highlight_color">#ff00838F</color>
  <color name="translation_inline_ayah_highlight_night_color">#ff34d399</color>
  <color name="translation_translator_color">#ff00838F</color>
  <color name="translation_text_color">#ff444444</color>
  <color name="translation_footnote_color">#ff999999</color>
  <color name="translation_divider_color">#ffe8e8e8</color>

  <color name="translation_ayah_selected_color">#ffe0f2f1</color>
  <color name="translation_ayah_selected_color_night">#ff2d2d2d</color>
  <color name="overlay_text_color">#ff505050</color>
  <color name="inactive">#9e9e9e</color>
  <color name="selection_highlight">#404694A6</color>
  <color name="audio_highlight">#4046A646</color>
  <color name="note_highlight">#40EBEB21</color>
  <color name="bookmark_highlight">#40A4A4A4</color>
  <color name="translation_highlight">#ff33b5e5</color>
  <color name="panel_label">@color/accent_color</color>
  <color name="notification_color">#ff112e44</color>
  <color name="audio_notification_color">@color/download_button_background</color>
  <color name="audio_notification_background_color">#ffd8d8d8</color>
  <color name="ayah_bookmark_color">#ff2e7d32</color>
  <color name="download_button_background">#ffe8f4f8</color>

  <color name="icon_tint">#ff212529</color>
  <color name="indicator_color">@color/accent_color</color>
  <color name="color_control_activated">@color/accent_color</color>
  <color name="color_control_normal">#4d00838f</color>
  
  
  <color name="snackbar_background_color">@color/download_button_background</color>
  <color name="overlay_panel">#dd000000</color>
  <color name="navbar_night_color">#ff000000</color>
  <color name="toast_background_color">#e6eeeeee</color>
  <color name="toast_text_color">#ff000000</color>
  <color name="toast_background_color_night">#ff2d2d2d</color>
  <color name="toast_text_color_night">#ffffffff</color>

  <!--
    highlights as they appear in a list. the page itself uses the pale HighlightColors, which
    are too weak to read as a colour against a light row, so lists use a darker ramp instead.
  -->
  <color name="highlight_list_yellow">#fff9a825</color>
  <color name="highlight_list_green">#ff2e7d32</color>
  <color name="highlight_list_blue">#ff1565c0</color>
  <color name="highlight_list_pink">#ffad1457</color>
  <color name="highlight_list_purple">#ff6a1b9a</color>
  <!-- the outline of a collection name chip - the accent colour, held back so it reads as a label -->
  <color name="tag_chip_border">#7300838f</color>
  <!-- an ayah bookmark with no highlight on it - deliberately not one of the five above -->
  <color name="unhighlighted_bookmark_color">#ff546e7a</color>
  <!-- reading bookmarks -->

  <color name="default_text">#ff000000</color>

  <color name="text_primary">#ff212121</color>
  <color name="disabled_color">#ffbdbdbd</color>
  <color name="text_hint_color">#ff9e9e9e</color>
</resources>

`

---

### File: common/audio/src/test/java/com/quran/labs/androidquran/common/audio/memorization/MemorizationPreferencesTest.kt

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

### File: common/audio/src/test/java/com/quran/labs/androidquran/common/audio/memorization/MemorizationPlanBuilderTest.kt

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
}

`

---

### File: common/audio/src/test/java/com/quran/labs/androidquran/common/audio/memorization/MemorizationEngineTest.kt

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

