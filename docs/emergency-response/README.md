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

`text`, `label`, and `instructions` are `{ "en": …, "es": … }` maps. Fallback
order is requested → Spanish → English. **Kaqchikel (`cak`) text is intentionally
absent** until professionally translated and recorded — never machine-translate
it (Wuqu' Kawoq treats linguistic fidelity as non-negotiable; no TTS engine
supports Kaqchikel). The runtime falls back to Spanish for cak in the meantime.

## Milestone status

- [x] **M1 — Tree authoring + engine.** Data-driven tree (EN/ES), generic
      `TriageEngine`, full structural + behavioral unit tests. ✅ *(this commit)*
- [ ] **M2 — Compose visual flow** (voice deferred): `QuestionScreenScaffold`,
      phase progress bar, large A/B/C/D + icon buttons, disposition screens,
      EN/ES toggle mid-flow via `SavedStateHandle`, forced light + large type,
      dashboard entry point.
- [ ] **M3 — Offline case logging:** `CaseLogEntity`/`PendingCaseLogEntity`, DAO,
      repository, `Migration(4→5)`, case history.
- [ ] **M4 — Escalation + vitals:** tiered call/SMS dispatch, vitals thresholds,
      `/cases` backend sync via existing `SyncWorker`.
- [ ] **M5 — Voice layer:** `TtsAudioManager`, pre-recorded native clips keyed by
      `audioKey`, es-only `TextToSpeech` fallback (never synthesize Kaqchikel).
- [ ] **M6 — Accessibility & field hardening.**

## Open items needing input

- **Escalation endpoint:** does Wuqu' Kawoq have a 24/7 on-call dispatch
  number / SMS gateway to wire the auto-call/SMS to? (Escalation only helps if
  someone answers.)
- **Peripheral vitals devices** (BP cuff / pulse oximeter — safe+natal's ~$10
  stack): in scope for M4 or later?
- **Case-log retention:** the existing 7-day post-sync soft-delete may conflict
  with audit/retention needs for emergency records.
- **Named clinical + linguistic reviewers** and the proficiency-validation plan.
