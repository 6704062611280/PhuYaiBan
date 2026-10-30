package entity;

import level.Level;
import level.Physics;
import util.SpriteSheet;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.util.List;

/**
 * ศัตรูแท็งก์: ตัวใหญ่ อึด (เลือดเยอะ) เดินช้า แต่พุ่งชนแรงมาก
 *
 * วงจร:  WALK (เดินช้า ๆ เข้าหา)
 *     -> WINDUP (หยุด สั่น มีเครื่องหมาย ! ให้ผู้เล่นรู้ตัว และหลบทัน)
 *     -> CHARGE (พุ่งตรงไปทางที่ล็อกไว้ เร็วและเจ็บมาก)
 *     -> RECOVER (มึนงง ขยับไม่ได้ = จังหวะโต้กลับ ถ้าพุ่งชนกำแพงจะมึนนานกว่า)
 * ไม่เดิน/พุ่งตกหลุมเอง และพุ่งไม่ข้ามกำแพง
 */
public class TankEnemy extends GroundEnemy {

    // res/images/tank.png : แผ่นเฟรม 56x70  เฟรม 0-1 = เดิน, 2 = เตรียมพุ่ง, 3 = พุ่ง (รูปหันขวา)
    private static final SpriteSheet SHEET = SpriteSheet.load("images/tank.png", 56, 70);

    private enum State { WALK, WINDUP, CHARGE, RECOVER }

    private static final int WINDUP_FRAMES = 45;          // เตรียมพุ่ง 0.75 วินาที (เวลาให้ผู้เล่นตอบสนอง)
    private static final int CHARGE_MAX_FRAMES = 50;      // พุ่งนานสุด
    private static final double CHARGE_SPEED = 7.5;       // พุ่งเร็วกว่าเดินของผู้เล่น (4.5) แต่แดช (14) หนีทัน
    private static final int RECOVER_FRAMES = 70;         // มึนหลังพุ่งปกติ
    private static final int CRASH_RECOVER_FRAMES = 110;  // มึนหลังพุ่งชนกำแพง/ขอบหลุม
    private static final int WALK_COOLDOWN = 100;         // เดินอย่างน้อยเท่านี้ก่อนพุ่งครั้งต่อไป
    private static final double TRIGGER_DIST = 360;       // เริ่มเตรียมพุ่งเมื่อผู้เล่นอยู่ในระยะแนวนอนนี้
    private static final double TRIGGER_HEIGHT_DIFF = 80; // และอยู่ระดับความสูงใกล้เคียงกัน

    private final double walkSpeed;
    private final int chargeDamage;

    private State state = State.WALK;
    private int stateTimer = 0;
    private int chargeCooldown = 60; // ตอนเริ่มยังไม่พุ่งทันที
    private int chargeDir = 1;
    private int animTick = 0;
    private boolean moving = false;

    /**
     * centerX = กลางตัวแนวนอน, feetY = ตำแหน่งเท้า (ช่อง T ในแผนที่ = ใต้ช่องนั้น)
     * difficulty = ตัวคูณความยากของด่าน (1.0 = ปกติ)
     */
    public TankEnemy(double centerX, double feetY, double difficulty) {
        super(centerX - 28, feetY - 70, 56, 70,
                (int) Math.round(120 * difficulty),  // เลือด 120 = โดน 12 นัด
                (int) Math.round(15 * difficulty),   // ดาเมจเมื่อชนตัวตอนเดินปกติ
                300);                                // คะแนนเมื่อฆ่า
        this.walkSpeed = 0.9 * difficulty;
        this.chargeDamage = (int) Math.round(30 * difficulty);
        this.showHealthAlways = true;
    }

    /** ดาเมจตอนชนตัว: ตอนพุ่งแรงกว่าเดินปกติ 2 เท่า */
    @Override
    public int getContactDamage() {
        return state == State.CHARGE ? chargeDamage : contactDamage;
    }

    public boolean isCharging() {
        return state == State.CHARGE;
    }

    public boolean isWindingUp() {
        return state == State.WINDUP;
    }

    public boolean isRecovering() {
        return state == State.RECOVER;
    }

    @Override
    public void update(Player player, Level level, List<Projectile> projectiles) {
        if (fellOutOfWorld(level)) return;
        animTick++;
        if (chargeCooldown > 0) chargeCooldown--;

        double dx = player.centerX() - centerX();
        moving = false;

        switch (state) {
            case WALK: {
                double vx = 0;
                if (Math.abs(dx) > 2) {
                    facingRight = dx > 0;
                    vx = facingRight ? walkSpeed : -walkSpeed;
                }
                if (vx != 0 && wouldWalkIntoVoid(level, vx)) vx = 0; // ไม่เดินตกหลุมเอง
                moving = vx != 0;
                Physics.moveX(this, vx, level);
                x = Math.max(0, Math.min(x, level.worldWidth - width));

                // ผู้เล่นอยู่ในระยะและระดับเดียวกัน -> เตรียมพุ่ง
                double heightDiff = Math.abs((y + height) - (player.y + player.height));
                if (onGround && chargeCooldown == 0
                        && Math.abs(dx) < TRIGGER_DIST && heightDiff < TRIGGER_HEIGHT_DIFF) {
                    facingRight = dx > 0;
                    chargeDir = facingRight ? 1 : -1; // ล็อกทิศตอนนี้เลย (ผู้เล่นหลบข้างได้)
                    state = State.WINDUP;
                    stateTimer = WINDUP_FRAMES;
                }
                break;
            }
            case WINDUP:
                if (--stateTimer <= 0) {
                    state = State.CHARGE;
                    stateTimer = CHARGE_MAX_FRAMES;
                }
                break;

            case CHARGE: {
                double vx = chargeDir * CHARGE_SPEED;
                if (wouldWalkIntoVoid(level, vx)) {
                    startRecover(CRASH_RECOVER_FRAMES); // หยุดที่ขอบหลุม
                    break;
                }
                double before = x;
                Physics.moveX(this, vx, level);
                x = Math.max(0, Math.min(x, level.worldWidth - width));
                boolean crashed = Math.abs(x - before) < Math.abs(vx) - 0.01;
                if (crashed) {
                    startRecover(CRASH_RECOVER_FRAMES); // ชนกำแพง มึนนาน
                } else if (--stateTimer <= 0) {
                    startRecover(RECOVER_FRAMES);
                }
                break;
            }
            case RECOVER:
                if (--stateTimer <= 0) {
                    state = State.WALK;
                    chargeCooldown = WALK_COOLDOWN;
                }
                break;
        }

        applyGravity(level);
    }

    private void startRecover(int frames) {
        state = State.RECOVER;
        stateTimer = frames;
    }

    /** นอกจอ: ยกเลิกท่าที่ค้างอยู่ ยืนรอ (ตกตามแรงโน้มถ่วงอย่างเดียว) */
    @Override
    public void updateOffScreen(Level level) {
        if (state == State.WINDUP || state == State.CHARGE) {
            state = State.WALK;
            chargeCooldown = Math.max(chargeCooldown, 60);
        }
        super.updateOffScreen(level);
    }

    @Override
    public void draw(Graphics2D g2, int cameraX) {
        int sx = (int) x - cameraX;
        int sy = (int) y;

        // ตอนเตรียมพุ่งตัวสั่น
        if (state == State.WINDUP) sx += (stateTimer % 2 == 0) ? 2 : -2;

        // เส้นความเร็วตามหลังตอนพุ่ง
        if (state == State.CHARGE) {
            g2.setStroke(new BasicStroke(3));
            for (int i = 0; i < 4; i++) {
                int ly = sy + 12 + i * 15;
                int tail = 22 + (animTick * 7 + i * 5) % 24;
                int bx = chargeDir > 0 ? sx - 4 : sx + width + 4;
                g2.setColor(new Color(255, 255, 255, 150));
                g2.drawLine(bx, ly, bx - chargeDir * tail, ly);
            }
            g2.setStroke(new BasicStroke(1));
        }

        if (SHEET != null) {
            int frame;
            switch (state) {
                case WINDUP:  frame = 2; break;
                case CHARGE:  frame = 3; break;
                case RECOVER: frame = 0; break;
                default:      frame = moving ? (animTick / 14) % 2 : 0;
            }
            SHEET.draw(g2, frame, sx, sy, width, height, facingRight);
        } else {
            drawFallback(g2, sx, sy);
        }

        // เครื่องหมายเตือน
        if (state == State.WINDUP) {
            drawWarning(g2, sx + width / 2, sy - 40);
        } else if (state == State.RECOVER) {
            drawDizzy(g2, sx + width / 2, sy - 20);
        }

        drawHealthBarAt(g2, sx, sy);
    }

    private void drawFallback(Graphics2D g2, int sx, int sy) {
        g2.setColor(state == State.CHARGE ? new Color(230, 90, 40) : new Color(120, 128, 140));
        g2.fillRect(sx, sy, width, height);
        g2.setColor(new Color(232, 137, 43));
        g2.fillRect(sx, sy + 10, width, 14);
        g2.setColor(state == State.WINDUP ? new Color(255, 240, 110) : new Color(255, 48, 48));
        int eyeX = facingRight ? sx + width - 22 : sx + 8;
        g2.fillRect(eyeX, sy + 28, 14, 8);
    }

    /** เครื่องหมาย ! กะพริบ */
    private void drawWarning(Graphics2D g2, int cx, int baseY) {
        if ((stateTimer / 5) % 2 == 1) return;
        g2.setColor(new Color(255, 214, 40));
        g2.fillPolygon(new int[] {cx - 14, cx + 14, cx}, new int[] {baseY + 22, baseY + 22, baseY - 6}, 3);
        g2.setColor(new Color(40, 20, 0));
        g2.setFont(new Font("SansSerif", Font.BOLD, 20));
        FontMetrics fm = g2.getFontMetrics();
        g2.drawString("!", cx - fm.stringWidth("!") / 2, baseY + 19);
    }

    /** ดาวหมุนเหนือหัวตอนมึน */
    private void drawDizzy(Graphics2D g2, int cx, int cy) {
        g2.setColor(new Color(255, 230, 80));
        for (int i = 0; i < 3; i++) {
            double a = animTick * 0.15 + i * (Math.PI * 2 / 3);
            int px = cx + (int) (Math.cos(a) * 16);
            int py = cy + (int) (Math.sin(a) * 5);
            g2.fillOval(px - 3, py - 3, 7, 7);
        }
    }

    /** หลอดเลือดแท็งก์ (กว้างกว่าตัวเล็กน้อย) ตามตำแหน่งที่ตัวสั่นอยู่ */
    private void drawHealthBarAt(Graphics2D g2, int sx, int sy) {
        int barY = sy - 10;
        g2.setColor(new Color(90, 20, 20));
        g2.fillRect(sx, barY, width, 6);
        g2.setColor(new Color(235, 170, 50));
        g2.fillRect(sx, barY, (int) (width * Math.max(0, (double) hp / maxHp)), 6);
        g2.setColor(new Color(0, 0, 0, 160));
        g2.drawRect(sx, barY, width, 6);
    }
}
