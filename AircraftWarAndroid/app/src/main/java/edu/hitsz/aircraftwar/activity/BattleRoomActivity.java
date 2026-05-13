package edu.hitsz.aircraftwar.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

import edu.hitsz.aircraftwar.R;
import edu.hitsz.aircraftwar.network.BattleRoomManager;

/**
 * 对战房间结果页。只展示双方最终得分，不展示实时游戏画面。
 */
public class BattleRoomActivity extends AppCompatActivity {

    private String roomId;
    private String currentPlayerName;
    private TextView tvTitle;
    private TextView tvStatus;
    private TextView tvPlayerOne;
    private TextView tvPlayerTwo;
    private TextView tvWinner;
    private Button btnRefresh;
    private Button btnBack;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_battle_room);

        roomId = getIntent().getStringExtra("battleRoomId");
        currentPlayerName = getIntent().getStringExtra("battlePlayerName");
        if (roomId == null || roomId.trim().isEmpty()) roomId = "room1001";
        if (currentPlayerName == null) currentPlayerName = "Player";

        tvTitle = findViewById(R.id.tv_battle_title);
        tvStatus = findViewById(R.id.tv_battle_status);
        tvPlayerOne = findViewById(R.id.tv_player_one);
        tvPlayerTwo = findViewById(R.id.tv_player_two);
        tvWinner = findViewById(R.id.tv_winner);
        btnRefresh = findViewById(R.id.btn_refresh_room);
        btnBack = findViewById(R.id.btn_back_menu);

        tvTitle.setText("对战房间 " + roomId);
        btnRefresh.setOnClickListener(v -> loadRoom());
        btnBack.setOnClickListener(v -> finish());

        loadRoom();
    }

    private void loadRoom() {
        tvStatus.setText("正在获取房间结果...");
        btnRefresh.setEnabled(false);

        BattleRoomManager.getInstance().getBattleRoom(roomId, room -> {
            btnRefresh.setEnabled(true);
            if (room == null) {
                tvStatus.setText("房间结果获取失败，请检查服务器连接后刷新");
                Toast.makeText(this, "获取房间失败", Toast.LENGTH_SHORT).show();
                return;
            }
            bindRoom(room);
        });
    }

    private void bindRoom(BattleRoomManager.BattleRoom room) {
        List<BattleRoomManager.BattleScore> players = room.getPlayers();
        tvStatus.setText(room.isAllFinished()
                ? "双方已结束，对战结果如下"
                : "等待另一位玩家结束游戏，可点击刷新查看");

        tvPlayerOne.setText(formatPlayer(players, 0));
        tvPlayerTwo.setText(formatPlayer(players, 1));

        if (players.size() >= 2) {
            BattleRoomManager.BattleScore first = players.get(0);
            BattleRoomManager.BattleScore second = players.get(1);
            if (first.getScore() == second.getScore()) {
                tvWinner.setText("结果：平局");
            } else {
                tvWinner.setText("胜者：" + first.getPlayerName() + "（" + first.getScore() + "分）");
            }
            tvWinner.setVisibility(View.VISIBLE);
        } else {
            tvWinner.setVisibility(View.GONE);
        }
    }

    private String formatPlayer(List<BattleRoomManager.BattleScore> players, int index) {
        if (players.size() <= index) {
            return "玩家" + (index + 1) + "：等待提交分数";
        }
        BattleRoomManager.BattleScore player = players.get(index);
        String marker = player.getPlayerName().equals(currentPlayerName) ? "（我）" : "";
        return "玩家" + (index + 1) + "：" + player.getPlayerName() + marker
                + "\n得分：" + player.getScore()
                + "\n时间：" + player.getPlayTime();
    }
}
