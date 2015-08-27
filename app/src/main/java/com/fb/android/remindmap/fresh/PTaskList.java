// Copyright 2004-present Facebook. All Rights Reserved.

package com.fb.android.remindmap.fresh;

import com.parse.ParseClassName;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by etorgas on 7/17/15.
 */

@ParseClassName("PTaskList")
public class PTaskList extends ParseObject implements Serializable {

    public String getTitle() {
        return getString("title");
    }

    public void setTitle(String title) {
        put("title", title);
    }

    public List<ParseUser> getCollaborators() {
        return getList("collaborators");
    }

    public void addCollaborators(ParseUser collaborator) {
        ArrayList<ParseUser> collaborators = new ArrayList<>();
        collaborators.add(collaborator);
        put("collaborators", collaborators);
    }

        public void addCollaborators(ArrayList<ParseUser> collaborators) {
        addAll("collaborators", collaborators);
    }

    public void removeCollaborator(ParseUser collaborator, ArrayList<ParseUser> collaborators) {
        collaborators.remove(collaborator);
        put("collaborators", collaborators);
    }

    public static ParseQuery<PTaskList> getQuery() {
        return ParseQuery.getQuery(PTaskList.class);
    }
}
