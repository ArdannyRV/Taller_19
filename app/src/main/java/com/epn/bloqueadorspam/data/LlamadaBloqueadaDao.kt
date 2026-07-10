package com.epn.bloqueadorspam.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LlamadaBloqueadaDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(llamada: LlamadaBloqueada): Long

    @Query("SELECT * FROM historial_bloqueos ORDER BY fecha DESC")
    fun obtenerHistorial(): Flow<List<LlamadaBloqueada>>

    @Query("DELETE FROM historial_bloqueos WHERE numero = :numero")
    suspend fun eliminarHistorialPorNumero(numero: String): Int
    
    @Query("SELECT COUNT(*) FROM historial_bloqueos")
    fun contarBloqueos(): Flow<Int>
}
