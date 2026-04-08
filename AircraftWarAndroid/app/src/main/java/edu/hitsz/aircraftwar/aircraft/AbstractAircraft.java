package edu.hitsz.aircraftwar.aircraft;

import edu.hitsz.aircraftwar.bullet.BaseBullet;
import edu.hitsz.aircraftwar.basic.AbstractFlyingObject;
import edu.hitsz.aircraftwar.strategy.ShootStrategy;

import java.util.LinkedList;
import java.util.List;

/**
 * 所有种类飞机的抽象父类
 */
public abstract class AbstractAircraft extends AbstractFlyingObject {

    protected int maxHp;
    protected int hp;
    protected ShootStrategy shootStrategy;

    public AbstractAircraft(int locationX, int locationY, int speedX, int speedY, int hp) {
        super(locationX, locationY, speedX, speedY);
        this.hp = hp;
        this.maxHp = hp;
    }

    public void decreaseHp(int decrease) {
        hp -= decrease;
        if (hp <= 0) {
            hp = 0;
            vanish();
        }
    }

    public void increaseHp(int increase) {
        if (increase <= 0) return;
        hp = Math.min(maxHp, hp + increase);
    }

    public int getHp() {
        return hp;
    }

    public void setShootStrategy(ShootStrategy strategy) {
        this.shootStrategy = strategy;
    }

    public List<BaseBullet> shoot() {
        if (shootStrategy != null) {
            return shootStrategy.shoot(this);
        }
        return new LinkedList<>();
    }
}
