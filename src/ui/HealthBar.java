package ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;

/** หลอดเลือด วาดติดมุมจอ (ไม่ขยับตามกล้อง) */
public class HealthBar {

    public static void draw(Graphics2D g2, int x, int y, int width, int height, int hp, int maxHp) {
        double ratio = Math.max(0, Math.min(1.0, (double) hp / maxHp));

        // พื้นหลังสีแดงเข้ม = เลือดที่หายไป
        g2.setColor(new Color(90, 20, 20));
        g2.fillRoundRect(x, y, width, height, 10, 10);

        // เลือดที่เหลือ: เขียว -> เหลือง -> แดง ตามปริมาณ
        Color fill = ratio > 0.5 ? new Color(60, 200, 80)
                   : ratio > 0.25 ? new Color(230, 190, 40)
                   : new Color(230, 60, 50);
        g2.setColor(fill);
        g2.fillRoundRect(x, y, (int) (width * ratio), height, 10, 10);

        // ขอบ
        g2.setStroke(new BasicStroke(2));
        g2.setColor(Color.WHITE);
        g2.drawRoundRect(x, y, width, height, 10, 10);
        g2.setStroke(new BasicStroke(1));

        // ตัวเลข
        String text = "HP " + Math.max(0, hp) + " / " + maxHp;
        g2.setFont(new Font("SansSerif", Font.BOLD, 13));
        FontMetrics fm = g2.getFontMetrics();
        int tx = x + (width - fm.stringWidth(text)) / 2;
        int ty = y + (height + fm.getAscent() - fm.getDescent()) / 2;
        g2.setColor(Color.BLACK);
        g2.drawString(text, tx + 1, ty + 1);
        g2.setColor(Color.WHITE);
        g2.drawString(text, tx, ty);
    }
}
