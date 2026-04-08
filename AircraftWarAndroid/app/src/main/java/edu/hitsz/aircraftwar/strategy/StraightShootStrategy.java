package edu.hitsz.aircraftwar.strategy;

import edu.hitsz.aircraftwar.aircraft.AbstractAircraft;
import edu.hitsz.aircraftwar.aircraft.HeroAircraft;
import edu.hitsz.aircraftwar.bullet.BaseBullet;
import edu.hitsz.aircraftwar.bullet.EnemyBullet;
import edu.hitsz.aircraftwar.bullet.HeroBullet;

import java.util.LinkedList;
import java.util.List;

/**
 * 直射策略
 */
public class StraightShootStrategy implements ShootStrategy {
    @Override
    public List<BaseBullet> shoot(AbstractAircraft shooter) {
        List<BaseBullet> res = new LinkedList<>();
        int direction = (shooter instanceof HeroAircraft) ? -1 : 1;
        int x = shooter.getLocationX();
        int y = shooter.getLocationY() + direction * 2;
        int speedX = 0;
        int speedY = shooter.getSpeedY() + direction * 5;
        int power = (shooter instanceof HeroAircraft) ? 30 : 10;

        BaseBullet bullet = (shooter instanceof HeroAircraft)
                ? new HeroBullet(x, y, speedX, speedY, power)
                : new EnemyBullet(x, y, speedX, speedY, power);
        res.add(bullet);
        return res;
    }
}
