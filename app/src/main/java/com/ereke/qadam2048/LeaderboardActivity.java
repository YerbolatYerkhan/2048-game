package com.ereke.qadam2048;

import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class LeaderboardActivity extends AppCompatActivity {

    private boolean isDark;
    private LeaderboardPagerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 1. КОНТРОЛЬНЫЙ ВЫСТРЕЛ: Запрещаем Android "помогать" с цветами в темной теме
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            getWindow().getDecorView().setForceDarkAllowed(false);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_leaderboard);

        // Определяем тему для цвета неактивного текста
        isDark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        TabLayout tabLayout = findViewById(R.id.leaderboardTabs);
        ViewPager2 viewPager = findViewById(R.id.leaderboardPager);

        // Убираем стандартный индикатор, будем рисовать свой овал
        tabLayout.setSelectedTabIndicator(null);
        tabLayout.setTabRippleColor(null);

        // --- ИНИЦИАЛИЗАЦИЯ АДАПТЕРА С СЛУШАТЕЛЕМ ---
        adapter = new LeaderboardPagerAdapter(this);

        // Этот слушатель автоматически обновит твою нижнюю панель,
        // когда данные загрузятся из нужной ветки Firebase
        adapter.setOnMyScoreLoadedListener((myData, myRank) -> {
            runOnUiThread(() -> setupMyStickyScore(myData, myRank));
        });

        viewPager.setAdapter(adapter);

        // Настройка вкладок через кастомную TextView
        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            TextView tv = new TextView(this);
            String[] titles = {"4X4", "5X5", "6X6"};
            tv.setText(titles[position]);
            tv.setGravity(Gravity.CENTER);
            tv.setTypeface(null, Typeface.BOLD);
            tv.setTextSize(14);
            tab.setCustomView(tv);
        }).attach();

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                updateTabVisual(tab, true);
                // При переключении таба можно обнулить старую липкую плашку,
                // пока не загрузятся данные нового режима
                findViewById(R.id.myScoreContainer).setAlpha(0.5f);
            }
            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                updateTabVisual(tab, false);
            }
            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        //  ФИКС ЗАПУСКА: Чтобы первый таб сразу стал белым при старте
        tabLayout.postDelayed(() -> {
            for (int i = 0; i < tabLayout.getTabCount(); i++) {
                updateTabVisual(tabLayout.getTabAt(i), i == tabLayout.getSelectedTabPosition());
            }
        }, 150);
    }

    private void updateTabVisual(TabLayout.Tab tab, boolean isSelected) {
        if (tab == null || tab.getCustomView() == null) return;
        TextView tv = (TextView) tab.getCustomView();

        if (isSelected) {
            GradientDrawable gd = new GradientDrawable();
            gd.setShape(GradientDrawable.RECTANGLE);
            gd.setCornerRadius(dp(100));
            gd.setColor(Color.WHITE);
            gd.mutate();

            tv.setBackground(gd);
            tv.setTextColor(Color.parseColor("#3E2723"));
        } else {
            tv.setBackground(null);
            tv.setTextColor(isDark ? Color.parseColor("#B0BEC5") : Color.parseColor("#8D6E63"));
        }

        tv.setPadding(dp(16), dp(4), dp(16), dp(4));
    }

    public void setupMyStickyScore(UserScore myData, int myRank) {
        View myView = findViewById(R.id.myScoreContainer);
        if (myData != null && myView != null) {
            myView.setAlpha(1.0f); // Данные пришли, делаем панель яркой

            TextView name = myView.findViewById(R.id.nameText);
            TextView score = myView.findViewById(R.id.scoreText);
            TextView rank = myView.findViewById(R.id.rankText);

            name.setText(myData.username);
            score.setText(java.text.NumberFormat.getInstance().format(myData.score));
            rank.setText(myRank > 0 ? String.valueOf(myRank) : "-");

            name.setTextColor(Color.parseColor("#FFC107"));
            score.setTextColor(Color.WHITE);
            rank.setTextColor(Color.parseColor("#FFC107"));

            // Фон для липкой плашки
            GradientDrawable bg = (GradientDrawable)
                    ContextCompat.getDrawable(this, R.drawable.brawl_item_bg_dark).mutate();
            bg.setStroke(dp(2), Color.parseColor("#FFC107"));
            myView.setBackground(bg);
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }
}