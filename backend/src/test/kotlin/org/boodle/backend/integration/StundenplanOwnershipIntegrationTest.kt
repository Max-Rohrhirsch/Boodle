package org.boodle.backend.integration

import org.boodle.backend.model.RaumService
import org.boodle.backend.model.RaumTable
import org.boodle.backend.model.RegulaereStunde
import org.boodle.backend.model.RegulaereStundeTable
import org.boodle.backend.model.StundenplanService
import org.boodle.backend.model.UnregulaereStunde
import org.boodle.backend.model.UnregulaereStundeTable
import org.boodle.backend.model.User
import org.boodle.backend.model.UserRole
import org.boodle.backend.model.UserService
import org.boodle.backend.model.UsersTable
import org.boodle.backend.model.Vorlesung
import org.boodle.backend.model.VorlesungAccessDeniedException
import org.boodle.backend.model.VorlesungService
import org.boodle.backend.model.VorlesungTable
import org.boodle.backend.model.Wochentag
import org.boodle.backend.security.SecurityUtils
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import java.time.LocalDate
import java.time.LocalTime

class StundenplanOwnershipIntegrationTest {

    private val userService = UserService()
    private val vorlesungService = VorlesungService()
    private val raumService = RaumService()
    private val securityUtils = SecurityUtils()
    private val stundenplanService = StundenplanService(securityUtils)

    companion object {
        @JvmStatic
        @BeforeAll
        fun connectDatabase() {
            Database.connect(
                url = "jdbc:h2:mem:stundenplankowndb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                driver = "org.h2.Driver",
                user = "sa",
                password = ""
            )

            transaction {
                SchemaUtils.create(
                    UsersTable,
                    VorlesungTable,
                    RaumTable,
                    RegulaereStundeTable,
                    UnregulaereStundeTable
                )
            }
        }
    }

    @BeforeEach
    fun clearAndSetup() {
        transaction {
            // Must delete in correct foreign key order: children first, then parents
            RegulaereStunde.all().forEach { it.delete() }
            UnregulaereStunde.all().forEach { it.delete() }
            Vorlesung.all().forEach { it.delete() }
            User.all().forEach { it.delete() }
        }
        SecurityContextHolder.clearContext()
    }

    @Test
    fun createRegulaereStunde_throwsAccessDenied_whenUserIsNotInstructor() {
        // Setup: create two instructors
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

        // Create Lecture owned by D1000001
        val lecture = vorlesungService.create(
            code = "INF101",
            name = "Einführung",
            beschreibung = "Basics",
            studiengang = "Informatik",
            dozentMatr = "D1000001"
        )

        // Authenticate as D1000002 (not the instructor)
        val auth = UsernamePasswordAuthenticationToken("D1000002", null, emptyList())
        SecurityContextHolder.getContext().authentication = auth

        // Attempt to create schedule slot: should throw
        assertThrows(VorlesungAccessDeniedException::class.java) {
            stundenplanService.createRegulaereStunde(
                vorlesungId = lecture.id,
                vortragsnummer = 1,
                vonUhrzeit = LocalTime.of(9, 0),
                bisUhrzeit = LocalTime.of(10, 30),
                wochentag = Wochentag.MONTAG,
                raumId = null,
                vonDatum = LocalDate.of(2026, 4, 1),
                bisDatum = LocalDate.of(2026, 6, 30),
                online = true
            )
        }
    }

    @Test
    fun createRegulaereStunde_succeeds_whenUserIsInstructor() {
        // Setup: create one instructor
        userService.create(
            matr = "D1000001",
            name = "Dozent 1",
            password = "pass-123",
            email = "d1@example.com",
            rolle = UserRole.DOZENT
        )

        // Create Lecture owned by D1000001
        val lecture = vorlesungService.create(
            code = "INF101",
            name = "Einführung",
            beschreibung = "Basics",
            studiengang = "Informatik",
            dozentMatr = "D1000001"
        )

        // Authenticate as D1000001 (the instructor)
        val auth = UsernamePasswordAuthenticationToken("D1000001", null, emptyList())
        SecurityContextHolder.getContext().authentication = auth

        // Create schedule slot: should succeed
        val slot = stundenplanService.createRegulaereStunde(
            vorlesungId = lecture.id,
            vortragsnummer = 1,
            vonUhrzeit = LocalTime.of(9, 0),
            bisUhrzeit = LocalTime.of(10, 30),
            wochentag = Wochentag.MONTAG,
            raumId = null,
            vonDatum = LocalDate.of(2026, 4, 1),
            bisDatum = LocalDate.of(2026, 6, 30),
            online = true
        )

        assert(slot.id > 0)
    }

    @Test
    fun deleteRegulaereStunde_throwsAccessDenied_whenUserIsNotInstructor() {
        // Setup: create two instructors
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

        // Create Lecture and slot owned by D1000001
        val lecture = vorlesungService.create(
            code = "INF101",
            name = "Einführung",
            beschreibung = "Basics",
            studiengang = "Informatik",
            dozentMatr = "D1000001"
        )

        // Authenticate as D1000001 to create slot
        var auth = UsernamePasswordAuthenticationToken("D1000001", null, emptyList())
        SecurityContextHolder.getContext().authentication = auth

        val slot = stundenplanService.createRegulaereStunde(
            vorlesungId = lecture.id,
            vortragsnummer = 1,
            vonUhrzeit = LocalTime.of(9, 0),
            bisUhrzeit = LocalTime.of(10, 30),
            wochentag = Wochentag.MONTAG,
            raumId = null,
            vonDatum = LocalDate.of(2026, 4, 1),
            bisDatum = LocalDate.of(2026, 6, 30),
            online = true
        )

        // Authenticate as D1000002 (not the instructor)
        auth = UsernamePasswordAuthenticationToken("D1000002", null, emptyList())
        SecurityContextHolder.getContext().authentication = auth

        // Attempt to delete: should throw
        assertThrows(VorlesungAccessDeniedException::class.java) {
            stundenplanService.deleteRegulaereStunde(slot.id)
        }
    }
}
