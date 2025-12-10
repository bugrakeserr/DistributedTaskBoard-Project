package concurrent;

import java.util.HashMap;
import java.util.Collection;
import java.util.List;
import java.util.ArrayList;

public class ThreadSafeHashMap<K, V> {
    private HashMap<K, V> map;
    private ReadWriteLock lock;

    public ThreadSafeHashMap() {
        this.map = new HashMap<>();
        this.lock = new ReadWriteLock();
    }
    
    public void put(K key, V value) throws InterruptedException {
        lock.lockWrite();
        try {
            map.put(key, value);
        } finally {
            lock.unlockWrite();
        }
    }

    public V get(K key) throws InterruptedException {
        lock.lockRead();
        try {
            return map.get(key);
        } finally {
            lock.unlockRead();
        }
    }

    public void remove(K key) throws InterruptedException {
        lock.lockWrite();
        try {
            map.remove(key);
        } finally {
            lock.unlockWrite();
        }
    }

    public List<K> keys() throws InterruptedException {
        lock.lockRead();
        try {
            return new ArrayList<>(map.keySet());
        } finally {
            lock.unlockRead();
        }
    }
    public Collection<V> values() throws InterruptedException {
        lock.lockRead();
        try {
            // Return a copy to prevent ConcurrentModificationException
            return new ArrayList<>(map.values());
        } finally {
            lock.unlockRead();
        }
    }
    
    /**
     * Atomically check if key exists and put if absent.
     * Prevents race condition in check-then-act patterns.
     * Returns true if put was successful, false if key already existed.
     */
    public boolean putIfAbsent(K key, V value) throws InterruptedException {
        lock.lockWrite();
        try {
            if (map.containsKey(key)) {
                return false;  // Key already exists
            }
            map.put(key, value);
            return true;
        } finally {
            lock.unlockWrite();
        }
    }
}
