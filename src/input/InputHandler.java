package input;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

/** เก็บสถานะปุ่มที่กดอยู่ ให้ส่วนอื่นของเกมมาอ่านค่า */
public class InputHandler extends KeyAdapter {
    public volatile boolean left, right;

    @Override
    public void keyPressed(KeyEvent e) {
        set(e.getKeyCode(), true);
    }

    @Override
    public void keyReleased(KeyEvent e) {
        set(e.getKeyCode(), false);
    }

    private void set(int key, boolean down) {
        if (key == KeyEvent.VK_A || key == KeyEvent.VK_LEFT)  left = down;
        if (key == KeyEvent.VK_D || key == KeyEvent.VK_RIGHT) right = down;
    }

    /** เรียกตอนเริ่มด่านใหม่ กันปุ่มค้าง */
    public void reset() {
        left = false;
        right = false;
    }
}
