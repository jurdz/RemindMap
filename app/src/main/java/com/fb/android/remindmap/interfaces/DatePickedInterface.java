// Copyright 2004-present Facebook. All Rights Reserved.

package com.fb.android.remindmap.interfaces;

import java.io.Serializable;
import java.util.Date;

/**
 * Created by etorgas on 7/18/15.
 */
public interface DatePickedInterface extends Serializable {
    void onDatePicked(Date date);
}
