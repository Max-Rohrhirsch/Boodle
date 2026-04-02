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
        val testPassword = "12345678"

        transaction {
            // Create test STUDENT user
            val studentEmail = "max@mail.com"
            val existingStudent = User.find { UsersTable.email eq studentEmail.trim().lowercase() }.firstOrNull()
            if (existingStudent == null) {
                User.new("T-0001") {
                    name = "Max Testuser"
                    passHash = requireNotNull(passwordEncoder.encode(testPassword))
                    email = studentEmail.trim().lowercase()
                    rolle = UserRole.STUDENT
                    createdAt = LocalDateTime.now()
                    updatedAt = LocalDateTime.now()
                }
            }

            // Create test ADMIN user
            val adminEmail = "admin@mail.com"
            val existingAdmin = User.find { UsersTable.email eq adminEmail.trim().lowercase() }.firstOrNull()
            if (existingAdmin == null) {
                User.new("A-0001") {
                    name = "Admin User"
                    passHash = requireNotNull(passwordEncoder.encode(testPassword))
                    email = adminEmail.trim().lowercase()
                    rolle = UserRole.ADMIN
                    createdAt = LocalDateTime.now()
                    updatedAt = LocalDateTime.now()
                }
            }

            // Create test DOZENT user
            val dozentEmail = "dozent@mail.com"
            val existingDozent = User.find { UsersTable.email eq dozentEmail.trim().lowercase() }.firstOrNull()
            if (existingDozent == null) {
                User.new("D-0001") {
                    name = "Dozent User"
                    passHash = requireNotNull(passwordEncoder.encode(testPassword))
                    email = dozentEmail.trim().lowercase()
                    rolle = UserRole.DOZENT
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
