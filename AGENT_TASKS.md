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
