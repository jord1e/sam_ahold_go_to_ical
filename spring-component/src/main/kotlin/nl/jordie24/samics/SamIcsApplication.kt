package nl.jordie24.samics

import nl.jordie24.samics.model.RegistrationModel
import nl.jordie24.samics.repository.RegistrationRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.crypto.encrypt.Encryptors
import org.springframework.security.crypto.keygen.KeyGenerators
import org.springframework.security.crypto.keygen.StringKeyGenerator
import org.springframework.stereotype.Component
import java.util.*

@SpringBootApplication
@EnableMongoRepositories
class SamIcsApplication

@Configuration
class SamIcsApplicationConfiguration {

    @Bean
    fun keyGenerator(): StringKeyGenerator = KeyGenerators.string()

}

fun main(args: Array<String>) {
    runApplication<SamIcsApplication>(*args)
}

@Component
class Test(private val registrationRepository: RegistrationRepository, private val keyGenerator: StringKeyGenerator) : CommandLineRunner {
    override fun run(vararg args: String?) {
        if (registrationRepository.count() == 0L) {
            val key = UUID.randomUUID().toString()
            println(key)
            val salt = keyGenerator.generateKey()
            registrationRepository.insert(RegistrationModel(
                    null,
                    3,
                    1,
                    Encryptors.delux(key, salt).encrypt("pnl....d"),
                    Encryptors.delux(key, salt).encrypt("mySuperSecretPassword"),
                    salt
            ))
        }
    }
}

@Configuration
@EnableWebSecurity
class SecurityConfig : WebSecurityConfigurerAdapter() {
    override fun configure(httpSecurity: HttpSecurity) {
        httpSecurity.httpBasic().disable()
    }
}
