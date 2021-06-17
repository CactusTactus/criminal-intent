package com.example.criminalintent.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.criminalintent.model.Crime

@Database(entities = [Crime::class], version = 3, exportSchema = false)
@TypeConverters(CrimeTypeConverters::class)
abstract class CrimeDatabase : RoomDatabase() {
    abstract fun crimeDao(): CrimeDao
}

val migration_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        val query = "ALTER TABLE Crime ADD COLUMN suspect TEXT NOT NULL DEFAULT ''"
        database.execSQL(query)
    }
}
val migration_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        val query = "ALTER TABLE Crime ADD COLUMN phoneNumber TEXT NOT NULL DEFAULT ''"
        database.execSQL(query)
    }
}