package entity;

import level.Level;

import java.awt.Color;
import java.awt.Graphics2D;

/** กระสุน ใช้ได้ทั้งของผู้เล่นและศัตรู (แยกด้วย fromPlayer) */
public class Projectile extends Entity {

    public boolean fromPlayer;          // เปลี่ยนได้ เมื่อกระสุนถูกสะท้อนกลับ
    public boolean reflected = false;   // true = ถูกศัตรูสะท้อนมา
    public final int damage;
    public boolean active = true;

    private double vx, vy;
    private int life = 120; // อยู่ได้สูงสุด 2 วินาที (60 เฟรม/วินาที)

    public Projectile(double centerX, double centerY, double angle, double speed,
                      boolean fromPlayer, int damage) {
        super(centerX - 5, centerY - 5, 10, 10, 1);
        this.vx = Math.cos(angle) * speed;
        this.vy = Math.sin(angle) * speed;
        this.fromPlayer = fromPlayer;
        this.damage = damage;
    }

    /** ถูกสะท้อน: ย้อนกลับตามเส้นทางเดิม กลายเป็นกระสุนศัตรู (speedScale < 1 = ช้าลง หลบง่ายขึ้น) */
    public void reflect(double speedScale) {
        vx = -vx * speedScale;
        vy = -vy * speedScale;
        fromPlayer = false;
        reflected = true;
        life = 120;
    }

    public double getVx() { return vx; }
    public double getVy() { return vy; }

    public void update(Level level) {
        x += vx;
        y += vy;
        life--;

        boolean outOfWorld = x < -width || x > level.worldWidth || y < -200 || y > level.worldHeight;
        boolean hitWall = level.isSolidAt(centerX(), centerY()); // ชนบล็อกตัน (#) แล้วหาย
        if (life <= 0 || outOfWorld || hitWall) {
            active = false;
        }
    }

    public void draw(Graphics2D g2, int cameraX) {
        int sx = (int) x - cameraX;
        if (reflected) { // กระสุนที่ถูกสะท้อน: สีฟ้าขาว มีแสงเรือง
            g2.setColor(new Color(120, 230, 255, 90));
            g2.fillOval(sx - 4, (int) y - 4, width + 8, height + 8);
            g2.setColor(new Color(210, 250, 255));
        } else {
            g2.setColor(fromPlayer ? new Color(255, 220, 60) : new Color(255, 70, 70));
        }
        g2.fillOval(sx, (int) y, width, height);
    }
}
