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

package mobi.maptrek.plugin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Pair;

import org.greenrobot.eventbus.EventBus;

public class PluginEntryReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        Intent pluginIntent = new Intent();
        pluginIntent.setClassName(extras.getString("packageName"), extras.getString("activityName"));
        Pair<Drawable, Intent> action = new Pair<>(null, pluginIntent);
        Pair<String, Pair<Drawable, Intent>> entry = new Pair<>(extras.getString("name"), action);
        EventBus.getDefault().post(entry);
    }
}
