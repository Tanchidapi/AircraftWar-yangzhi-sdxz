package edu.hitsz.aircraftwar.factory;

import edu.hitsz.aircraftwar.prop.AbstractProp;
import edu.hitsz.aircraftwar.prop.BulletProp;

public class BulletPropFactory implements PropFactory {
    public AbstractProp createProp(int locationX, int locationY, int speedX, int speedY) {
        return new BulletProp(locationX, locationY, speedX, speedY);
    }
}
