package com.trianxiety.myapp

import android.app.Application
import com.jakewharton.threetenabp.AndroidThreeTen

class TrianxietyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        AndroidThreeTen.init(this)
    }
}