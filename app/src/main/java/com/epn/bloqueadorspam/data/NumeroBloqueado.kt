package com.epn.bloqueadorspam.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad que representa la tabla "numeros_bloqueados" en la base de datos local.
 * Cada instancia de esta clase corresponderá a una fila de la tabla.
 */
@Entity(tableName = "numeros_bloqueados")
data class NumeroBloqueado(
    // El número de teléfono sirve como identificador único (clave primaria).
    // Se asume que el número ya ha sido normalizado (sin espacios ni caracteres especiales).
    @PrimaryKey
    val numero: String,
    
    // Fecha y hora en la que se agregó el número a la lista de bloqueo.
    // Útil para mostrar un historial o para ordenar los registros.
    val fechaAgregado: Long = System.currentTimeMillis()
)
