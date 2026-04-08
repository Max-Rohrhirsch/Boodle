package org.boodle.backend.model

import org.boodle.backend.security.SecurityUtils
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class StundenplanValidationException(message: String) : RuntimeException(message)
class RaumDoppelbuchungException(message: String) : RuntimeException(message)
class VorlesungAccessDeniedException(vorlesungId: Int, userMatr: String) : 
    RuntimeException("User '$userMatr' is not the instructor of Vorlesung '$vorlesungId' and cannot modify its schedule.")
class RaumKapazitaetException(raumId: Int, enrolled: Int, kapazitaet: Int) :
    RuntimeException("Raum $raumId capacity exceeded: $enrolled enrolled students, but room capacity is only $kapazitaet.")

@Service
class StundenplanService(private val securityUtils: SecurityUtils) {

    fun getAllRegulaereStunden(): List<RegulaereStundeDTO> = transaction {
        RegulaereStunde.all().map { it.toDTO() }
    }

    fun getRegulaereStundeById(id: Int): RegulaereStundeDTO = transaction {
        RegulaereStunde.findById(id)?.toDTO() ?: throw RegulaereStundeNotFoundException(id)
    }

    fun getRegulaereStundenByVorlesung(vorlesungId: Int): List<RegulaereStundeDTO> = transaction {
        Vorlesung.findById(vorlesungId) ?: throw VorlesungNotFoundException(vorlesungId)
        RegulaereStunde.find { RegulaereStundeTable.vorlesungId eq vorlesungId }.map { it.toDTO() }
    }

    fun createRegulaereStunde(
        vorlesungId: Int,
        vortragsnummer: Int,
        vonUhrzeit: LocalTime,
        bisUhrzeit: LocalTime,
        wochentag: Wochentag,
        raumId: Int?,
        vonDatum: LocalDate,
        bisDatum: LocalDate,
        online: Boolean
    ): RegulaereStundeDTO = transaction {
        val vorlesung = Vorlesung.findById(vorlesungId) ?: throw VorlesungNotFoundException(vorlesungId)
        
        // Ownership check: only instructor can create their lecture slots
        val currentUserMatr = securityUtils.requireCurrentUserMatr()
        if (vorlesung.dozentMatr != currentUserMatr) {
            throw VorlesungAccessDeniedException(vorlesungId, currentUserMatr)
        }
        
        validateBasics(vorlesungId, vortragsnummer, vonUhrzeit, bisUhrzeit)
        if (vonDatum.isAfter(bisDatum)) {
            throw StundenplanValidationException("vonDatum must be before or equal to bisDatum.")
        }
        val normalizedRaumId = validateRoomChoice(online, raumId)
        checkRegularConflicts(null, normalizedRaumId, wochentag, vonDatum, bisDatum, vonUhrzeit, bisUhrzeit)

        RegulaereStunde.new {
            this.vorlesungId = vorlesungId
            this.vortragsnummer = vortragsnummer
            this.vonUhrzeit = vonUhrzeit
            this.bisUhrzeit = bisUhrzeit
            this.wochentag = wochentag
            this.raumId = normalizedRaumId
            this.vonDatum = vonDatum
            this.bisDatum = bisDatum
            this.online = online
            this.createdAt = LocalDateTime.now()
            this.updatedAt = LocalDateTime.now()
        }.toDTO()
    }

    fun updateRegulaereStunde(
        id: Int,
        vortragsnummer: Int,
        vonUhrzeit: LocalTime,
        bisUhrzeit: LocalTime,
        wochentag: Wochentag,
        raumId: Int?,
        vonDatum: LocalDate,
        bisDatum: LocalDate,
        online: Boolean
    ): RegulaereStundeDTO = transaction {
        val stunde = RegulaereStunde.findById(id) ?: throw RegulaereStundeNotFoundException(id)
        val vorlesung = Vorlesung.findById(stunde.vorlesungId) ?: throw VorlesungNotFoundException(stunde.vorlesungId)
        
        // Ownership check: only instructor can modify their lecture slots
        val currentUserMatr = securityUtils.requireCurrentUserMatr()
        if (vorlesung.dozentMatr != currentUserMatr) {
            throw VorlesungAccessDeniedException(stunde.vorlesungId, currentUserMatr)
        }

        validateBasics(stunde.vorlesungId, vortragsnummer, vonUhrzeit, bisUhrzeit)
        if (vonDatum.isAfter(bisDatum)) {
            throw StundenplanValidationException("vonDatum must be before or equal to bisDatum.")
        }
        val normalizedRaumId = validateRoomChoice(online, raumId)
        checkRegularConflicts(id, normalizedRaumId, wochentag, vonDatum, bisDatum, vonUhrzeit, bisUhrzeit)

        stunde.vortragsnummer = vortragsnummer
        stunde.vonUhrzeit = vonUhrzeit
        stunde.bisUhrzeit = bisUhrzeit
        stunde.wochentag = wochentag
        stunde.raumId = normalizedRaumId
        stunde.vonDatum = vonDatum
        stunde.bisDatum = bisDatum
        stunde.online = online
        stunde.updatedAt = LocalDateTime.now()
        stunde.toDTO()
    }

    fun deleteRegulaereStunde(id: Int) = transaction {
        val stunde = RegulaereStunde.findById(id) ?: throw RegulaereStundeNotFoundException(id)
        val vorlesung = Vorlesung.findById(stunde.vorlesungId) ?: throw VorlesungNotFoundException(stunde.vorlesungId)
        
        // Ownership check: only instructor can delete their lecture slots
        val currentUserMatr = securityUtils.requireCurrentUserMatr()
        if (vorlesung.dozentMatr != currentUserMatr) {
            throw VorlesungAccessDeniedException(stunde.vorlesungId, currentUserMatr)
        }
        
        stunde.delete()
    }

    fun getAllUnregulaereStunden(): List<UnregulaereStundeDTO> = transaction {
        UnregulaereStunde.all().map { it.toDTO() }
    }

    fun getUnregulaereStundeById(id: Int): UnregulaereStundeDTO = transaction {
        UnregulaereStunde.findById(id)?.toDTO() ?: throw UnregulaereStundeNotFoundException(id)
    }

    fun getUnregulaereStundenByVorlesung(vorlesungId: Int): List<UnregulaereStundeDTO> = transaction {
        Vorlesung.findById(vorlesungId) ?: throw VorlesungNotFoundException(vorlesungId)
        UnregulaereStunde.find { UnregulaereStundeTable.vorlesungId eq vorlesungId }.map { it.toDTO() }
    }

    fun createUnregulaereStunde(
        vorlesungId: Int,
        status: UnregulaerStatus,
        vortragsnummer: Int,
        alteVortragsnummer: Int?,
        vonUhrzeit: LocalTime,
        bisUhrzeit: LocalTime,
        datum: LocalDate,
        raumId: Int?,
        online: Boolean
    ): UnregulaereStundeDTO = transaction {
        val vorlesung = Vorlesung.findById(vorlesungId) ?: throw VorlesungNotFoundException(vorlesungId)
        
        // Ownership check: only instructor can create their lecture slots
        val currentUserMatr = securityUtils.requireCurrentUserMatr()
        if (vorlesung.dozentMatr != currentUserMatr) {
            throw VorlesungAccessDeniedException(vorlesungId, currentUserMatr)
        }
        
        validateBasics(vorlesungId, vortragsnummer, vonUhrzeit, bisUhrzeit)
        val normalizedRaumId = validateRoomChoice(online, raumId)
        checkIrregularConflicts(null, status, normalizedRaumId, datum, vonUhrzeit, bisUhrzeit)

        UnregulaereStunde.new {
            this.vorlesungId = vorlesungId
            this.status = status
            this.vortragsnummer = vortragsnummer
            this.alteVortragsnummer = alteVortragsnummer
            this.vonUhrzeit = vonUhrzeit
            this.bisUhrzeit = bisUhrzeit
            this.datum = datum
            this.raumId = normalizedRaumId
            this.online = online
            this.createdAt = LocalDateTime.now()
            this.updatedAt = LocalDateTime.now()
        }.toDTO()
    }

    fun updateUnregulaereStunde(
        id: Int,
        status: UnregulaerStatus,
        vortragsnummer: Int,
        alteVortragsnummer: Int?,
        vonUhrzeit: LocalTime,
        bisUhrzeit: LocalTime,
        datum: LocalDate,
        raumId: Int?,
        online: Boolean
    ): UnregulaereStundeDTO = transaction {
        val stunde = UnregulaereStunde.findById(id) ?: throw UnregulaereStundeNotFoundException(id)
        val vorlesung = Vorlesung.findById(stunde.vorlesungId) ?: throw VorlesungNotFoundException(stunde.vorlesungId)
        
        // Ownership check: only instructor can modify their lecture slots
        val currentUserMatr = securityUtils.requireCurrentUserMatr()
        if (vorlesung.dozentMatr != currentUserMatr) {
            throw VorlesungAccessDeniedException(stunde.vorlesungId, currentUserMatr)
        }

        validateBasics(stunde.vorlesungId, vortragsnummer, vonUhrzeit, bisUhrzeit)
        val normalizedRaumId = validateRoomChoice(online, raumId)
        checkIrregularConflicts(id, status, normalizedRaumId, datum, vonUhrzeit, bisUhrzeit)

        stunde.status = status
        stunde.vortragsnummer = vortragsnummer
        stunde.alteVortragsnummer = alteVortragsnummer
        stunde.vonUhrzeit = vonUhrzeit
        stunde.bisUhrzeit = bisUhrzeit
        stunde.datum = datum
        stunde.raumId = normalizedRaumId
        stunde.online = online
        stunde.updatedAt = LocalDateTime.now()
        stunde.toDTO()
    }

    fun deleteUnregulaereStunde(id: Int) = transaction {
        val stunde = UnregulaereStunde.findById(id) ?: throw UnregulaereStundeNotFoundException(id)
        val vorlesung = Vorlesung.findById(stunde.vorlesungId) ?: throw VorlesungNotFoundException(stunde.vorlesungId)
        
        // Ownership check: only instructor can delete their lecture slots
        val currentUserMatr = securityUtils.requireCurrentUserMatr()
        if (vorlesung.dozentMatr != currentUserMatr) {
            throw VorlesungAccessDeniedException(stunde.vorlesungId, currentUserMatr)
        }
        
        stunde.delete()
    }

    private fun validateBasics(vorlesungId: Int, vortragsnummer: Int, vonUhrzeit: LocalTime, bisUhrzeit: LocalTime) {
        Vorlesung.findById(vorlesungId) ?: throw VorlesungNotFoundException(vorlesungId)
        if (vortragsnummer <= 0) throw StundenplanValidationException("vortragsnummer must be positive.")
        if (!vonUhrzeit.isBefore(bisUhrzeit)) throw StundenplanValidationException("vonUhrzeit must be before bisUhrzeit.")
    }

    private fun validateRoomChoice(online: Boolean, raumId: Int?): Int? {
        if (online) {
            if (raumId != null) throw StundenplanValidationException("Online lecture must not have a room.")
            return null
        }

        if (raumId == null) throw StundenplanValidationException("Vor-Ort lecture requires a room.")
        Raum.findById(raumId) ?: throw RaumNotFoundException(raumId)
        return raumId
    }

    private fun checkRegularConflicts(
        currentId: Int?,
        raumId: Int?,
        wochentag: Wochentag,
        vonDatum: LocalDate,
        bisDatum: LocalDate,
        vonUhrzeit: LocalTime,
        bisUhrzeit: LocalTime
    ) {
        if (raumId == null) return

        val regularRows = RegulaereStundeTable.select { RegulaereStundeTable.raumId eq raumId }
        regularRows.forEach { row ->
            val existingId = row[RegulaereStundeTable.id].value
            if (currentId != null && existingId == currentId) return@forEach

            val existingWochentag = row[RegulaereStundeTable.wochentag]
            if (existingWochentag != wochentag) return@forEach

            val existingVonDatum = row[RegulaereStundeTable.vonDatum]
            val existingBisDatum = row[RegulaereStundeTable.bisDatum]
            val existingVonZeit = row[RegulaereStundeTable.vonUhrzeit]
            val existingBisZeit = row[RegulaereStundeTable.bisUhrzeit]

            if (dateRangesOverlap(vonDatum, bisDatum, existingVonDatum, existingBisDatum)
                && timeRangesOverlap(vonUhrzeit, bisUhrzeit, existingVonZeit, existingBisZeit)
            ) {
                throw RaumDoppelbuchungException("Raum '$raumId' is already booked for this regular time slot.")
            }
        }

        val irregularRows = UnregulaereStundeTable.select { UnregulaereStundeTable.raumId eq raumId }
        irregularRows.forEach { row ->
            val status = row[UnregulaereStundeTable.status]
            if (status == UnregulaerStatus.ABGESAGT) return@forEach

            val existingDatum = row[UnregulaereStundeTable.datum]
            if (existingDatum.isBefore(vonDatum) || existingDatum.isAfter(bisDatum)) return@forEach
            if (dayOfWeekToWochentag(existingDatum.dayOfWeek) != wochentag) return@forEach

            val existingVonZeit = row[UnregulaereStundeTable.vonUhrzeit]
            val existingBisZeit = row[UnregulaereStundeTable.bisUhrzeit]
            if (timeRangesOverlap(vonUhrzeit, bisUhrzeit, existingVonZeit, existingBisZeit)) {
                throw RaumDoppelbuchungException("Raum '$raumId' conflicts with an irregular lecture slot.")
            }
        }
    }

    private fun checkIrregularConflicts(
        currentId: Int?,
        status: UnregulaerStatus,
        raumId: Int?,
        datum: LocalDate,
        vonUhrzeit: LocalTime,
        bisUhrzeit: LocalTime
    ) {
        if (raumId == null || status == UnregulaerStatus.ABGESAGT) return

        val irregularRows = UnregulaereStundeTable.select {
            (UnregulaereStundeTable.raumId eq raumId) and (UnregulaereStundeTable.datum eq datum)
        }

        irregularRows.forEach { row ->
            val existingId = row[UnregulaereStundeTable.id].value
            if (currentId != null && existingId == currentId) return@forEach
            if (row[UnregulaereStundeTable.status] == UnregulaerStatus.ABGESAGT) return@forEach

            val existingVonZeit = row[UnregulaereStundeTable.vonUhrzeit]
            val existingBisZeit = row[UnregulaereStundeTable.bisUhrzeit]
            if (timeRangesOverlap(vonUhrzeit, bisUhrzeit, existingVonZeit, existingBisZeit)) {
                throw RaumDoppelbuchungException("Raum '$raumId' is already booked at this date/time.")
            }
        }

        val weekday = dayOfWeekToWochentag(datum.dayOfWeek)
        val regularRows = RegulaereStundeTable.select { RegulaereStundeTable.raumId eq raumId }
        regularRows.forEach { row ->
            val existingWochentag = row[RegulaereStundeTable.wochentag]
            if (existingWochentag != weekday) return@forEach

            val existingVonDatum = row[RegulaereStundeTable.vonDatum]
            val existingBisDatum = row[RegulaereStundeTable.bisDatum]
            if (datum.isBefore(existingVonDatum) || datum.isAfter(existingBisDatum)) return@forEach

            val existingVonZeit = row[RegulaereStundeTable.vonUhrzeit]
            val existingBisZeit = row[RegulaereStundeTable.bisUhrzeit]
            if (timeRangesOverlap(vonUhrzeit, bisUhrzeit, existingVonZeit, existingBisZeit)) {
                throw RaumDoppelbuchungException("Raum '$raumId' conflicts with a regular lecture slot.")
            }
        }
    }

    private fun dateRangesOverlap(aStart: LocalDate, aEnd: LocalDate, bStart: LocalDate, bEnd: LocalDate): Boolean {
        return !(aEnd.isBefore(bStart) || aStart.isAfter(bEnd))
    }

    private fun timeRangesOverlap(aStart: LocalTime, aEnd: LocalTime, bStart: LocalTime, bEnd: LocalTime): Boolean {
        return aStart.isBefore(bEnd) && aEnd.isAfter(bStart)
    }

    private fun dayOfWeekToWochentag(dayOfWeek: DayOfWeek): Wochentag = when (dayOfWeek) {
        DayOfWeek.MONDAY -> Wochentag.MONTAG
        DayOfWeek.TUESDAY -> Wochentag.DIENSTAG
        DayOfWeek.WEDNESDAY -> Wochentag.MITTWOCH
        DayOfWeek.THURSDAY -> Wochentag.DONNERSTAG
        DayOfWeek.FRIDAY -> Wochentag.FREITAG
        DayOfWeek.SATURDAY -> Wochentag.SAMSTAG
        DayOfWeek.SUNDAY -> Wochentag.SONNTAG
    }
}
