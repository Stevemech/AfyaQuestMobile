# AfyaQuest — Default store listing

Copy-paste source for **Grow users → Store presence → Store listings → Default store listing**,
language **Default – English (United States)**.

Every claim below was checked against the app source. See `FACT-CHECK` at the bottom.

---

## App name

*(30 char limit — already filled in, no change needed)*

```
AfyaQuest
```

---

## Short description

*(80 char limit — uses 69)*

```
Gamified training and daily field tools for community health workers.
```

---

## Full description

*(4000 char limit — uses ~2,470)*

```
AfyaQuest turns community health training into something you can actually finish: a few minutes at a time, on the phone already in your pocket, even where there is no signal.

Built for Community Health Workers and Community Health Volunteers, AfyaQuest pairs a gamified curriculum with the everyday tools of field work — your route, your visits, and your end-of-day report, all in one app.

TRAINING THAT FITS BETWEEN VISITS
• 36 short video lessons across 6 modules: body systems, common childhood illnesses, chronic and infectious diseases, maternal and reproductive health, first aid and emergency care, and infection prevention and control
• A quiz after every video, so what you learn sticks
• Three daily questions to keep your knowledge sharp
• Earn XP, build a daily streak, level up, and unlock achievements as you go

YOUR DAY, ORGANISED
• Clock in and out so your supervisor knows you are in the field
• Open your daily itinerary and see the households assigned to you
• Mark each stop as you work through your route
• File your end-of-day report: patients visited, vaccinations given, health education covered, and the challenges you ran into

TOOLS FOR THE FIELD
• Find nearby hospitals, clinics, and health posts, with the services each one offers and how far away it is
• Get directions to any facility
• Follow the Emergency Response Guide step by step, with text-to-speech so you can listen while your hands are busy
• Ask Fred, the in-app assistant, for a quick explanation when a topic is not clear

WORKS WHERE YOU WORK
• Download lessons and videos for offline use
• Reports, quiz results, and progress are saved on your phone and sync automatically once you have a signal
• Handwriting input for writing notes quickly
• Available in English, Spanish, and Kaqchikel

FOR PROGRAMMES AND SUPERVISORS
Supervisors and programme administrators get a web dashboard showing training completion, field activity, and submitted reports across their team, so support reaches the people who need it.

PRIVACY YOU CAN CHECK
AfyaQuest contains no advertising and no third-party analytics or tracking. Your device location is used only to show your own position on the map — it stays on your phone and is never uploaded. You can request deletion of your account and data at any time.

Privacy policy: https://afyaquest.com/privacy/
Data safety details: https://afyaquest.com/data-safety/

AfyaQuest is a training and workflow tool. It does not diagnose or treat any condition, does not provide medical advice, and is not a substitute for clinical judgement, your programme's protocols, or referral to a qualified health professional.

An AfyaQuest account is required to use the app.
```

---

## Graphics

All files are in `graphics/`. Every one is inside Play's size and format limits.

| Field | File | Spec | Actual |
|---|---|---|---|
| App icon | `graphics/app-icon-512.png` | PNG/JPEG, ≤1 MB, 512×512 | PNG, 35 KB, 512×512 |
| Feature graphic | `graphics/feature-graphic-1024x500.png` | PNG/JPEG, ≤15 MB, 1024×500 | PNG, 46 KB, 1024×500 |
| Phone screenshots | `graphics/screenshots/01-05` | 2–8 images, PNG/JPEG, ≤8 MB each, 16:9 or 9:16, sides 320–3840 px | 5 PNGs, ≤270 KB each, 1080×1920 (exactly 9:16) |

Upload the screenshots in numbered order — that is the order they appear on the store page,
and the first two are what most people actually see.

| # | Screen | Headline on the image |
|---|---|---|
| 01 | Dashboard | Your whole day, in one place |
| 02 | Video modules | 36 video lessons in 6 modules |
| 03 | Inside a module | Watch a video, then take the quiz |
| 04 | Health facilities | Find the nearest health facility |
| 05 | Profile | Level up as you learn |

**Video** — optional, leave blank. It only accepts a public or unlisted YouTube URL, and an
empty field will not block publishing.

**Tablet / Chromebook / Android XR assets** — optional. Leave all three collapsed unless you
declare support for those form factors, in which case Play will require their own screenshots.

To regenerate any of the graphics after editing copy or swapping a source capture:

```
cd play-store-listing
/Users/steve/Documents/GitHub/afyasite/website/.venv-crop/bin/python generate_assets.py
```

---

## Not on this page, but required before you can publish

These live elsewhere in the console and will block the release if left empty:

- **App content → Privacy policy** — `https://afyaquest.com/privacy/`
- **App content → Data safety** — the questionnaire; work through it alongside
  `https://afyaquest.com/data-safety/`
- **App content → App access** — the app is behind a login, so Play requires working demo
  credentials or the reviewer cannot get past the sign-in screen and will reject the build
- **App content → Ads** — declare "No ads" (no ad SDKs are present)
- **Store settings → App category** — recommend **Education**. The app's core is a training
  curriculum, and the Medical category pulls in extra health-app policy review that this app
  does not need, since it makes no diagnostic or treatment claims.
- **Store settings → Contact details** — email, and the website `https://afyaquest.com`

---

## FACT-CHECK

Claims in the copy above, and where each was verified in the source:

| Claim | Source |
|---|---|
| 36 video lessons, 6 modules | `VideoModulesViewModel.kt` — 8+6+6+6+5+5 videos across modules 1–6 |
| Module topics | `VideoModulesViewModel.kt` module comments |
| Quiz after every video | `VideoModulesScreen.kt` "Take Quiz" per video; `progress/quiz` endpoint |
| Three daily questions | Dashboard "Answer your three daily questions"; `questions/daily` |
| XP, streaks, levels, achievements | `UserEntity.kt` (`totalPoints`, `level`, `rank`, `currentStreak`), `AchievementEntity.kt` |
| Clock in / out | `ApiService.clockAction`, `AuthRepository.clockAction` |
| Itinerary and assigned households | `ApiService.getItineraries` / `getAssignments`, `ClientHouseEntity.kt` |
| End-of-day report fields | `ReportEntity.kt` — patientsVisited, vaccinationsGiven, healthEducation, challenges |
| Nearby facilities with services and distance | `HealthFacilityEntity.kt`, Health Facilities tab |
| Emergency Response Guide + text-to-speech | `docs/emergency-response/`, TTS `<queries>` in `AndroidManifest.xml` |
| Assistant named Fred | `ChatMessageEntity.kt` doc comment; `chat/sendMessage` lambda |
| Offline download + auto sync | `VideoDownloadManager.kt`, `SyncManager.kt`, `isSynced` flags |
| Handwriting input | `HandwritingRecognitionHelper.kt` (ML Kit Digital Ink) |
| English, Spanish, Kaqchikel | `res/values`, `res/values-es`, `res/values-b+cak`; `SettingsScreen.kt` |
| No ads, no third-party analytics | `app/build.gradle.kts` — no ad or analytics SDKs |
| Location never uploaded | `MapViewModel.kt` holds it in state only; `SyncManager.kt` never sends it |
| Supervisor web dashboard | `adminweb/` |

Two things the copy deliberately does **not** claim, because the source does not support them:

- **Kiswahili.** A stale comment in `UserEntity.kt` says `// 'en' or 'sw'`, but the shipped
  resources are English, Spanish, and Kaqchikel, and the demo data is Guatemalan. The comment is
  what is wrong, not the resources.
- **Any clinical benefit or outcome.** The closing disclaimer is deliberate — health apps that
  imply diagnosis or treatment draw a much heavier policy review.
