package com.example.tonymcdonagh.Caffeined;

import android.os.AsyncTask;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
 * Created by tonymcdonagh on 12/02/2018.
 *  Handles the map and data request
 * The data is requested
 * Once it is returned in the list of hashmaps they are then added to the map
 */

public class GetMyPlacesData extends AsyncTask<Object, String, String> {

    String costomPlacesData;
    GoogleMap mMap;
    String url;
    String json = null;

    @Override
    protected String doInBackground(Object... objects) {
        mMap = (GoogleMap)objects[0];
        url = (String)objects[1];


        DownloadUrl downloadUrl = new DownloadUrl();
        try {
            costomPlacesData = downloadUrl.readUrl(url);
        } catch (IOException e) {
            e.printStackTrace();
        }


        return costomPlacesData;
    }




    @Override
    protected void onPostExecute(String s) {
        List<HashMap<String,String>> nearbyPlaceList = null;
        MyDataParser parser = new MyDataParser();
        nearbyPlaceList = parser.parse(s);
        showNearbyPlaces(nearbyPlaceList);
    }

    private void showNearbyPlaces(List<HashMap<String,String>> nearbyPlacesList)
    {
        for(int i =0; i<nearbyPlacesList.size(); i++)
        {

            MarkerOptions markerOptions = new MarkerOptions();
            HashMap<String, String> googlePlace = nearbyPlacesList.get(i);

            String placeName = googlePlace.get("name");
            String vicinity = googlePlace.get("vicinity");
            String rating = googlePlace.get("rating");
            double lat = Double.parseDouble(googlePlace.get("lat"));
            double lng = Double.parseDouble(googlePlace.get("lng"));
            String takeAway;
            if (googlePlace.get("takeAway").equals("no")){takeAway = "No";} else {takeAway = "Yes";}
            String sitIn;
            if (googlePlace.get("sitIn").equals("no")){sitIn = "No";} else {sitIn = "Yes";}

            String snippet = "Rating: "+ rating + "\n" +
                    "Take Away: "+ takeAway + "\n" +
                    "Sit in: "+ sitIn + "\n"
                    + vicinity ;

            LatLng latLng = new LatLng(lat, lng);
            markerOptions.position(latLng);
            markerOptions.title(placeName);
            markerOptions.snippet(snippet);
            markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.coffeew));

            mMap.addMarker(markerOptions);


        }
    }
}
