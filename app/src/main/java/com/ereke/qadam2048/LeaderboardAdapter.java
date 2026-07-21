package com.ereke.qadam2048;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {
    private List<UserScore> scoreList;
    private Typeface boldFont;
    private String currentDeviceId;

    public LeaderboardAdapter(List<UserScore> scoreList, Context context) {
        this.scoreList = scoreList;
        this.boldFont = Typeface.createFromAsset(context.getAssets(), "fonts/montserrat_bold.ttf");
        this.currentDeviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_leaderboard, parent, false);
        return new ViewHolder(view);
    }

    private int interpolateColor(int color1, int color2, float fraction) {
        float[] hsv1 = new float[3];
        float[] hsv2 = new float[3];
        Color.colorToHSV(color1, hsv1);
        Color.colorToHSV(color2, hsv2);
        for (int i = 0; i < 3; i++) {
            hsv1[i] = hsv1[i] + (hsv2[i] - hsv1[i]) * fraction;
        }
        return Color.HSVToColor(hsv1);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserScore user = scoreList.get(position);
        Context context = holder.itemView.getContext();

        // Останавливаем старые анимации перед переиспользованием ячейки
        if (holder.itemView.getTag() instanceof android.animation.ValueAnimator) {
            ((android.animation.ValueAnimator) holder.itemView.getTag()).cancel();
        }
        holder.itemView.clearAnimation();

        // --- 1. ШРИФТЫ ---
        if (boldFont != null) {
            holder.rankText.setTypeface(boldFont);
            holder.nameText.setTypeface(boldFont);
            holder.scoreText.setTypeface(boldFont);
        }

        // --- 2. ДАННЫЕ ---
        holder.nameText.setText(user.username);
        String formattedScore = java.text.NumberFormat.getInstance(java.util.Locale.US).format(user.score);
        holder.scoreText.setText(formattedScore);
        holder.rankText.setText(String.valueOf(position + 1));

        // --- 3. СБРОС СТИЛЕЙ ПО УМОЛЧАНИЮ (Для темной темы) ---
        boolean isMe = user.userId != null && user.userId.equals(currentDeviceId);
        holder.scoreText.setTextSize(18f);
        holder.rankText.setTextColor(Color.parseColor("#8D6E63")); // Приглушенный коричневый
        holder.nameText.setTextColor(Color.WHITE);
        holder.scoreText.setTextColor(Color.parseColor("#FFECB3")); // Светлое золото
        holder.nameText.setShadowLayer(0, 0, 0, 0);

        // --- 4. ЛОГИКА ТОП-3 (С АНИМАЦИЕЙ ПЕРЕЛИВА) ---
        if (position == 0) { // ЗОЛОТО 🥇
            holder.rankText.setText("🥇");
            android.graphics.drawable.GradientDrawable goldGradient = applyTopStyle(holder, "#FFD740", "#FFC107", "#3E2723", true, isMe);

            startGlowAnimation(holder, goldGradient, "#FFD740", "#FF8F00", isMe);

        } else if (position == 1) { // СЕРЕБРО 🥈
            holder.rankText.setText("🥈");
            android.graphics.drawable.GradientDrawable silverGradient = applyTopStyle(holder, "#E0E0E0", "#BDBDBD", "#3E2723", true, isMe);

            startGlowAnimation(holder, silverGradient, "#F5F5F5", "#9E9E9E", isMe);

        } else if (position == 2) { // БРОНЗА 🥉
            holder.rankText.setText("🥉");
            android.graphics.drawable.GradientDrawable bronzeGradient = applyTopStyle(holder, "#E6A15C", "#D37520", "#FFFFFF", false, isMe);

            startGlowAnimation(holder, bronzeGradient, "#FFCC80", "#BF360C", isMe);

        } else if (isMe) { // ТЫ ВНЕ ТОП-3 (Строгий темный стиль с золотой обводкой)
            android.graphics.drawable.GradientDrawable meBg = new android.graphics.drawable.GradientDrawable();
            meBg.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
            meBg.setCornerRadius(dpToPx(context, 16));
            meBg.setColor(Color.parseColor("#2D1B18")); // Темный шоколад
            meBg.setStroke(dpToPx(context, 2), Color.parseColor("#FFC107")); // Золотая рамка

            holder.itemView.setBackground(meBg);
            holder.nameText.setTextColor(Color.parseColor("#FFC107"));
            holder.nameText.setShadowLayer(8f, 0, 0, Color.parseColor("#40FFC107")); // Легкое золотое свечение

            // Анимация пульсации золотой рамки
            android.animation.ValueAnimator anim = android.animation.ValueAnimator.ofFloat(0.4f, 1f);
            anim.setDuration(1500);
            anim.setRepeatCount(android.animation.ValueAnimator.INFINITE);
            anim.setRepeatMode(android.animation.ValueAnimator.REVERSE);
            anim.addUpdateListener(a -> {
                float v = (float) a.getAnimatedValue();
                meBg.setAlpha((int)(v * 255));
            });
            anim.start();
            holder.itemView.setTag(anim);

        } else { // ОБЫЧНЫЙ ИГРОК
            holder.itemView.setBackgroundResource(R.drawable.brawl_item_bg_dark);
            holder.rankText.setTextColor(Color.parseColor("#8D6E63"));
        }

        // Восстанавливаем отступы, так как setBackground их сбрасывает
        int pX = dpToPx(context, 16);
        int pY = dpToPx(context, 10);
        holder.itemView.setPadding(pX, pY, pX, pY);

        setupClickAnimation(holder.itemView);
    }

    private void setFadeAnimation(View view, int position) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(view, "translationY", 300f, 0f);
        animator.setDuration(500);
        animator.setStartDelay(position * 50L); // Поочередное появление
        animator.setInterpolator(new DecelerateInterpolator());
        animator.start();
    }
    // Вспомогательный метод для запуска анимации (чтобы не дублировать код)
    private void startGlowAnimation(ViewHolder holder, android.graphics.drawable.GradientDrawable gd, String cStart, String cEnd, boolean isMe) {
        android.animation.ValueAnimator anim = android.animation.ValueAnimator.ofFloat(0f, 1f);
        anim.setDuration(2500);
        anim.setRepeatCount(android.animation.ValueAnimator.INFINITE);
        anim.setRepeatMode(android.animation.ValueAnimator.REVERSE);
        anim.addUpdateListener(animation -> {
            float v = (float) animation.getAnimatedValue();
            int color1 = interpolateColor(Color.parseColor(cStart), Color.parseColor(cEnd), v);
            gd.setColors(new int[]{color1, Color.parseColor(cEnd)});

            if (isMe) {
                holder.nameText.setShadowLayer(10f * v, 0, 0, Color.WHITE);
            }
        });
        anim.start();
        holder.itemView.setTag(anim);
    }
    private void highlightMe(ViewHolder holder, Context context) {
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable();
        bg.setColor(Color.parseColor("#2B2B2B")); // Цвет обычной карточки
        bg.setCornerRadius(dpToPx(context, 16));
        // Яркая синяя обводка, чтобы сразу видеть себя
        bg.setStroke(dpToPx(context, 3), Color.parseColor("#42A5F5"));
        holder.itemView.setBackground(bg);
    }

    private void startGlowAnimation(View view) {
        android.view.animation.Animation glow = new android.view.animation.AlphaAnimation(0.8f, 1.0f);
        glow.setDuration(1000);
        glow.setRepeatMode(android.view.animation.Animation.REVERSE);
        glow.setRepeatCount(android.view.animation.Animation.INFINITE);
        view.startAnimation(glow);
    }
    private android.graphics.drawable.GradientDrawable applyTopStyle(ViewHolder holder, String startColor, String endColor, String textColorStr, boolean removeShadow, boolean isMe) {
        Context context = holder.itemView.getContext();
        int textColor = Color.parseColor(textColorStr);

        android.graphics.drawable.GradientDrawable topLayer = new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                new int[] {Color.parseColor(startColor), Color.parseColor(endColor)});
        topLayer.setCornerRadius(dpToPx(context, 16));

        // Обводка (синяя для тебя, белая для остальных)
        if (isMe) {
            topLayer.setStroke(dpToPx(context, 3), Color.parseColor("#00FFD1")); // Стартовый бирюзовый
        } else {
            topLayer.setStroke(dpToPx(context, 2), Color.WHITE);
        }

        // Подложка 3D
        android.graphics.drawable.GradientDrawable bottomLayer = new android.graphics.drawable.GradientDrawable();
        bottomLayer.setCornerRadius(dpToPx(context, 16));
        bottomLayer.setColor(adjustAlpha(Color.parseColor(endColor), 0.7f));

        android.graphics.drawable.LayerDrawable layers = new android.graphics.drawable.LayerDrawable(
                new android.graphics.drawable.Drawable[] {bottomLayer, topLayer});
        layers.setLayerInset(1, 0, 0, 0, dpToPx(context, 4));

        holder.itemView.setBackground(layers);
        holder.itemView.setPadding(dpToPx(context, 16), dpToPx(context, 12), dpToPx(context, 16), dpToPx(context, 12));

        holder.nameText.setTextColor(textColor);
        holder.scoreText.setTextColor(textColor);
        holder.rankText.setTextColor(textColor);

        // Возвращаем слой для анимации
        return topLayer;
    }

    // Вспомогательный метод для прозрачности (если нужно)
    private int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    private int dpToPx(Context context, int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    private void setupClickAnimation(View view) {
        view.setOnTouchListener((v, event) -> {
            if (event.getAction() == MotionEvent.ACTION_DOWN)
                v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
            if (event.getAction() == MotionEvent.ACTION_UP || event.getAction() == MotionEvent.ACTION_CANCEL)
                v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
            return false;
        });
    }

    @Override
    public int getItemCount() { return scoreList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView rankText, nameText, scoreText;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            rankText = itemView.findViewById(R.id.rankText);
            nameText = itemView.findViewById(R.id.nameText);
            scoreText = itemView.findViewById(R.id.scoreText);
        }
    }
}