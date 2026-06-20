package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        MatchHistoryEntity::class,
        PlayerStatsEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class WordTacDatabase : RoomDatabase() {

    abstract fun wordTacDao(): WordTacDao

    companion object {
        @Volatile
        private var INSTANCE: WordTacDatabase? = null

        fun getDatabase(context: Context): WordTacDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WordTacDatabase::class.java,
                    "wordtac_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
