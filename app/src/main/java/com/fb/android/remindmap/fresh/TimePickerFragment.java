// Copyright 2004-present Facebook. All Rights Reserved.

package com.fb.android.remindmap.fresh;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TimePicker;

import com.fb.android.remindmap.R;
import com.fb.android.remindmap.interfaces.DatePickedInterface;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * Created by etorgas on 7/7/15.
 */

public class TimePickerFragment extends DialogFragment {

    private static final String ARG_DATE = "date";
    private static final String ARG_DATE_PICKED = "date_picked";

    private TimePicker mTimePicker;

    public static TimePickerFragment newInstance(Date date, DatePickedInterface datePickedInterface) {
        Bundle args = new Bundle();
        args.putSerializable(ARG_DATE, date);
        args.putSerializable(ARG_DATE_PICKED, datePickedInterface);

        TimePickerFragment fragment = new TimePickerFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        Date date = (Date) getArguments().getSerializable(ARG_DATE);
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        final int year = c.get(Calendar.YEAR);
        final int month = c.get(Calendar.MONTH);
        final int day = c.get(Calendar.DAY_OF_MONTH);
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int min = c.get(Calendar.MINUTE);

        View v = LayoutInflater.from(getActivity())
                .inflate(R.layout.dialog_time, null);

        mTimePicker = (TimePicker) v.findViewById(R.id.dialog_time_time_picker);
        mTimePicker.setCurrentHour(hour);
        mTimePicker.setCurrentMinute(min);

        return new AlertDialog.Builder(getActivity())
                .setView(v)
                .setTitle(R.string.time_picker_title)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int hour = mTimePicker.getCurrentHour();
                        int min = mTimePicker.getCurrentMinute();
                        Date date = new GregorianCalendar(year, month, day, hour, min).getTime();
                        DatePickedInterface datePickedInterface = (DatePickedInterface) getArguments()
                                .getSerializable(ARG_DATE_PICKED);
                        datePickedInterface.onDatePicked(date);
                    }
                })
                .create();
    }
}
