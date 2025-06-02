package com.mmmi.mediachat.server;

import com.formdev.flatlaf.FlatLightLaf;
import net.miginfocom.swing.MigLayout;

import javax.swing.ImageIcon;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicTextAreaUI;
import javax.swing.text.BadLocationException;
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
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.text.DefaultCaret;
import java.awt.image.BufferedImage;

public class ChatServer extends JFrame {

    private static JTextPane msg_area;
    private JTextArea msg_text;
    private JButton msg_send;
    private static JLabel serverStatusLabel;

    private JButton voiceCallButton;
    private JButton videoCallButton;
    private JLabel receivedVideoLabel;
    private JLabel voiceActivityLabel;
    private JLabel receivedVideoStatusLabel;
    private JButton fileTransferButton;
    private JProgressBar fileReceiveProgressBar;
    private JLabel fileReceiveStatusLabel;

    static ServerSocket serversocket;
    static Socket socket;
    static DataInputStream din;
    static DataOutputStream dout;

    private static final int CHAT_PORT = 1201;
    private static AtomicBoolean serverRunning = new AtomicBoolean(false);

    private String connectedClientUsername = "Unknown Client";

    private static final Color MY_MESSAGE_COLOR = new Color(208, 235, 255);
    private static final Color OTHER_MESSAGE_COLOR = new Color(232, 232, 250);
    private static final Color SYSTEM_MESSAGE_COLOR = new Color(200, 200, 200);
    private static final Color TEXT_COLOR = Color.BLACK;

    private static final Font MESSAGE_FONT = new Font("Segoe UI", Font.PLAIN, 13);

    public ChatServer() {
        setupFlatLaf();
        initComponents();
        setupWindow();
        setupControlsState();
        FileReceiver.setFileTransferUI(fileReceiveProgressBar, fileReceiveStatusLabel);
        startServerThread();
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
        setTitle("MediaChat Server");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(850, 800);
        setLocationRelativeTo(null);
    }

    private void setupControlsState() {
        setChatControlsEnabled(false);
        updateMediaButtonState(voiceCallButton, VoiceServer.isVoiceCallActive(), "📞 Start Voice", "📞 End Voice", new Color(77, 171, 247), new Color(220, 53, 69), "/icons/call_icon.png", "/icons/end_call_icon.png");
        updateMediaButtonState(videoCallButton, VideoServer.isVideoCallActive(), "📹 Start Video", "📹 End Video", new Color(77, 171, 247), new Color(220, 53, 69), "/icons/video_icon.png", "/icons/end_video_icon.png");
        updateFileTransferButtonState(FileReceiver.isFileReceiverActive());

        voiceActivityLabel.setText("Voice: Off");
        voiceActivityLabel.setForeground(Color.GRAY);
        receivedVideoStatusLabel.setText("Video: Off");
        receivedVideoStatusLabel.setForeground(Color.GRAY);
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

        serverStatusLabel = new JLabel("Initializing Server...");
        serverStatusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        serverStatusLabel.setForeground(new Color(50, 50, 50));
        statusPanel.add(serverStatusLabel, "growx");

        JPanel indicatorsPanel = new JPanel(new MigLayout("insets 0", "[][][]", "[]"));
        indicatorsPanel.setOpaque(false);

        voiceActivityLabel = createStatusIndicator("Voice: Off", Color.GRAY);
        receivedVideoStatusLabel = createStatusIndicator("Video: Off", Color.GRAY);

        indicatorsPanel.add(voiceActivityLabel, "gapx 10");
        indicatorsPanel.add(receivedVideoStatusLabel, "gapx 10");

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
        DefaultCaret caret = (DefaultCaret) msg_area.getCaret();
        caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
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

        new PlaceholderRenderer(msg_text, "اكتب رسالتك...");

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
        JPanel mediaControlPanel = new JPanel(new MigLayout("fillx, insets 0", "[grow, fill][grow, fill]", "[]"));
        mediaControlPanel.setBorder(BorderFactory.createTitledBorder("Media Controls"));
        voiceCallButton = createMediaToggleButton("/icons/add_call.png", "📞 End Voice", VoiceServer.isVoiceCallActive(), new Color(77, 171, 247), new Color(220, 53, 69), "/icons/call_icon.png", "/icons/call_end.png");
        videoCallButton = createMediaToggleButton("📹 Start Video", "📹 End Video", VideoServer.isVideoCallActive(), new Color(77, 171, 247), new Color(220, 53, 69), "/icons/video_icon.png", "/icons/video_camera_front_off.png");

        voiceCallButton.addActionListener(e -> toggleVoiceCall());
        videoCallButton.addActionListener(e -> toggleVideoCall());

        mediaControlPanel.add(voiceCallButton, "growx");
        mediaControlPanel.add(videoCallButton, "growx, wrap");

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
                System.err.println("Error loading icon from " + currentIconPath + ": " + e.getMessage());
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
                System.err.println("Error loading icon from " + currentIconPath + ": " + e.getMessage());
                button.setIcon(null);
                button.setText(isActive ? endText : startText);
            }
        } else {
            button.setIcon(null);
            button.setText(isActive ? endText : startText);
        }
    }


    private void toggleVoiceCall() {
        if (!VoiceServer.isVoiceCallActive()) {
            VoiceServer.startVoiceServer();
            appendSystemMessage("--- Voice Server Started ---");
            updateMediaButtonState(voiceCallButton, true, "📞 Start Voice", "📞 End Voice", new Color(77, 171, 247), new Color(220, 53, 69), "/icons/call_icon.png", "/icons/end_call_icon.png");
            voiceActivityLabel.setForeground(new Color(40, 167, 69));
            voiceActivityLabel.setText("Voice: Active");
        } else {
            VoiceServer.stopVoiceServer();
            appendSystemMessage("--- Voice Server Stopped ---");
            updateMediaButtonState(voiceCallButton, false, "📞 Start Voice", "📞 End Voice", new Color(77, 171, 247), new Color(220, 53, 69), "/icons/call_icon.png", "/icons/end_call_icon.png");
            voiceActivityLabel.setForeground(Color.GRAY);
            voiceActivityLabel.setText("Voice: Off");
        }
    }

    private void toggleVideoCall() {
        if (!VideoServer.isVideoCallActive()) {
            VideoServer.startVideoServer();
            appendSystemMessage("--- Video Server Started ---");
            updateMediaButtonState(videoCallButton, true, "/icons/video_call.png", "/icons/video_camera_front_off.png", new Color(77, 171, 247), new Color(220, 53, 69), "/icons/video_icon.png", "/icons/end_video_icon.png");
            receivedVideoStatusLabel.setForeground(new Color(40, 167, 69));
            receivedVideoStatusLabel.setText("Video: Active");
        } else {
            VideoServer.stopVideoServer();
            appendSystemMessage("--- Video Server Stopped ---");
            updateMediaButtonState(videoCallButton, false, "📹 Start Video", "📹 End Video", new Color(77, 171, 247), new Color(220, 53, 69), "/icons/video_icon.png", "/icons/end_video_icon.png");
            receivedVideoStatusLabel.setForeground(Color.GRAY);
            receivedVideoStatusLabel.setText("Video: Off");
        }
    }

    private void createFileTransferPanel(JPanel mainPanel) {
        JPanel fileTransferPanel = new JPanel(new MigLayout("fillx, insets 0", "[grow, fill][]", "[]"));
        fileTransferPanel.setBorder(BorderFactory.createTitledBorder("File Reception"));

        fileReceiveProgressBar = new JProgressBar(0, 100);
        fileReceiveProgressBar.setStringPainted(true);
        fileReceiveProgressBar.putClientProperty("JProgressBar.roundRect", true);
        fileTransferPanel.add(fileReceiveProgressBar, "growx, height 25!");

        fileTransferButton = createMediaToggleButton("📥 Start File Receiver", "⏹ Stop File Receiver", FileReceiver.isFileReceiverActive(), new Color(108, 117, 125), new Color(255, 193, 7), "/icons/download_icon.png", "/icons/stop_icon.png");
        fileTransferButton.addActionListener(this::toggleFileReceiverActionPerformed);
        fileTransferPanel.add(fileTransferButton, "width 180!, wrap");

        fileReceiveStatusLabel = new JLabel("File Server Off");
        fileReceiveStatusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        fileReceiveStatusLabel.setForeground(Color.GRAY);
        fileTransferPanel.add(fileReceiveStatusLabel, "span 2, growx");

        mainPanel.add(fileTransferPanel, "growx, wrap");
    }

    private void updateFileTransferButtonState(boolean isActive) {
        String startText = "📥 Start File Receiver";
        String stopText = "⏹ Stop File Receiver";
        Color startColor = new Color(108, 117, 125);
        Color stopColor = new Color(255, 193, 7);
        String startIconPath = "/icons/download_icon.png";
        String stopIconPath = "/icons/stop_icon.png";

        if (isActive) {
            updateMediaButtonState(fileTransferButton, true, startText, stopText, startColor, stopColor, startIconPath, stopIconPath);
            fileReceiveStatusLabel.setForeground(new Color(40, 167, 69));
            fileReceiveStatusLabel.setText("File Server Active");
        } else {
            updateMediaButtonState(fileTransferButton, false, startText, stopText, startColor, stopColor, startIconPath, stopIconPath);
            fileReceiveStatusLabel.setForeground(Color.GRAY);
            fileReceiveStatusLabel.setText("File Server Off");
        }
        fileTransferButton.setEnabled(true);
    }

    private void toggleFileReceiverActionPerformed(ActionEvent evt) {
        if (!FileReceiver.isFileReceiverActive()) {
            FileReceiver.startFileReceiver();
            appendSystemMessage("--- File Receiver Started ---");
        } else {
            FileReceiver.stopFileReceiver();
            appendSystemMessage("--- File Receiver Stopped ---");
        }
        updateFileTransferButtonState(FileReceiver.isFileReceiverActive());
    }


    private void createVideoPanel(JPanel mainPanel) {
        JPanel receivedVideoPanel = new JPanel(new MigLayout("fill, insets 5", "[grow, fill]", "[grow, fill]"));
        receivedVideoPanel.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(150, 150, 150)),
                "Received Video Feed"
        ));
        receivedVideoPanel.setBackground(Color.BLACK);

        receivedVideoLabel = new JLabel();
        receivedVideoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        receivedVideoPanel.add(receivedVideoLabel, "grow, push");

        VideoServer.setVideoDisplayLabel(receivedVideoLabel);
        VideoServer.setReceivedVideoStatusLabel(receivedVideoStatusLabel);
        VoiceServer.setVoiceActivityLabel(voiceActivityLabel);

        mainPanel.add(receivedVideoPanel, "grow, push");
    }

    private void msg_sendActionPerformed(ActionEvent evt) {
        try {
            String msgout = msg_text.getText().trim();
            if (!msgout.isEmpty()) {
                if (dout != null) {
                    String fullMessage = "Server: " + msgout;
                    dout.writeUTF(fullMessage);
                    appendMyMessage(fullMessage);
                    msg_text.setText("");
                    msg_text.requestFocusInWindow();
                } else {
                    appendSystemMessage("Error: Client not connected or output stream not ready.");
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

            String displayName = (alignment == SwingConstants.RIGHT) ? "Server" : sender;

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

    private void setChatControlsEnabled(boolean enabled) {
        msg_text.setEnabled(enabled);
        msg_send.setEnabled(enabled);
        voiceCallButton.setEnabled(enabled);
        videoCallButton.setEnabled(enabled);
        fileTransferButton.setEnabled(enabled);
    }

    private void startServerThread() {
        new Thread(() -> {
            serverRunning.set(true);
            try {
                serversocket = new ServerSocket(CHAT_PORT);
                SwingUtilities.invokeLater(() -> {
                    serverStatusLabel.setText("Waiting for connection on port " + CHAT_PORT + "...");
                    serverStatusLabel.setForeground(new Color(255, 165, 0));
                });
                appendSystemMessage("Server started, waiting for client...");

                socket = serversocket.accept();

                din = new DataInputStream(socket.getInputStream());
                dout = new DataOutputStream(socket.getOutputStream());

                String initialMessage = din.readUTF();
                if (initialMessage.startsWith("USERNAME:")) {
                    connectedClientUsername = initialMessage.substring("USERNAME:".length());
                    SwingUtilities.invokeLater(() -> {
                        serverStatusLabel.setText("Client Connected: " + connectedClientUsername);
                        serverStatusLabel.setForeground(new Color(40, 167, 69));
                        setChatControlsEnabled(true);
                    });
                    appendSystemMessage("--- Client " + connectedClientUsername + " Connected ---");
                } else {
                    SwingUtilities.invokeLater(() -> {
                        serverStatusLabel.setText("Client Connected (No Username)");
                        serverStatusLabel.setForeground(new Color(40, 167, 69));
                        setChatControlsEnabled(true);
                    });
                    appendSystemMessage("--- Client Connected (No Username) ---");
                    appendOtherMessage(initialMessage);
                }

                String msgin = "";
                while (socket.isConnected() && !socket.isClosed() && serverRunning.get()) {
                    msgin = din.readUTF();
                    final String receivedMsg = msgin;
                    SwingUtilities.invokeLater(() -> appendOtherMessage(receivedMsg));
                }

            } catch (IOException e) {
                String errorMessage = e.getMessage();
                if (errorMessage != null && errorMessage.contains("Address already in use")) {
                    errorMessage = "Port " + CHAT_PORT + " is already in use. Please close other instances.";
                } else if (errorMessage != null && errorMessage.contains("Connection reset")) {
                    errorMessage = "Client disconnected unexpectedly.";
                } else if (errorMessage != null && errorMessage.contains("Socket closed")) {
                    errorMessage = "Server socket closed.";
                }
                final String displayError = errorMessage != null ? errorMessage : "An unknown network error occurred.";

                SwingUtilities.invokeLater(() -> {
                    serverStatusLabel.setText("Error: " + displayError);
                    serverStatusLabel.setForeground(new Color(220, 53, 69));
                    setChatControlsEnabled(false);
                });
                appendSystemMessage("ERROR: " + displayError);
            } finally {
                try {
                    if (din != null) {
                        din.close();
                    }
                    if (dout != null) {
                        dout.close();
                    }
                    if (socket != null) {
                        socket.close();
                    }
                    if (!serverRunning.get() && serversocket != null && !serversocket.isClosed()) {
                        serversocket.close();
                    }
                    SwingUtilities.invokeLater(() -> {
                        serverStatusLabel.setText("Server Stopped.");
                        serverStatusLabel.setForeground(Color.GRAY);
                        setChatControlsEnabled(false);
                        updateMediaButtonState(voiceCallButton, false, "📞 Start Voice", "📞 End Voice", new Color(77, 171, 247), new Color(220, 53, 69), "/icons/call_icon.png", "/icons/end_call_icon.png");
                        updateMediaButtonState(videoCallButton, false, "📹 Start Video", "📹 End Video", new Color(77, 171, 247), new Color(220, 53, 69), "/icons/video_call.png", "/icons/video_camera_front_off.png");
                        updateFileTransferButtonState(false);
                        voiceActivityLabel.setText("Voice: Off");
                        voiceActivityLabel.setForeground(Color.GRAY);
                        receivedVideoStatusLabel.setText("Video: Off");
                        receivedVideoStatusLabel.setForeground(Color.GRAY);
                        fileReceiveStatusLabel.setText("File Server Off");
                        fileReceiveStatusLabel.setForeground(Color.GRAY);
                    });
                    appendSystemMessage("--- Server Stopped ---");
                } catch (IOException e) {
                    appendSystemMessage("Error closing resources: " + e.getMessage());
                }
            }
        }, "ChatServerMainThread").start();
    }

    @Override
    protected void processWindowEvent(java.awt.event.WindowEvent e) {
        if (e.getID() == java.awt.event.WindowEvent.WINDOW_CLOSING) {
            serverRunning.set(false);
            if (VoiceServer.isVoiceCallActive()) {
                VoiceServer.stopVoiceServer();
            }
            if (VideoServer.isVideoCallActive()) {
                VideoServer.stopVideoServer();
            }
            if (FileReceiver.isFileReceiverActive()) {
                FileReceiver.stopFileReceiver();
            }
            if (serversocket != null && !serversocket.isClosed()) {
                try {
                    serversocket.close();
                } catch (IOException ex) {
                    System.err.println("Error closing main server socket during shutdown: " + ex.getMessage());
                }
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
            new ChatServer().setVisible(true);
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
                        int x = insets.left + 5; // Small offset for better alignment
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
