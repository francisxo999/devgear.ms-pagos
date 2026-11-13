package com.devgear.ms_pagos.dto;

public record IniciarPagoResponseDTO(String url, String token, Long monto, String buyOrder) {}
