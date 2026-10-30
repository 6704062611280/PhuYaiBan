package ui;

import audio.Sound;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;

/** ปุ่มกดด้วยเมาส์ ใช้ร่วมกันทั้งหน้าเมนูและหน้า Game Over / Win */
public class Button {
    private final Rectangle bounds;
    private final String label;
    private final Runnable onClick;
    private boolean hovered;

    public Button(int x, int y, int w, int h, String label, Runnable onClick) {
        this.bounds = new Rectangle(x, y, w, h);
        this.label = label;
        this.onClick = onClick;
    }

    public void mouseMoved(int mx, int my) {
        hovered = bounds.contains(mx, my);
    }

    /** คืน true ถ้าคลิกโดนปุ่มนี้ (และสั่งงานแล้ว) */
    public boolean mousePressed(int mx, int my) {
        if (!bounds.contains(mx, my)) return false;
        Sound.play(Sound.Effect.CLICK);
        onClick.run();
        return true;
    }

    public void draw(Graphics2D g2) {
        Rectangle r = bounds;

        g2.setColor(hovered ? new Color(70, 130, 200) : new Color(40, 60, 100));
        g2.fillRoundRect(r.x, r.y, r.width, r.height, 20, 20);

        g2.setStroke(new BasicStroke(3));
        g2.setColor(hovered ? Color.WHITE : new Color(120, 160, 220));
        g2.drawRoundRect(r.x, r.y, r.width, r.height, 20, 20);
        g2.setStroke(new BasicStroke(1));

        g2.setFont(new Font("SansSerif", Font.BOLD, 28));
        FontMetrics fm = g2.getFontMetrics();
        int tx = r.x + (r.width - fm.stringWidth(label)) / 2;
        int ty = r.y + (r.height + fm.getAscent() - fm.getDescent()) / 2;
        g2.setColor(Color.WHITE);
        g2.drawString(label, tx, ty);
    }
}
