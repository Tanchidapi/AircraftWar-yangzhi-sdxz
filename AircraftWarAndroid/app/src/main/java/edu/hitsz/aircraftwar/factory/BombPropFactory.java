package edu.hitsz.aircraftwar.factory;

import edu.hitsz.aircraftwar.prop.AbstractProp;
import edu.hitsz.aircraftwar.prop.BombProp;

public class BombPropFactory implements PropFactory {
    public AbstractProp createProp(int locationX, int locationY, int speedX, int speedY) {
        return new BombProp(locationX, locationY, speedX, speedY);
    }
}
