# Quran Intelligent Memorizer & Custom Playlists: Architecture, Implementation & Debugging Guide

## 1. Executive Summary & Project Vision

The **Quran Intelligent Memorizer** is an advanced Hifz (Quran memorization) and custom audio playback system built directly into the official `quran_android` open-source codebase.

### Core Goals Requested by the User
1. **Intelligent Progressive Memorization Algorithm**:
   - Emulate the pedagogical methodology of dedicated Quran memorization apps (such as "Memorize Quran").
   - **Sliding Window Technique**: A window of size $W$ moves through a selected range of Ayahs.
   - **Solo Repetitions ($R_s$)**: Each new Ayah is recited independently $R_s$ times.
   - **Recitation Pauses ($D$)**: Optional silent pause intervals (e.g. 3s, 5s, or dynamic duration matching the Ayah's audio length) allowing the user to recite from memory.
   - **Voice Recitation Recording**: Real-time microphone capture during recitation pauses with one-tap instant playback comparison.
   - **Connect Repetitions ($R_c$)**: Previously learned Ayahs in the active window are linked together and repeated consecutively $R_c$ times to solidify the transition between verses.
   - **Memory Test & Masking**: Visual masking/blurring of Quranic text on the True Mushaf page with a "tap-to-peek" toggle to test recall.
2. **Custom Cross-Surah Playlists**:
   - Ability to define custom sequences of Ayahs across different Surahs (e.g., Al-Fatiha full $\to$ Al-Baqarah 1–7 $\to$ Al-Ikhlas full $\to$ Al-Kafirun full $\to$ Al-Falaq $\to$ An-Nas $\to$ Al-Baqarah 270–286).
   - Persistent playlist creation, editing, reordering, and deletion.
   - Dual-action playback: play continuously for listening, or launch directly into the Intelligent Memorization engine.
   - Accessible from the home screen top action bar (near Search and Bookmarks) and inside the reader.
   - Fast Ayah selection via numeric inputs, stepper controls, and double-ended range sliders.
3. **Zero-Crash Audio & Download Protection**:
   - Eliminate audio service crashes and UI lockups when audio files or gapless databases are not downloaded.
   - Route missing audio through the native downloading pipeline, display progress, and automatically start playback once ready.
4. **Live In-Playback Settings Adjustment**:
   - Ability to adjust parameters ($W, R_s, R_c$, Pause duration, Masking) directly during active playback via an in-HUD quick settings sheet without restarting or losing the current Ayah position.

---

## 2. System Architecture & Component Design

```
+-----------------------------------------------------------------------------------+
|                                  PRESENTATION LAYER                               |
|                                                                                   |
|  [QuranActivity]                [PagerActivity]                                   |
|   - Top-bar Playlist Action      - Overlay Management & Intent Routing            |
|                                  - Audio Pre-flight & Download Queue Orchestrator |
|                                                                                   |
|  [AudioBarWrapper] (Compose + Molecule)                                           |
|   ├── MemorizationHud (Active Step, Loops, Countdown, Masking, Live Settings)     |
|   ├── MemorizationLiveSettingsSheet (On-the-fly Window, Repeat, Pause Tuning)     |
|   ├── MemorizationSetupSheet (Range, Presets, Sliders, Numeric Direct Inputs)     |
|   └── PlaylistListSheet & PlaylistEditorSheet (Cross-Surah Segment Management)    |
+-----------------------------------------------------------------------------------+
                                        │
                                        ▼
+-----------------------------------------------------------------------------------+
|                                 DOMAIN STATE ENGINE                               |
|                                                                                   |
|  [MemorizationEngine] (@SingleIn(AppScope))                                       |
|   - Holds StateFlow<MemorizationState>                                            |
|   - Coordinates Step Execution: Solo -> Pause -> Connect -> Next Window          |
|   - Dynamic Plan Mutation via updateConfig() preserving current Ayah              |
|                                                                                   |
|  [MemorizationPlanBuilder]                                                        |
|   - Builds deterministic List<MemorizationStep>                                   |
|   - Handles both single contiguous ranges and arbitrary cross-Surah segments      |
+-----------------------------------------------------------------------------------+
       ▲                                        │
       │ updates / queries                      ▼
+──────────────────────────+     +──────────────────────────────────────────────────+
|      DATA LAYER          |     |              PLAYBACK & SERVICE LAYER            |
|                          |     |                                                  |
| [PlaylistRepository]     |     |  [AudioService] (Foreground Media3 / ExoPlayer)  |
|  - JSON persistence      |     |   - Gapless Timeline Tracking via                |
|  - SharedPreferences     |     |     updateAudioPlayPosition()                    |
|                          |     |   - handleNextMemorizationAyah() routing         |
| [MemorizationPreferences]|     |   - Recitation Pause Countdown & Job Management  |
|  - Saved sessions        |     |   - Loop-back Seeking (seekTo)                   |
|  - Mastered ranges       |     |   - Cross-Surah segment audio reload             |
|                          |     |                                                  |
| [HighlightsDrawer]       |     |  [RecitationRecorder] (MediaRecorder AAC)        |
|  - True Mushaf masking   |     |   - Live microphone capture during silent pause  |
+──────────────────────────+     +──────────────────────────────────────────────────+
```

---

## 3. Detailed Component Breakdown

### 3.1. `MemorizationEngine.kt` (`common/audio`)
- **Role**: Core singleton state machine managing Hifz progression.
- **Key Methods**:
  - `startMemorization(config)`: Builds deterministic step plan via `MemorizationPlanBuilder`, sets initial state, saves session to preferences.
  - `onAyahPlaybackCompleted(currentAyah)`: Called by `AudioService` whenever an Ayah completes. Handles loop counters (`currentRepeatIteration`), step transitions (`currentStepIndex++`), pause steps, and returns the next `SuraAyah` to play (or `null` when finished).
  - `updateConfig(newConfig)`: Dynamically rebuilds remaining steps with new parameters ($W, R_s, R_c$, pause mode, masking) and snaps to the step corresponding to the current Ayah without resetting user progress.
  - `skipCurrentStep()` / `previousStep()`: Fast manual navigation across memorization steps.
  - `toggleTextMask()` / `revealCurrentAyah()` / `hideCurrentAyah()`: Controls text visibility for memory testing.

### 3.2. `AudioService.kt` (`app/src/main/java`)
- **Role**: Foreground service driving audio playback via Media3 / ExoPlayer.
- **Critical Gapless Integration**:
  - Quran audio is typically provided as gapless Surah MP3 files paired with timing SQLite databases.
  - In normal playback, `updateAudioPlayPosition()` fires periodically as ExoPlayer advances through timestamps.
  - **The Gapless Fix**: Previously, `updateAudioPlayPosition()` called `localPlayer.seekTo(seekPos)` directly, completely skipping silent recitation pauses. We updated it to route through `handleNextMemorizationAyah(nextAyah, localAudioQueue)`:
    - If `stepType == PAUSE_FOR_RECITATION`: immediately calls `player.pause()`, removes position update messages, triggers `recitationRecorder.startRecording()`, launches a coroutine countdown job updating `pauseRemainingSeconds`, and upon countdown completion stops recording and advances to the next step.
    - If `stepType == PLAYBACK`: seeks player to target ayah timestamp and starts playback.
    - If transitioning across Surahs: detects `beforeSura != nextAyah.sura` and invokes `playAudio(true)`.

### 3.3. `PagerActivity.kt` (`app/src/main/java`)
- **Role**: Main reader activity orchestrating page view, audio presenter, and memorization overlays.
- **Download Pre-Flight Protection**:
  - `haveFilesForRange(start, end, qari)`: Checks local disk for gapless MP3s, ayah audio files, and gapless databases using `audioUtils.haveAllFiles` and `audioExtensionDecider`.
  - `findFirstMissingAudioRange(config, item)`: Scans all segments of a playlist or single range for missing audio.
  - `startMemorizationSession(config)`:
    - If files are missing: saves `pendingMemorizationConfig = config` (without starting `MemorizationEngine`) and invokes `audioPresenter.play(..., shouldStream = false)`.
    - `AudioPresenter` calls `handleRequiredDownload()`, launching `QuranDownloadService` and displaying `DownloadingAudioBar` with true download progress.
    - Upon `onDownloadSuccess()`, `startMemorizationSession` is called again. Once all segments are downloaded, `launchMemorizationPlayback()` initiates playback and displays `MemorizationHud`.
    - If the user cancels the download in `onCancelPressed()`, `pendingMemorizationConfig` is cleared.

### 3.4. `MemorizationHud.kt` & `MemorizationLiveSettingsSheet.kt` (`feature/audiobar`)
- **`MemorizationHud.kt`**:
  - Displays current Surah/Ayah, Step Type badge (`Solo`, `Connect`, `Pause`, `Review`), repeat counters (`Iter 2/3`), active window summary (`Ayahs 1–5`), and live countdown bar during recitation pauses.
  - Action buttons: Previous Step, Play/Pause, Skip Step, Stop, Masking Toggle (Eye icon), Recorded Voice Replay (Mic icon), and **Settings (Gear icon)**.
- **`MemorizationLiveSettingsSheet.kt`**:
  - Bottom sheet opened by tapping the Settings icon on `MemorizationHud`.
  - Allows live adjustments of Window Size ($1 \dots 10$), Solo Repeat ($1 \dots 10$), Connect Repeat ($0 \dots 10$), Recitation Pause mode, and Text Masking level.
  - Tapping "Apply" calls `memorizationEngine.updateConfig(updatedConfig)` with zero interruption to playback.

### 3.5. `PlaylistRepository.kt`, `PlaylistEditorSheet.kt` & `PlaylistListSheet.kt`
- **Role**: Cross-Surah playlist creation, storage, and execution.
- **Data Model**:
  - `CustomPlaylist`: `id`, `name`, `segments: List<PlaylistSegment>`, `createdAt`, `lastPlayedAt`.
  - `PlaylistSegment`: `sura`, `startAyah`, `endAyah`.
- **UI**:
  - Multi-segment editor with Surah picker, numeric input fields for start/end ayah, and range sliders.
  - Direct "Play All" button (continuous listening) and "Memorize" button (launches intelligent memorization on playlist sequence).
  - Accessible from `QuranActivity` (home screen toolbar icon) and reader view.

---

## 4. Complete Changelog of Files Modified & Added

| File Path | Action | Description |
|---|---|---|
| `app/src/main/java/.../service/AudioService.kt` | Modified | Routed gapless `updateAudioPlayPosition()` to `handleNextMemorizationAyah()`; added countdown coroutine, MediaRecorder integration, cross-surah segment handling. |
| `app/src/main/java/.../ui/PagerActivity.kt` | Modified | Added audio pre-flight verification (`findFirstMissingAudioRange`, `haveFilesForRange`); handled deferred download flow; integrated Live Settings and Playlists. |
| `app/src/main/java/.../ui/QuranActivity.kt` | Modified | Added top-bar playlist action icon near search and bookmarks. |
| `app/src/main/res/menu/home_menu.xml` | Modified | Added `action_playlists` menu item. |
| `app/src/main/res/drawable/ic_playlist.xml` | Created | Vector icon for playlists toolbar button. |
| `common/audio/.../memorization/MemorizationEngine.kt` | Modified | Added `updateConfig(newConfig)` for live parameter tuning without resetting session progress. |
| `common/audio/.../memorization/CustomPlaylist.kt` | Created | Data model for custom cross-surah playlists. |
| `common/audio/.../memorization/PlaylistSegment.kt` | Created | Data model for individual Surah ayah ranges within playlists. |
| `common/audio/.../memorization/PlaylistRepository.kt` | Created | Repository managing JSON-serialized playlist persistence in SharedPreferences. |
| `feature/audiobar/.../ui/MemorizationHud.kt` | Modified | Added `onOpenSettings` callback and Settings gear icon to bottom controls row. |
| `feature/audiobar/.../ui/MemorizationLiveSettingsSheet.kt` | Created | Bottom sheet for adjusting memorization parameters on the fly during playback. |
| `feature/audiobar/.../ui/MemorizationSetupSheet.kt` | Modified | Made `NumberStepper` internal; added direct numeric input and range sliders. |
| `feature/audiobar/.../ui/NumberInputOrStepper.kt` | Created | Composable component combining direct number typing with `+`/`-` buttons. |
| `feature/audiobar/.../ui/PlaylistListSheet.kt` | Created | Modal listing all saved playlists with Play, Memorize, Edit, and Delete actions. |
| `feature/audiobar/.../ui/PlaylistEditorSheet.kt` | Created | Multi-segment playlist editor supporting cross-Surah sequence definitions. |
| `feature/audiobar/.../AudioBarWrapper.kt` | Modified | Added download state guard: displays `DownloadingAudioBar` if download/prompt is active, preventing premature HUD display. |

---

## 5. Verification & Testing History

1. **Unit Tests**:
   - `:common:audio:testReleaseUnitTest` (100% passing across `MemorizationEngineTest`, `MemorizationPlanBuilderTest`, `MemorizationPreferencesTest`, `PlaylistRepositoryTest`).
2. **Build Verification**:
   - `assembleMadaniDebug` completed successfully with zero compilation errors.
3. **Physical Device Verification (`857dae20`, Xiaomi HyperOS / Android 15)**:
   - Deployed via `adb install -r -t app-madani-debug.apk`.
   - Verified that playing an undownloaded Surah in Intelligent Mode displays the download bar and starts playback cleanly upon completion.
   - Verified that gapless playback pauses for recitation countdown.
   - Verified that in-playback Settings icon opens `MemorizationLiveSettingsSheet` and dynamically adapts parameters.
   - Clean logcat confirmed with zero crashes or unhandled exceptions.
4. **Git Repository Status**:
   - All commits pushed to GitHub repository: `https://github.com/aadel1998/quran-intelligent-memorizer.git` on branch `main`.
