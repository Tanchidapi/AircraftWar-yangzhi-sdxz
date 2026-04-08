package edu.hitsz.aircraftwar.observer;

import java.util.ArrayList;
import java.util.List;

/**
 * 炸弹主题（观察者模式）
 */
public class BombSubject {
    private List<BombObserver> observerList = new ArrayList<>();

    public void addObserver(BombObserver observer) {
        observerList.add(observer);
    }

    public void removeObserver(BombObserver observer) {
        observerList.remove(observer);
    }

    public int notifyAllObservers() {
        int totalScore = 0;
        for (BombObserver observer : observerList) {
            totalScore += observer.update();
        }
        return totalScore;
    }

    public int bombExplode() {
        return notifyAllObservers();
    }
}
