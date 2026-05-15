package edu.hitsz.aircraftwar.network;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

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

    // 最近一次请求的错误信息，供UI层读取
    private volatile String lastError;

    private BattleRoomManager() {
        client = new OkHttpClient.Builder()
                .connectTimeout(8, TimeUnit.SECONDS)
                .readTimeout(8, TimeUnit.SECONDS)
                .writeTimeout(8, TimeUnit.SECONDS)
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

    public String getLastError() {
        return lastError;
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
        String url = getBaseUrl() + "/battle/rooms";
        Log.d(TAG, "创建房间请求: " + url + " data=" + gson.toJson(data));
        post(url, data, callback);
    }

    /**
     * 加入已有房间时同步携带难度，避免服务端把同房间号的加入请求误判成新房间。
     */
    public void joinRoom(String roomId, String playerName, String difficulty,
                         OnlineRankingManager.OnResultCallback<BattleRoom> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("playerName", playerName);
        data.put("difficulty", difficulty);
        String url = getBaseUrl() + "/battle/rooms/" + roomId + "/join";
        Log.d(TAG, "加入房间请求: " + url + " data=" + gson.toJson(data));
        post(url, data, callback);
    }

    public void readyInRoom(String roomId, String playerName,
                            OnlineRankingManager.OnResultCallback<BattleRoom> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("playerName", playerName);
        String url = getBaseUrl() + "/battle/rooms/" + roomId + "/ready";
        Log.d(TAG, "准备请求: " + url);
        post(url, data, callback);
    }

    public void startRoom(String roomId, String playerName,
                          OnlineRankingManager.OnResultCallback<BattleRoom> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("playerName", playerName);
        String url = getBaseUrl() + "/battle/rooms/" + roomId + "/start";
        post(url, data, callback);
    }

    public void submitBattleScore(String roomId, BattleScore score,
                                  OnlineRankingManager.OnResultCallback<BattleRoom> callback) {
        String url = getBaseUrl() + "/battle/rooms/" + roomId + "/scores";
        post(url, score, callback);
    }

    public void readyAgain(String roomId, String playerName,
                           OnlineRankingManager.OnResultCallback<BattleRoom> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("playerName", playerName);
        String url = getBaseUrl() + "/battle/rooms/" + roomId + "/again";
        post(url, data, callback);
    }

    public void leaveRoom(String roomId, String playerName,
                          OnlineRankingManager.OnResultCallback<Boolean> callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("playerName", playerName);
        String url = getBaseUrl() + "/battle/rooms/" + roomId + "/leave";
        postRaw(url, data, callback);
    }

    public void getBattleRoom(String roomId,
                              OnlineRankingManager.OnResultCallback<BattleRoom> callback) {
        Request request = new Request.Builder()
                .url(getBaseUrl() + "/battle/rooms/" + roomId)
                .get()
                .build();
        requestRoom(request, callback);
    }

    private void post(String url, Object data,
                      OnlineRankingManager.OnResultCallback<BattleRoom> callback) {
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"), gson.toJson(data));
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        requestRoom(request, callback);
    }

    private void postRaw(String url, Object data,
                         OnlineRankingManager.OnResultCallback<Boolean> callback) {
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"), gson.toJson(data));
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "请求失败: " + e.getMessage());
                lastError = "网络连接失败: " + e.getMessage();
                mainHandler.post(() -> {
                    if (callback != null) callback.onResult(false);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                boolean success = response.isSuccessful();
                if (!success && response.body() != null) {
                    String respBody = response.body().string();
                    lastError = parseErrorMessage(respBody, response.code());
                    Log.e(TAG, "请求异常: " + response.code() + " - " + respBody);
                } else {
                    if (response.body() != null) response.body().close();
                    lastError = null;
                }
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
                Log.e(TAG, "房间请求失败: " + e.getMessage() + " url=" + request.url());
                lastError = "网络连接失败: " + e.getMessage();
                mainHandler.post(() -> {
                    if (callback != null) callback.onResult(null);
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                BattleRoom room = null;
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String respBody = response.body().string();
                        Log.d(TAG, "房间响应成功: " + respBody);
                        room = gson.fromJson(respBody, BattleRoom.class);
                        lastError = null;
                    } catch (Exception e) {
                        Log.e(TAG, "解析房间失败: " + e.getMessage());
                        lastError = "解析服务器响应失败";
                    }
                } else if (response.body() != null) {
                    String respBody = response.body().string();
                    lastError = parseErrorMessage(respBody, response.code());
                    Log.e(TAG, "房间请求异常: " + response.code() + " - " + respBody);
                } else {
                    lastError = "服务器返回错误: " + response.code();
                }
                BattleRoom finalRoom = room;
                mainHandler.post(() -> {
                    if (callback != null) callback.onResult(finalRoom);
                });
            }
        });
    }

    /**
     * 从服务器JSON错误响应中提取错误信息
     */
    private String parseErrorMessage(String responseBody, int code) {
        try {
            JsonObject json = JsonParser.parseString(responseBody).getAsJsonObject();
            if (json.has("error")) {
                return json.get("error").getAsString();
            }
        } catch (Exception ignored) {
        }
        return "服务器错误 (" + code + ")";
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
        private boolean ready;
        private boolean readyNext;
        private boolean finished;
        private Integer score;
        private String playTime;
        private String difficulty;

        public String getPlayerName() { return playerName; }
        public boolean isHost() { return isHost; }
        public boolean isReady() { return ready; }
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
        private boolean allReady;
        private List<BattlePlayer> players;

        public String getRoomId() { return roomId; }
        public String getHostName() { return hostName; }
        public String getDifficulty() { return difficulty; }
        public String getStatus() { return status; }
        public int getRoundNo() { return roundNo; }
        public boolean isAllFinished() { return allFinished; }
        public boolean isAllReady() { return allReady; }
        public List<BattlePlayer> getPlayers() {
            return players == null ? new ArrayList<>() : players;
        }
    }
}
