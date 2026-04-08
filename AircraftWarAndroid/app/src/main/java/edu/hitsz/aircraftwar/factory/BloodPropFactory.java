package edu.hitsz.aircraftwar.factory;

import edu.hitsz.aircraftwar.prop.AbstractProp;
import edu.hitsz.aircraftwar.prop.BloodProp;

public class BloodPropFactory implements PropFactory {
    public AbstractProp createProp(int locationX, int locationY, int speedX, int speedY) {
        return new BloodProp(locationX, locationY, speedX, speedY);
    }
}
