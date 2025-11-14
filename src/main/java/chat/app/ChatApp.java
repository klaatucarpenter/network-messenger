package chat.app;

import javax.swing.*;
import java.awt.*;

public class ChatApp {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatApp().show());
    }

    private JFrame frame;
    private DefaultListModel<String> usersModel;
    private JPanel messagesPanel;
    private JScrollPane messagesScroll;
    private JTextField messageField;
    private JLabel headerTitle;
    private JLabel headerSubtitle;

    private void show() {
        frame = new JFrame("Simple Chat");
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

        usersModel = new DefaultListModel<>();
        JList<String> conversationsList = new JList<>(usersModel);
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

        headerTitle = new JLabel("Design chat");
        headerTitle.setFont(headerTitle.getFont().deriveFont(Font.BOLD, 18f));

        headerSubtitle = new JLabel("23 members");

        titlePanel.add(headerTitle);
        titlePanel.add(headerSubtitle);

        header.add(titlePanel, BorderLayout.WEST);

        return header;
    }

    private JComponent messagesArea() {
        messagesPanel = new JPanel();
        messagesPanel.setLayout(new BoxLayout(messagesPanel, BoxLayout.Y_AXIS));

        messagesScroll = new JScrollPane(messagesPanel);
        messagesScroll.getVerticalScrollBar().setUnitIncrement(16);
        return messagesScroll;
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

        messageField = new JTextField();
        messageField.setToolTipText("Your message");
        messageField.addActionListener(e -> sendCurrentText());

        JButton sendButton = new JButton("Send");

        JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        rightButtons.add(sendButton);
        sendButton.addActionListener(e -> sendCurrentText());

        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(rightButtons, BorderLayout.EAST);

        return inputPanel;
    }

    private void addMessage(String author, String text) {
        messagesPanel.add(messageBubble(author, text));
        messagesPanel.add(Box.createVerticalStrut(8));
        messagesPanel.revalidate();
        messagesPanel.repaint();
        SwingUtilities.invokeLater(() -> {
            JScrollBar bar = messagesScroll.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }

    private void appendSystemMessage(String text) {
        addMessage("System", text);
    }

    private void sendCurrentText() {
        String text = messageField.getText();
        if (text == null) return;
        text = text.trim();
        if (text.isEmpty()) return;
        appendSystemMessage("> " + text);
        messageField.setText("");
    }
}
