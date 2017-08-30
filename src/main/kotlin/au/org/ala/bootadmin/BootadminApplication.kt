package au.org.ala.bootadmin

import de.codecentric.boot.admin.config.EnableAdminServer
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.context.annotation.Configuration
import de.codecentric.boot.admin.notify.LoggingNotifier
import de.codecentric.boot.admin.notify.filter.FilteringNotifier
import de.codecentric.boot.admin.notify.Notifier
import org.springframework.scheduling.annotation.Scheduled
import java.util.concurrent.TimeUnit
import de.codecentric.boot.admin.notify.RemindingNotifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.Profile
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter

//@SpringBootApplication
@EnableAutoConfiguration
@EnableAdminServer
@Configuration
class BootadminApplication() {

    @Profile("secure")
    // tag::configuration-spring-security[]
    @Configuration
    class SecurityConfig : WebSecurityConfigurerAdapter() {
        @Throws(Exception::class)
        override fun configure(http: HttpSecurity) {
            // Page with login form is served as /login.html and does a POST on /login
            http.formLogin().loginPage("/login.html").loginProcessingUrl("/login").permitAll()
            // The UI does a POST on /logout on logout
            http.logout().logoutUrl("/logout")
            // The ui currently doesn't support csrf
            http.csrf().disable()

            // Requests for the login page and the static assets are allowed
            http.authorizeRequests()
                    .antMatchers("/login.html", "/**/*.css", "/img/**", "/third-party/**")
                    .permitAll()
            // ... and any other request needs to be authorized
            http.authorizeRequests().antMatchers("/**").authenticated()

            // Enable so that the clients can authenticate via HTTP basic for registering
            http.httpBasic()
        }
    }
    // end::configuration-spring-security[]

    @Configuration
    class NotifierConfig {
        @Bean
        @Primary
        fun remindingNotifier(): RemindingNotifier {
            val notifier = RemindingNotifier(filteringNotifier(loggerNotifier()))
            notifier.setReminderPeriod(TimeUnit.SECONDS.toMillis(10))
            return notifier
        }

        @Scheduled(fixedRate = 1_000L)
        fun remind() {
            remindingNotifier().sendReminders()
        }

        @Bean
        fun filteringNotifier(delegate: Notifier): FilteringNotifier {
            return FilteringNotifier(delegate)
        }

        @Bean
        fun loggerNotifier(): LoggingNotifier {
            return LoggingNotifier()
        }
    }
}

fun main(args: Array<String>) {
    SpringApplication.run(BootadminApplication::class.java, *args)
}
