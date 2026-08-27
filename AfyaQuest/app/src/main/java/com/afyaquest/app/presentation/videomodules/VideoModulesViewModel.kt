package com.afyaquest.app.presentation.videomodules

import android.content.Context
import android.util.Log
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.afyaquest.app.R
import com.afyaquest.app.data.remote.ApiService
import com.afyaquest.app.domain.model.VideoCategory
import com.afyaquest.app.domain.model.VideoModule
import com.afyaquest.app.domain.model.VideoModuleFolder
import com.afyaquest.app.util.ProgressDataStore
import com.afyaquest.app.util.TokenManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VideoModulesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val progressDataStore: ProgressDataStore,
    private val apiService: ApiService,
    private val tokenManager: TokenManager
) : ViewModel() {

    companion object {
        private const val S3_BASE = "https://afyaquest-media.s3.amazonaws.com/videos"

        const val MODULE_COUNT = 6

        /**
         * Module titles / descriptions live in string resources (learn_module_N_title / _desc).
         * Composables must resolve them with stringResource() so the per-app language (applied in
         * MainActivity.attachBaseContext) is respected; [moduleTitleRes] / [moduleDescriptionRes]
         * return the resource id for a module number.
         */
        @StringRes
        fun moduleTitleRes(moduleNumber: Int): Int = when (moduleNumber) {
            1 -> R.string.learn_module_1_title
            2 -> R.string.learn_module_2_title
            3 -> R.string.learn_module_3_title
            4 -> R.string.learn_module_4_title
            5 -> R.string.learn_module_5_title
            else -> R.string.learn_module_6_title
        }

        @StringRes
        fun moduleDescriptionRes(moduleNumber: Int): Int = when (moduleNumber) {
            1 -> R.string.learn_module_1_desc
            2 -> R.string.learn_module_2_desc
            3 -> R.string.learn_module_3_desc
            4 -> R.string.learn_module_4_desc
            5 -> R.string.learn_module_5_desc
            else -> R.string.learn_module_6_desc
        }

        val MODULE_ICONS = mapOf(
            1 to "\uD83E\uDEC0", 2 to "\uD83E\uDE7A", 3 to "\uD83E\uDDA0",
            4 to "\uD83E\uDD30", 5 to "\u26D1\uFE0F", 6 to "\uD83D\uDEE1\uFE0F"
        )

        fun allVideoUrls(): Map<String, String> {
            val map = mutableMapOf<String, String>()
            allVideos().forEach { v -> v.videoUrl?.let { map[v.id] = it } }
            return map
        }

        fun allVideos(): List<VideoModule> = listOf(
            // Module 1: Body Systems
            vm(1, "mod1-nervous-system", "Nervous System", "The brain, spinal cord, and nerves", "module-1/Nervous%20System.mp4"),
            vm(1, "mod1-musculoskeletal", "Musculoskeletal System", "Bones, muscles, and joints", "module-1/Muscukoskeletal%20Systme%20Final%20Video.mov"),
            vm(1, "mod1-lymphatic", "Lymphatic System", "Infection defense and fluid drainage", "module-1/Lymphatic%20System%20Final%20Video.mov"),
            vm(1, "mod1-endocrine", "The Endocrine System", "Hormones and glands", "module-1/The%20Endocrine%20System.mp4"),
            vm(1, "mod1-integumentary", "The Integumentary System", "Skin, hair, and nails", "module-1/The%20Integumentary%20System.mp4"),
            vm(1, "mod1-urinary", "Urinary System", "Kidneys and urinary health", "module-1/Urinary%20System.mov"),
            vm(1, "mod1-male-reproductive", "Male Reproductive System", "Male reproductive anatomy and health", "module-1/Male%20Reproductive%20System.mp4"),
            vm(1, "mod1-female-reproductive", "Female Reproductive System", "Female reproductive anatomy and health", "module-1/Female%20Reproductive%20System.mp4"),

            // Module 2: Common Childhood Illnesses
            vm(2, "mod2-warning-signs", "Warning Signs", "Danger signs and fever in children", "module-2/Warning%20Signs.mp4"),
            vm(2, "mod2-diarrhea", "Diarrhea", "Identification, dangers, and treatment", "module-2/diarrhea.mp4"),
            vm(2, "mod2-respiratory-infections", "Respiratory Infections", "Breathing danger signs", "module-2/Respiratory%20Infections%20Video.mov"),
            vm(2, "mod2-asthma-pneumonia-tb", "Asthma, Pneumonia & TB", "Respiratory diseases in children", "module-2/Asthma%2C%20Pneumonia%2C%20and%20Tuburculosis.mp4"),
            vm(2, "mod2-antibiotics", "The Rules of Antibiotics", "Proper antibiotic use", "module-2/The%20Rules%20of%20Antibiotics.mov"),
            vm(2, "mod2-malnutrition", "Marasmus & Kwashiorkor", "Identifying and treating malnutrition", "module-2/Marasmus%20and%20Kwashiorkor.mp4"),

            // Module 3: Chronic & Infectious Diseases
            vm(3, "mod3-chronic-illnesses", "Understanding Chronic Illnesses", "Introduction to non-communicable diseases", "module-3/Understanding%20Chronic%20Ilnesses.mp4"),
            vm(3, "mod3-diabetes", "Diabetes", "Managing and recognizing diabetes", "module-3/Diabetes.mov"),
            vm(3, "mod3-heart-disease", "Heart Disease, Hypertension & Strokes", "Cardiovascular conditions", "module-3/Heart%20Disease%2C%20Hypertension%2C%20%26%20Strokes.mov"),
            vm(3, "mod3-infectious-diseases", "Introduction to Infectious Diseases", "How infections spread and are prevented", "module-3/illnesses.mp4"),
            vm(3, "mod3-reproductive-tract", "Reproductive Tract Infections & Hepatitis B", "STIs and hepatitis B", "module-3/reproductive%20tract.mp4"),
            vm(3, "mod3-hiv-tb", "HIV/AIDS and Tuberculosis", "Understanding and managing HIV and TB", "module-3/HIV_AIDS%20and%20Tuberculosis.mp4"),

            // Module 4: Maternal & Reproductive Health
            vm(4, "mod4-antenatal-care", "Understanding Antenatal Care", "Prenatal care essentials", "module-4/Understanding_Antenatal_Care.mp4"),
            vm(4, "mod4-pregnancy", "Pregnancy: Normal or Not?", "Warning signs in pregnancy", "module-4/Pregnancy__Normal_or_Not_.mp4"),
            vm(4, "mod4-safe-delivery", "Safe Delivery & Newborn Care", "Birth and newborn essentials", "module-4/Safe_Delivery_%26_Newborn_Care.mp4"),
            vm(4, "mod4-birth-spacing", "Birth Spacing", "Healthy intervals between pregnancies", "module-4/Birth_Spacing.mp4"),
            vm(4, "mod4-short-term-contraception", "Short-Term Contraception", "Pills, injections, and barriers", "module-4/Short-Term_Contraception.mp4"),
            vm(4, "mod4-long-term-contraception", "Long-Term Contraception", "IUDs, implants, and permanent methods", "module-4/Long-Term_Contraception.mp4"),

            // Module 5: First Aid & Emergency Care
            vm(5, "mod5-abcde-method", "The ABCDE Method", "Primary assessment framework", "module-5/The%20ABCDE%20method.mp4"),
            vm(5, "mod5-treating-bleeding", "Treating Bleeding", "Controlling hemorrhage", "module-5/Treating_Bleeding.mp4"),
            vm(5, "mod5-splint-bone", "How to Splint a Broken Bone", "Fracture immobilization", "module-5/How_to_Splint_a_Broken_Bone.mp4"),
            vm(5, "mod5-choking", "How to Help Someone Choking", "Choking first aid response", "module-5/How_to_Help_Someone_Choking.mp4"),
            vm(5, "mod5-burns-stings", "First Aid: Burns & Stings", "Burn and sting treatment", "module-5/First_Aid__Burns_%26_Stings.mp4"),

            // Module 6: Infection Prevention & Control
            vm(6, "mod6-chain-of-infection", "The Chain of Infection", "How infections spread", "module-6/The_Chain_of_Infection.mp4"),
            vm(6, "mod6-5fs-disease", "The 5 F's of Disease", "Fecal-oral transmission routes", "module-6/The_5_F_s_of_Disease.mp4"),
            vm(6, "mod6-standard-precautions", "Standard Precautions", "Universal safety practices", "module-6/Standard_Precautions.mp4"),
            vm(6, "mod6-clinical-safety", "Clinical Safety Rules", "Safety in healthcare settings", "module-6/Clinical_Safety_Rules.mp4"),
            vm(6, "mod6-unseen-shield", "An Unseen Shield", "Immunity and vaccination", "module-6/An_Unseen_Shield.mp4")
        )

        private fun vm(mod: Int, id: String, title: String, desc: String, s3Path: String) = VideoModule(
            id = id,
            moduleNumber = mod,
            title = title,
            description = desc,
            thumbnail = MODULE_ICONS[mod] ?: "",
            duration = "",
            category = VideoCategory.BASICS,
            videoUrl = "$S3_BASE/$s3Path",
            s3Key = "videos/$s3Path",
            hasQuiz = true,
            watched = false
        )
    }

    private val _videos = MutableStateFlow<List<VideoModule>>(emptyList())
    val videos: StateFlow<List<VideoModule>> = _videos.asStateFlow()

    private val _watchedVideos = MutableStateFlow<Set<String>>(emptySet())
    val watchedVideos: StateFlow<Set<String>> = _watchedVideos.asStateFlow()

    private val _completedQuizzes = MutableStateFlow<Set<String>>(emptySet())
    val completedQuizzes: StateFlow<Set<String>> = _completedQuizzes.asStateFlow()

    /**
     * XP awarded by the most recent first-time [markVideoWatched] call in this ViewModel
     * (0 when the video had already been watched). Used by the player end card.
     */

    init {
        _videos.value = allVideos()
        loadSavedProgress()
    }

    private fun loadSavedProgress() {
        viewModelScope.launch {
            progressDataStore.getWatchedVideos().collect { watched ->
                _watchedVideos.value = watched
            }
        }
        viewModelScope.launch {
            progressDataStore.getCompletedQuizzes().collect { completed ->
                _completedQuizzes.value = completed
            }
        }
    }

    fun getModuleFolders(): List<VideoModuleFolder> {
        val watched = _watchedVideos.value
        val quizzes = _completedQuizzes.value
        return (1..MODULE_COUNT).map { modNum ->
            val modVideos = _videos.value.filter { it.moduleNumber == modNum }
            // Fallback text only; composables re-resolve these via moduleTitleRes()/moduleDescriptionRes()
            VideoModuleFolder(
                moduleNumber = modNum,
                title = context.getString(moduleTitleRes(modNum)),
                description = context.getString(moduleDescriptionRes(modNum)),
                icon = MODULE_ICONS[modNum] ?: "",
                videoCount = modVideos.size,
                watchedCount = modVideos.count { watched.contains(it.id) },
                quizzesCompleted = modVideos.count { quizzes.contains(it.id) }
            )
        }
    }

    fun getVideosForModule(moduleNumber: Int): List<VideoModule> {
        val watched = _watchedVideos.value
        val quizzes = _completedQuizzes.value
        return _videos.value
            .filter { it.moduleNumber == moduleNumber }
            .map { video ->
                video.copy(
                    watched = watched.contains(video.id),
                    quizComplete = quizzes.contains(video.id)
                )
            }
    }

    /** First unwatched video across all modules, in module/video order; null when everything is watched. */
    fun nextUnwatchedVideo(): VideoModule? {
        val watched = _watchedVideos.value
        return _videos.value.firstOrNull { !watched.contains(it.id) }
    }

    /**
     * Next unwatched video inside [moduleNumber]. When [afterVideoId] is given, videos after it are
     * preferred; if none remain after it, the first unwatched video before it is returned instead.
     */
    fun nextUnwatchedInModule(moduleNumber: Int, afterVideoId: String?): VideoModule? {
        val watched = _watchedVideos.value
        val modVideos = _videos.value.filter { it.moduleNumber == moduleNumber }
        val unwatched = modVideos.filter { !watched.contains(it.id) && it.id != afterVideoId }
        if (afterVideoId == null) return unwatched.firstOrNull()
        val currentIndex = modVideos.indexOfFirst { it.id == afterVideoId }
        if (currentIndex < 0) return unwatched.firstOrNull()
        val after = modVideos.drop(currentIndex + 1).firstOrNull { !watched.contains(it.id) }
        return after ?: unwatched.firstOrNull()
    }

    /** Module number that contains [videoId], or null for an unknown id. */
    fun moduleNumberFor(videoId: String): Int? =
        _videos.value.find { it.id == videoId }?.moduleNumber

    fun isVideoWatched(videoId: String): Boolean = _watchedVideos.value.contains(videoId)

    /** 1-based position of [videoId] inside its module (for "Video 2 of 8"), or null. */
    fun videoIndexInModule(videoId: String): Int? {
        val video = _videos.value.find { it.id == videoId } ?: return null
        val modVideos = _videos.value.filter { it.moduleNumber == video.moduleNumber }
        val index = modVideos.indexOfFirst { it.id == videoId }
        return if (index >= 0) index + 1 else null
    }

    fun videoCountInModule(moduleNumber: Int): Int =
        _videos.value.count { it.moduleNumber == moduleNumber }

    /** Marks a video watched (idempotent) and syncs the progress event to the API. */
    fun markVideoWatched(videoId: String) {
        if (_watchedVideos.value.contains(videoId)) return
        _watchedVideos.value = _watchedVideos.value + videoId
        viewModelScope.launch {
            progressDataStore.markVideoWatched(videoId)
            syncProgressToApi("module_watched", videoId)
        }
    }

    fun markQuizCompleted(videoId: String) {
        if (_completedQuizzes.value.contains(videoId)) return
        _completedQuizzes.value = _completedQuizzes.value + videoId
        viewModelScope.launch {
            progressDataStore.markQuizCompleted(videoId)
            syncProgressToApi("module_quiz_complete", videoId)
        }
    }

    private suspend fun syncProgressToApi(type: String, itemId: String) {
        try {
            val token = tokenManager.getIdToken() ?: return
            val body = mapOf<String, Any>("type" to type, "itemId" to itemId)
            apiService.updateUserProgress("Bearer $token", body)
        } catch (e: Exception) {
            Log.d("VideoModulesVM", "Progress sync failed (will retry): ${e.message}")
        }
    }

    fun getVideoUrl(videoId: String): String? =
        _videos.value.find { it.id == videoId }?.videoUrl

    fun getVideoTitle(videoId: String): String =
        _videos.value.find { it.id == videoId }?.title ?: "Video"

    fun getWatchedCount(): Int = _watchedVideos.value.size
    fun getQuizCompletedCount(): Int = _completedQuizzes.value.size
    fun getTotalVideos(): Int = _videos.value.size
}
