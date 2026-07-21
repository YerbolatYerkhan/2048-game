package com.ereke.qadam2048;

import android.graphics.Color;

import java.util.Random;

public class Particle {
    public float x, y; // Позиция частицы
    public float velocityX, velocityY; // Скорость частицы
    public int color; // Цвет частицы
    public int alpha; // Прозрачность
    public long lifetime; // Сколько частица будет жить (в миллисекундах)
    public long spawnTime; // Время создания частицы

    private static final Random random = new Random();

    public Particle(float startX, float startY, int tileValue, long currentTime) {
        this.x = startX;
        this.y = startY;

        // Рандомная скорость и направление
        float angle = (float) (random.nextDouble() * 2 * Math.PI); // Случайный угол
        float speed = random.nextFloat() * 10 + 5; // Случайная скорость
        this.velocityX = (float) (speed * Math.cos(angle));
        this.velocityY = (float) (speed * Math.sin(angle));

        // Цвет частицы в зависимости от номинала плитки (для сочности)
        this.color = getColorForTileValue(tileValue);
        this.alpha = 255; // Начинаем с полной непрозрачности
        this.lifetime = 300 + random.nextInt(200); // Живет 300-500 мс
        this.spawnTime = currentTime;
    }

    // Обновляем позицию и прозрачность частицы
    public void update(long currentTime) {
        long elapsed = currentTime - spawnTime;
        if (elapsed < lifetime) {
            x += velocityX;
            y += velocityY;

            // Считаем коэффициент затухания от 1.0 до 0.0
            float lifeFactor = 1 - ((float) elapsed / lifetime);
            this.alpha = (int) (255 * lifeFactor);

            // Чтобы частица плавно уменьшалась, а не просто бледнела
            // (Опционально: можно уменьшать и радиус)
        } else {
            this.alpha = 0;
        }
    }

    // Проверяем, жива ли частица
    public boolean isAlive(long currentTime) {
        return (currentTime - spawnTime) < lifetime;
    }

    // Получаем цвет плитки по ее значению (можно взять из MainView)
    private int getColorForTileValue(int value) {
        switch (value) {
            case 2:     return Color.parseColor("#EEE4DA"); // Светлый
            case 4:     return Color.parseColor("#EDE0C8"); // Светлый
            case 8:     return Color.parseColor("#F2B179"); // Оранжевый
            case 16:    return Color.parseColor("#F59563"); // Оранжевый
            case 32:    return Color.parseColor("#F67C5F"); // Красный
            case 64:    return Color.parseColor("#F65E3B"); // Красный
            case 128:   return Color.parseColor("#EDCF72"); // Желтый
            case 256:   return Color.parseColor("#EDCC61"); // Желтый
            case 512:   return Color.parseColor("#EDC850"); // Желтый
            case 1024:  return Color.parseColor("#EDC53F"); // Желтый
            case 2048:  return Color.parseColor("#EDC22E"); // Золотой
            default:    return Color.GRAY; // Дефолтный
        }
    }
}