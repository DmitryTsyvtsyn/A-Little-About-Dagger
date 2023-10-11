package com.example.android.dagger.user

import com.example.android.dagger.main.MainActivity
import com.example.android.dagger.settings.SettingsActivity

interface UserComponent {

    interface Factory {
        fun create(): UserComponent
    }

    // Classes that can be injected by this Component
    fun inject(activity: MainActivity)
    fun inject(activity: SettingsActivity)
}
