// Copyright 2004-present Facebook. All Rights Reserved.

package com.fb.android.remindmap.fresh;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Toast;

import com.fb.android.remindmap.R;

import java.util.ArrayList;

import static android.provider.ContactsContract.CommonDataKinds.Phone;

/**
 * Created by etorgas on 7/29/15.
 */
public class ContactsPickerActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {

    ListView mListView;
    ArrayList<Contact> mContactArrayList;
    CheckboxListViewAdapter mCheckboxListViewAdapter;
    FloatingActionButton mAddCollaboratorButton;
    ArrayList<Contact> mSelectedContacts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_contacts_picker);

        mListView = (ListView) findViewById(R.id.contact_list_view);
        displayContactsList();

        mSelectedContacts = new ArrayList<>();

        mAddCollaboratorButton = (FloatingActionButton) findViewById(R.id.collaborators_button);
        mAddCollaboratorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (Contact contact : mContactArrayList) {
                    if (contact.isSelected()) {
                        mSelectedContacts.add(contact);
                    }
                }

                Intent intent = new Intent();
                intent.putExtra("selectedContacts", mSelectedContacts);

                setResult(RESULT_OK, intent);

                finish();
            }
        });

    }

    private void displayContactsList() {

        mContactArrayList = new ArrayList<>();

        ContentResolver contentResolver = this.getContentResolver();
        Cursor cursor = contentResolver.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
        if (cursor.moveToFirst()) {
            do {
                String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));

                if (Integer.parseInt(cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
                    Cursor phoneCursor = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{id}, null);
                    while (phoneCursor.moveToNext()) {
                        String contactName = phoneCursor.getString(phoneCursor.getColumnIndex(Phone.DISPLAY_NAME));
                        String contactNumber = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        mContactArrayList.add(new Contact(contactName, contactNumber));
                        break;
                    }
                    phoneCursor.close();
                }

            } while (cursor.moveToNext());
        }

        mCheckboxListViewAdapter = new CheckboxListViewAdapter(mContactArrayList, this);

        mListView.setAdapter(mCheckboxListViewAdapter);
    }

    private String getPhoneNumber(long id) {
        String phone = null;
        Cursor phonesCursor = null;
        phonesCursor = queryPhoneNumbers(id);
        if (phonesCursor == null || phonesCursor.getCount() == 0) {
            // No valid number
            Toast.makeText(this, "No contacts with valid phone number found", Toast.LENGTH_LONG)
                    .show();
            return null;
        } else if (phonesCursor.getCount() == 1) {
            // only one number, call it.
            phone = phonesCursor.getString(phonesCursor
                    .getColumnIndex(Phone.NUMBER));
        } else {
            phonesCursor.moveToPosition(-1);
            while (phonesCursor.moveToNext()) {

                // Found super primary, call it.
                phone = phonesCursor.getString(phonesCursor
                        .getColumnIndex(Phone.NUMBER));
                break;

            }
        }

        return phone;
    }


    private Cursor queryPhoneNumbers(long contactId) {
        ContentResolver cr = getContentResolver();
        Uri baseUri = ContentUris.withAppendedId(
                ContactsContract.Contacts.CONTENT_URI,
                contactId);
        Uri dataUri = Uri.withAppendedPath(
                baseUri,
                ContactsContract.Contacts.Data.CONTENT_DIRECTORY);

        Cursor c = cr.query(
                dataUri,
                new String[]{Phone._ID, Phone.NUMBER,
                        Phone.IS_SUPER_PRIMARY, ContactsContract.RawContacts.ACCOUNT_TYPE, Phone.TYPE,
                        Phone.LABEL},
                ContactsContract.Data.MIMETYPE + "=?",
                new String[]{Phone.CONTENT_ITEM_TYPE},
                null);
        if (c != null && c.moveToFirst()) {
            return c;
        }
        return null;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

        int position = mListView.getPositionForView(buttonView);
        if (position != ListView.INVALID_POSITION) {
            Contact contact = mContactArrayList.get(position);
            contact.setSelected(isChecked);
        }
    }
}
