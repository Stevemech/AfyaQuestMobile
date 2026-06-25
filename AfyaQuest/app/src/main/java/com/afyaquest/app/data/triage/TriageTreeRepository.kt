package com.afyaquest.app.data.triage

import android.content.Context
import com.afyaquest.app.domain.triage.TriageEngine
import com.afyaquest.app.domain.triage.TriageTree
import com.afyaquest.app.domain.triage.withCakOverlay
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Loads the bundled emergency triage decision tree from app assets and exposes a
 * (cached) [TriageEngine] over it. The tree is data, not code — see
 * `assets/triage/triage_tree.json` and `docs/emergency-response/`.
 */
@Singleton
class TriageTreeRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val gson = Gson()

    @Volatile private var cachedTree: TriageTree? = null
    @Volatile private var cachedEngine: TriageEngine? = null

    fun loadTree(): TriageTree =
        cachedTree ?: synchronized(this) {
            cachedTree ?: run {
                val json = context.assets.open(ASSET_PATH).bufferedReader().use { it.readText() }
                val tree = gson.fromJson(json, TriageTree::class.java)
                tree.withCakOverlay(loadCakOverlay()).also { cachedTree = it }
            }
        }

    /** Draft Kaqchikel overlay (audioKey -> text). Empty/missing is fine. */
    private fun loadCakOverlay(): Map<String, String> =
        try {
            val json = context.assets.open(CAK_OVERLAY_PATH).bufferedReader().use { it.readText() }
            val type = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson<Map<String, String>>(json, type) ?: emptyMap()
        } catch (e: Exception) {
            emptyMap()
        }

    fun engine(): TriageEngine =
        cachedEngine ?: synchronized(this) {
            cachedEngine ?: TriageEngine(loadTree()).also { cachedEngine = it }
        }

    private companion object {
        const val ASSET_PATH = "triage/triage_tree.json"
        const val CAK_OVERLAY_PATH = "triage/triage_tree.cak.draft.json"
    }
}
