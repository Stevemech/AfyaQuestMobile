/* AfyaQuest x ALAS — prototype interactions
   Lightweight, no framework. Handles screen swaps, demo mode,
   and a couple of small affordances inside the mock screens. */

(function () {
    "use strict";

    const navButtons = document.querySelectorAll("[data-screen-target]");
    const screens = document.querySelectorAll(".screen");

    function switchTo(id) {
        screens.forEach((s) => s.classList.toggle("screen--active", s.id === id));
        navButtons.forEach((b) =>
            b.classList.toggle(
                "screen-nav__item--active",
                b.dataset.screenTarget === id
            )
        );

        const screen = document.getElementById(id);
        if (screen) screen.scrollTop = 0;

        const noteSections = document.querySelectorAll(".notes-section");
        noteSections.forEach((n) =>
            n.classList.toggle("notes-section--active", n.dataset.noteFor === id)
        );
    }

    navButtons.forEach((btn) =>
        btn.addEventListener("click", () => switchTo(btn.dataset.screenTarget))
    );

    document.querySelectorAll("[data-jump]").forEach((el) =>
        el.addEventListener("click", () => switchTo(el.dataset.jump))
    );

    // --- Language toggle (visual only) ---
    document.querySelectorAll(".lang-toggle__btn").forEach((btn) => {
        btn.addEventListener("click", () => {
            btn.parentElement
                .querySelectorAll(".lang-toggle__btn")
                .forEach((b) => b.classList.remove("lang-toggle__btn--active"));
            btn.classList.add("lang-toggle__btn--active");
        });
    });

    // --- Demo / presenter mode ---
    const stage = document.querySelector(".stage");
    const demoBtn = document.querySelector("[data-demo-toggle]");
    if (demoBtn) {
        demoBtn.addEventListener("click", () => {
            stage.classList.toggle("stage--demo");
            const inDemo = stage.classList.contains("stage--demo");
            demoBtn.querySelector(".demo-btn__label").textContent = inDemo
                ? "Exit demo mode"
                : "Enter demo mode";
        });
    }

    // Pressing "Esc" exits demo mode
    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape" && stage.classList.contains("stage--demo")) {
            stage.classList.remove("stage--demo");
            const label = document.querySelector(".demo-btn__label");
            if (label) label.textContent = "Enter demo mode";
        }
        // Arrow keys to flip between screens in demo mode
        if (stage.classList.contains("stage--demo")) {
            const ids = Array.from(navButtons).map((b) => b.dataset.screenTarget);
            const current = document.querySelector(".screen--active")?.id;
            const idx = ids.indexOf(current);
            if (e.key === "ArrowRight" && idx < ids.length - 1) switchTo(ids[idx + 1]);
            if (e.key === "ArrowLeft" && idx > 0) switchTo(ids[idx - 1]);
        }
    });

    // --- Symptom checkbox toggling (in triage wizard) ---
    document.querySelectorAll(".symptom").forEach((row) => {
        row.addEventListener("click", () => {
            row.classList.toggle("symptom--checked");
            updateTriageSeverity();
        });
    });

    // --- Time-since chips (single select per group) ---
    document.querySelectorAll("[data-chip-group]").forEach((group) => {
        group.querySelectorAll(".chip").forEach((chip) => {
            chip.addEventListener("click", () => {
                group
                    .querySelectorAll(".chip")
                    .forEach((c) => c.classList.remove("chip--active"));
                chip.classList.add("chip--active");
            });
        });
    });

    function updateTriageSeverity() {
        const checked = document.querySelectorAll(".symptom--checked").length;
        const critical = document.querySelectorAll(".symptom--checked.symptom--crit").length;
        const valueEl = document.querySelector(".triage__severity-value");
        if (!valueEl) return;
        let level = { label: "Watch", cls: "warn-amber", show: true };
        if (critical >= 1 || checked >= 3) {
            level = { label: "High · refer now", cls: "crit", show: true };
        } else if (checked >= 1) {
            level = { label: "Moderate · keep watch", cls: "amber", show: true };
        } else {
            level = { label: "Awaiting input", cls: "muted", show: false };
        }
        valueEl.textContent = level.label;
        valueEl.style.color = level.cls === "crit" ? "var(--crit-red)" :
            level.cls === "amber" ? "var(--warn-amber)" : "var(--ink-300)";
        const dot = document.querySelector(".triage__severity .severity-pulse");
        if (dot) {
            dot.style.background = level.cls === "crit" ? "var(--crit-red)" :
                level.cls === "amber" ? "var(--warn-amber)" : "var(--ink-300)";
            dot.style.boxShadow = level.cls === "crit"
                ? "0 0 0 4px rgba(200, 30, 30, 0.18)"
                : level.cls === "amber"
                ? "0 0 0 4px rgba(183, 121, 31, 0.18)"
                : "0 0 0 4px rgba(154, 168, 165, 0.18)";
        }
    }

    // Initial state
    switchTo("screen-welcome");
    updateTriageSeverity();
})();
