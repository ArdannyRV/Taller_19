package com.epn.bloqueadorspam.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "historial_bloqueos")
data class LlamadaBloqueada(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val numero: String,
    val fecha: Long = System.currentTimeMillis()
)
