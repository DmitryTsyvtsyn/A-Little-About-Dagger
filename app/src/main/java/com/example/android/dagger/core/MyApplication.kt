package com.example.android.dagger.core

import android.app.Application

class MyApplication : Application() {

    // Instance of the AppComponent that will be used by all the Activities in the project
    val appComponent: AppComponent by lazy { AppComponentImpl.Factory().create(applicationContext) }

}
