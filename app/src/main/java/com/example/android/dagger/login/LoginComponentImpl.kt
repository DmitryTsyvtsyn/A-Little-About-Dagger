package com.example.android.dagger.login

import com.example.android.dagger.core.AppComponentImpl

class LoginComponentImpl(private val appComponent: AppComponentImpl) : LoginComponent {

    override fun inject(activity: LoginActivity) {
        activity.loginViewModel = LoginViewModel(appComponent.userManagerProvider.get())
    }

}