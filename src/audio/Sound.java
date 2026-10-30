package audio;

import util.Assets;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

/**
 * เสียงเอฟเฟกต์ทั้งหมดของเกม
 * - ค่าเริ่มต้น: สร้างเสียงสังเคราะห์ด้วยโค้ดเอง ไม่ต้องมีไฟล์
 * - ถ้ามีไฟล์ res/sounds/<ชื่อ>.wav (เช่น shoot.wav) จะใช้ไฟล์นั้นแทน
 * - ถ้าเครื่องไม่มีอุปกรณ์เสียง เกมจะเล่นต่อได้ตามปกติ แค่ไม่มีเสียง
 */
public final class Sound {

    public enum Effect {
        SHOOT("shoot", 4),
        JUMP("jump", 2),
        DASH("dash", 2),
        HIT("hit", 3),
        ENEMY_DIE("enemy_die", 3),
        HURT("hurt", 2),
        CLICK("click", 1),
        LEVEL_CLEAR("level_clear", 1),
        GAME_OVER("game_over", 1),
        PICKUP("pickup", 2),
        REFLECT("reflect", 3),
        WIN("win", 1);

        final String file;
        final int voices; // จำนวน Clip ต่อเสียง (ให้เสียงเดียวกันซ้อนกันได้)

        Effect(String file, int voices) {
            this.file = file;
            this.voices = voices;
        }
    }

    private static final float RATE = 22050f;
    private static final AudioFormat FORMAT = new AudioFormat(RATE, 16, 1, true, false);

    private static final int SINE = 0, SQUARE = 1, SAW = 2, NOISE = 3;

    private static volatile Map<Effect, Clip[]> clips = null; // null = ยังไม่พร้อม / ไม่มีเสียง
    private static final int[] nextVoice = new int[Effect.values().length];

    /** ปิด/เปิดเสียง (ปุ่ม M) */
    public static volatile boolean muted = false;

    /** ความดังรวม 0.0 - 1.0 (ปุ่ม - และ = ปรับได้ตอนเล่น) ปรับค่าเริ่มต้นได้ที่นี่ */
    public static final double DEFAULT_VOLUME = 0.5;
    private static volatile double volume = DEFAULT_VOLUME;

    public static double getVolume() { return volume; }

    public static void changeVolume(double delta) {
        volume = Math.max(0.0, Math.min(1.0, Math.round((volume + delta) * 10) / 10.0));
    }

    private static void applyVolume(Clip c) {
        try {
            if (!c.isControlSupported(javax.sound.sampled.FloatControl.Type.MASTER_GAIN)) return;
            javax.sound.sampled.FloatControl g =
                (javax.sound.sampled.FloatControl) c.getControl(javax.sound.sampled.FloatControl.Type.MASTER_GAIN);
            float db = volume <= 0.001 ? g.getMinimum() : (float) (20.0 * Math.log10(volume));
            g.setValue(Math.max(g.getMinimum(), Math.min(g.getMaximum(), db)));
        } catch (Throwable ignored) {
            // อุปกรณ์ไม่รองรับการปรับความดัง: ข้าม
        }
    }

    private Sound() {}

    /** เตรียมเสียงใน thread แยก ไม่ให้หน่วงตอนเปิดเกม */
    public static void initAsync() {
        Thread t = new Thread(Sound::init, "sound-init");
        t.setDaemon(true);
        t.start();
    }

    private static void init() {
        try {
            Map<Effect, Clip[]> map = new EnumMap<>(Effect.class);
            for (Effect e : Effect.values()) {
                map.put(e, buildClips(e));
            }
            clips = map;
        } catch (Throwable t) {
            // ไม่มีอุปกรณ์เสียง ฯลฯ: เล่นเกมต่อแบบเงียบ
            clips = null;
        }
    }

    private static Clip[] buildClips(Effect e) throws Exception {
        byte[] wav = loadFile(e);
        byte[] pcm = null;
        Clip[] arr = new Clip[e.voices];

        for (int i = 0; i < arr.length; i++) {
            Clip c = AudioSystem.getClip();
            boolean opened = false;

            if (wav != null) {
                try (AudioInputStream ais = AudioSystem.getAudioInputStream(new ByteArrayInputStream(wav))) {
                    c.open(ais);
                    opened = true;
                } catch (UnsupportedAudioFileException | IOException ignored) {
                    // ไฟล์เสีย/ฟอร์แมตไม่รองรับ -> ใช้เสียงสังเคราะห์แทน
                }
            }
            if (!opened) {
                if (pcm == null) pcm = synth(e);
                c.open(FORMAT, pcm, 0, pcm.length);
            }
            arr[i] = c;
        }
        return arr;
    }

    private static byte[] loadFile(Effect e) {
        try (InputStream in = Assets.open("sounds/" + e.file + ".wav")) {
            return in == null ? null : in.readAllBytes();
        } catch (IOException ex) {
            return null;
        }
    }

    /** เล่นเสียง (เรียกได้จากทุก thread) */
    public static synchronized void play(Effect e) {
        Map<Effect, Clip[]> map = clips;
        if (muted || map == null) return;

        Clip[] voices = map.get(e);
        int i = nextVoice[e.ordinal()];
        nextVoice[e.ordinal()] = (i + 1) % voices.length;

        Clip c = voices[i];
        c.stop();
        applyVolume(c);
        c.setFramePosition(0);
        c.start();
    }

    // ---------- เสียงสังเคราะห์ ----------

    private static byte[] synth(Effect e) {
        switch (e) {
            case SHOOT:       return tone(0.09, 900, 300, SQUARE, 0.12, 1.5);
            case JUMP:        return tone(0.15, 300, 700, SQUARE, 0.12, 1.0);
            case DASH:        return tone(0.16, 0, 0, NOISE, 0.15, 1.5);
            case HIT:         return tone(0.07, 220, 110, SAW, 0.20, 1.2);
            case ENEMY_DIE:   return tone(0.28, 0, 0, NOISE, 0.22, 2.0);
            case HURT:        return tone(0.25, 180, 70, SQUARE, 0.22, 1.0);
            case CLICK:       return tone(0.05, 600, 800, SQUARE, 0.10, 1.0);
            case LEVEL_CLEAR: return sequence(SQUARE, 0.14, 0.11, 523, 659, 784, 1047);
            case GAME_OVER:   return sequence(SAW, 0.18, 0.22, 392, 330, 262, 175);
            case REFLECT:     return tone(0.13, 2200, 1500, SINE, 0.24, 2.5);
            case PICKUP:      return sequence(SINE, 0.22, 0.07, 659, 880, 1175);
            case WIN:         return sequence(SQUARE, 0.14, 0.13, 523, 659, 784, 1047, 784, 1047);
            default:          return new byte[0];
        }
    }

    /** สร้างเสียงเดี่ยว: ความถี่เลื่อนจาก f0 ไป f1 ค่อย ๆ เบาลงตาม decay */
    private static byte[] tone(double dur, double f0, double f1, int wave, double vol, double decay) {
        int n = (int) (dur * RATE);
        byte[] out = new byte[n * 2];
        Random rnd = new Random(42);
        double phase = 0;

        for (int i = 0; i < n; i++) {
            double t = (double) i / n;
            phase += 2 * Math.PI * (f0 + (f1 - f0) * t) / RATE;

            double s;
            switch (wave) {
                case SQUARE: s = Math.sin(phase) >= 0 ? 1 : -1; break;
                case SAW:    s = 2 * ((phase / (2 * Math.PI)) % 1.0) - 1; break;
                case NOISE:  s = rnd.nextDouble() * 2 - 1; break;
                default:     s = Math.sin(phase);
            }

            double attack = Math.min(1.0, i / (RATE * 0.003)); // กันเสียงดีดตอนเริ่ม
            double env = attack * Math.pow(1 - t, decay);
            short v = (short) (s * env * vol * Short.MAX_VALUE);
            out[2 * i] = (byte) (v & 0xff);
            out[2 * i + 1] = (byte) ((v >> 8) & 0xff);
        }
        return out;
    }

    /** เรียงโน้ตต่อกัน */
    private static byte[] sequence(int wave, double vol, double noteDur, double... freqs) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        for (double f : freqs) {
            byte[] note = tone(noteDur, f, f, wave, vol, 1.0);
            bos.write(note, 0, note.length);
        }
        return bos.toByteArray();
    }
}
