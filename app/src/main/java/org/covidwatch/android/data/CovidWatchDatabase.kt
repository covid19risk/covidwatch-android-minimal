package org.covidwatch.android.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Database(entities = [TemporaryContactNumber::class, SignedReport::class], version = 6, exportSchema = false)
abstract class CovidWatchDatabase : RoomDatabase() {

    abstract fun temporaryContactNumberDAO(): TemporaryContactNumberDAO
    abstract fun signedReportDAO(): SignedReportDAO

    companion object {
        private const val NUMBER_OF_THREADS = 4
        val databaseWriteExecutor: ExecutorService =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS)

        @Volatile
        private var INSTANCE: CovidWatchDatabase? = null

        fun getInstance(context: Context): CovidWatchDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }

        private fun buildDatabase(context: Context) =
            Room.databaseBuilder(
                context.applicationContext,
                CovidWatchDatabase::class.java, "covidwatch.db"
            ).fallbackToDestructiveMigration().build()
    }
}