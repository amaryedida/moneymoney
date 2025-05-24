package com.money.moneymoney

import android.os.Bundle
import android.util.Log
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

class DriveSyncActivity : AppCompatActivity() {
    private lateinit var buttonGoogleSignIn: SignInButton
    private lateinit var buttonSyncToDrive: Button
    private lateinit var buttonDownloadFromDrive: Button
    private lateinit var checkboxAutoSyncWifi: CheckBox
    private lateinit var googleSignInClient: GoogleSignInClient
    private var signedInAccount: GoogleSignInAccount? = null
    private var driveService: Drive? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drive_sync)

        buttonGoogleSignIn = findViewById(R.id.buttonGoogleSignIn)
        buttonSyncToDrive = findViewById(R.id.buttonSyncToDrive)
        buttonDownloadFromDrive = findViewById(R.id.buttonDownloadFromDrive)
        checkboxAutoSyncWifi = findViewById(R.id.checkboxAutoSyncWifi)

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
                createBackupFile(this@DriveSyncActivity)
                val result = uploadFileToDrive()
                runOnUiThread {
                    Toast.makeText(this@DriveSyncActivity, result, Toast.LENGTH_SHORT).show()
                }
            }
        }

        buttonDownloadFromDrive.setOnClickListener {
            if (signedInAccount == null) {
                Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            setupDriveService()
            CoroutineScope(Dispatchers.IO).launch {
                val result = downloadFileFromDrive()
                runOnUiThread {
                    Toast.makeText(this@DriveSyncActivity, result, Toast.LENGTH_SHORT).show()
                }
            }
        }

        checkboxAutoSyncWifi.setOnCheckedChangeListener { _, isChecked ->
            // TODO: Save auto-sync over WiFi preference
            Toast.makeText(this, "Auto Sync over WiFi: $isChecked", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            signedInAccount = account
            Toast.makeText(this, "Signed in as: ${account?.email}", Toast.LENGTH_SHORT).show()
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
            val filePath = getFileStreamPath("backup.db")
            FileOutputStream(filePath).use { it.write(output.toByteArray()) }
            "Download successful"
        } catch (e: Exception) {
            Log.e("DriveSync", "Download error", e)
            "Download error: ${e.localizedMessage}"
        }
    }

    private fun createBackupFile(context: android.content.Context) {
        try {
            val dbPath = context.getDatabasePath("MoneyMoney.db")
            val backupPath = context.getFileStreamPath("backup.db")
                java.io.FileInputStream(dbPath).use { input ->
                java.io.FileOutputStream(backupPath).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            Log.e("DriveSync", "Backup creation failed", e)
        }
    }
}
