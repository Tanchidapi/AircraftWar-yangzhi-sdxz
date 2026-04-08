package edu.hitsz.aircraftwar.factory;

import edu.hitsz.aircraftwar.aircraft.AbstractAircraft;
import edu.hitsz.aircraftwar.aircraft.BossEliteEnemy;

public class BossEliteEnemyFactory implements EnemyFactory {
    public AbstractAircraft createEnemy(int locationX, int locationY, int speedX, int speedY, int hp) {
        return new BossEliteEnemy(locationX, locationY, speedX, speedY, hp);
    }
}
