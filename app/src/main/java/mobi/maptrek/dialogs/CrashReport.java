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

package mobi.maptrek.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;

import java.io.File;
import java.util.Locale;

import mobi.maptrek.MapTrek;
import mobi.maptrek.R;
import mobi.maptrek.databinding.DialogCrashReportBinding;
import mobi.maptrek.provider.ExportProvider;

public class CrashReport extends DialogFragment {
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        DialogCrashReportBinding viewBinding = DialogCrashReportBinding.inflate(getLayoutInflater());

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
        dialogBuilder.setPositiveButton(R.string.actionSend, (dialog, which) -> sendReport(viewBinding.message.getText().toString()));
        dialogBuilder.setNegativeButton(R.string.cancel, (dialog, which) -> {});
        dialogBuilder.setView(viewBinding.getRoot());

        Dialog dialog = dialogBuilder.create();

        Window window = dialog.getWindow();
        assert window != null;
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.gravity = Gravity.BOTTOM;
        window.setAttributes(lp);

        return dialog;
    }

    private void sendReport(String message) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"novikov+trekarta@gmail.com"});
        File file = MapTrek.getApplication().getExceptionLog();
        intent.putExtra(Intent.EXTRA_STREAM, ExportProvider.getUriForFile(requireContext(), file));
        intent.setType("vnd.android.cursor.dir/email");
        intent.putExtra(Intent.EXTRA_SUBJECT, "Trekarta crash report");
        StringBuilder text = new StringBuilder();
        text.append(message);
        text.append("\n\nDevice : ").append(Build.DEVICE);
        text.append("\nBrand : ").append(Build.BRAND);
        text.append("\nModel : ").append(Build.MODEL);
        text.append("\nProduct : ").append(Build.PRODUCT);
        text.append("\nLocale : ").append(Locale.getDefault());
        text.append("\nBuild : ").append(Build.DISPLAY);
        text.append("\nVersion : ").append(Build.VERSION.RELEASE);
        try {
            PackageInfo info = requireContext().getPackageManager().getPackageInfo(requireContext().getPackageName(), 0);
            if (info != null) {
                text.append("\nApp code : ").append(info.versionCode);
                text.append("\nApp version : ").append(info.versionName);
            }
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        intent.putExtra(Intent.EXTRA_TEXT, text.toString());
        startActivity(Intent.createChooser(intent, getString(R.string.send_crash_report)));
    }
}
