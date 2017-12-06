package mobi.maptrek.util;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.TextView;

import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;

import mobi.maptrek.Configuration;
import mobi.maptrek.R;

public class HelperUtils {
    public static void showError(String message, CoordinatorLayout coordinatorLayout) {
        final Snackbar snackbar = Snackbar
                .make(coordinatorLayout, message, Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction(R.string.actionDismiss, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                snackbar.dismiss();
            }
        });
        TextView snackbarTextView = (TextView) snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
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
                    .setAction(R.string.ok, new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Configuration.setAdviceState(advice);
                        }
                    });
            TextView snackbarTextView = (TextView) snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
            snackbarTextView.setMaxLines(99);
            snackbar.show();
        }
    }

    public static boolean showTargetedAdvice(Activity activity, final long advice, @StringRes int messageResId, View focusOn, @DrawableRes int icon) {
        if (!Configuration.getAdviceState(advice))
            return false;

        TapTarget target = TapTarget.forView(focusOn, activity.getString(messageResId))
                .outerCircleColor(R.color.explanationBackground)
                .targetCircleColor(android.R.color.white)
                .textColor(android.R.color.white)
                .icon(activity.getDrawable(icon));
        showTargetedAdvice(activity, advice, target);
        return true;
    }

    public static boolean showTargetedAdvice(Activity activity, final long advice, @StringRes int messageResId, View focusOn, boolean transparent) {
        if (!Configuration.getAdviceState(advice))
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
                .targetCircleColor(android.R.color.white)
                .textColor(android.R.color.white);

        showTargetedAdvice(activity, advice, target);
        return true;
    }

    public static boolean showTargetedAdvice(Activity activity, long advice, @StringRes int messageResId, Rect rect) {
        if (!Configuration.getAdviceState(advice))
            return false;

        TapTarget target = TapTarget.forBounds(rect, activity.getString(messageResId))
                .outerCircleColor(R.color.explanationBackground)
                .targetCircleColor(android.R.color.white)
                .textColor(android.R.color.white)
                .transparentTarget(true);
        showTargetedAdvice(activity, advice, target);
        return true;
    }

    public static boolean showTargetedAdvice(Dialog dialog, long advice, @StringRes int messageResId, Rect rect) {
        if (!Configuration.getAdviceState(advice))
            return false;

        TapTarget target = TapTarget.forBounds(rect, dialog.getContext()
                .getString(messageResId))
                .outerCircleColor(R.color.explanationBackground)
                .targetCircleColor(android.R.color.white)
                .textColor(android.R.color.white)
                .transparentTarget(true);
        showTargetedAdvice(dialog, advice, target);
        return true;
    }

    private static void showTargetedAdvice(Activity activity, final long advice, TapTarget target) {
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
                    }
                });
    }

    private static void showTargetedAdvice(Dialog dialog, final long advice, TapTarget target) {
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
                    }
                });
    }

    public static boolean needsTargetedAdvice(long advice) {
        return Configuration.getAdviceState(advice);
    }
}
