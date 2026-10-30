package entity;

import level.Level;
import util.SpriteSheet;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;

/**
 * ศัตรูบิน: ลอยเหนือพื้น รักษาระยะห่างจากผู้เล่น และยิงกระสุนเล็งมาที่ผู้เล่น
 * เคลื่อนที่ให้ยิงยาก: ส่ายซ้ายขวา ลอยขึ้นลงไม่เป็นจังหวะ และหลบกระสุนที่พุ่งเข้าหาตัว
 * (หลบได้ทีละครั้ง มีช่วงพักก่อนหลบอีกครั้ง ยิงรัว ๆ ยังโดนได้)
 */
public class FlyingEnemy extends Enemy {

    // res/images/flyer.png : แผ่นเฟรม 44x30  เฟรม 0 = ปีกขึ้น, 1 = ปีกลง (รูปหันขวา)
    private static final SpriteSheet SHEET = SpriteSheet.load("images/flyer.png", 44, 30);

    private static final double SPEED = 1.5;
    private static final double TOO_FAR = 380;      // ไกลกว่านี้ -> บินเข้าหา
    private static final double TOO_CLOSE = 240;    // ใกล้กว่านี้ -> บินถอย
    private static final double SHOOT_RANGE = 650;  // ยิงเฉพาะเมื่อผู้เล่นอยู่ในระยะนี้

    private static final double STRAFE_SPEED = 1.4;     // ความเร็วส่ายซ้ายขวา
    private static final double DODGE_SPEED = 4.5;      // ความเร็วตอนหลบ
    private static final int DODGE_FRAMES = 14;         // หลบนานกี่เฟรม (~60px)
    private static final double DODGE_SEE_DIST = 230;   // เห็นกระสุนที่พุ่งเข้าหาในระยะนี้
    private static final double DODGE_HIT_WIDTH = 38;   // เส้นทางกระสุนผ่านใกล้ตัวกว่านี้ = มีโอกาสโดน -> หลบ
    private static final double MAX_DODGE_OFFSET = 110; // หลบออกจากความสูงปกติได้ไกลสุดเท่านี้

    private final double homeY;           // ความสูงปกติที่ลอย
    private final double phase;           // สุ่มจังหวะการส่าย ให้แต่ละตัวไม่ขยับพร้อมกัน
    private final int shootInterval;      // เฟรมระหว่างการยิงแต่ละนัด
    private final double bulletSpeed;
    private final int bulletDamage;
    private final int dodgeRestFrames;    // พักก่อนหลบอีกครั้ง (ด่านยากพักสั้นลง)

    private int tick = 0;                 // ตัวนับเฟรม ใช้ทำการโบกปีกและลอยขึ้นลง
    private int shootTimer;
    private boolean facingRight = false;

    private double dodgeOffsetY = 0;      // ระยะที่เบี่ยงจากความสูงปกติ (ค่อย ๆ กลับมาเอง)
    private double dodgeVx = 0, dodgeVy = 0;
    private int dodgeTimer = 0;
    private int dodgeCooldown = 0;

    /**
     * centerX, centerY = ตำแหน่งกลางตัวที่ลอยอยู่ (ช่อง F ในแผนที่)
     * difficulty = ตัวคูณความยากของด่าน (1.0 = ปกติ): ยิงถี่ขึ้น กระสุนเร็วขึ้น เจ็บขึ้น หลบถี่ขึ้น
     */
    public FlyingEnemy(double centerX, double centerY, double difficulty) {
        super(centerX - 22, centerY - 15, 44, 30,
                (int) Math.round(20 * difficulty),   // เลือด (ด่าน 1 = 20 = โดน 2 นัด)
                10,                                  // ดาเมจตอนชนตัว
                150);                                // คะแนนเมื่อฆ่า
        this.homeY = centerY - 15;
        this.phase = Math.random() * 100;
        this.shootInterval = (int) (120 / difficulty);                    // 120 เฟรม = ทุก 2 วินาที
        this.bulletSpeed = 5 * (1 + (difficulty - 1) * 0.5);
        this.bulletDamage = (int) Math.round(10 * difficulty);
        this.dodgeRestFrames = (int) (60 / difficulty);
        // สุ่มเวลายิงนัดแรก ไม่ให้ศัตรูหลายตัวยิงพร้อมกันเป๊ะ
        this.shootTimer = 60 + (int) (Math.random() * 60);
    }

    @Override
    public void update(Player player, Level level, List<Projectile> projectiles) {
        tick++;
        if (dodgeCooldown > 0) dodgeCooldown--;

        double dx = player.centerX() - centerX();
        facingRight = dx > 0;
        double dist = Math.abs(dx);

        // เคลื่อนที่แนวนอน: รักษาระยะ + ส่ายซ้ายขวาไม่ให้เล็งง่าย
        double move = Math.cos(tick * 0.045 + phase) * STRAFE_SPEED;
        if (dist > TOO_FAR) {
            move += facingRight ? SPEED : -SPEED;
        } else if (dist < TOO_CLOSE) {
            move += facingRight ? -SPEED : SPEED;
        }
        x += move;

        // หลบกระสุนที่พุ่งเข้าหา
        if (dodgeTimer == 0 && dodgeCooldown == 0) {
            startDodgeIfThreatened(projectiles, level);
        }
        if (dodgeTimer > 0) {
            x += dodgeVx;
            dodgeOffsetY = Math.max(-MAX_DODGE_OFFSET, Math.min(MAX_DODGE_OFFSET, dodgeOffsetY + dodgeVy));
            dodgeTimer--;
        } else {
            dodgeOffsetY *= 0.97; // ค่อย ๆ ลอยกลับสู่ความสูงเดิม
        }
        x = Math.max(0, Math.min(x, level.worldWidth - width));

        // ลอยขึ้นลงเป็นคลื่นสองความถี่ซ้อนกัน (คาดเดาจังหวะยากขึ้น)
        double wobble = Math.sin(tick * 0.05 + phase) * 20 + Math.sin(tick * 0.13 + phase * 2) * 10;
        y = Math.max(10, homeY + dodgeOffsetY + wobble);

        // ยิง
        double dy = player.centerY() - centerY();
        double realDist = Math.hypot(dx, dy);
        if (shootTimer > 0) shootTimer--;
        if (shootTimer == 0 && realDist <= SHOOT_RANGE) {
            double angle = Math.atan2(dy, dx); // เล็งไปที่ผู้เล่น (สูตรเดียวกับ M4)
            projectiles.add(new Projectile(centerX(), centerY(), angle,
                    bulletSpeed, false, bulletDamage));
            shootTimer = shootInterval;
        }
    }

    /**
     * มองหากระสุนของผู้เล่นที่กำลังพุ่งเข้าหาตัว ถ้าเส้นทางจะผ่านใกล้พอให้โดน -> พุ่งหลบไปทางตั้งฉากกับเส้นทางกระสุน
     */
    private void startDodgeIfThreatened(List<Projectile> projectiles, Level level) {
        for (Projectile p : projectiles) {
            if (!p.fromPlayer || !p.active) continue;

            double vx = p.getVx(), vy = p.getVy();
            double len = Math.hypot(vx, vy);
            if (len < 0.01) continue;

            double rx = centerX() - p.centerX();   // จากกระสุนมาถึงเรา
            double ry = centerY() - p.centerY();

            double along = (rx * vx + ry * vy) / len;       // บวก = กระสุนกำลังพุ่งเข้าหาเรา
            if (along <= 0 || along > DODGE_SEE_DIST) continue;

            double cross = (vx * ry - vy * rx) / len;       // ระยะตั้งฉากจากเส้นทางกระสุน (มีเครื่องหมาย บอกว่าเราอยู่ด้านไหน)
            if (Math.abs(cross) > DODGE_HIT_WIDTH) continue; // กระสุนผ่านไปไม่โดนอยู่แล้ว

            // ทิศหลบ = ตั้งฉากกับกระสุน ไปด้านที่เราอยู่ (ห่างจากเส้นทางมากขึ้น)
            double side = cross >= 0 ? 1 : -1;
            if (Math.abs(cross) < 1) side = Math.random() < 0.5 ? 1 : -1;
            double nx = -vy / len * side;
            double ny = vx / len * side;

            // ไม่หลบลงไปชนพื้น/บล็อก: ถ้าข้างล่างมีบล็อกให้กลับทิศเป็นหลบขึ้น
            if (ny > 0 && level.isSolidAt(centerX() + nx * 60, centerY() + ny * 60 + 20)) {
                nx = -nx;
                ny = -ny;
            }

            dodgeVx = nx * DODGE_SPEED;
            dodgeVy = ny * DODGE_SPEED;
            dodgeTimer = DODGE_FRAMES;
            dodgeCooldown = dodgeRestFrames;
            return;
        }
    }

    @Override
    public void draw(Graphics2D g2, int cameraX) {
        int sx = (int) x - cameraX;
        int sy = (int) y;

        if (SHEET != null) {
            int frame = (tick / (dodgeTimer > 0 ? 3 : 7)) % 2; // หลบอยู่ = กระพือเร็วขึ้น
            SHEET.draw(g2, frame, sx, sy, width, height, facingRight);
        } else {
            // ปีก โบกขึ้นลง (โบกเร็วขึ้นตอนหลบ)
            int flap = (int) (Math.sin(tick * (dodgeTimer > 0 ? 0.7 : 0.3)) * 10);
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
        }

        drawHealthBar(g2, cameraX);
    }
}
