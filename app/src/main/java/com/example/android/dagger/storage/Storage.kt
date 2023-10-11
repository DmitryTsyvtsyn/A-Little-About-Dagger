package com.example.android.dagger.storage

interface Storage {
    fun setString(key: String, value: String)
    fun getString(key: String): String
}
