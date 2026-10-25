package level;

import entity.Enemy;
import main.GamePanel;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.util.List;

/**
 * คลาสแม่ของด่าน: เก็บขนาดโลก พื้น ธงปลายทาง และสีธีม
 * ด่านจริง (Level1, Level2) สืบทอดไปกำหนดสีและรายการศัตรู
 */
public abstract class Level {
    public final int worldWidth;
    public final int groundY;   // ตำแหน่ง y ของผิวพื้น
    public final int goalX;     // ตำแหน่งธงปลายทาง (ผู้เล่นแตะแล้วผ่านด่าน)

    // ธีมสี (ด่านลูกกำหนดค่าใน constructor)
    protected Color skyTop = new Color(90, 160, 230);
    protected Color skyBottom = new Color(190, 225, 250);
    protected Color hillColor = new Color(110, 140, 180);
    protected Color groundColor = new Color(90, 60, 40);
    protected Color grassColor = new Color(70, 160, 70);
    protected boolean night = false;

    public Level(int worldWidth, int groundY) {
        this.worldWidth = worldWidth;
        this.groundY = groundY;
        this.goalX = worldWidth - 150;
    }

    public abstract String getName();

    /** สร้างศัตรูทั้งหมดของด่านนี้ (เรียกทุกครั้งที่เริ่มด่าน) */
    public abstract List<Enemy> createEnemies();

    public void draw(Graphics2D g2, int cameraX) {
        // ท้องฟ้า (ไม่ขยับ)
        g2.setPaint(new GradientPaint(0, 0, skyTop, 0, groundY, skyBottom));
        g2.fillRect(0, 0, GamePanel.WIDTH, GamePanel.HEIGHT);

        // ดาว (เฉพาะด่านกลางคืน) ตำแหน่งคงที่ คำนวณจากสูตร ไม่สุ่มใหม่ทุกเฟรม
        if (night) {
            g2.setColor(new Color(255, 255, 255, 200));
            for (int i = 0; i < 70; i++) {
                int sx = (i * 137) % GamePanel.WIDTH;
                int sy = (i * 71) % (groundY - 120);
                g2.fillRect(sx, sy, 2, 2);
            }
        }

        // ภูเขาไกล ๆ เลื่อนช้ากว่าพื้น (parallax) ให้ดูมีมิติ
        g2.setColor(hillColor);
        int hillOffset = (int) (cameraX * 0.3);
        for (int i = -1; i < 8; i++) {
            int hx = i * 400 - (hillOffset % 400);
            int[] xs = {hx, hx + 200, hx + 400};
            int[] ys = {groundY, groundY - 180, groundY};
            g2.fillPolygon(xs, ys, 3);
        }

        // พื้น
        g2.setColor(groundColor);
        g2.fillRect(0, groundY, GamePanel.WIDTH, GamePanel.HEIGHT - groundY);
        g2.setColor(grassColor);
        g2.fillRect(0, groundY, GamePanel.WIDTH, 12);

        // เสาบอกระยะทุก 200px (ไว้เห็นว่ากล้องเลื่อนจริง ลบทิ้งได้ตอนขัดเกลา)
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        for (int wx = 0; wx <= worldWidth; wx += 200) {
            int sx = wx - cameraX;
            if (sx < -50 || sx > GamePanel.WIDTH + 50) continue;
            g2.setColor(new Color(60, 40, 25));
            g2.fillRect(sx - 2, groundY - 30, 4, 30);
            g2.setColor(Color.WHITE);
            g2.drawString(String.valueOf(wx), sx - 10, groundY - 36);
        }

        drawGoalFlag(g2, cameraX);
    }

    private void drawGoalFlag(Graphics2D g2, int cameraX) {
        int sx = goalX - cameraX;
        if (sx < -80 || sx > GamePanel.WIDTH + 80) return;
        g2.setColor(new Color(220, 220, 220));
        g2.fillRect(sx - 3, groundY - 140, 6, 140);               // เสา
        g2.setColor(new Color(240, 200, 40));
        g2.fillPolygon(new int[] {sx + 3, sx + 63, sx + 3},       // ผืนธง
                new int[] {groundY - 138, groundY - 114, groundY - 90}, 3);
    }
}
