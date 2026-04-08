package edu.hitsz.aircraftwar.activity;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
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

        loadScores();
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
                Toast.makeText(this, "已加载在线排行榜", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "暂无在线数据，显示本地排行", Toast.LENGTH_SHORT).show();
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
