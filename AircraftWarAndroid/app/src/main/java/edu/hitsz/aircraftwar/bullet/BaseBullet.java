package edu.hitsz.aircraftwar.bullet;

import edu.hitsz.aircraftwar.application.GameConfig;
import edu.hitsz.aircraftwar.basic.AbstractFlyingObject;

/**
 * 子弹基类
 */
public abstract class BaseBullet extends AbstractFlyingObject {

    private int power = 10;

    public BaseBullet(int locationX, int locationY, int speedX, int speedY, int power) {
        super(locationX, locationY, speedX, speedY);
        this.power = power;
    }

    @Override
    public void forward() {
        super.forward();
        if (locationX <= 0 || locationX >= GameConfig.WINDOW_WIDTH) {
            vanish();
        }
        if (speedY > 0 && locationY >= GameConfig.WINDOW_HEIGHT) {
            vanish();
        } else if (locationY <= 0) {
            vanish();
        }
    }

    public int getPower() {
        return power;
    }
}
