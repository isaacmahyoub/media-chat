package com.mmmi.mediachat.client;

import com.formdev.flatlaf.FlatLightLaf; // استيراد FlatLaf
import net.miginfocom.swing.MigLayout;   // استيراد MigLayout

import javax.swing.*;
import java.awt.*; // لاستخدام Font
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;

public class ChatClient extends JFrame {

    private static JTextArea msg_area;
    private JTextField msg_text;
    private JButton msg_send;
    private static JLabel clientStatusLabel; // لتحديث حالة العميل

    static Socket socket;
    static DataInputStream din;
    static DataOutputStream dout;

    public ChatClient() {
        initComponents();
    }

    private void initComponents() {
        setTitle("MediaChat Client");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(480, 550); // حجم أكبر قليلاً
        setLocationRelativeTo(null); // توسيط النافذة

        // استخدام MigLayout للمحتوى الرئيسي
        JPanel mainPanel = new JPanel(new MigLayout("fill, insets 10", "[grow]", "[][grow, fill][]"));

        // شريط الحالة العلوي
        JPanel statusPanel = new JPanel(new MigLayout("fillx, insets 5", "[grow]", "[]"));
        statusPanel.setBackground(new Color(230, 240, 250)); // لون خلفية خفيف
        statusPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10)); // هامش داخلي
        clientStatusLabel = new JLabel("Attempting to connect to server...");
        clientStatusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14)); // خط سميك أكبر
        clientStatusLabel.setForeground(new Color(50, 50, 50)); // لون نص
        statusPanel.add(clientStatusLabel, "growx");
        mainPanel.add(statusPanel, "wrap, growx");

        // منطقة عرض الرسائل
        msg_area = new JTextArea();
        msg_area.setEditable(false);
        msg_area.setLineWrap(true);
        msg_area.setWrapStyleWord(true);
        msg_area.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        msg_area.setBackground(new Color(250, 250, 250));
        msg_area.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        JScrollPane scrollPane = new JScrollPane(msg_area);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1));
        mainPanel.add(scrollPane, "grow, wrap");

        // لوحة إدخال الرسائل وزر الإرسال
        JPanel inputPanel = new JPanel(new MigLayout("fillx, insets 0", "[grow, fill][]", "[]"));
        msg_text = new JTextField();
        msg_text.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        msg_text.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 180, 180)),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        inputPanel.add(msg_text, "growx");

        msg_send = new JButton("Send");
        msg_send.setFont(new Font("Segoe UI", Font.BOLD, 13));
        msg_send.setBackground(new Color(0, 123, 255));
        msg_send.setForeground(Color.WHITE);
        msg_send.setFocusPainted(false);
        msg_send.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));
        // يمكنك إضافة أيقونة هنا: msg_send.setIcon(new ImageIcon("path/to/send_icon.png"));
        msg_send.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                msg_sendActionPerformed(e);
            }
        });
        inputPanel.add(msg_send, "width 80!");
        mainPanel.add(inputPanel, "growx");

        add(mainPanel);
        msg_text.requestFocusInWindow();
    }

    private void msg_sendActionPerformed(ActionEvent evt) {
        try {
            String msgout = msg_text.getText().trim();
            if (!msgout.isEmpty()) {
                if (dout != null) {
                    dout.writeUTF(msgout);
                    msg_area.append("\nYou: " + msgout);
                    msg_text.setText("");
                    msg_text.requestFocusInWindow();
                } else {
                    msg_area.append("\nError: Not connected to server or output stream not ready.");
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            msg_area.append("\nError sending message: " + ex.getMessage());
            SwingUtilities.invokeLater(() -> clientStatusLabel.setText("Disconnected: " + ex.getMessage()));
        } catch (Exception ex) {
            ex.printStackTrace();
            msg_area.append("\nAn unexpected error occurred: " + ex.getMessage());
        }
    }

    public static void main(String args[]) {
        // تعيين FlatLaf كمظهر وتخطيط (Look and Feel)
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (UnsupportedLookAndFeelException ex) {
            System.err.println("Failed to initialize FlatLaf: " + ex);
        }

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                new ChatClient().setVisible(true);
            }
        });

        new Thread(() -> {
            try {
                String serverAddress = "localhost";
                int port = 1201;

                SwingUtilities.invokeLater(() -> clientStatusLabel.setText("Connecting to " + serverAddress + ":" + port + "..."));
                SwingUtilities.invokeLater(() -> msg_area.append("\nAttempting to connect to server..."));

                socket = new Socket(serverAddress, port);

                din = new DataInputStream(socket.getInputStream());
                dout = new DataOutputStream(socket.getOutputStream());

                SwingUtilities.invokeLater(() -> {
                    clientStatusLabel.setText("Connected to Server!");
                    msg_area.append("\n--- Connected to Server ---");
                });

                String msgin = "";
                while (socket.isConnected() && !socket.isClosed()) {
                    msgin = din.readUTF();
                    final String receivedMsg = msgin;
                    SwingUtilities.invokeLater(() -> msg_area.append("\nServer: " + receivedMsg));
                }

            } catch (ConnectException e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    clientStatusLabel.setText("Error: Server not running or unreachable.");
                    msg_area.append("\nERROR: Could not connect to server. Please ensure server is running.");
                });
            } catch (IOException e) {
                String errorMessage = e.getMessage();
                if (errorMessage != null && errorMessage.contains("Connection reset by peer")) {
                    errorMessage = "Server disconnected unexpectedly.";
                }
                final String displayError = errorMessage != null ? errorMessage : "An unknown network error occurred.";

                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    clientStatusLabel.setText("Disconnected: " + displayError);
                    msg_area.append("\nDISCONNECTED: " + displayError);
                });
            } finally {
                try {
                    if (din != null) din.close();
                    if (dout != null) dout.close();
                    if (socket != null) socket.close();
                    SwingUtilities.invokeLater(() -> {
                        clientStatusLabel.setText("Client Disconnected.");
                        msg_area.append("\n--- Client Disconnected ---");
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> msg_area.append("\nError closing resources: " + e.getMessage()));
                }
            }
        }).start();
    }
}