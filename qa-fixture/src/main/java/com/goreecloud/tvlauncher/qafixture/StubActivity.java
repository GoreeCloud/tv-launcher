package com.goreecloud.tvlauncher.qafixture;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.Gravity;
import android.widget.TextView;

/** Disposable, permission-free fixture only for isolated Google TV emulator tests. */
public final class StubActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        TextView title = new TextView(this);
        title.setText("GoreeCloud QA fixture - press Back to return");
        title.setTextSize(26);
        title.setTextColor(Color.WHITE);
        title.setBackgroundColor(0xFF101827);
        title.setGravity(Gravity.CENTER);
        setContentView(title);
    }
}
