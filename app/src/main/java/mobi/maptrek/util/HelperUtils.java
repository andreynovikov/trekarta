package mobi.maptrek.util;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.TextView;

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
}
