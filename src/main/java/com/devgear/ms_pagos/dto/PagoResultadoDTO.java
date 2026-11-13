package com.devgear.ms_pagos.dto;

public record PagoResultadoDTO(String estado, Long monto, String buyOrder, Long ordenId, String mensaje) {}
