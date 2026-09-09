package com.armadio.feature.garments

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.armadio.R
import com.armadio.core.database.entity.Garment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryRoute(viewModel: InventoryViewModel = hiltViewModel()) {
    val garments by viewModel.garments.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()
    val failed by viewModel.failed.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var editing by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<Long?>(null) }
    var deletingId by rememberSaveable { mutableStateOf<Long?>(null) }
    val current = garments.firstOrNull { it.id == editingId }

    if (editing) {
        // Wait for Room after process recreation before restoring an existing editor.
        if (editingId != null && current == null) {
            BackHandler { editing = false }
            Box(Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            GarmentEditor(current, busy, failed, onClose = { editing = false }, onSave = { name, brand, size, notes ->
                viewModel.save(current, name, brand, size, notes) { editing = false }
            })
        }
        return
    }

    val visible = garments.filter { garment ->
        listOfNotNull(garment.name, garment.brand, garment.size, garment.notes)
            .any { it.contains(query.trim(), ignoreCase = true) }
    }
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.inventory_title)) }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = {
                editingId = null
                viewModel.clearError()
                editing = true
            }) { Text(stringResource(R.string.add_garment)) }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Text(stringResource(R.string.inventory_intro), style = MaterialTheme.typography.bodyLarge) }
            item {
                OutlinedTextField(query, { query = it }, modifier = Modifier.fillMaxWidth(),
                    label = { Text(stringResource(R.string.search_garments)) }, singleLine = true)
            }
            if (visible.isEmpty()) item {
                Text(stringResource(if (garments.isEmpty()) R.string.empty_inventory else R.string.no_results),
                    modifier = Modifier.padding(vertical = 32.dp))
            }
            items(visible, key = { it.id }) { garment ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(garment.name, style = MaterialTheme.typography.titleLarge)
                        listOfNotNull(garment.brand, garment.size, garment.notes).forEach { Text(it) }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = {
                                editingId = garment.id
                                viewModel.clearError()
                                editing = true
                            }) { Text(stringResource(R.string.edit_garment)) }
                            TextButton(onClick = { viewModel.clearError(); deletingId = garment.id }) {
                                Text(stringResource(R.string.delete_garment))
                            }
                        }
                    }
                }
            }
        }
    }
    garments.firstOrNull { it.id == deletingId }?.let { garment ->
        AlertDialog(
            onDismissRequest = { if (!busy) deletingId = null },
            title = { Text(stringResource(R.string.delete_title)) },
            text = { Text(if (failed) stringResource(R.string.save_error) else stringResource(R.string.delete_message, garment.name)) },
            confirmButton = {
                TextButton(enabled = !busy, onClick = { viewModel.delete(garment) { deletingId = null } }) {
                    Text(stringResource(R.string.delete_garment))
                }
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = { deletingId = null }) { Text(stringResource(R.string.cancel)) }
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GarmentEditor(
    garment: Garment?, busy: Boolean, failed: Boolean, onClose: () -> Unit,
    onSave: (String, String, String, String) -> Unit,
) {
    var name by rememberSaveable(garment?.id) { mutableStateOf(garment?.name.orEmpty()) }
    var brand by rememberSaveable(garment?.id) { mutableStateOf(garment?.brand.orEmpty()) }
    var size by rememberSaveable(garment?.id) { mutableStateOf(garment?.size.orEmpty()) }
    var notes by rememberSaveable(garment?.id) { mutableStateOf(garment?.notes.orEmpty()) }
    BackHandler { if (!busy) onClose() }
    Scaffold(topBar = { TopAppBar(title = {
        Text(stringResource(if (garment == null) R.string.add_garment else R.string.edit_garment))
    }) }) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(name, { name = it }, label = { Text(stringResource(R.string.garment_name)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !busy)
            OutlinedTextField(brand, { brand = it }, label = { Text(stringResource(R.string.garment_brand)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !busy)
            OutlinedTextField(size, { size = it }, label = { Text(stringResource(R.string.garment_size)) },
                modifier = Modifier.fillMaxWidth(), singleLine = true, enabled = !busy)
            OutlinedTextField(notes, { notes = it }, label = { Text(stringResource(R.string.garment_notes)) },
                modifier = Modifier.fillMaxWidth(), minLines = 3, enabled = !busy)
            if (failed) Text(stringResource(R.string.save_error), color = MaterialTheme.colorScheme.error)
            Button(onClick = { onSave(name, brand, size, notes) }, enabled = name.isNotBlank() && !busy,
                modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.save)) }
            TextButton(onClick = onClose, enabled = !busy, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.cancel))
            }
        }
    }
}
