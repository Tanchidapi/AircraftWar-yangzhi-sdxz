package edu.hitsz.aircraftwar.network;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 对战房间网络管理器。
 * 房间只同步最终分数，不同步或渲染另一名玩家的实时画面。
 */
public class BattleRoomManager {

    private static final String TAG = "BattleRoom";
    private static BattleRoomManager instance;

    private final OkHttpClient client;
    private final Gson gson;
    private final Handler mainHandler;

    private BattleRoomManager() {
        client = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .build();
        gson = new Gson();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized BattleRoomManager getInstance() {
        if (instance == null) {
            instance = new BattleRoomManager();
        }
        return instance;
    }

    private String getBaseUrl() {
        return OnlineRankingManager.getInstance().getServerUrl() + "/api";
    }

    public void submitBattleScore(String roomId, BattleScore score,
                                  OnlineRankingManager.OnResultCallback<Boolean> callback) {
        String json = gson.toJson(score);
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"), json);
        Request request = new Request.Builder()
                .url(getBaseUrl() + "/battle/rooms/" + roomId + "/scores")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "提交对战分数失败: " + e.getMessage());
                mainHandler.post(() -> {
                    if (callback != null) callback.onResult(false);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                boolean success = response.isSuccessful();
                if (response.body() != null) response.body().close();
                mainHandler.post(() -> {
                    if (callback != null) callback.onResult(success);
                });
            }
        });
    }

    public void getBattleRoom(String roomId,
                              OnlineRankingManager.OnResultCallback<BattleRoom> callback) {
        Request request = new Request.Builder()
                .url(getBaseUrl() + "/battle/rooms/" + roomId)
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "获取对战房间失败: " + e.getMessage());
                mainHandler.post(() -> {
                    if (callback != null) callback.onResult(null);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                BattleRoom room = null;
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        room = gson.fromJson(response.body().string(), BattleRoom.class);
                    } catch (Exception e) {
                        Log.e(TAG, "解析对战房间失败: " + e.getMessage());
                    }
                }
                BattleRoom finalRoom = room;
                mainHandler.post(() -> {
                    if (callback != null) callback.onResult(finalRoom);
                });
            }
        });
    }

    public static class BattleScore {
        private String playerName;
        private int score;
        private String playTime;
        private String difficulty;

        public BattleScore(String playerName, int score, String playTime, String difficulty) {
            this.playerName = playerName;
            this.score = score;
            this.playTime = playTime;
            this.difficulty = difficulty;
        }

        public String getPlayerName() { return playerName; }
        public int getScore() { return score; }
        public String getPlayTime() { return playTime; }
        public String getDifficulty() { return difficulty; }
    }

    public static class BattleRoom {
        private String roomId;
        private boolean allFinished;
        private List<BattleScore> players;

        public String getRoomId() { return roomId; }
        public boolean isAllFinished() { return allFinished; }
        public List<BattleScore> getPlayers() {
            return players == null ? new ArrayList<>() : players;
        }
    }
}
