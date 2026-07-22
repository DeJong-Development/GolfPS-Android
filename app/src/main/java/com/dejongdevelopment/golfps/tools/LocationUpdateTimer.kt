package com.dejongdevelopment.golfps.tools

import android.os.Handler
import android.os.Looper

interface LocationUpdateTimerDelegate {
    fun updateLocationsNow()
}

class LocationUpdateTimer {
    private val handler = Handler(Looper.getMainLooper())
    private var timerRunnable: Runnable? = null

    var delegate: LocationUpdateTimerDelegate? = null

    fun startNewTimer(interval: Double, triggerImmediately: Boolean = true) {
        val currentDelegate = delegate
        invalidate()
        delegate = currentDelegate

        val intervalMs = (interval * 1000).toLong()
        val runnable = object : Runnable {
            override fun run() {
                delegate?.updateLocationsNow()
                handler.postDelayed(this, intervalMs)
            }
        }

        timerRunnable = runnable
        handler.postDelayed(runnable, intervalMs)

        if (triggerImmediately) {
            delegate?.updateLocationsNow()
        }
    }

    fun invalidate() {
        timerRunnable?.let { handler.removeCallbacks(it) }
        timerRunnable = null
        delegate = null
    }
}
