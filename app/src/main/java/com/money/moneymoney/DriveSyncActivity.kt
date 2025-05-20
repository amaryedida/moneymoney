package com.money.moneymoney

import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.SignInButton
import com.money.moneymoney.R
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File as DriveFile
import java.io.ByteArrayContent
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.ByteArrayOutputStream

class DriveSyncActivity : AppCompatActivity() {
    private lateinit var buttonGoogleSignIn: SignInButton
    private lateinit var buttonSyncToDrive: Button
    private lateinit var buttonDownloadFromDrive: Button
    private lateinit var checkboxAutoSyncWifi: CheckBox
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001
    private var signedInAccount: GoogleSignInAccount? = null
    private var driveService: Drive? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drive_sync)

        buttonGoogleSignIn = findViewById(R.id.buttonGoogleSignIn)
        buttonSyncToDrive = findViewById(R.id.buttonSyncToDrive)
        buttonDownloadFromDrive = findViewById(R.id.buttonDownloadFromDrive)
        checkboxAutoSyncWifi = findViewById(R.id.checkboxAutoSyncWifi)

        // Configure sign-in to request the user's ID, email address, and Drive file scope
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(
                com.google.android.gms.common.api.Scope("https://www.googleapis.com/auth/drive.file")
            )
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        buttonGoogleSignIn.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }

        buttonSyncToDrive.setOnClickListener {
            if (signedInAccount == null) {
                Toast.makeText(this, "Please sign in first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            setupDriveService()
            CoroutineScope(Dispatchers.IO).launch {
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task: Task<GoogleSignInAccount> = GoogleSignIn.getSignedInAccountFromIntent(data)
            handleSignInResult(task)
        }
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)
            signedInAccount = account
            Toast.makeText(this, "Signed in as: ${account?.email}", Toast.LENGTH_SHORT).show()
            // TODO: Enable Drive sync buttons
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
                AndroidHttp.newCompatibleTransport(),
                GsonFactory.getDefaultInstance(),
                credential
            ).setApplicationName(getString(R.string.app_name)).build()
        }
    }

    private fun uploadFileToDrive(): String {
        return try {
            // Example: Upload a local file named "backup.db" from app files dir
            val filePath = getFileStreamPath("backup.db")
            if (!filePath.exists()) return "Backup file not found"
            val fileContent = FileInputStream(filePath).readBytes()
            val driveFile = DriveFile()
            driveFile.name = "backup.db"
            val contentStream = ByteArrayContent.fromString("application/octet-stream", String(fileContent))
            val createdFile = driveService?.files()?.create(driveFile, contentStream)?.setFields("id")?.execute()
            if (createdFile != null) "Upload successful: ${createdFile.id}" else "Upload failed"
        } catch (e: Exception) {
            Log.e("DriveSync", "Upload error", e)
            "Upload error: ${e.localizedMessage}"
        }
    }

    private fun downloadFileFromDrive(): String {
        return try {
            // Example: Download the first file named "backup.db" from Drive
            val result = driveService?.files()?.list()?.setQ("name='backup.db'")?.setSpaces("drive")?.execute()
            val file = result?.files?.firstOrNull() ?: return "No backup found on Drive"
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
}
