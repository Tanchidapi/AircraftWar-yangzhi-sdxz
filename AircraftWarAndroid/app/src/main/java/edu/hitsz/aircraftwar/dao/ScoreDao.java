package edu.hitsz.aircraftwar.dao;

import java.util.List;

/**
 * 分数数据访问接口
 */
public interface ScoreDao {
    void doAdd(Score score);
    List<Score> getAllScores();
    List<Score> getScoresByDifficulty(String difficulty);
    void deleteScore(long id);
}
