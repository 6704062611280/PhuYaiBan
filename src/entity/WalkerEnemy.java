package entity;

import level.Level;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;

/** ศัตรูที่เดินตรงเข้าหาผู้เล่น */
public class WalkerEnemy extends Enemy {

    

    private boolean facingRight = false;

    /** วางบนพื้นที่ groundY โดยอัตโนมัติ */
    private final double speed;

public WalkerEnemy(double x, int groundY, double difficulty) {
    super(x, groundY - 50, 40, 50,
          (int) (30 * difficulty),    // เลือด
          (int) (15 * difficulty));   // ดาเมจตอนชน
    this.speed = 1.8 * difficulty;    // ความเร็วเดิน
}

    @Override
    public void update(Player player, Level level, List<Projectile> projectiles) {
        double dx = player.centerX() - centerX();
        if (Math.abs(dx) > 2) { // กันสั่นเวลาทับตำแหน่งผู้เล่นพอดี
            facingRight = dx > 0;
            x += facingRight ? speed : -speed;
        }
        // ไม่ให้หลุดขอบโลก
        x = Math.max(0, Math.min(x, level.worldWidth - width));
    }

    @Override
    public void draw(Graphics2D g2, int cameraX) {
        int sx = (int) x - cameraX;
        int sy = (int) y;

        g2.setColor(new Color(190, 50, 50));
        g2.fillRect(sx, sy, width, height);

        // ตา (หันไปทางผู้เล่น)
        g2.setColor(Color.WHITE);
        int eyeX = facingRight ? sx + width - 18 : sx + 6;
        g2.fillRect(eyeX, sy + 10, 12, 10);
        g2.setColor(Color.BLACK);
        g2.fillRect(facingRight ? eyeX + 6 : eyeX, sy + 12, 6, 6);

        drawHealthBar(g2, cameraX);
    }
}
