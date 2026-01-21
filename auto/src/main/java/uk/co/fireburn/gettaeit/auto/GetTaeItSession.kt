package uk.co.fireburn.gettaeit.auto

import android.content.Intent
import androidx.car.app.Screen
import androidx.car.app.Session
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import dagger.hilt.android.EntryPointAccessors
import uk.co.fireburn.gettaeit.auto.di.ViewModelFactoryProvider

class GetTaeItSession : Session(), ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()

    private val viewModel: AutoViewModel

    init {
        val factory = EntryPointAccessors.fromApplication(
            carContext,
            ViewModelFactoryProvider::class.java
        ).autoViewModelFactory()
        viewModel = factory.create(AutoViewModel::class.java)

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                viewModelStore.clear()
            }
        })
    }


    override fun onCreateScreen(intent: Intent): Screen {
        return GetTaeItScreen(carContext, viewModel)
    }
}
