package handler

import TwitchBotConfig
import json
import kotlinx.serialization.decodeFromString
import logger
import java.io.File

class RedeemHandler private constructor(
    private val redeems: Set<String>
) {
    companion object {
        val instance = run {
            val questionsFile = File("data/redeems.json")

            val redeems = if (!questionsFile.exists()) {
                questionsFile.createNewFile()
                logger.info("Redeems file created.")
                setOf()
            } else {
                try {
                    json.decodeFromString<Set<String>>(questionsFile.readText()).also { currentRedeem ->
                        logger.info("Existing redeems file found! Values: ${currentRedeem.joinToString(" | ")}")
                    }
                } catch (e: Exception) {
                    logger.warn("Error while reading redeems file. Initializing empty redeems", e)
                    setOf()
                }
            }

            if (redeems.isEmpty()) {
                // TODO: Remove this as soon as redeems can be added via UI
                logger.error("There are no existing redeems. As for version 1.0.0, you cannot set redeems in the UI. Thus they need to be added before app start in the json-file.")
                return@run null
            }

            RedeemHandler(redeems)
        }
    }

    private var usedRolls = 0

    fun popRandomRedeem(): String? {
        if (usedRolls > TwitchBotConfig.maximumRolls) {
            logger.info("No more rolls available. Aborting...")
            return null
        }

        return redeems.random().also { usedRolls++ }
    }

    fun resetRedeems() {
        logger.info("Resetting all previous redeem data")
        usedRolls = 0
    }
}