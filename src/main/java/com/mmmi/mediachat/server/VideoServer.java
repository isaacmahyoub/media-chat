package com.mmmi.mediachat.server;

import javax.swing.*;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class VideoServer {

    private static final int VIDEO_PORT = 1203;
    private static ServerSocket videoServerSocket;
    private static Socket videoSocket;
    private static JLabel videoDisplayLabel;
    private static volatile boolean isVideoCallActive = false;
    private static AtomicBoolean serverRunning = new AtomicBoolean(false);
    private static JLabel receivedVideoStatusLabel;

    public static void setVideoDisplayLabel(JLabel label) {
        videoDisplayLabel = label;
    }
    public static void setReceivedVideoStatusLabel(JLabel label) {
        receivedVideoStatusLabel = label;
    }

    public static void startVideoServer() {
        if (isVideoCallActive) {
            System.out.println("Video server is already active.");
            return;
        }
        isVideoCallActive = true;
        serverRunning.set(true);
        System.out.println("Attempting to start Video Server...");

        new Thread(() -> {
            try {
                videoServerSocket = new ServerSocket(VIDEO_PORT);
                System.out.println("Video Server listening on port " + VIDEO_PORT);

                while (isVideoCallActive && serverRunning.get()) {
                    videoSocket = videoServerSocket.accept();
                    System.out.println("Video Client connected from " + videoSocket.getInetAddress().getHostAddress());

                    SwingUtilities.invokeLater(() -> {
                        if (receivedVideoStatusLabel != null) receivedVideoStatusLabel.setText("Video: Active");
                        if (receivedVideoStatusLabel != null) receivedVideoStatusLabel.setForeground(java.awt.Color.BLUE.darker());
                    });

                    ObjectInputStream objectInputStream = new ObjectInputStream(videoSocket.getInputStream());

                    while (isVideoCallActive && serverRunning.get()) {
                        try {
                            ImageIcon receivedImage = (ImageIcon) objectInputStream.readObject();
                            if (videoDisplayLabel != null) {
                                SwingUtilities.invokeLater(() -> {
                                    videoDisplayLabel.setIcon(receivedImage);
                                });
                            }
                        } catch (ClassNotFoundException e) {
                            System.err.println("Class not found for received object: " + e.getMessage());
                            break;
                        } catch (IOException e) {
                            if (isVideoCallActive && serverRunning.get()) {
                                System.err.println("Video Stream Read Error: " + e.getMessage());
                                SwingUtilities.invokeLater(() -> {
                                    if (receivedVideoStatusLabel != null) receivedVideoStatusLabel.setText("Video: Disconnected");
                                    if (receivedVideoStatusLabel != null) receivedVideoStatusLabel.setForeground(java.awt.Color.RED);
                                });
                                break;
                            }
                        }
                    }
                    System.out.println("Video stream ended from client, or call stopped.");
                    objectInputStream.close();
                    videoSocket.close();
                }

            } catch (IOException e) {
                if (isVideoCallActive && serverRunning.get()) {
                    System.err.println("Video Server IO Error: " + e.getMessage());
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        if (receivedVideoStatusLabel != null) receivedVideoStatusLabel.setText("Video: Error");
                        if (receivedVideoStatusLabel != null) receivedVideoStatusLabel.setForeground(java.awt.Color.RED);
                    });
                } else {
                    System.out.println("Video Server stopped gracefully.");
                }
            } finally {
                stopVideoServer();
            }
        }, "VideoServerThread").start();
    }

    public static void stopVideoServer() {
        if (!isVideoCallActive) {
            System.out.println("Video server is already inactive.");
            return;
        }
        isVideoCallActive = false;
        serverRunning.set(false);

        if (videoDisplayLabel != null) {
            SwingUtilities.invokeLater(() -> videoDisplayLabel.setIcon(null));
        }
        if (videoSocket != null) {
            try {
                videoSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing videoSocket: " + e.getMessage());
            } finally {
                videoSocket = null;
            }
        }
        if (videoServerSocket != null) {
            try {
                videoServerSocket.close();
            } catch (IOException e) {
                System.err.println("Error closing videoServerSocket: " + e.getMessage());
            } finally {
                videoServerSocket = null;
            }
        }
        SwingUtilities.invokeLater(() -> {
            if (receivedVideoStatusLabel != null) {
                receivedVideoStatusLabel.setText("Video: Off");
                receivedVideoStatusLabel.setForeground(java.awt.Color.GRAY);
            }
        });
        System.out.println("Video Server Stopped.");
    }

    public static boolean isVideoCallActive() {
        return isVideoCallActive;
    }
}