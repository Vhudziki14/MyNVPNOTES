package com.example.mynvpnotes

import android.content.Intent
import android.graphics.Color
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

class NotesListActivity : AppCompatActivity() {

    private lateinit var layoutNotesContainer: LinearLayout
    private lateinit var btnAddNote: Button
    private lateinit var btnBack: Button
    private lateinit var tvNoNotes: TextView
    private lateinit var databaseHelper: DatabaseHelper
    private var userId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notes_list)

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
        loadNotes()
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
        layoutNotesContainer = findViewById(R.id.layoutNotesContainer)
        btnAddNote = findViewById(R.id.btnAddNote)
        btnBack = findViewById(R.id.btnBack)
        tvNoNotes = findViewById(R.id.tvNoNotes)
    }

    private fun setupClickListeners() {
        btnAddNote.setOnClickListener {
            navigateToCreateNote()
        }

        btnBack.setOnClickListener {
            performBackAction()
        }
    }

    private fun loadNotes() {
        layoutNotesContainer.removeAllViews()
        val notes = databaseHelper.getAllNotes(userId)

        if (notes.isEmpty()) {
            tvNoNotes.visibility = TextView.VISIBLE
            showEmptyState()
        } else {
            tvNoNotes.visibility = TextView.GONE
            for (note in notes) {
                addNoteToView(note)
            }
        }
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
        emojiTextView.text = "📝"
        emojiTextView.textSize = 64f
        emojiTextView.gravity = android.view.Gravity.CENTER

        val messageTextView = TextView(this)
        messageTextView.text = "No Notes Yet"
        messageTextView.textSize = 20f
        messageTextView.setTypeface(null, Typeface.BOLD)
        messageTextView.setTextColor(Color.BLACK)
        messageTextView.gravity = android.view.Gravity.CENTER
        messageTextView.setPadding(0, 24, 0, 16)

        val subtitleTextView = TextView(this)
        subtitleTextView.text = "Tap the + button to create your first note and start organizing your thoughts!"
        subtitleTextView.textSize = 14f
        subtitleTextView.setTextColor(Color.parseColor("#666666"))
        subtitleTextView.gravity = android.view.Gravity.CENTER
        subtitleTextView.setPadding(0, 0, 0, 32)
        subtitleTextView.setLineSpacing(1.2f, 1.2f)

        val createFirstNoteButton = Button(this)
        createFirstNoteButton.text = "Create Your First Note"
        createFirstNoteButton.textSize = 16f
        createFirstNoteButton.setTypeface(null, Typeface.BOLD)
        createFirstNoteButton.setBackgroundResource(R.drawable.button_primary)
        createFirstNoteButton.setTextColor(Color.WHITE)
        createFirstNoteButton.elevation = 4f
        createFirstNoteButton.setPadding(32, 16, 32, 16)

        createFirstNoteButton.setOnClickListener {
            navigateToCreateNote()
        }

        emptyStateLayout.addView(emojiTextView)
        emptyStateLayout.addView(messageTextView)
        emptyStateLayout.addView(subtitleTextView)
        emptyStateLayout.addView(createFirstNoteButton)

        layoutNotesContainer.addView(emptyStateLayout)
    }

    private fun addNoteToView(note: Note) {
        val noteCard = LinearLayout(this)
        noteCard.orientation = LinearLayout.VERTICAL
        noteCard.setPadding(24)
        noteCard.elevation = 8f
        noteCard.translationZ = 8f

        // Random card colors for visual appeal
        val cardColors = listOf(
            R.color.note_card_1,
            R.color.note_card_2,
            R.color.note_card_3,
            R.color.note_card_4,
            R.color.note_card_5
        )
        val randomColor = cardColors[note.id % cardColors.size]
        noteCard.setBackgroundColor(ContextCompat.getColor(this, randomColor))

        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        layoutParams.setMargins(0, 0, 0, 20)
        noteCard.layoutParams = layoutParams

        // Title section with icon
        val titleLayout = LinearLayout(this)
        titleLayout.orientation = LinearLayout.HORIZONTAL
        titleLayout.gravity = android.view.Gravity.CENTER_VERTICAL

        val titleIcon = TextView(this)
        titleIcon.text = "📌"
        titleIcon.textSize = 16f
        titleIcon.setPadding(0, 0, 12, 0)

        val tvTitle = TextView(this)
        tvTitle.text = note.title
        tvTitle.textSize = 18f
        tvTitle.setTypeface(null, Typeface.BOLD)
        tvTitle.setTextColor(Color.BLACK)
        tvTitle.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )

        titleLayout.addView(titleIcon)
        titleLayout.addView(tvTitle)

        // Content preview
        val tvContent = TextView(this)
        val contentPreview = if (note.content.length > 150)
            "${note.content.substring(0, 150)}..." else note.content
        tvContent.text = contentPreview
        tvContent.textSize = 14f
        tvContent.setTextColor(Color.parseColor("#444444"))
        tvContent.setPadding(0, 12, 0, 16)
        tvContent.setLineSpacing(1.4f, 1.4f)

        // Footer with date and actions
        val footerLayout = LinearLayout(this)
        footerLayout.orientation = LinearLayout.HORIZONTAL
        footerLayout.gravity = android.view.Gravity.CENTER_VERTICAL

        val tvDate = TextView(this)
        tvDate.text = "🕒 ${formatDate(note.date)}"
        tvDate.textSize = 12f
        tvDate.setTextColor(Color.parseColor("#666666"))
        tvDate.layoutParams = LinearLayout.LayoutParams(
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

        footerLayout.addView(tvDate)
        footerLayout.addView(actionButton)

        noteCard.addView(titleLayout)
        noteCard.addView(tvContent)
        noteCard.addView(footerLayout)

        // Add click listener to edit note
        noteCard.setOnClickListener {
            navigateToEditNote(note.id)
        }

        actionButton.setOnClickListener {
            navigateToEditNote(note.id)
        }

        // Add long press for delete option
        noteCard.setOnLongClickListener {
            showDeleteDialog(note)
            true
        }

        layoutNotesContainer.addView(noteCard)
    }

    private fun formatDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            dateString
        }
    }

    private fun showDeleteDialog(note: Note) {
        AlertDialog.Builder(this)
            .setTitle("Delete Note")
            .setMessage("Are you sure you want to delete '${note.title}'? This action cannot be undone.")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("Delete") { dialog, which ->
                if (databaseHelper.deleteNote(note.id)) {
                    showToast("🗑️ Note deleted successfully!")
                    loadNotes() // Refresh the list
                } else {
                    showToast("❌ Failed to delete note")
                }
            }
            .setNegativeButton("Cancel") { dialog, which ->
                dialog.dismiss()
            }
            .setCancelable(true)
            .create()
            .show()
    }

    private fun navigateToCreateNote() {
        val intent = Intent(this, CreateNoteActivity::class.java)
        startActivityForResult(intent, 1)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    private fun navigateToEditNote(noteId: Int) {
        val intent = Intent(this, CreateNoteActivity::class.java)
        intent.putExtra("NOTE_ID", noteId)
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
        loadNotes() // Refresh notes when returning from create/edit
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            // Refresh the notes list when returning from CreateNoteActivity
            loadNotes()
        }
    }
}