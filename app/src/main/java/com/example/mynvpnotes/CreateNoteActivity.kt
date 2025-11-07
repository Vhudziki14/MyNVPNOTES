package com.example.mynvpnotes

import android.Manifest
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.UUID

class CreateNoteActivity : AppCompatActivity() {

    // Views from your XML
    private lateinit var noteTitleEditText: TextInputEditText
    private lateinit var noteContentEditText: TextInputEditText
    private lateinit var saveNoteButton: Button
    private lateinit var clearButton: Button
    private lateinit var cancelButton: Button
    private lateinit var attachFileButton: Button
    private lateinit var imagePreview: ImageView
    private lateinit var reminderSwitch: SwitchMaterial

    // Data handling
    private var selectedImageUri: Uri? = null
    private val reminderCalendar = Calendar.getInstance()
    private var reminderTimeInMillis: Long? = null

    // Firebase Storage instance
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

    // Activity Result Launchers
    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            imagePreview.setImageURI(it)
            imagePreview.visibility = View.VISIBLE
        }
    }

    private val requestNotificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            showDateTimePicker()
        } else {
            Toast.makeText(this, "Reminder permission denied. Switch will be turned off.", Toast.LENGTH_LONG).show()
            reminderSwitch.isChecked = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_note)
        initializeViews()
        setupListeners()
    }

    private fun initializeViews() {
        noteTitleEditText = findViewById(R.id.etNoteTitle)
        noteContentEditText = findViewById(R.id.etNoteContent)
        saveNoteButton = findViewById(R.id.btnSaveNote)
        clearButton = findViewById(R.id.btnClear)
        cancelButton = findViewById(R.id.btnCancel)
        attachFileButton = findViewById(R.id.btnAttachFile)
        imagePreview = findViewById(R.id.note_image_preview)
        reminderSwitch = findViewById(R.id.reminder_switch)
    }

    private fun setupListeners() {
        saveNoteButton.setOnClickListener { saveNote() }
        clearButton.setOnClickListener { clearFields() }
        cancelButton.setOnClickListener { finish() }
        attachFileButton.setOnClickListener { selectImageLauncher.launch("image/*") }

        reminderSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                checkPermissionsAndShowPicker()
            } else {
                cancelReminder()
                reminderTimeInMillis = null
                Toast.makeText(this, "Reminder turned off.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun clearFields() {
        noteTitleEditText.text = null
        noteContentEditText.text = null
        imagePreview.visibility = View.GONE
        selectedImageUri = null
        reminderSwitch.isChecked = false
        Toast.makeText(this, "Fields cleared", Toast.LENGTH_SHORT).show()
    }

    private fun checkPermissionsAndShowPicker() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED -> {
                    showDateTimePicker()
                }
                else -> {
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            showDateTimePicker()
        }
    }

    private fun showDateTimePicker() {
        val currentCalendar = Calendar.getInstance()
        DatePickerDialog(this, { _, year, month, dayOfMonth ->
            reminderCalendar.set(Calendar.YEAR, year)
            reminderCalendar.set(Calendar.MONTH, month)
            reminderCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

            TimePickerDialog(this, { _, hourOfDay, minute ->
                reminderCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                reminderCalendar.set(Calendar.MINUTE, minute)
                reminderCalendar.set(Calendar.SECOND, 0)
                reminderCalendar.set(Calendar.MILLISECOND, 0)

                if (reminderCalendar.timeInMillis <= System.currentTimeMillis()) {
                    Toast.makeText(this, "Cannot set a reminder for a past time.", Toast.LENGTH_LONG).show()
                    reminderSwitch.isChecked = false
                } else {
                    reminderTimeInMillis = reminderCalendar.timeInMillis
                    val formattedDate = SimpleDateFormat("MMM dd, yyyy 'at' hh:mm a", Locale.getDefault()).format(reminderCalendar.time)
                    Toast.makeText(this, "Reminder set for $formattedDate", Toast.LENGTH_LONG).show()
                }
            }, currentCalendar.get(Calendar.HOUR_OF_DAY), currentCalendar.get(Calendar.MINUTE), false).show()
        }, currentCalendar.get(Calendar.YEAR), currentCalendar.get(Calendar.MONTH), currentCalendar.get(Calendar.DAY_OF_MONTH)).apply {
            datePicker.minDate = System.currentTimeMillis() - 1000
        }.show()
    }

    private fun saveNote() {
        val noteTitle = noteTitleEditText.text.toString().trim()
        val noteContent = noteContentEditText.text.toString().trim()

        if (noteTitle.isBlank()) {
            Toast.makeText(this, "Note title cannot be empty.", Toast.LENGTH_SHORT).show()
            return
        }

        saveNoteButton.isEnabled = false
        Toast.makeText(this, "Saving note...", Toast.LENGTH_SHORT).show()

        if (selectedImageUri != null) {
            uploadImageAndSaveNote(noteTitle, noteContent)
        } else {
            saveNoteToDatabase(noteTitle, noteContent, null)
        }
    }

    // *** FULLY REVISED AND CORRECTED IMPLEMENTATION ***
    private fun uploadImageAndSaveNote(noteTitle: String, noteContent: String) {
        val fileName = "images/${UUID.randomUUID()}.jpg"
        val storageRef = storage.reference.child(fileName)

        storageRef.putFile(selectedImageUri!!)
            .addOnSuccessListener {
                // The image upload was successful, now get the download URL.
                storageRef.downloadUrl
                    .addOnSuccessListener { uri ->
                        // Successfully retrieved the download URL.
                        val imageUrl = uri.toString()
                        saveNoteToDatabase(noteTitle, noteContent, imageUrl)
                    }
                    .addOnFailureListener { e ->
                        // Failed to get the download URL. This is where the error likely occurs.
                        val errorMessage = when ((e as? StorageException)?.errorCode) {
                            StorageException.ERROR_OBJECT_NOT_FOUND -> "Object does not exist at location. The upload may have failed silently."
                            StorageException.ERROR_BUCKET_NOT_FOUND -> "Target bucket does not exist."
                            StorageException.ERROR_PROJECT_NOT_FOUND -> "Target project does not exist."
                            StorageException.ERROR_QUOTA_EXCEEDED -> "Storage quota exceeded."
                            StorageException.ERROR_NOT_AUTHENTICATED -> "User is not authenticated. Please check your Firebase Storage rules."
                            StorageException.ERROR_NOT_AUTHORIZED -> "User is not authorized to perform the desired action. Please check your Firebase Storage rules."
                            else -> e.message
                        }
                        Toast.makeText(this, "Failed to get download URL: $errorMessage", Toast.LENGTH_LONG).show()
                        saveNoteButton.isEnabled = true // Re-enable the button on failure.
                    }
            }
            .addOnFailureListener { e ->
                // The image upload failed.
                Toast.makeText(this, "Image upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                saveNoteButton.isEnabled = true // Re-enable the button on failure.
            }
    }

    private fun saveNoteToDatabase(title: String, content: String, imageUrl: String?) {
        if (reminderSwitch.isChecked && reminderTimeInMillis != null) {
            scheduleReminder(reminderTimeInMillis!!, title)
        }

        // TODO: This is where you would call your ViewModel to save everything to Room
        // For example:
        // noteViewModel.saveNote(title, content, imageUrl, reminderTimeInMillis)

        Toast.makeText(this, "Note saved successfully!", Toast.LENGTH_LONG).show()
        finish() // Go back to the notes list
    }

    private fun scheduleReminder(timeInMillis: Long, noteTitle: String) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("NOTE_TITLE", noteTitle)
            putExtra("NOTE_CONTENT", "Don't forget about your note!")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            this,
            System.currentTimeMillis().toInt(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Toast.makeText(this, "App needs permission to schedule exact alarms.", Toast.LENGTH_LONG).show()
            return
        }

        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, timeInMillis, pendingIntent)
    }

    private fun cancelReminder() {
        // Placeholder for reminder cancellation logic
    }
}
