package com.trianxiety.myapp

fun convertTimeStamp(timeStamp: Long): String {
    val minutes = (timeStamp / 1000) / 60
    val seconds = (timeStamp / 1000) % 60
    var minutesString = minutes.toString()
    if (minutes < 10) minutesString = "0$minutesString"
    var secondsString = seconds.toString()
    if (seconds < 10) secondsString = "0$secondsString"
    return "$minutesString:$secondsString"
}

fun getIndex(tag: String): Int {
    when (tag) {
        "0" -> return 0
        "1" -> return 1
        "2" -> return 2
        "3" -> return 3
        "4" -> return 4
        "5" -> return 5
        "6" -> return 6
        "7" -> return 7
        "8" -> return 8
        "9" -> return 9
        "10" -> return 10
        "11" -> return 11
        "12" -> return 12
        "13" -> return 13
        "14" -> return 14
        "15" -> return 15
        "16" -> return 16
        "17" -> return 17
        "18" -> return 18
        else -> return -1
    }
}