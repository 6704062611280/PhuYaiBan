package level;

import entity.Enemy;
import entity.FlyingEnemy;
import entity.HealItem;
import entity.TankEnemy;
import entity.WalkerEnemy;
import main.GamePanel;
import util.Assets;

import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * ด่านหนึ่งด่าน โหลดจากไฟล์ข้อความ res/levels/xxx.txt (ดูรูปแบบในหัวไฟล์ด่าน)
 * แผนที่เป็นตาราง 1 ตัวอักษร = 1 ช่อง ขนาด TILE x TILE พิกเซล
 */
public class Level {

    public static final int TILE = 40;
    private static final int MIN_ROWS = 14;       // 14 x 40 = 560px >= ความสูงหน้าจอ
    private static final int HILL_BASE_Y = 440;   // เส้นฐานของภูเขาไกล ๆ ในฉากหลัง

    /** จุดเกิดศัตรูหนึ่งตัวที่อ่านได้จากแผนที่ */
    private static class Spawn {
        final char type;      // 'W' เดิน, 'F' บิน, 'T' แท็งก์, 'H' ไอเท็มฮีล
        final double x, y;    // W: (กลางช่อง, ใต้ช่อง)  F: (กลางช่อง, กลางช่อง)
        Spawn(char type, double x, double y) {
            this.type = type;
            this.x = x;
            this.y = y;
        }
    }

    public final String name;
    public final int worldWidth;
    public final int worldHeight;
    public final double difficulty;
    public final double startFeetX, startFeetY; // จุดเกิดผู้เล่น (กลางเท้า)
    public final Rectangle goalZone;            // พื้นที่ธง แตะแล้วผ่านด่าน

    private final int cols;
    private final char[][] tiles;               // [แถว][คอลัมน์]
    private final List<Spawn> spawns;
    private final int goalX, goalFeetY;
    private final boolean night;
    private final boolean walkerReflect; // true = Walker ในด่านนี้ยกโล่สะท้อนกระสุนได้

    // ธีมสี
    private final Color skyTop, skyBottom, hillColor, groundColor, grassColor;

    // ---------------------------------------------------------------
    //  โหลดและอ่านไฟล์ด่าน
    // ---------------------------------------------------------------

    /** โหลดด่านจากไฟล์ เช่น load("level1.txt") ถ้าหาไฟล์ไม่เจอจะสร้างด่านพื้นเรียบแทน */
    public static Level load(String fileName) {
        List<String> lines = new ArrayList<>();
        try (InputStream in = Assets.open("levels/" + fileName)) {
            if (in != null) {
                BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                String line;
                while ((line = br.readLine()) != null) {
                    lines.add(line);
                }
            }
        } catch (IOException ignored) {
            // ตกไปใช้ด่านสำรอง
        }
        if (lines.isEmpty()) {
            // ข้อความเป็นภาษาอังกฤษ เพราะคอนโซลบางเครื่อง (Windows) แสดงภาษาไทยเพี้ยน
            System.err.println("[Level] Level file not found: res/levels/" + fileName
                    + " -> using a flat fallback level.\n"
                    + Assets.describeSearch("levels/" + fileName)
                    + "  Fix: put the 'res' folder (with levels/" + fileName + ") in one of the places above.");
            lines = fallbackLines();
        }
        return parse(lines);
    }

    private static List<String> fallbackLines() {
        List<String> l = new ArrayList<>();
        l.add("name: LEVEL (missing file)");
        l.add("---");
        for (int r = 0; r < MIN_ROWS; r++) {
            StringBuilder sb = new StringBuilder();
            for (int c = 0; c < 40; c++) {
                if (r >= 11) sb.append('#');
                else if (r == 10 && c == 2) sb.append('P');
                else if (r == 10 && c == 37) sb.append('G');
                else sb.append(' ');
            }
            l.add(sb.toString());
        }
        return l;
    }

    private static Level parse(List<String> lines) {
        if (!lines.isEmpty() && lines.get(0).startsWith("﻿")) { // ตัด BOM ของ Notepad
            lines.set(0, lines.get(0).substring(1));
        }

        // แยกส่วนหัว / แผนที่ ด้วยบรรทัด ---
        int sep = -1;
        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).trim().equals("---")) {
                sep = i;
                break;
            }
        }
        List<String> header = sep < 0 ? new ArrayList<>() : lines.subList(0, sep);
        List<String> map = new ArrayList<>(lines.subList(sep + 1, lines.size()));

        String name = "LEVEL";
        boolean night = false;
        double difficulty = 1.0;
        boolean walkerReflect = true;
        for (String h : header) {
            String t = h.trim();
            if (t.isEmpty() || t.startsWith("//")) continue;
            int colon = t.indexOf(':');
            if (colon < 0) continue;
            String key = t.substring(0, colon).trim().toLowerCase();
            String val = t.substring(colon + 1).trim();
            switch (key) {
                case "name":
                    name = val;
                    break;
                case "theme":
                    night = val.equalsIgnoreCase("night");
                    break;
                case "walker_reflect":
                    walkerReflect = !(val.equalsIgnoreCase("off") || val.equalsIgnoreCase("false")
                            || val.equalsIgnoreCase("no") || val.equals("0"));
                    break;
                case "difficulty":
                    try {
                        difficulty = Double.parseDouble(val);
                    } catch (NumberFormatException e) {
                        System.err.println("[Level] difficulty ไม่ใช่ตัวเลข: " + val);
                    }
                    break;
                default:
                    break;
            }
        }

        // ตัดบรรทัดว่างท้ายไฟล์ (แต่ไม่ให้เหลือน้อยกว่า MIN_ROWS)
        while (map.size() > MIN_ROWS && map.get(map.size() - 1).trim().isEmpty()) {
            map.remove(map.size() - 1);
        }
        int rows = Math.max(map.size(), MIN_ROWS);
        int cols = 1;
        for (String row : map) cols = Math.max(cols, row.length());

        char[][] tiles = new char[rows][cols];
        for (char[] row : tiles) java.util.Arrays.fill(row, ' ');

        List<Spawn> spawns = new ArrayList<>();
        double startX = -1, startY = -1;
        int goalX = -1, goalY = -1;

        for (int r = 0; r < map.size(); r++) {
            String row = map.get(r);
            for (int c = 0; c < row.length(); c++) {
                char ch = row.charAt(c);
                double cx = c * TILE + TILE / 2.0;
                switch (ch) {
                    case '#':
                    case '=':
                        tiles[r][c] = ch;
                        break;
                    case 'P':
                        startX = cx;
                        startY = (r + 1) * TILE;
                        break;
                    case 'G':
                        goalX = (int) cx;
                        goalY = (r + 1) * TILE;
                        break;
                    case 'W':
                        spawns.add(new Spawn('W', cx, (r + 1) * TILE));
                        break;
                    case 'F':
                        spawns.add(new Spawn('F', cx, r * TILE + TILE / 2.0));
                        break;
                    case 'T':
                        spawns.add(new Spawn('T', cx, (r + 1) * TILE));
                        break;
                    case 'H':
                        spawns.add(new Spawn('H', cx, (r + 1) * TILE));
                        break;
                    default:
                        break; // ช่องว่าง ' ' '.' หรืออักขระอื่น = อากาศ
                }
            }
        }

        if (startX < 0) {
            System.err.println("[Level] ไม่พบ P (จุดเกิดผู้เล่น) ใช้ตำแหน่งเริ่มต้น");
            startX = 3 * TILE + TILE / 2.0;
            startY = 11 * TILE;
        }
        if (goalX < 0) {
            System.err.println("[Level] ไม่พบ G (ธงปลายทาง) วางไว้ท้ายด่าน");
            goalX = cols * TILE - 2 * TILE;
            goalY = 11 * TILE;
        }

        return new Level(name, night, difficulty, walkerReflect, tiles, spawns, startX, startY, goalX, goalY);
    }

    private Level(String name, boolean night, double difficulty, boolean walkerReflect,
                  char[][] tiles, List<Spawn> spawns,
                  double startX, double startY, int goalX, int goalY) {
        this.name = name;
        this.night = night;
        this.difficulty = difficulty;
        this.walkerReflect = walkerReflect;
        this.tiles = tiles;
        this.cols = tiles[0].length;
        this.worldWidth = cols * TILE;
        this.worldHeight = tiles.length * TILE;
        this.spawns = spawns;
        this.startFeetX = startX;
        this.startFeetY = startY;
        this.goalX = goalX;
        this.goalFeetY = goalY;
        this.goalZone = new Rectangle(goalX - 20, goalY - 140, 40, 140);

        if (night) {
            skyTop = new Color(10, 10, 40);
            skyBottom = new Color(60, 40, 100);
            hillColor = new Color(35, 30, 70);
            groundColor = new Color(45, 40, 55);
            grassColor = new Color(70, 90, 110);
        } else {
            skyTop = new Color(90, 160, 230);
            skyBottom = new Color(190, 225, 250);
            hillColor = new Color(110, 140, 180);
            groundColor = new Color(90, 60, 40);
            grassColor = new Color(70, 160, 70);
        }
    }

    // ---------------------------------------------------------------
    //  ศัตรู
    // ---------------------------------------------------------------

    /** สร้างศัตรูใหม่ทั้งหมดตามตำแหน่ง W / F / T ในแผนที่ */
    public List<Enemy> createEnemies() {
        List<Enemy> list = new ArrayList<>();
        for (Spawn s : spawns) {
            switch (s.type) {
                case 'W':
                    list.add(new WalkerEnemy(s.x, s.y, difficulty, walkerReflect));
                    break;
                case 'F':
                    list.add(new FlyingEnemy(s.x, s.y, difficulty));
                    break;
                case 'T':
                    list.add(new TankEnemy(s.x, s.y, difficulty));
                    break;
                default:
                    break; // 'H' ไม่ใช่ศัตรู
            }
        }
        return list;
    }

    /** สร้างไอเท็มฮีลใหม่ทั้งหมดตามตำแหน่ง H ในแผนที่ */
    public List<HealItem> createPickups() {
        List<HealItem> list = new ArrayList<>();
        for (Spawn s : spawns) {
            if (s.type == 'H') list.add(new HealItem(s.x, s.y));
        }
        return list;
    }

    // ---------------------------------------------------------------
    //  ถามข้อมูลช่องในแผนที่ (ใช้ตอนตรวจการชน)
    // ---------------------------------------------------------------

    public char tileAt(int col, int row) {
        if (row < 0 || row >= tiles.length || col < 0 || col >= cols) return ' ';
        return tiles[row][col];
    }

    /** บล็อกตัน (#) */
    public boolean isSolid(int col, int row) {
        return tileAt(col, row) == '#';
    }

    /** แพลตฟอร์มบาง (=) */
    public boolean isOneWay(int col, int row) {
        return tileAt(col, row) == '=';
    }

    /** จุดพิกัดโลกนี้อยู่ในบล็อกตันหรือไม่ (ใช้กับกระสุน) */
    public boolean isSolidAt(double px, double py) {
        return isSolid((int) Math.floor(px / TILE), (int) Math.floor(py / TILE));
    }

    // ---------------------------------------------------------------
    //  วาด
    // ---------------------------------------------------------------

    public void draw(Graphics2D g2, int cameraX) {
        // ท้องฟ้า (ไม่ขยับ)
        g2.setPaint(new GradientPaint(0, 0, skyTop, 0, HILL_BASE_Y, skyBottom));
        g2.fillRect(0, 0, GamePanel.WIDTH, GamePanel.HEIGHT);

        // ดาว (เฉพาะด่านกลางคืน) ตำแหน่งคงที่ คำนวณจากสูตร ไม่สุ่มใหม่ทุกเฟรม
        if (night) {
            g2.setColor(new Color(255, 255, 255, 200));
            for (int i = 0; i < 70; i++) {
                int sx = (i * 137) % GamePanel.WIDTH;
                int sy = (i * 71) % (HILL_BASE_Y - 120);
                g2.fillRect(sx, sy, 2, 2);
            }
        }

        // ภูเขาไกล ๆ เลื่อนช้ากว่าพื้น (parallax) ให้ดูมีมิติ
        g2.setColor(hillColor);
        int hillOffset = (int) (cameraX * 0.3);
        for (int i = -1; i < 8; i++) {
            int hx = i * 400 - (hillOffset % 400);
            g2.fillPolygon(new int[] {hx, hx + 200, hx + 400},
                    new int[] {HILL_BASE_Y, HILL_BASE_Y - 180, HILL_BASE_Y}, 3);
        }

        // ใต้ดินสีเข้ม (เห็นตรงหลุม)
        g2.setColor(groundColor.darker().darker());
        g2.fillRect(0, HILL_BASE_Y, GamePanel.WIDTH, GamePanel.HEIGHT - HILL_BASE_Y);

        drawTiles(g2, cameraX);
        drawGoalFlag(g2, cameraX);
    }

    private void drawTiles(Graphics2D g2, int cameraX) {
        int firstCol = Math.max(0, cameraX / TILE);
        int lastCol = Math.min(cols - 1, (cameraX + GamePanel.WIDTH) / TILE);

        for (int r = 0; r < tiles.length; r++) {
            for (int c = firstCol; c <= lastCol; c++) {
                char t = tiles[r][c];
                if (t == ' ') continue;
                int x = c * TILE - cameraX;
                int y = r * TILE;

                if (t == '#') {
                    g2.setColor(groundColor);
                    g2.fillRect(x, y, TILE, TILE);
                    g2.setColor(new Color(0, 0, 0, 45));
                    g2.drawRect(x, y, TILE - 1, TILE - 1);
                    if (!isSolid(c, r - 1)) { // ผิวบนสุดมีหญ้า
                        g2.setColor(grassColor);
                        g2.fillRect(x, y, TILE, 9);
                    }
                } else { // '=' แพลตฟอร์มบาง
                    g2.setColor(groundColor.brighter());
                    g2.fillRect(x, y, TILE, 12);
                    g2.setColor(grassColor);
                    g2.fillRect(x, y, TILE, 5);
                    g2.setColor(new Color(0, 0, 0, 60));
                    g2.drawRect(x, y, TILE - 1, 11);
                }
            }
        }
    }

    private void drawGoalFlag(Graphics2D g2, int cameraX) {
        int sx = goalX - cameraX;
        if (sx < -80 || sx > GamePanel.WIDTH + 80) return;
        g2.setColor(new Color(220, 220, 220));
        g2.fillRect(sx - 3, goalFeetY - 140, 6, 140);               // เสา
        g2.setColor(new Color(240, 200, 40));
        g2.fillPolygon(new int[] {sx + 3, sx + 63, sx + 3},         // ผืนธง
                new int[] {goalFeetY - 138, goalFeetY - 114, goalFeetY - 90}, 3);
    }
}
