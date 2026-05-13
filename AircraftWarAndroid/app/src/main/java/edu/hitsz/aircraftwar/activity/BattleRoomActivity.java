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
 * 联机对战房间。负责创建/加入、等待、开始、结算、再来一局和退出。
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
    private Button btnStart;
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
        btnStart = findViewById(R.id.btn_start_room);
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
        btnStart.setOnClickListener(v -> startRoom());
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
        tvServer.setText("服务器：" + OnlineRankingManager.getInstance().getServerUrl());
    }

    private void createRoom() {
        applyServerAddress();
        readInputs();
        String difficulty = getSelectedDifficulty();
        setBusy(true);
        BattleRoomManager.getInstance().createRoom(roomId, currentPlayerName, difficulty, room -> {
            setBusy(false);
            if (room == null) {
                Toast.makeText(this, "创建失败：房间可能已存在或服务器不可用", Toast.LENGTH_SHORT).show();
                return;
            }
            joinedRoom = true;
            showRoomPanel();
            bindRoom(room);
        });
    }

    private void joinRoom() {
        applyServerAddress();
        readInputs();
        setBusy(true);
        BattleRoomManager.getInstance().joinRoom(roomId, currentPlayerName, room -> {
            setBusy(false);
            if (room == null) {
                Toast.makeText(this, "加入失败：房间不存在、已满或服务器不可用", Toast.LENGTH_SHORT).show();
                return;
            }
            joinedRoom = true;
            showRoomPanel();
            bindRoom(room);
        });
    }

    private void readInputs() {
        roomId = inputRoom.getText().toString().trim();
        currentPlayerName = inputName.getText().toString().trim();
        if (TextUtils.isEmpty(roomId)) roomId = "room1001";
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
                Toast.makeText(this, "开始失败：需两名玩家就绪且只有房主可开始", Toast.LENGTH_SHORT).show();
                return;
            }
            bindRoom(room);
        });
    }

    private void readyAgain() {
        BattleRoomManager.getInstance().readyAgain(roomId, currentPlayerName, room -> {
            if (room == null) {
                Toast.makeText(this, "操作失败，请稍后重试", Toast.LENGTH_SHORT).show();
                return;
            }
            gameLaunched = false;
            bindRoom(room);
        });
    }

    private void loadRoom(boolean showToastOnError) {
        BattleRoomManager.getInstance().getBattleRoom(roomId, room -> {
            if (room == null) {
                if (showToastOnError) Toast.makeText(this, "房间状态获取失败", Toast.LENGTH_SHORT).show();
                return;
            }
            bindRoom(room);
        });
    }

    private void bindRoom(BattleRoomManager.BattleRoom room) {
        seenRoundNo = room.getRoundNo();
        tvTitle.setText("房间 " + room.getRoomId() + "  第 " + room.getRoundNo() + " 局");
        tvServer.setText("服务器：" + OnlineRankingManager.getInstance().getServerUrl());
        tvStatus.setText(buildStatus(room));

        List<BattleRoomManager.BattlePlayer> players = room.getPlayers();
        tvPlayerOne.setText(formatPlayer(players, 0, room));
        tvPlayerTwo.setText(formatPlayer(players, 1, room));

        boolean isHost = room.getHostName() != null && room.getHostName().equals(currentPlayerName);
        boolean full = players.size() >= 2;
        btnStart.setVisibility(isHost && full && "waiting".equals(room.getStatus()) ? View.VISIBLE : View.GONE);
        btnAgain.setVisibility("finished".equals(room.getStatus()) ? View.VISIBLE : View.GONE);

        bindWinner(room, players);

        if ("playing".equals(room.getStatus()) && !hasFinished(players) && !gameLaunched) {
            launchGame(room);
        }
    }

    private String buildStatus(BattleRoomManager.BattleRoom room) {
        if ("waiting".equals(room.getStatus())) {
            return room.getPlayers().size() < 2 ? "等待另一位玩家加入" : "两名玩家已就绪，等待房主开始";
        }
        if ("playing".equals(room.getStatus())) {
            return "对局进行中，完成后会回到房间结算";
        }
        if ("finished".equals(room.getStatus())) {
            return "本局已结束，可以再来一局或退出房间";
        }
        return "房间状态：" + room.getStatus();
    }

    private String formatPlayer(List<BattleRoomManager.BattlePlayer> players, int index,
                                BattleRoomManager.BattleRoom room) {
        if (players.size() <= index) return "玩家" + (index + 1) + "：等待加入";
        BattleRoomManager.BattlePlayer player = players.get(index);
        String marker = player.getPlayerName().equals(currentPlayerName) ? "（我）" : "";
        String host = player.isHost() ? " 房主" : "";
        StringBuilder text = new StringBuilder("玩家" + (index + 1) + "：" + player.getPlayerName() + marker + host);
        if ("finished".equals(room.getStatus()) || player.isFinished()) {
            text.append("\n得分：").append(player.getScore() == null ? "等待提交" : player.getScore());
            if (player.getPlayTime() != null) text.append("\n时间：").append(player.getPlayTime());
        } else if ("playing".equals(room.getStatus())) {
            text.append("\n状态：").append(player.isFinished() ? "已完成" : "游戏中");
        } else {
            text.append("\n状态：已就绪");
            if (player.isReadyNext()) text.append("，准备再来一局");
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
        for (BattleRoomManager.BattlePlayer player : players) {
            if (player.getScore() == null) continue;
            if (winner == null || player.getScore() > winner.getScore()) {
                winner = player;
                tie = false;
            } else if (player.getScore().equals(winner.getScore())) {
                tie = true;
            }
        }
        if (winner == null) {
            tvWinner.setVisibility(View.GONE);
            return;
        }
        tvWinner.setText(tie ? "结果：平局" : "胜者：" + winner.getPlayerName() + "（" + winner.getScore() + "分）");
        tvWinner.setVisibility(View.VISIBLE);
    }

    private boolean hasFinished(List<BattleRoomManager.BattlePlayer> players) {
        for (BattleRoomManager.BattlePlayer player : players) {
            if (player.getPlayerName().equals(currentPlayerName)) return player.isFinished();
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
                .setMessage("确定要退出当前房间吗？")
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
        tvStatus.setText("选择服务器 IP 后创建或加入房间");
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
