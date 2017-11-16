package mobi.maptrek;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.WindowManager;

import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.model.SliderPage;

import mobi.maptrek.fragments.IntroductionFragment;

public class IntroductionActivity extends AppIntro {
    public static final int CURRENT_INTRODUCTION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        SliderPage sliderPage = new SliderPage();
        sliderPage.setTitle("Hillshades");
        sliderPage.setDescription("Now you can download and view hillshades atop maps. Your custom maps will be shaded as well.");
        sliderPage.setImageDrawable(R.mipmap.hillshades);
        sliderPage.setBgColor(Color.parseColor("#2196F3")); // Blue 500
        addSlide(IntroductionFragment.newInstance(sliderPage));
    }

    @Override
    public void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        Configuration.setLastSeenIntroduction(IntroductionActivity.CURRENT_INTRODUCTION);
        finish();
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        Configuration.setLastSeenIntroduction(IntroductionActivity.CURRENT_INTRODUCTION);
        finish();
    }
}
