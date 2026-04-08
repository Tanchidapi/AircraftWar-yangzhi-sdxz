package edu.hitsz.aircraftwar.prop;

/**
 * 道具效果任务（Runnable）
 */
public class PropEffectTask implements Runnable {

    private PropEffectManager manager;

    public PropEffectTask(PropEffectManager manager) {
        this.manager = manager;
    }

    @Override
    public void run() {
        try {
            while (!manager.shouldEndEffect()) {
                Thread.sleep(100);
            }
            manager.endEffect();
        } catch (InterruptedException e) {
            // 线程被中断
        }
    }
}
