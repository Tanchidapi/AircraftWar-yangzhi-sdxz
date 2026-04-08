package edu.hitsz.aircraftwar.application;

import android.content.Context;

/**
 * 困难模式游戏（模板方法模式，难度随时间快速递增）
 */
public class HardGame extends GameSurfaceView {

    public HardGame(Context context, boolean soundEnabled) {
        super(context, "HARD", soundEnabled);
    }

    @Override
    protected int getEnemyMaxNumber() { return 8; }

    @Override
    protected double getEnemySpeedMultiplier() {
        double baseSpeed = 1.3;
        return Math.min(baseSpeed + (getCurrentDifficultyLevel() - 1) * 0.15, 3.5);
    }

    @Override
    protected int getHeroInitialHp() { return 1000; }

    @Override
    protected double getEliteSpawnRate() {
        double baseRate = 0.5;
        return Math.min(baseRate + (getCurrentDifficultyLevel() - 1) * 0.05, 0.9);
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
