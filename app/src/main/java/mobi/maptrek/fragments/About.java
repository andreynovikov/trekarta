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
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import mobi.maptrek.BuildConfig;
import mobi.maptrek.R;

public class About extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about, container, false);
        updateAboutInfo(view);
        return view;
    }

    private void updateAboutInfo(final View view) {
        // version
        String versionName;
        int versionBuild = 0;
        try {
            versionName = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName;
            versionBuild = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionCode;
        } catch (PackageManager.NameNotFoundException ex) {
            versionName = "unknown";
        }
        final TextView version = (TextView) view.findViewById(R.id.version);
        if (BuildConfig.DEBUG) {
            version.setText(getString(R.string.version, Integer.toString(versionBuild)));
        } else {
            version.setText(getString(R.string.version, versionName));
        }

        // Links
        final TextView homeLinks = (TextView) view.findViewById(R.id.links);
        String links = "<a href=\"" +
                "https://trekarta.info/" +
                "\">" +
                "https://trekarta.info/" +
                "</a>";
        homeLinks.setText(Html.fromHtml(links));
        homeLinks.setMovementMethod(LinkMovementMethod.getInstance());

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
        try (Reader in = new InputStreamReader(is, "UTF-8")) {
            for (; ; ) {
                int rsz = in.read(buffer, 0, buffer.length);
                if (rsz < 0)
                    break;
                out.append(buffer, 0, rsz);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        final TextView license = (TextView) view.findViewById(R.id.license);
        license.setText(Html.fromHtml(out.toString()));
        license.setMovementMethod(LinkMovementMethod.getInstance());

        // Credits
        is = resources.openRawResource(R.raw.credits);
        out = new StringBuilder();
        try (Reader in = new InputStreamReader(is, "UTF-8")) {
            for (; ; ) {
                int rsz = in.read(buffer, 0, buffer.length);
                if (rsz < 0)
                    break;
                out.append(buffer, 0, rsz);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        final TextView credits = (TextView) view.findViewById(R.id.credits);
        credits.setText(Html.fromHtml(out.toString()));
        credits.setMovementMethod(LinkMovementMethod.getInstance());
    }
}
