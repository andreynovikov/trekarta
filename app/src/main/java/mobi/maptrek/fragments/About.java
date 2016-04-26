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
            version.setText(getString(R.string.version, versionBuild));
        } else {
            version.setText(getString(R.string.version, versionName));
        }

        // Links
        StringBuilder links = new StringBuilder();
        links.append("<a href=\"");
        links.append("http://maptrek.mobi/");
        links.append("\">");
        links.append("http://maptrek.mobi/");
        links.append("</a>");
        final TextView homeLinks = (TextView) view.findViewById(R.id.links);
        homeLinks.setText(Html.fromHtml(links.toString()));
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
        final StringBuilder out = new StringBuilder();
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
    }
}
