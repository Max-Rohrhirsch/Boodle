package org.boodle.backend.integration

import org.boodle.backend.model.EnrollmentAlreadyExistsException
import org.boodle.backend.model.InvalidVorlesungInputException
import org.boodle.backend.model.LectureEnrollmentService
import org.boodle.backend.model.LectureEnrollmentTable
import org.boodle.backend.model.User
import org.boodle.backend.model.UserRole
import org.boodle.backend.model.UserService
import org.boodle.backend.model.UsersTable
import org.boodle.backend.model.Vorlesung
import org.boodle.backend.model.VorlesungService
import org.boodle.backend.model.VorlesungTable
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class LectureEnrollmentIntegrationTest {

    private val userService = UserService()
    private val vorlesungService = VorlesungService()
    private val lectureEnrollmentService = LectureEnrollmentService()

    companion object {
        @JvmStatic
        @BeforeAll
        fun connectDatabase() {
            Database.connect(
                url = "jdbc:h2:mem:lectureenrolldb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                driver = "org.h2.Driver",
                user = "sa",
                password = ""
            )

            transaction {
                SchemaUtils.create(UsersTable, VorlesungTable, LectureEnrollmentTable)
            }
        }
    }

    @BeforeEach
    fun clearTables() {
        transaction {
            LectureEnrollmentTable.deleteAll()
            Vorlesung.all().forEach { it.delete() }
            User.all().forEach { it.delete() }
        }
    }

    @Test
    fun enrollStudentDirect_persistsEnrollmentAndReturnsStudent() {
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
        vorlesungService.create(
            code = "INF101",
            name = "Einführung",
            beschreibung = "Basics",
            studiengang = "Informatik",
            dozentMatr = "D1000001"
        ).id

        val lecture = vorlesungService.getAllVorlesungen().first()

        lectureEnrollmentService.enrollStudentDirect(lecture.id, "1234567")

        assertEquals(listOf("1234567"), lectureEnrollmentService.getEnrolledStudents(lecture.id))
    }

    @Test
    fun enrollStudentDirect_throwsOnDuplicateEnrollment() {
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
        vorlesungService.create(
            code = "INF101",
            name = "Einführung",
            beschreibung = "Basics",
            studiengang = "Informatik",
            dozentMatr = "D1000001"
        ).id
        val lecture = vorlesungService.getAllVorlesungen().first()
        lectureEnrollmentService.enrollStudentDirect(lecture.id, "1234567")

        val exception = assertThrows(EnrollmentAlreadyExistsException::class.java) {
            lectureEnrollmentService.enrollStudentDirect(lecture.id, "1234567")
        }

        assertEquals("Student '1234567' is already enrolled in Vorlesung '${lecture.id}'.", exception.message)
    }

    @Test
    fun enrollStudentDirect_throwsWhenStudentMissing() {
        userService.create(
            matr = "D1000001",
            name = "Professor Test",
            password = "secure-pass-123",
            email = "dozent@example.com",
            rolle = UserRole.DOZENT
        )
        vorlesungService.create(
            code = "INF101",
            name = "Einführung",
            beschreibung = "Basics",
            studiengang = "Informatik",
            dozentMatr = "D1000001"
        ).id

        val lecture = vorlesungService.getAllVorlesungen().first()

        val exception = assertThrows(InvalidVorlesungInputException::class.java) {
            lectureEnrollmentService.enrollStudentDirect(lecture.id, "1234567")
        }

        assertEquals("Student with matr '1234567' not found.", exception.message)
    }
}
