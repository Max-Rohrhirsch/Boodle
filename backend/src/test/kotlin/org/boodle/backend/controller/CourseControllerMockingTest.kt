package org.boodle.backend.controller

import io.mockk.every
import io.mockk.mockk
import org.boodle.backend.model.InvalidKursInputException
import org.boodle.backend.model.KursDTO
import org.boodle.backend.model.KursInLectureService
import org.boodle.backend.model.KursLookupDTO
import org.boodle.backend.model.KursService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.time.LocalDateTime

class CourseControllerMockingTest {

    private val kursService = mockk<KursService>()
    private val kursInLectureService = mockk<KursInLectureService>()
    private val controller = CourseController(kursService, kursInLectureService)

    @Test
    fun createKurs_returnsCreated_whenServiceSucceeds() {
        val request = CreateKursRequest(
            name = "Mathe 1",
            dozentMatr = "D1000001",
            kurssprecher1Matr = null,
            kurssprecher2Matr = null
        )
        val dto = KursDTO(
            id = 1,
            name = request.name,
            dozentMatr = request.dozentMatr,
            kurssprecher1Matr = null,
            kurssprecher2Matr = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )

        every {
            kursService.create(request.name, request.dozentMatr, request.kurssprecher1Matr, request.kurssprecher2Matr)
        } returns dto

        val response = controller.createKurs(request)

        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertEquals(dto, response.body)
    }

    @Test
    fun createKurs_returnsBadRequest_whenInputInvalid() {
        val request = CreateKursRequest(
            name = "",
            dozentMatr = "D1000001",
            kurssprecher1Matr = null,
            kurssprecher2Matr = null
        )

        every {
            kursService.create(request.name, request.dozentMatr, request.kurssprecher1Matr, request.kurssprecher2Matr)
        } throws InvalidKursInputException("Name must not be empty.")

        val response = controller.createKurs(request)

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        val body = response.body as Map<*, *>
        assertEquals("Bad Request", body["error"])
    }

    @Test
    fun searchKurse_returnsLookupResults() {
        val result = listOf(KursLookupDTO(id = 1, name = "Mathe 1"))

        every { kursService.searchKurse("mat", 10) } returns result

        val response = controller.searchKurse("mat", 10)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(result, response.body)
    }
}
