package org.anchorer.pluginapk.app;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import org.anchorer.pluginapk.app.plugin.PluginManager;
import org.anchorer.pluginapk.library.TestInterface;

public class MainActivity extends AppCompatActivity {

    private TextView mMainTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMainTextView = (TextView) findViewById(R.id.main_text);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                TestInterface testPlugin = PluginManager.getInstance().createTestPluginInstance();
                mMainTextView.setText(testPlugin.getDateFromTimeStamp("yyyy-MM-dd", System.currentTimeMillis()));
            }
        }, 5000);
    }
}
