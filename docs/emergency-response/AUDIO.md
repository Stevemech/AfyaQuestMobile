# Voice layer — recording & production guide

The Emergency Response Guide reads each step aloud via a **"Tap to hear this
step"** button (question + every lettered option, or a disposition's label +
instructions). Audio resolves in this order:

1. **A pre-recorded native-speaker clip** for the active language, if one exists.
2. Otherwise **device text-to-speech** — but only for languages an engine can
   faithfully speak.

**Kaqchikel (`cak`) is never synthesized.** No mainstream TTS engine supports it,
and forcing it through Spanish phonemes mangles the orthography (`'`, `ä`, `k'`,
`tz'`, `x`). Until Kaqchikel clips are recorded, cak stays **text-only** — the
"Tap to hear" button simply doesn't appear in cak. English and Spanish use device
TTS today as an interim until es-GT clips are recorded.

## How playback is wired (already built — M5)

- `domain/triage/AudioManifest.kt` — `AudioManifest` + pure `AudioResolver`
  (the cak-never-synthesized rule; unit-tested in `AudioResolverTest`).
- `data/triage/TtsAudioManager.kt` — sequences clips (MediaPlayer) and TTS
  fallback; app-scoped singleton.
- `assets/audio/audio_manifest.json` — lists which clip keys exist per language.
- The flow/ViewModel call `speakCurrentStep()`; audio stops on answer/back/restart
  and when leaving the screen.

## To add real recordings (content task — no code change)

1. Record one clip per **audio key** below, per language, by a native speaker.
   - **es-GT** (Guatemalan Spanish) and **Kaqchikel (cak)** are the priorities for
     the Wuqu' Kawoq deployment; English optional (device TTS is acceptable).
   - Each clip = the on-screen text for that key. Option clips should read the
     letter too (e.g. *"A. Sí, es seguro"*) so the letter→action mapping is heard.
   - Clinical + linguistic sign-off per take (see CLINICAL-REVIEW.md).
2. Loudness-normalize, export mono `.mp3` (the manager expects `.mp3`).
3. Place at `assets/audio/<language>/<audioKey>.mp3`
   (e.g. `assets/audio/es/scene_safe.mp3`, `assets/audio/cak/scene_safe.mp3`).
4. List the recorded keys in `assets/audio/audio_manifest.json`:
   ```json
   { "version": "1", "clips": { "es": ["scene_safe", "scene_safe_a", ...],
                                "cak": ["scene_safe", ...] } }
   ```
   Only keys listed here are played from clips; everything else falls back per the
   rules above. This lets a partially-recorded language ship safely.

## Audio keys (77)

Question/option keys come straight from `triage_tree.json`; disposition keys add
a `_instructions` companion. Keep the file name identical to the key.

```
scene_safe  scene_safe_a  scene_safe_b
scene_ppe  scene_ppe_a  scene_ppe_b
scene_mechanism  scene_mechanism_a  scene_mechanism_b  scene_mechanism_c
prim_responsive  prim_responsive_a  prim_responsive_b
prim_airway_aware  prim_airway_aware_a  prim_airway_aware_b
prim_airway_unresp  prim_airway_unresp_a  prim_airway_unresp_b
prim_airway_open  prim_airway_open_a  prim_airway_open_b
prim_breathing  prim_breathing_a  prim_breathing_b  prim_breathing_c
prim_circulation  prim_circulation_a  prim_circulation_b
prim_disability  prim_disability_a  prim_disability_b  prim_disability_c  prim_disability_d
sec_t_head  sec_t_head_a  sec_t_head_b
sec_t_chest  sec_t_chest_a  sec_t_chest_b
sec_t_abdomen  sec_t_abdomen_a  sec_t_abdomen_b
sec_t_extremities  sec_t_extremities_a  sec_t_extremities_b
sec_t_spine  sec_t_spine_a  sec_t_spine_b
sec_m_conditions  sec_m_conditions_a  sec_m_conditions_b
sec_m_onset  sec_m_onset_a  sec_m_onset_b
sec_m_complaint  sec_m_complaint_a  sec_m_complaint_b  sec_m_complaint_c  sec_m_complaint_d
sec_m_vitals_breath  sec_m_vitals_breath_a  sec_m_vitals_breath_b
sec_m_vitals_pulse  sec_m_vitals_pulse_a  sec_m_vitals_pulse_b  sec_m_vitals_pulse_c
disp_unsafe  disp_unsafe_instructions
disp_cpr  disp_cpr_instructions
disp_immediate  disp_immediate_instructions
disp_priority  disp_priority_instructions
disp_stable  disp_stable_instructions
```
