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

package mobi.maptrek.fragments;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import org.oscim.android.canvas.AndroidBitmap;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.backend.canvas.Color;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;

import java.util.ArrayList;
import java.util.Stack;

import mobi.maptrek.MapHolder;
import mobi.maptrek.MapTrek;
import mobi.maptrek.R;
import mobi.maptrek.data.Route;
import mobi.maptrek.layers.RouteLayer;
import mobi.maptrek.layers.marker.ItemizedLayer;
import mobi.maptrek.layers.marker.MarkerItem;
import mobi.maptrek.layers.marker.MarkerSymbol;
import mobi.maptrek.util.MarkerFactory;
import mobi.maptrek.util.StringFormatter;

public class Ruler extends Fragment implements ItemizedLayer.OnItemGestureListener<MarkerItem> {
    private MapHolder mMapHolder;

    private View mMeasurementsView;
    private TextView mDistanceView;
    private TextView mSizeView;

    private MapPosition mMapPosition;
    private RouteLayer mRouteLayer;
    private ItemizedLayer<MarkerItem> mPointLayer;
    private Stack<GeoPoint> mPointHistory;
    private Route mRoute;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        mRoute = new Route();
        mMapPosition = new MapPosition();
        mPointHistory = new Stack<>();
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_ruler, container, false);

        mMeasurementsView = rootView.findViewById(R.id.measurements);
        mDistanceView = rootView.findViewById(R.id.distance);
        mSizeView = rootView.findViewById(R.id.size);

        ImageButton addButton = rootView.findViewById(R.id.addButton);
        addButton.setOnClickListener(v -> {
            if (mMapHolder.getMap().getMapPosition(mMapPosition)) {
                GeoPoint point = mMapPosition.getGeoPoint();
                mRoute.addInstruction(point);
                MarkerItem marker = new MarkerItem(point, null, null, point);
                mPointLayer.addItem(marker);
                mPointHistory.push(point);
                mMapHolder.getMap().updateMap(true);
                updateTrackMeasurements();
            }
        });

        ImageButton insertButton = rootView.findViewById(R.id.insertButton);
        insertButton.setOnClickListener(v -> {
            if (mMapHolder.getMap().getMapPosition(mMapPosition)) {
                GeoPoint point = mMapPosition.getGeoPoint();
                mRoute.insertInstruction(point);
                MarkerItem marker = new MarkerItem(point, null, null, point);
                mPointLayer.addItem(marker);
                mPointHistory.push(point);
                mMapHolder.getMap().updateMap(true);
                updateTrackMeasurements();
            }
        });

        ImageButton removeButton = rootView.findViewById(R.id.removeButton);
        removeButton.setOnClickListener(v -> {
            if (mRoute.length() > 0) {
                mMapHolder.getMap().getMapPosition(mMapPosition);
                Route.Instruction instruction = mRoute.getNearestInstruction(mMapPosition.getGeoPoint());
                mRoute.removeInstruction(instruction);
                MarkerItem marker = mPointLayer.getByUid(instruction);
                mPointLayer.removeItem(marker);
                mPointHistory.remove(instruction);
                mMapHolder.getMap().updateMap(true);
                mMapPosition = new MapPosition();
                updateTrackMeasurements();
            }
        });

        ImageButton undoButton = rootView.findViewById(R.id.undoButton);
        undoButton.setOnClickListener(v -> {
            if (!mPointHistory.isEmpty()) {
                GeoPoint point = mPointHistory.pop();
                Route.Instruction instruction = mRoute.getNearestInstruction(point);
                if (instruction == null) {
                    mPointHistory.clear();
                    return;
                }
                mRoute.removeInstruction(instruction);
                MarkerItem marker = mPointLayer.getByUid(instruction);
                mPointLayer.removeItem(marker);
                mMapHolder.getMap().updateMap(true);
                mMapPosition = new MapPosition();
                updateTrackMeasurements();
            }
        });

        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.e("R", "onAttach");
        try {
            mMapHolder = (MapHolder) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement MapHolder");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        mRouteLayer = new RouteLayer(mMapHolder.getMap(), Color.RED, 5, mRoute);
        mMapHolder.getMap().layers().add(mRouteLayer);
        Bitmap bitmap = new AndroidBitmap(MarkerFactory.getMarkerSymbol(getContext(), R.drawable.dot_black, Color.RED));
        MarkerSymbol symbol = new MarkerSymbol(bitmap, MarkerItem.HotspotPlace.CENTER);
        ArrayList<MarkerItem> items = new ArrayList<>(mRoute.length());
        for (GeoPoint point : mRoute.getCoordinates()) {
            items.add(new MarkerItem(point, null, null, point));
        }
        mPointLayer = new ItemizedLayer<>(mMapHolder.getMap(), items, symbol, MapTrek.density, this);
        mMapHolder.getMap().layers().add(mPointLayer);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateTrackMeasurements();
        mMapHolder.setObjectInteractionEnabled(false);
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapHolder.setObjectInteractionEnabled(true);
    }

    @Override
    public void onStop() {
        super.onStop();
        mMapHolder.getMap().layers().remove(mRouteLayer);
        mRouteLayer.onDetach();
        mRouteLayer = null;
        mMapHolder.getMap().layers().remove(mPointLayer);
        mPointLayer.onDetach();
        mPointLayer = null;
        mMapHolder.getMap().updateMap(true);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        Log.e("R", "onDetach");
        mMapHolder = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapPosition = null;
        mPointHistory = null;
        mRoute = null;
    }

    private void updateTrackMeasurements() {
        if (mRoute.length() > 1) {
            mMeasurementsView.setVisibility(View.VISIBLE);
            mDistanceView.setText(StringFormatter.distanceHP(mRoute.distance));
            mSizeView.setText(getResources().getQuantityString(R.plurals.numberOfSegments, mRoute.length() - 1, mRoute.length() - 1));
        } else {
            mMeasurementsView.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public boolean onItemSingleTapUp(int index, MarkerItem item) {
        return false;
    }

    @Override
    public boolean onItemLongPress(int index, MarkerItem item) {
        return false;
    }
}
