package handler

import User

class UserHandler {

    companion object {
        val leaderBoard = mutableMapOf<User, /* points: */ Int>()
        var winner: String? = null // holds the ID of the user that had the most points
    }

    fun updateLeaderBoard(user: User, points: Int) {
        val currentPoints = leaderBoard[user]
        val newPoints = if(currentPoints != null){
            currentPoints + points
        } else {
            points
        }

        leaderBoard[user] = newPoints
    }

    fun getTop3Users(): List<User> {
        val sortedList = leaderBoard.toList().sortedByDescending { it.second }
        return listOf(sortedList[0].first, sortedList[1].first, sortedList[2].first)
    }

    fun setWinner() {
        winner = leaderBoard.toList().sortedByDescending { it.second }[0].first.userID
    }
}