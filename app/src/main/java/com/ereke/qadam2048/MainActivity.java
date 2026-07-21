package com.ereke.qadam2048;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private MainView view;
    private MainView mView;
    private TextView currentComboText = null;
    private ImageButton btnVibration;
    private boolean isSettingsOpen = false; // Флаг: открыты ли настройки
    private android.widget.TextView tilesCounter;
    private String currentActiveMode = "CLASSIC"; // По умолчанию
    private boolean isVibrationEnabled = true; // По умолчанию включена

    private FrameLayout rootLayout;
    private MainView gameView;
    private ImageButton btnTheme;
    private ImageButton btnSettings;

    private ImageButton btnShop;
    private boolean isChallengeMode = false; // По умолчанию обычный режим

    // Добавь это в MainActivity
    public boolean isChallengeModeActive() {
        return isChallengeMode;
    }
    private android.os.CountDownTimer challengeTimer;
    private android.widget.ProgressBar timerBar;
    private View settingsView;
    private AdView adView;
    private DatabaseReference mDatabase;

    private FirebaseAnalytics mFirebaseAnalytics;
    private FirebaseRemoteConfig mFirebaseRemoteConfig;
    public static int undoCount = 5;

    public boolean isHammerMode = false;
    private AlertDialog shopDialog;
    private static final int MAX_UNDO = 5;


    private TextView tvShopCoinBalance;
    private TextView finalScoreText;
    private int bgColor;
    private int cheatCount = 3;
    private final int MAX_CHEAT = 3;
    private int textColor;
    private RewardedAd rewardedAd;
    private final String REWARD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917";

    private View gameOverOverlay;
    private ImageButton btnLeaderboard;
    boolean settingsOpened = false;
    private FrameLayout adContainerView;


    // ====== SETTINGS PANEL ======
    private FrameLayout settingsDim;
    private TextView bestScoreText;
    private boolean isTimeAttackMode = false;
    private LinearLayout settingsPanel;

    private ImageButton btnToggleTheme;

    private SwitchCompat soundSwitch;
    private SwitchCompat themeSwitch;

    private boolean isDarkMode;
    private Button btnAd;

    private int heartsCount = 3; // Счетчик жизней
    private LinearLayout heartsLayout;

    private SeekBar soundSeek;
    private SeekBar vibSeek;
    private boolean settingsVisible = false;
    private boolean settingsOpen = false;


    // Keys
    public static final String WIDTH = "width";
    public static final String HEIGHT = "height";
    public static final String SCORE = "score";
    public static final String HIGH_SCORE = "high_score";
    public static final String UNDO_SCORE = "undo_score";
    public static final String CAN_UNDO = "can_undo";
    public static final String UNDO_GRID = "undo_grid";
    public static final String GAME_STATE = "game_state";
    public static final String UNDO_GAME_STATE = "undo_game_state";
    private Animation clickAnim;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 0. Splash Screen (обязательно первым)
        androidx.core.splashscreen.SplashScreen splashScreen =
                androidx.core.splashscreen.SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);

        hideSystemUI();

        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);

        // --- СИСТЕМНЫЕ НАСТРОЙКИ ---
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

        // Фикс для темной темы: запрещаем системе принудительно перекрашивать наши вьюхи
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            getWindow().getDecorView().setForceDarkAllowed(false);
        }

        // --- ИНИЦИАЛИЗАЦИЯ АНИМАЦИИ ---
        clickAnim = AnimationUtils.loadAnimation(this, R.anim.button_click);

        // 1. Настройка SharedPreferences
        SharedPreferences prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(this);
        loadUndoData();
        loadCheatData();
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);
        boolean isSoundEnabled = prefs.getBoolean("sound_enabled", true);
        boolean isCloudEnabled = prefs.getBoolean("cloud_enabled", false);
        isVibrationEnabled = prefs.getBoolean("vibration_enabled", true);

        if (isDarkMode) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        }


        // 2. Создаем rootLayout (главный контейнер)
        rootLayout = new FrameLayout(this);
        rootLayout.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        int bgColor = androidx.core.content.ContextCompat.getColor(this,
                isDarkMode ? R.color.background_dark : R.color.background_light);
        rootLayout.setBackgroundColor(bgColor);

        // --- ЛОГИКА ВЫБОРА РАЗМЕРА СЕТКИ (АНТИ-БАГ) ---
        int savedSize = prefs.getInt("saved_grid_size", 0);
        int intentSize = getIntent().getIntExtra("GRID_SIZE", 4);

        // Финальный размер: приоритет у интента из меню, если его нет — берем сохранение
        final int finalGridSize = (getIntent().hasExtra("GRID_SIZE")) ? intentSize : (savedSize != 0 ? savedSize : 4);

        // Загружаем состояние челенджа (тайм-аттак)
        isChallengeMode = prefs.getBoolean("saved_is_challenge", false);

        // 3. Инициализируем игровую область (MainView)
        gameView = new MainView(this);
        mView = gameView;
        if (gameView.game != null) {
            gameView.game.numSquaresX = finalGridSize;
            gameView.game.numSquaresY = finalGridSize;
            gameView.game.grid = new Grid(finalGridSize, finalGridSize);
        }
        gameView.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        rootLayout.addView(gameView);

        // --- КНОПКИ УПРАВЛЕНИЯ (UI) ---

        // Кнопка Темы (Солнце/Луна)
        btnTheme = new ImageButton(this);
        btnTheme.setImageResource(isDarkMode ? R.drawable.ic_sun : R.drawable.ic_moon);
        btnTheme.setBackgroundResource(R.drawable.round_button_bg);
        btnTheme.setPadding(dp(12), dp(12), dp(12), dp(12));
        btnTheme.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        FrameLayout.LayoutParams themeParams = new FrameLayout.LayoutParams(dp(56), dp(56), Gravity.TOP | Gravity.END);
        themeParams.topMargin = dp(20);
        themeParams.rightMargin = dp(20);
        rootLayout.addView(btnTheme, themeParams);

        // Кнопка Вибрации
        btnVibration = new ImageButton(this);
        btnVibration.setImageResource(isVibrationEnabled ? R.drawable.ic_vibration_on : R.drawable.ic_vibration_off);
        btnVibration.setBackgroundResource(R.drawable.round_button_bg);
        btnVibration.setPadding(dp(12), dp(12), dp(12), dp(12));
        btnVibration.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        FrameLayout.LayoutParams vibParams = new FrameLayout.LayoutParams(dp(56), dp(56), Gravity.TOP | Gravity.END);
        vibParams.rightMargin = dp(20);
        vibParams.topMargin = dp(86);
        rootLayout.addView(btnVibration, vibParams);

        // Кнопка Настроек (Шестеренка)
        btnSettings = new ImageButton(this);
        btnSettings.setImageResource(R.drawable.ic_settings);
        btnSettings.setBackgroundResource(R.drawable.round_button_bg);
        btnSettings.setPadding(dp(12), dp(12), dp(12), dp(12));
        btnSettings.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        FrameLayout.LayoutParams settingsParams = new FrameLayout.LayoutParams(dp(56), dp(56), Gravity.TOP | Gravity.START);
        settingsParams.topMargin = dp(20);
        settingsParams.leftMargin = dp(20);
        rootLayout.addView(btnSettings, settingsParams);

        // Кнопка Лидерборда (Кубок)
        btnLeaderboard = new ImageButton(this);
        btnLeaderboard.setImageResource(R.drawable.ic_leaderboard);
        btnLeaderboard.setBackgroundResource(R.drawable.round_button_bg);
        btnLeaderboard.setPadding(dp(12), dp(12), dp(12), dp(12));
        btnLeaderboard.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        FrameLayout.LayoutParams lbParams = new FrameLayout.LayoutParams(dp(56), dp(56), Gravity.TOP | Gravity.START);
        lbParams.leftMargin = dp(20);
        lbParams.topMargin = dp(86);
        rootLayout.addView(btnLeaderboard, lbParams);

        // Кнопка Испытаний (Молния)
        ImageButton btnChallenges = new ImageButton(this);
        btnChallenges.setImageResource(R.drawable.ic_lightning);
        btnChallenges.setBackgroundResource(R.drawable.round_button_bg);
        btnChallenges.setPadding(dp(12), dp(12), dp(12), dp(12));
        btnChallenges.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        FrameLayout.LayoutParams chalParams = new FrameLayout.LayoutParams(dp(56), dp(56), Gravity.TOP | Gravity.START);
        chalParams.leftMargin = dp(20);
        chalParams.topMargin = dp(152);
        rootLayout.addView(btnChallenges, chalParams);

        setupHeartsUI();

        // Кнопка Магазина (Корзина)
        btnShop = new ImageButton(this);
        btnShop.setImageResource(R.drawable.ic_shop);
        btnShop.setBackgroundResource(R.drawable.round_button_bg);
        btnShop.setPadding(dp(12), dp(12), dp(12), dp(12));
        btnShop.setScaleType(ImageView.ScaleType.CENTER_INSIDE);

        FrameLayout.LayoutParams shopParams = new FrameLayout.LayoutParams(dp(56), dp(56), Gravity.TOP | Gravity.START);
        shopParams.topMargin = dp(20);
        shopParams.leftMargin = dp(86);
        rootLayout.addView(btnShop, shopParams);

        // --- СЛУШАТЕЛИ СОБЫТИЙ (LISTENERS) ---

        btnShop.setOnClickListener(v -> {
            v.startAnimation(clickAnim);
            triggerVibration(15);
            showShopDialog();
        });

        btnTheme.setOnClickListener(v -> {
            v.startAnimation(clickAnim);
            triggerVibration(15);

            SharedPreferences p = android.preference.PreferenceManager.getDefaultSharedPreferences(this);
            boolean currentDark = p.getBoolean("dark_mode", false);
            boolean newDark = !currentDark;

            p.edit().putBoolean("dark_mode", newDark).apply();

            // Меняем системную тему, чтобы ContextCompat переключил папки ресурсов на лету
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                    newDark ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
            );

            applyAppTheme(newDark);

            if (settingsView != null) {
                SwitchCompat themeSwitch = settingsView.findViewById(R.id.switch_dark_mode);
                if (themeSwitch != null) {
                    themeSwitch.setOnCheckedChangeListener(null);
                    themeSwitch.setChecked(newDark);

                    themeSwitch.setOnCheckedChangeListener((btn, checked) -> {
                        btn.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
                        p.edit().putBoolean("dark_mode", checked).apply();

                        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                                checked ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
                        );

                        applyAppTheme(checked);
                    });
                }
            }
        });

        btnVibration.setOnClickListener(v -> {
            v.startAnimation(clickAnim);
            isVibrationEnabled = !isVibrationEnabled;
            btnVibration.setImageResource(isVibrationEnabled ? R.drawable.ic_vibration_on : R.drawable.ic_vibration_off);
            prefs.edit().putBoolean("vibration_enabled", isVibrationEnabled).apply();
            if (isVibrationEnabled) triggerVibration(25);
        });

        btnChallenges.setOnClickListener(v -> {
            v.startAnimation(clickAnim);
            triggerVibration(15);
            showChallengesDialog();
        });

        btnLeaderboard.setOnClickListener(v -> {
            v.startAnimation(clickAnim);
            triggerVibration(15);
            showLeaderboard();
        });

        btnSettings.setOnClickListener(v -> {
            v.startAnimation(clickAnim);
            triggerVibration(15);
            isSettingsOpen = true;
            positionTimerUnderLogo();
            settingsView.setVisibility(View.VISIBLE);
            settingsView.setAlpha(0f);
            settingsView.animate().alpha(1f).setDuration(200).start();
        });

        // --- ПАНЕЛИ И ОВЕРЛЕИ ---
        settingsView = getLayoutInflater().inflate(R.layout.view_settings_panel, rootLayout, false);
        settingsView.setVisibility(View.GONE);
        rootLayout.addView(settingsView);

        createGameOverOverlay();

        // Передаем актуальное значение isDarkMode, считанное вверху метода
        initSettingsLogic(prefs, isSoundEnabled, isDarkMode, isCloudEnabled);
        initAds();

        // --- FIREBASE ---
        mDatabase = FirebaseDatabase.getInstance("https://first-project-2easy-default-rtdb.europe-west1.firebasedatabase.app/").getReference();

        // --- ЗАГРУЗКА ДАННЫХ И СТАРТ ИГРЫ (ФИКС БАГА) ---
        gameView.post(() -> {
            if (gameView.game != null) {
                String currentScoreKey = "high_score_" + finalGridSize + "x" + finalGridSize;
                gameView.game.highScore = prefs.getLong(currentScoreKey, 0);

                gameView.game.aGrid = new AnimationGrid(finalGridSize, finalGridSize);

                if (prefs.getBoolean("cloud_enabled", false)) loadScoreFromCloud();

                int sizeInStorage = prefs.getInt("saved_grid_size", 0);

                if (sizeInStorage == finalGridSize) {
                    load();
                } else {
                    gameView.game.newGame();
                }

                if (isChallengeMode) {
                    startChallengeTimer();
                    positionTimerUnderLogo();
                }

                boolean isAnyTile = false;
                if (gameView.game.grid != null && gameView.game.grid.field != null) {
                    checkLoop:
                    for (int x = 0; x < finalGridSize; x++) {
                        for (int y = 0; y < finalGridSize; y++) {
                            if (gameView.game.grid.field[x][y] != null) {
                                isAnyTile = true;
                                break checkLoop;
                            }
                        }
                    }
                }

                if (!isAnyTile) {
                    gameView.game.newGame();
                }

                gameView.invalidate();
            }
        });

        applyAppTheme(isDarkMode);

        // Финальная установка контента
        setContentView(rootLayout);
    }

    // Вынес логику настроек для чистоты
    // 1. Сначала добавь этот метод для плавной анимации
    private void showSettingsPanel(boolean show) {
        if (show) {
            settingsView.setVisibility(View.VISIBLE);
            settingsView.setAlpha(0f);
            settingsView.setScaleX(0.7f);
            settingsView.setScaleY(0.7f);
            settingsView.animate()
                    .alpha(1f).scaleX(1f).scaleY(1f)
                    .setDuration(300)
                    .setInterpolator(new android.view.animation.OvershootInterpolator(1.2f))
                    .start();
        } else {
            settingsView.animate()
                    .alpha(0f).scaleX(0.7f).scaleY(0.7f)
                    .setDuration(200)
                    .withEndAction(() -> settingsView.setVisibility(View.GONE))
                    .start();
        }
    }

    private void hideSystemUI() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            // Для новых версий Android (API 30+)
            final WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            // Для старых версий
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemUI(); // Возвращаем фуллскрин при возврате в игру
        }
    }

    // 2. Сама инициализация логики
    private void initSettingsLogic(SharedPreferences prefs, boolean isSound, boolean isDark, boolean isCloud) {
        SwitchCompat soundSwitch = settingsView.findViewById(R.id.switch_sound);
        soundSwitch.setChecked(isSound);
        soundSwitch.setOnCheckedChangeListener((b, checked) -> {
            b.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
            prefs.edit().putBoolean("sound_enabled", checked).apply();
        });

        SwitchCompat themeSwitch = settingsView.findViewById(R.id.switch_dark_mode);
        themeSwitch.setChecked(isDark);
        themeSwitch.setOnCheckedChangeListener((b, checked) -> {
            b.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK);
            prefs.edit().putBoolean("dark_mode", checked).apply();
            applyAppTheme(checked);
            updateHeartsDisplay();
            if (btnTheme != null) {
                btnTheme.setImageResource(checked ? R.drawable.ic_sun : R.drawable.ic_moon);
            }
        });

        SwitchCompat cloudSwitch = settingsView.findViewById(R.id.switch_cloud);
        cloudSwitch.setChecked(isCloud);
        cloudSwitch.setOnCheckedChangeListener((b, checked) -> {
            b.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
            prefs.edit().putBoolean("cloud_enabled", checked).apply();
        });

        View.OnClickListener settingsBtnListener = v -> {
            if (clickAnim != null) v.startAnimation(clickAnim);
            int id = v.getId();
            if (id == R.id.btn_tutorial) {
                showInteractiveTutorial();
            } else if (id == R.id.btn_achievements) {
                showLeaderboard();
            } else if (id == R.id.btn_privacy) {
                showPrivacyPolicy();
            } else if (id == R.id.btn_rate) {
                rateApp();
            } else if (id == R.id.btnClose || id == R.id.cardClose) {
                // ЗАКРЫТИЕ: Сбрасываем флаг и возвращаем полоску
                isSettingsOpen = false;
                positionTimerUnderLogo();
                showSettingsPanel(false);
            }
        };

        settingsView.findViewById(R.id.btn_tutorial).setOnClickListener(settingsBtnListener);
        settingsView.findViewById(R.id.btn_achievements).setOnClickListener(settingsBtnListener);
        settingsView.findViewById(R.id.btn_privacy).setOnClickListener(settingsBtnListener);
        View btnRate = settingsView.findViewById(R.id.btn_rate);
        btnRate.setOnClickListener(settingsBtnListener);
        Animation pulse = AnimationUtils.loadAnimation(this, R.anim.pulse_anim);
        btnRate.startAnimation(pulse);
        settingsView.findViewById(R.id.btnClose).setOnClickListener(settingsBtnListener);

        int cardId = getResources().getIdentifier("cardClose", "id", getPackageName());
        if (cardId != 0) {
            View cardClose = settingsView.findViewById(cardId);
            if (cardClose != null) cardClose.setOnClickListener(settingsBtnListener);
        }

        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                if (clickAnim != null) v.startAnimation(clickAnim);
                // ОТКРЫТИЕ: Ставим флаг и прячем полоску
                isSettingsOpen = true;
                positionTimerUnderLogo();
                showSettingsPanel(true);
            });
        }
    }

    // Вспомогательный метод для оценки приложения
    private void rateApp() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + getPackageName()));
            startActivity(intent);
        } catch (android.content.ActivityNotFoundException e) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName()));
            startActivity(intent);
        }
    }

    private void initAds() {
        // 1. Инициализация SDK (теперь с колбэком)
        MobileAds.initialize(this, initializationStatus -> {
            // Как только SDK готово, загружаем рекламу с вознаграждением
            loadRewardedAd();
        });

        // 2. Настройка существующего баннера
        adView = new AdView(this);
        adView.setAdUnitId("ca-app-pub-3940256099942544/6300978111");
        adView.setAdSize(getAdSize());
        FrameLayout.LayoutParams adParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
        );
        rootLayout.addView(adView, adParams);
        adView.loadAd(new AdRequest.Builder().build());
    }

    private AdSize getAdSize() {
        // Определяем ширину экрана в пикселях
        android.view.Display display = getWindowManager().getDefaultDisplay();
        android.util.DisplayMetrics outMetrics = new android.util.DisplayMetrics();
        display.getMetrics(outMetrics);

        float widthPixels = outMetrics.widthPixels;
        float density = outMetrics.density;

        // Считаем ширину в DP
        int adWidth = (int) (widthPixels / density);

        // Возвращаем размер баннера на всю доступную ширину
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(this, adWidth);
    }
    private void loadRewardedAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(this, REWARD_UNIT_ID, adRequest, new RewardedAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull RewardedAd ad) {
                rewardedAd = ad;
                Log.d("AdMob", "Реклама загружена успешно.");

                // Устанавливаем слушатель событий (закрытие, ошибки показа)
                rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        rewardedAd = null;
                        loadRewardedAd(); // Сразу грузим следующую
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                        rewardedAd = null;
                        Log.e("AdMob", "Ошибка показа: " + adError.getMessage());
                    }
                });
            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                rewardedAd = null;
                Log.e("AdMob", "Ошибка загрузки: " + loadAdError.getMessage());
            }
        });
    }
    public void showRewardAdWithLogic() {
        if (rewardedAd != null) {
            rewardedAd.show(this, rewardItem -> {
                // ПОЛУЧЕНИЕ НАГРАДЫ
                Log.d("AdMob", "Пользователь досмотрел рекламу.");
                applySecondChance();
            });
        } else {
            Toast.makeText(this, "Реклама еще не готова...", Toast.LENGTH_SHORT).show();
            loadRewardedAd(); // Пытаемся подгрузить
        }
    }
    private void applyAppTheme(boolean isDark) {
        // 1. Фон главного слоя (убирает белый ожог глаз)
        int bgColor = ContextCompat.getColor(this, isDark ? R.color.background_dark : R.color.background_light);
        rootLayout.setBackgroundColor(bgColor);

        // 2. Иконка кнопки на главном экране
        btnTheme.setImageResource(isDark ? R.drawable.ic_sun : R.drawable.ic_moon);

        // 3. Сама игра (плитки, текст, фон внутри View)
        gameView.applyTheme(isDark);
    }


    private int dp(int px) {
        return (int) (px * getResources().getDisplayMetrics().density);
    }
    private void updateRootBackground(boolean dark) {
        int colorRes = dark ? R.color.background_dark : R.color.background_light;
        rootLayout.setBackgroundColor(ContextCompat.getColor(this, colorRes));
    }

    private void createGameOverOverlay() {
        Typeface fontBold = Typeface.createFromAsset(getAssets(), "fonts/montserrat_bold.ttf");
        Typeface fontSemiBold = Typeface.createFromAsset(getAssets(), "fonts/montserrat_semibold.ttf");

        LinearLayout overlay = new LinearLayout(this);
        overlay.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        overlay.setBackgroundColor(Color.parseColor("#F2FAF8EF"));
        overlay.setGravity(Gravity.CENTER);
        overlay.setOrientation(LinearLayout.VERTICAL);
        overlay.setVisibility(View.GONE);
        overlay.setClickable(true);
        overlay.setFocusable(true);
        gameOverOverlay = overlay;

        // 1. Заголовок
        TextView title = new TextView(this);
        title.setText("GAME OVER");
        title.setTypeface(fontBold);
        title.setTextSize(44);
        title.setTextColor(Color.parseColor("#776E65"));
        title.setGravity(Gravity.CENTER);
        overlay.addView(title);

        // 2. Текущий счет
        finalScoreText = new TextView(this);
        finalScoreText.setTypeface(fontSemiBold);
        finalScoreText.setTextSize(32);
        finalScoreText.setPadding(0, dp(15), 0, 0);
        finalScoreText.setGravity(Gravity.CENTER);
        finalScoreText.setTextColor(Color.parseColor("#8F7A66"));
        overlay.addView(finalScoreText);

        // 3. Лучший счет
        bestScoreText = new TextView(this);
        bestScoreText.setTypeface(fontSemiBold);
        bestScoreText.setTextSize(18);
        bestScoreText.setPadding(0, dp(5), 0, dp(40)); // Увеличил отступ до кнопок
        bestScoreText.setTextColor(Color.parseColor("#776E65"));
        bestScoreText.setGravity(Gravity.CENTER);
        overlay.addView(bestScoreText);

        // 4. Кнопка TRY AGAIN (Обычный рестарт)
        Button btnTryAgain = new Button(this);
        btnTryAgain.setText("TRY AGAIN");
        btnTryAgain.setTypeface(fontBold);
        btnTryAgain.setTextColor(Color.WHITE);
        btnTryAgain.setBackgroundResource(R.drawable.round_button_bg);
        btnTryAgain.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#8F7A66")));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(240), dp(60));
        lp.setMargins(0, 0, 0, dp(15)); // Чтобы кнопки не слипались
        btnTryAgain.setLayoutParams(lp);
        btnTryAgain.setOnClickListener(v -> {
            hideGameOver();

            // 1. Сбрасываем игру
            gameView.game.newGame();
            gameView.game.canUndo = false;
            gameView.createOverlays();

            heartsCount = 3;
            updateHeartsDisplay();

            // 2. ПРОВЕРКА РЕЖИМА
            if ("TIME ATTACK".equals(currentActiveMode)) {
                isChallengeMode = true;
                isTimeAttackMode = true;

                if (timerBar != null) {
                    // ВАЖНО: Сначала делаем его видимым!
                    timerBar.setVisibility(View.VISIBLE);
                    timerBar.setAlpha(1.0f);

                    if (tilesCounter != null) {
                        tilesCounter.setVisibility(View.VISIBLE);
                    }

                    // ВАЖНО: Пересчитываем его позицию, чтобы он не улетел в 0,0
                    positionTimerUnderLogo();

                    // Запускаем таймер с небольшой задержкой, чтобы UI успел прогрузиться
                    timerBar.postDelayed(() -> {
                        startChallengeTimer();
                    }, 100);
                }
            } else {
                // Если это классика
                isChallengeMode = false;
                stopAndHideTimer();
            }

            gameView.invalidate();
        });
        overlay.addView(btnTryAgain);

        // 5. Кнопка SECOND CHANCE (С рекламой)
        Button btnAd = new Button(this);
        btnAd.setText("GET SECOND CHANCE");
        btnAd.setTypeface(fontBold);
        btnAd.setTextColor(Color.WHITE);
        btnAd.setBackgroundResource(R.drawable.round_button_bg);
        btnAd.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#EDC22E")));

        LinearLayout.LayoutParams lpAd = new LinearLayout.LayoutParams(dp(240), dp(60));
        btnAd.setLayoutParams(lpAd);
        // В методе, где создается оверлей (например, createGameOverOverlay)
        btnAd.setOnClickListener(v -> {
            if (heartsCount > 0) {
                // Если жизни есть — только тогда запускаем рекламный движок
                showRewardAdWithLogic();
            } else {
                // Если жизней нет — просто ругаемся и ничего не показываем
                Toast.makeText(this, "У вас больше нет сердец!", Toast.LENGTH_SHORT).show();
            }
        });
        overlay.addView(btnAd);

        rootLayout.addView(gameOverOverlay);
    }
    private Typeface getFont(String fileName) {
        return Typeface.createFromAsset(getAssets(), "fonts/" + fileName + ".ttf");
    }
    public void shakeGame() {
        gameView.animate()
                .translationX(20f).setDuration(50)
                .withEndAction(() -> gameView.animate().translationX(-20f).setDuration(50)
                        .withEndAction(() -> gameView.animate().translationX(0f).setDuration(50).start())
                        .start()).start();
    }
    public void onGameOver() {
        // 1. ПЕРВООЧЕРЕДНАЯ ОСТАНОВКА (чтобы игра не продолжалась в фоне)
        if (challengeTimer != null) {
            challengeTimer.cancel();
            challengeTimer = null;
        }
        stopAndHideTimer();

        runOnUiThread(() -> {
            // 2. Убираем таймбар с анимацией затухания
            if (timerBar != null) {
                timerBar.animate()
                        .alpha(0f)
                        .setDuration(300)
                        .withEndAction(() -> {
                            timerBar.setVisibility(View.GONE);
                            // Скрываем и счетчик тоже
                            if (tilesCounter != null) tilesCounter.setVisibility(View.GONE);
                        })
                        .start();
            }

            // Внутри onGameOver в runOnUiThread
            // Внутри runOnUiThread в onGameOver()
            // Внутри runOnUiThread в onGameOver()
            if (btnAd != null) {
                if (heartsCount > 0) {
                    // Шанс есть!
                    btnAd.setText("SECOND CHANCE (" + heartsCount + " ❤️)");
                    btnAd.setEnabled(true); // Кнопка активна
                    btnAd.setAlpha(1.0f);   // Яркая
                    btnAd.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#EDC22E"))); // Золотая
                } else {
                    // Шансов нет!
                    btnAd.setText("OUT OF HEARTS");
                    btnAd.setEnabled(false);
                    btnAd.setAlpha(0.3f);    // Почти прозрачная
                    btnAd.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY)); // Серая

                    // btnAd.setVisibility(View.GONE);
                }
            }

            // Проверяем, не показан ли уже оверлей, чтобы не запускать анимацию дважды
            if (gameOverOverlay != null && gameOverOverlay.getVisibility() != View.VISIBLE) {

                // 3. Эффект жесткой тряски экрана (Feedback проигрыша)
                gameView.animate()
                        .translationX(20f).setDuration(50)
                        .withEndAction(() -> gameView.animate().translationX(-20f).setDuration(50)
                                .withEndAction(() -> gameView.animate().translationX(0f).setDuration(50).start())
                                .start()).start();

                // 4. Логика работы с рекордами
                long currentScore = gameView.game.score;
                SharedPreferences prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(this);


                // Получаем ник игрока из памяти
                String savedName = getSharedPreferences("ereke_prefs", MODE_PRIVATE).getString("user_name", null);
                // Оставляем только это:
                String currentMode = gameView.game.grid.field.length + "x" + gameView.game.grid.field.length;
                String scoreKey = "high_score_" + currentMode;
                long savedBest = prefs.getLong(scoreKey, 0); // Теперь это единственное объявление
                if (currentScore >= savedBest && currentScore > 0) {
                    prefs.edit().putLong(scoreKey, currentScore).apply();
                    gameView.game.highScore = currentScore;

                    if (savedName == null) {
                        showNameInputDialog(currentScore, currentMode);
                    } else {
                        updateLeaderboardWithName(savedName);
                    }

                    finalScoreText.setText("NEW RECORD\n" + currentScore);
                    finalScoreText.setTextColor(Color.parseColor("#EDC22E"));
                } else {
                    // ОБЫЧНЫЙ СЧЕТ
                    if (savedName != null) {
                        // ИСПРАВЛЕНО: Добавлен currentMode
                        updateLeaderboardWithName(savedName);
                    }

                    finalScoreText.setText("SCORE\n" + currentScore);
                    finalScoreText.setTextColor(Color.parseColor("#776E65"));
                }

                // Обновляем текст Best Score в оверлее через тег
                TextView bestView = gameOverOverlay.findViewWithTag("BEST_SCORE_TAG");
                if (bestView != null) {
                    bestView.setText("BEST: " + Math.max(currentScore, savedBest));
                }

                // 5. Красивое появление Game Over меню (Overshoot эффект)
                gameOverOverlay.setVisibility(View.VISIBLE);
                gameOverOverlay.setAlpha(0f);
                gameOverOverlay.setScaleX(0.7f);
                gameOverOverlay.setScaleY(0.7f);

                gameOverOverlay.animate()
                        .alpha(1f)
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(600)
                        .setInterpolator(new android.view.animation.OvershootInterpolator(1.2f))
                        .start();
            }
        });
    }
    private void launchConfetti() {
        final ViewGroup root = (ViewGroup) getWindow().getDecorView().getRootView();
        final int[] colors = {Color.parseColor("#EDC22E"), Color.parseColor("#F2B179"),
                Color.parseColor("#F59563"), Color.parseColor("#F67C5F")};
        final java.util.Random random = new java.util.Random();

        for (int i = 0; i < 50; i++) {
            final ImageView piece = new ImageView(this);
            piece.setImageResource(R.drawable.confetti_piece);
            piece.setColorFilter(colors[random.nextInt(colors.length)]);

            // Начальная позиция — центр экрана
            int startX = root.getWidth() / 2;
            int startY = root.getHeight() / 2;
            piece.setX(startX);
            piece.setY(startY);
            root.addView(piece);

            // Случайное направление разлета
            float finalX = random.nextFloat() * root.getWidth();
            float finalY = random.nextFloat() * root.getHeight();
            float rotation = random.nextFloat() * 720;

            piece.animate()
                    .translationX(finalX)
                    .translationY(finalY)
                    .rotation(rotation)
                    .alpha(0f)
                    .setDuration(1500 + random.nextInt(1000))
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .withEndAction(() -> root.removeView(piece))
                    .start();
        }
    }
    private void hideGameOver() {
        gameOverOverlay.animate()
                .alpha(0f)
                .scaleX(1.1f) // Немного увеличиваем при исчезновении для эффекта "улета"
                .scaleY(1.1f)
                .setDuration(300)
                .withEndAction(() -> gameOverOverlay.setVisibility(View.GONE))
                .start();
    }
    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (gameView != null && gameView.game != null) {
            long currentScore = gameView.game.score;
            long recordScore = gameView.game.highScore;
            long finalScoreToSend = Math.max(currentScore, recordScore);

            int size = gameView.game.grid.field.length;
            String currentMode = size + "x" + size;

            // Отправляем самый свежий результат
            saveScoreToCloud(finalScoreToSend, currentMode);
        }
        save(); // Сохраняем состояние плиток локально
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (adView != null) adView.resume();

        SharedPreferences prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(this);

        if (gameView != null && gameView.game != null) {
            // 🔥 ФИКС: Формируем ключ на основе размера сетки (например, "high_score_6x6")
            int size = gameView.game.grid.field.length;
            String scoreKey = "high_score_" + size + "x" + size;
            long localBest = prefs.getLong(scoreKey, 0);

            boolean isGameEmpty = true;
            for (int x = 0; x < gameView.game.grid.field.length; x++) {
                for (int y = 0; y < gameView.game.grid.field[0].length; y++) {
                    if (gameView.game.grid.field[x][y] != null) {
                        isGameEmpty = false;
                        break;
                    }
                }
            }

            if (isGameEmpty) {
                load();
            }

            // Ставим рекорд именно для этого режима
            gameView.game.highScore = localBest;
            gameView.invalidate();
        }
    }
    public void triggerVibration(int duration) {
        // Если вибрация выключена пользователем — ничего не делаем
        if (!isVibrationEnabled) return;

        android.os.Vibrator v = (android.os.Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v != null && v.hasVibrator()) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                v.vibrate(android.os.VibrationEffect.createOneShot(duration, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(duration);
            }
        }
    }
    public void showFloatingScore(int addedScore) {
        if (addedScore <= 0) return;

        // Создаем текст программно
        final TextView floatingText = new TextView(this);
        floatingText.setText("+" + addedScore);
        floatingText.setTextSize(22);
        floatingText.setTextColor(Color.parseColor("#776E65")); // Твой фирменный цвет

        // Пытаемся поставить шрифт Montserrat, если он есть
        try {
            Typeface font = Typeface.createFromAsset(getAssets(), "fonts/montserrat_semibold.ttf");
            floatingText.setTypeface(font);
        } catch (Exception e) {
            floatingText.setTypeface(null, Typeface.BOLD);
        }

        // Параметры размещения (в центре сверху, где табло счета)
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        params.topMargin = dp(110); // Настрой этот отступ, чтобы текст вылетал из района счета

        floatingText.setLayoutParams(params);
        rootLayout.addView(floatingText);

        // 🔥 АНИМАЦИЯ: Взлет вверх и плавное исчезновение
        floatingText.animate()
                .translationYBy(-dp(80)) // Летит вверх на 80dp
                .alpha(0f)               // Становится прозрачным
                .setDuration(800)        // Длительность 0.8 сек
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .withEndAction(() -> rootLayout.removeView(floatingText)) // Удаляем из памяти после конца
                .start();
    }
    public void onScoreChanged(int currentScore, int addedPoints) {
        // 1. Легкая вибрация при слиянии
        triggerVibration(15);

        // 2. Если есть TextView для счета, анимируем его
        // animateScore(yourScoreTextView);

        // 3. Показываем всплывающее "+16"
        showFloatingScore(addedPoints);
    }
    @Override
    protected void onDestroy() {
        if (adView != null) adView.destroy();
        save();
        super.onDestroy();
    }

    // Saving game
    private void save() {
        if (gameView == null || gameView.game == null) return;

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();

        Tile[][] field = gameView.game.grid.field;
        Tile[][] undoField = gameView.game.grid.undoField;

        int gridSize = field.length;
        String modePrefix = gridSize + "x" + gridSize + "_";

        // Сохраняем размер, чтобы знать какой префикс юзать при загрузке
        editor.putInt("saved_grid_size", gridSize);

        // Сохраняем плитки с префиксом
        for (int x = 0; x < field.length; x++) {
            for (int y = 0; y < field[0].length; y++) {
                editor.putInt(modePrefix + x + " " + y, field[x][y] != null ? field[x][y].getValue() : 0);
                editor.putInt(modePrefix + UNDO_GRID + x + " " + y, undoField[x][y] != null ? undoField[x][y].getValue() : 0);
            }
        }

        // Сохраняем состояние игры с префиксом
        editor.putLong(modePrefix + SCORE, gameView.game.score);
        editor.putLong(modePrefix + UNDO_SCORE, gameView.game.lastScore);
        editor.putBoolean(modePrefix + CAN_UNDO, gameView.game.canUndo);
        editor.putInt(modePrefix + GAME_STATE, gameView.game.gameState);

        // Флаг, что сохранение именно для ЭТОГО режима существует
        editor.putBoolean(modePrefix + "hasGameSaved", true);
        editor.apply();
    }

    private void load() {
        try {
            if (gameView == null || gameView.game == null) return;

            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

            // 1. Определяем, в каком режиме мы СЕЙЧАС (4x4, 5x5 или 6x6)
            int gridSize = gameView.game.grid.field.length;
            String modePrefix = gridSize + "x" + gridSize + "_";

            // 2. Проверяем, есть ли сохранение именно для ЭТОГО режима
            if (!settings.getBoolean(modePrefix + "hasGameSaved", false)) {
                Log.d("LOAD_SYSTEM", "Нет сохранений для режима " + modePrefix);
                return;
            }

            gameView.game.aGrid.cancelAnimations();

            // 3. Загружаем плитки именно для этого размера поля
            for (int x = 0; x < gridSize; x++) {
                for (int y = 0; y < gridSize; y++) {
                    // Загружаем основное поле с префиксом
                    int val = settings.getInt(modePrefix + x + " " + y, 0);
                    gameView.game.grid.field[x][y] = (val != 0) ? new Tile(x, y, val) : null;

                    // Загружаем Undo поле с префиксом
                    int undoVal = settings.getInt(modePrefix + UNDO_GRID + x + " " + y, 0);
                    gameView.game.grid.undoField[x][y] = (undoVal != 0) ? new Tile(x, y, undoVal) : null;
                }
            }

            // 4. Загружаем счет и рекорды именно этого режима
            gameView.game.score = settings.getLong(modePrefix + SCORE, 0);

            // Рекорд берем из нашего нового ключа
            String scoreKey = "high_score_" + gridSize + "x" + gridSize;
            gameView.game.highScore = settings.getLong(scoreKey, 0);

            gameView.game.lastScore = settings.getLong(modePrefix + UNDO_SCORE, 0);
            gameView.game.canUndo = settings.getBoolean(modePrefix + CAN_UNDO, false);
            gameView.game.gameState = settings.getInt(modePrefix + GAME_STATE, 0);
            gameView.game.lastGameState = settings.getInt(modePrefix + UNDO_GAME_STATE, 0);

            gameView.invalidate();
            Log.d("LOAD_SYSTEM", "Успешно загружен режим: " + modePrefix + " Рекорд: " + gameView.game.highScore);

        } catch (Exception e) {
            Log.e("LOAD_ERROR", "Не удалось загрузить прогресс: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        save(); // Железное сохранение
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Проверяем, был ли нажат один из векторов движения
        boolean isMoveKey = (keyCode == KeyEvent.KEYCODE_DPAD_UP ||
                keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                keyCode == KeyEvent.KEYCODE_DPAD_RIGHT);

        if (isMoveKey) {
            // 1. Делаем ход
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP) gameView.game.move(0);
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) gameView.game.move(1);
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) gameView.game.move(2);
            if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) gameView.game.move(3);

            // 2. СБРАСЫВАЕМ ТАЙМЕР
            if (isChallengeMode) {
                startChallengeTimer(); // Этот вызов внутри себя должен делать .cancel() старому
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }


    private void showLeaderboard() {
        // Здесь мы сами передаем 0 и режим "4x4" по умолчанию
        showLeaderboard(0L, "4x4");
    }

    // 0. Добавь этот вспомогательный метод в MainActivity (вне showLeaderboard)
    private void showLeaderboard(long currentScore, String currentMode) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            getWindow().getDecorView().setForceDarkAllowed(false);
        }

        final Typeface montserratBold = Typeface.createFromAsset(getAssets(), "fonts/montserrat_bold.ttf");
        final String myId = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

        // 1. ГЛАВНЫЙ КОНТЕЙНЕР
        android.widget.LinearLayout root = new android.widget.LinearLayout(this);
        root.setOrientation(android.widget.LinearLayout.VERTICAL);
        root.setBackgroundResource(R.drawable.bg_leaderboard_card);
        root.setPadding(0, 0, 0, dp(20)); // Увеличил нижний паддинг всей карточки

        // 2. ШАПКА
        android.widget.RelativeLayout header = new android.widget.RelativeLayout(this);
        TextView title = new TextView(this);
        title.setText("🏆 RANKINGS");
        title.setTextSize(22);
        title.setTextColor(Color.parseColor("#FFECB3"));
        title.setTypeface(montserratBold);
        android.widget.RelativeLayout.LayoutParams tParams = new android.widget.RelativeLayout.LayoutParams(-2, -2);
        tParams.addRule(android.widget.RelativeLayout.CENTER_IN_PARENT);
        title.setPadding(0, dp(20), 0, dp(10));
        header.addView(title, tParams);

        android.widget.ImageButton btnClose = new android.widget.ImageButton(this);
        btnClose.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
        btnClose.setBackground(null);
        btnClose.setColorFilter(Color.parseColor("#FFECB3"));
        android.widget.RelativeLayout.LayoutParams cParams = new android.widget.RelativeLayout.LayoutParams(dp(40), dp(40));
        cParams.addRule(android.widget.RelativeLayout.ALIGN_PARENT_END);
        cParams.rightMargin = dp(10);
        cParams.topMargin = dp(10);
        header.addView(btnClose, cParams);
        root.addView(header);

        // 3. ТАБЫ
        com.google.android.material.tabs.TabLayout tabs = new com.google.android.material.tabs.TabLayout(this);
        tabs.setBackgroundResource(R.drawable.brawl_tabs_parent_bg);
        android.widget.LinearLayout.LayoutParams tabLp = new android.widget.LinearLayout.LayoutParams(-1, dp(46));
        tabLp.setMargins(dp(25), dp(5), dp(25), dp(10));
        tabs.setSelectedTabIndicator(androidx.core.content.ContextCompat.getDrawable(this, R.drawable.brawl_tab_indicator));
        tabs.setSelectedTabIndicatorGravity(com.google.android.material.tabs.TabLayout.INDICATOR_GRAVITY_STRETCH);
        tabs.setTabIndicatorFullWidth(true);
        tabs.setTabTextColors(Color.WHITE, Color.parseColor("#3E2723"));
        root.addView(tabs, tabLp);

        // 4. СПИСОК (ViewPager2) - Добавил отступ снизу, чтобы не липло к плашке "Я"
        androidx.viewpager2.widget.ViewPager2 pager = new androidx.viewpager2.widget.ViewPager2(this);
        android.widget.LinearLayout.LayoutParams pagerLp = new android.widget.LinearLayout.LayoutParams(-1, dp(350));
        pagerLp.bottomMargin = dp(10);
        pager.setLayoutParams(pagerLp);
        pager.setAdapter(new LeaderboardPagerAdapter(this));
        root.addView(pager);

        new com.google.android.material.tabs.TabLayoutMediator(tabs, pager, (tab, pos) -> {
            String[] titles = {"4X4", "5X5", "6X6"};
            tab.setText(titles[pos]);
        }).attach();

        int startTab = currentMode.equals("5x5") ? 1 : currentMode.equals("6x6") ? 2 : 0;
        pager.setCurrentItem(startTab, false);

        // 5. ЛИЧНАЯ ПЛАШКА "Я" (ЗАКРУГЛЕННЫЙ ОВЕРЛЕЙ КАК В СПИСКЕ)
        android.widget.LinearLayout myContainer = new android.widget.LinearLayout(this);
        android.widget.LinearLayout.LayoutParams containerLp = new android.widget.LinearLayout.LayoutParams(-1, -2);
        containerLp.setMargins(dp(15), dp(5), dp(15), dp(5)); // Те же отступы по бокам, что у элементов
        myContainer.setLayoutParams(containerLp);

        View myView = getLayoutInflater().inflate(R.layout.item_leaderboard_user, myContainer, false);

        // ПРИНУДИТЕЛЬНО ТАКОЙ ЖЕ ФОН КАК У ИГРОКА (Золотая обводка)
        myView.setBackgroundResource(R.drawable.bg_leaderboard_item_default); // Сначала дефолт
        // А теперь поверх накладываем твой кастомный стиль "Выделение меня"
        android.graphics.drawable.GradientDrawable myHighlight = new android.graphics.drawable.GradientDrawable();
        myHighlight.setColor(Color.parseColor("#2D1B18")); // Твой темный фон
        myHighlight.setCornerRadius(dp(16)); // ЗАКРУГЛЕНИЕ
        myHighlight.setStroke(dp(2), Color.parseColor("#FFC107")); // ЗОЛОТАЯ ОБВОДКА
        myView.setBackground(myHighlight);

        myContainer.addView(myView);
        root.addView(myContainer);

        // Поля данных
        TextView myRank = myView.findViewById(R.id.rankText);
        TextView myName = myView.findViewById(R.id.nameText);
        TextView myScoreT = myView.findViewById(R.id.scoreText);
        TextView myFlag = myView.findViewById(R.id.countryFlag);
        android.widget.ImageView myEdit = myView.findViewById(R.id.editNameIcon);

        myRank.setTypeface(montserratBold);
        myName.setTypeface(montserratBold);
        myScoreT.setTypeface(montserratBold);
        if (myEdit != null) myEdit.setVisibility(View.VISIBLE);

        // 6. ОБНОВЛЕНИЕ ПЛАШКИ
        pager.registerOnPageChangeCallback(new androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int pos) {
                String m = (pos == 0) ? "4x4" : (pos == 1) ? "5x5" : "6x6";
                FirebaseDatabase.getInstance("https://first-project-2easy-default-rtdb.europe-west1.firebasedatabase.app/").getReference("leaderboard").child(m)
                        .orderByChild("score").limitToLast(100)
                        .addListenerForSingleValueEvent(new com.google.firebase.database.ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                List<UserScore> list = new ArrayList<>();
                                for (DataSnapshot ds : snapshot.getChildren()) {
                                    UserScore us = ds.getValue(UserScore.class);
                                    if (us != null) list.add(us);
                                }
                                Collections.reverse(list);

                                boolean found = false;
                                for (int i = 0; i < list.size(); i++) {
                                    UserScore u = list.get(i);
                                    if (u != null && u.userId != null && u.userId.equals(myId)) {
                                        myName.setText(u.username);
                                        myScoreT.setText(java.text.NumberFormat.getInstance().format(u.score));
                                        myRank.setText(String.valueOf(i + 1));
                                        myFlag.setText(LeaderboardPagerAdapter.getEmojiFlag(u.country));
                                        myFlag.setVisibility(View.VISIBLE);
                                        found = true; break;
                                    }
                                }
                                if (!found) {
                                    myRank.setText("-");
                                    myName.setText("You");
                                    myScoreT.setText(java.text.NumberFormat.getInstance().format(currentScore));
                                    myFlag.setVisibility(View.GONE);
                                }
                            }
                            @Override public void onCancelled(@NonNull DatabaseError e) {}
                        });
            }
        });

        // 7. ПОКАЗ
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this).setView(root).create();
        btnClose.setOnClickListener(v -> dialog.dismiss());
        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0));
            android.view.WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
            lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.96);
            dialog.getWindow().setAttributes(lp);
        }

        if (myEdit != null) {
            myEdit.setOnClickListener(v -> {
                String[] modesList = {"4x4", "5x5", "6x6"};
                showNameInputDialog(currentScore, modesList[pager.getCurrentItem()]);
            });
        }
    }
    private String getEmojiFlag(String countryCode) {
        if (countryCode == null || countryCode.length() != 2) return "";
        try {
            int firstLetter = Character.codePointAt(countryCode.toUpperCase(), 0) - 0x41 + 0x1F1E6;
            int secondLetter = Character.codePointAt(countryCode.toUpperCase(), 1) - 0x41 + 0x1F1E6;
            return new String(Character.toChars(firstLetter)) + new String(Character.toChars(secondLetter));
        } catch (Exception e) {
            return "";
        }
    }

    private void showNameInputDialog(long scoreToSave, String mode) {
        final Typeface montserrat = Typeface.createFromAsset(getAssets(), "fonts/montserrat_semibold.ttf");

        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("ENTER YOUR NAME");
        input.setHintTextColor(Color.parseColor("#BBADA0"));
        input.setTextColor(Color.parseColor("#776E65"));
        input.setTypeface(montserrat);
        input.setGravity(Gravity.CENTER);
        input.setFilters(new android.text.InputFilter[] {new android.text.InputFilter.LengthFilter(12)});

        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(dp(25), dp(20), dp(25), dp(10));
        input.setLayoutParams(params);
        container.addView(input);

        runOnUiThread(() -> {
            android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                    .setView(container)
                    .setCancelable(false)
                    .setPositiveButton("SAVE", (d, which) -> {
                        String name = input.getText().toString().trim();
                        if (name.isEmpty()) name = "Player";

                        android.preference.PreferenceManager.getDefaultSharedPreferences(this)
                                .edit().putString("user_name", name).apply();

                        // 🔥 ИСПОЛЬЗУЕМ mode, КОТОРЫЙ ПРИШЕЛ В МЕТОД
                        updateLeaderboardWithName(name);

                        launchConfetti();
                    })
                    .create();

            dialog.show();

            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(R.drawable.leaderboard_window_bg);
                dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#8F7A66"));
                dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setTypeface(montserrat);
            }
        });
    }
    private void saveScoreToCloud(long newScore, String mode) {
        String userId = android.provider.Settings.Secure.getString(getContentResolver(),
                android.provider.Settings.Secure.ANDROID_ID);

        DatabaseReference scoreRef = mDatabase.child("leaderboard").child(mode).child(userId);

        SharedPreferences prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(this);
        String username = prefs.getString("user_name", "Player");
        String countryCode = getResources().getConfiguration().locale.getCountry().toUpperCase();
        if (countryCode.isEmpty()) countryCode = "UN";

        UserScore leaderEntry = new UserScore(username, newScore, userId, countryCode);

        scoreRef.setValue(leaderEntry).addOnSuccessListener(aVoid -> {
            Log.d("Firebase_Success", "Score updated to " + newScore + " in " + mode);
        });
    }

    private void updateLeaderboardWithName(String name) {
        String userId = android.provider.Settings.Secure.getString(getContentResolver(),
                android.provider.Settings.Secure.ANDROID_ID);

        // Список всех режимов, чтобы имя менялось везде разом
        String[] modes = {"4x4", "5x5", "6x6"};

        HashMap<String, Object> updates = new HashMap<>();
        updates.put("username", name);

        for (String mode : modes) {
            // Он обновит только "username", твои 400к очков останутся нетронутыми!
            mDatabase.child("leaderboard").child(mode).child(userId).updateChildren(updates)
                    .addOnSuccessListener(aVoid -> android.util.Log.d("Firebase", "Имя синхронизировано в: " + mode));
        }

        // Сохраняем имя локально
        android.preference.PreferenceManager.getDefaultSharedPreferences(this)
                .edit().putString("user_name", name).apply();
    }

    // А этот метод ты сможешь использовать, чтобы обновить СРАЗУ ВСЁ
    private void updateNameEverywhere(String name) {
        String[] modes = {"4x4", "5x5", "6x6"};
        for (String m : modes) {
            updateLeaderboardWithName(name);
        }
    }

    private void loadScoreFromCloud() {
        if (gameView == null || gameView.game == null) return;

        String userId = android.provider.Settings.Secure.getString(getContentResolver(),
                android.provider.Settings.Secure.ANDROID_ID);

        // 🔥 ФИКС: Определяем текущий режим
        int size = gameView.game.grid.field.length;
        String mode = size + "x" + size;
        String scoreKey = "high_score_" + mode;

        // Ссылаемся на конкретный режим в облаке
        mDatabase.child("users").child(userId).child("scores").child(mode).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                long cloudScore = 0;
                if (task.getResult().exists()) {
                    Object val = task.getResult().getValue();
                    cloudScore = (val instanceof Long) ? (long) val : 0;
                }

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                long localScore = prefs.getLong(scoreKey, 0);

                if (cloudScore > localScore) {
                    // Обновляем локальный рекорд именно для этого режима
                    prefs.edit().putLong(scoreKey, cloudScore).apply();
                    gameView.game.highScore = cloudScore;
                    runOnUiThread(() -> gameView.invalidate());
                    Log.d("SYNC", "Загрузили рекорд для " + mode + " из облака: " + cloudScore);
                }
                else if (localScore > cloudScore) {
                    // Пушим локальный рекорд этого режима в облако
                    saveScoreToCloud(localScore, mode);
                    Log.d("SYNC", "Отправили локальный рекорд " + mode + " в облако: " + localScore);
                }
            }
        });
    }

    private void showChallengesDialog() {
        final Typeface montserrat = Typeface.createFromAsset(getAssets(), "fonts/montserrat_semibold.ttf");

        // Инициализация UI (Таймер и счетчик)
        if (timerBar == null) {
            timerBar = new android.widget.ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
            timerBar.setMax(100);
            timerBar.setProgress(100);
            android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable(
                    android.graphics.drawable.GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[] {0xFFEDC22E, 0xFFF67C5F, 0xFFFF5F5F});
            gd.setCornerRadius(dp(20));
            android.graphics.drawable.ClipDrawable clip = new android.graphics.drawable.ClipDrawable(gd, Gravity.START, android.graphics.drawable.ClipDrawable.HORIZONTAL);
            android.graphics.drawable.LayerDrawable ld = new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{new android.graphics.drawable.ColorDrawable(Color.parseColor("#33776E65")), clip});
            ld.setId(0, android.R.id.background); ld.setId(1, android.R.id.progress);
            timerBar.setProgressDrawable(ld);
            rootLayout.addView(timerBar, new FrameLayout.LayoutParams(dp(56), dp(6)));
            timerBar.setVisibility(View.GONE);
        }

        if (tilesCounter == null) {
            tilesCounter = new android.widget.TextView(this);
            tilesCounter.setTextSize(14);
            tilesCounter.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/clearsans_bold.ttf"));
            tilesCounter.setTextColor(Color.parseColor("#F67C5F"));
            tilesCounter.setGravity(Gravity.CENTER);
            rootLayout.addView(tilesCounter, new FrameLayout.LayoutParams(-2, -2));
            tilesCounter.setVisibility(View.GONE);
        }

        LinearLayout container = new LinearLayout(this);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setPadding(dp(20), dp(20), dp(20), dp(20));

        TextView title = new TextView(this);
        title.setText("SELECT MODE");
        title.setTextSize(24);
        title.setTypeface(montserrat);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(Color.parseColor("#776E65"));
        title.setPadding(0, 0, 0, dp(20));
        container.addView(title);

        android.app.AlertDialog mainDialog = new android.app.AlertDialog.Builder(this).setView(container).create();

        // КНОПКА CLASSIC
        container.addView(createChallengeCard("CLASSIC", "Chill & Swipe. No rush.", "#8F7A66", 4, () -> {
            if (gameView.game.score > 0 && !gameView.game.gameLost()) {
                showSwitchConfirmation("CLASSIC",4, mainDialog);
            } else {
                executeSwitch("CLASSIC",4);
                mainDialog.dismiss();
            }
        }));

        container.addView(createSpace());

        // КНОПКА TIME ATTACK
        container.addView(createChallengeCard("TIME ATTACK", "3 seconds per move!", "#952509", 4 ,() -> {
            if (gameView.game.score > 0 && !gameView.game.gameLost()) {
                showSwitchConfirmation("TIME ATTACK",4, mainDialog);
            } else {
                executeSwitch("TIME ATTACK",4);
                mainDialog.dismiss();
            }
        }));

        container.addView(createSpace());

        // КНОПКА 5x5
        boolean is5x5 = currentActiveMode.equals("CLASSIC") && gameView.game.numSquaresX == 5;
        container.addView(createChallengeCard("BIG 5x5", "More space, more fun!", "#71b2d3", 5, () -> {
            if (gameView.game.score > 0) {
                showSwitchConfirmation("CLASSIC", 5, mainDialog);
            } else {
                executeSwitch("CLASSIC", 5);
                mainDialog.dismiss();
            }
        }));

        container.addView(createSpace());

        // КНОПКА 6x6
        boolean is6x6 = currentActiveMode.equals("CLASSIC") && gameView.game.numSquaresX == 6;
        container.addView(createChallengeCard("EXTREME 6x6", "Huge board for professionals!", "#716fb3", 6 , () -> {
            if (gameView.game.score > 0) {
                showSwitchConfirmation("CLASSIC", 6, mainDialog);
            } else {
                executeSwitch("CLASSIC", 6);
                mainDialog.dismiss();
            }
        }));

        mainDialog.show();
        if (mainDialog.getWindow() != null) {
            mainDialog.getWindow().setBackgroundDrawableResource(R.drawable.leaderboard_window_bg);
        }
    }

    // 🔥 КРАСИВОЕ ПОДТВЕРЖДЕНИЕ
    private void showSwitchConfirmation(final String targetMode, final int targetSize, final android.app.AlertDialog parentDialog) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(25), dp(25), dp(25), dp(25));
        layout.setGravity(Gravity.CENTER);

        TextView title = new TextView(this);
        title.setText("CONFIRM RESET");
        title.setTextSize(20);
        title.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/montserrat_semibold.ttf"));
        title.setTextColor(Color.parseColor("#776E65"));
        title.setPadding(0, 0, 0, dp(10));
        layout.addView(title);

        TextView msg = new TextView(this);
        msg.setText("Your current score will be lost.");
        msg.setGravity(Gravity.CENTER);
        msg.setTextColor(Color.parseColor("#776E65"));

        // ДОБАВЛЕНО: Шрифт и жирность для сообщения
        try {
            Typeface montserrat = Typeface.createFromAsset(getAssets(), "fonts/montserrat_semibold.ttf");
            msg.setTypeface(montserrat, Typeface.BOLD);
        } catch (Exception e) {
            msg.setTypeface(Typeface.DEFAULT_BOLD);
        }

        msg.setPadding(0, 0, 0, dp(20));
        layout.addView(msg);

        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);

        TextView btnCancel = createCustomButton("CANCEL", "#8F7A66");
        TextView btnReset = createCustomButton("RESET", "#F67C5F");

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(45), 1f);
        p.setMargins(0, 0, dp(10), 0);
        buttons.addView(btnCancel, p);
        buttons.addView(btnReset, new LinearLayout.LayoutParams(0, dp(45), 1f));

        layout.addView(buttons);

        final android.app.AlertDialog confirmDialog = new android.app.AlertDialog.Builder(this).setView(layout).create();
        if (confirmDialog.getWindow() != null) confirmDialog.getWindow().setBackgroundDrawableResource(R.drawable.leaderboard_window_bg);

        btnCancel.setOnClickListener(v -> confirmDialog.dismiss());
        btnReset.setOnClickListener(v -> {
            executeSwitch(targetMode, targetSize);
            confirmDialog.dismiss();
            if (parentDialog != null) parentDialog.dismiss();
        });

        confirmDialog.show();
    }

    // 🔥 ВСПОМОГАТЕЛЬНЫЙ МЕТОД ДЛЯ КНОПОК
    private TextView createCustomButton(String text, String color) {
        TextView b = new TextView(this);
        b.setText(text);
        b.setTextColor(Color.WHITE);
        b.setGravity(Gravity.CENTER);
        b.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/clearsans_bold.ttf"));
        android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
        gd.setColor(Color.parseColor(color));
        gd.setCornerRadius(dp(8));
        b.setBackground(gd);
        return b;
    }

    // 🔥 МЕТОД ВЫПОЛНЕНИЯ ПЕРЕКЛЮЧЕНИЯ
    private void executeSwitch(String mode, int gridSize) {
        // 1. ПОЛУЧАЕМ НАСТРОЙКИ
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean currentDark = prefs.getBoolean("dark_mode", false);

        // 2. ОБНОВЛЯЕМ СОСТОЯНИЕ VIEW (флаги и сброс фона)
        gameView.setDarkMode(currentDark);
        gameView.applyTheme(currentDark); // Метод, который мы правили: ставит background = null

        // 3. НАСТРОЙКА ЛОГИКИ СЕТКИ
        gameView.game.numSquaresX = gridSize;
        gameView.game.numSquaresY = gridSize;
        gameView.game.newGame();

        // 4. УПРАВЛЕНИЕ РЕЖИМАМИ (CLASSIC / TIME ATTACK)
        if (mode.equals("CLASSIC")) {
            currentActiveMode = "CLASSIC";
            isChallengeMode = false;
            if (challengeTimer != null) {
                challengeTimer.cancel();
                challengeTimer = null;
            }
            if (timerBar != null) {
                timerBar.clearAnimation();
                timerBar.setVisibility(View.GONE);
            }
            if (tilesCounter != null) {
                tilesCounter.setVisibility(View.GONE);
            }
            stopAndHideTimer();
        } else if (mode.equals("TIME ATTACK")) {
            currentActiveMode = "TIME ATTACK";
            isChallengeMode = true;
            positionTimerUnderLogo();
            if (timerBar != null) {
                timerBar.setVisibility(View.VISIBLE);
                timerBar.setProgress(100);
            }
            if (tilesCounter != null) {
                tilesCounter.setVisibility(View.VISIBLE);
            }
            startChallengeTimer();
        }

        // 5. ОЧИСТКА СОСТОЯНИЯ ГРЫ
        gameView.game.canUndo = false;

        gameView.post(new Runnable() {
            @Override
            public void run() {
                // Теперь вызываем пересчет координат
                gameView.onSizeChanged(gameView.getWidth(), gameView.getHeight(), 0, 0);
                // Принудительно перерисовываем. onDraw увидит background == null
                // и создаст НОВЫЙ битмап с ПРАВИЛЬНЫМИ цветами.
                gameView.invalidate();
            }
        });
    }


    public void startChallengeTimer() {
        // 1. Убиваем ЛЮБОЙ старый таймер
        if (challengeTimer != null) {
            challengeTimer.cancel();
            challengeTimer = null;
        }

        if (!isChallengeMode || gameView.game == null || gameView.game.gameLost()) {
            return;
        }

        boolean vibrationEnabled = android.preference.PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("vibration_enabled", true);

        challengeTimer = new android.os.CountDownTimer(3000, 30) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (gameView.game == null) return;

                // Если настройки открыты — скрываем. Если закрыты — проверяем, режим ли это.
                if (isSettingsOpen || !isChallengeMode) {
                    if (timerBar != null) timerBar.setVisibility(View.GONE);
                    if (tilesCounter != null) tilesCounter.setVisibility(View.GONE);
                    return;
                } else {
                    if (timerBar != null) timerBar.setVisibility(View.VISIBLE);
                    if (tilesCounter != null) tilesCounter.setVisibility(View.VISIBLE);
                }

                // ... остальной код таймера ...
                if (timerBar != null) {
                    timerBar.setProgress((int) (millisUntilFinished / 30));
                }
                positionTimerUnderLogo();
            }

            @Override
            public void onFinish() {
                runOnUiThread(() -> {
                    if (!isChallengeMode || gameView.game == null) return;

                    if (gameView.game.grid.isCellsAvailable()) {
                        if (vibrationEnabled) vibrate(140, 255);
                        gameView.game.addRandomTile();
                        updateTilesCounter();
                        gameView.invalidate();

                        if (gameView.game.gameLost()) {
                            onGameOver();
                        } else {
                            startChallengeTimer();
                        }
                    } else {
                        onGameOver();
                    }
                });
            }
        }.start();
    }

    private void stopAndHideTimer() {
        isTimeAttackMode = false; // Выключаем флаг режима при проигрыше

        if (challengeTimer != null) {
            challengeTimer.cancel();
            challengeTimer = null;
        }

        if (timerBar != null) {
            timerBar.animate().cancel();
            timerBar.setVisibility(View.GONE);
            timerBar.setAlpha(1.0f);
        }

        stopTimerWarningAnimation(); // Чтобы мигание не зависло в красном цвете
    }
    private void vibrate(int durationMs, int amplitude) {
        // ИСПОЛЬЗУЕМ ПРАВИЛЬНЫЙ КЛЮЧ ИЗ ЛОГОВ: "vibration_enabled"
        boolean isVibroEnabled = android.preference.PreferenceManager
                .getDefaultSharedPreferences(this)
                .getBoolean("vibration_enabled", true);

        if (!isVibroEnabled) return;

        Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (v == null || !v.hasVibrator()) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            long[] pattern = {0, 50, 30, 50};
            int[] amplitudes = {0, 255, 0, 255};
            v.vibrate(VibrationEffect.createWaveform(pattern, amplitudes, -1));
        } else {
            v.vibrate(durationMs);
        }
    }
    // Вспомогательный метод для создания карточки режима
    private View createChallengeCard(String title, String desc, String color, int targetSize, Runnable onClick) {
        Typeface font = Typeface.createFromAsset(getAssets(), "fonts/montserrat_semibold.ttf");

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(15), dp(15), dp(15), dp(15));
        card.setClickable(true);
        card.setFocusable(true);

        TextView nameTxt = new TextView(this);
        nameTxt.setText(title);
        nameTxt.setTextSize(18);
        nameTxt.setTextColor(Color.parseColor(color));
        nameTxt.setTypeface(font);

        TextView descTxt = new TextView(this);
        descTxt.setText(desc);
        descTxt.setTextSize(14);
        descTxt.setTextColor(Color.parseColor("#776E65"));
        descTxt.setTypeface(font);

        card.addView(nameTxt);
        card.addView(descTxt);

        boolean isActive;
        if (title.equals("TIME ATTACK")) {
            isActive = currentActiveMode.equals("TIME ATTACK");
        } else {
            // Если это один из классических режимов (4, 5 или 6)
            isActive = currentActiveMode.equals("CLASSIC") && gameView.game.numSquaresX == targetSize;
        }

        if (isActive) {
            card.setBackgroundResource(R.drawable.challenge_item_active_bg); // С коричневой обводкой
            card.setAlpha(1.0f);
        } else {
            card.setBackgroundResource(R.drawable.challenge_item_bg); // Обычный фон
            card.setAlpha(0.6f);
        }

        card.setOnClickListener(v -> {
            // currentActiveMode обновится внутри executeSwitch, здесь просто запускаем
            onClick.run();
        });

        return card;
    }

    private View createSpace() {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(12)));
        return v;
    }
    private void restartGame() {
        gameView.game.newGame(); // Твой метод сброса счета и поля
        gameView.invalidate();
        Toast.makeText(this, "Mode Switched!", Toast.LENGTH_SHORT).show();
    }
    // Этот метод мы будем вызывать везде, где происходит движение
    public void onUserMadeMove() {
        runOnUiThread(() -> {
            if (isChallengeMode) {
                startChallengeTimer(); // Это обнулит старый и запустит новый на 3 сек
            }
        });
    }
    private void startTimerWarningAnimation() {
        if (timerBar != null && timerBar.getAnimation() == null) {
            // 1. Делаем цвет ярко-алым (Blood Red)
            timerBar.getProgressDrawable().setColorFilter(Color.parseColor("#FF0000"), PorterDuff.Mode.SRC_IN);

            // 2. Создаем мигание прозрачности
            android.view.animation.Animation alphaAnim = new android.view.animation.AlphaAnimation(1.0f, 0.2f);
            alphaAnim.setDuration(150); // Еще быстрее для паники!
            alphaAnim.setRepeatMode(android.view.animation.Animation.REVERSE);
            alphaAnim.setRepeatCount(android.view.animation.Animation.INFINITE);

            // 3. Добавляем мелкокалиберную тряску (Shake)
            // Создаем CycleInterpolator для эффекта дрожания
            timerBar.animate()
                    .translationX(8f)
                    .setDuration(100)
                    .setInterpolator(new android.view.animation.CycleInterpolator(4))
                    .start();

            timerBar.startAnimation(alphaAnim);
        }
    }

    private void stopTimerWarningAnimation() {
        if (timerBar != null) {
            timerBar.clearAnimation();
            // Возвращаем стандартный цвет (например, белый или синий)
            timerBar.getProgressDrawable().clearColorFilter();
        }
    }



    public void shakeGameView(float intensity) {
        // Ограничиваем максимальную силу, чтобы картинка не улетела
        float force = Math.min(intensity * 5f, 25f);

        // Анимация: вправо-вниз -> влево-вверх -> на место
        gameView.animate()
                .translationX(force)
                .translationY(force)
                .setDuration(40)
                .withEndAction(() -> {
                    gameView.animate()
                            .translationX(-force)
                            .translationY(-force)
                            .setDuration(40)
                            .withEndAction(() -> {
                                gameView.animate()
                                        .translationX(0)
                                        .translationY(0)
                                        .setDuration(40)
                                        .start();
                            }).start();
                }).start();
    }
    // Добавь этот метод в MainActivity.java
    public void spawnParticles(int gridX, int gridY, int tileValue) {
        // Используем gameView. перед каждой переменной, так как они живут в классе MainView
        int cellX = gameView.startingX + gameView.gridWidth + (gameView.cellSize + gameView.gridWidth) * gridX + gameView.cellSize / 2;
        int cellY = gameView.startingY + gameView.gridWidth + (gameView.cellSize + gameView.gridWidth) * gridY + gameView.cellSize / 2;

        int numParticles = 10 + (tileValue / 64);
        if (numParticles > 30) numParticles = 30;

        long currentTime = System.currentTimeMillis();
        for (int i = 0; i < numParticles; i++) {
            gameView.game.particles.add(new Particle(cellX, cellY, tileValue, currentTime));
        }
        // Используем postInvalidate, так как это может вызываться из разных потоков
        gameView.postInvalidate();
    }
    private void updateTilesCounter() {
        if (tilesCounter != null && gameView != null && gameView.game != null) {
            int available = gameView.game.grid.getAvailableCells().size();
            tilesCounter.setText("" + available);

            // Визуальное предупреждение: если мест мало, красим в красный
            if (available <= 3) {
                tilesCounter.setTextColor(Color.RED);
            } else {
                tilesCounter.setTextColor(Color.parseColor("#776E65"));
            }
        }
    }

    private void positionTimerUnderLogo() {
        if (timerBar == null || gameView == null || tilesCounter == null) return;

        // 1. Если настройки открыты ИЛИ это НЕ режим челенджа — гасим всё нахер
        if (isSettingsOpen || !isChallengeMode) {
            timerBar.setVisibility(View.GONE);
            tilesCounter.setVisibility(View.GONE);
            return;
        }

        // --- Твой код расчета координат (без изменений) ---
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/clearsans_bold.ttf"));
        paint.setTextSize(gameView.headerTextSize);
        float logoWidth = paint.measureText("2048");
        Paint.FontMetrics fm = paint.getFontMetrics();
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams((int) logoWidth, dp(4));
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.leftMargin = gameView.startingX;
        float ascent = fm.ascent;
        float descent = fm.descent;
        int textShiftY = (int) ((descent + ascent) / 2) * 2;
        int headerBaselineY = gameView.sYAll - textShiftY + 80;
        lp.topMargin = headerBaselineY + (int) descent + dp(4);
        timerBar.setLayoutParams(lp);
        FrameLayout.LayoutParams tcParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
        tcParams.gravity = Gravity.TOP | Gravity.START;
        int spacing = dp(8);
        tcParams.leftMargin = lp.leftMargin + (int) logoWidth + spacing;
        tcParams.topMargin = headerBaselineY - (int) (Math.abs(ascent) * -0.2f);
        tilesCounter.setLayoutParams(tcParams);
        tilesCounter.setTextSize(TypedValue.COMPLEX_UNIT_PX, gameView.headerTextSize / 5.2f);
        // ------------------------------------------------

        // 2. Показываем только если мы в челендже и настройки закрыты
        timerBar.setVisibility(View.VISIBLE);
        tilesCounter.setVisibility(View.VISIBLE);
        timerBar.bringToFront();
        tilesCounter.bringToFront();

        updateTilesCounter();
    }

    private void applyHeartBonus() {
        if (gameView == null || gameView.game == null) return;

        // 1. Запоминаем текущий честный счет
        final long realScore = gameView.game.score;

        if (heartsCount > 0) {
            gameView.game.grid.clearSmallTiles();
            heartsCount--;
            updateHeartsDisplay();

            gameView.game.setEndState(false);
            gameView.game.gameState = 0;
            gameView.game.score = realScore; // Первый возврат

            hideGameOver();
            gameView.createOverlays();
            gameView.invalidate();

            // 🔥 ВТОРОЙ ШАНС ДЛЯ СЧЕТА (через 300мс)
            // Если что-то в фоне подменило очки на 1404, мы их вернем обратно
            gameView.postDelayed(() -> {
                if (gameView.game != null) {
                    gameView.game.score = realScore;
                    gameView.invalidate();
                    Log.d("ULTRA_FIX", "Счет принудительно возвращен к: " + realScore);
                }
            }, 300);

            gameView.setOnTouchListener(new InputListener(gameView, this));
        }
    }

    public int getHeartsCount() {
        return heartsCount;
    }

    public void showSecondChanceDialog() {
        // 1. ОСТАНАВЛИВАЕМ ТАЙМЕР (Критично для Time Attack)
        if (challengeTimer != null) {
            challengeTimer.cancel();
        }

        // 2. Создаем диалог
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("❤️ ВТОРОЙ ШАНС");
        builder.setMessage("У тебя осталось " + heartsCount + " ❤️\n\nХочешь использовать одно сердце, чтобы взорвать все плитки 2 и 4 и продолжить игру?");

        builder.setPositiveButton("СМОТРЕТЬ РЕКЛАМУ", (dialog, which) -> {
            // Здесь вызываешь AdMob Rewarded Ad.
            // В коллбэке успешного просмотра рекламы вызывай: applySecondChance();
            applySecondChance();
        });

        builder.setNegativeButton("НЕТ, СДАЮСЬ", (d, which) -> {
            heartsCount = 3;
            updateHeartsDisplay();
            onGameOver();
        });

        builder.setCancelable(false); // Чтобы нельзя было закрыть кликом мимо
        builder.show();
    }
    private void executeGameOverLogicInActivity() {
        // Если игрок сдался, просто запускаем стандартный финал
        onGameOver();
    }

    // ГЛАВНЫЙ МЕТОД ВОСКРЕШЕНИЯ
    public void applySecondChance() {
        runOnUiThread(() -> {
            if (gameView == null || gameView.game == null) return;

            hideGameOver();

            if (heartsCount > 0) {
                heartsCount--;
                updateHeartsDisplay();
            }

            // 1. Очищаем мелкие плитки
            gameView.game.grid.clearSmallTiles();

            // 2. СБРОС СОСТОЯНИЯ ПРОИГРЫША
            gameView.game.setEndState(false);
            gameView.game.gameState = 0;

            // 🔥 ВОТ ОНА, ГЛАВНАЯ СТРОКА:
            // Отключаем возможность отмены хода сразу после воскрешения
            gameView.game.canUndo = false;

            // 3. Оживляем управление
            gameView.createOverlays();
            gameView.setOnTouchListener(new InputListener(gameView, this));

            gameView.invalidate();

            Log.d("FIX", "Второй шанс активирован. Undo заблокирован.");
        });
    }

    private void triggerFlashEffect() {
        final View flash = new View(this);
        flash.setBackgroundColor(Color.WHITE);
        rootLayout.addView(flash, new FrameLayout.LayoutParams(-1, -1));

        flash.animate()
                .alpha(0f)
                .setDuration(400)
                .withEndAction(() -> rootLayout.removeView(flash))
                .start();
    }
    private void setupHeartsUI() {
        heartsLayout = new LinearLayout(this);
        heartsLayout.setOrientation(LinearLayout.HORIZONTAL);
        heartsLayout.setPadding(dp(8), dp(4), dp(8), dp(4));
        // Делаем полупрозрачный фон, как у кнопок, но продолговатый
        heartsLayout.setBackgroundResource(R.drawable.round_button_bg);
        heartsLayout.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#33776E65")));

        // Позиционируем справа, под кнопкой вибрации
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.TOP | Gravity.END;
        params.rightMargin = dp(20);
        params.topMargin = dp(152); // На уровне кнопки молнии (challenges), но с другой стороны

        rootLayout.addView(heartsLayout, params);
        updateHeartsDisplay();
    }

    public void updateHeartsDisplay() {
        if (heartsLayout == null) return;

        // Получаем текущее состояние темы из SharedPreferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isDarkMode = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("dark_mode", false);

        heartsLayout.removeAllViews();
        heartsLayout.setPadding(dp(10), 0, dp(10), 0);

        // Выбираем символ для потраченной жизни:
        // В темной теме пустые жизни будут белыми (🤍), в светлой — черными (🖤)
        String emptyHeart = isDarkMode ? "🤍" : "🖤";

        for (int i = 0; i < 3; i++) {
            TextView heart = new TextView(this);

            if (i < heartsCount) {
                heart.setText("❤️"); // Живое сердце всегда красное
                heart.setAlpha(1.0f);
            } else {
                heart.setText(emptyHeart);
                // Делаем чуть прозрачнее, чтобы не отвлекало от игры
                heart.setAlpha(1.0f);
            }

            heart.setTextSize(16);
            heart.setPadding(dp(2), 0, dp(2), 0);
            heartsLayout.addView(heart);
        }

        // Опционально: Меняем фон самой плашки под тему
        int bgColor = isDarkMode ? Color.parseColor("#44FFFFFF") : Color.parseColor("#33776E65");
        heartsLayout.setBackgroundTintList(android.content.res.ColorStateList.valueOf(bgColor));
    }
    // Этот метод вызывается из MainGame, когда ходов нет
    public void handleGameEndTrigger() {
        runOnUiThread(() -> {
            // Никакой рекламы сразу! Только показываем наше красивое меню.
            onGameOver();
        });
    }
    // Добавь это в MainActivity.java
    public void resetHearts() {
        runOnUiThread(() -> {
            heartsCount = 3;
            updateHeartsDisplay();
            Log.d("HEARTS", "Сердечки восстановлены до 3");
        });
    }

    private void showGridSizeSelection() {
        String[] options = {"Режим 5 x 5", "Режим 6 x 6"};

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Выберите размер поля");
        builder.setItems(options, (dialog, which) -> {
            int size = (which == 0) ? 5 : 6; // Если нажали первую кнопку - 5, вторую - 6

            // Запускаем ту же игру, но передаем размер
            Intent intent = new Intent(MainActivity.this, MainActivity.class);
            intent.putExtra("GRID_SIZE", size);
            startActivity(intent);
        });
        builder.show();
    }
    public int getMoveThreshold() {
        try {
            long val = mFirebaseRemoteConfig.getLong("move_threshold");

            // Логируем, чтобы ты видел реальное число в Android Studio
            Log.d("REMOTE_DEBUG", "Значение из облака: " + val);

            // Если пришло 0 (ошибка или не загрузилось) или слишком маленькое/большое число
            if (val <= 0 || val > 900) {
                return 100; // Твой проверенный стандарт
            }

            return (int) val;
        } catch (Exception e) {
            return 100; // На случай любой непредвиденной ошибки
        }
    }

    private void loadUndoData() {
        SharedPreferences prefs = getSharedPreferences("GamePrefs", MODE_PRIVATE);
        String lastDate = prefs.getString("last_undo_date", "");
        String currentDate = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());

        if (!currentDate.equals(lastDate)) {
            // Новый день! Даем 5 попыток
            undoCount = MAX_UNDO;
            prefs.edit().putString("last_undo_date", currentDate).apply();
        } else {
            undoCount = prefs.getInt("undo_count", MAX_UNDO);
        }
    }
    public boolean canDoUndo() {
        return undoCount > 0;
    }

    public void useUndo() {
        if (undoCount > 0) {
            undoCount--;
            // Сохраняем сразу, чтобы не потерять прогресс
            getSharedPreferences("GamePrefs", MODE_PRIVATE)
                    .edit()
                    .putInt("undo_count", undoCount)
                    .apply();
        }
    }

    public void addUndoFromAds() {
        undoCount = MAX_UNDO; // Или +5
        getSharedPreferences("GamePrefs", MODE_PRIVATE).edit().putInt("undo_count", undoCount).apply();
    }

    public int getUndoCount() {
        return undoCount;
    }

    public void decrementUndoCount() {
        if (undoCount > 0) {
            undoCount--;
        }
    }
    private void saveUndoData() {
        getSharedPreferences("GamePrefs", MODE_PRIVATE)
                .edit()
                .putInt("undo_count", undoCount)
                .apply();
    }

    public void showRewardedAdForUndo() {
        // 1. Цвета и настройки стиля
        int textColor = isDarkMode ? Color.parseColor("#F9F6F2") : Color.parseColor("#776E65");
        String cancelBtnColor = "#8F7A66"; // Коричневый (как CANCEL на скрине)
        String actionBtnColor = "#F67C5F"; // Коралловый (как RESET на скрине)

        // 2. Создание главного контейнера
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(25), dp(30), dp(25), dp(25));
        layout.setGravity(Gravity.CENTER);

        // 3. Заголовок (Жирный, CAPS)
        TextView title = new TextView(this);
        title.setText("LIMIT REACHED");
        title.setTextSize(22);
        try {
            Typeface montserratSemiBold = Typeface.createFromAsset(getAssets(), "fonts/montserrat_semibold.ttf");
            title.setTypeface(montserratSemiBold);
        } catch (Exception e) {
            title.setTypeface(Typeface.DEFAULT_BOLD);
        }
        title.setTextColor(textColor);
        title.setPadding(0, 0, 0, dp(15));
        layout.addView(title);

        // 4. Сообщение (Сделал жирным по твоей просьбе)
        TextView msg = new TextView(this);
        msg.setText("Watch a video to get 5 more undos.");
        msg.setGravity(Gravity.CENTER);
        msg.setTextSize(17);
        msg.setTextColor(textColor);
        try {
            Typeface montserratBold = Typeface.createFromAsset(getAssets(), "fonts/montserrat_semibold.ttf");
            // Применяем BOLD стиль к шрифту Montserrat
            msg.setTypeface(montserratBold, Typeface.BOLD);
        } catch (Exception e) {
            msg.setTypeface(Typeface.DEFAULT_BOLD);
        }
        msg.setPadding(0, 0, 0, dp(25));
        layout.addView(msg);

        // 5. Контейнер для кнопок
        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);

        // Твои кастомные кнопки из MainActivity
        TextView btnCancel = createCustomButton("CANCEL", cancelBtnColor);
        TextView btnWatch = createCustomButton("WATCH", actionBtnColor);

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(50), 1f);
        p.setMargins(0, 0, dp(12), 0);

        buttons.addView(btnCancel, p);
        buttons.addView(btnWatch, new LinearLayout.LayoutParams(0, dp(50), 1f));

        layout.addView(buttons);

        // 6. Создание и настройка AlertDialog
        final android.app.AlertDialog confirmDialog = new android.app.AlertDialog.Builder(this).setView(layout).create();

        if (confirmDialog.getWindow() != null) {
            // Устанавливаем твой фон с закругленными углами
            confirmDialog.getWindow().setBackgroundDrawableResource(R.drawable.leaderboard_window_bg);
        }

        // 7. Обработка нажатия CANCEL
        btnCancel.setOnClickListener(v -> {
            v.startAnimation(clickAnim);
            confirmDialog.dismiss();
        });

        // 8. Обработка нажатия WATCH (Логика рекламы)
        btnWatch.setOnClickListener(v -> {
            v.startAnimation(clickAnim);

            if (rewardedAd != null) {
                rewardedAd.show(this, rewardItem -> {
                    // Вызывается ТОЛЬКО если досмотрели до конца
                    addUndoFromAds(); // Твой метод: undoCount = 5 + save
                    if (gameView != null) {
                        gameView.invalidate(); // Перерисовываем полоски в MainView
                    }
                    Log.d("ADS_LOG", "Награда получена: +5 Undos");
                });

                // Подгружаем следующую рекламу для будущего использования
                loadRewardedAd();
            } else {
                // Если реклама еще не загрузилась в памяти
                Toast.makeText(this, "Ad is loading, please try again...", Toast.LENGTH_SHORT).show();
                // На всякий случай пробуем запустить загрузку
                loadRewardedAd();
            }

            confirmDialog.dismiss();
        });

        confirmDialog.show();
    }


    private void loadCheatData() {
        SharedPreferences prefs = getSharedPreferences("GamePrefs", MODE_PRIVATE);
        // Для читов тоже можно сделать проверку по дате, если хочешь обновлять лимит каждый день
        String lastDate = prefs.getString("last_cheat_date", "");
        String currentDate = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());

        if (!currentDate.equals(lastDate)) {
            cheatCount = MAX_CHEAT;
            prefs.edit().putString("last_cheat_date", currentDate).apply();
        } else {
            cheatCount = prefs.getInt("cheat_count", MAX_CHEAT);
        }
    }

    public void useCheat() {
        if (cheatCount > 0) {
            cheatCount--;
            getSharedPreferences("GamePrefs", MODE_PRIVATE)
                    .edit()
                    .putInt("cheat_count", cheatCount)
                    .apply();
            // Чтобы точки сразу гасли в MainView
            if (gameView != null) {
                gameView.invalidate();
            }
        }
    }

    public void addCheatFromAds() {
        this.cheatCount = 3; // Убедись, что переменная именно эта
        getSharedPreferences("GamePrefs", MODE_PRIVATE)
                .edit()
                .putInt("cheat_count", 3)
                .apply();
    }

    public int getCheatCount() {
        return cheatCount;
    }

    public void showRewardedAdForCheat() {
        // 1. Цвета и настройки стиля (как в твоем Undo)
        int textColor = isDarkMode ? Color.parseColor("#F9F6F2") : Color.parseColor("#776E65");
        String cancelBtnColor = "#8F7A66";
        String actionBtnColor = "#F67C5F";

        // 2. Создание главного контейнера
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(25), dp(30), dp(25), dp(25));
        layout.setGravity(Gravity.CENTER);

        // 3. Заголовок (Жирный, CAPS)
        TextView title = new TextView(this);
        title.setText("LIMIT REACHED"); // Оставил тот же заголовок для стиля
        title.setTextSize(22);
        try {
            Typeface montserratSemiBold = Typeface.createFromAsset(getAssets(), "fonts/montserrat_semibold.ttf");
            title.setTypeface(montserratSemiBold);
        } catch (Exception e) {
            title.setTypeface(Typeface.DEFAULT_BOLD);
        }
        title.setTextColor(textColor);
        title.setPadding(0, 0, 0, dp(15));
        layout.addView(title);

        // 4. Сообщение
        TextView msg = new TextView(this);
        msg.setText("Watch a video to get 3 more cheats."); // Текст под читы
        msg.setGravity(Gravity.CENTER);
        msg.setTextSize(17);
        msg.setTextColor(textColor);
        try {
            Typeface montserratBold = Typeface.createFromAsset(getAssets(), "fonts/montserrat_semibold.ttf");
            msg.setTypeface(montserratBold, Typeface.BOLD);
        } catch (Exception e) {
            msg.setTypeface(Typeface.DEFAULT_BOLD);
        }
        msg.setPadding(0, 0, 0, dp(25));
        layout.addView(msg);

        // 5. Контейнер для кнопок
        LinearLayout buttons = new LinearLayout(this);
        buttons.setOrientation(LinearLayout.HORIZONTAL);

        // Твои кастомные кнопки
        TextView btnCancel = createCustomButton("CANCEL", cancelBtnColor);
        TextView btnWatch = createCustomButton("WATCH", actionBtnColor);

        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, dp(50), 1f);
        p.setMargins(0, 0, dp(12), 0);

        buttons.addView(btnCancel, p);
        buttons.addView(btnWatch, new LinearLayout.LayoutParams(0, dp(50), 1f));

        layout.addView(buttons);

        // 6. Создание и настройка AlertDialog
        final android.app.AlertDialog confirmDialog = new android.app.AlertDialog.Builder(this).setView(layout).create();

        if (confirmDialog.getWindow() != null) {
            confirmDialog.getWindow().setBackgroundDrawableResource(R.drawable.leaderboard_window_bg);
        }

        // 7. Обработка нажатия CANCEL
        btnCancel.setOnClickListener(v -> {
            v.startAnimation(clickAnim);
            confirmDialog.dismiss();
        });

        // 8. Обработка нажатия WATCH (Логика рекламы для ЧИТОВ)
        btnWatch.setOnClickListener(v -> {
            v.startAnimation(clickAnim);

            if (rewardedAd != null) {
                rewardedAd.show(this, rewardItem -> {
                    // ВАЖНО: вызываем пополнение именно читов
                    addCheatFromAds();

                    // Принудительно обновляем View
                    if (gameView != null) {
                        gameView.invalidate();
                    }
                    Log.d("ADS_LOG", "Награда получена: +3 Cheats");
                });

                loadRewardedAd();
            } else {
                Toast.makeText(this, "Ad is loading, please try again...", Toast.LENGTH_SHORT).show();
                loadRewardedAd();
            }

            confirmDialog.dismiss();
        });

        confirmDialog.show();
    }


    public void showPrivacyPolicy() {
        int textColor = isDarkMode ? Color.parseColor("#F9F6F2") : Color.parseColor("#776E65");
        String btnColor = "#8F7A66";

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(25), dp(25), dp(25), dp(25));

        TextView title = new TextView(this);
        title.setText("PRIVACY POLICY");
        title.setTextSize(22);
        title.setGravity(Gravity.CENTER);
        try {
            title.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/montserrat_semibold.ttf"), Typeface.BOLD);
        } catch (Exception e) { title.setTypeface(Typeface.DEFAULT_BOLD); }
        title.setTextColor(textColor);
        title.setPadding(0, 0, 0, dp(20));
        layout.addView(title);

        ScrollView scrollView = new ScrollView(this);
        LinearLayout.LayoutParams scrollParams = new LinearLayout.LayoutParams(-1, dp(350));
        scrollView.setLayoutParams(scrollParams);

        TextView policyText = new TextView(this);

        // Получаем текст из CDATA
        String rawHtml = getString(R.string.privacy_policy_text);

        // Обрабатываем HTML
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            policyText.setText(Html.fromHtml(rawHtml, Html.FROM_HTML_MODE_LEGACY));
        } else {
            policyText.setText(Html.fromHtml(rawHtml));
        }

        // Делаем ссылки кликабельными
        policyText.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
        policyText.setTextSize(14);
        policyText.setTextColor(textColor);
        policyText.setLineSpacing(0, 1.2f);

        // Параметры, чтобы текст не схлопывался
        policyText.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));

        scrollView.addView(policyText);
        layout.addView(scrollView);

        TextView btnClose = createCustomButton("CLOSE", btnColor);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, dp(50));
        p.topMargin = dp(20);
        layout.addView(btnClose, p);

        final android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this).setView(layout).create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(R.drawable.leaderboard_window_bg);
        }

        btnClose.setOnClickListener(v -> {
            if (clickAnim != null) v.startAnimation(clickAnim);
            dialog.dismiss();
        });

        dialog.show();
    }


    private int currentStep = 1;
    private boolean isTutorialFinished = false; // Ключевой предохранитель

    public void showInteractiveTutorial() {
        try {
            isTutorialFinished = false; // Сбрасываем при каждом запуске
            final ViewGroup root = (ViewGroup) getWindow().getDecorView().findViewById(android.R.id.content);
            final View overlay = getLayoutInflater().inflate(R.layout.overlay_tutorial, null);

            final ImageView tutoArrow = overlay.findViewById(R.id.tuto_arrow);
            final TextView instruction = overlay.findViewById(R.id.tuto_instruction);
            final TextView stepText = overlay.findViewById(R.id.tuto_step_text);
            final TextView btnFinish = overlay.findViewById(R.id.btn_tutorial_finish);

            currentStep = 1;

            android.view.GestureDetector detector = new android.view.GestureDetector(this, new android.view.GestureDetector.SimpleOnGestureListener() {
                @Override
                public boolean onFling(android.view.MotionEvent e1, android.view.MotionEvent e2, float velocityX, float velocityY) {
                    // БЛОКИРОВКА: Если туториал закончен, жесты не обрабатываем
                    if (isTutorialFinished) return false;

                    float dx = e2.getX() - e1.getX();
                    float dy = e2.getY() - e1.getY();

                    if (Math.abs(dx) > Math.abs(dy)) {
                        if (Math.abs(dx) > 100) {
                            if (dx > 0) processStep(1, tutoArrow, instruction, stepText, btnFinish, overlay);
                            else processStep(3, tutoArrow, instruction, stepText, btnFinish, overlay);
                        }
                    } else {
                        if (Math.abs(dy) > 100) {
                            if (dy > 0) processStep(2, tutoArrow, instruction, stepText, btnFinish, overlay);
                            else processStep(4, tutoArrow, instruction, stepText, btnFinish, overlay);
                        }
                    }
                    return true;
                }
            });

            overlay.setOnTouchListener((v, event) -> {
                if (isTutorialFinished) return true; // Игнорируем касания в финале

                if (event.getAction() == MotionEvent.ACTION_MOVE) {
                    tutoArrow.setScaleX(1.1f);
                    tutoArrow.setScaleY(0.9f);
                } else if (event.getAction() == MotionEvent.ACTION_UP) {
                    tutoArrow.animate().scaleX(1f).scaleY(1f).setDuration(200).start();
                }
                return detector.onTouchEvent(event);
            });

            root.addView(overlay);
            refreshTutoUI(tutoArrow, instruction, stepText);

        } catch (Exception e) {
            android.util.Log.e("EREKE_DEBUG", "Tutorial Error: " + e.getMessage());
        }
    }

    private void processStep(int swipe, ImageView icon, TextView instr, TextView step, TextView btn, View overlay) {
        if (isTutorialFinished) return;

        if (swipe == currentStep) {
            icon.setColorFilter(android.graphics.Color.parseColor("#4CAF50"));
            triggerVisualFeedback(overlay, true, icon);

            new android.os.Handler().postDelayed(() -> {
                icon.clearColorFilter();

                if (currentStep < 4) {
                    currentStep++;
                    // Плавная смена шага
                    instr.animate().translationY(-50f).alpha(0f).setDuration(150).withEndAction(() -> {
                        refreshTutoUI(icon, instr, step);
                        instr.setTranslationY(50f);
                        instr.animate().translationY(0f).alpha(1f).setDuration(150).start();
                    }).start();
                } else {
                    // --- ЖЕСТКИЙ ФИНАЛ (Чтобы ничего не моргало) ---
                    isTutorialFinished = true;

                    // 1. ОСТАНАВЛИВАЕМ все текущие анимации МГНОВЕННО
                    if (icon != null) icon.animate().cancel();
                    if (instr != null) instr.animate().cancel();

                    // 2. Сразу скрываем счетчик и руку
                    if (step != null) step.setVisibility(View.GONE);
                    if (icon != null) {
                        icon.animate().alpha(0f).scaleX(0f).scaleY(0f).setDuration(200).start();
                    }

                    // 3. Выводим LEGENDARY без задержек и лишних текстов
                    instr.setAlpha(1f);
                    instr.setTranslationY(0f); // Сбрасываем позицию, чтобы не улетел
                    instr.setText("LEGENDARY!");
                    instr.setTextColor(android.graphics.Color.parseColor("#FFD700"));

                    instr.animate()
                            .scaleX(1.4f).scaleY(1.4f)
                            .setDuration(500)
                            .setInterpolator(new android.view.animation.OvershootInterpolator())
                            .start();

                    if (btn != null) {
                        btn.setVisibility(View.VISIBLE);
                        btn.setAlpha(0f);
                        btn.animate().alpha(1f).setDuration(500).start();
                        btn.setOnClickListener(v -> {
                            if (overlay.getParent() != null) {
                                ((ViewGroup)overlay.getParent()).removeView(overlay);
                            }
                        });
                    }
                }
            }, 300);
        } else {
            triggerVisualFeedback(overlay, false, icon);
        }
    }

    private void refreshTutoUI(ImageView icon, TextView instr, TextView step) {
        if (icon == null || instr == null) return;

        step.setText("STEP " + currentStep + " / 4");

        // Подготовка: иконка маленькая и прозрачная
        icon.setScaleX(0.3f);
        icon.setScaleY(0.3f);
        icon.setAlpha(0f);

        // Устанавливаем направление
        switch (currentStep) {
            case 1: instr.setText("SWIPE RIGHT"); icon.setRotation(0); break;
            case 2: instr.setText("SWIPE DOWN"); icon.setRotation(90); break;
            case 3: instr.setText("SWIPE LEFT"); icon.setRotation(180); break;
            case 4: instr.setText("SWIPE UP"); icon.setRotation(270); break;
        }

        // "Дорогой" вылет: иконка выпрыгивает с сильным отскоком (Overshoot)
        icon.animate()
                .scaleX(1f).scaleY(1f)
                .alpha(1f)
                .setDuration(600)
                // Коэффициент 3.0f дает сочный "пружинистый" эффект
                .setInterpolator(new android.view.animation.OvershootInterpolator(3.0f))
                .start();

        // Запускаем цикл "дыхания" (Idle Animation)
        ObjectAnimator pulseX = ObjectAnimator.ofFloat(icon, "scaleX", 1f, 1.1f);
        ObjectAnimator pulseY = ObjectAnimator.ofFloat(icon, "scaleY", 1f, 1.1f);
        pulseX.setDuration(1000);
        pulseY.setDuration(1000);
        pulseX.setRepeatCount(ValueAnimator.INFINITE);
        pulseY.setRepeatCount(ValueAnimator.INFINITE);
        pulseX.setRepeatMode(ValueAnimator.REVERSE);
        pulseY.setRepeatMode(ValueAnimator.REVERSE);
        pulseX.start();
        pulseY.start();

        float moveX = 0, moveY = 0;
        if (currentStep == 1) moveX = 150f;      // Вправо
        else if (currentStep == 2) moveY = 150f; // Вниз
        else if (currentStep == 3) moveX = -150f; // Влево
        else if (currentStep == 4) moveY = -150f; // Вверх

        icon.setTranslationX(0); icon.setTranslationY(0);
        icon.animate()
                .translationX(moveX)
                .translationY(moveY)
                .setDuration(1200)
                .setStartDelay(200)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(() -> refreshTutoUI(icon, instr, step)) // Цикличный показ
                .start();
    }


    private void triggerVisualFeedback(View overlay, boolean success, View targetIcon) {
        if (success) {
            // ВСПЫШКА: Создаем временный белый фон
            final View flash = new View(this);
            flash.setBackgroundColor(android.graphics.Color.WHITE);
            flash.setAlpha(0.4f);
            ((ViewGroup)overlay).addView(flash, new ViewGroup.LayoutParams(-1, -1));

            flash.animate().alpha(0f).setDuration(300).withEndAction(() ->
                    ((ViewGroup)overlay).removeView(flash)).start();
        } else {
            // ТРЯСКА (Shake): Если свайп неверный
            targetIcon.animate()
                    .translationXBy(20f).setDuration(50)
                    .withEndAction(() -> targetIcon.animate().translationXBy(-40f).setDuration(50)
                            .withEndAction(() -> targetIcon.animate().translationXBy(20f).setDuration(50).start()).start()).start();
        }
    }

    private void showShopDialog() {
        // 1. Создаем основной контейнер
        LinearLayout shopLayout = new LinearLayout(this);
        shopLayout.setOrientation(LinearLayout.VERTICAL);
        shopLayout.setPadding(dp(20), dp(20), dp(20), dp(20));
        shopLayout.setBackgroundResource(R.drawable.leaderboard_window_bg);

        // --- ВЕРХНЯЯ ПАНЕЛЬ (ТОЛЬКО ЗАГОЛОВОК) ---
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, dp(20));

        TextView title = new TextView(this);
        title.setText("SHOP");
        title.setTextSize(22);
        title.setTextColor(Color.parseColor("#776E65"));
        title.setTypeface(null, Typeface.BOLD);
        // Растягиваем заголовок на всю ширину, так как кнопки баланса больше нет
        title.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        title.setGravity(Gravity.CENTER); // Центруем для красоты

        // УДАЛЕНО: Весь блок coinBox и tvShopCoinBalance

        header.addView(title);
        shopLayout.addView(header);

        // --- БЛОК COMING SOON ---
        TextView tvComingSoon = new TextView(this);
        // Убрали призыв "Копите монеты", оставили только суть
        tvComingSoon.setText("NEW FEATURES COMING SOON!\n\n" +
                "We are working on magical boosters and special items to help you reach 2048.\n\n" +
                "Stay tuned for the next update!");
        tvComingSoon.setGravity(Gravity.CENTER);
        tvComingSoon.setPadding(0, dp(30), 0, dp(30));
        tvComingSoon.setTextSize(16);
        tvComingSoon.setTextColor(Color.parseColor("#8F7A66"));
        tvComingSoon.setTypeface(null, Typeface.ITALIC);

        shopLayout.addView(tvComingSoon);

        // Товары остаются закомментированными
    /*
    addShopItemToLayout(shopLayout, ...);
    */

        // 3. Показ диалога
        AlertDialog shopDialog = new AlertDialog.Builder(this)
                .setView(shopLayout)
                .setCancelable(true)
                .create();

        if (shopDialog.getWindow() != null) {
            shopDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        shopDialog.show();
    }

    // УНИВЕРСАЛЬНЫЙ МЕТОД СОЗДАНИЯ СТРОКИ ТОВАРА
    private void addShopItemToLayout(LinearLayout parent, String name, String desc, int price, String type, String emoji) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, dp(12), 0, dp(12));
        row.setGravity(Gravity.CENTER_VERTICAL);

        // Иконка
        FrameLayout iconFrame = new FrameLayout(this);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(50), dp(50));
        iconParams.rightMargin = dp(12);
        iconFrame.setLayoutParams(iconParams);
        iconFrame.setBackgroundResource(R.drawable.round_button_bg);

        TextView tvEmoji = new TextView(this);
        tvEmoji.setText(emoji);
        tvEmoji.setGravity(Gravity.CENTER);
        tvEmoji.setTextSize(22);
        iconFrame.addView(tvEmoji);

        // Текст
        LinearLayout textBlock = new LinearLayout(this);
        textBlock.setOrientation(LinearLayout.VERTICAL);
        textBlock.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));

        TextView tvName = new TextView(this);
        tvName.setText(name);
        tvName.setTextSize(17);
        tvName.setTextColor(Color.parseColor("#776E65"));
        tvName.setTypeface(null, Typeface.BOLD);

        TextView tvDesc = new TextView(this);
        tvDesc.setText(desc);
        tvDesc.setTextSize(12);
        tvDesc.setTextColor(Color.parseColor("#8F7A66"));

        textBlock.addView(tvName);
        textBlock.addView(tvDesc);

        // Кнопка
//        Button btnBuy = new Button(this);
//        btnBuy.setText(price + " 🪙");
//        btnBuy.setTextColor(Color.WHITE);
//        btnBuy.setAllCaps(false);
//        btnBuy.setBackgroundResource(R.drawable.score_bg_shape);
//        btnBuy.setLayoutParams(new LinearLayout.LayoutParams(dp(90), dp(42)));

//        btnBuy.setOnClickListener(v -> {
//            v.startAnimation(clickAnim);
//            if (spendCoins(price)) {
//                SharedPreferences prefs = getSharedPreferences("game_settings", MODE_PRIVATE);
//                int count = prefs.getInt("count_" + type, 0);
//                prefs.edit().putInt("count_" + type, count + 1).apply();
//
//                // --- ЛОГИКА АКТИВАЦИИ ---
//                if (type.equals("hammer")) {
//                    isHammerMode = true;
//                    if (shopDialog != null) shopDialog.dismiss(); // Закрываем магазин
//
//                }
//
////                if (tvShopCoinBalance != null) {
////                    tvShopCoinBalance.setText(getCoins() + " 🪙");
////                }
//            } else {
//
//            }
//        });

        row.addView(iconFrame);
        row.addView(textBlock);
//        row.addView(btnBuy);
        parent.addView(row);

        // Разделитель
        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(1)));
        divider.setBackgroundColor(Color.parseColor("#D6CDC4"));
        parent.addView(divider);
    }

// --- ЛОГИКА ДАННЫХ ---

    private int getCoins() {
        // Используем "game_settings" — это простое и понятное название файла
        SharedPreferences prefs = getSharedPreferences("game_settings", MODE_PRIVATE);
        return prefs.getInt("coins_balance", 0);
    }

    private void updateCoins(int amount) {
        SharedPreferences prefs = getSharedPreferences("game_settings", MODE_PRIVATE);
        int current = getCoins();
        // .apply() — это команда "сохранить прямо сейчас в фоновом режиме"
        prefs.edit().putInt("coins_balance", current + amount).apply();
    }
    private void spendCoins(int cost) {
//        int current = getCoins();
//        if (current >= cost) {
//            updateCoins(-cost);
//            return true;
//        }
        return;
    }

    public void rewardPlayerForTile(int tileValue) {
        // Получаем размер сетки
        SharedPreferences settings = getSharedPreferences("com.ereke.qadam2048", MODE_PRIVATE);
        int currentGridSize = settings.getInt("width", 4);

        int reward = 0;

        // --- БАЛАНС ДЛЯ 4x4 (Классика) ---
        if (currentGridSize == 4) {
            if (tileValue == 8)   reward = 0;   // Раньше было 0
            if (tileValue == 128)  reward = 0;   // Было 2
            if (tileValue == 256)  reward = 0;  // Было 5
            if (tileValue == 512)  reward = 0;  // Было 10
            if (tileValue == 1024) reward = 0; // Было 25
            if (tileValue == 2048) reward = 0; // Было 100
        }
        // --- БАЛАНС ДЛЯ 5x5 и 6x6 (Тут плитки собираются чаще) ---
        else {
            if (tileValue == 128)  reward = 0;   // Было 0 или 2
            if (tileValue == 256)  reward = 0;  // Было 2
            if (tileValue == 512)  reward = 0;  // Было 5
            if (tileValue == 1024) reward = 0;  // Было 15
            if (tileValue == 2048) reward = 0; // Было 50
        }

        if (reward > 0) {
            updateCoins(reward);

            // Сразу обновляем цифру в магазине
            if (tvShopCoinBalance != null) {
                tvShopCoinBalance.setText(getCoins() + " 🪙");
            }


        }
    }

    private void activateHammer() {
        isHammerMode = true;

    }

}
