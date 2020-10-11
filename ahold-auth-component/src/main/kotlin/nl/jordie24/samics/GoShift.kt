package nl.jordie24.samics

import java.time.LocalDateTime

data class GoShift(
    val store: String,
    val start: LocalDateTime,
    val end: LocalDateTime,
    var description: String? = null
)
