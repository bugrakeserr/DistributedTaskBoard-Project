package com.example.distributedtaskboard

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var editTextUsername: EditText
    private lateinit var buttonConnect: Button
    private lateinit var textError: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        editTextUsername = findViewById(R.id.editTextUsername)
        buttonConnect = findViewById(R.id.buttonConnect)
        textError = findViewById(R.id.textError)

        // Check if we came back with an error
        val error = intent.getStringExtra("error")
        if (error != null) {
            textError.text = error
            textError.visibility = TextView.VISIBLE
        }

        buttonConnect.setOnClickListener {
            val username = editTextUsername.text.toString().trim()
            
            if (username.isEmpty()) {
                showError("Username cannot be empty")
                return@setOnClickListener
            }
            
            if (username.contains(":") || username.contains(",")) {
                showError("Username cannot contain ':' or ','")
                return@setOnClickListener
            }

            if (username.length > 20) {
                showError("Username too long (max 20 characters)")
                return@setOnClickListener
            }
            
            // Pass username to MainActivity which will handle actual connection
            val intent = Intent(this@LoginActivity, MainActivity::class.java)
            intent.putExtra("username", username)
            startActivity(intent)
            finish()
        }
    }

    private fun showError(message: String) {
        textError.text = message
        textError.visibility = TextView.VISIBLE
    }
}
