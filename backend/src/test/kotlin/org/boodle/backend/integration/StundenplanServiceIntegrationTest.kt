package org.boodle.backend.integration

import org.boodle.backend.model.Raum
import org.boodle.backend.model.RaumDoppelbuchungException
import org.boodle.backend.model.RaumService
import org.boodle.backend.model.RaumTable
import org.boodle.backend.model.RegulaereStunde
import org.boodle.backend.model.RegulaereStundeTable
import org.boodle.backend.model.StundenplanService
import org.boodle.backend.model.StundenplanValidationException
import org.boodle.backend.model.UnregulaerStatus
import org.boodle.backend.model.UnregulaereStunde
import org.boodle.backend.model.UnregulaereStundeTable
import org.boodle.backend.model.User
import org.boodle.backend.model.UserRole
import org.boodle.backend.model.UserService
import org.boodle.backend.model.UsersTable
import org.boodle.backend.model.Vorlesung
import org.boodle.backend.model.VorlesungService
import org.boodle.backend.model.VorlesungTable
import org.boodle.backend.model.Wochentag
import org.boodle.backend.security.SecurityUtils
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.LocalTime

class StundenplanServiceIntegrationTest {

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
                url = "jdbc:h2:mem:stundenplandb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
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
    fun clearTables() {
        transaction {
            RegulaereStunde.all().forEach { it.delete() }
            UnregulaereStunde.all().forEach { it.delete() }
            Vorlesung.all().forEach { it.delete() }
            Raum.all().forEach { it.delete() }
            User.all().forEach { it.delete() }
        }
        // Clear security context
        org.springframework.security.core.context.SecurityContextHolder.clearContext()
    }

    @Test
    fun createRegulaereStunde_createsOfflineSlotWithRoom() {
        val data = seedLectureAndRoom()

        val created = stundenplanService.createRegulaereStunde(
            vorlesungId = data.vorlesungId,
            vortragsnummer = 1,
            vonUhrzeit = LocalTime.of(9, 0),
            bisUhrzeit = LocalTime.of(10, 30),
            wochentag = Wochentag.MONTAG,
            raumId = data.raumId,
            vonDatum = LocalDate.of(2026, 4, 1),
            bisDatum = LocalDate.of(2026, 6, 30),
            online = false
        )

        assertNotNull(created.id)
        assertEquals(data.raumId, created.raumId)
        assertEquals(false, created.online)
    }

    @Test
    fun createRegulaereStunde_throwsWhenOnlineHasRoom() {
        val data = seedLectureAndRoom()

        val exception = assertThrows(StundenplanValidationException::class.java) {
            stundenplanService.createRegulaereStunde(
                vorlesungId = data.vorlesungId,
                vortragsnummer = 1,
                vonUhrzeit = LocalTime.of(9, 0),
                bisUhrzeit = LocalTime.of(10, 30),
                wochentag = Wochentag.MONTAG,
                raumId = data.raumId,
                vonDatum = LocalDate.of(2026, 4, 1),
                bisDatum = LocalDate.of(2026, 6, 30),
                online = true
            )
        }

        assertEquals("Online lecture must not have a room.", exception.message)
    }

    @Test
    fun createRegulaereStunde_throwsOnRoomConflictWithRegularSlot() {
        val data = seedLectureAndRoom()

        stundenplanService.createRegulaereStunde(
            vorlesungId = data.vorlesungId,
            vortragsnummer = 1,
            vonUhrzeit = LocalTime.of(9, 0),
            bisUhrzeit = LocalTime.of(10, 30),
            wochentag = Wochentag.DIENSTAG,
            raumId = data.raumId,
            vonDatum = LocalDate.of(2026, 4, 1),
            bisDatum = LocalDate.of(2026, 6, 30),
            online = false
        )

        val exception = assertThrows(RaumDoppelbuchungException::class.java) {
            stundenplanService.createRegulaereStunde(
                vorlesungId = data.vorlesungId,
                vortragsnummer = 2,
                vonUhrzeit = LocalTime.of(10, 0),
                bisUhrzeit = LocalTime.of(11, 0),
                wochentag = Wochentag.DIENSTAG,
                raumId = data.raumId,
                vonDatum = LocalDate.of(2026, 5, 1),
                bisDatum = LocalDate.of(2026, 7, 1),
                online = false
            )
        }

        assertEquals("Raum '${data.raumId}' is already booked for this regular time slot.", exception.message)
    }

    @Test
    fun createUnregulaereStunde_throwsOnRoomConflictWithRegularSlot() {
        val data = seedLectureAndRoom()

        stundenplanService.createRegulaereStunde(
            vorlesungId = data.vorlesungId,
            vortragsnummer = 1,
            vonUhrzeit = LocalTime.of(8, 0),
            bisUhrzeit = LocalTime.of(9, 30),
            wochentag = Wochentag.MONTAG,
            raumId = data.raumId,
            vonDatum = LocalDate.of(2026, 4, 1),
            bisDatum = LocalDate.of(2026, 6, 30),
            online = false
        )

        val exception = assertThrows(RaumDoppelbuchungException::class.java) {
            stundenplanService.createUnregulaereStunde(
                vorlesungId = data.vorlesungId,
                status = UnregulaerStatus.ZUSAETZLICH,
                vortragsnummer = 2,
                alteVortragsnummer = null,
                vonUhrzeit = LocalTime.of(8, 30),
                bisUhrzeit = LocalTime.of(9, 0),
                datum = LocalDate.of(2026, 5, 4),
                raumId = data.raumId,
                online = false
            )
        }

        assertEquals("Raum '${data.raumId}' conflicts with a regular lecture slot.", exception.message)
    }

    private fun seedLectureAndRoom(): SeedData {
        userService.create(
            matr = "D1000001",
            name = "Professor Test",
            password = "secure-pass-123",
            email = "dozent@example.com",
            rolle = UserRole.DOZENT
        )

        val lecture = vorlesungService.create(
            code = "INF201",
            name = "Softwaretechnik",
            beschreibung = "Vorlesung",
            studiengang = "Informatik",
            dozentMatr = "D1000001"
        )

        val room = raumService.create(
            code = "A-101",
            beschreibung = "Hauptgebaeude"
        )

        // Set security context for the created course's instructor
        val auth = org.springframework.security.authentication.UsernamePasswordAuthenticationToken("D1000001", null, emptyList())
        org.springframework.security.core.context.SecurityContextHolder.getContext().authentication = auth

        return SeedData(vorlesungId = lecture.id, raumId = room.id)
    }

    data class SeedData(
        val vorlesungId: Int,
        val raumId: Int
    )
}
