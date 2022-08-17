package handler

import User
import logger

class UserHandler {

    companion object {
        private val leaderBoard = mutableMapOf<User, /* points: */ Int>()
        var winner: User? = null // holds the ID of the user that had the most points
            private set

        fun updateLeaderBoard(user: User, points: Int) {
            val currentPoints = leaderBoard[user]
            val newPoints = if(currentPoints != null){
                currentPoints + points
            } else {
                points
            }

            leaderBoard[user] = newPoints
            logger.info("Leaderboard updated. New Leaderboard: $leaderBoard")
        }

        fun getTop3Users(): List<User> {
            val sortedList = leaderBoard.toList().sortedByDescending { it.second }
            return if(sortedList.isEmpty()) {
                listOf()
            } else {
                listOf(
                    sortedList[0].first,
                    try{
                        sortedList[1].first
                    } catch (e: Exception) {User("None", "-1")},
                    try{
                        sortedList[2].first
                    } catch (e: Exception) {User("None", "-2")}
                )
            }
        }

        fun getTieBreakerUsers(): List<User> {
            return if(leaderBoard.toList().isEmpty()) {
                listOf()
            } else {
                leaderBoard.toList().filter {
                    it.second == leaderBoard.toList().sortedByDescending { userPoints -> userPoints.second }[0].second
                }.map { it.first }
            }
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