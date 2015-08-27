// Copyright 2004-present Facebook. All Rights Reserved.

package com.fb.android.remindmap.fresh;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ListView;

import com.fb.android.remindmap.R;
import com.parse.ParseUser;

import java.util.ArrayList;

/**
 * Created by etorgas on 8/3/15.
 */
public class RemoveCollaboratorsActivity extends FragmentActivity implements CompoundButton.OnCheckedChangeListener {


    ListView mListView;
    ArrayList<Contact> mCollaboratorsArrayList;
    CollaboratorsAdapter mCollaboratorsAdapter;
    Button mRemoveCollaboratorButton;
    ArrayList<ParseUser> mParseUsersToRemove;
    private ArrayList<ParseUser> mCollaboratorParseUsersArrayList;
    private ArrayList<Contact> mContactsToRemove;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_remove_collaborators);

        mListView = (ListView) findViewById(R.id.collaborator_list_view);
        displayCollaboratorsList();

        mContactsToRemove = new ArrayList<>();

        mRemoveCollaboratorButton = (Button) findViewById(R.id.remove_collaborators_button);
        mRemoveCollaboratorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (Contact collaborator : mCollaboratorsArrayList) {
                    if (collaborator.isSelected()) {
                        mContactsToRemove.add(collaborator);
                    }
                }

                Intent intent = new Intent();
                intent.putParcelableArrayListExtra("contactsToRemove", mContactsToRemove);

                setResult(RESULT_OK, intent);

                finish();
            }
        });

    }

    private void displayCollaboratorsList() {

        Intent intent = this.getIntent();

        mCollaboratorsArrayList = intent.getParcelableArrayListExtra("contacts");

        mCollaboratorsAdapter = new CollaboratorsAdapter(mCollaboratorsArrayList, this);

        mListView.setAdapter(mCollaboratorsAdapter);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        int position = mListView.getPositionForView(buttonView);
        if (position != ListView.INVALID_POSITION) {
            Contact contact = mCollaboratorsArrayList.get(position);
            contact.setSelected(isChecked);
        }
    }
}
