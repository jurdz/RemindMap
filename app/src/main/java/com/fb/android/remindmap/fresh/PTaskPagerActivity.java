// Copyright 2004-present Facebook. All Rights Reserved.

package com.fb.android.remindmap.fresh;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;

import com.fb.android.remindmap.R;
import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseQuery;

import java.util.List;

//Copyright 2004-present Facebook.All Rights Reserved.

/**
 * Created by judyl on 6/19/15.
 */

public class PTaskPagerActivity extends AppCompatActivity implements PTaskFragment.Callbacks {

    private static final String EXTRA_TASK_ID = "task_id";
    private static final String EXTRA_TASK_LIST_ID = "task_list_id";


    private ViewPager mViewPager;
    private List<PTask> mTasks;

    public static Intent newIntent(Context packageContext, String taskId, String taskListId) {
        Intent intent = new Intent(packageContext, PTaskPagerActivity.class);
        intent.putExtra(EXTRA_TASK_ID, taskId);
        intent.putExtra(EXTRA_TASK_LIST_ID, taskListId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment_pager);

        final String taskId = (String) getIntent().getSerializableExtra(EXTRA_TASK_ID);
        final String taskListId = (String) getIntent().getSerializableExtra(EXTRA_TASK_LIST_ID);

        mViewPager = (ViewPager) findViewById(R.id.activity_task_pager_view_pager);

        ParseQuery<PTask> query = PTask.getQuery();
        query.fromLocalDatastore();
        query.whereEqualTo("listId", taskListId);
        query.orderByAscending("date");
        query.findInBackground(new FindCallback<PTask>() {
            @Override
            public void done(List<PTask> tasks, ParseException e) {
                if (e == null) {
                    mTasks = tasks;

                    // Accounts for new task creation at the end of the view
                    mTasks.add(null);
                    FragmentManager fragmentManager = getSupportFragmentManager();
                    mViewPager.setAdapter(new FragmentStatePagerAdapter(fragmentManager) {

                        @Override
                        public Fragment getItem(int position) {
                            PTask task = mTasks.get(position);
                            if (task == null) {
                                return PTaskFragment.newInstance(null, taskListId);
                            }
                            return PTaskFragment.newInstance(task.getObjectId(), taskListId);
                        }

                        @Override
                        public int getCount() {
                            return mTasks.size();
                        }
                    });

                    for (int i = 0; i < mTasks.size() - 1; i++) {
                        if (mTasks.get(i).getObjectId().equals(taskId)) {
                            mViewPager.setCurrentItem(i);
                            break;
                        }
                        mViewPager.setCurrentItem(mTasks.size() - 1);
                    }
                }
            }
        });
    }

    @Override
    public void onTaskUpdated(PTask task, PTaskList taskList) {
    }
}
