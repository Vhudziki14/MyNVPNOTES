package com.example.mynvpnotes

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.util.regex.Pattern

class RegisterActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var tvLogin: TextView
    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var onBackPressedCallback: OnBackPressedCallback
    private var hasUnsavedChanges: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // Initialize database helper
        databaseHelper = DatabaseHelper(this)

        // Initialize views
        initViews()

        // Set up back pressed handler
        setupBackPressedHandler()

        // Set click listeners
        btnRegister.setOnClickListener {
            registerUser()
        }

        tvLogin.setOnClickListener {
            navigateToLogin()
        }

        // Setup text change listeners for unsaved changes detection
        setupTextChangeListeners()
    }

    private fun setupBackPressedHandler() {
        // Create the callback
        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Handle the back button event
                handleBackAction()
            }
        }

        // Add the callback to the OnBackPressedDispatcher
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    private fun handleBackAction() {
        if (hasUnsavedChanges) {
            showUnsavedChangesDialog()
        } else {
            navigateToLogin()
        }
    }

    private fun showUnsavedChangesDialog() {
        AlertDialog.Builder(this)
            .setTitle("Unsaved Changes")
            .setMessage("You have unsaved changes in the registration form. Do you want to discard them and go back to login?")
            .setPositiveButton("Discard") { dialog, which ->
                navigateToLogin()
            }
            .setNegativeButton("Keep Editing") { dialog, which ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }

    private fun initViews() {
        etUsername = findViewById(R.id.etUsername)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnRegister = findViewById(R.id.btnRegister)
        tvLogin = findViewById(R.id.tvLogin)

        // Set text colors to ensure visibility
        etUsername.setTextColor(Color.BLACK)
        etEmail.setTextColor(Color.BLACK)
        etPassword.setTextColor(Color.BLACK)
        etConfirmPassword.setTextColor(Color.BLACK)

        // Set hint colors
        etUsername.setHintTextColor(Color.parseColor("#888888"))
        etEmail.setHintTextColor(Color.parseColor("#888888"))
        etPassword.setHintTextColor(Color.parseColor("#888888"))
        etConfirmPassword.setHintTextColor(Color.parseColor("#888888"))

        // Make the login text clickable
        tvLogin.isClickable = true
    }

    private fun setupTextChangeListeners() {
        // Track changes for unsaved changes detection
        val textWatcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (!hasUnsavedChanges) {
                    hasUnsavedChanges = true
                }
            }
        }

        etUsername.addTextChangedListener(textWatcher)
        etEmail.addTextChangedListener(textWatcher)
        etPassword.addTextChangedListener(textWatcher)
        etConfirmPassword.addTextChangedListener(textWatcher)
    }

    private fun registerUser() {
        val username = etUsername.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()

        // Validation
        if (!validateInput(username, email, password, confirmPassword)) {
            return
        }

        // Register user
        val isRegistered = databaseHelper.registerUser(username, email, password)

        if (isRegistered) {
            showToast("Registration successful! Please login.")
            // Reset unsaved changes flag after successful registration
            hasUnsavedChanges = false
            navigateToLogin()
        } else {
            showToast("Registration failed. Please try again.")
        }
    }

    private fun validateInput(username: String, email: String, password: String, confirmPassword: String): Boolean {
        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(email) ||
            TextUtils.isEmpty(password) || TextUtils.isEmpty(confirmPassword)) {
            showToast("Please fill all fields")
            return false
        }

        if (username.length < 3) {
            showToast("Username must be at least 3 characters")
            etUsername.requestFocus()
            return false
        }

        if (!isValidEmail(email)) {
            showToast("Please enter a valid email address")
            etEmail.requestFocus()
            return false
        }

        if (password.length < 6) {
            showToast("Password must be at least 6 characters")
            etPassword.requestFocus()
            return false
        }

        if (password != confirmPassword) {
            showToast("Passwords do not match")
            etConfirmPassword.requestFocus()
            return false
        }

        // Check if username already exists
        if (databaseHelper.checkUsername(username)) {
            showToast("Username already exists")
            etUsername.requestFocus()
            return false
        }

        // Check if email already exists
        if (databaseHelper.checkEmail(email)) {
            showToast("Email already exists")
            etEmail.requestFocus()
            return false
        }

        return true
    }

    private fun isValidEmail(email: String): Boolean {
        val emailPattern = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$"
        )
        return emailPattern.matcher(email).matches()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish() // Finish RegisterActivity so user goes back to login
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove the callback when the activity is destroyed to prevent memory leaks
        if (::onBackPressedCallback.isInitialized) {
            onBackPressedCallback.remove()
        }
    }
}