import java.util.ArrayList;
import java.util.List;
import concurrent.AtomicInteger;
import concurrent.ThreadSafeHashMap;

public class TaskManager {
    // Thread-safe storage using custom concurrent utilities
    private ThreadSafeHashMap<Integer, Task> tasks;
    private AtomicInteger taskIdCounter;

    // Constructor
    public TaskManager() {
        this.tasks = new ThreadSafeHashMap<>();
        this.taskIdCounter = new AtomicInteger();
    }

    // Add a new task (thread-safe)
    public Task addTask(String description, String username) throws InterruptedException {
        int id = taskIdCounter.incrementAndGet();
        Task task = new Task(id, description, username);
        tasks.put(id, task);
        System.out.println("[TaskManager] Added task " + id + " by " + username + ": " + description);
        return task;
    }

    // Get all tasks (thread-safe)
    public List<Task> getAllTasks() throws InterruptedException {
        return new ArrayList<>(tasks.values());
    }

    // Get a specific task by id (thread-safe)
    public Task getTask(int taskId) throws InterruptedException {
        return tasks.get(taskId);
    }
    
    // Delete a task (thread-safe)
    public boolean deleteTask(int taskId) throws InterruptedException {
        Task task = getTask(taskId);
        if (task != null) {
            tasks.remove(taskId);
            System.out.println("[TaskManager] Deleted task " + taskId);
            return true;
        }
        return false;
    }

    // Update a task (thread-safe)
    // Uses atomic update to prevent data races when multiple users edit the same task
    public boolean updateTask(int taskId, String description, boolean isCompleted, String username) throws InterruptedException {
        Task task = getTask(taskId);
        if (task != null) {
            // Atomic update - all fields updated in one synchronized block
            task.update(description, isCompleted, username);
            System.out.println("[TaskManager] Updated task " + taskId + " by " + username + ": " + description + ", completed=" + isCompleted);
            return true;
        }
        return false;
    }
}
