package org.boodle.backend.integration

import org.boodle.backend.model.User
import org.boodle.backend.model.UserRole
import org.boodle.backend.model.UserService
import org.boodle.backend.model.UsersTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test

class UserServiceIntegrationTest {

    private val userService = UserService()

    companion object {
        @JvmStatic
        @BeforeAll
        fun connectDatabase() {
            Database.connect(
                url = "jdbc:h2:mem:usersvcdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                driver = "org.h2.Driver",
                user = "sa",
                password = ""
            )

            transaction {
                SchemaUtils.create(UsersTable)
            }
        }
    }

    @AfterEach
    fun cleanTable() {
        transaction {
            User.all().forEach { it.delete() }
        }
    }

    @Test
    fun createUser_persistsAndNormalizesEmail() {
        userService.create(
            matr = "1234567",
            name = "Erik Mustermann",
            password = "secure-pass-123",
            email = "  Erik@gmail.COM  ",
            rolle = UserRole.ADMIN
        )

        val found = userService.findByEmail("erik@gmail.com")

        assertEquals("1234567", found?.id?.value)
        assertEquals("erik@gmail.com", found?.email)
    }
}
