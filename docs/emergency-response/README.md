# AfyaQuest Emergency Response Guide

A voice-ready, EMT-style **ABCDE patient-assessment** tool for community health
workers (CHWs) in low-resource field settings. One question per screen, large
lettered A/B/C/D buttons, a real branching decision tree, ending in one of three
dispositions (stable / priority / immediate) plus two safety early exits (unsafe
scene, no breathing → CPR).

Built as a **Jetpack Compose feature module inside the existing AfyaQuest Android
app** (reusing its theme, offline Room sync, and en/es/cak i18n). Demo /
deployment partner: **Wuqu' Kawoq / Maya Health Alliance**, Guatemala. The
interaction model is adapted from the **safe+natal** toolkit — see
[REFERENCE-safenatal.md](REFERENCE-safenatal.md).

> ⚠️ **The bundled decision tree is a DRAFT and is NOT validated for clinical
> use.** It must be signed off by a licensed EMT/paramedic and a supervising
> physician before any field deployment. See
> [CLINICAL-REVIEW.md](CLINICAL-REVIEW.md).

## Architecture: the tree is data, not code

The entire triage flow lives in a single bundled JSON file so a clinician can
revise wording, ordering, branching, and severity logic **without code changes**:

- **Tree:** `AfyaQuest/app/src/main/assets/triage/triage_tree.json`
- **Models:** `AfyaQuest/app/src/main/java/com/afyaquest/app/domain/triage/TriageModels.kt`
- **Engine:** `.../domain/triage/TriageEngine.kt` — a pure-Kotlin (no Android deps)
  generic walker; immutable `TriageState`, accumulates flags, resolves computed
  (`auto`) nodes internally so they never reach the UI.
- **Tests:** `AfyaQuest/app/src/test/java/com/afyaquest/app/domain/triage/TriageEngineTest.kt`

### Node model

- **`question` node** — shown to the user: `text` + 2–4 `options`. Each option
  sets `setFlags` and points `next` at another node id or a disposition id.
- **`auto` node** — never shown: an ordered `routing` list (first match wins) plus
  a `default`. Used for mechanism routing (`sec_router`) and **computed severity**
  (`sec_eval_trauma`, `sec_eval_medical`) — the tool decides the disposition from
  accumulated flags rather than asking a panicking user to "pick the worst."
- **`disposition`** — terminal: `level` (early_exit / immediate / priority /
  stable), `color` (crit / warn / ok), `pulse`, localized `label` + `instructions`,
  and an `escalation` { tier, action }.

Every node, option, and disposition already carries an `audioKey` so the future
voice layer ("Tap to hear this step") slots in with no data rework.

### Phases (progress bar)

`scene_size_up → primary_survey → secondary_survey → disposition`
(`tree.phaseOrder`). The medical/trauma split inside the secondary survey is
driven by the `mechanism` flag, not by separate phases.

## Editing the tree

1. Edit `triage_tree.json` (add/reorder nodes, reword options, change branch
   targets or routing rules).
2. Run the tests — they fail fast on a dangling `next`, an unreachable node, a
   path that never reaches a disposition, or a routing cycle:
   ```bash
   cd AfyaQuest
   JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
     ./gradlew :app:testDebugUnitTest --tests "com.afyaquest.app.domain.triage.TriageEngineTest"
   ```
3. Bump `version` and keep `validated: false` until clinically signed off.

## Localization

The offered languages are **English and Kaqchikel (`cak`)** — the CHWs' native
language for the Wuqu' Kawoq deployment. `text`, `label`, and `instructions` are
`{ "en": …, "cak": … }` maps; fallback order is requested → English.

**Kaqchikel triage content is not yet authored** and must come from a native
speaker — never machine-translate it (Wuqu' Kawoq treats linguistic fidelity as
non-negotiable). Until then, selecting Kaqchikel falls back to English. The
earlier draft Spanish (`es`) strings are kept in `triage_tree.json` as a
reference for translators but are no longer offered in the app.

## Milestone status

- [x] **M1 — Tree authoring + engine.** Data-driven tree (EN/ES), generic
      `TriageEngine`, full structural + behavioral unit tests. ✅ *(this commit)*
- [x] **M2 — Compose visual flow** (voice deferred): ✅ *(this commit)*
      `presentation/emergencyguide/` — `EmergencyGuideScreen` (intro + big "Start
      assessment"), `TriageFlowScreen` (one question per screen, large lettered
      A/B/C/D buttons, phase progress bar, severity-colored disposition screen
      with a pulsing critical header), `EmergencyTriageViewModel` (state in
      `SavedStateHandle` via path replay), `TriageTreeRepository` (loads the
      asset). EN/ES toggle works mid-flow without losing place (state is
      language-independent; `LanguageManager` drives both content and chrome).
      Forced light theme; severity colors added to `Color.kt`; dashboard entry
      `EmergencyEntryCard`. *Pending polish:* per-node icons (the `icon` keys
      exist in the JSON but aren't rendered yet) and large-type accessibility
      scaling.
- [x] **M3 — Offline case logging:** ✅ *(this commit)*
      `CaseLogEntity` (single table, `isSynced` flag — mirrors `ReportEntity`),
      `CaseLogDao`, `CaseLogRepository`, a real non-destructive **`Migration(4→5)`**
      (SQL verified byte-for-byte against Room's exported schema in `schemas/`),
      and a `CaseHistoryScreen` (past cases, newest first, with disposition
      color + sync badge). The ViewModel logs a case when an assessment reaches a
      disposition, keyed by a stable session id so re-answering REPLACEs rather
      than duplicates. Robolectric `CaseLogDaoTest` covers the round-trip.
      Backend `/cases` sync is deferred to M4 (`CaseLogDao.getUnsynced()` is ready).
- [ ] **M4 — Escalation + vitals:** tiered call/SMS dispatch, vitals thresholds,
      `/cases` backend sync via existing `SyncWorker`.
- [x] **M5 — Voice layer:** ✅ *(this commit)*
      `data/triage/TtsAudioManager` sequences recorded clips (MediaPlayer) with
      device `TextToSpeech` fallback; pure `domain/triage/AudioResolver` enforces
      the rules (unit-tested). A labeled **"Tap to hear this step"** button reads
      the question + every lettered option (or a disposition's guidance) and is
      hidden when no audio is available. **Kaqchikel is never synthesized** — it
      plays only from a recorded clip, else stays text-only; en/es use device TTS
      as an interim. Recording the native clips is a content task — see
      [AUDIO.md](AUDIO.md) (77 keys, manifest-gated, no code change needed).
      **Update:** the "Tap to hear this step" button is currently **removed from
      the flow** by product decision (speaking is slower in a real emergency).
      The audio infrastructure (`TtsAudioManager`, `AudioResolver`, manifest) is
      retained and dormant — re-show the button to revive it.
- [x] **M6 — Accessibility & field hardening:** ✅ *(this commit)*
      per-node pictographic icons (`TriageIcons.kt`, icon-first after safe+natal;
      interim Material glyphs, swappable for custom pictographs), severity icons
      on disposition headers, `heading()` semantics on questions/results for
      TalkBack, and **keep-screen-on** during an assessment. Type uses scalable
      `sp` and large touch targets throughout. *Remaining for a real field pass:*
      custom field-tested pictographs and an on-device contrast/large-font audit.

## Open items needing input

- **Escalation endpoint:** does Wuqu' Kawoq have a 24/7 on-call dispatch
  number / SMS gateway to wire the auto-call/SMS to? (Escalation only helps if
  someone answers.)
- **Peripheral vitals devices** (BP cuff / pulse oximeter — safe+natal's ~$10
  stack): in scope for M4 or later?
- **Case-log retention:** the existing 7-day post-sync soft-delete may conflict
  with audit/retention needs for emergency records.
- **Named clinical + linguistic reviewers** and the proficiency-validation plan.
