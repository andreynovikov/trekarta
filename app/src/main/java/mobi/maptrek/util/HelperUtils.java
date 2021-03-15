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

package mobi.maptrek.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.snackbar.Snackbar;
import android.view.View;
import android.widget.TextView;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;

import mobi.maptrek.BuildConfig;
import mobi.maptrek.Configuration;
import mobi.maptrek.R;

public class HelperUtils {
    private static boolean isShowingTargetedAdvice = false;

    public static void showError(String message, CoordinatorLayout coordinatorLayout) {
        showError(message, R.string.actionDismiss, null, coordinatorLayout);
    }

    public static void showError(String message, @StringRes int action, View.OnClickListener listener, CoordinatorLayout coordinatorLayout) {
        final Snackbar snackbar = Snackbar.make(coordinatorLayout, message, Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction(action, view -> {
            if (listener != null)
                listener.onClick(view);
            snackbar.dismiss();
        });
        TextView snackbarTextView = snackbar.getView().findViewById(com.google.android.material.R.id.snackbar_text);
        snackbarTextView.setMaxLines(99);
        snackbar.show();
    }

    public static void showSaveError(Context context, CoordinatorLayout coordinatorLayout, Exception e) {
        showError(context.getString(R.string.msgSaveFailed, e.getMessage()), coordinatorLayout);
    }

    public static void showAdvice(final long advice, int messageResId, CoordinatorLayout coordinatorLayout) {
        if (Configuration.getAdviceState(advice)) {
            Snackbar snackbar = Snackbar
                    //TODO Return back actionGotIt sometime
                    .make(coordinatorLayout, messageResId, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.ok, view -> Configuration.setAdviceState(advice));
            TextView snackbarTextView = snackbar.getView().findViewById(com.google.android.material.R.id.snackbar_text);
            snackbarTextView.setMaxLines(99);
            snackbar.show();
        }
    }

    public static boolean showTargetedAdvice(Activity activity, final long advice, @StringRes int messageResId, View focusOn, @DrawableRes int icon) {
        if (isShowingTargetedAdvice || !Configuration.getAdviceState(advice))
            return false;

        @SuppressLint("UseCompatLoadingForDrawables")
        Drawable drawable = activity.getDrawable(icon);
        if (drawable != null)
            drawable.setTint(activity.getResources().getColor(R.color.textColorPrimary, activity.getTheme()));
        TapTarget target = TapTarget.forView(focusOn, activity.getString(messageResId))
                .outerCircleColor(R.color.explanationBackground)
                .targetCircleColor(R.color.colorBackground)
                .textColor(android.R.color.white)
                .icon(drawable);
        showTargetedAdvice(activity, advice, target);
        return true;
    }

    public static boolean showTargetedAdvice(Activity activity, final long advice, @StringRes int messageResId, View focusOn, boolean transparent) {
        if (isShowingTargetedAdvice || !Configuration.getAdviceState(advice))
            return false;

        TapTarget target;
        if (transparent) {
            Rect r = new Rect();
            focusOn.getGlobalVisibleRect(r);
            target = TapTarget.forBounds(r, activity.getString(messageResId)).transparentTarget(true);
        } else {
            target = TapTarget.forView(focusOn, activity.getString(messageResId));
        }
        target.outerCircleColor(R.color.explanationBackground)
                .targetCircleColor(R.color.colorBackground)
                .textColor(android.R.color.white);

        showTargetedAdvice(activity, advice, target);
        return true;
    }

    public static boolean showTargetedAdvice(Activity activity, long advice, @StringRes int messageResId, Rect rect) {
        if (isShowingTargetedAdvice || !Configuration.getAdviceState(advice))
            return false;

        TapTarget target = TapTarget.forBounds(rect, activity.getString(messageResId))
                .outerCircleColor(R.color.explanationBackground)
                .targetCircleColor(R.color.colorBackground)
                .textColor(android.R.color.white)
                .transparentTarget(true);
        showTargetedAdvice(activity, advice, target);
        return true;
    }

    public static boolean showTargetedAdvice(Dialog dialog, long advice, @StringRes int messageResId, Rect rect) {
        if (isShowingTargetedAdvice || !Configuration.getAdviceState(advice))
            return false;

        TapTarget target = TapTarget.forBounds(rect, dialog.getContext()
                .getString(messageResId))
                .outerCircleColor(R.color.explanationBackground)
                .targetCircleColor(R.color.colorBackground)
                .textColor(android.R.color.white)
                .transparentTarget(true);
        showTargetedAdvice(dialog, advice, target);
        return true;
    }

    private static void showTargetedAdvice(Activity activity, final long advice, TapTarget target) {
        if (BuildConfig.IS_TESTING.get())
            return;
        isShowingTargetedAdvice = true;
        target.tintTarget(false);
        TapTargetView.showFor(activity, target,
                new TapTargetView.Listener() {
                    @Override
                    public void onOuterCircleClick(TapTargetView view) {
                        view.dismiss(false);
                    }

                    @Override
                    public void onTargetDismissed(TapTargetView view, boolean userInitiated) {
                        Configuration.setAdviceState(advice);
                        isShowingTargetedAdvice = false;
                    }
                });
    }

    private static void showTargetedAdvice(Dialog dialog, final long advice, TapTarget target) {
        if (BuildConfig.IS_TESTING.get())
            return;
        isShowingTargetedAdvice = true;
        target.tintTarget(false);
        TapTargetView.showFor(dialog, target,
                new TapTargetView.Listener() {
                    @Override
                    public void onOuterCircleClick(TapTargetView view) {
                        view.dismiss(false);
                    }

                    @Override
                    public void onTargetDismissed(TapTargetView view, boolean userInitiated) {
                        Configuration.setAdviceState(advice);
                        isShowingTargetedAdvice = false;
                    }
                });
    }

    public static boolean needsTargetedAdvice(long advice) {
        return Configuration.getAdviceState(advice);
    }
}
