package mobi.maptrek.fragments;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import mobi.maptrek.R;
import mobi.maptrek.maps.maptrek.Index;

public class BaseMapDownload extends Fragment implements OnBackPressedListener {
    private OnMapActionListener mListener;

    private FragmentHolder mFragmentHolder;
    private Index mMapIndex;
    private TextView mMessageView;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_basemap_download, container, false);
        mMessageView = (TextView) rootView.findViewById(R.id.message);
        mMessageView.setText(getString(R.string.msgBaseMapDownload, Formatter.formatFileSize(getContext(), mMapIndex.getBaseMapSize())));
        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        FloatingActionButton floatingButton = mFragmentHolder.enableActionButton();
        floatingButton.setImageDrawable(getContext().getDrawable(R.drawable.ic_file_download));
        floatingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mMapIndex.downloadBaseMap();
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
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mFragmentHolder.removeBackClickListener(this);
        mFragmentHolder = null;
        mListener = null;
    }

    @Override
    public boolean onBackClick() {
        mFragmentHolder.disableActionButton();
        return false;
    }

    public void setMapIndex(Index mapIndex) {
        mMapIndex = mapIndex;
    }

}
