package com.trianxiety.myapp.viewmodel

import android.graphics.Color

data class Triangle(val c1: Int, val c2: Int, val c3: Int, var color: Int = Color.TRANSPARENT)

data class Square(
    val t1: Triangle,
    val t2: Triangle,
    val t3: Triangle,
    val t4: Triangle,
    val c1: Int,
    val c2: Int,
    val c3: Int,
    val c4: Int,
    var switch: Boolean = false
)

data class BorderSquare(
    val t1: Triangle,
    val t2: Triangle,
    val c1: Int,
    val c2: Int,
    val c3: Int,
    val c4: Int,
    var switch: Boolean = false
)