package com.example.android.dagger.login

interface LoginComponent {

    interface Factory {
        fun create(): LoginComponent
    }

    // Classes that can be injected by this Component
    fun inject(activity: LoginActivity)
}
