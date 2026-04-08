package edu.hitsz.aircraftwar.network;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import edu.hitsz.aircraftwar.dao.Score;
import okhttp3.*;

/**
 * 在线排行榜网络管理器（使用OkHttp）
 * 功能一：在线排行榜（HTTP/OkHttp 实现）
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

    private OnlineRankingManager() {
        client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build();
        gson = new Gson();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized OnlineRankingManager getInstance() {
        if (instance == null) {
            instance = new OnlineRankingManager();
        }
        return instance;
    }

    /**
     * 提交分数到服务器
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
                Log.e(TAG, "提交分数失败: " + e.getMessage());
                mainHandler.post(() -> {
                    if (callback != null) callback.onResult(false);
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
                Log.e(TAG, "获取排行榜失败: " + e.getMessage());
                mainHandler.post(() -> {
                    if (callback != null) callback.onResult(new ArrayList<>());
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
                final List<Score> finalScores = scores;
                mainHandler.post(() -> {
                    if (callback != null) callback.onResult(finalScores);
                });
            }
        });
    }

    /**
     * 回调接口
     */
    public interface OnResultCallback<T> {
        void onResult(T result);
    }
}
