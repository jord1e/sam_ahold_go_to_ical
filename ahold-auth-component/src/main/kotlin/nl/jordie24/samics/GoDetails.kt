package nl.jordie24.samics

import java.time.LocalTime

data class GoDetails(
    val hourType: String,
    val urenverwerking: String,
    val from: LocalTime,
    val to: LocalTime,
    val hours: LocalTime,
    val team: String,
    val activity: String?,
    val storeNumber: String?
)
