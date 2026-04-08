package edu.hitsz.aircraftwar.aircraft;

import edu.hitsz.aircraftwar.application.GameConfig;

/**
 * 普通敌机，不可射击
 */
public class MobEnemy extends AbstractAircraft {

    public MobEnemy(int locationX, int locationY, int speedX, int speedY, int hp) {
        super(locationX, locationY, speedX, speedY, hp);
    }

    @Override
    public void forward() {
        super.forward();
        if (locationY >= GameConfig.WINDOW_HEIGHT) {
            vanish();
        }
    }
}
