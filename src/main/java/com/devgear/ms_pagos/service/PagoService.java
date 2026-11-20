package com.devgear.ms_pagos.service;

import com.devgear.ms_pagos.client.CarritoClient;
import com.devgear.ms_pagos.client.OrdenClient;
import com.devgear.ms_pagos.dto.IniciarPagoResponseDTO;
import com.devgear.ms_pagos.dto.MetricasPagosDTO;
import com.devgear.ms_pagos.dto.OrdenCreadaDTO;
import com.devgear.ms_pagos.dto.PagoResultadoDTO;
import com.devgear.ms_pagos.exception.ResourceNotFoundException;
import com.devgear.ms_pagos.kafka.IntentoPagoProducer;
import com.devgear.ms_pagos.model.Pago;
import com.devgear.ms_pagos.repository.EstadoPagoClienteRepository;
import com.devgear.ms_pagos.repository.IntentoPagoRepository;
import com.devgear.ms_pagos.repository.PagoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class PagoService {

    private static final Logger log = LoggerFactory.getLogger(PagoService.class);

    private final PagoRepository pagos;
    private final IntentoPagoRepository intentos;
    private final EstadoPagoClienteRepository estados;
    private final AntifraudeService antifraude;
    private final IntentoPagoProducer producer;
    private final WebpayGateway webpay;
    private final CarritoClient carritoClient;
    private final OrdenClient ordenClient;
    private final String returnUrl;

    public PagoService(PagoRepository pagos, IntentoPagoRepository intentos, EstadoPagoClienteRepository estados,
                       AntifraudeService antifraude, IntentoPagoProducer producer, WebpayGateway webpay,
                       CarritoClient carritoClient, OrdenClient ordenClient,
                       @Value("${devgear.webpay.return-url}") String returnUrl) {
        this.pagos = pagos;
        this.intentos = intentos;
        this.estados = estados;
        this.antifraude = antifraude;
        this.producer = producer;
        this.webpay = webpay;
        this.carritoClient = carritoClient;
        this.ordenClient = ordenClient;
        this.returnUrl = returnUrl;
    }

    /** Paso 1: valida bloqueo, calcula el monto desde el carrito, registra el intento en Kafka y abre Webpay. */
    public IniciarPagoResponseDTO iniciar(String usuarioId, String bearerToken) {
        antifraude.verificarNoBloqueado(usuarioId);          // 429 si está bloqueado
        long monto = carritoClient.totalCarrito(bearerToken); // 400 si el carrito está vacío

        Pago pago = new Pago();
        pago.setUsuarioId(usuarioId);
        pago.setMonto(monto);
        pago.setCreadoEn(Instant.now());
        pago = pagos.save(pago);
        pago.setBuyOrder("DG" + pago.getId());

        producer.publicar(usuarioId, monto, pago.getBuyOrder());

        try {
            WebpayGateway.TransaccionCreada tx = webpay.crear(pago.getBuyOrder(), usuarioId, monto, returnUrl);
            pago.setToken(tx.token());
            pago.setActualizadoEn(Instant.now());
            pagos.save(pago);
            return new IniciarPagoResponseDTO(tx.url(), tx.token(), monto, pago.getBuyOrder());
        } catch (RuntimeException e) {
            pago.setEstado(Pago.ERROR);
            pago.setActualizadoEn(Instant.now());
            pagos.save(pago);
            throw e;
        }
    }

    /**
     * Paso 2: Webpay devuelve al cliente al frontend con token_ws y el frontend llama aquí.
     * Es idempotente: repetir la llamada no vuelve a confirmar ni a crear otra orden.
     */
    @Transactional
    public PagoResultadoDTO confirmar(String usuarioId, String bearerToken, String token) {
        Pago pago = pagos.findByTokenForUpdate(token)
                .filter(p -> p.getUsuarioId().equals(usuarioId))
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado"));

        if (Pago.PENDIENTE.equals(pago.getEstado())) {
            WebpayGateway.TransaccionConfirmada r = webpay.confirmar(token);
            if ("AUTHORIZED".equals(r.status())) {
                pago.setEstado(Pago.APROBADO);
                pago.setMetodoPago(WebpayGateway.describirMedioDePago(r.tipoPago(), r.ultimosDigitos()));
                log.info("Pago aprobado: {} por ${}", pago.getBuyOrder(), pago.getMonto());
            } else {
                pago.setEstado(Pago.RECHAZADO);
                log.warn("Pago rechazado por Transbank: {} (estado {})", pago.getBuyOrder(), r.status());
            }
            pago.setActualizadoEn(Instant.now());
            pagos.save(pago);
        }

        // Pagado pero sin orden todavía (primera vez, o un intento anterior de crearla falló): se crea ahora.
        String mensaje = null;
        if (Pago.APROBADO.equals(pago.getEstado()) && pago.getOrdenId() == null) {
            try {
                OrdenCreadaDTO orden = ordenClient.crearOrden(bearerToken, pago.getMetodoPago());
                pago.setOrdenId(orden.id());
                pago.setActualizadoEn(Instant.now());
                pagos.save(pago);
            } catch (RuntimeException e) {
                log.error("Pago {} aprobado pero no se pudo crear la orden: {}", pago.getBuyOrder(), e.getMessage());
                mensaje = "Tu pago fue aprobado, pero no pudimos crear la orden todavía. "
                        + "Vuelve a abrir este resultado en unos minutos (código " + pago.getBuyOrder() + ").";
            }
        }
        return new PagoResultadoDTO(pago.getEstado(), pago.getMonto(), pago.getBuyOrder(), pago.getOrdenId(), mensaje);
    }

    /** El cliente anuló el pago en el formulario de Webpay (vuelve con TBK_TOKEN). */
    @Transactional
    public void cancelar(String usuarioId, String token) {
        pagos.findByTokenForUpdate(token)
                .filter(p -> p.getUsuarioId().equals(usuarioId))
                .ifPresent(p -> {
                    if (Pago.PENDIENTE.equals(p.getEstado())) {
                        p.setEstado(Pago.CANCELADO);
                        p.setActualizadoEn(Instant.now());
                        pagos.save(p);
                    }
                });
    }

    @Transactional(readOnly = true)
    public MetricasPagosDTO metricas() {
        Map<String, Long> porEstado = new LinkedHashMap<>();
        for (String estado : new String[]{Pago.PENDIENTE, Pago.APROBADO, Pago.RECHAZADO, Pago.CANCELADO, Pago.ERROR}) {
            porEstado.put(estado, pagos.countByEstado(estado));
        }
        return new MetricasPagosDTO(
                intentos.count(),
                intentos.countByProcesadoEnAfter(Instant.now().minusSeconds(60)),
                estados.findByBloqueadoTrueAndBloqueadoHastaAfter(Instant.now()).size(),
                porEstado);
    }
}
