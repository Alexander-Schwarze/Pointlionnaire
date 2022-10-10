package scripts

import java.io.File

// Compile with: kotlinc UpdateProperties.kt -include-runtime -d UpdateProperties_1-0-1.jar

const val latestVersion = "1.0.1"

val defaultPropertiesValues = listOf(
    // TwitchBotConfig properties
    mapOf(
        // Since Version: 1.0.0
        Pair("channel", "channelName"),
        Pair("only_mods", "false"),
        Pair("command_prefix", "#"),
        Pair("leave_emote", "peepoLeave"),
        Pair("arrive_emote", "peepoArrive"),
        Pair("explanation_emote", "PepoG"),
        Pair("blacklisted_users", ""),
        Pair("blacklist_emote", "FeelsOkayMan"),
        Pair("no_question_pending_text", "There is currently no question pending"),
        Pair("maximum_rolls", "1"),
        Pair("no_more_rerolls_text", "You already used all your rerolls! KEWK"),
        Pair("gg_emote", "PIGGIES"),
        Pair("amount_questions", "6"),
        Pair("total_interval_duration", "30"),
        Pair("answer_duration", "3"),
        Pair("attention_emote", "OOOO"),
        Pair("time_up_emote", "borpaHYPER2"),
        Pair("points_for_top_3", "3,2,1"),
        Pair("game_up_emote", "borpaSpin"),
        Pair("no_winner_emote", "FeelsOkayMan"),
        Pair("tie_emote", "PauseChamp"),
        Pair("tiebreaker_answer_duration", "0.1"),
        Pair("max_amount_tries", "3"),
        Pair("something_went_wrong_emote", "modCheck"),
    )
)

// This file holds all properties, that should exist for the latest version in all files.
// Executing it will write the properties with default values of the latest version.
fun main() {
    try {
        val propertiesFiles = listOf(
            File("data/twitchBotConfig.properties")
        )


        val outputString = mutableListOf<String>()

        outputString += "Checking for updates, latest verson: $latestVersion"

        defaultPropertiesValues.forEachIndexed { index, currentDefaultPropertiesMap ->
            val currentPropertiesFile = propertiesFiles[index]
            var currentContent = mutableListOf<String>()
            currentDefaultPropertiesMap.forEach { property ->
                if (!currentPropertiesFile.exists()) {
                    currentPropertiesFile.createNewFile()
                    outputString += "Created properties file ${currentPropertiesFile.name}"
                } else if (currentContent.isEmpty()) {
                    currentContent = currentPropertiesFile.readLines().toMutableList()
                }

                if (currentContent.find { it.contains(property.key) } == null) {
                    currentContent += (property.key + "=" + property.value)
                    outputString += "Added property: \"${property.key}\" with default value \"${property.value}\""
                }
            }

            currentPropertiesFile.writeText(currentContent.joinToString("\n"))
        }

        outputString += "Successfully updated properties!"
        Runtime.getRuntime().exec(
            arrayOf(
                "cmd", "/c", "start", "cmd", "/k",
                "echo ${outputString.joinToString("& echo.")}"
            )
        )
    } catch (e: Exception) {
        Runtime.getRuntime().exec(
            arrayOf(
                "cmd", "/c", "start", "cmd", "/k",
                "echo An error occured, see the exception here:& echo.${e.message}"
            )
        )
    }
}