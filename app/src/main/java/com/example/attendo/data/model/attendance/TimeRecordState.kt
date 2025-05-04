package com.example.attendo.data.model.attendance

sealed class TimeRecordState {
    data object Loading : TimeRecordState()
    data object CheckedIn : TimeRecordState()
    data object CheckedOut : TimeRecordState()
    data class OnBreak(val breakTypeId: Int) : TimeRecordState()
    data class Error(val message: String) : TimeRecordState()
}