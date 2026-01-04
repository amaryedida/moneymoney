package com.money.moneymoney

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.google.api.client.http.ByteArrayContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File as DriveFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ByteArrayOutputStream
import java.io.File
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import android.database.sqlite.SQLiteDatabase

class DriveSyncActivity : AppCompatActivity() {
    private lateinit var buttonGoogleSignIn: SignInButton
    private lateinit var buttonSyncToDrive: Button
    private lateinit var buttonDownloadFromDrive: Button
    private lateinit var checkboxAutoSyncWifi: CheckBox
    private lateinit var buttonSignOut: Button
    private lateinit var credentialManager: CredentialManager
    private var signedInEmail: String? = null
    private var driveService: Drive? = null
    private lateinit var textViewUserEmail: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drive_sync)

        buttonGoogleSignIn = findViewById(R.id.buttonGoogleSignIn)
        buttonSyncToDrive = findViewById(R.id.buttonSyncToDrive)
        buttonDownloadFromDrive = findViewById(R.id.buttonDownloadFromDrive)
        checkboxAutoSyncWifi = findViewById(R.id.checkboxAutoSyncWifi)
        buttonSignOut = findViewById(R.id.buttonSignOut)
        buttonSignOut.visibility = View.GONE // Hide by default
        textViewUserEmail = findViewById(R.id.textViewUserEmail)

        // Initialize Credential Manager
        credentialManager = CredentialManager.create(this)

        // Disable Drive sync buttons until signed in
        buttonSyncToDrive.isEnabled = false
        buttonDownloadFromDrive.isEnabled = false

        buttonGoogleSignIn.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                signIn()
            }
        }

        buttonSyncToDrive.setOnClickListener {
            if (signedInEmail == null) {
                Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            setupDriveService()
            CoroutineScope(Dispatchers.IO).launch {
                val backupSuccessful = createBackupFile(this@DriveSyncActivity)
                if (backupSuccessful) {
                    val result = uploadFileToDrive()
                    runOnUiThread {
                        Toast.makeText(this@DriveSyncActivity, result, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@DriveSyncActivity, "Backup creation failed. Upload cancelled.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        buttonDownloadFromDrive.setOnClickListener {
            if (signedInEmail == null) {
                Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Show confirmation dialog
            AlertDialog.Builder(this)
                .setTitle("Confirm Restore")
                .setMessage("This will replace all current data with the backup data from Google Drive. This action cannot be undone. Do you want to continue?")
                .setPositiveButton("Yes") { _, _ ->
                    setupDriveService()
                    CoroutineScope(Dispatchers.IO).launch {
                        val result = downloadFileFromDrive()
                        runOnUiThread {
                            Toast.makeText(this@DriveSyncActivity, result, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .setNegativeButton("No", null)
                .show()
        }

        buttonSignOut.setOnClickListener {
            CoroutineScope(Dispatchers.Main).launch {
                signedInEmail = null
                driveService = null
                credentialManager.clearCredentialState(androidx.credentials.ClearCredentialStateRequest())
                Toast.makeText(this@DriveSyncActivity, "Signed out", Toast.LENGTH_SHORT).show()
                buttonGoogleSignIn.visibility = View.VISIBLE
                buttonSignOut.visibility = View.GONE
                buttonSyncToDrive.isEnabled = false
                buttonDownloadFromDrive.isEnabled = false
                textViewUserEmail.text = ""
            }
        }

        checkboxAutoSyncWifi.setOnCheckedChangeListener { _, isChecked ->
            // TODO: Save auto-sync over WiFi preference
            Toast.makeText(this, "Auto Sync over WiFi: $isChecked", Toast.LENGTH_SHORT).show()
        }

        // Auto sign-in or silent sign-in could be implemented here with getCredential(autoSelect=true)
        // For now, we require manual sign-in to ensure proper permission flows.
    }

    private suspend fun signIn() {
        try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(getString(R.string.default_web_client_id)) // Ensure this string resource exists or use a hardcoded fallback if missing
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(this, request)
            handleSignIn(result)
        } catch (e: GetCredentialException) {
            Log.e("DriveSync", "Sign-in failed", e)
            Toast.makeText(this, "Sign-in failed: ${e.message}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Log.e("DriveSync", "Sign-in error", e)
            Toast.makeText(this, "Sign-in error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleSignIn(result: GetCredentialResponse) {
        val credential = result.credential
        if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
            try {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val email = googleIdTokenCredential.id
                // Note: The 'id' in GoogleIdTokenCredential is the email address (or identifier). 
                // Wait, typically 'id' is the user's email for creating the credential object? 
                // Actually `googleIdTokenCredential.id` is the display name/email usually? 
                // Let's check documentation or assume it's the email or use `id` as account name.
                // Correction: `GoogleIdTokenCredential` does not expose email directly in a dedicated field other than `id` which is the account identifier (email) usually.
                // Actually `id` is the email.
                
                signedInEmail = email
                Toast.makeText(this, "Signed in as: $email", Toast.LENGTH_SHORT).show()
                buttonGoogleSignIn.visibility = View.GONE
                buttonSignOut.visibility = View.VISIBLE
                buttonSyncToDrive.isEnabled = true
                buttonDownloadFromDrive.isEnabled = true
                textViewUserEmail.text = email
            } catch (e: Exception) {
                Log.e("DriveSync", "Invalid credential", e)
                Toast.makeText(this, "Invalid credential data", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.e("DriveSync", "Unexpected credential type")
            Toast.makeText(this, "Unexpected credential type", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupDriveService() {
        if (driveService == null && signedInEmail != null) {
            val credential = GoogleAccountCredential.usingOAuth2(
                this, listOf("https://www.googleapis.com/auth/drive.file")
            )
            // Explicitly create Account object to avoid potential null name issues in internal credential logic
            credential.selectedAccount = android.accounts.Account(signedInEmail!!, "com.google")
            driveService = Drive.Builder(
                NetHttpTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            ).setApplicationName(getString(R.string.app_name)).build()
        }
    }

    private fun uploadFileToDrive(): String {
        return try {
            val folderName = "MoneyMoney"
            Log.d("DriveSync", "Starting uploadFileToDrive function.")
            // 1. Check if folder exists, else create it
            val folderList = driveService?.files()?.list()
                ?.setQ("mimeType='application/vnd.google-apps.folder' and name='$folderName' and trashed=false")
                ?.setSpaces("drive")
                ?.execute()
            val folderId = if (folderList != null && folderList.files.isNotEmpty()) {
                folderList.files[0].id
            } else {
                // Create folder
                val folderMetadata = DriveFile()
                folderMetadata.name = folderName
                folderMetadata.mimeType = "application/vnd.google-apps.folder"
                val folder = driveService?.files()?.create(folderMetadata)?.setFields("id")?.execute()
                Log.d("DriveSync", "Attempting to create Drive folder: $folderName")
                folder?.id
            }

            if (folderId == null) {
                Log.e("DriveSync", "Failed to create or find Drive folder: $folderName")
                "Failed to create/find Drive folder" // Explicitly return String
            } else {
                Log.d("DriveSync", "MoneyMoney Drive folder ID: $folderId")

                // 2. Check if backup.db exists in the folder
                val fileList = driveService?.files()?.list()
                    ?.setQ("name='backup.db' and '$folderId' in parents and trashed=false")
                    ?.setSpaces("drive")
                    ?.execute()
                val filePath = getFileStreamPath("backup.db")
                if (!filePath.exists()) "Backup file not found" // Explicitly return String
                else {
                    Log.d("DriveSync", "Local backup file path: ${filePath.absolutePath}")
                    Log.d("DriveSync", "Local backup file size: ${filePath.length()} bytes")
                    // --- Add verification step here ---
                    try {
                        SQLiteDatabase.openDatabase(filePath.absolutePath, null, SQLiteDatabase.OPEN_READONLY).close()
                        Log.d("DriveSync", "Local backup file is a valid SQLite database.")
                    } catch (e: Exception) {
                        Log.e("DriveSync", "Local backup file is NOT a valid SQLite database before upload.", e)
                        "Error: Created backup file is corrupted locally." // Explicitly return String
                    }
                    // --- End verification step ---

                    val fileContent = FileInputStream(filePath).readBytes()
                    val contentStream = ByteArrayContent("application/octet-stream", fileContent)
                    if (fileList != null && fileList.files.isNotEmpty()) {
                        Log.d("DriveSync", "backup.db found on Drive. Updating existing file.")
                        // File exists, update it
                        val fileId = fileList.files[0].id
                        val driveFile = DriveFile()
                        driveFile.name = "backup.db"
                        val updatedFile = driveService?.files()?.update(fileId, driveFile, contentStream)?.setFields("id")?.execute()
                        if (updatedFile != null) "Upload (replace) successful: ${updatedFile.id}" else "Upload failed" // Explicitly return String
                    } else {
                        // File does not exist, create it
                        Log.d("DriveSync", "backup.db not found on Drive. Creating new file.")
                        val driveFile = DriveFile()
                        driveFile.name = "backup.db"
                        driveFile.parents = listOf(folderId)
                        val createdFile = driveService?.files()?.create(driveFile, contentStream)?.setFields("id")?.execute()
                        if (createdFile != null) "Upload successful: ${createdFile.id}" else "Upload failed" // Explicitly return String
                    }
                }
            }
            Log.d("DriveSync", "uploadFileToDrive function finished.")
            // Need a final return statement here as well to cover all paths
            "Upload process completed" // Explicitly return String
        } catch (e: UserRecoverableAuthIOException) {
            startActivity(e.intent)
            "Permission needed. Please grant access and try again."
        } catch (e: Exception) {
            Log.e("DriveSync", "Upload error", e)
            "Upload error: ${e.localizedMessage}" // Explicitly return String
        }
    }

    private fun downloadFileFromDrive(): String {
        return try {
            Log.d("DriveSync", "downloadFileFromDrive function started.")
            val folderName = "MoneyMoney"

            // 1. Find the MoneyMoney folder
            val folderList = driveService?.files()?.list()
                ?.setQ("mimeType='application/vnd.google-apps.folder' and name='$folderName' and trashed=false")
                ?.setSpaces("drive")
                ?.execute()
            val folderId = if (folderList != null && folderList.files.isNotEmpty()) {
                Log.d("DriveSync", "Found MoneyMoney Drive folder ID: ${folderList.files[0].id}")
                folderList.files[0].id
            } else {
                Log.w("DriveSync", "No MoneyMoney folder found on Drive.")
                return "No MoneyMoney folder found on Drive"
            }

            // 2. Find backup.db in the folder
            Log.d("DriveSync", "Searching for backup.db in folder: $folderId")
            val fileList = driveService?.files()?.list()
                ?.setQ("name='backup.db' and '$folderId' in parents and trashed=false")
                ?.setSpaces("drive")
                ?.execute()
            val file = fileList?.files?.firstOrNull()
            if (file == null) {
                return "No backup found in MoneyMoney folder"
            }

            // 3. Download the backup file
            val output = ByteArrayOutputStream()
            driveService?.files()?.get(file.id)?.executeMediaAndDownloadTo(output)
            Log.d("DriveSync", "backup.db found and downloaded from Drive. File ID: ${file.id}")

            // 4. Save the backup file
            val backupPath = getFileStreamPath("backup.db")
            FileOutputStream(backupPath).use { it.write(output.toByteArray()) }
            Log.d("DriveSync", "Backup file saved locally at: ${backupPath.absolutePath}")
            
            if (!backupPath.exists()) {
                Log.e("DriveSync", "Local backup file does not exist after saving.")
                return "Error: Local backup file not saved."
            }

            // 5. Verify the downloaded backup file
            try {
                val backupDb = SQLiteDatabase.openDatabase(backupPath.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
                // Verify database schema
                val tables = backupDb.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'",
                    null
                )
                val tableCount = tables.count
                tables.close()
                backupDb.close()
                
                if (tableCount == 0) {
                    Log.e("DriveSync", "Downloaded backup file has no tables.")
                    return "Error: Downloaded backup file is empty or corrupted."
                }
                Log.d("DriveSync", "Downloaded backup file is valid with $tableCount tables.")
            } catch (e: Exception) {
                Log.e("DriveSync", "Downloaded backup file is NOT a valid SQLite database.", e)
                return "Error: Downloaded backup file is corrupted."
            }

            // 6. Restore the database
            val dbPath = getDatabasePath("MoneyMoney.db")
            Log.d("DriveSync", "Current database path: ${dbPath.absolutePath}")

            // Close all database connections
            Log.d("DriveSync", "Closing database connections...")
            val dbHelperForClose = DatabaseHelper(this)
            dbHelperForClose.close()
            
            // Wait a moment to ensure all connections are closed
            Thread.sleep(1000)
            
            // Check if database is still in use
            if (dbPath.exists() && !dbPath.delete()) {
                Log.e("DriveSync", "Cannot delete current database - it may be in use. Path: ${dbPath.absolutePath}, Size: ${dbPath.length()}, CanRead: ${dbPath.canRead()}, CanWrite: ${dbPath.canWrite()}")
                return "Error: Cannot replace database - it is currently in use."
            }

            // Replace the current database with the backup
            Log.d("DriveSync", "Attempting to replace database file...")
            Log.d("DriveSync", "Backup file path: ${backupPath.absolutePath}, Size: ${backupPath.length()}")
            if (dbPath.exists()) {
                Log.d("DriveSync", "Old dbPath exists: ${dbPath.absolutePath}, Size: ${dbPath.length()}")
            } else {
                Log.d("DriveSync", "Old dbPath does not exist.")
            }
            try {
                backupPath.copyTo(dbPath, overwrite = true)
                Log.d("DriveSync", "Database file replaced successfully. New dbPath size: ${dbPath.length()}")
                
                if (!dbPath.exists()) {
                    Log.e("DriveSync", "Database file does not exist after replacement.")
                    return "Error: Database file replacement failed."
                }

                // Verify the restored database
                val restoredDb = SQLiteDatabase.openDatabase(dbPath.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
                val restoredTables = restoredDb.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'",
                    null
                )
                val restoredTableCount = restoredTables.count
                restoredTables.close()
                restoredDb.close()

                if (restoredTableCount == 0) {
                    Log.e("DriveSync", "Restored database has no tables.")
                    return "Error: Database restoration verification failed."
                }

                // Reopen the database
                Log.d("DriveSync", "Reopening database...")
                val dbHelperForReopen = DatabaseHelper(this)
                try {
                    dbHelperForReopen.writableDatabase // This will create and/or open the database.
                    Log.d("DriveSync", "Database reopened successfully using new DatabaseHelper.")
                } catch (dbOpenException: Exception) {
                    Log.e("DriveSync", "Failed to reopen database after restore via new DatabaseHelper.", dbOpenException)
                    // Depending on desired behavior, you might want to return an error string here
                    // or attempt to delete the potentially problematic dbPath.
                    return "Error: Failed to reopen database after restore. ${dbOpenException.localizedMessage}"
                }

                "Download and restore successful"
            } catch (e: Exception) {
                Log.e("DriveSync", "Error replacing database file. Message: ${e.message}\nStacktrace: ${e.stackTraceToString()}", e)
                return "Error replacing database file: ${e.localizedMessage}"
            }
        } catch (e: UserRecoverableAuthIOException) {
            startActivity(e.intent)
            "Permission needed. Please grant access and try again."
        } catch (e: Exception) {
            Log.e("DriveSync", "Download error. Message: ${e.message}\nStacktrace: ${e.stackTraceToString()}", e)
            return "Download error: ${e.localizedMessage}"
        }
    }

    private fun createBackupFile(context: android.content.Context): Boolean {
        Log.d("DriveSync", "Starting createBackupFile function (new method).")
        val dbHelper = DatabaseHelper(context) // Keep for context and DB name, path
        val originalDbPath = context.getDatabasePath(DatabaseHelper.DATABASE_NAME)
        val backupPath = context.getFileStreamPath("backup.db")

        try {
            Log.d("DriveSync", "Original DB path: ${originalDbPath.absolutePath}")
            Log.d("DriveSync", "Target backup file path: ${backupPath.absolutePath}")

            // Ensure original database exists
            if (!originalDbPath.exists()) {
                Log.e("DriveSync", "Original database does not exist at ${originalDbPath.absolutePath}")
                return false
            }

            // Close all connections to the database before copying
            Log.d("DriveSync", "Closing database helper to release connections.")
            dbHelper.close() // This closes the SQLiteOpenHelper's database instance

            // It might take a moment for the system to release file locks
            // Consider a small delay or a more robust check if file lock issues persist,
            // but for now, a direct close should be attempted.
            // Thread.sleep(500) // Optional: if lock issues are common

            // Delete existing backup file
            if (backupPath.exists()) {
                Log.d("DriveSync", "Existing backup file found at ${backupPath.absolutePath}. Deleting.")
                if (!backupPath.delete()) {
                    Log.w("DriveSync", "Failed to delete existing backup file. Attempting to continue.")
                }
            }

            // Perform the file copy
            Log.d("DriveSync", "Attempting to copy database file...")
            originalDbPath.copyTo(backupPath, overwrite = true)

            if (backupPath.exists()) {
                Log.d("DriveSync", "Backup created successfully at: ${backupPath.absolutePath}")
                Log.d("DriveSync", "Backup file size: ${backupPath.length()} bytes")

                // Verification step: Try to open the backup to ensure it's valid
                var isValid = false
                try {
                    val backupDb = SQLiteDatabase.openDatabase(backupPath.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
                    isValid = backupDb.isOpen
                    if (isValid) {
                        // Optionally, check for tables as in downloadFileFromDrive
                        val tables = backupDb.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'", null)
                        val tableCount = tables.count
                        tables.close()
                        if (tableCount > 0) {
                            Log.d("DriveSync", "Verified backup DB: $tableCount tables found.")
                        } else {
                            Log.w("DriveSync", "Verified backup DB: No tables found.")
                            // Depending on strictness, could set isValid = false here
                        }
                    }
                    backupDb.close()
                } catch (e: Exception) {
                    Log.e("DriveSync", "Failed to open/verify the created backup file.", e)
                    isValid = false
                }

                if (!isValid) {
                    Log.e("DriveSync", "Created backup file appears to be corrupted or invalid.")
                    if (backupPath.exists()) backupPath.delete() // Clean up invalid backup
                    return false
                }
                return true
            } else {
                Log.e("DriveSync", "Backup file does not exist after copy operation.")
                return false
            }

        } catch (e: Exception) {
            Log.e("DriveSync", "Backup creation failed", e)
            // Clean up potentially corrupted backup file
            if (backupPath.exists()) {
                Log.w("DriveSync", "Deleting partially created or corrupted backup file due to error.")
                backupPath.delete()
            }
            return false
        } finally {
            // Ensure the helper is closed if it wasn't, though it should be.
            // Re-opening the main database connection if needed by other parts of the app
            // should be handled by those other parts, not here.
            // dbHelper.close() // Already called, or if an exception occurred before, it might need it.
            Log.d("DriveSync", "createBackupFile function finished.")
        }
    }
}