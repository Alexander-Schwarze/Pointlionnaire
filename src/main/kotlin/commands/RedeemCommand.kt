package commands

import Command
import handler.RedeemHandler
import TwitchBotConfig
import handler.UserHandler

val redeemCommand: Command = Command(
    names = listOf("redeem", "r"),
    handler = {
        if(UserHandler.winner != user.id) {
            return@Command
        }

        val redeem = RedeemHandler.instance?.popRandomRedeem()
        if(redeem == null) {
            chat.sendMessage(TwitchBotConfig.channel, "${TwitchBotConfig.noMoreRerollsText} ${TwitchBotConfig.explanationEmote}")
        } else {
            chat.sendMessage(TwitchBotConfig.channel, "Your redeem is: $redeem ${TwitchBotConfig.ggEmote}")
        }
    }
)