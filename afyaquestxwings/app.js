/* AfyaQuest × ALAS — prototype interactions
   Comprehensive: screen nav with transitions, demo mode,
   toasts, bottom sheets, slider, chat, search, filters, i18n. */

(function () {
    "use strict";

    // ============================================================
    //   STATE
    // ============================================================
    const state = {
        currentScreen: "screen-welcome",
        lang: "EN",
        gestationalAge: 32,
        clinicIdx: 0,
        bookmarks: new Set(),
    };

    const clinics = [
        { name: "Clínica San Juan", distance: "8.4 km",  eta: "14 min", caps: ["MgSO₄"] },
        { name: "Hospital Sololá",  distance: "12.1 km", eta: "22 min", caps: ["MgSO₄", "OR"] },
        { name: "C.S. Panajachel",  distance: "6.2 km",  eta: "11 min", caps: ["IV"] },
    ];

    const replies = [
        "Copy. Keep the BP log coming.",
        "If pulse goes above 110, escalate now.",
        "OR team prepping in case of seizure.",
        "Got it — meeting the ambulance at the door.",
        "Bay 3 is ready. ETA looks good.",
    ];
    let replyIdx = 0;

    const i18n = {
        EN: {
            "welcome.lang": "Español",
            "welcome.skip": "Skip",
            "welcome.title.a": "Emergency care,",
            "welcome.title.b": "on her side.",
            "welcome.sub": "A faster path from the field to the right facility — for pregnant women and anyone navigating reproductive care.",
            "welcome.chip.1": "Field-tested",
            "welcome.chip.2": "Offline-ready",
            "welcome.chip.3": "3 langs",
            "welcome.primary": "Start a triage",
            "welcome.primary.meta": "For CHVs and Alas teams",
            "welcome.secondary": "I'm in distress · Get help",
            "welcome.secondary.meta": "Patient-facing flow",
            "welcome.signin": "Already a CHV?",
            "welcome.signin.link": "Sign in",
            "toast.langChanged": "Language: English",
        },
        ES: {
            "welcome.lang": "Kaqchikel",
            "welcome.skip": "Saltar",
            "welcome.title.a": "Atención de emergencia,",
            "welcome.title.b": "a su lado.",
            "welcome.sub": "Un camino más rápido desde el campo al centro indicado — para mujeres embarazadas y quienes navegan la atención reproductiva.",
            "welcome.chip.1": "Probado en campo",
            "welcome.chip.2": "Sin conexión",
            "welcome.chip.3": "3 idiomas",
            "welcome.primary": "Iniciar triaje",
            "welcome.primary.meta": "Para CHV y equipos Alas",
            "welcome.secondary": "Necesito ayuda",
            "welcome.secondary.meta": "Flujo para pacientes",
            "welcome.signin": "¿Ya eres CHV?",
            "welcome.signin.link": "Iniciar sesión",
            "toast.langChanged": "Idioma: Español",
        },
        KAQ: {
            "welcome.lang": "English",
            "welcome.skip": "Tatzu",
            "welcome.title.a": "Anin q'atik aq'om,",
            "welcome.title.b": "rik'in rija'.",
            "welcome.sub": "Jun chanik b'ey richin ri yawa' rajawax to'ïk — kichin ixoqi' yawayel chuqa' kichin ri xkikanoj kik'aslem.",
            "welcome.chip.1": "Lotz'ulun pa juyu'",
            "welcome.chip.2": "Manäq xokon",
            "welcome.chip.3": "3 ch'ab'äl",
            "welcome.primary": "Tikitikirisaj triaje",
            "welcome.primary.meta": "Kichin CHV chuqa' Alas",
            "welcome.secondary": "Tinwajo' to'ïk",
            "welcome.secondary.meta": "Rub'eyal yawayel",
            "welcome.signin": "¿Ja kan CHV?",
            "welcome.signin.link": "Tatikirisaj",
            "toast.langChanged": "Ch'ab'äl: Kaqchikel",
        },
    };

    // ============================================================
    //   UTILS
    // ============================================================
    const $ = (sel, ctx = document) => ctx.querySelector(sel);
    const $$ = (sel, ctx = document) => Array.from(ctx.querySelectorAll(sel));

    const escapeHtml = s => String(s).replace(/[&<>"']/g, c =>
        ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));

    // --- Toast ---
    let toastTimer;
    function toast(message, opts = {}) {
        const el = $("#toast");
        if (!el) return;
        clearTimeout(toastTimer);
        el.className = "toast";
        if (opts.variant) el.classList.add(`toast--${opts.variant}`);
        $(".toast__text", el).textContent = message;
        const iconEl = $(".toast__icon", el);
        if (opts.icon) {
            iconEl.innerHTML = `<svg width="14" height="14"><use href="#${opts.icon}"/></svg>`;
            iconEl.style.display = "grid";
        } else {
            iconEl.innerHTML = "";
            iconEl.style.display = "none";
        }
        requestAnimationFrame(() => el.classList.add("toast--show"));
        toastTimer = setTimeout(() => el.classList.remove("toast--show"), opts.duration || 2400);
    }

    // --- Bottom sheet ---
    let sheetCleanupFns = [];
    let sheetCloseTimer = null;
    function openSheet(html, opts = {}) {
        clearTimeout(sheetCloseTimer);
        const sheet = $("#sheet");
        const content = $("#sheet-content");
        if (!sheet || !content) return;
        // Run any prior cleanup
        sheetCleanupFns.forEach(fn => { try { fn(); } catch (_) {} });
        sheetCleanupFns = [];
        content.innerHTML = html;
        sheet.hidden = false;
        requestAnimationFrame(() => sheet.classList.add("sheet--open"));
        $$("[data-sheet-close]", sheet).forEach(b => b.addEventListener("click", closeSheet));
        if (typeof opts.onOpen === "function") opts.onOpen(content, sheetCleanupFns);
    }

    function closeSheet() {
        const sheet = $("#sheet");
        if (!sheet || sheet.hidden) return;
        sheet.classList.remove("sheet--open");
        sheetCleanupFns.forEach(fn => { try { fn(); } catch (_) {} });
        sheetCleanupFns = [];
        clearTimeout(sheetCloseTimer);
        sheetCloseTimer = setTimeout(() => {
            sheet.hidden = true;
            $("#sheet-content").innerHTML = "";
        }, 300);
    }

    // ============================================================
    //   I18N
    // ============================================================
    function applyLang() {
        const dict = i18n[state.lang] || i18n.EN;
        $$("[data-i18n]").forEach(el => {
            const key = el.dataset.i18n;
            if (dict[key] != null) el.textContent = dict[key];
        });
    }

    function setLang(lang, opts = {}) {
        if (!i18n[lang]) return;
        const changed = lang !== state.lang;
        state.lang = lang;
        applyLang();
        $$(".lang-toggle__btn").forEach(b =>
            b.classList.toggle("lang-toggle__btn--active", b.dataset.lang === lang));
        if (changed && !opts.silent) {
            toast(i18n[lang]["toast.langChanged"] || `Language: ${lang}`, { icon: "i-globe" });
        }
    }

    $$(".lang-toggle__btn").forEach(btn =>
        btn.addEventListener("click", () => setLang(btn.dataset.lang)));

    $(".welcome__lang")?.addEventListener("click", () => {
        const order = ["EN", "ES", "KAQ"];
        setLang(order[(order.indexOf(state.lang) + 1) % order.length]);
    });

    // ============================================================
    //   SCREEN NAVIGATION
    // ============================================================
    const navButtons = $$("[data-screen-target]");
    const screens = $$(".screen");
    const screenOrder = screens.map(s => s.id);

    function switchTo(id) {
        if (!screenOrder.includes(id)) return;
        const fromIdx = screenOrder.indexOf(state.currentScreen);
        const toIdx = screenOrder.indexOf(id);
        const dir = toIdx >= fromIdx ? "forward" : "back";

        screens.forEach(s =>
            s.classList.remove("screen--active", "screen--enter-forward", "screen--enter-back"));

        const target = document.getElementById(id);
        if (target) {
            target.classList.add("screen--active",
                dir === "forward" ? "screen--enter-forward" : "screen--enter-back");
            target.scrollTop = 0;
            $$(".hub__body, .triage__body, .plan__body, .case__body, .library__body, .welcome", target)
                .forEach(c => { c.scrollTop = 0; });
        }

        navButtons.forEach(b =>
            b.classList.toggle("screen-nav__item--active", b.dataset.screenTarget === id));
        $$(".demo-dots__dot").forEach(d =>
            d.classList.toggle("demo-dots__dot--active", d.dataset.dot === id));

        state.currentScreen = id;
        closeSheet();
    }

    navButtons.forEach(b =>
        b.addEventListener("click", () => switchTo(b.dataset.screenTarget)));

    $$("[data-jump]").forEach(el => el.addEventListener("click", e => {
        // Validation for the Continue button on triage
        if (el.matches('.btn-primary[data-jump="screen-plan"]')) {
            if ($$(".symptom--checked").length === 0) {
                e.stopPropagation();
                toast("Tap at least one symptom to continue", { variant: "warn" });
                return;
            }
        }
        e.stopPropagation();
        switchTo(el.dataset.jump);
    }));

    $$(".demo-dots__dot").forEach(d =>
        d.addEventListener("click", () => switchTo(d.dataset.dot)));

    // ============================================================
    //   DEMO MODE
    // ============================================================
    const stage = $(".stage");
    const demoBtn = $("[data-demo-toggle]");
    demoBtn?.addEventListener("click", () => {
        stage.classList.toggle("stage--demo");
        const on = stage.classList.contains("stage--demo");
        const label = $(".demo-btn__label");
        if (label) label.textContent = on ? "Exit demo mode" : "Enter demo mode";
    });

    document.addEventListener("keydown", e => {
        // Don't capture keys while typing in inputs
        const tag = (e.target.tagName || "").toLowerCase();
        if (tag === "input" || tag === "textarea") return;

        if (e.key === "Escape") {
            if (!$("#sheet").hidden) { closeSheet(); return; }
            if (stage.classList.contains("stage--demo")) {
                stage.classList.remove("stage--demo");
                const label = $(".demo-btn__label");
                if (label) label.textContent = "Enter demo mode";
            }
            return;
        }
        if (stage.classList.contains("stage--demo")) {
            const idx = screenOrder.indexOf(state.currentScreen);
            if (e.key === "ArrowRight" && idx < screenOrder.length - 1) switchTo(screenOrder[idx + 1]);
            if (e.key === "ArrowLeft"  && idx > 0)                       switchTo(screenOrder[idx - 1]);
        }
    });

    // ============================================================
    //   WELCOME — skip, distress, sign in
    // ============================================================
    $(".welcome__skip")?.addEventListener("click", () => {
        toast("Skipped intro");
        switchTo("screen-hub");
    });

    $(".welcome__btn--secondary")?.addEventListener("click", () => {
        openSheet(`
            <div class="sheet__title">What kind of help do you need?</div>
            <div class="sheet__sub">If this is a medical emergency, please call your local emergency number.</div>
            <button class="sheet__choice" data-distress="counselor">
                <span class="sheet__choice-icon" style="background:var(--teal-50); color:var(--teal-700);">
                    <svg width="18" height="18"><use href="#i-phone"/></svg>
                </span>
                <div>
                    <div class="sheet__choice-title">Speak to a counselor</div>
                    <div class="sheet__choice-sub">Confidential · free · 24/7</div>
                </div>
            </button>
            <button class="sheet__choice" data-distress="clinic">
                <span class="sheet__choice-icon" style="background:var(--honey-100); color:var(--honey-700);">
                    <svg width="18" height="18"><use href="#i-pin"/></svg>
                </span>
                <div>
                    <div class="sheet__choice-title">Find a clinic near me</div>
                    <div class="sheet__choice-sub">Discreet, mapped by capability</div>
                </div>
            </button>
            <button class="sheet__choice sheet__choice--crit" data-distress="emergency">
                <span class="sheet__choice-icon" style="background:rgba(255,255,255,0.22); color:white;">
                    <svg width="18" height="18"><use href="#i-phone"/></svg>
                </span>
                <div>
                    <div class="sheet__choice-title">Call emergency services</div>
                    <div class="sheet__choice-sub">Bomberos · 122</div>
                </div>
            </button>
        `, { onOpen(content) {
            $$("[data-distress]", content).forEach(btn => btn.addEventListener("click", () => {
                const kind = btn.dataset.distress;
                closeSheet();
                if (kind === "emergency") setTimeout(openCallSheet, 300);
                else if (kind === "clinic") toast("Locating nearest clinics…", { icon: "i-pin" });
                else toast("Connecting with a counselor…", { icon: "i-phone" });
            }));
        }});
    });

    function openSignIn() {
        openSheet(`
            <div class="sheet__title">Sign in</div>
            <div class="sheet__sub">Enter your CHV ID and PIN.</div>
            <label class="sheet__field">
                <span>CHV ID</span>
                <input type="text" placeholder="e.g. SLA-0421" value="SLA-0421" />
            </label>
            <label class="sheet__field">
                <span>PIN</span>
                <input type="password" placeholder="••••" value="••••" />
            </label>
            <button class="sheet__action" data-signin>Sign in</button>
        `, { onOpen(content) {
            $("[data-signin]", content).addEventListener("click", () => {
                closeSheet();
                toast("Signed in as María Tzul", { icon: "i-check", variant: "ok" });
                setTimeout(() => switchTo("screen-hub"), 380);
            });
        }});
    }
    $(".welcome__signin strong")?.addEventListener("click", openSignIn);

    // ============================================================
    //   HUB
    // ============================================================
    $(".hub__avatar")?.addEventListener("click", openProfile);

    function openProfile() {
        openSheet(`
            <div class="sheet__profile">
                <div class="sheet__profile-avatar">MT</div>
                <div>
                    <div class="sheet__profile-name">María Tzul</div>
                    <div class="sheet__profile-meta">CHV #SLA-0421 · Sololá zone</div>
                </div>
            </div>
            <button class="sheet__choice" data-profile="cases">
                <span class="sheet__choice-icon" style="background:var(--teal-50);color:var(--teal-700);">
                    <svg width="16" height="16"><use href="#i-cases"/></svg>
                </span>
                <div>
                    <div class="sheet__choice-title">My cases</div>
                    <div class="sheet__choice-sub">3 active · 11 this week</div>
                </div>
            </button>
            <button class="sheet__choice" data-profile="settings">
                <span class="sheet__choice-icon" style="background:var(--ink-50);color:var(--ink-700);">
                    <svg width="16" height="16"><use href="#i-globe"/></svg>
                </span>
                <div>
                    <div class="sheet__choice-title">Settings</div>
                    <div class="sheet__choice-sub">Language, notifications, sync</div>
                </div>
            </button>
            <button class="sheet__choice" data-profile="signout">
                <span class="sheet__choice-icon" style="background:var(--rose-100);color:var(--rose-700);">
                    <svg width="16" height="16"><use href="#i-arrow-right"/></svg>
                </span>
                <div>
                    <div class="sheet__choice-title">Sign out</div>
                </div>
            </button>
        `, { onOpen(content) {
            $("[data-profile='cases']", content).addEventListener("click", () => {
                closeSheet();
                toast("Showing all my cases");
            });
            $("[data-profile='settings']", content).addEventListener("click", () => {
                closeSheet();
                toast("Settings opened");
            });
            $("[data-profile='signout']", content).addEventListener("click", () => {
                closeSheet();
                toast("Signed out", { icon: "i-check" });
                setTimeout(() => switchTo("screen-welcome"), 380);
            });
        }});
    }

    $(".hub__shift-cta")?.addEventListener("click", () => {
        toast("Shift active · 2h 14m remaining", { icon: "i-clock" });
    });

    // Stat cells — visual filter on case list
    let activeStatCell = null;
    $$(".stat-cell").forEach((cell, idx) => {
        cell.addEventListener("click", () => {
            const wasActive = activeStatCell === cell;
            $$(".stat-cell").forEach(c => c.classList.remove("stat-cell--active"));
            $$(".case-row").forEach(r => r.classList.remove("case-row--dim"));
            if (wasActive) {
                activeStatCell = null;
                toast("Filter cleared");
                return;
            }
            activeStatCell = cell;
            cell.classList.add("stat-cell--active");
            const label = $(".stat-cell__label", cell).textContent.trim();
            toast(`Filtering · ${label}`);
            const rows = $$(".case-row");
            if (idx === 1) rows.slice(0, 1).forEach(r => r.classList.add("case-row--dim"));
            else if (idx === 2) rows.slice(0, 2).forEach(r => r.classList.add("case-row--dim"));
        });
    });

    // Generic "See all" / section-head action (only those without data-jump)
    $$(".section-head__action").forEach(b => {
        if (b.dataset.jump) return;
        b.addEventListener("click", () => toast("Opening full list"));
    });

    // Case rows (non-data-jump)
    $$(".case-row").forEach(row => {
        if (row.hasAttribute("data-jump")) return;
        row.addEventListener("click", () => {
            const name = $(".case-row__title", row)?.firstChild?.textContent?.trim() || "case";
            toast(`Opening ${name}`, { icon: "i-cases" });
        });
    });

    // Quick library items → jump to library + open article
    $$(".quick-library__item").forEach(item => {
        item.addEventListener("click", () => {
            const title = $(".quick-library__title", item)?.textContent.trim() || "";
            switchTo("screen-library");
            setTimeout(() => openLibraryArticle(title), 360);
        });
    });

    // Bottom nav
    $$(".bottom-nav__item").forEach(item => {
        item.addEventListener("click", () => {
            const label = item.textContent.trim().split(/\s+/)[0];
            if (label === "Home")         switchTo("screen-hub");
            else if (label === "Cases")   switchTo("screen-case");
            else if (label === "Library") switchTo("screen-library");
            else if (label === "Me")      openProfile();
        });
    });

    $(".bottom-nav__sos")?.addEventListener("click", openSOS);

    function openSOS() {
        const phone = $(".phone");
        if (phone) {
            phone.classList.remove("phone--sos");
            void phone.offsetWidth; // restart animation
            phone.classList.add("phone--sos");
            setTimeout(() => phone.classList.remove("phone--sos"), 1300);
        }
        openSheet(`
            <div class="sheet__title" style="color:var(--crit-red);">Emergency</div>
            <div class="sheet__sub">What do you need right now?</div>
            <button class="sheet__choice sheet__choice--crit" data-sos="call">
                <span class="sheet__choice-icon" style="background:rgba(255,255,255,0.22);color:white;">
                    <svg width="18" height="18"><use href="#i-phone"/></svg>
                </span>
                <div>
                    <div class="sheet__choice-title">Call Bomberos · 122</div>
                    <div class="sheet__choice-sub">Ambulance dispatch</div>
                </div>
            </button>
            <button class="sheet__choice" data-sos="super">
                <span class="sheet__choice-icon" style="background:var(--honey-100);color:var(--honey-700);">
                    <svg width="18" height="18"><use href="#i-bell"/></svg>
                </span>
                <div>
                    <div class="sheet__choice-title">Notify supervisor</div>
                    <div class="sheet__choice-sub">Lic. Hernández · ALAS</div>
                </div>
            </button>
            <button class="sheet__choice" data-sos="protocol">
                <span class="sheet__choice-icon" style="background:var(--teal-50);color:var(--teal-700);">
                    <svg width="18" height="18"><use href="#i-note"/></svg>
                </span>
                <div>
                    <div class="sheet__choice-title">Open emergency protocol</div>
                    <div class="sheet__choice-sub">OB step-by-step triage</div>
                </div>
            </button>
        `, { onOpen(content) {
            $("[data-sos='call']", content).addEventListener("click", () => { closeSheet(); setTimeout(openCallSheet, 300); });
            $("[data-sos='super']", content).addEventListener("click", () => { closeSheet(); toast("Supervisor notified", { icon: "i-check", variant: "ok" }); });
            $("[data-sos='protocol']", content).addEventListener("click", () => { closeSheet(); switchTo("screen-triage"); });
        }});
    }

    // OB / RH triage card variant switch (data-jump handles nav for OB)
    $(".triage-card--ob")?.addEventListener("click", () => {
        const head = $("#screen-triage .triage__head");
        head?.classList.remove("triage__head--rh");
        const title = $("#screen-triage .appbar__title");
        if (title) title.textContent = "Obstetric triage";
    });
    $(".triage-card--rh")?.addEventListener("click", () => {
        const head = $("#screen-triage .triage__head");
        head?.classList.add("triage__head--rh");
        const title = $("#screen-triage .appbar__title");
        if (title) title.textContent = "Reproductive triage";
        switchTo("screen-triage");
    });

    // ============================================================
    //   TRIAGE WIZARD
    // ============================================================
    $$(".symptom").forEach(row => {
        row.addEventListener("click", () => {
            row.classList.toggle("symptom--checked");
            updateTriageSeverity();
        });
    });

    $$("[data-chip-group]").forEach(group =>
        $$(".chip", group).forEach(chip => chip.addEventListener("click", () => {
            $$(".chip", group).forEach(c => c.classList.remove("chip--active"));
            chip.classList.add("chip--active");
        }))
    );

    function updateTriageSeverity() {
        const checked = $$(".symptom--checked").length;
        const critical = $$(".symptom--checked.symptom--crit").length;
        const valueEl = $(".triage__severity-value");
        if (!valueEl) return;

        let label, cls;
        if (critical >= 1 || checked >= 3) { label = "High · refer now"; cls = "crit"; }
        else if (checked >= 1) { label = "Moderate · keep watch"; cls = "amber"; }
        else { label = "Awaiting input"; cls = "muted"; }

        valueEl.innerHTML = `<span class="severity-pulse"></span>${label}`;
        valueEl.style.color =
            cls === "crit"  ? "var(--crit-red)"   :
            cls === "amber" ? "var(--warn-amber)" :
                              "var(--ink-300)";
        const dot = $(".triage__severity .severity-pulse");
        if (dot) {
            dot.style.background =
                cls === "crit"  ? "var(--crit-red)"   :
                cls === "amber" ? "var(--warn-amber)" :
                                  "var(--ink-300)";
            dot.style.boxShadow =
                cls === "crit"  ? "0 0 0 4px rgba(200, 30, 30, 0.18)"  :
                cls === "amber" ? "0 0 0 4px rgba(183, 121, 31, 0.18)" :
                                  "0 0 0 4px rgba(154, 168, 165, 0.18)";
        }
        const continueBtn = $('.btn-primary[data-jump="screen-plan"]');
        if (continueBtn) {
            const disabled = checked === 0;
            continueBtn.toggleAttribute("disabled", disabled);
            continueBtn.style.opacity = disabled ? "0.55" : "1";
        }
    }

    // Mic / voice mode in triage header
    $('#screen-triage .appbar__action')?.addEventListener("click", () => {
        openSheet(`
            <div class="sheet__voice">
                <div class="sheet__voice-mic">
                    <svg width="34" height="34" style="color:white"><use href="#i-mic"/></svg>
                    <span class="sheet__voice-ring"></span>
                    <span class="sheet__voice-ring sheet__voice-ring--2"></span>
                </div>
                <div class="sheet__title" style="margin-top:18px;">Listening…</div>
                <div class="sheet__sub">Describe what you observe. Severity will update as you speak.</div>
                <div class="sheet__voice-transcript">"She has a severe headache and her hands are swollen. She's 32 weeks…"</div>
                <button class="sheet__action sheet__action--ghost" data-sheet-close>Tap to stop</button>
            </div>
        `);
    });

    // Gestational age slider — drag (mouse + touch)
    (function gaSlider() {
        const track  = $(".range-track");
        const fill   = $(".range-track__fill");
        const handle = $(".range-track__handle");
        const value  = $(".weeks-display__value");
        const ga     = $(".weeks-display__ga");
        if (!track) return;

        function setGa(weeks) {
            weeks = Math.max(4, Math.min(40, Math.round(weeks)));
            state.gestationalAge = weeks;
            const pct = ((weeks - 4) / 36) * 100;
            if (fill)   fill.style.width = pct + "%";
            if (handle) handle.style.left = pct + "%";
            if (value)  value.textContent = weeks;
            if (ga) {
                ga.textContent = weeks < 14 ? "1st trimester"
                              : weeks < 28 ? "2nd trimester"
                              :              "3rd trimester";
            }
        }
        function dragFromEvent(e) {
            const r = track.getBoundingClientRect();
            const x = e.touches ? e.touches[0].clientX : e.clientX;
            const pct = Math.max(0, Math.min(1, (x - r.left) / r.width));
            setGa(4 + pct * 36);
        }

        let dragging = false;
        const onDown = e => {
            dragging = true;
            document.body.style.userSelect = "none";
            dragFromEvent(e);
            e.preventDefault();
        };
        const onMove = e => { if (dragging) dragFromEvent(e); };
        const onUp = () => {
            if (!dragging) return;
            dragging = false;
            document.body.style.userSelect = "";
        };
        track.addEventListener("mousedown", onDown);
        track.addEventListener("touchstart", onDown, { passive: false });
        window.addEventListener("mousemove", onMove);
        window.addEventListener("touchmove", onMove, { passive: false });
        window.addEventListener("mouseup", onUp);
        window.addEventListener("touchend", onUp);
    })();

    // ============================================================
    //   ACTION PLAN
    // ============================================================
    $('#screen-plan .appbar__action')?.addEventListener("click", () => {
        openSheet(`
            <div class="sheet__title">Notifications</div>
            <div class="sheet__sub">Tracking this case · auto-updates</div>
            <div class="sheet__notif">
                <div class="sheet__notif-icon" style="background:var(--ok-green-bg);color:var(--ok-green);">
                    <svg width="14" height="14"><use href="#i-check"/></svg>
                </div>
                <div>
                    <div class="sheet__notif-title">Clínica San Juan acknowledged</div>
                    <div class="sheet__notif-sub">Dr. Velásquez · 2 min ago</div>
                </div>
            </div>
            <div class="sheet__notif">
                <div class="sheet__notif-icon" style="background:var(--rose-100);color:var(--rose-700);">
                    <svg width="14" height="14"><use href="#i-phone"/></svg>
                </div>
                <div>
                    <div class="sheet__notif-title">Ambulance dispatched</div>
                    <div class="sheet__notif-sub">Driver: Carlos · 8 min out</div>
                </div>
            </div>
            <div class="sheet__notif">
                <div class="sheet__notif-icon" style="background:var(--honey-100);color:var(--honey-700);">
                    <svg width="14" height="14"><use href="#i-clock"/></svg>
                </div>
                <div>
                    <div class="sheet__notif-title">Severity escalated to High</div>
                    <div class="sheet__notif-sub">Just now · auto</div>
                </div>
            </div>
        `);
    });

    // Reroute cycles clinics
    $(".map-card__alt")?.addEventListener("click", () => {
        state.clinicIdx = (state.clinicIdx + 1) % clinics.length;
        const c = clinics[state.clinicIdx];
        $(".map-card__facility-name").textContent = `${c.name} · ${c.eta}`;
        const meta = $(".map-card__facility-meta");
        meta.innerHTML = `${c.distance} · OB on call ` +
            c.caps.map(x => `<span class="map-card__capability">${x}</span>`).join(" ");
        $(".map-pin--clinic .map-pin__label").textContent = c.name;
        toast(`Routing to ${c.name}`, { icon: "i-route" });
    });

    // Action tiles
    $(".action-tile--call")?.addEventListener("click", openCallSheet);
    $$(".action-tile").forEach(tile => {
        if (tile.classList.contains("action-tile--call")) return;
        tile.addEventListener("click", () => {
            const label = $(".action-tile__label", tile)?.textContent.trim();
            if (label === "Notify clinic") {
                toast("Notified Clínica San Juan · ETA sent", { icon: "i-check", variant: "ok" });
                tile.classList.add("action-tile--done");
                const sub = $(".action-tile__sub", tile);
                if (sub) sub.textContent = "Acknowledged 9:42";
            } else if (label === "Get directions") {
                toast("Opening Maps…", { icon: "i-route" });
            } else if (label === "Voice note") {
                openVoiceNote();
            }
        });
    });

    function openVoiceNote() {
        openSheet(`
            <div class="sheet__voice">
                <div class="sheet__voice-mic sheet__voice-mic--rec">
                    <svg width="34" height="34" style="color:white"><use href="#i-mic"/></svg>
                    <span class="sheet__voice-ring"></span>
                    <span class="sheet__voice-ring sheet__voice-ring--2"></span>
                </div>
                <div class="sheet__title" style="margin-top:18px;">Recording…</div>
                <div class="sheet__sub">Speak normally. Audio attaches to the case.</div>
                <div class="sheet__voice-timer" data-voice-timer>0:00</div>
                <button class="sheet__action" data-stop-rec>Stop &amp; save</button>
            </div>
        `, { onOpen(content, cleanupFns) {
            let secs = 0;
            const timerEl = $("[data-voice-timer]", content);
            const t = setInterval(() => {
                secs++;
                const m = Math.floor(secs / 60);
                const s = (secs % 60).toString().padStart(2, "0");
                if (timerEl) timerEl.textContent = `${m}:${s}`;
            }, 1000);
            cleanupFns.push(() => clearInterval(t));
            $("[data-stop-rec]", content).addEventListener("click", () => {
                clearInterval(t);
                closeSheet();
                toast("Voice note saved to case", { icon: "i-check", variant: "ok" });
            });
        }});
    }

    // Steps - togglable done/undone
    function refreshStepCount() {
        const total = $$(".step").length;
        const done = $$(".step--done").length;
        const countEl = $(".step-list__count");
        if (countEl) countEl.textContent = `${total} steps · ${done} done`;
    }

    $$(".step").forEach((step, i) => {
        step.addEventListener("click", () => {
            const wasDone = step.classList.contains("step--done");
            step.classList.toggle("step--done");
            const numEl = $(".step__num", step);
            if (!wasDone) {
                numEl.innerHTML = `<svg width="14" height="14"><use href="#i-check"/></svg>`;
            } else {
                numEl.textContent = String(i + 1);
            }
            refreshStepCount();
        });
    });

    // Save draft / Hand off
    $$(".btn-secondary").forEach(b => b.addEventListener("click", () => {
        const label = b.textContent.trim();
        if (label === "Save draft") toast("Draft saved · syncs when online", { icon: "i-check" });
        else if (label === "Hand off") openHandoff();
    }));

    function openHandoff() {
        openSheet(`
            <div class="sheet__title">Hand off case</div>
            <div class="sheet__sub">Transfer this case to another team member.</div>
            <button class="sheet__choice" data-hand="Dr. Mendoza">
                <span class="sheet__choice-icon" style="background:var(--teal-100);color:var(--teal-700);">DM</span>
                <div>
                    <div class="sheet__choice-title">Dr. Mendoza</div>
                    <div class="sheet__choice-sub">Clínica SJ · online</div>
                </div>
            </button>
            <button class="sheet__choice" data-hand="Lic. Hernández">
                <span class="sheet__choice-icon" style="background:var(--rose-100);color:var(--rose-700);">LH</span>
                <div>
                    <div class="sheet__choice-title">Lic. Hernández</div>
                    <div class="sheet__choice-sub">ALAS supervisor</div>
                </div>
            </button>
            <button class="sheet__choice" data-hand="Juana P.">
                <span class="sheet__choice-icon" style="background:var(--honey-100);color:var(--honey-700);">JP</span>
                <div>
                    <div class="sheet__choice-title">Juana P.</div>
                    <div class="sheet__choice-sub">CHV · zone 4</div>
                </div>
            </button>
        `, { onOpen(content) {
            $$("[data-hand]", content).forEach(b => b.addEventListener("click", () => {
                const name = b.dataset.hand;
                closeSheet();
                toast(`Handed off to ${name}`, { icon: "i-check", variant: "ok" });
            }));
        }});
    }

    function openCallSheet() {
        openSheet(`
            <div class="sheet__call">
                <div class="sheet__call-avatar">
                    <svg width="28" height="28" style="color:white;"><use href="#i-phone"/></svg>
                    <span class="sheet__call-ring"></span>
                </div>
                <div class="sheet__call-name">Bomberos · 122</div>
                <div class="sheet__call-state" data-call-state>Calling…</div>
                <div class="sheet__call-timer" data-call-timer></div>
                <div class="sheet__call-actions">
                    <button class="sheet__call-btn" data-call-toggle="mute">Mute</button>
                    <button class="sheet__call-btn sheet__call-btn--end" data-sheet-close aria-label="End call">End</button>
                    <button class="sheet__call-btn" data-call-toggle="speaker">Speaker</button>
                </div>
            </div>
        `, { onOpen(content, cleanupFns) {
            const stateEl = $("[data-call-state]", content);
            const timerEl = $("[data-call-timer]", content);
            let secs = 0;
            let tick = null;
            const connectT = setTimeout(() => {
                stateEl.textContent = "Connected · sharing your location";
                stateEl.style.color = "var(--ok-green)";
                tick = setInterval(() => {
                    secs++;
                    const m = Math.floor(secs / 60);
                    const s = (secs % 60).toString().padStart(2, "0");
                    timerEl.textContent = `${m}:${s}`;
                }, 1000);
            }, 2200);
            cleanupFns.push(() => { clearTimeout(connectT); if (tick) clearInterval(tick); });
            $$("[data-call-toggle]", content).forEach(b => b.addEventListener("click", () => {
                b.classList.toggle("sheet__call-btn--on");
            }));
        }});
    }

    // ============================================================
    //   LIVE CASE — bookmark, vitals, chat
    // ============================================================
    $$('#screen-case .appbar__action, #screen-library .appbar__action').forEach(b => {
        b.addEventListener("click", () => {
            const filled = b.classList.toggle("appbar__action--filled");
            const screen = b.closest(".screen").id;
            if (filled) state.bookmarks.add(screen); else state.bookmarks.delete(screen);
            toast(filled ? "Saved to your bookmarks" : "Removed from bookmarks",
                  { icon: filled ? "i-bookmark" : null });
        });
    });

    $("#screen-case .section-head__action")?.addEventListener("click", () => {
        const now = new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
        openSheet(`
            <div class="sheet__title">Update vitals</div>
            <div class="sheet__sub">Re-checked at ${now}</div>
            <div class="sheet__vitals">
                <label class="sheet__field"><span>Blood pressure</span><input type="text" value="158/104" /></label>
                <label class="sheet__field"><span>Pulse (bpm)</span><input type="number" value="96" /></label>
                <label class="sheet__field"><span>FHR (bpm)</span><input type="number" value="142" /></label>
                <label class="sheet__field"><span>Note (optional)</span><input type="text" placeholder="Any observations…" /></label>
            </div>
            <button class="sheet__action" data-save-vitals>Save vitals</button>
        `, { onOpen(content) {
            $("[data-save-vitals]", content).addEventListener("click", () => {
                const inputs = $$(".sheet__field input", content);
                const bp = inputs[0].value.trim() || "—";
                const pulse = inputs[1].value.trim() || "—";
                const fhr = inputs[2].value.trim() || "—";
                $$("#screen-case .vital").forEach((v, i) => {
                    const valueEl = $(".vital__value", v);
                    if (!valueEl) return;
                    if (i === 0) valueEl.textContent = bp;
                    if (i === 1) valueEl.innerHTML = `${escapeHtml(pulse)}<span class="vital__unit">bpm</span>`;
                    if (i === 2) valueEl.innerHTML = `${escapeHtml(fhr)}<span class="vital__unit">bpm</span>`;
                });
                closeSheet();
                toast("Vitals updated · synced to clinic", { icon: "i-check", variant: "ok" });
            });
        }});
    });

    // Chat
    const chatForm = $("#chat-form");
    const chatInput = $("#chat-input");
    const chatMessages = $(".chat__messages");

    function appendChatBubble(text, from = "out") {
        if (!chatMessages) return;
        const bubble = document.createElement("div");
        bubble.className = `chat__bubble chat__bubble--${from}`;
        bubble.textContent = text;
        chatMessages.appendChild(bubble);
        const body = $(".case__body");
        if (body) body.scrollTop = body.scrollHeight;
    }

    chatForm?.addEventListener("submit", e => {
        e.preventDefault();
        const text = chatInput.value.trim();
        if (!text) return;
        appendChatBubble(text, "out");
        chatInput.value = "";

        const typing = document.createElement("div");
        typing.className = "chat__bubble chat__bubble--in chat__bubble--typing";
        typing.innerHTML = `<span></span><span></span><span></span>`;
        chatMessages.appendChild(typing);
        const body = $(".case__body");
        if (body) body.scrollTop = body.scrollHeight;

        setTimeout(() => {
            typing.remove();
            const reply = replies[replyIdx % replies.length];
            replyIdx++;
            appendChatBubble(reply, "in");
        }, 1000 + Math.random() * 600);
    });

    // ============================================================
    //   LIBRARY
    // ============================================================
    $$(".library__chips .chip").forEach(chip => {
        chip.addEventListener("click", () => {
            $$(".library__chips .chip").forEach(c => c.classList.remove("chip--active"));
            chip.classList.add("chip--active");
            const tag = chip.textContent.trim();
            const term = $("#library-search")?.value || "";
            filterLibrary(tag, term);
        });
    });

    $("#library-search")?.addEventListener("input", e => {
        const active = $(".library__chips .chip--active");
        const tag = active ? active.textContent.trim() : "All";
        filterLibrary(tag, e.target.value);
    });

    function filterLibrary(tag, term) {
        const t = term.trim().toLowerCase();
        const tl = tag.toLowerCase();
        let any = false;
        $$(".library-card").forEach(card => {
            const title = $(".library-card__title", card)?.textContent.toLowerCase() || "";
            const cat = $(".library-card__category", card)?.textContent.toLowerCase() || "";
            const matchTag = tag === "All" || cat.includes(tl);
            const matchTerm = !t || title.includes(t) || cat.includes(t);
            const show = matchTag && matchTerm;
            card.style.display = show ? "" : "none";
            if (show) any = true;
        });
        const existingHint = $("#library-empty");
        if (existingHint) existingHint.remove();
        if (!any) {
            const list = $(".library__body");
            const hint = document.createElement("div");
            hint.id = "library-empty";
            hint.style.cssText = "padding:24px 0; text-align:center; color:var(--ink-500); font-size:13px;";
            hint.textContent = `No guides match "${term || tag}"`;
            list?.appendChild(hint);
        }
    }

    $$(".library-card").forEach(card => {
        card.addEventListener("click", () => {
            const title = $(".library-card__title", card)?.textContent.trim() || "Article";
            openLibraryArticle(title);
        });
    });

    function openLibraryArticle(title) {
        openSheet(`
            <div class="sheet__article">
                <div class="sheet__sub" style="margin-bottom:4px;">Care library · offline</div>
                <div class="sheet__title">${escapeHtml(title)}</div>
                <div class="sheet__article-meta">4 min read · EN · ES · KAQ · Saved offline</div>
                <div class="sheet__article-body">
                    <p><strong>What you'll see.</strong> Severe headaches that don't go away.
                    Vision changes — spots, blurring, or sensitivity to light. Sudden swelling
                    in the face or hands. Pain in the upper right abdomen.</p>
                    <p><strong>What to do.</strong> Position her on her left side. Keep the room
                    dim and quiet. Do not give food or fluids by mouth. Track BP every 10 min.
                    Refer immediately to a facility with magnesium sulfate.</p>
                    <p><strong>What not to do.</strong> Don't dismiss "just a headache." Don't
                    wait for a seizure before calling. Bright lights can trigger eclampsia.</p>
                </div>
                <button class="sheet__action" data-sheet-close>Done</button>
            </div>
        `);
    }

    // ============================================================
    //   INITIAL STATE
    // ============================================================
    applyLang();
    switchTo("screen-welcome");
    updateTriageSeverity();
    refreshStepCount();

})();
