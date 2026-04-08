package org.boodle.backend.model

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.Table

import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.inList
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.springframework.stereotype.Service
import java.time.LocalDateTime

object KursgruppeTable : IntIdTable("kursgruppe") {
    val name = varchar("name", 255)
    val beschreibung = text("beschreibung").nullable()
    val createdAt = datetime("created_at")
    val updatedAt = datetime("updated_at")
}

object KursInGruppeTable : Table("kurs_in_gruppe") {
    val kursId = integer("kurs_id").references(KursTable.id)
    val kursgruppeId = integer("kursgruppe_id").references(KursgruppeTable.id)
    val createdAt = datetime("created_at")

    override val primaryKey = PrimaryKey(kursId, kursgruppeId)
}

data class KursgruppeDTO(
    val id: Int,
    val name: String,
    val beschreibung: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

data class KursgruppeWithKurseDTO(
    val id: Int,
    val name: String,
    val beschreibung: String?,
    val kurse: List<KursLookupDTO>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)

class Kursgruppe(id: EntityID<Int>) : Entity<Int>(id) {
    companion object : EntityClass<Int, Kursgruppe>(KursgruppeTable)

    var name by KursgruppeTable.name
    var beschreibung by KursgruppeTable.beschreibung
    var createdAt by KursgruppeTable.createdAt
    var updatedAt by KursgruppeTable.updatedAt
}

fun Kursgruppe.toDTO(): KursgruppeDTO = KursgruppeDTO(
    id = id.value,
    name = name,
    beschreibung = beschreibung,
    createdAt = createdAt,
    updatedAt = updatedAt
)

class KursgruppeNotFoundException(id: Int) : RuntimeException("Kursgruppe with ID '$id' was not found.")
class InvalidKursgruppeInputException(message: String) : RuntimeException(message)
class KursgruppeScheduleConflictException(message: String) : RuntimeException(message)

@Service
class KursgruppeService {

    fun getAllKursgruppen(): List<KursgruppeDTO> = transaction {
        Kursgruppe.all().map { it.toDTO() }
    }

    fun getKursgruppeById(id: Int): KursgruppeDTO = transaction {
        Kursgruppe.findById(id)?.toDTO() ?: throw KursgruppeNotFoundException(id)
    }

    fun getKursgruppeWithKurseById(id: Int): KursgruppeWithKurseDTO = transaction {
        val kursgruppe = Kursgruppe.findById(id) ?: throw KursgruppeNotFoundException(id)
        val kurse = KursInGruppeTable.select { KursInGruppeTable.kursgruppeId eq id }
            .mapNotNull { row ->
                val kursId = row[KursInGruppeTable.kursId]
                Kurs.findById(kursId)?.toLookupDTO()
            }
        
        KursgruppeWithKurseDTO(
            id = kursgruppe.id.value,
            name = kursgruppe.name,
            beschreibung = kursgruppe.beschreibung,
            kurse = kurse,
            createdAt = kursgruppe.createdAt,
            updatedAt = kursgruppe.updatedAt
        )
    }

    fun create(name: String, beschreibung: String?): KursgruppeDTO = transaction {
        val normalizedName = name.trim()

        if (normalizedName.isBlank()) throw InvalidKursgruppeInputException("Name must not be empty.")
        if (normalizedName.length > 255) throw InvalidKursgruppeInputException("Name is too long (max 255 chars).")

        Kursgruppe.new {
            this.name = normalizedName
            this.beschreibung = beschreibung?.trim()?.takeIf { it.isNotEmpty() }
            this.createdAt = LocalDateTime.now()
            this.updatedAt = LocalDateTime.now()
        }.toDTO()
    }

    fun update(id: Int, name: String, beschreibung: String?): KursgruppeDTO = transaction {
        val normalizedName = name.trim()
        if (normalizedName.isBlank()) throw InvalidKursgruppeInputException("Name must not be empty.")
        if (normalizedName.length > 255) throw InvalidKursgruppeInputException("Name is too long (max 255 chars).")

        val kursgruppe = Kursgruppe.findById(id) ?: throw KursgruppeNotFoundException(id)
        kursgruppe.name = normalizedName
        kursgruppe.beschreibung = beschreibung?.trim()?.takeIf { it.isNotEmpty() }
        kursgruppe.updatedAt = LocalDateTime.now()
        kursgruppe.toDTO()
    }

    fun delete(id: Int) = transaction {
        val kursgruppe = Kursgruppe.findById(id) ?: throw KursgruppeNotFoundException(id)
        // Delete all associations
        KursInGruppeTable.deleteWhere { kursgruppeId eq id }
        kursgruppe.delete()
    }

    fun addKursToGruppe(kursgruppeId: Int, kursId: Int) = transaction {
        val kursgruppe = Kursgruppe.findById(kursgruppeId) ?: throw KursgruppeNotFoundException(kursgruppeId)
        val kurs = Kurs.findById(kursId) ?: throw KursNotFoundException(kursId)

        // Check if already in group
        val exists = KursInGruppeTable.select {
            (KursInGruppeTable.kursgruppeId eq kursgruppeId) and
            (KursInGruppeTable.kursId eq kursId)
        }.firstOrNull() != null

        if (exists) {
            throw InvalidKursgruppeInputException("Kurs $kursId is already in group $kursgruppeId")
        }

        KursInGruppeTable.insert {
            it[KursInGruppeTable.kursId] = kursId
            it[KursInGruppeTable.kursgruppeId] = kursgruppeId
            it[KursInGruppeTable.createdAt] = LocalDateTime.now()
        }
    }

    fun removeKursFromGruppe(kursgruppeId: Int, kursId: Int) = transaction {
        val deleted = KursInGruppeTable.deleteWhere {
            (KursInGruppeTable.kursgruppeId eq kursgruppeId) and
            (KursInGruppeTable.kursId eq kursId)
        }

        if (deleted == 0) {
            throw InvalidKursgruppeInputException("Kurs $kursId is not in group $kursgruppeId")
        }
    }

    fun getKurseInGruppe(kursgruppeId: Int): List<KursLookupDTO> = transaction {
        Kursgruppe.findById(kursgruppeId) ?: throw KursgruppeNotFoundException(kursgruppeId)

        val kursIds = KursInGruppeTable.select { KursInGruppeTable.kursgruppeId eq kursgruppeId }
            .map { it[KursInGruppeTable.kursId] }

        if (kursIds.isEmpty()) return@transaction emptyList()

        Kurs.find { KursTable.id inList kursIds }
            .map { it.toLookupDTO() }
    }
}
