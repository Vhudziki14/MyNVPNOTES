package com.example.mynvpnotes

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.text.SimpleDateFormat
import java.util.*

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "NVPNotesApp.db"
        private const val DATABASE_VERSION = 3

        // Users table
        private const val TABLE_USERS = "users"
        private const val COLUMN_USER_ID = "user_id"
        private const val COLUMN_USERNAME = "username"
        private const val COLUMN_EMAIL = "email"
        private const val COLUMN_PASSWORD = "password"

        // Notes table
        private const val TABLE_NOTES = "notes"
        private const val COLUMN_NOTE_ID = "note_id"
        private const val COLUMN_NOTE_TITLE = "title"
        private const val COLUMN_NOTE_CONTENT = "content"
        private const val COLUMN_NOTE_DATE = "created_date"
        private const val COLUMN_USER_FK = "user_id"

        // Tasks table (new)
        private const val TABLE_TASKS = "tasks"
        private const val COLUMN_TASK_ID = "task_id"
        private const val COLUMN_TASK_TITLE = "title"
        private const val COLUMN_TASK_DESCRIPTION = "description"
        private const val COLUMN_TASK_DUE_DATE = "due_date"
        private const val COLUMN_TASK_PRIORITY = "priority"
        private const val COLUMN_TASK_COMPLETED = "completed"
        private const val COLUMN_TASK_CREATED_DATE = "created_date"
        private const val COLUMN_TASK_USER_FK = "user_id"
    }

    override fun onCreate(db: SQLiteDatabase) {
        // Create users table
        val createUsersTable = """
            CREATE TABLE $TABLE_USERS (
                $COLUMN_USER_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_USERNAME TEXT UNIQUE NOT NULL,
                $COLUMN_EMAIL TEXT UNIQUE NOT NULL,
                $COLUMN_PASSWORD TEXT NOT NULL
            )
        """.trimIndent()

        // Create notes table
        val createNotesTable = """
            CREATE TABLE $TABLE_NOTES (
                $COLUMN_NOTE_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NOTE_TITLE TEXT NOT NULL,
                $COLUMN_NOTE_CONTENT TEXT NOT NULL,
                $COLUMN_NOTE_DATE TEXT NOT NULL,
                $COLUMN_USER_FK INTEGER NOT NULL,
                FOREIGN KEY ($COLUMN_USER_FK) REFERENCES $TABLE_USERS($COLUMN_USER_ID) ON DELETE CASCADE
            )
        """.trimIndent()

        // Create tasks table (new)
        val createTasksTable = """
            CREATE TABLE $TABLE_TASKS (
                $COLUMN_TASK_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_TASK_TITLE TEXT NOT NULL,
                $COLUMN_TASK_DESCRIPTION TEXT,
                $COLUMN_TASK_DUE_DATE TEXT,
                $COLUMN_TASK_PRIORITY INTEGER DEFAULT 2,
                $COLUMN_TASK_COMPLETED INTEGER DEFAULT 0,
                $COLUMN_TASK_CREATED_DATE TEXT NOT NULL,
                $COLUMN_TASK_USER_FK INTEGER NOT NULL,
                FOREIGN KEY ($COLUMN_TASK_USER_FK) REFERENCES $TABLE_USERS($COLUMN_USER_ID) ON DELETE CASCADE
            )
        """.trimIndent()

        db.execSQL(createUsersTable)
        db.execSQL(createNotesTable)
        db.execSQL(createTasksTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 3) {
            db.execSQL("DROP TABLE IF EXISTS $TABLE_TASKS")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_NOTES")
            db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
            onCreate(db)
        }
    }

    // User methods (existing)
    fun registerUser(username: String, email: String, password: String): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(COLUMN_USERNAME, username)
            put(COLUMN_EMAIL, email)
            put(COLUMN_PASSWORD, password)
        }

        return try {
            val result = db.insert(TABLE_USERS, null, contentValues)
            db.close()
            result != -1L
        } catch (e: Exception) {
            db.close()
            false
        }
    }

    fun checkUser(username: String, password: String): Boolean {
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_USERS WHERE $COLUMN_USERNAME = ? AND $COLUMN_PASSWORD = ?"
        val cursor = db.rawQuery(query, arrayOf(username, password))
        val exists = cursor.count > 0
        cursor.close()
        db.close()
        return exists
    }

    fun checkUsername(username: String): Boolean {
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_USERS WHERE $COLUMN_USERNAME = ?"
        val cursor = db.rawQuery(query, arrayOf(username))
        val exists = cursor.count > 0
        cursor.close()
        db.close()
        return exists
    }

    fun checkEmail(email: String): Boolean {
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_USERS WHERE $COLUMN_EMAIL = ?"
        val cursor = db.rawQuery(query, arrayOf(email))
        val exists = cursor.count > 0
        cursor.close()
        db.close()
        return exists
    }

    fun getUserId(username: String): Int {
        val db = this.readableDatabase
        val query = "SELECT $COLUMN_USER_ID FROM $TABLE_USERS WHERE $COLUMN_USERNAME = ?"
        val cursor = db.rawQuery(query, arrayOf(username))
        var userId = -1

        if (cursor.moveToFirst()) {
            val columnIndex = cursor.getColumnIndex(COLUMN_USER_ID)
            if (columnIndex != -1) {
                userId = cursor.getInt(columnIndex)
            }
        }
        cursor.close()
        db.close()
        return userId
    }

    // Note methods (existing)
    fun addNote(title: String, content: String, userId: Int): Boolean {
        val db = this.writableDatabase
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val contentValues = ContentValues().apply {
            put(COLUMN_NOTE_TITLE, title)
            put(COLUMN_NOTE_CONTENT, content)
            put(COLUMN_NOTE_DATE, date)
            put(COLUMN_USER_FK, userId)
        }

        return try {
            val result = db.insert(TABLE_NOTES, null, contentValues)
            db.close()
            result != -1L
        } catch (e: Exception) {
            db.close()
            false
        }
    }

    fun updateNote(noteId: Int, title: String, content: String): Boolean {
        val db = this.writableDatabase
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val contentValues = ContentValues().apply {
            put(COLUMN_NOTE_TITLE, title)
            put(COLUMN_NOTE_CONTENT, content)
            put(COLUMN_NOTE_DATE, date)
        }

        return try {
            val result = db.update(TABLE_NOTES, contentValues, "$COLUMN_NOTE_ID = ?", arrayOf(noteId.toString()))
            db.close()
            result > 0
        } catch (e: Exception) {
            db.close()
            false
        }
    }

    fun deleteNote(noteId: Int): Boolean {
        val db = this.writableDatabase
        return try {
            val result = db.delete(TABLE_NOTES, "$COLUMN_NOTE_ID = ?", arrayOf(noteId.toString()))
            db.close()
            result > 0
        } catch (e: Exception) {
            db.close()
            false
        }
    }

    fun getAllNotes(userId: Int): List<Note> {
        val notes = mutableListOf<Note>()
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_NOTES WHERE $COLUMN_USER_FK = ? ORDER BY $COLUMN_NOTE_DATE DESC"
        val cursor = db.rawQuery(query, arrayOf(userId.toString()))

        try {
            if (cursor.moveToFirst()) {
                do {
                    val noteIdIndex = cursor.getColumnIndex(COLUMN_NOTE_ID)
                    val titleIndex = cursor.getColumnIndex(COLUMN_NOTE_TITLE)
                    val contentIndex = cursor.getColumnIndex(COLUMN_NOTE_CONTENT)
                    val dateIndex = cursor.getColumnIndex(COLUMN_NOTE_DATE)

                    if (noteIdIndex != -1 && titleIndex != -1 && contentIndex != -1 && dateIndex != -1) {
                        val noteId = cursor.getInt(noteIdIndex)
                        val title = cursor.getString(titleIndex)
                        val content = cursor.getString(contentIndex)
                        val date = cursor.getString(dateIndex)

                        notes.add(Note(noteId, title, content, date))
                    }
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            // Handle error
        } finally {
            cursor.close()
            db.close()
        }
        return notes
    }

    fun getNoteById(noteId: Int): Note? {
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_NOTES WHERE $COLUMN_NOTE_ID = ?"
        val cursor = db.rawQuery(query, arrayOf(noteId.toString()))

        var note: Note? = null
        try {
            if (cursor.moveToFirst()) {
                val noteIdIndex = cursor.getColumnIndex(COLUMN_NOTE_ID)
                val titleIndex = cursor.getColumnIndex(COLUMN_NOTE_TITLE)
                val contentIndex = cursor.getColumnIndex(COLUMN_NOTE_CONTENT)
                val dateIndex = cursor.getColumnIndex(COLUMN_NOTE_DATE)

                if (noteIdIndex != -1 && titleIndex != -1 && contentIndex != -1 && dateIndex != -1) {
                    val id = cursor.getInt(noteIdIndex)
                    val title = cursor.getString(titleIndex)
                    val content = cursor.getString(contentIndex)
                    val date = cursor.getString(dateIndex)

                    note = Note(id, title, content, date)
                }
            }
        } catch (e: Exception) {
            // Handle error
        } finally {
            cursor.close()
            db.close()
        }
        return note
    }

    // Task methods (new)
    fun addTask(title: String, description: String, dueDate: String, priority: Int, userId: Int): Boolean {
        val db = this.writableDatabase
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())

        val contentValues = ContentValues().apply {
            put(COLUMN_TASK_TITLE, title)
            put(COLUMN_TASK_DESCRIPTION, description)
            put(COLUMN_TASK_DUE_DATE, dueDate)
            put(COLUMN_TASK_PRIORITY, priority)
            put(COLUMN_TASK_COMPLETED, 0)
            put(COLUMN_TASK_CREATED_DATE, date)
            put(COLUMN_TASK_USER_FK, userId)
        }

        return try {
            val result = db.insert(TABLE_TASKS, null, contentValues)
            db.close()
            result != -1L
        } catch (e: Exception) {
            db.close()
            false
        }
    }

    fun updateTask(taskId: Int, title: String, description: String, dueDate: String, priority: Int, completed: Boolean): Boolean {
        val db = this.writableDatabase

        val contentValues = ContentValues().apply {
            put(COLUMN_TASK_TITLE, title)
            put(COLUMN_TASK_DESCRIPTION, description)
            put(COLUMN_TASK_DUE_DATE, dueDate)
            put(COLUMN_TASK_PRIORITY, priority)
            put(COLUMN_TASK_COMPLETED, if (completed) 1 else 0)
        }

        return try {
            val result = db.update(TABLE_TASKS, contentValues, "$COLUMN_TASK_ID = ?", arrayOf(taskId.toString()))
            db.close()
            result > 0
        } catch (e: Exception) {
            db.close()
            false
        }
    }

    fun deleteTask(taskId: Int): Boolean {
        val db = this.writableDatabase
        return try {
            val result = db.delete(TABLE_TASKS, "$COLUMN_TASK_ID = ?", arrayOf(taskId.toString()))
            db.close()
            result > 0
        } catch (e: Exception) {
            db.close()
            false
        }
    }

    fun getAllTasks(userId: Int): List<Task> {
        val tasks = mutableListOf<Task>()
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_TASKS WHERE $COLUMN_TASK_USER_FK = ? ORDER BY $COLUMN_TASK_COMPLETED ASC, $COLUMN_TASK_PRIORITY DESC, $COLUMN_TASK_DUE_DATE ASC"
        val cursor = db.rawQuery(query, arrayOf(userId.toString()))

        try {
            if (cursor.moveToFirst()) {
                do {
                    val taskIdIndex = cursor.getColumnIndex(COLUMN_TASK_ID)
                    val titleIndex = cursor.getColumnIndex(COLUMN_TASK_TITLE)
                    val descriptionIndex = cursor.getColumnIndex(COLUMN_TASK_DESCRIPTION)
                    val dueDateIndex = cursor.getColumnIndex(COLUMN_TASK_DUE_DATE)
                    val priorityIndex = cursor.getColumnIndex(COLUMN_TASK_PRIORITY)
                    val completedIndex = cursor.getColumnIndex(COLUMN_TASK_COMPLETED)
                    val createdDateIndex = cursor.getColumnIndex(COLUMN_TASK_CREATED_DATE)

                    if (taskIdIndex != -1 && titleIndex != -1 && descriptionIndex != -1 &&
                        dueDateIndex != -1 && priorityIndex != -1 && completedIndex != -1 && createdDateIndex != -1) {

                        val taskId = cursor.getInt(taskIdIndex)
                        val title = cursor.getString(titleIndex)
                        val description = cursor.getString(descriptionIndex)
                        val dueDate = cursor.getString(dueDateIndex)
                        val priority = cursor.getInt(priorityIndex)
                        val completed = cursor.getInt(completedIndex) == 1
                        val createdDate = cursor.getString(createdDateIndex)

                        tasks.add(Task(taskId, title, description, dueDate, priority, completed, createdDate))
                    }
                } while (cursor.moveToNext())
            }
        } catch (e: Exception) {
            // Handle error
        } finally {
            cursor.close()
            db.close()
        }
        return tasks
    }

    fun getTaskById(taskId: Int): Task? {
        val db = this.readableDatabase
        val query = "SELECT * FROM $TABLE_TASKS WHERE $COLUMN_TASK_ID = ?"
        val cursor = db.rawQuery(query, arrayOf(taskId.toString()))

        var task: Task? = null
        try {
            if (cursor.moveToFirst()) {
                val taskIdIndex = cursor.getColumnIndex(COLUMN_TASK_ID)
                val titleIndex = cursor.getColumnIndex(COLUMN_TASK_TITLE)
                val descriptionIndex = cursor.getColumnIndex(COLUMN_TASK_DESCRIPTION)
                val dueDateIndex = cursor.getColumnIndex(COLUMN_TASK_DUE_DATE)
                val priorityIndex = cursor.getColumnIndex(COLUMN_TASK_PRIORITY)
                val completedIndex = cursor.getColumnIndex(COLUMN_TASK_COMPLETED)
                val createdDateIndex = cursor.getColumnIndex(COLUMN_TASK_CREATED_DATE)

                if (taskIdIndex != -1 && titleIndex != -1 && descriptionIndex != -1 &&
                    dueDateIndex != -1 && priorityIndex != -1 && completedIndex != -1 && createdDateIndex != -1) {

                    val id = cursor.getInt(taskIdIndex)
                    val title = cursor.getString(titleIndex)
                    val description = cursor.getString(descriptionIndex)
                    val dueDate = cursor.getString(dueDateIndex)
                    val priority = cursor.getInt(priorityIndex)
                    val completed = cursor.getInt(completedIndex) == 1
                    val createdDate = cursor.getString(createdDateIndex)

                    task = Task(id, title, description, dueDate, priority, completed, createdDate)
                }
            }
        } catch (e: Exception) {
            // Handle error
        } finally {
            cursor.close()
            db.close()
        }
        return task
    }

    fun toggleTaskCompletion(taskId: Int, completed: Boolean): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(COLUMN_TASK_COMPLETED, if (completed) 1 else 0)
        }

        return try {
            val result = db.update(TABLE_TASKS, contentValues, "$COLUMN_TASK_ID = ?", arrayOf(taskId.toString()))
            db.close()
            result > 0
        } catch (e: Exception) {
            db.close()
            false
        }
    }

    fun getTasksCount(userId: Int): Int {
        val db = this.readableDatabase
        val query = "SELECT COUNT(*) FROM $TABLE_TASKS WHERE $COLUMN_TASK_USER_FK = ?"
        val cursor = db.rawQuery(query, arrayOf(userId.toString()))
        var count = 0

        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        db.close()
        return count
    }

    fun getCompletedTasksCount(userId: Int): Int {
        val db = this.readableDatabase
        val query = "SELECT COUNT(*) FROM $TABLE_TASKS WHERE $COLUMN_TASK_USER_FK = ? AND $COLUMN_TASK_COMPLETED = 1"
        val cursor = db.rawQuery(query, arrayOf(userId.toString()))
        var count = 0

        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        db.close()
        return count
    }

    fun getNotesCount(userId: Int): Int {
        val db = this.readableDatabase
        val query = "SELECT COUNT(*) FROM $TABLE_NOTES WHERE $COLUMN_USER_FK = ?"
        val cursor = db.rawQuery(query, arrayOf(userId.toString()))
        var count = 0

        if (cursor.moveToFirst()) {
            count = cursor.getInt(0)
        }
        cursor.close()
        db.close()
        return count
    }
}

// Data class for Note
data class Note(
    val id: Int,
    val title: String,
    val content: String,
    val date: String
)

// Data class for Task (new)
data class Task(
    val id: Int,
    val title: String,
    val description: String,
    val dueDate: String,
    val priority: Int, // 1 = Low, 2 = Medium, 3 = High
    val completed: Boolean,
    val createdDate: String
)