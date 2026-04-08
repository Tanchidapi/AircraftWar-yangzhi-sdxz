package edu.hitsz.aircraftwar.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import edu.hitsz.aircraftwar.R;
import edu.hitsz.aircraftwar.dao.Score;
import edu.hitsz.aircraftwar.dao.ScoreDao;
import edu.hitsz.aircraftwar.dao.ScoreDaoImpl;
import edu.hitsz.aircraftwar.network.OnlineRankingManager;

/**
 * 排行榜Activity
 */
public class RankingActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private Spinner spinnerDifficulty;
    private Button btnDelete, btnBack, btnOnline;
    private TextView tvTitle;
    private TextView tvServerStatus;

    private ScoreDao scoreDao;
    private List<Score> scoreList = new ArrayList<>();
    private ScoreAdapter adapter;
    private String currentDifficulty = "EASY";
    private int selectedPosition = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ranking);

        scoreDao = new ScoreDaoImpl(this);
        currentDifficulty = getIntent().getStringExtra("difficulty");
        if (currentDifficulty == null) currentDifficulty = "EASY";

        tvTitle = findViewById(R.id.tv_ranking_title);
        tvServerStatus = findViewById(R.id.tv_server_status);
        recyclerView = findViewById(R.id.recycler_ranking);
        spinnerDifficulty = findViewById(R.id.spinner_difficulty);
        btnDelete = findViewById(R.id.btn_delete);
        btnBack = findViewById(R.id.btn_back);
        btnOnline = findViewById(R.id.btn_online);

        // 难度选择
        ArrayAdapter<String> diffAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"简单", "普通", "困难"});
        diffAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDifficulty.setAdapter(diffAdapter);

        // 设置初始选中
        switch (currentDifficulty) {
            case "NORMAL": spinnerDifficulty.setSelection(1); break;
            case "HARD": spinnerDifficulty.setSelection(2); break;
            default: spinnerDifficulty.setSelection(0); break;
        }

        spinnerDifficulty.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case 0: currentDifficulty = "EASY"; break;
                    case 1: currentDifficulty = "NORMAL"; break;
                    case 2: currentDifficulty = "HARD"; break;
                }
                loadScores();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ScoreAdapter();
        recyclerView.setAdapter(adapter);

        // 删除按钮
        btnDelete.setOnClickListener(v -> deleteSelectedScore());

        // 返回按钮
        btnBack.setOnClickListener(v -> finish());

        // 在线排行榜按钮
        btnOnline.setOnClickListener(v -> loadOnlineRanking());

        // 长按在线排行榜按钮 -> 配置服务器地址
        btnOnline.setOnLongClickListener(v -> {
            showServerConfigDialog();
            return true;
        });

        loadScores();
        checkServerStatus();
    }

    /**
     * 检查服务器连接状态
     */
    private void checkServerStatus() {
        if (tvServerStatus == null) return;
        tvServerStatus.setText("🔄 检测服务器...");
        tvServerStatus.setTextColor(0xFFAAAAAA);

        OnlineRankingManager.getInstance().checkServerHealth(healthy -> {
            if (healthy) {
                tvServerStatus.setText("🟢 服务器已连接 (" +
                        OnlineRankingManager.getInstance().getServerUrl() + ")");
                tvServerStatus.setTextColor(0xFF4CAF50);
            } else {
                tvServerStatus.setText("🔴 服务器未连接（长按「在线排行」配置地址）");
                tvServerStatus.setTextColor(0xFFe94560);
            }
        });
    }

    /**
     * 显示服务器地址配置对话框
     */
    private void showServerConfigDialog() {
        EditText inputHost = new EditText(this);
        inputHost.setHint("服务器IP（如 10.0.2.2 或 192.168.x.x）");
        inputHost.setText("10.0.2.2");

        EditText inputPort = new EditText(this);
        inputPort.setHint("端口号（默认 5000）");
        inputPort.setText("5000");
        inputPort.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(50, 30, 50, 10);

        TextView tipText = new TextView(this);
        tipText.setText("提示：\n• Android模拟器访问宿主机请用 10.0.2.2\n• 真机请使用电脑的局域网IP");
        tipText.setTextSize(13);
        tipText.setPadding(0, 0, 0, 20);
        layout.addView(tipText);

        TextView hostLabel = new TextView(this);
        hostLabel.setText("服务器地址：");
        layout.addView(hostLabel);
        layout.addView(inputHost);

        TextView portLabel = new TextView(this);
        portLabel.setText("端口号：");
        portLabel.setPadding(0, 20, 0, 0);
        layout.addView(portLabel);
        layout.addView(inputPort);

        new AlertDialog.Builder(this)
                .setTitle("配置排行榜服务器")
                .setView(layout)
                .setPositiveButton("连接", (dialog, which) -> {
                    String host = inputHost.getText().toString().trim();
                    int port;
                    try {
                        port = Integer.parseInt(inputPort.getText().toString().trim());
                    } catch (NumberFormatException e) {
                        port = 5000;
                    }
                    OnlineRankingManager.setServerAddress(host, port);
                    Toast.makeText(this, "已更新服务器地址: " + host + ":" + port, Toast.LENGTH_SHORT).show();
                    checkServerStatus();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void loadScores() {
        selectedPosition = -1;
        scoreList.clear();
        scoreList.addAll(scoreDao.getScoresByDifficulty(currentDifficulty));
        adapter.notifyDataSetChanged();
        tvTitle.setText("排行榜 - " + getDifficultyName(currentDifficulty));
    }

    private void loadOnlineRanking() {
        Toast.makeText(this, "正在获取在线排行榜...", Toast.LENGTH_SHORT).show();
        OnlineRankingManager.getInstance().getRanking(currentDifficulty, scores -> {
            if (scores != null && !scores.isEmpty()) {
                scoreList.clear();
                scoreList.addAll(scores);
                adapter.notifyDataSetChanged();
                tvTitle.setText("🌐 在线排行榜 - " + getDifficultyName(currentDifficulty));
                Toast.makeText(this, "已加载在线排行榜（" + scores.size() + "条记录）",
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "服务器无数据或连接失败，显示本地排行", Toast.LENGTH_SHORT).show();
                loadScores();
            }
        });
    }

    private String getDifficultyName(String difficulty) {
        switch (difficulty) {
            case "NORMAL": return "普通";
            case "HARD": return "困难";
            default: return "简单";
        }
    }

    private void deleteSelectedScore() {
        if (selectedPosition < 0 || selectedPosition >= scoreList.size()) {
            Toast.makeText(this, "请先选择要删除的记录", Toast.LENGTH_SHORT).show();
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("确认删除")
                .setMessage("是否确定删除该记录？")
                .setPositiveButton("确定", (dialog, which) -> {
                    Score score = scoreList.get(selectedPosition);
                    scoreDao.deleteScore(score.getId());
                    loadScores();
                    Toast.makeText(this, "删除成功", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    // ==================== RecyclerView Adapter ====================

    class ScoreAdapter extends RecyclerView.Adapter<ScoreAdapter.ViewHolder> {

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_score, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            Score score = scoreList.get(position);
            holder.tvRank.setText(String.valueOf(position + 1));
            holder.tvName.setText(score.getName());
            holder.tvScore.setText(String.valueOf(score.getScore()));
            holder.tvTime.setText(score.getPlayTime());

            // 选中高亮
            holder.itemView.setBackgroundColor(
                    position == selectedPosition ? 0x330000FF : 0x00000000);

            holder.itemView.setOnClickListener(v -> {
                int oldPos = selectedPosition;
                selectedPosition = holder.getAdapterPosition();
                if (oldPos >= 0) notifyItemChanged(oldPos);
                notifyItemChanged(selectedPosition);
            });
        }

        @Override
        public int getItemCount() {
            return scoreList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView tvRank, tvName, tvScore, tvTime;

            ViewHolder(View itemView) {
                super(itemView);
                tvRank = itemView.findViewById(R.id.tv_rank);
                tvName = itemView.findViewById(R.id.tv_name);
                tvScore = itemView.findViewById(R.id.tv_score);
                tvTime = itemView.findViewById(R.id.tv_time);
            }
        }
    }
}
