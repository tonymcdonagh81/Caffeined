package com.example.tonymcdonagh.Caffeined;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.GeoDataClient;
import com.google.android.gms.location.places.PlaceDetectionClient;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;


/**
 * Created by tonymcdonagh on 26/01/2018.
 * Filter activity that displays a map with the devices current location and loads coffee shops from the datastore.
 * Allows user to filter data from datastore as required
 */

public class FilterActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = MainActivity.class.getSimpleName();
    private GoogleMap mMap;
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

    int PROXIMITY_RADIUS = 1500;
    double latitude;
    double longitude;

    // Keys for storing activity state.
    private static final String KEY_CAMERA_POSITION = "camera_position";
    private static final String KEY_LOCATION = "location";

    private SeekBar radius;
    String getData = "";
    int chain;
    int indie;
    int takeAway;
    int SitIn;
    int rating = 0;

    RadioGroup chainOrIndie, ratingGroup;
    RadioButton chainButton, indieButton;
    RadioButton allButton, oneButton, twoButton, threeButton, fourButton, fiveButton;
    TextView radDistance;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Retrieve location and camera position from saved instance state.
        if (savedInstanceState != null) {
            mLastKnownLocation = savedInstanceState.getParcelable(KEY_LOCATION);
            mCameraPosition = savedInstanceState.getParcelable(KEY_CAMERA_POSITION);
        }

        // Retrieve the content view that renders the map and buttons.
        setContentView(R.layout.activity_filter);

        // Construct a FusedLocationProviderClient.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // Build the map.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);



        radius = (SeekBar)findViewById(R.id.radius);
        radius.setMax(1500);
        radius.setProgress(1500);
        radius.incrementProgressBy(50);
        radDistance = (TextView)findViewById(R.id.radDistance);

        // perform seek bar change listener event used for getting the progress value
        radius.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progressChangedValue = 1500;

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                progress = progress / 50;
                progress = progress * 50;
                progressChangedValue = progress;
                PROXIMITY_RADIUS = progress;
                radDistance.setText(String.valueOf(PROXIMITY_RADIUS)+"M");

            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                // Detects when the bar starts to move.
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                getNearby();
            }
        });

        chainOrIndie = (RadioGroup)findViewById(R.id.radioGroupChain);
        chainButton = (RadioButton)findViewById(R.id.chainButton);
        indieButton = (RadioButton)findViewById(R.id.indieButton);
        chainOrIndie.check(R.id.bothButton);

        ratingGroup = (RadioGroup)findViewById(R.id.ratingGroup);
        oneButton = (RadioButton)findViewById(R.id.oneButton);
        twoButton = (RadioButton)findViewById(R.id.twoButton);
        threeButton = (RadioButton)findViewById(R.id.threeButton);
        fourButton = (RadioButton)findViewById(R.id.fourButton);
        fiveButton = (RadioButton)findViewById(R.id.fiveButton);
        ratingGroup.check(R.id.allButton);

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

        //load data from datastore
        onLoad();

        //closes info window if user touches it.
        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            public void onInfoWindowClick(Marker marker) {
                marker.hideInfoWindow();
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
     * Updates the map based on the rating requested.
     */
    public void rating_onClick(View v) {

        int checkedRating = ratingGroup.getCheckedRadioButtonId();
        if(checkedRating == R.id.oneButton) {
            rating = 1;
        } else if(checkedRating == R.id.twoButton) {
            rating = 2;
        } else if(checkedRating == R.id.threeButton) {
            rating = 3;
        } else if(checkedRating == R.id.fourButton) {
            rating = 4;
        } else if(checkedRating == R.id.fiveButton) {
            rating = 5;
        } else {
            rating = 0;
        }
        getNearby();
    }


    /**
     * Updates the map based on whether chain or independent is requested.
     */
    public void radio_onClick(View v) {

        int checkedId = chainOrIndie.getCheckedRadioButtonId();
        if(checkedId == R.id.chainButton) {
            chain = 1;
            indie = 0;
        } else if(checkedId == R.id.indieButton) {
            indie = 1;
            chain = 0;
        } else {
            indie = 0;
            chain = 0;
        }
        getNearby();
    }


    /**
     * Updates the map based on whether take away or sit in is requested.
     */
    public void onCheckboxClicked(View v) {
        // Is the view now checked?
        boolean checked = ((CheckBox) v).isChecked();

        // Check which checkbox was clicked
        switch(v.getId()) {
            case R.id.checkTake:
                if (checked){
                    takeAway = 1;
                }
                else {
                    takeAway = 0;}
                break;
            case R.id.checkSitIn:
                if (checked){
                    SitIn = 1;
                }
                else {
                    SitIn = 0;}

                break;
        }
        getNearby();
    }


    /**
     * Updates the map based on the current filter requests by calling for the relevant data from the datastore.
     */
    private void getNearby(){

        if (isOnline() == false){
            String out = "Offline, please check your connection";
            Toast.makeText(this, out, Toast.LENGTH_LONG).show();
        }
        else {

            mMap.clear();
            getData = "";
            if (chain == 1) {
                getData = getData + "&chainOrIndie=chain";
            }
            if (indie == 1) {
                getData = getData + "&chainOrIndie=indie";
            }
            if (takeAway == 1) {
                getData = getData + "&takeAway=takeAway";
            }
            if (SitIn == 1) {
                getData = getData + "&sitIn=sitIn";
            }
            if (rating > 0) {
                getData = getData + "&rating=" + rating;
            }

            latitude = mLastKnownLocation.getLatitude();
            longitude = mLastKnownLocation.getLongitude();


            mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(FilterActivity.this));
            String baseUrl = "http://1-dot-caf2-1516636108783.appspot.com/getNearby?";
            String locationUrl = "lat=" + latitude + "&lng=" + longitude + "&radius=" + PROXIMITY_RADIUS;
            String url = baseUrl + locationUrl + getData;
            Object dataTransfer[] = new Object[2];
            dataTransfer[0] = mMap;
            dataTransfer[1] = url;
            GetMyPlacesData getMyPlacesData = new GetMyPlacesData();
            getMyPlacesData.execute(dataTransfer);

        }
    }



    /**
     * Calls for the data from the datastore to be added to the map
     */
    private void onLoad(){


        if (isOnline() == false){
            String out = "Offline, please check your connection";
            Toast.makeText(this, out, Toast.LENGTH_LONG).show();
        }
        else {

            mMap.setInfoWindowAdapter(new CustomInfoWindowAdapter(FilterActivity.this));
            String url = "http://1-dot-caf2-1516636108783.appspot.com/getAllShops";
            Object dataTransfer[] = new Object[2];
            dataTransfer[0] = mMap;
            dataTransfer[1] = url;
            GetMyPlacesData getMyPlacesData = new GetMyPlacesData();
            getMyPlacesData.execute(dataTransfer);
        }

    }





}
