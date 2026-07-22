package com.dejongdevelopment.golfps.tools

import android.os.Handler
import android.os.Looper

interface PlayerUpdateTimerDelegate {
    fun updatePlayersNow()
}

class PlayerUpdateTimer {
    private val handler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null

    var delegate: PlayerUpdateTimerDelegate? = null

    fun startNewTimer(interval: Double, triggerImmediately: Boolean = false) {
        val currentDelegate = delegate
        invalidate()
        delegate = currentDelegate

        val intervalMs = (interval * 1000).toLong()
        val runnable = object : Runnable {
            override fun run() {
                delegate?.updatePlayersNow()
                handler.postDelayed(this, intervalMs)
            }
        }

        timerRunnable = runnable
        handler.postDelayed(runnable, intervalMs)

        if (triggerImmediately) {
            delegate?.updatePlayersNow()
        }
    }

    fun invalidate() {
        timerRunnable?.let { handler.removeCallbacks(it) }
        timerRunnable = null
        delegate = null
    }
}
