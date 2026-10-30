package entity;

import audio.Sound;
import level.Level;
import level.Physics;
import util.SpriteSheet;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;

/**
 * ศัตรูที่เดินตรงเข้าหาผู้เล่น
 * - ชนบล็อกได้ และกระโดดข้ามบล็อกเตี้ย ๆ ได้
 * - ถ้าผู้เล่นอยู่สูงกว่า (เช่น ยืนบนแพลตฟอร์ม) จะพยายามกระโดดขึ้นไปหา
 * - ไม่เดินตกหลุมที่ไม่มีพื้นรองรับด้วยตัวเอง (แต่เดินลงจากที่สูงลงพื้นข้างล่างได้)
 * - สกิลสะท้อนกระสุน (เปิด/ปิดได้ที่ walker_reflect ในไฟล์ด่าน):
 *     เห็นกระสุนของผู้เล่นพุ่งเข้าใส่ด้านหน้า -> ยกโล่ (มีดีเลย์เล็กน้อย) -> กระสุนที่โดนโล่ถูกสะท้อนกลับไปหาผู้เล่น
 *     จุดอ่อน: ยิงใกล้ ๆ จนโล่ยกไม่ทัน / ยิงตอนโล่ลง (มีช่วงพัก) / ยิงจากด้านหลัง (ตอนยกโล่จะไม่หันตามผู้เล่น)
 */
public class WalkerEnemy extends GroundEnemy {

    // res/images/walker.png : แผ่นเฟรม 40x50 เรียงแนวนอน  เฟรม 0-1 = เดิน (รูปหันขวา)
    private static final SpriteSheet SHEET = SpriteSheet.load("images/walker.png", 40, 50);

    private static final double STEP_JUMP_SPEED = 9.5;    // กระโดดข้ามบล็อกสูง 1 ช่อง (~70px)
    private static final double MAX_JUMP_SPEED = 13.5;    // กระโดดแรงสุด (สูงได้ราว 145px = 3 ช่อง)
    private static final double JUMP_EXTRA_HEIGHT = 30;   // กระโดดให้สูงกว่าเท้าผู้เล่นอีกนิด จะได้พ้นขอบแพลตฟอร์ม
    private static final double JUMP_TRIGGER_DY = 24;     // ผู้เล่นต้องสูงกว่าอย่างน้อยเท่านี้ถึงจะกระโดดหา
    private static final double JUMP_RANGE_X = 120;       // กระโดดหาเมื่อห่างแนวนอนไม่เกินเท่านี้
    private static final int JUMP_COOLDOWN = 40;          // เฟรมที่ต้องรอก่อนกระโดดอีกครั้ง

    // ----- สกิลสะท้อนกระสุน -----
    private enum Shield { DOWN, RAISING, UP }

    private static final double SHIELD_SEE_DIST = 280;    // เห็นกระสุนที่พุ่งเข้าหาในระยะนี้ (กระสุนผู้เล่นเร็ว 12px/เฟรม = ~23 เฟรมก่อนถึงตัว)
    private static final int SHIELD_RAISE_FRAMES = 6;     // ดีเลย์ยกโล่ (ยิงใกล้กว่า ~70px จะยกไม่ทัน)
    private static final int SHIELD_UP_FRAMES = 40;       // โล่ค้างอยู่นาน (0.67 วินาที)
    private static final int SHIELD_COOLDOWN_BASE = 150;  // พักก่อนยกโล่ครั้งต่อไป (ด่านยากพักสั้นลง) = ช่วงที่ยิงเข้าตัวได้
    private static final double SHIELD_FRONT_SLOPE = 1.73;// โล่ครอบคลุมกระสุนที่มาจากด้านหน้า ภายใน 60° จากแนวนอน (ยิงเฉียงสูงชันทะลุได้)
    private static final double REFLECT_SPEED_SCALE = 0.7;// กระสุนที่สะท้อนกลับช้าลง 30% (หลบง่ายขึ้น)
    private static final double SHIELD_WALK_FACTOR = 0.35;// ตอนยกโล่เดินช้าลง
    private static final double SHIELD_RADIUS = 34;

    private final double speed;
    private final boolean reflectEnabled;
    private final int shieldCooldownFrames;

    private int jumpCooldown = 0;
    private int animTick = 0;
    private boolean moving = false;

    private Shield shieldState = Shield.DOWN;
    private int shieldTimer = 0;
    private int shieldCooldown = 0;
    private int reflectFlash = 0; // นับถอยหลังแสงวาบตอนสะท้อนสำเร็จ

    /** ศัตรูเดินธรรมดา ไม่มีสกิลสะท้อนกระสุน */
    public WalkerEnemy(double centerX, double feetY, double difficulty) {
        this(centerX, feetY, difficulty, false);
    }

    /**
     * centerX = กลางตัวแนวนอน, feetY = ตำแหน่งเท้า (ช่อง W ในแผนที่ = ใต้ช่องนั้น)
     * difficulty = ตัวคูณความยากของด่าน (1.0 = ปกติ) มีผลกับเลือด ความเร็ว ดาเมจ และความถี่การยกโล่
     * reflect = true เปิดสกิลสะท้อนกระสุน
     */
    public WalkerEnemy(double centerX, double feetY, double difficulty, boolean reflect) {
        super(centerX - 20, feetY - 50, 40, 50,
                (int) Math.round(30 * difficulty),   // เลือด (ด่าน 1 = 30 = โดน 3 นัด)
                (int) Math.round(15 * difficulty),   // ดาเมจตอนชน
                100);                                // คะแนนเมื่อฆ่า
        this.speed = 1.8 * difficulty;
        this.reflectEnabled = reflect;
        this.shieldCooldownFrames = (int) Math.max(60, SHIELD_COOLDOWN_BASE / difficulty);
        this.shieldCooldown = 45; // ตอนเกิดยังไม่ยกโล่ทันที
    }

    public boolean canReflect() {
        return reflectEnabled;
    }

    /** โล่ยกเต็มที่และสะท้อนกระสุนได้อยู่ */
    public boolean isShieldUp() {
        return shieldState == Shield.UP;
    }

    // ---------------------------------------------------------------
    //  สกิลสะท้อนกระสุน
    // ---------------------------------------------------------------

    /** กระสุนมาจากด้านที่ตัวศัตรูหันหน้าอยู่ (และไม่เฉียงสูงชันเกินไป) */
    private boolean comesFromFront(Projectile p) {
        double vx = p.getVx(), vy = p.getVy();
        int dir = facingRight ? 1 : -1;
        if (vx * dir >= 0) return false;                       // ต้องวิ่งสวนทางกับทิศที่หัน = พุ่งเข้าหาหน้า
        return Math.abs(vy) <= Math.abs(vx) * SHIELD_FRONT_SLOPE;
    }

    /** กระสุนผู้เล่นลูกนี้กำลังพุ่งมาโดนตัวเรา (เข้าทางด้านหน้า ภายในระยะมองเห็น) */
    private boolean bulletThreatens(Projectile p) {
        if (!p.fromPlayer || !p.active || !comesFromFront(p)) return false;

        double len = Math.hypot(p.getVx(), p.getVy());
        if (len < 0.01) return false;
        double rx = centerX() - p.centerX();
        double ry = centerY() - p.centerY();

        double along = (rx * p.getVx() + ry * p.getVy()) / len;   // บวก = พุ่งเข้าหาเรา
        if (along <= 0 || along > SHIELD_SEE_DIST) return false;

        double cross = (p.getVx() * ry - p.getVy() * rx) / len;   // ระยะตั้งฉากจากเส้นทางกระสุน
        return Math.abs(cross) <= height / 2.0 + 14;              // เส้นทางผ่านตัวเรา = มีโอกาสโดน
    }

    private void updateShield(List<Projectile> projectiles) {
        if (reflectFlash > 0) reflectFlash--;
        if (shieldCooldown > 0) shieldCooldown--;

        switch (shieldState) {
            case DOWN:
                if (shieldCooldown == 0) {
                    for (Projectile p : projectiles) {
                        if (bulletThreatens(p)) {
                            shieldState = Shield.RAISING;
                            shieldTimer = SHIELD_RAISE_FRAMES;
                            break;
                        }
                    }
                }
                break;
            case RAISING:
                if (--shieldTimer <= 0) {
                    shieldState = Shield.UP;
                    shieldTimer = SHIELD_UP_FRAMES;
                }
                break;
            case UP:
                if (--shieldTimer <= 0) {
                    shieldState = Shield.DOWN;
                    shieldCooldown = shieldCooldownFrames;
                }
                break;
        }
    }

    /** GamePanel เรียกตอนกระสุนผู้เล่นชนตัวศัตรู: โล่ยกอยู่และกระสุนมาจากด้านหน้า -> สะท้อนกลับ ไม่เสียเลือด */
    @Override
    public boolean tryReflect(Projectile p) {
        if (!reflectEnabled || shieldState != Shield.UP || !comesFromFront(p)) return false;

        p.reflect(REFLECT_SPEED_SCALE);
        // ย้ายกระสุนมาอยู่ที่ผิวโล่ด้านหน้า แล้วพุ่งกลับตามเส้นทางเดิม
        int dir = facingRight ? 1 : -1;
        p.x = centerX() + dir * (width / 2.0 + 6) - p.width / 2.0;
        reflectFlash = 10;
        Sound.play(Sound.Effect.REFLECT);
        return true;
    }

    // ---------------------------------------------------------------
    //  การเคลื่อนที่
    // ---------------------------------------------------------------

    @Override
    public void update(Player player, Level level, List<Projectile> projectiles) {
        if (fellOutOfWorld(level)) return;
        animTick++;
        if (jumpCooldown > 0) jumpCooldown--;
        if (reflectEnabled) updateShield(projectiles);

        boolean shielding = shieldState != Shield.DOWN;

        // เดินเข้าหาผู้เล่น (ตอนยกโล่ไม่หันตามผู้เล่น = ล็อกทิศไว้ จึงมีจุดอ่อนด้านหลัง)
        double dx = player.centerX() - centerX();
        double vx = 0;
        if (!shielding) {
            if (Math.abs(dx) > 2) { // กันสั่นเวลาทับตำแหน่งผู้เล่นพอดี
                facingRight = dx > 0;
                vx = facingRight ? speed : -speed;
            }
        } else {
            boolean playerAhead = facingRight ? dx > 2 : dx < -2;
            if (playerAhead) vx = (facingRight ? 1 : -1) * speed * SHIELD_WALK_FACTOR;
        }

        // ไม่เดินตกหลุมนอกโลกเอง (หยุดที่ขอบ)
        if (vx != 0 && wouldWalkIntoVoid(level, vx)) {
            vx = 0;
        }
        moving = vx != 0;

        double before = x;
        Physics.moveX(this, vx, level);
        x = Math.max(0, Math.min(x, level.worldWidth - width)); // ไม่หลุดขอบโลก
        boolean blocked = vx != 0 && Math.abs(x - before) < Math.abs(vx) - 0.01;

        // กระโดด
        if (onGround && jumpCooldown == 0) {
            boolean jumped = false;

            // ผู้เล่นอยู่สูงกว่าและอยู่ใกล้พอ -> กระโดดขึ้นไปหา (คำนวณแรงให้พอดีกับความสูง)
            double heightAbove = (y + height) - (player.y + player.height); // บวก = ผู้เล่นสูงกว่า
            if (heightAbove > JUMP_TRIGGER_DY && Math.abs(dx) < JUMP_RANGE_X) {
                double needed = heightAbove + JUMP_EXTRA_HEIGHT;
                double jumpSpeed = Math.sqrt(2 * GRAVITY * needed) + 0.5; // +0.5 ชดเชยการคำนวณทีละเฟรม
                if (jumpSpeed <= MAX_JUMP_SPEED) {
                    jump(jumpSpeed);
                    jumpCooldown = JUMP_COOLDOWN;
                    jumped = true;
                }
                // สูงเกินกว่าจะกระโดดถึง: ไม่กระโดดเสียแรง ยืนรอใต้ผู้เล่น
            }

            // ถูกบล็อกขวางอยู่ -> กระโดดข้าม (ได้ถ้าบล็อกสูงไม่เกิน 1 ช่อง)
            if (!jumped && blocked) {
                jump(STEP_JUMP_SPEED);
                jumpCooldown = JUMP_COOLDOWN;
            }
        }

        applyGravity(level);
    }

    /** นอกจอ: เก็บโล่ลง (ไม่ค้างท่าไว้) แล้วยืนรอ ตกตามแรงโน้มถ่วงอย่างเดียว */
    @Override
    public void updateOffScreen(Level level) {
        shieldState = Shield.DOWN;
        reflectFlash = 0;
        super.updateOffScreen(level);
    }

    // ---------------------------------------------------------------
    //  วาด
    // ---------------------------------------------------------------

    @Override
    public void draw(Graphics2D g2, int cameraX) {
        int sx = (int) x - cameraX;
        int sy = (int) y;

        if (SHEET != null) {
            int frame = (moving && onGround) ? (animTick / 8) % 2 : 0;
            SHEET.draw(g2, frame, sx, sy, width, height, facingRight);
        } else {
            g2.setColor(new Color(190, 50, 50));
            g2.fillRect(sx, sy, width, height);

            // ตา (หันไปทางผู้เล่น)
            g2.setColor(Color.WHITE);
            int eyeX = facingRight ? sx + width - 18 : sx + 6;
            g2.fillRect(eyeX, sy + 10, 12, 10);
            g2.setColor(Color.BLACK);
            g2.fillRect(facingRight ? eyeX + 6 : eyeX, sy + 12, 6, 6);
        }

        if (reflectEnabled && shieldState != Shield.DOWN) {
            drawShield(g2, sx + width / 2, sy + height / 2);
        }
        drawHealthBar(g2, cameraX);
    }

    /** โล่เป็นส่วนโค้งครึ่งวงกลมด้านหน้า: ตอนกำลังยก = จาง ๆ, ยกเต็ม = สว่าง, สะท้อนสำเร็จ = วาบสีขาว */
    private void drawShield(Graphics2D g2, int cx, int cy) {
        boolean up = shieldState == Shield.UP;
        boolean flash = reflectFlash > 0;
        int start = facingRight ? -70 : 110;
        int r = (int) SHIELD_RADIUS;

        // กำลังจะหมดเวลา: กะพริบเตือน
        boolean fading = up && shieldTimer < 10 && (shieldTimer / 2) % 2 == 0;

        int fillAlpha = !up ? 40 : (flash ? 190 : (fading ? 60 : 105));
        Color base = flash ? new Color(255, 255, 255) : new Color(110, 225, 255);
        g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), fillAlpha));
        g2.fillArc(cx - r, cy - r, r * 2, r * 2, start, 140);

        g2.setStroke(new BasicStroke(up ? 3 : 1.5f));
        g2.setColor(new Color(base.getRed(), base.getGreen(), base.getBlue(), up ? 230 : 110));
        g2.drawArc(cx - r, cy - r, r * 2, r * 2, start, 140);
        g2.setStroke(new BasicStroke(1));
    }
}
