package com.devgear.ms_pagos.controller;

import com.devgear.ms_pagos.dto.*;
import com.devgear.ms_pagos.service.AntifraudeService;
import com.devgear.ms_pagos.service.PagoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pagos")
public class PagoController {

    private final PagoService pagoService;
    private final AntifraudeService antifraude;

    public PagoController(PagoService pagoService, AntifraudeService antifraude) {
        this.pagoService = pagoService;
        this.antifraude = antifraude;
    }

    // Sin body: el monto sale del carrito del usuario y la identidad, del token.
    @PostMapping("/iniciar")
    public ResponseEntity<IniciarPagoResponseDTO> iniciar(JwtAuthenticationToken auth) {
        return ResponseEntity.ok(pagoService.iniciar(usuarioId(auth), bearer(auth)));
    }

    @PostMapping("/commit")
    public ResponseEntity<PagoResultadoDTO> commit(@RequestParam("token_ws") String tokenWs, JwtAuthenticationToken auth) {
        return ResponseEntity.ok(pagoService.confirmar(usuarioId(auth), bearer(auth), tokenWs));
    }

    // Webpay devuelve TBK_TOKEN cuando el cliente anula el pago en su formulario.
    @PostMapping("/anular")
    public ResponseEntity<Void> anular(@RequestParam("token") String token, JwtAuthenticationToken auth) {
        pagoService.cancelar(usuarioId(auth), token);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/estado")
    public ResponseEntity<EstadoAntifraudeDTO> estado(JwtAuthenticationToken auth) {
        return ResponseEntity.ok(antifraude.estado(usuarioId(auth)));
    }

    // ---- Administración / dashboard ----

    @GetMapping("/admin/bloqueos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<BloqueoDTO>> bloqueos() {
        return ResponseEntity.ok(antifraude.bloqueosActivos());
    }

    @DeleteMapping("/admin/bloqueos/{idCliente}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> desbloquear(@PathVariable String idCliente) {
        antifraude.desbloquear(idCliente);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/metricas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MetricasPagosDTO> metricas() {
        return ResponseEntity.ok(pagoService.metricas());
    }

    private String usuarioId(JwtAuthenticationToken auth) {
        return auth.getToken().getClaimAsString("oid");
    }

    private String bearer(JwtAuthenticationToken auth) {
        return "Bearer " + auth.getToken().getTokenValue();
    }
}
