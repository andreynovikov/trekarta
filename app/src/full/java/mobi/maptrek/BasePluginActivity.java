package mobi.maptrek;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.Pair;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;

public class BasePluginActivity extends Activity {
    // Plugins
    private AbstractMap<String, Intent> mPluginPreferences = new HashMap<>();
    private AbstractMap<String, Pair<Drawable, Intent>> mPluginTools = new HashMap<>();

    public java.util.Map<String, Intent> getPluginsPreferences() {
        return mPluginPreferences;
    }

    public java.util.Map<String, Pair<Drawable, Intent>> getPluginsTools() {
        return mPluginTools;
    }

    public void initializePlugins() {
        PackageManager packageManager = getPackageManager();
        List<ResolveInfo> plugins;
        Intent initializationIntent = new Intent("mobi.maptrek.plugins.action.INITIALIZE");

        // enumerate initializable plugins
        plugins = packageManager.queryBroadcastReceivers(initializationIntent, 0);
        for (ResolveInfo plugin : plugins) {
            // send initialization broadcast, we send it directly instead of sending
            // one broadcast for all plugins to wake up stopped plugins:
            // http://developer.android.com/about/versions/android-3.1.html#launchcontrols
            Intent intent = new Intent();
            intent.setClassName(plugin.activityInfo.packageName, plugin.activityInfo.name);
            intent.setAction(initializationIntent.getAction());
            intent.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            sendBroadcast(intent);
        }

        // enumerate plugins with preferences
        plugins = packageManager.queryIntentActivities(new Intent("mobi.maptrek.plugins.preferences"), 0);
        for (ResolveInfo plugin : plugins) {
            Intent intent = new Intent();
            intent.setClassName(plugin.activityInfo.packageName, plugin.activityInfo.name);
            mPluginPreferences.put(plugin.activityInfo.loadLabel(packageManager).toString(), intent);
        }

        // enumerate plugins with views
        plugins = packageManager.queryIntentActivities(new Intent("mobi.maptrek.plugins.tool"), 0);
        for (ResolveInfo plugin : plugins) {
            // get menu icon
            Drawable icon = null;
            try {
                Resources resources = packageManager.getResourcesForApplication(plugin.activityInfo.applicationInfo);
                int id = resources.getIdentifier("ic_menu_tool", "drawable", plugin.activityInfo.packageName);
                if (id != 0)
                    icon = resources.getDrawable(id, getTheme());
            } catch (Resources.NotFoundException | PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }

            Intent intent = new Intent();
            intent.setClassName(plugin.activityInfo.packageName, plugin.activityInfo.name);
            Pair<Drawable, Intent> pair = new Pair<>(icon, intent);
            mPluginTools.put(plugin.activityInfo.loadLabel(packageManager).toString(), pair);
        }
    }

}
