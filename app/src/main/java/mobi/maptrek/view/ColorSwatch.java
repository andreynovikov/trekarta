/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package mobi.maptrek.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.android.colorpicker.ColorStateDrawable;

/**
 * Creates a circular swatch of a specified color.
 */
public class ColorSwatch extends FrameLayout {
    private ImageView mSwatchImage;
    private int mColor;

    public ColorSwatch(Context context, AttributeSet attrs) {
        super(context, attrs);
        LayoutInflater.from(context).inflate(com.android.colorpicker.R.layout.color_picker_swatch, this);
        mSwatchImage = (ImageView) findViewById(com.android.colorpicker.R.id.color_picker_swatch);
        setColor(Color.BLACK);
    }

    public void setColor(int color) {
        mColor = color;
        Drawable[] colorDrawable = new Drawable[]
                {getContext().getResources().getDrawable(com.android.colorpicker.R.drawable.color_picker_swatch, getContext().getTheme())};
        mSwatchImage.setImageDrawable(new ColorStateDrawable(colorDrawable, mColor));
    }

    public int getColor() {
        return mColor;
    }
}
