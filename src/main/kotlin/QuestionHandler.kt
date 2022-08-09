import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.decodeFromString
import java.io.File

class QuestionHandler private constructor(
    private val questions: Set<Question>
) {
    companion object {
        val instance = run {
            val questionsFile = File("data/questions.json")

            val questions = if (!questionsFile.exists()) {
                questionsFile.createNewFile()
                logger.info("Questions file created.")
                setOf()
            } else {
                try {
                    json.decodeFromString<Set<Question>>(questionsFile.readText()).also { currentQuestionsData ->
                        logger.info("Existing questions file found! Values: ${currentQuestionsData.joinToString(" | ")}")
                    }
                } catch (e: Exception) {
                    logger.warn("Error while reading questions file. Initializing empty questions", e)
                    setOf()
                }
            }

            if(questions.isEmpty()){
                // TODO: Remove this as soon as questions can be added via UI
                logger.error("There are no existing questions. As for version 1.0.0, you cannot set questions in the UI. Thus they need to be added before app start in the json-file.")
                return@run
            }

            QuestionHandler(questions)
        }
    }

    val currentQuestionText = MutableStateFlow<String?>(TwitchBotConfig.noQuestionPendingText)

    private var askedQuestions: Set<Question> = setOf()

    fun popNextRandomQuestion(isLast2Questions: Boolean): Question {
        val newQuestion = if(isLast2Questions) {
            questions.filter{
                it.isLast2Questions && it !in askedQuestions
            }
        } else {
            questions.filter{
                !it.isLast2Questions && it !in askedQuestions
            }
        }.random().also {
            askedQuestions = askedQuestions + it
            currentQuestionText.value = it.questionText
        }

        return newQuestion
    }

    fun resetCurrentQuestion() {
        currentQuestionText.value = TwitchBotConfig.noQuestionPendingText
    }
}