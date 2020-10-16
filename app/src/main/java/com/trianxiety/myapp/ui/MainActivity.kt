package com.trianxiety.myapp.ui

import android.content.Context
import android.content.DialogInterface
import android.graphics.*
import android.graphics.Path.FillType
import android.media.MediaPlayer
import android.os.*
import android.util.Log
import android.view.*
import android.widget.Chronometer
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.Guideline
import androidx.lifecycle.Observer
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.android.gms.ads.MobileAds
import com.google.firebase.auth.FirebaseAuth
import com.trianxiety.myapp.DiamondView
import com.trianxiety.myapp.R
import com.trianxiety.myapp.database.Game
import com.trianxiety.myapp.getIndex
import com.trianxiety.myapp.ui.rank.FirestoreAdapter
import com.trianxiety.myapp.ui.rank.Rank
import com.trianxiety.myapp.viewmodel.GameRank
import com.trianxiety.myapp.viewmodel.MAViewModel
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.sqrt

/*
    * Interstitial ad
    * Hardwaew Acceleration
    * Duration
    * Design
    * Database errors
    * Sign-in
    *  Bugs
        * Sometimes, a press is not recognized
        * Sometimes, after a move, it takes a little while until the squares become responsive again
        * Unintentional swaps
        * Alfa & Beta
    * How to restore the state when the activity is re-created after having been destroyed?
            * mode
            * rules
            * rotations
            * step-variables
            * switches
            * viewModel
    * Possibilities
        * Poly-player
        * Casino
*/

class MainActivity : AppCompatActivity(), View.OnTouchListener, View.OnDragListener,
    View.OnClickListener,
    FirestoreAdapter.OnGameSelectedListener {

    private val MODE = "mode"

    val viewModel: MAViewModel by viewModels()

    private lateinit var auth: FirebaseAuth

    private lateinit var vibrator: Vibrator

    private var squareCompletedOne = -1
    private var squareCompletedTwo = -1
    private var haveWon = false

    private lateinit var mediaPlayerGameLost: MediaPlayer
    private lateinit var mediaPlayerGameWon: MediaPlayer
    private lateinit var mediaPlayerSquareCompleted: MediaPlayer

    private lateinit var gameView: GameView

    private val guidelineIntersections = ArrayList<GuidelineIntersection>()
    private val imageViewSquares = ArrayList<View>()
    private val imageViewBorderSquares = ArrayList<ImageView>()
    private val thumbViewSquares = ArrayList<ImageView>()
    private val thumbViewBorderSquares = ArrayList<ImageView>()

    private val imageViewSquares__ = ArrayList<ImageView>()

    private var switch1 = false // True if the source-square has been exited
    private var switch2 = false // True if the center-square is the source
        set(value) {
            if (value) bordersToFront()
            else squaresToFront()
            field = value
        }
    private var switch3 = false // True if rotation is counter-clockwise
    private var switch4 = false // True if chronometer.onTickListener should be active

    private lateinit var dialogStart: DialogStart

    private lateinit var alertDialog: AlertDialog
    private lateinit var alertDialogBuilderNewGame: AlertDialog.Builder
    private lateinit var alertDialogBuilderMainMenu: AlertDialog.Builder
    private lateinit var alertDialogBuilderRankRules: AlertDialog.Builder
    //private lateinit var alertDialogBuilderClaimPrompt: AlertDialog.Builder

    private val START_MODE = -1
    private val MAIN_MODE = 0
    private val GAME_MODE = 1
    private val RANK_MODE = 2
    private val HISTORICAL_MODE = 3
    private val PLAY_MODE = 4
    private val RULES_MODE = 5

    private var positionMyGames = -1L
    private var positionAllGames = -1L

    private var mode = START_MODE
        set(value) {
            val previous = field
            field = value
            viewModel.mode = field
            removeViews()
            if (previous == MAIN_MODE) {
                handlerMain.removeCallbacks(animationMain)
                dialogStart.cancel()
            }
            if (previous == GAME_MODE) {
                chronometer.stop()
                switch4 = false
            }
            if (previous == PLAY_MODE) {
                handler9.removeCallbacks(animation9)
                step9 = 0
                switchLast = false
            }
            when (field) {
                MAIN_MODE -> {
                    enterMainMode()
                    handlerMain.postDelayed(animationMain, 0)
                }
                GAME_MODE -> {
                    enterGameMode()
                    viewModel.reset()
                }
                RANK_MODE -> {
                    supportFragmentManager.beginTransaction()
                        .add(R.id.parentLayout, Rank.newInstance("", ""), "rank").commit()
                }
                HISTORICAL_MODE -> {
                    if (previous == GAME_MODE) viewModel.gameSelected = viewModel.gameActive
                    enterHistoricalMode()
                    if (previous == GAME_MODE) txtRank.text = ""
                    if (previous == START_MODE || previous == PLAY_MODE) viewModel.gameSelected =
                        viewModel.gameSelected
                    if (previous == START_MODE || previous == RANK_MODE || previous == PLAY_MODE) setRank()
                    setStats(-1)
                    viewModel.copyColoring()
                }
                PLAY_MODE -> {
                    enterPlayMode()
                    do while (viewModel.rewind())
                    setStats(viewModel.rewindState)
                    handler9.postDelayed(animation9, 0)
                }
                RULES_MODE -> {
                    enterRulesMode()
                }

            }
        }

    private val handlerUpdate = Handler()
    private val delayUpdate = 50L
    private val delayedUpdate = object : Runnable {
        override fun run() {
            if (haveWon) {
                haveWon = false
                mode = HISTORICAL_MODE
            } else update()
        }
    }

    private val handlerMain = Handler()
    private val delayMain = 200L
    private val animationMain = object : Runnable {
        override fun run() {
            if (mode == MAIN_MODE) {
                viewModel.randomColoring()
                handlerMain.postDelayed(this, delayMain)
            }
        }
    }

    private var rules = 0
        set(value) {
            if (value == 0) {
                txtRulesHeadline.text = resources.getString(R.string.strHeadlineR0)
                txtRulesBody.text = resources.getString(R.string.strBodyR0)
                viewModel.gameSelected = viewModel.gameExample
                viewModel.copyColoring()
                do while (viewModel.rewind())
            }
            if (field == 2 && value != 2) thumbView_regret.alpha = 0f
            if (field == 3 && value != 3) {
                handler3.removeCallbacks(animation3)
                step3 = 0
                thumbView_square5.alpha = 0f
                imageView5.alpha = 0f
                for (i in 0..(3 - rotations)) {
                    viewModel.rotateClockwise(5)
                }
            }
            if (field == 4 && value != 4) {
                handler4.removeCallbacks(animation4)
                step4 = 0
                thumbView_square9.alpha = 0f
                imageView9.alpha = 0f
            }
            if (field == 5 && value != 5) {
                handler5.removeCallbacks(animation5)
                step5 = 0
                thumbView_square5.alpha = 0f
                thumbView_square7.alpha = 0f
                thumbView_square9.alpha = 0f
                imageView5.alpha = 0f
                imageView7.alpha = 0f
                imageView9.alpha = 0f
                if (rotations == 1 || rotations == 3) {
                    viewModel.rotateDouble(5)
                }
            }
            if (field == 6 && value != 6) {
                handler6.removeCallbacks(animation6)
                step6 = 0
                thumbView_square5.alpha = 0f
                thumbView_square2.alpha = 0f
                thumbView_square0.alpha = 0f
                imageView5.alpha = 0f
                imageView2.alpha = 0f
                imageView0.alpha = 0f
                if (rotations == 1 || rotations == 3) {
                    viewModel.squareSwap(0, 5)
                }
            }
            if (field == 7 && value != 7) {
                handler7.removeCallbacks(animation7)
                step7 = 0
                thumbView_square9.alpha = 0f
                thumbView_borderSquare0_2.alpha = 0f
                imageView9.alpha = 0f
                imageView0_2.alpha = 0f
                if (rotations == 1 || rotations == 3) {
                    viewModel.triangleSwap(1)
                }
            }
            if (field == 8 && value != 8) {
                handler8.removeCallbacks(animation8)
                step8 = 0
                imageView5.alpha = 0f
                thumbView_square5.alpha = 0f
                thumbView_regret.alpha = 0f
            }
            if (field == 9 && value != 9) {
                handler9.removeCallbacks(animation9)
                step9 = 0
                switchLast = false
                hideImageViews()
                setRotation(false)
            }
            rotations = 0
            field = value
        }

    private var rotations = 0
        set(value) {
            if (value == 4) field = 0
            else field = value
        }

    private val handler3 = Handler()
    private val delay3 = 1000L
    private var step3 = 0
    private val animation3 = object : Runnable {
        override fun run() {
            if (mode == RULES_MODE && rules == 3) {
                imageView5.bringToFront()
                thumbView_square5.alpha = 1f
                thumbView_square5.bringToFront()
                if (step3 == 0) {
                    thumbView_square5.setImageResource(R.drawable.thumb_2)
                    step3 = 1
                } else if (step3 == 1) {
                    thumbView_square5.setImageResource(R.drawable.thumb_1)
                    imageView5.alpha = 0.3f
                    step3 = 2
                } else if (step3 == 2) {
                    thumbView_square5.setImageResource(R.drawable.thumb_2)
                    imageView5.alpha = 0f
                    rotations++
                    viewModel.rotateClockwise(5)
                    step3 = 1
                }
                handler3.postDelayed(this, delay3)
            }
        }
    }

    private val handler4 = Handler()
    private val delay4 = 1000L
    private var step4 = 0
    private val animation4 = object : Runnable {
        override fun run() {
            if (mode == RULES_MODE && rules == 4) {
                imageView9.bringToFront()
                thumbView_square9.alpha = 1f
                thumbView_square9.bringToFront()
                if (step4 == 0) {
                    thumbView_square9.setImageResource(R.drawable.thumb_2)
                    step4 = 1
                } else if (step4 == 1) {
                    thumbView_square9.setImageResource(R.drawable.thumb_1)
                    imageView9.alpha = 0.3f
                    step4 = 2
                } else if (step4 == 2) {
                    thumbView_square9.setImageResource(R.drawable.thumb_2)
                    imageView9.alpha = 0f
                    invertRotation()
                    step4 = 1
                }
                handler4.postDelayed(this, delay4)
            }
        }
    }

    private val handler5 = Handler()
    private val delay5 = 1000L
    private var step5 = 0
        set(value) {
            if (value == 5) field = 0
            else field = value
        }
    private val animation5 = object : Runnable {
        override fun run() {
            if (mode == RULES_MODE && rules == 5) {
                when (step5) {
                    0 -> {
                        imageView5.alpha = 0.3f
                        imageView5.bringToFront()
                        thumbView_square5.setImageResource(R.drawable.thumb_1)
                        thumbView_square5.alpha = 1f
                        thumbView_square5.bringToFront()
                    }
                    1 -> {
                        imageView7.alpha = 0.3f
                        imageView7.bringToFront()
                        thumbView_square5.alpha = 0f
                        thumbView_square7.setImageResource(R.drawable.thumb_1)
                        thumbView_square7.alpha = 1f
                        thumbView_square7.bringToFront()
                    }
                    2 -> {
                        imageView7.alpha = 0f
                        imageView9.alpha = 0.3f
                        imageView9.bringToFront()
                        thumbView_square7.alpha = 0f
                        thumbView_square9.setImageResource(R.drawable.thumb_1)
                        thumbView_square9.alpha = 1f
                        thumbView_square9.bringToFront()
                    }
                    3 -> {
                        imageView5.alpha = 0f
                        imageView9.alpha = 0f
                        thumbView_square9.setImageResource(R.drawable.thumb_2)
                        thumbView_square9.alpha = 1f
                        thumbView_square9.bringToFront()
                        viewModel.rotateDouble(5)
                        rotations++
                    }
                    4 -> {
                        thumbView_square9.alpha = 0f
                        thumbView_square5.setImageResource(R.drawable.thumb_2)
                        thumbView_square5.alpha = 1f
                        thumbView_square5.bringToFront()
                    }
                }
                step5++
                handler5.postDelayed(this, delay5)
            }
        }
    }

    private val handler6 = Handler()
    private val delay6 = 1000L
    private var step6 = 0
        set(value) {
            if (value == 5) field = 0
            else field = value
        }
    private val animation6 = object : Runnable {
        override fun run() {
            if (mode == RULES_MODE && rules == 6) {
                when (step6) {
                    0 -> {
                        imageView5.alpha = 0.3f
                        imageView5.bringToFront()
                        thumbView_square5.setImageResource(R.drawable.thumb_1)
                        thumbView_square5.alpha = 1f
                        thumbView_square5.bringToFront()
                    }
                    1 -> {
                        imageView2.alpha = 0.3f
                        imageView2.bringToFront()
                        thumbView_square5.alpha = 0f
                        thumbView_square2.setImageResource(R.drawable.thumb_1)
                        thumbView_square2.alpha = 1f
                        thumbView_square2.bringToFront()
                    }
                    2 -> {
                        imageView2.alpha = 0f
                        imageView0.alpha = 0.3f
                        imageView0.bringToFront()
                        thumbView_square2.alpha = 0f
                        thumbView_square0.setImageResource(R.drawable.thumb_1)
                        thumbView_square0.alpha = 1f
                        thumbView_square0.bringToFront()
                    }
                    3 -> {
                        imageView5.alpha = 0f
                        imageView0.alpha = 0f
                        thumbView_square0.setImageResource(R.drawable.thumb_2)
                        thumbView_square0.alpha = 1f
                        thumbView_square0.bringToFront()
                        viewModel.squareSwap(0, 5)
                        rotations++
                    }
                    4 -> {
                        thumbView_square0.alpha = 0f
                        thumbView_square5.setImageResource(R.drawable.thumb_2)
                        thumbView_square5.alpha = 1f
                        thumbView_square5.bringToFront()
                    }
                }
                step6++
                handler6.postDelayed(this, delay6)
            }
        }
    }

    private val handler7 = Handler()
    private val delay7 = 1000L
    private var step7 = 0
        set(value) {
            if (value == 4) field = 0
            else field = value
        }
    private val animation7 = object : Runnable {
        override fun run() {
            if (mode == RULES_MODE && rules == 7) {
                when (step7) {
                    0 -> {
                        imageView9.alpha = 0.3f
                        imageView9.bringToFront()
                        thumbView_square9.setImageResource(R.drawable.thumb_1)
                        thumbView_square9.alpha = 1f
                        thumbView_square9.bringToFront()
                    }
                    1 -> {
                        thumbView_square9.alpha = 0f
                        imageView0_2.alpha = 0.3f
                        imageView0_2.bringToFront()
                        thumbView_borderSquare0_2.setImageResource(R.drawable.thumb_1)
                        thumbView_borderSquare0_2.alpha = 1f
                        thumbView_borderSquare0_2.bringToFront()
                    }
                    2 -> {
                        imageView9.alpha = 0f
                        imageView0_2.alpha = 0f
                        thumbView_borderSquare0_2.setImageResource(R.drawable.thumb_2)
                        thumbView_borderSquare0_2.alpha = 1f
                        thumbView_borderSquare0_2.bringToFront()
                        viewModel.triangleSwap(1)
                        rotations++
                    }
                    3 -> {
                        thumbView_borderSquare0_2.alpha = 0f
                        thumbView_square9.setImageResource(R.drawable.thumb_2)
                        thumbView_square9.alpha = 1f
                        thumbView_square9.bringToFront()
                    }
                }
                step7++
                handler7.postDelayed(this, delay7)
            }
        }
    }

    private val handler8 = Handler()
    private val delay8 = 1000L
    private var step8 = 0
        set(value) {
            if (value == 4) field = 0
            else field = value
        }
    private val animation8 = object : Runnable {
        override fun run() {
            if (mode == RULES_MODE && rules == 8) {
                when (step8) {
                    0 -> {
                        imageView5.alpha = 0.3f
                        imageView5.bringToFront()
                        thumbView_square5.setImageResource(R.drawable.thumb_1)
                        thumbView_square5.alpha = 1f
                        thumbView_square5.bringToFront()
                    }
                    1 -> {
                        thumbView_square5.alpha = 0f
                        thumbView_regret.setImageResource(R.drawable.thumb_1)
                        thumbView_regret.alpha = 1f
                        thumbView_regret.bringToFront()
                    }
                    2 -> {
                        imageView5.alpha = 0f
                        thumbView_regret.setImageResource(R.drawable.thumb_2)
                        thumbView_regret.alpha = 1f
                        thumbView_regret.bringToFront()
                    }
                    3 -> {
                        thumbView_regret.alpha = 0f
                        thumbView_square5.setImageResource(R.drawable.thumb_2)
                        thumbView_square5.alpha = 1f
                        thumbView_square5.bringToFront()
                    }
                }
                step8++
                handler8.postDelayed(this, delay8)
            }
        }
    }

    private val handler9 = Handler()
    private val delay9 = 200L
    private var step9 = 0
    private var switchLast = false
    private lateinit var thumbViewToBeRemoved: ImageView
    private val animation9 = object : Runnable {
        override fun run() {
            thumbViewToBeRemoved.alpha = 0f
            if ((mode == RULES_MODE && rules == 9) || mode == PLAY_MODE) {
                if (!switchLast) {
                    val move = viewModel.gameSelected.moves[viewModel.rewindState]
                    var almostFinished = false
                    var isFinished = false
                    if (viewModel.rewindState == viewModel.gameSelected.moves.size - 1) almostFinished =
                        true
                    when (move.type) {
                        viewModel.CLOCKWISE_ROTATION -> {
                            val index = move.first
                            when (step9) {
                                0 -> {
                                    thumbViewSquares[index].setImageResource(R.drawable.thumb_2)
                                    thumbViewSquares[index].alpha = 1f
                                    thumbViewSquares[index].bringToFront()
                                    step9++
                                }
                                1 -> {
                                    imageViewSquares[index].alpha = 0.3f
                                    imageViewSquares[index].bringToFront()
                                    thumbViewSquares[index].setImageResource(R.drawable.thumb_1)
                                    thumbViewSquares[index].alpha = 1f
                                    thumbViewSquares[index].bringToFront()
                                    step9++
                                }
                                2 -> {
                                    imageViewSquares[index].alpha = 0f
                                    thumbViewSquares[index].setImageResource(R.drawable.thumb_2)
                                    thumbViewSquares[index].alpha = 1f
                                    thumbViewSquares[index].bringToFront()
                                    thumbViewToBeRemoved = thumbViewSquares[index]
                                    viewModel.rewind(false)
                                    step9 = 0
                                    if (almostFinished) isFinished = true
                                }
                            }
                        }
                        viewModel.COUNTER_CLOCKWISE_ROTATION -> {
                            val index = move.first
                            when (step9) {
                                0 -> {
                                    thumbViewSquares[index].setImageResource(R.drawable.thumb_2)
                                    thumbViewSquares[index].alpha = 1f
                                    thumbViewSquares[index].bringToFront()
                                    step9++
                                }
                                1 -> {
                                    imageViewSquares[index].alpha = 0.3f
                                    imageViewSquares[index].bringToFront()
                                    thumbViewSquares[index].setImageResource(R.drawable.thumb_1)
                                    thumbViewSquares[index].alpha = 1f
                                    thumbViewSquares[index].bringToFront()
                                    step9++
                                }
                                2 -> {
                                    imageViewSquares[index].alpha = 0f
                                    thumbViewSquares[index].setImageResource(R.drawable.thumb_2)
                                    thumbViewSquares[index].alpha = 1f
                                    thumbViewSquares[index].bringToFront()
                                    thumbViewToBeRemoved = thumbViewSquares[index]
                                    viewModel.rewind(false)
                                    step9 = 0
                                    if (almostFinished) isFinished = true
                                }
                            }
                        }
                        viewModel.ROTATION_INVERT -> {
                            when (step9) {
                                0 -> {
                                    thumbViewSquares[9].setImageResource(R.drawable.thumb_2)
                                    thumbViewSquares[9].alpha = 1f
                                    thumbViewSquares[9].bringToFront()
                                    step9++
                                }
                                1 -> {
                                    imageViewSquares[9].alpha = 0.3f
                                    imageViewSquares[9].bringToFront()
                                    thumbViewSquares[9].setImageResource(R.drawable.thumb_1)
                                    thumbViewSquares[9].alpha = 1f
                                    thumbViewSquares[9].bringToFront()
                                    step9++
                                }
                                2 -> {
                                    imageViewSquares[9].alpha = 0f
                                    thumbViewSquares[9].setImageResource(R.drawable.thumb_2)
                                    thumbViewSquares[9].alpha = 1f
                                    thumbViewSquares[9].bringToFront()
                                    thumbViewToBeRemoved = thumbViewSquares[9]
                                    viewModel.rewind(false)
                                    step9 = 0
                                    if (almostFinished) isFinished = true
                                }
                            }
                        }
                        viewModel.DOUBLE_ROTATION -> {
                            val index = move.first
                            when (step9) {
                                0 -> {
                                    thumbViewSquares[index].setImageResource(R.drawable.thumb_2)
                                    thumbViewSquares[index].alpha = 1f
                                    thumbViewSquares[index].bringToFront()
                                    step9++
                                }
                                1 -> {
                                    imageViewSquares[index].alpha = 0.3f
                                    imageViewSquares[index].bringToFront()
                                    thumbViewSquares[index].setImageResource(R.drawable.thumb_1)
                                    thumbViewSquares[index].alpha = 1f
                                    thumbViewSquares[index].bringToFront()
                                    step9++
                                }
                                2 -> {
                                    thumbViewSquares[index].alpha = 0f
                                    imageViewSquares[9].alpha = 0.3f
                                    imageViewSquares[9].bringToFront()
                                    thumbViewSquares[9].setImageResource(R.drawable.thumb_1)
                                    thumbViewSquares[9].alpha = 1f
                                    thumbViewSquares[9].bringToFront()
                                    step9++
                                }
                                3 -> {
                                    imageViewSquares[index].alpha = 0f
                                    imageViewSquares[9].alpha = 0f
                                    thumbViewSquares[9].setImageResource(R.drawable.thumb_2)
                                    thumbViewSquares[9].alpha = 1f
                                    thumbViewSquares[9].bringToFront()
                                    thumbViewToBeRemoved = thumbViewSquares[9]
                                    viewModel.rewind(false)
                                    step9 = 0
                                    if (almostFinished) isFinished = true
                                }
                            }
                        }
                        viewModel.SQUARE_SWAP -> {
                            val indexSource = move.first
                            val indexTarget = move.second
                            when (step9) {
                                0 -> {
                                    thumbViewSquares[indexSource].setImageResource(R.drawable.thumb_2)
                                    thumbViewSquares[indexSource].alpha = 1f
                                    thumbViewSquares[indexSource].bringToFront()
                                    step9++
                                }
                                1 -> {
                                    imageViewSquares[indexSource].alpha = 0.3f
                                    imageViewSquares[indexSource].bringToFront()
                                    thumbViewSquares[indexSource].setImageResource(R.drawable.thumb_1)
                                    thumbViewSquares[indexSource].alpha = 1f
                                    thumbViewSquares[indexSource].bringToFront()
                                    step9++
                                }
                                2 -> {
                                    thumbViewSquares[indexSource].alpha = 0f
                                    imageViewSquares[indexTarget].alpha = 0.3f
                                    imageViewSquares[indexTarget].bringToFront()
                                    thumbViewSquares[indexTarget].setImageResource(R.drawable.thumb_1)
                                    thumbViewSquares[indexTarget].alpha = 1f
                                    thumbViewSquares[indexTarget].bringToFront()
                                    step9++
                                }
                                3 -> {
                                    imageViewSquares[indexSource].alpha = 0f
                                    imageViewSquares[indexTarget].alpha = 0f
                                    thumbViewSquares[indexTarget].setImageResource(R.drawable.thumb_2)
                                    thumbViewSquares[indexTarget].alpha = 1f
                                    thumbViewSquares[indexTarget].bringToFront()
                                    thumbViewToBeRemoved = thumbViewSquares[indexTarget]
                                    viewModel.rewind(false)
                                    step9 = 0
                                    if (almostFinished) isFinished = true
                                }
                            }
                        }
                        viewModel.TRIANGLE_SWAP -> {
                            val index = move.first
                            when (step9) {
                                0 -> {
                                    thumbViewSquares[9].setImageResource(R.drawable.thumb_2)
                                    thumbViewSquares[9].alpha = 1f
                                    thumbViewSquares[9].bringToFront()
                                    step9++
                                }
                                1 -> {
                                    imageViewSquares[9].alpha = 0.3f
                                    thumbViewSquares[9].setImageResource(R.drawable.thumb_1)
                                    imageViewSquares[9].bringToFront()
                                    thumbViewSquares[9].alpha = 1f
                                    thumbViewSquares[9].bringToFront()
                                    step9++
                                }
                                2 -> {
                                    thumbViewSquares[9].alpha = 0f
                                    imageViewBorderSquares[index].alpha = 0.3f
                                    thumbViewBorderSquares[index].setImageResource(R.drawable.thumb_1)
                                    imageViewBorderSquares[index].bringToFront()
                                    thumbViewBorderSquares[index].alpha = 1f
                                    thumbViewBorderSquares[index].bringToFront()
                                    step9++
                                }
                                3 -> {
                                    imageViewSquares[9].alpha = 0f
                                    imageViewBorderSquares[index].alpha = 0f
                                    thumbViewBorderSquares[index].setImageResource(R.drawable.thumb_2)
                                    thumbViewBorderSquares[index].alpha = 1f
                                    thumbViewBorderSquares[index].bringToFront()
                                    thumbViewToBeRemoved = thumbViewBorderSquares[index]
                                    viewModel.rewind(false)
                                    step9 = 0
                                    if (almostFinished) isFinished = true
                                }
                            }
                        }
                        viewModel.TIME_UP -> {
                            when (step9) {
                                0 -> step9++
                                1 -> step9++
                                2 -> step9++
                                3 -> {
                                    viewModel.rewind(false)
                                    step9 = 0
                                    isFinished = true
                                }
                            }

                        }
                    }
                    if (mode == PLAY_MODE) setStats(viewModel.rewindState)
                    if (!isFinished) handler9.postDelayed(this, delay9)
                    else {
                        switchLast = true
                        handler9.postDelayed(this, delay9 * 10)
                    }
                } else {
                    switchLast = false
                    do while (viewModel.rewind())
                    if (mode == PLAY_MODE) setStats(viewModel.rewindState)
                    handler9.postDelayed(this, delay9)
                }
            }
        }
    }

    private lateinit var toastHistoricalGames: Toast
    private lateinit var toastRules: Toast
    private lateinit var toastCongratulations: Toast

    private lateinit var interstitialAd: InterstitialAd

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeAd()

        auth = FirebaseAuth.getInstance()

        if (auth.currentUser == null) {
            auth.signInAnonymously()
                .addOnCompleteListener { Log.i("Auth", "Completed") }
                .addOnFailureListener { Log.i("Auth", it.localizedMessage) }
        }

        initViews()
        initAlertDialogs()
        gameView = GameView(this)
        linearLayout.addView(gameView)

        interstitialAd.adListener = object : AdListener() {
            override fun onAdLoaded() {
                // Code to be executed when an ad finishes loading.
            }

            override fun onAdFailedToLoad(errorCode: Int) {
                // Code to be executed when an ad request fails.
            }

            override fun onAdOpened() {
                // Code to be executed when the ad is displayed.
            }

            override fun onAdClicked() {
                // Code to be executed when the user clicks on an ad.
            }

            override fun onAdLeftApplication() {
                // Code to be executed when the user has left the app.
            }

            override fun onAdClosed() {
                // Code to be executed when the interstitial ad is closed.
                interstitialAd.loadAd(AdRequest.Builder().build())
            }
        }

        val gameObserver = Observer<MAViewModel.ObservableHelper> {
            if (mode == GAME_MODE) {
                if (it.trigger == viewModel.LOST || it.trigger == viewModel.WON) {
                    chronometer.stop()
                    if (it.trigger == viewModel.LOST) {

                        //Toast.makeText(this@MainActivity, R.string.game_over, Toast.LENGTH_SHORT).show()
                        mode = HISTORICAL_MODE
                        mediaPlayerGameLost.start()

                        if (interstitialAd.isLoaded) {
                            interstitialAd.show()
                        } else {
                            //Log.d("TAG", "The interstitial wasn't loaded yet.")
                        }
                    }
                    if (it.trigger == viewModel.WON) {
                        //Toast.makeText(this, R.string.you_won, Toast.LENGTH_SHORT).show()
                        haveWon = true
                        mediaPlayerGameWon.start()
                    }
                } else {
                    if (it.squareCompletedOne != -1) {
                        squareCompletedOne = it.squareCompletedOne
                        mediaPlayerSquareCompleted.start()
                    }
                    if (it.squareCompletedTwo != -1) squareCompletedTwo = it.squareCompletedTwo
                }
            }
            if (it.rotationSet) setRotation(it.rotationInvert)
            else if (it.rotationInvert) invertRotation()
            update()
        }

        val rankObserver = Observer<GameRank> {
            it?.let {
                positionMyGames = it.rankMy
                positionAllGames = it.rankAll
                setRank(true)
            }
        }

        viewModel.observable.observe(this, gameObserver)
        viewModel.gameRank.observe(this, rankObserver)

        toastHistoricalGames =
            Toast.makeText(this, R.string.str_toastHistoricalGames, Toast.LENGTH_SHORT)
        toastRules = Toast.makeText(this, R.string.str_toastRules, Toast.LENGTH_SHORT)
        toastCongratulations =
            Toast.makeText(this, R.string.str_toastCongratulations, Toast.LENGTH_LONG)

        if (savedInstanceState == null) mode = MAIN_MODE
        else mode = savedInstanceState.getInt(MODE)

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        mediaPlayerGameLost = MediaPlayer.create(this, R.raw.game_lost)
        mediaPlayerGameWon = MediaPlayer.create(this, R.raw.game_won)
        mediaPlayerSquareCompleted = MediaPlayer.create(this, R.raw.square_completed)

        Log.i("Gnidrow", "onCreate")
    }

    private fun initializeAd() {

        MobileAds.initialize(this) {}
        interstitialAd = InterstitialAd(this)
        interstitialAd.adUnitId = "ca-app-pub-3940256099942544/1033173712"
        //interstitialAd.adUnitId = resources.getString(R.string.ad_id)
        interstitialAd.loadAd(AdRequest.Builder().build())
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(MODE, mode)
        Log.i("Gnidrow", "onSaveInstanceState")
    }

    override fun onResume() {
        super.onResume()

        initializeAd()

        if (mode == MAIN_MODE) {
            handlerMain.removeCallbacks(animationMain)
            handlerMain.postDelayed(animationMain, 0)
        }
        if (mode == RULES_MODE && rules == 3) {
            handler3.removeCallbacks(animation3)
            handler3.postDelayed(animation3, 0)
        }
        if (mode == RULES_MODE && rules == 4) {
            handler4.removeCallbacks(animation4)
            handler4.postDelayed(animation4, 0)
        }
        if (mode == RULES_MODE && rules == 5) {
            handler5.removeCallbacks(animation5)
            handler5.postDelayed(animation5, 0)
        }
        if (mode == RULES_MODE && rules == 6) {
            handler6.removeCallbacks(animation6)
            handler6.postDelayed(animation6, 0)
        }
        if (mode == RULES_MODE && rules == 7) {
            handler7.removeCallbacks(animation7)
            handler7.postDelayed(animation7, 0)
        }
        if (mode == RULES_MODE && rules == 8) {
            handler8.removeCallbacks(animation8)
            handler8.postDelayed(animation8, 0)
        }
        if ((mode == RULES_MODE && rules == 9) || mode == PLAY_MODE) {
            handler9.removeCallbacks(animation9)
            handler9.postDelayed(animation9, 0)
        }
        Log.i("Gnidrow", "onResume")
    }

    override fun onPause() {
        super.onPause()
        handlerUpdate.removeCallbacks(delayedUpdate)
        handlerMain.removeCallbacks(animationMain)
        handler3.removeCallbacks(animation3)
        handler4.removeCallbacks(animation4)
        handler5.removeCallbacks(animation5)
        handler6.removeCallbacks(animation6)
        handler7.removeCallbacks(animation7)
        handler8.removeCallbacks(animation8)
        handler9.removeCallbacks(animation9)
        Log.i("Gnidrow", "onPause")
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu, menu)
        return true
    }

    override fun onOptionsItemSelected(menuItem: MenuItem): Boolean {

        return when (menuItem.itemId) {
            R.id.itemNewGame -> {
                if (mode != GAME_MODE) mode = GAME_MODE
                else {
                    alertDialog = alertDialogBuilderNewGame.create()
                    alertDialog.setCancelable(false)
                    alertDialog.show()
                }
                true
            }
            R.id.itemMainMenu -> {
                if (mode != GAME_MODE) mode = MAIN_MODE
                else {
                    alertDialog = alertDialogBuilderMainMenu.create()
                    alertDialog.setCancelable(false)
                    alertDialog.show()
                }
                true
            }
            else -> false
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            R.id.btnNewGame -> mode = GAME_MODE
            R.id.btnRank -> mode = RANK_MODE
            R.id.btnRankRules -> {
                alertDialog = alertDialogBuilderRankRules.create()
                alertDialog.setCancelable(false)
                alertDialog.show()
            }
            /*
            R.id.btnClaim -> {
                val fragment = supportFragmentManager.findFragmentByTag("rank")
                if (fragment != null) supportFragmentManager.beginTransaction().remove(fragment).commit()
                supportFragmentManager.beginTransaction()
                    .add(R.id.parentLayout, Claim.newInstance("", ""), "claim_1").commit()
            }
            R.id.btnSignIn -> {
                val fragment = supportFragmentManager.findFragmentByTag("results")
                if (fragment != null) supportFragmentManager.beginTransaction().remove(fragment).commit()
                supportFragmentManager.beginTransaction()
                    .add(R.id.parentLayout, Auth.newInstance("", ""), "auth").commit()
            }

            R.id.btnSave -> {
                if (true) {
                    // insert a User into the database
                }
                val fragment_1 = supportFragmentManager.findFragmentByTag("claim_1")
                if (fragment_1 != null) {
                    supportFragmentManager.beginTransaction().remove(fragment_1).commit()
                    mode = RANK_MODE
                }
                val fragment_2 = supportFragmentManager.findFragmentByTag("claim_2")
                if (fragment_2 != null) supportFragmentManager.beginTransaction().remove(fragment_2).commit()
            }
            R.id.btnCancel -> {
                val fragment_1 = supportFragmentManager.findFragmentByTag("claim_1")
                if (fragment_1 != null) {
                    supportFragmentManager.beginTransaction().remove(fragment_1).commit()
                    mode = RANK_MODE
                }
                val fragment_2 = supportFragmentManager.findFragmentByTag("claim_2")
                if (fragment_2 != null) supportFragmentManager.beginTransaction().remove(fragment_2).commit()
            }
             R.id.btnShare -> {
            }
            */
            R.id.btnBackToRank -> mode = RANK_MODE
            R.id.btnWatch -> {
                if (mode == HISTORICAL_MODE) mode = PLAY_MODE
                else if (mode == PLAY_MODE) mode = HISTORICAL_MODE
            }
            R.id.btnBack -> {
                if (mode == HISTORICAL_MODE) {
                    if (viewModel.rewind(true)) {
                        if (viewModel.rewindState == 0) {
                            btnBack.alpha = 0.2f
                            btnBack.isEnabled = false
                        }
                        if (viewModel.rewindState == viewModel.gameSelected.moves.size - 1) {
                            btnForward.alpha = 1f
                            btnForward.isEnabled = true
                        }
                        setStats(viewModel.rewindState)
                    }
                }
                if (mode == RULES_MODE) {
                    when (rules) {
                        0 -> {

                        }
                        1 -> {
                            rules = 0
                            btnBack.alpha = 0.2f
                            txtRulesHeadline.text = resources.getString(R.string.strHeadlineR0)
                            txtRulesBody.text = resources.getString(R.string.strBodyR0)
                            do while (viewModel.rewind())
                        }
                        2 -> {
                            rules = 1
                            txtRulesHeadline.text = resources.getString(R.string.strHeadlineR1)
                            txtRulesBody.text = resources.getString(R.string.strBodyR1)
                            viewModel.rotateClockwise(2)
                            do while (viewModel.rewind(false))
                        }
                        3 -> {
                            rules = 2
                            txtRulesHeadline.text = resources.getString(R.string.strHeadlineR2)
                            txtRulesBody.text = resources.getString(R.string.strBodyR2)
                            thumbView_regret.setImageResource(R.drawable.ic_arrow)
                            thumbView_regret.alpha = 1f
                            thumbView_regret.bringToFront()
                            do while (viewModel.rewind())
                            viewModel.rotateCounterClockwise(2)
                        }
                        4 -> {
                            rules = 3
                            txtRulesHeadline.text = resources.getString(R.string.strHeadlineR3)
                            txtRulesBody.text = resources.getString(R.string.strBodyR3)
                            setRotation(false)
                            handler3.postDelayed(animation3, 0)
                        }
                        5 -> {
                            rules = 4
                            txtRulesHeadline.text = resources.getString(R.string.strHeadlineR4)
                            txtRulesBody.text = resources.getString(R.string.strBodyR4)
                            handler4.postDelayed(animation4, 0)
                        }
                        6 -> {
                            rules = 5
                            txtRulesHeadline.text = resources.getString(R.string.strHeadlineR5)
                            txtRulesBody.text = resources.getString(R.string.strBodyR5)
                            handler5.postDelayed(animation5, 0)
                        }
                        7 -> {
                            rules = 6
                            txtRulesHeadline.text = resources.getString(R.string.strHeadlineR6)
                            txtRulesBody.text = resources.getString(R.string.strBodyR6)
                            handler6.postDelayed(animation6, 0)
                        }
                        8 -> {
                            rules = 7
                            txtRulesHeadline.text = resources.getString(R.string.strHeadlineR7)
                            txtRulesBody.text = resources.getString(R.string.strBodyR7)
                            handler7.postDelayed(animation7, 0)
                        }
                        9 -> {
                            rules = 8
                            btnForward.alpha = 1f
                            txtRulesHeadline.text = resources.getString(R.string.strHeadlineR8)
                            txtRulesBody.text = resources.getString(R.string.strBodyR8)
                            viewModel.gameSelected = viewModel.gameExample
                            viewModel.copyColoring()
                            do while (viewModel.rewind())
                            handler8.postDelayed(animation8, 0)
                        }
                    }
                }
            }
            R.id.btnForward -> {
                if (mode == HISTORICAL_MODE) {
                    if (viewModel.rewind(false)) {
                        if (viewModel.rewindState == viewModel.gameSelected.moves.size) {
                            btnForward.alpha = 0.2f
                            btnForward.isEnabled = false
                        }
                        if (viewModel.rewindState == 1) {
                            btnBack.alpha = 1f
                            btnBack.isEnabled = true
                        }
                        setStats(viewModel.rewindState)
                    }
                }
                if (mode == RULES_MODE) {
                    when (rules) {
                        0 -> {
                            rules = 1
                            btnBack.alpha = 1f
                            txtRulesHeadline.text = resources.getString(R.string.strHeadlineR1)
                            txtRulesBody.text = resources.getString(R.string.strBodyR1)
                            do while (viewModel.rewind(false))
                        }
                        1 -> {
                            rules = 2
                            txtRulesHeadline.text = resources.getString(R.string.strHeadlineR2)
                            txtRulesBody.text = resources.getString(R.string.strBodyR2)
                            thumbView_regret.setImageResource(R.drawable.ic_arrow)
                            thumbView_regret.alpha = 1f
                            thumbView_regret.bringToFront()
                            do while (viewModel.rewind())
                            viewModel.rotateCounterClockwise(2)
                        }
                        2 -> {
                            rules = 3
                            txtRulesHeadline.text = resources.getString(R.string.strHeadlineR3)
                            txtRulesBody.text = resources.getString(R.string.strBodyR3)
                            viewModel.rotateClockwise(2)
                            handler3.postDelayed(animation3, 0)
                        }
                        3 -> {
                            rules = 4
                            txtRulesHeadline.text = resources.getString(R.string.strHeadlineR4)
                            txtRulesBody.text = resources.getString(R.string.strBodyR4)
                            handler4.postDelayed(animation4, 0)
                        }
                        4 -> {
                            rules = 5
                            txtRulesHeadline.text = resources.getString(R.string.strHeadlineR5)
                            txtRulesBody.text = resources.getString(R.string.strBodyR5)
                            setRotation(false)
                            handler5.postDelayed(animation5, 0)
                        }
                        5 -> {
                            rules = 6
                            txtRulesHeadline.text = resources.getString(R.string.strHeadlineR6)
                            txtRulesBody.text = resources.getString(R.string.strBodyR6)
                            handler6.postDelayed(animation6, 0)
                        }
                        6 -> {
                            rules = 7
                            txtRulesHeadline.text = resources.getString(R.string.strHeadlineR7)
                            txtRulesBody.text = resources.getString(R.string.strBodyR7)
                            handler7.postDelayed(animation7, 0)
                        }
                        7 -> {
                            rules = 8
                            txtRulesHeadline.text = resources.getString(R.string.strHeadlineR8)
                            txtRulesBody.text = resources.getString(R.string.strBodyR8)
                            handler8.postDelayed(animation8, 0)
                        }
                        8 -> {
                            rules = 9
                            btnForward.alpha = 0.2f
                            txtRulesHeadline.text = resources.getString(R.string.strHeadlineR9)
                            txtRulesBody.text = resources.getString(R.string.strBodyR9)
                            handler9.postDelayed(animation9, 0)
                        }
                    }
                }
            }
            R.id.btnRules -> mode = RULES_MODE
        }
    }

    override fun onGameSelected(gameRetrieved: Game, positionMyGames: Int, positionAllGames: Int) {
        viewModel.gameSelected = gameRetrieved
        this.positionMyGames = positionMyGames.toLong()
        this.positionAllGames = positionAllGames.toLong()
        mode = HISTORICAL_MODE
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onTouch(view: View, motionEvent: MotionEvent): Boolean {

        if (mode != GAME_MODE) {
            if (mode == HISTORICAL_MODE || mode == PLAY_MODE) toastHistoricalGames.show()
            if (mode == RULES_MODE) toastRules.show()
            return false
        }

        return if (motionEvent.action === MotionEvent.ACTION_DOWN) {
            val dragShadowBuilder = MyDragShadowBuilder()
            /*
            val imageView = ImageView(this)
            imageView.setImageResource(R.drawable.ic_star)
            val dragShadowBuilder = View.DragShadowBuilder(imageView)
            */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) view.startDragAndDrop(
                null,
                dragShadowBuilder,
                view,
                0
            )
            else view.startDrag(null, dragShadowBuilder, view, 0)
            val source = view as View
            if (source.tag == "9") {
                if (!switch2) switch2 = true
            }
            true
        } else false
    }

    override fun onDrag(view: View, dragEvent: DragEvent): Boolean {

        if (mode != GAME_MODE || dragEvent.localState == null) {
            //if (dragEvent.localState == null) Log.i("Experiment", "Alfa: ${dragEvent.action}")
            //else Log.i("Experiment", "Beta")
            return false
        }
        when (dragEvent.action) {
            DragEvent.ACTION_DRAG_STARTED -> {
                if (view == constraintLayout || view == topConstraintLayout || view == parentLayout) return false
                val target = view as ImageView
                return (switch2 && target.tag.toString().length > 2) || (switch2 && target.tag.toString() == "9") || (!switch2 && target.tag.toString().length < 3)
            }
            DragEvent.ACTION_DRAG_ENDED -> {
                val source = dragEvent.localState as ImageView
                if (source.tag.toString().length < 3) {
                    val index = getIndex(source.tag.toString())
                    imageViewSquares[index].alpha = 0f
                } else source.alpha = 0f // Shouldn't reach here
                if (source.tag == "9") {
                    if (switch2) switch2 = false
                } else {
                    if (view != constraintLayout && view != topConstraintLayout && view != parentLayout) {
                        val target = view as ImageView
                        if (target.tag.toString().length < 3) {
                            val index = getIndex(target.tag.toString())
                            imageViewSquares[index].alpha = 0f
                        } else target.alpha = 0f // Shouldn't reach here
                    }
                }
                switch1 = false
                return true
            }
            DragEvent.ACTION_DRAG_ENTERED -> {
                val source = dragEvent.localState as ImageView

                if (source.tag.toString().length < 3) {
                    val index = getIndex(source.tag.toString())
                    imageViewSquares[index].alpha = 0.3f
                    imageViewSquares[index].bringToFront()
                } else source.alpha = 0.3f // Shouldn't reach here

                if (view != constraintLayout && view != topConstraintLayout && view != parentLayout) {
                    val target = view as ImageView

                    if (source.tag == "9") {
                        if (target.tag.toString().length > 2) {
                            target.alpha = 0.3f
                            vibrator.vibrate(50L)
                        }
                        if (target.tag.toString() == "9" && !switch1) vibrator.vibrate(50L)
                    } else {
                        if (target.tag.toString().length < 3) {

                            val index = getIndex(target.tag.toString())
                            imageViewSquares[index].alpha = 0.3f
                            imageViewSquares[index].bringToFront()

                            if (source != target || !switch1) vibrator.vibrate(50L)
                        }
                    }
                }
                return true
            }
            DragEvent.ACTION_DRAG_EXITED -> {
                if (view != constraintLayout && view != topConstraintLayout && view != parentLayout) {
                    val source = dragEvent.localState as ImageView
                    val target = view as ImageView
                    if (source == target) switch1 = true
                    if (target.tag.toString().length < 3) {
                        if (source != target) {
                            val index = getIndex(target.tag.toString())
                            imageViewSquares[index].alpha = 0f
                        }
                    } else target.alpha = 0f
                }
                return true
            }
            DragEvent.ACTION_DROP -> {
                if (view != constraintLayout && view != topConstraintLayout && view != parentLayout) {

                    val source = dragEvent.localState as ImageView
                    val target = view as ImageView

                    if (source.tag.toString().length < 3) {
                        val indexSource = getIndex(source.tag.toString())
                        imageViewSquares[indexSource].alpha = 0f
                    } else source.alpha = 0f
                    if (target.tag.toString().length < 3) {
                        val indexTarget = getIndex(target.tag.toString())
                        imageViewSquares[indexTarget].alpha = 0f
                    } else target.alpha = 0f

                    if (source.tag == "9") {
                        if (target.tag == "9" && !switch1) viewModel.invertRotation(SystemClock.elapsedRealtime() - chronometer.base)
                        else if (target.tag.toString().length > 2) viewModel.triangleSwap(
                            imageViewBorderSquares.indexOf(target),
                            SystemClock.elapsedRealtime() - chronometer.base
                        )
                    } else {
                        if (target.tag != "9") {
                            if (source == target) {
                                if (!switch1) {
                                    if (switch3) viewModel.rotateCounterClockwise(
                                        imageViewSquares__.indexOf(
                                            source
                                        ), SystemClock.elapsedRealtime() - chronometer.base
                                    )
                                    else viewModel.rotateClockwise(
                                        imageViewSquares__.indexOf(source),
                                        SystemClock.elapsedRealtime() - chronometer.base
                                    )
                                }
                            } else {
                                if (target.tag.toString().length <= 2) viewModel.squareSwap(
                                    imageViewSquares__.indexOf(source),
                                    imageViewSquares__.indexOf(target),
                                    SystemClock.elapsedRealtime() - chronometer.base
                                )
                            }
                        } else viewModel.rotateDouble(
                            imageViewSquares__.indexOf(source),
                            SystemClock.elapsedRealtime() - chronometer.base
                        )
                    }
                }
                return false
            }
            DragEvent.ACTION_DRAG_LOCATION -> return true
            else -> return false
        }

        /*
        if (mode != GAME_MODE || dragEvent.localState == null) {
            //if (dragEvent.localState == null) Log.i("Experiment", "Alfa: ${dragEvent.action}")
            //else Log.i("Experiment", "Beta")
            return false
        }
        when (dragEvent.action) {
            DragEvent.ACTION_DRAG_STARTED -> {
                if (view == constraintLayout || view == topConstraintLayout || view == parentLayout) return false
                val target = view as View
                return (switch2 && target.tag.toString().length > 2) || (switch2 && target.tag.toString() == "9") || (!switch2 && target.tag.toString().length <= 2)
            }
            DragEvent.ACTION_DRAG_ENDED -> {
                val source = dragEvent.localState as View
                source.alpha = 0f
                if (source.tag == "9") {
                    if (switch2) switch2 = false
                } else {
                    if (view != constraintLayout && view != topConstraintLayout && view != parentLayout) {
                        val target = view as View
                        target.alpha = 0f
                    }
                }
                switch1 = false
                return true
            }
            DragEvent.ACTION_DRAG_ENTERED -> {
                val source = dragEvent.localState as View
                source.alpha = 0.3f

                if (view != constraintLayout && view != topConstraintLayout && view != parentLayout) {
                    val target = view as View
                    if (source.tag == "9") {
                        if (target.tag.toString().length > 2) {
                            target.alpha = 0.3f
                            vibrator.vibrate(50L)
                        }
                        if (target.tag.toString() == "9" && !switch1) vibrator.vibrate(50L)
                    } else {
                        if (target.tag.toString().length <= 2) {

                            target.alpha = 0.3f

                            if (source != target || !switch1) vibrator.vibrate(50L)
                        }
                    }
                }
                return true
            }
            DragEvent.ACTION_DRAG_EXITED -> {
                if (view != constraintLayout && view != topConstraintLayout && view != parentLayout) {
                    val source = dragEvent.localState as View
                    val target = view as View
                    if (source == target) switch1 = true
                    target.alpha = 0f
                }
                return true
            }
            DragEvent.ACTION_DROP -> {
                if (view != constraintLayout && view != topConstraintLayout && view != parentLayout) {
                    val source = dragEvent.localState as View
                    val target = view as View
                    source.alpha = 0f
                    target.alpha = 0f
                    if (source.tag == "9") {
                        if (target.tag == "9" && !switch1) viewModel.invertRotation(SystemClock.elapsedRealtime() - chronometer.base)
                        else if (target.tag.toString().length > 2) viewModel.triangleSwap(
                            imageViewBorderSquares.indexOf(target),
                            SystemClock.elapsedRealtime() - chronometer.base
                        )
                    } else {
                        if (target.tag != "9") {
                            if (source == target) {
                                if (!switch1) {
                                    if (switch3) viewModel.rotateCounterClockwise(
                                        imageViewSquares.indexOf(
                                            source
                                        ), SystemClock.elapsedRealtime() - chronometer.base
                                    )
                                    else viewModel.rotateClockwise(
                                        imageViewSquares.indexOf(source),
                                        SystemClock.elapsedRealtime() - chronometer.base
                                    )
                                }
                            } else {
                                if (target.tag.toString().length <= 2) viewModel.squareSwap(
                                    imageViewSquares.indexOf(source),
                                    imageViewSquares.indexOf(target),
                                    SystemClock.elapsedRealtime() - chronometer.base
                                )
                            }
                        } else viewModel.rotateDouble(
                            imageViewSquares.indexOf(source),
                            SystemClock.elapsedRealtime() - chronometer.base
                        )
                    }
                }
                return false
            }
            DragEvent.ACTION_DRAG_LOCATION -> return true
            else -> return false
        }
        */
    }

    private fun removeViews() {

        topConstraintLayout.apply {
            removeView(chronometer)
            removeView(txtTopTop)
            removeView(txtTopBottom)
            //removeView(adView)
            removeView(txtRulesHeadline)
            removeView(txtRulesBody)
            removeView(btnBackToRank)
            removeView(txtRank)
            removeView(btnWatch)
            //removeView(btnShare)
        }

        constraintLayout.apply {
            removeView(btnBack)
            removeView(btnForward)
            removeView(txtMoveByMove)
            removeView(txtSquares)
            removeView(txtMoves)
            for (thumbView in thumbViewSquares) removeView(thumbView)
            for (thumbView in thumbViewBorderSquares) removeView(thumbView)
            removeView(thumbView_regret)
        }
        hideImageViews()
        val fragment = supportFragmentManager.findFragmentByTag("rank")
        if (fragment != null) supportFragmentManager.beginTransaction().remove(fragment).commit()

        toastHistoricalGames.cancel()
        toastRules.cancel()
        toastCongratulations.cancel()
    }

    private fun enterMainMode() {
        topConstraintLayout.apply {
            addView(txtTopTop)
            addView(txtTopBottom)
        }
        arrowCoversToFront()
        displayDialogStart()
    }

    private fun displayDialogStart() {
        dialogStart = DialogStart(this)
        dialogStart.setCancelable(false)
        dialogStart.show()
    }

    private fun enterGameMode() {
        topConstraintLayout.apply {
            //addView(adView)
            addView(chronometer)
        }
        val adRequest = AdRequest.Builder().build()
        //adView.loadAd(adRequest)
        squareCompletedOne = -1
        squareCompletedTwo = -1
        haveWon = false
        arrowsToFront()
        switch1 = false
        switch2 = false
        setRotation(false)
        chronometer.setTextColor(Color.WHITE)
        chronometer.base = SystemClock.elapsedRealtime()
        chronometer.start()
        switch4 = true
        initDiamonds()
    }

    private fun enterHistoricalMode() {

        topConstraintLayout.apply {
            addView(chronometer)
            addView(btnBackToRank)
            addView(txtRank)
            addView(btnWatch)
            //addView(btnShare)
        }

        constraintLayout.apply {
            addView(txtSquares)
            addView(txtMoves)
            addView(btnBack)
            addView(btnForward)
            addView(txtMoveByMove)
        }
        btnWatch.text = resources.getString(R.string.str_btnWatch_watch)
        btnWatch.setTextColor(Color.BLACK)
        btnBackToRank.isEnabled = true
        btnBack.isEnabled = true
        btnForward.isEnabled = false
        btnBackToRank.alpha = 1f
        btnBack.alpha = 1f
        btnForward.alpha = 0.2f
        txtMoveByMove.alpha = 1f

        /*
        if (viewModel.gameSelected.user == FirebaseAuth.getInstance().currentUser?.uid) {
            btnShare.isEnabled = true
            btnShare.alpha = 1f
        }
        else {
            btnShare.isEnabled = false
            btnShare.alpha = 0.2f
        }
        */

        arrowsToFront()
        hideImageViews()
    }

    private fun setStats(state: Int) {
        if (state == -1) {
            txtSquares.text =
                resources.getString(R.string.str_txtSquares) + "\n" + "${viewModel.gameSelected.squaresCompleted}/${viewModel.gameSelected.squaresCompleted}"
            txtMoves.text =
                resources.getString(R.string.str_txtMoves) + "\n" + "${viewModel.gameSelected.moves.size}/${viewModel.gameSelected.moves.size}"
            chronometer.base =
                SystemClock.elapsedRealtime() - viewModel.gameSelected.moves.last().timeStamp
            if (viewModel.gameSelected.moves.last().timeStamp == viewModel.gameSelected.duration) chronometer.setTextColor(
                Color.YELLOW
            )
            else chronometer.setTextColor(Color.WHITE)
        } else {
            if (state != 0) {
                txtSquares.text =
                    resources.getString(R.string.str_txtSquares) + "\n" + "${viewModel.gameSelected.moves[state - 1].completedSquares}/${viewModel.gameSelected.squaresCompleted}"
                chronometer.base =
                    SystemClock.elapsedRealtime() - viewModel.gameSelected.moves[state - 1].timeStamp
                if (viewModel.gameSelected.moves[state - 1].timeStamp == viewModel.gameSelected.duration) chronometer.setTextColor(
                    Color.YELLOW
                )
                else chronometer.setTextColor(Color.WHITE)
            } else {
                txtSquares.text =
                    resources.getString(R.string.str_txtSquares) + "\n" + "0/${viewModel.gameSelected.squaresCompleted}"
                chronometer.base = SystemClock.elapsedRealtime()
                chronometer.setTextColor(Color.WHITE)
            }
            txtMoves.text =
                resources.getString(R.string.str_txtMoves) + "\n" + "$state/${viewModel.gameSelected.moves.size}"
        }
    }

    private fun setRank(prompt: Boolean = false) {
        if (viewModel.gameSelected.squaresCompleted == 0) {
            txtRank.text = resources.getString(R.string.strNoRank) +
                    "\n${viewModel.gameSelected.endTime.substring(0, 10)}"
        } else {
            if (positionMyGames != -1L && positionAllGames == -1L) {
                txtRank.text =
                    "#$positionMyGames ${resources.getString(R.string.str_txtRank_your)}\n" +
                            "${viewModel.gameSelected.endTime.substring(0, 10)}"
            } else if (positionMyGames == -1L && positionAllGames != -1L) {
                txtRank.text =
                    "#$positionAllGames ${resources.getString(R.string.str_txtRank_all)}\n" +
                            "${viewModel.gameSelected.endTime.substring(0, 10)}"
            } else if (positionMyGames != -1L && positionAllGames != -1L) {
                txtRank.text =
                    "#$positionMyGames ${resources.getString(R.string.str_txtRank_your)}\n" +
                            "#$positionAllGames ${resources.getString(R.string.str_txtRank_all)}\n" +
                            "${viewModel.gameSelected.endTime.substring(0, 10)}"
                if (prompt && viewModel.gameSelected.squaresCompleted == 18) {
                    //toastCongratulations.setText("${R.string.str_toastCongratulations_1}$positionAllGames${R.string.str_toastCongratulations_2}")
                    toastCongratulations.show()
                }
                /*
                if (prompt) {
                    alertDialog = alertDialogBuilderClaimPrompt.create()
                    alertDialog.setCancelable(false)
                    alertDialog.show()
                }
                else {
                    // toastClaim.show()
                }
                 */
            } else {

            }
        }
    }

    private fun enterPlayMode() {

        topConstraintLayout.apply {
            addView(chronometer)
            addView(btnBackToRank)
            addView(txtRank)
            addView(btnWatch)
            //addView(btnShare)
        }


        constraintLayout.apply {
            addView(txtSquares)
            addView(txtMoves)
            addView(btnBack)
            addView(btnForward)
            addView(txtMoveByMove)
            for (thumbView in thumbViewSquares) addView(thumbView)
            for (thumbView in thumbViewBorderSquares) addView(thumbView)
            addView(thumbView_regret)
        }

        btnWatch.text = resources.getString(R.string.str_btnWatch_stop)
        btnWatch.setTextColor(Color.RED)
        btnBackToRank.isEnabled = false
        //btnShare.isEnabled = false
        btnBack.isEnabled = false
        btnForward.isEnabled = false
        btnBackToRank.alpha = 0.2f
        //btnShare.alpha = 0.2f
        btnBack.alpha = 0.2f
        btnForward.alpha = 0.2f
        txtMoveByMove.alpha = 0.2f
        arrowsToFront()

    }

    private fun enterRulesMode() {

        topConstraintLayout.apply {
            addView(txtRulesHeadline)
            addView(txtRulesBody)
        }

        constraintLayout.apply {
            addView(btnBack)
            addView(btnForward)
            for (thumbView in thumbViewSquares) addView(thumbView)
            for (thumbView in thumbViewBorderSquares) addView(thumbView)
            addView(thumbView_regret)
        }
        btnBack.isEnabled = true
        btnForward.isEnabled = true
        btnBack.alpha = 0.2f
        btnForward.alpha = 1f
        arrowsToFront()
        rules = 0
    }

    private fun arrowsToFront() {
        imageView_TopLeft.bringToFront()
        imageView_TopRight.bringToFront()
        imageView_BottomLeft.bringToFront()
        imageView_BottomRight.bringToFront()
    }

    private fun arrowCoversToFront() {
        topLeftCover.bringToFront()
        topRightCover.bringToFront()
        bottomLeftCover.bringToFront()
        bottomRightCover.bringToFront()
    }

    private fun squaresToFront() {
        for (imageView__ in imageViewSquares__) imageView__.bringToFront()
        //for (imageView in imageViewSquares) imageView.bringToFront()
        arrowsToFront() // Won't the arrows be covered by the non-transparent gameView?
        gameView.bringToFront()
    }

    private fun bordersToFront() {
        for (imageView in imageViewBorderSquares) imageView.bringToFront()
        arrowsToFront() // Won't the arrows be covered by the non-transparent gameView?
        gameView.bringToFront()
    }


    private fun hideImageViews() {
        for (imageView in imageViewSquares) imageView.alpha = 0f
        for (imageView in imageViewBorderSquares) imageView.alpha = 0f
        if (mode == PLAY_MODE || mode == RULES_MODE) {
            for (thumbView in thumbViewSquares) thumbView.alpha = 0f
            for (thumbView in thumbViewBorderSquares) thumbView.alpha = 0f
            thumbView_regret.alpha = 0f
        }
    }

    private fun initViews() {
        guidelineIntersections.add(
            GuidelineIntersection(
                vgl_1,
                hgl_4
            )
        )
        guidelineIntersections.add(
            GuidelineIntersection(
                vgl_1,
                hgl_6
            )
        )
        guidelineIntersections.add(
            GuidelineIntersection(
                vgl_1,
                hgl_8
            )
        )

        guidelineIntersections.add(
            GuidelineIntersection(
                vgl_2,
                hgl_3
            )
        )
        guidelineIntersections.add(
            GuidelineIntersection(
                vgl_2,
                hgl_4
            )
        )
        guidelineIntersections.add(
            GuidelineIntersection(
                vgl_2,
                hgl_5
            )
        )
        guidelineIntersections.add(
            GuidelineIntersection(
                vgl_2,
                hgl_6
            )
        )
        guidelineIntersections.add(
            GuidelineIntersection(
                vgl_2,
                hgl_7
            )
        )
        guidelineIntersections.add(
            GuidelineIntersection(
                vgl_2,
                hgl_8
            )
        )
        guidelineIntersections.add(
            GuidelineIntersection(
                vgl_2,
                hgl_9
            )
        )

        guidelineIntersections.add(
            GuidelineIntersection(
                vgl_3,
                hgl_2
            )
        )
        guidelineIntersections.add(
            GuidelineIntersection(
                vgl_3,
                hgl_3
            )
        )
        guidelineIntersections.add(
            GuidelineIntersection(
                vgl_3,
                hgl_4
            )
        )
        guidelineIntersections.add(
            GuidelineIntersection(
                vgl_3,
                hgl_5
            )
        )
        guidelineIntersections.add(
            GuidelineIntersection(
                vgl_3,
                hgl_6
            )
        )
        guidelineIntersections.add(
            GuidelineIntersection(
                vgl_3,
                hgl_7
            )
        )
        guidelineIntersections.add(
            GuidelineIntersection(
                vgl_3,
                hgl_8
            )
        )
        guidelineIntersections.add(
            GuidelineIntersection(
                vgl_3,
                hgl_9
            )
        )
        guidelineIntersections.add(
            GuidelineIntersection(
                vgl_3,
                hgl_10
            )
        )

        guidelineIntersections.add(
            GuidelineIntersection(
                vgl_4,
                hgl_1
            )
        )
        guidelineIntersections.add(
            GuidelineIntersection(
                vgl_4,
                hgl_2
            )
        )
        guidelineIntersections.add(
            GuidelineIntersection(
                vgl_4,
                hgl_3
            )
        )
        guidelineIntersections.add(
            GuidelineIntersection(
                vgl_4,
                hgl_4
            )
        )
        guidelineIntersections.add(
            GuidelineIntersection(
                vgl_4,
                hgl_5
            )
        )
        guidelineIntersections.add(
            GuidelineIntersection(
                vgl_4,
                hgl_6
            )
        )
        guidelineIntersections.add(
            GuidelineIntersection(
                vgl_4,
                hgl_7
            )
        )
        guidelineIntersections.add(
            GuidelineIntersection(
                vgl_4,
                hgl_8
            )
        )
        guidelineIntersections.add(
            GuidelineIntersection(
                vgl_4,
                hgl_9
            )
        )
        guidelineIntersections.add(
            GuidelineIntersection(
                vgl_4,
                hgl_10
            )
        )
        guidelineIntersections.add(
            GuidelineIntersection(
                vgl_4,
                hgl_11
            )
        )

        guidelineIntersections.add(
            GuidelineIntersection(
                vgl_5,
                hgl_2
            )
        )
        guidelineIntersections.add(
            GuidelineIntersection(
                vgl_5,
                hgl_3
            )
        )
        guidelineIntersections.add(
            GuidelineIntersection(
                vgl_5,
                hgl_4
            )
        )
        guidelineIntersections.add(
            GuidelineIntersection(
                vgl_5,
                hgl_5
            )
        )
        guidelineIntersections.add(
            GuidelineIntersection(
                vgl_5,
                hgl_6
            )
        )
        guidelineIntersections.add(
            GuidelineIntersection(
                vgl_5,
                hgl_7
            )
        )
        guidelineIntersections.add(
            GuidelineIntersection(
                vgl_5,
                hgl_8
            )
        )
        guidelineIntersections.add(
            GuidelineIntersection(
                vgl_5,
                hgl_9
            )
        )
        guidelineIntersections.add(
            GuidelineIntersection(
                vgl_5,
                hgl_10
            )
        )

        guidelineIntersections.add(
            GuidelineIntersection(
                vgl_6,
                hgl_3
            )
        )
        guidelineIntersections.add(
            GuidelineIntersection(
                vgl_6,
                hgl_4
            )
        )
        guidelineIntersections.add(
            GuidelineIntersection(
                vgl_6,
                hgl_5
            )
        )
        guidelineIntersections.add(
            GuidelineIntersection(
                vgl_6,
                hgl_6
            )
        )
        guidelineIntersections.add(
            GuidelineIntersection(
                vgl_6,
                hgl_7
            )
        )
        guidelineIntersections.add(
            GuidelineIntersection(
                vgl_6,
                hgl_8
            )
        )
        guidelineIntersections.add(
            GuidelineIntersection(
                vgl_6,
                hgl_9
            )
        )

        guidelineIntersections.add(
            GuidelineIntersection(
                vgl_7,
                hgl_4
            )
        )
        guidelineIntersections.add(
            GuidelineIntersection(
                vgl_7,
                hgl_6
            )
        )
        guidelineIntersections.add(
            GuidelineIntersection(
                vgl_7,
                hgl_8
            )
        )

        imageViewSquares.add(imageView0)
        imageViewSquares.add(imageView1)
        imageViewSquares.add(imageView2)
        imageViewSquares.add(imageView3)
        imageViewSquares.add(imageView4)
        imageViewSquares.add(imageView5)
        imageViewSquares.add(imageView6)
        imageViewSquares.add(imageView7)
        imageViewSquares.add(imageView8)
        imageViewSquares.add(imageView9)
        imageViewSquares.add(imageView10)
        imageViewSquares.add(imageView11)
        imageViewSquares.add(imageView12)
        imageViewSquares.add(imageView13)
        imageViewSquares.add(imageView14)
        imageViewSquares.add(imageView15)
        imageViewSquares.add(imageView16)
        imageViewSquares.add(imageView17)
        imageViewSquares.add(imageView18)

        imageViewBorderSquares.add(imageView0_1)
        imageViewBorderSquares.add(imageView0_2)
        imageViewBorderSquares.add(imageView1_3)
        imageViewBorderSquares.add(imageView1_4)
        imageViewBorderSquares.add(imageView2_4)
        imageViewBorderSquares.add(imageView2_5)
        imageViewBorderSquares.add(imageView3_6)
        imageViewBorderSquares.add(imageView4_6)
        imageViewBorderSquares.add(imageView4_7)
        imageViewBorderSquares.add(imageView5_7)
        imageViewBorderSquares.add(imageView6_8)
        imageViewBorderSquares.add(imageView7_10)
        imageViewBorderSquares.add(imageView8_11)
        imageViewBorderSquares.add(imageView10_12)
        imageViewBorderSquares.add(imageView11_13)
        imageViewBorderSquares.add(imageView11_14)
        imageViewBorderSquares.add(imageView12_14)
        imageViewBorderSquares.add(imageView12_15)
        imageViewBorderSquares.add(imageView13_16)
        imageViewBorderSquares.add(imageView14_16)
        imageViewBorderSquares.add(imageView14_17)
        imageViewBorderSquares.add(imageView15_17)
        imageViewBorderSquares.add(imageView16_18)
        imageViewBorderSquares.add(imageView17_18)

        thumbViewSquares.add(thumbView_square0)
        thumbViewSquares.add(thumbView_square1)
        thumbViewSquares.add(thumbView_square2)
        thumbViewSquares.add(thumbView_square3)
        thumbViewSquares.add(thumbView_square4)
        thumbViewSquares.add(thumbView_square5)
        thumbViewSquares.add(thumbView_square6)
        thumbViewSquares.add(thumbView_square7)
        thumbViewSquares.add(thumbView_square8)
        thumbViewSquares.add(thumbView_square9)
        thumbViewSquares.add(thumbView_square10)
        thumbViewSquares.add(thumbView_square11)
        thumbViewSquares.add(thumbView_square12)
        thumbViewSquares.add(thumbView_square13)
        thumbViewSquares.add(thumbView_square14)
        thumbViewSquares.add(thumbView_square15)
        thumbViewSquares.add(thumbView_square16)
        thumbViewSquares.add(thumbView_square17)
        thumbViewSquares.add(thumbView_square18)

        thumbViewBorderSquares.add(thumbView_borderSquare0_1)
        thumbViewBorderSquares.add(thumbView_borderSquare0_2)
        thumbViewBorderSquares.add(thumbView_borderSquare1_3)
        thumbViewBorderSquares.add(thumbView_borderSquare1_4)
        thumbViewBorderSquares.add(thumbView_borderSquare2_4)
        thumbViewBorderSquares.add(thumbView_borderSquare2_5)
        thumbViewBorderSquares.add(thumbView_borderSquare3_6)
        thumbViewBorderSquares.add(thumbView_borderSquare4_6)
        thumbViewBorderSquares.add(thumbView_borderSquare4_7)
        thumbViewBorderSquares.add(thumbView_borderSquare5_7)
        thumbViewBorderSquares.add(thumbView_borderSquare6_8)
        thumbViewBorderSquares.add(thumbView_borderSquare7_10)
        thumbViewBorderSquares.add(thumbView_borderSquare8_11)
        thumbViewBorderSquares.add(thumbView_borderSquare10_12)
        thumbViewBorderSquares.add(thumbView_borderSquare11_13)
        thumbViewBorderSquares.add(thumbView_borderSquare11_14)
        thumbViewBorderSquares.add(thumbView_borderSquare12_14)
        thumbViewBorderSquares.add(thumbView_borderSquare12_15)
        thumbViewBorderSquares.add(thumbView_borderSquare13_16)
        thumbViewBorderSquares.add(thumbView_borderSquare14_16)
        thumbViewBorderSquares.add(thumbView_borderSquare14_17)
        thumbViewBorderSquares.add(thumbView_borderSquare15_17)
        thumbViewBorderSquares.add(thumbView_borderSquare16_18)
        thumbViewBorderSquares.add(thumbView_borderSquare17_18)

        thumbViewToBeRemoved = thumbViewSquares[9]

        for (imageView in imageViewSquares) {
            //imageView.setOnTouchListener(this)
            //imageView.setOnDragListener(this)
            imageView.alpha = 0f
        }

        for (imageView in imageViewBorderSquares) {
            imageView.setOnDragListener(this)
            imageView.alpha = 0f
        }

        topConstraintLayout.setOnDragListener(this)
        constraintLayout.setOnDragListener(this)
        parentLayout.setOnDragListener(this)

        chronometer.onChronometerTickListener =
            Chronometer.OnChronometerTickListener { chronometer ->
                if (mode == GAME_MODE && switch4) {
                    if (chronometer.text == "59:00") {
                        chronometer.setTextColor(Color.RED)
                        Toast.makeText(this, R.string.str_toastAlmostTimeUp, Toast.LENGTH_SHORT)
                            .show()
                    }
                    if (chronometer.text == "59:59") {
                        chronometer.stop()
                        Toast.makeText(this, R.string.str_toastTimeUp, Toast.LENGTH_SHORT).show()
                        viewModel.timeUp(SystemClock.elapsedRealtime() - chronometer.base)
                    }
                }
            }


       imageViewSquares__.add(imageView__0)
       imageViewSquares__.add(imageView__1)
       imageViewSquares__.add(imageView__2)
       imageViewSquares__.add(imageView__3)
       imageViewSquares__.add(imageView__4)
       imageViewSquares__.add(imageView__5)
       imageViewSquares__.add(imageView__6)
       imageViewSquares__.add(imageView__7)
       imageViewSquares__.add(imageView__8)
       imageViewSquares__.add(imageView__9)
       imageViewSquares__.add(imageView__10)
       imageViewSquares__.add(imageView__11)
       imageViewSquares__.add(imageView__12)
       imageViewSquares__.add(imageView__13)
       imageViewSquares__.add(imageView__14)
       imageViewSquares__.add(imageView__15)
       imageViewSquares__.add(imageView__16)
       imageViewSquares__.add(imageView__17)
       imageViewSquares__.add(imageView__18)

       for (imageView__ in imageViewSquares__) {
           imageView__.setOnTouchListener(this)
           imageView__.setOnDragListener(this)
           imageView__.alpha = 0f
       }



    }

    private fun initDiamonds() {

        val path = Path()

        for (index in 0 until viewModel.squares.size) {

            val x1 = guidelineIntersections[viewModel.squares[index].c1].x.x
            val y1 = guidelineIntersections[viewModel.squares[index].c1].y.y

            val x2 = guidelineIntersections[viewModel.squares[index].c2].x.x
            val y2 = guidelineIntersections[viewModel.squares[index].c2].y.y
            val side1 = x2 - x1
            val side2 = y1 - y2
            val length = sqrt((side1*side1) +(side2*side2))

            Log.e("TAG", "foo: side1 = ${side1} side2 = ${side2} length = ${length}")

            path.moveTo(
                0f,
                side2
            )
            Log.e("TAG", "foo is : ${guidelineIntersections[viewModel.squares[index].c1].x.x}")


            path.lineTo(
                side1,
                0f
            )
            path.lineTo(
                side1+side1,
                side2
            )
            path.lineTo(
                side1,
                side2+side2
            )
            path.close()

            val diamondView = imageViewSquares[index] as DiamondView
            diamondView.drawDiamond(path)

        }

    }

    private fun initAlertDialogs() {
        alertDialogBuilderNewGame = AlertDialog.Builder(this)
        alertDialogBuilderNewGame.setTitle(resources.getString(R.string.str_aldNewGame_title))
        alertDialogBuilderNewGame.setMessage(resources.getString(R.string.str_aldNewGame_message))
        alertDialogBuilderNewGame.setPositiveButton(
            resources.getString(R.string.str_aldNewGame_positive)
        ) { dialog, which ->
            alertDialog.cancel()
            mode = GAME_MODE
        }
        alertDialogBuilderNewGame.setNegativeButton(
            resources.getString(R.string.str_aldNewGame_negative)
        ) { dialog, which ->
            alertDialog.cancel()
        }

        alertDialogBuilderMainMenu = AlertDialog.Builder(this)
        alertDialogBuilderMainMenu.setTitle(resources.getString(R.string.str_aldNewGame_title))
        alertDialogBuilderMainMenu.setMessage(R.string.str_aldMainMenu_message)
        alertDialogBuilderMainMenu.setPositiveButton(
            resources.getString(R.string.str_aldMainMenu_positive),
            DialogInterface.OnClickListener { dialog, which ->
                alertDialog.cancel()
                mode = MAIN_MODE
            })
        alertDialogBuilderMainMenu.setNegativeButton(
            resources.getString(R.string.str_aldMainMenu_negative)
        ) { dialog, which ->
            alertDialog.cancel()
        }

        alertDialogBuilderRankRules = AlertDialog.Builder(this)
        alertDialogBuilderRankRules.setTitle(resources.getString(R.string.str_aldRankRules_title))
        alertDialogBuilderRankRules.setMessage(R.string.str_aldRankRules)
        alertDialogBuilderRankRules.setPositiveButton(
            resources.getString(R.string.str_aldRankRules_positive),
            DialogInterface.OnClickListener { dialog, which ->
                alertDialog.cancel()
            })
        /*
        alertDialogBuilderClaimPrompt = AlertDialog.Builder(this)
        alertDialogBuilderClaimPrompt.setTitle(resources.getString(R.string.str_aldClaimPrompt_title))
        alertDialogBuilderClaimPrompt.setMessage(R.string.str_aldClaimPrompt)
        alertDialogBuilderClaimPrompt.setPositiveButton(
            resources.getString(R.string.str_aldClaimPrompt_positive),
            DialogInterface.OnClickListener { dialog, which ->
                alertDialog.cancel()
                supportFragmentManager.beginTransaction()
                    .add(R.id.parentLayout, Claim.newInstance("", ""), "claim_2").commit()
            })
        alertDialogBuilderClaimPrompt.setNegativeButton(
            resources.getString(R.string.str_aldClaimPrompt_negative),
            DialogInterface.OnClickListener { dialog, which ->
                alertDialog.cancel()
            })
        */
    }


    private fun update() {
        linearLayout.removeView(gameView)
        if (mode == HISTORICAL_MODE || mode == PLAY_MODE) {
            gameView.alpha = 0.5f
            chronometer.alpha = 0.5f
            imageView_TopLeft.alpha = 0.5f
            imageView_TopRight.alpha = 0.5f
            imageView_BottomLeft.alpha = 0.5f
            imageView_BottomRight.alpha = 0.5f
        } else {
            gameView.alpha = 1f
            chronometer.alpha = 1f
            imageView_TopLeft.alpha = 1f
            imageView_TopRight.alpha = 1f
            imageView_BottomLeft.alpha = 1f
            imageView_BottomRight.alpha = 1f
        }
        linearLayout.addView(gameView)
    }


    private fun setRotation(switch: Boolean) {
        switch3 = switch
        if (switch3) {
            imageView_TopLeft.setImageResource(R.drawable.ic_arrow_topleft_2)
            imageView_TopRight.setImageResource(R.drawable.ic_arrow_topright_2)
            imageView_BottomLeft.setImageResource(R.drawable.ic_arrow_bottomleft_2)
            imageView_BottomRight.setImageResource(R.drawable.ic_arrow_bottomright_2)
        } else {
            imageView_TopLeft.setImageResource(R.drawable.ic_arrow_topleft)
            imageView_TopRight.setImageResource(R.drawable.ic_arrow_topright)
            imageView_BottomLeft.setImageResource(R.drawable.ic_arrow_bottomleft)
            imageView_BottomRight.setImageResource(R.drawable.ic_arrow_bottomright)
        }
    }

    private fun invertRotation() {
        switch3 = !switch3
        if (switch3) {
            imageView_TopLeft.setImageResource(R.drawable.ic_arrow_topleft_2)
            imageView_TopRight.setImageResource(R.drawable.ic_arrow_topright_2)
            imageView_BottomLeft.setImageResource(R.drawable.ic_arrow_bottomleft_2)
            imageView_BottomRight.setImageResource(R.drawable.ic_arrow_bottomright_2)
        } else {
            imageView_TopLeft.setImageResource(R.drawable.ic_arrow_topleft)
            imageView_TopRight.setImageResource(R.drawable.ic_arrow_topright)
            imageView_BottomLeft.setImageResource(R.drawable.ic_arrow_bottomleft)
            imageView_BottomRight.setImageResource(R.drawable.ic_arrow_bottomright)
        }
    }


    inner class GameView(context: Context) : View(context) {

        private val paintBlack = Paint()

        init {
            paintBlack.apply {
                color = Color.BLACK
                style = Paint.Style.STROKE
                strokeJoin = Paint.Join.ROUND
                strokeCap = Paint.Cap.ROUND
                strokeWidth = resources.getDimension(R.dimen.stroke_size_black)
                isAntiAlias = true
            }
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val paint = Paint()

            paint.style = Paint.Style.FILL
            paint.isAntiAlias = true

            for (triangle in viewModel.triangles) {
                paint.color = triangle.color
                val path = Path()
                path.fillType = FillType.EVEN_ODD
                path.moveTo(
                    guidelineIntersections[triangle.c1].x.x,
                    guidelineIntersections[triangle.c1].y.y
                )
                path.lineTo(
                    guidelineIntersections[triangle.c2].x.x,
                    guidelineIntersections[triangle.c2].y.y
                )
                path.lineTo(
                    guidelineIntersections[triangle.c3].x.x,
                    guidelineIntersections[triangle.c3].y.y
                )
                path.close()
                canvas.drawPath(path, paint)
            }

            canvas.drawLine(
                guidelineIntersections[2].x.x,
                guidelineIntersections[2].y.y,
                guidelineIntersections[29].x.x,
                guidelineIntersections[29].y.y,
                paintBlack
            )
            canvas.drawLine(
                guidelineIntersections[1].x.x,
                guidelineIntersections[1].y.y,
                guidelineIntersections[38].x.x,
                guidelineIntersections[38].y.y,
                paintBlack
            )
            canvas.drawLine(
                guidelineIntersections[0].x.x,
                guidelineIntersections[0].y.y,
                guidelineIntersections[45].x.x,
                guidelineIntersections[45].y.y,
                paintBlack
            )
            canvas.drawLine(
                guidelineIntersections[3].x.x,
                guidelineIntersections[3].y.y,
                guidelineIntersections[48].x.x,
                guidelineIntersections[48].y.y,
                paintBlack
            )
            canvas.drawLine(
                guidelineIntersections[10].x.x,
                guidelineIntersections[10].y.y,
                guidelineIntersections[47].x.x,
                guidelineIntersections[47].y.y,
                paintBlack
            )
            canvas.drawLine(
                guidelineIntersections[19].x.x,
                guidelineIntersections[19].y.y,
                guidelineIntersections[46].x.x,
                guidelineIntersections[46].y.y,
                paintBlack
            )
            canvas.drawLine(
                guidelineIntersections[29].x.x,
                guidelineIntersections[29].y.y,
                guidelineIntersections[48].x.x,
                guidelineIntersections[48].y.y,
                paintBlack
            )
            canvas.drawLine(
                guidelineIntersections[18].x.x,
                guidelineIntersections[18].y.y,
                guidelineIntersections[47].x.x,
                guidelineIntersections[47].y.y,
                paintBlack
            )
            canvas.drawLine(
                guidelineIntersections[9].x.x,
                guidelineIntersections[9].y.y,
                guidelineIntersections[46].x.x,
                guidelineIntersections[46].y.y,
                paintBlack
            )
            canvas.drawLine(
                guidelineIntersections[2].x.x,
                guidelineIntersections[2].y.y,
                guidelineIntersections[39].x.x,
                guidelineIntersections[39].y.y,
                paintBlack
            )
            canvas.drawLine(
                guidelineIntersections[1].x.x,
                guidelineIntersections[1].y.y,
                guidelineIntersections[30].x.x,
                guidelineIntersections[30].y.y,
                paintBlack
            )
            canvas.drawLine(
                guidelineIntersections[0].x.x,
                guidelineIntersections[0].y.y,
                guidelineIntersections[19].x.x,
                guidelineIntersections[19].y.y,
                paintBlack
            )

            if (mode == GAME_MODE && !haveWon && squareCompletedOne != -1) {
                paint.color = Color.WHITE
                val path = Path()
                path.fillType = FillType.EVEN_ODD
                path.moveTo(
                    guidelineIntersections[viewModel.squares[squareCompletedOne].c1].x.x,
                    guidelineIntersections[viewModel.squares[squareCompletedOne].c1].y.y
                )
                path.lineTo(
                    guidelineIntersections[viewModel.squares[squareCompletedOne].c2].x.x,
                    guidelineIntersections[viewModel.squares[squareCompletedOne].c2].y.y
                )
                path.lineTo(
                    guidelineIntersections[viewModel.squares[squareCompletedOne].c3].x.x,
                    guidelineIntersections[viewModel.squares[squareCompletedOne].c3].y.y
                )
                path.lineTo(
                    guidelineIntersections[viewModel.squares[squareCompletedOne].c4].x.x,
                    guidelineIntersections[viewModel.squares[squareCompletedOne].c4].y.y
                )
                path.close()
                canvas.drawPath(path, paint)
                if (squareCompletedTwo != -1) {
                    val path = Path()
                    path.fillType = FillType.EVEN_ODD
                    path.moveTo(
                        guidelineIntersections[viewModel.squares[squareCompletedTwo].c1].x.x,
                        guidelineIntersections[viewModel.squares[squareCompletedTwo].c1].y.y
                    )
                    path.lineTo(
                        guidelineIntersections[viewModel.squares[squareCompletedTwo].c2].x.x,
                        guidelineIntersections[viewModel.squares[squareCompletedTwo].c2].y.y
                    )
                    path.lineTo(
                        guidelineIntersections[viewModel.squares[squareCompletedTwo].c3].x.x,
                        guidelineIntersections[viewModel.squares[squareCompletedTwo].c3].y.y
                    )
                    path.lineTo(
                        guidelineIntersections[viewModel.squares[squareCompletedTwo].c4].x.x,
                        guidelineIntersections[viewModel.squares[squareCompletedTwo].c4].y.y
                    )
                    path.close()
                    canvas.drawPath(path, paint)
                }
                squareCompletedOne = -1
                squareCompletedTwo = -1
                handlerUpdate.postDelayed(delayedUpdate, delayUpdate)
                return
            }

            if (mode == GAME_MODE && haveWon) {
                for (square in viewModel.squares) {
                    paint.color = Color.WHITE
                    val path = Path()
                    path.fillType = FillType.EVEN_ODD
                    path.moveTo(
                        guidelineIntersections[square.c1].x.x,
                        guidelineIntersections[square.c1].y.y
                    )
                    path.lineTo(
                        guidelineIntersections[square.c2].x.x,
                        guidelineIntersections[square.c2].y.y
                    )
                    path.lineTo(
                        guidelineIntersections[square.c3].x.x,
                        guidelineIntersections[square.c3].y.y
                    )
                    path.lineTo(
                        guidelineIntersections[square.c4].x.x,
                        guidelineIntersections[square.c4].y.y
                    )
                    path.close()
                    canvas.drawPath(path, paint)
                }
                handlerUpdate.postDelayed(delayedUpdate, delayUpdate * 3)
                return
            }

            if (mode == HISTORICAL_MODE || mode == PLAY_MODE) {
                if (viewModel.rewindState == -1 || viewModel.rewindState == viewModel.gameSelected.moves.size) {
                    for (borderSquare in viewModel.borderSquares) {
                        if (borderSquare.switch) {
                            paint.style = Paint.Style.STROKE
                            paint.strokeWidth = resources.getDimension(R.dimen.stroke_size_white)
                            paint.color = Color.WHITE
                            val path = Path()
                            path.fillType = FillType.EVEN_ODD
                            path.moveTo(
                                guidelineIntersections[borderSquare.c1].x.x,
                                guidelineIntersections[borderSquare.c1].y.y
                            )
                            path.lineTo(
                                guidelineIntersections[borderSquare.c2].x.x,
                                guidelineIntersections[borderSquare.c2].y.y
                            )
                            path.lineTo(
                                guidelineIntersections[borderSquare.c4].x.x,
                                guidelineIntersections[borderSquare.c4].y.y
                            )
                            path.lineTo(
                                guidelineIntersections[borderSquare.c3].x.x,
                                guidelineIntersections[borderSquare.c3].y.y
                            )
                            path.close()
                            canvas.drawPath(path, paint)
                        }
                    }
                }
                return
            }

            if (mode == RULES_MODE && rules == 2) {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = resources.getDimension(R.dimen.stroke_size_white)
                paint.color = Color.WHITE
                val path = Path()
                path.fillType = FillType.EVEN_ODD
                path.moveTo(
                    guidelineIntersections[viewModel.borderSquares[1].c1].x.x,
                    guidelineIntersections[viewModel.borderSquares[1].c1].y.y
                )
                path.lineTo(
                    guidelineIntersections[viewModel.borderSquares[1].c2].x.x,
                    guidelineIntersections[viewModel.borderSquares[1].c2].y.y
                )
                path.lineTo(
                    guidelineIntersections[viewModel.borderSquares[1].c4].x.x,
                    guidelineIntersections[viewModel.borderSquares[1].c4].y.y
                )
                path.lineTo(
                    guidelineIntersections[viewModel.borderSquares[1].c3].x.x,
                    guidelineIntersections[viewModel.borderSquares[1].c3].y.y
                )
                path.close()
                canvas.drawPath(path, paint)
            }

        }
    }

}

data class GuidelineIntersection(val x: Guideline, val y: Guideline)

class MyDragShadowBuilder : View.DragShadowBuilder() {
    override fun onProvideShadowMetrics(outShadowSize: Point, outShadowTouchPoint: Point) {
        outShadowSize.set(1, 1)
        outShadowTouchPoint.set(0, 0)
    }
}
