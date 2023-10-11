package com.example.android.dagger.core

import android.content.Context
import com.example.android.dagger.login.LoginComponent
import com.example.android.dagger.login.LoginComponentImpl
import com.example.android.dagger.registration.RegistrationComponent
import com.example.android.dagger.registration.RegistrationComponentImpl
import com.example.android.dagger.storage.SharedPreferencesStorage
import com.example.android.dagger.user.UserComponent
import com.example.android.dagger.user.UserComponentImpl
import com.example.android.dagger.user.UserManager

class AppComponentImpl private constructor(private val context: Context) : AppComponent {

    private val sharedStorageProvider = Provider { SharedPreferencesStorage(context) }

    val userManagerProvider = DoubleCheckProvider {
        val userComponentFactory = object : UserComponent.Factory {
            override fun create(): UserComponent = UserComponentImpl(this@AppComponentImpl)
        }
        UserManager(sharedStorageProvider.get(), userComponentFactory)
    }

    override fun loginComponent() = object : LoginComponent.Factory {
        override fun create() = LoginComponentImpl(this@AppComponentImpl)
    }

    override fun registrationComponent() = object : RegistrationComponent.Factory {
        override fun create() = RegistrationComponentImpl(this@AppComponentImpl)
    }

    override fun userManager(): UserManager = userManagerProvider.get()

    class Factory : AppComponent.Factory {
        override fun create(context: Context) = AppComponentImpl(context)
    }

}