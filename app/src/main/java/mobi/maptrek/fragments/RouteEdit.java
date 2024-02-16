/*
 * Copyright 2024 Andrey Novikov
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

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import org.oscim.android.canvas.AndroidBitmap;
import org.oscim.backend.canvas.Bitmap;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Stack;

import mobi.maptrek.MapHolder;
import mobi.maptrek.MapTrek;
import mobi.maptrek.R;
import mobi.maptrek.data.Route;
import mobi.maptrek.data.source.FileDataSource;
import mobi.maptrek.data.style.RouteStyle;
import mobi.maptrek.databinding.FragmentRouteEditBinding;
import mobi.maptrek.io.Manager;
import mobi.maptrek.io.RouteManager;
import mobi.maptrek.layers.RouteLayer;
import mobi.maptrek.layers.marker.ItemizedLayer;
import mobi.maptrek.layers.marker.MarkerItem;
import mobi.maptrek.layers.marker.MarkerSymbol;
import mobi.maptrek.ui.TextInputDialogFragment;
import mobi.maptrek.util.FileUtils;
import mobi.maptrek.util.HelperUtils;
import mobi.maptrek.util.MarkerFactory;
import mobi.maptrek.util.StringFormatter;

public class RouteEdit extends Fragment implements ItemizedLayer.OnItemGestureListener<MarkerItem>, TextInputDialogFragment.TextInputDialogCallback {
    private static final Logger logger = LoggerFactory.getLogger(RouteEdit.class);

    private static final String ROUTE_NAME = "route_name";
    private static final String POINT_NAME = "point_name";

    private FragmentHolder mFragmentHolder;
    private MapHolder mMapHolder;

    private MapPosition mMapPosition;
    private RouteLayer mRouteLayer;
    private ItemizedLayer<MarkerItem> mPointLayer;
    private TextInputDialogFragment textInputDialog;
    private RouteEditViewModel viewModel;
    private FragmentRouteEditBinding viewBinding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mMapPosition = new MapPosition();
        viewModel = new ViewModelProvider(this).get(RouteEditViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewBinding = FragmentRouteEditBinding.inflate(inflater, container, false);
        return viewBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewBinding.addButton.setOnClickListener(v -> {
            if (mMapHolder.getMap().getMapPosition(mMapPosition)) {
                GeoPoint point = mMapPosition.getGeoPoint();
                String title = viewModel.route.size() == 0 ? getString(R.string.start_point) : null;
                Route.Instruction instruction = viewModel.route.addInstruction(point);
                instruction.text = title;
                MarkerItem marker = new MarkerItem(instruction, title, null, point);
                mPointLayer.addItem(marker);
                viewModel.pointHistory.push(instruction);
                mMapHolder.getMap().updateMap(true);
                updateTrackMeasurements();
                if (viewModel.route.size() > 1)
                    updateFinishPoint();
            }
        });

        viewBinding.insertButton.setOnClickListener(v -> {
            if (mMapHolder.getMap().getMapPosition(mMapPosition)) {
                GeoPoint point = mMapPosition.getGeoPoint();
                Route.Instruction instruction = viewModel.route.insertInstruction(point);
                MarkerItem marker = new MarkerItem(instruction, null, null, point);
                mPointLayer.addItem(marker);
                viewModel.pointHistory.push(instruction);
                mMapHolder.getMap().updateMap(true);
                updateTrackMeasurements();
            }
        });

        viewBinding.removeButton.setOnClickListener(v -> {
            if (viewModel.route.size() > 0) {
                mMapHolder.getMap().getMapPosition(mMapPosition);
                Route.Instruction instruction = viewModel.route.getNearestInstruction(mMapPosition.getGeoPoint());
                viewModel.route.removeInstruction(instruction);
                MarkerItem marker = mPointLayer.getByUid(instruction);
                mPointLayer.removeItem(marker);
                viewModel.pointHistory.remove(instruction);
                mMapHolder.getMap().updateMap(true);
                mMapPosition = new MapPosition();
                updateTrackMeasurements();
                updateStartPoint();
                updateFinishPoint();
            }
        });

        viewBinding.undoButton.setOnClickListener(v -> {
            if (!viewModel.pointHistory.isEmpty()) {
                Route.Instruction instruction = viewModel.pointHistory.pop();
                viewModel.route.removeInstruction(instruction);
                MarkerItem marker = mPointLayer.getByUid(instruction);
                mPointLayer.removeItem(marker);
                mMapHolder.getMap().updateMap(true);
                mMapPosition = new MapPosition();
                updateTrackMeasurements();
                updateFinishPoint();
            }
        });

        viewBinding.saveButton.setOnClickListener(v -> {
            if (viewModel.route.size() < 2) {
                HelperUtils.showError(getString(R.string.msgTooFewRouteLegs), mFragmentHolder.getCoordinatorLayout());
                return;
            }
            textInputDialog = new TextInputDialogFragment.Builder()
                    .setId(ROUTE_NAME)
                    .setCallback(this)
                    .setTitle(getString(R.string.title_input_name))
                    .setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
                    .create();
            textInputDialog.show(getParentFragmentManager(), "titleInput");
        });

        textInputDialog = (TextInputDialogFragment) getParentFragmentManager().findFragmentByTag("titleInput");
        if (textInputDialog != null)
            textInputDialog.setCallback(this);
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            mMapHolder = (MapHolder) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context + " must implement MapHolder");
        }
        mFragmentHolder = (FragmentHolder) context;
    }

    @Override
    public void onStart() {
        super.onStart();
        Context context = requireContext();
        mRouteLayer = new RouteLayer(mMapHolder.getMap(), RouteStyle.DEFAULT_COLOR, 5, viewModel.route);
        mMapHolder.getMap().layers().add(mRouteLayer);
        Bitmap bitmap = new AndroidBitmap(MarkerFactory.getMarkerSymbol(context, R.drawable.dot_black, RouteStyle.DEFAULT_COLOR));
        MarkerSymbol symbol = new MarkerSymbol(bitmap, MarkerItem.HotspotPlace.CENTER);
        ArrayList<MarkerItem> items = new ArrayList<>(viewModel.route.size());
        for (Route.Instruction instruction : viewModel.route.getInstructions()) {
            items.add(new MarkerItem(instruction, instruction.text, null, instruction));
        }
        int strokeColor = getResources().getColor(R.color.colorBackground, context.getTheme());
        mPointLayer = new ItemizedLayer<>(mMapHolder.getMap(), items, symbol, MapTrek.density, strokeColor, this);
        mMapHolder.getMap().layers().add(mPointLayer);
    }

    @Override
    public void onResume() {
        super.onResume();
        updateTrackMeasurements();
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
        mMapHolder = null;
        mFragmentHolder = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapPosition = null;
    }

    private void updateTrackMeasurements() {
        int length = Math.max(viewModel.route.size() - 1, 0);
        viewBinding.distance.setText(StringFormatter.distanceHP(viewModel.route.distance));
        viewBinding.size.setText(getResources().getQuantityString(R.plurals.numberOfLegs, length, length));
    }

    private void updateStartPoint() {
        if (viewModel.route.size() < 1)
            return;
        Route.Instruction first = viewModel.route.get(0);
        if (first.text == null) {
            String title = getString(R.string.start_point);
            MarkerItem marker = mPointLayer.getByUid(first);
            first.text = title;
            marker.title = title;
            mPointLayer.updateItems();
            mMapHolder.getMap().updateMap(true);
        }
    }

    private void updateFinishPoint() {
        if (viewModel.route.size() < 2)
            return;
        String title = getString(R.string.finish_point);
        Route.Instruction last = viewModel.route.get(-1);
        if (last.text == null) {
            MarkerItem marker = mPointLayer.getByUid(last);
            last.text = title;
            marker.title = title;
        }
        Route.Instruction prev = last.getPrevious();
        if (title.equals(prev.text)) {
            MarkerItem marker = mPointLayer.getByUid(prev);
            prev.text = null;
            marker.title = null;
        }
        mPointLayer.updateItems();
        mMapHolder.getMap().updateMap(true);
    }

    private void saveDescription(String text) {
        text = text.trim();
        if (text.isEmpty())
            text = null;
        if (Objects.equals(text, viewModel.editedInstruction.text))
            return;
        viewModel.editedInstruction.text = text;
        mPointLayer.getByUid(viewModel.editedInstruction).title = text;
        mPointLayer.updateItems();
        mMapHolder.getMap().updateMap(true);
    }

    private void saveRoute(String name) {
        if (name.trim().isEmpty())
            return;
        Context context = requireContext();
        File dataDir = context.getExternalFilesDir("data");
        if (dataDir == null) {
            logger.error("Can not save route: application data folder missing");
            HelperUtils.showError(getString(R.string.error), mFragmentHolder.getCoordinatorLayout());
            return;
        }
        viewModel.route.name = name;
        FileDataSource source = new FileDataSource();
        source.name = viewModel.route.name;
        File file = new File(dataDir, FileUtils.sanitizeFilename(source.name) + RouteManager.EXTENSION);
        source.path = file.getAbsolutePath();
        source.routes.add(viewModel.route);
        Manager.save(source, new Manager.OnSaveListener() {
            @Override
            public void onSaved(FileDataSource source) {
                mFragmentHolder.popCurrent();
            }

            @Override
            public void onError(FileDataSource source, Exception e) {
                HelperUtils.showSaveError(context, mFragmentHolder.getCoordinatorLayout(), e);
            }
        });
    }

    @Override
    public boolean onItemSingleTapUp(int index, MarkerItem item) {
        viewModel.editedInstruction = (Route.Instruction) item.getUid();
        TextInputDialogFragment.Builder builder = new TextInputDialogFragment.Builder()
                .setId(POINT_NAME)
                .setOldValue(item.title)
                .setTitle(getString(R.string.description))
                .setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
                .setCallback(this);
        TextInputDialogFragment textInputDialog = builder.create();
        textInputDialog.show(getParentFragmentManager(), "titleInput");
        return true;
    }

    @Override
    public boolean onItemLongPress(int index, MarkerItem item) {
        return false;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override
    public void afterTextChanged(Editable s) {
    }

    @Override
    public void onTextInputPositiveClick(String id, String inputText) {
        if (ROUTE_NAME.equals(id))
            saveRoute(inputText);
        if (POINT_NAME.equals(id)) {
            saveDescription(inputText);
            viewModel.editedInstruction = null;
        }
    }

    @Override
    public void onTextInputNegativeClick(String id) {
        if (POINT_NAME.equals(id))
            viewModel.editedInstruction = null;
    }

    public static class RouteEditViewModel extends ViewModel {
        private final Route route = new Route();
        private final Stack<Route.Instruction> pointHistory = new Stack<>();
        private Route.Instruction editedInstruction;
    }
}
