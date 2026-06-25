package com.afyaquest.app.domain.triage

import com.google.gson.annotations.SerializedName

/**
 * Data-driven model for the Emergency Response Guide triage decision tree.
 *
 * The tree is authored as JSON (see `assets/triage/triage_tree.json`) so that a
 * clinician can revise wording, ordering, branching, and severity logic WITHOUT
 * code changes. [TriageEngine] is a generic walker over this model.
 *
 * NOTE: the bundled tree is a DRAFT and is not validated for clinical use
 * ([TriageTree.validated] == false). It must be signed off by a licensed
 * EMT/paramedic and a supervising physician before any field deployment.
 */

/** Localized strings keyed by language code (e.g. "en", "es", "cak"). */
typealias LocalizedText = Map<String, String>

data class TriageTree(
    val version: String = "",
    val validated: Boolean = false,
    val disclaimer: LocalizedText? = null,
    val startNodeId: String = "",
    val languages: List<String>? = null,
    /** Ordered phase keys used to drive the Scene -> Primary -> Secondary -> Disposition progress bar. */
    val phaseOrder: List<String>? = null,
    val nodes: List<TriageNode> = emptyList(),
    val dispositions: List<Disposition> = emptyList()
)

data class TriageNode(
    val id: String = "",
    val phase: String = "",
    /** [TriageKind.QUESTION] (shown to the user) or [TriageKind.AUTO] (computed routing, never shown). */
    val kind: String = TriageKind.QUESTION,
    val icon: String? = null,
    /** Key joining this node to a pre-recorded audio clip (voice layer, future milestone). */
    val audioKey: String? = null,
    val text: LocalizedText? = null,
    /** Present on question nodes. */
    val options: List<TriageOption>? = null,
    /** Present on auto nodes — evaluated in order, first match wins. */
    val routing: List<RoutingRule>? = null,
    /** Fallback target for an auto node when no routing rule matches. */
    val default: String? = null
)

data class TriageOption(
    val letter: String = "",
    val label: LocalizedText? = null,
    val audioKey: String? = null,
    /** State flags this answer sets/accumulates (e.g. {"mechanism":"trauma"}). */
    val setFlags: Map<String, String>? = null,
    /** Id of the next node OR a disposition id. */
    val next: String = ""
)

data class RoutingRule(
    /** Flag conditions; all keys must match (value present in the allowed list). Empty/absent = always matches. */
    @SerializedName("if") val condition: Map<String, List<String>>? = null,
    /** Id of the next node OR a disposition id when this rule matches. */
    val next: String = ""
)

data class Disposition(
    val id: String = "",
    /** [DispositionLevel] value. */
    val level: String = "",
    /** Severity color token: "crit" | "warn" | "ok". */
    val color: String = "",
    val pulse: Boolean = false,
    val audioKey: String? = null,
    val label: LocalizedText? = null,
    val instructions: LocalizedText? = null,
    val escalation: Escalation? = null
)

data class Escalation(
    /** [EscalationTier] value. */
    val tier: String = EscalationTier.NONE,
    /** "call_ems" | "call_facility" | "call_backup" | "monitor". */
    val action: String = ""
)

object TriageKind {
    const val QUESTION = "question"
    const val AUTO = "auto"
}

object DispositionLevel {
    const val EARLY_EXIT = "early_exit"
    const val IMMEDIATE = "immediate"
    const val PRIORITY = "priority"
    const val STABLE = "stable"
}

object EscalationTier {
    const val IMMEDIATE = "immediate"
    const val PRIORITY = "priority"
    const val NONE = "none"
}

/**
 * Resolve localized text with graceful fallback: requested language, then
 * English, then any available value. Returns "" if nothing is available.
 *
 * The offered languages are English and Kaqchikel (`cak`). Until the Kaqchikel
 * triage content is authored by a native speaker, `cak` falls back to English.
 */
fun LocalizedText?.localized(lang: String): String {
    if (this.isNullOrEmpty()) return ""
    return this[lang] ?: this["en"] ?: values.first()
}
