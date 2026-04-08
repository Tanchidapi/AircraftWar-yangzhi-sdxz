package edu.hitsz.aircraftwar.aircraft;

import edu.hitsz.aircraftwar.application.GameConfig;
import edu.hitsz.aircraftwar.strategy.StraightShootStrategy;

/**
 * 精英敌机
 */
public class EliteEnemy extends AbstractAircraft {

    public EliteEnemy(int locationX, int locationY, int speedX, int speedY, int hp) {
        super(locationX, locationY, speedX, speedY, hp);
        setShootStrategy(new StraightShootStrategy());
    }

    @Override
    public void forward() {
        super.forward();
        if (locationY >= GameConfig.WINDOW_HEIGHT) {
            vanish();
        }
    }
}
