// Copyright 2004-present Facebook. All Rights Reserved.

package com.fb.android.remindmap.geofences;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.fb.android.remindmap.R;
import com.fb.android.remindmap.fresh.PTask;
import com.fb.android.remindmap.interfaces.LocationFoundInterface;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by easmith on 7/13/15.
 */

public class LocationManager implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, ResultCallback<Status> {

    //Entry pt to Google Play Services (act/frags don't know about this, only use inst of LocationManager)
    protected GoogleApiClient mGoogleApiClient;

    private Geofence mGeofence;

    //Keep track of whether geofences were added
    private boolean mGeofencesAdded;

    //Used when requesting to add or remove geofences (ie. tasks)
    private PendingIntent mGeofencePendingIntent;

    //Used to persist application state about whether geofences were added
    private SharedPreferences mSharedPreferences;

    protected Location mCurrentLocation;
    private Context mContext;

    private boolean isConnected = false; //flag to ensure GoogleApiClient can connect

    public LocationManager(Context context) {
        mContext = context;
    }

    private LocationFoundInterface mLocationFound;

    //Automatically runs when GoogleApiClient successfully .connect()s
    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
    }

    //must use interface because only way to pass info from LocManager to Map Activity
    public void initialize(LocationFoundInterface locationFoundInterface) {
        buildGoogleApiClient();
        mGoogleApiClient.connect(); //connect() automatically calls onConnected() if successful

        // Initially set the PendingIntent used in addGeofences() and removeGeofences() to null.
        mGeofencePendingIntent = null;

        // Retrieve an instance of the SharedPreferences object (not necessary right now).
        mSharedPreferences = mContext.getSharedPreferences(
                LocationConstants.SHARED_PREFERENCES_NAME,
                Context.MODE_PRIVATE);

        // Get the value of mGeofencesAdded from SharedPreferences. Set to false as a default.
        mGeofencesAdded = mSharedPreferences.getBoolean(LocationConstants.GEOFENCES_ADDED_KEY, false);
        //setButtonsEnabledState();

        //Builds the geofences used. Geofence data is hard coded for testing purposes.
        //populateGeofenceList();

        mLocationFound = locationFoundInterface;
    }

    public void terminate() {
        mGoogleApiClient.disconnect();
    }

    public Location getLocation() {
        if (isConnected) {
            return mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        }
        return null;
    }

    @Override
    public void onConnected(Bundle bundle) {
        //TODO: add callback back.

        isConnected = true;
        getLocation();
//        displayToast();

        //manual firing of pendingIntent
//        try {
//            getGeofencePendingIntent().send();
//        }
//        catch (PendingIntent.CanceledException e){
//            return;
//        }

        //FixMe: Following line commented out for testing of transfer from LatLng to ParseGeoPoint.

//        addGeofencesButtonHandler(); //automatic firing of pending intent by Google Api cloud

        if (mLocationFound != null) {
            mLocationFound.onLocationFound();
        }

    }

    public void displayToast() {
        String msg = "Current Location: " +
                Double.toString(mCurrentLocation.getLatitude()) + "," +
                Double.toString(mCurrentLocation.getLongitude());
        Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onConnectionSuspended(int i) {
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        isConnected = false;
    }

    //Builds GeofencingRequest that specifies the list of geofences to be monitored (depends on number of tasks).
    //Specifies how geofence notifications are initially triggered

    private GeofencingRequest getGeofencingRequest(List<Geofence> geofences) {

        GeofencingRequest.Builder builder = new GeofencingRequest.Builder();
        Log.d("getGeofencingRequest", "building GEO request");

        // The INITIAL_TRIGGER_ENTER flag indicates that geofencing service should trigger a
        // GEOFENCE_TRANSITION_ENTER notification if the device
        // is already inside that geofence (ie testing within Building 20).
        builder.setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER); //GEOFENCE_TRANSITION_ENTER should be triggered when device is already in fence

        // Add the geofences to be monitored by geofencing service.
        builder.addGeofences(geofences);
        Log.d("getGeofencingRequest", "Num geofences added to request is: " + geofences.size());

        // Return a GeofencingRequest.
        return builder.build();
    }

    //Adds geofences, which sets alerts to be notified when the device enters or exits one of the
    //specified geofences. Handles the success or failure results returned by addGeofences()

    //updates/sends list of geofences in Google API (creates new geofenceRequest from newly built list of geofences (request first calls
    //buildGeofences, which queries parse for all tasks) and sends with same pendingIntent)

    //overrides whatever geofences are currently in Google API (by calling removeGeofences)
    //called upon saving task in PTaskFragment
    //TODO geofencing request, cancel geof associated with prev pending intent and sent new request associ with new geof (same pend intent tho) --one method inside locat manager (so we call one line here)
    public void publishGeofences() {
        //not connected to Google Api cloud/Play Store
        if (!mGoogleApiClient.isConnected()) {
            Toast.makeText(mContext, mContext.getString(R.string.not_connected), Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            wipeGeofences();
            queryTasks(); //TODO change order??
            //Log.d("LocManager", "Task size: FUN");
        } catch (SecurityException securityException) {
            // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
            logSecurityException(securityException);
        }
    }

    // Removes geofences, which stops further notifications when the device enters or exits
    // previously registered geofences
    // (saves battery life)

    public void wipeGeofences() {
        if (!mGoogleApiClient.isConnected()) {
            //Toast.makeText(this, getString(R.string.not_connected), Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            // Remove geofences
            LocationServices.GeofencingApi.removeGeofences(
                    mGoogleApiClient,
                    // This is the same pending intent that was used in addGeofences().
                    getGeofencePendingIntent()
            ).setResultCallback(this); // Result processed in onResult()
        } catch (SecurityException securityException) {
            // Catch exception generated if the app does not use ACCESS_FINE_LOCATION permission.
            logSecurityException(securityException);
        }
    }

    private void logSecurityException(SecurityException securityException) {
        Log.e("location manager", "Invalid location permission. " + "You need to use ACCESS_FINE_LOCATION with geofences", securityException);
    }


    // Runs when the result of calling addGeofences() and removeGeofences() becomes available.
    // Since we implement ResultCallback interface, we must define onResult()
    // Status: the status returned through a PendingIntent when addGeofences() or removeGeofences() get called.
    public void onResult(Status status) {
        if (status.isSuccess()) {
            // Update state and save in shared preferences.
            mGeofencesAdded = !mGeofencesAdded;
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            editor.putBoolean(LocationConstants.GEOFENCES_ADDED_KEY, mGeofencesAdded);
            editor.commit();
        }
    }

    //Gets a PendingIntent to send with the geofencing request to add or remove Geofences. Location Services (GoogleApiClient)
    //fires Intent inside this PendingIntent whenever a geofence transition occurs for the current list of geofences.
    //Returns a PendingIntent for GeofenceTransitionsIntentService (that handles intent sent from Location Serivces when geofence transition occurs)
    private PendingIntent getGeofencePendingIntent() {
        // Reuse the PendingIntent if we already have it
        if (mGeofencePendingIntent != null) {
            return mGeofencePendingIntent;
        }
        Intent intent = new Intent(mContext, GeofenceTransitionsIntentService.class); //pendingIntent starts intent service
        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // addGeofences() and removeGeofences().

        Log.d("getGeofencePendingIntent", "Pending intent fired");
        return PendingIntent.getService(mContext, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    //Dynamically create geofences based on the user's created tasks
    public List<Geofence> buildGeofences(List<PTask> mTasks, List<String> objectIds) {
        List<Geofence> geofences = new ArrayList<Geofence>();
        for (int i = 0; i < mTasks.size(); i++) {

            Log.d("buildGeofences", "task # " + i + " is " + mTasks.get(i));
            Log.d("buildGeofences", "Geofence object ID of " + i + " is " + objectIds.get(i));
            //builds geofence per coordinates specified by Parse task object
            geofences.add(new Geofence.Builder()
                    // Set the request ID of the geofence (unique identifier of this geofence)
                    .setRequestId(objectIds.get(i)) //geofence id is same as parse task object id

                            // Set the circular region of this geofence
                    .setCircularRegion(
                            mTasks.get(i).getGeoPoint().getLatitude(),
                            mTasks.get(i).getGeoPoint().getLongitude(),
                            LocationConstants.GEOFENCE_RADIUS_IN_METERS //(half mile currently)
                    )
                            // Sets expiration duration of the geofence (automatically removed after 12 hours currently):
                    .setExpirationDuration(getGeofenceExpirationDuration(mTasks.get(i)))

                            // Sets the transition types of interest. Alerts are only generated for these
                            // transitions: entry and exit
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER) //FIXME INITIAL_TRIGGER_ENTER??
                    .build());
        }
        Log.d("buildGeofences", "Num geofences made from tasks is " + geofences.size());
        Log.d("buildGeofences", "Request ID of geofence # 0 is " + geofences.get(0).getRequestId());
        return geofences;

    }

    public void queryTasks() {
        final List<String> objectIds = new ArrayList<String>();
        ParseQuery<PTask> query = PTask.getQuery();
        query.fromLocalDatastore(); //grabbing all tasks synced to local database
        query.findInBackground(new FindCallback<PTask>() {
            @Override
            public void done(final List<PTask> tasks, ParseException e) {
                if (e == null) {
                    //TODO network toast
                    for (int i = 0; i < tasks.size(); i++) {
                        objectIds.add(tasks.get(i).getUuidString());
                    }
                    Log.d("done", "Num task IDs : " + objectIds.size());
                    Log.d("done", "first task ID is : " + objectIds.get(0));

                    List<PTask> mTasks = tasks;
                    Log.d("done", "Task size: " + mTasks.size());
                    buildGeofences(mTasks, objectIds);
                    Log.d("done", "Geofence size: " + buildGeofences(mTasks, objectIds).size());
                    //getGeofencingRequest(buildGeofences(mTasks, objectIds));
                    LocationServices.GeofencingApi.addGeofences(
                            mGoogleApiClient,
                            getGeofencingRequest(buildGeofences(mTasks, objectIds)),
                            // A pending intent that that is reused when calling removeGeofences(). This
                            // pending intent is used to generate an intent when a matched geofence
                            // transition is observed.
                            getGeofencePendingIntent()
                    ).setResultCallback(LocationManager.this); // Result processed in onResult().
                }
            }
        });
    }

    public long getGeofenceExpirationDuration(PTask task) { //FIXME custom time
        Log.d("locManager", "date is: " + task.getDate());
        if (task.getCreatedAt() == null) {
            Log.d("LocationManager", "test case");
            return task.getDate().getTime() - new Date().getTime();
        }
        if (task.getDate() == null) {
            Log.d("LocationManager", "geofence date not specified; set to one day");
            return 86400000;  //1 day in milliseconds (if user does not specify date/time)
        } else if (task.getDate().getTime() < task.getCreatedAt().getTime()) {
            Log.d("LocationManager", "geofence expired");
            return 0;
        }
        Log.d("LocationManager", "geofence expires in: " + (task.getDate().getTime() - task.getCreatedAt().getTime()));
        return task.getDate().getTime() - task.getCreatedAt().getTime();
    }
}
