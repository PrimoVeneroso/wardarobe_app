package com.armadio.feature.garments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.armadio.R
import com.armadio.core.designsystem.spacing

@Composable
fun InventoryRoute(viewModel: InventoryViewModel = hiltViewModel()) {
    val activeCount by viewModel.activeCount.collectAsStateWithLifecycle()
    InventoryScreen(activeCount = activeCount)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(activeCount: Int) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.inventory_title)) }) },
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
            ) {
                Text(
                    text = pluralStringResource(R.plurals.inventory_count, activeCount, activeCount),
                    style = MaterialTheme.typography.headlineSmall,
                )
                Text(
                    text = stringResource(R.string.inventory_f1_placeholder),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
