package org.anchorer.pluginapk.app;

import android.app.Application;
import android.content.Context;

import org.anchorer.pluginapk.app.plugin.PluginManager;

/**
 * Application for this example project.
 * Created by Anchorer on 16/7/31.
 */
public class TestApplication extends Application {
    private static TestApplication mInstance;

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        mInstance = this;
        PluginManager.getInstance().loadPluginApk();
    }

    /**
     * 获取Application的实例
     */
    public static TestApplication getInstance() {
        return mInstance;
    }
}
