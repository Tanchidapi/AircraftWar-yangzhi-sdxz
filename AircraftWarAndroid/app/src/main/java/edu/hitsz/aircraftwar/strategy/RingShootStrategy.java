package edu.hitsz.aircraftwar.strategy;

import edu.hitsz.aircraftwar.aircraft.AbstractAircraft;
import edu.hitsz.aircraftwar.aircraft.HeroAircraft;
import edu.hitsz.aircraftwar.bullet.BaseBullet;
import edu.hitsz.aircraftwar.bullet.EnemyBullet;
import edu.hitsz.aircraftwar.bullet.HeroBullet;

import java.util.LinkedList;
import java.util.List;

/**
 * 环射策略
 */
public class RingShootStrategy implements ShootStrategy {
    @Override
    public List<BaseBullet> shoot(AbstractAircraft shooter) {
        List<BaseBullet> res = new LinkedList<>();
        int x = shooter.getLocationX();
        int y = shooter.getLocationY();
        int power = (shooter instanceof HeroAircraft) ? 25 : 10;
        double speed = 6.0;
        int n = 20;

        for (int i = 0; i < n; i++) {
            double angle = 2 * Math.PI * i / n;
            int vx = (int) Math.round(speed * Math.cos(angle));
            int vy = (int) Math.round(speed * Math.sin(angle));
            BaseBullet bullet = (shooter instanceof HeroAircraft)
                    ? new HeroBullet(x, y, vx, vy, power)
                    : new EnemyBullet(x, y, vx, vy, power);
            res.add(bullet);
        }
        return res;
    }
}
