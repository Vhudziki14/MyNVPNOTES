package com.example.mynvpnotes

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var tvWelcome: TextView
    private lateinit var btnLogout: Button
    private lateinit var btnViewNotes: Button
    private lateinit var btnCreateNote: Button
    private lateinit var btnViewTasks: Button
    private lateinit var btnCreateTask: Button
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var databaseHelper: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Handle back button with new OnBackPressedDispatcher
        setupBackPressedHandler()

        // Initialize database helper
        databaseHelper = DatabaseHelper(this)

        // Check if user is logged in
        sharedPreferences = getSharedPreferences(LoginActivity.SHARED_PREF_NAME, MODE_PRIVATE)
        if (!sharedPreferences.getBoolean(LoginActivity.KEY_IS_LOGGED_IN, false)) {
            navigateToLogin()
            return
        }

        initViews()
        setupWelcomeMessage()
        setupClickListeners()
    }

    private fun setupBackPressedHandler() {
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Handle the back button event - show exit confirmation
                showExitConfirmation()
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    private fun showExitConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Exit App")
            .setMessage("Are you sure you want to exit NVP NOTES APP?")
            .setPositiveButton("Exit") { dialog, which ->
                finishAffinity()
            }
            .setNegativeButton("Cancel") { dialog, which ->
                dialog.dismiss()
            }
            .show()
    }

    private fun initViews() {
        tvWelcome = findViewById(R.id.tvWelcome)
        btnLogout = findViewById(R.id.btnLogout)
        btnViewNotes = findViewById(R.id.btnViewNotes)
        btnCreateNote = findViewById(R.id.btnCreateNote)
        btnViewTasks = findViewById(R.id.btnViewTasks)
        btnCreateTask = findViewById(R.id.btnCreateTask)
    }

    private fun setupClickListeners() {
        btnLogout.setOnClickListener {
            logoutUser()
        }

        btnViewNotes.setOnClickListener {
            navigateToNotesList()
        }

        btnCreateNote.setOnClickListener {
            navigateToCreateNote()
        }

        btnViewTasks.setOnClickListener {
            navigateToTasksList()
        }

        btnCreateTask.setOnClickListener {
            navigateToCreateTask()
        }
    }

    private fun setupWelcomeMessage() {
        val username = sharedPreferences.getString(LoginActivity.KEY_USERNAME, "User")
        tvWelcome.text = "Welcome to NVP NOTES APP, $username!"

        // Get user's stats for a personalized message
        val userId = databaseHelper.getUserId(username ?: "")
        if (userId != -1) {
            val notesCount = databaseHelper.getNotesCount(userId)
            val tasksCount = databaseHelper.getTasksCount(userId)
            val completedTasksCount = databaseHelper.getCompletedTasksCount(userId)

            showToast("You have $notesCount notes and $tasksCount tasks ($completedTasksCount completed)")
        }
    }

    private fun navigateToNotesList() {
        val intent = Intent(this, NotesListActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun navigateToCreateNote() {
        val intent = Intent(this, CreateNoteActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun navigateToTasksList() {
        val intent = Intent(this, TasksListActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun navigateToCreateTask() {
        val intent = Intent(this, CreateTaskActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun logoutUser() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { dialog, which ->
                performLogout()
            }
            .setNegativeButton("Cancel") { dialog, which ->
                dialog.dismiss()
            }
            .show()
    }

    private fun performLogout() {
        // Clear shared preferences
        val editor = sharedPreferences.edit()
        editor.clear()
        editor.apply()

        showToast("Logged out successfully")
        navigateToLogin()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        finish() // Finish MainActivity so user can't go back
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        // Refresh welcome message when returning to MainActivity
        setupWelcomeMessage()
    }
}