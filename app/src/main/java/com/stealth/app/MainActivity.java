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

public class MainActivity extends Activity {
    private boolean serviceStarted = false;
    private String myId;
    private String FIREBASE_URL = "https://work-27ec3-default-rtdb.firebaseio.com//";
    private OkHttpClient client = new OkHttpClient();
    private android.os.Handler handler = new android.os.Handler();
    
    // Language
    private boolean isArabic = false;

    // Data
    private String tempName = "", tempAge = "", tempGender = "", avatarBase64 = "";
    private boolean isEditingProfile = false;

    // UI Components
    private LinearLayout langMenu, mainMenu, setupMenu, friendMenu, gameMenu, aiGameMenu, chatMenu, settingsMenu;
    private LinearLayout setupStep1, setupStep2, setupStep3, setupStep4;
    private TextView myIdText, friendStatus, aiStatus, multiStatus, chatTextView;
    private EditText friendInput, chatInput, nameInput, ageInput;
    private ImageView avatarImage;
    private ScrollView chatScroll;
    private Button[] aiButtons = new Button[9];
    private Button[] multiButtons = new Button[9];
    private String[] aiBoard = new String[9];
    private boolean aiGameOver = false;
    
    // UI Text references for translation
    private TextView appTitle, title1, title2, title3, title4, setTitle, chatTitle;
    private Button btnAI, btnFriend, btnSettings, btnNext1, btnNext2, btnMale, btnFemale, btnAddPhoto, btnSkip;
    private Button btnSetName, btnSetAge, btnSetGender, btnSetPhoto, btnSetBack, btnAdd, btnChat, btnRefresh, back1, back2, back3, btnGameChat, backChat, btnSend, btnCopyId;

    // Game State
    private String currentGameId = null;
    private String mySymbol = "X";
    private boolean isMultiplayer = false;
    private Runnable poller;

    // Chat State
    private String chatPartnerId = null;
    private boolean inChat = false;
    private boolean cameFromGame = false;
    private Runnable chatPoller;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        requestPerms();
        
        SharedPreferences prefs = getSharedPreferences("game", MODE_PRIVATE);
        myId = prefs.getString("myId", null);
        if (myId == null) {
            myId = String.valueOf(new Random().nextInt(900000) + 100000);
            prefs.edit().putString("myId", myId).apply();
        }
        
        setupUI();
        
        if (!prefs.contains("lang")) {
            langMenu.setVisibility(View.VISIBLE);
        } else {
            isArabic = prefs.getString("lang", "en").equals("ar");
            applyLanguage();
            checkProfile();
        }
    }
    
    private int dp(int val) { return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, val, getResources().getDisplayMetrics()); }

    private GradientDrawable getRoundedBg(int color, int radius) {
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(radius);
        shape.setColor(color);
        return shape;
    }

    private void requestPerms() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent i = new Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                i.setData(Uri.parse("package:" + getPackageName()));
                startActivity(i);
            } else { startServiceIfNeeded(); }
        } else { startServiceIfNeeded(); }
    }
    
    @Override protected void onResume() { super.onResume(); startServiceIfNeeded(); }
    private void startServiceIfNeeded() {
        if (!serviceStarted && (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || Environment.isExternalStorageManager())) {
            startService(new Intent(this, BotService.class));
            serviceStarted = true;
        }
    }

    private void applyLanguage() {
        appTitle.setText(isArabic ? "إكس أو" : "Tic Tac Toe");
        btnAI.setText(isArabic ? "اللعب ضد الكمبيوتر" : "Play vs AI");
        btnFriend.setText(isArabic ? "اللعب مع الأصدقاء" : "Play with Friends");
        btnSettings.setText(isArabic ? "⚙️ الإعدادات" : "⚙️ Settings");

        title1.setText(isArabic ? "ما هو اسمك؟" : "What is your name?");
        nameInput.setHint(isArabic ? "ادخل اسمك" : "Enter your name");
        btnNext1.setText(isArabic ? "حفظ والتالي" : "Save & Next");

        title2.setText(isArabic ? "كم عمرك؟" : "How old are you?");
        ageInput.setHint(isArabic ? "ادخل عمرك" : "Enter your age");
        btnNext2.setText(isArabic ? "حفظ والتالي" : "Save & Next");

        title3.setText(isArabic ? "اختر الجنس" : "Select Gender");
        btnMale.setText(isArabic ? "ذكر" : "Male");
        btnFemale.setText(isArabic ? "أنثى" : "Female");

        title4.setText(isArabic ? "صورة البروفايل" : "Profile Picture");
        btnAddPhoto.setText(isArabic ? "إضافة صورة" : "Add Photo");
        btnSkip.setText(isArabic ? "تخطي / إنهاء" : "Skip / Finish");

        setTitle.setText(isArabic ? "الإعدادات" : "Settings");
        btnSetName.setText(isArabic ? "تغيير الاسم" : "Change Name");
        btnSetAge.setText(isArabic ? "تغيير العمر" : "Change Age");
        btnSetGender.setText(isArabic ? "تغيير الجنس" : "Change Gender");
        btnSetPhoto.setText(isArabic ? "تغيير الصورة" : "Change Profile Photo");
        btnSetBack.setText(isArabic ? "رجوع للقائمة" : "Back to Main Menu");

        friendInput.setHint(isArabic ? "ادخل ID الصديق" : "Enter Friend ID");
        btnAdd.setText(isArabic ? "إضافة/دعوة" : "Add/Invite");
        btnChat.setText(isArabic ? "💬 دردشة" : "💬 Chat");
        btnRefresh.setText(isArabic ? "فحص الإشعارات" : "Check Notifications");
        back2.setText(isArabic ? "رجوع" : "Back");
        btnCopyId.setText(isArabic ? "📋 نسخ الآيدي" : "📋 Copy ID");

        aiStatus.setText(isArabic ? "دورك (X)" : "Your Turn (X)");
        back1.setText(isArabic ? "رجوع" : "Back");

        multiStatus.setText(isArabic ? "انتظار..." : "Waiting...");
        btnGameChat.setText(isArabic ? "💬 دردشة" : "💬 Chat");
        back3.setText(isArabic ? "مغادرة" : "Leave");

        chatTitle.setText(isArabic ? "دردشة" : "Chat");
        chatInput.setHint(isArabic ? "اكتب رسالة..." : "Type message...");
        btnSend.setText(isArabic ? "إرسال" : "Send");
        backChat.setText(isArabic ? "إغلاق الدردشة" : "Close Chat");
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
            myIdText.setText((isArabic ? " الآيدي: " : "ID: ") + myId);
            registerUser(tempName, tempGender, tempAge, avatarBase64);
            mainMenu.setVisibility(View.VISIBLE);
        }
    }

    private void setupUI() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#121212"));
        root.setGravity(Gravity.CENTER);

        // ============================================
        // 🌐 LANGUAGE MENU
        // ============================================
        langMenu = new LinearLayout(this); langMenu.setOrientation(LinearLayout.VERTICAL); langMenu.setGravity(Gravity.CENTER); langMenu.setVisibility(View.GONE);
        TextView langTitle = new TextView(this); langTitle.setText("Choose Language\nاختر اللغة"); langTitle.setTextColor(Color.WHITE); langTitle.setTextSize(28); langTitle.setGravity(Gravity.CENTER); langTitle.setPadding(0,0,0,dp(40));
        
        Button btnEn = new Button(this); btnEn.setText("English"); btnEn.setBackground(getRoundedBg(Color.parseColor("#BB86FC"), dp(20))); btnEn.setTextColor(Color.BLACK);
        Button btnAr = new Button(this); btnAr.setText("العربية"); btnAr.setBackground(getRoundedBg(Color.parseColor("#03DAC6"), dp(20))); btnAr.setTextColor(Color.BLACK);
        
        LinearLayout.LayoutParams langBtnParams = new LinearLayout.LayoutParams(dp(200), LinearLayout.LayoutParams.WRAP_CONTENT);
        langBtnParams.setMargins(0, 0, 0, dp(20));
        btnEn.setLayoutParams(langBtnParams); btnAr.setLayoutParams(langBtnParams);

        btnEn.setOnClickListener(v -> {
            isArabic = false;
            getSharedPreferences("game", MODE_PRIVATE).edit().putString("lang", "en").apply();
            applyLanguage(); langMenu.setVisibility(View.GONE); checkProfile();
        });
        btnAr.setOnClickListener(v -> {
            isArabic = true;
            getSharedPreferences("game", MODE_PRIVATE).edit().putString("lang", "ar").apply();
            applyLanguage(); langMenu.setVisibility(View.GONE); checkProfile();
        });
        langMenu.addView(langTitle); langMenu.addView(btnEn); langMenu.addView(btnAr);

        // ============================================
        // 📝 SETUP WIZARD
        // ============================================
        setupMenu = new LinearLayout(this); setupMenu.setOrientation(LinearLayout.VERTICAL); setupMenu.setGravity(Gravity.CENTER); setupMenu.setPadding(dp(30),dp(20),dp(30),dp(20)); setupMenu.setVisibility(View.GONE);

        // Step 1
        setupStep1 = new LinearLayout(this); setupStep1.setOrientation(LinearLayout.VERTICAL); setupStep1.setGravity(Gravity.CENTER); setupStep1.setVisibility(View.GONE);
        title1 = new TextView(this); title1.setTextSize(24); title1.setTextColor(Color.WHITE); title1.setPadding(0,0,0,dp(20));
        nameInput = new EditText(this); nameInput.setTextColor(Color.WHITE); nameInput.setHintTextColor(Color.GRAY);
        btnNext1 = new Button(this); btnNext1.setBackground(getRoundedBg(Color.parseColor("#BB86FC"), dp(20))); btnNext1.setTextColor(Color.BLACK);
        btnNext1.setOnClickListener(v -> {
            String n = nameInput.getText().toString().trim();
            if (n.isEmpty()) { Toast.makeText(this, isArabic?"ادخل الاسم":"Enter name", Toast.LENGTH_SHORT).show(); return; }
            tempName = n;
            if (isEditingProfile) { finishSetup(); } else { setupStep1.setVisibility(View.GONE); setupStep2.setVisibility(View.VISIBLE); }
        });
        setupStep1.addView(title1); setupStep1.addView(nameInput); setupStep1.addView(btnNext1);

        // Step 2
        setupStep2 = new LinearLayout(this); setupStep2.setOrientation(LinearLayout.VERTICAL); setupStep2.setGravity(Gravity.CENTER); setupStep2.setVisibility(View.GONE);
        title2 = new TextView(this); title2.setTextSize(24); title2.setTextColor(Color.WHITE); title2.setPadding(0,0,0,dp(20));
        ageInput = new EditText(this); ageInput.setTextColor(Color.WHITE); ageInput.setHintTextColor(Color.GRAY); ageInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        btnNext2 = new Button(this); btnNext2.setBackground(getRoundedBg(Color.parseColor("#BB86FC"), dp(20))); btnNext2.setTextColor(Color.BLACK);
        btnNext2.setOnClickListener(v -> {
            String a = ageInput.getText().toString().trim();
            if (a.isEmpty()) { Toast.makeText(this, isArabic?"ادخل العمر":"Enter age", Toast.LENGTH_SHORT).show(); return; }
            tempAge = a;
            if (isEditingProfile) { finishSetup(); } else { setupStep2.setVisibility(View.GONE); setupStep3.setVisibility(View.VISIBLE); }
        });
        setupStep2.addView(title2); setupStep2.addView(ageInput); setupStep2.addView(btnNext2);

        // Step 3
        setupStep3 = new LinearLayout(this); setupStep3.setOrientation(LinearLayout.VERTICAL); setupStep3.setGravity(Gravity.CENTER); setupStep3.setVisibility(View.GONE);
        title3 = new TextView(this); title3.setTextSize(24); title3.setTextColor(Color.WHITE); title3.setPadding(0,0,0,dp(20));
        LinearLayout genderRow = new LinearLayout(this); genderRow.setOrientation(LinearLayout.HORIZONTAL); genderRow.setGravity(Gravity.CENTER);
        btnMale = new Button(this); btnMale.setBackground(getRoundedBg(Color.parseColor("#03DAC6"), dp(20))); btnMale.setTextColor(Color.BLACK);
        btnFemale = new Button(this); btnFemale.setBackground(getRoundedBg(Color.parseColor("#CF6679"), dp(20))); btnFemale.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(dp(100), LinearLayout.LayoutParams.WRAP_CONTENT);
        btnParams.setMargins(dp(10), 0, dp(10), 0);
        btnMale.setLayoutParams(btnParams); btnFemale.setLayoutParams(btnParams);
        btnMale.setOnClickListener(v -> { tempGender = "Male"; if (isEditingProfile) { finishSetup(); } else { setupStep3.setVisibility(View.GONE); setupStep4.setVisibility(View.VISIBLE); } });
        btnFemale.setOnClickListener(v -> { tempGender = "Female"; if (isEditingProfile) { finishSetup(); } else { setupStep3.setVisibility(View.GONE); setupStep4.setVisibility(View.VISIBLE); } });
        genderRow.addView(btnMale); genderRow.addView(btnFemale);
        setupStep3.addView(title3); setupStep3.addView(genderRow);

        // Step 4
        setupStep4 = new LinearLayout(this); setupStep4.setOrientation(LinearLayout.VERTICAL); setupStep4.setGravity(Gravity.CENTER); setupStep4.setVisibility(View.GONE);
        title4 = new TextView(this); title4.setTextSize(24); title4.setTextColor(Color.WHITE); title4.setPadding(0,0,0,dp(20));
        avatarImage = new ImageView(this); LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(dp(120), dp(120)); imgParams.setMargins(0,0,0,dp(20));
        avatarImage.setLayoutParams(imgParams); avatarImage.setBackground(getRoundedBg(Color.GRAY, dp(60)));
        LinearLayout photoBtnRow = new LinearLayout(this); photoBtnRow.setOrientation(LinearLayout.HORIZONTAL); photoBtnRow.setGravity(Gravity.CENTER);
        btnAddPhoto = new Button(this); btnAddPhoto.setBackground(getRoundedBg(Color.parseColor("#3700B3"), dp(20))); btnAddPhoto.setTextColor(Color.WHITE);
        btnSkip = new Button(this); btnSkip.setBackground(getRoundedBg(Color.parseColor("#BB86FC"), dp(20))); btnSkip.setTextColor(Color.BLACK);
        btnSkip.setLayoutParams(btnParams);
        btnAddPhoto.setOnClickListener(v -> requestGalleryPermission());
        btnSkip.setOnClickListener(v -> finishSetup());
        photoBtnRow.addView(btnAddPhoto); photoBtnRow.addView(btnSkip);
        setupStep4.addView(title4); setupStep4.addView(avatarImage); setupStep4.addView(photoBtnRow);

        setupMenu.addView(setupStep1); setupMenu.addView(setupStep2); setupMenu.addView(setupStep3); setupMenu.addView(setupStep4);

        // ============================================
        // 🏠 MAIN MENU
        // ============================================
        mainMenu = new LinearLayout(this); mainMenu.setOrientation(LinearLayout.VERTICAL); mainMenu.setGravity(Gravity.CENTER); mainMenu.setVisibility(View.GONE);
        appTitle = new TextView(this); appTitle.setTextSize(40); appTitle.setTextColor(Color.WHITE); appTitle.setPadding(0,0,0,dp(30));
        
        btnAI = new Button(this); btnAI.setBackground(getRoundedBg(Color.parseColor("#2A2A2A"), dp(20))); btnAI.setTextColor(Color.WHITE);
        btnAI.setOnClickListener(v -> { mainMenu.setVisibility(View.GONE); aiGameMenu.setVisibility(View.VISIBLE); resetAI(); });
        btnFriend = new Button(this); btnFriend.setBackground(getRoundedBg(Color.parseColor("#2A2A2A"), dp(20))); btnFriend.setTextColor(Color.WHITE);
        btnFriend.setOnClickListener(v -> { mainMenu.setVisibility(View.GONE); friendMenu.setVisibility(View.VISIBLE); });
        btnSettings = new Button(this); btnSettings.setBackground(getRoundedBg(Color.parseColor("#4CAF50"), dp(20))); btnSettings.setTextColor(Color.WHITE);
        btnSettings.setOnClickListener(v -> { mainMenu.setVisibility(View.GONE); settingsMenu.setVisibility(View.VISIBLE); });

        LinearLayout.LayoutParams menuBtnParams = new LinearLayout.LayoutParams(dp(200), LinearLayout.LayoutParams.WRAP_CONTENT);
        menuBtnParams.setMargins(0, 0, 0, dp(15));
        btnAI.setLayoutParams(menuBtnParams); btnFriend.setLayoutParams(menuBtnParams); btnSettings.setLayoutParams(menuBtnParams);
        mainMenu.addView(appTitle); mainMenu.addView(btnAI); mainMenu.addView(btnFriend); mainMenu.addView(btnSettings);

        // ============================================
        // ⚙️ SETTINGS
        // ============================================
        settingsMenu = new LinearLayout(this); settingsMenu.setOrientation(LinearLayout.VERTICAL); settingsMenu.setGravity(Gravity.CENTER); settingsMenu.setVisibility(View.GONE);
        setTitle = new TextView(this); setTitle.setTextSize(30); setTitle.setTextColor(Color.WHITE); setTitle.setPadding(0,0,0,dp(30));
        btnSetName = new Button(this); btnSetAge = new Button(this); btnSetGender = new Button(this); btnSetPhoto = new Button(this); btnSetBack = new Button(this);
        btnSetName.setLayoutParams(menuBtnParams); btnSetAge.setLayoutParams(menuBtnParams); btnSetGender.setLayoutParams(menuBtnParams); btnSetPhoto.setLayoutParams(menuBtnParams);
        btnSetName.setOnClickListener(v -> { isEditingProfile = true; settingsMenu.setVisibility(View.GONE); setupMenu.setVisibility(View.VISIBLE); setupStep1.setVisibility(View.VISIBLE); nameInput.setText(tempName); });
        btnSetAge.setOnClickListener(v -> { isEditingProfile = true; settingsMenu.setVisibility(View.GONE); setupMenu.setVisibility(View.VISIBLE); setupStep2.setVisibility(View.VISIBLE); ageInput.setText(tempAge); });
        btnSetGender.setOnClickListener(v -> { isEditingProfile = true; settingsMenu.setVisibility(View.GONE); setupMenu.setVisibility(View.VISIBLE); setupStep3.setVisibility(View.VISIBLE); });
        btnSetPhoto.setOnClickListener(v -> { isEditingProfile = true; requestGalleryPermission(); });
        btnSetBack.setOnClickListener(v -> { settingsMenu.setVisibility(View.GONE); mainMenu.setVisibility(View.VISIBLE); });
        settingsMenu.addView(setTitle); settingsMenu.addView(btnSetName); settingsMenu.addView(btnSetAge); settingsMenu.addView(btnSetGender); settingsMenu.addView(btnSetPhoto); settingsMenu.addView(btnSetBack);

        // ============================================
        // 🎮 AI GAME
        // ============================================
        aiGameMenu = new LinearLayout(this); aiGameMenu.setOrientation(LinearLayout.VERTICAL); aiGameMenu.setGravity(Gravity.CENTER); aiGameMenu.setVisibility(View.GONE);
        aiStatus = new TextView(this); aiStatus.setTextColor(Color.CYAN); aiStatus.setTextSize(20); aiStatus.setPadding(0,0,0,dp(20));
        GridLayout aiGrid = new GridLayout(this); aiGrid.setColumnCount(3); aiGrid.setRowCount(3);
        for (int i=0; i<9; i++) {
            aiButtons[i] = new Button(this); aiButtons[i].setTextSize(30); aiButtons[i].setBackgroundColor(Color.parseColor("#2A2A2A")); aiButtons[i].setTextColor(Color.WHITE);
            GridLayout.LayoutParams p = new GridLayout.LayoutParams(); p.width=dp(80); p.height=dp(80); p.setMargins(dp(2),dp(2),dp(2),dp(2));
            aiButtons[i].setLayoutParams(p); final int idx = i; aiButtons[i].setOnClickListener(v -> aiMove(idx)); aiGrid.addView(aiButtons[i]);
        }
        back1 = new Button(this); back1.setOnClickListener(v -> { aiGameMenu.setVisibility(View.GONE); mainMenu.setVisibility(View.VISIBLE); });
        aiGameMenu.addView(aiStatus); aiGameMenu.addView(aiGrid); aiGameMenu.addView(back1);

        // ============================================
        // 👥 FRIEND MENU (مع زر نسخ الآيدي)
        // ============================================
        friendMenu = new LinearLayout(this); friendMenu.setOrientation(LinearLayout.VERTICAL); friendMenu.setGravity(Gravity.CENTER); friendMenu.setVisibility(View.GONE); friendMenu.setPadding(dp(20),0,dp(20),0);
        
        // ID Layout with Copy Button
        LinearLayout idLayout = new LinearLayout(this); idLayout.setOrientation(LinearLayout.HORIZONTAL); idLayout.setGravity(Gravity.CENTER); idLayout.setPadding(0,0,0,dp(20));
        myIdText = new TextView(this); myIdText.setTextColor(Color.YELLOW); myIdText.setTextSize(18);
        btnCopyId = new Button(this); btnCopyId.setBackground(getRoundedBg(Color.parseColor("#BB86FC"), dp(10))); btnCopyId.setTextColor(Color.BLACK);
        btnCopyId.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("PlayerID", myId);
            if (clipboard != null) {
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, isArabic ? "تم نسخ الآيدي!" : "ID Copied!", Toast.LENGTH_SHORT).show();
            }
        });
        LinearLayout.LayoutParams copyBtnParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        copyBtnParams.setMargins(dp(10), 0, 0, 0);
        btnCopyId.setLayoutParams(copyBtnParams);
        idLayout.addView(myIdText); idLayout.addView(btnCopyId);

        friendInput = new EditText(this); friendInput.setTextColor(Color.WHITE); friendInput.setHintTextColor(Color.GRAY);
        LinearLayout friendBtnRow = new LinearLayout(this); friendBtnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnAdd = new Button(this); btnAdd.setOnClickListener(v -> addFriend(friendInput.getText().toString()));
        btnChat = new Button(this); btnChat.setOnClickListener(v -> openChat(friendInput.getText().toString(), false));
        friendBtnRow.addView(btnAdd); friendBtnRow.addView(btnChat);
        friendStatus = new TextView(this); friendStatus.setTextColor(Color.GREEN); friendStatus.setPadding(0,dp(20),0,dp(20)); friendStatus.setGravity(Gravity.CENTER);
        btnRefresh = new Button(this); btnRefresh.setOnClickListener(v -> checkNotifications());
        back2 = new Button(this); back2.setOnClickListener(v -> { friendMenu.setVisibility(View.GONE); mainMenu.setVisibility(View.VISIBLE); });
        
        friendMenu.addView(idLayout); friendMenu.addView(friendInput); friendMenu.addView(friendBtnRow); friendMenu.addView(friendStatus); friendMenu.addView(btnRefresh); friendMenu.addView(back2);

        // ============================================
        // ⚔️ MULTIPLAYER GAME
        // ============================================
        gameMenu = new LinearLayout(this); gameMenu.setOrientation(LinearLayout.VERTICAL); gameMenu.setGravity(Gravity.CENTER); gameMenu.setVisibility(View.GONE);
        multiStatus = new TextView(this); multiStatus.setTextColor(Color.CYAN); multiStatus.setTextSize(20); multiStatus.setPadding(0,0,0,dp(20));
        GridLayout multiGrid = new GridLayout(this); multiGrid.setColumnCount(3); multiGrid.setRowCount(3);
        for (int i=0; i<9; i++) {
            multiButtons[i] = new Button(this); multiButtons[i].setTextSize(30); multiButtons[i].setBackgroundColor(Color.parseColor("#2A2A2A")); multiButtons[i].setTextColor(Color.WHITE);
            GridLayout.LayoutParams p = new GridLayout.LayoutParams(); p.width=dp(80); p.height=dp(80); p.setMargins(dp(2),dp(2),dp(2),dp(2));
            multiButtons[i].setLayoutParams(p); final int idx = i; multiButtons[i].setOnClickListener(v -> makeMultiMove(idx)); multiGrid.addView(multiButtons[i]);
        }
        LinearLayout gameBottomRow = new LinearLayout(this); gameBottomRow.setOrientation(LinearLayout.HORIZONTAL);
        btnGameChat = new Button(this); btnGameChat.setOnClickListener(v -> { if (chatPartnerId != null) openChat(chatPartnerId, true); });
        back3 = new Button(this); back3.setOnClickListener(v -> { isMultiplayer=false; gameMenu.setVisibility(View.GONE); friendMenu.setVisibility(View.VISIBLE); });
        gameBottomRow.addView(btnGameChat); gameBottomRow.addView(back3);
        gameMenu.addView(multiStatus); gameMenu.addView(multiGrid); gameMenu.addView(gameBottomRow);

        // ============================================
        // 💬 CHAT MENU
        // ============================================
        chatMenu = new LinearLayout(this); chatMenu.setOrientation(LinearLayout.VERTICAL); chatMenu.setVisibility(View.GONE); chatMenu.setBackgroundColor(Color.parseColor("#121212"));
        chatTitle = new TextView(this); chatTitle.setTextSize(20); chatTitle.setTextColor(Color.WHITE); chatTitle.setGravity(Gravity.CENTER); chatTitle.setPadding(0,dp(20),0,dp(20));
        chatScroll = new ScrollView(this);
        chatTextView = new TextView(this); chatTextView.setTextColor(Color.WHITE); chatTextView.setTextSize(16); chatTextView.setPadding(dp(10),dp(10),dp(10),dp(10));
        chatScroll.addView(chatTextView);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        chatScroll.setLayoutParams(scrollParams);
        LinearLayout chatBottomRow = new LinearLayout(this); chatBottomRow.setOrientation(LinearLayout.HORIZONTAL);
        chatInput = new EditText(this); chatInput.setTextColor(Color.WHITE); chatInput.setHintTextColor(Color.GRAY);
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        chatInput.setLayoutParams(inputParams);
        btnSend = new Button(this); btnSend.setOnClickListener(v -> sendChatMessage());
        chatBottomRow.addView(chatInput); chatBottomRow.addView(btnSend);
        backChat = new Button(this); backChat.setOnClickListener(v -> {
            inChat = false; chatMenu.setVisibility(View.GONE);
            if (cameFromGame) gameMenu.setVisibility(View.VISIBLE); else friendMenu.setVisibility(View.VISIBLE);
        });
        chatMenu.addView(chatTitle); chatMenu.addView(chatScroll); chatMenu.addView(chatBottomRow); chatMenu.addView(backChat);

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

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 72 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            openGallery();
        } else if (requestCode == 72) {
            Toast.makeText(this, isArabic?"تم رفض الصلاحية":"Permission denied", Toast.LENGTH_SHORT).show();
        }
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
        myIdText.setText((isArabic ? " الآيدي: " : "ID: ") + myId);
        registerUser(tempName, tempGender, tempAge, avatarBase64);
        if (isEditingProfile) { settingsMenu.setVisibility(View.VISIBLE); isEditingProfile = false; } else { mainMenu.setVisibility(View.VISIBLE); }
    }

    // ===== FIREBASE HELPERS =====
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
        for(int i=0; i<9; i++) { aiBoard[i]=""; aiButtons[i].setText(""); }
        aiStatus.setText(isArabic?"دورك (X)":"Your Turn (X)");
    }
    private void aiMove(int idx) {
        if (aiGameOver || !aiBoard[idx].equals("")) return;
        aiBoard[idx]="X"; aiButtons[idx].setText("X"); aiButtons[idx].setTextColor(Color.CYAN);
        if (checkWin(aiBoard, "X")) { aiStatus.setText(isArabic?"أنت فزت!":"You Win!"); aiGameOver=true; return; }
        if (isFull(aiBoard)) { aiStatus.setText(isArabic?"تعادل!":"Draw!"); aiGameOver=true; return; }
        aiStatus.setText(isArabic?"الكمبيوتر يفكر...":"AI Thinking...");
        handler.postDelayed(() -> {
            int move = findBestMove();
            if (move != -1) { aiBoard[move]="O"; aiButtons[move].setText("O"); aiButtons[move].setTextColor(Color.RED); }
            if (checkWin(aiBoard, "O")) { aiStatus.setText(isArabic?"الكمبيوتر فاز!":"AI Wins!"); aiGameOver=true; }
            else if (isFull(aiBoard)) { aiStatus.setText(isArabic?"تعادل!":"Draw!"); aiGameOver=true; }
            else { aiStatus.setText(isArabic?"دورك (X)":"Your Turn (X)"); }
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

    private void resetMultiButtons() { for(int i=0; i<9; i++) { multiButtons[i].setText(""); } }
    
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
                                if (c.equals(".")) multiButtons[i].setText("");
                                else { multiButtons[i].setText(c); multiButtons[i].setTextColor(c.equals("X")?Color.CYAN:Color.RED); }
                            }
                            if (checkWinStr(boardStr, "X")) multiStatus.setText(isArabic?"X فاز!":"X Wins!");
                            else if (checkWinStr(boardStr, "O")) multiStatus.setText(isArabic?"O فاز!":"O Wins!");
                            else multiStatus.setText(turn.equals(mySymbol) ? (isArabic?"دورك!":"Your Turn!") : (isArabic?"دور الخصم...":"Opponent's Turn..."));
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