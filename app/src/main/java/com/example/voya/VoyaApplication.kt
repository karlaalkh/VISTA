package com.example.voya

import android.app.Application
import com.example.voya.di.AppContainer

class VoyaApplication : Application() {
    val container: AppContainer by lazy { AppContainer() }
}
