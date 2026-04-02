package org.boodle.backend.model

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Service
import java.time.LocalDateTime

object VorlesungTable : IntIdTable("vorlesung") {
    val code = varchar("code", 20).uniqueIndex()
    val name = varchar("name", 255)
    val beschreibung = text("beschreibung")
    val studiengang = varchar("studiengang", 100)
    val dozentMatr = varchar("dozent_matr", 20)
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}

data class VorlesungDTO(
    val id: Int,
    val code: String,
    val name: String,
    val beschreibung: String,
    val studiengang: String,
    val dozentMatr: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

class Vorlesung(id: EntityID<Int>) : Entity<Int>(id) {
    companion object : EntityClass<Int, Vorlesung>(VorlesungTable)

    var code by VorlesungTable.code
    var name by VorlesungTable.name
    var beschreibung by VorlesungTable.beschreibung
    var studiengang by VorlesungTable.studiengang
    var dozentMatr by VorlesungTable.dozentMatr
    var createdAt by VorlesungTable.createdAt
    var updatedAt by VorlesungTable.updatedAt
}

fun Vorlesung.toDTO(): VorlesungDTO = VorlesungDTO(
    id = id.value,
    code = code,
    name = name,
    beschreibung = beschreibung,
    studiengang = studiengang,
    dozentMatr = dozentMatr,
    createdAt = createdAt,
    updatedAt = updatedAt
)

class VorlesungNotFoundException(id: Int) : RuntimeException("Vorlesung with ID '$id' was not found.")
class VorlesungCodeAlreadyExistsException(code: String) : RuntimeException("Vorlesung with code '$code' already exists.")
class InvalidVorlesungInputException(message: String) : RuntimeException(message)

@Service
class VorlesungService {

    fun getAllVorlesungen(): List<VorlesungDTO> = transaction {
        Vorlesung.all().map { it.toDTO() }
    }

    fun getVorlesungById(id: Int): VorlesungDTO = transaction {
        Vorlesung.findById(id)?.toDTO() ?: throw VorlesungNotFoundException(id)
    }

    fun create(
        code: String,
        name: String,
        beschreibung: String,
        studiengang: String,
        dozentMatr: String
    ): VorlesungDTO = transaction {
        val normalizedCode = code.trim().uppercase()
        val normalizedName = name.trim()

        if (normalizedCode.isBlank()) throw InvalidVorlesungInputException("Code must not be empty.")
        if (normalizedCode.length > 20) throw InvalidVorlesungInputException("Code is too long (max 20 chars).")
        if (normalizedName.isBlank()) throw InvalidVorlesungInputException("Name must not be empty.")
        if (beschreibung.trim().isBlank()) throw InvalidVorlesungInputException("Beschreibung must not be empty.")
        if (studiengang.trim().isBlank()) throw InvalidVorlesungInputException("Studiengang must not be empty.")

        // Verify dozent exists
        User.findById(dozentMatr) ?: throw InvalidVorlesungInputException("Dozent with matr '$dozentMatr' not found.")

        // Check code uniqueness
        Vorlesung.find { VorlesungTable.code eq normalizedCode }.firstOrNull()?.let {
            throw VorlesungCodeAlreadyExistsException(normalizedCode)
        }

        Vorlesung.new {
            this.code = normalizedCode
            this.name = normalizedName
            this.beschreibung = beschreibung.trim()
            this.studiengang = studiengang.trim()
            this.dozentMatr = dozentMatr
            this.createdAt = LocalDateTime.now()
            this.updatedAt = LocalDateTime.now()
        }.toDTO()
    }

    fun update(
        id: Int,
        name: String?,
        beschreibung: String?,
        studiengang: String?
    ): VorlesungDTO = transaction {
        val vorlesung = Vorlesung.findById(id) ?: throw VorlesungNotFoundException(id)

        name?.let {
            val normalizedName = it.trim()
            if (normalizedName.isBlank()) throw InvalidVorlesungInputException("Name must not be empty.")
            vorlesung.name = normalizedName
        }

        beschreibung?.let {
            val normalized = it.trim()
            if (normalized.isBlank()) throw InvalidVorlesungInputException("Beschreibung must not be empty.")
            vorlesung.beschreibung = normalized
        }

        studiengang?.let {
            val normalized = it.trim()
            if (normalized.isBlank()) throw InvalidVorlesungInputException("Studiengang must not be empty.")
            vorlesung.studiengang = normalized
        }

        vorlesung.updatedAt = LocalDateTime.now()
        vorlesung.toDTO()
    }

    fun delete(id: Int) = transaction {
        val vorlesung = Vorlesung.findById(id) ?: throw VorlesungNotFoundException(id)
        vorlesung.delete()
    }
}
