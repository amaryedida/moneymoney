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
                folder?.id
            }
            if (folderId == null) return "Failed to create/find Drive folder"

            // 2. Check if backup.db exists in folder
            val fileList = driveService?.files()?.list()
                ?.setQ("name='backup.db' and '$folderId' in parents and trashed=false")
                ?.setSpaces("drive")
                ?.execute()
            val filePath = getFileStreamPath("backup.db")
            if (!filePath.exists()) return "Backup file not found"
            val fileContent = FileInputStream(filePath).readBytes()
            val contentStream = ByteArrayContent.fromString("application/octet-stream", String(fileContent))
            if (fileList != null && fileList.files.isNotEmpty()) {
                // File exists, update it
                val fileId = fileList.files[0].id
                val driveFile = DriveFile()
                driveFile.name = "backup.db"
                val updatedFile = driveService?.files()?.update(fileId, driveFile, contentStream)?.setFields("id")?.execute()
                if (updatedFile != null) "Upload (replace) successful: ${updatedFile.id}" else "Upload failed"
            } else {
                // File does not exist, create it
                val driveFile = DriveFile()
                driveFile.name = "backup.db"
                driveFile.parents = listOf(folderId)
                val createdFile = driveService?.files()?.create(driveFile, contentStream)?.setFields("id")?.execute()
                if (createdFile != null) "Upload successful: ${createdFile.id}" else "Upload failed"
            }
        } catch (e: Exception) {
            Log.e("DriveSync", "Upload error", e)
            "Upload error: ${e.localizedMessage}"
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
                folderList.files[0].id
            } else {
                return "No MoneyMoney folder found on Drive"
            }
            // 2. Find backup.db in the folder
            val fileList = driveService?.files()?.list()
                ?.setQ("name='backup.db' and '$folderId' in parents and trashed=false")
                ?.setSpaces("drive")
                ?.execute()
            val file = fileList?.files?.firstOrNull() ?: return "No backup found in MoneyMoney folder"
            val output = ByteArrayOutputStream()
            driveService?.files()?.get(file.id)?.executeMediaAndDownloadTo(output)

            // 3. Save the backup file
            val backupPath = getFileStreamPath("backup.db")
            FileOutputStream(backupPath).use { it.write(output.toByteArray()) }
            Log.d("DriveSync", "Backup file saved locally at: ${backupPath.absolutePath}")
            if (!backupPath.exists()) {
                Log.e("DriveSync", "Local backup file does not exist after saving.")
                return "Error: Local backup file not saved."
            }
            Log.d("DriveSync", "Local backup file size: ${backupPath.length()}")

            // 4. Restore the database
            val dbPath = getDatabasePath("MoneyMoney.db")
            Log.d("DriveSync", "Current database path: ${dbPath.absolutePath}")

            // Close all database connections
            Log.d("DriveSync", "Closing database connections...")
            val dbHelperForClose = DatabaseHelper(this)
            dbHelperForClose.close()
            Log.d("DriveSync", "Database connections closed.")

            // Check if the current database file exists before replacement
            if (!dbPath.exists()) {
                 Log.w("DriveSync", "Current database file does not exist before replacement.")
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
                Log.d("DriveSync", "New database file size: ${dbPath.length()}")

            } catch (e: Exception) {
                Log.e("DriveSync", "Error replacing database file", e)
                return "Error replacing database file: ${e.localizedMessage}"
            }

            // Reopen the database
            Log.d("DriveSync", "Reopening database...")
            val dbHelperForReopen = DatabaseHelper(this)
            dbHelperForReopen.writableDatabase
            Log.d("DriveSync", "Database reopened.")

            "Download and restore successful"
        } catch (e: Exception) {
            Log.e("DriveSync", "Download error", e)
            "Download error: ${e.localizedMessage}"
        }
    }

    private fun createBackupFile(context: android.content.Context): Boolean {
        try {
            val dbHelper = DatabaseHelper(context)
            val db = dbHelper.readableDatabase

            // Get the backup file path
            val backupPath = context.getFileStreamPath("backup.db")

            // Delete existing backup if it exists
            if (backupPath.exists()) {
                backupPath.delete()
            }

            // Create a new backup using SQLite's backup API
            val backupDb = SQLiteDatabase.openDatabase(
                backupPath.absolutePath,
                null,
                SQLiteDatabase.CREATE_IF_NECESSARY
            )

            // Perform the backup
            db.beginTransaction()
            try {
                // Attach the backup database
                db.execSQL("ATTACH DATABASE '${backupPath.absolutePath}' AS backup")

                // Copy all tables
                val tables = db.rawQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name != 'android_metadata'",
                    null
                )

                tables.use { cursor ->
                    while (cursor.moveToNext()) {
                        val tableName = cursor.getString(0)
                        // Drop table if it exists in backup before creating
                        db.execSQL("DROP TABLE IF EXISTS backup.$tableName")
                        // Copy table structure and data
                        db.execSQL("CREATE TABLE backup.$tableName AS SELECT * FROM $tableName")
                    }
                }

                // Copy indices
                val indices = db.rawQuery(
                    "SELECT name, sql FROM sqlite_master WHERE type='index' AND name NOT LIKE 'sqlite_%'",
                    null
                )

                indices.use { cursor ->
                    while (cursor.moveToNext()) {
                        val indexName = cursor.getString(0)
                        val indexSql = cursor.getString(1)
                        // Create index in backup database
                        db.execSQL(indexSql.replace("CREATE INDEX", "CREATE INDEX IF NOT EXISTS"))
                    }
                }

                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
                // Detach the backup database
                db.execSQL("DETACH DATABASE backup")
            }

            // Close both databases
            backupDb.close()
            db.close()
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
            return false
        }
    }
}
