# AfyaQuest × ALAS — Emergency Care Concept

A self-contained UI concept for the Tuesday demo. It pitches a partnership
surface area between **AfyaQuest** (CHV mobile platform) and **ALAS / Wings**
(women's reproductive health) focused on **emergency situations** — pregnant
women in crisis and women navigating contraceptive complications.

> Status: visual / interactive concept. No backend, no build step, no deps
> beyond Google Fonts via CDN. Open `index.html` and you're done.

---

## Run it

```bash
# from the repo root
open afyaquestxwings/index.html
# or
cd afyaquestxwings && python3 -m http.server 4178
# then visit http://localhost:4178
```

Press **Enter demo mode** in the sidebar (or hit `Esc` to exit) to hide the
chrome and just show the phone — handy for screen-recording or projecting.
In demo mode, `←` / `→` walk between the six screens.

---

## What's in the demo

Six screens, told as one story:

| # | Screen          | Purpose                                                     |
|---|-----------------|-------------------------------------------------------------|
| 1 | Welcome         | Co-brand lockup, tone, language picker, two entry doors     |
| 2 | Triage hub      | Field-first home: open cases, two emergency entry buttons   |
| 3 | Triage wizard   | Symptom-driven, computes severity live as you tap           |
| 4 | Action plan     | Severity badge, nearest *capable* facility, call + steps    |
| 5 | Live case       | Status, vitals, timeline, chat with receiving clinician     |
| 6 | Care library    | Offline reference cards, multilingual, with media           |

The flow is wired up — the buttons in each screen jump to the next logical
screen, and the symptom checkboxes on the triage wizard re-compute the
severity meter at the bottom.

---

## Why these design choices

- **Carries AfyaQuest's brand forward.** Same teal primary (`#254D4D`),
  same honey orange, same card vocabulary as the existing Compose app —
  this should feel like an *expansion* of AfyaQuest, not a fork.
- **Adds an "ALAS rose"** (`#E8526F`) as the women's-emergency surface
  color. Used sparingly — primary CTAs, OB triage chrome, severity dots.
- **Wing motif in the lockup**, not literal angel wings — more like a
  guardian/uplift mark. Subtle.
- **Severity is the central UX object.** The triage wizard shows it
  live; it becomes the badge on the action plan; it color-shifts the
  whole header. Makes the "is this an emergency?" question visible from
  the moment the CHV starts tapping.
- **Capability-aware routing.** The map card calls out `MgSO₄` on the
  recommended clinic — because "nearest" isn't useful if they can't
  treat what you found. This is the kind of detail ALAS's clinical
  team will care about.
- **The CHV is never alone.** Live case keeps the receiving doctor in
  the loop until handoff. We're augmenting the CHV's judgment, not
  replacing it.

---

## What's *not* in the demo (talk track)

- Patient-facing flow (the "I'm in distress" button on welcome).
  Worth scoping, but a separate week of work.
- Voice-first triage (mic button is shown but inert).
- Offline-first sync UX — exists in the Compose app already, would
  reuse the same `SyncStatusIndicator` component.
- Auth, profile, multi-CHV households — out of scope; reuse existing
  AfyaQuest modules.

---

## Preview without running it

Each screen rendered to `preview-*.png` so you can flip through the
flow on your phone or in PR review without spinning up a server:

- `preview-screen-welcome.png` — co-brand landing
- `preview-screen-hub.png` — triage hub / home
- `preview-screen-triage.png` — symptom-driven wizard (OB shown)
- `preview-screen-plan.png` — high-severity action plan
- `preview-screen-case.png` — live case en route
- `preview-screen-library.png` — care reference library
- `preview-phone-only.png` — phone-only crop, useful for slides

If you change the design and want to refresh these,
`/tmp/qa-render/render.js` will regenerate them.

---

## Files

```
afyaquestxwings/
├── index.html         ← single-page prototype, all six screens inline
├── styles.css         ← design tokens + per-screen styles
├── app.js             ← screen nav, demo mode, severity logic
├── README.md          ← this file
└── preview-*.png      ← rendered screenshots of each screen
```

No build, no install. If we like the direction, the next step is to
port these screens into Jetpack Compose against the existing AfyaQuest
theme — the color tokens already match.
