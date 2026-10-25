package entity;

import level.Level;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;

/** คลาสแม่ของศัตรูทุกแบบ (WalkerEnemy, FlyingEnemy ใน M7) */
public abstract class Enemy extends Entity {

    public final int contactDamage; // เลือดที่ผู้เล่นเสียเมื่อชนตัวศัตรู

    public Enemy(double x, double y, int width, int height, int maxHp, int contactDamage) {
        super(x, y, width, height, maxHp);
        this.contactDamage = contactDamage;
    }

    /** projectiles ใช้สำหรับศัตรูที่ยิงได้ (ใส่กระสุนเพิ่มเข้าไปในลิสต์นี้) */
    public abstract void update(Player player, Level level, List<Projectile> projectiles);

    public abstract void draw(Graphics2D g2, int cameraX);

    /** หลอดเลือดเล็ก ๆ เหนือหัวศัตรู */
    protected void drawHealthBar(Graphics2D g2, int cameraX) {
        if (hp >= maxHp) return; // ยังไม่โดนตี ไม่ต้องแสดง
        int sx = (int) x - cameraX;
        int sy = (int) y - 10;
        g2.setColor(new Color(90, 20, 20));
        g2.fillRect(sx, sy, width, 5);
        g2.setColor(new Color(60, 200, 80));
        g2.fillRect(sx, sy, (int) (width * Math.max(0, (double) hp / maxHp)), 5);
    }
}
