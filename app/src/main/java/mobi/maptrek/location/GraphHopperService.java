/*
 * Copyright 2019 Andrey Novikov
 *
 * This program is free software: you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package mobi.maptrek.location;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.os.ResultReceiver;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.oscim.core.GeoPoint;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class GraphHopperService extends IntentService {
    public static final String EXTRA_POINTS = "points";

    public GraphHopperService() {
        super("GraphHopperService");
    }

    // https://docs.graphhopper.com/#operation/getRoute
    @SuppressLint("RestrictedApi")
    @Override
    protected void onHandleIntent(Intent workIntent) {
        double[] points = workIntent.getDoubleArrayExtra(EXTRA_POINTS);
        ResultReceiver receiver = workIntent.getParcelableExtra(Intent.EXTRA_RESULT_RECEIVER);
        OkHttpClient client = new OkHttpClient();
        HttpUrl.Builder urlBuilder = new HttpUrl.Builder()
                .scheme("https")
                .host("graphhopper.com")
                .addPathSegments("api/1/route")
                .addQueryParameter("key", "71856f01-9df7-41dc-89eb-7e0de877c059")
                .addQueryParameter("locale", "de")
                .addQueryParameter("vehicle", "car");
        if (points != null) {
            for (int i = 0; i < points.length; i += 2) {
                urlBuilder.addQueryParameter("point", points[i] + "," + points[i + 1]);
            }
        }
        Request request = new Request.Builder().url(urlBuilder.build()).get().build();

        Bundle bundle = new Bundle();
        bundle.putDoubleArray(EXTRA_POINTS, points);
        try {
            Response response = client.newCall(request).execute();
            ResponseBody body = response.body();
            if (body != null) {
                String json = body.string();
                JSONObject jsonObject = new JSONObject(json);
                body.close();
                if (response.isSuccessful()) {
                    JSONArray paths = jsonObject.getJSONArray("paths");
                    JSONObject path = paths.getJSONObject(0);
                    bundle.putDouble("distance", path.getDouble("distance"));
                    bundle.putLong("time", path.getLong("time") / 1000); // convert to seconds
                    bundle.putString("points", path.getString("points"));
                    receiver.send(1, bundle);
                } else {
                    bundle.putString("message", jsonObject.getString("message"));
                    receiver.send(0, bundle);
                    JSONArray hints = jsonObject.getJSONArray("hints");
                    for (int i = 0; i < hints.length(); i++)
                        Log.e("GHR", hints.getJSONObject(i).getString("message"));
                }
            } else {
                receiver.send(0, null);
            }
        } catch (IOException | JSONException e) {
            bundle.putString("message", e.getMessage());
            receiver.send(0, bundle);
        }
    }

    /**
     *  Licensed to GraphHopper GmbH under one or more contributor
     *  license agreements. See the NOTICE file distributed with this work for
     *  additional information regarding copyright ownership.
     *
     *  GraphHopper GmbH licenses this file to you under the Apache License,
     *  Version 2.0 (the "License"); you may not use this file except in
     *  compliance with the License. You may obtain a copy of the License at
     *
     *       http://www.apache.org/licenses/LICENSE-2.0
     *
     *  Unless required by applicable law or agreed to in writing, software
     *  distributed under the License is distributed on an "AS IS" BASIS,
     *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
     *  See the License for the specific language governing permissions and
     *  limitations under the License.
     *
     * Code which handles polyline encoding and other web stuff.
     * <p>
     * The necessary information for polyline encoding is in this answer:
     * http://stackoverflow.com/a/24510799/194609 with a link to official Java sources as well as to a
     * good explanation.
     * <p>
     *
     * @author Peter Karich
     */
    public static ArrayList<GeoPoint> decodePolyline(String encoded, int initCap, boolean is3D) {
        ArrayList<GeoPoint> poly = new ArrayList<>(initCap);
        int index = 0;
        int len = encoded.length();
        int lat = 0, lng = 0, ele = 0;
        while (index < len) {
            // latitude
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int deltaLatitude = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += deltaLatitude;

            // longitude
            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int deltaLongitude = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += deltaLongitude;

            if (is3D) {
                // elevation
                shift = 0;
                result = 0;
                do {
                    b = encoded.charAt(index++) - 63;
                    result |= (b & 0x1f) << shift;
                    shift += 5;
                } while (b >= 0x20);
                int deltaElevation = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
                ele += deltaElevation;
                poly.add(new GeoPoint(lat * 10, lng * 10)); // E5 to E6 conversion
                // ele / 100;
            } else {
                poly.add(new GeoPoint(lat * 10, lng * 10)); // E5 to E6 conversion
            }
        }
        return poly;
    }
}
