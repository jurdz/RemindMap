// Copyright 2004-present Facebook. All Rights Reserved.

package com.fb.android.remindmap.fresh;

import com.parse.ParseClassName;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.Date;
import java.util.UUID;

/**
 * Created by judyl on 7/14/15.
 */

@ParseClassName("PTask")
public class PTask extends ParseObject {

    public String getUuidString() {
        return getString("uuid");
    }

    public void setUuidString(UUID uuid) {
        put("uuid", uuid.toString());
    }

    public UUID getId() {
        return UUID.fromString(getUuidString());
    }

    public ParseUser getAuthor() {
        return getParseUser("author");
    }

    public void setAuthor(ParseUser currentUser) {
        put("author", currentUser);
    }

    public String getTitle() {
        return getString("title");
    }

    public void setTitle(String title) {
        put("title", title);
    }

    public Date getDate() {
        return getDate("date");
    }

    public void setDate(Date date) {
        put("date", date);
    }

    public ParseGeoPoint getGeoPoint() {
        return new ParseGeoPoint(getDouble("latitude"), getDouble("longitude"));
    }

    public void setGeoPoint(ParseGeoPoint geoPoint) {
        if (geoPoint != null) {
            put("latitude", geoPoint.getLatitude());
            put("longitude", geoPoint.getLongitude());
        }
    }

    public String getLocTitle() {
        return getString("location");
    }

    public ParseUser getAssignee() {
        return getParseUser("assignee");
    }

    public void setAssignee(ParseUser user) {
        put("assignee", user);
    }

    public void setLocTitle(String location) {
        put("location", location);
    }

    public String getListId() {
        return getString("listId");
    }

    public void setListId(String ListId) {
        put("listId", ListId);
    }

    public boolean isDraft() {
        return getBoolean("isDraft");
    }

    public void setDraft(boolean isDraft) {
        put("isDraft", isDraft);
    }

    public boolean isDone() {
        return getBoolean("isDone");
    }

    public void setDone(boolean isDone) {
        put("isDone", isDone);
    }

    public static ParseQuery<PTask> getQuery() {
        return ParseQuery.getQuery(PTask.class);
    }
}
