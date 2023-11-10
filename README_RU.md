# A-Little-About-Dagger

Всем привет, эта статья о популярной и многим известной библиотеке Dagger, используемой для внедрения зависимостей (DI).

Статья актуальна не только для Android разработчиков, но и для тех кто собирается использовать готовые решения для DI на Java/Kotlin проектах.

В данном репозитории содержится два варианта [Android приложения](https://github.com/android/codelab-android-dagger) из Google codelab:

1) ветка <code>with_dagger</code> - неизмененный вариант из оригинального репозитория
2) ветка <code>without_dagger</code> - переделанный в стиле сгенерированного кода Dagger без использования библиотеки

Я решил что написание кода который генерирует Dagger является хорошим примером для понимания как он устроен.

Давайте пройдемся по коду.

### Что генерирует Dagger?

Условимся, что термин зависимость эквивалентен термину класс из Java/Kotlin языков.

Начнем с главного Dagger компонента:

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

Dagger компонент (аннотация <b>@Component</b>) это не какая то магическая штука которая должна существовать в единственном экземпляре, на самом деле вы можете создать множество Dagger компонентов с разными модулями и благодаря такой возможности Dagger является неплохим решением для многомодульных проектов.

Ключевая особенность Dagger компонента состоит в том что он является центральной концепцией библиотеки и все крутится вокруг него, это буквально контейнер с зависимостями.

Есть еще понятие дочернего компонента (аннотация <b>@Subcomponent</b>). Это расширенная концепция Dagger компонента, благодаря которой можно вкладывать в основной компонент дочерние. На самом деле вам ничего не мешает создать отдельные Dagger компоненты и ограничивать их жизненный цикл.

Посмотрим какой код будет сгенерирован для вышеуказанного Dagger компонента в упрощенном виде:

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

Разберемся с ключевыми моментами.

##### 1) Dagger использует обертку <b>Provider</b> для отложенного создания экземпляров классов (зависимостей)

Provider это простейший параметризированный интерфейс с отдним методом, возвращающим экземпляр нужного класса (зависимости):

```kotlin
    public interface Provider<T> {
        T get();
    }
```

Dagger не может знать когда вам нужен тот или иной класс и поэтому оборачивает процесс создания конкретного экземпляра в Provider.

##### 2) Dagger модули хранятся в самом компоненте

Код модуля из указанного примера следующий:

```kotlin
    @Module
    abstract class StorageModule {
    
        // Makes Dagger provide SharedPreferencesStorage when a Storage type is requested
        @Binds
        abstract fun provideStorage(storage: SharedPreferencesStorage): Storage
        
    }
```

<b>@Binds</b> аннотация используется для того чтобы связать интерфейс Storage с его реализацией, на самом деле это упрощенная конструкция для:

```kotlin
    @Module
    object StorageModule {
    
        @Provides
        fun provideStorage(context: Context): Storage = SharedPreferencesStorage(context)
        
    }
```

Если зависимости в модуле используются в нескольких классах или зависят от классов из Dagger компонента, то Dagger делает их частью компонента, для которого был прописан модуль. В более простом случае модуль напрямую передается в нужный класс.

##### 3) Для дочерних компонентов (@Subcomponent) и зависимостей отмеченных <b>Scope</b> аннотациями создаются отдельные фабрики, так же как и для основного Dagger компонента

Для дочернего компонента вы сами прописываете интерфейс Factory, который реализует Dagger при кодгенерации:

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

Обратите внимание что для объявления дочернего компонента LoginComponent используется самописная Scope аннотация <b>ActivityScope</b>.

Также в Dagger существуют собственные Scope аннотации например <b>@Singleton</b>:

```kotlin
    @Singleton
    class UserManager @Inject constructor(
        ....
    ) {
    
        ....
        
    }
```

Для первого и второго случая Dagger генерирует специальные фабрики:

```kotlin
    class AppComponentImpl private constructor(private val context: Context) : AppComponent {
    
        ...
    
        override fun loginComponent() = object : LoginComponent.Factory {
            override fun create() = LoginComponentImpl(this@AppComponentImpl)
        }
    
        ...
    
    }
```

Это тоже своего рода Provider обертки, только с одним отличием - фабрики гарантируют создание нового экземпляра зависимости (класса) при каждом вызове create() метода.

##### 4) <b>DoubleCheckProvider</b> обертка для Singleton зависимостей

DoubleCheckProvider это одна из реализаций Provider интерфейса, которая при повторном вызове метода get() возвращает один и тот же экземпляр класса (зависимости). Вы можете называть это что-то вроде Singleton зависимости.

На самом деле в Dagger как такового Singleton не существует, так как вы можете хранить Dagger компонент не в пределах всего приложения, а локально в одном месте и он каждый раз будет пересоздаваться.

Важно придерживаться главной фишки Dagger основанной на компонентах, иначе говоря все что вы пишите привязано к Dagger компоненту, и от него зависит жизненный цикл всех зависимостей.

##### 5) Dagger генерирует специальные обертки для inject() вызовов (Activity, Fragment)

Вернемся к одному из дочерних компонентов и узнаем где происходит вызов <b>inject()</b> в Activity или во Fragment:

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

AppComponentImpl - реализация Dagger компонента, содержит общие зависимости для дочерных компонентов, поэтому RegistrationComponentImpl принимает его в качестве параметра конструктора.

RegistrationViewModel является общей зависимостью для RegistrationActivity, EnterDetailsFragment и TermsAndConditionsFragment, поэтому оборачивается в DoubleCheckProvider, чтобы все имели один и тот же экземпляр вьюмодели.

В моем примере конструкция <b>inject()</b> упрощена и не вынесена в отдельные обертки, которые генерирует Dagger:

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

Вам может показаться что этот код избыточен, но Dagger нуждается в таких обертках также как и в Provider и Factory. Это не человек способный понять, где нужно написать <b>inject()</b>, а где нужно создать зависимость.

### Преимущества Dagger и сфера его использования

Важно отметить, что Dagger генерирует больше кода по сравнению с тем что вы могли написать:

1) Provider обертки на каждую зависимость или отдельные фабрики для дочерних компонентов и Scope аннотированных классов
2) Классы обертки для inject вызовов в случае с Activity или Fragment

Dagger не стоит использовать в небольших проектах, так как это является лишней абстракцией и дополнительным увеличением количества Java/Kotlin классов в проекте. Следует добавить, что Dagger не является простой в понимании библиотекой, это усложняет читаемость кода и делает его неочевидным для тех кто первый раз столкнулся с ним в проекте.

Я считаю, что Dagger это хорошее решение для крупных проектов с многомодульной структурой, где можно его расширить и адаптировать под свои потребности.

### Немного о Hilt

[Hilt](https://developer.android.com/training/dependency-injection/hilt-android) является оберткой над Dagger и по мнению Google хорошим решением для ваших проектов.

Я не мог не обратить внимание на эту библиотеку и решил рассмотреть ее тоже.

Вы можете скачать [codelab-android-hilt](https://github.com/android/codelab-android-hilt) и глянуть своими глазами что генерирует Hilt, я лишь отмечу ключевые вещи:

1) Hilt генерирует в 2+ раза больше кода чем Dagger
2) Если Dagger не трогает ваши Activity и фрагменты, то Hilt генерирует для них суперклассы
3) Сгенерированный код запутанный, сложно читаемый и неочевидный в отличии от Dagger
4) Не решает задачи которые не может решить Dagger

В итоге, большие проекты с многомодульной структурой, как я уже отметил, вполне могут использовать Dagger, у него достаточно понятный сгенерированный код с возможностью адаптации под свои потребности.

Hilt напротив не стоит использовать в больших проектах, так как он добавляет еще один слой абстракции и его кодген сложный и запутанный, что повышает вероятность ошибок.

Что касается небольших проектов, так же как и в случае с Dagger не советую использовать Hilt как DI решение, пишите без лишних и ненужных абстракций, так ваш код не будет привязан к определенной библиотеки и останется понятным другим.

### Заключение

Наслаждайтесь жизнью, пишите хороший и понятный код, и конечно же делитесь знаниями с людьми!

Пожелания и улучшения:

<a href="https://t.me/rwcwuwr"><img src="https://upload.wikimedia.org/wikipedia/commons/thumb/8/82/Telegram_logo.svg/1024px-Telegram_logo.svg.png" width=160 /></a>

