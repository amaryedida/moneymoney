package com.money.moneymoney

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.SignInButton
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.gms.tasks.Task
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
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
    private lateinit var googleSignInClient: GoogleSignInClient
    private var signedInAccount: GoogleSignInAccount? = null
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

        // Disable Drive sync buttons until signed in
        buttonSyncToDrive.isEnabled = false
        buttonDownloadFromDrive.isEnabled = false

        // Register for the Activity Result for Google Sign-In
        val signInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                handleSignInResult(task)
            } else {
                Toast.makeText(this, "Sign-in cancelled or failed", Toast.LENGTH_SHORT).show()
            }
        }

        // Configure sign-in to request the user's ID, email address, and Drive file scope
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope("https://www.googleapis.com/auth/drive.file"))
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        buttonGoogleSignIn.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            signInLauncher.launch(signInIntent)
        }

        buttonSyncToDrive.setOnClickListener {
            if (signedInAccount == null) {
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
            if (signedInAccount == null) {
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
            googleSignInClient.signOut().addOnCompleteListener {
                signedInAccount = null
                Toast.makeText(this, "Signed out", Toast.LENGTH_SHORT).show()
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

        // Auto sign-in if user is already signed in
        val lastSignedInAccount = GoogleSignIn.getLastSignedInAccount(this)
        if (lastSignedInAccount != null) {
            signedInAccount = lastSignedInAccount
            buttonGoogleSignIn.visibility = View.GONE
            buttonSignOut.visibility = View.VISIBLE
            buttonSyncToDrive.isEnabled = true
            buttonDownloadFromDrive.isEnabled = true
            textViewUserEmail.text = lastSignedInAccount.email ?: ""
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            signedInAccount = account
            Toast.makeText(this, "Signed in as: ${account?.email}", Toast.LENGTH_SHORT).show()
            buttonGoogleSignIn.visibility = View.GONE
            buttonSignOut.visibility = View.VISIBLE
            buttonSyncToDrive.isEnabled = true
            buttonDownloadFromDrive.isEnabled = true
            textViewUserEmail.text = account?.email ?: ""
        } catch (e: ApiException) {
            Toast.makeText(this, "Sign-in failed: ${e.statusCode}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupDriveService() {
        if (driveService == null && signedInAccount != null) {
            val credential = GoogleAccountCredential.usingOAuth2(
                this, listOf("https://www.googleapis.com/auth/drive.file")
            )
            credential.selectedAccount = signedInAccount!!.account
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
                    val contentStream = ByteArrayContent.fromString("application/octet-stream", String(fileContent))
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
                Log.e("DriveSync", "Cannot delete current database - it may be in use.")
                return "Error: Cannot replace database - it is currently in use."
            }

            // Replace the current database with the backup
            Log.d("DriveSync", "Attempting to replace database file...")
            try {
                backupPath.copyTo(dbPath, overwrite = true)
                Log.d("DriveSync", "Database file replaced successfully.")
                
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
                dbHelperForReopen.writableDatabase
                Log.d("DriveSync", "Database reopened successfully.")

                "Download and restore successful"
            } catch (e: Exception) {
                Log.e("DriveSync", "Error replacing database file", e)
                return "Error replacing database file: ${e.localizedMessage}"
            }
        } catch (e: Exception) {
            Log.e("DriveSync", "Download error", e)
            return "Download error: ${e.localizedMessage}"
        }
    }

    private fun createBackupFile(context: android.content.Context): Boolean {
        try {
            val dbHelper = DatabaseHelper(context)
            Log.d("DriveSync", "Starting createBackupFile function.")
            val db = dbHelper.readableDatabase
            Log.d("DriveSync", "Accessed readable database.")

            // Get the backup file path
            val backupPath = context.getFileStreamPath("backup.db")
            Log.d("DriveSync", "Target backup file path: ${backupPath.absolutePath}")

            // Delete existing backup if it exists
            if (backupPath.exists()) {
                Log.d("DriveSync", "Existing backup file found at ${backupPath.absolutePath}. Deleting.")
                if (backupPath.delete()) {
                    Log.d("DriveSync", "Existing backup file deleted successfully.")
                } else {
                    Log.w("DriveSync", "Failed to delete existing backup file.")
                }
                backupPath.delete()
            }

            // Create a new backup using SQLite's backup API
            val backupDb = SQLiteDatabase.openDatabase(
                backupPath.absolutePath,
                null,
                SQLiteDatabase.CREATE_IF_NECESSARY or SQLiteDatabase.OPEN_READWRITE
            )
            Log.d("DriveSync", "Opened backup database in CREATE_IF_NECESSARY mode.")


            // Perform the backup
            db.beginTransaction()
            try {
                // Attach the backup database
                db.execSQL("ATTACH DATABASE '${backupPath.absolutePath}' AS backup")

                Log.d("DriveSync", "Attached backup database.")
                // Copy all tables
                val tables = db.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name != 'android_metadata'",
                    null
                )

                tables.use { cursor ->
                    while (cursor.moveToNext()) {
                        val tableName = cursor.getString(0)
                        Log.d("DriveSync", "Copying table: $tableName")
                        // Drop table if it exists in backup before creating
                        db.execSQL("DROP TABLE IF EXISTS backup.$tableName")
                        // Copy table structure and data
                        db.execSQL("CREATE TABLE backup.$tableName AS SELECT * FROM $tableName")
                    }
                }

                // Copy indices
                Log.d("DriveSync", "Copying indices.")
                val indices = db.rawQuery(
                    "SELECT name, sql FROM sqlite_master WHERE type='index' AND name NOT LIKE 'sqlite_%'",
                    null
                )

                indices.use { cursor ->
                    while (cursor.moveToNext()) {
                        val indexName = cursor.getString(0)
                        val indexSql = cursor.getString(1)
                        Log.d("DriveSync", "Copying index: $indexName")
                        // Create index in backup database
                        db.execSQL(indexSql.replace("CREATE INDEX", "CREATE INDEX IF NOT EXISTS"))
                    }
                }

                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
                Log.d("DriveSync", "Transaction ended.")
                // Detach the backup database
                db.execSQL("DETACH DATABASE backup")
                Log.d("DriveSync", "Detached backup database.")
            }

            // Close both databases
            backupDb.close()
            db.close()
            Log.d("DriveSync", "Closed original and backup databases.")
            dbHelper.close()

            Log.d("DriveSync", "Backup created successfully at: ${backupPath.absolutePath}")
            return true
        } catch (e: Exception) {
            Log.e("DriveSync", "Backup creation failed", e)
            // Delete the partially created backup file if it exists
            val backupPath = context.getFileStreamPath("backup.db")
            if (backupPath.exists()) {
                Log.w("DriveSync", "Deleting partially created backup file: ${backupPath.absolutePath}")
                backupPath.delete()
            }
            Log.e("DriveSync", "createBackupFile function finished with failure.")
            return false
        }
    }
}