package mobi.maptrek;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.WindowManager;

import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.model.SliderPage;

import mobi.maptrek.fragments.IntroductionFragment;

public class IntroductionActivity extends AppIntro {
    public static final int CURRENT_INTRODUCTION = 3;
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
            sliderPage.setTitle(getString(R.string.introOfflineMapsTitle));
            String description = getString(R.string.introOfflineMaps);
            if (BuildConfig.FULL_VERSION) {
                description += " ";
                description += getString(R.string.introOfflineMapsFull);
            }
            sliderPage.setDescription(description);
            sliderPage.setImageDrawable(R.mipmap.maps);
            addSlide(IntroductionFragment.newInstance(sliderPage));

            sliderPage.setTitle(getString(R.string.introPlacesTitle));
            if (BuildConfig.FULL_VERSION)
                description = getString(R.string.introPlacesPrefixFull);
            else
                description = getString(R.string.introPlacesPrefix);
            description += " ";
            description += getString(R.string.introPlaces);
            sliderPage.setDescription(description);
            sliderPage.setImageDrawable(R.mipmap.places);
            addSlide(IntroductionFragment.newInstance(sliderPage));

            sliderPage.setTitle(getString(R.string.introTracksTitle));
            sliderPage.setDescription(getString(R.string.introTracks));
            sliderPage.setImageDrawable(R.mipmap.tracking);
            addSlide(IntroductionFragment.newInstance(sliderPage));

            sliderPage.setTitle(getString(R.string.introOffroadTitle));
            sliderPage.setDescription(getString(R.string.introOffroad));
            sliderPage.setImageDrawable(R.mipmap.offroad);
            addSlide(IntroductionFragment.newInstance(sliderPage));

            mLastSeenIntroduction = 1; // fresh installation
        }

        if (mLastSeenIntroduction < 3) {
            sliderPage.setTitle(getString(R.string.introHikingTitle));
            sliderPage.setDescription(getString(R.string.introHiking));
            sliderPage.setImageDrawable(R.mipmap.hiking);
            addSlide(IntroductionFragment.newInstance(sliderPage));

            sliderPage.setTitle(getString(R.string.introSkiingTitle));
            sliderPage.setDescription(getString(R.string.introSkiing));
            sliderPage.setImageDrawable(R.mipmap.skiing);
            addSlide(IntroductionFragment.newInstance(sliderPage));
            mLastSeenIntroduction = 3; // 2017.11
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
