package commands

import Command
import handler.RedeemHandler
import TwitchBotConfig
import handler.UserHandler
import logger

val redeemCommand: Command = Command(
    names = listOf("redeem", "r"),
    handler = {
        if (UserHandler.winner?.id != user.id) {
            return@Command
        }

        if (RedeemHandler.instance.exceededMaximumRolls()) {
            logger.info("No more rerolls left")
            chat.sendMessage(
                TwitchBotConfig.channel,
                "${TwitchBotConfig.noMoreRerollsText} ${TwitchBotConfig.explanationEmote}"
            )
        } else {
            chat.sendMessage(
                TwitchBotConfig.channel,
                "Your redeem is: ${RedeemHandler.instance.popRandomRedeem()} ${TwitchBotConfig.ggEmote}" +
                        if(!RedeemHandler.instance.exceededMaximumRolls()) {
                            " If you don't like it, you can try another time!"
                        } else {
                            ""
                        }
            )
        }
    }
)