/*
 * Copyright 2018 Andrey Novikov
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
import android.support.design.widget.FloatingActionButton;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import mobi.maptrek.R;
import mobi.maptrek.maps.maptrek.Index;

public class BaseMapDownload extends Fragment implements OnBackPressedListener {
    private FragmentHolder mFragmentHolder;
    private Index mMapIndex;
    private TextView mMessageView;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_basemap_download, container, false);
        mMessageView = (TextView) rootView.findViewById(R.id.message);
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
    public void onResume() {
        super.onResume();
        long size = mMapIndex != null ? mMapIndex.getBaseMapSize() : Index.BASEMAP_SIZE_STUB * 1024 * 1024;
        mMessageView.setText(getString(R.string.msgBaseMapDownload, Formatter.formatFileSize(getContext(), size)));
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
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
