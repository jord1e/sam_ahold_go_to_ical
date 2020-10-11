package nl.jordie24.samics.controller;

import com.github.benmanes.caffeine.cache.Caffeine
import net.fortuna.ical4j.data.CalendarOutputter
import net.fortuna.ical4j.model.Calendar
import nl.jordie24.samics.GoToIcalConverter
import nl.jordie24.samics.rangeTo
import nl.jordie24.samics.repository.RegistrationRepository
import org.bson.types.ObjectId
import org.springframework.security.crypto.encrypt.Encryptors
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.io.Writer
import java.time.Duration
import java.time.YearMonth
import java.util.*
import javax.servlet.http.HttpServletResponse

@RestController
@RequestMapping("/sam-go")
class CalendarController(val registrationRepository: RegistrationRepository) {

    private val calendarCache = Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofMinutes(30))
            .build<ObjectId, Calendar>()

    @GetMapping("/sync.ics")
    fun syncCalendar(@RequestParam("cid") cid: ObjectId, @RequestParam("key") key: UUID, out: Writer, response: HttpServletResponse) {
        println("Calendar request for cid $cid")
        val opt = registrationRepository.findById(cid);
        if (!opt.isPresent) {
            throw  RuntimeException("TODO");
        }
        val model = opt.get();

        val (username, password) = Encryptors.delux(key.toString(), model.salt).run {
            decrypt(model.username) to decrypt(model.password)
        }

        val calendar = calendarCache.get(cid) {
            println("Cache miss: $it, making a request")
            val period = YearMonth.now().run {
                minusMonths(model.syncedMonthsBackwards)..plusMonths(model.syncedMonthsForwards)
            }.toSet()

            GoToIcalConverter(username, password).use { converter ->
                converter.processLoginFlow()
                converter.parseGoToCalendar(period)
            }
        }

        CalendarOutputter().output(calendar, out)
    }
}
