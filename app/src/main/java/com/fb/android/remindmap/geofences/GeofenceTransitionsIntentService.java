// Copyright 2004-present Facebook. All Rights Reserved.

package com.fb.android.remindmap.geofences;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

import com.fb.android.remindmap.fresh.ListDispatchActivity;
import com.fb.android.remindmap.R;
import com.fb.android.remindmap.fresh.PTask;
import com.fb.android.remindmap.fresh.PTaskListActivity;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by easmith on 7/16/15.
 */

public class GeofenceTransitionsIntentService extends IntentService {

    protected static final String TAG = "geofence-transitions-service";

    public GeofenceTransitionsIntentService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d("onHandleIntent", "Fired");

        GeofencingEvent geofencingEvent = GeofencingEvent.fromIntent(intent); //info of transition
        if (geofencingEvent.hasError()) {
            String errorMessage = GeofenceErrorMessages.getErrorString(
                    this,
                    geofencingEvent.getErrorCode());
            Log.e("Eliz", errorMessage);
            return;
        }

        // returns geofence transition type (GEOFENCE_TRANSITION_ENTER)
        int geofenceTransition = geofencingEvent.getGeofenceTransition();

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) { //FIXME + INITIAL_TRIGGER_ENTER???

            // Get the geofences that were triggered. A single event (brought to us by intent) can trigger multiple geofences (multiple tasks located within same location--different/overlapping geofences though).
            List<Geofence> triggeringGeofences = geofencingEvent.getTriggeringGeofences();

            List<String> taskIds = getTaskIds(triggeringGeofences);

            for (int i = 0; i < taskIds.size(); i++) {
                sendNotificationFromId(taskIds.get(i));
            }

        } else {
            // Log the error.
            Log.e("Eliz", getString(R.string.geofence_transition_invalid_type, geofenceTransition));
        }
    }

    public List<String> getTaskIds(List<Geofence> triggeringGeofences) {

        // Get the IDs of each geofence that was triggered.
        List<String> taskIds = new ArrayList<String>();
        for (Geofence geofence : triggeringGeofences) {
            taskIds.add(geofence.getRequestId()); //task's UUID (not ObjectId)
        }

        return taskIds;
    }

    public void sendNotificationFromId(final String taskId) {
        ParseQuery<PTask> query = PTask.getQuery();
        query.fromLocalDatastore();
        query.orderByDescending("date");
        query.whereEqualTo("uuid", taskId); //whereContainedIn?
        query.findInBackground(new FindCallback<PTask>() {
            @Override
            public void done(List<PTask> tasks, ParseException e) {
                String taskTitle = "";
                if (e == null) {
                    int requestCode = 0; //TODO membervariable
                    for (int i = 0; i < tasks.size(); i++) {
                        taskTitle = tasks.get(i).getTitle() + "!";
                        PTask task = tasks.get(i);
                        Toast.makeText(GeofenceTransitionsIntentService.this, "You're near the task: " + taskTitle, Toast.LENGTH_SHORT).show();
                        sendNotification(task, requestCode);
                        requestCode++;
                    }
                }
            }
        });
    }

    private void sendNotification(PTask task, int requestCode) {
        long when = System.currentTimeMillis();

        Intent notificationIntent = new Intent(this, PTaskListActivity.class); //getApplicationContext()?
        notificationIntent.putExtra("ID", task.getObjectId()).putExtra("requestCode", requestCode).putExtra("LIST_ID", task.getListId());

        // Construct a task stack.
        //StackBuilder holds artificial backstack for started activity (so proper backward navigation to home screen)
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

        //Adds backstack for intent (but not intent itself); adds task fragment to the task stack as the parent
        stackBuilder.addParentStack(ListDispatchActivity.class);

        // Push the Intent (that starts activity) onto stack.
        stackBuilder.addNextIntent(notificationIntent);

        //TODO manually fire pending intent to load correct task fragment (so onHandle intent loads correct task fragment with extra)
        //TODO gen 2 notif with 2 tasks IDS so each point to diff task ID
        //TODO PENDINT DOC (change request code integer-part of param list for getAct)
        // Gets PendingIntent containing entire back stack

        //requestCode different for each task that triggers different notification
        PendingIntent notificationPendingIntent = stackBuilder.getPendingIntent(requestCode, PendingIntent.FLAG_ONE_SHOT); //TODO (FLAG_UPDATE_CURRENT?); send task id as extra on intent

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.mipmap.ic_launcher_gem)
                .setContentTitle(task.getTitle())
                .setContentText("You're near " + task.getLocTitle())
                .setTicker("RemindMap Alert")
                .setContentIntent(notificationPendingIntent)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setWhen(when);
        //.setVibrate();

        // Dismiss notification once the user touches it
        builder.setAutoCancel(true);

        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Send notification
        mNotificationManager.notify((int) when, builder.build());
        Log.d("sendNotification", "notification sent");
    }
}
