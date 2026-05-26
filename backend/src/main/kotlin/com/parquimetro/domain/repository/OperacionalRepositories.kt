package com.parquimetro.domain.repository

import com.parquimetro.domain.entity.Infracao
import com.parquimetro.domain.entity.Pagamento
import com.parquimetro.domain.entity.StatusPagamento
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.UUID

interface InfracaoRepository : JpaRepository<Infracao, UUID> {
    fun findByVagaId(vagaId: UUID): List<Infracao>
}

interface PagamentoRepository : JpaRepository<Pagamento, UUID> {
    @Query("SELECT p FROM Pagamento p WHERE p.vaga.id = :vagaId AND p.expiraEm > :now AND p.statusPagamento = 'PAGO'")
    fun findAtivo(vagaId: UUID, now: Instant = Instant.now()): Pagamento?

    fun findByStatusPagamentoAndExpiraEmBefore(
        status: StatusPagamento,
        before: Instant
    ): List<Pagamento>
}
