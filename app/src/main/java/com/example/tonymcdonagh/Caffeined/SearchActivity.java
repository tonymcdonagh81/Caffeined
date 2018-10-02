package com.example.tonymcdonagh.Caffeined;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.util.List;


/**
 * Created by tonymcdonagh on 26/01/2018.
 * Search Activity opens with map at the devices current location and allows user to search for other locations
 */

public class SearchActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = SearchActivity.class.getSimpleName();
    public GoogleMap mMap;
    private CameraPosition mCameraPosition;


    // The entry point to the Fused Location Provider.
    private FusedLocationProviderClient mFusedLocationProviderClient;

    // A default location (Manchester, UK) and default zoom to use when location permission is
    // not granted.
    private final LatLng mDefaultLocation = new LatLng(53.4807590, -2.2426310);
    private static final int DEFAULT_ZOOM = 15;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    private boolean mLocationPermissionGranted;

    // The geographical location where the device is currently located. That is, the last-known
    // location retrieved by the Fused Location Provider.
    private Location mLastKnownLocation;
    int PROXIMITY_RADIUS = 500;

    // Keys for storing activity state.
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";

    double latitude;
    double longitude;

    EditText searchInput;
    boolean fistTouch = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve location and camera position from saved instance state.
        if (savedInstanceState != null) {
            mLastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            mCameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }

        // Retrieve the content view that renders the map and buttons.
        setContentView(R.layout.activity_search);

        // Construct a FusedLocationProviderClient.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Build the map.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        searchInput = findViewById(R.id.searchText);
        searchInput.setCursorVisible(false);

    }

    /**
     * Saves the state of the map when the activity is paused.
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (mMap != null) {
            outState.putParcelable(KEY_CAMERA_POSITION, mMap.getCameraPosition());
            outState.putParcelable(KEY_LOCATION, mLastKnownLocation);
            super.onSaveInstanceState(outState);
        }
    }


    /**
     * checks if there is a current connection to allow data to load
     */
    public boolean isOnline() {
        ConnectivityManager connMgr = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        return (networkInfo != null && networkInfo.isConnected());
    }




    /**
     * Manipulates the map when it's available.
     * This callback is triggered when the map is ready to be used.
     */
    @Override
    public void onMapReady(GoogleMap map) {
        mMap = map;

        //connection check
        if (isOnline() == false){
            String out = "Offline, please check your connection";
            Toast.makeText(this, out, Toast.LENGTH_LONG).show();
        }


        // Check for user  permission.
        getLocationPermission();

        // Turn on the My Location layer and the related control on the map.
        updateLocationUI();

        // Get the current location of the device and set the position of the map.
        getDeviceLocation();


        //closes info window if user touches it.
        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            public void onInfoWindowClick(Marker marker) {
                marker.hideInfoWindow();
            }

        });

        //listens for user clicking the map, drops pin centres map and researches
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

            @Override
            public void onMapClick(LatLng point) {

                mMap.clear();
                MarkerOptions marker = new MarkerOptions().position(
                        new LatLng(point.latitude, point.longitude)).title("Drop Pin");
                getPlaces(point.latitude, point.longitude);
                mMap.moveCamera(CameraUpdateFactory.newLatLng(point));
                mMap.animateCamera(CameraUpdateFactory.zoomTo(DEFAULT_ZOOM));
                mMap.addMarker(marker);

            }
        });


    }

    /**
     * Gets the current location of the device, and positions the map's camera.
     */
    private void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (mLocationPermissionGranted) {
                Task<Location> locationResult = mFusedLocationProviderClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            mLastKnownLocation = task.getResult();
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                    new LatLng(mLastKnownLocation.getLatitude(),
                                            mLastKnownLocation.getLongitude()), DEFAULT_ZOOM));
                        } else {
                            Log.d(TAG, "Current location is null. Using defaults.");
                            Log.e(TAG, "Exception: %s", task.getException());
                            mMap.moveCamera(CameraUpdateFactory
                                    .newLatLngZoom(mDefaultLocation, DEFAULT_ZOOM));
                            mMap.getUiSettings().setMyLocationButtonEnabled(false);
                        }
                    }
                });
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }


    /**
     * Check or prompt the user for permission to use the device location.
     */
    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }

    /**
     * Handles the result of the request for location permissions.
     */
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        mLocationPermissionGranted = false;
        switch (requestCode) {
            case PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    mLocationPermissionGranted = true;
                }
            }
        }
        updateLocationUI();
    }



    /**
     * Updates the map's UI settings based on whether the user has granted location permission.
     */
    private void updateLocationUI() {
        if (mMap == null) {
            return;
        }
        try {
            if (mLocationPermissionGranted) {
                mMap.setMyLocationEnabled(true);
                mMap.getUiSettings().setMyLocationButtonEnabled(true);
            } else {
                mMap.setMyLocationEnabled(false);
                mMap.getUiSettings().setMyLocationButtonEnabled(false);
                mLastKnownLocation = null;
                getLocationPermission();
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }


    /**
     * Updates the search field when touched to clear hit and shows cursor.
     * Will also trigger the search when the enter key is pushed.
     */
    public void onSelect(View v){

        if(fistTouch) {
            searchInput.setHint("");
            searchInput.setCursorVisible(true);
            fistTouch = false;
        }
        else if (searchInput.getText().toString() != ""){
            search();
        }
        else {}
    }




    /**
     * Searches for the location the user has entered when they push the go button.
     */
    public void geoLocate(View v) {

        search();
    }


    /**
     * Gets the input from the search field and using Geocoder finds the location requested.
     * Passes the loaction info to update the map and trigger the serach of places.
     */
    public void search(){
        InputMethodManager inputMethodManager = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);


        String locationSearch = searchInput.getText().toString();

        if (isOnline() == false) {
            String out = "Offline, please check your connection";
            Toast.makeText(this, out, Toast.LENGTH_LONG).show();
        } else {
            if (locationSearch != null || !locationSearch.equals("")) {
                Geocoder geocoder = new Geocoder(this);
                List<Address> list = null;
                try {
                    list = geocoder.getFromLocationName(locationSearch, 1);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (list != null && list.size() > 0) {
                    Address address = list.get(0);
                    String locality = address.getLocality();
                    mMap.clear();
                    latitude = address.getLatitude();
                    longitude = address.getLongitude();

                    goTo(latitude, longitude, locality, DEFAULT_ZOOM);
                    getPlaces(latitude, longitude);
                    searchInput.setText("");
                    searchInput.setHint("Location");
                    fistTouch = true;
                    searchInput.setCursorVisible(false);
                }
                else {                    searchInput.setText("");
                    searchInput.setHint("Location");
                    fistTouch = true;}
            }
        }
    }

    /**
     * Updates the map with the searched location.
     */
    private void goTo(double lat, double lng, String locality, float zoom){
        LatLng goLo = new LatLng(lat, lng);
        MarkerOptions marker = new MarkerOptions().position(
                new LatLng(lat, lng)).title(locality);

        CameraUpdate update = CameraUpdateFactory.newLatLngZoom(goLo, zoom);
        mMap.moveCamera(update);
        mMap.addMarker(marker);
    }


    /**
     * gets locations from search or drop pin to search for places.
     */
    public void getPlaces(double latitude, double longitude) {

        findType(latitude, longitude);
        findNames(latitude, longitude);
    }

    /**
     * Request to find places of type cafe.
     */
    public void findType(double latitude, double longitude) {
        String search = "type";
        String cafe = "cafe";
        String url = getUrl(latitude, longitude, search, cafe);
        Object dataTransfer[] = new Object[2];
        dataTransfer[0] = mMap;
        dataTransfer[1] = url;
        GetPlacesData getPlacesData = new GetPlacesData();
        getPlacesData.execute(dataTransfer);
    }

    /**
     * Request to find places with a name containing key words.
     */
    public void findNames(double latitude, double longitude) {

        String search = "name";
        String names = "coffee|starbucks|nero";
        String url = getUrl(latitude, longitude, search, names);
        Object dataTransfer[] = new Object[2];
        dataTransfer[0] = mMap;
        dataTransfer[1] = url;
        GetPlacesData getPlacesData = new GetPlacesData();
        getPlacesData.execute(dataTransfer);
    }

    /**
     * Builds url to find places.
     */
    private String getUrl(double latitude, double longitude, String search, String find)
    {
        StringBuilder googlePlaceUrl = new StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?");
        googlePlaceUrl.append("location="+latitude+","+longitude);
        googlePlaceUrl.append("&radius="+PROXIMITY_RADIUS);
        googlePlaceUrl.append("&"+search+"="+find);
        googlePlaceUrl.append("&sensor=ture");
        googlePlaceUrl.append("&key="+"AIzaSyDuwsWtj2Bs5R6M1QceJ-Z3YMILGkvPGsg");

        return  googlePlaceUrl.toString();
    }



}
