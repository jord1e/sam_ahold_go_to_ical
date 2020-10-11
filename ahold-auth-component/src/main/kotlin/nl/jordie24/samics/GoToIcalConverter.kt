package nl.jordie24.samics

import com.gargoylesoftware.htmlunit.BrowserVersion
import com.gargoylesoftware.htmlunit.WebClient
import com.gargoylesoftware.htmlunit.html.*
import net.fortuna.ical4j.model.Calendar
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.property.*
import net.fortuna.ical4j.util.RandomUidGenerator
import java.io.Closeable
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class GoToIcalConverter(private val username: String, private val password: String) : Closeable {

    companion object {
        private const val XQUERY =
            "//td[@class='calendarCellRegularPast' or @class='calendarCellRegularFuture' or @class='calendarCellRegularCurrent']/table/tbody/tr"
        private const val MONTH_BASE_URL =
            "https://sam.ahold.com/wrkbrn_jct/etm/time/timesheet/etmTnsMonth.jsp?NEW_MONTH_YEAR="
        private const val DAY_DATA_BASE_URL = "https://sam.ahold.com/etm/time/timesheet/etmTnsDetail.jsp?date="
        private val SHIFT_REGEX = "(.*)\\s([0-9]{2}):([0-9]{2})  ~  ([0-9]{2}):([0-9]{2})(?:\\s([^(].*[^)]))?".toRegex()
        private val MY_FMT = DateTimeFormatter.ofPattern("MM/yyyy")
        private val LDT_FMT = DateTimeFormatter.ofPattern("MM.dd.yyyy")
        private val HOUR_MINUTE_FMT = DateTimeFormatter.ofPattern("HH:mm")
        private val DURATION_FMT = DateTimeFormatter.ofPattern("H:mm")
        private val enableHtml = false // todo option
        private val wage = BigDecimal("6.41")
    }

    private val wc: WebClient = WebClient(BrowserVersion.CHROME)
    private var loggedIn = false
    private val uidGenerator = RandomUidGenerator()

    init {
        with(wc.options) {
            isCssEnabled = false
            isRedirectEnabled = true
            isDownloadImages = false
        }
    }

    fun processLoginFlow() {
        if (loggedIn) {
            return
        }

        val aholdLogin = wc.getPage<HtmlPage>("https://sam.ahold.com/")
        wc.disableJs() // auth?PartnerId=dingprod has a noscript submit button

        val aholdAuth = aholdLogin
//            .also { println(it.titleText) }
            .run {
                getFormByName("login")
            }.run {
                getInputByName<HtmlTextInput>("username").type(username)
                getInputByName<HtmlPasswordInput>("password").type(password)
                getInputByValue<HtmlSubmitInput>("Sign in").click<HtmlPage>()
            }

        wc.enableJs() // loginServlet redirect

        val loginInitial = aholdAuth.run {
            val loginInitial = clickFirstButton<HtmlPage>()
            wc.disableJs()
            loginInitial.refresh() as HtmlPage // noscript button
        }

        val login = loginInitial.clickFirstButton<HtmlPage>()
        login.clickFirstButton<HtmlPage>()

        wc.enableJs()
        wc.getPage<HtmlPage>("https://sam.ahold.com/wrkbrn_jct/etm/etmMenu.jsp?locale=nl_NL")

        loggedIn = true
    }

    fun parseGoToCalendar(monthsAndYears: Set<YearMonth>): Calendar {
        val calendar = Calendar()
        with(calendar.properties) {
            add(ProdId("-//Jordie//SamViewer 1.0//EN"))
            add(Version.VERSION_2_0)
            add(CalScale.GREGORIAN)
        }

        monthsAndYears
            .flatMap(this::retrieveGoEvents)
            .map(this::goShiftToEvent)
            .let(calendar.components::addAll)

        return calendar
    }

    private fun retrieveGoEvents(ym: YearMonth): Set<GoShift> {
        val page = wc.getPage<HtmlPage>(MONTH_BASE_URL + MY_FMT.format(ym))
        val result = mutableSetOf<GoShift>()
        val dayEntry = page.getByXPath<HtmlTableRow>(XQUERY)
        for (row in dayEntry) {
            val day = row.firstElementChild.firstElementChild.visibleText.toInt()
            val on = ym.atDay(day)
            val shifts = row.lastElementChild.firstElementChild.getElementsByTagName(HtmlParagraph.TAG_NAME)
            for (shift in shifts) {
//                println(shift.visibleText)
//                println()
                val groups = SHIFT_REGEX.find(shift.visibleText)!!.groupValues
                val store = groups[1]
                val sh = groups[2].toInt()
                val sm = groups[3].toInt()
                val eh = groups[4].toInt()
                val em = groups[5].toInt()
                val entry = GoShift(store, on.atTime(sh, sm), on.atTime(eh, em))
                if (groups.size > 5) {
                    entry.description = groups[6]
                }
                result.add(entry)
            }
        }
        return result
    }

    private fun retrieveGoDetails(shift: GoShift): Set<GoDetails> {
        val page = wc.getPage<HtmlPage>(DAY_DATA_BASE_URL + shift.start.format(LDT_FMT))
        val entries =
            page.getByXPath<HtmlTableRow>("//tr[td[@class='TD-dataBlockRowEven etmPadding' or @class='TD-dataBlockRowOdd etmPadding']]")
        val out = mutableSetOf<GoDetails>()
        for (entry in entries) {
            val children = entry.childElements.toList()
            val details = GoDetails(
                children[0].firstElementChild.visibleText,
                children[1].firstElementChild.visibleText,
                LocalTime.parse(children[2].firstElementChild.firstElementChild.visibleText, HOUR_MINUTE_FMT),
                LocalTime.parse(children[3].firstElementChild.firstElementChild.visibleText, HOUR_MINUTE_FMT),
                LocalTime.parse(children[4].firstElementChild.visibleText, DURATION_FMT),
                children[5].firstElementChild.visibleText,
                children[6].firstElementChild.visibleText,
                children[7].firstElementChild.visibleText
            )
            if (details.from >= shift.start.toLocalTime() && details.to <= shift.end.toLocalTime()) {
                out.add(details)
            }
        }
        return out
    }

    private fun goShiftToEvent(goShift: GoShift): VEvent {
        val event = VEvent(goShift.start, goShift.end, "Albert Heijn")
        event.properties.add(Location(goShift.store))
        event.properties.add(Uid(uidGenerator.generateUid().value))
        var desc: String? = ""
        if (!goShift.description.isNullOrEmpty()) {
            desc = goShift.description!!.capitalize() // !! needed because contracts are disabled??
        }
        val details = retrieveGoDetails(goShift)
        if (details.isNotEmpty()) {
            if (!desc.isNullOrEmpty()) {
                desc += if (enableHtml) "<br><br>" else "\n\n"
            }
            for (det in details) {
                if (enableHtml) {
                    desc += "${det.from.format(HOUR_MINUTE_FMT)} - ${det.to.format(HOUR_MINUTE_FMT)} (${
                        det.hours.format(
                            DURATION_FMT
                        )
                    })<ul><li>${det.hourType} (${det.urenverwerking}" +
                            (if (det.storeNumber.isNullOrEmpty()) "" else " / ${det.storeNumber}") + ")</li><li>${det.team}" +
                            (if (det.activity.isNullOrEmpty()) "" else " (${det.activity})") + "</li></ul>"

                } else {
                    desc += "${det.from.format(HOUR_MINUTE_FMT)} - ${det.to.format(HOUR_MINUTE_FMT)} (${
                        det.hours.format(
                            DURATION_FMT
                        )
                    })\n- ${det.hourType} (${det.urenverwerking}" +
                            (if (det.storeNumber.isNullOrEmpty()) "" else " / ${det.storeNumber}") + ")\n- ${det.team}" +
                            (if (det.activity.isNullOrEmpty()) "" else " (${det.activity})") + "\n\n"

                }
            }

        }
        desc = desc?.removeSuffix("\n\n")
        val (paidH, totalH) = calculatePaidHours(details);
        val paidM = paidH.remainder(BigDecimal.ONE).times(60.toBigDecimal()).setScale(0, RoundingMode.UNNECESSARY)
        desc += "\n\n\uD83D\uDCB0 €${
            paidH.times(wage).setScale(2, RoundingMode.HALF_UP)
        } (" + ((if (paidH.toBigInteger() == BigInteger.ZERO) "" else "${paidH.toBigInteger()}h ") +
                (if (paidM == BigDecimal.ZERO) "" else "${paidM}m")).replace(
            " $".toRegex(),
            ""
        ) + " / ${(paidH / totalH * 100.toBigDecimal()).setScale(0)}%)"

        if (desc != null) {
            event.properties.add(Description(desc))
        }
        return event
    }

    // Pair<PaidHours, total hours)
    private fun calculatePaidHours(details: Set<GoDetails>): Pair<BigDecimal, BigDecimal> {
        var totalHours = BigDecimal(0)
        var paidHours = BigDecimal(0)
        for (detail in details) {
            val hours = detail.hours.hour.toBigDecimal() + (detail.hours.minute.toBigDecimal()
                .divide(BigDecimal(60), 2, RoundingMode.HALF_UP))
            if (detail.hourType in setOf("Meeruren", "Gewerkte uren", "Betaalde pauze", "Ziek - Auto")) {
                paidHours += hours
            }
            totalHours += hours
        }
        return paidHours to totalHours
    }

    override fun close() {
        wc.close()
    }

}