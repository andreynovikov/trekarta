package mobi.maptrek.fragments;

import android.app.Fragment;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import mobi.maptrek.MapSelectionListener;
import mobi.maptrek.R;

public class MapSelection extends Fragment implements OnBackPressedListener, MapSelectionListener {
    private OnMapActionListener mListener;
    private FragmentHolder mFragmentHolder;
    private boolean[][] mSelectionState;
    private TextView mMessageView;
    private Resources mResources;
    private int mCounter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_map_selection, container, false);
        mMessageView = (TextView) rootView.findViewById(R.id.message);
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mListener.onBeginMapSelection(this);

        FloatingActionButton floatingButton = mFragmentHolder.enableActionButton();
        floatingButton.setImageDrawable(getContext().getDrawable(R.drawable.ic_file_download));
        floatingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mListener.onDownloadSelectedMaps(mSelectionState);
                mListener.onFinishMapSelection();
                mFragmentHolder.disableActionButton();
                mFragmentHolder.popCurrent();
            }
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            mListener = (OnMapActionListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnMapActionListener");
        }
        try {
            mFragmentHolder = (FragmentHolder) context;
            mFragmentHolder.addBackClickListener(this);
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement FragmentHolder");
        }
        mResources = getResources();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mFragmentHolder.removeBackClickListener(this);
        mFragmentHolder = null;
        mListener = null;
        mResources = null;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onBackClick() {
        mFragmentHolder.disableActionButton();
        mListener.onFinishMapSelection();
        return false;
    }

    @Override
    public void onMapSelected(int x, int y) {
        mCounter = mSelectionState[x][y] ? mCounter + 1 : mCounter - 1;
        mMessageView.setText(mResources.getQuantityString(R.plurals.itemsSelected, mCounter, mCounter));
    }

    @Override
    public void registerMapSelectionState(boolean[][] selectedState) {
        mSelectionState = selectedState;
    }
}
