# A-Little-About-Dagger

Hello everyone, this article is about the popular and well-known Dagger library used for dependency injection (DI).

The article is relevant not only for Android developers, but also for those who are going to use ready-made solutions for DI in Java/Kotlin projects.

This repository contains two versions of [Android application](https://github.com/android/codelab-android-dagger) from Google codelab:

1) branch <code>with_dagger</code> - unmodified version from the original repository
2) branch <code>without_dagger</code> - remade in the style of Dagger generated code without using the library

I decided that writing the code that Dagger generates is a good example for understanding how it works.

Let's walk through the code.

### What does Dagger generate?

Let's agree that the term dependency is equivalent to the term class from the Java/Kotlin languages.

So, let's start with the main Dagger component:

```kotlin
    @Singleton
    // Definition of a Dagger component that adds info from the different modules to the graph
    @Component(modules = [StorageModule::class, AppSubcomponents::class])
    interface AppComponent {
    
        // Factory to create instances of the AppComponent
        @Component.Factory
        interface Factory {
            // With @BindsInstance, the Context passed in will be available in the graph
            fun create(@BindsInstance context: Context): AppComponent
        }
    
        // Types that can be retrieved from the graph
        fun registrationComponent(): RegistrationComponent.Factory
        fun loginComponent(): LoginComponent.Factory
        fun userManager(): UserManager
    }
```
A Dagger component (annotation <b>@Component</b>) is not some kind of magical thing that should exist in a single copy. In fact you can create many Dagger components with different modules and thanks to this feature, Dagger is a good solution for multi-module projects.

The key feature of the Dagger component is that it's the central concept of the library and everything revolves around it, it's literally a container with dependencies.

There is also the concept of a child component (annotation <b>@Subcomponent</b>). This is an extended concept of a Dagger component, thanks to which you can nest child ones in the parent component. In fact there is nothing stopping you from creating separate Dagger components and limiting their lifecycle.

Let's see what code will be generated for the above Dagger component in a simplified form:

```kotlin
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
```

So, let's look at the key points.

##### 1) Dagger uses the <b>Provider</b> wrapper to lazy instantiate classes (dependencies)

Provider is the simplest parameterized interface with a separate method that returns an instance of the desired class (dependency):

```kotlin
    public interface Provider<T> {
        T get();
    }
```

Dagger cannot know when you need a particular class and therefore wraps the process of creating a specific instance in the Provider.

##### 2) Dagger modules are stored in the component itself

The module code from the above example is as follows:

```kotlin
    @Module
    abstract class StorageModule {
    
        // Makes Dagger provide SharedPreferencesStorage when a Storage type is requested
        @Binds
        abstract fun provideStorage(storage: SharedPreferencesStorage): Storage
        
    }

```

The <b>@Binds</b> annotation is used to bind the Storage interface to its implementation, in fact it's a simplified construct for:

```kotlin
    @Module
    object StorageModule {
    
        @Provides
        fun provideStorage(context: Context): Storage = SharedPreferencesStorage(context)
        
    }
```

If dependencies in a module are used in several classes or depend on classes from a Dagger component then Dagger makes them part of the component for which the module was written. In a simpler case the module is directly passed to the desired class.

##### 3) Separate factories are created for child components (@Subcomponent) and dependencies marked with <b>Scope</b> annotations, just like for the Dagger component

For the child component you write the Factory interface which Dagger implements during code generation:

```kotlin
    @ActivityScope
    // Definition of a Dagger subcomponent
    @Subcomponent
    interface LoginComponent {
    
        // Factory to create instances of LoginComponent
        @Subcomponent.Factory
        interface Factory {
            fun create(): LoginComponent
        }
    
        // Classes that can be injected by this Component
        fun inject(activity: LoginActivity)
    }
```

Please note that the self-written Scope annotation <b>ActivityScope</b> is used to declare the LoginComponent child component.

Dagger also has its own Scope annotations, for example <b>@Singleton</b>:

```kotlin
    @Singleton
    class UserManager @Inject constructor(
        ....
    ) {
    
        ....
        
    }
```

For the first and second cases Dagger generates special factories:

```kotlin
    class AppComponentImpl private constructor(private val context: Context) : AppComponent {
    
        ...
    
        override fun loginComponent() = object : LoginComponent.Factory {
            override fun create() = LoginComponentImpl(this@AppComponentImpl)
        }
    
        ...
    
    }
```

This is also a kind of Provider wrapper with only one difference - factories guarantee the creation of a new instance of the dependency (class) every time the create() method is called.

##### 4) <b>DoubleCheckProvider</b> wrapper for Singleton dependencies

DoubleCheckProvider is one of the implementations of the Provider interface which when the get() method is called repeatedly returns the same instance of the class (dependency). You can call it something like Singleton dependency.

In fact Singleton as such does not exist in Dagger since you can store a Dagger component not within the entire application, but locally in one place and it will be recreated every time.

It's important to adhere to the main feature of Dagger being component-based, in other words everything you write is tied to the Dagger component and the lifecycle of all dependencies depends on it.

##### 5) Dagger generates special wrappers for inject() calls (Activity, Fragment)

Let's go back to one of the child components and find out where the <b>inject()</b> call occurs in the Activity and in the Fragment:

```kotlin
    class RegistrationComponentImpl(private val appComponent: AppComponentImpl) : RegistrationComponent {
    
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
```

AppComponentImpl is a Dagger component implementation that contains common dependencies for child components, so RegistrationComponentImpl takes it as a constructor parameter.

RegistrationViewModel is a common dependency for RegistrationActivity, EnterDetailsFragment and TermsAndConditionsFragment, so it's wrapped in a DoubleCheckProvider so that they all have the same instance of the viewmodel.

In my example the <b>inject()</b> construct is simplified and not included in separate wrappers that Dagger generates:

```kotlin
    public final class RegistrationActivity_MembersInjector implements MembersInjector<RegistrationActivity> {
      private final Provider<RegistrationViewModel> registrationViewModelProvider;
    
      public RegistrationActivity_MembersInjector(Provider<RegistrationViewModel> registrationViewModelProvider) {
        this.registrationViewModelProvider = registrationViewModelProvider;
      }
    
      public static MembersInjector<RegistrationActivity> create(Provider<RegistrationViewModel> registrationViewModelProvider) {
        return new RegistrationActivity_MembersInjector(registrationViewModelProvider);
      }
    
      @Override
      public void injectMembers(RegistrationActivity instance) {
        injectRegistrationViewModel(instance, registrationViewModelProvider.get());
      }
    
      @InjectedFieldSignature("com.example.android.dagger.registration.RegistrationActivity.registrationViewModel")
      public static void injectRegistrationViewModel(RegistrationActivity instance, RegistrationViewModel registrationViewModel) {
        instance.registrationViewModel = registrationViewModel;
      }
    } 
```

You may think this code is redundant, but Dagger needs these wrappers just like Provider and Factory. This is not a person who can understand where to write <b>inject()</b> and where to create a dependency.

### Advantages of Dagger and scope of its use

It's important to note that Dagger generates more code than you might write:

1) Provider wrappers per dependency or separate factories for child components and Scope annotated classes
2) Wrapper classes for inject calls in the case of Activity or Fragment

Dagger should't be used in small projects as it's an unnecessary abstraction and an additional increase in the number of Java/Kotlin classes in the project. It should be added that Dagger is not an easy-to-understand library. This complicates the readability of the code and makes it not obvious to those who first encountered it in a project.

I believe that Dagger is a good solution for large projects with a multi-module structure where you can expand it and adapt it to your needs.

### A little about Hilt

[Hilt](https://developer.android.com/training/dependency-injection/hilt-android) is a wrapper around Dagger and according to Google a good solution for your projects.

I couldn't help but pay attention to this library and decided to consider it too.

You can download [codelab-android-hilt](https://github.com/android/codelab-android-hilt) and see with your own eyes what Hilt generates. I’ll just note the key things:

1) Hilt generates 2+ times more code than Dagger
2) If Dagger does not touch your Activities and fragments, then Hilt generates superclasses for them
3) The generated code is confusing, difficult to read and not obvious, unlike Dagger
4) Doesn't solve problems that Dagger can't solve

As a result large projects with a multi-module structure as I already noted can easily use Dagger. It has a fairly clear generated code with the ability to adapt to your needs.

Hilt on the contrary should not be used in large projects since it adds another layer of abstraction and it's codegen is complex and confusing which increases the likelihood of errors.

As for small projects just as in the case of Dagger, I do not recommend using Hilt as a DI solution, write without unnecessary abstractions so your code will not be tied to a specific library and will remain understandable to others.

[RUSSIAN VERSION](README_RU.md)

### Conclusion

Enjoy life, write good and understandable code, and of course share your knowledge with people!

Wishes and improvements:

<a href="https://t.me/rwcwuwr"><img src="https://upload.wikimedia.org/wikipedia/commons/thumb/8/82/Telegram_logo.svg/1024px-Telegram_logo.svg.png" width=160 /></a>
