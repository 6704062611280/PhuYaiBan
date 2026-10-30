package util;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

/**
 * รูปเรียงเฟรมแนวนอนในไฟล์เดียว (sprite sheet) เช่น player.png กว้าง 280 = 7 เฟรม x 40px
 * - ไฟล์ที่ความสูงเท่ากับ frameH และความกว้างหารด้วย frameW ลงตัว = หลายเฟรม
 * - ไฟล์อื่น ๆ (เช่นรูปเดี่ยวที่วาดเอง คนละขนาด) = ใช้เป็น 1 เฟรม ย่อ/ขยายให้พอดีตัวละคร
 * - ไม่พบไฟล์ = load() คืน null แล้วตัวละครจะใช้ภาพวาดสำรองในโค้ดแทน
 */
public final class SpriteSheet {

    private final BufferedImage[] frames;

    private SpriteSheet(BufferedImage[] frames) {
        this.frames = frames;
    }

    public static SpriteSheet load(String relPath, int frameW, int frameH) {
        BufferedImage img = Assets.image(relPath);
        if (img == null) return null;

        boolean isSheet = img.getHeight() == frameH && img.getWidth() >= frameW && img.getWidth() % frameW == 0;
        if (!isSheet) {
            return new SpriteSheet(new BufferedImage[] {img});
        }
        int n = img.getWidth() / frameW;
        BufferedImage[] f = new BufferedImage[n];
        for (int i = 0; i < n; i++) {
            f[i] = img.getSubimage(i * frameW, 0, frameW, frameH);
        }
        return new SpriteSheet(f);
    }

    public int count() {
        return frames.length;
    }

    /** เฟรมที่ i (ถ้าเกินจำนวนเฟรมที่มี ใช้เฟรมสุดท้าย) */
    public BufferedImage frame(int i) {
        return frames[Math.max(0, Math.min(i, frames.length - 1))];
    }

    /** วาดเฟรมที่ i ลงกรอบ x,y,w,h (รูปต้นฉบับต้องหันขวา ถ้าหันซ้ายจะพลิกให้) */
    public void draw(Graphics2D g2, int i, int x, int y, int w, int h, boolean facingRight) {
        Assets.drawFacing(g2, frame(i), x, y, w, h, facingRight);
    }
}
