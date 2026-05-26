package com.parquimetro.api.controller

import com.parquimetro.api.dto.*
import com.parquimetro.domain.entity.VagaStatus
import com.parquimetro.security.JwtService
import com.parquimetro.service.InfracaoService
import com.parquimetro.service.PagamentoService
import com.parquimetro.service.VagaService
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/auth")
class AuthController(private val jwtService: JwtService) {
    @PostMapping("/login")
    fun login(@RequestBody req: AuthRequest): AuthResponse {
        // TODO: validar contra UserRepository; stub para dev
        check(req.password == System.getenv("ADMIN_PASS")) { "Unauthorized" }
        return AuthResponse(jwtService.generate(req.username, listOf("ROLE_ADMIN")))
    }
}

@RestController
@RequestMapping("/api/vagas")
class VagaController(private val vagaService: VagaService) {

    @GetMapping
    fun list(pageable: Pageable) = vagaService.findAll(pageable)

    @GetMapping("/{id}")
    fun get(@PathVariable id: UUID) = vagaService.findById(id)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestBody req: VagaCreateRequest) = vagaService.create(req)

    @PatchMapping("/{id}/status")
    fun status(@PathVariable id: UUID, @RequestParam status: VagaStatus) =
        vagaService.updateStatus(id, status)
}

@RestController
@RequestMapping("/api/pagamentos")
class PagamentoController(private val pagamentoService: PagamentoService) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun pagar(@RequestBody req: PagamentoRequest) = pagamentoService.pagar(req)
}

@RestController
@RequestMapping("/api/infracoes")
class InfracaoController(private val infracaoService: InfracaoService) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun registrar(@RequestBody req: InfracaoRequest) = infracaoService.registrar(req)
}
