# Clinical & linguistic review checklist — Emergency Response Guide

**Status: NOT VALIDATED.** `triage_tree.json` is `version: 1.0.0-draft`,
`validated: false`. This tool must not be used on real patients until a licensed
EMT/paramedic and a supervising physician (medical director) have reviewed,
corrected, and signed off the decision tree, and it has been field-tested and
aligned with local protocols and scope of practice.

Edit `AfyaQuest/app/src/main/assets/triage/triage_tree.json` directly; the unit
tests guard structural integrity after every change.

## Open clinical questions (must be resolved before sign-off)

1. **CPR trigger via lay pulse check.** The medical path (`sec_m_vitals_pulse`
   option C, "cannot feel any pulse") routes to CPR. Lay pulse checks are
   unreliable; current lay-rescuer guidance often triggers CPR on "not
   breathing / only gasping" alone. **Decide:** keep, modify, or remove the
   pulse-based CPR trigger.
2. **CPR instructions are compression-only** (hands-only), omitting rescue-breath
   ratios, compression depth in cm, and infant/child modifications. Confirm scope.
3. **Airway maneuver in suspected trauma.** `prim_airway_unresp` instructs a
   head-tilt; in suspected spinal trauma a jaw-thrust is preferred. This was
   simplified for non-clinical users — confirm or split by mechanism.
4. **Choking pathway** (`prim_airway_open`) has no back-blows / abdominal-thrust
   (Heimlich) steps; a still-obstructed patient is routed to immediate transport.
   Decide whether to add explicit choking-relief steps.
5. **AVPU escalation.** "Voice" and "Pain" responses route to the secondary
   survey (then computed as priority), not immediately. Confirm whether decreased
   responsiveness should escalate faster.
6. **`mechanism = unknown` defaults to the trauma path** to preserve spinal
   precautions; this may over-immobilize medical patients. Validate.
7. **Vitals ranges are adult-only** (resp 12–20, pulse 60–100). No pediatric,
   geriatric, or pregnancy-adjusted ranges — material given Wuqu' Kawoq's
   maternal focus. Define whether/which age-banded ranges to add.
8. **Disposition thresholds are heuristic.** The mapping of findings to
   stable/priority/immediate in the auto-eval nodes (`sec_eval_trauma`,
   `sec_eval_medical`) must be calibrated to local transport resources and
   receiving-facility capabilities.
9. **No treatment/medication guidance** beyond direct pressure for bleeding and
   positioning is included by design. Confirm scope boundary.

## Linguistic review

- **es-GT (Guatemalan Spanish)** text is a plain-language draft, **not**
  professionally medically translated. Needs review by a native es-GT speaker
  familiar with local health terms and literacy levels.
- **Kaqchikel (cak)** is intentionally absent from the data. It must be
  professionally translated AND recorded by native speakers (for the voice
  layer) — never machine-translated. Until then the app falls back to Spanish.

## Proficiency validation (carry over from safe+natal)

Adopt a task-observation checklist (safe+natal used 60 tasks, **90% pass bar**)
run against standardized scenarios with a short retraining loop for those who
fail. Assume the worst-case operator: panicked, no medical training.

## Sign-off

| Role | Name | Date | Tree version reviewed |
|---|---|---|---|
| EMT / Paramedic | | | |
| Supervising physician (medical director) | | | |
| es-GT linguistic reviewer | | | |
| Kaqchikel linguistic reviewer | | | |

When sign-off is complete, set `validated: true` and bump `version` in
`triage_tree.json`.
