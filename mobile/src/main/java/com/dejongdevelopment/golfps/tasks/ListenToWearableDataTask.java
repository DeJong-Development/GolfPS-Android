package com.dejongdevelopment.golfps.tasks;

import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.DataItem;

import java.util.concurrent.ExecutionException;

// ------------ SEND DATA TO WEAR DEVICE --------------- //
public class ListenToWearableDataTask extends AsyncTask<Task<DataItem>, Void, Void> {
    @Override
    protected Void doInBackground(Task<DataItem>... args) {
        try {
            // Block on a task and get the result synchronously (because this is on a background
            // thread).
            DataItem dataItem = Tasks.await(args[0]);

            Log.e("TAG", "DataItem saved: " + dataItem);
        } catch (ExecutionException exception) {
            Log.e("TAG", "Task failed: " + exception);
        } catch (InterruptedException exception) {
            Log.e("TAG", "Interrupt occurred: " + exception);
        }
        return null;
    }
}
