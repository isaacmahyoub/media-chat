package com.mmmi.mediachat.server;

import javax.sound.sampled.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean; // لإدارة حالة الخادم بشكل أفضل
import javax.swing.JLabel; // لإضافة مؤشر النشاط
import javax.swing.SwingUtilities; // لتحديث واجهة المستخدم من ثريدات أخرى

public class VoiceServer {

    private static final int VOICE_PORT = 1202; // منفذ مخصص للصوت
    private static ServerSocket voiceServerSocket;
    private static Socket voiceSocket;
    private static SourceDataLine speakers; // لتشغيل الصوت المستلم في الخادم (كانت speakerLine)
    private static volatile boolean isVoiceCallActive = false; // لتتبع حالة المكالمة
    private static AtomicBoolean serverRunning = new AtomicBoolean(false); // للتحكم في ثريدات الخادم

    private static JLabel voiceActivityLabel; // مؤشر نشاط الصوت المستلم

    // دالة لتعيين مؤشر نشاط الصوت المستلم من ChatServer
    public static void setVoiceActivityLabel(JLabel label) {
        voiceActivityLabel = label;
    }

    public static void startVoiceServer() {
        if (isVoiceCallActive) {
            System.out.println("Voice server is already active.");
            return;
        }
        isVoiceCallActive = true; // تفعيل حالة المكالمة
        serverRunning.set(true); // تعيين الخادم على أنه يعمل
        System.out.println("VoiceServer: Attempting to start Voice Server...");
        SwingUtilities.invokeLater(() -> {
            if (voiceActivityLabel != null) {
                voiceActivityLabel.setText("Voice: Starting...");
                voiceActivityLabel.setForeground(java.awt.Color.ORANGE);
            }
        });

        new Thread(() -> {
            try {
                AudioFormat format = new AudioFormat(44100, 16, 2, true, false);
                System.out.println("VoiceServer: Audio format set: " + format);

                DataLine.Info speakerInfo = new DataLine.Info(SourceDataLine.class, format);
                if (!AudioSystem.isLineSupported(speakerInfo)) {
                    System.err.println("VoiceServer Error: Speaker line not supported for format: " + format);
                    SwingUtilities.invokeLater(() -> {
                        if (voiceActivityLabel != null) voiceActivityLabel.setText("Voice: Speaker Error");
                        if (voiceActivityLabel != null) voiceActivityLabel.setForeground(java.awt.Color.RED);
                    });
                    stopVoiceServer();
                    return;
                }
                speakers = (SourceDataLine) AudioSystem.getLine(speakerInfo);
                speakers.open(format);
                speakers.start();
                System.out.println("VoiceServer: Speakers opened successfully.");

                voiceServerSocket = new ServerSocket(VOICE_PORT);
                System.out.println("Voice Server listening on port " + VOICE_PORT);
                SwingUtilities.invokeLater(() -> {
                    if (voiceActivityLabel != null) {
                        voiceActivityLabel.setText("Voice: Waiting for client...");
                        voiceActivityLabel.setForeground(java.awt.Color.BLACK);
                    }
                });

                while (isVoiceCallActive && serverRunning.get()) { // استمر في قبول الاتصالات طالما الخادم يعمل
                    voiceSocket = voiceServerSocket.accept(); // انتظار اتصال العميل
                    System.out.println("Voice Client connected from " + voiceSocket.getInetAddress().getHostAddress());
                    SwingUtilities.invokeLater(() -> {
                        if (voiceActivityLabel != null) {
                            voiceActivityLabel.setText("Voice: Connected");
                            voiceActivityLabel.setForeground(java.awt.Color.BLUE.darker());
                        }
                    });

                    // ثريد منفصل لمعالجة كل اتصال عميل (للسماح بالتحكم في الاتصال الفردي)
                    new Thread(() -> {
                        InputStream inputStream = null;
                        OutputStream outputStream = null;
                        try {
                            inputStream = voiceSocket.getInputStream();
                            outputStream = voiceSocket.getOutputStream();
                            byte[] buffer = new byte[4096]; // زيادة حجم المخزن المؤقت (كان 1024)
                            int bytesRead;
                            while (isVoiceCallActive && serverRunning.get() && !voiceSocket.isClosed() && (bytesRead = inputStream.read(buffer)) != -1) {
                                if (bytesRead > 0) {
                                    speakers.write(buffer, 0, bytesRead);
                                    outputStream.write(buffer, 0, bytesRead);
                                    outputStream.flush();

                                    SwingUtilities.invokeLater(() -> {
                                        if (voiceActivityLabel != null) {
                                            voiceActivityLabel.setText("Voice: Active");
                                            voiceActivityLabel.setForeground(java.awt.Color.BLUE.darker());
                                        }
                                    });
                                }
                            }
                            System.out.println("Voice stream ended from client, or call stopped.");

                        } catch (IOException e) {
                            if (isVoiceCallActive && serverRunning.get()) {
                                System.err.println("Voice Server Stream Error: " + e.getMessage());
                                e.printStackTrace();
                                SwingUtilities.invokeLater(() -> {
                                    if (voiceActivityLabel != null) voiceActivityLabel.setText("Voice: Stream Error");
                                    if (voiceActivityLabel != null) voiceActivityLabel.setForeground(java.awt.Color.RED);
                                });
                            }
                        } finally {
                            try {
                                if (voiceSocket != null && !voiceSocket.isClosed()) {
                                    voiceSocket.close();
                                }
                            } catch (IOException e) {
                                System.err.println("Error closing voiceSocket: " + e.getMessage());
                            } finally {
                                voiceSocket = null;
                            }
                            SwingUtilities.invokeLater(() -> {
                                if (voiceActivityLabel != null && isVoiceCallActive) {
                                    voiceActivityLabel.setText("Voice: Idle (Client disconnected)");
                                    voiceActivityLabel.setForeground(java.awt.Color.ORANGE);
                                }
                            });
                            System.out.println("VoiceServer: Client handler thread finished.");
                        }
                    }, "VoiceServerClientHandler").start();
                }

            } catch (java.net.BindException e) {
                System.err.println("VoiceServer Error: Port " + VOICE_PORT + " is already in use.");
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    if (voiceActivityLabel != null) voiceActivityLabel.setText("Voice: Port In Use");
                    if (voiceActivityLabel != null) voiceActivityLabel.setForeground(java.awt.Color.RED);
                });
            } catch (LineUnavailableException e) {
                System.err.println("VoiceServer Error: Speaker Line Unavailable: " + e.getMessage());
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    if (voiceActivityLabel != null) voiceActivityLabel.setText("Voice: Audio Error");
                    if (voiceActivityLabel != null) voiceActivityLabel.setForeground(java.awt.Color.RED);
                });
            } catch (IOException e) {
                if (isVoiceCallActive && serverRunning.get()) {
                    System.err.println("Voice Server Main IO Error: " + e.getMessage());
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        if (voiceActivityLabel != null) voiceActivityLabel.setText("Voice: IO Error");
                        if (voiceActivityLabel != null) voiceActivityLabel.setForeground(java.awt.Color.RED);
                    });
                } else {
                    System.out.println("Voice Server stopped gracefully.");
                }
            } finally {
                stopVoiceServer();
            }
        }, "VoiceServerMainThread").start();
    }

    public static void stopVoiceServer() {
        if (!isVoiceCallActive && speakers == null && voiceServerSocket == null && voiceSocket == null && !serverRunning.get()) {
            System.out.println("Voice server is already inactive or fully stopped.");
            return;
        }
        System.out.println("VoiceServer: Attempting to stop voice server...");
        isVoiceCallActive = false; // تعطيل حالة المكالمة
        serverRunning.set(false); // إيقاف إشارة الخادم

        if (speakers != null) {
            try {
                speakers.stop();
                speakers.close();
                System.out.println("VoiceServer: Speakers closed.");
            } catch (Exception e) { System.err.println("Error closing speakers: " + e.getMessage()); }
            finally { speakers = null; }
        }
        if (voiceSocket != null) {
            try {
                voiceSocket.close();
                System.out.println("VoiceServer: Client voice socket closed.");
            } catch (IOException e) {
                System.err.println("Error closing voiceSocket (client): " + e.getMessage());
            } finally {
                voiceSocket = null;
            }
        }
        if (voiceServerSocket != null) {
            try {
                voiceServerSocket.close();
                System.out.println("VoiceServer: Server socket closed.");
            } catch (IOException e) {
                System.err.println("Error closing voiceServerSocket: " + e.getMessage());
            } finally {
                voiceServerSocket = null;
            }
        }
        SwingUtilities.invokeLater(() -> {
            if (voiceActivityLabel != null) {
                voiceActivityLabel.setText("Voice: Off");
                voiceActivityLabel.setForeground(java.awt.Color.GRAY);
            }
        });
        System.out.println("Voice Server Stopped.");
    }

    public static boolean isVoiceCallActive() {
        return isVoiceCallActive;
    }
}