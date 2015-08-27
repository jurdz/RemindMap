// Copyright 2004-present Facebook. All Rights Reserved.

package com.fb.android.remindmap.geofences;

import com.google.android.gms.maps.model.LatLng;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by easmith on 7/16/15.
 */
public final class LocationConstants {

    private LocationConstants() {
    }

    public static final String PACKAGE_NAME = "com.google.android.gms.location.Geofence";

    public static final String SHARED_PREFERENCES_NAME = PACKAGE_NAME + ".SHARED_PREFERENCES_NAME";

    public static final String GEOFENCES_ADDED_KEY = PACKAGE_NAME + ".GEOFENCES_ADDED_KEY";

    //Used to set an expiration time for a geofence. After this amount of time Location Services
    //stops tracking the geofence (TESTING: geofence expires after 12 hours)
    public static final long GEOFENCE_EXPIRATION_IN_HOURS = 12; //TODO change to due date minus current in ms

    public static final long GEOFENCE_EXPIRATION_IN_MILLISECONDS =
            GEOFENCE_EXPIRATION_IN_HOURS * 60 * 60 * 1000;
    public static final float GEOFENCE_RADIUS_IN_METERS = 1609; // 1 mile (TODO CHANGE upon determining works correctly)

    //mere testing purposes (later will not have hard-coded landmarks but rather leave up to user to specify)
    public static final Map<String, LatLng> BAY_AREA_LANDMARKS = new HashMap<String, LatLng>();

    static {
        // SFO
        BAY_AREA_LANDMARKS.put("SFO", new LatLng(37.621313, -122.378955));

        // MPK :)
        BAY_AREA_LANDMARKS.put("Facebook: Main campus", new LatLng(37.484575, -122.147924));

        // MPK :)
        BAY_AREA_LANDMARKS.put("Facebook: Building 20", new LatLng(37.481242, -122.1543899));

        // Googleplex :(
        BAY_AREA_LANDMARKS.put("GOOGLE", new LatLng(37.422611, -122.0840577));
    }
}
