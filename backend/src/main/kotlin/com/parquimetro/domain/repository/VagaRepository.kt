package com.parquimetro.domain.repository

import com.parquimetro.domain.entity.Vaga
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface VagaRepository : JpaRepository<Vaga, UUID> {
    fun findByCodigo(codigo: String): Vaga?
}
