package edu.hitsz.aircraftwar.application;

/**
 * 游戏常量配置类
 * 替代原PC端的Main类中的常量定义
 */
public class GameConfig {

    /**
     * 游戏画布宽度（逻辑坐标，运行时会根据屏幕缩放）
     */
    public static int WINDOW_WIDTH = 512;

    /**
     * 游戏画布高度（逻辑坐标，运行时会根据屏幕缩放）
     */
    public static int WINDOW_HEIGHT = 768;

    /**
     * 缩放比例（实际屏幕宽度 / 逻辑宽度）
     */
    public static float SCALE_X = 1.0f;
    public static float SCALE_Y = 1.0f;

    /**
     * 根据实际屏幕尺寸初始化缩放参数
     */
    public static void init(int screenWidth, int screenHeight) {
        SCALE_X = (float) screenWidth / WINDOW_WIDTH;
        SCALE_Y = (float) screenHeight / WINDOW_HEIGHT;
    }
}
