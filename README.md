# 💬 MediaChat: A Comprehensive Chat, Voice, Video, and File Transfer Application

MediaChat is a full-featured communication application built using Java Swing for the graphical user interface and Network Sockets for communication. The project aims to provide a comprehensive communication platform that supports text messages, voice calls, video calls, and the ability to send and receive files.

## 🌟 Key Features

* **Text Chat:**
    * Send and receive real-time text messages.
    * Organized and attractive user interface for displaying messages.
* **Voice Call:**
    * Bi-directional voice communication between client and server.
    * Captures audio from the microphone and sends it, and plays received audio through speakers.
    * Visual indicators for microphone activity and received voice activity.
* **Video Call:**
    * Live video streaming from the client's webcam to the server.
    * Displays local video feed (from the client's camera) within the UI.
    * Visual indicators for camera status.
* **File Transfer:**
    * Ability to send files from the client to the server.
    * Progress bar and status indicator to show file transfer progress.
* **Modern User Interface:**
    * Utilizes the `FlatLaf` library for a modern and appealing look and feel.
    * Employs `MigLayout` for flexible and easy-to-use component layout.
* **Resource Management:**
    * Efficient handling of network, audio, and video resources to ensure proper closure and prevent resource leaks.

## 🛠️ Technologies Used

* **Java SE:** The primary language for application development.
* **Java Swing:** For building the graphical user interfaces for both client and server.
* **Java Sockets:** For network communication between the client and server.
* **[Sarxos Webcam Library](https://github.com/sarxos/webcam-capture):** For capturing video from the webcam (must be added as a dependency).
* **[FlatLaf](https://www.formdev.com/flatlaf/):** For a modern look and feel for the UI (must be added as a dependency).
* **[MigLayout](http://www.miglayout.com/):** For laying out UI components (must be added as a dependency).

## 🚀 How to Run

### Prerequisites

* Java Development Kit (JDK) 8 or later.
* An Integrated Development Environment (IDE) Apache NetBeans.
* A working webcam (for video calls).

### Project Setup

1.  **Clone the Repository:**
    ```bash
    git clone https://github.com/isaacmahyoub/media-chat.git
    cd MediaChat
    ```
    *If you don't have a Git repository, you can manually download the project files.*

2.  **Open Project in IDE:**
    * Open the `MediaChat` project in your chosen IDE (NetBeans).

3.  **Add Libraries (Dependencies):**
    The following JAR files must be added to your project's build path (or Libraries):
    * `webcam-capture-x.x.x.jar` (from the Sarxos Webcam library)
    * `flatlaf-x.x.x.jar` (from the FlatLaf library)
    * `miglayout-swing-x.x.jar` (from the MigLayout library)
    * `slf4j-api-x.x.x.jar` (often required by Sarxos Webcam)
    * `slf4j-simple-x.x.x.jar` (for simple console logging, optional but recommended)

    *You can usually find these JARs on the libraries' official websites or via Maven Central if you are using Maven/Gradle.*

### Steps to Run

1.  **Build the Project:**
    In your IDE, perform a "Clean and Build Project" to ensure all changes and libraries are properly compiled.

2.  **Run the Server:**
    * Navigate to the `com.mmmi.mediachat.server.ChatServer` file.
    * Right-click on the file and select "Run File" (or its equivalent in your IDE).
    * Confirm that the server starts and listens on the required ports (you will see messages in the console).

3.  **Run the Client:**
    * Navigate to the `com.mmmi.mediachat.client.LoginClient` file.
    * Right-click on the file and select "Run File".
    * Enter your desired username in the login window and click "Connect to Chat".

4.  **Start Calls/Chat:**
    * **Chat:** Type messages in the text field and press Enter or the "Send" button.
    * **Voice Call:**
        * On the server interface, click "Start Voice Call".
        * On the client interface, click "Start Voice Call".
    * **Video Call:**
        * (After server is running) On the client interface, click "Start Video Call".
    * **File Transfer:**
        * (After chat connection is established) On the client interface, click "Send File" and select a file.
     
##  📸  Screenshots

### Login Interface
<img width="547" height="367" alt="image" src="https://github.com/user-attachments/assets/b37a8a3e-81f3-47cc-bb6c-a3f64b9a9183" />

### Chat Server Interface
<img width="1047" height="991" alt="image" src="https://github.com/user-attachments/assets/ff686dd2-dfcf-4f08-8647-baf1a2cab90b" />
<img width="1047" height="991" alt="image" src="https://github.com/user-attachments/assets/ca7be344-4cb3-42d2-b406-cd7016f6ab2f" />

### Chat Client Interface
<img width="1047" height="991" alt="image" src="https://github.com/user-attachments/assets/6fedc334-503f-4870-8d07-33d7f23eb3dc" />
<img width="1047" height="991" alt="image" src="https://github.com/user-attachments/assets/ef5b13d3-ed5b-45f7-a346-2df6f81b90f7" />


## ⚠️ Known Issues and Future Enhancements

* **Multi-Client Support:** The current design for voice and video services tends to support only one client at a time. Extending this to group calls would require more complex broadcasting logic.
* **Error Handling:** While error handling has been improved, more robust and user-friendly error messages can always be added.
* **Audio/Video Compression:** For real-world applications, audio and video compression (codecs) should be used to reduce bandwidth consumption.
* **Server-side Video Display:** Currently, the server does not display the received video feed from the client. A `JLabel` could be added to the server's UI for video display.
* **Secure Communication:** Encryption is not currently implemented. For sensitive applications, security layers (like SSL/TLS) should be added.
* **Camera/Microphone Detection:** Detection of audio and video devices could be improved to identify default devices or allow user selection.
* **Disconnection Handling:** Improve how sudden disconnections from either the client or server side are handled.

## 👥 Contributors

* **Isaac Mahyoub Abdullah Abdulkhaliq**
* **Mohammed Hassan Mohammed Abdullah**
* **Moayad Abdulghani Abdullah Abdulkhaliq**
* **Muneef Mustafa Abdullah Ibrahim**


## 🤝 Contributing

Contributions are welcome! If you have suggestions or improvements, feel free to open an issue or submit a pull request.


