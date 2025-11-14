package chat.app;

import chat.protocol.Protocol;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ChatApp {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new ChatApp().show());
    }

    private Thread listenerThread;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private String nick;

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
        SwingUtilities.invokeLater(this::connectAndLogin);
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
        if (out == null) {
            appendSystemMessage("Not connected");
            return;
        }
        if (text.toLowerCase().startsWith("dm ")) {
            String rest = text.substring("dm ".length()).trim();
            int sp = rest.indexOf(' ');
            if (sp <= 0) {
                appendSystemMessage("Usage: DM nick message");
            } else {
                String to = rest.substring(0, sp);
                String msg = rest.substring(sp + 1);
                out.println(Protocol.PRIV + to + " " + msg);
            }
        } else {
            out.println(Protocol.MSG + text);
        }
        messageField.setText("");
    }

    private void showErrorAndClose(String message) {
        JOptionPane.showMessageDialog(frame, message, "Error", JOptionPane.ERROR_MESSAGE);
        disconnect();
        if (frame != null) frame.dispose();
    }

    private void connectAndLogin() {
        String host = "127.0.0.1";
        int port = 5000;

        try {
            socket = new Socket(host, port);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        } catch (IOException e) {
            showErrorAndClose("Cannot connect to server at " + host + ":" + port + "\n" + e.getMessage());
            return;
        }

        while (true) {
            String proposed = JOptionPane.showInputDialog(frame, "Choose your nick (max " + Protocol.MAX_NICK_LENGTH + "):", "Login", JOptionPane.QUESTION_MESSAGE);
            if (proposed == null) {
                disconnect();
                frame.dispose();
                return;
            }
            proposed = proposed.trim();
            out.println(Protocol.HANDSHAKE + proposed);
            String resp;
            try {
                resp = in.readLine();
            } catch (IOException e) {
                showErrorAndClose("Connection lost during login: " + e.getMessage());
                return;
            }

            if (resp == null) {
                showErrorAndClose("Server closed the connection.");
                return;
            }

            if (Protocol.WELCOME.equals(resp)) {
                this.nick = proposed;
                headerTitle.setText("Room");
                headerSubtitle.setText("Logged in as " + nick);
                appendSystemMessage("Connected as " + nick + ")");
                startListener();
                break;
            } else if (Protocol.ERR_NICK_TAKEN.equals(resp) || Protocol.ERR_INVALID_NICK.equals(resp)) {
                JOptionPane.showMessageDialog(frame, resp, "Login failed", JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(frame, "Unexpected response: " + resp, "Login failed", JOptionPane.WARNING_MESSAGE);
            }
        }
    }

    private void startListener() {
        listenerThread = new Thread(() -> {
            try {
                String line;
                while ((line = in.readLine()) != null) {
                    final String ln = line;
                    SwingUtilities.invokeLater(() -> handleIncoming(ln));
                }
            } catch (IOException ignored) {
            } finally {
                SwingUtilities.invokeLater(() -> appendSystemMessage("Disconnected."));
            }
        }, "chat/listener");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    private void handleIncoming(String line) {
        if (line.startsWith(Protocol.FROM)) {
            String rest = line.substring(Protocol.FROM.length());
            int sp = rest.indexOf(' ');
            if (sp > 0) {
                String from = rest.substring(0, sp);
                String msg = rest.substring(sp + 1);
                addMessage(from, msg);
            }
        } else if (line.startsWith(Protocol.PRIV_FROM)) {
            String rest = line.substring(Protocol.PRIV_FROM.length());
            int sp = rest.indexOf(' ');
            if (sp > 0) {
                String from = rest.substring(0, sp);
                String msg = rest.substring(sp + 1);
                if (nick.equals(from)) {
                    addMessage("[DM from me] ", msg);
                } else {
                    addMessage("[DM from " + from + "] ", msg);
                }
            }
        } else if (line.startsWith(Protocol.LIST_USERS)) {
            String csv = line.substring(Protocol.LIST_USERS.length());
            usersModel.clear();
            if (!csv.isEmpty()) {
                for (String u : csv.split(",")) {
                    if (!u.isBlank()) usersModel.addElement(u.trim());
                }
            }
        } else if (line.startsWith("ERROR")) {
            appendSystemMessage(line);
        } else {
            appendSystemMessage("? " + line);
        }
    }

    private void disconnect() {
        try {
            if (out != null) {
                out.println(Protocol.QUIT);
                out.flush();
            }
        } catch (Exception ignored) {
        }
        try {
            if (socket != null) socket.close();
        } catch (IOException ignored) {
        }
        out = null;
        in = null;
        socket = null;
    }
}
