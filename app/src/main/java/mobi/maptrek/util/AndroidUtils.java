package mobi.maptrek.util;

import android.os.Build;
import android.support.annotation.NonNull;
import static mobi.maptrek.util.StringUtils.capitalize;

public class AndroidUtils {
    /**
     * Returns device name in user-friendly format
     */
    @NonNull
    public static String getDeviceName()
    {
        String manufacturer = Build.MANUFACTURER;
        String model = Build.MODEL;
        if (model.startsWith(manufacturer))
            return capitalize(model);
        else
            return capitalize(manufacturer) + " " + model;
    }
}
