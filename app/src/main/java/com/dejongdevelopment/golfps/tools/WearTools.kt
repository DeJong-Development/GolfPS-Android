package com.dejongdevelopment.golfps.tools

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

data class WearGolfState(
    val courseId: String,
    val courseName: String,
    val hole: Int,
    val distance: Int,
    val units: String,
    val club: String
)

interface WearCommandDelegate {
    fun goToNextHoleFromWear()
    fun goToPreviousHoleFromWear()
}

object WearTools : MessageClient.OnMessageReceivedListener {
    const val STATE_PATH = "/golf_state"
    const val COMMAND_PATH = "/golf_command"
    const val COMMAND_NEXT = "gotonext"
    const val COMMAND_PREVIOUS = "gotoprevious"

    private var context: Context? = null
    private var delegate: WearCommandDelegate? = null
    private var isListening = false
    private var lastState: WearGolfState? = null

    fun start(context: Context, delegate: WearCommandDelegate) {
        this.context = context.applicationContext
        this.delegate = delegate
        if (!isListening) {
            Wearable.getMessageClient(context).addListener(this)
            isListening = true
        }
    }

    fun stop() {
        context?.let { Wearable.getMessageClient(it).removeListener(this) }
        delegate = null
        context = null
        isListening = false
    }

    fun update(context: Context, state: WearGolfState) {
        if (lastState == state) return
        lastState = state

        val request = PutDataMapRequest.create(STATE_PATH).apply {
            dataMap.putString("course", state.courseId)
            dataMap.putString("courseName", state.courseName)
            dataMap.putInt("hole", state.hole)
            dataMap.putInt("distance", state.distance)
            dataMap.putString("units", state.units)
            dataMap.putString("club", state.club)
            dataMap.putLong("updatedAt", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()

        Wearable.getDataClient(context).putDataItem(request)
            .addOnFailureListener {
                DebugLogger.report(it, "Error updating Wear OS golf state")
            }
    }

    fun clear(context: Context) {
        lastState = null
        val request = PutDataMapRequest.create(STATE_PATH).apply {
            dataMap.putString("course", "")
            dataMap.putString("courseName", "")
            dataMap.putInt("hole", 0)
            dataMap.putInt("distance", 0)
            dataMap.putString("units", "")
            dataMap.putString("club", "")
            dataMap.putLong("updatedAt", System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()

        Wearable.getDataClient(context).putDataItem(request)
            .addOnFailureListener {
                DebugLogger.report(it, "Error clearing Wear OS golf state")
            }
    }

    override fun onMessageReceived(event: MessageEvent) {
        val command = when {
            event.path == "/$COMMAND_NEXT" -> COMMAND_NEXT
            event.path == "/$COMMAND_PREVIOUS" -> COMMAND_PREVIOUS
            event.path == COMMAND_PATH -> event.data.toString(Charsets.UTF_8)
            else -> return
        }

        when (command) {
            COMMAND_NEXT -> delegate?.goToNextHoleFromWear()
            COMMAND_PREVIOUS -> delegate?.goToPreviousHoleFromWear()
            else -> Log.d("WEAR", "Unknown Wear OS command: $command")
        }
    }
}
