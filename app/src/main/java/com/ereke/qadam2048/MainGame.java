package com.ereke.qadam2048;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.net.ssl.SSLSession;

public class MainGame {
    // В классе MainGame.java, в самом начале, после других переменных
    public ArrayList<Particle> particles = new ArrayList<>();
    public static final int SPAWN_SPRING = 5;
    private boolean active = true;
    private int currentStreamId = 0;

    public static final int SPAWN_ANIMATION = -1;
    public static final int MOVE_ANIMATION = 0;
    public static final int MERGE_ANIMATION = 1;

    public static final int FADE_GLOBAL_ANIMATION = 0;

    public static final long MOVE_ANIMATION_TIME = MainView.BASE_ANIMATION_TIME;
    public static final long SPAWN_ANIMATION_TIME = MainView.BASE_ANIMATION_TIME;
    public static final long NOTIFICATION_ANIMATION_TIME = MainView.BASE_ANIMATION_TIME * 5;
    public static final long NOTIFICATION_DELAY_TIME = MOVE_ANIMATION_TIME
            + SPAWN_ANIMATION_TIME;

    public int hearts = 3;
    private static final String HIGH_SCORE = "high score";

    public static final int startingMaxValue = 2048;
    public static final int endingMaxValue = 32768;


    private SoundPool soundPool;
    private HashMap<Integer, Integer> soundMap;


    // Odd state = game is not active
    // Even state = game is active
    // Win state = active state + 1
    public static final int GAME_WIN = 1;
    public static final int GAME_LOST = -1;
    public static final int GAME_NORMAL = 0;
    public static final int GAME_PAUSED = 10; // <--- ДОБАВЬ ЭТУ СТРОКУ (число может быть любым уникальным, например 10)
    public static final int GAME_NORMAL_WON = 1;
    public static final int GAME_ENDLESS = 2;
    public static final int GAME_ENDLESS_WON = 3;

    public Grid grid = null;

    private boolean hasLost;


    private InputListener inputListener;

    public AnimationGrid aGrid;
    // В MainGame.java
    public int numSquaresX;
    public int numSquaresY;
    final int startTiles = 2;

    public int gameState = 0;
    public boolean canUndo;

    public long score = 0;
    public long highScore = 0;

    public long lastScore = 0;
    public int lastGameState = 0;

    private long bufferScore = 0;
    private int bufferGameState = 0;

    private SoundPool soudPool;
    private HashMap<Integer, Integer> spMap;

    private Context mContext;

    private MainView mView;
    private SSLSession view;
    private boolean hasWon;

    public MainGame(Context context, MainView view) {
        mContext = context;
        this.mView = view;

        initSoundPool();
    }

    public void rollbackOneMove() {
        revertUndoState(); // если уже есть undo
    }


    public void continueAfterAd() {
        if (gameLost()) {
            gameState = GAME_NORMAL;
            mView.invalidate();
        }
    }

    public void newGame() {
        // 1. Проверяем: нужно ли пересоздать сетку
        if (grid == null || grid.field.length != numSquaresX || grid.field[0].length != numSquaresY) {
            grid = new Grid(numSquaresX, numSquaresY);
        } else {
            // Если размер тот же, просто готовим Undo и чистим поле
            prepareUndoState();
            grid.prepareSaveTiles();
            grid.saveTiles();
            grid.clearGrid();
        }

        // 2. Анимационную сетку пересоздаем ВСЕГДА под текущий размер
        aGrid = new AnimationGrid(numSquaresX, numSquaresY);

        // 3. Сбрасываем параметры ТЕКУЩЕЙ игры
        score = 0;
        gameState = GAME_NORMAL;
        hearts = 3;

        // --- КРИТИЧЕСКИЙ ФИКС ДЛЯ РЕКОРДА ---
        // Мы НЕ обнуляем highScore здесь до 0, потому что он должен быть
        // загружен из MainActivity (через SharedPreferences).
        // Но мы должны убедиться, что если мы начали НОВУЮ игру в НОВОМ режиме,
        // старый рекорд не "прилип".
        // ------------------------------------

        // 4. Добавляем стартовые плитки
        addStartTiles();

        canUndo = false; // При новой игре Undo обычно недоступен сразу

        if (mView != null) {
            mView.refreshLastTime = true;
            mView.resyncTime();
            mView.invalidate();
        }

        deactivateUndo();
    }

    public void deactivateUndo() {
        this.canUndo = false;
        // Если у тебя есть массив, который хранит прошлую сетку (например, undoField)
        // занули его, чтобы кнопке нечего было подставлять
        if (grid != null) {
            // Записываем "пустоту" в историю
            // Это зависит от того, как у тебя реализован класс Grid
        }
    }
    private void addStartTiles() {
        for (int xx = 0; xx < startTiles; xx++) {
            this.addRandomTile();
        }
    }

    public void addRandomTile() {
        if (grid != null && grid.isCellsAvailable()) {
            int value = Math.random() < 0.9 ? 2 : 4;
            Tile tile = new Tile(grid.randomAvailableCell(), value);
            spawnTile(tile);

            // Перерисовываем экран, чтобы плитка сразу появилась
            if (mView != null) {
                mView.invalidate();
            }
        }
    }

    // MainGame.java
    public void refreshGrid() {
        view.invalidate(); // Просто перерисовать всё на экране
    }

    private void spawnTile(Tile tile) {
        grid.insertTile(tile);
        aGrid.startAnimation(tile.getX(), tile.getY(), SPAWN_ANIMATION,
                SPAWN_ANIMATION_TIME, MOVE_ANIMATION_TIME, null); // Direction:
        // -1 =
        // EXPANDING
    }

    private void recordHighScore() {
        SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong(HIGH_SCORE, highScore);
        editor.commit();
    }

    private long getHighScore() {
        SharedPreferences settings = PreferenceManager
                .getDefaultSharedPreferences(mContext);
        return settings.getLong(HIGH_SCORE, -1);
    }

    private void prepareTiles() {
        for (Tile[] array : grid.field) {
            for (Tile tile : array) {
                if (grid.isCellOccupied(tile)) {
                    tile.setMergedFrom(null);
                }
            }
        }
    }

    private void moveTile(Tile tile, Cell cell) {

        grid.field[tile.getX()][tile.getY()] = null;
        grid.field[cell.getX()][cell.getY()] = tile;
        tile.updatePosition(cell);
    }

    private void saveUndoState() {
        grid.saveTiles();
        canUndo = true;
        lastScore = bufferScore;
        lastGameState = bufferGameState;
    }

    // cheat remove 2
    public void cheat() {
        playSound(3);
        ArrayList<Cell> notAvailableCell = grid.getNotAvailableCells();
        Tile tile;
        prepareUndoState();
        for (Cell cell : notAvailableCell) {
            tile = grid.getCellContent(cell);
            if (2 == tile.getValue()) {
                grid.removeTile(tile);
            }
        }

        if (grid.getNotAvailableCells().size() == 0) {
            addStartTiles();
        }
        saveUndoState();
        mView.resyncTime();
        mView.invalidate();

    }

    private void prepareUndoState() {
        grid.prepareSaveTiles();
        bufferScore = score;
        bufferGameState = gameState;
    }

    private void saveRecord(long score) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mView.getContext());
        Set<String> records = prefs.getStringSet("RECORDS", new HashSet<>());

        records.add(String.valueOf(score));

        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet("RECORDS", records);
        editor.apply();
    }


    public void revertUndoState() {
        if (canUndo) {
            grid.revertTiles(); // Восстанавливаем массив плиток
            score = lastScore;
            gameState = lastGameState;
            canUndo = false;

            // Это заставит игру заново "увидеть" плитки и нарисовать их
            if (mView != null) {
                mView.refreshLastTime = true;
                mView.invalidate();
            }
        }
    }

    // геттер состояния
    public int getGameState() {
        return gameState;
    }




    public boolean isActive() {
        return gameState == GAME_NORMAL;
    }


    public boolean gameLost() {
        // Игра окончена только если:
        // 1. Нет пустых клеток
        // 2. Нет доступных соединений (матчей)
        return !grid.isCellsAvailable() && !tileMatchesAvailable();
    }

    private boolean checkMatch(int x, int y, int value) {
        // Проверка соседа справа
        if (x < grid.field.length - 1) {
            Tile right = grid.field[x + 1][y];
            if (right != null && right.getValue() == value) return true;
        }
        // Проверка соседа снизу
        if (y < grid.field[0].length - 1) {
            Tile bottom = grid.field[x][y + 1];
            if (bottom != null && bottom.getValue() == value) return true;
        }
        return false;
    }

    public boolean isGameOver() {
        // Если игра уже перешла в состояние проигрыша
        if (gameState == GAME_LOST) {
            return true;
        }
        // Или если ходов больше не осталось
        return !movesAvailable();
    }


    public void resetInput() {
        if (inputListener != null) {
            inputListener.resetInput();
        }
    }


    public void rollbackAfterAd() {
        playSound(3);
        aGrid.cancelAnimations();
        grid.revertTiles();
        score = lastScore;
        gameState = GAME_NORMAL; // Возвращаем в активную игру
        mView.refreshLastTime = true;
        mView.invalidate();
    }


    public boolean gameWon() {
        return (gameState > 0 && gameState % 2 != 0);
    }


    // 1. Меняем void на boolean
    public boolean move(int direction) {
        playSound(1);
        aGrid.cancelAnimations(); // Отменяем старые, чтобы начать новые

        boolean moved = false;
        int mergeCount = 0;

        if (!isActive()) return false;

        prepareUndoState();
        Cell vector = getVector(direction);
        List<Integer> traversalsX = buildTraversalsX(vector);
        List<Integer> traversalsY = buildTraversalsY(vector);

        prepareTiles();

        for (int xx : traversalsX) {
            for (int yy : traversalsY) {
                Cell cell = new Cell(xx, yy);
                Tile tile = grid.getCellContent(cell);

                if (tile != null) {
                    Cell[] positions = findFarthestPosition(cell, vector);
                    Tile next = grid.getCellContent(positions[1]);

                    if (next != null && next.getValue() == tile.getValue() && next.getMergedFrom() == null) {
                        mergeCount++;
                        playSound(2);

                        Tile merged = new Tile(positions[1], tile.getValue() * 2);
                        Tile[] temp = {tile, next};
                        merged.setMergedFrom(temp);

                        grid.insertTile(merged);
                        grid.removeTile(tile);
                        tile.updatePosition(positions[1]);

                        // 🔥 ВОТ ОНИ - ТВОИ РОДНЫЕ АНИМАЦИИ:
                        int[] extras = {xx, yy};
                        aGrid.startAnimation(merged.getX(), merged.getY(), MOVE_ANIMATION,
                                MOVE_ANIMATION_TIME, 0, extras);
                        aGrid.startAnimation(merged.getX(), merged.getY(), MERGE_ANIMATION,
                                SPAWN_ANIMATION_TIME, MOVE_ANIMATION_TIME, null);

                        // Твой старый код очков и эффектов:
                        int pointsToAdd = merged.getValue();
                        score += pointsToAdd;
                        highScore = Math.max(score, highScore);

                        if (mContext instanceof MainActivity) {
                            final int fVal = merged.getValue();
                            final int mX = merged.getX();
                            final int mY = merged.getY();
                            ((MainActivity) mContext).runOnUiThread(() -> {
                                ((MainActivity) mContext).triggerVibration(Math.min(25 + (fVal/32), 100));
                                ((MainActivity) mContext).showFloatingScore(pointsToAdd);



                                ((MainActivity) mContext).rewardPlayerForTile(fVal);


                                // Тряска и частицы:
                                if (fVal / 128f > 0.5f) ((MainActivity) mContext).shakeGameView(fVal / 128f);
                                ((MainActivity) mContext).spawnParticles(mX, mY, fVal);
                            });
                        }
                        moved = true;
                    } else {
                        moveTile(tile, positions[0]);
                        int[] extras = {xx, yy, 0};
                        aGrid.startAnimation(positions[0].getX(), positions[0].getY(),
                                MOVE_ANIMATION, MOVE_ANIMATION_TIME, 0, extras);
                    }
                    if (!positionsEqual(cell, tile)) moved = true;
                }
            }
        }

        if (moved) {

            saveUndoState();
            addRandomTile();
            checkLose();
        }

        mView.resyncTime();
        mView.invalidate();
        return moved;
    }

    // В MainGame.java
    public void updateParticles() {
        long currentTime = System.currentTimeMillis();
        for (int i = particles.size() - 1; i >= 0; i--) {
            Particle p = particles.get(i);
            if (p.isAlive(currentTime)) {
                p.update(currentTime);
            } else {
                particles.remove(i);
            }
        }
    }
    public void removeTileAt(int x, int y) {
        Tile target = grid.getCellContent(x, y);
        if (target != null) {
            grid.removeTile(target);
            playSound(3); // Твой звук "erok" (поп)
            mView.invalidate(); // Перерисовываем экран
        }
    }

    public void saveGameState() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
        SharedPreferences.Editor editor = editor = settings.edit();

        // Сохраняем счет
        editor.putLong("score", score);
        editor.putLong("highScore", highScore);

        // Превращаем сетку в строку (например: "2,0,4,8,0,0,2...")
        StringBuilder str = new StringBuilder();
        for (int xx = 0; xx < numSquaresX; xx++) {
            for (int yy = 0; yy < numSquaresY; yy++) {
                Tile tile = grid.getCellContent(xx, yy);
                if (tile != null) {
                    str.append(tile.getValue()).append(",");
                } else {
                    str.append("0").append(",");
                }
            }
        }
        editor.putString("grid", str.toString());
        editor.apply();
    }

    public void loadGameState() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
        score = settings.getLong("score", 0);
        // Логика восстановления сетки из строки...
    }
    private void checkLose() {
        // 1. Если уже проиграли или пауза — выходим
        if (gameState == GAME_LOST || gameState == GAME_PAUSED) return;

        // 2. Если ходов реально нет
        if (!movesAvailable()) {
            gameState = GAME_LOST; // Фиксируем состояние проигрыша

            if (mContext instanceof MainActivity) {
                MainActivity activity = (MainActivity) mContext;
                // 3. Просто просим Activity показать финальный оверлей
                activity.runOnUiThread(activity::onGameOver);
            }
        }
    }

    private void executeGameOverLogic() {
        gameState = GAME_LOST;
        hasLost = true;
        playSound(6);
        if (mContext instanceof MainActivity) {
            ((MainActivity) mContext).onGameOver();
        }
    }

    public void setEndState(boolean lost) {
        this.hasLost = lost;
        if (lost) {
            this.gameState = GAME_LOST;
        } else {
            // КОГДА МЫ ВОСКРЕСАЕМ (lost == false)
            this.gameState = GAME_NORMAL;
            // УБЕДИСЬ, ЧТО ЗДЕСЬ НЕТ ВЫЗОВА loadGameState() или загрузки SharedPreferences!
        }
    }
    public void resetLossState() {
        this.hasLost = false;
        this.gameState = 0; // Или то число, которое у тебя отвечает за активную игру
    }
    private void shakeField() {
        // Если у тебя есть доступ к MainView, можно запустить анимацию там
        if (mView != null) {
            mView.animate().translationX(20).setDuration(50).withEndAction(() ->
                    mView.animate().translationX(-20).setDuration(50).withEndAction(() ->
                            mView.animate().translationX(0).setDuration(50).start()
                    ).start()
            ).start();
        }
    }


    private boolean movesAvailable() {
        return grid.isCellsAvailable() || tileMatchesAvailable();
    }

    private void endGame() {
        aGrid.startAnimation(-1, -1, FADE_GLOBAL_ANIMATION,
                NOTIFICATION_ANIMATION_TIME, NOTIFICATION_DELAY_TIME, null);
        if (score >= highScore) {
            highScore = score;
            recordHighScore();
        }
    }

    private Cell getVector(int direction) {
        Cell[] map = {new Cell(0, -1), // up
                new Cell(1, 0), // right
                new Cell(0, 1), // down
                new Cell(-1, 0) // left
        };
        return map[direction];
    }

    private List<Integer> buildTraversalsX(Cell vector) {
        List<Integer> traversals = new ArrayList<Integer>();

        for (int xx = 0; xx < numSquaresX; xx++) {
            traversals.add(xx);
        }
        if (vector.getX() == 1) {
            Collections.reverse(traversals);
        }

        return traversals;
    }

    private List<Integer> buildTraversalsY(Cell vector) {
        List<Integer> traversals = new ArrayList<Integer>();

        for (int xx = 0; xx < numSquaresY; xx++) {
            traversals.add(xx);
        }
        if (vector.getY() == 1) {
            Collections.reverse(traversals);
        }

        return traversals;
    }

    private Cell[] findFarthestPosition(Cell cell, Cell vector) {
        Cell previous;
        Cell nextCell = new Cell(cell.getX(), cell.getY());
        do {
            previous = nextCell;
            nextCell = new Cell(previous.getX() + vector.getX(),
                    previous.getY() + vector.getY());
        } while (grid.isCellWithinBounds(nextCell)
                && grid.isCellAvailable(nextCell));

        Cell[] answer = {previous, nextCell};
        return answer;
    }


    public boolean tileMatchesAvailable() {
        Tile tile;

        for (int xx = 0; xx < numSquaresX; xx++) {
            for (int yy = 0; yy < numSquaresY; yy++) {
                tile = grid.getCellContent(new Cell(xx, yy));

                if (tile != null) {
                    for (int direction = 0; direction < 4; direction++) {
                        Cell vector = getVector(direction);
                        Cell cell = new Cell(xx + vector.getX(), yy
                                + vector.getY());

                        Tile other = grid.getCellContent(cell);

                        if (other != null
                                && other.getValue() == tile.getValue()) {
                            return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private boolean positionsEqual(Cell first, Cell second) {
        return first.getX() == second.getX() && first.getY() == second.getY();
    }

    private int winValue() {
        if (!canContinue()) {
            return endingMaxValue;
        } else {
            return startingMaxValue;
        }
    }

    public void setEndlessMode() {
        gameState = GAME_ENDLESS;
        mView.invalidate();
        mView.refreshLastTime = true;
    }

    public boolean canContinue() {
        return !(gameState == GAME_ENDLESS || gameState == GAME_ENDLESS_WON);
    }
    private void initSoundPool() {
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setAudioAttributes(attrs)
                .setMaxStreams(2)
                .build();

        soundMap = new HashMap<>();

        // 1 – движение (мягкий "тик")
        soundMap.put(1, soundPool.load(mView.getContext(), R.raw.erok, 1));

        // 2 – появление плитки ("блум")
        soundMap.put(2, soundPool.load(mView.getContext(), R.raw.erok, 1));

        // 3 – объединение ("поп")
        soundMap.put(3, soundPool.load(mView.getContext(), R.raw.erok, 1));

        // 4 – большое объединение (512+)
        soundMap.put(4, soundPool.load(mView.getContext(), R.raw.merge, 1));

        // 5 – победа (красивый джингл)
        soundMap.put(5, soundPool.load(mView.getContext(), R.raw.lose, 1));

        // 6 – проигрыш (мягкий "бум")
        soundMap.put(6, soundPool.load(mView.getContext(), R.raw.lose, 1));
    }


    private void playSound(int id) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        boolean isSoundEnabled = prefs.getBoolean("sound_enabled", true);

        if (!isSoundEnabled) return;

        AudioManager audio = (AudioManager) mView.getContext().getSystemService(Context.AUDIO_SERVICE);
        float maxVolume = (float) audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        float currentVolume = (float) audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        float volume = currentVolume / maxVolume;

        if (soundMap.containsKey(id)) {
            // ОСТАНАВЛИВАЕМ старый звук перед запуском нового
            // Это не даст звукам "стакаться" и бить по ушам
            if (currentStreamId != 0) {
                soundPool.stop(currentStreamId);
            }

            // Запускаем звук и сохраняем его ID в currentStreamId
            currentStreamId = soundPool.play(soundMap.get(id), volume, volume, 1, 0, 1f);
        }
    }
}
