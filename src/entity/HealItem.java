package entity;

import util.SpriteSheet;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/** ไอเท็มฮีล (หัวใจ) วางในด่านด้วยตัว H ผู้เล่นเดินชนแล้วได้เลือดคืน (เมื่อเลือดไม่เต็ม) */
public class HealItem extends Entity {

    public static final int HEAL_AMOUNT = 30;

    // res/images/heal.png : รูปเดียว 24x24
    private static final SpriteSheet SHEET = SpriteSheet.load("images/heal.png", 24, 24);

    public boolean active = true;
    private int tick;

    /** centerX = กลางตัวแนวนอน, feetY = ตำแหน่งก้นไอเท็ม (ช่อง H ในแผนที่ = ใต้ช่องนั้น) */
    public HealItem(double centerX, double feetY) {
        super(centerX - 12, feetY - 24, 24, 24, 1);
        this.tick = (int) (Math.random() * 60); // ไม่ให้ทุกชิ้นลอยพร้อมกันเป๊ะ
    }

    public void update() {
        tick++;
    }

    /** พื้นที่เก็บ (กว้างกว่าตัวไอเท็มนิดหน่อย เก็บง่ายขึ้น) */
    public Rectangle getPickupBounds() {
        return new Rectangle((int) x - 8, (int) y - 12, width + 16, height + 16);
    }

    public void draw(Graphics2D g2, int cameraX) {
        int sx = (int) x - cameraX;
        int sy = (int) y + (int) Math.round(Math.sin(tick * 0.08) * 3 - 3); // ลอยขึ้นลงเบา ๆ

        // แสงเรืองรอบ ๆ
        int glow = 30 + (int) (Math.sin(tick * 0.1) * 8);
        g2.setColor(new Color(255, 120, 150, 55));
        g2.fillOval(sx + 12 - glow / 2, sy + 12 - glow / 2, glow, glow);

        if (SHEET != null) {
            SHEET.draw(g2, 0, sx, sy, width, height, true);
        } else {
            g2.setColor(new Color(238, 58, 82));
            g2.fillOval(sx + 1, sy + 3, 12, 12);
            g2.fillOval(sx + 11, sy + 3, 12, 12);
            g2.fillPolygon(new int[] {sx + 2, sx + 22, sx + 12}, new int[] {sy + 11, sy + 11, sy + 23}, 3);
        }
    }
}
