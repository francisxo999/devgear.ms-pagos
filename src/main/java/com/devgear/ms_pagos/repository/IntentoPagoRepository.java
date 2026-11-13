package com.devgear.ms_pagos.repository;

import com.devgear.ms_pagos.model.IntentoPago;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

public interface IntentoPagoRepository extends JpaRepository<IntentoPago, String> {

    long countByIdClienteAndOcurridoEnBetween(String idCliente, Instant desde, Instant hasta);

    long countByProcesadoEnAfter(Instant desde);
}
