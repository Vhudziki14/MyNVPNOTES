package com.example.mynvpnotes

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class CreateNoteActivity : AppCompatActivity() {

    private lateinit var etNoteTitle: EditText
    private lateinit var etNoteContent: EditText
    private lateinit var btnSaveNote: Button
    private lateinit var btnCancel: Button
    private lateinit var btnDelete: Button
    private lateinit var btnClear: Button
    private lateinit var databaseHelper: DatabaseHelper

    private var userId: Int = -1
    private var noteId: Int = -1
    private var isEditMode: Boolean = false
    private var hasUnsavedChanges: Boolean = false
    private lateinit var onBackPressedCallback: OnBackPressedCallback

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_note)

        // Initialize database helper
        databaseHelper = DatabaseHelper(this)
        userId = databaseHelper.getUserId(getUsername())

        if (userId == -1) {
            showToast("Error: User not found")
            finish()
            return
        }

        initViews()
        checkEditMode()
        setupClickListeners()
        setupTextChangeListeners()
        setupBackPressedHandler()
    }

    private fun setupBackPressedHandler() {
        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                handleBackAction()
            }
        }
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
                saveNoteAndExit()
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

    private fun saveNoteAndExit() {
        if (saveNote()) {
            finishWithTransition()
        }
    }

    private fun finishWithTransition() {
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun initViews() {
        etNoteTitle = findViewById(R.id.etNoteTitle)
        etNoteContent = findViewById(R.id.etNoteContent)
        btnSaveNote = findViewById(R.id.btnSaveNote)
        btnCancel = findViewById(R.id.btnCancel)
        btnDelete = findViewById(R.id.btnDelete)
        btnClear = findViewById(R.id.btnClear)

        // Set text colors to ensure visibility
        etNoteTitle.setTextColor(Color.BLACK)
        etNoteContent.setTextColor(Color.BLACK)

        // Set hint colors
        etNoteTitle.setHintTextColor(Color.parseColor("#888888"))
        etNoteContent.setHintTextColor(Color.parseColor("#888888"))
    }

    private fun setupTextChangeListeners() {
        val textWatcher = object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                if (!hasUnsavedChanges) {
                    hasUnsavedChanges = true
                }
                // Update clear button visibility
                updateClearButtonVisibility()
            }
        }

        etNoteTitle.addTextChangedListener(textWatcher)
        etNoteContent.addTextChangedListener(textWatcher)
    }

    private fun updateClearButtonVisibility() {
        val hasText = etNoteTitle.text.isNotEmpty() || etNoteContent.text.isNotEmpty()
        btnClear.isEnabled = hasText
    }

    private fun checkEditMode() {
        noteId = intent.getIntExtra("NOTE_ID", -1)
        if (noteId != -1) {
            isEditMode = true
            btnSaveNote.text = "Update Note"
            btnDelete.visibility = Button.VISIBLE
            loadNoteData()
        } else {
            isEditMode = false
            btnSaveNote.text = "Save Note"
            btnDelete.visibility = Button.GONE
        }
        updateClearButtonVisibility()
    }

    private fun loadNoteData() {
        try {
            val note = databaseHelper.getNoteById(noteId)
            if (note != null) {
                etNoteTitle.setText(note.title)
                etNoteContent.setText(note.content)
                hasUnsavedChanges = false
                updateClearButtonVisibility()
            } else {
                showToast("Note not found")
                finish()
            }
        } catch (e: Exception) {
            showToast("Error loading note")
            finish()
        }
    }

    private fun setupClickListeners() {
        btnSaveNote.setOnClickListener {
            saveNote()
        }

        btnCancel.setOnClickListener {
            handleBackAction()
        }

        btnDelete.setOnClickListener {
            showDeleteConfirmation()
        }

        btnClear.setOnClickListener {
            showClearConfirmation()
        }
    }

    private fun showClearConfirmation() {
        if (etNoteTitle.text.isEmpty() && etNoteContent.text.isEmpty()) {
            showToast("Nothing to clear")
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Clear All")
            .setMessage("Are you sure you want to clear all text? This action cannot be undone.")
            .setPositiveButton("Clear All") { dialog, which ->
                clearAllText()
            }
            .setNegativeButton("Cancel") { dialog, which ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }

    private fun clearAllText() {
        etNoteTitle.text.clear()
        etNoteContent.text.clear()
        etNoteTitle.requestFocus()
        hasUnsavedChanges = false
        updateClearButtonVisibility()
        showToast("All text cleared")
    }

    private fun saveNote(): Boolean {
        val title = etNoteTitle.text.toString().trim()
        val content = etNoteContent.text.toString().trim()

        if (TextUtils.isEmpty(title)) {
            showToast("Please enter a title")
            etNoteTitle.requestFocus()
            return false
        }

        if (TextUtils.isEmpty(content)) {
            showToast("Please enter note content")
            etNoteContent.requestFocus()
            return false
        }

        var success = false
        try {
            success = if (isEditMode) {
                databaseHelper.updateNote(noteId, title, content)
            } else {
                databaseHelper.addNote(title, content, userId)
            }
        } catch (e: Exception) {
            showToast("Error saving note: ${e.message}")
            return false
        }

        if (success) {
            val message = if (isEditMode) "Note updated successfully!" else "Note saved successfully!"
            showToast(message)
            hasUnsavedChanges = false
            setResult(RESULT_OK)
            return true
        } else {
            showToast("Failed to save note. Please try again.")
            return false
        }
    }

    private fun showDeleteConfirmation() {
        if (!isEditMode) return

        AlertDialog.Builder(this)
            .setTitle("Delete Note")
            .setMessage("Are you sure you want to delete this note? This action cannot be undone.")
            .setPositiveButton("Delete") { dialog, which ->
                deleteNote()
            }
            .setNegativeButton("Cancel") { dialog, which ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .show()
    }

    private fun deleteNote() {
        try {
            if (databaseHelper.deleteNote(noteId)) {
                showToast("Note deleted successfully!")
                setResult(RESULT_OK)
                finishWithTransition()
            } else {
                showToast("Failed to delete note")
            }
        } catch (e: Exception) {
            showToast("Error deleting note: ${e.message}")
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
        if (::onBackPressedCallback.isInitialized) {
            onBackPressedCallback.remove()
        }
    }
}