// Copyright 2004-present Facebook. All Rights Reserved.

package com.fb.android.remindmap.fresh;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.fb.android.remindmap.R;

/**
 * Created by judyl on 7/14/15.
 */

public class PTaskListActivity extends SingleFragmentActivity implements
        PTaskListFragment.Callbacks, PTaskFragment.Callbacks, NavigationDrawerFragment.ListCallbacks {

    @Override
    protected Fragment createFragment() {

        // Fetch the IDs for notifications
        if (getIntent().hasExtra("ID")) {
            String taskId = getIntent().getExtras().getString("ID");
            String listId = getIntent().getExtras().getString("LIST_ID");
            return PTaskFragment.newInstance(taskId, listId);
        }
        return new NavigationDrawerFragment();
    }

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_masterdetail;
    }

    @Override
    public void onTaskSelected(PTask task, PTaskList taskList) {
        Log.d("TaskList", "Selected: " + taskList.getTitle());
        String taskId = null;
        if (task != null) {
            taskId = task.getObjectId();
        }
        if (findViewById(R.id.detail_fragment_container) == null) {
            Intent intent = PTaskPagerActivity.newIntent(this, taskId, taskList.getObjectId());
            startActivity(intent);
        } else {
            Fragment newDetail = PTaskFragment.newInstance(taskId, taskList.getObjectId());
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.detail_fragment_container, newDetail)
                    .commit();
        }
    }

    @Override
    public void onTaskUpdated(PTask task, PTaskList taskList) {
        PTaskListFragment listFragment = (PTaskListFragment)
                getSupportFragmentManager()
                        .findFragmentById(R.id.content_frame);
        listFragment.updateUI();
    }

    @Override
    public void onTaskListSelected(PTaskList taskList) {
        if (findViewById(R.id.content_frame) == null) {
            Log.d("Empty ", "frame: " + taskList.getTitle());
            Intent intent = PTaskListPagerActivity.newIntent(this, taskList.getObjectId());
            startActivity(intent);
        } else {
            Log.d("Contained ", "frame: " + taskList.getTitle());
            PTaskListFragment newDetail = PTaskListFragment.newInstance(taskList.getObjectId());
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.content_frame, newDetail)
                    .commit();
        }
    }

    @Override
    public void onTaskListUpdated(PTaskList taskList) {
        NavigationDrawerFragment listFragment = (NavigationDrawerFragment)
                getSupportFragmentManager()
                        .findFragmentById(R.id.fragment_container);
        listFragment.updateUI();
    }
}
