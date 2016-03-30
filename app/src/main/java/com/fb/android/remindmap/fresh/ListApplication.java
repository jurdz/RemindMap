// Copyright 2004-present Facebook. All Rights Reserved.

package com.fb.android.remindmap.fresh;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.util.Base64;
import android.util.Log;

import com.parse.Parse;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseFacebookUtils;
import com.parse.ParseInstallation;
import com.parse.ParseObject;
import com.parse.ParsePush;
import com.parse.PushService;
import com.parse.SaveCallback;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Created by judyl on 7/7/15.
 */
public class ListApplication extends android.app.Application {

    public static final String TASK_GROUP_NAME = "ALL_TASKS";
    public static final String TASK_LIST_GROUP_NAME = "TALL_TASK_LISTS";
    public static final String APPLICATION_ID = "8D6udHxxsP3yGhvY8dY7k14p1PtMXyahvvlbCOGt";
    public static final String CLIENT_KEY = "BX5WcyAELsYbNxu3aKwxugPUBG26BWTYQ39yf6H4";

    @Override
    public void onCreate() {
        super.onCreate();

        // Registering ParseObject PTaskList
        ParseObject.registerSubclass(PTaskList.class);

        // Registering ParseObject PTask
        ParseObject.registerSubclass(PTask.class);

        // We want a local database too
        Parse.enableLocalDatastore(getApplicationContext());

        Parse.initialize(this, APPLICATION_ID, CLIENT_KEY);
        ParseFacebookUtils.initialize(this);

        // User may edit ParseObject PTask
        ParseACL defaultACL = new ParseACL();
        ParseACL.setDefaultACL(defaultACL, true);

        // Push Service
        ParseInstallation.getCurrentInstallation().saveInBackground();
        PushService.setDefaultPushCallback(this, PTaskListActivity.class);
        //TODO: https://www.parse.com/docs/android/api/com/parse/ParsePushBroadcastReceiver.html

        ParsePush.subscribeInBackground("", new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    Log.d("com.parse.push", "successfully subscribed to the broadcast channel.");
                } else {
                    Log.e("com.parse.push", "failed to subscribe for push", e);
                }
            }
        });

        try {
            PackageInfo info = getPackageManager().getPackageInfo(
                    "your.package",
                    PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA");
                md.update(signature.toByteArray());
                Log.d("KeyHash:", Base64.encodeToString(md.digest(), Base64.DEFAULT));
            }
        } catch (PackageManager.NameNotFoundException e) {

        } catch (NoSuchAlgorithmException e) {

        }
    }
}
