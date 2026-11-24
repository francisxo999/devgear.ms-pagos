package com.devgear.ms_pagos.service;

import com.devgear.ms_pagos.dto.BloqueoDTO;
import com.devgear.ms_pagos.dto.EstadoAntifraudeDTO;
import com.devgear.ms_pagos.exception.ClienteBloqueadoException;
import com.devgear.ms_pagos.kafka.IntentoPagoEvento;
import com.devgear.ms_pagos.model.EstadoPagoCliente;
import com.devgear.ms_pagos.model.IntentoPago;
import com.devgear.ms_pagos.repository.EstadoPagoClienteRepository;
import com.devgear.ms_pagos.repository.IntentoPagoRepository;
import com.devgear.ms_pagos.telemetria.TelemetriaPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Regla de velocidad: si un cliente hace {max-intentos} o más intentos de pago dentro de
 * {ventana-segundos}, queda bloqueado {bloqueo-minutos}. El conteo usa la hora del propio
 * evento y una ventana deslizante, por lo que no depende de cuándo lo procese el consumidor.
 */
@Service
public class AntifraudeService {

    private static final Logger log = LoggerFactory.getLogger(AntifraudeService.class);

    private final IntentoPagoRepository intentos;
    private final EstadoPagoClienteRepository estados;
    private final TelemetriaPublisher telemetria;
    private final int maxIntentos;
    private final long ventanaSegundos;
    private final long bloqueoMinutos;

    public AntifraudeService(IntentoPagoRepository intentos, EstadoPagoClienteRepository estados,
                             TelemetriaPublisher telemetria,
                             @Value("${devgear.antifraude.max-intentos:3}") int maxIntentos,
                             @Value("${devgear.antifraude.ventana-segundos:10}") long ventanaSegundos,
                             @Value("${devgear.antifraude.bloqueo-minutos:30}") long bloqueoMinutos) {
        this.intentos = intentos;
        this.estados = estados;
        this.telemetria = telemetria;
        this.maxIntentos = maxIntentos;
        this.ventanaSegundos = ventanaSegundos;
        this.bloqueoMinutos = bloqueoMinutos;
    }

    /** Lanza ClienteBloqueadoException (HTTP 429) si el cliente está bloqueado. Libera bloqueos vencidos. */
    @Transactional
    public void verificarNoBloqueado(String idCliente) {
        Optional<EstadoPagoCliente> opt = estados.findById(idCliente);
        if (opt.isEmpty() || !opt.get().isBloqueado()) {
            return;
        }
        EstadoPagoCliente estado = opt.get();
        Instant ahora = Instant.now();
        if (estado.getBloqueadoHasta() != null && ahora.isBefore(estado.getBloqueadoHasta())) {
            throw new ClienteBloqueadoException(estado.getBloqueadoHasta());
        }
        estado.setBloqueado(false);
        estado.setBloqueadoHasta(null);
        estado.setIntentosEnVentana(0);
        estado.setActualizadoEn(ahora);
        estados.save(estado);
        log.info("El bloqueo de {} expiró; cliente desbloqueado", idCliente);
    }

    /** Llamado por el consumidor de Kafka por cada intento de pago. Idempotente por eventId. */
    @Transactional
    public void procesarIntento(IntentoPagoEvento evento, int particion, long offset) {
        if (intentos.existsById(evento.eventId())) {
            log.debug("Intento {} ya procesado, se ignora", evento.eventId());
            return;
        }
        Instant ocurrio = Instant.parse(evento.timestamp());

        IntentoPago intento = new IntentoPago();
        intento.setEventId(evento.eventId());
        intento.setIdCliente(evento.idCliente());
        intento.setMonto(evento.monto());
        intento.setIdTransaccion(evento.idTransaccion());
        intento.setOcurridoEn(ocurrio);
        intento.setProcesadoEn(Instant.now());
        intento.setParticion(particion);
        intento.setOffsetKafka(offset);
        intentos.saveAndFlush(intento);
        telemetria.publicar(evento.eventId(), "PAGO_INTENTO", evento.idCliente(), null, null, evento.monto());

        long enVentana = intentos.countByIdClienteAndOcurridoEnBetween(
                evento.idCliente(), ocurrio.minus(Duration.ofSeconds(ventanaSegundos)), ocurrio);

        EstadoPagoCliente estado = estados.findById(evento.idCliente()).orElseGet(() -> {
            EstadoPagoCliente nuevo = new EstadoPagoCliente();
            nuevo.setIdCliente(evento.idCliente());
            return nuevo;
        });
        estado.setIntentosEnVentana((int) enVentana);
        estado.setActualizadoEn(Instant.now());

        Instant ahora = Instant.now();
        boolean yaBloqueado = estado.isBloqueado() && estado.getBloqueadoHasta() != null
                && ahora.isBefore(estado.getBloqueadoHasta());
        if (enVentana >= maxIntentos && !yaBloqueado) {
            estado.setBloqueado(true);
            estado.setBloqueadoHasta(ahora.plus(Duration.ofMinutes(bloqueoMinutos)));
            log.error("ALERTA ANTIFRAUDE: cliente {} hizo {} intentos de pago en {} s -> bloqueado {} min (hasta {})",
                    evento.idCliente(), enVentana, ventanaSegundos, bloqueoMinutos, estado.getBloqueadoHasta());
            telemetria.publicar(evento.eventId() + "-bloqueo", "PAGO_BLOQUEADO", evento.idCliente(), null, null, null);
        } else {
            log.info("Intento de pago de {} procesado ({} en la ventana de {} s) <- {}-{}",
                    evento.idCliente(), enVentana, ventanaSegundos, particion, offset);
        }
        estados.save(estado);
    }

    @Transactional(readOnly = true)
    public EstadoAntifraudeDTO estado(String idCliente) {
        Optional<EstadoPagoCliente> opt = estados.findById(idCliente);
        if (opt.isEmpty()) {
            return new EstadoAntifraudeDTO(false, null, 0);
        }
        EstadoPagoCliente e = opt.get();
        boolean activo = e.isBloqueado() && e.getBloqueadoHasta() != null && Instant.now().isBefore(e.getBloqueadoHasta());
        return new EstadoAntifraudeDTO(activo, activo ? e.getBloqueadoHasta() : null, e.getIntentosEnVentana());
    }

    @Transactional(readOnly = true)
    public List<BloqueoDTO> bloqueosActivos() {
        return estados.findByBloqueadoTrueAndBloqueadoHastaAfter(Instant.now()).stream()
                .map(e -> new BloqueoDTO(e.getIdCliente(), e.getBloqueadoHasta(), e.getIntentosEnVentana()))
                .toList();
    }

    /** Para repetir la demo: el administrador libera a un cliente. */
    @Transactional
    public void desbloquear(String idCliente) {
        estados.findById(idCliente).ifPresent(e -> {
            e.setBloqueado(false);
            e.setBloqueadoHasta(null);
            e.setIntentosEnVentana(0);
            e.setActualizadoEn(Instant.now());
            estados.save(e);
            log.info("Cliente {} desbloqueado manualmente", idCliente);
        });
    }
}
