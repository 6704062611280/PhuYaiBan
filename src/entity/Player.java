package entity;

import audio.Sound;
import input.InputHandler;
import level.Level;
import level.Physics;
import util.SpriteSheet;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.util.List;

public class Player extends Entity {

    // res/images/player.png : แผ่นเฟรม 40x60 เรียงแนวนอน (รูปหันขวา ไม่ต้องวาดปืนในรูป เกมวาดปืนหมุนตามเมาส์ให้)
    //   เฟรม 0-1 = ยืนนิ่ง, 2-5 = วิ่ง, 6 = กระโดด/ลอยกลางอากาศ
    private static final SpriteSheet SHEET = SpriteSheet.load("images/player.png", 40, 60);

    private static final double SPEED = 4.5;
    private static final double GRAVITY = 0.6;
    private static final double MAX_FALL_SPEED = 15;
    private static final double JUMP_SPEED = 12.5;      // กระโดดสูงสุด ~130px
    private static final double JUMP_CUT_SPEED = 4;     // ปล่อยปุ่มกลางอากาศ -> ตัดความเร็วขึ้น (กระโดดเตี้ย)

    private static final int DASH_FRAMES = 10;          // แดชนาน 10 เฟรม
    private static final double DASH_SPEED = 14;        // 10 เฟรม x 14 = ~140px
    private static final int DASH_COOLDOWN = 45;        // รอ 0.75 วินาทีก่อนแดชอีกครั้ง

    private static final int SHOOT_COOLDOWN = 12;   // เฟรม (12 = ยิงได้ ~5 นัด/วินาที)
    private static final double BULLET_SPEED = 12;
    private static final int BULLET_DAMAGE = 10;

    private static final int INVINCIBLE_FRAMES = 60; // อมตะหลังโดนตี 1 วินาที

    private double velocityY = 0;
    private boolean onGround = false;
    private boolean moving = false;
    private int animTick = 0;
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
        Sound.play(Sound.Effect.HURT);
    }

    /** ฟื้นเลือด (ไม่เกินเลือดสูงสุด) */
    public void heal(int amount) {
        if (!isAlive()) return;
        hp = Math.min(maxHp, hp + amount);
    }

    /** ตายทันที (เช่น ตกหลุม) ไม่สนอมตะ/แดช */
    public void kill() {
        hp = 0;
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
        animTick++;
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
                Sound.play(Sound.Effect.DASH);
            }
        }

        // ----- แนวนอน: แดช หรือ เดินปกติ (ชนบล็อกข้าง ๆ ได้) -----
        boolean dashing = dashTimer > 0;
        double dx = 0;
        if (dashing) {
            dx = dashDir * DASH_SPEED;
            dashTimer--;
        } else {
            if (input.left)  dx -= SPEED;
            if (input.right) dx += SPEED;
        }
        moving = dx != 0;
        Physics.moveX(this, dx, level);

        // ไม่ให้หลุดขอบโลก
        if (x < 0) x = 0;
        if (x + width > level.worldWidth) x = level.worldWidth - width;

        // ----- แนวตั้ง: กระโดด + แรงโน้มถ่วง + ลงจอดบนบล็อก/แพลตฟอร์ม -----
        if (dashing) {
            velocityY = 0; // ระหว่างแดชลอยตรง ไม่ตกตามแรงโน้มถ่วง
        } else {
            // กระโดด (ต้องยืนบนพื้น) + ปล่อยปุ่มเร็ว = กระโดดเตี้ย
            if (input.jump && onGround) {
                velocityY = -JUMP_SPEED;
                onGround = false;
                Sound.play(Sound.Effect.JUMP);
            }
            if (!input.jump && velocityY < -JUMP_CUT_SPEED) {
                velocityY = -JUMP_CUT_SPEED;
            }

            velocityY = Math.min(velocityY + GRAVITY, MAX_FALL_SPEED);
            int hit = Physics.moveY(this, velocityY, level);
            if (hit == Physics.LANDED) {
                velocityY = 0;
                onGround = true;
            } else {
                onGround = false;
                if (hit == Physics.HIT_CEILING) velocityY = 0;
            }
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
            Sound.play(Sound.Effect.SHOOT);
        }
    }

    /** เลือกเฟรมอนิเมชันตามสถานะ: 0-1 ยืน, 2-5 วิ่ง, 6 กระโดด */
    private int currentFrame() {
        if (dashTimer > 0) return 2;                       // แดช: ท่าก้าวยาว
        if (!onGround) return 6;                           // ลอยกลางอากาศ
        if (moving) return 2 + (animTick / 6) % 4;         // วิ่ง เปลี่ยนเฟรมทุก 6 เฟรม
        return (animTick / 30) % 2;                        // ยืนหายใจ ทุก 0.5 วินาที
    }

    /** วาดที่ตำแหน่งโลก ลบด้วย cameraX = ตำแหน่งบนหน้าจอ */
    public void draw(Graphics2D g2, int cameraX) {
        // ตอนอมตะให้กะพริบ (ข้ามการวาดเป็นช่วง ๆ)
        if (invincibleTimer > 0 && (invincibleTimer / 4) % 2 == 0) return;

        int sx = (int) x - cameraX;
        int sy = (int) y;
        int frame = currentFrame();

        // เงาตามหลังตอนแดช
        if (dashTimer > 0) {
            Composite old = g2.getComposite();
            for (int i = 3; i >= 1; i--) {
                float alpha = 0.45f - i * 0.1f;
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                int gx = sx - dashDir * i * 14;
                if (SHEET != null) {
                    SHEET.draw(g2, frame, gx, sy, width, height, facingRight);
                } else {
                    g2.setColor(new Color(120, 170, 255));
                    g2.fillRect(gx, sy, width, height);
                }
            }
            g2.setComposite(old);
        }

        if (SHEET != null) {
            SHEET.draw(g2, frame, sx, sy, width, height, facingRight);
        } else {
            g2.setColor(new Color(40, 90, 200));
            g2.fillRect(sx, sy, width, height);

            // ตา บอกทิศที่หัน
            g2.setColor(Color.WHITE);
            int eyeX = facingRight ? sx + width - 16 : sx + 6;
            g2.fillRect(eyeX, sy + 12, 10, 10);
            g2.setColor(Color.BLACK);
            g2.fillRect(facingRight ? eyeX + 5 : eyeX, sy + 14, 5, 6);
        }

        drawGun(g2, (int) centerX() - cameraX, (int) centerY());
    }

    /** ปืนหมุนตามเมาส์ (ถ้าเล็งไปทางซ้ายจะพลิกแนวตั้ง ด้ามปืนจะได้ไม่คว่ำ) */
    private void drawGun(Graphics2D g2, int cx, int cy) {
        AffineTransform old = g2.getTransform();
        g2.translate(cx, cy);
        g2.rotate(aimAngle);
        if (Math.cos(aimAngle) < 0) g2.scale(1, -1);

        g2.setColor(new Color(16, 18, 36));            // ขอบดำ
        g2.fillRect(-1, -6, 33, 11);
        g2.fillRect(2, 3, 8, 10);
        g2.setColor(new Color(58, 63, 82));            // ตัวปืน
        g2.fillRect(0, -5, 19, 8);
        g2.setColor(new Color(36, 40, 56));            // ด้าม
        g2.fillRect(3, 3, 6, 9);
        g2.setColor(new Color(112, 120, 146));         // ลำกล้อง
        g2.fillRect(19, -4, 10, 6);
        g2.setColor(new Color(150, 160, 190));         // แสงบนตัวปืน
        g2.fillRect(2, -5, 15, 2);
        g2.setColor(new Color(255, 210, 58));          // ปากกระบอก
        g2.fillRect(29, -4, 2, 6);

        g2.setTransform(old);
    }
}
