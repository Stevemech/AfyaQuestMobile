package com.example.afyaquest.presentation.modulequiz

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.afyaquest.domain.model.ModuleQuizQuestion
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class ModuleQuizViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
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
        "8" to "Male Reproductive System",
        "9" to "Female Reproductive System",
        "10" to "Urinary System"
    )

    private val allQuizzes: Map<String, List<ModuleQuizQuestion>> = mapOf(
        "8" to listOf(
            ModuleQuizQuestion(
                id = "8_q1",
                moduleId = "8",
                question = "What is the primary function of the male reproductive system?",
                options = listOf(
                    "To regulate body temperature",
                    "To produce hormones only",
                    "To create and deliver sperm and produce male sex hormones",
                    "To filter blood"
                ),
                correctAnswerIndex = 2,
                explanation = "The male reproductive system's primary function is to create and deliver sperm and to produce male sex hormones such as testosterone."
            ),
            ModuleQuizQuestion(
                id = "8_q2",
                moduleId = "8",
                question = "Which structure allows both urine and semen to exit the body?",
                options = listOf(
                    "Vas deferens",
                    "Prostate",
                    "Urethra",
                    "Seminal vesicle"
                ),
                correctAnswerIndex = 2,
                explanation = "The urethra is a shared passageway for both urine and semen to exit the body."
            ),
            ModuleQuizQuestion(
                id = "8_q3",
                moduleId = "8",
                question = "The penis consists of which two main parts?",
                options = listOf(
                    "Shaft and scrotum",
                    "Shaft and glans penis",
                    "Glans and urethra",
                    "Shaft and vas deferens"
                ),
                correctAnswerIndex = 1,
                explanation = "The penis is made up of the shaft (the main body) and the glans penis (the rounded tip)."
            ),
            ModuleQuizQuestion(
                id = "8_q4",
                moduleId = "8",
                question = "What is the primary role of the testicles?",
                options = listOf(
                    "Produce urine",
                    "Store seminal fluid",
                    "Produce sperm and testosterone",
                    "Regulate blood pressure"
                ),
                correctAnswerIndex = 2,
                explanation = "The testicles are responsible for producing sperm and the hormone testosterone."
            ),
            ModuleQuizQuestion(
                id = "8_q5",
                moduleId = "8",
                question = "The vas deferens connects the testicles to the:",
                options = listOf(
                    "Prostate",
                    "Urethra",
                    "Bladder",
                    "Seminal vesicle"
                ),
                correctAnswerIndex = 1,
                explanation = "The vas deferens is a tube that carries sperm from the testicles toward the urethra for ejaculation."
            ),
            ModuleQuizQuestion(
                id = "8_q6",
                moduleId = "8",
                question = "The seminal vesicles mainly produce:",
                options = listOf(
                    "Testosterone",
                    "Urine",
                    "Fluid that nourishes and makes up semen",
                    "Sperm cells"
                ),
                correctAnswerIndex = 2,
                explanation = "The seminal vesicles produce a nutrient-rich fluid that makes up the bulk of semen and helps nourish sperm."
            ),
            ModuleQuizQuestion(
                id = "8_q7",
                moduleId = "8",
                question = "The prostate gland contributes to semen by producing:",
                options = listOf(
                    "Sperm",
                    "The remaining seminal fluid",
                    "Urine",
                    "Hormones only"
                ),
                correctAnswerIndex = 1,
                explanation = "The prostate gland produces a milky fluid that mixes with sperm and seminal vesicle fluid to form semen."
            ),
            ModuleQuizQuestion(
                id = "8_q8",
                moduleId = "8",
                question = "Which hormone is considered the most important androgen?",
                options = listOf(
                    "Estrogen",
                    "Progesterone",
                    "Testosterone",
                    "Insulin"
                ),
                correctAnswerIndex = 2,
                explanation = "Testosterone is the primary male sex hormone and the most important androgen, responsible for male characteristics and reproductive function."
            ),
            ModuleQuizQuestion(
                id = "8_q9",
                moduleId = "8",
                question = "Testosterone is responsible for which of the following?",
                options = listOf(
                    "Deep voice and increased muscle mass",
                    "Blood clotting",
                    "Kidney filtration",
                    "Digestive enzyme production"
                ),
                correctAnswerIndex = 0,
                explanation = "Testosterone drives male secondary sex characteristics including a deeper voice and increased muscle mass."
            ),
            ModuleQuizQuestion(
                id = "8_q10",
                moduleId = "8",
                question = "Which of the following is defined as difficulty getting or keeping an erection?",
                options = listOf(
                    "Infertility",
                    "STI",
                    "Erectile dysfunction (impotence)",
                    "Low sperm motility"
                ),
                correctAnswerIndex = 2,
                explanation = "Erectile dysfunction (also called impotence) is the inability to get or maintain an erection sufficient for sexual activity."
            )
        ),
        "9" to listOf(
            ModuleQuizQuestion(
                id = "9_q1",
                moduleId = "9",
                question = "Which of the following is a main component of the female reproductive system?",
                options = listOf(
                    "Prostate",
                    "Ovaries",
                    "Testes",
                    "Vas deferens"
                ),
                correctAnswerIndex = 1,
                explanation = "The ovaries are a main component of the female reproductive system. The prostate, testes, and vas deferens are all part of the male reproductive system."
            ),
            ModuleQuizQuestion(
                id = "9_q2",
                moduleId = "9",
                question = "What is the primary function of the ovaries?",
                options = listOf(
                    "Protect external openings",
                    "Produce eggs",
                    "Deliver the baby",
                    "Store sperm"
                ),
                correctAnswerIndex = 1,
                explanation = "The ovaries produce eggs (ova) and also produce hormones such as estrogen and progesterone."
            ),
            ModuleQuizQuestion(
                id = "9_q3",
                moduleId = "9",
                question = "The fallopian tubes connect the ovaries to the:",
                options = listOf(
                    "Vagina",
                    "Vulva",
                    "Uterus",
                    "Pituitary gland"
                ),
                correctAnswerIndex = 2,
                explanation = "The fallopian tubes transport eggs from the ovaries to the uterus, and are where fertilization typically occurs."
            ),
            ModuleQuizQuestion(
                id = "9_q4",
                moduleId = "9",
                question = "Where does a fetus develop during pregnancy?",
                options = listOf(
                    "Vagina",
                    "Ovaries",
                    "Fallopian tubes",
                    "Uterus"
                ),
                correctAnswerIndex = 3,
                explanation = "The uterus (womb) is the organ where a fertilized egg implants and the fetus develops throughout pregnancy."
            ),
            ModuleQuizQuestion(
                id = "9_q5",
                moduleId = "9",
                question = "The vagina serves which of the following functions?",
                options = listOf(
                    "Produces eggs",
                    "Protects external openings",
                    "Connects the uterus to the outside and allows sperm entry",
                    "Produces hormones"
                ),
                correctAnswerIndex = 2,
                explanation = "The vagina is the canal that connects the uterus to the outside of the body. It allows sperm entry, serves as the birth canal, and allows menstrual flow to exit."
            ),
            ModuleQuizQuestion(
                id = "9_q6",
                moduleId = "9",
                question = "The vulva is primarily responsible for:",
                options = listOf(
                    "Producing hormones",
                    "Protecting external openings from damage",
                    "Transporting eggs",
                    "Supporting fetal development"
                ),
                correctAnswerIndex = 1,
                explanation = "The vulva comprises the external genitalia and its primary role is to protect the vaginal and urethral openings from damage and infection."
            ),
            ModuleQuizQuestion(
                id = "9_q7",
                moduleId = "9",
                question = "Which glands control the menstrual cycle?",
                options = listOf(
                    "Thyroid and adrenal glands",
                    "Ovaries and pituitary gland",
                    "Pancreas and ovaries",
                    "Uterus and liver"
                ),
                correctAnswerIndex = 1,
                explanation = "The menstrual cycle is regulated by hormones from the ovaries (estrogen and progesterone) and the pituitary gland (FSH and LH)."
            ),
            ModuleQuizQuestion(
                id = "9_q8",
                moduleId = "9",
                question = "The average menstrual cycle lasts approximately:",
                options = listOf(
                    "14 days",
                    "21 days",
                    "28 days",
                    "35 days"
                ),
                correctAnswerIndex = 2,
                explanation = "The average menstrual cycle is approximately 28 days, though cycles between 21–35 days are considered normal."
            ),
            ModuleQuizQuestion(
                id = "9_q9",
                moduleId = "9",
                question = "An ectopic pregnancy most commonly occurs in the:",
                options = listOf(
                    "Uterus",
                    "Vagina",
                    "Fallopian tube",
                    "Vulva"
                ),
                correctAnswerIndex = 2,
                explanation = "An ectopic pregnancy occurs when a fertilized egg implants outside the uterus. The fallopian tube is the most common site."
            ),
            ModuleQuizQuestion(
                id = "9_q10",
                moduleId = "9",
                question = "Placenta previa is a condition in which:",
                options = listOf(
                    "The placenta separates from the uterus before delivery",
                    "The placenta blocks the neck of the uterus",
                    "The fetus develops outside the uterus",
                    "Endometrial tissue grows outside the uterus"
                ),
                correctAnswerIndex = 1,
                explanation = "Placenta previa occurs when the placenta partially or fully covers the cervix (neck of the uterus), which can cause complications during delivery."
            )
        ),
        "10" to listOf(
            ModuleQuizQuestion(
                id = "10_q1",
                moduleId = "10",
                question = "Which of the following is NOT an organ associated with the urinary system?",
                options = listOf(
                    "Kidney",
                    "Ureter",
                    "Liver",
                    "Urethra"
                ),
                correctAnswerIndex = 2,
                explanation = "The liver is a digestive organ, not part of the urinary system. The kidneys, ureters, and urethra are all key urinary system organs."
            ),
            ModuleQuizQuestion(
                id = "10_q2",
                moduleId = "10",
                question = "What is the significance of the brain in urination?",
                options = listOf(
                    "Signals the secretion of hormones that initiate the filtration of toxins from the blood",
                    "Coordinates the emptying and filling of the bladder",
                    "Aids the movement of urine into the bladder",
                    "Detects the buildup of toxins in the bloodstream"
                ),
                correctAnswerIndex = 1,
                explanation = "The brain receives signals from the bladder and coordinates when to empty or hold urine by controlling the muscles involved in urination."
            ),
            ModuleQuizQuestion(
                id = "10_q3",
                moduleId = "10",
                question = "Which of the following is a common condition arising from complications in the urinary system?",
                options = listOf(
                    "UTIs",
                    "Pneumonia",
                    "Hypertension",
                    "Seizure"
                ),
                correctAnswerIndex = 0,
                explanation = "Urinary tract infections (UTIs) are one of the most common conditions affecting the urinary system. Pneumonia, hypertension, and seizures are not urinary conditions."
            ),
            ModuleQuizQuestion(
                id = "10_q4",
                moduleId = "10",
                question = "What can cause a urinary tract infection?",
                options = listOf(
                    "A buildup of toxins in the bloodstream",
                    "Drinking too much water",
                    "Bacteria entering any part of the urinary tract",
                    "Sitting on the same toilet as someone with a urinary tract infection"
                ),
                correctAnswerIndex = 2,
                explanation = "UTIs are caused by bacteria entering the urinary tract, most commonly E. coli from the digestive tract. UTIs are not spread through toilet seats or caused by drinking water."
            ),
            ModuleQuizQuestion(
                id = "10_q5",
                moduleId = "10",
                question = "Which of the following is NOT a symptom associated with urinary tract infections?",
                options = listOf(
                    "Painful and burning sensations when urinating",
                    "Cloudy and bloody urine",
                    "More frequent and sudden urges to urinate",
                    "Thick and white discharge"
                ),
                correctAnswerIndex = 3,
                explanation = "Thick and white discharge is typically associated with a yeast infection, not a UTI. Burning urination, cloudy or bloody urine, and frequent urgency are classic UTI symptoms."
            ),
            ModuleQuizQuestion(
                id = "10_q6",
                moduleId = "10",
                question = "Urinary tract infections are more prevalent in females compared to males.",
                options = listOf(
                    "True",
                    "False"
                ),
                correctAnswerIndex = 0,
                explanation = "True. Females have a shorter urethra, which makes it easier for bacteria to reach the bladder, making UTIs significantly more common in women."
            ),
            ModuleQuizQuestion(
                id = "10_q7",
                moduleId = "10",
                question = "What are kidney stones?",
                options = listOf(
                    "Deposits of acidic toxins that damage the renal capsule of the kidney",
                    "Hard clumps of salts and minerals",
                    "Structures that aid in the filtering of toxins from the blood",
                    "Proteins that aid the formation of urine"
                ),
                correctAnswerIndex = 1,
                explanation = "Kidney stones are hard deposits made of salts and minerals that form inside the kidneys and can cause significant pain when passing through the urinary tract."
            ),
            ModuleQuizQuestion(
                id = "10_q8",
                moduleId = "10",
                question = "Which of the following is NOT a symptom associated with kidney stones?",
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
                id = "10_q9",
                moduleId = "10",
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
                id = "10_q10",
                moduleId = "10",
                question = "What is a way of maintaining a healthy urinary system?",
                options = listOf(
                    "Wiping from back to front",
                    "Regularly drinking water",
                    "Resisting urges to urinate",
                    "Spending prolonged periods on the toilet"
                ),
                correctAnswerIndex = 1,
                explanation = "Drinking water regularly flushes bacteria from the urinary tract. The other options can all contribute to urinary problems."
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
        _isFinished.value = true
    }
}
