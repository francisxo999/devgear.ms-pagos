package com.devgear.ms_pagos.exception;

import java.time.Instant;

public class ClienteBloqueadoException extends RuntimeException {
    private final Instant bloqueadoHasta;

    public ClienteBloqueadoException(Instant bloqueadoHasta) {
        super("Demasiados intentos de pago detectados. Tu cuenta está bloqueada temporalmente para pagar.");
        this.bloqueadoHasta = bloqueadoHasta;
    }

    public Instant getBloqueadoHasta() { return bloqueadoHasta; }
}
