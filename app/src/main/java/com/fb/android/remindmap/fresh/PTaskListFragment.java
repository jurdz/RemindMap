// Copyright 2004-present Facebook. All Rights Reserved.

package com.fb.android.remindmap.fresh;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.fb.android.remindmap.R;
import com.fb.android.remindmap.geofences.LocationManager;
import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.ParseAnonymousUtils;
import com.parse.ParseException;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseQueryAdapter;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by judyl on 7/14/15.
 */

public class PTaskListFragment extends Fragment {

    private static final String ARG_TASK_LIST_ID = "task_list_id";
    private static final int EDIT_ACTIVITY_CODE = 200;
    private static final int REQUEST_CONTACTS = 4;
    private static final int REQUEST_CLEAR = 3;
    private static final int REQUEST_REMOVE_COLLABORATORS = 5;

    private PTaskList mTaskList;
    private String taskListId = null;

    private Callbacks mCallbacks;
    private ArrayList<Contact> mSelectedContacts;
    private ArrayList<ParseUser> mSelectedParseUsers;
    private ArrayList<ParseUser> mCollaborators = new ArrayList<>();

    private ParseQueryAdapter<PTask> taskListAdapter;
    private LayoutInflater inflater;

    private FloatingActionButton addTask;
    private ListView taskListView;
    private LinearLayout noTaskView;

    private LocationManager mLocationManager;
    private Location onStartLocation;

    public static PTaskListFragment newInstance(String taskListId) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_TASK_LIST_ID, taskListId);

        PTaskListFragment fragment = new PTaskListFragment();
        fragment.setArguments(args);
        return fragment;
    }

    // Required interface for hosting activities.
    public interface Callbacks {
        void onTaskSelected(PTask task, PTaskList taskList);

        void onTaskListUpdated(PTaskList taskList);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Fetch the taskListId
        taskListId = (String) getArguments().getSerializable(ARG_TASK_LIST_ID);

        setHasOptionsMenu(true);

        inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.p_fragment_task_list, container, false);

        // Set up Location Manager
        mLocationManager = new LocationManager(getActivity());
        mLocationManager.initialize(null);
        onStartLocation = mLocationManager.getLocation();

        // Set up the views
        addTask = (FloatingActionButton) v.findViewById(R.id.fab);
        taskListView = (ListView) v.findViewById(R.id.task_list_view);
        noTaskView = (LinearLayout) v.findViewById(R.id.no_task_view);
        taskListView.setEmptyView(noTaskView);

        ParseQuery<PTaskList> query = PTaskList.getQuery();
        query.fromLocalDatastore();
        query.whereEqualTo("objectId", taskListId);
        query.getFirstInBackground(new GetCallback<PTaskList>() {

            @Override
            public void done(PTaskList object, ParseException e) {
                if (e == null) {
                    mTaskList = object;
                    finishView(v, mTaskList);
                }
            }
        });
        return v;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    public void finishView(View v, final PTaskList taskList) {

        // Set up the Parse query to use in the adapter
        ParseQueryAdapter.QueryFactory<PTask> factory = new ParseQueryAdapter.QueryFactory<PTask>() {
            public ParseQuery<PTask> create() {
                ParseQuery<PTask> query = PTask.getQuery();
                query.whereEqualTo("listId", taskList.getObjectId());
                query.orderByAscending("date");
                query.fromLocalDatastore();
                return query;
            }
        };

        // Set up the adapter
        taskListAdapter = new TaskListAdapter(getActivity(), factory);

        // Attach the query adapter to the view
        ListView taskListView = (ListView) v.findViewById(R.id.task_list_view);
        taskListView.setAdapter(taskListAdapter);

        taskListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                PTask task = taskListAdapter.getItem(position);
                mCallbacks.onTaskSelected(task, taskList);
            }
        });

        addTask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                mCallbacks.onTaskSelected(null, mTaskList);
            }
        });

        ((AppCompatActivity) getActivity()).getSupportActionBar()
                .setTitle((CharSequence) taskList.getTitle());  //cause of error occassionally when "title" in menu bar does not load fast enough
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CONTACTS && resultCode == ContactsPickerActivity.RESULT_OK) {
            //FIXME: Moved initialization of mSelectedParseUsers into if statement.
            mSelectedParseUsers = new ArrayList<>();
            mSelectedContacts = data.getParcelableArrayListExtra("selectedContacts");
            for (Contact contact : mSelectedContacts) {
                String formatedPhoneNumber = contact.getPhoneNumber().replaceAll("[()\\s-]+", "");
                ParseQuery<ParseUser> phoneNumberQuery = ParseUser.getQuery();
                try {
                    ParseUser user = phoneNumberQuery.whereEqualTo("phoneNumber", formatedPhoneNumber).getFirst();
                    if (mSelectedParseUsers != null) {
                        boolean userAlreadySaved = false;
                        for (ParseUser selectedUser : mTaskList.getCollaborators()) {
                            if (selectedUser == user) {
                                userAlreadySaved = true;
                                break;
                            }
                        }
                        if (!userAlreadySaved) {
                            mSelectedParseUsers.add(user);
                        }
                    }
                } catch (ParseException e) {
                    Log.i("TAG", "", e);
                }
            }

            ParseQuery<PTaskList> query = PTaskList.getQuery();
            query.fromLocalDatastore();
            query.whereEqualTo("objectId", taskListId);
            try {
                mTaskList = query.getFirst();
            } catch (ParseException e) {
                Log.i("TAG", "", e);
            }
            mTaskList.addCollaborators(mSelectedParseUsers);
            mTaskList.saveEventually();
        }

        if (requestCode == REQUEST_REMOVE_COLLABORATORS &&
                resultCode == ContactsRemoverActivity.RESULT_OK) {
            ArrayList<Contact> contactsToRemove = data.getParcelableArrayListExtra("contactsToRemove");

            ArrayList<ParseUser> collaboratorParseUsers = new ArrayList<>(mTaskList.getCollaborators());
            //TODO: Parse query for users by matching phone numbers in contactsToRemove, then remove them.
            for (Contact contact : contactsToRemove) {
                try {
                    ParseQuery<ParseUser> parseUserQuery = ParseUser.getQuery();
                    parseUserQuery.whereEqualTo("phoneNumber", contact.getPhoneNumber());
                    ParseUser userToRemove = parseUserQuery.getFirst();
                    mTaskList.removeCollaborator(userToRemove, mCollaborators);
                } catch (ParseException e) {
                    Log.i("TAG", "", e);
                }
            }
        }

        // An OK result means the pinned dataset changed
        else if (resultCode == getActivity().RESULT_OK) {
            taskListAdapter.loadObjects();
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_task_list, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.action_sync:
                syncTasksToParse();
                return true;

            case R.id.action_view_collaborators:
                mCollaborators = (ArrayList<ParseUser>) mTaskList.getCollaborators();
                collaboratorsDialog(mCollaborators);
                return true;

            case R.id.action_delete_task_list:
                deleteTaskListDialog();
                return true;

            case R.id.action_logout:// Log out the current user
                deleteListFromLocal();
                deleteTasksFromLocal();
                ParseUser.logOut();
                Intent i = new Intent(getActivity(), ListDispatchActivity.class);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void syncTasksToParse() {
        // We could use saveEventually here, but we want to have some UI
        // around whether or not the draft has been saved to Parse
        ConnectivityManager cm = (ConnectivityManager)
                getActivity().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if ((ni != null) && (ni.isConnected())) {
            if (!ParseAnonymousUtils.isLinked(ParseUser.getCurrentUser())) {
                // If we have a network connection and a current logged in user, sync the todos.
                // In this app, local changes should overwrite content on the server.
                ParseQuery<PTask> query = PTask.getQuery();
                query.fromPin(ListApplication.TASK_GROUP_NAME);
                query.whereEqualTo("isDraft", true);
                query.findInBackground(new FindCallback<PTask>() {
                    public void done(List<PTask> tasks, ParseException e) {
                        if (e == null) {
                            for (final PTask task : tasks) {
                                // Set is draft flag to false before
                                // syncing to Parse
                                task.setDraft(false);
                                task.saveInBackground(new SaveCallback() {
                                    @Override
                                    public void done(ParseException e) {
                                        if (e == null) {
                                            // Let adapter know to update view
                                            if (e == null) {
                                                taskListAdapter.notifyDataSetChanged();
                                            }
                                        } else {
                                            // Reset the is draft flag locally to true
                                            task.setDraft(true);
                                        }
                                    }
                                });

                            }
                        } else {
                            Log.i("TaskListActivity",
                                    "syncTasksToParse: Error finding pinned tasks: "
                                            + e.getMessage());
                        }
                    }
                });
                deleteTasksFromLocal();
                loadFromParse();
            }
        } else {
            // If there is no connection, let the user know the sync didn't happen
            Toast.makeText(
                    getActivity().getApplicationContext(),
                    "Your device appears to be offline. Some tasks may not have been synced to Parse.",
                    Toast.LENGTH_LONG).show();
        }
    }

    private void loadFromParse() {
        ParseQuery<PTask> query = PTask.getQuery();
        query.whereEqualTo("listId", taskListId);
        query.findInBackground(new FindCallback<PTask>() {
            public void done(List<PTask> tasks, ParseException e) {
                if (e == null) {
                    ParseObject.pinAllInBackground((List<PTask>) tasks,
                            new SaveCallback() {
                                public void done(ParseException e) {
                                    if (e == null) {
                                        taskListAdapter.loadObjects();
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

    public void deleteListFromLocal() {
        ParseQuery<PTaskList> query = PTaskList.getQuery();
        query.fromLocalDatastore();
        query.findInBackground(new FindCallback<PTaskList>() {
            public void done(List<PTaskList> taskLists, ParseException e) {
                if (e == null) {
                    ParseObject.unpinAllInBackground(taskLists);
                } else {
                    Log.i("TaskListActivity",
                            "deleteListsFromLocal: Error finding pinned lists: " + e.getMessage());
                }
            }
        });
    }

    public void deleteTasksFromLocal() {
        ParseQuery<PTask> query = PTask.getQuery();
        query.fromLocalDatastore();
        query.findInBackground(new FindCallback<PTask>() {
            public void done(List<PTask> taskLists, ParseException e) {
                if (e == null) {
                    ParseObject.unpinAllInBackground(taskLists);
                } else {
                    Log.i("TaskListActivity",
                            "deleteTasksFromLocal: Error finding pinned todos: " + e.getMessage());
                }
            }
        });
    }

    private class TaskListAdapter extends ParseQueryAdapter<PTask> {

        public TaskListAdapter(Context context,
                               ParseQueryAdapter.QueryFactory<PTask> queryFactory) {
            super(context, queryFactory);
        }

        @Override
        public View getItemView(final PTask task, View view, ViewGroup parent) {
            final ViewHolder holder;
            if (view == null) {
                view = inflater.inflate(R.layout.p_list_item_task, parent, false);
                holder = new ViewHolder();
                holder.mTitleTextView = (TextView) view.findViewById(R.id.list_item_title_text_view);
                holder.mDateTextView = (TextView) view.findViewById(R.id.list_item_date_text_view);
                holder.mLocationTextView = (TextView) view.findViewById(R.id.list_item_location_text_view);
                holder.mDoneCheckBox = (CheckBox) view.findViewById(R.id.list_item_completed_check_box);
                view.setTag(holder);
            } else {
                holder = (ViewHolder) view.getTag();
            }

            TextView taskTitle = holder.mTitleTextView;
            TextView taskDate = holder.mDateTextView;
            TextView taskLocation = holder.mLocationTextView;
            CheckBox taskDone = holder.mDoneCheckBox;

            // UI around whether or not a draft has been saved to Parse
            if (task.isDraft()) {
                taskTitle.setTypeface(null, Typeface.BOLD);
            } else {
                taskTitle.setTypeface(null, Typeface.NORMAL);
            }

            taskTitle.setText(task.getTitle());
            taskDone.setChecked(task.isDone());

            // UI around whether or not date/location has been picked
            if (task.getDate() != null) {
                Long duration = task.getDate().getTime() - new Date().getTime();
                if (0 < duration) {
                    String until = getDurationBreakdown(duration);
                    taskDate.setText(until);
                    taskDate.setTextColor(getResources().getColor(R.color.BlueA700));
                } else {
                    taskDate.setText("Overdue");
                    taskDate.setTextColor(getResources().getColor(R.color.RedA200));
                }
            } else {
                taskDate.setText("");
            }

            if (task.getLocTitle() != null) {
                taskLocation.setText(task.getLocTitle());
            } else {
                taskLocation.setText("");
            }

            holder.mDoneCheckBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View buttonView) {
                    task.setDone(!task.isDone());
                    holder.mDoneCheckBox.setChecked(task.isDone());
                    task.saveEventually();
                }
            });

            return view;
        }
    }

    private static class ViewHolder {
        TextView mTitleTextView;
        TextView mDateTextView;
        TextView mLocationTextView;
        CheckBox mDoneCheckBox;
    }

    // Formatting Date string
    public static String getDurationBreakdown(long millis) {
        long days = TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);

        StringBuilder sb = new StringBuilder(64);
        if (days > 1) {
            sb.append(days + " d");
        } else if (days < 1) {
            sb.append(hours + " h");
            if (hours < 1) {
                sb.replace(0, 3, minutes + " m");
            }
        }
        return sb.toString();
    }

    public void collaboratorsDialog(ArrayList<ParseUser> collabs) {

        inflater = LayoutInflater.from(getActivity());

        // get prompts.xml view
        View promptView = inflater.inflate(R.layout.dialog_share, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());

        // set prompts.xml to be the layout file of the alertdialog builder
        alertDialogBuilder.setView(promptView);

        TextView titleText = (TextView) promptView.findViewById(R.id.p_task_list_title);
        ListView shareList = (ListView) promptView.findViewById(R.id.shareListView);
        Button addCollab = (Button) promptView.findViewById(R.id.p_task_list_add);
        Button remCollab = (Button) promptView.findViewById(R.id.p_task_list_rem);

        titleText.setText(mTaskList.getTitle());
        ArrayList<String> collaboratorNames = new ArrayList<>();

        for (ParseUser collaborator : mCollaborators) {
            try {
                collaboratorNames.add(collaborator.fetchIfNeeded().getString("name"));
            } catch (ParseException e) {
                Log.i("TAG", "", e);
            }
        }

        addCollab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                Intent intent = new Intent(getActivity(), ContactsPickerActivity.class);

                startActivityForResult(intent, REQUEST_CONTACTS);
            }
        });

        remCollab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                ArrayList<ParseUser> collabs = new ArrayList<>(mTaskList.getCollaborators());

                ArrayList<Contact> collabContacts = new ArrayList<>();

                for (ParseUser user : collabs) {
                    String name = (String) user.get("name");
                    String phoneNumber = (String) user.get("phoneNumber");

                    collabContacts.add(new Contact(name, phoneNumber));
                }

                Intent intent = new Intent(getActivity(), ContactsRemoverActivity.class);
                intent.putParcelableArrayListExtra("contacts", collabContacts);

                startActivityForResult(intent, REQUEST_REMOVE_COLLABORATORS);
            }
        });

        // Create ArrayAdapter using the title list.
        ArrayAdapter<String> listTitlesAdapter =
                new ArrayAdapter<>(getActivity(), R.layout.share_list_item, collaboratorNames);

        // Set the ArrayAdapter as the ListView's adapter.
        shareList.setAdapter(listTitlesAdapter);

        // setup a dialog window
        alertDialogBuilder
                .setCancelable(true)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        // create an alert dialog
        AlertDialog alertD = alertDialogBuilder.create();
        alertD.show();
    }

    //    public void sendPush() {
//        HashMap<String, Object> params = new HashMap<String, Object>();
//        params.put("recipientId", mSelectedAssignee);
//        params.put("message", "added to task... will edit soon");
//        ParseCloud.callFunctionInBackground("sendPushToUser", params, new FunctionCallback<String>() {
//            public void done(String success, ParseException e) {
//                if (e == null) {
//                    Log.d("push", "sent successfully");
//                }
//            }
//        });
//    }

    public void deleteTaskListDialog() {

        inflater = LayoutInflater.from(getActivity());
        View deleteView = inflater.inflate(R.layout.dialog_delete_list, null);

        final AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setView(deleteView);

        alertDialogBuilder
                .setCancelable(true)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        deleteTaskList();
                        taskListDeletedDialog();
                    }
                });

        alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        // create an alert dialog
        AlertDialog alertD = alertDialogBuilder.create();
        alertD.show();
    }

    private void taskListDeletedDialog() {

        inflater = LayoutInflater.from(getActivity());
        View listDeletedView = inflater.inflate(R.layout.dialog_list_deleted, null);

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setView(listDeletedView);

        alertDialogBuilder
                .setCancelable(true)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                        Intent intent = getActivity().getIntent();
                        getActivity().finish();
                        startActivity(intent);
                    }
                });

        // create an alert dialog
        AlertDialog alertD = alertDialogBuilder.create();
        alertD.show();
    }

    private void deleteTaskList() {
        ParseQuery<PTask> pTaskParseQuery = PTask.getQuery();
        pTaskParseQuery.whereEqualTo("listId", mTaskList.getObjectId());
        try {
            ArrayList<PTask> tasks = (ArrayList<PTask>) pTaskParseQuery.find();
            for (PTask task : tasks) {
                task.delete();
            }
        } catch (ParseException e) {
            Log.i("TAG", "", e);
        }
        try {
            mTaskList.delete();
        } catch (ParseException e) {
            Log.i("TAG", "", e);
        }
    }

    // Changes default behaviour of the overflow menu so that icons display
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (menu != null) {
            if (menu.getClass().getSimpleName().equals("MenuBuilder")) {
                try {
                    Method m = menu.getClass().getDeclaredMethod(
                            "setOptionalIconsVisible", Boolean.TYPE);
                    m.setAccessible(true);
                    m.invoke(menu, true);
                } catch (Exception e) {
                    Log.e(getClass().getSimpleName(), "failed to set icons for overflow menu", e);
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        syncTasksToParse();
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
