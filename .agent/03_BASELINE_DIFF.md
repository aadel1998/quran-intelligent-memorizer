# BASELINE DIFF LEDGER (ORIGINAL APP VS. CUSTOM EVOLUTION)
> **Upstream Repository:** `https://github.com/quran/quran_android.git`  
> **Fork Repository:** `https://github.com/aadel1998/quran-intelligent-memorizer.git`  
> **Active Branch:** `main`

---

## 1. Original Baseline Snapshot
- **Upstream Baseline**: Original `quran_android` application by Quran.com / Quran Labs.
- **Base Architecture**:
  - Legacy Android `ViewPager` rendering pre-computed bitmap pages of Madani Mushaf with SQLite coordinate bounding boxes.
  - Basic ExoPlayer audio bar supporting simple ayah range loops (repeat verse $N$ times, repeat range $M$ times).
  - Legacy XML-based preference screens and Android View layouts.
  - SQLite/SQLDelight database for simple bookmarks and tags.
- **Original Gaps & Technical Debt**:
  1. No intelligent memorization engine or cognitive scaffolding.
  2. No sliding-window recitation loop (connecting individual verses into a contiguous memorized block).
  3. No spaced-repetition (SRS) review tracking, mastery ratings, or exponential interval scheduling.
  4. No active recall quiz or progressive Ayah masking.
  5. UI was fragmented across legacy Android Views, Material 1/2 styles, and inconsistent dark modes.
  6. Incomplete localization in non-English locales (untranslated strings in Persian, Urdu, Bengali, etc.).

---

## 2. Modification & Enhancement Ledger (Living Table)

| Change ID | Date / Sprint | Status | Target Files | Original Behavior (Baseline) | New / Modified Behavior | Rationale & Agent Role |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| `CHG-001` | Phase 0 | `COMPLETED` | `AGENT_TASKS.md` | No structured phase roadmap | Added strict phase-by-phase execution protocol and architecture documentation | Foundation & roadmap planning |
| `CHG-002` | Phase 1 & 3 | `COMPLETED` | `common/audio/.../memorization/*` | Simple single-verse or range repeat looping | Implemented continuous sliding-window state machine (`MemorizationEngine`, `MemorizationPlanBuilder`) | Core algorithmic memorization foundation |
| `CHG-003` | Phase 4 & 5 | `COMPLETED` | `feature/audiobar/ui/MemorizationHud.kt` | Legacy static audio bar | Reactive Memorization HUD displaying active step (`Solo`, `Connect`, `Recite`, `Bridge`), countdown, and live tuning | Live user awareness during recitation |
| `CHG-004` | Phase 6 | `COMPLETED` | `app/.../ui/pager/`, `common/drawing/` | Quran pages always fully visible | True Mushaf visual masking concealing verses during recitation test steps | Active cognitive recall reinforcement |
| `CHG-005` | Phase 7 & 8 | `COMPLETED` | `common/audio/.../memorization/` | No recitation check or progress memory | Progressive masking, voice recitation self-check, and Surah memorization tracker | Hifz progress tracking and self-evaluation |
| `CHG-006` | Phase 9 | `COMPLETED` | `common/audio/.../CustomPlaylist.kt` | Audio playback strictly bound to single Surah | Custom cross-Surah memorization & review playlists | Flexible multi-surah practice |
| `CHG-007` | Audio Fixes | `COMPLETED` | `AudioService.kt`, `MediaSession` | Audio stopped when app closed; notifications lacked controls | Background `MediaStyle` notification with step tags, earphone remote controls, and PiP floating player | Frictionless audio playback on device |
| `CHG-008` | Modern UI Wave 1 | `COMPLETED` | `QuranModernHomeScreen.kt` | Legacy tabbed pager activity | Jetpack Compose modern home screen with Surahs, Juz quarters, Bookmarks, and Memorizer dashboard | Material 3 visual modernization |
| `CHG-009` | Modern UI Wave 2 | `COMPLETED` | `ModernReaderChrome.kt`, `ModernAyahActionSheet.kt` | Legacy context dialogs on Ayah tap | Modern reader chrome overlays and emerald Ayah Action Sheet (*Play from Here*, *Memorize*, *Bookmark*, *Tafseer*) | Enhanced reading ergonomics |
| `CHG-010` | Modern UI Wave 3 | `COMPLETED` | `ModernAudioSettingsSheet.kt`, `ModernQariListSheet.kt` | Legacy settings dialog | M3 audio settings sheet with speed slider, repeat controls, and reciter picker | Streamlined audio configuration |
| `CHG-011` | Modern UI Wave 4 | `COMPLETED` | `ModernJumpSheet.kt`, `ModernSearchScreen.kt` | Legacy search dialog and jump menu | Modern jump sheet with numeric keypad and live highlighted Quran search | Rapid navigation |
| `CHG-012` | Modern UI Wave 5 | `COMPLETED` | `ModernSettingsScreen.kt`, `ModernTranslationPickerSheet.kt` | Legacy PreferenceFragment | Material 3 settings hub with grouped cards, live switches, and download manager | Settings unification |
| `CHG-013` | Modern UI Wave 6-7 | `COMPLETED` | `ModernAboutScreen.kt`, `ModernHelpScreen.kt` | Outdated static text screens | Interactive categorized FAQ with accordion topics and emerald cards | User onboarding & support |
| `CHG-014` | Pro Features | `COMPLETED` | `ModernRevisionDashboardScreen.kt`, `ModernRecallQuizSheet.kt` | No review scheduling or quiz capabilities | Spaced repetition (SRS) dashboard, 30-Juz mastery matrix, recall quiz, JSON backup/restore | Professional memorizer capabilities |
| `CHG-015` | Iconography | `COMPLETED` | All Compose UI sheets and screens | Generic or inconsistent icons | Comprehensive iconography overhaul: distinct vector icons for all actions and settings | Visual comprehensibility |
| `CHG-016` | i18n Overhaul | `COMPLETED` | 57 files (`app/res`, `common/ui/core/res`) | Incomplete translations; English strings remained in Settings/Help | 100% localized across 12 languages with strict Holy Quranic Arabic script preservation | Universal multilingual accessibility |
