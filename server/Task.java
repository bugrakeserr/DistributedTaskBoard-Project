/**
 * Thread-safe Task class.
 * All getters and setters are synchronized to prevent data races
 * when multiple threads access the same task simultaneously.
 */
public class Task {
    // Fields
    private int id;
    private String description;
    private boolean isCompleted;
    private long timestamp;
    private String lastModifiedBy;

    // Constructor
    public Task(int id, String description, String username) {
        this.id = id;
        this.description = description;
        this.isCompleted = false;
        this.timestamp = System.currentTimeMillis();
        this.lastModifiedBy = username;
    }

    // Synchronized getters (prevent reading while another thread is writing)
    public synchronized int getId() {
        return id;
    }
    public synchronized String getDescription() {
        return description;
    }
    public synchronized boolean getIsCompleted() {
        return isCompleted;
    }
    public synchronized long getTimestamp() {
        return timestamp;
    }
    public synchronized String getLastModifiedBy() {
        return lastModifiedBy;
    }

    // Synchronized setters (prevent concurrent writes)
    public synchronized void setDescription(String description) {
        this.description = description;
        this.timestamp = System.currentTimeMillis();
    }
    public synchronized void setIsCompleted(boolean isCompleted) {
        this.isCompleted = isCompleted;
        this.timestamp = System.currentTimeMillis();
    }
    public synchronized void setLastModifiedBy(String username) {
        this.lastModifiedBy = username;
    }

    /**
     * Atomic update - updates all fields in one synchronized block.
     * This ensures no other thread can see a partially updated task.
     */
    public synchronized void update(String description, boolean isCompleted, String username) {
        this.description = description;
        this.isCompleted = isCompleted;
        this.lastModifiedBy = username;
        this.timestamp = System.currentTimeMillis();
    }

    // toString
    @Override
    public synchronized String toString() {
        return "Task [id=" + id + ", description=" + description + ", isCompleted=" + isCompleted + ", lastModifiedBy=" + lastModifiedBy + "]";
    }
}
