package edu.hitsz.aircraftwar.activity;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
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

        // 全屏
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
