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
 * 所有图片会按照逻辑坐标系(512x768)进行缩放，确保大小合理
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
     * 将Bitmap缩放到指定宽高
     */
    private static Bitmap scaleBitmap(Bitmap src, int targetWidth, int targetHeight) {
        if (src == null) return null;
        return Bitmap.createScaledBitmap(src, targetWidth, targetHeight, true);
    }

    /**
     * 初始化所有图片资源
     * @param context Android上下文
     */
    public static void init(Context context) {
        if (initialized) return;

        // 默认加载简单模式背景（背景不缩放，绘制时由Canvas拉伸）
        BACKGROUND_IMAGE = BitmapFactory.decodeResource(context.getResources(), R.drawable.bg);

        // 英雄机：约60x60
        HERO_IMAGE = scaleBitmap(
                BitmapFactory.decodeResource(context.getResources(), R.drawable.hero), 60, 60);

        // 普通敌机：约45x40
        MOB_ENEMY_IMAGE = scaleBitmap(
                BitmapFactory.decodeResource(context.getResources(), R.drawable.mob), 45, 40);

        // 精英敌机：约50x50
        ELITE_ENEMY_IMAGE = scaleBitmap(
                BitmapFactory.decodeResource(context.getResources(), R.drawable.elite), 50, 50);

        // 超级精英敌机：约55x55
        SUPER_ELITE_ENEMY_IMAGE = scaleBitmap(
                BitmapFactory.decodeResource(context.getResources(), R.drawable.eliteplus), 55, 55);

        // Boss敌机：约110x90
        BOSS_ELITE_ENEMY_IMAGE = scaleBitmap(
                BitmapFactory.decodeResource(context.getResources(), R.drawable.boss), 110, 90);

        // 英雄子弹：约8x20
        HERO_BULLET_IMAGE = scaleBitmap(
                BitmapFactory.decodeResource(context.getResources(), R.drawable.bullet_hero), 8, 20);

        // 敌机子弹：约8x20
        ENEMY_BULLET_IMAGE = scaleBitmap(
                BitmapFactory.decodeResource(context.getResources(), R.drawable.bullet_enemy), 8, 20);

        // 道具：约35x35
        PROP_BLOOD_IMAGE = scaleBitmap(
                BitmapFactory.decodeResource(context.getResources(), R.drawable.prop_blood), 35, 35);
        PROP_BOMB_IMAGE = scaleBitmap(
                BitmapFactory.decodeResource(context.getResources(), R.drawable.prop_bomb), 35, 35);
        PROP_BULLET_IMAGE = scaleBitmap(
                BitmapFactory.decodeResource(context.getResources(), R.drawable.prop_bullet), 35, 35);
        SUPER_PROP_BULLET_IMAGE = scaleBitmap(
                BitmapFactory.decodeResource(context.getResources(), R.drawable.prop_bulletplus), 35, 35);

        // 注册类名到图片的映射
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
