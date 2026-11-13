package com.devgear.ms_pagos.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ClienteBloqueadoException.class)
    public ResponseEntity<Map<String, Object>> bloqueado(ClienteBloqueadoException ex) {
        Map<String, Object> body = cuerpo(HttpStatus.TOO_MANY_REQUESTS, "Cliente bloqueado", ex.getMessage());
        body.put("bloqueadoHasta", ex.getBloqueadoHasta() == null ? null : ex.getBloqueadoHasta().toString());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(body);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> noEncontrado(ResourceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(cuerpo(HttpStatus.NOT_FOUND, "No encontrado", ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> solicitudInvalida(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(cuerpo(HttpStatus.BAD_REQUEST, "Solicitud inválida", ex.getMessage()));
    }

    @ExceptionHandler(ServicioExternoException.class)
    public ResponseEntity<Map<String, Object>> servicioExterno(ServicioExternoException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(cuerpo(HttpStatus.SERVICE_UNAVAILABLE, "Servicio no disponible", ex.getMessage()));
    }

    @ExceptionHandler(WebpayException.class)
    public ResponseEntity<Map<String, Object>> webpay(WebpayException ex) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(cuerpo(HttpStatus.BAD_GATEWAY, "Pasarela de pago", ex.getMessage()));
    }

    private Map<String, Object> cuerpo(HttpStatus status, String error, String mensaje) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status.value());
        body.put("error", error);
        body.put("message", mensaje);
        return body;
    }
}
