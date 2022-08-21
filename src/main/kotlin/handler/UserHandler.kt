package handler

import com.github.twitch4j.common.events.domain.EventUser
import logger

class UserHandler {

    companion object {
        private val leaderBoard = mutableMapOf<EventUser, /* points: */ Int>()
        var winner: EventUser? = null // holds the ID of the user that had the most points
            private set
        val tieBreakUsers = mutableListOf<EventUser>()

        fun updateLeaderBoard(user: EventUser, points: Int) {
            val currentPoints = leaderBoard[user]
            val newPoints = if(currentPoints != null){
                currentPoints + points
            } else {
                points
            }

            leaderBoard[user] = newPoints
            logger.info("Leaderboard updated. New Leaderboard: $leaderBoard")
        }

        fun getTop3Users(): List<EventUser?> {
            val sortedList = leaderBoard.toList().sortedByDescending { it.second }
            return if(sortedList.isEmpty()) {
                listOf()
            } else {
                listOf(
                    sortedList[0].first,
                    try{
                        sortedList[1].first
                    } catch (e: Exception) {null},
                    try{
                        sortedList[2].first
                    } catch (e: Exception) {null}
                )
            }
        }

        fun getTieBreakerUser(): List<EventUser> {
            return if(leaderBoard.toList().isEmpty()) {
                listOf()
            } else {
                leaderBoard.toList().filter {
                    it.second == leaderBoard.toList().sortedByDescending { userPoints -> userPoints.second }[0].second
                }.map { it.first }
            }
        }

        fun setTieBreakerUser() {
            tieBreakUsers.addAll(getTieBreakerUser())
        }

        fun isTieBreaker(): Boolean {
            return tieBreakUsers.isNotEmpty()
        }

        fun setWinner() {
            winner = if(leaderBoard.toList().isEmpty()) {
                null
            } else {
                leaderBoard.toList().sortedByDescending { it.second }[0].first
            }
        }
    }
}