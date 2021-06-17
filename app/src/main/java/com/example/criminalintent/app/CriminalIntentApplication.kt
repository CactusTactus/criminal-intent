package com.example.criminalintent.app

import android.app.Application
import com.example.criminalintent.database.CrimeRepository

class CriminalIntentApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        CrimeRepository.initialize(this)
    }
}