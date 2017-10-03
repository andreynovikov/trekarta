package mobi.maptrek;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.Pair;

import java.util.AbstractMap;
import java.util.HashMap;

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
    }
}
