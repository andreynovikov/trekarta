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

import androidx.core.splashscreen.SplashScreen;
import androidx.fragment.app.Fragment;

import com.github.appintro.AppIntro;
import com.github.appintro.model.SliderPage;

import mobi.maptrek.fragments.Introduction;

public class IntroductionActivity extends AppIntro {
    public static final int CURRENT_INTRODUCTION = 6;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
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
            sliderPage.setDescription(getString(R.string.introOfflineMaps));
            sliderPage.setImageDrawable(R.mipmap.maps);
            addSlide(Introduction.newInstance(sliderPage));

            sliderPage.setTitle(getString(R.string.introPlacesTitle));
            sliderPage.setDescription(getString(R.string.introPlaces));
            sliderPage.setImageDrawable(R.mipmap.places);
            addSlide(Introduction.newInstance(sliderPage));

            sliderPage.setTitle(getString(R.string.introTracksTitle));
            sliderPage.setDescription(getString(R.string.introTracks));
            sliderPage.setImageDrawable(R.mipmap.tracking);
            addSlide(Introduction.newInstance(sliderPage));

            sliderPage.setTitle(getString(R.string.introOffroadTitle));
            sliderPage.setDescription(getString(R.string.introOffroad));
            sliderPage.setImageDrawable(R.mipmap.offroad);
            addSlide(Introduction.newInstance(sliderPage));
        }

        if (lastSeenIntroduction < 3) {
            // 2017.11
            sliderPage.setTitle(getString(R.string.introHikingTitle));
            sliderPage.setDescription(getString(R.string.introHiking));
            sliderPage.setImageDrawable(R.mipmap.hiking);
            addSlide(Introduction.newInstance(sliderPage));
        }

        if (lastSeenIntroduction < 6) {
            // 2021.03
            sliderPage.setTitle(getString(R.string.introCyclingTitle));
            sliderPage.setDescription(getString(R.string.introCycling));
            sliderPage.setImageDrawable(R.mipmap.cycling);
            addSlide(Introduction.newInstance(sliderPage));
        }

        if (lastSeenIntroduction < 3) {
            // 2017.11
            sliderPage.setTitle(getString(R.string.introSkiingTitle));
            sliderPage.setDescription(getString(R.string.introSkiing));
            sliderPage.setImageDrawable(R.mipmap.skiing);
            addSlide(Introduction.newInstance(sliderPage));
        }

        if (lastSeenIntroduction < 5) {
            // 2019.09
            sliderPage.setTitle(getString(R.string.introNightModeTitle));
            sliderPage.setDescription(getString(R.string.introNightMode));
            sliderPage.setImageDrawable(R.mipmap.night);
            addSlide(Introduction.newInstance(sliderPage));
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
