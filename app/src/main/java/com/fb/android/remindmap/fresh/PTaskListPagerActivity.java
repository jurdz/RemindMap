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

/**
 * Created by judyl on 7/23/15.
 */
public class PTaskListPagerActivity extends AppCompatActivity implements PTaskListFragment.Callbacks {

    private static final String EXTRA_TASK_LIST_ID = "com.fb.android.remindmap.task_list_id";

    private ViewPager mViewPager;
    private List<PTaskList> mTaskLists;

    public static Intent newIntent(Context packageContext, String taskListId) {
        Intent intent = new Intent(packageContext, PTaskListPagerActivity.class);
        intent.putExtra(EXTRA_TASK_LIST_ID, taskListId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment_pager);

        final String taskListId = (String) getIntent().getSerializableExtra(EXTRA_TASK_LIST_ID);

        mViewPager = (ViewPager) findViewById(R.id.activity_task_pager_view_pager);

        ParseQuery<PTaskList> query = PTaskList.getQuery();
        query.fromLocalDatastore();
        query.orderByAscending("date");
        query.findInBackground(new FindCallback<PTaskList>() {
            @Override
            public void done(List<PTaskList> tasks, ParseException e) {
                if (e == null) {
                    mTaskLists = tasks;

                    FragmentManager fragmentManager = getSupportFragmentManager();
                    mViewPager.setAdapter(new FragmentStatePagerAdapter(fragmentManager) {

                        @Override
                        public Fragment getItem(int position) {
                            PTaskList taskList = mTaskLists.get(position);
                            return PTaskListFragment.newInstance(taskList.getObjectId());
                        }

                        @Override
                        public int getCount() {
                            return mTaskLists.size();
                        }
                    });

                    for (int i = 0; i < mTaskLists.size(); i++) {
                        if (mTaskLists.get(i).getObjectId().equals(taskListId)) {
                            mViewPager.setCurrentItem(i);
                            break;
                        }
                    }
                }
            }
        });
    }

    @Override
    public void onTaskListUpdated(PTaskList taskList) {
    }

    @Override
    public void onTaskSelected(PTask task, PTaskList taskList) {
    }
}
