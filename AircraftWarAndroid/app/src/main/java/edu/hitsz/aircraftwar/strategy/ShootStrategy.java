package edu.hitsz.aircraftwar.strategy;

import edu.hitsz.aircraftwar.aircraft.AbstractAircraft;
import edu.hitsz.aircraftwar.bullet.BaseBullet;
import java.util.List;

/**
 * 射击策略接口（策略模式）
 */
public interface ShootStrategy {
    List<BaseBullet> shoot(AbstractAircraft shooter);
}
