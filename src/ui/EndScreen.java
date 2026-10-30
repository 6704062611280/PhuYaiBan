package ui;

import main.GamePanel;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

/** หน้าจอจบเกม ใช้ทั้ง GAME OVER และ YOU WIN (ต่างกันแค่ข้อความและสี) */
public class EndScreen {

    private final Button[] buttons;

    public EndScreen(Runnable onPlayAgain, Runnable onMenu) {
        int w = 280, h = 56;
        int x = (GamePanel.WIDTH - w) / 2;
        buttons = new Button[] {
                new Button(x, 300, w, h, "PLAY AGAIN", onPlayAgain),
                new Button(x, 372, w, h, "MAIN MENU", onMenu)
        };
    }

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

    /** วาดทับฉากเกมที่ค้างอยู่ข้างหลัง */
    public void draw(Graphics2D g2, String title, Color titleColor, String info) {
        g2.setColor(new Color(0, 0, 0, 180));
        g2.fillRect(0, 0, GamePanel.WIDTH, GamePanel.HEIGHT);

        centered(g2, title, new Font("SansSerif", Font.BOLD, 64), titleColor, 190);
        centered(g2, info, new Font("SansSerif", Font.BOLD, 26), Color.WHITE, 250);

        for (Button b : buttons) {
            b.draw(g2);
        }
    }

    private void centered(Graphics2D g2, String text, Font font, Color color, int baselineY) {
        g2.setFont(font);
        g2.setColor(color);
        int w = g2.getFontMetrics().stringWidth(text);
        g2.drawString(text, (GamePanel.WIDTH - w) / 2, baselineY);
    }
}
