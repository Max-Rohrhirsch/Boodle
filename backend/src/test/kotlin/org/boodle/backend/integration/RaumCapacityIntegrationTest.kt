package org.boodle.backend.integration

import org.boodle.backend.model.Raum
import org.boodle.backend.model.RaumService
import org.boodle.backend.model.RaumTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RaumCapacityIntegrationTest {

    private val raumService = RaumService()

    companion object {
        @JvmStatic
        @BeforeAll
        fun connectDatabase() {
            Database.connect(
                url = "jdbc:h2:mem:raumcapacitydb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                driver = "org.h2.Driver",
                user = "sa",
                password = ""
            )

            transaction {
                SchemaUtils.create(RaumTable)
            }
        }
    }

    @BeforeEach
    fun clearTables() = transaction {
        Raum.all().forEach { it.delete() }
    }

    @Test
    fun createRoom_withDefaultCapacity() {
        val created = raumService.create("A101", "Hörsaal A")

        assertNotNull(created)
        assertEquals("A101", created.code)
        assertEquals(30, created.kapazitaet)
        assertEquals("Hörsaal A", created.beschreibung)
    }

    @Test
    fun createRoom_withCustomCapacity() {
        val created = raumService.create("B202", "Seminarraum B", 50)

        assertNotNull(created)
        assertEquals("B202", created.code)
        assertEquals(50, created.kapazitaet)
    }

    @Test
    fun createRoom_withSmallCapacity() {
        val created = raumService.create("C303", "Büro", 5)

        assertNotNull(created)
        assertEquals(5, created.kapazitaet)
    }

    @Test
    fun createRoom_rejectsZeroCapacity() {
        assertThrows<RuntimeException> {
            raumService.create("D404", "Invalid Room", 0)
        }
    }

    @Test
    fun createRoom_rejectsNegativeCapacity() {
        assertThrows<RuntimeException> {
            raumService.create("E505", "Invalid Room", -10)
        }
    }

    @Test
    fun updateRoom_modifyCapacity() {
        val created = raumService.create("F606", "Lab")
        val roomId = created.id

        val updated = raumService.update(roomId, "F606", "Lab", 40)

        assertEquals(roomId, updated.id)
        assertEquals(40, updated.kapazitaet)
    }

    @Test
    fun updateRoom_decreaseCapacity() {
        val created = raumService.create("G707", "Large Hall", 100)
        val roomId = created.id

        val updated = raumService.update(roomId, "G707", "Medium Hall", 60)

        assertEquals(60, updated.kapazitaet)
    }

    @Test
    fun updateRoom_rejectsInvalidCapacity() {
        val created = raumService.create("H808", "Room")
        val roomId = created.id

        assertThrows<RuntimeException> {
            raumService.update(roomId, "H808", "Room", 0)
        }

        // Verify capacity unchanged
        val unchanged = raumService.getRaumById(roomId)
        assertEquals(30, unchanged.kapazitaet)
    }

    @Test
    fun getRoomById_includesCapacity() {
        val created = raumService.create("I909", "Room", 75)
        val roomId = created.id

        val retrieved = raumService.getRaumById(roomId)

        assertNotNull(retrieved)
        assertEquals(75, retrieved.kapazitaet)
    }

    @Test
    fun getAllRooms_includesCapacities() {
        raumService.create("J010", "Room 1", 30)
        raumService.create("J011", "Room 2", 50)
        raumService.create("J012", "Room 3", 20)

        val all = raumService.getAllRaeume()

        assertEquals(3, all.size)
        assert(all.any { it.code == "J010" && it.kapazitaet == 30 })
        assert(all.any { it.code == "J011" && it.kapazitaet == 50 })
        assert(all.any { it.code == "J012" && it.kapazitaet == 20 })
    }
}
