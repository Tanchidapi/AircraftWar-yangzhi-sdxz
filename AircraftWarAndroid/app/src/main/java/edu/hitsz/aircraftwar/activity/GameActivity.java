package edu.hitsz.aircraftwar.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import edu.hitsz.aircraftwar.application.*;
import edu.hitsz.aircraftwar.dao.Score;
import edu.hitsz.aircraftwar.dao.ScoreDao;
import edu.hitsz.aircraftwar.dao.ScoreDaoImpl;
import edu.hitsz.aircraftwar.network.OnlineRankingManager;

/**
 * 游戏Activity
 * 承载GameSurfaceView，管理游戏生命周期
 */
public class GameActivity extends AppCompatActivity {

    private GameSurfaceView gameSurfaceView;
    private ScoreDao scoreDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 全屏沉浸式 —— 隐藏状态栏、导航栏，消除黑边
        // 注意：必须在 setContentView 之前设置 Window flags
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // 保持屏幕常亮
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // 获取参数
        String difficulty = getIntent().getStringExtra("difficulty");
        boolean soundEnabled = getIntent().getBooleanExtra("soundEnabled", true);

        if (difficulty == null) difficulty = "EASY";

        scoreDao = new ScoreDaoImpl(this);

        // 根据难度创建对应的游戏视图（模板方法模式）
        switch (difficulty) {
            case "NORMAL":
                gameSurfaceView = new NormalGame(this, soundEnabled);
                break;
            case "HARD":
                gameSurfaceView = new HardGame(this, soundEnabled);
                break;
            default:
                gameSurfaceView = new EasyGame(this, soundEnabled);
                break;
        }

        // 设置游戏结束回调
        gameSurfaceView.setOnGameOverListener((score, diff) -> {
            showGameOverDialog(score, diff);
        });

        setContentView(gameSurfaceView);

        // 在 setContentView 之后设置沉浸式全屏（此时 DecorView 已创建）
        enableImmersiveFullscreen();
    }

    /**
     * 启用沉浸式全屏模式
     * 必须在 setContentView 之后调用，否则 getInsetsController() 可能返回 null
     */
    private void enableImmersiveFullscreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ 使用 WindowInsetsController
            try {
                getWindow().setDecorFitsSystemWindows(false);
                android.view.WindowInsetsController controller = getWindow().getInsetsController();
                if (controller != null) {
                    controller.hide(android.view.WindowInsets.Type.systemBars());
                    controller.setSystemBarsBehavior(
                            android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
                }
            } catch (Exception e) {
                // 回退到旧方式
                setLegacyFullscreen();
            }
        } else {
            setLegacyFullscreen();
        }
    }

    /**
     * 旧版全屏方式（Android 10 及以下）
     */
    private void setLegacyFullscreen() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        // 当窗口重新获得焦点时，重新进入沉浸式模式
        if (hasFocus) {
            enableImmersiveFullscreen();
        }
    }

    /**
     * 游戏结束对话框
     */
    private void showGameOverDialog(int score, String difficulty) {
        EditText input = new EditText(this);
        input.setHint("请输入您的名字");
        input.setText("Player");

        new AlertDialog.Builder(this)
                .setTitle("游戏结束")
                .setMessage("您的得分：" + score)
                .setView(input)
                .setCancelable(false)
                .setPositiveButton("确定", (dialog, which) -> {
                    String playerName = input.getText().toString().trim();
                    if (TextUtils.isEmpty(playerName)) {
                        playerName = "Player";
                    }

                    String currentTime = LocalDateTime.now()
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                    // 保存到本地SQLite
                    Score newScore = new Score(playerName, score, currentTime, difficulty);
                    scoreDao.doAdd(newScore);

                    // 尝试提交到在线排行榜
                    OnlineRankingManager.getInstance().submitScore(newScore, success -> {
                        if (success) {
                            Toast.makeText(GameActivity.this,
                                    "✅ 分数已同步到在线排行榜", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(GameActivity.this,
                                    "⚠ 在线同步失败，分数已保存到本地", Toast.LENGTH_SHORT).show();
                        }
                    });

                    // 跳转到排行榜
                    Intent intent = new Intent(GameActivity.this, RankingActivity.class);
                    intent.putExtra("difficulty", difficulty);
                    intent.putExtra("currentScore", score);
                    startActivity(intent);
                    finish();
                })
                .show();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (gameSurfaceView != null) {
            gameSurfaceView.stopGame();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (gameSurfaceView != null) {
            gameSurfaceView.stopGame();
        }
    }
}
