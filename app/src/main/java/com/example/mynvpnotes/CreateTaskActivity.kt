package com.example.mynvpnotes

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class CreateTaskActivity : AppCompatActivity() {

    private lateinit var etTaskTitle: EditText
    private lateinit var etTaskDescription: EditText
    private lateinit var etDueDate: EditText
    private lateinit var spinnerPriority: Spinner
    private lateinit var btnSaveTask: Button
    private lateinit var btnCancel: Button
    private lateinit var btnDelete: Button
    private lateinit var databaseHelper: DatabaseHelper

    private var userId: Int = -1
    private var taskId: Int = -1
    private var isEditMode: Boolean = false
    private var hasUnsavedChanges: Boolean = false
    private val calendar = Calendar.getInstance()
    private lateinit var onBackPressedCallback: OnBackPressedCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_task)

        // Initialize database helper
        databaseHelper = DatabaseHelper(this)
        userId = databaseHelper.getUserId(getUsername())

        if (userId == -1) {
            showToast("Error: User not found")
            finish()
            return
        }

        initViews()
        setupPrioritySpinner()
        setupDueDatePicker()
        checkEditMode()
        setupClickListeners()
        setupTextChangeListeners()
        setupBackPressedHandler()
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
            finishWithTransition()
        }
    }

    private fun showUnsavedChangesDialog() {
        AlertDialog.Builder(this)
            .setTitle("Unsaved Changes")
            .setMessage("You have unsaved changes. Do you want to save them before exiting?")
            .setPositiveButton("Save") { dialog, which ->
                saveTaskAndExit()
            }
            .setNegativeButton("Discard") { dialog, which ->
                finishWithTransition()
            }
            .setNeutralButton("Cancel") { dialog, which ->
                dialog.dismiss()
            }
            .setCancelable(false)
            .show()
    }

    private fun saveTaskAndExit() {
        if (saveTask()) {
            finishWithTransition()
        }
    }

    private fun finishWithTransition() {
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun initViews() {
        etTaskTitle = findViewById(R.id.etTaskTitle)
        etTaskDescription = findViewById(R.id.etTaskDescription)
        etDueDate = findViewById(R.id.etDueDate)
        spinnerPriority = findViewById(R.id.spinnerPriority)
        btnSaveTask = findViewById(R.id.btnSaveTask)
        btnCancel = findViewById(R.id.btnCancel)
        btnDelete = findViewById(R.id.btnDelete)

        // Set text colors to ensure visibility
        etTaskTitle.setTextColor(Color.BLACK)
        etTaskDescription.setTextColor(Color.BLACK)
        etDueDate.setTextColor(Color.BLACK)

        // Set hint colors
        etTaskTitle.setHintTextColor(Color.parseColor("#888888"))
        etTaskDescription.setHintTextColor(Color.parseColor("#888888"))
        etDueDate.setHintTextColor(Color.parseColor("#888888"))
    }

    private fun setupPrioritySpinner() {
        val priorities = arrayOf("Low Priority", "Medium Priority", "High Priority")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, priorities)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPriority.adapter = adapter
        spinnerPriority.setSelection(1) // Default to Medium

        // Track spinner changes for unsaved changes
        spinnerPriority.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                if (!hasUnsavedChanges) {
                    hasUnsavedChanges = true
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupDueDatePicker() {
        etDueDate.setOnClickListener {
            showDatePickerDialog()
        }

        etDueDate.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                showDatePickerDialog()
            }
        }
    }

    private fun showDatePickerDialog() {
        val datePicker = DatePickerDialog(
            this,
            { _, year, month, day ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, day)
                updateDueDateText()
                if (!hasUnsavedChanges) {
                    hasUnsavedChanges = true
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Set minimum date to today
        datePicker.datePicker.minDate = System.currentTimeMillis() - 1000
        datePicker.show()
    }

    private fun updateDueDateText() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        etDueDate.setText(dateFormat.format(calendar.time))
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

        etTaskTitle.addTextChangedListener(textWatcher)
        etTaskDescription.addTextChangedListener(textWatcher)
        etDueDate.addTextChangedListener(textWatcher)
    }

    private fun checkEditMode() {
        taskId = intent.getIntExtra("TASK_ID", -1)
        if (taskId != -1) {
            isEditMode = true
            btnSaveTask.text = "Update Task"
            btnDelete.visibility = Button.VISIBLE
            loadTaskData()
        } else {
            isEditMode = false
            btnSaveTask.text = "Save Task"
            btnDelete.visibility = Button.GONE
        }
    }

    private fun loadTaskData() {
        try {
            val task = databaseHelper.getTaskById(taskId)
            if (task != null) {
                etTaskTitle.setText(task.title)
                etTaskDescription.setText(task.description)

                if (task.dueDate.isNotEmpty()) {
                    etDueDate.setText(task.dueDate)
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    val date = dateFormat.parse(task.dueDate)
                    date?.let {
                        calendar.time = it
                    }
                }

                spinnerPriority.setSelection(task.priority - 1) // Priority is 1-based

                // Reset unsaved changes flag after loading data
                hasUnsavedChanges = false
            } else {
                showToast("Task not found")
                finish()
            }
        } catch (e: Exception) {
            showToast("Error loading task")
            finish()
        }
    }

    private fun setupClickListeners() {
        btnSaveTask.setOnClickListener {
            saveTask()
        }

        btnCancel.setOnClickListener {
            handleBackAction()
        }

        btnDelete.setOnClickListener {
            showDeleteConfirmation()
        }
    }

    private fun saveTask(): Boolean {
        val title = etTaskTitle.text.toString().trim()
        val description = etTaskDescription.text.toString().trim()
        val dueDate = etDueDate.text.toString().trim()
        val priority = spinnerPriority.selectedItemPosition + 1 // Convert to 1-based

        if (TextUtils.isEmpty(title)) {
            showToast("Please enter a task title")
            etTaskTitle.requestFocus()
            return false
        }

        var success = false
        try {
            success = if (isEditMode) {
                val task = databaseHelper.getTaskById(taskId)
                if (task != null) {
                    databaseHelper.updateTask(taskId, title, description, dueDate, priority, task.completed)
                } else {
                    false
                }
            } else {
                databaseHelper.addTask(title, description, dueDate, priority, userId)
            }
        } catch (e: Exception) {
            showToast("Error saving task: ${e.message}")
            return false
        }

        if (success) {
            val message = if (isEditMode) "Task updated successfully!" else "Task saved successfully!"
            showToast(message)
            hasUnsavedChanges = false
            setResult(RESULT_OK)
            return true
        } else {
            showToast("Failed to save task. Please try again.")
            return false
        }
    }

    private fun showDeleteConfirmation() {
        if (!isEditMode) return

        AlertDialog.Builder(this)
            .setTitle("Delete Task")
            .setMessage("Are you sure you want to delete this task? This action cannot be undone.")
            .setPositiveButton("Delete") { dialog, which ->
                deleteTask()
            }
            .setNegativeButton("Cancel") { dialog, which ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }

    private fun deleteTask() {
        try {
            if (databaseHelper.deleteTask(taskId)) {
                showToast("Task deleted successfully!")
                setResult(RESULT_OK)
                finishWithTransition()
            } else {
                showToast("Failed to delete task")
            }
        } catch (e: Exception) {
            showToast("Error deleting task: ${e.message}")
        }
    }

    private fun getUsername(): String {
        val sharedPreferences = getSharedPreferences(LoginActivity.SHARED_PREF_NAME, MODE_PRIVATE)
        return sharedPreferences.getString(LoginActivity.KEY_USERNAME, "") ?: ""
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