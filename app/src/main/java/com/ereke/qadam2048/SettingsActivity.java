package com.ereke.qadam2048;

import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

import com.ereke.qadam2048.R;

public class SettingsActivity extends AppCompatActivity {

    private SwitchCompat switchSound, switchDark, switchCloud;
    private Button btnTutorial, btnAchievements, btnPrivacy, btnRate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_settings_panel);

        // Инициализация компонентов
        switchSound = findViewById(R.id.switch_sound);
        switchDark = findViewById(R.id.switch_dark_mode);
        switchCloud = findViewById(R.id.switch_cloud);

        btnTutorial = findViewById(R.id.btn_tutorial);

        // Пример обработки переключателя (Звук)
        switchSound.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Включить звук
            } else {
                // Выключить звук
            }
        });

        // Пример обработки кнопки (Учебник)
        btnTutorial.setOnClickListener(v -> {
            Toast.makeText(SettingsActivity.this, "Открываем учебник...", Toast.LENGTH_SHORT).show();
        });

        // Здесь можно добавить логику SharedPreferences для сохранения настроек
    }
}