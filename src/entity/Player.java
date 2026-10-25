package entity;

import input.InputHandler;
import level.Level;

import java.awt.Color;
import java.awt.Graphics2D;

public class Player extends Entity {

    private static final double SPEED = 4.5;
    private static final double GRAVITY = 0.6;
    private static final double MAX_FALL_SPEED = 15;

    private double velocityY = 0;
    public boolean facingRight = true;

    public Player(double x, double y) {
        super(x, y, 40, 60, 100);
    }

    public void update(InputHandler input, Level level) {
        // เดินซ้าย/ขวา
        if (input.left)  { x -= SPEED; facingRight = false; }
        if (input.right) { x += SPEED; facingRight = true; }

        // ไม่ให้หลุดขอบโลก
        if (x < 0) x = 0;
        if (x + width > level.worldWidth) x = level.worldWidth - width;

        // แรงโน้มถ่วง + พื้น
        velocityY = Math.min(velocityY + GRAVITY, MAX_FALL_SPEED);
        y += velocityY;
        if (y + height >= level.groundY) {
            y = level.groundY - height;
            velocityY = 0;
        }
    }

    /** วาดที่ตำแหน่งโลก ลบด้วย cameraX = ตำแหน่งบนหน้าจอ */
    public void draw(Graphics2D g2, int cameraX) {
        int sx = (int) x - cameraX;
        int sy = (int) y;

        g2.setColor(new Color(40, 90, 200));
        g2.fillRect(sx, sy, width, height);

        // ตา บอกทิศที่หัน
        g2.setColor(Color.WHITE);
        int eyeX = facingRight ? sx + width - 16 : sx + 6;
        g2.fillRect(eyeX, sy + 12, 10, 10);
        g2.setColor(Color.BLACK);
        g2.fillRect(facingRight ? eyeX + 5 : eyeX, sy + 14, 5, 6);
    }
}
