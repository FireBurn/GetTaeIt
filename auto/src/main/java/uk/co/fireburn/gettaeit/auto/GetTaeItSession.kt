package uk.co.fireburn.gettaeit.auto

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session

class GetTaeItSession : Session() {
    override fun onCreateScreen(intent: Intent): Screen {
        return GetTaeItScreen(carContext)
    }
}
