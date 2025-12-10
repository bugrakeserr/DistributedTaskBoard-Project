# Distributed Task Board

A real-time collaborative task management app built with a multithreaded Java server and Android client.

## Features

- **Real-time sync** - All changes appear instantly on all devices
- **Add/Update/Delete tasks** - Full CRUD operations
- **User presence** - See who's online, get join/leave notifications
- **Task attribution** - See who last modified each task

## Architecture

```
┌─────────────────┐       TCP:8080        ┌─────────────────┐
│  Android App    │◄────────────────────►│   Java Server    │
│  (Kotlin)       │                       │  (Multithreaded) │
└─────────────────┘                       └─────────────────┘
```

## Distributed System Concepts

| Concept | Implementation |
|---------|----------------|
| **Shared State** | All clients share the same task list stored on server |
| **State Synchronization** | Server broadcasts every change to all connected clients |
| **Consistency** | Single source of truth (server) ensures all clients see same data |
| **Fault Tolerance** | Clients reconnect and receive full state on rejoin |
| **Scalability** | One thread per client allows multiple simultaneous connections |

## Concurrency Concepts

| Concept | Implementation |
|---------|----------------|
| **Thread-per-Client** | Each client connection handled by dedicated thread |
| **Race Condition Prevention** | Atomic `putIfAbsent()` prevents duplicate usernames |
| **Read-Write Locking** | Multiple readers OR single writer for efficiency |
| **Synchronized Updates** | Task fields updated atomically to prevent partial reads |

## Custom Concurrency Primitives

Built from scratch **without `java.util.concurrent`**:

| Class | Purpose |
|-------|---------|
| `AtomicInteger` | Thread-safe counter for unique IDs |
| `ReadWriteLock` | Multiple readers OR single writer |
| `ThreadSafeHashMap` | Thread-safe map with `putIfAbsent()` |

## Project Structure

```
├── android-app/          # Android client (Kotlin)
│   ├── LoginActivity.kt  # User login
│   ├── MainActivity.kt   # Task board UI
│   └── TaskClient.kt     # Socket communication
│
└── server/               # Java server
    ├── ServerMain.java   # Entry point, broadcasts
    ├── ClientHandler.java # Per-client thread
    ├── TaskManager.java  # Task CRUD operations
    └── concurrent/       # Custom concurrency classes
```

## Running

### Server
```bash
# Option 1: Use script
bash run_server.sh

# Option 2: Manual
cd server
javac -d bin *.java concurrent/*.java
java -cp bin ServerMain
```

### Android
1. Install `DistributedTaskBoard.apk` on device (enable "Install from unknown sources")
2. Open app, enter username and connect

## Protocol

| Client → Server | Server → Client |
|-----------------|-----------------|
| `CONNECT:username` | `CONNECT_OK:users` |
| `ADD:description` | `ADD:id:desc:done:user` |
| `UPDATE:id:desc:done` | `UPDATE:id:desc:done:user` |
| `DELETE:id` | `DELETE:id` |

---

**CMPE436 - Distributed and Concurrent Programming**
