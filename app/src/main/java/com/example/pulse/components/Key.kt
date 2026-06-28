package com.example.pulse.components

import androidx.compose.ui.graphics.vector.ImageVector

data class Key(
    val type: KeyType,
    val label: String? = null,
    val icon: ImageVector? = null
)