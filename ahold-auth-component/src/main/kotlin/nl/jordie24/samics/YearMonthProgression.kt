package nl.jordie24.samics

import java.time.YearMonth

class YearMonthIterator(
    private val start: YearMonth,
    private val endInclusive: YearMonth,
    private val stepMonths: Long
) :
    Iterator<YearMonth> {

    private var currentYearMonth = start

    override fun hasNext() = currentYearMonth <= endInclusive

    override fun next() = currentYearMonth.also {
        currentYearMonth = currentYearMonth.plusMonths(stepMonths)
    }

}

class YearMonthProgression(
    override val start: YearMonth,
    override val endInclusive: YearMonth,
    private val stepMonths: Long = 1
) : Iterable<YearMonth>, ClosedRange<YearMonth> {

    override fun iterator() = YearMonthIterator(start, endInclusive, stepMonths)

    infix fun step(months: Long) = YearMonthProgression(start, endInclusive, months)

}

operator fun YearMonth.rangeTo(that: YearMonth) = YearMonthProgression(this, that)
