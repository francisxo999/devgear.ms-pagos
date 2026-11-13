package com.devgear.ms_pagos.model;

import jakarta.persistence.*;
import java.time.Instant;

/** Un intento de pago consumido desde pagos.intentos. PK = eventId (idempotencia). */
@Entity
@Table(name = "intentos_pago", indexes = {
        @Index(name = "idx_intento_cliente_fecha", columnList = "idCliente,ocurridoEn")
})
public class IntentoPago {

    @Id
    @Column(length = 64)
    private String eventId;

    @Column(nullable = false, length = 64)
    private String idCliente;

    private Long monto;

    @Column(length = 64)
    private String idTransaccion;

    @Column(nullable = false)
    private Instant ocurridoEn;

    @Column(nullable = false)
    private Instant procesadoEn;

    private Integer particion;

    @Column(name = "kafka_offset")
    private Long offsetKafka;

    public IntentoPago() {}

    public String getEventId() { return eventId; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public String getIdCliente() { return idCliente; }
    public void setIdCliente(String idCliente) { this.idCliente = idCliente; }
    public Long getMonto() { return monto; }
    public void setMonto(Long monto) { this.monto = monto; }
    public String getIdTransaccion() { return idTransaccion; }
    public void setIdTransaccion(String idTransaccion) { this.idTransaccion = idTransaccion; }
    public Instant getOcurridoEn() { return ocurridoEn; }
    public void setOcurridoEn(Instant ocurridoEn) { this.ocurridoEn = ocurridoEn; }
    public Instant getProcesadoEn() { return procesadoEn; }
    public void setProcesadoEn(Instant procesadoEn) { this.procesadoEn = procesadoEn; }
    public Integer getParticion() { return particion; }
    public void setParticion(Integer particion) { this.particion = particion; }
    public Long getOffsetKafka() { return offsetKafka; }
    public void setOffsetKafka(Long offsetKafka) { this.offsetKafka = offsetKafka; }
}
