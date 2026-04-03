package org.boodle.backend.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.boodle.backend.model.RaumDoppelbuchungException
import org.boodle.backend.model.RaumNotFoundException
import org.boodle.backend.model.RegulaereStundeNotFoundException
import org.boodle.backend.model.StundenplanService
import org.boodle.backend.model.StundenplanValidationException
import org.boodle.backend.model.UnregulaerStatus
import org.boodle.backend.model.UnregulaereStundeNotFoundException
import org.boodle.backend.model.VorlesungNotFoundException
import org.boodle.backend.model.Wochentag
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
import java.time.LocalDate
import java.time.LocalTime

@RestController
@RequestMapping("/api/stundenplan")
@Tag(name = "Schedule")
class StundenplanController(private val stundenplanService: StundenplanService) {

    @GetMapping("/regulaer")
    @Operation(summary = "Get all regular lecture slots")
    fun getAllRegular(): ResponseEntity<Any> = ResponseEntity.ok(stundenplanService.getAllRegulaereStunden())

    @GetMapping("/regulaer/{id}")
    @Operation(summary = "Get regular lecture slot by ID")
    fun getRegularById(@PathVariable id: Int): ResponseEntity<Any> =
        try {
            ResponseEntity.ok(stundenplanService.getRegulaereStundeById(id))
        } catch (e: RegulaereStundeNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(HttpStatus.NOT_FOUND, e.message ?: "Regular slot not found"))
        }

    @GetMapping("/vorlesung/{vorlesungId}/regulaer")
    @Operation(summary = "Get regular lecture slots by lecture")
    fun getRegularByLecture(@PathVariable vorlesungId: Int): ResponseEntity<Any> =
        try {
            ResponseEntity.ok(stundenplanService.getRegulaereStundenByVorlesung(vorlesungId))
        } catch (e: VorlesungNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(HttpStatus.NOT_FOUND, e.message ?: "Vorlesung not found"))
        }

    @PostMapping("/regulaer")
    @Operation(summary = "Create regular lecture slot")
    fun createRegular(@RequestBody request: CreateRegulaereStundeRequest): ResponseEntity<Any> =
        try {
            val created = stundenplanService.createRegulaereStunde(
                vorlesungId = request.vorlesungId,
                vortragsnummer = request.vortragsnummer,
                vonUhrzeit = request.vonUhrzeit,
                bisUhrzeit = request.bisUhrzeit,
                wochentag = request.wochentag,
                raumId = request.raumId,
                vonDatum = request.vonDatum,
                bisDatum = request.bisDatum,
                online = request.online
            )
            ResponseEntity.status(HttpStatus.CREATED).body(created)
        } catch (e: VorlesungNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(HttpStatus.NOT_FOUND, e.message ?: "Vorlesung not found"))
        } catch (e: RaumNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(HttpStatus.NOT_FOUND, e.message ?: "Raum not found"))
        } catch (e: RaumDoppelbuchungException) {
            ResponseEntity.status(HttpStatus.CONFLICT).body(errorBody(HttpStatus.CONFLICT, e.message ?: "Room conflict"))
        } catch (e: StundenplanValidationException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(HttpStatus.BAD_REQUEST, e.message ?: "Invalid request"))
        }

    @PutMapping("/regulaer/{id}")
    @Operation(summary = "Update regular lecture slot")
    fun updateRegular(@PathVariable id: Int, @RequestBody request: UpdateRegulaereStundeRequest): ResponseEntity<Any> =
        try {
            val updated = stundenplanService.updateRegulaereStunde(
                id = id,
                vortragsnummer = request.vortragsnummer,
                vonUhrzeit = request.vonUhrzeit,
                bisUhrzeit = request.bisUhrzeit,
                wochentag = request.wochentag,
                raumId = request.raumId,
                vonDatum = request.vonDatum,
                bisDatum = request.bisDatum,
                online = request.online
            )
            ResponseEntity.ok(updated)
        } catch (e: RegulaereStundeNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(HttpStatus.NOT_FOUND, e.message ?: "Regular slot not found"))
        } catch (e: RaumNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(HttpStatus.NOT_FOUND, e.message ?: "Raum not found"))
        } catch (e: RaumDoppelbuchungException) {
            ResponseEntity.status(HttpStatus.CONFLICT).body(errorBody(HttpStatus.CONFLICT, e.message ?: "Room conflict"))
        } catch (e: StundenplanValidationException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(HttpStatus.BAD_REQUEST, e.message ?: "Invalid request"))
        }

    @DeleteMapping("/regulaer/{id}")
    @Operation(summary = "Delete regular lecture slot")
    fun deleteRegular(@PathVariable id: Int): ResponseEntity<Any> =
        try {
            stundenplanService.deleteRegulaereStunde(id)
            ResponseEntity.noContent().build()
        } catch (e: RegulaereStundeNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(HttpStatus.NOT_FOUND, e.message ?: "Regular slot not found"))
        }

    @GetMapping("/unregulaer")
    @Operation(summary = "Get all irregular lecture slots")
    fun getAllIrregular(): ResponseEntity<Any> = ResponseEntity.ok(stundenplanService.getAllUnregulaereStunden())

    @GetMapping("/unregulaer/{id}")
    @Operation(summary = "Get irregular lecture slot by ID")
    fun getIrregularById(@PathVariable id: Int): ResponseEntity<Any> =
        try {
            ResponseEntity.ok(stundenplanService.getUnregulaereStundeById(id))
        } catch (e: UnregulaereStundeNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(HttpStatus.NOT_FOUND, e.message ?: "Irregular slot not found"))
        }

    @GetMapping("/vorlesung/{vorlesungId}/unregulaer")
    @Operation(summary = "Get irregular lecture slots by lecture")
    fun getIrregularByLecture(@PathVariable vorlesungId: Int): ResponseEntity<Any> =
        try {
            ResponseEntity.ok(stundenplanService.getUnregulaereStundenByVorlesung(vorlesungId))
        } catch (e: VorlesungNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(HttpStatus.NOT_FOUND, e.message ?: "Vorlesung not found"))
        }

    @PostMapping("/unregulaer")
    @Operation(summary = "Create irregular lecture slot")
    fun createIrregular(@RequestBody request: CreateUnregulaereStundeRequest): ResponseEntity<Any> =
        try {
            val created = stundenplanService.createUnregulaereStunde(
                vorlesungId = request.vorlesungId,
                status = request.status,
                vortragsnummer = request.vortragsnummer,
                alteVortragsnummer = request.alteVortragsnummer,
                vonUhrzeit = request.vonUhrzeit,
                bisUhrzeit = request.bisUhrzeit,
                datum = request.datum,
                raumId = request.raumId,
                online = request.online
            )
            ResponseEntity.status(HttpStatus.CREATED).body(created)
        } catch (e: VorlesungNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(HttpStatus.NOT_FOUND, e.message ?: "Vorlesung not found"))
        } catch (e: RaumNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(HttpStatus.NOT_FOUND, e.message ?: "Raum not found"))
        } catch (e: RaumDoppelbuchungException) {
            ResponseEntity.status(HttpStatus.CONFLICT).body(errorBody(HttpStatus.CONFLICT, e.message ?: "Room conflict"))
        } catch (e: StundenplanValidationException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(HttpStatus.BAD_REQUEST, e.message ?: "Invalid request"))
        }

    @PutMapping("/unregulaer/{id}")
    @Operation(summary = "Update irregular lecture slot")
    fun updateIrregular(@PathVariable id: Int, @RequestBody request: UpdateUnregulaereStundeRequest): ResponseEntity<Any> =
        try {
            val updated = stundenplanService.updateUnregulaereStunde(
                id = id,
                status = request.status,
                vortragsnummer = request.vortragsnummer,
                alteVortragsnummer = request.alteVortragsnummer,
                vonUhrzeit = request.vonUhrzeit,
                bisUhrzeit = request.bisUhrzeit,
                datum = request.datum,
                raumId = request.raumId,
                online = request.online
            )
            ResponseEntity.ok(updated)
        } catch (e: UnregulaereStundeNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(HttpStatus.NOT_FOUND, e.message ?: "Irregular slot not found"))
        } catch (e: RaumNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(HttpStatus.NOT_FOUND, e.message ?: "Raum not found"))
        } catch (e: RaumDoppelbuchungException) {
            ResponseEntity.status(HttpStatus.CONFLICT).body(errorBody(HttpStatus.CONFLICT, e.message ?: "Room conflict"))
        } catch (e: StundenplanValidationException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorBody(HttpStatus.BAD_REQUEST, e.message ?: "Invalid request"))
        }

    @DeleteMapping("/unregulaer/{id}")
    @Operation(summary = "Delete irregular lecture slot")
    fun deleteIrregular(@PathVariable id: Int): ResponseEntity<Any> =
        try {
            stundenplanService.deleteUnregulaereStunde(id)
            ResponseEntity.noContent().build()
        } catch (e: UnregulaereStundeNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(HttpStatus.NOT_FOUND, e.message ?: "Irregular slot not found"))
        }

    private fun errorBody(status: HttpStatus, message: String): Map<String, Any> = mapOf(
        "status" to status.value(),
        "error" to status.reasonPhrase,
        "message" to message
    )
}

data class CreateRegulaereStundeRequest(
    val vorlesungId: Int,
    val vortragsnummer: Int,
    val vonUhrzeit: LocalTime,
    val bisUhrzeit: LocalTime,
    val wochentag: Wochentag,
    val raumId: Int?,
    val vonDatum: LocalDate,
    val bisDatum: LocalDate,
    val online: Boolean
)

data class UpdateRegulaereStundeRequest(
    val vortragsnummer: Int,
    val vonUhrzeit: LocalTime,
    val bisUhrzeit: LocalTime,
    val wochentag: Wochentag,
    val raumId: Int?,
    val vonDatum: LocalDate,
    val bisDatum: LocalDate,
    val online: Boolean
)

data class CreateUnregulaereStundeRequest(
    val vorlesungId: Int,
    val status: UnregulaerStatus,
    val vortragsnummer: Int,
    val alteVortragsnummer: Int?,
    val vonUhrzeit: LocalTime,
    val bisUhrzeit: LocalTime,
    val datum: LocalDate,
    val raumId: Int?,
    val online: Boolean
)

data class UpdateUnregulaereStundeRequest(
    val status: UnregulaerStatus,
    val vortragsnummer: Int,
    val alteVortragsnummer: Int?,
    val vonUhrzeit: LocalTime,
    val bisUhrzeit: LocalTime,
    val datum: LocalDate,
    val raumId: Int?,
    val online: Boolean
)
