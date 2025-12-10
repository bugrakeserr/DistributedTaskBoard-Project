package concurrent;

public class AtomicInteger {
    private int value;

    public AtomicInteger(int value) {
        this.value = value;
    }

    public AtomicInteger() {
        this.value = 0;
    }

    public synchronized int get() {
        return value;
    }

    public synchronized void set(int value) {
        this.value = value;
    }

    public synchronized int incrementAndGet() {
        this.value++;
        return value;
    }
}
