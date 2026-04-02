package org.boodle.backend

import org.boodle.backend.model.User
import org.boodle.backend.model.UserRole
import org.boodle.backend.model.UsersTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.LocalDateTime

@SpringBootApplication
class BackendApplication {
    @Bean
    fun seedTestUserOnStartup(passwordEncoder: PasswordEncoder): ApplicationRunner = ApplicationRunner {
        val testEmail = "max@mail.com"
        val testPassword = "12345678"

        transaction {
            val existing = User.find { UsersTable.email eq testEmail.trim().lowercase() }.firstOrNull()

            if (existing == null) {
                User.new("T-0001") {
                    name = "Max Testuser"
                    passHash = requireNotNull(passwordEncoder.encode(testPassword))
                    email = testEmail.trim().lowercase()
                    rolle = UserRole.STUDENT
                    createdAt = LocalDateTime.now()
                    updatedAt = LocalDateTime.now()
                }
            }
        }
    }
}

fun main(args: Array<String>) {
    runApplication<BackendApplication>(*args)
}
