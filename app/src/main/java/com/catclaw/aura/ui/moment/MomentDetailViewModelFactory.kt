package com.catclaw.aura.ui.moment

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MomentDetailViewModelFactory(
    private val application: Application,
    private val cardId: String,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(MomentDetailViewModel::class.java))
        return MomentDetailViewModel(application, cardId) as T
    }
}
