import java.awt.*;
import java.awt.event.*;

public class keytest {
    public static void main(String[] args) {
        Frame f = new Frame("Key Test");
        f.setSize(300, 100);
        f.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                System.out.println("keyPressed: keyCode=" + e.getKeyCode()
                    + " VK_name=" + KeyEvent.getKeyText(e.getKeyCode())
                    + " keyChar=" + ((int)e.getKeyChar()) + "('" + e.getKeyChar() + "')");
            }
            public void keyTyped(KeyEvent e) {
                System.out.println("keyTyped:  keyChar=" + ((int)e.getKeyChar()) + "('" + e.getKeyChar() + "')");
            }
        });
        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) { System.exit(0); }
        });
        f.setVisible(true);
        System.out.println("Nhan phim bat ky tren cua so nay...");
    }
}
