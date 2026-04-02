package org.boodle.backend

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:contextdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "jwt.secret=0123456789abcdef0123456789abcdef",
        "jwt.issuer=test-issuer",
        "jwt.expiration-seconds=3600"
    ]
)
class BackendApplicationTests {

    @Test
    fun contextLoads() {
    }

}
