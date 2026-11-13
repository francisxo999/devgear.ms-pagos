package com.devgear.ms_pagos.repository;

import com.devgear.ms_pagos.model.EstadoPagoCliente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface EstadoPagoClienteRepository extends JpaRepository<EstadoPagoCliente, String> {

    List<EstadoPagoCliente> findByBloqueadoTrueAndBloqueadoHastaAfter(Instant ahora);
}
