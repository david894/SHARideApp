package com.kxxr.sharide.logic

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltAndroidApp
class NetworkViewModel @Inject constructor(application: Application) : AndroidViewModel(application) {
    private val networkObserver = NetworkObserver(application)

    val isConnected: StateFlow<Boolean> = networkObserver.isConnected

    init {
        networkObserver.startObserving()
    }

    override fun onCleared() {
        super.onCleared()
        networkObserver.stopObserving()
    }
}

annotation class HiltViewModel
