package com.example.attendo.data.model.attendance

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class BreakType(
    @SerialName("break_id") val breakId: Int? = null,
    @SerialName("description") val description: String,
    @SerialName("computes_as_work_time") val computesAs: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("is_active") val isActive: Boolean = true
)