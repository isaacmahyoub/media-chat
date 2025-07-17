
package com.mmmi.mediachat.client;

import javax.sound.sampled.*;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

public class VoiceClient {

    private static final String SERVER_ADDRESS = "localhost";
    // private static final String SERVER_ADDRESS = "0.0.0.0";

    private static final int VOICE_PORT = 1202;
    private static Socket voiceSocket;
    private static TargetDataLine microphoneLine;
    private static SourceDataLine speakers;
    private static volatile boolean isVoiceCallActive = false;
    private static AtomicBoolean clientRunning = new AtomicBoolean(false);

    private static JLabel micActivityLabel;
    private static JLabel voiceActivityLabel;

    public static void setMicActivityLabel(JLabel label) {
        micActivityLabel = label;
    }

    public static void setVoiceActivityLabel(JLabel label) {
        voiceActivityLabel = label;
    }

    public static void startVoiceClient() {
        if (isVoiceCallActive) {
            System.out.println("Voice client is already active.");
            return;
        }
        isVoiceCallActive = true;
        clientRunning.set(true);
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
                AudioFormat format = new AudioFormat(44100, 16, 1, true, false);
                System.out.println("VoiceClient: Audio format set: " + format);

                DataLine.Info micInfo = new DataLine.Info(TargetDataLine.class, format);
                microphoneLine = (TargetDataLine) AudioSystem.getLine(micInfo);
                microphoneLine.open(format);
                microphoneLine.start();

                SwingUtilities.invokeLater(() -> {
                    if (micActivityLabel != null) {
                        micActivityLabel.setText("Mic: Active");
                        micActivityLabel.setForeground(java.awt.Color.GREEN.darker());
                    }
                });

                DataLine.Info speakerInfo = new DataLine.Info(SourceDataLine.class, format);
                speakers = (SourceDataLine) AudioSystem.getLine(speakerInfo);
                speakers.open(format);
                speakers.start();

                SwingUtilities.invokeLater(() -> {
                    if (voiceActivityLabel != null) {
                        voiceActivityLabel.setText("Voice: Ready");
                        voiceActivityLabel.setForeground(java.awt.Color.GREEN.darker());
                    }
                });

                voiceSocket = new Socket(SERVER_ADDRESS, VOICE_PORT);
                OutputStream outputStream = voiceSocket.getOutputStream();
                InputStream inputStream = voiceSocket.getInputStream();

                SwingUtilities.invokeLater(() -> {
                    if (voiceActivityLabel != null) {
                        voiceActivityLabel.setText("Voice: Connected");
                        voiceActivityLabel.setForeground(java.awt.Color.BLUE.darker());
                    }
                });

                new Thread(() -> {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    try {
                        while (isVoiceCallActive && clientRunning.get() && !voiceSocket.isClosed()
                                && (bytesRead = microphoneLine.read(buffer, 0, buffer.length)) != -1) {
                            if (bytesRead > 0) {
                                outputStream.write(buffer, 0, bytesRead);
                                SwingUtilities.invokeLater(() -> {
                                    if (micActivityLabel != null) {
                                        micActivityLabel.setText("Mic: Sending");
                                        micActivityLabel.setForeground(java.awt.Color.GREEN.darker());
                                    }
                                });
                            }
                        }
                    } catch (IOException e) {
                        System.err.println("Mic Send Error: " + e.getMessage());
                        SwingUtilities.invokeLater(() -> {
                            if (micActivityLabel != null) {
                                micActivityLabel.setText("Mic: Send Error");
                                micActivityLabel.setForeground(java.awt.Color.RED);
                            }
                        });
                    }
                    System.out.println("VoiceClient: Microphone stream ended.");
                }, "VoiceClientSendThread").start();

                new Thread(() -> {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    try {
                        while (isVoiceCallActive && clientRunning.get() && !voiceSocket.isClosed()
                                && (bytesRead = inputStream.read(buffer)) != -1) {
                            speakers.write(buffer, 0, bytesRead);
                            SwingUtilities.invokeLater(() -> {
                                if (voiceActivityLabel != null) {
                                    voiceActivityLabel.setText("Voice: Receiving");
                                    voiceActivityLabel.setForeground(java.awt.Color.BLUE.darker());
                                }
                            });
                        }
                    } catch (IOException e) {
                        System.err.println("Speaker Receive Error: " + e.getMessage());
                        SwingUtilities.invokeLater(() -> {
                            if (voiceActivityLabel != null) {
                                voiceActivityLabel.setText("Voice: Receive Error");
                                voiceActivityLabel.setForeground(java.awt.Color.RED);
                            }
                        });
                    }
                    System.out.println("VoiceClient: Speaker stream ended.");
                }, "VoiceClientReceiveThread").start();

            } catch (LineUnavailableException | IOException e) {
                System.err.println("VoiceClient Init Error: " + e.getMessage());
                stopVoiceClient();
            }
        }, "VoiceClientMainThread").start();
    }

    public static void stopVoiceClient() {
        isVoiceCallActive = false;
        clientRunning.set(false);

        try {
            if (microphoneLine != null) {
                microphoneLine.stop();
                microphoneLine.close();
            }
            if (speakers != null) {
                speakers.stop();
                speakers.close();
            }
            if (voiceSocket != null) {
                voiceSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error during stop: " + e.getMessage());
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
