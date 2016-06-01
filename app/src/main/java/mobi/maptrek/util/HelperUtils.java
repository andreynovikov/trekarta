package mobi.maptrek.util;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.view.View;

import mobi.maptrek.R;

public class HelperUtils {
    public static void showSaveError(Context context, CoordinatorLayout coordinatorLayout, Exception e) {
        final Snackbar snackbar = Snackbar
                .make(coordinatorLayout, context.getString(R.string.msg_save_failed, e.getMessage()), Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction(R.string.action_dismiss, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                snackbar.dismiss();
            }
        });
        snackbar.show();
    }
}
