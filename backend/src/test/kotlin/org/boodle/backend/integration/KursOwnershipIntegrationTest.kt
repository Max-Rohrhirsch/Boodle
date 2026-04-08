package org.boodle.backend.integration

import org.boodle.backend.model.KursAccessDeniedException
import org.boodle.backend.model.Kurs
import org.boodle.backend.model.KursService
import org.boodle.backend.model.KursTable
import org.boodle.backend.model.User
import org.boodle.backend.model.UserRole
import org.boodle.backend.model.UserService
import org.boodle.backend.model.UsersTable
import org.boodle.backend.security.SecurityUtils
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder

class KursOwnershipIntegrationTest {

    private val userService = UserService()
    private val securityUtils = SecurityUtils()
    private val kursService = KursService(securityUtils)

    companion object {
        @JvmStatic
        @BeforeAll
        fun connectDatabase() {
            Database.connect(
                url = "jdbc:h2:mem:kursownershipdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
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
    fun clearAndSetup() {
        transaction {
            Kurs.all().forEach { it.delete() }
            User.all().forEach { it.delete() }
        }
        // Clear security context
        SecurityContextHolder.clearContext()
    }

    @Test
    fun updateKurs_throwsAccessDenied_whenUserIsNotInstructor() {
        // Setup: create two instructors and a course
        userService.create(
            matr = "D1000001",
            name = "Dozent 1",
            password = "pass-123",
            email = "d1@example.com",
            rolle = UserRole.DOZENT
        )
        userService.create(
            matr = "D1000002",
            name = "Dozent 2",
            password = "pass-123",
            email = "d2@example.com",
            rolle = UserRole.DOZENT
        )

        val kurs = transaction {
            Kurs.new {
                name = "Mathe 1"
                dozentMatr = "D1000001"
                kurssprecher1Matr = null
                kurssprecher2Matr = null
                createdAt = java.time.LocalDateTime.now()
                updatedAt = java.time.LocalDateTime.now()
            }
        }

        // Authenticate as D1000002 (not the instructor)
        val auth = UsernamePasswordAuthenticationToken("D1000002", null, emptyList())
        SecurityContextHolder.getContext().authentication = auth

        // Attempt to update: should throw
        val exception = assertThrows(KursAccessDeniedException::class.java) {
            kursService.update(kurs.id.value, name = "Mathe 2", null, null)
        }
        assertEquals("User 'D1000002' is not the instructor of Kurs '${kurs.id.value}' and cannot modify it.", exception.message)
    }

    @Test
    fun updateKurs_succeeds_whenUserIsInstructor() {
        // Setup: create one instructor and a course
        userService.create(
            matr = "D1000001",
            name = "Dozent 1",
            password = "pass-123",
            email = "d1@example.com",
            rolle = UserRole.DOZENT
        )

        val kurs = transaction {
            Kurs.new {
                name = "Mathe 1"
                dozentMatr = "D1000001"
                kurssprecher1Matr = null
                kurssprecher2Matr = null
                createdAt = java.time.LocalDateTime.now()
                updatedAt = java.time.LocalDateTime.now()
            }
        }

        // Authenticate as D1000001 (the instructor)
        val auth = UsernamePasswordAuthenticationToken("D1000001", null, emptyList())
        SecurityContextHolder.getContext().authentication = auth

        // Update: should succeed
        val updated = kursService.update(kurs.id.value, name = "Mathe 2", null, null)
        assertEquals("Mathe 2", updated.name)
    }

    @Test
    fun deleteKurs_throwsAccessDenied_whenUserIsNotInstructor() {
        // Setup: create two instructors and a course
        userService.create(
            matr = "D1000001",
            name = "Dozent 1",
            password = "pass-123",
            email = "d1@example.com",
            rolle = UserRole.DOZENT
        )
        userService.create(
            matr = "D1000002",
            name = "Dozent 2",
            password = "pass-123",
            email = "d2@example.com",
            rolle = UserRole.DOZENT
        )

        val kurs = transaction {
            Kurs.new {
                name = "Mathe 1"
                dozentMatr = "D1000001"
                kurssprecher1Matr = null
                kurssprecher2Matr = null
                createdAt = java.time.LocalDateTime.now()
                updatedAt = java.time.LocalDateTime.now()
            }
        }

        // Authenticate as D1000002 (not the instructor)
        val auth = UsernamePasswordAuthenticationToken("D1000002", null, emptyList())
        SecurityContextHolder.getContext().authentication = auth

        // Attempt to delete: should throw
        assertThrows(KursAccessDeniedException::class.java) {
            kursService.delete(kurs.id.value)
        }
    }

    @Test
    fun deleteKurs_succeeds_whenUserIsInstructor() {
        // Setup: create one instructor and a course
        userService.create(
            matr = "D1000001",
            name = "Dozent 1",
            password = "pass-123",
            email = "d1@example.com",
            rolle = UserRole.DOZENT
        )

        val kurs = transaction {
            Kurs.new {
                name = "Mathe 1"
                dozentMatr = "D1000001"
                kurssprecher1Matr = null
                kurssprecher2Matr = null
                createdAt = java.time.LocalDateTime.now()
                updatedAt = java.time.LocalDateTime.now()
            }
        }
        val kursId = kurs.id.value

        // Authenticate as D1000001 (the instructor)
        val auth = UsernamePasswordAuthenticationToken("D1000001", null, emptyList())
        SecurityContextHolder.getContext().authentication = auth

        // Delete: should succeed
        kursService.delete(kursId)

        // Verify deleted
        val deleted = transaction {
            Kurs.findById(kursId)
        }
        assertEquals(null, deleted)
    }
}
