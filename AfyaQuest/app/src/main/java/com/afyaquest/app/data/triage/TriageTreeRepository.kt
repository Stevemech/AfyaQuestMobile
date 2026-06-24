package com.afyaquest.app.data.triage

import android.content.Context
import com.afyaquest.app.domain.triage.TriageEngine
import com.afyaquest.app.domain.triage.TriageTree
import com.google.gson.Gson
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
                gson.fromJson(json, TriageTree::class.java).also { cachedTree = it }
            }
        }

    fun engine(): TriageEngine =
        cachedEngine ?: synchronized(this) {
            cachedEngine ?: TriageEngine(loadTree()).also { cachedEngine = it }
        }

    private companion object {
        const val ASSET_PATH = "triage/triage_tree.json"
    }
}
