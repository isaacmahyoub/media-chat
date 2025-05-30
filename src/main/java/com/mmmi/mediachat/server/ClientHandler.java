package com.mmmi.mediachat.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles communication with a single client connected to the chat server.
 */
public class ClientHandler implements Runnable {

    private static final Logger LOGGER = Logger.getLogger(ClientHandler.class.getName());
    private Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;
    private String clientUsername; // يمكن استخدام هذا لتخزين اسم المستخدم بعد تسجيل الدخول

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try {
            // تهيئة تيارات الإدخال والإخراج للدردشة النصية
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true); // AutoFlush

            // في هذه المرحلة، سنقوم فقط بقراءة رسالة ترحيب أولية وإرسال رد.
            // لاحقاً سنضيف منطق تسجيل الدخول والدردشة.

            String inputLine;
            // حلقة لقراءة الرسائل من العميل
            while ((inputLine = in.readLine()) != null) {
                LOGGER.log(Level.INFO, "Received from client {0}: {1}",
                        new Object[]{clientSocket.getInetAddress().getHostAddress(), inputLine});

                // مثال على الرد: أرسل نفس الرسالة مرة أخرى للعميل (Echo)
                out.println("Server received: " + inputLine);

                // يمكن هنا إضافة منطق لمعالجة الرسالة (مثلاً: التحقق من تسجيل الدخول، إعادة توجيه الرسائل)
                if ("bye".equalsIgnoreCase(inputLine.trim())) {
                    LOGGER.log(Level.INFO, "Client {0} requested to disconnect.", clientSocket.getInetAddress().getHostAddress());
                    break; // كسر الحلقة لإنهاء التعامل مع هذا العميل
                }
            }

        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Error handling client {0}: {1}",
                    new Object[]{clientSocket.getInetAddress().getHostAddress(), ex.getMessage()});
        } finally {
            // التأكد من إغلاق الموارد عند انتهاء التعامل مع العميل
            try {
                if (out != null) out.close();
                if (in != null) in.close();
                if (clientSocket != null && !clientSocket.isClosed()) {
                    clientSocket.close();
                    LOGGER.log(Level.INFO, "Client {0} disconnected.", clientSocket.getInetAddress().getHostAddress());
                }
            } catch (IOException ex) {
                LOGGER.log(Level.SEVERE, "Error closing resources for client {0}: {1}",
                        new Object[]{clientSocket.getInetAddress().getHostAddress(), ex.getMessage()});
            }
        }
    }
}