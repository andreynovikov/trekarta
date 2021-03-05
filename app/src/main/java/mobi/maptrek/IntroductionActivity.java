/*
 * Copyright 2021 Andrey Novikov
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

package mobi.maptrek;

import android.os.Bundle;
import androidx.fragment.app.Fragment;

import com.github.appintro.AppIntro;
import com.github.appintro.model.SliderPage;

import mobi.maptrek.fragments.IntroductionFragment;

public class IntroductionActivity extends AppIntro {
    public static final int CURRENT_INTRODUCTION = 6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int lastSeenIntroduction = Configuration.getLastSeenIntroduction();

        SliderPage sliderPage = new SliderPage();
        sliderPage.setBackgroundColor(getColor(R.color.explanationBackground));
        showStatusBar(true);
        setStatusBarColorRes(R.color.explanationBackground);
        setNavBarColorRes(R.color.explanationBackground);

        if (lastSeenIntroduction < 1) {
            // fresh installation

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
        }

        if (lastSeenIntroduction < 3) {
            // 2017.11
            sliderPage.setTitle(getString(R.string.introHikingTitle));
            sliderPage.setDescription(getString(R.string.introHiking));
            sliderPage.setImageDrawable(R.mipmap.hiking);
            addSlide(IntroductionFragment.newInstance(sliderPage));
        }

        if (lastSeenIntroduction < 6) {
            // 2021.03
            sliderPage.setTitle(getString(R.string.introCyclingTitle));
            sliderPage.setDescription(getString(R.string.introCycling));
            sliderPage.setImageDrawable(R.mipmap.cycling);
            addSlide(IntroductionFragment.newInstance(sliderPage));
        }

        if (lastSeenIntroduction < 3) {
            // 2017.11
            sliderPage.setTitle(getString(R.string.introSkiingTitle));
            sliderPage.setDescription(getString(R.string.introSkiing));
            sliderPage.setImageDrawable(R.mipmap.skiing);
            addSlide(IntroductionFragment.newInstance(sliderPage));
        }

        if (lastSeenIntroduction < 5 && BuildConfig.FULL_VERSION) {
            // 2019.09
            sliderPage.setTitle(getString(R.string.introNightModeTitle));
            sliderPage.setDescription(getString(R.string.introNightMode));
            sliderPage.setImageDrawable(R.mipmap.night);
            addSlide(IntroductionFragment.newInstance(sliderPage));
        }

        // TODO Do not show more then N slides at once
    }

    @Override
    public void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        Configuration.setLastSeenIntroduction(CURRENT_INTRODUCTION);
        finish();
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        Configuration.setLastSeenIntroduction(CURRENT_INTRODUCTION);
        finish();
    }
}
