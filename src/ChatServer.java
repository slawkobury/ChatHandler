import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ChatServer {

    private ServerSocket serverSocket;
    private Map<String, PrintWriter> users;
    private Map<String, List<String>> groups;
    private ExecutorService pool;


    public ChatServer() {
        users = new HashMap<>();
        groups = new HashMap<>();
        pool = Executors.newCachedThreadPool();
    }


    // Akceptowanie połączeń i uruchamianie nowych wątków
    public void startServer(int port) {
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server started on port " + port);

            while (true) {
                Socket clientSocket = serverSocket.accept();
                pool.execute(new ClientHandler(clientSocket));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private class ClientHandler implements Runnable {

        private Socket clientSocket;
        private PrintWriter out;
        private BufferedReader in;
        private String username;


        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }


        public void run() {

            try {
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                out = new PrintWriter(clientSocket.getOutputStream());

                // Rejestracja użytkownika
                out.println("Enter your username: ");
                username = in.readLine();
                synchronized (users) {
                    if (users.containsKey(username)) {
                        out.println("Username alredy taken.");
                        return;                 //???
                    }
                    users.put(username, out);
                }
                out.println("Welcome " + username + "!");
                String message;
                while ((message = in.readLine()) != null) {
                    // Obsługa wiadomości prywatnych lub grupowych
                    if (message.startsWith("/private")) {
                        handlePrivateMessage(message);
                    } else if (message.startsWith("/group")) {
                        handleGroupMessage(message);
                    } else {
                        broadcastMessage(username + ": " + message);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                closeConnection();
            }
        }

        private void handlePrivateMessage(String message) {
            String[] parts = message.split(" ", 3);
            if (parts.length < 3) {
                out.println("Invalid private message format.");
                return;
            }
            String recipient = parts[1];
            String msg = parts[2];

            PrintWriter recipientOut = users.get(recipient);
            if (recipientOut != null) {
                recipientOut.println(username + " (private): " + msg);
            } else {
                out.println("User " + recipient + " not found.");
            }
        }

        private void handleGroupMessage(String message) {
            String[] parts = message.split(" ", 3);
            if (parts.length < 3) {
                out.println("Invalid group message format");
                return;
            }
            String groupName = parts[1];
            String msg = parts[2];

            List<String> groupMembers = groups.get(groupName);
            if (groupMembers != null) {
                for (String member : groupMembers) {
                    PrintWriter groupMemberOut = users.get(member);
                    if (groupMemberOut != null) {
                        groupMemberOut.println(username + " (Group " + groupName + "): " + msg);
                    }
                }
            } else {
                out.println("Group " + groupName + "not found.");
            }

        }

        private void broadcastMessage(String message){
            synchronized(users){
                for(PrintWriter writer : users.values()){
                    writer.println(message);
                }
            }
        }

        private void closeConnection(){
            try{
                synchronized (users){
                    users.remove(username);
                }
                if (out != null){
                    out.close();
                }
                if ( in != null){
                    in.close();
                }
                if (clientSocket != null){
                    clientSocket.close();
                }
            } catch (IOException e){
                e.printStackTrace();
            }
        }
    }


    public static void main(String[] args) {
        ChatServer server = new ChatServer();
        server.startServer(12345);
    }

}
