package edu.hitsz.aircraftwar.aircraft;

import edu.hitsz.aircraftwar.application.GameConfig;
import edu.hitsz.aircraftwar.strategy.ScatterShootStrategy;

/**
 * 超级精英敌机
 */
public class SuperEliteEnemy extends AbstractAircraft {

    public SuperEliteEnemy(int locationX, int locationY, int speedX, int speedY, int hp) {
        super(locationX, locationY, speedX, speedY, hp);
        setShootStrategy(new ScatterShootStrategy());
    }

    @Override
    public void forward() {
        super.forward();
        if (locationY >= GameConfig.WINDOW_HEIGHT) {
            vanish();
        }
    }
}
