package edu.hitsz.aircraftwar.observer;

import edu.hitsz.aircraftwar.aircraft.AbstractAircraft;
import edu.hitsz.aircraftwar.aircraft.EliteEnemy;
import java.util.List;
import java.util.Iterator;

public class EliteEnemyObserver implements BombObserver {
    private List<AbstractAircraft> enemyAircrafts;

    public EliteEnemyObserver(List<AbstractAircraft> enemyAircrafts) {
        this.enemyAircrafts = enemyAircrafts;
    }

    @Override
    public int update() {
        int totalScore = 0;
        Iterator<AbstractAircraft> iterator = enemyAircrafts.iterator();
        while (iterator.hasNext()) {
            AbstractAircraft aircraft = iterator.next();
            if (aircraft instanceof EliteEnemy) {
                totalScore += 10;
                aircraft.vanish();
                iterator.remove();
            }
        }
        return totalScore;
    }
}
