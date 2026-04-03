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

enum class Wochentag {
    MONTAG,
    DIENSTAG,
    MITTWOCH,
    DONNERSTAG,
    FREITAG,
    SAMSTAG,
    SONNTAG
}

object RegulaereStundeTable : IntIdTable("regulaere_stunde") {
    val vorlesungId = integer("vorlesung_id").references(VorlesungTable.id)
    val vortragsnummer = integer("vortragsnummer")
    val vonUhrzeit = time("von_uhrzeit")
    val bisUhrzeit = time("bis_uhrzeit")
    val wochentag = enumerationByName<Wochentag>("wochentag", 20)
    val raumId = integer("raum_id").references(RaumTable.id).nullable()
    val vonDatum = date("von_datum")
    val bisDatum = date("bis_datum")
    val online = bool("online")
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}

data class RegulaereStundeDTO(
    val id: Int,
    val vorlesungId: Int,
    val vortragsnummer: Int,
    val vonUhrzeit: LocalTime,
    val bisUhrzeit: LocalTime,
    val wochentag: Wochentag,
    val raumId: Int?,
    val vonDatum: LocalDate,
    val bisDatum: LocalDate,
    val online: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

class RegulaereStunde(id: EntityID<Int>) : Entity<Int>(id) {
    companion object : EntityClass<Int, RegulaereStunde>(RegulaereStundeTable)

    var vorlesungId by RegulaereStundeTable.vorlesungId
    var vortragsnummer by RegulaereStundeTable.vortragsnummer
    var vonUhrzeit by RegulaereStundeTable.vonUhrzeit
    var bisUhrzeit by RegulaereStundeTable.bisUhrzeit
    var wochentag by RegulaereStundeTable.wochentag
    var raumId by RegulaereStundeTable.raumId
    var vonDatum by RegulaereStundeTable.vonDatum
    var bisDatum by RegulaereStundeTable.bisDatum
    var online by RegulaereStundeTable.online
    var createdAt by RegulaereStundeTable.createdAt
    var updatedAt by RegulaereStundeTable.updatedAt
}

fun RegulaereStunde.toDTO(): RegulaereStundeDTO = RegulaereStundeDTO(
    id = id.value,
    vorlesungId = vorlesungId,
    vortragsnummer = vortragsnummer,
    vonUhrzeit = vonUhrzeit,
    bisUhrzeit = bisUhrzeit,
    wochentag = wochentag,
    raumId = raumId,
    vonDatum = vonDatum,
    bisDatum = bisDatum,
    online = online,
    createdAt = createdAt,
    updatedAt = updatedAt
)

class RegulaereStundeNotFoundException(id: Int) : RuntimeException("Regulaere Stunde with ID '$id' was not found.")
