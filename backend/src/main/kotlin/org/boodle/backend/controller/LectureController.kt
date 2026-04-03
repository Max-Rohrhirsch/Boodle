package org.boodle.backend.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.boodle.backend.model.EnrollmentAlreadyExistsException
import org.boodle.backend.model.InvalidVorlesungInputException
import org.boodle.backend.model.VorlesungNotFoundException
import org.boodle.backend.model.VorlesungService
import org.boodle.backend.model.LectureEnrollmentService
import org.boodle.backend.model.KursInLectureService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/vorlesungen")
@Tag(name = "Lectures")
class LectureController(
    private val vorlesungService: VorlesungService,
    private val lectureEnrollmentService: LectureEnrollmentService,
    private val kursInLectureService: KursInLectureService
) {

    @GetMapping
    @Operation(summary = "Get all lectures")
    fun getAllVorlesungen(): ResponseEntity<Any> =
        ResponseEntity.ok(vorlesungService.getAllVorlesungen())

    @GetMapping("/search")
    @Operation(summary = "Search lectures for autocomplete")
    fun searchVorlesungen(
        @RequestParam("q") query: String,
        @RequestParam("limit", required = false, defaultValue = "10") limit: Int
    ): ResponseEntity<Any> =
        ResponseEntity.ok(vorlesungService.searchVorlesungen(query, limit))

    @GetMapping("/{id}")
    @Operation(summary = "Get lecture by ID")
    fun getVorlesungById(@PathVariable id: Int): ResponseEntity<Any> =
        try {
            ResponseEntity.ok(vorlesungService.getVorlesungById(id))
        } catch (e: VorlesungNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(HttpStatus.NOT_FOUND, e.message ?: "Vorlesung not found"))
        }

    @GetMapping("/{id}/kurse")
    @Operation(summary = "Get courses assigned to lecture")
    fun getKurseForVorlesung(@PathVariable id: Int): ResponseEntity<Any> =
        try {
            ResponseEntity.ok(kursInLectureService.getKurseForLecture(id))
        } catch (e: VorlesungNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(HttpStatus.NOT_FOUND, e.message ?: "Vorlesung not found"))
        }

    @PostMapping
    @Operation(summary = "Create lecture")
    fun createVorlesung(@RequestBody request: CreateVorlesungRequest): ResponseEntity<Any> =
        try {
            val created = vorlesungService.create(
                code = request.code,
                name = request.name,
                beschreibung = request.beschreibung,
                studiengang = request.studiengang,
                dozentMatr = request.dozentMatr
            )
            ResponseEntity.status(HttpStatus.CREATED).body(created)
        } catch (e: InvalidVorlesungInputException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(HttpStatus.BAD_REQUEST, e.message ?: "Invalid request"))
        }

    @PutMapping("/{id}")
    @Operation(summary = "Update lecture")
    fun updateVorlesung(@PathVariable id: Int, @RequestBody request: UpdateVorlesungRequest): ResponseEntity<Any> =
        try {
            val updated = vorlesungService.update(
                id = id,
                name = request.name,
                beschreibung = request.beschreibung,
                studiengang = request.studiengang
            )
            ResponseEntity.ok(updated)
        } catch (e: VorlesungNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(HttpStatus.NOT_FOUND, e.message ?: "Vorlesung not found"))
        } catch (e: InvalidVorlesungInputException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(HttpStatus.BAD_REQUEST, e.message ?: "Invalid request"))
        }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete lecture")
    fun deleteVorlesung(@PathVariable id: Int): ResponseEntity<Any> =
        try {
            vorlesungService.delete(id)
            ResponseEntity.noContent().build()
        } catch (e: VorlesungNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(HttpStatus.NOT_FOUND, e.message ?: "Vorlesung not found"))
        }

    @PostMapping("/{id}/enroll")
    @Operation(summary = "Directly enroll a student")
    fun enrollStudent(@PathVariable id: Int, @RequestBody request: EnrollStudentRequest): ResponseEntity<Any> =
        try {
            lectureEnrollmentService.enrollStudentDirect(id, request.studentMatr)
            ResponseEntity.status(HttpStatus.CREATED).build()
        } catch (e: VorlesungNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(HttpStatus.NOT_FOUND, e.message ?: "Vorlesung not found"))
        } catch (e: EnrollmentAlreadyExistsException) {
            ResponseEntity.status(HttpStatus.CONFLICT).body(errorBody(HttpStatus.CONFLICT, e.message ?: "Enrollment already exists"))
        } catch (e: InvalidVorlesungInputException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(HttpStatus.BAD_REQUEST, e.message ?: "Invalid request"))
        }

    @DeleteMapping("/{id}/enroll/{matr}")
    @Operation(summary = "Unenroll a student")
    fun unenrollStudent(@PathVariable id: Int, @PathVariable matr: String): ResponseEntity<Any> =
        try {
            lectureEnrollmentService.unenrollStudent(id, matr)
            ResponseEntity.noContent().build()
        } catch (e: EnrollmentAlreadyExistsException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(HttpStatus.NOT_FOUND, e.message ?: "Enrollment not found"))
        }

    @GetMapping("/{id}/students")
    @Operation(summary = "Get all enrolled students")
    fun getEnrolledStudents(@PathVariable id: Int): ResponseEntity<Any> =
        try {
            ResponseEntity.ok(lectureEnrollmentService.getEnrolledStudents(id))
        } catch (e: VorlesungNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(HttpStatus.NOT_FOUND, e.message ?: "Vorlesung not found"))
        }

    @GetMapping("/{id}/students/details")
    @Operation(summary = "Get all enrolled students with detail")
    fun getEnrolledStudentDetails(@PathVariable id: Int): ResponseEntity<Any> =
        try {
            ResponseEntity.ok(lectureEnrollmentService.getEnrolledStudentUsers(id))
        } catch (e: VorlesungNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(HttpStatus.NOT_FOUND, e.message ?: "Vorlesung not found"))
        }

    private fun errorBody(status: HttpStatus, message: String): Map<String, Any> = mapOf(
        "status" to status.value(),
        "error" to status.reasonPhrase,
        "message" to message
    )
}

data class CreateVorlesungRequest(
    val code: String,
    val name: String,
    val beschreibung: String,
    val studiengang: String,
    val dozentMatr: String
)

data class UpdateVorlesungRequest(
    val name: String?,
    val beschreibung: String?,
    val studiengang: String?
)

data class EnrollStudentRequest(
    val studentMatr: String
)
