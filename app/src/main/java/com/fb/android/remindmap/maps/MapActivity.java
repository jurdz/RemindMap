// Copyright 2004-present Facebook. All Rights Reserved.

package com.fb.android.remindmap.maps;

import android.app.SearchManager;
import android.content.Intent;
import android.database.Cursor;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.fb.android.remindmap.geofences.LocationManager;
import com.fb.android.remindmap.R;
import com.fb.android.remindmap.interfaces.LocationFoundInterface;
import com.fb.android.remindmap.interfaces.LocationPickedInterface;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.parse.ParseGeoPoint;

public class MapActivity extends AppCompatActivity implements LoaderCallbacks<Cursor>, LocationFoundInterface {

    GoogleMap mGoogleMap;
    private LocationManager mLocationManager;
    private LocationPickedInterface mLocationListener;
    private Button mLocationSavedButton;

    private ParseGeoPoint mMarkerGeoPoint;
    private String mMarkerTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_map);

        mLocationSavedButton = (Button) findViewById(R.id.save_location_button);
        mLocationSavedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mMarkerGeoPoint != null) {
                    ParseGeoPoint point = mMarkerGeoPoint;
                    String title = mMarkerTitle;

                    Intent intent = new Intent();
                    intent.putExtra("latitude", point.getLatitude());
                    intent.putExtra("longitude", point.getLongitude());
                    intent.putExtra("Title", title);

                    setResult(RESULT_OK, intent);

                    mLocationListener.onLocationPicked(mMarkerTitle, mMarkerGeoPoint);

                    finish();
                } else {
                    Toast.makeText(
                            getApplicationContext(),
                            "Please tap a marker to select a location before saving it",
                            Toast.LENGTH_LONG)
                            .show();
                }
            }
        });

        mLocationListener = (LocationPickedInterface) getIntent()
                .getSerializableExtra("locationIsPicked");

        mGoogleMap = ((MapFragment) getFragmentManager().findFragmentById(R.id.map)).getMap();

        mGoogleMap.setMyLocationEnabled(true);

        mLocationManager = new LocationManager(this);
        mLocationManager.initialize(this);

        handleIntent(getIntent());

        onSearchRequested();
    }

    private void handleIntent(Intent intent) {

        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            doSearch(intent.getStringExtra(SearchManager.QUERY));
        } else if (Intent.ACTION_VIEW.equals(intent.getAction())) {
            getPlace(intent.getStringExtra(SearchManager.EXTRA_DATA_KEY));
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu
        getMenuInflater().inflate(R.menu.activity_map, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            case R.id.menu_search:
                onSearchRequested();
                break;
        }
        return true;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    private void doSearch(String query) {
        Bundle data = new Bundle();
        data.putString("query", query);
        getSupportLoaderManager().restartLoader(0, data, this);
    }

    private void getPlace(String query) {
        Bundle data = new Bundle();
        data.putString("query", query);
        getSupportLoaderManager().restartLoader(1, data, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int arg0, Bundle query) {
        CursorLoader cLoader = null;
        if (arg0 == 0)
            cLoader = new CursorLoader(getBaseContext(), PlaceProvider.SEARCH_URI, null, null, new String[]{query.getString("query")}, null);
        else if (arg0 == 1)
            cLoader = new CursorLoader(getBaseContext(), PlaceProvider.DETAILS_URI, null, null, new String[]{query.getString("query")}, null);

        return cLoader;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> arg0, Cursor c) {
        showLocations(c);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
        // TODO Auto-generated method stub
    }

    private void showLocations(Cursor c) {

        MarkerOptions markerOptions;
        ParseGeoPoint position = null;
        LatLng latLngPosition = null;
        mGoogleMap.clear();
        boolean isClosestMarker = true;
        while (c.moveToNext()) {
            markerOptions = new MarkerOptions();
            position = new ParseGeoPoint(Double.parseDouble(c.getString(1)), Double.parseDouble(c.getString(2)));

            latLngPosition = new LatLng(position.getLatitude(), position.getLongitude());

            markerOptions.position(latLngPosition);
            final String title = c.getString(0);
            markerOptions.title(title);
            mGoogleMap.addMarker(markerOptions);

            displayMapToast();

            while (isClosestMarker == true) {
                if (position != null) {
                    CameraUpdate cameraPosition = CameraUpdateFactory.newLatLng(latLngPosition);
                    mGoogleMap.animateCamera(cameraPosition);
                }
                isClosestMarker = false;
            }

        }

        mGoogleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                CameraUpdate center =
                        CameraUpdateFactory.newLatLng(marker.getPosition());
                CameraUpdate zoom = CameraUpdateFactory.zoomTo(18);

                mGoogleMap.moveCamera(center);
                mGoogleMap.animateCamera(zoom);
                marker.showInfoWindow();

                mMarkerTitle = marker.getTitle();

                mMarkerGeoPoint = new ParseGeoPoint(marker.getPosition().latitude,
                        marker.getPosition().longitude);

                return true;
            }
        });
    }

    private void displayMapToast() {
        String msg = "Tap on marker to select location.";
        //Toast.makeText(MapActivity.this, msg, Toast.LENGTH_LONG).show();
        Toast toast = Toast.makeText(MapActivity.this, msg, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.TOP | Gravity.CENTER_VERTICAL, 0, 300);
        toast.show();
    }


    @Override
    public void onLocationFound() {
        Location myLocation = mLocationManager.getLocation();

        LatLng myLatLng = new LatLng(myLocation.getLatitude(),
                myLocation.getLongitude());

        CameraUpdate center =
                CameraUpdateFactory.newLatLng(myLatLng);
        CameraUpdate zoom = CameraUpdateFactory.zoomTo(14);

        mGoogleMap.moveCamera(center);
        mGoogleMap.animateCamera(zoom);
    }
}
