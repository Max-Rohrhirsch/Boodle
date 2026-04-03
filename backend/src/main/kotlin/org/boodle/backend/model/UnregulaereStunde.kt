package org.boodle.backend.model

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.date
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.javatime.time
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

enum class UnregulaerStatus {
    ZUSAETZLICH,
    VERLEGT,
    ABGESAGT
}

object UnregulaereStundeTable : IntIdTable("unregulaere_stunde") {
    val vorlesungId = integer("vorlesung_id").references(VorlesungTable.id)
    val status = enumerationByName<UnregulaerStatus>("status", 20)
    val vortragsnummer = integer("vortragsnummer")
    val alteVortragsnummer = integer("alte_vortragsnummer").nullable()
    val vonUhrzeit = time("von_uhrzeit")
    val bisUhrzeit = time("bis_uhrzeit")
    val datum = date("datum")
    val raumId = integer("raum_id").references(RaumTable.id).nullable()
    val online = bool("online")
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}

data class UnregulaereStundeDTO(
    val id: Int,
    val vorlesungId: Int,
    val status: UnregulaerStatus,
    val vortragsnummer: Int,
    val alteVortragsnummer: Int?,
    val vonUhrzeit: LocalTime,
    val bisUhrzeit: LocalTime,
    val datum: LocalDate,
    val raumId: Int?,
    val online: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

class UnregulaereStunde(id: EntityID<Int>) : Entity<Int>(id) {
    companion object : EntityClass<Int, UnregulaereStunde>(UnregulaereStundeTable)

    var vorlesungId by UnregulaereStundeTable.vorlesungId
    var status by UnregulaereStundeTable.status
    var vortragsnummer by UnregulaereStundeTable.vortragsnummer
    var alteVortragsnummer by UnregulaereStundeTable.alteVortragsnummer
    var vonUhrzeit by UnregulaereStundeTable.vonUhrzeit
    var bisUhrzeit by UnregulaereStundeTable.bisUhrzeit
    var datum by UnregulaereStundeTable.datum
    var raumId by UnregulaereStundeTable.raumId
    var online by UnregulaereStundeTable.online
    var createdAt by UnregulaereStundeTable.createdAt
    var updatedAt by UnregulaereStundeTable.updatedAt
}

fun UnregulaereStunde.toDTO(): UnregulaereStundeDTO = UnregulaereStundeDTO(
    id = id.value,
    vorlesungId = vorlesungId,
    status = status,
    vortragsnummer = vortragsnummer,
    alteVortragsnummer = alteVortragsnummer,
    vonUhrzeit = vonUhrzeit,
    bisUhrzeit = bisUhrzeit,
    datum = datum,
    raumId = raumId,
    online = online,
    createdAt = createdAt,
    updatedAt = updatedAt
)

class UnregulaereStundeNotFoundException(id: Int) : RuntimeException("Unregulaere Stunde with ID '$id' was not found.")
