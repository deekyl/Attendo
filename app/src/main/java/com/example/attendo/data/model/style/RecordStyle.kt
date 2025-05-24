package com.example.attendo.data.model.style

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class RecordStyle(
    val icon: ImageVector,
    val backgroundColor: Color,
    val textColor: Color,
    val actionText: String,
    val iconTint: Color
)