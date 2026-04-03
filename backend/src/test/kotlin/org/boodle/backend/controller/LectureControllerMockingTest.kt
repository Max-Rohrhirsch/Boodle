package org.boodle.backend.controller

import io.mockk.every
import io.mockk.mockk
import org.boodle.backend.model.EnrollmentAlreadyExistsException
import org.boodle.backend.model.LectureEnrollmentService
import org.boodle.backend.model.VorlesungLookupDTO
import org.boodle.backend.model.VorlesungDTO
import org.boodle.backend.model.VorlesungService
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import java.time.LocalDateTime

class LectureControllerMockingTest {

    private val vorlesungService = mockk<VorlesungService>()
    private val lectureEnrollmentService = mockk<LectureEnrollmentService>(relaxed = true)
    private val controller = LectureController(vorlesungService, lectureEnrollmentService)

    @Test
    fun enrollStudent_returnsCreated_whenEnrollmentSucceeds() {
        val response = controller.enrollStudent(1, EnrollStudentRequest(studentMatr = "1234567"))

        assertEquals(HttpStatus.CREATED, response.statusCode)
    }

    @Test
    fun enrollStudent_returnsConflict_whenAlreadyEnrolled() {
        every { lectureEnrollmentService.enrollStudentDirect(1, "1234567") } throws EnrollmentAlreadyExistsException(1, "1234567")

        val response = controller.enrollStudent(1, EnrollStudentRequest(studentMatr = "1234567"))

        assertEquals(HttpStatus.CONFLICT, response.statusCode)
        val body = response.body as Map<*, *>
        assertEquals("Conflict", body["error"])
    }

    @Test
    fun getAllVorlesungen_returnsDataFromService() {
        val dto = VorlesungDTO(
            id = 1,
            code = "INF101",
            name = "Einführung",
            beschreibung = "Basics",
            studiengang = "Informatik",
            dozentMatr = "D1000001",
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now()
        )
        every { vorlesungService.getAllVorlesungen() } returns listOf(dto)

        val response = controller.getAllVorlesungen()

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(listOf(dto), response.body)
    }

    @Test
    fun searchVorlesungen_returnsLookupResults() {
        val result = listOf(
            VorlesungLookupDTO(
                id = 7,
                code = "INF201",
                name = "Softwaretechnik"
            )
        )

        every { vorlesungService.searchVorlesungen("soft", 10) } returns result

        val response = controller.searchVorlesungen("soft", 10)

        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(result, response.body)
    }
}
