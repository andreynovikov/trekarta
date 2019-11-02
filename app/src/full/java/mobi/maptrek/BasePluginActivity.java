package mobi.maptrek;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Pair;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;

public abstract class BasePluginActivity extends AppCompatActivity {
    // Plugins
    private AbstractMap<String, Intent> mPluginPreferences = new HashMap<>();
    private AbstractMap<String, Pair<Drawable, Intent>> mPluginTools = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    public java.util.Map<String, Intent> getPluginsPreferences() {
        return mPluginPreferences;
    }

    public java.util.Map<String, Pair<Drawable, Intent>> getPluginsTools() {
        return mPluginTools;
    }

    public void initializePlugins() {
        PackageManager packageManager = getPackageManager();
        List<ResolveInfo> plugins;

        // enumerate initializable plugins
        sendExplicitBroadcast("mobi.maptrek.plugins.action.INITIALIZE");

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

    protected void sendExplicitBroadcast(String action) {
        PackageManager packageManager = getPackageManager();
        Intent intent = new Intent(action);
        List<ResolveInfo> plugins = packageManager.queryBroadcastReceivers(intent, 0);
        for (ResolveInfo plugin : plugins) {
            intent = new Intent(action);
            intent.setClassName(plugin.activityInfo.packageName, plugin.activityInfo.name);
            intent.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
            sendBroadcast(intent);
        }
    }

    @Subscribe
    public void onNewPluginEntry(Pair<String, Pair<Drawable, Intent>> entry) {
        mPluginTools.put(entry.first, entry.second);
    }
}
