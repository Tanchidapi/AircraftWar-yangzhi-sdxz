package edu.hitsz.aircraftwar.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import edu.hitsz.aircraftwar.R;
import edu.hitsz.aircraftwar.network.BattleRoomManager;
import edu.hitsz.aircraftwar.network.OnlineRankingManager;

/**
 * 联机对战房间。负责创建/加入、准备、开始、结算、再来一局和退出。
 * 流程：创建/加入房间 -> 两人都准备 -> 自动开始游戏 -> 结算 -> 再来一局(需双方同意)/退出
 */
public class BattleRoomActivity extends AppCompatActivity {

    private static final long POLL_INTERVAL_MS = 2000L;

    private String roomId;
    private String currentPlayerName;
    private boolean joinedRoom;
    private boolean gameLaunched;
    private int seenRoundNo = 1;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable pollTask = new Runnable() {
        @Override
        public void run() {
            if (joinedRoom) loadRoom(false);
            handler.postDelayed(this, POLL_INTERVAL_MS);
        }
    };

    private TextView tvTitle;
    private TextView tvStatus;
    private TextView tvPlayerOne;
    private TextView tvPlayerTwo;
    private TextView tvWinner;
    private TextView tvServer;
    private EditText inputServerHost;
    private EditText inputServerPort;
    private EditText inputRoom;
    private EditText inputName;
    private Spinner spinnerDifficulty;
    private Button btnCreate;
    private Button btnJoin;
    private Button btnReady;
    private Button btnAgain;
    private Button btnRefresh;
    private Button btnLeave;
    private LinearLayout setupPanel;
    private LinearLayout roomPanel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_battle_room);

        roomId = getIntent().getStringExtra("battleRoomId");
        currentPlayerName = getIntent().getStringExtra("battlePlayerName");
        joinedRoom = getIntent().getBooleanExtra("battleJoined", false);
        if (currentPlayerName == null) currentPlayerName = "Player";

        bindViews();
        setupDifficultySpinner();
        setupActions();

        if (joinedRoom && !TextUtils.isEmpty(roomId)) {
            showRoomPanel();
            loadRoom(true);
        } else {
            showSetupPanel();
        }
    }

    private void bindViews() {
        tvTitle = findViewById(R.id.tv_battle_title);
        tvStatus = findViewById(R.id.tv_battle_status);
        tvPlayerOne = findViewById(R.id.tv_player_one);
        tvPlayerTwo = findViewById(R.id.tv_player_two);
        tvWinner = findViewById(R.id.tv_winner);
        tvServer = findViewById(R.id.tv_server_address);
        inputServerHost = findViewById(R.id.input_server_host);
        inputServerPort = findViewById(R.id.input_server_port);
        inputRoom = findViewById(R.id.input_room_id);
        inputName = findViewById(R.id.input_player_name);
        spinnerDifficulty = findViewById(R.id.spinner_battle_difficulty);
        btnCreate = findViewById(R.id.btn_create_room);
        btnJoin = findViewById(R.id.btn_join_room);
        btnReady = findViewById(R.id.btn_ready_room);
        btnAgain = findViewById(R.id.btn_again_room);
        btnRefresh = findViewById(R.id.btn_refresh_room);
        btnLeave = findViewById(R.id.btn_leave_room);
        setupPanel = findViewById(R.id.panel_room_setup);
        roomPanel = findViewById(R.id.panel_room_status);
    }

    private void setupDifficultySpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, new String[]{"简单", "普通", "困难"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDifficulty.setAdapter(adapter);
    }

    private void setupActions() {
        btnCreate.setOnClickListener(v -> createRoom());
        btnJoin.setOnClickListener(v -> joinRoom());
        btnReady.setOnClickListener(v -> readyInRoom());
        btnAgain.setOnClickListener(v -> readyAgain());
        btnRefresh.setOnClickListener(v -> loadRoom(true));
        btnLeave.setOnClickListener(v -> confirmLeave());
    }

    private void applyServerAddress() {
        String host = inputServerHost.getText().toString().trim();
        String portText = inputServerPort.getText().toString().trim();
        if (TextUtils.isEmpty(host)) host = "10.0.2.2";
        int port = 5000;
        try {
            port = Integer.parseInt(portText);
        } catch (NumberFormatException ignored) {
        }
        OnlineRankingManager.setServerAddress(host, port);
        tvServer.setText("服务器: " + OnlineRankingManager.getInstance().getServerUrl());
    }

    private void createRoom() {
        applyServerAddress();
        readInputs();
        if (TextUtils.isEmpty(roomId)) {
            Toast.makeText(this, "请输入房间号", Toast.LENGTH_SHORT).show();
            return;
        }
        String difficulty = getSelectedDifficulty();
        setBusy(true);
        BattleRoomManager.getInstance().createRoom(roomId, currentPlayerName, difficulty, room -> {
            setBusy(false);
            if (room == null) {
                String error = BattleRoomManager.getInstance().getLastError();
                Toast.makeText(this, "创建失败: " + (error != null ? error : "服务器不可用"), Toast.LENGTH_LONG).show();
                return;
            }
            joinedRoom = true;
            gameLaunched = false;
            showRoomPanel();
            bindRoom(room);
            Toast.makeText(this, "房间创建成功，等待对手加入", Toast.LENGTH_SHORT).show();
        });
    }

    private void joinRoom() {
        applyServerAddress();
        readInputs();
        if (TextUtils.isEmpty(roomId)) {
            Toast.makeText(this, "请输入房间号", Toast.LENGTH_SHORT).show();
            return;
        }
        setBusy(true);
        // 加入房间时需要携带难度信息，以免服务器误创建新房间
        String difficulty = getSelectedDifficulty();
        BattleRoomManager.getInstance().joinRoom(roomId, currentPlayerName, difficulty, room -> {
            setBusy(false);
            if (room == null) {
                String error = BattleRoomManager.getInstance().getLastError();
                Toast.makeText(this, "加入失败: " + (error != null ? error : "服务器不可用"), Toast.LENGTH_LONG).show();
                return;
            }
            joinedRoom = true;
            gameLaunched = false;
            showRoomPanel();
            bindRoom(room);
            Toast.makeText(this, "成功加入房间", Toast.LENGTH_SHORT).show();
        });
    }

    private void readyInRoom() {
        BattleRoomManager.getInstance().readyInRoom(roomId, currentPlayerName, room -> {
            if (room == null) {
                String error = BattleRoomManager.getInstance().getLastError();
                Toast.makeText(this, "准备失败: " + (error != null ? error : "请稍后重试"), Toast.LENGTH_SHORT).show();
                return;
            }
            bindRoom(room);
        });
    }

    private void readInputs() {
        roomId = inputRoom.getText().toString().trim();
        currentPlayerName = inputName.getText().toString().trim();
        if (TextUtils.isEmpty(currentPlayerName)) currentPlayerName = "Player";
    }

    private String getSelectedDifficulty() {
        switch (spinnerDifficulty.getSelectedItemPosition()) {
            case 1: return "NORMAL";
            case 2: return "HARD";
            default: return "EASY";
        }
    }

    private void startRoom() {
        BattleRoomManager.getInstance().startRoom(roomId, currentPlayerName, room -> {
            if (room == null) {
                String error = BattleRoomManager.getInstance().getLastError();
                Toast.makeText(this, "开始失败: " + (error != null ? error : "请稍后重试"), Toast.LENGTH_SHORT).show();
                return;
            }
            bindRoom(room);
        });
    }

    private void readyAgain() {
        btnAgain.setEnabled(false);
        btnAgain.setText("已准备，等待对方...");
        BattleRoomManager.getInstance().readyAgain(roomId, currentPlayerName, room -> {
            if (room == null) {
                btnAgain.setEnabled(true);
                btnAgain.setText("再来一局");
                String error = BattleRoomManager.getInstance().getLastError();
                Toast.makeText(this, "操作失败: " + (error != null ? error : "请稍后重试"), Toast.LENGTH_SHORT).show();
                return;
            }
            gameLaunched = false;
            bindRoom(room);
        });
    }

    private void loadRoom(boolean showToastOnError) {
        if (TextUtils.isEmpty(roomId)) return;
        BattleRoomManager.getInstance().getBattleRoom(roomId, room -> {
            if (room == null) {
                if (showToastOnError) {
                    String error = BattleRoomManager.getInstance().getLastError();
                    Toast.makeText(this, "获取房间失败: " + (error != null ? error : "服务器不可用"), Toast.LENGTH_SHORT).show();
                }
                return;
            }
            bindRoom(room);
        });
    }

    private void bindRoom(BattleRoomManager.BattleRoom room) {
        seenRoundNo = room.getRoundNo();
        tvTitle.setText("房间 " + room.getRoomId() + "  第 " + room.getRoundNo() + " 局");
        tvServer.setText("服务器: " + OnlineRankingManager.getInstance().getServerUrl());
        tvStatus.setText(buildStatus(room));

        List<BattleRoomManager.BattlePlayer> players = room.getPlayers();
        tvPlayerOne.setText(formatPlayer(players, 0, room));
        tvPlayerTwo.setText(formatPlayer(players, 1, room));

        boolean isHost = room.getHostName() != null && room.getHostName().equals(currentPlayerName);
        boolean full = players.size() >= 2;
        boolean isWaiting = "waiting".equals(room.getStatus());
        boolean isFinished = "finished".equals(room.getStatus());
        boolean isPlaying = "playing".equals(room.getStatus());

        // 准备按钮：在等待状态且自己还没准备时显示
        boolean myReady = isMyReady(players);
        btnReady.setVisibility(isWaiting && full && !myReady ? View.VISIBLE : View.GONE);
        if (isWaiting && full && myReady) {
            // 已准备，等待对方
            btnReady.setVisibility(View.VISIBLE);
            btnReady.setEnabled(false);
            btnReady.setText("已准备，等待对方...");
        } else if (isWaiting && full && !myReady) {
            btnReady.setVisibility(View.VISIBLE);
            btnReady.setEnabled(true);
            btnReady.setText("准备");
        } else {
            btnReady.setVisibility(View.GONE);
        }

        // 再来一局按钮
        if (isFinished) {
            boolean myReadyNext = isMyReadyNext(players);
            btnAgain.setVisibility(View.VISIBLE);
            if (myReadyNext) {
                btnAgain.setEnabled(false);
                btnAgain.setText("已准备，等待对方...");
            } else {
                btnAgain.setEnabled(true);
                btnAgain.setText("再来一局");
            }
        } else {
            btnAgain.setVisibility(View.GONE);
        }

        bindWinner(room, players);

        // 当状态变为playing且自己还没完成时，启动游戏
        if (isPlaying && !hasFinished(players) && !gameLaunched) {
            launchGame(room);
        }
    }

    private String buildStatus(BattleRoomManager.BattleRoom room) {
        List<BattleRoomManager.BattlePlayer> players = room.getPlayers();
        if ("waiting".equals(room.getStatus())) {
            if (players.size() < 2) {
                return "等待另一位玩家加入...";
            }
            boolean allReady = room.isAllReady();
            if (allReady) {
                return "双方已准备，游戏即将开始...";
            }
            return "两名玩家已到齐，请点击\"准备\"按钮";
        }
        if ("playing".equals(room.getStatus())) {
            return "对局进行中...";
        }
        if ("finished".equals(room.getStatus())) {
            return "本局已结束 - 可选择\"再来一局\"或\"退出房间\"";
        }
        return "房间状态: " + room.getStatus();
    }

    private String formatPlayer(List<BattleRoomManager.BattlePlayer> players, int index,
                                BattleRoomManager.BattleRoom room) {
        if (players.size() <= index) return "玩家" + (index + 1) + ": 等待加入...";
        BattleRoomManager.BattlePlayer player = players.get(index);
        String marker = player.getPlayerName().equals(currentPlayerName) ? " (我)" : "";
        String host = player.isHost() ? " [房主]" : "";
        StringBuilder text = new StringBuilder("玩家" + (index + 1) + ": " + player.getPlayerName() + marker + host);

        if ("finished".equals(room.getStatus()) || player.isFinished()) {
            text.append("\n  得分: ").append(player.getScore() == null ? "等待提交" : player.getScore());
            if (player.getPlayTime() != null) text.append("\n  时间: ").append(player.getPlayTime());
            if (player.isReadyNext()) text.append("\n  [已准备再来一局]");
        } else if ("playing".equals(room.getStatus())) {
            text.append("\n  状态: ").append(player.isFinished() ? "已完成" : "游戏中...");
            text.append("\n  实时得分: ").append(player.getScore() == null ? 0 : player.getScore());
        } else {
            // waiting状态
            text.append("\n  状态: ").append(player.isReady() ? "已准备" : "未准备");
        }
        return text.toString();
    }

    private void bindWinner(BattleRoomManager.BattleRoom room, List<BattleRoomManager.BattlePlayer> players) {
        if (!"finished".equals(room.getStatus()) || players.size() < 2) {
            tvWinner.setVisibility(View.GONE);
            return;
        }
        BattleRoomManager.BattlePlayer winner = null;
        boolean tie = false;
        boolean allScored = true;
        for (BattleRoomManager.BattlePlayer player : players) {
            if (player.getScore() == null) {
                allScored = false;
                continue;
            }
            if (winner == null || player.getScore() > winner.getScore()) {
                winner = player;
                tie = false;
            } else if (player.getScore().equals(winner.getScore())) {
                tie = true;
            }
        }
        if (!allScored || winner == null) {
            tvWinner.setText("等待所有玩家提交分数...");
            tvWinner.setVisibility(View.VISIBLE);
            return;
        }
        tvWinner.setText(tie ? "结果: 平局!" : "胜者: " + winner.getPlayerName() + " (" + winner.getScore() + "分)");
        tvWinner.setVisibility(View.VISIBLE);
    }

    private boolean hasFinished(List<BattleRoomManager.BattlePlayer> players) {
        for (BattleRoomManager.BattlePlayer player : players) {
            if (player.getPlayerName().equals(currentPlayerName)) return player.isFinished();
        }
        return false;
    }

    private boolean isMyReady(List<BattleRoomManager.BattlePlayer> players) {
        for (BattleRoomManager.BattlePlayer player : players) {
            if (player.getPlayerName().equals(currentPlayerName)) return player.isReady();
        }
        return false;
    }

    private boolean isMyReadyNext(List<BattleRoomManager.BattlePlayer> players) {
        for (BattleRoomManager.BattlePlayer player : players) {
            if (player.getPlayerName().equals(currentPlayerName)) return player.isReadyNext();
        }
        return false;
    }

    private void launchGame(BattleRoomManager.BattleRoom room) {
        gameLaunched = true;
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("difficulty", room.getDifficulty());
        intent.putExtra("soundEnabled", true);
        intent.putExtra("battleMode", true);
        intent.putExtra("battleRoomId", room.getRoomId());
        intent.putExtra("battlePlayerName", currentPlayerName);
        intent.putExtra("battleRoundNo", room.getRoundNo());
        startActivity(intent);
    }

    private void confirmLeave() {
        new AlertDialog.Builder(this)
                .setTitle("退出房间")
                .setMessage("确定要退出当前房间吗?")
                .setPositiveButton("退出", (dialog, which) -> leaveRoom())
                .setNegativeButton("取消", null)
                .show();
    }

    private void leaveRoom() {
        if (!joinedRoom) {
            finish();
            return;
        }
        BattleRoomManager.getInstance().leaveRoom(roomId, currentPlayerName, success -> finish());
    }

    private void setBusy(boolean busy) {
        btnCreate.setEnabled(!busy);
        btnJoin.setEnabled(!busy);
    }

    private void showSetupPanel() {
        setupPanel.setVisibility(View.VISIBLE);
        roomPanel.setVisibility(View.GONE);
        tvStatus.setText("输入服务器地址后创建或加入房间");
    }

    private void showRoomPanel() {
        setupPanel.setVisibility(View.GONE);
        roomPanel.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        handler.removeCallbacks(pollTask);
        handler.postDelayed(pollTask, POLL_INTERVAL_MS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(pollTask);
    }
}
