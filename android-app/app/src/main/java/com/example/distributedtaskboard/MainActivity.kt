package com.example.distributedtaskboard

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var taskAdapter: TaskAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var editTextTask: EditText
    private lateinit var buttonAddTask: Button
    private lateinit var textOnlineUsers: android.widget.TextView
    private lateinit var taskClient: TaskClient
    private lateinit var username: String
    
    // Track online users
    private val onlineUsers = mutableSetOf<String>()

    // CHANGE THIS IP to your computer's real IP address (e.g., "192.168.1.x")
    // if you want to run the app on a real phone.
    // Keep "10.0.2.2" if using the Android Emulator on the same machine as the server.
    private val SERVER_IP = "35.239.33.56" 
    private val SERVER_PORT = 8080

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Get username from intent
        username = intent.getStringExtra("username") ?: ""
        if (username.isEmpty()) {
            // No username, go back to login
            goBackToLogin("Please enter a username")
            return
        }
        
        setContentView(R.layout.activity_main)
        
        // Update title to show username
        title = "Task Board - $username"

        recyclerView = findViewById(R.id.recyclerViewTasks)
        editTextTask = findViewById(R.id.editTextTask)
        buttonAddTask = findViewById(R.id.buttonAddTask)
        textOnlineUsers = findViewById(R.id.textOnlineUsers)
        
        // Add self to online users
        onlineUsers.add(username)
        updateOnlineUsersDisplay()

        // Initialize with an empty list. Data will come from the server.
        taskAdapter = TaskAdapter(
            mutableListOf(),
            onTaskCompleted = { task ->
                // Send update to server
                val message = "UPDATE:${task.id}:${task.description}:${task.isCompleted}"
                lifecycleScope.launch { taskClient.sendMessage(message) }
            },
            onTaskUpdated = { task ->
                showUpdateTaskDialog(task)
            },
            onTaskDeleted = { task ->
                // Send delete to server
                val message = "DELETE:${task.id}"
                lifecycleScope.launch { taskClient.sendMessage(message) }
            }
        )

        recyclerView.adapter = taskAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        taskClient = TaskClient(
            onMessageReceived = { message ->
                runOnUiThread {
                    handleServerMessage(message)
                }
            },
            onConnectionStatusChanged = { isConnected ->
                runOnUiThread {
                    val status = if (isConnected) "Connected as $username" else "Disconnected"
                    Toast.makeText(this, status, Toast.LENGTH_SHORT).show()
                }
            },
            onConnectionError = { error ->
                runOnUiThread {
                    goBackToLogin(error)
                }
            }
        )

        buttonAddTask.setOnClickListener {
            val taskDescription = editTextTask.text.toString()
            if (taskDescription.isNotBlank()) {
                // Send add command to server
                val message = "ADD:$taskDescription"
                lifecycleScope.launch { taskClient.sendMessage(message) }
                editTextTask.text.clear()
            }
        }

        // Connect to server with username
        lifecycleScope.launch {
            taskClient.connect(username, SERVER_IP, SERVER_PORT)
        }
    }

    private fun goBackToLogin(errorMessage: String) {
        val intent = Intent(this, LoginActivity::class.java)
        intent.putExtra("error", errorMessage)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
    
    private fun updateOnlineUsersDisplay() {
        val userList = onlineUsers.sorted().joinToString(", ") { user ->
            if (user == username) "$user (you)" else user
        }
        textOnlineUsers.text = if (onlineUsers.isEmpty()) "No users online" else userList
    }

    private fun handleServerMessage(message: String) {
        try {
            val colonIndex = message.indexOf(":")
            if (colonIndex == -1) return
            
            val command = message.substring(0, colonIndex)
            val payload = message.substring(colonIndex + 1)

            when (command) {
                "ADD", "UPDATE" -> {
                    // Format: ADD:id:description:isCompleted:lastModifiedBy
                    // Description might contain colons, so we parse from both ends
                    val firstColon = payload.indexOf(":")
                    if (firstColon == -1) return
                    
                    val id = payload.substring(0, firstColon).toInt()
                    val rest = payload.substring(firstColon + 1)  // description:isCompleted:lastModifiedBy
                    
                    // Find last two colons for isCompleted and lastModifiedBy
                    val lastColon = rest.lastIndexOf(":")
                    val secondLastColon = rest.lastIndexOf(":", lastColon - 1)
                    
                    if (secondLastColon == -1) return
                    
                    val description = rest.substring(0, secondLastColon)
                    val isCompleted = rest.substring(secondLastColon + 1, lastColon).toBoolean()
                    val lastModifiedBy = rest.substring(lastColon + 1)
                    
                    val task = Task(id, description, isCompleted, lastModifiedBy)
                    if (command == "ADD") {
                        taskAdapter.addTask(task)
                    } else {
                        taskAdapter.updateTask(task)
                    }
                }
                "DELETE" -> {
                    // Format: DELETE:id
                    val id = payload.toInt()
                    taskAdapter.removeTaskById(id)
                }
                "USER_JOINED" -> {
                    // Format: USER_JOINED:username
                    val joinedUser = payload
                    onlineUsers.add(joinedUser)
                    updateOnlineUsersDisplay()
                    if (joinedUser != username) {
                        Toast.makeText(this, "$joinedUser joined", Toast.LENGTH_SHORT).show()
                    }
                }
                "USER_LEFT" -> {
                    // Format: USER_LEFT:username
                    val leftUser = payload
                    onlineUsers.remove(leftUser)
                    updateOnlineUsersDisplay()
                    Toast.makeText(this, "$leftUser left", Toast.LENGTH_SHORT).show()
                }
                "ONLINE_USERS" -> {
                    // Format: ONLINE_USERS:user1,user2,user3
                    if (payload.isNotEmpty()) {
                        val users = payload.split(",")
                        onlineUsers.clear()
                        onlineUsers.addAll(users)
                        updateOnlineUsersDisplay()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun showUpdateTaskDialog(task: Task) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Update Task")

        val input = EditText(this)
        input.setText(task.description)
        builder.setView(input)

        builder.setPositiveButton("Update") { dialog, _ ->
            val newDescription = input.text.toString()
            if (newDescription.isNotBlank()) {
                 // Send update to server
                 val message = "UPDATE:${task.id}:$newDescription:${task.isCompleted}"
                 lifecycleScope.launch { taskClient.sendMessage(message) }
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }

        builder.show()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        lifecycleScope.launch {
            taskClient.disconnect()
        }
    }
}
