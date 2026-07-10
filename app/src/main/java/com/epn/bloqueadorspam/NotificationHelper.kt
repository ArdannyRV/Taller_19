package com.epn.bloqueadorspam

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/**
 * Objeto auxiliar (Singleton) para manejar la creación de canales de notificación
 * y la emisión de notificaciones relacionadas al bloqueo de llamadas.
 */
object NotificationHelper {

    // Identificador único para el canal de notificaciones
    const val CHANNEL_ID = "canal_bloqueos"

    /**
     * Crea el canal de notificaciones requerido para dispositivos con Android 8.0 (API 26) o superior.
     * Deberá ser llamado al iniciar la app (por ejemplo, desde MainActivity).
     */
    fun crearCanalNotificaciones(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Llamadas bloqueadas"
            val descriptionText = "Notificaciones para informar sobre llamadas de spam bloqueadas"
            val importance = NotificationManager.IMPORTANCE_HIGH
            
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            
            // Registramos el canal con el sistema
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Muestra una notificación alertando que un número específico fue bloqueado.
     * Se usa @SuppressLint porque nosotros mismos estamos realizando la validación manual
     * del permiso en Android 13+.
     */
    @SuppressLint("MissingPermission")
    fun mostrarNotificacionBloqueo(context: Context, numero: String) {
        // En Android 13 (Tiramisu - API 33) en adelante, es imperativo tener el permiso concedido
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permisoConcedido = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            )
            
            if (permisoConcedido != PackageManager.PERMISSION_GRANTED) {
                // Si el permiso no está concedido, salimos silenciosamente 
                // para evitar que la aplicación crashee.
                return
            }
        }

        // Construcción de la notificación
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Icono del sistema como fallback
            .setContentTitle("Llamada bloqueada")
            .setContentText("Se bloqueó una llamada de $numero")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true) // La notificación desaparece cuando el usuario la toca

        // Lanzamos la notificación. Usamos el tiempo actual como ID para que 
        // múltiples llamadas bloqueadas generen notificaciones independientes.
        with(NotificationManagerCompat.from(context)) {
            val notificationId = System.currentTimeMillis().toInt()
            notify(notificationId, builder.build())
        }
    }
}
