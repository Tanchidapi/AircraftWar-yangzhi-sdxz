package edu.hitsz.aircraftwar.application;

/**
 * 游戏常量配置类
 * 替代原PC端的Main类中的常量定义
 * 支持动态适配不同屏幕比例，消除黑边
 */
public class GameConfig {

    /**
     * 游戏画布宽度（逻辑坐标，固定值）
     */
    public static final int WINDOW_WIDTH = 512;

    /**
     * 游戏画布高度（逻辑坐标，会根据屏幕比例动态调整）
     * 默认值 768，实际值在 init() 中根据屏幕比例计算
     */
    public static int WINDOW_HEIGHT = 768;

    /**
     * 缩放比例（实际屏幕宽度 / 逻辑宽度）
     */
    public static float SCALE_X = 1.0f;
    public static float SCALE_Y = 1.0f;

    /**
     * 实际屏幕宽高（像素）
     */
    public static int SCREEN_WIDTH = 512;
    public static int SCREEN_HEIGHT = 768;

    /**
     * 根据实际屏幕尺寸初始化缩放参数
     * 动态调整逻辑高度以适配不同屏幕比例，消除黑边
     */
    public static void init(int screenWidth, int screenHeight) {
        SCREEN_WIDTH = screenWidth;
        SCREEN_HEIGHT = screenHeight;

        // 根据屏幕实际宽高比动态计算逻辑高度
        // 逻辑宽度固定为 WINDOW_WIDTH(512)，高度按比例缩放
        float screenRatio = (float) screenHeight / screenWidth;
        WINDOW_HEIGHT = (int) (WINDOW_WIDTH * screenRatio);

        // 缩放比例：统一使用宽度缩放比，保证X/Y缩放一致不变形
        SCALE_X = (float) screenWidth / WINDOW_WIDTH;
        SCALE_Y = (float) screenHeight / WINDOW_HEIGHT;
    }
}
