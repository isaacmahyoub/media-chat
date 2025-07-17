package com.mmmi.mediachat.client;

import com.formdev.flatlaf.FlatLightLaf;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicTextAreaUI;
import javax.swing.text.*;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChatClient extends JFrame {

    private static JTextPane msg_area;
    private JTextArea msg_text;
    private JButton msg_send;
    private static JLabel clientStatusLabel;

    private JButton voiceCallButton;
    private JButton videoCallButton;
    private JLabel webcamDisplayLabel;
    private JLabel micActivityLabel;
    private JLabel cameraStatusLabel;
    private JButton fileTransferButton;
    private JProgressBar fileSendProgressBar;
    private JLabel fileSendStatusLabel;

    static Socket socket;
    static DataInputStream din;
    static DataOutputStream dout;

    // private static final String SERVER_ADDRESS = "localhost";
    // private static final String SERVER_ADDRESS = "0.0.0.0";
    private static final String SERVER_ADDRESS = "13.49.68.159";

    private static final int CHAT_PORT = 1201;
    private static AtomicBoolean clientRunning = new AtomicBoolean(false);

    private String username;

    private static final Color MY_MESSAGE_COLOR = new Color(208, 235, 255);
    private static final Color OTHER_MESSAGE_COLOR = new Color(232, 232, 250);
    private static final Color SYSTEM_MESSAGE_COLOR = new Color(220, 220, 220);
    private static final Color TEXT_COLOR = Color.BLACK;

    private static final Font MESSAGE_FONT = new Font("Segoe UI", Font.PLAIN, 13);

    public ChatClient(String username) {
        this.username = username;
        setupFlatLaf();
        initComponents();
        setupWindow();
        setupControlsState();
        FileSender.setFileTransferUI(fileSendProgressBar, fileSendStatusLabel);
        startClientConnectionThread();
    }

    private void setupFlatLaf() {
        try {
            UIManager.put("Button.arc", 10);
            UIManager.put("Component.arc", 10);
            UIManager.put("ProgressBar.arc", 5);
            UIManager.put("TextArea.roundRect", true);
            FlatLightLaf.setup();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupWindow() {
        setTitle("MediaChat Client - " + username);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(850, 800);
        setLocationRelativeTo(null);
    }

    private void setupControlsState() {
        setChatControlsEnabled(false);
        updateMediaButtonState(voiceCallButton, VoiceClient.isVoiceCallActive(), "Start Voice", "End Voice", new Color(77, 171, 247), new Color(220, 53, 69), null, null);
        updateMediaButtonState(videoCallButton, VideoClient.isVideoCallActive(), "Start Video", "End Video", new Color(77, 171, 247), new Color(220, 53, 69), null, null);
        updateFileTransferButtonState(FileSender.isSendingFile());

        micActivityLabel.setText("Mic: Off");
        micActivityLabel.setForeground(Color.GRAY);
        cameraStatusLabel.setText("Cam: Off");
        cameraStatusLabel.setForeground(Color.GRAY);
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new MigLayout("fill, insets 10", "[grow, fill]", "[][grow, fill][][][][][grow, fill]"));
        mainPanel.setBackground(new Color(240, 240, 240));

        createStatusPanel(mainPanel);
        createMessageArea(mainPanel);
        createInputPanel(mainPanel);
        createMediaControls(mainPanel);
        createFileTransferPanel(mainPanel);
        createVideoPanel(mainPanel);

        add(mainPanel);
        msg_text.requestFocusInWindow();
    }

    private void createStatusPanel(JPanel mainPanel) {
        JPanel statusPanel = new JPanel(new MigLayout("fillx, insets 5", "[grow][right]", "[]"));
        statusPanel.setBackground(new Color(230, 240, 250));
        statusPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));

        clientStatusLabel = new JLabel("Connecting...");
        clientStatusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        clientStatusLabel.setForeground(new Color(50, 50, 50));
        statusPanel.add(clientStatusLabel, "growx");

        JPanel indicatorsPanel = new JPanel(new MigLayout("insets 0", "[][][]", "[]"));
        indicatorsPanel.setOpaque(false);

        micActivityLabel = createStatusIndicator("Mic: Off", Color.GRAY);
        cameraStatusLabel = createStatusIndicator("Cam: Off", Color.GRAY);

        indicatorsPanel.add(micActivityLabel, "gapx 10");
        indicatorsPanel.add(cameraStatusLabel, "gapx 10");

        statusPanel.add(indicatorsPanel, "wrap, right");
        mainPanel.add(statusPanel, "wrap, growx");
    }

    private JLabel createStatusIndicator(String text, Color initialColor) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        label.setForeground(initialColor);
        return label;
    }

    private void createMessageArea(JPanel mainPanel) {
        msg_area = new JTextPane();
        msg_area.setEditable(false);
        msg_area.setFont(MESSAGE_FONT);
        msg_area.setBackground(new Color(250, 250, 250));
        msg_area.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        msg_area.setContentType("text/html");

        JScrollPane scrollPane = new JScrollPane(msg_area);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        mainPanel.add(scrollPane, "grow, wrap");
    }

    private void createInputPanel(JPanel mainPanel) {
        JPanel inputPanel = new JPanel(new MigLayout("fillx, insets 0", "[grow, fill][]", "[]"));

        msg_text = new JTextArea(3, 20);
        msg_text.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        msg_text.setWrapStyleWord(true);
        msg_text.setLineWrap(true);
        msg_text.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 180, 180), 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));

        new PlaceholderRenderer(msg_text, "الرسالة");

        msg_text.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    if (e.isShiftDown()) {
                        msg_text.append("\n");
                    } else {
                        msg_sendActionPerformed(null);
                        e.consume();
                    }
                }
            }
        });

        JScrollPane msgScrollPane = new JScrollPane(msg_text);
        msgScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        msgScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        msgScrollPane.setBorder(BorderFactory.createEmptyBorder());

        inputPanel.add(msgScrollPane, "growx, height 70::");

        msg_send = new JButton("<html><div style='text-align: center;'>&#x27A4;</div></html>");
        msg_send.setFont(new Font("Segoe UI Emoji", Font.BOLD, 24));
        msg_send.putClientProperty("JButton.buttonType", "roundRect");
        msg_send.setBackground(new Color(0, 123, 255));
        msg_send.setForeground(Color.WHITE);
        msg_send.setFocusPainted(false);
        msg_send.setMargin(new Insets(0, 0, 0, 0));
        msg_send.addActionListener(this::msg_sendActionPerformed);
        inputPanel.add(msg_send, "width 50!, height 50!, aligny bottom, gapbottom 10");
        mainPanel.add(inputPanel, "growx, wrap");
    }

    private void createMediaControls(JPanel mainPanel) {
        JPanel mediaControlPanel = new JPanel(new MigLayout("insets 0, alignx center", "[grow][grow][grow]", "[]"));
        mediaControlPanel.setBorder(BorderFactory.createTitledBorder("Media Controls"));

        voiceCallButton = createMediaToggleButton("Start Voice", "End Voice", VoiceClient.isVoiceCallActive(), new Color(77, 171, 247), new Color(220, 53, 69), null, null);
        videoCallButton = createMediaToggleButton("Start Video", "End Video", VideoClient.isVideoCallActive(), new Color(77, 171, 247), new Color(220, 53, 69), null, null);

        voiceCallButton.addActionListener(e -> toggleVoiceCall());
        videoCallButton.addActionListener(e -> toggleVideoCall());

        mediaControlPanel.add(new JLabel(), "growx");
        mediaControlPanel.add(voiceCallButton, "growx, width 150!, height 40!, sg mediaButton");
        mediaControlPanel.add(videoCallButton, "growx, width 150!, height 40!, sg mediaButton, wrap");

        mainPanel.add(mediaControlPanel, "growx, wrap");
    }

    private JButton createMediaToggleButton(String startText, String endText, boolean isActive, Color startColor, Color endColor, String startIconPath, String endIconPath) {
        JButton button = new JButton();
        button.setFont(new Font("Segoe UI", Font.BOLD, 13));
        button.putClientProperty("JButton.buttonType", "roundRect");
        button.setBackground(isActive ? endColor : startColor);
        button.setForeground(Color.WHITE);
        button.setFocusPainted(false);

        String currentIconPath = isActive ? endIconPath : startIconPath;
        if (currentIconPath != null && !currentIconPath.isEmpty()) {
            try {
                ImageIcon icon = new ImageIcon(getClass().getResource(currentIconPath));
                Image image = icon.getImage();
                Image scaledImage = image.getScaledInstance(20, 20, Image.SCALE_SMOOTH);
                button.setIcon(new ImageIcon(scaledImage));
                button.setText("");
            } catch (Exception e) {
                button.setText(isActive ? endText : startText);
            }
        } else {
            button.setText(isActive ? endText : startText);
        }

        return button;
    }

    private void updateMediaButtonState(JButton button, boolean isActive, String startText, String endText, Color startColor, Color endColor, String startIconPath, String endIconPath) {
        button.setBackground(isActive ? endColor : startColor);

        String currentIconPath = isActive ? endIconPath : startIconPath;
        if (currentIconPath != null && !currentIconPath.isEmpty()) {
            try {
                ImageIcon icon = new ImageIcon(getClass().getResource(currentIconPath));
                Image image = icon.getImage();
                Image scaledImage = image.getScaledInstance(20, 20, Image.SCALE_SMOOTH);
                button.setIcon(new ImageIcon(scaledImage));
                button.setText("");
            } catch (Exception e) {
                button.setIcon(null);
                button.setText(isActive ? endText : startText);
            }
        } else {
            button.setIcon(null);
            button.setText(isActive ? endText : startText);
        }
    }

    private void toggleVoiceCall() {
        String startText = "Start Voice";
        String endText = "End Voice";
        Color startColor = new Color(77, 171, 247);
        Color endColor = new Color(220, 53, 69);

        if (!VoiceClient.isVoiceCallActive()) {
            VoiceClient.startVoiceClient();
            appendSystemMessage("--- Voice Client Started ---");
            updateMediaButtonState(voiceCallButton, true, startText, endText, startColor, endColor, null, null);
            micActivityLabel.setForeground(new Color(40, 167, 69));
            micActivityLabel.setText("Mic: Active");
        } else {
            VoiceClient.stopVoiceClient();
            appendSystemMessage("--- Voice Client Stopped ---");
            updateMediaButtonState(voiceCallButton, false, startText, endText, startColor, endColor, null, null);
            micActivityLabel.setForeground(Color.GRAY);
            micActivityLabel.setText("Mic: Off");
        }
    }

    private void toggleVideoCall() {
        String startText = "Start Video";
        String endText = "End Video";
        Color startColor = new Color(77, 171, 247);
        Color endColor = new Color(220, 53, 69);

        if (!VideoClient.isVideoCallActive()) {
            VideoClient.setLocalVideoDisplayLabel(webcamDisplayLabel);
            VideoClient.startVideoClient();
            appendSystemMessage("--- Video Client Started ---");
            updateMediaButtonState(videoCallButton, true, startText, endText, startColor, endColor, null, null);
            cameraStatusLabel.setForeground(new Color(40, 167, 69));
            cameraStatusLabel.setText("Cam: Active");
        } else {
            VideoClient.stopVideoClient();
            appendSystemMessage("--- Video Client Stopped ---");
            updateMediaButtonState(videoCallButton, false, startText, endText, startColor, endColor, null, null);
            cameraStatusLabel.setForeground(Color.GRAY);
            cameraStatusLabel.setText("Cam: Off");
        }
    }

    private void createFileTransferPanel(JPanel mainPanel) {
        JPanel fileTransferPanel = new JPanel(new MigLayout("fillx, insets 0", "[grow, fill][]", "[]"));
        fileTransferPanel.setBorder(BorderFactory.createTitledBorder("File Transfer"));

        fileSendProgressBar = new JProgressBar(0, 100);
        fileSendProgressBar.setStringPainted(true);
        fileSendProgressBar.putClientProperty("JProgressBar.roundRect", true);
        fileTransferPanel.add(fileSendProgressBar, "growx, height 25!");

        fileTransferButton = createMediaToggleButton("📤 Send File", "⏹ Cancel Sending", FileSender.isSendingFile(), new Color(108, 117, 125), new Color(255, 193, 7), null, null);
        fileTransferButton.addActionListener(this::sendFileActionPerformed);
        fileTransferPanel.add(fileTransferButton, "width 180!, wrap");

        fileSendStatusLabel = new JLabel("Ready to send files.");
        fileSendStatusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        fileSendStatusLabel.setForeground(Color.GRAY);
        fileTransferPanel.add(fileSendStatusLabel, "span 2, growx");

        mainPanel.add(fileTransferPanel, "growx, wrap");
    }

    private void updateFileTransferButtonState(boolean isActive) {
        String startText = "📤 Send File";
        String stopText = "⏹ Cancel Sending";
        Color startColor = new Color(108, 117, 125);
        Color stopColor = new Color(255, 193, 7);

        if (isActive) {
            updateMediaButtonState(fileTransferButton, true, startText, stopText, startColor, stopColor, null, null);
            fileSendStatusLabel.setForeground(new Color(40, 167, 69));
            fileSendStatusLabel.setText("Sending...");
        } else {
            updateMediaButtonState(fileTransferButton, false, startText, stopText, startColor, stopColor, null, null);
            fileSendStatusLabel.setForeground(Color.GRAY);
            fileSendStatusLabel.setText("Ready to send files.");
        }
        fileTransferButton.setEnabled(true);
    }

    private void sendFileActionPerformed(ActionEvent evt) {
        if (FileSender.isSendingFile()) {
            JOptionPane.showMessageDialog(this, "Already sending a file. Please wait.", "File Transfer", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (dout == null) {
            JOptionPane.showMessageDialog(this, "Not connected to server. Please connect first.", "File Transfer Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Choose File to Send");
        int userSelection = fileChooser.showOpenDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSend = fileChooser.getSelectedFile();
            if (fileToSend.length() > (50 * 1024 * 1024)) {
                JOptionPane.showMessageDialog(this, "File is too large (max 50MB).", "File Too Large", JOptionPane.WARNING_MESSAGE);
                return;
            }
            if (fileToSend.length() == 0) {
                JOptionPane.showMessageDialog(this, "Cannot send empty file.", "Empty File", JOptionPane.WARNING_MESSAGE);
                return;
            }
            FileSender.sendFile(fileToSend);
        }
    }

    private void createVideoPanel(JPanel mainPanel) {
        JPanel localVideoPanel = new JPanel(new MigLayout("fill, insets 5", "[grow, fill]", "[grow, fill]"));
        localVideoPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(150, 150, 150)),
                "Local Webcam Feed"
        ));
        localVideoPanel.setBackground(Color.BLACK);

        webcamDisplayLabel = new JLabel();
        webcamDisplayLabel.setHorizontalAlignment(SwingConstants.CENTER);
        localVideoPanel.add(webcamDisplayLabel, "grow, push");

        VideoClient.setLocalVideoDisplayLabel(webcamDisplayLabel);
        VideoClient.setCameraStatusLabel(cameraStatusLabel);
        VoiceClient.setMicActivityLabel(micActivityLabel);

        mainPanel.add(localVideoPanel, "grow, push");
    }

    private void setChatControlsEnabled(boolean enabled) {
        msg_text.setEnabled(enabled);
        msg_send.setEnabled(enabled);
        voiceCallButton.setEnabled(enabled);
        videoCallButton.setEnabled(enabled);
        fileTransferButton.setEnabled(enabled);
    }

    private void msg_sendActionPerformed(ActionEvent evt) {
        try {
            String msgout = msg_text.getText().trim();
            if (!msgout.isEmpty()) {
                if (dout != null) {
                    String fullMessage = username + ": " + msgout;
                    dout.writeUTF(fullMessage);
                    appendMyMessage(fullMessage);
                    msg_text.setText("");
                    msg_text.requestFocusInWindow();
                } else {
                    appendSystemMessage("Error: Not connected to server or output stream not ready.");
                }
            }
        } catch (IOException ex) {
            appendSystemMessage("Error sending message: " + ex.getMessage());
        } catch (Exception ex) {
            appendSystemMessage("An unexpected error occurred: " + ex.getMessage());
        }
    }

    private static void appendMyMessage(String fullMessage) {
        String[] parts = fullMessage.split(":", 2);
        String sender = parts[0];
        String messageContent = (parts.length > 1) ? parts[1].trim() : "";
        appendMessage(messageContent, MY_MESSAGE_COLOR, TEXT_COLOR, SwingConstants.RIGHT, sender);
    }

    private static void appendOtherMessage(String fullMessage) {
        String[] parts = fullMessage.split(":", 2);
        String sender = parts[0];
        String messageContent = (parts.length > 1) ? parts[1].trim() : "";
        appendMessage(messageContent, OTHER_MESSAGE_COLOR, TEXT_COLOR, SwingConstants.LEFT, sender);
    }

    private static void appendMessage(String messageContent, Color bgColor, Color fgColor, int alignment, String sender) {
        SwingUtilities.invokeLater(() -> {
            HTMLEditorKit editorKit = (HTMLEditorKit) msg_area.getEditorKit();
            HTMLDocument doc = (HTMLDocument) msg_area.getDocument();

            String displayName = (alignment == SwingConstants.RIGHT) ? "You" : sender;

            String borderRadius = (alignment == SwingConstants.RIGHT)
                    ? "border-radius: 12px 12px 0 12px;"
                    : "border-radius: 12px 12px 12px 0;";

            String marginStyle = (alignment == SwingConstants.RIGHT)
                    ? "margin-left: auto; margin-right: 5px;"
                    : "margin-right: auto; margin-left: 5px;";

            String htmlContent = String.format(
                    "<div style='"
                    + "display: flex;"
                    + "width: 100%%;"
                    + "justify-content: %s;"
                    + "margin: 8px 0;"
                    + "'>"
                    + "<div style='"
                    + "background-color: %s;"
                    + "color: %s;"
                    + "padding: 10px 14px;"
                    + "%s"
                    + "%s"
                    + "max-width: 75%%;"
                    + "min-width: 60px;"
                    + "overflow-wrap: break-word;"
                    + "box-shadow: 0 1px 2px rgba(0,0,0,0.1);"
                    + "font-size: 14px;"
                    + "line-height: 1.5;"
                    + "text-align: %s;"
                    + "'>"
                    + "<div style='font-size: 0.85em; color: #5E5E5E; margin-bottom: 5px; font-weight: bold;'>"
                    + "%s"
                    + "</div>"
                    + "%s"
                    + "</div>"
                    + "</div>",
                    (alignment == SwingConstants.RIGHT) ? "flex-end" : "flex-start",
                    toHexString(bgColor),
                    toHexString(fgColor),
                    borderRadius,
                    marginStyle,
                    (alignment == SwingConstants.RIGHT) ? "right" : "left",
                    escapeHtml(displayName),
                    escapeHtml(messageContent)
            );

            try {
                editorKit.insertHTML(doc, doc.getLength(), htmlContent, 0, 0, null);
                msg_area.setCaretPosition(doc.getLength());
            } catch (BadLocationException | IOException e) {
                e.printStackTrace();
            }
        });
    }

    private static void appendSystemMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            HTMLEditorKit editorKit = (HTMLEditorKit) msg_area.getEditorKit();
            HTMLDocument doc = (HTMLDocument) msg_area.getDocument();

            String htmlContent = String.format(
                    "<div style='"
                    + "text-align: center;"
                    + "margin: 12px 0;"
                    + "'>"
                    + "<span style='"
                    + "display: inline-block;"
                    + "background-color: #f0f0f0;"
                    + "color: #666;"
                    + "padding: 6px 16px;"
                    + "border-radius: 14px;"
                    + "font-size: 0.86em;"
                    + "font-style: italic;"
                    + "box-shadow: 0 1px 2px rgba(0,0,0,0.05);"
                    + "'>"
                    + "%s"
                    + "</span>"
                    + "</div>",
                    escapeHtml(message)
            );

            try {
                editorKit.insertHTML(doc, doc.getLength(), htmlContent, 0, 0, null);
                msg_area.setCaretPosition(doc.getLength());
            } catch (BadLocationException | IOException e) {
                e.printStackTrace();
            }
        });
    }

    private static String toHexString(Color color) {
        return String.format("#%02X%02X%02X", color.getRed(), color.getGreen(), color.getBlue());
    }

    private static String escapeHtml(String text) {
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;")
                .replace("/", "&#x2F;")
                .replace("\n", "<br/>");
    }

    private void startClientConnectionThread() {
    new Thread(() -> {
        clientRunning.set(true);
        while (clientRunning.get()) {
            try {
                String serverAddress = SERVER_ADDRESS;
                int port = CHAT_PORT;

                SwingUtilities.invokeLater(() -> clientStatusLabel.setText("Connecting to " + serverAddress + ":" + port + "..."));
                appendSystemMessage("Attempting to connect to server...");

                socket = new Socket(serverAddress, port);

                din = new DataInputStream(socket.getInputStream());
                dout = new DataOutputStream(socket.getOutputStream());

                dout.writeUTF("USERNAME:" + username);

                SwingUtilities.invokeLater(() -> {
                    clientStatusLabel.setText("Connected to Server as " + username + "!");
                    setChatControlsEnabled(true);
                });
                appendSystemMessage("--- Connected to Server as " + username + " ---");

                String msgin = "";
                while (socket.isConnected() && !socket.isClosed() && clientRunning.get()) {
                    msgin = din.readUTF();
                    final String receivedMsg = msgin;
                    SwingUtilities.invokeLater(() -> appendOtherMessage(receivedMsg));
                }
                break; // خرج من الحلقة إذا تم الاتصال وانتهى
            } catch (ConnectException e) {
                SwingUtilities.invokeLater(() -> {
                    clientStatusLabel.setText("Waiting for server...");
                    setChatControlsEnabled(false);
                });
                appendSystemMessage("Waiting for server to start...");
                try { Thread.sleep(2000); } catch (InterruptedException ex) { /* ignore */ }
            } catch (IOException e) {
                String errorMessage = e.getMessage();
                if (errorMessage != null && errorMessage.contains("Connection reset by peer")) {
                    errorMessage = "Server disconnected unexpectedly.";
                }
                final String displayError = errorMessage != null ? errorMessage : "An unknown network error occurred.";

                SwingUtilities.invokeLater(() -> {
                    clientStatusLabel.setText("Disconnected: " + displayError);
                    setChatControlsEnabled(false);
                });
                appendSystemMessage("DISCONNECTED: " + displayError);
                try { Thread.sleep(2000); } catch (InterruptedException ex) { /* ignore */ }
            } finally {
                try {
                    if (din != null) din.close();
                    if (dout != null) dout.close();
                    if (socket != null) socket.close();
                    SwingUtilities.invokeLater(() -> clientStatusLabel.setText("Client Disconnected."));
                    appendSystemMessage("--- Client Disconnected ---");
                } catch (IOException e) {
                    appendSystemMessage("Error closing resources: " + e.getMessage());
                }
            }
        }
    }, "ChatClientConnectionThread").start();
}

    @Override
    protected void processWindowEvent(java.awt.event.WindowEvent e) {
        if (e.getID() == java.awt.event.WindowEvent.WINDOW_CLOSING) {
            clientRunning.set(false);
            if (VoiceClient.isVoiceCallActive()) {
                VoiceClient.stopVoiceClient();
            }
            if (VideoClient.isVideoCallActive()) {
                VideoClient.stopVideoClient();
            }
            dispose();
        }
        super.processWindowEvent(e);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                FlatLightLaf.setup();
            } catch (Exception e) {
                e.printStackTrace();
            }
            new ChatClient("Client").setVisible(true);
        });
    }

    // --- Inner Class for Placeholder Text ---
    private static class PlaceholderRenderer extends FocusAdapter implements DocumentListener {

        private final JTextArea textArea;
        private final String placeholder;
        private boolean showPlaceholder = true;

        public PlaceholderRenderer(JTextArea textArea, String placeholder) {
            this.textArea = textArea;
            this.placeholder = placeholder;
            textArea.getDocument().addDocumentListener(this);
            textArea.addFocusListener(this);
            updatePlaceholderVisibility();

            textArea.setUI(new BasicTextAreaUI() {
                @Override
                protected void paintSafely(Graphics g) {
                    super.paintSafely(g);
                    if (showPlaceholder) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setColor(textArea.getDisabledTextColor());
                        g2.setFont(textArea.getFont());

                        Insets insets = textArea.getInsets();
                        FontMetrics fm = g2.getFontMetrics();
                        int x = insets.left + 5;
                        int y = insets.top + fm.getAscent();

                        g2.drawString(placeholder, x, y);
                        g2.dispose();
                    }
                }
            });
        }

        private void updatePlaceholderVisibility() {
            showPlaceholder = textArea.getText().isEmpty() && !textArea.hasFocus();
            textArea.repaint();
        }

        @Override
        public void insertUpdate(DocumentEvent e) {
            updatePlaceholderVisibility();
        }

        @Override
        public void removeUpdate(DocumentEvent e) {
            updatePlaceholderVisibility();
        }

        @Override
        public void changedUpdate(DocumentEvent e) {
            updatePlaceholderVisibility();
        }

        @Override
        public void focusGained(FocusEvent e) {
            updatePlaceholderVisibility();
        }

        @Override
        public void focusLost(FocusEvent e) {
            updatePlaceholderVisibility();
        }
    }
}