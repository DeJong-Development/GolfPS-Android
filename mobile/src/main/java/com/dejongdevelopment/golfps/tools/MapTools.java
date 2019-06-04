package com.dejongdevelopment.golfps.tools;

import android.os.AsyncTask;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.DataItem;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collection;
import java.util.concurrent.ExecutionException;

public class MapTools {

    public static LatLng getBoundsCenter(LatLngBounds bounds) {
        LatLng ne = bounds.northeast;
        LatLng sw = bounds.southwest;

        double latCenter = (ne.latitude + sw.latitude) / 2;
        double longCenter = (ne.longitude + sw.longitude) / 2;
        return new LatLng(latCenter, longCenter);
    }
    private static double latRad(Double lat) {
        double sinRad = Math.sin(lat * Math.PI / 180);
        double radX2 = Math.log((1 + sinRad) / (1 - sinRad)) / 2;
        return Math.max(Math.min(radX2, Math.PI), -Math.PI) / 2;
    }
    private static float zoom(Double mapPx, Double worldPx, Double fraction) {
        return (float)(Math.log(mapPx / worldPx / fraction) / Math.log(2));
    }
    public static float getBoundsZoomLevel(LatLngBounds bounds, View view) {
        float ZOOM_MAX = 20f;

        LatLng ne = bounds.northeast;
        LatLng sw = bounds.southwest;

        double latFraction = (latRad(ne.latitude) - latRad(sw.latitude)) / Math.PI;

        double lngDiff = ne.longitude - sw.longitude;
        double lngFraction = ((lngDiff < 0) ? (lngDiff + 360) : lngDiff) / 360;

        double viewHeight = view.getMeasuredHeight();
        double scaleY = view.getScaleY();
        double viewWidth = view.getMeasuredWidth();
        double scaleX = view.getScaleX();

        float latZoom = zoom((viewHeight / scaleY) / 4, 256D, latFraction);
        float lngZoom = zoom((viewWidth / scaleX) / 4, 256D, lngFraction);

        return Math.min(Math.min(latZoom, lngZoom), ZOOM_MAX);
    }

    public static double getElevation(LatLng latLng) {
        return getElevation(latLng.latitude, latLng.longitude);
    }
    public static double getElevation(Double latitude, Double longitude) {
        double result = Double.NaN;
        HttpURLConnection connection = null;

        String elevationUrl = "https://maps.googleapis.com/maps/api/elevation/json?locations=" + latitude + "," + longitude + "&key=AIzaSyCqpfes_W6_swSv-Uv9O4gFGjqAKaZ7r-o";
        try {
            URL altitudeUrl = new URL(elevationUrl);
            connection = (HttpURLConnection) altitudeUrl.openConnection();
            connection.setRequestMethod("GET");

            connection.setUseCaches(true);
            connection.setRequestProperty("Content-Type", "application/json");

            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            StringBuffer response = new StringBuffer();
            String line;
            while ((line = rd.readLine()) != null) {
                response.append(line);
            }

            JSONObject elevationResponse = new JSONObject(response.toString());
            JSONArray results = elevationResponse.getJSONArray("results");
            if (results.length() > 0) {
                JSONObject elevationResult = results.getJSONObject(0);
                result = Double.parseDouble(elevationResult.getString("elevation"));
            }

            rd.close();
            is.close();

        } catch (IOException e) {

        } catch (JSONException e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return result;
    }



    public static LatLng calculateDistanceCoordinates(LatLng startingCoordinates, double yardDistance, double angle) {
        double earthsRadiusInYards = 6371 * 1093.6133;
        double angularDistance = yardDistance / earthsRadiusInYards;

        double bearing = Math.toRadians(angle);

        double lat1 = Math.toRadians(startingCoordinates.latitude);
        double long1 = Math.toRadians(startingCoordinates.longitude);

        double lat2 = Math.asin( Math.sin(lat1)*Math.cos(angularDistance) +
                Math.cos(lat1)*Math.sin(angularDistance)*Math.cos(bearing) );
        double long2 = long1 + Math.atan2(Math.sin(bearing)*Math.sin(angularDistance)*Math.cos(lat1),
                Math.cos(angularDistance)-Math.sin(lat1)*Math.sin(lat2));

        lat2 = Math.toDegrees(lat2);
        long2 = (Math.toDegrees(long2)+540) % 360 - 180;

        return new LatLng(lat2, long2);
    }

    public static float calcBearing(LatLng start, LatLng finish){
        double latRad1 = Math.toRadians(start.latitude);
        double latRad2 = Math.toRadians(finish.latitude);
        double longDiff = Math.toRadians(finish.longitude-start.longitude);
        double y = Math.sin(longDiff)*Math.cos(latRad2);
        double x = Math.cos(latRad1)*Math.sin(latRad2) - Math.sin(latRad1)*Math.cos(latRad2)*Math.cos(longDiff);

        float calcBearing = (float)(Math.toDegrees(Math.atan2(y, x))+360)%360;
        return calcBearing;
    }
    private static double degreesToRadians(double degrees) {
        return degrees * Math.PI / 180;
    }

    public static int distanceFrom(LatLng first, LatLng second) {
        double earthRadiusKm = 6371;

        double lat1 = first.latitude;
        double lon1 = first.longitude;
        double lat2 = second.latitude;
        double lon2 = second.longitude;

        double dLat = degreesToRadians(lat2-lat1);
        double dLon = degreesToRadians(lon2-lon1);

        lat1 = degreesToRadians(lat1);
        lat2 = degreesToRadians(lat2);

        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.sin(dLon/2) * Math.sin(dLon/2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return (int)(earthRadiusKm * c * 1093.61); //convert km to yards
    }

}
