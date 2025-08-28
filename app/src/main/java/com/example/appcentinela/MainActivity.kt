package com.example.appcentinela

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var devicePolicyManager: DevicePolicyManager
    private lateinit var compName: ComponentName
    private val OVERLAY_PERMISSION_REQUEST_CODE = 1234

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        compName = ComponentName(this, DeviceAdmin::class.java)

        val btnActivate: Button = findViewById(R.id.btnActivate)
        val btnSettings: Button = findViewById(R.id.btnSettings)
        val btnGallery: Button = findViewById(R.id.btnGallery)
        // ===== 1. AÑADE ESTA LÍNEA para obtener la referencia al nuevo botón =====
        val btnUninstall: Button = findViewById(R.id.btnUninstall)

        btnActivate.setOnClickListener {
            if (!devicePolicyManager.isAdminActive(compName)) {
                // Si no es admin, empezamos el proceso de solicitar todos los permisos
                checkAndRequestPermissions()
            } else {
                Toast.makeText(this, "La protección ya está activa.", Toast.LENGTH_SHORT).show()
            }
        }

        btnSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        btnGallery.setOnClickListener {
            startActivity(Intent(this, GalleryActivity::class.java))
        }

        // ===== 2. AÑADE ESTE BLOQUE para la lógica del nuevo botón =====
        btnUninstall.setOnClickListener {
            // Creamos un Intent para ir a los detalles de la aplicación en los Ajustes del sistema
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            val uri: Uri = Uri.fromParts("package", packageName, null)
            intent.data = uri
            startActivity(intent)
        }
    }

    private fun checkAndRequestPermissions() {
        // ... (El resto de tu código no necesita cambios)
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Necesitamos permiso para mostrar sobre otras apps", Toast.LENGTH_LONG).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
            return
        }

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            return
        }

        askForDeviceAdmin()
    }

    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                askForDeviceAdmin()
            } else {
                Toast.makeText(this, "El permiso de cámara es necesario", Toast.LENGTH_LONG).show()
            }
        }

    private fun askForDeviceAdmin() {








        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, compName)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Permiso necesario para detectar intentos de desbloqueo incorrectos."
            )
        }
        startActivity(intent)
    }
}