package com.armadio.feature.garments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.armadio.core.database.dao.GarmentDao
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class InventoryViewModel @Inject constructor(
    garmentDao: GarmentDao,
) : ViewModel() {
    val activeCount: StateFlow<Int> = garmentDao.activeCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
}
