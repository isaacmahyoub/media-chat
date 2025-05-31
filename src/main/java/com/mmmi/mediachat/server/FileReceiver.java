package com.mmmi.mediachat.server;

import javax.swing.*;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.text.SimpleDateFormat;
import java.util.Date;

public class FileReceiver {

    private static final int FILE_PORT = 1204; 
    private static ServerSocket fileServerSocket;
    private static JProgressBar fileReceiveProgressBar;
    private static JLabel fileReceiveStatusLabel;
    private static AtomicBoolean isReceiving = new AtomicBoolean(false);
    private static AtomicBoolean serverRunning = new AtomicBoolean(false);

    // المجلد الذي سيتم حفظ الملفات فيه
    private static final String DOWNLOAD_DIR = "ReceivedFiles"; 

    // لربط شريط التقدم ومؤشر الحالة من واجهة ChatServer
    public static void setFileTransferUI(JProgressBar progressBar, JLabel statusLabel) {
        fileReceiveProgressBar = progressBar;
        fileReceiveStatusLabel = statusLabel;
        SwingUtilities.invokeLater(() -> {
            if (fileReceiveProgressBar != null) fileReceiveProgressBar.setValue(0);
            if (fileReceiveStatusLabel != null) fileReceiveStatusLabel.setText("Waiting to receive files...");
        });
    }

    public static void startFileReceiver() {
        if (isReceiving.get()) {
            System.out.println("File receiver is already active.");
            return;
        }
        serverRunning.set(true);
        System.out.println("Attempting to start File Receiver...");

        // التأكد من وجود مجلد التنزيلات
        File downloadFolder = new File(DOWNLOAD_DIR);
        if (!downloadFolder.exists()) {
            downloadFolder.mkdirs(); // إنشاء المجلدات إذا لم تكن موجودة
        }

        new Thread(() -> {
            try {
                fileServerSocket = new ServerSocket(FILE_PORT);
                System.out.println("File Server listening on port " + FILE_PORT);
                SwingUtilities.invokeLater(() -> {
                    if (fileReceiveStatusLabel != null) fileReceiveStatusLabel.setText("Waiting for file connection...");
                });

                while (serverRunning.get()) { // حلقة لانتظار اتصالات متعددة
                    Socket clientSocket = null;
                    DataInputStream dis = null;
                    BufferedOutputStream bos = null;

                    try {
                        clientSocket = fileServerSocket.accept(); // انتظار اتصال جديد
                        isReceiving.set(true); // بدء الاستقبال
                        System.out.println("File Client connected from " + clientSocket.getInetAddress().getHostAddress());

                        dis = new DataInputStream(clientSocket.getInputStream());

                        // استقبال معلومات الملف أولاً
                        String fileName = dis.readUTF();
                        long fileSize = dis.readLong();

                        // لتجنب overwriting الملفات بنفس الاسم
                        String uniqueFileName = generateUniqueFileName(fileName);
                        File outputFile = new File(DOWNLOAD_DIR, uniqueFileName);

                        SwingUtilities.invokeLater(() -> {
                            if (fileReceiveStatusLabel != null) fileReceiveStatusLabel.setText("Receiving: " + uniqueFileName + " (" + formatFileSize(fileSize) + ")");
                            if (fileReceiveProgressBar != null) {
                                fileReceiveProgressBar.setValue(0);
                                fileReceiveProgressBar.setStringPainted(true);
                            }
                        });

                        bos = new BufferedOutputStream(new FileOutputStream(outputFile));
                        byte[] buffer = new byte[4096];
                        long totalBytesRead = 0;
                        int bytesRead;

                        while (totalBytesRead < fileSize && (bytesRead = dis.read(buffer, 0, (int) Math.min(buffer.length, fileSize - totalBytesRead))) != -1) {
                            bos.write(buffer, 0, bytesRead);
                            totalBytesRead += bytesRead;
                            int progress = (int) ((totalBytesRead * 100) / fileSize);
                            final int currentProgress = progress;
                            final long currentBytes = totalBytesRead;

                            SwingUtilities.invokeLater(() -> {
                                if (fileReceiveProgressBar != null) fileReceiveProgressBar.setValue(currentProgress);
                                if (fileReceiveStatusLabel != null) {
                                    fileReceiveStatusLabel.setText(String.format("Receiving %s: %d%% (%s / %s)",
                                            uniqueFileName, currentProgress, formatFileSize(currentBytes), formatFileSize(fileSize)));
                                }
                            });
                        }
                        bos.flush();

                        if (totalBytesRead >= fileSize) { // تأكد من استلام الملف بالكامل
                            SwingUtilities.invokeLater(() -> {
                                if (fileReceiveStatusLabel != null) fileReceiveStatusLabel.setText("File '" + uniqueFileName + "' received successfully!");
                                if (fileReceiveProgressBar != null) fileReceiveProgressBar.setValue(100);
                                JOptionPane.showMessageDialog(null, "File '" + uniqueFileName + "' received successfully!", "File Transfer", JOptionPane.INFORMATION_MESSAGE);
                            });
                            System.out.println("File '" + uniqueFileName + "' received successfully. Saved to: " + outputFile.getAbsolutePath());
                        } else {
                            SwingUtilities.invokeLater(() -> {
                                if (fileReceiveStatusLabel != null) fileReceiveStatusLabel.setText("File '" + uniqueFileName + "' transfer incomplete!");
                                if (fileReceiveStatusLabel != null) fileReceiveStatusLabel.setForeground(java.awt.Color.ORANGE);
                                JOptionPane.showMessageDialog(null, "File '" + uniqueFileName + "' transfer incomplete!", "File Transfer Warning", JOptionPane.WARNING_MESSAGE);
                            });
                            System.err.println("File '" + uniqueFileName + "' transfer incomplete. Only " + totalBytesRead + " of " + fileSize + " bytes received.");
                        }

                    } catch (IOException e) {
                        if (serverRunning.get()) { // إذا كان الخادم لا يزال نشطًا
                            String errorMessage = "Error during file transfer: " + e.getMessage();
                            System.err.println(errorMessage);
                            e.printStackTrace();
                            SwingUtilities.invokeLater(() -> {
                                if (fileReceiveStatusLabel != null) fileReceiveStatusLabel.setText("Error receiving file: " + e.getMessage());
                                if (fileReceiveStatusLabel != null) fileReceiveStatusLabel.setForeground(java.awt.Color.RED);
                            });
                        }
                    } finally {
                        try {
                            if (bos != null) bos.close();
                            if (dis != null) dis.close();
                            if (clientSocket != null) clientSocket.close();
                        } catch (IOException e) {
                            System.err.println("Error closing file receiver resources: " + e.getMessage());
                        }
                        isReceiving.set(false); // تم الانتهاء من عملية الاستقبال
                        SwingUtilities.invokeLater(() -> {
                             if (fileReceiveStatusLabel != null && !fileReceiveStatusLabel.getText().startsWith("Error")) {
                                fileReceiveStatusLabel.setText("Waiting for file connection...");
                                fileReceiveStatusLabel.setForeground(java.awt.Color.BLACK);
                             }
                        });
                    }
                }
            } catch (IOException e) {
                if (serverRunning.get()) {
                    String errorMessage = "File Server IO Error: " + e.getMessage();
                    System.err.println(errorMessage);
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        if (fileReceiveStatusLabel != null) fileReceiveStatusLabel.setText("File Server Error: " + e.getMessage());
                        if (fileReceiveStatusLabel != null) fileReceiveStatusLabel.setForeground(java.awt.Color.RED);
                    });
                } else {
                    System.out.println("File Receiver stopped gracefully.");
                }
            } finally {
                stopFileReceiver(); // تأكد من إيقاف الخدمة عند انتهاء الثريد الرئيسي
            }
        }, "FileReceiverMainThread").start();
    }

    public static void stopFileReceiver() {
        if (!isReceiving.get() && fileServerSocket == null) { // إذا لم يكن هناك استقبال ولا سيرفر سوكيت
            System.out.println("File receiver is already inactive.");
            return;
        }
        serverRunning.set(false); // إشارة للثريد بالتوقف

        if (fileServerSocket != null) {
            try {
                fileServerSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing fileServerSocket: " + e.getMessage());
            } finally {
                fileServerSocket = null;
            }
        }
        isReceiving.set(false); // تأكد من إعادة تعيين الحالة
        SwingUtilities.invokeLater(() -> {
            if (fileReceiveStatusLabel != null) {
                fileReceiveStatusLabel.setText("File Server Off");
                fileReceiveStatusLabel.setForeground(java.awt.Color.GRAY);
            }
        });
        System.out.println("File Receiver Stopped.");
    }

    private static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " Bytes";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = ("KMGTPE").charAt(exp - 1) + "";
        return String.format("%.2f %sB", bytes / Math.pow(1024, exp), pre);
    }

    // لإنشاء اسم ملف فريد لتجنب التضارب
    private static String generateUniqueFileName(String originalFileName) {
        String name = originalFileName;
        String extension = "";
        int dotIndex = originalFileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < originalFileName.length() - 1) {
            name = originalFileName.substring(0, dotIndex);
            extension = originalFileName.substring(dotIndex);
        }
        SimpleDateFormat sdf = new SimpleDateFormat("_yyyyMMdd_HHmmss");
        return name + sdf.format(new Date()) + extension;
    }

    public static boolean isFileReceiverActive() {
        return serverRunning.get() && fileServerSocket != null && !fileServerSocket.isClosed();
    }
}