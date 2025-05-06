package com.example.attendo.data.model.attendance

import java.time.LocalDate

data class TimeRecordFilter(
    val userId: String,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val actionType: ActionType = ActionType.ALL,
    val limit: Int = 20,
    val ascending: Boolean = true
) {
    enum class ActionType {
        ALL, ENTRY, EXIT, BREAK
    }
}