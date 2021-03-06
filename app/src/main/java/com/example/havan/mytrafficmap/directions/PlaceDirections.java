package com.example.havan.mytrafficmap.directions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.text.Html;
import android.util.Log;
import android.widget.Toast;

import com.example.havan.mytrafficmap.R;
import com.example.havan.mytrafficmap.Utils;
import com.example.havan.mytrafficmap.view.AlertDialogManager;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.gson.Gson;

public class PlaceDirections {


    private String url;

    String json;

    private Context context;

    private ProgressDialog pDialog;

    private GoogleMap googleMap;

    private LatLng from;

    private LatLng to;

    private Activity activity;

    public Activity getActivity() {
        return activity;
    }

    public PlaceDirections(Context context, Activity activity,
                           GoogleMap googleMap, LatLng from, LatLng to) {
        this.context = context;
        this.googleMap = googleMap;
        this.from = from;
        this.to = to;
        this.activity = activity;
        pDialog = new ProgressDialog(context);
        url = getMapsApiDirectionsUrl();
        this.googleMap.clear();
        LoadDirections directions = new LoadDirections();
        directions.execute(url);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(from,
                14));

    }

    private String getMapsApiDirectionsUrl() {

        // add more mode here
        String waypoints =
                "origin=" + this.from.latitude + "," + this.from.longitude
                        + "&" +
                        "destination=" + to.latitude + "," + to.longitude;
        String routerType;

        routerType = "mode=driving";

        String sensor = "sensor=false";
        String params = waypoints + "&" + sensor + "&" + routerType;
        String output = "json";
        String url = "https://maps.googleapis.com/maps/api/directions/"
                + output + "?" + params;
        return url;
    }

    private void addMarkers() {
        if (googleMap != null) {
            googleMap.addMarker(new MarkerOptions().position(from)
                    .title("You are here!")
                    .snippet("This is your current location")
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.steppin)));

            googleMap.addMarker(new MarkerOptions().position(to)
                    .title(Utils.sTrDestination)
                    .snippet(Utils.sTrSnippet)
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.pin_flag_red)));
        }
    }

    private class LoadDirections extends AsyncTask<String, Void, String> {


        @Override
        protected void onPreExecute() {

            super.onPreExecute();
            pDialog = new ProgressDialog(getActivity());
            pDialog.setMessage(Html.fromHtml("<b>Processing</b><br/>Loading your route way..."));
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();

        }

        @Override
        protected String doInBackground(String... url) {
            String data = "";
            try {
                HttpConnection http = new HttpConnection();
                data = http.readUrl(url[0]);
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            pDialog.dismiss();
            super.onPostExecute(result);
            addMarkers();
            new ParserTask().execute(result);
        }
    }

    private class ParserTask extends AsyncTask<String,
            Integer, List<List<HashMap<String, String>>>> {

        @Override
        protected List<List<HashMap<String, String>>> doInBackground(
                String... jsonData) {

            JSONObject jObject;
            List<
                    List<
                            HashMap<
                                    String, String
                                    >
                            >
                    > routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                PathJSONParser parser = new PathJSONParser();
                routes = parser.parse(jObject);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return routes;
        }

        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> routes) {
            ArrayList<LatLng> points = null;
            PolylineOptions polyLineOptions = null;

            if (routes == null) return;
            // traversing through route
            for (int i = 0; i < routes.size(); i++) {
                points = new ArrayList<LatLng>();
                polyLineOptions = new PolylineOptions();
                List<HashMap<String, String>> path = routes.get(i);

                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                polyLineOptions.addAll(points);
                polyLineOptions.width(6);
                polyLineOptions.clickable(true);
                polyLineOptions.color(Color.parseColor("#26A1C3"));

                ///////////////////

                json = new Gson().toJson(points);
                Utils.sRoute = json;

            }
            if (polyLineOptions == null) {

                Toast toast = Toast.makeText(context,
                        "Don't have way for this", Toast.LENGTH_SHORT);
                toast.show();
            } else googleMap.addPolyline(polyLineOptions);
        }

    }

}