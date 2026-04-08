package org.boodle.backend.integration

import org.boodle.backend.model.KursEnrollmentService
import org.boodle.backend.model.KursEnrollmentTable
import org.boodle.backend.model.KursService
import org.boodle.backend.model.KursTable
import org.boodle.backend.model.User
import org.boodle.backend.model.UserLookupDTO
import org.boodle.backend.model.UserRole
import org.boodle.backend.model.UserService
import org.boodle.backend.model.UsersTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.boodle.backend.security.SecurityUtils

class KursEnrollmentIntegrationTest {

    private val userService = UserService()
    private val securityUtils = SecurityUtils()
    private val kursService = KursService(securityUtils)
    private val kursEnrollmentService = KursEnrollmentService()

    companion object {
        @JvmStatic
        @BeforeAll
        fun connectDatabase() {
            Database.connect(
                url = "jdbc:h2:mem:kursenrollmentdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                driver = "org.h2.Driver",
                user = "sa",
                password = ""
            )

            transaction {
                SchemaUtils.create(UsersTable, KursTable, KursEnrollmentTable)
            }
        }
    }

    @BeforeEach
    fun clearTables() {
        transaction {
            KursEnrollmentTable.deleteAll()
            KursTable.deleteAll()
            User.all().forEach { it.delete() }
        }
    }

    @Test
    fun enrollStudentToKurs_persistsEnrollmentAndReturnsStudentDetails() {
        val seed = seedCourseAndStudent()

        kursEnrollmentService.enrollStudentToKurs(seed.kursId, "1234567")

        val enrolledStudents = kursEnrollmentService.getEnrolledStudentsForKurs(seed.kursId)
        assertEquals(1, enrolledStudents.size)
        assertEquals("1234567", enrolledStudents.first().matr)
    }

    @Test
    fun enrollStudentToKurs_throwsOnDuplicateEnrollment() {
        val seed = seedCourseAndStudent()
        kursEnrollmentService.enrollStudentToKurs(seed.kursId, "1234567")

        val exception = assertThrows(RuntimeException::class.java) {
            kursEnrollmentService.enrollStudentToKurs(seed.kursId, "1234567")
        }

        assertEquals("Student '1234567' is already assigned to Kurs '${seed.kursId}'.", exception.message)
    }

    @Test
    fun unenrollStudentFromKurs_removesAssignment() {
        val seed = seedCourseAndStudent()
        kursEnrollmentService.enrollStudentToKurs(seed.kursId, "1234567")

        kursEnrollmentService.unenrollStudentFromKurs(seed.kursId, "1234567")

        val enrolledStudents = kursEnrollmentService.getEnrolledStudentsForKurs(seed.kursId)
        assertEquals(emptyList<UserLookupDTO>(), enrolledStudents)
    }

    private fun seedCourseAndStudent(): SeedData {
        userService.create(
            matr = "D1000001",
            name = "Professor Test",
            password = "secure-pass-123",
            email = "dozent@example.com",
            rolle = UserRole.DOZENT
        )

        userService.create(
            matr = "1234567",
            name = "Student Test",
            password = "secure-pass-123",
            email = "student@example.com",
            rolle = UserRole.STUDENT
        )

        val kurs = kursService.create(
            name = "Programmierung 1",
            dozentMatr = "D1000001",
            kurssprecher1Matr = null,
            kurssprecher2Matr = null
        )

        return SeedData(kurs.id)
    }

    data class SeedData(
        val kursId: Int
    )
}

