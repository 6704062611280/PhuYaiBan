package entity;

import level.Level;
import main.GamePanel;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;

/** คลาสแม่ของศัตรูทุกแบบ (WalkerEnemy, FlyingEnemy, TankEnemy) */
public abstract class Enemy extends Entity {

    public final int contactDamage; // เลือดที่ผู้เล่นเสียเมื่อชนตัวศัตรู
    public final int scoreValue;    // คะแนนที่ได้เมื่อฆ่า
    protected boolean showHealthAlways = false; // true = โชว์หลอดเลือดเหนือหัวตลอด (เช่น แท็งก์)

    public Enemy(double x, double y, int width, int height, int maxHp, int contactDamage, int scoreValue) {
        super(x, y, width, height, maxHp);
        this.contactDamage = contactDamage;
        this.scoreValue = scoreValue;
    }

    /** ดาเมจที่ผู้เล่นเสียเมื่อชนตัวศัตรูเฟรมนี้ (ศัตรูบางตัว เช่น แท็งก์ตอนพุ่ง เปลี่ยนค่านี้ได้) */
    public int getContactDamage() {
        return contactDamage;
    }

    /**
     * กระสุนผู้เล่นกำลังจะโดนตัวศัตรู: ศัตรูที่สะท้อนกระสุนได้ให้ override แล้วคืน true ถ้าสะท้อนไว้
     * (true = กระสุนไม่ทำดาเมจ และถูกเปลี่ยนเป็นกระสุนศัตรูที่พุ่งกลับ)
     */
    public boolean tryReflect(Projectile p) {
        return false;
    }

    /** projectiles ใช้สำหรับศัตรูที่ยิงได้ (ใส่กระสุนเพิ่มเข้าไปในลิสต์นี้) */
    public abstract void update(Player player, Level level, List<Projectile> projectiles);

    /**
     * ศัตรูที่อยู่นอกจอ: ยืนนิ่งรอ ไม่เดิน/บิน/ยิง (ตัวที่ต้องตกตามแรงโน้มถ่วงให้ override)
     * ผู้เล่นเลื่อนกล้องจนศัตรูเข้ามาในจอแล้ว ถึงจะเรียก update() ปกติ
     */
    public void updateOffScreen(Level level) {
        // ค่าเริ่มต้น: ไม่ทำอะไร
    }

    /** ตัวศัตรูอยู่ในกรอบหน้าจอ (แนวนอน) หรือไม่ */
    public boolean isOnScreen(int cameraX) {
        return x + width > cameraX && x < cameraX + GamePanel.WIDTH;
    }

    public abstract void draw(Graphics2D g2, int cameraX);

    /** หลอดเลือดเล็ก ๆ เหนือหัวศัตรู */
    protected void drawHealthBar(Graphics2D g2, int cameraX) {
        if (hp >= maxHp && !showHealthAlways) return; // ยังไม่โดนตี ไม่ต้องแสดง
        int sx = (int) x - cameraX;
        int sy = (int) y - 10;
        g2.setColor(new Color(90, 20, 20));
        g2.fillRect(sx, sy, width, 5);
        g2.setColor(new Color(60, 200, 80));
        g2.fillRect(sx, sy, (int) (width * Math.max(0, (double) hp / maxHp)), 5);
    }
}
