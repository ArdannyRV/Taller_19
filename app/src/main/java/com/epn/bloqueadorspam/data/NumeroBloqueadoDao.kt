package com.epn.bloqueadorspam.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) para la entidad [NumeroBloqueado].
 * Contiene los métodos para interactuar con la tabla "numeros_bloqueados".
 */
@Dao
interface NumeroBloqueadoDao {

    // Inserta un nuevo número. Si el número ya existe, lo reemplaza (actualiza).
    // Se usa 'suspend' para que se ejecute en una corrutina y no bloquee el hilo principal.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertar(numero: NumeroBloqueado)

    // Elimina un registro de la base de datos pasando el objeto completo.
    @Delete
    suspend fun eliminar(numero: NumeroBloqueado)

    // Elimina un número bloqueado proporcionando directamente el String del número.
    @Query("DELETE FROM numeros_bloqueados WHERE numero = :numero")
    suspend fun eliminarPorNumero(numero: String)

    // Obtiene todos los números bloqueados, ordenados del más reciente al más antiguo.
    // Retorna un Flow, lo que permite observar cambios en la base de datos en tiempo real (útil con Jetpack Compose).
    @Query("SELECT * FROM numeros_bloqueados ORDER BY fechaAgregado DESC")
    fun obtenerTodos(): Flow<List<NumeroBloqueado>>

    // Verifica de forma rápida si un número de teléfono específico está en la base de datos.
    @Query("SELECT EXISTS(SELECT 1 FROM numeros_bloqueados WHERE numero = :numero)")
    suspend fun existe(numero: String): Boolean
}
