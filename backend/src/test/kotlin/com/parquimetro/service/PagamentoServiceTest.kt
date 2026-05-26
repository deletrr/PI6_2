package com.parquimetro.service

import com.parquimetro.api.dto.PagamentoRequest
import com.parquimetro.domain.entity.*
import com.parquimetro.domain.repository.PagamentoRepository
import com.parquimetro.domain.repository.VagaRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import java.time.Instant
import java.util.Optional
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class PagamentoServiceTest {

    @Mock lateinit var pagamentoRepo: PagamentoRepository
    @Mock lateinit var vagaRepo: VagaRepository
    @InjectMocks lateinit var service: PagamentoService

    private val gf = GeometryFactory(PrecisionModel(), 4326)

    private fun vaga(id: UUID = UUID.randomUUID()) = Vaga(
        id = id,
        codigo = "A-01",
        localizacao = gf.createPoint(Coordinate(-46.6, -23.5)),
        status = VagaStatus.LIVRE
    )

    @Test
    fun `pagar cria pagamento e muda vaga para OCUPADA`() {
        val vagaId = UUID.randomUUID()
        val v = vaga(vagaId)
        val req = PagamentoRequest(vagaId.toString(), "123.456.789-00", "ABC1D23", 60)

        `when`(vagaRepo.findById(vagaId)).thenReturn(Optional.of(v))
        `when`(pagamentoRepo.save(any())).thenAnswer { it.arguments[0] as Pagamento }
        `when`(vagaRepo.save(any())).thenReturn(v)

        val resp = service.pagar(req)

        assertEquals(VagaStatus.OCUPADA, v.status)
        assertTrue(resp.valor > 0)
        assertTrue(resp.expiraEm > Instant.now().toEpochMilli())
    }

    @Test
    fun `valor calculado e 0_10 por minuto`() {
        val vagaId = UUID.randomUUID()
        val v = vaga(vagaId)
        val req = PagamentoRequest(vagaId.toString(), "123", "ABC1D23", 30)

        `when`(vagaRepo.findById(vagaId)).thenReturn(Optional.of(v))
        `when`(pagamentoRepo.save(any())).thenAnswer { it.arguments[0] as Pagamento }
        `when`(vagaRepo.save(any())).thenReturn(v)

        val resp = service.pagar(req)
        assertEquals(3.0, resp.valor, 0.001)
    }
}
