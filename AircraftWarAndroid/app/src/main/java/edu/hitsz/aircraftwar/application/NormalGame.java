package edu.hitsz.aircraftwar.application;

import android.content.Context;

/**
 * 普通模式游戏（模板方法模式，难度随时间递增）
 */
public class NormalGame extends GameSurfaceView {

    public NormalGame(Context context, boolean soundEnabled) {
        super(context, "NORMAL", soundEnabled);
    }

    @Override
    protected int getEnemyMaxNumber() { return 5; }

    @Override
    protected double getEnemySpeedMultiplier() {
        double baseSpeed = 1.0;
        return Math.min(baseSpeed + (getCurrentDifficultyLevel() - 1) * 0.1, 2.5);
    }

    @Override
    protected int getHeroInitialHp() { return 1500; }

    @Override
    protected double getEliteSpawnRate() {
        double baseRate = 0.3;
        return Math.min(baseRate + (getCurrentDifficultyLevel() - 1) * 0.03, 0.7);
    }

    @Override
    protected int getHeroShootCycle() { return 1; }

    @Override
    protected int getBossScoreThreshold() { return 500; }

    @Override
    protected int getBossInitialHp() { return 270; }

    @Override
    protected int getBossHpIncrement() { return 0; }
}
