package com.afyaquest.app.presentation.learn

import com.afyaquest.app.presentation.lessons.LessonsViewModel
import com.afyaquest.app.presentation.navigation.Screen
import com.afyaquest.app.presentation.videomodules.VideoModulesViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The focus topics and the featured row are hand-written lists of ids. These tests keep them
 * honest: an id that does not exist would silently drop a video or lesson from the Learn hub.
 */
class LearnTopicTest {

    private val allVideoIds = VideoModulesViewModel.allVideos().map { it.id }

    // ---- video catalogue the topics are built on ----

    @Test
    fun `video ids are unique`() {
        assertEquals(allVideoIds.size, allVideoIds.toSet().size)
    }

    @Test
    fun `every video has a stream url and belongs to one of the modules`() {
        VideoModulesViewModel.allVideos().forEach { video ->
            assertTrue("${video.id} has no https url", video.videoUrl.orEmpty().startsWith("https://"))
            assertTrue(
                "${video.id} is in module ${video.moduleNumber}",
                video.moduleNumber in 1..VideoModulesViewModel.MODULE_COUNT
            )
        }
    }

    // ---- topics ----

    @Test
    fun `topic ids are unique and safe to put in a route`() {
        val ids = LearnTopic.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
        ids.forEach { assertTrue("'$it' is not route safe", Regex("[a-z0-9-]+").matches(it)) }
    }

    @Test
    fun `every topic has content and only points at videos that exist`() {
        LearnTopic.entries.forEach { topic ->
            assertTrue("${topic.id} has no videos", topic.videoIds.isNotEmpty())
            assertEquals("${topic.id} lists a video twice", topic.videoIds.size, topic.videoIds.toSet().size)
            topic.videoIds.forEach { id ->
                assertTrue("${topic.id} points at unknown video $id", id in allVideoIds)
            }
        }
    }

    @Test
    fun `every topic only points at lessons that exist`() {
        LearnTopic.entries.forEach { topic ->
            assertEquals("${topic.id} lists a lesson twice", topic.lessonIds.size, topic.lessonIds.toSet().size)
            topic.lessonIds.forEach { id ->
                assertTrue("${topic.id} points at unknown lesson $id", id in LessonsViewModel.LESSON_IDS)
            }
        }
    }

    @Test
    fun `videosForTopic keeps the topic's own order`() {
        LearnTopic.entries.forEach { topic ->
            assertEquals(topic.videoIds, VideoModulesViewModel.videosForTopic(topic).map { it.id })
        }
    }

    @Test
    fun `maternal health starts with the female reproductive system and covers module 4`() {
        val topic = LearnTopic.MATERNAL_HEALTH
        assertEquals("mod1-female-reproductive", topic.videoIds.first())
        val module4 = VideoModulesViewModel.allVideos().filter { it.moduleNumber == 4 }.map { it.id }
        assertTrue(topic.videoIds.containsAll(module4))
        assertFalse("mod1-male-reproductive" in topic.videoIds)
    }

    @Test
    fun `nutrition leads with malnutrition and gathers the feeding content of other modules`() {
        val topic = LearnTopic.NUTRITION
        assertEquals("mod2-malnutrition", topic.videoIds.first())
        // Continued feeding in diarrhoea, newborn feeding, iron and folic acid in pregnancy.
        assertTrue(topic.videoIds.containsAll(listOf("mod2-diarrhea", "mod4-safe-delivery", "mod4-antenatal-care")))
        // The balanced-diet lesson plus the four nutrition lessons.
        assertEquals(setOf("lesson-2", "lesson-7", "lesson-8", "lesson-9", "lesson-10"), topic.lessonIds.toSet())
    }

    @Test
    fun `fromId and forVideo resolve topics`() {
        assertEquals(LearnTopic.MATERNAL_HEALTH, LearnTopic.fromId("maternal-health"))
        assertEquals(LearnTopic.NUTRITION, LearnTopic.fromId("nutrition"))
        assertNull(LearnTopic.fromId("no-such-topic"))
        assertNull(LearnTopic.fromId(null))

        // A video can be in two topics; the first topic that lists it is its home.
        assertEquals(LearnTopic.MATERNAL_HEALTH, LearnTopic.forVideo("mod4-antenatal-care"))
        assertEquals(LearnTopic.NUTRITION, LearnTopic.forVideo("mod2-malnutrition"))
        assertNull(LearnTopic.forVideo("mod1-nervous-system"))
    }

    // ---- featured row ----

    @Test
    fun `featured videos exist, are unique, and lead with the female reproductive system`() {
        val featured = FeaturedLearning.videoIds
        assertTrue(featured.isNotEmpty())
        assertEquals(featured.size, featured.toSet().size)
        featured.forEach { assertTrue("unknown featured video $it", it in allVideoIds) }
        assertEquals("mod1-female-reproductive", featured.first())
        assertFalse("mod1-male-reproductive" in featured)
        assertEquals(featured, VideoModulesViewModel.featuredVideos().map { it.id })
    }

    @Test
    fun `every featured video belongs to a topic and has a bundled thumbnail`() {
        FeaturedLearning.videoIds.forEach { id ->
            assertNotNull("$id is in no topic", LearnTopic.forVideo(id))
            assertNotNull("$id has no thumbnail", videoThumbnailRes(id))
        }
        assertNull(videoThumbnailRes("mod1-nervous-system"))
    }

    @Test
    fun `home features the first unwatched featured video, then falls back to the lead one`() {
        val (first, second, third) = FeaturedLearning.videoIds
        assertEquals(first, VideoModulesViewModel.featuredVideoFor(emptySet())?.id)
        assertEquals(second, VideoModulesViewModel.featuredVideoFor(setOf(first))?.id)
        assertEquals(third, VideoModulesViewModel.featuredVideoFor(setOf(first, second))?.id)
        assertEquals(first, VideoModulesViewModel.featuredVideoFor(setOf(first, second, third))?.id)
        // Watching unrelated videos does not move the spotlight.
        assertEquals(first, VideoModulesViewModel.featuredVideoFor(setOf("mod1-nervous-system"))?.id)
    }

    // ---- "next video" ordering shared by modules and topics ----

    @Test
    fun `nextUnwatched prefers the video after the current one`() {
        val videos = VideoModulesViewModel.videosForTopic(LearnTopic.MATERNAL_HEALTH)
        val next = VideoModulesViewModel.nextUnwatched(videos, emptySet(), "mod1-female-reproductive")
        assertEquals("mod4-antenatal-care", next?.id)
    }

    @Test
    fun `nextUnwatched skips watched videos and wraps around to earlier ones`() {
        val videos = VideoModulesViewModel.videosForTopic(LearnTopic.MATERNAL_HEALTH)
        val ids = videos.map { it.id }

        val skipping = VideoModulesViewModel.nextUnwatched(videos, setOf(ids[1], ids[2]), ids[0])
        assertEquals(ids[3], skipping?.id)

        // Last video just finished: everything after it is exhausted, so go back to the first open one.
        val wrapped = VideoModulesViewModel.nextUnwatched(videos, setOf(ids[0]), ids.last())
        assertEquals(ids[1], wrapped?.id)
    }

    @Test
    fun `nextUnwatched never returns the current video and is null when nothing is left`() {
        // A list with a single video: there is never a "next" one.
        val videos = VideoModulesViewModel.videosForTopic(LearnTopic.NUTRITION).take(1)
        val only = videos.single().id
        assertNull(VideoModulesViewModel.nextUnwatched(videos, emptySet(), only))
        assertNull(VideoModulesViewModel.nextUnwatched(videos, setOf(only), null))
        assertNull(VideoModulesViewModel.nextUnwatched(emptyList(), emptySet(), null))
    }

    @Test
    fun `nextUnwatched without a current video returns the first unwatched one`() {
        val videos = VideoModulesViewModel.videosForTopic(LearnTopic.MATERNAL_HEALTH)
        val ids = videos.map { it.id }
        assertEquals(ids[0], VideoModulesViewModel.nextUnwatched(videos, emptySet(), null)?.id)
        assertEquals(ids[1], VideoModulesViewModel.nextUnwatched(videos, setOf(ids[0]), null)?.id)
        // An id that is not in the list behaves like "no current video".
        assertEquals(ids[0], VideoModulesViewModel.nextUnwatched(videos, emptySet(), "mod1-nervous-system")?.id)
    }

    // ---- routes ----

    @Test
    fun `player route carries the topic only when there is one`() {
        assertEquals("video_player/mod4-pregnancy", Screen.VideoPlayer.createRoute("mod4-pregnancy"))
        assertEquals("video_player/mod4-pregnancy", Screen.VideoPlayer.createRoute("mod4-pregnancy", null))
        assertEquals("video_player/mod4-pregnancy", Screen.VideoPlayer.createRoute("mod4-pregnancy", ""))
        assertEquals(
            "video_player/mod4-pregnancy?topic=maternal-health",
            Screen.VideoPlayer.createRoute("mod4-pregnancy", LearnTopic.MATERNAL_HEALTH.id)
        )
        assertEquals("learn_topic/nutrition", Screen.Topic.createRoute(LearnTopic.NUTRITION.id))
    }
}
