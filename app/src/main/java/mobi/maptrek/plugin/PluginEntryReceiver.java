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
