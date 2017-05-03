package mobi.maptrek.fragments;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.io.File;
import java.util.Locale;

import mobi.maptrek.MapTrek;
import mobi.maptrek.R;
import mobi.maptrek.provider.ExportProvider;

public class CrashReport extends Fragment implements OnBackPressedListener {
    private FragmentHolder mFragmentHolder;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_crash_report, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        FloatingActionButton floatingButton = mFragmentHolder.enableActionButton();
        floatingButton.setImageDrawable(getContext().getDrawable(R.drawable.ic_send));
        floatingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"novikov+maptrek@gmail.com"});
                File file = MapTrek.getApplication().getExceptionLog();
                intent.putExtra(Intent.EXTRA_STREAM, ExportProvider.getUriForFile(getContext(), file));
                intent.setType("vnd.android.cursor.dir/email");
                intent.putExtra(Intent.EXTRA_SUBJECT, "MapTrek crash report");
                StringBuilder text = new StringBuilder();
                text.append("Device : ").append(Build.DEVICE);
                text.append("\nBrand : ").append(Build.BRAND);
                text.append("\nModel : ").append(Build.MODEL);
                text.append("\nProduct : ").append(Build.PRODUCT);
                text.append("\nLocale : ").append(Locale.getDefault().toString());
                text.append("\nBuild : ").append(Build.DISPLAY);
                text.append("\nVersion : ").append(Build.VERSION.RELEASE);
                try {
                    PackageInfo info = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
                    if (info != null) {
                        text.append("\nApk Version : ").append(info.versionCode).append(" ").append(info.versionName);
                    }
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                }
                intent.putExtra(Intent.EXTRA_TEXT, text.toString());
                startActivity(Intent.createChooser(intent, getString(R.string.send_crash_report)));
                mFragmentHolder.disableActionButton();
                mFragmentHolder.popCurrent();
            }
        });
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
}
