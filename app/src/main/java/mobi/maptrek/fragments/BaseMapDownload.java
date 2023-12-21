/*
 * Copyright 2023 Andrey Novikov
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

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import mobi.maptrek.R;
import mobi.maptrek.maps.maptrek.Index;

public class BaseMapDownload extends DialogFragment {
    @NonNull
    private final Index mMapIndex;

    public BaseMapDownload(@NonNull Index mapIndex) {
        super();
        mMapIndex = mapIndex;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final View dialogView = getLayoutInflater().inflate(R.layout.fragment_basemap_download, null);

        TextView messageView = dialogView.findViewById(R.id.message);
        long size = mMapIndex.getBaseMapSize();
        messageView.setText(getString(R.string.msgBaseMapDownload, Formatter.formatFileSize(getContext(), size)));

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
        dialogBuilder.setPositiveButton(R.string.actionDownload, (dialog, which) -> mMapIndex.downloadBaseMap());
        dialogBuilder.setNegativeButton(R.string.actionSkip, (dialog, which) -> {});
        dialogBuilder.setView(dialogView);

        Dialog dialog = dialogBuilder.create();

        Window window = dialog.getWindow();
        assert window != null;
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.gravity = Gravity.BOTTOM;
        window.setAttributes(lp);

        return dialog;
    }
}
