package com.example.appcentinela

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
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
        val btnGallery: Button = findViewById(R.id.btnGallery) // Asegúrate de añadir este ID
        btnGallery.setOnClickListener {
            startActivity(Intent(this, GalleryActivity::class.java))
        }
        btnUninstall.setOnClickListener {
            // 1. Revocamos el permiso de administrador si está activo
            if (devicePolicyManager.isAdminActive(compName)) {
                devicePolicyManager.removeActiveAdmin(compName)
                Toast.makeText(this, "Permiso de administrador desactivado.", Toast.LENGTH_SHORT).show()
            }

            // 2. Abrimos la pantalla de detalles de la app para que el usuario complete la desinstalación
            Toast.makeText(this, "Serás redirigido para completar la desinstalación.", Toast.LENGTH_LONG).show()
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            } catch (e: Exception) {
                // Como plan B, abre la lista de todas las apps
                startActivity(Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS))
            }
        }
    }


    private fun checkAndRequestPermissions() {
        // 1. Comprobar permiso de DIBUJAR SOBRE OTRAS APPS
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Necesitamos permiso para mostrar sobre otras apps", Toast.LENGTH_LONG).show()
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
            return // Detenemos el flujo aquí. El usuario volverá y lo intentará de nuevo.
        }

        // 2. Comprobar permiso de CÁMARA
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            return // Detenemos el flujo. La lógica continuará cuando el usuario responda.
        }

        // 3. Si todos los permisos anteriores están concedidos, pedimos ser ADMIN
        askForDeviceAdmin()
    }

    private val requestCameraPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permiso de cámara OK, ahora pedimos ser admin
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