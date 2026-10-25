package entity;

import level.Level;

import java.awt.Color;
import java.awt.Graphics2D;

/** กระสุน ใช้ได้ทั้งของผู้เล่นและศัตรู (แยกด้วย fromPlayer) */
public class Projectile extends Entity {

    public final boolean fromPlayer;
    public final int damage;
    public boolean active = true;

    private final double vx, vy;
    private int life = 120; // อยู่ได้สูงสุด 2 วินาที (60 เฟรม/วินาที)

    public Projectile(double centerX, double centerY, double angle, double speed,
                      boolean fromPlayer, int damage) {
        super(centerX - 5, centerY - 5, 10, 10, 1);
        this.vx = Math.cos(angle) * speed;
        this.vy = Math.sin(angle) * speed;
        this.fromPlayer = fromPlayer;
        this.damage = damage;
    }

    public void update(Level level) {
        x += vx;
        y += vy;
        life--;

        boolean outOfWorld = x < -width || x > level.worldWidth || y < -200;
        boolean hitGround = y + height >= level.groundY;
        if (life <= 0 || outOfWorld || hitGround) {
            active = false;
        }
    }

    public void draw(Graphics2D g2, int cameraX) {
        int sx = (int) x - cameraX;
        g2.setColor(fromPlayer ? new Color(255, 220, 60) : new Color(255, 70, 70));
        g2.fillOval(sx, (int) y, width, height);
    }
}
