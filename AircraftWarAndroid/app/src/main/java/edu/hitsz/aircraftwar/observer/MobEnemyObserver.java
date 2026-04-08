package edu.hitsz.aircraftwar.observer;

import java.util.List;
import java.util.Iterator;
import edu.hitsz.aircraftwar.aircraft.MobEnemy;
import edu.hitsz.aircraftwar.aircraft.AbstractAircraft;

public class MobEnemyObserver implements BombObserver {
    private List<AbstractAircraft> enemyAircrafts;

    public MobEnemyObserver(List<AbstractAircraft> enemyAircrafts) {
        this.enemyAircrafts = enemyAircrafts;
    }

    @Override
    public int update() {
        int totalScore = 0;
        Iterator<AbstractAircraft> iterator = enemyAircrafts.iterator();
        while (iterator.hasNext()) {
            AbstractAircraft aircraft = iterator.next();
            if (aircraft instanceof MobEnemy) {
                totalScore += 10;
                aircraft.vanish();
                iterator.remove();
            }
        }
        return totalScore;
    }
}
