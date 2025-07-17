package com.mmmi.mediachat.client;

import com.github.sarxos.webcam.Webcam;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

public class VideoClient {

    // private static final String SERVER_ADDRESS = "localhost";
    // private static final String SERVER_ADDRESS = "0.0.0.0";
    private static final String SERVER_ADDRESS = "13.49.68.159";

    private static final int VIDEO_PORT = 1203;
    private static Socket videoSocket;
    private static Webcam webcam;
    private static ObjectOutputStream objectOutputStream;
    private static volatile boolean isVideoCallActive = false;
    private static AtomicBoolean clientRunning = new AtomicBoolean(false);
    private static JLabel localVideoDisplayLabel;
    private static JLabel cameraStatusLabel; // مؤشر حالة الكاميرا

    public static void setLocalVideoDisplayLabel(JLabel label) {
        localVideoDisplayLabel = label;
    }

    // دالة لتعيين مؤشر حالة الكاميرا من ChatClient
    public static void setCameraStatusLabel(JLabel label) {
        cameraStatusLabel = label;
    }

    public static void startVideoClient() {
        if (isVideoCallActive) {
            System.out.println("Video client is already active.");
            return;
        }
        isVideoCallActive = true;
        clientRunning.set(true);
        System.out.println("Attempting to start Video Client...");

        new Thread(() -> {
            try {
                webcam = Webcam.getDefault();
                if (webcam == null) {
                    System.err.println("No webcam found!");
                    SwingUtilities.invokeLater(() -> {
                        if (cameraStatusLabel != null) cameraStatusLabel.setText("Cam: Not Found");
                        if (cameraStatusLabel != null) cameraStatusLabel.setForeground(java.awt.Color.RED);
                    });
                    stopVideoClient();
                    return;
                }
                webcam.open();
                // webcam.setViewSize(new Dimension(640, 480)); // يمكنك ضبط الدقة هنا

                videoSocket = new Socket(SERVER_ADDRESS, VIDEO_PORT);
                System.out.println("Video Client connected to Video Server on " + SERVER_ADDRESS + ":" + VIDEO_PORT);

                objectOutputStream = new ObjectOutputStream(videoSocket.getOutputStream());

                SwingUtilities.invokeLater(() -> {
                    if (cameraStatusLabel != null) cameraStatusLabel.setText("Cam: Active");
                    if (cameraStatusLabel != null) cameraStatusLabel.setForeground(java.awt.Color.GREEN.darker());
                });

                while (isVideoCallActive && clientRunning.get()) {
                    BufferedImage image = webcam.getImage();
                    if (image != null) {
                        ImageIcon icon = new ImageIcon(image);
                        
                        objectOutputStream.writeObject(icon);
                        objectOutputStream.flush();
                        
                        if (localVideoDisplayLabel != null) {
                            SwingUtilities.invokeLater(() -> {
                                localVideoDisplayLabel.setIcon(icon);
                            });
                        }
                    }
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
                System.out.println("Video stream ended from client, or call stopped.");

            } catch (ConnectException e) {
                System.err.println("Failed to connect to Video Server: " + e.getMessage());
                System.err.println("Please ensure Video Server is running and accessible on " + SERVER_ADDRESS + ":" + VIDEO_PORT);
                SwingUtilities.invokeLater(() -> {
                    if (cameraStatusLabel != null) cameraStatusLabel.setText("Cam: Server Unreachable");
                    if (cameraStatusLabel != null) cameraStatusLabel.setForeground(java.awt.Color.ORANGE);
                });
            } catch (IOException e) {
                if (isVideoCallActive && clientRunning.get()) {
                    System.err.println("Video Client IO Error: " + e.getMessage());
                    SwingUtilities.invokeLater(() -> {
                        if (cameraStatusLabel != null) cameraStatusLabel.setText("Cam: Disconnected");
                        if (cameraStatusLabel != null) cameraStatusLabel.setForeground(java.awt.Color.RED);
                    });
                } else {
                    System.out.println("Video Client stopped gracefully due to external stop request.");
                }
            } finally {
                stopVideoClient();
            }
        }, "VideoClientThread").start();
    }

    public static void stopVideoClient() {
        if (!isVideoCallActive) {
            System.out.println("Video client is already inactive.");
            return;
        }
        isVideoCallActive = false;
        clientRunning.set(false); // إيقاف الثريدات التابعة

        if (webcam != null && webcam.isOpen()) {
            webcam.close();
            webcam = null;
        }
        if (objectOutputStream != null) {
            try {
                objectOutputStream.close();
            } catch (IOException e) {
                System.err.println("Error closing objectOutputStream: " + e.getMessage());
            } finally {
                objectOutputStream = null;
            }
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
        if (localVideoDisplayLabel != null) {
            SwingUtilities.invokeLater(() -> localVideoDisplayLabel.setIcon(null));
        }
        // تحديث حالة مؤشر الكاميرا
        SwingUtilities.invokeLater(() -> {
            if (cameraStatusLabel != null) {
                cameraStatusLabel.setText("Cam: Off");
                cameraStatusLabel.setForeground(java.awt.Color.GRAY);
            }
        });
        System.out.println("Video Client Stopped.");
    }

    public static boolean isVideoCallActive() {
        return isVideoCallActive;
    }
}