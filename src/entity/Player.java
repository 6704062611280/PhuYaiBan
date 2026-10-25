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
    private static final double JUMP_SPEED = 12.5;      // กระโดดสูงสุด ~130px
    private static final double JUMP_CUT_SPEED = 4;     // ปล่อยปุ่มกลางอากาศ -> ตัดความเร็วขึ้น (กระโดดเตี้ย)

    private static final int DASH_FRAMES = 10;          // แดชนาน 10 เฟรม
    private static final double DASH_SPEED = 14;        // 10 เฟรม x 14 = ~140px
    private static final int DASH_COOLDOWN = 45;        // รอ 0.75 วินาทีก่อนแดชอีกครั้ง

    private static final int SHOOT_COOLDOWN = 12;   // เฟรม (12 = ยิงได้ ~5 นัด/วินาที)
    private static final double BULLET_SPEED = 20;
    private static final int BULLET_DAMAGE = 10;

    private static final int INVINCIBLE_FRAMES = 60; // อมตะหลังโดนตี 1 วินาที

    private double velocityY = 0;
    private boolean onGround = false;
    private int cooldown = 0;
    private int invincibleTimer = 0;
    private int dashTimer = 0;
    private int dashCooldown = 0;
    private int dashDir = 1;
    private double aimAngle = 0;
    public boolean facingRight = true;

    public Player(double x, double y) {
        super(x, y, 40, 60, 100);
    }

    /** ระหว่างแดชหลบการโจมตีได้ (i-frames) */
    public boolean isDashing() {
        return dashTimer > 0;
    }

    /** 0..1 ความพร้อมของแดช (1 = แดชได้) ใช้วาดหลอดบน HUD */
    public double dashReadyRatio() {
        return 1.0 - (double) dashCooldown / DASH_COOLDOWN;
    }

    /** โดนตี: ลดเลือด แล้วอมตะชั่วคราว (กันโดนซ้ำทุกเฟรม) */
    public void takeDamage(int amount) {
        if (invincibleTimer > 0 || dashTimer > 0 || !isAlive()) return;
        hp = Math.max(0, hp - amount);
        invincibleTimer = INVINCIBLE_FRAMES;
    }

    /** เริ่มด่านใหม่: ย้ายไปจุดเริ่ม ล้างสถานะ และฟื้นเลือดตามที่กำหนด */
    public void startLevel(double startX, double startY, int healAmount) {
        x = startX;
        y = startY;
        velocityY = 0;
        onGround = false;
        cooldown = 0;
        dashTimer = 0;
        dashCooldown = 0;
        invincibleTimer = INVINCIBLE_FRAMES; // อมตะสั้น ๆ ตอนเกิด
        hp = Math.min(maxHp, hp + healAmount);
    }

    public void update(InputHandler input, Level level, int cameraX, List<Projectile> projectiles) {
        if (invincibleTimer > 0) invincibleTimer--;
        if (dashCooldown > 0) dashCooldown--;

        // เริ่มแดช (กด Shift): ไปทางที่กดเดินอยู่ ถ้าไม่ได้กดก็ไปทางที่หันหน้า
        if (input.dashPressed) {
            input.dashPressed = false;
            if (dashCooldown == 0 && dashTimer == 0) {
                if (input.left && !input.right)      dashDir = -1;
                else if (input.right && !input.left) dashDir = 1;
                else                                 dashDir = facingRight ? 1 : -1;
                dashTimer = DASH_FRAMES;
                dashCooldown = DASH_COOLDOWN;
            }
        }

        if (dashTimer > 0) {
            // ระหว่างแดช: พุ่งแนวนอนเร็ว ๆ ไม่ตกตามแรงโน้มถ่วง
            x += dashDir * DASH_SPEED;
            velocityY = 0;
            dashTimer--;
        } else {
            // เดินซ้าย/ขวา
            if (input.left)  x -= SPEED;
            if (input.right) x += SPEED;

            // กระโดด (ต้องยืนบนพื้น) + ปล่อยปุ่มเร็ว = กระโดดเตี้ย
            if (input.jump && onGround) {
                velocityY = -JUMP_SPEED;
                onGround = false;
            }
            if (!input.jump && velocityY < -JUMP_CUT_SPEED) {
                velocityY = -JUMP_CUT_SPEED;
            }

            // แรงโน้มถ่วง
            velocityY = Math.min(velocityY + GRAVITY, MAX_FALL_SPEED);
            y += velocityY;
        }

        // ไม่ให้หลุดขอบโลก
        if (x < 0) x = 0;
        if (x + width > level.worldWidth) x = level.worldWidth - width;

        // พื้น
        if (y + height >= level.groundY) {
            y = level.groundY - height;
            velocityY = 0;
            onGround = true;
        } else {
            onGround = false;
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

        // เงาตามหลังตอนแดช
        if (dashTimer > 0) {
            for (int i = 1; i <= 3; i++) {
                g2.setColor(new Color(120, 170, 255, 110 - i * 25));
                g2.fillRect(sx - dashDir * i * 14, sy, width, height);
            }
        }

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
