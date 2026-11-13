package com.devgear.ms_pagos.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Evento del tópico "pagos.intentos". Clave del mensaje = idCliente: todos los intentos de un
 * mismo cliente van a la misma partición, así se procesan en orden y el conteo por ventana es correcto.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record IntentoPagoEvento(
        String eventId,
        String idCliente,
        Long monto,
        String idTransaccion,
        String timestamp
) {}
