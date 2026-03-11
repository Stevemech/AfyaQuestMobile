package com.example.afyaquest.presentation.modulequiz

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.afyaquest.data.remote.ApiService
import com.example.afyaquest.domain.model.ModuleQuizQuestion
import com.example.afyaquest.domain.model.QuizSubmissionRequest
import com.example.afyaquest.domain.model.QuizAnswer
import com.example.afyaquest.sync.VideoDownloadManager
import com.example.afyaquest.util.ProgressDataStore
import com.example.afyaquest.util.TokenManager
import com.example.afyaquest.util.XpManager
import com.example.afyaquest.util.XpRewards
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ModuleQuizViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val progressDataStore: ProgressDataStore,
    private val apiService: ApiService,
    private val tokenManager: TokenManager,
    private val xpManager: XpManager,
    private val videoDownloadManager: VideoDownloadManager
) : ViewModel() {

    val moduleId: String = savedStateHandle.get<String>("moduleId") ?: ""

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex.asStateFlow()

    private val _selectedAnswer = MutableStateFlow<Int?>(null)
    val selectedAnswer: StateFlow<Int?> = _selectedAnswer.asStateFlow()

    private val _showExplanation = MutableStateFlow(false)
    val showExplanation: StateFlow<Boolean> = _showExplanation.asStateFlow()

    private val _correctAnswers = MutableStateFlow(0)
    val correctAnswers: StateFlow<Int> = _correctAnswers.asStateFlow()

    private val _isFinished = MutableStateFlow(false)
    val isFinished: StateFlow<Boolean> = _isFinished.asStateFlow()

    private val moduleTitles = mapOf(
        "mod1-nervous-system" to "Nervous System",
        "mod1-musculoskeletal" to "Musculoskeletal System",
        "mod1-lymphatic" to "Lymphatic System",
        "mod1-endocrine" to "The Endocrine System",
        "mod1-integumentary" to "The Integumentary System",
        "mod1-urinary" to "Urinary System",
        "mod1-male-reproductive" to "Male Reproductive System",
        "mod1-female-reproductive" to "Female Reproductive System",
        "mod2-warning-signs" to "Warning Signs",
        "mod2-diarrhea" to "Diarrhea",
        "mod2-respiratory-infections" to "Respiratory Infections",
        "mod2-asthma-pneumonia-tb" to "Asthma, Pneumonia, and Tuberculosis",
        "mod2-antibiotics" to "The Rules of Antibiotics",
        "mod2-malnutrition" to "Marasmus and Kwashiorkor",
        "mod3-chronic-illnesses" to "Understanding Chronic Illnesses",
        "mod3-diabetes" to "Diabetes",
        "mod3-heart-disease" to "Heart Disease, Hypertension, & Strokes",
        "mod3-infectious-diseases" to "Infectious Diseases",
        "mod3-reproductive-tract" to "Reproductive Tract Infections",
        "mod3-hiv-tb" to "HIV/AIDS and Tuberculosis",
        "mod4-antenatal-care" to "Understanding Antenatal Care",
        "mod4-pregnancy" to "Pregnancy: Normal or Not?",
        "mod4-safe-delivery" to "Safe Delivery & Newborn Care",
        "mod4-birth-spacing" to "Birth Spacing",
        "mod4-short-term-contraception" to "Short-Term Contraception",
        "mod4-long-term-contraception" to "Long-Term Contraception",
        "mod5-abcde-method" to "The ABCDE Method",
        "mod5-treating-bleeding" to "Treating Bleeding",
        "mod5-splint-bone" to "How to Splint a Broken Bone",
        "mod5-choking" to "How to Help Someone Choking",
        "mod5-burns-stings" to "First Aid: Burns & Stings",
        "mod6-chain-of-infection" to "The Chain of Infection",
        "mod6-5fs-disease" to "The 5 F's of Disease",
        "mod6-standard-precautions" to "Standard Precautions",
        "mod6-clinical-safety" to "Clinical Safety Rules",
        "mod6-unseen-shield" to "An Unseen Shield"
    )

    // ──────────────────────────────────────────────────────────────
    //  ALL QUIZ DATA — 36 videos across 6 modules
    // ──────────────────────────────────────────────────────────────

    private val allQuizzes: Map<String, List<ModuleQuizQuestion>> = mapOf(

        // ════════════════════════════════════════════════════════════
        //  MODULE 1 — Human Body Systems
        // ════════════════════════════════════════════════════════════

        "mod1-nervous-system" to listOf(
            ModuleQuizQuestion(
                id = "mod1-ns-q1",
                moduleId = "mod1",
                question = "Which structure serves as the main communication pathway between the brain and the rest of the body?",
                options = listOf("Nerves", "Spinal cord", "Muscles", "Heart"),
                correctAnswerIndex = 1,
                explanation = "The spinal cord is the major communication pathway that relays signals between the brain and the rest of the body."
            ),
            ModuleQuizQuestion(
                id = "mod1-ns-q2",
                moduleId = "mod1",
                question = "A person can move their arm but cannot feel touch or temperature in it. Which structure is most likely involved?",
                options = listOf("Brain", "Spinal cord", "Nerves", "Lungs"),
                correctAnswerIndex = 2,
                explanation = "Sensory nerves carry signals about touch and temperature. Damage to these nerves can eliminate sensation while motor function remains intact."
            ),
            ModuleQuizQuestion(
                id = "mod1-ns-q3",
                moduleId = "mod1",
                question = "Which activity is primarily controlled by the somatic nervous system?",
                options = listOf("Digestion", "Sweating", "Speaking", "Heartbeat"),
                correctAnswerIndex = 2,
                explanation = "The somatic nervous system controls voluntary actions such as speaking and moving skeletal muscles."
            ),
            ModuleQuizQuestion(
                id = "mod1-ns-q4",
                moduleId = "mod1",
                question = "Which of the following best describes the role of nerves?",
                options = listOf(
                    "They store memories",
                    "They transmit signals between the CNS and the body",
                    "They produce movement independently",
                    "They regulate blood flow directly"
                ),
                correctAnswerIndex = 1,
                explanation = "Nerves act as communication cables that transmit electrical signals between the central nervous system and the rest of the body."
            ),
            ModuleQuizQuestion(
                id = "mod1-ns-q5",
                moduleId = "mod1",
                question = "A person suddenly becomes confused and cannot answer simple questions. Which part of the nervous system is most likely affected?",
                options = listOf("Nerves", "Spinal cord", "Brain", "Muscles"),
                correctAnswerIndex = 2,
                explanation = "Sudden confusion and inability to answer questions indicate a problem with brain function, as the brain controls cognition and communication."
            ),
            ModuleQuizQuestion(
                id = "mod1-ns-q6",
                moduleId = "mod1",
                question = "Which situation best demonstrates parasympathetic nervous system activity?",
                options = listOf(
                    "Heart rate increasing during fear",
                    "Muscles preparing for action",
                    "Digesting food after a meal",
                    "Running from danger"
                ),
                correctAnswerIndex = 2,
                explanation = "The parasympathetic nervous system controls 'rest and digest' functions, including digestion after eating."
            ),
            ModuleQuizQuestion(
                id = "mod1-ns-q7",
                moduleId = "mod1",
                question = "During assessment, why is checking alertness the first step?",
                options = listOf(
                    "It evaluates digestion",
                    "It quickly provides information about brain function",
                    "It measures muscle strength",
                    "It confirms spinal cord integrity"
                ),
                correctAnswerIndex = 1,
                explanation = "Alertness is a quick indicator of brain function. Changes in alertness can signal serious neurological problems."
            ),
            ModuleQuizQuestion(
                id = "mod1-ns-q8",
                moduleId = "mod1",
                question = "A person has numbness only on the left side of the body. Why is this particularly concerning?",
                options = listOf(
                    "It is common after sleeping",
                    "It may signal a serious nervous system problem",
                    "It is caused by digestion",
                    "It is related to fatigue"
                ),
                correctAnswerIndex = 1,
                explanation = "One-sided numbness can indicate a stroke or other serious nervous system condition and requires urgent evaluation."
            ),
            ModuleQuizQuestion(
                id = "mod1-ns-q9",
                moduleId = "mod1",
                question = "During nervous system assessment, which step should come first?",
                options = listOf(
                    "Ask about past illnesses",
                    "Observe whether the person is awake and alert",
                    "Refer immediately",
                    "Check digestion"
                ),
                correctAnswerIndex = 1,
                explanation = "Observing alertness is the first priority because it reveals the most about current brain function and urgency of the situation."
            ),
            ModuleQuizQuestion(
                id = "mod1-ns-q10",
                moduleId = "mod1",
                question = "A person has sudden shaking followed by confusion. What is the most appropriate action?",
                options = listOf(
                    "Wait to see if it happens again",
                    "Encourage rest only",
                    "Urgently refer for medical evaluation",
                    "Ignore if the person feels better"
                ),
                correctAnswerIndex = 2,
                explanation = "Sudden shaking (possible seizure) followed by confusion is a neurological emergency that requires urgent medical evaluation."
            )
        ),

        "mod1-musculoskeletal" to listOf(
            ModuleQuizQuestion(
                id = "mod1-ms-q1",
                moduleId = "mod1",
                question = "The musculoskeletal system is made up of which two main systems?",
                options = listOf(
                    "Nervous and circulatory",
                    "Muscular and skeletal",
                    "Digestive and respiratory",
                    "Endocrine and immune"
                ),
                correctAnswerIndex = 1,
                explanation = "The musculoskeletal system is the combination of the muscular system and the skeletal system working together."
            ),
            ModuleQuizQuestion(
                id = "mod1-ms-q2",
                moduleId = "mod1",
                question = "Which of the following is a function of bones?",
                options = listOf("Pumping blood", "Producing blood cells", "Digesting food", "Controlling thoughts"),
                correctAnswerIndex = 1,
                explanation = "Bones produce blood cells in the bone marrow, in addition to providing structural support and protecting organs."
            ),
            ModuleQuizQuestion(
                id = "mod1-ms-q3",
                moduleId = "mod1",
                question = "Which type of muscle allows a person to lift their arm consciously?",
                options = listOf("Smooth muscle", "Cardiac muscle", "Skeletal muscle", "Involuntary muscle"),
                correctAnswerIndex = 2,
                explanation = "Skeletal muscles are voluntary muscles attached to bones, allowing conscious movements like lifting an arm."
            ),
            ModuleQuizQuestion(
                id = "mod1-ms-q4",
                moduleId = "mod1",
                question = "What do tendons connect?",
                options = listOf("Bone to bone", "Muscle to bone", "Muscle to muscle", "Bone to cartilage"),
                correctAnswerIndex = 1,
                explanation = "Tendons are strong connective tissues that attach muscles to bones, enabling movement."
            ),
            ModuleQuizQuestion(
                id = "mod1-ms-q5",
                moduleId = "mod1",
                question = "Which structure cushions joints and prevents bones from rubbing together?",
                options = listOf("Ligaments", "Tendons", "Cartilage", "Bone marrow"),
                correctAnswerIndex = 2,
                explanation = "Cartilage is a smooth, flexible tissue that cushions joints and prevents friction between bones."
            ),
            ModuleQuizQuestion(
                id = "mod1-ms-q6",
                moduleId = "mod1",
                question = "A patient twisted their ankle. Which structure is most likely injured?",
                options = listOf("Bone", "Ligament", "Cardiac muscle", "Bone marrow"),
                correctAnswerIndex = 1,
                explanation = "A twisted ankle typically injures ligaments, which connect bone to bone and stabilize joints."
            ),
            ModuleQuizQuestion(
                id = "mod1-ms-q7",
                moduleId = "mod1",
                question = "A person lifts a heavy object and develops pain in the back muscle. This is most likely a:",
                options = listOf("Fracture", "Dislocation", "Sprain", "Strain"),
                correctAnswerIndex = 3,
                explanation = "A strain is an injury to a muscle or tendon, often caused by overexertion such as lifting heavy objects."
            ),
            ModuleQuizQuestion(
                id = "mod1-ms-q8",
                moduleId = "mod1",
                question = "A bone is visible through a wound after an accident. Why is this an emergency?",
                options = listOf(
                    "It confirms a strain",
                    "It is an open fracture with high bleeding and infection risk",
                    "It only affects muscles",
                    "It will heal on its own"
                ),
                correctAnswerIndex = 1,
                explanation = "An open (compound) fracture exposes bone to the environment, creating high risk for severe bleeding and infection."
            ),
            ModuleQuizQuestion(
                id = "mod1-ms-q9",
                moduleId = "mod1",
                question = "Which fractures are especially dangerous because they can cause severe internal bleeding?",
                options = listOf(
                    "Finger and toe fractures",
                    "Rib fractures only",
                    "Pelvic and femur fractures",
                    "Wrist fractures"
                ),
                correctAnswerIndex = 2,
                explanation = "Pelvic and femur fractures can damage large blood vessels, leading to life-threatening internal bleeding."
            ),
            ModuleQuizQuestion(
                id = "mod1-ms-q10",
                moduleId = "mod1",
                question = "A joint looks visibly out of place after a fall. What should you do?",
                options = listOf(
                    "Push it back into position",
                    "Ask the patient to force movement",
                    "Immobilize it and refer for medical care",
                    "Massage it firmly"
                ),
                correctAnswerIndex = 2,
                explanation = "A dislocated joint should be immobilized as-is and referred for professional medical care. Attempting to reposition it can cause further damage."
            )
        ),

        "mod1-lymphatic" to listOf(
            ModuleQuizQuestion(
                id = "mod1-ly-q1",
                moduleId = "mod1",
                question = "What are the two main functions of the lymphatic system?",
                options = listOf(
                    "Pump blood and control movement",
                    "Fight infection and drain excess fluid",
                    "Produce hormones and control temperature",
                    "Digest food and absorb nutrients"
                ),
                correctAnswerIndex = 1,
                explanation = "The lymphatic system's two primary roles are defending the body against infection and draining excess interstitial fluid."
            ),
            ModuleQuizQuestion(
                id = "mod1-ly-q2",
                moduleId = "mod1",
                question = "What is lymph?",
                options = listOf(
                    "A type of muscle",
                    "Extra fluid that contains waste and infection-fighting cells",
                    "A hormone made by the spleen",
                    "A type of bone marrow"
                ),
                correctAnswerIndex = 1,
                explanation = "Lymph is the clear fluid collected from tissues that carries waste products and immune cells through the lymphatic system."
            ),
            ModuleQuizQuestion(
                id = "mod1-ly-q3",
                moduleId = "mod1",
                question = "Why do lymph nodes often swell during infection?",
                options = listOf(
                    "They store blood",
                    "They produce hormones",
                    "They trap bacteria and viruses",
                    "They lose fluid"
                ),
                correctAnswerIndex = 2,
                explanation = "Lymph nodes swell because they are actively trapping and filtering pathogens while mounting an immune response."
            ),
            ModuleQuizQuestion(
                id = "mod1-ly-q4",
                moduleId = "mod1",
                question = "Which part of the lymphatic system helps develop white blood cells, especially in children?",
                options = listOf("Spleen", "Thoracic duct", "Thymus", "Lymphatic vessels"),
                correctAnswerIndex = 2,
                explanation = "The thymus is crucial for developing T-lymphocytes (a type of white blood cell) and is most active during childhood."
            ),
            ModuleQuizQuestion(
                id = "mod1-ly-q5",
                moduleId = "mod1",
                question = "A patient has painless swelling in the neck along with fatigue and weight loss. Which condition should you suspect?",
                options = listOf("Lymphadenopathy", "Lymphoma", "Lymphedema", "Cellulitis"),
                correctAnswerIndex = 1,
                explanation = "Painless lymph node swelling combined with systemic symptoms like fatigue and weight loss may indicate lymphoma, a cancer of the lymphatic system."
            ),
            ModuleQuizQuestion(
                id = "mod1-ly-q6",
                moduleId = "mod1",
                question = "Long-term swelling in one arm due to poor lymph drainage is most likely:",
                options = listOf("Lymphadenopathy", "Cellulitis", "Lymphedema", "Lymphoma"),
                correctAnswerIndex = 2,
                explanation = "Lymphedema is chronic swelling caused by impaired lymphatic drainage, often affecting a single limb."
            ),
            ModuleQuizQuestion(
                id = "mod1-ly-q7",
                moduleId = "mod1",
                question = "During a S.W.A.M.P. assessment, what does the 'A' stand for?",
                options = listOf(
                    "Apply pressure",
                    "Ask about fever, wounds, or recent infections",
                    "Assess blood pressure",
                    "Align the limb"
                ),
                correctAnswerIndex = 1,
                explanation = "In the S.W.A.M.P. mnemonic, 'A' stands for Ask — gathering history about fever, wounds, or recent infections to identify possible causes."
            ),
            ModuleQuizQuestion(
                id = "mod1-ly-q8",
                moduleId = "mod1",
                question = "A swollen area is red, warm, painful, and spreading quickly. What is your most appropriate action?",
                options = listOf(
                    "Monitor for one week",
                    "Massage the area",
                    "Refer immediately to a health facility",
                    "Ignore if there is no fever"
                ),
                correctAnswerIndex = 2,
                explanation = "Rapidly spreading redness with warmth and pain suggests a serious infection like cellulitis that requires immediate medical care."
            ),
            ModuleQuizQuestion(
                id = "mod1-ly-q9",
                moduleId = "mod1",
                question = "Which combination of symptoms requires immediate referral?",
                options = listOf(
                    "Mild swelling without pain",
                    "Swelling with fever and spreading redness",
                    "Slight tenderness after exercise",
                    "Swelling that improves with rest"
                ),
                correctAnswerIndex = 1,
                explanation = "Swelling combined with fever and spreading redness indicates a possible serious infection requiring urgent medical attention."
            ),
            ModuleQuizQuestion(
                id = "mod1-ly-q10",
                moduleId = "mod1",
                question = "Why is early recognition of lymphatic problems important?",
                options = listOf(
                    "It prevents muscle growth",
                    "It reduces appetite",
                    "It helps prevent serious complications and infection spread",
                    "It strengthens bones"
                ),
                correctAnswerIndex = 2,
                explanation = "Early recognition allows timely treatment, preventing complications such as chronic swelling, systemic infection, or cancer progression."
            )
        ),

        "mod1-endocrine" to listOf(
            ModuleQuizQuestion(
                id = "mod1-en-q1",
                moduleId = "mod1",
                question = "A patient is confused, sweaty, shaky, and complaining of dizziness. What should a CHV suspect first?",
                options = listOf("Hyperglycemia", "Hypoglycemia", "Thyroid underactivity", "Dehydration"),
                correctAnswerIndex = 1,
                explanation = "Confusion, sweating, shakiness, and dizziness are classic signs of hypoglycemia (low blood sugar), which requires prompt action."
            ),
            ModuleQuizQuestion(
                id = "mod1-en-q2",
                moduleId = "mod1",
                question = "A patient reports excessive thirst, frequent urination, and warm, dry skin. These symptoms are most consistent with:",
                options = listOf(
                    "Low blood sugar",
                    "High blood sugar",
                    "Thyroid overactivity",
                    "Adrenal hormone release"
                ),
                correctAnswerIndex = 1,
                explanation = "Excessive thirst, frequent urination, and warm dry skin are hallmarks of hyperglycemia (high blood sugar)."
            ),
            ModuleQuizQuestion(
                id = "mod1-en-q3",
                moduleId = "mod1",
                question = "A patient feels hot, anxious, has lost weight recently, and has a rapid pulse. Which gland is most likely involved?",
                options = listOf("Pancreas", "Pituitary gland", "Thyroid gland", "Reproductive glands"),
                correctAnswerIndex = 2,
                explanation = "Heat intolerance, anxiety, weight loss, and rapid pulse are signs of an overactive thyroid (hyperthyroidism)."
            ),
            ModuleQuizQuestion(
                id = "mod1-en-q4",
                moduleId = "mod1",
                question = "A CHV notices a patient is extremely weak and becomes unresponsive. This is considered:",
                options = listOf(
                    "A mild endocrine imbalance",
                    "A normal stress response",
                    "A red flag requiring immediate attention",
                    "A symptom of thyroid underactivity only"
                ),
                correctAnswerIndex = 2,
                explanation = "Extreme weakness and unresponsiveness are red flags indicating a potential life-threatening emergency requiring immediate action."
            ),
            ModuleQuizQuestion(
                id = "mod1-en-q5",
                moduleId = "mod1",
                question = "Why is hypoglycemia considered especially dangerous?",
                options = listOf(
                    "It always causes weight loss",
                    "It can quickly affect mental status and cause loss of consciousness",
                    "It leads to slow hormone release",
                    "It only affects adults"
                ),
                correctAnswerIndex = 1,
                explanation = "Hypoglycemia is dangerous because the brain depends on glucose; low levels can rapidly cause confusion, seizures, and loss of consciousness."
            ),
            ModuleQuizQuestion(
                id = "mod1-en-q6",
                moduleId = "mod1",
                question = "A patient reports cold intolerance, fatigue, weight gain, and a slow pulse. These findings suggest a problem with the:",
                options = listOf("Pancreas", "Adrenal glands", "Thyroid gland", "Pituitary gland"),
                correctAnswerIndex = 2,
                explanation = "Cold intolerance, fatigue, weight gain, and a slow pulse are classic signs of an underactive thyroid (hypothyroidism)."
            ),
            ModuleQuizQuestion(
                id = "mod1-en-q7",
                moduleId = "mod1",
                question = "A CHV assessing for endocrine problems should prioritize which observation?",
                options = listOf("Hair color", "Mental status and energy level", "Height", "Shoe size"),
                correctAnswerIndex = 1,
                explanation = "Mental status and energy level are key indicators of endocrine function, especially blood sugar and thyroid status."
            ),
            ModuleQuizQuestion(
                id = "mod1-en-q8",
                moduleId = "mod1",
                question = "The pancreas helps maintain balance in the body primarily by regulating:",
                options = listOf(
                    "Stress hormones",
                    "Growth hormones",
                    "Blood sugar levels",
                    "Body temperature directly"
                ),
                correctAnswerIndex = 2,
                explanation = "The pancreas produces insulin and glucagon to regulate blood sugar levels, maintaining metabolic homeostasis."
            ),
            ModuleQuizQuestion(
                id = "mod1-en-q9",
                moduleId = "mod1",
                question = "A patient has signs of very high blood sugar along with dehydration. This requires:",
                options = listOf(
                    "Home observation only",
                    "Increased appetite",
                    "Immediate attention to prevent serious complications",
                    "Waiting 24 hours"
                ),
                correctAnswerIndex = 2,
                explanation = "High blood sugar with dehydration can progress to diabetic ketoacidosis or hyperosmolar state, both of which are medical emergencies."
            ),
            ModuleQuizQuestion(
                id = "mod1-en-q10",
                moduleId = "mod1",
                question = "The endocrine system differs from the nervous system because it:",
                options = listOf(
                    "Works faster and has shorter effects",
                    "Works more slowly but has longer-lasting effects",
                    "Does not affect homeostasis",
                    "Only functions during stress"
                ),
                correctAnswerIndex = 1,
                explanation = "The endocrine system uses hormones that travel through the bloodstream, producing slower but longer-lasting effects compared to nerve signals."
            )
        ),

        "mod1-integumentary" to listOf(
            ModuleQuizQuestion(
                id = "mod1-ig-q1",
                moduleId = "mod1",
                question = "A patient has redness, warmth, swelling, pain, and fever in their lower leg. What condition should a CHV suspect?",
                options = listOf("Dehydration", "Cellulitis", "Mild bruising", "Vitamin D deficiency"),
                correctAnswerIndex = 1,
                explanation = "Redness, warmth, swelling, pain, and fever in a limb are classic signs of cellulitis, a bacterial skin infection."
            ),
            ModuleQuizQuestion(
                id = "mod1-ig-q2",
                moduleId = "mod1",
                question = "A patient presents with dry skin, dizziness, fatigue, and poor skin turgor. These findings suggest:",
                options = listOf("Edema", "Cellulitis", "Dehydration", "Burn injury"),
                correctAnswerIndex = 2,
                explanation = "Dry skin, dizziness, fatigue, and poor skin turgor (slow return after pinching) are hallmark signs of dehydration."
            ),
            ModuleQuizQuestion(
                id = "mod1-ig-q3",
                moduleId = "mod1",
                question = "Which skin layer contains blood vessels, nerves, and sweat glands?",
                options = listOf("Epidermis", "Dermis", "Hypodermis", "Nail bed"),
                correctAnswerIndex = 1,
                explanation = "The dermis is the middle layer of skin that contains blood vessels, nerves, sweat glands, and hair follicles."
            ),
            ModuleQuizQuestion(
                id = "mod1-ig-q4",
                moduleId = "mod1",
                question = "A patient has swelling in both ankles without redness or fever. This could indicate problems related to the:",
                options = listOf(
                    "Brain only",
                    "Digestive system",
                    "Heart, kidney, liver, or lymphatic system",
                    "Thyroid gland"
                ),
                correctAnswerIndex = 2,
                explanation = "Bilateral ankle edema without infection signs can indicate systemic issues with the heart, kidneys, liver, or lymphatic drainage."
            ),
            ModuleQuizQuestion(
                id = "mod1-ig-q5",
                moduleId = "mod1",
                question = "A CHV notices rapidly spreading redness with increasing pain and fever. What is the most appropriate action?",
                options = listOf(
                    "Reassure and monitor at home",
                    "Apply lotion and recheck later",
                    "Immediate referral to a health facility",
                    "Encourage exercise"
                ),
                correctAnswerIndex = 2,
                explanation = "Rapidly spreading redness with pain and fever indicates a potentially serious infection requiring immediate medical care."
            ),
            ModuleQuizQuestion(
                id = "mod1-ig-q6",
                moduleId = "mod1",
                question = "A patient has blistering and charred skin after contact with a hot surface. Why must burns be taken seriously?",
                options = listOf(
                    "They only affect appearance",
                    "They can lead to fluid loss and infection",
                    "They heal without complications",
                    "They rarely involve deeper layers"
                ),
                correctAnswerIndex = 1,
                explanation = "Severe burns destroy the skin's protective barrier, leading to significant fluid loss and high risk of infection."
            ),
            ModuleQuizQuestion(
                id = "mod1-ig-q7",
                moduleId = "mod1",
                question = "Which of the following findings would most strongly suggest infection rather than simple swelling?",
                options = listOf(
                    "Swelling without pain",
                    "Redness with warmth and fever",
                    "Mild ankle puffiness",
                    "Dry skin only"
                ),
                correctAnswerIndex = 1,
                explanation = "Redness, warmth, and fever together are cardinal signs of infection, distinguishing it from non-infectious swelling."
            ),
            ModuleQuizQuestion(
                id = "mod1-ig-q8",
                moduleId = "mod1",
                question = "A CHV assessing the integumentary system should prioritize observing:",
                options = listOf(
                    "Skin color, temperature, moisture, and condition",
                    "Eye movement",
                    "Lung sounds",
                    "Heart rhythm"
                ),
                correctAnswerIndex = 0,
                explanation = "Skin color, temperature, moisture, and overall condition provide the most relevant information about integumentary health."
            ),
            ModuleQuizQuestion(
                id = "mod1-ig-q9",
                moduleId = "mod1",
                question = "Damage to deeper skin layers can interfere with the body's ability to:",
                options = listOf(
                    "Produce hormones",
                    "Regulate temperature and fight infection",
                    "Digest food",
                    "Control movement"
                ),
                correctAnswerIndex = 1,
                explanation = "The skin plays a vital role in temperature regulation and serving as a barrier against infection; deep damage compromises both."
            ),
            ModuleQuizQuestion(
                id = "mod1-ig-q10",
                moduleId = "mod1",
                question = "Using the mnemonic 'RED HOT' which of the following represents the letter 'H'?",
                options = listOf("Hydration", "Heat", "Hair loss", "Hypotension"),
                correctAnswerIndex = 1,
                explanation = "In the RED HOT mnemonic, 'H' stands for Heat, which is an important sign to assess when evaluating skin conditions."
            )
        ),

        "mod1-urinary" to listOf(
            ModuleQuizQuestion(
                id = "mod1-ur-q1",
                moduleId = "mod1",
                question = "Which of the following is NOT an organ associated with the urinary system?",
                options = listOf("Kidney", "Ureter", "Liver", "Urethra"),
                correctAnswerIndex = 2,
                explanation = "The liver is a digestive organ, not part of the urinary system. The kidneys, ureters, and urethra are all key urinary system organs."
            ),
            ModuleQuizQuestion(
                id = "mod1-ur-q2",
                moduleId = "mod1",
                question = "What is the significance of the brain in urination?",
                options = listOf(
                    "Signals hormone secretion",
                    "Coordinates emptying and filling of bladder",
                    "Aids urine movement",
                    "Detects toxin buildup"
                ),
                correctAnswerIndex = 1,
                explanation = "The brain receives signals from the bladder and coordinates when to empty or hold urine by controlling the muscles involved in urination."
            ),
            ModuleQuizQuestion(
                id = "mod1-ur-q3",
                moduleId = "mod1",
                question = "Which is a common condition arising from complications in the urinary system?",
                options = listOf("UTIs", "Pneumonia", "Hypertension", "Seizure"),
                correctAnswerIndex = 0,
                explanation = "Urinary tract infections (UTIs) are one of the most common conditions affecting the urinary system."
            ),
            ModuleQuizQuestion(
                id = "mod1-ur-q4",
                moduleId = "mod1",
                question = "What can cause a urinary tract infection?",
                options = listOf(
                    "Toxin buildup",
                    "Drinking too much water",
                    "Bacteria entering any part of the urinary tract",
                    "Sitting on same toilet"
                ),
                correctAnswerIndex = 2,
                explanation = "UTIs are caused by bacteria entering the urinary tract, most commonly E. coli from the digestive tract."
            ),
            ModuleQuizQuestion(
                id = "mod1-ur-q5",
                moduleId = "mod1",
                question = "Which is NOT a symptom associated with urinary tract infections?",
                options = listOf(
                    "Painful/burning urination",
                    "Cloudy/bloody urine",
                    "More frequent/sudden urges",
                    "Thick and white discharge"
                ),
                correctAnswerIndex = 3,
                explanation = "Thick and white discharge is typically associated with a yeast infection, not a UTI."
            ),
            ModuleQuizQuestion(
                id = "mod1-ur-q6",
                moduleId = "mod1",
                question = "Urinary tract infections are more prevalent in females compared to males.",
                options = listOf("True", "False"),
                correctAnswerIndex = 0,
                explanation = "True. Females have a shorter urethra, which makes it easier for bacteria to reach the bladder."
            ),
            ModuleQuizQuestion(
                id = "mod1-ur-q7",
                moduleId = "mod1",
                question = "What are kidney stones?",
                options = listOf(
                    "Acidic toxin deposits",
                    "Hard clumps of salts and minerals",
                    "Filtering structures",
                    "Urine-forming proteins"
                ),
                correctAnswerIndex = 1,
                explanation = "Kidney stones are hard deposits made of salts and minerals that form inside the kidneys and can cause significant pain."
            ),
            ModuleQuizQuestion(
                id = "mod1-ur-q8",
                moduleId = "mod1",
                question = "Which is NOT a symptom associated with kidney stones?",
                options = listOf(
                    "Severe migraines",
                    "Extreme lower back and abdominal pain",
                    "High fever",
                    "Vomiting"
                ),
                correctAnswerIndex = 0,
                explanation = "Severe migraines are not a typical symptom of kidney stones. Common symptoms include intense lower back or abdominal pain, fever, and vomiting."
            ),
            ModuleQuizQuestion(
                id = "mod1-ur-q9",
                moduleId = "mod1",
                question = "What is a sign of poor hydration?",
                options = listOf(
                    "Strong-smelling urine",
                    "Relatively colorless urine",
                    "Frequent urges to urinate",
                    "A & B"
                ),
                correctAnswerIndex = 0,
                explanation = "Strong-smelling, dark-colored urine is a sign of dehydration. Relatively colorless urine indicates good hydration."
            ),
            ModuleQuizQuestion(
                id = "mod1-ur-q10",
                moduleId = "mod1",
                question = "What is a way of maintaining a healthy urinary system?",
                options = listOf(
                    "Wiping from back to front",
                    "Regularly drinking water",
                    "Resisting urges to urinate",
                    "Spending prolonged periods on the toilet"
                ),
                correctAnswerIndex = 1,
                explanation = "Drinking water regularly flushes bacteria from the urinary tract and helps prevent infections and kidney stones."
            )
        ),

        "mod1-male-reproductive" to listOf(
            ModuleQuizQuestion(
                id = "mod1-mr-q1",
                moduleId = "mod1",
                question = "Where are sperm cells produced?",
                options = listOf("Prostate", "Testicles", "Bladder", "Seminal Vesicle"),
                correctAnswerIndex = 1,
                explanation = "The testicles are responsible for producing sperm cells and the hormone testosterone."
            ),
            ModuleQuizQuestion(
                id = "mod1-mr-q2",
                moduleId = "mod1",
                question = "What is the function of the epididymis?",
                options = listOf(
                    "Produces semen",
                    "Stores and matures sperm",
                    "Produces testosterone",
                    "Pumps urine out of the body"
                ),
                correctAnswerIndex = 1,
                explanation = "The epididymis is a coiled tube where sperm are stored and mature before ejaculation."
            ),
            ModuleQuizQuestion(
                id = "mod1-mr-q3",
                moduleId = "mod1",
                question = "Impotence (erectile dysfunction) is defined as:",
                options = listOf(
                    "Inability to produce sperm",
                    "Problems with achieving or keeping an erection",
                    "Inability of sperm to swim",
                    "Blockage in the urethra"
                ),
                correctAnswerIndex = 1,
                explanation = "Erectile dysfunction (impotence) is the inability to achieve or maintain an erection sufficient for sexual activity."
            ),
            ModuleQuizQuestion(
                id = "mod1-mr-q4",
                moduleId = "mod1",
                question = "Sexually Transmissible Infections (STIs) are:",
                options = listOf(
                    "Only caused by fungi",
                    "Only transmitted through sharing food",
                    "Bacterial or viral infections acquired through sexual contact",
                    "Caused by low sperm production"
                ),
                correctAnswerIndex = 2,
                explanation = "STIs are infections caused by bacteria, viruses, or parasites that are primarily transmitted through sexual contact."
            ),
            ModuleQuizQuestion(
                id = "mod1-mr-q5",
                moduleId = "mod1",
                question = "Which structure carries urine AND semen out of the body?",
                options = listOf("Vas deferens", "Penis", "Urethra", "Scrotum"),
                correctAnswerIndex = 2,
                explanation = "The urethra is the shared tube that carries both urine and semen out of the body, though not at the same time."
            )
        ),

        "mod1-female-reproductive" to listOf(
            ModuleQuizQuestion(
                id = "mod1-fr-q1",
                moduleId = "mod1",
                question = "Which structure is the passageway leading from the uterus to the outside of the body?",
                options = listOf("Cervix", "Vagina", "Ovary", "Urethra"),
                correctAnswerIndex = 1,
                explanation = "The vagina is the muscular canal that connects the uterus to the outside of the body."
            ),
            ModuleQuizQuestion(
                id = "mod1-fr-q2",
                moduleId = "mod1",
                question = "An ectopic pregnancy occurs when a fertilized egg implants:",
                options = listOf(
                    "In the uterus",
                    "In the cervix",
                    "Outside the uterus, often in the fallopian tube",
                    "In the vaginal canal"
                ),
                correctAnswerIndex = 2,
                explanation = "An ectopic pregnancy occurs when a fertilized egg implants outside the uterus, most commonly in a fallopian tube, and is a medical emergency."
            ),
            ModuleQuizQuestion(
                id = "mod1-fr-q3",
                moduleId = "mod1",
                question = "Which part of the female reproductive system serves as the birth canal?",
                options = listOf("Vagina", "Fallopian tube", "Ovary", "Bladder"),
                correctAnswerIndex = 0,
                explanation = "The vagina serves as the birth canal during delivery, allowing the baby to pass from the uterus to the outside."
            ),
            ModuleQuizQuestion(
                id = "mod1-fr-q4",
                moduleId = "mod1",
                question = "Which structure connects the ovaries to the uterus and is the site where fertilization usually occurs?",
                options = listOf("Vagina", "Cervix", "Fallopian tubes", "Urethra"),
                correctAnswerIndex = 2,
                explanation = "The fallopian tubes connect the ovaries to the uterus, and fertilization of the egg by sperm typically occurs here."
            ),
            ModuleQuizQuestion(
                id = "mod1-fr-q5",
                moduleId = "mod1",
                question = "Which organ is responsible for housing and protecting a developing fetus?",
                options = listOf("Ovary", "Cervix", "Uterus", "Vagina"),
                correctAnswerIndex = 2,
                explanation = "The uterus (womb) is where a fertilized egg implants and where the fetus develops and is protected throughout pregnancy."
            )
        ),

        // ════════════════════════════════════════════════════════════
        //  MODULE 2 — Child Health & Common Illnesses
        // ════════════════════════════════════════════════════════════

        "mod2-warning-signs" to listOf(
            ModuleQuizQuestion(
                id = "mod2-ws-q1",
                moduleId = "mod2",
                question = "Which is a sign that a child needs immediate medical help?",
                options = listOf("Mild cough", "Convulsing", "Sneezing", "Mild headache"),
                correctAnswerIndex = 1,
                explanation = "Convulsions (seizures) are a danger sign indicating a serious condition that requires immediate medical attention."
            ),
            ModuleQuizQuestion(
                id = "mod2-ws-q2",
                moduleId = "mod2",
                question = "True or False: If a child does not get better within 3 days, they should be taken to a doctor.",
                options = listOf("True", "False"),
                correctAnswerIndex = 0,
                explanation = "If a child's condition does not improve within 3 days, medical evaluation is recommended to prevent the illness from worsening."
            ),
            ModuleQuizQuestion(
                id = "mod2-ws-q3",
                moduleId = "mod2",
                question = "Which is NOT a sign of dehydration?",
                options = listOf("Sunken eyes", "Lethargy", "Slow skin pinch", "Sweating a lot"),
                correctAnswerIndex = 3,
                explanation = "Sweating indicates the body has adequate fluid. Sunken eyes, lethargy, and slow skin pinch return are signs of dehydration."
            ),
            ModuleQuizQuestion(
                id = "mod2-ws-q4",
                moduleId = "mod2",
                question = "Which is a sign of lethargy in a child?",
                options = listOf(
                    "Playing actively with toys",
                    "Quickly responding to voices",
                    "Drowsiness when responding to voices",
                    "Running around normally"
                ),
                correctAnswerIndex = 2,
                explanation = "A lethargic child appears drowsy and responds slowly to voices or stimulation, which is a warning sign of serious illness."
            ),
            ModuleQuizQuestion(
                id = "mod2-ws-q5",
                moduleId = "mod2",
                question = "Why are fevers considered an important sign of illness?",
                options = listOf(
                    "They always mean the child is dehydrated",
                    "They indicate the child likely has an infection",
                    "They only happen during the night",
                    "They mean the child needs surgery"
                ),
                correctAnswerIndex = 1,
                explanation = "Fever is the body's response to infection. It signals that the immune system is fighting a pathogen."
            ),
            ModuleQuizQuestion(
                id = "mod2-ws-q6",
                moduleId = "mod2",
                question = "What is the maximum number of paracetamol doses recommended per day for a child with a fever?",
                options = listOf("2", "3", "5", "Unlimited"),
                correctAnswerIndex = 1,
                explanation = "A maximum of 3 doses of paracetamol per day is recommended for children to safely manage fever without risking toxicity."
            )
        ),

        "mod2-diarrhea" to listOf(
            ModuleQuizQuestion(
                id = "mod2-di-q1",
                moduleId = "mod2",
                question = "How is diarrhea defined in babies?",
                options = listOf(
                    "One loose stool in 24 hours",
                    "Three or more watery stools within 24 hours",
                    "Any change in stool color",
                    "Vomiting with fever"
                ),
                correctAnswerIndex = 1,
                explanation = "Diarrhea is defined as three or more watery or loose stools within a 24-hour period."
            ),
            ModuleQuizQuestion(
                id = "mod2-di-q2",
                moduleId = "mod2",
                question = "What are the most serious risks of diarrhea in babies?",
                options = listOf(
                    "Fever and rash",
                    "Dehydration and malnutrition",
                    "Cough and cold",
                    "Weight gain"
                ),
                correctAnswerIndex = 1,
                explanation = "Diarrhea causes rapid fluid and nutrient loss, making dehydration and malnutrition the most serious and life-threatening risks."
            ),
            ModuleQuizQuestion(
                id = "mod2-di-q3",
                moduleId = "mod2",
                question = "Which is a sign of dehydration in babies?",
                options = listOf("Frequent smiling", "Sunken eyes", "Increased appetite", "Fast hair growth"),
                correctAnswerIndex = 1,
                explanation = "Sunken eyes are a key sign of dehydration in babies, along with dry mouth, reduced urination, and lethargy."
            ),
            ModuleQuizQuestion(
                id = "mod2-di-q4",
                moduleId = "mod2",
                question = "Which fluids are appropriate for babies with diarrhea (over 6 months)?",
                options = listOf(
                    "Soda and sugary drinks",
                    "Energy drinks",
                    "Water, soup, and fresh juice",
                    "Only solid foods"
                ),
                correctAnswerIndex = 2,
                explanation = "Water, soup, and fresh juice help replace lost fluids. Sugary and caffeinated drinks can worsen diarrhea."
            ),
            ModuleQuizQuestion(
                id = "mod2-di-q5",
                moduleId = "mod2",
                question = "When should urgent medical care be sought for a baby with diarrhea?",
                options = listOf(
                    "When the baby plays normally",
                    "When the baby sleeps well",
                    "When the baby has no tears, is very sleepy, and stops urinating",
                    "When the baby drinks fluids often"
                ),
                correctAnswerIndex = 2,
                explanation = "No tears, extreme sleepiness, and no urination indicate severe dehydration requiring urgent medical intervention."
            )
        ),

        "mod2-respiratory-infections" to listOf(
            ModuleQuizQuestion(
                id = "mod2-ri-q1",
                moduleId = "mod2",
                question = "How do respiratory infections most commonly spread?",
                options = listOf(
                    "Through droplets when someone coughs or sneezes",
                    "Through food",
                    "Through mosquito bites",
                    "Through touching water"
                ),
                correctAnswerIndex = 0,
                explanation = "Respiratory infections spread primarily through respiratory droplets produced when an infected person coughs or sneezes."
            ),
            ModuleQuizQuestion(
                id = "mod2-ri-q2",
                moduleId = "mod2",
                question = "Which is a way to help prevent respiratory infections?",
                options = listOf(
                    "Ignoring symptoms",
                    "Sharing utensils with others",
                    "Washing your hands regularly",
                    "Coughing into your hands"
                ),
                correctAnswerIndex = 2,
                explanation = "Regular handwashing is one of the most effective ways to prevent the spread of respiratory infections."
            ),
            ModuleQuizQuestion(
                id = "mod2-ri-q3",
                moduleId = "mod2",
                question = "Which is a sign of severe breathing difficulty in a child?",
                options = listOf(
                    "Drinking water frequently",
                    "Breathing very fast and becoming tired",
                    "Sleeping more than usual",
                    "Smiling less"
                ),
                correctAnswerIndex = 1,
                explanation = "Fast breathing with exhaustion indicates the child is working hard to breathe and may be in respiratory distress."
            ),
            ModuleQuizQuestion(
                id = "mod2-ri-q4",
                moduleId = "mod2",
                question = "What is chest wall indrawing?",
                options = listOf(
                    "When the stomach moves during breathing",
                    "When the chest expands during breathing",
                    "When the child coughs loudly",
                    "When the chest sucks in while breathing in"
                ),
                correctAnswerIndex = 3,
                explanation = "Chest wall indrawing occurs when the lower chest wall sucks inward during inhalation, indicating severe respiratory difficulty."
            ),
            ModuleQuizQuestion(
                id = "mod2-ri-q5",
                moduleId = "mod2",
                question = "Which sign means a child needs to go to the hospital immediately?",
                options = listOf("Runny nose", "Mild cough", "Sneezing", "Blue lips or tongue"),
                correctAnswerIndex = 3,
                explanation = "Blue lips or tongue (cyanosis) indicates dangerously low oxygen levels and is a medical emergency."
            )
        ),

        "mod2-asthma-pneumonia-tb" to listOf(
            ModuleQuizQuestion(
                id = "mod2-apt-q1",
                moduleId = "mod2",
                question = "Which is a common sign of pneumonia in children?",
                options = listOf("Night sweats", "Chest pulling in during breathing", "Itchy skin", "Swollen joints"),
                correctAnswerIndex = 1,
                explanation = "Chest indrawing (pulling in during breathing) is a key sign of pneumonia in children, indicating respiratory distress."
            ),
            ModuleQuizQuestion(
                id = "mod2-apt-q2",
                moduleId = "mod2",
                question = "A child has had a persistent cough for three weeks with weight loss and night sweats. What should be the primary concern?",
                options = listOf("Asthma exacerbation", "Pneumonia", "Tuberculosis", "Seasonal allergies"),
                correctAnswerIndex = 2,
                explanation = "A persistent cough lasting more than two weeks with weight loss and night sweats are hallmark symptoms of tuberculosis."
            ),
            ModuleQuizQuestion(
                id = "mod2-apt-q3",
                moduleId = "mod2",
                question = "Which vaccine helps protect children from one of the common causes of pneumonia?",
                options = listOf("BCG vaccine", "Polio vaccine", "Pneumococcal vaccine", "Measles vaccine"),
                correctAnswerIndex = 2,
                explanation = "The pneumococcal vaccine protects against Streptococcus pneumoniae, one of the leading bacterial causes of pneumonia."
            ),
            ModuleQuizQuestion(
                id = "mod2-apt-q4",
                moduleId = "mod2",
                question = "Asthma mainly affects which part of the body?",
                options = listOf("Kidneys", "Airways in the lungs", "Stomach", "Heart"),
                correctAnswerIndex = 1,
                explanation = "Asthma is a chronic condition that inflames and narrows the airways in the lungs, making breathing difficult."
            ),
            ModuleQuizQuestion(
                id = "mod2-apt-q5",
                moduleId = "mod2",
                question = "Which is a common trigger for asthma attacks?",
                options = listOf("Drinking water", "Dust and smoke", "Sunlight exposure", "Eating vegetables"),
                correctAnswerIndex = 1,
                explanation = "Dust, smoke, and other airborne irritants are common triggers that can cause asthma attacks by inflaming the airways."
            ),
            ModuleQuizQuestion(
                id = "mod2-apt-q6",
                moduleId = "mod2",
                question = "A child with asthma suddenly develops wheezing and difficulty breathing after exposure to cold air. What is this episode called?",
                options = listOf("Asthma infection", "Asthma attack", "Pneumonia episode", "Lung failure"),
                correctAnswerIndex = 1,
                explanation = "An asthma attack (or exacerbation) occurs when triggers cause sudden airway narrowing, leading to wheezing and difficulty breathing."
            ),
            ModuleQuizQuestion(
                id = "mod2-apt-q7",
                moduleId = "mod2",
                question = "What is the main purpose of inhalers for children with asthma?",
                options = listOf(
                    "To cure asthma permanently",
                    "To open the airways and relieve breathing symptoms",
                    "To prevent infections in the stomach",
                    "To increase lung size"
                ),
                correctAnswerIndex = 1,
                explanation = "Inhalers deliver medication directly to the airways to open them up and relieve breathing symptoms during or before an attack."
            ),
            ModuleQuizQuestion(
                id = "mod2-apt-q8",
                moduleId = "mod2",
                question = "Which symptom is most commonly associated with tuberculosis (TB)?",
                options = listOf(
                    "Cough lasting more than two weeks",
                    "Sudden vision loss",
                    "Severe stomach pain",
                    "Skin swelling"
                ),
                correctAnswerIndex = 0,
                explanation = "A persistent cough lasting more than two weeks is the most characteristic symptom of pulmonary tuberculosis."
            ),
            ModuleQuizQuestion(
                id = "mod2-apt-q9",
                moduleId = "mod2",
                question = "Why is it important to complete the full course of TB treatment?",
                options = listOf(
                    "To reduce coughing immediately",
                    "To prevent the infection from returning or causing complications",
                    "To avoid vaccination",
                    "To shorten hospital stays"
                ),
                correctAnswerIndex = 1,
                explanation = "Completing the full treatment course ensures all TB bacteria are killed, preventing relapse and the development of drug-resistant TB."
            ),
            ModuleQuizQuestion(
                id = "mod2-apt-q10",
                moduleId = "mod2",
                question = "Which combination of actions helps prevent severe respiratory illness in children?",
                options = listOf(
                    "Early recognition, vaccination, and proper treatment",
                    "Eating more sweets and resting",
                    "Avoiding sunlight and exercise",
                    "Drinking cold water frequently"
                ),
                correctAnswerIndex = 0,
                explanation = "Preventing severe respiratory illness requires early recognition of symptoms, vaccination, and appropriate medical treatment."
            )
        ),

        "mod2-antibiotics" to listOf(
            ModuleQuizQuestion(
                id = "mod2-ab-q1",
                moduleId = "mod2",
                question = "What do antibiotics treat?",
                options = listOf("Viral infections", "Fungal infections", "Bacterial infections", "Parasitic infections"),
                correctAnswerIndex = 2,
                explanation = "Antibiotics are specifically designed to treat bacterial infections. They are not effective against viruses, fungi, or parasites."
            ),
            ModuleQuizQuestion(
                id = "mod2-ab-q2",
                moduleId = "mod2",
                question = "Which would NOT warrant use of antibiotic treatment?",
                options = listOf("Cholera", "HIV", "Salmonella", "Bloody diarrhea"),
                correctAnswerIndex = 1,
                explanation = "HIV is a viral infection and cannot be treated with antibiotics. It requires antiretroviral therapy (ART)."
            ),
            ModuleQuizQuestion(
                id = "mod2-ab-q3",
                moduleId = "mod2",
                question = "Patients should stop their antibiotic treatment whenever they start to feel their symptoms ease.",
                options = listOf("True", "False"),
                correctAnswerIndex = 1,
                explanation = "False. Patients must complete the full course of antibiotics even if symptoms improve, to ensure all bacteria are eliminated and prevent resistance."
            ),
            ModuleQuizQuestion(
                id = "mod2-ab-q4",
                moduleId = "mod2",
                question = "Antibiotics can be saved and reused for future infections.",
                options = listOf("True", "False"),
                correctAnswerIndex = 1,
                explanation = "False. Antibiotics should never be saved or shared. Each prescription is specific to the current infection and patient."
            ),
            ModuleQuizQuestion(
                id = "mod2-ab-q5",
                moduleId = "mod2",
                question = "Which reaction would NOT require immediate medical attention?",
                options = listOf("Difficulty breathing", "Swelling", "Rashes", "Dry eyes"),
                correctAnswerIndex = 3,
                explanation = "Dry eyes are a minor side effect. Difficulty breathing, swelling, and rashes can indicate a severe allergic reaction requiring immediate care."
            )
        ),

        "mod2-malnutrition" to listOf(
            ModuleQuizQuestion(
                id = "mod2-mn-q1",
                moduleId = "mod2",
                question = "Which physical characteristic is specifically associated with Marasmus?",
                options = listOf(
                    "Abnormally enlarged internal organs",
                    "Thin, light-colored hair patches",
                    "A skin and bones appearance with easily visible ribs",
                    "A rounded, bloated face"
                ),
                correctAnswerIndex = 2,
                explanation = "Marasmus causes severe wasting, giving the patient a 'skin and bones' appearance with clearly visible ribs due to extreme calorie deficiency."
            ),
            ModuleQuizQuestion(
                id = "mod2-mn-q2",
                moduleId = "mod2",
                question = "Why can the appearance of a patient with Kwashiorkor be misleading?",
                options = listOf(
                    "The patient may not look thin due to fluid accumulation",
                    "The patient's hair grows thicker and darker",
                    "The patient's skin appears healthy and tanned",
                    "The patient often shows signs of high energy levels"
                ),
                correctAnswerIndex = 0,
                explanation = "Kwashiorkor causes edema (fluid retention) that can mask the underlying malnutrition, making patients appear less malnourished than they are."
            ),
            ModuleQuizQuestion(
                id = "mod2-mn-q3",
                moduleId = "mod2",
                question = "In kwashiorkor, what is the underlying cause of a 'big bloated tummy'?",
                options = listOf(
                    "Strong abdominal muscle development",
                    "An abnormally enlarged liver",
                    "Excessive intake of calorie-rich foods",
                    "Air trapped in the digestive tract"
                ),
                correctAnswerIndex = 1,
                explanation = "In kwashiorkor, protein deficiency causes the liver to enlarge (hepatomegaly) as it accumulates fat, contributing to abdominal distension."
            ),
            ModuleQuizQuestion(
                id = "mod2-mn-q4",
                moduleId = "mod2",
                question = "What is a necessary step in the treatment of both Marasmus and Kwashiorkor?",
                options = listOf(
                    "Diagnosing any underlying illnesses",
                    "Physical exercise to build muscle mass",
                    "Immediate restriction of all calorie intake",
                    "Surgery to reduce the size of the liver"
                ),
                correctAnswerIndex = 0,
                explanation = "Identifying and treating underlying illnesses (such as infections) is essential because they worsen malnutrition and hinder recovery."
            ),
            ModuleQuizQuestion(
                id = "mod2-mn-q5",
                moduleId = "mod2",
                question = "What type of nutritional support is recommended?",
                options = listOf(
                    "Special foods with extra energy and calories",
                    "Clear liquid diets to reduce stomach bloating",
                    "A diet low in carbohydrates and fats",
                    "High-fiber diets to improve digestion"
                ),
                correctAnswerIndex = 0,
                explanation = "Malnourished patients need energy-dense therapeutic foods to gradually restore weight and nutritional status."
            )
        ),

        // ════════════════════════════════════════════════════════════
        //  MODULE 3 — Chronic & Infectious Diseases
        // ════════════════════════════════════════════════════════════

        "mod3-chronic-illnesses" to listOf(
            ModuleQuizQuestion(
                id = "mod3-ci-q1",
                moduleId = "mod3",
                question = "What is a defining characteristic of Non-Communicable Diseases (NCDs)?",
                options = listOf(
                    "They are primarily spread through direct physical contact",
                    "They are characterized by sudden onset and rapid transmission",
                    "They typically resolve quickly without long-term management",
                    "They are characterized by long duration and generally slow progression"
                ),
                correctAnswerIndex = 3,
                explanation = "NCDs are chronic conditions that develop over time, last long, and require ongoing management rather than acute treatment."
            ),
            ModuleQuizQuestion(
                id = "mod3-ci-q2",
                moduleId = "mod3",
                question = "What is the recommended first step in addressing NCDs with undiagnosed community members?",
                options = listOf(
                    "Conducting surgical procedures in local community centers",
                    "Immediately forming specialized support groups",
                    "Discussing the importance of regular screening and signs of the conditions",
                    "Providing intensive psycho-social support before symptoms appear"
                ),
                correctAnswerIndex = 2,
                explanation = "Education about screening and early warning signs empowers community members to seek timely diagnosis and care."
            ),
            ModuleQuizQuestion(
                id = "mod3-ci-q3",
                moduleId = "mod3",
                question = "Which two conditions should be screened for within the community?",
                options = listOf(
                    "Cancer and respiratory failure",
                    "Asthma and skin disorders",
                    "Malaria and tuberculosis",
                    "Diabetes and hypertension"
                ),
                correctAnswerIndex = 3,
                explanation = "Diabetes and hypertension are the most prevalent NCDs that can be effectively screened for at the community level."
            ),
            ModuleQuizQuestion(
                id = "mod3-ci-q4",
                moduleId = "mod3",
                question = "What role do Community Health Extension Workers (CHEW) play?",
                options = listOf(
                    "They are consulted for conducting community screenings",
                    "They exclusively handle legal aspects of health record keeping",
                    "They are the primary surgeons at the nearest health facility",
                    "They develop new pharmaceutical treatments for NCDs"
                ),
                correctAnswerIndex = 0,
                explanation = "CHEWs serve as a link between the community and health facilities, conducting screenings and providing health education."
            ),
            ModuleQuizQuestion(
                id = "mod3-ci-q5",
                moduleId = "mod3",
                question = "Why is it important to maintain updated records for NCD-related health activities?",
                options = listOf(
                    "To ensure patients don't follow up more than once",
                    "To identify which community members to exclude from groups",
                    "To provide a basis for increasing local health service costs",
                    "To track and improve the overall health of the community"
                ),
                correctAnswerIndex = 3,
                explanation = "Accurate records allow health workers to monitor community health trends, track individual progress, and improve service delivery."
            )
        ),

        "mod3-diabetes" to listOf(
            ModuleQuizQuestion(
                id = "mod3-db-q1",
                moduleId = "mod3",
                question = "Which symptoms suggest a person may have high blood sugar (hyperglycemia)?",
                options = listOf(
                    "Shaking, sweating, and confusion",
                    "Excessive thirst, frequent urination, and blurred vision",
                    "Rapid weight gain and swollen joints",
                    "Severe headache and neck stiffness"
                ),
                correctAnswerIndex = 1,
                explanation = "Excessive thirst (polydipsia), frequent urination (polyuria), and blurred vision are classic signs of hyperglycemia caused by elevated blood glucose."
            ),
            ModuleQuizQuestion(
                id = "mod3-db-q2",
                moduleId = "mod3",
                question = "A diabetic patient becomes sweaty, shaky, and confused. What is the most likely cause and what should you do?",
                options = listOf(
                    "High blood sugar — withhold all food",
                    "Low blood sugar — give a sugary drink or food immediately",
                    "Dehydration — give water only",
                    "Medication overdose — induce vomiting"
                ),
                correctAnswerIndex = 1,
                explanation = "These are symptoms of hypoglycemia (low blood sugar). Giving a quick source of sugar can rapidly restore blood glucose and prevent loss of consciousness."
            ),
            ModuleQuizQuestion(
                id = "mod3-db-q3",
                moduleId = "mod3",
                question = "What is the role of insulin in the body?",
                options = listOf(
                    "It breaks down fat for energy storage",
                    "It allows glucose to enter cells so the body can use it for energy",
                    "It increases blood sugar levels after meals",
                    "It eliminates glucose through the kidneys"
                ),
                correctAnswerIndex = 1,
                explanation = "Insulin is a hormone produced by the pancreas that allows cells to absorb glucose from the blood and use it as energy."
            ),
            ModuleQuizQuestion(
                id = "mod3-db-q4",
                moduleId = "mod3",
                question = "Which lifestyle change is most important for managing diabetes?",
                options = listOf(
                    "Eating large meals twice a day",
                    "Avoiding all physical activity",
                    "Eating balanced meals regularly and staying physically active",
                    "Drinking fruit juice throughout the day"
                ),
                correctAnswerIndex = 2,
                explanation = "Regular balanced meals and physical activity help maintain stable blood sugar levels, which is the cornerstone of diabetes management."
            ),
            ModuleQuizQuestion(
                id = "mod3-db-q5",
                moduleId = "mod3",
                question = "When should a CHV refer a diabetic patient to a health facility urgently?",
                options = listOf(
                    "When the patient feels slightly tired after a meal",
                    "When the patient skips one dose of medication",
                    "When the patient is confused, unresponsive, or has very high or very low blood sugar",
                    "When the patient reports mild thirst in the afternoon"
                ),
                correctAnswerIndex = 2,
                explanation = "Confusion, unresponsiveness, or extreme blood sugar levels (very high or very low) are emergencies that require immediate medical attention."
            )
        ),

        "mod3-heart-disease" to listOf(
            ModuleQuizQuestion(
                id = "mod3-hd-q1",
                moduleId = "mod3",
                question = "What does the top number (systolic) in a blood pressure reading represent?",
                options = listOf(
                    "The pressure when the heart relaxes between beats",
                    "The pressure when the heart contracts and pumps blood",
                    "The average blood pressure over a day",
                    "The amount of blood pumped per heartbeat"
                ),
                correctAnswerIndex = 1,
                explanation = "Systolic pressure (top number) measures the force of blood against artery walls when the heart contracts and pumps blood."
            ),
            ModuleQuizQuestion(
                id = "mod3-hd-q2",
                moduleId = "mod3",
                question = "Which blood pressure reading is considered hypertension?",
                options = listOf("130/85 mmHg", "120/80 mmHg", "150/95 mmHg", "110/70 mmHg"),
                correctAnswerIndex = 2,
                explanation = "A reading of 150/95 mmHg is above the threshold for hypertension. Normal blood pressure is around 120/80 mmHg."
            ),
            ModuleQuizQuestion(
                id = "mod3-hd-q3",
                moduleId = "mod3",
                question = "Which is a warning sign of a stroke?",
                options = listOf(
                    "Sudden weakness or numbness on one side of the body",
                    "Cold sweats and dizziness",
                    "Pain in the left arm that spreads to the jaw",
                    "Persistent coughing"
                ),
                correctAnswerIndex = 0,
                explanation = "Sudden one-sided weakness or numbness is a hallmark warning sign of stroke, along with facial drooping and speech difficulty."
            ),
            ModuleQuizQuestion(
                id = "mod3-hd-q4",
                moduleId = "mod3",
                question = "What lifestyle change can help manage high blood pressure?",
                options = listOf(
                    "Increasing salt intake",
                    "Staying physically active",
                    "Avoiding fruits and vegetables",
                    "Skipping medications when feeling fine"
                ),
                correctAnswerIndex = 1,
                explanation = "Regular physical activity helps lower blood pressure naturally and is a key part of managing hypertension."
            ),
            ModuleQuizQuestion(
                id = "mod3-hd-q5",
                moduleId = "mod3",
                question = "If someone experiences chest pain spreading to the arm, jaw, or back, what should they do?",
                options = listOf(
                    "Wait and see if it goes away",
                    "Take an over-the-counter painkiller",
                    "Go to the nearest health facility immediately",
                    "Drink water and rest"
                ),
                correctAnswerIndex = 2,
                explanation = "Chest pain radiating to the arm, jaw, or back may indicate a heart attack, which is a medical emergency requiring immediate care."
            )
        ),

        "mod3-infectious-diseases" to listOf(
            ModuleQuizQuestion(
                id = "mod3-id-q1",
                moduleId = "mod3",
                question = "Infectious diseases are marked by their ability to spread from person to person.",
                options = listOf("True", "False"),
                correctAnswerIndex = 0,
                explanation = "True. Infectious diseases are caused by pathogens and can be transmitted from one person to another through various routes."
            ),
            ModuleQuizQuestion(
                id = "mod3-id-q2",
                moduleId = "mod3",
                question = "Which is NOT a microorganism that can cause an infectious disease?",
                options = listOf("Fungi", "Virus", "Worms", "Pollen"),
                correctAnswerIndex = 3,
                explanation = "Pollen is a plant substance that can cause allergies but is not a microorganism and does not cause infectious disease."
            ),
            ModuleQuizQuestion(
                id = "mod3-id-q3",
                moduleId = "mod3",
                question = "Which is an example of a fungal infection?",
                options = listOf("Ringworm", "Salmonella", "Head lice", "UTIs"),
                correctAnswerIndex = 0,
                explanation = "Ringworm is caused by a fungus, despite its name. Salmonella is bacterial, head lice are parasites, and UTIs are typically bacterial."
            ),
            ModuleQuizQuestion(
                id = "mod3-id-q4",
                moduleId = "mod3",
                question = "How can infectious diseases be spread?",
                options = listOf(
                    "Sharing utensils",
                    "Coughing",
                    "Bug bites",
                    "All of the above"
                ),
                correctAnswerIndex = 3,
                explanation = "Infectious diseases can spread through multiple routes including contaminated objects, respiratory droplets, and insect vectors."
            ),
            ModuleQuizQuestion(
                id = "mod3-id-q5",
                moduleId = "mod3",
                question = "Which demographic is most susceptible to getting an infectious disease?",
                options = listOf(
                    "Teenagers",
                    "People with weak immune systems",
                    "Physically active people",
                    "Office workers"
                ),
                correctAnswerIndex = 1,
                explanation = "People with weakened immune systems (young children, elderly, immunocompromised) are most vulnerable to infectious diseases."
            )
        ),

        "mod3-reproductive-tract" to listOf(
            ModuleQuizQuestion(
                id = "mod3-rt-q1",
                moduleId = "mod3",
                question = "What are reproductive tract infections (RTIs)?",
                options = listOf(
                    "Infections that only affect the stomach",
                    "Infections that affect the reproductive or genital organs",
                    "Infections that only affect the lungs",
                    "Infections that only occur in children"
                ),
                correctAnswerIndex = 1,
                explanation = "RTIs are infections of the reproductive tract organs, which can affect both men and women."
            ),
            ModuleQuizQuestion(
                id = "mod3-rt-q2",
                moduleId = "mod3",
                question = "Which is an example of a sexually transmitted infection (STI)?",
                options = listOf("Asthma", "Gonorrhea", "Diabetes", "Influenza"),
                correctAnswerIndex = 1,
                explanation = "Gonorrhea is a bacterial STI transmitted through sexual contact, affecting the reproductive organs."
            ),
            ModuleQuizQuestion(
                id = "mod3-rt-q3",
                moduleId = "mod3",
                question = "What is one possible symptom of gonorrhea?",
                options = listOf("Burning during urination", "Sneezing", "Hair loss", "Blurred vision"),
                correctAnswerIndex = 0,
                explanation = "Burning or pain during urination is a common symptom of gonorrhea, along with abnormal discharge."
            ),
            ModuleQuizQuestion(
                id = "mod3-rt-q4",
                moduleId = "mod3",
                question = "What is the name of the painless sore that can appear in the first stage of syphilis?",
                options = listOf("Lesion", "Chancre", "Blister", "Ulcer"),
                correctAnswerIndex = 1,
                explanation = "A chancre is the characteristic painless sore that appears at the site of infection during primary syphilis."
            ),
            ModuleQuizQuestion(
                id = "mod3-rt-q5",
                moduleId = "mod3",
                question = "Which is a way to help prevent RTIs?",
                options = listOf(
                    "Avoid drinking water",
                    "Using condoms during sexual activity",
                    "Skipping vaccines",
                    "Ignoring symptoms"
                ),
                correctAnswerIndex = 1,
                explanation = "Consistent and correct condom use is one of the most effective ways to prevent sexually transmitted RTIs."
            )
        ),

        "mod3-hiv-tb" to listOf(
            ModuleQuizQuestion(
                id = "mod3-ht-q1",
                moduleId = "mod3",
                question = "What does HIV attack in the human body?",
                options = listOf(
                    "The digestive system",
                    "The immune system",
                    "The nervous system",
                    "The skeletal system"
                ),
                correctAnswerIndex = 1,
                explanation = "HIV attacks the immune system, specifically CD4 T-cells, weakening the body's ability to fight infections."
            ),
            ModuleQuizQuestion(
                id = "mod3-ht-q2",
                moduleId = "mod3",
                question = "What is AIDS?",
                options = listOf(
                    "A different disease unrelated to HIV",
                    "The first stage of HIV infection",
                    "The most advanced stage of HIV",
                    "A bacterial infection"
                ),
                correctAnswerIndex = 2,
                explanation = "AIDS (Acquired Immunodeficiency Syndrome) is the most advanced stage of HIV infection, when the immune system is severely damaged."
            ),
            ModuleQuizQuestion(
                id = "mod3-ht-q3",
                moduleId = "mod3",
                question = "Which can spread HIV?",
                options = listOf(
                    "Sharing food",
                    "Skin contact",
                    "Infected body fluids such as blood or semen",
                    "Drinking contaminated water"
                ),
                correctAnswerIndex = 2,
                explanation = "HIV is transmitted through specific body fluids: blood, semen, vaginal fluids, and breast milk. It cannot spread through casual contact."
            ),
            ModuleQuizQuestion(
                id = "mod3-ht-q4",
                moduleId = "mod3",
                question = "How can HIV be detected?",
                options = listOf("Urine test", "Blood test", "X-ray", "Temperature test"),
                correctAnswerIndex = 1,
                explanation = "HIV is detected through blood tests that look for HIV antibodies or the virus itself."
            ),
            ModuleQuizQuestion(
                id = "mod3-ht-q5",
                moduleId = "mod3",
                question = "What is the main treatment that allows people with HIV to live long, healthy lives?",
                options = listOf("Antibiotics", "Vaccination", "Antiretroviral therapy (ART)", "Surgery"),
                correctAnswerIndex = 2,
                explanation = "ART suppresses HIV replication, allowing the immune system to recover and enabling people to live long, healthy lives."
            ),
            ModuleQuizQuestion(
                id = "mod3-ht-q6",
                moduleId = "mod3",
                question = "Tuberculosis (TB) mainly affects which part of the body?",
                options = listOf("Liver", "Lungs", "Heart", "Kidneys"),
                correctAnswerIndex = 1,
                explanation = "TB primarily affects the lungs (pulmonary TB), though it can also affect other parts of the body."
            ),
            ModuleQuizQuestion(
                id = "mod3-ht-q7",
                moduleId = "mod3",
                question = "How does Tuberculosis usually spread?",
                options = listOf(
                    "Through mosquito bites",
                    "Through contaminated food",
                    "Through the air when an infected person coughs or sneezes",
                    "Through skin contact"
                ),
                correctAnswerIndex = 2,
                explanation = "TB bacteria are airborne and spread when an infected person coughs, sneezes, or speaks, releasing droplets into the air."
            ),
            ModuleQuizQuestion(
                id = "mod3-ht-q8",
                moduleId = "mod3",
                question = "Which is a common symptom of TB?",
                options = listOf("Hair loss", "Night sweats", "Tooth pain", "Blurred vision"),
                correctAnswerIndex = 1,
                explanation = "Night sweats are a classic symptom of TB, along with persistent cough, weight loss, and fever."
            ),
            ModuleQuizQuestion(
                id = "mod3-ht-q9",
                moduleId = "mod3",
                question = "How long is the usual treatment period for TB with antibiotics?",
                options = listOf("1-2 weeks", "1 month", "4-6 months", "2 years"),
                correctAnswerIndex = 2,
                explanation = "Standard TB treatment takes 4-6 months of consistent antibiotic therapy to fully eliminate the bacteria."
            ),
            ModuleQuizQuestion(
                id = "mod3-ht-q10",
                moduleId = "mod3",
                question = "Why are people with HIV more likely to get TB?",
                options = listOf(
                    "TB spreads through water",
                    "HIV weakens the immune system",
                    "TB bacteria only infect HIV patients",
                    "HIV and TB are the same disease"
                ),
                correctAnswerIndex = 1,
                explanation = "HIV weakens the immune system, making it harder to fight off TB bacteria. HIV-positive individuals are much more likely to develop active TB."
            )
        ),

        // ════════════════════════════════════════════════════════════
        //  MODULE 4 — Maternal & Reproductive Health
        // ════════════════════════════════════════════════════════════

        "mod4-antenatal-care" to listOf(
            ModuleQuizQuestion(
                id = "mod4-ac-q1",
                moduleId = "mod4",
                question = "How many antenatal care visits are recommended as a minimum during pregnancy?",
                options = listOf("1", "4", "8", "12"),
                correctAnswerIndex = 2,
                explanation = "The WHO recommends at least 8 antenatal care contacts during pregnancy to ensure healthy outcomes for mother and baby."
            ),
            ModuleQuizQuestion(
                id = "mod4-ac-q2",
                moduleId = "mod4",
                question = "Which nutrient is especially important during pregnancy to prevent neural tube defects?",
                options = listOf("Vitamin C", "Iron", "Folic acid", "Calcium"),
                correctAnswerIndex = 2,
                explanation = "Folic acid is critical for the developing baby's brain and spinal cord. Taking it early in pregnancy helps prevent neural tube defects."
            ),
            ModuleQuizQuestion(
                id = "mod4-ac-q3",
                moduleId = "mod4",
                question = "Which of the following is a danger sign during pregnancy that requires immediate medical attention?",
                options = listOf(
                    "Mild morning nausea",
                    "Slight ankle swelling in the evening",
                    "Severe headache with blurred vision and swelling",
                    "Occasional back discomfort"
                ),
                correctAnswerIndex = 2,
                explanation = "Severe headache with blurred vision and swelling can indicate pre-eclampsia, a life-threatening condition requiring urgent care."
            ),
            ModuleQuizQuestion(
                id = "mod4-ac-q4",
                moduleId = "mod4",
                question = "Why are regular antenatal checkups important?",
                options = listOf(
                    "They are only needed if the mother feels unwell",
                    "They help detect problems early and keep mother and baby healthy",
                    "They are required only for first-time mothers",
                    "They replace the need for a skilled birth attendant"
                ),
                correctAnswerIndex = 1,
                explanation = "Regular checkups allow health workers to monitor the pregnancy, detect complications early, and provide timely interventions."
            ),
            ModuleQuizQuestion(
                id = "mod4-ac-q5",
                moduleId = "mod4",
                question = "What is typically checked during an antenatal visit?",
                options = listOf(
                    "Only the baby's heartbeat",
                    "Blood pressure, weight, urine, and the baby's growth",
                    "Only the mother's diet",
                    "Only the expected delivery date"
                ),
                correctAnswerIndex = 1,
                explanation = "Antenatal visits include checking blood pressure, weight, urine (for protein and sugar), and monitoring the baby's growth and position."
            )
        ),

        "mod4-pregnancy" to listOf(
            ModuleQuizQuestion(
                id = "mod4-pg-q1",
                moduleId = "mod4",
                question = "Which of the following is a normal symptom during early pregnancy?",
                options = listOf(
                    "Heavy vaginal bleeding",
                    "Mild nausea and breast tenderness",
                    "Severe abdominal pain",
                    "High fever with chills"
                ),
                correctAnswerIndex = 1,
                explanation = "Mild nausea (morning sickness) and breast tenderness are common, normal symptoms of early pregnancy caused by hormonal changes."
            ),
            ModuleQuizQuestion(
                id = "mod4-pg-q2",
                moduleId = "mod4",
                question = "Which symptom during pregnancy is a danger sign that needs immediate medical care?",
                options = listOf(
                    "Occasional mild heartburn",
                    "Feeling the baby move",
                    "Vaginal bleeding at any stage of pregnancy",
                    "Increased appetite"
                ),
                correctAnswerIndex = 2,
                explanation = "Vaginal bleeding during pregnancy can indicate serious complications like miscarriage, placenta previa, or placental abruption."
            ),
            ModuleQuizQuestion(
                id = "mod4-pg-q3",
                moduleId = "mod4",
                question = "A pregnant woman reports sudden severe swelling of the face and hands along with a headache. What should you suspect?",
                options = listOf(
                    "Normal water retention",
                    "Pre-eclampsia",
                    "Mild allergic reaction",
                    "Dehydration"
                ),
                correctAnswerIndex = 1,
                explanation = "Sudden severe swelling of the face and hands with headache are warning signs of pre-eclampsia, a serious pregnancy complication."
            ),
            ModuleQuizQuestion(
                id = "mod4-pg-q4",
                moduleId = "mod4",
                question = "When should a pregnant woman seek urgent care if she notices reduced fetal movement?",
                options = listOf(
                    "Only if it persists for a full week",
                    "Immediately, as it could indicate the baby is in distress",
                    "Only after trying to eat a large meal",
                    "Never — reduced movement is always normal"
                ),
                correctAnswerIndex = 1,
                explanation = "Reduced fetal movement can signal that the baby is not getting enough oxygen and requires prompt medical evaluation."
            ),
            ModuleQuizQuestion(
                id = "mod4-pg-q5",
                moduleId = "mod4",
                question = "Which of the following is NOT a danger sign during pregnancy?",
                options = listOf(
                    "Severe persistent vomiting",
                    "Feeling tired in the afternoon",
                    "Leaking fluid from the vagina before due date",
                    "Convulsions"
                ),
                correctAnswerIndex = 1,
                explanation = "Mild fatigue is a normal part of pregnancy. Severe vomiting, leaking fluid, and convulsions are all danger signs requiring medical attention."
            )
        ),

        "mod4-safe-delivery" to listOf(
            ModuleQuizQuestion(
                id = "mod4-sd-q1",
                moduleId = "mod4",
                question = "Where is the safest place for a woman to give birth?",
                options = listOf(
                    "At home without assistance",
                    "At a health facility with a skilled birth attendant",
                    "In a vehicle on the way to the hospital",
                    "At a neighbor's house"
                ),
                correctAnswerIndex = 1,
                explanation = "A health facility with skilled attendants has the equipment and expertise to handle complications, making it the safest option."
            ),
            ModuleQuizQuestion(
                id = "mod4-sd-q2",
                moduleId = "mod4",
                question = "What should be done immediately after a baby is born?",
                options = listOf(
                    "Bathe the baby in cold water",
                    "Dry the baby and place them skin-to-skin on the mother's chest",
                    "Separate the baby from the mother for observation",
                    "Feed the baby water"
                ),
                correctAnswerIndex = 1,
                explanation = "Drying the baby and placing them skin-to-skin prevents hypothermia, promotes bonding, and encourages early breastfeeding."
            ),
            ModuleQuizQuestion(
                id = "mod4-sd-q3",
                moduleId = "mod4",
                question = "Which is a danger sign in a newborn that requires immediate medical attention?",
                options = listOf(
                    "Sneezing occasionally",
                    "Sleeping most of the day",
                    "Difficulty breathing or bluish skin color",
                    "Hiccupping after feeding"
                ),
                correctAnswerIndex = 2,
                explanation = "Difficulty breathing or bluish skin indicates the newborn is not getting enough oxygen and needs urgent medical intervention."
            ),
            ModuleQuizQuestion(
                id = "mod4-sd-q4",
                moduleId = "mod4",
                question = "When should breastfeeding begin after birth?",
                options = listOf(
                    "After 24 hours",
                    "Within the first hour after birth",
                    "Only when the baby cries for food",
                    "After the baby has been bathed"
                ),
                correctAnswerIndex = 1,
                explanation = "Initiating breastfeeding within the first hour provides the baby with colostrum, which is rich in antibodies and essential nutrients."
            ),
            ModuleQuizQuestion(
                id = "mod4-sd-q5",
                moduleId = "mod4",
                question = "How should the umbilical cord stump be cared for?",
                options = listOf(
                    "Apply traditional herbs or ash to help it dry",
                    "Keep it clean and dry without applying anything",
                    "Cover it tightly with a bandage at all times",
                    "Pull it off as soon as possible"
                ),
                correctAnswerIndex = 1,
                explanation = "The cord stump should be kept clean and dry. Applying substances increases infection risk; it will fall off naturally."
            )
        ),

        "mod4-birth-spacing" to listOf(
            ModuleQuizQuestion(
                id = "mod4-bs-q1",
                moduleId = "mod4",
                question = "What is the minimum recommended interval between a live birth and the next pregnancy?",
                options = listOf("6 months", "12 months", "24 months", "36 months"),
                correctAnswerIndex = 2,
                explanation = "The WHO recommends waiting at least 24 months after a live birth before the next pregnancy to allow the mother's body to recover."
            ),
            ModuleQuizQuestion(
                id = "mod4-bs-q2",
                moduleId = "mod4",
                question = "What is a health risk of pregnancies that are too close together?",
                options = listOf(
                    "Increased immunity for the mother",
                    "Higher risk of preterm birth and low birth weight",
                    "Faster recovery from delivery",
                    "Improved fetal nutrition"
                ),
                correctAnswerIndex = 1,
                explanation = "Short birth intervals increase the risk of preterm birth, low birth weight, and maternal nutritional depletion."
            ),
            ModuleQuizQuestion(
                id = "mod4-bs-q3",
                moduleId = "mod4",
                question = "How does birth spacing benefit the existing child?",
                options = listOf(
                    "It has no effect on older children",
                    "It ensures the older child receives adequate nutrition and attention",
                    "It makes the older child grow faster",
                    "It reduces the need for breastfeeding"
                ),
                correctAnswerIndex = 1,
                explanation = "Adequate spacing ensures the older child continues to receive breastfeeding, nutrition, and parental attention during critical development."
            ),
            ModuleQuizQuestion(
                id = "mod4-bs-q4",
                moduleId = "mod4",
                question = "After a miscarriage or abortion, how long should a woman wait before becoming pregnant again?",
                options = listOf(
                    "No waiting period is needed",
                    "At least 6 months",
                    "At least 2 years",
                    "At least 5 years"
                ),
                correctAnswerIndex = 1,
                explanation = "The WHO recommends waiting at least 6 months after a miscarriage or abortion to allow the body to recover before another pregnancy."
            ),
            ModuleQuizQuestion(
                id = "mod4-bs-q5",
                moduleId = "mod4",
                question = "Which method can help achieve healthy birth spacing?",
                options = listOf(
                    "Using no family planning methods",
                    "Relying only on traditional remedies",
                    "Using modern contraceptive methods",
                    "Avoiding health facility visits"
                ),
                correctAnswerIndex = 2,
                explanation = "Modern contraceptive methods are the most reliable way to plan pregnancies and achieve healthy birth spacing."
            )
        ),

        "mod4-short-term-contraception" to listOf(
            ModuleQuizQuestion(
                id = "mod4-sc-q1",
                moduleId = "mod4",
                question = "Which of the following is a short-term contraceptive method?",
                options = listOf(
                    "Copper IUD",
                    "Hormonal implant",
                    "Combined oral contraceptive pills",
                    "Vasectomy"
                ),
                correctAnswerIndex = 2,
                explanation = "Combined oral contraceptive pills are a short-term method that must be taken daily and can be stopped at any time."
            ),
            ModuleQuizQuestion(
                id = "mod4-sc-q2",
                moduleId = "mod4",
                question = "How do hormonal contraceptive pills primarily prevent pregnancy?",
                options = listOf(
                    "By killing sperm on contact",
                    "By preventing ovulation so no egg is released",
                    "By thickening the uterine wall",
                    "By permanently blocking the fallopian tubes"
                ),
                correctAnswerIndex = 1,
                explanation = "Hormonal pills primarily work by suppressing ovulation. They also thicken cervical mucus, making it harder for sperm to reach an egg."
            ),
            ModuleQuizQuestion(
                id = "mod4-sc-q3",
                moduleId = "mod4",
                question = "Which contraceptive method also protects against sexually transmitted infections?",
                options = listOf(
                    "Oral contraceptive pills",
                    "Injectable contraceptives",
                    "Male and female condoms",
                    "Hormonal patches"
                ),
                correctAnswerIndex = 2,
                explanation = "Condoms are the only contraceptive method that provides dual protection against both pregnancy and STIs."
            ),
            ModuleQuizQuestion(
                id = "mod4-sc-q4",
                moduleId = "mod4",
                question = "What is a common side effect when first starting hormonal contraceptive pills?",
                options = listOf(
                    "Permanent weight gain",
                    "Mild nausea or headaches that usually improve over time",
                    "Immediate infertility",
                    "Severe allergic reaction"
                ),
                correctAnswerIndex = 1,
                explanation = "Mild nausea, headaches, or spotting are common initial side effects that typically resolve within the first few months of use."
            ),
            ModuleQuizQuestion(
                id = "mod4-sc-q5",
                moduleId = "mod4",
                question = "How often is the injectable contraceptive (e.g., Depo-Provera) typically given?",
                options = listOf(
                    "Once a week",
                    "Every 3 months",
                    "Once a year",
                    "Every 5 years"
                ),
                correctAnswerIndex = 1,
                explanation = "Injectable contraceptives like Depo-Provera are given every 3 months (12 weeks) for continuous pregnancy prevention."
            )
        ),

        "mod4-long-term-contraception" to listOf(
            ModuleQuizQuestion(
                id = "mod4-lc-q1",
                moduleId = "mod4",
                question = "Which of the following is a long-acting reversible contraceptive (LARC)?",
                options = listOf(
                    "Male condom",
                    "Daily birth control pill",
                    "Intrauterine device (IUD)",
                    "Emergency contraception"
                ),
                correctAnswerIndex = 2,
                explanation = "IUDs are long-acting reversible contraceptives that can provide protection for 5-12 years and can be removed when pregnancy is desired."
            ),
            ModuleQuizQuestion(
                id = "mod4-lc-q2",
                moduleId = "mod4",
                question = "How long can a hormonal implant (e.g., Implanon/Nexplanon) provide contraception?",
                options = listOf("6 months", "1 year", "3-5 years", "Permanently"),
                correctAnswerIndex = 2,
                explanation = "Hormonal implants are inserted under the skin of the upper arm and provide effective contraception for 3-5 years."
            ),
            ModuleQuizQuestion(
                id = "mod4-lc-q3",
                moduleId = "mod4",
                question = "Which contraceptive method is considered permanent?",
                options = listOf(
                    "Hormonal implant",
                    "IUD",
                    "Tubal ligation or vasectomy",
                    "Injectable contraceptive"
                ),
                correctAnswerIndex = 2,
                explanation = "Tubal ligation (for women) and vasectomy (for men) are surgical procedures intended as permanent sterilization."
            ),
            ModuleQuizQuestion(
                id = "mod4-lc-q4",
                moduleId = "mod4",
                question = "Who is a good candidate for long-term contraception?",
                options = listOf(
                    "Only women who have already had children",
                    "Women who want effective protection without daily action",
                    "Only women over age 35",
                    "Only women who cannot use condoms"
                ),
                correctAnswerIndex = 1,
                explanation = "LARCs are suitable for any woman who wants highly effective, low-maintenance contraception, regardless of age or parity."
            ),
            ModuleQuizQuestion(
                id = "mod4-lc-q5",
                moduleId = "mod4",
                question = "What is a key advantage of long-acting contraceptives over short-term methods?",
                options = listOf(
                    "They protect against STIs",
                    "They never have side effects",
                    "They do not require remembering daily or frequent actions",
                    "They are always free of charge"
                ),
                correctAnswerIndex = 2,
                explanation = "LARCs are highly effective because they work continuously without requiring the user to remember daily pills or frequent injections."
            )
        ),

        // ════════════════════════════════════════════════════════════
        //  MODULE 5 — First Aid & Emergency Response
        // ════════════════════════════════════════════════════════════

        "mod5-abcde-method" to listOf(
            ModuleQuizQuestion(
                id = "mod5-ab-q1",
                moduleId = "mod5",
                question = "In the ABCDE method, what does the 'A' stand for?",
                options = listOf("Assessment", "Airway", "Alertness", "Abdomen"),
                correctAnswerIndex = 1,
                explanation = "A stands for Airway — the first step is to check that the patient's airway is open and clear of obstructions."
            ),
            ModuleQuizQuestion(
                id = "mod5-ab-q2",
                moduleId = "mod5",
                question = "What should you check during the 'B' (Breathing) step?",
                options = listOf(
                    "Whether the person can walk",
                    "Whether the person is breathing and if breathing is adequate",
                    "Whether the person can swallow",
                    "Whether the person has a pulse"
                ),
                correctAnswerIndex = 1,
                explanation = "The Breathing step involves looking for chest rise, listening for breath sounds, and feeling for air exchange to assess adequacy of breathing."
            ),
            ModuleQuizQuestion(
                id = "mod5-ab-q3",
                moduleId = "mod5",
                question = "Why must the ABCDE steps be followed in order?",
                options = listOf(
                    "It makes documentation easier",
                    "Each step addresses the most life-threatening problems first",
                    "It helps the patient relax",
                    "It is only a suggestion, not a requirement"
                ),
                correctAnswerIndex = 1,
                explanation = "The ABCDE sequence prioritizes the most immediately life-threatening conditions first — an obstructed airway kills faster than bleeding."
            ),
            ModuleQuizQuestion(
                id = "mod5-ab-q4",
                moduleId = "mod5",
                question = "What does 'C' stand for in the ABCDE method?",
                options = listOf("Consciousness", "Circulation", "Compression", "Communication"),
                correctAnswerIndex = 1,
                explanation = "C stands for Circulation — assessing pulse, blood pressure, and looking for signs of significant bleeding or shock."
            ),
            ModuleQuizQuestion(
                id = "mod5-ab-q5",
                moduleId = "mod5",
                question = "What does the 'E' (Exposure) step involve?",
                options = listOf(
                    "Exposing the patient to sunlight for warmth",
                    "Examining the patient's entire body for injuries while preventing heat loss",
                    "Asking the patient to exercise",
                    "Exposing wounds to open air"
                ),
                correctAnswerIndex = 1,
                explanation = "Exposure means carefully examining the whole body for hidden injuries, while keeping the patient warm to prevent hypothermia."
            )
        ),

        "mod5-treating-bleeding" to listOf(
            ModuleQuizQuestion(
                id = "mod5-tb-q1",
                moduleId = "mod5",
                question = "What is the first action you should take for external bleeding?",
                options = listOf(
                    "Apply a tourniquet immediately",
                    "Apply direct pressure to the wound with a clean cloth",
                    "Pour water on the wound",
                    "Elevate the limb above the head"
                ),
                correctAnswerIndex = 1,
                explanation = "Direct pressure with a clean cloth is the first and most effective step to control most external bleeding."
            ),
            ModuleQuizQuestion(
                id = "mod5-tb-q2",
                moduleId = "mod5",
                question = "When should a tourniquet be used?",
                options = listOf(
                    "For any cut or scrape",
                    "Only when direct pressure fails to stop life-threatening limb bleeding",
                    "Before applying any direct pressure",
                    "Only by trained doctors in hospitals"
                ),
                correctAnswerIndex = 1,
                explanation = "Tourniquets are reserved for life-threatening bleeding from limbs when direct pressure alone cannot control the hemorrhage."
            ),
            ModuleQuizQuestion(
                id = "mod5-tb-q3",
                moduleId = "mod5",
                question = "Which type of bleeding is most dangerous and appears as bright red blood spurting from a wound?",
                options = listOf(
                    "Capillary bleeding",
                    "Venous bleeding",
                    "Arterial bleeding",
                    "Internal bleeding"
                ),
                correctAnswerIndex = 2,
                explanation = "Arterial bleeding produces bright red blood that spurts with each heartbeat and can lead to life-threatening blood loss very quickly."
            ),
            ModuleQuizQuestion(
                id = "mod5-tb-q4",
                moduleId = "mod5",
                question = "Which sign suggests a person may be bleeding internally?",
                options = listOf(
                    "A visible wound with blood",
                    "Pale, cool, and clammy skin with rapid pulse and no visible wound",
                    "A small bruise on the arm",
                    "Mild bleeding from a scratch"
                ),
                correctAnswerIndex = 1,
                explanation = "Signs of shock (pale, cool, clammy skin, rapid weak pulse) without a visible wound can indicate internal bleeding."
            ),
            ModuleQuizQuestion(
                id = "mod5-tb-q5",
                moduleId = "mod5",
                question = "If bleeding does not stop after applying direct pressure for several minutes, what should you do?",
                options = listOf(
                    "Remove the cloth and start over",
                    "Add more cloth on top without removing the first and seek medical help",
                    "Stop applying pressure and let it air dry",
                    "Apply ice directly to the open wound"
                ),
                correctAnswerIndex = 1,
                explanation = "Never remove a blood-soaked dressing — add more layers on top to maintain pressure, and seek urgent medical care."
            )
        ),

        "mod5-splint-bone" to listOf(
            ModuleQuizQuestion(
                id = "mod5-sb-q1",
                moduleId = "mod5",
                question = "Which of the following is a sign of a possible fracture?",
                options = listOf(
                    "Mild itching around a joint",
                    "Swelling, deformity, and inability to move the limb",
                    "A small painless bump",
                    "Numbness after sitting for a long time"
                ),
                correctAnswerIndex = 1,
                explanation = "Swelling, visible deformity, severe pain, and loss of function are key signs of a fracture."
            ),
            ModuleQuizQuestion(
                id = "mod5-sb-q2",
                moduleId = "mod5",
                question = "What is the main purpose of applying a splint?",
                options = listOf(
                    "To heal the fracture completely",
                    "To immobilize the injured area and prevent further damage",
                    "To apply medication to the injury",
                    "To allow the patient to walk normally"
                ),
                correctAnswerIndex = 1,
                explanation = "A splint immobilizes the injured limb, reducing pain, preventing further injury, and protecting blood vessels and nerves."
            ),
            ModuleQuizQuestion(
                id = "mod5-sb-q3",
                moduleId = "mod5",
                question = "When should you NOT move a patient with a suspected injury?",
                options = listOf(
                    "When they have a hand injury",
                    "When a spinal injury is suspected",
                    "When they have a broken finger",
                    "When they are fully conscious"
                ),
                correctAnswerIndex = 1,
                explanation = "A patient with a suspected spinal injury should not be moved unless in immediate danger, as movement can cause paralysis."
            ),
            ModuleQuizQuestion(
                id = "mod5-sb-q4",
                moduleId = "mod5",
                question = "When applying a splint, which joints should be immobilized?",
                options = listOf(
                    "Only the joint above the fracture",
                    "Only the joint below the fracture",
                    "The joints above and below the fracture",
                    "No joints need to be immobilized"
                ),
                correctAnswerIndex = 2,
                explanation = "Immobilizing the joints above and below the fracture site prevents movement at the break and reduces pain and further injury."
            ),
            ModuleQuizQuestion(
                id = "mod5-sb-q5",
                moduleId = "mod5",
                question = "What should you check after applying a splint?",
                options = listOf(
                    "Whether the patient can walk",
                    "Circulation beyond the splint — feeling, warmth, and color of fingers or toes",
                    "Whether the bone has healed",
                    "Whether the splint looks neat"
                ),
                correctAnswerIndex = 1,
                explanation = "After splinting, check circulation distal to the splint (sensation, warmth, color, pulse) to ensure it is not too tight."
            )
        ),

        "mod5-choking" to listOf(
            ModuleQuizQuestion(
                id = "mod5-ch-q1",
                moduleId = "mod5",
                question = "Which is a sign that a person is choking severely?",
                options = listOf(
                    "They are coughing loudly",
                    "They cannot speak, breathe, or cough and may clutch their throat",
                    "They are sneezing repeatedly",
                    "They have watery eyes"
                ),
                correctAnswerIndex = 1,
                explanation = "Severe choking means the airway is completely blocked — the person cannot speak, cough, or breathe and needs immediate help."
            ),
            ModuleQuizQuestion(
                id = "mod5-ch-q2",
                moduleId = "mod5",
                question = "What technique is used to help a choking adult or child over 1 year old?",
                options = listOf(
                    "Hitting them on the chest",
                    "Abdominal thrusts (Heimlich maneuver)",
                    "Blowing into their mouth",
                    "Giving them water to drink"
                ),
                correctAnswerIndex = 1,
                explanation = "Abdominal thrusts (Heimlich maneuver) create upward pressure to expel the object blocking the airway."
            ),
            ModuleQuizQuestion(
                id = "mod5-ch-q3",
                moduleId = "mod5",
                question = "How should you help a choking infant (under 1 year old)?",
                options = listOf(
                    "Perform abdominal thrusts",
                    "Give 5 back blows followed by 5 chest thrusts",
                    "Turn them upside down and shake them",
                    "Sweep the mouth with your finger"
                ),
                correctAnswerIndex = 1,
                explanation = "For infants, alternate 5 back blows (between shoulder blades) with 5 chest thrusts. Abdominal thrusts are not safe for infants."
            ),
            ModuleQuizQuestion(
                id = "mod5-ch-q4",
                moduleId = "mod5",
                question = "If a choking person can still cough forcefully, what should you do?",
                options = listOf(
                    "Immediately perform abdominal thrusts",
                    "Encourage them to keep coughing to try to clear the obstruction",
                    "Give them bread to push the object down",
                    "Lay them flat on the ground"
                ),
                correctAnswerIndex = 1,
                explanation = "A forceful cough means the airway is only partially blocked. Encouraging coughing may clear it naturally without intervention."
            ),
            ModuleQuizQuestion(
                id = "mod5-ch-q5",
                moduleId = "mod5",
                question = "What should you do if a choking person becomes unconscious?",
                options = listOf(
                    "Leave them and wait for help",
                    "Lower them to the ground, call for emergency help, and begin CPR",
                    "Prop them up in a sitting position",
                    "Try to give them water"
                ),
                correctAnswerIndex = 1,
                explanation = "If a choking person becomes unconscious, begin CPR immediately. Each time you open the airway, look for the object before giving breaths."
            )
        ),

        "mod5-burns-stings" to listOf(
            ModuleQuizQuestion(
                id = "mod5-bs-q1",
                moduleId = "mod5",
                question = "How are burns classified by severity?",
                options = listOf(
                    "Mild, moderate, and critical based on pain level only",
                    "First-degree (superficial), second-degree (partial thickness), and third-degree (full thickness)",
                    "Small, medium, and large based on area only",
                    "Wet and dry burns"
                ),
                correctAnswerIndex = 1,
                explanation = "Burns are classified by depth: first-degree affects the outer skin, second-degree damages deeper layers with blisters, and third-degree destroys all skin layers."
            ),
            ModuleQuizQuestion(
                id = "mod5-bs-q2",
                moduleId = "mod5",
                question = "What is the correct first aid for a minor burn?",
                options = listOf(
                    "Apply butter or oil to the burn",
                    "Cool the burn under clean, cool running water for at least 10 minutes",
                    "Pop any blisters immediately",
                    "Cover the burn with cotton wool"
                ),
                correctAnswerIndex = 1,
                explanation = "Cooling a burn under clean running water for at least 10 minutes reduces tissue damage and pain. Never apply butter, oil, or ice."
            ),
            ModuleQuizQuestion(
                id = "mod5-bs-q3",
                moduleId = "mod5",
                question = "Which action should you AVOID when treating a burn?",
                options = listOf(
                    "Removing the person from the heat source",
                    "Applying ice or very cold water directly to the burn",
                    "Covering the burn with a clean, non-stick dressing",
                    "Removing clothing near the burn if it is not stuck"
                ),
                correctAnswerIndex = 1,
                explanation = "Ice and very cold water can worsen tissue damage and cause frostbite on burned skin. Use cool (not cold) running water instead."
            ),
            ModuleQuizQuestion(
                id = "mod5-bs-q4",
                moduleId = "mod5",
                question = "What is the first step when treating a bee or wasp sting?",
                options = listOf(
                    "Squeeze the area to remove the venom",
                    "Remove the stinger by scraping it out with a flat object",
                    "Apply hot water to the sting",
                    "Ignore it and wait for it to go away"
                ),
                correctAnswerIndex = 1,
                explanation = "The stinger should be scraped out with a flat edge (like a card). Squeezing can push more venom into the skin."
            ),
            ModuleQuizQuestion(
                id = "mod5-bs-q5",
                moduleId = "mod5",
                question = "When should a person with a burn be taken to a health facility immediately?",
                options = listOf(
                    "For any first-degree burn",
                    "Only if the burn becomes infected days later",
                    "When the burn is large, on the face/hands/genitals, or the person has difficulty breathing",
                    "Only for electrical burns"
                ),
                correctAnswerIndex = 2,
                explanation = "Large burns, burns on critical areas (face, hands, joints, genitals), chemical/electrical burns, and burns with breathing difficulty all require urgent care."
            )
        ),

        // ════════════════════════════════════════════════════════════
        //  MODULE 6 — Infection Prevention & Control
        // ════════════════════════════════════════════════════════════

        "mod6-chain-of-infection" to listOf(
            ModuleQuizQuestion(
                id = "mod6-ci-q1",
                moduleId = "mod6",
                question = "How many links make up the chain of infection?",
                options = listOf("3", "4", "6", "8"),
                correctAnswerIndex = 2,
                explanation = "The chain of infection has 6 links: infectious agent, reservoir, portal of exit, mode of transmission, portal of entry, and susceptible host."
            ),
            ModuleQuizQuestion(
                id = "mod6-ci-q2",
                moduleId = "mod6",
                question = "What is the 'reservoir' in the chain of infection?",
                options = listOf(
                    "The person who gets sick",
                    "The place where the pathogen lives and multiplies",
                    "The method of transmission",
                    "The type of medication used"
                ),
                correctAnswerIndex = 1,
                explanation = "The reservoir is the environment where the pathogen normally lives and reproduces, such as humans, animals, water, or soil."
            ),
            ModuleQuizQuestion(
                id = "mod6-ci-q3",
                moduleId = "mod6",
                question = "What does it mean to 'break the chain of infection'?",
                options = listOf(
                    "Cure the infected person only",
                    "Interrupt one or more links so the infection cannot spread",
                    "Remove all bacteria from the environment",
                    "Vaccinate everyone in the world"
                ),
                correctAnswerIndex = 1,
                explanation = "Breaking any single link in the chain — through hygiene, isolation, vaccination, or other measures — can stop the infection from spreading."
            ),
            ModuleQuizQuestion(
                id = "mod6-ci-q4",
                moduleId = "mod6",
                question = "Which of the following is an example of a 'portal of exit'?",
                options = listOf(
                    "Eating contaminated food",
                    "A person sneezing or coughing",
                    "Touching a doorknob",
                    "Having a weak immune system"
                ),
                correctAnswerIndex = 1,
                explanation = "A portal of exit is how the pathogen leaves the reservoir. Sneezing and coughing release respiratory pathogens into the environment."
            ),
            ModuleQuizQuestion(
                id = "mod6-ci-q5",
                moduleId = "mod6",
                question = "Handwashing breaks which part of the chain of infection?",
                options = listOf(
                    "Infectious agent",
                    "Reservoir",
                    "Mode of transmission",
                    "Susceptible host"
                ),
                correctAnswerIndex = 2,
                explanation = "Handwashing removes pathogens from hands, interrupting the mode of transmission (contact) between the source and a new host."
            )
        ),

        "mod6-5fs-disease" to listOf(
            ModuleQuizQuestion(
                id = "mod6-5f-q1",
                moduleId = "mod6",
                question = "What do the 5 F's of disease transmission stand for?",
                options = listOf(
                    "Fever, Flu, Fungus, Fatigue, Frostbite",
                    "Fingers, Flies, Fields, Fluids, Food",
                    "Feces, Fire, Floods, Fog, Frost",
                    "Family, Friends, Facilities, Farming, Fishing"
                ),
                correctAnswerIndex = 1,
                explanation = "The 5 F's — Fingers, Flies, Fields, Fluids, and Food — describe common routes by which fecal-oral diseases spread."
            ),
            ModuleQuizQuestion(
                id = "mod6-5f-q2",
                moduleId = "mod6",
                question = "How do 'Fingers' contribute to disease transmission?",
                options = listOf(
                    "Through typing on keyboards",
                    "By carrying germs from contaminated surfaces or feces to the mouth",
                    "Through handshakes only",
                    "By pointing at infected areas"
                ),
                correctAnswerIndex = 1,
                explanation = "Unwashed hands can carry pathogens from contaminated surfaces, feces, or other sources directly to the mouth, causing infection."
            ),
            ModuleQuizQuestion(
                id = "mod6-5f-q3",
                moduleId = "mod6",
                question = "How do flies spread disease?",
                options = listOf(
                    "By biting people like mosquitoes",
                    "By landing on feces and then on food, carrying pathogens on their legs and bodies",
                    "By living inside the human body",
                    "By contaminating water sources directly"
                ),
                correctAnswerIndex = 1,
                explanation = "Flies land on feces and garbage, picking up pathogens on their bodies, then transfer them to food and surfaces they land on."
            ),
            ModuleQuizQuestion(
                id = "mod6-5f-q4",
                moduleId = "mod6",
                question = "What does 'Fields' refer to in the 5 F's?",
                options = listOf(
                    "Agricultural crop fields only",
                    "Areas contaminated with human or animal waste where people may have contact",
                    "Football or sports fields",
                    "Hospital wards"
                ),
                correctAnswerIndex = 1,
                explanation = "Fields refers to soil or ground contaminated with feces, where people (especially children) can come into contact with pathogens."
            ),
            ModuleQuizQuestion(
                id = "mod6-5f-q5",
                moduleId = "mod6",
                question = "Which practice helps prevent disease spread through the 5 F's?",
                options = listOf(
                    "Eating food without washing hands",
                    "Leaving food uncovered outdoors",
                    "Washing hands with soap, covering food, and using clean water",
                    "Disposing of waste in open fields"
                ),
                correctAnswerIndex = 2,
                explanation = "Handwashing, food hygiene, safe water, and proper sanitation break the fecal-oral transmission routes described by the 5 F's."
            )
        ),

        "mod6-standard-precautions" to listOf(
            ModuleQuizQuestion(
                id = "mod6-sp-q1",
                moduleId = "mod6",
                question = "Standard precautions should be applied to:",
                options = listOf(
                    "Only patients known to have infections",
                    "Only patients with visible wounds",
                    "All patients at all times, regardless of their diagnosis",
                    "Only patients in hospitals"
                ),
                correctAnswerIndex = 2,
                explanation = "Standard precautions apply to ALL patients in ALL settings, because many infections are not immediately apparent."
            ),
            ModuleQuizQuestion(
                id = "mod6-sp-q2",
                moduleId = "mod6",
                question = "What is the single most effective way to prevent the spread of infection?",
                options = listOf(
                    "Wearing a mask",
                    "Proper hand hygiene",
                    "Using antibiotics",
                    "Isolating all patients"
                ),
                correctAnswerIndex = 1,
                explanation = "Proper hand hygiene (washing with soap and water or using alcohol-based hand rub) is the most effective measure against infection spread."
            ),
            ModuleQuizQuestion(
                id = "mod6-sp-q3",
                moduleId = "mod6",
                question = "When should personal protective equipment (PPE) such as gloves be worn?",
                options = listOf(
                    "Only during surgical procedures",
                    "When there is a risk of contact with blood, body fluids, or contaminated surfaces",
                    "Only when a patient requests it",
                    "Only when treating HIV patients"
                ),
                correctAnswerIndex = 1,
                explanation = "PPE should be worn whenever there is risk of exposure to blood, body fluids, mucous membranes, or non-intact skin."
            ),
            ModuleQuizQuestion(
                id = "mod6-sp-q4",
                moduleId = "mod6",
                question = "How should used sharps (needles, blades) be disposed of?",
                options = listOf(
                    "Placed in a regular waste bin",
                    "Recapped and stored for reuse",
                    "Placed immediately in a puncture-resistant sharps container",
                    "Thrown on the ground for later collection"
                ),
                correctAnswerIndex = 2,
                explanation = "Used sharps must go directly into a puncture-resistant sharps container. Never recap, bend, or reuse needles."
            ),
            ModuleQuizQuestion(
                id = "mod6-sp-q5",
                moduleId = "mod6",
                question = "Which of the following is a component of standard precautions?",
                options = listOf(
                    "Sharing gloves between patients to save resources",
                    "Safe handling and disposal of waste",
                    "Reusing syringes after rinsing with water",
                    "Washing hands only after visible contamination"
                ),
                correctAnswerIndex = 1,
                explanation = "Safe waste handling and disposal is a core component of standard precautions, along with hand hygiene, PPE use, and safe injection practices."
            )
        ),

        "mod6-clinical-safety" to listOf(
            ModuleQuizQuestion(
                id = "mod6-cs-q1",
                moduleId = "mod6",
                question = "What is the correct way to handle a used needle?",
                options = listOf(
                    "Recap it carefully before disposal",
                    "Do not recap — place it directly into a sharps container",
                    "Bend it to prevent reuse",
                    "Leave it on the tray for later cleanup"
                ),
                correctAnswerIndex = 1,
                explanation = "Used needles should never be recapped, bent, or broken. They must be placed immediately in a sharps container to prevent needlestick injuries."
            ),
            ModuleQuizQuestion(
                id = "mod6-cs-q2",
                moduleId = "mod6",
                question = "When should a health worker wash their hands in a clinical setting?",
                options = listOf(
                    "Only after touching a patient",
                    "Before and after patient contact, after touching contaminated surfaces, and before procedures",
                    "Only when hands are visibly dirty",
                    "Once at the beginning and end of each shift"
                ),
                correctAnswerIndex = 1,
                explanation = "The WHO '5 Moments for Hand Hygiene' includes before and after patient contact, before procedures, after body fluid exposure, and after touching surroundings."
            ),
            ModuleQuizQuestion(
                id = "mod6-cs-q3",
                moduleId = "mod6",
                question = "Why is it important to segregate clinical waste into different containers?",
                options = listOf(
                    "To make the workspace look organized",
                    "To ensure hazardous waste is treated appropriately and prevent infection",
                    "To reduce the number of waste bins needed",
                    "To comply with decoration standards"
                ),
                correctAnswerIndex = 1,
                explanation = "Waste segregation ensures infectious and hazardous materials are handled and disposed of safely, protecting health workers and the community."
            ),
            ModuleQuizQuestion(
                id = "mod6-cs-q4",
                moduleId = "mod6",
                question = "Which of the following protects a health worker during patient care?",
                options = listOf(
                    "Wearing open-toed shoes",
                    "Using appropriate PPE such as gloves, gowns, and eye protection",
                    "Avoiding hand sanitizer to prevent dry skin",
                    "Working without gloves to improve dexterity"
                ),
                correctAnswerIndex = 1,
                explanation = "PPE creates a barrier between health workers and infectious materials, reducing the risk of exposure and cross-contamination."
            ),
            ModuleQuizQuestion(
                id = "mod6-cs-q5",
                moduleId = "mod6",
                question = "What should a health worker do immediately after a needlestick injury?",
                options = listOf(
                    "Ignore it if there is no bleeding",
                    "Wash the area with soap and water and report the incident immediately",
                    "Apply a bandage and continue working",
                    "Squeeze the wound hard to push out blood"
                ),
                correctAnswerIndex = 1,
                explanation = "After a needlestick injury, wash the site immediately with soap and water, then report it for risk assessment and possible post-exposure prophylaxis."
            )
        ),

        "mod6-unseen-shield" to listOf(
            ModuleQuizQuestion(
                id = "mod6-us-q1",
                moduleId = "mod6",
                question = "What is immunity?",
                options = listOf(
                    "The ability to perform physical exercise",
                    "The body's ability to resist and fight off infections",
                    "A type of medication",
                    "A surgical procedure to remove germs"
                ),
                correctAnswerIndex = 1,
                explanation = "Immunity is the body's natural defense system that recognizes and fights off harmful pathogens like bacteria and viruses."
            ),
            ModuleQuizQuestion(
                id = "mod6-us-q2",
                moduleId = "mod6",
                question = "How do vaccines work?",
                options = listOf(
                    "They cure existing infections immediately",
                    "They introduce a weakened or inactive form of a pathogen to train the immune system",
                    "They replace the immune system entirely",
                    "They remove all bacteria from the body"
                ),
                correctAnswerIndex = 1,
                explanation = "Vaccines expose the immune system to a safe version of a pathogen, teaching it to recognize and fight the real pathogen in the future."
            ),
            ModuleQuizQuestion(
                id = "mod6-us-q3",
                moduleId = "mod6",
                question = "What is herd immunity?",
                options = listOf(
                    "When only farmers are vaccinated",
                    "When enough people in a community are immune to slow or stop disease spread, protecting those who cannot be vaccinated",
                    "When animals are vaccinated instead of humans",
                    "When everyone takes antibiotics at the same time"
                ),
                correctAnswerIndex = 1,
                explanation = "Herd immunity occurs when a large portion of the community becomes immune (through vaccination or infection), indirectly protecting vulnerable individuals."
            ),
            ModuleQuizQuestion(
                id = "mod6-us-q4",
                moduleId = "mod6",
                question = "Are vaccines safe for children?",
                options = listOf(
                    "No, vaccines cause the diseases they are meant to prevent",
                    "Yes, vaccines are thoroughly tested and the benefits far outweigh the risks",
                    "Only some vaccines are safe, and parents should choose which ones",
                    "Vaccines are only safe for adults"
                ),
                correctAnswerIndex = 1,
                explanation = "Vaccines undergo rigorous testing and monitoring. They are safe and effective, with mild side effects far outweighed by the protection they provide."
            ),
            ModuleQuizQuestion(
                id = "mod6-us-q5",
                moduleId = "mod6",
                question = "Why is it important to follow the childhood vaccination schedule?",
                options = listOf(
                    "Children only need vaccines if they are sick",
                    "The schedule is designed to protect children when they are most vulnerable to specific diseases",
                    "It is optional and does not affect health outcomes",
                    "Vaccines only work if given after age 5"
                ),
                correctAnswerIndex = 1,
                explanation = "The vaccination schedule is timed to provide protection at the ages when children are most at risk for specific diseases."
            )
        )
    )

    val questions: List<ModuleQuizQuestion> get() = allQuizzes[moduleId] ?: emptyList()

    fun getCurrentQuestion(): ModuleQuizQuestion? = questions.getOrNull(_currentQuestionIndex.value)

    fun isLastQuestion(): Boolean = _currentQuestionIndex.value == questions.size - 1

    fun getTotalQuestions(): Int = questions.size

    fun getModuleTitle(): String = moduleTitles[moduleId] ?: "Module Quiz"

    fun selectAnswer(index: Int) {
        if (_showExplanation.value) return
        _selectedAnswer.value = index
        if (index == getCurrentQuestion()?.correctAnswerIndex) {
            _correctAnswers.value = _correctAnswers.value + 1
        }
        _showExplanation.value = true
    }

    fun nextQuestion() {
        _currentQuestionIndex.value++
        _selectedAnswer.value = null
        _showExplanation.value = false
    }

    fun finishQuiz() {
        viewModelScope.launch {
            try {
                progressDataStore.markQuizCompleted(moduleId)

                xpManager.addXP(
                    XpRewards.MODULE_COMPLETED,
                    "Completed quiz for module $moduleId"
                )

                videoDownloadManager.scheduleOffload(moduleId)
            } catch (e: Exception) {
                Log.d("ModuleQuizVM", "Local progress save failed: ${e.message}")
            }

            try {
                val token = tokenManager.getIdToken()
                if (token != null) {
                    val request = QuizSubmissionRequest(
                        videoId = "module-$moduleId",
                        totalQuestions = getTotalQuestions(),
                        correctAnswers = _correctAnswers.value,
                        incorrectAnswers = getTotalQuestions() - _correctAnswers.value,
                        answers = questions.mapIndexed { index, q ->
                            QuizAnswer(
                                questionId = q.id,
                                selectedAnswer = if (index == _currentQuestionIndex.value) _selectedAnswer.value ?: -1 else -1,
                                isCorrect = false
                            )
                        }
                    )
                    apiService.submitQuiz("Bearer $token", request)

                    val progressBody = mapOf<String, Any>(
                        "type" to "module_quiz_complete",
                        "itemId" to moduleId,
                        "score" to _correctAnswers.value
                    )
                    apiService.updateUserProgress("Bearer $token", progressBody)
                }
            } catch (e: Exception) {
                Log.d("ModuleQuizVM", "Quiz sync failed (progress saved locally): ${e.message}")
            }

            _isFinished.value = true
        }
    }
}
