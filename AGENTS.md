# UNIVERSAL AGENT ENTRY ROUTER & SYSTEM RUNBOOK
> **Repository:** `quran-intelligent-memorizer`  
> **Application ID:** `com.quran.labs.androidquran.debug`  
> **Role:** Universal Navigation, Architectural Golden Rules, and Boot Sequence for AI Pair-Programming Agents.

---

## ⚡ 1-Minute Mandatory Agent Boot Sequence
Every agent starting a session on this codebase **MUST** follow this exact 4-step sequence before touching any code:

1. **Check Active Task & Resume Pointer**:
   - Open and read [`.agent/04_ACTIVE_SPRINT.md`](file:///C:/Users/Aadel/.gemini/antigravity/scratch/quran-intelligent-memorizer/.agent/04_ACTIVE_SPRINT.md).
   - Check the `Active Epic`, `Locked Files`, and the `EXACT RESUME POINTER`.
2. **Navigate the Pyramid Map (L0 -> L3)**:
   - Open [`.agent/01_PYRAMID_MAP.md`](file:///C:/Users/Aadel/.gemini/antigravity/scratch/quran-intelligent-memorizer/.agent/01_PYRAMID_MAP.md).
   - Locate the exact subsystem (L1) -> module cluster (L2) -> target file (L3).
   - ⚠️ **STRICT LAW**: NEVER grep or scan the entire repository blindly. Use the Pyramid Map to identify target files in seconds.
3. **Verify the Blast Radius & Network Callers**:
   - Open [`.agent/02_NETWORK_GRAPH.md`](file:///C:/Users/Aadel/.gemini/antigravity/scratch/quran-intelligent-memorizer/.agent/02_NETWORK_GRAPH.md).
   - Check the **Blast Radius Matrix** for your target file to understand what calls it and what shared state it mutates.
4. **Update Ledger & Sprint Before Handoff**:
   - Before ending your turn or switching agents, update:
     - [`.agent/03_BASELINE_DIFF.md`](file:///C:/Users/Aadel/.gemini/antigravity/scratch/quran-intelligent-memorizer/.agent/03_BASELINE_DIFF.md) (if any baseline behavior changed or features were added).
     - [`.agent/04_ACTIVE_SPRINT.md`](file:///C:/Users/Aadel/.gemini/antigravity/scratch/quran-intelligent-memorizer/.agent/04_ACTIVE_SPRINT.md) (with the exact resume pointer and checklist status).

---

## 🛠️ Verified Tech Stack, Build, Test & Run Commands
- **Environment**: Windows, Microsoft OpenJDK 21 (`C:\Users\Aadel\.jdks\ms-openjdk-21`), Android SDK Platform-Tools (`$env:LOCALAPPDATA\Android\Sdk\platform-tools`).
- **Core Stack**: Kotlin 1.9+, Java 21, Jetpack Compose, Material 3, Molecule, SQLDelight SQLite, Media3 / ExoPlayer 2, Coroutines StateFlow.

### Exact Terminal Commands
```powershell
# 1. Compile & Assemble Debug APK (Offline build verified, ~45s)
.\gradlew.bat assembleMadaniDebug --offline

# 2. Run Local Unit Tests (Includes Memorization Engine & Plan Builder test suites)
.\gradlew.bat testMadaniDebugUnitTest

# 3. Stream Install onto Connected Android Device
$env:Path = "$env:LOCALAPPDATA\Android\Sdk\platform-tools;$env:Path"
adb install -r app\build\outputs\apk\madani\debug\app-madani-debug.apk

# 4. Launch Main Quran Activity
adb shell am start -n com.quran.labs.androidquran.debug/com.quran.labs.androidquran.QuranDataActivity

# 5. Capture Device Screen for Visual Verification
adb shell screencap -p /sdcard/screen.png
adb pull /sdcard/screen.png screen.png

# 6. Check Live Logs for Crashes or Exceptions
adb logcat -d -t 150 | Select-String -Pattern "Quran|AndroidRuntime|FATAL|Exception"
```

---

## 🏛️ Architectural Golden Rules (Enforced)
1. **Sacred Holy Quranic Arabic Script Inviolability**:
   - Quranic Surah names (`سُورَةُ الْفَاتِحَةِ`, `الْفَاتِحَةِ`, `البَقَرَة`) and Holy Ayah texts **MUST** remain strictly in their authentic Arabic script across all 12 supported locales. Never replace Arabic Quranic text or calligraphy with transliteration or English names in reader and card headers.
2. **Full Multi-Language UI Parity (12 Supported Locales)**:
   - All user-facing UI strings (buttons, dialogs, hints, notification channels, audio steps) must use `@string` resources.
   - Synchronize strings in both `app/src/main/res/values-*/strings.xml` and `common/ui/core/src/main/res/values-*/strings.xml` across all 12 locales: `ar`, `fa`, `ur`, `in`, `tr`, `bn`, `ms`, `fr`, `ru`, `es`, `de`, `en`.
   - **Zero untranslated English strings allowed** in non-English locales.
3. **Emerald Material 3 Design System**:
   - Modern screens live in `app/src/main/java/com/quran/labs/androidquran/ui/modern/` and use `EmeraldPrimary` (`#10B981`), `DarkSlateSurface` (`#0F172A`/`#1E293B`), rounded cards (16-24dp), and `ModalBottomSheet`.
4. **Discrete Computational Engine Separation**:
   - Memorization math (sliding window, intervals, state transitions) resides strictly in `common/audio/memorization` and `feature/audiobar/ui`. UI layers observe `StateFlow` and dispatch pure intents without mutating playback queue state directly.
5. **High Blast-Radius Verification**:
   - Before modifying core symbols such as `SuraAyah.kt` (134 callers), `QuranInfo.kt` (88 callers), `QuranSettings.java` (59 callers), or `Theme.kt` (49 callers), consult [`.agent/02_NETWORK_GRAPH.md`](file:///C:/Users/Aadel/.gemini/antigravity/scratch/quran-intelligent-memorizer/.agent/02_NETWORK_GRAPH.md).
