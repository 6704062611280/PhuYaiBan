package entity;

import level.Level;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;

/** ศัตรูบิน: ลอยเหนือพื้น รักษาระยะห่างจากผู้เล่น และยิงกระสุนเล็งมาที่ผู้เล่น */
public class FlyingEnemy extends Enemy {

    private static final double SPEED = 1.5;
    private static final double TOO_FAR = 380;      // ไกลกว่านี้ -> บินเข้าหา
    private static final double TOO_CLOSE = 240;    // ใกล้กว่านี้ -> บินถอย
    private static final double SHOOT_RANGE = 650;  // ยิงเฉพาะเมื่อผู้เล่นอยู่ในระยะนี้
    private static final int SHOOT_INTERVAL = 120;  // 120 เฟรม = ยิงทุก 2 วินาที
    private static final double BULLET_SPEED = 5;
    private static final int BULLET_DAMAGE = 10;

    private final double baseY;   // ความสูงที่ลอย
    private int tick = 0;         // ตัวนับเฟรม ใช้ทำการโบกปีกและลอยขึ้นลง
    private int shootTimer;
    private boolean facingRight = false;

    public FlyingEnemy(double x, double baseY) {
        super(x, baseY, 44, 30, 20, 10); // hp 20 = โดน 2 นัด, ชนแล้วเสีย 10
        this.baseY = baseY;
        // สุ่มเวลายิงนัดแรก ไม่ให้ศัตรูหลายตัวยิงพร้อมกันเป๊ะ
        this.shootTimer = 60 + (int) (Math.random() * 60);
    }

    @Override
    public void update(Player player, Level level, List<Projectile> projectiles) {
        tick++;

        // เคลื่อนที่แนวนอน: รักษาระยะ
        double dx = player.centerX() - centerX();
        facingRight = dx > 0;
        double dist = Math.abs(dx);
        if (dist > TOO_FAR) {
            x += facingRight ? SPEED : -SPEED;
        } else if (dist < TOO_CLOSE) {
            x += facingRight ? -SPEED : SPEED;
        }
        x = Math.max(0, Math.min(x, level.worldWidth - width));

        // ลอยขึ้นลงเป็นคลื่น (sin)
        y = baseY + Math.sin(tick * 0.05) * 20;

        // ยิง
        double dy = player.centerY() - centerY();
        double realDist = Math.hypot(dx, dy);
        if (shootTimer > 0) shootTimer--;
        if (shootTimer == 0 && realDist <= SHOOT_RANGE) {
            double angle = Math.atan2(dy, dx); // เล็งไปที่ผู้เล่น (สูตรเดียวกับ M4)
            projectiles.add(new Projectile(centerX(), centerY(), angle,
                    BULLET_SPEED, false, BULLET_DAMAGE));
            shootTimer = SHOOT_INTERVAL;
        }
    }

    @Override
    public void draw(Graphics2D g2, int cameraX) {
        int sx = (int) x - cameraX;
        int sy = (int) y;

        // ปีก โบกขึ้นลง
        int flap = (int) (Math.sin(tick * 0.3) * 10);
        g2.setColor(new Color(150, 110, 200));
        g2.fillPolygon(
                new int[] {sx + 6, sx + width / 2, sx + width / 2 - 8},
                new int[] {sy + 10, sy + 10, sy - 6 - flap}, 3);
        g2.fillPolygon(
                new int[] {sx + width - 6, sx + width / 2, sx + width / 2 + 8},
                new int[] {sy + 10, sy + 10, sy - 6 - flap}, 3);

        // ตัว
        g2.setColor(new Color(110, 60, 170));
        g2.fillOval(sx, sy, width, height);

        // ตา
        g2.setColor(Color.WHITE);
        int eyeX = facingRight ? sx + width - 20 : sx + 8;
        g2.fillOval(eyeX, sy + 8, 12, 12);
        g2.setColor(Color.RED);
        g2.fillOval(facingRight ? eyeX + 6 : eyeX + 1, sy + 11, 5, 6);

        drawHealthBar(g2, cameraX);
    }
}
