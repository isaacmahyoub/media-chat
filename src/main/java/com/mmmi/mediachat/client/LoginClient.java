package com.mmmi.mediachat.client;

import com.formdev.flatlaf.FlatLightLaf;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class LoginClient extends JFrame {

    private JTextField usernameField;
    private JButton connectButton;
    private JLabel statusLabel;

    public LoginClient() {
        initComponents();
    }

    private void initComponents() {
        setTitle("MediaChat - Login");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(450, 300); // 
        setResizable(false);
        setLocationRelativeTo(null); // وضع النافذة في المنتصف

        JPanel mainPanel = new JPanel(new MigLayout("insets 30, fill", "[grow, fill]", "[][][][][]"));
        mainPanel.setBackground(new Color(240, 240, 240)); 

        // أيقونة أو شعار (يمكنك استبدالها بصورة حقيقية إذا أردت)
        JLabel logoLabel = new JLabel("💬 MediaChat", SwingConstants.CENTER);
        logoLabel.setFont(new Font("Segoe UI", Font.BOLD, 28));
        logoLabel.setForeground(new Color(0, 123, 255));
        mainPanel.add(logoLabel, "wrap, gapbottom 20");

        JLabel promptLabel = new JLabel("Enter your username to connect:");
        promptLabel.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        promptLabel.setForeground(new Color(70, 70, 70));
        mainPanel.add(promptLabel, "wrap, gapbottom 10");

        usernameField = new JTextField();
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 16));
        //
        usernameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(180, 180, 180)), // border عادي
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        usernameField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                connectActionPerformed();
            }
        });
        mainPanel.add(usernameField, "growx, wrap, h 45!"); // زيادة ارتفاع الحقل

        connectButton = new JButton("Connect to Chat");
        connectButton.setFont(new Font("Segoe UI", Font.BOLD, 17));
        connectButton.setBackground(new Color(0, 123, 255));
        connectButton.setForeground(Color.WHITE);
        connectButton.setFocusPainted(false);
        connectButton.setBorder(BorderFactory.createEmptyBorder(12, 25, 12, 25)); // هامش أكبر
        //
        connectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                connectActionPerformed();
            }
        });
        mainPanel.add(connectButton, "growx, wrap, gaptop 25, h 50!"); // زر أكبر وارتفاع أعلى

        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Segoe UI", Font.ITALIC, 12));
        statusLabel.setForeground(Color.GRAY);
        statusLabel.setHorizontalAlignment(SwingConstants.CENTER);
        mainPanel.add(statusLabel, "growx, wrap, gaptop 10");

        add(mainPanel);
        usernameField.requestFocusInWindow();
    }

    private void connectActionPerformed() {
        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            statusLabel.setText("Please enter a username to continue.");
            statusLabel.setForeground(new Color(220, 53, 69)); // لون أحمر للتنبيه
            usernameField.requestFocusInWindow();
            return;
        }

        // إخفاء نافذة تسجيل الدخول
        this.dispose();

        // بدء ChatClient مع اسم المستخدم المحدد
        SwingUtilities.invokeLater(() -> {
            new ChatClient(username).setVisible(true);
        });
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(new FlatLightLaf());
        } catch (UnsupportedLookAndFeelException ex) {
            System.err.println("Failed to initialize FlatLaf: " + ex);
        }

        SwingUtilities.invokeLater(() -> {
            new LoginClient().setVisible(true);
        });
    }
}