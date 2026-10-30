package entity;

import level.Level;
import level.Physics;

/**
 * ศัตรูที่เดินบนพื้น: มีแรงโน้มถ่วง ชนบล็อก และเช็กว่าก้าวต่อไปจะตกหลุมหรือไม่
 * (WalkerEnemy และ TankEnemy สืบทอดจากคลาสนี้)
 */
public abstract class GroundEnemy extends Enemy {

    protected static final double GRAVITY = 0.6;
    protected static final double MAX_FALL_SPEED = 15;

    protected double velocityY = 0;
    protected boolean onGround = false;
    protected boolean facingRight = false;

    public GroundEnemy(double x, double y, int width, int height, int maxHp, int contactDamage, int scoreValue) {
        super(x, y, width, height, maxHp, contactDamage, scoreValue);
    }

    /** หลุดลงนอกโลกไปแล้ว = หายไป (ไม่ได้คะแนน) คืน true ถ้าหายไปแล้ว */
    protected boolean fellOutOfWorld(Level level) {
        if (y > level.worldHeight + 100) {
            hp = 0;
            return true;
        }
        return false;
    }

    protected void jump(double jumpSpeed) {
        velocityY = -jumpSpeed;
        onGround = false;
    }

    protected void applyGravity(Level level) {
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

    /**
     * ก้าวถัดไปจะทำให้ขอบหน้าของตัวไปอยู่เหนือ "ที่ว่างเปล่าไม่มีพื้นเลยทั้งแนวลงไป" (หลุม) หรือไม่
     * ถ้าข้างล่างยังมีพื้น/แพลตฟอร์มรองรับอยู่ไม่ว่าจะลึกแค่ไหน ถือว่าปลอดภัย (เดินลงจากที่สูงได้)
     */
    protected boolean wouldWalkIntoVoid(Level level, double vx) {
        final int T = Level.TILE;
        double edgeX = vx > 0 ? x + width + vx : x + vx; // จุดหน้าสุดหลังก้าวถัดไป
        int col = (int) Math.floor(edgeX / T);
        int startRow = (int) Math.floor((y + height + 0.5) / T); // ช่องใต้เท้าลงไป
        int rows = level.worldHeight / T;
        for (int r = startRow; r < rows; r++) {
            if (level.isSolid(col, r) || level.isOneWay(col, r)) {
                return false; // มีพื้นรองรับ
            }
        }
        return true;
    }

    /** นอกจอ: ไม่เดิน แต่ยังตกตามแรงโน้มถ่วง (กันลอยค้างกลางอากาศ) */
    @Override
    public void updateOffScreen(Level level) {
        if (fellOutOfWorld(level)) return;
        applyGravity(level);
    }
}
