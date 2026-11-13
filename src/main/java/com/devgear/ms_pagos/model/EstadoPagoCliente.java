package com.devgear.ms_pagos.model;

import jakarta.persistence.*;
import java.time.Instant;

/** Estado antifraude de un cliente: lo escribe el consumidor de Kafka, lo lee ms-pagos antes de cobrar. */
@Entity
@Table(name = "estado_pago_cliente")
public class EstadoPagoCliente {

    @Id
    @Column(name = "id_cliente", length = 64)
    private String idCliente;

    // Intentos dentro de la ventana en el momento de la última evaluación
    @Column(name = "intentos_en_ventana", nullable = false)
    private int intentosEnVentana;

    @Column(nullable = false)
    private boolean bloqueado;

    @Column(name = "bloqueado_hasta")
    private Instant bloqueadoHasta;

    @Column(name = "actualizado_en")
    private Instant actualizadoEn;

    public EstadoPagoCliente() {}

    public String getIdCliente() { return idCliente; }
    public void setIdCliente(String idCliente) { this.idCliente = idCliente; }
    public int getIntentosEnVentana() { return intentosEnVentana; }
    public void setIntentosEnVentana(int intentosEnVentana) { this.intentosEnVentana = intentosEnVentana; }
    public boolean isBloqueado() { return bloqueado; }
    public void setBloqueado(boolean bloqueado) { this.bloqueado = bloqueado; }
    public Instant getBloqueadoHasta() { return bloqueadoHasta; }
    public void setBloqueadoHasta(Instant bloqueadoHasta) { this.bloqueadoHasta = bloqueadoHasta; }
    public Instant getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(Instant actualizadoEn) { this.actualizadoEn = actualizadoEn; }
}
