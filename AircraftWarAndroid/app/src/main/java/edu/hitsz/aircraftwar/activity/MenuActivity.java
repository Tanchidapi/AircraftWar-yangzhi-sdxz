package edu.hitsz.aircraftwar.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;

import edu.hitsz.aircraftwar.R;

/**
 * 主菜单Activity
 * 替代原PC端的GameMenuForm
 */
public class MenuActivity extends AppCompatActivity {

    private Button btnEasy, btnNormal, btnHard, btnRanking;
    private Spinner spinnerSound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        btnEasy = findViewById(R.id.btn_easy);
        btnNormal = findViewById(R.id.btn_normal);
        btnHard = findViewById(R.id.btn_hard);
        btnRanking = findViewById(R.id.btn_ranking);
        spinnerSound = findViewById(R.id.spinner_sound);

        // 音效选择
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new String[]{"音效：开", "音效：关"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSound.setAdapter(adapter);

        // 简单模式
        btnEasy.setOnClickListener(v -> startGame("EASY"));

        // 普通模式
        btnNormal.setOnClickListener(v -> startGame("NORMAL"));

        // 困难模式
        btnHard.setOnClickListener(v -> startGame("HARD"));

        // 排行榜
        btnRanking.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, RankingActivity.class);
            intent.putExtra("difficulty", "EASY");
            startActivity(intent);
        });
    }

    private void startGame(String difficulty) {
        boolean soundEnabled = spinnerSound.getSelectedItemPosition() == 0;
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("difficulty", difficulty);
        intent.putExtra("soundEnabled", soundEnabled);
        startActivity(intent);
    }
}
