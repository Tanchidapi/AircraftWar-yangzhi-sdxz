package edu.hitsz.aircraftwar.aircraft;

import edu.hitsz.aircraftwar.bullet.BaseBullet;
import edu.hitsz.aircraftwar.strategy.ShootStrategy;
import edu.hitsz.aircraftwar.strategy.StraightShootStrategy;
import edu.hitsz.aircraftwar.strategy.ScatterShootStrategy;
import edu.hitsz.aircraftwar.strategy.RingShootStrategy;

import java.util.List;

/**
 * 英雄飞机，游戏玩家操控（单例模式）
 */
public class HeroAircraft extends AbstractAircraft {

    private static HeroAircraft instance = null;
    private ShootStrategy shootStrategy = new StraightShootStrategy();

    private HeroAircraft(int locationX, int locationY, int speedX, int speedY, int hp) {
        super(locationX, locationY, speedX, speedY, hp);
    }

    public static synchronized HeroAircraft getInstance(int locationX, int locationY, int speedX, int speedY, int hp) {
        if (instance == null) {
            instance = new HeroAircraft(locationX, locationY, speedX, speedY, hp);
        }
        return instance;
    }

    /**
     * 重置单例（游戏重新开始时调用）
     */
    public static synchronized void resetInstance() {
        instance = null;
    }

    @Override
    public void forward() {
        // 英雄机由触屏控制，不通过forward函数移动
    }

    public void setShootStrategy(ShootStrategy strategy) {
        this.shootStrategy = strategy;
    }

    public void resetToStraightShoot() {
        setShootStrategy(new StraightShootStrategy());
    }

    public void setScatterShootStrategy() {
        if (!(shootStrategy instanceof RingShootStrategy)) {
            setShootStrategy(new ScatterShootStrategy());
        }
    }

    public void setRingShootStrategy() {
        setShootStrategy(new RingShootStrategy());
    }

    @Override
    public List<BaseBullet> shoot() {
        return shootStrategy.shoot(this);
    }
}
