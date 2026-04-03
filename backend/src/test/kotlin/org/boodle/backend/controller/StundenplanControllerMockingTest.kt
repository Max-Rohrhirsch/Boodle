package org.boodle.backend.controller

import io.mockk.every
import io.mockk.mockk
import org.boodle.backend.model.RaumDoppelbuchungException
import org.boodle.backend.model.RegulaereStundeDTO
import org.boodle.backend.model.StundenplanService
import org.boodle.backend.model.StundenplanValidationException
import org.boodle.backend.model.Wochentag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class StundenplanControllerMockingTest {

    private val stundenplanService = mockk<StundenplanService>()
    private val controller = StundenplanController(stundenplanService)

    @Test
    fun createRegular_returnsCreated_whenServiceSucceeds() {
        val request = CreateRegulaereStundeRequest(
            vorlesungId = 1,
            vortragsnummer = 1,
            vonUhrzeit = LocalTime.of(9, 0),
            bisUhrzeit = LocalTime.of(10, 30),
            wochentag = Wochentag.MONTAG,
            raumId = 1,
            vonDatum = LocalDate.of(2026, 4, 1),
            bisDatum = LocalDate.of(2026, 6, 30),
            online = false
        )

        val dto = RegulaereStundeDTO(
            id = 10,
            vorlesungId = request.vorlesungId,
            vortragsnummer = request.vortragsnummer,
            vonUhrzeit = request.vonUhrzeit,
            bisUhrzeit = request.bisUhrzeit,
            wochentag = request.wochentag,
            raumId = request.raumId,
            vonDatum = request.vonDatum,
            bisDatum = request.bisDatum,
            online = request.online,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        every {
            stundenplanService.createRegulaereStunde(
                request.vorlesungId,
                request.vortragsnummer,
                request.vonUhrzeit,
                request.bisUhrzeit,
                request.wochentag,
                request.raumId,
                request.vonDatum,
                request.bisDatum,
                request.online
            )
        } returns dto

        val response = controller.createRegular(request)

        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertEquals(dto, response.body)
    }

    @Test
    fun createRegular_returnsConflict_whenRoomIsDoubleBooked() {
        val request = CreateRegulaereStundeRequest(
            vorlesungId = 1,
            vortragsnummer = 1,
            vonUhrzeit = LocalTime.of(9, 0),
            bisUhrzeit = LocalTime.of(10, 30),
            wochentag = Wochentag.MONTAG,
            raumId = 1,
            vonDatum = LocalDate.of(2026, 4, 1),
            bisDatum = LocalDate.of(2026, 6, 30),
            online = false
        )

        every {
            stundenplanService.createRegulaereStunde(
                request.vorlesungId,
                request.vortragsnummer,
                request.vonUhrzeit,
                request.bisUhrzeit,
                request.wochentag,
                request.raumId,
                request.vonDatum,
                request.bisDatum,
                request.online
            )
        } throws RaumDoppelbuchungException("Raum '1' is already booked.")

        val response = controller.createRegular(request)

        assertEquals(HttpStatus.CONFLICT, response.statusCode)
        val body = response.body as Map<*, *>
        assertEquals("Conflict", body["error"])
    }

    @Test
    fun createRegular_returnsBadRequest_whenValidationFails() {
        val request = CreateRegulaereStundeRequest(
            vorlesungId = 1,
            vortragsnummer = 0,
            vonUhrzeit = LocalTime.of(10, 30),
            bisUhrzeit = LocalTime.of(9, 0),
            wochentag = Wochentag.MONTAG,
            raumId = null,
            vonDatum = LocalDate.of(2026, 4, 1),
            bisDatum = LocalDate.of(2026, 6, 30),
            online = false
        )

        every {
            stundenplanService.createRegulaereStunde(
                request.vorlesungId,
                request.vortragsnummer,
                request.vonUhrzeit,
                request.bisUhrzeit,
                request.wochentag,
                request.raumId,
                request.vonDatum,
                request.bisDatum,
                request.online
            )
        } throws StundenplanValidationException("Invalid")

        val response = controller.createRegular(request)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        val body = response.body as Map<*, *>
        assertEquals("Bad Request", body["error"])
    }
}
