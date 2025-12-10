package com.example.distributedtaskboard

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.Socket

class TaskClient(
    private val onMessageReceived: (String) -> Unit,
    private val onConnectionStatusChanged: (Boolean) -> Unit,
    private val onConnectionError: (String) -> Unit
) {

    private var socket: Socket? = null
    private var writer: PrintWriter? = null
    private var reader: BufferedReader? = null

    // Default values for local testing
    companion object {
        const val DEFAULT_SERVER_IP = "10.0.2.2"
        const val DEFAULT_SERVER_PORT = 8080
    }

    // Connect with username validation
    suspend fun connect(
        username: String,
        serverIp: String = DEFAULT_SERVER_IP,
        serverPort: Int = DEFAULT_SERVER_PORT
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                socket = Socket(serverIp, serverPort)
                writer = PrintWriter(socket!!.getOutputStream(), true)
                reader = BufferedReader(InputStreamReader(socket!!.getInputStream()))
                
                // Send CONNECT request with username
                writer?.println("CONNECT:$username")
                
                // Wait for server response
                val response = reader?.readLine()
                
                if (response != null && response.startsWith("CONNECT_OK")) {
                    onConnectionStatusChanged(true)
                    
                    // Parse online users from response (CONNECT_OK:user1,user2,...)
                    if (response.contains(":")) {
                        val onlineUsers = response.substring(response.indexOf(":") + 1)
                        if (onlineUsers.isNotEmpty()) {
                            onMessageReceived("ONLINE_USERS:$onlineUsers")
                        }
                    }
                    
                    // Start listening for messages from the server
                    while (true) {
                        val message = reader?.readLine() ?: break
                        onMessageReceived(message)
                    }
                    true
                } else {
                    // Connection rejected
                    val errorMsg = if (response != null && response.startsWith("CONNECT_ERROR:")) {
                        response.substring(14)
                    } else {
                        "Connection rejected by server"
                    }
                    onConnectionError(errorMsg)
                    disconnect()
                    false
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onConnectionError("Cannot connect to server: ${e.message}")
                disconnect()
                false
            } finally {
                onConnectionStatusChanged(false)
            }
        }
    }

    suspend fun sendMessage(message: String) {
        withContext(Dispatchers.IO) {
            writer?.println(message)
        }
    }

    suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            try {
                writer?.close()
                reader?.close()
                socket?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
