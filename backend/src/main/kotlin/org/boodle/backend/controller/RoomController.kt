package org.boodle.backend.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.boodle.backend.model.InvalidRaumInputException
import org.boodle.backend.model.RaumCodeAlreadyExistsException
import org.boodle.backend.model.RaumNotFoundException
import org.boodle.backend.model.RaumService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/raeume")
@Tag(name = "Rooms")
class RoomController(private val raumService: RaumService) {

    @GetMapping
    @Operation(summary = "Get all rooms")
    fun getAllRooms(): ResponseEntity<Any> = ResponseEntity.ok(raumService.getAllRaeume())

    @GetMapping("/{id}")
    @Operation(summary = "Get room by ID")
    fun getRoomById(@PathVariable id: Int): ResponseEntity<Any> =
        try {
            ResponseEntity.ok(raumService.getRaumById(id))
        } catch (e: RaumNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(HttpStatus.NOT_FOUND, e.message ?: "Raum not found"))
        }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create room")
    fun createRoom(@RequestBody request: CreateRaumRequest): ResponseEntity<Any> =
        try {
            val created = raumService.create(request.code, request.beschreibung, request.kapazitaet)
            ResponseEntity.status(HttpStatus.CREATED).body(created)
        } catch (e: RaumCodeAlreadyExistsException) {
            ResponseEntity.status(HttpStatus.CONFLICT).body(errorBody(HttpStatus.CONFLICT, e.message ?: "Raum already exists"))
        } catch (e: InvalidRaumInputException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(HttpStatus.BAD_REQUEST, e.message ?: "Invalid request"))
        }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update room")
    fun updateRoom(@PathVariable id: Int, @RequestBody request: UpdateRaumRequest): ResponseEntity<Any> =
        try {
            val updated = raumService.update(id, request.code, request.beschreibung, request.kapazitaet)
            ResponseEntity.ok(updated)
        } catch (e: RaumNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(HttpStatus.NOT_FOUND, e.message ?: "Raum not found"))
        } catch (e: RaumCodeAlreadyExistsException) {
            ResponseEntity.status(HttpStatus.CONFLICT).body(errorBody(HttpStatus.CONFLICT, e.message ?: "Raum already exists"))
        } catch (e: InvalidRaumInputException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(HttpStatus.BAD_REQUEST, e.message ?: "Invalid request"))
        }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete room")
    fun deleteRoom(@PathVariable id: Int): ResponseEntity<Any> =
        try {
            raumService.delete(id)
            ResponseEntity.noContent().build()
        } catch (e: RaumNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(HttpStatus.NOT_FOUND, e.message ?: "Raum not found"))
        }

    private fun errorBody(status: HttpStatus, message: String): Map<String, Any> = mapOf(
        "status" to status.value(),
        "error" to status.reasonPhrase,
        "message" to message
    )
}

data class CreateRaumRequest(
    val code: String,
    val beschreibung: String?,
    val kapazitaet: Int = 30
)

data class UpdateRaumRequest(
    val code: String,
    val beschreibung: String?,
    val kapazitaet: Int = 30
)
