package com.parquimetro.api

import com.parquimetro.security.JwtService
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class VagaControllerTest {

    companion object {
        @Container
        val postgres = PostgreSQLContainer("postgis/postgis:16-3.4").apply {
            withDatabaseName("parquimetro")
            withUsername("parquimetro")
            withPassword("parquimetro")
        }

        @JvmStatic
        @DynamicPropertySource
        fun props(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
            registry.add("mqtt.broker-url") { "tcp://localhost:1883" }
        }
    }

    @Autowired lateinit var mvc: MockMvc
    @Autowired lateinit var jwtService: JwtService

    private fun token() = jwtService.generate("admin", listOf("ROLE_ADMIN"))

    @Test
    fun `GET vagas retorna 200`() {
        mvc.get("/api/vagas") {
            header("Authorization", "Bearer ${token()}")
        }.andExpect { status { isOk() } }
    }

    @Test
    fun `GET vagas sem token retorna 403`() {
        mvc.get("/api/vagas").andExpect { status { isForbidden() } }
    }

    @Test
    fun `POST vagas cria nova vaga`() {
        mvc.post("/api/vagas") {
            header("Authorization", "Bearer ${token()}")
            contentType = MediaType.APPLICATION_JSON
            content = """{"codigo":"B-01","lat":-23.561,"lng":-46.655}"""
        }.andExpect {
            status { isCreated() }
            jsonPath("$.codigo") { value("B-01") }
            jsonPath("$.status") { value("LIVRE") }
        }
    }
}
