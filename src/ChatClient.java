import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;

public class ChatClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;
    private static Socket socket;
    private static PrintWriter out;
    private static BufferedReader in;
    private static JFrame frame;
    private static JTextArea textArea;
    private static JTextField textField;
    private static String username;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                setupGUI();
                connectToServer();
            }
        });
    }

    private static void setupGUI() {
        frame = new JFrame("Chat Client");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(400, 300);
        frame.setLayout(new BorderLayout());

        textArea = new JTextArea();
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        frame.add(scrollPane, BorderLayout.CENTER);

        textField = new JTextField();
        frame.add(textField, BorderLayout.SOUTH);

        textField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                sendMessage();
            }
        });

        frame.setVisible(true);

        // Prompt for username
        String inputUsername = JOptionPane.showInputDialog(frame, "Enter your username:");
        if (inputUsername != null && !inputUsername.isEmpty()) {
            username = inputUsername;
        } else {
            JOptionPane.showMessageDialog(frame, "Username cannot be empty");
            System.exit(0);
        }
    }

    private static void connectToServer() {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // Send username to server
            out.println(username);

            // Start listening for incoming messages from the server
            new Thread(new MessageListener()).start();
        } catch (IOException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(frame, "Unable to connect to server");
            System.exit(1);
        }
    }

    private static void sendMessage() {
        String message = textField.getText();
        if (!message.isEmpty()) {
            out.println(message);
            textField.setText("");
        }
    }

    // A separate thread to listen for incoming messages from the server
    private static class MessageListener implements Runnable {
        @Override
        public void run() {
            try {
                String message;
                while ((message = in.readLine()) != null) {
                    textArea.append(message + "\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
