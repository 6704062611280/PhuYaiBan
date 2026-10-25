package entity;

import java.awt.Rectangle;

/** คลาสแม่ของทุกอย่างที่อยู่ในโลกเกม (ผู้เล่น ศัตรู กระสุน) */
public abstract class Entity {
    public double x, y;          // มุมซ้ายบน (พิกัดโลก ไม่ใช่พิกัดหน้าจอ)
    public int width, height;
    public int hp, maxHp;

    public Entity(double x, double y, int width, int height, int maxHp) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.maxHp = maxHp;
        this.hp = maxHp;
    }

    public double centerX() { return x + width / 2.0; }
    public double centerY() { return y + height / 2.0; }

    public boolean isAlive() { return hp > 0; }

    public Rectangle getBounds() {
        return new Rectangle((int) x, (int) y, width, height);
    }
}
