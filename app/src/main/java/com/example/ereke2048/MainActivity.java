package com.example.ereke2048;

import static java.security.AccessController.getContext;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.DecelerateInterpolator;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends Activity {

    private MainView view;
    private TextView currentComboText = null;
    private ImageButton btnVibration;
    private android.widget.TextView tilesCounter;
    private String currentActiveMode = "CLASSIC"; // По умолчанию
    private boolean isVibrationEnabled = true; // По умолчанию включена

    private FrameLayout rootLayout;
    private MainView gameView;
    private ImageButton btnTheme;
    private ImageButton btnSettings;
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

    private TextView finalScoreText;
    private int bgColor;
    private int textColor;
    private RewardedAd rewardedAd;
    // Тестовый ID именно для REWARDED (не Interstitial!)
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

    private boolean isDarkMode = false;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 0. Splash Screen
        androidx.core.splashscreen.SplashScreen splashScreen =
                androidx.core.splashscreen.SplashScreen.installSplashScreen(this);

        super.onCreate(savedInstanceState);

        // 1. Настройка SharedPreferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isDarkMode = prefs.getBoolean("dark_mode", false);
        boolean isSoundEnabled = prefs.getBoolean("sound_enabled", true);
        boolean isCloudEnabled = prefs.getBoolean("cloud_enabled", false);

        // 2. Создаем rootLayout
        rootLayout = new FrameLayout(this);
        rootLayout.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        int bgColor = ContextCompat.getColor(this, isDarkMode ? R.color.background_dark : R.color.background_light);
        rootLayout.setBackgroundColor(bgColor);

        // 3. Инициализируем игру
        gameView = new MainView(this);
        gameView.setLayoutParams(new FrameLayout.LayoutParams(-1, -1));
        rootLayout.addView(gameView);

        // --- ПРАВАЯ СТОРОНА (Тема и Вибрация) ---

        // 4. Кнопка Темы
        btnTheme = new ImageButton(this);
        btnTheme.setImageResource(isDarkMode ? R.drawable.ic_sun : R.drawable.ic_moon);
        btnTheme.setBackgroundResource(R.drawable.round_button_bg);
        FrameLayout.LayoutParams themeParams = new FrameLayout.LayoutParams(dp(56), dp(56), Gravity.TOP | Gravity.END);
        themeParams.topMargin = dp(20);
        themeParams.rightMargin = dp(20);
        rootLayout.addView(btnTheme, themeParams);

        // 4.1. Кнопка Вибрации (под темой)
        btnVibration = new ImageButton(this);
        isVibrationEnabled = prefs.getBoolean("vibration_enabled", true);
        btnVibration.setImageResource(isVibrationEnabled ? R.drawable.ic_vibration_on : R.drawable.ic_vibration_off);
        btnVibration.setBackgroundResource(R.drawable.round_button_bg);
        FrameLayout.LayoutParams vibParams = new FrameLayout.LayoutParams(dp(56), dp(56), Gravity.TOP | Gravity.END);
        vibParams.rightMargin = dp(20);
        vibParams.topMargin = dp(86); // 20 + 56 + 10 зазор
        rootLayout.addView(btnVibration, vibParams);

        // --- ЛЕВАЯ СТОРОНА (Столбик: Настройки -> Лидерборд -> Челленджи) ---

        // 5. Кнопка Настроек (Самая верхняя слева)
        btnSettings = new ImageButton(this);
        btnSettings.setImageResource(R.drawable.ic_settings);
        btnSettings.setBackgroundResource(R.drawable.round_button_bg);
        FrameLayout.LayoutParams settingsParams = new FrameLayout.LayoutParams(dp(56), dp(56), Gravity.TOP | Gravity.START);
        settingsParams.topMargin = dp(20);
        settingsParams.leftMargin = dp(20);
        rootLayout.addView(btnSettings, settingsParams);

        // 5.1. Кнопка Таблицы Лидеров (ПОД настройками)
        btnLeaderboard = new ImageButton(this);
        btnLeaderboard.setImageResource(R.drawable.ic_leaderboard);
        btnLeaderboard.setBackgroundResource(R.drawable.round_button_bg);
        FrameLayout.LayoutParams lbParams = new FrameLayout.LayoutParams(dp(56), dp(56), Gravity.TOP | Gravity.START);
        lbParams.leftMargin = dp(20);
        lbParams.topMargin = dp(86); // 20 (отступ) + 56 (кнопка) + 10 (зазор)
        rootLayout.addView(btnLeaderboard, lbParams);

        // 5.2. Кнопка Испытаний/Молния (ПОД лидербордом)
        ImageButton btnChallenges = new ImageButton(this);
        btnChallenges.setImageResource(R.drawable.ic_lightning);
        btnChallenges.setBackgroundResource(R.drawable.round_button_bg);
        btnChallenges.setPadding(dp(12), dp(12), dp(12), dp(12));
        btnChallenges.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        FrameLayout.LayoutParams chalParams = new FrameLayout.LayoutParams(dp(56), dp(56), Gravity.TOP | Gravity.START);
        chalParams.leftMargin = dp(20);
        chalParams.topMargin = dp(152); // 86 + 56 + 10 зазор
        rootLayout.addView(btnChallenges, chalParams);

        setupHeartsUI();

        // --- СЛУШАТЕЛИ ---

        btnVibration.setOnClickListener(v -> {
            isVibrationEnabled = !isVibrationEnabled;
            btnVibration.setImageResource(isVibrationEnabled ? R.drawable.ic_vibration_on : R.drawable.ic_vibration_off);
            prefs.edit().putBoolean("vibration_enabled", isVibrationEnabled).apply();
            if (isVibrationEnabled) triggerVibration(20);
        });

        btnChallenges.setOnClickListener(v -> {
            triggerVibration(15);
            showChallengesDialog();
        });

        btnLeaderboard.setOnClickListener(v -> {
            triggerVibration(15);
            showLeaderboard();
        });

        // 6. Инфлейт панели настроек
        settingsView = getLayoutInflater().inflate(R.layout.view_settings_panel, rootLayout, false);
        settingsView.setVisibility(View.GONE);
        rootLayout.addView(settingsView);

        // 7. Оверлей Game Over
        createGameOverOverlay();

        // Логика кнопок настроек
        initSettingsLogic(prefs, isSoundEnabled, isDarkMode, isCloudEnabled);

        // 8. Реклама
        initAds();

        // 9. Firebase
        mDatabase = FirebaseDatabase.getInstance().getReference();

        long localBest = prefs.getLong("high score", 0);
        gameView.post(() -> {
            if (gameView.game != null) {
                gameView.game.highScore = localBest;
                if (prefs.getBoolean("cloud_enabled", false)) loadScoreFromCloud();
            }
        });

        // Устанавливаем контент
        setContentView(rootLayout);

        // ЗАПУСК ИГРЫ
        gameView.post(() -> {
            if (gameView.game != null) {
                load();
                boolean isAnyTile = false;
                for (int x = 0; x < gameView.game.grid.field.length; x++) {
                    for (int y = 0; y < gameView.game.grid.field[0].length; y++) {
                        if (gameView.game.grid.field[x][y] != null) { isAnyTile = true; break; }
                    }
                }
                if (!isAnyTile) gameView.game.newGame();
                gameView.invalidate();
            }
        });
    }

    // Вынес логику настроек для чистоты
    private void initSettingsLogic(SharedPreferences prefs, boolean isSound, boolean isDark, boolean isCloud) {
        // Настройка звука
        SwitchCompat soundSwitch = settingsView.findViewById(R.id.switch_sound);
        soundSwitch.setChecked(isSound);
        soundSwitch.setOnCheckedChangeListener((b, checked) ->
                prefs.edit().putBoolean("sound_enabled", checked).apply());

        // Настройка темы (с обновлением сердечек)
        SwitchCompat themeSwitch = settingsView.findViewById(R.id.switch_dark_mode);
        themeSwitch.setChecked(isDark);
        themeSwitch.setOnCheckedChangeListener((b, checked) -> {
            prefs.edit().putBoolean("dark_mode", checked).apply();
            applyAppTheme(checked);

            // ОБЯЗАТЕЛЬНО: обновляем иконки сердец, чтобы сменился цвет "пустых" жизней
            updateHeartsDisplay();

            // Если кнопка темы (солнце/луна) должна менять иконку сразу
            if (btnTheme != null) {
                btnTheme.setImageResource(checked ? R.drawable.ic_sun : R.drawable.ic_moon);
            }
        });

        // Настройка облачных сохранений
        SwitchCompat cloudSwitch = settingsView.findViewById(R.id.switch_cloud);
        cloudSwitch.setChecked(isCloud);
        cloudSwitch.setOnCheckedChangeListener((b, checked) -> {
            prefs.edit().putBoolean("cloud_enabled", checked).apply();
            if (checked) loadScoreFromCloud();
        });

        // Слушатели кнопок панели
        btnSettings.setOnClickListener(v -> showSettingsPanel(true));
        settingsView.findViewById(R.id.btnClose).setOnClickListener(v -> showSettingsPanel(false));

        // Кнопка быстрого переключения темы на главном экране
        btnTheme.setOnClickListener(v -> {
            triggerVibration(15);
            themeSwitch.setChecked(!themeSwitch.isChecked());
        });
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
        adView.setAdSize(AdSize.BANNER);
        FrameLayout.LayoutParams adParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.BOTTOM
        );
        rootLayout.addView(adView, adParams);
        adView.loadAd(new AdRequest.Builder().build());
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

    private void showSettingsPanel(boolean show) {
        if (show) {
            settingsView.setVisibility(View.VISIBLE);
            settingsView.setAlpha(0);
            settingsView.animate().alpha(1).setDuration(200).start();
        } else {
            settingsView.animate().alpha(0).setDuration(200).withEndAction(() ->
                    settingsView.setVisibility(View.GONE)).start();
        }
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
                    btnAd.setEnabled(false); // КНОПКА НЕ НАЖИМАЕТСЯ (ВАЖНО!)
                    btnAd.setAlpha(0.3f);    // Почти прозрачная
                    btnAd.setBackgroundTintList(ColorStateList.valueOf(Color.GRAY)); // Серая

                    // Опционально: можно вообще скрыть кнопку, если жизней 0
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
                long savedBest = prefs.getLong("high score", 0);

                // Получаем ник игрока из памяти
                String savedName = getSharedPreferences("ereke_prefs", MODE_PRIVATE).getString("user_name", null);

                if (currentScore >= savedBest && currentScore > 0) {
                    // НОВЫЙ РЕКОРД
                    prefs.edit().putLong("high score", currentScore).apply();
                    gameView.game.highScore = currentScore;

                    if (savedName == null) {
                        showNameInputDialog(currentScore);
                    } else {
                        updateLeaderboardWithName(savedName, currentScore);
                    }

                    finalScoreText.setText("NEW RECORD\n" + currentScore);
                    finalScoreText.setTextColor(Color.parseColor("#EDC22E")); // Золотой цвет
                } else {
                    // ОБЫЧНЫЙ СЧЕТ
                    if (savedName != null) {
                        updateLeaderboardWithName(savedName, savedBest);
                    }

                    finalScoreText.setText("SCORE\n" + currentScore);
                    finalScoreText.setTextColor(Color.parseColor("#776E65")); // Стандартный цвет
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
            saveScoreToCloud(gameView.game.highScore);
        }


    }

    @Override
    protected void onResume() {
        super.onResume();

        // 1. Реклама
        if (adView != null) adView.resume();

        // 2. Настройка рекорда
        SharedPreferences prefs = android.preference.PreferenceManager.getDefaultSharedPreferences(this);
        long localBest = prefs.getLong("high score", 0);

        if (gameView != null && gameView.game != null) {
            // Проверяем: если на поле НЕТ плиток, значит игра реально только запустилась
            // и нам нужно загрузить старый сейв.
            // Если же плитки есть (как после рекламы), то НИЧЕГО НЕ ГРУЗИМ!
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
                load(); // Грузим только если поле пустое
            }

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

        for (int x = 0; x < field.length; x++) {
            for (int y = 0; y < field[0].length; y++) {
                editor.putInt(x + " " + y, field[x][y] != null ? field[x][y].getValue() : 0);
                editor.putInt(UNDO_GRID + x + " " + y, undoField[x][y] != null ? undoField[x][y].getValue() : 0);
            }
        }

        editor.putLong(SCORE, gameView.game.score);
        editor.putLong(HIGH_SCORE, gameView.game.highScore);
        editor.putLong(UNDO_SCORE, gameView.game.lastScore);
        editor.putBoolean(CAN_UNDO, gameView.game.canUndo);
        editor.putInt(GAME_STATE, gameView.game.gameState);
        editor.putInt(UNDO_GAME_STATE, gameView.game.lastGameState);

        // Добавляем флаг, что сохранение существует
        editor.putBoolean("hasGameSaved", true);
        editor.apply();
    }

    private void load() {
        try {
            if (gameView == null || gameView.game == null) return;

            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

            // Если сохранений никогда не было — выходим
            if (!settings.getBoolean("hasGameSaved", false)) return;

            gameView.game.aGrid.cancelAnimations();

            for (int x = 0; x < gameView.game.grid.field.length; x++) {
                for (int y = 0; y < gameView.game.grid.field[0].length; y++) {
                    // Загружаем основное поле
                    int val = settings.getInt(x + " " + y, 0);
                    gameView.game.grid.field[x][y] = (val != 0) ? new Tile(x, y, val) : null;

                    // Загружаем Undo поле
                    int undoVal = settings.getInt(UNDO_GRID + x + " " + y, 0);
                    gameView.game.grid.undoField[x][y] = (undoVal != 0) ? new Tile(x, y, undoVal) : null;
                }
            }

            gameView.game.score = settings.getLong(SCORE, 0);
            gameView.game.highScore = settings.getLong(HIGH_SCORE, 0);
            gameView.game.lastScore = settings.getLong(UNDO_SCORE, 0);
            gameView.game.canUndo = settings.getBoolean(CAN_UNDO, false);
            gameView.game.gameState = settings.getInt(GAME_STATE, 0);
            gameView.game.lastGameState = settings.getInt(UNDO_GAME_STATE, 0);

            gameView.invalidate();
        } catch (Exception e) {
            Log.e("LOAD_ERROR", "Не удалось загрузить прогресс: " + e.getMessage());
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

            // 2. СБРАСЫВАЕМ ТАЙМЕР (Самое важное!)
            if (isChallengeMode) {
                startChallengeTimer(); // Этот вызов внутри себя должен делать .cancel() старому
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void showLeaderboard() {
        final Typeface montserrat = Typeface.createFromAsset(getAssets(), "fonts/montserrat_semibold.ttf");

        mDatabase.child("leaderboard").orderByChild("score").limitToLast(10)
                .get().addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {

                        // Главный контейнер (вертикальный)
                        android.widget.LinearLayout listContainer = new android.widget.LinearLayout(this);
                        listContainer.setOrientation(android.widget.LinearLayout.VERTICAL);
                        listContainer.setPadding(dp(16), dp(10), dp(16), dp(10));

                        // 1. Добавляем заголовок прямо в контейнер (в самый верх)
                        TextView titleView = new TextView(this);
                        titleView.setText("🏆 GLOBAL RANKING");
                        titleView.setTextSize(24);
                        titleView.setGravity(Gravity.CENTER);
                        titleView.setPadding(0, dp(15), 0, dp(15));
                        titleView.setTextColor(Color.parseColor("#776E65"));
                        titleView.setTypeface(montserrat);
                        listContainer.addView(titleView);

                        List<DataSnapshot> list = new ArrayList<>();
                        for (DataSnapshot ds : task.getResult().getChildren()) list.add(ds);
                        Collections.reverse(list);

                        int rank = 1;
                        for (DataSnapshot ds : list) {
                            View itemView = getLayoutInflater().inflate(R.layout.item_leaderboard, null);

                            TextView rT = itemView.findViewById(R.id.rankText);
                            TextView nT = itemView.findViewById(R.id.nameText);
                            TextView sT = itemView.findViewById(R.id.scoreText);

                            rT.setTypeface(montserrat);
                            nT.setTypeface(montserrat);
                            sT.setTypeface(montserrat);

                            String name = ds.child("username").getValue(String.class);
                            Long s = ds.child("score").getValue(Long.class);

                            rT.setText(rank == 1 ? "🥇" : rank == 2 ? "🥈" : rank == 3 ? "🥉" : String.valueOf(rank));
                            nT.setText(name != null ? name : "Unknown");
                            sT.setText(String.valueOf(s != null ? s : 0));

                            // Анимация строк
                            itemView.setAlpha(0f);
                            itemView.setTranslationY(40f);
                            itemView.animate().alpha(1f).translationY(0f).setDuration(450).setStartDelay(rank * 80L).start();

                            listContainer.addView(itemView);
                            rank++;
                        }

                        // Кнопка закрытия (внизу списка)
                        android.widget.Button closeBtn = new android.widget.Button(this);
                        closeBtn.setText("CLOSE");
                        closeBtn.setBackground(null); // Прозрачный фон
                        closeBtn.setTextColor(Color.parseColor("#8F7A66"));
                        closeBtn.setTypeface(montserrat);
                        closeBtn.setTextSize(18);
                        listContainer.addView(closeBtn);

                        android.widget.ScrollView scrollView = new android.widget.ScrollView(this);
                        scrollView.addView(listContainer);

                        runOnUiThread(() -> {
                            // Создаем диалог БЕЗ стандартного заголовка (используем наш titleView)
                            android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                                    .setView(scrollView)
                                    .create();

                            // Магия дизайна заднего борда
                            if (dialog.getWindow() != null) {
                                // 1. Делаем системный фон прозрачным
                                dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
                            }

                            dialog.show();

                            // 2. Применяем наш закругленный фон к самому окну
                            android.view.Window window = dialog.getWindow();
                            if (window != null) {
                                window.setBackgroundDrawableResource(R.drawable.leaderboard_window_bg);

                                // 3. Настраиваем ширину (90% экрана)
                                android.view.WindowManager.LayoutParams lp = new android.view.WindowManager.LayoutParams();
                                lp.copyFrom(window.getAttributes());
                                lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
                                lp.height = android.view.WindowManager.LayoutParams.WRAP_CONTENT;
                                window.setAttributes(lp);

                                // Затемнение заднего плана
                                window.setDimAmount(0.7f);
                            }

                            closeBtn.setOnClickListener(v -> dialog.dismiss());
                        });
                    }
                });
    }
    private void showNameInputDialog(long scoreToSave) {
        // Подгружаем шрифт
        final Typeface montserrat = Typeface.createFromAsset(getAssets(), "fonts/montserrat_semibold.ttf");

        // Создаем EditText
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("ENTER YOUR NAME");
        input.setHintTextColor(Color.parseColor("#BBADA0"));
        input.setTextColor(Color.parseColor("#776E65"));
        input.setTypeface(montserrat);
        input.setGravity(Gravity.CENTER);
        input.setFilters(new android.text.InputFilter[] {new android.text.InputFilter.LengthFilter(12)});

        // Контейнер для отступов
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
                    .setCancelable(false) // Чтобы нельзя было закрыть, не сохранив
                    .setPositiveButton("SAVE", (d, which) -> {
                        String name = input.getText().toString().trim();
                        if (name.isEmpty()) name = "Player";

                        // Сохраняем имя
                        getSharedPreferences("ereke_prefs", MODE_PRIVATE)
                                .edit().putString("user_name", name).apply();

                        // Отправляем в Firebase
                        updateLeaderboardWithName(name, scoreToSave);

                        launchConfetti();
                    })
                    .create();

            dialog.show();

            // Стилизуем задник как у Лидерборда
            if (dialog.getWindow() != null) {
                dialog.getWindow().setBackgroundDrawableResource(R.drawable.leaderboard_window_bg);
                dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setTextColor(Color.parseColor("#8F7A66"));
                dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setTypeface(montserrat);
            }
        });
    }
    private void saveScoreToCloud(long score) {
        String userId = android.provider.Settings.Secure.getString(getContentResolver(),
                android.provider.Settings.Secure.ANDROID_ID);
        if (mDatabase != null) {
            mDatabase.child("users").child(userId).child("highScore").setValue(score)
                    .addOnSuccessListener(aVoid -> Log.d("FIREBASE", "Score synced!"))
                    .addOnFailureListener(e -> Log.e("FIREBASE", "Sync failed: " + e.getMessage()));
        }
    }
    private void updateLeaderboardWithName(String name, long score) {
        // Получаем уникальный ID устройства
        String userId = android.provider.Settings.Secure.getString(getContentResolver(),
                android.provider.Settings.Secure.ANDROID_ID);

        HashMap<String, Object> userEntry = new HashMap<>();
        userEntry.put("username", name);
        userEntry.put("score", score);
        userEntry.put("timestamp", System.currentTimeMillis());

        // Отправляем в ветку leaderboard/ID_УСТРОЙСТВА
        mDatabase.child("leaderboard").child(userId).setValue(userEntry)
                .addOnSuccessListener(aVoid -> Log.d("Firebase", "Score updated!"))
                .addOnFailureListener(e -> Log.e("Firebase", "Failed to update", e));
    }
    private void loadScoreFromCloud() {
        String userId = android.provider.Settings.Secure.getString(getContentResolver(),
                android.provider.Settings.Secure.ANDROID_ID);

        mDatabase.child("users").child(userId).child("highScore").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                long cloudScore = 0;
                if (task.getResult().exists()) {
                    Object val = task.getResult().getValue();
                    cloudScore = (val instanceof Long) ? (long) val : 0;
                }

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                long localScore = prefs.getLong("high score", 0);

                // ЛОГИКА "ПОБЕЖДАЕТ СИЛЬНЕЙШИЙ"
                if (cloudScore > localScore) {
                    // В облаке рекорд больше -> обновляем телефон
                    prefs.edit().putLong("high score", cloudScore).apply();
                    if (gameView != null && gameView.game != null) {
                        gameView.game.highScore = cloudScore;
                        runOnUiThread(() -> gameView.invalidate());
                    }
                    Log.d("SYNC", "Загрузили рекорд из облака: " + cloudScore);
                }
                else if (localScore > cloudScore) {
                    // В телефоне рекорд больше -> пушим его в облако прямо сейчас!
                    saveScoreToCloud(localScore);
                    Log.d("SYNC", "Отправили локальный рекорд в облако: " + localScore);
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
        container.addView(createChallengeCard("CLASSIC", "Chill & Swipe. No rush.", "#8F7A66", () -> {
            if (gameView.game.score > 0 && !gameView.game.gameLost()) {
                showSwitchConfirmation("CLASSIC", mainDialog);
            } else {
                executeSwitch("CLASSIC");
                mainDialog.dismiss();
            }
        }));

        container.addView(createSpace());

        // КНОПКА TIME ATTACK
        container.addView(createChallengeCard("TIME ATTACK", "3 seconds per move!", "#952509", () -> {
            if (gameView.game.score > 0 && !gameView.game.gameLost()) {
                showSwitchConfirmation("TIME ATTACK", mainDialog);
            } else {
                executeSwitch("TIME ATTACK");
                mainDialog.dismiss();
            }
        }));

        mainDialog.show();
        if (mainDialog.getWindow() != null) {
            mainDialog.getWindow().setBackgroundDrawableResource(R.drawable.leaderboard_window_bg);
        }
    }

    // 🔥 КРАСИВОЕ ПОДТВЕРЖДЕНИЕ
    private void showSwitchConfirmation(final String targetMode, final android.app.AlertDialog parentDialog) {
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
            executeSwitch(targetMode);
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
    private void executeSwitch(String mode) {
        if (mode.equals("CLASSIC")) {
            currentActiveMode = "CLASSIC";
            isChallengeMode = false;

            // 1. Останавливаем сам таймер (чтобы плитки не летели)
            if (challengeTimer != null) {
                challengeTimer.cancel();
                challengeTimer = null; // Зануляем для верности
            }

            // 2. Скрываем визуальные элементы (полоску и цифры)
            if (timerBar != null) {
                timerBar.clearAnimation();
                timerBar.setVisibility(View.GONE);
            }
            if (tilesCounter != null) {
                tilesCounter.setVisibility(View.GONE);
            }

            // 3. Вызываем твой старый метод скрытия (если там есть доп. логика)
            stopAndHideTimer();

        } else if (mode.equals("TIME ATTACK")) {
            currentActiveMode = "TIME ATTACK";
            isChallengeMode = true;

            // Показываем всё обратно
            positionTimerUnderLogo();
            if (timerBar != null) {
                timerBar.setVisibility(View.VISIBLE);
                timerBar.setProgress(100); // Сбрасываем прогресс на макс
            }
            if (tilesCounter != null) {
                tilesCounter.setVisibility(View.VISIBLE);
            }

            // Запускаем таймер заново
            startChallengeTimer();
        }

        // Сбрасываем саму игру
        gameView.game.newGame();
        gameView.game.canUndo = false;
        gameView.invalidate();
    }

    // 🔥 НОВЫЙ МЕТОД ДЛЯ ЗАЩИТЫ ОТ СЛУЧАЙНОГО НАЖАТИЯ
    private void confirmModeChange(Runnable onConfirm, android.app.AlertDialog parentDialog) {
        // Если очков 0 или игра уже проиграна — подтверждение не нужно
        if (gameView.game.score == 0 || gameView.game.gameLost()) {
            onConfirm.run();
            return;
        }

        // Если идет активная игра с очками — спрашиваем
        new android.app.AlertDialog.Builder(this)
                .setTitle("Смена режима")
                .setMessage("Текущий прогресс будет потерян. Вы уверены?")
                .setPositiveButton("Да", (d, which) -> onConfirm.run())
                .setNegativeButton("Отмена", (d, which) -> {
                    // Если нажали отмена, просто закрываем окно выбора режимов или оставляем
                    parentDialog.dismiss();
                })
                .show();
    }
    public void startChallengeTimer() {
        // 1. Убиваем ЛЮБОЙ старый таймер. Без этого будет хаос.
        if (challengeTimer != null) {
            challengeTimer.cancel();
            challengeTimer = null;
        }

        // Если мы уже проиграли или вышли из режима — не запускаем новый
        if (!isChallengeMode || gameView.game == null || gameView.game.gameLost()) {
            return;
        }
        // Внутри startChallengeTimer
        boolean vibrationEnabled = PreferenceManager.getDefaultSharedPreferences(this)
                .getBoolean("vibration_enabled", true); // Исправили ключ здесь
        challengeTimer = new android.os.CountDownTimer(3000, 30) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (gameView.game == null) return;

                // Считаем свободные клетки
                int availableCells = gameView.game.grid.getAvailableCells().size();

                // 🔥 ЭПИЧНОЕ МИГАНИЕ: если клеток 2 или меньше
                if (availableCells <= 3) {
                    startTimerWarningAnimation();

                    // Добавляем тактильную отдачу (вибрация каждую секунду)
                    if (vibrationEnabled && millisUntilFinished % 1000 < 50) {
                        vibrate(190, 255);
                    }
                } else {
                    stopTimerWarningAnimation();
                }

                if (timerBar != null) {
                    timerBar.setProgress((int) (millisUntilFinished / 30));
                }


                updateTilesCounter();
            }

            @Override
            public void onFinish() {
                runOnUiThread(() -> {
                    // Защита: если режим выключен или активити сдохло — выходим
                    if (!isChallengeMode || gameView.game == null) return;

                    // ПРОВЕРКА: Есть ли свободное место?
                    if (gameView.game.grid.isCellsAvailable()) {
                        // Место есть — наказываем игрока новой плиткой

                        if (vibrationEnabled) {
                            vibrate(140, 255);
                        }

                        gameView.game.addRandomTile();
                        updateTilesCounter();
                        gameView.invalidate();

                        // Проверяем: не стал ли этот спавн последним?
                        // (Используем твой tileMatchesAvailable внутри gameLost)
                        if (gameView.game.gameLost()) {
                            onGameOver();
                        } else {
                            // Если еще можно играть — запускаем таймер на следующий круг
                            startChallengeTimer();
                        }
                    } else {
                        // 🔥 ВОТ ТУТ СМЕРТЬ ЦИКЛА:
                        // Если клетки кончились, и время ВЫШЛО — это проигрыш.
                        // Мы не вызываем startChallengeTimer(), поэтому цикл рвется.
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
    private View createChallengeCard(String title, String desc, String color, Runnable onClick) {
        Typeface font = Typeface.createFromAsset(getAssets(), "fonts/montserrat_semibold.ttf");

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundResource(R.drawable.challenge_item_bg);
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


        boolean isActive = title.equals(currentActiveMode);

        if (isActive) {
            // Выделяем активный режим:
            // Можно сменить фон на более яркий или добавить обводку
            card.setBackgroundResource(R.drawable.challenge_item_active_bg);
            card.setAlpha(1.0f);
        } else {
            card.setBackgroundResource(R.drawable.challenge_item_bg);
            card.setAlpha(0.6f); // Неактивные режимы чуть тусклее
        }

        card.setOnClickListener(v -> {
            currentActiveMode = title; // Запоминаем выбор
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


    /**
     * Тряска экрана. Чем выше intensity, тем сильнее амплитуда.
     */
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

        // 1. Измеряем логотип
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/clearsans_bold.ttf"));
        paint.setTextSize(gameView.headerTextSize);

        float logoWidth = paint.measureText("2048");
        Paint.FontMetrics fm = paint.getFontMetrics();

        // 2. Позиционируем ТАЙМЕР (полоску)
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams((int) logoWidth, dp(4));
        // ОБЯЗАТЕЛЬНО TOP и START, чтобы отступы работали как координаты X и Y
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.leftMargin = gameView.startingX;

        // Расчет Y (под буквы)
        float ascent = fm.ascent;
        float descent = fm.descent;
        int textShiftY = (int) ((descent + ascent) / 2) * 2;
        int headerBaselineY = gameView.sYAll - textShiftY + 80;

        lp.topMargin = headerBaselineY + (int) descent + dp(4);
        timerBar.setLayoutParams(lp);

        // 3. Позиционируем СЧЕТЧИК (Tiles Counter) — СТРОГО СПРАВА
        // Устанавливаем ширину WRAP_CONTENT, чтобы он не растягивался на весь экран
        FrameLayout.LayoutParams tcParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );

        // Снова TOP и START для точного позиционирования
        tcParams.gravity = Gravity.TOP | Gravity.START;

        // СЧИТАЕМ X: Начало логотипа + Ширина логотипа + зазор
        int spacing = dp(8);
        tcParams.leftMargin = lp.leftMargin + (int) logoWidth + spacing;

        // СЧИТАЕМ Y: Ставим его чуть выше линии таймера, чтобы цифры были вровень с логотипом
        // Примерно на 40% высоты шрифта выше базовой линии
        tcParams.topMargin = headerBaselineY - (int) (Math.abs(ascent) * -0.2f);

        tilesCounter.setLayoutParams(tcParams);

        // Подбираем размер шрифта
        tilesCounter.setTextSize(TypedValue.COMPLEX_UNIT_PX, gameView.headerTextSize / 5.2f);

        // Чтобы наверняка не перекрывалось
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
}
