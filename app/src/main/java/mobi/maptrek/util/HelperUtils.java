package mobi.maptrek.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.StringRes;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.util.Log;
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
                    .make(coordinatorLayout, messageResId, Snackbar.LENGTH_INDEFINITE)
                    .setAction(R.string.actionGotIt, new View.OnClickListener() {
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

    public static boolean showTargetedAdvice(Activity activity, final long advice, @StringRes int messageResId, View focusOn, boolean transparent) {
        if (!Configuration.getAdviceState(advice))
            return false;

        TapTarget target;
        if (transparent) {
            Rect r = new Rect();
            focusOn.getGlobalVisibleRect(r);
            Log.e("HU", "R: " + r.toString());
            target = TapTarget.forBounds(r, activity.getString(messageResId))
                    .transparentTarget(true);
        } else {
            target = TapTarget.forView(focusOn, activity.getString(messageResId));
        }
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
        return true;
    }
}
