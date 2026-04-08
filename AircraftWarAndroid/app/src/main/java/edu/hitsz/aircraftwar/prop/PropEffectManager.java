package edu.hitsz.aircraftwar.prop;

import edu.hitsz.aircraftwar.aircraft.HeroAircraft;
import edu.hitsz.aircraftwar.strategy.StraightShootStrategy;

/**
 * 道具效果管理器（单例模式）
 */
public class PropEffectManager {

    private static PropEffectManager instance = null;
    private HeroAircraft heroAircraft;
    private Thread currentEffectThread = null;
    private volatile long effectEndTime = 0;
    private volatile String currentEffectType = "";

    private PropEffectManager() {
    }

    public static synchronized PropEffectManager getInstance() {
        if (instance == null) {
            instance = new PropEffectManager();
        }
        return instance;
    }

    /**
     * 重置单例（游戏重新开始时调用）
     */
    public static synchronized void resetInstance() {
        if (instance != null) {
            instance.endEffect();
        }
        instance = null;
    }

    public void setHeroAircraft(HeroAircraft hero) {
        this.heroAircraft = hero;
    }

    public synchronized void activateScatterEffect(int duration) {
        if ("ring".equals(currentEffectType)) {
            return;
        }
        heroAircraft.setScatterShootStrategy();
        currentEffectType = "scatter";
        long newEndTime = System.currentTimeMillis() + duration;
        if (newEndTime > effectEndTime) {
            effectEndTime = newEndTime;
        }
        if (currentEffectThread == null || !currentEffectThread.isAlive()) {
            startEffectMonitor();
        }
    }

    public synchronized void activateRingEffect(int duration) {
        heroAircraft.setRingShootStrategy();
        currentEffectType = "ring";
        long newEndTime = System.currentTimeMillis() + duration;
        if (newEndTime > effectEndTime) {
            effectEndTime = newEndTime;
        }
        if (currentEffectThread == null || !currentEffectThread.isAlive()) {
            startEffectMonitor();
        }
    }

    private void startEffectMonitor() {
        PropEffectTask task = new PropEffectTask(this);
        currentEffectThread = new Thread(task);
        currentEffectThread.setDaemon(true);
        currentEffectThread.start();
    }

    public boolean shouldEndEffect() {
        return System.currentTimeMillis() >= effectEndTime;
    }

    public synchronized void endEffect() {
        if (heroAircraft != null) {
            heroAircraft.resetToStraightShoot();
            currentEffectType = "";
            effectEndTime = 0;
        }
    }
}
