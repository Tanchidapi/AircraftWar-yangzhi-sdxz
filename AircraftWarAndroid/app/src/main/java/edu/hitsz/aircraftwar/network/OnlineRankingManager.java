package edu.hitsz.aircraftwar.network;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import edu.hitsz.aircraftwar.dao.Score;
import okhttp3.*;

/**
 * 在线排行榜网络管理器（使用OkHttp）
 * 功能一：在线排行榜（HTTP/OkHttp 实现）
 * 当服务器不可用时，自动回退到模拟在线排行榜（使用本地数据模拟）
 */
public class OnlineRankingManager {

    private static final String TAG = "OnlineRanking";
    // 服务器API地址（可替换为实际服务器地址）
    private static final String BASE_URL = "https://aircraftwar-api.example.com/api";
    private static final String SUBMIT_SCORE_URL = BASE_URL + "/scores";
    private static final String GET_RANKING_URL = BASE_URL + "/ranking";

    private static OnlineRankingManager instance;
    private OkHttpClient client;
    private Gson gson;
    private Handler mainHandler;

    // 模拟在线排行榜数据（当服务器不可用时使用）
    private final List<Score> simulatedOnlineScores = new ArrayList<>();

    private OnlineRankingManager() {
        client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .build();
        gson = new Gson();
        mainHandler = new Handler(Looper.getMainLooper());
        initSimulatedData();
    }

    /**
     * 初始化模拟的在线排行榜数据
     */
    private void initSimulatedData() {
        simulatedOnlineScores.add(new Score("AcePlayer", 2580, "2026-04-01 10:30:00", "EASY"));
        simulatedOnlineScores.add(new Score("SkyKing", 1920, "2026-04-02 14:20:00", "EASY"));
        simulatedOnlineScores.add(new Score("StarPilot", 1650, "2026-04-03 09:15:00", "EASY"));
        simulatedOnlineScores.add(new Score("Phoenix", 1200, "2026-04-04 16:45:00", "EASY"));
        simulatedOnlineScores.add(new Score("Thunder", 980, "2026-04-05 11:00:00", "EASY"));

        simulatedOnlineScores.add(new Score("AcePlayer", 3200, "2026-04-01 11:00:00", "NORMAL"));
        simulatedOnlineScores.add(new Score("DragonFly", 2800, "2026-04-02 15:30:00", "NORMAL"));
        simulatedOnlineScores.add(new Score("SkyKing", 2100, "2026-04-03 10:00:00", "NORMAL"));
        simulatedOnlineScores.add(new Score("IronWing", 1750, "2026-04-04 17:20:00", "NORMAL"));
        simulatedOnlineScores.add(new Score("Blaze", 1400, "2026-04-05 12:10:00", "NORMAL"));

        simulatedOnlineScores.add(new Score("AcePlayer", 4500, "2026-04-01 12:00:00", "HARD"));
        simulatedOnlineScores.add(new Score("DragonFly", 3800, "2026-04-02 16:00:00", "HARD"));
        simulatedOnlineScores.add(new Score("Viper", 3100, "2026-04-03 11:30:00", "HARD"));
        simulatedOnlineScores.add(new Score("SkyKing", 2600, "2026-04-04 18:00:00", "HARD"));
        simulatedOnlineScores.add(new Score("StormRider", 2000, "2026-04-05 13:00:00", "HARD"));
    }

    public static synchronized OnlineRankingManager getInstance() {
        if (instance == null) {
            instance = new OnlineRankingManager();
        }
        return instance;
    }

    /**
     * 提交分数到服务器
     * 如果服务器不可用，将分数添加到模拟在线排行榜
     */
    public void submitScore(Score score, OnResultCallback<Boolean> callback) {
        String json = gson.toJson(score);
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"), json);

        Request request = new Request.Builder()
                .url(SUBMIT_SCORE_URL)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.w(TAG, "服务器不可用，分数已保存到模拟在线排行榜");
                // 服务器不可用时，添加到模拟数据
                synchronized (simulatedOnlineScores) {
                    simulatedOnlineScores.add(score);
                }
                mainHandler.post(() -> {
                    if (callback != null) callback.onResult(true);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                boolean success = response.isSuccessful();
                Log.d(TAG, "提交分数结果: " + success);
                mainHandler.post(() -> {
                    if (callback != null) callback.onResult(success);
                });
            }
        });
    }

    /**
     * 获取在线排行榜
     * 如果服务器不可用，返回模拟的在线排行榜数据
     */
    public void getRanking(String difficulty, OnResultCallback<List<Score>> callback) {
        String url = GET_RANKING_URL + "?difficulty=" + difficulty;

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.w(TAG, "服务器不可用，使用模拟在线排行榜数据");
                // 返回模拟数据
                List<Score> filtered = getSimulatedRanking(difficulty);
                mainHandler.post(() -> {
                    if (callback != null) callback.onResult(filtered);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                List<Score> scores = new ArrayList<>();
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    try {
                        Type listType = new TypeToken<List<Score>>() {}.getType();
                        scores = gson.fromJson(responseBody, listType);
                    } catch (Exception e) {
                        Log.e(TAG, "解析排行榜数据失败: " + e.getMessage());
                    }
                }
                // 如果服务器返回空数据，也使用模拟数据
                if (scores == null || scores.isEmpty()) {
                    scores = getSimulatedRanking(difficulty);
                }
                final List<Score> finalScores = scores;
                mainHandler.post(() -> {
                    if (callback != null) callback.onResult(finalScores);
                });
            }
        });
    }

    /**
     * 获取模拟的在线排行榜（按分数降序排列）
     */
    private List<Score> getSimulatedRanking(String difficulty) {
        List<Score> filtered = new ArrayList<>();
        synchronized (simulatedOnlineScores) {
            for (Score s : simulatedOnlineScores) {
                if (difficulty.equals(s.getDifficulty())) {
                    filtered.add(s);
                }
            }
        }
        Collections.sort(filtered, (a, b) -> b.getScore() - a.getScore());
        // 最多返回前10名
        if (filtered.size() > 10) {
            filtered = filtered.subList(0, 10);
        }
        return filtered;
    }

    /**
     * 回调接口
     */
    public interface OnResultCallback<T> {
        void onResult(T result);
    }
}
