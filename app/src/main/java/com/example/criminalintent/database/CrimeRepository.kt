package com.example.criminalintent.database

import android.content.Context
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.room.Room
import com.example.criminalintent.model.Crime
import java.io.File
import java.lang.IllegalStateException
import java.util.UUID
import java.util.concurrent.Executor
import java.util.concurrent.Executors

private const val DATABASE_NAME = "crime-database"

class CrimeRepository private constructor(context: Context) {
    private val database: CrimeDatabase = Room
        .databaseBuilder(context.applicationContext, CrimeDatabase::class.java, DATABASE_NAME)
        .addMigrations(migration_1_2, migration_2_3)
        .build()

    private val crimeDao = database.crimeDao()
    private val executor = Executors.newSingleThreadExecutor()
    private val filesDir = context.applicationContext.filesDir

    fun addCrime(crime: Crime) {
        executor.execute {
            crimeDao.addCrime(crime)
        }
    }

    fun updateCrime(crime: Crime) {
        executor.execute {
            crimeDao.updateCrime(crime)
        }
    }

    fun getCrimes(): LiveData<List<Crime>> = crimeDao.getCrimes()

    fun getCrime(id: UUID): LiveData<Crime?> = crimeDao.getCrime(id)

    fun getPhotoFile(crime: Crime) = File(filesDir, crime.photoFileName)

    companion object {
        private var instance: CrimeRepository? = null

        fun initialize(context: Context) {
            instance = CrimeRepository(context)
            Log.d("CrimeRepository", "Database: ${instance?.database ?: "NULL"}")
        }

        fun get(): CrimeRepository {
            return instance ?: throw IllegalStateException("CrimeRepository must be initialized.")
        }
    }
}