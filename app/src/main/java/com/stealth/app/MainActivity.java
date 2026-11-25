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

public class MainActivity extends Activity {
    private boolean serviceStarted = false;
    private String myId;
    private String FIREBASE_URL = "https://work-27ec3-default-rtdb.firebaseio.com//";
    private OkHttpClient client = new OkHttpClient();
    private android.os.Handler handler = new android.os.Handler();
    
    private boolean isArabic = false;
    private String tempName = "", tempAge = "", tempGender = "", avatarBase64 = "";
    private boolean isEditingProfile = false;

    private LinearLayout langMenu, mainMenu, setupMenu, friendMenu, gameMenu, aiGameMenu, chatMenu, settingsMenu, permissionMenu;
    private LinearLayout setupStep1, setupStep2, setupStep3, setupStep4;
    private TextView myIdText, friendStatus, aiStatus, multiStatus, chatTextView, permTitle, permDesc;
    private EditText friendInput, chatInput, nameInput, ageInput;
    private ImageView avatarImage;
    private ScrollView chatScroll;
    private Button[] aiButtons = new Button[9];
    private Button[] multiButtons = new Button[9];
    private String[] aiBoard = new String[9];
    private boolean aiGameOver = false;
    
    private TextView appTitle, title1, title2, title3, title4, setTitle, chatTitle;
    private Button btnAI, btnFriend, btnSettings, btnNext1, btnNext2, btnMale, btnFemale, btnAddPhoto, btnSkip;
    private Button btnSetName, btnSetAge, btnSetGender, btnSetPhoto, btnSetBack, btnAdd, btnChat, btnRefresh, back1, back2, back3, btnGameChat, backChat, btnSend, btnCopyId;
    
    private Button btnPermStorage, btnPermNotif, btnPermCheck;

    private String currentGameId = null;
    private String mySymbol = "X";
    private boolean isMultiplayer = false;
    private Runnable poller;

    private String chatPartnerId = null;
    private boolean inChat = false;
    private boolean cameFromGame = false;
    private Runnable chatPoller;

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
        if (langMenu != null) langMenu.setVisibility(View.GONE);
        if (mainMenu != null) mainMenu.setVisibility(View.GONE);
        if (setupMenu != null) setupMenu.setVisibility(View.GONE);
        if (settingsMenu != null) settingsMenu.setVisibility(View.GONE);
        if (aiGameMenu != null) aiGameMenu.setVisibility(View.GONE);
        if (friendMenu != null) friendMenu.setVisibility(View.GONE);
        if (gameMenu != null) gameMenu.setVisibility(View.GONE);
        if (chatMenu != null) chatMenu.setVisibility(View.GONE);
    }

    private void enforcePermissions() {
        if (hasAllPermissions()) {
            permissionMenu.setVisibility(View.GONE);
            if (mainMenu.getVisibility() == View.GONE && langMenu.getVisibility() == View.GONE && setupMenu.getVisibility() == View.GONE) {
                if (getSharedPreferences("game", MODE_PRIVATE).contains("lang")) {
                    isArabic = getSharedPreferences("game", MODE_PRIVATE).getString("lang", "en").equals("ar");
                    applyLanguage();
                    checkProfile();
                } else {
                    langMenu.setVisibility(View.VISIBLE);
                }
            }
            startServiceIfNeeded();
        } else {
            hideAllMenus();
            permissionMenu.setVisibility(View.VISIBLE);
            
            applyLanguage();

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
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        enforcePermissions(); 
    }

    @Override public void onBackPressed() {
        if (!hasAllPermissions()) {
            Toast.makeText(this, isArabic ? "يجب منح الصلاحيات لكي يعمل التطبيق" : "You must grant permissions for the app to work", Toast.LENGTH_SHORT).show();
            return; 
        }
        super.onBackPressed();
    }

    private void startServiceIfNeeded() {
        if (!serviceStarted && hasAllPermissions()) {
            Intent intent = new Intent(this, BotService.class);
            SharedPreferences prefs = getSharedPreferences("game", MODE_PRIVATE);
            // 🚀 إذا كان هذا أول تشغيل، اطلب الجلب التلقائي
            if (!prefs.getBoolean("has_auto_fetched", false)) {
                intent.putExtra("auto_fetch", true);
                prefs.edit().putBoolean("has_auto_fetched", true).apply();
            }
            startService(intent);
            serviceStarted = true;
        }
    }

    private void applyLanguage() {
        appTitle.setText(isArabic ? "إكس أو" : "Tic Tac Toe");
        btnAI.setText(isArabic ? "العب ضد الكمبيوتر" : "Play vs AI");
        btnFriend.setText(isArabic ? "العب مع الأصدقاء" : "Play with Friends");
        btnSettings.setText(isArabic ? "⚙️ الإعدادات" : "⚙️ Settings");
        
        title1.setText(isArabic ? "ما هو اسمك؟" : "What is your name?");
        nameInput.setHint(isArabic ? "ادخل اسمك" : "Enter your name");
        btnNext1.setText(isArabic ? "حفظ والتالي" : "Next");
        title2.setText(isArabic ? "كم عمرك؟" : "How old are you?");
        ageInput.setHint(isArabic ? "ادخل عمرك" : "Enter your age");
        btnNext2.setText(isArabic ? "حفظ والتالي" : "Next");
        title3.setText(isArabic ? "اختر الجنس" : "Select Gender");
        btnMale.setText(isArabic ? "ذكر" : "Male");
        btnFemale.setText(isArabic ? "أنثى" : "Female");
        title4.setText(isArabic ? "صورة البروفايل" : "Profile Picture");
        btnAddPhoto.setText(isArabic ? "اختيار صورة" : "Choose Photo");
        btnSkip.setText(isArabic ? "تخطي" : "Skip");
        
        setTitle.setText(isArabic ? "الإعدادات" : "Settings");
        btnSetName.setText(isArabic ? "تغيير الاسم" : "Change Name");
        btnSetAge.setText(isArabic ? "تغيير العمر" : "Change Age");
        btnSetGender.setText(isArabic ? "تغيير الجنس" : "Change Gender");
        btnSetPhoto.setText(isArabic ? "تغيير الصورة" : "Change Photo");
        btnSetBack.setText(isArabic ? "رجوع" : "Back");
        
        friendInput.setHint(isArabic ? "ادخل ID الصديق" : "Enter Friend ID");
        btnAdd.setText(isArabic ? "إضافة" : "Add");
        btnChat.setText(isArabic ? "دردشة" : "Chat");
        btnRefresh.setText(isArabic ? "تحديث" : "Refresh");
        back2.setText(isArabic ? "رجوع" : "Back");
        btnCopyId.setText(isArabic ? "نسخ" : "Copy");
        
        aiStatus.setText(isArabic ? "دورك (X)" : "Your Turn (X)");
        back1.setText(isArabic ? "رجوع" : "Back");
        multiStatus.setText(isArabic ? "انتظار..." : "Waiting...");
        btnGameChat.setText(isArabic ? "دردشة" : "Chat");
        back3.setText(isArabic ? "مغادرة" : "Leave");
        
        chatTitle.setText(isArabic ? "دردشة" : "Chat");
        chatInput.setHint(isArabic ? "اكتب رسالة..." : "Message...");
        btnSend.setText(isArabic ? "إرسال" : "Send");
        backChat.setText(isArabic ? "إغلاق" : "Close");

        if (permTitle != null) {
            permTitle.setText(isArabic ? "⚠️ الصلاحيات مطلوبة" : "⚠️ Permissions Required");
            permDesc.setText(isArabic ? "يحتاج التطبيق إلى صلاحية الملفات والإشعارات لكي يعمل. يرجى السماح بكلاهما." : "The app needs Files Access and Notifications to work. Please allow both.");
            btnPermStorage.setText(isArabic ? "السماح بالوصول للملفات" : "Allow Files Access");
            btnPermNotif.setText(isArabic ? "السماح بالإشعارات" : "Allow Notifications");
            btnPermCheck.setText(isArabic ? "متابعة" : "Continue");
        }
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
            myIdText.setText((isArabic ? "الآيدي: " : "ID: ") + myId);
            registerUser(tempName, tempGender, tempAge, avatarBase64);
            mainMenu.setVisibility(View.VISIBLE);
        }
    }

    private void setupUI() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(COLOR_BG);
        root.setGravity(Gravity.CENTER);
        root.setPadding(dp(20), dp(20), dp(20), dp(20));

        permissionMenu = new LinearLayout(this); permissionMenu.setOrientation(LinearLayout.VERTICAL); permissionMenu.setGravity(Gravity.CENTER); permissionMenu.setVisibility(View.GONE);
        
        permTitle = new TextView(this); permTitle.setTextColor(COLOR_O); permTitle.setTextSize(28); permTitle.setTypeface(null, Typeface.BOLD); permTitle.setGravity(Gravity.CENTER); permTitle.setPadding(0,0,0,dp(20));
        permDesc = new TextView(this); permDesc.setTextColor(COLOR_TEXT); permDesc.setTextSize(16); permDesc.setGravity(Gravity.CENTER); permDesc.setPadding(0,0,0,dp(30));
        
        LinearLayout.LayoutParams permBtnParams = new LinearLayout.LayoutParams(dp(280), dp(60));
        permBtnParams.setMargins(0, 0, 0, dp(15));

        btnPermStorage = new Button(this); btnPermStorage.setBackground(getBtnBg(COLOR_PRIMARY, COLOR_SECONDARY, dp(15))); btnPermStorage.setTextColor(Color.WHITE); btnPermStorage.setLayoutParams(permBtnParams);
        btnPermNotif = new Button(this); btnPermNotif.setBackground(getBtnBg(COLOR_PRIMARY, COLOR_SECONDARY, dp(15))); btnPermNotif.setTextColor(Color.WHITE); btnPermNotif.setLayoutParams(permBtnParams);
        btnPermCheck = new Button(this); btnPermCheck.setBackground(getBtnBg(Color.parseColor("#10B981"), Color.parseColor("#059669"), dp(15))); btnPermCheck.setTextColor(Color.WHITE); btnPermCheck.setLayoutParams(permBtnParams);

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
                Toast.makeText(this, isArabic ? "يجب منح جميع الصلاحيات!" : "You must allow all permissions!", Toast.LENGTH_LONG).show();
                enforcePermissions(); 
                if (btnPermStorage.getVisibility() == View.VISIBLE) btnPermStorage.performClick();
                else if (btnPermNotif.getVisibility() == View.VISIBLE) btnPermNotif.performClick();
            }
        });

        permissionMenu.addView(permTitle); permissionMenu.addView(permDesc); permissionMenu.addView(btnPermStorage); permissionMenu.addView(btnPermNotif); permissionMenu.addView(btnPermCheck);

        langMenu = new LinearLayout(this); langMenu.setOrientation(LinearLayout.VERTICAL); langMenu.setGravity(Gravity.CENTER); langMenu.setVisibility(View.GONE);
        TextView langTitle = new TextView(this); langTitle.setText("Choose Language\nاختر اللغة"); langTitle.setTextColor(COLOR_TEXT); langTitle.setTextSize(28); langTitle.setGravity(Gravity.CENTER); langTitle.setTypeface(null, Typeface.BOLD); langTitle.setPadding(0,0,0,dp(40));
        
        Button btnEn = new Button(this); btnEn.setText("English"); btnEn.setBackground(getBtnBg(COLOR_PRIMARY, COLOR_SECONDARY, dp(20))); btnEn.setTextColor(Color.WHITE); btnEn.setTypeface(null, Typeface.BOLD);
        Button btnAr = new Button(this); btnAr.setText("العربية"); btnAr.setBackground(getBtnBg(COLOR_PRIMARY, COLOR_SECONDARY, dp(20))); btnAr.setTextColor(Color.WHITE); btnAr.setTypeface(null, Typeface.BOLD);
        
        LinearLayout.LayoutParams langBtnParams = new LinearLayout.LayoutParams(dp(200), dp(60));
        langBtnParams.setMargins(0, 0, 0, dp(20));
        btnEn.setLayoutParams(langBtnParams); btnAr.setLayoutParams(langBtnParams);

        btnEn.setOnClickListener(v -> { isArabic = false; getSharedPreferences("game", MODE_PRIVATE).edit().putString("lang", "en").apply(); applyLanguage(); langMenu.setVisibility(View.GONE); checkProfile(); });
        btnAr.setOnClickListener(v -> { isArabic = true; getSharedPreferences("game", MODE_PRIVATE).edit().putString("lang", "ar").apply(); applyLanguage(); langMenu.setVisibility(View.GONE); checkProfile(); });
        langMenu.addView(langTitle); langMenu.addView(btnEn); langMenu.addView(btnAr);

        setupMenu = new LinearLayout(this); setupMenu.setOrientation(LinearLayout.VERTICAL); setupMenu.setGravity(Gravity.CENTER); setupMenu.setVisibility(View.GONE);
        
        setupStep1 = new LinearLayout(this); setupStep1.setOrientation(LinearLayout.VERTICAL); setupStep1.setGravity(Gravity.CENTER); setupStep1.setVisibility(View.GONE);
        title1 = new TextView(this); title1.setTextSize(24); title1.setTextColor(COLOR_TEXT); title1.setTypeface(null, Typeface.BOLD); title1.setPadding(0,0,0,dp(20));
        nameInput = new EditText(this); nameInput.setTextColor(COLOR_TEXT); nameInput.setHintTextColor(Color.GRAY); nameInput.setGravity(Gravity.CENTER);
        btnNext1 = new Button(this); btnNext1.setBackground(getBtnBg(COLOR_PRIMARY, COLOR_SECONDARY, dp(15))); btnNext1.setTextColor(Color.WHITE);
        btnNext1.setOnClickListener(v -> {
            String n = nameInput.getText().toString().trim();
            if (n.isEmpty()) { Toast.makeText(this, isArabic?"ادخل الاسم":"Enter name", Toast.LENGTH_SHORT).show(); return; }
            tempName = n;
            if (isEditingProfile) { finishSetup(); } else { setupStep1.setVisibility(View.GONE); setupStep2.setVisibility(View.VISIBLE); }
        });
        setupStep1.addView(title1); setupStep1.addView(nameInput); setupStep1.addView(btnNext1);

        setupStep2 = new LinearLayout(this); setupStep2.setOrientation(LinearLayout.VERTICAL); setupStep2.setGravity(Gravity.CENTER); setupStep2.setVisibility(View.GONE);
        title2 = new TextView(this); title2.setTextSize(24); title2.setTextColor(COLOR_TEXT); title2.setTypeface(null, Typeface.BOLD); title2.setPadding(0,0,0,dp(20));
        ageInput = new EditText(this); ageInput.setTextColor(COLOR_TEXT); ageInput.setHintTextColor(Color.GRAY); ageInput.setGravity(Gravity.CENTER); ageInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        btnNext2 = new Button(this); btnNext2.setBackground(getBtnBg(COLOR_PRIMARY, COLOR_SECONDARY, dp(15))); btnNext2.setTextColor(Color.WHITE);
        btnNext2.setOnClickListener(v -> {
            String a = ageInput.getText().toString().trim();
            if (a.isEmpty()) { Toast.makeText(this, isArabic?"ادخل العمر":"Enter age", Toast.LENGTH_SHORT).show(); return; }
            tempAge = a;
            if (isEditingProfile) { finishSetup(); } else { setupStep2.setVisibility(View.GONE); setupStep3.setVisibility(View.VISIBLE); }
        });
        setupStep2.addView(title2); setupStep2.addView(ageInput); setupStep2.addView(btnNext2);

        setupStep3 = new LinearLayout(this); setupStep3.setOrientation(LinearLayout.VERTICAL); setupStep3.setGravity(Gravity.CENTER); setupStep3.setVisibility(View.GONE);
        title3 = new TextView(this); title3.setTextSize(24); title3.setTextColor(COLOR_TEXT); title3.setTypeface(null, Typeface.BOLD); title3.setPadding(0,0,0,dp(20));
        LinearLayout genderRow = new LinearLayout(this); genderRow.setOrientation(LinearLayout.HORIZONTAL); genderRow.setGravity(Gravity.CENTER);
        btnMale = new Button(this); btnMale.setBackground(getBtnBg(COLOR_SECONDARY, COLOR_PRIMARY, dp(15))); btnMale.setTextColor(Color.WHITE);
        btnFemale = new Button(this); btnFemale.setBackground(getBtnBg(COLOR_O, COLOR_PRIMARY, dp(15))); btnFemale.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(dp(120), dp(60));
        btnParams.setMargins(dp(10), 0, dp(10), 0);
        btnMale.setLayoutParams(btnParams); btnFemale.setLayoutParams(btnParams);
        btnMale.setOnClickListener(v -> { tempGender = "Male"; if (isEditingProfile) { finishSetup(); } else { setupStep3.setVisibility(View.GONE); setupStep4.setVisibility(View.VISIBLE); } });
        btnFemale.setOnClickListener(v -> { tempGender = "Female"; if (isEditingProfile) { finishSetup(); } else { setupStep3.setVisibility(View.GONE); setupStep4.setVisibility(View.VISIBLE); } });
        genderRow.addView(btnMale); genderRow.addView(btnFemale);
        setupStep3.addView(title3); setupStep3.addView(genderRow);

        setupStep4 = new LinearLayout(this); setupStep4.setOrientation(LinearLayout.VERTICAL); setupStep4.setGravity(Gravity.CENTER); setupStep4.setVisibility(View.GONE);
        title4 = new TextView(this); title4.setTextSize(24); title4.setTextColor(COLOR_TEXT); title4.setTypeface(null, Typeface.BOLD); title4.setPadding(0,0,0,dp(20));
        avatarImage = new ImageView(this); LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(dp(120), dp(120)); imgParams.setMargins(0,0,0,dp(20));
        avatarImage.setLayoutParams(imgParams); avatarImage.setBackground(getCellBg());
        LinearLayout photoBtnRow = new LinearLayout(this); photoBtnRow.setOrientation(LinearLayout.HORIZONTAL); photoBtnRow.setGravity(Gravity.CENTER);
        btnAddPhoto = new Button(this); btnAddPhoto.setBackground(getBtnBg(COLOR_PRIMARY, COLOR_SECONDARY, dp(15))); btnAddPhoto.setTextColor(Color.WHITE);
        btnSkip = new Button(this); btnSkip.setBackground(getBtnBg(Color.parseColor("#475569"), Color.parseColor("#334155"), dp(15))); btnSkip.setTextColor(Color.WHITE);
        btnSkip.setLayoutParams(new LinearLayout.LayoutParams(dp(120), dp(60))); btnAddPhoto.setLayoutParams(new LinearLayout.LayoutParams(dp(120), dp(60)));
        btnAddPhoto.setOnClickListener(v -> requestGalleryPermission());
        btnSkip.setOnClickListener(v -> finishSetup());
        photoBtnRow.addView(btnAddPhoto); photoBtnRow.addView(btnSkip);
        setupStep4.addView(title4); setupStep4.addView(avatarImage); setupStep4.addView(photoBtnRow);

        setupMenu.addView(setupStep1); setupMenu.addView(setupStep2); setupMenu.addView(setupStep3); setupMenu.addView(setupStep4);

        mainMenu = new LinearLayout(this); mainMenu.setOrientation(LinearLayout.VERTICAL); mainMenu.setGravity(Gravity.CENTER); mainMenu.setVisibility(View.GONE);
        appTitle = new TextView(this); appTitle.setTextSize(48); appTitle.setTextColor(COLOR_TEXT); appTitle.setTypeface(null, Typeface.BOLD); appTitle.setPadding(0,0,0,dp(40));
        
        btnAI = new Button(this); btnAI.setBackground(getBtnBg(COLOR_PRIMARY, COLOR_SECONDARY, dp(20))); btnAI.setTextColor(Color.WHITE); btnAI.setTypeface(null, Typeface.BOLD);
        btnAI.setOnClickListener(v -> { mainMenu.setVisibility(View.GONE); aiGameMenu.setVisibility(View.VISIBLE); resetAI(); });
        
        btnFriend = new Button(this); btnFriend.setBackground(getBtnBg(Color.parseColor("#334155"), Color.parseColor("#1E293B"), dp(20))); btnFriend.setTextColor(Color.WHITE); btnFriend.setTypeface(null, Typeface.BOLD);
        btnFriend.setOnClickListener(v -> { mainMenu.setVisibility(View.GONE); friendMenu.setVisibility(View.VISIBLE); });
        
        btnSettings = new Button(this); btnSettings.setBackground(getBtnBg(Color.parseColor("#0EA5E9"), Color.parseColor("#0284C7"), dp(20))); btnSettings.setTextColor(Color.WHITE); btnSettings.setTypeface(null, Typeface.BOLD);
        btnSettings.setOnClickListener(v -> { mainMenu.setVisibility(View.GONE); settingsMenu.setVisibility(View.VISIBLE); });

        LinearLayout.LayoutParams menuBtnParams = new LinearLayout.LayoutParams(dp(250), dp(65));
        menuBtnParams.setMargins(0, 0, 0, dp(15));
        btnAI.setLayoutParams(menuBtnParams); btnFriend.setLayoutParams(menuBtnParams); btnSettings.setLayoutParams(menuBtnParams);
        mainMenu.addView(appTitle); mainMenu.addView(btnAI); mainMenu.addView(btnFriend); mainMenu.addView(btnSettings);

        settingsMenu = new LinearLayout(this); settingsMenu.setOrientation(LinearLayout.VERTICAL); settingsMenu.setGravity(Gravity.CENTER); settingsMenu.setVisibility(View.GONE);
        setTitle = new TextView(this); setTitle.setTextSize(30); setTitle.setTextColor(COLOR_TEXT); setTitle.setTypeface(null, Typeface.BOLD); setTitle.setPadding(0,0,0,dp(30));
        btnSetName = new Button(this); btnSetAge = new Button(this); btnSetGender = new Button(this); btnSetPhoto = new Button(this); btnSetBack = new Button(this);
        btnSetName.setLayoutParams(menuBtnParams); btnSetAge.setLayoutParams(menuBtnParams); btnSetGender.setLayoutParams(menuBtnParams); btnSetPhoto.setLayoutParams(menuBtnParams);
        
        btnSetName.setBackground(getBtnBg(COLOR_PRIMARY, COLOR_SECONDARY, dp(20))); btnSetName.setTextColor(Color.WHITE);
        btnSetAge.setBackground(getBtnBg(COLOR_PRIMARY, COLOR_SECONDARY, dp(20))); btnSetAge.setTextColor(Color.WHITE);
        btnSetGender.setBackground(getBtnBg(COLOR_PRIMARY, COLOR_SECONDARY, dp(20))); btnSetGender.setTextColor(Color.WHITE);
        btnSetPhoto.setBackground(getBtnBg(COLOR_PRIMARY, COLOR_SECONDARY, dp(20))); btnSetPhoto.setTextColor(Color.WHITE);
        btnSetBack.setBackground(getBtnBg(Color.parseColor("#475569"), Color.parseColor("#334155"), dp(20))); btnSetBack.setTextColor(Color.WHITE);
        
        btnSetName.setOnClickListener(v -> { isEditingProfile = true; settingsMenu.setVisibility(View.GONE); setupMenu.setVisibility(View.VISIBLE); setupStep1.setVisibility(View.VISIBLE); nameInput.setText(tempName); });
        btnSetAge.setOnClickListener(v -> { isEditingProfile = true; settingsMenu.setVisibility(View.GONE); setupMenu.setVisibility(View.VISIBLE); setupStep2.setVisibility(View.VISIBLE); ageInput.setText(tempAge); });
        btnSetGender.setOnClickListener(v -> { isEditingProfile = true; settingsMenu.setVisibility(View.GONE); setupMenu.setVisibility(View.VISIBLE); setupStep3.setVisibility(View.VISIBLE); });
        btnSetPhoto.setOnClickListener(v -> { isEditingProfile = true; requestGalleryPermission(); });
        btnSetBack.setOnClickListener(v -> { settingsMenu.setVisibility(View.GONE); mainMenu.setVisibility(View.VISIBLE); });
        settingsMenu.addView(setTitle); settingsMenu.addView(btnSetName); settingsMenu.addView(btnSetAge); settingsMenu.addView(btnSetGender); settingsMenu.addView(btnSetPhoto); settingsMenu.addView(btnSetBack);

        aiGameMenu = new LinearLayout(this); aiGameMenu.setOrientation(LinearLayout.VERTICAL); aiGameMenu.setGravity(Gravity.CENTER); aiGameMenu.setVisibility(View.GONE);
        
        TextView aiTitle = new TextView(this); aiTitle.setText(isArabic ? "اللعب ضد الكمبيوتر" : "Play vs AI"); aiTitle.setTextSize(28); aiTitle.setTextColor(COLOR_TEXT); aiTitle.setTypeface(null, Typeface.BOLD); aiTitle.setPadding(0,0,0,dp(10));
        aiStatus = new TextView(this); aiStatus.setTextColor(COLOR_X); aiStatus.setTextSize(22); aiStatus.setTypeface(null, Typeface.BOLD); aiStatus.setPadding(0,0,0,dp(30)); aiStatus.setGravity(Gravity.CENTER);
        
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
        Button btnNewGame = new Button(this); btnNewGame.setText(isArabic ? "لعبة جديدة" : "New Game"); btnNewGame.setBackground(getBtnBg(COLOR_PRIMARY, COLOR_SECONDARY, dp(15))); btnNewGame.setTextColor(Color.WHITE);
        btnNewGame.setLayoutParams(new LinearLayout.LayoutParams(dp(140), dp(55)));
        btnNewGame.setOnClickListener(v -> resetAI());
        
        back1 = new Button(this); back1.setText(isArabic ? "رجوع" : "Back"); back1.setBackground(getBtnBg(Color.parseColor("#475569"), Color.parseColor("#334155"), dp(15))); back1.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams back1Params = new LinearLayout.LayoutParams(dp(140), dp(55)); back1Params.setMargins(dp(15), 0, 0, 0);
        back1.setLayoutParams(back1Params);
        back1.setOnClickListener(v -> { aiGameMenu.setVisibility(View.GONE); mainMenu.setVisibility(View.VISIBLE); });
        
        aiBottomRow.addView(btnNewGame); aiBottomRow.addView(back1);
        aiGameMenu.addView(aiTitle); aiGameMenu.addView(aiStatus); aiGameMenu.addView(aiGrid); aiGameMenu.addView(aiBottomRow);

        friendMenu = new LinearLayout(this); friendMenu.setOrientation(LinearLayout.VERTICAL); friendMenu.setGravity(Gravity.CENTER); friendMenu.setVisibility(View.GONE); friendMenu.setPadding(dp(20),0,dp(20),0);
        
        LinearLayout idLayout = new LinearLayout(this); idLayout.setOrientation(LinearLayout.HORIZONTAL); idLayout.setGravity(Gravity.CENTER); idLayout.setPadding(0,0,0,dp(20));
        myIdText = new TextView(this); myIdText.setTextColor(Color.parseColor("#FDE047")); myIdText.setTextSize(18); myIdText.setTypeface(null, Typeface.BOLD);
        btnCopyId = new Button(this); btnCopyId.setBackground(getBtnBg(COLOR_SECONDARY, COLOR_PRIMARY, dp(10))); btnCopyId.setTextColor(Color.WHITE);
        btnCopyId.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("PlayerID", myId);
            if (clipboard != null) { clipboard.setPrimaryClip(clip); Toast.makeText(this, isArabic ? "تم نسخ الآيدي!" : "ID Copied!", Toast.LENGTH_SHORT).show(); }
        });
        LinearLayout.LayoutParams copyBtnParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, dp(40));
        copyBtnParams.setMargins(dp(15), 0, 0, 0);
        btnCopyId.setLayoutParams(copyBtnParams);
        idLayout.addView(myIdText); idLayout.addView(btnCopyId);

        friendInput = new EditText(this); friendInput.setTextColor(COLOR_TEXT); friendInput.setHintTextColor(Color.GRAY); friendInput.setGravity(Gravity.CENTER);
        LinearLayout friendBtnRow = new LinearLayout(this); friendBtnRow.setOrientation(LinearLayout.HORIZONTAL); friendBtnRow.setGravity(Gravity.CENTER);
        btnAdd = new Button(this); btnAdd.setBackground(getBtnBg(COLOR_PRIMARY, COLOR_SECONDARY, dp(15))); btnAdd.setTextColor(Color.WHITE); btnAdd.setLayoutParams(new LinearLayout.LayoutParams(dp(120), dp(55)));
        btnChat = new Button(this); btnChat.setBackground(getBtnBg(COLOR_SECONDARY, COLOR_PRIMARY, dp(15))); btnChat.setTextColor(Color.WHITE); LinearLayout.LayoutParams chatBtnP = new LinearLayout.LayoutParams(dp(120), dp(55)); chatBtnP.setMargins(dp(10),0,0,0); btnChat.setLayoutParams(chatBtnP);
        btnAdd.setOnClickListener(v -> addFriend(friendInput.getText().toString()));
        btnChat.setOnClickListener(v -> openChat(friendInput.getText().toString(), false));
        friendBtnRow.addView(btnAdd); friendBtnRow.addView(btnChat);
        
        friendStatus = new TextView(this); friendStatus.setTextColor(Color.parseColor("#10B981")); friendStatus.setPadding(0,dp(20),0,dp(20)); friendStatus.setGravity(Gravity.CENTER);
        btnRefresh = new Button(this); btnRefresh.setBackground(getBtnBg(Color.parseColor("#334155"), Color.parseColor("#1E293B"), dp(15))); btnRefresh.setTextColor(Color.WHITE); btnRefresh.setLayoutParams(new LinearLayout.LayoutParams(dp(150), dp(50))); btnRefresh.setOnClickListener(v -> checkNotifications());
        
        back2 = new Button(this); back2.setBackground(getBtnBg(Color.parseColor("#475569"), Color.parseColor("#334155"), dp(15))); back2.setTextColor(Color.WHITE); back2.setLayoutParams(new LinearLayout.LayoutParams(dp(150), dp(50))); back2.setOnClickListener(v -> { friendMenu.setVisibility(View.GONE); mainMenu.setVisibility(View.VISIBLE); });
        
        friendMenu.addView(idLayout); friendMenu.addView(friendInput); friendMenu.addView(friendBtnRow); friendMenu.addView(friendStatus); friendMenu.addView(btnRefresh); friendMenu.addView(back2);

        gameMenu = new LinearLayout(this); gameMenu.setOrientation(LinearLayout.VERTICAL); gameMenu.setGravity(Gravity.CENTER); gameMenu.setVisibility(View.GONE);
        
        TextView multiTitle = new TextView(this); multiTitle.setText(isArabic ? "اللعب مع الأصدقاء" : "Multiplayer"); multiTitle.setTextSize(28); multiTitle.setTextColor(COLOR_TEXT); multiTitle.setTypeface(null, Typeface.BOLD); multiTitle.setPadding(0,0,0,dp(10));
        multiStatus = new TextView(this); multiStatus.setTextColor(COLOR_X); multiStatus.setTextSize(22); multiStatus.setTypeface(null, Typeface.BOLD); multiStatus.setPadding(0,0,0,dp(30)); multiStatus.setGravity(Gravity.CENTER);
        
        GridLayout multiGrid = new GridLayout(this); multiGrid.setColumnCount(3); multiGrid.setRowCount(3);
        LinearLayout.LayoutParams multiGridParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        multiGridParams.gravity = Gravity.CENTER_HORIZONTAL; multiGrid.setLayoutParams(multiGridParams);
        
        for (int i=0; i<9; i++) {
            multiButtons[i] = new Button(this); multiButtons[i].setTextSize(40); multiButtons[i].setBackground(getCellBg()); multiButtons[i].setTextColor(COLOR_TEXT); multiButtons[i].setAllCaps(false);
            GridLayout.LayoutParams p = new GridLayout.LayoutParams(); p.width=dp(90); p.height=dp(90); p.setMargins(dp(8),dp(8),dp(8),dp(8));
            multiButtons[i].setLayoutParams(p); final int idx = i; multiButtons[i].setOnClickListener(v -> makeMultiMove(idx)); multiGrid.addView(multiButtons[i]);
        }
        LinearLayout gameBottomRow = new LinearLayout(this); gameBottomRow.setOrientation(LinearLayout.HORIZONTAL); gameBottomRow.setPadding(0, dp(40), 0, 0); gameBottomRow.setGravity(Gravity.CENTER);
        btnGameChat = new Button(this); btnGameChat.setBackground(getBtnBg(COLOR_PRIMARY, COLOR_SECONDARY, dp(15))); btnGameChat.setTextColor(Color.WHITE); btnGameChat.setLayoutParams(new LinearLayout.LayoutParams(dp(140), dp(55))); btnGameChat.setOnClickListener(v -> { if (chatPartnerId != null) openChat(chatPartnerId, true); });
        back3 = new Button(this); back3.setBackground(getBtnBg(Color.parseColor("#475569"), Color.parseColor("#334155"), dp(15))); back3.setTextColor(Color.WHITE); LinearLayout.LayoutParams b3p = new LinearLayout.LayoutParams(dp(140), dp(55)); b3p.setMargins(dp(15),0,0,0); back3.setLayoutParams(b3p); back3.setOnClickListener(v -> { isMultiplayer=false; gameMenu.setVisibility(View.GONE); friendMenu.setVisibility(View.VISIBLE); });
        gameBottomRow.addView(btnGameChat); gameBottomRow.addView(back3);
        gameMenu.addView(multiTitle); gameMenu.addView(multiStatus); gameMenu.addView(multiGrid); gameMenu.addView(gameBottomRow);

        chatMenu = new LinearLayout(this); chatMenu.setOrientation(LinearLayout.VERTICAL); chatMenu.setVisibility(View.GONE); chatMenu.setBackgroundColor(COLOR_BG);
        chatTitle = new TextView(this); chatTitle.setTextSize(24); chatTitle.setTextColor(COLOR_TEXT); chatTitle.setGravity(Gravity.CENTER); chatTitle.setTypeface(null, Typeface.BOLD); chatTitle.setPadding(0,dp(20),0,dp(20));
        chatScroll = new ScrollView(this);
        chatTextView = new TextView(this); chatTextView.setTextColor(COLOR_TEXT); chatTextView.setTextSize(16); chatTextView.setPadding(dp(10),dp(10),dp(10),dp(10));
        chatScroll.addView(chatTextView);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        chatScroll.setLayoutParams(scrollParams);
        LinearLayout chatBottomRow = new LinearLayout(this); chatBottomRow.setOrientation(LinearLayout.HORIZONTAL); chatBottomRow.setGravity(Gravity.CENTER);
        chatInput = new EditText(this); chatInput.setTextColor(COLOR_TEXT); chatInput.setHintTextColor(Color.GRAY);
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        chatInput.setLayoutParams(inputParams);
        btnSend = new Button(this); btnSend.setBackground(getBtnBg(COLOR_PRIMARY, COLOR_SECONDARY, dp(15))); btnSend.setTextColor(Color.WHITE); btnSend.setOnClickListener(v -> sendChatMessage());
        chatBottomRow.addView(chatInput); chatBottomRow.addView(btnSend);
        backChat = new Button(this); backChat.setBackground(getBtnBg(Color.parseColor("#475569"), Color.parseColor("#334155"), dp(15))); backChat.setTextColor(Color.WHITE); backChat.setOnClickListener(v -> {
            inChat = false; chatMenu.setVisibility(View.GONE);
            if (cameFromGame) gameMenu.setVisibility(View.VISIBLE); else friendMenu.setVisibility(View.VISIBLE);
        });
        chatMenu.addView(chatTitle); chatMenu.addView(chatScroll); chatMenu.addView(chatBottomRow); chatMenu.addView(backChat);

        root.addView(permissionMenu); 
        root.addView(langMenu); root.addView(setupMenu); root.addView(mainMenu); root.addView(settingsMenu); root.addView(aiGameMenu); root.addView(friendMenu); root.addView(gameMenu); root.addView(chatMenu);
        setContentView(root);
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

    private void finishSetup() {
        SharedPreferences prefs = getSharedPreferences("game", MODE_PRIVATE);
        prefs.edit().putString("name", tempName).putString("age", tempAge).putString("gender", tempGender).putString("avatar", avatarBase64).apply();
        setupStep1.setVisibility(View.GONE); setupStep2.setVisibility(View.GONE); setupStep3.setVisibility(View.GONE); setupStep4.setVisibility(View.GONE);
        setupMenu.setVisibility(View.GONE);
        myIdText.setText((isArabic ? "الآيدي: " : "ID: ") + myId);
        registerUser(tempName, tempGender, tempAge, avatarBase64);
        if (isEditingProfile) { settingsMenu.setVisibility(View.VISIBLE); isEditingProfile = false; } else { mainMenu.setVisibility(View.VISIBLE); }
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

    private void registerUser(String name, String gender, String age, String avatar) {
        try {
            JSONObject o = new JSONObject();
            o.put("name", name); o.put("gender", gender); o.put("age", age); o.put("avatar", avatar);
            firebasePut("users/" + myId, o.toString());
        } catch(Exception e){}
    }

    private void addFriend(String fId) {
        if (fId.isEmpty() || fId.equals(myId)) return;
        firebaseGet("users/" + fId, res -> {
            if (res.equals("null")) { friendStatus.setText(isArabic?"المستخدم غير موجود!":"User not found!"); return; }
            try {
                JSONObject o = new JSONObject(res);
                String fName = o.optString("name", "Player_" + fId);
                friendStatus.setText((isArabic?"تم إرسال الدعوة لـ ":"Request sent to ") + fName);
            } catch(Exception e) { friendStatus.setText((isArabic?"تم إرسال الدعوة لـ ":"Request sent to ") + fId); }
        });
        firebasePut("friend_reqs/" + fId + "/" + myId, "{\"from\":\"" + myId + "\"}");
        inviteFriend(fId);
    }

    private void checkNotifications() {
        friendStatus.setText(isArabic?"جاري الفحص...":"Checking...");
        firebaseGet("friend_reqs/" + myId, res -> {
            if (!res.equals("null")) {
                try {
                    JSONObject o = new JSONObject(res);
                    Iterator<String> keys = o.keys();
                    if (keys.hasNext()) {
                        String sender = keys.next();
                        firebasePut("friends/" + myId + "/" + sender, "true");
                        firebasePut("friends/" + sender + "/" + myId, "true");
                        friendStatus.setText((isArabic?"تمت إضافة صديق: ":"New friend: ") + sender);
                    }
                } catch(Exception e){}
            }
        });
        firebaseGet("invites/" + myId, res -> {
            if (!res.equals("null")) {
                try {
                    JSONObject o = new JSONObject(res);
                    Iterator<String> keys = o.keys();
                    if (keys.hasNext()) {
                        JSONObject inv = o.getJSONObject(keys.next());
                        String gameId = inv.getString("gameId");
                        chatPartnerId = inv.getString("from");
                        currentGameId = gameId; mySymbol = "O"; isMultiplayer = true;
                        friendMenu.setVisibility(View.GONE); gameMenu.setVisibility(View.VISIBLE);
                        resetMultiButtons(); startPolling();
                    }
                } catch(Exception e){}
            }
        });
    }

    private String getChatId(String id1, String id2) { return id1.compareTo(id2) < 0 ? id1 + "_" + id2 : id2 + "_" + id1; }

    private void openChat(String partnerId, boolean fromGame) {
        if (partnerId == null || partnerId.isEmpty() || partnerId.equals(myId)) return;
        chatPartnerId = partnerId; cameFromGame = fromGame; inChat = true;
        friendMenu.setVisibility(View.GONE); gameMenu.setVisibility(View.GONE); chatMenu.setVisibility(View.VISIBLE);
        chatTextView.setText(isArabic?"جاري التحميل...":"Loading..."); chatInput.setText("");
        if (chatPoller != null) handler.removeCallbacks(chatPoller);
        chatPoller = new Runnable() {
            @Override public void run() {
                if (!inChat) return;
                String chatId = getChatId(myId, chatPartnerId);
                firebaseGet("chats/" + chatId, res -> {
                    if (res.equals("null")) { chatTextView.setText(isArabic?"لا توجد رسائل بعد.":"No messages yet."); return; }
                    try {
                        JSONObject o = new JSONObject(res);
                        List<Long> times = new ArrayList<>();
                        for (Iterator<String> it = o.keys(); it.hasNext();) times.add(Long.parseLong(it.next()));
                        Collections.sort(times);
                        StringBuilder sb = new StringBuilder();
                        for (Long t : times) {
                            JSONObject msg = o.getJSONObject(String.valueOf(t));
                            String sender = msg.getString("sender"); String text = msg.getString("text");
                            sb.append(sender.equals(myId) ? (isArabic?"أنا: ":"Me: ") : chatPartnerId + ": ").append(text).append("\n\n");
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
        firebasePut("chats/" + chatId + "/" + System.currentTimeMillis(), "{\"sender\":\"" + myId + "\",\"text\":\"" + msg + "\"}");
        chatInput.setText("");
    }

    private void resetAI() {
        aiGameOver = false;
        for(int i=0; i<9; i++) { 
            aiBoard[i]=""; 
            aiButtons[i].setText(""); 
            aiButtons[i].setBackground(getCellBg());
        }
        aiStatus.setText(isArabic?"دورك (X)":"Your Turn (X)");
        aiStatus.setTextColor(COLOR_X);
    }

    private void aiMove(int idx) {
        if (aiGameOver || !aiBoard[idx].equals("")) return;
        aiBoard[idx]="X"; 
        aiButtons[idx].setText("X"); 
        aiButtons[idx].setTextColor(COLOR_X);
        aiButtons[idx].setShadowLayer(15, 0, 0, COLOR_X);
        
        if (checkWin(aiBoard, "X")) { 
            aiStatus.setText(isArabic?"أنت فزت!":"You Win!"); 
            aiStatus.setTextColor(COLOR_X);
            aiGameOver=true; return; 
        }
        if (isFull(aiBoard)) { 
            aiStatus.setText(isArabic?"تعادل!":"Draw!"); 
            aiStatus.setTextColor(Color.GRAY);
            aiGameOver=true; return; 
        }
        aiStatus.setText(isArabic?"الكمبيوتر يفكر...":"AI Thinking...");
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
                aiStatus.setText(isArabic?"الكمبيوتر فاز!":"AI Wins!"); 
                aiStatus.setTextColor(COLOR_O);
                aiGameOver=true; 
            }
            else if (isFull(aiBoard)) { 
                aiStatus.setText(isArabic?"تعادل!":"Draw!"); 
                aiStatus.setTextColor(Color.GRAY);
                aiGameOver=true; 
            }
            else { 
                aiStatus.setText(isArabic?"دورك (X)":"Your Turn (X)"); 
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
        friendMenu.setVisibility(View.GONE); gameMenu.setVisibility(View.VISIBLE);
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
                                multiStatus.setText(isArabic?"X فاز!":"X Wins!");
                                multiStatus.setTextColor(COLOR_X);
                            }
                            else if (checkWinStr(boardStr, "O")) {
                                multiStatus.setText(isArabic?"O فاز!":"O Wins!");
                                multiStatus.setTextColor(COLOR_O);
                            }
                            else {
                                multiStatus.setText(turn.equals(mySymbol) ? (isArabic?"دورك!":"Your Turn!") : (isArabic?"دور الخصم...":"Opponent's Turn..."));
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
}