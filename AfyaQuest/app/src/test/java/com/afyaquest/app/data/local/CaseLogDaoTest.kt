package com.afyaquest.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.afyaquest.app.data.local.dao.CaseLogDao
import com.afyaquest.app.data.local.entity.CaseLogEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Date

/**
 * Verifies the database assembles at the current version (Room builds `case_logs`
 * from [CaseLogEntity]) and that [CaseLogDao] round-trips correctly.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class CaseLogDaoTest {

    private lateinit var db: AfyaQuestDatabase
    private lateinit var dao: CaseLogDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AfyaQuestDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.caseLogDao()
    }

    @After
    fun teardown() = db.close()

    private fun sample(id: String, userId: String = "u1", synced: Boolean = false) = CaseLogEntity(
        id = id,
        userId = userId,
        treeVersion = "1.0.0-draft",
        language = "en",
        mechanism = "trauma",
        dispositionId = "disp_priority",
        dispositionLevel = "priority",
        flagsJson = "{}",
        pathJson = "[]",
        completedAt = Date(1_000L),
        isSynced = synced
    )

    @Test
    fun insert_and_query_by_user() = runBlocking {
        dao.insert(sample("c1"))
        dao.insert(sample("c2", userId = "u2"))

        val u1 = dao.getByUser("u1")
        assertEquals(1, u1.size)
        assertEquals("disp_priority", u1[0].dispositionId)
        assertEquals("trauma", u1[0].mechanism)
    }

    @Test
    fun replace_on_same_id_does_not_duplicate() = runBlocking {
        dao.insert(sample("session-1"))
        dao.insert(sample("session-1")) // same session re-logged after stepping back
        assertEquals(1, dao.getByUser("u1").size)
    }

    @Test
    fun unsynced_filtering_and_mark_synced() = runBlocking {
        dao.insert(sample("c1", synced = false))
        dao.insert(sample("c2", synced = true))
        assertEquals(1, dao.getUnsynced().size)
        assertEquals(1, dao.unsyncedCount().first())

        dao.markSynced("c1")
        assertEquals(0, dao.getUnsynced().size)
        assertEquals(0, dao.unsyncedCount().first())
    }

    @Test
    fun observe_emits_for_user_ordered_newest_first() = runBlocking {
        dao.insert(sample("older").copy(completedAt = Date(1_000L)))
        dao.insert(sample("newer").copy(completedAt = Date(2_000L)))
        val list = dao.observeByUser("u1").first()
        assertEquals(listOf("newer", "older"), list.map { it.id })
    }
}
