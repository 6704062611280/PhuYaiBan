package level;

import main.GamePanel;

import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;

/** โลกของด่านหนึ่ง: ความกว้าง พื้น และพื้นหลัง (M8 จะแยกเป็น Level1 / Level2) */
public class Level {
    public final int worldWidth;
    public final int groundY;   // ตำแหน่ง y ของผิวพื้น

    public Level(int worldWidth, int groundY) {
        this.worldWidth = worldWidth;
        this.groundY = groundY;
    }

    public void draw(Graphics2D g2, int cameraX) {
        // ท้องฟ้า (ไม่ขยับ)
        g2.setPaint(new GradientPaint(0, 0, new Color(90, 160, 230),
                0, groundY, new Color(190, 225, 250)));
        g2.fillRect(0, 0, GamePanel.WIDTH, GamePanel.HEIGHT);

        // ภูเขาไกล ๆ เลื่อนช้ากว่าพื้น (parallax) ให้ดูมีมิติ
        g2.setColor(new Color(110, 140, 180));
        int hillOffset = (int) (cameraX * 0.3);
        for (int i = -1; i < 8; i++) {
            int hx = i * 400 - (hillOffset % 400);
            int[] xs = {hx, hx + 200, hx + 400};
            int[] ys = {groundY, groundY - 180, groundY};
            g2.fillPolygon(xs, ys, 3);
        }

        // พื้น
        g2.setColor(new Color(90, 60, 40));
        g2.fillRect(0, groundY, GamePanel.WIDTH, GamePanel.HEIGHT - groundY);
        g2.setColor(new Color(70, 160, 70));
        g2.fillRect(0, groundY, GamePanel.WIDTH, 12);

        // เสาบอกระยะทุก 200px (ไว้เห็นว่ากล้องเลื่อนจริง ลบทิ้งได้ภายหลัง)
        g2.setFont(new Font("SansSerif", Font.PLAIN, 12));
        for (int wx = 0; wx <= worldWidth; wx += 200) {
            int sx = wx - cameraX;
            if (sx < -50 || sx > GamePanel.WIDTH + 50) continue;
            g2.setColor(new Color(60, 40, 25));
            g2.fillRect(sx - 2, groundY - 30, 4, 30);
            g2.setColor(Color.WHITE);
            g2.drawString(String.valueOf(wx), sx - 10, groundY - 36);
        }
    }
}
