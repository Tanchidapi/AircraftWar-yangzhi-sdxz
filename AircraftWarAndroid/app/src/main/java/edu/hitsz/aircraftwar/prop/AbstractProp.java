package edu.hitsz.aircraftwar.prop;

import edu.hitsz.aircraftwar.application.GameConfig;
import edu.hitsz.aircraftwar.basic.AbstractFlyingObject;

/**
 * 道具抽象基类
 */
public abstract class AbstractProp extends AbstractFlyingObject {
    public AbstractProp(int locationX, int locationY, int speedX, int speedY) {
        super(locationX, locationY, speedX, speedY);
    }

    @Override
    public void forward() {
        locationX += speedX;
        locationY += speedY;
        if (locationY >= GameConfig.WINDOW_HEIGHT) {
            vanish();
        }
    }
}
