package edu.hitsz.aircraftwar.activity;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.OvershootInterpolator;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import edu.hitsz.aircraftwar.R;

/**
 * 主菜单Activity
 * 替代原PC端的GameMenuForm
 */
public class MenuActivity extends AppCompatActivity {

    private Button btnEasy, btnNormal, btnHard, btnBattle, btnRanking;
    private Spinner spinnerSound;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        // 全屏沉浸式
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_FULLSCREEN);

        btnEasy = findViewById(R.id.btn_easy);
        btnNormal = findViewById(R.id.btn_normal);
        btnHard = findViewById(R.id.btn_hard);
        btnBattle = findViewById(R.id.btn_battle);
        btnRanking = findViewById(R.id.btn_ranking);
        spinnerSound = findViewById(R.id.spinner_sound);

        // 音效选择（自定义样式）
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new String[]{"音效：开", "音效：关"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSound.setAdapter(adapter);

        // 按钮点击缩放动画
        setupButtonAnimation(btnEasy);
        setupButtonAnimation(btnNormal);
        setupButtonAnimation(btnHard);
        setupButtonAnimation(btnBattle);
        setupButtonAnimation(btnRanking);

        // 简单模式
        btnEasy.setOnClickListener(v -> startGame("EASY"));

        // 普通模式
        btnNormal.setOnClickListener(v -> startGame("NORMAL"));

        // 困难模式
        btnHard.setOnClickListener(v -> startGame("HARD"));

        // 联机对战
        btnBattle.setOnClickListener(v -> showBattleDialog());

        // 排行榜
        btnRanking.setOnClickListener(v -> {
            Intent intent = new Intent(MenuActivity.this, RankingActivity.class);
            intent.putExtra("difficulty", "EASY");
            startActivity(intent);
        });

        // 入场动画
        playEnterAnimation();
    }

    /**
     * 为按钮添加按压缩放效果
     */
    private void setupButtonAnimation(View button) {
        button.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case android.view.MotionEvent.ACTION_DOWN:
                    v.animate().scaleX(0.93f).scaleY(0.93f).setDuration(100).start();
                    break;
                case android.view.MotionEvent.ACTION_UP:
                case android.view.MotionEvent.ACTION_CANCEL:
                    v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                    break;
            }
            return false; // 不消费事件，让onClick也能触发
        });
    }

    /**
     * 按钮入场动画（从下方弹入）
     */
    private void playEnterAnimation() {
        View[] buttons = {btnEasy, btnNormal, btnHard, btnBattle, btnRanking};
        for (int i = 0; i < buttons.length; i++) {
            View btn = buttons[i];
            btn.setAlpha(0f);
            btn.setTranslationY(80f);
            btn.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(500)
                    .setStartDelay(200 + i * 100L)
                    .setInterpolator(new OvershootInterpolator(1.0f))
                    .start();
        }
    }

    private void startGame(String difficulty) {
        boolean soundEnabled = spinnerSound.getSelectedItemPosition() == 0;
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("difficulty", difficulty);
        intent.putExtra("soundEnabled", soundEnabled);
        startActivity(intent);
    }

    private void showBattleDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 20, 50, 0);

        EditText inputRoom = new EditText(this);
        inputRoom.setHint("房间号，例如 room1001");
        layout.addView(inputRoom);

        EditText inputName = new EditText(this);
        inputName.setHint("玩家名");
        inputName.setText("Player");
        layout.addView(inputName);

        Spinner difficultySpinner = new Spinner(this);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new String[]{"简单", "普通", "困难"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        difficultySpinner.setAdapter(adapter);
        layout.addView(difficultySpinner);

        new AlertDialog.Builder(this)
                .setTitle("联机对战房间")
                .setMessage("两名玩家输入同一个房间号，分别完成自己的游戏后查看得分高低。")
                .setView(layout)
                .setPositiveButton("开始对战", (dialog, which) -> {
                    String roomId = inputRoom.getText().toString().trim();
                    String playerName = inputName.getText().toString().trim();
                    if (TextUtils.isEmpty(roomId)) roomId = "room1001";
                    if (TextUtils.isEmpty(playerName)) playerName = "Player";

                    String difficulty;
                    switch (difficultySpinner.getSelectedItemPosition()) {
                        case 1: difficulty = "NORMAL"; break;
                        case 2: difficulty = "HARD"; break;
                        default: difficulty = "EASY"; break;
                    }

                    boolean soundEnabled = spinnerSound.getSelectedItemPosition() == 0;
                    Intent intent = new Intent(this, GameActivity.class);
                    intent.putExtra("difficulty", difficulty);
                    intent.putExtra("soundEnabled", soundEnabled);
                    intent.putExtra("battleMode", true);
                    intent.putExtra("battleRoomId", roomId);
                    intent.putExtra("battlePlayerName", playerName);
                    startActivity(intent);
                })
                .setNegativeButton("取消", null)
                .show();
    }
}
