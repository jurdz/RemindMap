// Copyright 2004-present Facebook. All Rights Reserved.

package com.fb.android.remindmap.fresh;

/**
 * Created by judyl on 7/22/15.
 */

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.fb.android.remindmap.R;
import com.parse.FindCallback;
import com.parse.ParseACL;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseQueryAdapter;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.util.List;

public class SpinnerNavigationFragment extends Fragment {

    private static final int REQUEST_CLEAR = 3;

    private ListCallbacks mCallbacks;
    private LayoutInflater inflater;

    private ListView mDrawerList;
    private TaskListAdapter taskListAdapter;

    private Button mNewListButton;
    private PTaskList taskList;

    private RelativeLayout mContentView;

    // Required interface for hosting activities.
    public interface ListCallbacks {
        void onTaskListSelected(PTaskList taskList);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCallbacks = (ListCallbacks) activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        loadFromParse();
        inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.fragment_navigation_spinner, container, false);

        ParseQueryAdapter.QueryFactory<PTaskList> factory = new ParseQueryAdapter.QueryFactory<PTaskList>() {
            public ParseQuery<PTaskList> create() {
                ParseQuery<PTaskList> query = PTaskList.getQuery();
                query.orderByAscending("date");
                query.fromLocalDatastore();
                return query;
            }
        };

        finishView(v, factory);
        return v;
    }

    private void finishView(View v, ParseQueryAdapter.QueryFactory<PTaskList> factory) {

        mContentView = (RelativeLayout) v.findViewById(R.id.drawer_layout);
        mDrawerList = (ListView) v.findViewById(R.id.left_drawer);
        mNewListButton = (Button) v.findViewById(R.id.add_new_list);

        mNewListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {

                AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
                final EditText listName = new EditText(getActivity());
                listName.setInputType(InputType.TYPE_CLASS_TEXT);
                listName.setPadding(100, 0, 100, 25);
                alertDialogBuilder.setView(listName);

                // setup a dialog window
                alertDialogBuilder
                        .setCancelable(false)
                        .setIcon(android.R.drawable.ic_menu_edit)
                        .setTitle("Create New List")
                        .setMessage("Enter Title")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // get user input and set it to result
                                taskList = new PTaskList();
                                taskList.setTitle(listName.getText().toString());
                                taskList.addCollaborators(ParseUser.getCurrentUser());

                                // Setup public read and write access
                                ParseACL ACL = new ParseACL();
                                ACL.setPublicReadAccess(true);
                                ACL.setPublicWriteAccess(true);
                                taskList.setACL(ACL);

                                taskList.pinInBackground(ListApplication.TASK_LIST_GROUP_NAME,
                                        new SaveCallback() {

                                            @Override
                                            public void done(ParseException e) {
                                                if (getActivity().isFinishing()) {
                                                    return;
                                                }
                                                if (e == null) {
                                                    taskList.saveEventually();
                                                    Intent intent = getActivity().getIntent();
                                                    getActivity().finish();
                                                    startActivity(intent);
                                                    mCallbacks.onTaskListSelected(taskList);
                                                } else {
                                                    Toast.makeText(getActivity().getApplicationContext(),
                                                            "Error saving: " + e.getMessage(),
                                                            Toast.LENGTH_LONG).show();
                                                }
                                            }
                                        });
                            }
                        })
                        .setNegativeButton("Cancel",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });

                // create an alert dialog
                AlertDialog alertD = alertDialogBuilder.create();
                alertD.show();
            }
        });

        // Set up the adapter
        taskListAdapter = new TaskListAdapter(getActivity(), factory);

        // Attach the query adapter to the view
        mDrawerList.setAdapter(taskListAdapter);

        mDrawerList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PTaskList taskList = taskListAdapter.getItem(position);
                mCallbacks.onTaskListSelected(taskList);

                mDrawerList.setVisibility(View.GONE);
                mNewListButton.setVisibility(View.GONE);
            }
        });

        mContentView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                mDrawerList.setVisibility(View.GONE);
                mNewListButton.setVisibility(View.GONE);
            }
        });
    }

    // load lists where current user is under the list's collaborators.
    private void loadFromParse() {
        ParseQuery<PTaskList> ownersQuery = PTaskList.getQuery();
        ownersQuery.whereEqualTo("collaborators", ParseUser.getCurrentUser());
        ownersQuery.findInBackground(new FindCallback<PTaskList>() {
            public void done(final List<PTaskList> taskLists, ParseException e) {
                if (e == null) {
                    ParseObject.pinAllInBackground(taskLists,
                            new SaveCallback() {
                                public void done(ParseException e) {
                                    if (e == null) {
                                        taskListAdapter.loadObjects();
                                        Log.d("load", "lists: " + taskLists);
                                    } else {
                                        Log.i("TaskListActivity",
                                                "Error pinning todos: " + e.getMessage());
                                    }
                                }
                            });
                } else {
                    Log.i("TaskListActivity",
                            "loadFromParse: Error finding pinned todos: " + e.getMessage());
                }
            }
        });
    }

    private class TaskListAdapter extends ParseQueryAdapter<PTaskList> {

        public TaskListAdapter(Context context, ParseQueryAdapter.QueryFactory<PTaskList> queryFactory) {
            super(context, queryFactory);
        }

        public View getItemView(final PTaskList taskList, View view, ViewGroup parent) {
            final ViewHolder holder;
            if (view == null) {
                view = inflater.inflate(R.layout.fragment_navigation_spinner_list_item, parent, false);
                holder = new ViewHolder();
                holder.mTitleTextView = (TextView) view.findViewById(R.id.list_item_list_title);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }

            TextView taskListTitle = holder.mTitleTextView;
            taskListTitle.setText(taskList.getTitle());

            return view;
        }
    }

    private static class ViewHolder {
        TextView mTitleTextView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_navigation, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.action_expand:
                mDrawerList.setVisibility(View.VISIBLE);
                mNewListButton.setVisibility(View.VISIBLE);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CLEAR) {
            // TODO Clear task lists
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    // Let this method exist for casting reasons
    public void updateUI() {
    }
}
