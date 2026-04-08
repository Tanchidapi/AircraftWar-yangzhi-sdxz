package edu.hitsz.aircraftwar.dao;

/**
 * 分数实体类
 */
public class Score {
    private long id;
    private String name;
    private int score;
    private String playTime;
    private String difficulty;

    public Score(String name, int score, String playTime) {
        this(name, score, playTime, "EASY");
    }

    public Score(String name, int score, String playTime, String difficulty) {
        this.name = name;
        this.score = score;
        this.playTime = playTime;
        this.difficulty = difficulty;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getScore() { return score; }
    public void setScore(int score) { this.score = score; }
    public String getPlayTime() { return playTime; }
    public void setPlayTime(String playTime) { this.playTime = playTime; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
}
