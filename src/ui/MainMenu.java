package ui;

import main.GamePanel;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;

public class MainMenu {

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
            b.mouseMoved(mx, my);
        }
    }

    public void mousePressed(int mx, int my) {
        for (Button b : buttons) {
            if (b.mousePressed(mx, my)) return;
        }
    }

    // ---------- Draw ----------
    public void draw(Graphics2D g2) {
        // พื้นหลังไล่สี
        g2.setPaint(new GradientPaint(0, 0, new Color(15, 20, 45),
                0, GamePanel.HEIGHT, new Color(40, 25, 70)));
        g2.fillRect(0, 0, GamePanel.WIDTH, GamePanel.HEIGHT);

        // ชื่อเกม
        String title = "PhuYaiBan";
        g2.setFont(new Font("SansSerif", Font.BOLD, 80));
        FontMetrics fm = g2.getFontMetrics();
        int tx = (GamePanel.WIDTH - fm.stringWidth(title)) / 2;
        g2.setColor(new Color(0, 0, 0, 120));
        g2.drawString(title, tx + 4, 154); // เงา
        g2.setColor(new Color(120, 220, 255));
        g2.drawString(title, tx, 150);

        for (Button b : buttons) {
            b.draw(g2);
        }

        // วิธีเล่น
        g2.setFont(new Font("SansSerif", Font.PLAIN, 15));
        g2.setColor(new Color(200, 210, 235));
        String help = "A/D move   Space/W jump   Shift dash   Mouse aim + shoot   M mute";
        g2.drawString(help, (GamePanel.WIDTH - g2.getFontMetrics().stringWidth(help)) / 2, 470);
    }
}
