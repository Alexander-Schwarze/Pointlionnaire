package handler

import com.github.twitch4j.common.events.domain.EventUser
import logger

object UserHandler {
    private val leaderBoard = mutableMapOf<EventUser, /* points: */ Int>()
    var winner: EventUser? = null // holds the user that had the most points. Until the game is over, it stays null
        private set

    fun updateLeaderBoard(user: EventUser, points: Int) {
        val currentPoints = leaderBoard[user]
        val newPoints = if (currentPoints != null) {
            currentPoints + points
        } else {
            points
        }

        leaderBoard[user] = newPoints
        logger.info("Leaderboard updated. New Leaderboard: $leaderBoard")
    }

    fun getTop3Users(): List<EventUser> = leaderBoard.entries
        .sortedByDescending { it.value }
        .take(3)
        .map { it.key }

    fun getTieBreakerUsers(): List<EventUser> {
        return if (leaderBoard.toList().isEmpty()) {
            listOf()
        } else {
            leaderBoard.toList().filter {
                it.second == leaderBoard.toList().sortedByDescending { userPoints -> userPoints.second }[0].second
            }.map { it.first }
        }
    }

    val isTieBreaker get() = getTieBreakerUsers().size > 1

    fun setWinner() {
        winner = if (leaderBoard.isEmpty()) {
            null
        } else {
            leaderBoard.toList().sortedByDescending { it.second }[0].first
        }
    }

    fun resetUsers() {
        logger.info("Resetting all previous user data")
        leaderBoard.clear()
        winner = null
    }
}