package com.afyaquest.app.domain.triage

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Validates the bundled `triage_tree.json` AND the [TriageEngine] traversal logic.
 *
 * The structural tests are the safety net for clinician edits to the JSON: if a
 * reviewer changes wording/branching and accidentally points an option at a
 * missing id, leaves a node unreachable, or creates a path that never reaches a
 * disposition, these tests fail before the change ships.
 */
class TriageEngineTest {

    private lateinit var tree: TriageTree
    private lateinit var engine: TriageEngine

    @Before
    fun setUp() {
        tree = loadTree()
        engine = TriageEngine(tree)
    }

    // --- Structural integrity of the authored tree ----------------------------

    @Test
    fun tree_parses_with_expected_shape() {
        assertEquals("scene_safe", tree.startNodeId)
        assertFalse("bundled tree must be flagged as an unvalidated draft", tree.validated)
        assertTrue("expected several question nodes", tree.nodes.size >= 20)
        assertEquals(5, tree.dispositions.size)
        assertEquals(listOf("scene_size_up", "primary_survey", "secondary_survey", "disposition"), tree.phaseOrder)
    }

    @Test
    fun every_target_id_resolves_to_a_node_or_disposition() {
        val known = engine.allNodeIds() + engine.allDispositionIds()
        for (node in tree.nodes) {
            node.options?.forEach { opt ->
                assertTrue(
                    "node '${node.id}' option '${opt.letter}' points at unknown id '${opt.next}'",
                    opt.next in known
                )
            }
            node.routing?.forEach { rule ->
                assertTrue(
                    "node '${node.id}' routing points at unknown id '${rule.next}'",
                    rule.next in known
                )
            }
            node.default?.let {
                assertTrue("node '${node.id}' default points at unknown id '$it'", it in known)
            }
        }
    }

    @Test
    fun question_nodes_have_options_and_auto_nodes_have_routing() {
        for (node in tree.nodes) {
            when (node.kind) {
                TriageKind.QUESTION -> {
                    assertTrue("question '${node.id}' needs >=2 options", (node.options?.size ?: 0) >= 2)
                    assertNotNull("question '${node.id}' needs text", node.text)
                    node.options!!.forEach {
                        assertTrue("option on '${node.id}' missing letter", it.letter.isNotBlank())
                        assertNotNull("option '${it.letter}' on '${node.id}' missing label", it.label)
                    }
                }
                TriageKind.AUTO -> {
                    assertTrue("auto '${node.id}' needs routing", !node.routing.isNullOrEmpty())
                    assertNotNull("auto '${node.id}' needs a default", node.default)
                }
                else -> error("node '${node.id}' has unknown kind '${node.kind}'")
            }
        }
    }

    @Test
    fun all_nodes_and_dispositions_are_reachable_from_start() {
        val reachedNodes = mutableSetOf<String>()
        val reachedDisps = mutableSetOf<String>()
        val seen = mutableSetOf(tree.startNodeId)
        val queue = ArrayDeque<String>().apply { add(tree.startNodeId) }

        while (queue.isNotEmpty()) {
            val id = queue.removeFirst()
            if (engine.isDisposition(id)) { reachedDisps += id; continue }
            val n = engine.node(id) ?: error("unknown node '$id'")
            reachedNodes += id
            val targets = buildList {
                n.options?.forEach { add(it.next) }
                n.routing?.forEach { add(it.next) }
                n.default?.let { add(it) }
            }
            targets.forEach { if (seen.add(it)) queue.add(it) }
        }

        assertEquals(
            "unreachable nodes: ${engine.allNodeIds() - reachedNodes}",
            engine.allNodeIds(), reachedNodes
        )
        assertEquals(
            "unreachable dispositions: ${engine.allDispositionIds() - reachedDisps}",
            engine.allDispositionIds(), reachedDisps
        )
    }

    @Test
    fun every_path_terminates_in_a_disposition_and_never_surfaces_auto_nodes() {
        var leafCount = 0
        fun walk(state: TriageState, depth: Int) {
            check(depth < 200) { "path too deep at '${state.currentId}' — possible cycle" }
            if (state.isDisposition) {
                assertTrue(engine.isDisposition(state.currentId)); leafCount++; return
            }
            val n = engine.node(state.currentId)
                ?: error("surfaced unknown node '${state.currentId}'")
            assertEquals("auto node '${n.id}' must never be surfaced to the user", TriageKind.QUESTION, n.kind)
            n.options!!.forEach { walk(engine.answer(state, it.letter), depth + 1) }
        }
        walk(engine.start(), 0)
        assertTrue("expected many terminal paths", leafCount > 20)
    }

    // --- Behavioral / safety-critical paths -----------------------------------

    @Test
    fun unsafe_scene_is_an_immediate_early_exit() {
        val s = engine.answer(engine.start(), "B") // scene_safe -> No, danger
        assertTrue(s.isDisposition)
        assertEquals("disp_unsafe", s.currentId)
        assertEquals(DispositionLevel.EARLY_EXIT, engine.disposition(s.currentId)?.level)
    }

    @Test
    fun no_breathing_routes_straight_to_cpr() {
        var s = engine.start()
        s = engine.answer(s, "A") // scene_safe: safe
        s = engine.answer(s, "A") // scene_ppe: gloves
        s = engine.answer(s, "A") // scene_mechanism: trauma
        s = engine.answer(s, "A") // prim_responsive: reacts
        s = engine.answer(s, "A") // prim_airway_aware: speaks clearly
        assertEquals("prim_breathing", s.currentId)
        s = engine.answer(s, "C") // breathing absent
        assertEquals("disp_cpr", s.currentId)
        assertEquals(DispositionLevel.IMMEDIATE, engine.disposition(s.currentId)?.level)
        assertEquals(EscalationTier.IMMEDIATE, engine.disposition(s.currentId)?.escalation?.tier)
    }

    @Test
    fun avpu_unresponsive_is_immediate() {
        var s = engine.start()
        s = engine.answer(s, "A") // safe
        s = engine.answer(s, "A") // gloves
        s = engine.answer(s, "B") // medical
        s = engine.answer(s, "A") // responsive
        s = engine.answer(s, "A") // airway clear
        s = engine.answer(s, "A") // breathing normal
        s = engine.answer(s, "A") // no heavy bleeding
        assertEquals("prim_disability", s.currentId)
        s = engine.answer(s, "D") // no reaction at all
        assertEquals("disp_immediate", s.currentId)
    }

    @Test
    fun unknown_mechanism_routes_down_the_trauma_path() {
        var s = engine.start()
        s = engine.answer(s, "A") // safe
        s = engine.answer(s, "A") // gloves
        s = engine.answer(s, "C") // mechanism unknown
        s = engine.answer(s, "A") // responsive
        s = engine.answer(s, "A") // airway clear
        s = engine.answer(s, "A") // breathing normal
        s = engine.answer(s, "A") // no bleeding
        s = engine.answer(s, "A") // alert
        // sec_router (auto) must resolve straight to the trauma secondary survey
        assertEquals("sec_t_head", s.currentId)
        assertEquals("unknown", s.flags["mechanism"])
    }

    @Test
    fun clean_trauma_survey_computes_stable_but_any_injury_computes_priority() {
        // All-clear trauma secondary -> STABLE (severity computed, not asked)
        val stable = runTrauma(headInjury = false)
        assertEquals("disp_stable", stable.currentId)
        assertEquals(DispositionLevel.STABLE, engine.disposition(stable.currentId)?.level)

        // A single head/neck finding -> PRIORITY, decided by the auto eval node
        val priority = runTrauma(headInjury = true)
        assertEquals("disp_priority", priority.currentId)
        assertEquals(DispositionLevel.PRIORITY, engine.disposition(priority.currentId)?.level)
    }

    @Test
    fun medical_gradual_mild_is_stable_while_stroke_signs_are_priority() {
        val stable = runMedical(complaint = "C") // bad pain/fever, normal vitals, gradual
        assertEquals("disp_stable", stable.currentId)

        val priority = runMedical(complaint = "B") // stroke signs
        assertEquals("disp_priority", priority.currentId)
    }

    @Test
    fun no_pulse_in_medical_path_routes_to_cpr() {
        var s = driveToMedicalSecondary()
        s = engine.answer(s, "A") // no known illness
        s = engine.answer(s, "B") // gradual
        s = engine.answer(s, "C") // pain/fever
        s = engine.answer(s, "A") // breathing normal
        assertEquals("sec_m_vitals_pulse", s.currentId)
        s = engine.answer(s, "C") // cannot feel pulse
        assertEquals("disp_cpr", s.currentId)
    }

    // --- Engine utilities ------------------------------------------------------

    @Test
    fun progress_index_advances_by_phase() {
        val start = engine.start()
        assertEquals(1, engine.progressIndex(start)) // scene_size_up
        var s = engine.answer(start, "A")
        s = engine.answer(s, "A")
        s = engine.answer(s, "A") // now in primary_survey
        assertEquals(2, engine.progressIndex(s))
        val disp = engine.answer(engine.start(), "B") // disp_unsafe
        assertEquals(engine.phaseCount(), engine.progressIndex(disp))
    }

    @Test
    fun localized_falls_back_to_english_when_language_missing() {
        val text = mapOf("en" to "Hi", "es" to "Hola")
        assertEquals("Hi", text.localized("en"))
        assertEquals("Hola", text.localized("es"))
        // Kaqchikel content not yet authored -> falls back to English (Spanish no longer offered).
        assertEquals("Hi", text.localized("cak"))
        assertEquals("", (null as LocalizedText?).localized("en"))
    }

    // --- helpers ---------------------------------------------------------------

    private fun runTrauma(headInjury: Boolean): TriageState {
        var s = engine.start()
        s = engine.answer(s, "A") // safe
        s = engine.answer(s, "A") // gloves
        s = engine.answer(s, "A") // trauma
        s = engine.answer(s, "A") // responsive
        s = engine.answer(s, "A") // airway clear
        s = engine.answer(s, "A") // breathing normal
        s = engine.answer(s, "A") // no bleeding
        s = engine.answer(s, "A") // alert -> sec_t_head
        s = engine.answer(s, if (headInjury) "B" else "A") // head/neck
        s = engine.answer(s, "A") // chest ok
        s = engine.answer(s, "A") // abdomen ok
        s = engine.answer(s, "A") // extremities ok
        s = engine.answer(s, "A") // spine: feels and moves -> sec_eval_trauma (auto)
        return s
    }

    private fun driveToMedicalSecondary(): TriageState {
        var s = engine.start()
        s = engine.answer(s, "A") // safe
        s = engine.answer(s, "A") // gloves
        s = engine.answer(s, "B") // medical
        s = engine.answer(s, "A") // responsive
        s = engine.answer(s, "A") // airway clear
        s = engine.answer(s, "A") // breathing normal
        s = engine.answer(s, "A") // no bleeding
        s = engine.answer(s, "A") // alert -> sec_m_conditions
        return s
    }

    private fun runMedical(complaint: String): TriageState {
        var s = driveToMedicalSecondary()
        s = engine.answer(s, "A") // no known illness
        s = engine.answer(s, "B") // gradual
        s = engine.answer(s, complaint)
        s = engine.answer(s, "A") // breathing normal
        s = engine.answer(s, "A") // pulse normal -> sec_eval_medical (auto)
        return s
    }

    private fun loadTree(): TriageTree {
        val candidates = listOf(
            "src/main/assets/triage/triage_tree.json",
            "app/src/main/assets/triage/triage_tree.json",
            "AfyaQuest/app/src/main/assets/triage/triage_tree.json"
        )
        val file = candidates.map { File(it) }.firstOrNull { it.exists() }
            ?: error("triage_tree.json not found from cwd=${File(".").absolutePath}")
        return Gson().fromJson(file.readText(), TriageTree::class.java)
    }
}
