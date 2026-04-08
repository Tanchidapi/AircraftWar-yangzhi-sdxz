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
 * 连接本地 Flask 排行榜服务器
 *
 * 服务器地址说明：
 *   - Android 模拟器访问宿主机: 10.0.2.2
 *   - 真机同一局域网: 使用电脑的局域网IP（如 192.168.x.x）
 *   - 服务器端口: 5000
 */
public class OnlineRankingManager {

    private static final String TAG = "OnlineRanking";

    /**
     * 服务器地址配置
     * 模拟器使用 10.0.2.2 访问宿主机的 localhost
     * 如果使用真机测试，请改为电脑的局域网IP地址
     */
    private static String serverHost = "10.0.2.2";
    private static int serverPort = 5000;

    private static OnlineRankingManager instance;
    private OkHttpClient client;
    private Gson gson;
    private Handler mainHandler;

    private OnlineRankingManager() {
        client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
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
     * 设置服务器地址（可在设置中动态修改）
     * @param host 服务器IP地址
     * @param port 服务器端口
     */
    public static void setServerAddress(String host, int port) {
        serverHost = host;
        serverPort = port;
        // 重置实例以使用新地址
        instance = null;
    }

    private String getBaseUrl() {
        return "http://" + serverHost + ":" + serverPort + "/api";
    }

    /**
     * 健康检查 - 测试服务器是否可用
     */
    public void checkServerHealth(OnResultCallback<Boolean> callback) {
        Request request = new Request.Builder()
                .url(getBaseUrl() + "/health")
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.w(TAG, "服务器不可用: " + e.getMessage());
                mainHandler.post(() -> {
                    if (callback != null) callback.onResult(false);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                boolean healthy = response.isSuccessful();
                Log.d(TAG, "服务器健康检查: " + (healthy ? "正常" : "异常"));
                mainHandler.post(() -> {
                    if (callback != null) callback.onResult(healthy);
                });
            }
        });
    }

    /**
     * 提交分数到服务器
     */
    public void submitScore(Score score, OnResultCallback<Boolean> callback) {
        String json = gson.toJson(score);
        Log.d(TAG, "提交分数: " + json);

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"), json);

        Request request = new Request.Builder()
                .url(getBaseUrl() + "/scores")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "提交分数失败（网络错误）: " + e.getMessage());
                mainHandler.post(() -> {
                    if (callback != null) callback.onResult(false);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                boolean success = response.isSuccessful();
                String responseBody = response.body() != null ? response.body().string() : "";
                Log.d(TAG, "提交分数结果: " + success + " - " + responseBody);
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
        String url = getBaseUrl() + "/ranking?difficulty=" + difficulty;
        Log.d(TAG, "请求排行榜: " + url);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "获取排行榜失败（网络错误）: " + e.getMessage());
                mainHandler.post(() -> {
                    if (callback != null) callback.onResult(new ArrayList<>());
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                List<Score> scores = new ArrayList<>();
                if (response.isSuccessful() && response.body() != null) {
                    String responseBody = response.body().string();
                    Log.d(TAG, "排行榜响应: " + responseBody);
                    try {
                        Type listType = new TypeToken<List<Score>>() {}.getType();
                        scores = gson.fromJson(responseBody, listType);
                        if (scores == null) scores = new ArrayList<>();
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
     * 获取当前服务器地址（用于显示）
     */
    public String getServerUrl() {
        return "http://" + serverHost + ":" + serverPort;
    }

    /**
     * 回调接口
     */
    public interface OnResultCallback<T> {
        void onResult(T result);
    }
}
