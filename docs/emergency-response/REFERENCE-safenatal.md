# Reference: safe+natal (Emory × Wuqu' Kawoq / Maya Health Alliance)

The Emergency Response Guide adapts the interaction model of the **safe+natal**
toolkit (formerly *GRACE* / Mobile Maternal & Infant Health Program) — the prior
work the dev brief asked us to research. This file captures what it is, why it
works, and the specific design choices we are carrying over to a general EMT
ABCDE first-response triage tool.

## What it is

A checklist-based smartphone app for community health workers in rural
Guatemala, **co-designed** by Emory University (Dr. Gari Clifford, Dr. Rachel
Hall-Clifford) and the **Maya Health Alliance / Wuqu' Kawoq** (Dr. Peter
Rohloff) together with traditional indigenous Maya midwives (*comadronas*) who
are bilingual in **Kaqchikel and Spanish**, often have no formal medical
training, and in some cases cannot read.

- Guides a midwife through a **pictographic** checklist of complications grouped
  by visit type (prenatal / perinatal / postnatal), with **audio prompts
  recorded in Kaqchikel** for non-readers.
- Low-cost diagnostics feed the phone directly: a BP cuff, a pulse oximeter, and
  a ~$10 Doppler/ultrasound.
- **Tiered, automatic escalation the user cannot forget to trigger:** critical
  complications fire an **immediate automatic voice call** to a 24-hour on-call
  clinical team (interrupting the workflow); less-critical findings raise an
  alert screen at the end of the visit; abnormal vitals auto-send alert texts
  against hard thresholds (maternal HR ≤60 or ≥100, SpO₂ ≤90%, systolic BP ≤70
  or ≥140).
- **Offline-first with graceful degradation:** at minimum it sends a text to
  request help even on a weak signal; richer data syncs to the Maya Health
  Alliance medical record later when connectivity allows.

## Evidence

Pragmatic randomized controlled feasibility trial (~800 pregnancies; Martinez et
al., *Reproductive Health* 2018): early-access arm achieved a median **33
referrals/100 births vs 20** for control (p=0.03); >90% referral success; and
significantly higher detection of hypertensive disorders (**3.53% vs 1.07%**,
p=0.03) driven by automated BP monitoring. Midwives reached ~80% first-attempt
proficiency, 98% after a 2-day retraining, scored against a **60-task
observation checklist with a 90% passing bar**. Adopted as standard of care
across 50+ Kaqchikel communities around Tecpán; ~40 midwives cover ~800
births/year. In 2023 a $1.8M Google.org "AI for the Global Goals" grant extended
the toolkit with decision-support AI.

## Design principles carried into the Emergency Response Guide

| safe+natal choice | How we apply it |
|---|---|
| Co-design **with** frontline workers, not for them | Pressure-test the tree wording, icons, and ordering with actual CHWs/EMTs before deployment. |
| Pictographic, icon-first (assume non-literate user) | Each ABCDE check carries an icon key (`icon` field per node); UI milestone renders large picture cues. |
| Audio prompts in the user's language for every step | Every node/option/disposition already carries an `audioKey`; voice layer (later milestone) plays pre-recorded native clips. |
| Fixed, ordered checklist in clear phases | The tree enforces Scene → Primary (ABCDE) → Secondary → Disposition; the user cannot skip or reorder. |
| Escalation baked in, tiered, automatic | Each disposition carries an `escalation` tier/action; immediate findings interrupt, lower tiers prompt at the end (M4). |
| Vitals → hard numeric thresholds that auto-alert | Vitals questions set flags; auto-eval nodes compute severity from flags — the tool decides, not the panicked user. |
| Offline-first, guarantee the help-request on weak signal | Production target is the Android app's existing offline Room sync queue; SMS-fallback dispatch in M4. |
| A 24/7 human on the other end | Escalation needs a live on-call/dispatch endpoint (open item — see README). |
| Validate proficiency with an observable checklist + retraining | Adopt a safe+natal-style task-observation checklist and a 90% pass bar for CHW sign-off. |
| Low-cost phone-tethered measurement devices | BP cuff / pulse oximeter integration is a candidate for a later milestone. |
| Sync to a shared record for follow-up and audit | Optional `/cases` backend sync mirroring the existing reports pattern. |

## Sources

- Martinez B, et al. *mHealth intervention to improve the continuum of maternal
  and perinatal care in rural Guatemala: a pragmatic, randomized controlled
  feasibility trial.* Reproductive Health 2018;15(1):120.
  https://pmc.ncbi.nlm.nih.gov/articles/PMC6033207/ (DOI 10.1186/s12978-018-0554-z)
- NICHD (NIH) — Developing Mobile Health Solutions for Women in Guatemala:
  https://www.nichd.nih.gov/newsroom/news/091421-mobile_health_solutions
- Fogarty International Center (NIH) — mHealth app reduces LMIC pregnancy risks:
  https://www.fic.nih.gov/News/GlobalHealthMatters/july-august-2021/Pages/mHealth-app-reduces-LMIC-pregnancy-delivery-risks.aspx
- safe+natal evidence (publication list): https://safenatal.org/evidence/
- Emory Co-Design Lab for Health Equity — safe+natal:
  https://codesign.emory.edu/press-and-publications/safe+natal.html
- Emory News — Google.org support (2023):
  https://news.emory.edu/stories/2023/09/hs_google_safe_natal_grant_12-09-2023/story.html
- Wuqu' Kawoq (Maya Health Alliance) maternal health:
  https://wuqukawoq.org/maternal-health/
