// Copyright 2004-present Facebook. All Rights Reserved.

package com.fb.android.remindmap.fresh;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.fb.android.remindmap.R;
import com.parse.GetCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;

/**
 * Created by judyl on 7/20/15.
 */

public class AlertReceiver extends BroadcastReceiver {

    // Called when a broadcast is made targeting this class
    @Override
    public void onReceive(final Context context, Intent i) {

        String taskId = null;

        if (i.hasExtra("ID")) {
            taskId = i.getExtras().getString("ID");
        }

        ParseQuery<PTask> query = PTask.getQuery();
        query.fromLocalDatastore();
        query.whereEqualTo("uuid", taskId);
        query.getFirstInBackground(new GetCallback<PTask>() {

            @Override
            public void done(PTask object, ParseException e) {
                if (e == null) {
                    PTask task = object;
                    createNotification(context, task);
                    Log.d("AlertReceiver", "Retrieved: " + task.getTitle());
                } else {
                    Log.d("AlertReceiver", "Something went wrong (lol)");
                }
            }
        });
    }

    public void createNotification(Context context, PTask task) {

        // Define an Intent and an action to perform with it by another application
        Intent i = new Intent(context, PTaskListActivity.class);
        i.putExtra("ID", task.getObjectId());
        i.putExtra("LIST_ID", task.getListId());
        PendingIntent pI = PendingIntent.getActivity(context, 0, i, PendingIntent.FLAG_UPDATE_CURRENT);

        // Builds a notification
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.mipmap.ic_launcher_gem)
                .setContentTitle(task.getTitle())
                .setContentText("Due Now")
                .setTicker("RemindMap Alert");

        // Defines the Intent to fire when the notification is clicked
        mBuilder.setContentIntent(pI);

        mBuilder.setDefaults(Notification.DEFAULT_VIBRATE);
        mBuilder.setAutoCancel(true);

        // Gets NotificationManager used to notify user in background event
        NotificationManager mNotificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Post unique notification
        mNotificationManager.notify(task.getUuidString().hashCode(), mBuilder.build());
    }

}
