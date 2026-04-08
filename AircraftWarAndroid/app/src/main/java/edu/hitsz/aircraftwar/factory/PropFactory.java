package edu.hitsz.aircraftwar.factory;

import edu.hitsz.aircraftwar.prop.AbstractProp;

/**
 * 道具工厂接口（工厂模式）
 */
public interface PropFactory {
    AbstractProp createProp(int locationX, int locationY, int speedX, int speedY);
}
