package com.mmmi.mediachat.client;

import javax.sound.sampled.*;
import java.io.IOException;
import java.net.Socket;
import java.io.OutputStream;
import java.io.InputStream; // لإضافة InputStream لاستقبال الصوت
import java.util.concurrent.atomic.AtomicBoolean; // لإدارة حالة العميل
import javax.swing.JLabel; // لإضافة مؤشرات النشاط
import javax.swing.SwingUtilities; // لتحديث واجهة المستخدم

public class VoiceClient {

    private static final String SERVER_ADDRESS = "localhost"; // عنوان الخادم
    private static final int VOICE_PORT = 1202; // نفس المنفذ المستخدم في الخادم
    private static Socket voiceSocket;
    private static TargetDataLine microphoneLine; // لالتقاط الصوت
    private static SourceDataLine speakers; // لتشغيل الصوت المستلم (جديد)
    private static volatile boolean isVoiceCallActive = false; // لتتبع حالة المكالمة
    private static AtomicBoolean clientRunning = new AtomicBoolean(false); // للتحكم في ثريدات العميل

    private static JLabel micActivityLabel; // مؤشر نشاط الميكروفون
    private static JLabel voiceActivityLabel; // مؤشر نشاط الصوت المستلم

    // دالة لتعيين مؤشر نشاط الميكروفون من ChatClient
    public static void setMicActivityLabel(JLabel label) {
        micActivityLabel = label;
    }

    // دالة لتعيين مؤشر نشاط الصوت المستلم من ChatClient
    public static void setVoiceActivityLabel(JLabel label) {
        voiceActivityLabel = label;
    }

    public static void startVoiceClient() {
        if (isVoiceCallActive) {
            System.out.println("Voice client is already active.");
            return;
        }
        isVoiceCallActive = true; // تفعيل حالة المكالمة
        clientRunning.set(true); // تعيين العميل على أنه يعمل
        System.out.println("VoiceClient: Attempting to start Voice Client...");
        SwingUtilities.invokeLater(() -> {
            if (micActivityLabel != null) {
                micActivityLabel.setText("Mic: Starting...");
                micActivityLabel.setForeground(java.awt.Color.ORANGE);
            }
            if (voiceActivityLabel != null) {
                voiceActivityLabel.setText("Voice: Starting...");
                voiceActivityLabel.setForeground(java.awt.Color.ORANGE);
            }
        });

        new Thread(() -> {
            try {
                // تنسيق الصوت: 44.1kHz, 16-bit, stereo, signed, little-endian
                AudioFormat format = new AudioFormat(44100, 16, 2, true, false);
                System.out.println("VoiceClient: Audio format set: " + format);

                // تهيئة الميكروفون (لالتقاط الصوت)
                DataLine.Info microphoneInfo = new DataLine.Info(TargetDataLine.class, format);
                if (!AudioSystem.isLineSupported(microphoneInfo)) {
                    System.err.println("VoiceClient Error: Microphone line not supported for format: " + format);
                    SwingUtilities.invokeLater(() -> {
                        if (micActivityLabel != null) micActivityLabel.setText("Mic: Error");
                        if (micActivityLabel != null) micActivityLabel.setForeground(java.awt.Color.RED);
                    });
                    stopVoiceClient();
                    return;
                }
                microphoneLine = (TargetDataLine) AudioSystem.getLine(microphoneInfo);
                microphoneLine.open(format);
                microphoneLine.start();
                System.out.println("VoiceClient: Microphone opened successfully.");
                SwingUtilities.invokeLater(() -> {
                    if (micActivityLabel != null) micActivityLabel.setText("Mic: Active");
                    if (micActivityLabel != null) micActivityLabel.setForeground(java.awt.Color.GREEN.darker());
                });

                // تهيئة السماعات (لتشغيل الصوت المستلم من الخادم)
                DataLine.Info speakerInfo = new DataLine.Info(SourceDataLine.class, format);
                if (!AudioSystem.isLineSupported(speakerInfo)) {
                    System.err.println("VoiceClient Error: Speaker line not supported for format: " + format);
                    SwingUtilities.invokeLater(() -> {
                        if (voiceActivityLabel != null) voiceActivityLabel.setText("Voice: Speaker Error");
                        if (voiceActivityLabel != null) voiceActivityLabel.setForeground(java.awt.Color.RED);
                    });
                    stopVoiceClient();
                    return;
                }
                speakers = (SourceDataLine) AudioSystem.getLine(speakerInfo);
                speakers.open(format);
                speakers.start();
                System.out.println("VoiceClient: Speakers opened successfully.");


                voiceSocket = new Socket(SERVER_ADDRESS, VOICE_PORT);
                System.out.println("Voice Client connected to Voice Server on " + SERVER_ADDRESS + ":" + VOICE_PORT);
                SwingUtilities.invokeLater(() -> {
                    if (voiceActivityLabel != null) {
                        voiceActivityLabel.setText("Voice: Connected");
                        voiceActivityLabel.setForeground(java.awt.Color.BLUE.darker());
                    }
                });

                // ثريد لإرسال الصوت من الميكروفون إلى الخادم
                new Thread(() -> {
                    OutputStream outputStream = null;
                    try {
                        outputStream = voiceSocket.getOutputStream();
                        byte[] buffer = new byte[4096]; // زيادة حجم المخزن المؤقت
                        int bytesRead;

                        while (isVoiceCallActive && clientRunning.get() && !voiceSocket.isClosed() && (bytesRead = microphoneLine.read(buffer, 0, buffer.length)) != -1) {
                            if (bytesRead > 0) {
                                outputStream.write(buffer, 0, bytesRead); // إرسال الصوت
                                SwingUtilities.invokeLater(() -> {
                                    if (micActivityLabel != null) {
                                        micActivityLabel.setText("Mic: Active");
                                        micActivityLabel.setForeground(java.awt.Color.GREEN.darker());
                                    }
                                });
                            }
                        }
                        System.out.println("VoiceClient: Microphone stream ended.");
                    } catch (IOException e) {
                        if (isVoiceCallActive && clientRunning.get()) {
                            System.err.println("Voice Client Send Error: " + e.getMessage());
                            e.printStackTrace();
                            SwingUtilities.invokeLater(() -> {
                                if (micActivityLabel != null) micActivityLabel.setText("Mic: Send Error");
                                if (micActivityLabel != null) micActivityLabel.setForeground(java.awt.Color.RED);
                            });
                        }
                    } finally {
                        System.out.println("VoiceClient: Microphone thread stopped.");
                    }
                }, "VoiceClientSendThread").start();

                // ثريد لاستقبال الصوت من الخادم وتشغيله (جديد)
                new Thread(() -> {
                    InputStream inputStream = null;
                    try {
                        inputStream = voiceSocket.getInputStream();
                        byte[] buffer = new byte[4096]; // حجم المخزن المؤقت يجب أن يتطابق مع الإرسال
                        int bytesRead;

                        while (isVoiceCallActive && clientRunning.get() && !voiceSocket.isClosed() && (bytesRead = inputStream.read(buffer)) != -1) {
                            if (bytesRead > 0) {
                                speakers.write(buffer, 0, bytesRead); // تشغيل الصوت المستلم من الخادم
                                SwingUtilities.invokeLater(() -> {
                                    if (voiceActivityLabel != null) {
                                        voiceActivityLabel.setText("Voice: Active");
                                        voiceActivityLabel.setForeground(java.awt.Color.BLUE.darker());
                                    }
                                });
                            }
                        }
                        System.out.println("VoiceClient: Speaker stream ended.");
                    } catch (IOException e) {
                        if (isVoiceCallActive && clientRunning.get()) {
                            System.err.println("Voice Client Receive Error: " + e.getMessage());
                            e.printStackTrace();
                            SwingUtilities.invokeLater(() -> {
                                if (voiceActivityLabel != null) voiceActivityLabel.setText("Voice: Recv Error");
                                if (voiceActivityLabel != null) voiceActivityLabel.setForeground(java.awt.Color.RED);
                            });
                        }
                    } finally {
                        System.out.println("VoiceClient: Speaker thread stopped.");
                        SwingUtilities.invokeLater(() -> {
                            if (voiceActivityLabel != null && isVoiceCallActive) {
                                voiceActivityLabel.setText("Voice: Idle (Server disconnected)");
                                voiceActivityLabel.setForeground(java.awt.Color.ORANGE);
                            }
                        });
                    }
                }, "VoiceClientReceiveThread").start();


            } catch (LineUnavailableException e) {
                System.err.println("VoiceClient Error: Audio line unavailable: " + e.getMessage());
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    if (micActivityLabel != null) micActivityLabel.setText("Mic: Error");
                    if (micActivityLabel != null) micActivityLabel.setForeground(java.awt.Color.RED);
                    if (voiceActivityLabel != null) voiceActivityLabel.setText("Voice: Error");
                    if (voiceActivityLabel != null) voiceActivityLabel.setForeground(java.awt.Color.RED);
                });
            } catch (IOException e) {
                if (isVoiceCallActive && clientRunning.get()) {
                    System.err.println("Voice Client IO Error (Connection): " + e.getMessage());
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        if (micActivityLabel != null) micActivityLabel.setText("Mic: Conn Error");
                        if (micActivityLabel != null) micActivityLabel.setForeground(java.awt.Color.RED);
                        if (voiceActivityLabel != null) voiceActivityLabel.setText("Voice: Conn Error");
                        if (voiceActivityLabel != null) voiceActivityLabel.setForeground(java.awt.Color.RED);
                    });
                } else {
                    System.out.println("Voice Client stopped gracefully.");
                }
            } finally {
                stopVoiceClient(); // ضمان الإغلاق الآمن لجميع الموارد
            }
        }, "VoiceClientMainThread").start();
    }

    public static void stopVoiceClient() {
        if (!isVoiceCallActive && microphoneLine == null && speakers == null && voiceSocket == null && !clientRunning.get()) {
            System.out.println("Voice client is already inactive or fully stopped.");
            return;
        }
        System.out.println("VoiceClient: Attempting to stop voice client...");
        isVoiceCallActive = false; // تعطيل حالة المكالمة
        clientRunning.set(false); // إيقاف إشارة العميل

        if (microphoneLine != null) {
            try {
                microphoneLine.stop();
                microphoneLine.close();
                System.out.println("VoiceClient: Microphone closed.");
            } catch (Exception e) { System.err.println("Error closing microphone: " + e.getMessage()); }
            finally { microphoneLine = null; }
        }
        if (speakers != null) {
            try {
                speakers.stop();
                speakers.close();
                System.out.println("VoiceClient: Speakers closed.");
            } catch (Exception e) { System.err.println("Error closing speakers: " + e.getMessage()); }
            finally { speakers = null; }
        }
        if (voiceSocket != null) {
            try {
                voiceSocket.close();
                System.out.println("VoiceClient: Voice socket closed.");
            } catch (IOException e) {
                System.err.println("Error closing voiceSocket: " + e.getMessage());
            } finally {
                voiceSocket = null;
            }
        }
        SwingUtilities.invokeLater(() -> {
            if (micActivityLabel != null) {
                micActivityLabel.setText("Mic: Off");
                micActivityLabel.setForeground(java.awt.Color.GRAY);
            }
            if (voiceActivityLabel != null) {
                voiceActivityLabel.setText("Voice: Off");
                voiceActivityLabel.setForeground(java.awt.Color.GRAY);
            }
        });
        System.out.println("Voice Client Stopped.");
    }

    public static boolean isVoiceCallActive() {
        return isVoiceCallActive;
    }
}