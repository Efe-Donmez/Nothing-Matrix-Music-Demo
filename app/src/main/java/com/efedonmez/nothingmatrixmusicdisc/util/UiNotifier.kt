package com.efedonmez.nothingmatrixmusicdisc.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.widget.Toast

object UiNotifier {
    private val mainHandler = Handler(Looper.getMainLooper())

    fun show(context: Context, message: String) {
        val appCtx = context.applicationContext
        mainHandler.post {
            Toast.makeText(appCtx, message, Toast.LENGTH_SHORT).show()
        }
    }
}


