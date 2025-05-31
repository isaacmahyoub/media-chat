package com.mmmi.mediachat.client;

import javax.swing.*;
import java.io.*;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class FileSender {

    private static final String SERVER_ADDRESS = "localhost";
    private static final int FILE_PORT = 1204; // 
    private static JProgressBar fileSendProgressBar;
    private static JLabel fileSendStatusLabel;
    private static AtomicBoolean isSending = new AtomicBoolean(false);

    // لربط شريط التقدم ومؤشر الحالة من واجهة ChatClient
    public static void setFileTransferUI(JProgressBar progressBar, JLabel statusLabel) {
        fileSendProgressBar = progressBar;
        fileSendStatusLabel = statusLabel;
        SwingUtilities.invokeLater(() -> {
            if (fileSendProgressBar != null) fileSendProgressBar.setValue(0);
            if (fileSendStatusLabel != null) fileSendStatusLabel.setText("Ready to send files.");
        });
    }

    public static void sendFile(File file) {
        if (isSending.get()) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "Already sending a file. Please wait.", "File Transfer", JOptionPane.WARNING_MESSAGE));
            return;
        }

        if (file == null || !file.exists()) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(null, "Invalid file selected.", "File Transfer Error", JOptionPane.ERROR_MESSAGE));
            return;
        }

        isSending.set(true);
        SwingUtilities.invokeLater(() -> {
            if (fileSendStatusLabel != null) fileSendStatusLabel.setText("Sending: " + file.getName());
            if (fileSendProgressBar != null) {
                fileSendProgressBar.setValue(0);
                fileSendProgressBar.setStringPainted(true);
            }
        });

        new Thread(() -> {
            Socket socket = null;
            BufferedInputStream bis = null;
            DataOutputStream dos = null; // نستخدم DataOutputStream لإرسال بيانات الملف

            try {
                socket = new Socket(SERVER_ADDRESS, FILE_PORT);
                dos = new DataOutputStream(socket.getOutputStream());

                // إرسال معلومات الملف أولاً: اسم الملف وحجمه
                dos.writeUTF(file.getName());
                dos.writeLong(file.length());
                dos.flush();

                bis = new BufferedInputStream(new FileInputStream(file));
                byte[] buffer = new byte[4096]; // حجم المخزن المؤقت (4KB)
                long totalBytesRead = 0;
                int bytesRead;

                while ((bytesRead = bis.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;
                    int progress = (int) ((totalBytesRead * 100) / file.length());
                    final int currentProgress = progress;
                    final long currentBytes = totalBytesRead;

                    SwingUtilities.invokeLater(() -> {
                        if (fileSendProgressBar != null) fileSendProgressBar.setValue(currentProgress);
                        if (fileSendStatusLabel != null) {
                            fileSendStatusLabel.setText(String.format("Sending %s: %d%% (%s / %s)",
                                    file.getName(), currentProgress, formatFileSize(currentBytes), formatFileSize(file.length())));
                        }
                    });
                }
                dos.flush(); // للتأكد من إرسال جميع البايتات العالقة

                SwingUtilities.invokeLater(() -> {
                    if (fileSendStatusLabel != null) fileSendStatusLabel.setText("File '" + file.getName() + "' sent successfully!");
                    if (fileSendProgressBar != null) fileSendProgressBar.setValue(100);
                });
                System.out.println("File '" + file.getName() + "' sent successfully.");

            } catch (IOException e) {
                String errorMessage = "Failed to send file '" + file.getName() + "': " + e.getMessage();
                System.err.println(errorMessage);
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    if (fileSendStatusLabel != null) fileSendStatusLabel.setText("Error sending file: " + e.getMessage());
                    if (fileSendStatusLabel != null) fileSendStatusLabel.setForeground(java.awt.Color.RED);
                    JOptionPane.showMessageDialog(null, errorMessage, "File Transfer Error", JOptionPane.ERROR_MESSAGE);
                });
            } finally {
                try {
                    if (bis != null) bis.close();
                    if (dos != null) dos.close();
                    if (socket != null) socket.close();
                } catch (IOException e) {
                    System.err.println("Error closing file sender resources: " + e.getMessage());
                }
                isSending.set(false); // تم الانتهاء من عملية الإرسال
                SwingUtilities.invokeLater(() -> {
                     if (fileSendStatusLabel != null && !fileSendStatusLabel.getText().startsWith("Error")) {
                        fileSendStatusLabel.setText("Ready to send files.");
                        fileSendStatusLabel.setForeground(java.awt.Color.BLACK);
                     }
                });
            }
        }, "FileSenderThread").start();
    }

    private static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " Bytes";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = ("KMGTPE").charAt(exp - 1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }

    public static boolean isSendingFile() {
        return isSending.get();
    }
}