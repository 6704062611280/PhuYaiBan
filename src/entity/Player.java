package entity;

import input.InputHandler;
import level.Level;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;

public class Player extends Entity {

    private static final double SPEED = 4.5;
    private static final double GRAVITY = 0.6;
    private static final double MAX_FALL_SPEED = 15;

    private static final int SHOOT_COOLDOWN = 12;   // เฟรม (12 = ยิงได้ ~5 นัด/วินาที)
    private static final double BULLET_SPEED = 12;
    private static final int BULLET_DAMAGE = 10;

    private static final int INVINCIBLE_FRAMES = 60; // อมตะหลังโดนตี 1 วินาที

    private double velocityY = 0;
    private int cooldown = 0;
    private int invincibleTimer = 0;
    private double aimAngle = 0;
    public boolean facingRight = true;

    public Player(double x, double y) {
        super(x, y, 40, 60, 100);
    }

    /** โดนตี: ลดเลือด แล้วอมตะชั่วคราว (กันโดนซ้ำทุกเฟรม) */
    public void takeDamage(int amount) {
        if (invincibleTimer > 0 || !isAlive()) return;
        hp = Math.max(0, hp - amount);
        invincibleTimer = INVINCIBLE_FRAMES;
    }

    /** เริ่มด่านใหม่: ย้ายไปจุดเริ่ม ล้างสถานะ และฟื้นเลือดตามที่กำหนด */
    public void startLevel(double startX, double startY, int healAmount) {
        x = startX;
        y = startY;
        velocityY = 0;
        cooldown = 0;
        invincibleTimer = INVINCIBLE_FRAMES; // อมตะสั้น ๆ ตอนเกิด
        hp = Math.min(maxHp, hp + healAmount);
    }

    public void update(InputHandler input, Level level, int cameraX, List<Projectile> projectiles) {
        if (invincibleTimer > 0) invincibleTimer--;

        // เดินซ้าย/ขวา
        if (input.left)  x -= SPEED;
        if (input.right) x += SPEED;

        // ไม่ให้หลุดขอบโลก
        if (x < 0) x = 0;
        if (x + width > level.worldWidth) x = level.worldWidth - width;

        // แรงโน้มถ่วง + พื้น
        velocityY = Math.min(velocityY + GRAVITY, MAX_FALL_SPEED);
        y += velocityY;
        if (y + height >= level.groundY) {
            y = level.groundY - height;
            velocityY = 0;
        }

        // เล็งไปที่เมาส์ (แปลงพิกัดจอ -> พิกัดโลก ด้วยการบวก cameraX)
        double mouseWorldX = input.mouseX + cameraX;
        double mouseWorldY = input.mouseY;
        aimAngle = Math.atan2(mouseWorldY - centerY(), mouseWorldX - centerX());
        facingRight = mouseWorldX >= centerX();

        // ยิง
        if (cooldown > 0) cooldown--;
        if (input.mouseDown && cooldown == 0) {
            projectiles.add(new Projectile(
                    centerX() + Math.cos(aimAngle) * 30,   // เริ่มที่ปลายปืน
                    centerY() + Math.sin(aimAngle) * 30,
                    aimAngle, BULLET_SPEED, true, BULLET_DAMAGE));
            cooldown = SHOOT_COOLDOWN;
        }
    }

    /** วาดที่ตำแหน่งโลก ลบด้วย cameraX = ตำแหน่งบนหน้าจอ */
    public void draw(Graphics2D g2, int cameraX) {
        // ตอนอมตะให้กะพริบ (ข้ามการวาดเป็นช่วง ๆ)
        if (invincibleTimer > 0 && (invincibleTimer / 4) % 2 == 0) return;

        int sx = (int) x - cameraX;
        int sy = (int) y;

        g2.setColor(new Color(40, 90, 200));
        g2.fillRect(sx, sy, width, height);

        // ตา บอกทิศที่หัน
        g2.setColor(Color.WHITE);
        int eyeX = facingRight ? sx + width - 16 : sx + 6;
        g2.fillRect(eyeX, sy + 12, 10, 10);
        g2.setColor(Color.BLACK);
        g2.fillRect(facingRight ? eyeX + 5 : eyeX, sy + 14, 5, 6);

        // ปืน: เส้นจากกลางตัวไปทางเมาส์
        int cx = (int) centerX() - cameraX;
        int cy = (int) centerY();
        g2.setStroke(new BasicStroke(6, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(60, 60, 60));
        g2.drawLine(cx, cy, cx + (int) (Math.cos(aimAngle) * 28), cy + (int) (Math.sin(aimAngle) * 28));
        g2.setStroke(new BasicStroke(1));
    }
}
