package com.mmmi.mediachat.server;

import com.formdev.flatlaf.FlatLightLaf;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import javax.swing.text.*;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChatServer extends JFrame {

    private static JTextPane msg_area;
    private JTextField msg_text;
    private JButton msg_send;
    private static JLabel serverStatusLabel;

    private JButton startVoiceCallButton;
    private JButton endVoiceCallButton;
    private JButton startVideoCallButton;
    private JButton endVideoCallButton;
    private JLabel receivedVideoLabel;
    private JLabel voiceActivityLabel;
    private JLabel receivedVideoStatusLabel;
    private JButton startFileReceiverButton;
    private JProgressBar fileReceiveProgressBar;
    private JLabel fileReceiveStatusLabel;

    static ServerSocket serversocket;
    static Socket socket;
    static DataInputStream din;
    static DataOutputStream dout;

    private static final int CHAT_PORT = 1201;
    private static AtomicBoolean serverRunning = new AtomicBoolean(false);

    private String connectedClientUsername = "Unknown Client";
    private static final Color MY_MESSAGE_COLOR = new Color(240, 240, 240);
    private static final Color OTHER_MESSAGE_COLOR = new Color(220, 248, 198);
    private static final Color SYSTEM_MESSAGE_COLOR = new Color(100, 100, 100);
    private static final Color TEXT_COLOR = Color.BLACK;
    private static final Font MESSAGE_FONT = new Font("Segoe UI", Font.PLAIN, 13);


    public ChatServer() {
        initComponents();
        setChatControlsEnabled(false);
        endVoiceCallButton.setEnabled(false);
        endVideoCallButton.setEnabled(false);
        voiceActivityLabel.setText("Voice: Off");
        voiceActivityLabel.setForeground(Color.GRAY);
        receivedVideoStatusLabel.setText("Video: Off");
        receivedVideoStatusLabel.setForeground(Color.GRAY);
        fileReceiveStatusLabel.setText("File Server Off");
        fileReceiveStatusLabel.setForeground(Color.GRAY);
        startFileReceiverButton.setEnabled(true);
        
        FileReceiver.setFileTransferUI(fileReceiveProgressBar, fileReceiveStatusLabel);
        startServerThread();
    }

    private void initComponents() {
        setTitle("MediaChat Server");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(850, 800);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new MigLayout("fill, insets 10", "[grow, fill]", "[][grow, fill][][][][][grow, fill]"));

        JPanel statusPanel = new JPanel(new MigLayout("fillx, insets 5", "[grow][right]", "[]"));
        statusPanel.setBackground(new Color(230, 240, 250));
        statusPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        serverStatusLabel = new JLabel("Initializing Server...");
        serverStatusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        serverStatusLabel.setForeground(new Color(50, 50, 50));
        statusPanel.add(serverStatusLabel, "growx");

        JPanel indicatorsPanel = new JPanel(new MigLayout("insets 0", "[][][]", "[]"));
        indicatorsPanel.setOpaque(false);
        voiceActivityLabel = new JLabel("Voice: Off");
        voiceActivityLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        voiceActivityLabel.setForeground(Color.GRAY);
        indicatorsPanel.add(voiceActivityLabel, "gapx 10");

        receivedVideoStatusLabel = new JLabel("Video: Off");
        receivedVideoStatusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        receivedVideoStatusLabel.setForeground(Color.GRAY);
        indicatorsPanel.add(receivedVideoStatusLabel, "gapx 10");

        statusPanel.add(indicatorsPanel, "wrap, right");
        mainPanel.add(statusPanel, "wrap, growx");

        msg_area = new JTextPane();
        msg_area.setEditable(false);
        msg_area.setFont(MESSAGE_FONT);
        msg_area.setBackground(new Color(230, 230, 230));
        msg_area.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        msg_area.setContentType("text/html");


        JScrollPane scrollPane = new JScrollPane(msg_area);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        mainPanel.add(scrollPane, "grow, wrap");

        JPanel inputPanel = new JPanel(new MigLayout("fillx, insets 0", "[grow, fill][]", "[]"));
        msg_text = new JTextField();
        msg_text.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        msg_text.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 180, 180)),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        msg_text.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                msg_sendActionPerformed(e);
            }
        });
        inputPanel.add(msg_text, "growx");

        msg_send = new JButton("Send");
        msg_send.setFont(new Font("Segoe UI", Font.BOLD, 13));
        msg_send.setBackground(new Color(0, 123, 255));
        msg_send.setForeground(Color.WHITE);
        msg_send.setFocusPainted(false);
        msg_send.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        msg_send.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                msg_sendActionPerformed(e);
            }
        });
        inputPanel.add(msg_send, "width 80!");
        mainPanel.add(inputPanel, "growx, wrap");

        JPanel mediaControlPanel = new JPanel(new MigLayout("fillx, insets 0", "[grow, fill][grow, fill][grow, fill][grow, fill]", "[]"));
        mediaControlPanel.setBorder(BorderFactory.createTitledBorder("Media Controls"));

        startVoiceCallButton = new JButton("Start Voice Call");
        startVoiceCallButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        startVoiceCallButton.setBackground(new Color(40, 167, 69));
        startVoiceCallButton.setForeground(Color.WHITE);
        startVoiceCallButton.setFocusPainted(false);
        startVoiceCallButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!VoiceServer.isVoiceCallActive()) {
                    VoiceServer.startVoiceServer();
                    appendSystemMessage("--- Voice Server Started ---");
                    startVoiceCallButton.setEnabled(false);
                    endVoiceCallButton.setEnabled(true);
                }
            }
        });
        mediaControlPanel.add(startVoiceCallButton, "growx");

        endVoiceCallButton = new JButton("End Voice Call");
        endVoiceCallButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        endVoiceCallButton.setBackground(new Color(220, 53, 69));
        endVoiceCallButton.setForeground(Color.WHITE);
        endVoiceCallButton.setFocusPainted(false);
        endVoiceCallButton.setEnabled(false);
        endVoiceCallButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (VoiceServer.isVoiceCallActive()) {
                    VoiceServer.stopVoiceServer();
                    appendSystemMessage("--- Voice Server Stopped ---");
                    startVoiceCallButton.setEnabled(true);
                    endVoiceCallButton.setEnabled(false);
                }
            }
        });
        mediaControlPanel.add(endVoiceCallButton, "growx");

        startVideoCallButton = new JButton("Start Video Call");
        startVideoCallButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        startVideoCallButton.setBackground(new Color(255, 140, 0));
        startVideoCallButton.setForeground(Color.WHITE);
        startVideoCallButton.setFocusPainted(false);
        startVideoCallButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (!VideoServer.isVideoCallActive()) {
                    VideoServer.startVideoServer();
                    appendSystemMessage("--- Video Server Started ---");
                    startVideoCallButton.setEnabled(false);
                    endVideoCallButton.setEnabled(true);
                }
            }
        });
        mediaControlPanel.add(startVideoCallButton, "growx");

        endVideoCallButton = new JButton("End Video Call");
        endVideoCallButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        endVideoCallButton.setBackground(new Color(220, 53, 69));
        endVideoCallButton.setForeground(Color.WHITE);
        endVideoCallButton.setFocusPainted(false);
        endVideoCallButton.setEnabled(false);
        endVideoCallButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (VideoServer.isVideoCallActive()) {
                    VideoServer.stopVideoServer();
                    appendSystemMessage("--- Video Server Stopped ---");
                    startVideoCallButton.setEnabled(true);
                    endVideoCallButton.setEnabled(false);
                }
            }
        });
        mediaControlPanel.add(endVideoCallButton, "growx, wrap");
        mainPanel.add(mediaControlPanel, "growx, wrap");

        JPanel fileTransferPanel = new JPanel(new MigLayout("fillx, insets 0", "[grow, fill][]", "[]"));
        fileTransferPanel.setBorder(BorderFactory.createTitledBorder("File Reception"));

        fileReceiveProgressBar = new JProgressBar(0, 100);
        fileReceiveProgressBar.setStringPainted(true);
        fileTransferPanel.add(fileReceiveProgressBar, "growx, height 25!");

        startFileReceiverButton = new JButton("Start File Receiver");
        startFileReceiverButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        startFileReceiverButton.setBackground(new Color(108, 117, 125));
        startFileReceiverButton.setForeground(Color.WHITE);
        startFileReceiverButton.setFocusPainted(false);
        startFileReceiverButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startFileReceiverActionPerformed(e);
            }
        });
        fileTransferPanel.add(startFileReceiverButton, "width 160!, wrap");

        fileReceiveStatusLabel = new JLabel("File Server Off");
        fileReceiveStatusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        fileReceiveStatusLabel.setForeground(Color.GRAY);
        fileTransferPanel.add(fileReceiveStatusLabel, "span 2, growx");

        mainPanel.add(fileTransferPanel, "growx, wrap");

        JPanel receivedVideoPanel = new JPanel(new MigLayout("fill, insets 5", "[grow, fill]", "[grow, fill]"));
        receivedVideoPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(150, 150, 150)), "Received Video Feed"));
        receivedVideoPanel.setBackground(Color.BLACK);

        receivedVideoLabel = new JLabel();
        receivedVideoLabel.setHorizontalAlignment(SwingConstants.CENTER);
        receivedVideoPanel.add(receivedVideoLabel, "grow, push");
        
        VideoServer.setVideoDisplayLabel(receivedVideoLabel);
        VideoServer.setReceivedVideoStatusLabel(receivedVideoStatusLabel);
        VoiceServer.setVoiceActivityLabel(voiceActivityLabel);

        mainPanel.add(receivedVideoPanel, "grow, push");

        add(mainPanel);
        msg_text.requestFocusInWindow();
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
            ex.printStackTrace();
            appendSystemMessage("Error sending message: " + ex.getMessage());
        } catch (Exception ex) {
            ex.printStackTrace();
            appendSystemMessage("An unexpected error occurred: " + ex.getMessage());
        }
    }

    // دوال مساعدة لإضافة الرسائل مع تنسيق
    private static void appendMessage(String message, Color bgColor, Color fgColor, int alignment) {
        SwingUtilities.invokeLater(() -> {
            HTMLEditorKit editorKit = (HTMLEditorKit) msg_area.getEditorKit();
            HTMLDocument doc = (HTMLDocument) msg_area.getDocument();

            String htmlContent = String.format(
                "<div style='background-color:%s; color:%s; padding: 8px; margin: 2px 0; border-radius: 8px; max-width: 70%%; float: %s; clear: both;'>%s</div>",
                toHexString(bgColor),
                toHexString(fgColor),
                (alignment == StyleConstants.ALIGN_RIGHT ? "right" : "left"),
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

    private static void appendMyMessage(String message) {
        appendMessage(message, MY_MESSAGE_COLOR, TEXT_COLOR, StyleConstants.ALIGN_RIGHT);
    }

    private static void appendOtherMessage(String message) {
        // في الخادم: الرسائل المستلمة من العميل هي "رسائل الطرف الآخر"
        appendMessage(message, OTHER_MESSAGE_COLOR, TEXT_COLOR, StyleConstants.ALIGN_LEFT);
    }

    private static void appendSystemMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            HTMLEditorKit editorKit = (HTMLEditorKit) msg_area.getEditorKit();
            HTMLDocument doc = (HTMLDocument) msg_area.getDocument();
            String htmlContent = String.format(
                "<div style='text-align: center; color:%s; margin: 5px 0;'><i>%s</i></div>",
                toHexString(SYSTEM_MESSAGE_COLOR),
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

    private void startFileReceiverActionPerformed(ActionEvent evt) {
        if (!FileReceiver.isFileReceiverActive()) {
            FileReceiver.startFileReceiver();
            startFileReceiverButton.setText("File Receiver Active");
            startFileReceiverButton.setEnabled(false);
            appendSystemMessage("--- File Receiver Started ---");
        } else {
            JOptionPane.showMessageDialog(this, "File receiver is already active.", "File Receiver", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void setChatControlsEnabled(boolean enabled) {
        msg_text.setEnabled(enabled);
        msg_send.setEnabled(enabled);
        startVoiceCallButton.setEnabled(enabled);
        startVideoCallButton.setEnabled(enabled);
    }

    private void startServerThread() {
        new Thread(() -> {
            serverRunning.set(true);
            try {
                serversocket = new ServerSocket(CHAT_PORT);
                SwingUtilities.invokeLater(() -> serverStatusLabel.setText("Waiting for connection on port " + CHAT_PORT + "..."));
                appendSystemMessage("Server started, waiting for client...");

                socket = serversocket.accept();
                
                din = new DataInputStream(socket.getInputStream());
                dout = new DataOutputStream(socket.getOutputStream());

                String initialMessage = din.readUTF();
                if (initialMessage.startsWith("USERNAME:")) {
                    connectedClientUsername = initialMessage.substring("USERNAME:".length());
                    SwingUtilities.invokeLater(() -> {
                        serverStatusLabel.setText("Client Connected: " + connectedClientUsername);
                        setChatControlsEnabled(true);
                    });
                    appendSystemMessage("--- Client " + connectedClientUsername + " Connected ---");
                } else {
                    SwingUtilities.invokeLater(() -> {
                        serverStatusLabel.setText("Client Connected (No Username)");
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
                }
                final String displayError = errorMessage != null ? errorMessage : "An unknown network error occurred.";
                
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    serverStatusLabel.setText("Error: " + displayError);
                    setChatControlsEnabled(false);
                });
                appendSystemMessage("ERROR: " + displayError);
            } finally {
                try {
                    if (din != null) din.close();
                    if (dout != null) dout.close();
                    if (socket != null) socket.close();
                    if (!serverRunning.get() && serversocket != null) {
                        serversocket.close();
                    }
                    SwingUtilities.invokeLater(() -> {
                        serverStatusLabel.setText("Server Stopped.");
                    });
                    appendSystemMessage("--- Server Stopped ---");
                } catch (IOException e) {
                    e.printStackTrace();
                    appendSystemMessage("Error closing resources: " + e.getMessage());
                }
            }
        }, "ChatServerMainThread").start();
    }

    public static void main(String args[]) {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (UnsupportedLookAndFeelException ex) {
            System.err.println("Failed to initialize FlatLaf: " + ex);
        }

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new ChatServer().setVisible(true);
            }
        });
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
            if (serversocket != null) {
                try {
                    serversocket.close();
                } catch (IOException ex) {
                    System.err.println("Error closing main server socket: " + ex.getMessage());
                }
            }
            dispose();
        }
        super.processWindowEvent(e);
    }
}