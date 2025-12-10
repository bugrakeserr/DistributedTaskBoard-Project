import java.io.*;
import java.net.*;
import java.util.*;
import concurrent.*;

/**
 * Main server class for the Distributed Task Board.
 * Handles client connections and manages shared state.
 */
public class ServerMain {
    private static final int PORT = 8080;
    private static TaskManager taskManager = new TaskManager();

    // Thread-safe client management using custom concurrent utilities
    private static ThreadSafeHashMap<Integer, ClientHandler> clients = new ThreadSafeHashMap<>();
    private static AtomicInteger clientIdCounter = new AtomicInteger(0);
    
    // Track active usernames (thread-safe)
    private static ThreadSafeHashMap<String, Integer> activeUsernames = new ThreadSafeHashMap<>();

    public static void main(String[] args) {
        System.out.println("Server is starting...");
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is listening on port " + PORT);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("New client connected: " + clientSocket.getInetAddress());
                
                int clientId = clientIdCounter.incrementAndGet();
                ClientHandler clientHandler = new ClientHandler(clientSocket, clientId);
                try {
                    clients.put(clientId, clientHandler);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                Thread thread = new Thread(clientHandler);
                thread.start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    // Getter for TaskManager (used by ClientHandler)
    public static TaskManager getTaskManager() {
        return taskManager;
    }

    // Broadcast message to all connected clients (thread-safe)
    public static void broadcastMessage(String message) {
        try {
            System.out.println("[Server] Broadcasting: " + message);
            List<Integer> clientIds = clients.keys();
            for (int clientId : clientIds) {
                ClientHandler client = clients.get(clientId);
                if (client != null) {
                    client.sendMessage(message);
                }
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Remove disconnected client (thread-safe)
    public static void removeClient(int clientId, String username) {
        try {
            clients.remove(clientId);
            if (username != null && !username.isEmpty()) {
                activeUsernames.remove(username);
                System.out.println("[Server] User '" + username + "' disconnected");
                // Broadcast user left
                broadcastMessage("USER_LEFT:" + username);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    
    // Check if username is available and register it (atomic operation)
    public static boolean registerUsername(String username, int clientId) {
        try {
            if (username == null || username.trim().isEmpty()) {
                return false;
            }
            // Atomic check-and-put to prevent race condition
            // Two users trying to register same username simultaneously
            // will now be handled correctly
            return activeUsernames.putIfAbsent(username, clientId);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    // Get list of online users
    public static String getOnlineUsers() {
        try {
            List<String> users = activeUsernames.keys();
            return String.join(",", users);
        } catch (InterruptedException e) {
            e.printStackTrace();
            return "";
        }
    }
}
