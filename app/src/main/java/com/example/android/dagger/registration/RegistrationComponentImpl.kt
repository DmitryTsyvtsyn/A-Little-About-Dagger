package com.example.android.dagger.registration

import com.example.android.dagger.core.AppComponentImpl
import com.example.android.dagger.core.DoubleCheckProvider
import com.example.android.dagger.registration.enterdetails.EnterDetailsFragment
import com.example.android.dagger.registration.enterdetails.EnterDetailsViewModel
import com.example.android.dagger.registration.termsandconditions.TermsAndConditionsFragment

class RegistrationComponentImpl(private val appComponent: AppComponentImpl) :
    RegistrationComponent {

    private val registrationViewModelProvider = DoubleCheckProvider {
        RegistrationViewModel(appComponent.userManagerProvider.get())
    }

    override fun inject(activity: RegistrationActivity) {
        activity.registrationViewModel = registrationViewModelProvider.get()
    }

    override fun inject(fragment: EnterDetailsFragment) {
        fragment.registrationViewModel = registrationViewModelProvider.get()
        fragment.enterDetailsViewModel = EnterDetailsViewModel()
    }

    override fun inject(fragment: TermsAndConditionsFragment) {
        fragment.registrationViewModel = registrationViewModelProvider.get()
    }

}