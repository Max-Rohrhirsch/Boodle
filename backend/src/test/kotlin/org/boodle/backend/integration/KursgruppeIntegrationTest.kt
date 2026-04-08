package org.boodle.backend.integration

import org.boodle.backend.model.Kurs
import org.boodle.backend.model.KursTable
import org.boodle.backend.model.Kursgruppe
import org.boodle.backend.model.KursgruppeService
import org.boodle.backend.model.KursgruppeTable
import org.boodle.backend.model.KursInGruppeTable
import org.boodle.backend.model.InvalidKursgruppeInputException
import org.boodle.backend.model.KursgruppeNotFoundException
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class KursgruppeIntegrationTest {

    private val kursgruppeService = KursgruppeService()

    companion object {
        @JvmStatic
        @BeforeAll
        fun connectDatabase() {
            Database.connect(
                url = "jdbc:h2:mem:kursgruppedb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                driver = "org.h2.Driver",
                user = "sa",
                password = ""
            )

            transaction {
                SchemaUtils.create(KursTable, KursgruppeTable, KursInGruppeTable)
            }
        }
    }

    @BeforeEach
    fun clearTables() = transaction {
        Kursgruppe.all().forEach { it.delete() }
        Kurs.all().forEach { it.delete() }
    }

    @Test
    fun createKursgruppe_withValidData() {
        val created = kursgruppeService.create("Informatik Grundlagen", "Group for foundational courses")

        assertNotNull(created)
        assertEquals("Informatik Grundlagen", created.name)
        assertEquals("Group for foundational courses", created.beschreibung)
    }

    @Test
    fun createKursgruppe_withoutBeschreibung() {
        val created = kursgruppeService.create("Mathematik Advanced", null)

        assertNotNull(created)
        assertEquals("Mathematik Advanced", created.name)
        assertEquals(null, created.beschreibung)
    }

    @Test
    fun createKursgruppe_rejectsEmptyName() {
        assertThrows<InvalidKursgruppeInputException> {
            kursgruppeService.create("", "Non-empty description")
        }
    }

    @Test
    fun createKursgruppe_rejectsTooLongName() {
        val longName = "A".repeat(256)
        assertThrows<InvalidKursgruppeInputException> {
            kursgruppeService.create(longName, null)
        }
    }

    @Test
    fun updateKursgruppe_withValidData() {
        val created = kursgruppeService.create("Original Name", "Original Description")
        val id = created.id

        val updated = kursgruppeService.update(id, "Updated Name", "Updated Description")

        assertEquals("Updated Name", updated.name)
        assertEquals("Updated Description", updated.beschreibung)
    }

    @Test
    fun updateKursgruppe_nonExistentId_throwsException() {
        assertThrows<KursgruppeNotFoundException> {
            kursgruppeService.update(9999, "Name", "Desc")
        }
    }

    @Test
    fun deleteKursgruppe_removesSuccessfully() {
        val created = kursgruppeService.create("To Delete", null)
        val id = created.id

        kursgruppeService.delete(id)

        assertThrows<KursgruppeNotFoundException> {
            kursgruppeService.getKursgruppeById(id)
        }
    }

    @Test
    fun addKursToGruppe_succeeds() {
        val group = kursgruppeService.create("Group 1", null)
        val kurs = transaction {
            Kurs.new {
                name = "Course 1"
                dozentMatr = "D0001"
                createdAt = LocalDateTime.now()
                updatedAt = LocalDateTime.now()
            }
        }

        kursgruppeService.addKursToGruppe(group.id, kurs.id.value)

        val kurse = kursgruppeService.getKurseInGruppe(group.id)
        assertEquals(1, kurse.size)
        assertEquals("Course 1", kurse[0].name)
    }

    @Test
    fun addKursToGruppe_throwsWhenKursNotFound() {
        val group = kursgruppeService.create("Group 1", null)

        assertThrows<RuntimeException> {
            kursgruppeService.addKursToGruppe(group.id, 9999)
        }
    }

    @Test
    fun addKursToGruppe_throwsWhenAlreadyInGroup() {
        val group = kursgruppeService.create("Group 1", null)
        val kurs = transaction {
            Kurs.new {
                name = "Course 1"
                dozentMatr = "D0001"
                createdAt = LocalDateTime.now()
                updatedAt = LocalDateTime.now()
            }
        }

        kursgruppeService.addKursToGruppe(group.id, kurs.id.value)

        assertThrows<InvalidKursgruppeInputException> {
            kursgruppeService.addKursToGruppe(group.id, kurs.id.value)
        }
    }

    @Test
    fun getKursgruppe_withKurse() {
        val group = kursgruppeService.create("Group with Courses", "Description")
        val kurs1 = transaction {
            Kurs.new {
                name = "Course 1"
                dozentMatr = "D0001"
                createdAt = LocalDateTime.now()
                updatedAt = LocalDateTime.now()
            }
        }
        val kurs2 = transaction {
            Kurs.new {
                name = "Course 2"
                dozentMatr = "D0002"
                createdAt = LocalDateTime.now()
                updatedAt = LocalDateTime.now()
            }
        }

        kursgruppeService.addKursToGruppe(group.id, kurs1.id.value)
        kursgruppeService.addKursToGruppe(group.id, kurs2.id.value)

        val groupWithKurse = kursgruppeService.getKursgruppeWithKurseById(group.id)

        assertEquals(2, groupWithKurse.kurse.size)
        assertEquals("Course 1", groupWithKurse.kurse[0].name)
        assertEquals("Course 2", groupWithKurse.kurse[1].name)
    }

    @Test
    fun getAllKursgruppen() {
        kursgruppeService.create("Group 1", null)
        kursgruppeService.create("Group 2", "With description")
        kursgruppeService.create("Group 3", null)

        val all = kursgruppeService.getAllKursgruppen()

        assertEquals(3, all.size)
    }
}
