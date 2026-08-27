package com.afyaquest.app.domain.triage

/**
 * Immutable snapshot of a triage traversal. The ViewModel holds the current
 * [TriageState] (in SavedStateHandle, so a mid-flow language switch or process
 * death does not reset progress) and produces a new one on every answer.
 */
data class TriageState(
    /** Id of the node currently shown, or the disposition reached. */
    val currentId: String,
    /** Accumulated state flags from every answer so far. */
    val flags: Map<String, String>,
    /** Ordered audit trail of nodes answered and the option chosen at each. */
    val path: List<PathStep>,
    /** True when [currentId] is a disposition (the assessment has ended). */
    val isDisposition: Boolean
)

data class PathStep(val nodeId: String, val optionLetter: String)

/**
 * Generic, side-effect-free walker over a [TriageTree]. Pure Kotlin (no Android
 * dependencies) so it is fully unit-testable on the JVM.
 *
 * Auto (computed) nodes — e.g. mechanism routing and severity evaluation — are
 * resolved internally and never surfaced to the UI: [start] and [answer] always
 * return a state pointing at a question node or a disposition.
 */
class TriageEngine(val tree: TriageTree) {

    private val nodesById: Map<String, TriageNode> = tree.nodes.associateBy { it.id }
    private val dispositionsById: Map<String, Disposition> = tree.dispositions.associateBy { it.id }

    init {
        require(tree.startNodeId.isNotBlank()) { "triage tree has no startNodeId" }
        require(nodesById.containsKey(tree.startNodeId)) {
            "startNodeId '${tree.startNodeId}' is not a known node"
        }
    }

    fun node(id: String): TriageNode? = nodesById[id]
    fun disposition(id: String): Disposition? = dispositionsById[id]
    fun isDisposition(id: String): Boolean = dispositionsById.containsKey(id)
    fun allNodeIds(): Set<String> = nodesById.keys
    fun allDispositionIds(): Set<String> = dispositionsById.keys

    /** Convenience accessors for the currently active node/disposition. */
    fun currentNode(state: TriageState): TriageNode? = node(state.currentId)
    fun currentDisposition(state: TriageState): Disposition? = disposition(state.currentId)

    /** Begin a new assessment at the start node, resolving any leading auto nodes. */
    fun start(): TriageState {
        val resolved = resolve(tree.startNodeId, emptyMap())
        return TriageState(resolved, emptyMap(), emptyList(), isDisposition(resolved))
    }

    /** Apply the chosen option [letter] on the current question node and advance. */
    fun answer(state: TriageState, letter: String): TriageState {
        check(!state.isDisposition) {
            "cannot answer from disposition '${state.currentId}'"
        }
        val current = node(state.currentId)
            ?: error("current node '${state.currentId}' not found")
        check(current.kind == TriageKind.QUESTION) {
            "node '${current.id}' is kind '${current.kind}', not a question"
        }
        val option = current.options?.firstOrNull { it.letter == letter }
            ?: error("option '$letter' not found on node '${current.id}'")
        val newFlags = state.flags + (option.setFlags ?: emptyMap())
        val resolved = resolve(option.next, newFlags)
        return TriageState(
            currentId = resolved,
            flags = newFlags,
            path = state.path + PathStep(state.currentId, letter),
            isDisposition = isDisposition(resolved)
        )
    }

    /**
     * Follow auto (computed) nodes from [startId] until a question node or a
     * disposition is reached, returning that terminal id. Guards against routing
     * cycles via [MAX_HOPS].
     */
    fun resolve(startId: String, flags: Map<String, String>): String {
        var currentId = startId
        repeat(MAX_HOPS) {
            if (isDisposition(currentId)) return currentId
            val n = nodesById[currentId] ?: error("node '$currentId' not found")
            if (n.kind != TriageKind.AUTO) return currentId
            currentId = route(n, flags)
        }
        error("auto-routing exceeded $MAX_HOPS hops from '$startId' — possible cycle")
    }

    private fun route(node: TriageNode, flags: Map<String, String>): String {
        node.routing?.forEach { rule ->
            if (matches(rule.condition, flags)) return rule.next
        }
        return node.default
            ?: error("auto node '${node.id}' matched no rule and has no default")
    }

    private fun matches(condition: Map<String, List<String>>?, flags: Map<String, String>): Boolean {
        if (condition.isNullOrEmpty()) return true
        return condition.all { (key, allowed) -> flags[key] in allowed }
    }

    /**
     * 1-based index of the current phase within [TriageTree.phaseOrder], for the
     * progress bar. Dispositions map to the final phase.
     */
    fun progressIndex(state: TriageState): Int {
        val order = tree.phaseOrder ?: return 1
        if (state.isDisposition) return order.size
        val phase = node(state.currentId)?.phase ?: return 1
        val idx = order.indexOf(phase)
        return if (idx >= 0) idx + 1 else 1
    }

    fun phaseCount(): Int = tree.phaseOrder?.size ?: 1

    private companion object {
        const val MAX_HOPS = 64
    }
}
