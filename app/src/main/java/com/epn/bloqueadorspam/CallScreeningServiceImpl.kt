package com.epn.bloqueadorspam

import android.telecom.Call
import android.telecom.CallScreeningService
import android.util.Log
import com.epn.bloqueadorspam.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Servicio encargado de interceptar y evaluar las llamadas entrantes antes de que
 * suenen en el dispositivo. Si el número está en la lista negra, la llamada es rechazada.
 */
class CallScreeningServiceImpl : CallScreeningService() {

    override fun onScreenCall(callDetails: Call.Details) {
        // Extraemos el número entrante desde el handle de los detalles de la llamada
        // El formato típicamente viene como "tel:+593987654321"
        val rawNumber = callDetails.handle?.schemeSpecificPart ?: ""
        
        // Normalizamos: ignoramos prefijo de país y dejamos últimos 9-10 dígitos
        val normalizedNumber = com.epn.bloqueadorspam.utils.normalizarNumeroTelefono(rawNumber)

        // Validación de seguridad: si no pudimos extraer un número válido, 
        // dejamos pasar la llamada respondiendo con un CallResponse vacío.
        if (normalizedNumber.isEmpty()) {
            respondToCall(callDetails, CallResponse.Builder().build())
            return
        }

        // Utilizamos un CoroutineScope con Dispatchers.IO para realizar la consulta 
        // a la base de datos de Room en un hilo en segundo plano (background thread).
        // Se utiliza launch y try/catch para asegurar que siempre haya una respuesta.
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Instanciamos los DAOs accediendo a la base de datos
                val db = AppDatabase.getInstance(applicationContext)
                val numeroBloqueadoDao = db.numeroBloqueadoDao()
                val llamadaBloqueadaDao = db.llamadaBloqueadaDao()
                
                // Consultamos si el número existe en la base de datos local
                val estaBloqueado = numeroBloqueadoDao.existe(normalizedNumber)

                if (estaBloqueado) {
                    Log.d("CallScreening", "Número bloqueado interceptado: $normalizedNumber")
                    
                    // Si está en la lista negra, preparamos la respuesta de rechazo
                    val response = CallResponse.Builder()
                        .setDisallowCall(true)      // Impide que la llamada entre
                        .setRejectCall(true)        // Rechaza la llamada (cuelga al remitente)
                        .setSkipCallLog(false)      // (Opcional) true si no quieres que quede en el historial de llamadas
                        .setSkipNotification(true)  // Evita que el sistema muestre la notificación de llamada perdida normal
                        .build()

                    // Enviamos la instrucción al sistema para rechazarla
                    respondToCall(callDetails, response)
                    
                    // Guardar en el historial de llamadas bloqueadas
                    llamadaBloqueadaDao.insertar(com.epn.bloqueadorspam.data.LlamadaBloqueada(numero = normalizedNumber))

                    // Disparamos una notificación local indicando al usuario que la app bloqueó la llamada.
                    NotificationHelper.mostrarNotificacionBloqueo(applicationContext, normalizedNumber)
                } else {
                    // Si no está bloqueado, lo dejamos pasar construyendo un CallResponse por defecto
                    respondToCall(callDetails, CallResponse.Builder().build())
                }

            } catch (e: Exception) {
                Log.e("CallScreening", "Error interno al procesar la llamada entrante", e)
                // REGLA DE ORO: Si ocurre una excepción (ej. fallo de base de datos), 
                // NUNCA dejamos colgada la llamada, le permitimos pasar como medida de seguridad.
                respondToCall(callDetails, CallResponse.Builder().build())
            }
        }
    }
}
