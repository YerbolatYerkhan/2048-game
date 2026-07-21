package com.ereke.qadam2048;

import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

public class InputListener implements View.OnTouchListener {

    private static final String TAG = "INPUT_DBG";
    private MainView mView;
    private MainActivity mActivity;

    private float x, y;
    private float startingX, startingY;
    private float previousX, previousY;
    private boolean hasMoved;

    private static final int SWIPE_MIN_DISTANCE = 15;
    private static final int MOVE_THRESHOLD = 100;

    public InputListener(MainView view, MainActivity activity) {
        this.mView = view;
        this.mActivity = activity;
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        // 1. Проверка на проигрыш (остается без изменений)
        if (mView.game.gameLost()) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                this.x = event.getX();
                this.y = event.getY();
                if (iconPressed(mView.sXNewGame, mView.sYIcons)) {
                    restartGame();
                }
            }
            return true;
        }

        // 2. ПЕРЕХВАТЧИК ДЛЯ МОЛОТКА
        // Если активирован молоток, мы перехватываем клик и не пускаем его к свайпам
        if (mActivity.isHammerMode) {
            if (event.getAction() == MotionEvent.ACTION_DOWN) {
                float touchX = event.getX();
                float touchY = event.getY();

                int cellX = (int) ((touchX - mView.startingX) / (mView.cellSize + mView.gridWidth));
                int cellY = (int) ((touchY - mView.startingY) / (mView.cellSize + mView.gridWidth));

                // Проверяем, что игрок попал точно по сетке, а не в молоко
                if (cellX >= 0 && cellX < mView.game.grid.field.length &&
                        cellY >= 0 && cellY < mView.game.grid.field[0].length) {

                    mView.game.removeTileAt(cellX, cellY); // Разбиваем плитку
                    mActivity.isHammerMode = false;        // Отключаем режим
                    Toast.makeText(mActivity, "БАМ! Плитка разбита 🔨", Toast.LENGTH_SHORT).show();
                    mView.invalidate();                    // Перерисовываем поле
                }
            }
            return true; // Возвращаем true, чтобы заблокировать любые свайпы, пока молоток в руке!
        }

        // 3. СТАНДАРТНАЯ ОБРАБОТКА СВАЙПОВ
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                x = event.getX();
                y = event.getY();
                startingX = x;
                startingY = y;
                previousX = x;
                previousY = y;
                hasMoved = false;
                return true;

            case MotionEvent.ACTION_MOVE:
                x = event.getX();
                y = event.getY();

                if (mView.game.isActive()) {
                    float totalDx = x - startingX;
                    float totalDy = y - startingY;

                    int dynamicThreshold = mActivity.getMoveThreshold();

                    if (pathMoved() > dynamicThreshold * dynamicThreshold) {
                        boolean moved = false;
                        boolean actuallyMoved = false;

                        if (Math.abs(totalDy) > Math.abs(totalDx)) {
                            if (totalDy >= dynamicThreshold) {
                                moved = true;
                                actuallyMoved = mView.game.move(2); // Вниз
                            } else if (totalDy <= -dynamicThreshold) {
                                moved = true;
                                actuallyMoved = mView.game.move(0); // Вверх
                            }
                        } else {
                            if (totalDx >= dynamicThreshold) {
                                moved = true;
                                actuallyMoved = mView.game.move(1); // Вправо
                            } else if (totalDx <= -dynamicThreshold) {
                                moved = true;
                                actuallyMoved = mView.game.move(3); // Влево
                            }
                        }

                        if (moved) {
                            hasMoved = true;
                            startingX = x;
                            startingY = y;

                            if (actuallyMoved) {
                                mActivity.onUserMadeMove();
                                mView.resyncTime();
                                mView.invalidate();
                            }
                        }
                    }
                }
                previousX = x;
                previousY = y;
                return true;

            case MotionEvent.ACTION_UP:
                this.x = event.getX();
                this.y = event.getY();
                if (!hasMoved) {
                    handleIconClicks();
                }
                return true;
        }
        return true;
    }

    private void handleIconClicks() {
        // 1. Кнопка RESTART (Новая игра)
        if (iconPressed(mView.sXNewGame, mView.sYIcons)) {
            restartGame();
        }

        // 2. Кнопка UNDO (Отмена хода)
        else if (iconPressed(mView.sXUndo, mView.sYIcons)) {
            if (!mView.game.gameLost()) {
                // Проверяем лимит отмен в MainActivity
                if (mActivity.getUndoCount() > 0) {
                    mView.game.revertUndoState();
                    mActivity.useUndo(); // Уменьшаем лимит и сохраняем
                    mView.invalidate();
                } else {
                    // Если лимит 0 — показываем твой диалог с рекламой
                    mActivity.showRewardedAdForUndo();
                }
            }
        }

        // 3. Кнопка CHEAT (Молния) - ТЕПЕРЬ С ЛИМИТОМ
        else if (iconPressed(mView.sXCheat, mView.sYIcons)) {
            // Проверяем лимит читов в MainActivity (по аналогии с Undo)
            if (mActivity.getCheatCount() > 0) {
                // Если попытки есть — запускаем сам чит
                mView.game.cheat();
                // Уменьшаем счетчик читов в памяти и SharedPreferences
                mActivity.useCheat();
                mView.invalidate();
            } else {
                mActivity.showRewardedAdForCheat();
            }
        }

        // 4. Обработка Double Tap для Endless Mode
        else if (isTap(2) && inRange(mView.startingX, x, mView.endingX)
                && inRange(mView.startingY, y, mView.endingY) && mView.continueButtonEnabled) {
            mView.game.setEndlessMode();
            mView.invalidate();
        }
    }


    private void restartGame() {
        mView.game.newGame();
        mActivity.resetHearts();
        if (mActivity.isChallengeModeActive()) {
            mActivity.startChallengeTimer();
        }
        mView.invalidate();
    }

    private double pathMoved() {
        return (x - startingX) * (x - startingX) + (y - startingY) * (y - startingY);
    }

    private boolean isTap(int factor) {
        return pathMoved() <= SWIPE_MIN_DISTANCE * factor;
    }


    private boolean inRange(float start, float value, float end) {
        return (value >= start && value <= end);
    }

    private boolean iconPressed(int iconX, int iconY) {
        return inRange(iconX, this.x, iconX + mView.iconSize)
                && inRange(iconY, this.y, iconY + mView.iconSize);
    }

    public void resetInput() {
        x = 0; y = 0;
        startingX = 0; startingY = 0;
        hasMoved = false;
    }
}