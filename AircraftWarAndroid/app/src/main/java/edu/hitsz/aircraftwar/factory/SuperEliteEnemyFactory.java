package edu.hitsz.aircraftwar.factory;

import edu.hitsz.aircraftwar.aircraft.AbstractAircraft;
import edu.hitsz.aircraftwar.aircraft.SuperEliteEnemy;

public class SuperEliteEnemyFactory implements EnemyFactory {
    public AbstractAircraft createEnemy(int locationX, int locationY, int speedX, int speedY, int hp) {
        return new SuperEliteEnemy(locationX, locationY, speedX, speedY, hp);
    }
}
