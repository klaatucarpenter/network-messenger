package chat.app;

import chat.protocol.Protocol;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;

/**
 * Swing-based reference UI for the simple chat application.
 * <p>
 * This class builds a minimal messenger-like UI and delegates all networking and protocol
 * handling to {@link ChatClient}. It implements {@link ChatView} to receive events from
 * the client. All UI updates occur on the Swing Event Dispatch Thread.
 * </p>
 */
public class ChatApp implements ChatView {

    /**
     * Launches the Swing chat application.
     *
     * @param args ignored; no arguments are required
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatApp().show());
    }

    private ChatClient client;
    private String nick;

    private JFrame frame;
    private DefaultListModel<String> usersModel;
    private JPanel messagesPanel;
    private JScrollPane messagesScroll;
    private JTextField messageField;
    private JLabel headerTitle;
    private JLabel headerSubtitle;

    private static final Color BG_APP = new Color(244, 246, 255);
    private static final Color BG_SIDEBAR = new Color(26, 30, 60);
    private static final Color BG_CHAT = new Color(255, 255, 255);
    private static final Color BG_BUBBLE_ME = new Color(111, 97, 255);
    private static final Color BG_BUBBLE_THEM = new Color(235, 238, 252);
    private static final Color BG_INPUT = new Color(245, 246, 252);

    private static final Color FG_PRIMARY = new Color(17, 24, 39);
    private static final Color FG_MUTED = new Color(148, 163, 184);
    private static final Color FG_ON_PRIMARY = Color.WHITE;
    private static final float BUBBLE_MAX_PARENT_WIDTH = 0.7f;

    private void show() {
        frame = new JFrame("Simple Chat");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.getContentPane().setBackground(BG_APP);
        frame.setLayout(new BorderLayout());

        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                usersListPanel(),
                chatPanel());
        mainSplit.setResizeWeight(0.28);
        mainSplit.setContinuousLayout(true);

        frame.add(mainSplit, BorderLayout.CENTER);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (client != null) client.disconnect();
            }
        });

        frame.setSize(1200, 800);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        SwingUtilities.invokeLater(this::connectAndLogin);
    }

    private JPanel usersListPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 8));
        panel.setBackground(BG_SIDEBAR);

        JLabel title = new JLabel("Users");
        title.setForeground(Color.WHITE);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        panel.add(title, BorderLayout.NORTH);

        usersModel = new DefaultListModel<>();
        JList<String> conversationsList = new JList<>(usersModel);
        conversationsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        conversationsList.setBackground(BG_SIDEBAR);
        conversationsList.setForeground(Color.WHITE);
        conversationsList.setSelectionBackground(new Color(64, 70, 110));
        conversationsList.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        JScrollPane scroll = new JScrollPane(conversationsList);
        scroll.setBorder(null);
        scroll.getViewport().setBackground(BG_SIDEBAR);

        panel.add(scroll, BorderLayout.CENTER);

        return panel;
    }

    private JPanel chatPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(16, 8, 16, 16));
        panel.setBackground(BG_APP);

        JPanel card = new JPanel(new BorderLayout(8, 8));
        card.setBackground(BG_CHAT);
        card.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        card.add(chatHeader(), BorderLayout.NORTH);
        card.add(messagesArea(), BorderLayout.CENTER);
        card.add(inputArea(), BorderLayout.SOUTH);

        panel.add(card, BorderLayout.CENTER);

        return panel;
    }

    private JComponent chatHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));

        headerTitle = new JLabel("Design chat");
        headerTitle.setFont(headerTitle.getFont().deriveFont(Font.BOLD, 20f));
        headerTitle.setForeground(FG_PRIMARY);

        headerSubtitle = new JLabel("23 members");
        headerSubtitle.setFont(headerSubtitle.getFont().deriveFont(13f));
        headerSubtitle.setForeground(FG_MUTED);

        titlePanel.add(headerTitle);
        titlePanel.add(Box.createVerticalStrut(2));
        titlePanel.add(headerSubtitle);

        header.add(titlePanel, BorderLayout.WEST);

        return header;
    }

    private JComponent messagesArea() {
        messagesPanel = new JPanel();
        messagesPanel.setLayout(new BoxLayout(messagesPanel, BoxLayout.Y_AXIS));
        messagesPanel.setBackground(BG_CHAT);

        messagesScroll = new JScrollPane(messagesPanel);
        messagesScroll.setBorder(null);
        messagesScroll.getViewport().setBackground(BG_CHAT);
        messagesScroll.getVerticalScrollBar().setUnitIncrement(16);
        messagesScroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        return messagesScroll;
    }

    private static class RoundedPanel extends JPanel {
        private final int radius;

        RoundedPanel(int radius) {
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    private JLabel createWrappingLabel(String text, boolean fromMe) {
        JLabel label = new JLabel("<html>" + text + "</html>");
        label.setFont(label.getFont().deriveFont(13f));
        label.setForeground(fromMe ? FG_ON_PRIMARY : FG_PRIMARY);

        Dimension natural = label.getPreferredSize();
        int parentWidth = messagesScroll.getViewport().getWidth();

        int maxWidth = (int) (parentWidth * BUBBLE_MAX_PARENT_WIDTH);

        if (natural.width > maxWidth) {
            String html = "<html><body style='width:" + maxWidth + "px'>" + text + "</body></html>";
            label.setText(html);
        }

        return label;
    }

    private JComponent messageBubble(String author, String text, boolean isPrivate) {
        boolean fromMe = nick.equals(author);
        JPanel outer = new JPanel(new BorderLayout());
        outer.setOpaque(false);
        outer.setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));

        RoundedPanel bubble = new RoundedPanel(18);
        bubble.setLayout(new BoxLayout(bubble, BoxLayout.Y_AXIS));
        bubble.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));
        bubble.setBackground(fromMe ? BG_BUBBLE_ME : BG_BUBBLE_THEM);

        JLabel authorLabel = new JLabel(fromMe ? "You" : author);
        authorLabel.setFont(authorLabel.getFont().deriveFont(Font.BOLD, 12f));
        authorLabel.setForeground(fromMe ? FG_ON_PRIMARY : FG_PRIMARY);

        if (isPrivate) {
            JLabel dmLabel = new JLabel(fromMe ? "Direct message" : "Direct message to you");
            dmLabel.setFont(dmLabel.getFont().deriveFont(11f));
            dmLabel.setForeground(fromMe ? FG_ON_PRIMARY : FG_MUTED);
            bubble.add(dmLabel);
        }

        JLabel textLabel = createWrappingLabel(text, fromMe);

        bubble.add(authorLabel);
        bubble.add(Box.createVerticalStrut(4));
        bubble.add(textLabel);

        if (fromMe) {
            outer.add(bubble, BorderLayout.EAST);
        } else {
            outer.add(bubble, BorderLayout.WEST);
        }

        Dimension pref = outer.getPreferredSize();
        outer.setMaximumSize(new Dimension(Integer.MAX_VALUE, pref.height));

        return outer;
    }

    private JComponent inputArea() {
        JPanel inputPanel = new JPanel(new BorderLayout(8, 8));
        inputPanel.setBorder(BorderFactory.createEmptyBorder(12, 0, 0, 0));
        inputPanel.setOpaque(false);

        JPanel inner = new JPanel(new BorderLayout(8, 8));
        inner.setBackground(BG_INPUT);
        inner.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 8));

        messageField = new JTextField();
        messageField.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        messageField.setBackground(Color.WHITE);
        messageField.setToolTipText("Your message");
        messageField.addActionListener(e -> sendCurrentText());

        JButton sendButton = new JButton("Send");
        sendButton.setFocusPainted(false);

        JPanel rightButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        rightButtons.setOpaque(false);
        rightButtons.add(sendButton);
        sendButton.addActionListener(e -> sendCurrentText());

        inner.add(messageField, BorderLayout.CENTER);
        inner.add(rightButtons, BorderLayout.EAST);

        inputPanel.add(inner, BorderLayout.CENTER);
        return inputPanel;
    }

    private void addMessage(String author, String text, boolean isPrivate) {
        messagesPanel.add(messageBubble(author, text, isPrivate));
        messagesPanel.add(Box.createVerticalStrut(8));
        messagesPanel.revalidate();
        messagesPanel.repaint();
        SwingUtilities.invokeLater(() -> {
            JScrollBar bar = messagesScroll.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }

    private void appendSystemMessage(String text) {
        addMessage("System", text, false);
    }

    private void sendCurrentText() {
        String text = messageField.getText();
        if (text == null) return;
        text = text.trim();
        if (text.isEmpty()) return;
        if (text.toLowerCase().startsWith("dm ")) {
            String rest = text.substring("dm ".length()).trim();
            int sp = rest.indexOf(' ');
            if (sp <= 0) {
                appendSystemMessage("Usage: DM nick message");
            } else {
                String to = rest.substring(0, sp);
                String msg = rest.substring(sp + 1);
                if (client != null) client.sendPrivate(to, msg);
            }
        } else {
            if (client != null) client.sendPublic(text);
        }
        messageField.setText("");
    }

    private void showErrorAndClose(String message) {
        JOptionPane.showMessageDialog(frame, message, "Error", JOptionPane.ERROR_MESSAGE);
        if (client != null) client.disconnect();
        if (frame != null) frame.dispose();
    }

    private void connectAndLogin() {
        String host = "127.0.0.1";
        int port = 5000;

        try {
            client = new ChatClient(this);
            client.connect(host, port);
        } catch (IOException e) {
            showErrorAndClose("Cannot connect to server at " + host + ":" + port + "\n" + e.getMessage());
            return;
        }

        while (true) {
            String proposed = JOptionPane.showInputDialog(frame, "Choose your nick (max " + Protocol.MAX_NICK_LENGTH + "):", "Login", JOptionPane.QUESTION_MESSAGE);
            if (proposed == null) {
                if (client != null) client.disconnect();
                frame.dispose();
                return;
            }
            proposed = proposed.trim();
            String error = client.login(proposed);
            if (error == null) {
                this.nick = proposed;
                break;
            } else if (Protocol.ERR_NICK_TAKEN.equals(error) || Protocol.ERR_INVALID_NICK.equals(error)) {
                JOptionPane.showMessageDialog(frame, error, "Login failed", JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(frame, "Login failed: " + error, "Login failed", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    @Override
    public void onConnected(String nick) {
        headerTitle.setText("Room");
        headerSubtitle.setText("Logged in as " + nick);
        appendSystemMessage("Connected as " + nick);
    }

    @Override
    public void onPublicMessage(String from, String text) {
        addMessage(from, text, false);
    }

    @Override
    public void onPrivateMessage(String from, String to, String text) {
        boolean fromMe = nick != null && nick.equals(from);
        boolean toMe = nick != null && nick.equals(to);
        if (fromMe && !toMe) {
            addMessage(from, "[to " + to + "] " + text, true);
        } else {
            addMessage(from, text, true);
        }
    }

    @Override
    public void onUsers(java.util.List<String> users) {
        usersModel.clear();
        for (String u : users) usersModel.addElement(u);
    }

    @Override
    public void onSystemMessage(String text) {
        appendSystemMessage(text);
    }

    @Override
    public void onError(String error) {
        appendSystemMessage(error);
    }

    @Override
    public void onDisconnected() {
        appendSystemMessage("Disconnected.");
    }
}
