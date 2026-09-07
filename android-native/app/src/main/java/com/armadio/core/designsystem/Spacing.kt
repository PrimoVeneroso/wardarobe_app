package com.armadio.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val DefaultSpacing = Spacing()

val MaterialTheme.spacing: Spacing
    @Composable
    @ReadOnlyComposable
    get() = DefaultSpacing

class Spacing(
    val small: Dp = 8.dp,
    val medium: Dp = 16.dp,
    val large: Dp = 24.dp,
)
