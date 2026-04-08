package edu.hitsz.aircraftwar.aircraft;

import edu.hitsz.aircraftwar.application.GameConfig;
import edu.hitsz.aircraftwar.strategy.RingShootStrategy;

/**
 * Boss敌机
 */
public class BossEliteEnemy extends AbstractAircraft {

    public BossEliteEnemy(int locationX, int locationY, int speedX, int speedY, int hp) {
        super(locationX, locationY, speedX, speedY, hp);
        setShootStrategy(new RingShootStrategy());
    }

    @Override
    public void forward() {
        super.forward();
        if (locationY >= GameConfig.WINDOW_HEIGHT) {
            vanish();
        }
    }
}
