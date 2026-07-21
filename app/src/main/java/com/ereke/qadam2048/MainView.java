package com.ereke.qadam2048;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;


import androidx.core.content.ContextCompat;

import java.util.ArrayList;

public class MainView extends View {

    // Находим эти строки и меняем private на public
    public int cellSize = 0;
    public int gridWidth = 0;
    private boolean pendingThemeChange = false;
    private String comboText = "";
    private float comboAlpha = 0f;
    private float comboYOffset = 0f;
    private long comboStartTime = 0;
    private Drawable leaderboardIcon;
    private Drawable modsIcon;

    private float sYInstructions;      // Координата Y текста инструкций
    private float instructionsTextSize; // Размер шрифта инструкций

    private int sXLeaderboard, sYLeaderboard;
    private int sXMods, sYMods;


    public int startingY; // Верхняя граница сетки с плитками
    public int startingX;
    public int sYAll;

    private Object context;
    private Paint particlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private MainActivity activity;
    public interface RewardAdListener {
        void onShowRewardAd();
    }

    private RewardAdListener rewardAdListener;

    public void setRewardAdListener(RewardAdListener listener) {
        this.rewardAdListener = listener;
    }



    private Typeface boldFont;

    private int customTextColor = Color.BLACK;

    public boolean isDarkMode = false;



    // Internal variables
    Paint paint = new Paint();
    public MainGame game;
    private boolean rewardButtonShown = false;

    public boolean hasSaveState = false;
    private final int numCellTypes = 16;
    public boolean continueButtonEnabled = false;

    private int instructionTextColor = Color.BLACK;

    // Layout variables

    private float textSize = 0;
    private float cellTextSize = 0;

    private int TEXT_BLACK;
    private int TEXT_WHITE;
    private int TEXT_BROWN;
    private int TEXT_DARK_WHITE;
    private int TEXT_LIGHT_BLACK;


    public int endingX;
    public int endingY;
    private int textPaddingSize;
    private int iconPaddingSize;

    private int TextColor;


    private boolean inputEnabled = true;


    // поле класса
    private InputListener inputListener;

    // Assets
    private Drawable backgroundRectangle;
    private Drawable[] cellRectangle = new Drawable[numCellTypes];
    private BitmapDrawable[] bitmapCell = new BitmapDrawable[numCellTypes];
    private Drawable newGameIcon;
    private Drawable undoIcon;
    private Drawable cheatIcon;
    private Drawable lightUpRectangle;
    private Drawable fadeRectangle;
    private Bitmap background = null;
    private BitmapDrawable loseGameOverlay;
    private BitmapDrawable winGameContinueOverlay;
    private BitmapDrawable winGameFinalOverlay;

    // Text variables

    private int titleStartYAll;
    private int bodyStartYAll;
    private int eYAll;
    private int titleWidthHighScore;
    private int titleWidthScore;

    // Icon variables
    public int sYIcons;
    public int sXNewGame;
    public int sXUndo;
    public int sXCheat;
    public int iconSize;

    // Text values
    private String headerText;
    private String highScoreTitle;
    private String scoreTitle;
    private String instructionsText;
    private String winText;
    private String loseText;
    private String continueText;
    private String forNowText;
    private String endlessModeText;

    long lastFPSTime = System.nanoTime();
    long currentTime = System.nanoTime();

    float titleTextSize;
    float bodyTextSize;
    float headerTextSize;

    float gameOverTextSize;

    boolean refreshLastTime = true;

    static final int BASE_ANIMATION_TIME = 100000000;

    static final float MERGING_ACCELERATION = (float) -0.5;
    static final float INITIAL_VELOCITY = (1 - MERGING_ACCELERATION) / 4;
    private int logoRightX;


    public static void applyFontToWholeView(Context context, View rootView) {
        Typeface customTypeface = Typeface.createFromAsset(context.getAssets(), "fonts/clearsans_bold.ttf");
        FontUtils.applyCustomFont(rootView, customTypeface);
    }



    @Override
    public void onDraw(Canvas canvas) {
        if (game == null || game.grid == null || game.aGrid == null) {
            return;
        }

        // 🔥 ФИКС ТЕМЫ: Если фон был занулен в applyTheme, создаем его здесь.
        // На физ. устройстве это сработает тогда, когда ресурсы уже ТОЧНО обновились.
        if (background == null && getWidth() > 0 && getHeight() > 0) {
            createBackgroundBitmap(getWidth(), getHeight());
            createBitmapCells();
            createOverlays();
        }

        // 1. Рисуем статичный фон
        if (background != null) {
            canvas.drawBitmap(background, 0, 0, paint);
        }

        // 2. Рисуем динамические тексты
        drawHeader(canvas);
        drawInstructions(canvas);
        drawScoreText(canvas);

        if (!game.isActive() && !game.aGrid.isAnimationActive()) {
            drawNewGameButton(canvas, true);
        }

        // 4. Плитки и их анимации
        drawCells(canvas);

        // 5. Эффекты (частицы)
        drawParticles(canvas);

        // 7. Оверлеи конца игры
        if (!game.canContinue()) {
            drawEndlessText(canvas);
        }

        // 🔥 МОТОР АНИМАЦИИ
        if (game.aGrid.isAnimationActive() || !game.particles.isEmpty()) {
            tick();
            invalidate();
        } else if (!game.isActive() && refreshLastTime) {
            invalidate();
            refreshLastTime = false;
        }

        drawUndoIndicator(canvas);
        drawCheatIndicator(canvas);
    }


    @Override
    protected void onSizeChanged(int width, int height, int oldw, int oldh) {
        super.onSizeChanged(width, height, oldw, oldh);

        // 1. Простейшая проверка: если размеры нулевые, ничего не делаем
        if (width <= 0 || height <= 0) return;

        // 2. Инициализируем графику
        getLayout(width, height);
        createBackgroundBitmap(width, height);
        createBitmapCells();
        createOverlays();

        Log.d("MainView", "onSizeChanged завершен: " + width + "x" + height);
    }

    private void drawDrawable(Canvas canvas, Drawable draw, int startingX,
                              int startingY, int endingX, int endingY) {
        draw.setBounds(startingX, startingY, endingX, endingY);
        draw.draw(canvas);
    }


    // Убедись, что метод PUBLIC, чтобы MainActivity его видела
    public void setDarkMode(boolean darkMode) {
        this.isDarkMode = darkMode;
        this.background = null; // Сбрасываем кэшированный фон

        // Дополнительно сбрасываем кисти, если они зависят от темы
        if (paint != null) {
            paint.setAntiAlias(true);
        }
        // Если у тебя есть список объектов плиток, можно обнулить их цвета здесь
    }

    // До кучи добавим и этот, раз мы его вызываем
    public void notifyThemeChanged() {
        this.background = null;
    }
    private int getBackgroundForValue(int value) {
        int colorRes;
        switch (value) {
            case 0:    colorRes = R.color.tile_empty; break;
            case 2:    colorRes = R.color.tile_2; break;
            case 4:    colorRes = R.color.tile_4; break;
            case 8:    colorRes = R.color.tile_8; break;
            case 16:   colorRes = R.color.tile_16; break;
            case 32:   colorRes = R.color.tile_32; break;
            case 64:   colorRes = R.color.tile_64; break;
            case 128:  colorRes = R.color.tile_128; break;
            case 256:  colorRes = R.color.tile_256; break;
            case 512:  colorRes = R.color.tile_512; break;
            case 1024: colorRes = R.color.tile_1024; break;
            case 2048: colorRes = R.color.tile_2048; break;
            case 4096: colorRes = R.color.tile_4096; break;
            case 8192: colorRes = R.color.tile_8192; break;
            case 16384: colorRes = R.color.tile_16384; break;
            case 32768: colorRes = R.color.tile_32768; break;
            case 65536: colorRes = R.color.tile_65536; break;
            case 131072: colorRes = R.color.tile_131072; break;
            case 262144: colorRes = R.color.tile_262144; break;
            default:   colorRes = R.color.tile_empty; break;
        }

        // СТРАХОВКА: Используем принудительно контекст самого Activity (getContext() может быть багнутым)
        android.content.Context context = getContext();
        if (context instanceof android.app.Activity) {
            context = ((android.app.Activity) context).getApplicationContext();
        }

        // Если система всё равно пытается подсунуть темный цвет, хотя в коде у нас СВЕТЛАЯ тема:
        if (!isDarkMode) {
            // Если это плитка 0, 2 или 4 — хардкодим дефолтные светлые цвета на случай сбоя ресурсов Android
            if (value == 0) return Color.parseColor("#CDC1B4");
            if (value == 2) return Color.parseColor("#EEE4DA");
            if (value == 4) return Color.parseColor("#EDE0C8");
        } else {
            // На случай если включена ТЁМНАЯ тема, а Android пытается подсунуть светлые цвета
            if (value == 0) return Color.parseColor("#2A2A2A");
            if (value == 2) return Color.parseColor("#3E3E3E");
            if (value == 4) return Color.parseColor("#4A4A4A");
        }

        return androidx.core.content.ContextCompat.getColor(getContext(), colorRes);
    }
    private void drawCellText(Canvas canvas, int value, int sX, int sY) {
        // 1) Настройка Paint
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTypeface(Typeface.createFromAsset(getContext().getAssets(), "fonts/clearsans_bold.ttf"));
        paint.setTextAlign(Paint.Align.CENTER);  // выравнивание по центру по горизонтали
        paint.setFakeBoldText(true);

        // Цвет текста
        if (value >= 8) {
            paint.setColor(TEXT_WHITE);
        } else {
            paint.setColor(TEXT_BLACK);
        }


        // 2) Определяем размер шрифта в зависимости от значения
        float textSize = getTextSizeForValue(value);
        paint.setTextSize(textSize);

        // 3) Вычисляем координаты центра клетки
        float centerX = sX + cellSize / 2f;
        float centerY = sY + cellSize / 2f;

        // 4) Корректное вертикальное центрирование через FontMetrics
        Paint.FontMetrics fm = paint.getFontMetrics();
        float textY = centerY - (fm.ascent + fm.descent) / 2f;

        // 5) Рисуем текст
        canvas.drawText(String.valueOf(value), centerX, textY, paint);
    }

    // Метод для расчета подходящего размера шрифта в зависимости от значения
    private float getTextSizeForValue(int value) {
        float baseTextSize = cellSize * 0.5f; // Базовый размер для маленьких чисел

        // Если значение больше, чем 512, уменьшаем размер шрифта
        if (value > 512) {
            // Для больших значений уменьшаем размер шрифта (можно настроить множители по своему усмотрению)
            return baseTextSize * 0.6f;  // Например, уменьшаем на 40% для чисел больше 512
        } else if (value > 128) {
            // Для средних значений (128, 256, 512) немного уменьшаем
            return baseTextSize * 0.8f;  // Уменьшаем на 20% для чисел от 128 до 512
        }

        // Для маленьких значений оставляем базовый размер
        return baseTextSize;
    }

    public void updateGridSize(int size) {
        game.numSquaresX = size;
        game.numSquaresY = size;
        // Пересчитываем размеры экрана под новую сетку
        resyncTime();
        game.newGame();
        invalidate(); // Перерисовать экран
    }
    private float dpToPx(float dp) {
        return dp * getResources().getDisplayMetrics().density;
    }

    private void drawScoreText(Canvas canvas) {
        // Настройка шрифта и текста
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTypeface(Typeface.createFromAsset(getContext().getAssets(), "fonts/clearsans_bold.ttf"));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(bodyTextSize);
        paint.setFakeBoldText(true);

        // Фиксированная ширина для текста (максимум 99999)
        int fixedTextWidth = (int) paint.measureText("99999") + textPaddingSize * 3;

        // Вычисляем ширину блока для HIGH SCORE и SCORE (фиксированная ширина)
        int textWidthHighScore = fixedTextWidth;
        int textWidthScore = fixedTextWidth;

        // Позиция текста по центру
        int textMiddleHighScore = textWidthHighScore / 2;
        int textMiddleScore = textWidthScore / 2;

        // Начальная позиция X для SCORE и HIGH SCORE после "2048"
        int startXScore = logoRightX + (int) dpToPx(25);  // Сдвиг вправо от логотипа "2048"
        int sXScore = startXScore;
        int eXScore = sXScore + textWidthScore;

        int sXHighScore = eXScore + textPaddingSize;
        int eXHighScore = sXHighScore + textWidthHighScore;

        // Высота текста по центру (вертикальное выравнивание)
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        float centerYOffset = (fontMetrics.ascent + fontMetrics.descent) / 2f;
        float centerY = (sYAll + eYAll) / 2f;
        float textY = centerY - centerYOffset + dpToPx(9);  // Подняли чуть выше для лучшего расположения

        // Рисуем блок с HIGH SCORE
        backgroundRectangle.setBounds(sXHighScore, sYAll, eXHighScore, eYAll);
        backgroundRectangle.draw(canvas);

        paint.setTextSize(titleTextSize);
        paint.setColor(TEXT_BROWN);
        canvas.drawText(highScoreTitle, sXHighScore + textMiddleHighScore, titleStartYAll + dpToPx(4), paint);

        paint.setTextSize(bodyTextSize);
        paint.setColor(TEXT_WHITE);
        canvas.drawText(String.valueOf(game.highScore), sXHighScore + textMiddleHighScore, textY, paint);

        // Рисуем блок с SCORE
        backgroundRectangle.setBounds(sXScore, sYAll, eXScore, eYAll);
        backgroundRectangle.draw(canvas);

        paint.setTextSize(titleTextSize);
        paint.setColor(TEXT_BROWN);
        canvas.drawText(scoreTitle, sXScore + textMiddleScore, titleStartYAll + dpToPx(4), paint);

        paint.setTextSize(bodyTextSize);
        paint.setColor(TEXT_WHITE);
        canvas.drawText(String.valueOf(game.score), sXScore + textMiddleScore, textY, paint);
    }




    private void drawNewGameButton(Canvas canvas, boolean lightUp) {
        if (lightUp) {
            drawDrawable(canvas, lightUpRectangle, sXNewGame, sYIcons,
                    sXNewGame + iconSize, sYIcons + iconSize);
        } else {
            drawDrawable(canvas, backgroundRectangle, sXNewGame, sYIcons,
                    sXNewGame + iconSize, sYIcons + iconSize);
        }
        drawDrawable(canvas, newGameIcon, sXNewGame + iconPaddingSize, sYIcons
                        + iconPaddingSize, sXNewGame + iconSize - iconPaddingSize,
                sYIcons + iconSize - iconPaddingSize);
    }

    public void drawCheatButton(Canvas canvas) {
        // Рисуем фон кнопки
        drawDrawable(canvas, backgroundRectangle, sXCheat, sYIcons, sXCheat + iconSize, sYIcons + iconSize);
        // Рисуем иконку чита
        drawDrawable(canvas, cheatIcon, sXCheat + iconPaddingSize, sYIcons + iconPaddingSize,
                sXCheat + iconSize - iconPaddingSize, sYIcons + iconSize - iconPaddingSize);

        // Рисуем полоску лимита под кнопкой
        drawCheatIndicator(canvas);
    }

    private void drawCheatIndicator(Canvas canvas) {
        // 🔥 Берем живое значение из активити, чтобы дизайн обновлялся мгновенно
        int attempts = this.activity.getCheatCount();
        int totalDots = 3; // Для читов делаем 3 сегмента
        int gap = 3;

        float startX = sXCheat;
        float startY = sYIcons + iconSize + 15;
        // Делим ширину иконки на 3 части
        float segmentWidth = (float) iconSize / totalDots;

        // Цвета один-в-один как в твоем методе Undo
        int activeColor, inactiveColor;
        if (isDarkMode) {
            activeColor = Color.parseColor("#F9F6F2");
            inactiveColor = Color.parseColor("#4A4A4A");
        } else {
            activeColor = Color.parseColor("#8f7a66");
            inactiveColor = Color.parseColor("#D6CDC4");
        }

        indicatorPaint.setAntiAlias(true);

        for (int i = 0; i < totalDots; i++) {
            // Если индекс меньше текущего счета — красим в активный цвет
            indicatorPaint.setColor(i < attempts ? activeColor : inactiveColor);

            RectF segment = new RectF(
                    startX + (i * segmentWidth) + gap,
                    startY,
                    startX + ((i + 1) * segmentWidth) - gap,
                    startY + 10
            );
            // Рисуем скругленный сегмент как у Undo
            canvas.drawRoundRect(segment, 8, 8, indicatorPaint);
        }
    }

    // Выносим Paint в поля класса или создаем один раз перед циклом
    private final Paint indicatorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private void drawUndoButton(Canvas canvas) {
        // Рисуем саму кнопку
        drawDrawable(canvas, backgroundRectangle, sXUndo, sYIcons, sXUndo + iconSize, sYIcons + iconSize);
        drawDrawable(canvas, undoIcon, sXUndo + iconPaddingSize, sYIcons + iconPaddingSize, sXUndo + iconSize - iconPaddingSize, sYIcons + iconSize - iconPaddingSize);

        // Сразу под ней рисуем индикатор
        drawUndoIndicator(canvas);
    }
    private int dp(int dp) {
        float density = getContext().getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }

    private void drawUndoIndicator(Canvas canvas) {
        int attempts = this.activity.getUndoCount();
        int gap = 3;
        float startX = sXUndo;
        float startY = sYIcons + iconSize + 15; // Чуть увеличим отступ
        float segmentWidth = (float) iconSize / 5;

        // Выбираем цвета в зависимости от темы
        int activeColor, inactiveColor;
        if (isDarkMode) {
            activeColor = Color.parseColor("#F9F6F2"); // Светлый (почти белый) для активности
            inactiveColor = Color.parseColor("#4A4A4A"); // Темно-серый для пустоты
        } else {
            activeColor = Color.parseColor("#8f7a66"); // Твой классический коричневый
            inactiveColor = Color.parseColor("#D6CDC4"); // Светлый беж
        }

        for (int i = 0; i < 5; i++) {
            indicatorPaint.setColor(i < attempts ? activeColor : inactiveColor);

            RectF segment = new RectF(
                    startX + (i * segmentWidth) + gap,
                    startY,
                    startX + ((i + 1) * segmentWidth) - gap,
                    startY + 10 // Чуть толще для стиля
            );
            canvas.drawRoundRect(segment, 8, 8, indicatorPaint);
        }
    }

    private void drawHeader(Canvas canvas) {
        // Создаём и настраиваем Paint для заголовка
        paint.setAntiAlias(true);
        paint.setTypeface(Typeface.createFromAsset(getContext().getAssets(), "fonts/clearsans_bold.ttf"));
        paint.setTextSize(headerTextSize);
        paint.setColor(instructionTextColor); // Используем актуальный цвет
        paint.setTextAlign(Paint.Align.LEFT);

        // Вычисляем вертикальную позицию и добавляем небольшое смещение вниз
        int textShiftY = centerText() * 2;  // Это уже предыдущее смещение, можно добавить небольшую поправку
        int headerStartY = sYAll - textShiftY + 80;  // Сдвиг вниз на 110 пикселей, если нужно

        // Рисуем надпись "2048"
        canvas.drawText(headerText, startingX, headerStartY, paint);

        // Измеряем ширину текста и сохраняем правую границу
        float logoWidth = paint.measureText(headerText);
        logoRightX = startingX + (int) logoWidth;

        Log.d("ThemeDebug", "drawing header with color: " + instructionTextColor);


    }

    // Добавь это в MainView.java
    public int getHeaderBottomOnScreen() {
        int[] location = new int[2];
        this.getLocationOnScreen(location);

        // Формула в точности повторяет ту, что у тебя в drawHeader:
        // headerStartY = sYAll - (centerText() * 2) + 80
        int textShiftY = centerText() * 2;
        int headerStartY = sYAll - textShiftY + 80;

        // Возвращаем Y координату текста на экране + его высота
        return location[1] + headerStartY + (int)(headerTextSize / 4);
    }


    private void drawInstructions(Canvas canvas) {
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTextSize(instructionsTextSize + 3);

        // 🔁 Используем переменную цвета, а не флаг isDarkMode
        paint.setColor(instructionTextColor);

        int textShiftY = centerText() * 5;
        canvas.drawText(instructionsText, startingX, endingY - textShiftY + textPaddingSize, paint);
        Log.d("ThemeDebug", "drawing header with color: " + instructionTextColor);

    }


    public void updatePaintColors() {
        paint.setColor(instructionTextColor);
    }






    private void drawBackground(Canvas canvas) {
        drawDrawable(canvas, backgroundRectangle, startingX, startingY,
                endingX, endingY);
    }

    private void drawBackgroundGrid(Canvas canvas) {
        // 1. Создаем кисть для пустых ячеек (на случай, если drawable пустой)
        Paint emptyTilePaint = new Paint();
        emptyTilePaint.setAntiAlias(true);
        // Берем цвет tile_empty, который ты добавил в XML
        emptyTilePaint.setColor(ContextCompat.getColor(getContext(), R.color.tile_empty));

        // Цикл отрисовки сетки
        for (int xx = 0; xx < game.numSquaresX; xx++) {
            for (int yy = 0; yy < game.numSquaresY; yy++) {
                int sX = startingX + gridWidth + (cellSize + gridWidth) * xx;
                int eX = sX + cellSize;
                int sY = startingY + gridWidth + (cellSize + gridWidth) * yy;
                int eY = sY + cellSize;

                // 2. ПРОВЕРКА: Если массив или элемент null, рисуем просто цветной квадрат
                if (cellRectangle != null && cellRectangle.length > 0 && cellRectangle[0] != null) {
                    drawDrawable(canvas, cellRectangle[0], sX, sY, eX, eY);
                } else {
                    // Если картинки нет, рисуем программно — это спасет от краша
                    RectF rect = new RectF(sX, sY, eX, eY);
                    canvas.drawRoundRect(rect, 8, 8, emptyTilePaint);
                }
            }
        }
    }

    private void drawCells(Canvas canvas) {
        paint.setTextSize(textSize);
        paint.setTextAlign(Paint.Align.CENTER);
        // Outputting the individual cells
        for (int xx = 0; xx < game.numSquaresX; xx++) {
            for (int yy = 0; yy < game.numSquaresY; yy++) {
                int sX = startingX + gridWidth + (cellSize + gridWidth) * xx;
                int eX = sX + cellSize;
                int sY = startingY + gridWidth + (cellSize + gridWidth) * yy;
                int eY = sY + cellSize;

                Tile currentTile = game.grid.getCellContent(xx, yy);
                if (currentTile != null) {
                    // Get and represent the value of the tile
                    int value = currentTile.getValue();
                    int index = log2(value);

                    // Check for any active animations
                    ArrayList<AnimationCell> aArray = game.aGrid
                            .getAnimationCell(xx, yy);
                    boolean animated = false;
                    for (int i = aArray.size() - 1; i >= 0; i--) {
                        AnimationCell aCell = aArray.get(i);
                        // If this animation is not active, skip it
                        if (aCell.getAnimationType() == MainGame.SPAWN_ANIMATION) {
                            animated = true;
                        }
                        if (!aCell.isActive()) {
                            continue;
                        }

                        if (aCell.getAnimationType() == MainGame.SPAWN_ANIMATION) { // Spawning
                            // animation
                            double percentDone = aCell.getPercentageDone();
                            float textScaleSize = (float) (percentDone);
                            paint.setTextSize(textSize * textScaleSize);

                            float cellScaleSize = cellSize / 2
                                    * (1 - textScaleSize);
                            bitmapCell[index].setBounds(
                                    (int) (sX + cellScaleSize),
                                    (int) (sY + cellScaleSize),
                                    (int) (eX - cellScaleSize),
                                    (int) (eY - cellScaleSize));
                            bitmapCell[index].draw(canvas);
                        } else if (aCell.getAnimationType() == MainGame.MERGE_ANIMATION) { // Merging Animation
                            double percentDone = aCell.getPercentageDone();

                            // 1. Стандартная логика масштабирования (Scale)
                            float textScaleSize = (float) (1 + INITIAL_VELOCITY
                                    * percentDone + MERGING_ACCELERATION
                                    * percentDone * percentDone / 2);
                            paint.setTextSize(textSize * textScaleSize);

                            float cellScaleSize = cellSize / 2 * (1 - textScaleSize);

                            // Рисуем саму плитку
                            bitmapCell[index].setBounds(
                                    (int) (sX + cellScaleSize),
                                    (int) (sY + cellScaleSize),
                                    (int) (eX - cellScaleSize),
                                    (int) (eY - cellScaleSize));
                            bitmapCell[index].draw(canvas);

                            // 🔥 ДОБАВЛЯЕМ ВСПЫШКУ (Flash Effect)
                            // Рисуем белый блик, который затухает по мере завершения анимации
                            Paint flashPaint = new Paint();
                            flashPaint.setColor(Color.WHITE);

                            // Прозрачность: в начале анимации (percentDone = 0) вспышка яркая, в конце (1.0) исчезает
                            int alpha = (int) (150 * (1 - percentDone));
                            flashPaint.setAlpha(alpha);

                            canvas.drawRoundRect(
                                    sX + cellScaleSize,
                                    sY + cellScaleSize,
                                    eX - cellScaleSize,
                                    eY - cellScaleSize,
                                    8, 8, flashPaint); // 8, 8 — это радиус скругления углов
                        } else if (aCell.getAnimationType() == MainGame.MOVE_ANIMATION) { // Moving
                            // animation
                            double percentDone = aCell.getPercentageDone();
                            int tempIndex = index;
                            if (aArray.size() >= 2) {
                                tempIndex = tempIndex - 1;
                            }
                            int previousX = aCell.extras[0];
                            int previousY = aCell.extras[1];
                            int currentX = currentTile.getX();
                            int currentY = currentTile.getY();
                            int dX = (int) ((currentX - previousX)
                                    * (cellSize + gridWidth)
                                    * (percentDone - 1) * 1.0);
                            int dY = (int) ((currentY - previousY)
                                    * (cellSize + gridWidth)
                                    * (percentDone - 1) * 1.0);
                            bitmapCell[tempIndex].setBounds(sX + dX, sY + dY,
                                    eX + dX, eY + dY);
                            bitmapCell[tempIndex].draw(canvas);
                        }
                        animated = true;
                    }

                    // No active animations? Just draw the cell
                    if (!animated) {
                        bitmapCell[index].setBounds(sX, sY, eX, eY);
                        bitmapCell[index].draw(canvas);
                    }
                }
            }
        }
    }

    private void drawEndGameState(Canvas canvas) {
        double alphaChange = 1;
        for (AnimationCell animation : game.aGrid.globalAnimation) {
            if (animation.getAnimationType() == MainGame.FADE_GLOBAL_ANIMATION) {
                alphaChange = animation.getPercentageDone();
            }
        }

        BitmapDrawable displayOverlay = null;
        if (game.gameWon()) {
            displayOverlay = game.canContinue() ? winGameContinueOverlay : winGameFinalOverlay;
        }

        if (displayOverlay != null) {
            displayOverlay.setBounds(startingX, startingY, endingX, endingY);
            displayOverlay.setAlpha((int) (255 * alphaChange));
            displayOverlay.draw(canvas);
        }
    }





    private void drawEndlessText(Canvas canvas) {
        // 🔥 ДОБАВЛЯЕМ ПРОВЕРКУ:
        // Рисуем надпись только если игра НЕ активна (пауза/выигрыш)
        // и при этом мы НЕ проиграли совсем.
        if (game.isActive() || game.gameLost()) {
            return;
        }

        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTextSize(75);
        paint.setColor(TEXT_BLACK);

        int offsetX = 5;
        int offsetY = 22;

        canvas.drawText(endlessModeText, startingX + offsetX, sYIcons + offsetY - centerText() * 2, paint);
    }





    private void createEndGameStates(Canvas canvas, boolean win, boolean showButton) {
        int width = endingX - startingX;
        int length = endingY - startingY;
        int middleX = width / 2;
        int middleY = length / 2;

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTypeface(Typeface.createFromAsset(getContext().getAssets(), "fonts/clearsans_bold.ttf"));
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setAlpha(255);

        if (win) {
            lightUpRectangle.setAlpha(127);
            drawDrawable(canvas, lightUpRectangle, 0, 0, width, length);
            lightUpRectangle.setAlpha(255);

            paint.setColor(TEXT_WHITE);
            paint.setTextSize(gameOverTextSize);
            int textBottom = middleY - centerText();
            canvas.drawText(winText, middleX, textBottom, paint);

            paint.setTextSize(bodyTextSize);
            String text = showButton ? continueText : forNowText;
            canvas.drawText(text, middleX, textBottom + textPaddingSize * 2 - centerText() * 2, paint);
        } else {
            fadeRectangle.setAlpha(127);
            drawDrawable(canvas, fadeRectangle, 0, 0, width, length);
            fadeRectangle.setAlpha(255);

            paint.setColor(TEXT_BLACK);
            paint.setTextSize(gameOverTextSize);
            canvas.drawText(loseText, middleX, middleY - centerText(), paint);
        }
    }


    private void createBackgroundBitmap(int width, int height) {
        background = Bitmap
                .createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(background);
        drawHeader(canvas);
        drawCheatButton(canvas);
        drawNewGameButton(canvas, false);
        drawUndoButton(canvas);
        drawBackground(canvas);
        drawBackgroundGrid(canvas);
        drawInstructions(canvas);

    }

    public void createBitmapCells() {
        Resources resources = getResources();
        paint.setTextSize(cellTextSize);
        paint.setTextAlign(Paint.Align.CENTER);

        for (int xx = 0; xx < bitmapCell.length; xx++) {
            Bitmap bitmap = Bitmap.createBitmap(cellSize, cellSize, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);

            // 1. Считаем значение плитки (2, 4, 8...)
            int value = (xx == 0) ? 0 : (int) Math.pow(2, xx);

            // 2. РИСУЕМ ФОН ПЛИТКИ (Используем твой четкий метод!)
            Paint tilePaint = new Paint();
            tilePaint.setAntiAlias(true);
            tilePaint.setColor(getBackgroundForValue(value));

            RectF rect = new RectF(0, 0, cellSize, cellSize);
            canvas.drawRoundRect(rect, 25, 25, tilePaint); // Рисуем фон цветом

            // 3. РИСУЕМ ТЕКСТ (ЦИФРУ)
            if (value > 0) {
                drawCellText(canvas, value, 0, 0);
            }

            // Сохраняем результат в кэш плиток
            bitmapCell[xx] = new BitmapDrawable(resources, bitmap);
        }
    }

    public void createOverlays() {
        Resources resources = getResources();
        // Initalize overlays
        Bitmap bitmap = Bitmap.createBitmap(endingX - startingX, endingY
                - startingY, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        createEndGameStates(canvas, true, true);
        winGameContinueOverlay = new BitmapDrawable(resources, bitmap);
        bitmap = Bitmap.createBitmap(endingX - startingX, endingY - startingY,
                Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        createEndGameStates(canvas, true, false);
        winGameFinalOverlay = new BitmapDrawable(resources, bitmap);
        bitmap = Bitmap.createBitmap(endingX - startingX, endingY - startingY,
                Bitmap.Config.ARGB_8888);
        canvas = new Canvas(bitmap);
        createEndGameStates(canvas, false, false);
        loseGameOverlay = new BitmapDrawable(resources, bitmap);
    }

    private void tick() {
        currentTime = System.nanoTime();
        game.aGrid.tickAll(currentTime - lastFPSTime); // Вот тут происходит расчет кадров анимации
        game.updateParticles();
        lastFPSTime = currentTime;
    }

    public void resyncTime() {
        lastFPSTime = System.nanoTime();
    }

    private static int log2(int n) {
        if (n <= 0)
            throw new IllegalArgumentException();
        return 31 - Integer.numberOfLeadingZeros(n);
    }

    public MainGame getGame() {
        return game;
    }

    private void getLayout(int width, int height) {
        cellSize = Math.min(width / (game.numSquaresX + 1), height
                / (game.numSquaresY + 5));
        gridWidth = cellSize / 7;
        int screenMiddleX = width / 2;
        int screenMiddleY = height / 2;
        int boardMiddleX = screenMiddleX;
        int boardMiddleY = screenMiddleY + cellSize / 2;
        iconSize = cellSize / 2;

        paint.setTextAlign(Paint.Align.CENTER);
        paint.setTextSize(cellSize);
        textSize = cellSize * cellSize
                / Math.max(cellSize, paint.measureText("0000"));
        cellTextSize = textSize * 0.9f;
        titleTextSize = textSize / 3;
        bodyTextSize = (int) (textSize / 1.5);
        instructionsTextSize = (int) (textSize / 1.8);
        headerTextSize = textSize * 2;
        gameOverTextSize = textSize * 2;
        textPaddingSize = (int) (textSize / 3);
        iconPaddingSize = (int) (textSize / 5);

        // Grid Dimensions
        double halfNumSquaresX = game.numSquaresX / 2d;
        double halfNumSquaresY = game.numSquaresY / 2d;

        startingX = (int) (boardMiddleX - (cellSize + gridWidth)
                * halfNumSquaresX - gridWidth / 2);
        endingX = (int) (boardMiddleX + (cellSize + gridWidth)
                * halfNumSquaresX + gridWidth / 2);
        startingY = (int) (boardMiddleY - (cellSize + gridWidth)
                * halfNumSquaresY - gridWidth / 2);
        endingY = (int) (boardMiddleY + (cellSize + gridWidth)
                * halfNumSquaresY + gridWidth / 2);

        paint.setTextSize(titleTextSize);

        int textShiftYAll = centerText();
        // static variables
        sYAll = (int) (startingY - cellSize * 1.5);
        titleStartYAll = (int) (sYAll + textPaddingSize + titleTextSize / 2 - textShiftYAll);
        bodyStartYAll = (int) (titleStartYAll + textPaddingSize + titleTextSize
                / 2 + bodyTextSize / 2);

        titleWidthHighScore = (int) (paint.measureText(
                highScoreTitle == null ? "" : highScoreTitle));
        titleWidthScore = (int) (paint.measureText(
                scoreTitle == null ? "" : scoreTitle));

        paint.setTextSize(bodyTextSize);
        textShiftYAll = centerText();
        eYAll = (int) (bodyStartYAll + textShiftYAll + bodyTextSize / 2 + textPaddingSize);

        // ... твой текущий код в конце getLayout ...
        sYIcons = (startingY + eYAll) / 2 - iconSize / 2;
        sXNewGame = (endingX - iconSize);

        // Кнопка Лидерборд - ровно под Новой игрой
        sXLeaderboard = sXNewGame;
        sYLeaderboard = sYIcons + iconSize + iconPaddingSize;

        // Кнопка Моды - ровно под Лидербордом
        sXMods = sXNewGame;
        sYMods = sYLeaderboard + iconSize + iconPaddingSize;

        sXUndo = sXNewGame - iconSize * 3 / 2 - iconPaddingSize;
        sXCheat = sXUndo - iconSize * 3 / 2 - iconPaddingSize;
        // Внутри getLayout
        instructionsTextSize = textSize * 0.5f; // или твой расчет
        sYInstructions = endingY - (instructionsTextSize * 1.5f);
        resyncTime();

    }
    // Этот метод вернет точную координату верха игровой доски
    // 1. Возвращает верхнюю границу игровой сетки (плиток)
    public int getGridTop() {
        int[] location = new int[2];
        this.getLocationOnScreen(location);

        // Если View еще не на экране, location[1] будет 0.
        // В этом случае возвращаем хотя бы примерное значение, чтобы не было 0
        if (location[1] == 0) {
            return (int) startingY;
        }

        return location[1] + (int)startingY;
    }

    public int getInstructionsY() {
        return (int) sYInstructions;
    }

    public int getInstructionsHeight() {
        return (int) instructionsTextSize;
    }

    private int centerText() {
        return (int) ((paint.descent() + paint.ascent()) / 2);
    }



    public void setTextColorOverride(int color) {
        this.customTextColor = color;
        invalidate(); // Перерисовать view
    }

    public void updateThemeColors() {
        Resources resources = getContext().getResources();
        instructionTextColor = ContextCompat.getColor(
                getContext(), isDarkMode ? R.color.text_white : R.color.text_black
        );
        invalidate(); // Перерисовать view
    }


    public void applyTheme(boolean isDark) {
        // 1. Устанавливаем системную тему
        getContext().getTheme().applyStyle(isDark ?
                androidx.appcompat.R.style.Theme_AppCompat_DayNight :
                androidx.appcompat.R.style.Theme_AppCompat_Light, true);

        this.isDarkMode = isDark;

        // 2. Обновляем конфигурацию ресурсов
        Configuration config = new Configuration(getContext().getResources().getConfiguration());
        config.uiMode = isDark ? Configuration.UI_MODE_NIGHT_YES : Configuration.UI_MODE_NIGHT_NO;
        getContext().getResources().updateConfiguration(config, getContext().getResources().getDisplayMetrics());

        pendingThemeChange = false;
        Resources resources = getContext().getResources();

        try {
            // 3. Обновляем тексты
            headerText = safeGetString(resources, R.string.header);
            highScoreTitle = safeGetString(resources, R.string.high_score);
            scoreTitle = safeGetString(resources, R.string.score);
            instructionsText = safeGetString(resources, R.string.instructions);
            winText = safeGetString(resources, R.string.you_win);
            loseText = safeGetString(resources, R.string.game_over);
            continueText = safeGetString(resources, R.string.go_on);
            forNowText = safeGetString(resources, R.string.for_now);
            endlessModeText = safeGetString(resources, R.string.endless);

            // 4. Обновляем Drawable
            backgroundRectangle = safeGetDrawable(resources,
                    isDark ? R.drawable.background_rectangle_dark : R.drawable.background_rectangle);
            newGameIcon = safeGetDrawable(resources, R.drawable.ic_action_refresh);
            undoIcon = safeGetDrawable(resources, R.drawable.ic_action_undo);
            cheatIcon = safeGetDrawable(resources, R.drawable.ic_action_cheat);
            leaderboardIcon = safeGetDrawable(resources, R.drawable.ic_leaderboard);
            modsIcon = safeGetDrawable(resources, R.drawable.ic_lightning);
            lightUpRectangle = safeGetDrawable(resources, R.drawable.light_up_rectangle);
            fadeRectangle = safeGetDrawable(resources, R.drawable.fade_rectangle);

            // 5. Обновляем цвета из XML
            TEXT_WHITE = ContextCompat.getColor(getContext(), R.color.text_white);
            TEXT_BLACK = ContextCompat.getColor(getContext(), R.color.text_black);
            TEXT_BROWN = ContextCompat.getColor(getContext(), R.color.text_brown);
            TEXT_DARK_WHITE = ContextCompat.getColor(getContext(), R.color.text_dark_white);
            TEXT_LIGHT_BLACK = ContextCompat.getColor(getContext(), R.color.text_black);
            instructionTextColor = ContextCompat.getColor(getContext(),
                    isDark ? R.color.text_white : R.color.text_black);

            // 6. Фон самого View
            int bgColor = ContextCompat.getColor(getContext(),
                    isDark ? R.color.background_dark : R.color.background_light);
            setBackgroundColor(bgColor);

            // 7. Обновляем кисти
            updatePaintColors();

            // 🔥 8. КЛЮЧЕВОЙ МОМЕНТ: Уничтожаем старый Bitmap
            // Мы не вызываем здесь создание, просто зануляем, чтобы onDraw сделал всё сам
            background = null;

            invalidate();

        } catch (Exception e) {
            Log.e("applyTheme", "Ошибка при смене темы: " + e.getMessage());
            e.printStackTrace();
        }
    }




    private String safeGetString(Resources res, int id) {
        try {
            return res.getString(id);
        } catch (Exception e) {
            Log.w("MainView", "Не удалось загрузить строку: " + id);
            return "";
        }
    }

    private Drawable safeGetDrawable(Resources res, int id) {
        try {
            return ContextCompat.getDrawable(getContext(), id);
        } catch (Exception e) {
            Log.w("MainView", "Не удалось загрузить drawable: " + id);
            return new ColorDrawable(Color.GRAY); // запасной прямоугольник
        }
    }

    public void setInputEnabled(boolean enabled) {
        this.inputEnabled = enabled;
    }
    public void resetInput() {
        if (inputListener != null) {
            inputListener.resetInput();
        }
    }

    // Добавь этот метод в MainView.java
    private void drawParticles(Canvas canvas) {
        long currentTime = System.currentTimeMillis();

        // Идем по списку с конца в начало, чтобы безопасно удалять элементы
        for (int i = game.particles.size() - 1; i >= 0; i--) {
            Particle p = game.particles.get(i);

            if (p.isAlive(currentTime)) {
                p.update(currentTime);
                particlePaint.setColor(p.color);
                particlePaint.setAlpha(p.alpha); // Применяем прозрачность!

                // Рисуем частицу (можно сделать её чуть меньше для красоты)
                canvas.drawCircle(p.x, p.y, dpToPx(2), particlePaint);
            } else {
                // ✅ ВОТ ЭТО УДАЛЯЕТ СЛЕДЫ НАВСЕГДА
                game.particles.remove(i);
            }
        }

        // Если в списке еще есть живые частицы, просим перерисовать экран
        if (!game.particles.isEmpty()) {
            postInvalidateDelayed(16); // ~60 кадров в секунду
        }
    }




    public MainView(Context context) {
        super(context);
        this.activity = (MainActivity) context; // Теперь мы можем дергать методы активити
        game = new MainGame(context, this);
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);

        // Устанавливаем шрифт один раз
        try {
            Typeface font = Typeface.createFromAsset(context.getAssets(), "fonts/clearsans_bold.ttf");
            paint.setTypeface(font);
        } catch (Exception e) {
            paint.setTypeface(Typeface.DEFAULT_BOLD);
        }
        paint.setAntiAlias(true);

        // Загружаем сохраненную тему
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        isDarkMode = prefs.getBoolean("dark_mode", false);

        // Применяем тему (этот метод теперь будет обновлять всё)
        applyTheme(isDarkMode);

        // Передаем и само View, и MainActivity (через каст контекста)
        inputListener = new InputListener(this, (MainActivity) getContext());
        setOnTouchListener(inputListener);

        game.newGame();
    }



}