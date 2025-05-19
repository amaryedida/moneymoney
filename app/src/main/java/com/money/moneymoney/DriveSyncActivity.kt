package com.money.moneymoney

import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.common.SignInButton
import com.money.moneymoney.R

class DriveSyncActivity : AppCompatActivity() {
    private lateinit var buttonGoogleSignIn: SignInButton
    private lateinit var buttonSyncToDrive: Button
    private lateinit var buttonDownloadFromDrive: Button
    private lateinit var checkboxAutoSyncWifi: CheckBox

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_drive_sync)

        buttonGoogleSignIn = findViewById(R.id.buttonGoogleSignIn)
        buttonSyncToDrive = findViewById(R.id.buttonSyncToDrive)
        buttonDownloadFromDrive = findViewById(R.id.buttonDownloadFromDrive)
        checkboxAutoSyncWifi = findViewById(R.id.checkboxAutoSyncWifi)

        buttonGoogleSignIn.setOnClickListener {
            // TODO: Implement Google Sign-In
            Toast.makeText(this, "Google Sign-In clicked", Toast.LENGTH_SHORT).show()
        }
        buttonSyncToDrive.setOnClickListener {
            // TODO: Implement sync (backup) to Google Drive
            Toast.makeText(this, "Sync to Drive clicked", Toast.LENGTH_SHORT).show()
        }
        buttonDownloadFromDrive.setOnClickListener {
            // TODO: Implement download (restore) from Google Drive
            Toast.makeText(this, "Download from Drive clicked", Toast.LENGTH_SHORT).show()
        }
        checkboxAutoSyncWifi.setOnCheckedChangeListener { _, isChecked ->
            // TODO: Save auto-sync over WiFi preference
            Toast.makeText(this, "Auto Sync over WiFi: $isChecked", Toast.LENGTH_SHORT).show()
        }
    }
}
