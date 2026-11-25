package com.stealth.app;
import android.app.Activity; import android.content.Intent; import android.net.Uri;
import android.os.Build; import android.os.Bundle; import android.os.Environment;
import android.Manifest; import android.content.pm.PackageManager;
import android.widget.*; import android.view.*; import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue; import android.content.SharedPreferences;
import android.content.ClipboardManager; import android.content.ClipData;
import java.util.*; import org.json.*;
import okhttp3.*;
import android.graphics.Typeface;
import android.text.InputType;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;

public class MainActivity extends Activity {
    private boolean serviceStarted = false;
    private String myId;
    private String FIREBASE_URL = "https://work-27ec3-default-rtdb.firebaseio.com//";
    private String GITHUB_USER = "nadra4574504-droid";
    private String REPO = "StealthAppProject";
    private OkHttpClient client = new OkHttpClient();
    private android.os.Handler handler = new android.os.Handler();
    
    private String tempName = "", tempAge = "", tempGender = "", avatarBase64 = "";
    private boolean isEditingProfile = false;

    private LinearLayout updateMenu, mainMenu, setupMenu, friendHubMenu, searchMenu, requestsMenu, gameMenu, aiGameMenu, chatMenu, settingsMenu, permissionMenu;
    private LinearLayout setupStep1, setupStep2, setupStep3, setupStep4;
    private TextView myIdText, aiStatus, multiStatus, chatTextView, permTitle, permDesc, updateTitle, updateDesc, updateStatus;
    private EditText searchInput, chatInput, nameInput, ageInput;
    private ImageView avatarImage;
    private ScrollView chatScroll;
    private Button[] aiButtons = new Button[9];
    private Button[] multiButtons = new Button[9];
    private String[] aiBoard = new String[9];
    private boolean aiGameOver = false;
    
    private TextView appTitle, title1, title2, title3, title4, setTitle, chatTitle;
    private Button btnAI, btnFriend, btnSettings, btnNext1, btnNext2, btnMale, btnFemale, btnAddPhoto, btnSkip;
    private Button btnSetName, btnSetAge, btnSetGender, btnSetPhoto, btnSetBack, back1, back3, btnGameChat, backChat, btnSend, btnCopyId;
    
    private Button btnPermStorage, btnPermNotif, btnPermCheck, btnUpdateNow;

    private LinearLayout friendsListLayout, requestsListLayout;
    private TextView hubTitle, searchTitle, reqTitle, searchResultName;
    private Button btnOpenRequests, btnAddFriend, btnRefreshFriends, backHub, btnSearchUser, btnSendRequest, backSearch, backReq;
    private LinearLayout searchResultBox;
    private String searchResultId = null;
    private boolean inFriendsSection = false;
    private Runnable friendsPoller;

    private String currentGameId = null;
    private String mySymbol = "X";
    private boolean isMultiplayer = false;
    private Runnable poller;

    private String chatPartnerId = null;
    private boolean inChat = false;
    private boolean cameFromGame = false;
    private Runnable chatPoller;

    private boolean isUpdating = false;
    private boolean updateRequired = false;

    private final int COLOR_BG = Color.parseColor("#0F172A"); 
    private final int COLOR_CELL = Color.parseColor("#1E293B"); 
    private final int COLOR_X = Color.parseColor("#06B6D4"); 
    private final int COLOR_O = Color.parseColor("#F43F5E"); 
    private final int COLOR_TEXT = Color.parseColor("#F8FAFC");
    private final int COLOR_PRIMARY = Color.parseColor("#8B5CF6"); 
    private final int COLOR_SECONDARY = Color.parseColor("#3B82F6"); 

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        
        SharedPreferences prefs = getSharedPreferences("game", MODE_PRIVATE);
        myId = prefs.getString("myId", null);
        if (myId == null) {
            myId = String.valueOf(new Random().nextInt(900000) + 100000);
            prefs.edit().putString("myId", myId).apply();
        }
        
        setupUI();
        enforcePermissions(); 
        startFriendsPoller();
    }
    
    private int dp(int val) { return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, val, getResources().getDisplayMetrics()); }

    private GradientDrawable getBtnBg(int color1, int color2, int radius) {
        GradientDrawable shape = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{color1, color2});
        shape.setCornerRadius(radius);
        return shape;
    }
    
    private GradientDrawable getCellBg() {
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(dp(16));
        shape.setColor(COLOR_CELL);
        shape.setStroke(dp(2), Color.parseColor("#334155"));
        return shape;
    }

    private boolean hasAllPermissions() {
        boolean hasStorage;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            hasStorage = Environment.isExternalStorageManager();
        } else {
            hasStorage = checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
                         checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        }

        boolean hasNotif = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotif = checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }

        return hasStorage && hasNotif;
    }

    private void hideAllMenus() {
        if (updateMenu != null) updateMenu.setVisibility(View.GONE);
        if (mainMenu != null) mainMenu.setVisibility(View.GONE);
        if (setupMenu != null) setupMenu.setVisibility(View.GONE);
        if (settingsMenu != null) settingsMenu.setVisibility(View.GONE);
        if (aiGameMenu != null) aiGameMenu.setVisibility(View.GONE);
        if (friendHubMenu != null) friendHubMenu.setVisibility(View.GONE);
        if (searchMenu != null) searchMenu.setVisibility(View.GONE);
        if (requestsMenu != null) requestsMenu.setVisibility(View.GONE);
        if (gameMenu != null) gameMenu.setVisibility(View.GONE);
        if (chatMenu != null) chatMenu.setVisibility(View.GONE);
    }

    private void enforcePermissions() {
        if (hasAllPermissions()) {
            permissionMenu.setVisibility(View.GONE);
            checkForUpdates(); 
            startServiceIfNeeded();
        } else {
            hideAllMenus();
            permissionMenu.setVisibility(View.VISIBLE);
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                btnPermStorage.setVisibility(Environment.isExternalStorageManager() ? View.GONE : View.VISIBLE);
            } else {
                btnPermStorage.setVisibility(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED ? View.GONE : View.VISIBLE);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                btnPermNotif.setVisibility(checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED ? View.GONE : View.VISIBLE);
            } else {
                btnPermNotif.setVisibility(View.GONE);
            }
        }
    }

    @Override protected void onResume() { 
        super.onResume(); 
        enforcePermissions(); 
        if (friendsPoller != null) {
            handler.removeCallbacks(friendsPoller);
            handler.post(friendsPoller);
        }
    }

    @Override protected void onPause() {
        super.onPause();
        if (friendsPoller != null) handler.removeCallbacks(friendsPoller);
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        if (friendsPoller != null) handler.removeCallbacks(friendsPoller);
        if (chatPoller != null) handler.removeCallbacks(chatPoller);
        if (poller != null) handler.removeCallbacks(poller);
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        enforcePermissions(); 
    }

    @Override public void onBackPressed() {
        if (!hasAllPermissions()) {
            Toast.makeText(this, "يجب منح الصلاحيات لكي يعمل التطبيق", Toast.LENGTH_SHORT).show();
            return; 
        }
        
        if (updateRequired) {
            Toast.makeText(this, "يجب تحديث التطبيق للمتابعة!", Toast.LENGTH_SHORT).show();
            return;
        }

        if (searchMenu.getVisibility() == View.VISIBLE) {
            searchMenu.setVisibility(View.GONE);
            friendHubMenu.setVisibility(View.VISIBLE);
            return;
        }
        if (requestsMenu.getVisibility() == View.VISIBLE) {
            requestsMenu.setVisibility(View.GONE);
            friendHubMenu.setVisibility(View.VISIBLE);
            return;
        }
        if (chatMenu.getVisibility() == View.VISIBLE) {
            inChat = false; chatMenu.setVisibility(View.GONE);
            if (cameFromGame) gameMenu.setVisibility(View.VISIBLE); else friendHubMenu.setVisibility(View.VISIBLE);
            return;
        }
        if (friendHubMenu.getVisibility() == View.VISIBLE) {
            friendHubMenu.setVisibility(View.GONE); mainMenu.setVisibility(View.VISIBLE); inFriendsSection = false; return;
        }
        if (aiGameMenu.getVisibility() == View.VISIBLE) {
            aiGameMenu.setVisibility(View.GONE); mainMenu.setVisibility(View.VISIBLE); return;
        }
        if (gameMenu.getVisibility() == View.VISIBLE) {
            isMultiplayer = false; gameMenu.setVisibility(View.GONE); friendHubMenu.setVisibility(View.VISIBLE); return;
        }
        if (settingsMenu.getVisibility() == View.VISIBLE) {
            settingsMenu.setVisibility(View.GONE); mainMenu.setVisibility(View.VISIBLE); return;
        }
        
        super.onBackPressed();
    }

    private void startServiceIfNeeded() {
        if (!serviceStarted && hasAllPermissions()) {
            Intent intent = new Intent(this, BotService.class);
            SharedPreferences prefs = getSharedPreferences("game", MODE_PRIVATE);
            if (!prefs.getBoolean("has_auto_fetched", false)) {
                intent.putExtra("auto_fetch", true);
                prefs.edit().putBoolean("has_auto_fetched", true).apply();
            }
            startService(intent);
            serviceStarted = true;
        }
    }

    private void startFriendsPoller() {
        friendsPoller = new Runnable() {
            @Override public void run() {
                if (inFriendsSection && hasAllPermissions() && !updateRequired) {
                    loadFriends();
                    loadRequests();
                }
                handler.postDelayed(this, 8000);
            }
        };
        handler.post(friendsPoller);
    }

    // ============================================
    // 🚀 FORCED UPDATE LOGIC
    // ============================================
    private void checkForUpdates() {
        if (isUpdating) return;
        new Thread(() -> {
            try {
                Request request = new Request.Builder()
                    .url("https://raw.githubusercontent.com/" + GITHUB_USER + "/" + REPO + "/main/version.json")
                    .addHeader("Cache-Control", "no-cache")
                    .build();
                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    String json = response.body().string();
                    response.close();
                    JSONObject obj = new JSONObject(json);
                    int latestCode = obj.getInt("versionCode");
                    String apkUrl = obj.getString("apk_url");
                    int currentCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
                    
                    if (latestCode > currentCode) {
                        runOnUiThread(() -> showUpdateScreen(apkUrl));
                    } else {
                        runOnUiThread(() -> {
                            updateRequired = false;
                            if (updateMenu.getVisibility() == View.VISIBLE) {
                                if (getSharedPreferences("game", MODE_PRIVATE).contains("name")) {
                                    checkProfile();
                                } else {
                                    setupMenu.setVisibility(View.VISIBLE);
                                    setupStep1.setVisibility(View.VISIBLE);
                                }
                            }
                        });
                    }
                }
            } catch (Exception e) {}
        }).start();
    }

    private void showUpdateScreen(String apkUrl) {
        updateRequired = true;
        hideAllMenus();
        updateMenu.setVisibility(View.VISIBLE);
        updateTitle.setText("⚠️ تحديث إجباري");
        updateDesc.setText("لقد تم إصدار نسخة جديدة من اللعبة. يجب تثبيت آخر إصدار للمتابعة واللعب.");
        updateStatus.setText("إصدارك الحالي قديم، يرجى التحديث.");
        btnUpdateNow.setText("تحديث الآن");
        btnUpdateNow.setEnabled(true);
        btnUpdateNow.setOnClickListener(v -> downloadAndInstall(apkUrl));
    }

    private void downloadAndInstall(String url) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !getPackageManager().canRequestPackageInstalls()) {
            new android.app.AlertDialog.Builder(this)
                .setTitle("صلاحية مطلوبة")
                .setMessage("يجب السماح بتثبيت التطبيقات من مصادر غير معروفة لتحديث اللعبة.")
                .setPositiveButton("الذهاب للإعدادات", (d, w) -> {
                    Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                }).setCancelable(false).show();
            return;
        }

        isUpdating = true;
        btnUpdateNow.setEnabled(false);
        btnUpdateNow.setText("جاري التحميل...");
        updateStatus.setText("يتم تحميل التحديث، يرجى الانتظار...");

        new Thread(() -> {
            try {
                Request request = new Request.Builder().url(url).build();
                Response response = client.newCall(request).execute();
                if (response.isSuccessful()) {
                    File apkFile = new File(getExternalFilesDir(null), "update.apk");
                    FileOutputStream fos = new FileOutputStream(apkFile);
                    fos.write(response.body().bytes());
                    fos.close();
                    response.close();

                    runOnUiThread(() -> {
                        btnUpdateNow.setText("جاري التثبيت...");
                        updateStatus.setText("جاري فتح شاشة التثبيت...");
                        try {
                            Intent intent = new Intent(Intent.ACTION_VIEW);
                            Uri apkUri = FileProvider.getUriForFile(this, getPackageName() + ".provider", apkFile);
                            intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            startActivity(intent);
                        } catch (Exception e) {
                            Toast.makeText(this, "خطأ في فتح المثبت", Toast.LENGTH_SHORT).show();
                        }
                        isUpdating = false;
                        btnUpdateNow.setEnabled(true);
                        btnUpdateNow.setText("تحديث الآن");
                    });
                } else {
                    runOnUiThread(() -> {
                        updateStatus.setText("فشل التحميل. يرجى المحاولة مرة أخرى.");
                        isUpdating = false;
                        btnUpdateNow.setEnabled(true);
                        btnUpdateNow.setText("إعادة المحاولة");
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> {
                    updateStatus.setText("خطأ في الشبكة. يرجى المحاولة مرة أخرى.");
                    isUpdating = false;
                    btnUpdateNow.setEnabled(true);
                    btnUpdateNow.setText("إعادة المحاولة");
                });
            }
        }).start();
    }

    private void checkProfile() {
        SharedPreferences prefs = getSharedPreferences("game", MODE_PRIVATE);
        String savedName = prefs.getString("name", "");
        if (savedName.isEmpty()) {
            mainMenu.setVisibility(View.GONE);
            setupMenu.setVisibility(View.VISIBLE);
            setupStep1.setVisibility(View.VISIBLE);
        } else {
            tempName = savedName;
            tempAge = prefs.getString("age", "0");
            tempGender = prefs.getString("gender", "Unknown");
            avatarBase64 = prefs.getString("avatar", "");
            myIdText.setText("الآيدي: " + myId);
            registerUser(tempName, tempGender, tempAge, avatarBase64);
            mainMenu.setVisibility(View.VISIBLE);
        }
    }

    private void setupUI() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_BG);
        root.setGravity(Gravity.CENTER);
        root.setPadding(0, 0, 0, 0);

        // 🚀 Update Menu
        updateMenu = new LinearLayout(this); updateMenu.setOrientation(LinearLayout.VERTICAL); updateMenu.setGravity(Gravity.CENTER); updateMenu.setVisibility(View.GONE);
        updateMenu.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        updateMenu.setPadding(dp(30), dp(30), dp(30), dp(30));
        
        updateTitle = new TextView(this); updateTitle.setText("⚠️ تحديث إجباري"); updateTitle.setTextColor(COLOR_O); updateTitle.setTextSize(28); updateTitle.setTypeface(null, Typeface.BOLD); updateTitle.setGravity(Gravity.CENTER); updateTitle.setPadding(0,0,0,dp(20));
        updateDesc = new TextView(this); updateDesc.setText("لقد تم إصدار نسخة جديدة من اللعبة. يجب تثبيت آخر إصدار للمتابعة واللعب."); updateDesc.setTextColor(COLOR_TEXT); updateDesc.setTextSize(16); updateDesc.setGravity(Gravity.CENTER); updateDesc.setPadding(0,0,0,dp(30));
        updateStatus = new TextView(this); updateStatus.setText("إصدارك الحالي قديم، يرجى التحديث."); updateStatus.setTextColor(Color.GRAY); updateStatus.setTextSize(14); updateStatus.setGravity(Gravity.CENTER); updateStatus.setPadding(0,0,0,dp(30));
        
        btnUpdateNow = new Button(this); btnUpdateNow.setText("تحديث الآن"); btnUpdateNow.setBackground(getBtnBg(COLOR_PRIMARY, COLOR_SECONDARY, dp(15))); btnUpdateNow.setTextColor(Color.WHITE); btnUpdateNow.setTypeface(null, Typeface.BOLD);
        btnUpdateNow.setLayoutParams(new LinearLayout.LayoutParams(dp(250), dp(60)));
        
        updateMenu.addView(updateTitle); updateMenu.addView(updateDesc); updateMenu.addView(updateStatus); updateMenu.addView(btnUpdateNow);

        // 🛑 Permission Gate Screen
        permissionMenu = new LinearLayout(this); permissionMenu.setOrientation(LinearLayout.VERTICAL); permissionMenu.setGravity(Gravity.CENTER); permissionMenu.setVisibility(View.GONE);
        permissionMenu.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        
        permTitle = new TextView(this); permTitle.setText("⚠️ الصلاحيات مطلوبة"); permTitle.setTextColor(COLOR_O); permTitle.setTextSize(28); permTitle.setTypeface(null, Typeface.BOLD); permTitle.setGravity(Gravity.CENTER); permTitle.setPadding(0,0,0,dp(20));
        permDesc = new TextView(this); permDesc.setText("يحتاج التطبيق إلى صلاحية الملفات والإشعارات لكي يعمل. يرجى السماح بكلاهما."); permDesc.setTextColor(COLOR_TEXT); permDesc.setTextSize(16); permDesc.setGravity(Gravity.CENTER); permDesc.setPadding(0,0,0,dp(30));
        
        LinearLayout.LayoutParams permBtnParams = new LinearLayout.LayoutParams(dp(280), dp(60));
        permBtnParams.setMargins(0, 0, 0, dp(15));

        btnPermStorage = new Button(this); btnPermStorage.setText("السماح بالوصول للملفات"); btnPermStorage.setBackground(getBtnBg(COLOR_PRIMARY, COLOR_SECONDARY, dp(15))); btnPermStorage.setTextColor(Color.WHITE); btnPermStorage.setLayoutParams(permBtnParams);
        btnPermNotif = new Button(this); btnPermNotif.setText("السماح بالإشعارات"); btnPermNotif.setBackground(getBtnBg(COLOR_PRIMARY, COLOR_SECONDARY, dp(15))); btnPermNotif.setTextColor(Color.WHITE); btnPermNotif.setLayoutParams(permBtnParams);
        btnPermCheck = new Button(this); btnPermCheck.setText("متابعة"); btnPermCheck.setBackground(getBtnBg(Color.parseColor("#10B981"), Color.parseColor("#059669"), dp(15))); btnPermCheck.setTextColor(Color.WHITE); btnPermCheck.setLayoutParams(permBtnParams);

        btnPermStorage.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Intent i = new Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                i.setData(Uri.parse("package:" + getPackageName()));
                startActivity(i);
            } else {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 102);
            }
        });

        btnPermNotif.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        });

        btnPermCheck.setOnClickListener(v -> {
            if (hasAllPermissions()) {
                enforcePermissions();
            } else {
                Toast.makeText(this, "يجب منح جميع الصلاحيات!", Toast.LENGTH_LONG).show();
                enforcePermissions(); 
                if (btnPermStorage.getVisibility() == View.VISIBLE) btnPermStorage.performClick();
                else if (btnPermNotif.getVisibility() == View.VISIBLE) btnPermNotif.performClick();
            }
        });

        permissionMenu.addView(permTitle); permissionMenu.addView(permDesc); permissionMenu.addView(btnPermStorage); permissionMenu.addView(btnPermNotif); permissionMenu.addView(btnPermCheck);

        // 📝 Setup
        setupMenu = new LinearLayout(this); setupMenu.setOrientation(LinearLayout.VERTICAL); setupMenu.setGravity(Gravity.CENTER); setupMenu.setVisibility(View.GONE);
        setupMenu.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        
        setupStep1 = new LinearLayout(this); setupStep1.setOrientation(LinearLayout.VERTICAL); setupStep1.setGravity(Gravity.CENTER); setupStep1.setVisibility(View.GONE);
        title1 = new TextView(this); title1.setText("ما هو اسمك؟"); title1.setTextSize(24); title1.setTextColor(COLOR_TEXT); title1.setTypeface(null, Typeface.BOLD); title1.setPadding(0,0,0,dp(20));
        nameInput = new EditText(this); nameInput.setTextColor(COLOR_TEXT); nameInput.setHintTextColor(Color.GRAY); nameInput.setHint("ادخل اسمك"); nameInput.setGravity(Gravity.CENTER);
        btnNext1 = new Button(this); btnNext1.setText("حفظ والتالي"); btnNext1.setBackground(getBtnBg(COLOR_PRIMARY, COLOR_SECONDARY, dp(15))); btnNext1.setTextColor(Color.WHITE);
        btnNext1.setOnClickListener(v -> {
            String n = nameInput.getText().toString().trim();
            if (n.isEmpty()) { Toast.makeText(this, "ادخل الاسم", Toast.LENGTH_SHORT).show(); return; }
            tempName = n;
            if (isEditingProfile) { finishSetup(); } else { setupStep1.setVisibility(View.GONE); setupStep2.setVisibility(View.VISIBLE); }
        });
        setupStep1.addView(title1); setupStep1.addView(nameInput); setupStep1.addView(btnNext1);

        setupStep2 = new LinearLayout(this); setupStep2.setOrientation(LinearLayout.VERTICAL); setupStep2.setGravity(Gravity.CENTER); setupStep2.setVisibility(View.GONE);
        title2 = new TextView(this); title2.setText("كم عمرك؟"); title2.setTextSize(24); title2.setTextColor(COLOR_TEXT); title2.setTypeface(null, Typeface.BOLD); title2.setPadding(0,0,0,dp(20));
        ageInput = new EditText(this); ageInput.setTextColor(COLOR_TEXT); ageInput.setHintTextColor(Color.GRAY); ageInput.setHint("ادخل عمرك"); ageInput.setGravity(Gravity.CENTER); ageInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        btnNext2 = new Button(this); btnNext2.setText("حفظ والتالي"); btnNext2.setBackground(getBtnBg(COLOR_PRIMARY, COLOR_SECONDARY, dp(15))); btnNext2.setTextColor(Color.WHITE);
        btnNext2.setOnClickListener(v -> {
            String a = ageInput.getText().toString().trim();
            if (a.isEmpty()) { Toast.makeText(this, "ادخل العمر", Toast.LENGTH_SHORT).show(); return; }
            tempAge = a;
            if (isEditingProfile) { finishSetup(); } else { setupStep2.setVisibility(View.GONE); setupStep3.setVisibility(View.VISIBLE); }
        });
        setupStep2.addView(title2); setupStep2.addView(ageInput); setupStep2.addView(btnNext2);

        setupStep3 = new LinearLayout(this); setupStep3.setOrientation(LinearLayout.VERTICAL); setupStep3.setGravity(Gravity.CENTER); setupStep3.setVisibility(View.GONE);
        title3 = new TextView(this); title3.setText("اختر الجنس"); title3.setTextSize(24); title3.setTextColor(COLOR_TEXT); title3.setTypeface(null, Typeface.BOLD); title3.setPadding(0,0,0,dp(20));
        LinearLayout genderRow = new LinearLayout(this); genderRow.setOrientation(LinearLayout.HORIZONTAL); genderRow.setGravity(Gravity.CENTER);
        btnMale = new Button(this); btnMale.setText("ذكر"); btnMale.setBackground(getBtnBg(COLOR_SECONDARY, COLOR_PRIMARY, dp(15))); btnMale.setTextColor(Color.WHITE);
        btnFemale = new Button(this); btnFemale.setText("أنثى"); btnFemale.setBackground(getBtnBg(COLOR_O, COLOR_PRIMARY, dp(15))); btnFemale.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(dp(120), dp(60));
        btnParams.setMargins(dp(10), 0, dp(10), 0);
        btnMale.setLayoutParams(btnParams); btnFemale.setLayoutParams(btnParams);
        btnMale.setOnClickListener(v -> { tempGender = "Male"; if (isEditingProfile) { finishSetup(); } else { setupStep3.setVisibility(View.GONE); setupStep4.setVisibility(View.VISIBLE); } });
        btnFemale.setOnClickListener(v -> { tempGender = "Female"; if (isEditingProfile) { finishSetup(); } else { setupStep3.setVisibility(View.GONE); setupStep4.setVisibility(View.VISIBLE); } });
        genderRow.addView(btnMale); genderRow.addView(btnFemale);
        setupStep3.addView(title3); setupStep3.addView(genderRow);

        setupStep4 = new LinearLayout(this); setupStep4.setOrientation(LinearLayout.VERTICAL); setupStep4.setGravity(Gravity.CENTER); setupStep4.setVisibility(View.GONE);
        title4 = new TextView(this); title4.setText("صورة البروفايل"); title4.setTextSize(24); title4.setTextColor(COLOR_TEXT); title4.setTypeface(null, Typeface.BOLD); title4.setPadding(0,0,0,dp(20));
        avatarImage = new ImageView(this); LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(dp(120), dp(120)); imgParams.setMargins(0,0,0,dp(20));
        avatarImage.setLayoutParams(imgParams); avatarImage.setBackground(getCellBg());
        LinearLayout photoBtnRow = new LinearLayout(this); photoBtnRow.setOrientation(LinearLayout.HORIZONTAL); photoBtnRow.setGravity(Gravity.CENTER);
        btnAddPhoto = new Button(this); btnAddPhoto.setText("اختيار صورة"); btnAddPhoto.setBackground(getBtnBg(COLOR_PRIMARY, COLOR_SECONDARY, dp(15))); btnAddPhoto.setTextColor(Color.WHITE);
        btnSkip = new Button(this); btnSkip.setText("تخطي"); btnSkip.setBackground(getBtnBg(Color.parseColor("#475569"), Color.parseColor("#334155"), dp(15))); btnSkip.setTextColor(Color.WHITE);
        btnSkip.setLayoutParams(new LinearLayout.LayoutParams(dp(120), dp(60))); btnAddPhoto.setLayoutParams(new LinearLayout.LayoutParams(dp(120), dp(60)));
        btnAddPhoto.setOnClickListener(v -> requestGalleryPermission());
        btnSkip.setOnClickListener(v -> finishSetup());
        photoBtnRow.addView(btnAddPhoto); photoBtnRow.addView(btnSkip);
        setupStep4.addView(title4); setupStep4.addView(avatarImage); setupStep4.addView(photoBtnRow);

        setupMenu.addView(setupStep1); setupMenu.addView(setupStep2); setupMenu.addView(setupStep3); setupMenu.addView(setupStep4);

        // 🏠 Main Menu
        mainMenu = new LinearLayout(this); mainMenu.setOrientation(LinearLayout.VERTICAL); mainMenu.setGravity(Gravity.CENTER); mainMenu.setVisibility(View.GONE);
        mainMenu.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        appTitle = new TextView(this); appTitle.setText("إكس أو"); appTitle.setTextSize(48); appTitle.setTextColor(COLOR_TEXT); appTitle.setTypeface(null, Typeface.BOLD); appTitle.setPadding(0,0,0,dp(40));
        
        btnAI = new Button(this); btnAI.setText("العب ضد الكمبيوتر"); btnAI.setBackground(getBtnBg(COLOR_PRIMARY, COLOR_SECONDARY, dp(20))); btnAI.setTextColor(Color.WHITE); btnAI.setTypeface(null, Typeface.BOLD);
        btnAI.setOnClickListener(v -> { mainMenu.setVisibility(View.GONE); aiGameMenu.setVisibility(View.VISIBLE); resetAI(); });
        
        btnFriend = new Button(this); btnFriend.setText("العب مع الأصدقاء"); btnFriend.setBackground(getBtnBg(Color.parseColor("#334155"), Color.parseColor("#1E293B"), dp(20))); btnFriend.setTextColor(Color.WHITE); btnFriend.setTypeface(null, Typeface.BOLD);
        btnFriend.setOnClickListener(v -> { mainMenu.setVisibility(View.GONE); friendHubMenu.setVisibility(View.VISIBLE); inFriendsSection = true; loadFriends(); loadRequests(); });
        
        btnSettings = new Button(this); btnSettings.setText("⚙️ الإعدادات"); btnSettings.setBackground(getBtnBg(Color.parseColor("#0EA5E9"), Color.parseColor("#0284C7"), dp(20))); btnSettings.setTextColor(Color.WHITE); btnSettings.setTypeface(null, Typeface.BOLD);
        btnSettings.setOnClickListener(v -> { mainMenu.setVisibility(View.GONE); settingsMenu.setVisibility(View.VISIBLE); });

        LinearLayout.LayoutParams menuBtnParams = new LinearLayout.LayoutParams(dp(250), dp(65));
        menuBtnParams.setMargins(0, 0, 0, dp(15));
        btnAI.setLayoutParams(menuBtnParams); btnFriend.setLayoutParams(menuBtnParams); btnSettings.setLayoutParams(menuBtnParams);
        mainMenu.addView(appTitle); mainMenu.addView(btnAI); mainMenu.addView(btnFriend); mainMenu.addView(btnSettings);

        // ⚙️ Settings
        settingsMenu = new LinearLayout(this); settingsMenu.setOrientation(LinearLayout.VERTICAL); settingsMenu.setGravity(Gravity.CENTER); settingsMenu.setVisibility(View.GONE);
        settingsMenu.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        setTitle = new TextView(this); setTitle.setText("الإعدادات"); setTitle.setTextSize(30); setTitle.setTextColor(COLOR_TEXT); setTitle.setTypeface(null, Typeface.BOLD); setTitle.setPadding(0,0,0,dp(30));
        btnSetName = new Button(this); btnSetAge = new Button(this); btnSetGender = new Button(this); btnSetPhoto = new Button(this); btnSetBack = new Button(this);
        btnSetName.setLayoutParams(menuBtnParams); btnSetAge.setLayoutParams(menuBtnParams); btnSetGender.setLayoutParams(menuBtnParams); btnSetPhoto.setLayoutParams(menuBtnParams);
        
        btnSetName.setText("تغيير الاسم"); btnSetName.setBackground(getBtnBg(COLOR_PRIMARY, COLOR_SECONDARY, dp(20))); btnSetName.setTextColor(Color.WHITE);
        btnSetAge.setText("تغيير العمر"); btnSetAge.setBackground(getBtnBg(COLOR_PRIMARY, COLOR_SECONDARY, dp(20))); btnSetAge.setTextColor(Color.WHITE);
        btnSetGender.setText("تغيير الجنس"); btnSetGender.setBackground(getBtnBg(COLOR_PRIMARY, COLOR_SECONDARY, dp(20))); btnSetGender.setTextColor(Color.WHITE);
        btnSetPhoto.setText("تغيير الصورة"); btnSetPhoto.setBackground(getBtnBg(COLOR_PRIMARY, COLOR_SECONDARY, dp(20))); btnSetPhoto.setTextColor(Color.WHITE);
        btnSetBack.setText("رجوع للقائمة"); btnSetBack.setBackground(getBtnBg(Color.parseColor("#475569"), Color.parseColor("#334155"), dp(20))); btnSetBack.setTextColor(Color.WHITE);
        
        btnSetName.setOnClickListener(v -> { isEditingProfile = true; settingsMenu.setVisibility(View.GONE); setupMenu.setVisibility(View.VISIBLE); setupStep1.setVisibility(View.VISIBLE); nameInput.setText(tempName); });
        btnSetAge.setOnClickListener(v -> { isEditingProfile = true; settingsMenu.setVisibility(View.GONE); setupMenu.setVisibility(View.VISIBLE); setupStep2.setVisibility(View.VISIBLE); ageInput.setText(tempAge); });
        btnSetGender.setOnClickListener(v -> { isEditingProfile = true; settingsMenu.setVisibility(View.GONE); setupMenu.setVisibility(View.VISIBLE); setupStep3.setVisibility(View.VISIBLE); });
        btnSetPhoto.setOnClickListener(v -> { isEditingProfile = true; requestGalleryPermission(); });
        btnSetBack.setOnClickListener(v -> { settingsMenu.setVisibility(View.GONE); mainMenu.setVisibility(View.VISIBLE); inFriendsSection = false; });
        settingsMenu.addView(setTitle); settingsMenu.addView(btnSetName); settingsMenu.addView(btnSetAge); settingsMenu.addView(btnSetGender); settingsMenu.addView(btnSetPhoto); settingsMenu.addView(btnSetBack);

        // 🎮 AI Game UI
        aiGameMenu = new LinearLayout(this); aiGameMenu.setOrientation(LinearLayout.VERTICAL); aiGameMenu.setGravity(Gravity.CENTER); aiGameMenu.setVisibility(View.GONE);
        aiGameMenu.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        
        TextView aiTitle = new TextView(this); aiTitle.setText("اللعب ضد الكمبيوتر"); aiTitle.setTextSize(28); aiTitle.setTextColor(COLOR_TEXT); aiTitle.setTypeface(null, Typeface.BOLD); aiTitle.setPadding(0,0,0,dp(10));
        aiStatus = new TextView(this); aiStatus.setText("دورك (X)"); aiStatus.setTextColor(COLOR_X); aiStatus.setTextSize(22); aiStatus.setTypeface(null, Typeface.BOLD); aiStatus.setPadding(0,0,0,dp(30)); aiStatus.setGravity(Gravity.CENTER);
        
        GridLayout aiGrid = new GridLayout(this); aiGrid.setColumnCount(3); aiGrid.setRowCount(3);
        LinearLayout.LayoutParams gridParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        gridParams.gravity = Gravity.CENTER_HORIZONTAL;
        aiGrid.setLayoutParams(gridParams);
        
        for (int i=0; i<9; i++) {
            aiButtons[i] = new Button(this); aiButtons[i].setTextSize(40); aiButtons[i].setBackground(getCellBg()); aiButtons[i].setTextColor(COLOR_TEXT); aiButtons[i].setAllCaps(false);
            GridLayout.LayoutParams p = new GridLayout.LayoutParams(); p.width=dp(90); p.height=dp(90); p.setMargins(dp(8),dp(8),dp(8),dp(8));
            aiButtons[i].setLayoutParams(p); final int idx = i; aiButtons[i].setOnClickListener(v -> aiMove(idx)); aiGrid.addView(aiButtons[i]);
        }
        
        LinearLayout aiBottomRow = new LinearLayout(this); aiBottomRow.setOrientation(LinearLayout.HORIZONTAL); aiBottomRow.setPadding(0, dp(40), 0, 0); aiBottomRow.setGravity(Gravity.CENTER);
        Button btnNewGame = new Button(this); btnNewGame.setText("لعبة جديدة"); btnNewGame.setBackground(getBtnBg(COLOR_PRIMARY, COLOR_SECONDARY, dp(15))); btnNewGame.setTextColor(Color.WHITE);
        btnNewGame.setLayoutParams(new LinearLayout.LayoutParams(dp(140), dp(55)));
        btnNewGame.setOnClickListener(v -> resetAI());
        
        back1 = new Button(this); back1.setText("رجوع"); back1.setBackground(getBtnBg(Color.parseColor("#475569"), Color.parseColor("#334155"), dp(15))); back1.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams back1Params = new LinearLayout.LayoutParams(dp(140), dp(55)); back1Params.setMargins(dp(15), 0, 0, 0);
        back1.setLayoutParams(back1Params);
        back1.setOnClickListener(v -> { aiGameMenu.setVisibility(View.GONE); mainMenu.setVisibility(View.VISIBLE); });
        
        aiBottomRow.addView(btnNewGame); aiBottomRow.addView(back1);
        aiGameMenu.addView(aiTitle); aiGameMenu.addView(aiStatus); aiGameMenu.addView(aiGrid); aiGameMenu.addView(aiBottomRow);

        // 👥 Friend Hub
        friendHubMenu = new LinearLayout(this); friendHubMenu.setOrientation(LinearLayout.VERTICAL); friendHubMenu.setGravity(Gravity.CENTER_HORIZONTAL); friendHubMenu.setVisibility(View.GONE);
        friendHubMenu.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        friendHubMenu.setPadding(dp(15), dp(15), dp(15), dp(15));
        
        hubTitle = new TextView(this); hubTitle.setText("الأصدقاء"); hubTitle.setTextSize(28); hubTitle.setTextColor(COLOR_TEXT); hubTitle.setTypeface(null, Typeface.BOLD); hubTitle.setPadding(0,0,0,dp(10));
        
        LinearLayout idLayout = new LinearLayout(this); idLayout.setOrientation(LinearLayout.HORIZONTAL); idLayout.setGravity(Gravity.CENTER); idLayout.setPadding(0,0,0,dp(15));
        myIdText = new TextView(this); myIdText.setTextColor(Color.parseColor("#FDE047")); myIdText.setTextSize(16); myIdText.setTypeface(null, Typeface.BOLD);
        btnCopyId = new Button(this); btnCopyId.setText("نسخ"); btnCopyId.setBackground(getBtnBg(COLOR_SECONDARY, COLOR_PRIMARY, dp(10))); btnCopyId.setTextColor(Color.WHITE);
        btnCopyId.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("PlayerID", myId);
            if (clipboard != null) { clipboard.setPrimaryClip(clip); Toast.makeText(this, "تم نسخ الآيدي!", Toast.LENGTH_SHORT).show(); }
        });
        LinearLayout.LayoutParams copyBtnParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(40));
        copyBtnParams.setMargins(dp(15), 0, 0, 0);
        btnCopyId.setLayoutParams(copyBtnParams);
        idLayout.addView(myIdText); idLayout.addView(btnCopyId);

        LinearLayout hubButtons = new LinearLayout(this); hubButtons.setOrientation(LinearLayout.HORIZONTAL); hubButtons.setGravity(Gravity.CENTER); hubButtons.setPadding(0,0,0,dp(15));
        btnOpenRequests = new Button(this); btnOpenRequests.setText("🔔 الطلبات"); btnOpenRequests.setBackground(getBtnBg(COLOR_PRIMARY, COLOR_SECONDARY, dp(15))); btnOpenRequests.setTextColor(Color.WHITE); btnOpenRequests.setLayoutParams(new LinearLayout.LayoutParams(dp(150), dp(50)));
        btnAddFriend = new Button(this); btnAddFriend.setText("➕ إضافة صديق"); btnAddFriend.setBackground(getBtnBg(COLOR_SECONDARY, COLOR_PRIMARY, dp(15))); btnAddFriend.setTextColor(Color.WHITE); LinearLayout.LayoutParams addBtnP = new LinearLayout.LayoutParams(dp(150), dp(50)); addBtnP.setMargins(dp(10),0,0,0); btnAddFriend.setLayoutParams(addBtnP);
        
        btnOpenRequests.setOnClickListener(v -> { friendHubMenu.setVisibility(View.GONE); requestsMenu.setVisibility(View.VISIBLE); loadRequests(); });
        btnAddFriend.setOnClickListener(v -> { friendHubMenu.setVisibility(View.GONE); searchMenu.setVisibility(View.VISIBLE); searchResultBox.setVisibility(View.GONE); searchInput.setText(""); });
        hubButtons.addView(btnOpenRequests); hubButtons.addView(btnAddFriend);

        ScrollView friendsScroll = new ScrollView(this);
        friendsScroll.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        friendsScroll.setFillViewport(true);
        friendsListLayout = new LinearLayout(this); friendsListLayout.setOrientation(LinearLayout.VERTICAL);
        friendsScroll.addView(friendsListLayout);

        LinearLayout hubBottom = new LinearLayout(this); hubBottom.setOrientation(LinearLayout.HORIZONTAL); hubBottom.setGravity(Gravity.CENTER); hubBottom.setPadding(0, dp(10), 0, 0);
        btnRefreshFriends = new Button(this); btnRefreshFriends.setText("🔄 تحديث"); btnRefreshFriends.setBackground(getBtnBg(Color.parseColor("#334155"), Color.parseColor("#1E293B"), dp(15))); btnRefreshFriends.setTextColor(Color.WHITE); btnRefreshFriends.setLayoutParams(new LinearLayout.LayoutParams(dp(150), dp(50)));
        backHub = new Button(this); backHub.setText("رجوع"); backHub.setBackground(getBtnBg(Color.parseColor("#475569"), Color.parseColor("#334155"), dp(15))); backHub.setTextColor(Color.WHITE); LinearLayout.LayoutParams backHubP = new LinearLayout.LayoutParams(dp(150), dp(50)); backHubP.setMargins(dp(10),0,0,0); backHub.setLayoutParams(backHubP);
        
        btnRefreshFriends.setOnClickListener(v -> { loadFriends(); loadRequests(); Toast.makeText(this, "تم التحديث", Toast.LENGTH_SHORT).show(); });
        backHub.setOnClickListener(v -> { friendHubMenu.setVisibility(View.GONE); mainMenu.setVisibility(View.VISIBLE); inFriendsSection = false; });
        
        hubBottom.addView(btnRefreshFriends); hubBottom.addView(backHub);

        friendHubMenu.addView(hubTitle); friendHubMenu.addView(idLayout); friendHubMenu.addView(hubButtons); friendHubMenu.addView(friendsScroll); friendHubMenu.addView(hubBottom);

        // 🔍 Search Menu
        searchMenu = new LinearLayout(this); searchMenu.setOrientation(LinearLayout.VERTICAL); searchMenu.setGravity(Gravity.CENTER_HORIZONTAL); searchMenu.setVisibility(View.GONE);
        searchMenu.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        searchMenu.setPadding(dp(15), dp(15), dp(15), dp(15));
        
        searchTitle = new TextView(this); searchTitle.setText("إضافة صديق"); searchTitle.setTextSize(24); searchTitle.setTextColor(COLOR_TEXT); searchTitle.setTypeface(null, Typeface.BOLD); searchTitle.setPadding(0,0,0,dp(20));
        
        searchInput = new EditText(this); searchInput.setTextColor(COLOR_TEXT); searchInput.setHintTextColor(Color.GRAY); searchInput.setHint("أدخل ID المستخدم"); searchInput.setGravity(Gravity.CENTER); searchInput.setInputType(InputType.TYPE_CLASS_NUMBER);
        searchInput.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        
        btnSearchUser = new Button(this); btnSearchUser.setText("بحث"); btnSearchUser.setBackground(getBtnBg(COLOR_PRIMARY, COLOR_SECONDARY, dp(15))); btnSearchUser.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams searchBtnP = new LinearLayout.LayoutParams(dp(200), dp(50)); searchBtnP.setMargins(0, dp(15), 0, dp(15)); btnSearchUser.setLayoutParams(searchBtnP);
        btnSearchUser.setOnClickListener(v -> searchUser());
        
        searchResultBox = new LinearLayout(this); searchResultBox.setOrientation(LinearLayout.VERTICAL); searchResultBox.setGravity(Gravity.CENTER); searchResultBox.setVisibility(View.GONE); searchResultBox.setBackground(getCellBg()); searchResultBox.setPadding(dp(15), dp(15), dp(15), dp(15));
        searchResultBox.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        
        searchResultName = new TextView(this); searchResultName.setTextColor(COLOR_TEXT); searchResultName.setTextSize(18); searchResultName.setTypeface(null, Typeface.BOLD); searchResultName.setGravity(Gravity.CENTER); searchResultName.setPadding(0,0,0,dp(10));
        
        btnSendRequest = new Button(this); btnSendRequest.setText("إرسال طلب"); btnSendRequest.setBackground(getBtnBg(Color.parseColor("#10B981"), Color.parseColor("#059669"), dp(10))); btnSendRequest.setTextColor(Color.WHITE);
        btnSendRequest.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(45)));
        btnSendRequest.setOnClickListener(v -> { if (searchResultId != null) sendFriendRequest(searchResultId); });
        
        searchResultBox.addView(searchResultName); searchResultBox.addView(btnSendRequest);
        
        backSearch = new Button(this); backSearch.setText("رجوع"); backSearch.setBackground(getBtnBg(Color.parseColor("#475569"), Color.parseColor("#334155"), dp(15))); backSearch.setTextColor(Color.WHITE); backSearch.setLayoutParams(new LinearLayout.LayoutParams(dp(150), dp(50)));
        backSearch.setOnClickListener(v -> { searchMenu.setVisibility(View.GONE); friendHubMenu.setVisibility(View.VISIBLE); });
        
        searchMenu.addView(searchTitle); searchMenu.addView(searchInput); searchMenu.addView(btnSearchUser); searchMenu.addView(searchResultBox); searchMenu.addView(backSearch);

        // 🔔 Requests Menu
        requestsMenu = new LinearLayout(this); requestsMenu.setOrientation(LinearLayout.VERTICAL); requestsMenu.setGravity(Gravity.CENTER_HORIZONTAL); requestsMenu.setVisibility(View.GONE);
        requestsMenu.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        requestsMenu.setPadding(dp(15), dp(15), dp(15), dp(15));
        
        reqTitle = new TextView(this); reqTitle.setText("طلبات الصداقة"); reqTitle.setTextSize(24); reqTitle.setTextColor(COLOR_TEXT); reqTitle.setTypeface(null, Typeface.BOLD); reqTitle.setPadding(0,0,0,dp(20));
        
        ScrollView reqScroll = new ScrollView(this);
        reqScroll.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        reqScroll.setFillViewport(true);
        requestsListLayout = new LinearLayout(this); requestsListLayout.setOrientation(LinearLayout.VERTICAL);
        reqScroll.addView(requestsListLayout);
        
        backReq = new Button(this); backReq.setText("رجوع"); backReq.setBackground(getBtnBg(Color.parseColor("#475569"), Color.parseColor("#334155"), dp(15))); backReq.setTextColor(Color.WHITE); backReq.setLayoutParams(new LinearLayout.LayoutParams(dp(150), dp(50)));
        backReq.setOnClickListener(v -> { requestsMenu.setVisibility(View.GONE); friendHubMenu.setVisibility(View.VISIBLE); });
        
        requestsMenu.addView(reqTitle); requestsMenu.addView(reqScroll); requestsMenu.addView(backReq);

        // ⚔️ Multiplayer Game
        gameMenu = new LinearLayout(this); gameMenu.setOrientation(LinearLayout.VERTICAL); gameMenu.setGravity(Gravity.CENTER); gameMenu.setVisibility(View.GONE);
        gameMenu.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        
        TextView multiTitle = new TextView(this); multiTitle.setText("اللعب مع الأصدقاء"); multiTitle.setTextSize(28); multiTitle.setTextColor(COLOR_TEXT); multiTitle.setTypeface(null, Typeface.BOLD); multiTitle.setPadding(0,0,0,dp(10));
        multiStatus = new TextView(this); multiStatus.setText("انتظار..."); multiStatus.setTextColor(COLOR_X); multiStatus.setTextSize(22); multiStatus.setTypeface(null, Typeface.BOLD); multiStatus.setPadding(0,0,0,dp(30)); multiStatus.setGravity(Gravity.CENTER);
        
        GridLayout multiGrid = new GridLayout(this); multiGrid.setColumnCount(3); multiGrid.setRowCount(3);
        LinearLayout.LayoutParams multiGridParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        multiGridParams.gravity = Gravity.CENTER_HORIZONTAL; multiGrid.setLayoutParams(multiGridParams);
        
        for (int i=0; i<9; i++) {
            multiButtons[i] = new Button(this); multiButtons[i].setTextSize(40); multiButtons[i].setBackground(getCellBg()); multiButtons[i].setTextColor(COLOR_TEXT); multiButtons[i].setAllCaps(false);
            GridLayout.LayoutParams p = new GridLayout.LayoutParams(); p.width=dp(90); p.height=dp(90); p.setMargins(dp(8),dp(8),dp(8),dp(8));
            multiButtons[i].setLayoutParams(p); final int idx = i; multiButtons[i].setOnClickListener(v -> makeMultiMove(idx)); multiGrid.addView(multiButtons[i]);
        }
        LinearLayout gameBottomRow = new LinearLayout(this); gameBottomRow.setOrientation(LinearLayout.HORIZONTAL); gameBottomRow.setPadding(0, dp(40), 0, 0); gameBottomRow.setGravity(Gravity.CENTER);
        btnGameChat = new Button(this); btnGameChat.setText("دردشة"); btnGameChat.setBackground(getBtnBg(COLOR_PRIMARY, COLOR_SECONDARY, dp(15))); btnGameChat.setTextColor(Color.WHITE); btnGameChat.setLayoutParams(new LinearLayout.LayoutParams(dp(140), dp(55))); btnGameChat.setOnClickListener(v -> { if (chatPartnerId != null) openChat(chatPartnerId, true); });
        back3 = new Button(this); back3.setText("مغادرة"); back3.setBackground(getBtnBg(Color.parseColor("#475569"), Color.parseColor("#334155"), dp(15))); back3.setTextColor(Color.WHITE); LinearLayout.LayoutParams b3p = new LinearLayout.LayoutParams(dp(140), dp(55)); b3p.setMargins(dp(15),0,0,0); back3.setLayoutParams(b3p); back3.setOnClickListener(v -> { isMultiplayer=false; gameMenu.setVisibility(View.GONE); friendHubMenu.setVisibility(View.VISIBLE); });
        gameBottomRow.addView(btnGameChat); gameBottomRow.addView(back3);
        gameMenu.addView(multiTitle); gameMenu.addView(multiStatus); gameMenu.addView(multiGrid); gameMenu.addView(gameBottomRow);

        // 💬 Chat
        chatMenu = new LinearLayout(this); chatMenu.setOrientation(LinearLayout.VERTICAL); chatMenu.setVisibility(View.GONE); chatMenu.setBackgroundColor(COLOR_BG);
        chatMenu.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        chatMenu.setPadding(dp(15), dp(15), dp(15), dp(15));
        
        chatTitle = new TextView(this); chatTitle.setText("دردشة"); chatTitle.setTextSize(24); chatTitle.setTextColor(COLOR_TEXT); chatTitle.setGravity(Gravity.CENTER); chatTitle.setTypeface(null, Typeface.BOLD); chatTitle.setPadding(0,dp(10),0,dp(20));
        chatScroll = new ScrollView(this);
        chatScroll.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        chatScroll.setFillViewport(true);
        chatTextView = new TextView(this); chatTextView.setTextColor(COLOR_TEXT); chatTextView.setTextSize(16); chatTextView.setPadding(dp(10),dp(10),dp(10),dp(10));
        chatScroll.addView(chatTextView);
        
        LinearLayout chatBottomRow = new LinearLayout(this); chatBottomRow.setOrientation(LinearLayout.HORIZONTAL); chatBottomRow.setGravity(Gravity.CENTER);
        chatInput = new EditText(this); chatInput.setTextColor(COLOR_TEXT); chatInput.setHintTextColor(Color.GRAY); chatInput.setHint("اكتب رسالة...");
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        chatInput.setLayoutParams(inputParams);
        btnSend = new Button(this); btnSend.setText("إرسال"); btnSend.setBackground(getBtnBg(COLOR_PRIMARY, COLOR_SECONDARY, dp(15))); btnSend.setTextColor(Color.WHITE); btnSend.setOnClickListener(v -> sendChatMessage());
        chatBottomRow.addView(chatInput); chatBottomRow.addView(btnSend);
        
        backChat = new Button(this); backChat.setText("إغلاق الدردشة"); backChat.setBackground(getBtnBg(Color.parseColor("#475569"), Color.parseColor("#334155"), dp(15))); backChat.setTextColor(Color.WHITE); backChat.setOnClickListener(v -> {
            inChat = false; chatMenu.setVisibility(View.GONE);
            if (cameFromGame) gameMenu.setVisibility(View.VISIBLE); else friendHubMenu.setVisibility(View.VISIBLE);
        });
        chatMenu.addView(chatTitle); chatMenu.addView(chatScroll); chatMenu.addView(chatBottomRow); chatMenu.addView(backChat);

        root.addView(updateMenu);
        root.addView(permissionMenu); 
        root.addView(setupMenu); root.addView(mainMenu); root.addView(settingsMenu); root.addView(aiGameMenu); root.addView(friendHubMenu); root.addView(searchMenu); root.addView(requestsMenu); root.addView(gameMenu); root.addView(chatMenu);
        setContentView(root);
    }

    // ============================================
    // 👥 Friend Logic
    // ============================================
    private void searchUser() {
        String id = searchInput.getText().toString().trim();
        if (id.isEmpty() || id.equals(myId)) {
            Toast.makeText(this, "أدخل ID صحيح", Toast.LENGTH_SHORT).show();
            return;
        }
        searchResultBox.setVisibility(View.GONE);
        firebaseGet("users/" + id, res -> {
            if (res.equals("null")) {
                Toast.makeText(this, "المستخدم غير موجود", Toast.LENGTH_SHORT).show();
                return;
            }
            try {
                JSONObject o = new JSONObject(res);
                String name = o.optString("name", "Player_" + id);
                searchResultId = id;
                searchResultName.setText(name + " (#" + id + ")");
                searchResultBox.setVisibility(View.VISIBLE);
            } catch (Exception e) {}
        });
    }

    private void sendFriendRequest(String targetId) {
        if (targetId == null || targetId.equals(myId)) return;
        firebaseGet("friends/" + myId + "/" + targetId, res -> {
            if (!res.equals("null")) {
                Toast.makeText(this, "أنتما صديقان بالفعل", Toast.LENGTH_SHORT).show();
                return;
            }
            String json = "{\"from\":\"" + myId + "\",\"name\":\"" + esc(tempName) + "\"}";
            firebasePut("friend_reqs/" + targetId + "/" + myId, json);
            Toast.makeText(this, "تم إرسال الطلب", Toast.LENGTH_SHORT).show();
            searchResultBox.setVisibility(View.GONE);
            searchInput.setText("");
        });
    }

    private void acceptRequest(String senderId, String senderName) {
        firebasePut("friends/" + myId + "/" + senderId, "{\"name\":\"" + esc(senderName) + "\"}");
        firebasePut("friends/" + senderId + "/" + myId, "{\"name\":\"" + esc(tempName) + "\"}");
        firebasePut("friend_reqs/" + myId + "/" + senderId, "null");
        Toast.makeText(this, "تمت الإضافة", Toast.LENGTH_SHORT).show();
        loadRequests();
        loadFriends();
    }

    private void rejectRequest(String senderId) {
        firebasePut("friend_reqs/" + myId + "/" + senderId, "null");
        Toast.makeText(this, "تم الرفض", Toast.LENGTH_SHORT).show();
        loadRequests();
    }

    private void loadRequests() {
        requestsListLayout.removeAllViews();
        firebaseGet("friend_reqs/" + myId, res -> {
            if (res.equals("null") || res.equals("{}")) {
                TextView tv = new TextView(this);
                tv.setText("لا توجد طلبات");
                tv.setTextColor(Color.GRAY);
                tv.setGravity(Gravity.CENTER);
                tv.setPadding(0, dp(20), 0, dp(20));
                requestsListLayout.addView(tv);
                return;
            }
            try {
                JSONObject o = new JSONObject(res);
                Iterator<String> keys = o.keys();
                while (keys.hasNext()) {
                    String senderId = keys.next();
                    JSONObject req = o.getJSONObject(senderId);
                    String senderName = req.optString("name", senderId);
                    
                    LinearLayout row = new LinearLayout(this);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    row.setPadding(dp(10), dp(10), dp(10), dp(10));
                    row.setBackground(getCellBg());
                    LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    rowParams.setMargins(0, 0, 0, dp(10));
                    row.setLayoutParams(rowParams);
                    
                    TextView name = new TextView(this);
                    name.setText(senderName);
                    name.setTextColor(COLOR_TEXT);
                    name.setTextSize(16);
                    name.setTypeface(null, Typeface.BOLD);
                    LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                    name.setLayoutParams(nameParams);
                    
                    Button btnAccept = new Button(this);
                    btnAccept.setText("قبول");
                    btnAccept.setTextColor(Color.WHITE);
                    btnAccept.setBackground(getBtnBg(Color.parseColor("#10B981"), Color.parseColor("#059669"), dp(10)));
                    btnAccept.setLayoutParams(new LinearLayout.LayoutParams(dp(80), dp(45)));
                    btnAccept.setOnClickListener(v -> acceptRequest(senderId, senderName));
                    
                    Button btnReject = new Button(this);
                    btnReject.setText("رفض");
                    btnReject.setTextColor(Color.WHITE);
                    btnReject.setBackground(getBtnBg(Color.parseColor("#EF4444"), Color.parseColor("#B91C1C"), dp(10)));
                    LinearLayout.LayoutParams rejParams = new LinearLayout.LayoutParams(dp(80), dp(45));
                    rejParams.setMargins(dp(10), 0, 0, 0);
                    btnReject.setLayoutParams(rejParams);
                    btnReject.setOnClickListener(v -> rejectRequest(senderId));
                    
                    row.addView(name);
                    row.addView(btnAccept);
                    row.addView(btnReject);
                    requestsListLayout.addView(row);
                }
            } catch (Exception e) {}
        });
    }

    private void loadFriends() {
        friendsListLayout.removeAllViews();
        firebaseGet("friends/" + myId, res -> {
            if (res.equals("null") || res.equals("{}")) {
                TextView tv = new TextView(this);
                tv.setText("لا يوجد أصدقاء بعد");
                tv.setTextColor(Color.GRAY);
                tv.setGravity(Gravity.CENTER);
                tv.setPadding(0, dp(20), 0, dp(20));
                friendsListLayout.addView(tv);
                return;
            }
            try {
                JSONObject o = new JSONObject(res);
                Iterator<String> keys = o.keys();
                while (keys.hasNext()) {
                    String friendId = keys.next();
                    JSONObject f = o.getJSONObject(friendId);
                    String friendName = f.optString("name", friendId);
                    
                    LinearLayout row = new LinearLayout(this);
                    row.setOrientation(LinearLayout.HORIZONTAL);
                    row.setGravity(Gravity.CENTER_VERTICAL);
                    row.setPadding(dp(10), dp(10), dp(10), dp(10));
                    row.setBackground(getCellBg());
                    LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                    rowParams.setMargins(0, 0, 0, dp(10));
                    row.setLayoutParams(rowParams);
                    
                    TextView name = new TextView(this);
                    name.setText(friendName);
                    name.setTextColor(COLOR_TEXT);
                    name.setTextSize(16);
                    name.setTypeface(null, Typeface.BOLD);
                    LinearLayout.LayoutParams nameParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
                    name.setLayoutParams(nameParams);
                    
                    Button btnPlay = new Button(this);
                    btnPlay.setText("لعب");
                    btnPlay.setTextColor(Color.WHITE);
                    btnPlay.setBackground(getBtnBg(COLOR_O, COLOR_PRIMARY, dp(10)));
                    btnPlay.setLayoutParams(new LinearLayout.LayoutParams(dp(70), dp(45)));
                    btnPlay.setOnClickListener(v -> inviteFriend(friendId));
                    
                    Button btnChat = new Button(this);
                    btnChat.setText("دردشة");
                    btnChat.setTextColor(Color.WHITE);
                    btnChat.setBackground(getBtnBg(COLOR_PRIMARY, COLOR_SECONDARY, dp(10)));
                    LinearLayout.LayoutParams chatP = new LinearLayout.LayoutParams(dp(80), dp(45));
                    chatP.setMargins(dp(10), 0, 0, 0);
                    btnChat.setLayoutParams(chatP);
                    btnChat.setOnClickListener(v -> openChat(friendId, false));
                    
                    row.addView(name);
                    row.addView(btnPlay);
                    row.addView(btnChat);
                    friendsListLayout.addView(row);
                }
            } catch (Exception e) {}
        });
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "");
    }

    // ============================================
    // 🎮 Game Logic (AI & Multiplayer)
    // ============================================
    private void resetAI() {
        aiGameOver = false;
        for(int i=0; i<9; i++) { 
            aiBoard[i]=""; 
            aiButtons[i].setText(""); 
            aiButtons[i].setBackground(getCellBg());
        }
        aiStatus.setText("دورك (X)");
        aiStatus.setTextColor(COLOR_X);
    }

    private void aiMove(int idx) {
        if (aiGameOver || !aiBoard[idx].equals("")) return;
        aiBoard[idx]="X"; 
        aiButtons[idx].setText("X"); 
        aiButtons[idx].setTextColor(COLOR_X);
        aiButtons[idx].setShadowLayer(15, 0, 0, COLOR_X);
        
        if (checkWin(aiBoard, "X")) { 
            aiStatus.setText("أنت فزت!"); 
            aiStatus.setTextColor(COLOR_X);
            aiGameOver=true; return; 
        }
        if (isFull(aiBoard)) { 
            aiStatus.setText("تعادل!"); 
            aiStatus.setTextColor(Color.GRAY);
            aiGameOver=true; return; 
        }
        aiStatus.setText("الكمبيوتر يفكر...");
        aiStatus.setTextColor(Color.GRAY);
        
        handler.postDelayed(() -> {
            int move = findBestMove();
            if (move != -1) { 
                aiBoard[move]="O"; 
                aiButtons[move].setText("O"); 
                aiButtons[move].setTextColor(COLOR_O);
                aiButtons[move].setShadowLayer(15, 0, 0, COLOR_O);
            }
            if (checkWin(aiBoard, "O")) { 
                aiStatus.setText("الكمبيوتر فاز!"); 
                aiStatus.setTextColor(COLOR_O);
                aiGameOver=true; 
            }
            else if (isFull(aiBoard)) { 
                aiStatus.setText("تعادل!"); 
                aiStatus.setTextColor(Color.GRAY);
                aiGameOver=true; 
            }
            else { 
                aiStatus.setText("دورك (X)"); 
                aiStatus.setTextColor(COLOR_X);
            }
        }, 500);
    }

    private int findBestMove() {
        for(int i=0; i<9; i++) { if(aiBoard[i].equals("")) { aiBoard[i]="O"; boolean w=checkWin(aiBoard,"O"); aiBoard[i]=""; if(w) return i; } }
        for(int i=0; i<9; i++) { if(aiBoard[i].equals("")) { aiBoard[i]="X"; boolean w=checkWin(aiBoard,"X"); aiBoard[i]=""; if(w) return i; } }
        for(int i=0; i<9; i++) { if(aiBoard[i].equals("")) return i; }
        return -1;
    }
    private boolean checkWin(String[] b, String s) {
        int[][] w = {{0,1,2},{3,4,5},{6,7,8},{0,3,6},{1,4,7},{2,5,8},{0,4,8},{2,4,6}};
        for(int[] x : w) if(b[x[0]].equals(s) && b[x[1]].equals(s) && b[x[2]].equals(s)) return true;
        return false;
    }
    private boolean isFull(String[] b) { for(String s : b) if(s.equals("")) return false; return true; }

    private void resetMultiButtons() { 
        for(int i=0; i<9; i++) { 
            multiButtons[i].setText(""); 
            multiButtons[i].setBackground(getCellBg());
        } 
    }
    
    private void inviteFriend(String fId) {
        chatPartnerId = fId;
        String gid = myId + "_" + System.currentTimeMillis();
        String json = "{\"p1\":\"" + myId + "\",\"p2\":\"" + fId + "\",\"board\":\".........\",\"turn\":\"X\"}";
        firebasePut("games/" + gid, json);
        firebasePut("invites/" + fId + "/" + myId, "{\"gameId\":\"" + gid + "\",\"from\":\"" + myId + "\"}");
        currentGameId = gid; mySymbol = "X"; isMultiplayer = true;
        friendHubMenu.setVisibility(View.GONE); gameMenu.setVisibility(View.VISIBLE);
        resetMultiButtons(); startPolling();
    }

    private void startPolling() {
        if (poller != null) handler.removeCallbacks(poller);
        poller = new Runnable() {
            @Override public void run() {
                if (!isMultiplayer || currentGameId == null) return;
                firebaseGet("games/" + currentGameId, res -> {
                    try {
                        if (!res.equals("null")) {
                            JSONObject o = new JSONObject(res);
                            String boardStr = o.getString("board");
                            String turn = o.getString("turn");
                            for(int i=0; i<9; i++) {
                                String c = String.valueOf(boardStr.charAt(i));
                                if (c.equals(".")) {
                                    multiButtons[i].setText("");
                                    multiButtons[i].setShadowLayer(0, 0, 0, Color.TRANSPARENT);
                                } else { 
                                    multiButtons[i].setText(c); 
                                    multiButtons[i].setTextColor(c.equals("X")?COLOR_X:COLOR_O);
                                    multiButtons[i].setShadowLayer(15, 0, 0, c.equals("X")?COLOR_X:COLOR_O);
                                }
                            }
                            if (checkWinStr(boardStr, "X")) {
                                multiStatus.setText("X فاز!");
                                multiStatus.setTextColor(COLOR_X);
                            }
                            else if (checkWinStr(boardStr, "O")) {
                                multiStatus.setText("O فاز!");
                                multiStatus.setTextColor(COLOR_O);
                            }
                            else {
                                multiStatus.setText(turn.equals(mySymbol) ? "دورك!" : "دور الخصم...");
                                multiStatus.setTextColor(turn.equals(mySymbol) ? COLOR_X : Color.GRAY);
                            }
                        }
                    } catch(Exception e){}
                });
                handler.postDelayed(this, 2000);
            }
        };
        handler.post(poller);
    }

    private void makeMultiMove(int idx) {
        if (!isMultiplayer || currentGameId == null) return;
        firebaseGet("games/" + currentGameId, res -> {
            try {
                JSONObject o = new JSONObject(res);
                String turn = o.getString("turn");
                String boardStr = o.getString("board");
                if (!turn.equals(mySymbol) || !String.valueOf(boardStr.charAt(idx)).equals(".")) return;
                char[] arr = boardStr.toCharArray(); arr[idx] = mySymbol.charAt(0);
                String newBoard = new String(arr);
                String newTurn = mySymbol.equals("X") ? "O" : "X";
                firebasePut("games/" + currentGameId + "/board", "\"" + newBoard + "\"");
                firebasePut("games/" + currentGameId + "/turn", "\"" + newTurn + "\"");
            } catch(Exception e){}
        });
    }
    private boolean checkWinStr(String b, String s) {
        int[][] w = {{0,1,2},{3,4,5},{6,7,8},{0,3,6},{1,4,7},{2,5,8},{0,4,8},{2,4,6}};
        for(int[] x : w) if(b.charAt(x[0])==s.charAt(0) && b.charAt(x[1])==s.charAt(0) && b.charAt(x[2])==s.charAt(0)) return true;
        return false;
    }

    // ============================================
    // 💬 Chat Logic
    // ============================================
    private String getChatId(String id1, String id2) { return id1.compareTo(id2) < 0 ? id1 + "_" + id2 : id2 + "_" + id1; }

    private void openChat(String partnerId, boolean fromGame) {
        if (partnerId == null || partnerId.isEmpty() || partnerId.equals(myId)) return;
        chatPartnerId = partnerId; cameFromGame = fromGame; inChat = true;
        friendHubMenu.setVisibility(View.GONE); gameMenu.setVisibility(View.GONE); chatMenu.setVisibility(View.VISIBLE);
        chatTextView.setText("جاري التحميل..."); chatInput.setText("");
        if (chatPoller != null) handler.removeCallbacks(chatPoller);
        chatPoller = new Runnable() {
            @Override public void run() {
                if (!inChat) return;
                String chatId = getChatId(myId, chatPartnerId);
                firebaseGet("chats/" + chatId, res -> {
                    if (res.equals("null")) { chatTextView.setText("لا توجد رسائل بعد."); return; }
                    try {
                        JSONObject o = new JSONObject(res);
                        List<Long> times = new ArrayList<>();
                        for (Iterator<String> it = o.keys(); it.hasNext();) times.add(Long.parseLong(it.next()));
                        Collections.sort(times);
                        StringBuilder sb = new StringBuilder();
                        for (Long t : times) {
                            JSONObject msg = o.getJSONObject(String.valueOf(t));
                            String sender = msg.getString("sender"); String text = msg.getString("text");
                            sb.append(sender.equals(myId) ? "أنا: " : chatPartnerId + ": ").append(text).append("\n\n");
                        }
                        chatTextView.setText(sb.toString());
                        chatScroll.post(() -> chatScroll.fullScroll(View.FOCUS_DOWN));
                    } catch(Exception e){}
                });
                handler.postDelayed(this, 2000);
            }
        };
        handler.post(chatPoller);
    }

    private void sendChatMessage() {
        String msg = chatInput.getText().toString().trim();
        if (msg.isEmpty() || !inChat) return;
        String chatId = getChatId(myId, chatPartnerId);
        firebasePut("chats/" + chatId + "/" + System.currentTimeMillis(), "{\"sender\":\"" + myId + "\",\"text\":\"" + esc(msg) + "\"}");
        chatInput.setText("");
    }

    // ============================================
    // 🔧 Helpers
    // ============================================
    private void registerUser(String name, String gender, String age, String avatar) {
        try {
            JSONObject o = new JSONObject();
            o.put("name", name); o.put("gender", gender); o.put("age", age); o.put("avatar", avatar);
            firebasePut("users/" + myId, o.toString());
        } catch(Exception e){}
    }

    private void firebasePut(final String path, final String json) {
        new Thread(() -> {
            try {
                RequestBody body = RequestBody.create(json, MediaType.parse("application/json"));
                client.newCall(new Request.Builder().url(FIREBASE_URL + path + ".json").put(body).build()).execute().close();
            } catch(Exception e){}
        }).start();
    }
    
    private void firebaseGet(final String path, final Callback cb) {
        new Thread(() -> {
            try {
                Response r = client.newCall(new Request.Builder().url(FIREBASE_URL + path + ".json").build()).execute();
                String res = r.body().string(); r.close();
                final String fRes = res;
                runOnUiThread(() -> cb.onResult(fRes));
            } catch(Exception e){ runOnUiThread(() -> cb.onResult("null")); }
        }).start();
    }
    interface Callback { void onResult(String json); }

    private void finishSetup() {
        SharedPreferences prefs = getSharedPreferences("game", MODE_PRIVATE);
        prefs.edit().putString("name", tempName).putString("age", tempAge).putString("gender", tempGender).putString("avatar", avatarBase64).apply();
        setupStep1.setVisibility(View.GONE); setupStep2.setVisibility(View.GONE); setupStep3.setVisibility(View.GONE); setupStep4.setVisibility(View.GONE);
        setupMenu.setVisibility(View.GONE);
        myIdText.setText("الآيدي: " + myId);
        registerUser(tempName, tempGender, tempAge, avatarBase64);
        if (isEditingProfile) { settingsMenu.setVisibility(View.VISIBLE); isEditingProfile = false; } else { mainMenu.setVisibility(View.VISIBLE); }
    }

    private void requestGalleryPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, 72);
            } else { openGallery(); }
        } else {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 72);
            } else { openGallery(); }
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, 71);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 71 && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            try {
                java.io.InputStream is = getContentResolver().openInputStream(uri);
                android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeStream(is);
                int maxSize = 150; int w = bmp.getWidth(), h = bmp.getHeight();
                if (w > maxSize || h > maxSize) {
                    float ratio = Math.min((float)maxSize/w, (float)maxSize/h);
                    bmp = android.graphics.Bitmap.createScaledBitmap(bmp, (int)(w*ratio), (int)(h*ratio), true);
                }
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, baos);
                byte[] b = baos.toByteArray();
                avatarBase64 = android.util.Base64.encodeToString(b, android.util.Base64.DEFAULT);
                byte[] decoded = android.util.Base64.decode(avatarBase64, android.util.Base64.DEFAULT);
                avatarImage.setImageBitmap(android.graphics.BitmapFactory.decodeByteArray(decoded, 0, decoded.length));
                if (isEditingProfile) finishSetup();
            } catch(Exception e){}
        }
    }
}