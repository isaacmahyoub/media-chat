package com.mmmi.mediachat.server;

import com.formdev.flatlaf.FlatLightLaf; // استيراد FlatLaf
import net.miginfocom.swing.MigLayout;   // استيراد MigLayout

import javax.swing.*;
import java.awt.*; // لاستخدام Font
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ChatServer extends JFrame {

    private static JTextArea msg_area;
    private JTextField msg_text;
    private JButton msg_send;
    private static JLabel serverStatusLabel; // لتحديث حالة الخادم

    static ServerSocket serversocket;
    static Socket socket;
    static DataInputStream din;
    static DataOutputStream dout;

    public ChatServer() {
        initComponents();
    }

    private void initComponents() {
        setTitle("MediaChat Server");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(480, 550); // حجم أكبر قليلاً
        setLocationRelativeTo(null); // توسيط النافذة

        // استخدام MigLayout للمحتوى الرئيسي
        JPanel mainPanel = new JPanel(new MigLayout("fill, insets 10", "[grow]", "[][grow, fill][]"));

        // شريط الحالة العلوي
        JPanel statusPanel = new JPanel(new MigLayout("fillx, insets 5", "[grow]", "[]"));
        statusPanel.setBackground(new Color(230, 240, 250)); // لون خلفية خفيف
        statusPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10)); // هامش داخلي
        serverStatusLabel = new JLabel("Initializing Server...");
        serverStatusLabel.setFont(new Font("Segoe UI", Font.BOLD, 14)); // خط سميك أكبر
        serverStatusLabel.setForeground(new Color(50, 50, 50)); // لون نص
        statusPanel.add(serverStatusLabel, "growx");
        mainPanel.add(statusPanel, "wrap, growx"); // wrap لينتقل إلى صف جديد

        // منطقة عرض الرسائل
        msg_area = new JTextArea();
        msg_area.setEditable(false);
        msg_area.setLineWrap(true);
        msg_area.setWrapStyleWord(true);
        msg_area.setFont(new Font("Segoe UI", Font.PLAIN, 13)); // خط مقروء
        msg_area.setBackground(new Color(250, 250, 250)); // خلفية بيضاء تقريباً
        msg_area.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // هامش داخلي
        JScrollPane scrollPane = new JScrollPane(msg_area);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200), 1)); // حدود خفيفة
        mainPanel.add(scrollPane, "grow, wrap"); // grow, wrap لملء المساحة والانتقال لصف جديد

        // لوحة إدخال الرسائل وزر الإرسال
        JPanel inputPanel = new JPanel(new MigLayout("fillx, insets 0", "[grow, fill][]", "[]"));
        msg_text = new JTextField();
        msg_text.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        msg_text.setBorder(BorderFactory.createCompoundBorder( // حدود مع هامش داخلي
                BorderFactory.createLineBorder(new Color(180, 180, 180)),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
        inputPanel.add(msg_text, "growx");

        msg_send = new JButton("Send");
        msg_send.setFont(new Font("Segoe UI", Font.BOLD, 13));
        msg_send.setBackground(new Color(0, 123, 255)); // لون أزرق جذاب
        msg_send.setForeground(Color.WHITE); // نص أبيض
        msg_send.setFocusPainted(false); // إزالة التركيز حول النص
        msg_send.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15)); // هامش زر
        // يمكنك إضافة أيقونة هنا: msg_send.setIcon(new ImageIcon("path/to/send_icon.png"));
        msg_send.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                msg_sendActionPerformed(e);
            }
        });
        inputPanel.add(msg_send, "width 80!"); // عرض ثابت للزر
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
                    msg_area.append("\nServer: " + msgout);
                    msg_text.setText("");
                    msg_text.requestFocusInWindow(); // إعادة التركيز بعد الإرسال
                } else {
                    msg_area.append("\nError: Client not connected or output stream not ready.");
                }
            }
        } catch (IOException ex) {
            ex.printStackTrace();
            msg_area.append("\nError sending message: " + ex.getMessage());
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
                new ChatServer().setVisible(true);
            }
        });

        new Thread(() -> {
            try {
                int port = 1201;
                serversocket = new ServerSocket(port);
                SwingUtilities.invokeLater(() -> serverStatusLabel.setText("Waiting for connection on port " + port + "..."));
                SwingUtilities.invokeLater(() -> msg_area.append("\nServer started, waiting for client..."));

                socket = serversocket.accept();

                din = new DataInputStream(socket.getInputStream());
                dout = new DataOutputStream(socket.getOutputStream());

                SwingUtilities.invokeLater(() -> {
                    serverStatusLabel.setText("Client Connected from " + socket.getInetAddress().getHostAddress());
                    msg_area.append("\n--- Client Connected ---");
                });

                String msgin = "";
                while (socket.isConnected() && !socket.isClosed()) {
                    msgin = din.readUTF();
                    final String receivedMsg = msgin;
                    SwingUtilities.invokeLater(() -> msg_area.append("\nClient: " + receivedMsg));
                }

            } catch (IOException e) {
                String errorMessage = e.getMessage();
                if (errorMessage != null && errorMessage.contains("Address already in use")) {
                    errorMessage = "Port " + 1201 + " is already in use. Please choose another port.";
                } else if (errorMessage != null && errorMessage.contains("Connection reset")) {
                     errorMessage = "Client disconnected unexpectedly.";
                }
                final String displayError = errorMessage != null ? errorMessage : "An unknown network error occurred.";

                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    serverStatusLabel.setText("Error: " + displayError);
                    msg_area.append("\nERROR: " + displayError);
                });
            } finally {
                try {
                    if (din != null) din.close();
                    if (dout != null) dout.close();
                    if (socket != null) socket.close();
                    if (serversocket != null) serversocket.close();
                    SwingUtilities.invokeLater(() -> {
                        serverStatusLabel.setText("Server Stopped.");
                        msg_area.append("\n--- Server Stopped ---");
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> msg_area.append("\nError closing resources: " + e.getMessage()));
                }
            }
        }).start();
    }
}