package com.devgear.ms_pagos.exception;

/** Falla de un servicio del que depende el pago (carrito, ordenes). HTTP 503. */
public class ServicioExternoException extends RuntimeException {
    public ServicioExternoException(String message) { super(message); }
}
