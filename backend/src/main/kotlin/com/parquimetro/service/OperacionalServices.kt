package com.parquimetro.service

import com.parquimetro.api.dto.InfracaoRequest
import com.parquimetro.api.dto.PagamentoRequest
import com.parquimetro.api.dto.PagamentoResponse
import com.parquimetro.domain.entity.Infracao
import com.parquimetro.domain.entity.Pagamento
import com.parquimetro.domain.entity.StatusPagamento
import com.parquimetro.domain.entity.VagaStatus
import com.parquimetro.domain.repository.InfracaoRepository
import com.parquimetro.domain.repository.PagamentoRepository
import com.parquimetro.domain.repository.VagaRepository
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit

private const val VALOR_POR_MINUTO = 0.10

@Service
@Transactional
class PagamentoService(
    private val pagamentoRepo: PagamentoRepository,
    private val vagaRepo: VagaRepository
) {
    fun pagar(req: PagamentoRequest): PagamentoResponse {
        val vaga = vagaRepo.findById(req.vagaId).orElseThrow()
        val expira = Instant.now().plus(req.duracaoMinutos.toLong(), ChronoUnit.MINUTES)
        val valor = req.duracaoMinutos * VALOR_POR_MINUTO
        val p = pagamentoRepo.save(
            Pagamento(
                vaga = vaga,
                motoristaCpf = req.motoristaCpf,
                placa = req.placa,
                expiraEm = expira,
                valor = valor,
                statusPagamento = StatusPagamento.PAGO
            )
        )
        vaga.status = VagaStatus.OCUPADA
        vagaRepo.save(vaga)
        return PagamentoResponse(p.id, vaga.id, expira.toEpochMilli(), valor)
    }

    @Scheduled(fixedDelay = 60_000)
    fun expirarPagamentos() {
        pagamentoRepo.findByStatusPagamentoAndExpiraEmBefore(StatusPagamento.PAGO, Instant.now())
            .forEach { p ->
                p.statusPagamento = StatusPagamento.EXPIRADO
                p.vaga.status = VagaStatus.LIVRE
                pagamentoRepo.save(p)
            }
    }
}

@Service
@Transactional
class InfracaoService(
    private val infracaoRepo: InfracaoRepository,
    private val vagaRepo: VagaRepository
) {
    private val gf = GeometryFactory(PrecisionModel(), 4326)

    fun registrar(req: InfracaoRequest) {
        val vaga = vagaRepo.findById(req.vagaId).orElseThrow()
        val ponto = gf.createPoint(Coordinate(req.lng, req.lat))
        infracaoRepo.save(
            Infracao(
                vaga = vaga,
                fiscalId = req.fiscalId,
                fotoHash = req.fotoHash,
                localizacao = ponto
            )
        )
        vaga.status = VagaStatus.IRREGULAR
        vagaRepo.save(vaga)
    }
}
