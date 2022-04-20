/*
 * Copyright 2020 Andrey Novikov
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

import android.app.Activity;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

import mobi.maptrek.BuildConfig;
import mobi.maptrek.MapTrek;
import mobi.maptrek.R;
import mobi.maptrek.databinding.FragmentAboutBinding;

public class About extends Fragment {
    private FragmentAboutBinding mViews;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mViews = FragmentAboutBinding.inflate(inflater, container, false);
        updateAboutInfo();
        return mViews.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mViews = null;
    }

    private void updateAboutInfo() {
        // version
        String versionName;
        try {
            Activity activity = getActivity();
            if (!BuildConfig.DEBUG && activity != null)
                versionName = activity.getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName;
            else
                versionName = Integer.toString(MapTrek.versionCode);
        } catch (PackageManager.NameNotFoundException e) {
            versionName = Integer.toString(MapTrek.versionCode);
        }
        mViews.version.setText(getString(R.string.version, versionName));

        // Links
        String links = "<a href=\"" +
                "https://trekarta.info/" +
                "\">" +
                "https://trekarta.info/" +
                "</a>";
        mViews.links.setText(Html.fromHtml(links));
        mViews.links.setMovementMethod(LinkMovementMethod.getInstance());

        /*
        // Donations
        StringBuilder donations = new StringBuilder();
        donations.append("<a href=\"");
        donations.append(getString(R.string.playuri));
        donations.append("\">");
        donations.append(getString(R.string.donate_google));
        donations.append("</a><br /><a href=\"");
        donations.append(getString(R.string.paypaluri));
        donations.append("\">");
        donations.append(getString(R.string.donate_paypal));
        donations.append("</a>");

        final TextView donationlinks = (TextView) view.findViewById(R.id.donationlinks);
        donationlinks.setText(Html.fromHtml(donations.toString()));
        donationlinks.setMovementMethod(LinkMovementMethod.getInstance());
        */

        // License
        Resources resources = getResources();
        InputStream is = resources.openRawResource(R.raw.license);
        final char[] buffer = new char[100];
        StringBuilder out = new StringBuilder();
        try (Reader in = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            for (; ; ) {
                int rsz = in.read(buffer, 0, buffer.length);
                if (rsz < 0)
                    break;
                out.append(buffer, 0, rsz);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        mViews.license.setText(Html.fromHtml(out.toString()));
        mViews.license.setMovementMethod(LinkMovementMethod.getInstance());

        // Credits
        is = resources.openRawResource(R.raw.credits);
        out = new StringBuilder();
        try (Reader in = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            for (; ; ) {
                int rsz = in.read(buffer, 0, buffer.length);
                if (rsz < 0)
                    break;
                out.append(buffer, 0, rsz);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        mViews.credits.setText(Html.fromHtml(out.toString()));
        mViews.credits.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
