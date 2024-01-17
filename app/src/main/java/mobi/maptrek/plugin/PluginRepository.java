/*
 * Copyright 2022 Andrey Novikov
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

package mobi.maptrek.plugin;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;

import mobi.maptrek.util.ContextUtils;

public final class PluginRepository {
    private static final Logger logger = LoggerFactory.getLogger(PluginRepository.class);

    private final Context mContext;

    // Plugins
    private final AbstractMap<String, Intent> mPluginPreferences = new HashMap<>();
    private final AbstractMap<String, Pair<Drawable, Intent>> mPluginTools = new HashMap<>();

    public PluginRepository(@NonNull Context context) {
        mContext = context;
    }

    public AbstractMap<String, Pair<Drawable, Intent>> getPluginTools() {
        return mPluginTools;
    }

    public AbstractMap<String, Intent> getPluginPreferences() {
        return mPluginPreferences;
    }

    public void addPluginEntry(Pair<String, Pair<Drawable, Intent>> entry) {
        mPluginTools.put(entry.first, entry.second);
    }

    public void initializePlugins() {
        PackageManager packageManager = mContext.getPackageManager();
        List<ResolveInfo> plugins;

        // enumerate initializable plugins
        ContextUtils.sendExplicitBroadcast(mContext, "mobi.maptrek.plugins.action.INITIALIZE");

        // enumerate plugins with preferences
        plugins = packageManager.queryIntentActivities(new Intent("mobi.maptrek.plugins.preferences"), 0);
        for (ResolveInfo plugin : plugins) {
            logger.debug("Plugin preferences: {}.{}", plugin.activityInfo.packageName, plugin.activityInfo.name);
            Intent intent = new Intent();
            intent.setClassName(plugin.activityInfo.packageName, plugin.activityInfo.name);
            mPluginPreferences.put(plugin.activityInfo.loadLabel(packageManager).toString(), intent);
        }

        // enumerate plugins with views
        plugins = packageManager.queryIntentActivities(new Intent("mobi.maptrek.plugins.tool"), 0);
        for (ResolveInfo plugin : plugins) {
            logger.debug("Plugin views: {}.{}", plugin.activityInfo.packageName, plugin.activityInfo.name);
            // get menu icon
            Drawable icon = null;
            try {
                Resources resources = packageManager.getResourcesForApplication(plugin.activityInfo.applicationInfo);
                int id = resources.getIdentifier("ic_menu_tool", "drawable", plugin.activityInfo.packageName);
                if (id != 0)
                    icon = ResourcesCompat.getDrawable(resources, id, mContext.getTheme());
            } catch (Resources.NotFoundException | PackageManager.NameNotFoundException e) {
                logger.error("Failed to get plugin resources", e);
            }

            Intent intent = new Intent();
            intent.setClassName(plugin.activityInfo.packageName, plugin.activityInfo.name);
            Pair<Drawable, Intent> pair = new Pair<>(icon, intent);
            mPluginTools.put(plugin.activityInfo.loadLabel(packageManager).toString(), pair);
        }
    }
}
