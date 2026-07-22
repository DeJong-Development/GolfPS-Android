package com.dejongdevelopment.golfps.wear

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable

class WearGolfActivity : Activity(), DataClient.OnDataChangedListener {
    private lateinit var courseName: TextView
    private lateinit var holeNumber: TextView
    private lateinit var distance: TextView
    private lateinit var units: TextView
    private lateinit var club: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wear_golf)

        courseName = findViewById(R.id.courseName)
        holeNumber = findViewById(R.id.holeNumber)
        distance = findViewById(R.id.distance)
        units = findViewById(R.id.units)
        club = findViewById(R.id.club)

        findViewById<Button>(R.id.previousButton).setOnClickListener {
            sendCommand(COMMAND_PREVIOUS)
        }
        findViewById<Button>(R.id.nextButton).setOnClickListener {
            sendCommand(COMMAND_NEXT)
        }
    }

    override fun onResume() {
        super.onResume()
        Wearable.getDataClient(this).addListener(this)
        Wearable.getDataClient(this).getDataItems()
            .addOnSuccessListener { items ->
                items.firstOrNull { it.uri.path == STATE_PATH }?.let(::displayState)
                items.release()
            }
    }

    override fun onPause() {
        Wearable.getDataClient(this).removeListener(this)
        super.onPause()
    }

    override fun onDataChanged(events: DataEventBuffer) {
        events.filter { it.type == DataEvent.TYPE_CHANGED && it.dataItem.uri.path == STATE_PATH }
            .forEach { displayState(it.dataItem) }
    }

    private fun displayState(item: com.google.android.gms.wearable.DataItem) {
        val state = DataMapItem.fromDataItem(item).dataMap
        val courseId = state.getString("course", "")
        if (courseId.isBlank()) {
            courseName.setText(R.string.waiting_for_round)
            holeNumber.text = "#"
            distance.text = "—"
            units.text = ""
            club.text = ""
            return
        }

        courseName.text = state.getString("courseName", "GolfPS")
        holeNumber.text = "#${state.getInt("hole", 0)}"
        distance.text = state.getInt("distance", 0).toString()
        units.text = state.getString("units", "")
        club.text = state.getString("club", "")
    }

    private fun sendCommand(command: String) {
        Wearable.getNodeClient(this).connectedNodes.addOnSuccessListener { nodes ->
            nodes.forEach { node ->
                Wearable.getMessageClient(this)
                    .sendMessage(node.id, COMMAND_PATH, command.toByteArray(Charsets.UTF_8))
            }
        }
    }

    companion object {
        private const val STATE_PATH = "/golf_state"
        private const val COMMAND_PATH = "/golf_command"
        private const val COMMAND_NEXT = "gotonext"
        private const val COMMAND_PREVIOUS = "gotoprevious"
    }
}
