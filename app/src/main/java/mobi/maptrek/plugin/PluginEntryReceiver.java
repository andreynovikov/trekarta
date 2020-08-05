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
    private Bundle lastextras = null;
    @Override
    public void onReceive(Context context, Intent intent) {
        if (lastextras.equals(intent.getExtras())) {
            // nothing has changed; we can safely return
            return;
        }
        updateValues(intent);
        Intent pluginIntent = new Intent();
        pluginIntent.setClassName(lastextras.getString("packageName"), lastextras.getString("activityName"));
        Pair<Drawable, Intent> action = new Pair<>(null, pluginIntent);
        Pair<String, Pair<Drawable, Intent>> entry = new Pair<>(lastextras.getString("name"), action);
        EventBus.getDefault().post(entry);
    }

    private void updateValues(Intent intent) {
        lastextras = intent.getExtras();
    }
}
