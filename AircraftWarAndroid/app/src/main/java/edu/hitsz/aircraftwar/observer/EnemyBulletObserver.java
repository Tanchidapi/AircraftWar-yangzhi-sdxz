package edu.hitsz.aircraftwar.observer;

import edu.hitsz.aircraftwar.bullet.BaseBullet;
import java.util.List;

public class EnemyBulletObserver implements BombObserver {
    private List<BaseBullet> enemyBullets;

    public EnemyBulletObserver(List<BaseBullet> enemyBullets) {
        this.enemyBullets = enemyBullets;
    }

    @Override
    public int update() {
        enemyBullets.clear();
        return 0;
    }
}
