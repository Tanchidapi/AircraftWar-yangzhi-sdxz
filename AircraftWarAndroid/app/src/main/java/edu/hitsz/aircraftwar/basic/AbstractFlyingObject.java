package edu.hitsz.aircraftwar.basic;

import android.graphics.Bitmap;

import edu.hitsz.aircraftwar.aircraft.AbstractAircraft;
import edu.hitsz.aircraftwar.application.GameConfig;
import edu.hitsz.aircraftwar.application.ImageManager;

/**
 * 可飞行对象的父类（Android适配版）
 * 使用Android Bitmap替代BufferedImage
 */
public abstract class AbstractFlyingObject {

    /** x 轴坐标（图片中心） */
    protected int locationX;
    /** y 轴坐标（图片中心） */
    protected int locationY;
    /** x 轴移动速度 */
    protected int speedX;
    /** y 轴移动速度 */
    protected int speedY;
    /** 图片 */
    protected Bitmap image = null;
    /** x 轴长度 */
    protected int width = -1;
    /** y 轴长度 */
    protected int height = -1;
    /** 有效（生存）标记 */
    protected boolean isValid = true;

    public AbstractFlyingObject() {
    }

    public AbstractFlyingObject(int locationX, int locationY, int speedX, int speedY) {
        this.locationX = locationX;
        this.locationY = locationY;
        this.speedX = speedX;
        this.speedY = speedY;
    }

    /**
     * 可飞行对象根据速度移动
     * 若飞行对象触碰到横向边界，横向速度反向
     */
    public void forward() {
        locationX += speedX;
        locationY += speedY;
        if (locationX <= 0 || locationX >= GameConfig.WINDOW_WIDTH) {
            speedX = -speedX;
        }
    }

    /**
     * 碰撞检测
     */
    public boolean crash(AbstractFlyingObject flyingObject) {
        int factor = this instanceof AbstractAircraft ? 2 : 1;
        int fFactor = flyingObject instanceof AbstractAircraft ? 2 : 1;

        int x = flyingObject.getLocationX();
        int y = flyingObject.getLocationY();
        int fWidth = flyingObject.getWidth();
        int fHeight = flyingObject.getHeight();

        return x + (fWidth + this.getWidth()) / 2 > locationX
                && x - (fWidth + this.getWidth()) / 2 < locationX
                && y + (fHeight / fFactor + this.getHeight() / factor) / 2 > locationY
                && y - (fHeight / fFactor + this.getHeight() / factor) / 2 < locationY;
    }

    public int getLocationX() {
        return locationX;
    }

    public int getLocationY() {
        return locationY;
    }

    public void setLocation(double locationX, double locationY) {
        this.locationX = (int) locationX;
        this.locationY = (int) locationY;
    }

    public int getSpeedY() {
        return speedY;
    }

    public Bitmap getImage() {
        if (image == null) {
            image = ImageManager.get(this);
        }
        return image;
    }

    public int getWidth() {
        if (width == -1) {
            Bitmap bmp = ImageManager.get(this);
            if (bmp != null) {
                width = bmp.getWidth();
            }
        }
        return width;
    }

    public int getHeight() {
        if (height == -1) {
            Bitmap bmp = ImageManager.get(this);
            if (bmp != null) {
                height = bmp.getHeight();
            }
        }
        return height;
    }

    public boolean notValid() {
        return !this.isValid;
    }

    public void vanish() {
        isValid = false;
    }
}
