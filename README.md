# A-Little-About-Dagger

Всем привет, эта статья о популярной и многим известной библиотеке Dagger, используемой для внедрения зависимостей (DI).

Статья актуальна не только для Android разработчиков, но и для тех кто собирается использовать готовые решения для DI на Java/Kotlin проектах.

В данном репозитории содержится два варианта [Android приложения](https://github.com/android/codelab-android-dagger) из Google codelab:

1) ветка <code>with_dagger</code> - Неизмененный вариант из codelab-android-dagger репозитория
2) ветка <code>without_dagger</code> - Переделанный в стиле сгенерированного кода Dagger без использования библиотеки

Я решил что написание кода который генерирует Dagger является хорошим примером для понимания как он устроен.

Давайте пройдемся по коду.

### Что генерирует Dagger?

Условимся, что термин зависимость эквивалентен термину класс из Java/Kotlin языков.

Начнем с главного Dagger компонента:

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

Упрощенный код который будет сгенерирован для него:

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

Обратите внимание на несколько вещей:

1) Dagger использует обертку <b>Provider</b> для отложенного создания экземпляров зависимостей
2) Dagger модули хранятся в самом компоненте (<b>SharedPreferencesStorage</b>)
3) Для <b>@Subcomponent</b> элементов и зависимостей отмеченных <b>Scope</b> аннотациями создаются отдельные фабрики, так же как и для основного Dagger компонента
4) <b>DoubleCheckProvider</b> обертка которая позволяет сделать Singleton зависимости в пределах Dagger компонента

Давайте глянем на любой <b>Subcomponent</b> где происходит inject в Activity или Fragment:

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

<b>AppComponent</b> содержит общие зависимости для дочерных <b>Subcomponent</b> элементов, поэтому <b>RegistrationComponentImpl</b> принимает его в качестве параметра конструктора.

<b>RegistrationViewModel</b> является общей зависимостью для <b>RegistrationActivity</b>, <b>EnterDetailsFragment</b> и <b>TermsAndConditionsFragment</b>, поэтому оборачивается в <b>DoubleCheckProvider</b>, чтобы каждый имел один и тот же экземпляр вьюмодели.

Конструкция inject на радость достаточно простая и очевидная за исключением того что Dagger выносит ее в отдельно сгенерированные обертки.

Важно отметить, что Dagger генерирует больше кода по сравнению с тем что вы могли написать:

1) <b>Provider</b> обертки на каждую зависимость или отдельные фабрики для <b>Subcomponent</b> элементов и <b>Scope</b> аннотированных зависимостей
2) Классы обертки для inject конструкции в случае с Activity и Fragment

Отсюда следует что нет смысла использовать Dagger в небольших проектах, так как это является лишней абстракцией и дополнительным увеличением количества Java/Kotlin классов в проекте.

### Преимущества Dagger и сфера его использования

Я считаю, что Dagger это хорошее решение для средних, а по большей части крупных проектов с многомодульной структурой, которое можно расширить и легко доработать под свои нужды.

В случае с небольшими проектами Dagger только привносит лишнюю зависимость и усложняет читаемость кода, так как Dagger не является простой библиотекой, которую можно легко понять интуитивно.

### Немного о Hilt

[Hilt](https://developer.android.com/training/dependency-injection/hilt-android) является оберткой над Dagger и по мнению Google хорошим решением для ваших проектов.

Я не мог не обратить внимание на эту библиотеку и решил рассмотреть ее тоже. 

Вы можете скачать [codelab-android-hilt](https://github.com/android/codelab-android-hilt) и глянуть своими глазами что генерирует Hilt, я лишь отмечу ключевые вещи:

1) Hilt генерирует в 2+ раза больше кода чем Dagger
2) Если Dagger не трогает ваши Activity и фрагменты, то Hilt генерирует для них суперклассы
3) Сгенерированный код запутанный, сложно читаемый и неочевидный в отличии от Dagger
4) Не решает задачи которые не может решить Dagger

В итоге, средние и большие проекты с многомодульной структурой, как я уже отметил, вполне могут использовать Dagger, у него достаточно понятный сгенерированный код и его можно адаптировать под свои потребности.

Hilt напротив не стоит использовать в средних, тем более в больших проектах, так как он добавляет лишнюю абстракцию и делать кодген сложным и запутанным, что повышает вероятность ошибок.

Что касается небольших проектов, так же как и в случае с Dagger не советую использовать Hilt как DI решение, пишите без лишних и ненужных абстракций, так ваш код не будет привязан к определенной библиотеки и будет понятным другим.

### Заключение

Наслаждайтесь жизнью, пишите хороший и понятный код, и конечно же делитесь знаниями с людьми!

Пожелания и улучшения:

<a href="https://t.me/rwcwuwr"><img src="https://github.com/evitwilly/A-Little-About-Dagger/assets/40917658/41dbc75f-b3d4-4ef7-9096-f0ad76dfc51b" width="155" /></a>

