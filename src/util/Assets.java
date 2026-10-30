package util;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * โหลดรูป/เสียง/ด่านจากโฟลเดอร์ res
 * ลำดับการหา (เจอที่ไหนก่อนใช้ที่นั่น):
 *   1) ใน classpath / ใน .jar  (/res/...)
 *   2) โฟลเดอร์ res ใต้โฟลเดอร์ที่รันโปรแกรม (working directory)
 *   3) โฟลเดอร์ res ใต้โฟลเดอร์ย่อยชั้นแรกของ working directory (กรณีเปิด VS Code ที่โฟลเดอร์นอกสุด)
 *   4) โฟลเดอร์ res ใกล้ ๆ ที่เก็บไฟล์ .class / .jar (ไล่ขึ้นไปสูงสุด 5 ชั้น)
 * ถ้าไม่มีไฟล์ จะคืนค่า null แล้วเกมจะใช้ของในตัว (สี่เหลี่ยม / เสียงสังเคราะห์ / ด่านพื้นเรียบ) แทน
 */
public final class Assets {

    private static final int MAX_PARENT_LEVELS = 5;
    private static final Map<String, BufferedImage> images = new HashMap<>();

    private Assets() {}

    /** เปิดไฟล์ใน res เช่น open("images/player.png") ไม่เจอคืน null */
    public static InputStream open(String relPath) {
        InputStream in = Assets.class.getResourceAsStream("/res/" + relPath);
        if (in != null) return in;

        for (File f : candidateFiles(relPath)) {
            if (f.isFile()) {
                try {
                    return new FileInputStream(f);
                } catch (FileNotFoundException ignored) {
                    // ลองที่ถัดไป
                }
            }
        }
        return null;
    }

    /** ข้อความบอกว่าเคยลองหาไฟล์ที่ไหนบ้าง (ไว้แสดงตอนหาไม่เจอ จะได้รู้ว่าต้องวางไฟล์ที่ไหน) */
    public static String describeSearch(String relPath) {
        StringBuilder sb = new StringBuilder();
        sb.append("working dir = ").append(new File("").getAbsolutePath()).append('\n');
        sb.append("  searched: classpath /res/").append(relPath).append('\n');
        for (File f : candidateFiles(relPath)) {
            sb.append("  searched: ").append(f.getPath()).append('\n');
        }
        return sb.toString();
    }

    /** รายการตำแหน่งที่ไฟล์ res/<relPath> อาจอยู่ (เรียงตามลำดับความสำคัญ) */
    private static List<File> candidateFiles(String relPath) {
        Set<File> bases = new LinkedHashSet<>();

        File cwd = new File("").getAbsoluteFile();
        bases.add(cwd);

        File[] subDirs = cwd.listFiles(File::isDirectory);
        if (subDirs != null) {
            for (File d : subDirs) bases.add(d);
        }

        File codeDir = codeLocation();
        if (codeDir != null) {
            File p = codeDir;
            for (int i = 0; i <= MAX_PARENT_LEVELS && p != null; i++) {
                bases.add(p);
                p = p.getParentFile();
            }
        }

        List<File> result = new ArrayList<>();
        for (File b : bases) {
            result.add(new File(b, "res/" + relPath));
        }
        return result;
    }

    /** โฟลเดอร์ที่เก็บ .class (เช่น bin/ out/) หรือโฟลเดอร์ที่มี .jar อยู่ */
    private static File codeLocation() {
        try {
            File f = new File(Assets.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            return f.isFile() ? f.getParentFile() : f;
        } catch (Exception e) {
            return null;
        }
    }

    /** โหลดรูป (เก็บ cache) ไม่เจอหรืออ่านไม่ได้คืน null */
    public static synchronized BufferedImage image(String relPath) {
        if (images.containsKey(relPath)) return images.get(relPath);

        BufferedImage img = null;
        try (InputStream in = open(relPath)) {
            if (in != null) img = ImageIO.read(in);
        } catch (IOException ignored) {
            // ใช้ null
        }
        images.put(relPath, img);
        return img;
    }

    /** วาดรูปที่ตำแหน่ง/ขนาดที่กำหนด ถ้าหันซ้ายให้พลิกรูป (รูปต้นฉบับต้องหันขวา) */
    public static void drawFacing(Graphics2D g2, BufferedImage img, int x, int y, int w, int h, boolean facingRight) {
        if (facingRight) {
            g2.drawImage(img, x, y, w, h, null);
        } else {
            g2.drawImage(img, x + w, y, -w, h, null);
        }
    }
}
