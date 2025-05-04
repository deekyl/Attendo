package com.example.attendo.data.model.attendance

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

@Serializable
data class TimeRecord(
    @SerialName("record_id") val recordId: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("time") val time: String,
    @SerialName("is_entry") val isEntry: Boolean,
    @SerialName("break_type_id") val breakTypeId: Int? = null,
    @SerialName("location") val location: String? = null,
    @SerialName("is_manual") val isManual: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null
)