package com.example.tonymcdonagh.Caffeined;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Created by tonymcdonagh on 12/02/2018.
 * Handles the response string
 * converts this into a jsonarray which is then parsed into a hashmap
 * these are then added to list of hashmaps to be sent back
 */

public class MyDataParser {

    private HashMap<String, String> getPlace(JSONObject googlePlaceJson) {
        HashMap<String, String> customPlacesMap = new HashMap<>();
        String placeName = "-NA-";
        String vicinity = "-NA-";
        String latitude = "";
        String longitude = "";
        String rating = "";
        String takeAway = "";
        String sitIn = "";
        System.out.println(googlePlaceJson);

        try {
            if (!googlePlaceJson.isNull("name"))
            {
                    placeName = googlePlaceJson.getString("name");
            }
            if (!googlePlaceJson.isNull("vicinity"))
            {
                vicinity = googlePlaceJson.getString("vicinity");
            }
            latitude = googlePlaceJson.getString("lat");
            longitude = googlePlaceJson.getString("lng");
            rating = googlePlaceJson.getString("rating");
            takeAway = googlePlaceJson.getString("takeAway");
            sitIn = googlePlaceJson.getString("sitIn");

            customPlacesMap.put("name", placeName);
            customPlacesMap.put("vicinity", vicinity);
            customPlacesMap.put("lat", latitude);
            customPlacesMap.put("lng", longitude);
            customPlacesMap.put("rating", rating);
            customPlacesMap.put("takeAway", takeAway);
            customPlacesMap.put("sitIn", sitIn);

        } catch (JSONException e) {
            e.printStackTrace();
        }

        return customPlacesMap;
    }


    private List<HashMap<String, String>> getPlaces(JSONArray jsonArray)
    {
        int count = jsonArray.length();
        List<HashMap<String, String>> placesList = new ArrayList<>();
        HashMap<String, String> placeMap = null;

        for(int i = 0; i<count; i++)
        {
            try {
                placeMap = getPlace((JSONObject) jsonArray.get(i));
                placesList.add(placeMap);

            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
        return placesList;
    }

    public List<HashMap<String,String>> parse(String jsonData)
    {
        JSONArray jsonArray = null;
        JSONObject jsonObject;

        try {
            jsonObject = new JSONObject(jsonData);
            jsonArray = jsonObject.getJSONArray("results");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        return  getPlaces(jsonArray);
    }
}
