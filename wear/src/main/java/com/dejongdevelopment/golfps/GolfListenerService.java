package com.dejongdevelopment.golfps;

import android.content.Intent;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.WorkerThread;
import android.util.Log;
import android.util.Pair;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Golf Listener Service - Service will operate and accept messages from phone when running in background
 * Created by gdejong on 5/8/17.
 */

public class GolfListenerService extends WearableListenerService {

    private static final String TAG = "GolfListener-Wear";
    private static final String START_ACTIVITY_PATH = "/start-activity";
    private static final String START_RECEIVED_BACKGROUND = "/start-received-background";

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
        if(messageEvent.getPath().equals(START_ACTIVITY_PATH)){

            Intent intent = new Intent(this , DistanceActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

            // Get the node id from the host value of the URI
            String nodeId = messageEvent.getSourceNodeId();

            // Tell the phone that the service caught the start and will start a new activity
            Pair<String,String> nodeMessage = new Pair<String,String>(nodeId, START_RECEIVED_BACKGROUND);
            new SendMobileMessageTask().execute(nodeMessage);
        }
    }

    // ------------ SEND MESSAGES TO MOBILE DEVICE --------------- //
    private class SendMobileMessageTask extends AsyncTask<Pair<String,String>, Void, Void> {
        @Override
        protected Void doInBackground(Pair<String,String>... args) {
            sendMobileMessage(args[0].first, args[0].second);
            return null;
        }
    }
    @WorkerThread
    private void sendMobileMessage(String node, String msg) {
        byte[] data = new byte[0];

        Task<Integer> sendMessageTask = Wearable.getMessageClient(this).sendMessage(node, msg, data);
        try {
            // Block on a task and get the result synchronously (because this is on a background
            // thread).
            Integer result = Tasks.await(sendMessageTask);
            Log.d("TAG", "Message sent: " + result);
        } catch (ExecutionException exception) {
            Log.e("TAG", "Task failed: " + exception);
        } catch (InterruptedException exception) {
            Log.e("TAG", "Interrupt occurred: " + exception);
        }
    }
}
