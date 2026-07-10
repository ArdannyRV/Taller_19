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
    entities = [NumeroBloqueado::class, LlamadaBloqueada::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun numeroBloqueadoDao(): NumeroBloqueadoDao
    abstract fun llamadaBloqueadaDao(): LlamadaBloqueadaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "base_datos_spam"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
