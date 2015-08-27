// Copyright 2004-present Facebook. All Rights Reserved.

package com.fb.android.remindmap.interfaces;

import com.parse.ParseGeoPoint;

import java.io.Serializable;

/**
 * Created by etorgas on 7/19/15.
 */
public interface LocationPickedInterface  extends Serializable{
    void onLocationPicked(String title, ParseGeoPoint geoPoint);
}
