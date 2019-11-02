/*
 * Copyright 2018 Andrey Novikov
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

package mobi.maptrek.maps.online;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import androidx.annotation.NonNull;

import org.oscim.android.cache.TileCache;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class TileSourceFactory {
    @NonNull
    public static List<OnlineTileSource> fromPlugin(Context context, PackageManager packageManager, ResolveInfo provider) {
        List<OnlineTileSource> sources = new ArrayList<>();

        int id;
        String[] maps = null;
        try {
            Resources resources = packageManager.getResourcesForApplication(provider.activityInfo.applicationInfo);
            id = resources.getIdentifier("maps", "array", provider.activityInfo.packageName);
            if (id != 0)
                maps = resources.getStringArray(id);

            if (maps == null)
                return sources;

            File cacheDir = new File(context.getExternalCacheDir(), "online");
            boolean useCache = cacheDir.mkdir() || cacheDir.isDirectory();

            for (String map : maps) {
                String name = null;
                String uri = null;
                id = resources.getIdentifier(map + "_name", "string", provider.activityInfo.packageName);
                if (id != 0)
                    name = resources.getString(id);
                id = resources.getIdentifier(map + "_uri", "string", provider.activityInfo.packageName);
                if (id != 0)
                    uri = resources.getString(id);
                if (name == null || uri == null)
                    continue;
                OnlineTileSource.Builder builder = OnlineTileSource.builder(context);
                builder.name(name);
                builder.code(map);
                builder.uri(uri);

                id = resources.getIdentifier(map + "_license", "string", provider.activityInfo.packageName);
                if (id != 0)
                    builder.license(resources.getString(id));
                id = resources.getIdentifier(map + "_threads", "integer", provider.activityInfo.packageName);
                if (id != 0)
                    builder.threads(resources.getInteger(id));
                id = resources.getIdentifier(map + "_minzoom", "integer", provider.activityInfo.packageName);
                if (id != 0)
                    builder.zoomMin(resources.getInteger(id));
                id = resources.getIdentifier(map + "_maxzoom", "integer", provider.activityInfo.packageName);
                if (id != 0)
                    builder.zoomMax(resources.getInteger(id));

                OnlineTileSource source = builder.build();
                if (useCache) {
                    TileCache cache = new TileCache(context, cacheDir.getAbsolutePath(), map);
                    source.setCache(cache);
                }
                sources.add(source);
            }
        } catch (Resources.NotFoundException | PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return sources;
    }
}
