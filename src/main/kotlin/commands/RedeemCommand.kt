package commands

import Command
import handler.RedeemHandler
import TwitchBotConfig
import handler.UserHandler
import logger

val redeemCommand: Command = Command(
    names = listOf("redeem", "r"),
    handler = {
        if(UserHandler.winner?.id != user.id) {
            return@Command
        }

        val redeem = RedeemHandler.instance?.popRandomRedeem()
        if(redeem == null) {
            logger.info("No more rerolls left")
            chat.sendMessage(TwitchBotConfig.channel, "${TwitchBotConfig.noMoreRerollsText} ${TwitchBotConfig.explanationEmote}")
        } else {
            chat.sendMessage(TwitchBotConfig.channel, "Your redeem is: $redeem ${TwitchBotConfig.ggEmote}. If you don't like it, you can try another time!")
        }
    }
)