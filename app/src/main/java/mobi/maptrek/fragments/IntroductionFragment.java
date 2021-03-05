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

package mobi.maptrek.fragments;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import com.github.appintro.AppIntroBaseFragment;
import com.github.appintro.model.SliderPage;

import de.hdodenhof.circleimageview.CircleImageView;
import mobi.maptrek.R;

public class IntroductionFragment extends AppIntroBaseFragment {
    private static final String ARG_DRAWABLE = "drawable";
    private CircleImageView mImageView;

    public static IntroductionFragment newInstance(SliderPage sliderPage) {
        IntroductionFragment slide = new IntroductionFragment();
        slide.setArguments(sliderPage.toBundle());
        return slide;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mImageView = null;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        final View rootView = super.onCreateView(inflater, container, savedInstanceState);
        assert rootView != null;
        mImageView = rootView.findViewById(R.id.image);

        ViewTreeObserver viewTreeObserver = rootView.getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @SuppressWarnings("SuspiciousNameCombination")
                @Override
                public void onGlobalLayout() {
                    rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    int viewWidth = mImageView.getMeasuredWidth();
                    int viewHeight = mImageView.getMeasuredHeight();
                    if (viewWidth < viewHeight)
                        viewHeight = viewWidth;
                    if (viewWidth > viewHeight)
                        viewWidth = viewHeight;
                    mImageView.setImageBitmap(decodeSampledBitmapFromResource(getResources(),
                            getArguments().getInt(ARG_DRAWABLE), viewWidth, viewHeight));
                }
            });
        }
        return rootView;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_introduction;
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            // Calculate the largest inSampleSize value that is a power of 2
            while (height / inSampleSize > reqHeight && width / inSampleSize >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    private static Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight) {
        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(res, resId, options);
        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeResource(res, resId, options);
    }
}
