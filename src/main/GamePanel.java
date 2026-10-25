package main;

import state.GameState;
import ui.MainMenu;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class GamePanel extends JPanel implements Runnable {

    // ---------- ค่าคงที่ ----------
    public static final int WIDTH = 960;
    public static final int HEIGHT = 540;
    private static final int FPS = 60;

    // ---------- สถานะเกม ----------
    private volatile GameState state = GameState.MENU; // volatile: ถูกแก้จาก thread เมาส์/คีย์

    // ---------- game loop ----------
    private Thread gameThread;
    private int currentFps = 0; // แสดงมุมจอไว้ตรวจสอบ

    // ---------- เมนู ----------
    private final MainMenu menu;

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setDoubleBuffered(true);
        setFocusable(true);

        menu = new MainMenu(
                () -> setState(GameState.PLAYING), // Play
                () -> System.exit(0)               // Quit
        );

        // เมาส์: ชี้ (hover) และคลิก
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (state == GameState.MENU) {
                    menu.mouseMoved(e.getX(), e.getY());
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (state == GameState.MENU) {
                    menu.mousePressed(e.getX(), e.getY());
                }
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);

        // ปุ่ม ESC: กลับเมนู (ใช้ทดสอบตอนนี้)
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE && state != GameState.MENU) {
                    setState(GameState.MENU);
                }
            }
        });
    }

    public void startGameLoop() {
        gameThread = new Thread(this, "game-loop");
        gameThread.start();
    }

    public void setState(GameState newState) {
        this.state = newState;
    }

    public GameState getState() {
        return state;
    }

    // ---------- Game Loop (fixed timestep 60 FPS) ----------
    @Override
    public void run() {
        final double drawInterval = 1_000_000_000.0 / FPS;
        double delta = 0;
        long lastTime = System.nanoTime();
        long timer = 0;
        int frames = 0;

        while (gameThread != null) {
            long now = System.nanoTime();
            delta += (now - lastTime) / drawInterval;
            timer += now - lastTime;
            lastTime = now;

            if (delta >= 1) {
                update();
                repaint();
                delta--;
                frames++;
            }

            if (timer >= 1_000_000_000L) {
                currentFps = frames;
                frames = 0;
                timer = 0;
            }

            try {
                Thread.sleep(1); // กัน CPU เต็ม 100%
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    // ---------- อัปเดตตามสถานะ ----------
    private void update() {
        switch (state) {
            case MENU:
                // M2: จัดการเมนู
                break;
            case PLAYING:
                // M3+: อัปเดตผู้เล่น ศัตรู กระสุน
                break;
            case GAME_OVER:
            case WIN:
                break;
        }
    }

    // ---------- วาดตามสถานะ ----------
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        switch (state) {
            case MENU:
                menu.draw(g2);
                break;
            case PLAYING:
                drawPlaceholder(g2, "PLAYING", new Color(20, 60, 40));
                break;
            case GAME_OVER:
                drawPlaceholder(g2, "GAME OVER", new Color(70, 20, 20));
                break;
            case WIN:
                drawPlaceholder(g2, "YOU WIN", new Color(60, 60, 20));
                break;
        }

        // FPS counter (ลบทิ้งได้ตอนจบเกม)
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
        g2.drawString("FPS: " + currentFps, 10, 20);
    }

    // ชั่วคราว: ใช้ตรวจว่าสลับ state ได้ ใน M2 จะแทนด้วยเมนูจริง
    private void drawPlaceholder(Graphics2D g2, String text, Color bg) {
        g2.setColor(bg);
        g2.fillRect(0, 0, WIDTH, HEIGHT);
        g2.setColor(Color.WHITE);
        g2.setFont(new Font("SansSerif", Font.BOLD, 48));
        int textWidth = g2.getFontMetrics().stringWidth(text);
        g2.drawString(text, (WIDTH - textWidth) / 2, HEIGHT / 2);
    }
}
