// Copyright 2004-present Facebook. All Rights Reserved.

package com.fb.android.remindmap.fresh;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.fb.android.remindmap.geofences.LocationManager;
import com.fb.android.remindmap.R;
import com.fb.android.remindmap.interfaces.DatePickedInterface;
import com.fb.android.remindmap.interfaces.LocationFoundInterface;
import com.fb.android.remindmap.interfaces.LocationPickedInterface;
import com.fb.android.remindmap.maps.MapActivity;
import com.parse.FunctionCallback;
import com.parse.GetCallback;
import com.parse.ParseACL;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;

import java.io.Serializable;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.UUID;

// Copyright 2004-present Facebook.All Rights Reserved.

/**
 * Created by judyl on 7/15/15.
 */

public class PTaskFragment extends Fragment implements DatePickedInterface {

    private static final String ARG_TASK_ID = "task_id";
    private static final String ARG_TASK_LIST_ID = "task_list_id";
    private static final String DIALOG_DATE = "DialogDate";
    private static final int REQUEST_DATE = 0;

    private static final String LOCATION_GEO_POINT = "LocationGeoPoint";
    private static final int REQUEST_LOCATION = 1;

    private EditText mTitleField;
    private Button mDateButton;
    private Button mTimeButton;
    private Button mLocationButton;
    private Button mSaveButton;
    private Spinner mSpinner;

    //ArrayAdapter connects spinner widget to array-based data (as opposed to CursorAdapter)
    private ArrayAdapter<String> mAdapter;
    private ArrayList<String> mCollabs;

    protected int mPos;
    protected String mSelection;

    private static ParseUser mSelectedAssignee;
    private PTaskList taskList;
    private PTask task;
    private String taskId = null;
    private String taskListId = null;

    private Date mDate;
    private String mLocationTitle;

    private ParseGeoPoint locationGeoPoint;
    private SpecificSerializable interfaceSpecific = new SpecificSerializable();
    private LocationManager mLocationManager;
    private LocationFoundInterface mLocationFoundInterface;
    private Callbacks mCallbacks;
    private Activity mActivity;

    public interface Callbacks {
        void onTaskUpdated(PTask task, PTaskList taskList);
    }

    public static PTaskFragment newInstance(String taskId, String taskListId) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_TASK_ID, taskId);
        args.putSerializable(ARG_TASK_LIST_ID, taskListId);

        PTaskFragment fragment = new PTaskFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mCallbacks = (Callbacks) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallbacks = null;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        // Fetch the taskId
        if (ARG_TASK_ID != null) {
            taskId = (String) getArguments().getSerializable(ARG_TASK_ID);
        }

        // Fetch the taskListId
        taskListId = (String) getArguments().getSerializable(ARG_TASK_LIST_ID);

        mLocationManager = new LocationManager(getActivity());
        mLocationManager.initialize(mLocationFoundInterface);

        mActivity = getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.p_fragment_task, container, false);

        mTitleField = (EditText) v.findViewById(R.id.p_task_title);
        mDateButton = (Button) v.findViewById(R.id.p_task_date);
        mTimeButton = (Button) v.findViewById(R.id.p_task_time);
        mLocationButton = (Button) v.findViewById(R.id.p_task_location);
        mSaveButton = (Button) v.findViewById(R.id.p_task_save);
        mSpinner = (Spinner) v.findViewById(R.id.assignee_spinner);

        ParseQuery<PTaskList> queryList = PTaskList.getQuery();
        queryList.fromLocalDatastore();
        queryList.whereEqualTo("objectId", taskListId);
        try {
            taskList = queryList.getFirst();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        finishView(v, taskList);

        return v;
    }

    public void finishView(View v, final PTaskList taskList) {

        mCollabs = new ArrayList<>();
        for (ParseUser user : taskList.getCollaborators()) {
            try {
                mCollabs.add((String) user.fetchIfNeeded().get("name"));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        mAdapter = new ArrayAdapter<>(mActivity,
                android.R.layout.simple_spinner_dropdown_item);

        if (taskId == null) {
            // Setup public read and write access for new PTask
            ParseACL ACL = new ParseACL();
            ACL.setPublicReadAccess(true);
            ACL.setPublicWriteAccess(true);

            task = new PTask();
            task.setACL(ACL);
            task.setUuidString(UUID.randomUUID());
            updateDate(mDate);
            updateLocation(mLocationTitle);
            updateSpinner(mCollabs, mAdapter, mSpinner);

        } else {
            ParseQuery<PTask> query = PTask.getQuery();
            query.fromLocalDatastore();
            query.whereEqualTo("objectId", taskId);
            query.getFirstInBackground(new GetCallback<PTask>() {

                @Override
                public void done(PTask object, ParseException e) {
                    if (e == null) {
                        task = object;
                        mTitleField.setText(task.getTitle());
                        mDate = task.getDate();
                        mLocationTitle = task.getLocTitle();
                        updateSpinner(mCollabs, mAdapter, mSpinner);
                        updateDate(mDate);
                        updateLocation(mLocationTitle);
                    }
                }
            });
        }

        mTitleField.setImeOptions(EditorInfo.IME_ACTION_DONE);
        mTitleField.setSingleLine();

        mSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View v) {
                mCallbacks.onTaskUpdated(task, taskList);

                if (locationGeoPoint != null) {
                    task.setGeoPoint(locationGeoPoint);
                    task.setLocTitle(mLocationTitle);
                }

                if (mDate != null) {
                    task.setDate(mDate);
                    setNotification(v);
                }

                if (mSelectedAssignee != null) {
                    task.setAssignee(mSelectedAssignee);
//                    sendPush();
                } else {
                    task.setAssignee(ParseUser.getCurrentUser());
                }

                task.setTitle(mTitleField.getText().toString());
                task.setDraft(true);
                task.setAuthor(ParseUser.getCurrentUser());
                task.setListId(taskListId);

                task.pinInBackground(ListApplication.TASK_GROUP_NAME,
                        new SaveCallback() {

                            @Override
                            public void done(ParseException e) {
                                if (e == null) {
                                    //rewriting whatever is in GoogleAPI (does everything: creates geofence request,
                                    // pending intent, and adds geofences to Google API)
                                    if (locationGeoPoint != null) {
                                        mLocationManager.publishGeofences();
                                    }
                                    if (e == null) {
                                        getActivity().setResult(Activity.RESULT_OK);
                                        getActivity().finish();
                                    }
                                } else {
                                    Toast.makeText(getActivity().getApplicationContext(),
                                            "Error saving: " + e.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                }
                            }

                        });
            }
        });

        mDateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTitleField.getRootView().clearFocus();
                FragmentManager manager = getFragmentManager();
                if (mDate == null) {
                    mDate = new Date();
                }
                DatePickerFragment dialog = DatePickerFragment.newInstance(mDate,
                        PTaskFragment.this);
                dialog.setTargetFragment(PTaskFragment.this, REQUEST_DATE);
                dialog.show(manager, DIALOG_DATE);
            }
        });

        mTimeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTitleField.getRootView().clearFocus();
                FragmentManager manager = getFragmentManager();
                if (mDate == null) {
                    mDate = new Date();
                }
                TimePickerFragment dialog = TimePickerFragment.newInstance(mDate,
                        PTaskFragment.this);
                dialog.setTargetFragment(PTaskFragment.this, REQUEST_DATE);
                dialog.show(manager, DIALOG_DATE);
            }
        });

        mLocationButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mTitleField.getRootView().clearFocus();
                startActivityForResult(getIntent(v.getContext(), interfaceSpecific), REQUEST_LOCATION);
            }
        });
    }

    // This class solely exists because you can't serialize an entire activity
    private static class SpecificSerializable implements LocationPickedInterface, Serializable {
        public ParseGeoPoint mGeoPoint;

        @Override
        public void onLocationPicked(String title, ParseGeoPoint geoPoint) {
            mGeoPoint = geoPoint;
            //Do something with the title using an  interface
        }
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (savedInstanceState != null) {

            if (savedInstanceState.getSerializable("date") != null) {
                mDate = (Date) savedInstanceState.getSerializable("date");
            }
            if (savedInstanceState.getString("locationTitle") != null) {
                mLocationTitle = savedInstanceState.getString("locationTitle");
            }
            if (savedInstanceState.getString("geoPointLatitude") != null) {
                locationGeoPoint = new ParseGeoPoint(savedInstanceState.getDouble("geoPointLatitude"),
                        savedInstanceState.getDouble("geoPointLongitude"));
            }

            if (savedInstanceState.getSerializable("taskList") != null) {
                taskList = (PTaskList) savedInstanceState.getSerializable("taskList");
            }
        }
    }

    private void updateSpinner(ArrayList<String> collabs, ArrayAdapter<String> arrayAdapter, final Spinner spinner) {
        if (arrayAdapter != null) {
            arrayAdapter.addAll(collabs);
        }

        if (spinner != null) {
            spinner.setAdapter(arrayAdapter);

            if (mSelectedAssignee != null && task.getAssignee() != null) {
                spinner.setSelection(collabs.indexOf(task.getAssignee().get("name")));
            }
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    TextView assigneeName = (TextView) view;

                    ParseQuery<ParseUser> assigneeQuery = ParseUser.getQuery();
                    try {
                         mSelectedAssignee = assigneeQuery.whereEqualTo("name", assigneeName.getText()).getFirst();
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {

                }
            });
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        if (mDate != null) {
            savedInstanceState.putSerializable("date", mDate);
        }
        if (mLocationTitle != null) {
            savedInstanceState.putString("locationTitle", mLocationTitle);
        }
        if (locationGeoPoint != null) {
            savedInstanceState.putDouble("geoPointLatitude", locationGeoPoint.getLatitude());
            savedInstanceState.putDouble("geoPointLongitude", locationGeoPoint.getLongitude());
        }

        if (taskList != null) {
            savedInstanceState.putSerializable("taskList", taskList);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateDate(mDate);
        updateLocation(mLocationTitle);
    }

    private Intent getIntent(Context context, LocationPickedInterface locationPickedInterface) {
        Intent i = new Intent(context, MapActivity.class);
        i.putExtra("locationIsPicked", locationPickedInterface);
        return i;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_LOCATION) {
            if (resultCode == MapActivity.RESULT_OK) {
                double latitude = data.getDoubleExtra("latitude", 0);
                double longitude = data.getDoubleExtra("longitude", 0);

                mLocationTitle = data.getStringExtra("Title");
                locationGeoPoint = new ParseGeoPoint(latitude, longitude);
                updateLocation(mLocationTitle);
            }
        }
    }

    // If existing task, the delete action appears on menu
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);
        if (taskId == null) {
            menu.findItem(R.id.action_delete).setVisible(false);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.fragment_task, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete:
                task.deleteEventually();
                getActivity().setResult(Activity.RESULT_OK);
                getActivity().finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onDatePicked(Date date) {
        mDate = date;
        updateDate(mDate);
    }

    // Sets texts for a saved date or suggests the user to pick a date if not yet done
    private void updateDate(Date date) {
        if (date == null) {
            mDateButton.setText("Pick Date");
            mTimeButton.setText("Pick Time");
        } else {
            mDateButton.setText(DateFormat.getDateInstance().format(mDate));
            mTimeButton.setText(DateFormat.getTimeInstance().format(mDate));
        }
    }

    private void updateLocation(String locationTitle) {
        if (locationTitle == null) {
            mLocationButton.setText("Choose Location");
        } else {
            mLocationButton.setText(locationTitle);
        }
    }

    // Sets notification up upon user saving (method called in mSaveButton's onClickListener)
    public void setNotification(View v) {

        // Calculate time until alert
        Long start;
        if (task.getCreatedAt() != null) {
            start = task.getCreatedAt().getTime();
        } else {
            start = System.currentTimeMillis();
        }

        Long alertTime = System.currentTimeMillis() + (task.getDate().getTime() - start);
        Log.d("time", "alert: " + alertTime);

        // Define intent of using AlertReceiver
        Intent i = new Intent(v.getContext(), AlertReceiver.class);
        i.putExtra("ID", task.getUuidString());

        // Schedule for notification to appear later
        AlarmManager alarmManager = (AlarmManager) getActivity()
                .getSystemService(Context.ALARM_SERVICE);

        // Trigger for alertIntent to fire in alertTime seconds
        // FLAG_UPDATE_CURRENT: Update Intent if active
        alarmManager.set(AlarmManager.RTC_WAKEUP, alertTime,
                PendingIntent.getBroadcast(getActivity(), 0, i,
                        PendingIntent.FLAG_ONE_SHOT));
    }

    // Send push notification to assignee (method called in mSaveButton's onClickListener)
    public void sendPush() {
        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("recipientId", mSelectedAssignee);
        params.put("message", "You've been assigned to: " + task.getTitle());
        ParseCloud.callFunctionInBackground("sendPushToUser", params, new FunctionCallback<String>() {
            public void done(String success, ParseException e) {
                if (e == null) {
                    Log.d("push", "sent successfully");
                }
            }
        });
    }

    public int getSpinnerPosition() {
        return this.mPos;
    }

    public void setSpinnerPosition(int pos) {
        this.mPos = pos;
    }

    public String getSpinnerSelection() {
        return this.mSelection;
    }

    public void setSpinnerSelection(String selection) {
        this.mSelection = selection;
    }
}
