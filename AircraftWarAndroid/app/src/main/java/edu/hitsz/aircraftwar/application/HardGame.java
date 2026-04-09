package edu.hitsz.aircraftwar.application;

import android.content.Context;

/**
 * 困难模式游戏（模板方法模式，难度随时间递增但有合理上限）
 */
public class HardGame extends GameSurfaceView {

    public HardGame(Context context, boolean soundEnabled) {
        super(context, "HARD", soundEnabled);
    }

    @Override
    protected int getEnemyMaxNumber() { return 7; }

    @Override
    protected double getEnemySpeedMultiplier() {
        // 基础速度从1.0开始，每30秒增加0.08，上限2.0
        // 相比之前的1.3起步、0.15增速、3.5上限，大幅降低
        double baseSpeed = 1.0;
        return Math.min(baseSpeed + (getCurrentDifficultyLevel() - 1) * 0.08, 2.0);
    }

    @Override
    protected int getHeroInitialHp() { return 1000; }

    @Override
    protected double getEliteSpawnRate() {
        double baseRate = 0.45;
        return Math.min(baseRate + (getCurrentDifficultyLevel() - 1) * 0.04, 0.85);
    }

    @Override
    protected int getHeroShootCycle() { return 1; }

    @Override
    protected int getBossScoreThreshold() { return 400; }

    @Override
    protected int getBossInitialHp() { return 270; }

    @Override
    protected int getBossHpIncrement() { return 60; }
}
