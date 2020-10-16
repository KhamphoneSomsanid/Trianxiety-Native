package com.trianxiety.myapp.viewmodel

import android.graphics.Color
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.trianxiety.myapp.database.EndSquare
import com.trianxiety.myapp.database.Game
import com.trianxiety.myapp.database.Move
import java.util.*
import kotlin.collections.ArrayList

class MAViewModel : ViewModel() {

    val CLOCKWISE_ROTATION = 0
    val COUNTER_CLOCKWISE_ROTATION = 1
    val ROTATION_INVERT = 2
    val DOUBLE_ROTATION = 3
    val SQUARE_SWAP = 4
    val TRIANGLE_SWAP = 5
    val TIME_UP = 6

    private var _gameRank = MutableLiveData<GameRank>()
    val gameRank: LiveData<GameRank> = _gameRank

    private val MAIN_MODE = 0
    private val GAME_MODE = 1

    val triangles = ArrayList<Triangle>()
    val squares = ArrayList<Square>()
    val borderSquares = ArrayList<BorderSquare>()
    private val colors = ArrayList<Int>()

    val gameExample = Game()
    var gameActive: Game = gameExample

    var rewindState = -1
    var gameSelected: Game = gameExample
        set(value) {
            field = value
            rewindState = -1
        }

    private val NEITHER_LOST_NOR_WON = 0
    val LOST = -1
    val WON = 1

    var mode = MAIN_MODE

    private var goodLuck = 0 // Tells how many attempts were needed to get a valid draw

    val observable = MutableLiveData(ObservableHelper())

    var firestore: FirebaseFirestore
    private var games: CollectionReference

    init {
        initFigures()
        initGameExample()
        firestore = FirebaseFirestore.getInstance()
        games = firestore.collection("games")
    }

    private fun initFigures() {
        triangles.add(Triangle(10, 19, 20))
        triangles.add(Triangle(19, 20, 30))
        triangles.add(Triangle(10, 20, 21))
        triangles.add(Triangle(20, 21, 30))
        squares.add(Square(triangles[0], triangles[1], triangles[2], triangles[3], 10, 19, 30, 21))

        triangles.add(Triangle(3, 10, 11))
        triangles.add(Triangle(10, 11, 21))
        triangles.add(Triangle(3, 11, 12))
        triangles.add(Triangle(11, 12, 21))
        squares.add(Square(triangles[4], triangles[5], triangles[6], triangles[7], 3, 10, 21, 12))

        triangles.add(Triangle(21, 30, 31))
        triangles.add(Triangle(30, 31, 39))
        triangles.add(Triangle(21, 31, 32))
        triangles.add(Triangle(31, 32, 39))
        squares.add(
            Square(
                triangles[8],
                triangles[9],
                triangles[10],
                triangles[11],
                21,
                30,
                39,
                32
            )
        )

        triangles.add(Triangle(0, 3, 4))
        triangles.add(Triangle(3, 4, 12))
        triangles.add(Triangle(0, 4, 5))
        triangles.add(Triangle(4, 5, 12))
        squares.add(Square(triangles[12], triangles[13], triangles[14], triangles[15], 0, 3, 12, 5))

        triangles.add(Triangle(12, 21, 22))
        triangles.add(Triangle(21, 22, 32))
        triangles.add(Triangle(12, 22, 23))
        triangles.add(Triangle(22, 23, 32))
        squares.add(
            Square(
                triangles[16],
                triangles[17],
                triangles[18],
                triangles[19],
                12,
                21,
                32,
                23
            )
        )

        triangles.add(Triangle(32, 39, 40))
        triangles.add(Triangle(39, 40, 46))
        triangles.add(Triangle(32, 40, 41))
        triangles.add(Triangle(40, 41, 46))
        squares.add(
            Square(
                triangles[20],
                triangles[21],
                triangles[22],
                triangles[23],
                32,
                39,
                46,
                41
            )
        )

        triangles.add(Triangle(5, 12, 13))
        triangles.add(Triangle(12, 13, 23))
        triangles.add(Triangle(5, 13, 14))
        triangles.add(Triangle(13, 14, 23))
        squares.add(
            Square(
                triangles[24],
                triangles[25],
                triangles[26],
                triangles[27],
                5,
                12,
                23,
                14
            )
        )

        triangles.add(Triangle(23, 32, 33))
        triangles.add(Triangle(32, 33, 41))
        triangles.add(Triangle(23, 33, 34))
        triangles.add(Triangle(33, 34, 41))
        squares.add(
            Square(
                triangles[28],
                triangles[29],
                triangles[30],
                triangles[31],
                23,
                32,
                41,
                34
            )
        )

        triangles.add(Triangle(1, 5, 6))
        triangles.add(Triangle(5, 6, 14))
        triangles.add(Triangle(1, 6, 7))
        triangles.add(Triangle(6, 7, 14))
        squares.add(Square(triangles[32], triangles[33], triangles[34], triangles[35], 1, 5, 14, 7))

        triangles.add(Triangle(14, 23, 24))
        triangles.add(Triangle(23, 24, 34))
        triangles.add(Triangle(14, 24, 25))
        triangles.add(Triangle(24, 25, 34))
        squares.add(
            Square(
                triangles[36],
                triangles[37],
                triangles[38],
                triangles[39],
                14,
                23,
                34,
                25
            )
        )

        triangles.add(Triangle(34, 41, 42))
        triangles.add(Triangle(41, 42, 47))
        triangles.add(Triangle(34, 42, 43))
        triangles.add(Triangle(42, 43, 47))
        squares.add(
            Square(
                triangles[40],
                triangles[41],
                triangles[42],
                triangles[43],
                34,
                41,
                47,
                43
            )
        )

        triangles.add(Triangle(7, 14, 15))
        triangles.add(Triangle(14, 15, 25))
        triangles.add(Triangle(7, 15, 16))
        triangles.add(Triangle(15, 16, 25))
        squares.add(
            Square(
                triangles[44],
                triangles[45],
                triangles[46],
                triangles[47],
                7,
                14,
                25,
                16
            )
        )

        triangles.add(Triangle(25, 34, 35))
        triangles.add(Triangle(34, 35, 43))
        triangles.add(Triangle(25, 35, 36))
        triangles.add(Triangle(35, 36, 43))
        squares.add(
            Square(
                triangles[48],
                triangles[49],
                triangles[50],
                triangles[51],
                25,
                34,
                43,
                36
            )
        )

        triangles.add(Triangle(2, 7, 8))
        triangles.add(Triangle(7, 8, 16))
        triangles.add(Triangle(2, 8, 9))
        triangles.add(Triangle(8, 9, 16))
        squares.add(Square(triangles[52], triangles[53], triangles[54], triangles[55], 2, 7, 16, 9))

        triangles.add(Triangle(16, 25, 26))
        triangles.add(Triangle(25, 26, 36))
        triangles.add(Triangle(16, 26, 27))
        triangles.add(Triangle(26, 27, 36))
        squares.add(
            Square(
                triangles[56],
                triangles[57],
                triangles[58],
                triangles[59],
                16,
                25,
                36,
                27
            )
        )

        triangles.add(Triangle(36, 43, 44))
        triangles.add(Triangle(43, 44, 48))
        triangles.add(Triangle(36, 44, 45))
        triangles.add(Triangle(44, 45, 48))
        squares.add(
            Square(
                triangles[60],
                triangles[61],
                triangles[62],
                triangles[63],
                36,
                43,
                48,
                45
            )
        )

        triangles.add(Triangle(9, 16, 17))
        triangles.add(Triangle(16, 17, 27))
        triangles.add(Triangle(9, 17, 18))
        triangles.add(Triangle(17, 18, 27))
        squares.add(
            Square(
                triangles[64],
                triangles[65],
                triangles[66],
                triangles[67],
                9,
                16,
                27,
                18
            )
        )

        triangles.add(Triangle(27, 36, 37))
        triangles.add(Triangle(36, 37, 45))
        triangles.add(Triangle(27, 37, 38))
        triangles.add(Triangle(37, 38, 45))
        squares.add(
            Square(
                triangles[68],
                triangles[69],
                triangles[70],
                triangles[71],
                27,
                36,
                45,
                38
            )
        )

        triangles.add(Triangle(18, 27, 28))
        triangles.add(Triangle(27, 28, 38))
        triangles.add(Triangle(18, 28, 29))
        triangles.add(Triangle(28, 29, 38))
        squares.add(
            Square(
                triangles[72],
                triangles[73],
                triangles[74],
                triangles[75],
                18,
                27,
                38,
                29
            )
        )

        borderSquares.add(BorderSquare(triangles[2], triangles[5], 10, 20, 11, 21))
        borderSquares.add(BorderSquare(triangles[3], triangles[8], 20, 30, 21, 31))
        borderSquares.add(BorderSquare(triangles[6], triangles[13], 3, 11, 4, 12))
        borderSquares.add(BorderSquare(triangles[7], triangles[16], 11, 21, 12, 22))
        borderSquares.add(BorderSquare(triangles[10], triangles[17], 21, 31, 22, 32))
        borderSquares.add(BorderSquare(triangles[11], triangles[20], 31, 39, 32, 40))
        borderSquares.add(BorderSquare(triangles[15], triangles[24], 4, 12, 5, 13))
        borderSquares.add(BorderSquare(triangles[18], triangles[25], 12, 22, 13, 23))
        borderSquares.add(BorderSquare(triangles[19], triangles[28], 22, 32, 23, 33))
        borderSquares.add(BorderSquare(triangles[22], triangles[29], 32, 40, 33, 41))
        borderSquares.add(BorderSquare(triangles[26], triangles[33], 5, 13, 6, 14))
        borderSquares.add(BorderSquare(triangles[31], triangles[40], 33, 41, 34, 42))
        borderSquares.add(BorderSquare(triangles[35], triangles[44], 6, 14, 7, 15))
        borderSquares.add(BorderSquare(triangles[42], triangles[49], 34, 42, 35, 43))
        borderSquares.add(BorderSquare(triangles[46], triangles[53], 7, 15, 8, 16))
        borderSquares.add(BorderSquare(triangles[47], triangles[56], 15, 25, 16, 26))
        borderSquares.add(BorderSquare(triangles[50], triangles[57], 25, 35, 26, 36))
        borderSquares.add(BorderSquare(triangles[51], triangles[60], 35, 43, 36, 44))
        borderSquares.add(BorderSquare(triangles[55], triangles[64], 8, 16, 9, 17))
        borderSquares.add(BorderSquare(triangles[58], triangles[65], 16, 26, 17, 27))
        borderSquares.add(BorderSquare(triangles[59], triangles[68], 26, 36, 27, 37))
        borderSquares.add(BorderSquare(triangles[62], triangles[69], 36, 44, 37, 45))
        borderSquares.add(BorderSquare(triangles[67], triangles[72], 17, 27, 18, 28))
        borderSquares.add(BorderSquare(triangles[70], triangles[73], 27, 37, 28, 38))
    }

    private fun initGameExample() {

        triangles[0].color = Color.YELLOW //Color.YELLOW
        triangles[1].color = Color.YELLOW //Color.BLUE
        triangles[2].color = Color.YELLOW //Color.RED
        triangles[3].color = Color.YELLOW //Color.CYAN

        triangles[4].color = Color.CYAN //Color.MAGENTA
        triangles[5].color = Color.CYAN //Color.YELLOW
        triangles[6].color = Color.CYAN // Color.YELLOW
        triangles[7].color = Color.CYAN //Color.BLUE

        triangles[8].color = Color.MAGENTA //Color.YELLOW
        triangles[9].color = Color.MAGENTA //Color.CYAN
        triangles[10].color = Color.MAGENTA //Color.RED
        triangles[11].color = Color.MAGENTA //Color.MAGENTA

        triangles[12].color = Color.RED //Color.RED
        triangles[13].color = Color.RED //Color.MAGENTA
        triangles[14].color = Color.RED //Color.CYAN
        triangles[15].color = Color.RED //Color.RED

        triangles[16].color = Color.GREEN //Color.RED
        triangles[17].color = Color.GREEN //Color.MAGENTA
        triangles[18].color = Color.GREEN //Color.BLUE
        triangles[19].color = Color.GREEN //Color.GREEN

        triangles[20].color = Color.BLUE //Color.CYAN
        triangles[21].color = Color.BLUE //Color.YELLOW
        triangles[22].color = Color.BLUE //Color.RED
        triangles[23].color = Color.BLUE //Color.GREEN

        triangles[24].color = Color.CYAN //Color.CYAN
        triangles[25].color = Color.CYAN //Color.CYAN
        triangles[26].color = Color.CYAN //Color.RED
        triangles[27].color = Color.CYAN //Color.BLUE

        triangles[28].color = Color.RED //Color.MAGENTA
        triangles[29].color = Color.RED //Color.BLUE
        triangles[30].color = Color.RED //Color.CYAN
        triangles[31].color = Color.RED //Color.BLUE

        triangles[32].color = Color.GREEN //Color.BLUE
        triangles[33].color = Color.GREEN //Color.CYAN
        triangles[34].color = Color.GREEN //Color.GREEN
        triangles[35].color = Color.GREEN //Color.MAGENTA

        triangles[36].color = Color.WHITE
        triangles[37].color = Color.WHITE
        triangles[38].color = Color.WHITE
        triangles[39].color = Color.WHITE

        triangles[40].color = Color.YELLOW //Color.CYAN
        triangles[41].color = Color.YELLOW //Color.BLUE
        triangles[42].color = Color.YELLOW //Color.GREEN
        triangles[43].color = Color.YELLOW //Color.RED

        triangles[44].color = Color.BLUE //Color.RED
        triangles[45].color = Color.BLUE //Color.BLUE
        triangles[46].color = Color.BLUE //Color.YELLOW
        triangles[47].color = Color.BLUE //Color.MAGENTA

        triangles[48].color = Color.BLUE //Color.CYAN
        triangles[49].color = Color.BLUE //Color.RED
        triangles[50].color = Color.BLUE //Color.YELLOW
        triangles[51].color = Color.BLUE //Color.MAGENTA

        triangles[52].color = Color.YELLOW //Color.YELLOW
        triangles[53].color = Color.YELLOW //Color.GREEN
        triangles[54].color = Color.YELLOW //Color.RED
        triangles[55].color = Color.YELLOW //Color.BLUE

        triangles[56].color = Color.MAGENTA //Color.GREEN
        triangles[57].color = Color.MAGENTA //Color.CYAN
        triangles[58].color = Color.MAGENTA //Color.GREEN
        triangles[59].color = Color.MAGENTA //Color.MAGENTA

        triangles[60].color = Color.RED //Color.CYAN
        triangles[61].color = Color.RED //Color.BLUE
        triangles[62].color = Color.RED //Color.YELLOW
        triangles[63].color = Color.RED //Color.GREEN

        triangles[64].color = Color.CYAN //Color.GREEN
        triangles[65].color = Color.CYAN //Color.MAGENTA
        triangles[66].color = Color.CYAN //Color.YELLOW
        triangles[67].color = Color.CYAN //Color.YELLOW

        triangles[68].color = Color.GREEN //Color.RED
        triangles[69].color = Color.GREEN //Color.BLUE
        triangles[70].color = Color.GREEN //Color.MAGENTA
        triangles[71].color = Color.GREEN //Color.GREEN

        triangles[72].color = Color.MAGENTA //Color.MAGENTA
        triangles[73].color = Color.MAGENTA //Color.GREEN
        triangles[74].color = Color.MAGENTA //Color.GREEN
        triangles[75].color = Color.MAGENTA //Color.YELLOW

        /*
        triangles[0].color = Color.YELLOW
        triangles[1].color = Color.BLUE
        triangles[2].color = Color.RED
        triangles[3].color = Color.CYAN

        triangles[4].color = Color.MAGENTA
        triangles[5].color = Color.YELLOW
        triangles[6].color =  Color.YELLOW
        triangles[7].color = Color.BLUE

        triangles[8].color = Color.YELLOW
        triangles[9].color = Color.CYAN
        triangles[10].color = Color.RED
        triangles[11].color = Color.MAGENTA

        triangles[12].color = Color.RED
        triangles[13].color = Color.MAGENTA
        triangles[14].color = Color.CYAN
        triangles[15].color = Color.RED

        triangles[16].color = Color.RED
        triangles[17].color = Color.MAGENTA
        triangles[18].color = Color.BLUE
        triangles[19].color = Color.GREEN

        triangles[20].color = Color.CYAN
        triangles[21].color = Color.YELLOW
        triangles[22].color = Color.RED
        triangles[23].color = Color.GREEN

        triangles[24].color = Color.CYAN
        triangles[25].color = Color.CYAN
        triangles[26].color = Color.RED
        triangles[27].color = Color.BLUE

        triangles[28].color = Color.MAGENTA
        triangles[29].color = Color.BLUE
        triangles[30].color = Color.CYAN
        triangles[31].color = Color.BLUE

        triangles[32].color = Color.BLUE
        triangles[33].color = Color.CYAN
        triangles[34].color = Color.GREEN
        triangles[35].color = Color.MAGENTA

        triangles[36].color = Color.WHITE
        triangles[37].color = Color.WHITE
        triangles[38].color = Color.WHITE
        triangles[39].color = Color.WHITE

        triangles[40].color = Color.CYAN
        triangles[41].color = Color.BLUE
        triangles[42].color = Color.GREEN
        triangles[43].color = Color.RED

        triangles[44].color = Color.RED
        triangles[45].color = Color.BLUE
        triangles[46].color = Color.YELLOW
        triangles[47].color = Color.MAGENTA

        triangles[48].color = Color.CYAN
        triangles[49].color = Color.RED
        triangles[50].color = Color.YELLOW
        triangles[51].color = Color.MAGENTA

        triangles[52].color = Color.YELLOW
        triangles[53].color = Color.GREEN
        triangles[54].color = Color.RED
        triangles[55].color = Color.BLUE

        triangles[56].color = Color.GREEN
        triangles[57].color = Color.CYAN
        triangles[58].color = Color.GREEN
        triangles[59].color = Color.MAGENTA

        triangles[60].color = Color.CYAN
        triangles[61].color = Color.BLUE
        triangles[62].color = Color.YELLOW
        triangles[63].color = Color.GREEN

        triangles[64].color = Color.GREEN
        triangles[65].color = Color.MAGENTA
        triangles[66].color = Color.YELLOW
        triangles[67].color = Color.YELLOW

        triangles[68].color = Color.RED
        triangles[69].color = Color.BLUE
        triangles[70].color = Color.MAGENTA
        triangles[71].color = Color.GREEN

        triangles[72].color = Color.MAGENTA
        triangles[73].color = Color.GREEN
        triangles[74].color = Color.GREEN
        triangles[75].color = Color.YELLOW
        */

        for (square in squares) gameExample.endSquares.add(
            EndSquare(
                square.t1.color,
                square.t2.color,
                square.t3.color,
                square.t4.color
            )
        )
        for (i in 0..borderSquares.size) gameExample.triangleCrashes.add(false)

        gameExample.moves.add(Move(5, -1L, 5))
        gameExample.moves.add(Move(5, -1L, 1))
        gameExample.moves.add(Move(4, -1L, 2, 6))
        gameExample.moves.add(Move(5, -1L, 10))
        gameExample.moves.add(Move(5, -1L, 0))
        gameExample.moves.add(Move(3, -1L, 0))
        gameExample.moves.add(Move(3, -1L, 1))
        gameExample.moves.add(Move(5, -1L, 0))
        gameExample.moves.add(Move(5, -1L, 2))
        gameExample.moves.add(Move(3, -1L, 3))
        gameExample.moves.add(Move(3, -1L, 1))
        gameExample.moves.add(Move(4, -1L, 1, 2))
        gameExample.moves.add(Move(5, -1L, 2))
        gameExample.moves.add(Move(5, -1L, 3))
        gameExample.moves.add(Move(5, -1L, 4))
        gameExample.moves.add(Move(5, -1L, 5))
        gameExample.moves.add(Move(5, -1L, 8))
        gameExample.moves.add(Move(4, -1L, 5, 7))
        gameExample.moves.add(Move(5, -1L, 8))
        gameExample.moves.add(Move(4, -1L, 4, 10))
        gameExample.moves.add(Move(5, -1L, 3))
        gameExample.moves.add(Move(4, -1L, 6, 8))
        gameExample.moves.add(Move(5, -1L, 7))
        gameExample.moves.add(Move(0, -1L, 5))
        gameExample.moves.add(Move(4, -1L, 2, 7))
        gameExample.moves.add(Move(5, -1L, 4))
        gameExample.moves.add(Move(5, -1L, 16))
        gameExample.moves.add(Move(5, -1L, 17))
        gameExample.moves.add(Move(5, -1L, 20))
        gameExample.moves.add(Move(2, -1L))
        gameExample.moves.add(Move(1, -1L, 15))
        gameExample.moves.add(Move(5, -1L, 21))
        gameExample.moves.add(Move(4, -1L, 16, 17))
        gameExample.moves.add(Move(5, -1L, 22))
        gameExample.moves.add(Move(3, -1L, 12))
        gameExample.moves.add(Move(5, -1L, 16))
        gameExample.moves.add(Move(4, -1L, 14, 18))
        gameExample.moves.add(Move(5, -1L, 20))
        gameExample.moves.add(Move(4, -1L, 14, 17))
        gameExample.moves.add(Move(5, -1L, 16))
        gameExample.moves.add(Move(4, -1L, 6, 14))
        gameExample.moves.add(Move(5, -1L, 15))
        gameExample.moves.add(Move(5, -1L, 16))
        gameExample.moves.add(Move(1, -1L, 13))
        gameExample.moves.add(Move(5, -1L, 14))
        gameExample.moves.add(Move(2, -1L))
        gameExample.moves.add(Move(0, -1L, 2))
        gameExample.moves.add(Move(4, -1L, 2, 16))
        gameExample.moves.add(Move(5, -1L, 19))
        gameExample.moves.add(Move(3, -1L, 13))
        gameExample.moves.add(Move(5, -1L, 18))
        gameExample.moves.add(Move(4, -1L, 13, 16))
        gameExample.moves.add(Move(5, -1L, 18))
        gameExample.moves.add(Move(4, -1L, 5, 6))
        gameExample.moves.add(Move(4, -1L, 8, 16))
        gameExample.moves.add(Move(5, -1L, 12))
        gameExample.moves.add(Move(4, -1L, 4, 15))
        gameExample.moves.add(Move(5, -1L, 7))
        gameExample.moves.add(Move(4, -1L, 7, 18))
        gameExample.moves.add(Move(5, -1L, 8))
        gameExample.moves.add(Move(4, -1L, 4, 8))
        gameExample.moves.add(Move(5, -1L, 8))
        gameExample.moves.add(Move(2, -1L))
        gameExample.moves.add(Move(1, -1L, 6))
        gameExample.moves.add(Move(2, -1L))
        gameExample.moves.add(Move(0, -1L, 8))
        gameExample.moves.add(Move(5, -1L, 10))
        gameExample.moves.add(Move(4, -1L, 5, 10))
        gameExample.moves.add(Move(4, -1L, 6, 12))
        gameExample.moves.add(Move(5, -1L, 10))
        gameExample.moves.add(Move(0, -1L, 7))
        gameExample.moves.add(Move(4, -1L, 4, 13))
        gameExample.moves.add(Move(5, -1L, 8))
    }

    fun reset() {
        for (borderSquare in borderSquares) borderSquare.switch = false
        for (square in squares) square.switch = false
        observable.value = ObservableHelper()
        randomColoring()
        gameActive = Game()
    }


    fun randomColoring() {

        colors.add(Color.RED)
        colors.add(Color.RED)
        colors.add(Color.RED)
        colors.add(Color.RED)
        colors.add(Color.RED)
        colors.add(Color.RED)
        colors.add(Color.RED)
        colors.add(Color.RED)
        colors.add(Color.RED)
        colors.add(Color.RED)
        colors.add(Color.RED)
        colors.add(Color.RED)

        colors.add(Color.GREEN)
        colors.add(Color.GREEN)
        colors.add(Color.GREEN)
        colors.add(Color.GREEN)
        colors.add(Color.GREEN)
        colors.add(Color.GREEN)
        colors.add(Color.GREEN)
        colors.add(Color.GREEN)
        colors.add(Color.GREEN)
        colors.add(Color.GREEN)
        colors.add(Color.GREEN)
        colors.add(Color.GREEN)

        colors.add(Color.BLUE)
        colors.add(Color.BLUE)
        colors.add(Color.BLUE)
        colors.add(Color.BLUE)
        colors.add(Color.BLUE)
        colors.add(Color.BLUE)
        colors.add(Color.BLUE)
        colors.add(Color.BLUE)
        colors.add(Color.BLUE)
        colors.add(Color.BLUE)
        colors.add(Color.BLUE)
        colors.add(Color.BLUE)

        colors.add(Color.YELLOW)
        colors.add(Color.YELLOW)
        colors.add(Color.YELLOW)
        colors.add(Color.YELLOW)
        colors.add(Color.YELLOW)
        colors.add(Color.YELLOW)
        colors.add(Color.YELLOW)
        colors.add(Color.YELLOW)
        colors.add(Color.YELLOW)
        colors.add(Color.YELLOW)
        colors.add(Color.YELLOW)
        colors.add(Color.YELLOW)

        colors.add(Color.MAGENTA)
        colors.add(Color.MAGENTA)
        colors.add(Color.MAGENTA)
        colors.add(Color.MAGENTA)
        colors.add(Color.MAGENTA)
        colors.add(Color.MAGENTA)
        colors.add(Color.MAGENTA)
        colors.add(Color.MAGENTA)
        colors.add(Color.MAGENTA)
        colors.add(Color.MAGENTA)
        colors.add(Color.MAGENTA)
        colors.add(Color.MAGENTA)

        colors.add(Color.CYAN)
        colors.add(Color.CYAN)
        colors.add(Color.CYAN)
        colors.add(Color.CYAN)
        colors.add(Color.CYAN)
        colors.add(Color.CYAN)
        colors.add(Color.CYAN)
        colors.add(Color.CYAN)
        colors.add(Color.CYAN)
        colors.add(Color.CYAN)
        colors.add(Color.CYAN)
        colors.add(Color.CYAN)

        for ((index, triangle) in triangles.withIndex()) {
            if (index == 36 || index == 37 || index == 38 || index == 39) continue
            val random = Random()
            val number = random.nextInt(colors.size)
            triangle.color = colors[number]
            colors.removeAt(number)
        }

        var switch: Boolean

        do {
            switch = false
            for (borderSquare in borderSquares) {
                if (borderSquare.t1.color == borderSquare.t2.color) {
                    switch = true
                    if (borderSquare != borderSquares[0]) {
                        //Log.i("Luck", "Bad Luck!")
                        if (borderSquare.t1.color != borderSquares[0].t1.color) {
                            var temp = borderSquare.t1.color
                            borderSquare.t1.color = borderSquares[0].t1.color
                            borderSquares[0].t1.color = temp
                        } else {
                            val swapSquare = borderSquares[java.util.Random()
                                .nextInt(borderSquares.size - 1) + 1]
                            val temp = borderSquare.t1.color
                            borderSquare.t1.color = swapSquare.t1.color
                            swapSquare.t1.color = temp
                        }

                    } else {
                        val swapSquare =
                            borderSquares[java.util.Random().nextInt(borderSquares.size - 1) + 1]
                        val temp = borderSquare.t1.color
                        borderSquare.t1.color = swapSquare.t1.color
                        swapSquare.t1.color = temp
                    }
                }
            }
        } while (switch)

        if (luckRestriction()) {
            goodLuck++
            randomColoring()
            return
        } else {
            //Log.i("Test", "GoodLuck: $goodLuck")
            goodLuck = 0
        }

        observable.value = ObservableHelper()
    }

    private fun luckRestriction(): Boolean {
        val colors =
            arrayOf(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.CYAN, Color.MAGENTA)
        var doubles = 0
        for (square in squares) {
            if (square == squares[9]) continue
            val occurrences = arrayListOf(0, 0, 0, 0, 0, 0)
            for ((index, color) in colors.withIndex()) {
                if (square.t1.color == color) occurrences[index]++
                if (square.t2.color == color) occurrences[index]++
                if (square.t3.color == color) occurrences[index]++
                if (square.t4.color == color) occurrences[index]++
            }
            for (occurrence in occurrences) {
                if (occurrence > 2) return true
                else if (occurrence == 2) doubles++
            }
        }
        if (mode == GAME_MODE) {
            if (doubles > 9) return true
        }
        return false
    }

    fun copyColoring() {
        for ((index, square) in squares.withIndex()) {
            square.t1.color = gameSelected.endSquares[index].topLeft
            square.t2.color = gameSelected.endSquares[index].topRight
            square.t3.color = gameSelected.endSquares[index].bottomLeft
            square.t4.color = gameSelected.endSquares[index].bottomRight
        }
        for ((index, borderSquare) in borderSquares.withIndex()) {
            borderSquare.switch = gameSelected.triangleCrashes[index]
        }
        var invertCounter = 0
        for (move in gameSelected.moves) {
            if (move.type == ROTATION_INVERT) invertCounter++
        }
        if (invertCounter % 2 != 0) observable.value =
            ObservableHelper(rotationInvert = true, rotationSet = true)
        else observable.value = ObservableHelper(rotationInvert = false, rotationSet = true)
    }


    fun rotateClockwise(source: Int, timeStamp: Long = -1L) {

        var squareSource = squares[source]

        val temp = squareSource.t1.color
        squareSource.t1.color = squareSource.t3.color
        squareSource.t3.color = squareSource.t4.color
        squareSource.t4.color = squareSource.t2.color
        squareSource.t2.color = temp

        if (mode == 1) {
            gameActive.moves.add(
                Move(
                    CLOCKWISE_ROTATION,
                    timeStamp,
                    source
                )
            )
            evaluate()
        } else rewindUpdate()
    }

    fun rotateCounterClockwise(source: Int, timeStamp: Long = -1L) {

        var squareSource = squares[source]

        val temp = squareSource.t1.color
        squareSource.t1.color = squareSource.t2.color
        squareSource.t2.color = squareSource.t4.color
        squareSource.t4.color = squareSource.t3.color
        squareSource.t3.color = temp

        if (mode == 1) {
            gameActive.moves.add(
                Move(
                    COUNTER_CLOCKWISE_ROTATION,
                    timeStamp,
                    source
                )
            )
            evaluate()
        } else rewindUpdate()
    }

    fun invertRotation(timeStamp: Long = -1L) {
        if (mode == 1) {
            gameActive.moves.add(
                Move(
                    ROTATION_INVERT,
                    timeStamp
                )
            )
            evaluate()
        } else rewindUpdate(true)
    }

    fun rotateDouble(source: Int, timeStamp: Long = -1L) {

        var squareSource = squares[source]

        val temp1 = squareSource.t1.color
        squareSource.t1.color = squareSource.t4.color
        squareSource.t4.color = temp1
        val temp2 = squareSource.t2.color
        squareSource.t2.color = squareSource.t3.color
        squareSource.t3.color = temp2

        if (mode == 1) {
            gameActive.moves.add(
                Move(
                    DOUBLE_ROTATION,
                    timeStamp,
                    source
                )
            )
            evaluate()
        } else rewindUpdate()
    }

    fun squareSwap(source: Int, target: Int, timeStamp: Long = -1L) {

        var squareSource = squares[source]
        var squareTarget = squares[target]

        var temp: Int

        temp = squareSource.t1.color
        squareSource.t1.color = squareTarget.t1.color
        squareTarget.t1.color = temp

        temp = squareSource.t2.color
        squareSource.t2.color = squareTarget.t2.color
        squareTarget.t2.color = temp

        temp = squareSource.t3.color
        squareSource.t3.color = squareTarget.t3.color
        squareTarget.t3.color = temp

        temp = squareSource.t4.color
        squareSource.t4.color = squareTarget.t4.color
        squareTarget.t4.color = temp

        if (mode == 1) {
            gameActive.moves.add(
                Move(
                    SQUARE_SWAP,
                    timeStamp,
                    source,
                    target
                )
            )
            evaluate()
        } else rewindUpdate()
    }

    fun triangleSwap(target: Int, timeStamp: Long = -1L) {

        val borderSquareTarget = borderSquares[target]

        val temp = borderSquareTarget.t1.color
        borderSquareTarget.t1.color = borderSquareTarget.t2.color
        borderSquareTarget.t2.color = temp

        if (mode == GAME_MODE) {
            gameActive.moves.add(
                Move(
                    TRIANGLE_SWAP,
                    timeStamp,
                    target
                )
            )
            evaluate()
        } else rewindUpdate()
    }

    fun timeUp(timeStamp: Long = -1L) {
        if (mode == GAME_MODE) {
            gameActive.moves.add(
                Move(
                    TIME_UP,
                    timeStamp
                )
            )
            evaluate(true)
        } else rewindUpdate()
    }

    private fun evaluate(timeOut: Boolean = false) {
        var isLost = false
        var isWon = false
        val winControlHelper = winControl()
        var completedSquares = winControlHelper.squaresCompleted
        if (loseControl() || timeOut) isLost = true
        else if (completedSquares == 18) isWon = true
        if (isWon || isLost) {
            for (square in squares) gameActive.endSquares.add(
                EndSquare(
                    square.t1.color,
                    square.t2.color,
                    square.t3.color,
                    square.t4.color
                )
            )
            for (borderSquare in borderSquares) {
                if (borderSquare.switch) gameActive.triangleCrashes.add(true)
                else gameActive.triangleCrashes.add(false)
            }
            gameActive.user = FirebaseAuth.getInstance().currentUser?.uid
            gameActive.endTime = org.threeten.bp.LocalDateTime.now().toString()
            gameActive.squaresCompleted = completedSquares
            gameActive.moves.last().completedSquares = completedSquares
            for ((index, move) in gameActive.moves.withIndex()) {
                if (index == 0) {
                    if (move.completedSquares > 0) gameActive.duration =
                        gameActive.moves[0].timeStamp
                } else {
                    if (move.completedSquares > gameActive.moves[index - 1].completedSquares) gameActive.duration =
                        gameActive.moves[index].timeStamp
                }
            }

            val id = games.document().id
            gameActive.id = id

            games.document(id).set(gameActive).addOnCompleteListener { result ->
                if (result.isSuccessful) {

                    val queryAll = firestore.collection("games")
                        .orderBy("squaresCompleted", Query.Direction.DESCENDING)
                        .orderBy("duration", Query.Direction.ASCENDING)
                        .limit(100L)

                    val queryMy = firestore.collection("games")
                        .whereEqualTo("user", FirebaseAuth.getInstance().currentUser?.uid)
                        .orderBy("squaresCompleted", Query.Direction.DESCENDING)
                        .orderBy("duration", Query.Direction.ASCENDING)

                    queryAll.get().addOnSuccessListener { documentsAll ->

                        var listAll: MutableList<Game> = documentsAll.toObjects(Game::class.java)
                        var rankAll = listAll.indexOf(gameActive).toLong()
                        if (rankAll != -1L) rankAll = rankAll.plus(1)

                        queryMy.get().addOnSuccessListener { documentsMy ->

                            var listMy: MutableList<Game> = documentsMy.toObjects(Game::class.java)
                            var rankMy =
                                listMy.indexOf(listMy.last { it.id == id }).toLong().plus(1)

                            _gameRank.value = GameRank(rankMy, rankAll)
                        }

                    }
                } else {
                    //TODO set some negative value and on the basis of that show some toast in mainActivity
                }
            }.addOnFailureListener {
                //TODO set some negative value and on the basis of that show some toast in mainActivity
            }

            if (isLost) observable.value = ObservableHelper(LOST)
            if (isWon) observable.value = ObservableHelper(WON)
        } else {
            if (gameActive.moves.isNotEmpty()) {
                gameActive.moves.last().completedSquares = completedSquares
                if (gameActive.moves.last().type == ROTATION_INVERT) {
                    observable.value = ObservableHelper(
                        squareCompletedOne = winControlHelper.squareCompletedOne,
                        squareCompletedTwo = winControlHelper.squareCompletedTwo,
                        rotationInvert = true
                    )
                    return
                }
            }
            observable.value = ObservableHelper(
                squareCompletedOne = winControlHelper.squareCompletedOne,
                squareCompletedTwo = winControlHelper.squareCompletedTwo
            )
        }
    }

    private fun loseControl(): Boolean {
        var isLost = false
        for (borderSquare in borderSquares) {
            if (borderSquare.t1.color == borderSquare.t2.color) {
                borderSquare.switch = true
                isLost = true
            }
            // else borderSquare.switch = false
        }
        return isLost
    }

    private fun winControl(): WinControlHelper {
        var squaresCompleted = 0
        var squareCompletedOne = -1
        var squareCompletedTwo = -1
        for ((index, square) in squares.withIndex()) {
            if (square == squares[9]) continue
            if (square.t1.color == square.t2.color && square.t1.color == square.t3.color && square.t1.color == square.t4.color) {
                squaresCompleted++
                if (!square.switch && gameActive.moves.last().type == TRIANGLE_SWAP) {
                    if (squareCompletedOne == -1) squareCompletedOne = index
                    else squareCompletedTwo = index
                }
                square.switch = true
            } else square.switch = false
        }
        return WinControlHelper(squaresCompleted, squareCompletedOne, squareCompletedTwo)
    }


    fun rewind(back: Boolean = true): Boolean {

        if (rewindState < 0) rewindState = gameSelected.moves.size
        if ((back && rewindState == 0) || (!back && rewindState == gameSelected.moves.size)) return false
        var counter = rewindState
        if (back) {
            counter--
            rewindState--
        } else rewindState++

        val move = gameSelected.moves[counter]

        when (move.type) {
            CLOCKWISE_ROTATION -> {
                if (back) rotateCounterClockwise(move.first)
                else rotateClockwise(move.first)
            }
            COUNTER_CLOCKWISE_ROTATION -> {
                if (back) rotateClockwise(move.first)
                else rotateCounterClockwise(move.first)
            }
            ROTATION_INVERT -> invertRotation()
            DOUBLE_ROTATION -> rotateDouble(move.first)
            SQUARE_SWAP -> squareSwap(move.first, move.second)
            TRIANGLE_SWAP -> triangleSwap(move.first)
            TIME_UP -> timeUp()
        }
        return true
    }

    private fun rewindUpdate(switch: Boolean = false) {
        if (switch) {
            observable.value = ObservableHelper(rotationInvert = true)
            return
        }
        observable.value = ObservableHelper()
    }


    inner class WinControlHelper(
        val squaresCompleted: Int,
        val squareCompletedOne: Int,
        val squareCompletedTwo: Int
    )

    inner class ObservableHelper(
        val trigger: Int = NEITHER_LOST_NOR_WON,
        val squareCompletedOne: Int = -1,
        val squareCompletedTwo: Int = -1,
        val rotationInvert: Boolean = false,
        val rotationSet: Boolean = false
    )

}

data class GameRank(val rankMy: Long, val rankAll: Long)

