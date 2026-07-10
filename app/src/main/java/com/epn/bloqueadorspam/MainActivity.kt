package com.epn.bloqueadorspam

import android.Manifest
import android.app.role.RoleManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.epn.bloqueadorspam.ui.PantallaPrincipal
import com.epn.bloqueadorspam.ui.theme.BloqueadorSpamTheme

class MainActivity : ComponentActivity() {

    // Variables de estado para recomponer la UI de Compose cuando cambien.
    private var permisosConcedidos by mutableStateOf(false)
    private var rolConcedido by mutableStateOf(false)

    // Launcher para manejar el resultado de solicitar el rol de Call Screening
    private val requestRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            rolConcedido = true
        }
    }

    // Launcher para solicitar múltiples permisos en tiempo de ejecución
    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // Revisamos si TODOS los permisos solicitados fueron concedidos
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            permisosConcedidos = true
            // Tras obtener permisos, procedemos a solicitar el rol de aplicación de filtrado
            solicitarRolCallScreening()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // 1. Inicializar el canal de notificaciones
        NotificationHelper.crearCanalNotificaciones(this)

        // 2. Preparar los permisos a solicitar
        // Ejecutamos la solicitud de permisos tan pronto inicie la Activity
        requestPermissionsLauncher.launch(obtenerPermisosRequeridos())

        // 3. Configurar UI principal
        enableEdgeToEdge()
        setContent {
            BloqueadorSpamTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Llamada al Composable que manejará toda la lógica visual
                    PantallaPrincipal(
                        permisosConcedidos = permisosConcedidos,
                        rolConcedido = rolConcedido,
                        onSolicitarPermisos = { requestPermissionsLauncher.launch(obtenerPermisosRequeridos()) },
                        onSolicitarRol = { solicitarRolCallScreening() }
                    )
                }
            }
        }
    }

    /**
     * Solicita al sistema asignar a la aplicación el rol de filtrado de llamadas.
     * Requerido en Android 10+ (API 29+) para interceptar llamadas.
     */
    private fun solicitarRolCallScreening() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager?.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) == true) {
                if (roleManager.isRoleHeld(RoleManager.ROLE_CALL_SCREENING)) {
                    // Ya tenemos el rol concedido previamente
                    rolConcedido = true
                } else {
                    // Pedimos el rol lanzando el intent explícito
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
                    requestRoleLauncher.launch(intent)
                }
            }
        } else {
            // Para versiones anteriores a Android 10 (aunque tu minSdk es 29, 
            // siempre es buena práctica controlar el flujo por versiones).
            rolConcedido = true 
        }
    }

    private fun obtenerPermisosRequeridos(): Array<String> {
        val permisos = mutableListOf(
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ANSWER_PHONE_CALLS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permisos.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return permisos.toTypedArray()
    }
}