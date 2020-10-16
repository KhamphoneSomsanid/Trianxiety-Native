package com.trianxiety.myapp.database

import org.threeten.bp.LocalDateTime
import java.io.Serializable

data class Game(val startTime: String = LocalDateTime.now().toString()) : Serializable {
    var id: String? = null
    var user: String? = null
    var endTime = LocalDateTime.now().toString()
    var duration = 0L // Indicates the duration when the last square was completed
    var squaresCompleted = 0 // Indicates how many squares were completed in the game

    val moves = ArrayList<Move>() // A list containing all the moves in the game
    val endSquares = ArrayList<EndSquare>() // A list containing the end-states of all the squares
    val triangleCrashes =
        ArrayList<Boolean>() // A list indicating all the triangle crashes in the game
}

data class Move(
    val type: Int = -1,
    val timeStamp: Long = -1,
    val first: Int = -1,
    val second: Int = -1,
    var completedSquares: Int = 0
) : Serializable

data class EndSquare(
    val topLeft: Int = -1,
    val topRight: Int = -1,
    val bottomLeft: Int = -1,
    val bottomRight: Int = -1
) : Serializable






