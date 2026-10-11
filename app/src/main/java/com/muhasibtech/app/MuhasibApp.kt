package com.muhasibtech.app

import android.app.Application
import com.muhasibtech.app.data.local.AppDatabase
import com.muhasibtech.app.data.repository.AppRepository
import com.muhasibtech.app.data.seed.DefaultDataSeeder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MuhasibApp : Application() {

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    lateinit var database: AppDatabase
        private set

    lateinit var repository: AppRepository
        private set

    override fun onCreate() {
        super.onCreate()

        database = AppDatabase.getInstance(this)
        repository = AppRepository.getInstance(database)

        applicationScope.launch {
            DefaultDataSeeder.seed(database)
        }
    }
}
