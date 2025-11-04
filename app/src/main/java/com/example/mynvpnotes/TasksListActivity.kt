package com.example.mynvpnotes

import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import java.text.SimpleDateFormat
import java.util.*

class TasksListActivity : AppCompatActivity() {

    private lateinit var layoutTasksContainer: LinearLayout
    private lateinit var btnAddTask: Button
    private lateinit var btnBack: Button
    private lateinit var tvNoTasks: TextView
    private lateinit var databaseHelper: DatabaseHelper
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tasks_list)

        // Handle back button with new OnBackPressedDispatcher
        setupBackPressedHandler()

        databaseHelper = DatabaseHelper(this)
        userId = databaseHelper.getUserId(getUsername())

        if (userId == -1) {
            showToast("Error: User not found")
            finish()
            return
        }

        initViews()
        loadTasks()
        setupClickListeners()
    }

    private fun setupBackPressedHandler() {
        val onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Handle the back button event
                performBackAction()
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    private fun performBackAction() {
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun initViews() {
        layoutTasksContainer = findViewById(R.id.layoutTasksContainer)
        btnAddTask = findViewById(R.id.btnAddTask)
        btnBack = findViewById(R.id.btnBack)
        tvNoTasks = findViewById(R.id.tvNoTasks)
    }

    private fun setupClickListeners() {
        btnAddTask.setOnClickListener {
            navigateToCreateTask()
        }

        btnBack.setOnClickListener {
            performBackAction()
        }
    }

    private fun loadTasks() {
        layoutTasksContainer.removeAllViews()
        val tasks = databaseHelper.getAllTasks(userId)

        if (tasks.isEmpty()) {
            tvNoTasks.visibility = TextView.VISIBLE
            showEmptyState()
        } else {
            tvNoTasks.visibility = TextView.GONE

            // Add section headers
            val pendingTasks = tasks.filter { !it.completed }
            val completedTasks = tasks.filter { it.completed }

            if (pendingTasks.isNotEmpty()) {
                addSectionHeader("📋 Pending Tasks (${pendingTasks.size})")
                for (task in pendingTasks) {
                    addTaskToView(task)
                }
            }

            if (completedTasks.isNotEmpty()) {
                addSectionHeader("✅ Completed Tasks (${completedTasks.size})")
                for (task in completedTasks) {
                    addTaskToView(task)
                }
            }
        }
    }

    private fun addSectionHeader(title: String) {
        val headerLayout = LinearLayout(this)
        headerLayout.orientation = LinearLayout.HORIZONTAL
        headerLayout.setPadding(16)
        headerLayout.setBackgroundColor(ContextCompat.getColor(this, R.color.primary_light))

        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(0, 20, 0, 10)
        headerLayout.layoutParams = layoutParams

        val tvHeader = TextView(this)
        tvHeader.text = title
        tvHeader.textSize = 16f
        tvHeader.setTypeface(null, Typeface.BOLD)
        tvHeader.setTextColor(Color.WHITE)
        tvHeader.setPadding(16, 8, 16, 8)

        headerLayout.addView(tvHeader)
        layoutTasksContainer.addView(headerLayout)
    }

    private fun showEmptyState() {
        val emptyStateLayout = LinearLayout(this)
        emptyStateLayout.orientation = LinearLayout.VERTICAL
        emptyStateLayout.gravity = android.view.Gravity.CENTER
        emptyStateLayout.setPadding(48)

        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(0, 100, 0, 0)
        emptyStateLayout.layoutParams = layoutParams

        val emojiTextView = TextView(this)
        emojiTextView.text = "✅"
        emojiTextView.textSize = 64f
        emojiTextView.gravity = android.view.Gravity.CENTER

        val messageTextView = TextView(this)
        messageTextView.text = "No Tasks Yet"
        messageTextView.textSize = 20f
        messageTextView.setTypeface(null, Typeface.BOLD)
        messageTextView.setTextColor(Color.BLACK)
        messageTextView.gravity = android.view.Gravity.CENTER
        messageTextView.setPadding(0, 24, 0, 16)

        val subtitleTextView = TextView(this)
        subtitleTextView.text = "Tap the + button to create your first task and stay organized!"
        subtitleTextView.textSize = 14f
        subtitleTextView.setTextColor(Color.parseColor("#666666"))
        subtitleTextView.gravity = android.view.Gravity.CENTER
        subtitleTextView.setPadding(0, 0, 0, 32)
        subtitleTextView.setLineSpacing(1.2f, 1.2f)

        val createFirstTaskButton = Button(this)
        createFirstTaskButton.text = "Create Your First Task"
        createFirstTaskButton.textSize = 16f
        createFirstTaskButton.setTypeface(null, Typeface.BOLD)
        createFirstTaskButton.setBackgroundResource(R.drawable.button_primary)
        createFirstTaskButton.setTextColor(Color.WHITE)
        createFirstTaskButton.elevation = 4f
        createFirstTaskButton.setPadding(32, 16, 32, 16)

        createFirstTaskButton.setOnClickListener {
            navigateToCreateTask()
        }

        emptyStateLayout.addView(emojiTextView)
        emptyStateLayout.addView(messageTextView)
        emptyStateLayout.addView(subtitleTextView)
        emptyStateLayout.addView(createFirstTaskButton)

        layoutTasksContainer.addView(emptyStateLayout)
    }

    private fun addTaskToView(task: Task) {
        val taskCard = LinearLayout(this)
        taskCard.orientation = LinearLayout.VERTICAL
        taskCard.setPadding(20)
        taskCard.elevation = 6f
        taskCard.translationZ = 6f

        // Different background for completed tasks
        if (task.completed) {
            taskCard.setBackgroundColor(ContextCompat.getColor(this, R.color.light_gray))
        } else {
            taskCard.setBackgroundColor(Color.WHITE)
        }

        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(0, 0, 0, 12)
        taskCard.layoutParams = layoutParams

        // Title section with checkbox
        val titleLayout = LinearLayout(this)
        titleLayout.orientation = LinearLayout.HORIZONTAL
        titleLayout.gravity = android.view.Gravity.CENTER_VERTICAL

        val checkbox = TextView(this)
        checkbox.text = if (task.completed) "☑️" else "⏳"
        checkbox.textSize = 16f
        checkbox.setPadding(0, 0, 12, 0)

        val tvTitle = TextView(this)
        tvTitle.text = task.title
        tvTitle.textSize = 18f
        tvTitle.setTypeface(null, Typeface.BOLD)
        tvTitle.setTextColor(if (task.completed) Color.parseColor("#888888") else Color.BLACK)
        if (task.completed) {
            tvTitle.paintFlags = tvTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        }
        tvTitle.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        // Priority indicator
        val priorityView = TextView(this)
        priorityView.text = when (task.priority) {
            3 -> "🔥" // High
            2 -> "⚠️" // Medium
            else -> "💤" // Low
        }
        priorityView.textSize = 14f
        priorityView.setPadding(8, 0, 0, 0)

        titleLayout.addView(checkbox)
        titleLayout.addView(tvTitle)
        titleLayout.addView(priorityView)

        // Description
        if (task.description.isNotEmpty()) {
            val tvDescription = TextView(this)
            tvDescription.text = task.description
            tvDescription.textSize = 14f
            tvDescription.setTextColor(Color.parseColor("#444444"))
            tvDescription.setPadding(0, 8, 0, 8)
            tvDescription.setLineSpacing(1.3f, 1.3f)
            if (task.completed) {
                tvDescription.paintFlags = tvDescription.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            }
            taskCard.addView(tvDescription)
        }

        // Footer with due date and actions
        val footerLayout = LinearLayout(this)
        footerLayout.orientation = LinearLayout.HORIZONTAL
        footerLayout.gravity = android.view.Gravity.CENTER_VERTICAL

        val tvDueDate = TextView(this)
        tvDueDate.text = "📅 ${formatDate(task.dueDate)}"
        tvDueDate.textSize = 12f
        tvDueDate.setTextColor(Color.parseColor("#666666"))
        tvDueDate.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        val actionButton = TextView(this)
        actionButton.text = "✏️ Edit"
        actionButton.textSize = 12f
        actionButton.setTextColor(ContextCompat.getColor(this, R.color.primary_color))
        actionButton.setTypeface(null, Typeface.BOLD)
        actionButton.setPadding(16, 8, 16, 8)
        actionButton.background = ContextCompat.getDrawable(this, R.drawable.button_secondary)

        footerLayout.addView(tvDueDate)
        footerLayout.addView(actionButton)

        taskCard.addView(titleLayout)
        taskCard.addView(footerLayout)

        // Add click listener to toggle completion
        taskCard.setOnClickListener {
            toggleTaskCompletion(task)
        }

        // Add click listener to edit task
        actionButton.setOnClickListener {
            navigateToEditTask(task.id)
        }

        // Add long press for delete option
        taskCard.setOnLongClickListener {
            showDeleteDialog(task)
            true
        }

        layoutTasksContainer.addView(taskCard)
    }

    private fun toggleTaskCompletion(task: Task) {
        val newCompletedState = !task.completed
        if (databaseHelper.toggleTaskCompletion(task.id, newCompletedState)) {
            showToast(if (newCompletedState) "Task completed! 🎉" else "Task marked as pending")
            loadTasks() // Refresh the list
        } else {
            showToast("Failed to update task")
        }
    }

    private fun formatDate(dateString: String): String {
        if (dateString.isEmpty()) return "No due date"

        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            dateString
        }
    }

    private fun showDeleteDialog(task: Task) {
        AlertDialog.Builder(this)
            .setTitle("Delete Task")
            .setMessage("Are you sure you want to delete '${task.title}'? This action cannot be undone.")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("Delete") { dialog, which ->
                if (databaseHelper.deleteTask(task.id)) {
                    showToast("🗑️ Task deleted successfully!")
                    loadTasks() // Refresh the list
                } else {
                    showToast("❌ Failed to delete task")
                }
            }
            .setNegativeButton("Cancel") { dialog, which ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .create()
            .show()
    }

    private fun navigateToCreateTask() {
        val intent = Intent(this, CreateTaskActivity::class.java)
        startActivityForResult(intent, 1)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun navigateToEditTask(taskId: Int) {
        val intent = Intent(this, CreateTaskActivity::class.java)
        intent.putExtra("TASK_ID", taskId)
        startActivityForResult(intent, 1)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun getUsername(): String {
        val sharedPreferences = getSharedPreferences(LoginActivity.SHARED_PREF_NAME, MODE_PRIVATE)
        return sharedPreferences.getString(LoginActivity.KEY_USERNAME, "") ?: ""
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onResume() {
        super.onResume()
        loadTasks() // Refresh tasks when returning from create/edit
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            // Refresh the tasks list when returning from CreateTaskActivity
            loadTasks()
        }
    }
}