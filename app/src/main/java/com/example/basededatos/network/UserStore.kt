package com.example.basededatos.network

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest

data class UserProfile(
    val name: String,
    val university: String,
    val email: String,
    val subjects: List<String>
)

data class AssignmentRecord(
    val id: Long,
    val subject: String,
    val title: String,
    val dueAt: Long,
    val isCompleted: Boolean
)

data class StudyData(
    val subjects: List<String>,
    val assignments: List<AssignmentRecord>
)

class UserStore(context: Context) :
    SQLiteOpenHelper(context.applicationContext, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE $TABLE_USERS (
                $COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_NAME TEXT NOT NULL,
                $COLUMN_UNIVERSITY TEXT NOT NULL,
                $COLUMN_EMAIL TEXT NOT NULL UNIQUE,
                $COLUMN_PASSWORD TEXT NOT NULL,
                $COLUMN_SUBJECTS TEXT NOT NULL DEFAULT ''
            )""".trimIndent()
        )
        createAssignmentsTable(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE $TABLE_USERS ADD COLUMN $COLUMN_SUBJECTS TEXT NOT NULL DEFAULT ''")
        }
        if (oldVersion < 3) {
            createAssignmentsTable(db)
        }
    }

    suspend fun register(profile: UserProfile, password: String): Boolean =
        withContext(Dispatchers.IO) {
            val values = ContentValues().apply {
                put(COLUMN_NAME, profile.name.trim())
                put(COLUMN_UNIVERSITY, profile.university.trim())
                put(COLUMN_EMAIL, profile.email.trim().lowercase())
                put(COLUMN_PASSWORD, hashPassword(password))
                put(COLUMN_SUBJECTS, profile.subjects.joinToString("\n"))
            }
            writableDatabase.insert(TABLE_USERS, null, values) != -1L
        }

    suspend fun login(email: String, password: String): UserProfile? =
        withContext(Dispatchers.IO) {
            readableDatabase.query(
                TABLE_USERS,
                arrayOf(COLUMN_NAME, COLUMN_UNIVERSITY, COLUMN_EMAIL, COLUMN_SUBJECTS),
                "$COLUMN_EMAIL = ? AND $COLUMN_PASSWORD = ?",
                arrayOf(email.trim().lowercase(), hashPassword(password)),
                null,
                null,
                null
            ).use { cursor ->
                if (!cursor.moveToFirst()) return@withContext null
                UserProfile(
                    name = cursor.getString(0),
                    university = cursor.getString(1),
                    email = cursor.getString(2),
                    subjects = cursor.getString(3).toSubjects()
                )
            }
        }

    suspend fun getStudyData(email: String): StudyData = withContext(Dispatchers.IO) {
        val db = readableDatabase
        val normalizedEmail = email.trim().lowercase()
        val subjects = readSubjects(db, normalizedEmail)
        val assignments = mutableListOf<AssignmentRecord>()
        db.query(
            TABLE_ASSIGNMENTS,
            arrayOf(COLUMN_ASSIGNMENT_ID, COLUMN_ASSIGNMENT_SUBJECT, COLUMN_TITLE, COLUMN_DUE_AT, COLUMN_COMPLETED),
            "$COLUMN_EMAIL = ?",
            arrayOf(normalizedEmail),
            null,
            null,
            "$COLUMN_COMPLETED ASC, $COLUMN_DUE_AT ASC, $COLUMN_ASSIGNMENT_ID DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) {
                assignments += AssignmentRecord(
                    id = cursor.getLong(0),
                    subject = cursor.getString(1),
                    title = cursor.getString(2),
                    dueAt = cursor.getLong(3),
                    isCompleted = cursor.getInt(4) == 1
                )
            }
        }
        StudyData(subjects, assignments)
    }

    suspend fun addSubject(email: String, subject: String): Boolean = withContext(Dispatchers.IO) {
        val cleanSubject = subject.trim()
        if (cleanSubject.isBlank()) return@withContext false
        val db = writableDatabase
        val current = readSubjects(db, email.trim().lowercase())
        if (current.any { it.equals(cleanSubject, ignoreCase = true) }) {
            return@withContext false
        }
        val values = ContentValues().apply {
            put(COLUMN_SUBJECTS, (current + cleanSubject).joinToString("\n"))
        }
        db.update(
            TABLE_USERS,
            values,
            "$COLUMN_EMAIL = ?",
            arrayOf(email.trim().lowercase())
        ) > 0
    }

    suspend fun removeSubject(email: String, subject: String): Boolean = withContext(Dispatchers.IO) {
        val normalizedEmail = email.trim().lowercase()
        val db = writableDatabase
        db.beginTransaction()
        try {
            val current = readSubjects(db, normalizedEmail)
            val remaining = current.filterNot { it.equals(subject, ignoreCase = true) }
            if (remaining.size == current.size) return@withContext false

            db.delete(
                TABLE_ASSIGNMENTS,
                "$COLUMN_EMAIL = ? AND $COLUMN_ASSIGNMENT_SUBJECT = ?",
                arrayOf(normalizedEmail, subject)
            )
            val values = ContentValues().apply {
                put(COLUMN_SUBJECTS, remaining.joinToString("\n"))
            }
            val updated = db.update(
                TABLE_USERS,
                values,
                "$COLUMN_EMAIL = ?",
                arrayOf(normalizedEmail)
            ) > 0
            if (updated) db.setTransactionSuccessful()
            updated
        } finally {
            db.endTransaction()
        }
    }

    suspend fun addAssignment(
        email: String,
        subject: String,
        title: String,
        dueAt: Long
    ): Boolean = withContext(Dispatchers.IO) {
        val normalizedEmail = email.trim().lowercase()
        if (readSubjects(writableDatabase, normalizedEmail).none { it == subject }) {
            return@withContext false
        }
        val values = ContentValues().apply {
            put(COLUMN_EMAIL, normalizedEmail)
            put(COLUMN_ASSIGNMENT_SUBJECT, subject)
            put(COLUMN_TITLE, title.trim())
            put(COLUMN_DUE_AT, dueAt)
            put(COLUMN_COMPLETED, 0)
        }
        writableDatabase.insert(TABLE_ASSIGNMENTS, null, values) != -1L
    }

    suspend fun setAssignmentCompleted(email: String, assignmentId: Long, completed: Boolean): Boolean =
        withContext(Dispatchers.IO) {
            val values = ContentValues().apply {
                put(COLUMN_COMPLETED, if (completed) 1 else 0)
            }
            writableDatabase.update(
                TABLE_ASSIGNMENTS,
                values,
                "$COLUMN_ASSIGNMENT_ID = ? AND $COLUMN_EMAIL = ?",
                arrayOf(assignmentId.toString(), email.trim().lowercase())
            ) > 0
        }

    suspend fun deleteAssignment(email: String, assignmentId: Long): Boolean =
        withContext(Dispatchers.IO) {
            writableDatabase.delete(
                TABLE_ASSIGNMENTS,
                "$COLUMN_ASSIGNMENT_ID = ? AND $COLUMN_EMAIL = ?",
                arrayOf(assignmentId.toString(), email.trim().lowercase())
            ) > 0
        }

    suspend fun deleteAccount(email: String, password: String): Boolean =
        withContext(Dispatchers.IO) {
            val normalizedEmail = email.trim().lowercase()
            val db = writableDatabase
            val isValidAccount = db.query(
                TABLE_USERS,
                arrayOf(COLUMN_EMAIL),
                "$COLUMN_EMAIL = ? AND $COLUMN_PASSWORD = ?",
                arrayOf(normalizedEmail, hashPassword(password)),
                null,
                null,
                null
            ).use { it.moveToFirst() }
            if (!isValidAccount) return@withContext false

            db.beginTransaction()
            try {
                db.delete(TABLE_ASSIGNMENTS, "$COLUMN_EMAIL = ?", arrayOf(normalizedEmail))
                val deleted = db.delete(TABLE_USERS, "$COLUMN_EMAIL = ?", arrayOf(normalizedEmail)) > 0
                if (deleted) db.setTransactionSuccessful()
                deleted
            } finally {
                db.endTransaction()
            }
        }

    private fun readSubjects(db: SQLiteDatabase, email: String): List<String> {
        return db.query(
            TABLE_USERS,
            arrayOf(COLUMN_SUBJECTS),
            "$COLUMN_EMAIL = ?",
            arrayOf(email),
            null,
            null,
            null
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0).orEmpty().toSubjects() else emptyList()
        }
    }

    private fun createAssignmentsTable(db: SQLiteDatabase) {
        db.execSQL(
            """CREATE TABLE IF NOT EXISTS $TABLE_ASSIGNMENTS (
                $COLUMN_ASSIGNMENT_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COLUMN_EMAIL TEXT NOT NULL,
                $COLUMN_ASSIGNMENT_SUBJECT TEXT NOT NULL,
                $COLUMN_TITLE TEXT NOT NULL,
                $COLUMN_DUE_AT INTEGER NOT NULL,
                $COLUMN_COMPLETED INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY ($COLUMN_EMAIL) REFERENCES $TABLE_USERS($COLUMN_EMAIL) ON DELETE CASCADE
            )""".trimIndent()
        )
    }

    private fun String?.toSubjects(): List<String> =
        this.orEmpty().split("\n").map { it.trim() }.filter { it.isNotEmpty() }

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return digest.joinToString("") { byte -> "%02x".format(byte) }
    }

    private companion object {
        const val DATABASE_NAME = "UserDatabase.db"
        const val DATABASE_VERSION = 3
        const val TABLE_USERS = "users"
        const val TABLE_ASSIGNMENTS = "assignments"

        const val COLUMN_ID = "id"
        const val COLUMN_NAME = "nombre"
        const val COLUMN_UNIVERSITY = "universidad"
        const val COLUMN_EMAIL = "correo"
        const val COLUMN_PASSWORD = "password"
        const val COLUMN_SUBJECTS = "materias"

        const val COLUMN_ASSIGNMENT_ID = "id"
        const val COLUMN_ASSIGNMENT_SUBJECT = "materia"
        const val COLUMN_TITLE = "titulo"
        const val COLUMN_DUE_AT = "fecha_limite"
        const val COLUMN_COMPLETED = "completada"
    }
}
