package com.example.ereke2048;

import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

public class InputListener implements View.OnTouchListener {

    private MainView mView;
    private MainActivity mActivity;

    private float x, y;
    private float startingX, startingY;
    private float previousX, previousY;
    private float lastdx, lastdy;
    private boolean hasMoved;

    private int previousDirection = 1;
    private int veryLastDirection = 1;

    private static final int SWIPE_MIN_DISTANCE = 15;
    private static final int SWIPE_THRESHOLD_VELOCITY = 100;
    private static final int MOVE_THRESHOLD = 100;
    private static final int RESET_STARTING = 10;
    private Log Log;

    public InputListener(MainView view, MainActivity activity) {
        super();
        this.mView = view;
        this.mActivity = activity; // Запоминаем активити
    }


    @Override
    public boolean onTouch(View view, MotionEvent event) {
        // Если проиграли — блокируем ВСЁ, кроме системных кнопок оверлея
        if (mView.game.gameLost()) {
            // Если проиграли, мы разрешаем ТОЛЬКО нажатие кнопки "New Game"
            if (event.getAction() == MotionEvent.ACTION_UP) {
                float x = event.getX();
                float y = event.getY();

                // Проверяем нажатие на иконку Новой игры (чтобы игрок мог сбросить игру)
                if (iconPressed(mView.sXNewGame, mView.sYIcons)) {
                    mView.game.newGame();
                    mActivity.resetHearts();
                    if (mActivity.isChallengeModeActive()) {
                        mActivity.startChallengeTimer();
                    }
                    mView.setOnTouchListener(new InputListener(mView, mActivity));
                    mView.invalidate();
                }
            }
            // Возвращаем true, чтобы свайпы и реклама НЕ срабатывали сами по себе
            return true;
        }

        Log.v("INPUT_DBG", "onTouch action=" + event.getAction() + " hasMoved=" + hasMoved);
        switch (event.getAction()) {

            case MotionEvent.ACTION_DOWN:
                x = event.getX();
                y = event.getY();
                startingX = x;
                startingY = y;
                previousX = x;
                previousY = y;
                lastdx = 0;
                lastdy = 0;
                hasMoved = false;
                return true;

            case MotionEvent.ACTION_MOVE:
                x = event.getX();
                y = event.getY();

                if (mView.game.isActive()) {
                    // ... (твой код расчета dx, dy остается без изменений до проверки pathMoved) ...

                    float dx = x - previousX;
                    if (Math.abs(lastdx + dx) < Math.abs(lastdx) + Math.abs(dx)
                            && Math.abs(dx) > RESET_STARTING
                            && Math.abs(x - startingX) > SWIPE_MIN_DISTANCE) {
                        startingX = x;
                        startingY = y;
                        lastdx = dx;
                        previousDirection = veryLastDirection;
                    }
                    if (lastdx == 0) lastdx = dx;

                    float dy = y - previousY;
                    if (Math.abs(lastdy + dy) < Math.abs(lastdy) + Math.abs(dy)
                            && Math.abs(dy) > RESET_STARTING
                            && Math.abs(y - startingY) > SWIPE_MIN_DISTANCE) {
                        startingX = x;
                        startingY = y;
                        lastdy = dy;
                        previousDirection = veryLastDirection;
                    }
                    if (lastdy == 0) lastdy = dy;

                    if (pathMoved() > SWIPE_MIN_DISTANCE * SWIPE_MIN_DISTANCE) {
                        boolean moved = false;          // Распознан ли жест свайпа
                        boolean actuallyMoved = false;  // Сдвинулись ли плитки в игре

                        if (((dy >= SWIPE_THRESHOLD_VELOCITY && previousDirection == 1) || y - startingY >= MOVE_THRESHOLD) && previousDirection % 2 != 0) {
                            moved = true;
                            previousDirection *= 2;
                            veryLastDirection = 2;
                            actuallyMoved = mView.game.move(2); // Записываем результат: true или false
                        } else if (((dy <= -SWIPE_THRESHOLD_VELOCITY && previousDirection == 1) || y - startingY <= -MOVE_THRESHOLD) && previousDirection % 3 != 0) {
                            moved = true;
                            previousDirection *= 3;
                            veryLastDirection = 3;
                            actuallyMoved = mView.game.move(0);
                        } else if (((dx >= SWIPE_THRESHOLD_VELOCITY && previousDirection == 1) || x - startingX >= MOVE_THRESHOLD) && previousDirection % 5 != 0) {
                            moved = true;
                            previousDirection *= 5;
                            veryLastDirection = 5;
                            actuallyMoved = mView.game.move(1);
                        } else if (((dx <= -SWIPE_THRESHOLD_VELOCITY && previousDirection == 1) || x - startingX <= -MOVE_THRESHOLD) && previousDirection % 7 != 0) {
                            moved = true;
                            previousDirection *= 7;
                            veryLastDirection = 7;
                            actuallyMoved = mView.game.move(3);
                        }

                        if (moved) {
                            hasMoved = true;
                            startingX = x;
                            startingY = y;

                            // 🔥 ФИКС БАГА 1: Сбрасываем таймер ТОЛЬКО если был реальный ход плиток
                            if (actuallyMoved) {
                                mActivity.onUserMadeMove();
                            }
                        }
                    }
                }

                previousX = x;
                previousY = y;
                return true;
            case MotionEvent.ACTION_UP:
                x = event.getX();
                y = event.getY();
                previousDirection = 1;
                veryLastDirection = 1;

                // 📲 Обработка нажатий на иконки
                if (!hasMoved) {
                    // 🔄 Новая игра
                    // 🔄 Новая игра
                    if (iconPressed(mView.sXNewGame, mView.sYIcons)) {
                        mView.game.newGame();

                        // 🔥 ДОБАВЛЯЕМ ЗДЕСЬ: Восстановление сердечек
                        mActivity.resetHearts();

                        // ✅ СБРАСЫВАЕМ И ЗАПУСКАЕМ ТАЙМЕР
                        if (mActivity.isChallengeModeActive()) {
                            mActivity.startChallengeTimer();
                        }

                        // ✅ ВОССТАНАВЛИВАЕМ слушатель свайпов
                        mView.setOnTouchListener(new InputListener(mView, mActivity));
                        mView.invalidate();
                    }
                    // ⏪ Undo (только если не проиграл)
                    else if (iconPressed(mView.sXUndo, mView.sYIcons)) {
                        if (!mView.game.gameLost()) {
                            mView.game.revertUndoState();
                        }
                    }
                    // 💡 Чит
                    else if (iconPressed(mView.sXCheat, mView.sYIcons)) {
                        mView.game.cheat();
                    }
                    // ➡️ Кнопка Continue (для режима endless)
                    else if (isTap(2)
                            && inRange(mView.startingX, x, mView.endingX)
                            && inRange(mView.startingY, x, mView.endingY)
                            && mView.continueButtonEnabled) {
                        mView.game.setEndlessMode();
                    }
                }
                return true;
        }
        return true;
    }


    public void resetInput() {
        x = y = startingX = startingY = previousX = previousY = 0f;
        lastdx = lastdy = 0f;
        hasMoved = false;
        previousDirection = 1;
        veryLastDirection = 1;
        Log.d("INPUT_DBG", "resetInput called");
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

    private boolean iconPressed(int x, int y) {
        return inRange(x, this.x, x + mView.iconSize)
                && inRange(y, this.y, y + mView.iconSize);
    }
}
