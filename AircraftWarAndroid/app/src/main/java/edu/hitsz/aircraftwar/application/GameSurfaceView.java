package edu.hitsz.aircraftwar.application;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.LinkedList;
import java.util.List;

import edu.hitsz.aircraftwar.R;
import edu.hitsz.aircraftwar.aircraft.*;
import edu.hitsz.aircraftwar.basic.AbstractFlyingObject;
import edu.hitsz.aircraftwar.bullet.BaseBullet;
import edu.hitsz.aircraftwar.factory.*;
import edu.hitsz.aircraftwar.observer.*;
import edu.hitsz.aircraftwar.prop.*;

/**
 * 游戏主画面（SurfaceView实现）
 * 替代原PC端的BaseGame(JPanel)
 * 使用SurfaceView + 游戏循环线程实现Android端游戏渲染
 * 保留模板方法模式
 */
public abstract class GameSurfaceView extends SurfaceView implements SurfaceHolder.Callback, Runnable {

    private static final String TAG = "GameSurfaceView";

    // 游戏线程
    private Thread gameThread;
    private volatile boolean running = false;

    // 游戏时间控制
    private int timeInterval = 40; // 刷新间隔(ms)
    private int time = 0;
    private int cycleDuration = 600;
    private int cycleTime = 0;

    // 背景滚动
    private int backGroundTop = 0;

    // 游戏对象
    private HeroAircraft heroAircraft;
    private final List<AbstractAircraft> enemyAircrafts;
    private final List<BaseBullet> heroBullets;
    private final List<BaseBullet> enemyBullets;
    private final List<AbstractProp> props;
    private BombSubject bombSubject;

    // 游戏参数
    private int enemyMaxNumber;
    private int score = 0;
    private int boss_score;
    private double enemySpeedMultiplier;
    private int heroShootCycleCount = 0;
    private int heroShootCycle;
    private int bossCurrentHp;
    private int bossSpawnCount = 0;
    private int boss_shoot_cycle = 3;
    private int boss_shoot_cal = 0;

    // 游戏状态
    private boolean gameOverFlag = false;
    private String difficulty;
    private boolean soundEnabled;

    // 触屏控制
    private float touchX, touchY;

    // 画笔
    private Paint scorePaint;
    private Paint lifePaint;

    // 音效
    private SoundPool soundPool;
    private int soundBulletHit;
    private int soundGetSupply;
    private int soundBombExplosion;
    private int soundGameOver;
    private MediaPlayer bgmPlayer;
    private MediaPlayer bossBgmPlayer;
    private boolean isBossBgmPlaying = false;

    // 游戏结束回调
    private OnGameOverListener gameOverListener;

    public interface OnGameOverListener {
        void onGameOver(int score, String difficulty);
    }

    public void setOnGameOverListener(OnGameOverListener listener) {
        this.gameOverListener = listener;
    }

    public GameSurfaceView(Context context, String difficulty, boolean soundEnabled) {
        super(context);
        this.difficulty = difficulty;
        this.soundEnabled = soundEnabled;

        getHolder().addCallback(this);
        setFocusable(true);

        // 初始化图片资源
        ImageManager.init(context);
        ImageManager.loadBackgroundByDifficulty(context, difficulty);

        // 初始化游戏参数
        this.enemyMaxNumber = getEnemyMaxNumber();
        this.enemySpeedMultiplier = getEnemySpeedMultiplier();
        this.heroShootCycle = getHeroShootCycle();
        this.boss_score = getBossScoreThreshold();
        this.bossCurrentHp = getBossInitialHp();

        // 重置单例
        HeroAircraft.resetInstance();
        PropEffectManager.resetInstance();

        // 创建英雄机
        heroAircraft = HeroAircraft.getInstance(
                GameConfig.WINDOW_WIDTH / 2,
                GameConfig.WINDOW_HEIGHT - ImageManager.HERO_IMAGE.getHeight(),
                0, 0, getHeroInitialHp());

        // 初始化列表
        enemyAircrafts = new LinkedList<>();
        heroBullets = new LinkedList<>();
        enemyBullets = new LinkedList<>();
        props = new LinkedList<>();

        // 初始化炸弹观察者模式
        bombSubject = new BombSubject();
        bombSubject.addObserver(new MobEnemyObserver(enemyAircrafts));
        bombSubject.addObserver(new EliteEnemyObserver(enemyAircrafts));
        bombSubject.addObserver(new SuperEliteEnemyObserver(enemyAircrafts));
        bombSubject.addObserver(new EnemyBulletObserver(enemyBullets));

        // 初始化道具效果管理器
        PropEffectManager.getInstance().setHeroAircraft(heroAircraft);

        // 初始化画笔
        scorePaint = new Paint();
        scorePaint.setColor(Color.RED);
        scorePaint.setTextSize(44);
        scorePaint.setAntiAlias(true);
        scorePaint.setFakeBoldText(true);

        lifePaint = new Paint();
        lifePaint.setColor(Color.RED);
        lifePaint.setTextSize(44);
        lifePaint.setAntiAlias(true);
        lifePaint.setFakeBoldText(true);

        // 初始化音效
        if (soundEnabled) {
            initSound(context);
        }
    }

    private void initSound(Context context) {
        AudioAttributes attrs = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setMaxStreams(10)
                .setAudioAttributes(attrs)
                .build();

        soundBulletHit = soundPool.load(context, R.raw.bullet_hit, 1);
        soundGetSupply = soundPool.load(context, R.raw.get_supply, 1);
        soundBombExplosion = soundPool.load(context, R.raw.bomb_explosion, 1);
        soundGameOver = soundPool.load(context, R.raw.game_over, 1);

        // 背景音乐
        bgmPlayer = MediaPlayer.create(context, R.raw.bgm);
        if (bgmPlayer != null) {
            bgmPlayer.setLooping(true);
        }

        bossBgmPlayer = MediaPlayer.create(context, R.raw.bgm_boss);
        if (bossBgmPlayer != null) {
            bossBgmPlayer.setLooping(true);
        }
    }

    // ==================== SurfaceHolder.Callback ====================

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // 根据实际Surface尺寸初始化缩放
        GameConfig.init(getWidth(), getHeight());

        running = true;
        gameThread = new Thread(this);
        gameThread.start();

        // 播放背景音乐
        if (soundEnabled && bgmPlayer != null) {
            bgmPlayer.start();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        GameConfig.init(width, height);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        running = false;
        releaseSound();
    }

    // ==================== 触屏控制（替代鼠标监听） ====================

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (gameOverFlag) return true;

        // 将屏幕坐标转换为逻辑坐标
        float logicX = event.getX() / GameConfig.SCALE_X;
        float logicY = event.getY() / GameConfig.SCALE_Y;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                // 边界检查
                if (logicX >= 0 && logicX <= GameConfig.WINDOW_WIDTH
                        && logicY >= 0 && logicY <= GameConfig.WINDOW_HEIGHT) {
                    heroAircraft.setLocation(logicX, logicY);
                }
                break;
        }
        return true;
    }

    // ==================== 游戏主循环 ====================

    @Override
    public void run() {
        while (running) {
            long startTime = System.currentTimeMillis();

            time += timeInterval;

            // 周期性执行
            if (timeCountAndNewCycleJudge()) {
                // 新敌机产生
                spawnEnemies();
                // 射击
                shootAction();
            }

            // 移动
            bulletsMoveAction();
            propsMoveAction();
            aircraftsMoveAction();

            // 碰撞检测
            crashCheckAction();

            // 后处理
            postProcessAction();

            // 绘制
            draw();

            // 游戏结束检查
            if (heroAircraft.getHp() <= 0 && !gameOverFlag) {
                gameOverFlag = true;
                running = false;

                if (soundEnabled) {
                    stopAllMusic();
                    if (soundPool != null) {
                        soundPool.play(soundGameOver, 1, 1, 1, 0, 1);
                    }
                }

                // 通知Activity游戏结束
                post(() -> {
                    if (gameOverListener != null) {
                        gameOverListener.onGameOver(score, difficulty);
                    }
                });
            }

            // 控制帧率
            long elapsed = System.currentTimeMillis() - startTime;
            long sleepTime = timeInterval - elapsed;
            if (sleepTime > 0) {
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }

    // ==================== 游戏逻辑 ====================

    private boolean timeCountAndNewCycleJudge() {
        cycleTime += timeInterval;
        if (cycleTime >= cycleDuration) {
            cycleTime %= cycleDuration;
            return true;
        }
        return false;
    }

    private boolean hasBossAlive() {
        for (AbstractAircraft enemy : enemyAircrafts) {
            if (enemy instanceof BossEliteEnemy) return true;
        }
        return false;
    }

    private void spawnEnemies() {
        // Boss生成
        if (score > boss_score && !hasBossAlive()) {
            boss_score += getBossScoreThreshold();
            bossSpawnCount++;
            int bossHp = bossCurrentHp + (bossSpawnCount - 1) * getBossHpIncrement();
            EnemyFactory bossFactory = new BossEliteEnemyFactory();
            int speedX = (int) (5 * getEnemySpeedMultiplier());
            enemyAircrafts.add(bossFactory.createEnemy(
                    (int) (Math.random() * (GameConfig.WINDOW_WIDTH - ImageManager.ELITE_ENEMY_IMAGE.getWidth())),
                    (int) (Math.random() * GameConfig.WINDOW_HEIGHT * 0.05),
                    speedX, 0, bossHp));

            // Boss音乐
            if (soundEnabled) {
                switchToBossMusic();
            }
        }

        if (enemyAircrafts.size() < enemyMaxNumber) {
            boolean spawnElite = Math.random() < getEliteSpawnRate();
            boolean superSpawnElite = Math.random() < (getEliteSpawnRate() * 0.5);

            if (spawnElite) {
                EnemyFactory eliteFactory = new EliteEnemyFactory();
                int speedY = (int) (8 * getEnemySpeedMultiplier());
                enemyAircrafts.add(eliteFactory.createEnemy(
                        (int) (Math.random() * (GameConfig.WINDOW_WIDTH - ImageManager.ELITE_ENEMY_IMAGE.getWidth())),
                        (int) (Math.random() * GameConfig.WINDOW_HEIGHT * 0.05),
                        0, speedY, 60));
            }
            if (superSpawnElite) {
                EnemyFactory superFactory = new SuperEliteEnemyFactory();
                int speedX = (int) (5 * getEnemySpeedMultiplier());
                int speedY = (int) (6 * getEnemySpeedMultiplier());
                enemyAircrafts.add(superFactory.createEnemy(
                        (int) (Math.random() * (GameConfig.WINDOW_WIDTH - ImageManager.ELITE_ENEMY_IMAGE.getWidth())),
                        (int) (Math.random() * GameConfig.WINDOW_HEIGHT * 0.05),
                        speedX, speedY, 90));
            }

            EnemyFactory mobFactory = new MobEnemyFactory();
            int speedY = (int) (10 * getEnemySpeedMultiplier());
            enemyAircrafts.add(mobFactory.createEnemy(
                    (int) (Math.random() * (GameConfig.WINDOW_WIDTH - ImageManager.MOB_ENEMY_IMAGE.getWidth())),
                    (int) (Math.random() * GameConfig.WINDOW_HEIGHT * 0.05),
                    0, speedY, 30));
        }
    }

    private void shootAction() {
        // 敌机射击
        for (AbstractAircraft enemy : enemyAircrafts) {
            if (enemy instanceof EliteEnemy || enemy instanceof SuperEliteEnemy) {
                enemyBullets.addAll(enemy.shoot());
            }
        }
        boss_shoot_cal++;
        if (boss_shoot_cal > boss_shoot_cycle) {
            boss_shoot_cal = 0;
            for (AbstractAircraft enemy : enemyAircrafts) {
                if (enemy instanceof BossEliteEnemy) {
                    enemyBullets.addAll(enemy.shoot());
                }
            }
        }

        // 英雄射击
        heroShootCycleCount++;
        if (heroShootCycleCount >= heroShootCycle) {
            heroShootCycleCount = 0;
            heroBullets.addAll(heroAircraft.shoot());
        }
    }

    private void bulletsMoveAction() {
        for (BaseBullet bullet : heroBullets) bullet.forward();
        for (BaseBullet bullet : enemyBullets) bullet.forward();
    }

    private void propsMoveAction() {
        for (AbstractProp prop : props) prop.forward();
    }

    private void aircraftsMoveAction() {
        for (AbstractAircraft enemy : enemyAircrafts) enemy.forward();
    }

    private void crashCheckAction() {
        // 敌机子弹攻击英雄
        for (BaseBullet bullet : enemyBullets) {
            if (bullet.notValid()) continue;
            if (heroAircraft.crash(bullet)) {
                heroAircraft.decreaseHp(bullet.getPower());
                bullet.vanish();
            }
        }

        // 英雄子弹攻击敌机
        for (BaseBullet bullet : heroBullets) {
            if (bullet.notValid()) continue;
            for (AbstractAircraft enemy : enemyAircrafts) {
                if (enemy.notValid()) continue;
                if (enemy.crash(bullet)) {
                    enemy.decreaseHp(bullet.getPower());
                    bullet.vanish();

                    if (soundEnabled && soundPool != null) {
                        soundPool.play(soundBulletHit, 0.5f, 0.5f, 1, 0, 1);
                    }

                    if (enemy.notValid()) {
                        score += 10;

                        // Boss坠毁恢复背景音乐
                        if (enemy instanceof BossEliteEnemy && soundEnabled) {
                            switchToNormalMusic();
                        }

                        // 精英敌机掉落道具
                        if (enemy instanceof EliteEnemy || enemy instanceof SuperEliteEnemy) {
                            if (Math.random() < 0.8) {
                                dropProp(enemy.getLocationX(), enemy.getLocationY(), 1);
                            }
                        }
                        // Boss掉落多个道具
                        if (enemy instanceof BossEliteEnemy) {
                            if (Math.random() < 0.9) {
                                int dropCount = 1 + (int) (Math.random() * 3);
                                for (int i = 0; i < dropCount; i++) {
                                    int offsetX = (i == 1) ? -40 : (i == 2) ? 40 : 0;
                                    dropProp(enemy.getLocationX() + offsetX, enemy.getLocationY(), 1);
                                }
                            }
                        }
                    }
                }
                // 英雄机与敌机相撞
                if (enemy.crash(heroAircraft) || heroAircraft.crash(enemy)) {
                    enemy.vanish();
                    heroAircraft.decreaseHp(Integer.MAX_VALUE);
                }
            }
        }

        // 英雄获得道具
        for (AbstractProp prop : props) {
            if (prop.notValid()) continue;
            if (heroAircraft.crash(prop)) {
                if (soundEnabled && soundPool != null) {
                    soundPool.play(soundGetSupply, 1, 1, 1, 0, 1);
                }

                if (prop instanceof BloodProp) {
                    heroAircraft.increaseHp(30);
                } else if (prop instanceof BombProp) {
                    if (soundEnabled && soundPool != null) {
                        soundPool.play(soundBombExplosion, 1, 1, 1, 0, 1);
                    }
                    int earnedScore = bombSubject.bombExplode();
                    score += earnedScore;
                } else if (prop instanceof BulletProp) {
                    PropEffectManager.getInstance().activateScatterEffect(12000);
                } else if (prop instanceof SuperBulletProp) {
                    PropEffectManager.getInstance().activateRingEffect(12000);
                }
                prop.vanish();
            }
        }
    }

    private void dropProp(int px, int py, int count) {
        double pt = Math.random();
        PropFactory propFactory;
        if (pt < 0.25) {
            propFactory = new BloodPropFactory();
        } else if (pt < 0.5) {
            propFactory = new BombPropFactory();
        } else if (pt < 0.8) {
            propFactory = new BulletPropFactory();
        } else {
            propFactory = new SuperBulletPropFactory();
        }
        props.add(propFactory.createProp(px, py, 0, 5));
    }

    private void postProcessAction() {
        enemyBullets.removeIf(AbstractFlyingObject::notValid);
        heroBullets.removeIf(AbstractFlyingObject::notValid);
        enemyAircrafts.removeIf(AbstractFlyingObject::notValid);
        props.removeIf(AbstractFlyingObject::notValid);
    }

    // ==================== 绘制（Android Canvas） ====================

    private void draw() {
        Canvas canvas = null;
        try {
            canvas = getHolder().lockCanvas();
            if (canvas == null) return;

            // 计算缩放
            float scaleX = (float) canvas.getWidth() / GameConfig.WINDOW_WIDTH;
            float scaleY = (float) canvas.getHeight() / GameConfig.WINDOW_HEIGHT;
            canvas.save();
            canvas.scale(scaleX, scaleY);

            // 绘制背景（滚动）
            drawBackground(canvas);

            // 绘制子弹
            drawObjects(canvas, enemyBullets);
            drawObjects(canvas, heroBullets);

            // 绘制道具
            drawProps(canvas);

            // 绘制敌机
            drawAircrafts(canvas);

            // 绘制英雄机
            Bitmap heroImg = ImageManager.HERO_IMAGE;
            if (heroImg != null) {
                canvas.drawBitmap(heroImg,
                        heroAircraft.getLocationX() - heroImg.getWidth() / 2f,
                        heroAircraft.getLocationY() - heroImg.getHeight() / 2f, null);
            }

            // 绘制分数和生命值
            drawScoreAndLife(canvas);

            canvas.restore();
        } catch (Exception e) {
            Log.e(TAG, "绘制异常: " + e.getMessage());
        } finally {
            if (canvas != null) {
                try {
                    getHolder().unlockCanvasAndPost(canvas);
                } catch (Exception e) {
                    Log.e(TAG, "解锁Canvas异常: " + e.getMessage());
                }
            }
        }
    }

    private void drawBackground(Canvas canvas) {
        Bitmap bg = ImageManager.BACKGROUND_IMAGE;
        if (bg == null) return;

        Rect destTop = new Rect(0, backGroundTop - GameConfig.WINDOW_HEIGHT,
                GameConfig.WINDOW_WIDTH, backGroundTop);
        Rect destBottom = new Rect(0, backGroundTop,
                GameConfig.WINDOW_WIDTH, backGroundTop + GameConfig.WINDOW_HEIGHT);

        canvas.drawBitmap(bg, null, destTop, null);
        canvas.drawBitmap(bg, null, destBottom, null);

        backGroundTop += 1;
        if (backGroundTop >= GameConfig.WINDOW_HEIGHT) {
            backGroundTop = 0;
        }
    }

    private <T extends AbstractFlyingObject> void drawObjects(Canvas canvas, List<T> objects) {
        for (T obj : objects) {
            Bitmap img = obj.getImage();
            if (img != null) {
                canvas.drawBitmap(img,
                        obj.getLocationX() - img.getWidth() / 2f,
                        obj.getLocationY() - img.getHeight() / 2f, null);
            }
        }
    }

    private void drawProps(Canvas canvas) {
        for (AbstractProp prop : props) {
            Bitmap img = prop.getImage();
            if (img != null) {
                canvas.drawBitmap(img,
                        prop.getLocationX() - img.getWidth() / 2f,
                        prop.getLocationY() - img.getHeight() / 2f, null);
            }
        }
    }

    private void drawAircrafts(Canvas canvas) {
        for (AbstractAircraft enemy : enemyAircrafts) {
            Bitmap img = enemy.getImage();
            if (img != null) {
                canvas.drawBitmap(img,
                        enemy.getLocationX() - img.getWidth() / 2f,
                        enemy.getLocationY() - img.getHeight() / 2f, null);
            }
        }
    }

    private void drawScoreAndLife(Canvas canvas) {
        // 绘制半透明HUD背景
        Paint hudBgPaint = new Paint();
        hudBgPaint.setColor(Color.argb(120, 0, 0, 0));
        canvas.drawRect(0, 0, GameConfig.WINDOW_WIDTH, 120, hudBgPaint);

        // 分数
        scorePaint.setTextSize(36);
        canvas.drawText("SCORE: " + score, 15, 42, scorePaint);

        // 生命值
        lifePaint.setTextSize(36);
        canvas.drawText("HP: " + heroAircraft.getHp(), 15, 85, lifePaint);

        // 难度标识
        Paint diffPaint = new Paint();
        diffPaint.setColor(Color.YELLOW);
        diffPaint.setTextSize(28);
        diffPaint.setAntiAlias(true);
        diffPaint.setTextAlign(Paint.Align.RIGHT);
        String diffText = "";
        switch (difficulty) {
            case "EASY": diffText = "简单"; break;
            case "NORMAL": diffText = "普通"; break;
            case "HARD": diffText = "困难"; break;
        }
        canvas.drawText(diffText, GameConfig.WINDOW_WIDTH - 15, 42, diffPaint);
    }

    // ==================== 音乐控制 ====================

    private void switchToBossMusic() {
        if (bgmPlayer != null && bgmPlayer.isPlaying()) {
            bgmPlayer.pause();
        }
        if (bossBgmPlayer != null && !bossBgmPlayer.isPlaying()) {
            bossBgmPlayer.start();
            isBossBgmPlaying = true;
        }
    }

    private void switchToNormalMusic() {
        if (bossBgmPlayer != null && bossBgmPlayer.isPlaying()) {
            bossBgmPlayer.pause();
            isBossBgmPlaying = false;
        }
        if (bgmPlayer != null && !bgmPlayer.isPlaying()) {
            bgmPlayer.start();
        }
    }

    private void stopAllMusic() {
        if (bgmPlayer != null && bgmPlayer.isPlaying()) {
            bgmPlayer.pause();
        }
        if (bossBgmPlayer != null && bossBgmPlayer.isPlaying()) {
            bossBgmPlayer.pause();
        }
    }

    private void releaseSound() {
        if (bgmPlayer != null) {
            bgmPlayer.release();
            bgmPlayer = null;
        }
        if (bossBgmPlayer != null) {
            bossBgmPlayer.release();
            bossBgmPlayer = null;
        }
        if (soundPool != null) {
            soundPool.release();
            soundPool = null;
        }
    }

    public void stopGame() {
        running = false;
        releaseSound();
    }

    // ==================== 模板方法（抽象方法） ====================

    protected abstract int getEnemyMaxNumber();
    protected abstract int getHeroShootCycle();
    protected abstract double getEnemySpeedMultiplier();
    protected abstract int getHeroInitialHp();
    protected abstract double getEliteSpawnRate();
    protected abstract int getBossScoreThreshold();
    protected abstract int getBossInitialHp();
    protected abstract int getBossHpIncrement();

    protected int getGameTimeSeconds() {
        return time / 1000;
    }

    protected int getCurrentDifficultyLevel() {
        return Math.min(getGameTimeSeconds() / 30 + 1, 10);
    }
}
