package main;

import entity.Enemy;
import entity.Player;
import entity.Projectile;
import input.InputHandler;
import level.Level;
import level.Level1;
import level.Level2;
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
import java.awt.Rectangle;
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
    private static final int TOTAL_LEVELS = 2;
    private static final int HEAL_BETWEEN_LEVELS = 50; // ฟื้นเลือดเมื่อผ่านด่าน
    private int levelIndex = 1;
    private int bannerTimer = 0; // นับถอยหลังแสดงข้อความ "LEVEL N" กลางจอ
    // CopyOnWriteArrayList: game thread แก้ลิสต์ ขณะที่ EDT วาด จะไม่เกิด ConcurrentModificationException
    private final List<Projectile> projectiles = new CopyOnWriteArrayList<>();
    private final List<Enemy> enemies = new CopyOnWriteArrayList<>();

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
            }
        });
    }

    public void startGameLoop() {
        gameThread = new Thread(this, "game-loop");
        gameThread.start();
    }

    /** เริ่มเกมใหม่: สร้างด่านและผู้เล่น แล้วเข้าสู่ PLAYING */
    private void startGame() {
        player = new Player(100, 100);
        loadLevel(1, 0);
        setState(GameState.PLAYING);
        requestFocusInWindow(); // ให้คีย์บอร์ดทำงาน
    }

    /** โหลดด่านที่ n: สร้างฉากและศัตรูใหม่ ย้ายผู้เล่นไปจุดเริ่ม */
    private void loadLevel(int n, int heal) {
        levelIndex = n;
        level = (n == 1) ? new Level1() : new Level2();
        projectiles.clear();
        enemies.clear();
        enemies.addAll(level.createEnemies());
        player.startLevel(100, 100, heal); // เริ่มลอยอยู่ จะตกลงพื้นให้เห็นแรงโน้มถ่วง
        input.reset();
        cameraX = 0;
        bannerTimer = 120; // แสดงชื่อด่าน 2 วินาที
    }

    /** ผู้เล่นแตะธงปลายทาง: ไปด่านถัดไป หรือชนะถ้าเป็นด่านสุดท้าย */
    private void finishLevel() {
        if (levelIndex < TOTAL_LEVELS) {
            loadLevel(levelIndex + 1, HEAL_BETWEEN_LEVELS);
        } else {
            input.reset();
            setState(GameState.WIN);
        }
    }

    /** ตรวจการชนทั้งหมดในเฟรมนี้ */
    private void handleCollisions() {
        Rectangle playerBox = player.getBounds();

        // กระสุน
        for (Projectile p : projectiles) {
            if (!p.active) continue;
            Rectangle pBox = p.getBounds();
            if (p.fromPlayer) {
                // กระสุนผู้เล่น -> ศัตรู
                for (Enemy en : enemies) {
                    if (en.isAlive() && pBox.intersects(en.getBounds())) {
                        en.hp -= p.damage;
                        p.active = false;
                        break; // กระสุน 1 นัด โดนศัตรูได้ตัวเดียว
                    }
                }
            } else if (pBox.intersects(playerBox)) {
                // กระสุนศัตรู -> ผู้เล่น
                player.takeDamage(p.damage);
                p.active = false;
            }
        }

        // ตัวศัตรูชนผู้เล่น
        for (Enemy en : enemies) {
            if (en.isAlive() && en.getBounds().intersects(playerBox)) {
                player.takeDamage(en.contactDamage);
            }
        }
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
                for (Enemy en : enemies) {
                    en.update(player, level, projectiles);
                }
                handleCollisions();
                projectiles.removeIf(p -> !p.active);
                enemies.removeIf(en -> !en.isAlive());
                updateCamera();
                if (bannerTimer > 0) bannerTimer--;
                if (!player.isAlive()) {
                    input.reset();
                    setState(GameState.GAME_OVER);
                } else if (player.x + player.width >= level.goalX) {
                    finishLevel();
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
                // หลอดความพร้อมของแดช (เต็ม = แดชได้)
                double dashReady = player.dashReadyRatio();
                g2.setColor(new Color(40, 40, 60));
                g2.fillRect(20, 80, 220, 8);
                g2.setColor(dashReady >= 1 ? new Color(120, 200, 255) : new Color(90, 120, 160));
                g2.fillRect(20, 80, (int) (220 * dashReady), 8);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
                g2.drawString("A/D move   Space/W jump   Shift dash   Mouse aim+shoot   ESC menu", 10, 110);
                g2.setFont(new Font("SansSerif", Font.BOLD, 16));
                g2.drawString(level.getName() + "   Enemies: " + enemies.size()
                        + "   Reach the flag!", 260, 67);
                if (bannerTimer > 0) {
                    drawCenteredText(g2, level.getName(), new Font("SansSerif", Font.BOLD, 72),
                            new Color(255, 255, 255, Math.min(255, bannerTimer * 4)), HEIGHT / 2 - 40);
                }
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
                drawWorld(g2);
                g2.setColor(new Color(0, 0, 0, 170));
                g2.fillRect(0, 0, WIDTH, HEIGHT);
                drawCenteredText(g2, "YOU WIN!", new Font("SansSerif", Font.BOLD, 64),
                        new Color(255, 220, 60), HEIGHT / 2 - 10);
                drawCenteredText(g2, "Press ESC to return to menu", new Font("SansSerif", Font.PLAIN, 22),
                        Color.WHITE, HEIGHT / 2 + 40);
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
        for (Enemy en : enemies) {
            en.draw(g2, cameraX);
        }
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
