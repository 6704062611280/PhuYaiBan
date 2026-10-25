package ui;

import main.GamePanel;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;

public class MainMenu {

    /** ปุ่มหนึ่งปุ่ม: ตำแหน่ง ข้อความ และสิ่งที่ทำเมื่อกด */
    private static class Button {
        final Rectangle bounds;
        final String label;
        final Runnable onClick;
        boolean hovered;

        Button(int x, int y, int w, int h, String label, Runnable onClick) {
            this.bounds = new Rectangle(x, y, w, h);
            this.label = label;
            this.onClick = onClick;
        }
    }

    private final Button[] buttons;

    public MainMenu(Runnable onPlay, Runnable onQuit) {
        int w = 240, h = 60;
        int x = (GamePanel.WIDTH - w) / 2;
        buttons = new Button[] {
                new Button(x, 270, w, h, "PLAY", onPlay),
                new Button(x, 350, w, h, "QUIT", onQuit)
        };
    }

    // ---------- Input ----------
    public void mouseMoved(int mx, int my) {
        for (Button b : buttons) {
            b.hovered = b.bounds.contains(mx, my);
        }
    }

    public void mousePressed(int mx, int my) {
        for (Button b : buttons) {
            if (b.bounds.contains(mx, my)) {
                b.onClick.run();
                return;
            }
        }
    }

    // ---------- Draw ----------
    public void draw(Graphics2D g2) {
        // พื้นหลังไล่สี
        g2.setPaint(new GradientPaint(0, 0, new Color(15, 20, 45),
                0, GamePanel.HEIGHT, new Color(40, 25, 70)));
        g2.fillRect(0, 0, GamePanel.WIDTH, GamePanel.HEIGHT);

        // ชื่อเกม
        String title = "MY GAME";
        g2.setFont(new Font("SansSerif", Font.BOLD, 80));
        FontMetrics fm = g2.getFontMetrics();
        int tx = (GamePanel.WIDTH - fm.stringWidth(title)) / 2;
        g2.setColor(new Color(0, 0, 0, 120));
        g2.drawString(title, tx + 4, 154); // เงา
        g2.setColor(new Color(120, 220, 255));
        g2.drawString(title, tx, 150);

        // ปุ่ม
        for (Button b : buttons) {
            drawButton(g2, b);
        }
    }

    private void drawButton(Graphics2D g2, Button b) {
        Rectangle r = b.bounds;

        g2.setColor(b.hovered ? new Color(70, 130, 200) : new Color(40, 60, 100));
        g2.fillRoundRect(r.x, r.y, r.width, r.height, 20, 20);

        g2.setStroke(new BasicStroke(3));
        g2.setColor(b.hovered ? Color.WHITE : new Color(120, 160, 220));
        g2.drawRoundRect(r.x, r.y, r.width, r.height, 20, 20);

        g2.setFont(new Font("SansSerif", Font.BOLD, 30));
        FontMetrics fm = g2.getFontMetrics();
        int tx = r.x + (r.width - fm.stringWidth(b.label)) / 2;
        int ty = r.y + (r.height + fm.getAscent() - fm.getDescent()) / 2;
        g2.setColor(Color.WHITE);
        g2.drawString(b.label, tx, ty);
    }
}
