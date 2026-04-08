package edu.hitsz.aircraftwar.factory;

import edu.hitsz.aircraftwar.prop.AbstractProp;
import edu.hitsz.aircraftwar.prop.SuperBulletProp;

public class SuperBulletPropFactory implements PropFactory {
    public AbstractProp createProp(int locationX, int locationY, int speedX, int speedY) {
        return new SuperBulletProp(locationX, locationY, speedX, speedY);
    }
}
