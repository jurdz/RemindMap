// Copyright 2004-present Facebook. All Rights Reserved.

package com.fb.android.remindmap.maps;

/**
 * Created by etorgas on 7/13/15.
 */
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PlaceDetailsJSONParser {

    /** Receives a JSONObject and returns a list */
    public List<HashMap<String,String>> parse(JSONObject jObject){

        Double lat = Double.valueOf(0);
        Double lng = Double.valueOf(0);
        String formattedAddress = "";

        HashMap<String, String> hm = new HashMap<>();
        List<HashMap<String, String>> list = new ArrayList<>();

        try {
            lat = (Double) jObject.getJSONObject("result").getJSONObject("geometry").getJSONObject("location").get("lat");
            lng = (Double) jObject.getJSONObject("result").getJSONObject("geometry").getJSONObject("location").get("lng");
            formattedAddress = (String) jObject.getJSONObject("result").get("formatted_address");

        } catch(Exception e){
            e.printStackTrace();
        }

        hm.put("lat", Double.toString(lat));
        hm.put("lng", Double.toString(lng));
        hm.put("formatted_address",formattedAddress);

        list.add(hm);

        return list;
    }
}
