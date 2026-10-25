package main;

import entity.Player;
import entity.Projectile;
import input.InputHandler;
import level.Level;
import state.GameState;
import ui.HealthBar;
import ui.MainMenu;

import javax.swing.JPanel;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
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

    // ---------- ในเกม ----------
    private final InputHandler input = new InputHandler();
    private volatile Level level;
    private volatile Player player;
    private int cameraX = 0;
    // CopyOnWriteArrayList: game thread แก้ลิสต์ ขณะที่ EDT วาด จะไม่เกิด ConcurrentModificationException
    private final List<Projectile> projectiles = new CopyOnWriteArrayList<>();

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setDoubleBuffered(true);
        setFocusable(true);

        menu = new MainMenu(
                this::startGame,                   // Play
                () -> System.exit(0)               // Quit
        );

        // เมาส์: ชี้ (hover) และคลิก
        MouseAdapter mouse = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                trackMouse(e);
            }

            @Override
            public void mouseDragged(MouseEvent e) { // ขยับเมาส์ตอนกดค้าง
                trackMouse(e);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                trackMouse(e);
                if (state == GameState.MENU) {
                    menu.mousePressed(e.getX(), e.getY());
                } else if (state == GameState.PLAYING) {
                    input.mouseDown = true;
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                input.mouseDown = false;
            }

            private void trackMouse(MouseEvent e) {
                input.mouseX = e.getX();
                input.mouseY = e.getY();
                if (state == GameState.MENU) {
                    menu.mouseMoved(e.getX(), e.getY());
                }
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);

        addKeyListener(input);

        // ปุ่ม ESC: กลับเมนู (ใช้ทดสอบตอนนี้)
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE && state != GameState.MENU) {
                    setState(GameState.MENU);
                }
                // ชั่วคราว (ลบใน M6): กด H = โดนตี 20 เพื่อทดสอบหลอดเลือด
                if (e.getKeyCode() == KeyEvent.VK_H && state == GameState.PLAYING) {
                    player.takeDamage(20);
                }
            }
        });
    }

    public void startGameLoop() {
        gameThread = new Thread(this, "game-loop");
        gameThread.start();
    }

    /** เริ่มเกมใหม่: สร้างด่านและผู้เล่น แล้วเข้าสู่ PLAYING */
    private void startGame() {
        level = new Level(3000, 440);
        player = new Player(100, 100); // เริ่มลอยอยู่ จะตกลงพื้นให้เห็นแรงโน้มถ่วง
        input.reset();
        projectiles.clear();
        cameraX = 0;
        setState(GameState.PLAYING);
        requestFocusInWindow(); // ให้คีย์บอร์ดทำงาน
    }

    /** กล้องตามผู้เล่น (ให้ผู้เล่นอยู่กลางจอ) แต่ไม่เลยขอบโลก */
    private void updateCamera() {
        cameraX = (int) player.centerX() - WIDTH / 2;
        cameraX = Math.max(0, Math.min(cameraX, level.worldWidth - WIDTH));
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
                player.update(input, level, cameraX, projectiles);
                for (Projectile p : projectiles) {
                    p.update(level);
                }
                projectiles.removeIf(p -> !p.active);
                updateCamera();
                // M6+: ศัตรู การชน
                if (!player.isAlive()) {
                    input.reset();
                    setState(GameState.GAME_OVER);
                }
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
                drawWorld(g2);
                drawCrosshair(g2);
                HealthBar.draw(g2, 20, 50, 220, 24, player.hp, player.maxHp); // HUD ติดจอ
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
                g2.drawString("A/D or Arrows = move   Mouse = aim/shoot   H = test damage   ESC = menu", 10, 40 + 55);
                break;
            case GAME_OVER:
                drawWorld(g2);                      // ฉากค้างไว้ข้างหลัง
                g2.setColor(new Color(0, 0, 0, 170));
                g2.fillRect(0, 0, WIDTH, HEIGHT);
                drawCenteredText(g2, "GAME OVER", new Font("SansSerif", Font.BOLD, 64),
                        new Color(255, 80, 80), HEIGHT / 2 - 10);
                drawCenteredText(g2, "Press ESC to return to menu", new Font("SansSerif", Font.PLAIN, 22),
                        Color.WHITE, HEIGHT / 2 + 40);
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

    /** วาดโลกเกม (ด่าน กระสุน ผู้เล่น) ใช้ซ้ำทั้งตอนเล่นและตอน Game Over */
    private void drawWorld(Graphics2D g2) {
        level.draw(g2, cameraX);
        for (Projectile p : projectiles) {
            p.draw(g2, cameraX);
        }
        player.draw(g2, cameraX);
    }

    private void drawCenteredText(Graphics2D g2, String text, Font font, Color color, int baselineY) {
        g2.setFont(font);
        g2.setColor(color);
        int w = g2.getFontMetrics().stringWidth(text);
        g2.drawString(text, (WIDTH - w) / 2, baselineY);
    }

    private void drawCrosshair(Graphics2D g2) {
        int mx = input.mouseX, my = input.mouseY;
        g2.setColor(new Color(255, 255, 255, 220));
        g2.drawOval(mx - 8, my - 8, 16, 16);
        g2.drawLine(mx - 12, my, mx + 12, my);
        g2.drawLine(mx, my - 12, mx, my + 12);
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
