package com.epn.bloqueadorspam.data

/**
 * Data class que representa una llamada extraída del historial real del teléfono (CallLog).
 */
data class LlamadaTelefono(
    val numero: String,
    val fecha: Long,
    val tipo: Int,       // CallLog.Calls.INCOMING_TYPE, OUTGOING_TYPE, MISSED_TYPE, REJECTED_TYPE
    val duracion: String
)
