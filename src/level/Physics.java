package level;

import entity.Entity;

/**
 * การชนระหว่างตัวละครกับช่องในแผนที่ (บล็อก # และแพลตฟอร์มบาง =)
 * วิธีการ: ขยับทีละแกน (X แล้วค่อย Y) แล้วดันตัวละครออกจากบล็อกที่ทับอยู่
 */
public final class Physics {

    public static final int NONE = 0;
    public static final int LANDED = 1;        // ตกลงมาเหยียบพื้น/แพลตฟอร์ม
    public static final int HIT_CEILING = 2;   // เด้งหัวชนเพดาน

    private static final double EPS = 0.001;

    private Physics() {}

    /** ขยับแนวนอน dx ชนบล็อกตัน (#) แล้วหยุดชิดบล็อก (แพลตฟอร์มบางไม่ขวางด้านข้าง) */
    public static void moveX(Entity e, double dx, Level level) {
        if (dx == 0) return;
        final int T = Level.TILE;
        e.x += dx;

        int topRow = (int) Math.floor(e.y / T);
        int bottomRow = (int) Math.floor((e.y + e.height - EPS) / T);

        if (dx > 0) {
            int col = (int) Math.floor((e.x + e.width - EPS) / T);
            for (int r = topRow; r <= bottomRow; r++) {
                if (level.isSolid(col, r)) {
                    e.x = col * T - e.width;
                    return;
                }
            }
        } else {
            int col = (int) Math.floor(e.x / T);
            for (int r = topRow; r <= bottomRow; r++) {
                if (level.isSolid(col, r)) {
                    e.x = (col + 1) * T;
                    return;
                }
            }
        }
    }

    /**
     * ขยับแนวตั้ง dy (บวก = ลง)
     * @return LANDED / HIT_CEILING / NONE ผู้เรียกใช้ผลนี้ตั้ง velocityY และ onGround เอง
     */
    public static int moveY(Entity e, double dy, Level level) {
        if (dy == 0) return NONE;
        final int T = Level.TILE;
        double oldBottom = e.y + e.height;
        e.y += dy;

        int leftCol = (int) Math.floor(e.x / T);
        int rightCol = (int) Math.floor((e.x + e.width - EPS) / T);

        if (dy > 0) {
            // ตกลง: ดูช่องที่ฝ่าเท้าอยู่
            double newBottom = e.y + e.height;
            int row = (int) Math.floor(newBottom / T);
            for (int c = leftCol; c <= rightCol; c++) {
                boolean solid = level.isSolid(c, row);
                // แพลตฟอร์มบาง: ลงจอดได้เฉพาะตอนที่เท้าอยู่เหนือผิวบนมาก่อน (ไม่ใช่ทะลุขึ้นมาจากข้างล่าง)
                boolean oneWay = level.isOneWay(c, row) && oldBottom <= row * T + 0.01;
                if (solid || oneWay) {
                    e.y = row * T - e.height;
                    return LANDED;
                }
            }
        } else {
            // ขึ้น: ดูช่องที่หัวอยู่ (เฉพาะบล็อกตัน)
            int row = (int) Math.floor(e.y / T);
            for (int c = leftCol; c <= rightCol; c++) {
                if (level.isSolid(c, row)) {
                    e.y = (row + 1) * T;
                    return HIT_CEILING;
                }
            }
        }
        return NONE;
    }
}
