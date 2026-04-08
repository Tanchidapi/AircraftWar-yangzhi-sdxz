package edu.hitsz.aircraftwar.application;

import android.content.Context;

/**
 * 简单模式游戏（模板方法模式）
 */
public class EasyGame extends GameSurfaceView {

    public EasyGame(Context context, boolean soundEnabled) {
        super(context, "EASY", soundEnabled);
    }

    @Override
    protected int getEnemyMaxNumber() { return 3; }

    @Override
    protected double getEnemySpeedMultiplier() { return 0.7; }

    @Override
    protected int getHeroInitialHp() { return 1000; }

    @Override
    protected double getEliteSpawnRate() { return 0.2; }

    @Override
    protected int getHeroShootCycle() { return 1; }

    @Override
    protected int getBossScoreThreshold() { return Integer.MAX_VALUE; }

    @Override
    protected int getBossInitialHp() { return 0; }

    @Override
    protected int getBossHpIncrement() { return 0; }
}
