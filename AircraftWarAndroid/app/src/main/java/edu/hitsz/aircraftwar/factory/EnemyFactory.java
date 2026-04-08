package edu.hitsz.aircraftwar.factory;

import edu.hitsz.aircraftwar.aircraft.AbstractAircraft;

/**
 * 敌机工厂接口（工厂模式）
 */
public interface EnemyFactory {
    AbstractAircraft createEnemy(int locationX, int locationY, int speedX, int speedY, int hp);
}
