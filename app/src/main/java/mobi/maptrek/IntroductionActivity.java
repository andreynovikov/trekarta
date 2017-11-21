package mobi.maptrek;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.WindowManager;

import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.model.SliderPage;

import mobi.maptrek.fragments.IntroductionFragment;

public class IntroductionActivity extends AppIntro {
    public static final int CURRENT_INTRODUCTION = 2;
    int mLastSeenIntroduction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        mLastSeenIntroduction = Configuration.getLastSeenIntroduction();

        SliderPage sliderPage = new SliderPage();
        sliderPage.setBgColor(Color.parseColor("#2196F3")); // Blue 500

        if (mLastSeenIntroduction < 1) {
            sliderPage.setTitle("Hillshades");
            sliderPage.setDescription("Now you can download and view hillshades atop maps. Your custom maps will be shaded as well.");
            sliderPage.setImageDrawable(R.mipmap.hillshades);
            addSlide(IntroductionFragment.newInstance(sliderPage));
            mLastSeenIntroduction = 1;
        }

        if (mLastSeenIntroduction < 2) {
            sliderPage.setTitle("Location sharing");
            sliderPage.setDescription("Now you have more options on how to share your place or location on map. Not only you can send its coordinates as text, but you can open it in some other map application or share it as GPX or KML file.");
            sliderPage.setImageDrawable(R.mipmap.sharepoint);
            addSlide(IntroductionFragment.newInstance(sliderPage));
            mLastSeenIntroduction = 2;
        }

        // TODO Do not show more then N slides at once
    }

    @Override
    public void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        Configuration.setLastSeenIntroduction(mLastSeenIntroduction);
        finish();
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        Configuration.setLastSeenIntroduction(mLastSeenIntroduction);
        finish();
    }
}
