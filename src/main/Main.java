package main;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("My Game");
            GamePanel panel = new GamePanel();

            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setResizable(false);
            frame.add(panel);
            frame.pack();                     // ขนาดหน้าต่างตาม GamePanel
            frame.setLocationRelativeTo(null); // กลางจอ
            frame.setVisible(true);

            panel.startGameLoop();
        });
    }
}
