package com.trianxiety.myapp.ui

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import com.trianxiety.myapp.R

class DialogStart(context: Context) : Dialog(context, R.style.dialog) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_start)
    }

}