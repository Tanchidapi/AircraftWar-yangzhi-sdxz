package edu.hitsz.aircraftwar.application;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.util.HashMap;
import java.util.Map;

import edu.hitsz.aircraftwar.R;
import edu.hitsz.aircraftwar.aircraft.*;
import edu.hitsz.aircraftwar.bullet.*;
import edu.hitsz.aircraftwar.prop.*;

/**
 * 图片资源管理器（Android适配版）
 * 使用Android的BitmapFactory加载drawable资源
 */
public class ImageManager {

    private static final Map<String, Bitmap> CLASSNAME_IMAGE_MAP = new HashMap<>();

    public static Bitmap BACKGROUND_IMAGE;
    public static Bitmap HERO_IMAGE;
    public static Bitmap HERO_BULLET_IMAGE;
    public static Bitmap ENEMY_BULLET_IMAGE;
    public static Bitmap MOB_ENEMY_IMAGE;
    public static Bitmap ELITE_ENEMY_IMAGE;
    public static Bitmap SUPER_ELITE_ENEMY_IMAGE;
    public static Bitmap BOSS_ELITE_ENEMY_IMAGE;
    public static Bitmap PROP_BLOOD_IMAGE;
    public static Bitmap PROP_BOMB_IMAGE;
    public static Bitmap PROP_BULLET_IMAGE;
    public static Bitmap SUPER_PROP_BULLET_IMAGE;

    private static boolean initialized = false;

    /**
     * 初始化所有图片资源
     * @param context Android上下文
     */
    public static void init(Context context) {
        if (initialized) return;

        // 默认加载简单模式背景
        BACKGROUND_IMAGE = BitmapFactory.decodeResource(context.getResources(), R.drawable.bg);

        HERO_IMAGE = BitmapFactory.decodeResource(context.getResources(), R.drawable.hero);
        MOB_ENEMY_IMAGE = BitmapFactory.decodeResource(context.getResources(), R.drawable.mob);
        ELITE_ENEMY_IMAGE = BitmapFactory.decodeResource(context.getResources(), R.drawable.elite);
        SUPER_ELITE_ENEMY_IMAGE = BitmapFactory.decodeResource(context.getResources(), R.drawable.eliteplus);
        BOSS_ELITE_ENEMY_IMAGE = BitmapFactory.decodeResource(context.getResources(), R.drawable.boss);
        HERO_BULLET_IMAGE = BitmapFactory.decodeResource(context.getResources(), R.drawable.bullet_hero);
        ENEMY_BULLET_IMAGE = BitmapFactory.decodeResource(context.getResources(), R.drawable.bullet_enemy);
        PROP_BLOOD_IMAGE = BitmapFactory.decodeResource(context.getResources(), R.drawable.prop_blood);
        PROP_BOMB_IMAGE = BitmapFactory.decodeResource(context.getResources(), R.drawable.prop_bomb);
        PROP_BULLET_IMAGE = BitmapFactory.decodeResource(context.getResources(), R.drawable.prop_bullet);
        SUPER_PROP_BULLET_IMAGE = BitmapFactory.decodeResource(context.getResources(), R.drawable.prop_bulletplus);

        CLASSNAME_IMAGE_MAP.put(HeroAircraft.class.getName(), HERO_IMAGE);
        CLASSNAME_IMAGE_MAP.put(MobEnemy.class.getName(), MOB_ENEMY_IMAGE);
        CLASSNAME_IMAGE_MAP.put(EliteEnemy.class.getName(), ELITE_ENEMY_IMAGE);
        CLASSNAME_IMAGE_MAP.put(SuperEliteEnemy.class.getName(), SUPER_ELITE_ENEMY_IMAGE);
        CLASSNAME_IMAGE_MAP.put(BossEliteEnemy.class.getName(), BOSS_ELITE_ENEMY_IMAGE);
        CLASSNAME_IMAGE_MAP.put(HeroBullet.class.getName(), HERO_BULLET_IMAGE);
        CLASSNAME_IMAGE_MAP.put(EnemyBullet.class.getName(), ENEMY_BULLET_IMAGE);
        CLASSNAME_IMAGE_MAP.put(BloodProp.class.getName(), PROP_BLOOD_IMAGE);
        CLASSNAME_IMAGE_MAP.put(BombProp.class.getName(), PROP_BOMB_IMAGE);
        CLASSNAME_IMAGE_MAP.put(BulletProp.class.getName(), PROP_BULLET_IMAGE);
        CLASSNAME_IMAGE_MAP.put(SuperBulletProp.class.getName(), SUPER_PROP_BULLET_IMAGE);

        initialized = true;
    }

    public static Bitmap get(String className) {
        return CLASSNAME_IMAGE_MAP.get(className);
    }

    public static Bitmap get(Object obj) {
        if (obj == null) return null;
        return get(obj.getClass().getName());
    }

    /**
     * 根据游戏难度加载对应的背景图片
     */
    public static void loadBackgroundByDifficulty(Context context, String difficulty) {
        int bgResId = R.drawable.bg; // 默认简单
        if ("NORMAL".equals(difficulty)) {
            bgResId = R.drawable.bg2;
        } else if ("HARD".equals(difficulty)) {
            bgResId = R.drawable.bg3;
        }
        BACKGROUND_IMAGE = BitmapFactory.decodeResource(context.getResources(), bgResId);
    }
}
