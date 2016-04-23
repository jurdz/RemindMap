// Copyright 2004-present Facebook. All Rights Reserved.

package com.fb.android.remindmap.fresh;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.fb.android.remindmap.R;

import java.util.List;

/**
 * Created by etorgas on 7/29/15.
 */
public class CheckboxListViewAdapter extends ArrayAdapter<Contact> implements CompoundButton.OnCheckedChangeListener {

    private final List<Contact> mContactList;
    private final Context mContext;

    public CheckboxListViewAdapter(List<Contact> contactList, Context context) {
        super(context, R.layout.activity_contact_list_item, contactList);
        this.mContactList = contactList;
        this.mContext = context;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

    }

    private static class ContactHolder {
        public TextView contactName;
        public CheckBox checkBox;
    }

    public View getView(int position, View convertview, ViewGroup parent) {
        View v = convertview;

        ContactHolder holder = new ContactHolder();

        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        v = inflater.inflate(R.layout.activity_contact_list_item, null);

        holder.contactName = (TextView) v.findViewById(R.id.name_text_view);
        holder.checkBox = (CheckBox) v.findViewById(R.id.picked_contact_box);

        holder.checkBox.setOnCheckedChangeListener((ContactsPickerActivity) mContext);

        Contact contact = mContactList.get(position);
        holder.contactName.setText(contact.getName());
        holder.checkBox.setChecked(contact.isSelected());

        return v;
    }
}

