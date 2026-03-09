package com.example.afyaquest.data.local

import com.example.afyaquest.domain.model.Difficulty
import com.example.afyaquest.domain.model.Question

/**
 * Local daily questions bundled in the APK.
 * Questions are organized by category and rotated daily based on the day of year.
 * Each day serves 3 questions from different categories.
 */
object LocalDailyQuestions {

    /**
     * Get 3 daily questions based on the current day.
     * Cycles through the question bank so each day has different questions.
     */
    fun getQuestionsForDay(dayOfYear: Int): List<Question> {
        val allQuestions = allQuestions
        val totalQuestions = allQuestions.size
        if (totalQuestions < 3) return allQuestions

        // Use day of year to rotate through questions
        val startIndex = (dayOfYear * 3) % totalQuestions
        val selected = mutableListOf<Question>()
        for (i in 0 until 3) {
            selected.add(allQuestions[(startIndex + i) % totalQuestions])
        }
        return selected
    }

    val allQuestions: List<Question> = listOf(
        // ---- HYGIENE ----
        Question(
            id = "local_hyg_1",
            question = "How long should you wash your hands with soap to effectively remove germs?",
            options = listOf("5 seconds", "10 seconds", "At least 20 seconds", "1 minute"),
            correctAnswerIndex = 2,
            explanation = "The WHO recommends washing hands with soap for at least 20 seconds to effectively remove germs and prevent disease transmission.",
            category = "Hygiene",
            difficulty = Difficulty.EASY,
            points = 10
        ),
        Question(
            id = "local_hyg_2",
            question = "Which of these is NOT one of the critical moments for handwashing?",
            options = listOf("Before preparing food", "After using the toilet", "After watching television", "Before eating"),
            correctAnswerIndex = 2,
            explanation = "Critical moments for handwashing include before preparing/eating food, after using the toilet, after coughing/sneezing, and after touching animals. Watching television is not a critical moment.",
            category = "Hygiene",
            difficulty = Difficulty.EASY,
            points = 10
        ),
        Question(
            id = "local_hyg_3",
            question = "What is the most effective way to make water safe for drinking in a household?",
            options = listOf("Letting it settle overnight", "Boiling it for at least 1 minute", "Adding sugar", "Filtering through cloth only"),
            correctAnswerIndex = 1,
            explanation = "Boiling water for at least 1 minute (3 minutes at high altitude) kills most disease-causing organisms and is the most reliable household method for making water safe.",
            category = "Hygiene",
            difficulty = Difficulty.MEDIUM,
            points = 20
        ),
        Question(
            id = "local_hyg_4",
            question = "What is the correct way to dispose of a child's feces?",
            options = listOf("Leave it in the open", "Throw it in the river", "Dispose of it in a latrine or toilet", "Bury it in the garden"),
            correctAnswerIndex = 2,
            explanation = "A child's feces should always be disposed of in a latrine or toilet. Open defecation contaminates water sources and spreads diseases like cholera and typhoid.",
            category = "Hygiene",
            difficulty = Difficulty.EASY,
            points = 10
        ),
        Question(
            id = "local_hyg_5",
            question = "Which disease is most commonly spread through contaminated water?",
            options = listOf("Malaria", "Cholera", "Tuberculosis", "HIV"),
            correctAnswerIndex = 1,
            explanation = "Cholera is primarily spread through contaminated water and food. It causes severe diarrhea and dehydration and can be fatal if untreated.",
            category = "Hygiene",
            difficulty = Difficulty.MEDIUM,
            points = 20
        ),

        // ---- NUTRITION ----
        Question(
            id = "local_nut_1",
            question = "What is the recommended duration for exclusive breastfeeding?",
            options = listOf("2 months", "4 months", "6 months", "12 months"),
            correctAnswerIndex = 2,
            explanation = "The WHO recommends exclusive breastfeeding for the first 6 months of life. After 6 months, complementary foods should be introduced while continuing breastfeeding up to 2 years or beyond.",
            category = "Nutrition",
            difficulty = Difficulty.EASY,
            points = 10
        ),
        Question(
            id = "local_nut_2",
            question = "Which nutrient deficiency causes night blindness in children?",
            options = listOf("Iron", "Vitamin A", "Calcium", "Vitamin C"),
            correctAnswerIndex = 1,
            explanation = "Vitamin A deficiency is the leading cause of preventable childhood blindness. Foods rich in Vitamin A include orange and yellow fruits/vegetables, dark leafy greens, liver, and eggs.",
            category = "Nutrition",
            difficulty = Difficulty.MEDIUM,
            points = 20
        ),
        Question(
            id = "local_nut_3",
            question = "What is a sign of severe acute malnutrition in children?",
            options = listOf("Being very tall", "Edema (swelling) of both feet", "Having a large appetite", "Gaining weight rapidly"),
            correctAnswerIndex = 1,
            explanation = "Bilateral pitting edema (swelling in both feet) is a sign of kwashiorkor, a form of severe acute malnutrition. Other signs include wasting, very low MUAC, and visible severe wasting.",
            category = "Nutrition",
            difficulty = Difficulty.HARD,
            points = 30
        ),
        Question(
            id = "local_nut_4",
            question = "At what age should complementary foods be introduced to a breastfed infant?",
            options = listOf("2 months", "4 months", "6 months", "9 months"),
            correctAnswerIndex = 2,
            explanation = "Complementary foods should be introduced at 6 months of age while continuing breastfeeding. Before 6 months, breast milk alone provides all the nutrients a baby needs.",
            category = "Nutrition",
            difficulty = Difficulty.EASY,
            points = 10
        ),
        Question(
            id = "local_nut_5",
            question = "Which of these is the best source of iron for preventing anemia?",
            options = listOf("White rice", "Dark leafy greens and liver", "Bananas", "Milk"),
            correctAnswerIndex = 1,
            explanation = "Dark leafy greens (such as spinach and kale) and organ meats like liver are excellent sources of iron. Iron deficiency anemia is one of the most common nutritional deficiencies worldwide.",
            category = "Nutrition",
            difficulty = Difficulty.MEDIUM,
            points = 20
        ),

        // ---- MATERNAL HEALTH ----
        Question(
            id = "local_mat_1",
            question = "How many antenatal care visits does the WHO recommend during pregnancy?",
            options = listOf("2 visits", "4 visits", "At least 8 visits", "1 visit"),
            correctAnswerIndex = 2,
            explanation = "The WHO recommends at least 8 antenatal care contacts during pregnancy to reduce perinatal mortality and improve women's experience of care.",
            category = "Maternal Health",
            difficulty = Difficulty.MEDIUM,
            points = 20
        ),
        Question(
            id = "local_mat_2",
            question = "Which of these is a danger sign during pregnancy that requires immediate medical attention?",
            options = listOf("Mild nausea in the morning", "Vaginal bleeding", "Increased appetite", "Mild back pain"),
            correctAnswerIndex = 1,
            explanation = "Vaginal bleeding during pregnancy is a danger sign that could indicate placenta previa, placental abruption, or miscarriage. Other danger signs include severe headache, blurred vision, convulsions, and high fever.",
            category = "Maternal Health",
            difficulty = Difficulty.EASY,
            points = 10
        ),
        Question(
            id = "local_mat_3",
            question = "What supplement should pregnant women take to prevent neural tube defects?",
            options = listOf("Vitamin C", "Calcium", "Folic acid", "Vitamin D"),
            correctAnswerIndex = 2,
            explanation = "Folic acid (folate) supplementation before and during early pregnancy significantly reduces the risk of neural tube defects like spina bifida in newborns.",
            category = "Maternal Health",
            difficulty = Difficulty.MEDIUM,
            points = 20
        ),
        Question(
            id = "local_mat_4",
            question = "What is the leading cause of maternal death globally?",
            options = listOf("Malaria", "Postpartum hemorrhage (severe bleeding)", "HIV", "Tuberculosis"),
            correctAnswerIndex = 1,
            explanation = "Postpartum hemorrhage (severe bleeding after childbirth) is the leading cause of maternal death worldwide. Active management of the third stage of labor helps prevent it.",
            category = "Maternal Health",
            difficulty = Difficulty.HARD,
            points = 30
        ),
        Question(
            id = "local_mat_5",
            question = "When should a newborn be put to the breast for the first time?",
            options = listOf("Within the first hour after birth", "After 6 hours", "The next day", "When the mother feels ready"),
            correctAnswerIndex = 0,
            explanation = "Early initiation of breastfeeding within the first hour of birth is crucial. Colostrum (first milk) is rich in antibodies and nutrients essential for the newborn's immune system.",
            category = "Maternal Health",
            difficulty = Difficulty.EASY,
            points = 10
        ),

        // ---- IMMUNIZATION ----
        Question(
            id = "local_imm_1",
            question = "At what age should a child receive the BCG vaccine?",
            options = listOf("At birth", "6 weeks", "9 months", "5 years"),
            correctAnswerIndex = 0,
            explanation = "The BCG (Bacillus Calmette-Guerin) vaccine against tuberculosis should be given at birth or as soon as possible after birth.",
            category = "Immunization",
            difficulty = Difficulty.EASY,
            points = 10
        ),
        Question(
            id = "local_imm_2",
            question = "What does the pentavalent vaccine protect against?",
            options = listOf(
                "Malaria, TB, cholera, typhoid, and polio",
                "Diphtheria, pertussis, tetanus, hepatitis B, and Hib",
                "Measles, mumps, rubella, chickenpox, and polio",
                "HIV, hepatitis A, hepatitis B, HPV, and meningitis"
            ),
            correctAnswerIndex = 1,
            explanation = "The pentavalent vaccine protects against 5 diseases: Diphtheria, Pertussis (whooping cough), Tetanus, Hepatitis B, and Haemophilus influenzae type b (Hib).",
            category = "Immunization",
            difficulty = Difficulty.MEDIUM,
            points = 20
        ),
        Question(
            id = "local_imm_3",
            question = "At what age is the measles vaccine typically given?",
            options = listOf("At birth", "6 weeks", "9 months", "2 years"),
            correctAnswerIndex = 2,
            explanation = "The first dose of measles vaccine is typically given at 9 months of age. A second dose is given at 18 months in many countries to ensure full protection.",
            category = "Immunization",
            difficulty = Difficulty.EASY,
            points = 10
        ),
        Question(
            id = "local_imm_4",
            question = "What is the correct way to store most vaccines?",
            options = listOf("At room temperature", "In a freezer at -20°C", "In a refrigerator at 2-8°C", "In direct sunlight"),
            correctAnswerIndex = 2,
            explanation = "Most vaccines must be stored in a refrigerator at 2-8°C to maintain their effectiveness. This is known as the cold chain. Some vaccines must never be frozen.",
            category = "Immunization",
            difficulty = Difficulty.MEDIUM,
            points = 20
        ),
        Question(
            id = "local_imm_5",
            question = "Which of these is a contraindication for vaccination?",
            options = listOf("Mild fever", "Previous severe allergic reaction to the vaccine", "Mild diarrhea", "Being underweight"),
            correctAnswerIndex = 1,
            explanation = "A previous severe allergic reaction (anaphylaxis) to a vaccine or its components is a true contraindication. Mild illness, diarrhea, or being underweight are NOT reasons to delay vaccination.",
            category = "Immunization",
            difficulty = Difficulty.HARD,
            points = 30
        ),

        // ---- DISEASE PREVENTION ----
        Question(
            id = "local_dis_1",
            question = "What is the main way malaria is transmitted?",
            options = listOf("Through contaminated water", "Through mosquito bites", "Through coughing", "Through physical contact"),
            correctAnswerIndex = 1,
            explanation = "Malaria is transmitted through the bite of an infected female Anopheles mosquito. Prevention includes sleeping under insecticide-treated nets and removing standing water.",
            category = "Disease Prevention",
            difficulty = Difficulty.EASY,
            points = 10
        ),
        Question(
            id = "local_dis_2",
            question = "Which of these is a symptom of tuberculosis (TB)?",
            options = listOf("Persistent cough lasting more than 2 weeks", "Skin rash", "Swollen joints", "Blurred vision"),
            correctAnswerIndex = 0,
            explanation = "A persistent cough lasting more than 2 weeks is the most common symptom of pulmonary TB. Other symptoms include night sweats, weight loss, fever, and coughing up blood.",
            category = "Disease Prevention",
            difficulty = Difficulty.MEDIUM,
            points = 20
        ),
        Question(
            id = "local_dis_3",
            question = "What is the most effective way to prevent mosquito-borne diseases?",
            options = listOf("Drinking boiled water", "Sleeping under an insecticide-treated net", "Taking antibiotics", "Eating garlic"),
            correctAnswerIndex = 1,
            explanation = "Sleeping under insecticide-treated bed nets (ITNs) is one of the most effective ways to prevent malaria and other mosquito-borne diseases. Nets should be retreated or replaced regularly.",
            category = "Disease Prevention",
            difficulty = Difficulty.EASY,
            points = 10
        ),
        Question(
            id = "local_dis_4",
            question = "What is the oral rehydration solution (ORS) used for?",
            options = listOf("Treating malaria", "Preventing dehydration from diarrhea", "Curing TB", "Treating pneumonia"),
            correctAnswerIndex = 1,
            explanation = "ORS is a mixture of clean water, salt, and sugar used to prevent and treat dehydration caused by diarrhea. It replaces lost fluids and electrolytes and saves millions of lives annually.",
            category = "Disease Prevention",
            difficulty = Difficulty.EASY,
            points = 10
        ),
        Question(
            id = "local_dis_5",
            question = "How is HIV primarily transmitted?",
            options = listOf("Through mosquito bites", "Through sharing utensils", "Through unprotected sexual contact and contaminated blood", "Through coughing"),
            correctAnswerIndex = 2,
            explanation = "HIV is primarily transmitted through unprotected sexual contact, contaminated blood (needles/transfusions), and from mother to child during pregnancy, birth, or breastfeeding.",
            category = "Disease Prevention",
            difficulty = Difficulty.MEDIUM,
            points = 20
        ),

        // ---- FIRST AID ----
        Question(
            id = "local_fa_1",
            question = "What is the first step when encountering an unconscious person?",
            options = listOf("Give them water", "Check for responsiveness and call for help", "Move them immediately", "Wait for them to wake up"),
            correctAnswerIndex = 1,
            explanation = "The first step is to check for responsiveness by tapping and shouting, then call for help/emergency services. Check for breathing and begin CPR if needed.",
            category = "First Aid",
            difficulty = Difficulty.EASY,
            points = 10
        ),
        Question(
            id = "local_fa_2",
            question = "What is the correct compression rate for adult CPR?",
            options = listOf("60 per minute", "80 per minute", "100-120 per minute", "150 per minute"),
            correctAnswerIndex = 2,
            explanation = "The correct compression rate for CPR is 100-120 compressions per minute. Compressions should be at least 2 inches (5 cm) deep for adults.",
            category = "First Aid",
            difficulty = Difficulty.MEDIUM,
            points = 20
        ),
        Question(
            id = "local_fa_3",
            question = "How should you treat a minor burn?",
            options = listOf("Apply butter or oil", "Cool with clean running water for at least 20 minutes", "Pop any blisters", "Apply ice directly"),
            correctAnswerIndex = 1,
            explanation = "Minor burns should be cooled with clean running water for at least 20 minutes. Do not apply butter, oil, toothpaste, or ice. Cover with a clean, non-stick dressing.",
            category = "First Aid",
            difficulty = Difficulty.EASY,
            points = 10
        ),
        Question(
            id = "local_fa_4",
            question = "What should you do if someone is choking and cannot cough, speak, or breathe?",
            options = listOf("Give them water", "Perform abdominal thrusts (Heimlich maneuver)", "Slap them on the face", "Tell them to lie down"),
            correctAnswerIndex = 1,
            explanation = "For a conscious choking adult, perform abdominal thrusts (Heimlich maneuver) by standing behind them and giving upward thrusts to the abdomen above the navel.",
            category = "First Aid",
            difficulty = Difficulty.MEDIUM,
            points = 20
        ),
        Question(
            id = "local_fa_5",
            question = "How should you control severe bleeding from a wound?",
            options = listOf("Remove the bandage frequently to check", "Apply firm, direct pressure with a clean cloth", "Apply a tourniquet as the first step", "Pour water on the wound"),
            correctAnswerIndex = 1,
            explanation = "Apply firm, direct pressure to the wound using a clean cloth or dressing. Maintain pressure continuously. Elevate the injured limb if possible. Only use a tourniquet as a last resort.",
            category = "First Aid",
            difficulty = Difficulty.EASY,
            points = 10
        ),

        // ---- CHILD CARE ----
        Question(
            id = "local_cc_1",
            question = "What is a sign of pneumonia in a child under 5?",
            options = listOf("Fast breathing or difficulty breathing", "Itchy skin", "Constipation", "Excessive thirst"),
            correctAnswerIndex = 0,
            explanation = "Fast breathing or difficulty breathing is a key sign of pneumonia in children under 5. Other signs include chest in-drawing, inability to feed, and fever. Pneumonia is a leading killer of children under 5.",
            category = "Child Care",
            difficulty = Difficulty.MEDIUM,
            points = 20
        ),
        Question(
            id = "local_cc_2",
            question = "What is the danger sign of dehydration in infants?",
            options = listOf("Wet diapers", "Sunken fontanelle (soft spot on head)", "Good appetite", "Active and playful behavior"),
            correctAnswerIndex = 1,
            explanation = "A sunken fontanelle (soft spot on the baby's head) is a sign of dehydration. Other signs include dry mouth, no tears when crying, decreased urination, and lethargy.",
            category = "Child Care",
            difficulty = Difficulty.MEDIUM,
            points = 20
        ),
        Question(
            id = "local_cc_3",
            question = "At what age should a child start receiving vitamin A supplementation?",
            options = listOf("At birth", "6 months", "1 year", "2 years"),
            correctAnswerIndex = 1,
            explanation = "Vitamin A supplementation should begin at 6 months of age and continue every 6 months until age 5. Vitamin A is crucial for immune function and preventing blindness.",
            category = "Child Care",
            difficulty = Difficulty.MEDIUM,
            points = 20
        ),
        Question(
            id = "local_cc_4",
            question = "What is the MUAC (Mid-Upper Arm Circumference) cutoff for severe acute malnutrition in children 6-59 months?",
            options = listOf("Less than 11.5 cm", "Less than 13.5 cm", "Less than 15 cm", "Less than 17 cm"),
            correctAnswerIndex = 0,
            explanation = "A MUAC less than 11.5 cm indicates severe acute malnutrition (SAM) in children aged 6-59 months. MUAC between 11.5-12.5 cm indicates moderate acute malnutrition (MAM).",
            category = "Child Care",
            difficulty = Difficulty.HARD,
            points = 30
        ),
        Question(
            id = "local_cc_5",
            question = "How should oral rehydration solution (ORS) be prepared?",
            options = listOf(
                "Mix the entire packet with 500 ml of water",
                "Mix the entire packet with 1 liter of clean water",
                "Mix half the packet with 2 liters of water",
                "Dissolve in hot tea"
            ),
            correctAnswerIndex = 1,
            explanation = "One ORS packet should be dissolved in 1 liter (1000 ml) of clean water. The solution should be used within 24 hours. Do not boil the solution or add sugar.",
            category = "Child Care",
            difficulty = Difficulty.EASY,
            points = 10
        ),

        // ---- COMMUNITY HEALTH ----
        Question(
            id = "local_ch_1",
            question = "What is the purpose of a community health worker's home visit?",
            options = listOf(
                "To collect rent",
                "To assess health, provide education, and refer when needed",
                "To sell medicine",
                "To conduct surgery"
            ),
            correctAnswerIndex = 1,
            explanation = "Community health workers conduct home visits to assess the health of family members, provide health education, identify danger signs, make referrals, and follow up on treatments.",
            category = "Community Health",
            difficulty = Difficulty.EASY,
            points = 10
        ),
        Question(
            id = "local_ch_2",
            question = "What does the term 'referral' mean in community health?",
            options = listOf(
                "Sending a patient home",
                "Directing a patient to a higher-level health facility for additional care",
                "Prescribing medication",
                "Performing surgery in the community"
            ),
            correctAnswerIndex = 1,
            explanation = "A referral means directing a patient to a higher-level health facility or specialist for care that cannot be provided at the community level. Timely referrals can save lives.",
            category = "Community Health",
            difficulty = Difficulty.EASY,
            points = 10
        ),
        Question(
            id = "local_ch_3",
            question = "Which of these is an important skill for a Community Health Volunteer?",
            options = listOf("Surgery", "Active listening and health education", "Prescribing drugs", "Operating X-ray machines"),
            correctAnswerIndex = 1,
            explanation = "Active listening and health education are essential skills for Community Health Volunteers. CHVs serve as the link between the community and the health system.",
            category = "Community Health",
            difficulty = Difficulty.EASY,
            points = 10
        ),
        Question(
            id = "local_ch_4",
            question = "What is the purpose of growth monitoring in children?",
            options = listOf(
                "To measure how tall a child will become as an adult",
                "To track a child's growth and detect malnutrition early",
                "To determine a child's intelligence",
                "To decide what school they should attend"
            ),
            correctAnswerIndex = 1,
            explanation = "Growth monitoring tracks a child's weight and height over time to detect growth faltering or malnutrition early. This allows for timely intervention and nutritional support.",
            category = "Community Health",
            difficulty = Difficulty.MEDIUM,
            points = 20
        ),
        Question(
            id = "local_ch_5",
            question = "What should a CHV do when they identify a danger sign in a patient?",
            options = listOf(
                "Wait and observe for a week",
                "Refer the patient immediately to a health facility",
                "Prescribe medication",
                "Ignore it if the patient feels fine"
            ),
            correctAnswerIndex = 1,
            explanation = "When a CHV identifies danger signs, they should refer the patient immediately to the nearest health facility. Delay in referral can lead to complications or death.",
            category = "Community Health",
            difficulty = Difficulty.EASY,
            points = 10
        ),

        // ---- REPRODUCTIVE HEALTH ----
        Question(
            id = "local_rh_1",
            question = "What is family planning?",
            options = listOf(
                "Deciding how many rooms a house should have",
                "The ability to decide the number and spacing of children using contraceptive methods",
                "Planning family reunions",
                "Deciding which school children attend"
            ),
            correctAnswerIndex = 1,
            explanation = "Family planning allows individuals and couples to decide the number and spacing of their children through the use of contraceptive methods and information.",
            category = "Reproductive Health",
            difficulty = Difficulty.EASY,
            points = 10
        ),
        Question(
            id = "local_rh_2",
            question = "Which of these is a long-acting reversible contraceptive method?",
            options = listOf("Condoms", "Birth control pills", "Intrauterine device (IUD)", "Emergency contraception"),
            correctAnswerIndex = 2,
            explanation = "An IUD (intrauterine device) is a long-acting reversible contraceptive that can last 5-12 years depending on the type. Other long-acting methods include implants.",
            category = "Reproductive Health",
            difficulty = Difficulty.MEDIUM,
            points = 20
        ),
        Question(
            id = "local_rh_3",
            question = "What is the most effective way to prevent sexually transmitted infections (STIs)?",
            options = listOf("Taking antibiotics regularly", "Correct and consistent use of condoms", "Washing after intercourse", "Using birth control pills"),
            correctAnswerIndex = 1,
            explanation = "Correct and consistent use of condoms is the most effective way to prevent STIs besides abstinence. Birth control pills prevent pregnancy but do not protect against STIs.",
            category = "Reproductive Health",
            difficulty = Difficulty.EASY,
            points = 10
        ),

        // ---- EMERGENCY ----
        Question(
            id = "local_em_1",
            question = "What is the recovery position used for?",
            options = listOf(
                "A sleeping position for comfort",
                "Placing an unconscious but breathing person on their side to keep their airway clear",
                "A position for doing push-ups",
                "A position for administering medication"
            ),
            correctAnswerIndex = 1,
            explanation = "The recovery position is used for unconscious people who are breathing normally. It keeps the airway clear and prevents choking on vomit or fluids.",
            category = "Emergency",
            difficulty = Difficulty.EASY,
            points = 10
        ),
        Question(
            id = "local_em_2",
            question = "What are the signs of a stroke?",
            options = listOf(
                "Itchy skin and sneezing",
                "Face drooping, arm weakness, speech difficulty",
                "Stomach pain and nausea",
                "Joint pain and swelling"
            ),
            correctAnswerIndex = 1,
            explanation = "Remember FAST: Face drooping, Arm weakness, Speech difficulty, Time to call emergency services. Stroke requires immediate medical attention to prevent brain damage.",
            category = "Emergency",
            difficulty = Difficulty.MEDIUM,
            points = 20
        ),
        Question(
            id = "local_em_3",
            question = "What should you do if someone is having a seizure?",
            options = listOf(
                "Put something in their mouth",
                "Hold them down firmly",
                "Clear the area of hard objects and protect their head",
                "Pour water on their face"
            ),
            correctAnswerIndex = 2,
            explanation = "During a seizure, clear the area of hard or sharp objects, protect the person's head, and do NOT put anything in their mouth or restrain them. Turn them on their side after the seizure stops.",
            category = "Emergency",
            difficulty = Difficulty.MEDIUM,
            points = 20
        )
    )
}
