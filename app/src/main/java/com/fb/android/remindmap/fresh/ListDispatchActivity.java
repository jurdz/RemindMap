// Copyright 2004-present Facebook. All Rights Reserved.

package com.fb.android.remindmap.fresh;

import com.fb.android.remindmap.parselogin.ParseLoginDispatchActivity;

/**
 * Created by Judy Lin on 7/11/15.
 */

public class ListDispatchActivity extends ParseLoginDispatchActivity {

    @Override
    protected Class<?> getTargetClass() {
        return PTaskListActivity.class;
    }
}
