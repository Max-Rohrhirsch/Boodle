package org.boodle.backend.integration

import org.boodle.backend.model.InvalidKursInputException
import org.boodle.backend.model.Kurs
import org.boodle.backend.model.KursService
import org.boodle.backend.model.KursTable
import org.boodle.backend.model.User
import org.boodle.backend.model.UserRole
import org.boodle.backend.model.UserService
import org.boodle.backend.model.UsersTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.boodle.backend.security.SecurityUtils
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class KursServiceIntegrationTest {

    private val userService = UserService()
    private val securityUtils = SecurityUtils()
    private val kursService = KursService(securityUtils)

    companion object {
        @JvmStatic
        @BeforeAll
        fun connectDatabase() {
            Database.connect(
                url = "jdbc:h2:mem:kurssvcdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                driver = "org.h2.Driver",
                user = "sa",
                password = ""
            )

            transaction {
                SchemaUtils.create(UsersTable, KursTable)
            }
        }
    }

    @BeforeEach
    fun clearTables() {
        transaction {
            Kurs.all().forEach { it.delete() }
            User.all().forEach { it.delete() }
        }
    }

    @Test
    fun createKurs_persistsCourseAndNormalizesName() {
        userService.create(
            matr = "D1000001",
            name = "Professor Test",
            password = "secure-pass-123",
            email = "dozent@example.com",
            rolle = UserRole.DOZENT
        )

        val kurs = kursService.create(
            name = "  Programmierung 1  ",
            dozentMatr = "D1000001",
            kurssprecher1Matr = null,
            kurssprecher2Matr = null
        )

        assertEquals("Programmierung 1", kurs.name)
        assertEquals("D1000001", kurs.dozentMatr)
        assertEquals(1, kursService.getAllKurse().size)
    }

    @Test
    fun createKurs_throwsWhenDozentIsMissing() {
        val exception = assertThrows(InvalidKursInputException::class.java) {
            kursService.create(
                name = "Mathematik",
                dozentMatr = "D9999999",
                kurssprecher1Matr = null,
                kurssprecher2Matr = null
            )
        }

        assertEquals("Dozent with matr 'D9999999' not found.", exception.message)
    }
}

