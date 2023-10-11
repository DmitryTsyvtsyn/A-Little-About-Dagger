package com.example.android.dagger.user

import kotlin.random.Random

/**
 * UserDataRepository contains user-specific data such as username and unread notifications.
 */
class UserDataRepository constructor(private val userManager: UserManager) {

    val username: String
        get() = userManager.username

    var unreadNotifications: Int

    init {
        unreadNotifications = randomInt()
    }

    fun refreshUnreadNotifications() {
        unreadNotifications = randomInt()
    }

    private fun randomInt(): Int = Random.nextInt(until = 100)

}


