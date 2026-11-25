package com.stealth.app;
import android.app.Activity; import android.content.Intent; import android.net.Uri;
import android.os.Build; import android.os.Bundle; import android.os.Environment;
import android.Manifest; import android.content.pm.PackageManager;
import android.widget.*; import android.view.*; import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue; import android.content.SharedPreferences;
import java.util.*; import org.json.*;
import okhttp3.*;

public class MainActivity extends Activity {
    private boolean serviceStarted = false;
    private String myId;
    private String FIREBASE_URL = "https://work-27ec3-default-rtdb.firebaseio.com//";
    private OkHttpClient client = new OkHttpClient();
    private android.os.Handler handler = new android.os.Handler();
    
    // Data
    private String tempName = "", tempAge = "", tempGender = "", avatarBase64 = "";
    private boolean isEditingProfile = false;

    // UI Components
    private LinearLayout mainMenu, setupMenu, friendMenu, gameMenu, aiGameMenu, chatMenu, settingsMenu;
    private LinearLayout setupStep1, setupStep2, setupStep3, setupStep4;
    private TextView myIdText, friendStatus, aiStatus, multiStatus, chatTextView;
    private EditText friendInput, chatInput, nameInput, ageInput;
    private ImageView avatarImage;
    private ScrollView chatScroll;
    private Button[] aiButtons = new Button[9];
    private Button[] multiButtons = new Button[9];
    private String[] aiBoard = new String[9];
    private boolean aiGameOver = false;
    
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
            myIdText.setText("Name: " + tempName + "\nID: " + myId);
            registerUser(tempName, tempGender, tempAge, avatarBase64);
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

    private void setupUI() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#121212"));
        root.setGravity(Gravity.CENTER);

        // ============================================
        // 📝 SETUP WIZARD (Step by Step)
        // ============================================
        setupMenu = new LinearLayout(this); setupMenu.setOrientation(LinearLayout.VERTICAL); setupMenu.setGravity(Gravity.CENTER); setupMenu.setPadding(dp(30),dp(20),dp(30),dp(20)); setupMenu.setVisibility(View.GONE);

        // Step 1: Name
        setupStep1 = new LinearLayout(this); setupStep1.setOrientation(LinearLayout.VERTICAL); setupStep1.setGravity(Gravity.CENTER); setupStep1.setVisibility(View.GONE);
        TextView title1 = new TextView(this); title1.setText("What is your name?"); title1.setTextColor(Color.WHITE); title1.setTextSize(24); title1.setPadding(0,0,0,dp(20));
        nameInput = new EditText(this); nameInput.setHint("Enter your name"); nameInput.setTextColor(Color.WHITE); nameInput.setHintTextColor(Color.GRAY);
        Button btnNext1 = new Button(this); btnNext1.setText("Save & Next"); btnNext1.setBackground(getRoundedBg(Color.parseColor("#BB86FC"), dp(20))); btnNext1.setTextColor(Color.BLACK);
        btnNext1.setOnClickListener(v -> {
            String n = nameInput.getText().toString().trim();
            if (n.isEmpty()) { Toast.makeText(this, "Please enter name", Toast.LENGTH_SHORT).show(); return; }
            tempName = n;
            if (isEditingProfile) { finishSetup(); } else { setupStep1.setVisibility(View.GONE); setupStep2.setVisibility(View.VISIBLE); }
        });
        setupStep1.addView(title1); setupStep1.addView(nameInput); setupStep1.addView(btnNext1);

        // Step 2: Age
        setupStep2 = new LinearLayout(this); setupStep2.setOrientation(LinearLayout.VERTICAL); setupStep2.setGravity(Gravity.CENTER); setupStep2.setVisibility(View.GONE);
        TextView title2 = new TextView(this); title2.setText("How old are you?"); title2.setTextColor(Color.WHITE); title2.setTextSize(24); title2.setPadding(0,0,0,dp(20));
        ageInput = new EditText(this); ageInput.setHint("Enter your age"); ageInput.setTextColor(Color.WHITE); ageInput.setHintTextColor(Color.GRAY); ageInput.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
        Button btnNext2 = new Button(this); btnNext2.setText("Save & Next"); btnNext2.setBackground(getRoundedBg(Color.parseColor("#BB86FC"), dp(20))); btnNext2.setTextColor(Color.BLACK);
        btnNext2.setOnClickListener(v -> {
            String a = ageInput.getText().toString().trim();
            if (a.isEmpty()) { Toast.makeText(this, "Please enter age", Toast.LENGTH_SHORT).show(); return; }
            tempAge = a;
            if (isEditingProfile) { finishSetup(); } else { setupStep2.setVisibility(View.GONE); setupStep3.setVisibility(View.VISIBLE); }
        });
        setupStep2.addView(title2); setupStep2.addView(ageInput); setupStep2.addView(btnNext2);

        // Step 3: Gender
        setupStep3 = new LinearLayout(this); setupStep3.setOrientation(LinearLayout.VERTICAL); setupStep3.setGravity(Gravity.CENTER); setupStep3.setVisibility(View.GONE);
        TextView title3 = new TextView(this); title3.setText("Select Gender"); title3.setTextColor(Color.WHITE); title3.setTextSize(24); title3.setPadding(0,0,0,dp(20));
        LinearLayout genderRow = new LinearLayout(this); genderRow.setOrientation(LinearLayout.HORIZONTAL); genderRow.setGravity(Gravity.CENTER);
        Button btnMale = new Button(this); btnMale.setText("Male"); btnMale.setBackground(getRoundedBg(Color.parseColor("#03DAC6"), dp(20))); btnMale.setTextColor(Color.BLACK);
        Button btnFemale = new Button(this); btnFemale.setText("Female"); btnFemale.setBackground(getRoundedBg(Color.parseColor("#CF6679"), dp(20))); btnFemale.setTextColor(Color.WHITE);
        
        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(dp(100), LinearLayout.LayoutParams.WRAP_CONTENT);
        btnParams.setMargins(dp(10), 0, dp(10), 0);
        btnMale.setLayoutParams(btnParams); btnFemale.setLayoutParams(btnParams);

        btnMale.setOnClickListener(v -> { tempGender = "Male"; if (isEditingProfile) { finishSetup(); } else { setupStep3.setVisibility(View.GONE); setupStep4.setVisibility(View.VISIBLE); } });
        btnFemale.setOnClickListener(v -> { tempGender = "Female"; if (isEditingProfile) { finishSetup(); } else { setupStep3.setVisibility(View.GONE); setupStep4.setVisibility(View.VISIBLE); } });
        genderRow.addView(btnMale); genderRow.addView(btnFemale);
        setupStep3.addView(title3); setupStep3.addView(genderRow);

        // Step 4: Profile Picture
        setupStep4 = new LinearLayout(this); setupStep4.setOrientation(LinearLayout.VERTICAL); setupStep4.setGravity(Gravity.CENTER); setupStep4.setVisibility(View.GONE);
        TextView title4 = new TextView(this); title4.setText("Profile Picture"); title4.setTextColor(Color.WHITE); title4.setTextSize(24); title4.setPadding(0,0,0,dp(20));
        avatarImage = new ImageView(this);
        LinearLayout.LayoutParams imgParams = new LinearLayout.LayoutParams(dp(120), dp(120));
        imgParams.setMargins(0,0,0,dp(20));
        avatarImage.setLayoutParams(imgParams);
        avatarImage.setBackground(getRoundedBg(Color.GRAY, dp(60)));
        
        LinearLayout photoBtnRow = new LinearLayout(this); photoBtnRow.setOrientation(LinearLayout.HORIZONTAL); photoBtnRow.setGravity(Gravity.CENTER);
        Button btnAddPhoto = new Button(this); btnAddPhoto.setText("Add Photo"); btnAddPhoto.setBackground(getRoundedBg(Color.parseColor("#3700B3"), dp(20))); btnAddPhoto.setTextColor(Color.WHITE);
        Button btnSkip = new Button(this); btnSkip.setText("Skip / Finish"); btnSkip.setBackground(getRoundedBg(Color.parseColor("#BB86FC"), dp(20))); btnSkip.setTextColor(Color.BLACK);
        btnSkip.setLayoutParams(btnParams);

        btnAddPhoto.setOnClickListener(v -> requestGalleryPermission());
        btnSkip.setOnClickListener(v -> finishSetup());
        photoBtnRow.addView(btnAddPhoto); photoBtnRow.addView(btnSkip);
        setupStep4.addView(title4); setupStep4.addView(avatarImage); setupStep4.addView(photoBtnRow);

        setupMenu.addView(setupStep1); setupMenu.addView(setupStep2); setupMenu.addView(setupStep3); setupMenu.addView(setupStep4);

        // ============================================
        // 🏠 MAIN MENU
        // ============================================
        mainMenu = new LinearLayout(this); mainMenu.setOrientation(LinearLayout.VERTICAL); mainMenu.setGravity(Gravity.CENTER);
        TextView title = new TextView(this); title.setText("Tic Tac Toe"); title.setTextColor(Color.WHITE); title.setTextSize(40); title.setPadding(0,0,0,dp(30));
        
        Button btnAI = new Button(this); btnAI.setText("Play vs AI"); btnAI.setBackground(getRoundedBg(Color.parseColor("#2A2A2A"), dp(20))); btnAI.setTextColor(Color.WHITE);
        btnAI.setOnClickListener(v -> { mainMenu.setVisibility(View.GONE); aiGameMenu.setVisibility(View.VISIBLE); resetAI(); });
        
        Button btnFriend = new Button(this); btnFriend.setText("Play with Friends"); btnFriend.setBackground(getRoundedBg(Color.parseColor("#2A2A2A"), dp(20))); btnFriend.setTextColor(Color.WHITE);
        btnFriend.setOnClickListener(v -> { mainMenu.setVisibility(View.GONE); friendMenu.setVisibility(View.VISIBLE); });
        
        Button btnSettings = new Button(this); btnSettings.setText("⚙️ Settings"); btnSettings.setBackground(getRoundedBg(Color.parseColor("#4CAF50"), dp(20))); btnSettings.setTextColor(Color.WHITE);
        btnSettings.setOnClickListener(v -> { mainMenu.setVisibility(View.GONE); settingsMenu.setVisibility(View.VISIBLE); });

        LinearLayout.LayoutParams menuBtnParams = new LinearLayout.LayoutParams(dp(200), LinearLayout.LayoutParams.WRAP_CONTENT);
        menuBtnParams.setMargins(0, 0, 0, dp(15));
        btnAI.setLayoutParams(menuBtnParams); btnFriend.setLayoutParams(menuBtnParams); btnSettings.setLayoutParams(menuBtnParams);
        
        mainMenu.addView(title); mainMenu.addView(btnAI); mainMenu.addView(btnFriend); mainMenu.addView(btnSettings);

        // ============================================
        // ⚙️ SETTINGS MENU
        // ============================================
        settingsMenu = new LinearLayout(this); settingsMenu.setOrientation(LinearLayout.VERTICAL); settingsMenu.setGravity(Gravity.CENTER); settingsMenu.setVisibility(View.GONE);
        TextView setTitle = new TextView(this); setTitle.setText("Settings"); setTitle.setTextColor(Color.WHITE); setTitle.setTextSize(30); setTitle.setPadding(0,0,0,dp(30));
        
        Button btnSetName = new Button(this); btnSetName.setText("Change Name");
        Button btnSetAge = new Button(this); btnSetAge.setText("Change Age");
        Button btnSetGender = new Button(this); btnSetGender.setText("Change Gender");
        Button btnSetPhoto = new Button(this); btnSetPhoto.setText("Change Profile Photo");
        Button btnSetBack = new Button(this); btnSetBack.setText("Back to Main Menu");
        
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
        aiStatus = new TextView(this); aiStatus.setText("Your Turn (X)"); aiStatus.setTextColor(Color.CYAN); aiStatus.setTextSize(20); aiStatus.setPadding(0,0,0,dp(20));
        GridLayout aiGrid = new GridLayout(this); aiGrid.setColumnCount(3); aiGrid.setRowCount(3);
        for (int i=0; i<9; i++) {
            aiButtons[i] = new Button(this); aiButtons[i].setTextSize(30); aiButtons[i].setBackgroundColor(Color.parseColor("#2A2A2A")); aiButtons[i].setTextColor(Color.WHITE);
            GridLayout.LayoutParams p = new GridLayout.LayoutParams(); p.width=dp(80); p.height=dp(80); p.setMargins(dp(2),dp(2),dp(2),dp(2));
            aiButtons[i].setLayoutParams(p); final int idx = i; aiButtons[i].setOnClickListener(v -> aiMove(idx)); aiGrid.addView(aiButtons[i]);
        }
        Button back1 = new Button(this); back1.setText("Back"); back1.setOnClickListener(v -> { aiGameMenu.setVisibility(View.GONE); mainMenu.setVisibility(View.VISIBLE); });
        aiGameMenu.addView(aiStatus); aiGameMenu.addView(aiGrid); aiGameMenu.addView(back1);

        // ============================================
        // 👥 FRIEND MENU
        // ============================================
        friendMenu = new LinearLayout(this); friendMenu.setOrientation(LinearLayout.VERTICAL); friendMenu.setGravity(Gravity.CENTER); friendMenu.setVisibility(View.GONE); friendMenu.setPadding(dp(20),0,dp(20),0);
        myIdText = new TextView(this); myIdText.setTextColor(Color.YELLOW); myIdText.setTextSize(18); myIdText.setPadding(0,0,0,dp(20)); myIdText.setGravity(Gravity.CENTER);
        
        friendInput = new EditText(this); friendInput.setHint("Enter Friend ID"); friendInput.setTextColor(Color.WHITE); friendInput.setHintTextColor(Color.GRAY);
        
        LinearLayout friendBtnRow = new LinearLayout(this); friendBtnRow.setOrientation(LinearLayout.HORIZONTAL);
        Button btnAdd = new Button(this); btnAdd.setText("Add/Invite"); btnAdd.setOnClickListener(v -> addFriend(friendInput.getText().toString()));
        Button btnChat = new Button(this); btnChat.setText("💬 Chat"); btnChat.setOnClickListener(v -> openChat(friendInput.getText().toString(), false));
        friendBtnRow.addView(btnAdd); friendBtnRow.addView(btnChat);
        
        friendStatus = new TextView(this); friendStatus.setTextColor(Color.GREEN); friendStatus.setPadding(0,dp(20),0,dp(20)); friendStatus.setGravity(Gravity.CENTER);
        Button btnRefresh = new Button(this); btnRefresh.setText("Check Notifications"); btnRefresh.setOnClickListener(v -> checkNotifications());
        Button back2 = new Button(this); back2.setText("Back"); back2.setOnClickListener(v -> { friendMenu.setVisibility(View.GONE); mainMenu.setVisibility(View.VISIBLE); });
        
        friendMenu.addView(myIdText); friendMenu.addView(friendInput); friendMenu.addView(friendBtnRow); friendMenu.addView(friendStatus); friendMenu.addView(btnRefresh); friendMenu.addView(back2);

        // ============================================
        // ⚔️ MULTIPLAYER GAME
        // ============================================
        gameMenu = new LinearLayout(this); gameMenu.setOrientation(LinearLayout.VERTICAL); gameMenu.setGravity(Gravity.CENTER); gameMenu.setVisibility(View.GONE);
        multiStatus = new TextView(this); multiStatus.setText("Waiting..."); multiStatus.setTextColor(Color.CYAN); multiStatus.setTextSize(20); multiStatus.setPadding(0,0,0,dp(20));
        GridLayout multiGrid = new GridLayout(this); multiGrid.setColumnCount(3); multiGrid.setRowCount(3);
        for (int i=0; i<9; i++) {
            multiButtons[i] = new Button(this); multiButtons[i].setTextSize(30); multiButtons[i].setBackgroundColor(Color.parseColor("#2A2A2A")); multiButtons[i].setTextColor(Color.WHITE);
            GridLayout.LayoutParams p = new GridLayout.LayoutParams(); p.width=dp(80); p.height=dp(80); p.setMargins(dp(2),dp(2),dp(2),dp(2));
            multiButtons[i].setLayoutParams(p); final int idx = i; multiButtons[i].setOnClickListener(v -> makeMultiMove(idx)); multiGrid.addView(multiButtons[i]);
        }
        LinearLayout gameBottomRow = new LinearLayout(this); gameBottomRow.setOrientation(LinearLayout.HORIZONTAL);
        Button btnGameChat = new Button(this); btnGameChat.setText("💬 Chat"); btnGameChat.setOnClickListener(v -> {
            if (chatPartnerId != null) openChat(chatPartnerId, true);
        });
        Button back3 = new Button(this); back3.setText("Leave"); back3.setOnClickListener(v -> { isMultiplayer=false; gameMenu.setVisibility(View.GONE); friendMenu.setVisibility(View.VISIBLE); });
        gameBottomRow.addView(btnGameChat); gameBottomRow.addView(back3);
        gameMenu.addView(multiStatus); gameMenu.addView(multiGrid); gameMenu.addView(gameBottomRow);

        // ============================================
        // 💬 CHAT MENU
        // ============================================
        chatMenu = new LinearLayout(this); chatMenu.setOrientation(LinearLayout.VERTICAL); chatMenu.setVisibility(View.GONE); chatMenu.setBackgroundColor(Color.parseColor("#121212"));
        TextView chatTitle = new TextView(this); chatTitle.setText("Chat"); chatTitle.setTextColor(Color.WHITE); chatTitle.setTextSize(20); chatTitle.setGravity(Gravity.CENTER); chatTitle.setPadding(0,dp(20),0,dp(20));
        chatScroll = new ScrollView(this);
        chatTextView = new TextView(this); chatTextView.setTextColor(Color.WHITE); chatTextView.setTextSize(16); chatTextView.setPadding(dp(10),dp(10),dp(10),dp(10));
        chatScroll.addView(chatTextView);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f);
        chatScroll.setLayoutParams(scrollParams);
        LinearLayout chatBottomRow = new LinearLayout(this); chatBottomRow.setOrientation(LinearLayout.HORIZONTAL);
        chatInput = new EditText(this); chatInput.setHint("Type message..."); chatInput.setTextColor(Color.WHITE); chatInput.setHintTextColor(Color.GRAY);
        LinearLayout.LayoutParams inputParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        chatInput.setLayoutParams(inputParams);
        Button btnSend = new Button(this); btnSend.setText("Send"); btnSend.setOnClickListener(v -> sendChatMessage());
        chatBottomRow.addView(chatInput); chatBottomRow.addView(btnSend);
        Button backChat = new Button(this); backChat.setText("Close Chat"); backChat.setOnClickListener(v -> {
            inChat = false; chatMenu.setVisibility(View.GONE);
            if (cameFromGame) gameMenu.setVisibility(View.VISIBLE);
            else friendMenu.setVisibility(View.VISIBLE);
        });
        chatMenu.addView(chatTitle); chatMenu.addView(chatScroll); chatMenu.addView(chatBottomRow); chatMenu.addView(backChat);

        root.addView(setupMenu); root.addView(mainMenu); root.addView(settingsMenu); root.addView(aiGameMenu); root.addView(friendMenu); root.addView(gameMenu); root.addView(chatMenu);
        setContentView(root);
    }

    private void requestGalleryPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_MEDIA_IMAGES}, 72);
            } else {
                openGallery();
            }
        } else {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 72);
            } else {
                openGallery();
            }
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
            Toast.makeText(this, "Permission denied. Cannot access gallery.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 71 && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            try {
                java.io.InputStream is = getContentResolver().openInputStream(uri);
                android.graphics.Bitmap bmp = android.graphics.BitmapFactory.decodeStream(is);
                int maxSize = 150;
                int w = bmp.getWidth(), h = bmp.getHeight();
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
        
        myIdText.setText("Name: " + tempName + "\nID: " + myId);
        registerUser(tempName, tempGender, tempAge, avatarBase64);
        
        if (isEditingProfile) {
            settingsMenu.setVisibility(View.VISIBLE);
            isEditingProfile = false;
        } else {
            mainMenu.setVisibility(View.VISIBLE);
        }
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
            if (res.equals("null")) { friendStatus.setText("User not found!"); return; }
            try {
                JSONObject o = new JSONObject(res);
                String fName = o.optString("name", "Player_" + fId);
                friendStatus.setText("Request sent to " + fName);
            } catch(Exception e) { friendStatus.setText("Request sent to " + fId); }
        });
        firebasePut("friend_reqs/" + fId + "/" + myId, "{\"from\":\"" + myId + "\"}");
        inviteFriend(fId);
    }

    private void checkNotifications() {
        friendStatus.setText("Checking...");
        firebaseGet("friend_reqs/" + myId, res -> {
            if (!res.equals("null")) {
                try {
                    JSONObject o = new JSONObject(res);
                    Iterator<String> keys = o.keys();
                    if (keys.hasNext()) {
                        String sender = keys.next();
                        firebasePut("friends/" + myId + "/" + sender, "true");
                        firebasePut("friends/" + sender + "/" + myId, "true");
                        friendStatus.setText("New friend added: " + sender);
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
        chatTextView.setText("Loading..."); chatInput.setText("");
        
        if (chatPoller != null) handler.removeCallbacks(chatPoller);
        chatPoller = new Runnable() {
            @Override public void run() {
                if (!inChat) return;
                String chatId = getChatId(myId, chatPartnerId);
                firebaseGet("chats/" + chatId, res -> {
                    if (res.equals("null")) { chatTextView.setText("No messages yet. Say hi!"); return; }
                    try {
                        JSONObject o = new JSONObject(res);
                        List<Long> times = new ArrayList<>();
                        for (Iterator<String> it = o.keys(); it.hasNext();) times.add(Long.parseLong(it.next()));
                        Collections.sort(times);
                        StringBuilder sb = new StringBuilder();
                        for (Long t : times) {
                            JSONObject msg = o.getJSONObject(String.valueOf(t));
                            String sender = msg.getString("sender"); String text = msg.getString("text");
                            sb.append(sender.equals(myId) ? "Me: " : chatPartnerId + ": ").append(text).append("\n\n");
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
        aiStatus.setText("Your Turn (X)");
    }
    private void aiMove(int idx) {
        if (aiGameOver || !aiBoard[idx].equals("")) return;
        aiBoard[idx]="X"; aiButtons[idx].setText("X"); aiButtons[idx].setTextColor(Color.CYAN);
        if (checkWin(aiBoard, "X")) { aiStatus.setText("You Win!"); aiGameOver=true; return; }
        if (isFull(aiBoard)) { aiStatus.setText("Draw!"); aiGameOver=true; return; }
        aiStatus.setText("AI Thinking...");
        handler.postDelayed(() -> {
            int move = findBestMove();
            if (move != -1) { aiBoard[move]="O"; aiButtons[move].setText("O"); aiButtons[move].setTextColor(Color.RED); }
            if (checkWin(aiBoard, "O")) { aiStatus.setText("AI Wins!"); aiGameOver=true; }
            else if (isFull(aiBoard)) { aiStatus.setText("Draw!"); aiGameOver=true; }
            else { aiStatus.setText("Your Turn (X)"); }
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
                            if (checkWinStr(boardStr, "X")) multiStatus.setText("X Wins!");
                            else if (checkWinStr(boardStr, "O")) multiStatus.setText("O Wins!");
                            else multiStatus.setText(turn.equals(mySymbol) ? "Your Turn!" : "Opponent's Turn...");
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