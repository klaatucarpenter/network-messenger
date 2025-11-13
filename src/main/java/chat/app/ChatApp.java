package chat.app;

import javax.swing.*;
import java.awt.*;

public class ChatApp {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatApp().show());
    }

    private void show() {
        JFrame frame = new JFrame("Chat GUI");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(800, 600);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

}
