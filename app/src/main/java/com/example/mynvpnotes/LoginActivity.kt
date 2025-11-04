package com.example.mynvpnotes

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvRegister: TextView
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var sharedPreferences: SharedPreferences

    companion object {
        const val SHARED_PREF_NAME = "NVPNotesPrefs"
        const val KEY_USERNAME = "username"
        const val KEY_IS_LOGGED_IN = "isLoggedIn"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Initialize database helper FIRST
        databaseHelper = DatabaseHelper(this)

        // Check if user is already logged in
        sharedPreferences = getSharedPreferences(SHARED_PREF_NAME, MODE_PRIVATE)
        if (sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)) {
            navigateToMainActivity()
            return
        }

        // Initialize views
        initViews()

        // Set click listeners
        btnLogin.setOnClickListener {
            loginUser()
        }

        tvRegister.setOnClickListener {
            navigateToRegister()
        }
    }

    private fun initViews() {
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvRegister = findViewById(R.id.tvRegister)

        // Set text colors to ensure visibility
        etUsername.setTextColor(Color.BLACK)
        etPassword.setTextColor(Color.BLACK)

        // Set hint colors
        etUsername.setHintTextColor(Color.parseColor("#888888"))
        etPassword.setHintTextColor(Color.parseColor("#888888"))

        // Make the register text clickable
        tvRegister.isClickable = true
    }

    private fun loginUser() {
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString().trim()

        // Validation
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            showToast("Please fill all fields")
            return
        }

        // Check user credentials
        if (databaseHelper.checkUser(username, password)) {
            // Save login state
            val editor = sharedPreferences.edit()
            editor.putBoolean(KEY_IS_LOGGED_IN, true)
            editor.putString(KEY_USERNAME, username)
            editor.apply()

            showToast("Login successful!")
            navigateToMainActivity()
        } else {
            showToast("Invalid username or password")
        }
    }

    private fun navigateToRegister() {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        // Don't finish() here so user can come back to login
    }

    private fun navigateToMainActivity() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish() // Finish LoginActivity so user can't go back
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}