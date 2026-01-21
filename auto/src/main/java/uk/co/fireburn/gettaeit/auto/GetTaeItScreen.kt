package uk.co.fireburn.gettaeit.auto

import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.model.Pane
import androidx.car.app.model.PaneTemplate
import androidx.car.app.model.Row
import androidx.car.app.model.Template

class GetTaeItScreen(carContext: CarContext) : Screen(carContext) {
    override fun onGetTemplate(): Template {
        val row = Row.Builder()
            .setTitle("Hello Android Auto!")
            .build()

        return PaneTemplate.Builder(
            Pane.Builder()
                .addRow(row)
                .build()
        )
            .setHeaderAction(androidx.car.app.model.Action.APP_ICON)
            .build()
    }
}
