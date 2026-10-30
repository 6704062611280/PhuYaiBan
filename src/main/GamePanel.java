package main;

import audio.Sound;
import entity.Enemy;
import entity.HealItem;
import entity.Player;
import entity.Projectile;
import input.InputHandler;
import level.Level;
import state.GameState;
import ui.EndScreen;
import ui.HealthBar;
import ui.MainMenu;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GamePanel extends JPanel implements Runnable {

    private static final long serialVersionUID = 1L;

    // ---------- ค่าคงที่ ----------
    public static final int WIDTH = 960;
    public static final int HEIGHT = 540;
    private static final int FPS = 60;

    // รายชื่อไฟล์ด่านใน res/levels/ ตามลำดับ (เพิ่มด่านใหม่: สร้างไฟล์แล้วเติมชื่อตรงนี้)
    private static final String[] LEVEL_FILES = {"level1.txt", "level2.txt", "level3.txt", "level4.txt", "level5.txt"};
    private static final int TOTAL_LEVELS = LEVEL_FILES.length;
    private static final int HEAL_BETWEEN_LEVELS = 50; // ฟื้นเลือดเมื่อผ่านด่าน
    private static final int LEVEL_CLEAR_BONUS = 500;  // คะแนนโบนัสเมื่อผ่านด่าน

    // ---------- สถานะเกม ----------
    private volatile GameState state = GameState.MENU; // volatile: ถูกแก้จาก thread เมาส์/คีย์

    // ---------- game loop ----------
    private Thread gameThread;
    private int currentFps = 0;
    private boolean showFps = false; // กด F3 เพื่อเปิด/ปิด
    private volatile long volumeShownUntil = 0; // โชว์ระดับเสียงชั่วคราวหลังกด - หรือ =

    // ---------- หน้าจอ UI ----------
    private final MainMenu menu;
    private final EndScreen endScreen;

    // ---------- ในเกม ----------
    private final InputHandler input = new InputHandler();
    private volatile Level level;
    private volatile Player player;
    private int cameraX = 0;
    private int levelIndex = 1;
    private int bannerTimer = 0; // นับถอยหลังแสดงข้อความ "LEVEL N" กลางจอ
    private volatile int score = 0;
    private volatile boolean reloadRequested = false; // กด F5 = โหลดไฟล์ด่านปัจจุบันใหม่
    // CopyOnWriteArrayList: game thread แก้ลิสต์ ขณะที่ EDT วาด จะไม่เกิด ConcurrentModificationException
    private final List<Projectile> projectiles = new CopyOnWriteArrayList<>();
    private final List<Enemy> enemies = new CopyOnWriteArrayList<>();
    private final List<HealItem> pickups = new CopyOnWriteArrayList<>();

    public GamePanel() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(Color.BLACK);
        setDoubleBuffered(true);
        setFocusable(true);

        menu = new MainMenu(
                this::startGame,                   // Play
                () -> System.exit(0)               // Quit
        );
        endScreen = new EndScreen(
                this::startGame,                   // Play again
                () -> setState(GameState.MENU)     // Main menu
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
                switch (state) {
                    case MENU:
                        menu.mousePressed(e.getX(), e.getY());
                        break;
                    case GAME_OVER:
                    case WIN:
                        endScreen.mousePressed(e.getX(), e.getY());
                        break;
                    case PLAYING:
                        input.mouseDown = true;
                        break;
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
                } else if (state == GameState.GAME_OVER || state == GameState.WIN) {
                    endScreen.mouseMoved(e.getX(), e.getY());
                }
            }
        };
        addMouseListener(mouse);
        addMouseMotionListener(mouse);

        addKeyListener(input);

        // ปุ่มระบบ: ESC = กลับเมนู, M = ปิด/เปิดเสียง, F3 = แสดง FPS, F5 = โหลดด่านใหม่ (ไว้ลองแก้แผนที่)
        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                switch (e.getKeyCode()) {
                    case KeyEvent.VK_ESCAPE:
                        if (state != GameState.MENU) setState(GameState.MENU);
                        break;
                    case KeyEvent.VK_M:
                        Sound.muted = !Sound.muted;
                        break;
                    case KeyEvent.VK_MINUS:
                    case KeyEvent.VK_SUBTRACT:
                        Sound.changeVolume(-0.1);
                        volumeShownUntil = System.currentTimeMillis() + 1500;
                        Sound.play(Sound.Effect.CLICK);
                        break;
                    case KeyEvent.VK_EQUALS:
                    case KeyEvent.VK_ADD:
                        Sound.changeVolume(0.1);
                        volumeShownUntil = System.currentTimeMillis() + 1500;
                        Sound.play(Sound.Effect.CLICK);
                        break;
                    case KeyEvent.VK_F3:
                        showFps = !showFps;
                        break;
                    case KeyEvent.VK_F5:
                        if (state == GameState.PLAYING) reloadRequested = true;
                        break;
                    default:
                        break;
                }
            }
        });
    }

    public void startGameLoop() {
        gameThread = new Thread(this, "game-loop");
        gameThread.start();
    }

    /** เริ่มเกมใหม่ทั้งหมด: ศูนย์คะแนน สร้างผู้เล่นใหม่ เริ่มด่าน 1 */
    private void startGame() {
        score = 0;
        player = new Player(100, 100);
        loadLevel(1, 0);
        setState(GameState.PLAYING);
        requestFocusInWindow(); // ให้คีย์บอร์ดทำงาน
    }

    /** โหลดด่านที่ n: สร้างฉากและศัตรูใหม่ ย้ายผู้เล่นไปจุดเริ่ม */
    private void loadLevel(int n, int heal) {
        levelIndex = n;
        level = Level.load(LEVEL_FILES[n - 1]);
        projectiles.clear();
        enemies.clear();
        enemies.addAll(level.createEnemies());
        pickups.clear();
        pickups.addAll(level.createPickups());
        // วางผู้เล่นให้เท้าอยู่ที่จุด P ในแผนที่
        player.startLevel(level.startFeetX - player.width / 2.0, level.startFeetY - player.height, heal);
        input.reset();
        cameraX = 0;
        bannerTimer = 120; // แสดงชื่อด่าน 2 วินาที
    }

    /** ผู้เล่นแตะธงปลายทาง: ไปด่านถัดไป หรือชนะถ้าเป็นด่านสุดท้าย */
    private void finishLevel() {
        score += LEVEL_CLEAR_BONUS;
        if (levelIndex < TOTAL_LEVELS) {
            Sound.play(Sound.Effect.LEVEL_CLEAR);
            loadLevel(levelIndex + 1, HEAL_BETWEEN_LEVELS);
        } else {
            Sound.play(Sound.Effect.WIN);
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
                        if (en.tryReflect(p)) break; // ถูกสะท้อน: กระสุนกลายเป็นของศัตรู พุ่งกลับไป
                        en.hp -= p.damage;
                        p.active = false;
                        if (en.isAlive()) {
                            Sound.play(Sound.Effect.HIT);
                        } else {
                            score += en.scoreValue;
                            Sound.play(Sound.Effect.ENEMY_DIE);
                        }
                        break; // กระสุน 1 นัด โดนศัตรูได้ตัวเดียว
                    }
                }
            } else if (pBox.intersects(playerBox)) {
                // กระสุนศัตรู -> ผู้เล่น (ถ้าแดชอยู่ takeDamage จะไม่ทำอะไร แต่กระสุนยังหายไป)
                player.takeDamage(p.damage);
                p.active = false;
            }
        }

        // ตัวศัตรูชนผู้เล่น
        for (Enemy en : enemies) {
            if (en.isAlive() && en.getBounds().intersects(playerBox)) {
                player.takeDamage(en.getContactDamage());
            }
        }

        // เก็บไอเท็มฮีล (เก็บได้เฉพาะตอนเลือดไม่เต็ม ไม่งั้นปล่อยไว้ก่อน เก็บทีหลังได้)
        for (HealItem item : pickups) {
            if (item.active && player.hp < player.maxHp && playerBox.intersects(item.getPickupBounds())) {
                player.heal(HealItem.HEAL_AMOUNT);
                item.active = false;
                Sound.play(Sound.Effect.PICKUP);
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
    void update() {
        if (state != GameState.PLAYING) return; // เมนู/จบเกม: ไม่มีอะไรต้องอัปเดตต่อเฟรม

        if (reloadRequested) { // F5: อ่านไฟล์ด่านใหม่ (เลือด/คะแนนคงเดิม)
            reloadRequested = false;
            loadLevel(levelIndex, 0);
        }

        player.update(input, level, cameraX, projectiles);
        for (Projectile p : projectiles) {
            p.update(level);
        }
        for (Enemy en : enemies) {
            if (en.isOnScreen(cameraX)) {
                en.update(player, level, projectiles); // อยู่ในจอ: เดิน/บินเข้าหา และยิง
            } else {
                en.updateOffScreen(level);              // นอกจอ: ยืนนิ่งรอ
            }
        }
        handleCollisions();
        projectiles.removeIf(p -> !p.active);
        enemies.removeIf(en -> !en.isAlive());
        for (HealItem item : pickups) item.update();
        pickups.removeIf(item -> !item.active);
        updateCamera();
        if (bannerTimer > 0) bannerTimer--;

        if (player.y > level.worldHeight + 60) {
            player.kill(); // ตกหลุม
        }

        if (!player.isAlive()) {
            input.reset();
            Sound.play(Sound.Effect.GAME_OVER);
            setState(GameState.GAME_OVER);
        } else if (player.getBounds().intersects(level.goalZone)) {
            finishLevel();
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
                drawHud(g2);
                break;
            case GAME_OVER:
                drawWorld(g2); // ฉากค้างไว้ข้างหลัง
                endScreen.draw(g2, "GAME OVER", new Color(255, 80, 80), "Score: " + score);
                break;
            case WIN:
                drawWorld(g2);
                endScreen.draw(g2, "YOU WIN!", new Color(255, 220, 60), "Final score: " + score);
                break;
        }

        if (showFps) {
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
            g2.drawString("FPS: " + currentFps, 10, 20);
        }
        if (System.currentTimeMillis() < volumeShownUntil) {
            g2.setColor(new Color(255, 255, 255, 220));
            g2.setFont(new Font("SansSerif", Font.BOLD, 14));
            g2.drawString("Volume: " + (int) Math.round(Sound.getVolume() * 100) + "%  (- / =)", WIDTH - 170, HEIGHT - 30);
        }
        if (Sound.muted) {
            g2.setColor(new Color(255, 255, 255, 180));
            g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
            g2.drawString("MUTED (M)", WIDTH - 90, HEIGHT - 12);
        }
    }

    /** วาดโลกเกม (ด่าน ศัตรู กระสุน ผู้เล่น) ใช้ซ้ำทั้งตอนเล่นและตอนจบเกม */
    private void drawWorld(Graphics2D g2) {
        level.draw(g2, cameraX);
        for (HealItem item : pickups) {
            item.draw(g2, cameraX);
        }
        for (Enemy en : enemies) {
            en.draw(g2, cameraX);
        }
        for (Projectile p : projectiles) {
            p.draw(g2, cameraX);
        }
        player.draw(g2, cameraX);
    }

    /** HUD: ติดจอ ไม่ขยับตามกล้อง */
    private void drawHud(Graphics2D g2) {
        // หลอดเลือด
        HealthBar.draw(g2, 20, 20, 220, 24, player.hp, player.maxHp);

        // หลอดความพร้อมของแดช (เต็ม = แดชได้)
        double dashReady = player.dashReadyRatio();
        g2.setColor(new Color(40, 40, 60));
        g2.fillRect(20, 50, 220, 8);
        g2.setColor(dashReady >= 1 ? new Color(120, 200, 255) : new Color(90, 120, 160));
        g2.fillRect(20, 50, (int) (220 * dashReady), 8);

        // คะแนน + ด่าน (ชิดขวา)
        drawRightText(g2, "SCORE " + score, new Font("SansSerif", Font.BOLD, 24), 36);
        drawRightText(g2, level.name + "   Enemies: " + enemies.size(),
                new Font("SansSerif", Font.BOLD, 16), 60);

        // วิธีเล่น (ล่างซ้าย)
        g2.setColor(new Color(255, 255, 255, 190));
        g2.setFont(new Font("SansSerif", Font.PLAIN, 13));
        g2.drawString("A/D move   Space/W jump   Shift dash   Mouse aim+shoot   M mute   ESC menu",
                10, HEIGHT - 12);

        // ชื่อด่านกลางจอตอนเริ่ม
        if (bannerTimer > 0) {
            int alpha = Math.min(255, bannerTimer * 4);
            drawCenteredText(g2, level.name, new Font("SansSerif", Font.BOLD, 72),
                    new Color(255, 255, 255, alpha), HEIGHT / 2 - 40);
            drawCenteredText(g2, "Reach the flag!", new Font("SansSerif", Font.PLAIN, 26),
                    new Color(255, 255, 255, alpha), HEIGHT / 2);
        }
    }

    private void drawRightText(Graphics2D g2, String text, Font font, int baselineY) {
        g2.setFont(font);
        FontMetrics fm = g2.getFontMetrics();
        int x = WIDTH - 20 - fm.stringWidth(text);
        g2.setColor(new Color(0, 0, 0, 140));
        g2.drawString(text, x + 1, baselineY + 1);
        g2.setColor(Color.WHITE);
        g2.drawString(text, x, baselineY);
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
}
