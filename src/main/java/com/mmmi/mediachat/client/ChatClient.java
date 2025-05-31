package com.mmmi.mediachat.client;

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
import java.net.ConnectException;
import java.net.Socket;
import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChatClient extends JFrame {

    private static JTextPane msg_area;
    private JTextField msg_text;
    private JButton msg_send;
    private static JLabel clientStatusLabel;

    private JButton startVoiceCallButton;
    private JButton endVoiceCallButton;
    private JButton startVideoCallButton;
    private JButton endVideoCallButton;
    private JButton sendFileButton;
    private JLabel webcamDisplayLabel;
    private JLabel micActivityLabel;
    private JLabel cameraStatusLabel;
    private JProgressBar fileSendProgressBar;
    private JLabel fileSendStatusLabel;

    static Socket socket;
    static DataInputStream din;
    static DataOutputStream dout;

    private static final String SERVER_ADDRESS = "localhost";
    private static final int CHAT_PORT = 1201;
    private static AtomicBoolean clientRunning = new AtomicBoolean(false);

    private String username;

    private static final Color MY_MESSAGE_COLOR = new Color(220, 248, 198);
    private static final Color OTHER_MESSAGE_COLOR = new Color(240, 240, 240);
    private static final Color SYSTEM_MESSAGE_COLOR = new Color(100, 100, 100);
    private static final Color TEXT_COLOR = Color.BLACK;
    private static final Font MESSAGE_FONT = new Font("Segoe UI", Font.PLAIN, 13);


    public ChatClient(String username) {
        this.username = username;
        initComponents();
        setTitle("MediaChat Client - " + username);
        setChatControlsEnabled(false);
        endVoiceCallButton.setEnabled(false);
        endVideoCallButton.setEnabled(false);
        micActivityLabel.setText("Mic: Off");
        micActivityLabel.setForeground(Color.GRAY);
        cameraStatusLabel.setText("Cam: Off");
        cameraStatusLabel.setForeground(Color.GRAY);
        fileSendStatusLabel.setText("Ready to send files.");
        fileSendStatusLabel.setForeground(Color.BLACK);

        FileSender.setFileTransferUI(fileSendProgressBar, fileSendStatusLabel);

        startClientConnectionThread();
    }

    private void initComponents() {
        setTitle("MediaChat Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(850, 800);
        setLocationRelativeTo(null);

        JPanel mainPanel = new JPanel(new MigLayout("fill, insets 10", "[grow, fill]", "[][grow, fill][][][][][grow, fill]"));

        JPanel statusPanel = new JPanel(new MigLayout("fillx, insets 5", "[grow][right]", "[]"));
        statusPanel.setBackground(new Color(230, 240, 250));
        statusPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        clientStatusLabel = new JLabel("Attempting to connect to server...");
        clientStatusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        clientStatusLabel.setForeground(new Color(50, 50, 50));
        statusPanel.add(clientStatusLabel, "growx");

        JPanel indicatorsPanel = new JPanel(new MigLayout("insets 0", "[][][]", "[]"));
        indicatorsPanel.setOpaque(false);
        micActivityLabel = new JLabel("Mic: Off");
        micActivityLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        micActivityLabel.setForeground(Color.GRAY);
        indicatorsPanel.add(micActivityLabel, "gapx 10");

        cameraStatusLabel = new JLabel("Cam: Off");
        cameraStatusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        cameraStatusLabel.setForeground(Color.GRAY);
        indicatorsPanel.add(cameraStatusLabel, "gapx 10");

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
                if (!VoiceClient.isVoiceCallActive()) {
                    VoiceClient.startVoiceClient();
                    appendSystemMessage("--- Voice Client Started ---");
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
                if (VoiceClient.isVoiceCallActive()) {
                    VoiceClient.stopVoiceClient();
                    appendSystemMessage("--- Voice Client Stopped ---");
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
                if (!VideoClient.isVideoCallActive()) {
                    VideoClient.setLocalVideoDisplayLabel(webcamDisplayLabel);
                    VideoClient.startVideoClient();
                    appendSystemMessage("--- Video Client Started ---");
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
                if (VideoClient.isVideoCallActive()) {
                    VideoClient.stopVideoClient();
                    appendSystemMessage("--- Video Client Stopped ---");
                    startVideoCallButton.setEnabled(true);
                    endVideoCallButton.setEnabled(false);
                }
            }
        });
        mediaControlPanel.add(endVideoCallButton, "growx, wrap");
        mainPanel.add(mediaControlPanel, "growx, wrap");

        JPanel fileTransferPanel = new JPanel(new MigLayout("fillx, insets 0", "[grow, fill][]", "[]"));
        fileTransferPanel.setBorder(BorderFactory.createTitledBorder("File Transfer"));

        fileSendProgressBar = new JProgressBar(0, 100);
        fileSendProgressBar.setStringPainted(true);
        fileTransferPanel.add(fileSendProgressBar, "growx, height 25!");

        sendFileButton = new JButton("Send File");
        sendFileButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        sendFileButton.setBackground(new Color(108, 117, 125));
        sendFileButton.setForeground(Color.WHITE);
        sendFileButton.setFocusPainted(false);
        sendFileButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendFileActionPerformed(e);
            }
        });
        fileTransferPanel.add(sendFileButton, "width 100!, wrap");

        fileSendStatusLabel = new JLabel("Ready to send files.");
        fileSendStatusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        fileSendStatusLabel.setForeground(Color.BLACK);
        fileTransferPanel.add(fileSendStatusLabel, "span 2, growx");

        mainPanel.add(fileTransferPanel, "growx, wrap");

        JPanel localVideoPanel = new JPanel(new MigLayout("fill, insets 5", "[grow, fill]", "[grow, fill]"));
        localVideoPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createLineBorder(new Color(150, 150, 150)), "Local Webcam Feed"));
        localVideoPanel.setBackground(Color.DARK_GRAY);

        webcamDisplayLabel = new JLabel();
        webcamDisplayLabel.setHorizontalAlignment(SwingConstants.CENTER);
        localVideoPanel.add(webcamDisplayLabel, "grow, push");

        VideoClient.setLocalVideoDisplayLabel(webcamDisplayLabel);
        VideoClient.setCameraStatusLabel(cameraStatusLabel);
        VoiceClient.setMicActivityLabel(micActivityLabel);

        mainPanel.add(localVideoPanel, "grow, push");

        add(mainPanel);
        msg_text.requestFocusInWindow();
    }

    private void msg_sendActionPerformed(ActionEvent evt) {
        try {
            String msgout = msg_text.getText().trim();
            if (!msgout.isEmpty()) {
                if (dout != null) {
                    // إرسال الرسالة مع اسم المستخدم
                    String fullMessage = username + ": " + msgout;
                    dout.writeUTF(fullMessage);
                    //
                    appendMyMessage(fullMessage);
                    msg_text.setText("");
                    msg_text.requestFocusInWindow();
                } else {
                    appendSystemMessage("Error: Not connected to server or output stream not ready.");
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

            // 
            String htmlContent = String.format(
                "<div style='background-color:%s; color:%s; padding: 8px; margin: 2px 0; border-radius: 8px; max-width: 70%%; float: %s; clear: both;'>%s</div>",
                toHexString(bgColor), // تحويل Color إلى String Hex
                toHexString(fgColor),
                (alignment == StyleConstants.ALIGN_RIGHT ? "right" : "left"),
                escapeHtml(message)
            );
            
            try {
                // إدراج HTML في نهاية المستند
                editorKit.insertHTML(doc, doc.getLength(), htmlContent, 0, 0, null);
                // التمرير التلقائي لأسفل
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
        // 
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

    private void setChatControlsEnabled(boolean enabled) {
        msg_text.setEnabled(enabled);
        msg_send.setEnabled(enabled);
        startVoiceCallButton.setEnabled(enabled);
        startVideoCallButton.setEnabled(enabled);
        sendFileButton.setEnabled(enabled);
    }

    private void startClientConnectionThread() {
        new Thread(() -> {
            clientRunning.set(true);
            try {
                String serverAddress = "localhost";
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
                    // 
                    SwingUtilities.invokeLater(() -> appendOtherMessage(receivedMsg));
                }

            } catch (ConnectException e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    clientStatusLabel.setText("Error: Server not running or unreachable.");
                    setChatControlsEnabled(false);
                });
                appendSystemMessage("ERROR: Could not connect to server. Please ensure server is running.");
            } catch (IOException e) {
                String errorMessage = e.getMessage();
                if (errorMessage != null && errorMessage.contains("Connection reset by peer")) {
                    errorMessage = "Server disconnected unexpectedly.";
                }
                final String displayError = errorMessage != null ? errorMessage : "An unknown network error occurred.";

                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    clientStatusLabel.setText("Disconnected: " + displayError);
                    setChatControlsEnabled(false);
                });
                appendSystemMessage("DISCONNECTED: " + displayError);
            } finally {
                try {
                    if (din != null) din.close();
                    if (dout != null) dout.close();
                    if (socket != null) socket.close();
                    SwingUtilities.invokeLater(() -> {
                        clientStatusLabel.setText("Client Disconnected.");
                    });
                    appendSystemMessage("--- Client Disconnected ---");
                } catch (IOException e) {
                    e.printStackTrace();
                    appendSystemMessage("Error closing resources: " + e.getMessage());
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
}