package com.devgear.ms_pagos.repository;

import com.devgear.ms_pagos.model.Pago;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PagoRepository extends JpaRepository<Pago, Long> {

    // Bloquea la fila: si el navegador llama dos veces a /commit, la segunda espera a la primera.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Pago p where p.token = :token")
    Optional<Pago> findByTokenForUpdate(@Param("token") String token);

    Optional<Pago> findByBuyOrder(String buyOrder);

    List<Pago> findByUsuarioIdOrderByCreadoEnDesc(String usuarioId);

    long countByEstado(String estado);
}
