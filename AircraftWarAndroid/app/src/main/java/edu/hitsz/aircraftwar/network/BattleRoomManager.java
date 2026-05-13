package edu.hitsz.aircraftwar.network;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 对战房间网络管理器。房间同步状态和最终分数，不同步另一名玩家的实时画面。
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

    public void createRoom(String roomId, String playerName, String difficulty,
                           OnlineRankingManager.OnResultCallback<BattleRoom> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("roomId", roomId);
        data.put("playerName", playerName);
        data.put("difficulty", difficulty);
        post("/battle/rooms", data, callback);
    }

    public void joinRoom(String roomId, String playerName,
                         OnlineRankingManager.OnResultCallback<BattleRoom> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("playerName", playerName);
        post("/battle/rooms/" + roomId + "/join", data, callback);
    }

    public void startRoom(String roomId, String playerName,
                          OnlineRankingManager.OnResultCallback<BattleRoom> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("playerName", playerName);
        post("/battle/rooms/" + roomId + "/start", data, callback);
    }

    public void submitBattleScore(String roomId, BattleScore score,
                                  OnlineRankingManager.OnResultCallback<BattleRoom> callback) {
        post("/battle/rooms/" + roomId + "/scores", score, callback);
    }

    public void readyAgain(String roomId, String playerName,
                           OnlineRankingManager.OnResultCallback<BattleRoom> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("playerName", playerName);
        post("/battle/rooms/" + roomId + "/again", data, callback);
    }

    public void leaveRoom(String roomId, String playerName,
                          OnlineRankingManager.OnResultCallback<Boolean> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("playerName", playerName);
        postRaw("/battle/rooms/" + roomId + "/leave", data, callback);
    }

    public void getBattleRoom(String roomId,
                              OnlineRankingManager.OnResultCallback<BattleRoom> callback) {
        Request request = new Request.Builder()
                .url(getBaseUrl() + "/battle/rooms/" + roomId)
                .get()
                .build();
        requestRoom(request, callback);
    }

    private void post(String path, Object data,
                      OnlineRankingManager.OnResultCallback<BattleRoom> callback) {
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"), gson.toJson(data));
        Request request = new Request.Builder()
                .url(getBaseUrl() + path)
                .post(body)
                .build();
        requestRoom(request, callback);
    }

    private void postRaw(String path, Object data,
                         OnlineRankingManager.OnResultCallback<Boolean> callback) {
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"), gson.toJson(data));
        Request request = new Request.Builder()
                .url(getBaseUrl() + path)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "请求失败: " + e.getMessage());
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

    private void requestRoom(Request request,
                             OnlineRankingManager.OnResultCallback<BattleRoom> callback) {
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "房间请求失败: " + e.getMessage());
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
                        Log.e(TAG, "解析房间失败: " + e.getMessage());
                    }
                } else if (response.body() != null) {
                    Log.e(TAG, "房间请求异常: " + response.code() + " - " + response.body().string());
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
        private int roundNo;

        public BattleScore(String playerName, int score, String playTime, String difficulty, int roundNo) {
            this.playerName = playerName;
            this.score = score;
            this.playTime = playTime;
            this.difficulty = difficulty;
            this.roundNo = roundNo;
        }

        public String getPlayerName() { return playerName; }
        public int getScore() { return score; }
        public String getPlayTime() { return playTime; }
        public String getDifficulty() { return difficulty; }
        public int getRoundNo() { return roundNo; }
    }

    public static class BattlePlayer {
        private String playerName;
        private boolean isHost;
        private boolean readyNext;
        private boolean finished;
        private Integer score;
        private String playTime;
        private String difficulty;

        public String getPlayerName() { return playerName; }
        public boolean isHost() { return isHost; }
        public boolean isReadyNext() { return readyNext; }
        public boolean isFinished() { return finished; }
        public Integer getScore() { return score; }
        public String getPlayTime() { return playTime; }
        public String getDifficulty() { return difficulty; }
    }

    public static class BattleRoom {
        private String roomId;
        private String hostName;
        private String difficulty;
        private String status;
        private int roundNo;
        private boolean allFinished;
        private List<BattlePlayer> players;

        public String getRoomId() { return roomId; }
        public String getHostName() { return hostName; }
        public String getDifficulty() { return difficulty; }
        public String getStatus() { return status; }
        public int getRoundNo() { return roundNo; }
        public boolean isAllFinished() { return allFinished; }
        public List<BattlePlayer> getPlayers() {
            return players == null ? new ArrayList<>() : players;
        }
    }
}
