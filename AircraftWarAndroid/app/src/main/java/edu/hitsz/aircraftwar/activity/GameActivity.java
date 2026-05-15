package edu.hitsz.aircraftwar.activity;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import edu.hitsz.aircraftwar.network.BattleRoomManager;
import edu.hitsz.aircraftwar.network.OnlineRankingManager;

/**
 * 游戏Activity
 * 承载GameSurfaceView，管理游戏生命周期
 */
public class GameActivity extends AppCompatActivity {

    private GameSurfaceView gameSurfaceView;
    private ScoreDao scoreDao;
    private boolean battleMode;
    private String battleRoomId;
    private String battlePlayerName;
    private int battleRoundNo;
    private String difficulty;
    private final Handler battleScoreHandler = new Handler(Looper.getMainLooper());
    private int lastSyncedBattleScore = -1;
    private final Runnable battleScoreSyncTask = new Runnable() {
        @Override
        public void run() {
            syncBattleScorePreview();
            battleScoreHandler.postDelayed(this, 1000L);
        }
    };

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
        difficulty = getIntent().getStringExtra("difficulty");
        boolean soundEnabled = getIntent().getBooleanExtra("soundEnabled", true);
        battleMode = getIntent().getBooleanExtra("battleMode", false);
        battleRoomId = getIntent().getStringExtra("battleRoomId");
        battlePlayerName = getIntent().getStringExtra("battlePlayerName");
        battleRoundNo = getIntent().getIntExtra("battleRoundNo", 1);

        if (difficulty == null) difficulty = "EASY";
        if (battlePlayerName == null || battlePlayerName.trim().isEmpty()) battlePlayerName = "Player";

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

        if (battleMode) {
            gameSurfaceView.setOnScoreChangeListener((score, diff) -> syncBattleScorePreview());
        }
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
        if (battleMode) {
            showBattleGameOverDialog(score, difficulty);
            return;
        }

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

    private void showBattleGameOverDialog(int score, String difficulty) {
        battleScoreHandler.removeCallbacks(battleScoreSyncTask);
        String currentTime = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        BattleRoomManager.BattleScore battleScore = new BattleRoomManager.BattleScore(
                battlePlayerName, score, currentTime, difficulty, battleRoundNo);

        new AlertDialog.Builder(this)
                .setTitle("对战结束")
                .setMessage("房间：" + battleRoomId + "\n玩家：" + battlePlayerName + "\n您的得分：" + score)
                .setCancelable(false)
                .setPositiveButton("提交并查看房间", (dialog, which) -> {
                    BattleRoomManager.getInstance().submitBattleScore(battleRoomId, battleScore, room -> {
                        if (room != null) {
                            Toast.makeText(GameActivity.this,
                                    "对战分数已提交", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(GameActivity.this,
                                    "对战分数提交失败，请稍后刷新", Toast.LENGTH_SHORT).show();
                        }
                        Intent intent = new Intent(GameActivity.this, BattleRoomActivity.class);
                        intent.putExtra("battleRoomId", battleRoomId);
                        intent.putExtra("battlePlayerName", battlePlayerName);
                        intent.putExtra("battleJoined", true);
                        startActivity(intent);
                        finish();
                    });
                })
                .show();
    }

    private void syncBattleScorePreview() {
        if (!battleMode || gameSurfaceView == null || gameSurfaceView.isGameOver()) {
            return;
        }
        int currentScore = gameSurfaceView.getScore();
        if (currentScore == lastSyncedBattleScore) {
            return;
        }
        lastSyncedBattleScore = currentScore;
        String currentTime = LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        BattleRoomManager.BattleScore previewScore = new BattleRoomManager.BattleScore(
                battlePlayerName, currentScore, currentTime, difficulty, battleRoundNo);
        BattleRoomManager.getInstance().updateBattleScore(battleRoomId, previewScore, room -> {
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (battleMode) {
            battleScoreHandler.removeCallbacks(battleScoreSyncTask);
            battleScoreHandler.postDelayed(battleScoreSyncTask, 1000L);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        battleScoreHandler.removeCallbacks(battleScoreSyncTask);
        if (gameSurfaceView != null) {
            gameSurfaceView.stopGame();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        battleScoreHandler.removeCallbacks(battleScoreSyncTask);
        if (gameSurfaceView != null) {
            gameSurfaceView.stopGame();
        }
    }
}
