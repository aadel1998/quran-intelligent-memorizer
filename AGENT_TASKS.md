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
- [x] Verify GitHub CLI installed and authenticated (gh v2.98.0, user: aadel1998)
- [x] Rename `origin` → `upstream`, create `aadel1998/quran-intelligent-memorizer` (public)
- [x] Push initial codebase to `origin/main`
- [x] Create this `AGENT_TASKS.md` protocol file

---

## [ ] Phase 1: Environment Setup, Dependency Installation & Baseline Run
- [ ] Inspect project manifests and identify required runtimes
- [ ] Verify local runtime versions (JDK, Android SDK, Gradle)
- [ ] Setup any required `.env` or local config files
- [ ] Run linter, type-checker, and build (`./gradlew assembleMadaniDebug`)
- [ ] Confirm baseline project builds with zero errors

---

## [ ] Phase 2: Deep Codebase & Audio Architecture Discovery
- [ ] Document Data Layer (Surahs, Ayahs, audio URLs/timestamps)
- [ ] Document Audio Engine (player, queues, onEnded events, playback state)
- [ ] Document State Management & Storage (state library, local storage)
- [ ] Document UI Architecture (file paths for player controls, reader view, settings)

---

## [ ] Phase 3: Core Memorization State Machine (Progressive & Sliding Window Engine)
- [ ] Design and implement `MemorizationEngine` service/module
- [ ] Implement sliding window algorithm with configurable parameters:
  - `windowSize` (W), `singleRepeat` (Rs), `connectRepeat` (Rc), `delayMode`
- [ ] Implement execution flow: Solo → Connect → Bridge → Window advance
- [ ] Expose state: `isActive`, `currentWindowIndex`, `stepType`, `currentRepeatIteration`, etc.
- [ ] Expose controls: `startMemorization()`, `pause()`, `resume()`, `skipCurrentStep()`, `previousStep()`, `stopMemorization()`
- [ ] Verify with linter/type-checker

---

## [ ] Phase 4: UI Integration, Active HUD, and Memory-Test (Blur) Mode
- [ ] Create Memorization Setup Modal/Drawer (Surah, ayah range, parameters)
- [ ] Build Active Memorization HUD / Player Bar with step/repeat counters
- [ ] Implement visual highlighting & auto-scroll for active Ayah
- [ ] Implement text blur/masking during CONNECT and PAUSE_FOR_RECITATION steps
- [ ] Verify build/type-check passes

---

## [ ] Phase 5: Persistence, Edge Cases & Final Verification
- [ ] Save memorization settings and progress to local storage
- [ ] Ensure normal playback cleanly exits Memorization Mode
- [ ] Write unit tests for Memorization state machine
- [ ] Run full lint/type-check/build verification
- [ ] Final commit and push
