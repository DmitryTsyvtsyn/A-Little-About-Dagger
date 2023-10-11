package com.example.android.dagger.user

import com.example.android.dagger.core.AppComponentImpl
import com.example.android.dagger.core.DoubleCheckProvider
import com.example.android.dagger.main.MainActivity
import com.example.android.dagger.main.MainViewModel
import com.example.android.dagger.settings.SettingsActivity
import com.example.android.dagger.settings.SettingsViewModel

class UserComponentImpl(private val appComponent: AppComponentImpl) : UserComponent {

    private val userDataRepository = DoubleCheckProvider {
        UserDataRepository(appComponent.userManagerProvider.get())
    }

    override fun inject(activity: MainActivity) {
        activity.mainViewModel = MainViewModel(userDataRepository.get())
    }

    override fun inject(activity: SettingsActivity) {
        activity.settingsViewModel = SettingsViewModel(userDataRepository.get(), appComponent.userManagerProvider.get())
    }

}