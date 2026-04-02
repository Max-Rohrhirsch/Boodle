package org.boodle.backend.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.boodle.backend.model.InvalidKursInputException
import org.boodle.backend.model.KursInLectureAlreadyExistsException
import org.boodle.backend.model.KursNotFoundException
import org.boodle.backend.model.KursService
import org.boodle.backend.model.KursInLectureService
import org.boodle.backend.model.VorlesungNotFoundException
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/kurse")
@Tag(name = "Courses")
class CourseController(
    private val kursService: KursService,
    private val kursInLectureService: KursInLectureService
) {

    @GetMapping
    @Operation(summary = "Get all courses")
    fun getAllKurse(): ResponseEntity<Any> =
        ResponseEntity.ok(kursService.getAllKurse())

    @GetMapping("/{id}")
    @Operation(summary = "Get course by ID")
    fun getKursById(@PathVariable id: Int): ResponseEntity<Any> =
        try {
            ResponseEntity.ok(kursService.getKursById(id))
        } catch (e: KursNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(HttpStatus.NOT_FOUND, e.message ?: "Kurs not found"))
        }

    @PostMapping
    @Operation(summary = "Create course")
    fun createKurs(@RequestBody request: CreateKursRequest): ResponseEntity<Any> =
        try {
            val created = kursService.create(
                name = request.name,
                dozentMatr = request.dozentMatr,
                kurssprecher1Matr = request.kurssprecher1Matr,
                kurssprecher2Matr = request.kurssprecher2Matr
            )
            ResponseEntity.status(HttpStatus.CREATED).body(created)
        } catch (e: InvalidKursInputException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(HttpStatus.BAD_REQUEST, e.message ?: "Invalid request"))
        }

    @PutMapping("/{id}")
    @Operation(summary = "Update course")
    fun updateKurs(@PathVariable id: Int, @RequestBody request: UpdateKursRequest): ResponseEntity<Any> =
        try {
            val updated = kursService.update(
                id = id,
                name = request.name,
                kurssprecher1Matr = request.kurssprecher1Matr,
                kurssprecher2Matr = request.kurssprecher2Matr
            )
            ResponseEntity.ok(updated)
        } catch (e: KursNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(HttpStatus.NOT_FOUND, e.message ?: "Kurs not found"))
        } catch (e: InvalidKursInputException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(HttpStatus.BAD_REQUEST, e.message ?: "Invalid request"))
        }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete course")
    fun deleteKurs(@PathVariable id: Int): ResponseEntity<Any> =
        try {
            kursService.delete(id)
            ResponseEntity.noContent().build()
        } catch (e: KursNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(HttpStatus.NOT_FOUND, e.message ?: "Kurs not found"))
        }

    @PostMapping("/{id}/vorlesungen/{vorlesungId}")
    @Operation(summary = "Assign lecture to course")
    fun assignLectureToKurs(@PathVariable id: Int, @PathVariable vorlesungId: Int): ResponseEntity<Any> =
        try {
            kursInLectureService.assignLectureToKurs(id, vorlesungId)
            ResponseEntity.status(HttpStatus.CREATED).build()
        } catch (e: KursNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(HttpStatus.NOT_FOUND, e.message ?: "Kurs not found"))
        } catch (e: VorlesungNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(HttpStatus.NOT_FOUND, e.message ?: "Vorlesung not found"))
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(HttpStatus.BAD_REQUEST, e.message ?: "Invalid request"))
        }

    @DeleteMapping("/{id}/vorlesungen/{vorlesungId}")
    @Operation(summary = "Remove lecture from course")
    fun removeLectureFromKurs(@PathVariable id: Int, @PathVariable vorlesungId: Int): ResponseEntity<Any> =
        try {
            kursInLectureService.removeLectureFromKurs(id, vorlesungId)
            ResponseEntity.noContent().build()
        } catch (e: Exception) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(HttpStatus.NOT_FOUND, e.message ?: "Assignment not found"))
        }

    private fun errorBody(status: HttpStatus, message: String): Map<String, Any> = mapOf(
        "status" to status.value(),
        "error" to status.reasonPhrase,
        "message" to message
    )
}

data class CreateKursRequest(
    val name: String,
    val dozentMatr: String,
    val kurssprecher1Matr: String?,
    val kurssprecher2Matr: String?
)

data class UpdateKursRequest(
    val name: String?,
    val kurssprecher1Matr: String?,
    val kurssprecher2Matr: String?
)
