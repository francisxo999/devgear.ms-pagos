package com.devgear.ms_pagos.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "pagos", indexes = {
        @Index(name = "idx_pago_token", columnList = "token"),
        @Index(name = "idx_pago_usuario", columnList = "usuarioId")
})
public class Pago {

    public static final String PENDIENTE = "PENDIENTE";
    public static final String APROBADO = "APROBADO";
    public static final String RECHAZADO = "RECHAZADO";
    public static final String CANCELADO = "CANCELADO";
    public static final String ERROR = "ERROR";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String usuarioId;

    // Código de orden que ve Webpay (máx. 26 caracteres): "DG" + id del pago
    @Column(length = 26)
    private String buyOrder;

    @Column(length = 100)
    private String token;

    @Column(nullable = false)
    private Long monto;

    @Column(nullable = false, length = 16)
    private String estado = PENDIENTE;

    // Orden creada en ms-ordenes una vez aprobado el pago
    private Long ordenId;

    // Medio de pago informado por Webpay, ej. "Webpay Plus · Tarjeta de crédito terminada en 6623"
    @Column(length = 200)
    private String metodoPago;

    @Column(nullable = false)
    private Instant creadoEn;

    private Instant actualizadoEn;

    public Pago() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsuarioId() { return usuarioId; }
    public void setUsuarioId(String usuarioId) { this.usuarioId = usuarioId; }
    public String getBuyOrder() { return buyOrder; }
    public void setBuyOrder(String buyOrder) { this.buyOrder = buyOrder; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public Long getMonto() { return monto; }
    public void setMonto(Long monto) { this.monto = monto; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }
    public Long getOrdenId() { return ordenId; }
    public void setOrdenId(Long ordenId) { this.ordenId = ordenId; }
    public Instant getCreadoEn() { return creadoEn; }
    public void setCreadoEn(Instant creadoEn) { this.creadoEn = creadoEn; }
    public Instant getActualizadoEn() { return actualizadoEn; }
    public void setActualizadoEn(Instant actualizadoEn) { this.actualizadoEn = actualizadoEn; }
}
