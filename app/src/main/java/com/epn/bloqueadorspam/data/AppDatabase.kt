package com.epn.bloqueadorspam.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Clase principal de la base de datos de Room.
 * Sirve como punto de acceso principal a los datos persistidos de la aplicación.
 */
@Database(
    entities = [NumeroBloqueado::class], // Define las entidades (tablas) de la base de datos
    version = 1,                         // Versión del esquema (útil para migraciones futuras)
    exportSchema = false                 // Deshabilita la exportación del esquema a un archivo
)
abstract class AppDatabase : RoomDatabase() {

    // Método abstracto para acceder a las operaciones definidas en NumeroBloqueadoDao
    abstract fun numeroBloqueadoDao(): NumeroBloqueadoDao

    companion object {
        // @Volatile asegura que los cambios a INSTANCE sean visibles inmediatamente a todos los hilos
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Obtiene la instancia única de la base de datos (patrón Singleton).
         * Si no existe, la inicializa de forma segura mediante synchronized (thread-safe).
         */
        fun getInstance(context: Context): AppDatabase {
            // Retorna la instancia existente, si es null crea una nueva de forma sincronizada
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "base_datos_spam" // Nombre del archivo de la base de datos local
                ).build()
                INSTANCE = instance
                instance // Retorna la instancia recién creada
            }
        }
    }
}
