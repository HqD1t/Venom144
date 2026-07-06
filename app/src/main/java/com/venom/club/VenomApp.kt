package com.venom.club

import android.app.Application
import com.google.firebase.FirebaseApp

class VenomApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
