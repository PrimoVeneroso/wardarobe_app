package com.armadio.feature.garments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.armadio.core.database.dao.GarmentDao
import com.armadio.core.database.entity.Garment
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class InventoryViewModel @Inject constructor(
    private val garmentDao: GarmentDao,
) : ViewModel() {
    val activeCount: StateFlow<Int> = garmentDao.activeCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    val garments = garmentDao.activeGarments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    private val _busy = MutableStateFlow(false)
    val busy = _busy.asStateFlow()
    private val _failed = MutableStateFlow(false)
    val failed = _failed.asStateFlow()

    fun save(existing: Garment?, name: String, brand: String, size: String, notes: String, onSaved: () -> Unit) {
        if (name.isBlank() || _busy.value) return
        perform(onSaved) {
            val now = System.currentTimeMillis()
            val garment = (existing ?: Garment(
                uuid = UUID.randomUUID().toString(), name = name.trim(),
                categoryId = 0L, seasonsMask = 15, dressCodeMask = 0,
                condition = "GOOD", createdAt = now, updatedAt = now,
            )).copy(
                name = name.trim(), brand = brand.trim().ifEmpty { null },
                size = size.trim().ifEmpty { null }, notes = notes.trim().ifEmpty { null },
                updatedAt = now,
            )
            if (existing == null) garmentDao.insert(garment) else garmentDao.update(garment)
        }
    }

    fun delete(garment: Garment, onDeleted: () -> Unit) {
        if (_busy.value) return
        perform(onDeleted) { garmentDao.softDelete(garment.id, System.currentTimeMillis()) }
    }

    fun clearError() { _failed.value = false }

    private fun perform(onSuccess: () -> Unit, operation: suspend () -> Unit) {
        _busy.value = true
        _failed.value = false
        viewModelScope.launch {
            try {
                operation()
                onSuccess()
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                _failed.value = true
            } finally {
                _busy.value = false
            }
        }
    }
}
