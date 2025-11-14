package chat.app;

import javax.swing.*;
import java.awt.*;

public class ChatApp {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatApp().show());
    }

    private void show() {
        JFrame frame = new JFrame("Simple Chat");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                conversationsPanel(),
                chatPanel());
        mainSplit.setResizeWeight(0.28);
        mainSplit.setContinuousLayout(true);

        frame.add(mainSplit, BorderLayout.CENTER);

        frame.setSize(1200, 800);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    private JPanel conversationsPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JTextField searchField = new JTextField();
        searchField.setText("Search");
        panel.add(searchField, BorderLayout.NORTH);

        DefaultListModel<String> model = new DefaultListModel<>();
        model.addElement("Kunta Kinte");
        model.addElement("Piętaszek Friday");
        model.addElement("Rädler Meletepa");

        JList<String> conversationsList = new JList<>(model);
        conversationsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        panel.add(new JScrollPane(conversationsList), BorderLayout.CENTER);

        return panel;
    }

    private JPanel chatPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        panel.add(chatHeader(), BorderLayout.NORTH);
        panel.add(messagesArea(), BorderLayout.CENTER);
        panel.add(inputArea(), BorderLayout.SOUTH);

        return panel;
    }

    private JComponent chatHeader() {
        JPanel header = new JPanel(new BorderLayout());

        JPanel titlePanel = new JPanel();
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("Design chat");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));

        JLabel subtitle = new JLabel("23 members");

        titlePanel.add(title);
        titlePanel.add(subtitle);

        header.add(titlePanel, BorderLayout.WEST);

        return header;
    }

    private JComponent messagesArea() {
        JPanel messagesPanel = new JPanel();
        messagesPanel.setLayout(new BoxLayout(messagesPanel, BoxLayout.Y_AXIS));

        messagesPanel.add(messageBubble("Kunta Kinte",
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Etiam sagittis sem et magna lacinia dictum. Donec vehicula molestie mi quis luctus."));
        messagesPanel.add(Box.createVerticalStrut(8));
        messagesPanel.add(messageBubble("Rädler Meletepa",
                "Nunc consequat ultrices rutrum. "));

        JScrollPane scroll = new JScrollPane(messagesPanel);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        return scroll;
    }

    private JComponent messageBubble(String author, String text) {
        JPanel bubble = new JPanel();
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel authorLabel = new JLabel(author);
        authorLabel.setFont(authorLabel.getFont().deriveFont(Font.BOLD));

        JLabel textLabel = new JLabel("<html>" + text + "</html>");

        bubble.add(authorLabel);
        bubble.add(Box.createVerticalStrut(4));
        bubble.add(textLabel);

        bubble.setOpaque(true);

        return bubble;
    }

    private JComponent inputArea() {
        JPanel inputPanel = new JPanel(new BorderLayout(8, 8));

        JTextField messageField = new JTextField();
        messageField.setToolTipText("Your message");

        JButton sendButton = new JButton("Send");

        JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        rightButtons.add(sendButton);

        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(rightButtons, BorderLayout.EAST);

        return inputPanel;
    }

}
