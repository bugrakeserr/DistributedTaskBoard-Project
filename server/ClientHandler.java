import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Handles communication with a single connected client.
 * Each client connection runs in its own thread.
 */
public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private int clientId;
    private String username;
    private BufferedReader in;
    private PrintWriter out;

    public ClientHandler(Socket clientSocket, int clientId) {
        this.clientSocket = clientSocket;
        this.clientId = clientId;
        this.username = null;
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            // Wait for CONNECT message with username first
            String connectMessage;
            while ((connectMessage = in.readLine()) != null) {
                if (connectMessage.startsWith("CONNECT:")) {
                    String requestedUsername = connectMessage.substring(8).trim();
                    if (ServerMain.registerUsername(requestedUsername, clientId)) {
                        this.username = requestedUsername;
                        sendMessage("CONNECT_OK:" + ServerMain.getOnlineUsers());
                        System.out.println("[Server] User '" + username + "' connected");
                        // Broadcast new user joined
                        ServerMain.broadcastMessage("USER_JOINED:" + username);
                        break;
                    } else {
                        sendMessage("CONNECT_ERROR:Username already taken or invalid");
                    }
                }
            }
            
            if (username == null) {
                // Connection closed before registering
                return;
            }

            // Send current tasks to new client
            sendCurrentTasks();

            String message;
            while ((message = in.readLine()) != null) {
                System.out.println("[Server] Received from " + username + ": " + message);
                handleMessage(message);
            }
        } catch (IOException e) {
            System.out.println("[Server] Client " + clientId + " (" + username + ") connection error: " + e.getMessage());
        } finally {
            cleanup();
        }
    }

    private void sendCurrentTasks() {
        try {
            List<Task> tasks = ServerMain.getTaskManager().getAllTasks();
            for (Task task : tasks) {
                String message = "ADD:" + task.getId() + ":" + 
                               task.getDescription() + ":" + 
                               task.getIsCompleted() + ":" +
                               task.getLastModifiedBy();
                sendMessage(message);
            }
            System.out.println("[Server] Sent " + tasks.size() + " existing tasks to " + username);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void handleMessage(String message) {
        try {
            // Split with limit to handle colons in task descriptions
            String[] parts = message.split(":", 2);  // Split into max 2 parts first
            if (parts.length < 1) return;
            
            String command = parts[0];
            TaskManager taskManager = ServerMain.getTaskManager();
            
            switch (command) {
                case "ADD":
                    // Client sends: ADD:description
                    if (parts.length < 2 || parts[1].trim().isEmpty()) {
                        System.err.println("[Server] Invalid ADD message: missing description");
                        return;
                    }
                    String description = parts[1];
                    Task task = taskManager.addTask(description, username);
                    
                    // Broadcast to all clients (includes lastModifiedBy)
                    // Use | as separator for description to avoid colon issues
                    String addMessage = "ADD:" + task.getId() + ":" + 
                                      task.getDescription() + ":" + 
                                      task.getIsCompleted() + ":" +
                                      task.getLastModifiedBy();
                    ServerMain.broadcastMessage(addMessage);
                    break;
                    
                case "UPDATE":
                    // Client sends: UPDATE:id:description:isCompleted
                    // Description might contain colons, so parse carefully
                    String updatePayload = parts[1];
                    
                    // Find first colon (after id)
                    int firstColonIdx = updatePayload.indexOf(":");
                    if (firstColonIdx == -1) {
                        System.err.println("[Server] Invalid UPDATE message: " + message);
                        return;
                    }
                    
                    int updateTaskId = Integer.parseInt(updatePayload.substring(0, firstColonIdx));
                    
                    // Find last colon (before isCompleted: true/false)
                    int lastColonIdx = updatePayload.lastIndexOf(":");
                    if (lastColonIdx == firstColonIdx) {
                        System.err.println("[Server] Invalid UPDATE message: " + message);
                        return;
                    }
                    
                    // Description is everything between first and last colon
                    String updatedDescription = updatePayload.substring(firstColonIdx + 1, lastColonIdx);
                    String isCompletedStr = updatePayload.substring(lastColonIdx + 1);
                    boolean isCompleted = Boolean.parseBoolean(isCompletedStr);
                    
                    boolean updated = taskManager.updateTask(updateTaskId, updatedDescription, isCompleted, username);
                    
                    if (updated) {
                        // Broadcast to all clients (includes lastModifiedBy)
                        String updateMessage = "UPDATE:" + updateTaskId + ":" + 
                                              updatedDescription + ":" + isCompleted + ":" +
                                              username;
                        ServerMain.broadcastMessage(updateMessage);
                    }
                    break;
                    
                case "DELETE":
                    // Client sends: DELETE:id
                    if (parts.length < 2) {
                        System.err.println("[Server] Invalid DELETE message: missing id");
                        return;
                    }
                    int deleteTaskId = Integer.parseInt(parts[1].trim());
                    boolean deleted = taskManager.deleteTask(deleteTaskId);
                    
                    if (deleted) {
                        // Only broadcast if task actually existed
                        String deleteMessage = "DELETE:" + deleteTaskId;
                        ServerMain.broadcastMessage(deleteMessage);
                    }
                    break;
            }
            
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("[Server] Error handling message: " + message);
            e.printStackTrace();
        }
    }

    public void sendMessage(String message) {
        if (out != null) {
            out.println(message);
        }
    }

    private void cleanup() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (clientSocket != null) clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        ServerMain.removeClient(clientId, username);
    }
}

